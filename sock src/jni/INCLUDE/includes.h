#include <sys/uio.h>
#include <stdio.h>
#include <sys/types.h>
#include <unistd.h>
#include <string.h>
#include <math.h>
#include <stdlib.h>
#include <assert.h>
#include <dirent.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <sys/syscall.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <errno.h>
#include <stdio.h>
#include <sys/un.h>
#include <time.h>
#include <ctype.h>
#include <iostream>
#define LEN sizeof(struct MAPS)


struct FVector2D {
    float x, y;

    bool IsZero() const { return x == 0.f && y == 0.f; }

    float GetDistanceTo(const FVector2D& rhs) const {
        const FVector2D tmp{x - rhs.x, y - rhs.y};
        return sqrt(tmp.x * tmp.x + tmp.y * tmp.y);
    }
};

struct FVector3D {
    float x, y, z;

    bool IsZero() const { return x == 0.f && y == 0.f && z == 0.f; }

    float GetDistanceTo(const FVector3D& rhs) const {
        const FVector3D tmp{x - rhs.x, y - rhs.y, z - rhs.z};
        return sqrt(tmp.x * tmp.x + tmp.y * tmp.y + tmp.z * tmp.z);
    }
};



struct Vec4 {
    float  X, Y, Z, W;
};
struct Vec3 {
    float X;
    float Y;
    float Z;

    Vec3() {
        this->X = 0;
        this->Y = 0;
        this->Z = 0;
    }

    Vec3(float x, float y, float z) {
        this->X = x;
        this->Y = y;
        this->Z = z;
    }

    static Vec3 Zero() {
        return Vec3(0.0f, 0.0f, 0.0f);
    }

    static Vec3 Up() {
        return Vec3(0.0f, 1.0f, 0.0f);
    }

    static Vec3 Down() {
        return Vec3(0.0f, -1.0f, 0.0f);
    }

    static Vec3 Back() {
        return Vec3(0.0f, 0.0f, -1.0f);
    }

    static Vec3 Forward() {
        return Vec3(0.0f, 0.0f, 1.0f);
    }

    static Vec3 Left() {
        return Vec3(-1.0f, 0.0f, 0.0f);
    }

    static Vec3 Right() {
        return Vec3(1.0f, 0.0f, 0.0f);
    }

    float& operator[](int i) {
        return ((float*)this)[i];
    }

    float operator[](int i) const {
        return ((float*)this)[i];
    }

    bool operator==(const Vec3& src) const {
        return (src.X == X) && (src.Y == Y) && (src.Z == Z);
    }

    bool operator!=(const Vec3& src) const {
        return (src.X != X) || (src.Y != Y) || (src.Z != Z);
    }

    Vec3& operator+=(const Vec3& v) {
        X += v.X;
        Y += v.Y;
        Z += v.Z;
        return *this;
    }

    Vec3& operator-=(const Vec3& v) {
        X -= v.X;
        Y -= v.Y;
        Z -= v.Z;
        return *this;
    }

    Vec3& operator*=(float fl) {
        X *= fl;
        Y *= fl;
        Z *= fl;
        return *this;
    }

    Vec3& operator*=(const Vec3& v) {
        X *= v.X;
        Y *= v.Y;
        Z *= v.Z;
        return *this;
    }

    Vec3& operator/=(const Vec3& v) {
        X /= v.X;
        Y /= v.Y;
        Z /= v.Z;
        return *this;
    }

    Vec3& operator+=(float fl) {
        X += fl;
        Y += fl;
        Z += fl;
        return *this;
    }

    Vec3& operator/=(float fl) {
        X /= fl;
        Y /= fl;
        Z /= fl;
        return *this;
    }

    Vec3& operator-=(float fl) {
        X -= fl;
        Y -= fl;
        Z -= fl;
        return *this;
    }

    Vec3& operator=(const Vec3& vOther) {
        X = vOther.X;
        Y = vOther.Y;
        Z = vOther.Z;
        return *this;
    }

    Vec3 operator-(void) const {
        return Vec3(-X, -Y, -Z);
    }

    Vec3 operator+(const Vec3& v) const {
        return Vec3(X + v.X, Y + v.Y, Z + v.Z);
    }

    Vec3 operator-(const Vec3& v) const {
        return Vec3(X - v.X, Y - v.Y, Z - v.Z);
    }

    Vec3 operator*(float fl) const {
        return Vec3(X * fl, Y * fl, Z * fl);
    }

    Vec3 operator*(const Vec3& v) const {
        return Vec3(X * v.X, Y * v.Y, Z * v.Z);
    }

    Vec3 operator/(float fl) const {
        return Vec3(X / fl, Y / fl, Z / fl);
    }

    Vec3 operator/(const Vec3& v) const {
        return Vec3(X / v.X, Y / v.Y, Z / v.Z);
    }

    static float Dot(Vec3 lhs, Vec3 rhs) {
        return (((lhs.X * rhs.X) + (lhs.Y * rhs.Y)) + (lhs.Z * rhs.Z));
    }

    float sqrMagnitude() const {
        return (X * X + Y * Y + Z * Z);
    }

    float Magnitude() const {
        return sqrt(sqrMagnitude());
    }

    static float Distance(Vec3 a, Vec3 b) {
        Vec3 vector = Vec3(a.X - b.X, a.Y - b.Y, a.Z - b.Z);
        return sqrt(((vector.X * vector.X) + (vector.Y * vector.Y)) + (vector.Z * vector.Z));
    }
};
struct Vec3INT {
    int X, Y, Z;
};
struct Vec2 {
    float X, Y;
};


struct D3DMatrix

{
    float _11, _12, _13, _14;

    float _21, _22, _23, _24;

    float _31, _32, _33, _34;

    float _41, _42, _43, _44;

};


FVector3D Vec3ToFVector3D(const Vec3& vec) {
    FVector3D result;
    result.x = vec.X;
    result.y = vec.Y;
    result.z = vec.Z;
    return result;
}


struct Vec4 rot;
struct Vec3 scale, tran;

//deta
int height = 1080;
int width = 2340;
pid_t pid = -1;
int isBeta, nByte;
float mx = 0, my = 0, mz = 0;

struct MAPS
{
    long int fAddr;
    long int lAddr;
    struct MAPS* next;
};
struct Ulevel {
    uintptr_t addr;
    int size;
};
typedef struct MAPS* PMAPS;

#define SIZE sizeof(uintptr_t)






