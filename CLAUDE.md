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
**M0 and M1 (the walking-skeleton milestone) are both done (2026-09-29).**
M1's exit criterion is verified end to end, over real sockets, via a real
scheduled job: `DevClient` logs in (auto-creating an account), submits a
funds action, disconnects; the real `GameServer` process resolves the turn
on its own via `TurnScheduler`; a second, separate `DevClient` process logs
back in and sees the submitted funds value with the season counter
advanced — see `design.md` §5's M1 exit-criterion note for the full
verification record. M1's six slices — snapshot/journal/replay persistence
(`de.mkoehler.launchwindow.persistence`: `PlayerSnapshotStore`,
`PlayerActionJournal`, `SnapshotReplay`, `AtomicFiles`, `PlayerStateService`,
2026-09-28, hardened 2026-09-29), `AccountStore`
(`de.mkoehler.launchwindow.server.accounts`, adapted from StarWars,
2026-09-29), the minimal KryoNet protocol (`de.mkoehler.launchwindow.net`:
`MessageRegistry`, `HandshakeRequest`/`Response`,
`SubmitActionRequest`/`Response`, `FetchStateRequest`/`Response`,
2026-09-29), the request dispatcher + real KryoNet server/client wiring
(`RequestDispatcher` in `server`, `NetworkServer`/`NetworkClient`/
`AppVersion` in `core`, `GameServer` actually wired up and launched,
2026-09-29), the scheduler + server config file
(`de.mkoehler.launchwindow.server.schedule`: `GameMeta`/`ScheduleProfile`/
`Blackout`/`GameMetaStore` for the host-edited `game-meta.json`,
`SchedulerState`/`SchedulerStateStore` for the scheduler's own progress,
`TurnScheduler` tying them together, called from `GameServer.render()` on
the same thread as `NetworkServer.poll()`, 2026-09-29 — needed adding
`jackson-datatype-jsr310` as a new dependency for the `java.time` fields in
`game-meta.json`/`scheduler-state.json`, with timestamp-array output
disabled on the shared mapper so the host-edited file reads as `"09:00"`
rather than `[9,0]`; missed-day catch-up decided (skip the gap, resolve one
turn on the next check, not one per missed day) and the default
`game-meta.json` timezone hardcoded to `Europe/Berlin` rather than derived
from the server process, since the user is expected to remain the only host
for the foreseeable future), and the CLI/console dev client
(`de.mkoehler.launchwindow.devtools.DevClient` in the `dev-tools` module,
a plain `NetworkClient`-based REPL with `state`/`funds <amount>`/`help`/
`quit`, 2026-09-29) — are all done. **M2 (rules engine, headless) is
underway (started 2026-09-30);** see `design.md` §5 for its scope. `core`'s
`de.mkoehler.launchwindow.model` package has M2's domain-model groundwork
done ahead of schedule — `PlayerSnapshot` (the per-player aggregate root),
`Archetype`, and the three personnel classes (`Astronaut`/`SetSpecialist`/
`MissionControlOfficer`) with their skill enums — see `design.md` §3.4 for
the full list and what's still deliberately deferred.
**First rules-engine slice done (2026-09-30):** `core`'s new
`de.mkoehler.launchwindow.rules` package, headless (no libGDX/network/
persistence dependency) — `PersonnelLifecycle.advanceSeason` resolves the
age/morale/retirement/resignation sub-slice of personnel lifecycle
(§2.2(a)/(c)), with `PersonnelDeparture`/`DepartureReason`/`PersonnelGroup`
as its reported events and a `ResolutionEvent` marker interface meant to
be reused as the shared event vocabulary by every later rules-engine
resolver. 21 unit tests (as of the departure-protection reversal below);
also added one field to `Personnel`
(`agingProgressSeasons`, an internal 0-3 bookkeeping counter, see
`design.md` §3.4) and extended `MessageRegistryTest`'s existing
`FetchStateResponse` round-trip to cover it over Kryo — no `MessageRegistry`
changes needed, since it's a field on an already-registered class, not a
new type. Full `mvn clean package` (all 4 modules) passes.
**Two more rules-engine sub-slices done the same day:** skill values
switched `Integer`→`Double` on all three personnel classes (a flat gain
under 1/season would otherwise round to 0); `SkillGrowth` (pure
diminishing-returns/flat-R&D/partial-credit formulas, own numbers not
BASPM's, 13 tests); `TrainingProgress.advanceSeason` (fixed-length
`BASIC`/`ADVANCED` training countdown+completion, new `TrainingKind` enum
+ training fields on `Personnel`/each subclass, `TrainingKind` appended to
`MessageRegistry`, 12 tests) — full write-up in `design.md` §2.2(c)/§3.4,
including the composition-order contract for running `PersonnelLifecycle`
before `TrainingProgress` once both get wired into one pass. **Still not
started in this same M2 slice (as of 2026-09-30; R&D's own target-skill
field is since done — see further down):** `PERMANENT` training,
mission-based growth's caller, hiring, Expert, and actually starting a
training assignment (no player-action path exists yet). Not yet wired into
`PlayerStateService`/`TurnScheduler` — that integration is M4 scope, not
M2's.

**Bot-driven playout harness started the same day, right after the first
resolver landed, then extended repeatedly the same day as
`TrainingProgress` and `HiringPool` landed too (2026-09-30):** `dev-tools`'s
new `de.mkoehler.launchwindow.devtools.analysis.PersonnelLifecyclePlayouts`
now drives `PersonnelLifecycle`, `TrainingProgress`, and `HiringPool`
(both `refreshCandidates` and, via an occasional bot hire, `hire`) every
season, in the documented composition order, with a bot that starts real
training assignments (not just idle/busy toggling) and occasionally hires
from the current candidate pool — see `design.md` §3.10 for the full
description, its self-checking invariants (skill-range/IN_TRAINING-
consistency, `PERMANENT`-target validity, and the hiring-pool ones: unique
ids across all rosters/pools, exact pool size after a refresh, candidate
skills within their band), and the full run-by-run history of its tuning
numbers as each resolver was wired in — **current numbers (seed 42, 200
games, 80-season/20-year cap, 3,000 starting employees): 259 resigned,
1,271 retired, 5,358 still employed, 70,424 training completions (avg.
gain 5.25), 3,888 successful hires; hiring-pool quality bands landed
"Poor" 0 times, "Average" 575, "Good" 20,836, "Excellent" 26,589 — this
bot's rosters practically never see a below-average candidate batch, since
its churn keeps average morale high.** Run it
directly with
`mvn -pl dev-tools compile exec:java -DmainClass=de.mkoehler.launchwindow.devtools.analysis.PersonnelLifecyclePlayouts`.
Deliberately named after the resolvers it drives, not yet the general
full-game harness `design.md` §3.10 originally described — that needs
a real bot and the mission/program/budget resolvers, none of which exist
yet.

**Departure-protection rule reversed from BASPM's own shape (user
decision, 2026-09-30, after reviewing this same session's work) — see
`design.md` §2.2(c) for the full reasoning:** BASPM protects any busy
employee uniformly; Launch Window now protects **per group** instead -
**SET is never protected** (they only ever work on mission components, no
reason to block a departure mid-project), **Astronaut/Mission Control are
protected only while `ASSIGNED`**, no longer while `IN_TRAINING`
(protection is meant to model "don't vanish mid-mission," which training
isn't). SET's retirement age also dropped 70→65. `PersonnelDeparture` gained
`assignmentStateAtExit` as a "was working on something, assign a
replacement" hint. A nice side effect: this also resolves the `PERMANENT`-
training "immortality loophole" flagged in `design.md` §3.4 the same
session, without needing a guard inside `PERMANENT` itself — training
never protected Astronaut/Mission Control from departure again once the
rule changed. **Both interpretations behind this rule confirmed by the
user afterward (2026-09-30):** applies to resignation as well as
retirement; `ASSIGNED` as the "on a mission" proxy is fine as-is, and the
user gave a reason the seemingly-open gap likely isn't real — a mission is
only ever `SCHEDULED` until the next turn evaluation, the same evaluation
that would run the first departure check, so there's no real window where
a crew sits merely assigned without already being active (still worth
re-confirming once real mission scheduling exists, `design.md` §5 slice 3).

**`PERMANENT` training implemented the same session:** `Personnel.trainingTargetLevel`
(shared field, not per-subclass) plus `TrainingProgress.shouldReenroll` —
each round is `ADVANCED`-length, re-enrolling automatically until the
target level is reached or a termination guard fires
(`PERMANENT_TRAINING_GAIN_EPSILON = 0.05`, needed for zero-learning-
potential or near-skill-cap targets that would otherwise loop forever).
16 `TrainingProgress` tests now (up from 12). **Wired into the bot harness
the same session** — starting training now picks evenly among `BASIC`/
`ADVANCED`/`PERMANENT`, with a matching independent invariant
(`checkPermanentTrainingTarget`). New numbers (seed 42, 200 games):
resignations dropped sharply, 234→144, since an employee cycling through
`PERMANENT` re-enrollment spends far more of their career busy/training
(gaining morale) than periodically idling between shorter rounds would -
`PERMANENT` turns out to be a genuine low-effort way to keep staff from
resigning, not just a training convenience, worth remembering once real
balance tuning starts. See `design.md` §3.10 for the full numbers.

**Hiring pool implemented the same day (2026-09-30):**
`core/.../rules/HiringPool` — the fourth M2 rules-engine slice.
`refreshCandidates` replaces each group's candidate pool with a fresh
6-candidate batch every call, quality banded by that group's own current
average roster morale (4 bands, re-derived numbers from `baspm.md` §9.4's
mechanic); `hire` moves one candidate from pool to roster, returning a
`HireOutcome` (`HIRED`/`UNKNOWN_CANDIDATE`/`ROSTER_FULL`) rather than
throwing, since a stale candidate id or a full roster are both ordinary
outcomes in an async multiplayer game, not error conditions. Three user
decisions the same day: new hires auto-start `BASIC` training on a random
skill from their own group; roster capacity is now enforced (8 astronauts,
12 SET, 8 Mission Control); hiring itself is free for now. New
`PlayerSnapshot` fields for this: `astronautCandidates`/
`setSpecialistCandidates`/`missionControlCandidates` and
`nextPersonnelId` (a shared counter behind the new
`allocatePersonnelId()` — deliberately shared across all three groups and
both real hires and candidates, so no id is ever reused, which matters for
an async game where a stale action could otherwise resolve against the
wrong entity later). 11 new `HiringPoolTest` tests. **Composition-order
contract for whoever wires this into a real season pass (M4):** journaled
player actions (including `hire`) first, then `PersonnelLifecycle`, then
`TrainingProgress`, then `refreshCandidates` last.

**Wired into the bot harness the same session** (numbers folded into the
"Bot-driven playout harness" paragraph above): each season the bot may
first hire one random candidate out of each group's current pool (25%
chance per group, a stand-in for a journaled player action), matching the
composition order above — `PersonnelLifecycle`/`TrainingProgress` run as
before, then `HiringPool.refreshCandidates` runs last. Three new
invariants, all computed independently of `HiringPool`'s own logic: every
id is globally unique across all three rosters *and* all three candidate
pools; each pool holds exactly 6 candidates after a refresh; every
candidate's skills fall inside the range the harness's own independently
re-derived morale-to-band mapping would select — this needed promoting
`HiringPool`'s per-band skill-range constants from `private` to `public`
(plain tuning numbers, not the kind of internal decision logic that has to
stay off-limits to keep the cross-check genuine — see `design.md` §3.10's
note). Tenure accounting also needed a fix here: a mid-game hire's tenure
is measured from its own join season, not season 0, via a per-game
`joinedAtSeason` map — caught by the advisor before this was committed.
Full numbers and reasoning in `design.md` §3.10. This completes "finish
personnel first" for M2's current scope — see `design.md` §5 for what's
still open in the personnel slice (R&D's own resolver, mission-based
growth's caller, the Expert trait) and what's next (programs/mission-
configuration slice).

**Programs/mission-configuration slice done (2026-10-01):** new
`de.mkoehler.launchwindow.content` package — `ProgramCategory`/`Program`/
`MissionConfiguration`/`MissionComponent` (immutable records, content
definitions per `design.md` §3.4, not per-player state), `ContentCatalog`
validating every cross-reference at construction — and new
`de.mkoehler.launchwindow.rules.ProgramOpening` (`effectiveOpeningCost`,
`reliabilityTransferFor`, `open`), re-deriving `baspm.md` §10.1/§10.3's
opening-discount and reliability-transfer formulas with Launch Window's
own numbers. New `PlayerSnapshot` fields `openedProgramIds`/
`missionComponents` (the latter backed by a new per-player state class,
`OwnedMissionComponent`, appended to `MessageRegistry`). **Decided (user,
2026-10-01):** a component's reliability floor is the transfer-inclusive
starting value, not the bare baseline — see `design.md` §2.4(c)/§5 for the
full writeup, including why BASPM's own code left this exact choice
ambiguous. 16 new `ProgramOpeningTest` tests (including a fix for a real
bug the advisor caught: initial reliability wasn't clamped at the
component's own max), plus the two real correctness traps (an
already-owned component is never reset/re-transferred; two components
newly granted in the same `open` call never see each other's fresh
reliability when computing transfers). Unblocked SET's
R&D-target-component linkage (see `design.md` §3.4) — done the same day,
see below.

**`ProgramOpening` wired into `PersonnelLifecyclePlayouts` the same day
(2026-10-01):** a small fixture `ContentCatalog` (three programs, four
mission components, two reliability-transfer relationships — only one of
which can ever actually fire, by design, since the other's two components
are granted by the same program in the same `open` call, see
`design.md` §3.10) lets the bot open one random not-yet-open program per
season (10% chance), in the same journaled-player-action bucket as
`HiringPool.hire`. Four new invariants (funds never negative, opened ids
known to the catalog, owned-component reliability/floor ordering, every
owned component required by some opened program). **Numbers (same
200-game/80-season run, now shifted by this slice's own random draws —
no personnel resolver's behavior changed): 236 resigned (avg. tenure
29.7), 1,242 retired (avg. tenure 55.9), 5,371 still employed, 69,961
training completions (avg. gain 5.24), 3,849 hires, hiring-pool bands
0/597/21,001/26,402, 599 programs opened.** The fixture's total program
cost (2,300 at most) stays well under the 5,000 starting funds, so
`INSUFFICIENT_FUNDS` is exercised by `ProgramOpeningTest`'s own unit
test, not meaningfully by this harness — see `design.md` §3.10 for the
full writeup.

**R&D-target-component linkage done the same day (2026-10-01):**
`SetSpecialist.rdTargetComponentId` plus `ResearchProgress`/
`ResearchCompleted` in `de.mkoehler.launchwindow.rules` — a `SET`
employee's `ASSIGNED` state now always means R&D on a specific owned
`MissionComponent`, growing both the employee's own skill (flat,
`SkillGrowth.flatRdGain`, already built) and the component's reliability
(new `SkillGrowth.reliabilityGain`, diminishing-returns against the
component's own ceiling, **unconfirmed default shape and factor range**
— `baspm.md` has no decompiled trace for this half of the mechanic, only
a manual-level claim that it scales with skill at all; see `design.md`
§3.4 for the full proposal and the open question). `PersonnelDeparture`
gained `rdTargetComponentIdAtExit`, the reverse pointer §3.4 flagged as
this linkage's own follow-up. Wired into `PersonnelLifecyclePlayouts` the
same day: the bot now picks a random owned component when assigning a
`SET` specialist (falling back to training if none owned yet), with a new
invariant enforcing the `ASSIGNED` ⇔ owned-target pairing every season.
**Numbers (same run, shifted again — partly behavioral this time, not
just the random stream: `SET` can't go `ASSIGNED` until a program is
open and a component owned, which changes morale/resignation timing for
that group): 287 resigned (avg. tenure 29.2), 1,269 retired (avg. tenure
55.9), 5,345 still employed, 72,258 training completions (avg. gain
5.16), 3,901 hires, hiring-pool bands 0/608/21,560/25,832, 597 programs
opened, 37,300 R&D seasons completed (avg. reliability gain 1.65).** See
`design.md` §3.10 for the full writeup. This completes wiring every M2
resolver built so far into the harness.

**Mission-simulation decision gate resolved (user, 2026-10-01):** §2.5(c)'s
outcome-tier shape — the one open question that was blocking M2's
mission-simulation slice from starting — is settled: port BASPM's full
five-tier step-outcome ladder (clean success / rescued success /
half-credited continuing failure / full mission-ending failure /
crew-fatality-eligible worst tier), uniformly across every step, not a
per-step vital/optional split. Re-deriving Launch Window's own tuning
numbers, per `design.md` §1.2. This also settles `baspm.md` §8.4 item 1 —
the trace already showed the half-credited tier is reachable in BASPM's
own code, the user's in-game play just never happened to land that
narrow a roll band. **The loose end this decision created is now also
resolved, same day:** the "tiered goal-achievement rating" idea (§2.6(c))
had no trigger left once the vital/optional split was dropped, and is
now scrapped for v1 outright rather than redesigned around a new
trigger — goal achievement is plain achieved/not-achieved. This also
makes §2.9's win-condition tie-break's second-order criterion (best
goal-achievement rating) permanently moot for v1; the medal-table
comparison is all it has. That decision was only the design gate — see
below for the mission-simulation slice's own first implementation step.

**Mission simulation's pure formula half implemented (2026-10-01):**
`MissionStepResolution` + `MissionStepOutcome` (`core/.../rules`) — the
two-roll formula itself (base involvement-weighted probability, optional
crew-skill blend, flight-controller-scaled saving throw, five-tier
outcome bands), taking already-summed contribution totals as plain
`double`s with no dependency on `MissionComponent`/`Personnel`, the same
"formula class before its caller" order `SkillGrowth` followed. All four
tuning constants (a 65% crew-blend hardware threshold, a 20% crew-blend
weight, and two saving-throw band-size fractions) are unconfirmed
defaults, re-derived to values different from BASPM's own code-confirmed
numbers (a 70% threshold, a 25% weight) per `design.md` §1.2 — see
`design.md` §2.5(d) for the full writeup. **Deliberately not wired into
the bot harness yet** — no caller exists: the `MissionStep` content
shape, per-player in-progress-mission state, the resolver that walks a
season's step batch against it, post-mission reliability change, and
mission-based skill growth are all still open, as is re-confirming
`AssignmentState.ASSIGNED` as the "on an active mission" proxy (§2.2(c)'s
own open follow-up) — see `design.md` §5's M2 entry for the exact list.

Git repo
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

**Main-menu-as-clickable-map idea explored, not yet a design decision
(2026-09-29):** the user is considering an aerial-view agency complex
(AI-generated concept art, four seasonal variants planned) as the main menu,
with buildings as clickable hotspots that highlight on hover. Prototyped in
`dev-tools` (`MenuHotspotPrototype.java`, see "Build system" below for how
to run it) to test one specific question: since the seasonal art variants
won't have pixel-identical building placement, hand-authored hotspot
polygons need a margin of error, so a softened highlight edge (rather than
a crisp outline) would both look nicer and hide that margin. Tried baking
the polygon fill into a small offscreen `FrameBuffer` (much smaller than
the polygon's own bounding box) and stretching it back up with bilinear
filtering for a cheap blur - **this broke on the user's machine (whole
window went solid grey)**, not debugged further since it didn't matter for
the actual question. **What's locked in instead (2026-09-29): a plain
hard-edged highlight** - `ShapeType.Filled` polygon fill plus a
`ShapeType.Line` outline, both drawn in a single neutral near-white color
(`HIGHLIGHT_COLOR`) at the *same* low alpha (0.18 - matching the outline's
alpha to the fill's was a deliberate fix, an earlier version had the
outline more opaque than the fill and it read as a mismatched double-edge).
This alone proved the core concept (hover detection via
`Intersector.isPointInPolygon`, polygon-with-margin tolerating seasonal art
drift) well enough - the soft-edge blur is not a blocker and can be
revisited later if wanted. Still open/unconfirmed: actual per-building
hotspot polygon authoring (a real building outline, not the arbitrary demo
hexagon; possibly needs a small polygon-authoring dev tool of its own), how
hover ties into a tooltip/label, and whether this becomes the real
main-menu screen at all — none of that is decided yet, this was purely a
rendering-technique spike. The reference aerial-view art
(`dev-tools/src/main/resources/menu-prototype/aerial.png`) is the user's
own AI-generated concept image, not BASPM-derived, so it's exempt from the
source-material restrictions below — but it's prototype-only placeholder
art, not committed final main-menu art.

**Main-menu hub: real assets + full chrome spec now exist, still M5-scoped
groundwork, not implemented (2026-09-30).** Superseding the prototype
above with real production art: `assets-raw/main_menu/` now holds complete
NASA + SOVIET asset sets (4 seasons &times; 4 times of day each, plus one
`hotspots.json` sidecar per faction, authored with the new `HotspotEditor`
tool above) and the user shared a Personnel Recruitment screen mockup
(a private Claude Artifact) establishing the header/footer chrome shared
by every screen. Full details — the season/time-of-day art-cycling rule,
the asset crop needed (1920&times;1088 &rarr; 1920&times;1080), the
confirmed BASPM precedent (this *is* BASPM's own "Space Complex Screen",
`baspm.md`'s `WM` entry), the header/footer layout itself, and the
decision to drop BASPM's "End Season" button entirely (Launch Window's
async model has no equivalent action) — are in `design.md` §3.13 (new)
and §1.1/§3.12 (updated), not repeated here. Still nothing implemented in
`lwjgl3`; M2 (design.md §5) is still the actual next milestone, this is
groundwork prepared ahead of it.

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
  **Remaining astronaut/mission-control skill icons converted the same way
  (2026-10-01):** the user had since added 9 more bitmaps for the
  `AstronautSkill`/`MissionControlSkill` icons (same batch, same AI tool,
  `assets-raw/icons/*.png`) — `leadership`, `piloting`, `science`,
  `fitness` (astronaut; `EVA` reuses `icon_eva_suits.svg` from the SET
  batch above rather than getting its own file, since the user specified
  the astronaut "EVA" skill and the SET "EVA Suits" skill share one icon)
  and `propulsion`, `trajectory_and_guidance`, `spacecraft_systems`,
  `crew_and_payload`, `mission_operations` (mission control). All 5
  `AstronautSkill`/5 `MissionControlSkill`/5 `SetSkill` values now have an
  icon. The simple per-image median-blur background estimate used for the
  first batch silently punched a false hole into any icon area thicker
  than its blur radius (its own local neighborhood became mostly icon, not
  background, so the diff against itself read as "no icon here") — caught
  on the `science` icon's microscope arm. Fixed by fitting a smooth
  low-order 2D polynomial per color channel to only the image's outer
  border ring (always guaranteed real background, never samples the icon)
  and using that as the per-pixel background estimate instead - robust
  across the flat single-tone icons, an icon with a small non-white accent
  color (the `science` icon's DNA strand), and a two-tone icon layering a
  darker secondary gray over the background (`mission_operations`'s
  screens/map), all flattened to one silhouette the same way the first
  batch was.

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
(developer tools never shipped/packaged by any release workflow — the
CLI/console `DevClient`, `MenuHotspotPrototype`, a windowed libGDX
rendering spike, and `HotspotEditor` (2026-09-29), a plain Swing app (no
libGDX dependency at all, unlike the other two) for hand-authoring the
main-menu building hotspot polygons: load an image, left-click places a
point, right-click undoes the last one, Enter finishes+names the polygon
(prompting for a name; starts the next one automatically), Escape discards
the in-progress one, mouse wheel zooms centered on the cursor, arrow keys
nudge the selected finished polygon (Shift for a bigger step) — only
named/finished polygons save, as a `<image>.hotspots.json` sidecar (top-
left-origin, Y-down image-pixel-space coordinates — matching the raw PNG's
own indexing, not libGDX's Y-up convention; any flip needed happens on the
loading side, see `MenuHotspotPrototype.create()`'s own example). Also has
File > Import Polygons From... to seed a new image's polygons from another
image's sidecar (e.g. a seasonal variant sharing the same building
layout), which is exactly how the real `assets-raw/main_menu/` hotspots
below were produced. See "Current status" above for what it actually
produced. `dev-tools` also now has its `analysis` package (2026-09-30,
mirroring Robot-Rampage's own `dev-tools/.../analysis` placement,
`design.md` §3.10) — `PersonnelLifecyclePlayouts`, the first slice of the
bot-driven playout harness; see "Current status" above. `dev-tools` therefore now
also carries `gdx-backend-lwjgl3` + `gdx-platform:natives-desktop` (only
`MenuHotspotPrototype` needs them; `DevClient`/`HotspotEditor` don't, but
the module has one shared classpath). The exec-maven-plugin config still
defaults to `DevClient` — run a different `dev-tools` main class by
overriding the POM property directly with **`-DmainClass=...`**, not
`-Dexec.mainClass=...`
(the plugin's `<mainClass>` is bound to `${mainClass}`, the POM property,
so only a same-named `-D` override reaches it):
`mvn -pl dev-tools compile exec:java -DmainClass=de.mkoehler.launchwindow.devtools.HotspotEditor`
(or `...MenuHotspotPrototype`).
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
  mvn -pl dev-tools compile exec:java -Dexec.args="localhost 45825"
  ```
  **Reinstall `core` after *every* change to it, and after every new tag,
  not just when its own code changed** — `lwjgl3`/`server` resolve `core`
  from `~/.m2`, and a new git tag shifts the jgitver-computed
  `${project.version}` the next build expects, so a stale install silently
  stops matching (or runs old code). **Verified (2026-09-29, local-only
  tag experiments, nothing pushed): a plain commit alone does not shift
  the version** with this repo's current jgitver setup (no tags yet, no
  `useCommitDistance`/`useGitCommitId`) — every commit between two tags
  computes the exact same version string, only creating a *new tag* bumps
  it. The start scripts still always reinstall unconditionally regardless,
  since it costs nothing and remains exactly correct once tagging starts.
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
