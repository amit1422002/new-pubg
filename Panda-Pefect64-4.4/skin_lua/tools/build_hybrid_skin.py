#!/usr/bin/env python3
"""Ultimate Engine (lobby) + SRC HUB (in-game) -> skin_mod_bgmi.lua + skin.cpp"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ULTIMATE = ROOT / "tools" / "ultimate_skin_mod.lua"
SRC = ROOT / "tools" / "git_skin.lua"
VEHICLE = ROOT / "tools" / "src_hub_vehicle.lua"
OUT_LUA = ROOT / "app" / "src" / "main" / "assets" / "skin_mod_bgmi.lua"
OUT_CPP = ROOT / "app" / "src" / "main" / "jni" / "skin.cpp"

SRC_AUTO_INIT = """pcall(function()
    local ok, ticker = pcall(require, 'common.time_ticker')
    if ok and ticker then _G.Mytimer_ticker = ticker end
end)

_G.__SKIN_PATCHED = true
pcall(loadBuiltinAttachmentMaps)

if _G.Mytimer_ticker then
    pcall(function()
        _G.Mytimer_ticker.AddTimerOnce(2, ensureSkinTimers)
    end)
else
    pcall(ensureSkinTimers)
end"""

SRC_HUB_TAIL = r'''
-- ========== SRC HUB in-game bootstrap (match/training — lobby = Ultimate) ==========
local function isNonLobbyWorldCharacter(ch)
    if not ch or not slua.isValid(ch) then return false end
    local isLobbyActor = false
    pcall(function()
        if ch.IsLobbyActor and ch:IsLobbyActor() then isLobbyActor = true end
    end)
    return not isLobbyActor
end

local function srcHubInMatch()
    if _G.isInPlayableGame and _G.isInPlayableGame() then return true end
    if _G.__PLAYABLE_GAME_ACTIVE then return true end
    local okFight, fighting = pcall(function()
        return GameStatus and GameStatus.IsInFightingStatus and GameStatus.IsInFightingStatus()
    end)
    if okFight and fighting then return true end
    local ok, gs = pcall(function()
        return slua_GameFrontendHUD and slua_GameFrontendHUD:GetGameState()
    end)
    if ok and gs then
        local state = gs.GameModeState
        if not state and gs.GetGameModeState then state = gs:GetGameModeState() end
        if state and state ~= 'Lobby' and state ~= 'None' then return true end
    end
    if _G.getMatchPlayerCharacter then
        local ch = _G.getMatchPlayerCharacter()
        if isNonLobbyWorldCharacter(ch) then return true end
    end
    local okLobby, inLobby = pcall(function()
        return GameStatus and GameStatus.IsInLobbyOrMainCity and GameStatus.IsInLobbyOrMainCity()
    end)
    if okLobby and inLobby then return false end
    return false
end
_G.srcHubInMatch = srcHubInMatch

local function scheduleSrcHubRetry()
    if _G.__srcHubRetryTimer then return end
    local tries = 0
    local function retry()
        tries = tries + 1
        if _G.__SRC_HUB_STARTED and _G.__matchOutfitOk then
            _G.__srcHubRetryTimer = nil
            return
        end
        if srcHubInMatch() and _G.bootstrapSrcHubIngame then
            _G.bootstrapSrcHubIngame()
            if _G.__matchOutfitOk then
                _G.__srcHubRetryTimer = nil
                return
            end
        end
        if tries >= 40 then
            _G.__srcHubRetryTimer = nil
            skinProbeLog("SRC HUB retry gave up after " .. tries)
            return
        end
        if _G.Mytimer_ticker and _G.Mytimer_ticker.AddTimer then
            _G.Mytimer_ticker.AddTimer(2, retry)
        end
    end
    pcall(function()
        local ok, ticker = pcall(require, 'common.time_ticker')
        if ok and ticker then _G.Mytimer_ticker = ticker end
    end)
    if _G.Mytimer_ticker then
        _G.__srcHubRetryTimer = true
        _G.Mytimer_ticker.AddTimer(1, retry)
    end
end
_G.scheduleSrcHubRetry = scheduleSrcHubRetry

function _G.bootstrapSrcHubIngame()
    if not srcHubInMatch() then
        skinProbeLog("SRC HUB in-game: skip (lobby/loading) — will retry")
        scheduleSrcHubRetry()
        return
    end
    local firstBoot = not _G.__SRC_HUB_STARTED
    if firstBoot then
        skinProbeLog("SRC HUB in-game bootstrap")
        pcall(function()
            local ok, ticker = pcall(require, 'common.time_ticker')
            if ok and ticker then _G.Mytimer_ticker = ticker end
        end)
        if not _G.Mytimer_ticker then
            scheduleSrcHubRetry()
            return
        end
        pcall(_G.loadKillCountFromFile)
        _G.isFileWatcherActive = true
        pcall(InitItemUpgradeSystem)
        pcall(loadBuiltinAttachmentMaps)
        pcall(installExtendedSkinHooks)
        pcall(_G.hookVehicleEnterEvent)
        _G.__SRC_HUB_STARTED = true
        pcall(function()
            if _G.startSrcOutfitTimers then _G.startSrcOutfitTimers() end
        end)
        notify("SRC HUB in-game active")
    end
    _G.__outfitProbeDone = nil
    _G.__outfitZeroLogged = nil
    _G.__matchOutfitProbeDone = nil
    _G.__matchOutfitApplyLogged = nil
    _G.__matchCharMissLogged = nil
    _G.__avatarCompMissLogged = nil
    pcall(_G.ReadConfigFile, true)
    local res = tonumber(_G.AddOutfitLastLobbyOutfitRes) or tonumber(_G.__ACTIVE_OUTFIT_RES) or 0
    if res > 0 and _G.getOutfitInsId then
        local ins = tonumber(_G.getOutfitInsId(res)) or 0
        if ins > 0 then _G.AddOutfitLastLobbyOutfitIns = ins end
    end
    pcall(_G.GameAvatarHandlerplayers)
    pcall(function() _G.applyMatchOutfitNow(true) end)
    pcall(function()
        if _G.applyUltimateMatchOutfit then _G.applyUltimateMatchOutfit() end
    end)
    if not _G.__matchOutfitOk then
        scheduleSrcHubRetry()
    end
end

function _G.srcHubEnsureTimers()
    if _G.__SRC_HUB_STARTED then return end
    if not srcHubInMatch() then return end
    pcall(_G.ReadConfigFile, true)
    _G.bootstrapSrcHubIngame()
end
'''

ULTIMATE_BOOTSTRAP_PATCH = """    startMatchWatcher(char)
    pcall(function()
        if _G.bootstrapSrcHubIngame then _G.bootstrapSrcHubIngame() end
    end)
    return true"""

ULTIMATE_ENTER_GAME_PATCH = """            EventSystem:registEvent(EVENTTYPE_LOBBY, EVENTID_ENTER_GAME_BEGIN, function()
                syncWeaponCacheFromLobby()
                stopMatchWatcher()
                _bootstrapNotified = false
                later(3.0, function()
                    pcall(function()
                        if _G.bootstrapSrcHubIngame then _G.bootstrapSrcHubIngame() end
                        local char = getLocalChar()
                        if char then bootstrapMatch(char) end
                    end)
                end)
            end)"""

ULTIMATE_ENTER_GAME_OLD = """            EventSystem:registEvent(EVENTTYPE_LOBBY, EVENTID_ENTER_GAME_BEGIN, function()
                syncWeaponCacheFromLobby()
                stopMatchWatcher()
                _bootstrapNotified = false
            end)"""

ULTIMATE_ENSURE_PATCH = """function _G.ensureSkinTimers()
    if _G.__SKIN_BOOT_DONE then
        pcall(function()
            if isInRealMatch and isInRealMatch() and _G.bootstrapSrcHubIngame then
                _G.bootstrapSrcHubIngame()
            end
        end)
        return
    end"""


FAST_WEAPON_HANDLER = r'''
local _fastWeaponPollAt = 0
local function GameAvatarHandlerweapons()
    local now = os.clock()
    if now - _fastWeaponPollAt < 2.5 then return end
    _fastWeaponPollAt = now
    pcall(function()
        if checkSkinReloadStamp then _fastWeaponPollAt = 0 end
        local PlayerController = slua_GameFrontendHUD and slua_GameFrontendHUD:GetPlayerController()
        if not PlayerController then return end
        local uCharacter = PlayerController:GetPlayerCharacterSafety()
        if not uCharacter then return end
        local currweapon = uCharacter:GetCurrentWeapon()
        if _G.ProcessOneWeapon and currweapon and slua.isValid(currweapon) then
            _G.ProcessOneWeapon(currweapon, false)
        end
    end)
end
'''

FAST_PROCESS_ONE = r'''
function _G.ProcessOneWeapon(weapon, force)
    if not weapon or not slua.isValid(weapon) then return end
    local weaponid = weapon:GetItemDefineID().TypeSpecificID
    if not weaponid or weaponid == 0 then return end
    local baseId = weaponid
    if _G.resolveBaseWeaponId then
        local ok, bid = pcall(_G.resolveBaseWeaponId, weaponid)
        if ok and bid then baseId = bid end
    end
    local targetSkinID = _G.WeaponSkinIndex and _G.WeaponSkinIndex[baseId]
    if not force and targetSkinID and targetSkinID > 0 then
        local synData = weapon.synData
        local slot7 = synData and synData:Get(7)
        if slot7 then
            local currentSkin = slua.IndexReference(slot7, "defineID").TypeSpecificID
            if currentSkin == targetSkinID then return end
        end
    end
    if _G.apply_weapon_skin then _G.apply_weapon_skin(weapon, baseId) end
    local synData = weapon.synData
    if not synData then return end
    local slot7 = synData:Get(7)
    if not slot7 then return end
    local skinId = slua.IndexReference(slot7, "defineID").TypeSpecificID
    if not skinId or skinId == 0 then return end
    local skinHasAttachments = _G.SkinAttachmentMap and _G.SkinAttachmentMap[skinId]
    if not skinHasAttachments then return end
    local array = weapon.synData
    local needsMeshRefresh = false
    for AttachIdx = 0, 4 do
        local Data = array:Get(AttachIdx)
        if not Data then break end
        local currentId = slua.IndexReference(Data, "defineID").TypeSpecificID
        if currentId and currentId >= 0 and currentId < 10000000 then
            local correctId = nil
            if AttachIdx == 0 then correctId, _ = _G.get_muzzleid(currentId, skinId)
            elseif AttachIdx == 1 then correctId, _ = _G.get_forgripid(currentId, skinId)
            elseif AttachIdx == 2 then correctId, _ = _G.get_magazinesid(currentId, skinId)
            elseif AttachIdx == 3 then correctId, _ = _G.get_stockid(currentId, skinId)
            elseif AttachIdx == 4 then correctId, _ = _G.get_scopeid(currentId, skinId) end
            if correctId and correctId ~= 0 and correctId ~= currentId then
                Data.defineID.TypeSpecificID = correctId
                array:Set(AttachIdx, Data)
                needsMeshRefresh = true
            end
        end
    end
    if needsMeshRefresh then
        pcall(function() weapon:DelayHandleAvatarMeshChanged() end)
    end
end
'''


def strip_src_conflicts(lua: str) -> str:
    import re
    lua = re.sub(
        r"-- =+\n-- \[FIXED\] get_skin_id.*?_G\.get_skin_id2 = get_skin_id\n",
        "",
        lua,
        count=1,
        flags=re.DOTALL,
    )
    lua = re.sub(
        r"local function skinProbeLog\(\.\.\.\).*?\nend\n_G\.skinProbeLog = skinProbeLog\n+",
        "",
        lua,
        count=1,
        flags=re.DOTALL,
    )
    marker = "-- ============================================================\n-- [NEW] ProcessOneWeapon"
    end_marker = "local function GameAvatarHandlerkillcounter()"
    i = lua.find(marker)
    j = lua.find(end_marker, i) if i >= 0 else -1
    if i >= 0 and j > i:
        lua = lua[:i] + FAST_PROCESS_ONE + "\n" + FAST_WEAPON_HANDLER + "\n" + lua[j:]
    return lua


def inject_vehicle_module(lua: str, vehicle: str) -> str:
    import re
    lua = re.sub(
        r"function _G\.Game_Vehicle_Avatar_Change\(uCharacter\).*?\nend\n\nfunction _G\.GameAvatarHandlervehicles\(\).*?\nend\n",
        "",
        lua,
        count=1,
        flags=re.DOTALL,
    )
    marker = "_G.CurrentEquipVehicleID = 0"
    if marker not in lua:
        raise SystemExit("CurrentEquipVehicleID marker missing in SRC")
    lua = lua.replace(marker, marker + "\n\n" + vehicle.strip(), 1)
    needle = "    for k, v in pairs(newConfig) do\n        lastConfig[k] = v\n    end"
    insert = """    pcall(function()
        if _G.UpdateVehicleSkinMappings then _G.UpdateVehicleSkinMappings(newConfig) end
    end)

    for k, v in pairs(newConfig) do
        lastConfig[k] = v
    end"""
    if needle in lua:
        lua = lua.replace(needle, insert, 1)
    return lua


def strip_src_auto_init(lua: str, vehicle: str) -> str:
    if SRC_AUTO_INIT in lua:
        lua = lua.replace(SRC_AUTO_INIT, "", 1)
    lua = lua.replace("_G.ensureSkinTimers = ensureSkinTimers", "-- ensureSkinTimers -> srcHubEnsureTimers via bootstrap", 1)
    lua = lua.replace("Anubis", "SRC HUB")
    lua = strip_src_conflicts(lua)
    lua = inject_vehicle_module(lua, vehicle)
    return lua.rstrip()


def patch_ultimate(ultimate: str) -> str:
    ultimate = ultimate.replace(ULTIMATE_ENTER_GAME_OLD, ULTIMATE_ENTER_GAME_PATCH, 1)
    ultimate = ultimate.replace(
        "    startMatchWatcher(char)\n    return true",
        ULTIMATE_BOOTSTRAP_PATCH,
        1,
    )
    # extend ensureSkinTimers: after successful inject also prep in-game
    old = """    if injectAll(entity) then
        _G.__SKIN_BOOT_DONE = true
        onInjectReady()
    end
end"""
    new = """    if injectAll(entity) then
        _G.__SKIN_BOOT_DONE = true
        onInjectReady()
        pcall(function()
            if isInRealMatch and isInRealMatch() and _G.bootstrapSrcHubIngame then
                _G.bootstrapSrcHubIngame()
            end
        end)
    end
end"""
    ultimate = ultimate.replace(old, new, 1)
    return ultimate


HYBRID_START = """
(function() -- SRC HUB scope (Lua 200 local limit per function)
local skinProbeLog = _G.skinProbeLog or function(...) end
"""

HYBRID_END = """
end)()

pcall(function()
    later(2.5, function()
        local ok, err = pcall(start)
        if not ok then
            skinProbeLog("START ERROR: " .. tostring(err))
            log("START ERROR", err)
        end
    end)
end)
"""


def main():
    ultimate = ULTIMATE.read_text(encoding="utf-8")
    vehicle = VEHICLE.read_text(encoding="utf-8")
    src = strip_src_auto_init(SRC.read_text(encoding="utf-8"), vehicle)

    marker = '    skinProbeLog("UltimateEngine hooks installed")\nend'
    if marker not in ultimate:
        raise SystemExit("ultimate end marker not found")
    ultimate = patch_ultimate(ultimate)
    hybrid = ultimate.replace(
        marker,
        marker
        + "\n-- ==================== SRC HUB (in-game module) ====================\n"
        + HYBRID_START
        + src
        + "\n"
        + SRC_HUB_TAIL
        + HYBRID_END,
        1,
    )

    OUT_LUA.write_text(hybrid, encoding="utf-8")
    OUT_CPP.write_text(f'const char* skin_script = R"lua(\n{hybrid}\n)lua";\n', encoding="utf-8")
    print(f"hybrid {hybrid.count(chr(10))+1} lines -> {OUT_LUA}")


if __name__ == "__main__":
    main()
