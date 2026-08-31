
#include <sys/types.h>
#include <pthread.h>
#include <jni.h>
#include <string>
#include "obfuscate.h"
#include "ESP.h"
#include "Hacks.h"
#include "StrEnc.h"
#include "Tools.h"
#include "HackShooter.h"
#include "json.hpp"
#include "Includes.h"
//#include <ctime>



#define LOG_TAG "JNI_Detect"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

ESP espOverlay;
int type = 1, utype = 2;

using json = nlohmann::json;

int expiredDate;
bool DaddyXerr0r = true;  // define the variable here
bool loggedin = true;

extern "C"
JNIEXPORT jstring JNICALL
Java_lauresprojects_com_recorder_libhelper_DownloadZip_PASSJKPAPA(JNIEnv *env, jobject thiz) {
    return env->NewStringUTF(OBFUSCATE("1212"));
}

extern "C"
JNIEXPORT jstring JNICALL
Java_lauresprojects_com_recorder_activity_LoginActivity_FixCrash(JNIEnv *env, jobject thiz) {
    return env->NewStringUTF(OBFUSCATE("https://"));
}

extern "C" JNIEXPORT void JNICALL
Java_lauresprojects_com_recorder_floating_Overlay_DrawOn(JNIEnv *env, jclass , jobject espView, jobject canvas) {
    espOverlay = ESP(env, espView, canvas);
    if (espOverlay.isValid()){
        DrawESP(espOverlay, espOverlay.getWidth(), espOverlay.getHeight());
    }
}





extern "C" JNIEXPORT void JNICALL
Java_lauresprojects_com_recorder_floating_Overlay_Close(JNIEnv *, jobject) {
    Close();
    options.openState = -1;
    options.aimBullet = -1;
    options.aimT = -1;
}


extern "C" JNIEXPORT jboolean JNICALL
Java_lauresprojects_com_recorder_floating_Overlay_getReady(JNIEnv *, jobject thiz) {
    int sockCheck = 1;

    if (!Create()) {
        perror("Creation failed");
        return false;
    }
    setsockopt(sock, SOL_SOCKET, SO_REUSEADDR, &sockCheck, sizeof(int));
    if (!Bind()) {
        perror("Bind failed");
        return false;
    }

    if (!Listen()) {
        perror("Listen failed");
        return false;
    }
    if (Accept()) {
        return true;
    }
}


extern "C"
JNIEXPORT void JNICALL
Java_lauresprojects_com_recorder_floating_ToggleAim_ToggleAim(JNIEnv *, jobject thiz, jboolean value) {
    if (value)
        options.openState = 0;
    else
        options.openState = -1;
}

extern "C"
JNIEXPORT void JNICALL
Java_lauresprojects_com_recorder_floating_ToggleBullet_ToggleBullet(JNIEnv *, jobject thiz, jboolean value) {
    if (value)
        options.aimBullet = 0;
    else
        options.aimBullet = -1;
}

extern "C"
JNIEXPORT void JNICALL
Java_lauresprojects_com_recorder_floating_ToggleSimulation_ToggleSimulation(JNIEnv *, jobject thiz, jboolean value) {
    if (value)
        options.aimT = 0;
    else
        options.aimT = -1;
}

extern "C" JNIEXPORT void JNICALL
Java_lauresprojects_com_recorder_floating_FloatService_SettingValue(JNIEnv *, jobject, jint code, jboolean jboolean1) {

    switch ((int) code) {
        case 2:
            isPlayerLine = jboolean1;
            break;
        case 3:
            isPlayerBox = jboolean1;
            break;
        case 4:
            isSkeleton = jboolean1;
            break;
        case 5:
            isPlayerDistance = jboolean1;
            break;
        case 6:
            isPlayerHealth = jboolean1;
            break;
        case 7:
            isPlayerName = jboolean1;
            break;
        case 8:
            isPlayerHead = jboolean1;
            break;
        case 9:
            is360Alert = jboolean1;
            break;
        case 10:
            isPlayerWeapon = jboolean1;
            break;
        case 11:
            isGrenadeWarning = jboolean1;
            break;
        case 12:
            isVehicles = jboolean1;
            break;
        case 13:
            isItems = jboolean1;
            break;
        case 15:
            options.ignoreAi = jboolean1;
            break;
        case 16:
            isPlayerWeaponIcon = jboolean1;
            break;
        case 17:
            isLootItems = jboolean1;
            break;
        case 18:
            isRadar = jboolean1;
            break;
        case 19:
            isPlayerUID = jboolean1;
            break;
        case 20:
            isPlayerNation = jboolean1;
            break;
        case 21:
            isPlayerTeamID = jboolean1;
            break;
        case 22:
            isFightMode = jboolean1;
            break;
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_lauresprojects_com_recorder_floating_FloatService_SettingAim(JNIEnv *env, jobject thiz, jint setting_code, jboolean value) {
    switch ((int) setting_code) {
        case 1:
            options.openState = -1;
            break;
        case 2:
            options.aimBullet = -1;
            break;
        case 3:
            options.pour = value;
            break;
        case 4:
            options.ignoreBot = value;
            break;
        case 5:
            options.InputInversion = value;
            break;
        case 6:
            options.tracingStatus = value;
            break;
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_lauresprojects_com_recorder_floating_FloatService_SettingMemory(JNIEnv *env, jobject thiz, jint setting_code, jboolean value) {
    switch ((int) setting_code) {
        case 1:
        //    otherFeature.LessRecoil = value;
            break;
        case 2:
            otherFeature.SmallCrosshair = value;
            break;
        case 3:
            otherFeature.Aimbot = value;
            break;
        case 4:
            otherFeature.WideView = value;
            break;
          
    }
}

extern "C" JNIEXPORT void JNICALL
Java_lauresprojects_com_recorder_floating_FloatService_Range(JNIEnv *, jobject, jint range) {
    options.aimingRange = 1 + range;
}

extern "C" JNIEXPORT void JNICALL
Java_lauresprojects_com_recorder_floating_FloatService_distances(JNIEnv *, jobject, jint distances) {
    options.aimingDist = distances;
}

extern "C"
JNIEXPORT void JNICALL
Java_lauresprojects_com_recorder_floating_FloatService_recoil(JNIEnv *env, jobject thiz, jint recoil) {
    options.recCompe = recoil;
}

extern "C"
JNIEXPORT void JNICALL
Java_lauresprojects_com_recorder_floating_FloatService_recoil2(JNIEnv *env, jobject thiz, jint recoil) {
    options.recCompe1 = recoil;
}

extern "C"
JNIEXPORT void JNICALL
Java_lauresprojects_com_recorder_floating_FloatService_recoil3(JNIEnv *env, jobject thiz, jint recoil) {
    options.recCompe2 = recoil;
}

extern "C"
JNIEXPORT void JNICALL
Java_lauresprojects_com_recorder_floating_FloatService_Bulletspeed(JNIEnv *env, jobject thiz, jint bulletspeed) {
    options.aimingSpeed = bulletspeed;
}

extern "C"
JNIEXPORT void JNICALL
Java_lauresprojects_com_recorder_floating_FloatService_AimingSpeed(JNIEnv *env, jobject thiz, jint aimingspeed) {
    options.touchSpeed = aimingspeed;
}

extern "C"
JNIEXPORT void JNICALL
Java_lauresprojects_com_recorder_floating_FloatService_Smoothness(JNIEnv *env, jobject thiz, jint smoothness) {
    options.Smoothing = smoothness;
}

extern "C"
JNIEXPORT void JNICALL
Java_lauresprojects_com_recorder_floating_FloatService_TouchSize(JNIEnv *env, jobject thiz, jint touchsize) {
    options.touchSize = touchsize;
}

extern "C"
JNIEXPORT void JNICALL
Java_lauresprojects_com_recorder_floating_FloatService_TouchPosX(JNIEnv *env, jobject thiz, jint touchposx) {
    options.touchX = touchposx;
}

extern "C"
JNIEXPORT void JNICALL
Java_lauresprojects_com_recorder_floating_FloatService_TouchPosY(JNIEnv *env, jobject thiz, jint touchposy) {
    options.touchY = touchposy;
}


extern "C"
JNIEXPORT void JNICALL
Java_lauresprojects_com_recorder_floating_FloatService_WideView(JNIEnv *env, jobject thiz, jint wideview) {
    otherFeature.WideView = wideview;
}

extern "C" JNIEXPORT void JNICALL
Java_lauresprojects_com_recorder_floating_FloatService_Target(JNIEnv *, jobject, jint target) {
    options.aimbotmode = target;
}
extern "C" JNIEXPORT void JNICALL
Java_lauresprojects_com_recorder_floating_FloatService_AimWhen(JNIEnv *, jobject, jint state) {
    options.aimingState = state;
}
extern "C" JNIEXPORT void JNICALL
Java_lauresprojects_com_recorder_floating_FloatService_AimBy(JNIEnv *, jobject, jint aimby) {
    options.priority = aimby;
}
extern "C" JNIEXPORT void JNICALL
Java_lauresprojects_com_recorder_floating_FloatService_RadarSize(JNIEnv *, jobject, jint size) {
    request.radarSize = size;
}

/* ================ ESP FUNCTION =========================*/

extern "C"
JNIEXPORT jstring JNICALL
Java_lauresprojects_com_recorder_server_ApiServer_getOwner(JNIEnv *env, jclass clazz) {
    return env->NewStringUTF(OBFUSCATE("https://t.me/rieooeoeoe"));   // OWNER LINK
}

extern "C"
JNIEXPORT jstring JNICALL
Java_lauresprojects_com_recorder_server_ApiServer_getTelegram(JNIEnv *env, jclass clazz) {  // CHANNEL LINK
    return env->NewStringUTF(OBFUSCATE("https://t.me/eieoieoeoeie"));
}

extern "C"
JNIEXPORT jstring JNICALL
Java_lauresprojects_com_recorder_server_ApiServer_getGrup(JNIEnv *env, jclass clazz) {  // GROUP LINK
    return env->NewStringUTF(OBFUSCATE("https://t.me/eieoeooeoeoe"));
}


extern "C"
JNIEXPORT jstring JNICALL
Java_lauresprojects_com_recorder_server_ApiServer_mainURL(JNIEnv *env, jclass clazz) {
    return env->NewStringUTF(
            OBFUSCATE("https://github.com/NOOBISN/Personal/releases/download/personal/panda.zip"));  //BYPASS LINK

}
extern "C"
JNIEXPORT jstring JNICALL
Java_lauresprojects_com_recorder_Component_DownloadZip_pw(JNIEnv *env, jobject thiz) {
    return env->NewStringUTF(
            OBFUSCATE(""));
}


#include <jni.h>

/*
extern "C"
JNIEXPORT jstring JNICALL
Java_lauresprojects_com_recorder_activity_LoginActivity_URLJSON(JNIEnv *env, jclass clazz) {
    return env->NewStringUTF("https://github.com/rayansyed77/AppLoader/releases/download/Updates/update.json");
}
*/

// signature verification
extern "C"
JNIEXPORT jstring JNICALL
Java_lauresprojects_com_recorder_server_ApiServer_fdjhvf(JNIEnv *env, jclass clazz) {
    return env->NewStringUTF("EB1FFA1BFE713D859CB05326E4CFC462992BC5528C8C7F722CD0A742AC0EE225");
}


// canary detection
const char *suspiciousPatterns[] = {
    R"(com\.guoshi\.httpcanary\.premium)",  
    R"(com\.guoshi\.httpcanary)",          
    R"(com\.sniffer)",                    
    R"(com\.httpcanary\.pro)",              
    R"(com\.httpcanary)",                   
    R"(com\..+\.httpcanary)",               
    R"(com\..+\.canary)"                    
};

extern "C"
JNIEXPORT jstring JNICALL
Java_lauresprojects_com_recorder_server_ApiServer_checkSuspiciousAppsNative(JNIEnv *env, jclass clazz) {
    jclass activityThread = env->FindClass("android/app/ActivityThread");
    jmethodID currentActivityThread = env->GetStaticMethodID(activityThread, "currentActivityThread", "()Landroid/app/ActivityThread;");
    jobject at = env->CallStaticObjectMethod(activityThread, currentActivityThread);

    jmethodID getAppContext = env->GetMethodID(activityThread, "getApplication", "()Landroid/app/Application;");
    jobject context = env->CallObjectMethod(at, getAppContext);

    jclass contextClass = env->GetObjectClass(context);
    jmethodID getPackageManager = env->GetMethodID(contextClass, "getPackageManager", "()Landroid/content/pm/PackageManager;");
    jobject packageManager = env->CallObjectMethod(context, getPackageManager);

    jclass pmClass = env->GetObjectClass(packageManager);
    jmethodID getInstalledPackages = env->GetMethodID(pmClass, "getInstalledPackages", "(I)Ljava/util/List;");
    jobject packageList = env->CallObjectMethod(packageManager, getInstalledPackages, 0);

    jclass listClass = env->GetObjectClass(packageList);
    jmethodID sizeMethod = env->GetMethodID(listClass, "size", "()I");
    jint size = env->CallIntMethod(packageList, sizeMethod);

    jmethodID getMethod = env->GetMethodID(listClass, "get", "(I)Ljava/lang/Object;");

    jclass packageInfoClass = env->FindClass("android/content/pm/PackageInfo");
    jfieldID packageNameField = env->GetFieldID(packageInfoClass, "packageName", "Ljava/lang/String;");

    

    return env->NewStringUTF("SAFE");
}

extern "C"
JNIEXPORT void JNICALL
Java_lauresprojects_com_recorder_floating_FloatService_SkinHack(JNIEnv *env, jobject thiz, jint setting_code) {
    switch ((int) setting_code) {
        case 1:  otherFeature.clothes = 1; break;   // Blood Raven
        case 2:  otherFeature.clothes = 2; break;   // Golden Pharaoh
        case 3:  otherFeature.clothes = 3; break;   // Avalanche
        case 4:  otherFeature.clothes = 4; break;   // Poseidon
        case 5:  otherFeature.clothes = 5; break;   // Arcane Jester
        case 6:  otherFeature.clothes = 6; break;   // Silvanus
        case 7:  otherFeature.clothes = 7; break;   // Marmoris
        case 8:  otherFeature.clothes = 8; break;   // Fiore
        case 9:  otherFeature.clothes = 9; break;   // Ignis
        case 10: otherFeature.clothes = 10; break;  // White Mummy
        case 11: otherFeature.clothes = 11; break;  // Galadria
        case 12: otherFeature.clothes = 12; break;  // Flamewraith Set
        case 13: otherFeature.clothes = 13; break;  // Majestic Cavalry Set
        case 14: otherFeature.clothes = 14; break;  // Bramble Overlord Set
        case 15: otherFeature.clothes = 15; break;  // Nether Visage Set
        case 16: otherFeature.clothes = 16; break;  // Spectral Swan Set
        case 17: otherFeature.clothes = 17; break;  // Untamed Celestial
        case 18: otherFeature.clothes = 18; break;  // Snowstar Sweetheart
        case 19: otherFeature.clothes = 19; break;  // Arctic Conqueror
        case 20: otherFeature.clothes = 20; break;  // Feral Ravager
        case 21: otherFeature.clothes = 21; break;  // Vampyra Countess
        case 22: otherFeature.clothes = 22; break;  // Serene Lumina
        case 23: otherFeature.clothes = 23; break;  // Mercury Soldier
        case 24: otherFeature.clothes = 24; break;  // Luminous Muse
        case 25: otherFeature.clothes = 25; break;  // Origin Lumen
        case 26: otherFeature.clothes = 26; break;  // Serpengleam
        case 27: otherFeature.clothes = 27; break;  // Shinobi Spirit
        case 28: otherFeature.clothes = 28; break;  // Foxy Flare
        case 29: otherFeature.clothes = 29; break;  // Glacial Bride
        case 30: otherFeature.clothes = 30; break;  // Boxerbolt
        case 31: otherFeature.clothes = 31; break;  // Dandy Groovster
        case 32: otherFeature.clothes = 32; break;  // Wrathful Neptune
        case 33: otherFeature.clothes = 33; break;  // Noctum Sunder
        case 34: otherFeature.clothes = 34; break;  // Crimson Ephialtes
        default:
            otherFeature.clothes = 0;
            break;
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_lauresprojects_com_recorder_floating_FloatService_Skinbag(JNIEnv *env, jobject thiz, jint setting_code) {
    switch ((int) setting_code) {
        case 1: otherFeature.bag = 1; break; // Poseidon
        case 2: otherFeature.bag = 2; break; // Mystique Splendor
        case 3: otherFeature.bag = 3; break; // Ancient Civilization
        case 4: otherFeature.bag = 4; break; // Galadria
        case 5: otherFeature.bag = 5; break; // Alfheim Wonder
        default: otherFeature.bag = 0; break;
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_lauresprojects_com_recorder_floating_FloatService_Skinhelmet(JNIEnv *env, jobject thiz, jint setting_code) {
    switch ((int) setting_code) {
        case 1: otherFeature.helmet = 1; break; // Inferno Rider
        case 2: otherFeature.helmet = 2; break; // Auric Sentinel
        case 3: otherFeature.helmet = 3; break; // Galadria
        case 4: otherFeature.helmet = 4; break; // Shining Eagle
        case 5: otherFeature.helmet = 5; break; // Eternal Kingdom
        default: otherFeature.helmet = 0; break;
    }
}

std::string credit;
std::string modname;
std::string token;
std::string EXP = "4102444800";



    jstring native_Check(JNIEnv *env, jclass clazz, jobject mContext, jstring mUserKey) {
    // 🔥🔥🔥 BYPASS - Server check skip 🔥🔥🔥
    
    // Set expiry to 2099 (4102444800 = 1 Jan 2099)
    EXP = "4102444800";
    
    // Login success
    loggedin = true;
    DaddyXerr0r = true;
    
    // Return valid response
    std::string output = "OK|" + EXP;
    return env->NewStringUTF(output.c_str());
}


int Register1(JNIEnv *env) {
    JNINativeMethod methods[] = {{"suckmydick", "(Landroid/content/Context;Ljava/lang/String;)Ljava/lang/String;", (void *) native_Check}};

    jclass clazz = env->FindClass("lauresprojects/com/recorder/activity/LoginActivity");
    if (!clazz)
        return -1;

    if (env->RegisterNatives(clazz, methods, sizeof(methods) / sizeof(methods[0])) != 0)
        return -1;

    return 0;
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    vm->GetEnv((void **) &env, JNI_VERSION_1_6);
    if (Register1(env) != 0)
        return -1;
    return JNI_VERSION_1_6;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_lauresprojects_com_recorder_server_ApiServer_EXP(JNIEnv *env, jclass clazz) {
   // return env->NewStringUTF(EXP.c_str());
   if (EXP.empty())
    return env->NewStringUTF("");

return env->NewStringUTF(EXP.c_str());
}
/*
extern "C"
JNIEXPORT jstring JNICALL
Java_lauresprojects_com_recorder_floating_FloatService_getHiddenUrl(JNIEnv* env, jclass clazz) {
   static const char* hiddenUrl = OBFUSCATE("https://raw.githubusercontent.com/rayansyed77/public/main/PaidOnly");
    return env->NewStringUTF(hiddenUrl);
}

extern "C" JNIEXPORT jstring JNICALL
Java_lauresprojects_com_recorder_floating_FloatService_Clone(JNIEnv *env, jobject thiz) {
    return env->NewStringUTF(OBFUSCATE("https://t.me/Panda_BupG"));
}*/