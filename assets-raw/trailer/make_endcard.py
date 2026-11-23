"""Builds the trailer's end card (shot 37), build/endcard.mp4, from the game's splash animation.

Run from anywhere:  python assets-raw/trailer/make_endcard.py   (needs what make_splash.py needs)

The splash (assets-raw/splash/splash.jsx) is an 800 x 800, 30 fps comp on the logo's blue. For the
film it runs at the film's 24 fps on black, nested in a 1920 x 1080 comp, slowed a little to suit
the film's pace, and holds the finished logo so the edit can choose where to fade out. splash.jsx
itself stays untouched: this script rewrites its header lines on a copy.
"""
import os
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
SPLASH = os.path.normpath(os.path.join(HERE, "..", "splash"))
BUILD = os.path.join(HERE, "build")
CLI = r"C:\Program Files\EffectCraft\effectcraft-cli.exe"
OUT = os.path.join(BUILD, "endcard.mp4")

FPS = 24
SLOW = 1.25      # the splash's motion stretched by this much (it's built for a snappy app start)
SCALE = 90       # percent of the splash's 800 px: 720 px tall in the 1080 frame
DUR = 7.0        # seconds: the animation ends at 3.3 s x SLOW, the rest holds the logo


def run(*args):
    print(">", " ".join(args))
    subprocess.run(args, check=True)


def replace_once(text, old, new):
    assert text.count(old) == 1, old
    return text.replace(old, new)


def main():
    os.makedirs(BUILD, exist_ok=True)
    run(sys.executable, os.path.join(SPLASH, "split_logo.py"))
    jsx = open(os.path.join(SPLASH, "splash.jsx"), encoding="utf-8").read()
    layers = os.path.join(SPLASH, "build", "layers").replace("\\", "/") + "/"
    jsx = replace_once(jsx, 'var DIR = new File($.fileName).parent.fsName + "/build/layers/";',
                       f'var DIR = "{layers}";')
    # The inner comp runs long enough for the slowed outer comp; black around the patch.
    jsx = replace_once(jsx, "var FPS = 30, SIZE = 800, DUR = 4.0,",
                       f"var FPS = {FPS}, SIZE = 800, DUR = {DUR / SLOW:.4f},")
    jsx = replace_once(jsx, "comp.bgColor = [0x16 / 255, 0x30 / 255, 0x5c / 255];", "comp.bgColor = [0, 0, 0];")
    jsx += f"""
// The trailer's end card: the splash nested at the film's size, slowed by {SLOW}.
var card = app.project.items.addComp("End card", 1920, 1080, 1, {DUR}, {FPS});
card.bgColor = [0, 0, 0];
var nested = card.layers.add(comp);
nested.stretch = {SLOW * 100};
nested.startTime = 0;
nested.property("Transform").property("Position").setValue([960, 540]);
nested.property("Transform").property("Scale").setValue([{SCALE}, {SCALE}]);
writeLn("end card: " + card.width + "x" + card.height + ", " + card.duration + " s, stretch " + nested.stretch
        + ", out " + nested.outPoint);
"""
    script = os.path.join(BUILD, "endcard.jsx")
    open(script, "w", encoding="utf-8").write(jsx)
    project = os.path.join(BUILD, "endcard.ecproj")
    run(CLI, "script", script, "--empty", "--save-as", project)
    silent = os.path.join(BUILD, "endcard-silent.mp4")
    run(CLI, "render", "--project", project, "--comp", "End card", "--out", silent, "--format", "h264",
        "--audio", "off")
    # The edit places every shot's sound on A1: give the card a silent track like the stills'.
    run("ffmpeg", "-loglevel", "error", "-y", "-i", silent, "-f", "lavfi", "-i", "anullsrc=r=48000:cl=stereo",
        "-c:v", "copy", "-c:a", "aac", "-shortest", OUT)
    os.remove(silent)
    print("wrote", OUT, os.path.getsize(OUT) // 1024, "KB")


if __name__ == "__main__":
    main()
