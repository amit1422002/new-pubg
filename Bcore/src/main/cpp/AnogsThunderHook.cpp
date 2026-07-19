#include <Dobby/dobby.h>

#include <android/log.h>
#include <pthread.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <unistd.h>

#define TAG "AnogsThunder"
#define THUNDER_OFFSET 0x51FA80L
/** Was byte-patched RET — now inline hook returns 0. */
#define ANOGS_RET0_OFFSET 0x37F2CCL

static unsigned int rs;
static volatile int s_thunder_done;
static volatile int s_ret0_done;
static pthread_mutex_t s_hook_mu = PTHREAD_MUTEX_INITIALIZER;

typedef int64_t (*thunder_fn_t)(int64_t, int64_t, int64_t);
static thunder_fn_t orig_THUNDER = nullptr;
static void *orig_ANOGS_RET0 = nullptr;

static uintptr_t getLibLoadBias(const char *name) {
    FILE *f = fopen("/proc/self/maps", "r");
    if (!f) {
        return 0;
    }
    char line[512];
    uintptr_t min_start = 0;
    int found = 0;
    while (fgets(line, sizeof(line), f)) {
        if (!strstr(line, name)) {
            continue;
        }
        uintptr_t seg_start = 0;
        if (sscanf(line, "%lx-", &seg_start) != 1 || seg_start == 0) {
            continue;
        }
        if (!found || seg_start < min_start) {
            min_start = seg_start;
            found = 1;
        }
    }
    fclose(f);
    return found ? min_start : 0;
}

static bool isRxMapped(const char *name) {
    FILE *f = fopen("/proc/self/maps", "r");
    if (!f) {
        return false;
    }
    char line[512];
    bool rx = false;
    while (fgets(line, sizeof(line), f)) {
        if (strstr(line, name) && (strstr(line, "r-xp") || strstr(line, "r-xs"))) {
            rx = true;
            break;
        }
    }
    fclose(f);
    return rx;
}

static bool isExecAddr(uintptr_t addr) {
    FILE *f = fopen("/proc/self/maps", "r");
    if (!f) {
        return false;
    }
    char line[512];
    bool ok = false;
    while (fgets(line, sizeof(line), f)) {
        uintptr_t start = 0, end = 0;
        if (sscanf(line, "%lx-%lx", &start, &end) != 2) {
            continue;
        }
        if (addr >= start && addr < end && strstr(line, "r-x")) {
            ok = true;
            break;
        }
    }
    fclose(f);
    return ok;
}

static int64_t THUNDER(int64_t a1, int64_t /*a2*/, int64_t /*a3*/) {
    if ((rand_r(&rs) % 100) < 5) {
        int delay_ms = 20 + (rand_r(&rs) % 11);
        usleep((useconds_t) delay_ms * 1000);
    }
    return a1;
}

/** libanogs+0x37F2CC — replace host RET patch with inline hook return 0. */
static int64_t ANOGS_RET0(int64_t /*a1*/, int64_t /*a2*/, int64_t /*a3*/) {
    return 0;
}

static bool installOneHook(uintptr_t base, long offset, void *replace, void **orig,
                           volatile int *done_flag, const char *name) {
    if (*done_flag) {
        return true;
    }
    void *target = reinterpret_cast<void *>(base + offset);
    if (!isExecAddr(reinterpret_cast<uintptr_t>(target))) {
        __android_log_print(ANDROID_LOG_WARN, TAG, "%s target not exec base=%p off=0x%lx",
                            name, (void *) base, (unsigned long) offset);
        return false;
    }
    int rc = DobbyHook(target, replace, orig);
    if (rc == 0) {
        *done_flag = 1;
        __android_log_print(ANDROID_LOG_INFO, TAG, "inline HOOK ok %s libanogs+0x%lx base=%p target=%p",
                            name, (unsigned long) offset, (void *) base, target);
        return true;
    }
    __android_log_print(ANDROID_LOG_ERROR, TAG, "DobbyHook %s failed rc=%d target=%p", name, rc, target);
    return false;
}

static bool installHooksAtBase(uintptr_t base) {
    if (base == 0) {
        return false;
    }
    pthread_mutex_lock(&s_hook_mu);
    dobby_enable_near_branch_trampoline();
    bool ret0 = installOneHook(base, ANOGS_RET0_OFFSET, reinterpret_cast<void *>(ANOGS_RET0),
                               &orig_ANOGS_RET0, &s_ret0_done, "RET0");
    bool thunder = installOneHook(base, THUNDER_OFFSET, reinterpret_cast<void *>(THUNDER),
                                  reinterpret_cast<void **>(&orig_THUNDER), &s_thunder_done, "THUNDER");
    pthread_mutex_unlock(&s_hook_mu);
    return ret0 && thunder;
}

static void *thunderHookThread(void *) {
    rs = (unsigned) time(NULL) ^ (unsigned) getpid();

    for (int i = 0; i < 600 && !(s_thunder_done && s_ret0_done); i++) {
        if (!isRxMapped("libanogs.so")) {
            usleep(50000);
            continue;
        }
        uintptr_t base = getLibLoadBias("libanogs.so");
        if (base != 0 && installHooksAtBase(base)) {
            return nullptr;
        }
        usleep(50000);
    }

    if (!s_ret0_done || !s_thunder_done) {
        __android_log_print(ANDROID_LOG_WARN, TAG,
                            "libanogs hook timeout ret0=%d thunder=%d", s_ret0_done, s_thunder_done);
    }
    return nullptr;
}

bool anogs_hooks_installed() {
    return s_ret0_done != 0 && s_thunder_done != 0;
}

void anogs_install_thunder_hook() {
    if (s_thunder_done && s_ret0_done) {
        return;
    }
    pthread_t t;
    pthread_create(&t, nullptr, thunderHookThread, nullptr);
    pthread_detach(t);
}
