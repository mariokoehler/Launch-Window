"""Generates the art for the Firsts' front pages (ui-spec.md 4.18) with the user's ComfyUI workflow
(Qwen Image 2.1).

Uses the same workflow as the portraits and patches (../portraits/portraits_workflow_api.json), with the
latent resized per job. Three kinds of art, all text-free except the masthead:

- paper: a blank sheet of aged newsprint, landscape, the page's background (the page is the top half
  of a broadsheet, "above the fold").
- masthead: the fictional paper's nameplate, The Daily Zenith. The model writes the name itself, so
  no font licence is involved; spelling is checked by eye.
- one press photo per First (FIRSTS below), black and white, 1960s photojournalism. No flags, no
  insignia, no lettering: the agencies are the players', never a real country's.

Prompts are our own, written from what the real moments looked like, never from BASPM's art
(design.md 1.2). Every job gets --variants images with fixed seeds. There is no automatic check: the
model sometimes adds an emblem to a suit or letters to a rocket, so review sheets (review/, not
committed) are checked by eye. Record the chosen variant per job in picks.json. Only picked variants are
committed (git add -f; .gitignore keeps the others local).

Usage (ComfyUI running locally):
    python make_frontpage_art.py                      # every job, 3 variants each
    python make_frontpage_art.py --only photo-human-in-orbit --variants 5
    python make_frontpage_art.py --force              # regenerate files that already exist
    python make_frontpage_art.py --review-only        # only rebuild the review sheets

Writes variants/<job>_<n>.png and manifest.json (each variant's prompt, size and seed). ComfyUI keeps
its own copy in its output folder (lw_frontpage_<job>_<n>_*.png); this script never deletes those.
Then run make_frontpage_assets.py.
"""
import argparse
import json
import os
import random
import time
import urllib.parse
import urllib.request

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
WORKFLOW = os.path.join(HERE, '..', 'portraits', 'portraits_workflow_api.json')
MANIFEST = os.path.join(HERE, 'manifest.json')
VARIANTS = os.path.join(HERE, 'variants')
REVIEW = os.path.join(HERE, 'review')

PHOTO = ('A grainy black-and-white 1960s press photograph. {scene} Shot on a press camera, strong contrast, '
         'deep blacks, film grain, documentary photojournalism, monochrome. No text, no captions, no logos, '
         'no flags, no insignia, no badges or emblems on clothing, no lettering anywhere.')

# The page's Firsts, in the order of front-pages.json. Sizes are multiples of 64 near the photo slot's
# aspect (about 3:2 landscape).
FIRSTS = [
    ('earth-orbit-satellite',
     'At dusk, a small crowd of ordinary people on a city rooftop looks up at the sky; a man in a hat '
     'points at a tiny bright moving point of light among the first stars, and a woman beside him holds '
     'a portable transistor radio to her ear. Chimneys and television aerials in silhouette.'),
    ('human-in-space',
     'A slim single-stage rocket lifts off from a launch pad at dawn, a tall column of bright exhaust '
     'and billowing white smoke spreading across the pad, the steel launch gantry to one side, seen from '
     'a distance across flat coastal scrubland. The rocket has no markings.'),
    ('human-in-orbit',
     'A young test pilot in a bulky silver pressure suit, holding his round white helmet under one arm, '
     'smiles and waves to a crowd as he walks across an airfield tarmac, flanked by two technicians in '
     'white coveralls. Behind them, the open hatch of a small scorched space capsule on a recovery '
     'truck. Slight motion blur.'),
    ('crewed-lunar-flyby',
     'A photograph taken from inside a spacecraft through a small round window: the grey cratered '
     'surface of the Moon fills the lower part of the frame, close and rugged, and above its horizon '
     'a small half-lit Earth hangs in the black sky. The dark window frame and its bolts surround the '
     'view. Slightly soft focus, coarse grain, like a print from a returned film magazine.'),
    ('crewed-lunar-landing',
     'A ghostly, low-resolution image as received by television from the Moon: an astronaut in a '
     'bulky white spacesuit and backpack stands on grey powdery lunar ground beside the spindly '
     'foil-wrapped leg of a landing craft, long black shadows across the dust, black sky. Visible '
     'television scan lines, high contrast, blurred highlights.'),
]

JOBS = {
    'paper': dict(w=1344, h=832, prompt=(
        'A flat, evenly lit scan of the top half of a single blank sheet of old 1960s newsprint paper, '
        'landscape, filling the whole image edge to edge. Slightly yellowed off-white cream paper with '
        'fine fibrous grain, very faint mottled age toning, a little darker toward the edges and corners. '
        'Completely blank: no text, no print, no letters, no images, no lines, no folds, no stains. '
        'Top-down orthographic view, no shadows, no background, no table, no perspective.')),
    'masthead': dict(w=1344, h=256, prompt=(
        'A newspaper masthead nameplate on a plain flat white background, printed in solid black ink: the '
        'exact words "The Daily Zenith" in a large, elegant, traditional blackletter (Old English gothic) '
        'typeface, centered, correctly spelled, with a small engraved emblem between "Daily" and "Zenith" '
        'of a rising sun with rays over a horizon. Nothing else: no other text, no date, no lines, no '
        'border. Crisp vector-sharp edges, high contrast, flat, 1960s newspaper style.')),
}
for first_id, scene in FIRSTS:
    JOBS['photo-' + first_id] = dict(w=1024, h=704, prompt=PHOTO.format(scene=scene))


def seed_for(job, variant):
    return random.Random(f'launch-window-frontpage-{job}-{variant}').randrange(2 ** 48)


def generate(host, workflow, job, prompt, seed, prefix):
    """Queues one image, waits for it, and returns the PNG bytes."""
    graph = json.loads(json.dumps(workflow))
    graph.pop('_comment', None)
    graph['474']['inputs']['prompt'] = prompt
    graph['458']['inputs']['seed'] = seed
    graph['461']['inputs']['filename_prefix'] = prefix
    graph['456']['inputs']['width'] = job['w']
    graph['456']['inputs']['height'] = job['h']
    request = urllib.request.Request(host + '/prompt', data=json.dumps({'prompt': graph}).encode(),
                                     headers={'Content-Type': 'application/json'})
    prompt_id = json.load(urllib.request.urlopen(request))['prompt_id']
    deadline = time.time() + 900
    while time.time() < deadline:
        history = json.load(urllib.request.urlopen(host + '/history/' + prompt_id))
        if prompt_id in history:
            status = history[prompt_id].get('status', {})
            if status.get('status_str') == 'error':
                raise RuntimeError(json.dumps(status)[:1000])
            for output in history[prompt_id]['outputs'].values():
                for image in output.get('images', []):
                    query = urllib.parse.urlencode({'filename': image['filename'], 'subfolder': image['subfolder'],
                                                    'type': image['type']})
                    return urllib.request.urlopen(host + '/view?' + query).read()
            raise RuntimeError('no image in the output')
        time.sleep(1)
    raise RuntimeError('timed out')


def write_review(manifest):
    """One sheet per job: its variants side by side at half size, labelled."""
    os.makedirs(REVIEW, exist_ok=True)
    for old in os.listdir(REVIEW):
        if old.endswith('.png'):
            os.remove(os.path.join(REVIEW, old))
    for name in JOBS:
        entries = [e for e in manifest if e['job'] == name and os.path.exists(os.path.join(HERE, e['file']))]
        if not entries:
            continue
        tiles = [Image.open(os.path.join(HERE, e['file'])).convert('RGB') for e in entries]
        w = max(t.width for t in tiles) // 2
        h = max(t.height for t in tiles) // 2
        sheet = Image.new('RGB', (w * len(tiles), h + 20), (255, 255, 255))
        draw = ImageDraw.Draw(sheet)
        for i, (entry, tile) in enumerate(zip(entries, tiles)):
            sheet.paste(tile.resize((tile.width // 2, tile.height // 2), Image.LANCZOS), (i * w, 0))
            draw.text((i * w + 4, h + 4), f'{name} #{entry["variant"]}', fill=(0, 0, 0))
        sheet.save(os.path.join(REVIEW, name + '.png'))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--host', default='http://127.0.0.1:8188')
    parser.add_argument('--variants', type=int, default=3)
    parser.add_argument('--only', help='a job name (paper, masthead, photo-<first-id>)')
    parser.add_argument('--force', action='store_true')
    parser.add_argument('--review-only', action='store_true')
    args = parser.parse_args()
    manifest = []
    if os.path.exists(MANIFEST):
        with open(MANIFEST, encoding='utf-8') as source:
            manifest = json.load(source)
    if not args.review_only:
        with open(WORKFLOW, encoding='utf-8') as source:
            workflow = json.load(source)
        os.makedirs(VARIANTS, exist_ok=True)
        for name, job in JOBS.items():
            if args.only not in (None, name):
                continue
            for variant in range(1, args.variants + 1):
                file = f'variants/{name}_{variant}.png'
                if os.path.exists(os.path.join(HERE, file)) and not args.force:
                    continue
                seed = seed_for(name, variant)
                data = generate(args.host, workflow, job, job['prompt'], seed, f'lw_frontpage_{name}_{variant}')
                with open(os.path.join(HERE, file), 'wb') as out:
                    out.write(data)
                manifest = [e for e in manifest if e['file'] != file]
                manifest.append({'file': file, 'job': name, 'variant': variant, 'width': job['w'],
                                 'height': job['h'], 'seed': seed, 'prompt': job['prompt']})
                print(file, flush=True)
        manifest.sort(key=lambda e: (list(JOBS).index(e['job']) if e['job'] in JOBS else 99, e['variant']))
        with open(MANIFEST, 'w', encoding='utf-8') as out:
            json.dump(manifest, out, indent=2, ensure_ascii=False)
            out.write('\n')
    write_review(manifest)


if __name__ == '__main__':
    main()
