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
    # Time in orbit, and the suborbital hop (2026-10-14).
    "advanced-experiments-earth-orbit": {
        "pad": False, "before": ["orbit.jsx"],
        "success": (6.0, 2.4), "glitch": (6.0, 3.6), "failure": (6.0, 3.6),
        "times": [0.5, 1.8, 2.4, 3.2, 4.0, 5.9],
    },
    "biometric-data-transmission": {
        "pad": False, "before": ["orbit.jsx"],
        "success": (6.0, 2.6), "glitch": (6.0, 3.8), "failure": (6.0, 3.8),
        "times": [0.5, 1.8, 2.6, 3.2, 4.4, 5.9],
    },
    "duration-level-i": {
        "pad": False, "before": ["orbit.jsx"], "after": ["duration.jsx"],
        "success": (6.4, 2.6), "glitch": (6.4, 3.8), "failure": (6.4, 3.6),
        "times": [0.5, 2.0, 2.6, 3.4, 4.4, 6.3],
    },
    "duration-level-ii": {
        "pad": False, "before": ["orbit.jsx"], "after": ["duration.jsx"],
        "success": (6.4, 2.6), "glitch": (6.4, 3.8), "failure": (6.4, 3.6),
        "times": [0.5, 2.0, 2.6, 3.4, 4.4, 6.3],
    },
    "suborbital-flight": {
        "pad": False,
        "success": (6.4, 3.4), "glitch": (6.4, 4.0), "failure": (6.4, 3.6),
        "times": [0.5, 1.8, 2.8, 3.4, 4.4, 6.3],
    },
    "recovery": {
        "pad": False,
        "success": (6.4, 2.0), "glitch": (6.4, 3.0), "failure": (6.4, 3.4),
        "times": [0.5, 1.6, 2.6, 3.4, 4.4, 6.3],
    },
    # The way to the planets (2026-10-14): the burn out of Earth orbit, and the cruises on the map.
    "tpi": {
        "pad": False, "before": ["orbit.jsx"],
        "success": (6.0, 2.0), "glitch": (6.0, 3.2), "failure": (6.0, 3.0),
        "times": [0.5, 1.4, 2.0, 2.8, 3.8, 5.9],
    },
    "earth-mars-transit": {
        "pad": False, "after": ["cruise.jsx"],
        "success": (6.4, 2.4), "glitch": (6.4, 3.6), "failure": (6.4, 3.4),
        "times": [0.5, 1.8, 2.4, 3.2, 4.4, 6.3],
    },
    "earth-venus-transit": {
        "pad": False, "after": ["cruise.jsx"],
        "success": (6.4, 2.4), "glitch": (6.4, 3.6), "failure": (6.4, 3.4),
        "times": [0.5, 1.8, 2.4, 3.2, 4.4, 6.3],
    },
    "earth-jupiter-transit": {
        "pad": False, "after": ["cruise.jsx"],
        "success": (6.4, 2.4), "glitch": (6.4, 3.6), "failure": (6.4, 3.4),
        "times": [0.5, 1.8, 2.4, 3.2, 4.4, 6.3],
    },
    "jupiter-saturn-transit": {
        "pad": False, "after": ["cruise.jsx"],
        "success": (6.4, 2.4), "glitch": (6.4, 3.6), "failure": (6.4, 3.4),
        "times": [0.5, 1.8, 2.4, 3.2, 4.4, 6.3],
    },
    # At the planets and the Sun (2026-10-14).
    "venus-orbit": {
        "pad": False, "before": ["orbit.jsx"], "after": ["planet-orbit.jsx"],
        "success": (6.0, 2.4), "glitch": (6.0, 3.6), "failure": (6.0, 3.6),
        "times": [0.5, 1.8, 2.4, 3.2, 4.2, 5.9],
    },
    "mars-orbit": {
        "pad": False, "before": ["orbit.jsx"], "after": ["planet-orbit.jsx"],
        "success": (6.0, 2.4), "glitch": (6.0, 3.6), "failure": (6.0, 3.6),
        "times": [0.5, 1.8, 2.4, 3.2, 4.2, 5.9],
    },
    "sun-measurements": {
        "pad": False,
        "success": (6.0, 2.4), "glitch": (6.0, 3.6), "failure": (6.0, 3.6),
        "times": [0.5, 1.8, 2.4, 3.2, 4.2, 5.9],
    },
    # The Moon (2026-10-14).
    "lunar-orbit": {
        "pad": False, "before": ["orbit.jsx"], "after": ["planet-orbit.jsx"],
        "success": (6.0, 2.4), "glitch": (6.0, 3.6), "failure": (6.0, 3.6),
        "times": [0.5, 1.8, 2.4, 3.2, 4.2, 5.9],
    },
    "loi": {
        "pad": False, "before": ["orbit.jsx"],
        "success": (6.4, 2.5), "glitch": (6.4, 2.8), "failure": (6.4, 2.8),
        "times": [0.6, 1.2, 1.8, 2.5, 3.6, 6.3],
    },
    "tei": {
        "pad": False, "before": ["orbit.jsx"],
        "success": (6.0, 2.4), "glitch": (6.0, 3.0), "failure": (6.0, 3.0),
        "times": [0.5, 1.4, 2.0, 2.8, 3.8, 5.9],
    },
    "lunar-impact": {
        "pad": False, "before": ["orbit.jsx"],
        "success": (6.0, 2.2), "glitch": (6.0, 3.0), "failure": (6.0, 3.2),
        "times": [0.5, 1.6, 2.4, 3.2, 4.1, 5.9],
    },
    "lunar-approach": {
        "pad": False, "before": ["orbit.jsx"],
        "success": (6.0, 2.4), "glitch": (6.0, 3.6), "failure": (6.0, 3.4),
        "times": [0.5, 1.8, 2.4, 3.2, 4.2, 5.9],
    },
    "lunar-flyby": {
        "pad": False, "before": ["orbit.jsx"],
        "success": (6.4, 3.6), "glitch": (6.4, 4.4), "failure": (6.4, 4.4),
        "times": [0.5, 2.0, 3.0, 3.6, 4.4, 6.3],
    },
    "trans-lunar-coast": {
        "pad": False, "after": ["moon-coast.jsx"],
        "success": (6.4, 2.4), "glitch": (6.4, 3.6), "failure": (6.4, 3.4),
        "times": [0.5, 1.8, 2.4, 3.2, 4.4, 6.3],
    },
    "trans-earth-coast": {
        "pad": False, "after": ["moon-coast.jsx"],
        "success": (6.4, 2.4), "glitch": (6.4, 3.6), "failure": (6.4, 3.4),
        "times": [0.5, 1.8, 2.4, 3.2, 4.4, 6.3],
    },
    "earth-approach": {
        "pad": False, "before": ["orbit.jsx"],
        "success": (6.0, 2.4), "glitch": (6.0, 3.4), "failure": (6.0, 3.4),
        "times": [0.5, 1.8, 2.6, 3.4, 4.4, 5.9],
    },
    # The way down, split (2026-10-14): the chutes, then the touchdown and the recovery.
    "descent-chutes": {
        "pad": False, "after": ["descent.jsx"],
        "success": (6.0, 2.4), "glitch": (6.0, 3.4), "failure": (6.0, 2.6),
        "times": [0.5, 1.4, 2.2, 3.0, 4.2, 5.9],
    },
    "landing-and-recovery": {
        "pad": False, "after": ["touchdown.jsx"],
        "success": (6.0, 1.2), "glitch": (6.0, 1.8), "failure": (6.0, 1.8),
        "times": [0.5, 1.2, 1.8, 2.6, 3.6, 5.9],
    },
    "splashdown-and-recovery": {
        "pad": False, "after": ["touchdown.jsx"],
        "success": (6.0, 1.2), "glitch": (6.0, 1.8), "failure": (6.0, 1.8),
        "times": [0.5, 1.2, 1.8, 2.6, 3.6, 5.9],
    },
    "insertion": {
        "pad": False, "before": ["orbit.jsx"],
        "success": (6.0, 2.0), "glitch": (6.0, 3.0), "failure": (6.0, 3.0),
        "times": [0.5, 1.4, 2.0, 2.8, 4.0, 5.9],
    },
    # Rendezvous and docking (2026-10-14): closing in on the orbit, then the close-up (dock.jsx).
    "rendezvous-target-approach": {
        "pad": False, "before": ["orbit.jsx"],
        "success": (6.0, 2.6), "glitch": (6.0, 3.0), "failure": (6.0, 3.0),
        "times": [0.5, 1.8, 2.4, 2.8, 4.0, 5.9],
    },
    "rendezvous": {
        "pad": False, "before": ["dock.jsx"],
        "success": (6.0, 2.4), "glitch": (6.0, 3.4), "failure": (6.0, 3.4),
        "times": [0.5, 1.6, 2.4, 3.2, 4.2, 5.9],
    },
    "target-approach": {
        "pad": False, "before": ["dock.jsx"],
        "success": (6.4, 2.4), "glitch": (6.4, 2.6), "failure": (6.4, 2.6),
        "times": [0.5, 1.9, 2.3, 2.7, 3.6, 6.3],
    },
    "docking": {
        "pad": False, "before": ["dock.jsx"],
        "success": (6.4, 2.0), "glitch": (6.4, 3.4), "failure": (6.4, 3.4),
        "times": [0.5, 1.7, 2.3, 3.1, 4.2, 6.3],
    },
    "undocking": {
        "pad": False, "before": ["dock.jsx"],
        "success": (6.4, 1.2), "glitch": (6.4, 2.8), "failure": (6.4, 2.8),
        "times": [0.5, 1.2, 2.0, 2.6, 3.8, 6.3],
    },
    # Spacewalks (2026-10-14): the way home's capsule with a hatch, and a figure on a tether (spacewalk.jsx).
    "eva": {
        "pad": False, "before": ["spacewalk.jsx"],
        "success": (6.0, 2.4), "glitch": (6.8, 3.0), "failure": (6.4, 3.0),
        "times": [1.2, 2.0, 2.4, 2.8, 3.9, 5.9],
    },
    "crew-transfer": {
        "pad": False, "before": ["spacewalk.jsx"],
        "success": (5.6, 2.6), "glitch": (6.6, 3.2), "failure": (6.0, 3.2),
        "times": [1.2, 2.0, 2.6, 3.0, 4.0, 5.5],
    },
    # On the Moon (2026-10-14): the descent from low orbit and the landing (moon.jsx).
    "powered-descent": {
        "pad": False, "before": ["moon.jsx"],
        "success": (6.4, 2.4), "glitch": (6.4, 3.4), "failure": (6.4, 3.2),
        "times": [0.5, 2.0, 2.6, 3.4, 4.6, 6.3],
    },
    "lunar-landing": {
        "pad": False, "before": ["moon.jsx"],
        "success": (6.0, 2.6), "glitch": (6.0, 3.6), "failure": (6.0, 3.2),
        "times": [0.5, 1.6, 2.4, 3.0, 3.6, 5.9],
    },
    "lunar-ascent": {
        "pad": False, "before": ["orbit.jsx"],
        "success": (6.0, 1.8), "glitch": (6.0, 2.6), "failure": (6.0, 2.8),
        "times": [0.5, 1.2, 1.8, 2.6, 3.4, 5.9],
    },
    "lunar-liftoff": {
        "pad": False, "before": ["moon.jsx"],
        "success": (5.6, 1.0), "glitch": (5.6, 2.0), "failure": (5.6, 2.8),
        "times": [0.7, 1.2, 1.8, 2.6, 3.4, 5.5],
    },
    "moonwalk": {
        "pad": False, "before": ["moon.jsx"],
        "success": (5.4, 2.6), "glitch": (5.8, 3.2), "failure": (5.8, 3.0),
        "times": [0.6, 1.2, 2.2, 2.6, 3.4, 5.3],
    },
    # Apollo's transposition and extraction (2026-10-14): the third stage and its adapter (apollo.jsx).
    "transposition": {
        "pad": False, "before": ["dock.jsx", "moon.jsx", "apollo.jsx"],
        "success": (6.0, 3.0), "glitch": (6.4, 4.6), "failure": (6.4, 4.6),
        "times": [0.6, 1.6, 2.6, 3.4, 4.6, 5.9],
    },
    "extraction": {
        "pad": False, "before": ["dock.jsx", "moon.jsx", "apollo.jsx"],
        "success": (5.6, 1.2), "glitch": (5.6, 2.2), "failure": (5.6, 2.6),
        "times": [0.5, 1.2, 1.9, 2.6, 3.6, 5.5],
    },
    # A spaceplane's runway landing (2026-10-14).
    "runway-landing": {
        "pad": False, "before": ["moon.jsx"],
        "success": (6.0, 2.6), "glitch": (6.4, 3.2), "failure": (6.0, 3.2),
        "times": [0.8, 2.0, 2.6, 3.2, 4.0, 5.9],
    },
    # The planets' approaches, flybys and orbit insertion (2026-10-14): the Moon's, round the planet.
    "venus-approach": {
        "pad": False, "before": ["orbit.jsx"], "after": ["lunar-approach.jsx"],
        "success": (6.0, 2.4), "glitch": (6.0, 3.6), "failure": (6.0, 3.4),
        "times": [0.5, 1.8, 2.4, 3.2, 4.2, 5.9],
    },
    "mars-approach": {
        "pad": False, "before": ["orbit.jsx"], "after": ["lunar-approach.jsx"],
        "success": (6.0, 2.4), "glitch": (6.0, 3.6), "failure": (6.0, 3.4),
        "times": [0.5, 1.8, 2.4, 3.2, 4.2, 5.9],
    },
    "jupiter-approach": {
        "pad": False, "before": ["orbit.jsx"], "after": ["lunar-approach.jsx"],
        "success": (6.0, 2.4), "glitch": (6.0, 3.6), "failure": (6.0, 3.4),
        "times": [0.5, 1.8, 2.4, 3.2, 4.2, 5.9],
    },
    "saturn-approach": {
        "pad": False, "before": ["orbit.jsx"], "after": ["lunar-approach.jsx"],
        "success": (6.0, 2.4), "glitch": (6.0, 3.6), "failure": (6.0, 3.4),
        "times": [0.5, 1.8, 2.4, 3.2, 4.2, 5.9],
    },
    "venus-flyby": {
        "pad": False, "before": ["orbit.jsx"], "after": ["lunar-flyby.jsx"],
        "success": (6.4, 3.6), "glitch": (6.4, 4.4), "failure": (6.4, 4.4),
        "times": [0.5, 2.0, 3.0, 3.6, 4.4, 6.3],
    },
    "mars-flyby": {
        "pad": False, "before": ["orbit.jsx"], "after": ["lunar-flyby.jsx"],
        "success": (6.4, 3.6), "glitch": (6.4, 4.4), "failure": (6.4, 4.4),
        "times": [0.5, 2.0, 3.0, 3.6, 4.4, 6.3],
    },
    "jupiter-flyby": {
        "pad": False, "before": ["orbit.jsx"], "after": ["lunar-flyby.jsx"],
        "success": (6.4, 3.6), "glitch": (6.4, 4.4), "failure": (6.4, 4.4),
        "times": [0.5, 2.0, 3.0, 3.6, 4.4, 6.3],
    },
    "saturn-flyby": {
        "pad": False, "before": ["orbit.jsx"], "after": ["lunar-flyby.jsx"],
        "success": (6.4, 3.6), "glitch": (6.4, 4.4), "failure": (6.4, 4.4),
        "times": [0.5, 2.0, 3.0, 3.6, 4.4, 6.3],
    },
    "mercury-flyby": {
        "pad": False, "before": ["orbit.jsx"], "after": ["lunar-flyby.jsx"],
        "success": (6.4, 3.6), "glitch": (6.4, 4.4), "failure": (6.4, 4.4),
        "times": [0.5, 2.0, 3.0, 3.6, 4.4, 6.3],
    },
    "mars-orbit-injection": {
        "pad": False, "before": ["orbit.jsx"], "after": ["loi.jsx"],
        "success": (6.4, 2.5), "glitch": (6.4, 2.8), "failure": (6.4, 2.8),
        "times": [0.6, 1.2, 1.8, 2.5, 3.6, 6.3],
    },
    # Probe landers on the planets (2026-10-14): the bus, the landing, the work on the surface (probe.jsx).
    "probe-landing": {
        "pad": False, "before": ["orbit.jsx", "moon.jsx", "probe.jsx"],
        "success": (6.0, 2.6), "glitch": (6.0, 3.2), "failure": (6.0, 3.2),
        "times": [0.8, 1.8, 2.4, 3.0, 3.8, 5.9],
    },
    "probe-surface": {
        "pad": False, "before": ["orbit.jsx", "moon.jsx", "probe.jsx"],
        "success": (6.0, 2.2), "glitch": (6.0, 2.8), "failure": (6.0, 2.6),
        "times": [0.8, 1.6, 2.2, 2.8, 3.6, 5.9],
    },
    "probe-release": {
        "pad": False,
        "success": (5.6, 1.2), "glitch": (5.6, 2.2), "failure": (5.6, 2.4),
        "times": [0.5, 1.2, 1.9, 2.6, 3.6, 5.5],
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
        # A scene shared by several steps, before the step's own file uses it.
        for extra in STEPS[step].get("before", []):
            f.write(read(extra))
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
