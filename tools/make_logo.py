#!/usr/bin/env python3
"""Generates Aqua Client branding assets.

If ``branding/aqua_logo_source.png`` exists (the official logo artwork, transparent
background), it is used as the emblem source and only resized. Otherwise an original
placeholder emblem (water drop with a wave) is drawn procedurally.

Outputs (all RGBA PNG, transparent edges preserved, proportions kept):
  src/main/resources/assets/aqua/icon.png                  128 px  (Fabric/Mod Menu icon)
  src/main/resources/assets/aqua/textures/gui/emblem.png   256 px  (menu / title / island)
  src/main/resources/assets/aqua/textures/gui/emblem_small.png 64 px (watermark, notifications)
  branding/icons/aqua_<size>.png                            16..512 px
"""
import math
import os
import sys

from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "branding", "aqua_logo_source.png")
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "aqua")

ACCENT = (82, 228, 242)
ACCENT2 = (168, 243, 255)
DEEP = (26, 120, 168)


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(len(a)))


def drop_mask(size):
    """Anti-aliased water-drop silhouette (supersampled)."""
    ss = 4
    s = size * ss
    img = Image.new("L", (s, s), 0)
    d = ImageDraw.Draw(img)
    cx = s / 2
    r = s * 0.30
    cy = s * 0.60
    tip_y = s * 0.08
    # Circle body
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=255)
    # Tapered top: polygon tangent to circle
    ang = math.asin(r / (cy - tip_y))
    pts = [(cx, tip_y)]
    for sgn in (-1, 1):
        px = cx + sgn * r * math.cos(ang)
        py = cy - r * math.sin(ang)
        pts.append((px, py))
    d.polygon([pts[0], pts[1], (cx, cy), pts[2]], fill=255)
    return img.resize((size, size), Image.LANCZOS)


def wave_mask(size, y0, amp, thickness):
    ss = 4
    s = size * ss
    img = Image.new("L", (s, s), 0)
    d = ImageDraw.Draw(img)
    pts_top = []
    pts_bot = []
    for i in range(0, s + 1, 2):
        x = i
        y = y0 * s + math.sin(i / s * math.pi * 2.0 + 0.6) * amp * s
        pts_top.append((x, y))
        pts_bot.append((x, y + thickness * s))
    d.polygon(pts_top + pts_bot[::-1], fill=255)
    return img.resize((size, size), Image.LANCZOS)


def procedural_emblem(size=1024):
    body = drop_mask(size)
    # Vertical gradient fill
    grad = Image.new("RGBA", (size, size))
    gp = grad.load()
    for y in range(size):
        t = y / (size - 1)
        c = lerp(ACCENT2, DEEP, t ** 1.2)
        for x in range(size):
            gp[x, y] = c + (255,)
    emblem = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    emblem.paste(grad, (0, 0), body)

    # Inner highlight (glass feel)
    hl = Image.new("L", (size, size), 0)
    hd = ImageDraw.Draw(hl)
    hd.ellipse([size * 0.33, size * 0.44, size * 0.47, size * 0.62], fill=120)
    hl = hl.filter(ImageFilter.GaussianBlur(size * 0.03))
    white = Image.new("RGBA", (size, size), (255, 255, 255, 255))
    emblem = Image.composite(white, emblem, Image.composite(hl, Image.new("L", (size, size), 0), body))

    # Wave cut-out band (transparent) across the lower half
    wave = wave_mask(size, 0.66, 0.025, 0.055)
    cut = Image.composite(Image.new("L", (size, size), 0), emblem.getchannel("A"), wave)
    emblem.putalpha(cut)
    # Lower wave fill a touch darker for depth
    lower = wave_mask(size, 0.72, 0.025, 0.40)
    lower = Image.composite(lower, Image.new("L", (size, size), 0), body)
    tint = Image.new("RGBA", (size, size), DEEP + (255,))
    shade = Image.blend(emblem, tint, 0.35)
    emblem = Image.composite(shade, emblem, lower)
    a = emblem.getchannel("A")
    a = Image.composite(Image.new("L", (size, size), 0), a, wave)
    emblem.putalpha(a)
    return emblem


def fit_square(img, size):
    """Resize keeping proportions, centred on a transparent square canvas."""
    w, h = img.size
    scale = size / max(w, h)
    nw, nh = max(1, round(w * scale)), max(1, round(h * scale))
    r = img.resize((nw, nh), Image.LANCZOS)
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    out.paste(r, ((size - nw) // 2, (size - nh) // 2), r)
    return out


def main():
    if os.path.exists(SRC):
        src = Image.open(SRC).convert("RGBA")
        bbox = src.getchannel("A").getbbox()
        if bbox:
            src = src.crop(bbox)
        print("Using official logo artwork:", SRC)
    else:
        src = procedural_emblem(1024)
        print("No official logo found; generated original placeholder emblem.")
    os.makedirs(os.path.join(ASSETS, "textures", "gui"), exist_ok=True)
    os.makedirs(os.path.join(ROOT, "branding", "icons"), exist_ok=True)
    fit_square(src, 128).save(os.path.join(ASSETS, "icon.png"))
    fit_square(src, 256).save(os.path.join(ASSETS, "textures", "gui", "emblem.png"))
    fit_square(src, 64).save(os.path.join(ASSETS, "textures", "gui", "emblem_small.png"))
    for s in (16, 32, 48, 64, 128, 256, 512):
        fit_square(src, s).save(os.path.join(ROOT, "branding", "icons", f"aqua_{s}.png"))
    fit_square(src, 1024).save(os.path.join(ROOT, "branding", "aqua_emblem_1024.png"))
    return 0


if __name__ == "__main__":
    sys.exit(main())
