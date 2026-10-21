"""Builds one launch variant from the storyboard: python build.py success|glitch|failure [render] [a skin in SKINS]

Compares it with the storyboard frames (build/compare_<variant>.png: storyboard left, ours right,
at each frame's time) and, with "render", renders build/launch-<variant>.webm at 1500 kbps.
"mercury" (2026-10-17): Project Mercury's launch, its capsule and escape tower on the rocket (the replay
steps' mc- parts, ../steps/parts.py), published as mercury-launch-<variant>; "gemini" and "agena" the same
with Gemini (gm-) and the Agena target vehicle (ag-), as SKINS lists.
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
# A craft's own launch: its parts' prefix in the steps' layers, the clip's name, and any parts in place of the
# prefix's (2026-10-18: the lunar Gemini programs' stacks, Gemini's skin with the stack on the rocket).
SKINS = {"mercury": ("mc-", "mercury-launch"), "gemini": ("gm-", "gemini-launch"), "agena": ("ag-", "agena-launch"),
         "transtage": ("tt-", "transtage-launch"), "vostok": ("vk-", "vostok-launch"),
         "voskhod": ("vk-", "voskhod-launch", {"rocket": "vh-rocket", "debris-1": "vh-debris-1"}),
         "soyuz": ("sy-", "soyuz-launch"), "apollo": ("ap-", "apollo-launch"),
         "sputnik-1": ("sp1-", "sputnik-1-launch"), "explorer-1": ("ex1-", "explorer-1-launch"),
         "sputnik-2": ("sp2-", "sputnik-2-launch"), "ofo": ("of-", "ofo-launch"),
         "biosatellite": ("bs-", "biosatellite-launch")}
SKINS["soyuz-l1"] = ("sy-", "soyuz-l1-launch", {"rocket": "s1-rocket", "debris-1": "s1-debris-1"})
SKINS["soyuz-lok"] = ("sy-", "soyuz-lok-launch", {"rocket": "lok-rocket", "debris-1": "lok-debris-1"})
SKINS["lk"] = ("lkl-", "lk-launch")
SKINS["lk-700"] = ("l7-", "lk-700-launch")
SKINS["gemini-eor"] = ("gm-", "gemini-eor-launch", {"rocket": "ge-rocket", "debris-1": "ge-debris-1"})
SKINS["tug"] = ("sv-", "tug-launch")
SKINS["apollo-lm"] = ("ap-", "apollo-lm-launch", {"rocket": "a5-rocket", "debris-1": "a5-debris-1"})
SKINS["gemini-eor-empty"] = ("gm-", "gemini-eor-empty-launch", {"rocket": "ge-rocket-empty", "debris-1": "ge-debris-1-empty"})
for _key, _prefix in (("gemini-transtage", "gt"), ("gemini-lpm", "gl"), ("gemini-da", "gd")):
    SKINS[_key] = ("gm-", f"{_key}-launch", {"rocket": f"{_prefix}-rocket", "debris-1": f"{_prefix}-debris-1"})


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
    # A craft's own draws from the steps' layers (the storyboard's are among them), each part as its own.
    skin = next((SKINS[a] for a in sys.argv if a in SKINS), None)
    src = os.path.join(HERE, "..", "steps", "build") if skin else BUILD
    name = skin[1] if skin else "launch"
    layers = json.load(open(os.path.join(src, "layers.json")))
    script = os.path.join(BUILD, f"{name}-{key}.gen.jsx")
    with open(script, "w", encoding="utf-8") as f:
        f.write("var DIR = " + json.dumps(os.path.join(src, "layers").replace("\\", "/") + "/") + ";\n")
        f.write("var L = " + json.dumps(layers) + ";\n")
        if skin:
            alias = {n[len(skin[0]):]: n for n in layers if n.startswith(skin[0])}
            alias.update(skin[2] if len(skin) > 2 else {})
            f.write("var ALIAS = " + json.dumps(alias) + ";\n")
        f.write(f"var VARIANT = {json.dumps(board)}, DUR = {dur};\n")
        f.write("var KINK = " + json.dumps(kink()) + ";\n")
        f.write("var FALL = " + json.dumps(falls()) + ";\n")
        f.write(open(os.path.join(HERE, "..", "motion.jsx"), encoding="utf-8").read() + "\n")
        f.write('setup("Launch " + VARIANT, DUR);\n')
        for part in ("lib.jsx", f"{key}.jsx"):
            f.write(open(os.path.join(HERE, part), encoding="utf-8").read() + "\n")
    project = os.path.join(BUILD, f"{name}-{key}.ecproj")
    subprocess.run([CLI, "script", script, "--empty", "--save-as", project], check=True)
    rows = []
    for i, t in enumerate(times, 1):
        # Named for the clip, so skins built side by side don't overwrite each other's frames.
        out = os.path.join(BUILD, f"{name}-{key}_{t}.png")
        subprocess.run([CLI, "render-frame", "--project", project, "--comp", "Launch " + board,
                        "--time", str(t), "--max-side", "640", "--out", out], check=True, capture_output=True)
        rows.append((sheets.render(f"{board}{i}.dc.html", 640), Image.open(out).convert("RGB")))
    sheet = Image.new("RGB", (1290, 365 * len(rows)), "#888888")
    for r, (a, b) in enumerate(rows):
        sheet.paste(a, (0, r * 365))
        sheet.paste(b, (650, r * 365))
    sheet.save(os.path.join(BUILD, f"compare_{name}-{key}.png" if skin else f"compare_{key}.png"))
    if "publish" in sys.argv:
        # The game's copy: 960 x 540 (about the replay monitor's size at 1080p) and its sidecar.
        out = os.path.join(HERE, "..", "..", "..", "assets", "ui", "replay")
        os.makedirs(out, exist_ok=True)
        subprocess.run([CLI, "render", "--project", project, "--comp", "Launch " + board,
                        "--out", os.path.join(out, f"{name}-{key}.webm"), "--format", "webm",
                        "--webm-codec", "vp9", "--audio", "off", "--bitrate", "800",
                        "--resolution", "half"], check=True)
        with open(os.path.join(out, f"{name}-{key}.json"), "w") as f:
            json.dump({"duration": dur, "revealAt": REVEAL[key]}, f, indent=1)
            f.write("\n")
        # The sound cues aren't the render's: put them back in the sidecar just written (not with "nocues": publish.py
        # does it once after a batch).
        if "nocues" not in sys.argv:
            sys.path.insert(0, os.path.join(HERE, ".."))
            import cues
            cues.apply()
    if "render" in sys.argv:
        subprocess.run([CLI, "render", "--project", project, "--comp", "Launch " + board,
                        "--out", os.path.join(BUILD, f"{name}-{key}.webm"), "--format", "webm",
                        "--webm-codec", "vp9", "--audio", "off", "--bitrate", "1500"], check=True)


if __name__ == "__main__":
    main()
