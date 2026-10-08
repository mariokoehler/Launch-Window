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
- **Explainers** (`ui-spec.md` component 18, `design.md` §1.1): E-01 to E-19 are mocked up on
  the canvas `Launch Window — Explainers` (the Artifact tool's list finds it). All 19 are built, in the
  Museum's Encyclopedia tab, with every figure checked. Players will reach them through concept
  chips on the program, configuration and rocket screens (`ui-spec.md` component 19; the audit
  is in `design.md` §1.1). The chips are mocked up on the same canvas's bottom row and built in
  libGDX (2026-10-07); their look is confirmed.
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
  personnel file, a standing order per department and per person. Its numbers are unconfirmed.
  Reset the dev data root before the next play test.
- **Intelligence is built** (`design.md` §2.9, 2026-10-07): no sabotage, an eighth building
  (the Intelligence Office), a level dial with actions and passives, from the canvas
  `Launch Window — Intelligence Office` (`ui-spec.md` §4.15). Reset the dev data root before the
  next play test (new saved fields).
- **Help is built** (2026-10-08, `ui-spec.md` component 23): a help button and F1 open the rules
  by topic over the current screen, filtered to the screen's topics. Seven topics so far, to
  judge the format; the rest follow once it's confirmed. Numbers in `help.json` are tokens filled
  from the rules (`HelpValues`); a new rule number goes there, never into the text.
- **PR duty for astronauts is built** (`design.md` §2.2(c)), with 39 press stories
  (`assets/content/pr-press.json`), no repeats, and effects beyond prestige. Next is the next
  play test.

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
  lightly paraphrased, goes into the repo, with no exceptions. The UI reference screenshots that
  once sat in `assets-raw/baspm/ui/` were removed on 2026-10-08: our own design had moved past
  them. They're still in git history. Five icons once traced from BASPM's UI had to be removed for
  this rule, and dense traced path data is the tell. Everything in `assets-raw/` is the user's own
  art and needs no provenance check.

## Tools outside the repo

- **Content pack converter** (main PC only, because it reads `E:` and holds BASPM's ids):
  `E:\Temp\BASPM\tools\baspm_pack.py`, with its goal-id map `goals.map.json` next to it. Run
  `python baspm_pack.py discover`, then `python baspm_pack.py write <repo>\assets\content\early-era`.
  To extend the pack, add the scope and our own ids to its `PROGRAMS`/`COMPONENTS`/`CONFIG_NAMES`
  maps and to `goals.map.json`; mapping rules are in `design.md` §5, M3. `checklist.py` (same
  folder) regenerates `assets-raw/ART_CHECKLIST.md`; update its file maps when art lands.
- **ComfyUI** (Qwen Image 2.1) runs locally on port 8188. ComfyUI keeps its own copies of
  everything generated in `E:\Comfy-Desktop\ComfyUI-Shared\output` (`lw_portrait_*`,
  `lw_patch_*`): never delete those.
  - **Portraits:** `assets-raw/portraits/make_portraits.py` (fixed seeds, blocks of 50, also
    writes `core/.../personnel/portraits.json`), then `make_portrait_assets.py` for the 128 px
    JPEGs and the memorial photos. A new ethnicity needs a `names.json` entry and a key in
    `ETHNICITY_KEYS`, or `PersonnelNamesTest` fails.
  - **Goal patches:** `assets-raw/patches/make_patches.py` (3 variants per goal, edge check and
    reroll), spelling checked by eye in `review/`, the pick per goal in `picks.json`, then
    `make_patch_assets.py` for the 128 px RGBA PNGs (never JPEG, which drops the alpha). A new goal needs a `GOALS` entry, a pick and
    a converter run.
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
```

- **`lwjgl3`/`server`/`dev-tools` resolve `core` from `~/.m2`.** `mvn clean package` doesn't
  install it, so a stale install runs old code. A new git tag also shifts the jgitver version.
- **No `-am` on an exec step** (it walks the reactor into the parent pom). Pick a `dev-tools`
  main class with `-DmainClass=...`, not `-Dexec.mainClass`.
- **`start_client.cmd`/`start_server.cmd`/`reset_dev_data.cmd` end in `pause`;** don't run them
  from an automated shell. The reset works from the Bash tool as
  `cmd //c ".\reset_dev_data.cmd /y < nul"` (`/y` skips the question, `< nul` the pause; the
  `.\` is needed because this sandbox sets `NoDefaultCurrentDirectoryInExePath`). The reset deletes
  `data/` except `game-meta.json` and `accounts.json` (`/all` deletes the accounts too) and
  clears the client's report read marks. Run it with the server stopped.
- **Reset the dev data root** whenever saved state stops loading: after a field change on a
  saved class (Jackson is strict on unknown fields) or a content-pack change (the pack hash
  changes).
- **Dev mode:** `"devMode": true` in the server's `game-meta.json` (restart the server) shows
  "Dev · Resolve turn now" on the hub and allows `DevClient`'s `funds` cheat.
- **Bot playouts:** `mvn -pl dev-tools compile exec:java -DmainClass=de.mkoehler.launchwindow.devtools.analysis.ContentPackPlayouts`
  (also `PersonnelLifecyclePlayouts`, `CareerCurves`, `StateSizes`, `IntelligencePlayouts`; `design.md` §3.10).
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
- Many repo files are CRLF on disk: normalize before replacing multi-line blocks, and keep each
  file's line endings.
- Stopping a background client or server task can leave the forked `java.exe` running. Check
  with `tasklist` and kill with `taskkill /F /PID`.

**Maven and libGDX setup**
- Every runnable module needs its own `natives-desktop` dependency per native libGDX piece; a
  missing one only fails at runtime.
- The KryoNet fork comes from JitPack. A plugin's top-level `<configuration>` applies to every
  goal invoked from the CLI. Pom XML comments can't contain `--`. Use
  `${project.basedir}/../assets` for the shared assets folder.

**Network and saved formats**
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
  `assets/content/space-race-news.json` (`SpaceRaceNewsTest`) and `pr-press.json` (`PrDutyTest`);
  turn reports store their story ids, so never rename or reuse one.
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
- Hotspot files say `imageHeight` 1088; flip y against 1080, the cropped art.
- The client calls `core`'s rules against its preview state and never ports them (`ui-spec.md`
  G7). Tooltip text lives in `client.view.Tips`: read numbers from the rules' constants, never
  hard-code them.
