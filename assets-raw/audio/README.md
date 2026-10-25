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

## The hands-on puzzle's sounds (2026-10-25)

The user's Pixabay finds, each with a `.txt` beside it naming its page and uploader (credited in
`assets/CREDITS.md`), for a round of the hands-on puzzle resolving command by command (`ui-spec.md` component 26):

- `tense_loop.ogg`: under the round while its commands resolve, looping; fades out after the last.
- `failure.ogg`, `success.ogg`, `exceptional.ogg`: on each command's result as its needle settles (a normal
  result plays `success`).

`make_handson_sounds.py` publishes them (level raised, as OGG at 44.1 kHz) into `assets/audio/handson/`.

## Sound effects: not generated

Tried on 2026-10-15 with the user's Stable Audio 3 workflow (`Sound_Effects.json` in ComfyUI): 59 sounds of
the replay's audio plan, four seeds each. Almost none were usable (the user's verdict), so the takes and the
script were deleted (it's in git, b0680d2). The user picks curated clips (Pixabay) for a few impactful moments
instead of a sound under every action.

## Sound effects for the clips (2026-10-21)

The user's Pixabay finds, each with a `.txt` beside it naming its page and uploader (credited in
`assets/CREDITS.md`). A name ending in `_loop` loops seamlessly. The user's notes on where each goes:

- `short_puff.ogg`: an RCS thruster's puff (`rcs-puff`).
- `puff_followed_by_hiss.ogg`: a hatch opening on a spacewalk (`hatch-open`).
- `pump_loop.ogg`: under the rocket on the pad while it's loaded with propellant (`fueling`).
- `hissing_venting_loop.ogg`: after the propellant is loaded, the rocket waiting for the countdown and the
  launch (`vent`).
- `reentry_air_rush_loop.ogg`: the reentry clips, once the plasma shows (`reentry-plasma`).
- `water_splash.ogg`: a capsule touching down in the water (`splash`).
- `helicopter_loop.ogg`: the recovery clips with a helicopter (`helicopter`).
- `explosion.ogg`: whenever something explodes (`explosion`).

Four more, the user's own, made with Stable Audio 3 (no `.txt`):

- `decoupler.ogg`: two things separating, as at a stage separation (`decoupler`).
- `explosion_with_debris.ogg`: a blast with debris hitting the ground, for blasts near the ground, not in space
  (`explosion-debris`).
- `servomotor.ogg`: something mechanical moving, as a robot arm (`servo`).
- `deploy_solar_panel.ogg`: something folded unfolding: solar panels, antennas, landing legs (`deploy`).

`assets-raw/replay/sounds/make_effect_sounds.py` publishes them (level raised, as OGG) into the replay's
catalog, and `assets-raw/replay/effects.py` times their cues off the clips' built projects.
