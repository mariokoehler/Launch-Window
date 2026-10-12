"""Splits assets/branding/logo.svg into aligned layer PNGs (800 x 800, 4x) for splash.jsx.

Run through make_splash.py. Every layer is the full logo canvas with only its own elements, so
they stack in EffectCraft at the comp centre; the text uses the real fonts from assets/fonts/.
"""
import os
import re
import sys

import resvg_py

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.normpath(os.path.join(HERE, "..", ".."))
OUT = os.path.join(HERE, "build", "layers")
PX = 800

svg = open(os.path.join(REPO, "assets", "branding", "logo.svg"), encoding="utf-8").read()
lines = svg.splitlines()
defs = "\n".join(lines[3:7])
body = [l.strip() for l in lines if l.strip().startswith("<") and not l.strip().startswith(("<?", "<svg", "</svg", "<title", "<defs", "</defs", "<clipPath", "<g ", "</g", "<!--"))]
body = [l for l in body if 'id="sparkle"' not in l]


def pick(pattern):
    return [l for l in body if re.search(pattern, l)]


stars = pick(r'href="#sparkle".*fill="#f5efe1"')
rocket = [l for l in body if re.search(r'd="M100,40|d="M98\.5|d="M96,64|d="M95,78|d="M94,94|<rect|d="M94,112|d="M106,112', l)]
flame = pick(r'd="M94,122|d="M97,122')
layers = {
    "ring": (pick(r'r="98"'), False),
    "disc": (pick(r'r="88"'), False),
    "earth": (pick(r'r="150"|Q100,1[12]8'), True),
    "arc": (pick(r'C68,44'), True),
    "accent": (pick(r'href="#sparkle".*e0a72e'), True),
    "rocket": (rocket, False),
    "flame": (flame, False),
    "rules": (pick(r'<line'), True),
    "wordmark": (pick(r'LAUNCH WINDOW'), False),
    "tagline": (pick(r'Stay one'), False),
}
for i, s in enumerate(stars):
    layers[f"star{i}"] = ([s], True)
# The frame drawn over a rising rocket: gold ring, navy band and hairline, with the field cut out.
frame = ('<path fill-rule="evenodd" fill="#e0a72e" d="M2,100 a98,98 0 1,0 196,0 a98,98 0 1,0 -196,0 '
         'M12,100 a88,88 0 1,0 176,0 a88,88 0 1,0 -176,0 Z"/>'
         '<path fill-rule="evenodd" fill="#16305c" d="M12,100 a88,88 0 1,0 176,0 a88,88 0 1,0 -176,0 '
         'M16,100 a84,84 0 1,0 168,0 a84,84 0 1,0 -168,0 Z"/>'
         + "".join(pick(r'stroke-width="1.2"')))
layers["frame"] = ([frame], False)

os.makedirs(OUT, exist_ok=True)
fonts = os.path.join(REPO, "assets", "fonts")
used = set()
for name, (els, clip) in layers.items():
    if not els:
        sys.exit(f"nothing matched for {name}")
    used.update(els)
    inner = "\n".join(els)
    if clip:
        inner = f'<g clip-path="url(#field)">{inner}</g>'
    doc = (f'<svg xmlns="http://www.w3.org/2000/svg" width="200" height="200" viewBox="0 0 200 200">'
           f'{defs}{inner}</svg>')
    png = resvg_py.svg_to_bytes(svg_string=doc, width=PX, height=PX, font_dirs=[fonts], skip_system_fonts=True)
    with open(os.path.join(OUT, name + ".png"), "wb") as f:
        f.write(bytes(png))
missing = [l for l in body if l not in used and 'stroke-width="1.2"' not in l]
print(len(layers), "layers; unused:", missing)
