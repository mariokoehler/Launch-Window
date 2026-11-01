"""Places the clips' sound effects in cues.json, timed off the clips' own built projects:
python assets-raw/replay/effects.py

The effects are the user's Pixabay finds (2026-10-21, published by sounds/make_effect_sounds.py). Each plays
where its layer shows in a clip's EffectCraft project (steps/build/ and launch-storyboard/build/, written on
every build and kept out of git), so this runs on the main PC only, and again after any clip is published
with a changed timeline:
- explosion: each `burst` (not the spark where Vostok's cable parts, the clips with `r-cable`)
- rcs-puff: each puff of a thruster layer (PUFFS), and a few while `r-jet-stuck` is stuck open
- hatch-open: each hatch swinging open (`e-hatch*` turning away from shut), and Voskhod's airlock venting
- fueling: while the tanks' levels rise (`level-*`)
- vent: from the tanks topped off (or the clip's start, carried on) while the wisps show, until the engines
  light or the pad blows
- reentry-plasma: while an entry's glow shows (GLOWS)
- splash: the `r-splash` at splashdown
- helicopter: from just before the helicopter flies in to the clip's end
- decoupler (the user's own, Stable Audio 3, as the three below): springs, bolts, straps and latches firing
  (SEPARATIONS' flashes; latches only where they let go), a spent stage falling away, the ascent stage leaving
  the descent stage, the X-15's ventral fin dropped (where its turn parts from the plane's, with its chute opening
  after: the layer moves with the plane all along, and a fin torn off at speed has no chute)
- explosion-debris: a `burst` near the ground (GROUND_BLASTS: the pad, a launch falling back, the Luna landers)
- servo: an arm, a dish or a panel moving, a boom drawn in, a lid shut, PrOP-M's first step (SERVOS)
- deploy: booms, whips, wings and masts swinging out, ramps down, a lid or petals opening, panels opening,
  gear down (DEPLOYS)
Every cue of these sounds in cues.json is replaced, the rest left alone. Before a clip's earliest reveal every
variant must sound the same (ReplayClipsTest): there an effect only some variants have waits for the reveal if
it's just before it, and is left out if not (canonical). A clip whose project is newer
than its published render is skipped and named: the project may no longer be the picture.
Then cues.py writes the sidecars.
"""
import json
import os
import re
import sys
from multiprocessing import Pool

HERE = os.path.dirname(os.path.abspath(__file__))
GAME = os.path.normpath(os.path.join(HERE, "..", "..", "assets", "ui", "replay"))
BUILDS = [os.path.join(HERE, "steps", "build"), os.path.join(HERE, "launch-storyboard", "build")]
UNIT = 254016000000  # EffectCraft's ticks a second
VARIANTS = ("success", "glitch", "failure")
SOUNDS = {"explosion", "rcs-puff", "hatch-open", "fueling", "vent", "reentry-plasma", "splash", "helicopter",
          "decoupler", "explosion-debris", "servo", "deploy"}
PUFFS = {"r-jet-l", "r-jet-r", "k-brake", "k-push", "k-side", "g-puff-1", "g-puff-2", "vk-jets",
         "x15-rcs-down", "x15-rcs-up", "e154-rcs-down", "e154-rcs-up"}
MAP_PUFF = "m-puff"  # a course correction's puff on the map: far off, so quieter
GLOWS = {"r-glow", "e-glow", "b-glow", "pv-glow-1", "x15-heat", "e154-heat"}
HATCHES = {"e-hatch", "e-hatch-b", "e-hatch-pair"}
WISPS = {"wisp-1", "wisp-2", "wisp-3"}
LEVELS = {"level-lower", "level-upper"}
# Separations: the flashes as springs, bolts, straps and latches fire.
SEPARATIONS = {"d-springs", "r-pyro", "r-cut-a", "r-cut-b", "r-trunk-cut-a", "r-trunk-cut-b", "r-om-cut-a", "r-om-cut-b",
               "t-flash-sep", "t-flash-lm", "v-latch", "vk-latch", "pv-latch", "pv-clamp-1", "pv-clamp-2", "pv-clamp-3"}
LETTING_GO = re.compile(r"undock|jettison|cast-off|release|deployment|separation")  # where k-latch's flash lets go
GROUND_BLASTS = re.compile(r"(^|-)(launch-preparations|countdown|launch)$|^luna-(landing|liftoff)")
# (layer, prop, which spans): "all" every span, "out" growing away from its start, "in" shrinking back.
SERVOS = [("v-arm", "rotation", "all"), ("s-arm", "rotation", "all"), ("g-dish", "rotation", "all"),
          ("sv-panel", "rotation", "all"), ("e-boom", "scale", "in"), ("u-lid", "rotation", "in")]
DEPLOYS = [("g-boom", "rotation", "all"), ("u-ramp-f", "rotation", "all"), ("u-lid", "rotation", "out"),
           ("v-mast", "scale", "all"), ("t-panel-top", "rotation", "all"), ("e-boom", "scale", "out"),
           ("e154-gear", "rotation", "all")]
UNFOLDS = {"d-whips-1", "d-wing-l-1"}  # drawings shown in turn as they swing out: the first one's showing
PETALS = {"mars-2-mars-landing": 2.667}
# Clips whose failure breaks up at staging, a moment after the others stage: it hears their staging too.
BORROW = (re.compile(r"(^|-)ascent$"), "decoupler")  # Mars 2's petals open after touchdown (the legs' turn runs on from the fall)
WANTED = PUFFS | GLOWS | HATCHES | WISPS | LEVELS | SEPARATIONS | UNFOLDS | {
    MAP_PUFF, "burst", "r-cable", "r-jet-stuck", "e-vent", "r-splash", "y-heli", "flame-ignite", "o-orbit", "o-streak",
    "k-latch", "debris-3", "n-ascent", "x15-ventral", "w-plane", "r-main", "pr-rover"} | {n for n, _, _ in SERVOS + DEPLOYS}


def props(o, out):
    if isinstance(o, dict):
        if o.get("node") == "Prop" and o.get("keys"):
            out[o["match"]] = [(k["time"] / UNIT, k["value"]["v"]) for k in o["keys"]]
        for v in o.values():
            props(v, out)
    elif isinstance(o, list):
        for v in o:
            props(v, out)
    return out


def shown(keys, end):
    """The spans an opacity track is above 5 %."""
    spans, start = [], None
    for t, v in keys:
        if v > 5 and start is None:
            start = t
        elif v <= 5 and start is not None:
            spans.append((start, t))
            start = None
    if start is not None:
        spans.append((start, end))
    return spans


def moving(keys):
    """The spans a keyed value changes over, each as (start, end, value at start, value at end)."""
    spans, cur = [], None
    for (t0, v0), (t1, v1) in zip(keys, keys[1:]):
        if v1 != v0:
            cur = [t0, t1, v0, v1] if cur is None else [cur[0], t1, cur[2], v1]
        elif cur is not None:
            spans.append(tuple(cur))
            cur = None
    if cur is not None:
        spans.append(tuple(cur))
    return spans


def read(name):
    """A clip variant's layers: {layer: {"shown": spans, "moves": {prop: spans}, "first": {prop: value}}}."""
    path = next(p for p in (os.path.join(b, name + ".ecproj") for b in BUILDS) if os.path.exists(p))
    project = json.load(open(path, encoding="utf-8"))
    comp = next(i for i in project["items"].values() if i["kind"]["type"] == "Comp")
    dur = comp["kind"]["duration"] / UNIT
    layers = {}
    for layer in comp["kind"]["layers"]:
        if layer["name"] not in WANTED:
            continue
        keyed = props(layer.get("props", {}), {})
        layers[layer["name"]] = {"shown": shown(keyed["opacity"], dur) if "opacity" in keyed else None,
                                 "moves": {m: moving(k) for m, k in keyed.items() if m != "opacity"},
                                 "first": {m: k[0][1] for m, k in keyed.items()},
                                 "path": keyed.get("position") if layer["name"] == "y-heli" else None,
                                 "turn": keyed.get("rotation") if layer["name"] in ("x15-ventral", "w-plane") else None}
    return name, {"name": name.rsplit("-", 1)[0], "dur": dur, "layers": layers}


def r(t):
    return round(t, 3)


def cue(at, sound, volume=None, loop=False, carry=False, until=None, fade=None):
    c = {"at": r(at), "sound": sound}
    if volume is not None:
        c["volume"] = volume
    if loop:
        c["loop"] = True
    if carry:
        c["carry"] = True
    if until is not None:
        c["until"] = r(until)
        c["fade"] = fade
    return c


def onsets(spans, gap):
    """The spans' starts, a start within gap of the last one kept dropped."""
    out = []
    for s in sorted(spans):
        if not out or s - out[-1] > gap:
            out.append(s)
    return out


def effects(clip):
    """The effect cues of one clip variant, from its layers."""
    L, dur = clip["layers"], clip["dur"]
    starts = lambda name: [a for a, b in (L.get(name, {}).get("shown") or [])]
    cues = []
    bursts = starts("burst") if "r-cable" not in L else []
    far = "o-orbit" in L or "o-streak" in L
    ground = GROUND_BLASTS.search(clip["name"])
    for t in bursts:
        cues.append(cue(t, "explosion-debris") if ground else cue(t, "explosion", 0.5 if far else None))
    moves = lambda name, prop: L.get(name, {}).get("moves", {}).get(prop, [])
    parts = [t for name in SEPARATIONS for t in starts(name)]
    if LETTING_GO.search(clip["name"]):
        parts += starts("k-latch")
    parts += [a for a, b, v0, v1 in moves("debris-3", "position")[:1] if not any(abs(a - t) < 0.5 for t in bursts)]
    parts += [a for a, b, v0, v1 in moves("n-ascent", "position")[:1]]
    fin, plane = L.get("x15-ventral", {}).get("turn"), L.get("w-plane", {}).get("turn")
    if fin and plane and L.get("r-main", {}).get("shown"):
        parts += [t for (t, a), (_, b) in zip(fin, plane) if abs(a - b) > 0.5][:1]
    for t in onsets(parts, 0.3):
        cues.append(cue(t, "decoupler"))

    def size(v):
        return sum(abs(x) for x in v) if isinstance(v, list) else abs(v)

    def spans(table):
        out = []
        for name, prop, which in table:
            for a, b, v0, v1 in moves(name, prop):
                if which == "all" or (which == "out") == (size(v1) > size(v0)):
                    out.append(a)
        return out
    servo = spans(SERVOS) + [a for a, b, v0, v1 in moves("pr-rover", "position")[1:2]]
    for t in onsets(servo, 1.0):
        cues.append(cue(t, "servo"))
    unfold = spans(DEPLOYS) + [t for name in UNFOLDS for t in starts(name)[:1]]
    if clip["name"] in PETALS:
        unfold.append(PETALS[clip["name"]])
    for t in onsets(unfold, 1.0):
        cues.append(cue(t, "deploy"))
    puffs = [t for name in PUFFS for t in starts(name)]
    for a, b in (L.get("r-jet-stuck", {}).get("shown") or []):
        puffs += [t for t in (a, a + 0.7, a + 1.6) if t < b]
    for t in onsets(puffs, 0.15):
        cues.append(cue(t, "rcs-puff"))
    for t in onsets(starts(MAP_PUFF), 0.15):
        cues.append(cue(t, "rcs-puff", 0.25))
    opens = starts("e-vent")
    for name in HATCHES:
        for a, b, v0, v1 in L.get(name, {}).get("moves", {}).get("rotation", []):
            if abs(v1) > abs(v0):
                opens.append(a)
    for t in onsets(opens, 0.3):
        cues.append(cue(t, "hatch-open"))
    glow = [s for name in GLOWS for s in (L.get(name, {}).get("shown") or [])]
    if glow:
        a, b = min(s[0] for s in glow), max(s[1] for s in glow)
        ends = b >= dur - 0.05
        cues.append(cue(a, "reentry-plasma", loop=True, carry=a == 0, until=None if ends else b, fade=0.6))
    for t in onsets(starts("r-splash"), 0.3):
        cues.append(cue(t, "splash"))
    heli = L.get("y-heli", {}).get("path")
    if heli:
        enter = next((t for t, (x, y, *_) in heli if x < 2100), None)  # its body's edge comes into the frame
        if enter is not None:
            cues.append(cue(max(0.0, enter - 1.0), "helicopter", loop=True))
    fills = sorted((a, b) for name in LEVELS for a, b, v0, v1 in L.get(name, {}).get("moves", {}).get("position", []))
    merged = []
    for a, b in fills:
        if merged and a <= merged[-1][1] + 0.1:
            merged[-1][1] = max(merged[-1][1], b)
        else:
            merged.append([a, b])
    for a, b in merged:
        b = min([b] + [t for t in bursts if t > a])
        cues.append(cue(a, "fueling", loop=True, until=b, fade=0.4))
    wisps = [s for name in WISPS for s in (L.get(name, {}).get("shown") or [])]
    if wisps and not (merged and bursts):
        a = max([min(s[0] for s in wisps)] + [b for _, b in merged])
        b = min([max(s[1] for s in wisps)] + [t for t in bursts + starts("flame-ignite") if t > a])
        if b > a:
            ends = b >= dur - 0.05
            cues.append(cue(a, "vent", loop=True, carry=a == 0, until=None if ends else b, fade=0.3))
    return cues


NEAR = 0.35  # a variant's own effect this close before the reveal waits for it; one earlier is dropped


def canonical(variants, reveal, clip=""):
    """Before the earliest reveal every variant sounds the same: an effect every variant has stays (at the success's
    time), one only some have moves to the reveal if it's NEAR it and is dropped if not. A loop every variant starts
    stops where the success's does if that's before the reveal; if not, each variant keeps its own stop, but runs
    on at least to the reveal."""
    base = variants.get("success") or next(iter(variants.values()))

    def has(cues, c):
        return any(o["sound"] == c["sound"] and bool(o.get("loop")) == bool(c.get("loop"))
                   and abs(o["at"] - c["at"]) <= 0.05 and o["at"] < reveal for o in cues)

    common = [c for c in base if c["at"] < reveal and (all(has(cues, c) for cues in variants.values())
                                                       or (BORROW[0].search(clip) and c["sound"] == BORROW[1]))]
    out = {}
    for v, cues in variants.items():
        fixed = []
        for c in common:
            c = dict(c)
            if c.get("loop") and (c.get("until") is None or c["until"] >= reveal):
                mine = next(o for o in cues if o["sound"] == c["sound"] and o.get("loop") and abs(o["at"] - c["at"]) <= 0.05)
                c.pop("until", None)
                c.pop("fade", None)
                if mine.get("until") is not None:
                    c["until"], c["fade"] = max(mine["until"], reveal), mine["fade"]
            fixed.append(c)
        for c in cues:
            if c["at"] >= reveal:
                fixed.append(c)
            elif not any(c["sound"] == o["sound"] and abs(c["at"] - o["at"]) <= 0.05 for o in common) \
                    and reveal - c["at"] <= NEAR and (c.get("until") is None or c["until"] > reveal + 0.1):
                fixed.append(dict({k: x for k, x in c.items() if k != "carry"}, at=reveal))
        out[v] = fixed
    return out


def main():
    names = sorted(n[:-5] for n in os.listdir(GAME) if n.endswith(".json") and n != "no-signal.json")
    stale, todo = set(), []
    for n in names:
        clip = n.rsplit("-", 1)[0]
        project = next((p for p in (os.path.join(b, n + ".ecproj") for b in BUILDS) if os.path.exists(p)), None)
        webm = os.path.join(GAME, n + ".webm")
        if project is None or os.path.getmtime(project) > os.path.getmtime(webm) + 60:
            stale.add(clip)
        else:
            todo.append(n)
    todo = [n for n in todo if n.rsplit("-", 1)[0] not in stale]
    with Pool(6) as pool:
        read_ = dict(pool.imap_unordered(read, todo, chunksize=4))
    path = os.path.join(HERE, "cues.json")
    crlf = b"\r\n" in open(path, "rb").read(200)  # keep the file's line endings
    cues = json.load(open(path, encoding="utf-8"))
    for clip in sorted({n.rsplit("-", 1)[0] for n in todo}):
        sidecars = {v: json.load(open(os.path.join(GAME, f"{clip}-{v}.json"))) for v in VARIANTS
                    if os.path.exists(os.path.join(GAME, f"{clip}-{v}.json"))}
        reveal = min(s["revealAt"] for s in sidecars.values() if s.get("revealAt") is not None)
        new = canonical({v: effects(read_[f"{clip}-{v}"]) for v in sidecars}, reveal, clip)
        entry = cues.get(clip, {})
        for v in sidecars:
            kept = [c for c in entry.get(v, []) if c["sound"] not in SOUNDS]
            merged = sorted(kept + new[v], key=lambda c: (c["at"], c["sound"]))
            if merged:
                entry[v] = merged
            else:
                entry.pop(v, None)
        if entry:
            cues[clip] = {v: entry[v] for v in VARIANTS if v in entry}
        else:
            cues.pop(clip, None)
    with open(path, "w", encoding="utf-8", newline="\r\n" if crlf else "\n") as f:
        f.write("{\n" + ",\n".join(f"  {json.dumps(k)}: {json.dumps(v, ensure_ascii=False)}" for k, v in cues.items())
                + "\n}\n")
    sys.path.insert(0, HERE)
    import cues as cues_py
    cues_py.apply()
    count = sum(c["sound"] in SOUNDS for e in cues.values() for l in e.values() for c in l)
    print(f"{count} effect cues on {len(todo) // 3} clips; cues written")
    if stale:
        print("skipped, their projects newer than their renders (publish them again, then rerun):",
              ", ".join(sorted(stale)))


if __name__ == "__main__":
    main()
