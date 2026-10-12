"""Builds the splash video, assets/ui/splash/splash.webm (ui-spec.md 4.0), from the logo.

Run from anywhere:  python assets-raw/splash/make_splash.py   (needs: pip install resvg-py, and
EffectCraft at its default install path)

1. split_logo.py rasterizes assets/branding/logo.svg into aligned layers (build/layers/).
2. EffectCraft runs splash.jsx headless, which animates them; the project goes to build/.
3. EffectCraft renders the comp as VP9 WebM (gdx-video plays WebM and AV1 on desktop, never MP4),
   plus a GIF preview in build/ for reviewing.

Everything in build/ is regenerated on each run and stays out of git.
"""
import os
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.normpath(os.path.join(HERE, "..", ".."))
BUILD = os.path.join(HERE, "build")
CLI = r"C:\Program Files\EffectCraft\effectcraft-cli.exe"
VIDEO = os.path.join(REPO, "assets", "ui", "splash", "splash.webm")


def run(*args):
    print(">", " ".join(args))
    subprocess.run(args, check=True)


def main():
    os.makedirs(BUILD, exist_ok=True)
    run(sys.executable, os.path.join(HERE, "split_logo.py"))
    project = os.path.join(BUILD, "splash.ecproj")
    run(CLI, "script", os.path.join(HERE, "splash.jsx"), "--empty", "--save-as", project)
    os.makedirs(os.path.dirname(VIDEO), exist_ok=True)
    run(CLI, "render", "--project", project, "--comp", "Splash", "--out", VIDEO,
        "--format", "webm", "--webm-codec", "vp9", "--audio", "off")
    run(CLI, "render", "--project", project, "--comp", "Splash", "--out", os.path.join(BUILD, "splash.gif"),
        "--format", "gif", "--resolution", "half")
    print("wrote", VIDEO, os.path.getsize(VIDEO) // 1024, "KB")


if __name__ == "__main__":
    main()
