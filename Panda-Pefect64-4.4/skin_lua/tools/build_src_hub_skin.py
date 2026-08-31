#!/usr/bin/env python3
"""Build SRC HUB skin_mod_bgmi.lua from git base + user timer/weapon handlers."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "tools" / "git_skin.lua"
OUT_LUA = ROOT / "app" / "src" / "main" / "assets" / "skin_mod_bgmi.lua"
OUT_CPP = ROOT / "app" / "src" / "main" / "jni" / "skin.cpp"

TAIL_REPLACE_START = "-- ============================================================\n-- [NEW] ProcessOneWeapon"

USER_TAIL = r'''-- ============================================================
-- [NEW] ProcessOneWeapon
-- Ek weapon ke liye:
--   1. Skin apply karo (config se)
--   2. Agar skin lagi hai (SkinAttachmentMap mein entry hai)
--      to FORCE karo ki attachment bhi sahi hai
--   3. Attachment wrong hai to fix karo — cache ki parwah nahi
-- ============================================================
local function ProcessOneWeapon(weapon)
    if not weapon or not slua.isValid(weapon) then return end

    local weaponid = weapon:GetItemDefineID().TypeSpecificID
    if not weaponid or weaponid == 0 then return end

    _G.apply_weapon_skin(weapon, weaponid)

    local skinId = slua.IndexReference(weapon.synData:Get(7), "defineID").TypeSpecificID
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

            if AttachIdx == 0 then
                correctId, _ = _G.get_muzzleid(currentId, skinId)
            elseif AttachIdx == 1 then
                correctId, _ = _G.get_forgripid(currentId, skinId)
            elseif AttachIdx == 2 then
                correctId, _ = _G.get_magazinesid(currentId, skinId)
            elseif AttachIdx == 3 then
                correctId, _ = _G.get_stockid(currentId, skinId)
            elseif AttachIdx == 4 then
                correctId, _ = _G.get_scopeid(currentId, skinId)
            end

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
_G.ProcessOneWeapon = ProcessOneWeapon

local function GameAvatarHandlerweapons()
    pcall(function()
        if checkSkinReloadStamp then checkSkinReloadStamp() end
        local PlayerController = slua_GameFrontendHUD and slua_GameFrontendHUD:GetPlayerController()
        if not PlayerController then return end
        local uCharacter = PlayerController:GetPlayerCharacterSafety()
        if not uCharacter then return end

        local currweapon = uCharacter:GetCurrentWeapon()
        ProcessOneWeapon(currweapon)

        pcall(function()
            for slotIdx = 0, 1 do
                local w = uCharacter:GetWeaponByIndex(slotIdx)
                if w and slua.isValid(w) and w ~= currweapon then
                    ProcessOneWeapon(w)
                end
            end
        end)
    end)
end

local function GameAvatarHandlerkillcounter()
    pcall(_G.ForceUpdateKillCounterUI)
end

local function Lobby_Avatar_Handler()
    pcall(function()
        _G.ReadConfigFile(true)
        _G.CheckLobbyThemeChanges()
        _G.GameAvatarHandlerplayers()
        _G.HandlePetLogic()
    end)
end

local function Game_Avatar_Handler()
    pcall(_G.GameAvatarHandlerplayers)
end

local function startSkinTimerLoops()
    if _G.__SKIN_TIMERS_STARTED then return end
    if not _G.Mytimer_ticker then
        local ok, ticker = pcall(require, 'common.time_ticker')
        if ok and ticker then _G.Mytimer_ticker = ticker end
    end
    if not _G.Mytimer_ticker then return end

    _G.__SKIN_TIMERS_STARTED = true
    pcall(function()
        _G.Mytimer_ticker.AddTimerLoop(3,   Lobby_Avatar_Handler,           -1, 1)
        _G.Mytimer_ticker.AddTimerLoop(2,   Game_Avatar_Handler,            -1, 1)
        _G.Mytimer_ticker.AddTimerLoop(0.5, GameAvatarHandlerweapons,       -1, 1)
        _G.Mytimer_ticker.AddTimerLoop(0.5, GameAvatarHandlerkillcounter,   -1, 1)
        _G.Mytimer_ticker.AddTimerLoop(2.5, _G.GameAvatarHandlerDeadBox,    -1, 1)
        _G.Mytimer_ticker.AddTimerLoop(2,   _G.GameAvatarHandlervehicles,   -1, 1)
        _G.Mytimer_ticker.AddTimerLoop(2,   _G.FileWatcher,                 -1, 0.5)
        _G.Mytimer_ticker.AddTimerLoop(1.5, CheckGameEndLoop,               -1, 0.5)
        _G.Mytimer_ticker.AddTimerOnce(2, function()
            pcall(_G.TryShowWelcome)
            pcall(_G.ApplyLobbyTheme)
            pcall(_G.GameAvatarHandlerplayers)
            pcall(_G.HandlePetLogic)
        end)
    end)
end

function _G.ensureSkinTimers()
    pcall(_G.ReadConfigFile, true)
    if not _G.__SKIN_BOOTSTRAPPED then
        pcall(_G.loadKillCountFromFile)
        _G.isFileWatcherActive = true
        pcall(InitItemUpgradeSystem)
        pcall(installExtendedSkinHooks)
        _G.__SKIN_BOOTSTRAPPED = true
    end
    startSkinTimerLoops()
end

local TXtime_ticker = nil
pcall(function()
    TXtime_ticker = require('common.time_ticker')
end)
if TXtime_ticker then _G.Mytimer_ticker = TXtime_ticker end

_G.loadKillCountFromFile()
_G.isFileWatcherActive = true
_G.ReadConfigFile(true)
pcall(InitItemUpgradeSystem)
pcall(loadBuiltinAttachmentMaps)

_G.__SKIN_PATCHED = true
pcall(startSkinTimerLoops)
if not _G.__SKIN_TIMERS_STARTED then
    pcall(function()
        _G.Mytimer_ticker.AddTimerOnce(2, _G.ensureSkinTimers)
    end)
end
'''

USER_READ_CONFIG = r'''local function ReadConfigFile(force)
    local possiblePaths = {}
    if _G.__SKIN_CONFIG_BASE and _G.__SKIN_CONFIG_BASE ~= '' then
        table.insert(possiblePaths, _G.__SKIN_CONFIG_BASE .. 'config.ini')
        local base = _G.__SKIN_CONFIG_BASE:gsub('/$', '')
        table.insert(possiblePaths, base .. '/config.ini')
    end
    for _, base in ipairs(loadSkinDataBases and loadSkinDataBases() or {}) do
        table.insert(possiblePaths, base .. '/config.ini')
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
    end

    local configPath = nil
    for _, path in ipairs(possiblePaths) do
        local file = io.open(path, 'r')
        if file then
            file:close()
            configPath = path
            break
        end
    end

    if not configPath then return end

    pcall(LoadAttachmentFile, configPath)

    local file = io.open(configPath, 'r')
    if not file then return end
    local content = file:read('*all')
    file:close()

    if not force and content == _G.__lastConfigContent then return end
    _G.__lastConfigContent = content

    local newConfig = {}
    for line in content:gmatch('[^\r\n]+') do
        local key, value = line:match('(%w+)=(%d+)')
        if key and value then newConfig[key] = tonumber(value) end
    end

    _G.TargetLobbyThemeID = newConfig['LobbyTheme'] or 0
    _G.SuitSkin     = newConfig['SHIRT']     or 0
    _G.HatSkin      = newConfig['HAT']       or 0
    _G.FaceSkin     = newConfig['FACE']      or 0
    _G.MaskSkin     = newConfig['MASK']      or 0
    _G.GlovesSkin   = newConfig['GLOVES']    or 0
    _G.PantSkin     = newConfig['PANT']      or 0
    _G.ShoeSkin     = newConfig['SHOE']      or 0
    _G.ParachuteSkin= newConfig['PARACHUTE'] or 0
    _G.GliderSkin   = newConfig['GLIDER']    or 0
    _G.Backpack1Skin= newConfig['BACKPACK1'] or 0
    _G.Backpack2Skin= newConfig['BACKPACK2'] or 0
    _G.Backpack3Skin= newConfig['BACKPACK3'] or 0
    _G.Helmet1Skin  = newConfig['HELMET1']   or 0
    _G.Helmet2Skin  = newConfig['HELMET2']   or 0
    _G.Helmet3Skin  = newConfig['HELMET3']   or 0
    _G.Emote1Skin   = newConfig['EMOTE1']    or 0
    _G.Emote2Skin   = newConfig['EMOTE2']    or 0
    _G.Emote3Skin   = newConfig['EMOTE3']    or 0
    _G.PetSkin      = newConfig['PET_SKIN']  or 0
    _G.HallEffectSkin= newConfig['HALL_EFFECT'] or 0

    local function UpdateWep(configKey, weaponBaseID)
        local configVal = newConfig[configKey]
        if configVal and configVal ~= 0 then
            if not _G.WeaponSkinIndex then _G.WeaponSkinIndex = {} end
            _G.WeaponSkinIndex[weaponBaseID] = configVal
            lastConfig[configKey] = configVal
        end
    end

    UpdateWep('M416',     101004) UpdateWep('AKM',     101001) UpdateWep('SCAR',  101003) UpdateWep('M16A4', 101002)
    UpdateWep('GROZA',    101005) UpdateWep('AUG',     101006) UpdateWep('QBZ',   101007) UpdateWep('M762',  101008)
    UpdateWep('MK47',     101009) UpdateWep('G36C',    101010) UpdateWep('FAMAS', 101011)
    UpdateWep('Kar98',    103001) UpdateWep('M24',     103002) UpdateWep('AWM',   103003) UpdateWep('SKS',   103004)
    UpdateWep('VSS',      103005) UpdateWep('Mini14',  103006) UpdateWep('MK14',  103007) UpdateWep('SLR',   103009)
    UpdateWep('QBU',      103010) UpdateWep('MK12',    103011) UpdateWep('AMR',   103012) UpdateWep('Mosin', 103013)
    UpdateWep('UZI',      102001) UpdateWep('UMP',     102002) UpdateWep('Vector',102003) UpdateWep('Thompson',102004)
    UpdateWep('Bizon',    102005) UpdateWep('MP5K',    102007) UpdateWep('P90',   102009)
    UpdateWep('S12K',     104003) UpdateWep('DBS',     104004) UpdateWep('S1897', 104001) UpdateWep('S686',  104002)
    UpdateWep('M249',     105002) UpdateWep('DP28',    105001) UpdateWep('MG3',   105010)
    UpdateWep('Pan',      106001) UpdateWep('Machete', 106003) UpdateWep('Crowbar',106002) UpdateWep('Sickle',106004)

    for k, v in pairs(newConfig) do
        lastConfig[k] = v
    end

    pcall(loadSkinAttachmentMaps)
    pcall(bootstrapAttachmentSkins)
end
_G.ReadConfigFile = ReadConfigFile
'''


def main():
    lua = SRC.read_text(encoding="utf-8")

    # branding
    lua = lua.replace("Anubis", "SRC HUB").replace("SRCHUB", "SRCHUB")
    lua = lua.replace("Enjoy SRC HUB!", "Enjoy SRCHUB!")
    lua = lua.replace("SRC HUB sends their regards!", "SRCHUB sends their regards!")

    # match user WeaponBaseIDMap (no ACE32)
    lua = lua.replace("FAMAS=101011, ACE32=101102,", "FAMAS=101011,")

    # replace ReadConfigFile block
    rs = lua.index("local function ReadConfigFile(force)")
    re = lua.index("_G.ReadConfigFile = ReadConfigFile", rs) + len("_G.ReadConfigFile = ReadConfigFile")
    lua = lua[:rs] + USER_READ_CONFIG + lua[re:]

    # replace tail from ProcessOneWeapon marker
    ts = lua.index(TAIL_REPLACE_START)
    lua = lua[:ts] + USER_TAIL

    OUT_LUA.write_text(lua, encoding="utf-8")
    OUT_CPP.write_text(f'const char* skin_script = R"lua(\n{lua}\n)lua";\n', encoding="utf-8")
    print(f"wrote {OUT_LUA} ({lua.count(chr(10))+1} lines)")


if __name__ == "__main__":
    main()
