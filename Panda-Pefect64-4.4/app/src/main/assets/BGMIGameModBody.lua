-- Auto-built from BRPlayerCharacterBase.lua (points 1-5) — Anubis game mod body
_G.__BGMI_RunModBody = function()
if _G.__BGMI_BODY_RAN then return end

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
loadGameModConfig()

local CharacterBase, CombineClass, GameplayData, InGameMarkTools, SecurityCommonUtils, LegalMsg
pcall(function()
  CharacterBase = require("GameLua.GameCore.Framework.CharacterBase")
  CombineClass = require("combine_class")
  GameplayData = require("GameLua.GameCore.Data.GameplayData")
  InGameMarkTools = require("GameLua.Mod.BaseMod.Common.InGameMarkTools")
  SecurityCommonUtils = require("GameLua.Mod.BaseMod.Common.Security.SecurityCommonUtils")
  LegalMsg = require("client.slua.logic.common.logic_common_legal_msg")
end)

local function lazyInGameMarkTools()
  if InGameMarkTools then return InGameMarkTools end
  local ok, mod = pcall(require, "GameLua.Mod.BaseMod.Common.InGameMarkTools")
  if ok then InGameMarkTools = mod end
  return InGameMarkTools
end

local function lazyGameplayData()
  if GameplayData then return GameplayData end
  local ok, mod = pcall(require, "GameLua.GameCore.Data.GameplayData")
  if ok then GameplayData = mod end
  return GameplayData
end

local function noop()
end

local function InitializeBypass()
  if not _G.Mod_Bypass or _G.AKModBypassInitialized then
    return
  end
  pcall(function()
    local gc = _G.GameplayCallbacks or _G.GC
    if gc then
      gc.SendTssSdkAntiDataToLobby = noop
      gc.SendDSErrorLogToLobby = noop
      gc.SendDSHawkEyePatrolLogToLobby = noop
      gc.SendSecTLog = noop
      gc.SendDataMiningTLog = noop
      gc.SendActivityTLog = noop
    end
    local sm = require("GameLua.GameCore.Module.Subsystem.SubsystemMgr")
    if sm then
      local he = sm:Get("DSHawkEyePatrolSubsystem")
      if he then
        he.MarkSuspiciousPlayer = noop
      end
    end
    local cr = package.loaded["GameLua.Mod.BaseMod.Client.Security.ClientReportPlayerSubsystem"] or require("GameLua.Mod.BaseMod.Client.Security.ClientReportPlayerSubsystem")
    if cr then
      cr.OnInit = noop
      cr._OnPlayerKilledOtherPlayer = noop
      cr._RecordFatalDamager = noop
      cr._OnBattleResult = noop
    end
    local dr = package.loaded["GameLua.Mod.BaseMod.DS.Security.DSReportPlayerSubsystem"] or require("GameLua.Mod.BaseMod.DS.Security.DSReportPlayerSubsystem")
    if dr then
      dr.OnInit = noop
      dr._OnCharacterDied = noop
      dr._RecordFatalDamager = noop
    end
    pcall(function()
      local hb = package.loaded["GameLua.Mod.BaseMod.Common.Security.HiggsBosonComponent"]
      if not hb then
        local ok, result = pcall(require, "GameLua.Mod.BaseMod.Common.Security.HiggsBosonComponent")
        if ok then
          hb = result
        end
      end
      if hb then
        hb.ControlMHActive = noop
        hb.Tick = noop
        hb.OnTick = noop
        hb.ReceiveTick = noop
        hb.MHActiveLogic = noop
        hb.TriggerAvatarCheck = noop
        hb.StartAvatarCheck = noop
        hb.ReportItemID = noop
        hb.OnReportItemID = noop
        hb.ReceiveAnyDamage = noop
        hb.OnWeaponHitRecord = noop
        hb.ShowSecurityAlert = noop

        function hb.GetNetAvatarItemIDs()
          return {}
        end

        function hb.GetCurWeaponSkinID()
          return 0
        end

        if hb.StaticShowSecurityAlertInDev then
          hb.StaticShowSecurityAlertInDev = noop
        end
      end
      if _G.AvatarCheckCallback then
        _G.AvatarCheckCallback.StartAvatarCheck = noop
        _G.AvatarCheckCallback.OnReportItemID = noop

        function _G.AvatarCheckCallback.PostPlayerControllerLoginInit(controller)
          if slua.isValid(controller) and controller.HiggsBosonComponent then
            pcall(function()
              controller.HiggsBosonComponent:ControlMHActive(0)
              controller.HiggsBosonComponent.bMHActive = false
            end)
          end
        end
      end
      if _G.DisableHiggsBoson then
        local origDisable = _G.DisableHiggsBoson

        function _G.DisableHiggsBoson()
          pcall(origDisable)
        end
      end
    end)
    if gc then
      local origOnDSPlayerStateChanged = gc.OnDSPlayerStateChanged

      function gc:OnDSPlayerStateChanged(playerState, reason, ...)
        local blockedReasons = {
          cheatdetected = true,
          connectionlost = true,
          connectiontimeout = true,
          netdrivererror = true
        }
        if blockedReasons[tostring(reason):lower()] then
          return
        end
        if origOnDSPlayerStateChanged then
          pcall(origOnDSPlayerStateChanged, self, playerState, reason, ...)
        end
      end

      gc.OnPlayerRPCValidateFailed = noop
      gc.OnPlayerActorChannelError = noop
      gc.OnPlayerSpectateException = noop
      gc.OnShutdownAfterError = noop
      gc.OnPlayerNetConnectionClosed = noop
    end
    _G.AKModBypassInitialized = true
  end)
end

if _G.Mod_Bypass then InitializeBypass() end

local SharedVisualAssistOwner
local COLOR_HP_GREEN, COLOR_HP_YELLOW, COLOR_HP_RED, COLOR_BG
local VEC_Z85, VEC_Z90
pcall(function()
  COLOR_HP_GREEN = FLinearColor(0, 1, 0, 0.95)
  COLOR_HP_YELLOW = FLinearColor(1, 1, 0, 0.95)
  COLOR_HP_RED = FLinearColor(1, 0, 0, 0.95)
  COLOR_BG = FLinearColor(0, 0, 0, 0.55)
  COLOR_SNAPLINE = FLinearColor(1, 1, 1, 0.9)
  VEC_Z85, VEC_Z90 = FVector(0, 0, 85), FVector(0, 0, 90)
end)
local RPCDefinitions = {}
pcall(function()
  RPCDefinitions = {
    ServerRPC = {
      ServerRPC_NearDeathGiveupRescue = { Reliable = true, Params = {} },
      ServerRPC_CarryDeadBox = { Reliable = true, Params = { UEnums.EPropertyClass.Object } },
      RPC_Server_GmPlayAction = { Reliable = true, Params = { UEnums.EPropertyClass.Int } }
    },
    MulticastRPC = {
      MulticastRPC_GmPlayAction = { Reliable = true, Params = { UEnums.EPropertyClass.Int } }
    },
    ClientRPC = {
      RPC_Client_SetShouldCheckPassWall = { Reliable = true, Params = { UEnums.EPropertyClass.Bool } }
    }
  }
end)
_G.ServerRPC = RPCDefinitions.ServerRPC
_G.ClientRPC = RPCDefinitions.ClientRPC
_G.MulticastRPC = RPCDefinitions.MulticastRPC

local function IsPawnAlive(p)
  if not slua.isValid(p) then return false end
  if p.HealthStatus and SecurityCommonUtils and SecurityCommonUtils.IsHealthStatusAlive then
    return SecurityCommonUtils.IsHealthStatusAlive(p.HealthStatus)
  end
  if p.IsAlive then return p:IsAlive() end
  return p.GetHealth and 0 < (p:GetHealth() or 0) or false
end

local function GetPawnHealthRatio(p)
  local hp = p.GetHealth and p:GetHealth() or 100
  local maxHp = p.GetHealthMax and p:GetHealthMax() or 100
  return math.max(0, math.min(1, hp / (maxHp <= 0 and 100 or maxHp)))
end

local function getLocalPawnFromController(uCon)
  if not slua.isValid(uCon) then return nil end
  local pawn = nil
  pcall(function()
    if uCon.GetCurPawn then pawn = uCon:GetCurPawn() end
    if not slua.isValid(pawn) and uCon.GetPawn then pawn = uCon:GetPawn() end
    if not slua.isValid(pawn) and uCon.K2_GetPawn then pawn = uCon:K2_GetPawn() end
    if not slua.isValid(pawn) and uCon.GetPlayerCharacterSafety then pawn = uCon:GetPlayerCharacterSafety() end
    if not slua.isValid(pawn) then pawn = uCon.Pawn or uCon.Character or uCon.AcknowledgedPawn end
  end)
  return slua.isValid(pawn) and pawn or nil
end

local function IsLocalPlayerCharacter(char)
  if not char or not slua.isValid(char) then return false end
  local ok, result = pcall(function()
    local GD = lazyGameplayData()
    if GD and GD.GetPlayerCharacter then
      local lp = GD.GetPlayerCharacter()
      if slua.isValid(lp) and lp == char then return true end
    end
    local uCon = slua_GameFrontendHUD and slua_GameFrontendHUD:GetPlayerController()
    local localPawn = getLocalPawnFromController(uCon)
    return slua.isValid(localPawn) and localPawn == char
  end)
  return ok and result == true
end

local function IsEnemyPawn(myPawn, tPawn)
  if not slua.isValid(myPawn) or not slua.isValid(tPawn) or tPawn == myPawn then
    return false
  end
  local myTeam, otherTeam = myPawn.TeamID, tPawn.TeamID
  if myTeam ~= nil and otherTeam ~= nil and myTeam ~= 0 and otherTeam ~= 0 then
    return myTeam ~= otherTeam
  end
  if myPawn.CampID and tPawn.CampID and myPawn.CampID ~= 0 and tPawn.CampID ~= 0 then
    return myPawn.CampID ~= tPawn.CampID
  end
  return true
end

local function CollectAllPlayerPawns()
  local seen, list = {}, {}
  local function add(p)
    if p and slua.isValid(p) and not seen[p] then
      seen[p] = true
      table.insert(list, p)
    end
  end
  pcall(function()
    local GameRef = _G.Game
    if GameRef and GameRef.GetAllPlayerPawns then
      for _, p in pairs(GameRef:GetAllPlayerPawns() or {}) do add(p) end
    end
  end)
  pcall(function()
    local GD = lazyGameplayData()
    if GD and GD.GetAllPlayerPawns then
      for _, p in pairs(GD.GetAllPlayerPawns() or {}) do add(p) end
    end
  end)
  pcall(function()
    local GS = import("GameplayStatics")
    local world = slua_GameFrontendHUD and slua_GameFrontendHUD.GetWorld and slua_GameFrontendHUD:GetWorld()
    if not GS or not world then return end
    local classes = {
      "/Script/ShadowTrackerExtra.STExtraPlayerCharacter",
      "/Script/ShadowTrackerExtra.STExtraBaseCharacter"
    }
    for _, classPath in ipairs(classes) do
      local ok, actorClass = pcall(import, classPath)
      if ok and actorClass and GS.GetAllActorsOfClass then
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
  pcall(function()
    local GS = import("GameplayStatics")
    local world = slua_GameFrontendHUD and slua_GameFrontendHUD.GetWorld and slua_GameFrontendHUD:GetWorld()
    local ok, psClass = pcall(import, "/Script/ShadowTrackerExtra.STExtraPlayerState")
    if not GS or not world or not ok or not psClass then return end
    local states = GS.GetAllActorsOfClass(world, psClass)
    if not states then return end
    local count = states.Num and states:Num() or #states
    for i = 0, count - 1 do
      local ps = (type(states.Get) == "function") and states:Get(i) or states[i + 1]
      if slua.isValid(ps) then
        local p = (ps.GetPawn and ps:GetPawn()) or ps.Pawn or ps.Character
        add(p)
      end
    end
  end)
  return list
end

_G.Mod_AimAssist = false
_G.Mod_LuaESP = _G.Mod_LuaESP ~= false
_G.Mod_MagicHead = _G.Mod_MagicHead == true
_G.Mod_MagicBullet = _G.Mod_MagicBullet == true

local EspState = { cachedMarks = {}, cachedPawns = {}, lastPawnRefresh = 0 }

local function RunEspVisualFrame()
  if not _G.Mod_LuaESP then return end
  pcall(function()
    local marks = lazyInGameMarkTools()
    local hud = slua_GameFrontendHUD or _G.slua_GameFrontendHUD
    local uCon = hud and hud.GetPlayerController and hud:GetPlayerController()
    if not slua.isValid(uCon) then
      uCon = nil
      pcall(function()
        local ok, GS = pcall(import, "GameplayStatics")
        local world = hud and hud.GetWorld and hud:GetWorld()
        if ok and GS and world and GS.GetPlayerController then
          uCon = GS.GetPlayerController(world, 0)
        end
      end)
    end
    if not slua.isValid(uCon) then return end
    local currentPawn = getLocalPawnFromController(uCon)
    if not slua.isValid(currentPawn) then return end
    local myPos = currentPawn:K2_GetActorLocation()
    local HUD = uCon:GetHUD()
    local Canvas = slua.isValid(HUD) and HUD.Canvas or nil
    local now = os.clock()
    local cachedMarks, cachedPawns = EspState.cachedMarks, EspState.cachedPawns
    if 0.35 < now - EspState.lastPawnRefresh then
      EspState.lastPawnRefresh = now
      cachedPawns = CollectAllPlayerPawns()
      EspState.cachedPawns = cachedPawns
      for pawnPtr, markId in pairs(cachedMarks) do
        if pawnPtr ~= "_time" then
          local found = false
          for _, p in pairs(cachedPawns) do
            if p == pawnPtr then found = true; break end
          end
          if not found then
            if markId and marks then pcall(function() marks.HideMapMark(markId) end) end
            cachedMarks[pawnPtr] = nil
          end
        end
      end
    end
    for _, tPawn in pairs(cachedPawns) do
      if IsEnemyPawn(currentPawn, tPawn) then
        if IsPawnAlive(tPawn) then
          local enemyPos = tPawn:K2_GetActorLocation()
          local dx, dy, dz = enemyPos.X - myPos.X, enemyPos.Y - myPos.Y, enemyPos.Z - myPos.Z
          local dist = math.sqrt(dx * dx + dy * dy + dz * dz)
          if dist < 600000 then
            if tPawn.Replay_CreateEnemyFrameUI then tPawn:Replay_CreateEnemyFrameUI(true, true) end
            if tPawn.Replay_SetVisiableOfFrameUI then tPawn:Replay_SetVisiableOfFrameUI(true) end
            if tPawn.Replay_SetVisiableOfDistanceUI then tPawn:Replay_SetVisiableOfDistanceUI(true) end
            if tPawn.SetPlayerNameVisible then tPawn:SetPlayerNameVisible(true) end
            local distText = tostring((_G.__BGMI_DistToMeters and _G.__BGMI_DistToMeters(dist)) or math.floor(dist / 100)) .. 'm'
            if _G.__BGMI_ApplyEnemyDistance then
              pcall(_G.__BGMI_ApplyEnemyDistance, tPawn, dist, uCon, nil, nil)
            end
            local headPos, rootPos
            if 150000 < dist then
              headPos, rootPos = enemyPos + VEC_Z85, enemyPos - VEC_Z85
            else
              local realHead = tPawn:GetHeadLocation(false)
              headPos = realHead or enemyPos + VEC_Z85
              rootPos = realHead and enemyPos - VEC_Z90 or enemyPos - VEC_Z85
            end
            cachedMarks._time = cachedMarks._time or {}
            if now - (cachedMarks._time[tPawn] or 0) > 1.5 then
              cachedMarks._time[tPawn] = now
              if marks then
                local distText = tostring((_G.__BGMI_DistToMeters and _G.__BGMI_DistToMeters(dist)) or math.floor(dist / 100)) .. 'm'
                if cachedMarks[tPawn] then
                  pcall(function() marks.UpdateMapMarkLocation(cachedMarks[tPawn], headPos) end)
                  if marks.UpdateMapMarkCustomText then
                    pcall(function() marks.UpdateMapMarkCustomText(cachedMarks[tPawn], distText) end)
                  end
                else
                  cachedMarks[tPawn] = marks.ClientAddMapMark(1006, headPos, 0, distText, 4, tPawn)
                end
              end
            end
            if Canvas then
              local headScreen, rootScreen = FVector2D(0, 0), FVector2D(0, 0)
              if uCon:ProjectWorldLocationToScreen(headPos, false, headScreen) and uCon:ProjectWorldLocationToScreen(rootPos, false, rootScreen) then
                if _G.__BGMI_DrawEspDistanceOnCanvas then
                  pcall(_G.__BGMI_DrawEspDistanceOnCanvas, Canvas, uCon, headScreen.X, headScreen.Y - 30, distText)
                end
                local screenHeight = math.max(25, math.abs(headScreen.Y - rootScreen.Y))
                local scaleFactor = math.max(0.3, math.min(1.5, 15000 / math.max(10000, dist)))
                local barWidth, barHeight = 4 * scaleFactor, screenHeight * scaleFactor
                local barX, barY = headScreen.X - barWidth * 1.5, headScreen.Y
                local hp = GetPawnHealthRatio(tPawn)
                local color = hp < 0.3 and COLOR_HP_RED or hp < 0.6 and COLOR_HP_YELLOW or COLOR_HP_GREEN
                local fillHeight = math.max(1, barHeight * hp)
                Canvas:K2_DrawBox(FVector2D(barX, barY), FVector2D(barWidth, barHeight), 1, COLOR_BG)
                Canvas:K2_DrawBox(FVector2D(barX, barY + barHeight - fillHeight), FVector2D(barWidth, fillHeight), 1, color)
              end
            elseif _G.__BGMI_ResolveEspCanvas and _G.__BGMI_DrawEspDistanceOnCanvas then
              local fallbackCanvas = _G.__BGMI_ResolveEspCanvas(uCon)
              local fallbackScreen = _G.__BGMI_ProjectHeadToScreen and _G.__BGMI_ProjectHeadToScreen(uCon, headPos)
              if fallbackCanvas and fallbackScreen then
                pcall(_G.__BGMI_DrawEspDistanceOnCanvas, fallbackCanvas, uCon, fallbackScreen.X, fallbackScreen.Y - 30, distText)
              end
            end
          else
            HideEnemyAssistForPawn(tPawn, cachedMarks)
          end
        else
          HideEnemyAssistForPawn(tPawn, cachedMarks)
        end
      end
    end
  end)
end

_G.__BGMI_RunEspTick = RunEspVisualFrame
_G.__BGMI_ESP_REGISTERED = true

_G.__BGMI_EnsureEspTimer = function()
  if _G.__BGMI_ESP_TIMER_ON or not _G.__BGMI_RunEspTick then return end
  local ticker = _G.Mytimer_ticker
  if not ticker or not ticker.AddTimerLoop then return end
  _G.__BGMI_ESP_TIMER_ON = true
  pcall(function()
    ticker.AddTimerLoop(0, function()
      if _G.__BGMI_RunEspTick then
        pcall(_G.__BGMI_RunEspTick)
      elseif _G.__BGMI_BootEspTick then
        pcall(_G.__BGMI_BootEspTick)
      end
    end, -1, 0.05)
  end)
end

if _G.Mytimer_ticker and _G.Mod_LuaESP then
  pcall(_G.__BGMI_EnsureEspTimer)
end

--[[ AIM ASSIST / AIMBOT — commented off
local AimAssistConfig = {
  Enabled = true, AimSpeed = 7.5, AimRangeRate = 4.0, AimSpeedRate = 6.5,
  AimRangeRateSight = 4.8, AimSpeedRateSight = 6.8, HeadShotRate = 1.0,
  HeadShotRateSight = 1.0, BodyAimRate = 0.0, ChestRate = 0.0,
  CrouchRate = 0.75, ProneRate = 0.55,
}

local function get_shoot_weapon_entity(weapon)
  if not weapon or not slua.isValid(weapon) then return nil end
  local comp = weapon.ShootWeaponEntityComp
  if comp and slua.isValid(comp) then return comp end
  comp = weapon.ShootWeaponEntity_GEN_VARIABLE or weapon.ShootWeaponEntity
  if comp and slua.isValid(comp) then return comp end
  return nil
end

local function ApplyAimAssist(self)
  if not Client then return end
  if not _G.Mod_AimAssist then return end
  pcall(function()
    local player = GameplayData.GetPlayerCharacter()
    if not slua.isValid(player) then return end
    if not player.GetCurrentWeapon then return end
    local weapon = player:GetCurrentWeapon()
    if not slua.isValid(weapon) then return end
    local shootWeapon = get_shoot_weapon_entity(weapon)
    if not shootWeapon then return end
    if shootWeapon.AutoAimingConfig then
      for _, range in ipairs({"OuterRange", "InnerRange"}) do
        local cfg = shootWeapon.AutoAimingConfig[range]
        if cfg then
          cfg.Speed = AimAssistConfig.AimSpeed
          cfg.RangeRate = AimAssistConfig.AimRangeRate
          cfg.SpeedRate = AimAssistConfig.AimSpeedRate
          cfg.RangeRateSight = AimAssistConfig.AimRangeRateSight
          cfg.SpeedRateSight = AimAssistConfig.AimSpeedRateSight
          cfg.HeadShotRate = AimAssistConfig.HeadShotRate
          cfg.HeadShotRateSight = AimAssistConfig.HeadShotRateSight
          cfg.BodyAimRate = AimAssistConfig.BodyAimRate
          cfg.ChestRate = AimAssistConfig.ChestRate
          cfg.CrouchRate = AimAssistConfig.CrouchRate
          cfg.ProneRate = AimAssistConfig.ProneRate
        end
      end
    end
  end)
end
]]

local function HideEnemyAssistForPawn(tPawn, cachedMarks)
  if not slua.isValid(tPawn) then return end
  if tPawn.Replay_SetVisiableOfFrameUI then tPawn:Replay_SetVisiableOfFrameUI(false) end
  if tPawn.SetPlayerNameVisible then tPawn:SetPlayerNameVisible(false) end
  if cachedMarks then
    if cachedMarks._time then cachedMarks._time[tPawn] = nil end
    if cachedMarks[tPawn] then
      local marks = lazyInGameMarkTools()
      if marks then pcall(function() marks.HideMapMark(cachedMarks[tPawn]) end) end
      cachedMarks[tPawn] = nil
    end
  end
end

local function CleanupVisualAssistance(self)
  if not self then return end
  if self._AssistTimer then pcall(function() self:RemoveGameTimer(self._AssistTimer) end); self._AssistTimer = nil end
  if self._MagicBulletTimer then pcall(function() self:RemoveGameTimer(self._MagicBulletTimer) end); self._MagicBulletTimer = nil end
  if self._MagicHeadTimer then pcall(function() self:RemoveGameTimer(self._MagicHeadTimer) end); self._MagicHeadTimer = nil end
  if SharedVisualAssistOwner == self then SharedVisualAssistOwner = nil end
end

local function ClearStaleVisualAssistOwner()
  if SharedVisualAssistOwner and not slua.isValid(SharedVisualAssistOwner.Object) then
    CleanupVisualAssistance(SharedVisualAssistOwner)
  end
end

local function _unused_ShowLegalCredit()
  if popupShown then return end
  popupShown = true
  local content = table.concat({
    "RiP Pak | Premium Edition", "", "Stable Gameplay", "",
    "Telegram:", "@iam_asam", "", "Thank you for trusting RiP Pak Framework."
  }, "\n")
  LegalMsg.ShowOnePopUI({
    tabType = 999, title = "ADVANCED Pak Loaded", content = content,
    btnOKText = "OK", btnCancleText = "Telegram",
    acceptFunc = function() end,
    refuseFunc = function() import("KismetSystemLibrary"):LaunchURL("https://t.me/@Iam_asam") end
  })
end

local function ApplyMagicHeadHitboxes(char)
  if not Client or not slua.isValid(char) then return end
  pcall(function()
    local player = GameplayData.GetPlayerCharacter()
    if not slua.isValid(player) then return end
    local allChars = Game:GetAllPlayerPawns() or {}
    for _, c in pairs(allChars) do
      if slua.isValid(c) and c ~= player and c.TeamID ~= player.TeamID then
        local mesh = c.Mesh
        if slua.isValid(mesh) then
          local physAsset = mesh.PhysicsAssetOverride
          if not slua.isValid(physAsset) and slua.isValid(mesh.SkeletalMesh) then
            physAsset = mesh.SkeletalMesh.PhysicsAsset
          end
          if slua.isValid(physAsset) and physAsset.SkeletalBodySetups then
            _G._MBonesHead = _G._MBonesHead or {}
            local assetName = (physAsset.GetName and physAsset:GetName()) or tostring(physAsset)
            if not _G._MBonesHead[assetName] then
              local mb = {["head"] = 200}
              local setups = physAsset.SkeletalBodySetups
              for i = 1, 80 do
                local bs = nil
                pcall(function() bs = (type(setups.Get) == "function") and setups:Get(i - 1) or setups[i] end)
                if not bs or not slua.isValid(bs) then break end
                local bn = tostring(bs.BoneName):lower()
                local pct = nil
                for pat, val in pairs(mb) do
                  if string.find(bn, pat) then pct = val; break end
                end
                if pct then
                  local sc = 1.0 + pct / 100.0
                  local ag = bs.AggGeom
                  pcall(function()
                    local bx = (ag and ag.BoxElems) or bs.BoxElems
                    if bx then
                      local b = (type(bx.Get) == "function") and bx:Get(0) or bx[1]
                      if b then
                        b.X = (b.X or 30) * sc; b.Y = (b.Y or 30) * sc; b.Z = (b.Z or 60) * sc
                        if type(bx.Set) == "function" then bx:Set(0, b) else bx[1] = b end
                        if ag then bs.AggGeom = ag else bs.BoxElems = bx end
                      end
                    end
                  end)
                  pcall(function()
                    local sp = (ag and ag.SphylElems) or bs.SphylElems
                    if sp then
                      local s = (type(sp.Get) == "function") and sp:Get(0) or sp[1]
                      if s then
                        if s.Radius then s.Radius = s.Radius * sc end
                        if s.Length then s.Length = s.Length * sc end
                        if type(sp.Set) == "function" then sp:Set(0, s) else sp[1] = s end
                        if ag then bs.AggGeom = ag else bs.SphylElems = sp end
                      end
                    end
                  end)
                  pcall(function()
                    local sr = (ag and ag.SphereElems) or bs.SphereElems
                    if sr then
                      local r = (type(sr.Get) == "function") and sr:Get(0) or sr[1]
                      if r and r.Radius then
                        r.Radius = r.Radius * sc
                        if type(sr.Set) == "function" then sr:Set(0, r) else sr[1] = r end
                        if ag then bs.AggGeom = ag else bs.SphereElems = sr end
                      end
                    end
                  end)
                end
              end
              _G._MBonesHead[assetName] = true
              if mesh.RecreatePhysicsState then mesh:RecreatePhysicsState() end
            end
          end
        end
      end
    end
  end)
end

local function ApplyMagicBulletHitboxes(char)
  if not slua.isValid(char) then return end
  pcall(function()
    local allChars = Game:GetAllPlayerPawns() or {}
    for _, c in pairs(allChars) do
      if slua.isValid(c) and c ~= char and c.TeamID ~= char.TeamID then
        local mesh = c.Mesh
        if slua.isValid(mesh) then
          local physAsset = mesh.PhysicsAssetOverride
          if not slua.isValid(physAsset) and slua.isValid(mesh.SkeletalMesh) then
            physAsset = mesh.SkeletalMesh.PhysicsAsset
          end
          if slua.isValid(physAsset) and physAsset.SkeletalBodySetups then
            _G._MBones = _G._MBones or {}
            local assetName = (physAsset.GetName and physAsset:GetName()) or tostring(physAsset)
            if not _G._MBones[assetName] then
              local mb = {
                ["head"] = 200, ["neck_01"] = 150, ["pelvis"] = 150,
                ["spine_01"] = 150, ["spine_02"] = 150, ["spine_03"] = 150,
                ["upperarm_l"] = 150, ["upperarm_r"] = 150,
                ["lowerarm_l"] = 130, ["lowerarm_r"] = 130,
                ["hand_l"] = 100, ["hand_r"] = 100,
                ["thigh_l"] = 150, ["thigh_r"] = 150,
                ["calf_l"] = 130, ["calf_r"] = 130,
                ["foot_l"] = 100, ["foot_r"] = 100
              }
              local setups = physAsset.SkeletalBodySetups
              for i = 1, 80 do
                local bs = nil
                pcall(function() bs = (type(setups.Get) == "function") and setups:Get(i - 1) or setups[i] end)
                if not bs or not slua.isValid(bs) then break end
                local bn = tostring(bs.BoneName):lower()
                local pct = nil
                for pat, val in pairs(mb) do
                  if string.find(bn, pat) then pct = val; break end
                end
                if pct then
                  local sc = 1.0 + pct / 100.0
                  local ag = bs.AggGeom
                  pcall(function()
                    local bx = (ag and ag.BoxElems) or bs.BoxElems
                    if bx then
                      local b = (type(bx.Get) == "function") and bx:Get(0) or bx[1]
                      if b then
                        b.X = (b.X or 30) * sc; b.Y = (b.Y or 30) * sc; b.Z = (b.Z or 60) * sc
                        if type(bx.Set) == "function" then bx:Set(0, b) else bx[1] = b end
                        if ag then bs.AggGeom = ag else bs.BoxElems = bx end
                      end
                    end
                  end)
                  pcall(function()
                    local sp = (ag and ag.SphylElems) or bs.SphylElems
                    if sp then
                      local s = (type(sp.Get) == "function") and sp:Get(0) or sp[1]
                      if s then
                        if s.Radius then s.Radius = s.Radius * sc end
                        if s.Length then s.Length = s.Length * sc end
                        if type(sp.Set) == "function" then sp:Set(0, s) else sp[1] = s end
                        if ag then bs.AggGeom = ag else bs.SphylElems = sp end
                      end
                    end
                  end)
                  pcall(function()
                    local sr = (ag and ag.SphereElems) or bs.SphereElems
                    if sr then
                      local r = (type(sr.Get) == "function") and sr:Get(0) or sr[1]
                      if r and r.Radius then
                        r.Radius = r.Radius * sc
                        if type(sr.Set) == "function" then sr:Set(0, r) else sr[1] = r end
                        if ag then bs.AggGeom = ag else bs.SphereElems = sr end
                      end
                    end
                  end)
                end
              end
              _G._MBones[assetName] = true
              if mesh.RecreatePhysicsState then mesh:RecreatePhysicsState() end
            end
          end
        end
      end
    end
  end)
end

local BRPlayerCharacterBase
if CharacterBase and CombineClass then
BRPlayerCharacterBase = Class(CharacterBase, nil, {
  ServerRPC = RPCDefinitions.ServerRPC,
  ClientRPC = RPCDefinitions.ClientRPC,
  MulticastRPC = RPCDefinitions.MulticastRPC,
  ctor = function(self)
    self._AssistTimer = nil
    self._MagicBulletTimer = nil
    self._MagicHeadTimer = nil
  end,
  _PostConstruct = function(self)
    CharacterBase._PostConstruct(self)
    self:InitAddSpecialMoveInfo()
    self.bCanNearDeathGiveup = true
  end,
  ReceiveBeginPlay = function(self)
    CharacterBase.ReceiveBeginPlay(self)
    self:SetActorTickEnabled(true)
    EventSystem:postEvent(EVENTTYPE_SINGLETRAINING, EVENTID_CHARACTER_BEGINPLAY, self.Object)
    if Client and (_G.Mod_LuaESP or _G.Mod_MagicHead or _G.Mod_MagicBullet) then
      ClearStaleVisualAssistOwner()
      if _G.Mod_LuaESP and _G.__BGMI_EnsureEspTimer then
        pcall(_G.__BGMI_EnsureEspTimer)
      end
      if IsLocalPlayerCharacter(self.Object) then
        self:InitVisualAssistance()
      end
      if _G.Mod_LuaESP then
        for _, delay in ipairs({0.5, 1.5, 3.0}) do
          self:AddGameTimer(delay, false, function()
            if slua.isValid(self.Object) and IsLocalPlayerCharacter(self.Object) then
              self:InitVisualAssistance()
            end
            if _G.__BGMI_EnsureEspTimer then pcall(_G.__BGMI_EnsureEspTimer) end
          end)
        end
      end
    end
  end,
  ReceiveEndPlay = function(self, reason)
    CleanupVisualAssistance(self)
    CharacterBase.ReceiveEndPlay(self, reason)
    if Client and GameplayData.RemoveCharacter then GameplayData.RemoveCharacter(self.Object) end
  end,
  InitVisualAssistance = function(self)
    if not Client then return end
    if _G.Mod_LuaESP and _G.__BGMI_EnsureEspTimer then
      pcall(_G.__BGMI_EnsureEspTimer)
    end
    local iAmLocal = IsLocalPlayerCharacter(self.Object)
    if not iAmLocal then return end
    ClearStaleVisualAssistOwner()
    if SharedVisualAssistOwner and SharedVisualAssistOwner ~= self then
      if slua.isValid(SharedVisualAssistOwner.Object) then
        CleanupVisualAssistance(SharedVisualAssistOwner)
      else
        CleanupVisualAssistance(SharedVisualAssistOwner)
      end
    end
    if self._AssistTimer then CleanupVisualAssistance(self) end
    SharedVisualAssistOwner = self

    if _G.Mod_MagicBullet and not self._MagicBulletTimer then
      self._MagicBulletTimer = self:AddGameTimer(2.0, true, function()
        if slua.isValid(self.Object) then ApplyMagicBulletHitboxes(self.Object) end
      end)
    end

    if _G.Mod_MagicHead and not self._MagicHeadTimer then
      self._MagicHeadTimer = self:AddGameTimer(2.0, true, function()
        if slua.isValid(self.Object) then ApplyMagicHeadHitboxes(self.Object) end
      end)
    end

    if not _G.Mod_LuaESP then return end
    if _G.__BGMI_EnsureEspTimer then pcall(_G.__BGMI_EnsureEspTimer) end
    if not EspState.cachedPawns or not next(EspState.cachedPawns) then
      EspState.cachedPawns = CollectAllPlayerPawns()
      EspState.lastPawnRefresh = os.clock()
    end
    self._AssistTimer = self:AddGameTimer(0.05, true, function()
      pcall(function()
        if not slua.isValid(self.Object) then CleanupVisualAssistance(self); return end
        RunEspVisualFrame()
      end)
    end)
  end
})
BRPlayerCharacterBase = CombineClass.DeclareFeature(BRPlayerCharacterBase, {
  { SkyTransition = "GameLua.Mod.BaseMod.Gameplay.Feature.SkyControl.PlayerCharacterSkyTransitionFeature" },
  { CarryDeadBoxFeature = "GameLua.Mod.Library.GamePlay.Feature.CarryDeadBoxFeature" },
  { SpecialSuitFeature = "GameLua.Mod.Library.GamePlay.Feature.SpecialSuitFeature" },
  { TeleportPawnFeature = "GameLua.Mod.Library.GamePlay.Feature.TeleportPawnFeature" },
  { LifterControl = "GameLua.Mod.BaseMod.Gameplay.Feature.Player.CharacterLifterControlFeature" },
  { FinalKillEffect = "GameLua.Mod.BaseMod.Gameplay.Feature.Player.PlayerCharacterFinalKillEffectFeature" },
  { CampFeature = "GameLua.Mod.BaseMod.GamePlay.Feature.Camp.PlayerCharacterCampFeature" },
  { BuildSkateFeature = "GameLua.Mod.BaseMod.GamePlay.Feature.PlayerCharacterBuildVehicleFeature" },
  { CommonBornlandTransformFeature = "GameLua.Mod.BaseMod.GamePlay.Feature.HeroPropFeature.CommonBornlandTransformFeature" }
}, "BRPlayerCharacterBase")
if _G.__BGMI_DoExportMod then
    _G.__BGMI_DoExportMod(BRPlayerCharacterBase)
    _G.__BGMI_GAME_MOD_PATCHED = true
end
end

_G.__BGMI_StartMatchFeatures = function(self)
    pcall(function()
        if _G.Mod_LuaESP and _G.__BGMI_EnsureEspTimer then
            pcall(_G.__BGMI_EnsureEspTimer)
        end
        if slua.isValid(self) and self.InitVisualAssistance and _G.Mod_LuaESP then
            pcall(function() self:InitVisualAssistance() end)
        end
    end)
end

_G.__BGMI_InitVisualAssistance = function(ch)
    if _G.Mod_LuaESP and _G.__BGMI_EnsureEspTimer then
        pcall(_G.__BGMI_EnsureEspTimer)
    end
    if _G.Mod_LuaESP and _G.__BGMI_RunEspTick then
        pcall(_G.__BGMI_RunEspTick)
    elseif _G.Mod_LuaESP and _G.__BGMI_BootEspTick then
        pcall(_G.__BGMI_BootEspTick)
    end
    if ch and slua.isValid(ch) and ch.InitVisualAssistance and _G.Mod_LuaESP then
        pcall(function() ch:InitVisualAssistance() end)
    end
end

if _G.__BGMI_ESP_REGISTERED then
  _G.__BGMI_FEATURES_ACTIVE = true
end

end
