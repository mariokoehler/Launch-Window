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
  - **Which way it goes** (2026-10-16): the Earth orbit's map goes round anticlockwise, so over the top
    of the Earth the craft travels left. The way-home clips are drawn travelling right and published
    mirrored (`"mirror"` in `build.py`: the render goes through ffmpeg's `hflip`, the sheet through
    PIL's flip, since EffectCraft drops the turning Earth under a parent scaled by -100; a screen layer
    that must stay unflipped calls `unflip`, as `reentry`'s clouds do, for the unmirrored descents). Mirrored:
    `reentry-preparations`, `retrofire`, `deorbit-burn`, `retrofire-jettison`, `modules-separation`,
    `reentry` and `suborbital-flight` (so the cut from `deployment`, which goes right, flips: unconfirmed).
    `reentry-from-moon` is `reentry` unmirrored (`"source"`), for the step after `earth-approach`, which
    comes in from the left. Faint motes (`motes.jsx`, `r-mote-` parts) stream past against the craft's
    way in the orbit clips, and slow with the retrofire. `deorbit-burn` is the retrofire with the turn
    first (`TURN` in `retrofire.jsx`, everything 1.2 s later), for a burn no reentry preparations came
    before (Biosatellite's): rules in `replay-clips.json` pick it.
  - **A Soyuz-type craft** (2026-10-16): its orbital module (`r-om`, a sphere with a docking probe,
    latched at two points) rides on the nose through `deorbit-burn-om` (`OM` in `retrofire.jsx`; it first
    shows there, the map before it a dot) and `retrofire-jettison-om` (`OM`, `EARTH0`), and goes in
    `orbital-module-separation`, the step after the jettison, leaving the bare capsule the reentry shows.
    Rules pick them by the step before the burn (only these flights' burns follow those steps) and by
    `earlier`. Gemini EOR's lander tests fly the same steps, so they get the module too (unconfirmed).
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
    `splashdown-and-recovery`. `insertion` is in the Earth-orbit scene: since 2026-10-16 the craft
    coasts up its arc to the top on the planned orbit, slowing (its pace from angular momentum kept),
    and burns there to circularize; the glitch sags below the orbit until the relight, the failure falls
    back and burns up.
  - **Rendezvous and docking** (2026-10-14): `rendezvous-target-approach` is in the Earth-orbit
    scene with a second payload (`o-sat-b`). The rest share `dock.jsx`, a close-up over the way home's
    limb: the chaser (`k-carrier` with the capsule and its jets) is placed by offsets from docked, the
    target (`k-target`, its light and latches) or another capsule (`k-other`) ahead of it.
    `dockScene(other, t0)`: a step that carries on from another passes that one's length as `t0`, so
    the Earth's turn and the light's blinking carry on (`docking` from `target-approach`).
  - **Spacewalks** (2026-10-14): `spacewalk.jsx`, the way home's capsule (`r-capsule`) with a hatch,
    the figure (`e-suit`, with its fogged visor and gun) placed by a pose `[dx, dy, turn]` from where
    it stands in the hatch (`handOn` puts its hand on a point), its tether aimed and stretched from
    the hatch every frame. The camera zooms in on the world null (1.35, or 1.2 for the transfer's two
    craft). `eva` and `crew-transfer` (`spacewalkScene(true)`: a second capsule, a collar, a rail).
  - **On the Moon** (2026-10-14): `moon.jsx`, the lander (`n-carrier` with the descent and ascent
    stages and their plumes) placed by offsets from where it stands on the surface, and a lamp panel
    fixed to the screen (`lamp("alarm" | "fuel", lit, blinking)`). `powered-descent` over the limb
    (`n-limb`, the lander at 0.7), `lunar-landing` close over the surface (`n-ground`, `n-rocks`).
    `lunar-landing` is the clip of `landing` after `powered-descent` or `lunar-approach`: a rule's
    `"after"` (the checklist lists such a step's clips by the step before, and ticks it only when all
    have them). `moonwalk` zooms in twice on the ladder (the figure at 0.45, placed by where its feet
    are), `lunar-liftoff` moves the ascent stage off its carrier, and `lunar-ascent` is in the orbit
    scene round the Moon (`orbitScene("p-moon")`), from the surface up.
  - **Apollo's transposition and extraction** (2026-10-14): `apollo.jsx` (after `dock.jsx` and
    `moon.jsx`, whose chaser and lander it uses): the third stage (`t-stage`), the lander in its adapter
    (the lander's carrier turned -90 and at 0.42), the adapter's panels, the capsule placed by offsets
    from docked with the lander's hatch (`DOCKED`).
  - **The runway landing** (2026-10-14): `runway-landing.jsx` (after `moon.jsx`, for the dust), the
    rocket plane (`w-plane`, its skid `w-skid` folding up in the failure) placed by where its skid
    touches, on a dry lake (`w-ground`), the camera at 1.35.
  - **The planets** (2026-10-14): `lunar-approach.jsx`, `lunar-flyby.jsx` and `loi.jsx` read `PLANET`
    (the Moon if unset; only the Moon has the radio shadow and the far Earth), so a planet's step is a
    one-line file (`var PLANET = "saturn";`) with the Moon's file after it. `p-jupiter`, `p-saturn`
    (its ring drawn in two halves round the globe) and `p-mercury` are new.
  - **Probe landers** (2026-10-14): `probe.jsx` (after `orbit.jsx` for the rings, `moon.jsx` for the
    ground): the lander (`v-lander`, its mast and arm) placed by offsets from where it stands;
    `probeCamera()` zooms in, and must come after every `put()`: `put` parents a part to the world after
    placing it, so a part put once the world is scaled lands in the scaled space, off where it belongs.
    `probe-release` is the bus (`v-bus`) and its lander (`v-pod`) alone.
  - **The carrier's entry** (2026-10-15, `parts.py`'s `b-` parts): `bus-entry`, Venera 7's carrier
    after its lander has gone, falling into Venus's air (`b-limb`, settling just under it) under a bow
    of hot air (`b-glow`); its panels tear off at 3.3 and the body bursts at 3.9 in every variant (the
    carrier was meant to burn up). The variants differ in its radio rings, as the Earth orbit's do.
  - **A lander's entry** (2026-10-15, `e-` parts): `planet-entry.jsx` (`PLANET`, set by the one-line
    `venus-entry.jsx` or `mars-entry.jsx` first), the reentry's story at Venus (`b-limb`, Venera's
    sphere `v-pod` at twice its size) or Mars (`e-limb-mars`, the aeroshell `e-shell`), with a bow of
    hot air (`e-glow`) that turns with the lander.
  - **A lander's descent** (2026-10-15, `y-band`, `y-rip` and the `m-back`/`m-heat`/`m-lander`/`m-leg`/
    `m-flame`/`m-ground` parts): `planet-descent.jsx` (`PLANET`, set by `venus-descent.jsx` or
    `mars-descent.jsx`). Venus: the sphere at 1.6 (the landing's size) under `r-main-half` (reefed), then
    `r-main`, through the cloud bands; the glitch tears it back to half at 3.2, the failure tears it away.
    Mars: Viking's sequence, the speed lines (`a-flow`) stood on end as dust; the glitch's third engine
    lights late, the failure's never do. The rule picks Mars by `mars-approach` earlier in the season.
  - **The landing on Mars** (2026-10-15, `m-stones`, `m-rock`): `mars-landing.jsx` (after `orbit.jsx`,
    for the rings), on from the descent's last frame (`m-ground`'s line at 900): down on the engines, cut at
    2.4, the dish sending from 3.0; the glitch's right foot on `m-rock` (Viking 2), the failure sends once.
    `mars-surface.jsx` is `probe-surface`'s story at 1.5 times with Viking's lander: `v-arm` from the
    body's top right corner, stowed lying back along its top (-178), scooping at 18.
  - **The landers' tests in Earth orbit** (2026-10-14): `lander-orbit.jsx` (after `moon.jsx`): the
    lander at 0.8 over the way home's limb, the alarm lamp; `lander-engine-test`, `lander-staging`.
  - **The air launch** (2026-10-14): `air.jsx` (after `moon.jsx`, for the lamp): the carrier
    (`a-carrier`, its gear `a-gear` put away in flight) with the rocket plane (`w-plane` at 0.6) placed
    from the carrier's pose, the camera at 0.7 riding along; `air-takeoff` (on `a-ground`, a long
    runway), `air-cruise`, `air-drop`. `rocket-climb` and `rocket-flight` are the rocket plane alone
    over the limb. Mercury's parts (`m-orbit-mercury`, `m-mercury`, `m-arc-vm`) come in only for a
    cruise that names it.
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
