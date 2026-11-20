"""Publishes the juice's sounds: python assets-raw/audio/make_juice_sounds.py

The user's Pixabay finds (2026-11-20) are in assets-raw/audio/, each with a .txt beside it naming its page and
uploader, for the small effects of design.md section 5, Backlog (J2 and J3). This raises each one's level by its
gain, trims and fades it where a long tail would outstay its moment, and writes it under assets/audio/ui/ as
<id>.ogg at 44.1 kHz. Credit each file in assets/CREDITS.md.

Needs ffmpeg with libvorbis on the path.
"""
import os
import subprocess

REPO = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
RAW = os.path.join(REPO, "assets-raw", "audio")
OUT = os.path.join(REPO, "assets", "audio", "ui")

# id: (source in assets-raw/audio, gain in dB, length in seconds or None for all of it, fade-out seconds).
# Gains bring each peak to about -2 dBFS; the fanfare stays a little lower, being music under a dialog.
SOUNDS = {
    # J3: a turn resolves while the player is online.
    "new-turn": ("new_turn_chime", 2.5, None, 0),
    # J3: a First's front page slides up and lands (its peak at about 0.8 s); the long tail trimmed.
    "paper-lands": ("newspaper_thump", 0, 2.5, 1.0),
    # J2: a goal's patch stamps onto its card.
    "stamp": ("stamp", 0, None, 0),
    # J3: the race won. 19 s in all; the dialog fades it out when it closes.
    "race-won": ("game_won_fanfare", 6, None, 0),
}


def main():
    os.makedirs(OUT, exist_ok=True)
    for sid, (source, gain, length, fade) in SOUNDS.items():
        filters = [f"volume={gain}dB"]
        args = []
        if length is not None:
            args = ["-t", str(length)]
            if fade:
                filters.append(f"afade=t=out:st={length - fade}:d={fade}")
        subprocess.run(["ffmpeg", "-v", "error", "-y", "-i", os.path.join(RAW, source + ".ogg"), *args,
                        "-af", ",".join(filters), "-ar", "44100", "-c:a", "libvorbis", "-q:a", "6",
                        os.path.join(OUT, sid + ".ogg")], check=True)
    print(len(SOUNDS), "sounds written to", OUT)


if __name__ == "__main__":
    main()
