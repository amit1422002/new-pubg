local DEBUG = true
local function log(...)
    if DEBUG then print("[UltimateEngine]", ...) end
end

local function notify(...)
    log(...)
    local msg = table.concat({...}, " ")
    pcall(function()
        if ShowNotice then ShowNotice(msg) end
    end)
end

local function skinProbeLog(msg)
    log(msg)
    pcall(function()
        local path = _G.__SKIN_PROBE_PATH
        if not path or path == "" then return end
        local f = io.open(path, "a")
        if not f then return end
        f:write(os.date("%H:%M:%S ") .. tostring(msg) .. "\n")
        f:close()
    end)
end
_G.skinProbeLog = skinProbeLog

local MATCH_CONFIG = {
    outfitRes = 0,
    weaponSkins = {},
}


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
    for line in content:gmatch("[^\r\n]+") do
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
    if shirt > 0 and _G.isLobbyOutfitRes and not _G.isLobbyOutfitRes(shirt) then
        skinProbeLog("config skip invalid SHIRT=" .. tostring(shirt))
        shirt = 0
    end
    if shirt > 0 then
        if tonumber(_G.__configOutfitApplied) ~= shirt then
            _G.__configOutfitApplied = nil
        end
        MATCH_CONFIG.outfitRes = shirt
        _G.__ACTIVE_OUTFIT_RES = shirt
        _G.SuitSkin = shirt
        _G.AddOutfitLastLobbyOutfitRes = shirt
        if _G.__skinResToIns and _G.__skinResToIns[shirt] then
            _G.AddOutfitLastLobbyOutfitIns = _G.__skinResToIns[shirt]
        end
        pcall(function()
            if (tonumber(_G.AddOutfitLastLobbyOutfitIns) or 0) <= 0 and _G.getOutfitInsId then
                local ins = tonumber(_G.getOutfitInsId(shirt)) or 0
                if ins > 0 then _G.AddOutfitLastLobbyOutfitIns = ins end
            end
        end)
    end
    pcall(function()
        if _G.isLobbyOutfitRes and not _G.isLobbyOutfitRes(_G.AddOutfitLastLobbyOutfitRes) then
            if shirt > 0 then
                _G.AddOutfitLastLobbyOutfitRes = shirt
            end
            if not _G.isLobbyOutfitRes(_G.__ACTIVE_OUTFIT_RES) then
                _G.__ACTIVE_OUTFIT_RES = shirt > 0 and shirt or nil
            end
            if not _G.isLobbyOutfitRes(_G.SuitSkin) then
                _G.SuitSkin = shirt > 0 and shirt or 0
            end
        end
    end)
    local theme = tonumber(parsed.LobbyTheme) or 0
    if theme > 0 then
        _G.TargetLobbyThemeID = theme
        _G.LastAppliedThemeID = nil
    end
    MATCH_CONFIG.weaponSkins = MATCH_CONFIG.weaponSkins or {}
    for key, wid in pairs(WEAPON_CONFIG_KEYS) do
        local skinRes = tonumber(parsed[key])
        if skinRes and skinRes > 0 then
            MATCH_CONFIG.weaponSkins[wid] = skinRes
        end
    end
    local lobbyVehicle = tonumber(parsed.LOBBY_VEHICLE) or tonumber(parsed.VEHICLE) or tonumber(parsed.CAR) or 0
    if lobbyVehicle > 0 then
        _G.LobbyVehicleSkin = lobbyVehicle
    end
    pcall(function()
        if _G.UpdateVehicleSkinMappings then _G.UpdateVehicleSkinMappings(parsed) end
    end)
    if buildSkinMappings then pcall(buildSkinMappings) end
    _matchApplied = false
    return true
end

local WEAPON_ID_TO_CONFIG_KEY = {}
for configKey, weaponId in pairs(WEAPON_CONFIG_KEYS) do
    WEAPON_ID_TO_CONFIG_KEY[weaponId] = configKey
end

local function getWritableConfigPath()
    for _, path in ipairs(getConfigIniPaths()) do
        local f = io.open(path, "r")
        if f then f:close() return path end
    end
    return getConfigIniPaths()[1]
end

local function touchSkinReloadStamp(configPath)
    local bases = {}
    if configPath then
        local dir = configPath:match("^(.*)/[^/]+$")
        if dir then table.insert(bases, dir) end
    end
    if _G.__SKIN_CONFIG_BASE and _G.__SKIN_CONFIG_BASE ~= "" then
        table.insert(bases, _G.__SKIN_CONFIG_BASE:gsub("/$", ""))
    end
    table.insert(bases, "/storage/emulated/0/Android/data/com.pubg.imobile/files")
    table.insert(bases, "/data/user/0/com.pubg.imobile/files")
    local stamp = tostring(os.time()) .. "." .. tostring(math.random(1000, 9999))
    for _, base in ipairs(bases) do
        local f = io.open(base .. "/skin_reload.stamp", "w")
        if f then f:write(stamp) f:close() end
    end
end

function _G.patchConfigIni(key, value)
    key = tostring(key or "")
    value = tonumber(value) or 0
    if key == "" then return false end
    local path = getWritableConfigPath()
    if not path then return false end
    local content = ""
    local rf = io.open(path, "r")
    if rf then content = rf:read("*all") or "" rf:close() end
    local newLine = key .. "=" .. tostring(value)
    local found, lines = false, {}
    if content ~= "" then
        for line in content:gmatch("[^\r\n]+") do
            if line:match("^%s*" .. key .. "%s*=") then
                table.insert(lines, newLine) found = true
            else table.insert(lines, line) end
        end
    end
    if not found then table.insert(lines, newLine) end
    local wf = io.open(path, "w")
    if not wf then return false end
    wf:write(table.concat(lines, "\n") .. "\n") wf:close()
    _G.__skinReloadStamp = nil
    _lastIniContent = ""
    touchSkinReloadStamp(path)
    skinProbeLog("lobby->config " .. key .. "=" .. tostring(value))
    pcall(function() loadMatchConfigFromIni(true) end)
    pcall(function() if _G.ReadConfigFile then _G.ReadConfigFile(true) end end)
    return true
end

local function vehicleConfigKeyForSkin(resID)
    resID = tonumber(resID)
    if not resID or resID <= 0 or not _G.VehicleBaseIDMap then return nil end
    local skinStr, bestKey, bestLen = tostring(resID), nil, 0
    for configKey, basePrefix in pairs(_G.VehicleBaseIDMap) do
        local bp = tostring(basePrefix)
        if #bp > 0 and skinStr:sub(1, #bp) == bp and #bp > bestLen then
            bestKey, bestLen = configKey, #bp
        end
    end
    return bestKey
end

local function outfitConfigKeyForRes(resID)
    resID = tonumber(resID)
    if not resID or resID <= 0 then return nil end
    local vkey = vehicleConfigKeyForSkin(resID)
    if vkey then return vkey end
    if _G.isLobbyOutfitRes and _G.isLobbyOutfitRes(resID) then return "SHIRT" end
    local kind = _G.resolveClothKind and _G.resolveClothKind(resID) or nil
    if kind == "full_suit" or kind == "top" then return "SHIRT" end
    if kind == "pants" then return "PANT" end
    if kind == "shoes" then return "SHOE" end
    local s = tostring(resID)
    if s:sub(1, 4) == "1406" or s:sub(1, 4) == "1407" or s:sub(1, 4) == "1405"
        or s:sub(1, 4) == "1404" or s:sub(1, 5) == "12220" then
        return "SHIRT"
    end
    if s:sub(1, 5) == "14001" then return "PARACHUTE" end
    if s:sub(1, 6) == "150100" then
        if s:sub(1, 7) == "1501001" then return "BACKPACK1" end
        if s:sub(1, 7) == "1501002" then return "BACKPACK2" end
        return "BACKPACK3"
    end
    if s:sub(1, 6) == "150200" then
        if s:sub(1, 7) == "1502001" then return "HELMET1" end
        if s:sub(1, 7) == "1502002" then return "HELMET2" end
        return "HELMET3"
    end
    if s:sub(1, 3) == "500" then return "PET_SKIN" end
    return nil
end

function _G.syncLobbyItemToConfig(resID, weaponID)
    resID = tonumber(resID)
    if not resID or resID <= 0 then return end
    weaponID = tonumber(weaponID)
    if weaponID and weaponID > 0 then
        local wkey = WEAPON_ID_TO_CONFIG_KEY[weaponID]
        if wkey then _G.patchConfigIni(wkey, resID) return end
    end
    local key = outfitConfigKeyForRes(resID)
    if key then _G.patchConfigIni(key, resID) end
    pcall(_G.scheduleLobbyConfigFlush)
end

function _G.writeConfigBatch(updates)
    if not updates or not next(updates) then return false end
    local path = getWritableConfigPath()
    if not path then return false end
    local content = ""
    local rf = io.open(path, "r")
    if rf then content = rf:read("*all") or "" rf:close() end
    local pending, lines = {}, {}
    for k, v in pairs(updates) do
        if k and k ~= "" then pending[tostring(k)] = tonumber(v) or 0 end
    end
    if content ~= "" then
        for line in content:gmatch("[^\r\n]+") do
            local key = line:match("^%s*([%w_]+)%s*=")
            if key and pending[key] ~= nil then
                table.insert(lines, key .. "=" .. tostring(pending[key]))
                pending[key] = nil
            else
                table.insert(lines, line)
            end
        end
    end
    for key, value in pairs(pending) do
        table.insert(lines, key .. "=" .. tostring(value))
    end
    local wf = io.open(path, "w")
    if not wf then return false end
    wf:write(table.concat(lines, "\n") .. "\n") wf:close()
    _G.__skinReloadStamp = nil
    _lastIniContent = ""
    touchSkinReloadStamp(path)
    skinProbeLog("config saved (" .. tostring(#lines) .. " keys)")
    pcall(function() loadMatchConfigFromIni(true) end)
    pcall(function() if _G.ReadConfigFile then _G.ReadConfigFile(true) end end)
    return true
end

local function collectWornItemUpdates(updates)
    pcall(function()
        local AvatarData = require("client.logic.data.AvatarData")
        local wd = require("client.slua.logic.wardrobe.wardrobe_data")
        local function resFromIns(ins)
            ins = tonumber(ins)
            if not ins or ins <= 0 then return nil end
            if isInjectedIns(ins) then return tonumber(R.insToRes[ins]) end
            local d = wd:GetValidHallDepotItemDataByInsID(ins)
                or wd:GetHallDepotItemDataByInsID(ins)
            return d and tonumber(d.resID) or nil
        end
        for _, ins in pairs(AvatarData.GetRoleWear()) do
            local res = resFromIns(ins)
            local key = res and outfitConfigKeyForRes(res)
            if key then
                if key == "SHIRT" and updates.SHIRT and tonumber(updates.SHIRT) > 0 then
                else
                    updates[key] = res
                end
            elseif res and isVehicleSkinRes(res) then
                rememberLobbyVehicleSkin(res, ins)
            end
        end
    end)
end

local function isVehicleSkinRes(resID)
    resID = tonumber(resID)
    if not resID or resID <= 0 then return false end
    if vehicleConfigKeyForSkin(resID) then return true end
    local s = tostring(resID)
    return s:sub(1, 3) == "190" or s:sub(1, 3) == "196" or s:sub(1, 3) == "191" or s:sub(1, 3) == "195"
end

local function rememberLobbyVehicleSkin(resID, insID)
    resID = tonumber(resID)
    if not resID or resID <= 0 or not isVehicleSkinRes(resID) then return end
    if _G.__lastLobbyVehicleRes == resID and _G.LobbyVehicleSkin == resID then return end
    _G.__lastLobbyVehicleRes = resID
    _G.LobbyVehicleSkin = resID
    if _G.VehicleBaseIDMap then
        _G.VehicleSkinIndex = _G.VehicleSkinIndex or {}
        local key = vehicleConfigKeyForSkin(resID)
        if key then
            local base = _G.VehicleBaseIDMap[key]
            if base then
                _G.VehicleSkinIndex[base] = resID
                _G.VehicleSkinIndex[key] = resID
            end
        end
    end
    pcall(function() _G.syncLobbyItemToConfig(resID) end)
    pcall(function() _G.patchConfigIni("LOBBY_VEHICLE", resID) end)
    skinProbeLog("lobby vehicle " .. resID .. (insID and (" ins=" .. tostring(insID)) or ""))
end

local function resFromVehicleIns(insID)
    insID = tonumber(insID)
    if not insID or insID <= 0 then return nil, nil end
    if _G.__skinInsToRes and _G.__skinInsToRes[insID] then
        return tonumber(_G.__skinInsToRes[insID]), insID
    end
    local resID
    pcall(function()
        local wd = require("client.slua.logic.wardrobe.wardrobe_data")
        local d = wd:GetValidHallDepotItemDataByInsID(insID) or wd:GetHallDepotItemDataByInsID(insID)
        resID = d and tonumber(d.resID) or nil
    end)
    return resID, insID
end

local function syncVehicleCacheFromLobby()
    pcall(function()
        local fbd = require("client.slua.logic.wardrobe.fashionbag.fashionbag_data")
        local bag = fbd.GetCurrentFashionBag and fbd:GetCurrentFashionBag()
        if not bag then return end
        local lists = { bag.vehicle_skin_list, bag.vehicle_list, bag.garage_list }
        for _, lst in ipairs(lists) do
            if lst then
                for _, entry in pairs(lst) do
                    local insID = tonumber(entry and (entry.skin_id or entry.skinId or entry.instid)) or 0
                    if insID <= 0 and type(entry) == "number" then insID = entry end
                    local resID = resFromVehicleIns(insID)
                    if resID and isVehicleSkinRes(resID) then
                        rememberLobbyVehicleSkin(resID, insID)
                    end
                end
            end
        end
        for _, field in ipairs({ "vehicle_skin_id", "vehicle_skin", "vehicle_id", "garage_vehicle_id" }) do
            local insID = tonumber(bag[field]) or 0
            if insID > 0 then
                local resID = resFromVehicleIns(insID)
                if resID and isVehicleSkinRes(resID) then
                    rememberLobbyVehicleSkin(resID, insID)
                end
            end
        end
    end)
    pcall(function()
        if ENUM_AVATAR_SHOW_TYPE and ENUM_AVATAR_SHOW_TYPE.SHOW_POS_VEHICLE then
            local myUid = tonumber(DataMgr.roleData.uid)
            if not myUid then return end
            local BD = ModuleManager.GetModule(ModuleManager.DataModuleConfig.BasicDataAvatarWearInfo)
            local d = BD and BD:GetCacheData(myUid)
            local slot = d and d.pspace_wear_ext and d.pspace_wear_ext[ENUM_AVATAR_SHOW_TYPE.SHOW_POS_VEHICLE]
            local resID = slot and tonumber(slot[1]) or 0
            if resID > 0 and isVehicleSkinRes(resID) then
                rememberLobbyVehicleSkin(resID)
            end
        end
    end)
end

local function collectVehicleUpdates(updates)
    pcall(syncVehicleCacheFromLobby)
    local lobbyV = tonumber(_G.LobbyVehicleSkin) or 0
    if lobbyV > 0 then
        updates.LOBBY_VEHICLE = lobbyV
        local key = vehicleConfigKeyForSkin(lobbyV)
        if key then updates[key] = lobbyV end
    end
    if _G.VehicleSkinIndex and _G.VehicleBaseIDMap then
        for configKey, base in pairs(_G.VehicleBaseIDMap) do
            local skin = tonumber(_G.VehicleSkinIndex[base] or _G.VehicleSkinIndex[configKey]) or 0
            if skin > 0 then updates[configKey] = skin end
        end
    end
end

local function equipLobbyVehicleIns(insID)
    insID = tonumber(insID)
    if not insID or insID <= 0 then return false end
    local resID = select(1, resFromVehicleIns(insID))
    if not resID or resID <= 0 then return false end
    rememberLobbyVehicleSkin(resID, insID)
    local ok = false
    pcall(function()
        local HT = require("client.logic.lobby.hall_theme_utils")
        local fbd = require("client.slua.logic.wardrobe.fashionbag.fashionbag_data")
        local bagIdx = fbd.GetFashionBagUseIndex and fbd:GetFashionBagUseIndex() or 0
        if HT.proc_skin_list_chg then
            HT.proc_skin_list_chg("vehicle_skin", 0, insID, bagIdx, {})
            ok = true
        end
    end)
    if _G.__skinInsToRes and _G.__skinInsToRes[insID] then
        pcall(function() if injectArmory then injectArmory(resID, insID) end end)
    end
    pcall(function()
        if _G.applyLobbyGarageVehicleSkin then
            ok = _G.applyLobbyGarageVehicleSkin(resID) or ok
        end
    end)
    return ok
end

local function applyLobbyVehicleFromConfig()
    if GameStatus and GameStatus.IsInLobbyOrMainCity and not GameStatus.IsInLobbyOrMainCity() then
        return false
    end
    local skin = tonumber(_G.LobbyVehicleSkin) or 0
    if skin <= 0 then
        pcall(function()
            if _G.ReadConfigFile then _G.ReadConfigFile(true) end
        end)
        skin = tonumber(_G.LobbyVehicleSkin) or 0
    end
    if skin <= 0 then return false end
    if _G.__lastAppliedLobbyVehicle == skin then return true end
    local insID = (_G.__skinResToIns and _G.__skinResToIns[skin])
        or (_G.getOutfitInsId and _G.getOutfitInsId(skin)) or 0
    insID = tonumber(insID) or 0
    local ok = false
    if insID > 0 then
        ok = equipLobbyVehicleIns(insID) or ok
    end
    pcall(function()
        if _G.applyLobbyGarageVehicleSkin then
            ok = _G.applyLobbyGarageVehicleSkin(skin) or ok
        end
    end)
    if ok then
        _G.__lastAppliedLobbyVehicle = skin
        skinProbeLog("lobby bg car applied " .. skin)
    end
    return ok
end

local function hookLobbyVehiclePersist()
    pcall(function()
        local HT = require("client.logic.lobby.hall_theme_utils")
        if not HT or not HT.proc_skin_list_chg then return end
        local o = HT.proc_skin_list_chg
        HT.proc_skin_list_chg = function(typeName, id1, insID, bagIdx, extra)
            local ret = o(typeName, id1, insID, bagIdx, extra)
            typeName = tostring(typeName or "")
            if typeName == "vehicle_skin" or typeName == "vehicle" or typeName:find("vehicle", 1, true) then
                insID = tonumber(insID)
                local resID = select(1, resFromVehicleIns(insID))
                if resID and isVehicleSkinRes(resID) then
                    rememberLobbyVehicleSkin(resID, insID)
                    pcall(_G.scheduleLobbyConfigFlush)
                end
            end
            return ret
        end
    end)
end

function _G.flushLobbySelectionsToConfig()
    local updates = {}
    syncWeaponCacheFromLobby()
    pcall(syncVehicleCacheFromLobby)
    local outfit = tonumber(_G.AddOutfitLastLobbyOutfitRes) or tonumber(_G.__ACTIVE_OUTFIT_RES) or 0
    if outfit <= 0 then outfit = tonumber(cache().outfitRes) or 0 end
    if outfit <= 0 then outfit = tonumber(MATCH_CONFIG.outfitRes) or 0 end
    if outfit > 0 and isLobbyOutfitRes(outfit) then updates.SHIRT = outfit end
    for wid, w in pairs(cache().weapons or {}) do
        wid = tonumber(wid)
        local key = wid and WEAPON_ID_TO_CONFIG_KEY[wid]
        local res = w and tonumber(w.resID)
        if key and res and res > 0 then updates[key] = res end
    end
    if MATCH_CONFIG.weaponSkins then
        for key, wid in pairs(WEAPON_CONFIG_KEYS) do
            local res = tonumber(MATCH_CONFIG.weaponSkins[wid])
            if res and res > 0 then updates[key] = res end
        end
    end
    collectWornItemUpdates(updates)
    collectVehicleUpdates(updates)
    if _G.TargetLobbyThemeID and tonumber(_G.TargetLobbyThemeID) > 0 then
        updates.LobbyTheme = tonumber(_G.TargetLobbyThemeID)
    end
    return _G.writeConfigBatch(updates)
end

function _G.scheduleLobbyConfigFlush()
    if _G.__configFlushScheduled then return end
    _G.__configFlushScheduled = true
    later(1.5, function()
        _G.__configFlushScheduled = nil
        pcall(_G.flushLobbySelectionsToConfig)
    end)
end

local function applyConfigAccessories()
    pcall(function() if _G.ReadConfigFile then _G.ReadConfigFile(true) end end)
    if GameStatus and GameStatus.IsInLobbyOrMainCity and not GameStatus.IsInLobbyOrMainCity() then
        return
    end
    pcall(function()
        local AvatarData = require("client.logic.data.AvatarData")
        local wd = require("client.slua.logic.wardrobe.wardrobe_data")
        local wornRes = {}
        for _, ins in pairs(AvatarData.GetRoleWear()) do
            ins = tonumber(ins)
            if ins and ins > 0 then
                local d = wd:GetValidHallDepotItemDataByInsID(ins)
                    or wd:GetHallDepotItemDataByInsID(ins)
                local res = d and tonumber(d.resID) or nil
                if res and res > 0 then wornRes[res] = ins end
            end
        end
        for _, res in ipairs({
            tonumber(_G.ParachuteSkin),
            tonumber(_G.Backpack3Skin), tonumber(_G.Backpack2Skin), tonumber(_G.Backpack1Skin),
            tonumber(_G.Helmet3Skin), tonumber(_G.Helmet2Skin), tonumber(_G.Helmet1Skin),
            tonumber(_G.PetSkin),
        }) do
            if res and res > 0 and isInjectedRes(res) and wornRes[res] and isLobbyOutfitRes(res) then
                local ins = tonumber(R.resToIns[res]) or wornRes[res]
                if ins and ins > 0 then pcall(function() putOnCloth(ins) end) end
            end
        end
    end)
    pcall(function()
        if _G.GameAvatarHandlerplayers then _G.GameAvatarHandlerplayers() end
    end)
end

local function applySavedLobbyFull()
    loadMatchConfigFromIni(true)
    pcall(function() if _G.ReadConfigFile then _G.ReadConfigFile(true) end end)
    local savedOutfit = tonumber(MATCH_CONFIG.outfitRes) or 0
    if savedOutfit <= 0 then
        savedOutfit = tonumber(_G.AddOutfitLastLobbyOutfitRes) or tonumber(_G.__ACTIVE_OUTFIT_RES) or 0
    end
    if savedOutfit <= 0 then savedOutfit = tonumber(cache().outfitRes) or 0 end
    if savedOutfit > 0 then
        local insID = tonumber(_G.AddOutfitLastLobbyOutfitIns) or 0
        if insID <= 0 and _G.__skinResToIns then insID = tonumber(_G.__skinResToIns[savedOutfit]) or 0 end
        if insID <= 0 and _G.getOutfitInsId then insID = tonumber(_G.getOutfitInsId(savedOutfit)) or 0 end
        rememberLobbyOutfitRes(savedOutfit, insID)
        _G.AddOutfitLastLobbyOutfitRes = savedOutfit
        if insID > 0 then _G.AddOutfitLastLobbyOutfitIns = insID end
        syncLobbyOutfitToConfigIfNeeded(savedOutfit)
    end
    if MATCH_CONFIG.weaponSkins then
        local cch = cache()
        for wid, res in pairs(MATCH_CONFIG.weaponSkins) do
            wid, res = tonumber(wid), tonumber(res)
            if wid and res and res > 0 then
                cch.weapons[wid] = { resID = res, insID = tonumber(R.resToIns[res]) or 0 }
            end
        end
    end
    pcall(syncVehicleCacheFromLobby)
    applyConfiguredLobbySkins()
    applyConfigAccessories()
    pcall(function() if _G.ApplyLobbyTheme then _G.ApplyLobbyTheme() end end)
    pcall(applyLobbyVehicleFromConfig)
    local gunCount = 0
    if MATCH_CONFIG.weaponSkins then
        for _, res in pairs(MATCH_CONFIG.weaponSkins) do
            if tonumber(res) and tonumber(res) > 0 then gunCount = gunCount + 1 end
        end
    end
    skinProbeLog("config restore: outfit=" .. tostring(_G.__ACTIVE_OUTFIT_RES or MATCH_CONFIG.outfitRes or 0)
        .. " guns=" .. tostring(gunCount)
        .. " vehicle=" .. tostring(_G.LobbyVehicleSkin or 0)
        .. " theme=" .. tostring(_G.TargetLobbyThemeID or 0))
end

local function hookLobbyThemePersist()
    pcall(function()
        local ModuleManager = require("client.module_framework.ModuleManager")
        if not ModuleManager or not ModuleManager.LobbyModuleConfig then return end
        local mod = ModuleManager.GetModule(ModuleManager.LobbyModuleConfig.LobbyThemeManager)
        if not mod then return end
        local function onThemePick(themeID)
            themeID = tonumber(themeID)
            if not themeID or themeID <= 0 then return end
            _G.TargetLobbyThemeID = themeID
            _G.LastAppliedThemeID = nil
            pcall(function() _G.patchConfigIni("LobbyTheme", themeID) end)
        end
        if mod.SetTheme then
            local o = mod.SetTheme
            mod.SetTheme = function(self, themeID, ...)
                local ret = o(self, themeID, ...)
                onThemePick(themeID)
                return ret
            end
        end
        if mod.ShowThemeByItemID then
            local o = mod.ShowThemeByItemID
            mod.ShowThemeByItemID = function(self, themeID, ...)
                local ret = o(self, themeID, ...)
                onThemePick(themeID)
                return ret
            end
        end
        if mod.ApplyTheme then
            local o = mod.ApplyTheme
            mod.ApplyTheme = function(self, themeID, ...)
                local ret = o(self, themeID, ...)
                onThemePick(themeID)
                return ret
            end
        end
    end)
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
                if bootstrapUltimateSkin then
                    later(0.5, bootstrapUltimateSkin)
                end
                return true
            end
        end
    end
    return false
end

local ITEMS = {
    202408001, 202408003, 202408005, 202408009, 202408010, 202408011, 202408012, 202408013, 202408014, 202408015,
    202408016, 202408017, 202408018, 202408019, 202408021, 202408022, 202408023, 202408024, 202408025, 202408026,
    202408027, 202408028, 202408029, 202408030, 202408031, 202408032, 202408033, 202408034, 202408035, 202408036,
    202408037, 202408038, 202408039, 202408040, 202408041, 202408042, 202408043, 202408044, 202408045, 202408046,
    202408047, 202408048, 202408049, 202408050, 202408051, 202408052, 202408053, 202408054, 202408055, 202408056,
    202408057, 202408058, 202408059, 202408060, 202408061, 202408062, 202408063, 202408064, 202408065, 202408066,
    202408067, 202408068, 202408069, 202408070, 202408071, 202408072, 202408073, 202408074, 202408075, 202408076,
    202408077, 202408078, 202408079, 202408080, 202408081, 202408082, 202408083, 202408084, 202408085, 202408086,
    202408087, 202408088, 202408089, 202408090, 202408091, 202408092, 202408093, 202408094, 202408095, 202408096,
    202408097, 202408098, 202408099, 202408100, 202408101, 202408102, 202501001, 202501002, 202501003, 202501004,
    202501005, 202501006, 202501007, 202501008, 202501009, 202501010, 202501011,
    1407757, 1407696, 1407682, 1407512, 1407726, 1405207, 1407188, 1407187, 1406939, 1407286, 1407471, 1407470,
    1407441, 1404153, 1405208, 1406742, 1405718, 1407276, 1407079, 1407329, 1406569, 1405092, 1407318, 1400782,
    1405556, 1405923, 1405334, 1406021, 1405623, 1407277, 1407275, 1405962, 1601048, 1407161, 1406398, 1407330,
    1407225, 1405174, 1407174, 1406897, 1405208, 1405207, 1404049, 1405628, 1406469, 1407366, 1405870, 1405983,
    1406152, 1406311, 1406475, 1406641, 1406872, 1406971, 1406891, 1400687, 1407103, 1407219, 1407916, 1407917, 1407918,
    1401842, 12220054,
    40604012, 1402162, 1410585, 40605012, 40605014, 1410686, 1402145, 1410647, 40605015, 1400563, 1403028, 1404050,
    1404051, 1400651, 1400371, 1401469, 1401619, 1401621, 1401622, 1401615, 1401628, 452001, 107019, 108007, 452002,
    452003, 4151125, 4151118, 4151119, 4151122, 4151115, 4151140, 4151143, 2200101, 12220568, 12220023, 12219677, 12219716, 12209401,
    12209501, 12209701, 12209801, 12209901, 12210001, 2200201, 12210201, 12210601, 12220028, 12219819, 12211801,
    12212001, 12212201, 12212401, 2200301, 12212601, 12213201, 12219715, 12219814, 12213601, 12213801, 12214001,
    12214201,
    1501001687, 1501001677, 1501001672, 1501001649, 1501001174, 1501001668, 1501001081, 1501001618, 1501001220,
    1501001582, 1501001047, 1501001496, 1501001051, 1501001588, 1501000057, 1501001061, 1501001058, 1501001069,
    1501001628, 1501000724, 1501001724,
    1501002687, 1501002677, 1501002672, 1501002649, 1501002174, 1501002668, 1501002081, 1501002618, 1501002220,
    1501002582, 1501002047, 1501002496, 1501002051, 1501002588, 1501000057, 1501002061, 1501002058, 1501002069,
    1501002628, 1501002724,
    1501003687, 1501003677, 1501003672, 1501003649, 1501003174, 1501003668, 1501003081, 1501003618, 1501003220,
    1501003582, 1501003047, 1501003496, 1501003051, 1501003054, 1501003022, 1501003046, 1501003173,
    1501003588, 1501000057, 1501003061, 1501003058, 1501003069,
    1501003628, 1501003724,
    1502001014, 1502001069, 1502001023, 1502001173, 1502000497, 1502001497, 1502002014, 1502002069, 1502002023,
    1502002497, 1502003014, 1502003069, 1502003023, 1502003497,
    1908094, 1908095, 1908032, 1908036, 1908066, 1908067, 1908075, 1908076, 1908077, 1908078, 1908084, 1908085,
    1908086, 1908088, 1908089, 1908188, 1908189, 1901091, 1901073, 1901074, 1901075, 1901076, 1901047, 1901102,
    1901085, 1902022, 1902030, 1966018, 1960002, 1960003, 1903193, 1903201, 1903087, 1903075, 1903071, 1903072, 1903073,
    1903074, 1903076, 1961062, 1961063, 1961064, 1961147, 1961148, 1961149, 1961015, 1961145, 1961144, 1961056,
    1961055, 1961052, 1961007, 1961010, 1961012, 1961013, 1961014, 1961016, 1961017, 1961018, 1961020, 1961021,
    1961024, 1961025, 1961029, 1961030, 1961031, 1961041, 1961042, 1961044, 1961048, 1961050, 1961051, 1907054,
    1907058, 1907059, 1907063, 1915008, 1915012, 1915021, 1915022, 1915005, 1915006, 1915007, 1915009, 1953008,
    1953016, 1953004, 1904015, 1916004, 1916005, 1916006, 1919011,
    1101002081, 1101004236, 1101004046, 1101004226, 1101004062, 1101004086, 1101004163, 1101004201, 1101004209,
    1101004218, 1101001265, 1101001213, 1101001231, 1101001242, 1101001256, 1101001249, 1101003219, 1101003146,
    1101003208, 1101003167, 1101003181, 1101003195, 1101006062, 1101006075, 1101008154, 1101008163, 1101008136,
    1101008081, 1101008070, 1101008026, 1101008116, 1101008146, 1101008126, 1101005052, 1101005090, 1101012009,
    1101102041, 1101102049, 1101102017, 1101102007, 1101102025, 1101007046, 1101007062, 1102002136, 1102002438, 1102001103,
    1102001112, 1102001998, 1102001102, 1102003100, 1102003080, 1102003052, 1102003031, 1102003039, 1102004018,
    1103001202, 1103001179, 1103001191, 1103002087, 1103002094, 1103003087, 1103012010, 1103012019, 1103007028,
    1103006030, 1105001048, 1105001062, 1105002063, 1105002071, 1105002076, 1105010008, 1105010019, 1108004027,
    1108005052, 1108005050, 1108003025, 1108001057, 1108001104, 613004092, 615004007, 612004143, 612004233, 615004114, 1104003027, 1104003037,
    1104004024, 1104004035, 1104101001, 1104002022, 1104001035, 1103100007, 1103010004, 1103009051, 1103008023,
    1103004087, 1103005048, 1020070191, 1020050382, 1101100018, 1101010016, 1101009019, 1102105012, 1102105018
}

local INS_BASE = 2000000000
local PKG_SLOT = 3
local MELEE_ID = 108
local GUN_SUB = { [101]=true, [102]=true, [103]=true, [104]=true, [105]=true, [106]=true, [107]=true }
local NET_OK = NetErrorCode_NONE or "ok"

local R = { insToRes = {}, resToIns = {} }
local _matchApplied = false

local function cache()
    _G.AddOutfitEquippedCache = _G.AddOutfitEquippedCache or {
        outfitRes = nil, outfitIns = nil,
        weapons = {},
    }
    return _G.AddOutfitEquippedCache
end

local function cfg(resID)
    if not resID or not CDataTable or not CDataTable.GetTableData then return nil end
    return CDataTable.GetTableData("Item", resID)
end

local function subType(c)
    return c and (c.ItemSubType or c.itemSubType) or nil
end

local ST_TOP     = (ENUM_ITEM_SUBTYPE and ENUM_ITEM_SUBTYPE.Package_Slot) or 403
local ST_PANTS   = (ENUM_ITEM_SUBTYPE and ENUM_ITEM_SUBTYPE.Pants_Slot) or 404
local ST_SHOES   = (ENUM_ITEM_SUBTYPE and ENUM_ITEM_SUBTYPE.Shoes_Slot) or 405
local ST_UNDER_T = (ENUM_ITEM_SUBTYPE and ENUM_ITEM_SUBTYPE.UnderCloth) or 450
local ST_UNDER_P = (ENUM_ITEM_SUBTYPE and ENUM_ITEM_SUBTYPE.UnderPants) or 451
local WARDROBE_TAB_SUIT, WARDROBE_TAB_CLOTHES = 10, 3

pcall(function()
    local wm = require("client.slua.umg.Wardrobe.wardrobe_macro")
    WARDROBE_TAB_SUIT = wm.ENUM_WardrobeSubTabString.ENUM_WardrobeSubTabString_suit
    WARDROBE_TAB_CLOTHES = wm.ENUM_WardrobeSubTabString.ENUM_WardrobeSubTabString_clothes
end)

local FULL_SUIT_CLEAR_ST = {
    [ST_TOP] = true, [ST_PANTS] = true, [ST_SHOES] = true,
    [ST_UNDER_T] = true, [ST_UNDER_P] = true,
}

local function wardrobeTab(resID, depotData)
    if depotData and depotData.subTabType then return tonumber(depotData.subTabType) end
    local c = cfg(resID)
    return c and tonumber(c.WardrobeTab or c.wardrobeTab) or nil
end

local function isFullSuitRes(resID, depotData)
    resID = tonumber(resID)
    if not resID or resID <= 0 then return false end
    local ok, xs = pcall(function()
        local LogicXSuit = require("client.slua.logic.XSuit.logic_xsuit")
        return LogicXSuit.IsXSuit(resID)
    end)
    if ok and xs then return true end
    local tab = wardrobeTab(resID, depotData)
    if tab == WARDROBE_TAB_SUIT then return true end
    if tab == WARDROBE_TAB_CLOTHES then return false end
    for _, id in ipairs(ITEMS) do
        if tonumber(id) == resID and subType(cfg(resID)) == ST_TOP then
            return true
        end
    end
    return false
end

local function getClothKind(resID, depotData)
    resID = tonumber(resID)
    if not resID then return nil end
    local st = subType(cfg(resID))
    if st == ST_TOP then
        return isFullSuitRes(resID, depotData) and "full_suit" or "top"
    end
    if st == ST_PANTS then return "pants" end
    if st == ST_SHOES then return "shoes" end
    if st == ST_UNDER_T then return "under_top" end
    if st == ST_UNDER_P then return "under_pants" end
    return nil
end

local function isLobbyOutfitRes(resID, depotData)
    resID = tonumber(resID)
    if not resID or resID <= 0 then return false end
    if isVehicleSkinRes(resID) then return false end
    local st = subType(cfg(resID))
    if GUN_SUB[st] or st == MELEE_ID then return false end
    local kind = getClothKind(resID, depotData)
    if kind == "full_suit" or kind == "top" or kind == "pants" or kind == "shoes" then return true end
    for _, id in ipairs(ITEMS) do
        if tonumber(id) == resID then
            if GUN_SUB[st] or st == MELEE_ID then return false end
            if st == ST_TOP or isFullSuitRes(resID, depotData) then return true end
            return false
        end
    end
    local s = tostring(resID)
    return s:sub(1, 4) == "1406" or s:sub(1, 4) == "1407" or s:sub(1, 4) == "1405"
        or s:sub(1, 4) == "1404" or s:sub(1, 5) == "12220"
end
_G.isLobbyOutfitRes = isLobbyOutfitRes

local function resolveClothKind(resID, depotData)
    local kind = getClothKind(resID, depotData)
    if kind then return kind end
    resID = tonumber(resID)
    if not resID then return nil end
    for _, id in ipairs(ITEMS) do
        if tonumber(id) == resID then
            local st = subType(cfg(resID))
            if GUN_SUB[st] or st == MELEE_ID or isVehicleSkinRes(resID) then return nil end
            if st == ST_TOP or isFullSuitRes(resID, depotData) then return "full_suit" end
            return nil
        end
    end
    return nil
end
_G.resolveClothKind = resolveClothKind

local function normalizeOutfitRes(resID)
    resID = tonumber(resID) or 0
    if resID > 0 and isLobbyOutfitRes(resID) then return resID end
    return 0
end

local function subTypesToClearForKind(kind)
    if kind == "full_suit" then return FULL_SUIT_CLEAR_ST end
    if kind == "top" then return { [ST_TOP] = true } end
    if kind == "pants" then return { [ST_PANTS] = true } end
    if kind == "shoes" then return { [ST_SHOES] = true } end
    if kind == "under_top" then return { [ST_UNDER_T] = true } end
    if kind == "under_pants" then return { [ST_UNDER_P] = true } end
    return nil
end

local function isBodyClothSubType(st)
    st = tonumber(st)
    return st == ST_TOP or st == ST_PANTS or st == ST_SHOES or st == ST_UNDER_T or st == ST_UNDER_P
end

local function weaponIdFromSkin(resID)
    local m = CDataTable and CDataTable.GetTableData and CDataTable.GetTableData("WeaponSkinMapping", resID)
    if not m then return nil end
    return m.WeaponID or m.WeaponId
end

local function isInjectedIns(ins)
    return ins and R.insToRes[tonumber(ins)] ~= nil
end

local function isInjectedRes(res)
    return res and R.resToIns[tonumber(res)] ~= nil
end

local function insIdForItemRes(outfitRes)
    outfitRes = tonumber(outfitRes)
    if not outfitRes or outfitRes <= 0 then return 0 end
    if _G.__skinResToIns and _G.__skinResToIns[outfitRes] then
        return tonumber(_G.__skinResToIns[outfitRes]) or 0
    end
    if R.resToIns[outfitRes] then
        return tonumber(R.resToIns[outfitRes]) or 0
    end
    for i, res in ipairs(ITEMS) do
        if tonumber(res) == outfitRes then return INS_BASE + i end
    end
    return 0
end

local function isAllowedOutfitRes(resID)
    resID = tonumber(resID)
    if not resID or resID <= 0 then return false end
    if isInjectedRes(resID) then return true end
    if MATCH_CONFIG.outfitRes and tonumber(MATCH_CONFIG.outfitRes) == resID then return true end
    if _G.SuitSkin and tonumber(_G.SuitSkin) == resID then return true end
    return false
end
_G.isInjectedSkinRes = isInjectedRes
_G.isAllowedOutfitRes = isAllowedOutfitRes

local function invalidateSocialWearCache()
    local s = _G.AddOutfitSocialState
    if s then
        s.wearPatchKey, s.snapshotKey, s.fullSnapshot, s.lastHandSkin = nil, nil, nil, nil
    end
end

local function saveWeaponToCache(weaponID, resID, insID)
    weaponID, resID, insID = tonumber(weaponID), tonumber(resID), tonumber(insID)
    if not weaponID or not resID or resID <= 0 then return end
    local cch = cache()
    cch.weapons[weaponID] = { resID = resID, insID = insID or 0 }
    _G.AddOutfitLastAppliedSkin = {}
    _matchApplied = false
    invalidateSocialWearCache()
    log("Skin Memory", weaponID, "->", resID)
    pcall(function() _G.syncLobbyItemToConfig(resID, weaponID) end)
end

local function cacheWeaponSkinFromIns(weaponID, insID)
    weaponID, insID = tonumber(weaponID), tonumber(insID)
    if not weaponID or not insID or insID <= 0 then return end
    if isInjectedIns(insID) then
        saveWeaponToCache(weaponID, R.insToRes[insID], insID)
        return
    end
    pcall(function()
        local wd = require("client.slua.logic.wardrobe.wardrobe_data")
        local d = wd:GetValidHallDepotItemDataByInsID(insID) or wd:GetHallDepotItemDataByInsID(insID)
        if d and d.resID and tonumber(d.resID) > 0 then
            saveWeaponToCache(weaponID, tonumber(d.resID), insID)
        end
    end)
end

local function rememberLobbyOutfitRes(resID, insID)
    resID = tonumber(resID)
    insID = tonumber(insID)
    if not resID or resID <= 0 or not isLobbyOutfitRes(resID) then return end
    _G.AddOutfitLastLobbyOutfitRes = resID
    local cch = cache()
    cch.outfitRes = resID
    if insID and insID > 0 then
        cch.outfitIns = insID
        _G.AddOutfitLastLobbyOutfitIns = insID
    elseif isInjectedRes(resID) then
        cch.outfitIns = R.resToIns[resID]
        _G.AddOutfitLastLobbyOutfitIns = cch.outfitIns
    end
    MATCH_CONFIG.outfitRes = resID
    _G.__ACTIVE_OUTFIT_RES = resID
end

local function readWornOutfitFromWardrobe()
    local wornRes, wornIns
    local preferred = tonumber(_G.AddOutfitLastLobbyOutfitRes) or tonumber(_G.__ACTIVE_OUTFIT_RES) or 0
    pcall(function()
        local AvatarData = require("client.logic.data.AvatarData")
        local wd = require("client.slua.logic.wardrobe.wardrobe_data")
        local function resFromIns(ins)
            ins = tonumber(ins)
            if not ins or ins <= 0 then return nil, nil end
            if isInjectedIns(ins) then return tonumber(R.insToRes[ins]), ins end
            local d = wd:GetValidHallDepotItemDataByInsID(ins)
                or wd:GetHallDepotItemDataByInsID(ins)
            return d and tonumber(d.resID) or nil, ins
        end
        local function consider(ins)
            local res, insID = resFromIns(ins)
            if res and isFullSuitRes(res) then
                wornRes, wornIns = res, insID
            end
        end
        for _, ins in pairs(AvatarData.GetRoleWear()) do
            consider(ins)
            if preferred > 0 then
                local res = select(1, resFromIns(ins))
                if res == preferred then
                    wornRes, wornIns = preferred, ins
                end
            end
        end
        local fbd = require("client.slua.logic.wardrobe.fashionbag.fashionbag_data")
        local bag = fbd.GetCurrentFashionBag and fbd:GetCurrentFashionBag()
        if bag and bag.rolewear_list then
            for _, ins in pairs(bag.rolewear_list) do
                consider(ins)
                if preferred > 0 then
                    local res = select(1, resFromIns(ins))
                    if res == preferred then
                        wornRes, wornIns = preferred, ins
                    end
                end
            end
        end
    end)
    if preferred > 0 then
        local cch = cache()
        return preferred, wornIns or tonumber(cch.outfitIns) or tonumber(R.resToIns[preferred])
    end
    if wornRes and wornRes > 0 then return wornRes, wornIns end
    return nil, nil
end

local function syncLobbyOutfitToConfigIfNeeded(resID)
    resID = tonumber(resID)
    if not resID or resID <= 0 or not isLobbyOutfitRes(resID) then return end
    if _G.__lastLobbyOutfitSynced == resID then return end
    _G.__lastLobbyOutfitSynced = resID
    _G.SuitSkin = resID
    _G.__ACTIVE_OUTFIT_RES = resID
    MATCH_CONFIG.outfitRes = resID
    _G.__configOutfitApplied = nil
    _lastIniContent = ""
    pcall(function() _G.patchConfigIni("SHIRT", resID) end)
    skinProbeLog("lobby->config SHIRT=" .. tostring(resID))
end

local function resolveLobbyOutfitRes()
    local outfitRes = normalizeOutfitRes(_G.AddOutfitLastLobbyOutfitRes)
    if outfitRes <= 0 then outfitRes = normalizeOutfitRes(_G.__ACTIVE_OUTFIT_RES) end
    if outfitRes > 0 then return outfitRes end
    local cch = cache()
    outfitRes = normalizeOutfitRes(cch.outfitRes)
    if outfitRes > 0 then return outfitRes end
    outfitRes = normalizeOutfitRes(MATCH_CONFIG.outfitRes)
    if outfitRes > 0 then return outfitRes end
    outfitRes = normalizeOutfitRes(_G.SuitSkin)
    if outfitRes > 0 then return outfitRes end
    local wornRes, wornIns = readWornOutfitFromWardrobe()
    wornRes = normalizeOutfitRes(wornRes)
    if wornRes > 0 then
        rememberLobbyOutfitRes(wornRes, wornIns)
        syncLobbyOutfitToConfigIfNeeded(wornRes)
        return wornRes
    end
    return nil
end

local function resolveOutfitInsForRes(outfitRes)
    outfitRes = tonumber(outfitRes) or 0
    if outfitRes <= 0 then return 0 end
    local cch = cache()
    local ins = tonumber(cch.outfitIns) or tonumber(_G.AddOutfitLastLobbyOutfitIns) or 0
    if ins > 0 then return ins end
    if _G.__skinResToIns and _G.__skinResToIns[outfitRes] then
        return tonumber(_G.__skinResToIns[outfitRes]) or 0
    end
    if _G.getOutfitInsId then
        local g = tonumber(_G.getOutfitInsId(outfitRes)) or 0
        if g > 0 then return g end
    end
    return insIdForItemRes(outfitRes)
end

local function scheduleConfigOutfitRetry(attempt)
    local target = tonumber(MATCH_CONFIG.outfitRes) or tonumber(_G.AddOutfitLastLobbyOutfitRes) or 0
    if target <= 0 then return end
    if _G.__configOutfitApplied == target then return end
    attempt = tonumber(attempt) or 1
    if attempt > 18 then return end
    later(0.5 + (attempt * 0.4), function()
        if equipConfigOutfit() then return end
        scheduleConfigOutfitRetry(attempt + 1)
    end)
end

local function equipConfigOutfit()
    pcall(function() loadMatchConfigFromIni(false) end)
    local outfitRes = normalizeOutfitRes(MATCH_CONFIG.outfitRes)
    if outfitRes <= 0 then outfitRes = normalizeOutfitRes(_G.AddOutfitLastLobbyOutfitRes) end
    if outfitRes <= 0 then return false end
    if _G.__configOutfitApplied == outfitRes then return true end
    if not isLobbyOutfitRes(outfitRes) or not isInjectedRes(outfitRes) then
        skinProbeLog("config outfit wait inject res=" .. outfitRes)
        return false
    end
    local ins = resolveOutfitInsForRes(outfitRes)
    if ins <= 0 then
        skinProbeLog("config outfit wait ins res=" .. outfitRes)
        return false
    end
    pcall(refreshWardrobe)
    rememberLobbyOutfitRes(outfitRes, ins)
    if not putOnOutfit(ins) and not (_G.applyOutfitViaWardrobe and _G.applyOutfitViaWardrobe(outfitRes)) then
        skinProbeLog("config outfit putOn failed res=" .. outfitRes .. " ins=" .. ins)
        return false
    end
    _G.__configOutfitApplied = outfitRes
    pcall(function() onSocialWearDirty(true) end)
    skinProbeLog("config outfit equip " .. outfitRes .. " ins=" .. ins)
    return true
end

local function saveEquip(resID, insID)
    resID, insID = tonumber(resID), tonumber(insID)
    if not resID or not insID then return end
    local c = cfg(resID)
    local st = subType(c)
    local cch = cache()
    if GUN_SUB[st] then
        local wid = weaponIdFromSkin(resID)
        if wid then saveWeaponToCache(wid, resID, insID) end
    elseif st == MELEE_ID then
        saveWeaponToCache(MELEE_ID, resID, insID)
    elseif isVehicleSkinRes(resID) then
        rememberLobbyVehicleSkin(resID, insID)
    elseif resolveClothKind(resID) == "full_suit" and isLobbyOutfitRes(resID) then
        if cch.outfitRes == resID and cch.outfitIns == insID
            and _G.AddOutfitLastLobbyOutfitRes == resID
            and _G.AddOutfitLastLobbyOutfitIns == insID then
            return
        end
        cch.outfitRes, cch.outfitIns = resID, insID
        _G.AddOutfitLastLobbyOutfitRes = resID
        MATCH_CONFIG.outfitRes = resID
        _G.__ACTIVE_OUTFIT_RES = resID
        _G.SuitSkin = resID
        _G.AddOutfitLastLobbyOutfitIns = insID
        invalidateSocialWearCache()
        _G.__lastLobbyOutfitSynced = nil
        syncLobbyOutfitToConfigIfNeeded(resID)
        skinProbeLog("equip outfit " .. resID .. " ins=" .. insID)
    elseif resolveClothKind(resID) == "top" then
        if cch.outfitRes and isFullSuitRes(cch.outfitRes) then
            cch.outfitRes, cch.outfitIns = nil, nil
            invalidateSocialWearCache()
        end
    else
        pcall(function() _G.syncLobbyItemToConfig(resID) end)
    end
    pcall(_G.scheduleLobbyConfigFlush)
    _matchApplied = false
end

local function syncWeaponCacheFromLobby()
    local cch = cache()
    pcall(function()
        local fbd = require("client.slua.logic.wardrobe.fashionbag.fashionbag_data")
        local bag = fbd.GetCurrentFashionBag and fbd:GetCurrentFashionBag()
        if bag and bag.weapon_skin_list then
            for weaponID, entry in pairs(bag.weapon_skin_list) do
                weaponID = tonumber(weaponID)
                local insID = tonumber(entry and (entry.skin_id or entry.skinId)) or 0
                if weaponID and weaponID > 0 and insID > 0 then
                    if isInjectedIns(insID) then
                        local res = tonumber(R.insToRes[insID])
                        if res and res > 0 then
                            cch.weapons[weaponID] = { resID = res, insID = insID }
                        end
                    else
                        local wd = require("client.slua.logic.wardrobe.wardrobe_data")
                        local d = wd:GetValidHallDepotItemDataByInsID(insID)
                            or wd:GetHallDepotItemDataByInsID(insID)
                        if d and d.resID and tonumber(d.resID) > 0 then
                            cch.weapons[weaponID] = { resID = tonumber(d.resID), insID = insID }
                        end
                    end
                end
            end
        end
    end)
    pcall(function()
        local Arm = require("client.logic.armory.logic_armory")
        if Arm.rsp_list and Arm.rsp_list.install_list then
            for weaponID, entry in pairs(Arm.rsp_list.install_list) do
                weaponID = tonumber(weaponID)
                local insID = tonumber(entry and entry.skin_id) or 0
                if weaponID and weaponID > 0 and insID > 0 then
                    if isInjectedIns(insID) then
                        local res = tonumber(R.insToRes[insID])
                        if res and res > 0 then
                            cch.weapons[weaponID] = { resID = res, insID = insID }
                        end
                    else
                        local wd = require("client.slua.logic.wardrobe.wardrobe_data")
                        local d = wd:GetValidHallDepotItemDataByInsID(insID)
                            or wd:GetHallDepotItemDataByInsID(insID)
                        if d and d.resID and tonumber(d.resID) > 0 then
                            cch.weapons[weaponID] = { resID = tonumber(d.resID), insID = insID }
                        end
                    end
                end
            end
        end
    end)
    pcall(function()
        local wgl = require("client.slua.logic.wardrobe.logic_wardrobe_gun")
        if wgl.GetSkinIdByWeaponID then
            local guns = { 101001, 101002, 101003, 101004, 101005, 101006, 101007, 101008, 101009, 101010, 101012, 102001, 102002, 102003, 102004, 102005, 102007, 103001, 103002, 103003, 103004, 103005, 103006, 103007, 103008, 103009, 103010, 103011, 103012, 104001, 104002, 104003, 104004, 105001, 105002, 106001, 106002, 106003, 106004, 106005, 106006, 106007, 106008, 106010 }
            local wd = require("client.slua.logic.wardrobe.wardrobe_data")
            for _, wid in ipairs(guns) do
                local insID = tonumber(wgl:GetSkinIdByWeaponID(wid)) or 0
                if insID > 0 then
                    local d = wd:GetValidHallDepotItemDataByInsID(insID) or wd:GetHallDepotItemDataByInsID(insID)
                    if d and d.resID and tonumber(d.resID) > 0 then
                        cch.weapons[wid] = { resID = tonumber(d.resID), insID = insID }
                    end
                end
            end
        end
    end)
    pcall(function()
        if (tonumber(cache().outfitRes) or 0) <= 0 then
            resolveLobbyOutfitRes()
        end
    end)
    pcall(syncVehicleCacheFromLobby)
end

local function getCachedWeaponSkin(weaponID)
    weaponID = tonumber(weaponID) or 0
    if weaponID <= 0 then return nil end
    syncWeaponCacheFromLobby()
    local w = cache().weapons[weaponID]
    if w and w.resID and w.resID > 0 then return w.resID end
    return nil
end

local function getMatchWeaponSkin(weaponID)
    weaponID = tonumber(weaponID) or 0
    local fromCache = getCachedWeaponSkin(weaponID)
    if fromCache then return fromCache end
    if MATCH_CONFIG.weaponSkins then
        local fixed = tonumber(MATCH_CONFIG.weaponSkins[weaponID])
        if fixed and fixed > 0 then return fixed end
    end
    return nil
end

local function findWornInsBySubType(st)
    st = tonumber(st)
    if not st then return nil end
    local wd = require("client.slua.logic.wardrobe.wardrobe_data")
    local AvatarData = require("client.logic.data.AvatarData")
    for _, ins in pairs(AvatarData.GetRoleWear()) do
        ins = tonumber(ins)
        if ins and ins > 0 then
            local d = wd:GetHallDepotItemDataByInsID(ins)
            if d and tonumber(d.itemSubType) == st then
                return ins, d.resID
            end
        end
    end
    return nil
end

local function removeRoleWearBySubTypes(stMap)
    if not stMap then return end
    local wd = require("client.slua.logic.wardrobe.wardrobe_data")
    local AvatarData = require("client.logic.data.AvatarData")
    for _, ins in pairs(AvatarData.GetRoleWear()) do
        ins = tonumber(ins)
        if ins and ins > 0 then
            local d = wd:GetHallDepotItemDataByInsID(ins)
            if d and stMap[tonumber(d.itemSubType)] then
                AvatarData.RemoveRoleWearDataByValue(ins)
            end
        end
    end
end

local function clearFashionBagSlots(stMap)
    if not stMap then return end
    pcall(function()
        local fbd = require("client.slua.logic.wardrobe.fashionbag.fashionbag_data")
        local wfu = require("client.slua.logic.wardrobe.fashionbag.wardrobe_fashion_utils")
        local bag = fbd.GetCurrentFashionBag and fbd:GetCurrentFashionBag()
        if not bag or not bag.rolewear_list then return end
        for st, _ in pairs(stMap) do
            local idx = wfu.GetRoleWearIndexBySubType and wfu:GetRoleWearIndexBySubType(st)
            if idx then bag.rolewear_list[idx] = 0 end
        end
    end)
end

local function removeRoleWearBySubType(st)
    if not st then return end
    removeRoleWearBySubTypes({ [tonumber(st)] = true })
end

local function syncFashionBagRolewear()
    pcall(function()
        local fbd = require("client.slua.logic.wardrobe.fashionbag.fashionbag_data")
        fbd:SaveRolewearToFashionBag(fbd:GetFashionBagUseIndex())
    end)
end

local _ticker
pcall(function() _ticker = require("common.time_ticker") end)
local function later(sec, fn)
    if _G.SetTimer then pcall(_G.SetTimer, sec, fn) return end
    if _ticker and _ticker.AddTimer then pcall(_ticker.AddTimer, sec, fn) end
end

local function getEntity()
    local ok, dc = pcall(require, "client.slua.logic.wardrobe.logic_wardrobe_data_center")
    if not ok or not dc then return nil end
    local ok2, e = pcall(dc.GetWardrobeData)
    return ok2 and e or nil
end

local function alreadyHave(entity, resID)
    local arr = entity.ResIDToIndexArrayMap and entity.ResIDToIndexArrayMap[resID]
    if not arr then return false end
    for _, idx in pairs(arr) do
        local d = entity._data[idx]
        if d and d.count and d.count > 0 then return true end
    end
    return false
end

local function injectOne(entity, resID, insID)
    if alreadyHave(entity, resID) then
        R.resToIns[resID] = R.resToIns[resID] or insID
        R.insToRes[insID] = resID
        return true
    end
    local row = {
        instid = insID,
        res_id = resID,
        count = 1,
        lock_cnt = 0,
        isnew = 0,
        valid_hours = 0,
        expire_ts = 0,
    }
    entity:AddData(row)
    pcall(function()
        local data = entity.GetDataByInsID and entity:GetDataByInsID(insID)
        if data and entity.LoadConfigForData and CDataTable.GetTableData then
            entity:LoadConfigForData(data, CDataTable.GetTableData)
        end
    end)
    R.insToRes[insID] = resID
    R.resToIns[resID] = insID
    _G.__skinInsToRes = R.insToRes
    _G.__skinResToIns = R.resToIns
    log("Injected", resID, insID)
    return true
end

local function injectArmory(resID, insID)
    local wid = weaponIdFromSkin(resID)
    if not wid then return end
    local Arm = require("client.logic.armory.logic_armory")
    Arm.rsp_list = Arm.rsp_list or { skin_list = {}, install_list = {} }
    Arm.rsp_list.skin_list = Arm.rsp_list.skin_list or {}
    Arm.rsp_list.install_list = Arm.rsp_list.install_list or {}
    if not Arm.rsp_list.skin_list[wid] then Arm.rsp_list.skin_list[wid] = {} end
    Arm.rsp_list.skin_list[wid][resID] = { is_open = 1 }
    Arm.WardrobeInsList = Arm.WardrobeInsList or {}
    Arm.WardrobeInsList[resID] = insID
end

local function injectAll(entity)
    entity = entity or getEntity()
    if not entity then
        skinProbeLog("injectAll: wardrobe entity missing")
        return false
    end
    if not entity.bInit then
        skinProbeLog("injectAll: wardrobe not initialized yet")
        return false
    end
    _G.__configOutfitApplied = nil
    local n = 0
    for i, resID in ipairs(ITEMS) do
        local insID = INS_BASE + i
        if injectOne(entity, resID, insID) then
            n = n + 1
            local c = cfg(resID)
            if GUN_SUB[subType(c)] or subType(c) == MELEE_ID then
                injectArmory(resID, insID)
            end
        end
    end
    log("Injected", n, "/", #ITEMS)
    skinProbeLog("injectAll done " .. n .. "/" .. #ITEMS)
    if n > 0 and tonumber(MATCH_CONFIG.outfitRes) and tonumber(MATCH_CONFIG.outfitRes) > 0 then
        later(0.2, function()
            pcall(equipConfigOutfit)
            pcall(function() scheduleConfigOutfitRetry(1) end)
        end)
    end
    return n > 0
end

local function refreshWardrobe()
    pcall(function()
        if EventSystem and EVENTTYPE_WARDROBE then
            if EVENTID_WARDROBE_UPDATE_ITEM_LIST then
                EventSystem:postEvent(EVENTTYPE_WARDROBE, EVENTID_WARDROBE_UPDATE_ITEM_LIST)
            end
            if EVENTID_WARDROBE_UPDATE_AVATAR_LIST then
                EventSystem:postEvent(EVENTTYPE_WARDROBE, EVENTID_WARDROBE_UPDATE_AVATAR_LIST)
            end
            if EVENTID_WARDROBE_UPDATE_GUN_LIST then
                EventSystem:postEvent(EVENTTYPE_WARDROBE, EVENTID_WARDROBE_UPDATE_GUN_LIST, -1)
            end
        end
    end)
end

local function putOnCloth(insID)
    insID = tonumber(insID)
    local resID = R.insToRes[insID]
    if not resID then
        local idx = insID - INS_BASE
        if idx >= 1 and idx <= #ITEMS then
            resID = tonumber(ITEMS[idx])
            R.insToRes[insID] = resID
            R.resToIns[resID] = insID
        end
    end
    if not resID then
        skinProbeLog("putOnCloth: unknown ins=" .. tostring(insID))
        return false
    end
    if not isLobbyOutfitRes(resID) then
        skinProbeLog("putOnCloth: not outfit res=" .. tostring(resID))
        return false
    end
    local wd = require("client.slua.logic.wardrobe.wardrobe_data")
    local d = wd:GetHallDepotItemDataByInsID(insID) or wd:GetValidHallDepotItemDataByInsID(insID)
    if not d then
        d = { resID = resID, res_id = resID, instid = insID, insID = insID }
    end

    local kind = resolveClothKind(resID, d)
    if not kind then
        skinProbeLog("putOnCloth: unknown kind res=" .. tostring(resID))
        return false
    end
    local clearMap = subTypesToClearForKind(kind)
    if not clearMap then return end

    local itemSt = subType(cfg(resID)) or ST_TOP
    local oldIns, oldRes = findWornInsBySubType(itemSt)
    removeRoleWearBySubTypes(clearMap)
    clearFashionBagSlots(clearMap)
    saveEquip(resID, insID)

    local slot = PKG_SLOT
    pcall(function()
        local wfu = require("client.slua.logic.wardrobe.fashionbag.wardrobe_fashion_utils")
        local idx = wfu.GetRoleWearIndexBySubType and wfu:GetRoleWearIndexBySubType(itemSt)
        if idx then slot = idx end
    end)

    local olditem
    if oldIns and oldIns ~= insID then
        olditem = { res_id = oldRes or R.insToRes[oldIns], count = 1, instid = oldIns }
    end

    local WRH = require("client.network.Protocol.WardRobeHandler")
    local item = { res_id = resID, count = 1, instid = insID }
    WRH.on_depot_put_on_rsp(NET_OK, item, olditem, slot, insID, oldIns or 0)

    pcall(function()
        local av = require("client.slua.logic.wardrobe.logic_wardrobe_avatar")
        av:AddToWearInfo(itemSt, insID, resID, 0, 0)
        local displayResID = resID
        local LogicXSuit = require("client.slua.logic.XSuit.logic_xsuit")
        if LogicXSuit.IsXSuit(displayResID) then
            displayResID = LogicXSuit.GetItemShowID(insID) or displayResID
        end
        av:AvatarChange(displayResID, true, 0, 0)
        av:ProcessTakeOff()
        syncFashionBagRolewear()
    end)
    log("Equipped", kind, resID)
    return true
end

local function putOnOutfit(insID)
    return putOnCloth(insID)
end

local function equipWeaponSkin(weaponID, insID)
    weaponID, insID = tonumber(weaponID), tonumber(insID)
    if not weaponID or not insID or not isInjectedIns(insID) then return end
    local resID = R.insToRes[insID]
    saveEquip(resID, insID)

    local Arm = require("client.logic.armory.logic_armory")
    local fbd = require("client.slua.logic.wardrobe.fashionbag.fashionbag_data")
    local HT = require("client.logic.lobby.hall_theme_utils")
    local wgl = require("client.slua.logic.wardrobe.logic_wardrobe_gun")

    injectArmory(resID, insID)
    Arm.rsp_list.install_list[weaponID] = { skin_id = insID }
    if fbd.UpdateCurrentFashionBagWeaponSkin then
        fbd:UpdateCurrentFashionBagWeaponSkin(weaponID, insID)
    end

    local bagIdx = fbd:GetFashionBagUseIndex()
    HT.proc_skin_list_chg("weapon_skin", weaponID, insID, bagIdx, {})

    wgl:SetGunID(weaponID)
    wgl:UpdateCurrentGunAvatar(weaponID, insID)

    if EventSystem and EVENTTYPE_ARMORY and EVENTID_ARMORY_EQUIP_STAT_CHANGE then
        EventSystem:postEvent(EVENTTYPE_ARMORY, EVENTID_ARMORY_EQUIP_STAT_CHANGE, resID)
    end
    if EventSystem and EVENTTYPE_WARDROBE and EVENTID_WARDROBE_UPDATE_CURRENT_PUT_ON_GUN then
        EventSystem:postEvent(EVENTTYPE_WARDROBE, EVENTID_WARDROBE_UPDATE_CURRENT_PUT_ON_GUN, resID)
    end
    log("Weapon Skin", weaponID, resID, insID)
end

local SOCIAL = _G.AddOutfitSocialState or {}
_G.AddOutfitSocialState = SOCIAL
SOCIAL.debGen = SOCIAL.debGen or 0
SOCIAL.wearPatchKey = SOCIAL.wearPatchKey or nil
SOCIAL.snapshotKey = SOCIAL.snapshotKey or nil
SOCIAL.fullSnapshot = SOCIAL.fullSnapshot or nil

local function socialDebounce(sec, fn)
    SOCIAL.debGen = (SOCIAL.debGen or 0) + 1
    local gen = SOCIAL.debGen
    later(sec, function()
        if gen ~= SOCIAL.debGen then return end
        pcall(fn)
    end)
end

local function getLobbyCurPage()
    local p = nil
    pcall(function()
        local LMC = require("client.slua.logic.lobby.Main.Lobby_Main_Control")
        if LMC.GetCurPage then p = LMC.GetCurPage() end
    end)
    return p
end

local function getWeaponSkinResFast()
    local cch = cache()
    local wid = tonumber(DataMgr.Weapon_ID) or 0
    local w = wid > 0 and cch.weapons[wid] or nil
    if w and w.resID and w.resID > 0 then return w.resID end
    for _, ww in pairs(cch.weapons) do
        if ww.resID and ww.resID > 0 then return ww.resID end
    end
    return nil
end

local function resolveLobbyWeaponSkinRes()
    local wid = tonumber(DataMgr.Weapon_ID) or 0
    local skin = getWeaponSkinResFast()
    if skin and skin > 0 then return skin end

    if wid > 0 then
        local fromMatch = getMatchWeaponSkin(wid)
        if fromMatch and fromMatch > 0 then return fromMatch end
    end
    if MATCH_CONFIG.weaponSkins then
        for _, s in pairs(MATCH_CONFIG.weaponSkins) do
            s = tonumber(s)
            if s and s > 0 then return s end
        end
    end

    pcall(function()
        local Arm = require("client.logic.armory.logic_armory")
        local entry = Arm.rsp_list and Arm.rsp_list.install_list
            and Arm.rsp_list.install_list[wid > 0 and wid or 101004]
        local insID = tonumber(entry and entry.skin_id) or 0
        if insID > 0 and isInjectedIns(insID) then
            skin = tonumber(R.insToRes[insID])
        elseif insID > 0 then
            local wd = require("client.slua.logic.wardrobe.wardrobe_data")
            local d = wd:GetHallDepotItemDataByInsID(insID)
            if d and d.resID then skin = tonumber(d.resID) end
        end
    end)
    if skin and skin > 0 then return skin end

    pcall(function()
        local wgl = require("client.slua.logic.wardrobe.logic_wardrobe_gun")
        if wgl.GetSkinIdByWeaponID and wid > 0 then
            local insID = tonumber(wgl:GetSkinIdByWeaponID(wid)) or 0
            if insID > 0 and isInjectedIns(insID) then
                skin = tonumber(R.insToRes[insID])
            end
        end
    end)
    return (skin and skin > 0) and skin or nil
end

local function wearPatchKey()
    local outfit = resolveLobbyOutfitRes() or 0
    local skin = resolveLobbyWeaponSkinRes() or 0
    local openGun = 1
    pcall(function()
        local lds = require("client.slua.logic.wardrobe.logic_display_setting")
        if lds.data and lds.data.OpenGun ~= nil then openGun = lds.data.OpenGun and 1 or 0 end
    end)
    return outfit .. "_" .. skin .. "_" .. openGun
end

local function syncDepotShowWeaponFlags(depot)
    depot = depot or {}
    depot.vehicle = true
    depot.helmet = true
    depot.bag = true
    depot.pet = true
    depot.idle = true
    depot.hand = true
    pcall(function()
        local lds = require("client.slua.logic.wardrobe.logic_display_setting")
        if lds.data then
            if lds.data.OpenGun ~= nil then depot.weapon = lds.data.OpenGun end
            if lds.data.OpenSocialWeapon ~= nil then depot.social_weapon = lds.data.OpenSocialWeapon end
        end
    end)
    return depot
end

local function applyInjectedPspace(roleData)
    if not roleData then return end
    roleData.bshow = true
    roleData.pspace_wear_ext = roleData.pspace_wear_ext or {}
    local outfitRes = resolveLobbyOutfitRes()
    if outfitRes and outfitRes > 0 then
        roleData.pspace_wear_ext[ENUM_AVATAR_SHOW_TYPE.SHOW_POS_CLOTH] = { outfitRes, 0, 0 }
    end
    local skinRes = resolveLobbyWeaponSkinRes()
    if skinRes and skinRes > 0 then
        roleData.pspace_wear_ext[ENUM_AVATAR_SHOW_TYPE.SHOW_POS_WEAPON] = { 0, 0, 0 }
        roleData.pspace_wear_ext[ENUM_AVATAR_SHOW_TYPE.SHOW_POS_WEAPONSKIN] = { skinRes, 0, 0 }
        roleData.depot_show_info = roleData.depot_show_info or {}
        if roleData.depot_show_info.weapon == nil then
            roleData.depot_show_info.weapon = true
        end
    end
    roleData.depot_show_info = syncDepotShowWeaponFlags(roleData.depot_show_info)
end

local function patchSelfWearCache(force)
    local key = wearPatchKey()
    if not force and SOCIAL.wearPatchKey == key then return false end
    SOCIAL.wearPatchKey = key
    SOCIAL.snapshotKey = nil
    SOCIAL.fullSnapshot = nil

    local myUid = tonumber(DataMgr.roleData.uid)
    if not myUid then return false end

    local changed = false
    pcall(function()
        local BD = ModuleManager.GetModule(ModuleManager.DataModuleConfig.BasicDataAvatarWearInfo)
        local d = BD:GetCacheData(myUid)
        if not d then
            BD:OnHandleMsgDataAndCallback(myUid, buildLocalRoleDataForCoupleAvatar())
            return true
        end
        local oldCloth = d.pspace_wear_ext and d.pspace_wear_ext[ENUM_AVATAR_SHOW_TYPE.SHOW_POS_CLOTH]
        local oldSkin = d.pspace_wear_ext and d.pspace_wear_ext[ENUM_AVATAR_SHOW_TYPE.SHOW_POS_WEAPONSKIN]
        applyInjectedPspace(d)
        local nc = d.pspace_wear_ext[ENUM_AVATAR_SHOW_TYPE.SHOW_POS_CLOTH]
        local ns = d.pspace_wear_ext[ENUM_AVATAR_SHOW_TYPE.SHOW_POS_WEAPONSKIN]
        if oldCloth ~= nc or oldSkin ~= ns or not d.bshow then changed = true end
    end)
    return force or changed
end

local function requestSocialAvatarRefresh()
    pcall(function()
        if EventSystem and EVENTTYPE_LOBBY_SOCIAL and EVENTID_SOCIAL_LOBBY_REFRESH_AVATAR then
            EventSystem:postEvent(EVENTTYPE_LOBBY_SOCIAL, EVENTID_SOCIAL_LOBBY_REFRESH_AVATAR)
        end
    end)
end

local function onSocialWearDirty(forceRefresh)
    SOCIAL.lastHandSkin = nil
    if patchSelfWearCache(forceRefresh) then
        requestSocialAvatarRefresh()
    end
end

local function buildLocalRoleDataForCoupleAvatar()
    local key = wearPatchKey()
    if SOCIAL.fullSnapshot and SOCIAL.snapshotKey == key then
        return SOCIAL.fullSnapshot
    end
    syncWeaponCacheFromLobby()
    local cch = cache()
    local ad = DataMgr.avatarData or {}
    local gender = tonumber(ad.gamegender) or 2
    if gender < 1 then gender = 2 end

    local data = {
        uid = DataMgr.roleData.uid,
        gender = gender,
        bshow = true,
        pspace_wear_ext = {
            [ENUM_AVATAR_SHOW_TYPE.SHOW_POS_HEAD] = { tonumber(ad.headid) or 401993, 0, 0 },
            [ENUM_AVATAR_SHOW_TYPE.SHOW_POS_HAIR] = { tonumber(ad.hairid) or 40601001, 0, 0 },
            [ENUM_AVATAR_SHOW_TYPE.SHOW_POS_WEAPON] = { 0, 0, 0 },
            [ENUM_AVATAR_SHOW_TYPE.SHOW_POS_WEAPONSKIN] = { 0, 0, 0 },
        },
        depot_show_info = {
            weapon = true, social_weapon = true, idle = true,
            helmet = true, bag = true, vehicle = true, hand = true, pet = true
        },
    }

    local outfitRes = resolveLobbyOutfitRes()
    if outfitRes and outfitRes > 0 then
        data.pspace_wear_ext[ENUM_AVATAR_SHOW_TYPE.SHOW_POS_CLOTH] = { outfitRes, 0, 0 }
    end

    local skinRes = resolveLobbyWeaponSkinRes()
    if skinRes and skinRes > 0 then
        data.pspace_wear_ext[ENUM_AVATAR_SHOW_TYPE.SHOW_POS_WEAPON][1] = 0
        data.pspace_wear_ext[ENUM_AVATAR_SHOW_TYPE.SHOW_POS_WEAPONSKIN][1] = skinRes
    end
    data.depot_show_info = syncDepotShowWeaponFlags(data.depot_show_info)
    SOCIAL.fullSnapshot = data
    SOCIAL.snapshotKey = wearPatchKey()
    return data
end

local _myUidCached
local function isMyWearData(wearData)
    if not wearData then return false end
    if not _myUidCached then
        pcall(function() _myUidCached = tonumber(DataMgr.roleData.uid) end)
    end
    return _myUidCached and tonumber(wearData.uid) == _myUidCached
end

local function mergeInjectedWeaponIntoWearData(wearData)
    if not isMyWearData(wearData) then return end
    local skinRes = resolveLobbyWeaponSkinRes()
    wearData.depot_show_info = syncDepotShowWeaponFlags(wearData.depot_show_info)
    if not skinRes or skinRes <= 0 then return end
    wearData.mainWeaponInfo = wearData.mainWeaponInfo or {
        weaponResId = 0, weaponSkinId = 0,
        diyInfo = { diyWeaponId = 0, diyDefaultScheme = false, diyScheme = nil },
    }
    if wearData.mainWeaponInfo.weaponSkinId == skinRes
        and (tonumber(wearData.mainWeaponInfo.weaponResId) or 0) == 0 then
        return
    end
    wearData.mainWeaponInfo.weaponSkinId = skinRes
    wearData.mainWeaponInfo.weaponResId = 0
end

local function equipSocialHandWeapon(avatar, skinRes)
    if not avatar or not skinRes or skinRes <= 0 then return end
    if SOCIAL.lastHandSkin == skinRes then return end
    SOCIAL.lastHandSkin = skinRes
    pcall(function()
        avatar:PutonEquipment(skinRes, nil, { bIsUse = true })
    end)
end

local function shouldShowHandWeapon()
    local show = true
    pcall(function()
        local lds = require("client.slua.logic.wardrobe.logic_display_setting")
        if lds.data and lds.data.OpenGun ~= nil then
            show = lds.data.OpenGun ~= false
        end
    end)
    return show
end

local function mergeInjectedOutfitIntoWearData(wearData)
    if not isMyWearData(wearData) then return end
    local outfitRes = resolveLobbyOutfitRes()
    if not outfitRes or outfitRes <= 0 or not isFullSuitRes(outfitRes) then return end
    rememberLobbyOutfitRes(outfitRes)
    local AvatarData = require("client.logic.data.AvatarData")
    local converted = AvatarData.ConvertToAvatarCustom({ outfitRes, 0, 0 })
    if not converted then return end
    local newList = {}
    for _, e in ipairs(wearData.WearInfoList or {}) do
        if e and e.ItemID and isBodyClothSubType(subType(cfg(e.ItemID))) then
        else
            newList[#newList + 1] = e
        end
    end
    newList[#newList + 1] = converted
    wearData.WearInfoList = newList
end

local function mergeInjectedIntoWearData(wearData)
    if not wearData then return end
    mergeInjectedWeaponIntoWearData(wearData)
    mergeInjectedOutfitIntoWearData(wearData)
end

local function applyConfiguredLobbySkins()
    loadMatchConfigFromIni(true)
    local applied = false
    if equipConfigOutfit() then
        applied = true
    else
        local outfitRes = tonumber(resolveLobbyOutfitRes()) or 0
        if outfitRes > 0 and isInjectedRes(outfitRes) then
            local ins = resolveOutfitInsForRes(outfitRes)
            if ins > 0 then
                rememberLobbyOutfitRes(outfitRes, ins)
                putOnOutfit(ins)
                applied = true
                skinProbeLog("lobby outfit " .. outfitRes .. " ins=" .. ins)
            end
        elseif outfitRes > 0 then
            skinProbeLog("lobby outfit pending inject " .. outfitRes)
        end
    end
    if MATCH_CONFIG.weaponSkins then
        for wid, res in pairs(MATCH_CONFIG.weaponSkins) do
            wid = tonumber(wid)
            res = tonumber(res)
            if wid and res and res > 0 and isInjectedRes(res) then
                local ins = tonumber(R.resToIns[res]) or 0
                if ins > 0 then
                    equipWeaponSkin(wid, ins)
                    applied = true
                    skinProbeLog("config gun " .. wid .. " skin=" .. res)
                end
            end
        end
    end
    pcall(applyLobbyVehicleFromConfig)
    return applied
end

local function refreshLobbyOutfitVisual(force)
    if not GameStatus or not GameStatus.IsInLobbyOrMainCity or not GameStatus.IsInLobbyOrMainCity() then
        return false
    end
    loadMatchConfigFromIni(false)
    local outfitRes = normalizeOutfitRes(resolveLobbyOutfitRes())
    if outfitRes <= 0 then return false end
    local ins = resolveOutfitInsForRes(outfitRes)
    if ins <= 0 or not isInjectedRes(outfitRes) or not isLobbyOutfitRes(outfitRes) then return false end
    if force then
        _G.__configOutfitApplied = nil
        _G.__outfitProbeDone = nil
    end
    rememberLobbyOutfitRes(outfitRes, ins)
    pcall(refreshWardrobe)
    local ok = putOnOutfit(ins)
    if not ok and _G.applyOutfitViaWardrobe then
        ok = _G.applyOutfitViaWardrobe(outfitRes) or ok
    end
    pcall(function()
        if _G.GameAvatarHandlerplayers then _G.GameAvatarHandlerplayers() end
    end)
    pcall(function() onSocialWearDirty(true) end)
    if ok then _G.__needsLobbyOutfitRefresh = nil end
    skinProbeLog("lobby outfit refresh res=" .. outfitRes .. " ins=" .. ins .. " ok=" .. tostring(ok))
    return ok
end
_G.refreshLobbyOutfitVisual = refreshLobbyOutfitVisual

local function reapplyLobbyEquipped()
    if not GameStatus or not GameStatus.IsInLobbyOrMainCity or not GameStatus.IsInLobbyOrMainCity() then
        return
    end
    syncWeaponCacheFromLobby()
    loadMatchConfigFromIni(false)
    local curPage = getLobbyCurPage()

    if ENUM_LobbyPageType and curPage == ENUM_LobbyPageType.Left then
        applyConfiguredLobbySkins()
        onSocialWearDirty(true)
        return
    end

    refreshLobbyOutfitVisual(true)

    local cch = cache()

    if MATCH_CONFIG.weaponSkins then
        for wid, res in pairs(MATCH_CONFIG.weaponSkins) do
            wid = tonumber(wid)
            res = tonumber(res)
            if wid and res and isInjectedRes(res) then
                local ins = tonumber(R.resToIns[res]) or 0
                if ins > 0 then equipWeaponSkin(wid, ins) end
            end
        end
    end

    for wid, w in pairs(cch.weapons) do
        wid = tonumber(wid)
        if wid and w and w.resID and w.resID > 0 then
            if w.insID and isInjectedIns(w.insID) then
                equipWeaponSkin(wid, w.insID)
            else
                pcall(function() DataMgr.InitWeaponData(wid, w.resID, w.insID or 0) end)
            end
        end
    end

    pcall(function()
        local uid = tostring(DataMgr.roleData.uid)
        local LAM = require("client.logic.avatar.LobbyAvatarManager")
        local TAM = require("client.logic.avatar.logic_team_avatar_manager")
        local mainWid = tonumber(DataMgr.Weapon_ID) or 0
        local mw = mainWid > 0 and cch.weapons[mainWid] or nil
        if mw and mw.resID and mw.resID > 0 and TAM.GetAvatarByUid(uid) then
            LAM.EquipWeapon(uid, { weaponId = mainWid, skinId = mw.resID }, nil, true)
        end
    end)

    pcall(function()
        if EventSystem and EVENTTYPE_WARDROBE and EVENTID_WARDROBE_UPDATE_AVATAR_LIST then
            EventSystem:postEvent(EVENTTYPE_WARDROBE, EVENTID_WARDROBE_UPDATE_AVATAR_LIST)
        end
    end)
    log("Lobby Reapplied")
end

local function hookLobbySwipePersistence()
    pcall(function()
        local BD = ModuleManager.GetModule(ModuleManager.DataModuleConfig.BasicDataAvatarWearInfo)
        local oRsp = BD.on_get_avatar_show_rsp
        BD.on_get_avatar_show_rsp = function(self, res, target_uid, data)
            oRsp(self, res, target_uid, data)
                if tonumber(target_uid) == tonumber(DataMgr.roleData.uid) then
                patchSelfWearCache(true)
                SOCIAL.forceAvatarRedraw = true
                SOCIAL.lastHandSkin = nil
                if ENUM_LobbyPageType and getLobbyCurPage() == ENUM_LobbyPageType.Left then
                    requestSocialAvatarRefresh()
                end
            end
        end
    end)

    pcall(function()
        local AC = require("client.slua.logic.avatar.avatar_common")
        local oGetWear = AC.GetWearDataFromRoleData
        AC.GetWearDataFromRoleData = function(roleData)
            local wearData = oGetWear(roleData)
            if wearData and roleData and tonumber(roleData.uid) == tonumber(DataMgr.roleData.uid) then
                mergeInjectedIntoWearData(wearData)
            end
            return wearData
        end
        local oUp = AC.UpdateAvatar
        AC.UpdateAvatar = function(avatar, wearData, isShowWeapon, isShowHelmet, isShowBag)
            if isMyWearData(wearData) then
                mergeInjectedIntoWearData(wearData)
            end
            local showGun = isShowWeapon and shouldShowHandWeapon()
            if wearData and wearData.depot_show_info then
                showGun = showGun and wearData.depot_show_info.weapon ~= false
            end
            if isMyWearData(wearData) then
                for _, e in ipairs(wearData.WearInfoList or {}) do
                    if e and e.ItemID and isInjectedRes(e.ItemID) and isFullSuitRes(e.ItemID) then
                        rememberLobbyOutfitRes(e.ItemID)
                        break
                    end
                end
            end
            local ret = oUp(avatar, wearData, showGun, isShowHelmet, isShowBag)
            if showGun and isMyWearData(wearData) and avatar
                and ENUM_LobbyPageType and getLobbyCurPage() == ENUM_LobbyPageType.Left then
                local skin = tonumber(wearData.mainWeaponInfo and wearData.mainWeaponInfo.weaponSkinId) or 0
                if skin <= 0 then skin = resolveLobbyWeaponSkinRes() or 0 end
                if skin > 0 then equipSocialHandWeapon(avatar, skin) end
            end
            return ret
        end
    end)

    pcall(function()
        local CA = require("client.logic.avatar.CoupleAvatar")
        local Cfg = require("client.slua.logic.lobby.Left.CoupleAvatarConfig")
        local oMulti = CA._UpdateMultiAvatar
        if oMulti then
            CA._UpdateMultiAvatar = function(self, avatar, avatarType)
                local isSelf = avatarType == Cfg.AvatarType.Self
                    and self.SelfUID and tostring(self.SelfUID) == tostring(DataMgr.roleData.uid)
                if isSelf then
                    pcall(function()
                        local BD = ModuleManager.GetModule(ModuleManager.DataModuleConfig.BasicDataAvatarWearInfo)
                        local d = BD:GetCacheData(tonumber(self.SelfUID))
                        if d then applyInjectedPspace(d) end
                    end)
                    if SOCIAL.forceAvatarRedraw then
                        self.CompareDataCache[avatarType] = nil
                        SOCIAL.forceAvatarRedraw = nil
                    end
                end
                oMulti(self, avatar, avatarType)
                if isSelf and self.isShowWeapon ~= false and shouldShowHandWeapon()
                    and ENUM_LobbyPageType and getLobbyCurPage() == ENUM_LobbyPageType.Left then
                    local skin = resolveLobbyWeaponSkinRes()
                    if skin and skin > 0 then equipSocialHandWeapon(avatar, skin) end
                end
            end
        end
        local oHideCheck = CA.CheckSelfIsHideAvatar
        CA.CheckSelfIsHideAvatar = function(self, nSelfUId, tRoleData)
            if tostring(nSelfUId) == tostring(DataMgr.roleData.uid) then
                return false
            end
            return oHideCheck(self, nSelfUId, tRoleData)
        end

        local oUpdate = CA.Update
        CA.Update = function(self)
            local isSelf = self.SelfUID and tostring(self.SelfUID) == tostring(DataMgr.roleData.uid)
            local oHide = CA.HideAvatars
            if isSelf then
                CA.HideAvatars = function() end
            end
            local ok, err = pcall(oUpdate, self)
            CA.HideAvatars = oHide
            if not ok then log("CoupleAvatar.Update", err) end
        end

        local oRecv = CA.OnReceiveData
        CA.OnReceiveData = function(self, uid, data)
            if uid == self.SelfUID and tostring(uid) == tostring(DataMgr.roleData.uid) then
                if data then
                    applyInjectedPspace(data)
                else
                    data = buildLocalRoleDataForCoupleAvatar()
                end
            end
            return oRecv(self, uid, data)
        end
    end)

    pcall(function()
        if not EventSystem or not EventSystem.registEvent then return end
        if EVENTTYPE_LOBBY and EVENTID_SWITCHTO_PAGE_START then
            EventSystem:registEvent(EVENTTYPE_LOBBY, EVENTID_SWITCHTO_PAGE_START, function(_, _, toPage)
                if ENUM_LobbyPageType and toPage == ENUM_LobbyPageType.Left then
                    syncWeaponCacheFromLobby()
                    SOCIAL.lastHandSkin = nil
                    local o = resolveLobbyOutfitRes()
                    if o then rememberLobbyOutfitRes(o) end
                    patchSelfWearCache(true)
                    SOCIAL.forceAvatarRedraw = true
                end
            end)
        end
        if EVENTTYPE_LOBBY and EVENTID_SWITCHTO_PAGE_END then
            EventSystem:registEvent(EVENTTYPE_LOBBY, EVENTID_SWITCHTO_PAGE_END, function(_, _, _, toPage)
                if ENUM_LobbyPageType and toPage == ENUM_LobbyPageType.Left then
                    syncWeaponCacheFromLobby()
                    SOCIAL.lastHandSkin = nil
                    socialDebounce(0.45, function()
                        onSocialWearDirty(true)
                    end)
                elseif ENUM_LobbyPageType and toPage == ENUM_LobbyPageType.Mid then
                    SOCIAL.wearPatchKey = nil
                    SOCIAL.forceAvatarRedraw = true
                    _G.__configOutfitApplied = nil
                    _G.__outfitProbeDone = nil
                    socialDebounce(0.35, function()
                        pcall(refreshLobbyOutfitVisual, true)
                        pcall(reapplyLobbyEquipped)
                    end)
                end
            end)
        end
        if EVENTTYPE_LOBBY_SOCIAL and EVENTID_GOT_SOCIAL_LOBBY_SHOW_DATA then
            EventSystem:registEvent(EVENTTYPE_LOBBY_SOCIAL, EVENTID_GOT_SOCIAL_LOBBY_SHOW_DATA, function(_, _, nUId)
                if tonumber(nUId) == tonumber(DataMgr.roleData.uid) then
                    socialDebounce(0.2, function() patchSelfWearCache(false) end)
                end
            end)
        end
        if EVENTTYPE_WARDROBE and EVENTID_WARDROBE_UPDATE_CURRENT_PUT_ON_GUN then
            EventSystem:registEvent(EVENTTYPE_WARDROBE, EVENTID_WARDROBE_UPDATE_CURRENT_PUT_ON_GUN, function()
                SOCIAL.wearPatchKey = nil
                SOCIAL.snapshotKey = nil
                syncWeaponCacheFromLobby()
                if ENUM_LobbyPageType and getLobbyCurPage() == ENUM_LobbyPageType.Left then
                    socialDebounce(0.25, function() onSocialWearDirty(true) end)
                end
            end)
        end
    end)

    pcall(function()
        local lds = require("client.slua.logic.wardrobe.logic_display_setting")
        local oSwitch = lds.SwitchGun
        lds.SwitchGun = function(...)
            local r = oSwitch(...)
            SOCIAL.wearPatchKey = nil
            if ENUM_LobbyPageType and getLobbyCurPage() == ENUM_LobbyPageType.Left then
                socialDebounce(0.2, function() onSocialWearDirty(true) end)
            end
            return r
        end
    end)
end

local function hookDepotInit()
    pcall(function()
        local WDE = require("client.slua.logic.wardrobe.WardrobeDataEntity")
        local orig = WDE.InitData
        WDE.InitData = function(self, pkg)
            orig(self, pkg)
            skinProbeLog("WardrobeDataEntity.InitData")
            if injectAll(self) then
                _G.__SKIN_BOOT_DONE = true
                refreshWardrobe()
                later(0.35, applyConfiguredLobbySkins)
                later(1.2, function()
                    applyConfiguredLobbySkins()
                    reapplyLobbyEquipped()
                end)
            end
        end
    end)
end

local function hookWardrobeData()
    pcall(function()
        local wd = require("client.slua.logic.wardrobe.wardrobe_data")
        local function wrapGet(name)
            local o = wd[name]
            if not o then return end
            wd[name] = function(self, insID, ...)
                insID = tonumber(insID)
                if isInjectedIns(insID) then
                    local e = getEntity()
                    if e then return e:GetDataByInsID(insID) end
                end
                return o(self, insID, ...)
            end
        end
        wrapGet("GetHallDepotItemDataByInsID")
        wrapGet("GetValidHallDepotItemDataByInsID")
        local function wrapBool(name)
            local o = wd[name]
            if not o then return end
            wd[name] = function(self, id, ...)
                if isInjectedRes(tonumber(id)) or isInjectedIns(tonumber(id)) then return true end
                return o(self, id, ...)
            end
        end
        wrapBool("HasItem")
        wrapBool("HasValidItem")
        wrapBool("CheckHasPermanentItem")
    end)
end

local function hookPageFilter()
    pcall(function()
        local wl = require("client.slua.logic.wardrobe.logic_wardrobe_new")
        local o1 = wl.IsValidCurrentPageItem
        wl.IsValidCurrentPageItem = function(self, mainTab, subTab, v, t)
            if v and isInjectedRes(v.resID) and mainTab == 1 then
                if v.expireTS == 0 or not t or t < v.expireTS then
                    local st = v.itemSubType or subType(cfg(v.resID))
                    if st == ST_TOP then
                        local full = isFullSuitRes(v.resID, v)
                        if subTab == WARDROBE_TAB_SUIT and full then return true end
                        if subTab == WARDROBE_TAB_CLOTHES and not full then return true end
                    end
                    if v.subTabType == subTab then return true end
                end
            end
            return o1(self, mainTab, subTab, v, t)
        end
        local o2 = wl.IsCanUse
        wl.IsCanUse = function(self, resId)
            if isInjectedRes(resId) then return true end
            return o2(self, resId)
        end
        local o3 = wl.IsCharacterUse
        wl.IsCharacterUse = function(self, resId)
            if isInjectedRes(resId) then return true end
            return o3(self, resId)
        end
        local o4 = wl.GetWardrobeInsIdByResId
        wl.GetWardrobeInsIdByResId = function(self, resid)
            resid = tonumber(resid)
            if isInjectedRes(resid) then return R.resToIns[resid] end
            return o4(self, resid)
        end
    end)
end

local function hookArmory()
    pcall(function()
        local Arm = require("client.logic.armory.logic_armory")
        local og = Arm.GetSkinListByWeaponID
        Arm.GetSkinListByWeaponID = function(wid)
            local t = og(wid) or {}
            for resID, _ in pairs(R.resToIns) do
                if tonumber(weaponIdFromSkin(resID)) == tonumber(wid) then
                    t[resID] = t[resID] or { is_open = 1 }
                end
            end
            return t
        end
        local oa = Arm.get_weapon_skin_list_rsp
        Arm.get_weapon_skin_list_rsp = function(a, b, c, d)
            oa(a, b, c, d)
            for resID, insID in pairs(R.resToIns) do injectArmory(resID, insID) end
        end
        local oi = Arm.install_weapon_skin
        Arm.install_weapon_skin = function(cd, wid, ins)
            ins = tonumber(ins)
            if isInjectedIns(ins) then
                wid = tonumber(weaponIdFromSkin(R.insToRes[ins]) or wid)
                equipWeaponSkin(wid, ins)
                return
            end
            return oi(cd, wid, ins)
        end
    end)
    pcall(function()
        local AH = require("client.network.Protocol.ArmoryHandler")
        local o = AH.send_install_weapon_skin
        AH.send_install_weapon_skin = function(cd, wid, ins)
            ins = tonumber(ins)
            if isInjectedIns(ins) then
                wid = tonumber(weaponIdFromSkin(R.insToRes[ins]) or wid)
                equipWeaponSkin(wid, ins)
                return
            end
            return o(cd, wid, ins)
        end
    end)
end

local function hookGunSkinId()
    pcall(function()
        local wgl = require("client.slua.logic.wardrobe.logic_wardrobe_gun")
        local o = wgl.GetSkinIdByWeaponID
        wgl.GetSkinIdByWeaponID = function(self, wid)
            local c = cache()
            local w = c.weapons[wid]
            if w and isInjectedIns(w.insID) then return w.insID end
            local Arm = require("client.logic.armory.logic_armory")
            if Arm.rsp_list and Arm.rsp_list.install_list and Arm.rsp_list.install_list[wid] then
                local sid = Arm.rsp_list.install_list[wid].skin_id
                if sid and isInjectedIns(sid) then return sid end
            end
            return o(self, wid)
        end
    end)
end

local function backpackSkinDisplayReady(skinId, baseWeaponId)
    skinId = tonumber(skinId) or 0
    baseWeaponId = tonumber(baseWeaponId) or 0
    if skinId <= 0 or skinId == baseWeaponId then return false end
    _G.SkinLoadedCache = _G.SkinLoadedCache or _G.skinIdCache
    if _G.skinIdCache and _G.skinIdCache[skinId] then return true end
    if _G.skinIdCache2 and _G.skinIdCache2[skinId] then return true end
    local fn = _G.get_skin_id
    if fn and tonumber(fn(baseWeaponId)) == skinId then return true end
    return false
end

local function hookBackpackWeaponAppearance()
    pcall(function()
        local ok, WIIB = pcall(require, "GameLua.Mod.BaseMod.Client.Backpack.WeaponInfoItemBase")
        if not ok or not WIIB or not WIIB.__inner_impl then return end
        if _G.BackpackSkinHooked then return end

        local old_UpdateWeaponAppearanceInfo = WIIB.__inner_impl.UpdateWeaponAppearanceInfo

        WIIB.__inner_impl.UpdateWeaponAppearanceInfo = function(self, TypeSpecificID, BattleData, DragOrigin)
            TypeSpecificID = tonumber(TypeSpecificID) or 0
            local ItemData = cfg(TypeSpecificID)
            if not ItemData then
                return old_UpdateWeaponAppearanceInfo(self, TypeSpecificID, BattleData, DragOrigin)
            end

            local skin_id = 0
            if _G.get_skin_id then
                skin_id = tonumber(_G.get_skin_id(TypeSpecificID)) or 0
            end

            if not backpackSkinDisplayReady(skin_id, TypeSpecificID) then
                return old_UpdateWeaponAppearanceInfo(self, TypeSpecificID, BattleData, DragOrigin)
            end

            if self.__last_skin_applied == skin_id then
                return
            end
            self.__last_skin_applied = skin_id

            old_UpdateWeaponAppearanceInfo(self, skin_id, BattleData, DragOrigin)

            pcall(function()
                self.TypeSpecificIDTemp = TypeSpecificID
                self.ItemID = TypeSpecificID

                if self.UIRoot then
                    self.UIRoot.ItemID = TypeSpecificID
                    if self.UIRoot.TextBlock_WeaponName and ItemData.ItemName then
                        self.UIRoot.TextBlock_WeaponName:SetText(ItemData.ItemName)
                    end
                end

                if self.UpdateBullet then
                    self:UpdateBullet()
                end

                if self.UpdateWeaponAttachment then
                    self:UpdateWeaponAttachment()
                end
            end)
        end
        _G.BackpackSkinHooked = true
        skinProbeLog("Backpack WeaponInfoItemBase skin hook installed")
    end)
end

local function hookPutOn()
    pcall(function()
        local WRH = require("client.network.Protocol.WardRobeHandler")
        local o = WRH.send_depot_put_on_req
        WRH.send_depot_put_on_req = function(insID, extra)
            insID = tonumber(insID)
            if isInjectedIns(insID) then
                local resID = R.insToRes[insID]
                local c = cfg(resID)
                local st = subType(c)
                local kind = resolveClothKind(resID)
                if kind then
                    putOnCloth(insID)
                    return
                end
                if GUN_SUB[st] then
                    local wid = weaponIdFromSkin(resID)
                    if wid then equipWeaponSkin(wid, insID) end
                    return
                end
                if st == MELEE_ID then
                    equipWeaponSkin(MELEE_ID, insID)
                    return
                end
                if isVehicleSkinRes(resID) then
                    equipLobbyVehicleIns(insID)
                    return
                end
                local wd = require("client.slua.logic.wardrobe.wardrobe_data")
                local d = wd:GetHallDepotItemDataByInsID(insID)
                if d then
                    WRH.on_depot_put_on_rsp(NET_OK, { res_id = resID, count = 1, instid = insID }, nil, 1, insID, 0, extra)
                end
                return
            end
            return o(insID, extra)
        end
    end)
end

local function hookWeaponWear()
    pcall(function()
        local HT = require("client.logic.lobby.hall_theme_utils")
        local o = HT.IsWeaponWear
        HT.IsWeaponWear = function(insId)
            insId = tonumber(insId)
            if isInjectedIns(insId) then
                local c = cache()
                local Arm = require("client.logic.armory.logic_armory")
                for wid, w in pairs(c.weapons) do
                    if tonumber(w.insID) == insId then
                        if Arm.rsp_list and Arm.rsp_list.install_list and Arm.rsp_list.install_list[wid] then
                            return tonumber(Arm.rsp_list.install_list[wid].skin_id) == insId
                        end
                        return true
                    end
                end
            end
            return o(insId)
        end
    end)
end

local function hookCheckItemValid(comp)
    if not comp or not comp.CheckItemValid then return end
    local o = comp.CheckItemValid
    comp.CheckItemValid = function(self, resID)
        if isAllowedOutfitRes(resID) then return true end
        return o(self, resID)
    end
end

local function hookAvatarValid()
    pcall(function()
        hookCheckItemValid(require("GameLua.Mod.Library.GamePlay.Avatar.Component.CharacterAvatarComponent"))
    end)
    pcall(function()
        hookCheckItemValid(require("GameLua.Mod.Library.GamePlay.Avatar.Component.CharacterAvatarComponent2"))
    end)
end

local function isInRealMatch()
    if _G.isInPlayableGame then return _G.isInPlayableGame() end
    local ok, r = pcall(function()
        return GameStatus and GameStatus.IsInFightingStatus and GameStatus.IsInFightingStatus()
    end)
    return ok and r == true
end

local function getLocalChar()
    if _G.getMatchPlayerCharacter then
        local ch = _G.getMatchPlayerCharacter()
        if ch and slua.isValid(ch) then return ch end
    end
    local ok, GD = pcall(require, "GameLua.GameCore.Data.GameplayData")
    if not ok or not GD then return nil end
    local char = GD.GetPlayerCharacter()
    if char and slua.isValid(char) then return char end
    return nil
end

local function getWAC(char)
    local w = char and char.GetCurrentWeapon and char:GetCurrentWeapon()
    if slua.isValid(w) and slua.isValid(w.WeaponAvatarComponent) then
        return w.WeaponAvatarComponent
    end
    return nil
end

local function getDesiredOutfit()
    syncWeaponCacheFromLobby()
    local res = resolveLobbyOutfitRes()
    if res and res > 0 then return res end
    if MATCH_CONFIG.outfitRes and MATCH_CONFIG.outfitRes > 0 then
        return MATCH_CONFIG.outfitRes
    end
    return tonumber(_G.SuitSkin) or 0
end

local function matchApplyOutfit(char)
    if _G.applyMatchOutfitNow then
        return _G.applyMatchOutfitNow(true)
    end
    if not char or not slua.isValid(char) then return false end
    syncWeaponCacheFromLobby()
    local outfitRes = tonumber(getDesiredOutfit()) or 0
    pcall(function() if _G.ReadConfigFile then _G.ReadConfigFile(true) end end)
    if outfitRes > 0 then
        _G.SuitSkin = outfitRes
        _G.__ACTIVE_OUTFIT_RES = outfitRes
    end
    if outfitRes <= 0 then return false end
    pcall(function() if _G.download_item then _G.download_item(outfitRes) end end)

    local changed = false
    pcall(function()
        if _G.equip_character_avatar then
            changed = _G.equip_character_avatar(char) or changed
        end
    end)

    local comp = char.CharacterAvatarComp2_BP
    if slua.isValid(comp) then
        pcall(function()
            comp:PutOnCustomEquipmentByID(outfitRes)
            changed = true
        end)
        pcall(function()
            comp:HandleEquipItem(FItemDefineID(4, outfitRes), FAvatarCustomDefault())
            changed = true
        end)
    end

    if changed and not _G.__matchOutfitProbeDone then
        _G.__matchOutfitProbeDone = true
        skinProbeLog("match outfit " .. outfitRes .. " ok")
    end
    return changed
end

local _avatarItemsRegistered = false

local function getDesiredWeaponSkins()
    syncWeaponCacheFromLobby()
    local out, seen = {}, {}
    local function add(res)
        res = tonumber(res)
        if res and res > 0 and not seen[res] then seen[res] = true; out[#out+1] = res end
    end
    for wid, w in pairs(cache().weapons) do
        if wid ~= MELEE_ID and w.resID then add(w.resID) end
    end
    if MATCH_CONFIG.weaponSkins then
        for _, res in pairs(MATCH_CONFIG.weaponSkins) do add(res) end
    end
    return out
end

local GUN_MASTER_SYN_SLOT = 7

local function findSkinSlotInSynData(weapon)
    if not slua.isValid(weapon) then return GUN_MASTER_SYN_SLOT, 0 end
    local arr = weapon.synData
    if not arr or not slua.isValid(arr) then return GUN_MASTER_SYN_SLOT, 0 end
    local count = 0
    pcall(function() count = arr:Num() end)
    for i = 0, math.min(count - 1, 15) do
        local ok2, att = pcall(function() return arr:Get(i) end)
        if ok2 and att then
            local ok3, defRef = pcall(slua.IndexReference, att, "defineID")
            if ok3 and defRef then
                local tid = 0
                pcall(function() tid = tonumber(defRef.TypeSpecificID) or 0 end)
                if tid >= 1000000 then
                    return i, tid
                end
            end
        end
    end
    return GUN_MASTER_SYN_SLOT, 0
end

local function resolveWeaponTypeID(weaponResID)
    weaponResID = tonumber(weaponResID) or 0
    if weaponResID <= 0 then return 0 end
    local found = 0
    pcall(function()
        local wc = CDataTable.GetTableData("WeaponConfig", weaponResID)
        if wc then found = tonumber(wc.WeaponID or wc.WeaponId or wc.weaponID or 0) end
    end)
    if found > 0 then return found end
    pcall(function()
        local ic = CDataTable.GetTableData("Item", weaponResID)
        if ic then found = tonumber(ic.WeaponID or ic.weaponId or 0) end
    end)
    return found > 0 and found or weaponResID
end

local function findTargetSkinForWeaponRes(weaponResID)
    weaponResID = tonumber(weaponResID) or 0
    if weaponResID <= 0 then return nil end

    local memSkin = getMatchWeaponSkin(weaponResID)
    if memSkin then return memSkin end
    local typeID = resolveWeaponTypeID(weaponResID)
    if typeID > 0 and typeID ~= weaponResID then
        memSkin = getMatchWeaponSkin(typeID)
        if memSkin then return memSkin end
    end

    if MATCH_CONFIG.weaponSkins and MATCH_CONFIG.weaponSkins[weaponResID] then
        local fixed = tonumber(MATCH_CONFIG.weaponSkins[weaponResID])
        if fixed and fixed > 0 then return fixed end
    end

    for _, skinRes in ipairs(getDesiredWeaponSkins()) do
        local wid = weaponIdFromSkin(skinRes)
        if wid and tonumber(wid) == weaponResID then return skinRes end
    end

    local typeID = resolveWeaponTypeID(weaponResID)
    if typeID > 0 and typeID ~= weaponResID then
        if MATCH_CONFIG.weaponSkins and MATCH_CONFIG.weaponSkins[typeID] then
            local fixed = tonumber(MATCH_CONFIG.weaponSkins[typeID])
            if fixed and fixed > 0 then return fixed end
        end
        for _, skinRes in ipairs(getDesiredWeaponSkins()) do
            local wid = weaponIdFromSkin(skinRes)
            if wid and tonumber(wid) == typeID then return skinRes end
        end
    end

    local avatarMatch = nil
    pcall(function()
        local AU = import("AvatarUtils")
        local weaponBase = AU.GetWeaponAvatarParentID(AU.GetBPIDByResID(weaponResID), false)
        if not weaponBase or weaponBase <= 0 then return end
        for _, skinRes in ipairs(getDesiredWeaponSkins()) do
            local skinBase = AU.GetWeaponAvatarParentID(AU.GetBPIDByResID(skinRes), false)
            if skinBase and skinBase > 0 and skinBase == weaponBase then
                avatarMatch = skinRes
                return
            end
        end
    end)
    if avatarMatch then return avatarMatch end

    local c = cfg(weaponResID)
    local st = subType(c)
    if st and GUN_SUB[st] and MATCH_CONFIG.weaponSkins then
        for _, skinRes in pairs(MATCH_CONFIG.weaponSkins) do
            local skinWid = weaponIdFromSkin(skinRes)
            if skinWid then
                local sc = cfg(tonumber(skinWid))
                if sc and subType(sc) == st then return skinRes end
            end
            local sc = cfg(skinRes)
            if sc and GUN_SUB[subType(sc)] and subType(sc) == st then return skinRes end
        end
    end

    return nil
end

local function getSynMasterSkinID(weapon)
    if not slua.isValid(weapon) then return 0 end
    local id = 0
    pcall(function()
        local slot, tid = findSkinSlotInSynData(weapon)
        id = tid
        if id == 0 then
            local arr = weapon.synData
            if not arr or not slua.isValid(arr) then return end
            local att = arr:Get(GUN_MASTER_SYN_SLOT)
            if not att then return end
            id = slua.IndexReference(att, "defineID").TypeSpecificID or 0
        end
    end)
    return id
end

_G.AddOutfitSkinIdMappings = _G.AddOutfitSkinIdMappings or {}
_G.AddOutfitLastAppliedSkin = _G.AddOutfitLastAppliedSkin or {}

local function buildSkinMappings()
    syncWeaponCacheFromLobby()
    local m = _G.AddOutfitSkinIdMappings
    for k in pairs(m) do m[k] = nil end
    for wid, w in pairs(cache().weapons) do
        wid = tonumber(wid)
        if wid and w.resID and w.resID > 0 then
            m[wid] = { tonumber(w.resID) }
        end
    end
    if MATCH_CONFIG.weaponSkins then
        for weaponKey, skinRes in pairs(MATCH_CONFIG.weaponSkins) do
            weaponKey = tonumber(weaponKey)
            skinRes = tonumber(skinRes)
            if weaponKey and skinRes and skinRes > 0 and not m[weaponKey] then
                m[weaponKey] = { skinRes }
            end
        end
    end
end

local function get_skin_id(currentGunId, maxIt)
    currentGunId = tonumber(currentGunId) or 0
    maxIt = tonumber(maxIt) or 0
    if currentGunId <= 0 and maxIt <= 0 then return 0 end
    buildSkinMappings()
    if maxIt > 0 then
        local fromMem = getMatchWeaponSkin(maxIt)
        if fromMem then return fromMem end
    end
    local fromMem2 = getMatchWeaponSkin(resolveWeaponTypeID(currentGunId))
    if fromMem2 then return fromMem2 end
    local m = _G.AddOutfitSkinIdMappings
    if maxIt > 0 and m[maxIt] and m[maxIt][1] then return tonumber(m[maxIt][1]) end
    local list = m[currentGunId]
    if list and list[1] then return tonumber(list[1]) end
    local typeId = resolveWeaponTypeID(currentGunId)
    if typeId > 0 and m[typeId] and m[typeId][1] then return tonumber(m[typeId][1]) end
    local target = findTargetSkinForWeaponRes(maxIt > 0 and maxIt or currentGunId)
    if target then return target end
    return currentGunId
end

local function applySkinToWeaponRef(CurWeapon)
    if not slua.isValid(CurWeapon) then return false end
    local AttachmentArray = CurWeapon.synData
    if not AttachmentArray or not slua.isValid(AttachmentArray) then return false end

    local AttachmentData = AttachmentArray:Get(GUN_MASTER_SYN_SLOT)
    if not AttachmentData then return false end

    local current_gunid = 0
    pcall(function()
        current_gunid = slua.IndexReference(AttachmentData, "defineID").TypeSpecificID or 0
    end)
    if not current_gunid or current_gunid <= 0 then return false end

    local MaxIt = 0
    pcall(function()
        if CurWeapon.GetWeaponID then
            MaxIt = CurWeapon:GetWeaponID()
        end
        if MaxIt <= 0 then
            MaxIt = CurWeapon:GetItemDefineID().TypeSpecificID
        end
    end)
    MaxIt = tonumber(MaxIt) or 0
    local tmp_id = get_skin_id(current_gunid, MaxIt)
    tmp_id = tonumber(tmp_id) or 0
    if tmp_id <= 0 or MaxIt <= 0 then return false end
    if tmp_id == MaxIt and tmp_id == current_gunid then return true end

    local vWriteVals = _G.AddOutfitSkinIdMappings[MaxIt] or {}
    local isSkinValid = false
    local lastSkin = _G.AddOutfitLastAppliedSkin[MaxIt]
    if lastSkin then
        for _, writeVal in ipairs(vWriteVals) do
            if tonumber(writeVal) == lastSkin then
                isSkinValid = true
                break
            end
        end
    else
        for _, writeVal in ipairs(vWriteVals) do
            if tonumber(writeVal) == tmp_id then
                isSkinValid = true
                break
            end
        end
    end

    if not isSkinValid then
        local scopeID = 0
        pcall(function()
            if CurWeapon.GetScopeID then scopeID = CurWeapon:GetScopeID(false) or 0 end
        end)
        if scopeID > 0 then
            pcall(function()
                local scopeData = AttachmentArray:Get(4)
                if scopeData then
                    slua.IndexReference(scopeData, "defineID").TypeSpecificID = scopeID
                    AttachmentArray:Set(4, scopeData)
                end
            end)
        end
    end

    _G.AddOutfitLastAppliedSkin[current_gunid] = tmp_id

    if tmp_id ~= current_gunid then
        pcall(function()
            local defRef = slua.IndexReference(AttachmentData, "defineID")
            defRef.TypeSpecificID = tmp_id
            local c0 = cfg(tmp_id)
            if c0 and c0.ItemType and defRef.Type ~= nil then
                defRef.Type = c0.ItemType
            end
            AttachmentData.operationType = 0
            AttachmentArray:Set(GUN_MASTER_SYN_SLOT, AttachmentData)
        end)
        if CurWeapon.DelayHandleAvatarMeshChanged then
            CurWeapon:DelayHandleAvatarMeshChanged()
        end
        _G.AddOutfitLastAppliedSkin[MaxIt] = tmp_id
        return true
    end
    return false
end

function _G.equip_weapon_avatar(uCharacter)
    if not uCharacter or not slua.isValid(uCharacter) then return false end
    buildSkinMappings()
    local WeaponManager = uCharacter:GetWeaponManager()
    if not WeaponManager or not slua.isValid(WeaponManager) then return false end
    local uWeaponList = WeaponManager:GetAllInventoryWeaponList(false)
    if not uWeaponList or not slua.isValid(uWeaponList) then return false end

    local appliedAny = false
    for i = 0, uWeaponList:Num() - 1 do
        local CurWeapon = uWeaponList:Get(i)
        if slua.isValid(CurWeapon) and applySkinToWeaponRef(CurWeapon) then
            appliedAny = true
        end
    end
    return appliedAny
end

local function equipWeaponAvatarSynData(char)
    return _G.equip_weapon_avatar(char)
end

local applySkinToWeapon = applySkinToWeaponRef

local function registerWeaponAvatarItems(char)
    local pc = char.GetPlayerControllerSafety and char:GetPlayerControllerSafety()
    if not slua.isValid(pc) then
        return false
    end
    local AU = import("AvatarUtils")
    local BU = import("BackpackUtils")
    local addedCount = 0

    for _, resID in ipairs(getDesiredWeaponSkins()) do
        local doneDirect = false
        pcall(function()
            if pc.AddWeaponAvatarItem then
                pc:AddWeaponAvatarItem(tonumber(resID))
                doneDirect = true
                addedCount = addedCount + 1
            end
        end)
        if not doneDirect then
            pcall(function()
                local skinBPID = BU.GetBPIDByResID(tonumber(resID))
                local arr = slua.Array(UEnums.EPropertyClass.Int)
                local parents = AU.GetWeaponAvatarParentIDList(skinBPID, arr, false)
                if parents and parents.Num and parents:Num() > 0 and pc.WeaponAvatarItemList then
                    for _, parentID in pairs(parents) do
                        pc.WeaponAvatarItemList:Add(parentID, skinBPID)
                    end
                    addedCount = addedCount + 1
                end
            end)
        end
    end

    if addedCount == 0 then
        return false
    end

    pcall(function() if pc.InitWeaponAvatarItems then pc:InitWeaponAvatarItems() end end)
    pcall(function() if pc.OnWeaponAvatarUpdate then pc:OnWeaponAvatarUpdate() end end)
    return true
end

local function reloadCurrentWeaponAvatar(char)
    pcall(function()
        local weapon = char.GetCurrentWeapon and char:GetCurrentWeapon()
        if not slua.isValid(weapon) then return end
        local wac = weapon.WeaponAvatarComponent
        if slua.isValid(wac) then
            local ES = import("EWeaponAttachmentSocketType")
            pcall(function() wac:ClearMeshPathCacheBySlot(ES.MasterGun) end)
            pcall(function() wac:ClearMeshBySlot(ES.MasterGun, true, true) end)
        end
        if weapon.DelayHandleAvatarMeshChanged then
            weapon:DelayHandleAvatarMeshChanged()
        elseif slua.isValid(wac) and wac.ReloadAllEquippedAvatar then
            local ESlotDescDiff = import("ESlotDescDiff")
            wac:ReloadAllEquippedAvatar(ESlotDescDiff.MeshDiff)
        end
    end)
end

local _weaponDiagDone = false
local _weaponApplied = false
local _lastWeaponResID = 0
local _weaponSpawnHooked = false

local function onWeaponLuaInit(_, _, weapon)
    if not weapon or not slua.isValid(weapon) then return end
    local char = getLocalChar()
    if not char then return end
    local owner = nil
    pcall(function()
        if weapon.GetOwnerPawn then owner = weapon:GetOwnerPawn() end
    end)
    if not slua.isValid(owner) or owner ~= char then return end
    pcall(function()
        char:AddGameTimer(0.15, false, function()
            local c = getLocalChar()
            if c and slua.isValid(weapon) then
                applySkinToWeapon(weapon)
                _weaponApplied = false
            end
        end)
    end)
end

local function hookWeaponSpawn()
    if _weaponSpawnHooked then return end
    pcall(function()
        if EventSystem and EventSystem.registEvent and EVENTTYPE_PLAYEREVENT_WEAPON and EVENTID_PLAYEREVENT_WEAPON_LUA_INIT then
            EventSystem:registEvent(EVENTTYPE_PLAYEREVENT_WEAPON, EVENTID_PLAYEREVENT_WEAPON_LUA_INIT, onWeaponLuaInit)
            _weaponSpawnHooked = true
        end
    end)
end

local function matchApplyWeaponSkin(char)
    if not _avatarItemsRegistered then
        _avatarItemsRegistered = registerWeaponAvatarItems(char)
    end

    local curWeapon = char.GetCurrentWeapon and char:GetCurrentWeapon()
    if not slua.isValid(curWeapon) then return false end

    local curWeaponResID = 0
    pcall(function() curWeaponResID = curWeapon:GetItemDefineID().TypeSpecificID end)
    if curWeaponResID ~= _lastWeaponResID then
        _lastWeaponResID = curWeaponResID
        _weaponApplied = false
        _weaponDiagDone = false
    end

    if _weaponApplied then return true end

    local targetSkin = findTargetSkinForWeaponRes(curWeaponResID)
    local loadedSkin = 0
    pcall(function()
        local wac = getWAC(char)
        if wac then
            loadedSkin = wac.CachedLoadedID or 0
            if loadedSkin <= 0 then
                local ES = import("EWeaponAttachmentSocketType")
                loadedSkin = wac:GetEquippedItemDefineID(ES.MasterGun).TypeSpecificID or 0
            end
        end
    end)

    local synSkin = getSynMasterSkinID(curWeapon)
    if targetSkin and (loadedSkin == targetSkin or synSkin == targetSkin) then
        _weaponApplied = true
        return true
    end

    buildSkinMappings()
    local okSyn = applySkinToWeapon(curWeapon) or equipWeaponAvatarSynData(char)

    if not _weaponDiagDone then
        _weaponDiagDone = true
        local list = table.concat(getDesiredWeaponSkins(), ",")
        notify("Weapon: res=" .. tostring(curWeaponResID)
            .. " type=" .. tostring(resolveWeaponTypeID(curWeaponResID))
            .. " target=" .. tostring(targetSkin)
            .. " syn=" .. tostring(synSkin)
            .. " loaded=" .. tostring(loadedSkin)
            .. " ctrl=" .. tostring(_avatarItemsRegistered)
            .. " skins=[" .. list .. "]")
    end

    if okSyn and char.AddGameTimer then
        pcall(function()
            char:AddGameTimer(1.0, false, function()
                local c = getLocalChar()
                if not c then return end
                local w = c.GetCurrentWeapon and c:GetCurrentWeapon()
                if not slua.isValid(w) then return end
                local wac2 = w.WeaponAvatarComponent
                if not slua.isValid(wac2) then return end
                local cid = wac2.CachedLoadedID or 0
                local synId = getSynMasterSkinID(w)
                skinProbeLog("weapon verify syn=" .. tostring(synId) .. " cached=" .. tostring(cid) .. " target=" .. tostring(targetSkin))
                if targetSkin and (synId == targetSkin or cid == targetSkin) then
                    _weaponApplied = true
                end
            end)
        end)
    end

    return okSyn
end

local _matchTimer = nil
local _matchOutfitDone = false

local function startMatchWatcher(char)
    if _matchTimer then return end
    _matchOutfitDone = false
    _avatarItemsRegistered = false
    _weaponDiagDone = false
    _weaponApplied = false
    _lastWeaponResID = 0
    local elapsed = 0
    local outfitAttempts = 0

    _matchTimer = char:AddGameTimer(3.0, true, function()
        elapsed = elapsed + 3.0
        local cur = getLocalChar()
        if not cur or not slua.isValid(cur) then return end

        if not _matchOutfitDone and outfitAttempts < 8 then
            outfitAttempts = outfitAttempts + 1
            pcall(function()
                if _G.applyMatchOutfitNow then
                    _matchOutfitDone = _G.applyMatchOutfitNow(true) or _matchOutfitDone
                else
                    _matchOutfitDone = matchApplyOutfit(cur) or _matchOutfitDone
                end
            end)
        end
        if not _weaponApplied then
            matchApplyWeaponSkin(cur)
            pcall(function() _G.equip_weapon_avatar(cur) end)
        end

        if (_matchOutfitDone and _weaponApplied) or elapsed >= 30 then
            if _matchTimer and cur.RemoveGameTimer then
                pcall(function() cur:RemoveGameTimer(_matchTimer) end)
            end
            _matchTimer = nil
        end
    end)
end

local function stopMatchWatcher()
    if _matchTimer then
        pcall(function()
            local char = getLocalChar()
            if char and char.RemoveGameTimer then char:RemoveGameTimer(_matchTimer) end
        end)
        _matchTimer = nil
    end
    _matchOutfitDone = false
    _avatarItemsRegistered = false
    _weaponApplied = false
    _weaponDiagDone = false
    _lastWeaponResID = 0
end

local function hookPutOnRsp()
    pcall(function()
        local wl = require("client.slua.logic.wardrobe.logic_wardrobe_new")
        local o = wl.on_puton_rsp
        wl.on_puton_rsp = function(self, res, item, olditem, index, extra)
            o(self, res, item, olditem, index, extra)
            if not item or not item.instid then return end
            local resID = tonumber(item.res_id)
            local insID = tonumber(item.instid)
            if not resID or not insID then return end
            local c = cfg(resID)
            local st = subType(c)
            if resolveClothKind(resID) == "full_suit" and isLobbyOutfitRes(resID) then
                saveEquip(resID, insID)
            elseif GUN_SUB[st] then
                local wid = weaponIdFromSkin(resID)
                if wid then cacheWeaponSkinFromIns(wid, insID) end
            elseif st == MELEE_ID then
                cacheWeaponSkinFromIns(MELEE_ID, insID)
            elseif isVehicleSkinRes(resID) then
                rememberLobbyVehicleSkin(resID, insID)
                pcall(_G.scheduleLobbyConfigFlush)
            elseif isInjectedIns(insID) then
                saveEquip(resID, insID)
            end
            pcall(function() _G.syncLobbyItemToConfig(resID, weaponIdFromSkin(resID)) end)
        end
    end)
end

local function hookLobbyWeaponCache()
    pcall(function()
        local Arm = require("client.logic.armory.logic_armory")
        local oRsp = Arm.install_weapon_skin_rsp
        Arm.install_weapon_skin_rsp = function(client_data, errorCode, weapon_id, instanceID)
            oRsp(client_data, errorCode, weapon_id, instanceID)
            if errorCode == 0 or errorCode == NET_OK then
                cacheWeaponSkinFromIns(weapon_id, instanceID)
            end
        end
        local oH = Arm.HandleWeaponSkinChange
        Arm.HandleWeaponSkinChange = function(client_data, weapon_id, instanceID)
            oH(client_data, weapon_id, instanceID)
            cacheWeaponSkinFromIns(weapon_id, instanceID)
        end
    end)
    pcall(function()
        local wgl = require("client.slua.logic.wardrobe.logic_wardrobe_gun")
        local o = wgl.on_put_on_weapon_wear_rsp
        wgl.on_put_on_weapon_wear_rsp = function(self, client_data, res, weapon_id, new_skin_id, extra_weapon_list)
            o(self, client_data, res, weapon_id, new_skin_id, extra_weapon_list)
            if res == 0 or res == NET_OK then
                cacheWeaponSkinFromIns(weapon_id, new_skin_id)
            end
        end
    end)
    pcall(function()
        if not EventSystem or not EventSystem.registEvent then return end
        if EVENTTYPE_WARDROBE and EVENTID_WARDROBE_UPDATE_CURRENT_PUT_ON_GUN then
            EventSystem:registEvent(EVENTTYPE_WARDROBE, EVENTID_WARDROBE_UPDATE_CURRENT_PUT_ON_GUN, function(_, _, resOrFlag, weapon_id)
                weapon_id = tonumber(weapon_id)
                if weapon_id and weapon_id > 0 then
                    pcall(function()
                        local wgl = require("client.slua.logic.wardrobe.logic_wardrobe_gun")
                        local insID = tonumber(wgl:GetSkinIdByWeaponID(weapon_id)) or 0
                        if insID > 0 then cacheWeaponSkinFromIns(weapon_id, insID) end
                    end)
                elseif tonumber(resOrFlag) and tonumber(resOrFlag) > 100000 then
                    pcall(function()
                        local wid = weaponIdFromSkin(resOrFlag)
                        if wid then
                            local wd = require("client.slua.logic.wardrobe.wardrobe_data")
                            local ins = wd.GetWardrobeInsIdByResId and wd:GetWardrobeInsIdByResId(resOrFlag)
                            if ins and ins > 0 then cacheWeaponSkinFromIns(wid, ins) end
                        end
                    end)
                end
            end)
        end
    end)
end

local function hookWardrobePutOnReq()
    pcall(function()
        local wl = require("client.slua.logic.wardrobe.logic_wardrobe_new")
        local o = wl.wardrobe_puton_req
        wl.wardrobe_puton_req = function(self, insID, extra)
            insID = tonumber(insID)
            if isInjectedIns(insID) then
                local resID = R.insToRes[insID]
                local c = cfg(resID)
                local st = subType(c)
                if resolveClothKind(resID) then
                    putOnCloth(insID)
                    return
                end
            end
            return o(self, insID, extra)
        end
    end)
end

local _bootstrapNotified = false

local function bootstrapMatch(char)
    char = char or getLocalChar()
    if not char or not slua.isValid(char) then return false end
    pcall(function() loadMatchConfigFromIni(true) end)
    pcall(function() if _G.ReadConfigFile then _G.ReadConfigFile(true) end end)
    syncWeaponCacheFromLobby()
    _G.__matchOutfitProbeDone = nil
    _weaponApplied = false
    _weaponDiagDone = false
    _matchApplied = false
    if not _bootstrapNotified then
        _bootstrapNotified = true
        local cch = cache()
        local w = cch.weapons[101004]
    end
    startMatchWatcher(char)
    pcall(function()
        if _G.bootstrapSrcHubIngame then _G.bootstrapSrcHubIngame() end
    end)
    return true
end

local function hookMatchAvatar()
    pcall(function()
        local CAC = require("GameLua.Mod.Library.GamePlay.Avatar.Component.CharacterAvatarComponent")
        local o = CAC.OnAvatarAllMeshLoadedLua
        CAC.OnAvatarAllMeshLoadedLua = function(self)
            o(self)
            pcall(function()
                if self.IsLobbyActor and self:IsLobbyActor() then return end
                local isSelf = self.IsSelf and self:IsSelf()
                if not isSelf then return end
                if _G.isInLobby and _G.isInLobby() then return end
                _G.__PLAYABLE_GAME_ACTIVE = true
                if _G.bootstrapSrcHubIngame then _G.bootstrapSrcHubIngame() end
                local outfitRes = tonumber(getDesiredOutfit()) or 0
                if outfitRes > 0 and _G.applySuitToMatchComp then
                    local ok = _G.applySuitToMatchComp(self, outfitRes)
                    if ok then
                        _G.__lastAppliedMatchOutfit = outfitRes
                        _G.__matchOutfitOk = true
                        skinProbeLog("mesh outfit apply shirt=" .. outfitRes .. " ok=true")
                    end
                end
                local char = getLocalChar()
                if char and char.AddGameTimer then
                    char:AddGameTimer(0.5, false, function()
                        bootstrapMatch(char)
                        if _G.applyMatchOutfitNow then _G.applyMatchOutfitNow(true) end
                    end)
                end
            end)
        end
    end)
    pcall(function()
        local CAC2 = require("GameLua.Mod.Library.GamePlay.Avatar.Component.CharacterAvatarComponent2")
        if not CAC2 or not CAC2.OnAvatarAllMeshLoadedLua then return end
        local o2 = CAC2.OnAvatarAllMeshLoadedLua
        CAC2.OnAvatarAllMeshLoadedLua = function(self)
            o2(self)
            pcall(function()
                if self.IsLobbyActor and self:IsLobbyActor() then return end
                local isSelf = self.IsSelf and self:IsSelf()
                if not isSelf then return end
                if _G.isInLobby and _G.isInLobby() then return end
                _G.__PLAYABLE_GAME_ACTIVE = true
                if _G.bootstrapSrcHubIngame then _G.bootstrapSrcHubIngame() end
                local outfitRes = tonumber(getDesiredOutfit()) or 0
                if outfitRes > 0 and _G.applySuitToMatchComp then
                    local ok = _G.applySuitToMatchComp(self, outfitRes)
                    if ok then
                        _G.__lastAppliedMatchOutfit = outfitRes
                        _G.__matchOutfitOk = true
                        skinProbeLog("mesh outfit apply shirt=" .. outfitRes .. " ok=true")
                    elseif _G.applyMatchOutfitNow then
                        _G.applyMatchOutfitNow(true)
                    end
                elseif _G.applyMatchOutfitNow then
                    _G.applyMatchOutfitNow(true)
                end
            end)
        end
    end)
    pcall(function()
        local WAC = require("GameLua.Mod.Library.GamePlay.Avatar.Component.WeaponAvatarComponent")
        local oLoad = WAC.OnWeaponAvatarLoadedLua
        WAC.OnWeaponAvatarLoadedLua = function(self, slotID, definedID)
            oLoad(self, slotID, definedID)
            pcall(function()
                if self.IsLobbyActor and self:IsLobbyActor() then return end
                local isSelf = self.IsSelf and self:IsSelf()
                if not isSelf then return end
                local char = getLocalChar()
                if not char then return end
                bootstrapMatch(char)
                _weaponApplied = false
                if char.AddGameTimer then
                    char:AddGameTimer(0.2, false, function()
                        local c = getLocalChar()
                        if c then matchApplyWeaponSkin(c) end
                    end)
                end
            end)
        end
    end)
end

local function hookEnterGame()
    pcall(function()
        if EventSystem and EventSystem.registEvent and EVENTTYPE_LOBBY and EVENTID_ENTER_GAME_BEGIN then
            EventSystem:registEvent(EVENTTYPE_LOBBY, EVENTID_ENTER_GAME_BEGIN, function()
                pcall(_G.flushLobbySelectionsToConfig)
                syncWeaponCacheFromLobby()
                stopMatchWatcher()
                _bootstrapNotified = false
                _G.__PLAYABLE_GAME_ACTIVE = true
                _G.__SRC_HUB_STARTED = false
                _G.__srcHubRetryTimer = nil
                _G.__matchOutfitApplyLogged = nil
                _G.__matchOutfitOk = nil
                _G.__lastAppliedMatchOutfit = nil
                _G.__matchCharMissLogged = nil
                _G.__avatarCompMissLogged = nil
                _G.__outfitZeroLogged = nil
                _G.__matchOutfitProbeDone = nil
                pcall(function()
                    if _G.invalidateMatchOutfitCache then _G.invalidateMatchOutfitCache('enter game') end
                end)
                local outfitRes = tonumber(_G.AddOutfitLastLobbyOutfitRes)
                    or tonumber(_G.__ACTIVE_OUTFIT_RES) or tonumber(MATCH_CONFIG.outfitRes) or 0
                if outfitRes > 0 and _G.getOutfitInsId then
                    local ins = tonumber(_G.getOutfitInsId(outfitRes)) or 0
                    if ins > 0 then _G.AddOutfitLastLobbyOutfitIns = ins end
                end
                skinProbeLog("enter game: training/match session started outfit="
                    .. tostring(outfitRes) .. " ins=" .. tostring(_G.AddOutfitLastLobbyOutfitIns or 0))
                local function tryBootstrap(delay)
                    later(delay, function()
                        pcall(function()
                            if _G.bootstrapSrcHubIngame then _G.bootstrapSrcHubIngame() end
                            if _G.scheduleSrcHubRetry then _G.scheduleSrcHubRetry() end
                            local char = getLocalChar()
                            if char then bootstrapMatch(char) end
                            if outfitRes > 0 and _G.getOutfitInsId then
                                local ins = tonumber(_G.getOutfitInsId(outfitRes)) or 0
                                if ins > 0 then _G.AddOutfitLastLobbyOutfitIns = ins end
                            end
                            if _G.applyMatchOutfitNow then _G.applyMatchOutfitNow(true) end
                            if _G.applyUltimateMatchOutfit then _G.applyUltimateMatchOutfit() end
                        end)
                    end)
                end
                tryBootstrap(0.5)
                tryBootstrap(2.0)
                tryBootstrap(5.0)
                tryBootstrap(10.0)
            end)
        end
        if EventSystem and EventSystem.registEvent and EVENTTYPE_LOBBY and EVENTID_SWITCHTO_PAGE_END then
            EventSystem:registEvent(EVENTTYPE_LOBBY, EVENTID_SWITCHTO_PAGE_END, function(_, _, _, toPage)
                if ENUM_LobbyPageType and toPage == ENUM_LobbyPageType.Mid then
                    _G.__PLAYABLE_GAME_ACTIVE = false
                    _G.__SRC_HUB_STARTED = false
                    _G.__srcHubRetryTimer = nil
                    _G.__lastAppliedLobbyVehicle = nil
                    _G.__configOutfitApplied = nil
                    _G.__outfitProbeDone = nil
                    pcall(function()
                        if _G.invalidateMatchOutfitCache then _G.invalidateMatchOutfitCache('back to lobby') end
                    end)
                    pcall(_G.scheduleLobbyConfigFlush)
                    _G.__needsLobbyOutfitRefresh = true
                    skinProbeLog("back to lobby: playable session cleared")
                    local function restoreLobbyOutfit(delay)
                        later(delay, function()
                            if not GameStatus or not GameStatus.IsInLobbyOrMainCity
                                or not GameStatus.IsInLobbyOrMainCity() then
                                return
                            end
                            pcall(refreshLobbyOutfitVisual, true)
                            pcall(reapplyLobbyEquipped)
                            pcall(function()
                                if _G.GameAvatarHandlerplayers then _G.GameAvatarHandlerplayers() end
                            end)
                        end)
                    end
                    restoreLobbyOutfit(0.4)
                    restoreLobbyOutfit(1.5)
                    restoreLobbyOutfit(3.0)
                end
            end)
        end
    end)
end

local function onInjectReady()
    _G.__configOutfitApplied = nil
    refreshWardrobe()
    local function applyLobbyOutfitVisual()
        pcall(applySavedLobbyFull)
        pcall(reapplyLobbyEquipped)
        pcall(function()
            _G.__outfitProbeDone = nil
            if _G.startSrcOutfitTimers then _G.startSrcOutfitTimers() end
            if _G.GameAvatarHandlerplayers then _G.GameAvatarHandlerplayers() end
        end)
    end
    later(0.35, applyLobbyOutfitVisual)
    later(1.0, function()
        applyLobbyOutfitVisual()
        pcall(_G.scheduleLobbyConfigFlush)
    end)
    later(3.0, function()
        applyLobbyOutfitVisual()
        pcall(_G.scheduleLobbyConfigFlush)
    end)
    later(5.0, applyLobbyOutfitVisual)
    pcall(function() scheduleConfigOutfitRetry(1) end)
end

function _G.ensureSkinTimers()
    if _G.__SKIN_BOOT_DONE then return end
    local now = os.clock()
    if (_G.__lastEnsureTick or 0) > 0 and (now - _G.__lastEnsureTick) < 2.5 then return end
    _G.__lastEnsureTick = now
    loadMatchConfigFromIni(true)
    local entity = getEntity()
    if not entity or not entity.bInit then
        skinProbeLog("ensureSkinTimers: wardrobe not ready (retry 2s)")
        later(2.0, function() _G.ensureSkinTimers() end)
        return
    end
    skinProbeLog("ensureSkinTimers: injecting")
    if injectAll(entity) then
        _G.__SKIN_BOOT_DONE = true
        onInjectReady()
        pcall(function()
            if isInRealMatch and isInRealMatch() and _G.bootstrapSrcHubIngame then
                _G.bootstrapSrcHubIngame()
            end
        end)
    end
end

function _G.bootstrapUltimateSkin()
    local now = os.clock()
    if (_G.__lastBootstrapTick or 0) > 0 and (now - _G.__lastBootstrapTick) < 2.5 then return end
    _G.__lastBootstrapTick = now
    loadMatchConfigFromIni(true)
    if not _G.__SKIN_BOOT_DONE then
        local entity = getEntity()
        if entity and entity.bInit and injectAll(entity) then
            _G.__SKIN_BOOT_DONE = true
            onInjectReady()
        else
            skinProbeLog("bootstrapUltimateSkin: waiting for wardrobe (retry 2s)")
            later(2.0, _G.bootstrapUltimateSkin)
            return
        end
    end
    applyConfiguredLobbySkins()
    if GameStatus and GameStatus.IsInLobbyOrMainCity and GameStatus.IsInLobbyOrMainCity() then
        reapplyLobbyEquipped()
        onSocialWearDirty(true)
    end
end

local function start()
    _G.__SKIN_PATCHED = true
    skinProbeLog("UltimateEngine Outfit Script v3 start")
    notify("Ultimate Engine Active")
    loadMatchConfigFromIni(true)
    buildSkinMappings()
    _G.get_skin_id = get_skin_id
    _G.skinIdMappings = _G.AddOutfitSkinIdMappings
    hookDepotInit()
    hookWardrobeData()
    hookPageFilter()
    hookArmory()
    hookGunSkinId()
    hookBackpackWeaponAppearance()
    hookPutOn()
    hookWeaponWear()
    hookAvatarValid()
    hookPutOnRsp()
    hookLobbyWeaponCache()
    hookLobbySwipePersistence()
    hookLobbyThemePersist()
    hookLobbyVehiclePersist()
    hookWardrobePutOnReq()
    hookMatchAvatar()
    hookWeaponSpawn()
    hookEnterGame()

    pcall(function()
        if EventSystem and EventSystem.registEvent and EVENTTYPE_LOBBY and EVENTID_SWITCHTO_PAGE_END then
            EventSystem:registEvent(EVENTTYPE_LOBBY, EVENTID_SWITCHTO_PAGE_END, function(_, _, _, toPage)
                if ENUM_LobbyPageType and toPage == ENUM_LobbyPageType.Mid then
                    later(1.0, bootstrapUltimateSkin)
                end
            end)
        end
    end)

    pcall(function()
        if isInRealMatch() then
            local char = getLocalChar()
            if char then
                notify("Script injected in match - Starting application")
                bootstrapMatch(char)
            elseif _G.bootstrapSrcHubIngame then
                _G.bootstrapSrcHubIngame()
            end
        end
    end)

    if injectAll() then
        onInjectReady()
    else
        local tries = 0
        local function retry()
            tries = tries + 1
            if injectAll() then
                onInjectReady()
                _G.__SKIN_BOOT_DONE = true
                return
            end
            if tries < 40 then later(1.5, retry) end
        end
        later(1.5, retry)
    end
    later(3.0, function()
        local function tick()
            checkConfigReloadStamp()
            if loadMatchConfigFromIni(false) and GameStatus
                and GameStatus.IsInLobbyOrMainCity and GameStatus.IsInLobbyOrMainCity() then
                applyConfiguredLobbySkins()
            end
            later(3.0, tick)
        end
        tick()
    end)
    skinProbeLog("UltimateEngine hooks installed")
end
-- ==================== Anubis (in-game module) ====================

(function() -- Anubis scope (Lua 200 local limit per function)
local skinProbeLog = _G.skinProbeLog or function(...) end

_G.skinIdMappings = {
    [101004]={101004,1101004046,1101004226,1101004236,1101004062,1101004078,1101004086,1101004098,1101004138,1101004163,1101004201,1101004209,1101004218},
    [101001]={101001,1101001089,1101001213,1101001172,1101001127,1101001142,1101001153,1101001115,1101001102,1101001230,1101001241},
    [101003]={101003,1103003208,1101003195,1101003187,1101003098,1101003166,1101003069,1101003218,1101003079,1101003118,1101003145,1101003180,1101003056},
    [103001]={103001,1103001191,1103001101,1103001178,1103001145},
    [102002]={102002,1102002136,1102002043,1102002061,1102002424},
    [103002]={103002,1103002030,1103002087,1103002105,1103002112},
    [103003]={103003,1103003042,1103003087,1103003062,1103003022,1103003051,1103003030,1103003079},
    [101008]={101008,1101008079,1101008126,1101008104,1101008146,1101008026,1101008061,1101008116,1101008051},
    [102003]={102003,1102003019,1102003030,1102003064,1102003079},
    [105010]={105010,1105010018,1105010007},
    [102004]={102004,1102004017,1102004033},
    [105002]={105002,1105002090,1105002075,1105002018,1105002034,1105002057,1105002062},
    [105001]={105001,1105001047,1105001068,1105001033,1105001061},
    [101006]={101006,1101006061,1101006074,1101006043,1101006032,1101006084},
    [104004]={104004,1104004034,1104004015,1104004040}
}
_G.skinIdMappings2 = _G.skinIdMappings
_G.WeaponSkinMap = _G.skinIdMappings
_G.WeaponSkinIndex = _G.WeaponSkinIndex or {}
_G.skinIdCache2 = _G.skinIdCache2 or {}
_G.killCountInfo = _G.killCountInfo or {}
_G.lastFileContent = ""
_G.isFileWatcherActive = false
_G.WelcomeShown = false
_G.MatchEndMessageShown = false
_G.DeadBoxSkins = _G.DeadBoxSkins or {}
_G.AlreadyChangedSet = _G.AlreadyChangedSet or {}
_G.LastKillTime = {}
_G.TargetLobbyThemeID = 202408001
_G.LastAppliedThemeID = nil
_G.LastBackApplyValue = 0
_G.CurrentBagApplicationValue = 0
_G.LastHelmetApplyValue = 0
_G.CurrentHelmetApplicationValue = 0
_G.OutfitIndex = _G.OutfitIndex or {Suit=1,Bag=1,Helmet=1,Parachut=1,Pet=1}
_G.LastAppliedPet = 0
_G.skinIdCache = _G.skinIdCache or {}
_G.g_parts = _G.g_parts or {}
_G.lastAppliedAttachments = _G.lastAppliedAttachments or {}
_G.lastAppliedSkin = _G.lastAppliedSkin or {}
_G.CurrentEquipVehicleID = 0

-- Anubis in-game vehicle skins (lobby = Ultimate wardrobe inject)
_G.VehskinIdMappings = _G.VehskinIdMappings or {}
_G.VehicleSkinIndex = _G.VehicleSkinIndex or {}
_G.LobbyVehicleSkin = _G.LobbyVehicleSkin or 0

-- In-match default spawn IDs (raw, no skin) — user verified BGMI
_G.VehicleDefaultSpawnIDs = {
    UAZ=1908001, Buggy=1907001, Dacia=1903001, CoupeRB=1961001,
}

-- config.ini key -> avatar ID prefix (must match spawn ID start digits)
_G.VehicleBaseIDMap = {
    UAZ=19080,      -- spawn 1908001, skins 1908075...
    Dacia=1903,     -- spawn 1903001
    Buggy=19070,    -- spawn 1907001, skins 1907054...
    CoupeRB=19610,  -- spawn 1961001, skins 1961040...
    Bike=19040, Trike=19050,
    Boat=19100, Jet=19110, Scooter=19120, Bus=19130, Mirado=19150,
    Rony=19530, Snowbike=19540, Snowmobile=19550, Tempo=19560,
    Truck=19570, MonsterTruck=19580, BRDM=19590, ATV=19600,
    UTV=19620, Motorglider=19660, PG117=19160,
}

function _G.ResolveVehicleBaseFromSkinId(skinId)
    skinId = tonumber(skinId)
    if not skinId or skinId <= 0 then return nil end

    local fromTable
    pcall(function()
        local m = CDataTable and CDataTable.GetTableData
            and CDataTable.GetTableData("VehicleSkinMapping", skinId)
        if not m then return end
        local vid = tonumber(m.VehicleID or m.VehicleId or m.DefaultVehicleID or m.VehicleAvatarID)
        if vid and vid > 0 then fromTable = vid end
    end)
    if fromTable then return fromTable end

    pcall(function()
        local ic = CDataTable and CDataTable.GetTableData and CDataTable.GetTableData("Item", skinId)
        if ic then
            local vid = tonumber(ic.VehicleID or ic.VehicleId or ic.RelatedVehicleID)
            if vid and vid > 0 then fromTable = vid end
        end
    end)
    if fromTable then return fromTable end

    local skinStr = tostring(skinId)
    local best, bestLen = nil, 0
    for _, base in pairs(_G.VehicleBaseIDMap) do
        base = tonumber(base)
        if base then
            local bs = tostring(base)
            if skinStr:sub(1, #bs) == bs and #bs > bestLen then
                best, bestLen = base, #bs
            end
        end
    end
    return best
end

function _G.UpdateVehicleSkinMappings(newConfig)
    if not newConfig then return end
    _G.VehskinIdMappings = {}
    _G.VehicleSkinIndex = {}

    local function setVehicleSkin(baseKey, skinRes)
        skinRes = tonumber(skinRes)
        if not skinRes or skinRes <= 0 then return end
        local base = _G.VehicleBaseIDMap[baseKey]
        if not base then
            base = _G.ResolveVehicleBaseFromSkinId(skinRes)
        end
        if not base then return end
        _G.VehskinIdMappings[base] = { skinRes }
        _G.VehicleSkinIndex[base] = skinRes
        if _G.download_item and not (_G.skinIdCache and _G.skinIdCache[skinRes]) then
            pcall(_G.download_item, skinRes)
            _G.skinIdCache = _G.skinIdCache or {}
            _G.skinIdCache[skinRes] = true
        end
    end

    for configKey, basePrefix in pairs(_G.VehicleBaseIDMap) do
        local skinRes = newConfig[configKey]
        if skinRes and skinRes ~= 0 then
            setVehicleSkin(configKey, skinRes)
        end
    end

    local generic = newConfig.LOBBY_VEHICLE or newConfig.VEHICLE or newConfig.CAR
    if generic and generic ~= 0 then
        _G.LobbyVehicleSkin = generic
        setVehicleSkin(nil, generic)
    end

    local n = 0
    for _ in pairs(_G.VehskinIdMappings) do n = n + 1 end
    if _G.skinProbeLog and n ~= (_G.__lastVehicleMapCount or -1) then
        _G.__lastVehicleMapCount = n
        _G.skinProbeLog("vehicle mappings loaded=" .. tostring(n))
    end
end

function _G.FindVehicleSkinForDefault(defaultAvatarId)
    defaultAvatarId = tonumber(defaultAvatarId)
    if not defaultAvatarId or defaultAvatarId <= 0 then return nil end

    if _G.VehicleSkinIndex[defaultAvatarId] then
        return _G.VehicleSkinIndex[defaultAvatarId]
    end

    local defStr = tostring(defaultAvatarId)
    local bestSkin, bestLen = nil, 0
    for basePrefix, skins in pairs(_G.VehskinIdMappings) do
        local bp = tostring(basePrefix)
        if #bp > 0 and defStr:sub(1, #bp) == bp then
            local skin = skins and skins[1]
            if skin and #bp > bestLen then
                bestSkin, bestLen = skin, #bp
            end
        end
    end
    if bestSkin then return bestSkin end

    for basePrefix, skinRes in pairs(_G.VehicleSkinIndex) do
        local bp = tostring(basePrefix)
        if #bp > 0 and defStr:sub(1, #bp) == bp then return skinRes end
    end

    return nil
end

function _G.Game_Vehicle_Avatar_Change(uCharacter)
    if not uCharacter or not slua.isValid(uCharacter) then return false end
    local ok = false
    pcall(function()
        local CurrentVehicle = uCharacter.CurrentVehicle
        if not CurrentVehicle or not slua.isValid(CurrentVehicle) then return end

        local VehicleAvatar = CurrentVehicle.VehicleAvatar
        if not VehicleAvatar then return end

        VehicleAvatar.curSwitchEffectId = 7303001
        local defaultId = 0
        pcall(function() defaultId = VehicleAvatar:GetDefaultAvatarID() or 0 end)
        local currentId = 0
        pcall(function() currentId = CurrentVehicle:GetAvatarId() or 0 end)

        local targetSkin = _G.FindVehicleSkinForDefault(defaultId)
        if not targetSkin or targetSkin <= 0 then
            if _G.skinProbeLog then
                _G.skinProbeLog("vehicle no skin for defaultId=" .. tostring(defaultId) .. " (add Buggy/Dacia=skinId in config.ini)")
            end
            return
        end
        if currentId == targetSkin then
            _G.CurrentEquipVehicleID = targetSkin
            ok = true
            return
        end

        if _G.download_item then
            pcall(_G.download_item, targetSkin)
            _G.skinIdCache = _G.skinIdCache or {}
            _G.skinIdCache[targetSkin] = true
        end

        VehicleAvatar:ChangeItemAvatar(targetSkin, true)
        _G.CurrentEquipVehicleID = targetSkin
        ok = true
        if _G.skinProbeLog then
            local vname = "?"
            for name, spawnId in pairs(_G.VehicleDefaultSpawnIDs or {}) do
                if tonumber(spawnId) == tonumber(defaultId) then vname = name break end
            end
            _G.skinProbeLog("vehicle skin " .. vname .. " " .. tostring(defaultId) .. " -> " .. tostring(targetSkin))
        end
    end)
    return ok
end

function _G.applyLobbyGarageVehicleSkin(skinRes)
    skinRes = tonumber(skinRes) or tonumber(_G.LobbyVehicleSkin) or 0
    if skinRes <= 0 then return false end
    if _G.__lastGarageVehicleApplied == skinRes then return true end
    local ok = false
    pcall(function()
        if _G.download_item then
            pcall(_G.download_item, skinRes)
            _G.skinIdCache = _G.skinIdCache or {}
            _G.skinIdCache[skinRes] = true
        end
        local UIUtil = require("client.common.ui_util")
        local gi = UIUtil and UIUtil.GetGameInstance and UIUtil.GetGameInstance()
        if not gi then return end
        local UGameplayStatics = import("GameplayStatics")
        local uActor = import("Actor")
        for _, className in ipairs({ "STExtraLobbyVehicle", "STExtraVehicleBase", "UAEVehicle" }) do
            pcall(function()
                local cls = import(className)
                if not cls then return end
                local arr = UGameplayStatics.GetAllActorsOfClass(
                    gi, cls, slua.Array(UEnums.EPropertyClass.Object, uActor))
                if not arr then return end
                for _, actor in pairs(arr) do
                    if slua.isValid(actor) then
                        local va = actor.VehicleAvatar
                        if va and va.ChangeItemAvatar then
                            va:ChangeItemAvatar(skinRes, true)
                            ok = true
                        end
                    end
                end
            end)
        end
    end)
    if ok then _G.__lastGarageVehicleApplied = skinRes end
    return ok
end

function _G.GameAvatarHandlervehicles()
    local PlayerController = slua_GameFrontendHUD and slua_GameFrontendHUD:GetPlayerController()
    if not PlayerController then return end
    local uCharacter = PlayerController:GetPlayerCharacterSafety()
    if uCharacter and _G.Game_Vehicle_Avatar_Change then
        _G.Game_Vehicle_Avatar_Change(uCharacter)
    end
end

function _G.hookVehicleEnterEvent()
    if _G.__VEHICLE_ENTER_HOOKED then return end
    _G.__VEHICLE_ENTER_HOOKED = true
    pcall(function()
        if not EventSystem or not EventSystem.registEvent then return end
        if EVENTTYPE_INGAME and EVENTID_INGAME_ON_ENTER_VEHICLE then
            EventSystem:registEvent(EVENTTYPE_INGAME, EVENTID_INGAME_ON_ENTER_VEHICLE, function()
                pcall(function()
                    local pc = slua_GameFrontendHUD and slua_GameFrontendHUD:GetPlayerController()
                    local char = pc and pc:GetPlayerCharacterSafety()
                    if char and char.AddGameTimer then
                        char:AddGameTimer(0.15, false, function()
                            local c = pc:GetPlayerCharacterSafety()
                            if c then _G.Game_Vehicle_Avatar_Change(c) end
                        end)
                    elseif char then
                        _G.Game_Vehicle_Avatar_Change(char)
                    end
                end)
            end)
        end
    end)
    pcall(function()
        local VC = require("GameLua.Mod.Library.GamePlay.Vehicle.VehicleComponent")
        if not VC or not VC.__inner_impl then return end
        local o = VC.__inner_impl.OnEnterVehicle
        if not o then return end
        VC.__inner_impl.OnEnterVehicle = function(self, ...)
            local r = o(self, ...)
            pcall(function()
                local char = getSkinPlayerCharacter and getSkinPlayerCharacter()
                    or (slua_GameFrontendHUD and slua_GameFrontendHUD:GetPlayerController()
                        and slua_GameFrontendHUD:GetPlayerController():GetPlayerCharacterSafety())
                if char then _G.Game_Vehicle_Avatar_Change(char) end
            end)
            return r
        end
    end)
end

_G.SuitSkin = 0
_G.HatSkin = 0
_G.FaceSkin = 0
_G.MaskSkin = 0
_G.GlovesSkin = 0
_G.PantSkin = 0
_G.ShoeSkin = 0
_G.ParachuteSkin = 0
_G.GliderSkin = 0
_G.BackpackSkin = 0
_G.HelmetSkin = 0
_G.Backpack1Skin = 0
_G.Backpack2Skin = 0
_G.Backpack3Skin = 0
_G.Helmet1Skin = 0
_G.Helmet2Skin = 0
_G.Helmet3Skin = 0
_G.Emote1Skin = 0
_G.Emote2Skin = 0
_G.Emote3Skin = 0
_G.PetSkin = 0
_G.HallEffectSkin = 0

_G.muzzles = {
    id_flash_hider = { 201010, 201005, 201004 },
    id_compensator = { 201009, 201003, 201002 },
    id_suppressor = { 201011, 201006, 201007 }
}
_G.foregrips = {
    id_Angledforegrip = 202001, id_thumb_grip = 202006, id_vertical_grip = 202002,
    id_light_grip = 202004, id_half_grip = 202005, id_ergonomic_grip = 202051, id_laser_sight = 202007
}
_G.magazines = {
    id_expanded_mag = { 204011, 204007, 204004 }, id_quick_mag = { 204012, 204008, 204005 },
    id_expanded_quick_mag = { 204013, 204009, 204006 }
}
_G.scopes = {
    id_reddot = 203001, id_holo = 203002, id_2x = 203003, id_3x = 203014,
    id_4x = 203004, id_6x = 203015, id_8x = 203005
}
_G.stock = {
    id_microStock = 205001, id_tactical = 205002, id_bulletloop = 204014, id_CheekPad = 205003
}

_G.CustSlotType = {
    NONE = 0, HeadEquipemtSlot = 1, HairEquipemtSlot = 2, HatEquipemtSlot = 3,
    FaceEquipemtSlot = 4, ClothesEquipemtSlot = 5, PantsEquipemtSlot = 6,
    ShoesEquipemtSlot = 7, BackpackEquipemtSlot = 8, HelmetEquipemtSlot = 9,
    ArmorEquipemtSlot = 10, ParachuteEquipemtSlot = 11, GlassEquipemtSlot = 12,
    NightVisionEquipemtSlot = 13, BeardEquipemtSlot = 14, GlideEquipemtSlot = 15,
    HandEffectEquipemtSlot = 16, BackPack_PendantSlot = 17, MechaChestSlot = 18,
    MechaArmSlot = 19, MechaLegSlot = 20, MechaInnerSuitSlot = 21,
    FootEffectEquipemtSlot = 22, MaxSlotNum = 23, VehicleCut = 24,
    UnderClothSlot = 25, UnderPantsSlot = 26, SimpleSlotMax = 27, MAX = 28
}

local lastConfig = {}
local ItemUpgradeSystem = nil

-- ============================================================
-- [NEW] SkinAttachmentMap: skinID -> { attachmentName -> attachmentID }
-- Ye attachments.txt se load hoga
-- ============================================================
_G.SkinAttachmentMap = _G.SkinAttachmentMap or {}

local function LoadAttachmentFile(configPath)
    -- config.ini ke saath wali directory mein attachments.txt dhundho
    local dir = configPath:match("^(.*)/[^/]+$") or ""
    local attachPath = dir .. "/attachments.txt"
    
    local file = io.open(attachPath, 'r')
    if not file then return end
    
    local content = file:read('*all')
    file:close()
    
    -- Reset map
    _G.SkinAttachmentMap = {}
    
    local currentSkinID = nil
    for line in content:gmatch('[^\r\n]+') do
        -- Skip comments/separators
        if not line:match('^%s*#') and line:match('%S') then
            -- Check if it's a skin line: just a number (skin ID) optionally with |
            local skinID = line:match('^%s*(%d+)%s*|') or line:match('^%s*(%d+)%s*$')
            if skinID and #skinID >= 9 then
                -- Could be skin ID (9-10 digits) or attachment ID
                -- Skin IDs in attachments.txt are under #skin# section: e.g. 1101001116
                -- We detect by context - after a #---skin---# comment, next line is skin
                -- Actually the format is: skinID | skin name on one line, then attachment lines
                -- Skin line has long name after |, attachment line has short name
                -- Simpler: if line has 2 pipes => attachment line, if 1 pipe => could be skin
                local parts = {}
                for p in line:gmatch('[^|]+') do
                    table.insert(parts, p:match('^%s*(.-)%s*$'))
                end
                if #parts == 2 then
                    -- This is a skin line: "skinID | Skin Name - Gun"
                    currentSkinID = tonumber(parts[1])
                    if currentSkinID then
                        _G.SkinAttachmentMap[currentSkinID] = _G.SkinAttachmentMap[currentSkinID] or {}
                    end
                elseif #parts == 3 and currentSkinID then
                    -- This is an attachment line: "attachID | hexval | Attachment Name"
                    local attachID = tonumber(parts[1])
                    local attachName = parts[3]
                    if attachID and attachName and attachName ~= "" then
                        _G.SkinAttachmentMap[currentSkinID][attachName] = attachID
                    end
                end
            end
        end
    end
end
_G.LoadAttachmentFile = LoadAttachmentFile

-- ============================================================
-- [FIXED] WeaponBaseIDMap - config key -> weapon base ID
-- Ye map ReadConfigFile mein use hoga
-- ============================================================
_G.WeaponBaseIDMap = {
    M416=101004, AKM=101001, SCAR=101003, M16A4=101002,
    GROZA=101005, AUG=101006, QBZ=101007, M762=101008,
    MK47=101009, G36C=101010, FAMAS=101011, ACE32=101102,
    Kar98=103001, M24=103002, AWM=103003, SKS=103004,
    VSS=103005, Mini14=103006, MK14=103007, SLR=103009,
    QBU=103010, MK12=103011, AMR=103012, Mosin=103013,
    UZI=102001, UMP=102002, Vector=102003, Thompson=102004,
    Bizon=102005, MP5K=102007, P90=102009,
    S12K=104003, DBS=104004, S1897=104001, S686=104002,
    M249=105002, DP28=105001, MG3=105010,
    Pan=106001, Machete=106003, Crowbar=106002, Sickle=106004
}


local function InitItemUpgradeSystem()
    pcall(function()
        local ModuleManager = require("client.module_framework.ModuleManager")
        if ModuleManager then
            ItemUpgradeSystem = ModuleManager.GetModule(ModuleManager.CommonModuleConfig.ItemUpgradeSystem)
            if ItemUpgradeSystem then
                ItemUpgradeSystem:DefineAndResetData()
                ItemUpgradeSystem:OnInitialize()
            end
        end
    end)
end

local function get_group_id(itemId)
    if not ItemUpgradeSystem or not itemId then return nil end
    pcall(function()
        local itemcfg = ItemUpgradeSystem:GetUpgradeCfg(itemId)
        if itemcfg and itemcfg.GroupID then return itemcfg.GroupID end
    end)
    return nil
end

function _G.InitParts(groupId, itemId)
    if not itemId then return _G.g_parts end
    if _G.g_parts[itemId] and next(_G.g_parts[itemId]) then return _G.g_parts end
    if not _G.g_parts[itemId] then _G.g_parts[itemId] = {} end
    pcall(function()
        local realGroupId = groupId or get_group_id(itemId)
        if ItemUpgradeSystem and ItemUpgradeSystem.IsWeaponIsRefit and ItemUpgradeSystem:IsWeaponIsRefit(itemId) then
            realGroupId = ItemUpgradeSystem:GetNormalGroupID(realGroupId)
        end
        local CDataTable = _G.CDataTable or require("client.slua.config.ClientConfig.data_mgr")
        local cfg = CDataTable.GetTableByFilter("ItemUpgradeUnLockConfig", "GroupID", realGroupId)
        if cfg then
            for _, info in pairs(cfg) do
                local partId = info.PartId
                if ItemUpgradeSystem and ItemUpgradeSystem.IsWeaponIsRefit and ItemUpgradeSystem:IsWeaponIsRefit(itemId) then
                    local switched = ItemUpgradeSystem:PartIDSwitch(partId, true)
                    if switched and switched ~= partId then partId = switched end
                end
                local item = CDataTable.GetTableData("Item", partId)
                if item then _G.g_parts[itemId][item.ItemName] = partId end
            end
        end
    end)
    return _G.g_parts
end

function _G.GetSlotFromSkinID(skinid, stock)
    if not skinid or not stock then return 0 end
    local attachmentTypeMap = {
        [1] = {291004,291102,291001,291006,291005,291002,293003,293004,293009,293007,293005,293006,295001,295002,291007,291003,292002,292003,291011,291008},
        [2] = {205005,205102,205007,205009,205006},
        [3] = {203008,203009,203006,203022,203010}
    }
    local targetAttachmentIDs = attachmentTypeMap[stock]
    if not targetAttachmentIDs then return 0 end
    local UAvatarUtils = import("AvatarUtils")
    if not UAvatarUtils then return 0 end
    local uCurWeaponDefaultAttachmentList = UAvatarUtils.GetWeaponAvatarDefaultAttachmentSkin(skinid, {}, false) or {}
    for _, targetAttachmentID in ipairs(targetAttachmentIDs) do
        for attachmentID, attachmentSkinID in pairs(uCurWeaponDefaultAttachmentList) do
            if attachmentID == targetAttachmentID then return attachmentSkinID end
        end
    end
    return 0
end

-- ============================================================
-- [NEW] GetAttachFromMap - attachments.txt se attachment ID lo
-- avatarid = current skin ID (e.g. 1101001213)
-- attachName = "Flash Hider", "4x Scope", "Extended Mag" etc.
-- Returns: attachmentID (number) or nil
-- ============================================================
local function GetAttachFromMap(avatarid, attachName)
    if not avatarid or not attachName then return nil end
    local skinMap = _G.SkinAttachmentMap and _G.SkinAttachmentMap[avatarid]
    if not skinMap then return nil end
    local id = skinMap[attachName]
    if id and id ~= 0 then
        if not _G.skinIdCache2[id] then
            pcall(_G.download_item, id)
            _G.skinIdCache2[id] = true
        end
        return id
    end
    return nil
end

local function resolveAttachNameFromBaseId(baseId)
    if not baseId then return nil end
    for _, id in ipairs(_G.muzzles.id_flash_hider) do
        if id == baseId then return "Flash Hider" end
    end
    for _, id in ipairs(_G.muzzles.id_compensator) do
        if id == baseId then return "Compensator" end
    end
    for _, id in ipairs(_G.muzzles.id_suppressor) do
        if id == baseId then return "Suppressor" end
    end
    if baseId == _G.foregrips.id_Angledforegrip then return "Angled Foregrip" end
    if baseId == _G.foregrips.id_thumb_grip then return "Thumb Grip" end
    if baseId == _G.foregrips.id_vertical_grip then return "Vertical Foregrip" end
    if baseId == _G.foregrips.id_light_grip then return "Light Grip" end
    if baseId == _G.foregrips.id_half_grip then return "Half Grip" end
    if baseId == _G.foregrips.id_ergonomic_grip then return "Ergonomic Grip" end
    if baseId == _G.foregrips.id_laser_sight then return "Laser Sight" end
    for _, id in ipairs(_G.magazines.id_expanded_mag) do
        if id == baseId then return "Extended Mag" end
    end
    for _, id in ipairs(_G.magazines.id_quick_mag) do
        if id == baseId then return "Quickdraw Mag" end
    end
    for _, id in ipairs(_G.magazines.id_expanded_quick_mag) do
        if id == baseId then return "Extended Quickdraw Mag" end
    end
    if baseId == _G.scopes.id_reddot then return "Red Dot Sight" end
    if baseId == _G.scopes.id_holo then return "Holographic Sight" end
    if baseId == _G.scopes.id_2x then return "2x Scope" end
    if baseId == _G.scopes.id_3x then return "3x Scope" end
    if baseId == _G.scopes.id_4x then return "4x Scope" end
    if baseId == _G.scopes.id_6x then return "6x Scope" end
    if baseId == _G.scopes.id_8x then return "8x Scope" end
    if baseId == _G.stock.id_microStock then return "Stock" end
    if baseId == _G.stock.id_tactical then return "Tactical Stock" end
    if baseId == _G.stock.id_bulletloop then return "Bullet Loop" end
    if baseId == _G.stock.id_CheekPad then return "Cheek Pad" end
    return nil
end

local function normalizeAttachName(name)
    if not name or name == '' then return nil end
    local n = string.lower(name)
    if n:find('flash hider', 1, true) then return 'Flash Hider' end
    if n:find('compensator', 1, true) then return 'Compensator' end
    if n:find('suppressor', 1, true) then return 'Suppressor' end
    if n:find('angled', 1, true) and n:find('grip', 1, true) then return 'Angled Foregrip' end
    if n:find('vertical', 1, true) and n:find('grip', 1, true) then return 'Vertical Foregrip' end
    if n:find('thumb', 1, true) and n:find('grip', 1, true) then return 'Thumb Grip' end
    if n:find('light grip', 1, true) then return 'Light Grip' end
    if n:find('half grip', 1, true) then return 'Half Grip' end
    if n:find('ergonomic', 1, true) then return 'Ergonomic Grip' end
    if n:find('laser', 1, true) then return 'Laser Sight' end
    if n:find('extended quickdraw', 1, true) then return 'Extended Quickdraw Mag' end
    if n:find('extended quick', 1, true) then return 'Extended Quickdraw Mag' end
    if n:find('quickdraw', 1, true) then return 'Quickdraw Mag' end
    if n:find('extended mag', 1, true) or (n:find('extended', 1, true) and n:find('mag', 1, true)) then return 'Extended Mag' end
    if n:find('red dot', 1, true) then return 'Red Dot Sight' end
    if n:find('holographic', 1, true) or n == 'holo' then return 'Holographic Sight' end
    if n:find('8x', 1, true) then return '8x Scope' end
    if n:find('6x', 1, true) then return '6x Scope' end
    if n:find('4x', 1, true) then return '4x Scope' end
    if n:find('3x', 1, true) then return '3x Scope' end
    if n:find('2x', 1, true) then return '2x Scope' end
    if n:find('tactical stock', 1, true) then return 'Tactical Stock' end
    if n:find('bullet loop', 1, true) then return 'Bullet Loop' end
    if n:find('cheek pad', 1, true) then return 'Cheek Pad' end
    if n:find('stock', 1, true) then return 'Stock' end
    return name
end

local function lookupAttachPart(avatarid, ...)
    for i = 1, select('#', ...) do
        local name = select(i, ...)
        local fromMap = GetAttachFromMap(avatarid, name)
        if fromMap then return fromMap end
        local parts = _G.g_parts[avatarid]
        if parts then
            if parts[name] then return parts[name] end
            for k, v in pairs(parts) do
                local canon = normalizeAttachName(k)
                if k == name or canon == name or k:find(name, 1, true) or name:find(k, 1, true) then
                    return v
                end
            end
        end
    end
    return nil
end

local function classifyAttachmentBaseId(itemId)
    if not itemId or itemId == 0 then return nil end
    if itemId >= 10000000 then return nil end
    for _, id in ipairs(_G.muzzles.id_flash_hider) do if itemId == id then return 'muzzle' end end
    for _, id in ipairs(_G.muzzles.id_compensator) do if itemId == id then return 'muzzle' end end
    for _, id in ipairs(_G.muzzles.id_suppressor) do if itemId == id then return 'muzzle' end end
    if itemId == _G.foregrips.id_Angledforegrip or itemId == _G.foregrips.id_thumb_grip
        or itemId == _G.foregrips.id_vertical_grip or itemId == _G.foregrips.id_light_grip
        or itemId == _G.foregrips.id_half_grip or itemId == _G.foregrips.id_ergonomic_grip
        or itemId == _G.foregrips.id_laser_sight then return 'grip' end
    for _, ids in pairs(_G.magazines) do
        for _, id in ipairs(ids) do if itemId == id then return 'mag' end end
    end
    for _, sid in pairs(_G.scopes) do if itemId == sid then return 'scope' end end
    if itemId == _G.stock.id_microStock or itemId == _G.stock.id_tactical
        or itemId == _G.stock.id_bulletloop or itemId == _G.stock.id_CheekPad then return 'stock' end
    return nil
end

-- BGMI synData slots: 0=muzzle 1=scope 2=mag 3=stock 4=grip
local ATTACH_SLOT_KIND = { 'muzzle', 'scope', 'mag', 'stock', 'grip' }

-- base attachment ID -> skin attachment ID (builtin + skin_attachment_maps.lua)
_G.SkinAttachmentBaseOverrides = _G.SkinAttachmentBaseOverrides or {}

-- config.ini skin ID -> attachment map skin ID (when they differ)
local SKIN_ATTACH_ALIASES = {
    [1103003042] = 1103003087, -- AWM
    [1103001191] = 1103001179, -- Kar98
    [1103002030] = 1103002087, -- M24
    [1101008126] = 1101008051, -- M762
}

local function resolveAttachSkinId(skinId)
    if not skinId then return skinId end
    local alias = SKIN_ATTACH_ALIASES[skinId]
    return alias or skinId
end

local function compileLuaChunk(content, path)
    if not content or content == '' then return nil end
    local src = content
    if not src:match('^%s*return') then
        src = 'return ' .. src
    end
    local tag = path and ('@' .. path) or 'chunk'
    if loadstring then
        return loadstring(src, tag)
    end
    if load then
        return load(src, tag)
    end
    return nil
end

local function mergeAttachmentMapFile(data)
    if type(data) ~= 'table' then return 0 end
    local n = 0
    for skinId, map in pairs(data) do
        if type(map) == 'table' then
            local dest = _G.SkinAttachmentBaseOverrides[skinId] or {}
            for baseId, skinAttachId in pairs(map) do
                dest[baseId] = skinAttachId
                n = n + 1
            end
            _G.SkinAttachmentBaseOverrides[skinId] = dest
        end
    end
    return n
end

-- ATTACH_BUILTIN_BEGIN
local function loadBuiltinAttachmentMaps()
    if _G.__builtinAttachLoaded then return end
    mergeAttachmentMapFile({
        [1101001213] = {
            [201009] = 1010012068,
            [201010] = 1010012067,
            [201011] = 1010012069,
            [203001] = 1010012066,
            [203002] = 1010012065,
            [203003] = 1010012064,
            [203004] = 1010012062,
            [203014] = 1010012063,
            [203015] = 1010012075,
            [204011] = 1010012070,
            [204012] = 1010012072,
            [204013] = 1010012073,
        },
        [1101002081] = {
            [201009] = 1010020769,
            [201010] = 1010020768,
            [201011] = 1010020770,
            [203001] = 1010020759,
            [203002] = 1010020758,
            [203003] = 1010020757,
            [203004] = 1010020755,
            [203014] = 1010020756,
            [204011] = 1010020760,
            [204012] = 1010020766,
            [204013] = 1010020767,
            [205002] = 1010020775,
        },
        [1101003195] = {
            [201009] = 1010031911,
            [201010] = 1010031912,
            [201011] = 1010031913,
            [202001] = 1010031914,
            [202002] = 1010031916,
            [202004] = 1010031918,
            [202005] = 1010031919,
            [202006] = 1010031915,
            [203001] = 1010031906,
            [203002] = 1010031905,
            [203003] = 1010031904,
            [203004] = 1010031902,
            [203014] = 1010031903,
            [203015] = 1010031901,
            [204011] = 1010031907,
            [204012] = 1010031908,
            [204013] = 1010031909,
        },
        [1101004046] = {
            [201009] = 1010040475,
            [201010] = 1010040474,
            [201011] = 1010040476,
            [202001] = 1010040477,
            [202002] = 1010040479,
            [202004] = 1010040482,
            [202005] = 1010040483,
            [202006] = 1010040478,
            [203001] = 1010040470,
            [203002] = 1010040469,
            [203003] = 1010040468,
            [203004] = 1010040466,
            [203008] = 1010040462,
            [203014] = 1010040467,
            [203015] = 1010040481,
            [204011] = 1010040471,
            [204012] = 1010040472,
            [204013] = 1010040473,
            [205002] = 1010040480,
            [205005] = 1010040463,
            [291004] = 1010040461,
        },
        [1101005052] = {
            [201011] = 1010050467,
            [203002] = 1010050465,
            [203003] = 1010050464,
            [203004] = 1010050462,
            [203014] = 1010050463,
            [203015] = 1010050473,
            [204011] = 1010050468,
            [204012] = 1010050469,
            [204013] = 1010050470,
        },
        [1101006075] = {
            [201009] = 1010060701,
            [201010] = 1010060702,
            [201011] = 1010060703,
            [202001] = 1010060704,
            [202002] = 1010060706,
            [202004] = 1010060708,
            [202005] = 1010060709,
            [202006] = 1010060705,
            [203001] = 1010060696,
            [203002] = 1010060695,
            [203003] = 1010060694,
            [203004] = 1010060692,
            [203014] = 1010060693,
            [203015] = 1010060691,
            [204011] = 1010060697,
            [204012] = 1010060698,
            [204013] = 1010060699,
        },
        [1101007062] = {
            [201009] = 1010070578,
            [201010] = 1010070579,
            [201011] = 1010070581,
            [202001] = 1010070582,
            [202002] = 1010070584,
            [202004] = 1010070585,
            [202005] = 1010070586,
            [202006] = 1010070583,
            [203001] = 1010070574,
            [203002] = 1010070573,
            [203003] = 1010070572,
            [203004] = 1010070569,
            [203014] = 1010070571,
            [203015] = 1010070568,
            [204011] = 1010070575,
            [204012] = 1010070576,
            [204013] = 1010070577,
        },
        [1101100012] = {
            [201009] = 1011020028,
            [201010] = 1011020027,
            [201011] = 1011020029,
            [203001] = 1011020019,
            [203002] = 1011020018,
            [203003] = 1011020017,
            [203004] = 1011020015,
            [203014] = 1011020016,
            [203015] = 1011000053,
        },
        [1101102007] = {
            [201009] = 1011020028,
            [201010] = 1011020027,
            [201011] = 1011020029,
            [202001] = 1011020034,
            [202002] = 1011020036,
            [202004] = 1011020038,
            [202005] = 1011020039,
            [202006] = 1011020035,
            [203001] = 1011020019,
            [203002] = 1011020018,
            [203003] = 1011020017,
            [203004] = 1011020015,
            [203014] = 1011020016,
            [203015] = 1011020014,
            [204011] = 1011020024,
            [204012] = 1011020025,
            [204013] = 1011020026,
            [205002] = 1011020037,
        },
        [1102001120] = {
            [201002] = 1020011138,
            [201004] = 1020011137,
            [201006] = 1020011139,
            [203001] = 1020011133,
            [203002] = 1020011132,
            [204004] = 1020011134,
            [204005] = 1020011135,
            [204006] = 1020011136,
            [205001] = 1020011142,
        },
        [1102002136] = {
            [201002] = 1020021313,
            [201004] = 1020021314,
            [201006] = 1020021315,
            [202001] = 1020021316,
            [202002] = 1020021318,
            [202004] = 1020021323,
            [202005] = 1020021324,
            [202006] = 1020021317,
            [203001] = 1020021306,
            [203002] = 1020021306,
            [203003] = 1020021305,
            [203004] = 1020021303,
            [203014] = 1020021304,
            [203015] = 1020021302,
            [204004] = 1020021308,
            [204005] = 1020021309,
            [204006] = 1020021312,
        },
        [1102003080] = {
            [201002] = 1020030756,
            [201004] = 1020030755,
            [201006] = 1020030758,
            [202002] = 1020030760,
            [202004] = 1020030759,
            [202005] = 1020030757,
            [203001] = 1020030748,
            [203002] = 1020030747,
            [203003] = 1020030746,
            [203004] = 1020030744,
            [203014] = 1020030745,
            [203015] = 1020030764,
            [204004] = 1020030749,
            [204005] = 1020030750,
            [204006] = 1020030754,
            [205002] = 1020030765,
        },
        [1103001179] = {
            [201003] = 1030011739,
            [201005] = 1030011738,
            [201007] = 1030011741,
            [203001] = 1030011737,
            [203002] = 1030011736,
            [203003] = 1030011735,
            [203004] = 1030011733,
            [203005] = 1030011731,
            [203014] = 1030011734,
            [203015] = 1030011732,
            [205003] = 1030011742,
        },
        [1103002087] = {
            [201003] = 1030020825,
            [201005] = 1030020824,
            [201007] = 1030020826,
            [203001] = 1030020818,
            [203002] = 1030020817,
            [203003] = 1030020816,
            [203004] = 1030020814,
            [203005] = 1030020812,
            [203014] = 1030020815,
            [203015] = 1030020813,
            [205003] = 1030020827,
        },
        [1103003087] = {
            [201003] = 1030030826,
            [201005] = 1030030825,
            [201007] = 1030030827,
            [203001] = 1030030818,
            [203002] = 1030030817,
            [203003] = 1030030816,
            [203004] = 1030030814,
            [203005] = 1030030812,
            [203014] = 1030030815,
            [203015] = 1030030813,
            [204007] = 1030030822,
            [204008] = 1030030823,
            [204009] = 1030030824,
            [205003] = 1030030828,
        },
        [1103004037] = {
            [201003] = 1030040316,
            [201005] = 1030040315,
            [201007] = 1030040317,
            [202001] = 1030040326,
            [202002] = 1030040327,
            [202006] = 1030040328,
            [203004] = 1030040314,
            [203005] = 1030040312,
            [203015] = 1030040313,
            [204007] = 1030040319,
            [204008] = 1030040322,
            [204009] = 1030040318,
            [204011] = 1030040324,
            [204012] = 1030040325,
            [204013] = 1030040323,
            [205003] = 1030040329,
        },
        [1103006030] = {
            [201003] = 1030060246,
            [201005] = 1030060245,
            [201007] = 1030060247,
            [203004] = 1030060244,
            [203005] = 1030060242,
            [203015] = 1030060243,
            [204007] = 1030060249,
            [204009] = 1030060248,
            [204011] = 1030060253,
            [204013] = 1030060252,
        },
        [1103007028] = {
            [201003] = 1030070234,
            [201005] = 1030070233,
            [201007] = 1030070235,
            [201009] = 1030070229,
            [201010] = 1030070228,
            [201011] = 1030070232,
            [203001] = 1030070218,
            [203002] = 1030070217,
            [203003] = 1030070216,
            [203004] = 1030070214,
            [203005] = 1030070212,
            [203014] = 1030070215,
            [203015] = 1030070213,
            [204007] = 1030070225,
            [204008] = 1030070226,
            [204009] = 1030070227,
            [204011] = 1030070222,
            [204012] = 1030070223,
            [204013] = 1030070224,
            [205003] = 1030070236,
        },
        [1103012019] = {
            [203001] = 1030120138,
            [203002] = 1030120137,
            [203003] = 1030120136,
            [203004] = 1030120134,
            [203005] = 1030120132,
            [203014] = 1030120135,
            [203015] = 1030120133,
        },
        [1105010019] = {
            [203001] = 1050100144,
            [203002] = 1050100143,
            [203003] = 1050100142,
            [203004] = 1050100139,
            [203014] = 1050100141,
            [203015] = 1050100138,
        },
    })
    _G.__builtinAttachLoaded = true
    if next(_G.SkinAttachmentBaseOverrides) then
        _G.__attachMapsLoaded = true
    end
end
-- ATTACH_BUILTIN_END

local function loadSkinAttachmentMaps()
    pcall(loadBuiltinAttachmentMaps)
    if _G.__attachMapsLoaded then return end
    local paths = {}
    if _G.__SKIN_CONFIG_BASE and _G.__SKIN_CONFIG_BASE ~= '' then
        table.insert(paths, _G.__SKIN_CONFIG_BASE .. 'skin_attachment_maps.lua')
    end
    for _, base in ipairs(loadSkinDataBases()) do
        table.insert(paths, base .. '/skin_attachment_maps.lua')
    end
    for _, path in ipairs(paths) do
        local data = nil
        local f = io.open(path, 'r')
        if f then
            local content = f:read('*all')
            f:close()
            local chunk = compileLuaChunk(content, path)
            if chunk then
                local ok, result = pcall(chunk)
                if ok then data = result end
            end
        end
        if not data and loadfile then
            local chunk = loadfile(path)
            if chunk then
                local ok, result = pcall(chunk)
                if ok then data = result end
            end
        end
        if data then
            local n = mergeAttachmentMapFile(data)
            if n > 0 then
                _G.__attachMapsLoaded = true
                print('[SkinMod] attachment maps loaded: ' .. path .. ' (' .. n .. ' entries)')
                return
            end
        end
    end
end

local function applyBaseOverridesToAttachmentMap(skinId)
    skinId = resolveAttachSkinId(skinId)
    local overrides = _G.SkinAttachmentBaseOverrides and _G.SkinAttachmentBaseOverrides[skinId]
    if not overrides then return end
    _G.SkinAttachmentMap[skinId] = _G.SkinAttachmentMap[skinId] or {}
    for baseId, skinAttachId in pairs(overrides) do
        local name = resolveAttachNameFromBaseId(baseId)
        if baseId == 203008 then name = '4x Scope' end
        if baseId == 291004 then name = 'Extended Mag' end
        if name then
            _G.SkinAttachmentMap[skinId][name] = skinAttachId
        end
    end
end

local function getWeaponAppliedSkinId(weapon, fallback)
    local synData = weapon and weapon.synData
    if synData then
        local slot7 = synData:Get(7)
        if slot7 then
            local id = slua.IndexReference(slot7, "defineID").TypeSpecificID
            if id and id >= 1100000000 then
                return resolveAttachSkinId(id)
            end
        end
    end
    return resolveAttachSkinId(fallback)
end

local function getSkinAttachOverride(avatarid, current_id)
    if not avatarid or not current_id or current_id == 0 then return nil end
    if current_id >= 10000000 then return nil end
    if not _G.__attachMapsLoaded then pcall(loadSkinAttachmentMaps) end
    avatarid = resolveAttachSkinId(avatarid)
    local map = _G.SkinAttachmentBaseOverrides and _G.SkinAttachmentBaseOverrides[avatarid]
    if not map then return nil end
    local id = map[current_id]
    if id and id ~= 0 then
        if not _G.skinIdCache2[id] then
            pcall(_G.download_item, id)
            _G.skinIdCache2[id] = true
        end
        return id
    end
    return nil
end

local function applyAttachmentSkinForSlot(attachIdx, itemId, avatarid)
    local override = getSkinAttachOverride(avatarid, itemId)
    if override and override ~= itemId then
        return override, true
    end
    local kind = classifyAttachmentBaseId(itemId)
    if not kind and attachIdx >= 0 and attachIdx <= 4 then
        kind = ATTACH_SLOT_KIND[attachIdx + 1]
    end
    if kind == 'muzzle' then return _G.get_muzzleid(itemId, avatarid)
    elseif kind == 'grip' then return _G.get_forgripid(itemId, avatarid)
    elseif kind == 'mag' then return _G.get_magazinesid(itemId, avatarid)
    elseif kind == 'stock' then return _G.get_stockid(itemId, avatarid)
    elseif kind == 'scope' then return _G.get_scopeid(itemId, avatarid)
    end
    return itemId, false
end

local function hasAttachmentOverrideMap(skinId)
    skinId = resolveAttachSkinId(skinId)
    local map = _G.SkinAttachmentBaseOverrides and _G.SkinAttachmentBaseOverrides[skinId]
    return map and next(map) ~= nil
end

local function bootstrapAttachmentSkins()
    if not _G.WeaponSkinIndex then return end
    pcall(loadSkinAttachmentMaps)
    _G.__attachmentBootstrapDone = _G.__attachmentBootstrapDone or {}
    local UAvatarUtils = import("AvatarUtils")
    for _, skinId in pairs(_G.WeaponSkinIndex) do
        if skinId and skinId > 1000000000 and not _G.__attachmentBootstrapDone[skinId] then
            pcall(function() applyBaseOverridesToAttachmentMap(skinId) end)
            if not hasAttachmentOverrideMap(skinId) then
                pcall(function() _G.InitParts(get_group_id(skinId), skinId) end)
            end
            if UAvatarUtils and UAvatarUtils.GetWeaponAvatarDefaultAttachmentSkin and not hasAttachmentOverrideMap(skinId) then
                pcall(function()
                    local list = UAvatarUtils.GetWeaponAvatarDefaultAttachmentSkin(skinId, {}, false) or {}
                    _G.SkinAttachmentMap[skinId] = _G.SkinAttachmentMap[skinId] or {}
                    for baseId, skinAttachId in pairs(list) do
                        local name = resolveAttachNameFromBaseId(baseId)
                        if name and skinAttachId and skinAttachId ~= 0 then
                            _G.SkinAttachmentMap[skinId][name] = skinAttachId
                        end
                    end
                end)
            end
            if _G.g_parts[skinId] then
                _G.SkinAttachmentMap[skinId] = _G.SkinAttachmentMap[skinId] or {}
                for partName, partId in pairs(_G.g_parts[skinId]) do
                    local canon = normalizeAttachName(partName) or resolveAttachNameFromBaseId(partId)
                    if canon and partId and partId ~= 0 then
                        _G.SkinAttachmentMap[skinId][canon] = partId
                    end
                end
            end
            _G.__attachmentBootstrapDone[skinId] = true
        end
    end
end
_G.bootstrapAttachmentSkins = bootstrapAttachmentSkins

function _G.get_muzzleid(current_id, avatarid)
    local initial_id = current_id
    _G.InitParts(get_group_id(avatarid), avatarid)
    local function is_in_muzzles_list(muzzle_type)
        for _, id in ipairs(_G.muzzles[muzzle_type]) do
            if current_id == id then return true end
        end
        return false
    end
    local muzzle_type = nil
    if is_in_muzzles_list("id_flash_hider") then muzzle_type = "Flash Hider"
    elseif is_in_muzzles_list("id_compensator") then muzzle_type = "Compensator"
    elseif is_in_muzzles_list("id_suppressor") then muzzle_type = "Suppressor"
    end
    if muzzle_type then
        local found = lookupAttachPart(avatarid, muzzle_type,
            muzzle_type .. " (AR)", muzzle_type .. " (SMG)", muzzle_type .. " (Snipers)")
        if found then current_id = found end
    end
    if initial_id ~= current_id then return current_id, true end
    return current_id, false
end

function _G.get_forgripid(current_id, avatarid)
    local initial_id = current_id
    _G.InitParts(get_group_id(avatarid), avatarid)
    local grip_name = nil
    if current_id == _G.foregrips.id_Angledforegrip then grip_name = "Angled Foregrip"
    elseif current_id == _G.foregrips.id_thumb_grip then grip_name = "Thumb Grip"
    elseif current_id == _G.foregrips.id_vertical_grip then grip_name = "Vertical Foregrip"
    elseif current_id == _G.foregrips.id_light_grip then grip_name = "Light Grip"
    elseif current_id == _G.foregrips.id_half_grip then grip_name = "Half Grip"
    elseif current_id == _G.foregrips.id_ergonomic_grip then grip_name = "Ergonomic Grip"
    elseif current_id == _G.foregrips.id_laser_sight then grip_name = "Laser Sight"
    end
    if grip_name then
        local found = lookupAttachPart(avatarid, grip_name)
        if found then current_id = found end
    end
    if initial_id ~= current_id then return current_id, true end
    return current_id, false
end

function _G.get_magazinesid(current_id, avatarid)
    local initial_id = current_id
    _G.InitParts(get_group_id(avatarid), avatarid)
    local function is_in_magazine_list(mag_type)
        for _, id in ipairs(_G.magazines[mag_type]) do
            if current_id == id then return true end
        end
        return false
    end
    local magazine_type = nil
    if is_in_magazine_list("id_expanded_mag") then magazine_type = "Extended Mag"
    elseif is_in_magazine_list("id_quick_mag") then magazine_type = "Quickdraw Mag"
    elseif is_in_magazine_list("id_expanded_quick_mag") then magazine_type = "Extended Quickdraw Mag"
    end
    if magazine_type then
        local found = lookupAttachPart(avatarid, magazine_type)
        if found then current_id = found end
    end
    if initial_id ~= current_id then return current_id, true end
    return current_id, false
end

function _G.get_scopeid(current_id, avatarid)
    local initial_id = current_id
    _G.InitParts(get_group_id(avatarid), avatarid)
    local scope_name = nil
    if current_id == _G.scopes.id_reddot then scope_name = "Red Dot Sight"
    elseif current_id == _G.scopes.id_holo then scope_name = "Holographic Sight"
    elseif current_id == _G.scopes.id_2x then scope_name = "2x Scope"
    elseif current_id == _G.scopes.id_3x then scope_name = "3x Scope"
    elseif current_id == _G.scopes.id_4x then scope_name = "4x Scope"
    elseif current_id == _G.scopes.id_6x then scope_name = "6x Scope"
    elseif current_id == _G.scopes.id_8x then scope_name = "8x Scope"
    end
    if scope_name then
        local found = lookupAttachPart(avatarid, scope_name)
        if found then current_id = found end
    end
    if initial_id ~= current_id then return current_id, true end
    return current_id, false
end

function _G.get_stockid(current_id, avatarid)
    local initial_id = current_id
    _G.InitParts(get_group_id(avatarid), avatarid)
    local stock_name = nil
    if current_id == _G.stock.id_microStock then stock_name = "Stock"
    elseif current_id == _G.stock.id_tactical then stock_name = "Tactical Stock"
    elseif current_id == _G.stock.id_bulletloop then stock_name = "Bullet Loop"
    elseif current_id == _G.stock.id_CheekPad then stock_name = "Cheek Pad"
    end
    if stock_name then
        local found = lookupAttachPart(avatarid, stock_name)
        if found then current_id = found end
    end
    if initial_id ~= current_id then return current_id, true end
    return current_id, false
end

local function loadSkinDataBases()
    local bases = {}
    local seen = {}
    local function addBase(b)
        if b and b ~= '' and not seen[b] then
            seen[b] = true
            table.insert(bases, b)
        end
    end
    if _G.__SKIN_CONFIG_BASE and _G.__SKIN_CONFIG_BASE ~= '' then
        addBase(_G.__SKIN_CONFIG_BASE:gsub('/$', ''))
    end
    addBase('/storage/emulated/0/Android/data/com.pubg.imobile/files')
    addBase('/data/user/0/com.pubg.imobile/files')
    for _, pathFile in ipairs({
        '/storage/emulated/0/Android/data/com.pubg.imobile/files/skin_data.paths',
        '/data/user/0/com.pubg.imobile/files/skin_data.paths',
    }) do
        local f = io.open(pathFile, 'r')
        if f then
            for line in f:lines() do
                line = line:match('^%s*(.-)%s*$')
                if line and line ~= '' and not line:match('^#') then
                    addBase(line)
                end
            end
            f:close()
        end
    end
    return bases
end

local function ReadConfigFile(force)
    local now = os.clock()
    if not force and _G.__lastConfigReadClock and (now - _G.__lastConfigReadClock) < 90 then
        return
    end

    local possiblePaths = {}
    for _, base in ipairs(loadSkinDataBases()) do
        table.insert(possiblePaths, base .. '/config.ini')
    end
    
    if _G.__SKIN_CONFIG_BASE and _G.__SKIN_CONFIG_BASE ~= '' then
        table.insert(possiblePaths, 1, _G.__SKIN_CONFIG_BASE .. 'config.ini')
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
    
    if not configPath then
        return
    end

    local file = io.open(configPath, 'r')
    if not file then return end
    local content = file:read('*all')
    file:close()

    if not force and content == _G.__lastConfigContent then
        _G.__lastConfigReadClock = now
        return
    end
    _G.__lastConfigContent = content
    _G.__lastConfigReadClock = now
    _G.__attachmentBootstrapDone = {}
    
    local newConfig = {}
    for line in content:gmatch('[^\r\n]+') do
        local key, value = line:match('^%s*([%w_]+)%s*=%s*(%d+)%s*')
        if key and value then newConfig[key] = tonumber(value) end
    end
    
    local oldTheme = _G.TargetLobbyThemeID
    _G.TargetLobbyThemeID = newConfig['LobbyTheme'] or _G.TargetLobbyThemeID or 0
    if oldTheme ~= _G.TargetLobbyThemeID then _G.LastAppliedThemeID = nil end
    local configShirt = newConfig['SHIRT'] or 0
    local lobbyOutfit = tonumber(_G.AddOutfitLastLobbyOutfitRes) or tonumber(_G.__ACTIVE_OUTFIT_RES) or 0
    if lobbyOutfit > 0 then
        _G.SuitSkin = lobbyOutfit
        _G.__ACTIVE_OUTFIT_RES = lobbyOutfit
    elseif configShirt > 0 then
        _G.SuitSkin = configShirt
        _G.__ACTIVE_OUTFIT_RES = configShirt
        _G.AddOutfitLastLobbyOutfitRes = configShirt
    elseif _G.__ACTIVE_OUTFIT_RES and tonumber(_G.__ACTIVE_OUTFIT_RES) > 0 then
        _G.SuitSkin = tonumber(_G.__ACTIVE_OUTFIT_RES)
    else
        _G.SuitSkin = configShirt
    end
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
    local oldPet = _G.PetSkin
    local petVal = newConfig['PET_SKIN'] or newConfig['PET']
    if petVal then
        _G.PetSkin = petVal
        if oldPet ~= _G.PetSkin then _G.LastAppliedPet = 0 end
    end
    _G.HallEffectSkin= newConfig['HALL_EFFECT'] or 0

    -- ============================================================
    -- [FIXED] Gun Skin Logic
    -- Config mein user dega: M416=1101004046 (direct skin ID)
    -- Hum WeaponSkinIndex[weaponBaseID] = skinID store karte hain
    -- Phir game mein jab weapon equip hota hai, skin apply hoti hai
    -- ============================================================
    if not _G.WeaponSkinIndex then _G.WeaponSkinIndex = {} end
    for configKey, weaponBaseID in pairs(_G.WeaponBaseIDMap or {}) do
        local configVal = newConfig[configKey]
        if configVal and configVal ~= 0 then
            _G.WeaponSkinIndex[weaponBaseID] = configVal
            lastConfig[configKey] = configVal
        end
    end

    pcall(function()
        if _G.UpdateVehicleSkinMappings then _G.UpdateVehicleSkinMappings(newConfig) end
    end)

    for k, v in pairs(newConfig) do
        lastConfig[k] = v
    end

    pcall(LoadAttachmentFile, configPath)
    if _G.__SKIN_CONFIG_BASE and _G.__SKIN_CONFIG_BASE ~= '' then
        pcall(LoadAttachmentFile, _G.__SKIN_CONFIG_BASE .. 'config.ini')
    end
    pcall(loadSkinAttachmentMaps)
    pcall(bootstrapAttachmentSkins)
end
_G.ReadConfigFile = ReadConfigFile

local function checkSkinReloadStamp()
    for _, base in ipairs(loadSkinDataBases()) do
        local path = base .. '/skin_reload.stamp'
        local f = io.open(path, 'r')
        if f then
            local stamp = (f:read('*a') or ''):gsub('%s+', '')
            f:close()
            if stamp ~= '' and stamp ~= (_G.__skinReloadStamp or '') then
                _G.__skinReloadStamp = stamp
                ReadConfigFile(true)
                _G.__attachmentBootstrapDone = {}
                return true
            end
        end
    end
    if _G.__SKIN_CONFIG_BASE and _G.__SKIN_CONFIG_BASE ~= '' then
        local f = io.open(_G.__SKIN_CONFIG_BASE .. 'skin_reload.stamp', 'r')
        if f then
            local stamp = (f:read('*a') or ''):gsub('%s+', '')
            f:close()
            if stamp ~= '' and stamp ~= (_G.__skinReloadStamp or '') then
                _G.__skinReloadStamp = stamp
                ReadConfigFile(true)
                _G.__attachmentBootstrapDone = {}
                return true
            end
        end
    end
    return false
end

local function resolveBaseWeaponId(weaponid)
    if not weaponid or weaponid == 0 then return nil end
    if _G.WeaponSkinIndex and _G.WeaponSkinIndex[weaponid] then
        return weaponid
    end
    if _G.skinIdMappings then
        for baseId, skins in pairs(_G.skinIdMappings) do
            if baseId == weaponid then return baseId end
            for i = 1, #skins do
                if skins[i] == weaponid then return baseId end
            end
        end
    end
    if _G.WeaponBaseIDMap then
        for _, baseId in pairs(_G.WeaponBaseIDMap) do
            if baseId == weaponid then return baseId end
        end
    end
    return weaponid
end
_G.resolveBaseWeaponId = resolveBaseWeaponId

-- ============================================================
-- [FIXED] apply_weapon_skin - Yeh function weapon pe skin lagata hai
-- skinId = jo config se aaya (e.g. 1101004046)
-- weaponBaseID = weapon ka base ID (e.g. 101004 for M416)
-- ============================================================
local function apply_weapon_skin(CurWeapon, weaponBaseID)
    if not CurWeapon or not slua.isValid(CurWeapon) then return end
    if not weaponBaseID then return end

    weaponBaseID = resolveBaseWeaponId(weaponBaseID)
    local targetSkinID = _G.WeaponSkinIndex and _G.WeaponSkinIndex[weaponBaseID]
    if not targetSkinID or targetSkinID == 0 then return end

    -- Download skin agar cached nahi
    if not _G.skinIdCache[targetSkinID] then
        pcall(_G.download_item, targetSkinID)
        _G.skinIdCache[targetSkinID] = true
    end

    -- synData:Get(7) mein skin info hoti hai
    local synData = CurWeapon.synData
    if not synData then return end

    local slotData = synData:Get(7)
    if not slotData then return end

    local currentSkinID = slua.IndexReference(slotData, "defineID").TypeSpecificID
    local skinChanged = (currentSkinID ~= targetSkinID)
    if skinChanged then
        slotData.defineID.TypeSpecificID = targetSkinID
        synData:Set(7, slotData)
        pcall(function() CurWeapon:DelayHandleAvatarMeshChanged() end)
        pcall(function()
            local avatar = CurWeapon.WeaponAvatarComponent_BP or CurWeapon.WeaponAvatarComp or CurWeapon.AvatarComponent
            if avatar and avatar.ChangeItemAvatar then
                avatar:ChangeItemAvatar(targetSkinID, true)
            end
        end)
    end
    pcall(_G.apply_attachment, CurWeapon, targetSkinID)
end
_G.apply_weapon_skin = apply_weapon_skin

local function getSkinPlayerController()
    if slua_GameFrontendHUD and slua_GameFrontendHUD.GetPlayerController then
        local pc = slua_GameFrontendHUD:GetPlayerController()
        if pc and slua.isValid(pc) then return pc end
    end
    local ok, GD = pcall(require, "GameLua.GameCore.Data.GameplayData")
    if ok and GD and GD.GetPlayerController then
        local pc = GD.GetPlayerController()
        if pc and slua.isValid(pc) then return pc end
    end
    return nil
end

local function getCharacterFromController(pc)
    if not pc or not slua.isValid(pc) then return nil end
    local ch = nil
    local tryFns = {
        function() return pc.GetPlayerCharacterSafety and pc:GetPlayerCharacterSafety() end,
        function() return pc.GetPlayerCharacter and pc:GetPlayerCharacter() end,
        function() return pc.GetPawn and pc:GetPawn() end,
        function() return pc.K2_GetPawn and pc:K2_GetPawn() end,
        function() return pc.AcknowledgedPawn end,
        function() return pc.Pawn end,
        function()
            if pc.GetViewTarget then
                return pc:GetViewTarget()
            end
        end,
    }
    for _, fn in ipairs(tryFns) do
        pcall(function()
            local c = fn()
            if c and slua.isValid(c) then ch = c end
        end)
        if ch then return ch end
    end
    return nil
end

local function getSkinPlayerCharacter()
    local pc = getSkinPlayerController()
    local ch = getCharacterFromController(pc)
    if ch then return ch end
    pcall(function()
        local GD = require('GameLua.GameCore.Data.GameplayData')
        if not GD then return end
        if GD.GetPlayerCharacter then
            local c = GD.GetPlayerCharacter()
            if c and slua.isValid(c) then ch = c end
        end
        if not ch and GD.GetLocalFocusCharacter then
            local c = GD.GetLocalFocusCharacter()
            if c and slua.isValid(c) then ch = c end
        end
    end)
    return ch
end

local function getMatchPlayerCharacter()
    local ch = nil
    pcall(function()
        if slua_GameFrontendHUD and slua_GameFrontendHUD.GetPlayerController then
            local pc = slua_GameFrontendHUD:GetPlayerController()
            if pc and slua.isValid(pc) then
                ch = getCharacterFromController(pc)
            end
        end
    end)
    if ch and slua.isValid(ch) then return ch end
    ch = getSkinPlayerCharacter()
    if ch and slua.isValid(ch) then return ch end
    pcall(function()
        local GD = require('GameLua.GameCore.Data.GameplayData')
        if GD and GD.GetPlayerCharacter then
            local c = GD.GetPlayerCharacter()
            if c and slua.isValid(c) then ch = c end
        end
    end)
    return (ch and slua.isValid(ch)) and ch or nil
end
_G.getMatchPlayerCharacter = getMatchPlayerCharacter

local function probeActors(reason)
    local now = os.clock()
    if now - (_G.__skinProbeLast or 0) < 5 then return end
    _G.__skinProbeLast = now

    skinProbeLog('=== ACTOR PROBE:', reason or '?')
    skinProbeLog('  HUD:', slua_GameFrontendHUD ~= nil)

    local pcHud, pcHudValid = nil, false
    pcall(function()
        if slua_GameFrontendHUD and slua_GameFrontendHUD.GetPlayerController then
            pcHud = slua_GameFrontendHUD:GetPlayerController()
            pcHudValid = pcHud ~= nil and slua.isValid(pcHud)
        end
    end)
    skinProbeLog('  PC@HUD:', pcHud ~= nil, 'valid=' .. tostring(pcHudValid))

    local pcGD = nil
    pcall(function()
        local GD = require('GameLua.GameCore.Data.GameplayData')
        if GD and GD.GetPlayerController then
            pcGD = GD.GetPlayerController()
        end
    end)
    skinProbeLog('  PC@GameplayData:', pcGD ~= nil)

    local pc = getSkinPlayerController()
    local ch = getSkinPlayerCharacter()
    local chValid = ch ~= nil and slua.isValid(ch)
    skinProbeLog('  Character:', ch ~= nil, 'valid=' .. tostring(chValid))
    local pawn = getCharacterFromController(pc)
    skinProbeLog('  Pawn:', pawn ~= nil, pawn and slua.isValid(pawn))
    local targets = collectOutfitTargets and collectOutfitTargets() or {}
    skinProbeLog('  OutfitTargets:', #targets)

    if chValid then
        local w = nil
        pcall(function() w = ch:GetCurrentWeapon() end)
        if w and slua.isValid(w) then
            local defineId, slotSkin = 0, 0
            pcall(function()
                defineId = w:GetItemDefineID().TypeSpecificID
                slotSkin = slua.IndexReference(w.synData:Get(7), 'defineID').TypeSpecificID
            end)
            local base = resolveBaseWeaponId(defineId)
            local target = _G.WeaponSkinIndex and _G.WeaponSkinIndex[base]
            skinProbeLog('  Weapon: defineId=' .. tostring(defineId),
                'slotSkin=' .. tostring(slotSkin), 'base=' .. tostring(base),
                'target=' .. tostring(target))
        else
            skinProbeLog('  Weapon: none')
        end
        pcall(function()
            for slotIdx = 0, 1 do
                local w2 = ch:GetWeaponByIndex(slotIdx)
                if w2 and slua.isValid(w2) then
                    local id = 0
                    pcall(function() id = w2:GetItemDefineID().TypeSpecificID end)
                    skinProbeLog('  Slot' .. slotIdx .. ':', id)
                end
            end
        end)
    end

    local skinCount = 0
    if _G.WeaponSkinIndex then
        for _ in pairs(_G.WeaponSkinIndex) do skinCount = skinCount + 1 end
    end
    skinProbeLog('  WeaponSkinIndex:', skinCount,
        'timers=' .. tostring(_G.__SKIN_TIMERS_STARTED),
        'boot=' .. tostring(_G.__SKIN_BOOTSTRAPPED))
end
_G.probeActors = probeActors

function table.contains(table, element)
    for _, value in ipairs(table) do
        if value == element then return true end
    end
    return false
end

local function locationsClose(loc1, loc2, tolerance)
    local dx = loc1.X - loc2.X
    local dy = loc1.Y - loc2.Y
    local dz = loc1.Z - loc2.Z
    return dx * dx + dy * dy + dz * dz < tolerance * tolerance
end

local GEAR_BASE_BAG = { [1]=501001, [2]=501002, [3]=501003 }
local GEAR_BASE_HELM = { [1]=502001, [2]=502002, [3]=502003 }

local function isConfiguredGearSkin(itemId)
    itemId = tonumber(itemId) or 0
    if itemId <= 0 then return false end
    local skins = {
        _G.Backpack1Skin, _G.Backpack2Skin, _G.Backpack3Skin,
        _G.Helmet1Skin, _G.Helmet2Skin, _G.Helmet3Skin,
    }
    for _, s in ipairs(skins) do
        if s and tonumber(s) == itemId then return true end
    end
    return false
end

local function resolveGearLevelFromBase(addId, baseMap, getLevelFn, kind)
    addId = tonumber(addId) or 0
    if addId <= 0 then return nil end
    if addId >= 1501000000 then return nil end
    for lvl, base in pairs(baseMap) do
        if addId == base then return lvl end
    end
    if kind == "bag" then
        if addId < 501001 or addId > 501999 then return nil end
    elseif kind == "helmet" then
        if addId < 502001 or addId > 502999 then return nil end
    end
    if getLevelFn then
        local lvl = getLevelFn(addId)
        if lvl and lvl > 0 and lvl <= 3 then return lvl end
    end
    return nil
end

local function resolveGearLevelFromSkinRes(itemId, kind)
    itemId = tonumber(itemId) or 0
    if itemId <= 0 then return nil end
    local s = tostring(itemId)
    if kind == "bag" then
        if s:sub(1, 7) == "1501001" then return 1 end
        if s:sub(1, 7) == "1501002" then return 2 end
        if s:sub(1, 7) == "1501003" then return 3 end
    elseif kind == "helmet" then
        if s:sub(1, 7) == "1502001" then return 1 end
        if s:sub(1, 7) == "1502002" then return 2 end
        if s:sub(1, 7) == "1502003" then return 3 end
    end
    return nil
end

local function resolveConfiguredGearPreviewLevel(kind)
    if kind == "bag" then
        if _G.Backpack3Skin and _G.Backpack3Skin > 0 then return 3, GEAR_BASE_BAG[3] end
        if _G.Backpack2Skin and _G.Backpack2Skin > 0 then return 2, GEAR_BASE_BAG[2] end
        if _G.Backpack1Skin and _G.Backpack1Skin > 0 then return 1, GEAR_BASE_BAG[1] end
    else
        if _G.Helmet3Skin and _G.Helmet3Skin > 0 then return 3, GEAR_BASE_HELM[3] end
        if _G.Helmet2Skin and _G.Helmet2Skin > 0 then return 2, GEAR_BASE_HELM[2] end
        if _G.Helmet1Skin and _G.Helmet1Skin > 0 then return 1, GEAR_BASE_HELM[1] end
    end
    return nil, nil
end

local function playerHasGearItem(uCharacter, itemId, BackpackUtils)
    itemId = tonumber(itemId) or 0
    if itemId <= 0 then return false end
    local found = false
    pcall(function()
        local pc = nil
        if uCharacter and uCharacter.GetPlayerController then pc = uCharacter:GetPlayerController() end
        if (not pc or not slua.isValid(pc)) and slua_GameFrontendHUD then
            pc = slua_GameFrontendHUD:GetPlayerController()
        end
        if not pc or not slua.isValid(pc) then return end
        if BackpackUtils then
            if BackpackUtils.IsItemInBackpack and BackpackUtils.IsItemInBackpack(pc, itemId) then
                found = true return
            end
            if BackpackUtils.HasItemByID and BackpackUtils.HasItemByID(pc, itemId) then
                found = true return
            end
            if BackpackUtils.GetItemCountByItemID and (BackpackUtils.GetItemCountByItemID(pc, itemId) or 0) > 0 then
                found = true return
            end
        end
        local bc = pc.BackpackComponent
        if bc and bc.GetItemCountByItemID and (bc:GetItemCountByItemID(itemId) or 0) > 0 then
            found = true
        end
    end)
    return found
end

local function getGearLevelFromInventory(uCharacter, kind, BackpackUtils)
    local baseMap = kind == "bag" and GEAR_BASE_BAG or GEAR_BASE_HELM
    for lvl = 3, 1, -1 do
        local base = baseMap[lvl]
        if base and playerHasGearItem(uCharacter, base, BackpackUtils) then
            return lvl, base
        end
    end
    return nil, nil
end

local function getEquippedGearLevel(ApplyData, slotId, baseMap, getLevelFn, kind)
    if not ApplyData or not slua.isValid(ApplyData) then return nil, nil end
    local idx = findAvatarSlotIndex(ApplyData, slotId)
    if not idx then return nil, nil end
    local eq = ApplyData:Get(idx)
    if not eq then return nil, nil end
    local addId = tonumber(eq.AdditionalItemID) or 0
    local itemId = tonumber(eq.ItemId) or 0
    if addId > 0 then
        local lvl = resolveGearLevelFromBase(addId, baseMap, getLevelFn, kind)
        if lvl and lvl > 0 then return lvl, baseMap[lvl] or baseMap[1] end
        if getLevelFn then
            local lvl2 = getLevelFn(addId)
            if lvl2 and lvl2 > 0 and lvl2 <= 3 then return lvl2, baseMap[lvl2] or baseMap[1] end
        end
    end
    if itemId > 0 then
        local lvl = resolveGearLevelFromSkinRes(itemId, kind)
        if lvl and lvl > 0 then return lvl, baseMap[lvl] or baseMap[1] end
    end
    return nil, nil
end

local function resolveGearVisualState(uCharacter, ApplyData, BackpackUtils, slots, inMatch)
    local getBagLevel = BackpackUtils and BackpackUtils.GetEquipmentBagLevel or nil
    local getHelmLevel = BackpackUtils and BackpackUtils.GetEquipmentHelmetLevel or nil
    local bagLvl, bagBase, helmLvl, helmBase

    if inMatch then
        bagLvl, bagBase = getCharacterBattleGearLevel(uCharacter, "bag", BackpackUtils)
        if not bagLvl then
            bagLvl, bagBase = getGearLevelFromInventory(uCharacter, "bag", BackpackUtils)
        end
        helmLvl, helmBase = getCharacterBattleGearLevel(uCharacter, "helmet", BackpackUtils)
        if not helmLvl then
            helmLvl, helmBase = getGearLevelFromInventory(uCharacter, "helmet", BackpackUtils)
        end
    else
        bagLvl, bagBase = getEquippedGearLevel(
            ApplyData, slots.BackpackEquipemtSlot, GEAR_BASE_BAG, getBagLevel, "bag")
        helmLvl, helmBase = getEquippedGearLevel(
            ApplyData, slots.HelmetEquipemtSlot, GEAR_BASE_HELM, getHelmLevel, "helmet")
    end

    if ApplyData and slua.isValid(ApplyData) then
        if (not bagLvl or bagLvl <= 0) and getBagLevel then
            local idx = findAvatarSlotIndex(ApplyData, slots.BackpackEquipemtSlot)
            if idx then
                local eq = ApplyData:Get(idx)
                local addId = eq and tonumber(eq.AdditionalItemID) or 0
                if addId > 0 then
                    bagLvl = getBagLevel(addId) or resolveGearLevelFromBase(addId, GEAR_BASE_BAG, getBagLevel, "bag")
                    if bagLvl and bagLvl > 0 then bagBase = GEAR_BASE_BAG[bagLvl] end
                end
            end
        end
        if (not helmLvl or helmLvl <= 0) and getHelmLevel then
            local idx = findAvatarSlotIndex(ApplyData, slots.HelmetEquipemtSlot)
            if idx then
                local eq = ApplyData:Get(idx)
                local addId = eq and tonumber(eq.AdditionalItemID) or 0
                if addId > 0 then
                    helmLvl = getHelmLevel(addId) or resolveGearLevelFromBase(addId, GEAR_BASE_HELM, getHelmLevel, "helmet")
                    if helmLvl and helmLvl > 0 then helmBase = GEAR_BASE_HELM[helmLvl] end
                end
            end
        end
    end

    if not inMatch then
        if not bagLvl then bagLvl, bagBase = resolveConfiguredGearPreviewLevel("bag") end
        if not helmLvl then helmLvl, helmBase = resolveConfiguredGearPreviewLevel("helmet") end
    end

    local bagSkin = pickGearSkinForLevel(bagLvl, _G.Backpack1Skin, _G.Backpack2Skin, _G.Backpack3Skin)
    local helmSkin = pickGearSkinForLevel(helmLvl, _G.Helmet1Skin, _G.Helmet2Skin, _G.Helmet3Skin)

    if not inMatch then
        if (not bagSkin or bagSkin == 0) and (_G.Backpack3Skin or _G.Backpack2Skin or _G.Backpack1Skin) then
            bagSkin = _G.Backpack3Skin or _G.Backpack2Skin or _G.Backpack1Skin
            bagLvl = bagLvl or 3
            bagBase = bagBase or GEAR_BASE_BAG[bagLvl] or GEAR_BASE_BAG[3]
        end
        if (not helmSkin or helmSkin == 0) and (_G.Helmet3Skin or _G.Helmet2Skin or _G.Helmet1Skin) then
            helmSkin = _G.Helmet3Skin or _G.Helmet2Skin or _G.Helmet1Skin
            helmLvl = helmLvl or 3
            helmBase = helmBase or GEAR_BASE_HELM[helmLvl] or GEAR_BASE_HELM[3]
        end
    end

    return bagSkin, bagBase, bagLvl, helmSkin, helmBase, helmLvl
end

local function getCharacterBattleGearLevel(uCharacter, kind, BackpackUtils)
    if not uCharacter or not slua.isValid(uCharacter) then return nil, nil end
    local baseMap = kind == "bag" and GEAR_BASE_BAG or GEAR_BASE_HELM
    local getLevelFn = kind == "bag"
        and (BackpackUtils and BackpackUtils.GetEquipmentBagLevel)
        or (BackpackUtils and BackpackUtils.GetEquipmentHelmetLevel)
    local lvl = nil

    pcall(function()
        if kind == "helmet" then
            if uCharacter.GetHelmetLevel then lvl = uCharacter:GetHelmetLevel() end
            if (not lvl or lvl <= 0) and uCharacter.CurrentHelmetLevel then
                lvl = tonumber(uCharacter.CurrentHelmetLevel)
            end
            if (not lvl or lvl <= 0) and uCharacter.GetEquipHelmetLevel then
                lvl = uCharacter:GetEquipHelmetLevel()
            end
        else
            if uCharacter.GetBagLevel then lvl = uCharacter:GetBagLevel() end
            if (not lvl or lvl <= 0) and uCharacter.CurrentBagLevel then
                lvl = tonumber(uCharacter.CurrentBagLevel)
            end
            if (not lvl or lvl <= 0) and uCharacter.GetEquipBagLevel then
                lvl = uCharacter:GetEquipBagLevel()
            end
        end
        if (not lvl or lvl <= 0) and BackpackUtils then
            if kind == "bag" then
                if BackpackUtils.GetBagLevelByCharacter then lvl = BackpackUtils.GetBagLevelByCharacter(uCharacter) end
                if (not lvl or lvl <= 0) and BackpackUtils.GetCurrentBagLevel then
                    lvl = BackpackUtils.GetCurrentBagLevel(uCharacter)
                end
            else
                if BackpackUtils.GetHelmetLevelByCharacter then lvl = BackpackUtils.GetHelmetLevelByCharacter(uCharacter) end
                if (not lvl or lvl <= 0) and BackpackUtils.GetCurrentHelmetLevel then
                    lvl = BackpackUtils.GetCurrentHelmetLevel(uCharacter)
                end
            end
        end
        if (not lvl or lvl <= 0) and uCharacter.PlayerState then
            local ps = uCharacter.PlayerState
            if kind == "bag" and ps.BagLevel then lvl = tonumber(ps.BagLevel)
            elseif kind == "helmet" and ps.HelmetLevel then lvl = tonumber(ps.HelmetLevel) end
        end
    end)
    if lvl and lvl > 0 and lvl <= 3 then
        return lvl, baseMap[lvl] or baseMap[1]
    end

    local slotLvl, slotBase = nil, nil
    pcall(function()
        local comp = getCharacterAvatarComponent(uCharacter)
        local netData = comp and comp.NetAvatarData
        local applyData = netData and slua.isValid(netData) and netData.SlotSyncData
        if not applyData or not slua.isValid(applyData) then return end
        local slotId = kind == "bag"
            and _G.CustSlotType.BackpackEquipemtSlot
            or _G.CustSlotType.HelmetEquipemtSlot
        slotLvl, slotBase = getEquippedGearLevel(applyData, slotId, baseMap, getLevelFn, kind)
    end)
    if slotLvl and slotLvl > 0 then
        return slotLvl, slotBase
    end
    return nil, nil
end

local function stripModGearSlot(ApplyData, slotId, kind)
    if not ApplyData or not slua.isValid(ApplyData) then return false end
    local idx = findAvatarSlotIndex(ApplyData, slotId)
    if not idx then return false end
    local eq = ApplyData:Get(idx)
    if not eq then return false end
    local addId = tonumber(eq.AdditionalItemID) or 0
    local itemId = tonumber(eq.ItemId) or 0
    if addId > 0 then
        local baseMap = kind == "bag" and GEAR_BASE_BAG or GEAR_BASE_HELM
        for _, base in pairs(baseMap) do
            if addId == base then return false end
        end
        if kind == "bag" and addId >= 501001 and addId <= 501999 then return false end
        if kind == "helmet" and addId >= 502001 and addId <= 502999 then return false end
    end
    if itemId <= 0 then return false end
    if not isConfiguredGearSkin(itemId) and itemId < 1501000000 then return false end
    eq.ItemId = 0
    eq.AdditionalItemID = 0
    ApplyData:Set(idx, eq)
    return true
end

local function unequipModGearVisual(avatarComp, ApplyData, slotId, kind)
    -- Only clear mod cosmetic data on bag/helmet slots.
    -- Do NOT call PutOffEquipmentBySlot — it can strip the whole avatar including outfit.
    return stripModGearSlot(ApplyData, slotId, kind)
end

local function pickGearSkinForLevel(lvl, skin1, skin2, skin3)
    if not lvl or lvl <= 0 then return nil end
    local skin = (lvl == 2 and skin2) or (lvl == 3 and skin3) or skin1
    if skin and skin > 0 then return skin end
    return skin3 or skin2 or skin1
end

local function isInActiveMatch()
    if isInLobby() then return false end
    if _G.isInPlayableGame then
        return _G.isInPlayableGame()
    end
    local okFight, fighting = pcall(function()
        return GameStatus and GameStatus.IsInFightingStatus and GameStatus.IsInFightingStatus()
    end)
    if okFight and fighting then return true end
    if _G.__PLAYABLE_GAME_ACTIVE then return true end
    local ok, gs = pcall(function()
        return slua_GameFrontendHUD and slua_GameFrontendHUD:GetGameState()
    end)
    if not ok or not gs then return false end
    local state = gs.GameModeState
    if not state and gs.GetGameModeState then
        state = gs:GetGameModeState()
    end
    if not state then return false end
    return state ~= 'Lobby' and state ~= 'None'
end

local function isInLobby()
    local okLobby, inLobby = pcall(function()
        return GameStatus and GameStatus.IsInLobbyOrMainCity and GameStatus.IsInLobbyOrMainCity()
    end)
    if okLobby and inLobby then return true end
    local ok, gs = pcall(function()
        return slua_GameFrontendHUD and slua_GameFrontendHUD:GetGameState()
    end)
    if ok and gs then
        local state = gs.GameModeState
        if not state and gs.GetGameModeState then state = gs:GetGameModeState() end
        if state == 'Lobby' or state == 'None' or state == '' then return true end
    end
    return false
end
_G.isInLobby = isInLobby

local function isLobbyCharacter(ch)
    if not ch or not slua.isValid(ch) then return false end
    local isLobby = false
    pcall(function()
        if ch.IsLobbyActor and ch:IsLobbyActor() then isLobby = true end
    end)
    if isLobby then return true end
    pcall(function()
        local cls = ch.GetClass and ch:GetClass()
        if cls then
            local name = tostring(cls:GetName() or '')
            if name:find('Lobby', 1, true) or name:find('MainCity', 1, true) then
                isLobby = true
            end
        end
    end)
    return isLobby
end

local function isInPlayableGame()
    if isInLobby() then
        _G.__PLAYABLE_GAME_ACTIVE = false
        return false
    end
    local ch = getMatchPlayerCharacter()
    if ch and isLobbyCharacter(ch) then
        _G.__PLAYABLE_GAME_ACTIVE = false
        return false
    end
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
        if state and state ~= 'Lobby' and state ~= 'None' and state ~= '' then return true end
    end
    return false
end
_G.isInPlayableGame = isInPlayableGame

local function getCharacterAvatarComponent(uCharacter)
    if not uCharacter or not slua.isValid(uCharacter) then return nil end
    if uCharacter.CharacterAvatarComp2_BP and slua.isValid(uCharacter.CharacterAvatarComp2_BP) then
        return uCharacter.CharacterAvatarComp2_BP
    end
    if uCharacter.AvatarComponent2 and slua.isValid(uCharacter.AvatarComponent2) then
        return uCharacter.AvatarComponent2
    end
    if uCharacter.AvatarComponent and slua.isValid(uCharacter.AvatarComponent) then
        return uCharacter.AvatarComponent
    end
    if uCharacter.CharacterAvatarComp and slua.isValid(uCharacter.CharacterAvatarComp) then
        return uCharacter.CharacterAvatarComp
    end
    return nil
end

local function findAvatarSlotIndex(ApplyData, slotId)
    for i = 0, ApplyData:Num() - 1 do
        local eq = ApplyData:Get(i)
        if eq and eq.SlotID == slotId then return i end
    end
    return nil
end

local function ensureAvatarSlot(ApplyData, slotId)
    local idx = findAvatarSlotIndex(ApplyData, slotId)
    if idx then return idx end
    ApplyData:Add({ SlotID = slotId, ItemId = 0, AdditionalItemID = 0 })
    return ApplyData:Num() - 1
end

local function cacheDownloadItem(itemId)
    if not itemId or itemId == 0 then return end
    if not _G.skinIdCache[itemId] then
        pcall(_G.download_item, itemId)
        _G.skinIdCache[itemId] = true
    end
end

local function applyAvatarSlotSkin(ApplyData, slotId, targetSkinId, additionalItemId)
    if not targetSkinId or targetSkinId == 0 then return false end
    local idx = ensureAvatarSlot(ApplyData, slotId)
    local equipment = ApplyData:Get(idx)
    if not equipment then return false end
    local extra = additionalItemId or equipment.AdditionalItemID or 0
    if equipment.ItemId == targetSkinId and (not additionalItemId or equipment.AdditionalItemID == additionalItemId) then
        return false
    end
    cacheDownloadItem(targetSkinId)
    equipment.ItemId = targetSkinId
    if additionalItemId and additionalItemId > 0 then
        equipment.AdditionalItemID = additionalItemId
    end
    ApplyData:Set(idx, equipment)
    return true
end

local function refreshAvatarComp(avatarComp)
    pcall(function() avatarComp:OnRep_BodySlotStateChanged() end)
    pcall(function() avatarComp:OnAvatarMeshChanged() end)
    pcall(function()
        if avatarComp.RefreshAvatarDisplay then avatarComp:RefreshAvatarDisplay() end
    end)
    pcall(function()
        if avatarComp.UpdateAvatarDisplay then avatarComp:UpdateAvatarDisplay() end
    end)
    pcall(function()
        if avatarComp.ForceUpdateCharacterAppearance then avatarComp:ForceUpdateCharacterAppearance() end
    end)
end

local function tryAvatarCompEquip(avatarComp, slotId, skinId, additionalId)
    if not avatarComp or not skinId or skinId == 0 then return false end
    cacheDownloadItem(skinId)
    local changed = false
    pcall(function()
        if avatarComp.PutOnEquipmentBySlot then
            avatarComp:PutOnEquipmentBySlot(slotId, skinId, additionalId or 0)
            changed = true
        end
    end)
    pcall(function()
        if avatarComp.PutOnEquipment then
            avatarComp:PutOnEquipment(skinId, slotId, additionalId or 0)
            changed = true
        end
    end)
    pcall(function()
        if avatarComp.ChangeItemAvatar then
            avatarComp:ChangeItemAvatar(skinId, slotId, additionalId or 0)
            changed = true
        end
    end)
    pcall(function()
        if slotId == _G.CustSlotType.BackpackEquipemtSlot or slotId == _G.CustSlotType.HelmetEquipemtSlot then
            if avatarComp.UpdateEquipmentBySlot then
                avatarComp:UpdateEquipmentBySlot(slotId, skinId, additionalId or 0)
                changed = true
            end
            if avatarComp.SetEquipmentSlotData then
                avatarComp:SetEquipmentSlotData(slotId, skinId, additionalId or 0)
                changed = true
            end
        end
    end)
    return changed
end

local function readShirtFromConfigIni()
    for _, base in ipairs(loadSkinDataBases()) do
        local f = io.open(base .. '/config.ini', 'r')
        if f then
            local txt = f:read('*all') or ''
            f:close()
            local id = tonumber(txt:match('[%r\n]SHIRT=(%d+)')) or tonumber(txt:match('^SHIRT=(%d+)'))
            if id and id > 0 then return id end
        end
    end
    return 0
end

function _G.invalidateMatchOutfitCache(reason)
    _G.__matchOutfitOk = nil
    _G.__lastAppliedMatchOutfit = nil
    _G.__matchOutfitApplyLogged = nil
    _G.__lastGearAvatarTick = nil
    _G.__outfitProbeDone = nil
    _G.__matchOutfitProbeDone = nil
    _G.__outfitZeroLogged = nil
    _G.__matchCharMissLogged = nil
    _G.__avatarCompMissLogged = nil
    _G.__lastOutfitFailLog = nil
    _G.__outfitRetryChain = nil
    if reason then
        local now = os.clock()
        if _G.__lastOutfitCacheReason ~= reason or not _G.__lastOutfitCacheLog
            or (now - _G.__lastOutfitCacheLog) > 3 then
            _G.__lastOutfitCacheReason = reason
            _G.__lastOutfitCacheLog = now
            skinProbeLog('outfit cache cleared: ' .. tostring(reason))
        end
    end
end

local function resolveSuitSkinForAvatar()
    if isInPlayableGame() or isInActiveMatch() then
        pcall(function() if _G.ReadConfigFile then _G.ReadConfigFile(true) end end)
    end
    local suitSkin = tonumber(_G.AddOutfitLastLobbyOutfitRes) or tonumber(_G.__ACTIVE_OUTFIT_RES) or tonumber(_G.SuitSkin) or 0
    if _G.isLobbyOutfitRes and not _G.isLobbyOutfitRes(suitSkin) then suitSkin = 0 end
    if suitSkin <= 0 then
        suitSkin = readShirtFromConfigIni()
        if _G.isLobbyOutfitRes and not _G.isLobbyOutfitRes(suitSkin) then suitSkin = 0 end
    end
    if suitSkin > 0 then
        _G.__ACTIVE_OUTFIT_RES = suitSkin
        _G.SuitSkin = suitSkin
    end
    return suitSkin
end
_G.resolveMatchOutfitRes = resolveSuitSkinForAvatar

local function resolveOutfitInsId(suitSkin)
    suitSkin = tonumber(suitSkin) or 0
    if suitSkin <= 0 then return 0 end
    local ins = tonumber(_G.AddOutfitLastLobbyOutfitIns) or 0
    if ins > 0 then return ins end
    if _G.getOutfitInsId then
        return tonumber(_G.getOutfitInsId(suitSkin)) or 0
    end
    return 0
end

local function readClothesSlotRes(avatarComp, slots)
    if not avatarComp or not slua.isValid(avatarComp) then return 0 end
    local worn = 0
    pcall(function()
        local netData = avatarComp.NetAvatarData
        local applyData = netData and netData.SlotSyncData
        if applyData and slua.isValid(applyData) then
            local idx = findAvatarSlotIndex(applyData, slots.ClothesEquipemtSlot)
            if idx then
                local eq = applyData:Get(idx)
                if eq then worn = tonumber(eq.ItemId) or 0 end
            end
        end
    end)
    return worn
end

local function applySuitToCharacter(avatarComp, ApplyData, suitSkin, slots, outfitIns)
    if not suitSkin or suitSkin <= 0 then return false end
    outfitIns = tonumber(outfitIns) or resolveOutfitInsId(suitSkin)
    local changed = false
    cacheDownloadItem(suitSkin)
    pcall(function() if _G.download_item then _G.download_item(suitSkin) end end)
    if avatarComp then
        local addId = outfitIns > 0 and outfitIns or 0
        changed = tryAvatarCompEquip(avatarComp, slots.ClothesEquipemtSlot, suitSkin, addId) or changed
        pcall(function()
            if avatarComp.PutOnCustomEquipmentByID then
                avatarComp:PutOnCustomEquipmentByID(suitSkin)
                changed = true
            end
        end)
        pcall(function()
            if avatarComp.HandleEquipItem then
                avatarComp:HandleEquipItem(FItemDefineID(4, suitSkin), FAvatarCustomDefault())
                changed = true
            end
        end)
        if outfitIns > 0 then
            pcall(function()
                if avatarComp.PutOnEquipmentBySpecialID then
                    avatarComp:PutOnEquipmentBySpecialID(suitSkin, outfitIns)
                    changed = true
                end
            end)
        end
    end
    if ApplyData and slua.isValid(ApplyData) then
        changed = applyAvatarSlotSkin(ApplyData, slots.ClothesEquipemtSlot, suitSkin, outfitIns > 0 and outfitIns or nil) or changed
    end
    return changed
end

function _G.applySuitToMatchComp(avatarComp, suitSkin)
    if not avatarComp or not slua.isValid(avatarComp) then return false end
    suitSkin = tonumber(suitSkin) or 0
    if suitSkin <= 0 then return false end
    local outfitIns = resolveOutfitInsId(suitSkin)
    local slots = _G.CustSlotType
    local netData = avatarComp.NetAvatarData
    local ApplyData = netData and slua.isValid(netData) and netData.SlotSyncData
    local changed = applySuitToCharacter(avatarComp, ApplyData, suitSkin, slots, outfitIns)
    refreshAvatarComp(avatarComp)
    if readClothesSlotRes(avatarComp, slots) == suitSkin then
        return true
    end
    return changed
end

local function scheduleMatchOutfitRetry(suitSkin, attempt)
    if isInLobby() or not isInPlayableGame() then
        _G.__outfitRetryChain = nil
        return
    end
    attempt = tonumber(attempt) or 1
    if attempt > 20 then
        _G.__outfitRetryChain = nil
        return
    end
    if _G.__matchOutfitOk and _G.__lastAppliedMatchOutfit == suitSkin then
        _G.__outfitRetryChain = nil
        return
    end
    if _G.__outfitRetryChain == suitSkin and attempt == 1 then return end
    _G.__outfitRetryChain = suitSkin
    if not _G.Mytimer_ticker or not _G.Mytimer_ticker.AddTimerOnce then return end
    pcall(function()
        _G.Mytimer_ticker.AddTimerOnce(1.0 + (attempt * 0.5), function()
            if not isInPlayableGame() then
                _G.__outfitRetryChain = nil
                return
            end
            if _G.applyMatchOutfitNow and _G.applyMatchOutfitNow(true) then
                _G.__outfitRetryChain = nil
                return
            end
            scheduleMatchOutfitRetry(suitSkin, attempt + 1)
        end)
    end)
end

local function isLocalOutfitTarget(uCharacter)
    if not uCharacter or not slua.isValid(uCharacter) then return false end
    local isSelf = false
    pcall(function()
        if uCharacter.IsSelf and uCharacter:IsSelf() then isSelf = true end
        if uCharacter.bIsSelf then isSelf = true end
        if uCharacter.IsLocalPlayer and uCharacter:IsLocalPlayer() then isSelf = true end
    end)
    if isSelf then return true end
    local localChar = getSkinPlayerCharacter()
    return localChar and uCharacter == localChar
end
_G.isLocalOutfitTarget = isLocalOutfitTarget

local function collectOutfitTargets()
    local out, seen = {}, {}
    local function add(ch)
        if not ch or not slua.isValid(ch) then return end
        if not isLocalOutfitTarget(ch) then return end
        local key = tostring(ch)
        if seen[key] then return end
        seen[key] = true
        table.insert(out, ch)
    end
    add(getSkinPlayerCharacter())
    pcall(function()
        local pc = getSkinPlayerController()
        add(getCharacterFromController(pc))
    end)
    pcall(function()
        local m = require("client.logic.avatar.logic_team_avatar_manager")
        if m then
            if m.GetModelActor then add(m.GetModelActor()) end
            if m.GetShowingModel then add(m:GetShowingModel()) end
            if m.GetTeamAvatarActor then add(m:GetTeamAvatarActor()) end
            if m.GetMainAvatarActor then add(m:GetMainAvatarActor()) end
        end
    end)
    pcall(function()
        local UIUtil = require("client.common.ui_util")
        local gi = UIUtil and UIUtil.GetGameInstance and UIUtil.GetGameInstance()
        if not gi then return end
        local UGameplayStatics = import("GameplayStatics")
        if not UGameplayStatics then return end
        local uActor = import("Actor")
        for _, className in ipairs({ "STExtraLobbyCharacter", "UAECharacter", "STExtraBaseCharacter" }) do
            pcall(function()
                local cls = import(className)
                if cls then
                    local arr = UGameplayStatics.GetAllActorsOfClass(
                        gi, cls, slua.Array(UEnums.EPropertyClass.Object, uActor))
                    if arr then
                        for _, actor in pairs(arr) do
                            add(actor)
                        end
                    end
                end
            end)
        end
    end)
    pcall(function()
        local ModuleManager = require("client.module_framework.ModuleManager")
        if not ModuleManager or not ModuleManager.LobbyModuleConfig then return end
        local cfg = ModuleManager.LobbyModuleConfig
        for _, key in ipairs({ "LobbyModel", "logic_lobby_main", "LobbyAvatar" }) do
            local modName = cfg[key]
            if modName then
                local mod = ModuleManager.GetModule(modName)
                if mod then
                    if mod.GetShowModel then add(mod:GetShowModel()) end
                    if mod.GetLobbyModel then add(mod:GetLobbyModel()) end
                    if mod.GetModelActor then add(mod:GetModelActor()) end
                end
            end
        end
    end)
    return out
end
_G.collectOutfitTargets = collectOutfitTargets
_G.getCharacterFromController = getCharacterFromController
_G.getSkinPlayerCharacter = getSkinPlayerCharacter

local function applyOutfitViaWardrobe(itemId)
    if not itemId or itemId == 0 then return false end
    cacheDownloadItem(itemId)
    local ok = false
    pcall(function()
        local ModuleManager = require("client.module_framework.ModuleManager")
        if not ModuleManager or not ModuleManager.CommonModuleConfig then return end
        local cfg = ModuleManager.CommonModuleConfig
        for _, key in ipairs({ "logic_wardrobe", "logic_wardrobe_new", "WardrobeLogic" }) do
            local modName = cfg[key]
            if modName then
                local mod = ModuleManager.GetModule(modName)
                if mod then
                    if mod.PutOnEquipment then mod:PutOnEquipment(itemId); ok = true end
                    if mod.putOnEquipment then mod:putOnEquipment(itemId); ok = true end
                    if mod.ForcePutOnEquipment then mod:ForcePutOnEquipment(itemId); ok = true end
                end
            end
        end
    end)
    return ok
end
_G.applyOutfitViaWardrobe = applyOutfitViaWardrobe

function _G.equip_character_avatar(uCharacter)
    if not uCharacter or not slua.isValid(uCharacter) then return false end
    local inMatch = isInPlayableGame()
    if inMatch then
        local localChar = getMatchPlayerCharacter()
        if localChar and uCharacter ~= localChar then
            local isSelf = false
            pcall(function()
                if uCharacter.IsSelf and uCharacter:IsSelf() then isSelf = true end
                if uCharacter.bIsSelf then isSelf = true end
            end)
            if not isSelf then return false end
        end
    elseif not isLocalOutfitTarget(uCharacter) then
        return false
    end
    local avatarComp = getCharacterAvatarComponent(uCharacter)
    if not avatarComp then
        if inMatch and not isInLobby() and not _G.__avatarCompMissLogged then
            _G.__avatarCompMissLogged = true
            skinProbeLog('outfit: no avatarComp on match character')
        end
        return false
    end

    local netData = avatarComp.NetAvatarData
    local ApplyData = netData and slua.isValid(netData) and netData.SlotSyncData
    local BackpackUtils = import("BackpackUtils")
    local changed = false
    local slots = _G.CustSlotType
    local suitSkin = resolveSuitSkinForAvatar()
    if suitSkin <= 0 and inMatch and not _G.__outfitZeroLogged then
        _G.__outfitZeroLogged = true
        skinProbeLog('outfit missing: set SHIRT in config or select outfit in lobby')
    end

    if suitSkin > 0 and not inMatch then
        changed = applySuitToCharacter(avatarComp, ApplyData, suitSkin, slots) or changed
        pcall(function()
            if GameStatus and GameStatus.IsInLobbyOrMainCity and GameStatus.IsInLobbyOrMainCity() then
                applyOutfitViaWardrobe(suitSkin)
            end
        end)
    end
    if _G.HatSkin and _G.HatSkin > 0 then
        changed = tryAvatarCompEquip(avatarComp, slots.HatEquipemtSlot, _G.HatSkin, 0) or changed
    end
    if _G.FaceSkin and _G.FaceSkin > 0 then
        changed = tryAvatarCompEquip(avatarComp, slots.FaceEquipemtSlot, _G.FaceSkin, 0) or changed
    end
    if _G.MaskSkin and _G.MaskSkin > 0 then
        changed = tryAvatarCompEquip(avatarComp, slots.FaceEquipemtSlot, _G.MaskSkin, 0) or changed
    end
    if _G.GlovesSkin and _G.GlovesSkin > 0 then
        changed = tryAvatarCompEquip(avatarComp, slots.HandEffectEquipemtSlot, _G.GlovesSkin, 0) or changed
    end
    if _G.PantSkin and _G.PantSkin > 0 then
        changed = tryAvatarCompEquip(avatarComp, slots.PantsEquipemtSlot, _G.PantSkin, 0) or changed
    end
    if _G.ShoeSkin and _G.ShoeSkin > 0 then
        changed = tryAvatarCompEquip(avatarComp, slots.ShoesEquipemtSlot, _G.ShoeSkin, 0) or changed
    end
    if _G.GliderSkin and _G.GliderSkin > 0 then
        changed = tryAvatarCompEquip(avatarComp, slots.GlideEquipemtSlot, _G.GliderSkin, 0) or changed
    end
    if _G.ParachuteSkin and _G.ParachuteSkin > 0 then
        changed = tryAvatarCompEquip(avatarComp, slots.ParachuteEquipemtSlot, _G.ParachuteSkin, 0) or changed
    end

    local bagSkin, bagBase, bagLvl, helmSkin, helmBase, helmLvl = resolveGearVisualState(
        uCharacter, ApplyData, BackpackUtils, slots, inMatch)

    if bagSkin and bagSkin > 0 and bagLvl then
        changed = tryAvatarCompEquip(avatarComp, slots.BackpackEquipemtSlot, bagSkin, bagBase) or changed
    end
    if helmSkin and helmSkin > 0 and helmLvl then
        changed = tryAvatarCompEquip(avatarComp, slots.HelmetEquipemtSlot, helmSkin, helmBase) or changed
    end

    if ApplyData and slua.isValid(ApplyData) then
        changed = applyAvatarSlotSkin(ApplyData, slots.HatEquipemtSlot, _G.HatSkin) or changed
        changed = applyAvatarSlotSkin(ApplyData, slots.FaceEquipemtSlot, _G.FaceSkin) or changed
        if _G.MaskSkin and _G.MaskSkin ~= 0 and _G.MaskSkin ~= _G.FaceSkin then
            changed = applyAvatarSlotSkin(ApplyData, slots.FaceEquipemtSlot, _G.MaskSkin) or changed
        end
        changed = applyAvatarSlotSkin(ApplyData, slots.HandEffectEquipemtSlot, _G.GlovesSkin) or changed
        changed = applyAvatarSlotSkin(ApplyData, slots.PantsEquipemtSlot, _G.PantSkin) or changed
        changed = applyAvatarSlotSkin(ApplyData, slots.ShoesEquipemtSlot, _G.ShoeSkin) or changed
        changed = applyAvatarSlotSkin(ApplyData, slots.GlideEquipemtSlot, _G.GliderSkin) or changed
        changed = applyAvatarSlotSkin(ApplyData, slots.ParachuteEquipemtSlot, _G.ParachuteSkin) or changed
        if bagSkin and bagSkin > 0 and bagLvl then
            changed = applyAvatarSlotSkin(ApplyData, slots.BackpackEquipemtSlot, bagSkin, bagBase) or changed
        end
        if helmSkin and helmSkin > 0 and helmLvl then
            changed = applyAvatarSlotSkin(ApplyData, slots.HelmetEquipemtSlot, helmSkin, helmBase) or changed
        end
    end

    if suitSkin > 0 then
        changed = applySuitToCharacter(avatarComp, ApplyData, suitSkin, slots, resolveOutfitInsId(suitSkin)) or changed
    end

    if changed then refreshAvatarComp(avatarComp) end
    if inMatch then
        if not _G.__matchOutfitProbeDone then
            _G.__matchOutfitProbeDone = true
            pcall(function()
                skinProbeLog('outfit shirt=' .. tostring(suitSkin)
                    .. ' bag=' .. tostring(bagSkin) .. ' bagLvl=' .. tostring(bagLvl)
                    .. ' helm=' .. tostring(helmSkin) .. ' helmLvl=' .. tostring(helmLvl)
                    .. ' inMatch=true'
                    .. ' changed=' .. tostring(changed)
                    .. ' hasComp=' .. tostring(avatarComp ~= nil))
            end)
        end
    elseif not _G.__outfitProbeDone then
        _G.__outfitProbeDone = true
        pcall(function()
            skinProbeLog('outfit shirt=' .. tostring(suitSkin)
                .. ' bag=' .. tostring(bagSkin) .. ' bagLvl=' .. tostring(bagLvl)
                .. ' helm=' .. tostring(helmSkin) .. ' helmLvl=' .. tostring(helmLvl)
                .. ' inMatch=false'
                .. ' changed=' .. tostring(changed)
                .. ' hasComp=' .. tostring(avatarComp ~= nil))
        end)
    end
    return changed
end

function _G.applyMatchOutfitNow(force)
    if isInLobby() then return false end
    local inMatch = isInPlayableGame()
    if not inMatch then return false end
    local suitSkin = resolveSuitSkinForAvatar()
    if suitSkin <= 0 then
        local now = os.clock()
        if not _G.__lastOutfitFailLog or (now - _G.__lastOutfitFailLog) > 15 then
            _G.__lastOutfitFailLog = now
            skinProbeLog('applyMatchOutfitNow: no suitSkin (SHIRT=0 in config?)')
        end
        return false
    end
    if not force and _G.__matchOutfitOk and _G.__lastAppliedMatchOutfit == suitSkin then
        return true
    end
    if _G.__lastAppliedMatchOutfit and _G.__lastAppliedMatchOutfit ~= suitSkin then
        pcall(function() _G.invalidateMatchOutfitCache('outfit res changed') end)
    end
    local outfitIns = resolveOutfitInsId(suitSkin)
    if outfitIns > 0 then _G.AddOutfitLastLobbyOutfitIns = outfitIns end
    local char = getMatchPlayerCharacter()
    if not char then
        scheduleMatchOutfitRetry(suitSkin, 1)
        local now = os.clock()
        if not _G.__lastOutfitFailLog or (now - _G.__lastOutfitFailLog) > 15 then
            _G.__lastOutfitFailLog = now
            skinProbeLog('applyMatchOutfitNow: no character shirt=' .. suitSkin)
        end
        return false
    end
    pcall(function() if _G.download_item then _G.download_item(suitSkin) end end)
    local comp = getCharacterAvatarComponent(char)
    if not comp and char.CharacterAvatarComp2_BP and slua.isValid(char.CharacterAvatarComp2_BP) then
        comp = char.CharacterAvatarComp2_BP
    end
    local suitOk = false
    local worn = 0
    if comp and slua.isValid(comp) then
        suitOk = _G.applySuitToMatchComp(comp, suitSkin)
        worn = readClothesSlotRes(comp, _G.CustSlotType)
    end
    pcall(function() _G.equip_character_avatar(char) end)
    if suitOk then
        _G.__lastAppliedMatchOutfit = suitSkin
        _G.__matchOutfitOk = true
        skinProbeLog('match outfit apply shirt=' .. suitSkin
            .. ' ins=' .. tostring(outfitIns)
            .. ' slot=' .. tostring(worn) .. ' ok=true')
    else
        scheduleMatchOutfitRetry(suitSkin, 1)
        local now = os.clock()
        if not _G.__lastOutfitFailLog or (now - _G.__lastOutfitFailLog) > 8 then
            _G.__lastOutfitFailLog = now
            skinProbeLog('applyMatchOutfitNow: suit apply failed shirt=' .. suitSkin
                .. ' ins=' .. tostring(outfitIns)
                .. ' hasComp=' .. tostring(comp ~= nil)
                .. ' slot=' .. tostring(worn))
        end
    end
    return suitOk
end

function _G.applyMatchOutfitIfNeeded(force)
    return _G.applyMatchOutfitNow(force)
end

function _G.HandlePetLogic()
    pcall(function()
        if not _G.PetSkin or _G.PetSkin == 0 or _G.PetSkin == 50000 then return end
        if _G.PetSkin == _G.LastAppliedPet then
            _G.__petRetryTick = (_G.__petRetryTick or 0) + 1
            local every = isInActiveMatch() and 8 or 4
            if _G.__petRetryTick % every ~= 0 then return end
        end
        cacheDownloadItem(_G.PetSkin)

        local ModuleManager = require("client.module_framework.ModuleManager")
        if ModuleManager and ModuleManager.CommonModuleConfig then
            local logic_pet = ModuleManager.GetModule(ModuleManager.CommonModuleConfig.logic_pet)
            if logic_pet then
                if logic_pet.SetCurPetID then logic_pet:SetCurPetID(_G.PetSkin) end
                if logic_pet.EquipPet then logic_pet:EquipPet(_G.PetSkin) end
                if logic_pet.CreatePet then logic_pet:CreatePet(_G.PetSkin) end
                if logic_pet.ShowPet then logic_pet:ShowPet(_G.PetSkin) end
            end
        end

        local pc = getSkinPlayerController()
        if pc and slua.isValid(pc) then
            if pc.InitialPetInfo then pc.InitialPetInfo.PetId = _G.PetSkin end
            if pc.PetComponent and slua.isValid(pc.PetComponent) then
                if pc.PetComponent.SetPetID then pc.PetComponent:SetPetID(_G.PetSkin) end
                if pc.PetComponent.CreatePet then pc.PetComponent:CreatePet(_G.PetSkin) end
            end
        end
        _G.LastAppliedPet = _G.PetSkin
    end)
end

function _G.DeadBox_TemperRequest(PlayerController)
    if not _G.WeaponSkinIndex or not next(_G.WeaponSkinIndex) then return end
    local uCharacter = PlayerController:GetPlayerCharacterSafety()
    if not uCharacter then return end
    
    local UGameplayStatics = import("GameplayStatics")
    if UGameplayStatics then
        local uActor = import("Actor")
        local UIUtil = require("client.common.ui_util")
        if UIUtil then
            local uGameInstance = UIUtil.GetGameInstance()
            if uGameInstance then
                local APlayerTombBox = import("PlayerTombBox")
                local uActorArray = UGameplayStatics.GetAllActorsOfClass(uGameInstance, APlayerTombBox, slua.Array(UEnums.EPropertyClass.Object, uActor))
                for _, actor in pairs(uActorArray) do
                    if _G.IsPtrValid(actor) then
                        local DamageCauser = actor.DamageCauser
                        if DamageCauser and DamageCauser.Playerkey == PlayerController.Playerkey then
                            local Deadboxavatar = actor.DeadBoxAvatarComponent_BP
                            if Deadboxavatar and not table.contains(_G.AlreadyChangedSet, actor) then
                                local actorLocation = actor:K2_GetActorLocation()
                                local found = false
                                for _, entry in pairs(_G.DeadBoxSkins) do
                                    if locationsClose(entry.location, actorLocation, 1.0) then
                                        Deadboxavatar:ResetItemAvatar()
                                        Deadboxavatar:PreChangeItemAvatar(entry.SkinID)
                                        Deadboxavatar:SyncChangeItemAvatar(entry.SkinID)
                                        table.insert(_G.AlreadyChangedSet, actor)
                                        found = true
                                        break
                                    end
                                end
                                if not found then
                                    local ApplySkinID = 0
                                    local CurrentVehicle = uCharacter.CurrentVehicle
                                    if CurrentVehicle and _G.CurrentEquipVehicleID ~= 0 then
                                        ApplySkinID = tostring(_G.CurrentEquipVehicleID) .. "1"
                                    else
                                        local currweapon = uCharacter:GetCurrentWeapon()
                                        if currweapon then
                                            ApplySkinID = slua.IndexReference(currweapon.synData:Get(7), "defineID").TypeSpecificID
                                        end
                                    end
                                    if ApplySkinID and ApplySkinID ~= 0 then
                                        Deadboxavatar:ResetItemAvatar()
                                        Deadboxavatar:PreChangeItemAvatar(ApplySkinID)
                                        Deadboxavatar:SyncChangeItemAvatar(ApplySkinID)
                                        table.insert(_G.DeadBoxSkins, { location = actorLocation, SkinID = ApplySkinID })
                                        table.insert(_G.AlreadyChangedSet, actor)
                                    end
                                end
                            end
                        end
                    end
                end
            end
        end
    end
end

function _G.GameAvatarHandlerDeadBox()
    local PlayerController = slua_GameFrontendHUD and slua_GameFrontendHUD:GetPlayerController()
    if PlayerController then
        _G.DeadBox_TemperRequest(PlayerController)
    end
end

function _G.GameAvatarHandlerplayers()
    local inGame = isInPlayableGame()
    local desiredOutfit = inGame and resolveSuitSkinForAvatar() or 0
    local gearStateKey = nil
    if inGame then
        local ch = getMatchPlayerCharacter()
        if ch and slua.isValid(ch) then
            local BU = import("BackpackUtils")
            local bL = getCharacterBattleGearLevel(ch, "bag", BU)
            if not bL then bL = getGearLevelFromInventory(ch, "bag", BU) end
            local hL = getCharacterBattleGearLevel(ch, "helmet", BU)
            if not hL then hL = getGearLevelFromInventory(ch, "helmet", BU) end
            gearStateKey = tostring(bL) .. ":" .. tostring(hL)
        end
    end
    if inGame and desiredOutfit > 0
        and _G.__lastAppliedMatchOutfit
        and _G.__lastAppliedMatchOutfit ~= desiredOutfit then
        pcall(function() _G.invalidateMatchOutfitCache('lobby outfit changed') end)
        pcall(function() _G.applyMatchOutfitNow(true) end)
    elseif inGame and _G.__matchOutfitOk and desiredOutfit > 0
        and _G.__lastAppliedMatchOutfit == desiredOutfit then
        local now = os.clock()
        local sameGear = gearStateKey and gearStateKey == _G.__lastGearStateKey
        if sameGear and _G.__lastGearAvatarTick and (now - _G.__lastGearAvatarTick) < 15 then
            pcall(_G.HandlePetLogic)
            return
        end
        _G.__lastGearAvatarTick = now
        _G.__lastGearStateKey = gearStateKey
    end
    local targets = {}
    local inMatch = isInPlayableGame()
    if inMatch then
        local ch = getMatchPlayerCharacter()
        if ch and slua.isValid(ch) then
            targets = { ch }
        end
    else
        targets = collectOutfitTargets()
        if #targets == 0 then
            local ch = getSkinPlayerCharacter()
            if ch and slua.isValid(ch) then
                targets = { ch }
            end
        end
    end
    for _, uChar in ipairs(targets) do
        pcall(function() _G.equip_character_avatar(uChar) end)
    end
    _G.HandlePetLogic()
end


function _G.ApplyLobbyTheme()
    if isInActiveMatch() then return end
    pcall(function()
        local themeID = _G.TargetLobbyThemeID
        if not themeID or themeID == 0 then return end
        if _G.LastAppliedThemeID == themeID then return end
        cacheDownloadItem(themeID)
        local ModuleManager = require('client.module_framework.ModuleManager')
        if not ModuleManager then return end
        local LobbyThemeManager = ModuleManager.GetModule(ModuleManager.LobbyModuleConfig.LobbyThemeManager)
        if LobbyThemeManager then
            local ok = false
            if LobbyThemeManager.ShowThemeByItemID then
                pcall(function() LobbyThemeManager:ShowThemeByItemID(themeID) end)
                ok = true
            end
            if LobbyThemeManager.SetTheme then
                pcall(function() LobbyThemeManager:SetTheme(themeID) end)
                ok = true
            end
            if LobbyThemeManager.ApplyTheme then
                pcall(function() LobbyThemeManager:ApplyTheme(themeID) end)
                ok = true
            end
            if ok then _G.LastAppliedThemeID = themeID end
        end
    end)
end

function _G.CheckLobbyThemeChanges()
    pcall(function()
        local oldID = _G.TargetLobbyThemeID
        _G.ReadConfigFile(true)
        if _G.TargetLobbyThemeID ~= oldID then
            _G.LastAppliedThemeID = nil
            _G.ApplyLobbyTheme()
        end
    end)
end

function _G.TryShowWelcome()
    pcall(function()
        local CommonMsgBoxMgr = require("client.slua.logic.common.logic_common_msg_box")
        if not CommonMsgBoxMgr then return end
        local activeStatus = "Anubis Menu & Status\n"
        activeStatus = activeStatus .. "\nWeapon Skins: Active"
        activeStatus = activeStatus .. "\nKill Counter: Active"
        activeStatus = activeStatus .. "\nOutfit Skins: Active"
        activeStatus = activeStatus .. "\nLobby Theme: Active"
        activeStatus = activeStatus .. "\nDeadBox Skins: Active"
        activeStatus = activeStatus .. "\nVehicle Skins: Active"
        activeStatus = activeStatus .. "\n\nConfigure your values in config.ini and changes will apply automatically without UI hooks.\n\nEnjoy Anubis!"
        CommonMsgBoxMgr.Show(1, "Anubis MENU", activeStatus, function() end)
        _G.WelcomeShown = true
    end)
end

function _G.GetKillCounterPath()
    for _, base in ipairs(loadSkinDataBases()) do
        local path = base .. '/NumberUpdate.txt'
        local file = io.open(path, 'r')
        if file then file:close() return path end
    end
    for _, base in ipairs(loadSkinDataBases()) do
        local f = io.open(base .. '/config.ini', 'r')
        if f then f:close() return base .. '/NumberUpdate.txt' end
    end
    return '/storage/emulated/0/Android/data/com.pubg.imobile/files/NumberUpdate.txt'
end

_G.ActiveKillCounterPath = nil

local function saveKillCountToFile()
    if not _G.ActiveKillCounterPath then _G.ActiveKillCounterPath = _G.GetKillCounterPath() end
    local file = io.open(_G.ActiveKillCounterPath, 'w+')
    if not file then return end
    local content = '{\n'
    for weaponID, count in pairs(_G.killCountInfo) do
        content = content .. string.format('    [%d] = %d,\n', weaponID, count)
    end
    content = content .. '}'
    file:write(content)
    file:close()
    _G.lastFileContent = content
end

function _G.loadKillCountFromFile()
    if not _G.ActiveKillCounterPath then _G.ActiveKillCounterPath = _G.GetKillCounterPath() end
    local file = io.open(_G.ActiveKillCounterPath, 'r')
    if file then
        local content = file:read('*a')
        file:close()
        _G.lastFileContent = content
        if content ~= '' then
            content = content:gsub('\239\187\191', ''):gsub('^%s+', '')
            local tempTable = {}
            for weaponID, count in content:gmatch('%[(%d+)%]%s*=%s*(%d+)') do
                tempTable[tonumber(weaponID)] = tonumber(count)
            end
            if next(tempTable) then _G.killCountInfo = tempTable end
        end
    end
end

function _G.getKills(weaponID)
    return weaponID and _G.killCountInfo[weaponID] or 0
end

function _G.addKill(weaponID, count)
    if not weaponID or not count then return end
    local currentTime = os.clock()
    if _G.LastKillTime[weaponID] and (currentTime - _G.LastKillTime[weaponID]) < 0.5 then return end
    _G.LastKillTime[weaponID] = currentTime
    _G.killCountInfo[weaponID] = (_G.killCountInfo[weaponID] or 0) + count
    pcall(saveKillCountToFile)
    local PlayerController = slua_GameFrontendHUD and slua_GameFrontendHUD:GetPlayerController()
    if PlayerController then
        local uCharacter = PlayerController:GetPlayerCharacterSafety()
        if uCharacter then
            local currweapon = uCharacter:GetCurrentWeapon()
            if currweapon then
                local SkinID = slua.IndexReference(currweapon.synData:Get(7), "defineID").TypeSpecificID
                if _G.OurkillCountSystem then
                    _G.OurkillCountSystem:UpdateMainKillCounterUI(true, weaponID, SkinID)
                end
            end
        end
    end
end

function _G.ForceUpdateKillCounterUI()
    pcall(function()
        local PlayerController = slua_GameFrontendHUD and slua_GameFrontendHUD:GetPlayerController()
        if not PlayerController or not slua.isValid(PlayerController) then return end
        local uCharacter = PlayerController:GetPlayerCharacterSafety()
        if not uCharacter or not slua.isValid(uCharacter) then return end
        local currweapon = uCharacter:GetCurrentWeapon()
        if not currweapon or not slua.isValid(currweapon) then return end
        local DefineID = currweapon:GetItemDefineID() and currweapon:GetItemDefineID().TypeSpecificID or 0
        if DefineID == 0 then return end
        local currentEquipAvatarID = slua.IndexReference(currweapon.synData:Get(7), "defineID").TypeSpecificID
        local UIManager = require("client.slua_ui_framework.manager")
        local MainKillCounter = UIManager.GetUI(UIManager.UI_Config_InGame.MainKillCounter)
        if MainKillCounter and slua.isValid(MainKillCounter) then
            local ModuleManager = require("client.module_framework.ModuleManager")
            local LogicKillCounter = ModuleManager.GetModule(ModuleManager.CommonModuleConfig.LogicKillCounter)
            local curEquipedKillCounter = LogicKillCounter:GetEquipedKillCounterId(6114302174, currentEquipAvatarID)
            if not curEquipedKillCounter or curEquipedKillCounter == 0 then
                curEquipedKillCounter = LogicKillCounter:GetBaseKillCounterIdByWeaponId(DefineID)
            end
            local kills = _G.getKills(DefineID)
            MainKillCounter:SetKillCounterItemShowWithNum(curEquipedKillCounter, kills, currentEquipAvatarID)
            if MainKillCounter.KillCounterItem and MainKillCounter.KillCounterItem.SetVisibility then
                local ESlateVisibility = import("ESlateVisibility")
                MainKillCounter.KillCounterItem:SetVisibility(ESlateVisibility.Collapsed)
                MainKillCounter.KillCounterItem:SetVisibility(ESlateVisibility.SelfHitTestInvisible)
            end
        end
    end)
end

function _G.FileWatcher()
    if not _G.isFileWatcherActive then return end
    pcall(function()
        if not _G.ActiveKillCounterPath then _G.ActiveKillCounterPath = _G.GetKillCounterPath() end
        local file = io.open(_G.ActiveKillCounterPath, 'r')
        if not file then return end
        local currentContent = file:read('*a') or ""
        file:close()
        currentContent = currentContent:gsub('\239\187\191', ''):gsub('^%s+', ''):gsub('%s+$', '')
        if currentContent == "" or currentContent == _G.lastFileContent then return end
        _G.lastFileContent = currentContent
        local tempTable = {}
        for weaponID, count in currentContent:gmatch('%[(%d+)%]%s*=%s*(%d+)') do
            tempTable[tonumber(weaponID)] = tonumber(count)
        end
        if not next(tempTable) then return end
        _G.killCountInfo = tempTable
        _G.ForceUpdateKillCounterUI()
    end)
end

pcall(function()
    local MyDamageNumMainUI = require("GameLua.Mod.Library.Client.UI.DamageNumMainUI")
    if MyDamageNumMainUI then
        local UWidgetLayoutLibrary = import("WidgetLayoutLibrary")
        local GameplayData = require("GameLua.GameCore.Data.GameplayData")
        MyDamageNumMainUI.__inner_impl.ShowDamage = function(self, Damage, X, Y, Z, uFSlateColor, nFontSize)
            if not self.FlyNumItemPool or Damage == 0 then return end
            local Item = self.FlyNumItemPool:GetOneItem()
            self.UIRoot.CanvasPanel_28:AddChild(Item)
            local damageInfo = { item = Item, worldPosition = FVector(X, Y, Z), updateHandle = nil }
            local uPlayerController = GameplayData.GetPlayerController()
            local function UpdateScreenPosition()
                if slua.isValid(damageInfo.item) then
                    local ScreenPos = UWidgetLayoutLibrary.ProjectWorldLocationToWidgetPositionReturnValue(uPlayerController, damageInfo.worldPosition)
                    if ScreenPos then damageInfo.item:SetRenderTranslation(ScreenPos) end
                end
            end
            UpdateScreenPosition()
            damageInfo.updateHandle = self:AddGameTimer(0.016, true, function()
                if slua.isValid(damageInfo.item) then UpdateScreenPosition()
                else if damageInfo.updateHandle then self:RemoveGameTimer(damageInfo.updateHandle) end end
            end)
            Item.DamageText:SetText(tostring(Damage))
            if slua.isValid(uFSlateColor) then Item.DamageText:SetColorAndOpacity(uFSlateColor)
            else Item.DamageText:SetColorAndOpacity(FSlateColor(FLinearColor(1, 1, 1, 1))) end
            local Font = Item.DamageText.Font
            Font.Size = (nFontSize and type(nFontSize) == "number") and nFontSize or 18
            Item.DamageText:SetFont(Font)
            local animTime = 5.0
            if _G.bFadeAnim then
                Item:PlayAnimation(Item.Fadein, 0, 1, 0, 1)
                animTime = Item.Fadein:GetEndTime()
            end
            self:AddGameTimer(animTime, false, function()
                if slua.isValid(Item) then
                    if damageInfo.updateHandle then self:RemoveGameTimer(damageInfo.updateHandle) end
                    self.FlyNumItemPool:FreeOneItem(Item)
                end
            end)
        end
    end
end)

local function installOutfitHooks()
    if _G.__SKIN_OUTFIT_HOOKS then return end
    _G.__SKIN_OUTFIT_HOOKS = true
    pcall(function()
        local lobby_main = require("client.slua.umg.lobby.Main.Lobby_Main_UIBP")
        if lobby_main and lobby_main.__inner_impl then
            for _, methodName in ipairs({ "OnShow", "OnOpen", "Construct", "RegistEvents" }) do
                local orig = lobby_main.__inner_impl[methodName]
                if orig then
                    lobby_main.__inner_impl[methodName] = function(self, ...)
                        local ret = orig(self, ...)
                        pcall(function()
                            _G.__PLAYABLE_GAME_ACTIVE = false
                            _G.__outfitRetryChain = nil
                            _G.ReadConfigFile(true)
                            _G.ApplyLobbyTheme()
                            _G.GameAvatarHandlerplayers()
                        end)
                        return ret
                    end
                end
            end
        end
    end)
    pcall(function()
        local function allowOutfitRes(resID)
            if _G.isAllowedOutfitRes and _G.isAllowedOutfitRes(resID) then return true end
            local suit = tonumber(_G.SuitSkin) or 0
            if suit > 0 and tonumber(resID) == suit then return true end
            if _G.isInjectedSkinRes and _G.isInjectedSkinRes(resID) then return true end
            return false
        end
        local function hookCheck(comp)
            if not comp or not comp.CheckItemValid then return end
            local o = comp.CheckItemValid
            comp.CheckItemValid = function(self, resID)
                if allowOutfitRes(resID) then return true end
                return o(self, resID)
            end
        end
        hookCheck(require("GameLua.Mod.Library.GamePlay.Avatar.Component.CharacterAvatarComponent"))
        pcall(function()
            hookCheck(require("GameLua.Mod.Library.GamePlay.Avatar.Component.CharacterAvatarComponent2"))
        end)
    end)
    pcall(function()
        local function hookMeshOutfitApply(CAC)
            if not CAC or not CAC.OnAvatarAllMeshLoadedLua then return end
            local orig = CAC.OnAvatarAllMeshLoadedLua
            CAC.OnAvatarAllMeshLoadedLua = function(self)
                orig(self)
                pcall(function()
                    if self.IsLobbyActor and self:IsLobbyActor() then return end
                    local isSelf = false
                    pcall(function()
                        if self.IsSelf and self:IsSelf() then isSelf = true end
                    end)
                    if not isSelf or isInLobby() or not isInPlayableGame() then return end
                    local suitSkin = resolveSuitSkinForAvatar()
                    if suitSkin <= 0 or not _G.applySuitToMatchComp then return end
                    local ok = _G.applySuitToMatchComp(self, suitSkin)
                    if ok then
                        _G.__lastAppliedMatchOutfit = suitSkin
                        _G.__matchOutfitOk = true
                        skinProbeLog('mesh outfit apply shirt=' .. suitSkin .. ' ok=true')
                    end
                end)
            end
        end
        hookMeshOutfitApply(require("GameLua.Mod.Library.GamePlay.Avatar.Component.CharacterAvatarComponent"))
        pcall(function()
            hookMeshOutfitApply(require("GameLua.Mod.Library.GamePlay.Avatar.Component.CharacterAvatarComponent2"))
        end)
    end)
end

local function installWeaponEventHooks()
    if _G.__SKIN_WEAPON_HOOKS then return end
    _G.__SKIN_WEAPON_HOOKS = true
    _G.WeaponEvents = _G.WeaponEvents or { onWeaponChanged = function() end }
    _G.WeaponEvents.onWeaponChanged = function(weaponId)
        pcall(function()
            if not _G.WeaponSkinIndex or not next(_G.WeaponSkinIndex) then
                _G.ReadConfigFile()
            end
            local uCharacter = getSkinPlayerCharacter()
            if uCharacter then
                local currweapon = uCharacter:GetCurrentWeapon()
                if currweapon and slua.isValid(currweapon) then
                    _G.ProcessOneWeapon(currweapon, true)
                end
            end
        end)
        pcall(function()
            local uCharacter = getSkinPlayerCharacter()
            if not uCharacter or not _G.OurkillCountSystem then return end
            local currweapon = uCharacter:GetCurrentWeapon()
            if not currweapon then return end
            local DefineID = currweapon:GetItemDefineID().TypeSpecificID
            local SkinID = slua.IndexReference(currweapon.synData:Get(7), "defineID").TypeSpecificID
            _G.OurkillCountSystem:UpdateMainKillCounterUI(true, DefineID, SkinID)
        end)
    end
    pcall(function()
        local SlotBase = require("GameLua.Mod.BaseMod.Client.MainControlUI.SwitchWeaponSlotMode2")
        if not SlotBase or not SlotBase.__inner_impl then return end
        for _, methodName in ipairs({ "SwitchWeapon", "OnTouchWeaponSlot", "EquipWeaponBySlot" }) do
            local orig = SlotBase.__inner_impl[methodName]
            if orig then
                SlotBase.__inner_impl[methodName] = function(self, ...)
                    local ret = orig(self, ...)
                    pcall(function()
                        local uCharacter = getSkinPlayerCharacter()
                        if uCharacter then
                            _G.ProcessOneWeapon(uCharacter:GetCurrentWeapon(), true)
                        end
                    end)
                    return ret
                end
            end
        end
    end)
end

local function installExtendedSkinHooks()
    if _G.__SKIN_EXTENDED_HOOKS then return end
    _G.__SKIN_EXTENDED_HOOKS = true
    installWeaponEventHooks()
    installOutfitHooks()

    pcall(function()
local SKillInfo = require("GameLua.Mod.BaseMod.Client.KillInfoTips.KillInfo")
local ECharacterHealthStatus = import("ECharacterHealthStatus")
local SKillInfoModuleManager = require("client.module_framework.ModuleManager")
local O_FileItem = SKillInfo.__inner_impl.FileItem

SKillInfo.__inner_impl.FileItem = function(self, DamageRecordData)
    if not self or not DamageRecordData then return O_FileItem(self, DamageRecordData) end
    local LogicKillCounter = SKillInfoModuleManager.GetModule(SKillInfoModuleManager.CommonModuleConfig.LogicKillCounter)
    if not LogicKillCounter then return O_FileItem(self, DamageRecordData) end
    local uCharacter = slua_GameFrontendHUD and slua_GameFrontendHUD:GetPlayerController() and slua_GameFrontendHUD:GetPlayerController():GetPlayerCharacterSafety()
    if not uCharacter or not slua.isValid(uCharacter) then return O_FileItem(self, DamageRecordData) end
    local SelfName = uCharacter:GetPlayerNameSafety()
    if DamageRecordData.Causer == SelfName then
        local currWeapon = uCharacter:GetCurrentWeapon()
        if currWeapon and slua.isValid(currWeapon) then
            local DefineID = currWeapon:GetItemDefineID() and currWeapon:GetItemDefineID().TypeSpecificID or 0
            if DefineID ~= 0 then
                local ExpandData = slua.LuaArchiverDecode(LuaStateWrapper, DamageRecordData.ExpandDataContent) or {}
                local SupportKillCounter = LogicKillCounter:GetBaseKillCounterIdByWeaponId(DefineID)
                if SupportKillCounter and DamageRecordData.ResultHealthStatus == ECharacterHealthStatus.FinishedLastBreath then
                    ExpandData.KillCounterItemId = DefineID
                    ExpandData.KillCounterNum = (ExpandData.KillCounterNum or 0) + 1
                    _G.addKill(DefineID, 1)
                end
                local synData = currWeapon.synData
                if synData and slua.isValid(synData) then
                    local weaponDefineID = slua.IndexReference(synData:Get(7), "defineID")
                    if weaponDefineID and slua.isValid(weaponDefineID) then
                        DamageRecordData.CauserWeaponAvatarID = weaponDefineID.TypeSpecificID
                    end
                end
                DamageRecordData.ExpandDataContent = slua.LuaArchiverEncode(LuaStateWrapper, ExpandData)
            end
        end
    end
    O_FileItem(self, DamageRecordData)
end

local MyMainKillCounter = require("GameLua.Mod.BaseMod.Client.KillCounter.MainKillCounter")
local MyKillCountSubSystem = require("GameLua.Mod.BaseMod.Client.KillCounter.KillCounterUISubsystem")
_G.OurkillCountSystem = MyKillCountSubSystem.__inner_impl

MyMainKillCounter.__inner_impl.OnRefreshUI = function(self, _, _, UID)
    pcall(function()
        local ModuleManager = require("client.module_framework.ModuleManager")
        local LogicKillCounter = ModuleManager.GetModule(ModuleManager.CommonModuleConfig.LogicKillCounter)
        local uCharacter = slua_GameFrontendHUD:GetPlayerController():GetPlayerCharacterSafety()
        if not uCharacter then return end
        local currweapon = uCharacter:GetCurrentWeapon()
        if currweapon then
            local DefineID = currweapon:GetItemDefineID().TypeSpecificID
            local currentEquipAvatarID = slua.IndexReference(currweapon.synData:Get(7), "defineID").TypeSpecificID
            local curEquipedKillCounter = LogicKillCounter:GetEquipedKillCounterId(6114302174, currentEquipAvatarID)
            self.KillCounterItem:SetKillCounterItemShowWithNum(curEquipedKillCounter, _G.getKills(DefineID), currentEquipAvatarID)
        end
    end)
end

MyKillCountSubSystem.__inner_impl.CheckSupportKCUI = function(self) return true end

local o_CheckNeedMainKillCounterUI = MyKillCountSubSystem.__inner_impl.CheckNeedMainKillCounterUI
MyKillCountSubSystem.__inner_impl.CheckNeedMainKillCounterUI = function(self, Weapon, PlayerID)
    pcall(function()
        local uCharacter = slua_GameFrontendHUD:GetPlayerController():GetPlayerCharacterSafety()
        if not uCharacter then return end
        local currweapon = uCharacter:GetCurrentWeapon()
        if currweapon then
            local DefineID = currweapon:GetItemDefineID().TypeSpecificID
            _G.WeaponEvents.onWeaponChanged(DefineID)
            self:UpdateMainKillCounterUI(true, DefineID, slua.IndexReference(currweapon.synData:Get(7), "defineID").TypeSpecificID)
        end
    end)
end

local o_UpdateMainKillCounterUI = MyKillCountSubSystem.__inner_impl.UpdateMainKillCounterUI
MyKillCountSubSystem.__inner_impl.UpdateMainKillCounterUI = function(self, bShow, WeaponID, AvatarID)
    pcall(function()
        o_UpdateMainKillCounterUI(self, bShow, WeaponID, AvatarID)
        local UIManager = require("client.slua_ui_framework.manager")
        local MainKillCounter = UIManager.GetUI(UIManager.UI_Config_InGame.MainKillCounter)
        local uCharacter = slua_GameFrontendHUD:GetPlayerController():GetPlayerCharacterSafety()
        if not uCharacter then return end
        local currweapon = uCharacter:GetCurrentWeapon()
        if not bShow and MainKillCounter then
            UIManager.CloseUI(UIManager.UI_Config_InGame.MainKillCounter)
        elseif bShow and currweapon then
            local DefineID = currweapon:GetItemDefineID().TypeSpecificID
            local currentEquipAvatarID = slua.IndexReference(currweapon.synData:Get(7), "defineID").TypeSpecificID
            local ModuleManager = require("client.module_framework.ModuleManager")
            local LogicKillCounter = ModuleManager.GetModule(ModuleManager.CommonModuleConfig.LogicKillCounter)
            local SupportKillCounter = LogicKillCounter:GetBaseKillCounterIdByWeaponId(DefineID)
            if SupportKillCounter == nil and MainKillCounter then
                UIManager.CloseUI(UIManager.UI_Config_InGame.MainKillCounter)
            elseif DefineID == currentEquipAvatarID and MainKillCounter then
                UIManager.CloseUI(UIManager.UI_Config_InGame.MainKillCounter)
            else
                local curEquipedKillCounter = LogicKillCounter:GetEquipedKillCounterId(6114302174, currentEquipAvatarID)
                if not MainKillCounter then
                    UIManager.ShowUI(UIManager.UI_Config_InGame.MainKillCounter, DefineID, currentEquipAvatarID)
                    MainKillCounter = UIManager.GetUI(UIManager.UI_Config_InGame.MainKillCounter)
                    if MainKillCounter then
                        MainKillCounter:SetKillCounterItemShowWithNum(curEquipedKillCounter, _G.getKills(DefineID), currentEquipAvatarID)
                    end
                else
                    MainKillCounter:UpdateWeaponID(DefineID, currentEquipAvatarID)
                    MainKillCounter:SetKillCounterItemShowWithNum(curEquipedKillCounter, _G.getKills(DefineID), currentEquipAvatarID)
                end
            end
        end
    end)
end
    end)
end

pcall(function()
    local IngamePhoneStateUI = require("GameLua.Mod.Library.Client.UI.IngamePhoneStateUI")
    local Lobby_Main_Wifi_UIBP = require("client.slua.umg.lobby.Main.Lobby_Main_Wifi_UIBP")
    local o_UpdateQuality = Lobby_Main_Wifi_UIBP.__inner_impl.UpdateQuality
    Lobby_Main_Wifi_UIBP.__inner_impl.UpdateQuality = function(self)
        self.UIRoot.WidgetSwitcher_Quality:SetActiveWidgetIndex(0)
        self.UIRoot.TextBlock_High:SetText("Anubis")
        self.UIRoot.TextBlock_High:SetColorAndOpacity(FSlateColor(FLinearColor(1, 1, 1, 1)))
    end
    local o_UpdateArtQualityUI = IngamePhoneStateUI.__inner_impl.UpdateArtQualityUI
    IngamePhoneStateUI.__inner_impl.UpdateArtQualityUI = function(self, _, _)
        self.UIRoot.TextBlock_quality:SetText("Anubis")
        self.UIRoot.TextBlock_quality:SetColorAndOpacity(FSlateColor(FLinearColor(0, 1, 0, 1)))
    end
end)

local function download_item(id)
    local PufferManager = require('client.slua.logic.download.puffer.puffer_manager')
    local PufferConst = require('client.slua.logic.download.puffer_const')
    if not PufferManager or not PufferConst then return end
    local state = PufferManager.GetState(PufferConst.ENUM_DownloadType.ODPAK, {id})
    if state ~= PufferConst.ENUM_DownloadState.Done then
        PufferManager.Download(PufferConst.ENUM_DownloadType.ODPAK, {id})
    end
end
_G.download_item = download_item

_G.IsPtrValid = function(ptr)
    if ptr == nil then return false end
    return slua.isValid(ptr)
end

function _G.apply_attachment(CurWeapon, avatarid)
    if not CurWeapon or not avatarid or avatarid == 0 then return end
    pcall(loadSkinAttachmentMaps)
    avatarid = getWeaponAppliedSkinId(CurWeapon, avatarid)
    pcall(function() applyBaseOverridesToAttachmentMap(avatarid) end)
    if not hasAttachmentOverrideMap(avatarid) then
        pcall(function() _G.InitParts(get_group_id(avatarid), avatarid) end)
    end
    local array = CurWeapon.synData
    if not array then return end
    local needsMeshRefresh = false
    for AttachIdx = 0, 4 do
        local isrefresh = false
        local Data = array:Get(AttachIdx)
        if not Data then break end
        local itemid = slua.IndexReference(Data, "defineID").TypeSpecificID or 0
        if itemid < 10000000 then
            local newId, isrefresh = applyAttachmentSkinForSlot(AttachIdx, itemid, avatarid)
            if isrefresh and newId then
                Data.defineID.TypeSpecificID = newId
                array:Set(AttachIdx, Data)
                needsMeshRefresh = true
            end
        end
    end
    if needsMeshRefresh then
        pcall(function() CurWeapon:DelayHandleAvatarMeshChanged() end)
    end
end

function _G.ResetMatchState()
    _G.MatchEndMessageShown = false
    _G.LastKillTime = {}
    for k in pairs(_G.AlreadyChangedSet) do _G.AlreadyChangedSet[k] = nil end
    for k in pairs(_G.DeadBoxSkins) do _G.DeadBoxSkins[k] = nil end
end

local function ShowMatchEndMessage(message)
    pcall(function()
        local CommonMsgBoxMgr = require("client.slua.logic.common.logic_common_msg_box")
        if CommonMsgBoxMgr then
            CommonMsgBoxMgr.Show(1, "Anubis", message, function() end)
        end
    end)
end

local function GetRandomEndMessage(rank, kills)
    local messages = {
        "Congratulations! You fought well!", "Good game! See you on the battlefield!",
        "Well played soldier!", "Victory is in your blood!", "Another battle, another victory!",
        "You are a true champion!", "The battlefield honors you!", "Great performance today!",
        "Your skills are impressive!", "Keep up the good work!", "You made your team proud!",
        "Legendary performance!", "You are unstoppable!", "Fear the reaper! You are death itself!",
        "Anubis sends their regards!"
    }
    if rank == 1 then
        table.insert(messages, "WINNER WINNER CHICKEN DINNER!")
        table.insert(messages, "You are the last one standing! AMAZING!")
        table.insert(messages, "CHAMPION! You dominated the battlefield!")
    elseif rank <= 5 then
        table.insert(messages, "Top 5 finish! Excellent work!")
        table.insert(messages, "Almost there! Next time you will win!")
    end
    if kills >= 15 then table.insert(messages, "HATTRICK! " .. kills .. " kills! You are a beast!")
    elseif kills >= 10 then table.insert(messages, "Double digits! " .. kills .. " kills! Impressive!")
    elseif kills >= 5 then table.insert(messages, "Solid " .. kills .. " kills! Well done!")
    end
    return messages[math.random(#messages)]
end

local function OnMatchEnd()
    if _G.MatchEndMessageShown then return end
    pcall(function()
        local GameState = slua_GameFrontendHUD and slua_GameFrontendHUD:GetGameState()
        if not GameState then return end
        local PlayerController = slua_GameFrontendHUD:GetPlayerController()
        if not PlayerController then return end
        local MyKills = 0
        if _G.killCountInfo then
            for weapon, kills in pairs(_G.killCountInfo) do MyKills = MyKills + kills end
        end
        local MyRank = 99
        local TeamRank = GameState:GetTeamRank()
        if TeamRank and TeamRank > 0 then MyRank = TeamRank end
        local message = GetRandomEndMessage(MyRank, MyKills)
        message = message .. "\n\nRank: " .. MyRank .. "\nKills: " .. MyKills .. "\n\nAnubis"
        ShowMatchEndMessage(message)
        _G.MatchEndMessageShown = true
        _G.ResetMatchState()
    end)
end

local function CheckGameEndLoop()
    pcall(function()
        local GameState = slua_GameFrontendHUD and slua_GameFrontendHUD:GetGameState()
        if GameState and GameState.bIsMatchEnd and not _G.MatchEndMessageShown then
            OnMatchEnd()
        end
    end)
end


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

local function GameAvatarHandlerkillcounter()
    pcall(_G.ForceUpdateKillCounterUI)
end

local _skinUnifiedTickN = 0

local function skinModUnifiedTick()
    _skinUnifiedTickN = _skinUnifiedTickN + 1
    local n = _skinUnifiedTickN
    pcall(function()
        if checkSkinReloadStamp() then
            _G.__matchOutfitOk = nil
            _G.__lastAppliedMatchOutfit = nil
            _G.__lastGearAvatarTick = nil
        end
    end)
    if isInPlayableGame() then
        pcall(GameAvatarHandlerweapons)
        if n % 2 == 0 then pcall(_G.GameAvatarHandlervehicles) end
        if n % 3 == 0 then pcall(GameAvatarHandlerkillcounter) end
        if not _G.__matchOutfitOk and n % 2 == 0 then
            pcall(function() _G.applyMatchOutfitNow(true) end)
        elseif n % 6 == 0 then
            pcall(_G.applyMatchOutfitIfNeeded)
        end
        if n % 4 == 0 then pcall(_G.GameAvatarHandlerplayers) end
        if n % 5 == 0 then pcall(_G.GameAvatarHandlerDeadBox) end
        if n % 10 == 0 then pcall(_G.FileWatcher) end
        if n % 2 == 0 then pcall(CheckGameEndLoop) end
    else
        if n % 2 == 0 then
            pcall(function()
                if not _G.__lastOutfitConfigRead or (os.clock() - _G.__lastOutfitConfigRead) > 45 then
                    _G.ReadConfigFile()
                    _G.__lastOutfitConfigRead = os.clock()
                end
                if _G.__needsLobbyOutfitRefresh and _G.refreshLobbyOutfitVisual then
                    _G.refreshLobbyOutfitVisual(true)
                end
                _G.GameAvatarHandlerplayers()
            end)
        end
        if n % 3 == 0 then
            pcall(_G.CheckLobbyThemeChanges)
            pcall(_G.ApplyLobbyTheme)
        end
        if n % 4 == 0 then pcall(_G.HandlePetLogic) end
        if n % 10 == 0 then pcall(_G.scheduleLobbyConfigFlush) end
    end
end

function _G.startSrcOutfitTimers()
    if _G.__SRC_HUB_TIMERS_STARTED then return end
    local ok, ticker = pcall(require, 'common.time_ticker')
    if ok and ticker then _G.Mytimer_ticker = ticker end
    if not _G.Mytimer_ticker then return end

    if not _G.__SKIN_BOOTSTRAPPED then
        pcall(_G.loadKillCountFromFile)
        _G.isFileWatcherActive = true
        pcall(InitItemUpgradeSystem)
        pcall(installExtendedSkinHooks)
        pcall(loadBuiltinAttachmentMaps)
        _G.__SKIN_BOOTSTRAPPED = true
    end
    pcall(_G.ReadConfigFile, true)

    _G.__SRC_HUB_TIMERS_STARTED = true
    skinProbeLog('startSrcOutfitTimers: unified 3s schedule')
    pcall(function()
        _G.Mytimer_ticker.AddTimerLoop(3, skinModUnifiedTick, -1, 1)
        _G.Mytimer_ticker.AddTimerOnce(0.8, function()
            pcall(_G.ApplyLobbyTheme)
            pcall(_G.GameAvatarHandlerplayers)
            pcall(function() _G.applyMatchOutfitNow(true) end)
            pcall(_G.HandlePetLogic)
            local uCharacter = getMatchPlayerCharacter() or getSkinPlayerCharacter()
            if uCharacter then
                pcall(function() _G.ProcessOneWeapon(uCharacter:GetCurrentWeapon(), true) end)
            end
        end)
    end)
end
_G.startSrcIngameTimers = _G.startSrcOutfitTimers
_G.ensureSkinTimers = _G.startSrcOutfitTimers

pcall(function()
    local ok, ticker = pcall(require, 'common.time_ticker')
    if ok and ticker then _G.Mytimer_ticker = ticker end
end)

_G.__SKIN_PATCHED = true
pcall(loadBuiltinAttachmentMaps)

if _G.Mytimer_ticker then
    pcall(function()
        _G.Mytimer_ticker.AddTimerOnce(1, function()
            if _G.startSrcOutfitTimers then _G.startSrcOutfitTimers() end
        end)
    end)
else
    pcall(function()
        if _G.startSrcOutfitTimers then _G.startSrcOutfitTimers() end
    end)
end

-- ========== Anubis in-game bootstrap (match/training — lobby = Ultimate) ==========
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
            skinProbeLog("Anubis retry gave up after " .. tries)
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
        skinProbeLog("Anubis in-game: skip (lobby/loading) — will retry")
        scheduleSrcHubRetry()
        return
    end
    local firstBoot = not _G.__SRC_HUB_STARTED
    if firstBoot then
        skinProbeLog("Anubis in-game bootstrap")
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
        notify("Anubis in-game active")
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

end)()

pcall(function()
    later((_G.__SKIN_BOOT_DELAY_SEC or 2.5), function()
        local ok, err = pcall(start)
        if not ok then
            skinProbeLog("START ERROR: " .. tostring(err))
            log("START ERROR", err)
        end
    end)
end)


_G.getOutfitInsId = function(res)
    return insIdForItemRes(tonumber(res))
end

_G.applyUltimateMatchOutfit = function(char)
    char = char or getLocalChar()
    if char and slua.isValid(char) then
        return matchApplyOutfit(char)
    end
    return false
end
