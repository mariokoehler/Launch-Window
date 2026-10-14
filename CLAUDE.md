# CLAUDE.md — Launch Window project notes

Cross-session memory for this repo: how we work, where things live, and the pitfalls that
cost time before. Keep it short: it's loaded into every session. What the game *is* and why
belongs in `design.md`; screens in `ui-spec.md`; everything about the original game in
`baspm.md`. Update the right file in the same session a decision or finding is made.

## What this project is

**Launch Window** — *"Stay one small step ahead."* — a non-commercial fan project reimagining
**Buzz Aldrin's Space Program Manager** (BASPM, Slitherine/Matrix Games, Unity) as an
**asynchronous multiplayer** space-agency game. Players log in once a day (or whenever), queue
actions for the current turn, and the server resolves every player's turn at a fixed
wall-clock time. A game runs for weeks to months.

**Docs:**
- `design.md`: concept, mechanics, architecture, the roadmap (§5) and its backlog. Its full
  decision history up to 2026-10-06 is in git (`git show 6fc7af5:design.md`).
- `ui-spec.md`: every screen and shared component, the server gaps (§6), and notes for mockup
  sessions. Claude Design reads it from GitHub.
- `baspm.md`: an index into BASPM's decompiled source and data, with how its systems work.
- `assets/CREDITS.md`: every third-party asset. Credit each one there.
- **History:** the docs were consolidated on 2026-10-06. The earlier, much longer versions are in
  git: `git show 6fc7af5:CLAUDE.md` (dated status narrative, harness numbers),
  `git show 6fc7af5:design.md` (every trace and decision as it happened) and
  `git show 6fc7af5:DECISIONS.md` (the user's filled-in decisions checklist, in their words).

**Sibling projects** (same author; mined for patterns, not code): `C:\Users\mario\StarWars`
(real-time shooter, KryoNet) and `C:\Users\mario\Robot-Rampage` (turn-based, simultaneous
programming, KryoNet TCP). Robot-Rampage is closer in shape, but it resolves a turn live once
everyone confirms; Launch Window resolves on a daily schedule with nobody waiting online.

**Precedent inside BASPM:** it shipped a 1v1 play-by-email mode (`PBEMManager.cs`,
`baspm.md` §5), which proves its simulation can be paused, serialized and resumed per turn.

## Current status (2026-10-06)

- **M0–M4 done** (`design.md` §5): persistence and journal, accounts, KryoNet protocol,
  scheduler, the headless rules engine, the content pack, and player actions with the real
  season pass.
- **M5, the libGDX client, is in play testing.** Every hub building has its screen, as do the
  turn report, replay, Museum, Settings and game result. The first play-test feedback is built.
- **The decisions checklist came back on 2026-10-06,** was merged into the docs, and its code
  work is built (`design.md` §5, Backlog). Reset the dev data root before the next play test: the
  pack hash changed.
- **Explainers** (`ui-spec.md` component 18, `design.md` §1.1): E-01 to E-30 are mocked up on
  the canvas `Launch Window — Explainers` (the Artifact tool's list finds it). All 30 are built, in the
  Museum's Encyclopedia tab, with every figure checked. Players will reach them through concept
  chips on the program, configuration and rocket screens (`ui-spec.md` component 19; the audit
  is in `design.md` §1.1). The chips are mocked up on the same canvas's bottom row and built in
  libGDX (2026-10-07); their look is confirmed. E-20 to E-30, for the full content pack, are
  built and linked too (2026-10-09).
- **People worth caring about** (`design.md` §2.2(d), the user's idea, 2026-10-07): team chemistry
  from pair rapport and hidden traits, ambitions, career records, later poaching. The rules for
  phases 1 to 3 and the personal traits are built (2026-10-07), and so are their screens, from
  the canvas `Launch Window — People` (`ui-spec.md` components 20 to 22, §4.14). **Phase 4 is
  built** (2026-10-07, while the user was away): ambitions, loyalty, career records and the
  memorial's person page, rules and screens. Its numbers were confirmed on 2026-10-08, and
  three changes were built the same day: doubled ambition windows, faster rapport drift, and a
  mission as two teams for chemistry, the crew with CAPCOM and the controllers (`design.md`
  §2.2(d)). **Poaching (phase 5) is built** (2026-10-08, from the canvas
  `Launch Window — Poaching`, `ui-spec.md` §4.16): engineers and controllers only, offers from a
  personnel file, a standing order per department and per person, one approach per agency per
  person and a ceiling on matched raises (2026-10-09). Its numbers are unconfirmed. Reset the dev
  data root before the next play test.
- **Intelligence is built** (`design.md` §2.9, 2026-10-07): no sabotage, an eighth building
  (the Intelligence Office), a level dial with actions and passives, from the canvas
  `Launch Window — Intelligence Office` (`ui-spec.md` §4.15). Reset the dev data root before the
  next play test (new saved fields).
- **Help is built** (2026-10-08, `ui-spec.md` component 23): a help button and F1 open the rules
  by topic over the current screen, filtered to the screen's topics. 28 topics. Numbers in
  `help.json` are tokens filled from the rules (`HelpValues`); a new rule number goes there, never
  into the text, and a changed rule needs its topic's text changed in the same session.
- **The advisor is built** (2026-10-08, `ui-spec.md` component 24): a hub card with a new agency's
  first steps, ticked off from the state, until all are done or the player hides it.
- **The full content pack is built** (2026-10-08, `design.md` §5, M3): `assets/content/standard`,
  BASPM's whole tree (55 programs, 189 configurations), with its text and art, beside `early-era`.
  **It is the server's default since 2026-10-09** (`GameMeta.DEFAULT_CONTENT_PACK`; tests still pin
  `early-era`). New engine rules came with it: a payload's own lift, air drops with no rocket,
  configurations gated on a goal. Its components have blueprint drawings in the turn report
  (2026-10-09), and every program, component and real flight has a history note (2026-10-09). All
  its art is in, the last rocket programs (Juno II, Atlas) on 2026-10-09.
- **R&D decisions are built** (2026-10-10, `ui-spec.md` component 26, `design.md` §5): choices about a
  component under R&D, offered mostly in lulls, decided at once (`DecideAction`, `rules.RdDecisions`), ten
  types in `assets/content/decisions.json`. Rebuild client and server together before the play test.
- **Random events tell stories** (2026-10-10, `design.md` §2.7(d)): 105 in `event-stories.json`, sized per
  event from the playouts, no repeats until an event's stories have all run.
- **Looking ahead is built** (the user's idea, 2026-10-10, `ui-spec.md` component 25): R&D milestones with
  the seasons to each (`ResearchPlanning.project`), mission readiness and earliest launch, program staffing,
  training and retirement dates, the funds runway (`BudgetProgress.runway`), where a locked goal is earned.
- **PR duty for astronauts is built** (`design.md` §2.2(c)), with 39 press stories
  (`assets/content/pr-press.json`), no repeats, and effects beyond prestige. Next is the next
  play test.
- **Undoing a bad turn is built** (the user's idea, 2026-10-11, `design.md` §3.7): a zip of the data
  root before every turn, and `TurnRollback` to undo turns and redo them with a fixed build.
- **The hands-on decision is built** (the user's idea, 2026-10-11, from their "Probe Command
  Minigame" spec doc; `design.md` §5, Backlog; `ui-spec.md` component 26, type 11, and G13; from the
  canvas `Launch Window — Hands-on`): an eleventh R&D decision type, a resource puzzle the player can
  play or leave to the team, played step by step against the server (`rules.HandsOn`,
  `HandsOnAction`). Every number is unconfirmed; the bot gets the bonus 92 % of the time. Saved
  classes gained fields and there's a new action: reset the dev data root, rebuild client and server
  together.
- **Commercial contracts are built** (the user's idea, 2026-10-11, `design.md` §2.11, `ui-spec.md`
  §4.17 and component 27, from the canvas `Launch Window — Contracts`): a shared marketplace settled
  once a turn (`rules.ContractMarket`, `market/turn-N.json`), tiers by track record, money instead of
  prestige. Unlock sets in each pack's own hashed `commercial.json`, clients in
  `assets/content/commercial-clients.json` (`CommercialClientsTest`). Every number is unconfirmed; the
  first measurement says they fill the lulls but don't speed the race. The pack hash changed and
  saved classes gained fields: reset the dev data root, and rebuild client and server together (a new
  action).
- **Front pages for the Firsts are built** (the user's idea, 2026-10-11, `ui-spec.md` §4.18 and G14):
  a fictional newspaper's page on login, before the turn report, printed by the server
  (`rules.FrontPagePrinter`, `frontpages.json`), the words in `assets/content/front-pages.json`
  (outside the pack, `FrontPagesTest`). Mocked up in Photocraft (`assets-raw/frontpage/mockup/`), not
  Claude Design. A new message pair and a new refusal reason: rebuild client and server together (no
  reset needed). Names are now capped at 32 characters.
- **The smart bot** (2026-10-12, `design.md` §2.9 "A smarter bot"): `ContentPackPlayouts standard smart`
  lands the Moon race at a median of 87 (NASA) and 96 (Soviet) seasons; `-Dbot.slowGames=true` compares
  the slowest games with the fastest (a budget snowball, unresolved: the user's call).
- **A lost crew grounds crewed flights for 4 turns** (the user's decision, 2026-10-12, `design.md`
  §2.5(j), `rules.CrewedGrounding`; 4 turns confirmed), and the advisor has a one-off idle-staff tip
  (`ui-spec.md` component 24). A saved field, a new event and a new refusal reason: reset the dev data
  root, rebuild client and server together.
- **The answer to a rival's First, the "Sputnik shock"** (the user's decision, 2026-10-12, `design.md`
  §2.1(e), `rules.RivalFirsts`; the share unconfirmed): half a missed First's prestige counts at the next
  review. `RaceShock` (dev-tools) measures it with two smart bots in one game. A saved field and a new
  event: reset the dev data root, rebuild client and server together.
- **Raises between reviews** (the user's decision, 2026-10-12, `design.md` §2.1(f), `BudgetProgress`; the
  share unconfirmed): funding follows prestige up, never down, after the first review. It shrinks the slow
  tail by about 40 seasons and shortens the median game by about 9 (to about 78). Two saved fields and a
  new event: reset the dev data root, rebuild client and server together.
- **A Randomize button for the agency name** (the user's idea, 2026-10-12, `ui-spec.md` §4.2): 20 names a
  style in `assets/content/agency-names.json`, dealt without repeats. Client only: no reset, no rebuild
  of the server.
- **An animated splash** (the user's idea, 2026-10-12, `ui-spec.md` §4.0): a borderless square window
  plays the logo assembling itself (`assets/ui/splash/splash.webm`, made in EffectCraft by
  `assets-raw/splash/make_splash.py`), then opens the game's window. gdx-video is a new client
  dependency (WebM and AV1 only on desktop; its FFmpeg natives add about 15 MB to the jar). It tests
  the user's idea of animated mission steps (`design.md` §5, Backlog). Client only.
- **The replay as a film** (the user's idea, 2026-10-12, `ui-spec.md` §4.7.3 and §4.4): each step plays
  a clip on a monitor (`assets/ui/replay/`, found through `assets/content/replay-clips.json`, the no-signal
  loop for a step without one), its result shown at the clip's `revealAt`, the steps playing on by
  themselves again (Play/Pause; the 2026-10-06 decision reversed). The step list is the plan, so its length
  spoils nothing. A new turn with missions flown asks first to watch them spoiler-free, then the front
  pages, then the report (Settings: ask, always, never). A first satellite's six steps have clips
  (2026-10-13, `assets-raw/replay/steps/`), and so does a capsule's way home (2026-10-14: reentry
  preparations, retrofire, retropack jettison, reentry, descent and splashdown; a crewed orbit borrows the
  satellite's `earth-orbit`, unconfirmed), so Mercury's orbital flight plays whole; the rest play no signal. Client only: no reset, no server
  rebuild.
- **The replay console** (the user's idea, 2026-10-13, `ui-spec.md` §4.7.3, from the canvas
  `Launch Window — Replay Console`): the monitor is a console with a mission clock, a status gauge and
  telemetry, whose motion (`client.view.ReplayConsole`) tells nothing before the reveal. The clock is a
  show clock until steps get mission times. `-Dshots=console` renders its states. Client only.
- **Sound for the replay** (the user's idea, 2026-10-13, `ui-spec.md` §4.7.3 "Sound"): a pool of named
  sounds in `assets/content/replay-sounds.json` (each one's description is the brief; no takes yet means
  silent), cues in the clips' sidecars, the console's own sounds, room tone, a fanfare at the season
  summary (success or failure; no background music, user), music and effects volumes in Settings. All
  English (user). Every sound is real (Pixabay, NASA, Stable Audio 3, credited); a new one plays a
  synthesized `placeholder-*` take until it has its own (`assets-raw/replay/sounds/make_placeholder_sounds.py`).
- **The game's first sound** (the user's idea, 2026-10-12, NASA recordings in `assets/audio/`, credited):
  Armstrong's "one small step" with the splash, Kennedy's "We choose to go to the Moon" when the Join
  screen opens. Settings' Sound On/Off now works, for the whole device (`LaunchWindowGame.soundOn`).
  `ScreenSnapshot` runs silent. Client only.

## How we work

- **Screens:** Claude writes the spec in `ui-spec.md`, the user mocks it up in Claude Design,
  Claude builds it in libGDX. Small changes to screens and components that already exist go
  straight into libGDX, checked with `ScreenSnapshot` and recorded in `ui-spec.md`; mockups are
  for new screens, new components and bigger layout changes. The libGDX client is the
  reference for anything built.
- **Keep `Theme.java` in lockstep with the design system** whenever a mockup session adds or
  changes a component or token. The **UI Asset Sheet** (a private Claude Design canvas; the
  Artifact tool's list finds it) collects every icon and texture: add to it whenever a mockup brings a new
  one.
- **Advisor before and after** any multi-file feature.
- **Unspecified sub-details:** propose a concrete default in the doc, flagged unconfirmed,
  rather than stalling. Ask only for foundational or hard-to-reverse choices.
- **Flag unprompted:** security concerns (briefly), and educational or history material. Any
  real mission, engineering problem or tidbit that turns up goes into `design.md` §1.1 the same
  session; history notes are shown in-game (`ui-spec.md` component 15).
- **Content values follow BASPM** (user decisions 2026-10-03, confirmed 2026-10-06): costs,
  reliability, prestige, goals, budget numbers and training lengths come from BASPM's data;
  ids, names and all text are our own. Write text from the facts, never from BASPM's
  sentences, and verify every figure in a history note (BASPM's facts contradict each other).
- **Commits** are GPG-signed. If signing fails, start Kleopatra
  (`C:\Program Files (x86)\Gpg4win\bin\kleopatra.exe`) and retry; never `--no-gpg-sign`.
- **The GitHub repo** (`mariokoehler/Launch-Window`) is private. Ask before making it public.

## Source material (read-only, never copied into the repo)

- **Game install:** `E:\Steam\steamapps\common\Buzz Aldrin's Space Program Manager`.
- **Analysis dump:** `E:\Temp\BASPM`: decompiled C# (`source/`), unpacked bundles
  (`extracted/`), MonoBehaviour data (`monobehaviours/`). Some data files only exist loose
  under the install's `SPM-Windows_Data\Scripts\Data Scripts\`. Map in `baspm.md`.
- **Laptop:** if `COMPUTERNAME` is `S0147359`, there's no `E:` drive. Ask before any task
  that needs it.
- **Hard rule (`design.md` §1.2):** no BASPM text, art, audio, video or code, verbatim or
  lightly paraphrased, goes into the repo. Nothing extracted ever does (the blueprints below are
  generated from renders, never copied). The UI reference screenshots that
  once sat in `assets-raw/baspm/ui/` were removed on 2026-10-08: our own design had moved past
  them. They're still in git history. Five icons once traced from BASPM's UI had to be removed for
  this rule, and dense traced path data is the tell. Everything in `assets-raw/` is the user's own
  art and needs no provenance check, except `assets-raw/blueprints/`: generated from BASPM's R&D
  renders, the one decided exception (`design.md` §1.2). Ask before using BASPM's art that way again.

## Tools outside the repo

- **Content pack converter** (main PC only, because it reads `E:` and holds BASPM's ids):
  `E:\Temp\BASPM\tools\baspm_pack.py`, with its goal-id map `goals.map.json` next to it. Run
  `python baspm_pack.py discover`, then `python baspm_pack.py write <repo>\assets\content\early-era`.
  `write <dir> full` writes BASPM's whole tree with the ids in `full_maps.py` (same folder) and
  fails on anything it would drop; the early scope must keep reproducing the committed pack.
  The standard pack's text is generated from scratch modules, so edit `strings.json` directly now.
  Mapping rules are in `design.md` §5, M3. `checklist.py` (same
  folder) regenerates `assets-raw/ART_CHECKLIST.md`, rocket programs included; update its file maps
  when art lands.
- **ComfyUI** (Qwen Image 2.1) runs locally on port 8188. ComfyUI keeps its own copies of
  everything generated in `E:\Comfy-Desktop\ComfyUI-Shared\output` (`lw_portrait_*`,
  `lw_patch_*`): never delete those.
  - **Portraits:** `assets-raw/portraits/make_portraits.py` (fixed seeds, blocks of 50, also
    writes `core/.../personnel/portraits.json`), then `make_portrait_assets.py` for the 128 px
    JPEGs and the memorial photos. A new ethnicity needs a `names.json` entry and a key in
    `ETHNICITY_KEYS`, or `PersonnelNamesTest` fails.
  - **Goal patches:** `assets-raw/patches/make_patches.py` (3 variants per goal, edge check and
    reroll), spelling checked by eye in `review/`, the pick per goal in `picks.json` (only picked
    variants are committed, with `git add -f`: `.gitignore` keeps the rest local), then
    `make_patch_assets.py` for the 128 px RGBA PNGs (never JPEG, which drops the alpha). A new goal needs a `GOALS` entry, a pick and
    a converter run.
  - **Component blueprints** (main PC only, it reads BASPM's renders from `E:`):
    `assets-raw/blueprints/make_blueprints.py` (the user's workflow `render_to_blueprint.json`,
    flattened to `blueprints_workflow_api.json`; one description per component in
    `descriptions.json`, 3 seeds each, full-size review sheets in `review/`; a description's
    `"reference"` swaps BASPM's render for an image in `references/`, as `mig-e-154-spaceplane`'s
    four views from the user's art), the pick per component
    in `picks.json` (only picked variants are kept and committed, with `git add -f`), then
    `make_blueprint_assets.py` for `assets/ui/blueprints/*.jpg`. ComfyUI's own copies are
    `lw_blueprint_*`: never delete those. A new component needs a description, a pick and a
    converter run.
  - **Front-page art:** `assets-raw/frontpage/make_frontpage_art.py` (the newsprint, the masthead
    with its name written by the model, a press photo per First; 3 seeds each, review sheets in
    `review/`, spelling and stray insignia checked by eye), the pick per job in `picks.json` (only
    picked variants are committed, with `git add -f`), then `make_frontpage_assets.py` for
    `assets/ui/frontpage/`. ComfyUI's own copies are `lw_frontpage_*`: never delete those.
- **Photocraft** (an open-source Photoshop, as the `photocraft` MCP server): reads and writes only
  under `E:\Temp\automation` (copy files there first; file placing is disabled, so open the image and
  copy its layer across with `layer.copyToDocument`). New layers land above the active one, so check
  the stack after adding a fill layer. Centered type with a horizontal scale comes out off-centre:
  read the bounds and move it. Tabs draw as boxes: use two aligned text layers instead.
- **EffectCraft** (an open-source After Effects, `C:\Program Files\EffectCraft\effectcraft-cli.exe`):
  `script X.jsx --empty --save-as X.ecproj` runs After Effects-style JSX headless, `render-frame`
  checks a frame, `render --format webm` makes a clip. No vector export, and an SVG imports flat:
  split it into layers first (`assets-raw/splash/split_logo.py`). A null used as a parent needs its
  anchor and position at the comp centre, or its children land offset.
- **VectorCraft** (an open-source Illustrator, `C:\Program Files\VectorCraft\vectorcraft-cli.exe`):
  `run --cmd ID --params JSON ... --export X.svg` works headless (selection is `select.all` /
  `select.set`); `commands` lists the 732 commands. Worth it for what hand-written SVG can't do:
  Pathfinder (`object.pathfinder.unite` turns overlapping circles into one smoke outline),
  `path.knife` (cut a drawing into debris that fits back together), `object.path.offsetPath`,
  `outlineStroke`, and `convert IN OUT --outline-text`. The logo's fonts are in its own Fonts folder
  (2026-10-12); for another font, `text.findFontFiles` then `text.addFontFiles`, in one MCP session
  (`mcp --headless`): the add takes only files a finished search found, and `run` ends first.
- **Other converters** in `assets-raw/`: `make_hub_art.py`, `make_content_art.py`,
  `svg/ui/make_ui_icons.py` (needs `pip install resvg-py`), and `explainers/make_explainer_art.py`
  (the explainers' drawings: PNG without text plus a labels file, `ui-spec.md` component 18).

## Build and run

Maven multi-module (`core`, `lwjgl3` client, `server`, `dev-tools`), Java 25. KryoNet TCP port
45825, no UDP.

```
mvn clean package                                   # everything, with tests
mvn install -pl core -am -DskipTests                # after every change to core, and every new tag
mvn -pl server compile exec:java
mvn -pl lwjgl3 compile exec:exec
mvn -pl dev-tools compile exec:java -Dexec.args="localhost 45825"
mvn -pl dev-tools compile exec:java -DmainClass=de.mkoehler.launchwindow.devtools.HotspotEditor
mvn -pl dev-tools compile exec:exec@replayer        # any mission's replay, any outcome (start_replayer.cmd [pack])
```

- **`lwjgl3`/`server`/`dev-tools` resolve `core` from `~/.m2`.** `mvn clean package` doesn't
  install it, so a stale install runs old code. A new git tag also shifts the jgitver version.
- **No `-am` on an exec step** (it walks the reactor into the parent pom). Pick a `dev-tools`
  main class with `-DmainClass=...`, not `-Dexec.mainClass`.
- **`start_client.cmd`/`start_server.cmd`/`start_replayer.cmd`/`reset_dev_data.cmd` end in `pause`;** don't run them
  from an automated shell. The reset works from the Bash tool as
  `cmd //c ".\reset_dev_data.cmd /y < nul"` (`/y` skips the question, `< nul` the pause; the
  `.\` is needed because this sandbox sets `NoDefaultCurrentDirectoryInExePath`). The reset deletes
  `data/` except `game-meta.json` and `accounts.json` (`/all` deletes the accounts too) and
  clears the client's report read marks. Run it with the server stopped.
- **Reset the dev data root** whenever saved state stops loading: after a field change on a
  saved class (Jackson is strict on unknown fields) or a content-pack change (the pack hash
  changes).
- **Undoing a bad turn** (`design.md` §3.7): with the server stopped,
  `mvn -pl server compile exec:java -DmainClass=de.mkoehler.launchwindow.server.schedule.TurnRollback -Dexec.args="data <turn> --redo --yes"`
  (without `--yes` it only says what it would do; on the NAS, run the class from the fixed image,
  as in `deploy/docker-compose.yml`). The server zips the data root into `data/backups/` before
  every turn.
- **Dev mode:** `"devMode": true` in the server's `game-meta.json` (restart the server) shows
  "Dev · Resolve turn now" on the hub and allows `DevClient`'s `funds` cheat.
- **Bot playouts:** `mvn -pl dev-tools compile exec:java -DmainClass=de.mkoehler.launchwindow.devtools.analysis.ContentPackPlayouts`
  (`-Dexec.args=standard` for the full pack, which plays the Moon race; add `ahead` for the bot that
  works ahead, `side` for the one that also flies side missions, or `smart` for the best player, whose parts switch with `-Dbot.*` properties (`Bot.SMART`'s Javadoc), `-Dexec.args="standard side"`)
  (`-Dexec.args="standard side decisions"` plays with the R&D decisions and reports how often they come)
  (`RaceShock` plays NASA against the Soviets in one game, the answer to rivals' Firsts off and on, `-Drace.shares=0,0.5`; both it and `ContentPackPlayouts` take `-Drules.raiseShare=0` to turn off raises between reviews)
  (also `PersonnelLifecyclePlayouts`, `CareerCurves`, `StateSizes` (it takes the pack too), `IntelligencePlayouts`, `RaceLulls` (the Moon race's lulls with R&D tuned; `-Dexec.args="standard ahead+contracts 1.0 0.0"` plays with contracts against three simulated rivals); `design.md` §3.10).
- **Releasing** (ported from Robot-Rampage 2026-10-06; its and StarWars' CLAUDE.md hold the
  gotchas):
  - **Client zip, locally:** `mvn -pl lwjgl3 -am -Prelease-client -DskipTests verify` gives
    `lwjgl3/target/LaunchWindow-Client.zip` (jpackage app-image: `LaunchWindow.exe`, `app/`,
    `runtime/`, `update.cmd`). jpackage refuses an existing destination: delete
    `lwjgl3/target/jpackage` (or `clean`) before re-running. The runtime has no `java.exe`; to
    see startup errors run the jar in `app/` with the system JDK from outside `assets/`.
  - **DPI:** the exe's manifest declares it DPI-unaware, so Windows bitmap-scales it
    (`lwjgl3/src/main/jpackage-resources/LaunchWindow.manifest`). A player can override it per
    exe (Properties > Compatibility > Change high DPI settings > Application).
  - **A release:** push a tag `vX.Y.Z`. `.github/workflows/release-client.yml` (Windows,
    publishes the zip as a GitHub Release) and `release-server.yml` (Linux, pushes
    `ghcr.io/mariokoehler/launchwindow-server` to GHCR) run on it, passing the version to
    jgitver explicitly. Never push a tag without the user's go-ahead.
  - **Server:** `server/Dockerfile` (no Docker on this PC, so CI is its only build),
    `deploy/docker-compose.yml` for the NAS, with the data root bind-mounted at
    `/share/Container/launchwindow/data`. `.dockerignore` keeps `data/` (password hashes) and
    `assets-raw/` out of the image.
  - **While the repo is private,** the release zip and the image need a GitHub login:
    `update.cmd` can't download anonymously and the NAS needs a token for GHCR.
  - **Icons:** `assets-raw/icon.ico` (exe) and `lwjgl3/src/main/resources/launchwindow-icon-*.png`
    (window), both from `assets/branding/logo.png`: placeholders until an icon-only logo exists.

**Checking a screen: `ScreenSnapshot`** renders every screen state on a hidden window, one PNG
each; Read the PNGs afterwards.
```
mvn -q install -pl core -am -DskipTests
mvn -q -pl lwjgl3 test-compile
mvn -q -pl lwjgl3 dependency:build-classpath -Dmdep.outputFile=target/test-cp.txt -Dmdep.includeScope=test
cd assets && java --enable-native-access=ALL-UNNAMED -cp "../lwjgl3/target/classes;../lwjgl3/target/test-classes;$(cat ../lwjgl3/target/test-cp.txt)" de.mkoehler.launchwindow.lwjgl3.ScreenSnapshot <out-dir>
```
- **`-Dshots=contracts`** (before `-cp`) renders only the contract shots; add a group the same way
  when iterating on one screen.
- **Regression check for shared UI changes:** shoot before and after into fresh folders, then
  diff every PNG with PIL's `ImageChops.difference(...).getbbox()`. A run killed part-way leaves
  missing or stale PNGs, which make a diff look clean.
- **A snapshot block that calls `game.setSession`** changes the session for every shot after
  it: add new blocks at the end.
- **Shots read content and label JSON from the classpath** (`lwjgl3/target/classes`): after
  changing anything under `assets/content/` or a drawing's labels, run `mvn -q -pl lwjgl3
  test-compile` again, or the shots show the old text while the PNGs are new.
- **Shots render with no time passing:** a tooltip needs `ScreenSnapshot.showTooltip`, and the
  waiting chip a `render(0.6f)` first.

## Pitfalls

**Windows and PowerShell**
- PowerShell 5.1's `Set-Content -Encoding utf8` writes a BOM, which breaks javac. Write with
  `[IO.File]::WriteAllText(path, text, (New-Object Text.UTF8Encoding $false))`.
- PowerShell's pipe to a native process also prepends a BOM, which breaks `DevClient`'s login
  line: pipe its input from the Bash tool.
- Don't inline Python edit scripts in a Bash heredoc: the tool mangles `\n` and some apostrophes
  ("unexpected EOF"). Write the script to the scratchpad with the Write tool and run it.
- Many repo files are CRLF on disk: normalize before replacing multi-line blocks, and keep each
  file's line endings. Git normally stores LF, but a bulk-edited `ui-spec.md` once reached the repo
  with CRLF (9fb3091), and `git add --renormalize` didn't fix it. Check with
  `git cat-file -p :<file> | tr -cd '\r' | wc -c` before committing; staging an LF copy fixes it.
- Stopping a background client or server task can leave the forked `java.exe` running. Check
  with `tasklist` and kill with `taskkill /F /PID`.

**Maven and libGDX setup**
- Every runnable module needs its own `natives-desktop` dependency per native libGDX piece; a
  missing one only fails at runtime.
- The KryoNet fork comes from JitPack. A plugin's top-level `<configuration>` applies to every
  goal invoked from the CLI. Pom XML comments can't contain `--`. Use
  `${project.basedir}/../assets` for the shared assets folder.

**Network and saved formats**
- **A changed message shape** (a field added to an existing message, as `SubmitActionRequest.gameTurn`
  on 2026-10-09) needs client and server rebuilt together; the handshake's version check refuses
  the old side, so rebuild the client zip before a play test.
- **Append-only:** `MessageRegistry` registers new types last. `ResolutionEvent`'s
  `@JsonSubTypes` ids are never renamed or reused, and event and report fields are only added
  (`design.md` §3.7).
- **Kryo's 64 KB object buffer:** the turn report and the player's state go over the wire as
  gzipped JSON (`TurnReportResponse`, `FetchStateResponse`), and the server refuses a report that
  would still be too big; an oversized message drops the connection. A full agency's state is
  about 17 KB zipped (`StateSizeTest`; dev-tools `StateSizes` measures the bots' over 80 seasons).
- **KryoNet's `Client.stop()`** can block about 11 s on Windows. Never close a client on a
  thread a result waits on.
- `LaunchRequest` copies `rocketIds` with `List.copyOf`, which rejects nulls: send "" for a
  missing rocket.

**Content and rules**
- `strings.json` is deliberately outside `ContentPackLoader.FILES` (not hashed, not frozen).
  Never add it there. `ContentStringsTest` fails until every new id has text. The same goes for
  `assets/content/space-race-news.json` (`SpaceRaceNewsTest`), `pr-press.json` (`PrDutyTest`) and
  `event-stories.json` (`EventStoriesTest`, which holds each event to its sized count), and
  `decisions.json` (`DecisionsTest`; saved state keeps its ids too), `agency-names.json` (`AgencyNamesTest`), `replay-sounds.json`
  (`ReplaySoundsTest`: every take there, listed and credited; loops OGG or WAV, never MP3), `replay-clips.json`
  (`ReplayClipsTest`, which holds every rule to its three clips and sidecars), and `front-pages.json`
  (`FrontPagesTest`, which also holds every article to its columns; printed pages store story ids). Core's tests have no assets on the
  classpath, so `Decisions.standard()` is empty there: tests and tools set it with `Decisions.setStandard`
  (and put `EMPTY` back after);
  turn reports store their story ids, so never rename or reuse one.
- Help's `handsOn.*` tokens quote the first hands-on level in `decisions.json` (rounds, slots, the
  expert's cost): a later level with a different shape needs the help text reworded.
- `cancelsGoalIds` means "also credits, with prestige" (`rules.GoalCascade`), not "makes
  unearnable".
- A seat titled "FLIGHT DIRECTOR" doesn't always read the average skill; go by the seat's skill
  (`baspm.md` §10.11).
- A flight's component reliability *is* stamped at launch (`InProgressMission.launchReliability`,
  `design.md` §2.5(d)); read it through `MissionProgress.flyingReliability`, never the owned value.
- **Intelligence** is settled once a turn for every player (`IntelligenceBoard`, kept in
  `data/intelligence/turn-N.json`) and names rivals by `GameSeeds.rivalKey`, never the account
  id. Its random draws use their own salted seeds; keep them off the season's stream so playouts
  stay comparable. A new hub building needs its `Hub.Place` before its hotspot reaches `assets/`
  (`Place.valueOf` throws on an unknown name).
- A mission's prestige tier is read live, never stamped at launch (simultaneous copies of one
  configuration would all pay first-time).

**libGDX client**
- Never create drawables in a rebuild loop: use `UiKit.rect`/`icon`/`cardAccent`, which cache.
- Never use linear filtering on a stretched few-pixel nine-patch: it turns a flat fill into a
  gradient (`Theme.Shapes.rect`).
- Fonts are generated at the window's own scale (`Theme.Fonts.windowScale`), rebuilt in place
  250 ms after a resize, and snapped to the window's pixels (`Theme.PixelSnapCache`), so one
  glyph texel lands on one pixel. The snap also corrects for the batch's transform translation,
  since a `ScrollPane` always draws through one and its fractional offset cropped one side of
  every glyph's anti-aliasing. Screens re-lay out their text when `Fonts.generation()`
  changes; anything built off-stage must too (tooltips do it on show). Never turn libGDX's
  integer positions back on (they round to layout units, which are pixels only at 1x, and made
  baselines wobble), and never mipmap glyphs (greyer text).
- At 1x, glyph advances are whole pixels, so text runs a few percent wider than the design's
  fractional widths. Leave some slack in tight rows.
- libGDX loads a one-channel image as alpha: save greyscale art as RGB.
- An image with transparency needs colour bled into its fully transparent pixels, or mipmaps
  fringe its edge (`make_patch_assets.py`).
- Raw hotspot files (`assets-raw/main_menu`) are in the raw art's pixels; `make_hub_art.py` scales them to
  1920 × 1080. The client reads points with a top-left origin and flips y against 1080.
- The client calls `core`'s rules against its preview state and never ports them (`ui-spec.md`
  G7). Tooltip text lives in `client.view.Tips`: read numbers from the rules' constants, never
  hard-code them.
