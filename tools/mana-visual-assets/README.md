# MANA OS visual asset generator

This folder produces the first usable visual assets for the Android-preserving MANA OS update.

## Output
- `bootanimation.zip`: Android userspace boot animation, 1080x2400 logical canvas, 30 fps, 24 frames.
- `mana-lockscreen-wallpaper.png`: matching dark navy/cyan wallpaper base for the future OEM Keyguard overlay.
- `ASSET_INFO.json`: dimensions and limitations.

## Generate locally
Requires Python 3 and Pillow:
```sh
python -m pip install Pillow
python tools/mana-visual-assets/generate.py
```

GitHub Actions also generates and publishes these files as the workflow artifact **MANA-OS-visual-assets**.

## Important
The animation replaces only Android's userspace boot animation when installed by a verified product build. It does not replace the earlier bootloader/AVB splash. The wallpaper is an asset, not a lock-screen replacement. Do not flash these files directly or modify boot/vendor partitions manually. A device-specific product integration and on-device test are still required.
