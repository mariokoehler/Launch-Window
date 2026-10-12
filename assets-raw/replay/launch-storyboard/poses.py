"""Prints every part's transform and opacity per frame, leaving out parts at rest and hidden."""
import os
import re
import xml.etree.ElementTree as ET

HERE = os.path.dirname(os.path.abspath(__file__))
P = os.path.join(HERE, "project")
NS = "{http://www.w3.org/2000/svg}"


def frame(name):
    html = open(os.path.join(P, name), encoding="utf-8").read()
    svg = re.search(r"<svg.*?</svg>", html, re.S).group(0)
    root = ET.fromstring(svg)
    out = {}
    for g in root.iter(NS + "g"):
        gid = g.get("id")
        if gid:
            out[gid] = (g.get("transform", ""), g.get("opacity", "1"), g)
    return out


if __name__ == "__main__":
    for v in ("Success", "Glitch", "Failure"):
        for i in range(1, 7):
            f = frame(f"{v}{i}.dc.html")
            parts = []
            for gid, (tr, op, g) in f.items():
                rest = tr in ("", ) or re.fullmatch(r"translate\(0\.0 0\.0\) rotate\(0 [-\d.]+ [-\d.]+\)", tr)
                if op == "0" and gid not in ("trail",):
                    continue
                if rest and op == "1":
                    continue
                extra = ""
                if gid == "trail":
                    p = g.find(NS + "path")
                    extra = " dash=" + (p.get("stroke-dasharray") if p is not None else "?")
                parts.append(f"{gid}[{tr.replace('translate', 't').replace('rotate', 'r')} op={op}{extra}]")
            print(f"{v}{i}: " + "  ".join(parts))
        print()
