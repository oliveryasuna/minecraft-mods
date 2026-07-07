#!/usr/bin/env python3
"""Regenerate the display's derived glyph textures from the canonical base art.

The seven-segment display ships a small set of hand-authored 16x16 textures and
DERIVES the rest from them. This script is the single source of truth for that
derivation, so the derived textures are reproducible rather than lost shell
history.

Canonical inputs (committed, hand-authored) under textures/block/:
  - digit_0.png .. digit_9.png : the ten digits, transparent background, lit
    segments bright red and unlit segments dark red.
  - template.png              : all segments unlit (the blank display face).

Derived outputs (this script overwrites them):
  - digit_10.png .. digit_15.png              : the hex letters A b C d E F,
    composed from the same seven segments the digits use.
  - digit_<n>_lit.png            (n = 0..15)  : lit segments only (unlit dropped).
  - digit_<n>_glow{1,2}.png      (n = 0..15)  : base + a soft red halo.
  - digit_<n>_lit_glow{1,2}.png  (n = 0..15)  : lit-only + halo.
  - casing.png                                : the plain block-body texture used
    on the non-display faces of the item model.

How A-F is derived: each of the ten digits lights a known subset of the seven
segments (a..g). For every "ever-lit" pixel we compute the set of digits that
light it -- its signature -- which uniquely identifies its segment. From that
segment map we can compose any glyph, so the letters match the digits' art
exactly (no separate drawing needed).

Usage:
    pip install pillow
    python3 libraries/ssd/tools/generate_glyphs.py
"""

from __future__ import annotations

import os
from PIL import Image, ImageFilter

BLOCK_DIR = os.path.join(
    os.path.dirname(__file__),
    "..", "src", "main", "resources", "assets", "seven-segment-display", "textures", "block",
)

SIZE = 16
BRIGHT_MIN = 250  # red channel of a lit-segment pixel
DARK = (46, 10, 10, 255)  # an unlit-segment pixel (matches the source art)
GLOW_RGB = (255, 47, 47)

# Which segments (a..g) each digit lights -- standard 7-segment layout.
DIGIT_SEGMENTS = {
    0: "abcdef", 1: "bc", 2: "abged", 3: "abgcd", 4: "fgbc",
    5: "afgcd", 6: "afgedc", 7: "abc", 8: "abcdefg", 9: "abcdfg",
}

# Which segments each hex letter lights (glyph index -> segments). b/d are
# lower-case because upper-case B/D are indistinguishable from 8/0.
LETTER_SEGMENTS = {10: "abcefg", 11: "cdefg", 12: "adef", 13: "bcdeg", 14: "adefg", 15: "aefg"}

# Glow presets: index -> (gaussian blur radius, alpha scale, alpha cap).
GLOW_LEVELS = {1: (1.1, 1.0, 100), 2: (1.3, 1.35, 200)}


def _path(name: str) -> str:
    return os.path.join(BLOCK_DIR, name)


def _bright_pixels(image: Image.Image) -> set[tuple[int, int]]:
    px = image.load()
    return {(x, y) for y in range(SIZE) for x in range(SIZE)
            if px[x, y][3] > 0 and px[x, y][0] >= BRIGHT_MIN}


def _segment_map() -> dict[tuple[int, int], str]:
    """Map every segment pixel to its segment letter, via digit signatures."""
    digits = {d: Image.open(_path(f"digit_{d}.png")).convert("RGBA") for d in range(10)}
    bright = {d: _bright_pixels(img) for d, img in digits.items()}

    signature_to_segment = {
        frozenset(d for d in range(10) if seg in DIGIT_SEGMENTS[d]): seg
        for seg in "abcdefg"
    }

    result: dict[tuple[int, int], str] = {}
    for pixel in set().union(*bright.values()):
        signature = frozenset(d for d in range(10) if pixel in bright[d])
        segment = signature_to_segment.get(signature)
        if segment is None:
            raise SystemExit(f"Pixel {pixel} did not resolve to a segment; base art is non-standard.")
        result[pixel] = segment
    return result


def _blank() -> Image.Image:
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def _compose_letters(seg_map: dict[tuple[int, int], str]) -> None:
    """Build digit_10..15 (A-F) from the extracted segments."""
    d8 = Image.open(_path("digit_8.png")).convert("RGBA").load()  # bright colour source
    for value, segments in LETTER_SEGMENTS.items():
        image = _blank()
        px = image.load()
        for (x, y), segment in seg_map.items():
            px[x, y] = d8[x, y] if segment in segments else DARK
        image.save(_path(f"digit_{value}.png"))


def _lit_only(base: Image.Image) -> Image.Image:
    """Keep only the bright (lit) pixels; make the dark unlit ones transparent.

    Non-lit pixels keep their RGB under an alpha of 0 (matching the source art's byte layout, so a
    regeneration is a no-op) — transparent either way.
    """
    out = base.copy()
    px = out.load()
    for y in range(SIZE):
        for x in range(SIZE):
            r, g, b, a = px[x, y]
            if not (a > 0 and r >= BRIGHT_MIN):
                px[x, y] = (r, g, b, 0)
    return out


def _glow(lit: Image.Image, blur: float, scale: float, cap: int) -> Image.Image:
    mask = Image.new("L", (SIZE, SIZE), 0)
    mpx, lpx = mask.load(), lit.load()
    for y in range(SIZE):
        for x in range(SIZE):
            if lpx[x, y][3] > 0:
                mpx[x, y] = 255
    blurred = mask.filter(ImageFilter.GaussianBlur(blur)).load()
    glow = _blank()
    gpx = glow.load()
    for y in range(SIZE):
        for x in range(SIZE):
            alpha = min(cap, int(blurred[x, y] * scale))
            if alpha > 0:
                gpx[x, y] = (*GLOW_RGB, alpha)
    return glow


def _compose_variants() -> None:
    for value in range(16):
        base = Image.open(_path(f"digit_{value}.png")).convert("RGBA")
        lit = _lit_only(base)
        lit.save(_path(f"digit_{value}_lit.png"))
        for level, (blur, scale, cap) in GLOW_LEVELS.items():
            glow = _glow(lit, blur, scale, cap)
            Image.alpha_composite(glow, base).save(_path(f"digit_{value}_glow{level}.png"))
            Image.alpha_composite(glow, lit).save(_path(f"digit_{value}_lit_glow{level}.png"))


def _compose_casing() -> None:
    """A plain block-body texture: the display's background colour with a darker border."""
    body, edge = (11, 11, 13, 255), (5, 5, 6, 255)
    image = Image.new("RGBA", (SIZE, SIZE), body)
    px = image.load()
    for i in range(SIZE):
        for x, y in ((i, 0), (i, SIZE - 1), (0, i), (SIZE - 1, i)):
            px[x, y] = edge
    image.save(_path("casing.png"))


def main() -> None:
    seg_map = _segment_map()
    _compose_letters(seg_map)  # digit_10..15 must exist before _compose_variants
    _compose_variants()
    _compose_casing()
    print("Regenerated derived glyph textures in", os.path.normpath(BLOCK_DIR))


if __name__ == "__main__":
    main()
