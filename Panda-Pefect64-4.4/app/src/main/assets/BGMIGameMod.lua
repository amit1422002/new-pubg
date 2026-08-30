_G.__BGMI_MOD_VER = 4
_G.__BGMI_GAME_MOD_PATCHED = false
_G.__BGMI_FEATURES_ACTIVE = false
_G.__BGMI_BODY_RAN = false
_G.__BGMI_LAST_FAIL = "boot"

local function gamemodConfigPaths()
    local paths, seen = {}, {}
    local function add(path)
        if path and path ~= '' and not seen[path] then
            seen[path] = true
            table.insert(paths, path)
        end
    end
    local function addBase(base)
        if not base or base == '' then return end
        local trimmed = base:gsub('/$', '')
        add(trimmed .. '/gamemod_config.ini')
        add(base .. 'gamemod_config.ini')
    end
    addBase(_G.__GAMEMOD_CONFIG_BASE)
    addBase(_G.__SKIN_CONFIG_BASE)
    add('/data/user/0/com.pubg.imobile/files/gamemod_config.ini')
    add('/storage/emulated/0/Android/data/com.pubg.imobile/files/gamemod_config.ini')
    return paths
end

local function applyGameModConfigLine(key, val)
    local n = tonumber(val)
    if not n then return end
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

local function loadGameModConfig()
    _G.Mod_Bypass = false
    _G.Mod_AimAssist = false
    _G.Mod_MagicHead = false
    _G.Mod_MagicBullet = false
    _G.Mod_LuaESP = true
    _G.Mod_LuaESP_Box = true
    _G.Mod_LuaESP_Skeleton = true
    _G.Mod_LuaESP_EnemyCount = true
    for _, path in ipairs(gamemodConfigPaths()) do
        local f = io.open(path, 'r')
        if f then
            local content = f:read('*a') or ''
            f:close()
            for line in content:gmatch('[^\r\n]+') do
                local key, val = line:match('^%s*([%w_]+)%s*=%s*(%d+)%s*')
                if key and val then
                    applyGameModConfigLine(key, val)
                end
            end
            return
        end
    end
end
_G.__BGMI_ReloadConfig = loadGameModConfig
loadGameModConfig()

if not slua_GameFrontendHUD then
    slua_GameFrontendHUD = _G.slua_GameFrontendHUD
end

local function isValid(obj)
    return slua and slua.isValid and slua.isValid(obj)
end

local COLOR_ESP_DIST
pcall(function()
    COLOR_ESP_DIST = FLinearColor(1, 1, 1, 0.95)
end)

local function distUeToMeters(distUe)
    return math.max(0, math.floor((distUe or 0) / 100 + 0.5))
end

local function projectHeadToScreen(uCon, worldPos)
    if not isValid(uCon) or not worldPos or not uCon.ProjectWorldLocationToScreen then
        return nil
    end
    local screen = FVector2D(0, 0)
    local projected = false
    pcall(function()
        projected = uCon:ProjectWorldLocationToScreen(worldPos, false, screen) == true
    end)
    if projected then
        return screen
    end
    return nil
end

local function getEspFont(uCon)
    if _G.__BGMI_ESP_FONT and slua.isValid(_G.__BGMI_ESP_FONT) then return _G.__BGMI_ESP_FONT end
    pcall(function()
        local hud = uCon and uCon.GetHUD and uCon:GetHUD()
        if hud and hud.Font then _G.__BGMI_ESP_FONT = hud.Font end
    end)
    if not _G.__BGMI_ESP_FONT then
        pcall(function() _G.__BGMI_ESP_FONT = import("Font'/Engine/EngineFonts/Roboto.Roboto'") end)
    end
    if not _G.__BGMI_ESP_FONT then
        pcall(function() _G.__BGMI_ESP_FONT = import("/Script/Engine.Font'/Engine/EngineFonts/Roboto.Roboto'") end)
    end
    return _G.__BGMI_ESP_FONT
end

local function resolveEspCanvas(uCon)
    if not isValid(uCon) then return nil end
    local hud = uCon.GetHUD and uCon:GetHUD()
    if not isValid(hud) then return nil end
    if hud.Canvas then return hud.Canvas end
    if hud.GetCanvas then
        local ok, c = pcall(function() return hud:GetCanvas() end)
        if ok and c then return c end
    end
    return nil
end

local function drawEspDistanceOnCanvas(canvas, uCon, x, y, text)
    if not canvas or not text or text == '' then return false end
    local drew = false
    pcall(function()
        local color = COLOR_ESP_DIST or FLinearColor(1, 1, 1, 1)
        local font = getEspFont(uCon)
        if canvas.K2_DrawText and font then
            canvas:K2_DrawText(font, text, FVector2D(x, y), FVector2D(1.1, 1.1), color, 0,
                FLinearColor(0, 0, 0, 1), FVector2D(0, 0), true, true, false, FLinearColor(0, 0, 0, 1))
            drew = true
        elseif canvas.DrawText then
            canvas:DrawText(text, true, x, y, font, 1.2, false)
            drew = true
        end
    end)
    return drew
end

local function setWidgetDistanceText(ui, text, meters)
    if not ui then return false end
    for _, fname in ipairs({
        'TextBlock_Distance', 'Text_Distance', 'DistanceText', 'UTextBlock_Distance',
        'TextBlock_Dis', 'TextBlock_Meter', 'TextBlock_Dist', 'Text_Dist', 'Distance',
        'UTextBlock_Dist', 'TextBlock_DistanceNum', 'Text_DistanceNum'
    }) do
        local tb = ui[fname]
        if tb and tb.SetText then
            tb:SetText(text)
            pcall(function() if tb.SetVisibility then tb:SetVisibility(0) end end)
            pcall(function() if tb.SetWidgetVisibility then tb:SetWidgetVisibility(0) end end)
            return true
        end
    end
    if ui.SetDistanceText then ui:SetDistanceText(text); return true end
    if ui.SetDistance then ui:SetDistance(meters); return true end
    return false
end

local function trySetReplayDistanceUi(tPawn, meters, distUe, text)
    pcall(function()
        if tPawn.Replay_SetVisiableOfDistanceUI then tPawn:Replay_SetVisiableOfDistanceUI(true) end
        if tPawn.Replay_SetVisiableOfDistance then tPawn:Replay_SetVisiableOfDistance(true) end
        if tPawn.SetEnemyFrameDistanceVisible then tPawn:SetEnemyFrameDistanceVisible(true) end
        if tPawn.Replay_SetDistanceUI then tPawn:Replay_SetDistanceUI(meters) end
        if tPawn.Replay_SetDistanceNum then tPawn:Replay_SetDistanceNum(meters) end
        if tPawn.Replay_SetDistanceText then tPawn:Replay_SetDistanceText(text) end
        if tPawn.Replay_SetFrameUIDistance then tPawn:Replay_SetFrameUIDistance(meters) end
        if tPawn.Replay_SetEnemyFrameDistance then tPawn:Replay_SetEnemyFrameDistance(meters) end
        if tPawn.Replay_UpdateDistance then tPawn:Replay_UpdateDistance(meters) end
        if tPawn.Replay_UpdateEnemyFrameUI then tPawn:Replay_UpdateEnemyFrameUI(meters) end
        if tPawn.Replay_SetDistanceVisible then tPawn:Replay_SetDistanceVisible(true) end
        if tPawn.SetReplayUIDistance then tPawn:SetReplayUIDistance(meters) end
        if tPawn.Replay_SetDistanceToLocalCharacter then tPawn:Replay_SetDistanceToLocalCharacter(distUe) end
        if tPawn.Replay_SetDistanceToLocalPawn then tPawn:Replay_SetDistanceToLocalPawn(distUe) end
        if tPawn.Replay_RefreshEnemyDisUI then tPawn:Replay_RefreshEnemyDisUI(distUe) end
        if tPawn.Replay_UpdateEnemyDisUI then tPawn:Replay_UpdateEnemyDisUI(distUe) end
        if tPawn.Replay_SetEnemyDis then tPawn:Replay_SetEnemyDis(meters) end
        if tPawn.Replay_SetDistance then tPawn:Replay_SetDistance(distUe) end
        if tPawn.Replay_SetPlayerName then tPawn:Replay_SetPlayerName(text) end
    end)
    local set = false
    pcall(function()
        for _, root in ipairs({
            tPawn.ReplayEnemyFrameUI, tPawn.EnemyFrameUI, tPawn.ReplayFrameUI,
            tPawn.ReplayUI, tPawn.PlayerInfoWidget, tPawn.FrameUI,
            tPawn.ReplayWidget, tPawn.BP_Replay_Four_States_UI, tPawn.EnemyInfoHUD
        }) do
            if slua.isValid(root) then
                local ui = root.UIRoot or root.Widget or root
                if setWidgetDistanceText(ui, text, meters) then
                    set = true
                    break
                end
            end
        end
    end)
    if not set then
        pcall(function()
            local ui = tPawn.ReplayEnemyFrameUI or tPawn.EnemyFrameUI
            ui = ui and (ui.UIRoot or ui)
            local nameTb = ui and (ui.TextBlock_Name or ui.Text_Name or ui.PlayerName)
            if nameTb and nameTb.SetText then
                local base = ''
                pcall(function() if nameTb.GetText then base = nameTb:GetText() or '' end end)
                if base == '' or base == text then
                    nameTb:SetText(text)
                elseif not base:find(text, 1, true) then
                    nameTb:SetText(base .. '\n' .. text)
                end
            end
        end)
    end
end

local function applyEnemyDistanceEsp(tPawn, distUe, uCon, headScreen, canvas)
    if not isValid(tPawn) then return end
    local meters = distUeToMeters(distUe)
    local text = tostring(meters) .. 'm'
    trySetReplayDistanceUi(tPawn, meters, distUe, text)
    canvas = canvas or resolveEspCanvas(uCon)
    if canvas and headScreen then
        drawEspDistanceOnCanvas(canvas, uCon, headScreen.X, headScreen.Y - 28, text)
    elseif canvas and isValid(uCon) and tPawn.K2_GetActorLocation then
        local headPos = tPawn:K2_GetActorLocation() + FVector(0, 0, 95)
        local screen = projectHeadToScreen(uCon, headPos)
        if screen then
            drawEspDistanceOnCanvas(canvas, uCon, screen.X, screen.Y - 28, text)
        end
    end
end

_G.__BGMI_DistToMeters = distUeToMeters
_G.__BGMI_ApplyEnemyDistance = applyEnemyDistanceEsp
_G.__BGMI_ProjectHeadToScreen = projectHeadToScreen
_G.__BGMI_ResolveEspCanvas = resolveEspCanvas
_G.__BGMI_DrawEspDistanceOnCanvas = drawEspDistanceOnCanvas

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

-- Small crosshair / tight spread (weapon nerf — NOT aimbot)
local function applyWeaponNerf(Character)
    if not isValid(Character) then return end
    local Weapon = Character.GetCurrentWeapon and Character:GetCurrentWeapon()
    if not isValid(Weapon) then return end
    local ShootEntity = Weapon.ShootWeaponEntity or Weapon.ShootWeaponEntity_GEN_VARIABLE
    if not isValid(ShootEntity) then return end
    ShootEntity.GameDeviationFactor = 0.1
    ShootEntity.AccessoriesHRecoilFactor = 0.5
    ShootEntity.AccessoriesVRecoilFactor = 0.3
    ShootEntity.RecoilKickADS = 0.11
    --[[ AUTO AIM — off
    if ShootEntity.AutoAimingConfig then
        if ShootEntity.AutoAimingConfig.OuterRange then
            ShootEntity.AutoAimingConfig.OuterRange.Speed = 7.5
            ShootEntity.AutoAimingConfig.OuterRange.SpeedRate = 6.5
        end
        if ShootEntity.AutoAimingConfig.InnerRange then
            ShootEntity.AutoAimingConfig.InnerRange.Speed = 8.0
            ShootEntity.AutoAimingConfig.InnerRange.SpeedRate = 7.5
        end
    end
    ]]
end

local function tickAimMagic()
    pcall(function()
        applyWeaponNerf(getPlayerCharacter())
    end)
end

local function getAllPlayerPawns()
    local Game = _G.Game
    if Game and Game.GetAllPlayerPawns then
        return Game.GetAllPlayerPawns()
    end
    return {}
end

-- Boot ESP: runs without mod body / BRPlayerCharacterBase patch
local BootEsp = { pawns = {}, lastRefresh = 0, probeAt = 0 }

local function collectBootEspPawns()
    local seen, list = {}, {}
    local function add(p)
        if isValid(p) and not seen[p] then
            seen[p] = true
            list[#list + 1] = p
        end
    end
    pcall(function()
        for _, p in pairs(getAllPlayerPawns()) do add(p) end
    end)
    pcall(function()
        local ok, GD = pcall(require, "GameLua.GameCore.Data.GameplayData")
        if ok and GD and GD.GetAllPlayerPawns then
            for _, p in pairs(GD.GetAllPlayerPawns() or {}) do add(p) end
        end
    end)
    pcall(function()
        local ok, GS = pcall(import, "GameplayStatics")
        if not ok or not GS then return end
        local hud = slua_GameFrontendHUD or _G.slua_GameFrontendHUD
        local world = hud and hud.GetWorld and hud:GetWorld()
        if not world and GS.GetWorldContextObject and hud then
            world = GS.GetWorldContextObject(hud, 0)
        end
        if not world then return end
        for _, classPath in ipairs({
            "/Script/ShadowTrackerExtra.STExtraPlayerCharacter",
            "/Script/ShadowTrackerExtra.STExtraBaseCharacter",
        }) do
            local okCls, actorClass = pcall(import, classPath)
            if okCls and actorClass and GS.GetAllActorsOfClass then
                local actors = GS.GetAllActorsOfClass(world, actorClass)
                if actors then
                    local count = actors.Num and actors:Num() or #actors
                    for i = 0, count - 1 do
                        local p = (type(actors.Get) == "function") and actors:Get(i) or actors[i + 1]
                        add(p)
                    end
                end
            end
        end
    end)
    return list
end

local function getLocalPawnBoot()
    local ch = getPlayerCharacter()
    if isValid(ch) then return ch end
    local pc = getPlayerController()
    if not isValid(pc) then return nil end
    local pawn = nil
    pcall(function()
        if pc.GetCurPawn then pawn = pc:GetCurPawn() end
        if not isValid(pawn) and pc.GetPawn then pawn = pc:GetPawn() end
        if not isValid(pawn) and pc.K2_GetPawn then pawn = pc:K2_GetPawn() end
        if not isValid(pawn) and pc.GetPlayerCharacterSafety then pawn = pc:GetPlayerCharacterSafety() end
        if not isValid(pawn) then pawn = pc.Pawn or pc.Character or pc.AcknowledgedPawn end
    end)
    return isValid(pawn) and pawn or nil
end

local function runBootEspTick()
    if not _G.Mod_LuaESP then return end
    if _G.__BGMI_RunEspTick then
        pcall(_G.__BGMI_RunEspTick)
        return
    end
    pcall(function()
        local me = getLocalPawnBoot()
        if not isValid(me) then return end
        local now = os.clock()
        if 0.35 < now - BootEsp.lastRefresh then
            BootEsp.lastRefresh = now
            BootEsp.pawns = collectBootEspPawns()
        end
        local myTeam = me.TeamID
        local shown = 0
        for _, ep in pairs(BootEsp.pawns) do
            if isValid(ep) and ep ~= me then
                local et = ep.TeamID
                if myTeam == nil or et == nil or myTeam ~= et then
                    local alive = true
                    pcall(function()
                        if ep.GetHealth then alive = (ep:GetHealth() or 0) > 0 end
                    end)
                    if alive then
                        local enemyPos = ep.K2_GetActorLocation and ep:K2_GetActorLocation()
                        local distUe = 0
                        if enemyPos then
                            local myPos = me:K2_GetActorLocation()
                            local dx = enemyPos.X - myPos.X
                            local dy = enemyPos.Y - myPos.Y
                            local dz = enemyPos.Z - myPos.Z
                            distUe = math.sqrt(dx * dx + dy * dy + dz * dz)
                        end
                        local pc = getPlayerController()
                        local canvas, headScreen = nil, nil
                        pcall(function()
                            if isValid(pc) then
                                local hud = pc.GetHUD and pc:GetHUD()
                                canvas = isValid(hud) and hud.Canvas or nil
                                if enemyPos then
                                    local headPos = enemyPos + FVector(0, 0, 85)
                                    headScreen = projectHeadToScreen(pc, headPos)
                                end
                            end
                        end)
                        pcall(function()
                            if ep.Replay_CreateEnemyFrameUI then ep:Replay_CreateEnemyFrameUI(true, true) end
                            if ep.Replay_SetVisiableOfFrameUI then ep:Replay_SetVisiableOfFrameUI(true) end
                            if ep.Replay_SetVisiableOfDistanceUI then ep:Replay_SetVisiableOfDistanceUI(true) end
                            if ep.SetPlayerNameVisible then ep:SetPlayerNameVisible(true) end
                        end)
                        applyEnemyDistanceEsp(ep, distUe, pc, headScreen, canvas)
                        shown = shown + 1
                    end
                end
            end
        end
        if 5 < now - BootEsp.probeAt then
            BootEsp.probeAt = now
            writeGameModProbe('boot_esp pawns=' .. tostring(#BootEsp.pawns) .. ' shown=' .. tostring(shown))
        end
    end)
end

_G.__BGMI_BootEspTick = runBootEspTick
_G.__BGMI_ESP_REGISTERED = true

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

_G.__BGMI_StartMatchFeatures = function(Self)
    if _G.Mod_MagicBullet then
        enlargeEnemyHitboxes(Self or getPlayerCharacter())
    end
end

local function exportMod(mod)
    _G.__BGMI_GAME_MOD_CACHE = mod
    local candidates = {
        "GameLua.Mod.BR.GamePlay.BRPlayerCharacterBase",
        "GameLua.Mod.BRMod.GamePlay.BRPlayerCharacterBase",
        "GameLua.Mod.BR.GamePlay.Player.BRPlayerCharacterBase",
        "GameLua.Mod.BRMod.GamePlay.Player.BRPlayerCharacterBase",
        "GameLua.Mod.BR.GamePlay.Character.BRPlayerCharacterBase",
        "GameLua.Mod.BRMod.GamePlay.Character.BRPlayerCharacterBase",
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
    if _G.Mod_LuaESP then
        if _G.__BGMI_EnsureEspTimer then pcall(_G.__BGMI_EnsureEspTimer) end
        if _G.__BGMI_RunEspTick then
            pcall(_G.__BGMI_RunEspTick)
        elseif _G.__BGMI_BootEspTick then
            pcall(_G.__BGMI_BootEspTick)
        end
    end
    local ch = getPlayerCharacter()
    if not isValid(ch) then return end
    pcall(tickAimMagic)
    --[[ AIM ASSIST / AIMBOT — commented off
    if _G.Mod_AimAssist then
        if _G.__BGMI_ApplyAimAssist then
            pcall(_G.__BGMI_ApplyAimAssist, ch)
        else
            tickAimMagic()
        end
    end
    ]]
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
    if not _G.Mytimer_ticker then
        local retries = (_G.__GAMEMOD_TICKER_RETRIES or 0) + 1
        _G.__GAMEMOD_TICKER_RETRIES = retries
        writeGameModProbe('ticker wait retry=' .. tostring(retries))
        if retries <= 120 then
            if _G.SetTimer then
                pcall(_G.SetTimer, 1.0, _G.ensureGameModTimers)
            end
        end
        pcall(_G.ApplyBgmiGameMod)
        return
    end
    _G.__GAMEMOD_TIMERS_STARTED = true
    writeGameModProbe('timers start esp=' .. tostring(_G.Mod_LuaESP) .. ' fail=' .. tostring(_G.__BGMI_LAST_FAIL))
    pcall(function()
        if _G.Mod_LuaESP and _G.__BGMI_EnsureEspTimer then pcall(_G.__BGMI_EnsureEspTimer) end
        _G.Mytimer_ticker.AddTimerLoop(0, function()
            if not _G.Mod_LuaESP then return end
            if _G.__BGMI_RunEspTick then
                pcall(_G.__BGMI_RunEspTick)
            elseif _G.__BGMI_BootEspTick then
                pcall(_G.__BGMI_BootEspTick)
            end
        end, -1, 0.05)
        _G.Mytimer_ticker.AddTimerOnce(2, function()
            pcall(function() if _G.ApplyBgmiGameMod then _G.ApplyBgmiGameMod() end end)
        end)
        _G.Mytimer_ticker.AddTimerLoop(5, function()
            pcall(function() if _G.ApplyBgmiGameMod then _G.ApplyBgmiGameMod() end end)
        end, -1, 5)
        _G.Mytimer_ticker.AddTimerLoop(1, runRuntimeFeatureTick, -1, 1)
        _G.Mytimer_ticker.AddTimerLoop(2, function()
            pcall(tryStartMatchFeatures)
        end, -1, 2)
    end)
end

function _G.__BGMI_StartGameModDriver()
    if _G.__BGMI_DRIVER_STARTED then return end
    if not _G.Mytimer_ticker or not _G.Mytimer_ticker.AddTimerLoop then return end
    _G.__BGMI_DRIVER_STARTED = true
    writeGameModProbe('driver start esp=' .. tostring(_G.Mod_LuaESP))
    pcall(function()
        _G.Mytimer_ticker.AddTimerLoop(3, function()
            pcall(function() if _G.ApplyBgmiGameMod then _G.ApplyBgmiGameMod() end end)
        end, -1, 3)
    end)
end

local function canRunModBody()
    return not _G.__BGMI_BODY_RAN
end

function _G.ApplyBgmiGameMod()
    pcall(function()
        if _G.__BGMI_ReloadConfig then pcall(_G.__BGMI_ReloadConfig) end
        if _G.__BGMI_RunModBody and not _G.__BGMI_BODY_RAN then
            if not canRunModBody() then
                _G.__BGMI_LAST_FAIL = "defer_body"
                writeGameModProbe('defer_body esp=' .. tostring(_G.Mod_LuaESP))
            else
                local ok, err = pcall(_G.__BGMI_RunModBody)
                if ok or _G.__BGMI_ESP_REGISTERED then
                    _G.__BGMI_BODY_RAN = true
                    if _G.__BGMI_LAST_FAIL == "no_pc" or _G.__BGMI_LAST_FAIL == "boot"
                        or _G.__BGMI_LAST_FAIL == "bypass_ok" or _G.__BGMI_LAST_FAIL == "defer_body" then
                        _G.__BGMI_LAST_FAIL = "body_ok"
                    end
                    writeGameModProbe('body ok esp=' .. tostring(_G.Mod_LuaESP))
                else
                    _G.__BGMI_LAST_FAIL = "body_err:" .. tostring(err)
                    writeGameModProbe('body err ' .. tostring(err))
                end
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
    _G.__GAMEMOD_TICKER_RETRIES = 0
    pcall(_G.ensureGameModTimers)
end
pcall(_G.__BGMI_StartGameModDriver)
