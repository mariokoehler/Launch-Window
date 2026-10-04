"""Converts the prepared program, category and solar-system art (the user's own) into the
client's assets, named by content id so the client can find an image from the pack alone.

    assets/ui/programs/<program-id>.jpg              896 x 576 (shown at up to 620 x 399)
    assets/ui/categories/<category-id>_<AGENCY>.jpg  448 x 288 (shown at 112 x 72)
    assets/ui/solar_system.jpg                       1920 x 832
    assets/ui/configurations/<configuration-id>.jpg  560 x 360 (shown at up to 280 x 180)

Add a line to PROGRAMS, CATEGORIES or CONFIGURATIONS when a content pack gains one.
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


CONFIGURATIONS = {
    "sputnik-1-orbit": "sputnik_regular.png",
    "sputnik-1-extended": "sputnik_extended.png",
    "sputnik-2-orbit": "sputnik_2.png",
    "korabl-sputnik-2-orbit": "korabl_sputnik_2.png",
    "vostok-uncrewed-suborbital": "vostok_uncrewed_suborbital.png",
    "vostok-suborbital": "vostok_suborbital.png",
    "vostok-uncrewed-orbital": "vostok_uncrewed_orbital.png",
    "vostok-orbital": "vostok_orbital.png",
    "vostok-orbital-duration-1": "vostok_orbital_extended_1.png",
    "vostok-orbital-duration-2": "vostok_orbital_extended_2.png",
    "vostok-joint-orbital": "vostok_joint_orbital.png",
    "explorer-1-orbit": "explorer_i_regular.png",
    "explorer-1-extended": "explorer_i_extended.png",
    "mercury-uncrewed-suborbital": "mercury_uncrewed_suborbital.png",
    "mercury-suborbital": "mercury_suborbital.png",
    "mercury-uncrewed-orbital": "mercury_uncrewed_orbital.png",
    "mercury-orbital": "mercury_orbital.png",
    "mercury-orbital-duration-1": "mercury_orbital_extended.png",
    "mercury-joint-orbital": "mercury_joint_launch.png",
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
    for configuration, file in CONFIGURATIONS.items():
        save(os.path.join(HERE, "Mission Configuration", file), os.path.join(OUT, "configurations", configuration + ".jpg"),
             (560, 360))
    print("wrote", len(PROGRAMS), "programs,", len(CATEGORIES), "categories and the solar system to", OUT)


if __name__ == "__main__":
    main()
