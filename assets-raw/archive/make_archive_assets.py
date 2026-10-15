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
    # Added 2026-10-15: Surveyor 1, Apollo 15, Mariner 10, Pioneer 11, Luna 9, Lunokhod 1, Luna 16, Venera 15/16.
    "surveyor_1_4_foot_pad": "https://www.nasa.gov/wp-content/uploads/2021/05/surveyor_1_4_foot_pad.jpg",
    "surveyor_1_7_mosaic_image": "https://www.nasa.gov/wp-content/uploads/2021/05/surveyor_1_7_mosaic_image.jpg",
    "PIA02976": None, "s66-32144": None,
    "as15-86-11603": None, "as15-85-11425": None, "as15-88-11866": None, "S71-42955": None, "S71-43788": None,
    "PIA03103": None, "PIA23791": None, "PIA02961": None, "PIA02962": None,
    "p11-saturn-titan": "https://assets.science.nasa.gov/content/dam/science/psd/solar/internal_resources/730/"
                        "Saturn_and_its_rings.jpeg",
    "p11-jupiter-north": "https://assets.science.nasa.gov/content/dam/science/psd/solar/internal_resources/2551/"
                         "Fuzzy_color_image_of_Jupiter.jpeg",
    "ARC-1979-AC79-9114-57": None,
    "luna9close_big": "https://nssdc.gsfc.nasa.gov/planetary/lunar/luna9close_big.gif",
    # An LRO frame from NASA's PDS archive (public domain), 9 November 2011: cut to the Luna 17 stage and
    # Lunokhod's tracks (PIXEL_CROPS), mirrored so east is right (MIRRORED), as PIA12967 shows it.
    "M175502049RC": "https://pds.lroc.im-ldi.com/data/LRO-L-LROC-3-CDR-V1.0/LROLRC_1009/EXTRAS/BROWSE/2011313/"
                    "M175502049RC_pyr.tif",
    "lunokhod": "https://nssdc.gsfc.nasa.gov/image/spacecraft/lunokhod.jpg",
    "PIA12984": None,
    "luna16": "https://nssdc.gsfc.nasa.gov/image/spacecraft/luna16.jpg",
    "venera-maxwell": "https://pds-geosciences.wustl.edu/venera/GIF/venera-maxwell.gif",
    "PIA00465": "https://assets.science.nasa.gov/content/dam/science/psd/photojournal/pia/pia00/pia00465/PIA00465.jpg",
}


# Crops before scaling, as fractions (left, top, right, bottom): a typed data label under the picture.
CROPS = {"ARC-1973-AC73-9278": (0.0, 0.0, 1.0, 0.80)}
# Crops in pixels of the original (left, top, right, bottom), and pictures to mirror left to right.
PIXEL_CROPS = {"M175502049RC": (930, 25540, 1430, 25940)}
MIRRORED = {"M175502049RC"}


def fetch(url):
    # Through curl: on Windows it checks certificates against the system's store, where Python's own
    # bundle rejected NSSDC's (2026-10-15).
    for curl in ("curl", r"C:\Windows\System32\curl.exe"):
        done = subprocess.run([curl, "-sSL", "--fail", "--max-time", "600", "-A", "LaunchWindow-archive/1.0", url],
                              capture_output=True)
        if done.returncode == 0:
            return done.stdout
    # Windows' own curl checks against the system's store, where another bundle failed (pds-geosciences).
    raise RuntimeError(f"couldn't fetch {url}: {done.stderr.decode(errors='replace')}")


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
    source = Image.open(original(pid))
    if pid in PIXEL_CROPS:
        # A large multi-resolution TIFF: crop the full-size level before converting the rest.
        source = source.crop(PIXEL_CROPS[pid])
    im = rgb(source)
    if pid in CROPS:
        l, t, r, b = CROPS[pid]
        im = im.crop((round(l * im.width), round(t * im.height), round(r * im.width), round(b * im.height)))
    if pid in MIRRORED:
        im = im.transpose(Image.FLIP_LEFT_RIGHT)
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
    # David Scott's hammer and feather, cut from NSSDC's video to the demonstration (0:20 to 1:08).
    "apollo-15-hammer-and-feather": ("https://nssdc.gsfc.nasa.gov/planetary/image/AS15_Ham_feath_drop.mp4",
                                     "AS15_Ham_feath_drop.mp4", 20, 68),
}
SOUND_OUT = os.path.join(ROOT, "assets", "audio", "archive")


def sound(name):
    """The game's copy: mono OGG Vorbis (the video's picture dropped), through ffmpeg."""
    url, file, *cut = SOUNDS[name]
    src = os.path.join(ORIGINALS, file)
    if not os.path.exists(src):
        os.makedirs(ORIGINALS, exist_ok=True)
        with open(src, "wb") as f:
            f.write(fetch(url))
    os.makedirs(SOUND_OUT, exist_ok=True)
    out = os.path.join(SOUND_OUT, name + ".ogg")
    trim = ["-ss", str(cut[0]), "-to", str(cut[1])] if cut else []
    subprocess.run(["ffmpeg", "-v", "error", "-y", "-i", src, *trim, "-vn", "-ac", "1", "-ar", "44100",
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
