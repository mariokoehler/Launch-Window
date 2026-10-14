"""Generates the Quindar tones: python assets-raw/audio/quindar/make_quindar.py

Apollo's ground stations keyed the transmitter with two pure tones of 250 ms: 2525 Hz when CAPCOM
pressed to talk (the intro, the beep before the voice) and 2475 Hz when he let go (the outro, the beep
after it). Writes, beside this script, 48 kHz 16-bit mono WAVs and OGG copies (ffmpeg):

- quindar_intro.wav / quindar_outro.wav: the clean tones, -6 dBFS.
- quindar_intro_radio.wav / quindar_outro_radio.wav: as heard on the air-to-ground loop, band-limited to
  300-3000 Hz, slightly saturated, with a faint hiss under them (a little longer, so the hiss can fade).

The tones are made from scratch (a sine and its envelope): no recording is used, so there's nothing to
credit. A 4 ms raised-cosine fade at each end keeps the edges crisp without a click.
"""
import os
import shutil
import subprocess

import numpy as np
from scipy.io import wavfile
from scipy.signal import butter, sosfilt

HERE = os.path.dirname(os.path.abspath(__file__))
RATE = 48000
LENGTH = 0.250
TONES = {"intro": 2525.0, "outro": 2475.0}
PEAK = 10 ** (-6 / 20)


def envelope(n, ramp):
    env = np.ones(n)
    r = int(ramp * RATE)
    shape = 0.5 - 0.5 * np.cos(np.linspace(0, np.pi, r))
    env[:r] = shape
    env[-r:] = shape[::-1]
    return env


def tone(freq):
    n = int(LENGTH * RATE)
    t = np.arange(n) / RATE
    return np.sin(2 * np.pi * freq * t) * envelope(n, 0.004)


def radio(clean):
    # 60 ms of the loop's hiss either side, the tone band-limited and pushed into the line's soft limit.
    pad = int(0.060 * RATE)
    rng = np.random.default_rng(7)
    sig = np.concatenate([np.zeros(pad), clean, np.zeros(pad)])
    hiss = rng.normal(0, 1, sig.size)
    band = butter(4, [300, 3000], btype="bandpass", fs=RATE, output="sos")
    hiss = sosfilt(band, hiss)
    hiss = hiss / np.max(np.abs(hiss)) * 0.035 * envelope(sig.size, 0.020)
    line = np.tanh(1.8 * sosfilt(band, sig)) / np.tanh(1.8)
    return line + hiss


def write(name, sig):
    sig = sig / np.max(np.abs(sig)) * PEAK
    wav = os.path.join(HERE, name + ".wav")
    wavfile.write(wav, RATE, (sig * 32767).astype(np.int16))
    if shutil.which("ffmpeg"):
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-c:a", "libvorbis", "-q:a", "6",
                        os.path.join(HERE, name + ".ogg")], check=True)
    print(wav, f"{sig.size / RATE * 1000:.0f} ms")


def main():
    for which, freq in TONES.items():
        clean = tone(freq)
        write(f"quindar_{which}", clean)
        write(f"quindar_{which}_radio", radio(clean))


if __name__ == "__main__":
    main()
