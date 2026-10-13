"""Placeholder sounds for the replay (ui-spec.md 4.7.3, "Sound"), synthesized until real ones exist.

Writes them under assets/audio/replay/ and lists them in assets/content/replay-sounds.json, replacing
each placeholder sound's takes. A sound whose takes are already real (not a placeholder file) is left
alone. Run from the repository's root:

    python assets-raw/replay/sounds/make_placeholder_sounds.py

Needs numpy and scipy, and ffmpeg with libvorbis on the path (loops are OGG: MP3 can't loop seamlessly).
"""
import json
import os
import subprocess

import numpy as np
from scipy.io import wavfile

RATE = 44100
FOLDER = os.path.join('assets', 'audio', 'replay')
CATALOG = os.path.join('assets', 'content', 'replay-sounds.json')
PREFIX = 'placeholder-'


def t(seconds):
    return np.arange(int(RATE * seconds)) / RATE


def envelope(n, attack=0.005, release=0.02):
    e = np.ones(n)
    a = int(RATE * attack)
    r = int(RATE * release)
    e[:a] = np.linspace(0, 1, a)
    e[-r:] = np.linspace(1, 0, r)
    return e


def save(name, signal, ogg=False):
    """Writes a placeholder take and returns its path under assets/."""
    signal = np.clip(signal, -1, 1)
    wav = os.path.join(FOLDER, PREFIX + name + '.wav')
    wavfile.write(wav, RATE, (signal * 32767).astype(np.int16))
    if not ogg:
        return 'audio/replay/' + PREFIX + name + '.wav'
    subprocess.run(['ffmpeg', '-loglevel', 'error', '-y', '-i', wav, '-c:a', 'libvorbis', '-q:a', '5',
                    wav[:-4] + '.ogg'], check=True)
    os.remove(wav)
    return 'audio/replay/' + PREFIX + name + '.ogg'


def main():
    os.makedirs(FOLDER, exist_ok=True)
    with open(CATALOG, encoding='utf-8') as f:
        catalog = json.load(f)
    real = {sound['id'] for sound in catalog['sounds']
            if any(not os.path.basename(file).startswith(PREFIX) for file in sound['files'])}
    rng = np.random.default_rng(7)
    takes = {}
    # Soft sine blips, three pitches.
    takes['telemetry-blip'] = [
        save('telemetry-blip-%d' % (i + 1), 0.5 * np.sin(2 * np.pi * f * t(0.09)) * envelope(len(t(0.09)), 0.003, 0.05))
        for i, f in enumerate([1800, 1950, 2100])]
    # A warble: 880 and 1100 Hz alternating every 1/16 s.
    x = t(0.24)
    warble = np.where((x * 16).astype(int) % 2 == 0, 880, 1100)
    takes['caution-tone'] = [save('caution-tone', 0.5 * np.sin(2 * np.pi * np.cumsum(warble) / RATE)
                                  * envelope(len(x)))]
    # A square-wave klaxon, two pulses in 0.6 s, whole periods so the loop is seamless.
    x = t(0.6)
    takes['master-alarm'] = [save('master-alarm', np.sign(np.sin(2 * np.pi * 440 * x)) * 0.3 * ((x % 0.3) < 0.18),
                                  ogg=True)]
    # White noise.
    takes['no-signal-static'] = [save('no-signal-static', rng.normal(0, 0.15, len(t(4.0))), ogg=True)]
    # A decaying noise burst.
    x = t(0.04)
    takes['hold-click'] = [save('hold-click', rng.normal(0, 0.6, len(x)) * np.exp(-x * 150))]
    # A slow open-fifth drone, faded in and out.
    x = t(30.0)
    drone = 0.15 * (np.sin(2 * np.pi * 110 * x) + np.sin(2 * np.pi * 164.8 * x)
                    + np.sin(2 * np.pi * 220 * x * (1 + 0.002 * np.sin(x))))
    takes['music'] = [save('music', drone * envelope(len(x), 2.0, 2.0), ogg=True)]
    # Quiet noise with a mains hum, whole hum periods so the loop is seamless.
    x = t(10.0)
    takes['room-tone'] = [save('room-tone', rng.normal(0, 0.03, len(x)) + 0.03 * np.sin(2 * np.pi * 60 * x),
                               ogg=True)]

    # A sound with real takes keeps them, and its placeholder files go.
    for sound_id in real & takes.keys():
        for file in takes.pop(sound_id):
            os.remove(os.path.join('assets', file))
    for sound in catalog['sounds']:
        if sound['id'] in takes:
            sound['files'] = takes[sound['id']]
    with open(CATALOG, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(catalog, f, indent=2, ensure_ascii=False)
        f.write('\n')
    print('written:', sum(len(v) for v in takes.values()), 'takes')


if __name__ == '__main__':
    main()
