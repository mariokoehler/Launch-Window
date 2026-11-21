"""Clips for the trailer's shots (PLAN.md §4): MiniMax H3 from the picked keyframes and the shot's prompt.

A shot's mode picks the workflow: i2v (h3_i2v_workflow_api.json; first frame, and last frame when the shot
names one). r2v and multiframe follow when a shot needs them. The prompt is built in H3's format: the
image-alignment line, then integrated_multimodal_description ([Shot 1] ...), overall_soundscape and
non_diegetic_music (always N/A: the score comes in the edit).

Profiles: draft (672x384, the Lightning LoRA) to iterate; final (1344x768, full steps) for the overnight run.
A seed gives a different clip at another size or step count, so a draft take does not come back as its final.

Usage (ComfyUI running locally):
    python make_clips.py                                 # every shot with picked keyframes, draft, 2 variants
    python make_clips.py --only t03-sputnik-orbit --variants 4
    python make_clips.py --profile final --variants 1
    python make_clips.py --review-only

Writes clips/<profile>/<shot>_<n>.mp4 and clips/manifest.json (prompt, seed, profile, render time), and contact
sheets in review/ (four frames per take). The pick per shot goes in picks.json under "<shot>/<profile>". Clips are
not committed. ComfyUI keeps its own copy (video/lw_trailer_*); this script never deletes those.
"""
import argparse
import json
import os
import random
import subprocess
import time

from PIL import Image, ImageDraw

import comfy

HERE = os.path.dirname(os.path.abspath(__file__))
CLIPS = os.path.join(HERE, 'clips')
MANIFEST = os.path.join(CLIPS, 'manifest.json')
PICKS = os.path.join(HERE, 'picks.json')
REVIEW = os.path.join(HERE, 'review')

PROFILES = {
    'draft': {'size': (672, 384), 'turbo': True, 'steps': 20},
    'final': {'size': (1344, 768), 'turbo': False, 'steps': 25},
}
WORKFLOWS = {'i2v': 'h3_i2v_workflow_api.json', 'r2v': 'h3_r2v_workflow_api.json'}
REFERENCE_SLOTS = 4  # Reference Image 0..3 in the r2v workflow


def read(path):
    with open(os.path.join(HERE, path), encoding='utf-8') as source:
        return json.load(source)


def frames(seconds):
    """H3's length for a duration: at least 5 frames, rounded up to 17k + 5 (as the workflows' math node)."""
    n = max(5, round(seconds * 24))
    return n + (5 - n % 17) % 17


def seed_for(shot, profile, variant):
    return random.Random(f'launch-window-trailer-clip-{shot}-{profile}-{variant}').randrange(2 ** 48)


def prompt_for(shot, has_last):
    if has_last:
        end = f'{frames(shot["duration"]) / 24:.2f}'
        align = ('How the reference pictures align with the target video — Picture 1 (from Shot 1) aligns with the '
                 f'0.00-second mark of the target video; Picture 2 (from Shot 1) aligns with the {end}-second mark '
                 'of the target video.')
    else:
        align = 'For the target video, at 0.00 seconds into the target video, <Picture 1> (from [Shot 1]) is fully referenced.'
    return (f'{align}\n\n'
            f'integrated_multimodal_description: [Shot 1] {shot["video"]}\n'
            f'overall_soundscape: {shot["soundscape"]}\n'
            f'non_diegetic_music: N/A')


def i2v_graph(shot, profile, seed, prefix, chosen):
    graph = comfy.load(os.path.join(HERE, WORKFLOWS['i2v']))
    settings = PROFILES[profile]
    h3 = comfy.node(graph, 'MiniMaxH3ImageToVideo')
    first = chosen[f'{shot["id"]}/first']
    graph[comfy.node(graph, 'LoadImage', 'First Frame')]['inputs']['image'] = \
        comfy.upload(Image.open(os.path.join(HERE, first)), os.path.basename(first))
    last = chosen.get(f'{shot["id"]}/last') if shot['keyframes'].get('last') else None
    if last:
        graph[comfy.node(graph, 'LoadImage', 'Last Frame')]['inputs']['image'] = \
            comfy.upload(Image.open(os.path.join(HERE, last)), os.path.basename(last))
    else:
        comfy.remove(graph, comfy.node(graph, 'LoadImage', 'Last Frame'))
    graph[h3]['inputs']['prompt'] = prompt_for(shot, bool(last))
    graph[h3]['inputs']['width'], graph[h3]['inputs']['height'] = settings['size']
    graph[comfy.node(graph, 'PrimitiveFloat', 'Float (duration)')]['inputs']['value'] = shot['duration']
    graph[comfy.node(graph, 'PrimitiveBoolean', 'Boolean (Enable Lightning LoRA)')]['inputs']['value'] = \
        shot.get('turbo', settings['turbo'])
    graph[comfy.node(graph, 'PrimitiveInt', 'Int (Full)')]['inputs']['value'] = settings['steps']
    graph[comfy.node(graph, 'RandomNoise')]['inputs']['noise_seed'] = seed
    graph[comfy.node(graph, 'SaveVideo')]['inputs']['filename_prefix'] = prefix
    return graph, graph[h3]['inputs']['prompt']


def r2v_prompt(shot):
    """H3's reference format: subjects, summary, retention, the shots, sound. A shot gives subjects and
    retention as lines; <Picture N> is its N-th reference."""
    return (f'subject_definitions: {" ".join(shot["subjects"])}\n'
            f'summary: [reference generation] {shot["summary"]}\n'
            f'retention_analysis: {" ".join(shot["retention"])}\n'
            f'detailed_description: [Shot 1] {shot["video"]}\n'
            f'overall_soundscape: {shot["soundscape"]}\n'
            f'non_diegetic_music: N/A')


def r2v_graph(shot, profile, seed, prefix, chosen):
    """References are paths under this folder (shot["references"]); unused slots are removed."""
    graph = comfy.load(os.path.join(HERE, WORKFLOWS['r2v']))
    settings = PROFILES[profile]
    references = shot['references']
    if len(references) > REFERENCE_SLOTS:
        raise ValueError(f'{shot["id"]}: {len(references)} references, the workflow has {REFERENCE_SLOTS}')
    for slot in range(REFERENCE_SLOTS):
        loader = comfy.node(graph, 'LoadImage', f'Reference Image {slot}')
        if slot < len(references):
            image = Image.open(os.path.join(HERE, references[slot])).convert('RGB')
            graph[loader]['inputs']['image'] = comfy.upload(image, f'lw_trailer_ref_{shot["id"]}_{slot}.png')
        else:
            comfy.remove(graph, loader)
    h3 = comfy.node(graph, 'MiniMaxH3ReferenceToVideo')
    graph[h3]['inputs']['width'], graph[h3]['inputs']['height'] = settings['size']
    prompt = r2v_prompt(shot)
    graph[comfy.node(graph, 'PrimitiveStringMultiline', 'Input Text (Prompt)')]['inputs']['value'] = prompt
    graph[comfy.node(graph, 'PrimitiveFloat', 'Float (Duration)')]['inputs']['value'] = shot['duration']
    graph[comfy.node(graph, 'PrimitiveBoolean', 'Boolean (Enable Lightning LoRA)')]['inputs']['value'] = \
        shot.get('turbo', settings['turbo'])
    graph[comfy.node(graph, 'PrimitiveInt', 'Int (Full)')]['inputs']['value'] = settings['steps']
    graph[comfy.node(graph, 'RandomNoise')]['inputs']['noise_seed'] = seed
    graph[comfy.node(graph, 'SaveVideo')]['inputs']['filename_prefix'] = prefix
    return graph, prompt


def contact_sheet(entries, name, columns=4, width=336):
    """One row per take: four frames across it (start, two in between, end)."""
    rows = [e for e in entries if os.path.exists(os.path.join(HERE, e['file']))]
    if not rows:
        return
    height = round(width * 768 / 1344)
    label = 18
    chosen = read('picks.json') if os.path.exists(PICKS) else {}
    sheet = Image.new('RGB', (columns * (width + 4), len(rows) * (height + label)), (255, 255, 255))
    draw = ImageDraw.Draw(sheet)
    for r, entry in enumerate(rows):
        path = os.path.join(HERE, entry['file'])
        length = frames(entry['duration']) / 24
        for c in range(columns):
            at = min(length - 0.05, c * length / (columns - 1))
            png = subprocess.run(['ffmpeg', '-loglevel', 'error', '-ss', f'{at:.2f}', '-i', path, '-frames:v', '1',
                                  '-f', 'image2pipe', '-vcodec', 'png', '-'], capture_output=True, check=True).stdout
            from io import BytesIO
            frame = Image.open(BytesIO(png)).convert('RGB').resize((width, height), Image.LANCZOS)
            sheet.paste(frame, (c * (width + 4), r * (height + label)))
        mark = '  [pick]' if chosen.get(f'{entry["shot"]}/{entry["profile"]}') == entry['file'] else ''
        draw.text((4, r * (height + label) + height + 3),
                  f'{entry["shot"]} {entry["profile"]} #{entry["variant"]}  {entry.get("seconds", 0):.0f} s{mark}',
                  fill=(0, 0, 0))
    sheet.save(os.path.join(REVIEW, name))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--only', nargs='*')
    parser.add_argument('--profile', choices=PROFILES, default='draft')
    parser.add_argument('--variants', type=int, default=2)
    parser.add_argument('--force', action='store_true')
    parser.add_argument('--review-only', action='store_true')
    args = parser.parse_args()

    shots = read('shots.json')['shots']
    chosen = read('picks.json') if os.path.exists(PICKS) else {}
    manifest = read(os.path.join('clips', 'manifest.json')) if os.path.exists(MANIFEST) else []
    os.makedirs(os.path.join(CLIPS, args.profile), exist_ok=True)
    os.makedirs(REVIEW, exist_ok=True)

    for shot in shots:
        if args.only and shot['id'] not in args.only:
            continue
        if args.review_only:
            break
        if shot['mode'] not in WORKFLOWS:
            print(f'{shot["id"]}: mode {shot["mode"]} not wired yet, skipped')
            continue
        if shot['mode'] == 'i2v' and f'{shot["id"]}/first' not in chosen:
            print(f'{shot["id"]}: no picked first frame, skipped')
            continue
        build = i2v_graph if shot['mode'] == 'i2v' else r2v_graph
        for variant in range(1, args.variants + 1):
            file = f'clips/{args.profile}/{shot["id"]}_{variant}.mp4'
            path = os.path.join(HERE, file)
            if os.path.exists(path) and not args.force:
                continue
            seed = seed_for(shot['id'], args.profile, variant)
            graph, prompt = build(shot, args.profile, seed,
                                      f'video/lw_trailer_{args.profile}_{shot["id"]}_{variant}', chosen)
            started = time.time()
            name, data = comfy.run(graph)[0]
            seconds = time.time() - started
            with open(path, 'wb') as out:
                out.write(data)
            print(f'{file}  {seconds:.0f} s', flush=True)
            manifest = [e for e in manifest if e['file'] != file]
            manifest.append({'shot': shot['id'], 'profile': args.profile, 'variant': variant, 'file': file,
                             'seed': seed, 'duration': shot['duration'], 'seconds': round(seconds), 'prompt': prompt})
            with open(MANIFEST, 'w', encoding='utf-8') as out:
                json.dump(manifest, out, indent=1)

    order = {s['id']: i for i, s in enumerate(shots)}
    manifest.sort(key=lambda e: (order.get(e['shot'], 999), e['profile'], e['variant']))
    for profile in PROFILES:
        contact_sheet([e for e in manifest if e['profile'] == profile], f'clips_{profile}.png')


if __name__ == '__main__':
    main()
