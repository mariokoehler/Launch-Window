"""Keyframes for the trailer's shots (PLAN.md §4): Qwen Image 2.1 stills at H3's native 1344x768.

Every keyframe a shot names (first, last) gets --variants stills with fixed seeds. The prompt is the shot's
keyframe text plus its look (looks.json). The graph is the portraits' Qwen text-to-image workflow
(../portraits/portraits_workflow_api.json), run at 1344x768.

Review sheets go to review/keyframes_NN.png (not committed); record the chosen file per keyframe in
picks.json under "<shot>/<frame>". Only picked stills are committed (git add -f).

Usage (ComfyUI running locally):
    python make_keyframes.py                        # every shot, 3 variants each
    python make_keyframes.py --only t03-sputnik-orbit --variants 4
    python make_keyframes.py --force                # regenerate files that already exist
    python make_keyframes.py --review-only

Writes keyframes/<shot>_<frame>_<n>.png and keyframes/manifest.json (prompt and seed per still). ComfyUI keeps
its own copy (lw_trailer_key_*); this script never deletes those.
"""
import argparse
import json
import os
import random
import time

from PIL import Image, ImageDraw

import comfy

HERE = os.path.dirname(os.path.abspath(__file__))
WORKFLOW = os.path.join(HERE, '..', 'portraits', 'portraits_workflow_api.json')
KEYFRAMES = os.path.join(HERE, 'keyframes')
MANIFEST = os.path.join(KEYFRAMES, 'manifest.json')
PICKS = os.path.join(HERE, 'picks.json')
REVIEW = os.path.join(HERE, 'review')
SIZE = (1344, 768)


def read(name):
    with open(os.path.join(HERE, name), encoding='utf-8') as source:
        return json.load(source)


def seed_for(shot, frame, variant):
    return random.Random(f'launch-window-trailer-key-{shot}-{frame}-{variant}').randrange(2 ** 48)


def picks():
    return read('picks.json') if os.path.exists(PICKS) else {}


def graph_for(workflow, prompt, seed, prefix):
    graph = json.loads(json.dumps(workflow))
    graph['474']['inputs']['prompt'] = prompt
    graph['458']['inputs']['seed'] = seed
    graph['456']['inputs']['width'], graph['456']['inputs']['height'] = SIZE
    graph['468']['inputs']['switch'] = True  # the empty latent at our size, not the encoder's
    graph['461']['inputs']['filename_prefix'] = prefix
    return graph


def write_review(manifest, per_sheet=4, scale=0.5):
    os.makedirs(REVIEW, exist_ok=True)
    for old in os.listdir(REVIEW):
        if old.startswith('keyframes_'):
            os.remove(os.path.join(REVIEW, old))
    rows = []
    for entry in manifest:
        if entry['key'] not in rows:
            rows.append(entry['key'])
    columns = max(e['variant'] for e in manifest)
    chosen = picks()
    w, h = round(SIZE[0] * scale), round(SIZE[1] * scale)
    label = 18
    for sheet, start in enumerate(range(0, len(rows), per_sheet), 1):
        chunk = rows[start:start + per_sheet]
        image = Image.new('RGB', (columns * (w + 4), len(chunk) * (h + label)), (255, 255, 255))
        draw = ImageDraw.Draw(image)
        for r, key in enumerate(chunk):
            for entry in (e for e in manifest if e['key'] == key):
                path = os.path.join(HERE, entry['file'])
                if not os.path.exists(path):
                    continue
                x, y = (entry['variant'] - 1) * (w + 4), r * (h + label)
                image.paste(Image.open(path).convert('RGB').resize((w, h), Image.LANCZOS), (x, y))
                mark = '  [pick]' if chosen.get(key) == entry['file'] else ''
                draw.text((x + 4, y + h + 3), f'{key} #{entry["variant"]}{mark}', fill=(0, 0, 0))
        image.save(os.path.join(REVIEW, f'keyframes_{sheet:02d}.png'))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--only', nargs='*')
    parser.add_argument('--variants', type=int, default=3)
    parser.add_argument('--force', action='store_true')
    parser.add_argument('--review-only', action='store_true')
    parser.add_argument('--beat')
    args = parser.parse_args()

    shots = read('shots.json')['shots']
    looks = read('looks.json')
    manifest = read(os.path.join('keyframes', 'manifest.json')) if os.path.exists(MANIFEST) else []
    os.makedirs(KEYFRAMES, exist_ok=True)

    if not args.review_only:
        workflow = comfy.load(WORKFLOW)
        jobs = []
        for shot in shots:
            if (args.only and shot['id'] not in args.only) or (args.beat and shot['beat'] != args.beat):
                continue
            for frame, text in shot['keyframes'].items():
                if not text:
                    continue
                prompt = f'{text}\n\n{looks[shot["look"]]}'
                for variant in range(1, args.variants + 1):
                    key = f'{shot["id"]}/{frame}'
                    file = f'keyframes/{shot["id"]}_{frame}_{variant}.png'
                    seed = seed_for(shot['id'], frame, variant)
                    manifest = [e for e in manifest if e['file'] != file]
                    manifest.append({'key': key, 'variant': variant, 'file': file, 'seed': seed, 'prompt': prompt})
                    path = os.path.join(HERE, file)
                    if os.path.exists(path) and not args.force:
                        continue
                    jobs.append((file, path, graph_for(workflow, prompt, seed,
                                                       f'lw_trailer_key_{shot["id"]}_{frame}_{variant}')))
        # All stills go in at once, at the front of ComfyUI's queue: they take seconds each, so they run ahead of
        # any clip batch (one model swap, not one per still). The H3 models a clip run left in RAM are dropped first.
        if jobs:
            comfy.free()
            started = time.time()
            ids = [(file, path, comfy.queue(graph, front=True)) for file, path, graph in jobs]
            for file, path, prompt_id in ids:
                data = comfy.wait(prompt_id, timeout=1800)[0][1]
                with open(path, 'wb') as out:
                    out.write(data)
                print(f'{file}  {time.time() - started:.0f} s', flush=True)
        manifest.sort(key=lambda e: (e['key'], e['variant']))
        with open(MANIFEST, 'w', encoding='utf-8') as out:
            json.dump(manifest, out, indent=1)
    selected = {s['id'] for s in shots if (not args.only or s['id'] in args.only)
                and (not args.beat or s['beat'] == args.beat)}
    write_review([e for e in manifest if e['key'].split('/')[0] in selected])


if __name__ == '__main__':
    main()
