//
// Created by Milk on 4/9/21.
//
#include "oxorany.h"

#include "BoxCore.h"
#include "Log.h"
#include "IO.h"
#include <jni.h>
#include <JniHook/JniHook.h>
#include <Hook/VMClassLoaderHook.h>
#include <Hook/UnixFileSystemHook.h>
#include <Hook/BinderHook.h>
#include <Hook/DexFileHook.h>
#include <Hook/RuntimeHook.h>
#include <Hook/SystemPropertiesHook.h>
#include <Hook/LogScrubHook.h>
#include "Utils/HexDump.h"
#include "Utils/MemfdElfLoader.h"
#include "FarlightEspSocketServer.h"
#include "DeltaForceEspSocketServer.h"
#include "esp_protocol/farlight_esp_protocol.h"
#include "esp_protocol/delta_esp_protocol.h"
#include "hidden_api.h"
#include "block-anogs.hpp"

extern void anogs_install_thunder_hook();
extern bool anogs_hooks_installed();

#include <errno.h>
#include <string>
#include <vector>
#include <thread>
#include <stdlib.h>
#include <cstring>
#include <dlfcn.h>
#include <mutex>
#include <sys/mman.h>
#include <unistd.h>
#include <fcntl.h>

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOG_TAG "BthreadMain"

struct {
    JavaVM *vm;
    jclass NativeCoreClass;
    jmethodID getCallingUidId;
    jmethodID redirectPathString;
    jmethodID reversePathString;
    jmethodID redirectPathFile;
    jmethodID loadEmptyDex;
    jmethodID loadEmptyDexL;
    jmethodID spoofSystemPropertyId;
    int api_level;
} VMEnv;


JNIEnv *getEnv() {
    JNIEnv *env;
    VMEnv.vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6);
    return env;
}

JNIEnv *ensureEnvCreated() {
    JNIEnv *env = getEnv();
    if (env == NULL) {
        VMEnv.vm->AttachCurrentThread(&env, NULL);
    }
    return env;
}
/*
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_anubis_loader_AnubisCore_BthreadMain(JNIEnv *env, jobject thiz) {
    LOGE("Native BthreadMain called");
    const char* key = "anubis_license_activated";
    if (strcmp(key, "anubis_license_activated") == 0) {
        return JNI_TRUE;
    } else {
        abort(); // crash
        return JNI_FALSE;
    }
}
*/

int BoxCore::getCallingUid(JNIEnv *env, int orig) {
    env = ensureEnvCreated();
    return env->CallStaticIntMethod(VMEnv.NativeCoreClass, VMEnv.getCallingUidId, orig);
}

jstring BoxCore::redirectPathString(JNIEnv *env, jstring path) {
    env = ensureEnvCreated();
    return (jstring) env->CallStaticObjectMethod(VMEnv.NativeCoreClass, VMEnv.redirectPathString, path);
}

jobject BoxCore::redirectPathFile(JNIEnv *env, jobject path) {
    env = ensureEnvCreated();
    return env->CallStaticObjectMethod(VMEnv.NativeCoreClass, VMEnv.redirectPathFile, path);
}

jstring BoxCore::reversePathString(JNIEnv *env, jstring path) {
    env = ensureEnvCreated();
    if (VMEnv.reversePathString == nullptr) {
        return path;
    }
    return (jstring) env->CallStaticObjectMethod(VMEnv.NativeCoreClass, VMEnv.reversePathString, path);
}

bool BoxCore::isPathScrubJniReady() {
    return VMEnv.NativeCoreClass != nullptr && VMEnv.reversePathString != nullptr;
}

jlongArray BoxCore::loadEmptyDex(JNIEnv *env) {
    env = ensureEnvCreated();
    return (jlongArray) env->CallStaticObjectMethod(VMEnv.NativeCoreClass, VMEnv.loadEmptyDex);
}

int BoxCore::getApiLevel() {
    return VMEnv.api_level;
}

JavaVM *BoxCore::getJavaVM() {
    return VMEnv.vm;
}

void nativeHook(JNIEnv *env) {
    BaseHook::init(env);
    UnixFileSystemHook::init(env);
    VMClassLoaderHook::init(env);
//    RuntimeHook::init(env);
    BinderHook::init(env);
    DexFileHook::init(env);
    SystemPropertiesHook::init(env);
    LogScrubHook::init();
}

void hideXposed(JNIEnv *env, jclass clazz) {
    ALOGD("set hideXposed");
    VMClassLoaderHook::hideXposed();
}

void initNative(JNIEnv *env, jclass clazz, jint api_level, jclass jni_hook_class,
                jclass method_utils_class) {
    ALOGD("NativeCore init.");
    VMEnv.api_level = api_level;
    VMEnv.NativeCoreClass = (jclass) env->NewGlobalRef(env->FindClass(VMCORE_CLASS));
    VMEnv.getCallingUidId = env->GetStaticMethodID(VMEnv.NativeCoreClass, "getCallingUid", "(I)I");
    VMEnv.redirectPathString = env->GetStaticMethodID(VMEnv.NativeCoreClass, "redirectPath","(Ljava/lang/String;)Ljava/lang/String;");
    VMEnv.reversePathString = env->GetStaticMethodID(VMEnv.NativeCoreClass, "reversePath","(Ljava/lang/String;)Ljava/lang/String;");
    VMEnv.redirectPathFile = env->GetStaticMethodID(VMEnv.NativeCoreClass, "redirectPath","(Ljava/io/File;)Ljava/io/File;");
    VMEnv.spoofSystemPropertyId = env->GetStaticMethodID(
            VMEnv.NativeCoreClass, "spoofSystemProperty",
            "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;");
    JniHook::InitJniHook(env, api_level, jni_hook_class, method_utils_class);
}

jclass BoxCore::getNativeCoreClass() {
    return VMEnv.NativeCoreClass;
}

jmethodID BoxCore::getSpoofSystemPropertyId() {
    return VMEnv.spoofSystemPropertyId;
}

void addIORule(JNIEnv *env, jclass clazz, jstring target_path,jstring relocate_path) {
    ALOGD("set addIORule");
    IO::addRule(env->GetStringUTFChars(target_path, JNI_FALSE),env->GetStringUTFChars(relocate_path, JNI_FALSE));
}

void enableIO(JNIEnv *env, jclass clazz) {
    ALOGD("set enableIO");
    IO::init(env);
    nativeHook(env);
}

bool disableHiddenApi(JNIEnv *env, jclass clazz) {
    ALOGD("set disableHiddenApi");
    if(!disable_hidden_api(env)){
        ALOGD("set disableHiddenApi Fail!!!");
        return false;
    }
    return true;
}

/*

jstring ActivateSdkLog(JNIEnv *env, jclass clazz) {
    const char* url = oxorany("https://Zlibs.shop/BlackZen/connect.php");
    return env->NewStringUTF(url);
}
*/


// new cpp code 
#define SECMAGIC 0xdeadbeef

#if defined(__aarch64__) // 64-bit architecture
uint64_t OriSyscall(uint64_t num, uint64_t SYSARG_1, uint64_t SYSARG_2, uint64_t SYSARG_3,uint64_t SYSARG_4, uint64_t SYSARG_5, uint64_t SYSARG_6) {
    uint64_t x0;
    __asm__ volatile ( "mov x8, %1\n\t" "mov x0, %2\n\t" "mov x1, %3\n\t" "mov x2, %4\n\t" "mov x3, %5\n\t" "mov x4, %6\n\t" "mov x5, %7\n\t" "svc #0\n\t" "mov %0, x0\n\t" :"=r"(x0) :"r"(num), "r"(SYSARG_1), "r"(SYSARG_2), "r"(SYSARG_3), "r"(SYSARG_4), "r"(SYSARG_5), "r"(SYSARG_6) :"x8", "x0", "x1", "x2", "x3", "x4", "x4", "x5" );
    return x0;
}
#elif defined(__arm__) // 32-bit architecture
uint32_t OriSyscall(uint32_t num, uint32_t SYSARG_1, uint32_t SYSARG_2, uint32_t SYSARG_3,uint32_t SYSARG_4, uint32_t SYSARG_5, uint32_t SYSARG_6) {
    uint32_t x0;
    __asm__ volatile ( "mov r7, %1\n\t" "mov r0, %2\n\t" "mov r1, %3\n\t" "mov r2, %4\n\t" "mov r3, %5\n\t" "mov r4, %6\n\t" "mov r5, %7\n\t" "svc #0\n\t" "mov %0, r0\n\t" :"=r"(x0) :"r"(num), "r"(SYSARG_1), "r"(SYSARG_2), "r"(SYSARG_3), "r"(SYSARG_4), "r"(SYSARG_5), "r"(SYSARG_6) :"r7", "r0", "r1", "r2", "r3", "r4", "r5" );
    return x0;
}
#else
#error "Unsupported architecture"
#endif

void sig_callback(int signo, siginfo_t *info, void *data){
    int my_signo = info->si_signo;
    unsigned long syscall_number;
    unsigned long SYSARG_1, SYSARG_2, SYSARG_3, SYSARG_4, SYSARG_5, SYSARG_6;
#if defined(__aarch64__)
    syscall_number = ((ucontext_t *) data)->uc_mcontext.regs[8];
    SYSARG_1 = ((ucontext_t *) data)->uc_mcontext.regs[0];
    SYSARG_2 = ((ucontext_t *) data)->uc_mcontext.regs[1];
    SYSARG_3 = ((ucontext_t *) data)->uc_mcontext.regs[2];
    SYSARG_4 = ((ucontext_t *) data)->uc_mcontext.regs[3];
    SYSARG_5 = ((ucontext_t *) data)->uc_mcontext.regs[4];
    SYSARG_6 = ((ucontext_t *) data)->uc_mcontext.regs[5];
#elif defined(__arm__)
    syscall_number = ((ucontext_t *) data)->uc_mcontext.arm_r7;
    SYSARG_1 = ((ucontext_t *) data)->uc_mcontext.arm_r0;
    SYSARG_2 = ((ucontext_t *) data)->uc_mcontext.arm_r1;
    SYSARG_3 = ((ucontext_t *) data)->uc_mcontext.arm_r2;
    SYSARG_4 = ((ucontext_t *) data)->uc_mcontext.arm_r3;
    SYSARG_5 = ((ucontext_t *) data)->uc_mcontext.arm_r4;
    SYSARG_6 = ((ucontext_t *) data)->uc_mcontext.arm_r5;
#else
#error "Unsupported architecture"
#endif
    switch (syscall_number) {
        case __NR_openat:{
            int fd = (int) SYSARG_1;
            const char *pathname = (const char *) SYSARG_2;
            int flags = (int) SYSARG_3;
            int mode = (int) SYSARG_4;
            const char *open_path = pathname;
            const char *redirected = nullptr;
            // Only redirect /proc reads — full IO::redirectPath on game paths breaks UE4 I/O.
            if (pathname != nullptr && strncmp(pathname, "/proc/", 6) == 0) {
                redirected = IO::redirectPath(pathname);
                if (redirected != nullptr) {
                    open_path = redirected;
                }
            }
#if defined(__aarch64__)
            long ret = OriSyscall(__NR_openat, fd, (uint64_t) open_path, flags, mode, SECMAGIC, SECMAGIC);
            ((ucontext_t *) data)->uc_mcontext.regs[0] = (uint64_t) ret;
#elif defined(__arm__)
            long ret = OriSyscall(__NR_openat, fd, (uint32_t) open_path, flags, mode, SECMAGIC, SECMAGIC);
            ((ucontext_t *) data)->uc_mcontext.arm_r0 = (uint32_t) ret;
#endif
            if (redirected != nullptr && redirected != pathname) {
                free((void *) redirected);
            }
            break;
        }
        default: {
            // Should not trap — re-execute if it happens.
#if defined(__aarch64__)
            long ret = OriSyscall(syscall_number, SYSARG_1, SYSARG_2, SYSARG_3, SYSARG_4, SYSARG_5, SYSARG_6);
            ((ucontext_t *) data)->uc_mcontext.regs[0] = (uint64_t) ret;
#elif defined(__arm__)
            long ret = OriSyscall(syscall_number, SYSARG_1, SYSARG_2, SYSARG_3, SYSARG_4, SYSARG_5, SYSARG_6);
            ((ucontext_t *) data)->uc_mcontext.arm_r0 = (uint32_t) ret;
#endif
            break;
        }
    }
}

void init_seccomp(JNIEnv *env, jclass clazz) {
    struct sock_filter filter[] = {BPF_STMT(BPF_LD | BPF_W | BPF_ABS, offsetof(struct seccomp_data, nr)),BPF_JUMP(BPF_JMP | BPF_JEQ | BPF_K, __NR_openat, 0, 2),BPF_STMT(BPF_LD | BPF_W | BPF_ABS, offsetof(struct seccomp_data, args[4])),BPF_JUMP(BPF_JMP | BPF_JEQ | BPF_K, SECMAGIC, 0, 1),BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_ALLOW),BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_TRAP)};

    struct sock_fprog prog;
    prog.filter = filter;
    prog.len = (unsigned short) (sizeof(filter) / sizeof(filter[0]));

    struct sigaction sa;
    sigset_t sigset;
    sigfillset(&sigset);
    sa.sa_sigaction = sig_callback;
    sa.sa_mask = sigset;
    sa.sa_flags = SA_SIGINFO;

    if (sigaction(SIGSYS, &sa, NULL) == -1) {
        return;
    }
    if (prctl(PR_SET_NO_NEW_PRIVS, 1, 0, 0, 0) == -1) {
        return;
    }
    if (prctl(PR_SET_SECCOMP, SECCOMP_MODE_FILTER, &prog) == -1) {
        return;
    }
    ALOGE("InitCvmSeccomp Successes");
}

static void nukeTargetLibrary(JNIEnv* env, jclass /* clazz */, jstring libName, jint layerMask) {
    const char* utf = env->GetStringUTFChars(libName, nullptr);
    if (utf == nullptr) {
        return;
    }
    std::string name(utf);
    env->ReleaseStringUTFChars(libName, utf);
    uint32_t mask = layerMask < 0 ? NUKE_LAYERS_ALL : static_cast<uint32_t>(layerMask);
    std::thread([name, mask]() { nukeLibrary(name, mask); }).detach();
}

static void installAnogsThunderHook(JNIEnv *, jclass) {
    anogs_install_thunder_hook();
}

static jboolean areAnogsHooksInstalled(JNIEnv *, jclass) {
    return anogs_hooks_installed() ? JNI_TRUE : JNI_FALSE;
}

static jlong getMappedLibraryBase(JNIEnv *env, jclass, jstring libName) {
    if (libName == nullptr) {
        return 0;
    }
    const char *utf = env->GetStringUTFChars(libName, nullptr);
    if (utf == nullptr) {
        return 0;
    }
    std::string name(utf);
    env->ReleaseStringUTFChars(libName, utf);
    void *base = nullptr;
    size_t size = 0;
    getLibraryInfo(name, &base, &size);
    return reinterpret_cast<jlong>(base);
}

static jboolean memfdLoadElf(JNIEnv *env, jclass, jbyteArray jElf);
static jboolean memfdLoadHookElf(JNIEnv *env, jclass, jbyteArray jElf);

static void setSuppressNativeLog(JNIEnv *, jclass, jboolean suppress) {
    setNativeLogSuppressed(suppress == JNI_TRUE);
}

static void setLogScrubConfig(JNIEnv *env, jclass, jboolean enabled, jstring hostPkg, jstring guestPkg) {
    const char *host = hostPkg != nullptr ? env->GetStringUTFChars(hostPkg, nullptr) : "";
    const char *guest = guestPkg != nullptr ? env->GetStringUTFChars(guestPkg, nullptr) : "";
    LogScrubHook::setConfig(enabled == JNI_TRUE, host, guest);
    if (hostPkg != nullptr) {
        env->ReleaseStringUTFChars(hostPkg, host);
    }
    if (guestPkg != nullptr) {
        env->ReleaseStringUTFChars(guestPkg, guest);
    }
}

static std::string readProcFile(const char *path) {
    if (path == nullptr) {
        return {};
    }
    int fd = openat(AT_FDCWD, path, O_RDONLY, 0);
    if (fd < 0) {
        return {};
    }
    std::string out;
    char buf[4096];
    ssize_t n;
    while ((n = read(fd, buf, sizeof(buf))) > 0) {
        out.append(buf, static_cast<size_t>(n));
    }
    close(fd);
    return out;
}

static jstring readRealProcSelfMaps(JNIEnv *env, jclass) {
    std::string maps = readProcFile("/proc/self/maps");
    if (maps.empty()) {
        return env->NewStringUTF("");
    }
    return env->NewStringUTF(maps.c_str());
}

static bool memoryRangeReadable(const void *addr, size_t len) {
    if (addr == nullptr || len == 0) {
        return false;
    }
    const long pageSize = sysconf(_SC_PAGESIZE);
    if (pageSize <= 0) {
        return false;
    }
    uintptr_t start = reinterpret_cast<uintptr_t>(addr);
    uintptr_t end = start + len;
    for (uintptr_t page = start & ~(static_cast<uintptr_t>(pageSize) - 1);
         page < end;
         page += static_cast<uintptr_t>(pageSize)) {
        unsigned char vec = 0;
        if (mincore(reinterpret_cast<void *>(page),
                    static_cast<size_t>(pageSize),
                    &vec) != 0) {
            return false;
        }
    }
    return true;
}

static bool patchExecBytes(void *addr, const uint8_t *patch, size_t patchLen,
                           const uint8_t *expected, size_t expectedLen) {
    if (addr == nullptr || patch == nullptr || patchLen == 0) {
        return false;
    }
    size_t probeLen = patchLen;
    if (expected != nullptr && expectedLen > probeLen) {
        probeLen = expectedLen;
    }
    if (!memoryRangeReadable(addr, probeLen)) {
        return false;
    }
    if (memcmp(addr, patch, patchLen) == 0) {
        return true;
    }
    const long pageSize = sysconf(_SC_PAGESIZE);
    if (pageSize <= 0) {
        return false;
    }
    uintptr_t start = reinterpret_cast<uintptr_t>(addr);
    uintptr_t page = start & ~(static_cast<uintptr_t>(pageSize) - 1);
    size_t span = static_cast<size_t>(start - page) + patchLen;
    span = (span + static_cast<size_t>(pageSize) - 1)
            & ~(static_cast<size_t>(pageSize) - 1);

    if (mprotect(reinterpret_cast<void *>(page), span, PROT_READ | PROT_WRITE | PROT_EXEC) != 0) {
        return false;
    }

    bool ok = false;
    if (expected != nullptr && expectedLen > 0) {
        if (memcmp(addr, expected, expectedLen) != 0 && memcmp(addr, patch, patchLen) != 0) {
            mprotect(reinterpret_cast<void *>(page), span, PROT_READ | PROT_EXEC);
            return false;
        }
    }

    memcpy(addr, patch, patchLen);
    __builtin___clear_cache(reinterpret_cast<char *>(addr),
                            reinterpret_cast<char *>(addr) + patchLen);
    ok = memcmp(addr, patch, patchLen) == 0;
    mprotect(reinterpret_cast<void *>(page), span, PROT_READ | PROT_EXEC);
    return ok;
}

static void *findLibraryBaseBypass(const std::string &name, size_t *outSize) {
    std::string maps = readProcFile("/proc/self/maps");
    if (maps.empty()) {
        return nullptr;
    }
    unsigned long minStart = 0;
    unsigned long maxEnd = 0;
    bool found = false;
    size_t pos = 0;
    while (pos < maps.size()) {
        size_t end = maps.find('\n', pos);
        if (end == std::string::npos) {
            end = maps.size();
        }
        std::string line = maps.substr(pos, end - pos);
        pos = end + 1;
        if (line.find(name) == std::string::npos) {
            continue;
        }
        unsigned long segStart = strtoul(line.c_str(), nullptr, 16);
        size_t dash = line.find('-');
        if (dash == std::string::npos) {
            continue;
        }
        unsigned long segEnd = strtoul(line.substr(dash + 1).c_str(), nullptr, 16);
        if (!found) {
            minStart = segStart;
            maxEnd = segEnd;
            found = true;
        } else {
            if (segStart < minStart) {
                minStart = segStart;
            }
            if (segEnd > maxEnd) {
                maxEnd = segEnd;
            }
        }
    }
    if (!found) {
        return nullptr;
    }
    if (outSize != nullptr) {
        *outSize = static_cast<size_t>(maxEnd - minStart);
    }
    return reinterpret_cast<void *>(minStart);
}

static jboolean patchMappedLibraryRva(JNIEnv *env, jclass, jstring libName, jlong rva,
                                    jbyteArray patchArr, jbyteArray expectedArr) {
    if (libName == nullptr || patchArr == nullptr || rva < 0) {
        return JNI_FALSE;
    }
    const char *utf = env->GetStringUTFChars(libName, nullptr);
    if (utf == nullptr) {
        return JNI_FALSE;
    }
    std::string name(utf);
    env->ReleaseStringUTFChars(libName, utf);

    size_t size = 0;
    void *base = findLibraryBaseBypass(name, &size);
    if (base == nullptr) {
        return JNI_FALSE;
    }

    jsize patchLen = env->GetArrayLength(patchArr);
    if (patchLen <= 0) {
        return JNI_FALSE;
    }
    jbyte *patchBytes = env->GetByteArrayElements(patchArr, nullptr);
    if (patchBytes == nullptr) {
        return JNI_FALSE;
    }

    const uint8_t *expected = nullptr;
    jsize expectedLen = 0;
    jbyte *expectedBytes = nullptr;
    if (expectedArr != nullptr) {
        expectedLen = env->GetArrayLength(expectedArr);
        if (expectedLen > 0) {
            expectedBytes = env->GetByteArrayElements(expectedArr, nullptr);
            if (expectedBytes != nullptr) {
                expected = reinterpret_cast<const uint8_t *>(expectedBytes);
            }
        }
    }

    void *target = reinterpret_cast<void *>(reinterpret_cast<uintptr_t>(base)
            + static_cast<uintptr_t>(rva));
    bool ok = patchExecBytes(target,
                             reinterpret_cast<const uint8_t *>(patchBytes),
                             static_cast<size_t>(patchLen),
                             expected,
                             static_cast<size_t>(expectedLen));

    if (expectedBytes != nullptr) {
        env->ReleaseByteArrayElements(expectedArr, expectedBytes, JNI_ABORT);
    }
    env->ReleaseByteArrayElements(patchArr, patchBytes, JNI_ABORT);
    return ok ? JNI_TRUE : JNI_FALSE;
}

static jboolean patchAbsoluteAddress(JNIEnv *env, jclass, jlong address,
                                     jbyteArray patchArr, jbyteArray expectedArr) {
    if (address <= 0 || patchArr == nullptr) {
        return JNI_FALSE;
    }
    jsize patchLen = env->GetArrayLength(patchArr);
    if (patchLen <= 0) {
        return JNI_FALSE;
    }
    jbyte *patchBytes = env->GetByteArrayElements(patchArr, nullptr);
    if (patchBytes == nullptr) {
        return JNI_FALSE;
    }

    const uint8_t *expected = nullptr;
    jsize expectedLen = 0;
    jbyte *expectedBytes = nullptr;
    if (expectedArr != nullptr) {
        expectedLen = env->GetArrayLength(expectedArr);
        if (expectedLen > 0) {
            expectedBytes = env->GetByteArrayElements(expectedArr, nullptr);
            if (expectedBytes != nullptr) {
                expected = reinterpret_cast<const uint8_t *>(expectedBytes);
            }
        }
    }

    void *target = reinterpret_cast<void *>(static_cast<uintptr_t>(address));
    bool ok = patchExecBytes(target,
                             reinterpret_cast<const uint8_t *>(patchBytes),
                             static_cast<size_t>(patchLen),
                             expected,
                             static_cast<size_t>(expectedLen));

    if (expectedBytes != nullptr) {
        env->ReleaseByteArrayElements(expectedArr, expectedBytes, JNI_ABORT);
    }
    env->ReleaseByteArrayElements(patchArr, patchBytes, JNI_ABORT);
    return ok ? JNI_TRUE : JNI_FALSE;
}

struct FarlightEspEntryPod {
    float x;
    float top;
    float bottom;
    float w;
    float middle;
    float health;
    float teamId;
    float distance;
};

static jboolean memfdLoadElf(JNIEnv *env, jclass, jbyteArray jElf) {
    if (jElf == nullptr) {
        return JNI_FALSE;
    }
    jsize len = env->GetArrayLength(jElf);
    if (len < 1024) {
        return JNI_FALSE;
    }
    jbyte *bytes = env->GetByteArrayElements(jElf, nullptr);
    if (bytes == nullptr) {
        return JNI_FALSE;
    }
    FarlightEspServerEnsure();
    bool deltaListen = DeltaForceEspServerEnsure();
    bool ok = hasad_load_elf_from_memory(bytes, static_cast<size_t>(len));
    env->ReleaseByteArrayElements(jElf, bytes, JNI_ABORT);
    if (!ok) {
        return JNI_FALSE;
    }
    if (!deltaListen) {
        deltaListen = DeltaForceEspServerEnsure();
    }
    __android_log_print(ANDROID_LOG_INFO, "FarlightEsp",
                        "memfd ELF loaded handle=%p deltaListen=%d",
                        hasad_memfd_last_handle(), deltaListen ? 1 : 0);
    return JNI_TRUE;
}

static jboolean memfdLoadHookElf(JNIEnv *env, jclass, jbyteArray jElf) {
    if (jElf == nullptr) {
        return JNI_FALSE;
    }
    jsize len = env->GetArrayLength(jElf);
    if (len < 1024) {
        return JNI_FALSE;
    }
    jbyte *bytes = env->GetByteArrayElements(jElf, nullptr);
    if (bytes == nullptr) {
        return JNI_FALSE;
    }
    bool ok = hasad_load_elf_from_memory(bytes, static_cast<size_t>(len));
    env->ReleaseByteArrayElements(jElf, bytes, JNI_ABORT);
    if (!ok) {
        return JNI_FALSE;
    }
    __android_log_print(ANDROID_LOG_INFO, "GuestLogin",
                        "memfd guest hook loaded handle=%p", hasad_memfd_last_handle());
    return JNI_TRUE;
}

static jfloatArray buildEspFloatArray(JNIEnv *env, int count, const FarlightEspEntryPod *buffer) {
    jfloat zero = 0.f;
    if (count <= 0 || buffer == nullptr) {
        jfloatArray arr = env->NewFloatArray(1);
        env->SetFloatArrayRegion(arr, 0, 1, &zero);
        return arr;
    }
    constexpr int kFields = 8;
    jfloatArray arr = env->NewFloatArray(1 + count * kFields);
    jfloat header = static_cast<jfloat>(count);
    env->SetFloatArrayRegion(arr, 0, 1, &header);

    jfloat flat[64 * kFields];
    for (int i = 0; i < count; ++i) {
        const int base = i * kFields;
        flat[base + 0] = buffer[i].x;
        flat[base + 1] = buffer[i].top;
        flat[base + 2] = buffer[i].bottom;
        flat[base + 3] = buffer[i].w;
        flat[base + 4] = buffer[i].middle;
        flat[base + 5] = buffer[i].health;
        flat[base + 6] = buffer[i].teamId;
        flat[base + 7] = buffer[i].distance;
    }
    env->SetFloatArrayRegion(arr, 1, count * kFields, flat);
    return arr;
}

static jfloatArray pollFarlightEspFrames(JNIEnv *env, jclass) {
    FarlightEspServerEnsure();
    FarlightEspEntryPod buffer[64];
    int count = 0;
    FarlightEspServerFetch(0, 0, reinterpret_cast<FarlightEspEntry *>(buffer), 64, &count);

    static int sLogCounter;
    if ((++sLogCounter % 300) == 1) {
        __android_log_print(ANDROID_LOG_INFO, "FarlightEsp",
                            "jni poll count=%d listen=%d client=%d memfd=%p",
                            count,
                            FarlightEspServerListening() ? 1 : 0,
                            FarlightEspServerClientConnected() ? 1 : 0,
                            hasad_memfd_last_handle());
    }
    return buildEspFloatArray(env, count, buffer);
}

static jintArray getFarlightEspBridgeStatus(JNIEnv *env, jclass) {
    jint status[4];
    status[0] = FarlightEspServerLastCount();
    status[1] = FarlightEspServerClientConnected() ? 1 : 0;
    status[2] = FarlightEspServerListening() ? 1 : 0;
    status[3] = hasad_memfd_last_handle() != nullptr ? 1 : 0;
    jintArray arr = env->NewIntArray(4);
    env->SetIntArrayRegion(arr, 0, 4, status);
    return arr;
}

static void farlightEspStartReader(JNIEnv *, jclass) {
    FarlightEspServerEnsure();
}

static void farlightEspSetScreen(JNIEnv *, jclass, jint width, jint height) {
    FarlightEspServerSetScreen(width, height);
}

static jfloatArray farlightEspPoll(JNIEnv *env, jclass) {
    return pollFarlightEspFrames(env, nullptr);
}

static jboolean registerFarlightEspNatives(JNIEnv *env, jclass, jclass bridgeClass) {
    if (!FarlightEspServerEnsure()) {
        ALOGE("registerFarlightEspNatives: socket server failed");
        return JNI_FALSE;
    }
    if (bridgeClass == nullptr) {
        ALOGE("registerFarlightEspNatives: bridge class null");
        return JNI_FALSE;
    }
    if (hasad_memfd_last_handle() == nullptr) {
        ALOGE("registerFarlightEspNatives: no memfd handle");
        return JNI_FALSE;
    }

    JNINativeMethod methods[] = {
            {"nativeStartReader", "()V", reinterpret_cast<void *>(farlightEspStartReader)},
            {"nativeSetScreen", "(II)V", reinterpret_cast<void *>(farlightEspSetScreen)},
            {"nativePoll", "()[F", reinterpret_cast<void *>(farlightEspPoll)},
    };
    if (env->RegisterNatives(bridgeClass, methods, sizeof(methods) / sizeof(methods[0])) < 0) {
        ALOGE("registerFarlightEspNatives: RegisterNatives failed");
        return JNI_FALSE;
    }
    ALOGD("registerFarlightEspNatives ok handle=%p bridge=%p",
          hasad_memfd_last_handle(), bridgeClass);
    return JNI_TRUE;
}

struct DeltaEspEntryPod {
    float x;
    float top;
    float bottom;
    float w;
    float middle;
    float health;
    float teamId;
    float distance;
    float isBot;
    float headScreenX;
    float headScreenY;
    float nameChars[DELTA_MAX_NAME_CHARS];
    float boneSegs[DELTA_MAX_BONE_SEGS * 4];
};

static jfloatArray buildDeltaEspFloatArray(JNIEnv *env, int count, const DeltaEspEntryPod *buffer) {
    jfloat zero = 0.f;
    if (count <= 0 || buffer == nullptr) {
        jfloatArray arr = env->NewFloatArray(1);
        env->SetFloatArrayRegion(arr, 0, 1, &zero);
        return arr;
    }
    constexpr int kFields = 8 + 1 + 2 + DELTA_MAX_NAME_CHARS + DELTA_MAX_BONE_SEGS * 4;
    jfloatArray arr = env->NewFloatArray(1 + count * kFields);
    jfloat header = static_cast<jfloat>(count);
    env->SetFloatArrayRegion(arr, 0, 1, &header);

    std::vector<jfloat> flat(static_cast<size_t>(count) * kFields);
    for (int i = 0; i < count; ++i) {
        const int base = i * kFields;
        flat[base + 0] = buffer[i].x;
        flat[base + 1] = buffer[i].top;
        flat[base + 2] = buffer[i].bottom;
        flat[base + 3] = buffer[i].w;
        flat[base + 4] = buffer[i].middle;
        flat[base + 5] = buffer[i].health;
        flat[base + 6] = buffer[i].teamId;
        flat[base + 7] = buffer[i].distance;
        flat[base + 8] = buffer[i].isBot;
        flat[base + 9] = buffer[i].headScreenX;
        flat[base + 10] = buffer[i].headScreenY;
        for (int n = 0; n < DELTA_MAX_NAME_CHARS; ++n) {
            flat[base + 11 + n] = buffer[i].nameChars[n];
        }
        for (int b = 0; b < DELTA_MAX_BONE_SEGS * 4; ++b) {
            flat[base + 11 + DELTA_MAX_NAME_CHARS + b] = buffer[i].boneSegs[b];
        }
    }
    env->SetFloatArrayRegion(arr, 1, count * kFields, flat.data());
    return arr;
}

static jboolean ensureDeltaForceEspServer(JNIEnv *, jclass) {
    return DeltaForceEspServerEnsure() ? JNI_TRUE : JNI_FALSE;
}

static jfloatArray pollDeltaForceEspFrames(JNIEnv *env, jclass) {
    DeltaForceEspServerEnsure();
    DeltaEspEntryPod buffer[64];
    int count = 0;
    DeltaForceEspServerFetch(0, 0, reinterpret_cast<DeltaEspEntry *>(buffer), 64, &count);

    static int sLogCounter;
    if ((++sLogCounter % 300) == 1) {
        __android_log_print(ANDROID_LOG_INFO, "DeltaForceEsp",
                            "jni poll count=%d listen=%d client=%d memfd=%p",
                            count,
                            DeltaForceEspServerListening() ? 1 : 0,
                            DeltaForceEspServerClientConnected() ? 1 : 0,
                            hasad_memfd_last_handle());
    }
    return buildDeltaEspFloatArray(env, count, buffer);
}

static jintArray getDeltaForceEspBridgeStatus(JNIEnv *env, jclass) {
    jint status[4];
    status[0] = DeltaForceEspServerLastCount();
    status[1] = DeltaForceEspServerClientConnected() ? 1 : 0;
    status[2] = DeltaForceEspServerListening() ? 1 : 0;
    status[3] = hasad_memfd_last_handle() != nullptr ? 1 : 0;
    jintArray arr = env->NewIntArray(4);
    env->SetIntArrayRegion(arr, 0, 4, status);
    return arr;
}

static void deltaForceEspStartReader(JNIEnv *, jclass) {
    DeltaForceEspServerEnsure();
}

static void deltaForceEspSetScreen(JNIEnv *, jclass, jint width, jint height) {
    DeltaForceEspServerSetScreen(width, height);
}

static jfloatArray deltaForceEspPoll(JNIEnv *env, jclass) {
    return pollDeltaForceEspFrames(env, nullptr);
}

static jboolean registerDeltaForceEspNatives(JNIEnv *env, jclass, jclass bridgeClass) {
    if (!DeltaForceEspServerEnsure()) {
        ALOGE("registerDeltaForceEspNatives: socket server failed errno=%d", errno);
        return JNI_FALSE;
    }
    if (bridgeClass == nullptr) {
        ALOGE("registerDeltaForceEspNatives: bridge class null");
        return JNI_FALSE;
    }
    if (hasad_memfd_last_handle() == nullptr) {
        ALOGE("registerDeltaForceEspNatives: no memfd handle");
        return JNI_FALSE;
    }

    JNINativeMethod methods[] = {
            {"nativeStartReader", "()V", reinterpret_cast<void *>(deltaForceEspStartReader)},
            {"nativeSetScreen", "(II)V", reinterpret_cast<void *>(deltaForceEspSetScreen)},
            {"nativePoll", "()[F", reinterpret_cast<void *>(deltaForceEspPoll)},
    };
    if (env->RegisterNatives(bridgeClass, methods, sizeof(methods) / sizeof(methods[0])) < 0) {
        if (env->ExceptionCheck()) {
            env->ExceptionClear();
        }
        ALOGE("registerDeltaForceEspNatives: RegisterNatives failed");
        return JNI_FALSE;
    }
    ALOGD("registerDeltaForceEspNatives ok handle=%p bridge=%p",
          hasad_memfd_last_handle(), bridgeClass);
    return JNI_TRUE;
}

static void setDeltaForceEspScreenNative(JNIEnv *, jclass, jint width, jint height) {
    DeltaForceEspServerEnsure();
    DeltaForceEspServerSetScreen(width, height);
}

static void setDeltaForceEspSmallCrosshairNative(JNIEnv *, jclass, jboolean enabled) {
    DeltaForceEspServerEnsure();
    DeltaForceEspServerSetSmallCrosshair(enabled ? 1 : 0);
}

static void setDeltaForceEspAimbotConfigNative(JNIEnv *env, jclass, jfloatArray arr) {
    DeltaForceEspServerEnsure();
    if (arr == nullptr) {
        DeltaForceEspServerSetAimbotConfig(nullptr);
        return;
    }
    const jsize len = env->GetArrayLength(arr);
    if (len < 15) {
        return;
    }
    jfloat *data = env->GetFloatArrayElements(arr, nullptr);
    if (data == nullptr) {
        return;
    }
    DeltaEspAimbotConfig cfg{};
    cfg.enabled = data[0] > 0.5f ? 1 : 0;
    cfg.teamCheck = data[1] > 0.5f ? 1 : 0;
    cfg.visibleCheck = data[2] > 0.5f ? 1 : 0;
    cfg.aimWhenZooming = data[3] > 0.5f ? 1 : 0;
    cfg.aimWhenFiringOnly = data[4] > 0.5f ? 1 : 0;
    cfg.ignoreLayingOnTheGround = data[5] > 0.5f ? 1 : 0;
    cfg.useBoneTargeting = data[6] > 0.5f ? 1 : 0;
    cfg.freeBoneTargeting = data[7] > 0.5f ? 1 : 0;
    cfg.prioritizeClosest = data[8] > 0.5f ? 1 : 0;
    cfg.maxDistance = data[9];
    cfg.smoothness = data[10];
    cfg.fov = data[11];
    cfg.targetHeight = data[12];
    cfg.verticalAdjust = data[13];
    cfg.aimBone = static_cast<int>(data[14]);
    env->ReleaseFloatArrayElements(arr, data, JNI_ABORT);
    DeltaForceEspServerSetAimbotConfig(&cfg);
}

static void setDeltaForceEspBulletTrackConfigNative(JNIEnv *env, jclass, jfloatArray arr) {
    DeltaForceEspServerEnsure();
    if (arr == nullptr) {
        DeltaForceEspServerSetBulletTrackConfig(nullptr);
        return;
    }
    const jsize len = env->GetArrayLength(arr);
    if (len < 9) {
        return;
    }
    jfloat *data = env->GetFloatArrayElements(arr, nullptr);
    if (data == nullptr) {
        return;
    }
    DeltaEspBulletTrackConfig cfg{};
    cfg.enabled = data[0] > 0.5f ? 1 : 0;
    cfg.teamCheck = data[1] > 0.5f ? 1 : 0;
    cfg.whenFiring = data[2] > 0.5f ? 1 : 0;
    cfg.whenZooming = data[3] > 0.5f ? 1 : 0;
    cfg.useBoneTargeting = data[4] > 0.5f ? 1 : 0;
    cfg.maxDistance = data[5];
    cfg.fov = data[6];
    cfg.aimBone = static_cast<int>(data[7]);
    cfg.verticalAdjust = data[8];
    env->ReleaseFloatArrayElements(arr, data, JNI_ABORT);
    DeltaForceEspServerSetBulletTrackConfig(&cfg);
}

static JNINativeMethod gMethods[] = {
        {"disableHiddenApi", "()Z",(void *) disableHiddenApi},
        {"init_seccomp",   "()V",  (void *) init_seccomp},
        {"hideXposed", "()V",      (void *) hideXposed},
        {"addIORule",  "(Ljava/lang/String;Ljava/lang/String;)V", (void *) addIORule},
        {"enableIO",   "()V",(void *) enableIO},
        {"initNative",   "(ILjava/lang/Class;Ljava/lang/Class;)V", (void *) initNative},
        {"nukeTargetLibrary", "(Ljava/lang/String;I)V", (void *) nukeTargetLibrary},
        {"installAnogsThunderHook", "()V", (void *) installAnogsThunderHook},
        {"areAnogsHooksInstalled", "()Z", (void *) areAnogsHooksInstalled},
        {"getMappedLibraryBase", "(Ljava/lang/String;)J", (void *) getMappedLibraryBase},
        {"memfdLoadElf", "([B)Z", (void *) memfdLoadElf},
        {"memfdLoadHookElf", "([B)Z", (void *) memfdLoadHookElf},
        {"setSuppressNativeLog", "(Z)V", (void *) setSuppressNativeLog},
        {"setLogScrubConfig", "(ZLjava/lang/String;Ljava/lang/String;)V", (void *) setLogScrubConfig},
        {"readRealProcSelfMaps", "()Ljava/lang/String;", (void *) readRealProcSelfMaps},
        {"patchMappedLibraryRva", "(Ljava/lang/String;J[B[B)Z", (void *) patchMappedLibraryRva},
        {"patchAbsoluteAddress", "(J[B[B)Z", (void *) patchAbsoluteAddress},
        {"registerFarlightEspNatives", "(Ljava/lang/Class;)Z", (void *) registerFarlightEspNatives},
        {"pollFarlightEspFrames", "()[F", (void *) pollFarlightEspFrames},
        {"getFarlightEspBridgeStatus", "()[I", (void *) getFarlightEspBridgeStatus},
        {"registerDeltaForceEspNatives", "(Ljava/lang/Class;)Z", (void *) registerDeltaForceEspNatives},
        {"ensureDeltaForceEspServer", "()Z", (void *) ensureDeltaForceEspServer},
        {"setDeltaForceEspScreen", "(II)V", (void *) setDeltaForceEspScreenNative},
        {"setDeltaForceEspSmallCrosshair", "(Z)V", (void *) setDeltaForceEspSmallCrosshairNative},
        {"setDeltaForceEspAimbotConfig", "([F)V", (void *) setDeltaForceEspAimbotConfigNative},
        {"setDeltaForceEspBulletTrackConfig", "([F)V", (void *) setDeltaForceEspBulletTrackConfigNative},
        {"pollDeltaForceEspFrames", "()[F", (void *) pollDeltaForceEspFrames},
        {"getDeltaForceEspBridgeStatus", "()[I", (void *) getDeltaForceEspBridgeStatus},
   //     {"ActivateSdkLog", "()Ljava/lang/String;",(void *) ActivateSdkLog},
    
        
};

int registerNativeMethods(JNIEnv *env, const char *className,JNINativeMethod *gMethods, int numMethods) {
    jclass clazz;
    clazz = env->FindClass(className);
    if (clazz == nullptr) {
        return JNI_FALSE;
    }
    if (env->RegisterNatives(clazz, gMethods, numMethods) < 0) {
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

int registerNatives(JNIEnv *env) {
    if (!registerNativeMethods(env, VMCORE_CLASS, gMethods,sizeof(gMethods) / sizeof(gMethods[0])))
        return JNI_FALSE;
    return JNI_TRUE;
}

void registerMethod(JNIEnv *jenv) {
    registerNatives(jenv);
}

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    VMEnv.vm = vm;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_EVERSION;
    }
    registerMethod(env);
    // Do not call nukeLibrary() from JNI_OnLoad: its seccomp + FD exhaustion apply to the
    // whole process and block normal file I/O so the loader / game cannot start.
    // std::thread([]() {
    //     nukeLibrary("libanogs.so", NUKE_LAYERS_ALL);
    // }).detach();
    

    return JNI_VERSION_1_6;
}
 