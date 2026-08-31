#!/usr/bin/env python3
"""Build BGMIGameModBody.lua from jni/BRPlayerCharacterBase.lua (mod section only)."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
src = (ROOT / "app/src/main/jni/BRPlayerCharacterBase.lua").read_text(encoding="utf-8")
start = src.index("local CharacterBase = require")
end = src.index('}, "BRPlayerCharacterBase")') + len('}, "BRPlayerCharacterBase")')
core = src[start:end]

header = r'''-- Auto-built from BRPlayerCharacterBase.lua (points 1-5) — Anubis game mod body
_G.__BGMI_RunModBody = function()
if _G.__BGMI_BODY_RAN then return end

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

'''

footer = r'''
if _G.__BGMI_DoExportMod then
    _G.__BGMI_DoExportMod(BRPlayerCharacterBase)
    _G.__BGMI_BODY_RAN = true
    _G.__BGMI_GAME_MOD_PATCHED = true
end

_G.__BGMI_StartMatchFeatures = function(self)
    pcall(function()
        if slua.isValid(self) and self.InitVisualAssistance and _G.Mod_LuaESP then
            self:InitVisualAssistance()
        end
    end)
end

_G.__BGMI_InitVisualAssistance = function(ch)
    if ch and slua.isValid(ch) and ch.InitVisualAssistance and _G.Mod_LuaESP then
        pcall(function() ch:InitVisualAssistance() end)
    end
end

end
'''

# Patch core
core = core.replace("_G.Mod_AimAssist = true", "_G.Mod_AimAssist = _G.Mod_AimAssist ~= false")
core = core.replace("ShowLegalCredit()\n      ", "")
core = core.replace("      ShowLegalCredit()\n", "")
core = core.replace("local popupShown = false\n\nlocal function ShowLegalCredit()", "local function _unused_ShowLegalCredit()")
core = core.replace(
    "    if not self._MagicBulletTimer then",
    "    if _G.Mod_MagicBullet and not self._MagicBulletTimer then",
)
core = core.replace(
    "    if not self._MagicHeadTimer then",
    "    if _G.Mod_MagicHead and not self._MagicHeadTimer then",
)
core = core.replace(
    "        ApplyAimAssist(self)\n\n        local myTeamId",
    "        if _G.Mod_AimAssist then ApplyAimAssist(self) end\n\n        if not _G.Mod_LuaESP then return end\n        local myTeamId",
)
core = core.replace(
    "  if _G.AKModBypassInitialized then",
    "  if not _G.Mod_Bypass or _G.AKModBypassInitialized then",
)
core = core.replace(
    "end\n\nInitializeBypass()\n\nlocal SharedVisualAssistOwner",
    "end\n\nif _G.Mod_Bypass then InitializeBypass() end\n\nlocal SharedVisualAssistOwner",
    1,
)
core = core.replace(
    "CombineClass.DeclareFeature(BRPlayerCharacterBase,",
    "BRPlayerCharacterBase = CombineClass.DeclareFeature(BRPlayerCharacterBase,",
)

out = header + core + footer
out_path = ROOT / "app/src/main/assets/BGMIGameModBody.lua"
out_path.write_text(out, encoding="utf-8")
print("wrote", out_path, len(out), "chars")
