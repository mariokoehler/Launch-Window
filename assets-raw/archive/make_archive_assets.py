"""Downloads the archive's pictures and sounds and writes the game's copies:
python assets-raw/archive/make_archive_assets.py [id ...] (only those; any other word: just the thumbnails)

The pictures are the ones assets-raw/archive/PAGES.md chose, with their rights checked there. Each original
is fetched once into originals/ (not committed: some are 30 MB TIFFs) from NASA's image library (through its
asset API, which lists the original file) or from NSSDC. The game's copy is assets/ui/archive/<id>.jpg:
at most 1600 px on its long edge, always RGB (libGDX loads a one-channel image as alpha, and most of these
are black and white), JPEG at quality 85. With assets/content/archive.json in place, it also writes each
page's thumbnail, assets/ui/archive/<page>-thumb.jpg (480 x 308, a centered crop of its first picture).
The sounds (SOUNDS) become mono OGG in assets/audio/archive/ through ffmpeg. Downloads go through curl.
"""
import json
import os
import subprocess
import sys

import numpy as np
from PIL import Image

Image.MAX_IMAGE_PIXELS = None
HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, "..", ".."))
ORIGINALS = os.path.join(HERE, "originals")
OUT = os.path.join(ROOT, "assets", "ui", "archive")
CONTENT = os.path.join(ROOT, "assets", "content", "archive.json")
LONG_EDGE = 1600
THUMB = (480, 308)

NSSDC = "https://nssdc.gsfc.nasa.gov/imgcat/higher_res/"
# The pictures by id: NASA's library finds its own; the rest name their NSSDC file.
PICTURES = {
    # Luna 3, Ranger 7, Lunar Orbiter, the spacewalks
    "lu3_1": NSSDC + "luna_3/lu3_1.tiff",
    "PIA02975": None, "PIA02974": None,
    "ra7_p200": NSSDC + "ranger/ra7_p200.tiff",
    "s65-30431": None, "s65-30433": None,
    "LRC-1966-B701_P-07825": None, "LRC-1967-B701_P-01194": None,
    # Apollo 8 and 11
    "as08-14-2383": None, "as08-13-2329": None, "as08-14-2453": None, "as08-17-2670": None,
    "as11-40-5903": None, "as11-40-5878": None, "as11-40-5875": None, "as11-44-6581": None, "as11-44-6642": None,
    # Mariner 4 and 9
    "PIA14033": None, "PIA14032": None, "PIA02979": None,
    "PIA02995": None, "PIA02999": None, "PIA02998": None,
    "m09_mtvs4109_09": NSSDC + "mariner_9/m09_mtvs4109_09.tiff",
    # Viking 1, Pioneer 10, Venera 13, Voyager 1
    "PIA00381": None, "PIA00382": None, "PIA00563": None,
    "ARC-1973-AC73-9253": None, "ARC-1973-AC73-9278": None, "ARC-1973-AC73-9279": None, "ARC-1972-AC72-1338": None,
    "v13_vg261_262": NSSDC + "venera/v13_vg261_262.tiff",
    "PIA00452": None, "PIA23645": None, "PIA00451": None,
}


# Crops before scaling, as fractions (left, top, right, bottom): a typed data label under the picture.
CROPS = {"ARC-1973-AC73-9278": (0.0, 0.0, 1.0, 0.80)}


def fetch(url):
    # Through curl: on Windows it checks certificates against the system's store, where Python's own
    # bundle rejected NSSDC's (2026-10-15).
    done = subprocess.run(["curl", "-sSL", "--fail", "--max-time", "300", "-A", "LaunchWindow-archive/1.0", url],
                          capture_output=True, check=True)
    return done.stdout


def library_original(nasa_id):
    """The original file's URL from NASA's image library: its asset list's ~orig file."""
    files = json.loads(fetch(f"https://images-api.nasa.gov/asset/{nasa_id}"))["collection"]["items"]
    hrefs = [f["href"] for f in files]
    for marker in ("~orig.", "~large.", "~medium."):
        for href in hrefs:
            if marker in href and href.lower().rsplit(".", 1)[-1] in ("jpg", "jpeg", "tif", "tiff", "png"):
                return href.replace("http://", "https://").replace(" ", "%20")
    raise RuntimeError(f"{nasa_id}: no image in {hrefs}")


def original(pid):
    os.makedirs(ORIGINALS, exist_ok=True)
    have = [f for f in os.listdir(ORIGINALS) if f.rsplit(".", 1)[0] == pid]
    if have:
        return os.path.join(ORIGINALS, have[0])
    url = PICTURES[pid] or library_original(pid)
    data = fetch(url)
    path = os.path.join(ORIGINALS, pid + "." + url.rsplit(".", 1)[-1].lower())
    with open(path, "wb") as f:
        f.write(data)
    print(f"  fetched {pid} ({len(data) // 1024} KB) from {url}")
    return path


def rgb(im):
    """Any mode to 8-bit RGB: 16-bit greyscale is scaled down first, so it doesn't come out white."""
    if im.mode in ("I;16", "I;16B", "I;16L", "I", "F"):
        a = np.asarray(im).astype(np.float64)
        # 16-bit, or 12 bits stored in 16 (scanners do both): scale the range the data uses to 8 bits.
        full = 65535.0 if a.max() > 4095 else 4095.0 if a.max() > 255 else 255.0
        im = Image.fromarray(np.clip(a * 255.0 / full, 0, 255).astype(np.uint8), "L")
    return im.convert("RGB")


def game_copy(pid):
    im = rgb(Image.open(original(pid)))
    if pid in CROPS:
        l, t, r, b = CROPS[pid]
        im = im.crop((round(l * im.width), round(t * im.height), round(r * im.width), round(b * im.height)))
    scale = min(1.0, LONG_EDGE / max(im.size))
    if scale < 1:
        im = im.resize((round(im.width * scale), round(im.height * scale)), Image.LANCZOS)
    os.makedirs(OUT, exist_ok=True)
    out = os.path.join(OUT, pid.lower() + ".jpg")
    im.save(out, "JPEG", quality=85)
    return out, im.size


def thumbnails():
    if not os.path.exists(CONTENT):
        return
    for page in json.load(open(CONTENT, encoding="utf-8"))["pages"]:
        im = Image.open(os.path.join(ROOT, "assets", "ui", page["images"][0]["file"])).convert("RGB")
        k = max(THUMB[0] / im.width, THUMB[1] / im.height)
        im = im.resize((round(im.width * k), round(im.height * k)), Image.LANCZOS)
        left, top = (im.width - THUMB[0]) // 2, (im.height - THUMB[1]) // 2
        im.crop((left, top, left + THUMB[0], top + THUMB[1])).save(
            os.path.join(OUT, page["id"] + "-thumb.jpg"), "JPEG", quality=85)
        print("  thumb", page["id"])


# The sounds: the game's name, the original's URL and file name. NASA's recordings, from NSSDC.
SOUNDS = {
    "apollo-8-genesis": ("https://nssdc.gsfc.nasa.gov/planetary/image/as8_genesis1a.mov", "as8_genesis1a.mov"),
    "apollo-11-eagle-has-landed": ("https://nssdc.gsfc.nasa.gov/planetary/lunar/a11landing.aiff", "a11landing.aiff"),
    # Carl Sagan reading his Pale Blue Dot passage: Library of Congress, cosmos000110 (design.md 1.1, the
    # user's decision of 2026-10-13: not public domain, credited as it is, out at once if the estate objects).
    "pale-blue-dot-sagan": ("https://tile.loc.gov/storage-services/media/ls/sagan/1958124-3-1.mp3",
                            "sagan-cosmos000110.mp3"),
}
SOUND_OUT = os.path.join(ROOT, "assets", "audio", "archive")


def sound(name):
    """The game's copy: mono OGG Vorbis (the video's picture dropped), through ffmpeg."""
    url, file = SOUNDS[name]
    src = os.path.join(ORIGINALS, file)
    if not os.path.exists(src):
        os.makedirs(ORIGINALS, exist_ok=True)
        with open(src, "wb") as f:
            f.write(fetch(url))
    os.makedirs(SOUND_OUT, exist_ok=True)
    out = os.path.join(SOUND_OUT, name + ".ogg")
    subprocess.run(["ffmpeg", "-v", "error", "-y", "-i", src, "-vn", "-ac", "1", "-ar", "44100",
                    "-c:a", "libvorbis", "-q:a", "4", out], check=True)
    print(f"{name}: {os.path.getsize(out) // 1024} KB")


def main():
    only = sys.argv[1:]
    for pid in PICTURES:
        if only and pid not in only:
            continue
        out, size = game_copy(pid)
        print(f"{pid}: {size[0]} x {size[1]}, {os.path.getsize(out) // 1024} KB")
    for name in SOUNDS:
        if not only or name in only:
            sound(name)
    thumbnails()


if __name__ == "__main__":
    main()
