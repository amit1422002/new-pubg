#!/usr/bin/env python3
"""Restore full in-game skin lua from git HEAD skin.cpp -> assets + skin.cpp"""
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT_LUA = ROOT / "tools" / "full_skin_mod.lua"
ASSETS = ROOT / "app" / "src" / "main" / "assets" / "skin_mod_bgmi.lua"
CPP = ROOT / "app" / "src" / "main" / "jni" / "skin.cpp"

HOOK_TAIL = """
-- hook integration (Blaze guest login)
_G.__SKIN_PATCHED = true
_G.__SKIN_TIMERS_STARTED = true
function _G.ensureSkinTimers()
    pcall(_G.ReadConfigFile)
    if _G.Mytimer_ticker then
        pcall(_G.GameAvatarHandlerplayers)
        pcall(_G.Lobby_Avatar_Handler)
    end
end
"""

READ_CONFIG_PATCH = """local function ReadConfigFile()
    local possiblePaths = {}
    if _G.__SKIN_CONFIG_BASE and _G.__SKIN_CONFIG_BASE ~= "" then
        local base = _G.__SKIN_CONFIG_BASE:gsub("/$", "")
        table.insert(possiblePaths, 1, base .. "/config.ini")
        table.insert(possiblePaths, _G.__SKIN_CONFIG_BASE .. "config.ini")
    end
    local defaults = {
        '/storage/emulated/0/Android/data/com.pubg.imobile/files/config.ini',
        '/storage/emulated/0/Android/data/com.pubg.krmobile/files/config.ini',
        '/storage/emulated/0/Android/data/com.vng.pubgmobile/files/config.ini',
        '/storage/emulated/0/Android/data/com.rekoo.pubgm/files/config.ini',
        '/data/user/0/com.pubg.imobile/files/config.ini',
        '/storage/emulated/0/config.ini'
    }
    for _, p in ipairs(defaults) do
        table.insert(possiblePaths, p)
    end"""

def main():
    git_cpp = subprocess.check_output(
        ["git", "-C", str(ROOT), "show", "HEAD:app/src/main/jni/skin.cpp"],
        text=True,
        errors="replace",
    )
    marker_start = 'R"lua('
    marker_end = ')lua"'
    start = git_cpp.index(marker_start) + len(marker_start)
    end = git_cpp.rindex(marker_end)
    lua = git_cpp[start:end]

    old_read = """local function ReadConfigFile()
    local possiblePaths = {
        '/storage/emulated/0/Android/data/com.pubg.imobile/files/config.ini',
        '/storage/emulated/0/Android/data/com.pubg.krmobile/files/config.ini',
        '/storage/emulated/0/Android/data/com.vng.pubgmobile/files/config.ini',
        '/storage/emulated/0/Android/data/com.rekoo.pubgm/files/config.ini',
        '/storage/emulated/0/config.ini'
    }"""
    if old_read not in lua:
        raise SystemExit("ReadConfigFile block not found - manual merge needed")
    lua = lua.replace(old_read, READ_CONFIG_PATCH, 1)

    # skin_reload.stamp watcher inside FileWatcher area - add after ReadConfigFile in timer
    stamp_watcher = """
local function checkSkinReloadStamp()
    local bases = {}
    if _G.__SKIN_CONFIG_BASE and _G.__SKIN_CONFIG_BASE ~= "" then
        table.insert(bases, _G.__SKIN_CONFIG_BASE:gsub("/$", ""))
    end
    table.insert(bases, "/storage/emulated/0/Android/data/com.pubg.imobile/files")
    table.insert(bases, "/data/user/0/com.pubg.imobile/files")
    for _, base in ipairs(bases) do
        local f = io.open(base .. "/skin_reload.stamp", "r")
        if f then
            local stamp = (f:read("*a") or ""):gsub("%s+", "")
            f:close()
            if stamp ~= "" and stamp ~= (_G.__skinReloadStamp or "") then
                _G.__skinReloadStamp = stamp
                pcall(_G.ReadConfigFile)
            end
            return
        end
    end
end
"""
    if "checkSkinReloadStamp" not in lua:
        lua = lua.replace("local function Lobby_Avatar_Handler()", stamp_watcher + "\nlocal function Lobby_Avatar_Handler()", 1)
        lua = lua.replace(
            "_G.Mytimer_ticker.AddTimerLoop(3,   Lobby_Avatar_Handler,           -1, 1)",
            "_G.Mytimer_ticker.AddTimerLoop(3,   Lobby_Avatar_Handler,           -1, 1)\n        _G.Mytimer_ticker.AddTimerLoop(2,   checkSkinReloadStamp,           -1, 1)",
            1,
        )

    if "_G.__SKIN_PATCHED" not in lua:
        lua = lua.rstrip() + "\n" + HOOK_TAIL

    OUT_LUA.write_text(lua, encoding="utf-8")
    ASSETS.write_text(lua, encoding="utf-8")
    CPP.write_text(f'const char* skin_script = R"lua(\n{lua}\n)lua";\n', encoding="utf-8")
    print(f"restored {len(lua)} chars, {lua.count(chr(10))+1} lines")

if __name__ == "__main__":
    main()
