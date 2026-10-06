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
- **Next, waiting on Claude:** ideas for astronauts' PR duty (`design.md` §2.2), asked for by
  the user.

## How we work

- **Screens:** Claude writes the spec in `ui-spec.md`, the user mocks it up in Claude Design,
  Claude builds it in libGDX. Small changes to screens and components that already exist go
  straight into libGDX, checked with `ScreenSnapshot` and recorded in `ui-spec.md`; mockups are
  for new screens, new components and bigger layout changes. The libGDX client is the
  reference for anything built.
- **Keep `Theme.java` in lockstep with the design system** whenever a mockup session adds or
  changes a component or token. The **UI Asset Sheet** (a private Claude Design canvas; ask the
  user for the link) collects every icon and texture: add to it whenever a mockup brings a new
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
  lightly paraphrased, goes into the repo. The one exception is the reference screenshots in
  `assets-raw/baspm/ui/`. Five icons once traced from BASPM's UI had to be removed for this, and
  dense traced path data is the tell. Everything else in `assets-raw/` is the user's own art and
  needs no provenance check.

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
  `svg/ui/make_ui_icons.py` (needs `pip install resvg-py`).

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
  (also `PersonnelLifecyclePlayouts`, `CareerCurves`; `design.md` §3.10).
- **Release packaging** is deferred; copy the sibling projects' `release-client` profile,
  Dockerfile and GitHub Actions when it's needed.

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
- **Kryo's 64 KB object buffer:** the turn report goes over the wire gzipped, and the server
  refuses a report that would still be too big; an oversized message drops the connection.
- **KryoNet's `Client.stop()`** can block about 11 s on Windows. Never close a client on a
  thread a result waits on.
- `LaunchRequest` copies `rocketIds` with `List.copyOf`, which rejects nulls: send "" for a
  missing rocket.

**Content and rules**
- `strings.json` is deliberately outside `ContentPackLoader.FILES` (not hashed, not frozen).
  Never add it there. `ContentStringsTest` fails until every new id has text. The same goes for
  `assets/content/space-race-news.json` (`SpaceRaceNewsTest`); turn reports store its story ids,
  so never rename or reuse one.
- `cancelsGoalIds` means "also credits, with prestige" (`rules.GoalCascade`), not "makes
  unearnable".
- A seat titled "FLIGHT DIRECTOR" doesn't always read the average skill; go by the seat's skill
  (`baspm.md` §10.11).
- A mission's prestige tier is read live, never stamped at launch (simultaneous copies of one
  configuration would all pay first-time).

**libGDX client**
- Never create drawables in a rebuild loop: use `UiKit.rect`/`icon`/`cardAccent`, which cache.
- Never use linear filtering on a stretched few-pixel nine-patch: it turns a flat fill into a
  gradient (`Theme.Shapes.rect`).
- Fonts are generated at the monitor's scale and drawn scaled down, so `Theme.Fonts` turns off
  integer positions (they made baselines wobble). Mipmaps made text greyer; the filter stays
  linear.
- libGDX loads a one-channel image as alpha: save greyscale art as RGB.
- An image with transparency needs colour bled into its fully transparent pixels, or mipmaps
  fringe its edge (`make_patch_assets.py`).
- Hotspot files say `imageHeight` 1088; flip y against 1080, the cropped art.
- The client calls `core`'s rules against its preview state and never ports them (`ui-spec.md`
  G7). Tooltip text lives in `client.view.Tips`: read numbers from the rules' constants, never
  hard-code them.
