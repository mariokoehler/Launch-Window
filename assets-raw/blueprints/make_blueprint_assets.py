"""Converts the picked blueprint drawings into the images the client ships.

Each component's chosen variant in picks.json (variants/<component-id>_soft_<n>.png, 864x352, generated
by make_blueprints.py) becomes

    assets/ui/blueprints/<component-id>.jpg   864 x 352 (shown at about 576 x 235 in the turn report)

kept at its generated size: the turn report's R&D card is about two thirds of that wide, the same
margin the other content art keeps for larger windows. JPEG at quality 88 like make_content_art.py;
full chroma resolution (subsampling 0) made no visible difference to the thin lines at that size.

The background's shade of blue differs a little from drawing to drawing. That is wanted (user,
2026-10-09): a report rarely shows more than two side by side, and the variation gives them character.
Never normalise it here.

Usage (from anywhere; needs Pillow):
    python make_blueprint_assets.py
"""
import json
import os
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
PICKS = os.path.join(HERE, 'picks.json')
OUT = os.path.normpath(os.path.join(HERE, '..', '..', 'assets', 'ui', 'blueprints'))
SIZE = (864, 352)


def main():
    with open(PICKS, encoding='utf-8') as source:
        picks = json.load(source)
    os.makedirs(OUT, exist_ok=True)
    total = 0
    for component, file in picks.items():
        image = Image.open(os.path.join(HERE, file)).convert('RGB')
        if image.size != SIZE:
            image = image.resize(SIZE, Image.LANCZOS)
        target = os.path.join(OUT, component + '.jpg')
        image.save(target, quality=88)
        total += os.path.getsize(target)
    print(f'wrote {len(picks)} drawings, {total / 1e6:.1f} MB, to {OUT}')


if __name__ == '__main__':
    sys.exit(main())
