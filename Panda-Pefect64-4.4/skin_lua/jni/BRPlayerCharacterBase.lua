require("client.slua.umg.NewSetting.Account.setting_account_protect")
require("client.slua.umg.NewSetting.Account.setting_account_channel")
require("client.slua.umg.NewSetting.Account.setting_account_bottom")
local UI = require("client.slua.umg.NewSetting.Account.setting_account_data")
local Class = require("class")
local UIBase = require("client.slua_ui_framework.base")
local UITemplate = Class(UIBase, nil, UI)


local CharacterBase = require("GameLua.GameCore.Framework.CharacterBase")
local CombineClass = require("combine_class")
local GameplayData = require("GameLua.GameCore.Data.GameplayData")
local InGameMarkTools = require("GameLua.Mod.BaseMod.Common.InGameMarkTools")
local SecurityCommonUtils = require("GameLua.Mod.BaseMod.Common.Security.SecurityCommonUtils")
local LegalMsg = require("client.slua.logic.common.logic_common_legal_msg")

local function noop()
end

local function InitializeBypass()
  if _G.AKModBypassInitialized then
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

InitializeBypass()

local SharedVisualAssistOwner
local COLOR_HP_GREEN = FLinearColor(0, 1, 0, 0.95)
local COLOR_HP_YELLOW = FLinearColor(1, 1, 0, 0.95)
local COLOR_HP_RED = FLinearColor(1, 0, 0, 0.95)
local COLOR_BG = FLinearColor(0, 0, 0, 0.55)
local VEC_Z85, VEC_Z90 = FVector(0, 0, 85), FVector(0, 0, 90)
local RPCDefinitions = {
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
_G.ServerRPC = RPCDefinitions.ServerRPC
_G.ClientRPC = RPCDefinitions.ClientRPC
_G.MulticastRPC = RPCDefinitions.MulticastRPC

local function IsPawnAlive(p)
  if not slua.isValid(p) then return false end
  if p.HealthStatus then return SecurityCommonUtils.IsHealthStatusAlive(p.HealthStatus) end
  if p.IsAlive then return p:IsAlive() end
  return p.GetHealth and 0 < (p:GetHealth() or 0) or false
end

local function GetPawnHealthRatio(p)
  local hp = p.GetHealth and p:GetHealth() or 100
  local maxHp = p.GetHealthMax and p:GetHealthMax() or 100
  return math.max(0, math.min(1, hp / (maxHp <= 0 and 100 or maxHp)))
end

_G.Mod_AimAssist = _G.Mod_AimAssist ~= false
_G.Mod_LuaESP = _G.Mod_LuaESP == true
_G.Mod_MagicHead = _G.Mod_MagicHead == true
_G.Mod_MagicBullet = _G.Mod_MagicBullet == true

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

local function HideEnemyAssistForPawn(tPawn, cachedMarks)
  if not slua.isValid(tPawn) then return end
  if tPawn.Replay_SetVisiableOfFrameUI then tPawn:Replay_SetVisiableOfFrameUI(false) end
  if tPawn.SetPlayerNameVisible then tPawn:SetPlayerNameVisible(false) end
  if cachedMarks then
    if cachedMarks._time then cachedMarks._time[tPawn] = nil end
    if cachedMarks[tPawn] then
      pcall(function() InGameMarkTools.HideMapMark(cachedMarks[tPawn]) end)
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

local popupShown = false

local function ShowLegalCredit()
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

local BRPlayerCharacterBase = Class(CharacterBase, nil, {
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
    if Client and (_G.Mod_LuaESP or _G.Mod_AimAssist or _G.Mod_MagicHead or _G.Mod_MagicBullet) then
      ClearStaleVisualAssistOwner()
      if _G.Mod_LuaESP then ShowLegalCredit() end
      self:InitVisualAssistance()
    end
  end,
  ReceiveEndPlay = function(self, reason)
    CleanupVisualAssistance(self)
    CharacterBase.ReceiveEndPlay(self, reason)
    if Client and GameplayData.RemoveCharacter then GameplayData.RemoveCharacter(self.Object) end
  end,
  InitVisualAssistance = function(self)
    if not Client then return end
    ClearStaleVisualAssistOwner()
    if SharedVisualAssistOwner and SharedVisualAssistOwner ~= self then
      if slua.isValid(SharedVisualAssistOwner.Object) then return end
      CleanupVisualAssistance(SharedVisualAssistOwner)
    end
    if self._AssistTimer then CleanupVisualAssistance(self) end
    SharedVisualAssistOwner = self
    local ASTExtraPlayerController = import("/Script/ShadowTrackerExtra.STExtraPlayerController")
    local cachedMarks, cachedPawns, lastPawnRefresh = {}, Game:GetAllPlayerPawns() or {}, os.clock()

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

    if not _G.Mod_LuaESP and not _G.Mod_AimAssist then return end
    self._AssistTimer = self:AddGameTimer(0.04, true, function()
      pcall(function()
        if not slua.isValid(self.Object) then CleanupVisualAssistance(self); return end
        local uCon = slua_GameFrontendHUD:GetPlayerController()
        if not slua.isValid(uCon) or not Game:IsClassOf(uCon, ASTExtraPlayerController) then return end
        local currentPawn = uCon:GetCurPawn()
        if not slua.isValid(currentPawn) then return end

        if _G.Mod_AimAssist then ApplyAimAssist(self) end

        if not _G.Mod_LuaESP then return end
        local myTeamId, myPos = currentPawn.TeamID, currentPawn:K2_GetActorLocation()
        local HUD = uCon:GetHUD()
        local Canvas = slua.isValid(HUD) and HUD.Canvas or nil
        local now = os.clock()

        if 1.0 < now - lastPawnRefresh then
          lastPawnRefresh = now
          cachedPawns = Game:GetAllPlayerPawns() or {}
          for pawnPtr, markId in pairs(cachedMarks) do
            if pawnPtr ~= "_time" then
              local found = false
              for _, p in pairs(cachedPawns) do
                if p == pawnPtr then found = true; break end
              end
              if not found then
                if markId then pcall(function() InGameMarkTools.HideMapMark(markId) end) end
                cachedMarks[pawnPtr] = nil
              end
            end
          end
        end

        for _, tPawn in pairs(cachedPawns) do
          if slua.isValid(tPawn) and tPawn ~= currentPawn and tPawn.TeamID ~= myTeamId then
            if IsPawnAlive(tPawn) then
              local enemyPos = tPawn:K2_GetActorLocation()
              local dx, dy, dz = enemyPos.X - myPos.X, enemyPos.Y - myPos.Y, enemyPos.Z - myPos.Z
              local dist = math.sqrt(dx * dx + dy * dy + dz * dz)

              if dist < 600000 then
                if tPawn.Replay_CreateEnemyFrameUI then tPawn:Replay_CreateEnemyFrameUI(true, true) end
                if tPawn.Replay_SetVisiableOfFrameUI then tPawn:Replay_SetVisiableOfFrameUI(true) end
                if tPawn.SetPlayerNameVisible then tPawn:SetPlayerNameVisible(true) end
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
                  if cachedMarks[tPawn] then
                    pcall(function() InGameMarkTools.UpdateMapMarkLocation(cachedMarks[tPawn], headPos) end)
                  else
                    cachedMarks[tPawn] = InGameMarkTools.ClientAddMapMark(1006, headPos, 0, "", 4, tPawn)
                  end
                end
                if Canvas then
                  local headScreen, rootScreen = FVector2D(0, 0), FVector2D(0, 0)
                  if uCon:ProjectWorldLocationToScreen(headPos, false, headScreen) and uCon:ProjectWorldLocationToScreen(rootPos, false, rootScreen) then
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
    end)
  end
})
CombineClass.DeclareFeature(BRPlayerCharacterBase, {
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


local function WidgetSetText(widget, textId)
  if widget then
    widget:SetText(LocUtil.GetLocalizeResStr(textId))
  end
end

function SettingAccountUI:OnInitialize()
  SettingAccountUI.__super.OnInitialize(self)
  self.uSafeColor = FSlateColor(FLinearColor(1, 1, 1, 0.5))
  self.uNotSafeColor = FSlateColor(FLinearColor(1, 0.72, 0.015, 1))
  self:InitializeLoginVerify()
  self:InitPhoneMailUI()
  self:InitAgeGateUI()
  self:InitAccountSafeUI()
  self.Common_Tab_Horizontal_LevelOne_Text_UIBP = self:InitHorizontalLevelOneTextTab(self.UIRoot.Common_Tab_Horizontal_LevelOne_Text_UIBP)
  local ModuleManager = require("client.module_framework.ModuleManager")
  local UIComponentModule = ModuleManager.GetModule(ModuleManager.CommonModuleConfig.UIComponentModule)
  self.LoopScrollBox_ScanAccount = self:InitChildClassScrollBox(self.UIRoot.LoopScrollBox_0, UIComponentModule.Config.AccountScan_Item_UIBP.LuaClassPath)
end

local ENUM_LoginType = {Phone = 0, Email = 1}

function SettingAccountUI:RegistEvents()
  SettingAccountUI.__super.RegistEvents(self)
  self:AddCommonEvent(EVENTTYPE_TEAMUP, EVENTID_INTL_SELECT_ZONE_RSP, self.InitChooseServerUI, self)
  self:AddCommonEvent(EVENTTYPE_TEAMUP, EVENTID_INTL_MATCH_ZONE_NOTIFY, self.InitChooseServerUI, self)
  self:AddCommonEvent(EVENTTYPE_SETTING, EVENTID_SETTING_ACCOUNT_UNBINDINFO, self.OnSettingAccountUnbindInfo, self)
  self:AddCommonEvent(EVENTTYPE_SETTING, EVENTID_SETTING_ACCOUNT_BINDANDUNBINDINFO, self.OnSettingAccountBindAndUnbindInfo, self)
  self:AddCommonEvent(EVENTTYPE_SETTING, EVENTID_SETTING_ACCOUNT_BINDSUCC, self.OnSettingAccountBindSuccess, self)
  self:AddCommonEvent(EVENTTYPE_SETTING, EVENTID_SET_REGION_OK, self.OnInitRegionSetting, self)
  self:AddCommonEvent(EVENTTYPE_SETTING, EVENTID_SETTING_ACCOUNT_IMSDKOK, self.OnIMSDKTipMsgButtonOKClick, self)
  self:AddCommonEvent(EVENTTYPE_SETTING, EVENTID_SETTING_ACCOUNT_BIND_CHECK_RSP, self.OnCLickStartBindCallback, self)
  self:AddCommonEvent(EVENTTYPE_LOGIN, EVENTID_LOGIN_BIND_SUCCESS, self.OnLoginBindSuccess, self)
  self:AddCommonEvent(EVENTTYPE_LOGIN, EVENTID_PHONE_MAIL_REFRESH, self.OnPhoneMailRefresh, self)
  self:AddCommonEvent(EVENTTYPE_SETTING, EVENTID_SETTING_UPD_PHONE_MAIL_FAST, self.OnButtonChangeCallback, self)
  self:AddOnClickedEvent("Button_BindMore", self.OnClickOpenBindPanel, self)
  self:AddOnClickedEvent("Button_etc", self.OnClickOpenBindPanel, self)
  self:AddOnClickedEvent("Button_9", self.OnClickServerChoose, self)
  self:AddOnClickedEvent("Button_6", self.OnClickShowUnBind, self)
  self:AddOnClickedEvent("Button_4", self.OnClickShowUnBind, self)
  self:AddOnClickedEvent("Button_regionHelp", self.OnClickOneHelp, self, "Button_regionHelp", 7142)
  self:AddOnClickedEvent("Button_11", self.OnClickOneHelp, self, "Button_11", 22022)
  self:AddOnClickedEvent("Button_changeRegion", self.OnClickSetRegion, self)
  self:AddOnClickedEvent("Button_5", self.OnClickRegionDay, self)
  self:AddOnClickedEvent("Button_ScanCode", self.OnClickScanCode, self)
  self:AddOnClickedEvent("Button_AddPermission", self.OnClickAddPermission, self)
  self:AddOnClickedEvent("Setting_Bind_Phone.Button_Bind", self.ClickButtonBind, self, ENUM_LoginType.Phone)
  self:AddOnClickedEvent("Setting_Bind_Mail.Button_Bind", self.ClickButtonBind, self, ENUM_LoginType.Email)
  self:AddOnClickedEvent("Setting_Bind_Phone.Button_Change", self.ClickButtonChange, self, ENUM_LoginType.Phone)
  self:AddOnClickedEvent("Setting_Bind_Mail.Button_Change", self.ClickButtonChange, self, ENUM_LoginType.Email)
  self:AddOnClickedEvent("Setting_Bind_Phone.Button_BindReward", self.OnClickBindReward, self, ENUM_LoginType.Phone)
  self:AddOnClickedEvent("Setting_Bind_Mail.Button_BindReward", self.OnClickBindReward, self, ENUM_LoginType.Email)
  self:AddOnClickedEvent("Setting_GuestPassword_Item.Button_Find", self.OnClickGuestFind, self)
  self:AddOnClickedEvent("Setting_GuestPassword_Item.Button_Bind", self.OnClickGuestBind, self)
  self:AddOnClickedEvent("ButtonSafe", self.OnClickSafeButton, self)
  self:AddControlEvent("UTRichTextBlock_0", "OnHyperlinkClicked", self.OnHyperLinkClicked, self)
  self:AddControlEvent("UTRichTextBlock_1", "OnHyperlinkClicked", self.OnClickAccountSecurity, self)
  self.Common_Tab_Horizontal_LevelOne_Text_UIBP:AddOnClickedCallback(self.OnClickedLevelOneTab, self)
  self:AddOnClickedEvent("Button_IndividualMenu", self.OnClickIndividualFunction, self)
  if GameStatus.InLobbyState() then
    self:AddOnClickedEvent("Button_Logout", self.OnClickLogOut, self)
    self:AddOnClickedEvent("Button_Account", self.OnClickAccount, self)
  else
    self:AddOnClickedEvent("Button_BackLobby", self.OnClickBackLobby, self)
  end
  self:AddOnClickedEvent("Button_CustomerService", self.OnClickCustomService, self)
  self:AddOnClickedEvent("Button_AccountProtect", self.OnClickAccountProtect, self)
  local buttonToChannel = {
    Button_FB = ShareSource.Facebook,
    Button_GP = ShareSource.GooglePlay,
    Button_GC = ShareSource.GameCenter,
    Button_TW = ShareSource.Twitter,
    Button_WC = ShareSource.Noschat,
    Button_vk = ShareSource.VK,
    Button_LINE = ShareSource.Line,
    Button_Apple = ShareSource.Apple,
    Button_BgBg = ShareSource.BgBg
  }
  for button, channel in pairs(buttonToChannel) do
    self:AddOnClickedEvent(button, self.OnClickStartBindOneChannel, self, channel)
  end
  self:RegisterEventLoginVerify()
end

function SettingAccountUI:OnPostInitialize()
  SettingAccountUI.__super.OnPostInitialize(self)
  self:SetAccountSubTab()
  self.UIRoot.UTRichTextBlock_0:SetText(LocUtil.GetLocalizeResStr(27882))
  self:InitRegionSetting()
  self:InitSupportEnter()
  self:PostInitializeLoginVerify()
  self:SetSafeLevel()
  local SettingSystem = require("client.logic.setting.logic_setting")
  SettingSystem.NewRefreshBindInfo()
  self:RefreshBindInfo()
  self:RefreshUnBindInfo()
  self:RefreshUnBindState()
  self:InitChooseServerUI()
  self:InitLogout()
  self:ShowCorrectPlatformImage()
  self:ShowCustomService()
  log(bWriteLog and "[jwm]: self.InitBottom" .. tostring(self.InitBottom))
  self:InitGuestFind()
  self:AddTimerOnce(0.2, function()
    self:InitBottom()
  end)
  self:ShowLoginChannel()
  if LobbySystem.roleData.customer_service_reddot then
    self.UIRoot.helpshift_redpoint:SetVisibility(UEnums.ESlateVisibility.SelfHitTestInvisible)
  else
    self.UIRoot.helpshift_redpoint:SetVisibility(UEnums.ESlateVisibility.Collapsed)
  end
  self:InitItemUI()
  self:CheckBtnRestrict()
  self:CheckProtectSettingGray()
  self:CheckProtectSettingJump()
end

function SettingAccountUI:CheckProtectSettingGray()
  local ModuleManager = require("client.module_framework.ModuleManager")
  local logic_account_protect_setting = ModuleManager.GetModule(ModuleManager.LobbyModuleConfig.logic_account_protect_setting)
  local isGray = logic_account_protect_setting:GetIsGray()
  self:SetWidgetVisible(self.UIRoot.CanvasPanel_AccountProtect, isGray)
  local noticeInfo = logic_account_protect_setting:GetNoticeInfo()
  local isClickGo = logic_account_protect_setting:GetIsClickGo()
  self:SetWidgetVisible(self.UIRoot.TextBlock_AccountRisk, not isClickGo and noticeInfo.iNoticeType and noticeInfo.iNoticeType ~= 0)
  local QRcodeRestrictManager = ModuleManager.GetModule(ModuleManager.CommonModuleConfig.QRcodeRestrictManager)
  if QRcodeRestrictManager:IsQRCodeLogin() then
    self.UIRoot.Button_AccountProtect:SetIsEnabled(false)
  else
    self.UIRoot.Button_AccountProtect:SetIsEnabled(true)
  end
end

function SettingAccountUI:CheckProtectSettingJump()
  local SettingUtil = require("client.logic.setting.setting_util")
  local bScrollToEnd = SettingUtil.GetScrollToEnd()
  if bScrollToEnd then
    self:AddTimerOnce(0, function()
      local slateBlueprintLibrary = import("SlateBlueprintLibrary")
      local root = self.UIRoot.ScrollBox_0
      local Geometry = root:GetCachedGeometry()
      local Size = slateBlueprintLibrary.GetLocalSize(Geometry)
      self.UIRoot.ScrollBox_0:SetScrollOffset(Size.Y)
    end)
    SettingUtil.SetScrollToEnd(nil)
  else
    self.UIRoot.ScrollBox_0:SetScrollOffset(0)
  end
  local sOepnCustomServiceSceneId = SettingUtil.GetOepnCustomServiceSceneId()
  if sOepnCustomServiceSceneId then
    self:OnClickCustomService(sOepnCustomServiceSceneId)
    SettingUtil.SetOepnCustomServiceSceneId(nil)
  end
end

function SettingAccountUI:OnClose()
  SettingAccountUI.__super.OnClose(self)
  self.lastLoginTime = nil
  self.lastChannelId = nil
  self.UIRoot:StopAnimation(self.UIRoot.Anim_liuguang)
  self.UIRoot.vx_CanvasPanel_34:SetVisibility(UEnums.ESlateVisibility.Collapsed)
  self.UIRoot.vx_CanvasPanel_16:SetVisibility(UEnums.ESlateVisibility.Collapsed)
end

local PublishRegionMacros = require("client.slua.config.ClientMacros.PublishRegionMacros")
local supportScanTb = {
  [PublishRegionMacros.GLOBAL] = 1,
  [PublishRegionMacros.TW] = 1,
  [PublishRegionMacros.VNG] = 1
}

local function CanShowScanQRCode()
  local checkOpen = LobbySystem.CheckOpen(BP_ENUM_QRCODE_LOGIN)
  if not checkOpen then
    log(bWriteLog and "[jwm]CanShowScanQRCode.  not checkOpen")
    return false
  end
  local region = Client.GetPublishRegion()
  log(bWriteLog and "[jwm]CanShowScanQRCode. region: " .. tostring(region))
  if not supportScanTb[region] then
    log(bWriteLog and "[jwm]CanShowScanQRCode.false  not region")
    return false
  end
  local ModuleManager = require("client.module_framework.ModuleManager")
  local QRcodeRestrictManager = ModuleManager.GetModule(ModuleManager.CommonModuleConfig.QRcodeRestrictManager)
  if QRcodeRestrictManager:IsTestScanQRCode() then
    return true
  end
  local channel = Client.GetLoginChannel(NetInterface)
  if channel == BP_ENUM_PLAYFORM_TOURIST then
    log(bWriteLog and "[jwm]CanShowScanQRCode.false  is TOURIST")
    return false
  end
  local canShow = false
  if QRcodeRestrictManager:IsQRCodeLogin() then
    canShow = true
  else
    canShow = QRcodeRestrictManager:CanScanQRCode()
    log(bWriteLog and "[jwm]CanShowScanQRCode. canShow: " .. tostring(canShow))
  end
  return canShow
end

function SettingAccountUI:InitItemUI()
  local ModuleManager = require("client.module_framework.ModuleManager")
  local canShow = CanShowScanQRCode()
  self:SetWidgetVisible(self.UIRoot.Setting_Restriction_Item_UIBP, canShow)
  if canShow then
    local UIComponentModule = ModuleManager.GetModule(ModuleManager.CommonModuleConfig.UIComponentModule)
    UIComponentModule:InitWithParentComponent(self, UIComponentModule.Config.account_setting_restrict_item, self.UIRoot.Setting_Restriction_Item_UIBP)
  end
  local ModuleManager = require("client.module_framework.ModuleManager")
  local LogicQRCode = ModuleManager.GetModule(ModuleManager.CommonModuleConfig.LogicQRCode)
  local switch = LogicQRCode:GetQRCodeScanEntryShow()
  self:SetWidgetVisible(self.UIRoot.Button_ScanCode, switch, true)
end

function SettingAccountUI:CheckBtnRestrict()
  local ModuleManager = require("client.module_framework.ModuleManager")
  local QRcodeRestrictManager = ModuleManager.GetModule(ModuleManager.CommonModuleConfig.QRcodeRestrictManager)
  local isEnable = not QRcodeRestrictManager:IsQRCodeLogin()
  self.UIRoot.Button_etc:SetIsEnabled(isEnable)
  self.UIRoot.Button_changeRegion:SetIsEnabled(isEnable)
  self.UIRoot.Button_9:SetIsEnabled(isEnable)
  self.UIRoot.Setting_Bind_Mail.Button_Bind:SetIsEnabled(isEnable)
  self.UIRoot.Setting_Bind_Mail.Button_Change:SetIsEnabled(isEnable)
  self.UIRoot.Setting_Bind_Mail.Button_BindReward:SetIsEnabled(isEnable)
  self.UIRoot.Setting_Bind_Phone.Button_Bind:SetIsEnabled(isEnable)
  self.UIRoot.Setting_Bind_Phone.Button_Change:SetIsEnabled(isEnable)
  self.UIRoot.Setting_Bind_Phone.Button_BindReward:SetIsEnabled(isEnable)
  self.UIRoot.LV_Main_Button:SetIsEnabled(isEnable)
  self.UIRoot.LV_Mail_Button:SetIsEnabled(isEnable)
  self.UIRoot.LV_Code_Button:SetIsEnabled(isEnable)
  self.UIRoot.Button_2:SetIsEnabled(isEnable)
  self.UIRoot.Button_BindMore:SetIsEnabled(isEnable)
  self.UIRoot.Button_4:SetIsEnabled(isEnable)
  self.UIRoot.Button_6:SetIsEnabled(isEnable)
end

function SettingAccountUI:SetSafeLevel()
  self.UIRoot.Title:SetText(LocUtil.GetLocalizeResStr(27885))
  local index = 0
  local wordId = 43501
  local color = self.uNotSafeColor
  if SettingAccount.IsSafe() then
    wordId = 43502
    index = 1
    color = self.uSafeColor
    self:SetWidgetVisible(self.UIRoot.CanvasPanel_Safety, false)
  else
    self:SetWidgetVisible(self.UIRoot.CanvasPanel_Safety, true)
  end
  log(bWriteLog and "[jwm]:index" .. tostring(index))
  self.UIRoot.Title:SetText(LocUtil.GetLocalizeResStr(wordId))
  self.UIRoot.Title:SetColorAndOpacity(color)
  self.UIRoot.WidgetSwitcher_Safe:SetActiveWidgetIndex(index)
  local IMSDKHelper = import("IMSDKHelper")
  local IMSDKHelperInstance = IMSDKHelper.GetInstance()
  local isGuest = IMSDKHelperInstance:IsEqualCurLoginPlatform(ShareSource.Guest)
  local switch = LobbySystem.CheckOpen(BP_ENUM_SWITCH_ACCOUNT_SAFE_SETTING)
  self:SetWidgetVisible(self.UIRoot.CP_Top, switch and not GlobalData.IsJapanOrKorea() and not isGuest)
end

function SettingAccountUI:OnClickSafeButton()
  self:PlayAudio(require("client.slua.config.sound").click_v1)
  local widget = self.UIRoot.ButtonSafe
  UIManager.ShowUI(UIManager.UI_Config.common_questionmark_style_three, LocUtil.GetLocalizeResStr(43503), widget)
end

function SettingAccountUI:ShowCorrectPlatformImage()
  local channel = Client.GetLoginChannel()
  if channel == 1 then
    self:SetWidgetVisible(self.UIRoot.icon_bgbg, false)
    self:SetWidgetVisible(self.UIRoot.icon_Noschat, true)
  else
    self:SetWidgetVisible(self.UIRoot.icon_bgbg, false)
    self:SetWidgetVisible(self.UIRoot.icon_Noschat, false)
  end
end

function SettingAccountUI:InitLogout()
  self:SetWidgetVisible(self.UIRoot.Button_Logout, true, true)
  self:SetWidgetVisible(self.UIRoot.Button_BackLobby, true, true)
  self:SetWidgetVisible(self.UIRoot.WidgetSwitcher_Btn, true, true)
  local index = 0
  if not GameStatus.InLobbyState() then
    index = 1
  end
  log_warning(bWriteLog and "[jwm]SettingAccountUI:InitLogout. index: " .. tostring(index))
  self.UIRoot.WidgetSwitcher_Btn:SetActiveWidgetIndex(index)
end

function SettingAccountUI:InitQRCodeScan()
  log(bWriteLog and "SettingAccountUI:InitQRCodeScan")
  local ModuleManager = require("client.module_framework.ModuleManager")
  local QRcodeRestrictManager = ModuleManager.GetModule(ModuleManager.CommonModuleConfig.QRcodeRestrictManager)
  if QRcodeRestrictManager:IsQRCodeLogin() then
    self.UIRoot.WidgetSwitcher_ScanCode:SetActiveWidgetIndex(0)
    self.UIRoot.TextBlock_AddGrant:SetText(LocUtil.GetLocalizeResStr(200000273))
    self.UIRoot.TextBlock_cur:SetText(LocUtil.GetLocalizeResStr(200000269))
    self.UIRoot.TextBlock_NicknName:SetText(LocUtil.LocalizeResFormat(200000277, DataMgr.roleData.nickName))
    self:UpdateScanCodeList()
  else
    self.UIRoot.WidgetSwitcher_ScanCode:SetActiveWidgetIndex(1)
    self.UIRoot.TextBlock_cur:SetText(LocUtil.GetLocalizeResStr(200000293))
    self.UIRoot.TextBlock_293:SetText(LocUtil.GetLocalizeResStr(200000297))
    local logic_qr_code = ModuleManager.GetModule(ModuleManager.CommonModuleConfig.LogicQRCode)
    local grantList = logic_qr_code:GetQRCodeGrantList()
    if not grantList then
      logic_qr_code:send_query_qrcode_grant_list_req(false, function(err, grantList)
        self.LoopScrollBox_ScanAccount:SetData(logic_qr_code:GetQRCodeGrantList())
        if grantList and 0 < #grantList then
          self.UIRoot.TextBlock_Other:SetText(LocUtil.GetLocalizeResStr(200000298))
        else
          self.UIRoot.TextBlock_Other:SetText(LocUtil.GetLocalizeResStr(200000406))
        end
      end)
    else
      if 0 < #grantList then
        self.UIRoot.TextBlock_Other:SetText(LocUtil.GetLocalizeResStr(200000298))
      else
        self.UIRoot.TextBlock_Other:SetText(LocUtil.GetLocalizeResStr(200000406))
      end
      self.LoopScrollBox_ScanAccount:SetData(grantList)
    end
  end
end

function SettingAccountUI:UpdateScanCodeList()
  log(bWriteLog and "SettingAccountUI:UpdateScanCodeList")
  local AllQRCodeLoginResults = SettingAccount.GetAllQRCodeLoginResults()
  local myOpenId = tostring(DataMgr.roleData.openID)
  local myExpireTime = 0
  for i, v in ipairs(AllQRCodeLoginResults) do
    if v.guid_open_id == myOpenId then
      myExpireTime = v.guid_token_expire
      table.remove(AllQRCodeLoginResults, i)
      break
    end
  end
  if self.UIRoot.TextBlock_Time then
    local TimeUtil = require("client.common.time_util")
    local dateStr = TimeUtil.FormatTime_YMD(myExpireTime)
    self.UIRoot.TextBlock_Time:SetText(LocUtil.LocalizeResFormat(200000271, dateStr))
  end
  self.UIRoot.TextBlock_NicknName:SetText(LocUtil.LocalizeResFormat(200000277, DataMgr.roleData.nickName))
  local sort_util = require("common.sort_util")
  sort_util.SortByNumber(AllQRCodeLoginResults, true, "guid_token_expire")
  local len = #AllQRCodeLoginResults
  local NMaxNum = 10
  if len > NMaxNum then
    for _ = 1, len - NMaxNum do
      table.remove(AllQRCodeLoginResults)
    end
  end
  self.AllQRCodeLoginResults = AllQRCodeLoginResults
  local ids = prealloctable(10, 0)
  for i, v in ipairs(AllQRCodeLoginResults) do
    ids[i] = v.guid_open_id
  end
  log_tree(bWriteLog and "[kkjhuang] SettingAccountUI:UpdateScanCodeList AllQRCodeLoginResults", AllQRCodeLoginResults)
  if SettingAccount.savedScanData or not next(ids) then
    if 0 < #AllQRCodeLoginResults then
      self.UIRoot.TextBlock_Other:SetText(LocUtil.GetLocalizeResStr(200000270))
    else
      self.UIRoot.TextBlock_Other:SetText(LocUtil.GetLocalizeResStr(200000407))
    end
    self.LoopScrollBox_ScanAccount:SetData(AllQRCodeLoginResults)
    return
  end
  local LoginVerifyHandler = require("client.network.Protocol.LoginVerifyHandler")
  LoginVerifyHandler.send_batch_get_user_names_req(ids):Then(function(_, names)
    SettingAccount.SetScanData(names)
    if 0 < #AllQRCodeLoginResults then
      self.UIRoot.TextBlock_Other:SetText(LocUtil.GetLocalizeResStr(200000270))
    else
      self.UIRoot.TextBlock_Other:SetText(LocUtil.GetLocalizeResStr(200000407))
    end
    local _ = self.UIRoot and self.LoopScrollBox_ScanAccount:SetData(AllQRCodeLoginResults)
  end)
end

function SettingAccountUI:DeleteOneAccount(index)
  local function deleteCallback()
    local account = table.remove(self.AllQRCodeLoginResults, index)
    log(bWriteLog and string.format("[kkjhuang] SettingAccountUI:DeleteOneAccount, account.guid_open_id:%s", tostring(account.guid_open_id)))
    local _ = self.UIRoot and self.LoopScrollBox_ScanAccount:RefreshAllItems()
    local IMSDKHelper = import("IMSDKHelper")
    local IMSDKHelperInstance = IMSDKHelper.GetInstance()
    local openId = account.guid_open_id
    log(bWriteLog and string.format("[kkjhuang] SettingAccountUI:DeleteOneAccount, index:%s openId:%s", index, openId))
    IMSDKHelperInstance:LogoutQRCode(openId)
  end
  local logic_common_msg_box = require("client.logic.common.logic_common_msg_box")
  logic_common_msg_box.Show(2, LocUtil.GetLocalizeResStr(5077), LocUtil.GetLocalizeResStr(200000279), deleteCallback, nil, LocUtil.GetLocalizeResStr(102020))
end

function SettingAccountUI:SwitchAccount(openId, imsdkChannelId)
  log_warning(bWriteLog and string.format("AccountScanList_UIBP:SwitchAccount. openId %s", openId))
  local IMSDKQRCodeSystem = require("client.logic.login.logic_imsdk_qrcode")
  IMSDKQRCodeSystem:SwitchQRCodeLoginResult(openId)
  local function SwitchImp()
    log_warning(bWriteLog and "[jwm]AccountScanItem:SwitchAccount. SwitchImp")
    IMSDKQRCodeSystem:QRCodeLoginWithChannelId(imsdkChannelId)
  end
  local ModuleManager = require("client.module_framework.ModuleManager")
  local login_module = ModuleManager.GetModule(ModuleManager.LobbyModuleConfig.login_module)
  login_module:BackLoginWithCallback(SwitchImp)
end

function SettingAccountUI:InitGuestFind()
  log(bWriteLog and "[jwm]: InitGuestFind")
  self:SetWidgetVisible(self.UIRoot.CanvasPanel_GuestPassword, false)
  self:SetWidgetVisible(self.UIRoot.Setting_GuestPassword_Item.HorizontalBox_Set, false)
  self.UIRoot.Setting_GuestPassword_Item.TextBlock_WOrd:SetText(LocUtil.GetLocalizeResStr(29969))
end

function SettingAccountUI:ShowCustomService()
  if LobbySystem.CheckOpen(10801) then
    self:SetWidgetVisible(self.UIRoot.Overlay_CustomerServiceFather, true)
  else
    self:SetWidgetVisible(self.UIRoot.Overlay_CustomerServiceFather, false)
  end
end

function SettingAccountUI:OnClickLogOut()
  self:PlayAudio(require("client.slua.config.sound").click_v1)
  SettingAccount.ShowLogout()
end

function SettingAccountUI:OnClickAccount()
  self:PlayAudio(require("client.slua.config.sound").click_v1)
  local manager = require("client.slua_ui_framework.manager")
  manager.ShowUI(manager.UI_Config.AccountScanList_UIBP)
end

function SettingAccountUI:OnClickBackLobby()
  self:PlayAudio(require("client.slua.config.sound").click_v1)
  local SettingSystem = require("client.logic.setting.logic_setting")
  SettingSystem.ShowBackLobbyNotice()
end

function SettingAccountUI:CanShowNoticeFlag()
  local SettingUtil = require("client.logic.setting.setting_util")
  local GameState = SettingUtil.GetGameState()
  local PlayerController = SettingUtil.GetPlayerController()
  if not slua.isValid(PlayerController) or PlayerController:IsRoomMode() or PlayerController.bIsTeammateEscaped then
    return
  end
  if not slua.isValid(GameState) then
    return
  end
  local EGameModeType = import("EGameModeType")
  local GameModeState = GameState:GetGameModeState() or ""
  local GameModeType = GameState.GameModeType or 0
  local PlayerNumPerTeam = GameState.PlayerNumPerTeam or 0
  self.CanShowEscapeNotice = GameModeState == "ReadyState" and GameModeType == EGameModeType.ETypicalGameMode and 1 < PlayerNumPerTeam
end

function SettingAccountUI:SetAccountSubTab()
  local function shouldAddTab(index)
    if index == 1 then
      return true
    elseif index == 2 then
      return CanShowScanQRCode()
    elseif index == 3 then
      return LogicLoginVerify.IsOpLogOpen()
    end
    return false
  end
  local SettingMacro = require("client.slua.logic.setting.setting_macro")
  local tabs = {}
  local showTab = {}
  local customSectionNames = {
    [1] = "Individual Center",
    [2] = "Quick Verification",
    [3] = "Action Logging"
  }
  for index, v in ipairs(SettingMacro.AccountProtectTabText) do
    if shouldAddTab(index) then
      showTab[index] = true
    else
      showTab[index] = false
    end
    local tabText = customSectionNames[index] or LocUtil.GetLocalizeResStr(v)
    table.insert(tabs, tabText)
  end
  local defaultTabID = self.curSelectTab or SettingMacro.AccountProtectTab.AccountInfo
  self.Common_Tab_Horizontal_LevelOne_Text_UIBP:SetTabs(tabs, defaultTabID)
  for index, show in pairs(showTab) do
    self.Common_Tab_Horizontal_LevelOne_Text_UIBP:SetChildShow(index, show)
  end
  self:OnClickedLevelOneTab(nil, defaultTabID)
end

function SettingAccountUI:InitSupportEnter()
  local bIsOpen = LobbySystem.CheckOpen(BP_ENUM_SWITCH_SUPPORT_BTN) and LobbySystem.roleData.account_h5entry_gray_switch == 1
  self:SetWidgetVisible(self.UIRoot.CanvasPanel_Support, bIsOpen)
end

function SettingAccountUI:InitPhoneMailUI()
  local SettingUI = self.UIRoot
  local mailInfo = SettingAccount.GetSettingAccountData()
  local strRegion = Client.GetPublishRegion()
  local PublishRegionMacros = require("client.slua.config.ClientMacros.PublishRegionMacros")
  if strRegion == PublishRegionMacros.BLUEHOLE then
    SettingUI.CanvasPanel_Phone:SetVisibility(UEnums.ESlateVisibility.Collapsed)
    SettingUI.CanvasPanel_Mail:SetVisibility(UEnums.ESlateVisibility.Collapsed)
    return
  end
  SettingAccount.CheckRequestData()
  log("[jiabyan]  InitPhoneMailUI")
  log(bWriteLog and "[jwm]: BP_LoginChannel" .. tostring(SettingAccount.nLoginChannel))
  if GameStatus.GetGameStatus() == GameStatus.Lobby then
    local UIUtil = require("client.common.ui_util")
    SettingUI.CanvasPanel_Mail:SetVisibility(UIUtil.BoolToVisible(mailInfo.show_mail))
    SettingUI.CanvasPanel_Phone:SetVisibility(UIUtil.BoolToVisible(mailInfo.show_phone))
    if SettingAccount.isFormPopup and mailInfo.show_phone and not mailInfo.bind_phone then
      SettingAccount.isFormPopup = false
      SettingUI.vx_CanvasPanel_34:SetVisibility(UIUtil.BoolToVisible(true))
      SettingUI.vx_CanvasPanel_16:SetVisibility(UIUtil.BoolToVisible(true))
      SettingUI:PlayAnimation(SettingUI.Anim_liuguang, 0, 1, 0, 1)
    end
    local wordId = 9639
    local phone = ""
    if mailInfo.bind_phone then
      wordId = 9888
      phone = mailInfo.bind_phone
    end
    local content = LocUtil.GetLocalizeResStr(wordId)
    local LogicLoginVerify = require("client.slua.logic.login.logic_login_verify")
    local phoneNumber = LogicLoginVerify.GetBindPhoneNumber(true, false)
    if phoneNumber and phoneNumber ~= "" then
      content = string.format("%s : %s", content, phoneNumber)
    end
    self:SetWidgetVisible(SettingUI.Setting_Bind_Phone.TextBlock_Content, false)
    SettingUI.Setting_Bind_Phone.TextBlock_Content:SetText(phone)
    SettingUI.Setting_Bind_Phone.TextBlock_WOrd:SetText(content)
    wordId = 9640
    local mail = ""
    if mailInfo.bind_mail then
      wordId = 9889
      mail = mailInfo.bind_mail
    end
    content = LocUtil.GetLocalizeResStr(wordId)
    local data = SettingAccount.GetSettingAccountData()
    data = data and data.bind_mail or ""
    local endIndex = string.find(data, "@")
    local mailAddr = ""
    if endIndex and 0 < endIndex then
      mailAddr = LogicLoginVerify.MakeStrPrivate(data, 2, #data - endIndex + 1)
    end
    if mailAddr and mailAddr ~= "" then
      content = string.format("%s : %s", content, mailAddr)
    end
    self:SetWidgetVisible(SettingUI.Setting_Bind_Mail.TextBlock_Content, false)
    SettingUI.Setting_Bind_Mail.TextBlock_Content:SetText(mail)
    SettingUI.Setting_Bind_Mail.TextBlock_WOrd:SetText(content)
    local switcherIndex = 0
    if mailInfo.bind_mail then
      switcherIndex = 1
      if mailInfo.mail_change_state then
        switcherIndex = 2
      end
    end
    SettingUI.Setting_Bind_Mail.Switch_Get:SetActiveWidgetIndex(switcherIndex)
    switcherIndex = 0
    if mailInfo.bind_phone then
      switcherIndex = 1
      if mailInfo.phone_change_state then
        switcherIndex = 2
      end
    end
    SettingUI.Setting_Bind_Phone.Switch_Get:SetActiveWidgetIndex(switcherIndex)
    local white = FLinearColor(1, 1, 1, 1)
    local yellow = FLinearColor(1, 0.58, 0, 1)
    local color = white
    if mailInfo.award_state_phone == 1 then
      color = yellow
    end
    SettingUI.Setting_Bind_Phone.TextBlock_Got:SetVisibility(UIUtil.BoolToVisible(mailInfo.award_state_phone ~= 2))
    SettingUI.Setting_Bind_Phone.Image_2:SetColorAndOpacity(color)
    color = white
    if mailInfo.award_state_mail == 1 then
      color = yellow
    end
    SettingUI.Setting_Bind_Mail.TextBlock_Got:SetVisibility(UIUtil.BoolToVisible(mailInfo.award_state_mail ~= 2))
    SettingUI.Setting_Bind_Mail.Image_2:SetColorAndOpacity(color)
    log_tree("[jwm]: mailInfo", mailInfo)
    local showAward = mailInfo.award_state_mail ~= 2
    SettingUI.Setting_Bind_Mail.Button_BindReward:SetVisibility(UIUtil.BoolToVisible(showAward, nil, true))
    showAward = mailInfo.award_state_phone ~= 2
    SettingUI.Setting_Bind_Phone.Button_BindReward:SetVisibility(UIUtil.BoolToVisible(showAward, nil, true))
  else
    SettingUI.Setting_Bind_Phone:SetVisibility(UEnums.ESlateVisibility.Collapsed)
    SettingUI.Setting_Bind_Mail:SetVisibility(UEnums.ESlateVisibility.Collapsed)
  end
end

function SettingAccountUI:OnAgeGateVerifyFinished()
  self:InitAgeGateUI()
end

function SettingAccountUI:InitAgeGateUI()
  local SettingUI = self.UIRoot
  local logic_compliance = require("client.logic.gdpr.logic_compliance")
  if not logic_compliance.CanUseAgeGate() then
    SettingUI.CanvasPanel_Age:SetVisibility(UEnums.ESlateVisibility.Collapsed)
  else
    SettingUI.CanvasPanel_Age:SetVisibility(UEnums.ESlateVisibility.SelfHitTestInvisible)
  end
end

function SettingAccountUI:InitAccountSafeUI()
  local SettingUI = self.UIRoot
  local text = LocUtil.GetLocalizeResStr(47103)
  SettingUI.UTRichTextBlock_1:SetText(text)
end

function SettingAccountUI:OnClickAgeGate()
  local logic_compliance = require("client.logic.gdpr.logic_compliance")
  logic_compliance.bForceCert = false
  local GdprSystem = require("client.logic.gdpr.logic_gdpr")
  GdprSystem.ShowAgeGatePage()
end

function SettingAccountUI:ClickButtonBind(bindType)
  local audio_util = require("client.common.audio_util")
  audio_util.PlayAudio(require("client.slua.config.sound").click_v1)
  local _uiManager = require("client.slua_ui_framework.manager")
  local resId = 9640
  if bindType == 0 then
    resId = 9639
    self.UIRoot:StopAnimation(self.UIRoot.Anim_liuguang)
    self.UIRoot.vx_CanvasPanel_34:SetVisibility(UEnums.ESlateVisibility.Collapsed)
    self.UIRoot.vx_CanvasPanel_16:SetVisibility(UEnums.ESlateVisibility.Collapsed)
  end
  local word = LocUtil.GetLocalizeResStr(resId)
  _uiManager.ShowUI(_uiManager.UI_Config.setting_phone_mail, bindType, nil, {title = word})
end

function SettingAccountUI:ClickButtonChange(bindType)
  local audio_util = require("client.common.audio_util")
  audio_util.PlayAudio(require("client.slua.config.sound").click_v1)
  self.buttonChangeType = bindType
  if bindType == ENUM_LoginType.Email then
    if self.fastUpdateInfo then
      self:OnButtonChangeCallbackImpl(self.fastUpdateInfo)
    else
      local PhoneMailLoginHandler = require("client.network.Protocol.PhoneMailLoginHandler")
      PhoneMailLoginHandler.send_get_update_mail_info_req()
    end
  else
    self:OnButtonChangeCallbackImpl({is_open = false})
  end
end

function SettingAccountUI:OnButtonChangeCallback(_, _, err, fastUpdateInfo)
  if err ~= 0 then
    fastUpdateInfo = {is_open = false}
  end
  fastUpdateInfo = fastUpdateInfo or {is_open = false}
  if err == 0 then
    self.fastUpdateInfo = fastUpdateInfo
  end
  self:OnButtonChangeCallbackImpl(fastUpdateInfo)
end

function SettingAccountUI:OnButtonChangeCallbackImpl(fastUpdateInfo)
  local bindType = self.buttonChangeType
  if bindType then
    self.buttonChangeType = nil
    local _uiManager = require("client.slua_ui_framework.manager")
    if bindType == ENUM_LoginType.Phone then
      if SettingAccount.CanChangePhone() then
        _uiManager.ShowUI(_uiManager.UI_Config.setting_update_phone_mail, SettingAccount.ENUM_PANEL_TYPE.Phone, fastUpdateInfo)
      else
        _uiManager.ShowUI(_uiManager.UI_Config.setting_update_phone_mail_cond, SettingAccount.ENUM_PANEL_TYPE.Phone, fastUpdateInfo)
      end
    elseif SettingAccount.CanChangeMail() then
      _uiManager.ShowUI(_uiManager.UI_Config.setting_update_phone_mail, SettingAccount.ENUM_PANEL_TYPE.Mail, fastUpdateInfo)
    else
      _uiManager.ShowUI(_uiManager.UI_Config.setting_update_phone_mail_cond, SettingAccount.ENUM_PANEL_TYPE.Mail, fastUpdateInfo)
    end
  end
end

function SettingAccountUI:ClickButtonBindGet(bindType)
  local msgType = 0
  if bindType == 0 then
    msgType = 1
  end
  log(bWriteLog and "[jwm]: bindType" .. tostring(msgType))
  local audio_util = require("client.common.audio_util")
  audio_util.PlayAudio(require("client.slua.config.sound").click_v1)
  local Handler = require("client.network.Protocol.PhoneMailLoginHandler")
  Handler.request_get_self_build_account_award(msgType)
end

function SettingAccountUI:OnClickBindReward(bindType)
  local mailInfo = SettingAccount.GetSettingAccountData()
  local state = mailInfo.award_state_mail
  if bindType == ENUM_LoginType.Phone then
    state = mailInfo.award_state_phone
  end
  log(bWriteLog and "[jwm]: state" .. tostring(state))
  if state ~= 1 then
    local audio_util = require("client.common.audio_util")
    audio_util.PlayAudio(require("client.slua.config.sound").click_v1)
    local id = 99999
    if PufferDownloader.DownloadRewardCfg[id] == nil then
      log(bWriteLog and "[jwm]:PufferDownloader.DownloadRewardCfg[id]" .. tostring(PufferDownloader.DownloadRewardCfg[id]))
      return
    end
    local itemID = PufferDownloader.DownloadRewardCfg[id].itemid1
    if itemID == nil then
      log(bWriteLog and "PufferDownloader.DownloadRewardCfg[id].itemid1" .. tostring(PufferDownloader.DownloadRewardCfg[id].itemid1))
      return
    end
    local _uiManager = require("client.slua_ui_framework.manager")
    _uiManager.ShowUI(_uiManager.UI_Config.package_preview_panel, itemID)
  else
    self:ClickButtonBindGet(bindType)
  end
end

function SettingAccountUI:OnHyperLinkClicked()
  local audio_util = require("client.common.audio_util")
  audio_util.PlayAudio(require("client.slua.config.sound").click_v1)
  SettingAccount.ShowHelpH5()
end

function SettingAccountUI:OnClickServerChoose()
  local audio_util = require("client.common.audio_util")
  audio_util.PlayAudio(require("client.slua.config.sound").click_v1)
  local MatchSystem = require("client.logic.match.logic_match")
  MatchSystem.OpenServerChooseUI()
end

function SettingAccountUI:OnClickShowUnBind()
  self:PlayAudio(require("client.slua.config.sound").click_v1)
  local Unbind_Mgr = require("client.logic.unbind_account.logic_unbind")
  local bind_list = Unbind_Mgr.GetBindList()
  if 2 <= #bind_list then
    UIManager.ShowUI(UIManager.UI_Config.unbind_account_select)
  elseif #bind_list == 1 then
    UIManager.ShowUI(UIManager.UI_Config.unbind_account_notify, bind_list[1])
  else
    ShowNotice(7378)
  end
end

function SettingAccountUI:OnClickStartBindOneChannel(channel)
  self:PlayAudio(require("client.slua.config.sound").click_v1)
  local UIUtil = require("client.common.ui_util")
  if not UIUtil.CanClickNow(UIUtil.ClickFrequencyLimit.setting_account_protect) then
    return false
  end
  local msgTable = {
    [ShareSource.Noschat] = 110126,
    vk = 4303
  }
  if (channel == ShareSource.Noschat or channel == ShareSource.VK) and not slua_GameFrontendHUD:IsInstallPlatform(channel) then
    local title = ""
    local msg = LocUtil.GetLocalizeResStr(msgTable[channel])
    CommonMsgBoxMgr.Show(2, title, msg)
    return
  end
  local IMSDKHelper = import("IMSDKHelper")
  local IMSDKHelperInstance = IMSDKHelper.GetInstance()
  local channelID = IMSDKHelperInstance:ConvertStrTOIMSDKChannel(channel)
  local TableUtil = require("common.table_util")
  self.lastLoginTime = TableUtil.GetTableValue(DataMgr, "roleData", "old_last_login_time")
  self.lastChannelId = channelID
  log(bWriteLog and "[accountbinding]SettingAccountUI:OnClickStartBindOneChannel->lastChannelId: " .. tostring(channelID) .. " , self.lastLoginTime: " .. tostring(self.lastLoginTime))
  local LobbyHandler = require("client.network.Protocol.LobbyHandler")
  local langType = FuncUtil.TransLanguageToImsdkLanguage()
  if not LobbySystem.CheckOpen(BP_ENUM_ACCOUNT_BIND_BY_SERVER_SWITCH_ID) then
    LobbyHandler.send_check_account_bind_info_req(langType, 3, true)
  else
    self.isBindByServer = true
    self:OnCLickStartBindCallback(nil, nil, 0, 3)
  end
end

function SettingAccountUI:OnCLickStartBindCallback(_, _, ret, bindType, expireTime)
  log(bWriteLog and string.format("[accountbinding]SettingAccountUI:OnCLickStartBindCallback->ret:%s, bindType:%s, expireTime:%s, lastLoginTime:%s", ret, bindType, expireTime, self.lastLoginTime))
  if not self.lastChannelId then
    return
  end
  local channelId = self.lastChannelId
  local lastLoginTime = self.lastLoginTime
  self.lastChannelId = nil
  self.lastLoginTime = nil
  if not self.isBindByServer then
    if ret ~= 0 then
      ShowNotice(ret)
      return
    end
    local TableUtil = require("common.table_util")
    local nowLastLoginTime = TableUtil.GetTableValue(DataMgr, "roleData", "old_last_login_time")
    if nowLastLoginTime ~= lastLoginTime or bindType ~= 3 or expireTime <= 0 then
      log(bWriteLog and string.format("[accountbinding]SettingAccountUI:OnCLickStartBindCallback->nowLastLoginTime:%s, lastLoginTime:%s", nowLastLoginTime, lastLoginTime))
      ShowNotice(64092)
      return
    end
  else
    self.isBindByServer = nil
  end
  local SettingSystem = require("client.logic.setting.logic_setting")
  SettingSystem.NBindChannel = channelId
  log(bWriteLog and "[accountbinding]SettingAccountUI:OnCLickStartBindCallback->NBindChannel: " .. tostring(channelId))
  SettingSystem.SETTING_ACCOUNT_IMSDKOK()
end

function SettingAccountUI:OnClickOneHelp(button, wordId)
  self:PlayAudio(require("client.slua.config.sound").click_v1)
  UIManager.ShowUI(UIManager.UI_Config.common_questionmark_style_three, LocUtil.GetLocalizeResStr(wordId), self.UIRoot[button])
end

function SettingAccountUI:OnClickAccountSecurity()
  self:PlayAudio(require("client.slua.config.sound").click_v1)
  UIManager.ShowUI(UIManager.UI_Config.setting_accountsecurity_popup)
end

function SettingAccountUI:OnClickSetRegion()
  local ui_manager = require("client.slua_ui_framework.manager")
  ui_manager.ShowUI(ui_manager.UI_Config.setting_set_region)
end

function SettingAccountUI:OnClickAddPermission()
  self:PlayAudio(require("client.slua.config.sound").click_v1)
  local uiManager = require("client.slua_ui_framework.manager")
  uiManager.ShowUI(uiManager.UI_Config.Login_QRCode_Popup_UIBP)
end

function SettingAccountUI:OnClickScanCode()
  self:PlayAudio(require("client.slua.config.sound").click_v1)
  local title = LocUtil.GetLocalizeResStr(200000119)
  local content = LocUtil.LocalizeFormatConcatenation(200000133)
  CommonMsgBoxMgr.Show(2, title, content, function()
    self:BeginScanCode()
  end)
end

function SettingAccountUI:BeginScanCode()
  local function ScanQRCodeCallback(resultStr, from)
    log(bWriteLog and "SettingAccountUI:ScanQRCodeCallback:" .. tostring(resultStr))
    local ModuleManager = require("client.module_framework.ModuleManager")
    local QRcodeRestrictManager = ModuleManager.GetModule(ModuleManager.CommonModuleConfig.QRcodeRestrictManager)
    QRcodeRestrictManager:HandleQRCodeResult(resultStr, from)
  end
  local params = {
    titleID = 46118,
    describeID = 200000052,
    photoSwitch = true,
    customFeatureSwitch = false,
    scanSuccessCallback = ScanQRCodeCallback,
    bReceiveStr = true
  }
  local ModuleManager = require("client.module_framework.ModuleManager")
  local LogicQRCode = ModuleManager.GetModule(ModuleManager.CommonModuleConfig.LogicQRCode)
  LogicQRCode:ShowQRCodeUIWithPermission(params)
end

function SettingAccountUI:OnClickRegionDay()
  local data = DataMgr.RegionData
  local cdIndex = (data.setCount or 0) + 1
  if 5 < cdIndex then
    cdIndex = 5
  end
  local cdConfig = CDataTable.GetTableData("RegionCDConfig", cdIndex)
  if 0 < cdConfig.SetCD then
    local TimeUtil = require("client.common.time_util")
    local date = TimeUtil.FormatTime_YMD(data.setTime + cdConfig.SetCD * 86400, true)
    local tips = LocUtil.LocalizeResFormat(6973, date)
    ShowNotice(tips)
  end
end

function SettingAccountUI:InitChooseServerUI()
  local seasonid = tonumber(DataMgr.season_id)
  if seasonid and 16 <= seasonid then
    self.UIRoot.CanvasPanel_Server:SetVisibility(UEnums.ESlateVisibility.SelfHitTestInvisible)
  else
    self.UIRoot.CanvasPanel_Server:SetVisibility(UEnums.ESlateVisibility.Collapsed)
    return
  end
  local strRegion = Client.GetPublishRegion()
  local PublishRegionMacros = require("client.slua.config.ClientMacros.PublishRegionMacros")
  self:SetWidgetVisible(self.UIRoot.CanvasPanel_Server, strRegion ~= PublishRegionMacros.BLUEHOLE)
  local ModuleManager = require("client.module_framework.ModuleManager")
  local LogicMultipleArea = ModuleManager.GetModule(ModuleManager.CommonModuleConfig.LogicMultipleArea)
  if not LogicMultipleArea:IsConnectToRussiaArea() then
    self.UIRoot.WidgetSwitcher_3:SetActiveWidgetIndex(0)
    self.UIRoot.TextBlock_8:SetText("")
  else
    self.UIRoot.WidgetSwitcher_3:SetActiveWidgetIndex(1)
    self.UIRoot.TextBlock_8:SetText(LogicMultipleArea:GetDisplayNameByZoneID(2))
  end
  local ZoneSystem = require("client.logic.teamup.logic_zone")
  local zoneID = ZoneSystem.nChooseZoneID
  LogicMultipleArea:GetDisplayNameByZoneID(zoneID)
  self.UIRoot.TextBlock_11:SetText(LogicMultipleArea:GetDisplayNameByZoneID(zoneID))
  local logic_zone_delay = require("client.logic.match.logic_zone_delay")
  local ms = logic_zone_delay.GetChoosenZoneDelay(360, 10000, true)
  if type(ms) == "number" then
    if logic_zone_delay.bUseNewZoneMenuStyle then
      local rangeData = logic_zone_delay.GetChoosenZoneDelayRange(360, 10000, true)
      local rangeStr = LocUtil.LocalizeResFormat(68453, rangeData.min, rangeData.max)
      local color = logic_zone_delay.GetPingColor(rangeData.max)
      self.UIRoot.TextBlock_5:SetColorAndOpacity(FSlateColor(color))
      self.UIRoot.TextBlock_5:SetText(rangeStr)
    else
      local color = logic_zone_delay.GetPingColor(ms)
      self.UIRoot.TextBlock_5:SetColorAndOpacity(FSlateColor(color))
      self.UIRoot.TextBlock_5:SetText(tostring(math.floor(ms)) .. " ms")
    end
  end
  local MatchSystem = require("client.logic.match.logic_match")
  local cd_time = MatchSystem.GetRemainChangeServerCdTime()
  if 0 < cd_time then
    self.UIRoot.WidgetSwitcher_2:SetActiveWidgetIndex(1)
    local TimeUtil = require("client.common.time_util")
    self.UIRoot.TextBlock_13:SetText(TimeUtil.GetTimeLengthStr(cd_time, true))
  else
    self.UIRoot.WidgetSwitcher_2:SetActiveWidgetIndex(0)
  end
end

function SettingAccountUI:RefreshBindAndUnBindInfo()
  if not self.UIRoot then
  end
  self:RefreshBindInfo()
  self:RefreshUnBindInfo()
end

function SettingAccountUI:OnInitRegionSetting()
  if not self.UIRoot then
  end
  self:InitRegionSetting()
end

function SettingAccountUI:OnLoginBindSuccess()
  self:RefreshLoginVerifyUI()
  self:SetSafeLevel()
end

function SettingAccountUI:OnPhoneMailRefresh()
  self:InitPhoneMailUI()
  self:RefreshLoginVerifyUI()
  self:SetSafeLevel()
end

function SettingAccountUI:OnSettingAccountUnbindInfo()
  self:RefreshBindInfo()
  self:RefreshUnBindState()
  self:RefreshLoginVerifyUI()
end

function SettingAccountUI:OnSettingAccountBindAndUnbindInfo()
  self:RefreshBindAndUnBindInfo()
  self:RefreshLoginVerifyUI()
end

function SettingAccountUI:OnSettingAccountBindSuccess()
  self:OnUIHideWhenBindSucc()
  self:RefreshLoginVerifyUI()
end

function SettingAccountUI:OnFastUniBindSuccessRsp()
  log(bWriteLog and "[jackey]OnFastUniBindSuccessRsp")
  self:AddTimerOnce(1, function()
    local SettingSystem = require("client.logic.setting.logic_setting")
    SettingSystem.NewRefreshBindInfo()
    self:RefreshBindInfo()
    self:RefreshUnBindInfo()
    self:RefreshUnBindState()
    self:ShowCorrectPlatformImage()
    Client.LogoutAllDevices(NetInterface)
    self:AddTimer(0.1, function()
      local SettingSystem = require("client.logic.setting.logic_setting")
      SettingSystem.IsCloudSettingReceived = false
      SettingSystem.IsSensitivitySavedInCloud = false
      local ModuleManager = require("client.module_framework.ModuleManager")
      local login_module = ModuleManager.GetModule(ModuleManager.LobbyModuleConfig.login_module)
      login_module:sendLogout()
      Client.EnableIosStuckWork(GameFrontendHUD, false)
      Client.CrashLog(NetInterface, 4, "Login", "LogoutE")
      coroutine.yield(0.1)
      self:CloseSelf()
    end)
  end)
end

function SettingAccountUI:InitializeLoginVerify()
  self.ScrollBox_SafeLog = self:InitScrollBox(self.UIRoot.LoopScrollBox_Log)
end

function SettingAccountUI:RegisterEventLoginVerify()
  self.ScrollBox_SafeLog:SetRefreshItemCallback(self.OnRefreshSafeLog, self)
  self.ScrollBox_SafeLog:AddItemEvent("Button_Details", "OnClicked", self.onClickSafeLogItem, self)
  self:AddCommonEvent(EVENTTYPE_SETTING, EVENTID_SETTING_ACCOUNT_LOGINVERIFY_LOAD_RSP, self.OnLoginVerifyLoadRsp, self)
  self:AddCommonEvent(EVENTTYPE_SETTING, EVENTID_SETTING_ACCOUNT_LOGINVERIFY_OPEN_RSP, self.LoginVerifyCommonOnRsp, self)
  self:AddCommonEvent(EVENTTYPE_SETTING, EVENTID_SETTING_ACCOUNT_LOGINVERIFY_CLOSE_RSP, self.LoginVerifyCommonOnRsp, self)
  self:AddCommonEvent(EVENTTYPE_SETTING, EVENTID_SETTING_ACCOUNT_LOGINVERIFY_MAIL_OPEN_RSP, self.LoginVerifyCommonOnRsp, self)
  self:AddCommonEvent(EVENTTYPE_SETTING, EVENTID_SETTING_ACCOUNT_LOGINVERIFY_MAIL_CLOSE_RSP, self.LoginVerifyCommonOnRsp, self)
  self:AddCommonEvent(EVENTTYPE_SETTING, EVENTID_SETTING_ACCOUNT_LOGINVERIFY_CODE_OPEN_RSP, self.LoginVerifyCommonOnRsp, self)
  self:AddCommonEvent(EVENTTYPE_SETTING, EVENTID_SETTING_ACCOUNT_LOGINVERIFY_CODE_CLOSE_RSP, self.LoginVerifyCommonOnRsp, self)
  self:AddCommonEvent(EVENTTYPE_SETTING, EVENTID_SETTING_ACCOUNT_LOGINVERIFY_DEL_DEV_RSP, self.LoginVerifyCommonOnRsp, self)
  self:AddCommonEvent(EVENTTYPE_SETTING, EVENTID_SETTING_ACCOUNT_LOGINVERIFY_GEN_CODE_LIST_RSP, self.LoginVerifyCommonOnRsp, self)
  self:AddCommonEvent(EVENTTYPE_SETTING, EVENTID_SETTING_AGEGATE_VERIFY_FINISHED, self.OnAgeGateVerifyFinished, self)
  self:AddCommonEvent(EVENTTYPE_SETTING, EVENTID_SETTING_ACCOUNT_FAST_UNBIND_SUCCESS, self.OnFastUniBindSuccessRsp, self)
  self:AddOnClickedEvent("Button_LV_Help", self.OnButtonHelpClick, self)
  self:AddOnClickedEvent("LV_Main_Button", self.OnLoginVerifySwitchClick, self)
  self:AddOnClickedEvent("LV_Mail_Button", self.OnLoginVerifyMailSwitchClick, self)
  self:AddOnClickedEvent("LV_Code_Button", self.OnLoginVerifyCodeSwitchClick, self)
  self:AddOnClickedEvent("Button_Log", self.OnClickSafeLogAll, self)
  self:AddOnClickedEvent("Button_2", self.OnClickAgeGate, self)
  for i = 1, 4 do
    self:AddOnClickedEvent("Button_Del_Dev_" .. i, self.OnDevListDelClick, self, i)
  end
end

function SettingAccountUI:PostInitializeLoginVerify()
  self:RefreshLoginVerifyUI()
  LogicLoginVerify.GetAllDataReq()
  LogicLoginVerify.GetAllOpListReq()
end

function SettingAccountUI:RefreshLoginVerifyUI()
  local UIRoot = self.UIRoot
  if LogicLoginVerify.IsFunctionOpen() and LogicLoginVerify.IsBindPhoneOpen() then
    UIRoot.CP_LV_Title:SetVisibility(UEnums.ESlateVisibility.SelfHitTestInvisible)
    UIRoot.CP_LV_Body:SetVisibility(UEnums.ESlateVisibility.SelfHitTestInvisible)
    if LogicLoginVerify.GetBindPhoneNumber() then
      UIRoot.CP_LV_Main:SetVisibility(UEnums.ESlateVisibility.SelfHitTestInvisible)
      if LogicLoginVerify.IsLoginVerifyOpen() then
        UIRoot.CP_LV_Detail:SetVisibility(UEnums.ESlateVisibility.SelfHitTestInvisible)
        UIRoot.CP_DEV:SetVisibility(UEnums.ESlateVisibility.SelfHitTestInvisible)
      else
        UIRoot.CP_LV_Detail:SetVisibility(UEnums.ESlateVisibility.Collapsed)
        UIRoot.CP_DEV:SetVisibility(UEnums.ESlateVisibility.Collapsed)
      end
    else
      UIRoot.CP_LV_Main:SetVisibility(UEnums.ESlateVisibility.Collapsed)
      UIRoot.CP_LV_Detail:SetVisibility(UEnums.ESlateVisibility.Collapsed)
      UIRoot.CP_DEV:SetVisibility(UEnums.ESlateVisibility.Collapsed)
    end
  else
    UIRoot.CP_LV_Title:SetVisibility(UEnums.ESlateVisibility.Collapsed)
    UIRoot.CP_LV_Body:SetVisibility(UEnums.ESlateVisibility.Collapsed)
    UIRoot.CP_DEV:SetVisibility(UEnums.ESlateVisibility.Collapsed)
  end
  if LogicLoginVerify.IsOpLogOpen() then
    UIRoot.CP_Log_Title:SetVisibility(UEnums.ESlateVisibility.SelfHitTestInvisible)
    UIRoot.CP_Log_Body:SetVisibility(UEnums.ESlateVisibility.SelfHitTestInvisible)
  else
    UIRoot.CP_Log_Title:SetVisibility(UEnums.ESlateVisibility.Collapsed)
    UIRoot.CP_Log_Body:SetVisibility(UEnums.ESlateVisibility.Collapsed)
  end
  if LogicLoginVerify.IsLoginVerifyOpen() then
    UIRoot.WidgetSwitcher_LV_Main:SetActiveWidgetIndex(0)
    UIRoot.HorizontalBox_28:SetVisibility(UEnums.ESlateVisibility.Collapsed)
  else
    UIRoot.WidgetSwitcher_LV_Main:SetActiveWidgetIndex(1)
    UIRoot.HorizontalBox_28:SetVisibility(UEnums.ESlateVisibility.SelfHitTestInvisible)
  end
  if LogicLoginVerify.IsLoginVerifyMailOpen() then
    UIRoot.WidgetSwitcher_1:SetActiveWidgetIndex(0)
  else
    UIRoot.WidgetSwitcher_1:SetActiveWidgetIndex(1)
  end
  UIRoot.LV_Mail_Button:SetVisibility(UEnums.ESlateVisibility.Visible)
  if LogicLoginVerify.IsLoginVerifyCodeOpen() then
    UIRoot.LV_Code_Check:SetVisibility(UEnums.ESlateVisibility.SelfHitTestInvisible)
  else
    UIRoot.LV_Code_Check:SetVisibility(UEnums.ESlateVisibility.Collapsed)
  end
  UIRoot.LV_Code_Button:SetVisibility(UEnums.ESlateVisibility.Visible)
  local dev_list = LogicLoginVerify.GetDevList()
  for i = 1, 4 do
    local data = dev_list[i]
    if data then
      UIRoot["DevInfo_" .. i]:SetVisibility(UEnums.ESlateVisibility.SelfHitTestInvisible)
      UIRoot["Text_DevName_" .. i]:SetText(data.sysHardware:gsub("^%l", string.upper))
      if data.is_cur_dev or data.is_cur then
        UIRoot["Text_CurDev_" .. i]:SetVisibility(UEnums.ESlateVisibility.SelfHitTestInvisible)
      else
        UIRoot["Text_CurDev_" .. i]:SetVisibility(UEnums.ESlateVisibility.Collapsed)
      end
    else
      UIRoot["DevInfo_" .. i]:SetVisibility(UEnums.ESlateVisibility.Collapsed)
    end
  end
  local opList = LogicLoginVerify.GetOpList()
  self.ScrollBox_SafeLog:SetData(opList)
  self.ScrollBox_SafeLog:SetVisibility(UEnums.ESlateVisibility.SelfHitTestInvisible)
  self.UIRoot.LoopScrollBox_Log:SetVisibility(UEnums.ESlateVisibility.SelfHitTestInvisible)
  local hasRed = false
  for _, v in ipairs(opList) do
    if v.busi_security_data then
      hasRed = true
      break
    end
  end
  if hasRed then
    self.UIRoot.Log_Title.TextBlock_0:SetVisibility(UEnums.ESlateVisibility.SelfHitTestInvisible)
  else
    self.UIRoot.Log_Title.TextBlock_0:SetVisibility(UEnums.ESlateVisibility.Collapsed)
  end
  self:RefreshLoginVerifyOpListHeight()
  self:RefreshLoginVerifyConst()
end

function SettingAccountUI:RefreshLoginVerifyOpListHeight()
  local UIRoot = self.UIRoot
  local originSize = UIRoot.LoopScrollBox_Log.Slot:GetSize()
  local logList = LogicLoginVerify.GetOpList()
  local logLen = #logList
  local oneSize = 40
  if self.safe_log_show_all then
    local sizeY = math.min(oneSize * logLen, oneSize * 10)
    log(bWriteLog and "[jwm]:sizeY" .. tostring(sizeY))
    UIRoot.LoopScrollBox_Log.Slot:SetSize(FVector2D(originSize.X, sizeY))
    UIRoot.WidgetSwitcher_Log:SetActiveWidgetIndex(1)
  else
    UIRoot.LoopScrollBox_Log.Slot:SetSize(FVector2D(originSize.X, oneSize * 3))
    UIRoot.WidgetSwitcher_Log:SetActiveWidgetIndex(0)
  end
  if logLen <= 3 then
    UIRoot.WidgetSwitcher_Log:SetVisibility(UEnums.ESlateVisibility.Collapsed)
  else
    UIRoot.WidgetSwitcher_Log:SetVisibility(UEnums.ESlateVisibility.SelfHitTestInvisible)
  end
end

function SettingAccountUI:RefreshLoginVerifyConst()
  local UIRoot = self.UIRoot
  WidgetSetText(UIRoot.LV_Title.Title, 27886)
  WidgetSetText(UIRoot.TextBlock_2, 27460)
  if LogicLoginVerify.IsLoginVerifyOpen() then
    WidgetSetText(UIRoot.TextBlock_3, 27966)
  else
    WidgetSetText(UIRoot.TextBlock_3, 27903)
  end
  WidgetSetText(UIRoot.TextBlock_38, 27888)
  WidgetSetText(UIRoot.TextBlock_4, 27888)
  WidgetSetText(UIRoot.TextBlock_27, 27889)
  WidgetSetText(UIRoot.TextBlock_7, 27889)
  WidgetSetText(UIRoot.TextBlock_9, 27485)
  WidgetSetText(UIRoot.TextBlock_14, 27462)
  WidgetSetText(UIRoot.TextBlock_54, 27463)
  WidgetSetText(UIRoot.TextBlock_55, 27890)
  WidgetSetText(UIRoot.TextBlock_56, 27464)
  if LogicLoginVerify.IsLoginVerifyCodeOpen() then
    WidgetSetText(UIRoot.LV_Code_Button_Text, 102037)
  else
    WidgetSetText(UIRoot.LV_Code_Button_Text, 27890)
  end
  WidgetSetText(UIRoot.TextBlock_84, 27894)
  WidgetSetText(UIRoot.TextBlock_85, 27895)
  for i = 1, 4 do
    WidgetSetText(UIRoot["Text_CurDev_" .. i], 27467)
    WidgetSetText(UIRoot["Text_ButtonDel_" .. i], 27468)
  end
  WidgetSetText(UIRoot.Log_Title.Title, 27469)
end

function SettingAccountUI:OnLoginVerifyLoadRsp(_)
  self:RefreshLoginVerifyUI()
  self:SetSafeLevel()
end

function SettingAccountUI:LoginVerifyCommonOnRsp(_, eventId, err)
  if err ~= 0 then
    ShowNotice(LogicLoginVerify.GetSpecialErrCodeNotice(err, err))
    return
  end
  if eventId == EVENTID_SETTING_ACCOUNT_LOGINVERIFY_OPEN_RSP then
    UIManager.CloseUI(UIManager.UI_Config.login_verify_code_box)
  end
  if eventId == EVENTID_SETTING_ACCOUNT_LOGINVERIFY_MAIL_OPEN_RSP then
    ShowNotice(27482)
  end
  if eventId == EVENTID_SETTING_ACCOUNT_LOGINVERIFY_MAIL_CLOSE_RSP then
    ShowNotice(29085)
  end
  if eventId == EVENTID_SETTING_ACCOUNT_LOGINVERIFY_CODE_OPEN_RSP then
    self:OnClickShowCodeVerify()
  end
  if eventId == EVENTID_SETTING_ACCOUNT_LOGINVERIFY_DEL_DEV_RSP then
    ShowNotice(27483)
  end
  if eventId == EVENTID_SETTING_ACCOUNT_LOGINVERIFY_GEN_CODE_LIST_RSP then
    self:OnClickShowCodeVerify()
  end
  self:RefreshLoginVerifyUI()
end

function SettingAccountUI:OnButtonHelpClick()
  self:PlayAudio(require("client.slua.config.sound").click_v1)
  local title = LocUtil.GetLocalizeResStr(27900)
  local content = {
    LocUtil.GetLocalizeResStr(27461),
    " ",
    LocUtil.GetLocalizeResStr(27908),
    LocUtil.GetLocalizeResStr(27909),
    " ",
    LocUtil.GetLocalizeResStr(27910),
    LocUtil.GetLocalizeResStr(27911),
    " ",
    LocUtil.GetLocalizeResStr(27912)
  }
  content = table.concat(content, "\n")
  local ui_manager = require("client.slua_ui_framework.manager")
  ui_manager.ShowUI(ui_manager.UI_Config.HelpTip, 0, title, content)
end

function SettingAccountUI:OnLoginVerifySwitchClick()
  self:PlayAudio(require("client.slua.config.sound").click_v1)
  if LogicLoginVerify.IsLoginVerifyOpen() then
    local title = LocUtil.GetLocalizeResStr(5077)
    local msg = LocUtil.GetLocalizeResStr(27718)
    local function clickOkCallback()
      LogicLoginVerify.CloseLoginVerifyReq()
    end
    CommonMsgBoxMgr.Show(2, title, msg, clickOkCallback)
  else
    local phoneNumber = LogicLoginVerify.GetBindPhoneNumber(true, false)
    if phoneNumber and phoneNumber ~= "" then
      do
        local title = LocUtil.GetLocalizeResStr(27503)
        local msg = LocUtil.LocalizeResFormat(27471, phoneNumber)
        local function clickOkCallback()
          local msgData = {}
          msgData.title = LocUtil.GetLocalizeResStr(27503)
          msgData.msg = LocUtil.LocalizeResFormat(27474, phoneNumber)
          msgData.btnOK = LocUtil.GetLocalizeResStr(27479)
          msgData.styleType = 3
          msgData.codeMinLen = 5
          msgData.canResendTimeGetter = LogicLoginVerify.GetVerifyCodeNextCanSendTime
          msgData.needChangeType = false
          function msgData.resendCallback()
            LogicLoginVerify.SendVerifyCodeReq(LogicLoginVerify.ENUM_AccountType.Phone, LogicLoginVerify.ENUM_VerifyType.OpenVerify, 60, true)
          end
          function msgData.verifyCallback(verifyCode)
            LogicLoginVerify.OpenLoginVerifyReq(verifyCode)
            return false
          end
          UIManager.ShowUI(UIManager.UI_Config.login_verify_code_box, msgData)
        end
        CommonMsgBoxMgr.Show(2, title, msg, clickOkCallback)
      end
    end
  end
end

function SettingAccountUI:OnLoginVerifyMailSwitchClick()
  self:PlayAudio(require("client.slua.config.sound").click_v1)
  if not LogicLoginVerify.IsLoginVerifyOpen() then
    ShowNotice(27480)
    return
  end
  if LogicLoginVerify.IsLoginVerifyMailOpen() then
    LogicLoginVerify.CloseLoginVerifyMailReq()
  else
    local mailAddr = LogicLoginVerify.GetBindMailAddr()
    if mailAddr and mailAddr ~= "" then
      LogicLoginVerify.OpenLoginVerifyMailReq()
    else
      ShowNotice(27481)
      local extraData = {
        title = LocUtil.GetLocalizeResStr(9640),
        showCancel = true,
        okWord = LocUtil.GetLocalizeResStr(27898)
      }
      UIManager.ShowUI(UIManager.UI_Config.setting_associate_mail, 1, nil, extraData)
    end
  end
end

function SettingAccountUI:OnLoginVerifyCodeSwitchClick()
  self:PlayAudio(require("client.slua.config.sound").click_v1)
  if LogicLoginVerify.IsLoginVerifyCodeOpen() then
    self:OnClickShowCodeVerify()
  else
    if not LogicLoginVerify.IsLoginVerifyOpen() then
      ShowNotice(27480)
      return
    end
    local title = LocUtil.GetLocalizeResStr(27501)
    local msg = LocUtil.GetLocalizeResStr(27487)
    local function clickOkCallback()
      LogicLoginVerify.OpenLoginVerifyCodeReq()
    end
    CommonMsgBoxMgr.Show(2, title, msg, clickOkCallback)
  end
end

function SettingAccountUI:OnClickShowCodeVerify()
  log("SettingAccountUI:OnClickShowCodeVerify")
  local msgData = {}
  msgData.title = LocUtil.GetLocalizeResStr(27465)
  msgData.msg = LocUtil.GetLocalizeResStr(27495)
  msgData.btnOK = LocUtil.GetLocalizeResStr(29091)
  msgData.btnCancel = LocUtil.GetLocalizeResStr(110038)
  msgData.styleType = 4
  function msgData.clickOkCallback()
    LogicLoginVerify.GenNewSpareCodeReq()
  end
  UIManager.ShowUI(UIManager.UI_Config.setting_account_spare_code, msgData)
end

function SettingAccountUI:OnDevListDelClick(index)
  self:PlayAudio(require("client.slua.config.sound").click_v1)
  local list = LogicLoginVerify.GetDevList()
  local data = list and list[index]
  if not data then
    return
  end
  local title = LocUtil.GetLocalizeResStr(5077)
  local msg = LocUtil.GetLocalizeResStr(27488)
  local function clickOkCallback()
    LogicLoginVerify.DelDevReq(index)
  end
  CommonMsgBoxMgr.Show(2, title, msg, clickOkCallback)
end

local SafeLogOpTypeName = {
  [1] = 9889,
  [2] = 29960,
  [3] = 9888,
  [6] = 27503,
  [7] = 27501,
  [8] = 29961,
  [9] = 27502,
  [10] = 29083,
  [11] = 27500,
  [12] = 27499,
  [13] = 27498,
  [14] = 29964,
  [18] = 29963
}

function SettingAccountUI:onClickSafeLogItem(widget, index)
  self:PlayAudio(require("client.slua.config.sound").click_v1)
  local data = self.ScrollBox_SafeLog:GetItemData(index)
  if data.busi_security_data then
    local args = {
      busi_security_data = data.busi_security_data,
      sysHardware = data.sysHardware:gsub("^%l", string.upper),
      showTimer = false
    }
    UIManager.ShowUI(UIManager.UI_Config.Inform_Popup_Abnormal_UIBP, data)
  end
end

function SettingAccountUI:OnRefreshSafeLog(widget, index)
  local data = self.ScrollBox_SafeLog:GetItemData(index)
  local TimeUtil = require("client.common.time_util")
  widget.Text_Date:SetText(TimeUtil.FormatTime_YMD(data.op_time, true))
  widget.Text_DevName:SetText(data.sysHardware:gsub("^%l", string.upper))
  local SettingSystem = require("client.logic.setting.logic_setting")
  local name = SettingSystem.GetNameByImsdkChannel(data.account_type)
  local isDeviceLogin = data.op_type == 18
  if isDeviceLogin then
    local richText = "<Setting_SafeLog_Green>%s</>"
    if tostring(data.account_type) == "FF" then
      richText = "<Setting_SafeLog_Yellow>%s</>"
    end
    if not data.account_type then
      name = LocUtil.GetLocalizeResStr(29963)
    end
    widget.UTRichTextBlock_Op:SetText(string.format(richText, name))
  else
    local richText = "<Setting_SafeLog_Green>%s</>"
    local opText = LocUtil.GetLocalizeResStr(SafeLogOpTypeName[data.op_type])
    widget.UTRichTextBlock_Op:SetText(string.format(richText, opText))
  end
  if data.busi_security_data then
    widget.CanvasPanel_1:SetVisibility(UEnums.ESlateVisibility.SelfHitTestInvisible)
  else
    widget.CanvasPanel_1:SetVisibility(UEnums.ESlateVisibility.Collapsed)
  end
end

function SettingAccountUI:OnClickSafeLogAll()
  self:PlayAudio(require("client.slua.config.sound").click_v1)
  self.safe_log_show_all = not self.safe_log_show_all
  self:RefreshLoginVerifyOpListHeight()
end

function SettingAccountUI:OnClickCustomService(sceneId)
  self.UIRoot.helpshift_redpoint:SetVisibility(UEnums.ESlateVisibility.Collapsed)
  self:PlayAudio(require("client.slua.config.sound").click_v1)
  local ModuleManager = require("client.module_framework.ModuleManager")
  local BasicDataTLogReport = ModuleManager.GetModule(ModuleManager.DataModuleConfig.BasicDataTLogReport)
  BasicDataTLogReport:ReportImmediate(TLogEventDefine.LobbySettingHelp, 0)
  local SettingSystem = require("client.logic.setting.logic_setting")
  local LogicCustomerService = require("client.slua.logic.CustomerService.LogicCustomerService")
  SettingSystem.OpenService(sceneId or LogicCustomerService.E_EntranceType.Settings)
end

function SettingAccountUI:OnClickGuestFind()
  self:PlayAudio(require("client.slua.config.sound").click_v1)
  local ui_manager = require("client.slua_ui_framework.manager")
  ui_manager.ShowUI(ui_manager.UI_Config.guest_find_password_post)
end

function SettingAccountUI:OnClickGuestBind()
  self:PlayAudio(require("client.slua.config.sound").click_v1)
  local GuestBindManager = require("client.slua.logic.guest_bind.logic_guest_bind_1700")
  GuestBindManager.ChangeGuestAccountPassword()
end

function SettingAccountUI:OnClickedLevelOneTab(widget, index)
  log(bWriteLog and string.format("SettingAccountUI:OnClickedLevelOneTab self.curSelectTab = %s, index = %s", self.curSelectTab, index))
  self:PlayAudio(require("client.slua.config.sound").tab_v1)
  if self.curSelectTab == index then
    return
  end
  local SettingMacro = require("client.slua.logic.setting.setting_macro")
  if index == SettingMacro.AccountProtectTab.ScanQRCode then
    local LogicTxMissionMain = require("client.slua.logic.TxMission.logic_xmission_main")
    if GameStatus.InFightState() or LogicTxMissionMain.IsInXMission() then
      ShowNotice(200000295)
      self.Common_Tab_Horizontal_LevelOne_Text_UIBP:Select(self.curSelectTab)
      return
    end
    self:InitQRCodeScan()
  end
  self.curSelectTab = index
  self.UIRoot.WidgetSwitcher_AccProtect:SetActiveWidgetIndex(self.curSelectTab - 1)
end

function SettingAccountUI:OnClickAccountProtect()
  self:PlayAudio(require("client.slua.config.sound").click_v1)
  local ModuleManager = require("client.module_framework.ModuleManager")
  local logic_account_protect_setting = ModuleManager.GetModule(ModuleManager.LobbyModuleConfig.logic_account_protect_setting)
  logic_account_protect_setting:SetIsClickGo(true, 3)
  self:SetWidgetVisible(self.UIRoot.TextBlock_AccountRisk, false)
  local logic_account_protect_setting = ModuleManager.GetModule(ModuleManager.LobbyModuleConfig.logic_account_protect_setting)
  logic_account_protect_setting:JumpToITopH5()
end

function SettingAccountUI:OnClickIndividualFunction()
  self:PlayAudio(require("client.slua.config.sound").click_v1)
  local uiManager = require("client.slua_ui_framework.manager")
  if uiManager then
    uiManager.ShowUI(uiManager.UI_Config.AccountScanList_UIBP)
  end
end


local SettingAccountUI = require("client.slua.umg.NewSetting.Account.setting_account_data")
local SettingSystem = require("client.logic.setting.logic_setting")
local SettingAccount = require("client.logic.setting.logic_setting_account")
local Collapsed = UEnums.ESlateVisibility.Collapsed
local Visible = UEnums.ESlateVisibility.Visible
local SelfHitTestInvisible = UEnums.ESlateVisibility.SelfHitTestInvisible
local LobbyHideWidgets = {
  "CanvasBgBg",
  "CanvasApple",
  "CanvasFb",
  "CanvasTW",
  "CanvasGC",
  "CanvasGp",
  "CanvasVK",
  "CanvasWC",
  "CanvasLine",
  "CanvasDC"
}
local HideCanvases = {
  "CanvasPanel_FB",
  "CanvasPanel_GC",
  "CanvasPanel_TW",
  "CanvasPanel_Vk",
  "CanvasPanel_WC",
  "CanvasPanel_GP",
  "CanvasPanel_LINE",
  "CanvasPanel_BgBg",
  "CanvasPanel_51",
  "CanvasPanel_Discord",
  "CanvasPanel_ETC"
}
local firstLoginChannel = ""
local secondLoginChannel = ""

function SettingAccountUI:ctor()
  firstLoginChannel = ""
  secondLoginChannel = ""
end

function SettingAccountUI:RefreshBindInfo()
  local SettingUtil = require("client.logic.setting.setting_util")
  if SettingUtil.GetAutoBind() then
    log(bWriteLog and "[jwm]: SettingUtil.GetAutoBind()")
    local LobbyHandler = require("client.network.Protocol.LobbyHandler")
    local langType = FuncUtil.TransLanguageToImsdkLanguage()
    LobbyHandler.send_check_account_bind_info_req(langType, 3)
    SettingUtil.SetAutoBind(nil)
  end
  local root = self.UIRoot
  root.panelBindFB:SetVisibility(Collapsed)
  root.panelBindFB:SetVisibility(Collapsed)
  root.panelSwitchAccountFB:SetVisibility(Collapsed)
  for _, name in pairs(LobbyHideWidgets) do
    root[name]:SetVisibility(Collapsed)
  end
  for _, name in pairs(HideCanvases) do
    log(bWriteLog and "[jwm]: name" .. tostring(name))
    root[name]:SetVisibility(Collapsed)
  end
  local IMSDKHelper = import("IMSDKHelper")
  local IMSDKHelperInstance = IMSDKHelper.GetInstance()
  local bindCount = IMSDKHelperInstance:GetBindCount()
  local unifiedaccount = IMSDKHelperInstance:IsEqualCurLoginPlatform("unifiedaccount")
  local hms = IMSDKHelperInstance:IsEqualCurLoginPlatform(ShareSource.Hms)
  local guest = IMSDKHelperInstance:IsEqualCurLoginPlatform(ShareSource.Guest)
  local noBind
  if bindCount == 0 and (unifiedaccount or hms) then
    noBind = true
  end
  log(bWriteLog and "[jwm]: noBind" .. tostring(noBind))
  log(bWriteLog and "[jwm]: guest" .. tostring(guest))
  self.binds = {}
  if noBind or guest then
    self:NoBind()
  else
    self:AlreadyBind()
  end
  self.UIRoot.TextBlock_1:SetText(LocUtil.GetLocalizeResStr(25395))
  self.UIRoot.TextBindFB:SetText(LocUtil.GetLocalizeResStr(25394))
end

function SettingAccountUI:RefreshUnBindInfo()
  local IMSDKHelper = import("IMSDKHelper")
  local IMSDKHelperInstance = IMSDKHelper.GetInstance()
  local bindCount = IMSDKHelperInstance:GetBindCount()
  if 2 <= bindCount then
    SettingAccount.UnbindPlatformName = IMSDKHelperInstance:GetCurLoginPlatform()
  else
    SettingAccount.UnbindPlatformName = ""
  end
end

local unbindCanvases = {
  "CanvasPanelUnbindBgBg",
  "CanvasPanelUnbindApple",
  "CanvasPanelUnbindFb",
  "CanvasPanelUnbindGp",
  "CanvasPanelUnbindLINE",
  "CanvasPanelUnbindGc",
  "CanvasPanelUnbindTw",
  "CanvasPanelUnbindvk",
  "CanvasPanelUnbindwc",
  "CanvasPanelUnbindDC"
}
local unbindText = {
  "TextBlock_ubbgbg",
  "TextBlock_ubapple",
  "TextBlock_ubfb",
  "TextBlock_ubgd",
  "TextBlock_ubwc",
  "TextBlock_ubgc",
  "TextBlock_ubgp",
  "TextBlock_ubline",
  "TextBlock_ubtw",
  "TextBlock_ubdc"
}
local unbindNeedShowCanvases = {
  "CanvasPanelUnbindFb",
  "CanvasPanelUnbindGc",
  "CanvasPanelUnbindGp",
  "CanvasPanelUnbindwc"
}
local defaultUnbindCanvases = {
  [14] = "CanvasPanelUnbindvk",
  [35] = "CanvasPanelUnbindTw",
  [36] = "CanvasPanelUnbindLINE",
  [31] = "CanvasPanelUnbindBgBg",
  [40] = "CanvasPanelUnbindApple",
  [39] = "CanvasPanelUnbindDC"
}

function SettingAccountUI:RefreshUnBindState()
  local root = self.UIRoot
  for _, v in pairs(unbindCanvases) do
    root[v]:SetVisibility(Collapsed)
  end
  local UIUtil = require("client.common.ui_util")
  root.CanvasPanel_35:SetVisibility(UIUtil.BoolToVisible(SettingSystem.UnbindIsShow))
  for _, v in pairs(unbindText) do
    root[v]:SetText(tostring(SettingSystem.SUnbindDays))
  end
  if SettingSystem.NUnbindChannel then
    local canvas = unbindNeedShowCanvases[SettingSystem.NUnbindChannel]
    if canvas then
      root[canvas]:SetVisibility(Visible)
    else
      canvas = defaultUnbindCanvases[SettingSystem.NUnbindChannel]
      log(bWriteLog and "[jwm]: canvas" .. tostring(canvas))
      log(bWriteLog and "[jwm]: BP_Setting_Unbind_Channel" .. tostring(SettingSystem.NUnbindChannel))
      if canvas then
        root[canvas]:SetVisibility(Visible)
      end
    end
  end
end

function SettingAccountUI:OnClickOpenBindPanel()
  local audio_util = require("client.common.audio_util")
  audio_util.PlayAudio(require("client.slua.config.sound").click_v1)
  SettingAccount.sFirstChannel = firstLoginChannel
  SettingAccount.sSecondChannel = secondLoginChannel
  log(bWriteLog and "[bgp] OnClickOpenBindPanel=======Slua========")
  local uimanager = require("client.slua_ui_framework.manager")
  uimanager.ShowUI(uimanager.UI_Config.setting_bindchoice_panel)
  local TLogReportUtils = require("client.slua.config.tlog.tlog_report_utils")
  TLogReportUtils.ReportTLogEvent(TLogEventDefine.AccountSafe_IntoSocailBind)
end

function SettingAccountUI:InitRegionSetting()
  local IMSDKHelper = import("IMSDKHelper")
  local IMSDKHelperInstance = IMSDKHelper.GetInstance()
  local isGuest = IMSDKHelperInstance:IsEqualCurLoginPlatform(ShareSource.Guest)
  local notOpen = not LobbySystem.CheckOpen(770001)
  local ModuleManager = require("client.module_framework.ModuleManager")
  local LogicMultipleArea = ModuleManager.GetModule(ModuleManager.CommonModuleConfig.LogicMultipleArea)
  local isRussiaArea = LogicMultipleArea:IsConnectToRussiaArea()
  log(bWriteLog and "[jwm]: notOpen" .. tostring(notOpen))
  log(bWriteLog and "[jwm]: isGuest" .. tostring(isGuest))
  log(bWriteLog and "[jwm]: isRussiaArea: " .. tostring(isRussiaArea))
  local hidePanel = notOpen or isGuest or isRussiaArea
  log(bWriteLog and "[jwm]: hidePanel" .. tostring(hidePanel))
  local UIUtil = require("client.common.ui_util")
  self.UIRoot.CanvasPanel_Region:SetVisibility(UIUtil.BoolToVisible(not hidePanel))
  if not hidePanel then
    self:AddTimerOnce(0.3, function()
      local LogicLoginVerify = require("client.slua.logic.login.logic_login_verify")
      LogicLoginVerify.HandleOpenRegion()
    end)
  end
  self.UIRoot.TextBlock_170:SetText(LocUtil.LocalizeResFormat(7141, SettingAccount.Setting_Region_Name))
  local switcherIndex = 1
  if SettingAccount.Setting_Region_Set_Time == "" then
    switcherIndex = 0
  end
  self.UIRoot.WidgetSwitcher_0:SetActiveWidgetIndex(switcherIndex)
  self.UIRoot.Text_RegionLeftTime:SetText(tostring(SettingAccount.Setting_Region_Set_Time))
  log(bWriteLog and "[jwm]: SettingAccount.nLoginChannel" .. tostring(SettingAccount.nLoginChannel))
end

function SettingAccountUI:NoBind()
  self.UIRoot.panelBindFB:SetVisibility(Visible)
  self.UIRoot.Button_etc:SetVisibility(Visible)
  self.UIRoot.CanvasPanel_ETC:SetVisibility(SelfHitTestInvisible)
  self.UIRoot.Button_BindMore:SetVisibility(Visible)
end

local channelToFunc = {
  apple = "IsAlreadyBindApple",
  gpgc = "IsAlreadyBindGPGC",
  twitter = "IsAlreadyBindTwitter",
  facebook = "IsAlreadyBindFB",
  [ShareSource.Noschat] = "IsAlreadyBindNosChat",
  vk = "IsAlreadyBindVK",
  [ShareSource.BgBg] = "IsAlreadyBindBgBg",
  discord = "IsAlreadyBindDiscord",
  line = "IsAlreadyBindLine"
}

function SettingAccountUI:SetAlreadyBindStateIfNeedOneChannel(channel)
  local IMSDKHelper = import("IMSDKHelper")
  local IMSDKHelperInstance = IMSDKHelper.GetInstance()
  local funcName = channelToFunc[channel]
  log(bWriteLog and "[jwm]: channel" .. tostring(channel))
  log(bWriteLog and "[jwm]: funcName" .. tostring(funcName))
  if not funcName then
    log_error("not funcName")
    return
  end
  if IMSDKHelperInstance[funcName](IMSDKHelperInstance) then
    self:SetAlreadyBindState(channel)
  end
end

function SettingAccountUI:CEHandle()
  local IMSDKHelper = import("IMSDKHelper")
  local IMSDKHelperInstance = IMSDKHelper.GetInstance()
  local bindCount = IMSDKHelperInstance:GetBindCount()
  if bindCount == 1 then
    local PublishRegionMacros = require("client.slua.config.ClientMacros.PublishRegionMacros")
    local isCE = PublishRegionMacros.IsCEVersion()
    local UIUtil = require("client.common.ui_util")
    self.UIRoot.Button_BindMore:SetVisibility(UIUtil.BoolToVisible(not isCE, nil, true))
  end
end

local jkChannels = {
  ShareSource.Apple,
  ShareSource.GpGc,
  ShareSource.Twitter,
  ShareSource.Facebook,
  ShareSource.Line,
  ShareSource.Discord
}
local vngChannels = {
  ShareSource.Apple,
  ShareSource.GpGc,
  ShareSource.Facebook
}
local twChannels = {
  ShareSource.Apple,
  ShareSource.GpGc,
  ShareSource.Facebook,
  ShareSource.Line
}
local defaultChannels = {
  ShareSource.Apple,
  ShareSource.Facebook,
  ShareSource.Noschat,
  ShareSource.VK,
  ShareSource.GpGc,
  ShareSource.Twitter,
  ShareSource.BgBg,
  ShareSource.Discord
}
local Regions = {
  KOREA = jkChannels,
  JAPAN = jkChannels,
  VNG = vngChannels,
  TW = twChannels
}

function SettingAccountUI:AlreadyBind()
  local strRegion = Client.GetPublishRegion()
  local channels = Regions[strRegion]
  channels = channels or defaultChannels
  for _, v in pairs(channels) do
    self:SetAlreadyBindStateIfNeedOneChannel(v)
  end
  self.UIRoot.Button_BindMore:SetVisibility(Collapsed)
  self:CEHandle()
end

local function IsLoginByChannel(channel)
  local IMSDKHelper = import("IMSDKHelper")
  local IMSDKHelperInstance = IMSDKHelper.GetInstance()
  return IMSDKHelperInstance:IsEqualCurLoginPlatform(channel)
end

local channelToWidget = {
  googleplay = "CanvasGp",
  gamecenter = "CanvasGC",
  [ShareSource.Noschat] = "CanvasWC",
  twitter = "CanvasTW",
  vk = "CanvasVK",
  line = "CanvasLine",
  [ShareSource.BgBg] = "CanvasBgBg",
  apple = "CanvasApple",
  discord = "CanvasDC",
  facebook = "CanvasFb"
}

function SettingAccountUI:SwitchImgBindPos(channel)
  local allSortCanvas = {
    "CanvasGp",
    "CanvasGC",
    "CanvasWC",
    "CanvasTW",
    "CanvasVK",
    "CanvasLine",
    "CanvasBgBg",
    "CanvasApple",
    "CanvasDC",
    "CanvasFb"
  }
  if IsLoginByChannel(channel) then
    if channel == ShareSource.GooglePlay then
      table.remove(allSortCanvas, 2)
    elseif channel == ShareSource.GameCenter then
      table.remove(allSortCanvas, 1)
    end
    local canvasName = channelToWidget[channel]
    local function sortFunc(a, b)
      return canvasName == a and canvasName ~= b
    end
    table.sort(allSortCanvas, sortFunc)
  end
end

function SettingAccountUI:AddOneChanel(channel)
  table.insert(self.binds, channel)
end

function SettingAccountUI:ShowOneChannel(channel)
  self.UIRoot[channelToWidget[channel]]:SetVisibility(Visible)
  self:AddOneChanel(channel)
end

function SettingAccountUI:SetAlreadyBindState(channel)
  log(bWriteLog and "[jwm]:SetAlreadyBindState  channel" .. tostring(channel))
  local IMSDKHelper = import("IMSDKHelper")
  local IMSDKHelperInstance = IMSDKHelper.GetInstance()
  local root = self.UIRoot
  root.panelSwitchAccountFB:SetVisibility(Visible)
  local platformName = Client.GetDevicePlatformName()
  local DevicePlatformNameMacros = require("client.slua.config.ClientMacros.DevicePlatformNameMacros")
  local function dealNormal()
    firstLoginChannel = channel
    self:SwitchImgBindPos(channel)
    self:ShowOneChannel(channel)
    self:SetImgBindOpen(channel)
  end
  if channel == ShareSource.Facebook then
    firstLoginChannel = channel
    self:SetImgBindOpen(channel)
    self:ShowOneChannel(channel)
  elseif channel == ShareSource.GpGc then
    if platformName == DevicePlatformNameMacros.Android then
      if IMSDKHelperInstance:IsAlreadyBindGooglePlay() then
        firstLoginChannel = channel
        self:SwitchImgBindPos(channel)
        self:ShowOneChannel(ShareSource.GooglePlay)
        self:SetImgBindOpen(channel)
      end
    elseif IMSDKHelperInstance:IsAlreadyBindGameCenter() then
      firstLoginChannel = channel
      self:SwitchImgBindPos(channel)
      self:ShowOneChannel(ShareSource.GameCenter)
      self:SetImgBindOpen(channel)
    end
  elseif channel == ShareSource.Apple then
    if platformName == DevicePlatformNameMacros.IOS then
      dealNormal()
    end
  elseif channel == ShareSource.Noschat or channel == ShareSource.Twitter or channel == ShareSource.VK or channel == ShareSource.Line or channel == ShareSource.BgBg or channel == ShareSource.Discord then
    dealNormal()
  end
end

function SettingAccountUI:SetImgBindOpen(_)
end

function SettingAccountUI:ShowGlobalBtn(index)
  if index < 0 or not SettingAccount.loginTypeOrderList then
    return false
  end
  local channelName = SettingAccount.loginTypeOrderList[index]
  log(bWriteLog and "[jwm]: index" .. tostring(index))
  log(bWriteLog and "[jwm]: channelName" .. tostring(channelName))
  local root = self.UIRoot
  local IMSDKHelper = import("IMSDKHelper")
  local IMSDKHelperInstance = IMSDKHelper.GetInstance()
  if channelName == ShareSource.Facebook then
    root.Button_FB:SetVisibility(Visible)
    root.CanvasPanel_FB:SetVisibility(SelfHitTestInvisible)
    return true
  elseif channelName == ShareSource.Twitter then
    root.Button_TW:SetVisibility(Visible)
    root.CanvasPanel_TW:SetVisibility(SelfHitTestInvisible)
    return true
  elseif channelName == ShareSource.Noschat then
    if slua_GameFrontendHUD:IsInstallPlatform(ShareSource.Noschat) then
      root.Button_WC:SetVisibility(Visible)
      root.CanvasPanel_WC:SetVisibility(SelfHitTestInvisible)
      return true
    end
    return false
  elseif channelName == ShareSource.GpGc then
    local platformName = Client.GetDevicePlatformName()
    local DevicePlatformNameMacros = require("client.slua.config.ClientMacros.DevicePlatformNameMacros")
    if platformName == DevicePlatformNameMacros.Android then
      if IMSDKHelperInstance:IsAlreadyBindGooglePlay() then
        root.Button_GP:SetVisibility(Visible)
        root.CanvasPanel_GP:SetVisibility(SelfHitTestInvisible)
        return true
      end
    elseif IMSDKHelperInstance:IsAlreadyBindGameCenter() then
      root.Button_GC:SetVisibility(Visible)
      root.CanvasPanel_GC:SetVisibility(SelfHitTestInvisible)
      return true
    end
    return false
  elseif channelName == ShareSource.VK then
    if slua_GameFrontendHUD:IsInstallPlatform(ShareSource.VK) then
      root.Button_VK:SetVisibility(Visible)
      root.CanvasPanel_Vk:SetVisibility(SelfHitTestInvisible)
      return true
    else
      return false
    end
  elseif channelName == ShareSource.Line then
    root.Button_LINE:SetVisibility(Visible)
    root.CanvasPanel_LINE:SetVisibility(SelfHitTestInvisible)
    return true
  elseif channelName == ShareSource.BgBg then
    local strRegion = Client.GetPublishRegion()
    local PublishRegionMacros = require("client.slua.config.ClientMacros.PublishRegionMacros")
    if PublishRegionMacros.IsJapanOrKorea() or strRegion == PublishRegionMacros.VNG or strRegion == PublishRegionMacros.TW then
      return false
    else
      if slua_GameFrontendHUD:IsInstallPlatform(ShareSource.BgBg) then
        root.Button_BgBg:SetVisibility(Visible)
        root.CanvasPanel_BgBg:SetVisibility(SelfHitTestInvisible)
        return true
      end
      return false
    end
  elseif channelName == ShareSource.Apple then
    if Client.IsIOSVersionAbove13() then
      root.Button_Apple:SetVisibility(Visible)
      root.CanvasPanel_51:SetVisibility(SelfHitTestInvisible)
      return true
    end
    return false
  elseif channelName == ShareSource.Discord then
    root.Button_DC:SetVisibility(Visible)
    root.CanvasPanel_Discord:SetVisibility(SelfHitTestInvisible)
    return true
  end
end

function SettingAccountUI:OnIMSDKTipMsgButtonOKClick()
  log(bWriteLog and "[jwm]: OnIMSDKTipMsgButtonOKClick")
  if not self.UIRoot then
    return
  end
  if SettingSystem.NIMSDKTipMsgBtnOKEvent ~= 1 then
    return
  end
  local bindFunctions = {
    "BindFB",
    "BindGPGC",
    "BindGPGC",
    "BindNosChat"
  }
  bindFunctions[35] = "BindTwitter"
  bindFunctions[36] = "BindLine"
  bindFunctions[14] = "BindVK"
  bindFunctions[31] = "BindBgBg"
  bindFunctions[40] = "BindApple"
  bindFunctions[39] = "BindDiscord"
  local IMSDKSystem = require("client.logic.login.logic_imsdk")
  IMSDKSystem.StartIMSDKTimer(3)
  local IMSDKHelper = import("IMSDKHelper")
  local IMSDKHelperInstance = IMSDKHelper.GetInstance()
  local funcName = bindFunctions[SettingSystem.NBindChannel]
  log(bWriteLog and "[jwm]: funcName" .. tostring(funcName))
  log(bWriteLog and "[jwm]: BP_BindChannel" .. tostring(SettingSystem.NBindChannel))
  local logic_imsdk_deeplink_login = require("client.logic.login.logic_imsdk_deeplink_login")
  logic_imsdk_deeplink_login:Init()
  if logic_imsdk_deeplink_login:BindViaSystemWebview(SettingSystem.NBindChannel) then
    return
  end
  if funcName then
    IMSDKHelperInstance[funcName](IMSDKHelperInstance)
  end
end

function SettingAccountUI:OnUIHideWhenBindSucc()
  local IMSDKHelper = import("IMSDKHelper")
  local IMSDKHelperInstance = IMSDKHelper.GetInstance()
  IMSDKHelperInstance:SaveLastIMSDKChannelID(SettingSystem.NBindChannel)
end

local SMail = "/Game/UMG/Texture_200/Atlas/SettingUI/Frames/T_icon_Mailbox_png.T_icon_Mailbox_png"
local SPhone = "/Game/UMG/Texture_200/Atlas/SettingUI/Frames/T_icon_Phone_png.T_icon_Phone_png"
local Paths = {
  [BP_ENUM_PLAYFORM_BGBG] = "/Game/UMG/Texture/Atlas/SettingUI/Frames/T_icon_FB_png.T_icon_FB_png",
  [BP_ENUM_PLAYFORM_WX] = "/Game/UMG/Texture/Atlas/SettingUI/Frames/T_icon_Noschat_png.T_icon_Noschat_png",
  [BP_ENUM_PLAYFORM_TOURIST] = "/Game/UMG/Texture_200/Atlas/SettingUI/Frames/T_icon_Youke_png.T_icon_Youke_png",
  [BP_ENUM_PLAYFORM_GAMECENTER] = "/Game/UMG/Texture/Atlas/SettingUI/Frames/T_icon_GameCenter_png.T_icon_GameCenter_png",
  [BP_ENUM_PLAYFORM_GOOGLEPLAY] = "/Game/UMG/Texture/Atlas/SettingUI/Frames/T_Icon_GooglePlay_png.T_Icon_GooglePlay_png",
  [BP_ENUM_PLAYFORM_TWITTER] = "/Game/UMG/Texture/Atlas/SettingUI/Frames/T_icon_Twitter_png.T_icon_Twitter_png",
  [BP_ENUM_PLAYFORM_VK] = "/Game/UMG/Texture/Atlas/SettingUI/Frames/T_icon_VK_png.T_icon_VK_png",
  [BP_ENUM_PLAYFORM_LINE] = "/Game/UMG/Texture/Atlas/SettingUI/Frames/T_icon_line_png.T_icon_line_png",
  [BP_ENUM_PLAYFORM_BGBGByiTOP] = "/Game/UMG/Texture/Atlas/SettingUI/Frames/T_icon_BGBG_png.T_icon_BGBG_png",
  [BP_ENUM_PLAYFORM_AppleByiTOP] = "/Game/UMG/Texture/Atlas/SettingUI/Frames/T_icon_Apple_png.T_icon_Apple_png",
  [BP_ENUM_PLAYFORM_HMSByiTOP] = "/Game/UMG/Texture_200/Atlas/LoginUI/Frames/Login_term_001_png.Login_term_001_png",
  [BP_ENUM_PLAYFORM_DiscordByiTOP] = "/Game/UMG/Texture_200/Atlas/LoginUI/Frames/Login_discord_png.Login_discord_png",
  [BP_ENUM_PLAYFORM_QRCODE] = "/Game/UMG/Texture_200/Atlas/Common_New_Atlas/Frames/Common_Icon_QRCode_png.Common_Icon_QRCode_png"
}

function SettingAccountUI:ShowLoginChannel()
  local accountData = SettingAccount.GetSettingAccountData()
  local path = Paths[BP_ENUM_PLAYFORM_TOURIST]
  if accountData.account_type == 1 then
    path = SMail
  elseif accountData.account_type == 2 then
    path = SPhone
  else
    local loginChannel = SettingAccount.nLoginChannel
    log(bWriteLog and "[jwm]:loginChannel" .. tostring(loginChannel))
    if Paths[loginChannel] then
      path = Paths[loginChannel]
    end
  end
  local IMSDKQRCodeSystem = require("client.logic.login.logic_imsdk_qrcode")
  local isQRCode = IMSDKQRCodeSystem:IsQRCodeLogined()
  if isQRCode then
    path = Paths[BP_ENUM_PLAYFORM_QRCODE]
  end
  log(bWriteLog and "[jwm]:ShowLoginChannel path " .. tostring(path))
  self:SetTexture(self.UIRoot.Image_login, path)
end


local Setting_Basic_Child_NewUIBP = {}
local SettingSystem = require("client.logic.setting.logic_setting")
local BasicCfg1 = {}
local BasicCfg2 = {}
local VerticalBoxes = {
  "VerticalBox_Aim",
  "VerticalBox_Advanced"
}

function Setting_Basic_Child_NewUIBP:ctor(_, param)
  self.nIndex = 0
  self.sJumpKey = param and param.jumpKey
  log(bWriteLog and "[jwm]: Setting_Basic_Child_NewUIBP self.sJumpKey" .. tostring(self.sJumpKey))
  self.nRedId = param and param.id
  self.bForceHighlight = param and param.bForceHighlight
  self.bChangedMSwitchLayoutData = false
  self.MSwitchModified = false
end

function Setting_Basic_Child_NewUIBP:RegistEventExtra()
  self:AddOnClickedEvent("Button_Aim", self.OnButton_AimClick, self)
  self:AddOnClickedEvent("Button_Advanced", self.OnButton_AdvancedClick, self)
  self:AddCommonEvent(EVENTTYPE_SETTING, EVENTID_SETTING_REFRESH_RED_POINT, self.CheckTabAllRedPoint, self)
  self:AddCommonEvent(EVENTTYPE_SETTING, EVENTID_SETTING_JUMP_RED_POINT, self.JumpRedPoint, self)
  self:AddCommonEvent(EVENTTYPE_SETTING, EVENTID_SETTING_REFRESH_SETTING_BASIC, self.RefreshCurrentWindow, self)
  self:AddCommonEvent(EVENTTYPE_SETTING, EVENTID_SETTING_MSWITCH_CHANGE, function()
    self.MSwitchModified = true
    self:ChangeWeaponMSwitchLayoutData()
  end)
end

function Setting_Basic_Child_NewUIBP:OnPostInitialize()
  Setting_Basic_Child_NewUIBP.__super.OnPostInitialize(self)
  self:SetWidgetVisible(self.UIRoot.TextBlock_Dev, false)
  local Config1 = CDataTable.GetTableByFilter("SettingBasicCfg", "Group", 1)
  local Config2 = CDataTable.GetTableByFilter("SettingBasicCfg", "Group", 2)
  BasicCfg1 = {}
  if SettingSystem.CheckSettingCfgFail(Config1, BasicCfg1) then
    return
  end
  BasicCfg2 = {}
  if SettingSystem.CheckSettingCfgFail(Config2, BasicCfg2) then
    return
  end
  self:InitJumpRed()
  if self.sJumpKey then
    self:JumpByIdOrKey(self.sJumpKey, "GetJumpHeightByKey")
  end
end

function Setting_Basic_Child_NewUIBP:OnShow()
  Setting_Basic_Child_NewUIBP.__super.OnShow(self)
end

function Setting_Basic_Child_NewUIBP:OnClose()
  if self.MSwitchModified then
    self:ChangeWeaponMSwitchLayoutData()
  end
  Setting_Basic_Child_NewUIBP.__super.OnClose(self)
  for _, v in pairs(VerticalBoxes) do
    self.UIRoot[v]:ClearChildren()
  end
  SettingSystem.CloseBefore()
  slua_GameFrontendHUD:FinishModifyUserSettings()
end

function Setting_Basic_Child_NewUIBP:InitJumpRed()
  log(bWriteLog and "[jwm]: BasicChild:InitJumpRed")
  if type(self.nRedId) ~= "number" then
    self:RemoveAllTimer()
    self:AddTimer(0, function()
      self:JumpFail()
    end)
    return
  end
  self:JumpByIdOrKey(self.nRedId, "GetJumpHeightByJumpId")
end

function Setting_Basic_Child_NewUIBP:JumpFail()
  self:SwitchWindow(1)
end

function Setting_Basic_Child_NewUIBP:RefreshCurrentWindow()
  self:SwitchWindow(self.nIndex, true, true)
end

function Setting_Basic_Child_NewUIBP:SwitchWindow(index, bForce, quick)
  if self.nIndex == index and not bForce then
    log(bWriteLog and "[jwm]: SwitchWindow self.nIndex == index")
    return
  end
  log(bWriteLog and string.format("[jwm]: Setting_Basic_Child_NewUIBP:SwitchWindow index:%s ", index))
  self:CheckTabAllRedPoint()
  self:SelectTopButton(index)
  if not quick then
    self:UnRegistEvents()
  end
  SettingSystem.CloseBefore()
  self.nIndex = index
  local window = VerticalBoxes[index]
  if index == 1 then
    SettingSystem.OnShowUI(self.UIRoot[window], BasicCfg1, self)
  else
    SettingSystem.OnShowUI(self.UIRoot[window], BasicCfg2, self)
  end
  self.UIRoot.mainScrollBox:SetScrollOffset(0)
  if not quick then
    self:RegistEvents()
    self:RegistEventExtra()
  end
end

function Setting_Basic_Child_NewUIBP:JumpRedPoint(_, _, redData)
  local jumpId = redData and redData.id
  if not redData then
    log(bWriteLog and "[jwm]: not jumpId")
    return
  end
  self:JumpByIdOrKey(jumpId, "GetJumpHeightByJumpId")
end

function Setting_Basic_Child_NewUIBP:RefreshSecondaryTabRed(basicCfg, redPoint, findKey)
  for _, gourp in pairs(basicCfg) do
    if type(gourp) == "table" then
      for _, itemName in pairs(gourp) do
        if itemName == findKey then
          self:SetWidgetVisible(redPoint, true)
          return
        end
      end
    end
  end
end

function Setting_Basic_Child_NewUIBP:CheckTabAllRedPoint()
  log(bWriteLog and "[jwm]: CheckTabAllRedPoint")
  SettingSystem.CheckTabAllRedPoint(self)
  self:SetWidgetVisible(self.UIRoot.Image_RedPoint, false)
  local SettingRedManager = require("client.logic.setting.setting_redpoint_manager")
  local oneRed = SettingRedManager.GetCurTabRedPointData(1)
  if not oneRed or not next(oneRed) then
    return
  end
  self:SetWidgetVisible(self.UIRoot.Image_RedPoint, false)
  for id, val in pairs(oneRed) do
    if val then
      local findKey = SettingRedManager.RedId2Key(id)
      if findKey then
        self:RefreshSecondaryTabRed(BasicCfg2, self.UIRoot.Image_RedPoint, findKey)
      end
    end
  end
end

function Setting_Basic_Child_NewUIBP:JumpByIdOrKey(jumpIdOrKey, funcName)
  local height, isFind, findKey = SettingSystem[funcName](jumpIdOrKey, BasicCfg1)
  self:RemoveAllTimer()
  self:AddTimer(0, function()
    if not isFind then
      log(bWriteLog and "[jwm]: not isFind")
      self:SwitchWindow(2, true)
      height, isFind, findKey = SettingSystem[funcName](jumpIdOrKey, BasicCfg2)
    else
      self:SwitchWindow(1)
    end
    log(bWriteLog and "[jwm]:height" .. tostring(height))
    if isFind then
      log(bWriteLog and "[jwm]:isFind" .. tostring(isFind))
      self.UIRoot.mainScrollBox:SetScrollOffset(height)
      SettingSystem.PlayRedAnim(findKey, self.nRedId ~= nil, self.bForceHighlight)
    else
      self:JumpFail()
    end
  end)
end

local OtherSwitcherCfg = {
  WidgetSwitcher_3 = {0, 1},
  WidgetSwitcher_7 = {1, 0}
}

function Setting_Basic_Child_NewUIBP:SelectTopButton(index)
  local root = self.UIRoot
  for k, v in pairs(OtherSwitcherCfg) do
    root[k]:SetActiveWidgetIndex(v[index])
  end
  self:SetWidgetVisible(root.VerticalBox_Advanced, index == 2)
  self:SetWidgetVisible(root.VerticalBox_Aim, index == 1)
end

function Setting_Basic_Child_NewUIBP:OnButton_AimClick()
  self:PlayAudio(require("client.slua.config.sound").click)
  self:SwitchWindow(1)
end

function Setting_Basic_Child_NewUIBP:OnButton_AdvancedClick()
  self:PlayAudio(require("client.slua.config.sound").click)
  self:SwitchWindow(2)
end


local function CompareLayoutDetail_OffAxis(A, B)
  if math.abs(A.Scale.X - B.Scale.X) > 0.01 then
    return true
  elseif 0.01 < math.abs(A.Scale.Y - B.Scale.Y) then
    return true
  elseif 1 < math.abs(A.RelativePos.X - (B.RelativePos.X + (A.OriginSize.X - B.OriginSize.X) * (1 - A.Scale.X) / 2)) then
    return true
  elseif 1 < math.abs(A.RelativePos.Y - (B.RelativePos.Y + (A.OriginSize.Y - B.OriginSize.Y) * (1 - A.Scale.Y) / 2)) then
    return true
  elseif 0.01 < math.abs(A.Opacity - B.Opacity) then
    return true
  elseif 0.01 < math.abs(A.AnchorType.Minimum.X - B.AnchorType.Minimum.X) then
    return true
  elseif 0.01 < math.abs(A.AnchorType.Minimum.Y - B.AnchorType.Minimum.Y) then
    return true
  elseif 0.01 < math.abs(A.AnchorType.Maximum.X - B.AnchorType.Maximum.X) then
    return true
  elseif 0.01 < math.abs(A.AnchorType.Maximum.Y - B.AnchorType.Maximum.Y) then
    return true
  end
end

local function CopyLayoutDetail_OffAxis(Target, Source)
  Target.Scale = Source.Scale
  Target.RelativePos.X = Source.RelativePos.X + (Source.OriginSize.X - Target.OriginSize.X) * (1 - Source.Scale.X) / 2
  Target.RelativePos.Y = Source.RelativePos.Y + (Source.OriginSize.Y - Target.OriginSize.Y) * (1 - Source.Scale.Y) / 2
  Target.AnchorType = Source.AnchorType
  Target.Opacity = Source.Opacity
end

function Setting_Basic_Child_NewUIBP:ChangeWeaponMSwitchLayoutData()
  local SettingConfig = slua_GameFrontendHUD:GetUserSettings()
  if not SettingConfig then
    print(bWriteLog and "Setting_Basic_Child_NewUIBP:ChangeWeaponMSwitchLayoutData get UserSetting failed")
    return
  end
  if SettingConfig.bSeperateShootMBtn == true and not self.bChangedMSwitchLayoutData then
    local SlotIndice = {0, 1, 2, 3, 4, 5, 6, 7, 9}
    local PropertyNameList = {"LayoutDetailDict1", "LayoutDetailDict2", "LayoutDetailDict3"}
    local Setting_UIElemLayout_Interface = require("client.slua.umg.NewSetting.Account.setting_account_data")
  end
end

return UITemplate
