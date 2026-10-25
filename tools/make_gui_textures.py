"""GUI textures: microscope screen (panel + notes) and the research journal (book, tabs, arrows)."""
import math
import random
import sys
from PIL import Image, ImageDraw

OUT = sys.argv[1]
random.seed(7)


def bevel(d, x0, y0, x1, y1, fill, light, dark, width=1):
    d.rectangle([x0, y0, x1, y1], fill=fill)
    for i in range(width):
        d.line([x0 + i, y0 + i, x1 - i, y0 + i], fill=light)
        d.line([x0 + i, y0 + i, x0 + i, y1 - i], fill=light)
        d.line([x0 + i, y1 - i, x1 - i, y1 - i], fill=dark)
        d.line([x1 - i, y0 + i, x1 - i, y1 - i], fill=dark)


def rounded_panel(d, x0, y0, x1, y1, fill, light, dark, outline=(0, 0, 0, 255)):
    # Vanilla-like rounded corners: 1px outline, 2px bevel.
    d.rectangle([x0 + 1, y0, x1 - 1, y1], fill=outline)
    d.rectangle([x0, y0 + 1, x1, y1 - 1], fill=outline)
    d.rectangle([x0 + 1, y0 + 1, x1 - 1, y1 - 1], fill=fill)
    d.line([x0 + 1, y0 + 1, x1 - 2, y0 + 1], fill=light)
    d.line([x0 + 1, y0 + 2, x1 - 3, y0 + 2], fill=light)
    d.line([x0 + 1, y0 + 1, x0 + 1, y1 - 2], fill=light)
    d.line([x0 + 2, y0 + 1, x0 + 2, y1 - 3], fill=light)
    d.line([x0 + 2, y1 - 1, x1 - 1, y1 - 1], fill=dark)
    d.line([x0 + 3, y1 - 2, x1 - 1, y1 - 2], fill=dark)
    d.line([x1 - 1, y0 + 2, x1 - 1, y1 - 1], fill=dark)
    d.line([x1 - 2, y0 + 3, x1 - 2, y1 - 1], fill=dark)


def slot(d, x, y, dark=False):
    # x, y = item position; frame is one pixel outside.
    if dark:
        bevel(d, x - 1, y - 1, x + 16, y + 16, (34, 40, 50, 255), (18, 22, 28, 255), (98, 110, 126, 255))
    else:
        bevel(d, x - 1, y - 1, x + 16, y + 16, (139, 139, 139, 255), (55, 55, 55, 255), (255, 255, 255, 255))


def noise(img, box, amount, seed):
    rnd = random.Random(seed)
    px = img.load()
    x0, y0, x1, y1 = box
    for y in range(y0, y1):
        for x in range(x0, x1):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            n = rnd.randint(-amount, amount)
            px[x, y] = (max(0, min(255, r + n)), max(0, min(255, g + n)), max(0, min(255, b + n)), a)


def screw(d, x, y):
    d.ellipse([x - 2, y - 2, x + 2, y + 2], fill=(150, 156, 166, 255), outline=(40, 44, 52, 255))
    d.line([x - 1, y + 1, x + 1, y - 1], fill=(70, 74, 84, 255))


# ---------------------------------------------------------------- microscope
img = Image.new("RGBA", (512, 256), (0, 0, 0, 0))
d = ImageDraw.Draw(img)
W, H = 176, 222
# Body: dark lab-steel top part, classic light inventory part below.
rounded_panel(d, 0, 0, W - 1, H - 1, (198, 198, 198, 255), (255, 255, 255, 255), (85, 85, 85, 255))
bevel(d, 4, 4, W - 5, 123, (58, 66, 80, 255), (30, 34, 42, 255), (110, 122, 140, 255))
noise(img, (5, 5, W - 5, 123), 4, 1)
d = ImageDraw.Draw(img)
# Brushed lines
for y in range(6, 122, 3):
    d.line([6, y, W - 7, y], fill=(62, 71, 86, 255))
# Eyepiece ring (center 108, 64): brass outer ring with shading, black inside.
CX, CY = 108, 64
for r in range(54, 45, -1):
    t = (54 - r) / 8
    col = (int(120 + 110 * t), int(86 + 90 * t), int(30 + 40 * t), 255)
    d.ellipse([CX - r, CY - r, CX + r, CY + r], fill=col)
d.ellipse([CX - 54, CY - 54, CX + 54, CY + 54], outline=(40, 28, 10, 255))
# Highlight arc on the brass (top-left)
for a in range(200, 290):
    rad = math.radians(a)
    for rr in (51, 52):
        d.point([CX + rr * math.cos(rad), CY + rr * math.sin(rad)], fill=(255, 238, 170, 255))
# Knurled marks around the ring
for a in range(0, 360, 12):
    rad = math.radians(a)
    x0, y0 = CX + 53 * math.cos(rad), CY + 53 * math.sin(rad)
    x1, y1 = CX + 50 * math.cos(rad), CY + 50 * math.sin(rad)
    d.line([x0, y0, x1, y1], fill=(96, 66, 22, 255))
d.ellipse([CX - 47, CY - 47, CX + 47, CY + 47], fill=(8, 8, 10, 255), outline=(30, 20, 6, 255))
# Slot plates on the left
for (sx, sy) in ((21, 38), (21, 76)):
    bevel(d, sx - 6, sy - 6, sx + 21, sy + 21, (74, 84, 100, 255), (120, 132, 150, 255), (28, 32, 40, 255))
    slot(d, sx, sy, dark=True)
# Slide clips
for sx, sy in ((21, 38),):
    d.rectangle([sx - 4, sy + 6, sx - 2, sy + 9], fill=(170, 176, 186, 255))
    d.rectangle([sx + 17, sy + 6, sx + 19, sy + 9], fill=(170, 176, 186, 255))
for (x, y) in ((9, 9), (W - 10, 9), (9, 118), (W - 10, 118)):
    screw(d, x, y)
# Player inventory slots (vanilla positions)
for row in range(3):
    for col in range(9):
        slot(d, 8 + col * 18, 140 + row * 18)
for col in range(9):
    slot(d, 8 + col * 18, 198)

# Notes panel at (180, 0): clipboard paper, 140 x 222.
NX, NW = 180, 140
rounded_panel(d, NX, 0, NX + NW - 1, H - 1, (120, 84, 52, 255), (160, 118, 78, 255), (70, 46, 26, 255))
noise(img, (NX + 3, 3, NX + NW - 3, H - 3), 6, 2)
d = ImageDraw.Draw(img)
d.rectangle([NX + 6, 12, NX + NW - 7, H - 7], fill=(246, 241, 226, 255))
d.line([NX + 7, H - 6, NX + NW - 6, H - 6], fill=(60, 40, 22, 255))
d.line([NX + NW - 6, 13, NX + NW - 6, H - 6], fill=(60, 40, 22, 255))
for y in range(30, H - 10, 10):
    d.line([NX + 9, y, NX + NW - 10, y], fill=(222, 228, 236, 255))
d.line([NX + 20, 13, NX + 20, H - 8], fill=(238, 196, 196, 255))
# Clip
bevel(d, NX + 46, 3, NX + 93, 16, (176, 182, 192, 255), (230, 234, 240, 255), (96, 102, 112, 255))
d.rectangle([NX + 60, 6, NX + 79, 12], fill=(120, 126, 136, 255))
img.save(OUT + "/microscope.png")

# ---------------------------------------------------------------- journal
img = Image.new("RGBA", (512, 256), (0, 0, 0, 0))
d = ImageDraw.Draw(img)
BW, BH = 300, 190
# Cover (leather, teal like the item)
d.rounded_rectangle([0, 0, BW - 1, BH - 1], radius=6, fill=(26, 74, 68, 255), outline=(8, 26, 24, 255))
noise(img, (1, 1, BW - 1, BH - 1), 7, 3)
d = ImageDraw.Draw(img)
d.rounded_rectangle([3, 3, BW - 4, BH - 4], radius=4, outline=(52, 120, 110, 255))
# Page stacks
for i, off in enumerate((7, 6, 5)):
    shade = 210 - i * 14
    d.rectangle([off, off + 1, BW - 1 - off, BH - 2 - off + 2], fill=(shade, shade - 8, shade - 30, 255))
# Pages
LP = (10, 8, 149, 181)
RP = (150, 8, 289, 181)
for (x0, y0, x1, y1), left in ((LP, True), (RP, False)):
    for x in range(x0, x1 + 1):
        # shading toward the gutter
        dist = (x1 - x) if left else (x - x0)
        g = min(1.0, dist / 18.0)
        base = (247, 240, 222)
        k = 0.82 + 0.18 * g
        d.line([x, y0, x, y1], fill=(int(base[0] * k), int(base[1] * k), int(base[2] * k), 255))
noise(img, (10, 8, 290, 182), 3, 4)
d = ImageDraw.Draw(img)
d.line([149, 8, 149, 181], fill=(150, 136, 108, 255))
d.line([150, 8, 150, 181], fill=(170, 156, 128, 255))
# Ribbon bookmark hanging from the bottom
d.polygon([(214, 181), (222, 181), (222, 189), (218, 186), (214, 189)], fill=(170, 30, 40, 255))
# Tabs at (0, 200): 28 x 24, normal and selected
for i, (fill, light) in enumerate((((150, 128, 96, 255), (190, 168, 132, 255)), ((236, 226, 200, 255), (255, 250, 236, 255)))):
    x0 = i * 32
    d.rounded_rectangle([x0, 200, x0 + 27, 223 + 6], radius=4, fill=fill, outline=(60, 44, 26, 255))
    d.line([x0 + 2, 202, x0 + 25, 202], fill=light)
    d.line([x0 + 2, 202, x0 + 2, 223], fill=light)
# Arrows at (64, 200): 18 x 10, right / left, normal / hovered
def arrow(x0, y0, right, col, dark):
    pts = [(0, 3), (10, 3), (10, 0), (17, 5), (10, 10), (10, 7), (0, 7)]
    if not right:
        pts = [(17 - x, y) for x, y in pts]
    d.polygon([(x0 + x, y0 + y) for x, y in pts], fill=col, outline=dark)
arrow(64, 200, True, (140, 104, 64, 255), (60, 40, 20, 255))
arrow(84, 200, False, (140, 104, 64, 255), (60, 40, 20, 255))
arrow(104, 200, True, (210, 160, 70, 255), (90, 60, 20, 255))
arrow(124, 200, False, (210, 160, 70, 255), (90, 60, 20, 255))
img.save(OUT + "/journal.png")
print("ok")
