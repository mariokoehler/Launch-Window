# Historical audio

Both clips come from NASA's historical sounds, https://www.nasa.gov/historical-sounds/ (added by the
user, 2026-10-12). NASA's recordings are generally not subject to copyright in the United States; credit
NASA when they're used in the game (`assets/CREDITS.md`, once one ships).

- `jfk.mp3`: President Kennedy at Rice University, 12 September 1962, "We choose to go to the Moon".
- `smallStep.mp3`: Neil Armstrong on the Moon, 20 July 1969, "That's one small step for (a) man".

BASPM ships its own cut of the Kennedy excerpt (`KennedyMoonSpeech.ogg`); ours is NASA's copy, not
BASPM's file (checked 2026-10-12: the same archival recording, but NASA's download).

## The Quindar tones (`quindar/`)

Apollo's "beeps" around CAPCOM's transmissions: 250 ms sine tones, 2525 Hz as the mic keyed on (before
the voice) and 2475 Hz as it keyed off (after it). Generated from scratch by `quindar/make_quindar.py`
(2026-10-14, nothing recorded, nothing to credit): a clean pair (`quindar_intro`, `quindar_outro`) and a
pair as heard on the air-to-ground loop (`_radio`: 300-3000 Hz, slightly saturated, a faint hiss), each
as WAV (48 kHz, 16-bit, mono, -6 dBFS) and OGG.

## Radio call-outs (`chatter/`)

The user's (2026-10-14): flight-loop calls generated in ComfyUI with the VibeVoice model, brownian noise
(`brownian_noise.ogg`) mixed under each for the radio and the Quindar tones at both ends. Each file's
name is exactly what's said. Lines that fit the same moment ("go for launch", "all systems go for
launch") are one sound's takes: `chatter/make_radio_sounds.py` publishes them into the replay
(`assets/audio/replay/radio-*`, `replay-sounds.json`), and `assets-raw/replay/cues.json` places them.
`chatter/CALLOUTS.md` has ideas for the lines still missing, the failures above all.

From 2026-10-15 Claude makes takes too, with the user's workflow (`Radio_Callouts.json` in ComfyUI):
`chatter/make_callouts.py` queues every line in `chatter/callouts.json` with four seeds, and
`chatter/check_callouts.py` transcribes each take with Whisper, measures it, and copies those whose
words all came through into ComfyUI's `output/lw_callout/review/`. The user listens there and keeps the
best; the kept takes are renamed into this folder ("(n)" for a second take of a line). End a line with
" …" in `callouts.json`: without the pause VibeVoice often stops mid-word (the check fails a take whose
voice runs to its last sample). A line that opens with "Flight," needs more seeds: the word is often
swallowed.

## Sound effects (`sfx/`)

From 2026-10-15, with the user's workflow (`Sound_Effects.json` in ComfyUI: Stable Audio 3 Medium, the
prompt first expanded by Qwen with its SFX template). `sfx/make_sfx.py` queues every sound the replay's
audio plan (`assets-raw/replay/sounds/audio_cues.py`) still lacks, with four seeds, and copies each take
into ComfyUI's `output/lw_sfx/review/` named for what it should be ("pyro - one-shot 0.2-0.4 s - a
sharp, metallic crack-bang with a short ring, no boom (s1101).flac"), beside the prompts it was given
(`sfx-prompts.json`) and the levels (`qc.txt`; a "silent" or "clipped" prefix marks a take to doubt). Takes run
longer than the plan's length, leaving room to cut: a one-shot is followed by silence, a loop runs 8 s or
more to find a seamless stretch in. The user listens there, keeps the best and cuts the loops in
Audacity; kept takes then go into `replay-sounds.json` (credited) and their cues into `cues.json`.
`--variant=neg` adds a negative prompt against music (the workflow's is empty), `--variant=raw` also
skips Qwen; their takes go to `output/lw_sfx/<variant>/review/` to compare seed for seed.
