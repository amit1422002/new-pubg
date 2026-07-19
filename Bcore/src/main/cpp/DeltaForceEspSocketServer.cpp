#include "DeltaForceEspSocketServer.h"

#include "esp_protocol/delta_esp_protocol.h"
#include "esp_protocol/socket_framing.h"

#include <android/log.h>
#include <atomic>
#include <algorithm>
#include <cerrno>
#include <cstring>
#include <mutex>

namespace {

constexpr const char *kTag = "DeltaForceEsp";

int g_listenFd = 0;
int g_clientFd = 0;
std::mutex g_sockMutex;
std::atomic<int> g_screenW{0};
std::atomic<int> g_screenH{0};
std::atomic<int> g_smallCrosshair{0};
DeltaEspAimbotConfig g_aimbotCfg{};
std::mutex g_aimbotMutex;
DeltaEspBulletTrackConfig g_bulletTrackCfg{};
std::mutex g_bulletTrackMutex;
std::atomic<int> g_lastCount{0};
std::atomic<int> g_fetchCalls{0};

void CloseClient() {
    if (g_clientFd > 0) {
        close(g_clientFd);
        g_clientFd = 0;
    }
}

void CloseAll() {
    CloseClient();
    if (g_listenFd > 0) {
        shutdown(g_listenFd, SHUT_RDWR);
        close(g_listenFd);
        g_listenFd = 0;
    }
}

bool CreateListenSocket() {
    CloseAll();
    g_listenFd = socket(AF_UNIX, SOCK_STREAM, 0);
    if (g_listenFd < 0) {
        __android_log_print(ANDROID_LOG_ERROR, kTag, "sock server socket failed errno=%d", errno);
        return false;
    }
    char nameBuf[108]{};
    sockaddr_un addr{};
    esp_socket::initAbstractAddrNamed(addr, nameBuf, sizeof(nameBuf), DELTA_ESP_SOCKET_NAME);
    const socklen_t addrLen = esp_socket::abstractUnixAddrLen(addr);
    if (bind(g_listenFd, reinterpret_cast<sockaddr *>(&addr), addrLen) < 0) {
        __android_log_print(ANDROID_LOG_ERROR, kTag, "sock server bind failed errno=%d name=%s",
                            errno, DELTA_ESP_SOCKET_NAME);
        CloseAll();
        return false;
    }
    if (listen(g_listenFd, 4) < 0) {
        __android_log_print(ANDROID_LOG_ERROR, kTag, "sock server listen failed errno=%d", errno);
        CloseAll();
        return false;
    }
    __android_log_print(ANDROID_LOG_INFO, kTag, "sock server listening fd=%d name=%s",
                        g_listenFd, DELTA_ESP_SOCKET_NAME);
    return true;
}

bool AcceptClient(int timeoutMs) {
    if (g_clientFd > 0) {
        return true;
    }
    if (g_listenFd <= 0) {
        return false;
    }
    pollfd pfd{};
    pfd.fd = g_listenFd;
    pfd.events = POLLIN;
    const int pr = poll(&pfd, 1, timeoutMs < 0 ? -1 : timeoutMs);
    if (pr <= 0) {
        return false;
    }
    g_clientFd = accept(g_listenFd, nullptr, nullptr);
    if (g_clientFd > 0) {
        __android_log_print(ANDROID_LOG_INFO, kTag, "sock server client fd=%d", g_clientFd);
    }
    return g_clientFd > 0;
}

} // namespace

bool DeltaForceEspServerEnsure() {
    std::lock_guard<std::mutex> lock(g_sockMutex);
    if (g_listenFd > 0) {
        return true;
    }
    return CreateListenSocket();
}

void DeltaForceEspServerSetScreen(int width, int height) {
    if (width > 0 && height > 0) {
        if (width < height) {
            std::swap(width, height);
        }
        g_screenW.store(width);
        g_screenH.store(height);
    }
}

void DeltaForceEspServerSetSmallCrosshair(int enabled) {
    g_smallCrosshair.store(enabled != 0 ? 1 : 0);
}

void DeltaForceEspServerSetAimbotConfig(const DeltaEspAimbotConfig *cfg) {
    std::lock_guard<std::mutex> lock(g_aimbotMutex);
    if (cfg != nullptr) {
        g_aimbotCfg = *cfg;
    } else {
        g_aimbotCfg = DeltaEspAimbotConfig{};
    }
}

void DeltaForceEspServerSetBulletTrackConfig(const DeltaEspBulletTrackConfig *cfg) {
    std::lock_guard<std::mutex> lock(g_bulletTrackMutex);
    if (cfg != nullptr) {
        g_bulletTrackCfg = *cfg;
    } else {
        g_bulletTrackCfg = DeltaEspBulletTrackConfig{};
    }
}

bool DeltaForceEspServerFetch(int screenW, int screenH, DeltaEspEntry *out, int maxOut, int *outCount) {
    if (outCount != nullptr) {
        *outCount = 0;
    }
    if (out == nullptr || maxOut <= 0) {
        return false;
    }
    if (screenW <= 0) {
        screenW = g_screenW.load();
    }
    if (screenH <= 0) {
        screenH = g_screenH.load();
    }
    if (screenW <= 0 || screenH <= 0) {
        screenW = 2400;
        screenH = 1080;
    }

    std::lock_guard<std::mutex> lock(g_sockMutex);
    if (g_listenFd <= 0 && !CreateListenSocket()) {
        return false;
    }
    if (g_clientFd <= 0) {
        AcceptClient(50);
    }
    if (g_clientFd <= 0) {
        return false;
    }

    static std::atomic<int> sBootInits{0};
    DeltaEspRequest req{};
    if (sBootInits.load() < 2) {
        req.mode = DELTA_ESP_MODE_INIT;
        sBootInits.fetch_add(1);
    } else {
        req.mode = DELTA_ESP_MODE_DRAW;
    }
    req.screenWidth = screenW;
    req.screenHeight = screenH;
    req.smallCrosshair = g_smallCrosshair.load();
    {
        std::lock_guard<std::mutex> lock(g_aimbotMutex);
        req.aimbot = g_aimbotCfg;
    }
    {
        std::lock_guard<std::mutex> lock(g_bulletTrackMutex);
        req.bulletTrack = g_bulletTrackCfg;
    }

    if (!esp_socket::sendPacket(g_clientFd, &req, sizeof(req))) {
        CloseClient();
        return false;
    }

    DeltaEspResponse resp{};
    const int got = esp_socket::recvPacket(g_clientFd, &resp, sizeof(resp));
    if (got <= 0) {
        CloseClient();
        return false;
    }

    int count = resp.playerCount;
    if (count < 0) {
        count = 0;
    }
    if (count > maxOut) {
        count = maxOut;
    }
    if (count > DELTA_MAX_ESP_PLAYERS) {
        count = DELTA_MAX_ESP_PLAYERS;
    }
    if (count > 0) {
        memcpy(out, resp.players, static_cast<size_t>(count) * sizeof(DeltaEspEntry));
    }
    if (outCount != nullptr) {
        *outCount = count;
    }
    g_lastCount.store(count);
    const int calls = ++g_fetchCalls;
    if (calls == 1 || (calls % 300) == 0) {
        __android_log_print(ANDROID_LOG_INFO, kTag,
                            "sock server fetch count=%d screen=%dx%d client=%d",
                            count, screenW, screenH, g_clientFd);
    }
    return true;
}

int DeltaForceEspServerLastCount() {
    return g_lastCount.load();
}

bool DeltaForceEspServerListening() {
    return g_listenFd > 0;
}

bool DeltaForceEspServerClientConnected() {
    return g_clientFd > 0;
}
