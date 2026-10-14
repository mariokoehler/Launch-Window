"""Writes the clips' sound cues into their sidecars: python assets-raw/replay/cues.py

The cues are in cues.json, {clip: {variant: [cue, ...]}}, in the sidecar's own form
(ReplayClips.Cue: at, sound, volume, loop, until, fade, carry; the sounds are replay-sounds.json's).
They live here, not in the builders, so a cue changes without a render, and a publish (which rewrites
a sidecar) puts them back by calling apply(). A sidecar with no cues listed loses any it had.

Before the earliest reveal every variant must sound the same (ReplayClipsTest): a loop whose until
falls at or after it sounds the same as one that plays on, so only what's heard before it counts.
"""
import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))
GAME = os.path.normpath(os.path.join(HERE, "..", "..", "assets", "ui", "replay"))


def apply():
    cues = json.load(open(os.path.join(HERE, "cues.json"), encoding="utf-8"))
    wanted = {f"{clip}-{variant}": list_ for clip, variants in cues.items() for variant, list_ in variants.items()}
    for name in sorted(os.listdir(GAME)):
        if not name.endswith(".json"):
            continue
        key = name[:-5]
        path = os.path.join(GAME, name)
        sidecar = json.load(open(path, encoding="utf-8"))
        if key in wanted:
            sidecar["cues"] = wanted.pop(key)
        elif "cues" in sidecar:
            del sidecar["cues"]
        else:
            continue
        with open(path, "w") as f:
            json.dump(sidecar, f, indent=1)
            f.write("\n")
    if wanted:
        raise SystemExit("cues for clips that aren't published: " + ", ".join(sorted(wanted)))


if __name__ == "__main__":
    apply()
    print("cues written")
