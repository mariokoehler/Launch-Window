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
    python make_clips.py --profile final --variants 1 --queue   # the overnight run: queue it all, exit at once
    python make_clips.py --collect                       # in the morning: fetch what ComfyUI finished
    python make_clips.py --review-only

Renders are queued in ComfyUI first and listed in clips/pending.json; ComfyUI works through its queue on its own.
Without --queue the script then waits and collects them; if it is stopped, --collect (or a rerun) picks up
where it left off, and a render ComfyUI lost (a restart) is queued again by a rerun. Before queuing, ComfyUI is
asked to unload its models (comfy.free), so nothing left over from keyframes or another workflow holds RAM.

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

MIXED = {'i2v': 'MiniMax_H3_FL2VA_pruned_mixed_int4_int8_convrot.safetensors',
         'r2v': 'MiniMax_H3_Ref2VA_pruned_mixed_int4_int8_convrot.safetensors'}
PROFILES = {
    'draft': {'size': (672, 384), 'turbo': True, 'steps': 20},
    # Finals use the community mixed INT4/INT8 models (PLAN.md §7, memory): 5 GB smaller than the workflows'
    # pruned INT8 ones, the same quality in a side-by-side test. Takes rendered before 2026-11-21 evening used
    # the INT8 model; final-int8 renders those again (same seeds).
    'final': {'size': (1344, 768), 'turbo': False, 'steps': 25, 'unet': MIXED},
    'final-int8': {'size': (1344, 768), 'turbo': False, 'steps': 25, 'seeds': 'final'},
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
    profile = PROFILES[profile].get('seeds', profile)
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
    if 'unet' in settings:
        graph[comfy.node(graph, 'UNETLoader')]['inputs']['unet_name'] = settings['unet']['i2v']
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
    if 'unet' in settings:
        graph[comfy.node(graph, 'UNETLoader')]['inputs']['unet_name'] = settings['unet']['r2v']
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
    parser.add_argument('--beat')
    parser.add_argument('--queue', action='store_true', help='queue the renders in ComfyUI and exit at once')
    parser.add_argument('--collect', action='store_true', help='fetch finished renders (pending.json), queue nothing')
    args = parser.parse_args()

    shots = read('shots.json')['shots']
    chosen = read('picks.json') if os.path.exists(PICKS) else {}
    manifest = read(os.path.join('clips', 'manifest.json')) if os.path.exists(MANIFEST) else []
    os.makedirs(os.path.join(CLIPS, args.profile), exist_ok=True)
    os.makedirs(REVIEW, exist_ok=True)

    # Every render queued and not yet collected, with what the manifest needs: ComfyUI renders on its own, so
    # nothing has to wait on it, and a killed or closed script loses nothing.
    pending_path = os.path.join(CLIPS, 'pending.json')
    pending = read(os.path.join('clips', 'pending.json')) if os.path.exists(pending_path) else {}

    def save_pending():
        with open(pending_path, 'w', encoding='utf-8') as out:
            json.dump(pending, out, indent=1)

    def save_manifest():
        with open(MANIFEST, 'w', encoding='utf-8') as out:
            json.dump(manifest, out, indent=1)

    def chosen_shot(shot):
        return (not args.only or shot['id'] in args.only) and (not args.beat or shot['beat'] == args.beat)

    # 1. Queue what is missing: every take of the chosen shots without a file and not already in ComfyUI.
    jobs = []
    for shot in shots:
        if args.review_only or args.collect or not chosen_shot(shot):
            continue
        if shot['mode'] not in WORKFLOWS:
            print(f'{shot["id"]}: mode {shot["mode"]} not wired yet, skipped')
            continue
        if shot['mode'] == 'i2v' and f'{shot["id"]}/first' not in chosen:
            print(f'{shot["id"]}: no picked first frame, skipped')
            continue
        for variant in range(1, args.variants + 1):
            file = f'clips/{args.profile}/{shot["id"]}_{variant}.mp4'
            if os.path.exists(os.path.join(HERE, file)) and not args.force:
                continue
            if file in pending and comfy.state(pending[file]['id']) != 'lost':
                continue
            jobs.append((shot, variant, file))
    if jobs:
        # Drop whatever ComfyUI still holds (Qwen Image after keyframes, the other H3 model), then queue the
        # i2v takes before the r2v ones, so the batch switches models once.
        comfy.free()
        jobs.sort(key=lambda job: job[0]['mode'])
    for shot, variant, file in jobs:
        build = i2v_graph if shot['mode'] == 'i2v' else r2v_graph
        seed = seed_for(shot['id'], args.profile, variant)
        graph, prompt = build(shot, args.profile, seed, f'video/lw_trailer_{args.profile}_{shot["id"]}_{variant}',
                              chosen)
        pending[file] = {'id': comfy.queue(graph), 'queued': time.time(), 'shot': shot['id'],
                         'profile': args.profile, 'variant': variant, 'seed': seed, 'duration': shot['duration'],
                         'prompt': prompt}
        save_pending()
        print(f'{file}  queued', flush=True)

    # 2. Collect: everything pending, finished or failed; without --queue/--collect, wait for the rest.
    wait = not (args.queue or args.collect or args.review_only)
    for file, entry in sorted(pending.items(), key=lambda item: item[1].get('queued', item[1].get('started', 0))):
        if args.review_only:
            break
        state = comfy.state(entry['id'])
        while wait and state in ('queued', 'running'):
            time.sleep(5)
            state = comfy.state(entry['id'])
        if state in ('queued', 'running'):
            print(f'{file}  {state}')
            continue
        del pending[file]
        save_pending()
        if state == 'lost':
            print(f'{file}  lost (ComfyUI restarted?): run again to queue it again')
            continue
        try:
            name, data = comfy.fetch(entry['id'])[0]
        except RuntimeError as failure:
            print(f'{file}  FAILED: {failure}')
            continue
        with open(os.path.join(HERE, file), 'wb') as out:
            out.write(data)
        seconds = comfy.render_seconds(entry['id'])
        print(f'{file}  {seconds or 0:.0f} s', flush=True)
        manifest = [e for e in manifest if e['file'] != file]
        manifest.append({'shot': entry.get('shot'), 'profile': entry.get('profile'), 'variant': entry.get('variant'),
                         'file': file, 'seed': entry.get('seed'), 'duration': entry.get('duration'),
                         'seconds': round(seconds or 0), 'prompt': entry.get('prompt')})
        save_manifest()

    order = {s['id']: i for i, s in enumerate(shots)}
    manifest.sort(key=lambda e: (order.get(e['shot'], 999), e['profile'], e['variant']))
    save_manifest()
    selected = {s['id'] for s in shots if chosen_shot(s)}
    suffix = f'_{args.beat}' if args.beat else ('_selection' if args.only else '')
    for profile in PROFILES:
        contact_sheet([e for e in manifest if e['profile'] == profile and e['shot'] in selected],
                      f'clips_{profile}{suffix}.png')


if __name__ == '__main__':
    main()
