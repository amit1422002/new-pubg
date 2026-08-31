-- SRC HUB in-game vehicle skins (lobby = Ultimate wardrobe inject)
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
