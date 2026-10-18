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
    # Coming home (2026-10-14): the orbital order, the capsule centred in all of them. Mirrored
    # (2026-10-16): drawn travelling right, they play travelling left, as the payload does over the top
    # of the Earth orbit's map (anticlockwise).
    "reentry-preparations": {
        "pad": False, "mirror": True, "before": ["motes.jsx"],
        "success": (6.0, 3.0), "glitch": (6.0, 3.6), "failure": (6.5, 3.4),
        "times": [0.5, 1.6, 2.4, 3.2, 4.4, 5.9],
    },
    # Reentry preparations with the craft already base first (2026-10-17): after a suborbital flight's turn,
    # or after the retrofire (-burn). No second turn: it holds, trims, and Mercury's periscope goes in.
    "reentry-trim": {
        "pad": False, "mirror": True, "before": ["motes.jsx"],
        "success": (6.0, 3.0), "glitch": (6.0, 3.4), "failure": (6.0, 3.0),
        "times": [0.5, 1.6, 2.4, 3.0, 4.0, 5.9],
    },
    "reentry-trim-burn": {
        "pad": False, "mirror": True, "before": ["motes.jsx"], "after": ["reentry-trim.jsx"],
        "success": (6.0, 3.0), "glitch": (6.0, 3.4), "failure": (6.0, 3.0),
        "times": [0.5, 1.6, 2.4, 3.0, 4.0, 5.9],
    },
    "retrofire": {
        "pad": False, "mirror": True, "before": ["motes.jsx"],
        "success": (6.4, 2.6), "glitch": (6.4, 3.0), "failure": (6.4, 3.0),
        "times": [0.5, 1.4, 2.0, 2.8, 3.8, 6.3],
    },
    # A burn with no reentry preparations before it (2026-10-16): the turn first, then the retrofire,
    # 1.2 s later (deorbit-burn.jsx sets TURN for retrofire.jsx).
    "deorbit-burn": {
        "pad": False, "mirror": True, "before": ["motes.jsx"], "after": ["retrofire.jsx"],
        "success": (7.6, 3.8), "glitch": (7.6, 4.2), "failure": (7.6, 4.2),
        "times": [0.3, 1.0, 1.8, 2.6, 4.0, 7.5],
    },
    "retrofire-jettison": {
        "pad": False, "mirror": True, "before": ["motes.jsx"],
        "success": (5.6, 1.6), "glitch": (5.6, 3.4), "failure": (6.4, 3.2),
        "times": [0.5, 1.1, 1.8, 2.6, 3.8, 5.5],
    },
    # A Soyuz-type craft (2026-10-16): its orbital module on the nose through the burn and the jettison,
    # then dropped in the modules separation.
    "deorbit-burn-om": {
        "pad": False, "mirror": True, "before": ["motes.jsx"], "after": ["retrofire.jsx"],
        "success": (7.6, 3.8), "glitch": (7.6, 4.2), "failure": (7.6, 4.2),
        "times": [0.3, 1.0, 1.8, 2.6, 4.0, 7.5],
    },
    "retrofire-jettison-om": {
        "pad": False, "mirror": True, "before": ["motes.jsx"], "after": ["retrofire-jettison.jsx"],
        "success": (5.6, 1.6), "glitch": (5.6, 3.4), "failure": (6.4, 3.2),
        "times": [0.5, 1.1, 1.8, 2.6, 3.8, 5.5],
    },
    # The LK-700's hop and the engine test after it, the orbital module on the nose.
    "suborbital-flight-om": {
        "pad": False, "mirror": True, "before": ["motes.jsx"], "after": ["suborbital-flight.jsx"],
        "success": (6.4, 3.4), "glitch": (6.4, 4.0), "failure": (6.4, 3.6),
        "times": [0.5, 1.8, 2.8, 3.4, 4.4, 6.3],
    },
    "retrofire-om": {
        "pad": False, "mirror": True, "before": ["motes.jsx"], "after": ["retrofire.jsx"],
        "success": (6.4, 2.6), "glitch": (6.4, 3.0), "failure": (6.4, 3.0),
        "times": [0.5, 1.4, 2.0, 2.8, 3.8, 6.3],
    },
    # Vostok's "retrofire jettison" (2026-10-16): it had no pack to drop, so the burn's end instead.
    "retro-shutdown": {
        "pad": False, "mirror": True, "before": ["motes.jsx"],
        "success": (4.4, 1.4), "glitch": (4.4, 2.8), "failure": (5.2, 3.0),
        "times": [0.2, 0.8, 1.6, 2.4, 3.4, 4.3],
    },
    # Gemini (2026-10-16): the equipment section goes first, then the retrofire and the retro section.
    "equipment-section-separation": {
        "pad": False, "mirror": True, "before": ["motes.jsx"],
        "success": (6.0, 1.6), "glitch": (6.0, 3.4), "failure": (6.4, 3.2),
        "times": [0.5, 1.0, 1.8, 2.8, 4.0, 5.9],
    },
    "orbital-module-separation": {
        "pad": False, "mirror": True, "before": ["motes.jsx"],
        "success": (5.6, 1.6), "glitch": (5.6, 3.4), "failure": (6.4, 3.2),
        "times": [0.5, 1.0, 1.8, 2.8, 3.8, 5.5],
    },
    # Vostok's way (2026-10-14): the module instead of the jettison, and a landing instead of the sea.
    "modules-separation": {
        "pad": False, "mirror": True, "before": ["motes.jsx"],
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
        "pad": False, "mirror": True, "before": ["motes.jsx"],
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
    # The probe alone after the burn, coming alive (2026-10-16).
    "earth-departure": {
        "pad": False, "before": ["orbit.jsx"],
        "success": (6.4, 2.2), "glitch": (6.4, 2.8), "failure": (6.4, 3.0),
        "times": [0.5, 1.6, 2.4, 3.4, 4.4, 6.3],
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
        "success": (6.0, 2.4), "glitch": (6.0, 3.2), "failure": (6.0, 3.0),
        "times": [0.4, 1.2, 1.8, 2.4, 3.2, 5.9],
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
    # Surveyor's own (2026-10-17): the tripod on its three verniers.
    "surveyor-landing": {
        "pad": False, "before": ["orbit.jsx", "moon.jsx"],
        "success": (6.0, 2.6), "glitch": (6.0, 3.9), "failure": (6.0, 3.4),
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
        "pad": False, "before": ["moon.jsx", "craft.jsx"],
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
        "success": (8.0, 3.75), "glitch": (8.0, 4.2), "failure": (8.0, 4.2),
        "times": [0.9, 1.8, 2.7, 3.75, 5.4, 7.9],
    },
    # Probe landers on the planets (2026-10-14): the bus, the landing, the work on the surface (probe.jsx).
    "probe-landing": {
        "pad": False, "before": ["orbit.jsx", "moon.jsx", "probe.jsx"],
        "success": (6.0, 2.6), "glitch": (6.0, 3.2), "failure": (6.0, 3.2),
        "times": [0.8, 1.8, 2.4, 3.0, 3.8, 5.9],
    },
    # Luna 17's rover (2026-10-16): down the ramps first, then a drive (the second surface operations).
    "lunokhod-deploy": {
        "pad": False, "before": ["orbit.jsx", "lunokhod.jsx"],
        "success": (6.4, 3.6), "glitch": (6.4, 4.8), "failure": (6.4, 3.4),
        "times": [0.6, 1.6, 2.8, 3.4, 4.2, 6.3],
    },
    # The Luna landers (2026-10-16): the Ye-8 stage's landing with Luna 17's rover or Luna 15's sampler on
    # its deck, and the sampler's drilling.
    "luna-landing-rover": {
        "pad": False, "before": ["orbit.jsx", "lunokhod.jsx", "luna.jsx"], "after": ["luna-landing.jsx"],
        "success": (6.0, 2.8), "glitch": (6.0, 3.2), "failure": (6.0, 2.2),
        "times": [0.4, 1.4, 2.2, 2.7, 3.4, 5.9],
    },
    "luna-landing-sampler": {
        "pad": False, "before": ["orbit.jsx", "lunokhod.jsx", "luna.jsx"], "after": ["luna-landing.jsx"],
        "success": (6.0, 2.8), "glitch": (6.0, 3.2), "failure": (6.0, 2.2),
        "times": [0.4, 1.4, 2.2, 2.7, 3.4, 5.9],
    },
    "luna-sampler": {
        "pad": False, "before": ["orbit.jsx", "lunokhod.jsx", "luna.jsx"],
        "success": (6.4, 4.6), "glitch": (6.4, 5.2), "failure": (6.4, 3.4),
        "times": [0.8, 1.6, 2.4, 3.2, 4.5, 6.3],
    },
    "luna-liftoff": {
        "pad": False, "before": ["orbit.jsx", "lunokhod.jsx", "luna.jsx"],
        "success": (6.0, 1.8), "glitch": (6.0, 2.6), "failure": (6.0, 2.2),
        "times": [0.5, 1.2, 1.8, 2.2, 3.0, 5.9],
    },
    "lunokhod-drive": {
        "pad": False, "before": ["orbit.jsx", "lunokhod.jsx"],
        "success": (6.4, 4.4), "glitch": (6.4, 4.4), "failure": (6.4, 3.6),
        "times": [0.5, 2.0, 2.8, 3.4, 4.6, 6.3],
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
    # Project Mercury's own (2026-10-17): the generic timelines ("source") with its capsule ("skin": the mc- parts),
    # its retros lit one after another and its periscope stowed after the burn (project-mercury.jsx, stow.jsx).
    "mercury-launch-preparations": {
        "pad": True, "source": "launch-preparations", "skin": "mc-",
        "success": (6.4, 2.6), "glitch": (6.4, 3.5), "failure": (7.0, 3.1),
        "times": [0.5, 2.0, 2.8, 3.4, 4.6, 6.4],
    },
    "mercury-countdown": {
        "pad": True, "source": "countdown", "skin": "mc-",
        "success": (6.4, 4.8), "glitch": (7.2, 3.6), "failure": (7.0, 4.7),
        "times": [0.0, 2.2, 3.0, 4.4, 4.8, 6.4],
    },
    "mercury-ascent": {
        "pad": False, "source": "ascent", "skin": "mc-", "alias": {"debris-1": "mc-debris-1-bare"},
        "success": (6.5, 3.6), "glitch": (6.5, 4.0), "failure": (7.0, 3.4),
        "times": [0.0, 1.5, 2.9, 3.4, 4.2, 6.4],
    },
    "mercury-deployment": {
        "pad": False, "source": "deployment", "skin": "mc-",
        "success": (6.0, 2.6), "glitch": (6.0, 2.6), "failure": (6.5, 2.3),
        "times": [0.5, 1.2, 1.9, 2.4, 3.6, 5.9],
    },
    "mercury-suborbital-flight": {
        "pad": False, "mirror": True, "source": "suborbital-flight", "skin": "mc-", "before": ["motes.jsx"],
        "success": (6.4, 3.4), "glitch": (6.4, 4.0), "failure": (6.4, 3.6),
        "times": [0.5, 1.8, 2.8, 3.4, 4.4, 6.3],
    },
    "mercury-earth-orbit": {
        "pad": False, "source": "earth-orbit", "skin": "mc-",
        "success": (6.0, 2.1), "glitch": (6.0, 3.4), "failure": (6.5, 3.0),
        "times": [0.5, 2.0, 2.8, 3.6, 4.6, 5.9],
    },
    "mercury-duration": {
        "pad": False, "source": "duration-level-i", "skin": "mc-", "before": ["orbit.jsx"], "after": ["duration.jsx"],
        "success": (6.4, 2.6), "glitch": (6.4, 3.8), "failure": (6.4, 3.6),
        "times": [0.5, 2.0, 2.6, 3.4, 4.4, 6.3],
    },
    "mercury-rendezvous": {
        "pad": False, "source": "rendezvous-target-approach", "skin": "mc-", "before": ["orbit.jsx"],
        "success": (6.0, 2.6), "glitch": (6.0, 3.0), "failure": (6.0, 3.0),
        "times": [0.5, 1.8, 2.4, 2.8, 4.0, 5.9],
    },
    "mercury-reentry-preparations": {
        "pad": False, "mirror": True, "source": "reentry-preparations", "skin": "mc-", "before": ["motes.jsx"],
        "success": (6.0, 3.0), "glitch": (6.0, 3.6), "failure": (6.5, 3.4),
        "times": [0.5, 1.6, 2.4, 3.2, 4.4, 5.9],
    },
    "mercury-retrofire": {
        "pad": False, "mirror": True, "source": "retrofire", "skin": "mc-", "before": ["motes.jsx", "project-mercury.jsx", "stow.jsx"],
        "success": (6.4, 2.6), "glitch": (6.4, 3.0), "failure": (6.4, 3.0),
        "times": [0.5, 1.4, 2.0, 2.8, 3.8, 6.3],
    },
    "mercury-retrofire-hop": {
        "pad": False, "mirror": True, "source": "retrofire", "skin": "mc-", "before": ["motes.jsx", "project-mercury.jsx"],
        "success": (6.4, 2.6), "glitch": (6.4, 3.0), "failure": (6.4, 3.0),
        "times": [0.5, 1.4, 2.0, 2.8, 3.8, 6.3],
    },
    "mercury-deorbit-burn": {
        "pad": False, "mirror": True, "source": "deorbit-burn", "skin": "mc-", "before": ["motes.jsx", "project-mercury.jsx", "stow.jsx"], "after": ["retrofire.jsx"],
        "success": (7.6, 3.8), "glitch": (7.6, 4.2), "failure": (7.6, 4.2),
        "times": [0.3, 1.0, 1.8, 2.6, 4.0, 7.5],
    },
    "mercury-reentry-trim": {
        "pad": False, "mirror": True, "source": "reentry-trim-burn", "skin": "mc-", "before": ["motes.jsx"], "after": ["reentry-trim.jsx"],
        "success": (6.0, 3.0), "glitch": (6.0, 3.4), "failure": (6.0, 3.0),
        "times": [0.5, 1.6, 2.4, 3.0, 4.0, 5.9],
    },
    "mercury-retrofire-jettison": {
        "pad": False, "mirror": True, "source": "retrofire-jettison", "skin": "mc-", "before": ["motes.jsx"],
        "success": (5.6, 1.6), "glitch": (5.6, 3.4), "failure": (6.4, 3.2),
        "times": [0.5, 1.1, 1.8, 2.6, 3.8, 5.5],
    },
    "mercury-reentry": {
        "pad": False, "mirror": True, "source": "reentry", "skin": "mc-",
        "success": (6.4, 4.0), "glitch": (6.4, 4.4), "failure": (6.4, 3.4),
        "times": [0.4, 1.6, 2.6, 3.4, 4.6, 6.3],
    },
    "mercury-descent-and-splashdown": {
        "pad": False, "source": "descent-and-splashdown", "skin": "mc-", "after": ["descent.jsx"],
        "success": (6.4, 2.4), "glitch": (6.4, 3.4), "failure": (6.4, 2.6),
        "times": [0.5, 1.4, 2.2, 3.0, 4.2, 6.3],
    },
    # The first satellites' own (2026-10-18, user; the early-era pack): Sputnik 1 (sp1-), Explorer 1 (ex1-) and
    # Sputnik 2 (sp2-) in the generic satellite's timelines.
    "sputnik-1-launch-preparations": {
        "pad": True, "source": "launch-preparations", "skin": "sp1-",
        "success": (6.4, 2.6), "glitch": (6.4, 3.5), "failure": (7.0, 3.1),
        "times": [0.5, 2.0, 2.8, 3.4, 4.6, 6.4],
    },
    "sputnik-1-countdown": {
        "pad": True, "source": "countdown", "skin": "sp1-",
        "success": (6.4, 4.8), "glitch": (7.2, 3.6), "failure": (7.0, 4.7),
        "times": [0.0, 2.2, 3.0, 4.4, 4.8, 6.4],
    },
    "sputnik-1-ascent": {
        "pad": False, "source": "ascent", "skin": "sp1-",
        "success": (6.5, 3.6), "glitch": (6.5, 4.0), "failure": (7.0, 3.4),
        "times": [0.0, 1.5, 2.9, 3.4, 4.2, 6.4],
    },
    "sputnik-1-deployment": {
        "pad": False, "source": "deployment", "skin": "sp1-",
        "success": (6.0, 2.6), "glitch": (6.0, 2.6), "failure": (6.5, 2.3),
        "times": [0.5, 1.2, 1.9, 2.4, 3.6, 5.9],
    },
    "sputnik-1-earth-orbit": {
        "pad": False, "source": "earth-orbit", "skin": "sp1-",
        "success": (6.0, 2.1), "glitch": (6.0, 3.4), "failure": (6.5, 3.0),
        "times": [0.5, 2.0, 2.8, 3.6, 4.6, 5.9],
    },
    "sputnik-1-advanced-experiments-earth-orbit": {
        "pad": False, "source": "advanced-experiments-earth-orbit", "skin": "sp1-", "before": ["orbit.jsx"],
        "success": (6.0, 2.4), "glitch": (6.0, 3.6), "failure": (6.0, 3.6),
        "times": [0.5, 1.8, 2.4, 3.2, 4.0, 5.9],
    },
    "explorer-1-launch-preparations": {
        "pad": True, "source": "launch-preparations", "skin": "ex1-",
        "success": (6.4, 2.6), "glitch": (6.4, 3.5), "failure": (7.0, 3.1),
        "times": [0.5, 2.0, 2.8, 3.4, 4.6, 6.4],
    },
    "explorer-1-countdown": {
        "pad": True, "source": "countdown", "skin": "ex1-",
        "success": (6.4, 4.8), "glitch": (7.2, 3.6), "failure": (7.0, 4.7),
        "times": [0.0, 2.2, 3.0, 4.4, 4.8, 6.4],
    },
    "explorer-1-ascent": {
        "pad": False, "source": "ascent", "skin": "ex1-",
        "success": (6.5, 3.6), "glitch": (6.5, 4.0), "failure": (7.0, 3.4),
        "times": [0.0, 1.5, 2.9, 3.4, 4.2, 6.4],
    },
    "explorer-1-deployment": {
        "pad": False, "source": "deployment", "skin": "ex1-",
        "success": (6.0, 2.6), "glitch": (6.0, 2.6), "failure": (6.5, 2.3),
        "times": [0.5, 1.2, 1.9, 2.4, 3.6, 5.9],
    },
    "explorer-1-earth-orbit": {
        "pad": False, "source": "earth-orbit", "skin": "ex1-",
        "success": (6.0, 2.1), "glitch": (6.0, 3.4), "failure": (6.5, 3.0),
        "times": [0.5, 2.0, 2.8, 3.6, 4.6, 5.9],
    },
    "explorer-1-advanced-experiments-earth-orbit": {
        "pad": False, "source": "advanced-experiments-earth-orbit", "skin": "ex1-", "before": ["orbit.jsx"],
        "success": (6.0, 2.4), "glitch": (6.0, 3.6), "failure": (6.0, 3.6),
        "times": [0.5, 1.8, 2.4, 3.2, 4.0, 5.9],
    },
    "sputnik-2-launch-preparations": {
        "pad": True, "source": "launch-preparations", "skin": "sp2-",
        "success": (6.4, 2.6), "glitch": (6.4, 3.5), "failure": (7.0, 3.1),
        "times": [0.5, 2.0, 2.8, 3.4, 4.6, 6.4],
    },
    "sputnik-2-countdown": {
        "pad": True, "source": "countdown", "skin": "sp2-",
        "success": (6.4, 4.8), "glitch": (7.2, 3.6), "failure": (7.0, 4.7),
        "times": [0.0, 2.2, 3.0, 4.4, 4.8, 6.4],
    },
    "sputnik-2-ascent": {
        "pad": False, "source": "ascent", "skin": "sp2-",
        "success": (6.5, 3.6), "glitch": (6.5, 4.0), "failure": (7.0, 3.4),
        "times": [0.0, 1.5, 2.9, 3.4, 4.2, 6.4],
    },
    "sputnik-2-deployment": {
        "pad": False, "source": "deployment", "skin": "sp2-",
        "success": (6.0, 2.6), "glitch": (6.0, 2.6), "failure": (6.5, 2.3),
        "times": [0.5, 1.2, 1.9, 2.4, 3.6, 5.9],
    },
    "sputnik-2-earth-orbit": {
        "pad": False, "source": "earth-orbit", "skin": "sp2-",
        "success": (6.0, 2.1), "glitch": (6.0, 3.4), "failure": (6.5, 3.0),
        "times": [0.5, 2.0, 2.8, 3.6, 4.6, 5.9],
    },
    "sputnik-2-biometric-data-transmission": {
        "pad": False, "source": "biometric-data-transmission", "skin": "sp2-", "before": ["orbit.jsx"],
        "success": (6.0, 2.6), "glitch": (6.0, 3.8), "failure": (6.0, 3.8),
        "times": [0.5, 1.8, 2.6, 3.2, 4.4, 5.9],
    },
    # Vostok's own (2026-10-18, user; the early-era pack): the generic timelines with its spacecraft ("skin": the vk-
    # parts), as Mercury's; the climb throws its fairing off as Mercury's tower (a-les), and the separation's cable
    # meets its module (project-vostok.jsx).
    "vostok-launch-preparations": {
        "pad": True, "source": "launch-preparations", "skin": "vk-",
        "success": (6.4, 2.6), "glitch": (6.4, 3.5), "failure": (7.0, 3.1),
        "times": [0.5, 2.0, 2.8, 3.4, 4.6, 6.4],
    },
    "vostok-countdown": {
        "pad": True, "source": "countdown", "skin": "vk-",
        "success": (6.4, 4.8), "glitch": (7.2, 3.6), "failure": (7.0, 4.7),
        "times": [0.0, 2.2, 3.0, 4.4, 4.8, 6.4],
    },
    "vostok-ascent": {
        "pad": False, "source": "ascent", "skin": "vk-", "alias": {"debris-1": "vk-debris-1-bare"},
        "success": (6.5, 3.6), "glitch": (6.5, 4.0), "failure": (7.0, 3.4),
        "times": [0.0, 1.5, 2.9, 3.4, 4.2, 6.4],
    },
    "vostok-deployment": {
        "pad": False, "source": "deployment", "skin": "vk-",
        "success": (6.0, 2.6), "glitch": (6.0, 2.6), "failure": (6.5, 2.3),
        "times": [0.5, 1.2, 1.9, 2.4, 3.6, 5.9],
    },
    "vostok-suborbital-flight": {
        "pad": False, "source": "suborbital-flight", "skin": "vk-", "mirror": True, "before": ["motes.jsx"],
        "success": (6.4, 3.4), "glitch": (6.4, 4.0), "failure": (6.4, 3.6),
        "times": [0.5, 1.8, 2.8, 3.4, 4.4, 6.3],
    },
    "vostok-earth-orbit": {
        "pad": False, "source": "earth-orbit", "skin": "vk-",
        "success": (6.0, 2.1), "glitch": (6.0, 3.4), "failure": (6.5, 3.0),
        "times": [0.5, 2.0, 2.8, 3.6, 4.6, 5.9],
    },
    "vostok-duration-level-i": {
        "pad": False, "source": "duration-level-i", "skin": "vk-", "before": ["orbit.jsx"], "after": ["duration.jsx"],
        "success": (6.4, 2.6), "glitch": (6.4, 3.8), "failure": (6.4, 3.6),
        "times": [0.5, 2.0, 2.6, 3.4, 4.4, 6.3],
    },
    "vostok-duration-level-ii": {
        "pad": False, "source": "duration-level-ii", "skin": "vk-", "before": ["orbit.jsx"], "after": ["duration.jsx"],
        "success": (6.4, 2.6), "glitch": (6.4, 3.8), "failure": (6.4, 3.6),
        "times": [0.5, 2.0, 2.6, 3.4, 4.4, 6.3],
    },
    "vostok-reentry-preparations": {
        "pad": False, "source": "reentry-preparations", "skin": "vk-", "mirror": True, "before": ["motes.jsx"],
        "success": (6.0, 3.0), "glitch": (6.0, 3.6), "failure": (6.5, 3.4),
        "times": [0.5, 1.6, 2.4, 3.2, 4.4, 5.9],
    },
    "vostok-reentry-trim": {
        "pad": False, "source": "reentry-trim", "skin": "vk-", "mirror": True, "before": ["motes.jsx"],
        "success": (6.0, 3.0), "glitch": (6.0, 3.4), "failure": (6.0, 3.0),
        "times": [0.5, 1.6, 2.4, 3.0, 4.0, 5.9],
    },
    "vostok-retrofire": {
        "pad": False, "source": "retrofire", "skin": "vk-", "mirror": True, "before": ["motes.jsx"],
        "success": (6.4, 2.6), "glitch": (6.4, 3.0), "failure": (6.4, 3.0),
        "times": [0.5, 1.4, 2.0, 2.8, 3.8, 6.3],
    },
    "vostok-retro-shutdown": {
        "pad": False, "source": "retro-shutdown", "skin": "vk-", "mirror": True, "before": ["motes.jsx"],
        "success": (4.4, 1.4), "glitch": (4.4, 2.8), "failure": (5.2, 3.0),
        "times": [0.2, 0.8, 1.6, 2.4, 3.4, 4.3],
    },
    "vostok-modules-separation": {
        "pad": False, "source": "modules-separation", "skin": "vk-", "mirror": True, "before": ["project-vostok.jsx", "motes.jsx"],
        "success": (5.6, 1.8), "glitch": (5.6, 3.6), "failure": (6.4, 3.8),
        "times": [0.5, 1.0, 1.6, 2.6, 3.6, 5.5],
    },
    "vostok-reentry": {
        "pad": False, "source": "reentry", "skin": "vk-", "mirror": True,
        "success": (6.4, 4.0), "glitch": (6.4, 4.4), "failure": (6.4, 3.4),
        "times": [0.4, 1.6, 2.6, 3.4, 4.6, 6.3],
    },
    "vostok-rendezvous": {
        "pad": False, "source": "rendezvous-target-approach", "skin": "vk-", "before": ["orbit.jsx"],
        "success": (6.0, 2.6), "glitch": (6.0, 3.0), "failure": (6.0, 3.0),
        "times": [0.5, 1.8, 2.4, 2.8, 4.0, 5.9],
    },
    "vostok-descent-and-landing": {
        "pad": False, "source": "descent-and-landing", "skin": "vk-", "after": ["descent.jsx"],
        "success": (6.4, 2.4), "glitch": (6.4, 3.4), "failure": (6.4, 2.6),
        "times": [0.5, 1.4, 2.2, 3.0, 4.2, 6.3],
    },
    # Voskhod's own (2026-10-18, user): Vostok's skin with Voskhod's parts in its place (the backup retro on the
    # sphere's top, gone by the reentry), and its soft landing (project-voskhod.jsx).
    "voskhod-launch-preparations": {
        "pad": True, "source": "launch-preparations", "skin": "vk-", "alias": {"rocket": "vh-rocket", "debris-1": "vh-debris-1", "d-payload": "vh-d-payload", "o-sat": "vh-o-sat", "o-sat-b": "vh-o-sat-b", "r-capsule": "vh-r-capsule", "r-capsule-up": "vh-r-capsule-up"},
        "success": (6.4, 2.6), "glitch": (6.4, 3.5), "failure": (7.0, 3.1),
        "times": [0.5, 2.0, 2.8, 3.4, 4.6, 6.4],
    },
    "voskhod-countdown": {
        "pad": True, "source": "countdown", "skin": "vk-", "alias": {"rocket": "vh-rocket", "debris-1": "vh-debris-1", "d-payload": "vh-d-payload", "o-sat": "vh-o-sat", "o-sat-b": "vh-o-sat-b", "r-capsule": "vh-r-capsule", "r-capsule-up": "vh-r-capsule-up"},
        "success": (6.4, 4.8), "glitch": (7.2, 3.6), "failure": (7.0, 4.7),
        "times": [0.0, 2.2, 3.0, 4.4, 4.8, 6.4],
    },
    "voskhod-deployment": {
        "pad": False, "source": "deployment", "skin": "vk-", "alias": {"rocket": "vh-rocket", "debris-1": "vh-debris-1", "d-payload": "vh-d-payload", "o-sat": "vh-o-sat", "o-sat-b": "vh-o-sat-b", "r-capsule": "vh-r-capsule", "r-capsule-up": "vh-r-capsule-up"},
        "success": (6.0, 2.6), "glitch": (6.0, 2.6), "failure": (6.5, 2.3),
        "times": [0.5, 1.2, 1.9, 2.4, 3.6, 5.9],
    },
    "voskhod-suborbital-flight": {
        "pad": False, "source": "suborbital-flight", "skin": "vk-", "alias": {"rocket": "vh-rocket", "debris-1": "vh-debris-1", "d-payload": "vh-d-payload", "o-sat": "vh-o-sat", "o-sat-b": "vh-o-sat-b", "r-capsule": "vh-r-capsule", "r-capsule-up": "vh-r-capsule-up"}, "mirror": True, "before": ["motes.jsx"],
        "success": (6.4, 3.4), "glitch": (6.4, 4.0), "failure": (6.4, 3.6),
        "times": [0.5, 1.8, 2.8, 3.4, 4.4, 6.3],
    },
    "voskhod-earth-orbit": {
        "pad": False, "source": "earth-orbit", "skin": "vk-", "alias": {"rocket": "vh-rocket", "debris-1": "vh-debris-1", "d-payload": "vh-d-payload", "o-sat": "vh-o-sat", "o-sat-b": "vh-o-sat-b", "r-capsule": "vh-r-capsule", "r-capsule-up": "vh-r-capsule-up"},
        "success": (6.0, 2.1), "glitch": (6.0, 3.4), "failure": (6.5, 3.0),
        "times": [0.5, 2.0, 2.8, 3.6, 4.6, 5.9],
    },
    "voskhod-duration-level-i": {
        "pad": False, "source": "duration-level-i", "skin": "vk-", "alias": {"rocket": "vh-rocket", "debris-1": "vh-debris-1", "d-payload": "vh-d-payload", "o-sat": "vh-o-sat", "o-sat-b": "vh-o-sat-b", "r-capsule": "vh-r-capsule", "r-capsule-up": "vh-r-capsule-up"}, "before": ["orbit.jsx"], "after": ["duration.jsx"],
        "success": (6.4, 2.6), "glitch": (6.4, 3.8), "failure": (6.4, 3.6),
        "times": [0.5, 2.0, 2.6, 3.4, 4.4, 6.3],
    },
    "voskhod-reentry-preparations": {
        "pad": False, "source": "reentry-preparations", "skin": "vk-", "alias": {"rocket": "vh-rocket", "debris-1": "vh-debris-1", "d-payload": "vh-d-payload", "o-sat": "vh-o-sat", "o-sat-b": "vh-o-sat-b", "r-capsule": "vh-r-capsule", "r-capsule-up": "vh-r-capsule-up"}, "mirror": True, "before": ["motes.jsx"],
        "success": (6.0, 3.0), "glitch": (6.0, 3.6), "failure": (6.5, 3.4),
        "times": [0.5, 1.6, 2.4, 3.2, 4.4, 5.9],
    },
    "voskhod-reentry-trim": {
        "pad": False, "source": "reentry-trim", "skin": "vk-", "alias": {"rocket": "vh-rocket", "debris-1": "vh-debris-1", "d-payload": "vh-d-payload", "o-sat": "vh-o-sat", "o-sat-b": "vh-o-sat-b", "r-capsule": "vh-r-capsule", "r-capsule-up": "vh-r-capsule-up"}, "mirror": True, "before": ["motes.jsx"],
        "success": (6.0, 3.0), "glitch": (6.0, 3.4), "failure": (6.0, 3.0),
        "times": [0.5, 1.6, 2.4, 3.0, 4.0, 5.9],
    },
    "voskhod-retrofire": {
        "pad": False, "source": "retrofire", "skin": "vk-", "alias": {"rocket": "vh-rocket", "debris-1": "vh-debris-1", "d-payload": "vh-d-payload", "o-sat": "vh-o-sat", "o-sat-b": "vh-o-sat-b", "r-capsule": "vh-r-capsule", "r-capsule-up": "vh-r-capsule-up"}, "mirror": True, "before": ["motes.jsx"],
        "success": (6.4, 2.6), "glitch": (6.4, 3.0), "failure": (6.4, 3.0),
        "times": [0.5, 1.4, 2.0, 2.8, 3.8, 6.3],
    },
    "voskhod-retro-shutdown": {
        "pad": False, "source": "retro-shutdown", "skin": "vk-", "alias": {"rocket": "vh-rocket", "debris-1": "vh-debris-1", "d-payload": "vh-d-payload", "o-sat": "vh-o-sat", "o-sat-b": "vh-o-sat-b", "r-capsule": "vh-r-capsule", "r-capsule-up": "vh-r-capsule-up"}, "mirror": True, "before": ["motes.jsx"],
        "success": (4.4, 1.4), "glitch": (4.4, 2.8), "failure": (5.2, 3.0),
        "times": [0.2, 0.8, 1.6, 2.4, 3.4, 4.3],
    },
    "voskhod-modules-separation": {
        "pad": False, "source": "modules-separation", "skin": "vk-", "alias": {"rocket": "vh-rocket", "debris-1": "vh-debris-1", "d-payload": "vh-d-payload", "o-sat": "vh-o-sat", "o-sat-b": "vh-o-sat-b", "r-capsule": "vh-r-capsule", "r-capsule-up": "vh-r-capsule-up"}, "mirror": True, "before": ["project-vostok.jsx", "motes.jsx"],
        "success": (5.6, 1.8), "glitch": (5.6, 3.6), "failure": (6.4, 3.8),
        "times": [0.5, 1.0, 1.6, 2.6, 3.6, 5.5],
    },
    "voskhod-rendezvous": {
        "pad": False, "source": "rendezvous-target-approach", "skin": "vk-", "alias": {"rocket": "vh-rocket", "debris-1": "vh-debris-1", "d-payload": "vh-d-payload", "o-sat": "vh-o-sat", "o-sat-b": "vh-o-sat-b", "r-capsule": "vh-r-capsule", "r-capsule-up": "vh-r-capsule-up"}, "before": ["orbit.jsx"],
        "success": (6.0, 2.6), "glitch": (6.0, 3.0), "failure": (6.0, 3.0),
        "times": [0.5, 1.8, 2.4, 2.8, 4.0, 5.9],
    },
    "voskhod-ascent": {
        "pad": False, "source": "ascent", "skin": "vk-", "alias": {"rocket": "vh-rocket", "debris-1": "vh-debris-1-bare", "d-payload": "vh-d-payload", "o-sat": "vh-o-sat", "o-sat-b": "vh-o-sat-b", "r-capsule": "vh-r-capsule", "r-capsule-up": "vh-r-capsule-up", "a-les": "vk-a-les"},
        "success": (6.5, 3.6), "glitch": (6.5, 4.0), "failure": (7.0, 3.4),
        "times": [0.0, 1.5, 2.9, 3.4, 4.2, 6.4],
    },
    "voskhod-reentry": {
        "pad": False, "source": "reentry", "skin": "vk-", "alias": {"rocket": "vh-rocket", "debris-1": "vh-debris-1", "d-payload": "vh-d-payload", "o-sat": "vh-o-sat", "o-sat-b": "vh-o-sat-b", "r-capsule": "vh-r-capsule-bare", "r-capsule-up": "vh-r-capsule-up"}, "mirror": True,
        "success": (6.4, 4.0), "glitch": (6.4, 4.4), "failure": (6.4, 3.4),
        "times": [0.4, 1.6, 2.6, 3.4, 4.6, 6.3],
    },
    "voskhod-descent": {
        "pad": False, "source": "descent-chutes", "skin": "vk-", "alias": {"rocket": "vh-rocket", "debris-1": "vh-debris-1", "d-payload": "vh-d-payload", "o-sat": "vh-o-sat", "o-sat-b": "vh-o-sat-b", "r-capsule": "vh-r-capsule", "r-capsule-up": "vh-r-capsule-up"}, "after": ["descent.jsx"],
        "success": (6.0, 2.4), "glitch": (6.0, 3.4), "failure": (6.0, 2.6),
        "times": [0.5, 1.4, 2.2, 3.0, 4.2, 5.9],
    },
    "voskhod-landing-and-recovery": {
        "pad": False, "source": "landing-and-recovery", "skin": "vk-", "before": ["project-voskhod.jsx"], "alias": {"rocket": "vh-rocket", "debris-1": "vh-debris-1", "d-payload": "vh-d-payload", "o-sat": "vh-o-sat", "o-sat-b": "vh-o-sat-b", "r-capsule": "vh-r-capsule", "r-capsule-up": "vh-r-capsule-up"}, "after": ["touchdown.jsx"],
        "success": (6.0, 1.2), "glitch": (6.0, 1.8), "failure": (6.0, 1.8),
        "times": [0.5, 1.2, 1.8, 2.6, 3.6, 5.9],
    },
    # Voskhod 2's spacewalk (2026-10-18, user): the craft with its airlock (vh-e-capsule) in the spacewalk scene: out
    # through the airlock, filming, and back in with the suit ballooned (eva-film.jsx, eva-in.jsx).
    "voskhod-eva-out": {
        "pad": False, "source": "eva-out", "skin": "vh-", "alias": {"r-capsule": "vh-e-capsule"}, "before": ["spacewalk.jsx"],
        "success": (5.6, 2.6), "glitch": (6.0, 3.2), "failure": (6.6, 3.2),
        "times": [1.2, 2.2, 2.8, 3.4, 4.4, 5.5],
    },
    "voskhod-eva-film": {
        "pad": False, "source": "eva-film", "skin": "vh-", "alias": {"r-capsule": "vh-e-capsule"}, "before": ["spacewalk.jsx"],
        "success": (4.6, 1.2), "glitch": (4.6, 2.2), "failure": (4.6, 2.4),
        "times": [0.5, 1.1, 1.3, 2.2, 2.6, 4.5],
    },
    "voskhod-eva-in": {
        "pad": False, "source": "eva-in", "skin": "vh-", "alias": {"r-capsule": "vh-e-capsule"}, "before": ["spacewalk.jsx"],
        "success": (3.8, 1.9), "glitch": (5.2, 2.1), "failure": (6.4, 3.0),
        "times": [0.6, 1.8, 2.2, 3.0, 4.0, 5.0],
    },
    # Soyuz 7K-OK's own (2026-10-18, user; sy- parts): its three modules, the orbital module on the nose until the
    # modules separate (project-soyuz.jsx's OM), its soft landing, its fairing and tower thrown off in the climb.
    "soyuz-launch-preparations": {
        "pad": True, "source": "launch-preparations", "skin": "sy-",
        "success": (6.4, 2.6), "glitch": (6.4, 3.5), "failure": (7.0, 3.1),
        "times": [0.5, 2.0, 2.8, 3.4, 4.6, 6.4],
    },
    "soyuz-countdown": {
        "pad": True, "source": "countdown", "skin": "sy-",
        "success": (6.4, 4.8), "glitch": (7.2, 3.6), "failure": (7.0, 4.7),
        "times": [0.0, 2.2, 3.0, 4.4, 4.8, 6.4],
    },
    "soyuz-deployment": {
        "pad": False, "source": "deployment", "skin": "sy-",
        "success": (6.0, 2.6), "glitch": (6.0, 2.6), "failure": (6.5, 2.3),
        "times": [0.5, 1.2, 1.9, 2.4, 3.6, 5.9],
    },
    "soyuz-earth-orbit": {
        "pad": False, "source": "earth-orbit", "skin": "sy-",
        "success": (6.0, 2.1), "glitch": (6.0, 3.4), "failure": (6.5, 3.0),
        "times": [0.5, 2.0, 2.8, 3.6, 4.6, 5.9],
    },
    "soyuz-insertion": {
        "pad": False, "source": "insertion", "skin": "sy-", "before": ["orbit.jsx"],
        "success": (6.0, 2.4), "glitch": (6.0, 3.2), "failure": (6.0, 3.0),
        "times": [0.4, 1.2, 1.8, 2.4, 3.2, 5.9],
    },
    "soyuz-target-approach": {
        "pad": False, "source": "target-approach", "skin": "sy-", "before": ["dock.jsx"],
        "success": (6.4, 2.4), "glitch": (6.4, 2.6), "failure": (6.4, 2.6),
        "times": [0.5, 1.9, 2.3, 2.7, 3.6, 6.3],
    },
    "soyuz-docking": {
        "pad": False, "source": "docking", "skin": "sy-", "before": ["dock.jsx"],
        "success": (6.4, 2.0), "glitch": (6.4, 3.4), "failure": (6.4, 3.4),
        "times": [0.5, 1.7, 2.3, 3.1, 4.2, 6.3],
    },
    "soyuz-undocking": {
        "pad": False, "source": "undocking", "skin": "sy-", "before": ["dock.jsx"],
        "success": (6.4, 1.2), "glitch": (6.4, 2.8), "failure": (6.4, 2.8),
        "times": [0.5, 1.2, 2.0, 2.6, 3.8, 6.3],
    },
    "soyuz-rendezvous": {
        "pad": False, "source": "rendezvous", "skin": "sy-", "before": ["dock.jsx"],
        "success": (6.0, 2.4), "glitch": (6.0, 3.4), "failure": (6.0, 3.4),
        "times": [0.5, 1.6, 2.4, 3.2, 4.2, 5.9],
    },
    "soyuz-reentry": {
        "pad": False, "source": "reentry", "skin": "sy-", "mirror": True,
        "success": (6.4, 4.0), "glitch": (6.4, 4.4), "failure": (6.4, 3.4),
        "times": [0.4, 1.6, 2.6, 3.4, 4.6, 6.3],
    },
    "soyuz-descent": {
        "pad": False, "source": "descent-chutes", "skin": "sy-", "after": ["descent.jsx"],
        "success": (6.0, 2.4), "glitch": (6.0, 3.4), "failure": (6.0, 2.6),
        "times": [0.5, 1.4, 2.2, 3.0, 4.2, 5.9],
    },
    "soyuz-ascent": {
        "pad": False, "source": "ascent", "skin": "sy-", "alias": {"debris-1": "sy-debris-1-bare"},
        "success": (6.5, 3.6), "glitch": (6.5, 4.0), "failure": (7.0, 3.4),
        "times": [0.0, 1.5, 2.9, 3.4, 4.2, 6.4],
    },
    "soyuz-joint": {
        "pad": False, "source": "rendezvous-target-approach", "skin": "sy-", "before": ["orbit.jsx"],
        "success": (6.0, 2.6), "glitch": (6.0, 3.0), "failure": (6.0, 3.0),
        "times": [0.5, 1.8, 2.4, 2.8, 4.0, 5.9],
    },
    "soyuz-reentry-preparations": {
        "pad": False, "source": "reentry-preparations", "skin": "sy-", "mirror": True, "before": ["project-soyuz.jsx", "motes.jsx"],
        "success": (6.0, 3.0), "glitch": (6.0, 3.6), "failure": (6.5, 3.4),
        "times": [0.5, 1.6, 2.4, 3.2, 4.4, 5.9],
    },
    "soyuz-reentry-trim": {
        "pad": False, "source": "reentry-trim-burn", "skin": "sy-", "mirror": True, "before": ["project-soyuz.jsx", "motes.jsx"], "after": ["reentry-trim.jsx"],
        "success": (6.0, 3.0), "glitch": (6.0, 3.4), "failure": (6.0, 3.0),
        "times": [0.5, 1.6, 2.4, 3.0, 4.0, 5.9],
    },
    "soyuz-retrofire": {
        "pad": False, "source": "retrofire", "skin": "sy-", "mirror": True, "before": ["project-soyuz.jsx", "motes.jsx"],
        "success": (6.4, 2.6), "glitch": (6.4, 3.0), "failure": (6.4, 3.0),
        "times": [0.5, 1.4, 2.0, 2.8, 3.8, 6.3],
    },
    "soyuz-deorbit-burn": {
        "pad": False, "source": "deorbit-burn", "skin": "sy-", "mirror": True, "before": ["project-soyuz.jsx", "motes.jsx"], "after": ["retrofire.jsx"],
        "success": (7.6, 3.8), "glitch": (7.6, 4.2), "failure": (7.6, 4.2),
        "times": [0.3, 1.0, 1.8, 2.6, 4.0, 7.5],
    },
    "soyuz-modules-separation": {
        "pad": False, "source": "modules-separation", "skin": "sy-", "mirror": True, "before": ["project-soyuz.jsx", "motes.jsx"],
        "success": (5.6, 1.8), "glitch": (5.6, 3.6), "failure": (6.4, 3.8),
        "times": [0.5, 1.0, 1.6, 2.6, 3.6, 5.5],
    },
    "soyuz-suborbital-flight": {
        "pad": False, "source": "suborbital-flight", "skin": "sy-", "mirror": True, "before": ["project-soyuz.jsx", "motes.jsx"],
        "success": (6.4, 3.4), "glitch": (6.4, 4.0), "failure": (6.4, 3.6),
        "times": [0.5, 1.8, 2.8, 3.4, 4.4, 6.3],
    },
    "soyuz-landing-and-recovery": {
        "pad": False, "source": "landing-and-recovery", "skin": "sy-", "before": ["project-soyuz.jsx"], "after": ["touchdown.jsx"],
        "success": (6.0, 1.2), "glitch": (6.0, 1.8), "failure": (6.0, 1.8),
        "times": [0.5, 1.2, 1.8, 2.6, 3.6, 5.9],
    },
    "soyuz-retrofire-jettison": {
        "pad": False, "source": "retrofire-jettison-om", "skin": "sy-", "mirror": True, "before": ["motes.jsx"], "after": ["retrofire-jettison.jsx"],
        "success": (5.6, 1.6), "glitch": (5.6, 3.4), "failure": (6.4, 3.2),
        "times": [0.5, 1.1, 1.8, 2.6, 3.8, 5.5],
    },
    "soyuz-orbital-module-separation": {
        "pad": False, "source": "orbital-module-separation", "skin": "sy-", "mirror": True, "before": ["motes.jsx"],
        "success": (5.6, 1.6), "glitch": (5.6, 3.4), "failure": (6.4, 3.2),
        "times": [0.5, 1.0, 1.8, 2.8, 3.8, 5.5],
    },
    # Apollo's command and service module (2026-10-18, user; ap- parts): on the pad in its adapter under the boost
    # cover and tower (thrown off in the climb); out of it alone, or on the S-IVB to the Moon (the -stack clips);
    # with the lander once it's extracted; the three mains coming home. The lander's own scenes are a later pass.
    "apollo-launch-preparations": {
        "pad": True, "source": "launch-preparations", "skin": "ap-",
        "success": (6.4, 2.6), "glitch": (6.4, 3.5), "failure": (7.0, 3.1),
        "times": [0.5, 2.0, 2.8, 3.4, 4.6, 6.4],
    },
    "apollo-countdown": {
        "pad": True, "source": "countdown", "skin": "ap-",
        "success": (6.4, 4.8), "glitch": (7.2, 3.6), "failure": (7.0, 4.7),
        "times": [0.0, 2.2, 3.0, 4.4, 4.8, 6.4],
    },
    "apollo-deployment": {
        "pad": False, "source": "deployment", "skin": "ap-",
        "success": (6.0, 2.6), "glitch": (6.0, 2.6), "failure": (6.5, 2.3),
        "times": [0.5, 1.2, 1.9, 2.4, 3.6, 5.9],
    },
    "apollo-earth-orbit": {
        "pad": False, "source": "earth-orbit", "skin": "ap-",
        "success": (6.0, 2.1), "glitch": (6.0, 3.4), "failure": (6.5, 3.0),
        "times": [0.5, 2.0, 2.8, 3.6, 4.6, 5.9],
    },
    "apollo-insertion": {
        "pad": False, "source": "insertion", "skin": "ap-", "before": ["orbit.jsx"],
        "success": (6.0, 2.4), "glitch": (6.0, 3.2), "failure": (6.0, 3.0),
        "times": [0.4, 1.2, 1.8, 2.4, 3.2, 5.9],
    },
    "apollo-duration-level-i": {
        "pad": False, "source": "duration-level-i", "skin": "ap-", "before": ["orbit.jsx"], "after": ["duration.jsx"],
        "success": (6.4, 2.6), "glitch": (6.4, 3.8), "failure": (6.4, 3.6),
        "times": [0.5, 2.0, 2.6, 3.4, 4.4, 6.3],
    },
    "apollo-duration-level-ii": {
        "pad": False, "source": "duration-level-ii", "skin": "ap-", "before": ["orbit.jsx"], "after": ["duration.jsx"],
        "success": (6.4, 2.6), "glitch": (6.4, 3.8), "failure": (6.4, 3.6),
        "times": [0.5, 2.0, 2.6, 3.4, 4.4, 6.3],
    },
    "apollo-lunar-flyby": {
        "pad": False, "source": "lunar-flyby", "skin": "ap-", "before": ["orbit.jsx"],
        "success": (6.4, 3.6), "glitch": (6.4, 4.4), "failure": (6.4, 4.4),
        "times": [0.5, 2.0, 3.0, 3.6, 4.4, 6.3],
    },
    "apollo-tei": {
        "pad": False, "source": "tei", "skin": "ap-", "before": ["orbit.jsx"],
        "success": (6.0, 2.4), "glitch": (6.0, 3.0), "failure": (6.0, 3.0),
        "times": [0.5, 1.4, 2.0, 2.8, 3.8, 5.9],
    },
    "apollo-reentry": {
        "pad": False, "source": "reentry", "skin": "ap-", "mirror": True,
        "success": (6.4, 4.0), "glitch": (6.4, 4.4), "failure": (6.4, 3.4),
        "times": [0.4, 1.6, 2.6, 3.4, 4.6, 6.3],
    },
    "apollo-splashdown-and-recovery": {
        "pad": False, "source": "splashdown-and-recovery", "skin": "ap-", "after": ["touchdown.jsx"],
        "success": (6.0, 1.2), "glitch": (6.0, 1.8), "failure": (6.0, 1.8),
        "times": [0.5, 1.2, 1.8, 2.6, 3.6, 5.9],
    },
    "apollo-ascent": {
        "pad": False, "source": "ascent", "skin": "ap-", "alias": {"debris-1": "ap-debris-1-bare"},
        "success": (6.5, 3.6), "glitch": (6.5, 4.0), "failure": (7.0, 3.4),
        "times": [0.0, 1.5, 2.9, 3.4, 4.2, 6.4],
    },
    "apollo-deployment-stack": {
        "pad": False, "source": "deployment", "skin": "ap-", "alias": {"d-payload": "ap-d-payload-stack"},
        "success": (6.0, 2.6), "glitch": (6.0, 2.6), "failure": (6.5, 2.3),
        "times": [0.5, 1.2, 1.9, 2.4, 3.6, 5.9],
    },
    "apollo-earth-orbit-stack": {
        "pad": False, "source": "earth-orbit", "skin": "ap-", "alias": {"o-sat": "ap-o-sat-stack"},
        "success": (6.0, 2.1), "glitch": (6.0, 3.4), "failure": (6.5, 3.0),
        "times": [0.5, 2.0, 2.8, 3.6, 4.6, 5.9],
    },
    "apollo-tli": {
        "pad": False, "source": "tpi", "skin": "ap-", "alias": {"o-sat": "ap-o-sat-stack"}, "before": ["orbit.jsx"],
        "success": (6.0, 2.0), "glitch": (6.0, 3.2), "failure": (6.0, 3.0),
        "times": [0.5, 1.4, 2.0, 2.8, 3.8, 5.9],
    },
    "apollo-lunar-approach": {
        "pad": False, "source": "lunar-approach", "skin": "ap-", "alias": {"o-sat": "ap-o-sat-lm"}, "before": ["orbit.jsx"],
        "success": (6.0, 2.4), "glitch": (6.0, 3.6), "failure": (6.0, 3.4),
        "times": [0.5, 1.8, 2.4, 3.2, 4.2, 5.9],
    },
    "apollo-lunar-approach-csm": {
        "pad": False, "source": "lunar-approach", "skin": "ap-", "before": ["orbit.jsx"],
        "success": (6.0, 2.4), "glitch": (6.0, 3.6), "failure": (6.0, 3.4),
        "times": [0.5, 1.8, 2.4, 3.2, 4.2, 5.9],
    },
    "apollo-loi": {
        "pad": False, "source": "loi", "skin": "ap-", "alias": {"o-sat": "ap-o-sat-lm"}, "before": ["orbit.jsx"],
        "success": (6.4, 2.5), "glitch": (6.4, 2.8), "failure": (6.4, 2.8),
        "times": [0.6, 1.2, 1.8, 2.5, 3.6, 6.3],
    },
    "apollo-loi-csm": {
        "pad": False, "source": "loi", "skin": "ap-", "before": ["orbit.jsx"],
        "success": (6.4, 2.5), "glitch": (6.4, 2.8), "failure": (6.4, 2.8),
        "times": [0.6, 1.2, 1.8, 2.5, 3.6, 6.3],
    },
    "apollo-lunar-orbit": {
        "pad": False, "source": "lunar-orbit", "skin": "ap-", "alias": {"o-sat": "ap-o-sat-lm"}, "before": ["orbit.jsx"], "after": ["planet-orbit.jsx"],
        "success": (6.0, 2.4), "glitch": (6.0, 3.6), "failure": (6.0, 3.6),
        "times": [0.5, 1.8, 2.4, 3.2, 4.2, 5.9],
    },
    "apollo-lunar-orbit-csm": {
        "pad": False, "source": "lunar-orbit", "skin": "ap-", "before": ["orbit.jsx"], "after": ["planet-orbit.jsx"],
        "success": (6.0, 2.4), "glitch": (6.0, 3.6), "failure": (6.0, 3.6),
        "times": [0.5, 1.8, 2.4, 3.2, 4.2, 5.9],
    },
    "apollo-tlc": {
        "pad": False, "source": "trans-lunar-coast", "skin": "ap-", "alias": {"m-probe": "ap-m-probe"}, "after": ["moon-coast.jsx"],
        "success": (6.4, 2.4), "glitch": (6.4, 3.6), "failure": (6.4, 3.4),
        "times": [0.5, 1.8, 2.4, 3.2, 4.4, 6.3],
    },
    "apollo-tlc-csm": {
        "pad": False, "source": "trans-lunar-coast", "skin": "ap-", "alias": {"m-probe": "ap-m-probe-csm"}, "after": ["moon-coast.jsx"],
        "success": (6.4, 2.4), "glitch": (6.4, 3.6), "failure": (6.4, 3.4),
        "times": [0.5, 1.8, 2.4, 3.2, 4.4, 6.3],
    },
    "apollo-tec": {
        "pad": False, "source": "trans-earth-coast", "skin": "ap-", "alias": {"m-probe": "ap-m-probe-csm"}, "after": ["moon-coast.jsx"],
        "success": (6.4, 2.4), "glitch": (6.4, 3.6), "failure": (6.4, 3.4),
        "times": [0.5, 1.8, 2.4, 3.2, 4.4, 6.3],
    },
    "apollo-descent": {
        "pad": False, "source": "descent-chutes", "skin": "ap-", "after": ["descent.jsx"],
        "success": (6.0, 2.4), "glitch": (6.0, 3.4), "failure": (6.0, 2.6),
        "times": [0.5, 1.4, 2.2, 3.0, 4.2, 5.9],
    },
    "apollo-reentry-preparations": {
        "pad": False, "source": "reentry-preparations", "skin": "ap-", "mirror": True, "before": ["project-apollo.jsx", "motes.jsx"],
        "success": (6.0, 3.0), "glitch": (6.0, 3.6), "failure": (6.5, 3.4),
        "times": [0.5, 1.6, 2.4, 3.2, 4.4, 5.9],
    },
    "apollo-retrofire": {
        "pad": False, "source": "retrofire", "skin": "ap-", "mirror": True, "before": ["project-apollo.jsx", "motes.jsx"],
        "success": (6.4, 2.6), "glitch": (6.4, 3.0), "failure": (6.4, 3.0),
        "times": [0.5, 1.4, 2.0, 2.8, 3.8, 6.3],
    },
    "apollo-modules-separation": {
        "pad": False, "source": "modules-separation", "skin": "ap-", "mirror": True, "before": ["project-apollo.jsx", "motes.jsx"],
        "success": (5.6, 1.8), "glitch": (5.6, 3.6), "failure": (6.4, 3.8),
        "times": [0.5, 1.0, 1.6, 2.6, 3.6, 5.5],
    },
    # Apollo's lunar module (2026-10-18, user; ap-n-, ap-k- parts): in the lander's scenes, the transposition and
    # extraction (the CSM, the S-IVB), docking (the lander whole, or its ascent stage after the Moon), the ascent.
    "apollo-transposition": {
        "pad": False, "source": "transposition", "skin": "ap-", "before": ["dock.jsx", "moon.jsx", "apollo.jsx"],
        "success": (6.0, 3.0), "glitch": (6.4, 4.6), "failure": (6.4, 4.6),
        "times": [0.6, 1.6, 2.6, 3.4, 4.6, 5.9],
    },
    "apollo-extraction": {
        "pad": False, "source": "extraction", "skin": "ap-", "before": ["dock.jsx", "moon.jsx", "apollo.jsx"],
        "success": (5.6, 1.2), "glitch": (5.6, 2.2), "failure": (5.6, 2.6),
        "times": [0.5, 1.2, 1.9, 2.6, 3.6, 5.5],
    },
    "apollo-docking": {
        "pad": False, "source": "docking", "skin": "ap-", "before": ["dock.jsx"],
        "success": (6.4, 2.0), "glitch": (6.4, 3.4), "failure": (6.4, 3.4),
        "times": [0.5, 1.7, 2.3, 3.1, 4.2, 6.3],
    },
    "apollo-undocking": {
        "pad": False, "source": "undocking", "skin": "ap-", "before": ["dock.jsx"],
        "success": (6.4, 1.2), "glitch": (6.4, 2.8), "failure": (6.4, 2.8),
        "times": [0.5, 1.2, 2.0, 2.6, 3.8, 6.3],
    },
    "apollo-lander-engine-test": {
        "pad": False, "source": "lander-engine-test", "skin": "ap-", "before": ["moon.jsx", "lander-orbit.jsx"],
        "success": (6.0, 1.4), "glitch": (6.0, 2.0), "failure": (6.0, 2.0),
        "times": [0.6, 1.2, 1.8, 2.4, 3.6, 5.9],
    },
    "apollo-lander-staging": {
        "pad": False, "source": "lander-staging", "skin": "ap-", "before": ["moon.jsx", "lander-orbit.jsx"],
        "success": (6.0, 1.4), "glitch": (6.0, 2.0), "failure": (6.0, 2.0),
        "times": [0.6, 1.2, 1.8, 2.4, 3.6, 5.9],
    },
    "apollo-powered-descent": {
        "pad": False, "source": "powered-descent", "skin": "ap-", "before": ["moon.jsx"],
        "success": (6.4, 2.4), "glitch": (6.4, 3.4), "failure": (6.4, 3.2),
        "times": [0.5, 2.0, 2.6, 3.4, 4.6, 6.3],
    },
    "apollo-lunar-landing": {
        "pad": False, "source": "lunar-landing", "skin": "ap-", "before": ["moon.jsx"],
        "success": (6.0, 2.6), "glitch": (6.0, 3.6), "failure": (6.0, 3.2),
        "times": [0.5, 1.6, 2.4, 3.0, 3.6, 5.9],
    },
    "apollo-lunar-liftoff": {
        "pad": False, "source": "lunar-liftoff", "skin": "ap-", "before": ["moon.jsx"],
        "success": (5.6, 1.0), "glitch": (5.6, 2.0), "failure": (5.6, 2.8),
        "times": [0.7, 1.2, 1.8, 2.6, 3.4, 5.5],
    },
    "apollo-moonwalk": {
        "pad": False, "source": "moonwalk", "skin": "ap-", "before": ["moon.jsx"],
        "success": (5.4, 2.6), "glitch": (5.8, 3.2), "failure": (5.8, 3.0),
        "times": [0.6, 1.2, 2.2, 2.6, 3.4, 5.3],
    },
    "apollo-docking-asc": {
        "pad": False, "source": "docking", "skin": "ap-", "alias": {"k-target": "ap-k-target-asc"}, "before": ["dock.jsx"],
        "success": (6.4, 2.0), "glitch": (6.4, 3.4), "failure": (6.4, 3.4),
        "times": [0.5, 1.7, 2.3, 3.1, 4.2, 6.3],
    },
    "apollo-lem-jettison": {
        "pad": False, "source": "undocking", "skin": "ap-", "alias": {"k-target": "ap-k-target-asc"}, "before": ["dock.jsx"],
        "success": (6.4, 1.2), "glitch": (6.4, 2.8), "failure": (6.4, 2.8),
        "times": [0.5, 1.2, 2.0, 2.6, 3.8, 6.3],
    },
    "apollo-lunar-ascent": {
        "pad": False, "source": "lunar-ascent", "skin": "ap-", "alias": {"o-sat": "ap-o-sat-asc", "o-sat-b": "ap-o-sat"}, "before": ["orbit.jsx"],
        "success": (6.0, 1.8), "glitch": (6.0, 2.6), "failure": (6.0, 2.8),
        "times": [0.5, 1.2, 1.8, 2.6, 3.4, 5.9],
    },
    # Project Gemini's own (2026-10-17): the generic timelines ("source") with its spacecraft ("skin": the gm- parts),
    # its adapter's equipment section on until the reentry preparations drop it (adapter.jsx), its retros
    # rippled (project-gemini.jsx) and its descent on two points (two-point.jsx); and the Agena's launch (ag-).
    "gemini-launch-preparations": {
        "pad": True, "source": "launch-preparations", "skin": "gm-",
        "success": (6.4, 2.6), "glitch": (6.4, 3.5), "failure": (7.0, 3.1),
        "times": [0.5, 2.0, 2.8, 3.4, 4.6, 6.4],
    },
    "gemini-countdown": {
        "pad": True, "source": "countdown", "skin": "gm-",
        "success": (6.4, 4.8), "glitch": (7.2, 3.6), "failure": (7.0, 4.7),
        "times": [0.0, 2.2, 3.0, 4.4, 4.8, 6.4],
    },
    "gemini-ascent": {
        "pad": False, "source": "ascent", "skin": "gm-",
        "success": (6.5, 3.6), "glitch": (6.5, 4.0), "failure": (7.0, 3.4),
        "times": [0.0, 1.5, 2.9, 3.4, 4.2, 6.4],
    },
    "gemini-deployment": {
        "pad": False, "source": "deployment", "skin": "gm-",
        "success": (6.0, 2.6), "glitch": (6.0, 2.6), "failure": (6.5, 2.3),
        "times": [0.5, 1.2, 1.9, 2.4, 3.6, 5.9],
    },
    "gemini-earth-orbit": {
        "pad": False, "source": "earth-orbit", "skin": "gm-",
        "success": (6.0, 2.1), "glitch": (6.0, 3.4), "failure": (6.5, 3.0),
        "times": [0.5, 2.0, 2.8, 3.6, 4.6, 5.9],
    },
    "gemini-insertion": {
        "pad": False, "source": "insertion", "skin": "gm-", "before": ["orbit.jsx"],
        "success": (6.0, 2.4), "glitch": (6.0, 3.2), "failure": (6.0, 3.0),
        "times": [0.4, 1.2, 1.8, 2.4, 3.2, 5.9],
    },
    "gemini-duration-level-ii": {
        "pad": False, "source": "duration-level-ii", "skin": "gm-", "before": ["orbit.jsx"], "after": ["duration.jsx"],
        "success": (6.4, 2.6), "glitch": (6.4, 3.8), "failure": (6.4, 3.6),
        "times": [0.5, 2.0, 2.6, 3.4, 4.4, 6.3],
    },
    "gemini-suborbital-flight": {
        "pad": False, "mirror": True, "source": "suborbital-flight", "skin": "gm-", "before": ["motes.jsx"], "after": ["adapter.jsx"],
        "success": (6.4, 3.4), "glitch": (6.4, 4.0), "failure": (6.4, 3.6),
        "times": [0.5, 1.8, 2.8, 3.4, 4.4, 6.3],
    },
    "gemini-reentry-trim": {
        "pad": False, "mirror": True, "source": "reentry-trim", "skin": "gm-", "before": ["motes.jsx", "gemini-esep-trim.jsx"], "after": ["adapter.jsx"],
        "success": (6.0, 3.0), "glitch": (6.0, 3.4), "failure": (6.0, 3.0),
        "times": [0.5, 1.6, 2.4, 3.0, 4.0, 5.9],
    },
    "gemini-reentry-preparations": {
        "pad": False, "mirror": True, "source": "reentry-preparations", "skin": "gm-", "before": ["motes.jsx", "gemini-esep.jsx"], "after": ["adapter.jsx"],
        "success": (6.0, 3.0), "glitch": (6.0, 3.6), "failure": (6.5, 3.4),
        "times": [0.5, 1.6, 2.4, 3.2, 4.4, 5.9],
    },
    "gemini-retrofire": {
        "pad": False, "mirror": True, "source": "retrofire", "skin": "gm-", "before": ["motes.jsx", "project-gemini.jsx"],
        "success": (6.4, 2.6), "glitch": (6.4, 3.0), "failure": (6.4, 3.0),
        "times": [0.5, 1.4, 2.0, 2.8, 3.8, 6.3],
    },
    "gemini-retrofire-jettison": {
        "pad": False, "mirror": True, "source": "retrofire-jettison", "skin": "gm-", "before": ["motes.jsx"],
        "success": (5.6, 1.6), "glitch": (5.6, 3.4), "failure": (6.4, 3.2),
        "times": [0.5, 1.1, 1.8, 2.6, 3.8, 5.5],
    },
    "gemini-reentry": {
        "pad": False, "mirror": True, "source": "reentry", "skin": "gm-",
        "success": (6.4, 4.0), "glitch": (6.4, 4.4), "failure": (6.4, 3.4),
        "times": [0.4, 1.6, 2.6, 3.4, 4.6, 6.3],
    },
    "gemini-descent-and-splashdown": {
        "pad": False, "source": "descent-and-splashdown", "skin": "gm-", "before": ["two-point.jsx"], "after": ["descent.jsx"],
        "success": (6.4, 2.4), "glitch": (6.4, 3.4), "failure": (6.4, 2.6),
        "times": [0.5, 1.4, 2.2, 3.0, 4.2, 6.3],
    },
    "gemini-recovery": {
        "pad": False, "source": "recovery", "skin": "gm-",
        "success": (6.4, 2.0), "glitch": (6.4, 3.0), "failure": (6.4, 3.4),
        "times": [0.5, 1.6, 2.6, 3.4, 4.4, 6.3],
    },
    # Gemini's spacewalks in steps (2026-10-18, user): out on the tether (a lone spacewalk); or an experiment set
    # up, run and taken down. The docking flights' stand-up, out and photo are docked, below.
    "gemini-eva-out": {
        "pad": False, "source": "eva-out", "skin": "gm-", "alias": {"r-capsule": "gm-e-capsule"}, "before": ["spacewalk.jsx"],
        "success": (5.6, 2.6), "glitch": (6.0, 3.2), "failure": (6.6, 3.2),
        "times": [1.2, 2.2, 2.8, 3.4, 4.4, 5.5],
    },
    "gemini-eva-setup": {
        "pad": False, "source": "eva-setup", "skin": "gm-", "alias": {"r-capsule": "gm-e-capsule"}, "before": ["spacewalk.jsx"],
        "success": (4.6, 3.1), "glitch": (5.4, 3.6), "failure": (6.6, 3.8),
        "times": [1.2, 2.4, 3.0, 3.5, 4.0, 4.5],
    },
    "gemini-eva-experiments": {
        "pad": False, "source": "eva-experiments", "skin": "gm-", "alias": {"r-capsule": "gm-e-capsule"}, "before": ["spacewalk.jsx"],
        "success": (5.0, 1.6), "glitch": (5.0, 2.2), "failure": (4.6, 2.7),
        "times": [0.7, 1.2, 1.6, 2.1, 2.7, 4.5],
    },
    "gemini-eva-teardown": {
        "pad": False, "source": "eva-teardown", "skin": "gm-", "alias": {"r-capsule": "gm-e-capsule"}, "before": ["spacewalk.jsx"],
        "success": (4.8, 1.0), "glitch": (5.6, 1.8), "failure": (5.2, 2.0),
        "times": [0.6, 1.0, 1.5, 2.0, 3.0, 4.7],
    },
    # Docked to the Agena (2026-10-18, user; Gemini 10, 11 and 12 had theirs so), the docking flights' three: a
    # stand-up in the hatch (egress), out on the tether on from the stand-up, a photo and back in.
    "gemini-eva-standup-docked": {
        "pad": False, "source": "eva-standup", "skin": "gm-", "alias": {"r-capsule": "gm-e-capsule"}, "before": ["eva-docked.jsx", "spacewalk.jsx"],
        "success": (4.6, 1.0), "glitch": (5.6, 1.6), "failure": (4.2, 1.7),
        "times": [0.6, 1.0, 1.6, 2.2, 3.2, 4.5],
    },
    "gemini-eva-out-docked": {
        "pad": False, "source": "eva-out-standing", "skin": "gm-", "alias": {"r-capsule": "gm-e-capsule"}, "before": ["eva-docked.jsx", "spacewalk.jsx"], "after": ["eva-out.jsx"],
        "success": (4.2, 1.2), "glitch": (4.6, 1.8), "failure": (5.2, 1.8),
        "times": [0.2, 0.8, 1.4, 2.0, 3.0, 4.1],
    },
    "gemini-eva-photo-docked": {
        "pad": False, "source": "eva-photo", "skin": "gm-", "alias": {"r-capsule": "gm-e-capsule"}, "before": ["eva-docked.jsx", "spacewalk.jsx"],
        "success": (5.2, 1.7), "glitch": (5.8, 2.4), "failure": (5.8, 2.3),
        "times": [0.6, 1.2, 1.7, 2.4, 3.4, 5.1],
    },
    # On a rendezvous (2026-10-18, user): the other Gemini a few metres off, its crewman taking pictures.
    "gemini-eva-pair": {
        "pad": False, "source": "eva-pair", "skin": "gm-", "alias": {"r-capsule": "gm-e-capsule"}, "before": ["spacewalk.jsx"],
        "success": (6.2, 2.7), "glitch": (6.4, 3.3), "failure": (7.0, 3.3),
        "times": [1.2, 2.4, 2.9, 3.3, 4.4, 6.1],
    },
    "gemini-rendezvous-target-approach": {
        "pad": False, "source": "rendezvous-target-approach", "skin": "gm-", "before": ["orbit.jsx"],
        "success": (6.0, 2.6), "glitch": (6.0, 3.0), "failure": (6.0, 3.0),
        "times": [0.5, 1.8, 2.4, 2.8, 4.0, 5.9],
    },
    "gemini-rendezvous": {
        "pad": False, "source": "rendezvous", "skin": "gm-", "before": ["dock.jsx"],
        "success": (6.0, 2.4), "glitch": (6.0, 3.4), "failure": (6.0, 3.4),
        "times": [0.5, 1.6, 2.4, 3.2, 4.2, 5.9],
    },
    "gemini-target-approach": {
        "pad": False, "source": "target-approach", "skin": "gm-", "before": ["dock.jsx"],
        "success": (6.4, 2.4), "glitch": (6.4, 2.6), "failure": (6.4, 2.6),
        "times": [0.5, 1.9, 2.3, 2.7, 3.6, 6.3],
    },
    "gemini-docking": {
        "pad": False, "source": "docking", "skin": "gm-", "before": ["dock.jsx"],
        "success": (6.4, 2.0), "glitch": (6.4, 3.4), "failure": (6.4, 3.4),
        "times": [0.5, 1.7, 2.3, 3.1, 4.2, 6.3],
    },
    # The lunar Gemini programs' Earth-orbit tests with their own hardware (2026-10-18, user; parts.py's gt-, gtd-,
    # tt-, gl- and gd-): Gemini's timelines and skin, the stack in its place. Gemini on its Transtage (the
    # circumlunar heat-shield test), and docked to one for the burn (the Transtage test).
    "gemini-transtage-launch-preparations": {
        "pad": True, "source": "launch-preparations", "skin": "gm-", "alias": {"rocket": "gt-rocket", "debris-1": "gt-debris-1", "d-payload": "gt-d-payload", "o-sat": "gt-o-sat"},
        "success": (6.4, 2.6), "glitch": (6.4, 3.5), "failure": (7.0, 3.1),
        "times": [0.5, 2.0, 2.8, 3.4, 4.6, 6.4],
    },
    "gemini-transtage-countdown": {
        "pad": True, "source": "countdown", "skin": "gm-", "alias": {"rocket": "gt-rocket", "debris-1": "gt-debris-1", "d-payload": "gt-d-payload", "o-sat": "gt-o-sat"},
        "success": (6.4, 4.8), "glitch": (7.2, 3.6), "failure": (7.0, 4.7),
        "times": [0.0, 2.2, 3.0, 4.4, 4.8, 6.4],
    },
    "gemini-transtage-ascent": {
        "pad": False, "source": "ascent", "skin": "gm-", "alias": {"rocket": "gt-rocket", "debris-1": "gt-debris-1", "d-payload": "gt-d-payload", "o-sat": "gt-o-sat"},
        "success": (6.5, 3.6), "glitch": (6.5, 4.0), "failure": (7.0, 3.4),
        "times": [0.0, 1.5, 2.9, 3.4, 4.2, 6.4],
    },
    "gemini-transtage-deployment": {
        "pad": False, "source": "deployment", "skin": "gm-", "alias": {"rocket": "gt-rocket", "debris-1": "gt-debris-1", "d-payload": "gt-d-payload", "o-sat": "gt-o-sat"},
        "success": (6.0, 2.6), "glitch": (6.0, 2.6), "failure": (6.5, 2.3),
        "times": [0.5, 1.2, 1.9, 2.4, 3.6, 5.9],
    },
    "gemini-transtage-earth-orbit": {
        "pad": False, "source": "earth-orbit", "skin": "gm-", "alias": {"rocket": "gt-rocket", "debris-1": "gt-debris-1", "d-payload": "gt-d-payload", "o-sat": "gt-o-sat"},
        "success": (6.0, 2.1), "glitch": (6.0, 3.4), "failure": (6.5, 3.0),
        "times": [0.5, 2.0, 2.8, 3.6, 4.6, 5.9],
    },
    # Circumlunar Gemini's heat-shield test (2026-10-18, user): the Transtage's burn stretches the orbit into a long
    # ellipse (no trans-lunar injection: nothing leaves the Earth), and the capsule coasts out along it
    # (high-orbit-scene.jsx); the Transtage test burns docked.
    "gemini-transtage-hoi": {
        "pad": False, "source": "high-orbit-injection", "skin": "gm-", "alias": {"o-sat": "gt-o-sat"}, "before": ["orbit.jsx", "high-orbit-scene.jsx"],
        "success": (6.0, 2.0), "glitch": (6.0, 3.2), "failure": (6.0, 3.0),
        "times": [0.5, 1.4, 2.0, 2.8, 3.8, 5.9],
    },
    "gemini-transtage-docked-hoi": {
        "pad": False, "source": "high-orbit-injection", "skin": "gm-", "alias": {"o-sat": "gtd-o-sat"}, "before": ["orbit.jsx", "high-orbit-scene.jsx"],
        "success": (6.0, 2.0), "glitch": (6.0, 3.2), "failure": (6.0, 3.0),
        "times": [0.5, 1.4, 2.0, 2.8, 3.8, 5.9],
    },
    "gemini-high-orbit": {
        "pad": False, "source": "high-orbit", "skin": "gm-", "before": ["orbit.jsx", "high-orbit-scene.jsx"],
        "success": (6.0, 2.1), "glitch": (6.0, 3.4), "failure": (6.0, 3.4),
        "times": [0.5, 1.8, 2.2, 3.0, 4.0, 5.9],
    },
    # The Transtage alone: its own launch (as the Agena's), and the target of the Transtage test's docking.
    "transtage-launch-preparations": {
        "pad": True, "source": "launch-preparations", "skin": "tt-",
        "success": (6.4, 2.6), "glitch": (6.4, 3.5), "failure": (7.0, 3.1),
        "times": [0.5, 2.0, 2.8, 3.4, 4.6, 6.4],
    },
    "transtage-countdown": {
        "pad": True, "source": "countdown", "skin": "tt-",
        "success": (6.4, 4.8), "glitch": (7.2, 3.6), "failure": (7.0, 4.7),
        "times": [0.0, 2.2, 3.0, 4.4, 4.8, 6.4],
    },
    "transtage-ascent": {
        "pad": False, "source": "ascent", "skin": "tt-",
        "success": (6.5, 3.6), "glitch": (6.5, 4.0), "failure": (7.0, 3.4),
        "times": [0.0, 1.5, 2.9, 3.4, 4.2, 6.4],
    },
    "transtage-deployment": {
        "pad": False, "source": "deployment", "skin": "tt-",
        "success": (6.0, 2.6), "glitch": (6.0, 2.6), "failure": (6.5, 2.3),
        "times": [0.5, 1.2, 1.9, 2.4, 3.6, 5.9],
    },
    "transtage-target-approach": {
        "pad": False, "source": "target-approach", "skin": "gm-", "alias": {"k-target": "tt-k-target"}, "before": ["dock.jsx"],
        "success": (6.4, 2.4), "glitch": (6.4, 2.6), "failure": (6.4, 2.6),
        "times": [0.5, 1.9, 2.3, 2.7, 3.6, 6.3],
    },
    "transtage-docking": {
        "pad": False, "source": "docking", "skin": "gm-", "alias": {"k-target": "tt-k-target"}, "before": ["dock.jsx"],
        "success": (6.4, 2.0), "glitch": (6.4, 3.4), "failure": (6.4, 3.4),
        "times": [0.5, 1.7, 2.3, 3.1, 4.2, 6.3],
    },
    "transtage-undocking": {
        "pad": False, "source": "undocking", "skin": "gm-", "alias": {"k-target": "tt-k-target"}, "before": ["dock.jsx"],
        "success": (6.4, 1.2), "glitch": (6.4, 2.8), "failure": (6.4, 2.8),
        "times": [0.5, 1.2, 2.0, 2.6, 3.8, 6.3],
    },
    # Gemini with the lunar propulsion module; the propulsion module test's burn is the insertion's.
    "gemini-lpm-launch-preparations": {
        "pad": True, "source": "launch-preparations", "skin": "gm-", "alias": {"rocket": "gl-rocket", "debris-1": "gl-debris-1", "d-payload": "gl-d-payload", "o-sat": "gl-o-sat"},
        "success": (6.4, 2.6), "glitch": (6.4, 3.5), "failure": (7.0, 3.1),
        "times": [0.5, 2.0, 2.8, 3.4, 4.6, 6.4],
    },
    "gemini-lpm-countdown": {
        "pad": True, "source": "countdown", "skin": "gm-", "alias": {"rocket": "gl-rocket", "debris-1": "gl-debris-1", "d-payload": "gl-d-payload", "o-sat": "gl-o-sat"},
        "success": (6.4, 4.8), "glitch": (7.2, 3.6), "failure": (7.0, 4.7),
        "times": [0.0, 2.2, 3.0, 4.4, 4.8, 6.4],
    },
    "gemini-lpm-ascent": {
        "pad": False, "source": "ascent", "skin": "gm-", "alias": {"rocket": "gl-rocket", "debris-1": "gl-debris-1", "d-payload": "gl-d-payload", "o-sat": "gl-o-sat"},
        "success": (6.5, 3.6), "glitch": (6.5, 4.0), "failure": (7.0, 3.4),
        "times": [0.0, 1.5, 2.9, 3.4, 4.2, 6.4],
    },
    "gemini-lpm-deployment": {
        "pad": False, "source": "deployment", "skin": "gm-", "alias": {"rocket": "gl-rocket", "debris-1": "gl-debris-1", "d-payload": "gl-d-payload", "o-sat": "gl-o-sat"},
        "success": (6.0, 2.6), "glitch": (6.0, 2.6), "failure": (6.5, 2.3),
        "times": [0.5, 1.2, 1.9, 2.4, 3.6, 5.9],
    },
    "gemini-lpm-insertion": {
        "pad": False, "source": "insertion", "skin": "gm-", "alias": {"rocket": "gl-rocket", "debris-1": "gl-debris-1", "d-payload": "gl-d-payload", "o-sat": "gl-o-sat"}, "before": ["orbit.jsx"],
        "success": (6.0, 2.4), "glitch": (6.0, 3.2), "failure": (6.0, 3.0),
        "times": [0.4, 1.2, 1.8, 2.4, 3.2, 5.9],
    },
    "gemini-lpm-earth-orbit": {
        "pad": False, "source": "earth-orbit", "skin": "gm-", "alias": {"rocket": "gl-rocket", "debris-1": "gl-debris-1", "d-payload": "gl-d-payload", "o-sat": "gl-o-sat"},
        "success": (6.0, 2.1), "glitch": (6.0, 3.4), "failure": (6.5, 3.0),
        "times": [0.5, 2.0, 2.8, 3.6, 4.6, 5.9],
    },
    "gemini-lpm-earth-approach": {
        "pad": False, "source": "earth-approach", "skin": "gm-", "alias": {"rocket": "gl-rocket", "debris-1": "gl-debris-1", "d-payload": "gl-d-payload", "o-sat": "gl-o-sat"}, "before": ["orbit.jsx"],
        "success": (6.0, 2.4), "glitch": (6.0, 3.4), "failure": (6.0, 3.4),
        "times": [0.5, 1.8, 2.6, 3.4, 4.4, 5.9],
    },
    # The direct-ascent craft; its two module tests' burns are the insertion's.
    "gemini-da-launch-preparations": {
        "pad": True, "source": "launch-preparations", "skin": "gm-", "alias": {"rocket": "gd-rocket", "debris-1": "gd-debris-1", "d-payload": "gd-d-payload", "o-sat": "gd-o-sat"},
        "success": (6.4, 2.6), "glitch": (6.4, 3.5), "failure": (7.0, 3.1),
        "times": [0.5, 2.0, 2.8, 3.4, 4.6, 6.4],
    },
    "gemini-da-countdown": {
        "pad": True, "source": "countdown", "skin": "gm-", "alias": {"rocket": "gd-rocket", "debris-1": "gd-debris-1", "d-payload": "gd-d-payload", "o-sat": "gd-o-sat"},
        "success": (6.4, 4.8), "glitch": (7.2, 3.6), "failure": (7.0, 4.7),
        "times": [0.0, 2.2, 3.0, 4.4, 4.8, 6.4],
    },
    "gemini-da-ascent": {
        "pad": False, "source": "ascent", "skin": "gm-", "alias": {"rocket": "gd-rocket", "debris-1": "gd-debris-1", "d-payload": "gd-d-payload", "o-sat": "gd-o-sat"},
        "success": (6.5, 3.6), "glitch": (6.5, 4.0), "failure": (7.0, 3.4),
        "times": [0.0, 1.5, 2.9, 3.4, 4.2, 6.4],
    },
    "gemini-da-deployment": {
        "pad": False, "source": "deployment", "skin": "gm-", "alias": {"rocket": "gd-rocket", "debris-1": "gd-debris-1", "d-payload": "gd-d-payload", "o-sat": "gd-o-sat"},
        "success": (6.0, 2.6), "glitch": (6.0, 2.6), "failure": (6.5, 2.3),
        "times": [0.5, 1.2, 1.9, 2.4, 3.6, 5.9],
    },
    "gemini-da-insertion": {
        "pad": False, "source": "insertion", "skin": "gm-", "alias": {"rocket": "gd-rocket", "debris-1": "gd-debris-1", "d-payload": "gd-d-payload", "o-sat": "gd-o-sat"}, "before": ["orbit.jsx"],
        "success": (6.0, 2.4), "glitch": (6.0, 3.2), "failure": (6.0, 3.0),
        "times": [0.4, 1.2, 1.8, 2.4, 3.2, 5.9],
    },
    "gemini-da-earth-orbit": {
        "pad": False, "source": "earth-orbit", "skin": "gm-", "alias": {"rocket": "gd-rocket", "debris-1": "gd-debris-1", "d-payload": "gd-d-payload", "o-sat": "gd-o-sat"},
        "success": (6.0, 2.1), "glitch": (6.0, 3.4), "failure": (6.5, 3.0),
        "times": [0.5, 2.0, 2.8, 3.6, 4.6, 5.9],
    },
    "agena-launch-preparations": {
        "pad": True, "source": "launch-preparations", "skin": "ag-",
        "success": (6.4, 2.6), "glitch": (6.4, 3.5), "failure": (7.0, 3.1),
        "times": [0.5, 2.0, 2.8, 3.4, 4.6, 6.4],
    },
    "agena-countdown": {
        "pad": True, "source": "countdown", "skin": "ag-",
        "success": (6.4, 4.8), "glitch": (7.2, 3.6), "failure": (7.0, 4.7),
        "times": [0.0, 2.2, 3.0, 4.4, 4.8, 6.4],
    },
    "agena-ascent": {
        "pad": False, "source": "ascent", "skin": "ag-",
        "success": (6.5, 3.6), "glitch": (6.5, 4.0), "failure": (7.0, 3.4),
        "times": [0.0, 1.5, 2.9, 3.4, 4.2, 6.4],
    },
    "agena-deployment": {
        "pad": False, "source": "deployment", "skin": "ag-",
        "success": (6.0, 2.6), "glitch": (6.0, 2.6), "failure": (6.5, 2.3),
        "times": [0.5, 1.2, 1.9, 2.4, 3.6, 5.9],
    },
    # The OKB-MiG E-154's own (2026-10-17): the X-15's timelines ("source") with its craft (e154.jsx, read by craft.jsx).
    "e154-takeoff": {
        "pad": False, "source": "air-takeoff", "before": ["moon.jsx", "e154.jsx", "craft.jsx", "air.jsx"],
        "success": (6.0, 2.0), "glitch": (6.0, 3.2), "failure": (6.0, 2.8),
        "times": [0.5, 1.6, 2.2, 2.8, 3.6, 5.9],
    },
    "e154-cruise": {
        "pad": False, "source": "air-cruise", "before": ["moon.jsx", "e154.jsx", "craft.jsx", "air.jsx"],
        "success": (5.6, 1.8), "glitch": (5.6, 2.8), "failure": (5.6, 2.8),
        "times": [0.5, 1.6, 2.2, 2.8, 3.6, 5.5],
    },
    "e154-drop": {
        "pad": False, "source": "air-drop", "before": ["moon.jsx", "e154.jsx", "craft.jsx", "air.jsx"],
        "success": (6.0, 1.6), "glitch": (6.0, 2.6), "failure": (6.0, 2.6),
        "times": [0.5, 1.0, 1.6, 2.4, 3.4, 5.9],
    },
    "e154-climb": {
        "pad": False, "source": "rocket-climb", "before": ["moon.jsx", "e154.jsx", "craft.jsx", "air.jsx"],
        "success": (6.0, 2.4), "glitch": (6.0, 3.2), "failure": (6.0, 3.2),
        "times": [0.5, 1.8, 2.4, 3.0, 4.0, 5.9],
    },
    "e154-peak": {
        "pad": False, "source": "x15-peak", "before": ["moon.jsx", "e154.jsx", "craft.jsx", "air.jsx", "x15.jsx"],
        "success": (6.0, 2.2), "glitch": (6.0, 3.2), "failure": (6.0, 3.2),
        "times": [0.5, 1.3, 2.4, 3.0, 3.6, 5.9],
    },
    "e154-fast": {
        "pad": False, "source": "x15-fast", "before": ["moon.jsx", "e154.jsx", "craft.jsx", "air.jsx", "x15.jsx"],
        "success": (6.0, 2.4), "glitch": (6.0, 2.8), "failure": (6.0, 2.9),
        "times": [0.5, 1.8, 2.4, 3.0, 3.8, 5.9],
    },
    "e154-descent": {
        "pad": False, "source": "x15-descent", "before": ["moon.jsx", "e154.jsx", "craft.jsx", "air.jsx", "x15.jsx"],
        "success": (6.0, 2.0), "glitch": (6.0, 2.7), "failure": (6.0, 3.0),
        "times": [0.5, 2.0, 2.6, 3.2, 4.2, 5.9],
    },
    "e154-landing": {
        "pad": False, "source": "runway-landing", "before": ["moon.jsx", "e154.jsx", "craft.jsx"],
        "success": (6.0, 2.6), "glitch": (6.4, 3.2), "failure": (6.0, 3.2),
        "times": [0.8, 2.0, 2.6, 3.2, 4.0, 5.9],
    },
    # The X-15's own (2026-10-17): x15.jsx's plane on its own, the peak, the fast run and the way down.
    "x15-peak": {
        "pad": False, "before": ["moon.jsx", "craft.jsx", "air.jsx", "x15.jsx"],
        "success": (6.0, 2.2), "glitch": (6.0, 3.2), "failure": (6.0, 3.2),
        "times": [0.5, 1.3, 2.4, 3.0, 3.6, 5.9],
    },
    "x15-fast": {
        "pad": False, "before": ["moon.jsx", "craft.jsx", "air.jsx", "x15.jsx"],
        "success": (6.0, 2.4), "glitch": (6.0, 2.8), "failure": (6.0, 2.9),
        "times": [0.5, 1.8, 2.4, 3.0, 3.8, 5.9],
    },
    "x15-descent": {
        "pad": False, "before": ["moon.jsx", "craft.jsx", "air.jsx", "x15.jsx"],
        "success": (6.0, 2.0), "glitch": (6.0, 2.7), "failure": (6.0, 3.0),
        "times": [0.5, 2.0, 2.6, 3.2, 4.2, 5.9],
    },
    # PrOP-M on Mars (2026-10-17): Mars 2's third season on the surface.
    "mars-rover": {
        "pad": False, "before": ["orbit.jsx"],
        "success": (6.4, 3.4), "glitch": (6.4, 3.6), "failure": (6.4, 3.6),
        "times": [0.8, 1.6, 2.6, 3.3, 4.2, 6.3],
    },
    # Viking's own (2026-10-17): the orbiter lets the lander capsule go, and it burns out of orbit.
    "viking-separation": {
        "pad": False,
        "success": (6.4, 2.7), "glitch": (6.4, 3.0), "failure": (6.4, 3.0),
        "times": [0.5, 1.2, 2.4, 2.8, 4.0, 6.3],
    },
    # Pioneer Venus Multiprobe's own (2026-10-17): multiprobe.jsx's bus and probes.
    "multiprobe-large-release": {
        "pad": False, "before": ["orbit.jsx", "multiprobe.jsx"], "after": ["multiprobe-release.jsx"],
        "success": (6.0, 1.2), "glitch": (6.0, 2.4), "failure": (6.0, 2.4),
        "times": [0.5, 1.2, 2.2, 2.8, 4.0, 5.9],
    },
    "multiprobe-small-release": {
        "pad": False, "before": ["orbit.jsx", "multiprobe.jsx"], "after": ["multiprobe-release.jsx"],
        "success": (6.0, 1.9), "glitch": (6.0, 2.5), "failure": (6.0, 2.6),
        "times": [0.5, 1.5, 2.0, 2.6, 3.4, 5.9],
    },
    "multiprobe-large-entry": {
        "pad": False, "before": ["orbit.jsx", "multiprobe.jsx"], "after": ["multiprobe-entry.jsx"],
        "success": (6.0, 3.7), "glitch": (6.0, 3.7), "failure": (6.0, 3.2),
        "times": [0.5, 1.6, 2.6, 3.2, 3.8, 5.9],
    },
    "multiprobe-small-entry": {
        "pad": False, "before": ["orbit.jsx", "multiprobe.jsx"], "after": ["multiprobe-entry.jsx"],
        "success": (6.0, 3.7), "glitch": (6.0, 3.7), "failure": (6.0, 3.2),
        "times": [0.5, 1.6, 2.6, 3.2, 3.8, 5.9],
    },
    "multiprobe-large-descent": {
        "pad": False, "before": ["orbit.jsx", "multiprobe.jsx"], "after": ["multiprobe-descent.jsx"],
        "success": (6.0, 3.2), "glitch": (6.0, 3.35), "failure": (6.0, 3.35),
        "times": [0.4, 0.9, 1.8, 3.2, 4.6, 5.9],
    },
    "multiprobe-small-descent": {
        "pad": False, "before": ["orbit.jsx", "multiprobe.jsx"], "after": ["multiprobe-descent.jsx"],
        "success": (6.6, 3.2), "glitch": (6.6, 3.35), "failure": (6.6, 3.35),
        "times": [0.5, 2.0, 3.2, 4.6, 5.4, 6.5],
    },
    # A lander's entry at Venus or Mars (2026-10-15): planet-entry.jsx, the reentry's story there.
    "venus-entry": {
        "pad": False, "after": ["planet-entry.jsx"],
        "success": (6.4, 4.0), "glitch": (6.4, 4.4), "failure": (6.4, 3.4),
        "times": [0.4, 1.6, 2.6, 3.4, 4.6, 6.3],
    },
    "mars-entry": {
        "pad": False, "after": ["planet-entry.jsx"],
        "success": (6.4, 4.0), "glitch": (6.4, 4.4), "failure": (6.4, 3.4),
        "times": [0.4, 1.6, 2.6, 3.4, 4.6, 6.3],
    },
    # A lander's descent at Venus or Mars (2026-10-15): planet-descent.jsx, Venera's chute or Viking's way down.
    "venus-descent": {
        "pad": False, "after": ["planet-descent.jsx"],
        "success": (6.0, 3.4), "glitch": (6.0, 3.5), "failure": (6.0, 3.5),
        "times": [0.4, 1.4, 2.6, 3.4, 4.4, 5.9],
    },
    "mars-descent": {
        "pad": False, "after": ["planet-descent.jsx"],
        "success": (6.0, 3.2), "glitch": (6.0, 3.5), "failure": (6.0, 3.3),
        "times": [0.3, 1.2, 2.2, 3.2, 4.2, 5.4],
    },
    # A lander's landing on Mars (2026-10-15): on from mars-descent, Viking 2's tilt, Mars 3's silence.
    "mars-landing": {
        "pad": False, "before": ["orbit.jsx"],
        "success": (6.0, 3.8), "glitch": (6.0, 2.8), "failure": (6.0, 3.8),
        "times": [0.0, 1.4, 2.4, 3.0, 3.9, 5.9],
    },
    # Work on the surface of Mars (2026-10-15): probe-surface's story with Viking's lander.
    "mars-surface": {
        "pad": False, "before": ["orbit.jsx"],
        "success": (6.0, 2.2), "glitch": (6.0, 2.8), "failure": (6.0, 2.6),
        "times": [0.8, 1.6, 2.2, 2.8, 3.6, 5.9],
    },
    # The carrier's entry at Venus (2026-10-15): it burns up in every variant; the data it sends differs.
    "bus-entry": {
        "pad": False, "before": ["orbit.jsx"],
        "success": (6.0, 2.1), "glitch": (6.0, 2.85), "failure": (6.0, 2.85),
        "times": [0.5, 1.4, 2.2, 3.0, 3.5, 4.1],
    },
    # The landers' tests in Earth orbit (2026-10-14): lander-orbit.jsx.
    "lander-engine-test": {
        "pad": False, "before": ["moon.jsx", "lander-orbit.jsx"],
        "success": (6.0, 1.4), "glitch": (6.0, 2.0), "failure": (6.0, 2.0),
        "times": [0.6, 1.2, 1.8, 2.4, 3.6, 5.9],
    },
    "lander-staging": {
        "pad": False, "before": ["moon.jsx", "lander-orbit.jsx"],
        "success": (6.0, 1.4), "glitch": (6.0, 2.0), "failure": (6.0, 2.0),
        "times": [0.6, 1.2, 1.8, 2.4, 3.6, 5.9],
    },
    # The air launch (2026-10-14): the carrier plane and the rocket plane (air.jsx), and the rocket plane alone.
    "air-takeoff": {
        "pad": False, "before": ["moon.jsx", "craft.jsx", "air.jsx"],
        "success": (6.0, 2.0), "glitch": (6.0, 3.2), "failure": (6.0, 2.8),
        "times": [0.5, 1.6, 2.2, 2.8, 3.6, 5.9],
    },
    "air-cruise": {
        "pad": False, "before": ["moon.jsx", "craft.jsx", "air.jsx"],
        "success": (5.6, 1.8), "glitch": (5.6, 2.8), "failure": (5.6, 2.8),
        "times": [0.5, 1.6, 2.2, 2.8, 3.6, 5.5],
    },
    "air-drop": {
        "pad": False, "before": ["moon.jsx", "craft.jsx", "air.jsx"],
        "success": (6.0, 1.6), "glitch": (6.0, 2.6), "failure": (6.0, 2.6),
        "times": [0.5, 1.0, 1.6, 2.4, 3.4, 5.9],
    },
    "rocket-climb": {
        "pad": False, "before": ["moon.jsx", "craft.jsx", "air.jsx"],
        "success": (6.0, 2.4), "glitch": (6.0, 3.2), "failure": (6.0, 3.2),
        "times": [0.5, 1.8, 2.4, 3.0, 4.0, 5.9],
    },
    "venus-mercury-transit": {
        "pad": False, "after": ["cruise.jsx"],
        "success": (6.4, 2.4), "glitch": (6.4, 3.6), "failure": (6.4, 3.4),
        "times": [0.5, 1.8, 2.4, 3.2, 4.4, 6.3],
    },
    "reentry": {
        "pad": False, "mirror": True,
        "success": (6.4, 4.0), "glitch": (6.4, 4.4), "failure": (6.4, 3.4),
        "times": [0.4, 1.6, 2.6, 3.4, 4.6, 6.3],
    },
    # Home from the Moon, the craft comes in from the left (earth-approach): the same reentry, unmirrored.
    "reentry-from-moon": {
        "pad": False, "source": "reentry",
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
        # "skin" (2026-10-17): a craft's own drawings, mc-<part> for <part> (motion.jsx's ALIAS); "alias"
        # names more, or other ones, for a step of its own.
        if STEPS[step].get("skin"):
            prefix = STEPS[step]["skin"]
            alias = {n[len(prefix):]: n for n in json.load(open(os.path.join(BUILD, "layers.json"))) if n.startswith(prefix)}
            alias.update(STEPS[step].get("alias", {}))
            f.write("var ALIAS = " + json.dumps(alias) + ";\n")
        f.write(f"var VARIANT = {json.dumps(variant)}, DUR = {dur};\n")
        # "mirror": the world drawn left for right (motion.jsx's setup), so the craft travels left.
        f.write(f"var MIRROR = {json.dumps(bool(STEPS[step].get('mirror')))};\n")
        f.write(read("..", "motion.jsx"))
        f.write(f"setup({json.dumps(step + ' ' + variant)}, DUR);\n")
        if STEPS[step]["pad"]:
            f.write(read("..", "launch-storyboard", "lib.jsx"))
            f.write(read("pad.jsx"))
        # A scene shared by several steps, before the step's own file uses it.
        for extra in STEPS[step].get("before", []):
            f.write(read(extra))
        # "source": another step's timeline, published under this name.
        f.write(read(f"{STEPS[step].get('source', step)}.jsx"))
        # A timeline shared by several steps, after the step's own file has set its switches.
        for extra in STEPS[step].get("after", []):
            f.write(read(extra))
    out = os.path.join(BUILD, f"{step}-{variant}.ecproj")
    subprocess.run([CLI, "script", script, "--empty", "--save-as", out], check=True, capture_output=True)
    return out


def render(step, variant, project_, out, kbps, extra):
    """Renders a variant to a WebM; a mirrored step's goes through ffmpeg's hflip, from a sharper render."""
    if not STEPS[step].get("mirror"):
        subprocess.run([CLI, "render", "--project", project_, "--comp", f"{step} {variant}", "--out", out,
                        "--format", "webm", "--webm-codec", "vp9", "--audio", "off", "--bitrate", str(kbps)] + extra,
                       check=True, capture_output=True)
        return
    raw = os.path.join(BUILD, f"{step}-{variant}.unflipped.webm")
    subprocess.run([CLI, "render", "--project", project_, "--comp", f"{step} {variant}", "--out", raw,
                    "--format", "webm", "--webm-codec", "vp9", "--audio", "off", "--bitrate", str(kbps * 6)] + extra,
                   check=True, capture_output=True)
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", raw, "-vf", "hflip", "-an", "-c:v", "libvpx-vp9",
                    "-b:v", f"{kbps}k", "-pix_fmt", "yuv420p", out], check=True)
    os.remove(raw)


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
                im = Image.open(out).convert("RGB")
                row.append(im.transpose(Image.Transpose.FLIP_LEFT_RIGHT) if spec.get("mirror") else im)
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
            render(step, v, projects[v], os.path.join(BUILD, f"{step}-{v}.webm"), 1500, [])
        if "publish" in sys.argv:
            # The game's copy: 960 x 540 (about the replay monitor's size at 1080p) and its sidecar.
            render(step, v, projects[v], os.path.join(GAME, f"{step}-{v}.webm"), 800, ["--resolution", "half"])
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
