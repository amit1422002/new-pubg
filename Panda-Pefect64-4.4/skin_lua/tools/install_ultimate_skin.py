#!/usr/bin/env python3
"""Sync tools/ultimate_skin_mod.lua -> assets/skin_mod_bgmi.lua and jni/skin.cpp"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = Path(__file__).resolve().parent / "ultimate_skin_mod.lua"
LUA_OUT = ROOT / "app" / "src" / "main" / "assets" / "skin_mod_bgmi.lua"
CPP_OUT = ROOT / "app" / "src" / "main" / "jni" / "skin.cpp"

def main():
    lua = SRC.read_text(encoding="utf-8")
    LUA_OUT.write_text(lua, encoding="utf-8")
    CPP_OUT.write_text(f'const char* skin_script = R"lua(\n{lua}\n)lua";\n', encoding="utf-8")
    print(f"synced {len(lua)} chars -> {LUA_OUT.name} and {CPP_OUT.name}")

if __name__ == "__main__":
    main()
