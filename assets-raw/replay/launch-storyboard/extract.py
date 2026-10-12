"""Cuts every part out of the storyboard frames into a layer PNG at its world position.

Writes build/layers/<id>.png (cropped to the part) and build/layers.json:
{id: {"x", "y", "w", "h", "px", "py"}} in world px, pivot from the part's rotate() centre.
The trail is per variant (trail-Success, ...): taken from the variant's last frame, undashed.
"""
import io
import json
import os
import re
import xml.etree.ElementTree as ET

import resvg_py
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
P = os.path.join(HERE, "project")
OUT = os.path.join(HERE, "build", "layers")
FONTS = os.path.normpath(os.path.join(HERE, "..", "..", "..", "assets", "fonts"))
SVGNS = "http://www.w3.org/2000/svg"
NS = "{" + SVGNS + "}"
ET.register_namespace("", SVGNS)
# The world canvas a part is drawn on before cropping.
WX, WY, WW, WH = -200, -1600, 2320, 2900
VARIANTS = ("Success", "Glitch", "Failure")
SKIP = {"flame", "cameraGuide"}


def root_of(name):
    html = open(os.path.join(P, name), encoding="utf-8").read()
    return ET.fromstring(re.search(r"<svg.*?</svg>", html, re.S).group(0))


def pivot(g):
    m = re.search(r"rotate\([-\d.]+ ([-\d.]+) ([-\d.]+)\)", g.get("transform", ""))
    return (float(m.group(1)), float(m.group(2))) if m else (0.0, 0.0)


def rasterize(defs, g, name, screen=False):
    inner = "".join(ET.tostring(c, encoding="unicode") for c in g)
    vb = (0, 0, 1920, 1080) if screen else (WX, WY, WW, WH)
    svg = (f'<svg xmlns="{SVGNS}" width="{vb[2]}" height="{vb[3]}" viewBox="{" ".join(map(str, vb))}">'
           f'{defs}{inner}</svg>')
    png = resvg_py.svg_to_bytes(svg_string=svg, width=vb[2], height=vb[3], font_dirs=[FONTS])
    im = Image.open(io.BytesIO(bytes(png))).convert("RGBA")
    box = (0, 0, vb[2], vb[3]) if screen else im.getchannel("A").getbbox()
    if box is None:
        raise SystemExit(f"{name}: nothing drawn")
    if not screen:
        box = (max(0, box[0] - 2), max(0, box[1] - 2), min(vb[2], box[2] + 2), min(vb[3], box[3] + 2))
    im.crop(box).save(os.path.join(OUT, name + ".png"))
    return {"x": box[0] + vb[0], "y": box[1] + vb[1], "w": box[2] - box[0], "h": box[3] - box[1]}


def main():
    os.makedirs(OUT, exist_ok=True)
    layers = {}
    frames = [(v, i) for v in VARIANTS for i in range(1, 7)]
    for v, i in frames:
        root = root_of(f"{v}{i}.dc.html")
        defs = "".join(ET.tostring(d, encoding="unicode") for d in root.iter(NS + "defs"))
        for g in root.iter(NS + "g"):
            gid = g.get("id")
            if not gid or gid in SKIP:
                continue
            name = gid
            if gid == "trail":
                if i != 6:
                    continue
                name = f"trail-{v}"
                for p in g.iter(NS + "path"):
                    p.attrib.pop("stroke-dasharray", None)
            if name in layers:
                continue
            info = rasterize(defs, g, name, screen=(gid == "paper"))
            info["px"], info["py"] = pivot(g)
            if gid.startswith("flame-"):
                info["px"], info["py"] = 960.0, 840.0  # they flicker about the bell
            layers[name] = info
    with open(os.path.join(HERE, "build", "layers.json"), "w") as f:
        json.dump(layers, f, indent=1)
    print(len(layers), "layers:", " ".join(sorted(layers)))


if __name__ == "__main__":
    main()
