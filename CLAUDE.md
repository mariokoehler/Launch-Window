# CLAUDE.md — Launch Window project notes

Cross-session memory for this repo: gotchas, decisions and their reasons,
conventions — so future sessions don't have to rediscover them. Game design/
architecture (what the game *is*, how each system is meant to work) belongs
in `design.md`, not here. Everything we learn about the **original** game
we're adapting (Buzz Aldrin's Space Program Manager, "BASPM") belongs in
`baspm.md` — a reference dictionary/index into its decompiled source and
extracted assets, not a copy of its content. All three files get updated the
same session a decision/finding is made, not after.

## What this project is

**Launch Window** — *"Stay one small step ahead."* — a non-commercial fan
project reimagining **Buzz Aldrin's Space Program Manager** (BASPM,
Slitherine/Matrix Games, Unity) as an **asynchronous multiplayer**
space-agency management game. Name and tagline decided 2026-09-27 (a working
title of "Space Race" was used before this). Where BASPM is single-player/
turn-based-realtime (or PBEM 1v1, see below), Launch Window is a
daily-cadence async multiplayer game: players log in once a day (or
whenever), queue up actions for the current turn, and a server-side process
resolves everyone's turn at a fixed time, after which players return
to see results and plan the next turn. A full game is expected to span weeks
to months, not a single sitting.

Full design lives in [`design.md`](./design.md) — read that first for concept,
mechanics, and architecture decisions. Everything about *how the original
game works* (menus, formulas, data files, source layout) lives in
[`baspm.md`](./baspm.md) — read that when a design decision needs to compare
against or diverge from the original.

**Sibling projects** (same author, prior multiplayer games — mined for
patterns, not code, since each has a different network model):
- `C:\Users\mario\StarWars` — real-time authoritative-server deathmatch
  shooter (KryoNet, UDP snapshots, client prediction/reconciliation). Its
  `CLAUDE.md`/`design.md` pairing is the template this project follows.
- `C:\Users\mario\Robot-Rampage` — turn-based simultaneous-programming board
  game adaptation (KryoNet, TCP-only, command/event protocol, no prediction).
  Much closer in shape to what Launch Window needs than StarWars is: turn-based,
  simultaneous secret input, server-resolved. The **big difference from
  Robot-Rampage**: that game resolves a turn the instant every player
  confirms (or a short timer expires) — a live multi-minute session. Space
  Race resolves on a **daily wall-clock schedule** regardless of who's online
  when — nobody is ever "waiting live" for other players, which changes the
  netcode shape entirely (no persistent connections during a turn, no
  reconnect-during-programming machinery, but real scheduled-job
  infrastructure and notification/email needs instead).

**Genuinely relevant prior art found inside BASPM itself** (see `baspm.md`
§5, "PBEM multiplayer"): the original game already shipped **two
asynchronous multiplayer modes** (`MultiplayerColdWar`, `MultiplayerFreeform`)
built on a play-by-email-style HTTP protocol (`PBEMManager.cs`) — challenge/
accept, upload/download a turn's save-game blob, resign, claim-on-timeout.
The manual confirms it's **strictly 1v1** (host + one opponent, "you'll get
an e-mail notification that will inform you when to play your next move").
It proves the underlying single-player simulation was already built to be
paused, serialized, handed off, and resumed — exactly the property a
daily-batch async multiplayer server needs. It also isn't fully isolated
the way an early pass at this analysis assumed: `GameWorld` tracks the
opponent's login name/agency and exposes which *generic goals* they've
achieved (`GetAllGenericGoalsAchievedInfoByOpposingFaction`), which a
side-by-side comparison screen renders — real, numbers-backed precedent for
a "see your rival's progress" feature. What's still genuinely absent is any
*interactive* cross-player mechanic — no chat, no sabotage, no trading —
that part of Launch Window's design has no BASPM precedent. Worth studying its
turn-boundary/serialization design (`baspm.md` §5) before inventing our own
from scratch.

## Current status

**Analysis phase is done; basic project scaffolding exists (2026-09-27);
milestone/slice roadmap drafted, first domain-model groundwork implemented
(2026-09-28).** The initial `CLAUDE.md`/`design.md`/`baspm.md` docs plus a
deep-dive analysis pass over BASPM's decompiled source (`baspm.md` §8-§13)
came first; the Maven multi-module skeleton (`pom.xml` + `core`/`lwjgl3`/
`server`/`dev-tools`) came after, mirroring the sibling projects — see
"Build system" below. **`design.md` §5 has the implementation roadmap**
(milestones M0-M7+, format borrowed from Robot-Rampage's own sibling
project) — read that before picking up any implementation work, to see
what milestone it belongs to and what's already decided about ordering.
**M0 (this scaffold) is done. M1 (the walking-skeleton milestone) is
underway: its first two slices are done** — snapshot/journal/replay
persistence (`de.mkoehler.launchwindow.persistence`: `PlayerSnapshotStore`,
`PlayerActionJournal`, `SnapshotReplay`, `AtomicFiles`, `PlayerStateService`,
2026-09-28, hardened 2026-09-29) and `AccountStore`
(`de.mkoehler.launchwindow.server.accounts`, adapted from StarWars,
2026-09-29) — **the KryoNet protocol, the scheduler, and a CLI dev client
are still to come, see `design.md` §5's M1 slice list for exactly what's
open, including one flagged decision gate (the login → player-data-
directory mapping).** `core`'s
`de.mkoehler.launchwindow.model` package has M2's domain-model groundwork
done ahead of schedule — `PlayerSnapshot` (the per-player aggregate root),
`Archetype`, and the three personnel classes (`Astronaut`/`SetSpecialist`/
`MissionControlOfficer`) with their skill enums — see `design.md` §3.4 for
the full list and what's still deliberately deferred; no resolution logic,
screens, wire protocol, or content-definition types yet. Git repo
initialized 2026-09-27 and pushed
to GitHub as a **private**
repo (`mariokoehler/Launch-Window` — GitHub doesn't allow spaces in repo
names, so "Launch Window" became "Launch-Window"; local folder stays
`LaunchWindow`). Will flip to public once something playable exists or
private-repo GitHub Actions limits become a problem — ask before making that
change.

**Visual design system decided (2026-09-28).** A "restrained engineering
blueprint" UI look — see `design.md` §3.12 for the full color/type/layout
system (colors are semantic, not decorative; thin borders + solid fills;
readability-critical surfaces always sit on a flat white background, never
directly on the textured page frame). Wordmark/logo intentionally deferred
as a separate task (`design.md` §4) — the UI typeface (Archivo + Work Sans)
is not meant to double as the brand mark. **Translated into code
(2026-09-28):** `core/.../client/ui/Theme.java`, mirroring the sibling
projects' `Theme.java` pattern (colors/spacing/radii/borders as constants,
`Theme.Fonts` for gdx-freetype-generated bitmap fonts, `Theme.Shapes` for
the actual Scene2D drawables) — see `design.md` §3.12 for what's
implemented vs. still open (the Archivo/Work Sans `.ttf` files themselves
aren't in the repo yet). Needed two new Maven dependencies, `gdx-freetype`
(`core/pom.xml`) and `gdx-freetype-platform`/`natives-desktop`
(`lwjgl3/pom.xml`) — same gotcha as every other native-backed libGDX piece,
each runnable module needs its own natives dependency independently.

**UI Asset Sheet canvas started (2026-09-29)**, a living Claude Artifact
(not a repo file) collecting the graphical assets `Theme.java` will need to
replicate the mockup screens in libGDX: the tileable blueprint-grid
background texture (spec'd as an exact 100×100 px repeat unit, with the
libGDX Pixmap/Texture/TiledDrawable build steps written out) and every icon
glyph used so far, grouped by where they appear. Unlike the earlier
one-off exploration canvases (Style Direction, Logo Concepts), this one is
meant to be kept in sync — add to it whenever a new screen mockup
introduces a new icon or texture, rather than letting glyphs go untracked
across mockups. Its own asset provenance note repeats the BASPM-traced-icon
story from the source-material note below, so a reader of the sheet alone
still gets the warning. Link is with the user (private Claude Artifact,
not committed to this repo); ask them if a future session needs it and
doesn't have it in context.

**Logo decided (2026-09-28):** `assets/branding/logo.svg` — a mission-patch
illustration (Saturn V ascending past Earth's limb, starfield, a gold
"window" arc, ribbon wordmark), superseding the four earlier abstract
symbol concepts once the user supplied a reference image. See `design.md`
§3.12 for the full description and the follow-up production tasks
(outlining the wordmark text, a small-size icon-only crop) still open.

## Source material locations (read-only, reference-only — never edit, never copy into the repo)

- **Machine caveat:** if the environment variable `COMPUTERNAME` is
  `S0147359`, this is the user's laptop, which has neither the original
  game install nor the extracted/decompiled asset dump below — there's no
  `E:` drive at all on that machine. If a task needs anything under `E:\...`
  while on that machine, stop and ask the user rather than assuming the
  paths exist; work it out with them before proceeding.
- **Game install** (Steam):
  `E:\Steam\steamapps\common\Buzz Aldrin's Space Program Manager`
- **Extracted/decompiled analysis dump**: `E:\Temp\BASPM` — decompiled C#
  source (`source/`), unpacked asset bundles (`extracted/`), exported
  MonoBehaviour instance data as JSON (`monobehaviours/`). Full map in
  `baspm.md`.
- **These are for private design analysis only.** Nothing under either path
  — text, art, audio, video, code — may be copied or lightly paraphrased
  into the Launch Window repo itself: see `design.md` §1.2 (hard rule, not a
  preference — this repo is public on GitHub, so anything committed to it is
  effectively redistributed). Read for understanding, then write Space
  Race's own original version from the underlying real-world facts.
  **One explicit exception approved by the user (2026-09-28):**
  `assets-raw/baspm/ui/*.png` — BASPM UI screenshots committed to the repo
  as reference material for later Launch Window UI work. The user judged
  screenshots low-risk and asked for this single carve-out while keeping
  the rule active otherwise; don't extend it to other asset types (art,
  audio, video, source) without asking again.
  **The rule caught a real violation the same day (2026-09-28):** five icon
  SVGs (`assets-raw/svg/icon_rockets.svg` and four others for the SET
  skills) had been traced/converted from BASPM's own SET Centre UI and
  committed to `origin/master`. Caught while building the Personnel
  Recruitment mockup (a Claude Artifact, not part of this repo) when asked
  to use them for the mockup's sort-by-skill buttons — the path data's
  density (hundreds of plotted anchor points) was the tell that they were
  traced rather than drawn. The screenshot exception above does not cover
  them (icons are a derived-art asset type, not a screenshot), so on
  confirmation from the user they were removed outright rather than kept
  and just avoided. The mockup itself uses simple original stroke icons
  (rocket/signal-waves/shield/capsule/helmet) instead. If icons for these
  five SET skills are wanted again, they need to be drawn original from
  the underlying real-world concepts, not traced from BASPM's asset dump —
  same posture as everything else under `baspm.md`'s source-material note.
  **Replaced with legitimate originals (2026-09-29):** the user generated
  five new bitmap icons with an AI image tool (`assets-raw/icons/*.png`,
  described by the user as original, not BASPM-derived) and asked for them
  to be vectorized. Done via luminance thresholding to isolate the white
  icon from its background/shadow/watermark, then contour tracing (holes
  via `evenodd`) — a mechanical bitmap→vector conversion of the user's own
  new source art, not a repeat of the BASPM-tracing violation above.
  `assets-raw/svg/icon_*.svg` now hold these new traces.

## Conventions / preferences (carried over from the sibling projects)

- **Design decisions go into `design.md` immediately**, same session as the
  decision — not summarized after the fact. Findings about the original game
  go into `baspm.md` the same way.
- **When a sub-detail is left unspecified, propose a concrete default
  directly in the doc and flag it as unconfirmed**, rather than stalling to
  ask. Reserve a real pause/question for foundational or hard-to-reverse
  choices (a core library, a balance-defining formula, the network model).
- **Proactively flag security-relevant concerns during design even
  unprompted** (e.g. auth, save-data trust boundaries) — kept brief.
- **Proactively flag educational-content opportunities, unprompted, the
  moment they come up** (user, 2026-09-27 — `design.md` §1.1 is the standing
  design pillar this serves). Launch Window wants to explain the real
  engineering/physics behind a mission, not just theme itself around real
  programs — e.g. why orbital rendezvous is counterintuitive, why restarting
  an engine in zero-g is hard. Any time BASPM analysis or Launch Window design
  work surfaces a real historical mission, a real engineering challenge, or
  a place BASPM's own historical-facts text (`baspm.md` §3.2) stops short of
  explaining *why* something was hard, say so and note it in `design.md`
  §1.1 the same session — don't save it up for later.
- Commits (once a repo exists) should be GPG-signed — if `git commit` fails
  with a `gpg-agent` connect error, start Kleopatra
  (`C:\Program Files (x86)\Gpg4win\bin\kleopatra.exe`) and retry the same
  commit; never `--no-gpg-sign`.
- **Advisor before and after** on any multi-file feature once implementation
  starts: consult before writing code, again once it looks complete. Caught
  real design-level and integration-level bugs on both sibling projects.
- Fan-project note: BASPM's names/art/data are Slitherine/Matrix Games'
  IP, referenced here purely for non-commercial analysis and inspiration,
  the same posture StarWars/Robot-Rampage take toward their own source
  material. Launch Window ships its own name, art, and (likely reworked)
  program/mission naming — not a copy of BASPM's content. **This is now a
  hard rule, not just intent — see `design.md` §1.2 and the source-material
  note above: no verbatim or lightly-paraphrased text, no reused art/audio/
  video files, ever committed to this repo.**

## Build system

**Decided (user, 2026-09-27), see `design.md` §3.3/§3.9:** Maven,
multi-module, mirroring the sibling projects' template almost directly
(Robot-Rampage's `pom.xml` was the base, not StarWars' — its `lwjgl-bom`
import replaces StarWars' ~200-line per-LWJGL-classifier block). Modules:
`core` (shared client/server code — `NetworkConstants`, a placeholder
`LaunchWindowGame`, and the `de.mkoehler.launchwindow.model` package holding
the first real domain types, see `design.md` §3.4), `lwjgl3` (desktop client
launcher), `server` (`gdx-backend-headless` dedicated server), `dev-tools`
(empty for now — no concrete tool exists yet, see its own `pom.xml` comment
for why it has no `exec-maven-plugin` config).
Java 25 (`maven.compiler.release`), Maven 3.8.4 confirmed working locally.

- `mvn clean package` from the repo root builds everything and runs the
  tests.
- `start_client.cmd` / `start_server.cmd` reinstall `core` and run the
  module — but **don't run them from an automated/non-interactive shell,
  they end in `pause`**; use the underlying commands directly instead (see
  below), backgrounded if needed.
- By hand: install `core` first, **then** run the target module *alone* —
  no `-am` on the exec step (a bare exec goal with `-am` walks the whole
  reactor, including the `pom`-packaging parent, and fails there):
  ```
  mvn install -pl core -am -DskipTests
  mvn -pl server compile exec:java
  mvn -pl lwjgl3 compile exec:exec
  ```
  **Reinstall `core` after *every* change to it, and after every new commit
  or tag, not just when its own code changed** — `lwjgl3`/`server` resolve
  `core` from `~/.m2`, and a commit shifts the jgitver-computed
  `${project.version}` the next build expects, so a stale install silently
  stops matching (or runs old code). The start scripts always reinstall
  unconditionally for exactly this reason.
- **KryoNet TCP port: 45825** (`NetworkConstants.TCP_PORT`) — deliberately
  different from StarWars' 45625/tcp+45626/udp and Robot-Rampage's 45725/tcp,
  so all three servers can run on the same machine/NAS at once. TCP-only, no
  UDP channel: Launch Window is async/daily-batch (design.md 3.1/3.3), not
  real-time, so there's no continuous-simulation channel to justify one.
- **Verified locally (2026-09-27):** `mvn clean package` passes (all 4
  modules, including `core`'s one real test — a Jackson round-trip proving
  the flat-file JSON persistence mechanism design.md §3.7/§3.8 depends on).
  Both the server (binds 45825, logs a KryoNet "Server opened") and the
  client (opens an LWJGL3 window, no `SharedLibraryLoadRuntimeException`)
  were actually launched and confirmed running via `tasklist`/
  `Get-CimInstance Win32_Process`, not just "no error in the log" — then
  killed. **`TaskStop` on the background task reported success but left the
  underlying `java.exe` processes running** (Windows-specific, exec:exec
  forks a detached child process) — verify with `tasklist`/
  `Get-CimInstance Win32_Process -Filter "Name='java.exe'"` and `taskkill
  /F /PID <pid>` by hand if a launched client/server needs killing, don't
  trust a bare "stopped" report.

### Maven + libGDX gotchas (carried over from the sibling projects, still valid)

- Maven's default `maven-compiler-plugin` ignores `maven.compiler.release`;
  the parent pins 3.13.0. Surefire is pinned to 3.2.5 for JUnit 5.
- **Each native-backed libGDX piece needs its *own* `*-platform`
  `natives-desktop` dependency**, independently, in every runnable module
  (`lwjgl3`, `server`). Missing one only fails at *runtime*
  (`SharedLibraryLoadRuntimeException`), never at build/test time.
- `${project.parent.basedir}` is not a valid property — use
  `${project.basedir}/../assets` for the shared `assets/` resource dir.
- The KryoNet fork is JitPack-only; the parent pom declares the JitPack
  repository.
- A plugin's top-level `<configuration>` applies to every goal of that
  plugin invoked from the CLI — `lwjgl3`'s exec config is written for
  `exec:exec`; `server`'s is written for `exec:java`. Don't share one goal's
  config with the other.
- XML comments in poms can't contain a literal `--` anywhere in their text.
- **Release packaging (client zip via jpackage, server Docker image) is
  deliberately deferred** — no game content exists yet to package. Both
  sibling projects have a working, documented template for this
  (`release-client` Maven profile, `Dockerfile`, `docker-compose.yml`,
  tag-triggered GitHub Actions) once it's actually needed — see their own
  `CLAUDE.md`/`design.md` "Releasing" sections, migrate from there rather
  than re-deriving it from scratch.
