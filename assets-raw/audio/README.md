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
