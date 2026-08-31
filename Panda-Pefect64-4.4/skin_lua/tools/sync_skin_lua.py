#!/usr/bin/env python3
"""Sync Ultimate skin lua -> assets + skin.cpp. Edit tools/ultimate_skin_mod.lua then run this."""
from pathlib import Path
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
INSTALL = Path(__file__).resolve().parent / "install_ultimate_skin.py"

if __name__ == "__main__":
    subprocess.check_call([sys.executable, str(INSTALL)])
