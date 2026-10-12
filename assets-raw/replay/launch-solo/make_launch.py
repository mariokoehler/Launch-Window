"""Mission step clip, first try: a launch, success. Draws the layers in the explainers' style
(navy line art, hatched tanks, gold for fire and paths, a faint grid), then has EffectCraft
animate (launch.jsx) and render them.

World coordinates are comp pixels at rest (1920 x 1080, y down); a layer is a crop of the world,
placed by its top-left corner, or by a pivot when it turns or scales.

Run from anywhere:  python assets-raw/replay/launch-solo/make_launch.py   (needs: pip install
resvg-py, and EffectCraft at its default install path). Writes build/launch-success.webm.
"""
import json
import os
import re
import subprocess

import resvg_py

HERE = os.path.dirname(os.path.abspath(__file__))
BUILD = os.path.join(HERE, "build")
LAYERS = os.path.join(BUILD, "layers")
FONTS = os.path.normpath(os.path.join(HERE, "..", "..", "..", "assets", "fonts"))
CLI = r"C:\Program Files\EffectCraft\effectcraft-cli.exe"
VECTORCRAFT = r"C:\Program Files\VectorCraft\vectorcraft-cli.exe"
SHIFT = 500  # VectorCraft draws on an artboard from (0, 0)

NAVY = "#16305c"
GOLD = "#e0a72e"
DEFS = f"""<defs>
<pattern id="hatch" width="10" height="10" patternUnits="userSpaceOnUse" patternTransform="rotate(45)">
<rect width="10" height="10" fill="rgba(22,48,92,0.10)"/><line x1="0" y1="0" x2="0" y2="10" stroke="rgba(22,48,92,0.35)" stroke-width="2.5"/></pattern>
<pattern id="earth" width="14" height="14" patternUnits="userSpaceOnUse" patternTransform="rotate(45)">
<rect width="14" height="14" fill="rgba(22,48,92,0.05)"/><line x1="0" y1="0" x2="0" y2="14" stroke="rgba(22,48,92,0.18)" stroke-width="2"/></pattern>
</defs>"""
LINE = f'fill="#ffffff" stroke="{NAVY}" stroke-linejoin="round"'

layers = {}


def layer(name, box, body, pivot=None):
    """box = (x, y, w, h) in world px; pivot in world px, or None for the top-left corner."""
    x, y, w, h = box
    svg = (f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="{x} {y} {w} {h}">'
           f'{DEFS}{body}</svg>')
    png = resvg_py.svg_to_bytes(svg_string=svg, width=w, height=h, font_dirs=[FONTS], skip_system_fonts=True)
    with open(os.path.join(LAYERS, name + ".png"), "wb") as f:
        f.write(bytes(png))
    px, py = pivot if pivot else (x, y)
    layers[name] = {"x": x, "y": y, "w": w, "h": h, "px": px, "py": py}


def rocket():
    b = []
    b.append(f'<path d="M960 368 L960 384" stroke="{NAVY}" stroke-width="3"/>')
    b.append(f'<path d="M956 402 L958 384 L962 384 L964 402 Z" {LINE} stroke-width="2"/>')
    b.append(f'<path d="M952 402 L968 402 L984 456 L936 456 Z" {LINE} stroke-width="3"/>')                 # capsule
    b.append(f'<rect x="936" y="456" width="48" height="130" {LINE} stroke-width="3"/>')                    # stage 2
    b.append(f'<rect x="943" y="470" width="34" height="102" fill="url(#hatch)" stroke="{NAVY}" stroke-width="1.5"/>')
    b.append(f'<path d="M936 586 L984 586 L990 606 L930 606 Z" {LINE} stroke-width="3"/>')                 # interstage
    b.append(f'<path d="M930 760 L898 816 L930 816 Z" {LINE} stroke-width="3"/>')                           # fins
    b.append(f'<path d="M990 760 L1022 816 L990 816 Z" {LINE} stroke-width="3"/>')
    b.append(f'<rect x="930" y="606" width="60" height="210" {LINE} stroke-width="3"/>')                    # stage 1
    b.append(f'<rect x="937" y="620" width="46" height="80" fill="url(#hatch)" stroke="{NAVY}" stroke-width="1.5"/>')
    b.append(f'<rect x="937" y="712" width="46" height="92" fill="url(#hatch)" stroke="{NAVY}" stroke-width="1.5"/>')
    b.append(f'<path d="M948 816 L972 816 L980 842 L940 842 Z" {LINE} stroke-width="3"/>')                 # engine bell
    layer("rocket", (890, 360, 140, 488), "".join(b))


def flame():
    body = (f'<path d="M941 842 C939 900 952 960 960 1004 C968 960 981 900 979 842 Z" '
            f'fill="rgba(224,167,46,0.30)" stroke="{GOLD}" stroke-width="3" stroke-linejoin="round"/>'
            f'<path d="M949 842 C949 880 956 915 960 938 C964 915 971 880 971 842 Z" fill="{GOLD}"/>')
    layer("flame", (930, 838, 60, 172), body, pivot=(960, 842))


def ground():
    body = (f'<rect x="0" y="880" width="1920" height="230" fill="#ffffff"/><rect x="0" y="880" width="1920" height="230" fill="url(#earth)"/>'
            f'<line x1="0" y1="880" x2="1920" y2="880" stroke="{NAVY}" stroke-width="3"/>')
    layer("ground", (0, 876, 1920, 234), body)
    # The pad: two blocks either side of the flame trench, so the first fire shows in the gap.
    pad = (f'<rect x="898" y="842" width="38" height="38" fill="url(#hatch)" stroke="{NAVY}" stroke-width="3"/>'
           f'<rect x="984" y="842" width="38" height="38" fill="url(#hatch)" stroke="{NAVY}" stroke-width="3"/>')
    layer("pad", (894, 838, 132, 46), pad)


def tower():
    b = [f'<line x1="1046" y1="336" x2="1046" y2="880" stroke="{NAVY}" stroke-width="3"/>',
         f'<line x1="1100" y1="336" x2="1100" y2="880" stroke="{NAVY}" stroke-width="3"/>',
         f'<rect x="1036" y="326" width="74" height="10" {LINE} stroke-width="2.5"/>',
         f'<line x1="1100" y1="331" x2="1150" y2="331" stroke="{NAVY}" stroke-width="2.5"/>',
         f'<line x1="1146" y1="331" x2="1146" y2="356" stroke="{NAVY}" stroke-width="1.5"/>']
    y = 880
    while y > 340:
        top = max(336, y - 46)
        b.append(f'<line x1="1046" y1="{top}" x2="1100" y2="{top}" stroke="{NAVY}" stroke-width="2"/>')
        b.append(f'<path d="M1046 {y} L1100 {top} M1100 {y} L1046 {top}" stroke="rgba(22,48,92,0.55)" stroke-width="1.5"/>')
        y = top
    layer("tower", (1030, 320, 130, 562), "".join(b))
    for name, ay, x0 in (("armUpper", 520, 984), ("armLower", 690, 990)):
        w = 1046 - x0
        arm = (f'<rect x="{x0}" y="{ay - 7}" width="{w}" height="14" fill="#ffffff" stroke="{NAVY}" stroke-width="2.5"/>'
               f'<path d="M{x0} {ay + 7} L{x0 + w / 3} {ay - 7} L{x0 + 2 * w / 3} {ay + 7} L{1046} {ay - 7}" '
               f'fill="none" stroke="rgba(22,48,92,0.55)" stroke-width="1.5"/>')
        layer(name, (x0 - 4, ay - 11, w + 8, 22), arm, pivot=(1046, ay))


def ruler():
    top, bottom = -2600, 880
    b = [f'<line x1="150" y1="{top}" x2="150" y2="{bottom}" stroke="{NAVY}" stroke-width="2"/>']
    for i, y in enumerate(range(bottom, top, -40)):
        x0 = 126 if i % 5 == 0 else 140
        b.append(f'<line x1="{x0}" y1="{y}" x2="150" y2="{y}" stroke="{NAVY}" stroke-width="{2 if i % 5 == 0 else 1.5}"/>')
    layer("ruler", (110, top - 4, 50, bottom - top + 8), "".join(b))


def cloud(name, cx, cy, s):
    d = (f"M{cx - 120 * s} {cy + 30 * s} "
         f"C{cx - 150 * s} {cy + 30 * s} {cx - 150 * s} {cy - 10 * s} {cx - 110 * s} {cy - 8 * s} "
         f"C{cx - 105 * s} {cy - 45 * s} {cx - 45 * s} {cy - 50 * s} {cx - 35 * s} {cy - 20 * s} "
         f"C{cx - 20 * s} {cy - 60 * s} {cx + 50 * s} {cy - 60 * s} {cx + 55 * s} {cy - 18 * s} "
         f"C{cx + 100 * s} {cy - 35 * s} {cx + 140 * s} {cy - 5 * s} {cx + 120 * s} {cy + 30 * s} Z")
    body = f'<path d="{d}" fill="#ffffff" stroke="rgba(22,48,92,0.45)" stroke-width="2.5" stroke-linejoin="round"/>'
    layer(name, (int(cx - 160 * s), int(cy - 70 * s), int(320 * s), int(110 * s)), body)


def trail():
    top, bottom = -2700, 842
    body = (f'<line x1="960" y1="{bottom}" x2="960" y2="{top}" stroke="{GOLD}" stroke-width="4" '
            f'stroke-dasharray="14 12"/>')
    layer("trail", (950, top, 20, bottom - top + 4), body)


def united(circles):
    """One outline around overlapping circles [(cx, cy, r), ...], merged by VectorCraft's
    Pathfinder: SVG path data in the same coordinates."""
    out = os.path.join(BUILD, "united.svg")
    args = [VECTORCRAFT, "run"]
    for cx, cy, r in circles:
        # Shifted into positive space for the artboard, and back below.
        args += ["--cmd", "shape.ellipse", "--params",
                 json.dumps({"x": cx - r + SHIFT, "y": cy - r + SHIFT, "width": 2 * r, "height": 2 * r})]
    args += ["--cmd", "select.all", "--cmd", "object.pathfinder.unite", "--export", out]
    subprocess.run(args, check=True, capture_output=True)
    d = re.search(r'<path[^>]* d="([^"]+)"', open(out, encoding="utf-8").read()).group(1)
    return re.sub(r"-?\d+(?:\.\d+)?", lambda m: str(round(float(m.group(0)) - SHIFT, 3)), d)


def puffs():
    # A puff of smoke: three circles, the middle one higher, drawn as one outline.
    for r in (42, 58, 76):
        d = united([(-0.55 * r, 0.12 * r, 0.62 * r), (0, -0.12 * r, 0.82 * r), (0.55 * r, 0.12 * r, 0.62 * r)])
        body = (f'<path d="{d}" fill="#ffffff" stroke="{NAVY}" stroke-width="2.5" stroke-linejoin="round"/>'
                f'<path d="{d}" fill="rgba(22,48,92,0.05)"/>')
        layer(f"puff{r}", (int(-1.25 * r), int(-r - 3), int(2.5 * r), int(1.9 * r)), body, pivot=(0, 0))


def wisp():
    body = (f'<path d="M10 40 q7 -6 0 -13 q-7 -7 0 -14 q7 -6 0 -12" fill="none" '
            f'stroke="rgba(22,48,92,0.5)" stroke-width="2" stroke-linecap="round"/>')
    layer("wisp", (0, 0, 20, 42), body, pivot=(10, 21))


def paper():
    lines = "".join(f'<line x1="{x}" y1="0" x2="{x}" y2="1080"/>' for x in range(0, 1921, 40))
    lines += "".join(f'<line x1="0" y1="{y}" x2="1920" y2="{y}"/>' for y in range(0, 1081, 40))
    body = f'<rect width="1920" height="1080" fill="#ffffff"/><g stroke="rgba(22,48,92,0.06)" stroke-width="1.5">{lines}</g>'
    layer("paper", (0, 0, 1920, 1080), body)


def main():
    os.makedirs(LAYERS, exist_ok=True)
    paper(); rocket(); flame(); ground(); tower(); ruler(); trail(); puffs(); wisp()
    cloud("cloud1", 1520, 300, 1.0)
    cloud("cloud2", 470, -420, 1.2)
    cloud("cloud3", 1380, -1150, 0.9)
    cloud("cloud4", 640, -1900, 1.3)
    script = os.path.join(BUILD, "launch.gen.jsx")
    with open(script, "w", encoding="utf-8") as f:
        f.write("var DIR = " + json.dumps(LAYERS.replace("\\", "/") + "/") + ";\n")
        f.write("var L = " + json.dumps(layers) + ";\n")
        f.write(open(os.path.join(HERE, "launch.jsx"), encoding="utf-8").read())
    project = os.path.join(BUILD, "launch.ecproj")
    subprocess.run([CLI, "script", script, "--empty", "--save-as", project], check=True)
    subprocess.run([CLI, "render", "--project", project, "--comp", "Launch",
                    "--out", os.path.join(BUILD, "launch-success.webm"), "--format", "webm",
                    "--webm-codec", "vp9", "--audio", "off", "--bitrate", "1500"], check=True)


if __name__ == "__main__":
    main()
