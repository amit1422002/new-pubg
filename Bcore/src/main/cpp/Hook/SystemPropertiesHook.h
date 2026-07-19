//
// SystemProperties native hook for guest stealth
//

#ifndef ANUBIS_SYSTEMPROPERTIESHOOK_H
#define ANUBIS_SYSTEMPROPERTIESHOOK_H

#include <jni.h>

class SystemPropertiesHook {
public:
    static void init(JNIEnv *env);
};

#endif
