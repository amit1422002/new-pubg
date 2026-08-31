#!/usr/bin/env python3
"""Assemble tools/ultimate_skin_mod.lua from extracted Ultimate Engine script."""
from pathlib import Path

SRC = Path(__file__).parent / "_extracted_ultimate.lua"
OUT = Path(__file__).parent / "ultimate_skin_mod.lua"

NOTIFY_BLOCK = """local function notify(...)
    log(...)
    pcall(function()
        if ShowNotice then ShowNotice(table.concat({...}, " ")) end
    end)
end
"""

CONFIG_BRIDGE = """
local WEAPON_CONFIG_KEYS = {
    M416=101004, AKM=101001, SCAR=101003, M16A4=101002, GROZA=101005, AUG=101006,
    QBZ=101007, M762=101008, MK47=101009, G36C=101010, FAMAS=101011, ACE32=101102,
    Kar98=103001, M24=103002, AWM=103003, SKS=103004, VSS=103005, Mini14=103006,
    MK14=103007, SLR=103009, QBU=103010, MK12=103011, AMR=103012, Mosin=103013,
    UZI=102001, UMP=102002, Vector=102003, Thompson=102004, Bizon=102005, MP5K=102007,
    P90=102009, S12K=104003, DBS=104004, S1897=104001, S686=104002,
    M249=105002, DP28=105001, MG3=105010, Pan=108,
}

local _lastIniContent = ""
local _lastConfigReadClock = 0

local function parseIniContent(content)
    local cfg = {}
    for line in content:gmatch("[^\\r\\n]+") do
        local key, value = line:match("^%s*([%w_]+)%s*=%s*(%d+)%s*")
        if key and value then cfg[key] = tonumber(value) end
    end
    return cfg
end

local function getConfigIniPaths()
    local paths = {}
    if _G.__SKIN_CONFIG_BASE and _G.__SKIN_CONFIG_BASE ~= "" then
        local base = _G.__SKIN_CONFIG_BASE:gsub("/$", "")
        table.insert(paths, base .. "/config.ini")
        table.insert(paths, _G.__SKIN_CONFIG_BASE .. "config.ini")
    end
    table.insert(paths, "/storage/emulated/0/Android/data/com.pubg.imobile/files/config.ini")
    table.insert(paths, "/data/user/0/com.pubg.imobile/files/config.ini")
    return paths
end

local function loadMatchConfigFromIni(force)
    local now = os.clock()
    if not force and _lastConfigReadClock > 0 and (now - _lastConfigReadClock) < 3 then
        return false
    end
    local configPath = nil
    for _, path in ipairs(getConfigIniPaths()) do
        local f = io.open(path, "r")
        if f then
            f:close()
            configPath = path
            break
        end
    end
    if not configPath then return false end
    local f = io.open(configPath, "r")
    if not f then return false end
    local content = f:read("*all")
    f:close()
    if not force and content == _lastIniContent then
        _lastConfigReadClock = now
        return false
    end
    _lastIniContent = content
    _lastConfigReadClock = now
    local parsed = parseIniContent(content)
    local shirt = tonumber(parsed.SHIRT) or 0
    if shirt > 0 then
        MATCH_CONFIG.outfitRes = shirt
    end
    MATCH_CONFIG.weaponSkins = MATCH_CONFIG.weaponSkins or {}
    for key, wid in pairs(WEAPON_CONFIG_KEYS) do
        local skinRes = tonumber(parsed[key])
        if skinRes and skinRes > 0 then
            MATCH_CONFIG.weaponSkins[wid] = skinRes
        end
    end
    if buildSkinMappings then pcall(buildSkinMappings) end
    _matchApplied = false
    return true
end

local function checkConfigReloadStamp()
    local paths = {}
    if _G.__SKIN_CONFIG_BASE and _G.__SKIN_CONFIG_BASE ~= "" then
        table.insert(paths, _G.__SKIN_CONFIG_BASE .. "skin_reload.stamp")
        local base = _G.__SKIN_CONFIG_BASE:gsub("/$", "")
        table.insert(paths, base .. "/skin_reload.stamp")
    end
    table.insert(paths, "/storage/emulated/0/Android/data/com.pubg.imobile/files/skin_reload.stamp")
    table.insert(paths, "/data/user/0/com.pubg.imobile/files/skin_reload.stamp")
    for _, path in ipairs(paths) do
        local f = io.open(path, "r")
        if f then
            local stamp = (f:read("*a") or ""):gsub("%s+", "")
            f:close()
            if stamp ~= "" and stamp ~= (_G.__skinReloadStamp or "") then
                _G.__skinReloadStamp = stamp
                loadMatchConfigFromIni(true)
                if reapplyLobbyEquipped and GameStatus and GameStatus.IsInLobbyOrMainCity
                    and GameStatus.IsInLobbyOrMainCity() then
                    later(0.5, reapplyLobbyEquipped)
                end
                return true
            end
        end
    end
    return false
end
"""

START_TAIL = """
    loadMatchConfigFromIni(true)
    _G.__SKIN_PATCHED = true
    _G.__SKIN_TIMERS_STARTED = true
    later(3.0, function()
        local function tick()
            checkConfigReloadStamp()
            loadMatchConfigFromIni(false)
            later(3.0, tick)
        end
        tick()
    end)
"""

OLD_INJECT_BLOCK = """    if injectAll() then
        refreshWardrobe()
        later(1.0, reapplyLobbyEquipped)
        return
    end
    local tries = 0
    local function retry()
        tries = tries + 1
        if injectAll() then
            refreshWardrobe()
            later(1.0, reapplyLobbyEquipped)
            return
        end
        if tries < 40 then later(1.5, retry) end
    end
    later(1.5, retry)
end"""

NEW_INJECT_BLOCK = """    if injectAll() then
        refreshWardrobe()
        later(1.0, reapplyLobbyEquipped)
    else
        local tries = 0
        local function retry()
            tries = tries + 1
            if injectAll() then
                refreshWardrobe()
                later(1.0, reapplyLobbyEquipped)
                return
            end
            if tries < 40 then later(1.5, retry) end
        end
        later(1.5, retry)
    end""" + START_TAIL + """
end"""


def main():
    lua = SRC.read_text(encoding="utf-8")

    # Add notify after log()
    lua = lua.replace(
        "local function log(...)\n    if DEBUG then print(\"[UltimateEngine]\", ...) end\nend\n",
        "local function log(...)\n    if DEBUG then print(\"[UltimateEngine]\", ...) end\nend\n\n" + NOTIFY_BLOCK,
        1,
    )

    # Insert config bridge after MATCH_CONFIG block
    marker = "local ITEMS = {"
    idx = lua.find(marker)
    if idx == -1:
        raise SystemExit("ITEMS marker not found")
    lua = lua[:idx] + CONFIG_BRIDGE + "\n" + lua[idx:]

    # Fix start() tail: no early return + config timer
    if OLD_INJECT_BLOCK not in lua:
        raise SystemExit("start() inject block not found")
    lua = lua.replace(OLD_INJECT_BLOCK, NEW_INJECT_BLOCK, 1)

    OUT.write_text(lua, encoding="utf-8")
    size = OUT.stat().st_size
    lines = lua.count("\n") + 1
    print(f"wrote {OUT}")
    print(f"lines={lines} bytes={size}")


if __name__ == "__main__":
    main()
