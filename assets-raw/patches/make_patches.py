"""Generates embroidered mission patches for the goals with the user's ComfyUI workflow (Qwen Image 2.1).

Uses the same workflow as the personnel portraits (../portraits/portraits_workflow_api.json); only the
prompt differs. Qwen Image 2.1 writes an alpha channel itself, so each patch comes back as a 256x256
RGBA PNG on a transparent background.

Each goal in GOALS has its own ring text, center artwork and colors, written from the real hardware
and missions (never from BASPM's own patches, design.md 1.2). Patches carry no real-world dates,
since a player earns a goal in whatever season they reach it. The ring text is set here rather than
read from strings.json, because a patch splits a long name across the top and bottom of the ring.

Every goal gets --variants patches with their own seeds. A patch that runs off the image, or doesn't
come back as a transparent circle, is rerolled with the next seed (up to MAX_ATTEMPTS). Spelling can't
be checked by the script, so it writes review sheets (review/, not committed) to check by eye. Record
the chosen variant per goal in picks.json. Only picked variants are committed (git add -f; .gitignore
keeps the others local, 2026-10-08), so manifest.json lists some files a fresh clone won't have.

Usage (ComfyUI running locally):
    python make_patches.py                       # every goal, 3 variants each
    python make_patches.py --only human-in-orbit --variants 5
    python make_patches.py --force               # regenerate files that already exist
    python make_patches.py --review-only         # only rebuild the review sheets

Writes variants/<goal-id>_<n>.png next to this script and manifest.json (each variant's ring text,
prompt, seed and attempts). ComfyUI also keeps its own copy in its output folder (named
lw_patch_<goal-id>_<n>_*.png); this script never deletes those.
"""
import argparse
import io
import json
import os
import random
import sys
import time
import urllib.parse
import urllib.request

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
WORKFLOW = os.path.join(HERE, '..', 'portraits', 'portraits_workflow_api.json')
MANIFEST = os.path.join(HERE, 'manifest.json')
VARIANTS = os.path.join(HERE, 'variants')
REVIEW = os.path.join(HERE, 'review')
MAX_ATTEMPTS = 6

# Palettes: ring color, edge thread, lettering, field behind the artwork, and the palette line.
NASA = dict(ring='dark navy blue', edge='gold', letters='white', field='deep navy-blue space',
            palette='navy blue, gold, white, sky blue and silver')
SOVIET = dict(ring='deep scarlet red', edge='gold', letters='gold', field='deep black space',
              palette='scarlet red, gold, white, black and silver')

GOALS = [
    dict(id='earth-orbit-satellite', top='SATELLITE', bottom='IN ORBIT',
         ring='royal blue', edge='white', letters='white', field='deep black space',
         palette='royal blue, white, gold and silver',
         art='a small generic cylindrical satellite with two short antennas, in flat vintage 1950s '
             'vector-illustration style, circling a large blue-and-white Earth that fills the lower half of '
             'the patch. A thin elliptical white orbit line loops around the Earth.'),
    dict(id='radio-signals-from-orbit', top='RADIO', bottom='FROM ORBIT',
         ring='dark teal green', edge='gold', letters='white', field='deep navy-blue space',
         palette='teal green, navy blue, gold and white',
         art='a small satellite high above the curved limb of the Earth, sending concentric radio-wave arcs '
             'down to a white parabolic dish antenna standing on the ground at the bottom of the scene, in '
             'flat vintage 1950s vector-illustration style.'),
    dict(id='sputnik-1-flown', top='SPUTNIK 1', bottom='EARTH SATELLITE', **SOVIET,
         art='the Sputnik 1 satellite: a small polished silver metal sphere with four long, thin, swept-back '
             'whip antennas trailing behind it, in flat vintage 1950s vector-illustration style, flying in '
             'orbit above a large blue-and-white Earth in the lower half of the patch. Short radio-wave arcs '
             'radiate from the sphere.'),
    dict(id='explorer-1-flown', top='EXPLORER 1', bottom='COSMIC RAY SURVEY', **NASA,
         art='the Explorer 1 satellite: a long, thin, pencil-shaped cylinder with a pointed nose, striped '
             'in white and dark gray bands, with four thin wire antennas sticking out from its middle, in '
             'flat vintage 1950s vector-illustration style, spinning along a curved white orbit line above a '
             'blue Earth in the lower part of the patch.'),
    dict(id='magnetic-field-study', top='SPACE ENVIRONMENT', bottom='MEASURED',
         ring='deep purple', edge='gold', letters='gold', field='deep black space',
         palette='deep purple, gold, glowing orange, blue and white',
         art='the Earth in the center surrounded by curved magnetic field lines looping from pole to pole, '
             'and two glowing orange doughnut-shaped radiation belts around it, with a tiny satellite '
             'crossing the belts on a thin orbit line, in flat vintage 1960s scientific-diagram style.'),
    dict(id='organisms-in-microgravity', top='LIFE IN WEIGHTLESSNESS', bottom='',
         ring='forest green', edge='white', letters='gold', field='deep teal-blue space',
         palette='forest green, teal, gold, white and pale blue',
         art='a small round sealed biological container with a porthole, showing two tiny white mice and a '
             'cluster of green plant seedlings floating weightlessly inside, in flat vintage 1960s '
             'vector-illustration style, above the curved limb of the Earth. A green laurel sprig and a '
             'stylized DNA double helix frame the container on either side.'),
    dict(id='sputnik-2-flown', top='SPUTNIK 2', bottom='ORBITAL PASSENGER', **SOVIET,
         art='the Sputnik 2 satellite: a tall cone-shaped spacecraft with a small sphere near its top and a '
             'round cylindrical cabin at its base, in flat vintage 1950s vector-illustration style, orbiting '
             'above the Earth. Beside it, the head of a small alert mixed-breed dog with pointed ears, shown '
             'in profile, looks up toward the stars.'),
    dict(id='korabl-sputnik-2-flown', top='KORABL-SPUTNIK 2', bottom='SAFE RETURN', **SOVIET,
         art='a round spherical descent capsule hanging under a large white-and-orange parachute, '
             'descending toward green fields, in flat vintage 1960s vector-illustration style. Above it, the '
             'heads of two small dogs side by side, one light and one darker, both with pointed ears.'),
    dict(id='vostok-uncrewed-suborbital-flown', top='VOSTOK', bottom='UNCREWED SUBORBITAL TEST', **SOVIET,
         art='the Vostok spacecraft: a round spherical descent module sitting on a double-cone equipment '
             'module ringed by small round gas tanks, in flat vintage 1960s vector-illustration style, '
             'rising on a high arc-shaped trajectory line that climbs from the Earth and falls back to it, '
             'without ever circling the planet.'),
    dict(id='vostok-suborbital-flown', top='VOSTOK', bottom='CREWED SUBORBITAL FLIGHT', **SOVIET,
         art='the Vostok spacecraft: a round spherical descent module with a small round porthole, sitting on '
             'a double-cone equipment module ringed by small round gas tanks, in flat vintage 1960s '
             'vector-illustration style, at the top of a high arc-shaped trajectory line that climbs from the '
             'Earth and falls back to it. A small white cosmonaut helmet emblem shines above it.'),
    dict(id='vostok-uncrewed-orbital-flown', top='VOSTOK', bottom='UNCREWED ORBITAL TEST', **SOVIET,
         art='the Vostok spacecraft: a round spherical descent module sitting on a double-cone equipment '
             'module ringed by small round gas tanks, in flat vintage 1960s vector-illustration style, '
             'travelling along a full elliptical orbit line that circles all the way around a blue Earth in '
             'the center of the patch.'),
    dict(id='vostok-orbital-flown', top='VOSTOK', bottom='CREWED ORBITAL FLIGHT', **SOVIET,
         art='the Vostok spacecraft: a round spherical descent module with a small round porthole, sitting on '
             'a double-cone equipment module, in flat vintage 1960s vector-illustration style, circling a '
             'blue Earth on a full elliptical orbit line. A cosmonaut in an orange pressure suit with a white '
             'helmet descends under a small parachute near the bottom of the patch.'),
    dict(id='mercury-uncrewed-suborbital-flown', top='MERCURY', bottom='UNCREWED SUBORBITAL TEST', **NASA,
         art='the Mercury capsule: a small black cone-shaped capsule with ribbed sides, a short cylindrical '
             'nose and a red lattice escape tower on top, riding a slim white Redstone rocket, in flat vintage '
             '1960s vector-illustration style, launching on a high arc-shaped trajectory line that climbs from '
             'the coast and falls back into the blue ocean.'),
    dict(id='mercury-suborbital-flown', top='MERCURY', bottom='CREWED SUBORBITAL FLIGHT', **NASA,
         art='the Mercury capsule: a small black cone-shaped capsule with ribbed sides and a small window, in '
             'flat vintage 1960s vector-illustration style, at the top of a high arc-shaped trajectory line '
             'that climbs from the coast and falls back into the blue ocean, where a small splash and a '
             'striped parachute mark the landing.'),
    dict(id='mercury-uncrewed-orbital-flown', top='MERCURY', bottom='UNCREWED ORBITAL TEST', **NASA,
         art='the Mercury capsule: a small black cone-shaped capsule with ribbed sides, in flat vintage 1960s '
             'vector-illustration style, travelling along a full elliptical orbit line that circles all the '
             'way around a blue Earth in the center of the patch. A slim silver Atlas rocket stands at the '
             'lower left.'),
    dict(id='mercury-orbital-flown', top='MERCURY', bottom='CREWED ORBITAL FLIGHT', **NASA,
         art='the Mercury capsule: a small black cone-shaped capsule with ribbed sides and a small window, in '
             'flat vintage 1960s vector-illustration style, circling a blue Earth three times on three '
             'overlapping thin white orbit lines. A small rising sun peeks over the edge of the Earth.'),
    dict(id='duration-level-1', top='A DAY IN SPACE', bottom='',
         ring='burnt orange', edge='gold', letters='white', field='deep navy-blue space',
         palette='burnt orange, navy blue, gold and white',
         art='the curved limb of the Earth with a bright sunrise on one side and a dark night side with tiny '
             'city lights on the other, and a small generic space capsule passing between them on a thin '
             'orbit line, with a stylized clock face in the sky above, in flat vintage 1960s '
             'vector-illustration style.'),
    dict(id='duration-level-2', top='DAYS IN SPACE', bottom='',
         ring='deep maroon', edge='gold', letters='gold', field='deep navy-blue space',
         palette='maroon, navy blue, gold, white and blue',
         art='a blue Earth in the center wrapped in many thin overlapping white orbit lines, with a small '
             'generic space capsule on the outermost one, and a row of small suns and crescent moons arching '
             'over the top of the scene to mark the passing days, in flat vintage 1960s vector-illustration '
             'style.'),
    dict(id='joint-crewed-launch', top='TWO CREWS', bottom='TOGETHER IN ORBIT',
         ring='steel gray', edge='gold', letters='white', field='deep navy-blue space',
         palette='steel gray, navy blue, gold, white and blue',
         art='two identical small generic crewed space capsules flying side by side close together above the '
             'curved limb of the Earth, each on its own thin orbit line, the two lines running almost '
             'parallel, in flat vintage 1960s vector-illustration style.'),
    dict(id='human-in-space', top='HUMAN IN SPACE', bottom='',
         ring='midnight blue', edge='silver', letters='white', field='deep black space',
         palette='midnight blue, silver, white, gold and sky blue',
         art='a front-facing silhouette of a pilot in a pressure suit and rounded helmet, rising on a vertical '
             'white rocket trail above the thin blue band of the atmosphere at the curved edge of the Earth, '
             'into black space with stars, in flat vintage 1960s vector-illustration style.'),
    dict(id='human-in-orbit', top='HUMAN IN ORBIT', bottom='',
         ring='deep royal blue', edge='gold', letters='gold', field='deep black space',
         palette='royal blue, gold, white and sky blue',
         art='a pressure-suit helmet with a reflective visor in the center, with a blue Earth reflected in the '
             'visor, encircled by a thin white orbit ellipse that loops around the helmet like a ring, in flat '
             'vintage 1960s vector-illustration style.'),
]

# The standard pack's goals beyond the early era (2026-10-08).
from patches_full import FULL_GOALS  # noqa: E402
GOALS += FULL_GOALS

INTRO = ('A 1960s embroidered space mission patch, circular, isolated on a fully transparent background '
         '(PNG with alpha channel, no backdrop, no shadow, no surface beneath it). The whole patch is fully '
         'visible and centered, with an even transparent margin of about one twentieth of the image width on '
         'every side, never touching or cut off by the image edges.')


def prompt_for(goal):
    if goal['bottom']:
        bottom = (f'At the bottom of the ring, between three small gold stars on each side, is the smaller '
                  f'text "{goal["bottom"]}", curved along the border.')
    else:
        bottom = 'At the bottom of the ring are three small gold stars and no text.'
    return (f'{INTRO}\n\n'
            f'Center artwork: {goal["art"]} The background of the patch is {goal["field"]} with a scattering '
            f'of small white four-pointed stars.\n\n'
            f'Border: a wide outer ring in {goal["ring"]} with a thick {goal["edge"]} embroidered merrowed edge '
            f'and a thin inner gold ring. Inside the ring, curved along the top of the border in bold '
            f'{goal["letters"]} uppercase sans-serif embroidered lettering, is the exact text "{goal["top"]}", '
            f'evenly spaced and correctly spelled. {bottom}\n\n'
            f'Style: authentic stitched-thread texture, satin embroidery sheen, clean bold shapes, limited '
            f'palette of {goal["palette"]}, symmetrical badge composition, centered, front-facing view, sharp '
            f'detail, high resolution, 1:1 aspect ratio. No extra text, no dates, no agency logos or emblems, '
            f'no flags, no watermark, no misspelled letters.')


def seed_for(goal_id, variant, attempt):
    return random.Random(f'launch-window-patch-{goal_id}-{variant}-{attempt}').randrange(2 ** 48)


def check(image):
    """Returns None if the image is a whole transparent-backed circle, else the reason it isn't."""
    if image.mode != 'RGBA':
        return 'no alpha channel'
    alpha = image.getchannel('A')
    w, h = image.size
    if any(alpha.getpixel(p) > 10 for p in [(0, 0), (w - 1, 0), (0, h - 1), (w - 1, h - 1)]):
        return 'opaque corner'
    box = alpha.point(lambda v: 255 if v > 10 else 0).getbbox()
    if box is None:
        return 'empty'
    margin = 3
    if box[0] < margin or box[1] < margin or box[2] > w - margin or box[3] > h - margin:
        return f'touches the edge {box}'
    bw, bh = box[2] - box[0], box[3] - box[1]
    if abs(bw - bh) > 0.08 * max(bw, bh):
        return f'not round {bw}x{bh}'
    solid = sum(1 for v in alpha.crop(box).getdata() if v > 128) / (bw * bh)
    if not 0.70 <= solid <= 0.86:
        return f'not a filled circle ({solid:.2f} of its box)'
    return None


def generate(host, workflow, prompt, seed, prefix):
    """Queues one image, waits for it, and returns the PNG bytes."""
    graph = json.loads(json.dumps(workflow))
    graph.pop('_comment', None)
    graph['474']['inputs']['prompt'] = prompt
    graph['458']['inputs']['seed'] = seed
    graph['461']['inputs']['filename_prefix'] = prefix
    request = urllib.request.Request(host + '/prompt', data=json.dumps({'prompt': graph}).encode(),
                                     headers={'Content-Type': 'application/json'})
    prompt_id = json.load(urllib.request.urlopen(request))['prompt_id']
    deadline = time.time() + 600
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


def make_variant(host, workflow, goal, variant):
    """Generates one variant, rerolling the seed until it passes check(); returns its manifest entry."""
    prompt = prompt_for(goal)
    name = f'{goal["id"]}_{variant}'
    reason = None
    for attempt in range(MAX_ATTEMPTS):
        seed = seed_for(goal['id'], variant, attempt)
        data = generate(host, workflow, prompt, seed, 'lw_patch_' + name)
        reason = check(Image.open(io.BytesIO(data)))
        if reason is None or attempt == MAX_ATTEMPTS - 1:
            break
        print(f'  {name} attempt {attempt + 1} rejected: {reason}', flush=True)
    os.makedirs(VARIANTS, exist_ok=True)
    with open(os.path.join(VARIANTS, name + '.png'), 'wb') as out:
        out.write(data)
    return {'file': f'variants/{name}.png', 'goal': goal['id'], 'variant': variant, 'top': goal['top'],
            'bottom': goal['bottom'], 'seed': seed, 'attempts': attempt + 1, 'check': reason or 'ok',
            'prompt': prompt}


def checkerboard(size, cell=16):
    board = Image.new('RGBA', (size, size), (205, 205, 205, 255))
    draw = ImageDraw.Draw(board)
    for y in range(0, size, cell):
        for x in range(0, size, cell):
            if (x // cell + y // cell) % 2 == 0:
                draw.rectangle([x, y, x + cell - 1, y + cell - 1], fill=(240, 240, 240, 255))
    return board


def write_review(manifest, per_sheet=3):
    """Writes review sheets at native size: one row per goal, its variants side by side, on a
    checkerboard so the transparency shows, labelled with the goal id and variant number."""
    os.makedirs(REVIEW, exist_ok=True)
    for old in os.listdir(REVIEW):
        if old.endswith('.png'):
            os.remove(os.path.join(REVIEW, old))
    goals = []
    for entry in manifest:
        if entry['goal'] not in goals:
            goals.append(entry['goal'])
    columns = max(e['variant'] for e in manifest)
    tile, label = 256, 20
    for sheet, start in enumerate(range(0, len(goals), per_sheet), 1):
        rows = goals[start:start + per_sheet]
        image = Image.new('RGBA', (columns * tile, len(rows) * (tile + label)), (255, 255, 255, 255))
        draw = ImageDraw.Draw(image)
        for r, goal_id in enumerate(rows):
            for entry in (e for e in manifest if e['goal'] == goal_id):
                path = os.path.join(HERE, entry['file'])
                if not os.path.exists(path):
                    continue
                x, y = (entry['variant'] - 1) * tile, r * (tile + label)
                cell = checkerboard(tile)
                cell.alpha_composite(Image.open(path).convert('RGBA'))
                image.paste(cell, (x, y))
                flag = '' if entry['check'] == 'ok' else '  [' + entry['check'] + ']'
                draw.text((x + 4, y + tile + 4), f'{goal_id} #{entry["variant"]}{flag}', fill=(0, 0, 0, 255))
        image.save(os.path.join(REVIEW, f'sheet_{sheet:02d}.png'))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--host', default='http://127.0.0.1:8188')
    parser.add_argument('--variants', type=int, default=3)
    parser.add_argument('--only', help='a goal id')
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
        goals = [g for g in GOALS if args.only in (None, g['id'])]
        if not goals:
            print('unknown goal id: ' + args.only)
            return 1
        start = time.time()
        for goal in goals:
            for variant in range(1, args.variants + 1):
                known = next((e for e in manifest if e['goal'] == goal['id'] and e['variant'] == variant), None)
                if (known and not args.force and known['prompt'] == prompt_for(goal)
                        and os.path.exists(os.path.join(HERE, known['file']))):
                    continue
                entry = make_variant(args.host, workflow, goal, variant)
                manifest = [e for e in manifest if not (e['goal'] == goal['id'] and e['variant'] == variant)]
                manifest.append(entry)
                manifest.sort(key=lambda e: ([g['id'] for g in GOALS].index(e['goal']), e['variant']))
                with open(MANIFEST, 'w', encoding='utf-8', newline='\n') as out:
                    json.dump(manifest, out, indent=2, ensure_ascii=False)
                    out.write('\n')
                print(f'{entry["file"]} seed {entry["seed"]} attempts {entry["attempts"]} {entry["check"]} '
                      f'{round(time.time() - start)} s', flush=True)
    write_review(manifest)
    print('done')


if __name__ == '__main__':
    sys.exit(main())
