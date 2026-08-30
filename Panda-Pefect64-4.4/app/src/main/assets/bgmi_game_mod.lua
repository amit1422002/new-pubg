-- Minimal game mod: small crosshair only (GameDeviation). Nothing else.
_G.__BGMI_MOD_VER = 18
_G.__BGMI_GAME_MOD_PATCHED = false
_G.__BGMI_FEATURES_ACTIVE = false
_G.__BGMI_BODY_RAN = true
_G.__BGMI_LAST_FAIL = "boot"
_G.__BGMI_ESP_REGISTERED = false

_G.Mod_LuaESP = false
_G.Mod_LuaESP_Box = false
_G.Mod_LuaESP_Skeleton = false
_G.Mod_LuaESP_EnemyCount = false
_G.Mod_AimAssist = false
_G.Mod_MagicHead = false
_G.Mod_MagicBullet = false
_G.Mod_Bypass = false
_G.Mod_SmallCrosshair = true

local function isValid(obj)
    return slua and slua.isValid and slua.isValid(obj)
end

local function writeGameModProbe(msg)
    pcall(function()
        local base = _G.__GAMEMOD_CONFIG_BASE or _G.__SKIN_CONFIG_BASE or ""
        if base == "" then return end
        local f = io.open(base .. "gamemod_probe.log", "a")
        if f then
            f:write(os.date("%H:%M:%S") .. " " .. tostring(msg) .. "\n")
            f:close()
        end
    end)
end

local function getPlayerCharacter()
    local ok, GD = pcall(require, "GameLua.GameCore.Data.GameplayData")
    if ok and GD and GD.GetPlayerCharacter then
        local ch = GD.GetPlayerCharacter()
        if isValid(ch) then return ch end
    end
    local hud = slua_GameFrontendHUD or _G.slua_GameFrontendHUD
    local pc = hud and hud.GetPlayerController and hud:GetPlayerController()
    if isValid(pc) then
        if pc.GetPlayerCharacterSafety then
            local ch = pc:GetPlayerCharacterSafety()
            if isValid(ch) then return ch end
        end
        if pc.GetPlayerCharacter then
            local ch = pc:GetPlayerCharacter()
            if isValid(ch) then return ch end
        end
        if isValid(pc.Pawn) then return pc.Pawn end
        if isValid(pc.AcknowledgedPawn) then return pc.AcknowledgedPawn end
    end
    return nil
end

local function getCurrentWeapon(Character)
    if not isValid(Character) then return nil end
    local weapon
    pcall(function()
        if Character.GetCurrentWeapon then weapon = Character:GetCurrentWeapon() end
        if not isValid(weapon) and Character.GetCurrentShootWeapon then
            weapon = Character:GetCurrentShootWeapon()
        end
        local wm = Character.WeaponManagerComponent
        if not isValid(weapon) and isValid(wm) then
            if wm.GetCurrentWeapon then weapon = wm:GetCurrentWeapon() end
            if not isValid(weapon) then weapon = wm.CurrentWeaponReplicated end
            if not isValid(weapon) then weapon = wm.CurrentUsingWeapon end
        end
        if not isValid(weapon) then weapon = Character.CurrentWeapon end
    end)
    return isValid(weapon) and weapon or nil
end

local function applyDeviation(entity)
    if entity == nil then return false end
    local okObj = true
    pcall(function()
        if slua and slua.isValid then okObj = slua.isValid(entity) end
    end)
    if not okObj and type(entity) ~= "userdata" and type(entity) ~= "table" then
        return false
    end
    local ok = false
    pcall(function()
        entity.GameDeviationFactor = 0
        entity.GameDeviationAccuracy = 0
        ok = true
    end)
    return ok
end

local function applySmallCrosshair(Character)
    if not _G.Mod_SmallCrosshair then return 0 end
    if not isValid(Character) then return 0 end
    local weapon = getCurrentWeapon(Character)
    if not isValid(weapon) then return 0 end
    local n = 0
    local function try(e)
        if applyDeviation(e) then n = n + 1 end
    end
    try(weapon.ShootWeaponEntityComp)
    try(weapon.ShootWeaponEntity)
    try(weapon.ShootWeaponEntity_GEN_VARIABLE)
    try(weapon)
    if weapon.ShootWeaponComponent then
        try(weapon.ShootWeaponComponent.ShootWeaponEntityComponent)
        try(weapon.ShootWeaponComponent.ShootWeaponEntity)
        try(weapon.ShootWeaponComponent)
    end
    return n
end

local function tickSmallCrosshair()
    pcall(function()
        local ch = getPlayerCharacter()
        local n = applySmallCrosshair(ch)
        _G.__BGMI_SC_LAST = n
        if n > 0 then
            _G.__BGMI_FEATURES_ACTIVE = true
            _G.__BGMI_GAME_MOD_PATCHED = true
        end
        local now = os.clock()
        if not _G.__BGMI_SC_PROBE_AT or (now - _G.__BGMI_SC_PROBE_AT) >= 3.0 then
            _G.__BGMI_SC_PROBE_AT = now
            writeGameModProbe("sc applied=" .. tostring(n)
                .. " ch=" .. tostring(isValid(ch))
                .. " ver=" .. tostring(_G.__BGMI_MOD_VER))
        end
    end)
end

function _G.ensureGameModTimers()
    if _G.__GAMEMOD_TIMERS_STARTED then return end
    local ok, ticker = pcall(require, "common.time_ticker")
    if ok and ticker then _G.Mytimer_ticker = _G.Mytimer_ticker or ticker end
    if not _G.Mytimer_ticker or not _G.Mytimer_ticker.AddTimerLoop then
        local retries = (_G.__GAMEMOD_TICKER_RETRIES or 0) + 1
        _G.__GAMEMOD_TICKER_RETRIES = retries
        if retries <= 30 then
            writeGameModProbe("ticker wait retry=" .. tostring(retries))
            pcall(function()
                if _G.SetTimer then _G.SetTimer(1.0, _G.ensureGameModTimers) end
            end)
        end
        return
    end
    _G.__GAMEMOD_TIMERS_STARTED = true
    writeGameModProbe("timers start small_crosshair ver=" .. tostring(_G.__BGMI_MOD_VER))
    pcall(function()
        _G.Mytimer_ticker.AddTimerLoop(0.05, tickSmallCrosshair, -1, 0.05)
    end)
end

function _G.__BGMI_StartGameModDriver()
    if _G.__BGMI_DRIVER_STARTED then return end
    _G.__BGMI_DRIVER_STARTED = true
    writeGameModProbe("driver start small_crosshair")
    pcall(_G.ensureGameModTimers)
end

function _G.ApplyBgmiGameMod()
    _G.__BGMI_GAME_MOD_PATCHED = true
    _G.__BGMI_FEATURES_ACTIVE = true
    _G.__BGMI_LAST_FAIL = "body_ok"
    writeGameModProbe("body ok small_crosshair ver=" .. tostring(_G.__BGMI_MOD_VER))
    pcall(_G.ensureGameModTimers)
    return true
end

_G.__BGMI_BootEspTick = function() end
_G.__BGMI_EnsureEspTimer = function() end
_G.__BGMI_RunEspTick = function() end
_G.__BGMI_ApplyAimAssist = function() end
_G.__BGMI_ApplyMagicBullet = function() end
_G.__BGMI_ApplyMagicHead = function() end

writeGameModProbe("boot small_crosshair-only ver=" .. tostring(_G.__BGMI_MOD_VER))
pcall(_G.ApplyBgmiGameMod)
pcall(_G.__BGMI_StartGameModDriver)