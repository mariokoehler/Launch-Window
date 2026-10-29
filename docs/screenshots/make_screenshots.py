"""The README's screenshots, made from a ScreenSnapshot folder (CLAUDE.md, "Checking a screen").

    python docs/screenshots/make_screenshots.py <ScreenSnapshot folder>

Each pick is scaled to 1280 px wide and saved as JPEG next to this script. The README links them by
these names: rename one there too.
"""
import sys
from pathlib import Path

from PIL import Image

# README name: ScreenSnapshot shot
PICKS = {
    "space-complex": "hub-nasa-summer-upgrading",
    "turn-report": "report",
    "replay": "replay-launch-watching",
    "front-page": "frontpage-landing",
    "mission-plan": "plan-assessment",
    "roads-to-the-moon": "roads-nasa",
    "assemble": "people-assemble",
    "archive": "archive-page-apollo-11",
    "explainer": "explainer-e-01-1",
    "intelligence": "intel-operations",
}
WIDTH = 1280


def main():
    shots = Path(sys.argv[1])
    out = Path(__file__).parent
    for name, shot in PICKS.items():
        image = Image.open(shots / f"{shot}.png").convert("RGB")
        height = round(image.height * WIDTH / image.width)
        image.resize((WIDTH, height), Image.LANCZOS).save(out / f"{name}.jpg", quality=88, optimize=True)
        print(name, "<-", shot)


if __name__ == "__main__":
    main()
