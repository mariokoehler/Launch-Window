# Mission step clips (proofs of concept)

Short animated clips for the mission replay's steps, in the explainers' style, made in EffectCraft
and played as WebM through gdx-video (`design.md` §5, Backlog, "Animated mission steps"; the splash
in `assets-raw/splash/` is the same pipeline). Nothing here is in the game yet. Every clip is
1920 × 1080 at 1500 kbps, so they compare; the real size waits for the replay viewer's redesign.
Each folder's `build/` is regenerated on every run and stays out of git.

- **`launch-solo/`**: the launch, success only, by Claude alone (2026-10-12). `make_launch.py`
  draws the layers as SVG and `launch.jsx` animates them with formulas (constant-acceleration
  climb, a camera that holds the rocket, then lets it go).
  `python assets-raw/replay/launch-solo/make_launch.py`
- **`launch-storyboard/`**: the launch in three variants (success, glitch then saved, failure),
  rebuilt from the Claude Design canvas `Launch Window — Mission Steps: Launch`. `project/` is the
  canvas's files as downloaded (each frame a plain SVG with named parts; the notes, with every
  timing, are in `canvas.json`). `extract.py` cuts the parts into layers, `lib.jsx` holds the
  motion helpers, and `success.jsx`, `glitch.jsx` and `failure.jsx` are the timelines.
  `python extract.py`, then `python build.py success|glitch|failure [render]`, from its folder;
  `build/compare_<variant>.png` puts the storyboard beside the render at each frame's time.

**The checklist:** `CLIP_CHECKLIST.md` lists every distinct mission step in the standard pack, one
entry per clip, with what is on screen and how often it flies, and ticks each variant as its clip
lands. `python assets-raw/replay/make_clip_checklist.py` regenerates it (2026-10-13; never edit it
by hand).

**The workflow decided (user, 2026-10-12):** Claude animates the steps alone, as in
`launch-solo/`; Claude Design comes in only for ideas where a step has none yet.
