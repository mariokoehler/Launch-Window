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
    layer("e-hole", f'<rect x="{x0 + 4}" y="{y - 2}" width="{x1 - x0 - 8}" height="10" fill="{NAVY}"/>', (x1, y))
    layer("e-hatch", f'<rect x="{x0}" y="{y - 8}" width="{x1 - x0}" height="10" rx="3" fill="#ffffff" {LINE} stroke-width="3"/>',
          (x1, y))
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
    recovery_parts()
    map_parts()
    planet_parts()
    moon_parts()
    way_parts()
    dock_parts()
    eva_parts()
    lunar_parts()
    stage_parts()
    plane_parts()
    probe_parts()
    carrier_entry_parts()
    planet_entry_parts()
    planet_descent_parts()
    air_parts()
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
