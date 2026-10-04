"""Writes the UI chrome icons as SVG sources and rasterizes every UI icon for the libGDX client.

The chrome icons are simple stroke glyphs from the UI Asset Sheet (Claude Design artifact, the
project's own original icons). Each is written here as an SVG with stroke="currentColor", then
rasterized white on transparent so the client can tint it at runtime. The SET skill icons come from
assets-raw/svg/icon_*.svg (the user's own traced art) and are rasterized by height only, keeping
their aspect ratio, as the Asset Sheet requires.

Run from the repo root:  python assets-raw/svg/ui/make_ui_icons.py   (needs: pip install resvg-py)
"""
import os
import re

import resvg_py

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.normpath(os.path.join(HERE, "..", "..", ".."))
OUT = os.path.join(REPO, "assets", "ui", "icons")

STROKE = ('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" '
          'stroke-width="2" stroke-linecap="round" stroke-linejoin="round">{}</svg>')

CHROME = {
    "back": '<path d="M19 12H5M12 19l-7-7 7-7"/>',
    "home": '<path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V9z"/><path d="M9 22V12h6v10"/>',
    "settings": '<circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/>',
    "prestige": '<polygon points="12 2 15 9 22 9 16.5 13.5 18.5 21 12 16.5 5.5 21 7.5 13.5 2 9 9 9 12 2"/>',
    "game_time": '<rect x="3" y="4" width="18" height="18"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/>',
    "clock": '<circle cx="12" cy="12" r="9"/><polyline points="12 7 12 12 16 14"/>',
    "funds": '<line x1="12" y1="1" x2="12" y2="23"/><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/>',
    "set_employees": '<path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/>',
    "astronauts": '<circle cx="12" cy="12" r="8"/><rect x="6" y="10" width="12" height="6" rx="3"/><line x1="12" y1="4" x2="12" y2="1.5"/>',
    "flight_controllers": '<path d="M3 18v-6a9 9 0 0 1 18 0v6"/><path d="M21 19a2 2 0 0 1-2 2h-1a2 2 0 0 1-2-2v-3a2 2 0 0 1 2-2h3zM3 19a2 2 0 0 0 2 2h1a2 2 0 0 0 2-2v-3a2 2 0 0 0-2-2H3z"/>',
    "open_programs": '<path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/>',
    "sort_desc": '<path d="M12 5v14M19 12l-7 7-7-7"/>',
    "sort_asc": '<path d="M12 19V5M5 12l7-7 7 7"/>',
    "chevron": '<path d="M9 18l6-6-6-6"/>',
    "morale": '<path d="M12 21C12 21 4 15.5 4 9.5A4.5 4.5 0 0 1 12 6.5 4.5 4.5 0 0 1 20 9.5C20 15.5 12 21 12 21Z"/>',
    "learning_potential": '<path d="M22 12l-9-9-11 6 11 6 9-9z"/><path d="M11 9v9l-7-3.9V9"/>',
    "check": '<polyline points="5 12.5 10 17 19 7"/>',
    "lock": '<rect x="4" y="11" width="16" height="10"/><path d="M8 11V7a4 4 0 0 1 8 0v4"/>',
}

# The astronaut glyph's antenna tip is a filled dot.
EXTRA_FILL = {"astronauts": '<circle cx="12" cy="1.5" r="0.8" fill="currentColor" stroke="none"/>'}

SKILLS = ["rockets", "space_probes", "human_rated_rockets", "crewed_spacecraft", "eva_suits"]

CHROME_PX = 48
SKILL_HEIGHT_PX = 64


def white(svg):
    return svg.replace("currentColor", "#ffffff")


def main():
    os.makedirs(OUT, exist_ok=True)
    for name, body in CHROME.items():
        svg = STROKE.format(body + EXTRA_FILL.get(name, ""))
        with open(os.path.join(HERE, name + ".svg"), "w", encoding="utf-8", newline="\n") as f:
            f.write(svg + "\n")
        png = resvg_py.svg_to_bytes(svg_string=white(svg), width=CHROME_PX, height=CHROME_PX)
        with open(os.path.join(OUT, name + ".png"), "wb") as f:
            f.write(bytes(png))
    for skill in SKILLS:
        with open(os.path.join(REPO, "assets-raw", "svg", "icon_" + skill + ".svg"), encoding="utf-8") as f:
            svg = f.read()
        w, h = (float(v) for v in re.search(r'viewBox="0 0 ([\d.]+) ([\d.]+)"', svg).groups())
        width = max(1, round(SKILL_HEIGHT_PX * w / h))
        png = resvg_py.svg_to_bytes(svg_string=svg, width=width, height=SKILL_HEIGHT_PX)
        with open(os.path.join(OUT, "skill_" + skill + ".png"), "wb") as f:
            f.write(bytes(png))
    print("wrote", len(CHROME), "chrome icons and", len(SKILLS), "skill icons to", OUT)


if __name__ == "__main__":
    main()
