#ifndef XT_CameraView_H
#define XT_CameraView_H

struct FMatrix {
    float M[4][4];
};
struct FRotator {
    float Pitch;
    float Yaw;
    float Roll;
};

struct FMinimalViewInfo
{
    Vec3 Location;
    Vec3 LocationLocalSpace;
    FRotator Rotation;
    float FOV;

};

struct FCameraCacheEntry {
    float TimeStamp;
    char pad[0xc];
    FMinimalViewInfo POV;
};
struct CameraView {
    Vec3 Location;
    Vec3 LocationLocalSpace;
    FRotator Rotation;
    float FOV;
};
CameraView cameraView = CameraView();
CameraView getCameraView(uintptr_t address) {
    CameraView buff;
    vm_readv(address, &buff, sizeof(CameraView));
    return buff;
}
FRotator ToRotator(Vec3 local, Vec3 target) {
    Vec3 rotation;
    rotation.X = local.X - target.X;
    rotation.Y = local.Y - target.Y;
    rotation.Z = local.Z - target.Z;

    FRotator newViewAngle = {0};

    float hyp = sqrt(rotation.X * rotation.X + rotation.Y * rotation.Y);

    newViewAngle.Pitch = -atan(rotation.Z / hyp) * (180.f / (float) 3.14159265358979323846);
    newViewAngle.Yaw = atan(rotation.Y / rotation.X) * (180.f / (float) 3.14159265358979323846);
    newViewAngle.Roll = (float) 0.f;

    if (rotation.X >= 0.f)
        newViewAngle.Yaw += 180.0f;

    return newViewAngle;
}

Vec3 MarixToVector(FMatrix matrix) {
    return Vec3(matrix.M[3][0], matrix.M[3][1], matrix.M[3][2]);
}
FMatrix MatrixMulti(FMatrix m1, FMatrix m2) {
    FMatrix matrix = FMatrix();
    for (int i = 0; i < 4; i++) {
        for (int j = 0; j < 4; j++) {
            for (int k = 0; k < 4; k++) {
                matrix.M[i][j] += m1.M[i][k] * m2.M[k][j];
            }
        }
    }
    return matrix;
}
FMatrix RotToMatrix(FRotator rotation) {
    float radPitch = rotation.Pitch * ((float)M_PI / 180.0f);
    float radYaw = rotation.Yaw * ((float)M_PI / 180.0f);
    float radRoll = rotation.Roll * ((float)M_PI / 180.0f);

    float SP = sinf(radPitch);
    float CP = cosf(radPitch);
    float SY = sinf(radYaw);
    float CY = cosf(radYaw);
    float SR = sinf(radRoll);
    float CR = cosf(radRoll);

    FMatrix matrix;

    matrix.M[0][0] = (CP * CY);
    matrix.M[0][1] = (CP * SY);
    matrix.M[0][2] = (SP);
    matrix.M[0][3] = 0;

    matrix.M[1][0] = (SR * SP * CY - CR * SY);
    matrix.M[1][1] = (SR * SP * SY + CR * CY);
    matrix.M[1][2] = (-SR * CP);
    matrix.M[1][3] = 0;

    matrix.M[2][0] = (-(CR * SP * CY + SR * SY));
    matrix.M[2][1] = (CY * SR - CR * SP * SY);
    matrix.M[2][2] = (CR * CP);
    matrix.M[2][3] = 0;

    matrix.M[3][0] = 0;
    matrix.M[3][1] = 0;
    matrix.M[3][2] = 0;
    matrix.M[3][3] = 1;

    return matrix;
}

Vec2 World2ScreenMain(CameraView camViewInfo, Vec3 worldLocation) {
    FMatrix tempMatrix = RotToMatrix(camViewInfo.Rotation);
    Vec3 vAxisX(tempMatrix.M[0][0], tempMatrix.M[0][1], tempMatrix.M[0][2]);
    Vec3 vAxisY(tempMatrix.M[1][0], tempMatrix.M[1][1], tempMatrix.M[1][2]);
    Vec3 vAxisZ(tempMatrix.M[2][0], tempMatrix.M[2][1], tempMatrix.M[2][2]);
    Vec3 vDelta = worldLocation - camViewInfo.Location;
    Vec3 vTransformed(Vec3::Dot(vDelta, vAxisY), Vec3::Dot(vDelta, vAxisZ), Vec3::Dot(vDelta, vAxisX));

    if (vTransformed.Z < 1.0f) { vTransformed.Z = 1.0f;}

    float fov = camViewInfo.FOV;
    float screenCenterX = (width / 2.0f);
    float screenCenterY = (height / 2.0f);

    Vec2 vec2;
    vec2.X = screenCenterX + vTransformed.X * (screenCenterX / tanf(fov * ((float)M_PI / 360))) / vTransformed.Z;
    vec2.Y = screenCenterY - vTransformed.Y * (screenCenterX / tanf(fov * ((float)M_PI / 360))) / vTransformed.Z;
    return vec2;
}

Vec3 World2Screen(CameraView camViewInfo, Vec3 worldLocation) {
    FMatrix tempMatrix = RotToMatrix(camViewInfo.Rotation);
    Vec3 vAxisX(tempMatrix.M[0][0], tempMatrix.M[0][1], tempMatrix.M[0][2]);
    Vec3 vAxisY(tempMatrix.M[1][0], tempMatrix.M[1][1], tempMatrix.M[1][2]);
    Vec3 vAxisZ(tempMatrix.M[2][0], tempMatrix.M[2][1], tempMatrix.M[2][2]);
    Vec3 vDelta = worldLocation - camViewInfo.Location;
    Vec3 vTransformed(Vec3::Dot(vDelta, vAxisY), Vec3::Dot(vDelta, vAxisZ), Vec3::Dot(vDelta, vAxisX));

    if (vTransformed.Z < 1.0f) { vTransformed.Z = 1.0f;}

    float fov = camViewInfo.FOV;
    float screenCenterX = (width / 2.0f);
    float screenCenterY = (height / 2.0f);

    Vec3 vec3;

    if (tanf(fov * ((float)M_PI / 360)) < 0.01f)
        vec3.Z = 1;
    else
        vec3.Z = 0;
    vec3.X = screenCenterX + vTransformed.X * (screenCenterX / tanf(fov * ((float)M_PI / 360))) / vTransformed.Z;
    vec3.Y = screenCenterY - vTransformed.Y * (screenCenterX / tanf(fov * ((float)M_PI / 360))) / vTransformed.Z;
    return vec3;
}

static inline Vec3 calcMemoryAimAnglesFromScreen(float screenX, float screenY, float screenPitchAdj,
                                                 const CameraView &cam) {
    const float cx = width * 0.5f;
    const float cy = height * 0.5f;
    const float errX = screenX - cx;
    const float errY = (screenY + screenPitchAdj) - cy;
    const float halfFov = cam.FOV * ((float) M_PI / 360.f);
    const float pxScale = tanf(halfFov) / fmaxf(cx, 1.f);
    Vec3 ang;
    ang.X = cam.Rotation.Yaw + atanf(errX * pxScale) * (180.f / (float) M_PI);
    ang.Y = cam.Rotation.Pitch - atanf(errY * pxScale) * (180.f / (float) M_PI);
    ang.Z = 0.f;
    while (ang.X > 180.f) ang.X -= 360.f;
    while (ang.X < -180.f) ang.X += 360.f;
    return ang;
}

static inline Vec3 calcMemoryAimAngles(Vec3 targetLoc, const CameraView &cam, float screenPitchAdj) {
    Vec3 screen = World2Screen(cam, targetLoc);
    if (screen.Z != 0.f) {
        return CalcMousePos(targetLoc, cam.Location);
    }
    return calcMemoryAimAnglesFromScreen(screen.X, screen.Y, screenPitchAdj, cam);
}

/** Same angles as the working memory aimbot (screen FOV math + scope pitch adj). */
static inline Vec3 calcUnifiedBodyAimLock(const Vec3 &bodyWorld, const CameraView &cam,
                                          float screenPitchAdj) {
    return calcMemoryAimAngles(bodyWorld, cam, screenPitchAdj);
}

FRotator CalcAngle(Vec3 LocalHeadPosition, Vec2 AimPosition)
{
    Vec3 rotation = LocalHeadPosition;
    float hyp = sqrt(rotation.X * rotation.X + rotation.Y * rotation.Y);

    FRotator newAngle = FRotator();

    newAngle.Pitch = (-atan(rotation.Z / hyp) * (180.0f /(float) 3.14159265358979323846));
    newAngle.Yaw = (atan(rotation.Y / rotation.X) * (180.0f /(float) 3.14159265358979323846));
    newAngle.Roll = 0.0f;

    if (rotation.X >= 0.0f)
        newAngle.Yaw += 180.0f;

    return newAngle;
}

struct Vec2 World2RadarRectangle(struct FCameraCacheEntry CameraCache, struct Vec3 EnemyPos, struct Vec2 RadarPos, float RadarSize) {
    //Agak pusing dikit jir 2 jam bikin kode ini doang
    struct Vec2 results;
    FRotator qAngle = CameraCache.POV.Rotation;
    Vec3 CameraLoc = CameraCache.POV.Location;
    float flDeltaX = EnemyPos.X - CameraLoc.X;
    float flDeltaY = EnemyPos.Y - CameraLoc.Y;
    float flAngle = qAngle.Yaw;
    float flYaw = (flAngle) * (3.14159265358979323846f / 180.0);
    float flMainViewAngles_CosYaw = cos(flYaw);
    float flMainViewAngles_SinYaw = sin(flYaw);
    // rotate
    float x = flDeltaY * (-flMainViewAngles_CosYaw) + flDeltaX * flMainViewAngles_SinYaw;
    float y = flDeltaX * (-flMainViewAngles_CosYaw) - flDeltaY * flMainViewAngles_SinYaw;
    y = -y;
    x = -x;
    float clipAngle = 10000; // 3000 = 30m ini tuh maksudnya untuk jangkauan dari radarnya mau berapa meter biar keliatan di map
    if (fabs(x) > clipAngle || fabs(y) > clipAngle){
        x = (y > -x ? (y > x ? clipAngle * x / y : clipAngle) : (y > x ? -clipAngle * x / y : -clipAngle));
        y = (y > -x ? (y > x ? clipAngle : clipAngle * y / x) : (y > x ? -clipAngle : -clipAngle * y / x));
    }
    results = {RadarPos.X + int(x / clipAngle * RadarSize), RadarPos.Y - int(y / clipAngle * RadarSize)};
    return results;//Code credits @SubaktiKesumaGMG & Unkowncheats for lil bit theory
}

struct Vec2 World2RadarRound(struct FCameraCacheEntry CameraCache, struct Vec3 EnemyPos, struct Vec2 RadarPos, float RadarSize) {
    //Remake the code for rounded minimap, Lumayan bikin pusing muter muter walaupun terlihat simpel hasilnya
    struct Vec2 results;
    FRotator qAngle = CameraCache.POV.Rotation;
    Vec3 CameraLoc = CameraCache.POV.Location;
    float flDeltaX = EnemyPos.X - CameraLoc.X;
    float flDeltaY = EnemyPos.Y - CameraLoc.Y;
    float flAngle = qAngle.Yaw;
    float flYaw = (flAngle) * (3.14159265358979323846f / 180.0);
    float flMainViewAngles_CosYaw = cos(flYaw);
    float flMainViewAngles_SinYaw = sin(flYaw);
    // rotate
    float x = flDeltaY * (-flMainViewAngles_CosYaw) + flDeltaX * flMainViewAngles_SinYaw;
    float y = flDeltaX * (-flMainViewAngles_CosYaw) - flDeltaY * flMainViewAngles_SinYaw;
    y = -y;
    x = -x;
    double vectorLength = sqrt((x * x) + (y * y));
    float clipAngle = 10000;
    if (vectorLength > clipAngle) {
        // Bikin Kode gini doang gw berjam jam mikir teorinya :)
        x = (x / vectorLength) * clipAngle;
        y = (y / vectorLength) * clipAngle;
    }
    results = {RadarPos.X + int(x / clipAngle * RadarSize), RadarPos.Y - int(y / clipAngle * RadarSize)};
    return results;//Code credits @SubaktiKesumaGMG & Unkowncheats for lil bit theory
}

#endif //XT_CameraView_H



