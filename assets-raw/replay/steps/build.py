"""Builds one step clip in its three variants: python build.py <step> [render|publish]

Writes build/<step>-<variant>.ecproj and build/sheet_<step>.png (the variants side by side, a row per
time in TIMES), which is the thing to look at. "render" renders build/<step>-<variant>.webm at full
size; "publish" writes the game's copies, 960 x 540, and their sidecars to assets/ui/replay/.
Run parts.py first (it draws the layers).
"""
import json
import os
import subprocess
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
BUILD = os.path.join(HERE, "build")
CLI = r"C:\Program Files\EffectCraft\effectcraft-cli.exe"
GAME = os.path.normpath(os.path.join(HERE, "..", "..", "..", "assets", "ui", "replay"))
VARIANTS = ("success", "glitch", "failure")

# Per step: whether it's on the pad (it then draws the launch's scene), each variant's length and
# the second its result may show (ui-spec.md 4.7.3: from then on it can't turn into another
# variant), and the times the sheet shows.
STEPS = {
    "launch-preparations": {
        "pad": True,
        "success": (6.4, 2.6), "glitch": (6.4, 3.5), "failure": (7.0, 3.1),
        "times": [0.5, 2.0, 2.8, 3.4, 4.6, 6.4],
    },
    "countdown": {
        "pad": True,
        "success": (6.4, 4.8), "glitch": (7.2, 3.6), "failure": (7.0, 4.7),
        "times": [0.0, 2.2, 3.0, 4.4, 4.8, 6.4],
    },
    "ascent": {
        "pad": False,
        "success": (6.5, 3.6), "glitch": (6.5, 4.0), "failure": (7.0, 3.4),
        "times": [0.0, 1.5, 2.9, 3.4, 4.2, 6.4],
    },
    "deployment": {
        "pad": False,
        "success": (6.0, 2.6), "glitch": (6.0, 2.6), "failure": (6.5, 2.3),
        "times": [0.5, 1.2, 1.9, 2.4, 3.6, 5.9],
    },
    "earth-orbit": {
        "pad": False,
        "success": (6.0, 2.1), "glitch": (6.0, 3.4), "failure": (6.5, 3.0),
        "times": [0.5, 2.0, 2.8, 3.6, 4.6, 5.9],
    },
    # Coming home (2026-10-14): the orbital order, the capsule centred in all of them.
    "reentry-preparations": {
        "pad": False,
        "success": (6.0, 3.0), "glitch": (6.0, 3.6), "failure": (6.5, 3.4),
        "times": [0.5, 1.6, 2.4, 3.2, 4.4, 5.9],
    },
    "retrofire": {
        "pad": False,
        "success": (6.4, 2.6), "glitch": (6.4, 3.0), "failure": (6.4, 3.0),
        "times": [0.5, 1.4, 2.0, 2.8, 3.8, 6.3],
    },
    "retrofire-jettison": {
        "pad": False,
        "success": (5.6, 1.6), "glitch": (5.6, 3.4), "failure": (6.4, 3.2),
        "times": [0.5, 1.1, 1.8, 2.6, 3.8, 5.5],
    },
    # Vostok's way (2026-10-14): the module instead of the jettison, and a landing instead of the sea.
    "modules-separation": {
        "pad": False,
        "success": (5.6, 1.8), "glitch": (5.6, 3.6), "failure": (6.4, 3.8),
        "times": [0.5, 1.0, 1.6, 2.6, 3.6, 5.5],
    },
    "descent-and-landing": {
        "pad": False, "after": ["descent.jsx"],
        "success": (6.4, 2.4), "glitch": (6.4, 3.4), "failure": (6.4, 2.6),
        "times": [0.5, 1.4, 2.2, 3.0, 4.2, 6.3],
    },
    "reentry": {
        "pad": False,
        "success": (6.4, 4.0), "glitch": (6.4, 4.4), "failure": (6.4, 3.4),
        "times": [0.4, 1.6, 2.6, 3.4, 4.6, 6.3],
    },
    "descent-and-splashdown": {
        "pad": False, "after": ["descent.jsx"],
        "success": (6.4, 2.4), "glitch": (6.4, 3.4), "failure": (6.4, 2.6),
        "times": [0.5, 1.4, 2.2, 3.0, 4.2, 6.3],
    },
}


def read(*path):
    return open(os.path.join(HERE, *path), encoding="utf-8").read() + "\n"


def project(step, variant):
    dur = STEPS[step][variant][0]
    script = os.path.join(BUILD, f"{step}-{variant}.gen.jsx")
    with open(script, "w", encoding="utf-8") as f:
        f.write("var DIR = " + json.dumps(os.path.join(BUILD, "layers").replace("\\", "/") + "/") + ";\n")
        f.write("var L = " + open(os.path.join(BUILD, "layers.json")).read() + ";\n")
        f.write(f"var VARIANT = {json.dumps(variant)}, DUR = {dur};\n")
        f.write(read("..", "motion.jsx"))
        f.write(f"setup({json.dumps(step + ' ' + variant)}, DUR);\n")
        if STEPS[step]["pad"]:
            f.write(read("..", "launch-storyboard", "lib.jsx"))
            f.write(read("pad.jsx"))
        f.write(read(f"{step}.jsx"))
        # A timeline shared by several steps, after the step's own file has set its switches.
        for extra in STEPS[step].get("after", []):
            f.write(read(extra))
    out = os.path.join(BUILD, f"{step}-{variant}.ecproj")
    subprocess.run([CLI, "script", script, "--empty", "--save-as", out], check=True, capture_output=True)
    return out


def main():
    step = sys.argv[1]
    spec = STEPS[step]
    projects = {v: project(step, v) for v in VARIANTS}
    rows = []
    for t in spec["times"]:
        row = []
        for v in VARIANTS:
            out = os.path.join(BUILD, f"{step}-{v}_{t}.png")
            if t <= spec[v][0]:
                # The last frame starts one frame before the end.
                subprocess.run([CLI, "render-frame", "--project", projects[v], "--comp", f"{step} {v}",
                                "--time", str(min(t, round(spec[v][0] - 1 / 30, 3))), "--max-side", "640", "--out", out], check=True, capture_output=True)
                row.append(Image.open(out).convert("RGB"))
            else:
                row.append(Image.new("RGB", (640, 360), "#dddddd"))
        rows.append((t, row))
    sheet = Image.new("RGB", (3 * 650 + 60, 370 * len(rows) + 30), "#888888")
    draw = ImageDraw.Draw(sheet)
    for i, v in enumerate(VARIANTS):
        draw.text((60 + i * 650, 8), f"{v}  ({spec[v][0]} s, reveal {spec[v][1]})", fill="#ffffff")
    for r, (t, row) in enumerate(rows):
        draw.text((8, 30 + r * 370 + 170), f"{t} s", fill="#ffffff")
        for i, im in enumerate(row):
            sheet.paste(im, (60 + i * 650, 30 + r * 370))
    sheet.save(os.path.join(BUILD, f"sheet_{step}.png"))
    for v in VARIANTS:
        if "render" in sys.argv:
            subprocess.run([CLI, "render", "--project", projects[v], "--comp", f"{step} {v}",
                            "--out", os.path.join(BUILD, f"{step}-{v}.webm"), "--format", "webm",
                            "--webm-codec", "vp9", "--audio", "off", "--bitrate", "1500"], check=True)
        if "publish" in sys.argv:
            # The game's copy: 960 x 540 (about the replay monitor's size at 1080p) and its sidecar.
            subprocess.run([CLI, "render", "--project", projects[v], "--comp", f"{step} {v}",
                            "--out", os.path.join(GAME, f"{step}-{v}.webm"), "--format", "webm",
                            "--webm-codec", "vp9", "--audio", "off", "--bitrate", "800",
                            "--resolution", "half"], check=True, capture_output=True)
            with open(os.path.join(GAME, f"{step}-{v}.json"), "w") as f:
                json.dump({"duration": spec[v][0], "revealAt": spec[v][1]}, f, indent=1)
                f.write("\n")
    if "publish" in sys.argv:
        # The sound cues aren't the render's: put them back in the sidecars just written.
        sys.path.insert(0, os.path.join(HERE, ".."))
        import cues
        cues.apply()
    print(os.path.join(BUILD, f"sheet_{step}.png"))


if __name__ == "__main__":
    main()
