#ifndef REI_BASE_STRUCT_H
#define REI_BASE_STRUCT_H

#include "support.h"
#include "obfuscate.h"
#include "init.h"

#include <string>
#define maxplayerCount 100
#define maxvehicleCount 50
#define maxitemsCount 400
#define maxgrenadeCount 10
#define maxzonesCount 10
#define maxboxitemsCount 10


struct PlayerBone {
    bool isBone = false;
    Vec2 neck;
    Vec2 cheast;
    Vec2 pelvis;
    Vec2 lSh;
    Vec2 rSh;
    Vec2 lElb;
    Vec2 rElb;
    Vec2 lWr;
    Vec2 rWr;
    Vec2 lTh;
    Vec2 rTh;
    Vec2 lKn;
    Vec2 rKn;
    Vec2 lAn;
    Vec2 rAn;
    Vec2 head;
};

struct PlayerWeapon {
    bool isWeapon = false;
    int id;
    int ammo;
};

enum Mode {
    InitMode = 1,
    ESPMode = 2,
    HackMode = 3,
    StopMode = 4,
    CameraOnlyMode = 5,
    PlayersBoneMode = 7,
    DrawSyncMode = 8,
    PlayersDiscoverMode = 9,
    AimSyncMode = 10,
};

struct EspCameraView {
    float locX, locY, locZ;
    float rotPitch, rotYaw, rotRoll;
    float fov;
    bool valid;
};

struct PlayerBoneWorld {
    bool isBone = false;
    Vec3 points[16]{};
};

#define maxBoneRefreshBatch 32

struct EspBoneRefresh {
    int playerIndex = -1;
    PlayerBoneWorld boneWorld{};
};

struct Options {
    int aimbotmode;
    int openState;
    int aimT;
    int aimingState;
    bool ignoreBot;
    bool tracingStatus;
    int priority;
    bool pour;
    int aimingRange;
    int aimingDist;
    int aimingSpeed;
    int touchSpeed;
    int recCompe;
    int aimBullet;
    bool ignoreAi;
    float Smoothing;
    bool InputInversion;
    int touchSize;
    int touchX;
    int touchY;
    int recCompe1;
    int recCompe2;
    float recScope[9];
    bool customScope;
    bool prediction;
    bool rayansyed77;
    bool bypassssss;
    bool crashfixcc;
    bool lololol;
};

struct OtherFeature {
    bool SmallCrosshair;
    bool NoShake;
    bool Aimbot;
    int WideView;
    bool ShowDamage;
    int clothes;
    int bag;
    int helmet;
    bool espItems;
    bool espVehicles;
    bool espLootBox;
};

struct Request {
    int Mode;
    Options options;
    OtherFeature otherFeature;
    int ScreenWidth;
    int ScreenHeight;
    Vec2 radarPos;
    float radarSize;
};

struct VehicleData {
    char VehicleName[50];
    float Distance;
    float Fuel;
    float Health;
    Vec3 Location;
};

struct ItemData {
    char ItemName[50];
    float Distance;
    Vec3 Location;
};

struct GrenadeData {
    int type;
    float Distance;
    Vec3 Location;
};

struct ZoneData {
    float Distance;
    Vec3 Location;
};

struct PlayerData {
    char PlayerNameByte[100];
    char PlayerNation[100];
    char PlayerUID[100];
    int TeamID;
    int States;
    float Health;
    float Healthy;
    float Distance;
    bool isBot;
    bool isVisible;
    Vec4 Precise;
    Vec3 HeadLocation;
    Vec2 RadarLocation;
    PlayerWeapon Weapon;
    PlayerBone Bone;
    PlayerBoneWorld BoneWorld;
    int StatusPlayer;
    uint64_t actorAddr;
    uint32_t boneTick;
    bool rayansyed77;
    bool bypassssss;
    bool crashfixcc;
    bool lololol;
};

struct BoxItemData {
    int itemCount;
    int itemID[50];
    float Distance;
    Vec3 Location;
};

struct Response {
    bool Success;
    bool InLobby;
    int PlayerCount;
    int VehicleCount;
    int ItemsCount;
    int BoxItemsCount;
    int GrenadeCount;
    int ZoneCount;
    float fov;
    bool rayansyed77;
    bool bypassssss;
    bool crashfixcc;
    bool lololol;
    bool touchAimActive;
    float touchAimX;
    float touchAimY;
    bool touchUinputReady;
    bool pcAimActive;
    float pcAimX;
    float pcAimY;
    bool pcAimFiring;
    EspCameraView espCamera;
    int BoneRefreshCount;
    uint32_t boneTick;
    EspBoneRefresh BoneRefresh[maxBoneRefreshBatch];
    PlayerData Players[maxplayerCount];
    VehicleData Vehicles[maxvehicleCount];
    ItemData Items[maxitemsCount];
    GrenadeData Grenade[maxgrenadeCount];
    ZoneData Zones[maxzonesCount];
    BoxItemData BoxItems[maxboxitemsCount];
};

#endif //REI_BASE_STRUCT_H
