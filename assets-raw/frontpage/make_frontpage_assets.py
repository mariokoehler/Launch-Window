"""Converts the picked front-page art (picks.json, from make_frontpage_art.py) into what the client
ships, in assets/ui/frontpage/ (ui-spec.md 4.18):

- paper.jpg: the newsprint, resized to the page's size at 1920x1080 (PAGE).
- masthead.png: the nameplate as ink on transparent, so the client can draw it over the paper. The
  generated image is black on white; its darkness becomes the alpha, in the page's ink colour, and
  it's cropped to the lettering. RGB colour everywhere, so mipmaps can't fringe it.
- <first-id>.jpg, one per First: the press photo printed in ink on the paper colour (a multiply of the
  photo over the newsprint, baked), so the client draws it as it is.

Usage:
    python make_frontpage_assets.py
"""
import json
import os

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
PICKS = os.path.join(HERE, 'picks.json')
OUT = os.path.join(HERE, '..', '..', 'assets', 'ui', 'frontpage')
PAGE = (1440, 900)
INK = np.array([0x1c, 0x1c, 0x1a], dtype=np.float64)   # Theme.INK, the page's ink
PAPER = np.array([0xf5, 0xf0, 0xdd], dtype=np.float64)  # the newsprint's mid tone, for the photos


def variant(picks, job):
    return Image.open(os.path.join(HERE, 'variants', f'{job}_{picks[job]}.png'))


def masthead(image):
    grey = np.asarray(image.convert('L'), dtype=np.float64)
    white = np.percentile(grey, 95)
    alpha = np.clip((white - grey) / (white - 20), 0, 1)
    alpha[alpha < 0.03] = 0
    rgba = np.zeros(grey.shape + (4,), dtype=np.uint8)
    rgba[..., :3] = INK.astype(np.uint8)
    rgba[..., 3] = np.round(alpha * 255).astype(np.uint8)
    out = Image.fromarray(rgba, 'RGBA')
    left, top, right, bottom = out.getchannel('A').getbbox()
    pad = 6
    return out.crop((max(0, left - pad), max(0, top - pad), min(out.width, right + pad),
                     min(out.height, bottom + pad)))


def photo(image):
    grey = np.asarray(image.convert('L'), dtype=np.float64)[..., None] / 255
    return Image.fromarray(np.round(INK + (PAPER - INK) * grey).astype(np.uint8), 'RGB')


def main():
    with open(PICKS, encoding='utf-8') as source:
        picks = json.load(source)
    os.makedirs(OUT, exist_ok=True)
    variant(picks, 'paper').convert('RGB').resize(PAGE, Image.LANCZOS).save(
        os.path.join(OUT, 'paper.jpg'), quality=90)
    masthead(variant(picks, 'masthead')).save(os.path.join(OUT, 'masthead.png'))
    for job in picks:
        if job.startswith('photo-'):
            photo(variant(picks, job)).save(os.path.join(OUT, job[len('photo-'):] + '.jpg'), quality=90)
    for name in sorted(os.listdir(OUT)):
        print(name, Image.open(os.path.join(OUT, name)).size)


if __name__ == '__main__':
    main()
