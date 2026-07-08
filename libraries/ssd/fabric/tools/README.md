# SSD tools

Asset-generation tooling for the Seven-Segment Display mod. Not part of the Gradle build — run
manually when the base art changes.

## `generate_glyphs.py`

Regenerates the **derived** glyph textures from the **canonical** hand-authored ones.

- **Inputs** (committed, edit these): `textures/block/digit_0..9.png` and `template.png`.
- **Outputs** (derived, overwritten): the hex letters `digit_10..15.png` (A–F), every `_lit` /
  `_glow{1,2}` / `_lit_glow{1,2}` variant, and `casing.png`.

The letters are composed from the exact same seven segments the digits use (extracted by digit
"signature"), so they match the base art's style automatically.

```sh
pip install pillow
python3 libraries/ssd/tools/generate_glyphs.py
```

Regeneration is visually identical to the committed textures; only dead RGB under fully-transparent
pixels may differ, which has no visual or functional effect.
