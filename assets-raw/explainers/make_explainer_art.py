"""Rasterizes the explainer drawings for the libGDX client, and pulls their text out as labels.

Each <name>.svg here is one explainer sheet's drawing (from the Explainers mockup canvas in Claude
Design, the project's own art; ui-spec.md component 18). The client draws text itself, so it stays
as sharp as the rest of the screen at any window size: this script removes every <text> from the
drawing, renders the rest at twice its size on an opaque white ground with the drawing's faint grid
(left out for a small inline drawing whose root says data-grid="none")
(RGB, so mipmaps never fringe and libGDX never reads it as alpha), and writes the text to a labels
file the client places over the picture.

Outputs, per drawing, under assets/ui/explainers/:
  <name>.png   the drawing without its text, 2x
  <name>.json  {"width", "height", "labels": [{"x", "y", "anchor", "kind", "size", "color", "text"}]}
               x and y in the SVG's own units, y the text's baseline; anchor start/middle/end;
               kind the CSS class (d-lbl, d-lbl-gold, d-txt, d-sm, d-head, d-big) or "num" for a
               digit in a numbered circle; size the font size if the source overrides it, else 0.

Run from the repo root:  python assets-raw/explainers/make_explainer_art.py   (needs: pip install resvg-py pillow)
"""
import glob
import io
import json
import os
import re
import xml.etree.ElementTree as ET

import resvg_py
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.normpath(os.path.join(HERE, "..", ".."))
OUT = os.path.join(REPO, "assets", "ui", "explainers")
SVG = "http://www.w3.org/2000/svg"
SCALE = 2
GRID = 24
GRID_COLOR = "rgba(22,48,92,0.05)"

# The class colors, from the mockup's CSS; an inline fill wins.
CLASS_COLORS = {
    "d-lbl": "#16305c", "d-head": "#16305c", "d-big": "#16305c",
    "d-lbl-gold": "#8a5d12", "d-txt": "#5b5548", "d-sm": "#5b5548",
}


def style_of(element):
    """The element's inline style as a dict."""
    pairs = (part.split(":", 1) for part in (element.get("style") or "").split(";") if ":" in part)
    return {key.strip(): value.strip() for key, value in pairs}


def labels_of(root):
    """Every <text> as a label, removing it from the tree."""
    labels = []
    parents = {child: parent for parent in root.iter() for child in parent}
    for text in list(root.iter("{%s}text" % SVG)):
        style = style_of(text)
        kind = text.get("class") or "num"
        size = style.get("font-size", "")
        color = text.get("fill") or style.get("fill") or CLASS_COLORS.get(kind, "#16305c")
        labels.append({
            "x": float(text.get("x")), "y": float(text.get("y")),
            "anchor": text.get("text-anchor", "start"),
            "kind": kind,
            "size": float(size[:-2]) if size.endswith("px") else 0,
            "color": color,
            "text": "".join(text.itertext()),
        })
        parents[text].remove(text)
    return labels


def ground(width, height, grid):
    """The opaque white ground and, unless the drawing says data-grid="none", its faint grid, as SVG."""
    lines = [f'<rect width="{width}" height="{height}" fill="#ffffff"/>']
    if not grid:
        return "".join(lines)
    for x in range(0, width + 1, GRID):
        lines.append(f'<path d="M{x} 0 L{x} {height}" stroke="{GRID_COLOR}" stroke-width="1"/>')
    for y in range(0, height + 1, GRID):
        lines.append(f'<path d="M0 {y} L{width} {y}" stroke="{GRID_COLOR}" stroke-width="1"/>')
    return "".join(lines)


def convert(path):
    name = os.path.splitext(os.path.basename(path))[0]
    ET.register_namespace("", SVG)
    root = ET.parse(path).getroot()
    width, height = (int(float(v)) for v in root.get("viewBox").split()[2:])
    grid = root.get("data-grid") != "none"
    labels = labels_of(root)
    body = ET.tostring(root, encoding="unicode")
    body = re.sub(r"^<svg[^>]*>", "", body)[: -len("</svg>")]
    svg = (f'<svg xmlns="{SVG}" viewBox="0 0 {width} {height}" width="{width * SCALE}" '
           f'height="{height * SCALE}">{ground(width, height, grid)}{body}</svg>')
    png = bytes(resvg_py.svg_to_bytes(svg_string=svg))
    Image.open(io.BytesIO(png)).convert("RGB").save(os.path.join(OUT, name + ".png"), optimize=True)
    with open(os.path.join(OUT, name + ".json"), "w", encoding="utf-8", newline="\n") as file:
        json.dump({"width": width, "height": height, "labels": labels}, file, indent=1, ensure_ascii=False)
        file.write("\n")
    print(f"{name}: {width}x{height}, {len(labels)} labels")


def main():
    os.makedirs(OUT, exist_ok=True)
    for path in sorted(glob.glob(os.path.join(HERE, "*.svg"))):
        convert(path)


if __name__ == "__main__":
    main()
