"""Publishes the hands-on puzzle's sounds: python assets-raw/audio/make_handson_sounds.py

The user's Pixabay finds (2026-10-25) are in assets-raw/audio/, each with a .txt beside it naming its page and
uploader. This raises each one's level by its gain and writes it under assets/audio/handson/ as <id>.ogg (OGG:
the loop must be seamless), at 44.1 kHz. The screen (client.screen.HandsOnScreen) names the files itself.
Credit each file in assets/CREDITS.md.

Needs ffmpeg with libvorbis on the path.
"""
import os
import subprocess

REPO = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
RAW = os.path.join(REPO, "assets-raw", "audio")
OUT = os.path.join(REPO, "assets", "audio", "handson")

# id: (source in assets-raw/audio, gain in dB). Gains bring each peak to about -2 dBFS.
SOUNDS = {
    # Under a round while its commands resolve, looping.
    "tense-loop": ("tense_loop", 6),
    # On a command's result as its needle settles.
    "failure": ("failure", 0),
    "success": ("success", 10),
    "exceptional": ("exceptional", -1),
}


def main():
    os.makedirs(OUT, exist_ok=True)
    for sid, (source, gain) in SOUNDS.items():
        subprocess.run(["ffmpeg", "-v", "error", "-y", "-i", os.path.join(RAW, source + ".ogg"),
                        "-af", f"volume={gain}dB", "-ar", "44100", "-c:a", "libvorbis", "-q:a", "6",
                        os.path.join(OUT, sid + ".ogg")], check=True)
    print(len(SOUNDS), "sounds written to", OUT)


if __name__ == "__main__":
    main()
