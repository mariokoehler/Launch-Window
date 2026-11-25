"""Art for the 1.0 launch's poster and banner (assets-raw/adverts/launch/README.md).

The trailer's keyframes are 1344x768, too small for a 2400x3600 poster. This makes taller, sharper versions in
ComfyUI with the trailer's own Qwen Image 2.1 workflow (../../portraits/portraits_workflow_api.json):

  poster_t2i_<n>   the Saturn V liftoff (trailer shot c28) rendered fresh at portrait size, its prompt and look
  poster_i2i_<n>   the picked keyframe itself, set on a tall canvas with its sky carried up, then refined at
                   portrait size with a light denoise, so it stays that very frame
  banner_<shot>_<n> the Moon keyframes (c34, c35, c36) refined at 2x the same way, for the banner's film strip

Usage (ComfyUI running locally):  python make_art.py [poster|banner|all] [--variants N]
Writes into out/ (not committed except the picks, see README.md). ComfyUI keeps its own copies (lw_launch_*).
"""
import argparse
import json
import os
import random
import sys

from PIL import Image, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
RAW = os.path.normpath(os.path.join(HERE, '..', '..'))
sys.path.insert(0, os.path.join(RAW, 'trailer'))
import comfy  # noqa: E402

WORKFLOW = os.path.join(RAW, 'portraits', 'portraits_workflow_api.json')
KEYFRAMES = os.path.join(RAW, 'trailer', 'keyframes')
OUT = os.path.join(HERE, 'out')
POSTER = (1600, 2400)


def read(path):
    with open(path, encoding='utf-8') as f:
        return json.load(f)


def shot(shot_id):
    for s in read(os.path.join(RAW, 'trailer', 'shots.json'))['shots']:
        if s['id'] == shot_id:
            return s
    raise KeyError(shot_id)


def prompt_for(shot_id, extra=''):
    s = shot(shot_id)
    looks = read(os.path.join(RAW, 'trailer', 'looks.json'))
    return (s['keyframes']['first'] + extra + ' ' + looks[s['look']]).strip()


def seed_for(job, n):
    return random.Random(f'launch-window-launch-art-{job}-{n}').randrange(2 ** 48)


def t2i(prompt, size, seed, prefix):
    graph = {k: v for k, v in read(WORKFLOW).items() if not k.startswith('_')}
    graph['474']['inputs']['prompt'] = prompt
    graph['458']['inputs']['seed'] = seed
    graph['456']['inputs']['width'], graph['456']['inputs']['height'] = size
    graph['468']['inputs']['switch'] = True
    graph['461']['inputs']['filename_prefix'] = prefix
    return graph


def i2i(prompt, image_name, seed, prefix, denoise):
    graph = t2i(prompt, (64, 64), seed, prefix)
    graph['900'] = {'class_type': 'LoadImage', 'inputs': {'image': image_name}}
    graph['901'] = {'class_type': 'VAEEncode', 'inputs': {'pixels': ['900', 0], 'vae': ['454', 0]}}
    graph['458']['inputs']['latent_image'] = ['901', 0]
    graph['458']['inputs']['denoise'] = denoise
    return graph


def run(graph, path):
    if os.path.exists(path):
        return
    data = comfy.run(graph)[0][1]
    with open(path, 'wb') as f:
        f.write(data)
    print('wrote', path, flush=True)


def tall_canvas(keyframe, size):
    """The keyframe scaled to the canvas's width, at its foot, its sky carried up and feathered into it."""
    src = Image.open(keyframe).convert('RGB')
    w, h = size
    # Keep the rocket, the tower and the inner edges of both smoke clouds.
    left, right = 200, 1180
    crop = src.crop((left, 0, right, src.height))
    crop = crop.resize((w, round(crop.height * w / crop.width)), Image.LANCZOS)
    top = h - crop.height
    # The sky's colour per row over its top band, extrapolated upward (a clear sky deepens toward the zenith).
    rows = [crop.crop((0, y, w, y + 1)).resize((1, 1), Image.BOX).getpixel((0, 0)) for y in range(0, 160)]
    c0 = [sum(r[i] for r in rows[:40]) / 40 for i in range(3)]
    c1 = [sum(r[i] for r in rows[120:]) / 40 for i in range(3)]
    slope = [(a - b) / 120 for a, b in zip(c0, c1)]  # per pixel, going up
    canvas = Image.new('RGB', size)
    px = canvas.load()
    for y in range(top + 160):
        d = top + 20 - y
        col = tuple(max(0, min(255, round(c0[i] + slope[i] * d))) for i in range(3))
        for x in range(w):
            px[x, y] = col
    mask = Image.new('L', crop.size, 255)
    for y in range(160):
        mask.paste(round(255 * y / 160), (0, y, w, y + 1))
    canvas.paste(crop, (0, top), mask)
    return canvas


def poster(variants):
    s_prompt = prompt_for('c28-apollo8-launch', ' A tall vertical frame: the whole rocket and tower in the '
                          'lower half, a wide stretch of clear blue sky above it.')
    for n in range(1, variants + 1):
        run(t2i(s_prompt, POSTER, seed_for('poster-t2i', n), 'lw_launch_poster_t2i'),
            os.path.join(OUT, f'poster_t2i_{n}.png'))
    canvas = tall_canvas(os.path.join(KEYFRAMES, 'c28-apollo8-launch_first_2.png'), POSTER)
    canvas_path = os.path.join(OUT, 'poster_canvas.png')
    canvas.save(canvas_path)
    name = comfy.upload(canvas, 'lw_launch_poster_canvas.png')
    for n in range(1, variants + 1):
        run(i2i(prompt_for('c28-apollo8-launch'), name, seed_for('poster-i2i', n), 'lw_launch_poster_i2i',
                0.45), os.path.join(OUT, f'poster_i2i_{n}.png'))


def banner(variants):
    for shot_id, pick in (('c34-eagle-landing', 'first_2'), ('c35-ladder', 'first_3'), ('c36-boot', 'first_3'),
                          ('c28-apollo8-launch', 'first_2')):
        src = Image.open(os.path.join(KEYFRAMES, f'{shot_id}_{pick}.png')).convert('RGB')
        big = src.resize((src.width * 2 // 16 * 16, src.height * 2 // 16 * 16), Image.LANCZOS)
        path = os.path.join(OUT, f'banner_{shot_id}_canvas.png')
        big.save(path)
        name = comfy.upload(big, f'lw_launch_banner_{shot_id}.png')
        for n in range(1, variants + 1):
            run(i2i(prompt_for(shot_id), name, seed_for('banner-' + shot_id, n), 'lw_launch_banner', 0.3),
                os.path.join(OUT, f'banner_{shot_id}_{n}.png'))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('job', nargs='?', default='all', choices=['poster', 'banner', 'all'])
    parser.add_argument('--variants', type=int, default=2)
    args = parser.parse_args()
    os.makedirs(OUT, exist_ok=True)
    if args.job in ('poster', 'all'):
        poster(args.variants)
    if args.job in ('banner', 'all'):
        banner(args.variants)


if __name__ == '__main__':
    main()
