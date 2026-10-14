"""Checks built clips (run build.py first): python check.py spoilers|handoffs <step> ...

spoilers: renders every variant every 0.2 s up to its earliest reveal and says where they differ.
Before a variant's reveal nothing may tell it apart (ui-spec.md 4.7.3), and the glitch and the
failure share their first sign of trouble, so a difference should only show at the moment that
decides it, just before the reveal.
handoffs: compares each step's success and glitch last frame with the next step's first (the steps
named, in order); what's left should be one frame's motion.
"""
import os
import subprocess
import sys

from PIL import Image, ImageChops, ImageStat

import build as b


def frame(step, variant, t, name, side):
    out = os.path.join(b.BUILD, name)
    subprocess.run([b.CLI, "render-frame", "--project", os.path.join(b.BUILD, f"{step}-{variant}.ecproj"),
                    "--comp", f"{step} {variant}", "--time", str(round(t, 3)), "--max-side", str(side), "--out", out],
                   check=True, capture_output=True)
    return Image.open(out).convert("RGB")


def spoilers(steps):
    for step in steps:
        spec = b.STEPS[step]
        reveal = [spec[v][1] for v in b.VARIANTS]
        # Each pair up to the earlier of its two reveals.
        pairs = {(i, j): min(reveal[i], reveal[j]) for i, j in ((0, 1), (0, 2), (1, 2))}
        t, seen = 0.0, {}
        while t < max(pairs.values()) - 0.05:
            ims = [frame(step, v, t, f"check-{step}-{v}.png", 480) for v in b.VARIANTS]
            for (i, j), until in pairs.items():
                pair = f"{b.VARIANTS[i]}/{b.VARIANTS[j]} (reveal {until})"
                if t < until - 0.05 and pair not in seen and ImageChops.difference(ims[i], ims[j]).getbbox():
                    seen[pair] = round(t, 2)
            t += 0.2
        print(f"{step}: first differ at", seen or "never")


def handoffs(steps):
    for a, c in zip(steps, steps[1:]):
        start = frame(c, "success", 0, f"check-{c}-start.png", 640)
        for v in ("success", "glitch"):
            end = frame(a, v, b.STEPS[a][v][0] - 1 / 30, f"check-{a}-{v}-end.png", 640)
            d = ImageChops.difference(end, start)
            print(f"{a} {v} -> {c}: differ in {d.getbbox()}, mean {sum(ImageStat.Stat(d).mean) / 3:.2f}")


if __name__ == "__main__":
    {"spoilers": spoilers, "handoffs": handoffs}[sys.argv[1]](sys.argv[2:])
