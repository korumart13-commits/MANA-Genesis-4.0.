#!/usr/bin/env python3
"""Generate original MANA OS boot animation and lock-screen wallpaper assets."""
from PIL import Image, ImageDraw, ImageFont, ImageFilter
from pathlib import Path
import math, zipfile, json

OUT = Path("out/mana-visual-assets")
PART = OUT / "bootanimation" / "part0"
PART.mkdir(parents=True, exist_ok=True)
W, H = 540, 1200
BG = (7, 20, 38)
CYAN = (84, 200, 245)
WHITE = (234, 244, 255)
MUTED = (145, 169, 197)
font_path = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
font = ImageFont.truetype(font_path, 54)
small = ImageFont.truetype(font_path, 17)

def frame(i):
    im = Image.new("RGB", (W,H), BG)
    # restrained radial glow behind the identity mark
    glow = Image.new("RGBA", (W,H), (0,0,0,0))
    gd = ImageDraw.Draw(glow)
    alpha = int(20 + 14 * math.sin(i / 23 * math.pi))
    gd.ellipse((95, 390, 445, 740), fill=(20,110,170,alpha))
    glow = glow.filter(ImageFilter.GaussianBlur(65))
    im = Image.alpha_composite(im.convert("RGBA"), glow)
    d = ImageDraw.Draw(im)
    cx, cy = W//2, 545
    # subtle cyan ring, animated sweep
    d.arc((cx-92,cy-92,cx+92,cy+92), start=(i*9)%360, end=(i*9)%360+255, fill=CYAN+(210,), width=3)
    d.ellipse((cx-70,cy-70,cx+70,cy+70), outline=(84,200,245,65), width=1)
    label = "MANA"
    box = d.textbbox((0,0), label, font=font)
    d.text((cx-(box[2]-box[0])/2, cy-32), label, font=font, fill=WHITE+(255,))
    sub = "M A N A   O S"
    sb = d.textbbox((0,0), sub, font=small)
    d.text((cx-(sb[2]-sb[0])/2, cy+45), sub, font=small, fill=CYAN+(235,))
    # small baseline light
    pulse = 0.45 + 0.55 * (0.5 + 0.5*math.sin(i/23*2*math.pi))
    width = int(52 + 90*pulse)
    d.rounded_rectangle((cx-width//2, cy+118, cx+width//2, cy+120), radius=2, fill=CYAN+(int(80+150*pulse),))
    return im.convert("RGB")

for i in range(24):
    frame(i).save(PART / f"{i:02d}.png", optimize=True)

boot_dir = OUT / "bootanimation"
(boot_dir / "desc.txt").write_text("1080 2400 30\np 1 0 part0\n", encoding="utf-8")
zip_path = OUT / "bootanimation.zip"
with zipfile.ZipFile(zip_path, "w", compression=zipfile.ZIP_STORED) as z:
    z.write(boot_dir / "desc.txt", "desc.txt")
    for p in sorted(PART.glob("*.png")):
        z.write(p, f"part0/{p.name}")

# A matching static wallpaper background for a future real Keyguard overlay.
wall = Image.new("RGB", (W,H), BG)
g = Image.new("RGBA", (W,H), (0,0,0,0))
gd = ImageDraw.Draw(g)
gd.ellipse((30,300,510,780), fill=(12,93,145,55))
gd.ellipse((120,420,420,720), outline=(84,200,245,38), width=2)
g = g.filter(ImageFilter.GaussianBlur(55))
wall = Image.alpha_composite(wall.convert("RGBA"),g)
d = ImageDraw.Draw(wall)
# quiet lower-third brand signature; clock is rendered by Android Keyguard itself.
brand = "MANA OS"
bf = ImageFont.truetype(font_path, 25)
bb = d.textbbox((0,0),brand,font=bf)
d.text(((W-(bb[2]-bb[0]))/2, 1030),brand,font=bf,fill=CYAN+(175,))
wall_path = OUT / "mana-lockscreen-wallpaper.png"
wall.convert("RGB").save(wall_path, optimize=True)

(OUT / "ASSET_INFO.json").write_text(json.dumps({
 "name":"MANA OS visual asset pack",
 "target":"realme RMX5566RU / RE645F",
 "bootanimation":"bootanimation.zip; 1080x2400 logical canvas; 30 fps; 24 frames; Android userspace animation only",
 "wallpaper":"mana-lockscreen-wallpaper.png; 540x1200 source; resize/crop to device display",
 "not_included":"No bootloader splash, Keyguard replacement, OTA or flashable firmware."
}, indent=2), encoding="utf-8")
print(f"Generated {zip_path} ({zip_path.stat().st_size} bytes)")
