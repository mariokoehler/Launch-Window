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
import sys

import make_score

HERE = os.path.dirname(os.path.abspath(__file__))
BUILD = os.path.join(HERE, 'build')
FILMCRAFT = r'C:\Program Files\FilmCraft\filmcraft-cli.exe'
TICKS = 254016000000  # FilmCraft's ticks per second
FPS = 24
CAPTION = {'font': 'Work Sans', 'size': 44, 'position': [110, 990], 'align': 'left', 'fade': 0.4}
SUBTITLE = {'font': 'Work Sans', 'size': 46, 'position': [960, 930], 'align': 'center', 'fade': 0.25,
            'shadow': True}
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


def media_length(path):
    return float(subprocess.run(['ffprobe', '-v', 'error', '-show_entries', 'format=duration', '-of', 'csv=p=0',
                                 path], capture_output=True, text=True, check=True).stdout.strip())


def overlay(project, begin, text, style):
    """A caption or subtitle: a text graphic on its own clip, faded in and out with opacity keyframes."""
    made = filmcraft(project, [{'id': 'graphics.newText', 'params': {
        'text': text['text'], 'position': style['position'], 'size': style['size'], 'font': style['font'],
        'seconds': text['for'], 'time': ticks(begin), 'newClip': True}}])[0]
    props = {'align': style['align'], 'position': style['position']}
    if style.get('shadow'):
        props['shadow'] = True
    fade, end = style['fade'], begin + text['for']
    filmcraft(project, [{'id': 'graphics.set', 'params': {
        'clip': made['clip'], 'layer': made['layer'], 'props': props}}] + [
        {'id': 'graphics.set', 'params': {
            'clip': made['clip'], 'layer': made['layer'], 'props': {'opacity': value},
            'time': ticks(when), 'keyframe': True}}
        for when, value in ((begin, 0), (begin + fade, 100), (end - fade, 100), (end - 0.01, 0))])


def placeholder_beep(path):
    """Two short beeps a second, a stand-in until the real recording of Sputnik's signal is in."""
    subprocess.run(['ffmpeg', '-loglevel', 'error', '-y', '-f', 'lavfi', '-i',
                    'aevalsrc=0.4*sin(2*PI*900*t)*lt(mod(t\\,0.5)\\,0.18):s=48000:d=10', path], check=True)


def held(shot, path):
    """A shot's "hold": its clip with the last frame held that many seconds longer (and silence under it), for a
    static shot that ends too soon after it settles (a calendar on its date). Made once in build/."""
    out = os.path.join(BUILD, f'hold-{shot["id"]}-{os.path.basename(path)}')
    if not os.path.exists(out) or os.path.getmtime(out) < os.path.getmtime(path):
        subprocess.run(['ffmpeg', '-loglevel', 'error', '-y', '-i', path, '-vf',
                        f'tpad=stop_mode=clone:stop_duration={shot["hold"]}', '-af', f'apad=pad_dur={shot["hold"]}',
                        '-c:v', 'libx264', '-crf', '12', '-preset', 'slow', '-c:a', 'aac', '-b:a', '256k', out],
                       check=True)
    return out


def stretched(shot, path):
    """A shot's "stretch": its clip slowed by that factor (a near-still shot given room for its line), the frames
    repeated at the film's rate and its sound slowed alike. Made once in build/; "trim" is in the slowed time."""
    out = os.path.join(BUILD, f'stretch{shot["stretch"]}-{shot["id"]}-{os.path.basename(path)}')
    if not os.path.exists(out) or os.path.getmtime(out) < os.path.getmtime(path):
        subprocess.run(['ffmpeg', '-loglevel', 'error', '-y', '-i', path, '-vf',
                        f'setpts=PTS*{shot["stretch"]},fps={FPS}', '-af', f'atempo={1 / shot["stretch"]:.5f}',
                        '-c:v', 'libx264', '-crf', '12', '-preset', 'slow', '-c:a', 'aac', '-b:a', '256k', out],
                       check=True)
    return out


def prepared(shot, path):
    """The clip as the edit uses it: held at its end or slowed, if the shot asks."""
    if shot.get('hold'):
        return held(shot, path)
    if shot.get('stretch'):
        return stretched(shot, path)
    return path


def still(shot):
    """A shot's "still": a photograph (a path under this folder) as a clip of its duration, cropped to 16:9 by
    "crop" ([x, y, w, h], fractions of the image) and pushed in slowly, with silence under it. Made once in build/."""
    source = os.path.join(HERE, shot['still'])
    out = os.path.join(BUILD, f'still-{shot["id"]}.mp4')
    if not os.path.exists(out) or os.path.getmtime(out) < os.path.getmtime(source):
        x, y, w, h = shot.get('crop', [0, 0, 1, 1])
        frames = round(shot['duration'] * FPS)
        push = shot.get('push', 0.04)  # how far it zooms in over the shot
        video = (f'crop=iw*{w}:ih*{h}:iw*{x}:ih*{y},scale=3840:2160,'
                 f"zoompan=z='1+{push}*on/{frames}':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':d=1:s=1920x1080:fps={FPS}")
        subprocess.run(['ffmpeg', '-loglevel', 'error', '-y', '-loop', '1', '-i', source, '-f', 'lavfi', '-i',
                        'anullsrc=r=48000:cl=stereo', '-vf', video, '-frames:v', str(frames), '-shortest',
                        '-c:v', 'libx264', '-crf', '12', '-preset', 'slow', '-pix_fmt', 'yuv420p', '-c:a', 'aac',
                        out], check=True)
    return out


def take_for(shot, picks, profile):
    """The final take where one is picked (in a final cut), else the draft; a still or built shot is always in."""
    if shot.get('still'):
        return still(shot)
    if shot.get('built'):  # made by its own script (the end card), rebuilt when missing
        if not os.path.exists(os.path.join(HERE, shot['built'])):
            subprocess.run([sys.executable, os.path.join(HERE, shot['builder'])], check=True)
        return shot['built']
    for each in ([profile, 'draft'] if profile != 'draft' else ['draft']):
        if f'{shot["id"]}/{each}' in picks:
            return picks[f'{shot["id"]}/{each}']
    return None


def film_cut(shots, picks, profile, beats=None):
    """The shots in the cut, in film order: those with a take, of the given beats only if any are given."""
    return [s for s in shots if take_for(s, picks, profile) and (not beats or s['beat'] in beats)]


def timeline(cut):
    """Each shot's start on the timeline (its blackBefore included), and the film's length."""
    starts, at = {}, 0.0
    for shot in cut:
        start, end = shot.get('trim', [0.0, shot['duration']])
        at += shot.get('blackBefore', 0.0)  # a pause in black, faded into and out of (the breather after 1967)
        starts[shot['id']] = at
        at += end - start
    return starts, at


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--profile', default='draft')
    parser.add_argument('--frames', nargs='*', type=float, default=[])
    parser.add_argument('--beat', nargs='*', help='only these beats')
    parser.add_argument('--no-score', action='store_true', help='leave the score out')
    args = parser.parse_args()

    os.makedirs(BUILD, exist_ok=True)
    shots = read('shots.json')['shots']
    audio = read('audio.json')['tracks']
    picks = read('picks.json')
    project = os.path.join(BUILD, f'trailer-{args.profile}.fcproj')
    if os.path.exists(project):
        os.remove(project)

    def take(shot):
        return take_for(shot, picks, args.profile)

    cut = film_cut(shots, picks, args.profile, args.beat)
    if not cut:
        raise SystemExit(f'no picked {args.profile} clips')
    ids = {s['id'] for s in cut}
    audio = [t for t in audio if 'under' not in t or t['under'] in ids]
    starts, length = timeline(cut)
    score = make_score.mix(starts, length, picks, f'score-{args.profile}')
    if score and not args.no_score:
        audio.append({'id': 'score', 'file': os.path.relpath(score, HERE), 'at': 0.0, 'track': 'A4',
                      'gainDb': read('score.json').get('gainDb', 0.0)})
    for track in audio:
        path = os.path.join(HERE, track['file'])
        if os.path.basename(path).startswith('placeholder-beep') and not os.path.exists(path):
            placeholder_beep(path)

    # 1. The sequence and the media.
    media = [prepared(s, os.path.join(HERE, take(s))) for s in cut]
    media += [os.path.join(HERE, t['file']) for t in audio]
    sequence, imported = filmcraft(project, [
        {'id': 'file.newSequence', 'params': {'name': f'Launch Window trailer ({args.profile})',
                                              'width': 1920, 'height': 1080, 'fps': FPS, 'audio': 4}},
        {'id': 'file.import', 'params': {'paths': [m.replace(os.sep, '/') for m in media]}},
    ])
    if imported['errors']:
        raise RuntimeError(imported['errors'])
    items = imported['items']

    # 2. The shots back to back on V1 (their sound on A1); audio.json on its tracks (A2 effects, A3
    #    narration), each for its file's own length; the score on A4.
    places = []
    for shot, item in zip(cut, items):
        start, end = shot.get('trim', [0.0, shot['duration']])
        places.append({'id': 'timeline.place', 'params': {
            'item': item, 'track': 'V1', 'audioTrack': 'A1', 'seconds': starts[shot['id']],
            'sourceIn': {'seconds': start}, 'duration': {'seconds': end - start}}})
    for track, item in zip(audio, items[len(cut):]):
        when = starts[track['under']] + track.get('offset', 0.0) if 'under' in track else track['at']
        span = min(track.get('for', 1e9), media_length(os.path.join(HERE, track['file'])), length - when)
        places.append({'id': 'timeline.place', 'params': {
            'item': item, 'track': track.get('track', 'A2'), 'seconds': when, 'video': False,
            'sourceIn': {'seconds': 0}, 'duration': {'seconds': span}}})
    placed = filmcraft(project, places)
    video_clips = [p['clips'][0] for p in placed[:len(cut)]]
    sound_clips = [p['clips'][1] if len(p['clips']) > 1 else None for p in placed[:len(cut)]]
    audio_clips = [p['clips'][0] for p in placed[len(cut):]]

    # 3. Frame filling, the fades from and to black, the gains, the captions and subtitles.
    effects = [{'id': 'clip.fillFrame', 'params': {'clips': video_clips}},
               {'id': 'sequence.applyVideoTransition', 'params': {
                   'clip': video_clips[0], 'effect': 'cross_dissolve', 'frames': FADE_FRAMES, 'edge': 'in',
                   'align': 'start'}},
               {'id': 'sequence.applyVideoTransition', 'params': {
                   'clip': video_clips[-1], 'effect': 'cross_dissolve', 'frames': FADE_FRAMES, 'edge': 'out',
                   'align': 'end'}}]
    for i, shot in enumerate(cut):
        if shot.get('blackBefore') and i > 0:
            # "fadeBefore": how slowly the shot before goes to black, in seconds (a beat's end given room).
            # "fadeIn": how this shot comes out of the black, in seconds (0: a cut).
            fade_out = round(shot['fadeBefore'] * FPS) if 'fadeBefore' in shot else FADE_FRAMES
            fade_in = round(shot['fadeIn'] * FPS) if 'fadeIn' in shot else FADE_FRAMES * 2
            effects += [{'id': 'sequence.applyVideoTransition', 'params': {
                            'clip': video_clips[i - 1], 'effect': 'cross_dissolve', 'frames': fade_out,
                            'edge': 'out', 'align': 'end'}}]
            if fade_in:
                effects += [{'id': 'sequence.applyVideoTransition', 'params': {
                                'clip': video_clips[i], 'effect': 'cross_dissolve', 'frames': fade_in,
                                'edge': 'in', 'align': 'start'}}]
    filmcraft(project, effects)
    gains = [{'id': 'effects.setParam', 'params': {'clip': clip, 'effect': 'volume', 'param': 'level',
                                                   'value': track['gainDb']}}
             for track, clip in zip(audio, audio_clips) if 'gainDb' in track]
    gains += [{'id': 'effects.setParam', 'params': {'clip': clip, 'effect': 'volume', 'param': 'level',
                                                    'value': shot['clipGainDb']}}
              for shot, clip in zip(cut, sound_clips) if clip and 'clipGainDb' in shot]
    if gains:
        filmcraft(project, gains)

    for shot in cut:
        for kind, style in (('caption', CAPTION), ('subtitle', SUBTITLE)):
            text = shot.get(kind)
            if text:
                overlay(project, starts[shot['id']] + text['at'], text, style)

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
