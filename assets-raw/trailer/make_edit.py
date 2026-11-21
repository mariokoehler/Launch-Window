"""Cuts the trailer in FilmCraft from the script and the picks (PLAN.md §4), headless.

Every shot with a picked clip for the profile (picks.json "<shot>/<profile>") goes on V1 in film order, trimmed
to its "trim" [in, out] seconds and filled to the 1920x1080 frame, its own H3 sound on A1. A shot's caption is a
text graphic over it, faded in and out. audio.json's tracks go on A2. The film fades in from black and out to
black.

FilmCraft's CLI answers each command with the ids the next ones need (imported items, placed clips), so the edit
is built in a few runs: import, place, then the effects on the placed clips.

Usage:
    python make_edit.py                    # draft clips
    python make_edit.py --profile final
    python make_edit.py --frames 1.0 7.5   # also render single frames to build/ for a look

Writes build/trailer-<profile>.fcproj and build/trailer-<profile>.mp4.
"""
import argparse
import json
import os
import subprocess

HERE = os.path.dirname(os.path.abspath(__file__))
BUILD = os.path.join(HERE, 'build')
FILMCRAFT = r'C:\Program Files\FilmCraft\filmcraft-cli.exe'
TICKS = 254016000000  # FilmCraft's ticks per second
FPS = 24
CAPTION = {'font': 'Work Sans', 'size': 44, 'position': [110, 990], 'fade': 0.4}
FADE_FRAMES = 12


def read(name):
    with open(os.path.join(HERE, name), encoding='utf-8') as source:
        return json.load(source)


def ticks(seconds):
    return round(seconds * TICKS)


def filmcraft(project, commands, save=True):
    """Runs commands (dicts of id and params) on the project; returns each command's result."""
    script = os.path.join(BUILD, 'step.jsonl')
    with open(script, 'w', encoding='utf-8') as out:
        for command in commands:
            out.write(json.dumps(command) + '\n')
    args = [FILMCRAFT, '--compact']
    args += ['--project', project, '--save'] if os.path.exists(project) else ['--save-as', project]
    done = subprocess.run(args + ['run', script], capture_output=True, text=True, encoding='utf-8')
    results = []
    for line in done.stdout.splitlines():
        line = line.strip()
        if not line.startswith('{'):
            continue
        result = json.loads(line)
        if not result.get('ok'):
            raise RuntimeError(f'FilmCraft: {line}\n{done.stderr}')
        results.append(result.get('result'))
    if done.returncode != 0 or len(results) != len(commands):
        raise RuntimeError(f'FilmCraft exit {done.returncode}: {done.stdout}\n{done.stderr}')
    return results


def placeholder_beep(path):
    """Two short beeps a second, a stand-in until the real recording of Sputnik's signal is in."""
    subprocess.run(['ffmpeg', '-loglevel', 'error', '-y', '-f', 'lavfi', '-i',
                    'aevalsrc=0.4*sin(2*PI*900*t)*lt(mod(t\\,0.5)\\,0.18):s=48000:d=10', path], check=True)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--profile', default='draft')
    parser.add_argument('--frames', nargs='*', type=float, default=[])
    args = parser.parse_args()

    os.makedirs(BUILD, exist_ok=True)
    shots = read('shots.json')['shots']
    audio = read('audio.json')['tracks']
    picks = read('picks.json')
    project = os.path.join(BUILD, f'trailer-{args.profile}.fcproj')
    if os.path.exists(project):
        os.remove(project)

    cut = [s for s in shots if f'{s["id"]}/{args.profile}' in picks]
    if not cut:
        raise SystemExit(f'no picked {args.profile} clips')
    for track in audio:
        path = os.path.join(HERE, track['file'])
        if os.path.basename(path).startswith('placeholder-beep') and not os.path.exists(path):
            placeholder_beep(path)

    # 1. The sequence and the media.
    media = [os.path.join(HERE, picks[f'{s["id"]}/{args.profile}']) for s in cut]
    media += [os.path.join(HERE, t['file']) for t in audio]
    sequence, imported = filmcraft(project, [
        {'id': 'file.newSequence', 'params': {'name': f'Launch Window trailer ({args.profile})',
                                              'width': 1920, 'height': 1080, 'fps': FPS}},
        {'id': 'file.import', 'params': {'paths': [m.replace('\\', '/') for m in media]}},
    ])
    if imported['errors']:
        raise RuntimeError(imported['errors'])
    items = imported['items']

    # 2. The shots back to back on V1 (their sound on A1), audio.json on A2.
    starts, at, places = {}, 0.0, []
    for shot, item in zip(cut, items):
        start, end = shot.get('trim', [0.0, shot['duration']])
        starts[shot['id']] = at
        places.append({'id': 'timeline.place', 'params': {
            'item': item, 'track': 'V1', 'audioTrack': 'A1', 'seconds': at,
            'sourceIn': {'seconds': start}, 'duration': {'seconds': end - start}}})
        at += end - start
    length = at
    for track, item in zip(audio, items[len(cut):]):
        when = starts[track['under']] + track.get('offset', 0.0) if 'under' in track else track['at']
        if 'under' in track:
            start, end = next(s for s in cut if s['id'] == track['under']).get('trim', [0.0, next(s for s in cut if s['id'] == track['under'])['duration']])
            span = end - start
        else:
            span = track['for']
        places.append({'id': 'timeline.place', 'params': {
            'item': item, 'track': 'A2', 'seconds': when, 'video': False,
            'sourceIn': {'seconds': 0}, 'duration': {'seconds': span - track.get('offset', 0.0)}}})
    placed = filmcraft(project, places)
    video_clips = [p['clips'][0] for p in placed[:len(cut)]]
    audio_clips = [p['clips'][0] for p in placed[len(cut):]]

    # 3. Frame filling, the fades from and to black, the gains, the captions.
    effects = [{'id': 'clip.fillFrame', 'params': {'clips': video_clips}},
               {'id': 'sequence.applyVideoTransition', 'params': {
                   'clip': video_clips[0], 'effect': 'cross_dissolve', 'frames': FADE_FRAMES, 'edge': 'in', 'align': 'start'}},
               {'id': 'sequence.applyVideoTransition', 'params': {
                   'clip': video_clips[-1], 'effect': 'cross_dissolve', 'frames': FADE_FRAMES, 'edge': 'out', 'align': 'end'}}]
    filmcraft(project, effects)
    gains = [{'id': 'effects.setParam', 'params': {'clip': clip, 'effect': 'volume', 'param': 'level',
                                                   'value': track['gainDb']}}
             for track, clip in zip(audio, audio_clips) if 'gainDb' in track]
    if gains:
        filmcraft(project, gains)

    for shot in cut:
        caption = shot.get('caption')
        if not caption:
            continue
        begin = starts[shot['id']] + caption['at']
        made = filmcraft(project, [{'id': 'graphics.newText', 'params': {
            'text': caption['text'], 'position': CAPTION['position'], 'size': CAPTION['size'],
            'font': CAPTION['font'], 'seconds': caption['for'], 'time': ticks(begin), 'newClip': True}}])[0]
        fade, end = CAPTION['fade'], begin + caption['for']
        filmcraft(project, [{'id': 'graphics.set', 'params': {
            'clip': made['clip'], 'layer': made['layer'], 'props': {'opacity': value},
            'time': ticks(when), 'keyframe': True}}
            for when, value in ((begin, 0), (begin + fade, 100), (end - fade, 100), (end - 0.01, 0))])

    # 4. Export, and any frames asked for.
    movie = os.path.join(BUILD, f'trailer-{args.profile}.mp4')
    exported = filmcraft(project, [{'id': 'file.exportMedia', 'params': {'path': movie.replace('\\', '/'),
                                                                         'format': 'h264'}}])[0]
    for seconds in args.frames:
        subprocess.run([FILMCRAFT, '--project', project, 'render', '--seconds', str(seconds), '--out',
                        os.path.join(BUILD, f'frame-{args.profile}-{seconds:05.2f}.png')], check=True,
                       capture_output=True)
    print(f'{movie}  {length:.1f} s, {len(cut)} shots, exported in {exported["result"]["seconds"]:.1f} s')


if __name__ == '__main__':
    main()
