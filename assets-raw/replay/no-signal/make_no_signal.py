"""The replay's no-signal clip (ui-spec.md 4.7.3): an antenna sweeping the sky, signal arcs pulsing
out of it, for any step that has no clip of its own yet. Outcome-neutral, and a seamless 3 s loop:
the sweep's period is the clip's length and the arcs' period divides it. The game writes "No
signal" in the monitor's bezel; the clip has no text.

Run from anywhere:  python assets-raw/replay/no-signal/make_no_signal.py   (needs: pip install
resvg-py, and EffectCraft at its default install path). Writes assets/ui/replay/no-signal.webm and
its sidecar no-signal.json, and no-signal.png, a still for when no video can play.
"""
import json
import os
import subprocess

import resvg_py

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.normpath(os.path.join(HERE, "..", "..", ".."))
BUILD = os.path.join(HERE, "build")
LAYERS = os.path.join(BUILD, "layers")
OUT = os.path.join(REPO, "assets", "ui", "replay")
CLI = r"C:\Program Files\EffectCraft\effectcraft-cli.exe"
NAVY = "#16305c"
GOLD = "#e0a72e"
DEFS = """<defs><pattern id="earth" width="14" height="14" patternUnits="userSpaceOnUse" patternTransform="rotate(45)">
<rect width="14" height="14" fill="rgba(22,48,92,0.05)"/><line x1="0" y1="0" x2="0" y2="14" stroke="rgba(22,48,92,0.18)" stroke-width="2"/></pattern></defs>"""
DURATION = 3.0

layers = {}


def layer(name, box, body, pivot=None):
    x, y, w, h = box
    svg = (f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="{x} {y} {w} {h}">'
           f'{DEFS}{body}</svg>')
    with open(os.path.join(LAYERS, name + ".png"), "wb") as f:
        f.write(bytes(resvg_py.svg_to_bytes(svg_string=svg, width=w, height=h)))
    px, py = pivot if pivot else (x, y)
    layers[name] = {"x": x, "y": y, "px": px, "py": py}


def draw():
    grid = "".join(f'<line x1="{x}" y1="0" x2="{x}" y2="1080"/>' for x in range(0, 1921, 40))
    grid += "".join(f'<line x1="0" y1="{y}" x2="1920" y2="{y}"/>' for y in range(0, 1081, 40))
    layer("paper", (0, 0, 1920, 1080),
          f'<rect width="1920" height="1080" fill="#ffffff"/><g stroke="rgba(22,48,92,0.06)" stroke-width="1.5">{grid}</g>')
    layer("ground", (0, 876, 1920, 204),
          f'<rect x="0" y="880" width="1920" height="200" fill="#ffffff"/><rect x="0" y="880" width="1920" height="200" fill="url(#earth)"/>'
          f'<line x1="0" y1="880" x2="1920" y2="880" stroke="{NAVY}" stroke-width="3"/>')
    # The mount: a tripod and a short mast up to the dish's pivot at (960, 660).
    layer("mount", (880, 640, 160, 244),
          f'<path d="M960 660 L912 880 M960 660 L1008 880 M960 660 L960 880 M928 806 L992 806" '
          f'fill="none" stroke="{NAVY}" stroke-width="3" stroke-linecap="round"/>'
          f'<rect x="946" y="648" width="28" height="22" fill="#ffffff" stroke="{NAVY}" stroke-width="3"/>')
    # The dish, opening upward: the bowl, its feed and the feed's horn at the focus (960, 470).
    layer("dish", (820, 440, 280, 230),
          f'<path d="M840 540 Q960 700 1080 540 Z" fill="#ffffff" stroke="{NAVY}" stroke-width="3" stroke-linejoin="round"/>'
          f'<path d="M852 548 Q960 680 1068 548" fill="none" stroke="rgba(22,48,92,0.35)" stroke-width="1.5"/>'
          f'<path d="M900 540 L956 476 M1020 540 L964 476" fill="none" stroke="{NAVY}" stroke-width="2"/>'
          f'<rect x="950" y="462" width="20" height="16" fill="#ffffff" stroke="{NAVY}" stroke-width="2.5"/>'
          f'<line x1="960" y1="620" x2="960" y2="664" stroke="{NAVY}" stroke-width="3"/>',
          pivot=(960, 660))
    # One signal arc, drawn about the feed; the script scales copies of it outward.
    layer("arc", (860, 370, 200, 110),
          f'<path d="M886 446 A80 80 0 0 1 1034 446" fill="none" stroke="{GOLD}" stroke-width="4" '
          f'stroke-linecap="round" stroke-dasharray="14 10"/>', pivot=(960, 470))


SCRIPT = """
var comp = app.project.items.addComp("No signal", 1920, 1080, 1, DUR, 30);
function tf(l, n) { return l.property("Transform").property(n); }
function add(name, parent) {
    var d = L[name];
    var l = comp.layers.add(app.project.importFile(new ImportOptions(new File(DIR + name + ".png"))));
    l.name = name;
    if (parent) l.parent = parent;
    tf(l, "Anchor Point").setValue([d.px - d.x, d.py - d.y]);
    tf(l, "Position").setValue(parent ? [d.px - L[parent.name].x, d.py - L[parent.name].y] : [d.px, d.py]);
    return l;
}
add("paper");
add("mount");
var dish = add("dish");
// The sweep: tilted toward the sky, swinging 14 degrees each way, once per loop.
tf(dish, "Rotation").expression = "-20 + 14 * Math.sin(2 * Math.PI * time / " + DUR + ")";
// Three arcs, a third of a second apart, each growing out of the feed and fading, once a second.
for (var i = 0; i < 3; i++) {
    var a = add("arc", dish);
    a.name = "arc" + i;
    var ph = "((time + " + (i / 3).toFixed(4) + ") % 1)";
    tf(a, "Scale").expression = "var p = " + ph + "; var s = 40 + 160 * p; [s, s]";
    tf(a, "Opacity").expression = "var p = " + ph + "; 100 * Math.sin(Math.PI * p) * (1 - 0.5 * p)";
}
add("ground");
"""


def main():
    os.makedirs(LAYERS, exist_ok=True)
    os.makedirs(OUT, exist_ok=True)
    draw()
    script = os.path.join(BUILD, "no_signal.gen.jsx")
    with open(script, "w", encoding="utf-8") as f:
        f.write("var DIR = " + json.dumps(LAYERS.replace("\\", "/") + "/") + ";\n")
        f.write("var L = " + json.dumps(layers) + ";\n")
        f.write(f"var DUR = {DURATION};\n")
        f.write(SCRIPT)
    project = os.path.join(BUILD, "no-signal.ecproj")
    subprocess.run([CLI, "script", script, "--empty", "--save-as", project], check=True)
    video = os.path.join(OUT, "no-signal.webm")
    subprocess.run([CLI, "render", "--project", project, "--comp", "No signal", "--out", video,
                    "--format", "webm", "--webm-codec", "vp9", "--audio", "off", "--bitrate", "600",
                    "--resolution", "half"], check=True)
    with open(os.path.join(OUT, "no-signal.json"), "w") as f:
        json.dump({"duration": DURATION, "loop": True}, f, indent=1)
        f.write("\n")
    # The still, for when no video can play: one frame, as the monitor shows it.
    subprocess.run([CLI, "render-frame", "--project", project, "--comp", "No signal", "--time", "0.5",
                    "--max-side", "960", "--out", os.path.join(OUT, "no-signal.png")], check=True)
    print("wrote", video, os.path.getsize(video) // 1024, "KB")


if __name__ == "__main__":
    main()
