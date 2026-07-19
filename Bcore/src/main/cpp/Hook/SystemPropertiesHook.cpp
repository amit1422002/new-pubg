//
// Intercept android.os.SystemProperties native reads for guest stealth.
//

#include "SystemPropertiesHook.h"
#include "JniHook/JniHook.h"
#include "BoxCore.h"
#include "Log.h"

#include <cstdlib>
#include <cstring>
#include <string>

static jstring (*orig_native_get)(JNIEnv *, jclass, jstring, jstring);
static jint (*orig_native_get_int)(JNIEnv *, jclass, jstring, jint);
static jboolean (*orig_native_get_boolean)(JNIEnv *, jclass, jstring, jboolean);

static jstring callJavaSpoof(JNIEnv *env, jstring key, jstring original) {
    if (BoxCore::getNativeCoreClass() == nullptr || BoxCore::getSpoofSystemPropertyId() == nullptr) {
        return original;
    }
    return (jstring) env->CallStaticObjectMethod(
            BoxCore::getNativeCoreClass(),
            BoxCore::getSpoofSystemPropertyId(),
            key,
            original);
}

static jstring new_native_get(JNIEnv *env, jclass clazz, jstring key, jstring def) {
    jstring result = orig_native_get(env, clazz, key, def);
    if (key == nullptr) {
        return result;
    }
    jstring spoofed = callJavaSpoof(env, key, result != nullptr ? result : def);
    if (spoofed != nullptr && env->ExceptionCheck()) {
        env->ExceptionClear();
        return result;
    }
    return spoofed != nullptr ? spoofed : result;
}

static jint new_native_get_int(JNIEnv *env, jclass clazz, jstring key, jint def) {
    jint result = orig_native_get_int(env, clazz, key, def);
    if (key == nullptr) {
        return result;
    }
    jstring keyStr = key;
    jstring valStr = env->NewStringUTF(std::to_string(result).c_str());
    jstring spoofed = callJavaSpoof(env, keyStr, valStr);
    env->DeleteLocalRef(valStr);
    if (spoofed == nullptr || env->ExceptionCheck()) {
        if (env->ExceptionCheck()) {
            env->ExceptionClear();
        }
        return result;
    }
    const char *utf = env->GetStringUTFChars(spoofed, nullptr);
    jint out = result;
    if (utf != nullptr) {
        out = static_cast<jint>(atoi(utf));
        env->ReleaseStringUTFChars(spoofed, utf);
    }
    env->DeleteLocalRef(spoofed);
    return out;
}

static jboolean new_native_get_boolean(JNIEnv *env, jclass clazz, jstring key, jboolean def) {
    jboolean result = orig_native_get_boolean(env, clazz, key, def);
    if (key == nullptr) {
        return result;
    }
    jstring valStr = env->NewStringUTF(result ? "1" : "0");
    jstring spoofed = callJavaSpoof(env, key, valStr);
    env->DeleteLocalRef(valStr);
    if (spoofed == nullptr || env->ExceptionCheck()) {
        if (env->ExceptionCheck()) {
            env->ExceptionClear();
        }
        return result;
    }
    const char *utf = env->GetStringUTFChars(spoofed, nullptr);
    jboolean out = result;
    if (utf != nullptr) {
        out = (strcmp(utf, "1") == 0 || strcasecmp(utf, "true") == 0) ? JNI_TRUE : JNI_FALSE;
        env->ReleaseStringUTFChars(spoofed, utf);
    }
    env->DeleteLocalRef(spoofed);
    return out;
}

void SystemPropertiesHook::init(JNIEnv *env) {
    const char *className = "android/os/SystemProperties";
    JniHook::HookJniFun(env, className, "native_get",
                        "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
                        (void *) new_native_get,
                        (void **) (&orig_native_get), true);
    JniHook::HookJniFun(env, className, "native_get_int",
                        "(Ljava/lang/String;I)I",
                        (void *) new_native_get_int,
                        (void **) (&orig_native_get_int), true);
    JniHook::HookJniFun(env, className, "native_get_boolean",
                        "(Ljava/lang/String;Z)Z",
                        (void *) new_native_get_boolean,
                        (void **) (&orig_native_get_boolean), true);
}
