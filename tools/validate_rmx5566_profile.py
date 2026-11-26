#!/usr/bin/env python3
"""Guard the owner-confirmed RMX5566RU target profile against accidental drift."""
from pathlib import Path

report = Path("device/RMX5566RU_SOURCE_DISCOVERY_2026-11-27.md").read_text()
required = [
    "RMX5566_16.0.5.1310 (EX01)",
    "Android 16",
    "realme UI",
    "RMX5566_11",
    "5.15.197-android13-8-o-01074-gdfa4acfa411e",
    "Qualcomm Snapdragon 685",
    "8 GB physical + 16 GB RAM expansion",
    "256 GB total",
    "7000 mAh",
    "6.8 inch",
    "8 MP front, 50 MP rear",
    "1 October 2026",
    "RMX5253",
    "Do not build or flash",
]
missing = [item for item in required if item not in report]
if missing:
    raise SystemExit("RMX5566 profile is incomplete or drifted: " + ", ".join(missing))
if "C85 5G" not in report or "MediaTek" not in report:
    raise SystemExit("C85 5G platform mismatch warning is missing")
print("Validated owner-confirmed RMX5566 / Snapdragon 685 / Android 16 profile and ROM safety gate.")
