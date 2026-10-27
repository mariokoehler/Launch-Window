"""Converts the Space Complex hub art (the user's own, assets-raw/main_menu/) into the client's
assets: 2 agencies x 4 seasons x 4 times of day, plus each agency's hotspot polygons.

    assets/ui/hub/<AGENCY>_<season>_<time>.jpg   1920 x 1080
    assets/ui/hub/<AGENCY>.hotspots.json         scaled to 1920 x 1080
    assets/ui/archetype_<agency>.jpg             872 x 490, the Join screen's card (summer noon)

The raw images can be any size a little taller than 16:9 (since 2026-10-10 they are 2720 x 1536;
the first set was 1920 x 1088). The bottom rows past 16:9 are cropped (design.md 3.13), then the
image is scaled to 1920 x 1080. The hotspots, drawn on the raw image with a top-left origin, are
scaled by the same factor; the client reads their points as 1920 x 1080 pixels.
Run from the repo root:  python assets-raw/make_hub_art.py   (needs Pillow)
"""
import json
import os

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.normpath(os.path.join(HERE, ".."))
RAW = os.path.join(HERE, "main_menu")
OUT = os.path.join(REPO, "assets", "ui", "hub")

AGENCIES = ("NASA", "SOVIET")
SEASONS = ("winter", "spring", "summer", "fall")
TIMES = ("dawn", "noon", "dusk", "night")
WIDTH, HEIGHT = 1920, 1080
CARD = (872, 490)
CARD_VIEW = ("summer", "noon")


def main():
    os.makedirs(OUT, exist_ok=True)
    for agency in AGENCIES:
        scale = None
        for season in SEASONS:
            for time in TIMES:
                image = Image.open(os.path.join(RAW, f"LW_main_menu_{agency}_{season}_{time}.png")).convert("RGB")
                height = round(image.width * HEIGHT / WIDTH)
                if image.height < height:
                    raise SystemExit(f"{agency} {season} {time} is wider than 16:9: {image.size}")
                if scale is not None and image.width / WIDTH != scale:
                    raise SystemExit(f"{agency} {season} {time} differs in size from the others: {image.size}")
                scale = image.width / WIDTH
                image = image.crop((0, 0, image.width, height))
                if image.size != (WIDTH, HEIGHT):
                    image = image.resize((WIDTH, HEIGHT), Image.LANCZOS)
                image.save(os.path.join(OUT, f"{agency}_{season}_{time}.jpg"), quality=80, optimize=True)
                if (season, time) == CARD_VIEW:
                    image.resize(CARD, Image.LANCZOS).save(
                        os.path.join(REPO, "assets", "ui", f"archetype_{agency.lower()}.jpg"), quality=85, optimize=True)
        with open(os.path.join(RAW, f"LW_main_menu_{agency}.hotspots.json"), encoding="utf-8") as f:
            hotspots = json.load(f)
        if hotspots["imageWidth"] / WIDTH != scale:
            raise SystemExit(f"{agency} hotspots were drawn on a {hotspots['imageWidth']} px wide image")
        for polygon in hotspots["polygons"]:
            for point in polygon["points"]:
                point["x"] = round(point["x"] / scale, 1)
                point["y"] = round(point["y"] / scale, 1)
        hotspots["imageWidth"], hotspots["imageHeight"] = WIDTH, HEIGHT
        with open(os.path.join(OUT, f"{agency}.hotspots.json"), "w", encoding="utf-8", newline="\n") as f:
            json.dump(hotspots, f, indent=2)
            f.write("\n")
    print("ok")


if __name__ == "__main__":
    main()
