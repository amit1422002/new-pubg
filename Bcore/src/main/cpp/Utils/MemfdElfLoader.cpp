#include "MemfdElfLoader.h"

#include <android/dlext.h>
#include <android/log.h>
#include <dlfcn.h>
#include <errno.h>
#include <stdint.h>
#include <string.h>
#include <unistd.h>

#include <sys/mman.h>
#include <sys/syscall.h>

#ifndef MFD_CLOEXEC
#define MFD_CLOEXEC 0x0001U
#endif

#ifndef __NR_memfd_create
#if defined(__aarch64__)
#define __NR_memfd_create 279
#elif defined(__arm__)
#define __NR_memfd_create 385
#endif
#endif

static const char *kTag = "Memfd";

static void *g_last_memfd_handle = nullptr;

void *hasad_memfd_last_handle() {
    return g_last_memfd_handle;
}

static int memfd_create_anon() {
#if defined(__NR_memfd_create)
    return static_cast<int>(syscall(__NR_memfd_create, "jit-cache", MFD_CLOEXEC));
#else
    errno = ENOSYS;
    return -1;
#endif
}

bool hasad_load_elf_from_memory(const void *bytes, size_t len) {
    if (bytes == nullptr || len < 4) {
        return false;
    }

    int fd = memfd_create_anon();
    if (fd < 0) {
        __android_log_print(ANDROID_LOG_ERROR, kTag, "memfd_create failed errno=%d", errno);
        return false;
    }

    const auto *data = static_cast<const uint8_t *>(bytes);
    size_t off = 0;
    while (off < len) {
        ssize_t n = write(fd, data + off, len - off);
        if (n <= 0) {
            __android_log_print(ANDROID_LOG_ERROR, kTag, "memfd write failed errno=%d", errno);
            close(fd);
            return false;
        }
        off += static_cast<size_t>(n);
    }

    android_dlextinfo ext{};
    ext.flags = ANDROID_DLEXT_USE_LIBRARY_FD;
    ext.library_fd = fd;
    ext.library_fd_offset = 0;

    void *handle = android_dlopen_ext("libx.so", RTLD_NOW, &ext);
    const char *dlerr = dlerror();
    close(fd);

    if (handle == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, kTag, "android_dlopen_ext failed: %s",
                            dlerr != nullptr ? dlerr : "unknown");
        return false;
    }

    __android_log_print(ANDROID_LOG_INFO, kTag, "memfd ELF loaded handle=%p", handle);
    g_last_memfd_handle = handle;

    typedef void (*bootstrap_fn)();
    bootstrap_fn bootstrap = reinterpret_cast<bootstrap_fn>(dlsym(handle, "delta_esp_bootstrap"));
    if (bootstrap != nullptr) {
        bootstrap();
    }
    return true;
}
