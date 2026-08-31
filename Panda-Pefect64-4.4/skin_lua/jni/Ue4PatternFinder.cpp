#include "Ue4PatternFinder.h"

#include <android/log.h>
#include <dlfcn.h>
#include <link.h>

#include <cstdint>
#include <cstdlib>
#include <cstring>
#include <sstream>
#include <string>
#include <unistd.h>
#include <vector>

namespace {

constexpr const char *kTag = "UE4_OFFSETS";

#define PLOGI(...) __android_log_print(ANDROID_LOG_INFO, kTag, __VA_ARGS__)
#define PLOGE(...) __android_log_print(ANDROID_LOG_ERROR, kTag, __VA_ARGS__)

template <typename T>
T ReadMem(uintptr_t address) {
    if (address < 0x1000000) {
        return T();
    }
    return *reinterpret_cast<T *>(address);
}

inline bool IsValidPtr1(uintptr_t ptr) {
    return ptr > 0x1000000000ULL && ptr < 0x8000000000ULL;
}

static uintptr_t PatternScan(const uint8_t *data, size_t size, const char *pattern) {
    std::vector<int> patternBytes;
    std::string patternStr(pattern);
    std::stringstream ss(patternStr);
    std::string item;

    while (ss >> item) {
        if (item == "?" || item == "??") {
            patternBytes.push_back(-1);
        } else {
            patternBytes.push_back(static_cast<int>(std::strtoul(item.c_str(), nullptr, 16)));
        }
    }

    if (patternBytes.empty()) {
        return 0;
    }

    const size_t patternSize = patternBytes.size();
    if (size < patternSize) {
        return 0;
    }

    for (size_t i = 0; i <= size - patternSize; i++) {
        bool found = true;
        for (size_t j = 0; j < patternSize; j++) {
            if (patternBytes[j] != -1 && data[i + j] != static_cast<uint8_t>(patternBytes[j])) {
                found = false;
                break;
            }
        }
        if (found) {
            return reinterpret_cast<uintptr_t>(data + i);
        }
    }
    return 0;
}

static bool ParseHexPattern(const char *hex, std::vector<uint8_t> &bytes, std::vector<bool> &mask) {
    bytes.clear();
    mask.clear();

    const char *cur = hex;
    while (*cur) {
        if (*cur == ' ') {
            cur++;
            continue;
        }

        if (*cur == '?') {
            bytes.push_back(0x00);
            mask.push_back(false);
            cur++;
            if (*cur == '?') {
                cur++;
            }
        } else {
            char byteStr[3] = {0, 0, 0};
            byteStr[0] = cur[0];
            byteStr[1] = cur[1];
            bytes.push_back(static_cast<uint8_t>(std::strtoul(byteStr, nullptr, 16)));
            mask.push_back(true);
            cur += 2;
        }
    }
    return !bytes.empty();
}

static uintptr_t PatternScanHex(uintptr_t base, size_t size, const char *hexPattern) {
    if (!IsValidPtr1(base) || hexPattern == nullptr) {
        return 0;
    }

    std::vector<uint8_t> pattern;
    std::vector<bool> mask;
    if (!ParseHexPattern(hexPattern, pattern, mask)) {
        return 0;
    }

    const size_t patLen = pattern.size();
    if (size < patLen) {
        return 0;
    }

    const uintptr_t end = base + size - patLen;
    for (uintptr_t addr = base; addr <= end; addr++) {
        bool match = true;
        for (size_t i = 0; i < patLen; i++) {
            if (mask[i] && *reinterpret_cast<uint8_t *>(addr + i) != pattern[i]) {
                match = false;
                break;
            }
        }
        if (match) {
            return addr;
        }
    }
    return 0;
}

static uintptr_t Find_GNames_BL(uintptr_t base, size_t size) {
    if (!IsValidPtr1(base) || size == 0) {
        return 0;
    }

    uintptr_t addr = PatternScanHex(base, size,
        "09 ?? ?? D3 29 45 7D 92 09 68 69 F8 ?? ?? ?? 12 28 59 68 F8 28 07 ?? B4");
    if (!addr) {
        return 0;
    }

    const uintptr_t blAddr = addr - 0x18;
    if (!IsValidPtr1(blAddr)) {
        return 0;
    }

    const uint32_t ins = ReadMem<uint32_t>(blAddr);
    if ((ins & 0xFC000000U) != 0x94000000U) {
        return 0;
    }

    const int32_t imm = (static_cast<int32_t>(ins << 6) >> 6) << 2;
    const uintptr_t target = blAddr + static_cast<uintptr_t>(imm);
    return IsValidPtr1(target) ? target : 0;
}

static uintptr_t Find_GUObjectArray(uintptr_t base, size_t size) {
    if (!IsValidPtr1(base) || size == 0) {
        return 0;
    }

    const char *pattern =
        "?? ?? ?? FF 00 ?? 00 00 00 ?? 00 00 00 ?? 00 ?? ?? 00 00 00 00 ?? 00 ?? ?? ?? ?? ?? ?? 00 00 ?? ?? ?? ?? ?? ?? ?? ?? 00 00 40 ??";
    const uintptr_t addr = PatternScan(reinterpret_cast<const uint8_t *>(base), size, pattern);
    if (!addr) {
        return 0;
    }

    const uintptr_t target = addr - 0xE8;
    return IsValidPtr1(target) ? target : 0;
}

static uintptr_t Find_GetActorArray(uintptr_t base, size_t size) {
    if (!IsValidPtr1(base) || size == 0) {
        return 0;
    }

    const char *pattern = "A0 00 00 36 68 42 44 B9";
    const uintptr_t addr = PatternScan(reinterpret_cast<const uint8_t *>(base), size, pattern);
    if (!addr) {
        return 0;
    }

    const uintptr_t target = addr - 0x2C;
    return IsValidPtr1(target) ? target : 0;
}

static uintptr_t Find_GnativeApp(uintptr_t base, size_t size) {
    if (!IsValidPtr1(base) || size == 0) {
        return 0;
    }

    const char *pattern = "00 00 00 00 00 00 80 3F 77 CC 2B 32 77 CC 2B 32";
    const uintptr_t addr = PatternScan(reinterpret_cast<const uint8_t *>(base), size, pattern);
    if (!addr) {
        return 0;
    }

    const uintptr_t target = addr - 0x1B0;
    return IsValidPtr1(target) ? target : 0;
}

struct Ue4Module {
    uintptr_t base = 0;
    size_t size = 0;
    std::string path;
};

static bool getLoadRange(const dl_phdr_info *info, uintptr_t *start, uintptr_t *end) {
    if (info == nullptr || info->dlpi_phdr == nullptr || info->dlpi_phnum <= 0) {
        return false;
    }

    uintptr_t lo = UINTPTR_MAX;
    uintptr_t hi = 0;
    for (int i = 0; i < info->dlpi_phnum; ++i) {
        const ElfW(Phdr) *seg = &info->dlpi_phdr[i];
        if (seg->p_type != PT_LOAD) {
            continue;
        }
        const uintptr_t segStart = static_cast<uintptr_t>(info->dlpi_addr + seg->p_vaddr);
        const uintptr_t segEnd = segStart + static_cast<uintptr_t>(seg->p_memsz);
        if (segStart < lo) {
            lo = segStart;
        }
        if (segEnd > hi) {
            hi = segEnd;
        }
    }
    if (hi <= lo) {
        return false;
    }
    *start = lo;
    *end = hi;
    return true;
}

static int ue4PhdrCallback(dl_phdr_info *info, size_t, void *data) {
    auto *out = static_cast<Ue4Module *>(data);
    if (info->dlpi_name == nullptr || info->dlpi_name[0] == '\0') {
        return 0;
    }
    if (strstr(info->dlpi_name, "libUE4.so") == nullptr) {
        return 0;
    }

    uintptr_t start = 0;
    uintptr_t end = 0;
    if (!getLoadRange(info, &start, &end)) {
        return 0;
    }

    out->base = static_cast<uintptr_t>(info->dlpi_addr);
    out->size = static_cast<size_t>(end - start);
    out->path = info->dlpi_name;
    return 1;
}

static bool findUe4Module(Ue4Module *out) {
    Ue4Module mod{};
    dl_iterate_phdr(ue4PhdrCallback, &mod);
    if (mod.base == 0 || mod.size == 0) {
        return false;
    }
    *out = mod;
    return true;
}

static void logSymbol(const char *name, uintptr_t base, uintptr_t addr) {
    if (addr == 0) {
        PLOGE("%s NOT FOUND", name);
        return;
    }
    const uintptr_t offset = (addr >= base) ? (addr - base) : 0;
    PLOGE("%s addr=0x%llx offset=0x%llx", name,
          static_cast<unsigned long long>(addr),
          static_cast<unsigned long long>(offset));
}

} // namespace

void ue4_pattern_finder_scan_and_log() {
    Ue4Module ue4{};
    if (!findUe4Module(&ue4)) {
        PLOGE("libUE4.so not loaded");
        return;
    }

    PLOGE("=== UE4 pattern scan start pid=%d ===", getpid());
    PLOGE("base=0x%llx size=0x%zx path=%s",
          static_cast<unsigned long long>(ue4.base),
          ue4.size,
          ue4.path.c_str());

    const uintptr_t gnames = Find_GNames_BL(ue4.base, ue4.size);
    const uintptr_t guobject = Find_GUObjectArray(ue4.base, ue4.size);
    const uintptr_t actorArray = Find_GetActorArray(ue4.base, ue4.size);
    const uintptr_t gnative = Find_GnativeApp(ue4.base, ue4.size);

    logSymbol("GNames_BL", ue4.base, gnames);
    logSymbol("GUObjectArray", ue4.base, guobject);
    logSymbol("GetActorArray", ue4.base, actorArray);
    logSymbol("GNativeApp", ue4.base, gnative);
    PLOGE("=== UE4 pattern scan end ===");
}
