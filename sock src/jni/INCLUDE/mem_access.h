#ifndef XT_MEM_ACCESS_H
#define XT_MEM_ACCESS_H

#include <fcntl.h>
#include <unistd.h>
#include <sys/uio.h>
#include <sys/syscall.h>
#include <cstdint>
#include <cstring>
#include <cstdlib>

extern int32_t gGMemFD;
extern pid_t pid;

namespace xt_mem {

namespace detail {

#if defined(__aarch64__)
static inline long sc6(long nr, long a0, long a1, long a2, long a3, long a4, long a5) {
    register long x8 asm("x8") = nr;
    register long x0 asm("x0") = a0;
    register long x1 asm("x1") = a1;
    register long x2 asm("x2") = a2;
    register long x3 asm("x3") = a3;
    register long x4 asm("x4") = a4;
    register long x5 asm("x5") = a5;
    asm volatile("svc #0"
                 : "+r"(x0)
                 : "r"(x1), "r"(x2), "r"(x3), "r"(x4), "r"(x5), "r"(x8)
                 : "memory", "cc");
    return x0;
}
#elif defined(__arm__)
static inline long sc6(long nr, long a0, long a1, long a2, long a3, long a4, long a5) {
    register long r7 asm("r7") = nr;
    register long r0 asm("r0") = a0;
    register long r1 asm("r1") = a1;
    register long r2 asm("r2") = a2;
    register long r3 asm("r3") = a3;
    register long r4 asm("r4") = a4;
    register long r5 asm("r5") = a5;
    asm volatile("svc #0"
                 : "+r"(r0)
                 : "r"(r1), "r"(r2), "r"(r3), "r"(r4), "r"(r5), "r"(r7)
                 : "memory", "cc");
    return r0;
}
#else
static inline long sc6(long nr, long a0, long a1, long a2, long a3, long a4, long a5) {
    return syscall(nr, a0, a1, a2, a3, a4, a5);
}
#endif

static constexpr uint8_t kPx = 0xA7;

static inline void build_proc_mem_path(char *out, size_t cap, pid_t p) {
    static const uint8_t pre[] = {0x88, 0xD7, 0xD5, 0xC8, 0xC4, 0x88};
    static const uint8_t suf[] = {0x88, 0xCA, 0xC2, 0xCA};
    if (cap < 24) {
        if (cap > 0) {
            out[0] = '\0';
        }
        return;
    }
    size_t i = 0;
    for (size_t n = 0; n < sizeof pre; n++) {
        out[i++] = static_cast<char>(pre[n] ^ kPx);
    }
    if (p <= 0) {
        out[i] = '\0';
        return;
    }
    char rev[16];
    int r = 0;
    pid_t x = p;
    while (x > 0 && r < 15) {
        rev[r++] = static_cast<char>('0' + (x % 10));
        x /= 10;
    }
    while (r > 0) {
        out[i++] = rev[--r];
    }
    for (size_t n = 0; n < sizeof suf; n++) {
        out[i++] = static_cast<char>(suf[n] ^ kPx);
    }
    out[i] = '\0';
}

static inline int sys_open(const char *path, int flags) {
#if defined(SYS_openat)
    return static_cast<int>(syscall(SYS_openat, AT_FDCWD, path, flags, 0, 0, 0));
#else
    return open(path, flags);
#endif
}

static inline ssize_t sys_pread64(int fd, void *buf, size_t len, off64_t off) {
#if defined(SYS_pread64)
    return syscall(SYS_pread64, fd, buf, len, off, 0, 0);
#else
    return pread64(fd, buf, len, off);
#endif
}

static inline ssize_t sys_pwrite64(int fd, const void *buf, size_t len, off64_t off) {
#if defined(SYS_pwrite64)
    return syscall(SYS_pwrite64, fd, buf, len, off, 0, 0);
#else
    return pwrite64(fd, buf, len, off);
#endif
}

static inline ssize_t sys_preadv(int fd, const struct iovec *iov, int iovcnt, off64_t off) {
#if defined(SYS_preadv)
    return syscall(SYS_preadv, fd, iov, iovcnt, off, 0, 0);
#else
    return preadv(fd, iov, iovcnt, off);
#endif
}

static inline ssize_t sys_pwritev(int fd, const struct iovec *iov, int iovcnt, off64_t off) {
#if defined(SYS_pwritev)
    return syscall(SYS_pwritev, fd, iov, iovcnt, off, 0, 0);
#else
    return pwritev(fd, iov, iovcnt, off);
#endif
}

static inline ssize_t vm_iov(pid_t target, void *local, void *remote, size_t len, bool write) {
    struct iovec li{local, len};
    struct iovec ri{remote, len};
    const long nr = write ? SYS_process_vm_writev : SYS_process_vm_readv;
    return sc6(nr, target, reinterpret_cast<long>(&li), 1,
                 reinterpret_cast<long>(&ri), 1, 0);
}

#if defined(XT_MEM_STEALTH)
static constexpr size_t kStealthChunkMax = 512;

static inline ssize_t vm_iov_chunked(pid_t target, void *local, uintptr_t remote, size_t len,
                                     bool write) {
    if (target < 1 || local == nullptr || len == 0) {
        return 0;
    }
    uint8_t *dst = static_cast<uint8_t *>(local);
    uintptr_t src = remote;
    size_t remaining = len;
    ssize_t total = 0;
    while (remaining > 0) {
        size_t chunk = remaining;
        if (chunk > kStealthChunkMax) {
            chunk = kStealthChunkMax;
        }
        ssize_t n = vm_iov(target, dst, reinterpret_cast<void *>(src), chunk, write);
        if (n <= 0) {
            return total > 0 ? total : n;
        }
        dst += n;
        src += static_cast<uintptr_t>(n);
        remaining -= static_cast<size_t>(n);
        total += n;
        if (static_cast<size_t>(n) < chunk) {
            break;
        }
    }
    return total;
}
#endif

} // namespace detail

static inline int open_proc_mem(pid_t target) {
    if (target < 1) {
        return -1;
    }
    char path[48];
    detail::build_proc_mem_path(path, sizeof path, target);
    int fd = detail::sys_open(path, O_RDWR | O_CLOEXEC);
    if (fd < 0) {
        fd = detail::sys_open(path, O_RDONLY | O_CLOEXEC);
    }
    return fd;
}

static inline void detach() {
    if (gGMemFD >= 0) {
        close(gGMemFD);
        gGMemFD = -1;
    }
}

static inline bool attach(pid_t target) {
    detach();
    if (target < 1) {
        return false;
    }
#if defined(XT_MEM_STEALTH) && !defined(XT_MEM_ALLOW_VM_FALLBACK)
    return true;
#else
    // Open /proc/pid/mem for fallback (and primary when not stealth-only)
    gGMemFD = open_proc_mem(target);
#if defined(XT_MEM_STEALTH)
    return true; // process_vm_readv may still work even if mem fd fails
#else
    return gGMemFD >= 0;
#endif
#endif
}

static inline bool transfer(pid_t target, uintptr_t remote, void *local, size_t size, bool write,
                            int optional_fd = -1) {
    if (size == 0 || local == nullptr) {
        return true;
    }

    if (target >= 1) {
#if defined(XT_MEM_STEALTH)
        ssize_t n = detail::vm_iov_chunked(target, local, remote, size, write);
#else
        ssize_t n = detail::vm_iov(target, local, reinterpret_cast<void *>(remote), size, write);
#endif
        if (n == static_cast<ssize_t>(size)) {
            return true;
        }
    }

    // /proc/pid/mem fallback — needed when process_vm_readv fails under virtualization
#if !defined(XT_MEM_STEALTH) || defined(XT_MEM_ALLOW_VM_FALLBACK)
    int mem_fd = (gGMemFD >= 0) ? gGMemFD : optional_fd;
    if (mem_fd < 0 && target >= 1) {
        mem_fd = open_proc_mem(target);
        if (mem_fd >= 0) {
            gGMemFD = mem_fd;
        }
    }
    if (mem_fd >= 0) {
        const ssize_t n =
            write ? detail::sys_pwrite64(mem_fd, local, size, static_cast<off64_t>(remote))
                  : detail::sys_pread64(mem_fd, local, size, static_cast<off64_t>(remote));
        if (n == static_cast<ssize_t>(size)) {
            return true;
        }
    }
#endif
    return false;
}

static inline bool load(uintptr_t remote, void *local, size_t size) {
    if (pid < 1) {
        return false;
    }
    return transfer(pid, remote, local, size, false);
}

static inline bool store(uintptr_t remote, const void *local, size_t size) {
    if (pid < 1) {
        return false;
    }
    return transfer(pid, remote, const_cast<void *>(local), size, true);
}

static inline ssize_t fd_preadv(int fd, const struct iovec *iov, int iovcnt, off64_t off) {
    return detail::sys_preadv(fd, iov, iovcnt, off);
}

static inline ssize_t fd_pwritev(int fd, const struct iovec *iov, int iovcnt, off64_t off) {
    return detail::sys_pwritev(fd, iov, iovcnt, off);
}

} // namespace xt_mem

#endif
