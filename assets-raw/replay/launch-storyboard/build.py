"""Builds one launch variant from the storyboard: python build.py success|glitch|failure [render]

Compares it with the storyboard frames (build/compare_<variant>.png: storyboard left, ours right,
at each frame's time) and, with "render", renders build/launch-<variant>.webm at 1500 kbps.
"""
import io
import json
import os
import subprocess
import re
import sys

import resvg_py
from PIL import Image

import poses
import sheets

HERE = os.path.dirname(os.path.abspath(__file__))
BUILD = os.path.join(HERE, "build")
CLI = r"C:\Program Files\EffectCraft\effectcraft-cli.exe"
VARIANTS = {"success": ("Success", 6.3, [0.0, 1.2, 2.0, 2.8, 4.2, 6.0]),
            "glitch": ("Glitch", 6.7, [0.0, 1.4, 2.2, 3.0, 4.0, 6.4]),
            "failure": ("Failure", 7.6, [0.0, 1.4, 2.2, 3.4, 4.6, 7.0])}
# When each variant can't turn into another any more (ui-spec.md 4.7.3), by hand: the glitch stutters
# from 2.6 and the failure flickers from 3.0, so a success is certain once 3.0 has passed clean; the
# glitch only once the engine catches (3.4); the failure once the engine cuts (3.2).
REVEAL = {"success": 3.2, "glitch": 3.5, "failure": 3.3}


def kink():
    """The glitch trail's points, [[y, x], ...] from the pad up: where the bell went."""
    g = poses.frame("Glitch6.dc.html")["trail"][2]
    d = g.find(poses.NS + "path").get("d")
    pts = [tuple(map(float, p)) for p in re.findall(r"([-\d.]+) ([-\d.]+)", d)]
    return [[y, x] for x, y in pts]


def falls():
    """Each debris piece's fall: the quadratic curve [x0, y0, cx, cy, x1, y1] of its fallPath."""
    f = poses.frame("Failure6.dc.html")
    out = {}
    for i in range(1, 7):
        d = f[f"fallPath-{i}"][2].find(poses.NS + "path").get("d")
        out[f"debris-{i}"] = [float(v) for v in re.findall(r"[-\d.]+", d)]
    return out


def main():
    key = sys.argv[1]
    board, dur, times = VARIANTS[key]
    layers = json.load(open(os.path.join(BUILD, "layers.json")))
    script = os.path.join(BUILD, f"launch-{key}.gen.jsx")
    with open(script, "w", encoding="utf-8") as f:
        f.write("var DIR = " + json.dumps(os.path.join(BUILD, "layers").replace("\\", "/") + "/") + ";\n")
        f.write("var L = " + json.dumps(layers) + ";\n")
        f.write(f"var VARIANT = {json.dumps(board)}, DUR = {dur};\n")
        f.write("var KINK = " + json.dumps(kink()) + ";\n")
        f.write("var FALL = " + json.dumps(falls()) + ";\n")
        f.write(open(os.path.join(HERE, "..", "motion.jsx"), encoding="utf-8").read() + "\n")
        f.write('setup("Launch " + VARIANT, DUR);\n')
        for part in ("lib.jsx", f"{key}.jsx"):
            f.write(open(os.path.join(HERE, part), encoding="utf-8").read() + "\n")
    project = os.path.join(BUILD, f"launch-{key}.ecproj")
    subprocess.run([CLI, "script", script, "--empty", "--save-as", project], check=True)
    rows = []
    for i, t in enumerate(times, 1):
        out = os.path.join(BUILD, f"{key}_{t}.png")
        subprocess.run([CLI, "render-frame", "--project", project, "--comp", "Launch " + board,
                        "--time", str(t), "--max-side", "640", "--out", out], check=True, capture_output=True)
        rows.append((sheets.render(f"{board}{i}.dc.html", 640), Image.open(out).convert("RGB")))
    sheet = Image.new("RGB", (1290, 365 * len(rows)), "#888888")
    for r, (a, b) in enumerate(rows):
        sheet.paste(a, (0, r * 365))
        sheet.paste(b, (650, r * 365))
    sheet.save(os.path.join(BUILD, f"compare_{key}.png"))
    if "publish" in sys.argv:
        # The game's copy: 960 x 540 (about the replay monitor's size at 1080p) and its sidecar.
        out = os.path.join(HERE, "..", "..", "..", "assets", "ui", "replay")
        os.makedirs(out, exist_ok=True)
        subprocess.run([CLI, "render", "--project", project, "--comp", "Launch " + board,
                        "--out", os.path.join(out, f"launch-{key}.webm"), "--format", "webm",
                        "--webm-codec", "vp9", "--audio", "off", "--bitrate", "800",
                        "--resolution", "half"], check=True)
        with open(os.path.join(out, f"launch-{key}.json"), "w") as f:
            json.dump({"duration": dur, "revealAt": REVEAL[key]}, f, indent=1)
            f.write("\n")
    if "render" in sys.argv:
        subprocess.run([CLI, "render", "--project", project, "--comp", "Launch " + board,
                        "--out", os.path.join(BUILD, f"launch-{key}.webm"), "--format", "webm",
                        "--webm-codec", "vp9", "--audio", "off", "--bitrate", "1500"], check=True)


if __name__ == "__main__":
    main()
