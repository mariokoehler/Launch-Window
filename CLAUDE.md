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

**Next up (as of 2026-10-03, later the same day):** M4 is complete too
(`design.md` §5, M4 slices 4a–4e). Players act through journaled
`PlayerAction`s, and undo is by inverse actions only, so the journal stays
append-only. The server pins the content pack from `game-meta.json`, players
claim a slot (archetype, unique agency name and director name), turns resolve
through `SeasonResolver` with seeded randomness, and the first winner goes to
`game-result.json`. The exit proof ran over real sockets with a scheduled
resolution. Next is M5, the libGDX client. **Workflow (user, 2026-10-03):**
Claude writes the screen spec (`ui-spec.md`, read by Claude Design from GitHub), the user makes
the mockups in Claude Design, and Claude implements them. Keep `ui-spec.md` current as screens
change, and close its server gaps G1–G7. **Workflow update (user, 2026-10-05):** small changes to
screens and components that already exist in libGDX are made in libGDX directly, without a mockup
round. Check them with `ScreenSnapshot` and record them in `ui-spec.md`. Mockups remain for new
screens, new components and bigger layout changes. The libGDX client is now the reference for
anything already built. **Gotcha for proof runs:** pipe
DevClient input from the Bash tool, not PowerShell. PowerShell's pipe to a
native process prepends a byte-order mark, which breaks the login line.

**As of earlier on 2026-10-03:** M2 and M3 are both complete. M3 (`design.md`
§5) added a JSON content-pack loader, five buildings with upgrades, agencies,
creation cost, program and rocket-program closing, rocket choice at launch, and
CAPCOM. It also authored the early-era pack (`assets/content/early-era`), and
`ContentPackPlayouts` bots reach the declared goals in 189 of 200 games. Next is
M4: player-action dispatch (opening programs and rocket programs, launches with
a `LaunchRequest`, hires, upgrades, conversions, closing, as journaled actions),
the real season pass on the server, and game-meta wiring (the declared goal list
and the win check). Known departures from BASPM are recorded in `baspm.md` §12.1
and `design.md` §2.7 and §5.

**M3 values are BASPM-close** (user decision, 2026-10-03), extending the budget
exception below to the whole content pack: costs, reliability, prestige, step
involvements, and goals come from BASPM's Data Scripts, while ids and names are
our own. The pack is generated by `E:\Temp\BASPM\tools\baspm_pack.py`, with its
goal-id map in `goals.map.json` next to it. Run `python baspm_pack.py discover`,
then `python baspm_pack.py write <repo>\assets\content\early-era`. The script
lives on the main PC only, never in the repo, because it reads `E:` and holds
BASPM's internal ids. To extend the pack, add the scope and our own ids to its
`PROGRAMS`/`COMPONENTS`/`CONFIG_NAMES` maps and to `goals.map.json`. Its mapping
rules are written down in `design.md` §5, M3 slice 4. `checklist.py` (same folder) regenerates
`assets-raw/ART_CHECKLIST.md` from `baspm_programs.txt` and the art on disk; update its file maps when
art lands. **Pitfall:** PowerShell 5.1's `Set-Content -Encoding
utf8` writes a BOM, which breaks javac. Write files with
`[IO.File]::WriteAllText(path, text, (New-Object Text.UTF8Encoding $false))`.
Many repo files are CRLF on disk, so normalize before string-replacing
multi-line blocks.

Budget defaults are close to BASPM's own values, per the user's 2026-10-03
instruction: starting funds 25,000 / prestige 250 / 3,000 per season, BASPM's
Normal-difficulty review table, salaries drawn from BASPM's 25-60 range, and
component maintenance from BASPM's `MaintenanceCost` values. `design.md` §2.1(d)
flags that §1.2 normally asks for re-derived numbers; this is the user's
directed exception, to revisit if they prefer stricter re-derivation. The
conversion rate (4 prestige per fund) is my own, unconfirmed. In the harness, the
bot never goes into debt at these values, so the conversion is covered by unit
tests only.

**Slot staffing done (2026-10-03, M5 groundwork):** missions are staffed through
BASPM's mission-control and flight-crew layouts one to one (`StaffingSlot`,
slot-indexed step involvements, `MissionStaffing.autoAssign`/`reassign`, the new
`ReassignControlSlotAction`). Each roll reads the one person in each slot, which
**supersedes the 2026-10-02 averaging rule described below**. Only used slots are
required: crewed Mercury/Vostok need 9 controllers + 1 CAPCOM, not "10 + 1".
Control slots are released after their last season, as in BASPM, and CAPCOM is
re-staffable mid-mission (both confirmed by the user the same day). The
converter (`E:\Temp\BASPM\tools\baspm_pack.py`, pre-slot copy kept as
`.bak-preslots`) now emits the layouts. Details and harness numbers are in
`design.md` §5 M5, "Slot staffing". **Next:** the M5 server gaps G1-G9
(`ui-spec.md` §6) and the libGDX screens as mockups arrive. Reset the dev data
root before a DevClient run: the journaled launch action's fields changed.

**Display name dropped, "callsign" renamed (user, 2026-10-04):** the account
has no display name any more (login and password only, handshake included).
The per-game persona is the agency's **Director** (`directorName`, one title
for both archetypes), chosen once at slot claim. Old `accounts.json` and
snapshots won't load (Jackson is strict on unknown fields), so reset the dev
data root. The same day the user decided on **history notes**: a dedicated
icon with a hover card for optional real-world tidbits (`ui-spec.md`
component 15, `design.md` §1.1/§3.12), with the first one on the join
screen's Director field (Korolev as the secret "Chief Designer"). When a
tidbit turns up during any work, add it to `design.md` §1.1 as a history
note candidate.

**`Theme.java` brought current with the design system (2026-10-04),** ahead
of the libGDX implementation session that will soon start wiring up the
Connect/Join Game mockups. Added the two color tokens the design system
already had but `Theme.java` didn't (`SUCCESS_ON_PAGE`, `COMMIT` +
`ButtonKind.COMMIT`, both previously deferred in `design.md` §3.13 as "not
speculative now" — that deferral is reversed, since catching up later is
now worse than keeping it current), plus style factories for the three
components the Connect/Join Game mockups introduced and `Theme.java` had
never seen: `Shapes.textField`/`optionCard`/`historyNoteCard`. Verified by
compiling `Theme.java` standalone against the cached libGDX jars in
`~/.m2` (a full `mvn clean package` can't run in this sandboxed session —
`jitpack.io`, which `core`'s KryoNet fork dependency needs, is blocked by
this environment's egress policy; not a code problem, just re-verify with
a full build on a machine that can reach it). **Two things this surfaced,
both still open** (full detail in `design.md` §3.13's own paragraph):
the `HistoryNote` open-book icon is still only inline SVG inside the design
system's own component markup, not yet a real texture/SVG asset under
`assets-raw/svg/` the way every other skill icon is; and the design
system's `HistoryNote` popover CSS has a `box-shadow`, the first shadow
anywhere in this design system, which contradicts `Theme.java`'s own
documented "no shadows" rule — `Theme.java` deliberately left its shadow
out rather than silently deciding either way, needs a real decision.
**Going forward: keep `Theme.java` in lockstep with the design system
every time a mockup session adds or changes a component/token** — this
gap existed because three components shipped across two sessions without
a matching `Theme.java` update; don't let that gap reopen now that the
libGDX side is about to start consuming it.

**Connect and Join Game are built in libGDX (2026-10-04),** from the user's
Claude Design mockups (canvas "Launch Window — Connect & Join Game") and the
UI Asset Sheet. What exists now:
- **Screens:** `client.screen` (`ConnectScreen`, `JoinScreen`, and a stand-in
  `AgencyScreen` until the hub has a mockup).
- **Widgets:** `client.ui` (`UiKit`, `FormField`, `OptionCard`, `HistoryNote`, `HistoryNotes`).
- **Networking:** `client.connect`. `NetworkGateway` runs each call on a background
  thread over the session's one connection (see below) and posts the result back
  to the render thread. `LoginResult` tells the handshake's refusals apart without
  a reason code, since the handshake's shape is pinned.
- **Server changes:** `ClaimSlotResponse` gained a `Reason` enum (registered
  last in `MessageRegistry`), and the login rule moved to `core`'s `LoginNames`.
- **Full build re-verified on the main PC** (the earlier `Theme.java` session
  couldn't reach jitpack): `mvn clean package` is green.
- **Assets:** the six font TTFs plus their OFL licenses (`assets/fonts`),
  `assets/branding/logo.png`, and `assets/ui/` (the history-note icon and the
  archetype thumbnails cropped from the spring-noon hub art).

**To check a screen against its mockup, run `ScreenSnapshot`.** It renders on a
hidden window and writes one PNG per state; Read the PNGs afterwards:
```
mvn -q install -pl core -am -DskipTests
mvn -q -pl lwjgl3 test-compile
mvn -q -pl lwjgl3 dependency:build-classpath -Dmdep.outputFile=target/test-cp.txt -Dmdep.includeScope=test
cd assets && java --enable-native-access=ALL-UNNAMED -cp "../lwjgl3/target/classes;../lwjgl3/target/test-classes;$(cat ../lwjgl3/target/test-cp.txt)" de.mkoehler.launchwindow.lwjgl3.ScreenSnapshot <out-dir>
```
**Pitfall:** never use linear filtering on a stretched few-pixel nine-patch.
It turns a flat fill into a gradient (fixed in `Theme.Shapes.rect`).
**Open, for the user:** the copy changes in `ui-spec.md` §4.2's "Implemented"
note need their OK. (The notice/hint-boxes-as-components and the history-note
shadow questions below are both closed now.)

**G1 game info done (2026-10-04),** see `design.md` §5 M5 "Game info": the game date
(turn 1 = Winter 1955, unconfirmed), content packs frozen per game under
`<dataRoot>/content/<packId>/` with a shared `ContentPackHash`, one schedule rule
(`ResolutionSchedule`) for the scheduler and the countdown, and the client showing
date and countdown. **Pitfall:** KryoNet's `Client.stop()` can block up to about 11 s
on Windows after a finished exchange. That was the "weird timeouts" with quick
connect/close cycles. Never close a client on a thread a result waits on.
**Space Complex hub built in libGDX (2026-10-04),** from the hub mockup
(`SpaceComplexScreen`). It replaces the stand-in agency screen and is where every
breadcrumb root and home button now go.
- **Logic:** `client.view.Hub`. **Art:** `assets-raw/make_hub_art.py` (32 JPEGs, cropped
  to 1080 rows).
- **Shared base:** `SessionScreen`, under `ChromeScreen`, gives the hub the toast,
  dialogs, refresh and navigation without the chrome.
- **Unconfirmed choices** are in `ui-spec.md` §4.3's "Implemented" note (time of day
  measured from the last step, no Log out, no Turn Report yet).
- **Pitfall:** hotspot files say `imageHeight` 1088. Flip y against 1080, the cropped
  art.

**VAB built in libGDX (2026-10-04),** from the VAB mockup (canvas "Launch Window
— VAB mockup", private, ask the user for the link): the landing screen, Open Rocket
Program, Manage Rocket Programs, and Rocket R&D (`RndBoardScreen`, shared with Payload
R&D). The hub strip also follows the revised, all-solid mockup.
- **Regression check for shared UI changes:** copy `shots/` first, re-shoot, then diff
  every PNG with PIL's `ImageChops.difference(...).getbbox()`.
- **Pitfall:** a snapshot block that calls `game.setSession` changes the session for
  every shot after it. Add new blocks at the end.

**Headquarters mission planning built in libGDX (2026-10-04)** (`ui-spec.md`
§4.6.5–§4.6.8): configuration selection, steps, the planning hub, and Assemble
mission, up to a real launch.
- **Rules:** the odds come from `core`'s `MissionAssessment`, which is exact, not
  simulated. The launch button follows `MissionLaunch.check`.
- **Drafts:** the plan draft lives on the session (`ClientSession.draft`).
- **Pitfall:** `LaunchRequest` copies `rocketIds` with `List.copyOf`, which rejects
  nulls, so `MissionDraft.request()` sends "" for a missing rocket.

**Mission Control built in libGDX (2026-10-04)** (`ui-spec.md` §4.7; the replay viewer
came with G2, below): the flight controllers, scheduled and active missions with Scrub
and Cancel, and Re-staff. The SET Centre's hire and personnel dialogs are now shared
by department.
- **Pitfall:** `shoot.sh` clears `shots/` first and allows 400 s. A run killed
  part-way leaves stale PNGs that make a pixel diff look clean.

**G2, the turn report and the replay built (2026-10-04).** Each player's turn report is
saved per turn (`players/<id>/reports/<turn>.json`, a `rules.TurnReport`). It's sent as
JSON inside `TurnReportResponse`, and read by `TurnReportScreen` and `ReplayScreen`.
- **Pitfall, a save format now:** `ResolutionEvent`'s `@JsonSubTypes` ids are never
  renamed or reused, and event and report fields are only added, like
  `MessageRegistry`'s append-only rule. See `design.md` §3.7.
- **Pitfall, Kryo's 64 KB object buffer:** the report goes over the wire gzipped, because a
  busy turn's JSON outgrows the buffer, and an oversized message drops the connection.
  The server refuses a report that would still be too big.

**Museum, Settings and the game result built (2026-10-04),** with G3 (rival summaries,
`RivalsRequest`) and G10 (`PlayerSnapshot.memorial`) plus `PlayerSnapshot.goalRecords`, both
approved by the user as additive save fields. The client reads the race through
`WinCondition`'s goal-time helpers, never its own port. The hub strip has a gear for
Settings.

**Personnel portraits via ComfyUI (2026-10-04):** the user's ComfyUI (Qwen Image 2.1) runs
locally on port 8188, and `assets-raw/portraits/make_portraits.py` queues the user's portrait
workflow over its HTTP API (`portraits_workflow_api.json`, the workflow's subgraph flattened into
API format). It writes `assets-raw/portraits/<nasa|soviet>/*.png` and `manifest.json` (each
portrait's role, sex, ethnicity, age, prompt and seed; reruns give the same prompts and seeds and
skip existing files). 100 per agency so far, generated in blocks of 50 with their own random
sequence each, so raising `--count` adds portraits without changing earlier ones. The script also
writes `core/src/main/resources/personnel/portraits.json`, the trimmed catalog the game reads.
**Candidates are named after their portrait (2026-10-05):** `HiringPool` picks a portrait first, and
`PersonnelNames` draws the name (lists per ethnicity and sex in `personnel/names.json`) and the age
(the portrait's age range) to match. A new ethnicity in the art needs a `names.json` entry and a key in
`make_portraits.py`'s `ETHNICITY_KEYS`; `PersonnelNamesTest` fails until both exist. Details are in
`design.md` §5 M5, G5. **Age is a trade-off too (user, 2026-10-05):** older candidates start more skilled,
younger ones with higher learning potential, measured against each group's career
(`HiringPool.experience`). The skill shift is centred on the group's average candidate
(`experienceBonus`), so it's a trade-off and not a difficulty change. Tune it with `dev-tools`' `CareerCurves` (rookie-vs-veteran skill curves);
numbers in `design.md` §5 M5, G5. The hire screen shows "AGE 41 · RETIRES IN 24 YRS". **Portraits are shown in the client (2026-10-05):** rerun
`assets-raw/portraits/make_portrait_assets.py` after generating new portraits. It writes the 128 px
greyscale JPEGs to `assets/ui/portraits/` (RGB on purpose: libGDX loads one-channel images as alpha).
`ScreenSnapshot`'s `withPortraits` gives fixture people matching portraits and names. 256x256 is the smallest size the model holds together at. ComfyUI keeps its
own copies in `E:\Comfy-Desktop\ComfyUI-Shared\output`; never delete those. The portraits are
the user's own generated art (`assets-raw/` rule).

**Dev mode for play testing (2026-10-04):** set `"devMode": true` in the server's
`game-meta.json` and restart it. The hub strip then shows "Dev · Resolve turn now", which
resolves the open turn at once (the day's scheduled resolution still comes). Dev mode also
allows `DevClient`'s `funds` cheat.

**Font pitfall (2026-10-04):** fonts are generated at the monitor's scale and drawn scaled
down, so `BitmapFont`'s default integer positions rounded each glyph apart and made baselines
wobble. `Theme.Fonts` turns them off. Mipmaps were tried and made text greyer, so the filter
stays linear. The user will judge the fonts again once the packaged client can use Windows'
high-DPI setting (libGDX otherwise ignores high DPI, as in the sibling projects).

**Astronaut Complex and Public Affairs Office built (2026-10-04),** which completes
`ui-spec.md` §7's phase 4; every hub building now opens a screen. **Known limit:** the client
uses `BudgetSchedule.baspmClose()` for the review table, the forecast and conversions, because
the schedule isn't sent to the client. **Next:** ask the user (the remaining spec work is the
later phases and the open gaps in `ui-spec.md` §6).

**Headquarters built in libGDX (2026-10-04),** as far as opening a program, from the
Headquarters mockup: landing with the building panel and upgrade, Manage open
programs, Select celestial body, a body's programs, and Program details with Open
and Close. The user confirmed the program, category and solar-system art is their
own; `assets-raw/make_content_art.py` converts it. G9 is partly done (categories
default to Earth). See `ui-spec.md` §4.6's "Implemented" note for the unconfirmed
choices (no Closed state, no First ribbon). **Next:** the VAB (its mockup exists, ask
the user for the link if needed) or the hub.

**SET Centre built in libGDX (2026-10-04),** from the SET Centre mockup and the old
Personnel Recruitment mockup: overview with Training, Confirm and Manage department
dialogs, Payload R&D, and Hire, on a shared `ChromeScreen`. Also done: G5 candidate
names (`rules.PersonnelNames`) and G8 department salary adjustment
(`rules.DepartmentSalaries`, `AdjustSalariesAction`). See `design.md` §5 M5 and
`ui-spec.md` §4.8's "Implemented" note for the choices made while building them, all
confirmed by the user the same day (including G8's missing per-season limit). **Pitfall:** never
create drawables in a rebuild loop; use `UiKit.rect`/`icon`/`cardAccent`, which cache.
**Next:** the hub (its mockup exists), then the rest of `ui-spec.md` §7's first loop
(Headquarters opening a program, VAB rocket programs).

**Decisions checklist (2026-10-05):** `DECISIONS.md` gathers every still-unconfirmed choice with
multiple-choice options and a recommendation each. The user fills it in after the first play test.
When it comes back, write each outcome into `design.md`/`ui-spec.md` (and the code where it changes
anything), then remove the settled items from the file.

**Laika on the memorial (user, 2026-10-05):** `MissionConfiguration.animalPassengers` (only
Sputnik 2) puts animals that don't come home on the memorial (`MemorialEntry.Cause.PASSENGER`).
- **Pack hash changed:** reset the dev data root.
- **Converter:** emits the field from its `ANIMAL_PASSENGERS` map.
- **Memorial photos:** in `assets-raw/memorial/`, converted by `make_portrait_assets.py`, with the
  crops set in `crops.json`.
- **Laika's photo:** CC BY 4.0, from Wikimedia Commons. **Credit every third-party asset in
  `assets/CREDITS.md`**; the photo's source notes are in `assets-raw/memorial/SOURCES.md`.
  It's credited to the Museum of Cosmonautics / Main Archive of Moscow (Mos.ru).

**G4 display text done (2026-10-05):** `assets/content/early-era/strings.json` names every id, and
most entries also carry a description and, where there's a real story, a history note.
`ContentNames` looks them up once `ClientSession.start` has installed the pack's text.
- **Sources (user decision):** content follows BASPM's data, with the text rewritten in our
  own words. Extract the facts, then write from the facts, not from BASPM's sentences.
- **Verify the figures** in every history note before using it, because BASPM's facts
  contradict each other.
- **Not frozen:** `strings.json` is deliberately outside `ContentPackLoader.FILES`, so it's
  neither hashed nor frozen. Never add it there.
- **Checks:** `ContentStringsTest` checks coverage, stray keys, lengths and font
  characters. When the pack grows, add text for every new id or the test fails.

**Connect screen remembers the last login (user, 2026-10-05):** server, login and password of the
last successful login live in `~/.launch-window/client-settings.json` (`client.settings`, ported from
Robot-Rampage's `SettingsStore`), so Enter logs straight in. Clear text on purpose (`design.md` §5's
accepted-risk note). `ScreenSnapshot` overrides `createSettingsStore()` so shots never read the real file.

**One long-lived connection per client session (user, 2026-10-04),** replacing
the connection-per-call design (`design.md` §3.3). `NetworkGateway` logs in once
at Connect, reuses the connection, reconnects and logs in again by itself if it
has dropped, never repeats a call that broke mid-exchange, and closes off-thread.
`ClientGatewayIntegrationTest` proves the reuse and a reconnect after a server
restart. **Next:** main-menu groundwork (art by season and time of day, hotspots,
hover; the hub stays chrome-free), then G5 names, then G8 salary adjustment.

**Mockup feedback from the libGDX build, closed out the same day (2026-10-04).**
Seven items came back from implementing Connect/Join Game; all are done now:
- `Notice` (warning/danger/info) is now a real design-system component, plus a
  new `body-strong` type token for its bold lead-in — no longer `Theme.java`-only.
  The Connect/Join Game mockups dropped their local `.banner`/`.hint` CSS for it.
- The mockups themselves picked up every wording/color/layout fix the
  implementation made: Join Game's heading ("Found your agency", not BASPM's
  product name plus an unknowable date), its hint text (no longer claims the
  game is "already in progress" or implies fixed starting funds), its eyebrow/
  heading/footnote (switched from navy/ink to the on-page colors, since they
  sit directly on the blue page), a bold lead-in on its own line instead of
  inline (libGDX can't mix fonts in one label), and a "Pick an agency style."
  state the original spec didn't cover.
- The UI Asset Sheet now notes the history-note icon is a real SVG+PNG asset
  pair, not just design-system markup.
- **The history-note popover's `box-shadow` is resolved (user, 2026-10-04):
  drop it.** It was the only shadow this design system had and read as out
  of place; the design system's CSS now matches `Theme.java`'s own flat
  `historyNoteCard()` exactly — no shadow anywhere in this system, this
  popover included.

**Space Complex hub chrome: decided — no chrome (user, 2026-10-04).**
`ui-spec.md` §4.3 left chrome-or-not an open proposal; both got mocked up
side by side in a new canvas ("Launch Window — Space Complex Hub", private,
ask the user for the link) to compare directly instead of guessing. The
no-chrome version (full-bleed art, the real `LW_main_menu_NASA.hotspots.json`
7-building hover highlights, a slim overlay strip for the hub's own info)
won outright — the user judged it clearly better than the with-chrome
version (which letterboxes the hero art) and had that artboard deleted,
not kept around as a rejected alternative. `ui-spec.md` §4.3 and `design.md`
§3.13 both updated to record this as decided, not proposed. Two new icons
(clock, document) went into the UI Asset Sheet the same day. **Next:** this
can now become the real `lwjgl3` screen whenever that's picked up.

**SET Centre mockup built — Personnel overview + Payload R&D (2026-10-04,
later the same day, per `ui-spec.md` §7's suggested order).** One two-artboard
canvas (`Launch Window — SET Centre mockup`, private, ask the user for the
link). This needed a real design system change first: component 6 (Action
Result Toast) has no green "success" case among `Notice`'s three existing
variants (warning/danger/info), so a fourth, `success`, was added (bundle.css/
index.d.ts/`Notice/README.md`/`design-system.json`) and ported into
`Theme.java`'s `NoticeKind` enum (`SUCCESS`, plus its `textColor()`/
`Shapes.notice()` cases) the same session, keeping the two in lockstep per
the standing commitment from the Connect/Join Game session above. The
mockup itself builds out two previously-unbuilt reusable components for
real: the **Training dialog** (component 12 — Basic/Advanced/Permanent, a
skill picker reusing the five `SetSkill` icons, a target-level slider for
Permanent) and the **Manage department dialog** (component 13 — the ±50%
one-off salary slider with a live projected-salary/projected-morale
preview, per BASPM's own one-off-adjustment shape). Personnel rows
(component 1) use `Badge variant="active"` for anyone not idle (in
training or researching) and a plain muted pill for Idle/Unavailable,
since Badge has no neutral variant and gold's "active/in-progress"
semantic already fits exactly; SET never gets Expert stars (that trait is
Astronaut/Mission-Control only), so the mockup doesn't render any. The
Payload R&D board demonstrates all of component 10's pieces on four
fixture components, including one sitting exactly at its R&D ceiling with
a green "Cap reached" badge and its four slots already auto-released — the
literal mechanic `ResearchProgress.releaseResearchersIfMaxed` (M2,
mission-simulation slice) implements server-side. **Also fixed in this
mockup, not just avoided going forward:** the existing Personnel
Recruitment mockup's breadcrumb conflates "Headquarters" with the actual
hub/home screen (its home icon's `aria-label` even says "Home —
Headquarters") — a leftover from before `ui-spec.md` §3's flow was
finalized, where **Space Complex** is the true hub and Headquarters is an
ordinary sibling building. The SET Centre mockup's breadcrumb root is
"Space Complex" throughout; the Personnel Recruitment mockup itself was
left as-is (out of scope unless asked). No new icons were needed — the
board reuses the five SET skill icons already in the UI Asset Sheet.

**VAB mockup built — Overview, Open Rocket Program, Manage Rocket Programs,
Rocket R&D (2026-10-04, later the same day, next in `ui-spec.md` §7's
order).** Four linked artboards (`Launch Window — VAB mockup`, private, ask
the user for the link). First real build of **component 7, the Building
Panel** (level/cap/rocket-class gate/upkeep, a real Upgrade flow with a
price breakdown and confirmation, then an "Upgrading — 1 season left"
state) — reusable as-is for the other four capped buildings. Also the
first use of **component 4, the Price Breakdown** (line items + a total),
built as plain local markup rather than a design-system component, same
posture as the modal-dialog pattern the SET Centre mockup introduced:
nothing has needed either one twice yet, so neither is promoted. Open
Rocket Program (4.5.1) lists not-yet-open rockets with real
`ROCKETS`/`HUMAN_RATED_ROCKETS` payload figures (suborbital/LEO/TLI/TPI,
straight from `MissionComponent.RocketSpec`'s own fields) and demonstrates
`ROCKET_CLASS_TOO_LARGE` (a Medium-class rocket greyed out against a
Level-1, Light-only VAB). Manage Rocket Programs (4.5.2) shows reliability
bars (component 3) for the two already-open rockets and demonstrates
`MISSIONS_IN_FLIGHT` (Close refused, with the navy refusal toast, for a
rocket flagged on a mission). Rocket R&D (4.5.3) reuses the SET Centre's
R&D-board pattern (component 10) verbatim, retargeted at rocket components
— real assign/release interactivity, same two rockets as Manage Rocket
Programs for narrative consistency. Breadcrumb root is "Space Complex"
throughout. No new design-system components, tokens, or icons were needed.
**Footer fix (user, 2026-10-04):** the footer must stay at the bottom of the
frame. It had ridden up under short lists in the VAB and SET Centre mockups.
Both are fixed, and the rule is in `design.md` §3.13. **Mockup pitfall:** a
style on an `x-import` only sizes its slot, so `flex: 1` on a
`LaunchWindow.Card` doesn't fill. Put the fill and scroll on a plain wrapper
`div`, and give the footer `margin-top: auto`.

**Headquarters mockup built, as far as opening a program (2026-10-04,
`ui-spec.md` §4.6.1–§4.6.4).** Five linked artboards (`Launch Window —
Headquarters mockup`, private, ask the user for the link):
- **Data:** a Soviet agency, the early-era pack's real costs, and the real
  solar-system, category and program art, uploaded to the canvas as JPEG
  copies.
- **First builds:** the goal badge (component 8, a circle proposal, not yet
  the `logo.svg` patch frame), plus the price breakdown with discounts and
  owned components.
- **Refusals:** `HEADQUARTERS_FULL` and `GOALS_NOT_MET`.
- **History note:** Gagarin's parachute landing, on Vostok. Belka and
  Strelka are recorded as a candidate in `design.md` §1.1.
- **Phase 2 of `ui-spec.md` §7 is now fully mocked up.**

**Mockup pitfall:** never put a `<button>` (or a mounted `LaunchWindow.Button`)
inside an `<a href>`, because the button swallows the click. Style the `<a>`
itself with `class="lw-btn lw-btn--primary label"`. This was fixed in the VAB
and SET Centre canvases.

**Seven components promoted to the design system (2026-10-04, at the libGDX
session's request):** `Dialog`, `FilterChips`, `SegmentedControl`,
`ReliabilityBar`, `SkillBar`, `RdSlot` and `IconButton` (the white on-page
button). Each has a README and a preview. The UI Asset Sheet gained a
"Components" artboard with specimens and `Theme.java` build notes.
`design.md` §3.13 has the details. `RdSlot`'s Release is now navy, not red.
**Open:** the libGDX session should align its unpushed code with these names
and values. The mockup canvases still use their local copies.

**Content findings closed (2026-10-04),** see `design.md` §3.13.
- **Explorer 1's discount:** it's BASPM data that only its GSA faction could
  use. `baspm_pack.py` now drops cross-agency discounts (backup:
  `.bak-crossagency`).
- **"Human in space" and "Human in orbit"** are earned through the goal cascade.
  In pack terms, "cancels" means "also credits, with prestige"
  (`rules.GoalCascade`).

**Pitfall:** don't read `cancelsGoalIds` as "makes unearnable".

**Headquarters mission planning mocked up (2026-10-04, `ui-spec.md`
§4.6.5–§4.6.8, the start of §7's phase 3).** Four new artboards on the
Headquarters canvas (bottom row), following Vostok Orbital: configuration
selection, steps and goals, the planning hub (assessment, rockets, payload
R&D), and Assemble mission. They're the first mockups to mount the promoted
components. Details and the unconfirmed proposals are in `design.md` §3.13.
- **Assessment numbers:** the mockup derives the odds from `core`'s formulas.
  `ui-spec.md` G7 now asks for a `core` helper that returns them, so the
  client doesn't re-derive them.
- **New icon:** a stand-in Flight Director icon,
  `assets-raw/svg/icon_flight_director.svg`, added to the UI Asset Sheet with
  the ten astronaut and mission-control skill icons.
- **Preview tool for mockups:** a canvas artboard can be screenshotted
  locally. Read the canvas's `artifact-type/dc-runtime.js`, save it next to
  the `.dc.html` files as `support.js`, and serve the folder over HTTP.
  Headless Chromium takes the screenshot; the window needs about 87 px of
  extra height. Copy the `/_blob/` assets into a local `_blob/` folder and
  serve SVGs as `image/svg+xml`.

**Hub strip made solid (2026-10-04, libGDX feedback):** the see-through Next
turn and Turn Report chips were unreadable on bright art. Every strip element
is now a solid chip (navy nameplate, white chips, white Turn Report with a
navy outline), and the fade is gone. **Lesson:** check anything drawn over the
hub art against the brightest variant (Soviet winter noon) and the darkest
(winter night), not just one sample. The hub canvas's dev panel switches the
art for this. See `design.md` §3.13.

**Mission Control mocked up (2026-10-04, `ui-spec.md` §4.7):** a new canvas,
`Launch Window — Mission Control mockup` (private, ask the user for the link),
with five artboards: the controller overview, scheduled missions with Scrub,
active missions with Cancel, re-staffing, and the step-by-step replay viewer
with its season summary. It continues the Headquarters canvas (1960 · S3, right
after Vostok Orbital's launch). Details and unconfirmed choices are in
`design.md` §3.13. `ui-spec.md` G2 now lists every field the replay reads.
**The turn report (§4.4) is on the same canvas, now titled "Mission Control &
Turn Report mockup", which completes §7's phase 3.** Next in §7 is phase 4:
Astronaut Complex, Public Affairs Office, Museum, settings, game result. **Pitfall:**
the design system serves no `tokens.css`; send the local copy as a file when
installing it on a new canvas.

**Goal cascade in the mockups (2026-10-04):** the Headquarters steps screen
shows "Also credits …" under a step's goals, following `rules.GoalCascade`
(see "Content findings closed" above).

**Astronaut Complex and Public Affairs Office mocked up (2026-10-04, `ui-spec.md`
§4.9/§4.10, first in §7's phase 4, so play testing can start):** a new canvas,
`Launch Window — Astronaut Complex & Public Affairs Office mockup` (private, ask the
user for the link). Expert training is a fourth kind in the Training dialog, and the
Public Affairs Office shows the budget, the next-review forecast, the review table,
news, and the debt and multiplier states. Details and the unconfirmed choices are in
`design.md` §3.13.
- **Negative funds on the blue page** are a solid `danger` tag with white text, now a
  documented rule of the design system's `danger` token and `Theme.DANGER`. A red text
  color was approved by the user but failed contrast (3.7:1), so no new token.
- **Expert training as a fourth Training-dialog kind** is confirmed by the user.
- **Turn report fixture corrected:** its budget had invented numbers, and it put review
  #2 in 1960. Reviews come every 16 of the player's seasons (1959 · S1, 1963 · S1, …).
- **Pitfall:** a page's own `renderVals` key named like a `chromeVals` key (for example
  `fundsCls`) is silently overwritten by the chrome, because `chromeVals` is merged last.
- **Phase 4 is done too (2026-10-04, later the same day):** the Museum (four tabs),
  Settings, and the game result, on a new canvas, `Launch Window — Museum, Settings &
  Game Result mockup` (private, ask the user for the link). Details are in `design.md`
  §3.13. New server gap: G10, the memorial list.
- **Hub Settings gear decided (user, 2026-10-04):** a white icon-only gear at the end
  of the hub strip opens Settings (and so Log out). It's in the hub mockup too.
- **Default declared goals are preliminary (user, 2026-10-04):** they end the race at
  the first crewed orbital flight (the cascade credits all three). The full game will
  declare extended goals.
- **Mockup tip:** `<sc-for>` inside a CSS grid lays its items out as grid cells, so a
  table can be a grid with one `sc-for` per row.
- **Trophy icon added (2026-10-04):** `assets-raw/svg/ui/trophy.svg` and
  `assets/ui/icons/trophy.png` (via `make_ui_icons.py`, which needs `pip install
  resvg-py`), and on the UI Asset Sheet with the news arrows (which reuse the sort
  glyphs).

Everything below is history; if this paragraph and a later one disagree, the
later one wins.


**Mission prestige, sub-slice 1, done (2026-10-03):** `MissionPrestige`,
`FlightTier`, `MissionPrestigeChanged`, plus defaulted constructors on
`MissionStep`/`MissionConfiguration` so existing content is unchanged.
`MissionProgress` accumulates per season, a failing step replaces the season
total (forfeiting that season's earlier prestige, `baspm.md` §11.7), prestige
floors at zero, and a flight counts only when it completes. **Pitfall worth
remembering:** the tier is read live per mission, not stamped at launch -
an advisor review caught that a launch-time stamp let simultaneous copies of
one configuration all pay first-time. Also a pitfall, from this same session:
`mvn clean package` does not install `core` into `~/.m2`, so a standalone
`exec:java` in `dev-tools` ran a stale `core` jar and reported a false
mismatch. Reinstall `core` first (`mvn -pl core install -DskipTests`), as
below says. Harness numbers are in `design.md` §5/§3.10.

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
before `TrainingProgress` once both get wired into one pass. **Since
superseded:** `PERMANENT` training, hiring, the Expert trait, and
mission-based growth's caller are all done (see the later paragraphs).
**Still open in the personnel slice:** `BASIC`/`ADVANCED`/`PERMANENT`
training have no player start action (only the Expert and pull-out
actions exist), and submitting a hire as a player action has no dispatch
path yet. Not wired into `PlayerStateService`/`TurnScheduler` - that is
M4 scope, not M2's.

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
still open in the personnel slice (the player start actions, see the
personnel slice's own status paragraph further down) and what's next
(programs/mission-configuration slice, since done).

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
§3.4 for the full proposal and the open question — **superseded later
the same day once the trace existed, see below**). `PersonnelDeparture`
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

**R&D-to-reliability formula traced against real BASPM source and
upgraded to match, same day (2026-10-01), once the user was back on the
machine with `E:\Temp\BASPM`/game-install access — this is the "check a
formula once back on the main PC" follow-up flagged during the prior
laptop session.** `baspm.md` §10.8 (new) traces
`MissionComponentsManager.PerformRDProcessOnPlayerMsnCmp` plus the loose,
previously-missed `SPM-Windows_Data\Scripts\Data Scripts\General
Container\MissionComponents.xml`/`MsnCmpsManagerConstants.xml` (unpacked
on disk, not in the `extracted/`/`monobehaviours/` dump — same fix applied
to §9.4's `MoraleUpperLimit` dead end the same session, see below): BASPM's
real mechanic has **two reliability ceilings per component** (a separate,
lower `MaxRDReliability` R&D alone can reach, below the absolute
`MaxReliability` only flying missions can approach) and **up to four SET
employees R&D the same component at once, in explicit slots with sharply
diminishing per-slot reward bands** (first slot richest, fourth poorest),
not the single-assignee formula `design.md` §3.4 had proposed as an
unconfirmed placeholder. **Presented as two explicit decisions, both
decided (user, 2026-10-01): adopt the two-ceiling shape, and adopt the
full multi-slot mechanic rather than simplifying it away.** Implemented
the same session: `MissionComponent` gained `maxRdReliability`/
`rdSlotBandMaxAmounts` (23 constructor call sites updated across 5
files), `SetSpecialist` gained `rdSlot`, `SkillGrowth.reliabilityGain`
was removed and replaced with `rdReliabilityGain`/`drawRdSlotRawAmount`/
`rdSlotBandLower`/`rdSlotBandUpper` (the now-dead
`RD_RELIABILITY_FACTOR_MIN`/`MAX`/`drawRdReliabilityFactor` removed too),
and `ResearchProgress` was rewritten around a per-component (not
per-specialist) pass — a component's headroom factor is now computed
once per season and shared by every occupied slot, matching BASPM's own
code exactly, which **supersedes the sequential "stacks in roster order"
design this same linkage shipped with earlier the same day**. Per-employee
event attribution (there's no such concept in BASPM once slots are
summed) is Launch Window's own addition: each `ResearchCompleted`
reports a proportional share of the season's actually-applied,
post-clamp total, so summing every event's `reliabilityGain` always
equals the component's real delta. `baspm.md` §9.4 also got the same
loose-XML-file fix applied retroactively: `PersonnelManagerConstants.xml`
*is* recoverable after all (same lesson, different file) — BASPM's own
"Poor" hiring tier only appears in its two lowest morale bands (≤50 at
20%, ≤60 at 10%), directionally consistent with the harness's own numbers
but not a confirmed match, since Launch Window's `HiringPool` picks one
deterministic skill band per roster while BASPM mixes probabilistic
profiles within six wider tiers — plausibly the same underlying shape,
not proven the same mechanism (corrected from an earlier, over-strong
version of this same claim the same session). Wired into `PersonnelLifecyclePlayouts`
the same session: the bot now claims a free R&D slot (0-3) when assigning
a `SET` specialist to a component, falling back to training if all four
are already taken, with two new invariants (slot in range and paired with
`ASSIGNED` exactly, no two specialists ever share a (component, slot)
pair). **Two more real issues were caught and fixed in the
post-implementation advisor pass, both reflected in the final numbers
below:**
1. `advanceComponent` returned early without emitting a
   `ResearchCompleted` event (dropping the already-applied flat skill
   gain from the report, not just the reliability gain) whenever a
   component's nominal reliability gain was zero for the season — common
   once a component sits at its new `maxRdReliability` cap. Fixed to
   always emit one event per occupied slot, 0-share when there's nothing
   to distribute.
2. **BASPM itself auto-releases every SET employee from a component once
   it reaches its R&D ceiling** (`baspm.md` §10.8's own
   `ProcessAndRetrieveAllMissionComponentsThatReachedMaxRDReliabilityAtSomePointDuringTheLastSeason`/
   `UnassignAllSETPersonnelFromMissionComponent`, confirmed by reading
   `Player.cs` directly) — missed in the first implementation pass, which
   would have left specialists parked on a maxed component farming flat
   skill gain forever, a mechanic the user's "adopt BASPM's full
   mechanic" decision didn't actually choose. Ported as a new
   `ResearchComponentMaxed` event + release-to-`IDLE` logic in
   `ResearchProgress`, with the unconfirmed nuance that BASPM triggers it
   from interseason news-item generation rather than inside the R&D pass
   itself. This needed one harness fix too:
   `PersonnelLifecyclePlayouts`'s own reliability-gain aggregation
   blindly cast every research event to `ResearchCompleted`, which threw
   on the first `ResearchComponentMaxed` — fixed via `instanceof`.

Also fixed the same pass: §9.4's correction below had overstated what the
newly-found `PersonnelManagerConstants.xml` actually shows (BASPM's
"Poor" hiring tier is reachable up to `MoraleUpperLimit="60"`, not only
`"50"`, and the two systems' mechanisms aren't directly comparable —
softened to "plausibly similar," see `baspm.md` §9.4 for the corrected
text), a magic `0.1` threshold copied verbatim from BASPM was replaced
with a named, Launch-Window-own `ResearchProgress.HEADROOM_STAGNATION_THRESHOLD`
constant per `design.md` §1.2, a unit test was using a real BASPM
component's exact `L1`-`L4` values (also a §1.2 violation, swapped for
arbitrary numbers), and the class Javadoc's claimed "roster order" was
corrected to the grouped order the code actually uses.

**Final numbers for this slice (same 200-game/80-season run, shifted
several times across the issues above — the release mechanic in
particular is a real behavioral change, not just a random-stream shift,
since freed specialists get reshuffled by the bot sooner instead of
sitting parked doing nothing): 223 resigned (avg. tenure 31.3), 1,244
retired (avg. tenure 55.2), 5,365 still employed, 73,553 training
completions (avg. gain 5.11), 3,832 hires, hiring-pool bands
0/573/20,384/27,043, 599 programs opened, 36,285 R&D seasons completed
(avg. reliability gain 1.53).** Full `mvn clean package` passes (328
tests). See `design.md` §3.4/§3.10 for the full decided formula writeup
and harness numbers.

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
tuning constants (a 65 crew-blend hardware threshold, a 20% crew-blend
weight, and two saving-throw band-size fractions) are unconfirmed
defaults, re-derived to values different from BASPM's own code-confirmed
numbers (a 70 threshold, a 25% weight, from
`ComputeProbabilityOfSuccessForMissionStep` itself, not `Constants.xml`)
per `design.md` §1.2 — see
`design.md` §2.5(d) for the full writeup.

**Step/season content shape done the same day (2026-10-01):**
`MissionStep`/`MissionSeason` plus a reshaped `MissionConfiguration`
(`core/.../content`, `durationInSeasons()` now derived from
`seasons.size()` rather than stored) — involvement expressed against
skills/components, never specific personnel, so it stays decoupled from
who is actually assigned. **One real formula bug the advisor caught
before this landed:** the first draft let `crewInvolvements` sum to 100
or less, like `controllerInvolvements` — but
`MissionStepResolution.computeBaseProbability`'s blend is a weighted
average, so a partial crew sum reads as a skill shortfall and quietly
*lowers* the blended probability, even with maximally skilled crew.
Fixed: `crewInvolvements` is now either empty or sums to exactly 100;
`controllerInvolvements` stays "100 or less" since partial controller
participation is a real content lever, and an empty list now explicitly
means the saving throw can never succeed. Every involvement percentage
is 1-100, not 0-100 (a zero-percent entry is indistinguishable from "not
involved"). `ContentCatalog` additionally requires every step's
component ids to both exist and be required by the step's own mission's
program. **Crew-blend shape confirmed (user, 2026-10-01) — see
`design.md` §2.5(e)'s own "Blend shape confirmed" paragraph:** the
trade-off was surfaced first - the weighted-average blend means involving
crew *lowers* success odds whenever crew skill is below the component sum
(the ordinary case), which sits oddly against `baspm.md` §8.2's "a bonus
on top of already-solid hardware" description, an additive bonus formula
would fit more literally - but the user chose to keep the simpler,
already-implemented weighted-average shape as-is rather than rework it.
**Superseded the same day, see directly below:** this paragraph originally
left the per-player in-progress-mission state, the resolver, and the
`AssignmentState.ASSIGNED` re-confirmation all open - all three are done
now.

**Per-player mission state, launch, and season resolution done
(2026-10-02):** `InProgressMission` (`core/.../model`, Kryo-registered)
plus `MissionLaunch`/`MissionProgress` (`core/.../rules`) close out the
list directly above — the per-player state, the resolver, and the
`AssignmentState.ASSIGNED` re-confirmation are all done now, not just the
pure formula/content shape. `Personnel` gained `assignedMissionId`
(nullable; for Astronaut/Mission Control, `ASSIGNED` now means "on this
mission instance," the same way `SetSpecialist.rdTargetComponentId`
already disambiguates SET's own `ASSIGNED`). Two user decisions:
crew/controller skill contribution for a given skill is the **average**
across every assigned person, not a sum (a sum would push the weighted-
average blend's inputs past the 0-100 scale §2.5(e)'s own fix depends
on); a `CATASTROPHIC` outcome permanently loses the **entire** assigned
crew, not one random member (matching real catastrophic accidents -
Apollo 1, Challenger, Columbia - with no partial survival; `baspm.md`'s
own `Player.AddFallenAstronaut` confirms BASPM permanently removes a
named astronaut here, though the exact who/how-many rule needs
`E:\Temp\BASPM` access to trace, flagged as `baspm.md` §8.4 item 6 for
later). Lost astronauts are reported as `PersonnelDeparture` with a new
`DepartureReason.LOST_IN_ACCIDENT`, reusing the existing event record
rather than inventing a parallel one. Composition order:
`PersonnelLifecycle` → `MissionProgress` → `TrainingProgress` →
`ResearchProgress` → `HiringPool.refreshCandidates`. **Wired into the bot
harness the same day:** three original fixture mission configurations
(`sputnik-launch-std`, `explorer-mission-std`, `mercury-mission-std` -
one uncrewed single-season, one uncrewed multi-season, one crewed
multi-season with every step flagged crew-risk) and a new
`maybeLaunchMission` journaled-action stand-in. **A real bot-shape
change this forced:** an Astronaut/Mission Control officer's idle-to-
`ASSIGNED` transition used to be a direct per-employee coin flip inside
`shuffleAstronaut`/`shuffleMissionControlOfficer` - since `ASSIGNED` now
always points at a real mission, that path moved entirely into
`maybeLaunchMission`, and the per-employee shuffle only decides idle vs.
training now. New invariants: the bidirectional `ASSIGNED`/
`assignedMissionId` pairing (both directions - the person's own state and
every `InProgressMission`'s own assigned-id lists), nobody on two
missions at once, season index in range, and (added once exact counts
landed) every mission's own assigned-list sizes match its configuration's
declared counts. **Numbers (same 200-game/80-season/seed-42 run, after
both balance-hole fixes below): 2,138 missions launched, mission steps
1,610 successful / 420 minor-glitch-solved / 475 minor glitch / 650
failure / 37 catastrophic (all from `mercury-mission-std`, the only
crew-risk configuration), 74 astronauts lost in accidents, 17 retirements
specifically via the new retire-on-release check (already included in
the 1,032 total retired, not additional).** **The personnel numbers
shifted a lot more than expected versus the pre-mission-sim baseline, and
in the opposite direction from a first guess** - resignations roughly
*quadrupled* (1,098 vs. 223 before) and the Excellent hiring band fell by
more than half (12,604 vs. 27,043), meaning *more* idle time, not less.
Cause, not a bug: Astronaut/
Mission Control officers lost their old unconditional coin-flip into
`ASSIGNED` (busy-morale bonus to roughly half of them every reassignment
roll) and now only reach `ASSIGNED` via `maybeLaunchMission` actually
firing (15% chance/season, exactly however many a picked configuration's
own counts require) - otherwise they sit `IDLE` losing morale every
season instead. **Two balance holes this surfaced were both
resolved the same day (user, 2026-10-02), not left open:**
`MissionConfiguration` now declares its own exact
`requiredAstronautCount`/`requiredMissionControlOfficerCount` (replacing
the earlier content-derived boolean shape - `MissionLaunch` rejects any
other count, via new `WRONG_ASTRONAUT_COUNT`/
`WRONG_MISSION_CONTROL_OFFICER_COUNT` outcomes); and `MissionProgress`
now retires an over-age crew/controller on release instead of returning
them to `IDLE` (closing the free-relaunch loophole - a `retirementAgeFor`
helper moved to `PersonnelLifecycle`, package-private, shared by both
resolvers). Harness and numbers updated to match (`maybeLaunchMission`
now picks exactly a configuration's own declared counts via
`pickExactlyIdleIds`, not a random 1-2) - see `design.md` §3.10 for the
full before/after numbers tables, including the second pass after both
fixes landed. **This completes M2's own mission-simulation exit
criterion** (a multi-season mission spanning several turns, bot-driven)
— still open for this slice (as of before the next paragraph): post-mission
reliability change, mission-based skill growth, prestige/goals, the
mission's own cost at launch, the goal-dependency reliability modifier,
and the Expert trait. **Superseded by the later paragraphs:** every item
on that list is now done except prestige/goals and the goal-dependency
modifier.

**Post-mission reliability change and mission-based skill growth done
(2026-10-02):** both resolved once per in-progress mission *per season*,
right after that season's steps resolve, regardless of outcome -
`MissionProgress.applyPostSeasonReliabilityChange`/
`applyPostSeasonSkillGrowth`, re-deriving `baspm.md` §10.4/§9.3/§9.4's
own "fires on both the success and failure path, once per mission"
shape onto Launch Window's season-incremental model. **Decided (user,
2026-10-02): reliability change aggregates per component across the
whole season, not per individual step** - for each component involved
in any step the season actually executed, the worst outcome category
wins (`FAILURE` > `MINOR_GLITCH` > `SUCCESS`; `MINOR_GLITCH_SOLVED`→
`SUCCESS` and `CATASTROPHIC`→`FAILURE` are both unconfirmed-default
mappings, `baspm.md` only traces a four-way taxonomy with no fifth,
worse-than-`FAILURE` tier). New `SkillGrowth.missionReliabilityGain`/
`missionReliabilityLoss` re-derive §10.4's own shape: gain grows toward
`maxReliability` the same diminishing-returns way R&D does; loss shrinks
toward `OwnedMissionComponent.getFloorReliability()`, scaled by the
failing step's own involvement percentage (only the failure case is
involvement-scaled, matching the trace). Reaching `maxRdReliability`
this way releases any SET researchers still assigned there, via a new
shared `ResearchProgress.releaseResearchersIfMaxed` helper now called by
both this path and `ResearchProgress`'s own R&D pass, rather than
duplicating the release logic (`baspm.md` traces both triggers calling
the same underlying method). Mission-based skill growth re-derives
§9.3/§9.4's own shape with `SkillGrowth.diminishingReturnsGain`: every
surviving astronaut grows **all five** `AstronautSkill` values every
season flown; every surviving controller grows every
`MissionControlSkill` that appeared in *any* step that season -
deliberately broader than BASPM's own "one skill they were actually
using," since Launch Window's averaged-contribution crew model has no
per-person skill ownership to key a narrower rule off of. BASPM's
Flight-Director-specific growth variant is deliberately not adopted -
Launch Window has no "who is Flight Director for this mission" concept
yet, a separate open design question. Both mechanics run before
`releaseSurvivors`'s own retire-on-release check, so a crew member's
final season still counts for both. Two new event records,
`MissionReliabilityChanged`/`MissionSkillGrowthApplied`. **Wired into the
bot harness the same day** - no new bot decision was needed, since the
harness already calls `MissionProgress.advanceSeason`; added five new
`PersonnelPlayoutReport` fields (`missionReliabilityChangeCount`/
`averageMissionReliabilityDelta`, `missionSkillGrowthCount`/
`averageMissionSkillGrowth`, `missionTriggeredSetReleaseCount` - the
last kept separate from `ResearchProgress`'s own R&D-pass release
count), a non-negative-gain invariant for `MissionSkillGrowthApplied`,
and two new harness tests proving both mechanics - and specifically the
mission-triggered SET-release path - are actually reachable. **Numbers
(same 200-game/80-season/seed-42 run as the mission-sim pass above):
1,053 resigned/1,037 retired/5,344 still employed/84,592 training
completions/4,516 hires/598 programs opened/36,131 R&D seasons; 2,103
missions launched, mission steps 1,725 successful/403 minor-glitch-
solved/422 minor glitch/604 failure/41 catastrophic, 82 lost in
accidents, 24 retired on mission release - personnel/hiring/program
totals within a few percent of the prior pass, consistent with the
random stream shifting (extra post-season draws every mission season).
**The step mix also moved toward success, in the direction the new
feedback loop predicts:** success share rose 50.4%→54.0%
(1,610/3,192→1,725/3,195) with every unsuccessful category falling,
despite a flat total step count - consistent with reliability gain
raising base probability and skill growth raising the crew/saving-throw
inputs, both now feeding back into later rolls, which the harness had
no mechanism to do before this slice. **Single seed, not checked
against run-to-run variance** - steps aren't independent draws within a
game, so this is a plausible, not confirmed, causal read. R&D seasons
fell slightly too (36,313→36,131, ~0.5% - noise-level, not attributed
to anything here).** The two new mechanics themselves: 3,903
reliability changes (avg. delta +1.24, gains outweighing losses, as
expected since successful/glitch-solved steps far outnumber failures),
5,567 skill-growth events (avg. total gain 5.29), and 152 SET
specialists released via the new mission-triggered R&D-ceiling path
specifically (one event can release up to four at once, one component's
full slot count) - confirms that path is reachable at this scale. Full
writeup in `design.md` §2.5(g).

**Mission cost at launch done (2026-10-02):** **user-confirmed from
in-game play** (`baspm.md` §9.5's own added note) - a mission's cost is
deducted in full the instant the player decides to launch, before the
next turn evaluates, and is never refunded on a later scrub, so this is
no longer treated as an unconfirmed default. `MissionConfiguration`
gained a `cost` field, deducted up front by `MissionLaunch.launch` right
after the program-open check (same ordering `ProgramOpening.open`
already uses for its own funds check), rejecting with a new
`MissionLaunchOutcome.INSUFFICIENT_FUNDS` - mutating nothing - when
funds are below cost. A flat one-time cost, distinct from the recurring
per-season upkeep the still-unbuilt budget slice owns, and from
`Program.openingCost()`/`maintenanceCost()`, which price the program
itself rather than flying one of its missions. 45 existing
`MissionConfiguration` constructor call sites across 5 files needed the
new argument (mechanical, same kind of change as the R&D two-ceiling
upgrade earlier this project needed) - all defaulted to `0` except where
a test exercises cost specifically. 5 new tests: `MissionConfigurationTest`
(rejects negative, accepts zero) and `MissionLaunchTest` (deducts the
exact cost, rejects below-cost funds with nothing mutated, accepts an
exact-match boundary case). **Harness-wired the same day:** the three
fixture configurations got small launch costs (50/75/150, ascending
with crew/duration, well under the programs' own 500/600/1,200 opening
costs) - `maybeLaunchMission` already treated any non-`LAUNCHED`
outcome as an ordinary no-op, so no new harness code was needed, only a
Javadoc update. **The harness's full numbers run came back bit-for-bit
identical to the pre-cost run, and `missionsLaunchedCount` itself didn't
drop either, confirming `INSUFFICIENT_FUNDS` never actually fired in
this 200-game run** - this harness has no income mechanic at all, so
funds only ever fall, but the fixture's modest costs never drove any
game's funds low enough to matter within 80 seasons (same situation
`ProgramOpening`'s own `INSUFFICIENT_FUNDS` is in) - the dedicated
`MissionLaunchTest` cases are what actually prove it works.
Full writeup in `design.md` §2.5(h). **Still open for mission
simulation:** prestige/goals and the goal-dependency reliability
modifier (both blocked on the budget/prestige/goals slice, not started
yet).

**The Expert trait done (2026-10-02):** `baspm.md` §9.5 fully traced
Tiger Teams the same day the mission-cost note above was first written,
resolving the cross-reference that had kept this blocked - the user
then made every remaining design decision the same day, so this shipped
in full rather than waiting for a second wave (`design.md` §2.2(c)/§2.5(i)
have the complete writeup; this is the short version). New
`TrainingKind.EXPERT`: an Astronaut or Mission Control officer (never
SET) with a skill at or above **80** can start open-ended training
toward the Expert trait in it via a new validated action
(`TrainingProgress.startAstronautExpertTraining`/
`startMissionControlExpertTraining` - rejects below-threshold, already-
Expert, not-idle, or unknown ids); each season rolls a flat 25% success
chance plus up to another 10% scaled by how far above the threshold the
skill sits (interpolated linearly to 100%), with no skill growth either
way - success grants the trait, failure just continues into another
season. An employee can hold the trait in several skills over a career
(one per successful stint), and it's permanent once granted. A new
general **pull-out-of-training action** (`TrainingProgress.pullOutOfTraining`)
shipped alongside this - applies to any `TrainingKind`, not just
`EXPERT`, since nothing in the project had this before; credits
`SkillGrowth.partialCredit` for the three fixed-length kinds' current
round, nothing for `EXPERT`. **The mission-side rescue mechanic**
(`MissionProgress.applyExpertRescue`): when a step's first roll already
failed, any Expert whose skill matches that step's crew/controller
involvement can boost the saving-throw probability - re-deriving
`baspm.md` §9.5's own gap-closing formula
(`MissionStepResolution.applyExpertBoost`), generalized so crew-skill
Experts get a real mechanical role Tiger Teams itself never gave them
(user's own call, grounded in the real Ken Mattingly/Apollo 13 ground-
rescue precedent). **Eligibility is roster-wide, not mission-scoped** -
idle, in training, or assigned to *this* mission all count at full
strength; an Astronaut assigned to a *different* active mission is
excluded entirely (physically elsewhere), but a Mission Control officer
in the same situation scales to 75% instead of being excluded (still on
Earth). Multiple Experts stack sequentially, each closing a share of the
remaining gap left by the one before (50% on a given employee's first
use this turn, 40% second, 10% third, 0% beyond that - exhaustion is
per employee, not per step), which already guarantees some failure
chance always remains; an explicit 95% cap on top handles the case where
a big roster would otherwise make that chance negligible in practice,
without ever lowering an already-higher natural probability. Needed a
`MissionProgress.resolveStep` restructuring (drawing its own rolls
explicitly, since an Expert can only be "used" once a saving throw is
actually needed) - confirmed bit-for-bit identical harness numbers with
zero Experts before wiring the bot in, so this was a pure extension, not
a regression. New model fields: `Astronaut`/`MissionControlOfficer` each
gained an `expertSkills` set (a `LinkedHashSet`, the first `Set` type
`MessageRegistry` has ever needed to register over the wire). **Wired
into the bot harness the same day:** the bot starts Expert training
(20% chance, when eligible) and pulls people out of training (3% chance
per season) exclusively through the validated actions, never by setting
fields directly, so the harness's own threshold/pairing invariants are
a genuine cross-check, not the bot agreeing with itself; a third new
invariant independently re-derives each `ExpertRescueApplied` event's
expected boost percent from its own position in that season's event
list. **Numbers (same 200-game/80-season/seed-42 run): 1,092 resigned/
1,013 retired/5,344 still employed/71,178 training completions/4,515
hires/598 programs opened/37,426 R&D seasons; 2,164 missions launched,
mission steps 1,958 successful/793 minor-glitch-solved/285 minor
glitch/385 failure/33 catastrophic, 66 lost in accidents, 30 retired on
mission release; 3,518 Expert-training starts (3,108 completed), 3,663
pull-outs, 1,766 nonzero Expert rescues.** **The step-mix shift is the
rescue mechanic itself, not noise:** minor-glitch-solved rose 403→793,
failure fell 604→385, catastrophic fell 41→33, accidents fell 82→66,
and the average post-mission reliability delta rose 1.24→2.17 - the
direct, expected effect of 1,766 nonzero rescues pulling outcomes
toward success. Training completions fell 84,592→71,178, consistent
with Expert stints (which never emit `TrainingCompleted`, success or
fail) plus 3,663 pull-outs displacing rounds that would otherwise have
completed. Single seed, not checked against run-to-run variance, same
caveat as every other numbers pass this session.

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

- **`assets-raw/` is the user's own original art (user, 2026-10-04),** except the BASPM
  reference screenshots in `assets-raw/baspm/`. Converting anything else there into
  `assets/` needs no provenance check.

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
