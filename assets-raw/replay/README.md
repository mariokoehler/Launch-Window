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
  `lib.jsx` keeps only the launch's scene; the motion helpers are in `../motion.jsx`, shared.
- **`steps/`**: the other steps of a first satellite (Sputnik 1, Explorer 1), in three variants
  each (2026-10-13): `launch-preparations`, `countdown`, `ascent`, `deployment`, `earth-orbit`.
  `parts.py` draws their layers and copies in the storyboard's (the pad steps are the launch's own
  scene, and the rocket in flight is its rocket cut at the stages; `python parts.py d- o-` redraws
  only the parts so named, as a full run takes 2 minutes). `pad.jsx` holds the pad's propellant,
  leak, fire, clock and explosion; each step's `<step>.jsx` its three variants. From its folder,
  `python build.py <step>` writes `build/sheet_<step>.png` (the variants side by side, a row per
  moment), and `publish` writes the game's copies and sidecars. Every step is generic: it plays
  for every program, so the payload is the rocket's own nose section.
  - **The way home** (2026-10-14): `reentry-preparations`, `retrofire`, `retrofire-jettison`, `reentry`
    and `descent-and-splashdown`, for a capsule (`parts.py`'s `r-` parts, `python parts.py r-`). The
    capsule is the same nose section, twice the launch's size, centred in every clip: the world moves
    instead (the Earth's limb comes up as it sinks, the clouds and then the sea as it falls). In orbit it
    hangs off `r-carrier` (0 = nose right, its way; 180 = base first, for the burn), and it's nose up
    for the descent (`r-capsule-up`, the carrier at 270, which is how reentry ends). The retro pack sits
    on its base until the jettison. They're built for the orbital order; Mercury's suborbital flight
    fires its retros *before* the preparations, so its cuts don't match there, and Vostok has
    `modules-separation` instead of the jettison. The Earth's turn carries on from clip to clip (the
    constants at the top of each `d-earth` turn).
  - **Vostok's way** (2026-10-14): `modules-separation` (the module is the same pack, `r-module`,
    turning about its own centre; the cable between them, `r-cable`, is aimed and stretched every
    frame; its glitch is Vostok 1's) and `descent-and-landing`. The two descents share `descent.jsx`,
    which `build.py` appends after the step's own file (`"after"`), where `GROUND` picks the sea or
    the ground.
  - **Time in orbit and the suborbital hop** (2026-10-14, `parts.py`'s `x-` parts):
    `advanced-experiments-earth-orbit`, `biometric-data-transmission`, `duration-level-i` and `-ii`
    (one timeline, `duration.jsx`, at two levels; several days begin where a day ends), and
    `suborbital-flight` (the capsule rides a dashed parabola up to its top, turning base first, and
    ends level as the retrofire begins; only the dashed line changes at that cut). The orbit steps
    share `orbit.jsx` (`"before"`): the Earth-orbit scene, starting where its success ends, with the
    radio rings, gauges, and `intro(true)` to fade a step's own overlays in when it cuts in from the
    Earth orbit. The heartbeat strip is a sweep monitor (a head rewrites one beat a slot), so it needs
    no mask.
  - **Recovery** (2026-10-14, `parts.py`'s `y-` parts): on from the splashdown's last frame (its
    camera 200 px down), a helicopter hoists the capsule out of the sea. Its sea is in front of the
    capsule (`y-sea-front`), so a capsule that floods and goes under (the failure, Liberty Bell 7)
    shows through the water.
  - **The way to the planets** (2026-10-14, `parts.py`'s `m-` parts): `tpi` (in the Earth-orbit
    scene, the camera up and left so the probe can leave to the right) and the cruises, which share
    `cruise.jsx` (`FROM`, `TO`, `ARC` set by each step's file): a map of the planets round the Sun
    (orbit radii spread to fit, not to scale; the asteroid belt dotted), the transfer orbit a half
    ellipse from `START_ANGLE` (parts.py and cruise.jsx compute the same curve), the probe sweeping
    equal areas. The planets pivot on their own centres and move round, so they stay upright.
  - **At the planets and the Sun** (2026-10-14, `p-` and `s-` parts): `venus-orbit` and `mars-orbit`
    share `planet-orbit.jsx` (`PLANET`), the orbit scene with `orbitScene("p-venus")` and a mapping
    beam; `sun-measurements` has its own scene, the Sun big at the left and the wind sent out along
    its rays.
  - **The Moon** (2026-10-14, `p-moon`, `l-` parts): the orbit scene round the Moon, the Earth small
    at the left and the Moon's radio shadow (`l-shadow`, `inShadow` in `orbit.jsx`) where no rings
    are sent. `lunar-orbit` is `planet-orbit.jsx` with `PLANET = "moon"`; `loi` integrates its open
    and closed paths (conics with their low point behind the Moon) so the burn falls in the shadow;
    `tei` leaves straight, as `tpi` does; `lunar-impact` dives at the near edge. `lunar-approach`
    zooms the world in about the Moon (the world null's scale) and ends on `loi`'s first frame (the
    open path's pace, `H_OPEN`, is shared); `lunar-flyby` is the same path seen from further out.
    The coasts share `moon-coast.jsx` (`LEG`), whose paths `parts.py` stores in `layers.json`
    (`_paths`) so the timeline follows the drawn line; `earth-approach` has the entry corridor
    (`l-corridor`).
  - **The way down in two steps** (2026-10-14): `descent-chutes` (the `descent` step's clip, a rule
    maps it) is `descent.jsx` with `CHUTES`, ending under the main as the clouds clear;
    `touchdown.jsx` (`GROUND`) carries on from that frame exactly, for `landing-and-recovery` and
    `splashdown-and-recovery`. `insertion` is in the Earth-orbit scene.
  - **Rendezvous and docking** (2026-10-14): `rendezvous-target-approach` is in the Earth-orbit
    scene with a second payload (`o-sat-b`). The rest share `dock.jsx`, a close-up over the way home's
    limb: the chaser (`k-carrier` with the capsule and its jets) is placed by offsets from docked, the
    target (`k-target`, its light and latches) or another capsule (`k-other`) ahead of it.
    `dockScene(other, t0)`: a step that carries on from another passes that one's length as `t0`, so
    the Earth's turn and the light's blinking carry on (`docking` from `target-approach`).
  - **Checks** (`check.py`, after `build.py`): `python check.py spoilers <step> ...` says where the
    variants first differ before their reveals (glitch and failure share their first sign of trouble,
    so every pair should part only at the moment that decides it), and `python check.py handoffs
    <step> <next step> ...` compares each success and glitch's last frame with the next step's first
    (what's left should be one frame's motion).
  - **Continuity:** the steps cut straight into each other, so a step's success and glitch end on
    the same frame, and the pad steps end on the next one's first (launch preparations on the
    countdown's, the countdown on the launch's). The venting loops every 0.8 s, so a pad step's
    success and glitch last a multiple of 0.8 s.

**Sound cues:** `cues.json` holds every clip's cues (the sounds are `assets/content/replay-sounds.json`'s),
and `python assets-raw/replay/cues.py` writes them into the sidecars; both builders run it on publish,
so a cue changes without a render. Before a clip's earliest reveal its variants must sound the same
(`ReplayClipsTest`).

**The checklist:** `CLIP_CHECKLIST.md` lists every distinct mission step in the standard pack, one
entry per clip, with what is on screen and how often it flies, and ticks each variant as its clip
lands. `python assets-raw/replay/make_clip_checklist.py` regenerates it (2026-10-13; never edit it
by hand).

**The workflow decided (user, 2026-10-12):** Claude animates the steps alone, as in
`launch-solo/`; Claude Design comes in only for ideas where a step has none yet.
