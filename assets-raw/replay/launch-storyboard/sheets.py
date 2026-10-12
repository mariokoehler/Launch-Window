"""Renders the storyboard frames (the SVG inside each .dc.html) into one contact sheet per variant."""
import io
import os
import re

import resvg_py
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
P = os.path.join(HERE, "project")
FONTS = os.path.normpath(os.path.join(HERE, "..", "..", "..", "assets", "fonts"))


def svg_of(name):
    html = open(os.path.join(P, name), encoding="utf-8").read()
    return re.search(r"<svg.*?</svg>", html, re.S).group(0)


def render(name, w):
    s = svg_of(name)
    png = resvg_py.svg_to_bytes(svg_string=s, width=w, font_dirs=[FONTS])
    return Image.open(io.BytesIO(bytes(png))).convert("RGB")


if __name__ == "__main__":
    for variant in ("Success", "Glitch", "Failure"):
        ims = [render(f"{variant}{i}.dc.html", 640) for i in range(1, 7)]
        sheet = Image.new("RGB", (640 * 3 + 20, 360 * 2 + 10), "#888888")
        for i, im in enumerate(ims):
            sheet.paste(im, ((i % 3) * 650, (i // 3) * 370))
        sheet.save(os.path.join(HERE, f"sheet_{variant}.png"))
    print("ok")
