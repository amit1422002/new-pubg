#!/usr/bin/env python3
"""Merge BGMIGameMod.lua + BGMIGameModBody.lua into bgmi_game_mod.lua assets."""
from pathlib import Path
import shutil

REPO = Path(__file__).resolve().parents[2]
assets = REPO / "app" / "src" / "main" / "assets"
boot = (assets / "BGMIGameMod.lua").read_text(encoding="utf-8")
body = (assets / "BGMIGameModBody.lua").read_text(encoding="utf-8")
tail = "\npcall(function() if _G.ApplyBgmiGameMod then _G.ApplyBgmiGameMod() end end)\n"
merged = boot.rstrip() + "\n\n" + body.lstrip() + tail

out_app = assets / "bgmi_game_mod.lua"
out_app.write_text(merged, encoding="utf-8")
print(f"wrote {out_app} ({out_app.stat().st_size} bytes)")

out_skin = REPO / "skin_lua" / "assets" / "bgmi_game_mod.lua"
out_skin.parent.mkdir(parents=True, exist_ok=True)
shutil.copy2(out_app, out_skin)
print(f"copied -> {out_skin}")
