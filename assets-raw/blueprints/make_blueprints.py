"""Generates blueprint drawings of the mission components for the turn report's R&D section.

Each drawing is generated from BASPM's own R&D render of that component (read from E:, never copied
into the repo) plus a prompt written from that render in our own words (descriptions.json) and a shared
style block, with the user's ComfyUI workflow render_to_blueprint.json (Qwen Image 2.1), flattened to
blueprints_workflow_api.json. The originals are only a reference; the drawing style, line work and
lettering are the model's (user decision 2026-10-09).

Every original sits on the same 4096x4096 sheet in the same panel, so one fixed crop fits them all. The
crop is scaled to REFERENCE_WIDTH and uploaded to ComfyUI; the output is OUTPUT_SIZE.

Our component ids map to BASPM's through COMPONENTS in E:\\Temp\\BASPM\\tools\\full_maps.py, which is read
at run time so BASPM's ids stay out of the repo. This script only runs on the main PC.

Every component gets --variants drawings with their own seeds. A description
with a "reference" uses that image instead of BASPM's render (mig-e-154-spaceplane: BASPM's is a different
craft, so its reference is composed from the user's own art). Review
sheets go to review/ (not committed); record the chosen file per component in picks.json. Only the
picked variants are kept and committed (git add -f); manifest.json still lists every seed.

Usage (ComfyUI running locally):
    python make_blueprints.py                              # every described component, 3 variants each
    python make_blueprints.py --only mercury-capsule --variants 4
    python make_blueprints.py --force                      # regenerate files that already exist
    python make_blueprints.py --review-only

Writes variants/<component-id>_<style>_<n>.png and manifest.json (prompt, seed per variant). ComfyUI
also keeps its own copy in its output folder (lw_blueprint_*); this script never deletes those.
"""
import argparse
import importlib.util
import io
import json
import os
import random
import sys
import time
import urllib.parse
import urllib.request
import uuid

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
WORKFLOW = os.path.join(HERE, 'blueprints_workflow_api.json')
DESCRIPTIONS = os.path.join(HERE, 'descriptions.json')
MANIFEST = os.path.join(HERE, 'manifest.json')
PICKS = os.path.join(HERE, 'picks.json')
VARIANTS = os.path.join(HERE, 'variants')
REVIEW = os.path.join(HERE, 'review')

BASPM = r'E:\Temp\BASPM'
FULL_MAPS = os.path.join(BASPM, 'tools', 'full_maps.py')
SOURCE_CROP = (815, 1677, 3281, 2454)  # the panel, without its header strip and rounded corners
REFERENCE_WIDTH = 864
OUTPUT_SIZE = (864, 352)

# The background, by name. Only the soft gradient is used (user decision 2026-10-09): a paper texture
# clashed with the UI's own engineering-paper background, and a stronger gradient lost the grid and the
# pencil feel.
STYLES = {
    'soft': ('The background is a rich blueprint blue with a gentle, smooth vertical gradient: a slightly lighter '
             'blueprint blue at the top, fading evenly to a deeper navy blue at the bottom, never pale and never '
             'black, with a clearly visible fine grid of thin light-blue lines over the whole sheet. No paper '
             'texture, no border or frame around the sheet.'),
}

INTRO = ('A hand-drawn technical engineering drawing of {subject}, made by a skilled aerospace draftsman using '
         'professional drafting pencils on classic blueprint paper. {background} The {object} is drawn in white '
         'and pale ice-blue pencil. The dark markings are deep navy, almost black. Wide landscape layout, {layout}.')

OUTRO = ('Drafting style: precise, confident line work with varied line weights — fine, sharp white pencil for thin '
         'construction and center lines, firmer white strokes for outlines. Soft pale-blue shading on curved '
         'surfaces shows their roundness, with visible pencil grain and directional hatching strokes. Faint '
         'construction lines, dash-dot center lines through each view, small dimension lines with arrowheads and '
         'hand-lettered measurements in white, slight smudges and a few visible guide marks. A small hand-lettered '
         'title block in the lower right corner. Authentic vintage 1960s NASA engineering blueprint feel, '
         'hand-drawn on paper rather than printed, high detail, sharp focus, evenly lit flat scan of the sheet.')


def prompt_for(entry, style):
    intro = INTRO.format(subject=entry['subject'], background=STYLES[style], object=entry['object'],
                         layout=entry['layout'])
    return f'{intro}\n\n{entry["body"]}\n\n{OUTRO}'


def seed_for(component_id, style, variant):
    return random.Random(f'launch-window-blueprint-{component_id}-{style}-{variant}').randrange(2 ** 48)


def baspm_ids():
    """Our component id -> BASPM's, from the converter's maps."""
    spec = importlib.util.spec_from_file_location('full_maps', FULL_MAPS)
    maps = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(maps)
    return {ours: theirs for theirs, ours in maps.COMPONENTS.items()}


def source_path(baspm_id):
    """The 16:9 render; BASPM splits them over four bundles by the id's first letter."""
    c = baspm_id[0]
    bundle = '1' if 'A' <= c < 'J' else '2' if 'J' <= c < 'P' else '3' if 'P' <= c <= 'R' else '4'
    return os.path.join(BASPM, 'extracted', 'AssetBundles', f'RDResults{bundle}-16.9.unity3d', 'Texture2D',
                        f'MsnCmpRDLrg_{baspm_id}.png')


def reference(entry, baspm_id):
    """BASPM's render, or the entry's own reference (a path under this folder) where BASPM's render shows
    something else than our component: references/ holds those, made from the user's art."""
    if 'reference' in entry:
        flat = Image.open(os.path.join(HERE, entry['reference'])).convert('RGB')
    else:
        image = Image.open(source_path(baspm_id)).convert('RGBA').crop(SOURCE_CROP)
        flat = Image.new('RGB', image.size, (0, 0, 0))
        flat.paste(image, mask=image.getchannel('A'))
    height = round(flat.height * REFERENCE_WIDTH / flat.width)
    return flat.resize((REFERENCE_WIDTH, height), Image.LANCZOS)


def upload(host, image, name):
    """Uploads a PNG to ComfyUI's input folder and returns the name to load it by."""
    data = io.BytesIO()
    image.save(data, 'PNG')
    boundary = uuid.uuid4().hex
    body = (f'--{boundary}\r\nContent-Disposition: form-data; name="image"; filename="{name}"\r\n'
            f'Content-Type: image/png\r\n\r\n').encode() + data.getvalue() + \
           (f'\r\n--{boundary}\r\nContent-Disposition: form-data; name="overwrite"\r\n\r\ntrue'
            f'\r\n--{boundary}--\r\n').encode()
    request = urllib.request.Request(host + '/upload/image', data=body,
                                     headers={'Content-Type': f'multipart/form-data; boundary={boundary}'})
    answer = json.load(urllib.request.urlopen(request))
    return (answer['subfolder'] + '/' if answer.get('subfolder') else '') + answer['name']


def generate(host, workflow, reference_name, prompt, seed, prefix):
    """Queues one image, waits for it, and returns the PNG bytes."""
    graph = json.loads(json.dumps(workflow))
    graph.pop('_comment', None)
    graph['483']['inputs']['image'] = reference_name
    graph['474']['inputs']['prompt'] = prompt
    graph['456']['inputs']['width'], graph['456']['inputs']['height'] = OUTPUT_SIZE
    graph['458']['inputs']['seed'] = seed
    graph['461']['inputs']['filename_prefix'] = prefix
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


def write_review(manifest, per_sheet=4):
    """One sheet per few components: a row per component and style, its variants side by side at full
    size, so fine detail can be judged. The current pick (picks.json) is marked."""
    os.makedirs(REVIEW, exist_ok=True)
    for old in os.listdir(REVIEW):
        if old.startswith('sheet_') and old.endswith('.png'):
            os.remove(os.path.join(REVIEW, old))
    rows = []
    for entry in manifest:
        if (entry['component'], entry['style']) not in rows:
            rows.append((entry['component'], entry['style']))
    columns = max(e['variant'] for e in manifest)
    picks = {}
    if os.path.exists(PICKS):
        with open(PICKS, encoding='utf-8') as source:
            picks = json.load(source)
    w, h = OUTPUT_SIZE
    label = 18
    for sheet, start in enumerate(range(0, len(rows), per_sheet), 1):
        chunk = rows[start:start + per_sheet]
        image = Image.new('RGB', (columns * (w + 4), len(chunk) * (h + label)), (255, 255, 255))
        draw = ImageDraw.Draw(image)
        for r, (component, style) in enumerate(chunk):
            for entry in (e for e in manifest if e['component'] == component and e['style'] == style):
                path = os.path.join(HERE, entry['file'])
                if not os.path.exists(path):
                    continue
                x, y = (entry['variant'] - 1) * (w + 4), r * (h + label)
                image.paste(Image.open(path).convert('RGB'), (x, y))
                picked = '  [pick]' if picks.get(component) == entry['file'] else ''
                draw.text((x + 4, y + h + 3), f'{component} {style} #{entry["variant"]}{picked}', fill=(0, 0, 0))
        image.save(os.path.join(REVIEW, f'sheet_{sheet:02d}.png'))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--host', default='http://127.0.0.1:8188')
    parser.add_argument('--variants', type=int, default=3)
    parser.add_argument('--styles', default='soft')
    parser.add_argument('--only', help='comma-separated component ids')
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
        with open(DESCRIPTIONS, encoding='utf-8') as source:
            descriptions = json.load(source)
        ids = baspm_ids()
        only = set(args.only.split(',')) if args.only else None
        components = [c for c in descriptions if only is None or c in only]
        if only and only - set(descriptions):
            print('not described: ' + ', '.join(sorted(only - set(descriptions))))
            return 1
        styles = args.styles.split(',')
        start = time.time()
        for component in components:
            reference_name = None
            for style in styles:
                prompt = prompt_for(descriptions[component], style)
                for variant in range(1, args.variants + 1):
                    name = f'{component}_{style}_{variant}'
                    known = next((e for e in manifest if e['file'] == f'variants/{name}.png'), None)
                    # A variant in the manifest is done even when its file is gone: only the picks
                    # are kept (user, 2026-10-09). --force, or a changed prompt, generates it again.
                    if known and not args.force and known['prompt'] == prompt:
                        continue
                    if reference_name is None:
                        reference_name = upload(args.host, reference(descriptions[component], ids.get(component)), f'lw_blueprint_ref_{component}.png')
                    seed = seed_for(component, style, variant)
                    data = generate(args.host, workflow, reference_name, prompt, seed, 'lw_blueprint_' + name)
                    os.makedirs(VARIANTS, exist_ok=True)
                    with open(os.path.join(HERE, 'variants', name + '.png'), 'wb') as out:
                        out.write(data)
                    entry = {'file': f'variants/{name}.png', 'component': component, 'style': style,
                             'variant': variant, 'seed': seed, 'prompt': prompt}
                    manifest = [e for e in manifest if e['file'] != entry['file']]
                    manifest.append(entry)
                    order = list(descriptions)
                    manifest.sort(key=lambda e: (order.index(e['component']) if e['component'] in order else 999,
                                                 e['style'], e['variant']))
                    with open(MANIFEST, 'w', encoding='utf-8', newline='\n') as out:
                        json.dump(manifest, out, indent=2, ensure_ascii=False)
                        out.write('\n')
                    print(f'{entry["file"]} seed {seed} {round(time.time() - start)} s', flush=True)
    write_review(manifest)
    print('done')


if __name__ == '__main__':
    sys.exit(main())
