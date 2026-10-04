"""Converts the Space Complex hub art (the user's own, assets-raw/main_menu/) into the client's
assets: 2 agencies x 4 seasons x 4 times of day, plus each agency's hotspot polygons.

    assets/ui/hub/<AGENCY>_<season>_<time>.jpg   1920 x 1080
    assets/ui/hub/<AGENCY>.hotspots.json         copied as is

The raw images are 1920 x 1088. Only the bottom 8 rows are cropped (design.md 3.13): the
hotspots use a top-left origin and sit clear of that strip, so their coordinates stay valid.
Run from the repo root:  python assets-raw/make_hub_art.py   (needs Pillow)
"""
import os
import shutil

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.normpath(os.path.join(HERE, ".."))
RAW = os.path.join(HERE, "main_menu")
OUT = os.path.join(REPO, "assets", "ui", "hub")

AGENCIES = ("NASA", "SOVIET")
SEASONS = ("winter", "spring", "summer", "fall")
TIMES = ("dawn", "noon", "dusk", "night")
WIDTH, HEIGHT = 1920, 1080


def main():
    os.makedirs(OUT, exist_ok=True)
    for agency in AGENCIES:
        shutil.copyfile(os.path.join(RAW, f"LW_main_menu_{agency}.hotspots.json"),
                        os.path.join(OUT, f"{agency}.hotspots.json"))
        for season in SEASONS:
            for time in TIMES:
                image = Image.open(os.path.join(RAW, f"LW_main_menu_{agency}_{season}_{time}.png")).convert("RGB")
                if image.width != WIDTH or image.height < HEIGHT:
                    raise SystemExit(f"unexpected size {image.size} for {agency} {season} {time}")
                image.crop((0, 0, WIDTH, HEIGHT)).save(os.path.join(OUT, f"{agency}_{season}_{time}.jpg"),
                                                       quality=80, optimize=True)
    print("ok")


if __name__ == "__main__":
    main()
