"""Writes CLIP_CHECKLIST.md: every distinct mission step the replay needs a clip for.

A step id loses its number (03-launch is launch), as ReplayClips.kind does. A kind that ends in
"-<another kind>" (gemini-a-launch, atv-launch) folds into the longest such kind, because
ReplayClips falls back to a rule for the words after a dash. Each entry is one clip key: the kinds
it covers, how often the standard pack flies it, and what is on screen, all read from the pack.
The variants are ticked from the clips in assets/ui/replay/, found through replay-clips.json.

Run from anywhere: python assets-raw/replay/make_clip_checklist.py
It fails on a clip file that no rule names, so nothing on disk goes unlisted.
"""

import collections
import json
import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
CONTENT = REPO / "assets" / "content"
REPLAY = REPO / "assets" / "ui" / "replay"
OUT = Path(__file__).resolve().parent / "CLIP_CHECKLIST.md"

VARIANTS = ["success", "glitch", "failure"]

# Families, in the order a mission meets them; the first whose test matches a key wins.
FAMILIES = [
    ("Launch", lambda k: re.search(r"launch|countdown|^ascent$|airdrop|drop-plane|takeoff|^pre-launch$|"
                                   r"high-speed-leg|peak-altitude|powered-ascent|wings-deployment|suborbital", k)),
    ("Return to Earth", lambda k: re.search(r"retrofire|reentry|de-?orbit|splashdown|recovery|^descent|"
                                            r"^landing|earth-approach", k)
                                  and not re.search(r"lunar|lander|lk-|probe", k)),
    ("Rendezvous and docking", lambda k: re.search(r"rendezvous|docking|undocking|target-approach|tpi|"
                                                   r"transposition|extraction|crew-transfer|joint|co-", k)),
    ("Crew in space", lambda k: re.search(r"eva|egress|duration|experiments|biometric", k)),
    ("Moon", lambda k: re.search(r"lunar|moon|tli|loi|tei|trans-(lunar|earth)|lem|lk-|langley|lll|"
                                 r"lun-|dir-asc|lpu|powered-descent", k)),
    ("Planets and deep space", lambda k: re.search(r"venus|mars|mercury|jupiter|saturn|sun|helio|"
                                                   r"probe|lander|interplanetary|earth-departure", k)),
    ("Earth orbit", lambda k: True),
]


def load(path):
    return json.loads(path.read_text(encoding="utf-8"))


def kind(step_id):
    return re.sub(r"^\d+-", "", step_id)


# A prefix names a vehicle (gemini-a, atv, transtage) when it comes before two of these, or one
# and it has a digit (soyuz-7k-ok, lk-700): those steps show the same thing for another craft. Other prefixes change the scene (lunar-ascent
# is not ascent), so their steps keep their own entry.
VEHICLE_MARKERS = {"launch-preparations", "countdown", "launch", "deployment", "retrofire", "reentry"}


def suffix_of(k, kinds):
    """The longest other kind k ends in after a dash, as ReplayClips falls back, or None."""
    best = None
    for other in kinds:
        if other != k and k.endswith("-" + other) and (best is None or len(other) > len(best)):
            best = other
    return best


def fold(kinds):
    """Maps each kind to its clip key: folded into its suffix only behind a vehicle prefix."""
    seen = collections.Counter(k[:-len(m) - 1] for k in kinds for m in VEHICLE_MARKERS if k.endswith("-" + m))
    vehicles = {prefix for prefix, n in seen.items() if n >= 2 or re.search(r"\d", prefix)}
    keys = {}
    for k in kinds:
        other = suffix_of(k, kinds)
        keys[k] = other if other and k[:-len(other) - 1] in vehicles else k
        # reentry-a and reentry-b: the same step for the two craft of a joint mission.
        lettered = re.fullmatch(r"(.+)-[ab]", k)
        if lettered and lettered.group(1) in kinds:
            keys[k] = lettered.group(1)
    # A key that folds again (a-b-c into b-c into c) ends at the last one.
    for k in keys:
        while keys[keys[k]] != keys[k]:
            keys[k] = keys[keys[k]]
    return keys


def vehicle(component_id, components):
    if component_id.startswith("@rocket"):
        return "launcher"
    c = components.get(component_id)
    if c is None:
        return "other"
    skill = c["associatedSkill"]
    if skill in ("ROCKETS", "HUMAN_RATED_ROCKETS"):
        return "launcher"
    if skill == "EVA_SUITS":
        return "EVA suit"
    if skill == "CREWED_SPACECRAFT":
        if "lander" in component_id or "lunar-module" in component_id or component_id.startswith("lk-"):
            return "crewed lander"
        if "aircraft" in component_id or "spaceplane" in component_id:
            return "spaceplane"
        return "crewed craft"
    for word, name in (("rover", "rover"), ("roving", "rover"), ("lander", "probe lander"),
                       ("satellite", "satellite"), ("docking", "docking system"), ("orbiter", "orbiter")):
        if word in component_id:
            return name
    if component_id in ("agena-target-vehicle", "gemini-transtage", "s-ivb-space-tug"):
        return "stage or target"
    return "probe"


def rule_clip(key, rules):
    """The clip a rule names for this key itself; a clip it only borrows by suffix doesn't count."""
    for rule in rules:
        if key == rule["step"]:
            return rule["clip"]
    return None


def plural(n, word):
    return f"{n} {word}" + ("" if n == 1 else "s")


def clip_rule_exact(key, rules):
    return any(rule["step"] == key for rule in rules)


def top(counter, n, names=None):
    items = counter.most_common(n)
    return ", ".join(f"{names(k) if names else k} {v}" for k, v in items)


def main():
    pack = CONTENT / "standard"
    configs = load(pack / "mission-configurations.json")
    programs = {p["id"]: p for p in load(pack / "programs.json")}
    bodies = {c["id"]: c["body"] for c in load(pack / "categories.json")}
    components = {c["id"]: c for c in load(pack / "mission-components.json")}
    strings = load(pack / "strings.json")
    early = {kind(st["id"]) for m in load(CONTENT / "early-era" / "mission-configurations.json")
             for s in m["seasons"] for st in s["steps"]}
    rules = load(CONTENT / "replay-clips.json")["rules"]

    uses = []  # (kind, config, season index, step)
    for m in configs:
        for i, season in enumerate(m["seasons"]):
            for st in season["steps"]:
                uses.append((kind(st["id"]), m, i, st))
    all_kinds = {u[0] for u in uses}
    keys = fold(all_kinds)

    entries = collections.defaultdict(lambda: {
        "kinds": collections.Counter(), "configs": set(), "programs": collections.Counter(),
        "agencies": set(), "bodies": collections.Counter(), "vehicles": collections.Counter(),
        "crewed": 0, "risk": 0, "later": 0, "uses": 0, "early": False})
    for k, m, i, st in uses:
        e = entries[keys[k]]
        program = programs[m["programId"]]
        e["kinds"][k] += 1
        e["uses"] += 1
        if m["id"] not in e["configs"]:
            e["programs"][m["programId"]] += 1
        e["configs"].add(m["id"])
        e["agencies"].add(program["agency"])
        e["bodies"][bodies[program["categoryId"]]] += 1
        for v in {vehicle(ci["componentId"], components) for ci in st["componentInvolvements"]
                  if ci["percent"] > 0}:
            e["vehicles"][v] += 1
        e["crewed"] += bool(st["crewInvolvements"])
        e["risk"] += bool(st["crewRisk"])
        e["later"] += i > 0
        e["early"] |= k in early

    # Every clip file on disk must belong to a rule, or it would go unlisted.
    named = {r["clip"] for r in rules}
    stray = sorted({p.stem.rsplit("-", 1)[0] for p in REPLAY.glob("*.webm") if p.stem != "no-signal"} - named)
    if stray:
        sys.exit(f"Clips no rule in replay-clips.json names: {', '.join(stray)}")

    def step_name(k):
        s = strings["steps"].get(k)
        return s["name"] if s else k

    def program_name(p):
        return strings["programs"].get(p, {}).get("name", p)

    by_family = collections.defaultdict(list)
    for key, e in entries.items():
        family = next(name for name, test in FAMILIES if test(key))
        by_family[family].append((key, e))

    done_variants = 0
    done_keys = 0
    lines = []
    for family, _ in FAMILIES:
        items = sorted(by_family.get(family, []), key=lambda ke: (-ke[1]["uses"], ke[0]))
        if not items:
            continue
        lines += ["", f"## {family} ({len(items)})", ""]
        for key, e in items:
            clip = rule_clip(key, rules)
            ticks = []
            have = 0
            cues = None
            for v in VARIANTS:
                ok = clip is not None and (REPLAY / f"{clip}-{v}.webm").exists() \
                    and (REPLAY / f"{clip}-{v}.json").exists()
                have += ok
                ticks.append(f"[{'x' if ok else ' '}] {v}")
                if ok and cues is None:
                    cues = bool(load(REPLAY / f"{clip}-{v}.json").get("cues"))
            done_variants += have
            done_keys += have == len(VARIANTS)
            n = len(e["configs"])
            flags = []
            if e["early"]:
                flags.append("**(early-era)**")
            if e["risk"]:
                flags.append(f"crew risk in {e['risk']} of {e['uses']}")
            elif e["crewed"]:
                flags.append(f"crewed in {e['crewed']} of {e['uses']}")
            else:
                flags.append("uncrewed")
            if e["later"]:
                flags.append(f"later seasons in {e['later']} of {e['uses']}")
            lines.append(f"- [{'x' if have == len(VARIANTS) else ' '}] **{step_name(key)}** `{key}`: "
                         f"{plural(e['uses'], 'use')} in {plural(n, 'configuration')}, "
                         f"{plural(len(e['programs']), 'program')} "
                         f"({'/'.join(sorted(e['agencies']))}) " + ", ".join(flags))
            lines.append(f"  - Clip: {('`' + clip + '`') if clip else 'none yet'}; " + ", ".join(ticks)
                         + ("" if cues is None else f"; cues {'yes' if cues else 'no'}"))
            others = {k: c for k, c in e["kinds"].items() if k != key}
            if others:
                lines.append("  - Also covers: " + ", ".join(f"`{k}` {c}" for k, c in sorted(others.items())))
            borrowed = suffix_of(key, all_kinds)
            if borrowed and not clip_rule_exact(key, rules):
                lines.append(f"  - Until it has a rule of its own, it plays `{borrowed}`'s clip: give it one "
                             f"before that clip lands, or order the rules so this one comes first.")
            lines.append(f"  - On screen: {top(e['vehicles'], 6) or 'nothing listed'}")
            lines.append(f"  - Destinations: {top(e['bodies'], 4)}")
            lines.append(f"  - Programs: {top(e['programs'], 4, program_name)}")

    total = len(entries)
    header = [
        "# Clip checklist: mission steps for the replay",
        "",
        "Every distinct mission step in the standard pack (`assets/content/standard`), one entry per clip",
        "key, for the replay's animated steps (`ui-spec.md` §4.7.3). Generated by",
        "`assets-raw/replay/make_clip_checklist.py`: don't edit by hand, rerun it. A step id loses its",
        "number, and a step whose id is another step's behind a vehicle prefix (`gemini-a-launch`,",
        "`atv-launch`) folds into that one, as `ReplayClips` falls back to the words after a dash. Variants are ticked when the clip",
        "and its sidecar are in `assets/ui/replay/` and a rule in `replay-clips.json` names it.",
        "",
        f"**Status:** {done_keys}/{total} steps complete, {done_variants}/{total * len(VARIANTS)} variants. "
        f"{len({u[0] for u in uses})} step ids in {len(configs)} configurations.",
        "",
        "How to read an entry (all counted automatically, a first pass):",
        "- **Uses**: step occurrences across all configurations; sorted by it within each family.",
        "  **(early-era)** marks steps the early-era pack flies, which players see first.",
        "- **Crew risk / crewed / uncrewed**: from the steps' `crewRisk` and crew involvements. A crew-risk",
        "  step's failure needs care, and may want its own `catastrophic` variant later.",
        "- **Later seasons**: occurrences after a mission's first season (coasts, transits, long stays).",
        "- **On screen**: the kinds of component the step involves, counted per use (launcher, crewed",
        "  craft, probe, ...). A mix suggests the step may need more than one clip.",
        "- **Destinations**: the programs' target bodies, not where the step happens (a Moon program's",
        "  launch is still on Earth).",
        "- **Also covers**: the step ids folded into this one, with their counts. Only a vehicle prefix",
        "  folds (one seen before a launch, deployment, retrofire or reentry step: `gemini-a`, `atv`);",
        "  `lunar-ascent` keeps its own entry, with a note that it borrows `ascent`'s clip until it has a rule.",
    ]
    OUT.write_text("\n".join(header + lines) + "\n", encoding="utf-8", newline="\n")
    print(f"{OUT}: {total} clip keys, {done_keys} complete")


if __name__ == "__main__":
    main()
