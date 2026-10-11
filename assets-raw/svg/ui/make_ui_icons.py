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
    # The explainer's close button (ui-spec.md component 18), 2026-10-06.
    "close": '<path d="M6 6l12 12M18 6 6 18"/>',
    # A diagnosis's clue icons (ui-spec.md component 26, the R&D decisions canvas), 2026-10-10.
    "clue_trace": '<path d="M3 4v16h18"/><path d="M6 14l3-3 3 2 3-7 3 9 2-2"/>',
    "clue_inspect": '<circle cx="10.5" cy="10.5" r="6"/><path d="M15 15l6 6"/><path d="M8 10.5h5M10.5 8v5"/>',
    "clue_note": '<path d="M5 3h10l4 4v14H5z"/><path d="M8 11h8M8 15h8M8 7h4"/><path d="M14 3v4h4"/>',
    "morale": '<path d="M12 21C12 21 4 15.5 4 9.5A4.5 4.5 0 0 1 12 6.5 4.5 4.5 0 0 1 20 9.5C20 15.5 12 21 12 21Z"/>',
    # A mortarboard: a flat board, the cap under it, and the tassel off the right corner (redrawn 2026-10-06).
    "learning_potential": '<path d="M2 9.5 12 5l10 4.5L12 14z"/><path d="M6 11.3V16c0 1.7 2.7 3 6 3s6-1.3 6-3v-4.7"/><path d="M22 9.5V15"/>',
    "check": '<polyline points="5 12.5 10 17 19 7"/>',
    "lock": '<rect x="4" y="11" width="16" height="10"/><path d="M8 11V7a4 4 0 0 1 8 0v4"/>',
    "play": '<polygon points="7 4 19 12 7 20 7 4"/>',
    "pause": '<line x1="9" y1="5" x2="9" y2="19"/><line x1="15" y1="5" x2="15" y2="19"/>',
    "step_back": '<polygon points="17 5 8 12 17 19 17 5"/><line x1="7" y1="5" x2="7" y2="19"/>',
    "step_forward": '<polygon points="7 5 16 12 7 19 7 5"/><line x1="17" y1="5" x2="17" y2="19"/>',
    "restart": '<polyline points="4 4 4 10 10 10"/><path d="M5.5 15a7 7 0 1 0 1.6-7.3L4 10"/>',
    "document": '<path d="M7 3h7l5 5v13a1 1 0 0 1-1 1H7a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1z"/><path d="M14 3v5h5"/><line x1="9" y1="12" x2="15" y2="12"/><line x1="9" y1="16" x2="15" y2="16"/>',
    # The game result's trophy (Museum, Settings & Game Result mockup).
    "trophy": '<path d="M8 21h8M12 17v4M7 4h10v5a5 5 0 0 1-10 0V4z"/><path d="M17 5h3v2a3 3 0 0 1-3 3M7 5H4v2a3 3 0 0 0 3 3"/>',
    # A set square with its cut-out and two scale ticks: the concept chip's "how it works" glyph
    # (ui-spec.md component 19, Explainers canvas, 2026-10-07).
    "concept": '<path d="M4 20V4l16 16z"/><path d="M8 16v-3.2l3.2 3.2z"/><path d="M4 8h2M4 12h2.5"/>',
    # People (ui-spec.md components 20 and 22, People canvas, 2026-10-07): a team trait's mark (three
    # filled dots, below), "works well with" (two linked rings) and "clashes with" (a spark).
    "trait_team": '',
    "rel_good": '<circle cx="8.5" cy="12" r="5.5"/><circle cx="15.5" cy="12" r="5.5"/>',
    "rel_clash": '<path d="M13.5 2.5L5.5 13.5h6l-1.5 8 8.5-11.5h-6z"/>',
    # The Intelligence Office (Intelligence Office canvas, 2026-10-07): marker, footer, check button.
    "binoculars": '<path d="M2.6 15.5 4.6 6.6A2 2 0 0 1 6.5 5h1.6A1.5 1.5 0 0 1 9.6 6.5V13"/><path d="M21.4 15.5 19.4 6.6A2 2 0 0 0 17.5 5h-1.6a1.5 1.5 0 0 0-1.5 1.5V13"/><path d="M9.6 9.5h4.8"/><circle cx="6.3" cy="16.5" r="3.8"/><circle cx="17.7" cy="16.5" r="3.8"/>',
    # Poaching (Poaching canvas, 2026-10-08): keep at any price (a shield with a tick), let go (a door
    # with an arrow out), joined from a rival (an arrow into a door).
    "retain_keep": '<path d="M12 2.5 4.5 5.5v6c0 4.6 3.2 8.4 7.5 10 4.3-1.6 7.5-5.4 7.5-10v-6z"/><polyline points="8.5 12 11 14.5 15.5 9.5"/>',
    "retain_go": '<path d="M13 4H5v16h8"/><path d="M15 12h7M19 8.5 22.5 12 19 15.5"/>',
    "joined": '<path d="M11 4h8v16h-8"/><path d="M2 12h11M9.5 8.5 13 12l-3.5 3.5"/>',
    # Help (ui-spec.md component 23, 2026-10-08): the header's help button (a question mark in a
    # circle, its dot below) and the search field's magnifier.
    "help": '<circle cx="12" cy="12" r="9.5"/><path d="M9.2 9.3a2.9 2.9 0 0 1 5.6 1c0 1.9-2.8 2.5-2.8 4.2"/>',
    "search": '<circle cx="10.5" cy="10.5" r="6.5"/><line x1="15.5" y1="15.5" x2="21" y2="21"/>',
    # The contract marker (ui-spec.md component 27, Contracts canvas, 2026-10-11): a page with a folded
    # corner, two lines, a signature, and a round seal with two ribbons. The outline marks a flight
    # that unlocks a contract line; the filled one (FILLED below) the flight contracts fly.
    "contract_unlock": '<path d="M12.2 21H5V2.5h8l4.5 4.5v5"/><path d="M13 2.5V7h4.5"/><path d="M8 8h3.5M8 11.5h5.5"/>'
                       '<path d="M7.8 16.4c.8-1.7 1.6-1.7 2 0s1.2 1.2 2-.4"/><circle cx="16.5" cy="16" r="3.4"/>'
                       '<path d="M15 19.3l-1 3.2M18 19.3l1 3.2"/>',
}

# Icons drawn as filled shapes rather than strokes; the contract flight's page knocks its lines out
# with a mask.
FILLED = {
    "contract_flight": '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><defs><mask id="lwcm" '
                       'maskUnits="userSpaceOnUse" x="0" y="0" width="24" height="24"><rect width="24" height="24" '
                       'fill="#fff"/><path d="M13 1.5V7h5.5" fill="none" stroke="#000" stroke-width="1.6"/>'
                       '<path d="M7.5 8h4M7.5 11.5h6" stroke="#000" stroke-width="1.8" stroke-linecap="round"/>'
                       '<path d="M7.4 16.4c.8-1.7 1.6-1.7 2 0s1.2 1.2 2-.4" fill="none" stroke="#000" '
                       'stroke-width="1.6" stroke-linecap="round"/><circle cx="16.5" cy="16" r="5.4" fill="#000"/>'
                       '</mask></defs><path d="M4 1.5h9.6l5 5V21.5H4z" fill="currentColor" mask="url(#lwcm)"/>'
                       '<circle cx="16.5" cy="16" r="4" fill="currentColor"/><path d="M15 19.5l-1 3M18 19.5l1 3" '
                       'stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>',
}

# The astronaut glyph's antenna tip is a filled dot.
EXTRA_FILL = {"astronauts": '<circle cx="12" cy="1.5" r="0.8" fill="currentColor" stroke="none"/>',
              "help": '<circle cx="12" cy="17.6" r="1.1" fill="currentColor" stroke="none"/>',
              "trait_team": '<g fill="currentColor" stroke="none"><circle cx="12" cy="5.2" r="4"/>'
                            '<circle cx="4.8" cy="18.4" r="4"/><circle cx="19.2" cy="18.4" r="4"/></g>'}

SKILLS = ["rockets", "space_probes", "human_rated_rockets", "crewed_spacecraft", "eva_suits",
          # Astronaut skills (EVA reuses eva_suits) and Mission Control skills, plus the Flight Director.
          "leadership", "piloting", "science", "fitness",
          "propulsion", "trajectory_and_guidance", "spacecraft_systems", "crew_and_payload", "mission_operations",
          "flight_director"]

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
    for name, svg in FILLED.items():
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
    print("wrote", len(CHROME) + len(FILLED), "chrome icons and", len(SKILLS), "skill icons to", OUT)


if __name__ == "__main__":
    main()
