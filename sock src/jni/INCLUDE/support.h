#ifndef XT_ESP_SUPPORT_H
#define XT_ESP_SUPPORT_H

#include "socket.h"
#include "mem_access.h"
#include <unistd.h>
#include <cstdlib>
#define PI 3.141592653589793238

extern int32_t gGMemFD;

bool pvm(void *address, void *buffer, size_t size, bool iswrite)
{
	return iswrite ? xt_mem::store((uintptr_t) address, buffer, size)
	               : xt_mem::load((uintptr_t) address, buffer, size);
}

template<typename T>
T Read(unsigned long address) {
    T data{};
    xt_mem::load(address, &data, sizeof(T));
    return data;
}

static inline bool vm_readv(unsigned long address, void *buffer, size_t size) {
	return xt_mem::load(address, buffer, size);
}

static inline bool vm_writev(unsigned long address, void *buffer, size_t size) {
	return xt_mem::store(address, buffer, size);
}
template<typename T>
void Writes(unsigned long address, T data) {
    pvm(reinterpret_cast < void *>(address), reinterpret_cast<void *>(&data), sizeof(T), true);
}

template <typename T>
struct TArray {
    uintptr_t base;
    int32_t count;
    int32_t max;

    std::vector<T> ToVec() const {
        if (!IsValid()) {
            return {};
        }
        std::vector<T> vec{};
        vec.resize(static_cast<size_t>(count));
        const size_t bytes = static_cast<size_t>(count) * sizeof(T);
        if (!vm_readv(base, vec.data(), bytes)) {
            return {};
        }
        return vec;
    }

    T operator[](size_t u) const {
        return Read<T>(base + u * sizeof(T));
    }

    bool IsValid() const {
        return base && count > 0 && count <= max && max > 0;
    }
};
//start 64
struct ActorsEncryption64 {
    uint64_t Enc_1, Enc_2;
    uint64_t Enc_3, Enc_4;
};

struct Encryption_Chunk64 {
    uint32_t val_1, val_2, val_3, val_4;
    uint32_t val_5, val_6, val_7, val_8;
};

uint64_t DecryptActorsArray64(uint64_t uLevel, int Actors_Offset, int EncryptedActors_Offset) {
    if (uLevel < 0x10000000)
        return 0;

    if (Read<uint64_t>(uLevel + Actors_Offset) > 0)
        return uLevel + Actors_Offset;

    if (Read<uint64_t>(uLevel + EncryptedActors_Offset) > 0)
        return uLevel + EncryptedActors_Offset;

    auto Encryption = Read<ActorsEncryption64>(uLevel + EncryptedActors_Offset + 0x10);

    if (Encryption.Enc_1 > 0) {
        auto Enc = Read<Encryption_Chunk64>(Encryption.Enc_1 + 0x80);
        return (((Read<uint8_t>(Encryption.Enc_1 + Enc.val_1)
                  | (Read<uint8_t>(Encryption.Enc_1 + Enc.val_2) << 8))
                 | (Read<uint8_t>(Encryption.Enc_1 + Enc.val_3) << 0x10)) & 0xFFFFFF
                | ((uint64_t) Read<uint8_t>(Encryption.Enc_1 + Enc.val_4) << 0x18)
                | ((uint64_t) Read<uint8_t>(Encryption.Enc_1 + Enc.val_5) << 0x20)) &
               0xFFFF00FFFFFFFFFF
               | ((uint64_t) Read<uint8_t>(Encryption.Enc_1 + Enc.val_6) << 0x28)
               | ((uint64_t) Read<uint8_t>(Encryption.Enc_1 + Enc.val_7) << 0x30)
               | ((uint64_t) Read<uint8_t>(Encryption.Enc_1 + Enc.val_8) << 0x38);
    } else if (Encryption.Enc_2 > 0) {
        auto Encrypted_Actors = Read<uint64_t>(Encryption.Enc_2);
        if (Encrypted_Actors > 0) {
            return (uint16_t) (Encrypted_Actors - 0x400) & 0xFF00
                   | (uint8_t) (Encrypted_Actors - 0x04)
                   | (Encrypted_Actors + 0xFC0000) & 0xFF0000
                   | (Encrypted_Actors - 0x4000000) & 0xFF000000
                   | (Encrypted_Actors + 0xFC00000000) & 0xFF00000000
                   | (Encrypted_Actors + 0xFC0000000000) & 0xFF0000000000
                   | (Encrypted_Actors + 0xFC000000000000) & 0xFF000000000000
                   | (Encrypted_Actors - 0x400000000000000) & 0xFF00000000000000;
        }
    } else if (Encryption.Enc_3 > 0) {
        auto Encrypted_Actors = Read<uint64_t>(Encryption.Enc_3);
        if (Encrypted_Actors > 0)
            return (Encrypted_Actors >> 0x38) | (Encrypted_Actors << (64 - 0x38));
    } else if (Encryption.Enc_4 > 0) {
        auto Encrypted_Actors = Read<uint64_t>(Encryption.Enc_4);
        if (Encrypted_Actors > 0)
            return Encrypted_Actors ^ 0xCDCD00;
    }
    return 0;
}
//end 64

//start32
struct ActorsEncryption32 {
	uint32_t Enc_1, Enc_2;
	uint32_t Enc_3, Enc_4;
};
 
struct Encryption_Chunk32 {
	uint32_t val_1, val_2;
	uint32_t val_3, val_4;
};

uint32_t DecryptActorsArray32(uint32_t uLevel, int Actors_Offset, int EncryptedActors_Offset)
{
	if (uLevel < 0x10000000)
		return 0;
 
	if (Read<uint32_t>(uLevel + Actors_Offset) > 0)
		return uLevel + Actors_Offset;
 
	if (Read<uint32_t>(uLevel + EncryptedActors_Offset) > 0)
		return uLevel + EncryptedActors_Offset;
 
	auto Encryption = Read<ActorsEncryption32>(uLevel + EncryptedActors_Offset + 0x0C);
 
	if (Encryption.Enc_1 > 0)
	{
		auto Enc = Read<Encryption_Chunk32>(Encryption.Enc_1 + 0x80);
 
		return (Read<uint8_t>(Encryption.Enc_1 + Enc.val_1)
			| (Read<uint8_t>(Encryption.Enc_1 + Enc.val_2) << 8)
			| (Read<uint8_t>(Encryption.Enc_1 + Enc.val_3) << 0x10)
			| (Read<uint8_t>(Encryption.Enc_1 + Enc.val_4) << 0x18));
	}
	else if (Encryption.Enc_2 > 0)
	{
		auto Encrypted_Actors = Read<uint32_t>(Encryption.Enc_2);
		if (Encrypted_Actors > 0)
		{
			return ((unsigned short)Encrypted_Actors - 0x400) & 0xFF00
				| (unsigned char)(Encrypted_Actors - 0x04)
				| (Encrypted_Actors + 0xFC0000) & 0xFF0000
				| (Encrypted_Actors - 0x4000000) & 0xFF000000;
		}
	}
	else if (Encryption.Enc_3 > 0)
	{
		auto Encrypted_Actors = Read<uint32_t>(Encryption.Enc_3);
		if (Encrypted_Actors > 0)
			return (Encrypted_Actors >> 0x18) | (Encrypted_Actors << (32 - 0x18));
	}
	else if (Encryption.Enc_4 > 0)
	{
		auto Encrypted_Actors = Read<uint32_t>(Encryption.Enc_4);
		if (Encrypted_Actors > 0)
			return Encrypted_Actors ^ 0xCDCD00;
	}
	return 0;
}

//end32


float get_3D_Distance(float Self_x, float Self_y, float Self_z, float Object_x, float Object_y, float Object_z)
{
	float x, y, z;
	x = Self_x - Object_x;
	y = Self_y - Object_y;
	z = Self_z - Object_z;
	return (float)(sqrt(x * x + y * y + z * z));
}

struct D3DMatrix ToMatrixWithScale(struct Vec3 translation,struct Vec3 scale,struct Vec4 rot)
{
	struct D3DMatrix m;

	m._41 = translation.X;
	m._42 = translation.Y;
	m._43 = translation.Z;

	float x2 = rot.X + rot.X;
	float y2 = rot.Y + rot.Y;
	float z2 = rot.Z + rot.Z;

	float xx2 = rot.X * x2;
	float yy2 = rot.Y * y2;
	float zz2 = rot.Z * z2;

	m._11 = (1.0f - (yy2 + zz2)) * scale.X;
	m._22 = (1.0f - (xx2 + zz2)) * scale.Y;
	m._33 = (1.0f - (xx2 + yy2)) * scale.Z;

	float wx2 = rot.W * x2;
	float yz2 = rot.Y * z2;

	m._23 = (yz2 + wx2) * scale.Y;
	m._32 = (yz2 - wx2) * scale.Z;

	float xy2 = rot.X * y2;
	float wz2 = rot.W * z2;

	m._12 = (xy2 + wz2) * scale.X;
	m._21 = (xy2 - wz2) * scale.Y;

	float xz2 = rot.X * z2;
	float wy2 = rot.W * y2;

	m._13 = (xz2 - wy2) * scale.X;
	m._31 = (xz2 + wy2) * scale.Z;

	m._14 = 0.0f;
	m._24 = 0.0f;
	m._34 = 0.0f;
	m._44 = 1.0f;

	return m;
}

struct Vec3 mat2Cord(struct D3DMatrix pM1,struct D3DMatrix pM2)
{
	struct  Vec3 pOut;

	pOut.X = pM1._41 * pM2._11 + pM1._42 * pM2._21 + pM1._43 * pM2._31 + pM1._44 * pM2._41;
	pOut.Y = pM1._41 * pM2._12 + pM1._42 * pM2._22 + pM1._43 * pM2._32 + pM1._44 * pM2._42;
	pOut.Z = pM1._41 * pM2._13 + pM1._42 * pM2._23 + pM1._43 * pM2._33 + pM1._44 * pM2._43;

	return pOut;
}

static uintptr_t sCachedUe4Base = 0;

static inline void clearUe4BaseCache() { sCachedUe4Base = 0; }

static bool procAlive(pid_t p) {
    if (p < 10) {
        return false;
    }
    char path[48];
    snprintf(path, sizeof(path), "/proc/%d/status", (int) p);
    return access(path, F_OK) == 0;
}

uintptr_t getBase() {
    if (sCachedUe4Base != 0) {
        return sCachedUe4Base;
    }
    if (pid < 10) {
        return 0;
    }
    FILE *fp;
    char filename[32], buffer[1024];
    snprintf(filename, sizeof(filename), "/proc/%d/maps", pid);
    fp = fopen(filename, "rt");
    if (fp == nullptr) {
        return 0;
    }

    uintptr_t bestByElf = 0;
    uintptr_t bestByOffset = 0;
    uintptr_t minAny = 0;
    bool any = false;

    while (fgets(buffer, sizeof(buffer), fp)) {
        if (!strstr(buffer, "libUE4.so")) {
            continue;
        }
        uintptr_t start = 0, end = 0, fileOff = 0;
        char perms[8] = {};
#if defined(__LP64__)
        // 78abc000-78def000 r-xp 00123000 ...
        if (sscanf(buffer, "%lx-%lx %7s %lx", &start, &end, perms, &fileOff) < 3 || start == 0) {
            continue;
        }
#else
        if (sscanf(buffer, "%x-%x %7s %x", &start, &end, perms, &fileOff) < 3 || start == 0) {
            continue;
        }
#endif
        if (!any || start < minAny) {
            minAny = start;
            any = true;
        }
        // ELF load bias = mapping_start - file_offset (correct ASLR base)
        if (fileOff < start) {
            uintptr_t cand = start - fileOff;
            if (bestByOffset == 0 || cand < bestByOffset) {
                bestByOffset = cand;
            }
        }
        // Confirm ELF magic at candidate
        uint32_t magic = 0;
        if (vm_readv(start, &magic, sizeof(magic)) && magic == 0x464C457Fu) {
            if (bestByElf == 0 || start < bestByElf) {
                bestByElf = start;
            }
        }
        if (fileOff < start) {
            uintptr_t cand = start - fileOff;
            magic = 0;
            if (vm_readv(cand, &magic, sizeof(magic)) && magic == 0x464C457Fu) {
                if (bestByElf == 0 || cand < bestByElf) {
                    bestByElf = cand;
                }
            }
        }
    }
    fclose(fp);

    uintptr_t base = bestByElf ? bestByElf : (bestByOffset ? bestByOffset : minAny);
    if (base != 0) {
        sCachedUe4Base = base;
    }
    return base;
}

static bool hasUe4Maps(pid_t p) {
	char mapsPath[64];
	char line[512];
	snprintf(mapsPath, sizeof(mapsPath), "/proc/%d/maps", (int) p);
	FILE *fp = fopen(mapsPath, "r");
	if (!fp) return false;
	bool found = false;
	while (fgets(line, sizeof(line), fp)) {
		if (strstr(line, "libUE4.so")) {
			found = true;
			break;
		}
	}
	fclose(fp);
	return found;
}

static pid_t readEnvGamePid() {
	const char *env = getenv("XT_GAME_PID");
	if (env == nullptr || env[0] == '\0') {
		return 0;
	}
	int p = atoi(env);
	if (p >= 10 && procAlive((pid_t) p)) {
		return (pid_t) p;
	}
	return 0;
}

static pid_t readHostGamePidFile() {
	pid_t fromEnv = readEnvGamePid();
	if (fromEnv >= 10) {
		return fromEnv;
	}
	const char *paths[] = {
			"/data/data/lauresprojects.com.recorder/files/game.pid",
			"/data/user/0/lauresprojects.com.recorder/files/game.pid",
			"/data/data/com.blazehealth.tracker/files/game.pid",
			"/data/user/0/com.blazehealth.tracker/files/game.pid",
			nullptr
	};
	for (int i = 0; paths[i] != nullptr; i++) {
		FILE *f = fopen(paths[i], "r");
		if (!f) continue;
		int p = 0;
		if (fscanf(f, "%d", &p) == 1 && p >= 10 && procAlive((pid_t) p)) {
			fclose(f);
			return (pid_t) p;
		}
		fclose(f);
	}
	return 0;
}

static pid_t findBlackboxGuestPid(const char *guestPkg, const char *hostPkg) {
	DIR *dir = opendir("/proc");
	if (dir == NULL) return 0;
	struct dirent *ptr;
	char filepath[256];
	char filetext[256];
	pid_t best = 0;
	while ((ptr = readdir(dir)) != NULL) {
		if (ptr->d_type != DT_DIR) continue;
		int id = atoi(ptr->d_name);
		if (id < 10) continue;
		if (!hasUe4Maps((pid_t) id)) continue;

		snprintf(filepath, sizeof(filepath), "/proc/%s/cmdline", ptr->d_name);
		FILE *fp = fopen(filepath, "r");
		if (fp == NULL) continue;
		memset(filetext, 0, sizeof(filetext));
		fread(filetext, 1, sizeof(filetext) - 1, fp);
		fclose(fp);

		if (guestPkg != nullptr && strstr(filetext, guestPkg) != NULL) {
			closedir(dir);
			return (pid_t) id;
		}
		if (hostPkg != nullptr && strstr(filetext, hostPkg) != NULL && strchr(filetext, ':') != NULL) {
			best = (pid_t) id;
		}
	}
	closedir(dir);
	return best;
}

pid_t getPid(char * name)
{
	pid_t pid = 0;
#if !defined(XT_MEM_STEALTH)
	char text[69];
	sprintf(text,"pidof %s",name);
	FILE *chkRun = popen(text, "r");
	if (chkRun)
	{
		char output[16];
		fgets(output ,sizeof(output),chkRun);
		pclose(chkRun);
		pid= atoi(output);
	}
	if (pid >= 10 && hasUe4Maps(pid)) {
		return pid;
	}
#endif
	pid = 0;

	DIR* dir = opendir("/proc");
	if (dir != NULL) {
		struct dirent* ptr;
		char filepath[256];
		char filetext[256];
		while ((ptr = readdir(dir)) != NULL) {
			if (ptr->d_type != DT_DIR) continue;
			int id = atoi(ptr->d_name);
			if (id < 10) continue;

			snprintf(filepath, sizeof(filepath), "/proc/%s/cmdline", ptr->d_name);
			FILE* fp = fopen(filepath, "r");
			if (fp == NULL) continue;
			memset(filetext, 0, sizeof(filetext));
			fread(filetext, 1, sizeof(filetext) - 1, fp);
			fclose(fp);

			if (strstr(filetext, name) != NULL) {
				if (hasUe4Maps((pid_t) id)) {
					closedir(dir);
					return (pid_t) id;
				}
			}
		}
		closedir(dir);
	}

	pid_t bb = findBlackboxGuestPid(name, "lauresprojects.com.recorder");
	if (bb < 10) {
		bb = findBlackboxGuestPid(name, "com.blazehealth.tracker");
	}
	if (bb >= 10) {
		return bb;
	}
	return 0;
}

float getF(uintptr_t address)
{
	float buff;
	vm_readv(address, &buff, 4);
	return buff;
}

uintptr_t getA(uintptr_t address)
{
	uintptr_t buff = 0;
	if (!vm_readv(address, &buff, SIZE)) {
		return 0;
	}
	return buff;
}

int getI(uintptr_t address)
{
	int buff;
	vm_readv(address, &buff, 4);
	return buff;
}

Vec3 Multiply_VectorFloat(const Vec3& Vec, float Scale)
{
    Vec3 multiply = {Vec.X * Scale, Vec.Y * Scale, Vec.Z * Scale};
    return multiply;
}

Vec3 Add_VectorVector(struct Vec3 Vect1, struct Vec3 Vect2)
{
    Vec3 add;
    add.X = Vect1.X + Vect2.X;
    add.Y = Vect1.Y + Vect2.Y;
    add.Z = Vect1.Z + Vect2.Z;
    return add;
}

Vec3 getVec3(uintptr_t addr) {
    Vec3 vec;
    vm_readv(addr, &vec, sizeof(vec));
    return vec;
}


int isValidItem(int id)
{
	if (id >= 100000 && id < 999999)
		return 1;
    return 0;
}

int isValid64(uintptr_t addr)
{
    if (addr < 0x1000000000 || addr>0xefffffffff || addr % SIZE != 0)
        return 0;
    return 1;
}

// ---- DeltaForce-style robust pointer decode (BGMI encrypted slots) ----
// Same idea as DecodeBgmiObjectPtr: a slot value can already be a valid
// pointer, a pointer-to-pointer, or an encoded value that needs -0x20.
static inline bool IsPtrPlausible(uintptr_t p) {
    return isValid64(p) != 0;
}

#ifndef BGMI_DECODE_SUB
#define BGMI_DECODE_SUB 0x20
#endif

// slot = raw qword already read from memory (e.g. *(base+GWorld)).
static inline uintptr_t DecodeBgmiObjectPtr(uintptr_t slot) {
    if (!slot) {
        return 0;
    }
    // 1) already a valid object pointer
    if (IsPtrPlausible(slot)) {
        return slot;
    }
    // 2) pointer-to-pointer
    uintptr_t deref = getA(slot);
    if (IsPtrPlausible(deref)) {
        return deref;
    }
    // 3) encoded — object lives at slot - 0x20
    if (slot >= (uintptr_t) BGMI_DECODE_SUB) {
        uintptr_t decoded = getA(slot - (uintptr_t) BGMI_DECODE_SUB);
        if (IsPtrPlausible(decoded)) {
            return decoded;
        }
    }
    return 0;
}

int isValid32(uintptr_t addr)
{
    if (addr < 0x10000000 || addr>0xefffffff || addr % SIZE != 0)
        return 0;
    return 1;
}

float getDistance(struct Vec3 mxyz,struct Vec3 exyz)
{
	return sqrt ((mxyz.X-exyz.X)*(mxyz.X-exyz.X)+(mxyz.Y-exyz.Y)*(mxyz.Y-exyz.Y)+(mxyz.Z-exyz.Z)*(mxyz.Z-exyz.Z))/100;
}

struct Vec3 World2Screen(struct D3DMatrix viewMatrix, struct Vec3 pos)
{
	struct Vec3 screen;
	float screenW = (viewMatrix._14 * pos.X) + (viewMatrix._24 * pos.Y) + (viewMatrix._34 * pos.Z) + viewMatrix._44;

	if (screenW < 0.01f)
		screen.Z = 1;
	else
		screen.Z = 0;

	float screenX = (viewMatrix._11 * pos.X) + (viewMatrix._21 * pos.Y) + (viewMatrix._31 * pos.Z) + viewMatrix._41;
	float screenY = (viewMatrix._12 * pos.X) + (viewMatrix._22 * pos.Y) + (viewMatrix._32 * pos.Z) + viewMatrix._42;
	screen.Y = (height / 2) - (height / 2) * screenY / screenW;
	screen.X = (width / 2) + (width / 2) * screenX / screenW;

	return screen;
}

struct Vec2 World2ScreenMain(struct D3DMatrix viewMatrix, struct Vec3 pos)
{
	struct Vec2 screen;
	float screenW = (viewMatrix._14 * pos.X) + (viewMatrix._24 * pos.Y) + (viewMatrix._34 * pos.Z) + viewMatrix._44;

	float screenX = (viewMatrix._11 * pos.X) + (viewMatrix._21 * pos.Y) + (viewMatrix._31 * pos.Z) + viewMatrix._41;
	float screenY = (viewMatrix._12 * pos.X) + (viewMatrix._22 * pos.Y) + (viewMatrix._32 * pos.Z) + viewMatrix._42;
	screen.Y = (height / 2) - (height / 2) * screenY / screenW;
	screen.X = (width / 2) + (width / 2) * screenX / screenW;

	return screen;
}

struct D3DMatrix getOMatrix(uintptr_t boneAddr)
{
    float oMat[11];
    vm_readv(boneAddr,&oMat,sizeof(oMat));
    rot.X=oMat[0];
	rot.Y=oMat[1];
	rot.Z=oMat[2];
	rot.W=oMat[3];
			
	tran.X=oMat[4];
	tran.Y=oMat[5];
	tran.Z=oMat[6];
			
	scale.X=oMat[8];
	scale.Y=oMat[9];
	scale.Z=oMat[10];
			
	return ToMatrixWithScale(tran,scale,rot);
}

Vec3 CalcMousePos(Vec3 headPos, Vec3 uMyobejctPos) {
    Vec3 AimPos;
    float x = headPos.X - uMyobejctPos.X;
    float y = headPos.Y - uMyobejctPos.Y;
    float z = headPos.Z - uMyobejctPos.Z;
    AimPos.X = atan2(y, x) * 180.f / M_PI;
    AimPos.Y = atan2(z, sqrt(x * x + y * y)) * 180.f / PI;
    return AimPos;
}
int isapkrunning(char * name) {
    DIR *dir = NULL;
    struct dirent *ptr = NULL;
    FILE *fp = NULL;
    char filepath[50];
    char filetext[128];
    dir = opendir("/proc/");
    if (dir != NULL) {
        while ((ptr = readdir(dir)) != NULL) {
            if ((strcmp(ptr->d_name, ".") == 0) || (strcmp(ptr->d_name, "..") == 0))
                continue;
            if (ptr->d_type != DT_DIR)
                continue;
            sprintf(filepath, "/proc/%s/cmdline", ptr->d_name);
            fp = fopen(filepath, "r");
            if (NULL != fp) {
                fgets(filetext, sizeof(filetext), fp);
                if (strcmp(filetext, name) == 0) {
                    closedir(dir);
                    return 1;
                }
                fclose(fp);
            }
        }
    }
    closedir(dir);
    return 0;
}
char* getText(uintptr_t addr)
{
	static char txt[42];
	memset(txt, 0, 42);
	char buff[41];
	vm_readv(addr + 4+SIZE, &buff, 41);
	int i;
	for (i = 0; i < 41; i++) {
		if (buff[i] == 0)
			break;
		txt[i] = buff[i];
		if (buff[i] == 67 && i > 10)
			break;
	}
	int end = i + 1;
	if (end > 41) end = 41;
	txt[end] = '\0';
	return txt;
}

char *getNameByte(uintptr_t address)
{
	// Encoded form "65:66:..." — must fit PlayerNameByte[100]; never strcat without bounds.
	static char lj[100];
	memset(lj, 0, sizeof(lj));
	unsigned short nameI[32];
	memset(nameI, 0, sizeof(nameI));
	vm_readv(address, nameI, sizeof(nameI));
	size_t used = 0;
	for (int i = 0; i < 32; i++)
	{
		if (nameI[i] == 0) {
			break;
		}
		// Cap nonsense wchar values (bot/garbage FStrings)
		const unsigned v = nameI[i] > 0x10FFFF ? 0 : (unsigned)nameI[i];
		if (v == 0) {
			break;
		}
		char s[16];
		const int n = snprintf(s, sizeof(s), "%u:", v);
		if (n <= 0) {
			break;
		}
		if (used + (size_t)n >= sizeof(lj)) {
			break;
		}
		memcpy(lj + used, s, (size_t)n);
		used += (size_t)n;
		lj[used] = '\0';
	}
	// Drop trailing ':' so Java split does not get empty token
	while (used > 0 && lj[used - 1] == ':') {
		lj[--used] = '\0';
	}
	return lj;
}

void dump(const uintptr_t gaddr, const int gsize, char* name)
{
	char buff[0x100000];
	uintptr_t addr = gaddr;
	int size = gsize;
	FILE* fp = fopen(name, "w");
	while (size > 0) {
		if (size < 0x100000) {
			vm_readv(addr, buff, size);
			for (int i = 0; i < size; i++)
				fwrite(&buff[i], 1, 1, fp);
		} else {
			vm_readv(addr, buff, 0x100000);
			for (int i = 0; i < 0x100000; i++)
				fwrite(&buff[i], 1, 1, fp);
		}
		addr += 0x100000;
		size -= 0x100000;
	}
	fclose(fp);
}

void getGNameRes(uintptr_t gname, uintptr_t pBase, char *name) {
 auto aaa = Read<uint32_t>(pBase + 0x18);
 auto fffarr = getA(gname + ((aaa / 0x4000) * 0x8));
    auto fff = getA(fffarr + ((aaa % 0x4000) * 0x8));
 strcpy(name , getText(fff));
}

/*
 * Cached FName resolver. FName-index -> string never changes for a running game,
 * so after warmup most actors resolve with a single index read + a hash hit.
 * This collapses ~4 syscalls/actor down to ~1, killing the per-scan lag.
 */
#define XT_FNAME_CACHE_SIZE 16384
#define XT_GNAME_CHUNK_CACHE 1024

struct XtFNameSlot {
    uint32_t key;
    uint8_t used;
    char txt[42];
};

static XtFNameSlot sXtFNameCache[XT_FNAME_CACHE_SIZE];
static uintptr_t sXtGNameChunkPtr[XT_GNAME_CHUNK_CACHE];
static uint8_t sXtGNameChunkHas[XT_GNAME_CHUNK_CACHE];
static uintptr_t sXtGNameBase = 0;

static inline void resetNameCaches(uintptr_t gnameBase) {
    sXtGNameBase = gnameBase;
    memset(sXtGNameChunkHas, 0, sizeof(sXtGNameChunkHas));
    memset(sXtFNameCache, 0, sizeof(sXtFNameCache));
}

void getGNameResCached(uintptr_t gname, uintptr_t pBase, char *name) {
    if (gname != sXtGNameBase) {
        resetNameCaches(gname);
    }
    uint32_t idx = Read<uint32_t>(pBase + 0x18);
    uint32_t h = idx & (XT_FNAME_CACHE_SIZE - 1);
    for (int probe = 0; probe < 8; probe++) {
        XtFNameSlot &s = sXtFNameCache[(h + probe) & (XT_FNAME_CACHE_SIZE - 1)];
        if (s.used && s.key == idx) {
            strcpy(name, s.txt);
            return;
        }
        if (!s.used) {
            break;
        }
    }

    uint32_t chunk = idx / 0x4000;
    uintptr_t chunkPtr;
    if (chunk < XT_GNAME_CHUNK_CACHE && sXtGNameChunkHas[chunk]) {
        chunkPtr = sXtGNameChunkPtr[chunk];
    } else {
        chunkPtr = getA(gname + chunk * 0x8);
        if (chunk < XT_GNAME_CHUNK_CACHE) {
            sXtGNameChunkPtr[chunk] = chunkPtr;
            sXtGNameChunkHas[chunk] = 1;
        }
    }
    uintptr_t entry = getA(chunkPtr + (idx % 0x4000) * 0x8);

    char tmp[42];
    strcpy(tmp, getText(entry));
    strcpy(name, tmp);

    for (int probe = 0; probe < 8; probe++) {
        XtFNameSlot &s = sXtFNameCache[(h + probe) & (XT_FNAME_CACHE_SIZE - 1)];
        if (!s.used || s.key == idx) {
            s.used = 1;
            s.key = idx;
            strncpy(s.txt, tmp, 41);
            s.txt[41] = '\0';
            return;
        }
    }
}

enum type {
	TYPE_DWORD,
	TYPE_FLOAT
};

void WriteDword(uintptr_t address, int value) {
    pvm(reinterpret_cast<void *>(address), reinterpret_cast<void *>(&value), 4, true);
}

void WriteFloat(uintptr_t address, float value) {
    pvm(reinterpret_cast<void *>(address), reinterpret_cast<void *>(&value), 4, true);
}

void Write(uintptr_t address, const char *value, type TYPE) {
    switch (TYPE) {
        case TYPE_DWORD:
            WriteDword(address, atoi(value));
            break;
        case TYPE_FLOAT:
            WriteFloat(address, atof(value));
            break;
    }
}

bool ProcessRead1(void *address, void *buffer, size_t size, bool write = false) {
    return write ? xt_mem::store((uintptr_t) address, buffer, size)
                 : xt_mem::load((uintptr_t) address, buffer, size);
}


bool PVM_WriteAddr(void *addr, void *buffer, size_t length) {
    return ProcessRead1(addr, buffer, length, true);
}

template<typename T>
void Write2(uintptr_t addr, T value) {
    PVM_WriteAddr((void *) addr, &value, sizeof(T));
}

float GetPitch(float Fov,float compe,float dist) {
    if ((int) Fov == 70) // no scope
    {
        return ((30.0f / 30) * compe) + dist;
    } else if ((int) Fov == 55) // redot
    {
		return ((33.75f / 30) * compe) + dist;
		
	} else if ((int) Fov == 44) //x2
	{
        return ((45.0f / 30) * compe) + dist;
		
	} else if ((int) Fov == 26) //x3
	{
		return ((60.0f / 30) * compe) + dist;
		
	} else if ((int) Fov == 20)// x4
	{
		return ((86.25f / 30) * compe) + dist;
		
	} else if ((int) Fov == 13)// x6
	{
	    return ((112.5f / 30) * compe) + dist;
		
    } else { // no open scope and x8
		return ((12.0f / 30) * compe) + dist;
	}
    
}

float GetPitchCustom(float Fov,float compe,float dist,float recScope[9]) {
    if ((int) Fov == 11) //x8
    {
        return ((recScope[8] / 30) * compe);
    } 
	else if ((int) Fov == 13)// x6
	{
	    return ((recScope[7] / 30) * compe) + dist;
    } 
	else if ((int) Fov == 20)// x4
	{
		return ((recScope[6] / 30) * compe) + dist;
	}
	else if ((int) Fov == 26) //x3
	{
		return ((recScope[5] / 30) * compe) + dist;
	} 
	else if ((int) Fov == 44) //x2
	{
        return ((recScope[4] / 30) * compe) + dist;
	} 
	else if ((int) Fov == 55) // redot & hollo
    {
		return ((recScope[3] / 30) * compe) + dist;
	}
	else if ((int) Fov == 70) // no  scope
    {
		return ((recScope[2] / 30) * compe) + dist;
	}
	else if ((int) Fov == 80) // tpp no open scope
    {
		return ((recScope[1] / 30) * compe) + dist;
	}
	else if ((int) Fov == 90) // fpp no open scope
    {
		return ((recScope[0] / 30) * compe) + dist;
	}
}


#endif //XT_ESP_SUPPORT_H

