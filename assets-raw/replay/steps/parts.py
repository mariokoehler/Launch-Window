"""Draws the layers of the step clips (launch preparations, countdown, ascent, deployment, Earth
orbit, and the way home: reentry preparations to descent and splashdown) in the launch's style, and copies in the launch storyboard's own layers, so the steps on the
pad match the launch pixel for pixel and the rocket in flight is the same rocket, cut at its stages.

python parts.py [name prefix ...] redraws only the parts whose names start so (it's slow).
Writes build/layers/<name>.png and build/layers.json: {name: {"x", "y", "w", "h", "px", "py"}} in
world px (1920 x 1080 at rest, y down), the pivot in px, py. A part is drawn on a large world canvas
and cropped to what it draws.

The craft is never a particular one: these steps play for every program, so the payload is the
launch rocket's own nose section.
"""
import io
import json
import math
import os
import re
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET

import resvg_py
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
BUILD = os.path.join(HERE, "build")
OUT = os.path.join(BUILD, "layers")
BOARD = os.path.join(HERE, "..", "launch-storyboard")
FONTS = os.path.normpath(os.path.join(HERE, "..", "..", "..", "assets", "fonts"))
SVGNS = "http://www.w3.org/2000/svg"
NS = "{" + SVGNS + "}"
ET.register_namespace("", SVGNS)

NAVY = "#16305c"
GOLD = "#e0a72e"
GOLD_FILL = "rgba(224,167,46,0.30)"
DEFS = ('<defs><pattern id="hatch" width="8" height="8" patternUnits="userSpaceOnUse" patternTransform="rotate(45)">'
        '<rect width="8" height="8" fill="rgba(22,48,92,0.08)"/><line x1="0" y1="0" x2="0" y2="8" '
        'stroke="rgba(22,48,92,0.35)" stroke-width="2"/></pattern>'
        '<pattern id="earth" width="14" height="14" patternUnits="userSpaceOnUse" patternTransform="rotate(45)">'
        '<rect width="14" height="14" fill="rgba(22,48,92,0.04)"/><line x1="0" y1="0" x2="0" y2="14" '
        'stroke="rgba(22,48,92,0.16)" stroke-width="2"/></pattern></defs>')
LINE = f'stroke="{NAVY}" stroke-linejoin="round" stroke-linecap="round"'
# The world canvas a part is drawn on before cropping: wide enough for the ascent's whole climb.
WX, WY, WW, WH = -900, -4200, 6400, 5800

layers = {}


ONLY = sys.argv[1:]


def layer(name, body, pivot):
    if ONLY and not any(name.startswith(p) for p in ONLY):
        return
    svg = (f'<svg xmlns="{SVGNS}" width="{WW}" height="{WH}" viewBox="{WX} {WY} {WW} {WH}">'
           f'{DEFS}{body}</svg>')
    png = resvg_py.svg_to_bytes(svg_string=svg, width=WW, height=WH, font_dirs=[FONTS], skip_system_fonts=True)
    im = Image.open(io.BytesIO(bytes(png))).convert("RGBA")
    box = im.getchannel("A").getbbox()
    if box is None:
        # An empty part (a carrier): one transparent pixel at the pivot.
        box = (int(pivot[0]) - WX, int(pivot[1]) - WY, int(pivot[0]) - WX + 1, int(pivot[1]) - WY + 1)
    box = (max(0, box[0] - 2), max(0, box[1] - 2), min(WW, box[2] + 2), min(WH, box[3] + 2))
    im.crop(box).save(os.path.join(OUT, name + ".png"))
    layers[name] = {"x": box[0] + WX, "y": box[1] + WY, "w": box[2] - box[0], "h": box[3] - box[1],
                    "px": float(pivot[0]), "py": float(pivot[1])}


def board_group(gid):
    """The inner SVG of one of the storyboard's parts, at rest (its group's transform dropped)."""
    html = open(os.path.join(BOARD, "project", "Failure6.dc.html"), encoding="utf-8").read()
    root = ET.fromstring(re.search(r"<svg.*?</svg>", html, re.S).group(0))
    for g in root.iter(NS + "g"):
        if g.get("id") == gid:
            return "".join(ET.tostring(c, encoding="unicode") for c in g)
    raise SystemExit(f"{gid}: not in the storyboard")


def cloud(cx, cy, s=1.0):
    """A cloud as the storyboard draws them: three bumps on a flat base line."""
    w = 110 * s
    return (f'<path d="M{cx - w / 2} {cy} C{cx - w / 2} {cy - 26 * s} {cx - w / 4} {cy - 30 * s} {cx - w / 6} {cy - 20 * s} '
            f'C{cx - w / 8} {cy - 46 * s} {cx + w / 6} {cy - 48 * s} {cx + w / 6} {cy - 22 * s} '
            f'C{cx + w / 4} {cy - 34 * s} {cx + w / 2} {cy - 26 * s} {cx + w / 2} {cy} Z" fill="#ffffff" {LINE} stroke-width="2"/>'
            f'<path d="M{cx - w / 2 - 6 * s} {cy + 6 * s} L{cx + w / 2 + 6 * s} {cy + 6 * s}" fill="none" {LINE} stroke-width="2"/>')


def puff(cx, cy, r):
    """A smoke puff in the storyboard's manner: three overlapping circles, outlined."""
    c = [(cx - 0.8 * r, cy + 0.3 * r, 0.8 * r), (cx, cy - 0.15 * r, r), (cx + 0.8 * r, cy + 0.3 * r, 0.8 * r)]
    out = "".join(f'<circle cx="{x}" cy="{y}" r="{rr}" fill="#ffffff" stroke="{NAVY}" stroke-width="2"/>' for x, y, rr in c)
    return out + "".join(f'<circle cx="{x}" cy="{y}" r="{rr - 1.6}" fill="#ffffff"/>' for x, y, rr in c)


# --- On the pad: launch preparations and countdown -------------------------------------------

def pad_parts():
    # Propellant in the two tanks: a gold tint revealed from the bottom, and its level line.
    for name, top, bottom in (("lower", 642, 768), ("upper", 502, 618)):
        layer(f"fill-{name}", f'<rect x="917" y="{top}" width="86" height="{bottom - top}" fill="rgba(224,167,46,0.26)"/>',
              (917, top))
        layer(f"level-{name}", f'<path d="M917 {bottom} L1003 {bottom}" fill="none" stroke="{GOLD}" stroke-width="3"/>',
              (960, bottom))
    # Propellant on its way: a gold dot that runs up the tower and along an arm into a tank.
    for i in range(1, 25):
        layer(f"dot-{i}", f'<circle cx="1081" cy="880" r="5" fill="{GOLD}"/>', (1081, 880))
    # A leak at the lower arm's joint: a fan of spray and a puff of vapour.
    layer("leak-jet", "".join(f'<path d="M1012 704 L{x} {y}" fill="none" stroke="{GOLD}" stroke-width="3" stroke-linecap="round"/>'
                              for x, y in ((1052, 712), (1048, 728), (1038, 742))), (1012, 704))
    layer("leak-puff", puff(1046, 722, 14), (1030, 716))
    # Fire at the rocket's base, in two shapes that alternate.
    for i, k in ((1, 1.0), (2, 0.82)):
        tongues = []
        for j, (x, h) in enumerate(((905, 70), (932, 104), (960, 82), (988, 110), (1015, 66))):
            hh = h * (k if j % 2 == 0 else 2 - k)
            tongues.append(f'<path d="M{x - 16} 842 C{x - 18} {842 - hh * 0.5} {x - 4} {842 - hh * 0.8} {x} {842 - hh} '
                           f'C{x + 4} {842 - hh * 0.8} {x + 18} {842 - hh * 0.5} {x + 16} 842 Z" '
                           f'fill="{GOLD_FILL}" stroke="{GOLD}" stroke-width="3" stroke-linejoin="round"/>')
        layer(f"fire-{i}", "".join(tongues), (960, 842))
    # The countdown clock, standing on the ground right of the tower: a panel on two legs.
    clock = (f'<path d="M1440 760 L1440 880 M1560 760 L1560 880" fill="none" {LINE} stroke-width="3"/>'
             f'<rect x="1400" y="600" width="200" height="160" rx="6" fill="#ffffff" {LINE} stroke-width="3"/>'
             f'<rect x="1416" y="616" width="168" height="128" rx="3" fill="url(#hatch)" {LINE} stroke-width="1.5"/>')
    layer("clock", clock, (1500, 680))
    # Seven-segment digits for the clock: lit segments navy, the others faint.
    seg = {"a": (0, 0, 1), "b": (1, 0, 0), "c": (1, 1, 0), "d": (0, 2, 1), "e": (0, 1, 0), "f": (0, 0, 0), "g": (0, 1, 1)}
    lit = {0: "abcdef", 1: "bc", 2: "abdeg", 3: "abcdg", 4: "bcfg", 5: "acdfg", 6: "acdefg", 7: "abc", 8: "abcdefg", 9: "abcdfg"}
    cx, top, w, h = 1500, 636, 52, 88
    for n in range(10):
        parts = []
        for s, (col, row, horiz) in seg.items():
            on = s in lit[n]
            fill = NAVY if on else "rgba(22,48,92,0.10)"
            if horiz:
                x0, y0 = cx - w / 2 + 6, top + row * h / 2
                parts.append(f'<path d="M{x0} {y0} L{x0 + 6} {y0 - 5} L{x0 + w - 18} {y0 - 5} L{x0 + w - 12} {y0} '
                             f'L{x0 + w - 18} {y0 + 5} L{x0 + 6} {y0 + 5} Z" fill="{fill}"/>')
            else:
                x0, y0 = cx - w / 2 + col * w, top + row * h / 2 + 6
                parts.append(f'<path d="M{x0} {y0} L{x0 + 5} {y0 + 6} L{x0 + 5} {y0 + h / 2 - 18} L{x0} {y0 + h / 2 - 12} '
                             f'L{x0 - 5} {y0 + h / 2 - 18} L{x0 - 5} {y0 + 6} Z" fill="{fill}"/>')
        layer(f"digit-{n}", "".join(parts), (cx, top + h / 2))


# --- The climb: ascent -------------------------------------------------------------------------

# The planned path: a circular arc turning right from the rocket's start, 8° off vertical.
P0, TH0, R = (860.0, 200.0), 8.0, 5200.0


def path_at(s):
    th = math.radians(TH0) + s / R
    t0 = math.radians(TH0)
    cx, cy = P0[0] + R * math.cos(t0), P0[1] + R * math.sin(t0)
    return cx - R * math.cos(th), cy - R * math.sin(th), math.degrees(th)


def ascent_parts():
    # The carrier the stages hang off: nothing drawn, the pivot at the rocket's middle.
    layer("carrier", "", (960, 600))
    # The upper stage's engine bell, hidden by the interstage until the stages part.
    layer("upper-bell", f'<path d="M944 621 L976 621 L982 639 L938 639 Z" fill="#ffffff" {LINE} stroke-width="3"/>', (960, 639))
    layer("flame-upper", f'<path d="M943 640 C941 680 952 720 960 750 C968 720 979 680 977 640 Z" fill="{GOLD_FILL}" '
                         f'stroke="{GOLD}" stroke-width="3" stroke-linejoin="round"/>'
                         f'<path d="M950 640 C950 665 956 690 960 705 C964 690 970 665 970 640 Z" fill="{GOLD}"/>', (960, 640))
    pts = [path_at(s) for s in range(0, 7000, 20)]
    d = "M" + " L".join(f"{x:.1f} {y:.1f}" for x, y, _ in pts)
    layer("planned", f'<path d="{d}" fill="none" stroke="{GOLD}" stroke-width="3" stroke-dasharray="14 12" opacity="0.8"/>',
          (0, 0))
    # The ground the rocket left, below the first frame's bottom edge.
    layer("a-ground", f'<rect x="-900" y="640" width="3200" height="700" fill="url(#earth)"/>'
                      f'<path d="M-900 640 L2300 640" fill="none" {LINE} stroke-width="3"/>', (0, 0))
    # Clouds beside the path, which the camera passes.
    for i, (s, side, k) in enumerate(((150, -380, 1.2), (500, 420, 1.0), (850, -300, 1.4), (1300, 360, 1.1),
                                      (1800, -420, 0.9), (2300, 300, 1.0)), 1):
        x, y, th = path_at(s)
        nx, ny = math.cos(math.radians(th)), math.sin(math.radians(th))
        layer(f"a-cloud-{i}", cloud(x + side * nx, y + side * ny, k), (x + side * nx, y + side * ny))


# --- In orbit: deployment and Earth orbit ------------------------------------------------------

def deployment_parts():
    # The Earth's limb under the stage, and the thin line of its atmosphere.
    cx, cy, r = 960, 3100, 2280
    layer("d-earth", f'<circle cx="{cx}" cy="{cy}" r="{r}" fill="url(#earth)" {LINE} stroke-width="3"/>'
                     f'<circle cx="{cx}" cy="{cy}" r="{r + 22}" fill="none" {LINE} stroke-width="1.5" stroke-dasharray="6 8" opacity="0.6"/>',
          (cx, cy))
    # The Moon's limb in its place, for the close-ups in lunar orbit (2026-10-20): no air, a few craters near the top,
    # which turn past as the limb does.
    craters = "".join(f'<ellipse cx="{cx + x}" cy="{cy - r + y}" rx="{rx}" ry="{rx * 0.3:.1f}" fill="#ffffff" {LINE} stroke-width="2.4" '
                      f'transform="rotate({ang} {cx} {cy})"/>'
                      for x, y, rx, ang in ((0, 40, 60, 0), (0, 70, 30, 9), (0, 34, 44, -11), (0, 60, 52, 20), (0, 44, 36, -24)))
    layer("d-moon", f'<circle cx="{cx}" cy="{cy}" r="{r}" fill="url(#hatch)" opacity="0.9" {LINE} stroke-width="3"/>{craters}', (cx, cy))
    # The upper stage and its payload, nose to the right (the storyboard's own parts, turned), all
    # drawn 1.5 times the launch's size and placed by D (a point at rest -> where it's drawn).
    def D(x, y):
        return (DX + DK * (x - 850), DY + DK * (y - 520))
    big = f"translate({DX} {DY}) scale({DK}) translate(-850 -520)"
    turn = "translate(-120 20) rotate(90 960 500)"
    layer("d-stage", f'<g transform="{big}"><g transform="{turn}">{board_group("debris-2")}'
                     f'<path d="M944 621 L976 621 L982 639 L938 639 Z" fill="#ffffff" {LINE} stroke-width="3"/></g></g>', D(770, 520))
    layer("d-payload", f'<g transform="{big}"><g transform="{turn}">{board_group("debris-1")}</g></g>', D(914, 520))
    # The springs between them, and a puff from the stage's thrusters as it backs away.
    layer("d-springs", f'<g transform="{big}">' + "".join(
        f'<path d="M{x} {y} l6 -6 l6 12 l6 -12 l6 12 l6 -6" fill="none" stroke="{GOLD}" stroke-width="3" '
        f'stroke-linejoin="round" stroke-linecap="round"/>' for x, y in ((842, 488), (842, 552))) + "</g>", D(842, 520))
    layer("d-puff", f'<g transform="{big}">{puff(820, 452, 14)}{puff(820, 590, 14)}</g>', D(820, 520))
    # A chip knocked off the payload when the stage hits it.
    layer("d-chip", f'<g transform="{big}"><path d="M0 0 L22 -4 L26 14 L4 18 Z" transform="translate(890 490)" '
                    f'fill="#ffffff" {LINE} stroke-width="2"/></g>', D(902, 497))


# Where deployment's craft sits: (850, 520) at rest lands on (DX, DY), DK times the size.
DX, DY, DK = 700, 560, 1.5


def orbit_parts():
    cx, cy = 960, 560
    layer("o-earth", f'<circle cx="{cx}" cy="{cy}" r="250" fill="url(#earth)" {LINE} stroke-width="3"/>'
                     f'<circle cx="{cx}" cy="{cy}" r="268" fill="none" {LINE} stroke-width="1.5" stroke-dasharray="6 8" opacity="0.6"/>',
          (cx, cy))
    layer("o-orbit", f'<circle cx="{cx}" cy="{cy}" r="390" fill="none" stroke="{GOLD}" stroke-width="3" stroke-dasharray="14 12" opacity="0.8"/>',
          (cx, cy))
    # A long ellipse round the Earth (2026-10-18, high-orbit-scene.jsx): the Earth at its focus, its low point 390
    # below, its high point 1100 above (the timeline turns it to where the burn ends); thicker, as the camera is back.
    # The return's path (high-orbit-trim.jsx): the same high point, its low point lowered into the entry corridor (276).
    layer("o-ellipse-down", f'<ellipse cx="{cx}" cy="{cy - 412.0}" rx="551.0" ry="688.0" fill="none" stroke="{GOLD}" '
                            f'stroke-width="5" stroke-dasharray="22 18" opacity="0.8"/>', (cx, cy))
    # A stage that parts from the payload on the map (empty unless a skin draws one, 2026-10-18).
    layer("o-stage", "", (960, 560))
    layer("o-ellipse", f'<ellipse cx="{cx}" cy="{cy - 355.0}" rx="655.0" ry="745.0" fill="none" stroke="{GOLD}" stroke-width="5" '
                       f'stroke-dasharray="22 18" opacity="0.8"/>', (cx, cy))
    # The payload, small, nose up (it turns to face its way), and the glow when it comes down.
    # Thicker lines, so it still reads at 0.6 of its size on the replay's small monitor.
    sat = re.sub(r'stroke-width="[\d.]+"', 'stroke-width="5"', board_group("debris-1"))
    layer("o-sat", f'<g transform="translate(960 560) scale(0.6) translate(-960 -426)">{sat}</g>', (960, 560))
    layer("o-streak", f'<path d="M944 600 C942 660 952 740 960 790 C968 740 978 660 976 600 Z" fill="{GOLD_FILL}" '
                      f'stroke="{GOLD}" stroke-width="3" stroke-linejoin="round"/>', (960, 560))
    # The same flame from a longer craft's tail (2026-10-20), whose engine is lower than a satellite's: Gemini docked into
    # the Transtage (gtd-), Gemini with its propulsion module (gl-).
    for name, dy in (("gtd-o-streak", 66), ("gl-o-streak-lpm", 66)):
        layer(name, f'<g transform="translate(0 {dy})"><path d="M944 600 C942 660 952 740 960 790 C968 740 978 660 '
                                   f'976 600 Z" fill="{GOLD_FILL}" stroke="{GOLD}" stroke-width="3" stroke-linejoin="round"/></g>',
                  (960, 560))
    # Radio rings from the payload while it's alive: four, so they can overlap.
    for i in range(1, 5):
        layer(f"ring-{i}", f'<circle cx="960" cy="560" r="40" fill="none" stroke="{GOLD}" stroke-width="4"/>', (960, 560))


# --- Coming home: reentry preparations, retrofire, retrofire jettison, reentry, descent and splashdown

# The capsule (the payload, the rocket's nose section) drawn RK times the launch's size with its
# centre on RC. In orbit it hangs off a carrier turned 0 = nose right (its way), base and retro pack
# behind it; parts drawn in that frame use RT. For the descent, RU draws it nose up, which is the
# carrier turned 270, so the two cut into each other.
RC, RK = (960, 470), 2.0


def RT(body):
    return f'<g transform="translate({RC[0]} {RC[1]}) scale({RK}) rotate(90) translate(-960 -426)">{body}</g>'


def RT_AT(x, y):
    """Where a point of the capsule's own drawing lands in RT (nose right)."""
    return (RC[0] - RK * (y - 426), RC[1] + RK * (x - 960))


def RU_AT(x, y):
    """Where a point of the capsule's own drawing lands in RU (nose up)."""
    return (RC[0] + RK * (x - 960), RC[1] + RK * (y - 426))


def D_AT(x, y):
    """Where a point of the launch's drawing lands in the deployment (DX, DY, DK)."""
    return (DX + DK * (x - 850), DY + DK * (y - 520))


def RU(body):
    return f'<g transform="translate({RC[0]} {RC[1]}) scale({RK}) translate(-960 -426)">{body}</g>'


def capsule():
    # Thinner lines before the scale, so they land a little heavier than the launch's (it plays small).
    return re.sub(r'stroke-width="([\d.]+)"', lambda m: f'stroke-width="{float(m.group(1)) * 0.8:.2f}"',
                  board_group("debris-1"))


def return_parts():
    # The orbit the craft is on, through its centre, round the deployment's Earth.
    layer("r-orbit", f'<circle cx="960" cy="3100" r="{3100 - RC[1]}" fill="none" stroke="{GOLD}" stroke-width="3" '
                     f'stroke-dasharray="14 12" opacity="0.8"/>', (960, 3100))
    layer("r-carrier", "", RC)
    layer("r-capsule", RT(capsule()), RC)
    layer("r-capsule-up", RU(capsule()), RC)
    # Mercury's periscope (2026-10-17): nothing on the generic capsule (mc-r-periscope draws it).
    layer("r-periscope", "", RC)
    # The retro pack on the base: a box of three motors, held by two straps.
    pack = (f'<rect x="928" y="500" width="64" height="20" rx="3" fill="#ffffff" {LINE} stroke-width="2.4"/>'
            + "".join(f'<path d="M{x - 5} 520 L{x + 5} 520 L{x + 7} 529 L{x - 7} 529 Z" fill="#ffffff" {LINE} stroke-width="2"/>'
                      for x in (942, 960, 978)))
    # It turns about the second strap's hinge, which is what a pack left hanging on it swings from.
    layer("r-pack", RT(pack), RT_AT(1003, 482))
    layer("r-strap-a", RT(f'<path d="M930 506 L917 482" fill="none" {LINE} stroke-width="2.4"/>'), RT_AT(917, 482))
    layer("r-strap-b", RT(f'<path d="M990 506 L1003 482" fill="none" {LINE} stroke-width="2.4"/>'), RT_AT(1003, 482))
    # A strap lets go: a short gold flash where it meets the capsule.
    layer("r-cut-a", RT(f'<circle cx="917" cy="484" r="6" fill="{GOLD}"/>'), RT_AT(917, 484))
    layer("r-cut-b", RT(f'<circle cx="1003" cy="484" r="6" fill="{GOLD}"/>'), RT_AT(1003, 484))
    # One flame per retro motor.
    for i, x in enumerate((942, 960, 978), 1):
        layer(f"r-flame-{i}", RT(f'<path d="M{x - 6} 530 C{x - 7} 548 {x - 3} 566 {x} 584 C{x + 3} 566 {x + 7} 548 {x + 6} 530 Z" '
                                 f'fill="{GOLD_FILL}" stroke="{GOLD}" stroke-width="2" stroke-linejoin="round"/>'), RT_AT(x, 530))
    # The attitude jets at the nose, a puff each side, and one jet stuck open (a long plume).
    layer("r-jet-l", RT(puff(912, 378, 10)), RT_AT(926, 378))
    layer("r-jet-r", RT(puff(1008, 378, 10)), RT_AT(994, 378))
    layer("r-jet-stuck", RT(f'<path d="M986 376 C1016 368 1050 358 1086 350 L1090 366 C1054 372 1018 384 988 392 Z" '
                            f'fill="{GOLD_FILL}" stroke="{GOLD}" stroke-width="2" stroke-linejoin="round"/>'), RC)
    # Reentry: the glow at the base (a bow of hot air, two shells), the air streaming past, and the
    # pieces of a break-up.
    glow = (f'<path d="M906 516 C920 560 1000 560 1014 516" fill="none" stroke="{GOLD}" stroke-width="4" stroke-linecap="round"/>'
            f'<path d="M896 520 C912 584 1008 584 1024 520 C1000 548 920 548 896 520 Z" fill="{GOLD_FILL}" stroke="{GOLD}" '
            f'stroke-width="2" stroke-linejoin="round"/>')
    layer("r-glow", RT(glow), RC)
    layer("r-glow-side", RT(f'<path d="M1008 380 C1030 420 1034 470 1020 512 C1040 470 1040 420 1008 380 Z" fill="{GOLD_FILL}" '
                            f'stroke="{GOLD}" stroke-width="2" stroke-linejoin="round"/>'), RC)
    # The break-up's two halves: the nose cone and the body below it (nose right, as the carrier at 0).
    nose = f'<path d="M915 440 Q915 384 960 352 Q1005 384 1005 440 Z" fill="#ffffff" {LINE} stroke-width="2.4"/>'
    body = (f'<path d="M915 444 L1005 444 L1005 500 L915 500 Z" fill="#ffffff" {LINE} stroke-width="2.4"/>'
            f'<path d="M915 470 L1005 470" fill="none" {LINE} stroke-width="1.6"/>')
    layer("r-half-nose", RT(nose), RT_AT(960, 410))
    layer("r-half-body", RT(body), RT_AT(960, 472))
    for i in range(1, 9):
        layer(f"r-streak-{i}", f'<path d="M960 470 L1050 470" fill="none" stroke="{GOLD}" stroke-width="3" stroke-linecap="round"/>',
              (960, 470))
    # Motes in orbit (2026-10-16): faint dashes that stream past against the craft's way (motes.jsx).
    for i in range(1, 15):
        layer(f"r-mote-{i}", f'<path d="M930 470 L990 470" fill="none" stroke="rgba(22,48,92,0.40)" stroke-width="4" '
              'stroke-linecap="round"/>', (960, 470))
    for i, pts in enumerate(("0 0 26 -4 30 16 4 20", "0 0 18 -8 24 10 6 14", "0 0 30 2 22 18 2 14", "0 0 14 -6 20 8"), 1):
        layer(f"r-chip-{i}", f'<polygon points="{pts}" transform="translate(950 460)" fill="#ffffff" {LINE} stroke-width="3"/>',
              (962, 466))
    # Descent: the drogue, the main chute (open, half open), a main that streams unopened, the sea,
    # the splash and its rings. The chutes hang from the nose, which is at the top in RU.
    top = RC[1] - RK * (426 - 352)
    def chute(w, h, lines_from, name):
        cx = RC[0]
        dome_y = top - lines_from
        d = (f'<path d="M{cx - w / 2} {dome_y} C{cx - w / 2} {dome_y - h} {cx + w / 2} {dome_y - h} {cx + w / 2} {dome_y} '
             f'C{cx + w / 4} {dome_y - h * 0.18} {cx - w / 4} {dome_y - h * 0.18} {cx - w / 2} {dome_y} Z" fill="#ffffff" {LINE} stroke-width="4"/>'
             f'<path d="M{cx} {dome_y - h * 0.75} L{cx} {dome_y - h * 0.15}" fill="none" {LINE} stroke-width="2"/>')
        lines = "".join(f'<path d="M{cx + k * w / 2} {dome_y} L{cx} {top}" fill="none" {LINE} stroke-width="2"/>'
                        for k in (-1, -0.45, 0.45, 1))
        layer(name, lines + d, (cx, top))
    chute(110, 70, 120, "r-drogue")
    chute(400, 190, 210, "r-main")
    chute(170, 230, 210, "r-main-half")
    # The streamer: the main out of its bag but never filling, a long flapping strip.
    layer("r-streamer", f'<path d="M960 {top} L960 {top - 70} C946 {top - 130} 974 {top - 190} 958 {top - 260} '
                        f'C970 {top - 300} 950 {top - 330} 962 {top - 360}" fill="none" {LINE} stroke-width="5"/>'
                        f'<path d="M960 {top} L960 {top - 70}" fill="none" {LINE} stroke-width="2"/>', (960, top))
    layer("r-sea", f'<rect x="-900" y="1100" width="3800" height="900" fill="url(#earth)"/>'
                   + "".join(f'<path d="M{x} 1100 q20 -10 40 0 q20 10 40 0" fill="none" {LINE} stroke-width="3"/>'
                             for x in range(-900, 2900, 80)), (960, 1100))
    layer("r-splash", "".join(f'<path d="M{960 + dx} 1100 Q{960 + dx * 1.6} {1100 - h} {960 + dx * 2.3} {1100 - h * 0.4}" '
                              f'fill="none" stroke="{GOLD}" stroke-width="4" stroke-linecap="round"/>'
                              for dx, h in ((-60, 110), (-34, 170), (-12, 200), (12, 200), (34, 170), (60, 110))), (960, 1100))
    for i in range(1, 4):
        layer(f"r-ring-{i}", f'<ellipse cx="960" cy="1100" rx="150" ry="22" fill="none" {LINE} stroke-width="3"/>', (960, 1100))
    # Modules separation: the same pack as the module behind the capsule (Vostok's retro engine is in
    # its instrument module), turning about its own centre, and the cable that can hold the two
    # together (Vostok 1), drawn along +x from its pivot so the timeline can stretch and aim it.
    layer("r-module", RT(pack), RT_AT(960, 512))
    # A Soyuz-type craft's orbital module (2026-10-16): a sphere on the nose with its docking probe,
    # latched at two points; it turns about latch b, which is what a module left hanging swings from.
    om = (f'<circle cx="960" cy="312" r="38" fill="#ffffff" {LINE} stroke-width="2.4"/>'
          f'<path d="M924 300 Q960 314 996 300" fill="none" {LINE} stroke-width="1.6"/>'
          f'<path d="M960 274 L960 256" fill="none" {LINE} stroke-width="3"/>'
          f'<circle cx="960" cy="254" r="4" fill="#ffffff" {LINE} stroke-width="2"/>')
    layer("r-om", RT(om), RT_AT(980, 348))
    layer("r-om-cut-a", RT(f'<circle cx="940" cy="348" r="6" fill="{GOLD}"/>'), RT_AT(940, 348))
    layer("r-om-cut-b", RT(f'<circle cx="980" cy="348" r="6" fill="{GOLD}"/>'), RT_AT(980, 348))
    # Gemini's adapter (2026-10-16): the equipment section, a truncated cone behind the base that hides
    # the retro pack (its retro section) until it goes; it turns about latch b, as the pack does.
    trunk = (f'<path d="M915 502 L1005 502 L1040 600 L880 600 Z" fill="#ffffff" {LINE} stroke-width="2.4"/>'
             f'<path d="M898 552 L1022 552" fill="none" {LINE} stroke-width="1.6"/>')
    layer("r-trunk", RT(trunk), RT_AT(1002, 504))
    layer("r-trunk-cut-a", RT(f'<circle cx="918" cy="504" r="6" fill="{GOLD}"/>'), RT_AT(918, 504))
    layer("r-trunk-cut-b", RT(f'<circle cx="1002" cy="504" r="6" fill="{GOLD}"/>'), RT_AT(1002, 504))
    layer("r-pyro", RT("".join(f'<circle cx="{x}" cy="502" r="6" fill="{GOLD}"/>' for x in (934, 960, 986))), RT_AT(960, 502))
    layer("r-cable", f'<path d="M0 0 C25 -6 75 6 100 0" transform="translate(960 470)" fill="none" {LINE} stroke-width="3"/>',
          (960, 470))
    # Landing: the ground (a horizon line with gentle rises and tufts), and the dust a touchdown kicks up.
    ground = (f'<rect x="-900" y="1100" width="3800" height="900" fill="url(#earth)"/>'
              f'<path d="M-900 1100 ' + " ".join(f"Q{x + 120} {1100 - (14 if (x // 240) % 2 else 6)} {x + 240} 1100"
                                                for x in range(-900, 2900, 240)) + f'" fill="none" {LINE} stroke-width="3"/>'
              + "".join(f'<path d="M{x} 1100 l4 -14 M{x + 8} 1100 l0 -18 M{x + 16} 1100 l-4 -14" fill="none" {LINE} stroke-width="2"/>'
                        for x in range(-860, 2900, 310)))
    layer("r-ground", ground, (960, 1100))
    layer("r-dust", puff(880, 1088, 22) + puff(1040, 1088, 22) + puff(960, 1080, 16), (960, 1100))
    for i, (x, y, k) in enumerate(((560, 700, 1.3), (1420, 880, 1.1), (700, 1060, 1.4), (1300, 1240, 1.2),
                                   (520, 1400, 1.0), (1480, 1540, 1.3)), 1):
        layer(f"r-cloud-{i}", cloud(x, y, k), (x, y))


# --- Time in orbit: experiments, a day or days in orbit, biometric data; and the suborbital hop -----

LABEL = f'font-family="Archivo" font-weight="600" font-size="24" fill="{NAVY}" letter-spacing="2"'
SMALL = LABEL.replace('font-size="24"', 'font-size="20"')


def gauge_panel(name, labels, x0=1470, y0=40):
    """A panel of gauges, top right: a label and an empty bar per row; each bar's fill is its own part."""
    h = 40 + 52 * len(labels)
    body = f'<rect x="{x0}" y="{y0}" width="400" height="{h}" rx="6" fill="#ffffff" {LINE} stroke-width="3"/>'
    for i, label in enumerate(labels):
        y = y0 + 42 + 52 * i
        body += (f'<text x="{x0 + 22}" y="{y + 8}" {LABEL}>{label}</text>'
                 f'<rect x="{x0 + 170}" y="{y - 12}" width="204" height="24" rx="3" fill="none" {LINE} stroke-width="2.4"/>')
    layer(name, body, (x0, y0))
    return [(x0 + 172, y0 + 42 + 52 * i) for i in range(len(labels))]


def time_parts():
    # Night: the Earth's far half shaded, turning about its centre (the orbit scene's Earth).
    layer("x-night", f'<path d="M960 310 A250 250 0 0 1 960 810 Z" fill="rgba(22,48,92,0.16)"/>', (960, 560))
    # The crew's supplies, and a satellite's power: each fill a gold bar from its left end.
    for i, (x, y) in enumerate(gauge_panel("x-supplies", ["OXYGEN", "POWER", "FOOD"]), 1):
        layer(f"x-bar-{i}", f'<rect x="{x}" y="{y - 10}" width="200" height="20" rx="2" fill="{GOLD}" opacity="0.85"/>', (x, y))
    for x, y in gauge_panel("x-power", ["POWER"]):
        layer("x-bar-p", f'<rect x="{x}" y="{y - 10}" width="200" height="20" rx="2" fill="{GOLD}" opacity="0.85"/>', (x, y))
    # The heartbeat strip, top left (clear of the orbit), drawn as a sweep monitor: four slots of
    # one beat each, rewritten left to right by a moving head; static for a dropout.
    X0, X1, Y0, Y1, BASE = 60, 700, 30, 140, 96
    beat = "l40 0 q8 -10 16 0 l16 0 l6 6 l8 -52 l9 64 l7 -18 l20 0 q10 -16 22 0 l16 0"
    import random
    rnd = random.Random(7)
    for k in range(4):
        x = X0 + 160 * k
        layer(f"x-beat-{k + 1}", f'<path d="M{x} {BASE} {beat}" fill="none" stroke="{NAVY}" stroke-width="3.5" '
                                 f'stroke-linejoin="round"/>', (x, BASE))
        zig = f"M{x} {BASE} " + " ".join(f"L{x + d} {BASE + rnd.randint(-26, 26)}" for d in range(10, 161, 10))
        layer(f"x-static-{k + 1}", f'<path d="{zig}" fill="none" stroke="{GOLD}" stroke-width="2.5" stroke-linejoin="round"/>',
              (x, BASE))
    layer("x-ecg-head", f'<circle cx="{X0}" cy="{BASE}" r="6" fill="{GOLD}"/>', (X0, BASE))
    layer("x-ecg-frame", f'<rect x="{X0 - 10}" y="{Y0}" width="{X1 - X0 + 20}" height="{Y1 - Y0}" rx="6" fill="#ffffff" '
                         f'{LINE} stroke-width="3"/><text x="{X0 + 6}" y="{Y0 + 30}" {SMALL}>HEARTBEAT</text>', (X0, Y0))
    # The suborbital arc: a parabola with its top at the capsule (RC), drawn in the world.
    pts = " ".join(f"L{x} {RC[1] + SUB_K * (x - RC[0]) ** 2:.1f}" for x in range(-900, 2901, 20))
    layer("x-arc", f'<path d="M-900 {RC[1] + SUB_K * (-900 - RC[0]) ** 2:.1f} {pts}" fill="none" stroke="{GOLD}" '
                   f'stroke-width="3" stroke-dasharray="14 12" opacity="0.8"/>', RC)


# The suborbital arc's shape: y = RC's y + SUB_K * (x - RC's x)^2 (the timeline uses the same).
SUB_K = 0.00015


# --- Recovery: a helicopter picks the capsule out of the sea --------------------------------------

def recovery_parts():
    # The helicopter, side on, facing left, its hoist under the cabin at (960, 230).
    heli = (f'<path d="M820 170 C820 128 860 112 920 112 L1010 112 C1050 112 1070 130 1072 158 L1072 196 '
            f'C1072 214 1056 224 1036 224 L860 224 C836 224 820 206 820 186 Z" fill="#ffffff" {LINE} stroke-width="4"/>'
            f'<path d="M836 160 C840 136 864 126 896 126 L920 126 L920 168 L836 168 Z" fill="url(#hatch)" {LINE} stroke-width="3"/>'
            f'<path d="M1072 150 L1250 140 L1250 156 L1072 186" fill="#ffffff" {LINE} stroke-width="4"/>'
            f'<circle cx="1258" cy="146" r="24" fill="none" {LINE} stroke-width="3"/>'
            f'<path d="M950 112 L950 92 M930 92 L970 92" fill="none" {LINE} stroke-width="4"/>'
            f'<path d="M860 224 L850 252 M1030 224 L1040 252 M820 252 L1070 252" fill="none" {LINE} stroke-width="4"/>'
            f'<rect x="948" y="224" width="24" height="10" fill="#ffffff" {LINE} stroke-width="3"/>')
    layer("y-heli", heli, (960, 230))
    layer("y-rotor", f'<path d="M760 90 L1140 90" fill="none" {LINE} stroke-width="5"/>', (950, 90))
    # The hoist cable, along +y from the winch, 100 px long (stretched by the timeline), with its hook.
    layer("y-cable", f'<path d="M960 234 L960 334" fill="none" {LINE} stroke-width="3"/>', (960, 234))
    layer("y-hook", f'<path d="M960 0 L960 10 C960 22 972 22 972 12" transform="translate(0 334)" fill="none" {LINE} '
                    f'stroke-width="4"/>', (960, 334))
    # The flotation collar round the capsule's base, at the sea line (the descent's camera: base at 618).
    base = RC[1] + RK * (500 - 426)
    layer("y-collar", f'<ellipse cx="960" cy="{base - 6}" rx="120" ry="24" fill="{GOLD_FILL}" stroke="{GOLD}" stroke-width="4"/>',
          (960, base))
    # The sea in front of the capsule (the same as r-sea), so what's under the water line shows
    # through its hatching, as the descent's sea looks.
    layer("y-sea-front", f'<rect x="-900" y="1100" width="3800" height="900" fill="url(#earth)"/>'
                         + "".join(f'<path d="M{x} 1100 q20 -10 40 0 q20 10 40 0" fill="none" {LINE} stroke-width="3"/>'
                                   for x in range(-900, 2900, 80)), (960, 1100))


# --- The planets: a map of the inner and outer planets, for the cruises -----------------------------

# The Sun at the map's centre, and each planet's orbit radius on the map (not to scale: spread so they
# all fit), with the asteroid belt between Mars and Jupiter. The transfer arcs are half ellipses with the
# Sun at a focus, from one orbit at START_ANGLE to the other opposite it (going anticlockwise, angles
# falling, as the orbit clips): cruise.jsx computes the same curve.
SUN = (960, 540)
PLANET_R = {"venus": 150, "earth": 215, "mars": 290, "jupiter": 440, "saturn": 525}
BELT = (345, 385)
START_ANGLE = 180
TRANSITS = {"em": ("earth", "mars"), "ev": ("earth", "venus"), "ej": ("earth", "jupiter"), "js": ("jupiter", "saturn")}


def transfer(r1, r2, nu):
    """The transfer orbit's distance from the Sun, nu degrees of travel from its start."""
    a, b = min(r1, r2), max(r1, r2)
    e, p = (b - a) / (b + a), 2 * a * b / (a + b)
    c = math.cos(math.radians(nu))
    return p / (1 + e * c) if r1 < r2 else p / (1 - e * c)


def map_parts():
    cx, cy = SUN
    rays = "".join(f'<path d="M{cx + 30 * math.cos(math.radians(a))} {cy + 30 * math.sin(math.radians(a))} '
                   f'L{cx + 44 * math.cos(math.radians(a))} {cy + 44 * math.sin(math.radians(a))}" fill="none" '
                   f'stroke="{GOLD}" stroke-width="4" stroke-linecap="round"/>' for a in range(0, 360, 30))
    layer("m-sun", f'<circle cx="{cx}" cy="{cy}" r="22" fill="{GOLD_FILL}" stroke="{GOLD}" stroke-width="4"/>{rays}', SUN)
    orbits = "".join(f'<circle cx="{cx}" cy="{cy}" r="{r}" fill="none" {LINE} stroke-width="2" stroke-dasharray="6 10" '
                     f'opacity="0.45"/>' for r in PLANET_R.values())
    import random
    rnd = random.Random(11)
    dots = "".join(f'<circle cx="{cx + rr * math.cos(a):.1f}" cy="{cy + rr * math.sin(a):.1f}" r="2.2" fill="{NAVY}" '
                   f'opacity="0.35"/>' for rr, a in ((rnd.uniform(*BELT), rnd.uniform(0, 2 * math.pi)) for _ in range(260)))
    layer("m-orbits", orbits + dots, SUN)
    # The planets, drawn on the right of the Sun (angle 0), each pivoting on its own centre: the
    # timeline moves them round their orbits upright.
    x = lambda name: cx + PLANET_R[name]
    layer("m-venus", f'<circle cx="{x("venus")}" cy="{cy}" r="13" fill="#ffffff" {LINE} stroke-width="3"/>'
                     f'<path d="M{x("venus") - 9} {cy - 4} L{x("venus") + 9} {cy - 4} M{x("venus") - 10} {cy + 4} L{x("venus") + 10} {cy + 4}" '
                     f'fill="none" {LINE} stroke-width="1.5" opacity="0.6"/>', (x("venus"), cy))
    layer("m-earth", f'<circle cx="{x("earth")}" cy="{cy}" r="14" fill="url(#earth)" {LINE} stroke-width="3"/>', (x("earth"), cy))
    layer("m-mars", f'<circle cx="{x("mars")}" cy="{cy}" r="11" fill="url(#hatch)" {LINE} stroke-width="3"/>', (x("mars"), cy))
    layer("m-jupiter", f'<circle cx="{x("jupiter")}" cy="{cy}" r="28" fill="#ffffff" {LINE} stroke-width="3"/>'
                       + "".join(f'<path d="M{x("jupiter") - w} {cy + dy} L{x("jupiter") + w} {cy + dy}" fill="none" {LINE} stroke-width="2" opacity="0.6"/>'
                                 for dy, w in ((-14, 22), (-5, 27), (6, 27), (15, 21))), (x("jupiter"), cy))
    layer("m-saturn", f'<circle cx="{x("saturn")}" cy="{cy}" r="19" fill="#ffffff" {LINE} stroke-width="3"/>'
                      f'<ellipse cx="{x("saturn")}" cy="{cy}" rx="38" ry="10" fill="none" {LINE} stroke-width="3" '
                      f'transform="rotate(-18 {x("saturn")} {cy})"/>', (x("saturn"), cy))
    # The transfer arcs, dashed gold.
    for key, (a, b) in TRANSITS.items():
        r1, r2 = PLANET_R[a], PLANET_R[b]
        pts = []
        for i in range(0, 181, 3):
            r = transfer(r1, r2, i)
            ang = math.radians(START_ANGLE - i)
            pts.append(f"{cx + r * math.cos(ang):.1f} {cy + r * math.sin(ang):.1f}")
        layer(f"m-arc-{key}", f'<path d="M{" L".join(pts)}" fill="none" stroke="{GOLD}" stroke-width="3.5" stroke-dasharray="14 10" '
                              f'opacity="0.85"/>', SUN)
    # Mercury, its own parts (added for Mariner 10, 2026-10-14, so the other cruises' map stays as it was):
    # its orbit, the planet, and the arc from Venus.
    rm = 95
    layer("m-orbit-mercury", f'<circle cx="{cx}" cy="{cy}" r="{rm}" fill="none" {LINE} stroke-width="2" stroke-dasharray="6 10" '
                             f'opacity="0.45"/>', SUN)
    layer("m-mercury", f'<circle cx="{cx + rm}" cy="{cy}" r="8" fill="url(#earth)" {LINE} stroke-width="3"/>', (cx + rm, cy))
    pts = []
    for i in range(0, 181, 3):
        r = transfer(PLANET_R["venus"], rm, i)
        ang = math.radians(START_ANGLE - i)
        pts.append(f"{cx + r * math.cos(ang):.1f} {cy + r * math.sin(ang):.1f}")
    layer("m-arc-vm", f'<path d="M{" L".join(pts)}" fill="none" stroke="{GOLD}" stroke-width="3.5" stroke-dasharray="14 10" '
                      f'opacity="0.85"/>', SUN)
    # The probe: a small body with a dish, drawn at the Sun's place (the timeline moves it), and its puff.
    layer("m-probe", f'<rect x="{cx - 9}" y="{cy - 7}" width="18" height="14" rx="2" fill="#ffffff" {LINE} stroke-width="3"/>'
                     f'<path d="M{cx - 14} {cy - 14} Q{cx} {cy - 4} {cx + 14} {cy - 14}" fill="none" {LINE} stroke-width="3"/>'
                     f'<path d="M{cx} {cy - 7} L{cx} {cy - 10}" fill="none" {LINE} stroke-width="2"/>', SUN)
    layer("m-puff", puff(cx, cy, 10), SUN)
    for i in range(1, 4):
        layer(f"m-ring-{i}", f'<circle cx="{cx}" cy="{cy}" r="16" fill="none" stroke="{GOLD}" stroke-width="3"/>', SUN)


# --- At the planets and the Sun: Venus and Mars orbits, solar measurements ----------------------------

def planet_parts():
    cx, cy = 960, 560
    # Venus under its clouds: bands that curve with the globe.
    bands = "".join(f'<path d="M{cx - w} {cy + dy} Q{cx} {cy + dy + bend} {cx + w} {cy + dy}" fill="none" {LINE} '
                    f'stroke-width="2" opacity="0.5"/>'
                    for dy, w, bend in ((-170, 182, 26), (-100, 230, 34), (-30, 248, 38), (40, 246, 38), (110, 226, 32), (175, 176, 24)))
    layer("p-venus", f'<circle cx="{cx}" cy="{cy}" r="250" fill="#ffffff" {LINE} stroke-width="3"/>{bands}'
                     f'<circle cx="{cx}" cy="{cy}" r="268" fill="none" {LINE} stroke-width="1.5" stroke-dasharray="6 8" opacity="0.6"/>',
          (cx, cy))
    # Mars: rusty hatching, craters, a polar cap.
    craters = "".join(f'<circle cx="{cx + x}" cy="{cy + y}" r="{r}" fill="#ffffff" {LINE} stroke-width="2" opacity="0.8"/>'
                      for x, y, r in ((-90, -40, 26), (60, 50, 18), (110, -90, 14), (-30, 120, 20), (-150, 70, 12), (20, -130, 10)))
    layer("p-mars", f'<circle cx="{cx}" cy="{cy}" r="250" fill="url(#hatch)" {LINE} stroke-width="3"/>{craters}'
                    f'<path d="M{cx - 90} {cy - 233} Q{cx} {cy - 200} {cx + 90} {cy - 233} A250 250 0 0 0 {cx - 90} {cy - 233} Z" '
                    f'fill="#ffffff" {LINE} stroke-width="2"/>', (cx, cy))
    # Jupiter: belts across the globe and the great spot; Saturn: belts and its ring, seen a little from
    # above (the ring's far side behind the globe, drawn in two parts); Mercury: craters on bare rock.
    belts = lambda spec: "".join(f'<path d="M{cx - w} {cy + dy} Q{cx} {cy + dy + bend} {cx + w} {cy + dy}" fill="none" {LINE} '
                                 f'stroke-width="{sw}" opacity="0.55"/>' for dy, w, bend, sw in spec)
    layer("p-jupiter", f'<circle cx="{cx}" cy="{cy}" r="250" fill="#ffffff" {LINE} stroke-width="3"/>'
                       + belts(((-150, 200, 22, 3), (-80, 236, 28, 5), (-20, 249, 30, 3), (50, 244, 30, 5), (120, 220, 26, 3), (180, 172, 20, 2)))
                       + f'<ellipse cx="{cx + 70}" cy="{cy + 82}" rx="44" ry="22" fill="url(#hatch)" {LINE} stroke-width="2.4"/>', (cx, cy))
    ring = lambda half: (f'<path d="M{cx - 430} {cy} A430 96 0 0 {0 if half == "front" else 1} {cx + 430} {cy}" fill="none" {LINE} stroke-width="5"/>'
                         f'<path d="M{cx - 340} {cy} A340 70 0 0 {0 if half == "front" else 1} {cx + 340} {cy}" fill="none" {LINE} stroke-width="2.4"/>')
    layer("p-saturn", ring("back") + f'<circle cx="{cx}" cy="{cy}" r="250" fill="#ffffff" {LINE} stroke-width="3"/>'
                      + belts(((-140, 208, 22, 3), (-60, 242, 28, 4), (70, 240, 28, 3), (150, 196, 22, 4))) + ring("front"), (cx, cy))
    pits = "".join(f'<circle cx="{cx + x}" cy="{cy + y}" r="{r}" fill="#ffffff" {LINE} stroke-width="2" opacity="0.85"/>'
                   for x, y, r in ((-110, -80, 30), (90, -120, 16), (130, 40, 24), (-40, 70, 14), (30, 150, 20), (-160, 110, 12), (40, -30, 10), (-60, -170, 9)))
    layer("p-mercury", f'<circle cx="{cx}" cy="{cy}" r="250" fill="url(#earth)" {LINE} stroke-width="3"/>{pits}', (cx, cy))
    # The mapping beam: a gold wedge from the probe (at the scene's centre at rest) towards the planet
    # (left, at rest; the timeline turns it with the probe).
    layer("p-beam", f'<path d="M{cx} {cy} L{cx - 130} {cy - 22} L{cx - 130} {cy + 22} Z" fill="{GOLD_FILL}" stroke="{GOLD}" '
                    f'stroke-width="2" stroke-linejoin="round"/>', (cx, cy))
    # The Sun, big at the left, its rays; the probe's orbit round it, through the screen's middle.
    sx, sy = 240, 540
    rays = "".join(f'<path d="M{sx + 140 * math.cos(math.radians(a)):.1f} {sy + 140 * math.sin(math.radians(a)):.1f} '
                   f'L{sx + 178 * math.cos(math.radians(a)):.1f} {sy + 178 * math.sin(math.radians(a)):.1f}" fill="none" '
                   f'stroke="{GOLD}" stroke-width="5" stroke-linecap="round"/>' for a in range(0, 360, 20))
    layer("s-sun", f'<circle cx="{sx}" cy="{sy}" r="120" fill="{GOLD_FILL}" stroke="{GOLD}" stroke-width="5"/>{rays}', (sx, sy))
    layer("s-orbit", f'<circle cx="{sx}" cy="{sy}" r="760" fill="none" {LINE} stroke-width="2.5" stroke-dasharray="14 12" opacity="0.6"/>',
          (sx, sy))
    # The solar wind: short gold dashes the timeline sends out from the Sun along its rays.
    for i in range(1, 15):
        layer(f"s-wind-{i}", f'<path d="M{sx} {sy} L{sx + 60} {sy}" fill="none" stroke="{GOLD}" stroke-width="4" '
                             f'stroke-linecap="round" opacity="0.8"/>', (sx, sy))


# --- The Moon: its globe for the orbit scene, and the Earth-Moon map for the coasts --------------------

def moon_parts():
    cx, cy = 960, 560
    # The Moon: grey maria (darker hatch) and craters, no air (no dashed line round it).
    maria = "".join(f'<ellipse cx="{cx + x}" cy="{cy + y}" rx="{rx}" ry="{ry}" fill="url(#hatch)" opacity="0.7"/>'
                    for x, y, rx, ry in ((-70, -60, 80, 55), (50, 20, 60, 70), (-20, 110, 70, 40)))
    craters = "".join(f'<circle cx="{cx + x}" cy="{cy + y}" r="{r}" fill="#ffffff" {LINE} stroke-width="2" opacity="0.85"/>'
                      for x, y, r in ((120, -110, 22), (150, 60, 16), (-150, 30, 18), (-60, -170, 12), (90, 160, 14), (-120, 150, 10)))
    layer("p-moon", f'<circle cx="{cx}" cy="{cy}" r="250" fill="#ffffff" {LINE} stroke-width="3"/>{maria}{craters}', (cx, cy))
    # The Earth, small and far off to the left (where the radio is), for the Moon scenes.
    layer("l-earth-far", f'<circle cx="90" cy="560" r="34" fill="url(#earth)" {LINE} stroke-width="3"/>', (90, 560))
    # The radio's line to the Earth: the shadow behind the Moon where it can't reach (a faint wedge).
    layer("l-shadow", f'<path d="M{cx} {cy - 250} L2400 {cy - 250} L2400 {cy + 250} L{cx} {cy + 250} Z" fill="rgba(22,48,92,0.05)"/>',
          (cx, cy))


# --- The way to the Moon and back: the open path past the Moon, the Earth-Moon map, the corridor -------

def bezier(p0, c1, c2, p1, n=60):
    pts = []
    for i in range(n + 1):
        u = i / n
        a, b, c, d = (1 - u) ** 3, 3 * u * (1 - u) ** 2, 3 * u * u * (1 - u), u ** 3
        pts.append((a * p0[0] + b * c1[0] + c * c2[0] + d * p1[0], a * p0[1] + b * c1[1] + c * c2[1] + d * p1[1]))
    return pts


def way_parts():
    cx, cy = 960, 560
    # The open path past the Moon (as loi.jsx's: low point 390 behind it, e 1.6), dashed.
    pts = []
    for th in range(-112, 113, 2):
        r = 390 * 2.6 / (1 + 1.6 * math.cos(math.radians(th)))
        pts.append(f"{cx + r * math.cos(math.radians(th)):.1f} {cy + r * math.sin(math.radians(th)):.1f}")
    layer("l-open", f'<path d="M{" L".join(pts)}" fill="none" stroke="{GOLD}" stroke-width="3" stroke-dasharray="14 12" '
                    f'opacity="0.8"/>', (cx, cy))
    # The Earth-Moon map: the Earth at the left, the Moon at the right on its orbit (dashed, round the
    # Earth), the way out below and the way home above, both dashed, looping round the Moon's far side.
    E, M = (330, 560), (1590, 560)
    layer("l-map-earth", f'<circle cx="{E[0]}" cy="{E[1]}" r="62" fill="url(#earth)" {LINE} stroke-width="3"/>'
                         f'<circle cx="{E[0]}" cy="{E[1]}" r="96" fill="none" {LINE} stroke-width="1.5" stroke-dasharray="5 8" '
                         f'opacity="0.5"/>', E)
    layer("l-map-moon", f'<circle cx="{M[0]}" cy="{M[1]}" r="28" fill="#ffffff" {LINE} stroke-width="3"/>'
                        f'<circle cx="{M[0] - 8}" cy="{M[1] - 6}" r="7" fill="url(#hatch)"/>', M)
    layer("l-map-orbit", f'<circle cx="{E[0]}" cy="{E[1]}" r="{M[0] - E[0]}" fill="none" {LINE} stroke-width="2" '
                         f'stroke-dasharray="6 10" opacity="0.4"/>', E)
    out = bezier((E[0] + 70, E[1] + 66), (700, 860), (1350, 800), (M[0], M[1] + 92)) + \
          bezier((M[0], M[1] + 92), (M[0] + 110, M[1] + 92), (M[0] + 110, M[1] - 92), (M[0], M[1] - 92))[1:]
    home = bezier((M[0], M[1] - 92), (1350, 300), (700, 260), (E[0] + 70, E[1] - 66))
    for name, path in (("out", out), ("home", home)):
        d = "M" + " L".join(f"{x:.1f} {y:.1f}" for x, y in path)
        layer(f"l-path-{name}", f'<path d="{d}" fill="none" stroke="{GOLD}" stroke-width="3.5" stroke-dasharray="14 10" '
                                f'opacity="0.85"/>', E)
        layers.setdefault("_paths", {})[name] = [[round(x, 1), round(y, 1)] for x, y in path]
    # The entry corridor: a narrow band at the top of the Earth's air (the orbit scene's Earth).
    a0, a1, r0, r1 = math.radians(228), math.radians(292), 262, 290
    band = (f'M{cx + r1 * math.cos(a0):.1f} {cy + r1 * math.sin(a0):.1f} A{r1} {r1} 0 0 1 {cx + r1 * math.cos(a1):.1f} '
            f'{cy + r1 * math.sin(a1):.1f} L{cx + r0 * math.cos(a1):.1f} {cy + r0 * math.sin(a1):.1f} A{r0} {r0} 0 0 0 '
            f'{cx + r0 * math.cos(a0):.1f} {cy + r0 * math.sin(a0):.1f} Z')
    layer("l-corridor", f'<path d="{band}" fill="{GOLD_FILL}" stroke="{GOLD}" stroke-width="2.5" stroke-dasharray="8 6"/>', (cx, cy))


# --- Rendezvous and docking: closing in on the orbit, then the close-up over the Earth's limb ----

# The docking target, nose up as the payload is drawn (centre 960 426): a long body with its engine
# at the top and the funnel-shaped docking collar at the bottom, the wide end facing whoever docks.
def target_units(w=2.4):
    return (f'<path d="M950 336 L940 312 L980 312 L970 336 Z" fill="#ffffff" {LINE} stroke-width="{w}"/>'
            f'<rect x="930" y="336" width="60" height="180" fill="#ffffff" {LINE} stroke-width="{w}"/>'
            f'<path d="M930 380 L990 380 M930 470 L990 470" fill="none" {LINE} stroke-width="{w * 0.7:.2f}"/>'
            f'<path d="M930 516 L990 516 L1000 540 L920 540 Z" fill="#ffffff" {LINE} stroke-width="{w}"/>'
            f'<ellipse cx="960" cy="540" rx="40" ry="7" fill="#ffffff" {LINE} stroke-width="{w * 0.8:.2f}"/>')


# The close-up: craft KK times the launch's size. The chaser (the capsule, nose right: its way) is
# centred on KC; the target lies ahead of it with its collar's mouth at KM, where the capsule's nose
# meets it docked. Every part's pivot is at rest where the docked stack has it.
KK, KC = 1.3, (800, 470)
KM = (880, 470)


def K_AT(c, x, y, rot):
    """Where a point of a nose-up drawing (centre 960 426) lands drawn KK times at c, turned rot (90: nose right)."""
    dx, dy = KK * (x - 960), KK * (y - 426)
    if rot == 90:
        return (c[0] - dy, c[1] + dx)
    return (c[0] + dy, c[1] - dx)


def KT(c, rot, body):
    return f'<g transform="translate({c[0]} {c[1]}) scale({KK}) rotate({rot}) translate(-960 -426)">{body}</g>'


def dock_parts():
    # A second payload in the orbit scene, for two craft closing in (the same drawing as o-sat).
    sat = re.sub(r'stroke-width="[\d.]+"', 'stroke-width="5"', board_group("debris-1"))
    layer("o-sat-b", f'<g transform="translate(960 560) scale(0.6) translate(-960 -426)">{sat}</g>', (960, 560))
    # The close-up.
    layer("k-carrier", "", KC)
    layer("k-chaser", KT(KC, 90, capsule()), KC)
    # The target's centre lands so its collar's mouth (960 540 nose up) is at KM.
    tc = (KM[0] + KK * (540 - 426), KM[1])
    layer("k-target", KT(tc, 90, target_units()), KM)
    # The other capsule of a rendezvous, nose left, facing the chaser.
    oc = (1150, 470)
    layer("k-other", KT(oc, -90, capsule()), oc)
    # The target's blinking light, on its collar.
    lp = K_AT(tc, 922, 512, 90)
    layer("k-light", f'<circle cx="{lp[0]:.1f}" cy="{lp[1]:.1f}" r="7" fill="{GOLD}" {LINE} stroke-width="2"/>', lp)
    # The latches catching: a flash at the top and bottom of the collar's mouth.
    top, bot = K_AT(tc, 920, 540, 90), K_AT(tc, 1000, 540, 90)
    layer("k-latch", "".join(f'<circle cx="{x:.1f}" cy="{y:.1f}" r="9" fill="{GOLD}"/>' for x, y in (top, bot)), KM)
    # The chaser's jets: a puff ahead of each shoulder (braking, or backing away), behind the base
    # (closing), on its top (pushing it down, out of the way), and one stuck open, a long plume.
    sh_t, sh_b = K_AT(KC, 915, 440, 90), K_AT(KC, 1005, 440, 90)
    layer("k-brake", puff(sh_t[0] + 18, sh_t[1] - 6, 9) + puff(sh_b[0] + 18, sh_b[1] + 6, 9), KC)
    b_t, b_b = K_AT(KC, 920, 500, 90), K_AT(KC, 1000, 500, 90)
    layer("k-push", puff(b_t[0] - 16, b_t[1], 9) + puff(b_b[0] - 16, b_b[1], 9), KC)
    side = K_AT(KC, 915, 470, 90)
    layer("k-side", puff(side[0], side[1] - 16, 10), side)
    root = K_AT(KC, 915, 490, 90)
    layer("k-stuck", f'<path d="M{root[0] - 6:.1f} {root[1]:.1f} C{root[0] - 14:.1f} {root[1] - 40:.1f} {root[0] - 20:.1f} '
                     f'{root[1] - 80:.1f} {root[0] - 28:.1f} {root[1] - 120:.1f} L{root[0] - 12:.1f} {root[1] - 122:.1f} '
                     f'C{root[0] - 6:.1f} {root[1] - 80:.1f} {root[0] + 2:.1f} {root[1] - 40:.1f} {root[0] + 6:.1f} {root[1]:.1f} Z" '
                     f'fill="{GOLD_FILL}" stroke="{GOLD}" stroke-width="2" stroke-linejoin="round"/>', KC)


# --- Spacewalks: the way home's capsule (2x, nose right), a hatch on its top, a suited figure -------

# The figure stands at rest in the open hatch, facing the nose: its centre (and pivot) at EP, the
# tether's end at its backpack (EW), the hand that holds things and the gun at EH.
EP, EW, EH = (870, 300), (850, 300), (916, 296)
HATCH = (836, 926, 380)
# A spacewalk on a rendezvous (2026-10-18): the other capsule is this one mirrored about XP, its nose as far
# off as the rendezvous leaves it. A docked Agena: its cone's mouth at AGENA_MOUTH, round the capsule's nose.
XP = 1199
AGENA_MOUTH = 1084


def mirror(body, xm=XP):
    return f'<g transform="translate({2 * xm} 0) scale(-1 1)">{body}</g>'


def agena_docked(axis_y):
    """The Agena docked at the capsule's nose (nose right, at 1108 on axis_y), as big as the capsule, its light lit."""
    tc = (AGENA_MOUTH + RK * (540 - 426), axis_y)
    body = agena_target(2.4) + f'<circle cx="938" cy="508" r="4" fill="{GOLD}" {LINE} stroke-width="1.4"/>'
    return f'<g transform="translate({tc[0]} {tc[1]}) scale({RK}) rotate(90) translate(-960 -426)">{body}</g>'


def pair_parts(prefix, capsule_body, hole_body, hatch_body, hatch_pivot):
    """The other capsule of a rendezvous, mirrored: its body, the hole and the hatch (which turns the other way)."""
    layer(prefix + "e-capsule-pair", mirror(capsule_body), (2 * XP - RC[0], RC[1]))
    pivot = (2 * XP - hatch_pivot[0], hatch_pivot[1])
    layer(prefix + "e-hole-pair", mirror(hole_body), pivot)
    layer(prefix + "e-hatch-pair", mirror(hatch_body), pivot)


def suit():
    return (f'<path d="M858 318 L852 360 L862 366 M882 318 L890 360 L900 364" fill="none" {LINE} stroke-width="9"/>'
            f'<path d="M858 318 L852 360 L862 366 M882 318 L890 360 L900 364" fill="none" stroke="#ffffff" stroke-width="4" '
            f'stroke-linecap="round" stroke-linejoin="round"/>'
            f'<rect x="838" y="262" width="16" height="44" rx="3" fill="#ffffff" {LINE} stroke-width="3"/>'
            f'<rect x="852" y="262" width="38" height="58" rx="9" fill="#ffffff" {LINE} stroke-width="3"/>'
            f'<path d="M858 272 L846 302 M884 272 L914 294" fill="none" {LINE} stroke-width="9"/>'
            f'<path d="M858 272 L846 302 M884 272 L914 294" fill="none" stroke="#ffffff" stroke-width="4" stroke-linecap="round"/>'
            f'<circle cx="870" cy="242" r="21" fill="#ffffff" {LINE} stroke-width="3"/>'
            f'<path d="M874 230 Q892 232 890 246 Q888 258 874 256 Z" fill="#ffffff" {LINE} stroke-width="2.4"/>')


def eva_parts():
    layer("e-suit", suit(), EP)
    # The visor fogging over, and a puff from the hand-held gun ahead of the hand.
    layer("e-fog", f'<path d="M874 230 Q892 232 890 246 Q888 258 874 256 Z" fill="{GOLD}" opacity="0.85" {LINE} stroke-width="2.4"/>', EP)
    layer("e-gun", f'<path d="M912 292 L926 292" fill="none" {LINE} stroke-width="4"/>' + puff(940, 290, 9), EP)
    # The tether, drawn along +x from its pivot so the timeline can stretch and aim it.
    layer("e-tether", f'<path d="M0 0 C30 -10 70 10 100 0" transform="translate(850 300)" fill="none" {LINE} stroke-width="3"/>',
          EW)
    # The hatch, hinged at its front edge, and the dark opening under it.
    x0, x1, y = HATCH
    hole = f'<rect x="{x0 + 4}" y="{y - 2}" width="{x1 - x0 - 8}" height="10" fill="{NAVY}"/>'
    hatch = f'<rect x="{x0}" y="{y - 8}" width="{x1 - x0}" height="10" rx="3" fill="#ffffff" {LINE} stroke-width="3"/>'
    layer("e-hole", hole, (x1, y))
    layer("e-hatch", hatch, (x1, y))
    pair_parts("", RT(capsule()), hole, hatch, (x1, y))
    layer("e-agena", agena_docked(RC[1]), (AGENA_MOUTH, RC[1]))
    # A crew transfer: a second capsule docked nose to nose, a collar between the noses (1108 and
    # 1148), its hatch mirrored about the collar's middle, and a handrail between the two hatches.
    layer("e-capsule-b", f'<g transform="translate(1296 470) scale(2) rotate(-90) translate(-960 -426)">{capsule()}</g>', (1296, 470))
    layer("e-collar", f'<rect x="1090" y="452" width="76" height="36" rx="4" fill="#ffffff" {LINE} stroke-width="3"/>'
                      f'<path d="M1128 452 L1128 488" fill="none" {LINE} stroke-width="2"/>', (1128, 470))
    m0, m1 = 2256 - x1, 2256 - x0
    layer("e-hole-b", f'<rect x="{m0 + 4}" y="{y - 2}" width="{m1 - m0 - 8}" height="10" fill="{NAVY}"/>', (m0, y))
    layer("e-hatch-b", f'<rect x="{m0}" y="{y - 8}" width="{m1 - m0}" height="10" rx="3" fill="#ffffff" {LINE} stroke-width="3"/>',
          (m0, y))
    layer("e-rail", f'<path d="M944 352 L1312 352 M960 352 L960 378 M1296 352 L1296 378" fill="none" {LINE} stroke-width="4"/>',
          (1128, 352))
    # The experiment package on the hull, ahead of the hatch; its pivot is where the hand takes it.
    layer("e-box", f'<rect x="958" y="356" width="34" height="24" rx="3" fill="url(#hatch)" {LINE} stroke-width="3"/>'
                   f'<rect x="958" y="356" width="34" height="24" rx="3" fill="{GOLD_FILL}"/>', (975, 360))
    # The spacewalk's kit (2026-10-18, spacewalk.jsx's kit): what holds the package to the hull (none here, a
    # craft's own skin draws one), the collector plate on its boom that rises out of the package (pivot where
    # the boom leaves it, so it grows from there), the package's lamp, a micrometeoroid's spark, and the
    # hand-held camera (drawn in the figure's hand at rest, pivot there) and its flash at the lens.
    layer("e-mount", "", (975, 380))
    layer("e-boom", f'<path d="M975 356 L975 322" fill="none" {LINE} stroke-width="4"/>'
                    f'<rect x="951" y="306" width="48" height="16" rx="2" fill="url(#hatch)" {LINE} stroke-width="3"/>'
                    f'<rect x="951" y="306" width="48" height="16" rx="2" fill="{GOLD_FILL}"/>'
                    f'<path d="M967 306 L967 322 M983 306 L983 322" fill="none" {LINE} stroke-width="1.6"/>', (975, 356))
    layer("e-lamp", f'<circle cx="985" cy="368" r="5" fill="{GOLD}" {LINE} stroke-width="2"/>', (985, 368))
    layer("e-spark", f'<path d="M975 294 L980 307 L993 312 L980 317 L975 330 L970 317 L957 312 L970 307 Z" fill="{GOLD}" '
                     f'stroke="{GOLD}" stroke-width="1.5" stroke-linejoin="round"/>', (975, 312))
    layer("e-camera", f'<rect x="904" y="286" width="20" height="16" rx="2" fill="#ffffff" {LINE} stroke-width="3"/>'
                      f'<rect x="924" y="289" width="7" height="10" rx="1" fill="#ffffff" {LINE} stroke-width="2.4"/>'
                      f'<path d="M908 286 L910 281 L916 281 L918 286" fill="none" {LINE} stroke-width="2.4"/>', EH)
    flash = (f'<path d="M944 284 L947 291 L955 294 L947 297 L944 304 L941 297 L933 294 L941 291 Z" fill="#ffffff" '
             f'stroke="{GOLD}" stroke-width="2.4" stroke-linejoin="round"/>')
    layer("e-flash", flash, (944, 294))
    # Voskhod 2's (2026-10-18; any craft's): a puff from the suit's valve at the figure's hip, as Leonov bled his suit
    # down to fit back in.
    layer("e-vent", puff(836, 318, 12) + puff(822, 324, 8), EP)
    # The other capsule's crewman, standing in its hatch with a camera, all mirrored (pivots too).
    layer("e-suit-b", mirror(suit()), (2 * XP - EP[0], EP[1]))
    camera = (f'<rect x="904" y="286" width="20" height="16" rx="2" fill="#ffffff" {LINE} stroke-width="3"/>'
              f'<rect x="924" y="289" width="7" height="10" rx="1" fill="#ffffff" {LINE} stroke-width="2.4"/>'
              f'<path d="M908 286 L910 281 L916 281 L918 286" fill="none" {LINE} stroke-width="2.4"/>')
    layer("e-camera-b", mirror(camera), (2 * XP - EH[0], EH[1]))
    layer("e-flash-b", mirror(flash), (2 * XP - 944, 294))


# --- On the Moon: the lander (two stages), the surface close up, the limb seen from low orbit ----------

# The lander at rest stands on the surface (y 880) with its engine's centre on x 960; its pivot (and its
# carrier's) is at LC, the middle of the descent stage. Never a particular one: two stages, two legs.
LC = (960, 760)
GROUND_Y = 880


def lander_parts():
    layer("n-carrier", "", LC)
    legs = (f'<path d="M906 770 L820 876 M1014 770 L1100 876 M914 790 L850 852 M1006 790 L1070 852" fill="none" '
            f'{LINE} stroke-width="4"/>'
            f'<ellipse cx="818" cy="878" rx="18" ry="5" fill="#ffffff" {LINE} stroke-width="3"/>'
            f'<ellipse cx="1102" cy="878" rx="18" ry="5" fill="#ffffff" {LINE} stroke-width="3"/>')
    descent = (f'<path d="M946 792 L974 792 L984 822 L936 822 Z" fill="#ffffff" {LINE} stroke-width="3"/>'
               f'<path d="M912 722 L1008 722 L1022 738 L1022 778 L1008 794 L912 794 L898 778 L898 738 Z" fill="url(#hatch)" '
               f'{LINE} stroke-width="3"/>')
    layer("n-descent", legs + descent, LC)
    ascent = (f'<path d="M916 722 L916 684 L936 652 L986 652 L1006 674 L1006 722 Z" fill="#ffffff" {LINE} stroke-width="3"/>'
              f'<path d="M930 676 L946 662 L946 686 Z" fill="#ffffff" {LINE} stroke-width="2.4"/>'
              f'<rect x="950" y="640" width="22" height="12" rx="2" fill="#ffffff" {LINE} stroke-width="2.4"/>'
              f'<path d="M996 652 L1012 630" fill="none" {LINE} stroke-width="2.4"/>'
              f'<circle cx="1014" cy="626" r="6" fill="#ffffff" {LINE} stroke-width="2.4"/>')
    layer("n-ascent", ascent, LC)
    # The descent engine's plume, from the bell's mouth down, and the ascent engine's, from between the
    # stages (both pivot where they leave the engine, so they flicker and grow from there).
    layer("n-plume", f'<path d="M940 824 C944 850 952 880 960 910 C968 880 976 850 980 824 Z" fill="{GOLD_FILL}" '
                     f'stroke="{GOLD}" stroke-width="3" stroke-linejoin="round"/>', (960, 824))
    layer("n-flame-up", f'<path d="M948 724 C950 744 955 762 960 780 C965 762 970 744 972 724 Z" fill="{GOLD_FILL}" '
                        f'stroke="{GOLD}" stroke-width="3" stroke-linejoin="round"/>', (960, 724))
    # A ladder down the right leg, from the porch at the descent stage's top to the footpad.
    import math as _m
    (x0, y0), (x1, y1) = (1022, 726), (1098, 870)
    nx, ny = (y1 - y0), -(x1 - x0)
    n = _m.hypot(nx, ny)
    nx, ny = 9 * nx / n, 9 * ny / n
    rungs = "".join(f"M{x0 + (x1 - x0) * k / 6 - nx:.1f} {y0 + (y1 - y0) * k / 6 - ny:.1f} "
                    f"L{x0 + (x1 - x0) * k / 6 + nx:.1f} {y0 + (y1 - y0) * k / 6 + ny:.1f} " for k in range(1, 6))
    layer("n-ladder", f'<path d="M{x0 + nx:.1f} {y0 + ny:.1f} L{x1 + nx:.1f} {y1 + ny:.1f} {rungs}" fill="none" {LINE} '
                      f'stroke-width="2.4"/>', LC)
    # The dust the plume blows off the surface, either side.
    layer("n-dust", puff(860, 872, 20) + puff(800, 876, 14) + puff(1060, 872, 20) + puff(1120, 876, 14), (960, 880))


def lunokhod_parts():
    """Luna 17 and Lunokhod 1 (2026-10-16): the descent stage with its ramps, and the rover, in the Moon's
    close-up (the surface at GROUND_Y). The rover faces right; it's drawn on the stage's deck (its wheels'
    foot at (960, 776)), where its carrier stands, and its wheels and lid turn about their own pivots."""
    deck = 776
    # The stage: a deck on four spherical tanks (two seen), four legs, an engine bell below.
    stage = (f'<path d="M868 790 L800 876 M1052 790 L1120 876 M884 800 L836 860 M1036 800 L1084 860" fill="none" '
             f'{LINE} stroke-width="4"/>'
             f'<ellipse cx="798" cy="878" rx="18" ry="5" fill="#ffffff" {LINE} stroke-width="3"/>'
             f'<ellipse cx="1122" cy="878" rx="18" ry="5" fill="#ffffff" {LINE} stroke-width="3"/>'
             f'<path d="M944 820 L976 820 L986 850 L934 850 Z" fill="#ffffff" {LINE} stroke-width="3"/>'
             f'<circle cx="912" cy="816" r="30" fill="#ffffff" {LINE} stroke-width="3"/>'
             f'<circle cx="1008" cy="816" r="30" fill="#ffffff" {LINE} stroke-width="3"/>'
             f'<rect x="860" y="{deck}" width="200" height="16" fill="url(#hatch)" {LINE} stroke-width="3"/>')
    layer("u-stage", stage, (960, deck))
    # A ramp: two rails and their rungs, drawn along +x from its hinge at the deck's edge (the timeline
    # turns it down to the ground, from folded up).
    rails = (f'<path d="M0 -3 L208 -3 M0 5 L208 5" fill="none" {LINE} stroke-width="3"/>'
             + "".join(f'<path d="M{x} -3 L{x} 5" fill="none" {LINE} stroke-width="2"/>' for x in range(20, 208, 26)))
    layer("u-ramp-f", f'<g transform="translate(1060 {deck})">{rails}</g>', (1060, deck))
    layer("u-ramp-r", f'<g transform="translate(860 {deck}) scale(-1 1)">{rails}</g>', (860, deck))
    # The rover, its wheels' foot at (960, deck): a tub wider at the top, the lid on it (solar cells on its
    # underside, seen once it's open), the cameras and the laser reflector at the front (right), a pencil
    # antenna and a cone antenna behind the lid.
    o = (960, deck)

    def at(d):
        return f'<g transform="translate({o[0]} {o[1]})">{d}</g>'
    layer("u-rover", "", o)
    tub = (f'<path d="M-62 -26 L62 -26 L74 -70 L-74 -70 Z" fill="#ffffff" {LINE} stroke-width="3"/>'
           f'<path d="M-66 -48 L66 -48" fill="none" {LINE} stroke-width="1.6"/>'
           f'<circle cx="64" cy="-58" r="5" fill="#ffffff" {LINE} stroke-width="2"/>'
           f'<circle cx="60" cy="-38" r="5" fill="#ffffff" {LINE} stroke-width="2"/>'
           f'<rect x="72" y="-68" width="10" height="12" fill="url(#hatch)" {LINE} stroke-width="2"/>')
    layer("u-tub", at(tub), o)
    ant = (f'<path d="M-44 -70 L-44 -150" fill="none" {LINE} stroke-width="2.4"/>'
           f'<circle cx="-44" cy="-152" r="4" fill="#ffffff" {LINE} stroke-width="2"/>'
           f'<path d="M24 -70 L24 -96 M14 -96 L34 -96 L24 -118 Z" fill="#ffffff" {LINE} stroke-width="2.4"/>')
    layer("u-ant", at(ant), o)
    lid = (f'<path d="M-76 -70 L76 -70 L76 -64 L-76 -64 Z" fill="url(#hatch)" {LINE} stroke-width="2"/>'
           f'<path d="M-76 -70 Q-72 -98 0 -100 Q72 -98 76 -70 Z" fill="#ffffff" {LINE} stroke-width="3"/>')
    layer("u-lid", at(lid), (o[0] - 76, o[1] - 70))
    # A soil heap on the lid (the failure), and the lid glowing as the rover overheats.
    layer("u-soil", at(f'<path d="M-40 -100 Q-20 -116 4 -104 Q22 -114 40 -98 Z" fill="url(#earth)" {LINE} stroke-width="2"/>'),
          (o[0] - 76, o[1] - 70))
    layer("u-glow", at(f'<path d="M-82 -68 Q-78 -108 0 -110 Q78 -108 82 -68" fill="{GOLD_FILL}" stroke="{GOLD}" '
                       f'stroke-width="3"/>'), (o[0] - 76, o[1] - 70))
    for i, x in enumerate((-54, -18, 18, 54), 1):
        spokes = "".join(f'<path d="M{x + 4 * math.cos(a):.1f} {-15 + 4 * math.sin(a):.1f} L{x + 15 * math.cos(a):.1f} '
                         f'{-15 + 15 * math.sin(a):.1f}" fill="none" {LINE} stroke-width="1.6"/>'
                         for a in (k * math.pi / 4 for k in range(8)))
        layer(f"u-wheel-{i}", at(f'<circle cx="{x}" cy="-15" r="15" fill="#ffffff" {LINE} stroke-width="3"/>{spokes}'
                                 f'<circle cx="{x}" cy="-15" r="4" fill="#ffffff" {LINE} stroke-width="2"/>'),
              (o[0] + x, o[1] - 15))
    # The dust its wheels kick up (behind it), and a crater's rim further on: a low mound on the way.
    layer("u-kick", puff(880, 870, 12) + puff(860, 874, 8), (870, 874))
    layer("u-rim", f'<path d="M1820 {GROUND_Y + 2} Q1990 {GROUND_Y - 96} 2160 {GROUND_Y + 2} Z" fill="#ffffff"/>'
                   f'<path d="M1820 {GROUND_Y + 2} Q1990 {GROUND_Y - 96} 2160 {GROUND_Y + 2} Z" fill="url(#earth)" {LINE} '
                   f'stroke-width="3"/><path d="M1840 {GROUND_Y + 2} L2140 {GROUND_Y + 2}" fill="none" stroke="#ffffff" '
                   f'stroke-width="4"/>', (1990, GROUND_Y))
    # The whole lander on a carrier for the landing (2026-10-16), the stage's engine plume, and Luna 15's
    # payload: the return rocket (a stack of tanks) with the return capsule (a sphere) on top, and the drill
    # arm at the deck's front edge, drawn along +x from its hinge (the timeline swings it).
    layer("u-lander", "", (960, deck))
    layer("u-plume", f'<path d="M940 852 C944 878 952 908 960 938 C968 908 976 878 980 852 Z" fill="{GOLD_FILL}" '
                     f'stroke="{GOLD}" stroke-width="3" stroke-linejoin="round"/>', (960, 852))
    rocket = (f'<rect x="924" y="716" width="72" height="60" rx="6" fill="#ffffff" {LINE} stroke-width="3"/>'
              f'<path d="M924 736 L996 736 M924 756 L996 756" fill="none" {LINE} stroke-width="1.6"/>'
              f'<rect x="924" y="736" width="72" height="20" fill="url(#hatch)" {LINE} stroke-width="1.6"/>'
              f'<path d="M960 716 L960 776" fill="none" {LINE} stroke-width="1.6"/>'
              f'<path d="M940 716 L948 700 L972 700 L980 716 Z" fill="#ffffff" {LINE} stroke-width="2.4"/>'
              f'<path d="M996 724 L1012 700" fill="none" {LINE} stroke-width="2.4"/>')
    layer("s-rocket", rocket, (960, deck))
    layer("s-capsule", f'<circle cx="960" cy="678" r="22" fill="#ffffff" {LINE} stroke-width="3"/>'
                       f'<path d="M938 678 L982 678" fill="none" {LINE} stroke-width="1.6"/>', (960, 678))
    arm = (f'<path d="M0 0 L132 0" fill="none" {LINE} stroke-width="4"/>'
           f'<circle cx="0" cy="0" r="6" fill="#ffffff" {LINE} stroke-width="2.4"/>'
           f'<rect x="126" y="-7" width="16" height="14" rx="2" fill="#ffffff" {LINE} stroke-width="2.4"/>'
           f'<path d="M142 0 L154 0 M144 -4 L152 4 M144 4 L152 -4" fill="none" {LINE} stroke-width="2"/>')
    layer("s-arm", f'<g transform="translate(1060 {deck - 6})">{arm}</g>', (1060, deck - 6))
    layer("s-core", f'<rect x="1196" y="{deck - 10}" width="14" height="8" rx="2" fill="url(#earth)" {LINE} '
                    f'stroke-width="2"/>', (1203, deck - 6))
    layer("s-flame", f'<path d="M946 778 C948 800 954 822 960 846 C966 822 972 800 974 778 Z" fill="{GOLD_FILL}" '
                     f'stroke="{GOLD}" stroke-width="3" stroke-linejoin="round"/>', (960, deck))
    layer("s-flash", f'<circle cx="960" cy="678" r="30" fill="none" stroke="{GOLD}" stroke-width="4"/>', (960, 678))
    # A laser from Earth (up right), bouncing off the reflector: a gold line drawn from its foot up.
    layer("u-laser", f'<path d="M0 0 L700 -700" fill="none" stroke="{GOLD}" stroke-width="3" stroke-dasharray="18 10"/>',
          (0, 0))


def lunar_parts():
    lander_parts()
    # The surface close up: a horizon of low rises, craters seen flat, a boulder field right of the
    # landing spot and a crater rim further on.
    rises = " ".join(f"Q{x + 160} {GROUND_Y - (10 if (x // 320) % 2 else 4)} {x + 320} {GROUND_Y}" for x in range(-900, 2900, 320))
    craters = "".join(f'<ellipse cx="{x}" cy="{y}" rx="{rx}" ry="{rx * 0.22:.1f}" fill="#ffffff" {LINE} stroke-width="2.4"/>'
                      for x, y, rx in ((420, 930, 60), (760, 990, 40), (1500, 960, 80), (240, 1030, 50), (1180, 1040, 46)))
    layer("n-ground", f'<rect x="-900" y="{GROUND_Y}" width="3800" height="900" fill="url(#earth)"/>'
                      f'<path d="M-900 {GROUND_Y} {rises}" fill="none" {LINE} stroke-width="3"/>{craters}', (960, GROUND_Y))
    rocks = "".join(f'<path d="M{x - w} {GROUND_Y + 4} Q{x - w} {GROUND_Y - h} {x} {GROUND_Y - h} Q{x + w} {GROUND_Y - h} {x + w} {GROUND_Y + 4} Z" '
                    f'fill="#ffffff" {LINE} stroke-width="3"/>'
                    for x, w, h in ((1240, 22, 26), (1300, 30, 40), (1372, 18, 22), (1430, 26, 34), (1200, 14, 14), (1490, 16, 18)))
    layer("n-rocks", rocks, (1340, GROUND_Y))
    # A lamp panel, top right, fixed to the screen: a label and a lamp; the lamp's gold fill is its own part.
    for name, label in (("alarm", "ALARM"), ("fuel", "FUEL")):
        layer(f"n-panel-{name}", f'<rect x="1580" y="50" width="290" height="70" rx="8" fill="#ffffff" {LINE} stroke-width="3"/>'
                                 f'<text x="1606" y="96" {LABEL}>{label}</text>'
                                 f'<circle cx="1826" cy="85" r="18" fill="#ffffff" {LINE} stroke-width="3"/>', (1725, 85))
    layer("n-lamp", f'<circle cx="1826" cy="85" r="14" fill="{GOLD}"/>', (1826, 85))
    # The Moon's limb seen from low orbit, for the descent from it.
    cx, cy, r = 960, 3000, 2200
    marks = "".join(f'<ellipse cx="{x}" cy="{y}" rx="{rx}" ry="{rx * 0.3:.1f}" fill="#ffffff" {LINE} stroke-width="2.4"/>'
                    for x, y, rx in ((300, 960, 50), (700, 880, 34), (1180, 900, 60), (1600, 990, 44), (1000, 1020, 30)))
    layer("n-limb", f'<circle cx="{cx}" cy="{cy}" r="{r}" fill="url(#earth)" {LINE} stroke-width="3"/>{marks}', (cx, cy))


# Apollo's moonwalks (user, 2026-10-20): the flag and the rock box, beside the lander (the surface close up).
# The flag stands at FLAG_X: a plain cloth, no country's. Its pole pivots where the hand holds it once it's in
# the ground (the hand's height over the feet: suit()'s EH less FP, scaled 0.45); the cloth, on its telescoping
# crossbar, pivots at the pole's top, so a horizontal scale runs the bar out; the furled cloth hangs on the pole.
FLAG_X, FLAG_TOP = 1180, 796
FLAG_GRIP = (FLAG_X, GROUND_Y - 0.45 * 70)


def moonwalk_kit_parts():
    x, top = FLAG_X, FLAG_TOP
    layer("e-flag-pole", f'<path d="M{x} {top} L{x} {GROUND_Y + 6}" fill="none" {LINE} stroke-width="3"/>'
                         f'<circle cx="{x}" cy="{top - 3}" r="3.5" fill="#ffffff" {LINE} stroke-width="2"/>', FLAG_GRIP)
    # The cloth 0.9 by 1.5 m, as Apollo's were (3 by 5 feet).
    edge = (f'M{x + 1} {top + 3} L{x + 63} {top + 3} L{x + 63} {top + 41} Q{x + 47} {top + 45} {x + 32} {top + 41} '
            f'Q{x + 16} {top + 37} {x + 1} {top + 41} Z')
    cloth = (f'<path d="M{x} {top + 2} L{x + 64} {top + 2}" fill="none" {LINE} stroke-width="3"/>'
             f'<path d="{edge}" fill="url(#hatch)" {LINE} stroke-width="2.6"/><path d="{edge}" fill="{GOLD_FILL}"/>')
    layer("e-flag-cloth", cloth, (x, top + 2))
    # The crossbar stuck part-way (Apollo 11's): the cloth bunched in folds along it.
    folds = "".join(f'<path d="M{x + k} {top + 4} L{x + k + 2} {top + 38}" fill="none" {LINE} stroke-width="1.6" opacity="0.7"/>'
                    for k in (13, 26, 39, 52))
    layer("e-flag-folds", folds, (x, top + 2))
    layer("e-flag-furl", f'<rect x="{x + 1}" y="{top + 4}" width="7" height="40" rx="3" fill="url(#hatch)" {LINE} stroke-width="2.4"/>'
                         f'<rect x="{x + 1}" y="{top + 4}" width="7" height="40" rx="3" fill="{GOLD_FILL}"/>', (x, top + 2))
    # The rock box, an aluminium case with ribs and a handle on its lid, drawn the size of the experiment package's
    # frame (e-box: its pivot, where the hand holds it, at the handle); the lid hinges at its back edge.
    body = (f'<rect x="950" y="364" width="50" height="30" rx="3" fill="#ffffff" {LINE} stroke-width="3"/>'
            f'<path d="M962 368 L962 390 M975 368 L975 390 M988 368 L988 390" fill="none" {LINE} stroke-width="1.6" opacity="0.6"/>')
    layer("e-rockbox", body, (975, 360))
    layer("e-rockbox-lid", f'<rect x="948" y="358" width="54" height="7" rx="2" fill="#ffffff" {LINE} stroke-width="2.6"/>'
                           f'<path d="M966 358 L966 352 L984 352 L984 358" fill="none" {LINE} stroke-width="2.6"/>', (948, 362))
    # Two rocks to pick up, out beyond the boulders where the figure stops (samples.jsx's P1 and P2), gold so they
    # read as the samples; pivot at their middles.
    for i, (rx, w, h) in enumerate(((1630, 11, 9), (1670, 9, 8)), 1):
        rock = f'M{rx - w} {GROUND_Y} Q{rx - w} {GROUND_Y - h} {rx} {GROUND_Y - h} Q{rx + w} {GROUND_Y - h} {rx + w} {GROUND_Y} Z'
        layer(f"e-pebble-{i}", f'<path d="{rock}" fill="#ffffff"/><path d="{rock}" fill="{GOLD_FILL}" {LINE} stroke-width="2.4"/>',
              (rx, GROUND_Y - h / 2))


# The rover (user, 2026-10-20), Apollo's: side on, travelling right, about 40 px/m like the lander. Its middle at
# LRV (the frame's middle, between the wheels), wheels 0.8 m across at LRV_WHEELS; the two crew sit side by side, the
# far one a little behind and higher; the dish and the TV camera on the front, the tool pallet behind.
LRV = (1400, 846)
LRV_WHEELS = (1354, 1446)  # rear, front
LRV_R = 16


def seated(hx, hy, far=False):
    """A suited figure in a seat, side on, facing right: its hip at (hx, hy), at the standing figure's scale."""
    body = (f'<rect x="838" y="262" width="16" height="44" rx="3" fill="#ffffff" {LINE} stroke-width="3"/>'
            f'<rect x="852" y="262" width="38" height="58" rx="9" fill="#ffffff" {LINE} stroke-width="3"/>'
            f'<path d="M872 314 L910 316 L914 352 L926 354" fill="none" {LINE} stroke-width="9"/>'
            f'<path d="M872 314 L910 316 L914 352 L926 354" fill="none" stroke="#ffffff" stroke-width="4" '
            f'stroke-linecap="round" stroke-linejoin="round"/>'
            f'<path d="M878 274 L900 296 L916 292" fill="none" {LINE} stroke-width="9"/>'
            f'<path d="M878 274 L900 296 L916 292" fill="none" stroke="#ffffff" stroke-width="4" stroke-linecap="round"/>'
            f'<circle cx="870" cy="242" r="21" fill="#ffffff" {LINE} stroke-width="3"/>'
            f'<path d="M874 230 Q892 232 890 246 Q888 258 874 256 Z" fill="{GOLD_FILL}" {LINE} stroke-width="2.4"/>')
    fade = ' opacity="0.75"' if far else ""
    return f'<g transform="translate({hx} {hy}) scale(0.45) translate(-872 -316)"{fade}>{body}</g>'


def lrv_parts():
    cx, cy = LRV
    rx, fx = LRV_WHEELS
    wy = GROUND_Y - LRV_R
    # The chassis, its seats' backs, the fenders over the wheels (the rear one's extension its own part), the
    # console between the seats, the dish on its mast and the TV camera, the tool pallet at the back.
    frame = (f'<path d="M{cx - 66} {cy} L{cx + 66} {cy}" fill="none" {LINE} stroke-width="5"/>'
             f'<path d="M{fx - 22} {cy + 2} Q{fx} {wy - 30} {fx + 22} {cy + 2} M{rx - 22} {cy + 2} Q{rx} {wy - 30} {rx + 22} {cy + 2}" '
             f'fill="none" {LINE} stroke-width="3"/>'
             f'<path d="M{cx - 30} {cy} L{cx - 36} {cy - 34} M{cx - 4} {cy} L{cx - 10} {cy - 34}" fill="none" {LINE} stroke-width="3"/>'
             f'<rect x="{cx - 76}" y="{cy - 22}" width="26" height="20" rx="2" fill="url(#hatch)" {LINE} stroke-width="2.6"/>'
             f'<path d="M{cx + 30} {cy} L{cx + 30} {cy - 30} L{cx + 40} {cy - 34}" fill="none" {LINE} stroke-width="3"/>'
             f'<path d="M{cx + 56} {cy} L{cx + 56} {cy - 66}" fill="none" {LINE} stroke-width="2.6"/>'
             f'<path d="M{cx + 40} {cy - 74} Q{cx + 56} {cy - 58} {cx + 72} {cy - 74} Z" fill="#ffffff" {LINE} stroke-width="2.6"/>'
             f'<rect x="{cx + 58}" y="{cy - 22}" width="14" height="10" rx="2" fill="#ffffff" {LINE} stroke-width="2.4"/>'
             f'<path d="M{cx + 72} {cy - 17} L{cx + 78} {cy - 17}" fill="none" {LINE} stroke-width="2.4"/>')
    layer("lrv-body", frame, LRV)
    layer("lrv-crew", seated(cx - 4, cy - 4, far=True) + seated(cx - 22, cy - 2), LRV)
    layer("lrv-fender", f'<path d="M{rx - 22} {cy + 2} L{rx - 36} {cy + 10}" fill="none" {LINE} stroke-width="3"/>',
          (rx - 22, cy + 2))
    wheel = (f'<circle cx="0" cy="0" r="{LRV_R}" fill="url(#hatch)" {LINE} stroke-width="3"/>'
             f'<circle cx="0" cy="0" r="5" fill="#ffffff" {LINE} stroke-width="2.4"/>'
             + "".join(f'<path d="M0 0 L{LRV_R * math.cos(k * math.pi / 3):.1f} {LRV_R * math.sin(k * math.pi / 3):.1f}" '
                       f'fill="none" {LINE} stroke-width="1.6" opacity="0.7"/>' for k in range(6)))
    for name, x in (("lrv-wheel-front", fx), ("lrv-wheel-rear", rx)):
        layer(name, f'<g transform="translate({x} {wy})">{wheel}</g>', (x, wy))
    # The dust thrown up behind the rear wheel, low; and the rooster tail with the fender gone, arcing over the crew.
    layer("lrv-dust", puff(rx - 26, GROUND_Y - 6, 8) + puff(rx - 40, GROUND_Y - 10, 6), (rx - 20, GROUND_Y - 4))
    spray = ""
    for k in range(14):
        u = k / 13
        sx, sy = rx - 4 + 120 * u, GROUND_Y - 30 - 90 * u + 70 * u * u
        spray += (f'<circle cx="{sx:.1f}" cy="{sy:.1f}" r="{4 + 4 * math.sin(math.pi * u) + (k % 2):.1f}" '
                  f'fill="rgba(224,167,46,0.5)" stroke="{GOLD}" stroke-width="2"/>')
    layer("lrv-spray", spray, (rx - 20, GROUND_Y - 10))
    # The crew off the rover, walking back: two standing figures (e-suit's drawing).
    layer("lrv-walker", suit(), EP)
    # The drive's ground beyond the lander: craters and rocks out to 3400, the rock the wheels go over (pivot on
    # the ground), and a far ridge drawn long, moved slower than the camera.
    craters = "".join(f'<ellipse cx="{x}" cy="{y}" rx="{r}" ry="{r * 0.22:.1f}" fill="#ffffff" {LINE} stroke-width="2.4"/>'
                      for x, y, r in ((1700, 940, 50), (2050, 1000, 70), (2380, 930, 40), (2700, 980, 90), (3050, 950, 60)))
    rocks = "".join(f'<path d="M{x - w} {GROUND_Y + 4} Q{x - w} {GROUND_Y - h} {x} {GROUND_Y - h} Q{x + w} {GROUND_Y - h} {x + w} '
                    f'{GROUND_Y + 4} Z" fill="#ffffff" {LINE} stroke-width="3"/>'
                    for x, w, h in ((1640, 12, 12), (1980, 18, 22), (2240, 10, 9), (2520, 24, 30), (2860, 14, 14), (3200, 20, 24)))
    layer("lrv-terrain", f'<rect x="2800" y="{GROUND_Y}" width="1200" height="900" fill="url(#earth)"/>'
                         f'<path d="M2800 {GROUND_Y} L4000 {GROUND_Y}" fill="none" {LINE} stroke-width="3"/>{craters}{rocks}',
          (2000, GROUND_Y))
    layer("lrv-bump", f'<path d="M1834 {GROUND_Y + 3} Q1836 {GROUND_Y - 9} 1846 {GROUND_Y - 9} Q1856 {GROUND_Y - 9} 1858 {GROUND_Y + 3} Z" '
                      f'fill="#ffffff" {LINE} stroke-width="2.6"/>', (1846, GROUND_Y))
    ridge = " ".join(f"Q{x + 150} {GROUND_Y - (110 if (x // 300) % 3 == 1 else 60)} {x + 300} {GROUND_Y - 20}" for x in range(-900, 4200, 300))
    layer("lrv-ridge", f'<path d="M-900 {GROUND_Y - 20} {ridge}" fill="none" {LINE} stroke-width="2.4" opacity="0.45"/>',
          (960, GROUND_Y))


def surveyor_parts():
    """Surveyor (2026-10-17) in the Moon's close-up, on a carrier at LC like the crewed lander, its footpads on
    GROUND_Y: an open tripod frame with three legs (the near one in front), two boxed compartments, the TV
    camera with its mirror hood, a mast with the planar antenna and the solar panel (its own part, turned
    about the mast's top to find the Sun), and three small vernier engines underneath, a plume each. The big
    solid retro rocket is gone by then: it burned out and fell away kilometres up."""
    layer("sv-carrier", "", LC)
    legs = (f'<path d="M884 804 L816 870 M920 772 L830 858 M1036 804 L1104 870 M1000 772 L1090 858 '
            f'M948 806 L928 884" fill="none" {LINE} stroke-width="4"/>'
            f'<ellipse cx="812" cy="874" rx="20" ry="6" fill="#ffffff" {LINE} stroke-width="3"/>'
            f'<ellipse cx="1108" cy="874" rx="20" ry="6" fill="#ffffff" {LINE} stroke-width="3"/>'
            f'<ellipse cx="926" cy="887" rx="22" ry="6" fill="#ffffff" {LINE} stroke-width="3"/>')
    layer("sv-legs", legs, LC)
    bells = "".join(f'<path d="M{x - 6} {y} L{x + 6} {y} L{x + 10} {y + 16} L{x - 10} {y + 16} Z" fill="#ffffff" '
                    f'{LINE} stroke-width="2.4"/>' for x, y in ((900, 804), (1020, 804), (968, 810)))
    frame = (f'<path d="M880 804 L1040 804 L960 724 Z M908 776 L1012 776 M960 804 L960 596" fill="none" {LINE} '
             f'stroke-width="3"/>')
    boxes = (f'<rect x="886" y="770" width="50" height="30" fill="#ffffff" {LINE} stroke-width="3"/>'
             f'<rect x="886" y="770" width="50" height="9" fill="url(#hatch)" {LINE} stroke-width="2"/>'
             f'<rect x="984" y="774" width="40" height="26" fill="#ffffff" {LINE} stroke-width="3"/>'
             f'<rect x="984" y="774" width="40" height="8" fill="url(#hatch)" {LINE} stroke-width="2"/>')
    camera = (f'<rect x="1030" y="742" width="16" height="50" rx="3" fill="#ffffff" {LINE} stroke-width="3"/>'
              f'<path d="M1028 742 L1048 742 L1056 722 L1034 726 Z" fill="url(#hatch)" {LINE} stroke-width="2.4"/>')
    grid = "".join(f'M{980 + 16 * k} 572 L{980 + 16 * k} 620 M980 {572 + 16 * k} L1028 {572 + 16 * k} ' for k in (1, 2))
    antenna = (f'<path d="M960 604 L980 598" fill="none" {LINE} stroke-width="3"/>'
               f'<rect x="980" y="572" width="48" height="48" fill="#ffffff" {LINE} stroke-width="3"/>'
               f'<path d="{grid}" fill="none" {LINE} stroke-width="1.6"/>')
    layer("sv-body", bells + frame + boxes + camera + antenna, LC)
    cells = "".join(f'M{846 + 24 * k} 568 L{846 + 24 * k} 620 ' for k in (1, 2, 3)) + 'M846 594 L942 594'
    layer("sv-panel", f'<path d="M960 600 L942 596" fill="none" {LINE} stroke-width="3"/>'
                      f'<rect x="846" y="568" width="96" height="52" fill="url(#hatch)" {LINE} stroke-width="3"/>'
                      f'<path d="{cells}" fill="none" {LINE} stroke-width="1.6"/>', (960, 600))
    # The verniers' plumes, from each bell's mouth down (left, right, the near one in the middle).
    for i, (x, y) in enumerate(((900, 820), (1020, 820), (968, 826)), 1):
        layer(f"sv-plume-{i}", f'<path d="M{x - 10} {y} C{x - 8} {y + 18} {x - 4} {y + 38} {x} {y + 56} C{x + 4} '
                               f'{y + 38} {x + 8} {y + 18} {x + 10} {y} Z" fill="{GOLD_FILL}" stroke="{GOLD}" '
                               f'stroke-width="3" stroke-linejoin="round"/>', (x, y))
    # The dust thrown up where something hits the ground (the failure's antenna), centred on its pivot.
    layer("sv-thud", puff(940, 872, 16) + puff(980, 868, 22) + puff(1020, 874, 14), (980, 880))


# --- Transposition and extraction: the third stage with the lander in its adapter -------------------

def stage_parts():
    # The third stage, lying along the way (engine right), its front at x 1080; the lander rides in the
    # adapter ahead of it, which the capsule's base closes at x 940 at launch.
    layer("t-stage", f'<rect x="1080" y="400" width="420" height="140" fill="#ffffff" {LINE} stroke-width="3"/>'
                     f'<path d="M1180 400 L1180 540 M1420 400 L1420 540" fill="none" {LINE} stroke-width="2"/>'
                     f'<path d="M1500 440 L1544 420 L1544 520 L1500 500 Z" fill="#ffffff" {LINE} stroke-width="3"/>',
          (1290, 470))
    # The adapter's two panels seen side on, hinged at the stage's front edge; open, they swing out.
    layer("t-panel-top", f'<path d="M1080 400 L940 422 L940 430 L1080 410 Z" fill="#ffffff" {LINE} stroke-width="3"/>', (1080, 405))
    layer("t-panel-bottom", f'<path d="M1080 540 L940 518 L940 510 L1080 530 Z" fill="#ffffff" {LINE} stroke-width="3"/>', (1080, 535))
    # The flashes: the capsule letting go of the adapter, and the lander letting go of the stage.
    layer("t-flash-sep", "".join(f'<circle cx="940" cy="{y}" r="9" fill="{GOLD}"/>' for y in (424, 516)), (940, 470))
    layer("t-flash-lm", "".join(f'<circle cx="1078" cy="{y}" r="9" fill="{GOLD}"/>' for y in (414, 526)), (1078, 470))


# --- The spaceplane: a rocket plane landing on a dry lake ----------------------------------------------

# Never a particular one: a long body, a canopy, a wedge tail, a stub wing, a skid at the back and a nose
# wheel. It stands on the ground (y 880), its pivot where the skid touches (WS).
WS = (830, 880)


def plane_parts():
    body = (f'<path d="M748 806 L760 806 L760 834 L748 834 Z" fill="#ffffff" {LINE} stroke-width="3"/>'
            f'<path d="M760 742 L796 742 L846 800 L760 800 Z" fill="#ffffff" {LINE} stroke-width="3"/>'
            f'<path d="M760 800 L1100 800 Q1172 812 1196 820 Q1172 828 1100 840 L760 840 Z" fill="#ffffff" {LINE} stroke-width="3"/>'
            f'<path d="M1052 800 Q1080 778 1112 800" fill="#ffffff" {LINE} stroke-width="3"/>'
            f'<path d="M880 830 L998 822 L1018 828 L900 836 Z" fill="url(#hatch)" {LINE} stroke-width="2.4"/>'
            f'<path d="M1100 840 L1100 872" fill="none" {LINE} stroke-width="3"/>'
            f'<circle cx="1100" cy="875" r="5" fill="#ffffff" {LINE} stroke-width="2.4"/>')
    layer("w-plane", body, WS)
    # The main skid on its strut, hinged at the body, so it can fold up when it gives way.
    layer("w-skid", f'<path d="M842 840 L830 878 M806 880 L856 880" fill="none" {LINE} stroke-width="4"/>', (842, 840))
    # The dry lake: flat to the horizon, hills along it, a runway's marks.
    hills = " ".join(f"Q{x + 70} {880 - h} {x + 140} 880" for x, h in zip(range(-200, 2100, 140), [15, 26, 11, 32, 20, 9, 23, 35, 17, 13, 29, 19, 10, 24, 31, 15, 21]))
    layer("w-ground", f'<path d="M-200 880 {hills}" fill="none" {LINE} stroke-width="2" opacity="0.5"/>'
                      f'<rect x="-200" y="880" width="2400" height="400" fill="url(#earth)"/>'
                      f'<path d="M-200 880 L2200 880" fill="none" {LINE} stroke-width="3"/>'
                      + "".join(f'<path d="M{x} 912 L{x + 70} 912" fill="none" {LINE} stroke-width="3" opacity="0.6"/>'
                                for x in range(-160, 2200, 160)), (960, 880))
    # Sparks off the tail scraping along, streaming back (left) from its pivot.
    layer("w-sparks", f'<path d="M760 878 L640 870 M760 878 L660 884 M760 878 L700 862" fill="none" stroke="{GOLD}" '
                      f'stroke-width="3" stroke-linecap="round"/>', (760, 878))


# --- Probe landers on the planets: the bus that carries one, the lander, its mast and arm ----------------

# The lander at rest stands on the surface (y 880) centred on x 760: a sphere on a landing ring, a mast
# on top (its own part, raised once down) and a sampler arm (its own part, swung out from the side).
VL = (760, 812)


def probe_parts():
    x, y = VL
    lander = (f'<path d="M{x - 40} {y + 30} L{x - 62} {y + 62} M{x + 40} {y + 30} L{x + 62} {y + 62}" fill="none" {LINE} stroke-width="4"/>'
              f'<ellipse cx="{x}" cy="{y + 62}" rx="74" ry="8" fill="#ffffff" {LINE} stroke-width="3"/>'
              f'<circle cx="{x}" cy="{y}" r="48" fill="#ffffff" {LINE} stroke-width="3"/>'
              f'<path d="M{x - 48} {y} Q{x} {y + 16} {x + 48} {y}" fill="none" {LINE} stroke-width="2"/>'
              f'<ellipse cx="{x}" cy="{y - 6}" rx="22" ry="10" fill="url(#hatch)" {LINE} stroke-width="2"/>')
    layer("v-lander", lander, (x, y))
    # The mast with its dish, pivoting at the sphere's top so it can be raised from lying flat.
    layer("v-mast", f'<path d="M{x} {y - 48} L{x} {y - 120}" fill="none" {LINE} stroke-width="4"/>'
                    f'<path d="M{x - 22} {y - 132} Q{x} {y - 112} {x + 22} {y - 132}" fill="#ffffff" {LINE} stroke-width="3"/>',
          (x, y - 48))
    # The sampler arm, drawn out to the right along the ground, pivoting where it leaves the sphere.
    layer("v-arm", f'<path d="M{x + 40} {y + 20} L{x + 150} {y + 30} L{x + 210} {y + 50}" fill="none" {LINE} stroke-width="5"/>'
                   f'<path d="M{x + 202} {y + 44} L{x + 226} {y + 60} L{x + 210} {y + 66} Z" fill="#ffffff" {LINE} stroke-width="3"/>',
          (x + 40, y + 20))
    # A puff of soil the scoop lifts.
    layer("v-soil", puff(x + 230, y + 62, 10), (x + 230, y + 66))
    # The bus that carries a lander to its planet: a body, two solar panels, a dish; the lander rides
    # under its body (at rest there, pivot at the joint).
    bx, by = 960, 380
    bus = (f'<path d="M{bx - 300} {by - 14} L{bx - 90} {by - 14} L{bx - 90} {by + 14} L{bx - 300} {by + 14} Z" fill="url(#hatch)" {LINE} stroke-width="3"/>'
           f'<path d="M{bx + 90} {by - 14} L{bx + 300} {by - 14} L{bx + 300} {by + 14} L{bx + 90} {by + 14} Z" fill="url(#hatch)" {LINE} stroke-width="3"/>'
           f'<rect x="{bx - 70}" y="{by - 60}" width="140" height="120" rx="8" fill="#ffffff" {LINE} stroke-width="3"/>'
           f'<path d="M{bx - 70} {by - 70} L{bx - 90} {by} L{bx - 70} {by + 70} M{bx + 70} {by - 70} L{bx + 90} {by} L{bx + 70} {by + 70}" fill="none" {LINE} stroke-width="2"/>'
           f'<path d="M{bx - 50} {by - 60} Q{bx} {by - 120} {bx + 50} {by - 60} Z" fill="#ffffff" {LINE} stroke-width="3"/>'
           f'<path d="M{bx - 40} {by + 60} L{bx - 30} {by + 76} L{bx + 30} {by + 76} L{bx + 40} {by + 60}" fill="none" {LINE} stroke-width="3"/>')
    layer("v-bus", bus, (bx, by))
    layer("v-pod", f'<circle cx="{bx}" cy="{by + 126}" r="48" fill="#ffffff" {LINE} stroke-width="3"/>'
                   f'<path d="M{bx - 48} {by + 126} Q{bx} {by + 142} {bx + 48} {by + 126}" fill="none" {LINE} stroke-width="2"/>'
                   f'<ellipse cx="{bx}" cy="{by + 120}" rx="22" ry="10" fill="url(#hatch)" {LINE} stroke-width="2"/>', (bx, by + 126))
    layer("v-latch", "".join(f'<circle cx="{bx + dx}" cy="{by + 78}" r="8" fill="{GOLD}"/>' for dx in (-26, 26)), (bx, by + 78))


def multiprobe_parts():
    """Pioneer Venus Multiprobe (2026-10-17, NSSDCA): the bus, a drum 2.5 m across with solar cells round its side, spun
    about its axis (here lying left to right, its forward end right, seen a little from the right so that end shows as
    an ellipse), an omni antenna and a horn at its aft end; the large probe in the middle of its forward end and three
    small probes 120 degrees apart round it. A probe is a blunt 45-degree cone (the heat shield, forward) on an
    afterbody, a sphere inside; the large one's shield and aft cover come off for its descent (pv-big-*, children of
    the carrier pv-big at its shield's base), the small ones keep theirs. About 140 px to the metre."""
    cx, cy, R = 720, 540, 175
    x0, x1 = 635, 805
    drum = (f'<path d="M{x1} {cy - R} L{x0} {cy - R} A26 {R} 0 0 0 {x0} {cy + R} L{x1} {cy + R}" fill="url(#hatch)" '
            f'{LINE} stroke-width="3"/>'
            f'<ellipse cx="{x1}" cy="{cy}" rx="26" ry="{R}" fill="#ffffff" {LINE} stroke-width="3"/>'
            f'<ellipse cx="{x1 + 2}" cy="{cy}" rx="10" ry="64" fill="#ffffff" {LINE} stroke-width="2"/>'
            f'<path d="M{x0 - 26} {cy} L{x0 - 84} {cy}" fill="none" {LINE} stroke-width="4"/>'
            f'<path d="M{x0 - 84} {cy - 14} L{x0 - 100} {cy} L{x0 - 84} {cy + 14} L{x0 - 68} {cy} Z" fill="#ffffff" {LINE} '
            f'stroke-width="2.4"/>'
            f'<path d="M{x0 - 20} {cy + 70} L{x0 - 62} {cy + 56} L{x0 - 62} {cy + 120} L{x0 - 18} {cy + 104} Z" fill="#ffffff" '
            f'{LINE} stroke-width="2.4"/>')
    layer("pv-bus", drum, (cx, cy))
    # A row of cells across the drum's side, moved round by the timeline to show the spin.
    for i in range(1, 9):
        layer(f"pv-cell-{i}", f'<path d="M{x0 + 6} {cy} L{x1 - 6} {cy}" fill="none" {LINE} stroke-width="2.4"/>', (cx, cy))
    # The latches' flash: the large probe's spring release on the axis, a small probe's clamp at its base.
    layer("pv-latch", "".join(f'<circle cx="812" cy="{cy + dy}" r="8" fill="{GOLD}"/>' for dy in (-60, 60)), (812, cy))
    # The large probe, nose right, its shield's base at (870, 540).
    bx = 870
    layer("pv-big", "", (bx, cy))
    layer("pv-big-shield", f'<path d="M{bx} {cy - 105} L{bx + 70} {cy - 35} Q{bx + 92} {cy} {bx + 70} {cy + 35} L{bx} {cy + 105} Z" '
                           f'fill="url(#hatch)" {LINE} stroke-width="3"/>'
                           f'<path d="M{bx + 6} {cy - 99} L{bx + 6} {cy + 99}" fill="none" {LINE} stroke-width="2"/>', (bx, cy))
    layer("pv-big-aft", f'<path d="M{bx} {cy - 95} C{bx - 38} {cy - 90} {bx - 56} {cy - 40} {bx - 56} {cy} C{bx - 56} {cy + 40} '
                        f'{bx - 38} {cy + 90} {bx} {cy + 95} Z" fill="#ffffff" {LINE} stroke-width="3"/>', (bx, cy))
    # Its pressure vessel: a titanium sphere with a flat ring at its middle, the antenna on its aft end, three
    # towers for the chute's lines, and outside arms (a prism on two, the temperature sensor on the third).
    sx = bx - 6
    sphere = (f'<path d="M{sx - 50} {cy} L{sx - 76} {cy} M{sx - 40} {cy - 30} L{sx - 58} {cy - 44} M{sx - 40} {cy + 30} '
              f'L{sx - 58} {cy + 44} M{sx + 18} {cy - 46} L{sx + 34} {cy - 76} M{sx + 18} {cy + 46} L{sx + 34} {cy + 76} '
              f'M{sx - 10} {cy + 49} L{sx - 18} {cy + 84}" fill="none" {LINE} stroke-width="3"/>'
              f'<circle cx="{sx - 80}" cy="{cy}" r="6" fill="#ffffff" {LINE} stroke-width="2.4"/>'
              f'<rect x="{sx + 28}" y="{cy - 88}" width="14" height="14" fill="#ffffff" {LINE} stroke-width="2.4"/>'
              f'<rect x="{sx + 28}" y="{cy + 74}" width="14" height="14" fill="#ffffff" {LINE} stroke-width="2.4"/>'
              f'<circle cx="{sx}" cy="{cy}" r="50" fill="#ffffff" {LINE} stroke-width="3"/>'
              f'<path d="M{sx - 6} {cy - 50} L{sx - 6} {cy + 50} M{sx + 6} {cy - 50} L{sx + 6} {cy + 50}" fill="none" {LINE} '
              f'stroke-width="2"/>')
    layer("pv-big-sphere", sphere, (bx, cy))
    # The temperature sensor's spark (the anomaly near 12.5 km), at the third arm's tip.
    layer("pv-spark", f'<path d="M{sx - 18} {cy + 84} m-14 0 l28 0 m-14 -14 l0 28 m-10 -10 l20 20 m0 -20 l-20 20" fill="none" '
                      f'stroke="{GOLD}" stroke-width="4" stroke-linecap="round"/>', (sx - 18, cy + 84))
    # A small probe, nose right, its shield's base at (960, 540), for copies 1 to 3 and their copies behind the
    # large probe (b); its pivot is its middle.
    small = (f'<path d="M960 486 C942 490 932 512 932 540 C932 568 942 590 960 594 Z" fill="#ffffff" {LINE} stroke-width="3"/>'
             f'<path d="M960 484 L997 521 Q1009 540 997 559 L960 596 Z" fill="url(#hatch)" {LINE} stroke-width="3"/>'
             f'<path d="M932 540 L920 540" fill="none" {LINE} stroke-width="3"/>')
    for n in ("1", "2", "3", "1b", "2b", "3b"):
        layer(f"pv-small-{n}", small, (968, 540))
    for i in (1, 2, 3):
        layer(f"pv-clamp-{i}", f'<circle cx="934" cy="540" r="7" fill="{GOLD}"/>', (968, 540))
        # The bow of hot air ahead of a probe falling nose down, its pivot at the nose.
        layer(f"pv-glow-{i}", f'<path d="M810 470 C860 610 1060 610 1110 470 C1050 550 870 550 810 470 Z" fill="{GOLD_FILL}" '
                              f'stroke="{GOLD}" stroke-width="2" stroke-linejoin="round"/>'
                              f'<path d="M830 488 C880 590 1040 590 1090 488" fill="none" stroke="{GOLD}" stroke-width="4" '
                              f'stroke-linecap="round"/>', (960, 500))
        layer(f"pv-dust-{i}", puff(930, 872, 18) + puff(970, 866, 24) + puff(1010, 874, 16), (970, 880))
    # Radio rings for the small probes: two each.
    for i in range(1, 7):
        layer(f"pv-ring-{i}", f'<circle cx="960" cy="560" r="40" fill="none" stroke="{GOLD}" stroke-width="4"/>', (960, 560))
    # Venus's ground under the clouds: a flat plain of slabs (as Venera 9 and 10 saw it), in the dim light below.
    slabs = "".join(f'<path d="M{x} {GROUND_Y + 4} L{x + 10} {GROUND_Y - h} L{x + w} {GROUND_Y - h - 3} L{x + w + 8} {GROUND_Y + 4} Z" '
                    f'fill="#ffffff" {LINE} stroke-width="2.4"/>'
                    for x, w, h in ((180, 60, 10), (520, 40, 8), (1260, 70, 12), (1580, 46, 9), (1820, 30, 7)))
    cracks = "".join(f'<path d="M{x} {y} L{x + 70} {y + 6} L{x + 150} {y - 2}" fill="none" {LINE} stroke-width="2" opacity="0.6"/>'
                     for x, y in ((200, 940), (760, 990), (1300, 930), (1600, 1020), (420, 1050)))
    layer("pv-ground", f'<rect x="-900" y="{GROUND_Y}" width="3800" height="900" fill="url(#hatch)"/>'
                       f'<path d="M-900 {GROUND_Y} L2900 {GROUND_Y}" fill="none" {LINE} stroke-width="3"/>{slabs}{cracks}',
          (960, GROUND_Y))


def carrier_entry_parts():
    # The carrier's entry (2026-10-15): Venus's limb under its clouds, as the Earth's (d-earth) with
    # Venus's bands, and its air in two dashed shells; the carrier (v-bus) in three pieces that come
    # apart (its panels, then its body), the bow of hot air under it, and the body glowing.
    cx, cy, r = 960, 3100, 2280
    bands = "".join(f'<circle cx="{cx}" cy="{cy}" r="{r - d}" fill="none" {LINE} stroke-width="2" opacity="0.5"/>'
                    for d in (70, 160, 270, 400))
    layer("b-limb", f'<circle cx="{cx}" cy="{cy}" r="{r}" fill="#ffffff" {LINE} stroke-width="3"/>{bands}'
                    f'<circle cx="{cx}" cy="{cy}" r="{r + 22}" fill="none" {LINE} stroke-width="1.5" stroke-dasharray="6 8" opacity="0.6"/>'
                    f'<circle cx="{cx}" cy="{cy}" r="{r + 60}" fill="none" {LINE} stroke-width="1.5" stroke-dasharray="6 12" opacity="0.4"/>',
          (cx, cy))
    bx, by = 960, 380
    panel = (lambda x0, x1: f'<path d="M{x0} {by - 14} L{x1} {by - 14} L{x1} {by + 14} L{x0} {by + 14} Z" '
                            f'fill="url(#hatch)" {LINE} stroke-width="3"/>')
    layer("b-panel-l", panel(bx - 300, bx - 90), (bx - 195, by))
    layer("b-panel-r", panel(bx + 90, bx + 300), (bx + 195, by))
    body = (f'<rect x="{bx - 70}" y="{by - 60}" width="140" height="120" rx="8" fill="#ffffff" {LINE} stroke-width="3"/>'
            f'<path d="M{bx - 70} {by - 70} L{bx - 90} {by} L{bx - 70} {by + 70} M{bx + 70} {by - 70} L{bx + 90} {by} L{bx + 70} {by + 70}" fill="none" {LINE} stroke-width="2"/>'
            f'<path d="M{bx - 50} {by - 60} Q{bx} {by - 120} {bx + 50} {by - 60} Z" fill="#ffffff" {LINE} stroke-width="3"/>'
            f'<path d="M{bx - 40} {by + 60} L{bx - 30} {by + 76} L{bx + 30} {by + 76} L{bx + 40} {by + 60}" fill="none" {LINE} stroke-width="3"/>')
    layer("b-body", body, (bx, by))
    layer("b-hot", f'<rect x="{bx - 70}" y="{by - 60}" width="140" height="120" rx="8" fill="{GOLD_FILL}" stroke="{GOLD}" stroke-width="3"/>',
          (bx, by))
    # The bow of hot air ahead of it (it falls base first, so under it), as wide as the panels.
    bow = (f'<path d="M{bx - 330} {by + 20} C{bx - 220} {by + 190} {bx + 220} {by + 190} {bx + 330} {by + 20} '
           f'C{bx + 200} {by + 120} {bx - 200} {by + 120} {bx - 330} {by + 20} Z" fill="{GOLD_FILL}" stroke="{GOLD}" '
           f'stroke-width="2" stroke-linejoin="round"/>'
           f'<path d="M{bx - 300} {by + 40} C{bx - 190} {by + 170} {bx + 190} {by + 170} {bx + 300} {by + 40}" fill="none" '
           f'stroke="{GOLD}" stroke-width="4" stroke-linecap="round"/>')
    layer("b-glow", bow, (bx, by + 100))


def viking_parts():
    """The Viking orbiter (2026-10-17, NSSDCA), side on, about 64 px to the metre: an octagonal body 2.5 m across and
    0.46 m high, its two propellant tanks and the main engine above it, four wings of solar panels (the two seen
    across span 9.75 m; the other two, end on, are short stubs), the 1.5 m high-gain dish on a boom from one edge of its
    base. The lander capsule (planet_entry_parts()'s e-shell, its backshell up) hangs under the body."""
    x, y = 960, 300
    wings = (f'<path d="M{x - 312} {y - 10} L{x - 84} {y - 10} L{x - 84} {y + 10} L{x - 312} {y + 10} Z M{x + 84} {y - 10} '
             f'L{x + 312} {y - 10} L{x + 312} {y + 10} L{x + 84} {y + 10} Z" fill="url(#hatch)" {LINE} stroke-width="3"/>'
             f'<path d="M{x - 198} {y - 10} L{x - 198} {y + 10} M{x + 198} {y - 10} L{x + 198} {y + 10}" fill="none" {LINE} '
             f'stroke-width="2"/>')
    stubs = (f'<path d="M{x - 40} {y - 15} L{x - 64} {y - 46} M{x + 40} {y - 15} L{x + 64} {y - 46}" fill="none" {LINE} '
             f'stroke-width="3"/>'
             f'<rect x="{x - 80}" y="{y - 58}" width="26" height="14" fill="url(#hatch)" {LINE} stroke-width="2.4"/>'
             f'<rect x="{x + 54}" y="{y - 58}" width="26" height="14" fill="url(#hatch)" {LINE} stroke-width="2.4"/>')
    tanks = (f'<rect x="{x - 56}" y="{y - 78}" width="48" height="64" rx="22" fill="#ffffff" {LINE} stroke-width="3"/>'
             f'<rect x="{x + 8}" y="{y - 78}" width="48" height="64" rx="22" fill="#ffffff" {LINE} stroke-width="3"/>'
             f'<path d="M{x - 12} {y - 92} L{x + 12} {y - 92} L{x + 20} {y - 124} L{x - 20} {y - 124} Z" fill="#ffffff" {LINE} '
             f'stroke-width="3"/><path d="M{x} {y - 78} L{x} {y - 92}" fill="none" {LINE} stroke-width="3"/>')
    body = (f'<path d="M{x - 80} {y - 8} L{x - 64} {y - 15} L{x + 64} {y - 15} L{x + 80} {y - 8} L{x + 80} {y + 8} L{x + 64} {y + 15} '
            f'L{x - 64} {y + 15} L{x - 80} {y + 8} Z" fill="#ffffff" {LINE} stroke-width="3"/>'
            f'<path d="M{x - 64} {y - 15} L{x - 64} {y + 15} M{x + 64} {y - 15} L{x + 64} {y + 15}" fill="none" {LINE} stroke-width="2"/>')
    dish = (f'<path d="M{x + 80} {y + 8} L{x + 150} {y + 40}" fill="none" {LINE} stroke-width="3"/>'
            f'<path d="M{x + 132} {y + 100} Q{x + 176} {y + 68} {x + 208} {y + 24} Q{x + 178} {y + 62} {x + 132} {y + 100} Z" '
            f'fill="#ffffff" {LINE} stroke-width="3"/>'
            f'<path d="M{x + 132} {y + 100} Q{x + 156} {y + 56} {x + 208} {y + 24}" fill="none" {LINE} stroke-width="3"/>')
    layer("vk-orbiter", wings + stubs + tanks + body + dish, (x, y))
    # The latches under the body, and the capsule's deorbit jets: two of its four clusters, on the backshell's
    # shoulders, firing out along its axis (a child of e-shell, so they turn with it).
    layer("vk-latch", "".join(f'<circle cx="{x + dx}" cy="{y + 18}" r="8" fill="{GOLD}"/>' for dx in (-30, 30)), (x, y + 18))
    jet = lambda jx: (f'<path d="M{jx - 7} 400 C{jx - 6} 380 {jx - 3} 362 {jx} 344 C{jx + 3} 362 {jx + 6} 380 {jx + 7} 400 Z" '
                      f'fill="{GOLD_FILL}" stroke="{GOLD}" stroke-width="2.4" stroke-linejoin="round"/>')
    layer("vk-jets", jet(930) + jet(990), (960, 400))


def prop_m_parts():
    """PrOP-M (2026-10-17), the little rover Mars 2 and Mars 3 carried: a flat box with a bump in the middle on two wide
    skis (one seen), two short bars out of its front that a rock would push in, and its tether. Drawn on the ground
    (y 900) with its pivot under its middle, a few times larger than it was (215 mm) so it reads beside the lander."""
    x, y = 960, 900
    body = (f'<rect x="{x - 24}" y="{y - 24}" width="48" height="14" rx="3" fill="#ffffff" {LINE} stroke-width="3"/>'
            f'<path d="M{x - 9} {y - 24} Q{x} {y - 36} {x + 9} {y - 24}" fill="url(#hatch)" {LINE} stroke-width="2.4"/>'
            f'<path d="M{x + 24} {y - 16} L{x + 38} {y - 10} M{x + 24} {y - 12} L{x + 36} {y - 4}" fill="none" {LINE} '
            f'stroke-width="2.4"/>')
    layer("pr-rover", body, (x, y))
    # The ski: a flat runner, its front turned up, hinged under the body (the timeline lifts and slides it).
    layer("pr-ski", f'<path d="M{x - 30} {y - 2} L{x + 22} {y - 2} Q{x + 30} {y - 2} {x + 32} {y - 9}" fill="none" {LINE} '
                    f'stroke-width="3.5"/><path d="M{x - 12} {y - 10} L{x - 12} {y - 2} M{x + 10} {y - 10} L{x + 10} {y - 2}" '
                    f'fill="none" {LINE} stroke-width="2.4"/>', (x, y))
    # The tether, 100 px along +x from its pivot (the timeline aims and stretches it).
    layer("pr-cable", f'<path d="M{x} {y} L{x + 100} {y}" fill="none" {LINE} stroke-width="2"/>', (x, y))
    # The bars' flash when they meet a rock.
    layer("pr-bump", f'<circle cx="{x + 40}" cy="{y - 8}" r="9" fill="none" stroke="{GOLD}" stroke-width="3.5"/>', (x + 40, y - 8))


def x15_parts():
    """The X-15's own bits (2026-10-17), on w-plane's drawing (nose right at 1196 820, tail at 760), children of it: the
    lower ventral fin under the tail (dropped on a small chute before landing, or torn off at speed), the nose's
    attitude jets (puffs up and down, for steering where the air is too thin for its fins), the shock cone off the
    nose, the nose and leading edges glowing hot, and a hot spot on the belly."""
    layer("x15-ventral", f'<path d="M762 840 L800 840 L816 878 L768 878 Z" fill="#ffffff" {LINE} stroke-width="3"/>', (788, 858))
    jet = lambda y0, y1: (f'<path d="M1146 {y0} C1144 {(2 * y0 + y1) / 3:.0f} 1148 {(y0 + 2 * y1) / 3:.0f} 1152 {y1} '
                          f'C1156 {(y0 + 2 * y1) / 3:.0f} 1160 {(2 * y0 + y1) / 3:.0f} 1158 {y0} Z" fill="{GOLD_FILL}" '
                          f'stroke="{GOLD}" stroke-width="2.4" stroke-linejoin="round"/>')
    layer("x15-rcs-up", jet(804, 770), (1152, 804))
    layer("x15-rcs-down", jet(836, 870), (1152, 836))
    layer("x15-shock", f'<path d="M1196 820 L900 690 M1196 820 L900 950" fill="none" {LINE} stroke-width="2.4" '
                       f'stroke-dasharray="14 10" opacity="0.6"/>', (1196, 820))
    layer("x15-heat", f'<path d="M1100 800 Q1172 812 1196 820 Q1172 828 1100 840 Z" fill="{GOLD_FILL}" stroke="{GOLD}" '
                      f'stroke-width="3"/><path d="M880 830 L998 822 L1018 828" fill="none" stroke="{GOLD}" stroke-width="4"/>',
          (1150, 820))
    layer("x15-hot", f'<circle cx="980" cy="840" r="16" fill="{GOLD_FILL}" stroke="{GOLD}" stroke-width="3"/>', (980, 840))


def planet_entry_parts():
    # A lander's entry at another planet (2026-10-15): Mars's limb (Venus's is b-limb), hatched as
    # p-mars, with one thin shell of air; the Mars landers' aeroshell (Viking's and Mars 3's blunt cone,
    # heat shield down; Venera's is a sphere, v-pod); and a bow of hot air the lander's width.
    cx, cy, r = 960, 3100, 2280
    layer("e-limb-mars", f'<circle cx="{cx}" cy="{cy}" r="{r}" fill="url(#hatch)" {LINE} stroke-width="3"/>'
                         f'<circle cx="{cx}" cy="{cy}" r="{r + 22}" fill="none" {LINE} stroke-width="1.5" stroke-dasharray="6 8" opacity="0.6"/>',
          (cx, cy))
    x, y = 960, 440
    layer("e-shell", f'<path d="M{x - 100} {y + 20} L{x - 34} {y - 40} L{x + 34} {y - 40} L{x + 100} {y + 20} Z" fill="#ffffff" {LINE} stroke-width="3"/>'
                     f'<path d="M{x - 108} {y + 20} Q{x} {y + 66} {x + 108} {y + 20} Z" fill="url(#hatch)" {LINE} stroke-width="3"/>',
          (x, y))
    bow = (f'<path d="M{x - 150} {y + 30} C{x - 100} {y + 170} {x + 100} {y + 170} {x + 150} {y + 30} '
           f'C{x + 90} {y + 110} {x - 90} {y + 110} {x - 150} {y + 30} Z" fill="{GOLD_FILL}" stroke="{GOLD}" '
           f'stroke-width="2" stroke-linejoin="round"/>'
           f'<path d="M{x - 130} {y + 50} C{x - 80} {y + 150} {x + 80} {y + 150} {x + 130} {y + 50}" fill="none" '
           f'stroke="{GOLD}" stroke-width="4" stroke-linecap="round"/>')
    layer("e-glow", bow, (x, y))


def planet_descent_parts():
    # A lander's descent at another planet (2026-10-15). Venus: its cloud deck as long wavy bands to fall
    # through, and a rip across the canopy (a gold tear). Mars: the aeroshell in its pieces, Viking's
    # way down (the backshell the chute hangs from, the heat shield, the lander inside with its legs
    # folded, its three descent engines' flames), and the ground, hatched as Mars and cratered.
    for i in range(1, 7):
        wave = " ".join(f"Q{x + 60} {(-12 if (x // 120) % 2 else 12)} {x + 120} 0" for x in range(-400, 2320, 120))
        layer(f"y-band-{i}", f'<g transform="translate(0 600)"><path d="M-400 0 {wave} L2320 34 L-400 34 Z" '
                             f'fill="url(#earth)" stroke="none"/><path d="M-400 0 {wave}" fill="none" {LINE} '
                             f'stroke-width="2.4" opacity="0.6"/></g>', (960, 600))
    # On the main chute's canopy (r-main at rest: the canopy is from y -35 to 112).
    layer("y-rip", f'<path d="M880 6 L904 30 L894 46 L922 66 L912 84 L940 102" fill="none" stroke="{GOLD}" '
                   f'stroke-width="6" stroke-linecap="round" stroke-linejoin="round"/>', (910, 54))
    x, y = 960, 440
    layer("m-back", f'<path d="M{x - 100} {y + 20} L{x - 34} {y - 40} L{x + 34} {y - 40} L{x + 100} {y + 20} Z" '
                    f'fill="#ffffff" {LINE} stroke-width="3"/>', (x, y - 40))
    layer("m-heat", f'<path d="M{x - 108} {y + 20} Q{x} {y + 66} {x + 108} {y + 20} Z" fill="url(#hatch)" {LINE} stroke-width="3"/>',
          (x, y + 32))
    # The lander: a six-sided body (seen side on, a flat box with bevelled ends), a dish on a short mast.
    layer("m-lander", f'<path d="M{x - 80} {y - 6} L{x - 64} {y - 24} L{x + 64} {y - 24} L{x + 80} {y - 6} L{x + 80} {y + 12} '
                      f'L{x - 80} {y + 12} Z" fill="#ffffff" {LINE} stroke-width="3"/>'
                      f'<path d="M{x + 30} {y - 24} L{x + 30} {y - 50}" fill="none" {LINE} stroke-width="3"/>'
                      f'<path d="M{x + 10} {y - 58} Q{x + 30} {y - 40} {x + 50} {y - 58}" fill="#ffffff" {LINE} stroke-width="3"/>',
          (x, y))
    # A leg each side, drawn out (down and out to its foot pad), pivoting at the body's lower corner.
    layer("m-leg-l", f'<path d="M{x - 74} {y + 10} L{x - 116} {y + 64}" fill="none" {LINE} stroke-width="4"/>'
                     f'<ellipse cx="{x - 120}" cy="{y + 68}" rx="16" ry="5" fill="#ffffff" {LINE} stroke-width="3"/>', (x - 74, y + 10))
    layer("m-leg-r", f'<path d="M{x + 74} {y + 10} L{x + 116} {y + 64}" fill="none" {LINE} stroke-width="4"/>'
                     f'<ellipse cx="{x + 120}" cy="{y + 68}" rx="16" ry="5" fill="#ffffff" {LINE} stroke-width="3"/>', (x + 74, y + 10))
    for i, fx in enumerate((x - 44, x, x + 44), 1):
        layer(f"m-flame-{i}", f'<path d="M{fx - 8} {y + 14} C{fx - 9} {y + 40} {fx - 4} {y + 66} {fx} {y + 90} '
                              f'C{fx + 4} {y + 66} {fx + 9} {y + 40} {fx + 8} {y + 14} Z" fill="{GOLD_FILL}" stroke="{GOLD}" '
                              f'stroke-width="2" stroke-linejoin="round"/>', (fx, y + 14))
    craters = "".join(f'<ellipse cx="{cx}" cy="{cy}" rx="{rx}" ry="{rx * 0.22:.1f}" fill="#ffffff" {LINE} stroke-width="2.4"/>'
                      for cx, cy, rx in ((-300, 1180, 90), (420, 1260, 60), (980, 1160, 120), (1500, 1300, 80), (2100, 1190, 70),
                                         (200, 1420, 140), (1300, 1500, 110)))
    layer("m-ground", f'<rect x="-900" y="1100" width="3800" height="900" fill="url(#hatch)"/>'
                      f'<path d="M-900 1100 ' + " ".join(f"Q{gx + 160} {1100 - (18 if (gx // 320) % 2 else 8)} {gx + 320} 1100"
                                                        for gx in range(-900, 2900, 320)) + f'" fill="none" {LINE} stroke-width="3"/>'
                      + craters, (960, 1100))
    # The landing (mars-landing): stones on m-ground's line, clear of where the feet come down (x 840 and
    # 1080), and the rock one foot lands on (Viking 2's), under the right foot.
    stones = "".join(f'<path d="M{sx - w} 1102 L{sx - w * 0.6} {1102 - h} L{sx + w * 0.4} {1100 - h * 1.1} L{sx + w} 1102 Z" '
                     f'fill="#ffffff" {LINE} stroke-width="2.4"/>'
                     for sx, w, h in ((180, 26, 18), (420, 14, 10), (640, 20, 14), (1290, 16, 12), (1500, 30, 20), (1760, 18, 12)))
    layer("m-stones", stones, (960, 1100))
    layer("m-rock", f'<path d="M1056 1104 L1066 1080 L1090 1072 L1104 1084 L1108 1104 Z" fill="#ffffff" {LINE} stroke-width="2.4"/>',
          (1080, 1100))


# --- The air launch: a carrier plane with the rocket plane under its wing --------------------------------

# The carrier, nose right, standing on the runway (y 772), its pivot at its main wheels (AC). The rocket
# plane (w-plane at 0.6) hangs on a pylon under the wing, its body's middle at (940 690).
AC = (700, 772)


def air_parts():
    grey = "rgba(22,48,92,0.22)"
    body = (f'<path d="M330 380 L400 380 L470 525 L330 525 Z" fill="#ffffff" {LINE} stroke-width="3"/>'
            f'<path d="M300 525 L1420 525 Q1500 530 1520 560 Q1500 590 1420 595 L300 595 Q280 560 300 525 Z" fill="#ffffff" {LINE} stroke-width="3"/>'
            + "".join(f'<rect x="{x}" y="538" width="16" height="12" rx="2" fill="#ffffff" {LINE} stroke-width="2"/>' for x in (1420, 1444, 1468))
            + f'<path d="M640 586 L1080 600 L1100 612 L660 600 Z" fill="url(#hatch)" {LINE} stroke-width="2.4"/>'
            + "".join(f'<path d="M{x} 604 L{x} 624" fill="none" {LINE} stroke-width="3"/>'
                      f'<ellipse cx="{x}" cy="636" rx="44" ry="14" fill="#ffffff" {LINE} stroke-width="3"/>' for x in (760, 1000))
            + f'<path d="M940 604 L940 664" fill="none" {LINE} stroke-width="4"/>')
    layer("a-carrier", body, AC)
    # Its landing gear, its own part so it can be put away once it's flying.
    layer("a-gear", "".join(f'<path d="M{x} 595 L{x} 754" fill="none" {LINE} stroke-width="4"/>'
                            f'<circle cx="{x - 12}" cy="760" r="12" fill="#ffffff" {LINE} stroke-width="3"/>'
                            f'<circle cx="{x + 12}" cy="760" r="12" fill="#ffffff" {LINE} stroke-width="3"/>' for x in (700, 1220)), AC)
    # A long runway for the takeoff (the camera rides along): flat to the horizon, its marks, low hills.
    g = AC[1]
    hills = " ".join(f"Q{x + 90} {g - (10 + (x * 7) % 23)} {x + 180} {g}" for x in range(-2000, 9000, 180))
    layer("a-ground", f'<path d="M-2000 {g} {hills}" fill="none" {LINE} stroke-width="2" opacity="0.5"/>'
                      f'<rect x="-2000" y="{g}" width="11000" height="500" fill="url(#earth)"/>'
                      f'<path d="M-2000 {g} L9000 {g}" fill="none" {LINE} stroke-width="3"/>'
                      + "".join(f'<path d="M{x} {g + 32} L{x + 90} {g + 32}" fill="none" {LINE} stroke-width="4" opacity="0.6"/>'
                                for x in range(-2000, 9000, 220)), (960, g))
    # Smoke from the inboard engine, and the speed lines of the air going by.
    layer("a-smoke", "".join(f'<circle cx="{x}" cy="{y}" r="{r}" fill="{grey}" {LINE} stroke-width="2"/>'
                             for x, y, r in ((700, 636, 18), (670, 628, 14), (640, 640, 12))), (716, 636))
    for i in range(1, 9):
        layer(f"a-flow-{i}", f'<path d="M960 540 L1080 540" fill="none" {LINE} stroke-width="2.4" opacity="0.5"/>', (960, 540))
    # The rocket plane's engine flame, back from its nozzle (in the rocket plane's own drawing).
    layer("w-flame", f'<path d="M748 808 C700 812 650 816 600 820 C650 824 700 828 748 832 Z" fill="{GOLD_FILL}" '
                     f'stroke="{GOLD}" stroke-width="3" stroke-linejoin="round"/>', (748, 820))


def e154_parts():
    """The OKB-MiG E-154 (2026-10-17), the Soviet X-15, from the user's own four views: a needle nose with a long probe, the
    canopy well forward, a tall swept fin, swept wings set low, a low tailplane, a strake under its tail and four nozzles
    in a cluster (two seen). It lands on wheels. Drawn as w-plane is, nose right, its pivot WS (830 880) on the ground
    line, so the air-launch timelines place it the same way. And its carrier, a Soviet bomber with four turboprops
    (two seen, their propellers as discs), the E-154 slung under its belly behind the wing; the same pivot as a-carrier."""
    body = (f'<path d="M760 806 L1040 802 Q1130 806 1190 816 L1214 820 L1190 824 Q1130 834 1040 838 L760 836 Z" '
            f'fill="#ffffff" {LINE} stroke-width="3"/>'
            f'<path d="M1214 820 L1240 820" fill="none" {LINE} stroke-width="2.4"/>'
            f'<path d="M1046 803 Q1074 784 1114 807" fill="#ffffff" {LINE} stroke-width="3"/>'
            f'<path d="M880 804 L842 740 L814 740 L792 806 Z" fill="#ffffff" {LINE} stroke-width="3"/>'
            f'<path d="M896 830 L1004 824 L1016 830 L884 838 Z" fill="url(#hatch)" {LINE} stroke-width="2.4"/>'
            f'<path d="M772 830 L824 828 L830 834 L766 836 Z" fill="url(#hatch)" {LINE} stroke-width="2.4"/>'
            f'<ellipse cx="757" cy="813" rx="5" ry="7" fill="#ffffff" {LINE} stroke-width="2.4"/>'
            f'<ellipse cx="757" cy="829" rx="5" ry="7" fill="#ffffff" {LINE} stroke-width="2.4"/>'
            f'<path d="M1150 806 L1150 834" fill="none" {LINE} stroke-width="1.6" opacity="0.6"/>')
    layer("e154-plane", body, WS)
    # The strake under its tail: the piece that tears away at speed (its own part, as x15-ventral).
    layer("e154-strake", f'<path d="M800 836 L852 838 L842 858 L808 854 Z" fill="#ffffff" {LINE} stroke-width="3"/>', (826, 847))
    # Its wheels, each leg hinged under the body where it meets it, so each folds up about its own hinge; it lands on them.
    layer("e154-gear", f'<path d="M930 838 L930 868" fill="none" {LINE} stroke-width="4"/>'
                       f'<circle cx="930" cy="872" r="8" fill="#ffffff" {LINE} stroke-width="3"/>', (930, 838))
    layer("e154-nose", f'<path d="M1120 834 L1120 868" fill="none" {LINE} stroke-width="4"/>'
                       f'<circle cx="1120" cy="873" r="7" fill="#ffffff" {LINE} stroke-width="3"/>', (1120, 834))
    # Four nozzles' flames, back from the tail (the two seen), pivoting where w-flame does.
    flame = "".join(f'<path d="M752 {y - 8} C704 {y - 5} 654 {y - 2} 604 {y} C654 {y + 2} 704 {y + 5} 752 {y + 8} Z" '
                    f'fill="{GOLD_FILL}" stroke="{GOLD}" stroke-width="3" stroke-linejoin="round"/>' for y in (813, 829))
    layer("e154-flame", flame, (748, 820))
    jet = lambda y0, y1: (f'<path d="M1146 {y0} C1144 {(2 * y0 + y1) / 3:.0f} 1148 {(y0 + 2 * y1) / 3:.0f} 1152 {y1} '
                          f'C1156 {(y0 + 2 * y1) / 3:.0f} 1160 {(2 * y0 + y1) / 3:.0f} 1158 {y0} Z" fill="{GOLD_FILL}" '
                          f'stroke="{GOLD}" stroke-width="2.4" stroke-linejoin="round"/>')
    layer("e154-rcs-up", jet(808, 774), (1152, 808))
    layer("e154-rcs-down", jet(832, 866), (1152, 832))
    layer("e154-shock", f'<path d="M1214 820 L918 690 M1214 820 L918 950" fill="none" {LINE} stroke-width="2.4" '
                        f'stroke-dasharray="14 10" opacity="0.6"/>', (1214, 820))
    layer("e154-heat", f'<path d="M1130 806 Q1190 814 1214 820 Q1190 826 1130 834 Z" fill="{GOLD_FILL}" stroke="{GOLD}" '
                       f'stroke-width="3"/><path d="M896 830 L1004 824 L1016 830" fill="none" stroke="{GOLD}" stroke-width="4"/>',
          (1170, 820))
    layer("e154-hot", f'<circle cx="980" cy="838" r="16" fill="{GOLD_FILL}" stroke="{GOLD}" stroke-width="3"/>', (980, 838))

    # The carrier, a Soviet four-turboprop bomber (fictional, as the E-154 is), standing on its main wheels at AC.
    disc = lambda x, y, r: (f'<ellipse cx="{x}" cy="{y}" rx="7" ry="{r}" fill="rgba(22,48,92,0.08)" {LINE} stroke-width="2"/>'
                            f'<path d="M{x - 6} {y - 5} L{x + 12} {y} L{x - 6} {y + 5} Z" fill="#ffffff" {LINE} stroke-width="2"/>')
    carrier = (f'<path d="M500 525 L420 392 L370 392 L410 525 Z" fill="#ffffff" {LINE} stroke-width="3"/>'
               f'<path d="M300 540 Q290 528 330 525 L1400 525 Q1470 530 1500 556 Q1470 588 1400 592 L330 592 Q290 588 300 540 Z" '
               f'fill="#ffffff" {LINE} stroke-width="3"/>'
               f'<path d="M1430 540 Q1470 546 1488 558 M1440 572 Q1468 566 1488 558" fill="none" {LINE} stroke-width="2"/>'
               + "".join(f'<rect x="{x}" y="534" width="14" height="10" rx="2" fill="#ffffff" {LINE} stroke-width="2"/>'
                         for x in (1360, 1382))
               + f'<path d="M332 566 L430 562 L442 574 L320 576 Z" fill="url(#hatch)" {LINE} stroke-width="2.4"/>'
               f'<path d="M780 572 L1080 580 L1100 592 L800 594 Z" fill="url(#hatch)" {LINE} stroke-width="2.4"/>'
               f'<rect x="900" y="590" width="108" height="24" rx="10" fill="#ffffff" {LINE} stroke-width="3"/>'
               f'<rect x="1040" y="584" width="92" height="20" rx="9" fill="#ffffff" {LINE} stroke-width="3"/>'
               + disc(1014, 602, 34) + disc(1138, 594, 30)
               + f'<path d="M862 592 L862 636" fill="none" {LINE} stroke-width="4"/>')
    layer("su-carrier", carrier, AC)
    layer("su-gear", "".join(f'<path d="M{x} {y0} L{x} 754" fill="none" {LINE} stroke-width="4"/>'
                             f'<circle cx="{x - 12}" cy="760" r="12" fill="#ffffff" {LINE} stroke-width="3"/>'
                             f'<circle cx="{x + 12}" cy="760" r="12" fill="#ffffff" {LINE} stroke-width="3"/>'
                             for x, y0 in ((1086, 604), (1420, 592))), AC)
    grey = "rgba(22,48,92,0.22)"
    layer("su-smoke", "".join(f'<circle cx="{x}" cy="{y}" r="{r}" fill="{grey}" {LINE} stroke-width="2"/>'
                    for x, y, r in ((890, 604, 18), (860, 596, 14), (830, 608, 12))), (906, 604))


def departure_parts():
    # Earth departure (2026-10-16): the payload alone after the injection burn, close up and nose right,
    # coming alive. Drawn GK times o-sat's size, its centre on G; the boom, the dish and the lamps hang
    # off it (pivots in place), the boom drawn upright and the dish's mast hanging down.
    gx, gy = G
    sat = re.sub(r'stroke-width="[\d.]+"', 'stroke-width="3"', board_group("debris-1"))
    layer("g-probe", f'<g transform="translate({gx} {gy}) scale({GK}) rotate(90) translate(-960 -426)">{sat}</g>', G)
    bx, by = gx - 95, gy - 74
    layer("g-boom", f'<path d="M{bx} {by} L{bx} {by - 190}" fill="none" {LINE} stroke-width="4"/>'
                    f'<rect x="{bx - 9}" y="{by - 212}" width="18" height="24" rx="3" fill="#ffffff" {LINE} stroke-width="3"/>',
          (bx, by))
    dx, dy = gx - 95, gy + 74
    layer("g-dish", f'<path d="M{dx} {dy} L{dx} {dy + 62}" fill="none" {LINE} stroke-width="4"/>'
                    f'<path d="M{dx - 34} {dy + 58} Q{dx} {dy + 92} {dx + 34} {dy + 58} Z" fill="#ffffff" {LINE} stroke-width="3"/>'
                    f'<path d="M{dx} {dy + 74} L{dx} {dy + 94}" fill="none" {LINE} stroke-width="2"/>',
          (dx, dy))
    # The instruments' lamps, on when they're switched on.
    layer("g-lamps", "".join(f'<circle cx="{gx + x}" cy="{gy + y}" r="7" fill="{GOLD}" {LINE} stroke-width="2"/>'
                             for x, y in ((20, -30), (50, -30), (80, -30))), G)
    # Spin-up puffs, fore and aft on opposite sides.
    layer("g-puff-1", puff(gx + 60, gy - 92, 12), (gx + 60, gy - 92))
    layer("g-puff-2", puff(gx - 60, gy + 92, 12), (gx - 60, gy + 92))
    # The tracking pulse, a wavefront ")" centred on the pivot, turned to where it goes (out and back).
    arcs = lambda k: "".join(f'<path d="M{960 + k * (dx - 12)} {560 - h} Q{960 + k * (dx + 16)} 560 {960 + k * (dx - 12)} {560 + h}" '
                             f'fill="none" stroke="{GOLD}" stroke-width="4" stroke-linecap="round"/>' for h, dx in ((22, 0), (40, 18)))
    layer("g-pulse-out", arcs(1), (960, 560))
    layer("g-pulse-back", arcs(1), (960, 560))


# Where earth departure's probe sits, and its size against o-sat's drawing.
G, GK = (1120, 500), 1.7


def copy_board_layers():
    src = os.path.join(BOARD, "build")
    if not os.path.exists(os.path.join(src, "layers.json")):
        subprocess.run([sys.executable, os.path.join(BOARD, "extract.py")], check=True)
    board = json.load(open(os.path.join(src, "layers.json")))
    for name, info in board.items():
        shutil.copy(os.path.join(src, "layers", name + ".png"), os.path.join(OUT, name + ".png"))
        layers[name] = info


# --- Project Mercury (2026-10-17): its capsule for the steps it flies ---------------------------------
# Drawn as the storyboard's capsule (debris-1) is, nose up, centred on x 960, its base at y 500 and 90
# wide, its top at 352, so everything placed against that one (RT, RU, the chutes' top, the straps'
# hinges, the break-up's seam) fits it too. A step's "skin" (build.py) draws mc-<part> for <part>.
# From the photographs: a blunt heat shield, bulging a little; a cone, shingled, with the window up
# near its top; the recovery compartment, a short cylinder; the antenna canister on it, narrower; the
# retropack strapped to the shield's middle (three straps; two seen side on), three motors.


def mercury_body(k=1.0):
    """The capsule's outline, its stroke widths times k."""
    w = lambda x: f'stroke-width="{x * k:.2f}"'
    shield = f'<path d="M915 500 Q960 515 1005 500 Z" fill="#ffffff" {LINE} {w(3)}/>'
    cone = f'<path d="M915 500 L941 418 L979 418 L1005 500 Z" fill="#ffffff" {LINE} {w(3)}/>'
    # The shingles: lines along the cone, clear of the window.
    shingles = "".join(f'<path d="M{915 + 90 * f:.1f} 500 L{941 + 38 * f:.1f} 418" fill="none" {LINE} {w(1.6)} opacity="0.6"/>'
                       for f in (0.17, 0.83))
    window = f'<path d="M952 432 L968 432 L971 452 L949 452 Z" fill="rgba(22,48,92,0.10)" {LINE} {w(2)}/>'
    neck = f'<rect x="941" y="388" width="38" height="30" fill="#ffffff" {LINE} {w(3)}/>'
    canister = (f'<rect x="946" y="360" width="28" height="28" fill="#ffffff" {LINE} {w(3)}/>'
                f'<rect x="950" y="352" width="20" height="8" fill="#ffffff" {LINE} {w(2.4)}/>'
                f'<path d="M946 372 L974 372" fill="none" {LINE} {w(1.6)} opacity="0.6"/>')
    return shield + cone + shingles + window + neck + canister


def mercury_tower(k=1.0):
    """The escape tower on the canister's top: a truss, its motor and the nose above (Redstone and
    Atlas alike)."""
    w = lambda x: f'stroke-width="{x * k:.2f}"'
    truss = (f'<path d="M948 352 L954 290 L966 290 L972 352" fill="none" {LINE} {w(2.4)}/>'
             + "".join(f'<path d="M{948 + 6 * (352 - y) / 62:.1f} {y} L{972 - 6 * (352 - y2) / 62:.1f} {y2}" fill="none" {LINE} {w(1.6)}/>'
                       for y, y2 in ((352, 331), (331, 311), (311, 290)))
             + "".join(f'<path d="M{972 - 6 * (352 - y) / 62:.1f} {y} L{948 + 6 * (352 - y2) / 62:.1f} {y2}" fill="none" {LINE} {w(1.6)}/>'
                       for y, y2 in ((331, 311),)))
    motor = (f'<rect x="952" y="248" width="16" height="42" fill="#ffffff" {LINE} {w(2.4)}/>'
             f'<path d="M952 248 L960 232 L968 248 Z" fill="#ffffff" {LINE} {w(2.4)}/>'
             f'<path d="M952 290 L948 296 M968 290 L972 296" fill="none" {LINE} {w(2)}/>')
    return truss + motor


def mercury_pack(k=1.0):
    w = lambda x: f'stroke-width="{x * k:.2f}"'
    return (f'<rect x="940" y="506" width="40" height="14" rx="3" fill="#ffffff" {LINE} {w(2.4)}/>'
            + "".join(f'<path d="M{x - 4} 520 L{x + 4} 520 L{x + 6} 528 L{x - 6} 528 Z" fill="#ffffff" {LINE} {w(2)}/>'
                      for x in (948, 960, 972)))


def mercury_parts():
    # The parts no other craft draws, empty for them: the escape tower (the ascent jettisons it) and the
    # landing bag (the descent lets the shield down on it).
    layer("a-les", "", (960, 300))
    layer("r-bag", "", RU_AT(960, 506))
    small = lambda body: re.sub(r'stroke-width="([\d.]+)"', lambda m: f'stroke-width="{float(m.group(1)) * 0.8:.2f}"', body)
    cap = small(mercury_body())
    # On the pad and in the launch's break-up, the tower on; on the way up, the tower is its own part.
    rocket = board_group("rocket")
    nose = rocket.index('<path xmlns="http://www.w3.org/2000/svg" d="M915 440')
    layer("mc-rocket", rocket[:nose] + mercury_body() + mercury_tower(), (960, 600))
    layer("mc-debris-1", mercury_body() + mercury_tower(), (960, 420))
    layer("mc-debris-1-bare", mercury_body(), (960, 420))
    layer("mc-a-les", mercury_tower(), (960, 300))
    # Deployment: the capsule with its pack on, as d-payload (turned, 1.5 times the launch).
    big = f"translate({DX} {DY}) scale({DK}) translate(-850 -520)"
    turn = "translate(-120 20) rotate(90 960 500)"
    layer("mc-d-payload", f'<g transform="{big}"><g transform="{turn}">{mercury_body()}{mercury_pack()}</g></g>', D_AT(914, 520))
    # In orbit, small, nose up, thick lines (as o-sat), and the second craft of a joint flight.
    thick = re.sub(r'stroke-width="[\d.]+"', 'stroke-width="5"', mercury_body() + mercury_pack())
    for name in ("mc-o-sat", "mc-o-sat-b"):
        layer(name, f'<g transform="translate(960 560) scale(0.6) translate(-960 -426)">{thick}</g>', (960, 560))
    # Coming home: the capsule nose right (RT) and nose up (RU).
    layer("mc-r-capsule", RT(cap), RC)
    layer("mc-r-capsule-up", RU(cap), RC)
    # The pack, its straps from its corners to the shield's rim, hinged there; the pack turns about
    # strap b's hinge, as the generic one does.
    layer("mc-r-pack", RT(small(mercury_pack())), RT_AT(1003, 501))
    layer("mc-r-strap-a", RT(f'<path d="M942 508 L917 501" fill="none" {LINE} stroke-width="2"/>'), RT_AT(917, 501))
    layer("mc-r-strap-b", RT(f'<path d="M978 508 L1003 501" fill="none" {LINE} stroke-width="2"/>'), RT_AT(1003, 501))
    layer("mc-r-cut-a", RT(f'<circle cx="917" cy="502" r="6" fill="{GOLD}"/>'), RT_AT(917, 502))
    layer("mc-r-cut-b", RT(f'<circle cx="1003" cy="502" r="6" fill="{GOLD}"/>'), RT_AT(1003, 502))
    for i, x in enumerate((948, 960, 972), 1):
        layer(f"mc-r-flame-{i}", RT(f'<path d="M{x - 5} 529 C{x - 6} 547 {x - 3} 565 {x} 583 C{x + 3} 565 {x + 6} 547 {x + 5} 529 Z" '
                                    f'fill="{GOLD_FILL}" stroke="{GOLD}" stroke-width="2" stroke-linejoin="round"/>'), RT_AT(x, 529))
    # The attitude jets, on the recovery compartment.
    layer("mc-r-jet-l", RT(puff(927, 400, 9)), RT_AT(936, 400))
    layer("mc-r-jet-r", RT(puff(993, 400, 9)), RT_AT(984, 400))
    layer("mc-r-jet-stuck", RT(f'<path d="M980 398 C1010 390 1044 380 1080 372 L1084 388 C1048 394 1012 406 982 414 Z" '
                               f'fill="{GOLD_FILL}" stroke="{GOLD}" stroke-width="2" stroke-linejoin="round"/>'), RC)
    # The periscope: out from the cone's side near the shield, its head turned forward; it slides in
    # along its own length (reentry-trim.jsx), its pivot at its root.
    layer("mc-r-periscope", RT(f'<path d="M921 480 L903 480 L903 470" fill="none" {LINE} stroke-width="3.2"/>'
                               f'<rect x="899" y="462" width="8" height="9" fill="#ffffff" {LINE} stroke-width="2"/>'), RT_AT(921, 480))
    # Reentry: the glow along the cone's windward side.
    layer("mc-r-glow-side", RT(f'<path d="M982 420 C998 448 1010 476 1014 504 C1020 474 1004 444 982 420 Z" fill="{GOLD_FILL}" '
                               f'stroke="{GOLD}" stroke-width="2" stroke-linejoin="round"/>'), RC)
    # The break-up: the top (canister and recovery compartment) from the cone, at the generic seam's
    # pivots (reentry.jsx throws them from there).
    top = small(f'<rect x="941" y="388" width="38" height="30" fill="#ffffff" {LINE} stroke-width="3"/>'
                f'<rect x="946" y="360" width="28" height="28" fill="#ffffff" {LINE} stroke-width="3"/>'
                f'<rect x="950" y="352" width="20" height="8" fill="#ffffff" {LINE} stroke-width="2.4"/>')
    body = small(f'<path d="M915 500 Q960 515 1005 500 Z" fill="#ffffff" {LINE} stroke-width="3"/>'
                 f'<path d="M915 500 L941 418 L979 418 L1005 500 Z" fill="#ffffff" {LINE} stroke-width="3"/>'
                 f'<path d="M952 432 L968 432 L971 452 L949 452 Z" fill="rgba(22,48,92,0.10)" {LINE} stroke-width="2"/>')
    layer("mc-r-half-nose", RT(top), RT_AT(960, 410))
    layer("mc-r-half-body", RT(body), RT_AT(960, 472))
    # The landing bag: the shield let down 34 px on a pleated skirt (nose up, RU), its pivot at the base
    # so it grows down from there.
    skirt = "".join(f'<path d="M{917 + 2 * j} {502 + 8 * j} L{1003 - 2 * j} {502 + 8 * j}" fill="none" {LINE} stroke-width="1.6" opacity="0.7"/>'
                    for j in range(1, 4))
    bag = (f'<path d="M917 502 L921 534 L999 534 L1003 502" fill="#ffffff" {LINE} stroke-width="2"/>{skirt}'
           f'<path d="M915 534 Q960 549 1005 534 Z" fill="#ffffff" {LINE} stroke-width="3"/>')
    layer("mc-r-bag", RU(small(bag)), RU_AT(960, 506))


# --- The first satellites (2026-10-18, user; the early-era pack), nose up on the rocket's top (y 500, 90 px
# across). Sputnik 1 (58 cm, four antennas swept back) and Sputnik 2 (a cone about 4 m tall: an instrument
# section on top, a sphere like Sputnik 1, Laika's cylindrical cabin at the foot) rode under a nose
# fairing; Explorer 1 (2 m long, 15 cm across, the last stage's motor its tail) stood bare on Juno I's
# spinning tub of small motors. Drawn larger than their rockets' scale where they'd vanish (Sputnik 1's
# sphere, Explorer 1's width): unconfirmed proportions.


def sat_fairing(top=330, k=1.0):
    """A plain cone fairing on the rocket's top, its tip at top."""
    return (f'<path d="M915 500 L915 450 Q930 {top + 40} 960 {top} Q990 {top + 40} 1005 450 L1005 500 Z" fill="#ffffff" '
            f'{LINE} stroke-width="{3 * k:.2f}"/>'
            f'<path d="M915 450 L1005 450" fill="none" {LINE} stroke-width="{1.6 * k:.2f}" opacity="0.6"/>')


def sputnik1(k=1.0):
    """Sputnik 1 nose up: the sphere (centre 960 430) and its four antennas swept back (down)."""
    w = lambda x: f'stroke-width="{x * k:.2f}"'
    ants = "".join(f'<path d="M{960 + dx} {440} L{960 + ex} 560" fill="none" {LINE} {w(2)}/>'
                   for dx, ex in ((-18, -70), (-8, -30), (8, 30), (18, 70)))
    return (ants + f'<circle cx="960" cy="430" r="26" fill="#ffffff" {LINE} {w(2.6)}/>'
            f'<path d="M934 432 Q960 442 986 432" fill="none" {LINE} {w(1.4)} opacity="0.7"/>'
            f'<path d="M944 414 Q952 408 962 408" fill="none" {LINE} {w(1.4)} opacity="0.6"/>')


def explorer1_whips(base=500, k=1.0, spread=1.0):
    """Explorer 1's four whip antennas, spread out by spread (1 out in orbit; 0.13 folded down along the body, as they
    rode to orbit, until its spin flung them out)."""
    w = lambda x: f'stroke-width="{x * k:.2f}"'
    x = lambda s, d: f"{960 + s * (8 + (d - 8) * spread):.1f}"
    return ("".join(f'<path d="M{x(s, 8)} {base - 96} Q{x(s, 40)} {base - 86} {x(s, 62)} {base - 54}" fill="none" '
                    f'{LINE} {w(1.6)}/>' for s in (-1, 1))
            + "".join(f'<path d="M{x(s, 8)} {base - 92} Q{x(s, 26)} {base - 70} {x(s, 36)} {base - 34}" fill="none" '
                      f'{LINE} {w(1.4)} opacity="0.8"/>' for s in (-1, 1)))


def explorer1(base=500, k=1.0, motor=True, spread=1.0):
    """Explorer 1 nose up, its foot at base: the motor case, the instrument section in bands, the nose, four whips
    (explorer1_whips; spread None for none)."""
    w = lambda x: f'stroke-width="{x * k:.2f}"'
    out = ""
    if motor:
        out += f'<path d="M955 {base} L951 {base + 10} L969 {base + 10} L965 {base} Z" fill="#ffffff" {LINE} {w(1.8)}/>'
    out += (f'<rect x="952" y="{base - 90}" width="16" height="90" fill="#ffffff" {LINE} {w(2.2)}/>'
            + "".join(f'<rect x="952" y="{base - 150 + 12 * j}" width="16" height="6" fill="{NAVY}" opacity="0.85"/>'
                      for j in range(5))
            + f'<rect x="952" y="{base - 150}" width="16" height="60" fill="none" {LINE} {w(2.2)}/>'
            f'<path d="M952 {base - 150} L960 {base - 178} L968 {base - 150} Z" fill="#ffffff" {LINE} {w(2)}/>')
    if spread is not None:
        out += explorer1_whips(base, k, spread)
    return out


def juno_tub(k=1.0):
    """Juno I's spinning tub on the rocket's top: a short drum, its ring of small motors' ends showing."""
    w = lambda x: f'stroke-width="{x * k:.2f}"'
    return (f'<rect x="930" y="462" width="60" height="38" fill="#ffffff" {LINE} {w(2.6)}/>'
            + "".join(f'<path d="M{x} 462 L{x} 500" fill="none" {LINE} {w(1.2)} opacity="0.6"/>' for x in (942, 954, 966, 978))
            + f'<path d="M930 470 L990 470" fill="none" {LINE} {w(1.4)} opacity="0.7"/>')


def sputnik2(k=1.0):
    """Sputnik 2 nose up, its foot at 500: the cone frame, Laika's cabin, the sphere, the instrument section."""
    w = lambda x: f'stroke-width="{x * k:.2f}"'
    return (f'<path d="M918 500 L948 352 M1002 500 L972 352 M918 500 L1002 500" fill="none" {LINE} {w(2)}/>'
            f'<rect x="932" y="452" width="56" height="44" rx="4" fill="#ffffff" {LINE} {w(2.6)}/>'
            f'<circle cx="960" cy="474" r="7" fill="rgba(22,48,92,0.12)" {LINE} {w(1.6)}/>'
            f'<circle cx="960" cy="420" r="24" fill="#ffffff" {LINE} {w(2.4)}/>'
            + "".join(f'<path d="M{960 + dx} 428 L{960 + ex} 452" fill="none" {LINE} {w(1.4)}/>' for dx, ex in ((-14, -26), (14, 26)))
            + f'<rect x="948" y="364" width="24" height="32" fill="#ffffff" {LINE} {w(2.2)}/>'
            f'<path d="M948 364 L960 350 L972 364 Z" fill="#ffffff" {LINE} {w(2)}/>')


def satellite_parts():
    rocket = board_group("rocket")
    nose = rocket.index('<path xmlns="http://www.w3.org/2000/svg" d="M915 440')
    big = f"translate({DX} {DY}) scale({DK}) translate(-850 -520)"
    turn = "translate(-120 20) rotate(90 960 500)"
    sats = {
        # prefix: (on the rocket, bare in the deployment and in orbit)
        "sp1": (sat_fairing(), sputnik1()),
        "sp2": (sat_fairing(top=300), sputnik2()),
        # Explorer 1 (2026-10-20, user): its whips folded down along it on the rocket, out in orbit.
        "ex1": (juno_tub() + explorer1(base=462, motor=False, spread=EX1_FOLDED), explorer1()),
    }
    for key, (on_rocket, bare) in sats.items():
        layer(f"{key}-rocket", rocket[:nose] + on_rocket, (960, 600))
        layer(f"{key}-debris-1", on_rocket, (960, 420))
        layer(f"{key}-d-payload", f'<g transform="{big}"><g transform="{turn}">{bare}</g></g>', D_AT(914, 520))
        thick = re.sub(r'stroke-width="[\d.]+"', 'stroke-width="5"', bare)
        layer(f"{key}-o-sat", f'<g transform="translate(960 560) scale(0.6) translate(-960 -426)">{thick}</g>', (960, 560))
    # Explorer 1's deployment (explorer-whips.jsx): the satellite folded, and its whips in EX1_STEPS layers from folded
    # to out, shown one at a time as its spin flings them out.
    layer("ex1-d-payload", f'<g transform="{big}"><g transform="{turn}">{explorer1(spread=None)}</g></g>', D_AT(914, 520))
    for n in range(EX1_STEPS + 1):
        spread = EX1_FOLDED + (1 - EX1_FOLDED) * n / EX1_STEPS
        layer(f"ex1-d-whips-{n}", f'<g transform="{big}"><g transform="{turn}">{explorer1_whips(spread=spread)}</g></g>',
              D_AT(914, 520))


EX1_FOLDED, EX1_STEPS = 0.13, 12


# --- Vostok (2026-10-18, user; the early-era pack): the descent sphere and its instrument module ---------
# The sphere 2.30 m across, the instrument module 2.25 m long and 2.43 m across at its ring of gas bottles,
# its one retro engine (the TDU-1) at the far end; four steel bands held the sphere to it. On the rocket
# (the launch's frame, the third stage 90 px, about 2.6 m: 34.6 px/m) under a nose fairing; coming home
# (the way home's frame, nose up about 960 426) the sphere where the generic capsule is (radius 74,
# 64.3 px/m), the module behind its base.
VK_L = 34.6


def vostok_launch(bare=True, k=1.0, backup=False):
    """The craft nose up on the rocket's top (y 500): the module, the sphere on it, and bare its engine;
    Voskhod's (backup) with its backup retro rocket on the sphere's top."""
    w = lambda x: f'stroke-width="{x * k:.2f}"'
    out = ""
    if backup:
        out += (f'<rect x="951" y="330" width="18" height="20" rx="2" fill="#ffffff" {LINE} {w(2)}/>'
                f'<path d="M951 334 L944 328 M969 334 L976 328" fill="none" {LINE} {w(1.6)}/>')
    if bare:
        out += f'<path d="M954 500 L948 512 L972 512 L966 500 Z" fill="#ffffff" {LINE} {w(2)}/>'
    out += (f'<path d="M930 500 L918 470 L938 432 L982 432 L1002 470 L990 500 Z" fill="#ffffff" {LINE} {w(2.4)}/>'
            f'<path d="M918 470 L1002 470" fill="none" {LINE} {w(1.4)} opacity="0.6"/>'
            + "".join(f'<circle cx="{x}" cy="470" r="5" fill="#ffffff" {LINE} {w(1.6)}/>' for x in (926, 943, 960, 977, 994))
            + f'<rect x="942" y="424" width="36" height="8" fill="#ffffff" {LINE} {w(2)}/>'
            f'<circle cx="960" cy="386" r="40" fill="#ffffff" {LINE} {w(2.6)}/>'
            f'<circle cx="948" cy="380" r="13" fill="none" {LINE} {w(1.6)} opacity="0.8"/>'
            f'<circle cx="977" cy="372" r="4" fill="rgba(22,48,92,0.12)" {LINE} {w(1.4)}/>'
            + ("" if backup else f'<path d="M960 346 L960 330 M974 349 L984 336" fill="none" {LINE} {w(1.6)}/>')
            + (f'<path d="M974 349 L984 336" fill="none" {LINE} {w(1.6)}/>' if backup else ""))
    return out


def vostok_fairing(k=1.0):
    """The nose fairing over it on the rocket, two portholes."""
    w = lambda x: f'stroke-width="{x * k:.2f}"'
    return (f'<path d="M915 500 L915 412 L948 326 Q960 316 972 326 L1005 412 L1005 500 Z" fill="#ffffff" {LINE} {w(3)}/>'
            f'<path d="M915 412 L1005 412" fill="none" {LINE} {w(1.6)} opacity="0.6"/>'
            + "".join(f'<circle cx="{x}" cy="440" r="7" fill="rgba(22,48,92,0.10)" {LINE} {w(1.6)}/>' for x in (942, 978)))


def vostok_sphere(backup=False):
    """The descent sphere coming home (nose up, about 960 426, radius 74): its hatch, a window, two whips;
    Voskhod's (backup) with its backup retro rocket on the top in their place."""
    out = (f'<circle cx="960" cy="426" r="74" fill="#ffffff" {LINE} stroke-width="3"/>'
           f'<circle cx="940" cy="420" r="26" fill="none" {LINE} stroke-width="2" opacity="0.8"/>'
           f'<circle cx="992" cy="398" r="8" fill="rgba(22,48,92,0.12)" {LINE} stroke-width="2"/>')
    if backup:
        return (f'<rect x="943" y="316" width="34" height="38" rx="3" fill="#ffffff" {LINE} stroke-width="2.6"/>'
                f'<path d="M943 324 L928 312 M977 324 L992 312" fill="none" {LINE} stroke-width="2.2"/>' + out
                + f'<path d="M988 358 L1002 340" fill="none" {LINE} stroke-width="2"/>')
    return out + f'<path d="M960 352 L960 330 M988 358 L1002 340" fill="none" {LINE} stroke-width="2"/>'


def vostok_module():
    """The instrument module behind the sphere's base (500): its collar, two cones about the gas bottles,
    the engine's nozzle at the end (664)."""
    return (f'<rect x="927" y="500" width="66" height="12" fill="#ffffff" {LINE} stroke-width="2.4"/>'
            f'<path d="M927 512 L882 560 L882 576 L915 640 L1005 640 L1038 576 L1038 560 L993 512 Z" fill="#ffffff" {LINE} '
            f'stroke-width="2.4"/>'
            + "".join(f'<circle cx="{x}" cy="568" r="10" fill="#ffffff" {LINE} stroke-width="1.8"/>'
                      for x in (896, 922, 948, 972, 998, 1024))
            + f'<path d="M950 640 L938 664 L982 664 L970 640 Z" fill="#ffffff" {LINE} stroke-width="2.2"/>')


def voskhod_spacewalk():
    """Voskhod 2 in the spacewalk scene (world px, nose right): the Volga airlock standing off the sphere, its
    outer hatch where the scene's hatch is (836 to 926 at 380), so the figure climbs out of its end; the sphere
    below it (1.15 m, 75 px/m), the instrument module behind, the backup retro on the nose."""
    airlock = (f'<rect x="836" y="380" width="90" height="206" fill="#ffffff" {LINE} stroke-width="3"/>'
               + "".join(f'<path d="M836 {y} L926 {y}" fill="none" {LINE} stroke-width="1.4" opacity="0.6"/>'
                         for y in (420, 460, 500, 540))
               + f'<rect x="830" y="380" width="102" height="10" rx="3" fill="#ffffff" {LINE} stroke-width="2.6"/>')
    module = (f'<rect x="780" y="632" width="15" height="44" fill="#ffffff" {LINE} stroke-width="2.6"/>'
              f'<path d="M780 632 L720 574 L700 574 L640 609 L640 699 L700 734 L720 734 L780 676 Z" fill="#ffffff" {LINE} '
              f'stroke-width="2.6"/>'
              + "".join(f'<circle cx="710" cy="{y}" r="11" fill="#ffffff" {LINE} stroke-width="1.8"/>' for y in (590, 618, 646, 674, 702))
              + f'<path d="M640 644 L620 634 L620 674 L640 664 Z" fill="#ffffff" {LINE} stroke-width="2.2"/>')
    sphere = (f'<circle cx="881" cy="654" r="86" fill="#ffffff" {LINE} stroke-width="3"/>'
              f'<circle cx="858" cy="672" r="28" fill="none" {LINE} stroke-width="2" opacity="0.8"/>'
              f'<circle cx="920" cy="690" r="9" fill="rgba(22,48,92,0.12)" {LINE} stroke-width="2"/>'
              f'<rect x="967" y="636" width="34" height="36" rx="3" fill="#ffffff" {LINE} stroke-width="2.6"/>'
              f'<path d="M1001 644 L1012 632 M1001 664 L1012 676" fill="none" {LINE} stroke-width="2.2"/>')
    return module + sphere + airlock


def vostok_parts():
    small = lambda body: re.sub(r'stroke-width="([\d.]+)"', lambda m: f'stroke-width="{float(m.group(1)) * 0.8:.2f}"', body)
    rocket = board_group("rocket")
    nose = rocket.index('<path xmlns="http://www.w3.org/2000/svg" d="M915 440')
    # The launch: under its fairing on the pad and in a break-up; the climb throws the fairing off as it
    # does Mercury's tower (the ascent's a-les), the craft bare under it; bare in the deployment and in orbit.
    layer("vk-rocket", rocket[:nose] + vostok_launch(bare=False) + vostok_fairing(), (960, 600))
    layer("vk-debris-1", vostok_launch(bare=False) + vostok_fairing(), (960, 420))
    layer("vk-debris-1-bare", vostok_launch(bare=False), (960, 420))
    layer("vk-a-les", vostok_fairing(), (960, 300))
    big = f"translate({DX} {DY}) scale({DK}) translate(-850 -520)"
    turn = "translate(-120 20) rotate(90 960 500)"
    layer("vk-d-payload", f'<g transform="{big}"><g transform="{turn}">{vostok_launch()}</g></g>', D_AT(914, 520))
    thick = re.sub(r'stroke-width="[\d.]+"', 'stroke-width="5"', vostok_launch())
    for name in ("vk-o-sat", "vk-o-sat-b"):
        layer(name, f'<g transform="translate(960 560) scale(0.6) translate(-960 -426)">{thick}</g>', (960, 560))
    # Coming home: the sphere nose right (RT) and nose up (RU); the module is the pack (the retrofire's)
    # and the module (the separation's, about its middle); the bands from the module's collar onto the
    # sphere, hinged there; the pack turns about band b's hinge, as the generic one does.
    sph, mod = small(vostok_sphere()), small(vostok_module())
    layer("vk-r-capsule", RT(sph), RC)
    layer("vk-r-capsule-up", RU(sph), RC)
    layer("vk-r-periscope", "", RC)
    layer("vk-r-pack", RT(mod), RT_AT(1014, 477))
    layer("vk-r-module", RT(mod), RT_AT(960, 575))
    layer("vk-r-strap-a", RT(f'<path d="M928 506 L906 477" fill="none" {LINE} stroke-width="2.4"/>'), RT_AT(906, 477))
    layer("vk-r-strap-b", RT(f'<path d="M992 506 L1014 477" fill="none" {LINE} stroke-width="2.4"/>'), RT_AT(1014, 477))
    layer("vk-r-cut-a", RT(f'<circle cx="906" cy="478" r="6" fill="{GOLD}"/>'), RT_AT(906, 478))
    layer("vk-r-cut-b", RT(f'<circle cx="1014" cy="478" r="6" fill="{GOLD}"/>'), RT_AT(1014, 478))
    layer("vk-r-pyro", RT("".join(f'<circle cx="{x}" cy="504" r="6" fill="{GOLD}"/>' for x in (930, 960, 990))), RT_AT(960, 504))
    # One engine: one flame; the other two empty.
    layer("vk-r-flame-1", RT(f'<path d="M944 664 C942 690 952 716 960 740 C968 716 978 690 976 664 Z" fill="{GOLD_FILL}" '
                             f'stroke="{GOLD}" stroke-width="2" stroke-linejoin="round"/>'), RT_AT(960, 664))
    layer("vk-r-flame-2", "", RT_AT(960, 664))
    layer("vk-r-flame-3", "", RT_AT(960, 664))
    # The attitude jets, on the module's forward cone.
    layer("vk-r-jet-l", RT(puff(896, 540, 10)), RT_AT(906, 540))
    layer("vk-r-jet-r", RT(puff(1024, 540, 10)), RT_AT(1014, 540))
    layer("vk-r-jet-stuck", RT(f'<path d="M1016 536 C1046 528 1080 518 1116 510 L1120 526 C1084 532 1048 544 1018 552 Z" '
                               f'fill="{GOLD_FILL}" stroke="{GOLD}" stroke-width="2" stroke-linejoin="round"/>'), RC)
    layer("vk-r-glow-side", RT(f'<path d="M1012 372 C1034 396 1042 430 1034 466 C1052 430 1040 392 1012 372 Z" fill="{GOLD_FILL}" '
                               f'stroke="{GOLD}" stroke-width="2" stroke-linejoin="round"/>'), RC)
    # Voskhod (2026-10-18, user): Vostok's parts with the backup retro on the sphere's top while in orbit; it's
    # gone by the reentry (vh-r-capsule-bare, the reentry's and the descent's: assumed jettisoned, unconfirmed).
    layer("vh-rocket", rocket[:nose] + vostok_launch(bare=False, backup=True) + vostok_fairing(), (960, 600))
    layer("vh-debris-1", vostok_launch(bare=False, backup=True) + vostok_fairing(), (960, 420))
    layer("vh-debris-1-bare", vostok_launch(bare=False, backup=True), (960, 420))
    layer("vh-d-payload", f'<g transform="{big}"><g transform="{turn}">{vostok_launch(backup=True)}</g></g>', D_AT(914, 520))
    thick = re.sub(r'stroke-width="[\d.]+"', 'stroke-width="5"', vostok_launch(backup=True))
    for name in ("vh-o-sat", "vh-o-sat-b"):
        layer(name, f'<g transform="translate(960 560) scale(0.6) translate(-960 -426)">{thick}</g>', (960, 560))
    layer("vh-r-capsule", RT(small(vostok_sphere(backup=True))), RC)
    layer("vh-r-capsule-bare", RT(sph), RC)
    layer("vh-r-capsule-up", RU(sph), RC)
    # The backup retro alone (2026-10-19, user), thrown off with the instrument module at the modules separation
    # (modules-separation.jsx's BACKUP, over the bare sphere): its fate is unconfirmed, but it's gone by the reentry.
    layer("vh-r-backup", RT(small(f'<rect x="943" y="316" width="34" height="38" rx="3" fill="#ffffff" {LINE} stroke-width="2.6"/>'
                                  f'<path d="M943 324 L928 312 M977 324 L992 312" fill="none" {LINE} stroke-width="2.2"/>')),
          RT_AT(960, 335))
    # Voskhod 2's spacewalk (2026-10-18, user): the craft with its airlock (vh-e-capsule, in place of the scene's
    # capsule), no package, no hand-held gun (Leonov had none).
    layer("vh-e-capsule", voskhod_spacewalk(), RC)
    layer("vh-e-box", "", (975, 360))
    layer("vh-e-gun", "", EP)
    # The soft landing (2026-10-18; Voskhod's crew stayed aboard): a braking rocket in the chute's lines fires
    # just before touchdown (touchdown.jsx's SOFT), a flash under the base. Generic: any capsule nose up.
    layer("r-soft", RU(f'<path d="M912 506 C924 526 944 538 960 548 C976 538 996 526 1008 506 C990 520 930 520 912 506 Z" '
                       f'fill="{GOLD_FILL}" stroke="{GOLD}" stroke-width="2" stroke-linejoin="round"/>'), RU_AT(960, 506))
    # The break-up: the sphere's two halves.
    layer("vk-r-half-nose", RT(small(f'<path d="M886 424 A74 74 0 0 1 1034 424 Z" fill="#ffffff" {LINE} stroke-width="3"/>')),
          RT_AT(960, 410))
    layer("vk-r-half-body", RT(small(f'<path d="M886 430 A74 74 0 0 0 1034 430 Z" fill="#ffffff" {LINE} stroke-width="3"/>')),
          RT_AT(960, 472))


# --- Soyuz 7K-OK (2026-10-18, user): three modules, nose first: the orbital module (an elongated sphere,
# the docking probe on its nose, 2.98 m long), the descent module (the "headlight", 2.24 m long, 2.17 m
# across), the service module (2.26 m long, 2.72 m across at its skirt, the engine and two solar-panel wings,
# drawn at 60 % of their 9.8 m span so they fit the frame). Each drawing nose up; sizes from the photographs,
# unconfirmed where the sources are silent.


# The 7K-L1 (2026-10-19, user): the orbital module's place taken by a support cone (the fairing's and the escape
# tower's mount, its height unconfirmed), and a high-gain dish on a boom for the radio from the Moon (its size and
# place unconfirmed).
SOYUZ_CONE = 1.0
# The 7K-LOK's service module (2026-10-19): the craft is 10.06 m long, so 4.4 m (the 7K-OK's is 2.26), LOK_LONGER px
# more of it coming home (fitted to the frame, not to scale). The Blok D under it: 3.7 m across, BLOK_D_H long with its
# engine (5.5 m, unconfirmed).
LOK_SM, LOK_LONGER = 4.4, 100
BLOK_D_H = 5.5


def blok_d(top, k):
    """The Blok D stage, nose up, its top at top: an adapter cone up to the craft, the tank section, the engine."""
    r, w = 1.85 * k, lambda x: f'stroke-width="{x:.2f}"'
    a = top + 0.6 * k
    body_end = top + (BLOK_D_H - 0.6) * k
    out = (f'<path d="M{960 - 1.36 * k:.1f} {top:.1f} L{960 - r:.1f} {a:.1f} L{960 + r:.1f} {a:.1f} L{960 + 1.36 * k:.1f} {top:.1f} Z" '
           f'fill="#ffffff" {LINE} {w(2.4)}/>'
           f'<rect x="{960 - r:.1f}" y="{a:.1f}" width="{2 * r:.1f}" height="{body_end - a:.1f}" fill="#ffffff" {LINE} {w(2.6)}/>'
           + "".join(f'<path d="M{960 - r:.1f} {a + (body_end - a) * f:.1f} L{960 + r:.1f} {a + (body_end - a) * f:.1f}" fill="none" '
                     f'{LINE} {w(1.2)} opacity="0.5"/>' for f in (0.35, 0.7))
           + f'<path d="M{960 - 0.3 * k:.1f} {body_end:.1f} L{960 - 0.5 * k:.1f} {body_end + 0.6 * k:.1f} L{960 + 0.5 * k:.1f} '
             f'{body_end + 0.6 * k:.1f} L{960 + 0.3 * k:.1f} {body_end:.1f} Z" fill="#ffffff" {LINE} {w(2)}/>')
    return out


def soyuz_dish(x0, y0, k, sw=2.0):
    """The high-gain dish on a boom from x0, y0 (the service module's right side), looking out and forward."""
    ax, ay = math.cos(math.radians(-50)), math.sin(math.radians(-50))
    cx, cy = x0 + 0.75 * k, y0 - 0.75 * k
    r = 0.5 * k
    p1, p2 = (cx - ay * r, cy + ax * r), (cx + ay * r, cy - ax * r)
    back = (cx - ax * 0.55 * r * 2, cy - ay * 0.55 * r * 2)
    return (f'<path d="M{x0:.1f} {y0:.1f} L{cx - ax * 0.4 * r:.1f} {cy - ay * 0.4 * r:.1f}" fill="none" {LINE} '
            f'stroke-width="{sw:.2f}"/>'
            f'<path d="M{p1[0]:.1f} {p1[1]:.1f} Q{back[0]:.1f} {back[1]:.1f} {p2[0]:.1f} {p2[1]:.1f} Z" fill="#ffffff" {LINE} '
            f'stroke-width="{sw:.2f}"/>'
            f'<path d="M{cx:.1f} {cy:.1f} L{cx + ax * 0.6 * r:.1f} {cy + ay * 0.6 * r:.1f}" fill="none" {LINE} '
            f'stroke-width="{sw * 0.8:.2f}"/>')


def soyuz_stack(top, k, panels=True, probe=True, wing=0.6, nose="om", dish=False, sm_h=2.26):
    """Soyuz nose up, its probe's tip at top, k px per metre (the parts' own sizes in metres). nose: the orbital
    module ("om"), the 7K-L1's support cone ("cone", SOYUZ_CONE tall) or nothing ("none"); dish: the high-gain
    antenna on a boom off the service module's top; sm_h: the service module's length (the 7K-LOK's is longer)."""
    w = lambda x: f'stroke-width="{x:.2f}"'
    y = top
    out = ""
    if nose == "om":
        if probe:
            out += f'<path d="M960 {y:.1f} L960 {y + 0.45 * k:.1f}" fill="none" {LINE} {w(2.4)}/>'
            out += f'<path d="M952 {y + 0.45 * k:.1f} L968 {y + 0.45 * k:.1f} L964 {y + 0.3 * k:.1f} L956 {y + 0.3 * k:.1f} Z" fill="#ffffff" {LINE} {w(1.8)}/>'
            y += 0.45 * k
        om_h, om_r = 2.98 * k, 1.13 * k
        out += (f'<ellipse cx="960" cy="{y + om_h / 2:.1f}" rx="{om_r:.1f}" ry="{om_h / 2:.1f}" fill="#ffffff" {LINE} {w(2.6)}/>'
                f'<circle cx="{960 + om_r * 0.45:.1f}" cy="{y + om_h * 0.55:.1f}" r="{0.22 * k:.1f}" fill="none" {LINE} {w(1.6)} opacity="0.8"/>'
                f'<path d="M{960 - om_r:.1f} {y + om_h / 2:.1f} Q960 {y + om_h / 2 + 0.25 * k:.1f} {960 + om_r:.1f} {y + om_h / 2:.1f}" '
                f'fill="none" {LINE} {w(1.2)} opacity="0.5"/>')
        y += om_h
    elif nose == "cone":
        ch = SOYUZ_CONE * k
        out += (f'<path d="M{960 - 0.22 * k:.1f} {y:.1f} L{960 - 0.55 * k:.1f} {y + ch:.1f} L{960 + 0.55 * k:.1f} {y + ch:.1f} '
                f'L{960 + 0.22 * k:.1f} {y:.1f} Z" fill="#ffffff" {LINE} {w(2.4)}/>'
                f'<path d="M{960 - 0.4 * k:.1f} {y + ch * 0.55:.1f} L{960 + 0.4 * k:.1f} {y + ch * 0.55:.1f}" fill="none" {LINE} '
                f'{w(1.2)} opacity="0.5"/>')
        y += ch
    dm_h, top_r, base_r = 2.24 * k, 0.5 * k, 1.085 * k
    out += (f'<path d="M{960 - top_r:.1f} {y:.1f} C{960 - top_r - 0.35 * k:.1f} {y + 0.5 * k:.1f} {960 - base_r:.1f} {y + 1.2 * k:.1f} '
            f'{960 - base_r:.1f} {y + dm_h:.1f} L{960 + base_r:.1f} {y + dm_h:.1f} C{960 + base_r:.1f} {y + 1.2 * k:.1f} '
            f'{960 + top_r + 0.35 * k:.1f} {y + 0.5 * k:.1f} {960 + top_r:.1f} {y:.1f} Z" fill="#ffffff" {LINE} {w(2.6)}/>'
            f'<circle cx="{960 + 0.42 * k:.1f}" cy="{y + 1.0 * k:.1f}" r="{0.13 * k:.1f}" fill="rgba(22,48,92,0.12)" {LINE} {w(1.4)}/>')
    y += dm_h
    sm_top, sm_skirt, sm_h = 1.1 * k, 1.36 * k, sm_h * k
    if dish:
        out += soyuz_dish(960 + sm_top, y + 0.35 * k, k)
    out += (f'<path d="M{960 - sm_top:.1f} {y:.1f} L{960 - sm_top:.1f} {y + 0.9 * k:.1f} L{960 - sm_skirt:.1f} {y + 1.4 * k:.1f} '
            f'L{960 - sm_skirt:.1f} {y + sm_h:.1f} L{960 + sm_skirt:.1f} {y + sm_h:.1f} L{960 + sm_skirt:.1f} {y + 1.4 * k:.1f} '
            f'L{960 + sm_top:.1f} {y + 0.9 * k:.1f} L{960 + sm_top:.1f} {y:.1f} Z" fill="#ffffff" {LINE} {w(2.6)}/>'
            f'<path d="M{960 - sm_top:.1f} {y + 0.3 * k:.1f} L{960 + sm_top:.1f} {y + 0.3 * k:.1f}" fill="none" {LINE} {w(1.2)} opacity="0.5"/>'
            f'<path d="M{960 - 0.2 * k:.1f} {y + sm_h:.1f} L{960 - 0.33 * k:.1f} {y + sm_h + 0.4 * k:.1f} L{960 + 0.33 * k:.1f} '
            f'{y + sm_h + 0.4 * k:.1f} L{960 + 0.2 * k:.1f} {y + sm_h:.1f} Z" fill="#ffffff" {LINE} {w(2)}/>')
    if panels:
        span, h, py = 4.0 * wing * k, 0.95 * k, y + 1.55 * k
        for sgn in (-1, 1):
            x0 = 960 + sgn * sm_skirt
            x1 = x0 + sgn * span
            lo, hi = min(x0, x1), max(x0, x1)
            out += (f'<rect x="{lo:.1f}" y="{py - h / 2:.1f}" width="{hi - lo:.1f}" height="{h:.1f}" fill="{GM_DARK}" {LINE} {w(2)}/>'
                    + "".join(f'<path d="M{lo + (hi - lo) * f:.1f} {py - h / 2:.1f} L{lo + (hi - lo) * f:.1f} {py + h / 2:.1f}" '
                              f'fill="none" {LINE} {w(1.2)} opacity="0.7"/>' for f in (0.25, 0.5, 0.75)))
    return out


def soyuz_fairing(k=1.0):
    """The fairing over it on the rocket, and the escape tower on top."""
    wd = lambda x: f'stroke-width="{x * k:.2f}"'
    return (f'<path d="M912 500 L912 300 Q914 250 948 226 L972 226 Q1006 250 1008 300 L1008 500 Z" fill="#ffffff" {LINE} {wd(3)}/>'
            f'<path d="M912 300 L1008 300" fill="none" {LINE} {wd(1.4)} opacity="0.6"/>'
            + "".join(f'<path d="M{x} 340 L{x} 470" fill="none" {LINE} {wd(1.2)} opacity="0.5"/>' for x in (934, 986))
            + f'<path d="M952 226 L956 186 L964 186 L968 226" fill="none" {LINE} {wd(2.2)}/>'
            f'<rect x="953" y="140" width="14" height="46" fill="#ffffff" {LINE} {wd(2.2)}/>'
            f'<path d="M953 140 L960 124 L967 140 Z" fill="#ffffff" {LINE} {wd(2)}/>'
            f'<path d="M953 186 L946 194 M967 186 L974 194" fill="none" {LINE} {wd(1.8)}/>')


def soyuz_om_home():
    """The orbital module on the descent module's top (352), as the way home's r-om (66 px/m)."""
    return (f'<rect x="938" y="342" width="44" height="12" fill="#ffffff" {LINE} stroke-width="2.4"/>'
         f'<ellipse cx="960" cy="244" rx="72" ry="98" fill="#ffffff" {LINE} stroke-width="2.6"/>'
         f'<circle cx="992" cy="262" r="14" fill="none" {LINE} stroke-width="1.6" opacity="0.8"/>'
         f'<path d="M888 244 Q960 260 1032 244" fill="none" {LINE} stroke-width="1.2" opacity="0.5"/>'
         f'<path d="M960 146 L960 118" fill="none" {LINE} stroke-width="2.4"/>'
         f'<path d="M952 120 L968 120 L964 110 L956 110 Z" fill="#ffffff" {LINE} stroke-width="1.8"/>')


def soyuz_dm_home():
    """The descent module coming home (nose up, its base at 500, 72 across half, its hatch's top at 352)."""
    return (f'<path d="M928 352 C904 384 888 430 888 500 L1032 500 C1032 430 1016 384 992 352 Z" fill="#ffffff" {LINE} '
            f'stroke-width="3"/>'
            f'<path d="M888 500 Q960 514 1032 500" fill="none" {LINE} stroke-width="2.4"/>'
            f'<circle cx="988" cy="420" r="9" fill="rgba(22,48,92,0.12)" {LINE} stroke-width="2"/>'
            f'<rect x="940" y="346" width="40" height="8" fill="#ffffff" {LINE} stroke-width="2"/>')


def soyuz_sm_home(wings=True, dish=False, longer=0):
    """The service module behind the base (500 to 664), its engine and the two wings (40 % span: the way home's
    turns would swing longer ones off the frame); the 7K-L1's dish; longer px more of it (the 7K-LOK's)."""
    out = soyuz_dish(1032, 523, 66, 2.0) if dish else ""
    end = 650 + longer
    out += (f'<rect x="896" y="500" width="128" height="12" fill="#ffffff" {LINE} stroke-width="2.4"/>'
            f'<path d="M888 512 L888 560 L870 590 L870 {end} L1050 {end} L1050 590 L1032 560 L1032 512 Z" fill="#ffffff" {LINE} '
            f'stroke-width="2.6"/>'
            f'<path d="M888 532 L1032 532" fill="none" {LINE} stroke-width="1.2" opacity="0.5"/>'
            f'<path d="M948 {end} L940 {end + 18} L980 {end + 18} L972 {end} Z" fill="#ffffff" {LINE} stroke-width="2.2"/>')
    if longer:
        out += f'<path d="M870 650 L1050 650" fill="none" {LINE} stroke-width="1.2" opacity="0.5"/>'
    if not wings:
        return out
    for x0, x1 in ((870, 760), (1050, 1160)):
        lo, hi = min(x0, x1), max(x0, x1)
        out += (f'<rect x="{lo}" y="596" width="{hi - lo}" height="40" fill="{GM_DARK}" {LINE} stroke-width="2"/>'
                + "".join(f'<path d="M{lo + (hi - lo) * f:.1f} 596 L{lo + (hi - lo) * f:.1f} 636" fill="none" {LINE} '
                          f'stroke-width="1.2" opacity="0.7"/>' for f in (0.25, 0.5, 0.75)))
    return out


def soyuz_parts():
    small = lambda body: re.sub(r'stroke-width="([\d.]+)"', lambda m: f'stroke-width="{float(m.group(1)) * 0.8:.2f}"', body)
    rocket = board_group("rocket")
    nose = rocket.index('<path xmlns="http://www.w3.org/2000/svg" d="M915 440')
    big = f"translate({DX} {DY}) scale({DK}) translate(-850 -520)"
    turn = "translate(-120 20) rotate(90 960 500)"
    # The launch (33.8 px/m, the rocket's top stage 2.66 m): under its fairing and tower on the pad; the climb
    # throws both off (a-les), the craft under them with its wings folded; deployed in orbit.
    K = 33.8
    top = 500 - (0.45 + 2.98 + 2.24 + 2.26) * K
    folded = soyuz_stack(top, K, panels=False)
    out = soyuz_stack(top, K)
    layer("sy-rocket", rocket[:nose] + folded + soyuz_fairing(), (960, 600))
    layer("sy-debris-1", folded + soyuz_fairing(), (960, 420))
    layer("sy-debris-1-bare", folded, (960, 420))
    layer("sy-a-les", soyuz_fairing(), (960, 300))
    layer("sy-d-payload", f'<g transform="{big}"><g transform="{turn}">{out}</g></g>', D_AT(914, 520))
    thick = re.sub(r'stroke-width="[\d.]+"', 'stroke-width="5"', out)
    for name in ("sy-o-sat", "sy-o-sat-b"):
        layer(name, f'<g transform="translate(960 560) scale(0.6) translate(-960 -426)">{thick}</g>', (960, 560))
    # Coming home (66 px/m): the descent module where the generic capsule is; the service module the pack (the
    # retrofire's) and the module (the separation's, about its middle); the orbital module on the nose, latched
    # at two points, turning about latch b; no straps.
    dm, sm = small(soyuz_dm_home()), small(soyuz_sm_home())
    layer("sy-r-capsule", RT(dm), RC)
    layer("sy-r-capsule-up", RU(dm), RC)
    layer("sy-r-periscope", "", RC)
    layer("sy-r-pack", RT(sm), RT_AT(960, 580))
    layer("sy-r-module", RT(sm), RT_AT(960, 580))
    # The 7K-L1 (2026-10-19, user; s1- parts, played on Soyuz's skin): on the rocket its support cone where the
    # orbital module was, under the same fairing and tower, which take the cone with them in the climb (assumed,
    # unconfirmed); out of it the descent and service modules, the wings and the dish unfolded; coming home the
    # service module with its dish.
    l1_top = 500 - (SOYUZ_CONE + 2.24 + 2.26) * K
    l1_folded = soyuz_stack(l1_top, K, panels=False, nose="cone")
    l1_bare = soyuz_stack(l1_top + SOYUZ_CONE * K, K, panels=False, nose="none")
    l1_out = soyuz_stack(l1_top + SOYUZ_CONE * K, K, nose="none", dish=True)
    layer("s1-rocket", rocket[:nose] + l1_folded + soyuz_fairing(), (960, 600))
    layer("s1-debris-1", l1_folded + soyuz_fairing(), (960, 420))
    layer("s1-debris-1-bare", l1_bare, (960, 420))
    layer("s1-d-payload", f'<g transform="{big}"><g transform="{turn}">{l1_out}</g></g>', D_AT(914, 520))
    l1_thick = re.sub(r'stroke-width="[\d.]+"', 'stroke-width="5"', l1_out)
    for name in ("s1-o-sat", "s1-o-sat-b"):
        layer(name, f'<g transform="translate(960 560) scale(0.6) translate(-960 -426)">{l1_thick}</g>', (960, 560))
    l1_sm = small(soyuz_sm_home(dish=True))
    layer("s1-r-pack", RT(l1_sm), RT_AT(960, 580))
    layer("s1-r-module", RT(l1_sm), RT_AT(960, 580))
    # The 7K-LOK (2026-10-19, user; lok- parts, played on Soyuz's skin): 10.06 m long, so its service module is
    # LOK_SM long (the rest is 7K-OK's), no wings (fuel cells), its Kontakt probe on the orbital module's nose (drawn
    # as Soyuz's probe). On the rocket it's drawn at Soyuz's height (KL px/m) under the same fairing and tower; it
    # leaves the rocket on its Blok D (05-deployment), then the Blok D (its own deployment); coming home the longer
    # service module (project-soyuz-lok.jsx has its pivot and near end for the cable).
    KL = 33.8 * (0.45 + 2.98 + 2.24 + 2.26) / (0.45 + 2.98 + 2.24 + LOK_SM)
    lok_top = 500 - (0.45 + 2.98 + 2.24 + LOK_SM) * KL
    lok = soyuz_stack(lok_top, KL, panels=False, sm_h=LOK_SM)
    on_d = soyuz_stack(lok_top - BLOK_D_H * KL, KL, panels=False, sm_h=LOK_SM) + blok_d(500 - BLOK_D_H * KL, KL)
    layer("lok-rocket", rocket[:nose] + lok + soyuz_fairing(), (960, 600))
    layer("lok-debris-1", lok + soyuz_fairing(), (960, 420))
    layer("lok-debris-1-bare", lok, (960, 420))
    layer("lok-d-payload", f'<g transform="{big}"><g transform="{turn}">{lok}</g></g>', D_AT(914, 520))
    layer("lok-d-payload-stack", f'<g transform="{big}"><g transform="{turn}">{on_d}</g></g>', D_AT(914, 520))
    layer("lok-d-stage", f'<g transform="{big}"><g transform="{turn}">{blok_d(500, KL)}</g></g>', D_AT(770, 520))
    lok_map = re.sub(r'stroke-width="[\d.]+"', 'stroke-width="5"', soyuz_stack(500 - (0.45 + 2.98 + 2.24 + LOK_SM) * K, K,
                                                                               panels=False, sm_h=LOK_SM))
    for name in ("lok-o-sat", "lok-o-sat-b"):
        layer(name, f'<g transform="translate(960 560) scale(0.6) translate(-960 -426)">{lok_map}</g>', (960, 560))
    lok_sm = small(soyuz_sm_home(wings=False, longer=LOK_LONGER))
    lok_mid = (500 + 668 + LOK_LONGER) / 2
    layer("lok-r-pack", RT(lok_sm), RT_AT(960, lok_mid))
    # Its engine's flame at its own nozzle (the 7K-OK's is LOK_LONGER further in, inside the service module).
    layer("lok-r-flame-1", RT(f'<path d="M944 {668 + LOK_LONGER} C942 {694 + LOK_LONGER} 952 {720 + LOK_LONGER} 960 {744 + LOK_LONGER} '
                              f'C968 {720 + LOK_LONGER} 978 {694 + LOK_LONGER} 976 {668 + LOK_LONGER} Z" fill="{GOLD_FILL}" '
                              f'stroke="{GOLD}" stroke-width="2" stroke-linejoin="round"/>'), RT_AT(960, 668 + LOK_LONGER))
    # On the map still on its Blok D (the orbit insertion: the stack's last burn of the climb).
    lok_stage_map = re.sub(r'stroke-width="[\d.]+"', 'stroke-width="5"',
                           soyuz_stack(500 - (0.45 + 2.98 + 2.24 + LOK_SM) * K, K, panels=False, sm_h=LOK_SM) + blok_d(500, K))
    layer("lok-o-sat-stage", f'<g transform="translate(960 560) scale(0.6) translate(-960 -426)">{lok_stage_map}</g>', (960, 560))
    layer("lok-r-module", RT(lok_sm), RT_AT(960, lok_mid))
    layer("sy-r-om", RT(small(soyuz_om_home())), RT_AT(980, 352))
    layer("sy-r-om-cut-a", RT(f'<circle cx="940" cy="350" r="6" fill="{GOLD}"/>'), RT_AT(940, 350))
    layer("sy-r-om-cut-b", RT(f'<circle cx="980" cy="350" r="6" fill="{GOLD}"/>'), RT_AT(980, 350))
    for n in ("strap-a", "strap-b", "cut-a", "cut-b"):
        layer(f"sy-r-{n}", "", RC)
    layer("sy-r-pyro", RT("".join(f'<circle cx="{x}" cy="504" r="6" fill="{GOLD}"/>' for x in (910, 960, 1010))), RT_AT(960, 504))
    layer("sy-r-flame-1", RT(f'<path d="M944 668 C942 694 952 720 960 744 C968 720 978 694 976 668 Z" fill="{GOLD_FILL}" '
                             f'stroke="{GOLD}" stroke-width="2" stroke-linejoin="round"/>'), RT_AT(960, 668))
    layer("sy-r-flame-2", "", RT_AT(960, 668))
    layer("sy-r-flame-3", "", RT_AT(960, 668))
    layer("sy-r-jet-l", RT(puff(880, 540, 10)), RT_AT(890, 540))
    layer("sy-r-jet-r", RT(puff(1040, 540, 10)), RT_AT(1030, 540))
    layer("sy-r-jet-stuck", RT(f'<path d="M1034 536 C1064 528 1098 518 1134 510 L1138 526 C1102 532 1066 544 1036 552 Z" '
                               f'fill="{GOLD_FILL}" stroke="{GOLD}" stroke-width="2" stroke-linejoin="round"/>'), RC)
    layer("sy-r-glow-side", RT(f'<path d="M1000 366 C1024 396 1036 440 1036 496 C1050 440 1030 392 1000 366 Z" fill="{GOLD_FILL}" '
                               f'stroke="{GOLD}" stroke-width="2" stroke-linejoin="round"/>'), RC)
    layer("sy-r-half-nose", RT(small(f'<path d="M928 352 C904 384 892 420 891 440 L1029 440 C1028 420 1016 384 992 352 Z" '
                                     f'fill="#ffffff" {LINE} stroke-width="3"/>')), RT_AT(960, 410))
    layer("sy-r-half-body", RT(small(f'<path d="M890 444 L1030 444 C1032 466 1032 484 1032 500 L888 500 C888 484 888 466 890 444 Z" '
                                     f'fill="#ffffff" {LINE} stroke-width="3"/>')), RT_AT(960, 472))
    # The docking close-up (39 px/m, as Gemini's there): the active Soyuz, its probe's tip at the generic
    # capsule's nose (352), nose right; the passive one facing it, its drogue's mouth at the target's (540);
    # the other of a rendezvous, nose left. Its jets: braking at the orbital module, pushing at the service
    # module's foot, one on the side, one stuck.
    S = 39
    whole = soyuz_stack(352, S)
    layer("sy-k-chaser", KT(KC, 90, whole), KC)
    oc = (1150, 470)
    layer("sy-k-other", KT(oc, -90, whole), oc)
    # The passive one, upside down so its drogue (a cone, mouth at the bottom) meets the chaser's probe.
    passive = soyuz_stack(352, S, probe=False)
    drogue = f'<path d="M944 352 L920 336 L1000 336 L976 352 Z" fill="#ffffff" {LINE} stroke-width="2.2"/>'
    # Flip it about y = 446 so its nose (352) lands at 540 and its foot above; the drogue's mouth at 540.
    flipped = f'<g transform="translate(0 892) scale(1 -1)">{passive}{drogue}</g>'
    tc = (KM[0] + KK * (540 - 426), KM[1])
    layer("sy-k-target", KT(tc, 90, flipped), KM)
    foot = 352 + (0.45 + 2.98 + 2.24 + 2.26 + 0.4) * S
    sh_t, sh_b = K_AT(KC, 916, 400, 90), K_AT(KC, 1004, 400, 90)
    layer("sy-k-brake", puff(sh_t[0] + 18, sh_t[1] - 6, 9) + puff(sh_b[0] + 18, sh_b[1] + 6, 9), KC)
    b_t, b_b = K_AT(KC, 912, foot - 10, 90), K_AT(KC, 1008, foot - 10, 90)
    layer("sy-k-push", puff(b_t[0] - 16, b_t[1], 9) + puff(b_b[0] - 16, b_b[1], 9), KC)
    side = K_AT(KC, 916, 560, 90)
    layer("sy-k-side", puff(side[0], side[1] - 16, 10), side)
    root = K_AT(KC, 912, 580, 90)
    layer("sy-k-stuck", f'<path d="M{root[0] - 6:.1f} {root[1]:.1f} C{root[0] - 14:.1f} {root[1] - 40:.1f} {root[0] - 20:.1f} '
                        f'{root[1] - 80:.1f} {root[0] - 28:.1f} {root[1] - 120:.1f} L{root[0] - 12:.1f} {root[1] - 122:.1f} '
                        f'C{root[0] - 6:.1f} {root[1] - 80:.1f} {root[0] + 2:.1f} {root[1] - 40:.1f} {root[0] + 6:.1f} {root[1]:.1f} Z" '
                        f'fill="{GOLD_FILL}" stroke="{GOLD}" stroke-width="2" stroke-linejoin="round"/>', KC)
    # The 7K-LOK in the close-up (2026-10-19): the active one and the other of a rendezvous, its push at its own foot.
    lok_whole = soyuz_stack(352, S, panels=False, sm_h=LOK_SM)
    layer("lok-k-chaser", KT(KC, 90, lok_whole), KC)
    layer("lok-k-other", KT(oc, -90, lok_whole), oc)
    lok_foot = 352 + (0.45 + 2.98 + 2.24 + LOK_SM + 0.4) * S
    b_t, b_b = K_AT(KC, 912, lok_foot - 10, 90), K_AT(KC, 1008, lok_foot - 10, 90)
    layer("lok-k-push", puff(b_t[0] - 16, b_t[1], 9) + puff(b_b[0] - 16, b_b[1], 9), KC)


# --- Apollo (2026-10-18, user): the command and service module (CSM). The command module a cone 3.9 m across
# and 3.2 m tall, its docking probe on the apex; the service module a cylinder 3.9 m across, its RCS quads, the
# high-gain antenna at its foot and the SPS engine's big bell. On the pad, on the S-IVB (6.6 m: the rocket's
# top, 90 px, 13.6 px/m): the adapter (SLA, 8.5 m, the lander inside), the CSM, the boost cover over the
# command module and the escape tower, which the climb throws off together (a-les). Sizes from the
# photographs, the service module's length (4.6 m, the bell 2.8 m) unconfirmed.
AP_L = 13.6


def apollo_csm(y0, k, probe=True):
    """The CSM nose up, the service module's foot at y0 (the bell below it), k px per metre."""
    w = lambda x: f'stroke-width="{x:.2f}"'
    r = 1.95 * k
    sm_h, cm_h, bell = 4.6 * k, 3.2 * k, 2.8 * k
    top_sm = y0 - sm_h
    out = (f'<path d="M{960 - 0.4 * k:.1f} {y0:.1f} L{960 - 1.25 * k:.1f} {y0 + bell:.1f} L{960 + 1.25 * k:.1f} {y0 + bell:.1f} '
           f'L{960 + 0.4 * k:.1f} {y0:.1f} Z" fill="#ffffff" {LINE} {w(2.2)}/>'
           f'<rect x="{960 - r:.1f}" y="{top_sm:.1f}" width="{2 * r:.1f}" height="{sm_h:.1f}" fill="#ffffff" {LINE} {w(2.6)}/>'
           + "".join(f'<path d="M{960 - r:.1f} {top_sm + f * sm_h:.1f} L{960 + r:.1f} {top_sm + f * sm_h:.1f}" fill="none" {LINE} '
                     f'{w(1.2)} opacity="0.5"/>' for f in (0.2, 0.85))
           # The RCS quads, two seen, and the high-gain antenna on its boom at the foot.
           + "".join(f'<rect x="{960 + sgn * r - (0.35 * k if sgn > 0 else 0.0):.1f}" y="{top_sm + 0.35 * sm_h:.1f}" '
                     f'width="{0.35 * k:.1f}" height="{0.5 * k:.1f}" fill="#ffffff" {LINE} {w(1.6)} transform="translate({sgn * 0.3 * k:.1f} 0)"/>'
                     for sgn in (-1, 1))
           + f'<path d="M{960 - r:.1f} {y0 - 0.3 * k:.1f} L{960 - r - 0.9 * k:.1f} {y0 + 0.3 * k:.1f}" fill="none" {LINE} {w(1.6)}/>'
           f'<circle cx="{960 - r - 1.0 * k:.1f}" cy="{y0 + 0.45 * k:.1f}" r="{0.32 * k:.1f}" fill="#ffffff" {LINE} {w(1.6)}/>')
    base = top_sm
    apex = base - cm_h
    out += (f'<path d="M{960 - r:.1f} {base:.1f} L{960 - 0.3 * k:.1f} {apex:.1f} L{960 + 0.3 * k:.1f} {apex:.1f} L{960 + r:.1f} {base:.1f} Z" '
            f'fill="#ffffff" {LINE} {w(2.6)}/>'
            f'<path d="M{960 + 0.6 * k:.1f} {base - 1.6 * k:.1f} L{960 + 0.95 * k:.1f} {base - 1.6 * k:.1f} L{960 + 0.85 * k:.1f} '
            f'{base - 1.95 * k:.1f} L{960 + 0.6 * k:.1f} {base - 1.95 * k:.1f} Z" fill="rgba(22,48,92,0.12)" {LINE} {w(1.4)}/>'
            f'<path d="M{960 - 0.9 * k:.1f} {base - 0.5 * k:.1f} L{960 - 0.35 * k:.1f} {base - 0.5 * k:.1f} L{960 - 0.5 * k:.1f} '
            f'{base - 1.8 * k:.1f} L{960 - 0.7 * k:.1f} {base - 1.8 * k:.1f} Z" fill="none" {LINE} {w(1.2)} opacity="0.7"/>')
    if probe:
        out += f'<path d="M960 {apex:.1f} L960 {apex - 0.6 * k:.1f}" fill="none" {LINE} {w(2)}/>'
    return out


def apollo_lm_small(apex, k):
    """A small lunar module docked on the command module's apex, upside down (its top hatch on the probe):
    the ascent stage (faceted), the descent stage (an octagon seen side on) and its legs, folded out."""
    w = lambda x: f'stroke-width="{x:.2f}"'
    a0 = apex - 0.6 * k
    asc = (f'<path d="M{960 - 1.4 * k:.1f} {a0:.1f} L{960 + 1.4 * k:.1f} {a0:.1f} L{960 + 2.1 * k:.1f} {a0 - 1.2 * k:.1f} '
           f'L{960 + 1.9 * k:.1f} {a0 - 2.4 * k:.1f} L{960 - 1.9 * k:.1f} {a0 - 2.4 * k:.1f} L{960 - 2.1 * k:.1f} {a0 - 1.2 * k:.1f} Z" '
           f'fill="#ffffff" {LINE} {w(2.2)}/>')
    d0 = a0 - 2.4 * k
    desc = (f'<rect x="{960 - 2.1 * k:.1f}" y="{d0 - 1.6 * k:.1f}" width="{4.2 * k:.1f}" height="{1.6 * k:.1f}" fill="{GM_DARK}" '
            f'{LINE} {w(2.2)}/>'
            + "".join(f'<path d="M{960 + sgn * 2.1 * k:.1f} {d0 - 0.8 * k:.1f} L{960 + sgn * 3.6 * k:.1f} {d0 - 2.9 * k:.1f} '
                      f'M{960 + sgn * 3.2 * k:.1f} {d0 - 2.9 * k:.1f} L{960 + sgn * 4.0 * k:.1f} {d0 - 2.9 * k:.1f}" fill="none" {LINE} {w(1.8)}/>'
                      for sgn in (-1, 1)))
    return asc + desc


def apollo_sivb(y0, k):
    """The S-IVB under it from y0 (the adapter's foot) down 6.6 m across, 17.8 m long (shortened), its J-2 bell."""
    w = lambda x: f'stroke-width="{x:.2f}"'
    h = 9.0 * k
    return (f'<rect x="{960 - 3.3 * k:.1f}" y="{y0:.1f}" width="{6.6 * k:.1f}" height="{h:.1f}" fill="#ffffff" {LINE} {w(2.6)}/>'
            + "".join(f'<path d="M{960 - 3.3 * k:.1f} {y0 + f * h:.1f} L{960 + 3.3 * k:.1f} {y0 + f * h:.1f}" fill="none" {LINE} '
                      f'{w(1.2)} opacity="0.5"/>' for f in (0.25, 0.7))
            + f'<path d="M{960 - 0.6 * k:.1f} {y0 + h:.1f} L{960 - 1.2 * k:.1f} {y0 + h + 1.6 * k:.1f} L{960 + 1.2 * k:.1f} '
            f'{y0 + h + 1.6 * k:.1f} L{960 + 0.6 * k:.1f} {y0 + h:.1f} Z" fill="#ffffff" {LINE} {w(2)}/>')


def apollo_sla(y0, k):
    """The adapter on the S-IVB's top (y0): a frustum from 6.6 m to 3.9 m, 8.5 m tall."""
    return (f'<path d="M{960 - 3.3 * k:.1f} {y0:.1f} L{960 - 1.95 * k:.1f} {y0 - 8.5 * k:.1f} L{960 + 1.95 * k:.1f} {y0 - 8.5 * k:.1f} '
            f'L{960 + 3.3 * k:.1f} {y0:.1f} Z" fill="#ffffff" {LINE} stroke-width="2.6"/>'
            f'<path d="M{960 - 2.62 * k:.1f} {y0 - 4.25 * k:.1f} L{960 + 2.62 * k:.1f} {y0 - 4.25 * k:.1f}" fill="none" {LINE} '
            f'stroke-width="1.2" opacity="0.5"/>')


def apollo_tower(cm_base, k):
    """The boost cover over the command module and the escape tower on it (10 m)."""
    apex = cm_base - 3.2 * k
    w = lambda x: f'stroke-width="{x:.2f}"'
    return (f'<path d="M{960 - 2.05 * k:.1f} {cm_base + 0.2 * k:.1f} L{960 - 0.5 * k:.1f} {apex - 0.4 * k:.1f} L{960 + 0.5 * k:.1f} '
            f'{apex - 0.4 * k:.1f} L{960 + 2.05 * k:.1f} {cm_base + 0.2 * k:.1f} Z" fill="#ffffff" {LINE} {w(2.6)}/>'
            f'<path d="M{960 - 0.45 * k:.1f} {apex - 0.4 * k:.1f} L{960 - 0.3 * k:.1f} {apex - 3.6 * k:.1f} M{960 + 0.45 * k:.1f} '
            f'{apex - 0.4 * k:.1f} L{960 + 0.3 * k:.1f} {apex - 3.6 * k:.1f}" fill="none" {LINE} {w(2)}/>'
            f'<rect x="{960 - 0.4 * k:.1f}" y="{apex - 8.6 * k:.1f}" width="{0.8 * k:.1f}" height="{5.0 * k:.1f}" fill="#ffffff" '
            f'{LINE} {w(2)}/>'
            f'<path d="M{960 - 0.4 * k:.1f} {apex - 8.6 * k:.1f} L960 {apex - 10 * k:.1f} L{960 + 0.4 * k:.1f} {apex - 8.6 * k:.1f} Z" '
            f'fill="#ffffff" {LINE} {w(2)}/>')


# --- The later NASA satellites (2026-10-21, user; the standard pack): the Orbiting Frog Otolith, Biosatellite and
# Pegasus, in the first satellites' manner. Proportions from the blueprints and photographs, drawn larger than their
# rockets' scale where they'd vanish (OFO's 0.76 m): unconfirmed.

# OFO (0.76 m across, 1.2 m long): an octagonal lower section for the electronics, a truncated cone above it for the
# frogs' centrifuge under its heat shield; four booms folded against its side rode to orbit and swung out once the
# yo-yo weights had slowed the Scout's spin (of-d-whips-0 to -12, explorer-whips.jsx), to keep it steady below a
# thousandth of a g.
OFO_FOLDED = 0.0


def ofo_booms(base=500, k=1.0, spread=1.0):
    """OFO's four booms, hinged at the octagon's top corners: up along the cone folded (spread 0), straight out at 1.
    The outer pair seen side on, the inner pair foreshortened (towards and away from us)."""
    w = lambda x: f'stroke-width="{x * k:.2f}"'
    a = math.radians(6 + 84 * spread)
    out = ""
    for s in (-1, 1):
        for hx, length, sw, op in ((30, 54, 1.8, 1.0), (16, 32, 1.5, 0.8)):
            x0, y0 = 960 + s * hx, base - 48
            x1, y1 = x0 + s * length * math.sin(a), y0 - length * math.cos(a)
            out += (f'<path d="M{x0} {y0} L{x1:.1f} {y1:.1f}" fill="none" {LINE} {w(sw)} opacity="{op}"/>'
                    f'<circle cx="{x1:.1f}" cy="{y1:.1f}" r="{3.2 * k:.1f}" fill="#ffffff" {LINE} {w(1.4)} opacity="{op}"/>')
    return out


def ofo(base=500, k=1.0, spread=1.0):
    """OFO nose up, its foot at base: the octagon (three faces seen), the cone under its shield, the booms (spread None
    for none)."""
    w = lambda x: f'stroke-width="{x * k:.2f}"'
    out = (f'<rect x="930" y="{base - 48}" width="60" height="48" fill="#ffffff" {LINE} {w(2.4)}/>'
           + "".join(f'<path d="M{x} {base - 48} L{x} {base}" fill="none" {LINE} {w(1.4)} opacity="0.7"/>' for x in (942, 978))
           + f'<path d="M953 {base} L951 {base + 6} L969 {base + 6} L967 {base} Z" fill="#ffffff" {LINE} {w(1.6)}/>'
           f'<path d="M932 {base - 48} L946 {base - 100} L974 {base - 100} L988 {base - 48} Z" fill="url(#hatch)" {LINE} {w(2.4)}/>'
           f'<path d="M939 {base - 74} L981 {base - 74}" fill="none" {LINE} {w(1.4)} opacity="0.7"/>'
           f'<rect x="954" y="{base - 106}" width="12" height="6" fill="#ffffff" {LINE} {w(1.6)}/>')
    if spread is not None:
        out += ofo_booms(base, k, spread)
    return out


# Biosatellite: the recovery capsule on its adapter (the blueprint's louvred drum, the deorbit motor inside it); the
# capsule a bell on its heat shield, the parachute canister on top. Coming home (bs-r-) it's drawn in the generic
# capsule's place (its base 915 to 1005 at 500, its top at 352, where the chutes' lines meet); on the rocket and in orbit
# at BS_K of that on the adapter. The adapter stays in orbit: it's gone by the de-orbit burn, which starts with the
# capsule alone (no step separates them; unconfirmed). Its one motor fires from a stub on the shield's middle.
BS_K = 0.7


def bios_capsule(k=1.0):
    """The capsule nose up, its base at 500, its stroke widths times k."""
    w = lambda x: f'stroke-width="{x * k:.2f}"'
    return (f'<path d="M915 500 C915 420 938 372 960 372 C982 372 1005 420 1005 500 Z" '
            f'fill="#ffffff" {LINE} {w(3)}/>'
            f'<path d="M915 500 Q960 516 1005 500 Z" fill="url(#hatch)" {LINE} {w(2.6)}/>'
            f'<path d="M918 470 Q960 480 1002 470" fill="none" {LINE} {w(1.6)} opacity="0.6"/>'
            f'<path d="M927 430 Q960 438 993 430" fill="none" {LINE} {w(1.6)} opacity="0.6"/>'
            f'<rect x="952" y="356" width="16" height="18" rx="2" fill="#ffffff" {LINE} {w(2.2)}/>')


def bios_adapter(top, k=1.0):
    """The adapter drum under the capsule, its top at top: 80 across, 70 tall, a panel of louvres."""
    w = lambda x: f'stroke-width="{x * k:.2f}"'
    return (f'<rect x="920" y="{top}" width="80" height="70" fill="#ffffff" {LINE} {w(2.6)}/>'
            + "".join(f'<rect x="946" y="{top + 18 + 9 * j}" width="28" height="5" fill="{NAVY}" opacity="0.85"/>' for j in range(4))
            + f'<path d="M920 {top + 8} L1000 {top + 8}" fill="none" {LINE} {w(1.4)} opacity="0.6"/>')


def biosatellite(k=1.0):
    """The satellite nose up, its foot at 500: the adapter, the capsule on it."""
    top = 430
    return (bios_adapter(top, k)
            + f'<g transform="translate(960 {top}) scale({BS_K}) translate(-960 -500)">{bios_capsule(k / BS_K)}</g>')


def ofo_bios_parts():
    rocket = board_group("rocket")
    nose = rocket.index('<path xmlns="http://www.w3.org/2000/svg" d="M915 440')
    big = f"translate({DX} {DY}) scale({DK}) translate(-850 -520)"
    turn = "translate(-120 20) rotate(90 960 500)"
    sats = {
        # prefix: (on the rocket, bare in the deployment and in orbit). OFO's booms folded on the Scout, out in orbit.
        "of": (sat_fairing(top=350), ofo(spread=OFO_FOLDED), ofo()),
        "bs": (sat_fairing(top=270), biosatellite(), biosatellite()),
    }
    for key, (on_rocket, deploying, bare) in sats.items():
        layer(f"{key}-rocket", rocket[:nose] + on_rocket, (960, 600))
        layer(f"{key}-debris-1", on_rocket, (960, 420))
        layer(f"{key}-d-payload", f'<g transform="{big}"><g transform="{turn}">{deploying}</g></g>', D_AT(914, 520))
        thick = re.sub(r'stroke-width="[\d.]+"', 'stroke-width="5"', bare)
        layer(f"{key}-o-sat", f'<g transform="translate(960 560) scale(0.6) translate(-960 -426)">{thick}</g>', (960, 560))
    # OFO's deployment (explorer-whips.jsx): the satellite without its booms, and the booms from folded to out.
    layer("of-d-payload", f'<g transform="{big}"><g transform="{turn}">{ofo(spread=None)}</g></g>', D_AT(914, 520))
    for n in range(EX1_STEPS + 1):
        layer(f"of-d-whips-{n}", f'<g transform="{big}"><g transform="{turn}">{ofo_booms(spread=n / EX1_STEPS)}</g></g>',
              D_AT(914, 520))
    # Biosatellite coming home: the capsule nose right (RT) and nose up (RU); its motor the pack, on a stub at the
    # shield's middle; one flame; no straps, no periscope.
    small = lambda body: re.sub(r'stroke-width="([\d.]+)"', lambda m: f'stroke-width="{float(m.group(1)) * 0.8:.2f}"', body)
    cap = small(bios_capsule())
    layer("bs-r-capsule", RT(cap), RC)
    layer("bs-r-capsule-up", RU(cap), RC)
    motor = small(f'<path d="M954 508 L966 508 L966 516 L954 516 Z" fill="#ffffff" {LINE} stroke-width="2.4"/>'
                  f'<circle cx="960" cy="528" r="13" fill="#ffffff" {LINE} stroke-width="2.6"/>'
                  f'<path d="M954 540 L950 552 L970 552 L966 540 Z" fill="#ffffff" {LINE} stroke-width="2.2"/>')
    layer("bs-r-pack", RT(motor), RT_AT(960, 512))
    layer("bs-r-module", RT(motor), RT_AT(960, 528))
    for n in ("strap-a", "strap-b", "cut-a", "cut-b", "periscope", "flame-2", "flame-3"):
        layer(f"bs-r-{n}", "", RC)
    layer("bs-r-flame-1", RT(f'<path d="M950 552 C948 574 954 596 960 616 C966 596 972 574 970 552 Z" fill="{GOLD_FILL}" '
                             f'stroke="{GOLD}" stroke-width="2" stroke-linejoin="round"/>'), RT_AT(960, 552))
    # The attitude jets, on the bell's shoulders.
    layer("bs-r-jet-l", RT(puff(922, 430, 9)), RT_AT(932, 430))
    layer("bs-r-jet-r", RT(puff(998, 430, 9)), RT_AT(988, 430))
    layer("bs-r-jet-stuck", RT(f'<path d="M986 432 C1016 424 1050 414 1086 406 L1090 422 C1054 428 1018 440 988 448 Z" '
                               f'fill="{GOLD_FILL}" stroke="{GOLD}" stroke-width="2" stroke-linejoin="round"/>'), RC)
    # The break-up: the neck and canister from the bell.
    layer("bs-r-half-nose", RT(small(f'<path d="M927 430 C935 396 948 372 960 372 C972 372 985 396 993 430 Q960 438 927 430 Z" fill="#ffffff" {LINE} stroke-width="3"/>'
                                     f'<rect x="952" y="356" width="16" height="18" rx="2" fill="#ffffff" {LINE} stroke-width="2.2"/>')),
          RT_AT(960, 410))
    layer("bs-r-half-body", RT(small(f'<path d="M915 500 C915 470 919 448 927 430 L993 430 C1001 448 1005 470 1005 500 Z" '
                                     f'fill="#ffffff" {LINE} stroke-width="3"/>'
                                     f'<path d="M915 500 Q960 516 1005 500 Z" fill="url(#hatch)" {LINE} stroke-width="2.6"/>')),
          RT_AT(960, 472))


# Pegasus (2026-10-21, user): it rode to orbit folded inside a boilerplate Apollo service module, under a boilerplate
# command module and escape tower (Apollo's own drawings on the rocket: its launch clips are Apollo's); in orbit the
# boilerplate went off on its own and Pegasus stayed on the S-IV stage, whose two wings of detector panels (14.6 m
# each, 4.3 m wide) a motor unfolded to a 29 m span. In the deployment (pegasus-wings.jsx) the stage keeps it and the
# payload that drifts off is the boilerplate CSM, its service module open at the foot over the folded wings; the wings
# unfold in PG_STEPS drawings each. At the launch's 13.6 px/m (AP_L), true to scale there; on the map at PG_MAP of
# their length so the craft fits its orbit (unconfirmed).
PG_STEPS, PG_MAP = 12, 0.45
PG_H = 4.3


def pegasus_core(k=AP_L):
    """The centre section on the stage's top (500) between the folded wings: a frame PG_H tall."""
    h = PG_H * k
    return (f'<rect x="950" y="{500 - h:.1f}" width="20" height="{h:.1f}" fill="#ffffff" {LINE} stroke-width="2.4"/>'
            f'<path d="M950 {500 - h / 2:.1f} L970 {500 - h / 2:.1f}" fill="none" {LINE} stroke-width="1.4" opacity="0.6"/>'
            f'<path d="M960 {500 - h:.1f} L960 {500 - h - 8:.1f}" fill="none" {LINE} stroke-width="2"/>')


def pegasus_wing(side, spread, k=AP_L, length=1.0):
    """One wing out from the centre section to the side (-1 left, 1 right), opened by spread (0 a folded pack 11 px
    thick, 1 its full 14.6 m times length), its panels across it."""
    h = PG_H * k
    full = 14.6 * k * length
    reach = 11 + (full - 11) * spread
    x0 = 960 + side * 10
    x1 = x0 + side * reach
    panels = max(2, round(7 * spread) + 1)
    lines = "".join(f'<path d="M{x0 + side * reach * j / panels:.1f} {500 - h:.1f} L{x0 + side * reach * j / panels:.1f} 500" '
                    f'fill="none" {LINE} stroke-width="1.4" opacity="0.7"/>' for j in range(1, panels))
    return (f'<path d="M{x0:.1f} {500 - h:.1f} L{x1:.1f} {500 - h:.1f} L{x1:.1f} 500 L{x0:.1f} 500 Z" fill="url(#hatch)" {LINE} '
            f'stroke-width="2.4"/>{lines}'
            f'<path d="M{x0:.1f} {500 - h / 2:.1f} L{x1:.1f} {500 - h / 2:.1f}" fill="none" {LINE} stroke-width="1.2" opacity="0.5"/>')


def pegasus_parts():
    k = AP_L
    h = PG_H * k
    big = f"translate({DX} {DY}) scale({DK}) translate(-850 -520)"
    turn = "translate(-120 20) rotate(90 960 500)"
    stage = board_group("debris-2") + '<path d="M944 621 L976 621 L982 639 L938 639 Z" fill="#ffffff" stroke="#16305c" stroke-width="3"/>'
    folded = pegasus_wing(-1, 0) + pegasus_wing(1, 0) + pegasus_core()
    d = lambda body: f'<g transform="{big}"><g transform="{turn}">{body}</g></g>'
    layer("pg-d-stage", d(stage + folded), D_AT(770, 520))
    # The boilerplate CSM: Apollo's, its service module run down over the folded wings to the stage (no engine bell).
    csm = apollo_csm(500 - h, k)
    csm = csm[csm.index("<rect"):]
    r = 1.95 * k
    shell = f'<rect x="{960 - r:.1f}" y="{500 - h - 2:.1f}" width="{2 * r:.1f}" height="{h + 2:.1f}" fill="#ffffff" {LINE} stroke-width="2.6"/>'
    layer("pg-d-payload", d(shell + csm), D_AT(914, 520))
    for n in range(PG_STEPS + 1):
        for side, name in ((-1, "l"), (1, "r")):
            layer(f"pg-d-wing-{name}-{n}", d(pegasus_wing(side, n / PG_STEPS)), D_AT(770, 520))
    # On the map: the stage nose up, Pegasus open on it.
    body = (stage + pegasus_wing(-1, 1, length=PG_MAP) + pegasus_wing(1, 1, length=PG_MAP) + pegasus_core())
    thick = re.sub(r'stroke-width="[\d.]+"', 'stroke-width="5"', body)
    layer("pg-o-sat", f'<g transform="translate(960 560) scale(0.6) translate(-960 -540)">{thick}</g>', (960, 560))



def apollo_parts():
    small = lambda body: re.sub(r'stroke-width="([\d.]+)"', lambda m: f'stroke-width="{float(m.group(1)) * 0.8:.2f}"', body)
    thick = lambda body: re.sub(r'stroke-width="[\d.]+"', 'stroke-width="5"', body)
    rocket = board_group("rocket")
    nose = rocket.index('<path xmlns="http://www.w3.org/2000/svg" d="M915 440')
    big = f"translate({DX} {DY}) scale({DK}) translate(-850 -520)"
    turn = "translate(-120 20) rotate(90 960 500)"
    k = AP_L
    sla_top = 500 - 8.5 * k
    csm_l = apollo_csm(sla_top, k)
    cm_base = sla_top - 4.6 * k
    on_rocket = apollo_sla(500, k) + csm_l
    layer("ap-rocket", rocket[:nose] + on_rocket + apollo_tower(cm_base, k), (960, 600))
    layer("ap-debris-1", on_rocket + apollo_tower(cm_base, k), (960, 420))
    layer("ap-debris-1-bare", on_rocket, (960, 420))
    layer("ap-a-les", apollo_tower(cm_base, k), (960, 300))
    # Out of the climb: the CSM alone (a flight in Earth orbit), or still on the S-IVB in its adapter (a flight
    # to the Moon, which the S-IVB sends there); on the maps also the CSM with the lander, after the extraction.
    # On the maps the craft is drawn bigger (the CSM 2.2 times) so it reads at the map's 0.6.
    csm_alone = apollo_csm(500, k)
    stack = apollo_sivb(500 - 4.0 * k, k * 0.5) + apollo_sla(500 - 4.0 * k, k * 0.5) + apollo_csm(500 - 8.25 * k, k * 0.5)
    layer("ap-d-payload", f'<g transform="{big}"><g transform="{turn}">{csm_alone}</g></g>', D_AT(914, 520))
    layer("ap-d-payload-stack", f'<g transform="{big}"><g transform="{turn}">{stack}</g></g>', D_AT(914, 520))
    K2 = 2.2 * k
    map_csm = apollo_csm(560, K2 * 0.5)
    map_lm = apollo_csm(560, K2 * 0.5) + apollo_lm_small(560 - (4.6 + 3.2) * K2 * 0.5, K2 * 0.5)
    map_stack = (apollo_sivb(560 - 1.5 * K2, K2 * 0.25) + apollo_sla(560 - 1.5 * K2, K2 * 0.25)
                 + apollo_csm(560 - 1.5 * K2 - 8.5 * K2 * 0.25, K2 * 0.25))
    for name, body in (("ap-o-sat", map_csm), ("ap-o-sat-b", map_csm), ("ap-o-sat-lm", map_lm), ("ap-o-sat-stack", map_stack)):
        layer(name, f'<g transform="translate(960 560) scale(0.6) translate(-960 -426)">{thick(body)}</g>', (960, 560))
    # The coasts' map probe (moon-coast.jsx): the craft small at the Sun's place, nose up.
    cx, cy = SUN
    for name, body in (("ap-m-probe", map_lm), ("ap-m-probe-csm", map_csm)):
        layer(name, f'<g transform="translate({cx} {cy}) scale(0.32) translate(-960 -500)">{thick(body)}</g>', SUN)
    # Coming home (31 px/m): the command module where the generic capsule is (its base at 500, 121 across); the
    # service module behind it the pack (the burn's) and the module (the separation's, about its middle); one
    # engine; no straps; the parachutes three mains.
    K = 31
    full = apollo_csm(500 + 4.6 * K, K)
    cut = full.index(f'<path d="M{960 - 1.95 * K:.1f} {500:.1f} L')
    sm, cmd = small(full[:cut]), small(full[cut:])
    layer("ap-r-capsule", RT(cmd), RC)
    layer("ap-r-capsule-up", RU(cmd), RC)
    layer("ap-r-periscope", "", RC)
    layer("ap-r-pack", RT(sm), RT_AT(960, 576))
    layer("ap-r-module", RT(sm), RT_AT(960, 576))
    for n in ("strap-a", "strap-b", "cut-a", "cut-b"):
        layer(f"ap-r-{n}", "", RC)
    layer("ap-r-pyro", RT("".join(f'<circle cx="{x}" cy="502" r="6" fill="{GOLD}"/>' for x in (910, 960, 1010))), RT_AT(960, 502))
    exit_y = 500 + (4.6 + 2.8) * K
    layer("ap-r-flame-1", RT(f'<path d="M928 {exit_y:.0f} C924 {exit_y + 30:.0f} 944 {exit_y + 64:.0f} 960 {exit_y + 96:.0f} '
                             f'C976 {exit_y + 64:.0f} 996 {exit_y + 30:.0f} 992 {exit_y:.0f} Z" fill="{GOLD_FILL}" stroke="{GOLD}" '
                             f'stroke-width="2" stroke-linejoin="round"/>'), RT_AT(960, exit_y))
    layer("ap-r-flame-2", "", RT_AT(960, exit_y))
    layer("ap-r-flame-3", "", RT_AT(960, exit_y))
    quad = 500 + 0.35 * 4.6 * K
    layer("ap-r-jet-l", RT(puff(886, quad, 10)), RT_AT(896, quad))
    layer("ap-r-jet-r", RT(puff(1034, quad, 10)), RT_AT(1024, quad))
    layer("ap-r-jet-stuck", RT(f'<path d="M1030 {quad - 4:.0f} C1060 {quad - 12:.0f} 1094 {quad - 22:.0f} 1130 {quad - 30:.0f} '
                               f'L1134 {quad - 14:.0f} C1098 {quad - 8:.0f} 1062 {quad + 4:.0f} 1032 {quad + 12:.0f} Z" '
                               f'fill="{GOLD_FILL}" stroke="{GOLD}" stroke-width="2" stroke-linejoin="round"/>'), RC)
    layer("ap-r-glow-side", RT(f'<path d="M972 410 C996 440 1016 470 1024 500 C1032 466 1006 434 972 410 Z" fill="{GOLD_FILL}" '
                               f'stroke="{GOLD}" stroke-width="2" stroke-linejoin="round"/>'), RC)
    layer("ap-r-half-nose", RT(small(f'<path d="M951 401 L969 401 L996 450 L924 450 Z" fill="#ffffff" {LINE} stroke-width="3"/>')),
          RT_AT(960, 410))
    layer("ap-r-half-body", RT(small(f'<path d="M922 454 L998 454 L1020 500 L900 500 Z" fill="#ffffff" {LINE} stroke-width="3"/>')),
          RT_AT(960, 472))
    # Three mains (Apollo landed under three; Apollo 15 under two, one having collapsed): the glitch's half-open
    # main is two full and one deflated.
    top = RC[1] - RK * (426 - 352)
    def canopy(cx, dome_y, w_, h):
        return (f'<path d="M{cx - w_ / 2} {dome_y} C{cx - w_ / 2} {dome_y - h} {cx + w_ / 2} {dome_y - h} {cx + w_ / 2} {dome_y} '
                f'C{cx + w_ / 4} {dome_y - h * 0.18} {cx - w_ / 4} {dome_y - h * 0.18} {cx - w_ / 2} {dome_y} Z" fill="#ffffff" {LINE} '
                f'stroke-width="4"/>'
                + "".join(f'<path d="M{cx + kk * w_ / 2} {dome_y} L960 {top}" fill="none" {LINE} stroke-width="1.6"/>'
                          for kk in (-1, 1)))
    three = canopy(760, top - 190, 200, 120) + canopy(1160, top - 190, 200, 120) + canopy(960, top - 230, 210, 130)
    layer("ap-r-main", three, (960, top))
    limp = (f'<path d="M760 {top - 170} C752 {top - 230} 772 {top - 260} 760 {top - 300} C774 {top - 270} 770 {top - 220} 780 {top - 170} Z" '
            f'fill="#ffffff" {LINE} stroke-width="3"/><path d="M770 {top - 170} L960 {top}" fill="none" {LINE} stroke-width="1.6"/>')
    layer("ap-r-main-half", limp + canopy(1160, top - 190, 200, 120) + canopy(960, top - 230, 210, 130), (960, top))


# --- Apollo's lunar module (2026-10-18, user): in the lander's frame (the generic one's place and size: the
# descent stage 4.2 m across between x 898 and 1022, the legs to their pads at 878, the ascent stage on top),
# so the landing's and the moonwalk's timings fit it. The descent stage in its gold foil, two of its four legs
# seen; the ascent stage faceted, its window, the docking tunnel on top, the rendezvous radar and the RCS quads.


def lm_descent():
    legs = (f'<path d="M906 770 L820 876 M1014 770 L1100 876 M914 790 L850 852 M1006 790 L1070 852" fill="none" '
            f'{LINE} stroke-width="4"/>'
            f'<ellipse cx="818" cy="878" rx="18" ry="5" fill="#ffffff" {LINE} stroke-width="3"/>'
            f'<ellipse cx="1102" cy="878" rx="18" ry="5" fill="#ffffff" {LINE} stroke-width="3"/>')
    bell = f'<path d="M946 792 L974 792 L984 822 L936 822 Z" fill="#ffffff" {LINE} stroke-width="3"/>'
    stage = (f'<path d="M912 722 L1008 722 L1022 738 L1022 778 L1008 794 L912 794 L898 778 L898 738 Z" fill="{GOLD_FILL}" '
             f'{LINE} stroke-width="3"/>'
             f'<path d="M912 722 L912 794 M1008 722 L1008 794" fill="none" {LINE} stroke-width="1.6" opacity="0.6"/>'
             + "".join(f'<path d="M{x} {y} l9 4 l-6 6 l10 3" fill="none" stroke="{GOLD}" stroke-width="1.6" opacity="0.9"/>'
                       for x, y in ((924, 736), (952, 760), (980, 742), (938, 772), (990, 770))))
    return legs + bell + stage


def lm_ascent():
    return (f'<path d="M914 722 L910 696 L922 668 L944 652 L980 652 L1000 664 L1010 690 L1008 722 Z" fill="#ffffff" {LINE} '
            f'stroke-width="3"/>'
            f'<path d="M926 678 L944 664 L945 690 Z" fill="rgba(22,48,92,0.14)" {LINE} stroke-width="2"/>'
            f'<path d="M914 704 L1008 704" fill="none" {LINE} stroke-width="1.4" opacity="0.6"/>'
            f'<rect x="950" y="640" width="20" height="12" fill="#ffffff" {LINE} stroke-width="2.4"/>'
            f'<path d="M996 660 L1010 642" fill="none" {LINE} stroke-width="2.2"/>'
            f'<circle cx="1013" cy="638" r="7" fill="#ffffff" {LINE} stroke-width="2.2"/>'
            + "".join(f'<rect x="{x}" y="694" width="10" height="10" fill="#ffffff" {LINE} stroke-width="2"/>'
                      f'<path d="M{x + (-4 if x < 960 else 14)} 699 L{x + (0 if x < 960 else 10)} 699" fill="none" {LINE} stroke-width="2"/>'
                      for x in (898, 1014)))


# The Soviet LK (2026-10-19, user; lkl- parts) in the lander's frame, about 40 px/m: the LPU (its landing unit, the
# frame it stands on) and the LVA (the cabin, 2.3 m across, its big window, the passive Kontakt plate on top, and the
# Blok E under it, whose one engine both lands and lifts off, firing down through the LPU). The Earth-orbit tests flew
# without legs (T2K): legs=False. Sizes from the drawings, unconfirmed.
def lk_lpu(legs=False):
    out = ""
    if legs:
        out += (f'<path d="M908 790 L836 874 M1012 790 L1084 874 M922 792 L874 852 M998 792 L1046 852" fill="none" {LINE} '
                f'stroke-width="4"/>'
                f'<ellipse cx="834" cy="876" rx="16" ry="5" fill="#ffffff" {LINE} stroke-width="3"/>'
                f'<ellipse cx="1086" cy="876" rx="16" ry="5" fill="#ffffff" {LINE} stroke-width="3"/>')
    out += (f'<path d="M906 768 L1014 768 L1024 780 L1024 790 L1014 802 L906 802 L896 790 L896 780 Z" fill="url(#hatch)" {LINE} '
            f'stroke-width="3"/>'
            + "".join(f'<circle cx="{x}" cy="812" r="10" fill="#ffffff" {LINE} stroke-width="2.4"/>' for x in (920, 1000)))
    return out


LK_BACKUP = (934, 986)             # the backup engine's two nozzles, either side of the main one


def lk_lva():
    return (f'<path d="M952 772 L948 802 L972 802 L968 772 Z" fill="#ffffff" {LINE} stroke-width="2.6"/>'
            + "".join(f'<path d="M{x - 5} 772 L{x - 7} 792 L{x + 7} 792 L{x + 5} 772 Z" fill="#ffffff" {LINE} stroke-width="2.2"/>'
                      for x in LK_BACKUP)
            +
            f'<rect x="928" y="736" width="64" height="36" fill="#ffffff" {LINE} stroke-width="3"/>'
            f'<path d="M928 754 L992 754" fill="none" {LINE} stroke-width="1.4" opacity="0.6"/>'
            f'<ellipse cx="960" cy="702" rx="48" ry="40" fill="#ffffff" {LINE} stroke-width="3"/>'
            f'<ellipse cx="934" cy="696" rx="13" ry="16" fill="rgba(22,48,92,0.14)" {LINE} stroke-width="2.2"/>'
            f'<path d="M952 662 L952 654 L968 654 L968 662" fill="none" {LINE} stroke-width="2.4"/>'
            f'<rect x="938" y="648" width="44" height="6" rx="2" fill="#ffffff" {LINE} stroke-width="2.4"/>'
            f'<path d="M996 680 L1012 660" fill="none" {LINE} stroke-width="2.2"/>'
            + "".join(f'<rect x="{x}" y="716" width="10" height="10" fill="#ffffff" {LINE} stroke-width="2"/>' for x in (900, 1010)))


# The LOK and the LK in the spacewalk scene (2026-10-19; world px, 75 px/m, the axis at y 470): the LOK nose right with
# its orbital module's hatch where the scene's is (836 to 926 at 380), docked probe to plate with the LK, whose cabin
# has its hatch on top LK_HATCH_X across; the way back is the same picture mirrored about LK_MIRROR (the ascent stage
# left, the LOK right). crew-transfer.jsx reads the hatches' distance and the rail's end from lok-transfer.jsx.
LKW = 75 / 40                      # the lander's frame (about 40 px/m) to the spacewalk scene's
LK_DOCK_X = 881 + (2.98 / 2 + 0.45) * 75   # where the LOK's probe meets the LK's plate (the orbital module's middle
                                           # under the hatch)
LK_HATCH_X = LK_DOCK_X + (702 - 648) * LKW
LK_MIRROR = (881 + LK_HATCH_X) / 2


def lok_spacewalk(tip_x):
    """The LOK nose right, its probe's tip at tip_x."""
    k = 75
    lok = soyuz_stack(0, k, panels=False, sm_h=LOK_SM)
    # Its lines as heavy as the LK's beside it (drawn at the lander's frame, LKW times up).
    lok = re.sub(r'stroke-width="([\d.]+)"', lambda m: f'stroke-width="{float(m.group(1)) * 1.7:.2f}"', lok)
    return f'<g transform="translate({tip_x} 470) rotate(90) translate(-960 0)">{lok}</g>'


def lk_spacewalk(plate_x, lpu=True):
    """The LK nose left (its plate at plate_x), or the ascent stage alone."""
    body = (lk_lpu() if lpu else "") + lk_lva()
    return f'<g transform="translate({plate_x} 470) scale({LKW}) rotate(-90) translate(-960 -648)">{body}</g>'


def soyuz_spacewalk(tip_x):
    """The 7K-OK nose right, its probe's tip at tip_x, wings out."""
    k = 75
    ok = soyuz_stack(0, k)
    ok = re.sub(r'stroke-width="([\d.]+)"', lambda m: f'stroke-width="{float(m.group(1)) * 1.7:.2f}"', ok)
    return f'<g transform="translate({tip_x} 470) rotate(90) translate(-960 0)">{ok}</g>'


def soyuz_ok_eva_parts():
    x0, x1, y = HATCH
    one = soyuz_spacewalk(LK_DOCK_X)
    layer("sy-e-capsule", one, RC)
    layer("sy-e-capsule-pair", one + f'<g transform="translate({2 * LK_DOCK_X:.1f} 0) scale(-1 1)">{one}</g>', RC)
    layer("sy-e-box", "", (975, 360))
    layer("sy-e-blank", "", RC)
    hb = 2 * LK_DOCK_X - (x0 + x1) / 2
    m0 = hb - (x1 - x0) / 2
    layer("sy-e-hole-b", f'<rect x="{m0 + 4:.1f}" y="{y - 2}" width="{x1 - x0 - 8}" height="10" fill="{NAVY}"/>', (m0, y))
    layer("sy-e-hatch-b", f'<rect x="{m0:.1f}" y="{y - 8}" width="{x1 - x0}" height="10" rx="3" fill="#ffffff" {LINE} '
                          f'stroke-width="3"/>', (m0, y))
    layer("sy-e-rail", f'<path d="M944 352 L{hb - 30:.1f} 352 M960 352 L960 384 M{hb - 44:.1f} 352 L{hb - 44:.1f} 384" '
                       f'fill="none" {LINE} stroke-width="4"/>', (960, 352))
    print("soyuz transfer: over", round(hb - (x0 + x1) / 2, 1), "rail end", round(hb - 85, 1), "middle", round(((x0 + x1) / 2 + hb) / 2, 1))


def lok_eva_parts():
    soyuz_ok_eva_parts()
    x0, x1, y = HATCH
    m0 = LK_HATCH_X - (x1 - x0) / 2
    lok = lok_spacewalk(LK_DOCK_X)
    layer("lok-e-capsule", lok, RC)
    layer("lok-e-capsule-b", lk_spacewalk(LK_DOCK_X), (1296, 470))
    layer("lok-e-collar", "", (LK_DOCK_X, 470))
    layer("lok-e-box", "", (975, 360))
    layer("lok-e-hole-b", f'<rect x="{m0 + 4:.1f}" y="{y - 2}" width="{x1 - x0 - 8}" height="10" fill="{NAVY}"/>', (m0, y))
    layer("lok-e-hatch-b", f'<rect x="{m0:.1f}" y="{y - 8}" width="{x1 - x0}" height="10" rx="3" fill="#ffffff" {LINE} '
                           f'stroke-width="3"/>', (m0, y))
    rail_end = LK_HATCH_X - 30
    layer("lok-e-rail", f'<path d="M944 352 L{rail_end:.1f} 352 M960 352 L960 384 M{rail_end - 14:.1f} 352 L{rail_end - 14:.1f} 384" '
                        f'fill="none" {LINE} stroke-width="4"/>', (960, 352))
    # The lone spacewalk's handrail: from the hatch along the orbital module towards its nose, where the LK would dock.
    layer("lok-e-rail-solo", f'<path d="M944 352 L{LK_DOCK_X - 20} 352 M960 352 L960 384 M{LK_DOCK_X - 34} 352 '
                             f'L{LK_DOCK_X - 34} 392" fill="none" {LINE} stroke-width="4"/>', (960, 352))
    # The way back, mirrored: the ascent stage on the left, its hatch the scene's, the LOK on the right.
    back = lambda body: f'<g transform="translate({2 * LK_MIRROR:.1f} 0) scale(-1 1)">{body}</g>'
    layer("lkl-e-capsule", back(lk_spacewalk(LK_DOCK_X, lpu=False)), RC)
    layer("lkl-e-capsule-b", back(lok), (1296, 470))


LK_H = (822 - 648) / 40           # the LK's height in metres, its plate to its LPU's tanks (the lander's frame)


def lk_up(base_y, k):
    """The LK nose up at k px/m, its foot at base_y."""
    return f'<g transform="translate(960 {base_y:.1f}) scale({k / 40:.4f}) translate(-960 -822)">{lk_lpu()}{lk_lva()}</g>'


def lk_down(plate_y, k):
    """The LK upside down at k px/m, its plate at plate_y (docked on a craft below it)."""
    return f'<g transform="translate(960 {plate_y:.1f}) scale({k / 40:.4f} {-k / 40:.4f}) translate(-960 -648)">{lk_lpu()}{lk_lva()}</g>'


def lok_lk_parts():
    """The crewed lander test's LOK and LK together (2026-10-19, user): as the N1 stacked them, the Blok D, the LK on
    it and the LOK on the LK (the third stage's burn, and leaving the Blok D), and docked, the LOK's probe in the LK's
    plate (in orbit, after a turn-around and docking no step shows)."""
    lok_l = 0.45 + 2.98 + 2.24 + LOK_SM
    thick = lambda body: re.sub(r'stroke-width="[\d.]+"', 'stroke-width="5"', body)
    def n1(bottom, k, stage):
        y = bottom
        out = ""
        if stage:
            y -= BLOK_D_H * k
            out += blok_d(y, k)
        out = lk_up(y, k) + out
        plate = y - LK_H * k
        return soyuz_stack(plate - 0.3 * k - lok_l * k, k, panels=False, sm_h=LOK_SM) + out, (bottom + plate - lok_l * k) / 2
    k = 33.8 * 0.6
    body, mid = n1(500, k, True)
    layer("lok-o-sat-stage-lk", f'<g transform="translate(960 560) scale(0.6) translate(-960 {-mid:.1f})">{thick(body)}</g>', (960, 560))
    top = 500 - lok_l * k
    docked = soyuz_stack(top, k, panels=False, sm_h=LOK_SM) + lk_down(top, k)
    mid = (500 + top - LK_H * k) / 2
    layer("lok-o-sat-lk", f'<g transform="translate(960 560) scale(0.6) translate(-960 {-mid:.1f})">{thick(docked)}</g>', (960, 560))
    big = f"translate({DX} {DY}) scale({DK}) translate(-850 -520)"
    turn = "translate(-120 20) rotate(90 960 500)"
    KL = 33.8 * (0.45 + 2.98 + 2.24 + 2.26) / lok_l * 0.75
    pair, _ = n1(500, KL, False)
    layer("lok-d-payload-lk", f'<g transform="{big}"><g transform="{turn}">{pair}</g></g>', D_AT(914, 520))
    layer("lok-d-stage-lk", f'<g transform="{big}"><g transform="{turn}">{blok_d(500, KL)}</g></g>', D_AT(770, 520))


def lm_parts():
    lok_eva_parts()
    lok_lk_parts()
    # The LK (2026-10-19): its plume and its ascent flame both from the Blok E's nozzle (802).
    layer("lkl-n-descent", lk_lpu(), LC)
    layer("lkl-n-ascent", lk_lva(), LC)
    lk_flame = (f'<path d="M948 802 C950 822 955 840 960 858 C965 840 970 822 972 802 Z" fill="{GOLD_FILL}" '
                f'stroke="{GOLD}" stroke-width="3" stroke-linejoin="round"/>')
    layer("lkl-n-flame-up", lk_flame, (960, 802))
    # The backup engine's test (the crewed test's second): its two plumes, either side of the main nozzle.
    layer("lkl-n-plume-backup", "".join(f'<path d="M{x - 9} 792 C{x - 8} 818 {x - 4} 846 {x} 872 C{x + 4} 846 {x + 8} 818 {x + 9} 792 Z" '
                                        f'fill="{GOLD_FILL}" stroke="{GOLD}" stroke-width="3" stroke-linejoin="round"/>'
                                        for x in LK_BACKUP), (960, 792))
    main_plume = (f'<path d="M944 802 C946 830 952 860 960 890 C968 860 974 830 976 802 Z" fill="{GOLD_FILL}" '
                  f'stroke="{GOLD}" stroke-width="3" stroke-linejoin="round"/>')
    layer("lkl-n-plume", main_plume, (960, 802))
    # The crewed test's first (lk-undock-test.jsx): the main engine and the backup lit together.
    layer("lkl-n-plume-both", main_plume + "".join(
        f'<path d="M{x - 9} 792 C{x - 8} 818 {x - 4} 846 {x} 872 C{x + 4} 846 {x + 8} 818 {x + 9} 792 Z" '
        f'fill="{GOLD_FILL}" stroke="{GOLD}" stroke-width="3" stroke-linejoin="round"/>' for x in LK_BACKUP), (960, 802))
    # The LOK docked on top of it there, nose down, its probe in the LK's plate (648 in the lander's frame, 40 px/m),
    # where the carrier puts the plate on the screen (LC moved by lander-orbit.jsx's LO, scaled by its LK).
    plate_y = LC[1] - 290 + (648 - LC[1]) * 0.8
    lok_down = re.sub(r'stroke-width="([\d.]+)"', lambda m: f'stroke-width="{float(m.group(1)) * 1.15:.2f}"',
                      soyuz_stack(648, 40, panels=False, sm_h=LOK_SM))
    layer("lkl-n-lok", f'<g transform="translate(960 {plate_y:.1f}) scale(0.8) translate(-960 -648)">'
                       f'<g transform="translate(0 1296) scale(1 -1)">{lok_down}</g></g>', (960, plate_y))
    # 39 px/m, as the LOK beside it (the lander's frame is about 40).
    lk_flip = "translate(960 540) scale(0.98 -0.98) translate(-960 -648)"
    lk_tc = (KM[0] + KK * (540 - 426), KM[1])
    layer("lkl-k-target", KT(lk_tc, 90, f'<g transform="{lk_flip}">{lk_lpu()}{lk_lva()}</g>'), KM)
    layer("lkl-k-target-asc", KT(lk_tc, 90, f'<g transform="{lk_flip}">{lk_lva()}</g>'), KM)
    # On the rocket (its own launch, the uncrewed test: no tower), out of it and on the map: nose up, its plate on top.
    lk_rocket = board_group("rocket")
    lk_nose = lk_rocket.index('<path xmlns="http://www.w3.org/2000/svg" d="M915 440')
    lk_up = f'<g transform="translate(960 500) scale(0.75) translate(-960 -810)">{lk_lpu()}{lk_lva()}</g>'
    lk_shroud = (f'<path d="M906 500 L906 400 Q910 330 960 300 Q1010 330 1014 400 L1014 500 Z" fill="#ffffff" {LINE} stroke-width="3"/>'
                 f'<path d="M906 400 L1014 400" fill="none" {LINE} stroke-width="1.4" opacity="0.6"/>')
    layer("lkl-rocket", lk_rocket[:lk_nose] + lk_up + lk_shroud, (960, 600))
    layer("lkl-debris-1", lk_up + lk_shroud, (960, 420))
    layer("lkl-debris-1-bare", lk_up, (960, 420))
    layer("lkl-a-les", lk_shroud, (960, 400))
    big = f"translate({DX} {DY}) scale({DK}) translate(-850 -520)"
    turn = "translate(-120 20) rotate(90 960 500)"
    layer("lkl-d-payload", f'<g transform="{big}"><g transform="{turn}">{lk_up}</g></g>', D_AT(914, 520))
    lk_thick = lambda body: re.sub(r'stroke-width="[\d.]+"', 'stroke-width="5"', body)
    lk_map = f'<g transform="translate(960 560) scale(0.9) translate(-960 -720)">{lk_thick(lk_lpu() + lk_lva())}</g>'
    # Still on the rocket's third stage (the orbit insertion: its last burn of the climb).
    lk_stage = (f'<rect x="922" y="822" width="76" height="90" fill="#ffffff" {LINE} stroke-width="3"/>'
                f'<path d="M922 852 L998 852" fill="none" {LINE} stroke-width="1.4" opacity="0.6"/>'
                f'<path d="M948 912 L942 930 L978 930 L972 912 Z" fill="#ffffff" {LINE} stroke-width="2.4"/>')
    layer("lkl-o-sat-stage", f'<g transform="translate(960 560) scale(0.75) translate(-960 -790)">'
                             f'{lk_thick(lk_stage + lk_lpu() + lk_lva())}</g>', (960, 560))
    lk_map_asc = f'<g transform="translate(960 560) scale(0.9) translate(-960 -720)">{lk_thick(lk_lva())}</g>'
    layer("lkl-o-sat", lk_map, (960, 560))
    layer("lkl-o-sat-asc", lk_map_asc, (960, 560))
    # In the lander's scenes (the ap- skin: n-descent, n-ascent; the plumes, the ladder and the dust are the
    # generic ones, in the same places).
    layer("ap-n-descent", lm_descent(), LC)
    layer("ap-n-ascent", lm_ascent(), LC)
    # In the docking close-up (39 px/m, 1.32 times the lander's frame): upside down, its tunnel's top (640) at
    # the target's mouth (540), the rest above; whole, or the ascent stage alone (after the Moon).
    flip = "translate(960 540) scale(1.32 -1.32) translate(-960 -640)"
    tc = (KM[0] + KK * (540 - 426), KM[1])
    layer("ap-k-target", KT(tc, 90, f'<g transform="{flip}">{lm_descent()}{lm_ascent()}</g>'), KM)
    layer("ap-k-target-asc", KT(tc, 90, f'<g transform="{flip}">{lm_ascent()}</g>'), KM)
    # The chaser: the CSM nose right, its probe's tip at the generic capsule's nose (352).
    S = 39
    y0 = 352 + (4.6 + 3.2 + 0.6) * S
    layer("ap-k-chaser", KT(KC, 90, apollo_csm(y0, S)), KC)
    # The other CSM of a rendezvous (2026-10-20, user: the rendezvous flight's spacecraft B), nose left, facing the chaser.
    layer("ap-k-other", KT((1150, 470), -90, apollo_csm(y0, S)), (1150, 470))
    quad = y0 - 0.65 * 4.6 * S
    sh_t, sh_b = K_AT(KC, 960 - 1.95 * S - 10, quad, 90), K_AT(KC, 960 + 1.95 * S + 10, quad, 90)
    layer("ap-k-brake", puff(sh_t[0] + 18, sh_t[1] - 6, 9) + puff(sh_b[0] + 18, sh_b[1] + 6, 9), KC)
    b_t, b_b = K_AT(KC, 960 - 1.95 * S, y0, 90), K_AT(KC, 960 + 1.95 * S, y0, 90)
    layer("ap-k-push", puff(b_t[0] - 16, b_t[1], 9) + puff(b_b[0] - 16, b_b[1], 9), KC)
    side = K_AT(KC, 960 - 1.95 * S - 6, quad, 90)
    layer("ap-k-side", puff(side[0], side[1] - 16, 10), side)
    root = K_AT(KC, 960 - 1.95 * S, quad + 20, 90)
    layer("ap-k-stuck", f'<path d="M{root[0] - 6:.1f} {root[1]:.1f} C{root[0] - 14:.1f} {root[1] - 40:.1f} {root[0] - 20:.1f} '
                        f'{root[1] - 80:.1f} {root[0] - 28:.1f} {root[1] - 120:.1f} L{root[0] - 12:.1f} {root[1] - 122:.1f} '
                        f'C{root[0] - 6:.1f} {root[1] - 80:.1f} {root[0] + 2:.1f} {root[1] - 40:.1f} {root[0] + 6:.1f} {root[1]:.1f} Z" '
                        f'fill="{GOLD_FILL}" stroke="{GOLD}" stroke-width="2" stroke-linejoin="round"/>', KC)
    # The S-IVB in the transposition (the generic stage's box): its bands, the auxiliary propulsion modules at
    # its aft end, the J-2's bell.
    layer("ap-t-stage", f'<rect x="1080" y="400" width="420" height="140" fill="#ffffff" {LINE} stroke-width="3"/>'
                        f'<path d="M1180 400 L1180 540 M1420 400 L1420 540" fill="none" {LINE} stroke-width="2"/>'
                        f'<path d="M1280 400 L1280 540" fill="none" {LINE} stroke-width="1.2" opacity="0.5"/>'
                        f'<rect x="1440" y="386" width="40" height="14" rx="2" fill="#ffffff" {LINE} stroke-width="2.2"/>'
                        f'<rect x="1440" y="540" width="40" height="14" rx="2" fill="#ffffff" {LINE} stroke-width="2.2"/>'
                        f'<path d="M1500 440 L1550 414 L1550 526 L1500 500 Z" fill="#ffffff" {LINE} stroke-width="3"/>',
          (1290, 470))
    # The lunar ascent's map: the ascent stage alone climbing to the CSM (o-sat-b), bigger so it reads.
    body = re.sub(r'stroke-width="[\d.]+"', 'stroke-width="5"', lm_ascent())
    layer("ap-o-sat-asc", f'<g transform="translate(960 560) scale(1.1) translate(-960 -681)">{body}</g>', (960, 560))
    # Apollo 5 (2026-10-20, user; a5- parts): the first lunar module, LM-1, flew alone on a Saturn IB, without legs, inside
    # its adapter under a nose cone and no CSM or escape tower (NASA). In orbit the cone came off, the adapter's four
    # panels opened, and the LM backed out on its own thrusters. On the pad and in the climb the adapter and the cone (the
    # tower's part, a-les, empty: the cone stayed on into orbit); on the insertion's map the S-IVB, adapter and cone; in
    # the deployment the S-IVB with two of its panels seen open, the LM leaving it; on the maps the LM alone; in the
    # lander's scenes the LM without legs.
    rocket = board_group("rocket")
    nose = rocket.index('<path xmlns="http://www.w3.org/2000/svg" d="M915 440')
    big = f"translate({DX} {DY}) scale({DK}) translate(-850 -520)"
    turn = "translate(-120 20) rotate(90 960 500)"
    thick = lambda body: re.sub(r'stroke-width="[\d.]+"', 'stroke-width="5"', body)
    k = AP_L
    sla_top = 500 - 8.5 * k

    def cone(top, kk):
        return (f'<path d="M{960 - 1.95 * kk:.1f} {top:.1f} C{960 - 1.95 * kk:.1f} {top - 1.6 * kk:.1f} {960 - 0.6 * kk:.1f} '
                f'{top - 2.7 * kk:.1f} 960 {top - 2.9 * kk:.1f} C{960 + 0.6 * kk:.1f} {top - 2.7 * kk:.1f} {960 + 1.95 * kk:.1f} '
                f'{top - 1.6 * kk:.1f} {960 + 1.95 * kk:.1f} {top:.1f} Z" fill="#ffffff" {LINE} stroke-width="2.6"/>')
    a5_launch = apollo_sla(500, k) + cone(sla_top, k)
    layer("a5-rocket", rocket[:nose] + a5_launch, (960, 600))
    layer("a5-debris-1", a5_launch, (960, 440))
    layer("a5-a-les", "", (960, 300))
    K2 = 2.2 * k * 0.25
    a5_map = apollo_sivb(560 - 1.5 * 2.2 * k, K2) + apollo_sla(560 - 1.5 * 2.2 * k, K2) + cone(560 - 1.5 * 2.2 * k - 8.5 * K2, K2)
    layer("a5-o-sat-stack", f'<g transform="translate(960 560) scale(0.6) translate(-960 -426)">{thick(a5_map)}</g>', (960, 560))
    # The LM without legs, in the lander's frame; in the launch's (k px/m), its descent stage's foot (the bell) at 500.
    desc = lm_descent()
    no_legs = desc[desc.index('<path d="M946 792 L974 792'):]
    lm_up = lambda base, kk: (f'<g transform="translate(960 {base:.1f}) scale({kk / 40:.4f}) translate(-960 -822)">'
                              f'{no_legs}{lm_ascent()}</g>')
    layer("a5-d-payload", f'<g transform="{big}"><g transform="{turn}">{lm_up(500, k)}</g></g>', D_AT(914, 520))
    panels = "".join(f'<path d="M{960 + s_ * 3.3 * k:.1f} 500 L{960 + s_ * (3.3 + 4.5) * k:.1f} {500 - 4.5 * k:.1f} '
                     f'L{960 + s_ * (3.3 + 3.6) * k:.1f} {500 - 5.4 * k:.1f} L{960 + s_ * 2.6 * k:.1f} {500 - 0.7 * k:.1f} Z" '
                     f'fill="#ffffff" {LINE} stroke-width="2.6"/>' for s_ in (-1, 1))
    layer("a5-d-stage", f'<g transform="{big}"><g transform="{turn}">{apollo_sivb(500, k) + panels}</g></g>', D_AT(770, 520))
    lm_map = re.sub(r'stroke-width="[\d.]+"', 'stroke-width="5"', no_legs + lm_ascent())
    layer("a5-o-sat", f'<g transform="translate(960 560) scale(0.9) translate(-960 -735)">{lm_map}</g>', (960, 560))
    layer("a5-n-descent", no_legs, LC)
    layer("a5-n-ascent", lm_ascent(), LC)
    layer("a5-none", "", LC)
    # Apollo's Earth-orbit tests in one scale (2026-10-20, user). The transposition and the extraction (apollo.jsx) drew the
    # lander at 0.42 of the lander's frame and the S-IVB 3.6 m across, against the CSM at 39 px/m; now the lander is at the
    # docking close-up's 1.32 (the CSM's scale), its legs folded as they rode in the adapter, and the S-IVB 6.6 m across,
    # its front past the lander's feet, the adapter's panels from it to the service module's base.
    sk = 39
    st_x0, st_half = 1200, 3.3 * sk
    layer("ap-t-stage", f'<rect x="{st_x0}" y="{470 - st_half:.1f}" width="{17.8 * sk:.1f}" height="{2 * st_half:.1f}" fill="#ffffff" '
                        f'{LINE} stroke-width="3"/>'
                        + "".join(f'<path d="M{st_x0 + f * 17.8 * sk:.1f} {470 - st_half:.1f} L{st_x0 + f * 17.8 * sk:.1f} '
                                  f'{470 + st_half:.1f}" fill="none" {LINE} stroke-width="2" opacity="0.5"/>' for f in (0.25, 0.7)),
          (st_x0 + 8.9 * sk, 470))
    sm_half = 1.95 * sk
    for name, sgn in (("ap-t-panel-top", -1), ("ap-t-panel-bottom", 1)):
        y_st, y_sm = 470 + sgn * st_half, 470 + sgn * sm_half
        layer(name, f'<path d="M{st_x0} {y_st:.1f} L948 {y_sm:.1f} L948 {y_sm - sgn * 8:.1f} L{st_x0} {y_st - sgn * 10:.1f} Z" '
                    f'fill="#ffffff" {LINE} stroke-width="3"/>', (st_x0, y_st - sgn * 5))
    layer("ap-t-flash-sep", "".join(f'<circle cx="948" cy="{470 + sg * sm_half:.1f}" r="9" fill="{GOLD}"/>' for sg in (-1, 1)), (948, 470))
    layer("ap-t-flash-lm", "".join(f'<circle cx="{st_x0 - 4}" cy="{470 + sg * 70:.1f}" r="9" fill="{GOLD}"/>' for sg in (-1, 1)),
          (st_x0 - 4, 470))
    # The lander with its legs folded, in the lander's frame: the struts up along the descent stage, the pads at its top.
    full = lm_descent()
    folded_legs = (f'<path d="M906 790 L884 730 M1014 790 L1036 730" fill="none" {LINE} stroke-width="4"/>'
                   f'<ellipse cx="882" cy="726" rx="12" ry="5" fill="#ffffff" {LINE} stroke-width="3"/>'
                   f'<ellipse cx="1038" cy="726" rx="12" ry="5" fill="#ffffff" {LINE} stroke-width="3"/>')
    folded = folded_legs + full[full.index('<path d="M946 792 L974 792'):]
    layer("ap-n-descent-folded", folded, LC)
    # The first docking (after the transposition): the lander still on the S-IVB, its legs folded, the stage beyond it, in
    # the docking close-up's frame (ap-k-target's: the lander upside down, its tunnel's top at the target's mouth).
    flip = "translate(960 540) scale(1.32 -1.32) translate(-960 -640)"
    sivb_lf = 1 / 1.32 * sk                                   # the S-IVB in the lander's frame, px/m
    sivb = (f'<rect x="{960 - 3.3 * sivb_lf:.1f}" y="826" width="{6.6 * sivb_lf:.1f}" height="{17.8 * sivb_lf:.1f}" fill="#ffffff" '
            f'{LINE} stroke-width="2.4"/>'
            + "".join(f'<path d="M{960 - 3.3 * sivb_lf:.1f} {826 + f * 17.8 * sivb_lf:.1f} L{960 + 3.3 * sivb_lf:.1f} '
                      f'{826 + f * 17.8 * sivb_lf:.1f}" fill="none" {LINE} stroke-width="1.6" opacity="0.5"/>' for f in (0.25, 0.7)))
    tc = (KM[0] + KK * (540 - 426), KM[1])
    layer("ap-k-target-sivb", KT(tc, 90, f'<g transform="{flip}">{sivb}{folded}{lm_ascent()}</g>'), KM)
    # The high-orbit test's burn to the elliptical orbit: the S-IVB still under the CSM (ap-o-sat-stack), its flame at the
    # J-2's bell.
    K2 = 2.2 * AP_L
    bell = 560 - 1.5 * K2 + 10.6 * K2 * 0.25
    dy = 560 + 0.6 * (bell - 426) - 600
    layer("ap-o-streak-stack", f'<g transform="translate(0 {dy:.1f})"><path d="M944 600 C942 660 952 740 960 790 C968 740 978 660 976 600 Z" '
                               f'fill="{GOLD_FILL}" stroke="{GOLD}" stroke-width="3" stroke-linejoin="round"/></g>', (960, 560))
    # The spacewalk scene (nose right, the axis at 470, 68 px/m): the CSM with its command module's side hatch on the
    # cone where the scene's hatch is (836 to 926, its middle at 380), sloped as the cone is; no package on the hull;
    # a handrail on posts from the hatch along the cone towards the nose; for the lunar module tests the LM docked on
    # the nose, legs out (Apollo 9's picture).
    E = 68
    slope = math.degrees(math.atan2(1.65, 3.2))
    xb = 881 - 1.2 * E                                        # the command module's base, so the hatch's middle is at 881
    base_y = 500 - 4.6 * E
    csm = re.sub(r'stroke-width="([\d.]+)"', lambda m: f'stroke-width="{float(m.group(1)) * 1.4:.2f}"', apollo_csm(500, E))
    lm = re.sub(r'stroke-width="([\d.]+)"', lambda m: f'stroke-width="{float(m.group(1)) * 1.4:.2f}"',
                apollo_lm_small(base_y - 3.2 * E, E))
    on_axis = lambda body: f'<g transform="translate({xb:.1f} 470) rotate(90) translate(-960 {-base_y:.1f})">{body}</g>'
    layer("ap-e-capsule", on_axis(csm), RC)
    layer("ap-e-capsule-lm", on_axis(csm + lm), RC)
    x0, x1, hy = HATCH
    xm = (x0 + x1) / 2
    hole = (f'<g transform="rotate({slope:.2f} {xm} {hy})"><rect x="{x0 + 4}" y="{hy - 2}" width="{x1 - x0 - 8}" height="10" '
            f'fill="{NAVY}"/></g>')
    hatch = (f'<g transform="rotate({slope:.2f} {xm} {hy})"><rect x="{x0}" y="{hy - 8}" width="{x1 - x0}" height="10" rx="3" '
             f'fill="#ffffff" {LINE} stroke-width="3"/></g>')
    a_ = math.radians(slope)
    hinge = (xm + (x1 - xm) * math.cos(a_), hy + (x1 - xm) * math.sin(a_))
    layer("ap-e-hole", hole, hinge)
    layer("ap-e-hatch", hatch, hinge)
    layer("ap-e-box", "", (975, 360))
    surf = lambda x: hy + (x - xm) * math.tan(a_)
    layer("ap-e-rail-solo", f'<path d="M944 352 L1012 352 M960 352 L960 {surf(960):.1f} M998 352 L998 {surf(998):.1f}" fill="none" '
                            f'{LINE} stroke-width="4"/>', (960, 352))


# --- The LK-700 (2026-10-19, user; l7- parts): Chelomei's direct-ascent lander, as our blueprint draws it. Nose up,
# 2.7 m across (Wikipedia's span): the return capsule (LK-1's line) a cylinder with two portholes and the hatch under a
# cone with a cap and a small dish on its tip; the return stage (Block IV) a banded cylinder; the landing stage (Block
# III) with its six legs, folded until the descent (two seen), and its engines under it, a big one and two small.
# The braking stage (Block II) and the trans-lunar stage fly only to the Moon (a later package). Sizes from the
# blueprint, unconfirmed (Wikipedia's 21.2 m and 154 t are the whole complex with its trans-lunar stage).
L7_W, L7_CYL, L7_CONE, L7_RET, L7_LAND = 2.7, 1.4, 1.4, 2.0, 1.4
L7_CAP = 0.3
L7_LEN = L7_LAND + L7_RET + L7_CYL + L7_CONE + L7_CAP


def l7_capsule(base, k, w=1.0, hatch=True, shield=True):
    """The return capsule, its base (heat shield) at y base, k px/m, stroke widths times w; its side hatch (none
    where the spacewalk scene draws the hatch on top), and its heat shield's curve (alone)."""
    sw = lambda x: f'stroke-width="{x * w:.2f}"'
    r, rt = L7_W / 2 * k, 0.45 * k
    y1 = base - L7_CYL * k
    y2 = y1 - L7_CONE * k
    out = (f'<path d="M{960 - r:.1f} {base:.1f} L{960 - r:.1f} {y1:.1f} L{960 - rt:.1f} {y2:.1f} L{960 + rt:.1f} {y2:.1f} '
           f'L{960 + r:.1f} {y1:.1f} L{960 + r:.1f} {base:.1f} Z" fill="#ffffff" {LINE} {sw(2.6)}/>'
           f'<path d="M{960 - r:.1f} {y1:.1f} L{960 + r:.1f} {y1:.1f}" fill="none" {LINE} {sw(1.6)}/>'
           + (f'<path d="M{960 - r:.1f} {base:.1f} Q960 {base + 0.2 * k:.1f} {960 + r:.1f} {base:.1f}" fill="none" {LINE} {sw(2.2)}/>'
              if shield else "")
           # The hatch on the cylinder and the two portholes beside it, two small windows in the cone.
           + (f'<rect x="{960 - 0.3 * k:.1f}" y="{y1 + 0.25 * k:.1f}" width="{0.6 * k:.1f}" height="{0.8 * k:.1f}" '
              f'rx="{0.08 * k:.1f}" fill="none" {LINE} {sw(1.4)} opacity="0.8"/>' if hatch else "")
           + "".join(f'<circle cx="{960 + s * 0.8 * k:.1f}" cy="{y1 + 0.55 * k:.1f}" r="{0.17 * k:.1f}" fill="rgba(22,48,92,0.12)" '
                     f'{LINE} {sw(1.6)}/>' for s in (-1, 1))
           + "".join(f'<circle cx="{960 + s * 0.42 * k:.1f}" cy="{y1 - 0.75 * k:.1f}" r="{0.1 * k:.1f}" fill="rgba(22,48,92,0.12)" '
                     f'{LINE} {sw(1.4)}/>' for s in (-1, 1))
           # The cap on the tip and the dish on its mast.
           + f'<path d="M{960 - rt:.1f} {y2:.1f} Q960 {y2 - L7_CAP * k:.1f} {960 + rt:.1f} {y2:.1f}" fill="#ffffff" {LINE} {sw(2.2)}/>'
           f'<path d="M960 {y2 - 0.15 * k:.1f} L960 {y2 - 0.55 * k:.1f}" fill="none" {LINE} {sw(1.6)}/>'
           f'<path d="M{960 - 0.3 * k:.1f} {y2 - 0.62 * k:.1f} Q960 {y2 - 0.42 * k:.1f} {960 + 0.3 * k:.1f} {y2 - 0.62 * k:.1f}" '
           f'fill="none" {LINE} {sw(1.6)}/>')
    return out


def l7_return(top, k, w=1.0):
    """The return stage (Block IV), its top at y top."""
    sw = lambda x: f'stroke-width="{x * w:.2f}"'
    r, h = L7_W / 2 * k, L7_RET * k
    return (f'<rect x="{960 - r:.1f}" y="{top:.1f}" width="{2 * r:.1f}" height="{h:.1f}" fill="#ffffff" {LINE} {sw(2.6)}/>'
            f'<rect x="{960 - r:.1f}" y="{top + 0.55 * h:.1f}" width="{2 * r:.1f}" height="{0.25 * h:.1f}" fill="{GM_DARK}" '
            f'opacity="0.7" {LINE} {sw(1.4)}/>'
            + "".join(f'<path d="M{960 - r:.1f} {top + f * h:.1f} L{960 + r:.1f} {top + f * h:.1f}" fill="none" {LINE} {sw(1.2)} '
                      f'opacity="0.5"/>' for f in (0.2, 0.92)))


def l7_landing(top, k, w=1.0, legs="folded"):
    """The landing stage (Block III), its top at y top, its engines under it; legs folded along its sides, or none."""
    sw = lambda x: f'stroke-width="{x * w:.2f}"'
    r, h = L7_W / 2 * k, L7_LAND * k
    foot = top + h
    out = "".join(f'<path d="M{x - a * k:.1f} {foot:.1f} L{x - b * k:.1f} {foot + n * k:.1f} L{x + b * k:.1f} {foot + n * k:.1f} '
                  f'L{x + a * k:.1f} {foot:.1f} Z" fill="#ffffff" {LINE} {sw(2.2)}/>'
                  for x, a, b, n in ((960 - 0.75 * k, 0.1, 0.16, 0.3), (960 + 0.75 * k, 0.1, 0.16, 0.3), (960, 0.18, 0.32, 0.45)))
    out += (f'<rect x="{960 - r:.1f}" y="{top:.1f}" width="{2 * r:.1f}" height="{h:.1f}" fill="url(#hatch)" {LINE} {sw(2.6)}/>'
            f'<path d="M{960 - r:.1f} {top + 0.3 * h:.1f} L{960 + r:.1f} {top + 0.3 * h:.1f}" fill="none" {LINE} {sw(1.2)} '
            f'opacity="0.6"/>')
    if legs == "folded":
        out += "".join(f'<path d="M{960 + s * (r + 0.06 * k):.1f} {top + 0.08 * k:.1f} L{960 + s * (r + 0.14 * k):.1f} '
                       f'{foot + 0.25 * k:.1f}" fill="none" {LINE} {sw(2.6)}/>'
                       f'<rect x="{960 + s * (r + 0.14 * k) - 0.18 * k:.1f}" y="{foot + 0.25 * k:.1f}" width="{0.36 * k:.1f}" '
                       f'height="{0.1 * k:.1f}" rx="{0.04 * k:.1f}" fill="#ffffff" {LINE} {sw(1.8)}/>' for s in (-1, 1))
    return out


def l7_stack(foot, k, w=1.0, landing=True, ret=True, hatch=True):
    """The craft nose up, its landing stage's foot (or the return stage's, or the capsule's base) at y foot."""
    y = foot
    out = ""
    if landing:
        y -= L7_LAND * k
        out += l7_landing(y, k, w)
    if ret:
        y -= L7_RET * k
        out = l7_return(y, k, w) + out
    return out + l7_capsule(y, k, w, hatch=hatch, shield=not ret)


def lk700_parts():
    thick = lambda body: re.sub(r'stroke-width="[\d.]+"', 'stroke-width="5"', body)
    small = lambda body: re.sub(r'stroke-width="([\d.]+)"', lambda m: f'stroke-width="{float(m.group(1)) * 0.8:.2f}"', body)
    rocket = board_group("rocket")
    nose = rocket.index('<path xmlns="http://www.w3.org/2000/svg" d="M915 440')
    big = f"translate({DX} {DY}) scale({DK}) translate(-850 -520)"
    turn = "translate(-120 20) rotate(90 960 500)"
    # The launch (33.8 px/m, as Soyuz's: 2.7 m is the rocket's top), under a fairing and escape tower (Soyuz's shape,
    # assumed), which the climb throws off.
    K = 33.8
    craft = l7_stack(500 - 0.25 * K, K)
    layer("l7-rocket", rocket[:nose] + craft + soyuz_fairing(), (960, 600))
    layer("l7-debris-1", craft + soyuz_fairing(), (960, 420))
    layer("l7-debris-1-bare", craft, (960, 420))
    layer("l7-a-les", soyuz_fairing(), (960, 300))
    layer("l7-d-payload", f'<g transform="{big}"><g transform="{turn}">{craft}</g></g>', D_AT(914, 520))
    # On the maps, centred, thick; still on the rocket's top stage for the orbit insertion (the second ascent).
    mid = 500 - L7_LEN * K / 2
    for name in ("l7-o-sat", "l7-o-sat-b"):
        layer(name, f'<g transform="translate(960 560) scale(0.6) translate(-960 {-mid:.1f})">{thick(l7_stack(500, K))}</g>',
              (960, 560))
    stage = (f'<rect x="{960 - 1.45 * K:.1f}" y="{500 + 0.35 * K:.1f}" width="{2.9 * K:.1f}" height="{3.2 * K:.1f}" fill="#ffffff" '
             f'{LINE} stroke-width="3"/>'
             f'<path d="M{960 - 1.45 * K:.1f} {500 + 1.3 * K:.1f} L{960 + 1.45 * K:.1f} {500 + 1.3 * K:.1f}" fill="none" {LINE} '
             f'stroke-width="1.4" opacity="0.6"/>'
             f'<path d="M{960 - 0.4 * K:.1f} {500 + 3.55 * K:.1f} L{960 - 0.6 * K:.1f} {500 + 4.2 * K:.1f} L{960 + 0.6 * K:.1f} '
             f'{500 + 4.2 * K:.1f} L{960 + 0.4 * K:.1f} {500 + 3.55 * K:.1f} Z" fill="#ffffff" {LINE} stroke-width="2.4"/>')
    mid_s = (500 - L7_LEN * K + 500 + 4.2 * K) / 2
    layer("l7-o-sat-stage", f'<g transform="translate(960 560) scale(0.5) translate(-960 {-mid_s:.1f})">'
                            f'{thick(stage + l7_stack(500, K))}</g>', (960, 560))
    # Coming home (50 px/m, the capsule's base at 500, its tip about 350, where the chutes hang): the capsule alone,
    # or still on the return stage (r-capsule-stage: the burn and the jettison); the landing stage the pack (its
    # engines fire the burn), the return stage the module (the separation); the joints' flashes; no straps.
    R = 50
    cap = small(l7_capsule(500, R))
    ret = small(l7_return(500, R))
    cap_on = small(l7_capsule(500, R, shield=False))
    land = small(l7_landing(500 + L7_RET * R, R))
    land_mid = 500 + (L7_RET + L7_LAND / 2) * R
    ret_mid = 500 + L7_RET / 2 * R
    layer("l7-r-capsule", RT(cap), RC)
    layer("l7-r-capsule-up", RU(cap), RC)
    layer("l7-r-capsule-stage", RT(ret + cap_on), RC)
    layer("l7-r-periscope", "", RC)
    layer("l7-r-pack", RT(land), RT_AT(960, land_mid))
    layer("l7-r-module", RT(ret), RT_AT(960, ret_mid))
    for n in ("strap-a", "strap-b"):
        layer(f"l7-r-{n}", "", RC)
    joint = 500 + L7_RET * R
    rr = L7_W / 2 * R
    layer("l7-r-cut-a", RT(f'<circle cx="{960 - rr + 6:.1f}" cy="{joint:.1f}" r="6" fill="{GOLD}"/>'), RT_AT(960 - rr + 6, joint))
    layer("l7-r-cut-b", RT(f'<circle cx="{960 + rr - 6:.1f}" cy="{joint:.1f}" r="6" fill="{GOLD}"/>'), RT_AT(960 + rr - 6, joint))
    layer("l7-r-pyro", RT("".join(f'<circle cx="{x:.1f}" cy="503" r="6" fill="{GOLD}"/>' for x in (960 - rr + 10, 960, 960 + rr - 10))),
          RT_AT(960, 503))
    # Its engines' flames: the two small ones (1, 3) and the big one in the middle (2).
    exit_y = joint + L7_LAND * R
    for i, (x, n, a) in enumerate(((960 - 0.75 * R, 0.3, 0.16), (960, 0.45, 0.32), (960 + 0.75 * R, 0.3, 0.16)), 1):
        y0, hw, ln = exit_y + n * R, a * R, (1.0 if i == 2 else 0.7) * R
        layer(f"l7-r-flame-{i}", RT(f'<path d="M{x - hw:.1f} {y0:.1f} C{x - hw:.1f} {y0 + ln * 0.4:.1f} {x - 2:.1f} {y0 + ln * 0.8:.1f} '
                                    f'{x:.1f} {y0 + ln:.1f} C{x + 2:.1f} {y0 + ln * 0.8:.1f} {x + hw:.1f} {y0 + ln * 0.4:.1f} {x + hw:.1f} '
                                    f'{y0:.1f} Z" fill="{GOLD_FILL}" stroke="{GOLD}" stroke-width="2" stroke-linejoin="round"/>'),
              RT_AT(x, y0))
    # The attitude jets on the capsule's cylinder, a puff each side, and one stuck open.
    jy = 500 - 0.5 * L7_CYL * R
    layer("l7-r-jet-l", RT(puff(960 - rr - 12, jy, 10)), RT_AT(960 - rr - 2, jy))
    layer("l7-r-jet-r", RT(puff(960 + rr + 12, jy, 10)), RT_AT(960 + rr + 2, jy))
    layer("l7-r-jet-stuck", RT(f'<path d="M{960 + rr:.1f} {jy - 4:.1f} C{960 + rr + 30:.1f} {jy - 12:.1f} {960 + rr + 64:.1f} {jy - 22:.1f} '
                               f'{960 + rr + 100:.1f} {jy - 30:.1f} L{960 + rr + 104:.1f} {jy - 14:.1f} C{960 + rr + 68:.1f} {jy - 8:.1f} '
                               f'{960 + rr + 32:.1f} {jy + 4:.1f} {960 + rr + 2:.1f} {jy + 12:.1f} Z" fill="{GOLD_FILL}" stroke="{GOLD}" '
                               f'stroke-width="2" stroke-linejoin="round"/>'), RC)
    layer("l7-r-glow-side", RT(f'<path d="M{960 + 0.45 * R + 4:.1f} {500 - (L7_CYL + L7_CONE) * R:.1f} C{960 + rr + 10:.1f} '
                               f'{500 - L7_CYL * R:.1f} {960 + rr + 12:.1f} 460 {960 + rr + 4:.1f} 500 C{960 + rr + 24:.1f} 460 '
                               f'{960 + rr + 14:.1f} {500 - L7_CYL * R - 10:.1f} {960 + 0.45 * R + 4:.1f} {500 - (L7_CYL + L7_CONE) * R:.1f} Z" '
                               f'fill="{GOLD_FILL}" stroke="{GOLD}" stroke-width="2" stroke-linejoin="round"/>'), RC)
    # The break-up's halves: the cone (with its cap) and the cylinder.
    y1, y2 = 500 - L7_CYL * R, 500 - (L7_CYL + L7_CONE) * R
    layer("l7-r-half-nose", RT(small(f'<path d="M{960 - rr:.1f} {y1:.1f} L{960 - 0.45 * R:.1f} {y2:.1f} Q960 {y2 - L7_CAP * R:.1f} '
                                     f'{960 + 0.45 * R:.1f} {y2:.1f} L{960 + rr:.1f} {y1:.1f} Z" fill="#ffffff" {LINE} stroke-width="3"/>')),
          RT_AT(960, (y1 + y2) / 2))
    layer("l7-r-half-body", RT(small(f'<rect x="{960 - rr:.1f}" y="{y1 + 4:.1f}" width="{2 * rr:.1f}" height="{L7_CYL * R - 4:.1f}" '
                                     f'fill="#ffffff" {LINE} stroke-width="3"/>')), RT_AT(960, (y1 + 500) / 2))
    # The spacewalk scene (nose right, the axis at 470, the hull's top at the hatch's 380: 66.7 px/m): the capsule's
    # cylinder under the hatch (836 to 926), the return and landing stages behind it; a handrail from the hatch along
    # the cone, on posts (the lone spacewalk's middle step, eva-rail.jsx: the figure goes hand over hand to its end).
    E = 90 / (L7_W / 2)
    side = re.sub(r'stroke-width="([\d.]+)"', lambda m: f'stroke-width="{float(m.group(1)) * 1.4:.2f}"', l7_stack(500, E, hatch=False))
    cyl0 = 834
    base_y = 500 - (L7_LAND + L7_RET) * E
    layer("l7-e-capsule", f'<g transform="translate({cyl0} 470) rotate(90) translate(-960 {-base_y:.1f})">{side}</g>', RC)
    layer("l7-e-box", "", (975, 360))
    cone0 = cyl0 + L7_CYL * E
    slope = (L7_W / 2 - 0.45) / L7_CONE
    surf = lambda x: 380 + (x - cone0) * slope
    layer("l7-e-rail-solo", f'<path d="M944 352 L1012 352 M960 352 L960 {surf(960):.1f} M998 352 L998 {surf(998):.1f}" fill="none" '
                            f'{LINE} stroke-width="4"/>', (960, 352))


# --- Project Gemini (2026-10-17): its spacecraft, and the Agena it docked with --------------------------
# The reentry module seen from its back (the side with the hatches): the heat shield, the cabin's cone
# with the two hatches, a window in each, the reentry control (RCS) section, a short cylinder, and the
# rendezvous and recovery (R&R) section, a little tapered, its nose flat (the radar). Dark, as it flew.
# Behind the shield, the white adapter: the retrograde section (the four retros, three seen side on)
# and the equipment section, widening to the rocket's width. Drawn in metres from the shield's centre
# (x out, y up), sx px/m across and sy along; the way home's frame stretches it 9 % along so that the
# module fills the generic capsule's box (base 90 at y 500, top 352): the chutes, the hinges and the
# break-up's seam fit it.
GM_SHIELD, GM_RETRO, GM_EQUIP = 1.145, 1.236, 1.525          # half-widths: the shield, the seam, the base
GM_RETRO_Y, GM_EQUIP_Y = -0.55, -2.29
GM_DARK = "#aab4c6"
GM_R = (960, 500, 90 / 2.29, 148 / 3.45)                     # the way home's frame: cx, base, sx, sy
GM_L = (960, 500, 90 / 3.05, 90 / 3.05 * 1.09)               # on the rocket: the adapter as wide as it


def gm_pt(f, x, y):
    cx, base, sx, sy = f
    return f"{cx + x * sx:.1f} {base - y * sy:.1f}"


def gm_poly(f, pts, fill, w, extra=""):
    return f'<path d="M{" L".join(gm_pt(f, x, y) for x, y in pts)} Z" fill="{fill}" {LINE} stroke-width="{w}"{extra}/>'


def gemini_module(f, k=1.0):
    """The reentry module, stroke widths times k."""
    w = lambda x: f"{x * k:.2f}"
    cx, base, sx, sy = f
    shield = (f'<path d="M{gm_pt(f, -GM_SHIELD, 0)} Q{gm_pt(f, 0, -0.26)} {gm_pt(f, GM_SHIELD, 0)} Z" fill="{GM_DARK}" '
              f'{LINE} stroke-width="{w(3)}"/>')
    cone = gm_poly(f, [(-GM_SHIELD, 0), (-0.49, 1.77), (0.49, 1.77), (GM_SHIELD, 0)], GM_DARK, w(3))
    rcs = gm_poly(f, [(-0.49, 1.77), (-0.49, 2.2), (0.49, 2.2), (0.49, 1.77)], GM_DARK, w(3))
    rr = gm_poly(f, [(-0.49, 2.2), (-0.42, 3.3), (-0.3, 3.45), (0.3, 3.45), (0.42, 3.3), (0.49, 2.2)], GM_DARK, w(3))
    nose = f'<path d="M{gm_pt(f, -0.42, 3.3)} L{gm_pt(f, 0.42, 3.3)}" fill="none" {LINE} stroke-width="{w(1.6)}"/>'
    # The RCS section's thrusters, two seen.
    jets = "".join(f'<circle cx="{cx + x * sx:.1f}" cy="{base - 1.98 * sy:.1f}" r="{2.2 * k:.1f}" fill="#ffffff" {LINE} '
                   f'stroke-width="{w(1.4)}"/>' for x in (-0.25, 0.25))
    hatches = "".join(gm_poly(f, [(s * 0.07, 0.3), (s * 0.8, 0.3), (s * 0.5, 1.58), (s * 0.07, 1.58)], "none", w(1.6),
                              ' opacity="0.8"') for s in (-1, 1))
    windows = "".join(gm_poly(f, [(s * 0.16, 1.0), (s * 0.58, 1.0), (s * 0.47, 1.45), (s * 0.19, 1.45)], "#eef2f7", w(2))
                      for s in (-1, 1))
    return shield + cone + rcs + rr + nose + jets + hatches + windows


def gemini_retro(f, k=1.0, nozzles=False):
    """The adapter's retrograde section; with its retros' nozzles, once the equipment section is gone."""
    w = lambda x: f"{x * k:.2f}"
    cx, base, sx, sy = f
    out = ""
    if nozzles:
        out = "".join(gm_poly(f, [(x - 0.13, GM_RETRO_Y + 0.05), (x - 0.2, GM_RETRO_Y - 0.2), (x + 0.2, GM_RETRO_Y - 0.2),
                                  (x + 0.13, GM_RETRO_Y + 0.05)], "#ffffff", w(2)) for x in (-0.55, 0, 0.55))
    out += gm_poly(f, [(-GM_SHIELD, 0), (-GM_RETRO, GM_RETRO_Y), (GM_RETRO, GM_RETRO_Y), (GM_SHIELD, 0)], "#ffffff", w(2.4))
    out += "".join(f'<path d="M{gm_pt(f, s * GM_SHIELD * u, 0)} L{gm_pt(f, s * GM_RETRO * u, GM_RETRO_Y)}" fill="none" {LINE} '
                   f'stroke-width="{w(1.2)}" opacity="0.5"/>' for s in (-1, 1) for u in (0.35, 0.7))
    return out


def gemini_equip(f, k=1.0):
    """The adapter's equipment section, from the seam to the base."""
    w = lambda x: f"{x * k:.2f}"
    out = gm_poly(f, [(-GM_RETRO, GM_RETRO_Y), (-GM_EQUIP, GM_EQUIP_Y), (GM_EQUIP, GM_EQUIP_Y), (GM_RETRO, GM_RETRO_Y)],
                  "#ffffff", w(2.4))
    out += "".join(f'<path d="M{gm_pt(f, s * GM_RETRO * u, GM_RETRO_Y)} L{gm_pt(f, s * GM_EQUIP * u, GM_EQUIP_Y)}" fill="none" '
                   f'{LINE} stroke-width="{w(1.2)}" opacity="0.5"/>' for s in (-1, 1) for u in (0.35, 0.7))
    return out


def gemini_whole(f, k=1.0):
    return gemini_equip(f, k) + gemini_retro(f, k) + gemini_module(f, k)


# The Agena target vehicle, nose up: the docking adapter (TDA) at the top with its cone, the long body,
# the engine at the bottom. On the rocket (L) under its shroud, on an adapter cone from the rocket's
# width. In the docking close-up it's drawn the other way up, as target_units is.
def agena_launch(shroud=True, k=1.0):
    w = lambda x: f'stroke-width="{x * k:.2f}"'
    adapter = f'<path d="M915 500 L937 470 L983 470 L1005 500 Z" fill="#ffffff" {LINE} {w(3)}/>'
    body = (f'<rect x="937" y="318" width="46" height="152" fill="#ffffff" {LINE} {w(3)}/>'
            + "".join(f'<path d="M937 {y} L983 {y}" fill="none" {LINE} {w(1.6)} opacity="0.7"/>' for y in (440, 392, 340)))
    tda = f'<rect x="941" y="300" width="38" height="18" fill="#ffffff" {LINE} {w(2.4)}/>'
    if shroud:
        top = f'<path d="M939 300 Q939 264 960 246 Q981 264 981 300 Z" fill="#ffffff" {LINE} {w(3)}/>'
    else:
        top = (f'<path d="M941 300 L928 282 L992 282 L979 300 Z" fill="#ffffff" {LINE} {w(2.4)}/>'
               f'<path d="M956 282 L960 292 L964 282" fill="none" {LINE} {w(1.6)}/>')
    return adapter + body + tda + top


def agena_target(w=2.4):
    """Engine at the top, the docking cone's mouth at the bottom (y 540, x 920 to 1000), as target_units."""
    return (f'<path d="M948 262 L940 238 L980 238 L972 262 Z" fill="#ffffff" {LINE} stroke-width="{w}"/>'
            f'<rect x="930" y="262" width="60" height="232" fill="#ffffff" {LINE} stroke-width="{w}"/>'
            + "".join(f'<path d="M930 {y} L990 {y}" fill="none" {LINE} stroke-width="{w * 0.7:.2f}" opacity="0.8"/>'
                      for y in (300, 380, 460))
            + f'<rect x="936" y="494" width="48" height="26" fill="#ffffff" {LINE} stroke-width="{w}"/>'
            f'<path d="M936 520 L984 520 L1000 540 L920 540 Z" fill="#ffffff" {LINE} stroke-width="{w}"/>'
            f'<path d="M954 540 L960 528 L966 540" fill="none" {LINE} stroke-width="{w * 0.7:.2f}"/>')


# The lunar Gemini programs' hardware (2026-10-18), in Gemini's frame f (its units about a metre: the adapter's
# base 3.05 m across, at GM_EQUIP_Y, where it sits on the rocket). The Transtage: the Titan III's top stage,
# 3.05 m across and 4.57 m long, two engines (AJ10-138) at its foot. The lunar propulsion module and the
# direct-ascent craft were proposals: their shapes follow our blueprints, their sizes unconfirmed.
TT_LEN = 4.57


def gm_shift(body, f, dy):
    """A drawing in frame f moved up dy units."""
    return f'<g transform="translate(0 {-dy * f[3]:.1f})">{body}</g>'


def bells(f, y, xs, length, throat, exit_, k=1.0):
    """Engine bells hanging from y (units) at each x, down length."""
    return "".join(gm_poly(f, [(x - throat, y), (x - exit_, y - length), (x + exit_, y - length), (x + throat, y)],
                           "#ffffff", f"{2.4 * k:.2f}") for x in xs)


def transtage_body(f, y0, k=1.0):
    """The Transtage's drum from y0 up TT_LEN, its bands."""
    w = lambda x: f"{x * k:.2f}"
    out = gm_poly(f, [(-GM_EQUIP, y0), (-GM_EQUIP, y0 + TT_LEN), (GM_EQUIP, y0 + TT_LEN), (GM_EQUIP, y0)], "#ffffff", w(2.4))
    out += "".join(f'<path d="M{gm_pt(f, -GM_EQUIP, y0 + u)} L{gm_pt(f, GM_EQUIP, y0 + u)}" fill="none" {LINE} '
                   f'stroke-width="{w(1.2)}" opacity="0.6"/>' for u in (0.9, TT_LEN - 0.9))
    # A dark panel band round its middle, as the blueprint has.
    out += gm_poly(f, [(-GM_EQUIP, y0 + 1.7), (-GM_EQUIP, y0 + 2.9), (GM_EQUIP, y0 + 2.9), (GM_EQUIP, y0 + 1.7)], GM_DARK, w(1.6),
                   ' opacity="0.7"')
    return out


def transtage_dock(f, y0, k=1.0):
    """The docking adapter on the Transtage's top (y0): a shoulder, a neck and the cone, its mouth y0 + 1.1 up."""
    w = f"{2.4 * k:.2f}"
    return (gm_poly(f, [(-GM_EQUIP, y0), (-0.6, y0 + 0.35), (0.6, y0 + 0.35), (GM_EQUIP, y0)], "#ffffff", w)
            + gm_poly(f, [(-0.6, y0 + 0.35), (-0.6, y0 + 0.75), (0.6, y0 + 0.75), (0.6, y0 + 0.35)], "#ffffff", w)
            + gm_poly(f, [(-0.6, y0 + 0.75), (-1.0, y0 + 1.1), (1.0, y0 + 1.1), (0.6, y0 + 0.75)], "#ffffff", w))


def transtage_launch(f, shroud=True, k=1.0):
    """The Transtage alone on the rocket (its foot on the rocket's top), under a shroud or bare with its engines."""
    y0 = GM_EQUIP_Y
    out = "" if shroud else bells(f, y0, (-0.6, 0.6), 0.9, 0.18, 0.42, k)
    out += transtage_body(f, y0, k)
    top = y0 + TT_LEN
    if shroud:
        out += (f'<path d="M{gm_pt(f, -GM_EQUIP, top)} Q{gm_pt(f, -GM_EQUIP, top + 2.2)} {gm_pt(f, 0, top + 2.9)} '
                f'Q{gm_pt(f, GM_EQUIP, top + 2.2)} {gm_pt(f, GM_EQUIP, top)} Z" fill="#ffffff" {LINE} stroke-width="{3 * k:.2f}"/>')
    else:
        out += transtage_dock(f, top, k)
    return out


def gemini_on_transtage(f, bare):
    """Gemini launched on top of its Transtage (the circumlunar heat-shield test): the stage's engines below."""
    y0 = GM_EQUIP_Y
    out = bells(f, y0, (-0.6, 0.6), 0.9, 0.18, 0.42) if bare else ""
    return out + transtage_body(f, y0) + gm_shift(gemini_whole(f), f, TT_LEN)


def gemini_docked_transtage(f):
    """Gemini docked nose first into a Transtage's cone (the Transtage test's burn): the stage's engines below."""
    y0 = GM_EQUIP_Y
    top = y0 + TT_LEN
    mouth = top + 1.1
    # Gemini upside down, its nose (3.45 up) 0.25 inside the cone's mouth: y' = c - y.
    c = mouth - 0.25 + 3.45
    flip = f'<g transform="translate(0 {1000 - c * f[3]:.1f}) scale(1 -1)">{gemini_whole(f)}</g>'
    return bells(f, y0, (-0.6, 0.6), 0.9, 0.18, 0.42) + transtage_body(f, y0) + flip + transtage_dock(f, top)


LPM_H = 1.6


def gemini_on_lpm(f, bare):
    """Gemini with the lunar propulsion module behind its adapter: a short frustum, wider at its foot, one engine."""
    y0 = GM_EQUIP_Y
    out = bells(f, y0, (0,), 0.9, 0.3, 0.75) if bare else ""
    out += gm_poly(f, [(-1.85, y0), (-GM_EQUIP, y0 + LPM_H), (GM_EQUIP, y0 + LPM_H), (1.85, y0)], "#ffffff", "2.4")
    # Its tanks' dark panels, round the middle.
    out += "".join(gm_poly(f, [(x - 0.32, y0 + 0.4), (x - 0.29, y0 + 1.2), (x + 0.29, y0 + 1.2), (x + 0.32, y0 + 0.4)], GM_DARK,
                           "1.6", ' opacity="0.8"') for x in (-1.1, 0, 1.1))
    return out + gm_shift(gemini_whole(f), f, LPM_H)


LAND_H, ASC_H = 1.5, 1.3


def gemini_direct(f, bare):
    """The direct-ascent craft: Gemini's reentry module on an ascent module on a landing module with its legs."""
    return gemini_direct_modules(f, bare) + gm_shift(gemini_module(f), f, GM_EQUIP_Y + LAND_H + ASC_H)


def gemini_direct_modules(f, bare):
    """The direct-ascent craft's ascent and landing modules, the landing module's foot at GM_EQUIP_Y."""
    y0 = GM_EQUIP_Y
    out = bells(f, y0, (0,), 0.8, 0.28, 0.6) if bare else ""
    # Out of the rocket, its two legs seen: struts splayed from the landing module's side, a pad at each foot.
    if bare:
        out += "".join(f'<path d="M{gm_pt(f, s * 1.55, y0 + 0.65)} L{gm_pt(f, s * 2.25, y0 - 0.55)} M{gm_pt(f, s * 1.95, y0 - 0.55)} '
                       f'L{gm_pt(f, s * 2.55, y0 - 0.55)}" fill="none" {LINE} stroke-width="2.4"/>' for s in (-1, 1))
    half = lambda u: 1.75 - 0.45 * u / LAND_H
    out += gm_poly(f, [(-1.75, y0), (-1.3, y0 + LAND_H), (1.3, y0 + LAND_H), (1.75, y0)], "#ffffff", "2.4")
    out += gm_poly(f, [(-half(0.45), y0 + 0.45), (-half(0.95), y0 + 0.95), (half(0.95), y0 + 0.95), (half(0.45), y0 + 0.45)],
                   GM_DARK, "1.6", ' opacity="0.7"')
    a0 = y0 + LAND_H
    out += gm_poly(f, [(-1.3, a0), (-1.3, a0 + ASC_H), (1.3, a0 + ASC_H), (1.3, a0)], "#ffffff", "2.4")
    out += "".join(gm_poly(f, [(x - 0.3, a0 + 0.35), (x - 0.3, a0 + 0.95), (x + 0.3, a0 + 0.95), (x + 0.3, a0 + 0.35)], GM_DARK,
                           "1.6", ' opacity="0.8"') for x in (-0.75, 0.75))
    return out


def transtage_target(w=2.4):
    """The Transtage as a docking target, as agena_target: engines at the top, the cone's mouth at the bottom (y 540)."""
    return (f'<path d="M922 270 L934 311 L946 311 L958 270 Z M962 270 L974 311 L986 311 L998 270 Z" fill="#ffffff" {LINE} '
            f'stroke-width="{w}"/>'
            f'<rect x="899" y="311" width="122" height="159" fill="#ffffff" {LINE} stroke-width="{w}"/>'
            f'<rect x="899" y="370" width="122" height="44" fill="{GM_DARK}" opacity="0.7" {LINE} stroke-width="{w * 0.7:.2f}"/>'
            f'<path d="M899 340 L1021 340 M899 444 L1021 444" fill="none" {LINE} stroke-width="{w * 0.5:.2f}" opacity="0.6"/>'
            f'<path d="M899 470 L1021 470 L984 494 L936 494 Z" fill="#ffffff" {LINE} stroke-width="{w}"/>'
            f'<rect x="936" y="494" width="48" height="26" fill="#ffffff" {LINE} stroke-width="{w}"/>'
            f'<path d="M936 520 L984 520 L1000 540 L920 540 Z" fill="#ffffff" {LINE} stroke-width="{w}"/>')


def gemini_parts():
    # Empty for everyone else: the main hung from two points (descent.jsx's TILT draws it).
    layer("r-main-2pt", "", (960, RC[1] - RK * (426 - 352)))
    small = lambda body: re.sub(r'stroke-width="([\d.]+)"', lambda m: f'stroke-width="{float(m.group(1)) * 0.8:.2f}"', body)
    rocket = board_group("rocket")
    nose = rocket.index('<path xmlns="http://www.w3.org/2000/svg" d="M915 440')
    # On the pad and the way up: no escape tower (the crew had ejection seats).
    layer("gm-rocket", rocket[:nose] + gemini_whole(GM_L), (960, 600))
    layer("gm-debris-1", gemini_whole(GM_L), (960, 410))
    big = f"translate({DX} {DY}) scale({DK}) translate(-850 -520)"
    turn = "translate(-120 20) rotate(90 960 500)"
    layer("gm-d-payload", f'<g transform="{big}"><g transform="{turn}">{gemini_whole(GM_L)}</g></g>', D_AT(914, 520))
    thick = re.sub(r'stroke-width="[\d.]+"', 'stroke-width="5"', gemini_whole(GM_L))
    for name in ("gm-o-sat", "gm-o-sat-b"):
        layer(name, f'<g transform="translate(960 560) scale(0.6) translate(-960 -426)">{thick}</g>', (960, 560))
    # Coming home: the module nose right (RT) and nose up (RU), its retrograde section the pack (turning
    # about its rim at the shield, which a section left hanging swings from), no straps, the latches at
    # the rim; the equipment section the trunk, from the seam on (adapter.jsx drops it).
    mod = small(gemini_module(GM_R))
    layer("gm-r-capsule", RT(mod), RC)
    layer("gm-r-capsule-up", RU(mod), RC)
    layer("gm-r-pack", RT(small(gemini_retro(GM_R, nozzles=True))), RT_AT(1005, 500))
    layer("gm-r-strap-a", "", RT_AT(915, 500))
    layer("gm-r-strap-b", "", RT_AT(1005, 500))
    layer("gm-r-cut-a", RT(f'<circle cx="915" cy="501" r="6" fill="{GOLD}"/>'), RT_AT(915, 501))
    layer("gm-r-cut-b", RT(f'<circle cx="1005" cy="501" r="6" fill="{GOLD}"/>'), RT_AT(1005, 501))
    seam = 500 - GM_RETRO_Y * GM_R[3]
    rim = 960 + GM_RETRO * GM_R[2]
    layer("gm-r-trunk", RT(small(gemini_equip(GM_R))), RT_AT(rim, seam))
    layer("gm-r-trunk-cut-a", RT(f'<circle cx="{1920 - rim:.1f}" cy="{seam:.1f}" r="6" fill="{GOLD}"/>'), RT_AT(1920 - rim, seam))
    layer("gm-r-trunk-cut-b", RT(f'<circle cx="{rim:.1f}" cy="{seam:.1f}" r="6" fill="{GOLD}"/>'), RT_AT(rim, seam))
    noz = seam + 0.2 * GM_R[3]
    for i, x in enumerate((-0.55, 0, 0.55), 1):
        px = 960 + x * GM_R[2]
        layer(f"gm-r-flame-{i}", RT(f'<path d="M{px - 6:.1f} {noz:.1f} C{px - 7:.1f} {noz + 18:.1f} {px - 3:.1f} {noz + 36:.1f} {px:.1f} '
                                    f'{noz + 54:.1f} C{px + 3:.1f} {noz + 36:.1f} {px + 7:.1f} {noz + 18:.1f} {px + 6:.1f} {noz:.1f} Z" '
                                    f'fill="{GOLD_FILL}" stroke="{GOLD}" stroke-width="2" stroke-linejoin="round"/>'), RT_AT(px, noz))
    # The attitude jets, on the RCS section.
    jy = 500 - 1.98 * GM_R[3]
    layer("gm-r-jet-l", RT(puff(930, jy, 9)), RT_AT(939, jy))
    layer("gm-r-jet-r", RT(puff(990, jy, 9)), RT_AT(981, jy))
    layer("gm-r-jet-stuck", RT(f'<path d="M981 {jy - 4:.1f} C1010 {jy - 12:.1f} 1044 {jy - 22:.1f} 1080 {jy - 30:.1f} L1084 {jy - 14:.1f} '
                               f'C1048 {jy - 8:.1f} 1012 {jy + 4:.1f} 983 {jy + 12:.1f} Z" fill="{GOLD_FILL}" stroke="{GOLD}" '
                               f'stroke-width="2" stroke-linejoin="round"/>'), RC)
    layer("gm-r-glow-side", RT(f'<path d="M984 430 C1000 456 1012 480 1016 504 C1022 476 1006 450 984 430 Z" fill="{GOLD_FILL}" '
                               f'stroke="{GOLD}" stroke-width="2" stroke-linejoin="round"/>'), RC)
    # The break-up: the nose (R&R and RCS sections) from the cabin, at the generic seam's pivots.
    top = small(gm_poly(GM_R, [(-0.49, 1.77), (-0.49, 2.2), (0.49, 2.2), (0.49, 1.77)], GM_DARK, 3)
                + gm_poly(GM_R, [(-0.49, 2.2), (-0.42, 3.3), (-0.3, 3.45), (0.3, 3.45), (0.42, 3.3), (0.49, 2.2)], GM_DARK, 3))
    body = small(f'<path d="M{gm_pt(GM_R, -GM_SHIELD, 0)} Q{gm_pt(GM_R, 0, -0.26)} {gm_pt(GM_R, GM_SHIELD, 0)} Z" fill="{GM_DARK}" '
                 f'{LINE} stroke-width="3"/>'
                 + gm_poly(GM_R, [(-GM_SHIELD, 0), (-0.49, 1.77), (0.49, 1.77), (GM_SHIELD, 0)], GM_DARK, 3)
                 + "".join(gm_poly(GM_R, [(s * 0.16, 1.0), (s * 0.58, 1.0), (s * 0.47, 1.45), (s * 0.19, 1.45)], "#eef2f7", 2)
                           for s in (-1, 1)))
    layer("gm-r-half-nose", RT(top), RT_AT(960, 410))
    layer("gm-r-half-body", RT(body), RT_AT(960, 472))
    # The main on two points (descent.jsx's TILT): the same canopy, its lines to a ring, and from there
    # two legs to the nose and the shield's upper rim with the module turned TILT_DEG off upright about
    # its centre (35 degrees nose up over the water).
    a = math.radians(55)
    ring = (960, RC[1] - RK * (426 - 352))
    def at(x, y):
        dx, dy = RK * (x - 960), RK * (y - 426)
        return (RC[0] + dx * math.cos(a) - dy * math.sin(a), RC[1] + dx * math.sin(a) + dy * math.cos(a))
    legs = "".join(f'<path d="M{ring[0]} {ring[1]} L{p[0]:.1f} {p[1]:.1f}" fill="none" {LINE} stroke-width="2.4"/>'
                   for p in (at(960, 352), at(915, 500)))
    cx, w_, h = 960, 400, 190
    dome_y = ring[1] - 210
    canopy = (f'<path d="M{cx - w_ / 2} {dome_y} C{cx - w_ / 2} {dome_y - h} {cx + w_ / 2} {dome_y - h} {cx + w_ / 2} {dome_y} '
              f'C{cx + w_ / 4} {dome_y - h * 0.18} {cx - w_ / 4} {dome_y - h * 0.18} {cx - w_ / 2} {dome_y} Z" fill="#ffffff" {LINE} stroke-width="4"/>'
              f'<path d="M{cx} {dome_y - h * 0.75} L{cx} {dome_y - h * 0.15}" fill="none" {LINE} stroke-width="2"/>'
              + "".join(f'<path d="M{cx + kk * w_ / 2} {dome_y} L{cx} {ring[1]}" fill="none" {LINE} stroke-width="2"/>'
                        for kk in (-1, -0.45, 0.45, 1)))
    layer("gm-r-main-2pt", legs + canopy, ring)
    # The docking close-up: Gemini whole, nose right (the chaser) or left (the other of a rendezvous),
    # and the Agena, its cone's mouth where the generic target's collar is.
    whole = small(gemini_whole(GM_R))
    layer("gm-k-chaser", KT(KC, 90, whole), KC)
    tc = (KM[0] + KK * (540 - 426), KM[1])
    layer("gm-k-target", KT(tc, 90, agena_target()), KM)
    oc = (1150, 470)
    layer("gm-k-other", KT(oc, -90, whole), oc)
    lp = K_AT(tc, 938, 508, 90)
    layer("gm-k-light", f'<circle cx="{lp[0]:.1f}" cy="{lp[1]:.1f}" r="7" fill="{GOLD}" {LINE} stroke-width="2"/>', lp)
    # Its thrusters: braking ahead of the cabin's shoulders, pushing from the adapter's base, one on top.
    sh_t, sh_b = K_AT(KC, 926, 470, 90), K_AT(KC, 994, 470, 90)
    layer("gm-k-brake", puff(sh_t[0] + 18, sh_t[1] - 6, 9) + puff(sh_b[0] + 18, sh_b[1] + 6, 9), KC)
    base_y = 500 - GM_EQUIP_Y * GM_R[3]
    b_t, b_b = K_AT(KC, 908, base_y, 90), K_AT(KC, 1012, base_y, 90)
    layer("gm-k-push", puff(b_t[0] - 16, b_t[1], 9) + puff(b_b[0] - 16, b_b[1], 9), KC)
    side = K_AT(KC, 912, 520, 90)
    layer("gm-k-side", puff(side[0], side[1] - 16, 10), side)
    root = K_AT(KC, 910, 530, 90)
    layer("gm-k-stuck", f'<path d="M{root[0] - 6:.1f} {root[1]:.1f} C{root[0] - 14:.1f} {root[1] - 40:.1f} {root[0] - 20:.1f} '
                        f'{root[1] - 80:.1f} {root[0] - 28:.1f} {root[1] - 120:.1f} L{root[0] - 12:.1f} {root[1] - 122:.1f} '
                        f'C{root[0] - 6:.1f} {root[1] - 80:.1f} {root[0] + 2:.1f} {root[1] - 40:.1f} {root[0] + 6:.1f} {root[1]:.1f} Z" '
                        f'fill="{GOLD_FILL}" stroke="{GOLD}" stroke-width="2" stroke-linejoin="round"/>', KC)
    # The spacewalk: Gemini whole as the way home's capsule, raised EV_UP so the open hatch is where the
    # figure stands (the cone's top edge at y 380 under it), the hatch on that slope, and the experiment
    # package on a short mount down to the RCS section.
    EV_UP = 23
    gm_capsule = f'<g transform="translate(0 {-EV_UP})">{RT(whole)}</g>'
    layer("gm-e-capsule", gm_capsule, RC)
    slope = math.degrees(math.atan2(2 * (0.655 * GM_R[2]), 2 * (1.77 * GM_R[3])))
    edge = lambda x: 380 + (x - 881) * math.tan(math.radians(slope))
    x0, x1 = HATCH[0], HATCH[1]
    hy = edge(x1)
    gm_hole = (f'<g transform="rotate({slope:.2f} {x1} {hy:.1f})"><rect x="{x0 + 4}" y="{hy - 2:.1f}" width="{x1 - x0 - 8}" '
               f'height="10" fill="{NAVY}"/></g>')
    gm_hatch = (f'<g transform="rotate({slope:.2f} {x1} {hy:.1f})"><rect x="{x0}" y="{hy - 8:.1f}" width="{x1 - x0}" '
                f'height="10" rx="3" fill="{GM_DARK}" {LINE} stroke-width="3"/></g>')
    layer("gm-e-hole", gm_hole, (x1, hy))
    layer("gm-e-hatch", gm_hatch, (x1, hy))
    # A spacewalk on a rendezvous: the other Gemini; and one docked, with the Agena at the nose (2026-10-18).
    pair_parts("gm-", gm_capsule, gm_hole, gm_hatch, (x1, hy))
    layer("gm-e-agena", agena_docked(RC[1] - EV_UP), (AGENA_MOUTH, RC[1] - EV_UP))
    rcs_top = RC[1] - EV_UP - RK * (0.49 * GM_R[2])
    # The mount stays on the hull when the package is taken (2026-10-18; the spacewalk's teardown).
    layer("gm-e-mount", f'<path d="M966 380 L966 {rcs_top:.1f} M984 380 L984 {rcs_top:.1f}" fill="none" '
                        f'{LINE} stroke-width="3"/>', (975, 380))
    layer("gm-e-box", f'<rect x="958" y="356" width="34" height="24" rx="3" fill="url(#hatch)" {LINE} stroke-width="3"/>'
                      f'<rect x="958" y="356" width="34" height="24" rx="3" fill="{GOLD_FILL}"/>', (975, 360))
    # The Agena's own launch (its steps, "atv-", in a Gemini docking flight): on the rocket under its
    # shroud, and in the deployment with its docking cone bare.
    layer("ag-rocket", rocket[:nose] + agena_launch(), (960, 600))
    layer("ag-debris-1", agena_launch(), (960, 380))
    layer("ag-d-payload", f'<g transform="{big}"><g transform="{turn}">{agena_launch(shroud=False)}</g></g>', D_AT(914, 520))
    # The lunar Gemini programs' own hardware (2026-10-18, user; their Earth-orbit tests): the Transtage alone (tt-,
    # under a shroud on the rocket, bare with its docking cone, and the docking target), and four stacks on the
    # rocket, in deployment and on the map, played as Gemini's skin with these parts in its place: Gemini on a
    # Transtage (gt-), Gemini docked nose first into one (gtd-, on the map only), Gemini with the lunar
    # propulsion module (gl-) and the direct-ascent craft (gd-).
    stacks = {"gt": gemini_on_transtage, "gl": gemini_on_lpm, "gd": gemini_direct}
    for key, draw in stacks.items():
        layer(f"{key}-rocket", rocket[:nose] + draw(GM_L, False), (960, 600))
        layer(f"{key}-debris-1", draw(GM_L, False), (960, 410))
        layer(f"{key}-d-payload", f'<g transform="{big}"><g transform="{turn}">{draw(GM_L, True)}</g></g>', D_AT(914, 520))
        thick = re.sub(r'stroke-width="[\d.]+"', 'stroke-width="5"', draw(GM_L, True))
        layer(f"{key}-o-sat", f'<g transform="translate(960 560) scale(0.6) translate(-960 -426)">{thick}</g>', (960, 560))
    # The heat-shield test's stack on the map in two, overlaying gt-o-sat exactly (the same frame): Gemini and the
    # Transtage under it, which lets go after the high-orbit injection (2026-10-18, high-orbit-injection.jsx's SEP).
    th = lambda body: re.sub(r'stroke-width="[\d.]+"', 'stroke-width="5"', body)
    on_map = lambda body: f'<g transform="translate(960 560) scale(0.6) translate(-960 -426)">{th(body)}</g>'
    layer("gt-o-sat-gemini", on_map(gm_shift(gemini_whole(GM_L), GM_L, TT_LEN)), (960, 560))
    # Coming home on that ellipse (2026-10-18, user): the capsule and its retrograde section, the equipment section
    # gone at the reentry preparations, in the same frame.
    layer("gt-o-sat-home", on_map(gm_shift(gemini_retro(GM_L) + gemini_module(GM_L), GM_L, TT_LEN)), (960, 560))
    layer("gt-o-sat-stage", on_map(bells(GM_L, GM_EQUIP_Y, (-0.6, 0.6), 0.9, 0.18, 0.42) + transtage_body(GM_L, GM_EQUIP_Y)),
          (960, 560))
    thick = re.sub(r'stroke-width="[\d.]+"', 'stroke-width="5"', gemini_docked_transtage(GM_L))
    layer("gtd-o-sat", f'<g transform="translate(960 560) scale(0.6) translate(-960 -426)">{thick}</g>', (960, 560))
    # Circumlunar Gemini's flight to the Moon (2026-10-20, user): the docked stack in two, overlaying gtd-o-sat exactly,
    # for the trans-lunar injection after which the Transtage lets go (tpi.jsx's SEP); the Transtage alone on the
    # map (the uncrewed flyby) and Gemini and the Transtage on the coasts' map (moon-coast.jsx, small at the Sun's
    # place, as ap-m-probe); and Gemini whole and in two for the Earth approach, where the adapter goes (gm-o-sat's
    # frame).
    y0, top = GM_EQUIP_Y, GM_EQUIP_Y + TT_LEN
    c = top + 1.1 - 0.25 + 3.45
    layer("gtd-o-sat-gemini", on_map(f'<g transform="translate(0 {1000 - c * GM_L[3]:.1f}) scale(1 -1)">{gemini_whole(GM_L)}</g>'),
          (960, 560))
    layer("gtd-o-sat-stage", on_map(bells(GM_L, y0, (-0.6, 0.6), 0.9, 0.18, 0.42) + transtage_body(GM_L, y0)
                                    + transtage_dock(GM_L, top)), (960, 560))
    layer("tt-o-sat", on_map(transtage_launch(GM_L, shroud=False)), (960, 560))
    layer("gm-o-sat-capsule", on_map(gemini_module(GM_L)), (960, 560))
    layer("gm-o-sat-adapter", on_map(gemini_equip(GM_L) + gemini_retro(GM_L)), (960, 560))
    # Lunar-orbit Gemini coming home (2026-10-20, user): the propulsion module and the adapter go on the Earth approach,
    # before the air (earth-approach.jsx's DROP).
    layer("gl-o-sat-aft", on_map(bells(GM_L, GM_EQUIP_Y, (0,), 0.9, 0.3, 0.75)
                                 + gm_poly(GM_L, [(-1.85, GM_EQUIP_Y), (-GM_EQUIP, GM_EQUIP_Y + LPM_H), (GM_EQUIP, GM_EQUIP_Y + LPM_H),
                                                  (1.85, GM_EQUIP_Y)], "#ffffff", "2.4")
                                 + gm_shift(gemini_equip(GM_L) + gemini_retro(GM_L), GM_L, LPM_H)), (960, 560))
    layer("gl-o-sat-capsule", on_map(gm_shift(gemini_module(GM_L), GM_L, LPM_H)), (960, 560))
    cx, cy = SUN
    for name, body in (("gm-m-probe", gemini_whole(GM_L)), ("tt-m-probe", transtage_launch(GM_L, shroud=False)),
                       ("gl-m-probe", gemini_on_lpm(GM_L, True))):
        layer(name, f'<g transform="translate({cx} {cy}) scale(0.32) translate(-960 -480)">{th(body)}</g>', SUN)
    layer("tt-rocket", rocket[:nose] + transtage_launch(GM_L, shroud=True), (960, 600))
    layer("tt-debris-1", transtage_launch(GM_L, shroud=True), (960, 410))
    layer("tt-d-payload", f'<g transform="{big}"><g transform="{turn}">{transtage_launch(GM_L, shroud=False)}</g></g>',
          D_AT(914, 520))
    tc = (KM[0] + KK * (540 - 426), KM[1])
    layer("tt-k-target", KT(tc, 90, transtage_target()), KM)
    # The direct-ascent craft coming home (2026-10-19, user): no adapter, so no retrograde section; its ascent and
    # landing modules are the trunk the reentry preparations drop (adapter.jsx), latched at the shield's rim.
    drop = -(GM_EQUIP_Y + LAND_H + ASC_H)
    rim = 960 + 1.3 * GM_R[2]
    layer("gd-r-pack", "", RT_AT(1005, 500))
    layer("gd-r-trunk", RT(small(gm_shift(gemini_direct_modules(GM_R, True), GM_R, drop))), RT_AT(rim, 500))
    layer("gd-r-trunk-cut-a", RT(f'<circle cx="{1920 - rim:.1f}" cy="500" r="6" fill="{GOLD}"/>'), RT_AT(1920 - rim, 500))
    layer("gd-r-trunk-cut-b", RT(f'<circle cx="{rim:.1f}" cy="500" r="6" fill="{GOLD}"/>'), RT_AT(rim, 500))
    # The direct-ascent craft to the Moon (2026-10-20, user). The trans-lunar injection is the rocket's (70 % of the step):
    # its upper stage stays on through the parking orbit, burns and lets go (tpi.jsx's SEP). A plain hydrogen stage
    # (3.05 m across, about 9.1 m long, two engines), drawn as lunar-orbit Gemini's Centaur once was: rockets stay
    # generic. On the map in a frame 0.6 of the others', the stack's middle where the craft's is.
    ST_LEN, ST_ADAPT = 9.1, 0.6
    craft_foot = GM_EQUIP_Y - 0.8
    s0 = craft_foot - ST_ADAPT - ST_LEN - 1.0
    craft_top = GM_EQUIP_Y + LAND_H + ASC_H + 3.45
    sk = 0.6
    fs = (960, GM_L[1] - (craft_foot + craft_top) / 2 * GM_L[3] + (s0 + craft_top) / 2 * sk * GM_L[3], GM_L[2] * sk, GM_L[3] * sk)
    c0 = craft_foot - ST_ADAPT

    def stage_at(f):
        return (bells(f, c0 - ST_LEN, (-0.6, 0.6), 1.0, 0.2, 0.45, sk)
                + gm_poly(f, [(-GM_EQUIP, c0 - ST_LEN), (-GM_EQUIP, c0), (GM_EQUIP, c0), (GM_EQUIP, c0 - ST_LEN)], "#ffffff", "2.4")
                + "".join(f'<path d="M{gm_pt(f, -GM_EQUIP, c0 - u)} L{gm_pt(f, GM_EQUIP, c0 - u)}" fill="none" {LINE} '
                          f'stroke-width="1.2" opacity="0.6"/>' for u in (1.2, ST_LEN - 1.6))
                # The adapter up to the landing module's foot, its engine bell inside.
                + gm_poly(f, [(-GM_EQUIP, c0), (-1.75, GM_EQUIP_Y), (1.75, GM_EQUIP_Y), (GM_EQUIP, c0)], "#ffffff", "2.4"))
    layer("gd-o-sat-stack", on_map(stage_at(fs) + gemini_direct(fs, False)), (960, 560))
    layer("gd-o-sat-top", on_map(gemini_direct(fs, False)), (960, 560))
    layer("gd-o-sat-stage", on_map(stage_at(fs)), (960, 560))
    fd = (960, GM_L[1] - GM_EQUIP_Y * GM_L[3] + s0 * sk * GM_L[3], GM_L[2] * sk, GM_L[3] * sk)
    layer("gd-d-payload-stack", f'<g transform="{big}"><g transform="{turn}">{stage_at(fd) + gemini_direct(fd, False)}</g></g>',
          D_AT(914, 520))
    # The flames on the map, each from its own engine (the generic one's top is at 600): the stage's, the landing
    # module's (the whole craft), the ascent module's (after the Moon: it has a small bell of its own, hidden in the
    # landing module until then).
    stage_tail = fs[1] - (c0 - ST_LEN - 1.0) * fs[3]
    asc_bell = lambda f: bells(f, GM_EQUIP_Y + LAND_H, (0,), 0.5, 0.22, 0.42)
    for name, tail in (("gd-o-streak-stage", stage_tail), ("gd-o-streak", 500 - (GM_EQUIP_Y - 0.8) * GM_L[3]),
                       ("gd-o-streak-asc", 500 - (GM_EQUIP_Y + LAND_H - 0.5) * GM_L[3])):
        dy = 560 + 0.6 * (tail - 426) - 600
        layer(name, f'<g transform="translate(0 {dy:.1f})"><path d="M944 600 C942 660 952 740 960 790 C968 740 978 660 976 600 Z" '
                    f'fill="{GOLD_FILL}" stroke="{GOLD}" stroke-width="3" stroke-linejoin="round"/></g>', (960, 560))
    # After the Moon: the ascent module and the capsule; on the Earth approach the capsule alone and what it casts off
    # (both modules, or the ascent module after a landing), all in gd-o-sat's frame. Nothing waits in lunar orbit for it
    # (the lunar ascent's o-sat-b): it flies straight home.
    capsule_at = lambda f: gm_shift(gemini_module(f), f, GM_EQUIP_Y + LAND_H + ASC_H)
    asc_only = lambda f: gm_shift(asc_bell(f) + gm_poly(f, [(-1.3, GM_EQUIP_Y + LAND_H), (-1.3, GM_EQUIP_Y + LAND_H + ASC_H),
                                                            (1.3, GM_EQUIP_Y + LAND_H + ASC_H), (1.3, GM_EQUIP_Y + LAND_H)], "#ffffff", "2.4"),
                                  f, 0)
    layer("gd-o-sat-asc", on_map(asc_only(GM_L) + capsule_at(GM_L)), (960, 560))
    layer("gd-o-sat-capsule", on_map(capsule_at(GM_L)), (960, 560))
    layer("gd-o-sat-aft", on_map(gemini_direct_modules(GM_L, True)), (960, 560))
    layer("gd-o-sat-aft-asc", on_map(asc_only(GM_L)), (960, 560))
    layer("gd-none", "", (960, 560))
    for name, body in (("gd-m-probe", gemini_direct(GM_L, True)), ("gd-m-probe-asc", asc_only(GM_L) + capsule_at(GM_L))):
        layer(name, f'<g transform="translate({SUN[0]} {SUN[1]}) scale(0.32) translate(-960 -480)">{th(body)}</g>', SUN)
    # On the Moon, in the lander's frame (gd-n-descent, gd-n-ascent in the generic lander's place, so its scenes' timings
    # fit): the landing module's top at the porch (y 722), its bell's mouth by the plume's root (822), 42 px a unit, so
    # the capsule's nose stays inside the moonwalk's close-up. Legs of its own reach the ground (the Earth-orbit tests'
    # stubs don't). Sizes unconfirmed, from our blueprint.
    fn = (960, 722 + (GM_EQUIP_Y + LAND_H) * 42, 42, 42)
    legs = (f'<path d="M906 760 L836 876 M1014 760 L1084 876 M916 784 L870 850 M1004 784 L1050 850" fill="none" {LINE} '
            f'stroke-width="4"/>'
            f'<ellipse cx="834" cy="878" rx="18" ry="5" fill="#ffffff" {LINE} stroke-width="3"/>'
            f'<ellipse cx="1086" cy="878" rx="18" ry="5" fill="#ffffff" {LINE} stroke-width="3"/>')
    landing = gemini_direct_modules(fn, False)
    cut = landing.index(gm_poly(fn, [(-1.3, GM_EQUIP_Y + LAND_H), (-1.3, GM_EQUIP_Y + LAND_H + ASC_H),
                                     (1.3, GM_EQUIP_Y + LAND_H + ASC_H), (1.3, GM_EQUIP_Y + LAND_H)], "#ffffff", "2.4"))
    layer("gd-n-descent", legs + bells(fn, GM_EQUIP_Y, (0,), 0.8, 0.28, 0.6) + landing[:cut], LC)
    layer("gd-n-ascent", landing[cut:] + capsule_at(fn), LC)


# --- Gemini EOR (2026-10-19, user; ge-, ll- and sv- parts): its Earth-orbit tests. Gemini's reentry module and
# retrograde section, then the EOR service module in the equipment section's place (our blueprint's ribbed cone,
# dark on one side, out to GE_B) and behind it an open frame (the blueprint's second view) that holds the Langley
# light lander upright, its canopy towards the capsule. The lander (ll-): an open platform on four legs (two seen),
# two dark tanks either side of a clear canopy with the seat, one engine under it. The S-IVB tug (sv-): its docking
# ring and ribbed collar, the forward cone, the long tank, the aft cone and its J-2's ribbed bell. Our reading of
# the blueprints; the sizes are unconfirmed.
GE_B, GE_SM_L, GE_BAY_L = 1.65, 3.0, 3.0
GE_BAY0 = GM_RETRO_Y - GE_SM_L                               # the frame's front, where the service module ends
GE_BASE = GE_BAY0 - GE_BAY_L                                 # its open end
LL_TOP = 1.9                                                 # the canopy's top above the platform's foot
TUG_L = 0.3 + 0.8 + 2.0 + 11.0 + 2.2 + 3.0                   # the tug, ring to bell


def langley(cx, base, kx, ky, pilot=False, w=1.0):
    """The Langley lander nose up, its platform's foot at base (px), kx and ky px/m across and along."""
    X = lambda u: cx + u * kx
    Y = lambda v: base - v * ky
    sw = lambda x: f'stroke-width="{x * w:.2f}"'

    def poly(pts, fill, width, extra=""):
        return f'<path d="M{" L".join(f"{X(a):.1f} {Y(b):.1f}" for a, b in pts)} Z" fill="{fill}" {LINE} {sw(width)}{extra}/>'
    out = poly([(-0.18, 0), (-0.32, -0.55), (0.32, -0.55), (0.18, 0)], "#ffffff", 2.4)
    out += "".join(f'<path d="M{X(s * 0.85):.1f} {Y(0.1):.1f} L{X(s * 1.3):.1f} {Y(-0.72):.1f} M{X(s * 0.5):.1f} {Y(0):.1f} '
                   f'L{X(s * 1.1):.1f} {Y(-0.42):.1f}" fill="none" {LINE} {sw(2.6)}/>'
                   f'<ellipse cx="{X(s * 1.3):.1f}" cy="{Y(-0.75):.1f}" rx="{0.18 * kx:.1f}" ry="{0.05 * ky + 1:.1f}" fill="#ffffff" '
                   f'{LINE} {sw(2)}/>' for s in (-1, 1))
    out += poly([(-1.0, 0), (-1.0, 0.25), (1.0, 0.25), (1.0, 0)], "#ffffff", 2.6)
    # The canopy, clear, the seat in it, and its pilot's helmet when crewed.
    out += (f'<rect x="{X(-0.32):.1f}" y="{Y(LL_TOP):.1f}" width="{0.64 * kx:.1f}" height="{(LL_TOP - 0.25) * ky:.1f}" '
            f'rx="{0.3 * kx:.1f}" fill="rgba(22,48,92,0.08)" {LINE} {sw(2.2)}/>'
            f'<path d="M{X(-0.14):.1f} {Y(1.2):.1f} L{X(-0.14):.1f} {Y(0.45):.1f} L{X(0.2):.1f} {Y(0.45):.1f}" fill="none" {LINE} '
            f'{sw(2.2)} opacity="0.7"/>')
    if pilot:
        out += (f'<circle cx="{X(0):.1f}" cy="{Y(1.32):.1f}" r="{0.19 * kx:.1f}" fill="#ffffff" {LINE} {sw(2.2)}/>'
                f'<path d="M{X(0.02):.1f} {Y(1.42):.1f} L{X(0.17):.1f} {Y(1.4):.1f} L{X(0.17):.1f} {Y(1.24):.1f} L{X(0.02):.1f} '
                f'{Y(1.22):.1f} Z" fill="{GM_DARK}" {LINE} {sw(1.6)}/>')
    for s in (-1, 1):
        x0 = min(s * 0.37, s * 0.99)
        out += (f'<rect x="{X(x0):.1f}" y="{Y(1.5):.1f}" width="{0.62 * kx:.1f}" height="{1.25 * ky:.1f}" rx="{0.31 * kx:.1f}" '
                f'fill="{GM_DARK}" {LINE} {sw(2.6)}/>'
                f'<path d="M{X(s * 0.37):.1f} {Y(0.88):.1f} L{X(s * 0.99):.1f} {Y(0.88):.1f}" fill="none" {LINE} {sw(1.4)} opacity="0.6"/>'
                f'<rect x="{X(s * 1.06) - 0.07 * kx:.1f}" y="{Y(1.0):.1f}" width="{0.14 * kx:.1f}" height="{0.2 * ky:.1f}" '
                f'fill="#ffffff" {LINE} {sw(1.6)}/>')
    # The dish on its mast, over the right-hand tank.
    out += (f'<path d="M{X(0.68):.1f} {Y(1.5):.1f} L{X(0.82):.1f} {Y(1.82):.1f}" fill="none" {LINE} {sw(1.8)}/>'
            f'<path d="M{X(0.66):.1f} {Y(1.86):.1f} Q{X(0.84):.1f} {Y(1.74):.1f} {X(1.0):.1f} {Y(1.9):.1f} Z" fill="#ffffff" '
            f'{LINE} {sw(1.8)}/>')
    return out


def eor_sm(f, k=1.0):
    """The service module, from the retrograde section's seam back to the frame."""
    w = lambda x: f"{x * k:.2f}"
    y0, y1 = GM_RETRO_Y, GE_BAY0
    out = gm_poly(f, [(-GM_RETRO, y0), (-GE_B, y1), (GE_B, y1), (GM_RETRO, y0)], "#ffffff", w(2.4))
    out += gm_poly(f, [(0, y0), (0, y1), (GE_B, y1), (GM_RETRO, y0)], GM_DARK, w(1.2), ' opacity="0.55"')
    out += "".join(f'<path d="M{gm_pt(f, s * GM_RETRO * u, y0)} L{gm_pt(f, s * GE_B * u, y1)}" fill="none" {LINE} '
                   f'stroke-width="{w(1.2)}" opacity="0.5"/>' for s in (-1, 1) for u in (0.3, 0.65))
    # The ribbed collar at its narrow end.
    c = GM_RETRO + (GE_B - GM_RETRO) * 0.3 / GE_SM_L
    out += f'<path d="M{gm_pt(f, -c, y0 - 0.3)} L{gm_pt(f, c, y0 - 0.3)}" fill="none" {LINE} stroke-width="{w(1.4)}" opacity="0.7"/>'
    return out


def eor_bay(f, k=1.0):
    """The open frame behind the service module: its bulkhead, two longerons and the stubs of its back ring."""
    w = lambda x: f"{x * k:.2f}"
    y0, y1, t = GE_BAY0, GE_BASE, 0.12
    out = gm_poly(f, [(-GE_B, y0), (-GE_B, y0 - t), (GE_B, y0 - t), (GE_B, y0)], "#ffffff", w(2.4))
    for s in (-1, 1):
        out += gm_poly(f, [(s * GE_B, y0), (s * GE_B, y1), (s * (GE_B - t), y1), (s * (GE_B - t), y0)], "#ffffff", w(2.4))
        out += gm_poly(f, [(s * GE_B, y1), (s * GE_B, y1 + t), (s * (GE_B - 0.4), y1 + t), (s * (GE_B - 0.4), y1)], "#ffffff", w(2.4))
    return out


def eor_stack(f, lander=True, pilot=False, k=1.0):
    """Gemini EOR whole: the lander (or none) in its frame, the service module, the retrograde section, the module."""
    out = ""
    if lander:
        base = f[1] - (GE_BAY0 - 0.15 - LL_TOP) * f[3]
        out += langley(f[0], base, f[2], f[3], pilot, k * 0.8)
    return out + eor_sm(f, k) + eor_bay(f, k) + gemini_retro(f, k) + gemini_module(f, k)


def tug_up(cx, top, k):
    """The S-IVB tug nose up, its docking ring's top at top, k px/m."""
    X = lambda u: cx + u * k
    Y = lambda v: top + v * k

    def poly(pts, fill, width, extra=""):
        return f'<path d="M{" L".join(f"{X(a):.1f} {Y(b):.1f}" for a, b in pts)} Z" fill="{fill}" {LINE} stroke-width="{width}"{extra}/>'
    y = [0, 0.3, 1.1, 3.1, 14.1, 16.3, 19.3]
    out = poly([(-0.5, y[5]), (-1.15, y[6]), (1.15, y[6]), (0.5, y[5])], "#ffffff", 2.4)
    out += "".join(f'<path d="M{X(-0.5 - 0.65 * u):.1f} {Y(y[5] + 3 * u):.1f} L{X(0.5 + 0.65 * u):.1f} {Y(y[5] + 3 * u):.1f}" '
                   f'fill="none" {LINE} stroke-width="1.2" opacity="0.5"/>' for u in (0.2, 0.4, 0.6, 0.8))
    out += poly([(-3.3, y[4]), (-1.5, y[5]), (1.5, y[5]), (3.3, y[4])], "#ffffff", 2.6)
    out += poly([(-3.3, y[3]), (-3.3, y[4]), (3.3, y[4]), (3.3, y[3])], "#ffffff", 3)
    out += "".join(f'<path d="M{X(-3.3):.1f} {Y(v):.1f} L{X(3.3):.1f} {Y(v):.1f}" fill="none" {LINE} stroke-width="1.2" '
                   f'opacity="0.45"/>' for v in (y[3] + 3.6, y[3] + 7.4))
    out += poly([(-1.5, y[2]), (-3.3, y[3]), (3.3, y[3]), (1.5, y[2])], "#ffffff", 2.6)
    out += "".join(f'<path d="M{X(s * 1.5 * u):.1f} {Y(y[2]):.1f} L{X(s * 3.3 * u):.1f} {Y(y[3]):.1f}" fill="none" {LINE} '
                   f'stroke-width="1.2" opacity="0.5"/>' for s in (-1, 1) for u in (0.35, 0.7))
    out += poly([(-1.5, y[1]), (-1.5, y[2]), (1.5, y[2]), (1.5, y[1])], "#ffffff", 2.4)
    out += "".join(f'<path d="M{X(u):.1f} {Y(y[1]):.1f} L{X(u):.1f} {Y(y[2]):.1f}" fill="none" {LINE} stroke-width="1.2" '
                   f'opacity="0.5"/>' for u in (-1.0, -0.5, 0, 0.5, 1.0))
    out += poly([(-1.1, y[0]), (-1.1, y[1]), (1.1, y[1]), (1.1, y[0])], GM_DARK, 2.4)
    return out


def gemini_eor_parts():
    small = lambda body: re.sub(r'stroke-width="([\d.]+)"', lambda m: f'stroke-width="{float(m.group(1)) * 0.8:.2f}"', body)
    thick = lambda body, s=5: re.sub(r'stroke-width="[\d.]+"', f'stroke-width="{s}"', body)
    rocket = board_group("rocket")
    nose = rocket.index('<path xmlns="http://www.w3.org/2000/svg" d="M915 440')
    big = f"translate({DX} {DY}) scale({DK}) translate(-850 -520)"
    turn = "translate(-120 20) rotate(90 960 500)"
    # On the rocket (no tower: ejection seats), out of it and on the map: the frame's open end on the rocket's top.
    launch = gm_shift(eor_stack(GM_L), GM_L, GM_EQUIP_Y - GE_BASE)
    mid = 500 - (3.45 - GE_BASE) * GM_L[3] / 2
    layer("ge-rocket", rocket[:nose] + launch, (960, 600))
    layer("ge-debris-1", launch, (960, mid))
    layer("ge-d-payload", f'<g transform="{big}"><g transform="{turn}">{launch}</g></g>', D_AT(914, 520))
    layer("ge-o-sat", f'<g transform="translate(960 560) scale(0.45) translate(-960 {-mid:.1f})">{thick(launch)}</g>', (960, 560))
    # Coming home: the service module and its frame, empty, are the trunk the modules' separation drops (adapter.jsx),
    # from the seam on, as Gemini's equipment section.
    seam = 500 - GM_RETRO_Y * GM_R[3]
    rim = 960 + GM_RETRO * GM_R[2]
    layer("ge-r-trunk", RT(small(eor_sm(GM_R) + eor_bay(GM_R))), RT_AT(rim, seam))
    # The docking close-up. The stack as the chaser (the tug's docking), the lander in its frame, its jets behind it at
    # the frame's open end.
    sx, sy = GM_R[2], GM_R[3]
    layer("ge-k-chaser", KT(KC, 90, small(eor_stack(GM_R))), KC)
    base_y = 500 - GE_BASE * sy
    b_t, b_b = K_AT(KC, 960 - GE_B * sx, base_y, 90), K_AT(KC, 960 + GE_B * sx, base_y, 90)
    layer("ge-k-push", puff(b_t[0] - 16, b_t[1], 9) + puff(b_b[0] - 16, b_b[1], 9), KC)
    # The stack as the lander's target, empty: the frame's open end at the target's mouth (540), the rest beyond it.
    tc = (KM[0] + KK * (540 - 426), KM[1])
    moved = f'<g transform="translate(0 {540 - base_y:.1f})">{small(eor_stack(GM_R, lander=False))}</g>'
    layer("ge-k-target", KT(tc, 90, moved), KM)
    lp = K_AT(tc, 960 - GE_B * sx + 8, 530, 90)
    layer("ge-k-light", f'<circle cx="{lp[0]:.1f}" cy="{lp[1]:.1f}" r="7" fill="{GOLD}" {LINE} stroke-width="2"/>', lp)
    corners = [K_AT(tc, 960 + s * (GE_B - 0.06) * sx, 540, 90) for s in (-1, 1)]
    layer("ge-k-latch", "".join(f'<circle cx="{x:.1f}" cy="{y:.1f}" r="9" fill="{GOLD}"/>' for x, y in corners), KM)
    # The lander as the chaser, its canopy first: docked (no offset), it sits in the frame, its canopy 0.15 m off the
    # bulkhead (the mouth is at 426 - (KM - KC) / KK in its own drawing). With its pilot, or empty (cast off).
    mouth = 426 - (KM[0] - KC[0]) / KK
    ll_base = mouth - (GE_BAY_L - 0.15 - LL_TOP) * sy
    layer("ll-k-chaser", KT(KC, 90, small(langley(960, ll_base, sx, sy, pilot=True))), KC)
    layer("ll-k-chaser-empty", KT(KC, 90, small(langley(960, ll_base, sx, sy))), KC)
    sh_t, sh_b = K_AT(KC, 960 - 0.99 * sx, ll_base - 1.5 * sy, 90), K_AT(KC, 960 + 0.99 * sx, ll_base - 1.5 * sy, 90)
    layer("ll-k-brake", puff(sh_t[0] + 18, sh_t[1] - 6, 9) + puff(sh_b[0] + 18, sh_b[1] + 6, 9), KC)
    p_t, p_b = K_AT(KC, 960 - 1.0 * sx, ll_base, 90), K_AT(KC, 960 + 1.0 * sx, ll_base, 90)
    layer("ll-k-push", puff(p_t[0] - 16, p_t[1], 9) + puff(p_b[0] - 16, p_b[1], 9), KC)
    side = K_AT(KC, 960 - 1.06 * sx, ll_base - 0.9 * sy, 90)
    layer("ll-k-side", puff(side[0], side[1] - 16, 10), side)
    root = K_AT(KC, 960 - 1.06 * sx, ll_base - 0.6 * sy, 90)
    layer("ll-k-stuck", f'<path d="M{root[0] - 6:.1f} {root[1]:.1f} C{root[0] - 14:.1f} {root[1] - 40:.1f} {root[0] - 20:.1f} '
                        f'{root[1] - 80:.1f} {root[0] - 28:.1f} {root[1] - 120:.1f} L{root[0] - 12:.1f} {root[1] - 122:.1f} '
                        f'C{root[0] - 6:.1f} {root[1] - 80:.1f} {root[0] + 2:.1f} {root[1] - 40:.1f} {root[0] + 6:.1f} {root[1]:.1f} Z" '
                        f'fill="{GOLD_FILL}" stroke="{GOLD}" stroke-width="2" stroke-linejoin="round"/>', KC)
    # The tug as the stack's target, its ring's mouth at the target's (540), as big as the stack.
    tug_flip = f'<g transform="translate(0 1080) scale(1 -1)">{tug_up(960, 540, sx)}</g>'
    layer("sv-k-target", KT(tc, 90, tug_flip), KM)
    lp = K_AT(tc, 960 - 1.1 * sx + 7, 533, 90)
    layer("sv-k-light", f'<circle cx="{lp[0]:.1f}" cy="{lp[1]:.1f}" r="7" fill="{GOLD}" {LINE} stroke-width="2"/>', lp)
    corners = [K_AT(tc, 960 + s * 1.1 * sx, 540, 90) for s in (-1, 1)]
    layer("sv-k-latch", "".join(f'<circle cx="{x:.1f}" cy="{y:.1f}" r="9" fill="{GOLD}"/>' for x, y in corners), KM)
    # The tug's own launch, a little wider than the rocket: its aft end in an interstage on the rocket's top.
    kl = 104 / 6.6
    t_top = 500 - (TUG_L - 0.0) * kl
    inter = (f'<path d="M915 500 L908 {500 - 5.2 * kl:.1f} L1012 {500 - 5.2 * kl:.1f} L1005 500 Z" fill="#ffffff" {LINE} '
             f'stroke-width="3"/>')
    tug_l = tug_up(960, t_top, kl) + inter
    t_mid = (t_top + 500) / 2
    layer("sv-rocket", rocket[:nose] + tug_l, (960, 600))
    layer("sv-debris-1", tug_l, (960, t_mid))
    layer("sv-d-payload", f'<g transform="{big}"><g transform="{turn}">{tug_up(960, t_top, kl)}</g></g>', D_AT(914, 520))
    layer("sv-o-sat", f'<g transform="translate(960 560) scale(0.45) translate(-960 {-t_mid:.1f})">{thick(tug_up(960, t_top, kl))}</g>',
          (960, 560))
    # The burn to the high orbit on the map: the stack docked nose first into the tug's ring, upside down on it,
    # as big as the tug; in two, the stack (o-sat) and the tug (o-stage) in one frame, so the tug lets go
    # (hoi-separate.jsx).
    fb = (960, 500, kl, kl * 1.09)
    ring = 500 - TUG_L * kl
    nose_y = ring + 0.3 * kl
    c = nose_y + 500 - 3.45 * fb[3]
    stack = f'<g transform="translate(0 {c:.1f}) scale(1 -1)">{eor_stack(fb, lander=False)}</g>'
    top = nose_y - (3.45 - GE_BASE) * fb[3]
    b_mid = (top + 500) / 2
    on_map = lambda body: f'<g transform="translate(960 560) scale(0.42) translate(-960 {-b_mid:.1f})">{thick(body, 3.5)}</g>'
    layer("ge-o-btm", on_map(stack), (960, 560))
    layer("ge-o-btm-stage", on_map(tug_up(960, ring, kl)), (960, 560))
    # The spacewalks to the lander and back (crew-transfer.jsx with gemini-eor-transfer.jsx's numbers): the stack as
    # the way home's capsule (raised EV_UP, as gm-e-capsule), drawn nose left on the way out (mirrored about the
    # hatch's middle, so the lander is ahead of the figure) and nose right on the way back, moved on so the lander's
    # seat is where the figure starts. Gemini's hatch is the second one on both ways, the lander has none.
    EV_UP, xm = 23, (HATCH[0] + HATCH[1]) / 2
    pic = f'<g transform="translate(0 {-EV_UP})">{RT(small(eor_stack(GM_R)))}</g>'
    seat = RT_AT(960, 500 - (GE_BAY0 - 0.15 - LL_TOP + 1.0) * sy)[0]
    over = 2 * xm - seat - xm                                   # the seat's distance from the hatch, either way
    slope = math.degrees(math.atan2(2 * (0.655 * GM_R[2]), 2 * (1.77 * GM_R[3])))
    edge = lambda x: 380 + (x - 881) * math.tan(math.radians(slope))
    x0, x1 = HATCH[0], HATCH[1]
    hy = edge(x1)
    hole = (f'<g transform="rotate({slope:.2f} {x1} {hy:.1f})"><rect x="{x0 + 4}" y="{hy - 2:.1f}" width="{x1 - x0 - 8}" '
            f'height="10" fill="{NAVY}"/></g>')
    hatch = (f'<g transform="rotate({slope:.2f} {x1} {hy:.1f})"><rect x="{x0}" y="{hy - 8:.1f}" width="{x1 - x0}" '
             f'height="10" rx="3" fill="{GM_DARK}" {LINE} stroke-width="3"/></g>')
    layer("ge-e-blank", "", RC)
    layer("ge-e-capsule-out", mirror(pic, xm), RC)
    layer("ge-e-hole-out", mirror(hole, xm), (2 * xm - x1, hy))
    layer("ge-e-hatch-out", mirror(hatch, xm), (2 * xm - x1, hy))
    rail = 290
    bay_top = RC[1] - EV_UP - GE_B * RK * GM_R[2]
    end = xm + over - 30
    layer("ge-e-rail-out", f'<path d="M930 {rail} L{end:.1f} {rail} M960 {rail} L960 360 M{end - 14:.1f} {rail} '
                           f'L{end - 14:.1f} {bay_top:.1f}" fill="none" {LINE} stroke-width="4"/>', (960, rail))
    shift = f'translate({over:.1f} 0)'
    layer("ge-e-capsule-back", f'<g transform="{shift}">{pic}</g>', RC)
    a = math.radians(slope)
    hinge = (x1 + (x0 - x1) * math.cos(a) + over, hy + (x0 - x1) * math.sin(a))
    layer("ge-e-hole-back", f'<g transform="{shift}">{hole}</g>', hinge)
    layer("ge-e-hatch-back", f'<g transform="{shift}">{hatch}</g>', hinge)
    layer("ge-e-rail-back", f'<path d="M900 {rail} L{end:.1f} {rail} M930 {rail} L930 {bay_top:.1f} M{end - 14:.1f} {rail} '
                            f'L{end - 14:.1f} 362" fill="none" {LINE} stroke-width="4"/>', (960, rail))
    print("gemini-eor transfer: over", round(over, 1), "rail end", round(end, 1), "bay top", round(bay_top, 1))
    # In the lander's scenes (the ll- skin), 55 px/m, crewed: the whole lander is the descent stage, no ascent stage.
    layer("ll-n-descent", langley(960, 800, 55, 55, pilot=True, w=1.25), LC)
    layer("ll-n-ascent", "", LC)
    exit_y = 800 + 0.55 * 55
    plume = (f'<path d="M943 {exit_y:.1f} C945 {exit_y + 32:.1f} 951 {exit_y + 64:.1f} 960 {exit_y + 96:.1f} C969 {exit_y + 64:.1f} '
             f'975 {exit_y + 32:.1f} 977 {exit_y:.1f} Z" fill="{GOLD_FILL}" stroke="{GOLD}" stroke-width="3" stroke-linejoin="round"/>')
    layer("ll-n-plume", plume, (960, exit_y))
    layer("ll-n-flame-up", plume, (960, exit_y))

    # Gemini EOR to the Moon (2026-10-20, user). The flyby and the lunar-orbit flight carry no lander: their stack has its
    # frame empty (ge-*-empty), as the way home's trunk and the lander's target already draw it.
    empty = gm_shift(eor_stack(GM_L, lander=False), GM_L, GM_EQUIP_Y - GE_BASE)
    layer("ge-rocket-empty", rocket[:nose] + empty, (960, 600))
    layer("ge-debris-1-empty", empty, (960, mid))
    layer("ge-d-payload-empty", f'<g transform="{big}"><g transform="{turn}">{empty}</g></g>', D_AT(914, 520))
    layer("ge-o-sat-empty", f'<g transform="translate(960 560) scale(0.45) translate(-960 {-mid:.1f})">{thick(empty)}</g>', (960, 560))
    layer("ge-k-chaser-empty", KT(KC, 90, small(eor_stack(GM_R, lander=False))), KC)
    # On the map, the flame from the frame's open end (where the service module's engine fires through).
    bottom = 500 - GM_EQUIP_Y * GM_L[3]
    dy = 560 + 0.45 * (bottom - mid) - 600
    layer("ge-o-streak", f'<g transform="translate(0 {dy:.1f})"><path d="M944 600 C942 660 952 740 960 790 C968 740 978 660 976 600 Z" '
                         f'fill="{GOLD_FILL}" stroke="{GOLD}" stroke-width="3" stroke-linejoin="round"/></g>', (960, 560))
    # On the coasts' map, small at the Sun's place.
    for name, body in (("ge-m-probe", launch), ("ge-m-probe-empty", empty)):
        layer(name, f'<g transform="translate({SUN[0]} {SUN[1]}) scale(0.3) translate(-960 {-mid:.1f})">{thick(body)}</g>', SUN)
    # The trans-lunar injection: the stack docked nose first into the tug's ring, as the heat-shield test's burn to the
    # high orbit (ge-o-btm, empty), with the lander in its frame; the tug's flame at its bell.
    stack_l = f'<g transform="translate(0 {c:.1f}) scale(1 -1)">{eor_stack(fb)}</g>'
    layer("ge-o-btm-lander", on_map(stack_l), (960, 560))
    dy = 560 + 0.42 * (500 - b_mid) - 600
    layer("ge-o-streak-tug", f'<g transform="translate(0 {dy:.1f})"><path d="M944 600 C942 660 952 740 960 790 C968 740 978 660 976 600 Z" '
                             f'fill="{GOLD_FILL}" stroke="{GOLD}" stroke-width="3" stroke-linejoin="round"/></g>', (960, 560))
    # Coming home from the Moon there's no retrofire: the modules separation drops the service module, its empty frame and
    # the retrograde section together, from the heat shield's rim (as the direct-ascent craft's trunk).
    rim_s = 960 + GM_SHIELD * GM_R[2]
    layer("ge-r-pack-moon", "", RT_AT(1005, 500))
    layer("ge-r-trunk-moon", RT(small(eor_sm(GM_R) + eor_bay(GM_R) + gemini_retro(GM_R))), RT_AT(rim_s, 500))
    layer("ge-r-trunk-moon-cut-a", RT(f'<circle cx="{1920 - rim_s:.1f}" cy="500" r="6" fill="{GOLD}"/>'), RT_AT(1920 - rim_s, 500))
    layer("ge-r-trunk-moon-cut-b", RT(f'<circle cx="{rim_s:.1f}" cy="500" r="6" fill="{GOLD}"/>'), RT_AT(rim_s, 500))
    # The Langley lander on the Moon (lg-), at the moonwalk's 40 px/m, its pads on the ground (878): with its pilot, or empty
    # while the pilot is out; one stage, so at liftoff the whole lander goes (n-ascent) and nothing stays (n-descent empty).
    # No ladder: the pilot steps off the open platform (langley-walk.jsx). On the map, alone, for its climb back to the stack.
    lb = 878 - 0.75 * 40
    layer("lg-n-descent", langley(960, lb, 40, 40, pilot=True), LC)
    layer("lg-n-descent-empty", langley(960, lb, 40, 40), LC)
    layer("lg-n-whole", langley(960, lb, 40, 40, pilot=True), LC)
    for name in ("lg-n-ascent", "lg-n-ladder", "lg-none"):
        layer(name, "", LC)
    lex = lb + 0.55 * 40
    lplume = (f'<path d="M946 {lex:.1f} C948 {lex + 26:.1f} 953 {lex + 52:.1f} 960 {lex + 78:.1f} C967 {lex + 52:.1f} '
              f'972 {lex + 26:.1f} 974 {lex:.1f} Z" fill="{GOLD_FILL}" stroke="{GOLD}" stroke-width="3" stroke-linejoin="round"/>')
    layer("lg-n-plume", lplume, (960, lex))
    layer("lg-n-flame-up", lplume, (960, lex))
    layer("lg-o-sat", f'<g transform="translate(960 560) scale(0.9) translate(-960 -500)">{thick(langley(960, 530, 34, 34, pilot=True), 4)}</g>',
          (960, 560))
    oy = 560 + 0.9 * (530 + 0.55 * 34 - 500) - 600
    layer("lg-o-streak", f'<g transform="translate(0 {oy:.1f})"><path d="M944 600 C942 660 952 740 960 790 C968 740 978 660 976 600 Z" '
                         f'fill="{GOLD_FILL}" stroke="{GOLD}" stroke-width="3" stroke-linejoin="round"/></g>', (960, 560))


# The LK-700 to the Moon (2026-10-20, user). BASPM gives every step from the parking orbit on to the craft, so its
# "spacecraft" is the whole complex (Wikipedia's 21.2 m and 154 t): under the craft a trans-lunar stage, which burns for
# the Moon and lets go; the landing stage then brakes into lunar orbit and lands (the braking stage folded into it); the
# return stage lifts off with the capsule and fires home. The trans-lunar stage's shape is ours, unconfirmed. On the Moon
# (l7m-) the craft stands on its six legs (two seen), at the moonwalk's scale, the landing stage's top at the porch.
L7_TL = 4.6                          # the trans-lunar stage below the craft's foot: its interstage and body, the bell under


def l7_tl_stage(foot, k):
    """The trans-lunar stage under the craft, its interstage's top at the landing stage's foot (y foot)."""
    r, ri = 1.45 * k, L7_W / 2 * k
    a, b = foot + 0.6 * k, foot + L7_TL * k
    return (f'<path d="M{960 - ri:.1f} {foot:.1f} L{960 - r:.1f} {a:.1f} L{960 + r:.1f} {a:.1f} L{960 + ri:.1f} {foot:.1f} Z" '
            f'fill="#ffffff" {LINE} stroke-width="2.4"/>'
            f'<rect x="{960 - r:.1f}" y="{a:.1f}" width="{2 * r:.1f}" height="{b - a:.1f}" fill="#ffffff" {LINE} stroke-width="2.6"/>'
            + "".join(f'<path d="M{960 - r:.1f} {a + (b - a) * f:.1f} L{960 + r:.1f} {a + (b - a) * f:.1f}" fill="none" {LINE} '
                      f'stroke-width="1.2" opacity="0.5"/>' for f in (0.3, 0.65))
            + f'<path d="M{960 - 0.4 * k:.1f} {b:.1f} L{960 - 0.65 * k:.1f} {b + 0.7 * k:.1f} L{960 + 0.65 * k:.1f} {b + 0.7 * k:.1f} '
              f'L{960 + 0.4 * k:.1f} {b:.1f} Z" fill="#ffffff" {LINE} stroke-width="2.2"/>')


def lk700_moon_parts():
    K = 33.8
    thick = lambda body: re.sub(r'stroke-width="[\d.]+"', 'stroke-width="5"', body)
    big = f"translate({DX} {DY}) scale({DK}) translate(-850 -520)"
    turn = "translate(-120 20) rotate(90 960 500)"
    mid = 500 - L7_LEN * K / 2
    on_map = lambda body: f'<g transform="translate(960 560) scale(0.6) translate(-960 {-mid:.1f})">{thick(body)}</g>'

    def streak(name, tail):
        dy = 560 + 0.6 * (tail - mid) - 600
        layer(name, f'<g transform="translate(0 {dy:.1f})"><path d="M944 600 C942 660 952 740 960 790 C968 740 978 660 976 600 Z" '
                    f'fill="{GOLD_FILL}" stroke="{GOLD}" stroke-width="3" stroke-linejoin="round"/></g>', (960, 560))
    # The complex on the map (l7-o-sat's frame, so the craft alone overlays it) and in two for the burn; out of the rocket.
    layer("l7-o-sat-tl", on_map(l7_stack(500, K) + l7_tl_stage(500, K)), (960, 560))
    layer("l7-o-sat-tlstage", on_map(l7_tl_stage(500, K)), (960, 560))
    streak("l7-o-streak-tl", 500 + (L7_TL + 0.7) * K)
    streak("l7-o-streak", 500 + 0.45 * K)
    tl_d = l7_stack(500 - (L7_TL + 0.7) * K, K) + l7_tl_stage(500 - (L7_TL + 0.7) * K, K)
    layer("l7-d-payload-tl", f'<g transform="{big}"><g transform="{turn}">{tl_d}</g></g>', D_AT(914, 520))
    # After the Moon: the return stage and the capsule, with the return stage's own small engine (inside the landing
    # stage until then).
    ret_top = 500 - L7_LAND * K - L7_RET * K
    asc_bell = (f'<path d="M{960 - 0.22 * K:.1f} {500 - L7_LAND * K:.1f} L{960 - 0.38 * K:.1f} {500 - L7_LAND * K + 0.5 * K:.1f} '
                f'L{960 + 0.38 * K:.1f} {500 - L7_LAND * K + 0.5 * K:.1f} L{960 + 0.22 * K:.1f} {500 - L7_LAND * K:.1f} Z" fill="#ffffff" '
                f'{LINE} stroke-width="2.2"/>')
    asc = asc_bell + l7_stack(500 - L7_LAND * K, K, landing=False)
    layer("l7-o-sat-asc", on_map(asc), (960, 560))
    streak("l7-o-streak-asc", 500 - L7_LAND * K + 0.5 * K)
    layer("l7-none", "", (960, 560))
    for name, body, m in (("l7-m-probe", l7_stack(500, K), mid), ("l7-m-probe-asc", asc, (ret_top + 500 - L7_LAND * K) / 2)):
        layer(name, f'<g transform="translate({SUN[0]} {SUN[1]}) scale(0.32) translate(-960 {-m:.1f})">{thick(body)}</g>', SUN)
    # On the Moon, 40 px/m: the landing stage's top at the porch (722), its legs out to pads on the ground (876).
    k = 40
    land_top = 722
    legs = (f'<path d="M908 734 L836 874 M1012 734 L1084 874 M916 760 L870 850 M1004 760 L1050 850" fill="none" {LINE} '
            f'stroke-width="4"/>'
            f'<ellipse cx="834" cy="876" rx="16" ry="5" fill="#ffffff" {LINE} stroke-width="3"/>'
            f'<ellipse cx="1086" cy="876" rx="16" ry="5" fill="#ffffff" {LINE} stroke-width="3"/>')
    layer("l7m-n-descent", legs + l7_landing(land_top, k, legs=None), LC)
    layer("l7m-n-ascent", l7_stack(land_top, k, landing=False), LC)
    exit_y = land_top + (L7_LAND + 0.45) * k
    layer("l7m-n-plume", f'<path d="M942 {exit_y:.1f} C944 {exit_y + 30:.1f} 951 {exit_y + 60:.1f} 960 {exit_y + 90:.1f} C969 {exit_y + 60:.1f} '
                         f'976 {exit_y + 30:.1f} 978 {exit_y:.1f} Z" fill="{GOLD_FILL}" stroke="{GOLD}" stroke-width="3" '
                         f'stroke-linejoin="round"/>', (960, exit_y))


# The Soviet craft to the Moon (2026-10-20, user). BASPM gives the Blok D, the stage on the craft's tail, to the rocket
# (@rocket-1), and the steps say what it does: the 7K-L1's burns for the Moon and lets go; the 7K-L3's LOK rides on it to
# the Moon, with the LK between them on a landing (the N1's stack), and it brakes them into lunar orbit. On the map
# (0.6 of the launch's size, as the Earth-orbit tests' parts), in the deployment, on the coasts' map (small at the Sun's
# place); the flames from the Blok D's bell. The LK on the Moon (lkm-) stands on its legs, at the moonwalk's scale, with a
# ladder down its right leg. Unconfirmed.
def soviet_moon_parts():
    K = 33.8
    thick = lambda body: re.sub(r'stroke-width="[\d.]+"', 'stroke-width="5"', body)
    on_map = lambda body, mid=426: f'<g transform="translate(960 560) scale(0.6) translate(-960 {-mid:.1f})">{thick(body)}</g>'
    coast = lambda body, mid: f'<g transform="translate({SUN[0]} {SUN[1]}) scale(0.32) translate(-960 {-mid:.1f})">{thick(body)}</g>'
    big = f"translate({DX} {DY}) scale({DK}) translate(-850 -520)"
    turn = "translate(-120 20) rotate(90 960 500)"
    deploy = lambda body: f'<g transform="{big}"><g transform="{turn}">{body}</g></g>'

    def streak(name, tail, mid=426):
        dy = 560 + 0.6 * (tail - mid) - 600
        layer(name, f'<g transform="translate(0 {dy:.1f})"><path d="M944 600 C942 660 952 740 960 790 C968 740 978 660 976 600 Z" '
                    f'fill="{GOLD_FILL}" stroke="{GOLD}" stroke-width="3" stroke-linejoin="round"/></g>', (960, 560))
    # The 7K-L1 on its Blok D: out of the rocket, in the parking orbit, and in two for the burn (s1-o-sat over the stage).
    l1_len = 2.24 + 2.26
    l1 = soyuz_stack(500 - l1_len * K, K, nose="none", dish=True)
    l1_on_d = soyuz_stack(500 - (l1_len + BLOK_D_H) * K, K, nose="none", dish=True) + blok_d(500 - BLOK_D_H * K, K)
    layer("s1-d-payload-stack", deploy(l1_on_d), D_AT(914, 520))
    layer("s1-o-sat-stage", on_map(l1 + blok_d(500, K)), (960, 560))
    layer("s1-o-sat-dstage", on_map(blok_d(500, K)), (960, 560))
    streak("bd-o-streak", 500 + BLOK_D_H * K)
    layer("s1-m-probe", coast(l1, 500 - l1_len * K / 2), SUN)
    # The 7K-LOK alone, on its Blok D, and the N1's stack (the Blok D, the LK on it, the LOK on the LK), as lok_lk_parts.
    lok_l = 0.45 + 2.98 + 2.24 + LOK_SM
    lok = soyuz_stack(500 - lok_l * K, K, panels=False, sm_h=LOK_SM)
    layer("lok-m-probe", coast(lok, 500 - lok_l * K / 2), SUN)
    layer("lok-m-probe-stage", coast(lok + blok_d(500, K), 500 - (lok_l - BLOK_D_H) * K / 2), SUN)

    def n1(bottom, k):
        y = bottom - BLOK_D_H * k
        plate = y - LK_H * k
        body = soyuz_stack(plate - 0.3 * k - lok_l * k, k, panels=False, sm_h=LOK_SM) + lk_up(y, k) + blok_d(y, k)
        return body, (bottom + plate - lok_l * k) / 2
    k = 33.8 * 0.6
    body, mid = n1(500, k)
    streak("n1-o-streak", 500, mid)
    layer("lok-m-probe-n1", coast(body, mid), SUN)
    KL = 33.8 * (0.45 + 2.98 + 2.24 + 2.26) / lok_l * 0.75
    stack_d, _ = n1(500, KL)
    layer("lok-d-payload-n1", deploy(stack_d), D_AT(914, 520))
    # The LK on the Moon.
    layer("lkm-n-descent", lk_lpu(legs=True), LC)
    layer("lkm-n-ascent", lk_lva(), LC)
    layer("lkm-n-flame-up", f'<path d="M948 802 C950 822 955 840 960 858 C965 840 970 822 972 802 Z" fill="{GOLD_FILL}" '
                            f'stroke="{GOLD}" stroke-width="3" stroke-linejoin="round"/>', (960, 802))
    layer("lkm-n-plume", f'<path d="M944 802 C946 830 952 860 960 890 C968 860 974 830 976 802 Z" fill="{GOLD_FILL}" '
                         f'stroke="{GOLD}" stroke-width="3" stroke-linejoin="round"/>', (960, 802))
    (x0, y0), (x1, y1) = (1028, 786), (1100, 872)
    nx, ny = (y1 - y0), -(x1 - x0)
    n = math.hypot(nx, ny)
    nx, ny = 8 * nx / n, 8 * ny / n
    rungs = "".join(f"M{x0 + (x1 - x0) * j / 5 - nx:.1f} {y0 + (y1 - y0) * j / 5 - ny:.1f} "
                    f"L{x0 + (x1 - x0) * j / 5 + nx:.1f} {y0 + (y1 - y0) * j / 5 + ny:.1f} " for j in range(1, 5))
    layer("lkm-n-ladder", f'<path d="M{x0 + nx:.1f} {y0 + ny:.1f} L{x1 + nx:.1f} {y1 + ny:.1f} {rungs}" fill="none" {LINE} '
                          f'stroke-width="2.4"/>', LC)


# --- The robot probes as themselves (2026-10-21, user; the standard pack) -----------------------------------------
# Each probe drawn once in the launch's frame, nose up, its foot on the stage's top (500): folded on the rocket and in the
# deployment (its panels and booms up along it), and open in orbit, on the coasts' and cruises' maps and at the Earth
# departure (probe_skin). On the rocket it rides under the generic nose fairing, which the ascent throws off as Mercury's
# tower (<p>-a-les), the probe standing bare on its adapter (<p>-debris-1-bare): the pad's clips stay generic, the
# fairing there being the generic nose. Shapes from the blueprints and photographs, drawn to fit the generic sprite (a
# probe of a few decimetres as large as one of a few metres; panels and booms shortened): unconfirmed.


def _r(x, y, w, h, sw=2.4, fill="#ffffff", extra=""):
    return f'<rect x="{x}" y="{y}" width="{w}" height="{h}" fill="{fill}" {LINE} stroke-width="{sw}"{extra}/>'


def _p(d, sw=2.4, fill="#ffffff", op=1.0):
    return f'<path d="{d}" fill="{fill}" {LINE} stroke-width="{sw}"' + (f' opacity="{op}"' if op != 1.0 else "") + "/>"


def _l(d, sw=1.8, op=1.0):
    return _p(d, sw, "none", op)


def _c(cx, cy, r, sw=2.4, fill="#ffffff"):
    return f'<circle cx="{cx}" cy="{cy}" r="{r}" fill="{fill}" {LINE} stroke-width="{sw}"/>'


def _panel(x, y, w, h, n=3):
    """A solar panel: hatched, n cells along its long side."""
    cells = "".join((f"M{x + w * j / n:.1f} {y} L{x + w * j / n:.1f} {y + h} " if w >= h else
                     f"M{x} {y + h * j / n:.1f} L{x + w} {y + h * j / n:.1f} ") for j in range(1, n))
    return _r(x, y, w, h, 2.2, "url(#hatch)") + (_l(cells, 1.2, 0.7) if cells else "")


def _bowl(cx, y, w, depth):
    """A dish opening up, its rim at y, and its feed."""
    return (_p(f"M{cx - w / 2} {y} Q{cx} {y + 2 * depth} {cx + w / 2} {y} Z")
            + _l(f"M{cx} {y + depth} L{cx} {y - depth * 1.4}", 1.6))


def _side_dish(cx, cy, h, depth, facing):
    """A dish seen edge on, facing left (-1) or right (1), its back at cx."""
    return _p(f"M{cx} {cy - h / 2} Q{cx - facing * 2 * depth} {cy} {cx} {cy + h / 2} Z") + _l(
        f"M{cx} {cy} L{cx + facing * depth * 1.6} {cy}", 1.4)


def _oct(y0, y1, x0=932, x1=988):
    c = 6
    return _p(f"M{x0 + c} {y0} L{x1 - c} {y0} L{x1} {y0 + c} L{x1} {y1 - c} L{x1 - c} {y1} L{x0 + c} {y1} L{x0} {y1 - c} "
              f"L{x0} {y0 + c} Z") + _l(f"M{x0 + 14} {y0} L{x0 + 14} {y1} M{x1 - 14} {y0} L{x1 - 14} {y1}", 1.2, 0.6)


def _wings(y, h, reach, n=4, gap=4, x0=932, x1=988):
    """Two panels straight out to the sides (open)."""
    return _panel(x0 - gap - reach, y, reach, h, n) + _panel(x1 + gap, y, reach, h, n) + _l(
        f"M{x0 - gap} {y + h / 2} L{x0} {y + h / 2} M{x1} {y + h / 2} L{x1 + gap} {y + h / 2}", 2)


def _folded(y0, y1, w=10, xl=920, xr=990):
    """Two panels folded up along the body (on the rocket)."""
    return _panel(xl, y0, w, y1 - y0, 3) + _panel(xr, y0, w, y1 - y0, 3)


def _adapter():
    return _p("M946 490 L974 490 L978 500 L942 500 Z")


def _e8_stage(legs_open):
    """The Lunas' landing stage (E-8): four spherical tanks in a ring, the cylindrical ones between, four legs."""
    out = _r(934, 470, 52, 12, 2.2) + _c(938, 480, 11) + _c(982, 480, 11) + _c(960, 484, 10)
    if legs_open:
        out += _l("M932 486 L908 504 M988 486 L1012 504 M950 490 L944 508 M970 490 L976 508", 2.6)
        out += "".join(f'<ellipse cx="{x}" cy="{y}" rx="8" ry="2.5" fill="#ffffff" {LINE} stroke-width="2"/>'
                       for x, y in ((906, 506), (1014, 506), (943, 510), (977, 510)))
    else:
        out += _l("M932 486 L926 470 M988 486 L994 470", 2.4)
    return out


def pioneer10(open_):
    out = (_r(944, 456, 32, 26) + _l("M952 456 L952 482 M968 456 L968 482", 1.2, 0.6)
           + _bowl(960, 444, 84, 12) + _l("M934 446 L960 414 L986 446", 1.4) + _r(955, 406, 10, 8, 1.8))
    if open_:
        out += (_l("M944 470 L896 510 M976 470 L1024 510", 2) + _r(884, 504, 14, 24, 2) + _r(1022, 504, 14, 24, 2)
                + _l("M976 462 L1096 432", 1.6) + _c(1098, 431, 4, 1.6))
    else:
        out += _r(926, 458, 10, 26, 2) + _r(984, 458, 10, 26, 2)
    return out


def pioneer2(open_):
    return (_r(930, 446, 60, 26) + _l("M930 459 L990 459", 1.2, 0.6) + _p("M930 446 L946 424 L974 424 L990 446 Z")
            + _p("M930 472 L946 488 L974 488 L990 472 Z") + _p("M954 488 L950 497 L970 497 L966 488 Z", 2)
            + "".join(_p(f"M{x - 3} 424 L{x - 4} 417 L{x + 4} 417 L{x + 3} 424 Z", 1.6) for x in (948, 960, 972)))


def pioneer4(open_):
    stripes = "".join(f'<path d="M960 402 L{x0} 486 L{x1} 486 Z" fill="{NAVY}" opacity="0.35"/>' for x0, x1 in ((946, 952), (958, 964), (970, 975)))
    return (_p("M944 486 L960 402 L976 486 Z") + stripes + _r(944, 486, 32, 8, 2) + _l("M960 402 L960 388", 1.6))


def pioneer6(open_):
    out = (_r(930, 444, 60, 40, 2.4, "url(#hatch)") + _l("M930 450 L990 450", 1.4, 0.7) + _r(950, 484, 20, 8, 2)
           + _l("M960 444 L960 384", 2.2) + _l("M952 400 L968 400 M954 412 L966 412", 1.4))
    if open_:
        out += _l("M930 466 L846 486 M990 466 L1074 486", 1.8) + _c(844, 487, 3.5, 1.6) + _c(1076, 487, 3.5, 1.6)
    else:
        out += _l("M928 472 L928 420 M992 472 L992 420", 1.6)
    return out


def pv_drum():
    return (_r(918, 452, 84, 34, 2.4, "url(#hatch)") + _r(918, 446, 84, 6, 2) + _l("M932 452 L932 486 M988 452 L988 486", 1.2, 0.6))


def pv_orbiter(open_):
    out = pv_drum() + _p("M952 486 L948 498 L972 498 L968 486 Z", 2) + _l("M960 446 L960 404", 2) + _side_dish(962, 412, 36, 8, -1)
    out += _l("M1002 466 L1120 466", 1.6) + _c(1122, 466, 3.5, 1.6) if open_ else _l("M1004 486 L1004 432", 1.6)
    return out


def pv_multiprobe(open_):
    return (pv_drum() + _p("M940 446 C940 428 950 414 960 414 C970 414 980 428 980 446 Z") + _l("M942 438 L978 438", 1.4, 0.6)
            + _p("M922 446 Q922 432 931 432 Q940 432 940 446 Z", 2) + _p("M980 446 Q980 432 989 432 Q998 432 998 446 Z", 2))


def ranger(open_):
    out = (_r(934, 470, 52, 16) + _p("M942 470 L954 406 L966 406 L978 470 Z") + _l("M946 450 L974 450 M950 428 L970 428", 1.2, 0.6)
           + _c(960, 397, 9))
    if open_:
        out += (_wings(472, 14, 96, 4, 4, 934, 986) + _l("M944 486 L918 508", 2)
                + f'<ellipse cx="910" cy="514" rx="22" ry="7" transform="rotate(-30 910 514)" fill="#ffffff" {LINE} stroke-width="2.2"/>')
    else:
        out += _folded(412, 468, 11, 921, 988) + _p("M948 486 Q960 494 972 486 Z", 2)
    return out


def lunar_orbiter(open_):
    out = (_p("M928 488 L944 446 L976 446 L992 488 Z") + _l("M936 468 L984 468", 1.2, 0.6) + _p("M948 446 Q960 426 972 446 Z")
           + _p("M956 432 L952 420 L968 420 L964 432 Z", 2))
    if open_:
        out += (_panel(858, 486, 68, 10, 3) + _panel(994, 486, 68, 10, 3) + _l("M932 470 L874 452", 1.8)
                + _side_dish(872, 452, 34, 7, -1) + _l("M988 466 L1052 438", 1.6) + _r(1050, 430, 10, 8, 1.6))
    else:
        out += _folded(436, 486, 9, 918, 993)
    return out


def _mariner(top, open_, reach=92, n=4, two=False):
    """A Mariner's octagonal bus (its top at top) and its panels: four (two seen) or two."""
    out = _oct(top, top + 30)
    if open_:
        out += _wings(top + 10, 10, reach, n)
    else:
        out += _folded(top - 56, top + 2, 10, 920, 990)
    return out


def mariner5(open_):
    return (_mariner(456, open_) + _l("M960 456 L960 398", 2) + _r(955, 392, 10, 6, 1.6) + _side_dish(984, 448, 20, 5, 1)
            + _r(952, 486, 16, 6, 2))


def mariner6(open_):
    return (_mariner(458, open_) + _bowl(960, 452, 56, 9) + _l("M984 458 L984 380", 2) + _r(950, 488, 20, 10, 2))


def mariner9(open_):
    return (_mariner(460, open_) + _p("M940 460 L952 430 L968 430 L980 460 Z") + _p("M956 430 L953 418 L967 418 L964 430 Z", 2)
            + _l("M978 460 L990 386", 2) + _side_dish(996, 452, 22, 5, 1) + _r(950, 490, 20, 8, 2))


def mariner10(open_):
    out = _oct(456, 486) + _l("M960 456 L960 428", 2) + _bowl(960, 424, 38, 6) + _r(948, 486, 24, 10, 2)
    if open_:
        out += _wings(466, 12, 74, 3) + _l("M988 474 L1088 516", 1.6) + _c(1090, 517, 3.5, 1.6)
    else:
        out += _folded(400, 458, 10, 920, 990) + _l("M990 484 L990 462", 1.4)
    return out


def viking(open_):
    out = (_oct(462, 490) + _p("M936 462 L948 428 L972 428 L984 462 Z") + _l("M942 446 L978 446", 1.4, 0.6)
           + _r(954, 420, 12, 8, 2))
    if open_:
        out += _wings(470, 10, 100, 4) + _side_dish(1000, 484, 26, 6, 1) + _l("M940 462 L930 404", 1.8)
    else:
        out += _folded(404, 464, 10, 918, 992)
    return out


def surveyor(open_):
    out = (_l("M930 486 L990 486 L960 440 Z M944 464 L976 464", 2.2) + _c(960, 474, 13) + _l("M960 440 L960 402", 2.2)
           + _r(934, 470, 18, 12, 2) + _r(972, 472, 16, 10, 2))
    if open_:
        out += (_l("M934 484 L898 512 M986 484 L1022 512 M960 488 L960 516", 2.4)
                + "".join(f'<ellipse cx="{x}" cy="{y}" rx="9" ry="3" fill="#ffffff" {LINE} stroke-width="2"/>'
                          for x, y in ((896, 514), (1024, 514), (960, 518)))
                + _panel(904, 384, 50, 30, 3) + _l("M954 400 L960 404", 1.6) + _r(970, 380, 30, 30, 2) + _l("M960 404 L970 398", 1.6))
    else:
        out += _l("M930 486 L924 452 M990 486 L996 452", 2.2) + _panel(940, 404, 18, 30, 2) + _r(962, 404, 18, 30, 2)
    return out


def luna2(open_):
    reach = 1.0 if open_ else 0.4
    whips = "".join(f"M{960 + s * dx} {y} L{960 + s * (dx + ex * reach):.0f} {y - 40 * (0.6 + 0.4 * reach):.0f} "
                    for s in (-1, 1) for dx, ex, y in ((16, 30, 430), (10, 18, 426)))
    return (_l(whips, 1.6) + _c(960, 446, 24) + _l("M936 446 Q960 456 984 446", 1.2, 0.7) + _l("M960 422 L960 386", 1.8)
            + _c(960, 384, 3, 1.6) + _r(950, 470, 20, 8, 2))


def luna3(open_):
    return (_r(940, 430, 40, 44, 2.4, "url(#hatch)") + _p("M940 430 Q940 410 960 408 Q980 410 980 430 Z")
            + _p("M940 474 Q940 490 960 492 Q980 490 980 474 Z") + _c(960, 420, 4, 1.6)
            + _l("M946 414 L930 392 M974 414 L990 392 M960 408 L960 390", 1.6))


def luna15(open_):
    return (_e8_stage(open_) + _r(948, 440, 24, 30, 2.2) + _c(944, 462, 6, 1.8) + _c(976, 462, 6, 1.8)
            + _c(960, 428, 10) + _l("M954 420 L944 404 M966 420 L976 404", 1.4))


def luna17(open_):
    tub = _p("M936 456 L984 456 L978 440 L942 440 Z") + _p("M940 440 Q960 430 980 440", 2, "none")
    wheels = "".join(_c(x, 462, 4, 1.6) for x in (942, 954, 966, 978))
    return _e8_stage(open_) + _l("M932 468 L916 482 M988 468 L1004 482", 2) + tub + wheels


def mars2(open_):
    out = (_r(946, 444, 28, 36) + _r(938, 480, 44, 10, 2) + _p("M924 444 Q960 408 996 444 Z") + _l("M930 438 L990 438", 1.2, 0.6))
    if open_:
        out += _wings(452, 12, 84, 4, 4, 946, 974) + _l("M946 470 L934 470", 2) + _side_dish(932, 470, 40, 9, -1)
    else:
        out += _folded(446, 492, 9, 914, 997)
    return out


def mars_m69(open_):
    out = (_r(942, 446, 36, 40) + _p("M942 446 L956 406 Q960 400 964 406 L978 446 Z") + _c(934, 480, 9) + _c(986, 480, 9))
    if open_:
        out += _wings(454, 12, 84, 4, 6, 942, 978) + _side_dish(990, 432, 40, 9, 1) + _l("M978 432 L990 432", 2)
    else:
        out += _folded(400, 448, 9, 922, 989)
    return out


def venera15(open_):
    w = 54 if open_ else 36
    sar = _p(f"M{960 - w} 418 Q960 444 {960 + w} 418 Q960 432 {960 - w} 418 Z")
    out = _r(946, 436, 28, 50) + _l("M946 470 L974 470", 1.2, 0.6) + sar + _l("M960 428 L960 436", 2)
    if open_:
        out += _wings(452, 12, 92, 4, 4, 946, 974) + _side_dish(978, 476, 32, 8, 1)
    else:
        out += _folded(440, 490, 9, 924, 987)
    return out


def venera7(open_):
    out = _r(946, 440, 28, 30) + _bowl(960, 436, 50, 8) + _c(960, 482, 14) + _l("M946 482 Q960 488 974 482", 1.2, 0.6)
    if open_:
        out += _wings(448, 12, 82, 3, 4, 946, 974)
    else:
        out += _folded(404, 470, 10, 922, 988)
    return out


def venera9(open_):
    out = (f'<ellipse cx="960" cy="428" rx="34" ry="5" fill="#ffffff" {LINE} stroke-width="2"/>' + _c(960, 416, 24)
           + _l("M938 410 Q960 400 982 410", 1.2, 0.6) + _r(948, 442, 24, 36)
           + _p("M940 478 L980 478 L974 492 L946 492 Z", 2))
    if open_:
        out += _wings(450, 12, 84, 4, 4, 948, 972) + _side_dish(984, 466, 28, 7, 1)
    else:
        out += _folded(446, 492, 9, 928, 984)
    return out


# prefix: (drawing, its middle's y when open, its width open: the sprites are scaled to fit)
PROBES = {
    "pio-": (pioneer10, 462, 220), "pa2-": (pioneer2, 458, 70), "pi4-": (pioneer4, 442, 40), "pi6-": (pioneer6, 440, 230),
    "pvo-": (pv_orbiter, 452, 210), "pvm-": (pv_multiprobe, 456, 90), "rng-": (ranger, 450, 270),
    "lor-": (lunar_orbiter, 460, 210), "mr5-": (mariner5, 456, 250), "mr6-": (mariner6, 452, 250),
    "mr9-": (mariner9, 452, 250), "mr10-": (mariner10, 460, 210), "vik-": (viking, 456, 270),
    "svy-": (surveyor, 450, 130), "lu2-": (luna2, 436, 100), "lu3-": (luna3, 450, 64), "lu15-": (luna15, 460, 110),
    "lu17-": (luna17, 470, 110), "ma2-": (mars2, 452, 230), "m69-": (mars_m69, 450, 230), "ve15-": (venera15, 448, 230),
    "ve7-": (venera7, 450, 220), "ve9-": (venera9, 450, 220),
}


def probe_skin_parts():
    big = f"translate({DX} {DY}) scale({DK}) translate(-850 -520)"
    turn = "translate(-120 20) rotate(90 960 500)"
    thick = lambda body, w: re.sub(r'stroke-width="[\d.]+"', f'stroke-width="{w}"', body)
    fairing = board_group("debris-1")
    for p, (draw, mid, width) in PROBES.items():
        folded, opened = draw(False), draw(True)
        layer(f"{p}a-les", fairing, (960, 300))
        layer(f"{p}debris-1-bare", _adapter() + folded, (960, 420))
        layer(f"{p}d-payload", f'<g transform="{big}"><g transform="{turn}">{folded}</g></g>', D_AT(914, 520))
        k = min(1.0, 200 / width)
        for name in ("o-sat", "o-sat-b"):
            layer(f"{p}{name}", f'<g transform="translate(960 560) scale({0.6 * k:.3f}) translate(-960 -{mid})">{thick(opened, 5)}</g>',
                  (960, 560))
        cx, cy = SUN
        layer(f"{p}m-probe", f'<g transform="translate({cx} {cy}) scale({0.32 * min(1.0, 120 / width):.3f}) translate(-960 -{mid})">'
                             f'{thick(opened, 6)}</g>', SUN)
        gx, gy = G
        layer(f"{p}g-probe", f'<g transform="translate({gx} {gy}) scale({GK * min(1.0, 120 / width):.3f}) rotate(90) '
                             f'translate(-960 -{mid})">{thick(opened, 3)}</g>', G)
        layer(f"{p}g-boom", "", G)
        layer(f"{p}g-dish", "", G)
    # Luna 15's way home: the return rocket alone (its cylinder of tanks and the sphere on top) on the coast home and the
    # Earth approach, and the return sphere (about 50 cm, its two whips) for the reentry and the landing under its chute,
    # where the generic capsule is.
    ret = _r(948, 440, 24, 30, 2.2) + _c(944, 462, 6, 1.8) + _c(976, 462, 6, 1.8) + _c(960, 428, 10) + _l(
        "M954 420 L944 404 M966 420 L976 404", 1.4) + _p("M954 470 L950 480 L970 480 L966 470 Z", 2)
    layer("lu15-o-sat-ret", f'<g transform="translate(960 560) scale(0.9) translate(-960 -442)">{thick(ret, 5)}</g>', (960, 560))
    cx, cy = SUN
    layer("lu15-m-probe-ret", f'<g transform="translate({cx} {cy}) scale(0.4) translate(-960 -442)">{thick(ret, 6)}</g>', SUN)
    small = lambda body: re.sub(r'stroke-width="([\d.]+)"', lambda m: f'stroke-width="{float(m.group(1)) * 0.8:.2f}"', body)
    sphere = (f'<circle cx="960" cy="446" r="54" fill="#ffffff" {LINE} stroke-width="3"/>'
              f'<path d="M906 452 Q960 470 1014 452" fill="none" {LINE} stroke-width="2" opacity="0.7"/>'
              f'<path d="M944 394 L930 352 M976 394 L990 352" fill="none" {LINE} stroke-width="2.4"/>'
              f'<rect x="952" y="384" width="16" height="10" rx="2" fill="#ffffff" {LINE} stroke-width="2.2"/>'
              f'<path d="M960 384 L960 352" fill="none" {LINE} stroke-width="2"/>')
    layer("lu15-r-capsule", RT(small(sphere)), RC)
    layer("lu15-r-capsule-up", RU(small(sphere)), RC)
    layer("lu15-r-half-nose", RT(small(f'<path d="M906 446 A54 54 0 0 1 1014 446 Z" fill="#ffffff" {LINE} stroke-width="3"/>')),
          RT_AT(960, 410))
    layer("lu15-r-half-body", RT(small(f'<path d="M906 450 A54 54 0 0 0 1014 450 Z" fill="#ffffff" {LINE} stroke-width="3"/>')),
          RT_AT(960, 472))


def main():
    os.makedirs(OUT, exist_ok=True)
    copy_board_layers()
    pad_parts()
    ascent_parts()
    deployment_parts()
    orbit_parts()
    return_parts()
    time_parts()
    recovery_parts()
    map_parts()
    planet_parts()
    moon_parts()
    way_parts()
    dock_parts()
    eva_parts()
    lunar_parts()
    moonwalk_kit_parts()
    lrv_parts()
    surveyor_parts()
    lunokhod_parts()
    stage_parts()
    plane_parts()
    probe_parts()
    multiprobe_parts()
    carrier_entry_parts()
    planet_entry_parts()
    viking_parts()
    prop_m_parts()
    x15_parts()
    e154_parts()
    planet_descent_parts()
    air_parts()
    departure_parts()
    mercury_parts()
    vostok_parts()
    soyuz_parts()
    apollo_parts()
    lm_parts()
    satellite_parts()
    ofo_bios_parts()
    pegasus_parts()
    probe_skin_parts()
    gemini_parts()
    gemini_eor_parts()
    lk700_parts()
    soviet_moon_parts()
    lk700_moon_parts()
    if ONLY:
        # Keep the parts not redrawn.
        old = json.load(open(os.path.join(BUILD, "layers.json")))
        old.update(layers)
        layers.clear()
        layers.update(old)
    with open(os.path.join(BUILD, "layers.json"), "w") as f:
        json.dump(layers, f, indent=1)
    print(len(layers), "layers")


if __name__ == "__main__":
    main()
