"""Rasterizes the explainer drawings for the libGDX client, and pulls their text out as labels.

Each <name>.svg here is one explainer sheet's drawing (from the Explainers mockup canvas in Claude
Design, the project's own art; ui-spec.md component 18). The client draws text itself, so it stays
as sharp as the rest of the screen at any window size: this script removes every <text> from the
drawing, renders the rest at twice its size on an opaque white ground with the drawing's faint grid
(left out for a small inline drawing whose root says data-grid="none")
(RGB, so mipmaps never fringe and libGDX never reads it as alpha), and writes the text to a labels
file the client places over the picture.

A drawing may carry its own <style> block (the mockup's rules for its d-* text classes and any
shape classes): resvg applies it to the shapes, and a text class's font-size there becomes the
label's size. Text inherits class, text-anchor, fill and translate() from its <g> ancestors. Two
kinds of text stay in the picture, set by resvg in the repo's own fonts: text that is rotated (a
label can't turn), written in its final case; and a white outline under any text of class "halo",
so its label stays readable over hatching.

Outputs, per drawing, under assets/ui/explainers/:
  <name>.png   the drawing without its text, 2x
  <name>.json  {"width", "height", "labels": [{"x", "y", "anchor", "kind", "size", "color", "text"}]}
               x and y in the SVG's own units, y the text's baseline; anchor start/middle/end;
               kind the CSS class (d-lbl, d-lbl-gold, d-txt, d-sm, d-head, d-big, d-body) or "num"
               for a digit in a numbered circle (class d-num, or none); size the font size if the
               source sets one, else 0.

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
FONTS = os.path.join(REPO, "assets", "fonts")
SVG = "http://www.w3.org/2000/svg"
SCALE = 2
GRID = 24
GRID_COLOR = "rgba(22,48,92,0.05)"
HALO = "fill:#ffffff;stroke:#ffffff;stroke-width:6px;stroke-linejoin:round"

# The class colors, from the mockup's CSS; a drawing's own <style> and an inline fill win.
CLASS_COLORS = {
    "d-lbl": "#16305c", "d-head": "#16305c", "d-big": "#16305c",
    "d-lbl-gold": "#8a5d12", "d-txt": "#5b5548", "d-sm": "#5b5548",
    "d-body": "#1c1c1a", "d-num": "#f5efe1",
}
# The design system's colours, for a fill still written as a CSS variable.
VARIABLES = {"--navy": "#16305c", "--gold": "#e0a72e", "--gold-text": "#8a5d12", "--ink": "#1c1c1a",
             "--ink-muted": "#5b5548", "--on-fill": "#f5efe1"}


def style_of(element):
    """The element's inline style as a dict."""
    pairs = (part.split(":", 1) for part in (element.get("style") or "").split(";") if ":" in part)
    return {key.strip(): value.strip() for key, value in pairs}


def color(value):
    """A fill as #rrggbb, resolving a CSS variable."""
    match = re.fullmatch(r"var\((--[\w-]+)\)", value or "")
    return VARIABLES[match.group(1)] if match else value


def class_rules(root):
    """The drawing's own <style> rules for its text classes: {class: {"size": float, "fill": str}}."""
    rules = {}
    for style in root.iter("{%s}style" % SVG):
        for name, body in re.findall(r"\.([\w-]+)\s*\{([^}]*)\}", style.text or ""):
            size = re.search(r"font-size:\s*([\d.]+)px", body)
            fill = re.search(r"(?<![-\w])fill:\s*(#[0-9a-fA-F]{6})", body)
            rules[name] = {"size": float(size.group(1)) if size else 0, "fill": fill.group(1) if fill else None}
    return rules


def translate_of(element):
    """The x and y of a translate() in the element's transform, else 0, 0."""
    match = re.search(r"translate\(\s*([-\d.]+)[ ,]+([-\d.]+)\s*\)", element.get("transform") or "")
    return (float(match.group(1)), float(match.group(2))) if match else (0.0, 0.0)


def labels_of(root):
    """Every <text> as a label, removing it from the tree; rotated text stays, a halo leaves its outline."""
    rules = class_rules(root)
    labels = []

    def walk(parent, inherited):
        for child in list(parent):
            tag = child.tag.split("}")[-1]
            if tag != "text" and tag != "g":
                continue
            dx, dy = translate_of(child)
            context = dict(inherited)
            context["tx"] += dx
            context["ty"] += dy
            classes = (child.get("class") or "").split()
            text_class = next((c for c in classes if c.startswith("d-")), None)
            if text_class:
                context["kind"] = text_class
            if child.get("text-anchor"):
                context["anchor"] = child.get("text-anchor")
            own_fill = child.get("fill") or style_of(child).get("fill")
            if own_fill:
                context["fill"] = color(own_fill)
            if tag == "g":
                walk(child, context)
                continue
            if "rotate" in (child.get("transform") or ""):
                continue
            kind = context["kind"] or "num"
            size = style_of(child).get("font-size", "")
            labels.append({
                "x": float(child.get("x")) + context["tx"], "y": float(child.get("y")) + context["ty"],
                "anchor": context["anchor"],
                "kind": "num" if kind == "d-num" else kind,
                "size": float(size[:-2]) if size.endswith("px") else rules.get(kind, {}).get("size", 0),
                "color": context["fill"] or rules.get(kind, {}).get("fill") or CLASS_COLORS.get(kind, "#16305c"),
                "text": "".join(child.itertext()),
            })
            if "halo" in classes:
                child.set("style", HALO)
            else:
                parent.remove(child)

    walk(root, {"tx": 0.0, "ty": 0.0, "kind": None, "anchor": "start", "fill": None})
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
    fonts = sorted(glob.glob(os.path.join(FONTS, "*.ttf")))
    png = bytes(resvg_py.svg_to_bytes(svg_string=svg, font_files=fonts, skip_system_fonts=True))
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
