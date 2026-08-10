#!/usr/bin/env python3
"""Create the offline Kindness Corner hero artwork for the GMRC learning path."""
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "feature-child-home/src/main/res/drawable-nodpi/mw_location_kindness_corner.png"
W, H = 960, 420


def mix(a, b, t):
    return tuple(round(a[i] * (1 - t) + b[i] * t) for i in range(3))


def heart(draw, cx, cy, size, fill):
    r = size * .28
    draw.ellipse((cx - size * .48, cy - size * .18, cx - size * .02, cy + size * .25), fill=fill)
    draw.ellipse((cx + size * .02, cy - size * .18, cx + size * .48, cy + size * .25), fill=fill)
    draw.polygon([(cx - size * .48, cy), (cx + size * .48, cy), (cx, cy + size * .62)], fill=fill)


image = Image.new("RGB", (W, H))
for y in range(H):
    for x in range(W):
        image.putpixel((x, y), mix((226, 249, 244), (121, 204, 188), (y / H) * .75))
draw = ImageDraw.Draw(image, "RGBA")

# Sun, cloud bubbles, and a calm, welcoming horizon.
draw.ellipse((714, 34, 836, 156), fill=(255, 213, 111, 210))
for box in [(58, 62, 210, 115), (122, 36, 286, 99), (802, 120, 1000, 194)]:
    draw.ellipse(box, fill=(255, 255, 255, 105))
draw.polygon([(0, 276), (120, 224), (250, 260), (390, 215), (520, 258), (680, 210), (960, 258), (960, 420), (0, 420)], fill=(93, 177, 119, 185))
draw.polygon([(0, 322), (170, 290), (310, 329), (465, 282), (640, 326), (815, 280), (960, 312), (960, 420), (0, 420)], fill=(69, 149, 104, 215))

# Kindness Corner: a little open-air reading/encouragement nook.
draw.rounded_rectangle((340, 116, 690, 345), radius=32, fill=(250, 251, 228, 235), outline=(255, 255, 255, 220), width=5)
draw.polygon([(316, 126), (515, 52), (714, 126)], fill=(17, 116, 116, 235), outline=(7, 87, 91, 230))
draw.line((366, 132, 366, 325), fill=(7, 87, 91, 220), width=12)
draw.line((664, 132, 664, 325), fill=(7, 87, 91, 220), width=12)
draw.rounded_rectangle((395, 214, 635, 304), radius=18, fill=(244, 185, 75, 230), outline=(161, 104, 45, 180), width=4)
draw.line((413, 247, 617, 247), fill=(255, 231, 157, 230), width=4)
heart(draw, 515, 174, 62, (239, 112, 105, 240))

# Friendly plant friends on the sign.
for x, y, size in [(90, 321, 50), (166, 334, 38), (786, 328, 58), (862, 342, 40)]:
    draw.ellipse((x - size, y, x + size, y + size * 1.3), fill=(48, 130, 83, 220))
    draw.ellipse((x - size * .25, y - size * .35, x + size * .25, y + size * .25), fill=(85, 176, 101, 230))
    draw.line((x, y + size * .8, x, y + size * 2.2), fill=(93, 89, 62, 220), width=7)

# Milo-like animal face peeking from the left, plus two friends sharing the nook.
def animal(cx, cy, fur, inner):
    r = 45
    draw.polygon([(cx - 35, cy - 28), (cx - 60, cy - 78), (cx - 10, cy - 53)], fill=inner)
    draw.polygon([(cx + 35, cy - 28), (cx + 60, cy - 78), (cx + 10, cy - 53)], fill=inner)
    draw.ellipse((cx - r, cy - r, cx + r, cy + r), fill=fur, outline=(42, 75, 76, 220), width=3)
    for dx in (-16, 16):
        draw.ellipse((cx + dx - 6, cy - 10, cx + dx + 6, cy + 2), fill=(35, 57, 59))
        draw.ellipse((cx + dx - 2, cy - 8, cx + dx + 2, cy - 4), fill="white")
    draw.ellipse((cx - 6, cy + 6, cx + 6, cy + 14), fill=(239, 124, 112))
    draw.arc((cx - 16, cy + 8, cx + 16, cy + 31), 10, 170, fill=(42, 75, 76), width=3)

animal(252, 302, (255, 201, 84), (246, 137, 99))
animal(720, 305, (114, 185, 193), (76, 145, 161))
# Shared book and small encouragement hearts.
draw.rounded_rectangle((470, 292, 560, 331), radius=8, fill=(109, 139, 203), outline=(57, 82, 122), width=3)
draw.line((515, 294, 515, 328), fill=(255, 244, 203), width=3)
heart(draw, 300, 100, 30, (239, 112, 105, 210))
heart(draw, 758, 190, 24, (255, 205, 91, 220))

draw.rounded_rectangle((18, 18, W - 18, H - 18), radius=30, outline=(255, 255, 255, 150), width=4)
OUT.parent.mkdir(parents=True, exist_ok=True)
image.save(OUT, format="PNG", optimize=True)
print(OUT)
