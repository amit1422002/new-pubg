//
// Created by Milk on 4/10/21.
//

#include "IO.h"
#include "Log.h"

jmethodID getAbsolutePathMethodId;

list<IO::RelocateInfo> relocate_rule;

char *replace(const char *str, const char *src, const char *dst) {
    const char *pos = str;
    int count = 0;
    while ((pos = strstr(pos, src))) {
        count++;
        pos += strlen(src);
    }

    size_t result_len = strlen(str) + (strlen(dst) - strlen(src)) * count + 1;
    char *result = (char *) malloc(result_len);
    if (result == nullptr) {
        return nullptr;
    }
    memset(result, 0, result_len);

    const char *left = str;
    const char *right = nullptr;

    while ((right = strstr(left, src))) {
        strncat(result, left, right - left);
        strcat(result, dst);
        right += strlen(src);
        left = right;
    }
    strcat(result, left);
    return result;
}

const char *IO::redirectPath(const char *__path) {
    if (__path == nullptr) {
        return __path;
    }
    // Already inside the virtual tree — never redirect again (prevents nested .vfs paths).
    if (strstr(__path, "/.vfs/") != nullptr) {
        return __path;
    }
    list<IO::RelocateInfo>::iterator iterator;
    for (iterator = relocate_rule.begin(); iterator != relocate_rule.end(); ++iterator) {
        IO::RelocateInfo info = *iterator;
        if (info.targetPath == nullptr || info.relocatePath == nullptr) {
            continue;
        }
        size_t targetLen = strlen(info.targetPath);
        if (targetLen == 0) {
            continue;
        }
        // Prefix match only — strstr() caused double-redirect when relocatePath still
        // contained the target substring (e.g. GMS WorkManager SQLite paths).
        if (strncmp(__path, info.targetPath, targetLen) == 0
            && (__path[targetLen] == '\0' || __path[targetLen] == '/')) {
            char *ret = replace(__path, info.targetPath, info.relocatePath);
            return ret;
        }
    }
    return __path;
}

jstring IO::redirectPath(JNIEnv *env, jstring path) {
    return BoxCore::redirectPathString(env, path);
}

jstring IO::reversePath(JNIEnv *env, jstring path) {
    return BoxCore::reversePathString(env, path);
}

jobject IO::redirectPath(JNIEnv *env, jobject path) {
//    auto pathStr =
//            reinterpret_cast<jstring>(env->CallObjectMethod(path, getAbsolutePathMethodId));
//    jstring redirect = redirectPath(env, pathStr);
//    jobject file = env->NewObject(fileClazz, fileNew, redirect);
//    env->DeleteLocalRef(pathStr);
//    env->DeleteLocalRef(redirect);
    return BoxCore::redirectPathFile(env, path);
}

void IO::addRule(const char *targetPath, const char *relocatePath) {
    IO::RelocateInfo info{};
    info.targetPath = targetPath;
    info.relocatePath = relocatePath;
    relocate_rule.push_back(info);
}

void IO::init(JNIEnv *env) {
    jclass tmpFile = env->FindClass("java/io/File");
    getAbsolutePathMethodId = env->GetMethodID(tmpFile, "getAbsolutePath", "()Ljava/lang/String;");
}
