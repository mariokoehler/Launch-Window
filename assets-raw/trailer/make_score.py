"""The trailer's score (PLAN.md §4, step 5): cues generated with Stable Audio 3 in ComfyUI, fitted to the cut.

No music model places a swell at a given second (tried 2026-11-23: MiniMax Music 3 and Stable Audio 3 follow
a mood, not a timeline), so the fitting happens here, the way an editor cuts library music to picture. Each cue
in score.json is one mood for one stretch of the film. Its picked take is slid so the moment its loudness
arrives at its peak lands on the cue's "hit" (a shot's start plus an offset), then heard only from "from" to
"to" (shots too), faded in and out over their "fade" seconds; a fade of 0 is a hard stop (the music cuts out
for 1967 and on the N1's flash). make_edit.py calls mix() with its own timeline and lays the result on A4.

Usage:
    python make_score.py --queue [cue ...]     # every seed of these cues (all if none) to ComfyUI
    python make_score.py --collect             # finished takes to music/takes/ as OGG, with a look at each
    python make_score.py --look music/takes/x.ogg   # the loudness curve and peak of any file
    python make_score.py [--profile final]     # mixes build/score-<profile>.wav for the whole film

A take's peak is where its loudness, smoothed over a second, first comes within PEAK_DB of its maximum.
A cue's "peak" (seconds into the picked take) overrides that when the ear disagrees.
"""
import argparse
import glob
import json
import os
import subprocess
import urllib.request

HERE = os.path.dirname(os.path.abspath(__file__))
BUILD = os.path.join(HERE, 'build')
TAKES = os.path.join(HERE, 'music', 'takes')
COMFY = 'http://127.0.0.1:8188'
OUTPUT = r'E:\Comfy-Desktop\ComfyUI-Shared\output'
PREFIX = 'lw_trailer_score'
PEAK_DB = 1.5
RATE = 48000


def read(name):
    with open(os.path.join(HERE, name), encoding='utf-8') as source:
        return json.load(source)


def graph(prompt, seconds, seed, prefix):
    """ComfyUI's Stable Audio 3 template, flattened (without its Qwen prompt rewriter)."""
    return {
        '1': {'class_type': 'CheckpointLoaderSimple', 'inputs': {'ckpt_name': 'stable_audio_3_medium.safetensors'}},
        '2': {'class_type': 'CLIPLoader', 'inputs': {'clip_name': 't5gemma_b_b_ul2.safetensors', 'type': 'stable_audio',
                                                     'device': 'default'}},
        '3': {'class_type': 'CLIPTextEncode', 'inputs': {'clip': ['2', 0], 'text': prompt}},
        '4': {'class_type': 'CLIPTextEncode', 'inputs': {'clip': ['2', 0], 'text': ''}},
        '5': {'class_type': 'EmptyLatentAudio', 'inputs': {'seconds': seconds, 'batch_size': 1}},
        '6': {'class_type': 'KSampler', 'inputs': {
            'model': ['1', 0], 'positive': ['3', 0], 'negative': ['4', 0], 'latent_image': ['5', 0], 'seed': seed,
            'steps': 8, 'cfg': 1.0, 'sampler_name': 'lcm', 'scheduler': 'simple', 'denoise': 1.0}},
        '7': {'class_type': 'VAEDecodeAudio', 'inputs': {'samples': ['6', 0], 'vae': ['1', 2]}},
        '8': {'class_type': 'SaveAudio', 'inputs': {'audio': ['7', 0], 'filename_prefix': prefix}},
    }


def queue(ids):
    for cue in read('score.json')['cues']:
        if ids and cue['id'] not in ids:
            continue
        for seed in cue['seeds']:
            body = json.dumps({'prompt': graph(cue['prompt'], cue['seconds'], seed,
                                               f'{PREFIX}_{cue["id"]}_{seed}')}).encode()
            request = urllib.request.Request(COMFY + '/prompt', data=body, headers={'Content-Type': 'application/json'})
            print(cue['id'], seed, json.load(urllib.request.urlopen(request)).get('prompt_id'))


def loudness(path):
    """Loudness in dB per 10 ms, smoothed over a second, and the take's length."""
    import numpy as np
    raw = subprocess.run(['ffmpeg', '-v', 'error', '-i', path, '-ac', '1', '-ar', '8000', '-f', 'f32le', '-'],
                         capture_output=True, check=True).stdout
    y = np.frombuffer(raw, dtype=np.float32)
    hop = 80
    frames = len(y) // hop
    rms = np.sqrt(np.mean(y[:frames * hop].reshape(frames, hop) ** 2, axis=1) + 1e-12)
    db = 20 * np.log10(rms / rms.max())
    smooth = np.convolve(db, np.ones(100) / 100, mode='same')
    return smooth, len(y) / 8000


def peak_of(path):
    import numpy as np
    smooth, _ = loudness(path)
    # Ignore the last second (a decoded take can end in a click) when finding the maximum.
    body = smooth[:-100] if len(smooth) > 200 else smooth
    return float(np.argmax(body >= body.max() - PEAK_DB)) / 100


def look(path):
    """A PNG beside the take: the loudness curve, the peak marked. Prints the peak and length."""
    import matplotlib
    matplotlib.use('Agg')
    import matplotlib.pyplot as plt
    import numpy as np
    smooth, length = loudness(path)
    peak = peak_of(path)
    fig, ax = plt.subplots(figsize=(12, 2.6))
    ax.plot(np.arange(len(smooth)) / 100, smooth, lw=1)
    ax.axvline(peak, color='red', lw=1)
    ax.set_ylim(-50, 2)
    ax.set_xticks(np.arange(0, length + 1, 5))
    ax.grid(axis='x', alpha=0.3)
    ax.set_title(f'{os.path.basename(path)}  {length:.1f} s  peak {peak:.1f} s')
    fig.tight_layout()
    fig.savefig(os.path.splitext(path)[0] + '.png', dpi=70)
    plt.close(fig)
    print(f'{os.path.relpath(path, HERE)}  {length:.1f} s  peak {peak:.1f} s')


def collect():
    os.makedirs(TAKES, exist_ok=True)
    for source in sorted(glob.glob(os.path.join(OUTPUT, f'{PREFIX}_*.flac'))):
        name = os.path.basename(source)[len(PREFIX) + 1:].rsplit('_', 1)[0]  # <cue>_<seed>
        out = os.path.join(TAKES, name + '.ogg')
        if os.path.exists(out) and os.path.getmtime(out) >= os.path.getmtime(source):
            continue
        subprocess.run(['ffmpeg', '-v', 'error', '-y', '-i', source, '-c:a', 'libvorbis', '-q:a', '8', out],
                       check=True)
        look(out)


def mix(starts, length, picks, name):
    """Mixes every picked cue whose hit is in this cut into build/<name>.wav, the film's length; None if none."""
    score = read('score.json')
    parts = []
    for cue in score['cues']:
        take = picks.get(f'score/{cue["id"]}')
        if not take or cue['hit']['shot'] not in starts:
            continue
        path = os.path.join(HERE, take)

        def when(mark, default):
            return starts[mark['shot']] + mark.get('offset', 0.0) if mark and mark['shot'] in starts else default

        peak = cue.get('peak', {}).get(take) if isinstance(cue.get('peak'), dict) else None
        place = when(cue['hit'], 0.0) - (peak if peak is not None else peak_of(path))  # the take's start
        heard_from = max(when(cue.get('from'), 0.0), place, 0.0)
        heard_to = min(when(cue.get('to'), length), place + media_length(path), length)
        if heard_to <= heard_from:
            continue
        fade_in = (cue.get('from') or {}).get('fade', 0.0) if heard_from > place + 0.01 else 0.0
        fade_out = (cue.get('to') or {}).get('fade', 0.0)
        parts.append((path, heard_from - place, heard_from, heard_to, max(fade_in, 0.01), max(fade_out, 0.01)))
        print(f'score: {cue["id"]} ({os.path.basename(take)}) from {heard_from:.2f} s to {heard_to:.2f} s, '
              f'its peak at {when(cue["hit"], 0.0):.2f} s')
    if not parts:
        return None
    os.makedirs(BUILD, exist_ok=True)
    out = os.path.join(BUILD, name + '.wav')
    args, chains = ['ffmpeg', '-v', 'error', '-y'], []
    for i, (path, source_in, begin, end, fade_in, fade_out) in enumerate(parts):
        args += ['-i', path]
        span = end - begin
        delay = round(begin * 1000)
        chains.append(f'[{i}:a]aresample={RATE},aformat=channel_layouts=stereo,'
                      f'atrim=start={source_in:.4f}:duration={span:.4f},asetpts=PTS-STARTPTS,'
                      f'afade=t=in:d={fade_in:.3f},afade=t=out:st={span - fade_out:.4f}:d={fade_out:.3f},'
                      f'adelay={delay}|{delay}[c{i}]')
    inputs = ''.join(f'[c{i}]' for i in range(len(parts)))
    chains.append(f'{inputs}amix=inputs={len(parts)}:normalize=0,apad,atrim=duration={length:.4f}[out]')
    subprocess.run(args + ['-filter_complex', ';'.join(chains), '-map', '[out]', '-ar', str(RATE), out], check=True)
    return out


def media_length(path):
    return float(subprocess.run(['ffprobe', '-v', 'error', '-show_entries', 'format=duration', '-of', 'csv=p=0',
                                 path], capture_output=True, text=True, check=True).stdout.strip())


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--queue', nargs='*')
    parser.add_argument('--collect', action='store_true')
    parser.add_argument('--look', nargs='*')
    parser.add_argument('--profile', default='draft')
    args = parser.parse_args()
    if args.queue is not None:
        queue(args.queue)
    elif args.collect:
        collect()
    elif args.look:
        for path in args.look:
            look(path)
    else:
        import make_edit
        picks = read('picks.json')
        starts, length = make_edit.timeline(make_edit.film_cut(read('shots.json')['shots'], picks, args.profile))
        print(mix(starts, length, picks, f'score-{args.profile}'))


if __name__ == '__main__':
    main()
