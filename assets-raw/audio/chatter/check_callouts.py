"""Screens generated call-out takes: python assets-raw/audio/chatter/check_callouts.py

Needs `pip install faster-whisper nvidia-cublas-cu12 nvidia-cudnn-cu12` (Whisper large-v3, about 3 GB on
first use). Run it once ComfyUI is idle: VibeVoice and Whisper don't fit on the GPU together (the CPU
fallback works, slower).

For every take make_callouts.py wrote (lw_callout/*_00001.mp3), it transcribes the finished take and the
bare voice and compares them with the line in callouts.json, then measures the bare voice (length,
leading and trailing silence, the longest pause, peak, clipping). Verdicts:
  PASS   every word heard, confidently, nothing clipped: the finished take (noise, Quindar tones) is copied
         to lw_callout/review/ for the user's ear
  CHECK  every word heard, but some uncertainly, or clipped, or only in the bare voice: copied to review/
         too, marked
  FAIL   a word missing, added or changed, or the voice cut off (speech up to its last sample)
Whisper tidies up mumbles, so a pass says the words are there, not that they're said well; a FAIL is
usually right, and the two transcripts disagreeing is a good sign of a garbled take. Spectrograms of
each take go to lw_callout/qc/ (the bursts of speech and the Quindar tones show; a mumble has no high
frequencies). How a take sounds stays the user's call. A take is checked once (qc/report.json keeps
it), so takes the user deleted from review/ stay deleted; delete the report to check everything again.
"""
import difflib
import glob
import importlib.util
import json
import os
import re
import shutil
import sys

import numpy as np

for pkg in ("nvidia.cublas", "nvidia.cudnn"):  # pip's CUDA wheels: put their DLLs where ctranslate2 finds them
    spec = importlib.util.find_spec(pkg)
    if spec:
        d = os.path.join(spec.submodule_search_locations[0], "bin")
        os.add_dll_directory(d)
        os.environ["PATH"] = d + os.pathsep + os.environ["PATH"]
from faster_whisper import WhisperModel, decode_audio  # noqa: E402

from make_callouts import LINES, OUTPUT, slug  # noqa: E402

REVIEW = os.path.join(OUTPUT, "review")
QC = os.path.join(OUTPUT, "qc")
EXPECTED = {slug(line["say"]): line.get("hear", line["say"]) for line in LINES}
PROMPT = "Flight, EECOM, FIDO, CAPCOM. Telemetry, acquisition of signal, program alarm, drogue, abort, SCE to Aux, downlink."
SAME = {"we've": "we have", "we're": "we are", "it's": "it is", "he's": "he is", "ok": "okay",
        "60": "sixty", "30": "thirty", "2": "two", "break": "brake", "standby": "stand by",
        "cut-off": "cutoff"}


def words(text):
    text = text.lower().replace("’", "'")
    out = []
    for w in re.findall(r"[a-z0-9'-]+", text):
        out.extend(SAME.get(w, w).split())
    return out


def load_model():
    try:
        return WhisperModel("large-v3", device="cuda", compute_type="float16")
    except Exception as e:  # the GPU is still busy
        print("CPU fallback:", e, flush=True)
        return WhisperModel("large-v3", device="cpu", compute_type="int8")


def transcribe(model, path):
    segments, _ = model.transcribe(path, language="en", beam_size=5, word_timestamps=True, initial_prompt=PROMPT,
                                   vad_filter=False, condition_on_previous_text=False)
    segments = list(segments)
    return " ".join(s.text.strip() for s in segments), [w for s in segments for w in (s.words or [])]


def metrics(audio, rate=24000):
    frame = rate // 50
    n = len(audio) // frame
    level = 20 * np.log10(np.sqrt(np.mean(audio[:n * frame].reshape(n, frame) ** 2, axis=1) + 1e-12))
    loud = np.flatnonzero(level > level.max() - 35)
    gap = run = 0
    for v in level[loud[0]:loud[-1] + 1] > level.max() - 35:
        run = 0 if v else run + 1
        gap = max(gap, run)
    return dict(seconds=round(len(audio) / rate, 2), lead=round(loud[0] / 50, 2), trail=round((n - 1 - loud[-1]) / 50, 2),
                pause=round(gap / 50, 2), peak_db=round(float(20 * np.log10(np.max(np.abs(audio)) + 1e-9)), 1),
                clipped=int(np.sum(np.abs(audio) > 0.99)))


def spectrogram(audio, path):
    from PIL import Image
    frames = np.lib.stride_tricks.sliding_window_view(audio, 1024)[::256] * np.hanning(1024)
    s = 20 * np.log10(np.abs(np.fft.rfft(frames, axis=1)).T + 1e-6)
    img = (np.clip((s - (s.max() - 80)) / 80, 0, 1)[::-1] * 255).astype(np.uint8)
    Image.fromarray(img).resize((min(1600, img.shape[1] * 2), 300)).save(path)


def main():
    os.makedirs(REVIEW, exist_ok=True)
    os.makedirs(QC, exist_ok=True)
    model = load_model()
    path = os.path.join(QC, "report.json")
    report = json.load(open(path, encoding="utf-8")) if os.path.exists(path) else []
    seen = {r["take"] for r in report}  # checked before: never copied to review/ again, so deletions stick
    for take in sorted(glob.glob(os.path.join(OUTPUT, "*_00001.mp3"))):
        name = os.path.basename(take)[:-len("_00001.mp3")]
        want = EXPECTED.get(re.sub(r"_s\d+$", "", name))
        clean = os.path.join(OUTPUT, "clean", name + "_00001.flac")
        if not want or not os.path.exists(clean) or name in seen:
            continue
        heard, heard_words = transcribe(model, take)
        heard_clean, _ = transcribe(model, clean)
        ops = difflib.SequenceMatcher(a=words(want), b=words(heard)).get_opcodes()
        errors = sum(max(i2 - i1, j2 - j1) for op, i1, i2, j1, j2 in ops if op != "equal")
        clean_right = words(heard_clean) == words(want)
        unsure = [(w.word.strip(), round(w.probability, 2)) for w in heard_words if w.probability < 0.6]
        voice = decode_audio(clean, sampling_rate=24000)
        m = metrics(voice)
        spectrogram(decode_audio(take, sampling_rate=24000), os.path.join(QC, name + ".png"))
        if m["trail"] < 0.03:  # speech to the last sample: VibeVoice stopped mid-word
            verdict = "FAIL"
        elif errors:  # the bare voice heard right means the noise fooled Whisper, not the voice: a CHECK
            verdict = "CHECK" if clean_right else "FAIL"
        else:
            verdict = "CHECK" if unsure or m["clipped"] else "PASS"
        if verdict != "FAIL":
            shutil.copyfile(take, os.path.join(REVIEW, ("" if verdict == "PASS" else "check_") + name + ".mp3"))
        report.append(dict(take=name, verdict=verdict, want=want, heard=heard, heard_clean=heard_clean,
                           errors=errors, unsure=unsure, **m))
        print(f"{verdict:5} {name:72} {m['seconds']:5}s | {heard}"
              + (f" | clean: {heard_clean}" if words(heard_clean) != words(heard) else "")
              + (f" | unsure: {unsure}" if unsure else ""), flush=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(report, f, indent=1, ensure_ascii=False)
    counts = {v: sum(r["verdict"] == v for r in report) for v in ("PASS", "CHECK", "FAIL")}
    print(counts)


if __name__ == "__main__":
    main()
