"""Publishes the user's sound effects for the clips: python assets-raw/replay/sounds/make_effect_sounds.py

The user's Pixabay finds (2026-10-21) are in assets-raw/audio/, each with a .txt beside it naming its page and
uploader. This raises each one's level by its gain, writes it under assets/audio/replay/ as <id>.ogg (OGG: the
loops must be seamless) and writes its entry into assets/content/replay-sounds.json, after rocket-roar,
replacing an entry of the same id. Credit each file in assets/CREDITS.md. Where they play is
assets-raw/replay/effects.py's.

Needs ffmpeg with libvorbis on the path.
"""
import json
import os
import subprocess

REPO = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", ".."))
RAW = os.path.join(REPO, "assets-raw", "audio")
CATALOG = os.path.join(REPO, "assets", "content", "replay-sounds.json")

# id: (source in assets-raw/audio, kind, gain in dB, volume, description)
SOUNDS = {
    "rcs-puff": ("short_puff", "EFFECT", 2, 0.45,
                 "Clips. An attitude thruster's puff: a short, dry pneumatic hiss (0.5 s). Plays as each jet fires on "
                 "screen: a capsule turning or braking, a craft backing away, a docking's push, a probe's "
                 "separation jets and springs, a course correction's puff on the map, the rocket plane's "
                 "reaction jets; again and again while a jet is stuck open."),
    "hatch-open": ("puff_followed_by_hiss", "EFFECT", 5.5, 0.5,
                   "Clips. A hatch coming open: a puff and the hiss of the air going (1.6 s). Plays as a "
                   "spacewalk's or a crew transfer's hatch swings open, and as Voskhod's airlock vents."),
    "fueling": ("pump_loop", "LOOP", 7, 0.35,
                "Clips. Propellant pumping up the tower: a pump's steady throb, a seamless loop (9.8 s). Plays "
                "under the launch preparations while the tanks fill, and stops as they're topped off (or as "
                "the pad blows). OGG, for a seamless loop."),
    "vent": ("hissing_venting_loop", "LOOP", 10, 0.3,
             "Clips. The oxidizer boiling off: a soft, steady hiss, a seamless loop (3.5 s). Plays from the moment "
             "the tanks are topped off, on through the countdown, until the engines light at the launch. OGG, "
             "for a seamless loop."),
    "reentry-plasma": ("reentry_air_rush_loop", "LOOP", 2, 0.55,
                       "Clips. Reentry: a rushing roar of air, a seamless loop (10.3 s). Plays while the heat's "
                       "glow shows round a capsule, a probe or a carrier coming into an atmosphere, and while "
                       "the rocket plane's skin glows on its fast run. OGG, for a seamless loop."),
    "splash": ("water_splash", "EFFECT", 7, 0.7,
               "Clips. A capsule hitting the sea: a heavy splash (3 s). Plays as the splash shows at splashdown, "
               "hard or soft."),
    "helicopter": ("helicopter_loop", "LOOP", 4, 0.4,
                   "Clips. A recovery helicopter hovering some way off: rotor thump and turbine, a seamless loop "
                   "(35 s). Plays while the helicopter flies in over the recovery. OGG, for a seamless loop."),
    "explosion": ("explosion", "EFFECT", 0, 0.8,
                  "Clips. A blast and its rumble (5 s). Plays wherever something blows up on screen: a rocket "
                  "on the pad or in flight, a stage, a craft breaking up in reentry, a carrier burning up, "
                  "a lander or a probe hitting the ground."),
}


def main():
    entries = []
    for sid, (source, kind, gain, volume, description) in SOUNDS.items():
        out = os.path.join(REPO, "assets", "audio", "replay", sid + ".ogg")
        subprocess.run(["ffmpeg", "-v", "error", "-y", "-i", os.path.join(RAW, source + ".ogg"),
                        "-af", f"volume={gain}dB", "-c:a", "libvorbis", "-q:a", "6", out], check=True)
        entries.append({"id": sid, "kind": kind, "volume": volume, "files": [f"audio/replay/{sid}.ogg"],
                        "credit": True, "description": description})
    catalog = json.load(open(CATALOG, encoding="utf-8"))
    ids = set(SOUNDS)
    sounds = [s for s in catalog["sounds"] if s["id"] not in ids]
    at = next(i for i, s in enumerate(sounds) if s["id"] == "rocket-roar") + 1
    catalog["sounds"] = sounds[:at] + entries + sounds[at:]
    with open(CATALOG, "w", encoding="utf-8", newline="\n") as f:
        json.dump(catalog, f, indent=2, ensure_ascii=False)
        f.write("\n")
    print(len(entries), "sounds written")


if __name__ == "__main__":
    main()
