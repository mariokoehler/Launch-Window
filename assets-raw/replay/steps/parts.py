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
    # The payload, small, nose up (it turns to face its way), and the glow when it comes down.
    # Thicker lines, so it still reads at 0.6 of its size on the replay's small monitor.
    sat = re.sub(r'stroke-width="[\d.]+"', 'stroke-width="5"', board_group("debris-1"))
    layer("o-sat", f'<g transform="translate(960 560) scale(0.6) translate(-960 -426)">{sat}</g>', (960, 560))
    layer("o-streak", f'<path d="M944 600 C942 660 952 740 960 790 C968 740 978 660 976 600 Z" fill="{GOLD_FILL}" '
                      f'stroke="{GOLD}" stroke-width="3" stroke-linejoin="round"/>', (960, 560))
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


def copy_board_layers():
    src = os.path.join(BOARD, "build")
    if not os.path.exists(os.path.join(src, "layers.json")):
        subprocess.run([sys.executable, os.path.join(BOARD, "extract.py")], check=True)
    board = json.load(open(os.path.join(src, "layers.json")))
    for name, info in board.items():
        shutil.copy(os.path.join(src, "layers", name + ".png"), os.path.join(OUT, name + ".png"))
        layers[name] = info


def main():
    os.makedirs(OUT, exist_ok=True)
    copy_board_layers()
    pad_parts()
    ascent_parts()
    deployment_parts()
    orbit_parts()
    return_parts()
    time_parts()
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
