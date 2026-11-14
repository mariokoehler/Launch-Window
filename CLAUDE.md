# CLAUDE.md — Launch Window project notes

Cross-session memory for this repo: how we work, where things live, and the pitfalls that
cost time before. Keep it short: it's loaded into every session. What the game *is* and why
belongs in `design.md`; screens in `ui-spec.md`; everything about the original game in
`baspm.md`. Update the right file in the same session a decision or finding is made. Don't log
features here: a new system gets one line under "What's built", its details go in the docs.

## What this project is

**Launch Window** — *"Stay one small step ahead."* — a non-commercial fan project reimagining
**Buzz Aldrin's Space Program Manager** (BASPM, Slitherine/Matrix Games, Unity) as an
**asynchronous multiplayer** space-agency game. Players log in once a day (or whenever), queue
actions for the current turn, and the server resolves every player's turn at a fixed
wall-clock time. A game runs for weeks to months.

**Docs:**
- `design.md`: concept, mechanics, architecture, open questions and the unconfirmed numbers (§4),
  the roadmap (§5) and its backlog.
- `ui-spec.md`: every screen and shared component, the server gaps (§6). Claude Design reads it
  from GitHub.
- `baspm.md`: an index into BASPM's decompiled source and data, with how its systems work.
- `assets/CREDITS.md`: every third-party asset. Credit each one there.
- `ideas/`: full plans for ideas kept for later, each linked from `design.md` §5, Backlog.
- **History:** the docs were pruned on 2026-10-06 and again on 2026-11-12. The longer versions are
  in git: `git show 5c95135b:CLAUDE.md` (a dated log of every feature as built, with its
  reset/rebuild notes), `git show 5c95135b:design.md`, `git show 5c95135b:ui-spec.md`; older still,
  `git show 6fc7af5:design.md` (every trace and decision as it happened) and
  `git show 6fc7af5:DECISIONS.md` (the user's filled-in decisions checklist, in their words).

**Sibling projects** (same author; mined for patterns, not code): `C:\Users\mario\StarWars`
(real-time shooter, KryoNet) and `C:\Users\mario\Robot-Rampage` (turn-based, simultaneous
programming, KryoNet TCP). Robot-Rampage resolves a turn live once everyone confirms; Launch
Window resolves on a daily schedule with nobody waiting online.

## Current status (2026-11-12)

**Feature complete; play testing.** Work now is fixes and small improvements from play tests. The
first live game starts in January 2027. Many numbers are still unconfirmed: the list is in
`design.md` §4.

**What's built** (details in the docs):
- Server, persistence, accounts, KryoNet protocol, scheduler, headless rules engine, bots
  (`design.md` §3, §5). Turn backups and `TurnRollback` (§3.7). Filler bots that hold an agency
  (`BotPlayers`, `bot.FillerBot`).
- Content packs: `standard` (BASPM's whole tree, the server's default,
  `GameMeta.DEFAULT_CONTENT_PACK`) and `early-era` (tests pin it). A guided progression opens
  `standard` in stages (`design.md` §2.3(d)); Roads to the Moon compares the landing roads (§2.3(e),
  `ui-spec.md` §4.21).
- The libGDX client: every hub building, the turn report, replay, Museum, Settings, game result.
- People: chemistry, traits, ambitions, loyalty, career records, poaching, specialists with
  skill ceilings, PR duty (`design.md` §2.2(c)(d)).
- Intelligence Office, foreign hardware, rivals' failed attempts (`design.md` §2.9, `ui-spec.md` §4.15).
- R&D decisions and the hands-on puzzle (`ui-spec.md` component 26), random events with stories
  (§2.7(d)), commercial contracts (§2.11), publishing findings (§2.13), the archive (§2.12).
- Front pages for Firsts, the world reacts, theories of the day, In memory (`ui-spec.md` §4.18).
- Public prestige and the standing charts (`design.md` §2.1(g), `ui-spec.md` component 30).
- The replay as a film with a console and sound; every craft flies as itself (`ui-spec.md`
  §4.7.3; the overhaul is confirmed, user, 2026-10-21).
- Explainers E-01 to E-31 and concept chips (`ui-spec.md` components 18 and 19, `design.md` §1.1),
  help and its public wiki (component 23), the advisor (component 24), looking ahead (component 25).
- Deployment: client zip and server image built by tag-triggered CI (`design.md` §5, M6).
- Server settings: the admin edits `game-meta.json` in the client behind an admin password the server prints at
  startup, and can start a new game from there (`ui-spec.md` §4.22, G21).
- Pay expectations: people expect raises (reviews, good work); being underpaid caps morale, and morale changes
  how well people work; raise one person or a whole department (`design.md` §2.2(c), `ui-spec.md` §4.23).
- Incident investigations: a failure's own investigation, led by an engineer, may win back part of the lost
  reliability; a lost crew's independent board reports on the paper (`design.md` §2.14, `ui-spec.md` §4.24).

## How we work

- **Screens:** Claude writes the spec in `ui-spec.md`, the user mocks it up in Claude Design,
  Claude builds it in libGDX. Small changes to screens and components that already exist go
  straight into libGDX, checked with `ScreenSnapshot` and recorded in `ui-spec.md`; mockups are
  for new screens, new components and bigger layout changes. The libGDX client is the
  reference for anything built.
- **Keep `Theme.java` in lockstep with the design system** whenever a mockup session adds or
  changes a component or token. The **UI Asset Sheet** (a private Claude Design canvas; the
  Artifact tool's list finds it) collects every icon and texture: add to it whenever a mockup brings a new
  one. Every canvas (`Launch Window — <screen>`) is found the same way.
- **Advisor before and after** any multi-file feature.
- **Unspecified sub-details:** propose a concrete default in the doc, flagged unconfirmed (and
  listed in `design.md` §4), rather than stalling. Ask only for foundational or hard-to-reverse choices.
- **Flag unprompted:** security concerns (briefly).
- **No new history notes** (user, 2026-11-12, `design.md` §1.1): the game has enough. Don't add,
  collect or propose them; fixing a wrong fact in an existing note is still welcome. Facts a new
  feature needs (an explainer's, a clip's) are still checked and recorded in §1.1.
- **Content values follow BASPM** (user decisions 2026-10-03, confirmed 2026-10-06): costs,
  reliability, prestige, goals, budget numbers and training lengths come from BASPM's data;
  ids, names and all text are our own. Write text from the facts, never from BASPM's
  sentences, and verify every figure in a history note (BASPM's facts contradict each other).
  Deviating from BASPM is fine when it makes the game better.
- **Help** (`assets/content/help.json`, `ui-spec.md` component 23): numbers in it are tokens filled from
  the rules (`HelpValues`); a new rule number goes there, never into the text, and a changed rule needs
  its topic's text changed in the same session.
- **The public wiki mirrors help** (github.com/mariokoehler/Launch-Window/wiki): a hand-written `Home.md`
  with the user's banner, and one page a topic plus `_Sidebar.md`, written by dev-tools' `HelpWiki`. After a
  change to `help.json` or a rule it quotes: clone `https://github.com/mariokoehler/Launch-Window.wiki.git`,
  `mvn -q install -pl core -am -DskipTests`, then
  `mvn -pl dev-tools compile exec:java -DmainClass=de.mkoehler.launchwindow.devtools.HelpWiki -Dexec.args=<clone>`,
  commit and push. A renamed topic title leaves its old page behind: delete it. Pages show screenshots
  (`HelpWiki.SCREENS` picks shots per topic): after a screen changes, shoot (below) and pass the folder too,
  `-Dexec.args="<clone> <shots>"` (Windows paths); it empties and remakes `images/help/`, so nothing else goes
  there (the banner is in `images/`). A missing shot or subheading fails it.
- **The README's screenshots** are `docs/screenshots/`, made from a shots folder by
  `docs/screenshots/make_screenshots.py <shots>` (its `PICKS`). The README says the game has no licence yet
  and takes no contributions (user).
- **Commits** are GPG-signed. If signing fails, start Kleopatra
  (`C:\Program Files (x86)\Gpg4win\bin\kleopatra.exe`) and retry; never `--no-gpg-sign`.
- **The GitHub repo** (`mariokoehler/Launch-Window`) is public since 2026-10-28 (the user, for the wiki):
  everything pushed is published, so the hard rule below matters doubly.
- **After a change, say what it needs:** client only; rebuild client and server together (a changed
  message, saved field or rule shared by both); or reset the dev data root (a pack change or a
  saved-format break). See "Build and run".

## Source material (read-only, never copied into the repo)

- **Game install:** `E:\Steam\steamapps\common\Buzz Aldrin's Space Program Manager`.
- **Analysis dump:** `E:\Temp\BASPM`: decompiled C# (`source/`), unpacked bundles
  (`extracted/`), MonoBehaviour data (`monobehaviours/`). Some data files only exist loose
  under the install's `SPM-Windows_Data\Scripts\Data Scripts\`. Map in `baspm.md`.
- **Laptop:** if `COMPUTERNAME` is `S0147359`, there's no `E:` drive. Ask before any task
  that needs it.
- **Hard rule (`design.md` §1.2):** no BASPM text, art, audio, video or code, verbatim or
  lightly paraphrased, goes into the repo. Nothing extracted ever does. Five icons once traced
  from BASPM's UI had to be removed for this rule, and dense traced path data is the tell.
  Everything in `assets-raw/` is the user's own art and needs no provenance check, except
  `assets-raw/blueprints/`: generated from BASPM's R&D renders, the one decided exception
  (`design.md` §1.2). Ask before using BASPM's art that way again.

## Tools outside the repo

- **Content pack converter** (main PC only, because it reads `E:` and holds BASPM's ids):
  `E:\Temp\BASPM\tools\baspm_pack.py`, with its goal-id map `goals.map.json` next to it. Run
  `python baspm_pack.py discover`, then `python baspm_pack.py write <repo>\assets\content\early-era`.
  `write <dir> full` writes BASPM's whole tree with the ids in `full_maps.py` (same folder) and
  fails on anything it would drop; the early scope must keep reproducing the committed pack.
  The standard pack's text is generated from scratch modules, so edit `strings.json` directly now.
  Mapping rules (renames, retyped steps, dropped configurations, gates) are in `design.md` §5, M3.
  `checklist.py` (same folder) regenerates `assets-raw/ART_CHECKLIST.md`, rocket programs included;
  update its file maps when art lands.
- **ComfyUI** (Qwen Image 2.1) runs locally on port 8188. ComfyUI keeps its own copies of
  everything generated in `E:\Comfy-Desktop\ComfyUI-Shared\output` (`lw_portrait_*`,
  `lw_patch_*`, `lw_blueprint_*`, `lw_frontpage_*`): never delete those.
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
    `"reference"` swaps BASPM's render for an image in `references/`), the pick per component
    in `picks.json` (only picked variants are kept and committed, with `git add -f`), then
    `make_blueprint_assets.py` for `assets/ui/blueprints/*.jpg`. A new component needs a
    description, a pick and a converter run.
  - **Front-page art:** `assets-raw/frontpage/make_frontpage_art.py` (the newsprint, the masthead
    with its name written by the model, a press photo per First, the orbit photo's 12 pilot
    variants; 3 seeds each, review sheets in `review/`, spelling and stray insignia checked by eye),
    the pick per job in `picks.json` (only picked variants are committed, with `git add -f`), then
    `make_frontpage_assets.py` for `assets/ui/frontpage/`.
  - **Radio call-outs:** `assets-raw/audio/chatter/make_callouts.py` (the user's VibeVoice workflow,
    the lines in `callouts.json`, 4 seeds each), then `check_callouts.py` (Whisper large-v3 via
    `faster-whisper`: a transcript against the line, plus levels; passing takes go to
    `output/lw_callout/review/` for the user's ear). Run the check only with ComfyUI idle: VibeVoice-Large
    and Whisper don't fit on the GPU together, and the clash hung ComfyUI once. Kept takes are renamed
    into `chatter/`, then `make_radio_sounds.py` and `assets-raw/replay/cues.py`.
  - **Clip effects:** the user's Pixabay finds in `assets-raw/audio/` go in through
    `assets-raw/replay/sounds/make_effect_sounds.py`; `assets-raw/replay/effects.py` cues them where their layers
    show in the built projects (main PC only; rerun it after publishing a clip whose timeline changed).
    It skips a clip whose project is newer than its render; a rebuild without a publish does that. To clear one, compare
    the render's frames with the project's (`render-frame`; a `"mirror"` step's render is the project flipped by ffmpeg),
    and touch the WebMs only if they match.
  - **Sound effects:** Stable Audio 3 was tried for the replay's sounds and almost nothing was usable:
    the user curates clips (Pixabay) instead. Every sound is credited; a new sound plays a synthesized
    `placeholder-*` take until it has its own (`assets-raw/replay/sounds/make_placeholder_sounds.py`).
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
  The replay clips are built by `assets-raw/replay/` (`build.py`: a step's `"skin"` draws `<prefix>-<part>`,
  an `"alias"` alone draws without a skin; `launch-storyboard/build.py <variant> <skin>`). Publish a batch with
  `assets-raw/replay/publish.py [-j 3] <clip> ... launch:<skin>`: three at a time (one render keeps about three
  cores busy, user), a log each, the sound cues applied once at the end. `only:failure` (or another variant)
  builds and publishes just that variant. A single `build.py ... publish` exits non-zero when cues name clips
  not yet published: that's the cue check, not the render.
- **VectorCraft** (an open-source Illustrator, `C:\Program Files\VectorCraft\vectorcraft-cli.exe`):
  `run --cmd ID --params JSON ... --export X.svg` works headless (selection is `select.all` /
  `select.set`); `commands` lists the 732 commands. Worth it for what hand-written SVG can't do:
  Pathfinder (`object.pathfinder.unite` turns overlapping circles into one smoke outline),
  `path.knife` (cut a drawing into debris that fits back together), `object.path.offsetPath`,
  `outlineStroke`, and `convert IN OUT --outline-text`. The logo's fonts are in its own Fonts folder;
  for another font, `text.findFontFiles` then `text.addFontFiles`, in one MCP session
  (`mcp --headless`): the add takes only files a finished search found, and `run` ends first.
- **Other converters** in `assets-raw/`: `make_hub_art.py`, `make_content_art.py`,
  `svg/ui/make_ui_icons.py` (needs `pip install resvg-py`), `explainers/make_explainer_art.py`
  (the explainers' drawings: PNG without text plus a labels file, `ui-spec.md` component 18),
  `archive/make_archive_assets.py` (the archive's pictures and sounds; sources per figure in
  `assets-raw/archive/CHECKS.md`, rights in `PAGES.md`), and `splash/make_splash.py` (the splash clip).

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
mvn -pl dev-tools compile exec:exec@handson         # any hands-on level's puzzle on its own (start_handson.cmd)
```

- **`lwjgl3`/`server`/`dev-tools` resolve `core` from `~/.m2`.** `mvn clean package` doesn't
  install it, so a stale install runs old code. A new git tag also shifts the jgitver version.
- **No `-am` on an exec step** (it walks the reactor into the parent pom). Pick a `dev-tools`
  main class with `-DmainClass=...`, not `-Dexec.mainClass`.
- **`start_client.cmd`/`start_server.cmd`/`start_replayer.cmd`/`start_handson.cmd`/`reset_dev_data.cmd` end in `pause`;** don't run them
  from an automated shell. The reset works from the Bash tool as
  `cmd //c ".\reset_dev_data.cmd /y < nul"` (`/y` skips the question, `< nul` the pause; the
  `.\` is needed because this sandbox sets `NoDefaultCurrentDirectoryInExePath`). The reset deletes
  `data/` except `game-meta.json` and `accounts.json` (`/all` deletes the accounts too) and
  clears the client's report read marks. Run it with the server stopped. With the server running, Server settings ›
  Admin › Start a new game does the same from the client (old game zipped to `data/old-games/`).
- **Reset the dev data root** whenever saved state stops loading: after a field change on a
  saved class (Jackson is strict on unknown fields) or a content-pack change (the pack hash
  changes). Adding a saved field alone needs no reset.
- **Undoing a bad turn** (`design.md` §3.7): with the server stopped,
  `mvn -pl server compile exec:java -DmainClass=de.mkoehler.launchwindow.server.schedule.TurnRollback -Dexec.args="data <turn> --redo --yes"`
  (without `--yes` it only says what it would do; on the NAS, run the class from the fixed image,
  as in `deploy/docker-compose.yml`). The server zips the data root into `data/backups/` before
  every turn.
- **Dev mode:** `"devMode": true` in the server's `game-meta.json` (restart the server, or set it in the client's
  Server settings, which applies at once; the admin password is in the server's log and the file) shows
  "Dev · Resolve turn now" on the hub and allows `DevClient`'s `funds` cheat. `"devArchiveOpen": true`
  beside it opens every archive page, saving nothing (for reviewing them). `"bots"` there adds filler bots.
- **Bot playouts:** `mvn -pl dev-tools compile exec:java -DmainClass=de.mkoehler.launchwindow.devtools.analysis.ContentPackPlayouts`
  (`-Dexec.args=standard` for the full pack, which plays the Moon race; add `ahead` for the bot that
  works ahead, `side` for the one that also flies side missions, or `smart` for the best player, whose parts switch with `-Dbot.*` properties (`Bot.SMART`'s Javadoc; `-Dbot.roads=<nasa>,<soviet>` plays each road to the Moon, `-Dbot.slowGames=true` compares the slowest games with the fastest), `-Dexec.args="standard side"`)
  (`-Dexec.args="standard side decisions"` plays with the R&D decisions and reports how often they come)
  (`RaceShock` plays NASA against the Soviets in one game, the answer to rivals' Firsts off and on, `-Drace.shares=0,0.5`, or publishing findings never and always, `-Drace.publish=never,always`; both it and `ContentPackPlayouts` take `-Drules.raiseShare=0` to turn off raises between reviews and `-Drules.findingBonus=0` to turn off the findings' bonus; `ContentPackPlayouts` takes `-Drules.investigations=off` and prints the investigations it led)
  (money: `-Dplayout.economy=true` keeps a per-season ledger, played past the race (`-Dplayout.seasons`, `-Dplayout.games`); `RaceShock` takes `-Drace.economy=true`; `EconomyCeiling` prices the most an agency can spend against each review, from the rules alone; all three take `-Drules.budget=<bottom>-<top>,...` for other review ranges and `-Drules.flatten=<knee>,<slope>` for another flattening, `1,1` being BASPM's straight line; `-Drules.pay=none` (or some of `market,performance,ceiling,upside`) turns pay expectations off, with a people ledger beside the money one, and `-Dbot.pay=false` plays a bot that never raises anyone)
  (also `PersonnelLifecyclePlayouts`, `CareerCurves`, `StateSizes` (it takes the pack too), `IntelligencePlayouts`, `RaceLulls` (the Moon race's lulls with R&D tuned; `-Dexec.args="standard ahead+contracts 1.0 0.0"` plays with contracts against three simulated rivals); `design.md` §3.10).
- **Releasing** (ported from Robot-Rampage; its and StarWars' CLAUDE.md hold the gotchas):
  - **Client zip, locally:** `mvn -pl lwjgl3 -am -Prelease-client -DskipTests verify` gives
    `lwjgl3/target/LaunchWindow-Client.zip` (jpackage app-image: `LaunchWindow.exe`, `app/`,
    `runtime/`, `update.cmd`). jpackage refuses an existing destination: delete
    `lwjgl3/target/jpackage` (or `clean`) before re-running. The runtime has no `java.exe`; to
    see startup errors run the jar in `app/` with the system JDK from outside `assets/`.
    The client jar carries the replay clips; the server jar only `assets/content/`.
  - **DPI:** the exe's manifest declares it DPI-unaware, so Windows bitmap-scales it
    (`lwjgl3/src/main/jpackage-resources/LaunchWindow.manifest`). A player can override it per
    exe (Properties > Compatibility > Change high DPI settings > Application).
  - **A release:** push a tag `vX.Y.Z`. `.github/workflows/release-client.yml` (Windows,
    publishes the zip as a GitHub Release) and `release-server.yml` (Linux, pushes
    `ghcr.io/mariokoehler/launchwindow-server` to GHCR) run on it, passing the version to
    jgitver explicitly. Never push a tag without the user's go-ahead.
  - **Server:** `server/Dockerfile` (no Docker on this PC, so CI is its only build),
    `deploy/docker-compose.yml` for the NAS (`nas5714.myqnapcloud.com:45825`, the client's default
    server), with the data root bind-mounted at `/share/Container/launchwindow/data`. `.dockerignore`
    keeps `data/` (password hashes) and `assets-raw/` out of the image.
  - **The GHCR image's visibility** is a separate package setting from the repo's: it was private
    (needing a token on the NAS) on 2026-10-29, and the user means it to be public. Check it before
    relying on it.
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
- **One group only:** `-Dshots=<group>` (before `-cp`) renders just that group, e.g. `connect`,
  `contracts`, `intel`, `replay`, `console`, `decisions`, `standing` (grep `ScreenSnapshot` for
  `getProperty("shots")` for the rest). Add a group the same way when iterating on one screen.
- **Text fit:** every run also writes `text-fit.tsv` beside the shots (`TextFit`: text overlapping
  text, leaving its box or the screen, cut short, or a scroll pane that now scrolls).
  Shots render at the default Large text size; `-Dui.textScale=1` renders at Standard, `-Dtextfit.marks=true` writes each
  problem shot outlined in red to `marked/`. Run at 1 and 1.12 and compare: at 1 only the scroll
  panes that scroll by design and a few deliberate overlays are listed. A new screen must pass at 1.12 (the default).
- **Regression check for shared UI changes:** shoot before and after into fresh folders, then
  diff every PNG with PIL's `ImageChops.difference(...).getbbox()`. A run killed part-way leaves
  missing or stale PNGs, which make a diff look clean.
- **A snapshot block that calls `game.setSession`** changes the session for every shot after
  it: add new blocks at the end.
- **Shots read content and label JSON from the classpath** (`lwjgl3/target/classes`): after
  changing anything under `assets/content/` or a drawing's labels, run `mvn -q -pl lwjgl3
  test-compile` again, or the shots show the old text while the PNGs are new.
- **Shots render with no time passing:** a tooltip needs `ScreenSnapshot.showTooltip`, and the
  waiting chip a `render(0.6f)` first. `ScreenSnapshot` runs silent.

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
  missing one only fails at runtime. gdx-video (the splash, the replay clips) plays WebM and AV1
  only on desktop.
- The KryoNet fork comes from JitPack. A plugin's top-level `<configuration>` applies to every
  goal invoked from the CLI. Pom XML comments can't contain `--`. Use
  `${project.basedir}/../assets` for the shared assets folder.

**Network and saved formats**
- **Saved formats are pinned by a fixture:** `SavedFormatTest` opens every game
  under `server/src/test/resources/saved-format/` the way the server does and resolves its next turn. Saved
  state is read strictly (never `ignoreUnknown`), so a saved field renamed, removed or retyped, or an enum
  constant renamed, fails there. Before the first live game, `dev` may be written again when a format
  changes (say so in the commit): `mvn -q install -pl core -am -DskipTests`, `mvn -q -pl server
  test-compile`, then `mvn -q -pl server exec:java -Dexec.classpathScope=test
  -DmainClass=de.mkoehler.launchwindow.server.SavedFormatFixture -Dexec.args=dev`. At the release that
  starts the first live game, write `v1` and never write it again: from then on a breaking change needs a
  step in `SnapshotMigrations` (the snapshot carries `formatVersion`). Adding a field needs nothing.
- **A changed message shape** (a field added to an existing message) needs client and server rebuilt
  together; the handshake's version check refuses the old side, so rebuild the client zip before a play test.
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
- A server handler's exception is logged and doesn't kill the server; a rules failure inside an action is
  logged and refused. Accounts are made only on purpose (`CreateAccountRequest`), hashed with PBKDF2.

**Content and rules**
- **Pack files** are the ones in `ContentPackLoader.FILES`: hashed and frozen per game, so any change,
  a text fix included, reaches new games only and changes the pack hash. Among them `decisions.json`,
  `pr-press.json`, `commercial-clients.json` (identical in both packs, `RulesContentTest`: edit standard's,
  copy it to early-era), `commercial.json` and `findings.json` (a pack without it doesn't load). The rules
  read them through `catalog.rules()`, never `Decisions.standard()` and friends, which are the client's
  copies (installed by `ClientSession.start`). The playouts play without the press stories and clients,
  and with the decisions only when asked (`ContentPackPlayouts`' constructor).
- **Text files outside the pack** (not hashed, not frozen; never add them to `ContentPackLoader.FILES`),
  each held by its test until every id has text: `strings.json` (`ContentStringsTest`) and, in
  `assets/content/`, `space-race-news.json` (`SpaceRaceNewsTest`), `event-stories.json` (`EventStoriesTest`,
  each event to its sized count), `agency-names.json` (`AgencyNamesTest`), `front-pages.json`
  (`FrontPagesTest`, every article to its columns), `front-page-reactions.json` (`FrontPageReactionsTest`),
  `theories.json` (`TheoriesTest`), `memorial-pages.json` (`MemorialPagesTest`), `foreign-hardware.json`
  (`ForeignHardwareTest`: every program and rocket an entry, a role up to 46 characters), `moon-roads.json`
  (`RoadsToMoonTest`), `archive.json` (`ArchiveTest`), `concepts.json` (`ConceptsTest`), `explainers.json`
  (`ExplainersTest`), `help.json` (`HelpTest`), `investigation-causes.json` (`InvestigationCausesTest`: every group and
  phase a failing step can land on has causes, none blames a crew), `replay-sounds.json` (`ReplaySoundsTest`: every take listed
  and credited; loops OGG or WAV, never MP3) and `replay-clips.json` (`ReplayClipsTest`: every rule to its
  three clips and sidecars).
- **Ids in saved state are never renamed or reused:** turn reports store story ids, printed pages their
  story, reaction, theory and memorial line ids, saved state decision, press-story and client ids, and
  investigation cause, factor and board-story ids.
- Help's `handsOn.*` tokens quote the first hands-on level in `decisions.json` (rounds, slots, the
  expert's cost): a later level with a different shape needs the help text reworded.
- A finding's kinds are step ids without numbering or a/b (`Findings.kind`); `FindingsTest` fails on a
  kind that matches no step of the standard pack.
- `cancelsGoalIds` means "also credits, with prestige" (`rules.GoalCascade`), not "makes
  unearnable".
- A seat titled "FLIGHT DIRECTOR" doesn't always read the average skill; go by the seat's skill
  (`baspm.md` §10.11).
- A flight's component reliability *is* stamped at launch (`InProgressMission.launchReliability`,
  `design.md` §2.5(d)); read it through `MissionProgress.flyingReliability`, never the owned value.
- A mission's prestige tier is read live, never stamped at launch (simultaneous copies of one
  configuration would all pay first-time).
- An R&D decision's "seasons of R&D" are fixed when it's offered (`PendingDecision.seasonGain`): staffing
  the component before deciding doesn't grow the effect.
- `FrontPage.firstPage()` means a First's page; never `!followUp()`.
- **Intelligence** is settled once a turn for every player (`IntelligenceBoard`, kept in
  `data/intelligence/turn-N.json`) and names rivals by `GameSeeds.rivalKey`, never the account
  id. Its random draws use their own salted seeds; keep them off the season's stream so playouts
  stay comparable. A new hub building needs its `Hub.Place` before its hotspot reaches `assets/`
  (`Place.valueOf` throws on an unknown name).
- Names (agency, director) are capped at 32 characters, as are logins.

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
- Everything clickable clicks through one capture listener per stage (`client.ui.ClickSound`), so a new
  widget needs nothing; a bare `new ClickListener()` counts as a click-swallower, not a click.
- libGDX loads a one-channel image as alpha: save greyscale art as RGB.
- An image with transparency needs colour bled into its fully transparent pixels, or mipmaps
  fringe its edge (`make_patch_assets.py`).
- Raw hotspot files (`assets-raw/main_menu`) are in the raw art's pixels; `make_hub_art.py` scales them to
  1920 × 1080. The client reads points with a top-left origin and flips y against 1080.
- The client calls `core`'s rules against its preview state and never ports them (`ui-spec.md`
  G7). Tooltip text lives in `client.view.Tips`: read numbers from the rules' constants, never
  hard-code them.
