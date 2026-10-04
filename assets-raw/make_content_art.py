"""Converts the prepared program, category and solar-system art (the user's own) into the
client's assets, named by content id so the client can find an image from the pack alone.

    assets/ui/programs/<program-id>.jpg              896 x 576 (shown at up to 620 x 399)
    assets/ui/categories/<category-id>_<AGENCY>.jpg  448 x 288 (shown at 112 x 72)
    assets/ui/solar_system.jpg                       1920 x 832

Add a line to PROGRAMS or CATEGORIES when a content pack gains a program or category.
Run from the repo root:  python assets-raw/make_content_art.py   (needs Pillow)
"""
import os

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.normpath(os.path.join(HERE, ".."))
OUT = os.path.join(REPO, "assets", "ui")

PROGRAMS = {
    "sputnik-1": "sputnik.png",
    "sputnik-2": "sputnik_2.png",
    "korabl-sputnik-2": "korabl_sputnik_2.png",
    "vostok": "vostok.png",
    "explorer-1": "explorer_I.png",
    "mercury": "mercury.png",
}

CATEGORIES = {
    "earth-orbit-satellites": "earth_orbiting_satellites",
    "one-crew-capsules": "one_crew_ballistic_capsule",
}


def save(src, dst, size):
    image = Image.open(src).convert("RGB")
    if size is not None and image.size != size:
        image = image.resize(size, Image.LANCZOS)
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    image.save(dst, quality=88)


def main():
    for program, file in PROGRAMS.items():
        save(os.path.join(HERE, "Programs", file), os.path.join(OUT, "programs", program + ".jpg"), (896, 576))
    for category, stem in CATEGORIES.items():
        for agency in ("NASA", "SOVIET"):
            src = os.path.join(HERE, "Program Categories", stem + "_" + agency + ".png")
            if os.path.exists(src):
                save(src, os.path.join(OUT, "categories", category + "_" + agency + ".jpg"), (448, 288))
    save(os.path.join(HERE, "backgrounds", "solar_system.png"), os.path.join(OUT, "solar_system.jpg"), (1920, 832))
    print("wrote", len(PROGRAMS), "programs,", len(CATEGORIES), "categories and the solar system to", OUT)


if __name__ == "__main__":
    main()
