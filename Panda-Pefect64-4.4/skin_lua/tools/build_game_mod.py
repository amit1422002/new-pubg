#!/usr/bin/env python3
"""Rebuild game mod lua assets: body (optional) + merge -> bgmi_game_mod.lua"""
import subprocess
import sys
from pathlib import Path

TOOLS = Path(__file__).resolve().parent

def main():
    # Body rebuild from BRPlayerCharacterBase.lua (optional — skip if body hand-edited)
    if "--body" in sys.argv:
        subprocess.check_call([sys.executable, str(TOOLS / "build_gamemod_body.py")])
    subprocess.check_call([sys.executable, str(TOOLS / "merge_gamemod_asset.py")])
    print("OK — rebuild app APK so assets deploy to BGMI clone")

if __name__ == "__main__":
    main()
