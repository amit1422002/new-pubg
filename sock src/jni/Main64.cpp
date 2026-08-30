/**************************
* BUILD ON Android ide
* TELEGRAM : @rayansyed77
* *************************/

#include "INCLUDE/struct.h"
#include "INCLUDE/CameraView.h"
#include "INCLUDE/Offsets.h"
#include "INCLUDE/Offsets2.h"
#include "INCLUDE/AimTouch.hpp"
#include "INCLUDE/TouchSimAim.hpp"
#include "INCLUDE/TouchSimStealth.hpp"
#include "INCLUDE/MemPatchStealth.hpp"

#include "INCLUDE/BulletTrackShared.h"
#include "INCLUDE/Log.h"
#include <cmath>
#include <time.h>
#include <sys/uio.h>
#include <sys/syscall.h>
#include <sys/resource.h>
#include <unistd.h>

int typeeee = 1;
int32_t gGMemFD = -1;
bool gEnable = false;
bool Trigger = false;

bool vngver = false;

static inline bool stealthMemRead(uintptr_t addr, void *buf, size_t size) {
    return vm_readv(addr, buf, size);
}

static uint32_t sBulletTrackIpcSeq = 0;

static inline bool btMemWrite(uintptr_t addr, const void *buf, size_t n) {
    if (!buf || n == 0 || addr == 0) {
        return false;
    }
    if (gGMemFD >= 0) {
        iovec iov{const_cast<void *>(buf), n};
        return xt_mem::fd_pwritev(gGMemFD, &iov, 1, (off64_t) addr) == (ssize_t) n;
    }
    return vm_writev(addr, const_cast<void *>(buf), n);
}

static inline bool btMemRead(uintptr_t addr, void *buf, size_t n) {
    if (!buf || n == 0 || addr == 0) {
        return false;
    }
    if (gGMemFD >= 0) {
        iovec iov{buf, n};
        return xt_mem::fd_preadv(gGMemFD, &iov, 1, (off64_t) addr) == (ssize_t) n;
    }
    return vm_readv(addr, buf, n);
}

// ===================================================================
//  Bullet Track core (clean rewrite)
//
//  Crash model that killed the game earlier: the shellcode lives inside a
//  live UE4 .text function. Writing those code bytes while the game thread is
//  executing them (apply/restore every frame, or re-applying on a flaky
//  read-back) tears an instruction mid-execution -> SIGSEGV inside the game.
//
//  Rules this rewrite enforces:
//   1. Code bytes (shellcode + bypass) are written EXACTLY ONCE per UE4 base,
//      and restored EXACTLY ONCE when BT turns off. Never per-bullet, never on
//      a "drift" re-check. After that only the DATA aim-slot is touched.
//   2. The aim slot only ever receives a finite, in-range world coordinate.
//      No NaN, no 1e5-unit junk (that also crashed the game's impact math).
//   3. Per-bullet throttle is done purely by choosing WHICH coordinate goes in
//      the slot (enemy head vs. the player's own crosshair point) — a data
//      write, never a code write.
// ===================================================================

static bool sBtCodeApplied = false;
static uintptr_t sBtPatchedBase = 0;

static inline bool btFiniteVec(const Vec3 &v) {
    return std::isfinite(v.X) && std::isfinite(v.Y) && std::isfinite(v.Z)
           && fabsf(v.X) < 1.0e7f && fabsf(v.Y) < 1.0e7f && fabsf(v.Z) < 1.0e7f;
}

/**
 * A sane world point straight down the crosshair at ~the target's distance.
 * Used for "skip" bullets: the shot lands where the player is already aiming
 * (no assist), and the coordinate stays in normal game range so the in-game
 * impact math never faults.
 */
static Vec3 btCrosshairPoint(const CameraView &cam, float dist) {
    FMatrix m = RotToMatrix(cam.Rotation);
    const float fx = m.M[0][0], fy = m.M[0][1], fz = m.M[0][2];
    float d = dist;
    if (!(d > 200.0f)) d = 4000.0f;
    if (d > 30000.0f) d = 30000.0f;
    return Vec3(cam.Location.X + fx * d,
                cam.Location.Y + fy * d,
                cam.Location.Z + fz * d);
}

/** Write shellcode + bypass ONCE. Idempotent: a second call is a no-op. */
static void btApplyCodeOnce(uintptr_t ue4Base) {
    using namespace OffsetsAllBt64;
    if (ue4Base == 0 || ps_Global_TargetFunc == 0 || ps_Global_Patch1 == 0) {
        return;
    }
    if (sBtCodeApplied && sBtPatchedBase == ue4Base) {
        return;
    }
    const uintptr_t targetFn = ue4Base + ps_Global_TargetFunc;
    const uintptr_t patch1 = ue4Base + ps_Global_Patch1;
    const bool ok = btMemWrite(targetFn, kTargetFuncShellCode, sizeof(kTargetFuncShellCode))
                    && btMemWrite(patch1, BypassBT_patch, sizeof(BypassBT_patch));
    if (ok) {
        sBtCodeApplied = true;
        sBtPatchedBase = ue4Base;
        LOGI("sock64 BT code applied once base=0x%lx", (unsigned long) ue4Base);
    } else {
        LOGW("sock64 BT code apply failed fd=%d", gGMemFD);
    }
}

/** Restore original bytes ONCE (only when BT turns off). */
static void btRestoreCode(uintptr_t ue4Base) {
    using namespace OffsetsAllBt64;
    if (!sBtCodeApplied) {
        return;
    }
    if (ue4Base == 0) {
        ue4Base = sBtPatchedBase;
    }
    if (ue4Base != 0 && ps_Global_TargetFunc != 0 && ps_Global_Patch1 != 0) {
        btMemWrite(ue4Base + ps_Global_TargetFunc, kTargetFuncOrigCode, sizeof(kTargetFuncOrigCode));
        btMemWrite(ue4Base + ps_Global_Patch1, BypassBT_orig, sizeof(BypassBT_orig));
        LOGI("sock64 BT code restored base=0x%lx", (unsigned long) ue4Base);
    }
    sBtCodeApplied = false;
    sBtPatchedBase = 0;
}

/** Data-only write of the aim slot. Safe to call every frame. */
static void btWriteAim(uintptr_t ue4Base, const Vec3 &aim) {
    using namespace OffsetsAllBt64;
    if (!sBtCodeApplied || ue4Base == 0 || !btFiniteVec(aim)) {
        return;
    }
    const uintptr_t aimSlot = ue4Base + ps_Global_TargetFunc + ps_ShellCode_Aim3DPosition;
    btMemWrite(aimSlot, &aim, sizeof(aim));
}

static void syncBulletTrackFast(uintptr_t ue4Base, const Request &req, const CameraView &cam,
                                const Vec3 &myPos, bool allowTrack,
                                float &outNearest, Vec3 &outTarget);

static inline void publishBulletTrackIpc(bool trigger, float nearest, const Vec3 &targetBt) {
    const bool enabled = gEnable && !vngver;
    const bool hasTarget = nearest > 0.f;
    const bool fire = enabled && trigger && hasTarget;
    bulletTrackIpcPublish(pid, enabled, fire, hasTarget,
                          targetBt.X, targetBt.Y, targetBt.Z, sBulletTrackIpcSeq);
}

static inline uint64_t espMonotonicMs() {
    struct timespec ts {};
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return static_cast<uint64_t>(ts.tv_sec) * 1000ull +
           static_cast<uint64_t>(ts.tv_nsec / 1000000ull);
}

static uint64_t sLastBoneRefreshMs = 0;

uintptr_t ReadValue(uintptr_t addr) {
    uintptr_t var = 0;
    vm_readv(addr, &var, sizeof(var));
    return var;
}

void RAYAN_BaseAddress_FLOAT(uintptr_t addr, float value) {
    vm_writev(addr, &value, sizeof(value));
}

// --- Global Toggle Booleans ---
bool isSwitchWeapon = false;
bool isHighJump = false;

FRotator RotatorRotation(Vec3 camera, Vec3 xyz) {
    Vec3 vecDelta = Vec3((camera.X - xyz.X), (camera.Y - xyz.Y), (camera.Z - xyz.Z));
    float hyp = sqrt(vecDelta.X * vecDelta.X + vecDelta.Y * vecDelta.Y);

    FRotator ViewAngles = FRotator();
    ViewAngles.Pitch = -(float) atan(vecDelta.Z / hyp) * (float) (180.0f / PI);
    ViewAngles.Yaw = (float) atan(vecDelta.Y / vecDelta.X) * (float) (180.0f / PI);
    ViewAngles.Roll = (float) 0;

    if (vecDelta.X >= 0.0f)
        ViewAngles.Yaw += 180.0f;

    return ViewAngles;
}

struct Vec2 GetBonePosToScreen(uintptr_t pBase, int bones, CameraView &cameraView) {
    uintptr_t boneAddr = getA(pBase + OffsetsAll64::Mesh);
    struct D3DMatrix baseMatrix = getOMatrix(boneAddr + OffsetsAll64::FixAttachInfoList + 0x10);
    boneAddr = getA(boneAddr + OffsetsAll64::StaticMesh);
    struct D3DMatrix oMatrix = getOMatrix(boneAddr + (bones) * 48);
    Vec3 scr = World2Screen(cameraView, mat2Cord(oMatrix, baseMatrix));
    struct Vec2 out{};
    out.X = scr.X;
    out.Y = scr.Y;
    return out;
}

struct BoneMeshCache {
    uintptr_t mesh = 0;
    uintptr_t staticMesh = 0;
    bool valid = false;
};

static BoneMeshCache getBoneMeshCache(uintptr_t pBase) {
    BoneMeshCache cache{};
    uintptr_t mesh = DecodeBgmiObjectPtr(getA(pBase + OffsetsAll64::Mesh));
    if (!IsPtrPlausible(mesh)) {
        mesh = getA(pBase + OffsetsAll64::Mesh);
    }
    if (!IsPtrPlausible(mesh)) {
        return cache;
    }
    cache.mesh = mesh;
    uintptr_t boneBuf = DecodeBgmiObjectPtr(getA(mesh + OffsetsAll64::StaticMesh));
    if (!IsPtrPlausible(boneBuf)) {
        boneBuf = getA(mesh + OffsetsAll64::StaticMesh);
    }
    // MasterPose fallback (blackbox ResolvePubgBoneMesh)
    if (!IsPtrPlausible(boneBuf) && OffsetsAll64::MasterPoseComponent) {
        uintptr_t master = DecodeBgmiObjectPtr(getA(mesh + OffsetsAll64::MasterPoseComponent));
        if (!IsPtrPlausible(master)) {
            master = getA(mesh + OffsetsAll64::MasterPoseComponent);
        }
        if (IsPtrPlausible(master) && master != mesh) {
            boneBuf = DecodeBgmiObjectPtr(getA(master + OffsetsAll64::StaticMesh));
            if (!IsPtrPlausible(boneBuf)) {
                boneBuf = getA(master + OffsetsAll64::StaticMesh);
            }
            if (IsPtrPlausible(boneBuf)) {
                cache.mesh = master;
            }
        }
    }
    cache.staticMesh = boneBuf;
    cache.valid = IsPtrPlausible(boneBuf);
    return cache;
}

static int playerDataScore(const PlayerData &pl) {
    int score = 0;
    if (pl.BoneWorld.isBone) {
        score += 4;
    }
    if (pl.HeadLocation.Z != 1.f) {
        score += 2;
    }
    score += static_cast<int>(strnlen(pl.PlayerNameByte, sizeof(pl.PlayerNameByte)));
    if (pl.Health > 0.f) {
        score += 1;
    }
    return score;
}

static void dedupePlayerList(Response &resp) {
    int out = 0;
    for (int i = 0; i < resp.PlayerCount && i < maxplayerCount; i++) {
        bool dup = false;
        const PlayerData &a = resp.Players[i];
        for (int j = 0; j < out; j++) {
            PlayerData &b = resp.Players[j];
            if (a.actorAddr != 0 && a.actorAddr == b.actorAddr) {
                dup = true;
                if (playerDataScore(a) > playerDataScore(b)) {
                    b = a;
                }
                break;
            }
        }
        if (!dup) {
            if (out != i) {
                resp.Players[out] = resp.Players[i];
            }
            out++;
        }
    }
    resp.PlayerCount = out;
}

static D3DMatrix oMatrixFromBoneData(const float *oMat) {
    Vec4 rot{oMat[0], oMat[1], oMat[2], oMat[3]};
    Vec3 tran{oMat[4], oMat[5], oMat[6]};
    Vec3 scale{oMat[8], oMat[9], oMat[10]};
    return ToMatrixWithScale(tran, scale, rot);
}

static int maxBoneIndex(bool isTrainingModel, bool isMetroMode) {
    if (isMetroMode) {
        return 24;
    }
    if (isTrainingModel) {
        return 61;
    }
    return 59;
}

static void readBoneWorldsBatch(const BoneMeshCache &cache, const int bones[16], bool isTrainingModel,
                                  bool isMetroMode, Vec3 worldPos[16]) {
    // Prefer FixAttachInfoList+0x10 (PUBG/BGMI); fallback ComponentToWorld @ 0x1D0
    struct D3DMatrix baseMatrix = getOMatrix(cache.mesh + OffsetsAll64::FixAttachInfoList + 0x10);
    if (fabsf(baseMatrix._41) + fabsf(baseMatrix._42) + fabsf(baseMatrix._43) < 1.f) {
        baseMatrix = getOMatrix(cache.mesh + 0x1D0);
    }
    const int maxIdx = maxBoneIndex(isTrainingModel, isMetroMode);
    const size_t blobSize = static_cast<size_t>(maxIdx + 1) * 48;
    uint8_t blob[62 * 48];
    if (blobSize > sizeof(blob)) {
        return;
    }
    if (!vm_readv(cache.staticMesh, blob, blobSize)) {
        return;
    }
    for (int i = 0; i < 16; i++) {
        int j = 0;
        for (; j < i; j++) {
            if (bones[j] == bones[i]) {
                worldPos[i] = worldPos[j];
                break;
            }
        }
        if (j == i) {
            const float *oMat = reinterpret_cast<const float *>(blob + bones[i] * 48);
            struct D3DMatrix oMatrix = oMatrixFromBoneData(oMat);
            worldPos[i] = mat2Cord(oMatrix, baseMatrix);
        }
    }
}

struct ScreenProjector {
    Vec3 camLoc;
    Vec3 axisX, axisY, axisZ;
    float screenScale;
    float cx, cy;
    float offZ;

    explicit ScreenProjector(const CameraView &cam) {
        FMatrix tempMatrix = RotToMatrix(cam.Rotation);
        axisX = Vec3(tempMatrix.M[0][0], tempMatrix.M[0][1], tempMatrix.M[0][2]);
        axisY = Vec3(tempMatrix.M[1][0], tempMatrix.M[1][1], tempMatrix.M[1][2]);
        axisZ = Vec3(tempMatrix.M[2][0], tempMatrix.M[2][1], tempMatrix.M[2][2]);
        camLoc = cam.Location;
        cx = width / 2.0f;
        cy = height / 2.0f;
        float tanHalf = tanf(cam.FOV * ((float) PI / 360.0f));
        if (tanHalf < 0.01f) {
            offZ = 1.f;
            screenScale = cx;
        } else {
            offZ = 0.f;
            screenScale = cx / tanHalf;
        }
    }

    Vec3 project(const Vec3 &world) const {
        Vec3 delta = world - camLoc;
        float tz = Vec3::Dot(delta, axisX);
        if (tz < 1.0f) {
            Vec3 out{};
            out.Z = 1.f;
            out.X = cx;
            out.Y = cy;
            return out;
        }
        float tx = Vec3::Dot(delta, axisY);
        float ty = Vec3::Dot(delta, axisZ);
        Vec3 out{};
        out.Z = offZ;
        out.X = cx + tx * screenScale / tz;
        out.Y = cy - ty * screenScale / tz;
        return out;
    }

    Vec2 project2(const Vec3 &world) const {
        Vec3 s = project(world);
        Vec2 out{s.X, s.Y};
        return out;
    }
};

static uintptr_t sCachedCameraMgr = 0;
static uintptr_t sFastLocalPlayer = 0;
static uintptr_t sCachedShootEntity = 0;
static PlayerBoneWorld sSlotBoneWorld[maxplayerCount];
static Vec3 sLastRootPos[maxplayerCount];
static bool sHasLastRoot[maxplayerCount];
static Vec3 sBoneRootAnchor[maxplayerCount];
static bool sHasBoneRootAnchor[maxplayerCount];
static PlayerData sCamOnlyCache[maxplayerCount];
static uint32_t sBoneRefreshTick = 0;

static uintptr_t sBtTargetPawn = 0;
static uintptr_t sBonePawn[maxplayerCount];
static uint8_t sBoneFlags[maxplayerCount];
static int sBonePlayerCount = 0;
static BoneMeshCache sDrawBoneCache[maxplayerCount];

static BoneMeshCache &resolveBoneCacheForPawn(int slot, uintptr_t pBase) {
    BoneMeshCache &cache = sDrawBoneCache[slot];
    if (!isValid64(pBase)) {
        cache = BoneMeshCache{};
        return cache;
    }
    const uintptr_t mesh = getA(pBase + OffsetsAll64::Mesh);
    if (!cache.valid || cache.mesh != mesh) {
        cache = getBoneMeshCache(pBase);
    }
    return cache;
}

/** Keeps enemies visible between rotating actor-window scans (stops ESP blink). */
static uintptr_t sPersistPawn[maxplayerCount];
static PlayerData sPersistData[maxplayerCount];
static uint8_t sPersistFlags[maxplayerCount];
static uint8_t sPersistMiss[maxplayerCount];
static int sPersistCount = 0;

static void fillPlayerBoneWorld(PlayerBoneWorld &bw, const BoneMeshCache &cache, bool isTrainingModel,
                                bool isMetroMode);

static bool readActorWorldPos(uintptr_t actor, Vec3 &outPos) {
    if (!isValid64(actor)) {
        return false;
    }
    uintptr_t root = getA(actor + OffsetsAll64::RootComponent);
    if (!isValid64(root)) {
        return false;
    }
    vm_readv(root + OffsetsAll64::Position, &outPos, sizeof(outPos));
    return outPos.X != 0.f || outPos.Y != 0.f || outPos.Z != 0.f;
}

static bool getPlayerWorldPos(uintptr_t pBase, Vec3 &outPos) {
    uintptr_t vehicle = getA(pBase + OffsetsAll64::CurrentVehicle);
    if (isValid64(vehicle) && readActorWorldPos(vehicle, outPos)) {
        return true;
    }
    return readActorWorldPos(pBase, outPos);
}

static void syncLastRootPositions() {
    for (int i = 0; i < sBonePlayerCount && i < maxplayerCount; i++) {
        Vec3 p{};
        if (getPlayerWorldPos(sBonePawn[i], p)) {
            sLastRootPos[i] = p;
            sHasLastRoot[i] = true;
        }
    }
}

static void shiftBoneWorldByDelta(PlayerBoneWorld &bw, const Vec3 &delta) {
    if (!bw.isBone) {
        return;
    }
    for (int pi = 0; pi < 16; pi++) {
        bw.points[pi].X += delta.X;
        bw.points[pi].Y += delta.Y;
        bw.points[pi].Z += delta.Z;
    }
}

static void anchorBonesToRoot(int slot, uintptr_t pBase) {
    Vec3 root{};
    if (getPlayerWorldPos(pBase, root)) {
        sBoneRootAnchor[slot] = root;
        sHasBoneRootAnchor[slot] = true;
        sLastRootPos[slot] = root;
        sHasLastRoot[slot] = true;
    }
}

/** Every draw-sync: cheap root read + shift cached bones so skeleton tracks movement between bone passes. */
static void refreshCachedPlayerRootsForDraw(const Vec3 &myPos) {
    for (int i = 0; i < sBonePlayerCount && i < maxplayerCount; i++) {
        uintptr_t pBase = sBonePawn[i];
        if (!isValid64(pBase)) {
            continue;
        }
        Vec3 objPos{};
        if (!getPlayerWorldPos(pBase, objPos)) {
            continue;
        }
        sCamOnlyCache[i].Distance = getDistance(myPos, objPos);
        if (sSlotBoneWorld[i].isBone) {
            if (sHasBoneRootAnchor[i]) {
                const Vec3 delta = {
                        objPos.X - sBoneRootAnchor[i].X,
                        objPos.Y - sBoneRootAnchor[i].Y,
                        objPos.Z - sBoneRootAnchor[i].Z};
                const float d2 = delta.X * delta.X + delta.Y * delta.Y + delta.Z * delta.Z;
                if (d2 > 0.01f && d2 < 1600.f) {
                    shiftBoneWorldByDelta(sSlotBoneWorld[i], delta);
                } else if (d2 >= 1600.f) {
                    sSlotBoneWorld[i].isBone = false;
                    sHasBoneRootAnchor[i] = false;
                }
            }
            sBoneRootAnchor[i] = objPos;
            sHasBoneRootAnchor[i] = true;
        }
        sLastRootPos[i] = objPos;
        sHasLastRoot[i] = true;
    }
}

/** Cheap root read each draw-sync — update distance only (no bone shifting; shifting caused skeleton drift). */
static void refreshCachedPlayerPositionsOnly(const Vec3 &myPos) {
    for (int i = 0; i < sBonePlayerCount && i < maxplayerCount; i++) {
        uintptr_t pBase = sBonePawn[i];
        if (!isValid64(pBase)) {
            continue;
        }
        Vec3 objPos{};
        if (!getPlayerWorldPos(pBase, objPos)) {
            continue;
        }
        sCamOnlyCache[i].Distance = getDistance(myPos, objPos);
    }
}

static bool isPlayerKnocked(uintptr_t pBase) {
    if (!isValid64(pBase)) {
        return false;
    }
    float breath = 0.f;
    vm_readv(pBase + OffsetsAll64::NearDeathBreath, &breath, sizeof(breath));
    if (breath > 0.01f) {
        return true;
    }
    if (OffsetsAll64::NearDeathComponent != 0) {
        const uintptr_t ndc = getA(pBase + OffsetsAll64::NearDeathComponent);
        if (isValid64(ndc)) {
            return true;
        }
    }
    return false;
}

static bool isPlayerFullyDead(uintptr_t pBase) {
    if (!isValid64(pBase)) {
        return true;
    }
    // Knocked pawns can still have bDead set on some builds — keep them if breath/NDC alive.
    if (getI(pBase + OffsetsAll64::bDead) != 0) {
        return !isPlayerKnocked(pBase);
    }
    float hb[2] = {0.f, 0.f};
    vm_readv(pBase + OffsetsAll64::Health, hb, sizeof(hb));
    if (hb[0] <= 0.01f && hb[1] <= 0.01f) {
        return !isPlayerKnocked(pBase);
    }
    if (hb[1] > 0.01f && (hb[0] / hb[1] * 100.f) <= 0.01f) {
        return !isPlayerKnocked(pBase);
    }
    return false;
}

static bool isPlayerAlive(uintptr_t pBase) {
    return !isPlayerFullyDead(pBase);
}

/** Softer alive check for close combat — avoids flicker from noisy health reads. */
static bool isCloseRangeAlive(uintptr_t pBase, float distM) {
    if (!isValid64(pBase)) {
        return false;
    }
    if (getI(pBase + OffsetsAll64::bDead) != 0) {
        return isPlayerKnocked(pBase);
    }
    if (distM < 20.f) {
        return true;
    }
    if (distM > 55.f) {
        return isPlayerAlive(pBase);
    }
    float hb[2] = {0.f, 0.f};
    vm_readv(pBase + OffsetsAll64::Health, hb, sizeof(hb));
    if (hb[0] > 0.5f) {
        return true;
    }
    if (hb[1] > 0.01f && (hb[0] / hb[1] * 100.f) > 1.f) {
        return true;
    }
    return isPlayerKnocked(pBase);
}

static void mergePlayerKeepBones(PlayerData &dst, const PlayerData &src, const PlayerData &prev) {
    dst = src;
    if (!dst.BoneWorld.isBone && prev.BoneWorld.isBone) {
        dst.BoneWorld = prev.BoneWorld;
        dst.Bone.isBone = true;
    }
    if (strlen(dst.PlayerNameByte) < 2 && strlen(prev.PlayerNameByte) >= 2) {
        strncpy(dst.PlayerNameByte, prev.PlayerNameByte, sizeof(dst.PlayerNameByte) - 1);
        dst.PlayerNameByte[sizeof(dst.PlayerNameByte) - 1] = '\0';
    }
}

static void clearBoneSlotByPawn(uintptr_t pawn) {
    if (!isValid64(pawn)) {
        return;
    }
    for (int i = 0; i < sBonePlayerCount && i < maxplayerCount; i++) {
        if (sBonePawn[i] == pawn) {
            sSlotBoneWorld[i] = PlayerBoneWorld{};
            sHasBoneRootAnchor[i] = false;
            sDrawBoneCache[i] = BoneMeshCache{};
            sCamOnlyCache[i] = PlayerData{};
            sCamOnlyCache[i].actorAddr = pawn;
        }
    }
}

static void evictPersistPawn(uintptr_t pawn) {
    if (!isValid64(pawn)) {
        return;
    }
    clearBoneSlotByPawn(pawn);
    for (int p = 0; p < sPersistCount; p++) {
        if (sPersistPawn[p] == pawn) {
            sPersistMiss[p] = 99;
            sPersistData[p].BoneWorld.isBone = false;
            sPersistData[p].Bone.isBone = false;
            break;
        }
    }
}

static void rebuildPlayersFromPersist(Response &response, const Vec3 &myPos) {
    const int found = response.PlayerCount;
    bool matched[maxplayerCount];
    for (int p = 0; p < sPersistCount; p++) {
        matched[p] = false;
    }

    for (int r = 0; r < found; r++) {
        uintptr_t pawn = sBonePawn[r];
        if (!isValid64(pawn)) {
            continue;
        }
        int slot = -1;
        for (int p = 0; p < sPersistCount; p++) {
            if (sPersistPawn[p] == pawn) {
                slot = p;
                break;
            }
        }
        if (slot < 0 && sPersistCount < maxplayerCount) {
            slot = sPersistCount++;
            sPersistPawn[slot] = pawn;
            sPersistFlags[slot] = sBoneFlags[r];
            sPersistMiss[slot] = 0;
        }
        if (slot >= 0) {
            mergePlayerKeepBones(sPersistData[slot], response.Players[r], sPersistData[slot]);
            sPersistData[slot].actorAddr = pawn;
            sPersistFlags[slot] = sBoneFlags[r];
            sPersistMiss[slot] = 0;
            matched[slot] = true;
        }
    }

    Vec3 objPos{};
    for (int p = 0; p < sPersistCount; p++) {
        uintptr_t pawn = sPersistPawn[p];
        float distM = sPersistData[p].Distance;
        if (getPlayerWorldPos(pawn, objPos)) {
            distM = getDistance(myPos, objPos);
            sPersistData[p].Distance = distM;
        }
        if (!isCloseRangeAlive(pawn, distM)) {
            if (distM < 30.f && !isPlayerFullyDead(pawn)) {
                sPersistMiss[p] = 0;
                continue;
            }
            sPersistMiss[p] = 99;
            continue;
        }
        if (matched[p]) {
            continue;
        }
        if (distM < 300.f) {
            sPersistMiss[p] = 0;
            continue;
        }
        sPersistMiss[p]++;
        if (sPersistMiss[p] > 48) {
            sPersistMiss[p] = 99;
        }
    }

    for (int i = 0; i < sPersistCount; i++) {
        if (sPersistMiss[i] >= 99) {
            continue;
        }
        for (int j = i + 1; j < sPersistCount; j++) {
            if (sPersistMiss[j] >= 99) {
                continue;
            }
            if (sPersistPawn[i] == sPersistPawn[j]) {
                sPersistMiss[j] = 99;
            }
        }
    }

    int out = 0;
    for (int p = 0; p < sPersistCount; p++) {
        if (sPersistMiss[p] >= 99) {
            continue;
        }
        if (out >= maxplayerCount) {
            break;
        }
        response.Players[out] = sPersistData[p];
        response.Players[out].actorAddr = sPersistPawn[p];
        sBonePawn[out] = sPersistPawn[p];
        sBoneFlags[out] = sPersistFlags[p];
        out++;
    }
    response.PlayerCount = out;
    dedupePlayerList(response);

    int compact = 0;
    for (int p = 0; p < sPersistCount; p++) {
        if (sPersistMiss[p] >= 99) {
            continue;
        }
        if (compact != p) {
            sPersistPawn[compact] = sPersistPawn[p];
            sPersistData[compact] = sPersistData[p];
            sPersistFlags[compact] = sPersistFlags[p];
            sPersistMiss[compact] = sPersistMiss[p];
        }
        compact++;
    }
    sPersistCount = compact;
}

static size_t safeNameLen(const char *name, size_t maxLen) {
    if (!name || maxLen == 0) {
        return 0;
    }
    return strnlen(name, maxLen);
}

static bool isPlayerPawnActorName(const char *name) {
    if (!name || name[0] == '\0') {
        return false;
    }
    if (strncmp(name, "BP_Player", 9) == 0) {
        return true;
    }
    return strstr(name, "PlayerPawn") != nullptr || strstr(name, "PlayerCharacter") != nullptr ||
           strstr(name, "CharacterModelTaget") != nullptr || strstr(name, "STExtraCharacter") != nullptr ||
           strstr(name, "STExtraPlayer") != nullptr || strstr(name, "LobbyPawn") != nullptr;
}

static bool isExcludedWorldActorName(const char *name) {
    if (safeNameLen(name, 100) < 3) {
        return true;
    }
    return strstr(name, "Projectile") != nullptr || strstr(name, "Bullet") != nullptr ||
           strstr(name, "Impact") != nullptr || strstr(name, "Hit") != nullptr ||
           strstr(name, "Effect") != nullptr || strstr(name, "Decal") != nullptr ||
           strstr(name, "Shell") != nullptr || strstr(name, "Trail") != nullptr ||
           strstr(name, "Damage") != nullptr || strstr(name, "Blood") != nullptr;
}

static bool isLocalPlayerPawn(uintptr_t pBase, uintptr_t localPlayer, uintptr_t myCharacter,
                              uint32_t role) {
    if (role == 258) {
        return true;
    }
    if (isValid64(localPlayer) && pBase == localPlayer) {
        return true;
    }
    if (isValid64(myCharacter) && pBase == myCharacter) {
        return true;
    }
    return false;
}

static bool isEnemyPlayerActor(const char *name) {
    if (isExcludedWorldActorName(name)) {
        return false;
    }
    return isPlayerPawnActorName(name);
}

static void fillEspCamera(EspCameraView &out, const CameraView &cam) {
    out.locX = cam.Location.X;
    out.locY = cam.Location.Y;
    out.locZ = cam.Location.Z;
    out.rotPitch = cam.Rotation.Pitch;
    out.rotYaw = cam.Rotation.Yaw;
    out.rotRoll = cam.Rotation.Roll;
    out.fov = cam.FOV;
    out.valid = true;
}

static void setEncodedName(char *dst, size_t dstLen, const char *encoded) {
    if (!dst || dstLen == 0 || !encoded) {
        return;
    }
    strncpy(dst, encoded, dstLen - 1);
    dst[dstLen - 1] = '\0';
    // Trailing ':' becomes "" after Java split → Integer.parseInt crash
    size_t n = strlen(dst);
    while (n > 0 && dst[n - 1] == ':') {
        dst[--n] = '\0';
    }
}

/** True when string is only digit chars (player UID mistaken as display name). */
static bool looksLikeNumericId(const char *text) {
    if (!text || text[0] == '\0') {
        return true;
    }
    if (strchr(text, ':')) {
        int parts = 0;
        for (const char *p = text; *p; ) {
            char *end = nullptr;
            const long v = strtol(p, &end, 10);
            if (end == p) {
                return false;
            }
            if (v < 48 || v > 57) {
                return false;
            }
            parts++;
            if (*end == ':') {
                end++;
            }
            if (*end == '\0') {
                break;
            }
            p = end;
        }
        return parts >= 6;
    }
    int digits = 0;
    for (const char *p = text; *p; p++) {
        if (*p < '0' || *p > '9') {
            return false;
        }
        digits++;
    }
    return digits >= 6;
}

static bool tryStorePlayerString(char *out, size_t outLen, const char *candidate) {
    if (!candidate || strlen(candidate) < 2 || looksLikeNumericId(candidate)) {
        return false;
    }
    setEncodedName(out, outLen, candidate);
    return true;
}

/** Read UE FString at pawn+fieldOff — wchar data pointer, not object header. */
static bool readPlayerFStringField(uintptr_t pawn, uintptr_t fieldOff, char *out, size_t outLen) {
    if (!isValid64(pawn) || outLen == 0) {
        return false;
    }
    out[0] = '\0';
    const uintptr_t fstrInline = pawn + fieldOff;
    const uintptr_t layer0 = getA(fstrInline);

    if (isValid64(layer0)) {
        const uintptr_t wcharData = getA(layer0);
        if (isValid64(wcharData) && tryStorePlayerString(out, outLen, getNameByte(wcharData))) {
            return true;
        }
        if (tryStorePlayerString(out, outLen, getNameByte(layer0))) {
            return true;
        }
        char *ansi = getText(layer0);
        if (ansi && tryStorePlayerString(out, outLen, ansi)) {
            return true;
        }
    }
    char *ansiInline = getText(fstrInline);
    if (ansiInline && tryStorePlayerString(out, outLen, ansiInline)) {
        return true;
    }
    if (isValid64(layer0) && tryStorePlayerString(out, outLen, getNameByte(getA(layer0)))) {
        return true;
    }
    return false;
}

static bool readActorIsAI(uintptr_t pBase) {
    if (!isValid64(pBase)) {
        return false;
    }
    uint8_t flag = 0;
    vm_readv(pBase + OffsetsAll64::bIsAI, &flag, sizeof(flag));
    return flag != 0;
}

/** Bot from actor class name or bIsAI (bots use fake display names in newer builds). */
static bool isActorLikelyBot(uintptr_t pBase, const char *actorName) {
    if (actorName && actorName[0] != '\0') {
        if (strstr(actorName, "TPlanAI") || strstr(actorName, "PlanAI") ||
            strstr(actorName, "PlayerPawn_AI") || strstr(actorName, "AIPawn") ||
            strstr(actorName, "BotPawn") || strstr(actorName, "FakePlayer") ||
            strstr(actorName, "CharacterModelTaget")) {
            return true;
        }
    }
    return readActorIsAI(pBase);
}

static void copyEnemyIdentity(PlayerData *data, uintptr_t pBase, const char *actorName,
                              int persistSlot) {
    data->isBot = isActorLikelyBot(pBase, actorName);
    if (data->isBot) {
        setEncodedName(data->PlayerNameByte, sizeof(data->PlayerNameByte),
                       OBFUSCATE("82:79:66:79:84:"));
        setEncodedName(data->PlayerNation, sizeof(data->PlayerNation),
                       OBFUSCATE("69:999:000:"));
        setEncodedName(data->PlayerUID, sizeof(data->PlayerUID),
                       OBFUSCATE("66:111:116:"));
        return;
    }
    if (!readPlayerFStringField(pBase, OffsetsAll64::PlayerName, data->PlayerNameByte,
                                sizeof(data->PlayerNameByte))) {
        if (persistSlot >= 0 && strlen(sPersistData[persistSlot].PlayerNameByte) >= 2 &&
            !sPersistData[persistSlot].isBot &&
            !looksLikeNumericId(sPersistData[persistSlot].PlayerNameByte)) {
            setEncodedName(data->PlayerNameByte, sizeof(data->PlayerNameByte),
                           sPersistData[persistSlot].PlayerNameByte);
        } else {
            setEncodedName(data->PlayerNameByte, sizeof(data->PlayerNameByte),
                           OBFUSCATE("80:108:97:121:101:114:"));
        }
    }
    if (!readPlayerFStringField(pBase, OffsetsAll64::Nation, data->PlayerNation,
                                sizeof(data->PlayerNation))) {
        if (persistSlot >= 0 && strlen(sPersistData[persistSlot].PlayerNation) >= 2) {
            setEncodedName(data->PlayerNation, sizeof(data->PlayerNation),
                           sPersistData[persistSlot].PlayerNation);
        }
    }
    readPlayerFStringField(pBase, OffsetsAll64::PlayerUID, data->PlayerUID,
                           sizeof(data->PlayerUID));
    if (strlen(data->PlayerUID) < 2 && persistSlot >= 0 &&
        strlen(sPersistData[persistSlot].PlayerUID) >= 2) {
        setEncodedName(data->PlayerUID, sizeof(data->PlayerUID),
                       sPersistData[persistSlot].PlayerUID);
    }
}

static bool isBoneWorldPlausible(const PlayerBoneWorld &bw, const Vec3 &myPos) {
    if (!bw.isBone) {
        return false;
    }
    const Vec3 &head = bw.points[0];
    if (head.X == 0.f && head.Y == 0.f && head.Z == 0.f) {
        return false;
    }
    return getDistance(myPos, head) <= 400.f;
}

static void fillPlayerBoneWorld(PlayerBoneWorld &bw, const BoneMeshCache &cache, bool isTrainingModel,
                                bool isMetroMode) {
    bw.isBone = false;
    if (!cache.valid) {
        return;
    }
    int bones[16] = {6, 5, 5, 1, 12, 33, 13, 34, 14, 35, 53, 57, 54, 58, 55, 59};
    if (isMetroMode) {
        int metroBones[16] = {6, 5, 5, 1, 8, 14, 9, 15, 10, 16, 19, 22, 20, 23, 21, 24};
        std::copy(std::begin(metroBones), std::end(metroBones), std::begin(bones));
    } else if (isTrainingModel) {
        int trainingBones[16] = {6, 5, 4, 1, 14, 35, 15, 36, 16, 37, 55, 59, 56, 60, 57, 61};
        std::copy(std::begin(trainingBones), std::end(trainingBones), std::begin(bones));
    }
    Vec3 worldPos[16];
    readBoneWorldsBatch(cache, bones, isTrainingModel, isMetroMode, worldPos);
    for (int i = 0; i < 16; i++) {
        bw.points[i] = worldPos[i];
    }
    bw.isBone = true;
}

static PlayerBone projectBoneWorld(const PlayerBoneWorld &bw, const CameraView &projCam, Vec3 &outHeadScreen) {
    PlayerBone b{};
    if (!bw.isBone) {
        return b;
    }
    b.isBone = true;
    ScreenProjector proj(projCam);
    outHeadScreen = proj.project(bw.points[0]);
    b.head = Vec2{outHeadScreen.X, outHeadScreen.Y};
    b.neck = proj.project2(bw.points[1]);
    b.cheast = proj.project2(bw.points[2]);
    b.pelvis = proj.project2(bw.points[3]);
    b.lSh = proj.project2(bw.points[4]);
    b.rSh = proj.project2(bw.points[5]);
    b.lElb = proj.project2(bw.points[6]);
    b.rElb = proj.project2(bw.points[7]);
    b.lWr = proj.project2(bw.points[8]);
    b.rWr = proj.project2(bw.points[9]);
    b.lTh = proj.project2(bw.points[10]);
    b.rTh = proj.project2(bw.points[11]);
    b.lKn = proj.project2(bw.points[12]);
    b.rKn = proj.project2(bw.points[13]);
    b.lAn = proj.project2(bw.points[14]);
    b.rAn = proj.project2(bw.points[15]);
    return b;
}

// Overlay draws skeleton/box from PlayerBone screen coords — always project BoneWorld → Bone.
static void projectAllPlayerBones(Response &resp, const CameraView &cam) {
    for (int i = 0; i < resp.PlayerCount && i < maxplayerCount; i++) {
        PlayerData &pl = resp.Players[i];
        if (!pl.BoneWorld.isBone) {
            pl.Bone.isBone = false;
            continue;
        }
        Vec3 headScreen{};
        pl.Bone = projectBoneWorld(pl.BoneWorld, cam, headScreen);
        if (pl.Bone.isBone && headScreen.Z != 1.f) {
            pl.HeadLocation = headScreen;
        }
    }
}

static void refreshCameraView(uintptr_t camMgr) {
    if (camMgr) {
        cameraView = getCameraView(camMgr + OffsetsAll64::CameraCache + OffsetsAll64::POV);
        sCachedCameraMgr = camMgr;
    }
}

static float calcAimDistOk(float worldDist) {
    if (worldDist > 350.f) return worldDist / 5.f;
    if (worldDist > 270.f) return worldDist / 8.f;
    if (worldDist > 180.f) return worldDist / 16.f;
    if (worldDist > 80.f) return 1.f;
    return 0.f;
}

static float calcScopeScreenPitchAdj(float scopeFov, float worldDist, int activeRecoil,
                                     bool useCustomScope, const float customRecScope[9]) {
    const float ok = calcAimDistOk(worldDist);
    if (!useCustomScope) {
        return GetPitch(scopeFov, (float) activeRecoil, ok);
    }
    return GetPitchCustom(scopeFov, (float) activeRecoil, ok, const_cast<float *>(customRecScope));
}

static void readBoneWorlds(const BoneMeshCache &cache, const int bones[16], bool isTrainingModel,
                           bool isMetroMode, Vec3 worldPos[16]) {
    readBoneWorldsBatch(cache, bones, isTrainingModel, isMetroMode, worldPos);
}

static bool isOnScreen(const Vec3 &screen, float margin) {
    return screen.Z != 1.0f && screen.X > -margin && screen.X < width + margin;
}

struct Vec3 GetBoneFromIndx(uintptr_t actor, int idx) {
    uintptr_t boneAddr = getA(actor + OffsetsAll64::Mesh);
    struct D3DMatrix baseMatrix = getOMatrix(boneAddr + OffsetsAll64::FixAttachInfoList + 0x10);
   // boneAddr = getA(boneAddr + OffsetsAll64::MasterPoseComponent + 0x8);
   boneAddr = getA(boneAddr + OffsetsAll64::StaticMesh);
    struct D3DMatrix oMatrix = getOMatrix(boneAddr + (idx * 48));
    return mat2Cord(oMatrix, baseMatrix);
}

PlayerWeapon getPlayerWeapon(uintptr_t playerPawn) {
    PlayerWeapon p;
    if (!isValid64(playerPawn)) {
        return p;
    }
    uintptr_t weaponBase = getA(getA(playerPawn + OffsetsAll64::WeaponManagerComponent) +
                                OffsetsAll64::CurrentWeaponReplicated);
    if (!isValid64(weaponBase)) {
        return p;
    }
    uintptr_t addr[8];
    vm_readv(getA(weaponBase + OffsetsAll64::Children), addr, sizeof(addr));
    for (int i = 0; i < (sizeof(addr) / sizeof(uintptr_t)); i++) {
        if (isValid64(addr[i]) && getI(addr[i] + OffsetsAll64::DrawShootLineTime) == 2) {
            p.isWeapon = true;
            p.id = getI(getA(addr[i] + OffsetsAll64::WeaponEntityComp) + OffsetsAll64::WeaponId);
            p.ammo = getI(addr[i] + OffsetsAll64::CurBulletNumInClip);
            break;
        }
    }
    return p;
}

static int readLocalClipAmmo(uintptr_t localPlayer) {
    if (!isValid64(localPlayer)) {
        return -1;
    }
    PlayerWeapon pw = getPlayerWeapon(localPlayer);
    if (pw.isWeapon && pw.ammo >= 0 && pw.ammo <= 500) {
        return pw.ammo;
    }
    uintptr_t weaponBase = getA(getA(localPlayer + OffsetsAll64::WeaponManagerComponent) +
                                OffsetsAll64::CurrentWeaponReplicated);
    if (!isValid64(weaponBase)) {
        return -1;
    }
    uintptr_t shootEntity = getA(weaponBase + OffsetsAll64::ShootWeaponEntityComp);
    if (isValid64(shootEntity)) {
        int ammo = getI(shootEntity + OffsetsAll64::CurBulletNumInClip);
        if (ammo >= 0 && ammo <= 500) {
            return ammo;
        }
    }
    uintptr_t addrs[8] = {};
    uintptr_t children = getA(weaponBase + OffsetsAll64::Children);
    if (!isValid64(children)) {
        return -1;
    }
    vm_readv(children, addrs, sizeof(addrs));
    for (int i = 0; i < 8; i++) {
        if (!isValid64(addrs[i])) {
            continue;
        }
        if (getI(addrs[i] + OffsetsAll64::DrawShootLineTime) == 2) {
            int ammo = getI(addrs[i] + OffsetsAll64::CurBulletNumInClip);
            if (ammo >= 0 && ammo <= 500) {
                return ammo;
            }
        }
    }
    return -1;
}

/**
 * Duty-cycle gate for bullet track: opens a short window each period so only
 * roughly 1 in 5-6 rounds is tracked (AR fire interval ~90-110 ms). Stateless,
 * so multiple calls per frame from different sync paths stay consistent.
 */
static void tickBulletShotCounter(uintptr_t localPlayer, bool &allowTrack) {
    allowTrack = false;
    if (!gEnable || !isValid64(localPlayer)) {
        return;
    }
    const uint64_t nowMs = espMonotonicMs();
    const uint64_t periodMs = 550;
    const uint64_t windowMs = 110;
    allowTrack = (nowMs % periodMs) < windowMs;
}

static Vec3 predictTargetPos(Vec3 loc, uintptr_t pBase, Vec3 myPos, float bulletSpeed) {
    if (bulletSpeed == 0) {
        return loc;
    }
    uintptr_t movementComponent = getA(pBase + OffsetsAll64::CharacterMovement);
    uintptr_t currentVehicle = getA(pBase + OffsetsAll64::CurrentVehicle);
    if (currentVehicle) {
        Vec3 linearVelocity = getVec3(currentVehicle + OffsetsAll64::ReplicatedMovement);
        float dist = getDistance(myPos, loc);
        float timeToTravel = dist / bulletSpeed;
        return Add_VectorVector(loc, Multiply_VectorFloat(linearVelocity, timeToTravel));
    }
    Vec3 vel = getVec3(movementComponent + OffsetsAll64::Velocity);
    float dist = getDistance(myPos, loc);
    float timeToTravel = dist / bulletSpeed;
    return Add_VectorVector(loc, Multiply_VectorFloat(vel, timeToTravel));
}


static PlayerBone getPlayerBone(const BoneMeshCache &cache, const CameraView &projCam, bool isTrainingModel,
                                bool isMetroMode, PlayerBoneWorld &boneWorld, Vec3 &outHeadScreen) {
    fillPlayerBoneWorld(boneWorld, cache, isTrainingModel, isMetroMode);
    if (!boneWorld.isBone) {
        PlayerBone b{};
        b.isBone = false;
        return b;
    }
    return projectBoneWorld(boneWorld, projCam, outHeadScreen);
}

struct PawnComponent {
    uintptr_t Controller;
    PawnComponent (uintptr_t pBase) {
        Controller = getA(pBase + OffsetsAll64::Controller);
    }
    void setShowDamage() {
        Write(Controller + OffsetsAll64::GameReplayType, "2", TYPE_DWORD);
    }
    bool isValid() {
        return (Controller != 0);
    }
};


struct ShootWeaponBase {
    uintptr_t FromBase;
    uintptr_t Base;
    uintptr_t ShootWeaponEntity;
    
    uintptr_t CurrentVehicle;
    int bIsWeaponFiring;

    ShootWeaponBase(uintptr_t pBase) {
        FromBase = getA(pBase + OffsetsAll64::WeaponManagerComponent);
        Base = getA(FromBase + OffsetsAll64::CurrentWeaponReplicated);
        ShootWeaponEntity = getA(Base + OffsetsAll64::ShootWeaponEntityComp);
        bIsWeaponFiring = getI(pBase + OffsetsAll64::bIsWeaponFiring);
        
        CurrentVehicle = getA(pBase + OffsetsAll64::CurrentVehicle);

    }

    void bypassBT() {
        Write2<char>(Base + OffsetsAll64::ShootMode, 2);
        Write2<char>(Base + OffsetsAll64::ShootMode, 2);
    }

    void setMagic(){
        
    }

    void setLessRecoil() {
        constexpr int offsets[] = {0x50, 0x54, 0x58};
        for (const auto& offset : offsets) {
            if (getF(ShootWeaponEntity + OffsetsAll64::SRecoilInfo + offset) != 0) {
                Write(ShootWeaponEntity + OffsetsAll64::SRecoilInfo + offset, "0", TYPE_FLOAT);
            }
        }
    }

    void FixBT(){
         Write(ShootWeaponEntity + OffsetsAll64::BulletTrackDistanceFix, "1000.0", TYPE_FLOAT);
         Write(ShootWeaponEntity + OffsetsAll64::MaxBulletImpactFXClampDistance, "1000.0", TYPE_FLOAT);
    }

    void setHitX() {
        if (getF(ShootWeaponEntity + OffsetsAll64::ExtraHitPerformScale) != 50) {
            Write(ShootWeaponEntity + OffsetsAll64::ExtraHitPerformScale, "50", TYPE_FLOAT);
        }
    }
    
    void setNoShake() {
        if (getI(ShootWeaponEntity + OffsetsAll64::AnimationKick) != 0) {
             Write(ShootWeaponEntity + OffsetsAll64::AnimationKick, "0", TYPE_FLOAT);
        }
    }

    void setFastSwitchWeapon() {
        if (getI(ShootWeaponEntity + OffsetsAll64::SwitchWeaponSpeedScale) != 100) {
             Write(ShootWeaponEntity + OffsetsAll64::SwitchWeaponSpeedScale, "100", TYPE_FLOAT);
        }
    }
    
    void setInstantHit() {
        if (getI(ShootWeaponEntity + OffsetsAll64::BulletFireSpeed) >= 900000) {
             Write(ShootWeaponEntity + OffsetsAll64::BulletFireSpeed, "900000", TYPE_FLOAT);
        }
    }
    
    void setFastShootInterval() {
        if (getF(ShootWeaponEntity + OffsetsAll64::ShootInterval) != 0.048000) {
            Write(ShootWeaponEntity + OffsetsAll64::ShootInterval, "0.048000", TYPE_FLOAT);
        }
    }

    void FastVehicle(){
        int bIsEngineStarted = getI(CurrentVehicle + OffsetsAll64::bIsEngineStarted);
        if (bIsEngineStarted != 0){
            Write(CurrentVehicle + OffsetsAll64::ExtraBoostFactor, "400",TYPE_FLOAT);
        }
    }
 
 
     void setAimbot() {
        
    }
    
    
    

    bool isFiring() const {
        return (bIsWeaponFiring != 0);
    }

    bool isValid() const {
        return (Base != 0);
    }
};

/**
 * One complete bullet-track tick. Called from every path that used to poke the
 * BT patch. It is fully self-contained and crash-safe:
 *   - code patched once via btApplyCodeOnce / restored once via btRestoreCode
 *   - only the aim DATA slot is written per frame, always a finite coordinate
 *   - throttle (allowTrack) just selects head vs. crosshair — never a code write
 * outNearest/outTarget report the locked enemy (or -1) for callers/IPC.
 */
static void syncBulletTrackFast(uintptr_t ue4Base, const Request &req, const CameraView &cam,
                                const Vec3 &myPos, bool allowTrack,
                                float &outNearest, Vec3 &outTarget) {
    outNearest = -1.f;
    outTarget = {};
    gEnable = (req.options.aimBullet == 0);

    if (!gEnable || vngver || !isValid64(sFastLocalPlayer) || ue4Base == 0) {
        btRestoreCode(ue4Base);
        Trigger = false;
        publishBulletTrackIpc(false, -1.f, outTarget);
        return;
    }

    btApplyCodeOnce(ue4Base);

    // Safe data-only weapon fixes (never touch executable code).
    ShootWeaponBase btWeapon(sFastLocalPlayer);
    if (btWeapon.isValid()) {
        btWeapon.FixBT();
        btWeapon.bypassBT();
    }

    const float aimRadius = req.options.aimingRange > 0 ? (float) req.options.aimingRange : 250.f;
    float aimDist = req.options.aimingDist > 0 ? (float) req.options.aimingDist : 300.f;
    if (aimDist < 80.f) {
        aimDist = 300.f;
    }
    const int aimWhen = req.options.aimingState > 0 ? req.options.aimingState : 3;
    const bool aimKnoced = req.options.pour;
    const float aimSpeed = req.options.aimingSpeed > 0 ? (float) req.options.aimingSpeed : 660.f;

    bool trigger = false;
    if (aimWhen == 3) {
        trigger = true;
    } else if (aimWhen == 1) {
        trigger = getI(sFastLocalPlayer + OffsetsAll64::bIsWeaponFiring) != 0;
    } else if (aimWhen == 2) {
        trigger = getI(sFastLocalPlayer + OffsetsAll64::bIsGunADS) != 0;
    }
    Trigger = trigger;

    // Always resolve the nearest valid enemy so callers/IPC know the lock state
    // and so distance is available for the crosshair fallback point.
    const ScreenProjector proj(cam);
    const int localTeam = getI(sFastLocalPlayer + OffsetsAll64::TeamID);
    const int n = sBonePlayerCount < maxplayerCount ? sBonePlayerCount : maxplayerCount;
    float best = -1.f;
    Vec3 bestLoc{};
    float bestDist3d = 0.f;
    uintptr_t bestPawn = 0;

    for (int i = 0; i < n; i++) {
        uintptr_t pBase = sBonePawn[i];
        if (!isValid64(pBase) || !isPlayerAlive(pBase)) {
            continue;
        }
        if (req.options.ignoreAi) {
            uint8_t isBot = 0;
            vm_readv(pBase + OffsetsAll64::bIsAI, &isBot, sizeof(isBot));
            if (isBot) {
                continue;
            }
        }
        const int teamId = getI(pBase + OffsetsAll64::TeamID);
        if (localTeam > 0 && teamId > 0 && teamId == localTeam) {
            continue;
        }

        float hb[2] = {0.f, 0.f};
        vm_readv(pBase + OffsetsAll64::Health, hb, sizeof(hb));
        const float healthPct = hb[1] > 0.01f ? hb[0] / hb[1] * 100.f : hb[0];
        if (!(aimKnoced || healthPct > 0.f)) {
            continue;
        }

        Vec3 objPos{};
        if (!getPlayerWorldPos(pBase, objPos)) {
            continue;
        }
        const float dist3d = getDistance(myPos, objPos);
        if (dist3d <= 0.f || dist3d > aimDist) {
            continue;
        }

        Vec3 headWorld = objPos + Vec3(0.f, 0.f, 80.f);
        if (sSlotBoneWorld[i].isBone) {
            headWorld = sSlotBoneWorld[i].points[0];
        }
        const Vec3 headScr = proj.project(headWorld);
        if (headScr.Z == 1.f) {
            continue;
        }
        const float cx = headScr.X - (float) width / 2.f;
        const float cy = headScr.Y - (float) height / 2.f;
        const float centerDist = sqrtf(cx * cx + cy * cy);
        if (centerDist >= aimRadius) {
            continue;
        }

        if (best < 0.f || centerDist < best) {
            best = centerDist;
            bestPawn = pBase;
            bestDist3d = dist3d;
            // Head-only bullet track (mesh bone index 6 = head)
            bestLoc = GetBoneFromIndx(pBase, 6);
            if (aimSpeed > 0.f) {
                bestLoc = predictTargetPos(bestLoc, pBase, myPos, aimSpeed);
            }
        }
    }

    const bool haveTarget = (bestPawn != 0 && best >= 0.f && btFiniteVec(bestLoc));
    if (haveTarget) {
        outNearest = best;
        outTarget = bestLoc;
        sBtTargetPawn = bestPawn;
    }

    // Decide the coordinate for THIS frame. Head only when we are triggered,
    // inside the throttle window, and actually have a locked enemy. Otherwise
    // send the shot down the crosshair (no assist) with a sane coordinate.
    const bool trackNow = trigger && allowTrack && haveTarget;
    const Vec3 aim = trackNow ? bestLoc : btCrosshairPoint(cam, bestDist3d);
    btWriteAim(ue4Base, aim);

    publishBulletTrackIpc(trackNow, outNearest, outTarget);
}

static void refreshCachedPlayerBones(Response &out, const Vec3 &myPos) {
    int liveCount = 0;
    const int n = sBonePlayerCount < maxplayerCount ? sBonePlayerCount : maxplayerCount;
    const bool halfRate = false;
    (void) sBoneRefreshTick;
    ++sBoneRefreshTick;
    const int phase = (int) (sBoneRefreshTick & 1u);
    for (int bi = 0; bi < n; bi++) {
        uintptr_t pBase = sBonePawn[bi];
        if (isPlayerFullyDead(pBase)) {
            evictPersistPawn(pBase);
            continue;
        }
        if (liveCount != bi) {
            sBonePawn[liveCount] = pBase;
            sBoneFlags[liveCount] = sBoneFlags[bi];
            sDrawBoneCache[liveCount] = sDrawBoneCache[bi];
            sSlotBoneWorld[liveCount] = sSlotBoneWorld[bi];
        }
        PlayerData *data = &out.Players[liveCount];
        *data = sCamOnlyCache[bi];
        data->actorAddr = pBase;
        const bool isTrainingModel = (sBoneFlags[liveCount] & 1) != 0;
        const bool isMetroMode = (sBoneFlags[liveCount] & 2) != 0;

        if (halfRate && (bi & 1) != phase && sSlotBoneWorld[bi].isBone) {
            data->BoneWorld = sSlotBoneWorld[bi];
            data->Bone.isBone = true;
            data->HeadLocation = ScreenProjector(cameraView).project(data->BoneWorld.points[0]);
            Vec3 worldPos{};
            if (getPlayerWorldPos(pBase, worldPos)) {
                data->Distance = getDistance(myPos, worldPos);
            }
            liveCount++;
            continue;
        }

        BoneMeshCache &boneCache = sDrawBoneCache[liveCount];
        if (boneCache.mesh == 0 || boneCache.staticMesh == 0) {
            boneCache = getBoneMeshCache(pBase);
        } else {
            uintptr_t mesh = getA(pBase + OffsetsAll64::Mesh);
            if (mesh != boneCache.mesh) {
                boneCache = getBoneMeshCache(pBase);
            } else {
                boneCache.valid = isValid64(boneCache.staticMesh);
            }
        }
        Vec3 worldPos{};
        const bool hasWorldPos = getPlayerWorldPos(pBase, worldPos);
        if (boneCache.valid) {
            fillPlayerBoneWorld(data->BoneWorld, boneCache, isTrainingModel, isMetroMode);
            if (data->BoneWorld.isBone) {
                data->HeadLocation =
                        ScreenProjector(cameraView).project(data->BoneWorld.points[0]);
                sSlotBoneWorld[liveCount] = data->BoneWorld;
                sCamOnlyCache[liveCount] = *data;
                anchorBonesToRoot(liveCount, pBase);
            }
        }
        if ((!boneCache.valid || !data->BoneWorld.isBone) && hasWorldPos) {
            data->HeadLocation = ScreenProjector(cameraView).project(
                    worldPos + Vec3(0.f, 0.f, 120.f));
        }
        if (hasWorldPos) {
            data->Distance = getDistance(myPos, worldPos);
        }
        liveCount++;
    }
    sBonePlayerCount = liveCount;
    out.PlayerCount = liveCount;
    syncLastRootPositions();
}

static void snapshotPlayersForCamOnly(const Response &src, const Vec3 &myPos) {
    const int n = src.PlayerCount < maxplayerCount ? src.PlayerCount : maxplayerCount;
    sBonePlayerCount = n;
    for (int i = 0; i < n; i++) {
        const uintptr_t newPawn = src.Players[i].actorAddr;
        if (sBonePawn[i] != newPawn) {
            sHasLastRoot[i] = false;
            sHasBoneRootAnchor[i] = false;
            sDrawBoneCache[i] = BoneMeshCache{};
            sSlotBoneWorld[i] = PlayerBoneWorld{};
        }
        sCamOnlyCache[i] = src.Players[i];
        sBonePawn[i] = newPawn;
        if (src.Players[i].BoneWorld.isBone) {
            sSlotBoneWorld[i] = src.Players[i].BoneWorld;
        } else {
            sSlotBoneWorld[i] = PlayerBoneWorld{};
        }
        (void) myPos;
    }
}

/** InitMode fast path: health/dist/vis for tracked pawns — no actor scan, no bone reads. */
static void refreshCachedPlayerMetadataLight(Response &out, const Vec3 &myPos, bool needVisCheck,
                                             float localRenderTime) {
    const ScreenProjector proj(cameraView);
    int live = 0;
    const int n = sBonePlayerCount < maxplayerCount ? sBonePlayerCount : maxplayerCount;
    for (int i = 0; i < n; i++) {
        uintptr_t pBase = sBonePawn[i];
        if (!isValid64(pBase)) {
            continue;
        }
        if (isPlayerFullyDead(pBase)) {
            evictPersistPawn(pBase);
            continue;
        }
        PlayerData *data = &out.Players[live];
        *data = sCamOnlyCache[i];
        data->actorAddr = pBase;

        float hb[2] = {0.f, 0.f};
        vm_readv(pBase + OffsetsAll64::Health, hb, sizeof(hb));
        if (hb[1] > 0.01f) {
            data->Health = hb[0] / hb[1] * 100.f;
        } else {
            data->Health = hb[0];
        }
        vm_readv(pBase + OffsetsAll64::NearDeathBreath, &data->Healthy, sizeof(data->Healthy));
        // Knocked: Health often 0 — keep ESP visible using breath as HP %.
        if (data->Healthy > 0.01f && data->Health <= 0.5f) {
            float knockHp = data->Healthy;
            if (knockHp <= 1.5f) {
                knockHp *= 100.f;
            }
            if (knockHp > 100.f) {
                knockHp = 100.f;
            }
            if (knockHp < 1.f) {
                knockHp = 1.f;
            }
            data->Health = knockHp;
        }

        Vec3 objPos{};
        if (getPlayerWorldPos(pBase, objPos)) {
            data->Distance = getDistance(myPos, objPos);
        }

        if (needVisCheck) {
            const uintptr_t mesh = getA(pBase + OffsetsAll64::Mesh);
            if (mesh) {
                const float rt = getF(mesh + OffsetsAll64::LastRenderTime);
                data->isVisible = (fabsf(rt - localRenderTime) < 0.05f);
            }
        }

        if (sSlotBoneWorld[i].isBone) {
            data->BoneWorld = sSlotBoneWorld[i];
            data->Bone.isBone = true;
            data->HeadLocation = proj.project(sSlotBoneWorld[i].points[0]);
        } else if (objPos.X != 0.f || objPos.Y != 0.f || objPos.Z != 0.f) {
            data->HeadLocation = proj.project(objPos + Vec3(0.f, 0.f, 80.f));
        }

        sCamOnlyCache[live] = *data;
        sBonePawn[live] = pBase;
        sBoneFlags[live] = sBoneFlags[i];
        sDrawBoneCache[live] = sDrawBoneCache[i];
        sSlotBoneWorld[live] = sSlotBoneWorld[i];
        live++;
    }
    out.PlayerCount = live;
    sBonePlayerCount = live;
    dedupePlayerList(out);
    (void) myPos;
}

/** Light bone pass — staggered batches; skip game reads beyond 300 m. */
static void refreshCachedPlayerBonesLight(Response &out, const Vec3 &myPos) {
    const int n = sBonePlayerCount < maxplayerCount ? sBonePlayerCount : maxplayerCount;
    static int sBoneBatch = 0;
    int activeCount = 0;
    for (int bi = 0; bi < n; bi++) {
        const float d = sCamOnlyCache[bi].Distance;
        if (d <= 0.f || d <= 300.f) {
            activeCount++;
        }
    }
    const int numBatches = activeCount > 48 ? 3 : (activeCount > 24 ? 2 : 1);
    const int curBatch = sBoneBatch++ % numBatches;
    const ScreenProjector proj(cameraView);

    int live = 0;
    for (int bi = 0; bi < n; bi++) {
        uintptr_t pBase = sBonePawn[bi];
        if (!isValid64(pBase)) {
            continue;
        }
        PlayerData *data = &out.Players[live];
        *data = sCamOnlyCache[bi];
        data->actorAddr = pBase;

        const float dist = sCamOnlyCache[bi].Distance;
        const bool inEspRange = dist > 0.f && dist <= 300.f;
        const bool closeEnemy = inEspRange && dist < 60.f;
        const bool midEnemy = inEspRange && dist >= 60.f && dist < 140.f;
        const bool farEnemy = inEspRange && dist >= 140.f;
        const bool refreshBones = closeEnemy
                || (midEnemy && ((sBoneBatch & 1u) == 0u))
                || (farEnemy && numBatches == 1)
                || (farEnemy && ((bi % numBatches) == curBatch));
        if (refreshBones) {
            const bool isTrainingModel = (sBoneFlags[bi] & 1) != 0;
            const bool isMetroMode = (sBoneFlags[bi] & 2) != 0;
            BoneMeshCache &boneCache = sDrawBoneCache[bi];
            if (!boneCache.valid || boneCache.mesh != getA(pBase + OffsetsAll64::Mesh)) {
                boneCache = resolveBoneCacheForPawn(bi, pBase);
            }
            if (boneCache.valid) {
                fillPlayerBoneWorld(data->BoneWorld, boneCache, isTrainingModel, isMetroMode);
                if (data->BoneWorld.isBone) {
                    sSlotBoneWorld[bi] = data->BoneWorld;
                    anchorBonesToRoot(bi, pBase);
                }
            }
        } else if (inEspRange && !sSlotBoneWorld[bi].isBone) {
            Vec3 objPos{};
            if (getPlayerWorldPos(pBase, objPos)) {
                data->Distance = getDistance(myPos, objPos);
                data->HeadLocation = proj.project(objPos + Vec3(0.f, 0.f, 80.f));
            }
        }

        if (sSlotBoneWorld[bi].isBone) {
            data->BoneWorld = sSlotBoneWorld[bi];
            data->Bone.isBone = true;
            data->HeadLocation = proj.project(sSlotBoneWorld[bi].points[0]);
        }
        sCamOnlyCache[live] = *data;
        sBonePawn[live] = pBase;
        sBoneFlags[live] = sBoneFlags[bi];
        sDrawBoneCache[live] = sDrawBoneCache[bi];
        sSlotBoneWorld[live] = sSlotBoneWorld[bi];
        live++;
    }
    if (live <= 0 && n > 0) {
        live = n;
        for (int bi = 0; bi < n; bi++) {
            out.Players[bi] = sCamOnlyCache[bi];
            out.Players[bi].actorAddr = sBonePawn[bi];
            if (sSlotBoneWorld[bi].isBone) {
                out.Players[bi].BoneWorld = sSlotBoneWorld[bi];
                out.Players[bi].Bone.isBone = true;
                out.Players[bi].HeadLocation =
                        proj.project(sSlotBoneWorld[bi].points[0]);
            }
        }
    }
    sBonePlayerCount = live;
    out.PlayerCount = live;
    dedupePlayerList(out);
}

/** Draw-sync fast path: camera + cached bones reproject — zero game reads. */
static void fillResponseFromCamCache(Response &out, const Vec3 &myPos) {
    const ScreenProjector proj(cameraView);
    int live = 0;
    const int n = sBonePlayerCount < maxplayerCount ? sBonePlayerCount : maxplayerCount;
    for (int i = 0; i < n; i++) {
        uintptr_t pawn = sBonePawn[i];
        if (!isValid64(pawn)) {
            continue;
        }
        PlayerData *data = &out.Players[live];
        *data = sCamOnlyCache[i];
        data->actorAddr = pawn;
        if (sSlotBoneWorld[i].isBone) {
            data->BoneWorld = sSlotBoneWorld[i];
            data->Bone.isBone = true;
            data->HeadLocation = proj.project(sSlotBoneWorld[i].points[0]);
        } else if (sHasLastRoot[i]) {
            data->HeadLocation = proj.project(sLastRootPos[i] + Vec3(0.f, 0.f, 88.f));
            data->Distance = getDistance(myPos, sLastRootPos[i]);
        } else {
            Vec3 objPos{};
            if (getPlayerWorldPos(pawn, objPos)) {
                data->HeadLocation = proj.project(objPos + Vec3(0.f, 0.f, 88.f));
                data->Distance = getDistance(myPos, objPos);
            }
        }
        sCamOnlyCache[live] = *data;
        live++;
    }
    out.PlayerCount = live > 0 ? live : n;
    if (live == 0 && n > 0) {
        for (int i = 0; i < n && i < maxplayerCount; i++) {
            out.Players[i] = sCamOnlyCache[i];
            out.Players[i].actorAddr = sBonePawn[i];
            if (sSlotBoneWorld[i].isBone) {
                out.Players[i].BoneWorld = sSlotBoneWorld[i];
                out.Players[i].Bone.isBone = true;
            }
        }
    }
    sBonePlayerCount = out.PlayerCount;
    dedupePlayerList(out);
    (void) myPos;
}

static inline void applySmallCrosshairForLocal(uintptr_t localPlayer) {
    if (!isSmallCrosshair) {
        sSmallCrosshairPatch.reset();
        return;
    }
    if (!isValid64(localPlayer)) {
        return;
    }
    ShootWeaponBase localWeapon(localPlayer);
    if (!localWeapon.isValid() || !isValid64(localWeapon.ShootWeaponEntity)) {
        return;
    }
    sCachedShootEntity = localWeapon.ShootWeaponEntity;
    tickSmallCrosshairStealth(sCachedShootEntity, true, true);
}

static inline void applySmallCrosshairCached() {
    if (!isSmallCrosshair) {
        sSmallCrosshairPatch.reset();
        return;
    }
    if (!isValid64(sCachedShootEntity)) {
        return;
    }
    tickSmallCrosshairStealth(sCachedShootEntity, true, true);
}


struct SceneComponent {
    uintptr_t CameraComponent;
    SceneComponent(uintptr_t pBase) {
        CameraComponent = getA(pBase + OffsetsAll64::ThirdPersonCameraComponent);
    }
    void setWideView(float fov) {
        if (getI(CameraComponent + OffsetsAll64::FieldOfView) != 0.0) {
            Write(CameraComponent + OffsetsAll64::FieldOfView, std::to_string(fov).c_str(), TYPE_FLOAT);
        }
    }
    bool isValid() const {
        return (CameraComponent != 0);
    }
};






static pid_t sCachedGamePid = 0;

/** Find game PID; prefers env/file from host app, then live /proc scan. */
static pid_t resolveGamePidOnce(int *outType) {
    if (sCachedGamePid >= 10) {
        char statusPath[48];
        snprintf(statusPath, sizeof(statusPath), "/proc/%d/status", (int) sCachedGamePid);
        if (access(statusPath, F_OK) == 0) {
            if (outType) *outType = 3;
            return sCachedGamePid;
        }
        sCachedGamePid = 0;
        sCachedUe4Base = 0;
    }
    pid_t fromHost = readHostGamePidFile();
    if (fromHost >= 10) {
        sCachedGamePid = fromHost;
        if (outType) *outType = 3;
        vngver = false;
        return sCachedGamePid;
    }
    const char *pkgs[] = {
            "com.pubg.imobile",
            "com.tencent.ig",
            "com.pubg.krmobile",
            "com.vng.pubgmobile",
            "com.rekoo.pubgm"
    };
    const int types[] = {3, 1, 1, 2, 1};
    for (int i = 0; i < 5; i++) {
        pid_t p = getPid((char *) pkgs[i]);
        if (p >= 10) {
            sCachedGamePid = p;
            if (outType) *outType = types[i];
            vngver = (types[i] == 2);
            return sCachedGamePid;
        }
    }
    return 0;

}

// ---- blackbox_raw / bgmiesp DeltaForceEspReader world+local resolve ----
static uintptr_t g_cachedWorld = 0;
static uintptr_t g_cachedPC = 0;
static uintptr_t g_cachedPawn = 0;
static uintptr_t g_cachedCam = 0;

// Exact blackbox IsLikelyUWorld: level circular OwningWorld + GameInstance
static inline bool IsLikelyUWorld(uintptr_t world) {
    if (!IsPtrPlausible(world)) {
        return false;
    }
    uintptr_t level = getA(world + OffsetsAll64::PersistentLevel);
    if (!IsPtrPlausible(level)) {
        return false;
    }
    // LevelOwningWorld offset varies slightly across dumps — try common ones
    static const uintptr_t kOwnOffs[] = {0xC0, 0xB8, 0xB0, 0xC8, 0xA8};
    bool circular = false;
    for (uintptr_t off : kOwnOffs) {
        if (getA(level + off) == world) {
            circular = true;
            break;
        }
    }
    uintptr_t giOff = OffsetsAll64::OwningGameInstance
                              ? OffsetsAll64::OwningGameInstance
                              : 0x470;
    uintptr_t gi = DecodeBgmiObjectPtr(getA(world + giOff));
    if (circular && IsPtrPlausible(gi)) {
        return true;
    }
    // Soft accept: GI valid + NetDriver or GameState plausible (circular dump mismatch)
    if (IsPtrPlausible(gi)) {
        uintptr_t gs = DecodeBgmiObjectPtr(getA(world + (OffsetsAll64::GameState ? OffsetsAll64::GameState : 0x428)));
        uintptr_t net = DecodeBgmiObjectPtr(getA(world + OffsetsAll64::NetDriver));
        if (IsPtrPlausible(gs) || IsPtrPlausible(net)) {
            return true;
        }
    }
    return false;
}

// blackbox DecodeBgmiWorldFromSlot — validates with IsLikelyUWorld at EVERY step
// (NOT IsPtrPlausible — that was accepting encrypted GWorld slots as "world")
static uintptr_t DecodeBgmiWorldFromSlot(uintptr_t slotQword) {
    if (IsLikelyUWorld(slotQword)) {
        return slotQword;
    }
    uintptr_t deref = getA(slotQword);
    if (IsLikelyUWorld(deref)) {
        return deref;
    }
    if (slotQword >= 0x20) {
        uintptr_t decoded = getA(slotQword - 0x20);
        if (IsLikelyUWorld(decoded)) {
            return decoded;
        }
    }
    return 0;
}

static int SafeTArrayCount(uintptr_t tarrayBase) {
    if (!tarrayBase) return 0;
    int count = getI(tarrayBase + sizeof(uintptr_t));
    if (count < 0 || count > 8000) return 0;
    return count;
}

static bool ReadLevelActorTArray(uintptr_t level, uintptr_t *outArray, int *outCount) {
    *outArray = 0;
    *outCount = 0;
    if (!IsPtrPlausible(level)) return false;

    uintptr_t clusterOff = OffsetsAll64::LevelActorCluster
                                   ? OffsetsAll64::LevelActorCluster
                                   : 0xE0;
    uintptr_t actorsOff = OffsetsAll64::ActorClusterActors
                                  ? OffsetsAll64::ActorClusterActors
                                  : 0x28;
    uintptr_t cluster = getA(level + clusterOff);
    if (IsPtrPlausible(cluster)) {
        int cnt = SafeTArrayCount(cluster + actorsOff);
        uintptr_t data = getA(cluster + actorsOff);
        if (IsPtrPlausible(data) && cnt > 0) {
            *outArray = cluster + actorsOff; // address of TArray struct
            *outCount = cnt;
            return true;
        }
    }
    uintptr_t legacy = OffsetsAll64::LevelActorsLegacy
                               ? OffsetsAll64::LevelActorsLegacy
                               : 0xA0;
    uintptr_t tryOffs[] = {legacy, (uintptr_t) 0x98, (uintptr_t) 0xA0};
    for (uintptr_t off : tryOffs) {
        // Prefer DecryptActorsArray64 path address for sock TArray read
        uintptr_t slot = DecryptActorsArray64(level, (int) off, 0x448);
        if (!slot) {
            uintptr_t arr = getA(level + off);
            int cnt = SafeTArrayCount(level + off);
            if (arr && cnt > 0 && cnt <= 8000) {
                *outArray = level + off; // TArray lives at level+off
                *outCount = cnt;
                return true;
            }
            continue;
        }
        TArray<uint64_t> ta = Read<TArray<uint64_t>>(slot);
        if (ta.IsValid() && ta.count > 0) {
            *outArray = slot;
            *outCount = ta.count;
            return true;
        }
    }
    return false;
}

// Try latest UEPointers: GWorld → GWorldExternal → GEngine→Viewport→World
static uintptr_t ResolveWorld(uintptr_t base, int *outMethod) {
    if (outMethod) *outMethod = 0;
    if (g_cachedWorld && !IsLikelyUWorld(g_cachedWorld)) {
        g_cachedWorld = 0;
    }
    if (IsLikelyUWorld(g_cachedWorld)) {
        if (outMethod) *outMethod = 9;
        return g_cachedWorld;
    }

    auto trySlot = [&](uintptr_t raw, int method) -> uintptr_t {
        if (!raw) return 0;
        uintptr_t world = DecodeBgmiWorldFromSlot(raw);
        if (IsLikelyUWorld(world)) {
            g_cachedWorld = world;
            if (outMethod) *outMethod = method;
            return world;
        }
        return 0;
    };

    // 1) Latest internal GWorld (0xE40B678)
    if (OffsetsGame::GWorld) {
        uintptr_t w = trySlot(getA(base + OffsetsGame::GWorld), 1);
        if (w) return w;
    }

    // 2) External World pointer (UEExternalPointers::WorldExternal) — sock is out-of-process
    if (OffsetsGame::GWorldExternal) {
        uintptr_t w = trySlot(getA(base + OffsetsGame::GWorldExternal), 2);
        if (w) return w;
    }

    // 3) GEngine → GameViewport → World (blackbox path)
    if (OffsetsGame::GEngine) {
        uintptr_t engRaw = getA(base + OffsetsGame::GEngine);
        uintptr_t engine = DecodeBgmiObjectPtr(engRaw);
        if (!IsPtrPlausible(engine) && engRaw >= 0x20) {
            engine = getA(engRaw - 0x20);
        }
        if (IsPtrPlausible(engine)) {
            static const uintptr_t kVpOffs[] = {0x810, 0x38C, 0x790, 0x7F8, 0x838, 0x7D8, 0x808};
            static const uintptr_t kWorldOffs[] = {0x78, 0x70, 0x80};
            for (uintptr_t vpOff : kVpOffs) {
                uintptr_t viewport = DecodeBgmiObjectPtr(getA(engine + vpOff));
                if (!IsPtrPlausible(viewport)) continue;
                for (uintptr_t wo : kWorldOffs) {
                    uintptr_t world = DecodeBgmiObjectPtr(getA(viewport + wo));
                    if (IsLikelyUWorld(world)) {
                        g_cachedWorld = world;
                        if (outMethod) *outMethod = 3;
                        return world;
                    }
                    world = DecodeBgmiWorldFromSlot(getA(viewport + wo));
                    if (IsLikelyUWorld(world)) {
                        g_cachedWorld = world;
                        if (outMethod) *outMethod = 3;
                        return world;
                    }
                }
            }
        }
    }

    // 4) Neighborhood scan around latest GWorld only (NOT old 0xE4F28C0)
    if (OffsetsGame::GWorld) {
        uintptr_t slotAddr = base + OffsetsGame::GWorld;
        for (int delta = -0x400; delta <= 0x400; delta += 8) {
            uintptr_t raw = getA(slotAddr + (uintptr_t) delta);
            if (!raw) continue;
            uintptr_t w = trySlot(raw, 4);
            if (w) return w;
        }
    }

    g_cachedWorld = 0;
    return 0;
}

static bool ResolveLocalFromEncryptedPlayers(uintptr_t gameInstance,
                                             uintptr_t *outPC, uintptr_t *outPawn,
                                             uintptr_t *outCam) {
    if (!IsPtrPlausible(gameInstance)) return false;
    uintptr_t encOff = OffsetsAll64::EncryptedLocalPlayers
                               ? OffsetsAll64::EncryptedLocalPlayers
                               : 0x38;
    uintptr_t flagOff = OffsetsAll64::UseEncryptLocalPlayerPtr
                                ? OffsetsAll64::UseEncryptLocalPlayerPtr
                                : 0x80;
    uint8_t encFlag = 1;
    vm_readv(gameInstance + flagOff, &encFlag, 1);
    uintptr_t encBase = gameInstance + encOff;
    uintptr_t encData = getA(encBase);
    int encCount = SafeTArrayCount(encBase);
    if (encFlag != 0 && IsPtrPlausible(encData) && encCount > 0 && encCount <= 4) {
        for (int i = 0; i < encCount; i++) {
            uintptr_t enc = getA(encData + (uintptr_t) i * sizeof(uintptr_t));
            uintptr_t localPlayer = DecodeBgmiObjectPtr(enc);
            if (!IsPtrPlausible(localPlayer)) continue;
            uintptr_t pc = DecodeBgmiObjectPtr(
                    getA(localPlayer + OffsetsAll64::PlayerController));
            if (!IsPtrPlausible(pc)) continue;
            uintptr_t pawn = DecodeBgmiObjectPtr(
                    getA(pc + OffsetsAll64::AcknowledgedPawn));
            uintptr_t cam = DecodeBgmiObjectPtr(
                    getA(pc + OffsetsAll64::PlayerCameraManager));
            *outPC = pc;
            *outPawn = IsPtrPlausible(pawn) ? pawn : 0;
            *outCam = IsPtrPlausible(cam) ? cam : 0;
            return true;
        }
    }
    return false;
}

static bool ResolveLocalFromGameInstance(uintptr_t world,
                                         uintptr_t *outPC, uintptr_t *outPawn,
                                         uintptr_t *outCam, int *outGiOff) {
    uintptr_t giOff = OffsetsAll64::OwningGameInstance
                              ? OffsetsAll64::OwningGameInstance
                              : 0x470;
    uintptr_t gi = DecodeBgmiObjectPtr(getA(world + giOff));
    if (!IsPtrPlausible(gi)) return false;
    if (outGiOff) *outGiOff = (int) giOff;

    // 1) encrypted LocalPlayers (BGMI)
    if (ResolveLocalFromEncryptedPlayers(gi, outPC, outPawn, outCam)) {
        return true;
    }

    // 2) plain LocalPlayers TArray @ 0x48
    uintptr_t lpOff = OffsetsAll64::LocalPlayers ? OffsetsAll64::LocalPlayers : 0x48;
    uintptr_t lpArr = getA(gi + lpOff);
    int lpCount = SafeTArrayCount(gi + lpOff);
    if (!IsPtrPlausible(lpArr) || lpCount <= 0 || lpCount > 8) return false;

    uintptr_t localPlayer = DecodeBgmiObjectPtr(getA(lpArr));
    if (!IsPtrPlausible(localPlayer)) return false;
    uintptr_t pc = DecodeBgmiObjectPtr(
            getA(localPlayer + OffsetsAll64::PlayerController));
    if (!IsPtrPlausible(pc)) return false;
    uintptr_t pawn = DecodeBgmiObjectPtr(
            getA(pc + OffsetsAll64::AcknowledgedPawn));
    uintptr_t cam = DecodeBgmiObjectPtr(
            getA(pc + OffsetsAll64::PlayerCameraManager));
    *outPC = pc;
    *outPawn = IsPtrPlausible(pawn) ? pawn : 0;
    *outCam = IsPtrPlausible(cam) ? cam : 0;
    return true;
}

static bool ResolveLocalFromNetDriver(uintptr_t world,
                                      uintptr_t *outPC, uintptr_t *outPawn,
                                      uintptr_t *outCam,
                                      uintptr_t *dbgNet, uintptr_t *dbgConn) {
    static const uintptr_t kConnOffs[] = {0x78, 0x88, 0x80};
    uintptr_t net = DecodeBgmiObjectPtr(getA(world + OffsetsAll64::NetDriver));
    if (dbgNet) *dbgNet = net;
    if (!IsPtrPlausible(net)) return false;
    for (uintptr_t cOff : kConnOffs) {
        uintptr_t conn = DecodeBgmiObjectPtr(getA(net + cOff));
        if (dbgConn) *dbgConn = conn;
        if (!IsPtrPlausible(conn)) continue;
        uintptr_t pc = DecodeBgmiObjectPtr(
                getA(conn + OffsetsAll64::PlayerController));
        if (!IsPtrPlausible(pc)) continue;
        uintptr_t pawn = DecodeBgmiObjectPtr(
                getA(pc + OffsetsAll64::AcknowledgedPawn));
        uintptr_t cam = DecodeBgmiObjectPtr(
                getA(pc + OffsetsAll64::PlayerCameraManager));
        *outPC = pc;
        *outPawn = IsPtrPlausible(pawn) ? pawn : 0;
        *outCam = IsPtrPlausible(cam) ? cam : 0;
        return true;
    }
    return false;
}

// GameState PlayerArray → find local PC via Controller→AckPawn == pawn
static bool ResolveLocalFromPlayerArray(uintptr_t world,
                                        uintptr_t *outPC, uintptr_t *outPawn,
                                        uintptr_t *outCam) {
    uintptr_t gsOff = OffsetsAll64::GameState ? OffsetsAll64::GameState : 0x428;
    uintptr_t paOff = OffsetsAll64::PlayerArray ? OffsetsAll64::PlayerArray : 0x4C8;
    uintptr_t pawnPriv = OffsetsAll64::PawnPrivate ? OffsetsAll64::PawnPrivate : 0x528;
    uintptr_t pawnCtrl = OffsetsAll64::PawnController ? OffsetsAll64::PawnController : 0x4E8;

    uintptr_t gameState = DecodeBgmiObjectPtr(getA(world + gsOff));
    if (!IsPtrPlausible(gameState)) return false;
    uintptr_t playerArray = getA(gameState + paOff);
    int playerCount = SafeTArrayCount(gameState + paOff);
    if (!IsPtrPlausible(playerArray) || playerCount <= 0) return false;
    int scan = playerCount < 128 ? playerCount : 128;
    for (int i = 0; i < scan; i++) {
        uintptr_t ps = DecodeBgmiObjectPtr(
                getA(playerArray + (uintptr_t) i * sizeof(uintptr_t)));
        if (!ps) continue;
        uintptr_t pawn = DecodeBgmiObjectPtr(getA(ps + pawnPriv));
        if (!pawn) continue;
        uintptr_t ctrl = DecodeBgmiObjectPtr(getA(pawn + pawnCtrl));
        if (!ctrl) continue;
        uintptr_t ack = DecodeBgmiObjectPtr(
                getA(ctrl + OffsetsAll64::AcknowledgedPawn));
        if (ack == pawn) {
            uintptr_t cam = DecodeBgmiObjectPtr(
                    getA(ctrl + OffsetsAll64::PlayerCameraManager));
            *outPC = ctrl;
            *outPawn = pawn;
            *outCam = IsPtrPlausible(cam) ? cam : 0;
            return true;
        }
    }
    return false;
}

static int ResolveLocalChain(uintptr_t world,
                             uintptr_t *outPC, uintptr_t *outPawn, uintptr_t *outCam) {
    *outPC = *outPawn = *outCam = 0;
    int giOff = 0;
    uintptr_t dbgNet = 0, dbgConn = 0;

    if (IsPtrPlausible(g_cachedPC)) {
        uintptr_t pawn = DecodeBgmiObjectPtr(
                getA(g_cachedPC + OffsetsAll64::AcknowledgedPawn));
        uintptr_t cam = DecodeBgmiObjectPtr(
                getA(g_cachedPC + OffsetsAll64::PlayerCameraManager));
        if (IsPtrPlausible(cam) || IsPtrPlausible(pawn)) {
            *outPC = g_cachedPC;
            *outPawn = IsPtrPlausible(pawn) ? pawn : g_cachedPawn;
            *outCam = IsPtrPlausible(cam) ? cam : g_cachedCam;
            g_cachedPawn = *outPawn;
            g_cachedCam = *outCam;
            return 9;
        }
    }

    if (ResolveLocalFromGameInstance(world, outPC, outPawn, outCam, &giOff)) {
        g_cachedPC = *outPC;
        g_cachedPawn = *outPawn;
        g_cachedCam = *outCam;
        return 1;
    }
    if (ResolveLocalFromPlayerArray(world, outPC, outPawn, outCam)) {
        g_cachedPC = *outPC;
        g_cachedPawn = *outPawn;
        g_cachedCam = *outCam;
        return 3;
    }
    if (ResolveLocalFromNetDriver(world, outPC, outPawn, outCam, &dbgNet, &dbgConn)) {
        g_cachedPC = *outPC;
        g_cachedPawn = *outPawn;
        g_cachedCam = *outCam;
        return 2;
    }
    if (IsPtrPlausible(g_cachedPC)) {
        *outPC = g_cachedPC;
        *outPawn = g_cachedPawn;
        *outCam = g_cachedCam;
        return 8;
    }
    return 0;
}

int main() {
	//CheckPackage();
    // Yield CPU to the game — sock64 is external reader; high priority = game stutter.
    setpriority(PRIO_PROCESS, 0, 10);
    LOGI("sock64 main() enter");
    if (!Create()) {
        LOGE("sock64 Create failed errno=%d", errno);
        perror("Creation Failed");
        return 0;
    }
    LOGI("sock64 socket created, connecting to somethingf...");
    if (!Connect()) {
        LOGE("sock64 Connect failed socket=somethingf errno=%d", errno);
        perror("Connection Failed");
        return 0;
    }
    LOGI("sock64 connected socket=somethingf");

    pthread_t aim_tid;
    pthread_create(&aim_tid, nullptr, AimBotAuto, nullptr);
    pthread_detach(aim_tid);

    pid = 0;
    for (int wait = 0; wait < 180 && sCachedGamePid < 10; wait++) {
        pid = resolveGamePidOnce(&typeeee);
        if (pid >= 10) break;
        usleep(1000000);
    }
    if (pid < 10 && sCachedGamePid >= 10) {
        pid = sCachedGamePid;
    }

    uintptr_t base = 0;
    if (pid >= 10) {
        xt_mem::attach(pid);
        clearUe4BaseCache(); // force fresh ELF-validated base
        base = getBase();
        uint32_t elfMagic = 0;
        if (base) vm_readv(base, &elfMagic, sizeof(elfMagic));
        LOGI("sock64 initial pid=%d type=%d base=0x%lx elfMagic=0x%x GWorld=0x%lx Ext=0x%lx Eng=0x%lx",
             (int) pid, typeeee, (unsigned long) base, elfMagic,
             (unsigned long) OffsetsGame::GWorld,
             (unsigned long) OffsetsGame::GWorldExternal,
             (unsigned long) OffsetsGame::GEngine);
    } else {
        LOGI("sock64 initial pid=%d type=%d base=0x0", (int) pid, typeeee);
    }
    Request request{};
 OffsetsAll64::GameType64(typeeee);
    OffsetsGame::GameOffset(typeeee);
    OffsetsAllBt64::GameType64(typeeee);
    LOGI("sock64 offsets type=%d GWorld=0x%lx GNames=0x%lx BT=0x%lx Patch1=0x%lx", typeeee,
         (unsigned long) OffsetsGame::GWorld, (unsigned long) OffsetsGame::GNames,
         (unsigned long) OffsetsAllBt64::ps_Global_TargetFunc,
         (unsigned long) OffsetsAllBt64::ps_Global_Patch1);

    bool aimbot = true;
    bool aimKnoced = false;
    bool isAvlive = true;
    bool aimAI = true;
    bool checkWideView = false;
    bool clothesskin = false;
    bool isShowDamage = false;
    bool predicitionaim = true;

    int aimBy = 1, aimFor = 1, aimWhen = 3;
    bool firing = false, ads = false, adsfiring = false, trigger = false;
    float aimRadius = 200, aimDist = 50, aimSpeed = 660, recCom = 50, RadarSize = 0;
    FRotator aimRotation;
    Vec3 pointingAngle;
    uint64_t cameraManager;
    struct Vec3 cam;
    uint64_t yawPitch = 0;
    float xy0, xy1;
    float timeToTravel;
    struct Vec2 RadarPosition = {static_cast<float>(width / 4), static_cast<float>(height / 4)};
    struct Vec3 sex,predicted, bonePos, velocity, MyPos, ObjPos, targetLoc, targetLocBt, targetAimTouch;
    struct D3DMatrix vMat;
    int isBack = 0, type = 69;
    int changed = 1;
    int myteamID = 101;
    char loaded[0x4000], loadedpn[20];
    char weaponData[100], name[100];
    int weaponID = 0;
    FVector3D targetLocked;
    FMinimalViewInfo POV = FMinimalViewInfo();
    struct FCameraCacheEntry CameraCache;
    Response response{};
    Response emptyResponse{};
    uintptr_t FromBase;
    uintptr_t Base;
    uintptr_t ShootWeaponEntity;
    uintptr_t CameraComponent;
    int bIsWeaponFiring;
    struct Vec2 headPos, rootPos;
    bool customScope = false;
    bool isVisibility = false;
    float recScope[9] = {};
    uintptr_t slot = 0;
    uintptr_t weaponSlot = 0;
    int recCom2 = 35, recoilTouch = 35;
    int recCom1 = 35;
    bool showdamage = false;
    uintptr_t gname_buff[30];
    uint32_t debugTick = 0;
    while ((receive((void *) &request) > 0)) {
        debugTick++;
        isSmallCrosshair = request.otherFeature.SmallCrosshair;
        sSmallCrosshairPatch.beginFrame(!isSmallCrosshair);
        gEnable = (request.options.aimBullet == 0);

        if (pid < 10) {
            int t = 0;
            pid = resolveGamePidOnce(&t);
            if (pid >= 10) {
                typeeee = t;
                xt_mem::attach(pid);
                base = getBase();
                OffsetsAll64::GameType64(typeeee);
                OffsetsGame::GameOffset(typeeee);
                OffsetsAllBt64::GameType64(typeeee);
                LOGI("sock64 attached pid=%d type=%d base=0x%lx BT=0x%lx", (int) pid, typeeee,
                     (unsigned long) base, (unsigned long) OffsetsAllBt64::ps_Global_TargetFunc);
            }
        }
        if (base == 0 && pid >= 10) {
            base = getBase();
        }
        if (pid < 10 || base == 0) {
            if ((debugTick & 31u) == 1u) {
                LOGW("sock64 no pid/base pid=%d base=0x%lx type=%d mode=%d",
                     (int) pid, (unsigned long) base, typeeee, request.Mode);
            }
            response.Success = false;
            response.InLobby = true;
            response.PlayerCount = 0;
            response.VehicleCount = 0;
            response.ItemsCount = 0;
            response.GrenadeCount = 0;
            response.BoxItemsCount = 0;
            send((void *) &response, sizeof(response));
            continue;
        }

        if (request.Mode == CameraOnlyMode && sCachedCameraMgr != 0) {
            height = request.ScreenHeight;
            width = request.ScreenWidth;
            response = Response{};
            response.Success = true;
            cameraView = getCameraView(sCachedCameraMgr + OffsetsAll64::CameraCache + OffsetsAll64::POV);
            fillEspCamera(response.espCamera, cameraView);

            send((void *) &response, sizeof(response));
            continue;
        }

        if (request.Mode == DrawSyncMode && sCachedCameraMgr != 0) {
            height = request.ScreenHeight;
            width = request.ScreenWidth;
            response = Response{};
            response.touchAimActive = false;
            cameraView = getCameraView(sCachedCameraMgr + OffsetsAll64::CameraCache + OffsetsAll64::POV);
            MyPos = cameraView.Location;
            fillEspCamera(response.espCamera, cameraView);
            response.fov = tanf(cameraView.FOV * ((float) PI / 360.0f));
            static uint32_t sDrawAuxTick = 0;
            static uint64_t sLastPositionRefreshMs = 0;
            const uint32_t auxTick = ++sDrawAuxTick;
            const uint64_t nowMs = espMonotonicMs();
            bool anyClose = false;
            for (int ci = 0; ci < sBonePlayerCount && ci < maxplayerCount; ci++) {
                const float d = sCamOnlyCache[ci].Distance;
                if (d > 0.f && d < 80.f) {
                    anyClose = true;
                    break;
                }
            }
            // Root positions: ~15–20 Hz is enough for smooth ESP without flooding
            // process_vm_readv (each read can briefly stall the game process).
            const bool refreshPositionsNow =
                    sLastPositionRefreshMs == 0 ||
                    nowMs - sLastPositionRefreshMs >= 55u;
            uint32_t boneIntervalMs = anyClose ? 110u : 200u;
            if (sBonePlayerCount > 4) {
                boneIntervalMs += 40u;
            }
            if (sBonePlayerCount > 10) {
                boneIntervalMs += 40u;
            }
            const bool needBones = sBonePlayerCount > 0 &&
                    (sLastBoneRefreshMs == 0 ||
                     nowMs - sLastBoneRefreshMs >= boneIntervalMs);
            const bool refreshBonesNow = needBones;
            if (sBonePlayerCount > 0 && refreshPositionsNow) {
                sLastPositionRefreshMs = nowMs;
                refreshCachedPlayerPositionsOnly(MyPos);
            }
            if (refreshBonesNow) {
                sLastBoneRefreshMs = nowMs;
                refreshCachedPlayerBonesLight(response, MyPos);
                syncLastRootPositions();
            }
            fillResponseFromCamCache(response, MyPos);
            response.Success = response.PlayerCount > 0;
            if (request.options.aimT == 1 && ((auxTick & 1u) == 0u)) {
                const int tsWhen = request.options.aimingState;
                const int firing = getI(sFastLocalPlayer + OffsetsAll64::bIsWeaponFiring) != 0;
                const int ads = getI(sFastLocalPlayer + OffsetsAll64::bIsGunADS) != 0;
                if (tsWhen == 3) {
                    response.touchAimActive = true;
                } else if (tsWhen == 1) {
                    response.touchAimActive = firing != 0;
                } else if (tsWhen == 2) {
                    response.touchAimActive = ads != 0;
                }
            }
            if ((auxTick & 7u) == 0u) {
                applySmallCrosshairCached();
            }
            projectAllPlayerBones(response, cameraView);
            // Bullet-track mem writes only when enabled — ESP-only must stay read-light.
            if (gEnable) {
                float btNear = -1.f;
                Vec3 btAim{};
                bool btAllow = false;
                tickBulletShotCounter(sFastLocalPlayer, btAllow);
                syncBulletTrackFast(base, request, cameraView, MyPos,
                                    btAllow, btNear, btAim);
            }
            send((void *) &response, sizeof(response));
            continue;
        }

        // InitMode cached light path BEFORE ResolveWorld — avoids UI freeze on overlay thread.
        if (request.Mode == InitMode && sCachedCameraMgr != 0 && sBonePlayerCount > 0) {
            static uint8_t sInitFastPass = 0;
            if (++sInitFastPass < 4) {
                height = request.ScreenHeight;
                width = request.ScreenWidth;
                response = Response{};
                cameraView = getCameraView(sCachedCameraMgr + OffsetsAll64::CameraCache +
                                           OffsetsAll64::POV);
                MyPos = cameraView.Location;
                fillEspCamera(response.espCamera, cameraView);
                response.fov = tanf(cameraView.FOV * ((float) PI / 360.0f));
                refreshCachedPlayerPositionsOnly(MyPos);
                fillResponseFromCamCache(response, MyPos);
                const uint64_t nowMs = espMonotonicMs();
                if (sLastBoneRefreshMs == 0 || nowMs - sLastBoneRefreshMs >= 80u) {
                    sLastBoneRefreshMs = nowMs;
                    refreshCachedPlayerBonesLight(response, MyPos);
                    syncLastRootPositions();
                }
                projectAllPlayerBones(response, cameraView);
                response.Success = response.PlayerCount > 0;
                if (gEnable) {
                    float btNear = -1.f;
                    Vec3 btAim{};
                    bool btAllow = false;
                    tickBulletShotCounter(sFastLocalPlayer, btAllow);
                    syncBulletTrackFast(base, request, cameraView, MyPos,
                                        btAllow, btNear, btAim);
                }
                send((void *) &response, sizeof(response));
                continue;
            }
            sInitFastPass = 0;
        }

        if (request.Mode == PlayersBoneMode && sFastLocalPlayer != 0 && sCachedCameraMgr != 0 &&
            sBonePlayerCount > 0) {
            height = request.ScreenHeight;
            width = request.ScreenWidth;
            response = Response{};
            cameraView = getCameraView(sCachedCameraMgr + OffsetsAll64::CameraCache + OffsetsAll64::POV);
            MyPos = cameraView.Location;
            fillEspCamera(response.espCamera, cameraView);
            response.fov = tanf(cameraView.FOV * ((float) PI / 360.0f));
            refreshCachedPlayerBones(response, MyPos);
            syncLastRootPositions();
            fillResponseFromCamCache(response, MyPos);
            response.Success = sBonePlayerCount > 0;
            response.PlayerCount = sBonePlayerCount;
            projectAllPlayerBones(response, cameraView);
            send((void *) &response, sizeof(response));
            continue;
        }

        uintptr_t gWorldEnc = getA(base + OffsetsGame::GWorld);
        uintptr_t gWorldExt = OffsetsGame::GWorldExternal
                                      ? getA(base + OffsetsGame::GWorldExternal)
                                      : 0;
        uintptr_t gEngRaw = OffsetsGame::GEngine ? getA(base + OffsetsGame::GEngine) : 0;
        int uWorldMethod = 0;
        uintptr_t uWorld = ResolveWorld(base, &uWorldMethod);

        // GNames: try current + blackbox + newer dump offsets
        uintptr_t gNamesOuter = 0;
        uintptr_t gname = 0;
        int gnameMethod = 0;
        static const uintptr_t kGNamesOffs[] = {
                OffsetsGame::GNames, 0xE40BB20 // latest dump only
        };
        for (size_t ni = 0; ni < sizeof(kGNamesOffs) / sizeof(kGNamesOffs[0]); ni++) {
            uintptr_t no = kGNamesOffs[ni];
            if (no == 0) continue;
            if (ni > 0 && no == OffsetsGame::GNames) continue;
            uintptr_t outer = DecodeBgmiObjectPtr(getA(base + no));
            if (!outer) outer = getA(base + no);
            uintptr_t pool = getA(outer + 0x110);
            if (isValid64(pool)) {
                gNamesOuter = outer;
                gname = pool;
                gnameMethod = 1;
                if (no != OffsetsGame::GNames) OffsetsGame::GNames = no;
                break;
            }
            if (isValid64(outer)) {
                gNamesOuter = outer;
                gname = outer;
                gnameMethod = 2;
                if (no != OffsetsGame::GNames) OffsetsGame::GNames = no;
                break;
            }
        }
        if ((debugTick & 31u) == 1u) {
            uint32_t elfMagic = 0;
            vm_readv(base, &elfMagic, sizeof(elfMagic));
            LOGI("sock64 resolve tick=%u mode=%d pid=%d base=0x%lx elf=0x%x "
                 "GWorldRaw=0x%lx ExtRaw=0x%lx EngRaw=0x%lx uWorld=0x%lx(m%d) "
                 "GNamesOuter=0x%lx gname=0x%lx(m%d) GWorldOff=0x%lx ExtOff=0x%lx EngOff=0x%lx",
                 debugTick, request.Mode, (int) pid, (unsigned long) base, elfMagic,
                 (unsigned long) gWorldEnc, (unsigned long) gWorldExt, (unsigned long) gEngRaw,
                 (unsigned long) uWorld, uWorldMethod,
                 (unsigned long) gNamesOuter, (unsigned long) gname, gnameMethod,
                 (unsigned long) OffsetsGame::GWorld,
                 (unsigned long) OffsetsGame::GWorldExternal,
                 (unsigned long) OffsetsGame::GEngine);
        }
        if (!isValid64(uWorld)) {
            sFastLocalPlayer = 0;
            if ((debugTick & 31u) == 1u) {
                LOGW("sock64 invalid uWorld base=0x%lx GWorldOffset=0x%lx GWorldRaw=0x%lx uWorld=0x%lx",
                     (unsigned long) base, (unsigned long) OffsetsGame::GWorld,
                     (unsigned long) gWorldEnc, (unsigned long) uWorld);
            }
            response.Success = false;
            response.InLobby = true;
            send((void *) &response, sizeof(response));
            continue;
        }
        auto level = DecodeBgmiObjectPtr(getA(uWorld + OffsetsAll64::PersistentLevel));
        if (!isValid64(level)) {
            level = getA(uWorld + OffsetsAll64::PersistentLevel);
        }
        if (!isValid64(level)) {
            sFastLocalPlayer = 0;
            if ((debugTick & 31u) == 1u) {
                LOGW("sock64 invalid level uWorld=0x%lx level=0x%lx PersistentLevel=0x%lx",
                     (unsigned long) uWorld, (unsigned long) level,
                     (unsigned long) OffsetsAll64::PersistentLevel);
            }
            response.InLobby = true;
            send((void *) &response, sizeof(response));
            continue;
        }

        uintptr_t playerController = 0, LocalPlayer = 0, CameraManager = 0;
        int localMethod = ResolveLocalChain(uWorld, &playerController, &LocalPlayer, &CameraManager);
        uintptr_t uMyObject = 0;
        if (isValid64(playerController)) {
            uMyObject = DecodeBgmiObjectPtr(
                    getA(playerController + OffsetsAll64::STExtraBaseCharacter));
        }
        uintptr_t aimControl = playerController;

        // Debug NetDriver intermediates when local chain fails
        uintptr_t dbgNet = 0, dbgConn = 0, dbgGi = 0;
        if ((debugTick & 31u) == 1u) {
            dbgNet = DecodeBgmiObjectPtr(getA(uWorld + OffsetsAll64::NetDriver));
            if (IsPtrPlausible(dbgNet)) {
                dbgConn = DecodeBgmiObjectPtr(getA(dbgNet + OffsetsAll64::ServerConnection));
            }
            dbgGi = DecodeBgmiObjectPtr(getA(uWorld + OffsetsAll64::OwningGameInstance));
            uintptr_t probeArr = 0;
            int probeCnt = 0;
            ReadLevelActorTArray(level, &probeArr, &probeCnt);
            bool worldOk = IsLikelyUWorld(uWorld);
            LOGI("sock64 chain level=0x%lx pc=0x%lx local=0x%lx cam=0x%lx method=%d "
                 "net=0x%lx conn=0x%lx gi=0x%lx actors=%d worldOk=%d GWorldOff=0x%lx mode=%d",
                 (unsigned long) level, (unsigned long) playerController,
                 (unsigned long) LocalPlayer, (unsigned long) CameraManager, localMethod,
                 (unsigned long) dbgNet, (unsigned long) dbgConn, (unsigned long) dbgGi,
                 probeCnt, worldOk ? 1 : 0,
                 (unsigned long) OffsetsGame::GWorld, request.Mode);
        }

        if (isValid64(LocalPlayer)) {
            sFastLocalPlayer = LocalPlayer;
        } else {
            sFastLocalPlayer = 0;
        }

        gEnable = (request.options.aimBullet == 0);

        auto CharMove = ReadValue(LocalPlayer + 0x518);
        
        auto switchweapon = ReadValue(LocalPlayer + 0x2BFC);
        


        FMinimalViewInfo POV = FMinimalViewInfo();
        FCameraCacheEntry CameraCache = FCameraCacheEntry();

        if (CameraManager) {
            CameraCache = Read<FCameraCacheEntry>(
                    CameraManager + OffsetsAll64::CameraCache);
            POV = CameraCache.POV;
            sCachedCameraMgr = CameraManager;
        }

        if (request.Mode == PlayersBoneMode) {
            height = request.ScreenHeight;
            width = request.ScreenWidth;
            response = Response{};
            if (CameraManager) {
                cameraView = getCameraView(CameraManager + OffsetsAll64::CameraCache + OffsetsAll64::POV);
                MyPos = cameraView.Location;
                fillEspCamera(response.espCamera, cameraView);
            }
            refreshCachedPlayerBones(response, MyPos);
            syncLastRootPositions();
            response.Success = sBonePlayerCount > 0;
            response.PlayerCount = sBonePlayerCount;
            send((void *) &response, sizeof(response));
            continue;
        }

        if (request.Mode == CameraOnlyMode) {
            height = request.ScreenHeight;
            width = request.ScreenWidth;
            response = Response{};
            response.Success = true;
            response.BoneRefreshCount = 0;
            if (CameraManager) {
                cameraView = getCameraView(CameraManager + OffsetsAll64::CameraCache + OffsetsAll64::POV);
                fillEspCamera(response.espCamera, cameraView);
            }

            send((void *) &response, sizeof(response));
            continue;
        }

        if (request.Mode == InitMode || request.Mode == AimSyncMode) {
            height = request.ScreenHeight;
            width = request.ScreenWidth;

            aimRadius = (float) request.options.aimingRange;
            aimDist = (float) request.options.aimingDist;
            aimSpeed = (float) request.options.aimingSpeed;
            aimFor = request.options.aimbotmode;
            aimWhen = request.options.aimingState;
            aimBy = request.options.priority;
            aimKnoced = request.options.pour;
            aimAI = request.options.ignoreBot;
            recCom = (float) request.options.recCompe;
            aimbot = request.options.openState == 0;
            predicitionaim = request.options.prediction;
            aimTouch = request.options.aimT == 0;
            touchSimMode = request.options.aimT == 1;
            memoryAimAssist = aimbot && !aimTouch && !touchSimMode;
            ScrWidth = request.ScreenWidth;
            ScrHeight = request.ScreenHeight;
            tsScrW = ScrWidth;
            tsScrH = ScrHeight;
            aimSmooth = request.options.Smoothing;
            touchRange = (float) request.options.touchSize;
            touchX = (float) request.options.touchX;
            touchY = (float) request.options.touchY;
            tsTouchX = touchX;
            tsTouchY = touchY;
            tsTouchRange = touchRange;
            tsDragSpeed = (float) request.options.touchSpeed;
            tsSmoothMs = request.options.Smoothing;
            tsInvert = request.options.InputInversion;
            ChargingPortLeft = request.options.InputInversion;
            aimingSpeed = (float) request.options.touchSpeed;
            std::copy(request.options.recScope, request.options.recScope + 9, recScope);
            recCom1 = request.options.recCompe1;
            recCom2 = request.options.recCompe2;
            customScope = request.options.customScope;

            RadarPosition = request.radarPos;
            RadarSize = request.radarSize;

            isShowDamage = request.otherFeature.ShowDamage;
        //    isLessRecoil = request.otherFeature.LessRecoil;
        //    isMagicH = request.otherFeature.InstantHit;
            isSmallCrosshair = request.otherFeature.SmallCrosshair;
            isWideView = request.otherFeature.WideView;
            isClothes = request.otherFeature.clothes;
            isBag = request.otherFeature.bag;
            isHelmet = request.otherFeature.helmet;
        //    isZeroRecoil = request.otherFeature.ZeroRecoil;
        //    isAimbot = request.otherFeature.Aimbot  ;
        //    isFastShootInterval = request.otherFeature.FastShootInterval;
          //  isHitX = request.otherFeature.HitX;
         //   isNoShake = request.otherFeature.NoShake;
          //  isFastSwitchWeapon = request.otherFeature.FastSwitchWeapon;
        }

        float nearest = -1.0f;
        float tsNearest = -1.0f;
        Vec3 tsScreenTarget{};
        uintptr_t tsTargetPawn = 0;
        bool allowBulletTrack = false;
        uintptr_t localShootEntity = 0;

        if (request.Mode == AimSyncMode) {
            height = request.ScreenHeight;
            width = request.ScreenWidth;
            response = Response{};

            if (CameraManager) {
                cameraView = getCameraView(CameraManager + OffsetsAll64::CameraCache + OffsetsAll64::POV);
                MyPos = cameraView.Location;
                fillEspCamera(response.espCamera, cameraView);
            }

            Trigger = false;
            if (gEnable && LocalPlayer && !vngver) {
                if (aimWhen == 3) {
                    Trigger = true;
                } else if (aimWhen == 1) {
                    Trigger = Read<bool>(LocalPlayer + OffsetsAll64::bIsWeaponFiring);
                } else if (aimWhen == 2) {
                    Trigger = !Read<bool>(LocalPlayer + OffsetsAll64::bIsGunADS);
                }
            }

            tickBulletShotCounter(LocalPlayer, allowBulletTrack);

            if (gEnable && isValid64(LocalPlayer)) {
                ShootWeaponBase btWeapon(LocalPlayer);
                if (btWeapon.isValid()) {
                    btWeapon.FixBT();
                    btWeapon.bypassBT();
                }
            }

            targetAimTouch = {};
            targetLoc = {};
            targetLocBt = {};
            tsWorldTarget = {};
            nearest = -1.0f;
            tsNearest = -1.0f;
            tsScreenTarget = {};
            tsTargetPawn = 0;

            if ((aimbot || touchSimMode || gEnable) && LocalPlayer && isValid64(playerController)) {
                cameraManager = getA(playerController + OffsetsAll64::PlayerCameraManager);
                aimControl = playerController;
                firing = false;
                ads = false;
                if (aimWhen == 1 || aimWhen == 3) {
                    firing = getI(LocalPlayer + OffsetsAll64::bIsWeaponFiring) != 0;
                }
                if (aimWhen == 2 || aimWhen == 3) {
                    ads = getI(LocalPlayer + OffsetsAll64::bIsGunADS) != 0;
                }
            }

            const ScreenProjector aimProjector(cameraView);
            const int localTeamAim =
                    isValid64(LocalPlayer) ? getI(LocalPlayer + OffsetsAll64::TeamID) : -1;

            for (int ai = 0; ai < sBonePlayerCount && ai < maxplayerCount; ai++) {
                uintptr_t pBase = sBonePawn[ai];
                if (!isPlayerAlive(pBase)) {
                    continue;
                }

                float healthbuffAim[2] = {0.f, 0.f};
                vm_readv(pBase + OffsetsAll64::Health, healthbuffAim, sizeof(healthbuffAim));

                const int teamId = getI(pBase + OffsetsAll64::TeamID);
                if (localTeamAim > 0 && teamId > 0 && teamId == localTeamAim) {
                    continue;
                }

                if (!getPlayerWorldPos(pBase, ObjPos)) {
                    ObjPos = Vec3();
                }
                const float dist3d = getDistance(MyPos, ObjPos);
                if (dist3d > 300.0f) {
                    continue;
                }

                const bool isTrainingModel = (sBoneFlags[ai] & 1) != 0;
                const bool isMetroMode = (sBoneFlags[ai] & 2) != 0;
                BoneMeshCache &boneCache = sDrawBoneCache[ai];
                PlayerBoneWorld bw{};
                if (boneCache.valid) {
                    fillPlayerBoneWorld(bw, boneCache, isTrainingModel, isMetroMode);
                }

                Vec3 headWorld;
                if (bw.isBone) {
                    headWorld = bw.points[0];
                } else {
                    headWorld = ObjPos + Vec3(0.f, 0.f, 80.f);
                }
                const Vec3 headScreen = aimProjector.project(headWorld);
                if (headScreen.Z == 1.0f) {
                    continue;
                }

                const float healthPct = healthbuffAim[1] > 0.01f
                        ? healthbuffAim[0] / healthbuffAim[1] * 100.f
                        : healthbuffAim[0];
                bool isBot = false;
                vm_readv(pBase + OffsetsAll64::bIsAI, &isBot, sizeof(isBot));
                if (request.options.ignoreAi && isBot) {
                    continue;
                }

                const float cx = headScreen.X - width / 2.f;
                const float cy = headScreen.Y - height / 2.f;
                const float centerDist = sqrtf(cx * cx + cy * cy);
                if (centerDist >= aimRadius || dist3d > aimDist) {
                    continue;
                }
                if (!(aimKnoced || healthPct > 0)) {
                    continue;
                }

                float pickDist = centerDist;
                if (aimBy != 1) {
                    pickDist = dist3d;
                }

                const bool pickTarget = ((aimbot || aimTouch) && !touchSimMode) || gEnable;
                if (pickTarget && (nearest < 0.f || pickDist < nearest)) {
                    nearest = pickDist;
                    if (aimFor == 1 || aimFor == 2) {
                        targetLoc = GetBoneFromIndx(pBase, 5);
                    } else {
                        targetLoc = GetBoneFromIndx(pBase, 3);
                    }
                    if (aimSpeed != 0) {
                        targetLoc = predictTargetPos(targetLoc, pBase, MyPos, aimSpeed);
                    }
                    if (gEnable) {
                        sBtTargetPawn = pBase;
                        targetLocBt = targetLoc;
                    }
                    {
                        const float scope = cameraView.FOV;
                        const float worldDist = getDistance(MyPos, targetLoc);
                        int recoilForAim = 0;
                        if (firing > 0) {
                            recoilForAim = recCom1;
                        }
                        const float screenPitchAdj = calcScopeScreenPitchAdj(
                                scope, worldDist, recoilForAim, customScope, recScope);
                        if (aimTouch) {
                            targetAimTouch = World2Screen(cameraView, targetLoc);
                            targetAimTouch.Y += screenPitchAdj;
                        }
                        if ((aimbot || aimTouch || gEnable) && !touchSimMode) {
                            pointingAngle = calcMemoryAimAngles(targetLoc, cameraView, screenPitchAdj);
                        }
                    }
                }

                if (touchSimMode && (!aimAI || !isBot)) {
                    const Vec3 bodyWorld = GetBoneFromIndx(pBase, 3);
                    const Vec3 bodyScr = aimProjector.project(bodyWorld);
                    if (bodyScr.Z != 1.0f) {
                        const float bcx = bodyScr.X - width / 2.f;
                        const float bcy = bodyScr.Y - height / 2.f;
                        const float fovDist = sqrtf(bcx * bcx + bcy * bcy);
                        float tsPickRadius = aimRadius;
                        const float wide = (width < height ? width : height) * 0.55f;
                        if (wide > tsPickRadius) {
                            tsPickRadius = wide;
                        }
                        if (fovDist < tsPickRadius
                            && (tsNearest < 0.f || fovDist < tsNearest)) {
                            tsNearest = fovDist;
                            tsScreenTarget = bodyScr;
                            tsWorldTarget = bodyWorld;
                            tsTargetPawn = pBase;
                        }
                    }
                }
            }

        } else {

        response.Success = false;
        response.InLobby = false;
        response.PlayerCount = 0;
        response.VehicleCount = 0;
        response.ItemsCount = 0;
        response.GrenadeCount = 0;
        response.BoxItemsCount = 0;
	//	response.ZoneoCount = 0;        

        strcpy(loaded, "");
        nearest = -1.0f;
        tsNearest = -1.0f;
        tsScreenTarget = {};
        tsTargetPawn = 0;
        tsWorldTarget = {};

        if (CameraManager) {
            cameraView = getCameraView(CameraManager + OffsetsAll64::CameraCache + OffsetsAll64::POV);
            MyPos = cameraView.Location;
            response.fov = tanf(cameraView.FOV * ((float) PI / 360.0f));
        }

        Trigger = false;
        if (gEnable && LocalPlayer && !vngver) {
            if (aimWhen == 3) {
                Trigger = true;
            } else if (aimWhen == 1) {
                Trigger = Read<bool>(LocalPlayer + OffsetsAll64::bIsWeaponFiring);
            } else if (aimWhen == 2) {
                Trigger = !Read<bool>(LocalPlayer + OffsetsAll64::bIsGunADS);
            }
        }

        allowBulletTrack = false;
        tickBulletShotCounter(LocalPlayer, allowBulletTrack);

        if ((aimbot || touchSimMode) && LocalPlayer && isValid64(playerController)) {
            cameraManager = getA(playerController + OffsetsAll64::PlayerCameraManager);
            firing = false;
            ads = false;
            if (aimWhen == 1 || aimWhen == 3) {
                firing = getI(LocalPlayer + OffsetsAll64::bIsWeaponFiring) != 0;
            }
            if (aimWhen == 2 || aimWhen == 3) {
                ads = getI(LocalPlayer + OffsetsAll64::bIsGunADS) != 0;
            }
        }

        const bool needVisCheck = request.Mode == PlayersDiscoverMode || aimbot || aimTouch || gEnable || touchSimMode;
        float localRenderTime = 0.f;
        if (needVisCheck && LocalPlayer) {
            uintptr_t localMesh = getA(LocalPlayer + OffsetsAll64::Mesh);
            if (localMesh) {
                localRenderTime = getF(localMesh + OffsetsAll64::LastRenderTime);
            }
        }

        bool metadataOnlyScan = false;
        const bool discoverOnly = (request.Mode == PlayersDiscoverMode);

        const bool idleDiscoverOnly =
                request.Mode == PlayersDiscoverMode && sBonePlayerCount == 0;
        if (idleDiscoverOnly) {
            static uint32_t sIdleDiscoverSkip = 0;
            const bool runHeavy = (++sIdleDiscoverSkip % 3u) == 0u;
            if (!runHeavy) {
                response.InLobby = !isValid64(LocalPlayer);
                response.PlayerCount = 0;
                if (CameraManager) {
                    sCachedCameraMgr = CameraManager;
                    cameraView = getCameraView(CameraManager + OffsetsAll64::CameraCache +
                                                OffsetsAll64::POV);
                    MyPos = cameraView.Location;
                    fillEspCamera(response.espCamera, cameraView);
                }
                send((void *) &response, sizeof(response));
                continue;
            }
        }

        TArray<uint64_t> ActorArray{};
        int totalActors = 0;
        uintptr_t actorTArrayAddr = 0;
        if (ReadLevelActorTArray(level, &actorTArrayAddr, &totalActors) && actorTArrayAddr) {
            ActorArray = Read<TArray<uint64_t>>(actorTArrayAddr);
            totalActors = ActorArray.count;
        } else {
            ActorArray = Read<TArray<uint64_t>>(DecryptActorsArray64(level, 0xA0, 0x448));
            totalActors = ActorArray.count;
        }
        if (totalActors < 0) {
            totalActors = 0;
        }
        if (totalActors > ActorArray.max) {
            totalActors = ActorArray.max;
        }
        const bool scanWorldLoot = false;
        const bool scanWorldVehicles = false;
        const int worldScanCount = 0;
        const bool scanGrenades = false;
        static int sWorldScanOffset = 0;
        static int sEnemyScanOffset = 0;
        static const int kEnemyScanChunk = 280;
        static const int kEnemyScanPasses = discoverOnly ? 2 : 1;
        sBtTargetPawn = 0;
        const bool scanPlayerBonesForPass =
                !metadataOnlyScan && !discoverOnly &&
                (request.Mode == InitMode || request.Mode == PlayersBoneMode || needVisCheck);
        const bool canScanActors =
                isValid64(gname) && ActorArray.IsValid() && totalActors > 0;

        const int localTeamId =
                isValid64(LocalPlayer) ? getI(LocalPlayer + OffsetsAll64::TeamID) : -1;

        localShootEntity = 0;
        if (isValid64(LocalPlayer)) {
            ShootWeaponBase localWeapon(LocalPlayer);
            if (localWeapon.isValid()) {
                localShootEntity = localWeapon.ShootWeaponEntity;
                sCachedShootEntity = localShootEntity;
                // FixBT writes weapon floats — only when bullet track is on (ESP-only stays read-only).
                if (gEnable) {
                    localWeapon.FixBT();
                    localWeapon.bypassBT();
                }
                if (fixdamage) {
                    localWeapon.bypassBT();
                }
            }
        }

        if (canScanActors) {
        // Pass 1 — enemies first. Small lists: one sweep. Large lists: multi-chunk + persist.
        const bool sweepAllEnemies = totalActors <= kEnemyScanChunk;
        const int scanPasses = sweepAllEnemies ? 1
                : (discoverOnly ? kEnemyScanPasses
                                 : (scanPlayerBonesForPass && sBonePlayerCount < 6 ? kEnemyScanPasses : 1));
        for (int scanPass = 0; scanPass < scanPasses; scanPass++) {
        const int enemyScanCount = sweepAllEnemies ? totalActors : kEnemyScanChunk;
        const int enemyScanStart = sweepAllEnemies ? 0 : (sEnemyScanOffset % (totalActors > 0 ? totalActors : 1));
        for (int si = 0; si < enemyScanCount; si++) {
            if (response.PlayerCount >= maxplayerCount) {
                break;
            }
            int i = sweepAllEnemies ? si : (enemyScanStart + si);
            if (!sweepAllEnemies && i >= totalActors) {
                i -= totalActors;
            }
            if (i < 0 || i >= totalActors) {
                continue;
            }
            uintptr_t pBase = ActorArray[static_cast<size_t>(i)];

            if (!isValid64(pBase)) {
                continue;
            }

            getGNameResCached(gname, pBase, name);

            if (!isEnemyPlayerActor(name)) {
                continue;
            }

            {

            const uint32_t actorRole = getI(pBase + OffsetsAll64::Role);
            if (isLocalPlayerPawn(pBase, LocalPlayer, uMyObject, actorRole)) {
                if (actorRole == 258) {
                    if (isLessRecoil) {
                        float newValue = 3.0f;
                        vm_writev(base + OffsetsAll64::CustomTimeDilation, &newValue, sizeof(newValue));
                    }
                    if (aimbot || touchSimMode) {
                        uintptr_t control = getA(pBase + OffsetsAll64::STPlayerController);
                        if (isValid64(control)) {
                            aimControl = control;
                            cameraManager = getA(control + OffsetsAll64::PlayerCameraManager);
                        }
                        if (aimWhen == 1 || aimWhen == 3) {
                            vm_readv(pBase + OffsetsAll64::bIsWeaponFiring, &firing, 1);
                        }
                        if (aimWhen == 2 || aimWhen == 3) {
                            vm_readv(pBase + OffsetsAll64::bIsGunADS, &ads, 1);
                        }
                    }
                    slot = getI(FromBase + OffsetsAll64::CurUseWeaponLogicSocketRep);
                    PawnComponent pawnComponent(pBase);
                    if (pawnComponent.isValid() && isShowDamage) {
                        pawnComponent.setShowDamage();
                    }
                    SceneComponent sceneComponent(pBase);
                    if (sceneComponent.isValid()) {
                        if (isWideView == 0 && checkWideView) {
                            checkWideView = false;
                            sceneComponent.setWideView(85.0f);
                        } else if (isWideView >= 1 && isWideView <= 8) {
                            checkWideView = true;
                            static const float fovValues[] = {85.0f, 90.0f, 95.0f, 100.0f, 110.0f,
                                                             120.0f, 130.0f, 140.0f};
                            if (isWideView < static_cast<int>(sizeof(fovValues) / sizeof(fovValues[0]))) {
                                sceneComponent.setWideView(fovValues[isWideView]);
                            }
                        }
                    }
                    myteamID = getI(pBase + OffsetsAll64::TeamID);
                }
                continue;
            }

            if (response.PlayerCount >= maxplayerCount) {
                continue;
            }
            if (isPlayerFullyDead(pBase)) {
                evictPersistPawn(pBase);
                continue;
            }

            bool alreadyListed = false;
            for (int du = 0; du < response.PlayerCount; du++) {
                if (response.Players[du].actorAddr == pBase) {
                    alreadyListed = true;
                    break;
                }
            }
            if (alreadyListed) {
                continue;
            }

            vm_readv(pBase + OffsetsAll64::Health, healthbuff, sizeof(healthbuff));

            PlayerData *data = &response.Players[response.PlayerCount];
            *data = PlayerData{};
            data->actorAddr = pBase;
            if (healthbuff[1] > 0.01f) {
                data->Health = healthbuff[0] / healthbuff[1] * 100.f;
            } else {
                data->Health = healthbuff[0];
            }
            data->TeamID = getI(pBase + OffsetsAll64::TeamID);
            if (data->Health > 200.f) {
                continue;
            }
            const bool isTrainingModel = strstr(name, "CharacterModelTaget") != nullptr;
            if (!isTrainingModel && localTeamId > 0 && data->TeamID > 0 &&
                data->TeamID == localTeamId) {
                continue;
            }

                bool isMetroMode = strstr(name, "PlayerPawn_TPlanAI");
                if (!getPlayerWorldPos(pBase, ObjPos)) {
                    ObjPos = Vec3();
                }
            data->Distance = getDistance(MyPos, ObjPos);
            if (data->Distance > 300.0f) {
                continue;
            }

                intptr_t Mesh = getA(pBase + OffsetsAll64::Mesh);
                if (needVisCheck && Mesh) {
                    float EntityRenderTime = getF(Mesh + OffsetsAll64::LastRenderTime);
                    isVisibility = (fabsf(EntityRenderTime - localRenderTime) < 0.05f);
                } else {
                    isVisibility = false;
                }

                BoneMeshCache boneCache{};
                if (!metadataOnlyScan && scanPlayerBonesForPass) {
                    boneCache = getBoneMeshCache(pBase);
                }

                data->RadarLocation = World2RadarRound(CameraCache, ObjPos, RadarPosition, RadarSize);
                data->isVisible = isVisibility;
                data->StatusPlayer = getI(pBase + OffsetsAll64::CurrentStates);

                vm_readv(pBase + OffsetsAll64::NearDeathBreath, &data->Healthy,
                         sizeof(data->Healthy));
                // Knocked players report Health≈0 — map breath into Health so ESP stays on.
                if (data->Healthy > 0.01f && data->Health <= 0.5f) {
                    float knockHp = data->Healthy;
                    if (knockHp <= 1.5f) {
                        knockHp *= 100.f;
                    }
                    if (knockHp > 100.f) {
                        knockHp = 100.f;
                    }
                    if (knockHp < 1.f) {
                        knockHp = 1.f;
                    }
                    data->Health = knockHp;
                }
            {
                int persistSlot = -1;
                for (int p = 0; p < sPersistCount; p++) {
                    if (sPersistPawn[p] == pBase) {
                        persistSlot = p;
                        break;
                    }
                }
                copyEnemyIdentity(data, pBase, name, persistSlot);
            }
            if (request.options.ignoreAi && data->isBot) {
                continue;
            }

            data->Bone.isBone = false;
                data->BoneWorld.isBone = false;
                const int playerSlot = response.PlayerCount;
                if (playerSlot < maxplayerCount) {
                    sBonePawn[playerSlot] = pBase;
                    sBoneFlags[playerSlot] =
                            static_cast<uint8_t>((isTrainingModel ? 1 : 0) | (isMetroMode ? 2 : 0));
                    if (!metadataOnlyScan && scanPlayerBonesForPass && boneCache.valid) {
                        sDrawBoneCache[playerSlot] = boneCache;
                        fillPlayerBoneWorld(data->BoneWorld, boneCache, isTrainingModel, isMetroMode);
                        if (data->BoneWorld.isBone) {
                            Vec3 hs{};
                            data->Bone = projectBoneWorld(data->BoneWorld, cameraView, hs);
                            if (data->Bone.isBone && hs.Z != 1.f) {
                                data->HeadLocation = hs;
                            }
                        }
                    } else if (metadataOnlyScan || !scanPlayerBonesForPass) {
                        sDrawBoneCache[playerSlot] = BoneMeshCache{};
                    }
                }
                const float headZ = isValid64(getA(pBase + OffsetsAll64::CurrentVehicle))
                        ? 120.f : 80.f;
                if (data->BoneWorld.isBone) {
                    data->HeadLocation =
                            ScreenProjector(cameraView).project(data->BoneWorld.points[0]);
                } else if (ObjPos.X != 0.f || ObjPos.Y != 0.f || ObjPos.Z != 0.f) {
                    data->HeadLocation = ScreenProjector(cameraView).project(
                            ObjPos + Vec3(0.f, 0.f, headZ));
                } else {
                    data->HeadLocation = Vec3{0.f, 0.f, 1.f};
                }

                data->Weapon = PlayerWeapon{};

            if ((aimbot || aimTouch || gEnable || touchSimMode)
                && data->HeadLocation.Z != 1.0f && (aimKnoced || data->Health > 0) && data->Distance <= aimDist
                && (aimTouch || aimbot || gEnable || isVisibility == 1) && (!aimAI || !data->isBot)
                && (aimbot || gEnable || aimTouch))
                {
                    float centerDist = sqrt((data->HeadLocation.X - width / 2) * (data->HeadLocation.X - width / 2) + (data->HeadLocation.Y - height / 2) * (data->HeadLocation.Y - height / 2));
                    if (centerDist < aimRadius)
                    {
                        if (aimBy != 1)
                            centerDist = data->Distance;

                        const bool pickTarget = ((aimbot || aimTouch) && !touchSimMode) || gEnable;
                        if (pickTarget && (nearest > centerDist || nearest < 0)) {
                            nearest = centerDist;

                            if (aimFor == 1) {
                                targetLoc = GetBoneFromIndx(pBase, 5);
                            } else if (aimFor == 2) {
                                targetLoc = GetBoneFromIndx(pBase, 5);
                            } else {
                                targetLoc = GetBoneFromIndx(pBase, 3);
                            }

                            if (aimSpeed != 0) {
                                targetLoc = predictTargetPos(targetLoc, pBase, MyPos, aimSpeed);
                            }

                            if (gEnable) {
                                sBtTargetPawn = pBase;
                                targetLocBt = targetLoc;
                            }

                            {
                                const float scope = cameraView.FOV;
                                const float worldDist = getDistance(MyPos, targetLoc);
                                int recoilForAim = 0;
                                if (slot == 2) {
                                    if (firing > 0) {
                                        recoilForAim = recCom2;
                                    }
                                } else if (firing > 0) {
                                    recoilForAim = recCom1;
                                }
                                const float screenPitchAdj = calcScopeScreenPitchAdj(
                                        scope, worldDist, recoilForAim, customScope, recScope);

                                if (aimTouch) {
                                    targetAimTouch = World2Screen(cameraView, targetLoc);
                                    targetAimTouch.Y += screenPitchAdj;
                                }
                                if ((aimbot || aimTouch || gEnable) && !touchSimMode) {
                                    pointingAngle = calcMemoryAimAngles(targetLoc, cameraView, screenPitchAdj);
                                }
                            }
                        }
                    }
                }

            if (touchSimMode && data->HeadLocation.Z != 1.0f
                && (aimKnoced || data->Health > 0)
                && data->Distance <= aimDist
                && (!aimAI || !data->isBot)) {
                // Lock directly on the real chest bone (no prediction lead).
                // Prediction at low bullet-speed throws the aim far off body.
                const Vec3 bodyWorld = GetBoneFromIndx(pBase, 3);
                const Vec3 bodyScr = ScreenProjector(cameraView).project(bodyWorld);
                if (bodyScr.Z == 1.0f) {
                    continue;
                }
                const float cx = bodyScr.X - width / 2.f;
                const float cy = bodyScr.Y - height / 2.f;
                const float fovDist = sqrtf(cx * cx + cy * cy);
                // FOV-wide pick: cover most of the screen, not just a tiny
                // center circle. Still locks the enemy nearest the crosshair.
                float tsPickRadius = aimRadius;
                float wide = (width < height ? width : height) * 0.55f;
                if (wide > tsPickRadius) tsPickRadius = wide;
                if (fovDist < tsPickRadius) {
                    if (tsNearest < 0.f || fovDist < tsNearest) {
                        tsNearest = fovDist;
                        tsScreenTarget = bodyScr;
                        tsWorldTarget = bodyWorld;
                        tsTargetPawn = pBase;
                    }
                }
            }
                

            response.PlayerCount++;

            }
        }
        if (!sweepAllEnemies && totalActors > kEnemyScanChunk) {
            sEnemyScanOffset = (enemyScanStart + kEnemyScanChunk) % totalActors;
        }
        }

        // Pass 2 — loot/vehicles only: rotating window (low priority).
        if (worldScanCount > 0) {
        const int scanStart = totalActors > worldScanCount
                ? (sWorldScanOffset % totalActors) : 0;
        for (int si = 0; si < worldScanCount; si++) {
            const int i = scanStart + si;
            if (i >= totalActors) {
                break;
            }
            uintptr_t pBase = ActorArray[i];

            if (!isValid64(pBase)) {
                continue;
            }

            getGNameResCached(gname, pBase, name);

            if (isEnemyPlayerActor(name)) {
                continue;
            }

            if (strnlen(name, sizeof(name)) < 5) {
                continue;
            }
            if (getI(pBase + SIZE) != 8) {
                continue;
            } else if (scanWorldVehicles && (strstr(name, "VH") || (strstr(name, "PickUp_") && !strstr(name, "BP")) ||
                       strstr(name, "Rony") || strstr(name, "Mirado") || strstr(name, "LadaNiva") ||
                       strstr(name, "AquaRail"))) {
                VehicleData* data = &response.Vehicles[response.VehicleCount];
                vm_readv(getA(pBase + OffsetsAll64::RootComponent) + OffsetsAll64::Position, &ObjPos, sizeof(ObjPos));
                uintptr_t Vehicle = getA(pBase + OffsetsAll64::VehicleCommon);
                data->Location = World2Screen(cameraView, ObjPos);
                if (data->Location.Z == 1.0f || data->Location.X > width + 200 || data->Location.X < -200)
                    continue;
                data->Distance = getDistance(MyPos, ObjPos);
                if (data->Distance > 300.0f) {
                    continue;
                }
                data->Fuel = getF(Vehicle + OffsetsAll64::VFuel) * 100 / getF(Vehicle + OffsetsAll64::VFuelMax);
                data->Health = getF(Vehicle + OffsetsAll64::VHealth) * 100 / getF(Vehicle + OffsetsAll64::VHealthMax);
                strcpy(data->VehicleName, name);
                if (response.VehicleCount >= maxvehicleCount) {
                    continue;
                }
                response.VehicleCount++;
            } else if (scanWorldLoot && (strstr(name, "Pickup_C") || strstr(name, "PickUp") ||
                       strstr(name, "BP_Ammo") || strstr(name, "BP_QK") ||
                       strstr(name, "Wrapper"))) {
                ItemData *data = &response.Items[response.ItemsCount];
                vm_readv(getA(pBase + OffsetsAll64::RootComponent) + OffsetsAll64::Position,
                         &ObjPos, sizeof(ObjPos));
                data->Location = World2Screen(cameraView, ObjPos);
                if (data->Location.Z == 1.0f || data->Location.X > width + 100 ||
                    data->Location.X < -50)
                    continue;
                data->Distance = getDistance(MyPos, ObjPos);
                if (data->Distance > 1000.0f)
                    continue;
                strcpy(data->ItemName, name);
                if (response.ItemsCount >= maxitemsCount)
                    continue;
                response.ItemsCount++;
            }else if (scanWorldLoot && (strstr(name, "AirDropPlane") || strstr(name, "PlayerDeadInventoryBox_C") || strstr(name, "AirDropBox") || strstr(name, "AirDropSpacecraft"))) {
                ItemData *data = &response.Items[response.ItemsCount];
                vm_readv(getA(pBase + OffsetsAll64::RootComponent) + OffsetsAll64::Position,
                         &ObjPos, sizeof(ObjPos));
                data->Location = World2Screen(cameraView, ObjPos);
                if (data->Location.Z == 1.0f || data->Location.X > width + 100 ||
                    data->Location.X < -50)
                    continue;
                data->Distance = getDistance(MyPos, ObjPos);
                strcpy(data->ItemName, name);
                if (response.ItemsCount >= maxitemsCount)
                    continue;
                response.ItemsCount++;
            } else if (scanGrenades && (strstr(name, "BP_Projectile_FragGrenade_C") || strstr(name, "BP_Grenade_Burn_C") ||
                       strstr(name, "BP_Projectile_BurnGrenade_C") || strstr(name, "BP_Projectile_StunGrenade_C") ||
                       strstr(name, "BP_Projectile_SmokeBomb_C"))) {                       

                GrenadeData *data = &response.Grenade[response.GrenadeCount];
                vm_readv(getA(pBase + OffsetsAll64::RootComponent) + OffsetsAll64::Position,
                         &ObjPos, sizeof(ObjPos));
                data->Location = World2Screen(cameraView, ObjPos);
                data->Distance = getDistance(MyPos, ObjPos);
                if (data->Distance > 150.0f)
                    continue;
                if (strstr(name, "Shoulei"))
                    data->type = 1;
                else if (strstr(name, "Burn"))
                    data->type = 2;
                else if (strstr(name, "Stun"))
                    data->type = 3;
                else if (strstr(name, "Smoke"))
                    data->type = 4;
				if (response.GrenadeCount >= maxgrenadeCount)
				{
					continue;
				}
				response.GrenadeCount++;
			} 
					
            
            if (scanWorldLoot && request.otherFeature.espLootBox &&
                (strstr(name, "PickUpListWrapperActor") || strstr(name, "AirDropListWrapperActor"))) {
                BoxItemData *data = &response.BoxItems[response.BoxItemsCount];
                vm_readv(getA(pBase + OffsetsAll64::RootComponent) + OffsetsAll64::Position, &ObjPos, sizeof(ObjPos));
                data->Location = World2Screen(cameraView, ObjPos);
                if (data->Location.Z == 1.0f || data->Location.X > width + 100 || data->Location.X < -50)
                    continue;
                data->Distance = getDistance(MyPos, ObjPos);
                if (data->Distance > 150.0f)
                    continue;
                auto PickUpData = getA(pBase + OffsetsAll64::PickUpDataList);
                data->itemCount = getI(pBase + OffsetsAll64::PickUpDataList + sizeof(uintptr_t));
                for (int ij = 0; ij < data->itemCount; ij++) {
                    data->itemID[ij] = getI(PickUpData + (ij * 0x38 + 0x4));
                }
                if (response.BoxItemsCount >= maxboxitemsCount) {
                    continue;
                }
                response.BoxItemsCount++;
            }

        }
        if (totalActors > worldScanCount) {
            sWorldScanOffset = (scanStart + worldScanCount) % totalActors;
        }
        }

        if (request.Mode == InitMode || request.Mode == PlayersDiscoverMode) {
            rebuildPlayersFromPersist(response, MyPos);
            snapshotPlayersForCamOnly(response, MyPos);
        }
        }


        if (request.Mode != AimSyncMode && request.Mode != CameraOnlyMode) {
            sBonePlayerCount = response.PlayerCount;
        }
        }

        if (response.PlayerCount + response.ItemsCount + response.VehicleCount +
            response.GrenadeCount + response.BoxItemsCount > 0)
            response.Success = true;

        if (CameraManager) {
            fillEspCamera(response.espCamera, cameraView);
        }

        response.touchAimActive = false;
        response.touchAimX = 0;
        response.touchAimY = 0;

        if (touchSimMode) {
            // Smart Memory Aim: host ESP picks screen XY, BCore injects MotionEvent — no mem writes.
            tsAimActive = false;
            tsAimX = 0.f;
            tsAimY = 0.f;
            tsResetStealthState();
        } else if ((firing || ads) && nearest > 0 && aimTouch) {
            if (targetAimTouch.Z != 1.0f && targetAimTouch.X > 0 && targetAimTouch.Y > 0) {
                aim_x = targetAimTouch.X;
                aim_y = targetAimTouch.Y;
                isAim = true;
                response.touchAimActive = true;
                response.touchAimX = targetAimTouch.X;
                response.touchAimY = targetAimTouch.Y;
            } else {
                isAim = false;
            }
        } else if ((firing || ads) && nearest > 0 && aimbot && !touchSimMode) {
            const Vec3 memScr = World2Screen(cameraView, targetLoc);
            if (memScr.Z != 1.0f && memScr.X > 0.f && memScr.Y > 0.f) {
                aim_x = memScr.X;
                aim_y = memScr.Y;
                isAim = true;
                response.touchAimActive = true;
                response.touchAimX = memScr.X;
                response.touchAimY = memScr.Y;
            } else {
                isAim = false;
            }
        } else {
            aim_x = 0;
            aim_y = 0;
            isAim = false;
        }

        applySmallCrosshairForLocal(LocalPlayer);

        {
            // Single crash-safe BT tick: code patched once, only the aim data
            // slot flips (head vs. crosshair) under the throttle window.
            float btNear = -1.f;
            Vec3 btAim{};
            syncBulletTrackFast(base, request, cameraView, MyPos,
                                allowBulletTrack, btNear, btAim);
        }

        // Skeleton/box need projected PlayerBone (screen) — not just BoneWorld
        projectAllPlayerBones(response, cameraView);

        if ((debugTick & 31u) == 1u) {
            int boneOk = 0;
            for (int bi = 0; bi < response.PlayerCount && bi < maxplayerCount; bi++) {
                if (response.Players[bi].Bone.isBone) boneOk++;
            }
            LOGI("sock64 bones players=%d withBone=%d", response.PlayerCount, boneOk);
        }

        send((void *) &response, sizeof(response));

        if (request.Mode == AimSyncMode) {
            continue;
        }

    }
}

