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
