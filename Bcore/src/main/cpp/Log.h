#include <android/log.h>

#define TAG "NativeCore"

#ifndef ANUBIS_LOG_H
#define ANUBIS_LOG_H

static inline bool& nativeLogSuppressed() {
    static bool suppressed = false;
    return suppressed;
}

static inline void setNativeLogSuppressed(bool suppressed) {
    nativeLogSuppressed() = suppressed;
}

#if 1
#define log_print_error(...) do { if (!nativeLogSuppressed()) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__); } while (0)
#define log_print_debug(...) do { if (!nativeLogSuppressed()) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__); } while (0)
#else
#define log_print_error(...)
#define log_print_debug(...)
#endif

#define ALOGE(...) log_print_error(__VA_ARGS__)
#define ALOGD(...) log_print_debug(__VA_ARGS__)

#ifndef SPEED_LOG_H
#define SPEED_LOG_H 1
#endif

#endif
