"""Generates sound-effect takes in ComfyUI: python assets-raw/audio/sfx/make_sfx.py [seeds] [--only=<id>,...]

Queues the user's Sound_Effects workflow (Stable Audio 3 Medium; its prompt expanded first by Qwen with the
workflow's SFX template, as its use_reprompt switch does) for every sound the replay's audio plan
(assets-raw/replay/sounds/audio_cues.py, LIB) still lacks, one take per seed (default 1101, 2202, 3303,
4404). The subgraph is flattened here node for node (its category combo, switch and length maths become
plain values). Each take is a FLAC in output/lw_sfx/, and a copy goes to output/lw_sfx/review/, named
for what it should be, so the user can judge a take by its file name:

    <sound> - <one-shot|loop> <the plan's length> - <what you hear> (s<seed>).flac

A take is longer than the plan's length, so there's room to cut: a one-shot is followed by silence, a loop
runs long enough to find a seamless stretch in. Takes already there are skipped, so a rerun fills gaps and
the user's deletions in review/ stick (sfx-prompts.json there lists Qwen's expanded prompt per take, and
qc.txt the levels: a "silent" or "clipped" prefix marks a take to doubt). The batch's end calls /free.

--variant=neg adds a negative prompt (NEGATIVE: the workflow's is empty, and Stable Audio drifts into
music); --variant=raw also skips Qwen, giving Stable Audio the brief itself. A variant's takes go to
output/lw_sfx/<variant>/ and its own review/, to compare with the plain ones seed for seed.
"""
import glob
import json
import math
import os
import re
import shutil
import sys
import time
import urllib.request

URL = "http://127.0.0.1:8188"
VARIANT = next((a[len("--variant="):] for a in sys.argv if a.startswith("--variant=")), "")
PREFIX = "lw_sfx" + ("/" + VARIANT if VARIANT else "")
OUTPUT = os.path.join(r"E:\Comfy-Desktop\ComfyUI-Shared\output", *PREFIX.split("/"))
NEGATIVE = "music, melody, musical instruments, drums, beat, rhythm, song, chords, synth pad"
REVIEW = os.path.join(OUTPUT, "review")
WORKFLOW = r"E:\Comfy-Desktop\ComfyUI-Installs\ComfyUI\ComfyUI\user\default\workflows\Sound_Effects.json"
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "..", "replay", "sounds"))
import audio_cues  # noqa: E402  (the plan: id -> (type, status, length, takes, volume, hear, where))

ARGS = [a for a in sys.argv[1:] if not a.startswith("--")]
SEEDS = [int(s) for s in ARGS[0].split(",")] if ARGS else [1101, 2202, 3303, 4404]


def template():
    """The workflow's own reprompt text: its prompt template with the SFX system prompt."""
    sub = json.load(open(WORKFLOW, encoding="utf-8"))["definitions"]["subgraphs"][0]
    nodes = {n["id"]: n for n in sub["nodes"]}
    prompts = json.loads(nodes[49]["widgets_values"][0])
    return nodes[38]["widgets_values"][0].replace("SYSTEM_PROMPTS", prompts["SFX"])


# Briefs that read better out of the plan's context: one that only says "the same" as the sound before it,
# and low rumbles, which came back all but silent (2026-10-15: every airframe-buffet take), asked for loud.
BRIEFS = {
    "servo-stall": "A small electric motor straining against a jam: a higher, labouring whine with a rhythmic knock.",
    "suit-breathing-hard": "Fast, heavy, labouring breaths of an astronaut in trouble, close up inside a space suit's "
                           "helmet, with the hiss of its oxygen flow.",
    "airframe-buffet": "Loud, close turbulence inside an aircraft's cabin: metal panels and fittings rattling hard, "
                       "the airframe shaking, heavy thuds.",
    "runway-rumble": "Loud, close: heavy aircraft tyres rolling fast on a concrete runway, a deep rumble and the "
                     "rhythmic thump of the joints.",
}


def brief(sound):
    """What Stable Audio is asked for: the plan's first sentence (or BRIEFS'), its own Stable Audio hint if
    it has one, and whether it's a single sound or a loop."""
    kind, _, length, _, _, hear, where = audio_cues.LIB[sound]
    first = BRIEFS.get(sound) or re.split(r"(?<=[.!?])\s", hear, 1)[0]
    text = first
    hint = re.search(r"Stable Audio: '([^']+)'", where)
    if hint:
        text += " " + hint.group(1)[0].upper() + hint.group(1)[1:] + "."
    if kind == "loop":
        text += " A steady, continuous sound at a constant level, no fade in or out, so it can be looped."
    else:
        text += f" One single isolated sound about {length.replace(' ', '')} long, then silence."
    return text


def duration(sound):
    kind, _, length = audio_cues.LIB[sound][:3]
    upper = float(re.findall(r"[\d.]+", length)[-1])
    if kind == "loop":
        return float(min(24, max(8, math.ceil(upper * 2))))
    return float(max(2, math.ceil(upper * 1.5 + 0.5)))


def slug(text):
    return re.sub(r"_+", "_", re.sub(r"[^a-z0-9]+", "_", text.lower().replace("'", ""))).strip("_")


def label(sound, seed):
    """The review copy's name: the sound, its kind and length, and what you should hear."""
    kind, _, length, _, _, hear, _ = audio_cues.LIB[sound]
    first = re.split(r"(?<=[.!?])\s", hear, 1)[0].rstrip(".")
    what = first.split(": ", 1)[-1]
    what = re.sub(r"[^A-Za-z0-9,' ()-]+", " ", what).strip()
    if len(what) > 90:
        what = what[:90].rsplit(" ", 1)[0]
    return f"{sound} - {kind} {length.replace(' loop', '')} - {what} (s{seed})"


def workflow(sound, seed, prompt):
    dur = duration(sound)
    text = prompt.replace("USER_INPUT", brief(sound)).replace("AUDIO_LENGTH", str(int(dur)))
    # Qwen's expansion, or (raw) the brief as Qwen would end it.
    positive = brief(sound) + f" Length: {int(dur)} seconds" if VARIANT == "raw" else ["28", 0]
    flow = {
        "25": {"class_type": "CheckpointLoaderSimple", "inputs": {"ckpt_name": "stable_audio_3_medium_base.safetensors"}},
        "26": {"class_type": "CLIPLoader", "inputs": {"clip_name": "t5gemma_b_b_ul2.safetensors", "type": "stable_audio", "device": "default"}},
        "29": {"class_type": "CLIPLoader", "inputs": {"clip_name": "qwen3.5_2b_bf16.safetensors", "type": "stable_diffusion", "device": "default"}},
        "28": {"class_type": "TextGenerate", "inputs": {
            "clip": ["29", 0], "prompt": text, "max_length": 256, "sampling_mode": "on",
            "sampling_mode.temperature": 0.7, "sampling_mode.top_k": 64, "sampling_mode.top_p": 0.95,
            "sampling_mode.min_p": 0.05, "sampling_mode.repetition_penalty": 1.05, "sampling_mode.seed": 0,
            "sampling_mode.presence_penalty": 0, "thinking": False, "use_default_template": True, "mtp": "auto"}},
        "54": {"class_type": "PreviewAny", "inputs": {"source": ["28", 0]}},
        "6": {"class_type": "CLIPTextEncode", "inputs": {"clip": ["26", 0], "text": positive}},
        "7": {"class_type": "CLIPTextEncode", "inputs": {"clip": ["26", 0], "text": NEGATIVE if VARIANT else ""}},
        "11": {"class_type": "EmptyLatentAudio", "inputs": {"seconds": dur, "batch_size": 1}},
        "3": {"class_type": "KSampler", "inputs": {
            "model": ["25", 0], "positive": ["6", 0], "negative": ["7", 0], "latent_image": ["11", 0], "seed": seed,
            "steps": 50, "cfg": 7, "sampler_name": "lcm", "scheduler": "simple", "denoise": 1}},
        "12": {"class_type": "VAEDecodeAudio", "inputs": {"samples": ["3", 0], "vae": ["25", 2]}},
        "57": {"class_type": "SaveAudioAdvanced", "inputs": {
            "audio": ["12", 0], "filename_prefix": f"{PREFIX}/{sound}_s{seed}", "format": "flac"}},
    }
    if VARIANT == "raw":
        for node in ("28", "29", "54"):
            del flow[node]
    return flow


def post(path, body=None):
    req = urllib.request.Request(URL + path, json.dumps(body or {}).encode(), {"Content-Type": "application/json"})
    return json.load(urllib.request.urlopen(req)) if path == "/prompt" else urllib.request.urlopen(req)


def levels(path):
    """Peak and RMS in dBFS and the share of samples at full scale (Stable Audio normalises to 0 dB, so
    a peak there alone is no fault), or None if it can't be read (pip install soundfile)."""
    try:
        import numpy as np
        import soundfile as sf
        data, _ = sf.read(path, always_2d=True)
        peak = float(np.max(np.abs(data))) or 1e-9
        rms = float(np.sqrt(np.mean(data ** 2))) or 1e-9
        return 20 * math.log10(peak), 20 * math.log10(rms), float(np.mean(np.abs(data) >= 0.999))
    except Exception:
        return None


def prefix(lv):
    """"silent " for a take with next to nothing in it, "clipped " for one flat at full scale for more than
    0.5 % of its samples (a few short runs there are Stable Audio's normalising, not a fault), else ""."""
    if lv is None:
        return ""
    return "silent " if lv[0] < -30 else "clipped " if lv[2] > 0.005 else ""


def main():
    sounds = [s for s, v in audio_cues.LIB.items() if v[1] == "new"]
    only = [a[len("--only="):] for a in sys.argv if a.startswith("--only=")]
    if only:
        sounds = [s for s in sounds if s in only[0].split(",")]
    os.makedirs(REVIEW, exist_ok=True)
    prompt = template()
    jobs = [(s, seed) for s in sounds for seed in SEEDS
            if not glob.glob(os.path.join(OUTPUT, f"{s}_s{seed}_*.flac"))]
    pending = {}
    for s, seed in jobs:
        pending[(s, seed)] = post("/prompt", {"prompt": workflow(s, seed, prompt)})["prompt_id"]
    print(f"queued {len(jobs)} takes of {len(sounds)} sounds", flush=True)
    prompts_path = os.path.join(REVIEW, "sfx-prompts.json")
    prompts = json.load(open(prompts_path, encoding="utf-8")) if os.path.exists(prompts_path) else {}
    qc = []
    while pending:
        time.sleep(5)
        for key, pid in list(pending.items()):
            hist = json.load(urllib.request.urlopen(f"{URL}/history/{pid}"))
            if pid not in hist:
                continue
            del pending[key]
            s, seed = key
            entry = hist[pid]
            if entry.get("status", {}).get("status_str") == "error":
                print("FAILED", s, seed, flush=True)
                continue
            text = entry.get("outputs", {}).get("54", {}).get("text")
            files = glob.glob(os.path.join(OUTPUT, f"{s}_s{seed}_*.flac"))
            if not files:
                print("NO FILE", s, seed, flush=True)
                continue
            name = label(s, seed)
            lv = levels(files[0])
            dest = os.path.join(REVIEW, prefix(lv) + name + ".flac")
            shutil.copy(files[0], dest)
            prompts[os.path.basename(dest)] = {"asked": brief(s), "expanded": text[0] if isinstance(text, list) else text,
                                               "seconds": duration(s)}
            json.dump(prompts, open(prompts_path, "w", encoding="utf-8"), indent=1, ensure_ascii=False)
            print(f"{len(pending):3d} left  {os.path.basename(dest)}", flush=True)
    # The levels of every take still in review/, its prefix brought up to date.
    for path in sorted(glob.glob(os.path.join(REVIEW, "*.flac"))):
        lv = levels(path)
        bare = re.sub(r"^(check|silent|clipped) ", "", os.path.basename(path))
        fixed = os.path.join(REVIEW, prefix(lv) + bare)
        if fixed != path:
            os.replace(path, fixed)
            if os.path.basename(path) in prompts:
                prompts[os.path.basename(fixed)] = prompts.pop(os.path.basename(path))
            path = fixed
        qc.append(f"{os.path.basename(path)}\tpeak {lv[0]:.1f} dB\trms {lv[1]:.1f} dB\tfull scale {100 * lv[2]:.2f} %"
                  if lv else os.path.basename(path))
    with open(os.path.join(REVIEW, "qc.txt"), "w", encoding="utf-8") as f:
        f.write("\n".join(qc) + "\n")
    json.dump(prompts, open(prompts_path, "w", encoding="utf-8"), indent=1, ensure_ascii=False)
    post("/free", {"unload_models": True, "free_memory": True})
    print("done")


if __name__ == "__main__":
    main()
