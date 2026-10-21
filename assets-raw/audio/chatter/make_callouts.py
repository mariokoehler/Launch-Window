"""Generates radio call-out takes in ComfyUI: python assets-raw/audio/chatter/make_callouts.py [seeds] [--retry]

Queues the user's Radio_Callouts workflow (VibeVoice-Large cloning mc_sample2.ogg, brownian noise under
it, the Quindar tones at both ends) for every line in callouts.json, one take per seed (default 1101,
2202, 3303, 4404). ComfyUI writes the finished take to output/lw_callout/<line>_s<seed>_00001.mp3 and the
bare voice to output/lw_callout/clean/ (FLAC, for the check). Takes already there are skipped, so a rerun
fills gaps; --lines=<file> reads another lines file (round2.json, the second round's pools; give
check_callouts.py the same); --retry takes only the lines with fewer than two takes that
passed the last check (qc/report.json), for more seeds; --only=<part of a line's file name> takes one line. The batch's last job unloads VibeVoice, so check_callouts.py's Whisper fits on the GPU.

A line's "say" is what VibeVoice reads ("S-C-E" spells it out); "hear", if given, is what the check
expects in the transcript. Next: check_callouts.py, then the user listens to review/ and keeps the best.
"""
import glob
import json
import os
import re
import sys
import time
import urllib.request

URL = "http://127.0.0.1:8188"
OUTPUT = r"E:\Comfy-Desktop\ComfyUI-Shared\output\lw_callout"
HERE = os.path.dirname(os.path.abspath(__file__))
LINES_FILE = next((a[len("--lines="):] for a in sys.argv if a.startswith("--lines=")), "callouts.json")


def load_lines(name):
    """A lines file: a list of lines (callouts.json), or round 2's pools, each with its lines (round2.json)."""
    data = json.load(open(os.path.join(HERE, name), encoding="utf-8"))
    return data if isinstance(data, list) else [line for pool in data["pools"] for line in pool["lines"]]


LINES = load_lines(LINES_FILE)
ARGS = [a for a in sys.argv[1:] if not a.startswith("--")]
SEEDS = [int(s) for s in ARGS[0].split(",")] if ARGS else [1101, 2202, 3303, 4404]


def slug(text):
    text = text.replace("S-C-E", "SCE").lower().replace("'", "")
    return re.sub(r"_+", "_", re.sub(r"[^a-z0-9]+", "_", text)).strip("_")


def workflow(text, seed, name, last):
    return {
        "15": {"class_type": "LoadAudio", "inputs": {"audio": "mc_sample2.ogg"}},
        "44": {"class_type": "VibeVoiceSingleSpeakerNode", "inputs": {
            "voice_to_clone": ["15", 0], "text": text, "model": "VibeVoice-Large", "attention_type": "auto",
            "quantize_llm": "full precision", "free_memory_after_generate": last, "diffusion_steps": 20,
            "seed": seed, "cfg_scale": 1.3, "use_sampling": False, "temperature": 0.95, "top_p": 0.95,
            "max_words_per_chunk": 250, "voice_speed_factor": 1.0}},
        "47": {"class_type": "LoadAudio", "inputs": {"audio": "brownian_noise.ogg"}},
        "46": {"class_type": "AudioMerge", "inputs": {"audio1": ["44", 0], "audio2": ["47", 0], "merge_method": "add"}},
        "49": {"class_type": "LoadAudio", "inputs": {"audio": "quindar_intro_radio.ogg"}},
        "54": {"class_type": "AudioAdjustVolume", "inputs": {"audio": ["49", 0], "volume": -15}},
        "50": {"class_type": "AudioConcat", "inputs": {"audio1": ["54", 0], "audio2": ["46", 0], "direction": "after"}},
        "51": {"class_type": "LoadAudio", "inputs": {"audio": "quindar_outro_radio.ogg"}},
        "55": {"class_type": "AudioAdjustVolume", "inputs": {"audio": ["51", 0], "volume": -15}},
        "52": {"class_type": "AudioConcat", "inputs": {"audio1": ["50", 0], "audio2": ["55", 0], "direction": "after"}},
        "45": {"class_type": "SaveAudioAdvanced", "inputs": {"audio": ["52", 0],
            "filename_prefix": f"lw_callout/{name}", "format": "mp3", "format.quality": "V0"}},
        "60": {"class_type": "SaveAudioAdvanced", "inputs": {"audio": ["44", 0],
            "filename_prefix": f"lw_callout/clean/{name}", "format": "flac"}},
    }


def post(path, body):
    req = urllib.request.Request(URL + path, json.dumps(body).encode(), {"Content-Type": "application/json"})
    return json.load(urllib.request.urlopen(req)) if path == "/prompt" else urllib.request.urlopen(req)


def main():
    lines = LINES
    if "--retry" in sys.argv:
        report = json.load(open(os.path.join(OUTPUT, "qc", "report.json"), encoding="utf-8"))
        good = {}
        for r in report:
            key = re.sub(r"_s\d+$", "", r["take"])
            good[key] = good.get(key, 0) + (r["verdict"] != "FAIL")
        lines = [line for line in LINES if good.get(slug(line["say"]), 0) < 2]
        print("retrying:", [line["say"] for line in lines])
    only = [a[len("--only="):] for a in sys.argv if a.startswith("--only=")]
    if only:
        lines = [line for line in lines if only[0] in slug(line["say"])]
    jobs = [(line["say"], seed, f"{slug(line['say'])}_s{seed}") for line in lines for seed in SEEDS]
    jobs = [j for j in jobs if not glob.glob(os.path.join(OUTPUT, j[2] + "_*.mp3"))]
    pending = {}
    for i, (text, seed, name) in enumerate(jobs):
        pending[name] = post("/prompt", {"prompt": workflow(text, seed, name, i == len(jobs) - 1)})["prompt_id"]
    print(f"queued {len(jobs)} takes", flush=True)
    wait(pending)


def wait(pending):
    """Waits for the queued takes (name: prompt id) to leave ComfyUI's queue, then unloads its models. One
    request a cycle: asking for every prompt's history each time ran out of local ports on a big batch."""
    while pending:
        time.sleep(15)
        queue = json.load(urllib.request.urlopen(f"{URL}/queue"))
        queued = {job[1] for job in queue["queue_running"] + queue["queue_pending"]}
        for name, pid in list(pending.items()):
            if pid not in queued:
                print("done", name, flush=True)
                del pending[name]
    post("/free", {"unload_models": True, "free_memory": True})


if __name__ == "__main__":
    main()
