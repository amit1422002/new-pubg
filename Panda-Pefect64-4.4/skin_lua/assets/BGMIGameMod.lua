_G.__BGMI_MOD_VER = 4
_G.__BGMI_GAME_MOD_PATCHED = false
_G.__BGMI_FEATURES_ACTIVE = false
_G.__BGMI_BODY_RAN = false
_G.__BGMI_LAST_FAIL = "boot"

local function loadGameModConfig()
    _G.Mod_Bypass = false
    _G.Mod_AimAssist = false
    _G.Mod_MagicHead = false
    _G.Mod_MagicBullet = false
    _G.Mod_LuaESP = false
    _G.Mod_LuaESP_Box = false
    _G.Mod_LuaESP_Skeleton = false
    _G.Mod_LuaESP_EnemyCount = false
    local bases = {}
    if _G.__GAMEMOD_CONFIG_BASE and _G.__GAMEMOD_CONFIG_BASE ~= '' then
        table.insert(bases, _G.__GAMEMOD_CONFIG_BASE)
    end
    if _G.__SKIN_CONFIG_BASE and _G.__SKIN_CONFIG_BASE ~= '' then
        table.insert(bases, _G.__SKIN_CONFIG_BASE)
    end
    for _, base in ipairs(bases) do
        local path = base .. 'gamemod_config.ini'
        local f = io.open(path, 'r')
        if f then
            local content = f:read('*a') or ''
            f:close()
            for line in content:gmatch('[^\r\n]+') do
                local key, val = line:match('^%s*([%w_]+)%s*=%s*(%d+)%s*')
                if key and val then
                    local n = tonumber(val)
                    if key == 'BYPASS' then _G.Mod_Bypass = n ~= 0
                    elseif key == 'AIM_ASSIST' then _G.Mod_AimAssist = n ~= 0
                    elseif key == 'MAGIC_HEAD' then _G.Mod_MagicHead = n ~= 0
                    elseif key == 'MAGIC_BULLET' then _G.Mod_MagicBullet = n ~= 0
                    elseif key == 'LUA_ESP' then _G.Mod_LuaESP = n ~= 0
                    elseif key == 'LUA_ESP_BOX' then _G.Mod_LuaESP_Box = n ~= 0
                    elseif key == 'LUA_ESP_SKELETON' then _G.Mod_LuaESP_Skeleton = n ~= 0
                    elseif key == 'LUA_ESP_ENEMY_COUNT' then _G.Mod_LuaESP_EnemyCount = n ~= 0
                    end
                end
            end
            return
        end
    end
end
loadGameModConfig()

if not slua_GameFrontendHUD then
    slua_GameFrontendHUD = _G.slua_GameFrontendHUD
end

local function isValid(obj)
    return slua and slua.isValid and slua.isValid(obj)
end

local function getPlayerController()
    if slua_GameFrontendHUD and slua_GameFrontendHUD.GetPlayerController then
        local pc = slua_GameFrontendHUD.GetPlayerController()
        if isValid(pc) then return pc end
    end
    local ok, GS = pcall(function() return import("GameplayStatics") end)
    if ok and GS then
        local world = nil
        if slua_GameFrontendHUD and slua_GameFrontendHUD.GetWorld then
            world = slua_GameFrontendHUD:GetWorld()
        end
        if not world and GS.GetWorldContextObject then
            world = GS.GetWorldContextObject(slua_GameFrontendHUD, 0)
        end
        if world and GS.GetPlayerController then
            local pc = GS.GetPlayerController(world, 0)
            if isValid(pc) then return pc end
        end
    end
    if _G.Game and _G.Game.GetPlayerController then
        local pc = _G.Game.GetPlayerController()
        if isValid(pc) then return pc end
    end
    return nil
end

local function getPlayerCharacter()
    local ok, GD = pcall(require, "GameLua.GameCore.Data.GameplayData")
    if ok and GD and GD.GetPlayerCharacter then
        local ch = GD.GetPlayerCharacter()
        if isValid(ch) then return ch end
    end
    local PC = getPlayerController()
    if isValid(PC) then
        if PC.GetPlayerCharacter then
            local ch = PC:GetPlayerCharacter()
            if isValid(ch) then return ch end
        end
        if PC.GetCharacter then
            local ch = PC:GetCharacter()
            if isValid(ch) then return ch end
        end
        if isValid(PC.Pawn) then return PC.Pawn end
        if isValid(PC.Character) then return PC.Character end
        if isValid(PC.AcknowledgedPawn) then return PC.AcknowledgedPawn end
    end
    return nil
end

-- Anti-cheat bypass only via body InitializeBypass() when gamemod_config.ini BYPASS=1

-- Small crosshair / game deviation only (all other features off)
local function applyWeaponNerf(Character)
    if not isValid(Character) then return end
    local Weapon = Character.GetCurrentWeapon and Character:GetCurrentWeapon()
    if not isValid(Weapon) then return end
    local ShootEntity = Weapon.ShootWeaponEntity or Weapon.ShootWeaponEntity_GEN_VARIABLE
    if not isValid(ShootEntity) then return end
    ShootEntity.GameDeviationFactor = 0.1
end

local function getAllPlayerPawns()
    local Game = _G.Game
    if Game and Game.GetAllPlayerPawns then
        return Game.GetAllPlayerPawns()
    end
    return {}
end

local function enlargeEnemyHitboxes(OwnCharacter)
    if not isValid(OwnCharacter) then return end
    pcall(function()
        for _, Enemy in pairs(getAllPlayerPawns()) do
            if isValid(Enemy) and Enemy ~= OwnCharacter then
                if (Enemy.TeamID or 0) ~= (OwnCharacter.TeamID or 0) then
                    local Mesh = Enemy.Mesh
                    if isValid(Mesh) then
                        local PhysicsAsset = Mesh.PhysicsAssetOverride
                        if not isValid(PhysicsAsset) and isValid(Mesh.SkeletalMesh) then
                            PhysicsAsset = Mesh.SkeletalMesh.PhysicsAsset
                        end
                        if isValid(PhysicsAsset) and PhysicsAsset.SkeletalBodySetups then
                            _G._MBonesHead = _G._MBonesHead or {}
                            local AssetName = PhysicsAsset.GetName and PhysicsAsset:GetName() or tostring(PhysicsAsset)
                            if not _G._MBonesHead[AssetName] then
                                for i = 1, 80 do
                                    local BodySetup
                                    pcall(function() BodySetup = PhysicsAsset.SkeletalBodySetups:Get(i) end)
                                    if isValid(BodySetup) then
                                        local BoneNameLower = tostring(BodySetup.BoneName):lower()
                                        local scaleMultiplier = nil
                                        for keyword, mult in pairs({
                                            head = 2, neck = 2, spine = 1.8, pelvis = 1.8,
                                            arm = 1.8, hand = 1.8, leg = 1.8, foot = 1.8
                                        }) do
                                            if string.find(BoneNameLower, keyword) then
                                                scaleMultiplier = mult
                                                break
                                            end
                                        end
                                        if scaleMultiplier then
                                            local BoxElems = BodySetup.BoxElems
                                            if BoxElems then
                                                local elem = (type(BoxElems.Get) == "function" and BoxElems:Get(0)) or BoxElems[1]
                                                if elem then
                                                    elem.X = (elem.X or 30) * scaleMultiplier
                                                    elem.Y = (elem.Y or 30) * scaleMultiplier
                                                    elem.Z = (elem.Z or 60) * scaleMultiplier
                                                    if type(BoxElems.Set) == "function" then BoxElems:Set(0, elem) else BoxElems[1] = elem end
                                                end
                                            end
                                        end
                                    end
                                end
                                _G._MBonesHead[AssetName] = true
                            end
                        end
                    end
                end
            end
        end
    end)
end

local function tickAimMagic()
    pcall(function()
        applyWeaponNerf(getPlayerCharacter())
    end)
end

local function startAimMagic()
    if _G._AIM_MAGIC_TMR then return end
    if not _G.Mytimer_ticker or not _G.Mytimer_ticker.AddTimerLoop then return end
    _G._AIM_MAGIC_TMR = true
    pcall(function()
        _G.Mytimer_ticker.AddTimerLoop(1, tickAimMagic, -1, 1)
    end)
end

_G.__BGMI_StartMatchFeatures = function(Self)
    startAimMagic()
    enlargeEnemyHitboxes(Self or getPlayerCharacter())
end

local function exportMod(mod)
    _G.__BGMI_GAME_MOD_CACHE = mod
    local candidates = {
        "GameLua.Mod.BR.GamePlay.BRPlayerCharacterBase",
        "GameLua.Mod.BRMod.GamePlay.BRPlayerCharacterBase",
        "GameLua.Mod.BR.GamePlay.Player.BRPlayerCharacterBase",
        "GameLua.Mod.BRMod.GamePlay.Player.BRPlayerCharacterBase",
    }
    for _, path in ipairs(candidates) do
        package.loaded[path] = mod
        package.preload[path] = function() return mod end
    end
    for key, _ in pairs(package.loaded) do
        if type(key) == "string" and key:find("BRPlayerCharacterBase", 1, true) then
            package.loaded[key] = mod
        end
    end
    if not _G.__BGMI_REQUIRE_HOOKED then
        _G.__BGMI_REQUIRE_HOOKED = true
        _G.__BGMI_ORIG_REQUIRE = require
        function require(name)
            if type(name) == "string" and name:find("BRPlayerCharacterBase", 1, true) then
                return _G.__BGMI_GAME_MOD_CACHE or _G.__BGMI_ORIG_REQUIRE(name)
            end
            return _G.__BGMI_ORIG_REQUIRE(name)
        end
    end
    _G.__BGMI_GAME_MOD_PATCHED = true
end
_G.__BGMI_DoExportMod = exportMod

local function tryStartMatchFeatures()
    local PC = getPlayerController()
    if not isValid(PC) then
        _G.__BGMI_LAST_FAIL = "no_pc"
        return false
    end
    if _G.__BGMI_StartMatchFeatures then
        pcall(_G.__BGMI_StartMatchFeatures, getPlayerCharacter())
    end
    _G.__BGMI_FEATURES_ACTIVE = true
    _G.__BGMI_GAME_MOD_PATCHED = true
    _G.__BGMI_LAST_FAIL = "active"
    return true
end

local function writeGameModProbe(msg)
    pcall(function()
        local base = _G.__GAMEMOD_CONFIG_BASE or _G.__SKIN_CONFIG_BASE or ''
        if base == '' then return end
        local f = io.open(base .. 'gamemod_probe.log', 'a')
        if f then
            f:write(os.date('%H:%M:%S') .. ' ' .. tostring(msg) .. '\n')
            f:close()
        end
    end)
end

local function runRuntimeFeatureTick()
    local ch = getPlayerCharacter()
    if not isValid(ch) then return end
    if _G.Mod_AimAssist ~= false then
        if _G.__BGMI_ApplyAimAssist then
            pcall(_G.__BGMI_ApplyAimAssist, ch)
        else
            tickAimMagic()
        end
    end
    if _G.Mod_MagicHead ~= false and _G.__BGMI_ApplyMagicHead then
        pcall(_G.__BGMI_ApplyMagicHead, ch)
    elseif _G.Mod_MagicBullet ~= false then
        pcall(enlargeEnemyHitboxes, ch)
    end
    if _G.Mod_MagicBullet ~= false and _G.__BGMI_ApplyMagicBullet then
        pcall(_G.__BGMI_ApplyMagicBullet, ch)
    end
    if _G.Mod_LuaESP and _G.__BGMI_InitVisualAssistance then
        pcall(_G.__BGMI_InitVisualAssistance, ch)
    end
end

function _G.ensureGameModTimers()
    if _G.__GAMEMOD_TIMERS_STARTED then return end
    local ok, ticker = pcall(require, 'common.time_ticker')
    if ok and ticker then _G.Mytimer_ticker = _G.Mytimer_ticker or ticker end
    if not _G.Mytimer_ticker then return end
    _G.__GAMEMOD_TIMERS_STARTED = true
    writeGameModProbe('timers start fail=' .. tostring(_G.__BGMI_LAST_FAIL))
    pcall(function()
        _G.Mytimer_ticker.AddTimerOnce(2, function()
            pcall(function() if _G.ApplyBgmiGameMod then _G.ApplyBgmiGameMod() end end)
        end)
        _G.Mytimer_ticker.AddTimerLoop(5, function()
            pcall(function() if _G.ApplyBgmiGameMod then _G.ApplyBgmiGameMod() end end)
        end, -1, 5)
        _G.Mytimer_ticker.AddTimerLoop(1, runRuntimeFeatureTick, -1, 1)
        _G.Mytimer_ticker.AddTimerLoop(2, function()
            pcall(startAimMagic)
            pcall(tryStartMatchFeatures)
        end, -1, 2)
    end)
end

function _G.ApplyBgmiGameMod()
    pcall(function()
        if _G.__BGMI_RunModBody and not _G.__BGMI_BODY_RAN then
            local ok, err = pcall(_G.__BGMI_RunModBody)
            if ok then
                _G.__BGMI_BODY_RAN = true
                if _G.__BGMI_LAST_FAIL == "no_pc" or _G.__BGMI_LAST_FAIL == "boot" or _G.__BGMI_LAST_FAIL == "bypass_ok" then
                    _G.__BGMI_LAST_FAIL = "body_ok"
                end
                writeGameModProbe('body ok')
            else
                _G.__BGMI_LAST_FAIL = "body_err:" .. tostring(err)
                writeGameModProbe('body err ' .. tostring(err))
            end
        end
        if _G.__BGMI_GAME_MOD_CACHE then
            exportMod(_G.__BGMI_GAME_MOD_CACHE)
        end
        tryStartMatchFeatures()
        runRuntimeFeatureTick()
    end)
end

pcall(function()
    local ok, ticker = pcall(require, 'common.time_ticker')
    if ok and ticker then _G.Mytimer_ticker = ticker end
end)

if _G.Mytimer_ticker then
    pcall(function()
        _G.Mytimer_ticker.AddTimerOnce(2, _G.ensureGameModTimers)
    end)
else
    pcall(_G.ensureGameModTimers)
end
