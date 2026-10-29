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
- `ideas/`: full plans for ideas kept for later, each linked from `design.md` §5, Backlog.
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
  by topic over the current screen, filtered to the screen's topics. 33 topics (synced with the code 2026-10-26). Numbers in
  `help.json` are tokens filled from the rules (`HelpValues`); a new rule number goes there, never
  into the text, and a changed rule needs its topic's text changed in the same session.
  **The public wiki mirrors it** (2026-10-28, github.com/mariokoehler/Launch-Window/wiki): a hand-written
  `Home.md` with the user's banner, and one page a topic plus `_Sidebar.md`, written by dev-tools' `HelpWiki`
  with the numbers filled. After a change to `help.json` or a rule it quotes, clone
  `https://github.com/mariokoehler/Launch-Window.wiki.git`, run
  `mvn -pl dev-tools compile exec:java -DmainClass=de.mkoehler.launchwindow.devtools.HelpWiki -Dexec.args=<clone>`
  (after `mvn -q install -pl core -am -DskipTests`), commit and push. A renamed topic title leaves its old page behind: delete it.
  **Pages show the screens they talk about** (2026-10-29): `HelpWiki.SCREENS` picks `ScreenSnapshot` shots per topic,
  each after the first paragraph or under a named subheading. After a screen changes, shoot (below) and pass the
  folder as a second argument (`-Dexec.args="<clone> <shots>"`, Windows paths): it remakes `images/help/`, which it
  empties first, so nothing else goes in there (the banner is in `images/`). A missing shot or subheading fails it.
- **The README** (2026-10-29): its screenshots are `docs/screenshots/`, made from a shots folder by
  `docs/screenshots/make_screenshots.py <shots>` (its `PICKS`). It says the game has no licence yet and takes no contributions (user).
- **The advisor is built** (2026-10-08, `ui-spec.md` component 24): a hub card with a new agency's
  first steps, ticked off from the state, until all are done or the player hides it.
- **The full content pack is built** (2026-10-08, `design.md` §5, M3): `assets/content/standard`,
  BASPM's whole tree (55 programs, 189 configurations; 187 since the E-154 became the Soviet X-15, 186 without Apollo's Agena docking), with its text and art, beside `early-era`.
  **It is the server's default since 2026-10-09** (`GameMeta.DEFAULT_CONTENT_PACK`; tests still pin
  `early-era`). New engine rules came with it: a payload's own lift, air drops with no rocket,
  configurations gated on a goal. Its components have blueprint drawings in the turn report
  (2026-10-09), and every program, component and real flight has a history note (2026-10-09). All
  its art is in, the last rocket programs (Juno II, Atlas) on 2026-10-09.
- **R&D decisions are built** (2026-10-10, `ui-spec.md` component 26, `design.md` §5): choices about a
  component under R&D, offered mostly in lulls, decided at once (`DecideAction`, `rules.RdDecisions`), ten
  types in each pack's `decisions.json` (`assets/content/standard/`). Rebuild client and server together before the play test.
  The pools were grown on 2026-10-25 to 185 decisions, about 20 first steps a type (pairing 12, hands-on 7);
  `-Dshots=decisions` renders every banner. Only one option a decision may take an overseer (the banner lists
  the engineers, not the option), and no negative funds effect. Rebuild client and server together; no reset.
- **Random events tell stories** (2026-10-10, `design.md` §2.7(d)): 105 in `event-stories.json`, sized per
  event from the playouts, no repeats until an event's stories have all run.
- **Looking ahead is built** (the user's idea, 2026-10-10, `ui-spec.md` component 25): R&D milestones with
  the seasons to each (`ResearchPlanning.project`), mission readiness and earliest launch, program staffing,
  training and retirement dates, the funds runway (`BudgetProgress.runway`), where a locked goal is earned.
- **PR duty for astronauts is built** (`design.md` §2.2(c)), with 39 press stories
  (each pack's `pr-press.json`), no repeats, and effects beyond prestige. Next is the next
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
  each pack's `commercial-clients.json` (`CommercialClientsTest`). Every number is unconfirmed; the
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
  preparations, retrofire, retropack jettison or modules separation, reentry, descent to the sea or the
  ground; a crewed orbit borrows the satellite's `earth-orbit`, user), so Mercury's and Vostok's orbital
  flights play whole. By 2026-10-14 every step has a clip, many borrowed, and all 249 seasons
  play whole (Venera 7's carrier entry got its own, `bus-entry`, 2026-10-15: it burns up; the landers' entries
  `venus-entry` and `mars-entry`, no longer the Earth capsule's; their descents `venus-descent` and `mars-descent`, Viking's landing `mars-landing` and surface work `mars-surface`); a clip rule can name the step before (`landing` on the Moon, a runway or a planet) or, with `earlier`, any step before it in the season (a descent at Mars). Client only: no reset, no server
  rebuild.
- **The replay console** (the user's idea, 2026-10-13, `ui-spec.md` §4.7.3, from the canvas
  `Launch Window — Replay Console`): the monitor is a console with a mission clock, a status gauge and
  telemetry, whose motion (`client.view.ReplayConsole`) tells nothing before the reveal. The clock is a
  show clock until steps get mission times. `-Dshots=console` renders its states. Client only.
- **Sound for the replay** (the user's idea, 2026-10-13, `ui-spec.md` §4.7.3 "Sound"): a pool of named
  sounds in `assets/content/replay-sounds.json` (each one's description is the brief; no takes yet means
  silent), cues in the clips' sidecars (kept in `assets-raw/replay/cues.json`, written by its `cues.py`;
  the first, a rocket roar under the burns, 2026-10-14; radio calls on the early steps, the user's
  VibeVoice takes, one voice at a time, 2026-10-14; on most steps' glitches and failures, Claude's takes
  from the user's workflow, 2026-10-15; a call on every step and outcome from a pool of two lines or more, 2026-10-21; the user's effects, 2026-10-21, timed off the clips' built projects by
  `effects.py`), the console's own sounds, room tone, a fanfare at the season
  summary (success or failure; no background music, user), music and effects volumes in Settings. All
  English (user). Every sound is real (Pixabay, NASA, Stable Audio 3, credited); a new one plays a
  synthesized `placeholder-*` take until it has its own (`assets-raw/replay/sounds/make_placeholder_sounds.py`).
- **The game's first sound** (the user's idea, 2026-10-12, NASA recordings in `assets/audio/`, credited):
  Armstrong's "one small step" with the splash, Kennedy's "We choose to go to the Moon" when the Join
  screen opens. Settings' Sound On/Off now works, for the whole device (`LaunchWindowGame.soundOn`).
  `ScreenSnapshot` runs silent. Client only.
- **The archive** (the user's idea, 2026-10-15, `design.md` §2.12, `ui-spec.md` §4.19): pages about real
  missions (NASA's public-domain pictures, sound, our own text), unlocked for everyone in a game when a
  goal is first reached there, linked from the turn report, in a Museum tab. Mocked up and built the same
  day: 20 pages (`assets/content/archive.json`, sources per figure in `assets-raw/archive/CHECKS.md`), their
  pictures and sounds (`assets-raw/archive/make_archive_assets.py`; rights in `PAGES.md`), a new message pair
  (`ArchiveRequest`, derived from goal records, nothing saved). Once the race is decided, every page still
  closed opens for everyone (user, 2026-10-15). Rebuild client and server together; no reset.
- **The retrofire before the pack's jettison** (user, 2026-10-16, `design.md` §5, M3): 20 standard
  configurations had them the other way round, as BASPM does; the converter now swaps them. The pack hash
  changed: reset the dev data root. The way-home clips play mirrored (the orbit's way), with a turn before a
  lone burn and an insertion that coasts to the top first (`ui-spec.md` §4.7.3).
  Gemini EOR's way home is reordered too (equipment section first), and replay rules can name `programs`
  and a `later` step: rebuild the client (the replay's clip choice changed in `core`).
- **Vehicles as themselves in the replay** (user, 2026-10-17, `ui-spec.md` §4.7.3): Surveyor, Pioneer Venus
  Multiprobe (its own six clips), Viking's separation, PrOP-M on Mars 2's third season, the X-15's peak, fast run and
  descent (and a drop that clears the carrier), and a slower Mars orbit insertion (`loi.jsx`'s `SLOW`; every approach now
  arrives at the insertion's speed). Client only.
- **The OKB-MiG E-154 is the Soviet X-15** (user, 2026-10-17, `design.md` §1): the converter rebuilds it from the X-15
  (`baspm_pack.py`'s `e154_from_x15`), three configurations where it had five (187 in the pack), its own art carried
  over, its speed flight's patch made (2026-10-17). Its clips are the X-15's timelines with its own craft
  and a Soviet carrier (`e154.jsx`, `craft.jsx`). The pack hash changed: reset the dev data root, rebuild client and
  server together.
- **Spacecraft as themselves, from Mercury on** (user, 2026-10-17, `ui-spec.md` §4.7.3): the programs from Mercury
  to the Moon landings get their own spacecraft in the replay, a package a program, the rocket staying generic.
  Mercury's is done: a step's `"skin"` in `build.py` draws `mc-<part>` for `<part>` (`motion.jsx`'s `ALIAS`), so
  the generic timelines play unchanged (`launch-storyboard/build.py <variant> mercury` for the launch). Client only.
  Gemini's is done too (`gm-`, and the Agena's own launch, `ag-`, picked by a rule's `"vehicle"`): rebuild the client
  (`core`'s rule matching changed). Its spacewalks play in steps (user, 2026-10-18, `ui-spec.md` §4.7.3): setup,
  experiments and teardown; a stand-up, out on the tether, a photo and back in, docked to the Agena; on a rendezvous,
  beside the other Gemini, whose crewman takes the pictures. The Earth-orbit tests of the circumlunar, lunar-orbital
  and direct-ascent Gemini fly Gemini too, with their Transtage, propulsion module and direct-ascent craft (user,
  2026-10-18). Vostok (`vk-`, Korabl-Sputnik 2 too), Sputnik 1, Explorer 1 and Sputnik 2 fly as themselves (user,
  2026-10-18): the early-era pack has no generic craft left. Voskhod too, with its soft landing and Leonov's spacewalk,
  and Soyuz 7K-OK, its orbital module on the nose, and Apollo, its CSM and its lunar module (user, 2026-10-18). Circumlunar Gemini's Earth-orbit tests no
  longer call their Transtage burn a trans-lunar injection: the converter renames it `high-orbit-injection`, and
  the replay shows the long ellipse it puts the capsule on (user, 2026-10-18). The pack hash changed: reset the
  dev data root, rebuild client and server together. Rockets stay generic (user: the report doesn't
  say which flew). Client only.
  The Soviet lunar craft's Earth-orbit tests fly as themselves (user, 2026-10-19, `ui-spec.md` §4.7.3): the 7K-L1
  (`s1-`), the 7K-LOK on its Blok D (`lok-`) and the LK without legs (`lkl-`), on Soyuz's skin and the lander's scenes
  (`build.py`'s `variant_of`). Client only.
  Their Moon flights too (user, 2026-10-20): on their Blok D (BASPM's rocket), which the 7K-L1 drops after the burn for
  the Moon and the 7K-L3 keeps into lunar orbit; the LK lands on its legs, and its cosmonaut plants the flag. The crewed
  orbit flight's second lunar orbit is retyped its Blok D separation: the pack hash changed, reset the dev data root,
  rebuild client and server together.
  So do the LK-700's (`l7-`, user, 2026-10-19), with a way home of its own instead of Soyuz's orbital module, and
  Gemini EOR's (`ge-`, its Langley lander `ll-`, the S-IVB tug `sv-`; user, 2026-10-19).
  The LK-700's Moon flights too (user, 2026-10-20): a trans-lunar stage of its own burns for the Moon and lets go, the
  landing stage brakes and lands, the return stage lifts off with the capsule: every crewed Moon program now flies as itself. Client only.
  Apollo's Earth-orbit tests too (user, 2026-10-20, `ui-spec.md` §4.7.3): Apollo 5's LM alone, the suborbital arc, one scale
  for the transposition, the high-orbit burn, the CSM's own spacewalks, two CSMs' rendezvous, the docking flight's Agena. Client only.
  Soyuz 7K-OK's steps typed by BASPM's names (user, 2026-10-20, `design.md` §5, M3): insertions, way home, duration levels,
  docking, egress and ingress; its spacewalk and crew transfer in three clips each. The pack hash changed: reset the dev data
  root, rebuild client and server together.
  Circumlunar Gemini's Moon flights fly as themselves (user, 2026-10-20, `ui-spec.md` §4.7.3): the Transtage alone
  to the Moon, or Gemini docked into it for the burn (`tpi.jsx`'s `SEP`), dropping its adapter on the Earth approach
  (`earth-approach.jsx`'s `DROP`). Client only.
  Direct-ascent Gemini's too (user, 2026-10-20): the rocket's stage makes the trans-lunar injection and lets go, the
  landing module brakes and lands, the ascent module lifts off and fires home, and the flag goes up. Client only.
  Gemini EOR's too (user, 2026-10-20): the tug burns for the Moon and lets go, the Langley lander lands, plants the flag
  and climbs back to the stack, the Moon under the lunar-orbit close-ups (`d-moon`). Client only.
  Lunar-orbit Gemini's too (user, 2026-10-20): the propulsion module makes the trans-lunar injection, the insertion
  and the burn home. Its flybys pass the Moon in one step now, BASPM's insertion and orbit replaced in the converter
  (`design.md` §5, M3): the pack hash changed, reset the dev data root, rebuild client and server together.
  The converter types the Moon programs' steps by BASPM's own step names where its TypeIDs disagree (`RETYPED`:
  Gemini EOR's Moon flights now reach the Moon; Apollo's Earth-orbit flights too), and every flyby drops BASPM's slingshot step (Apollo's too)
  (user, 2026-10-20, `design.md` §5, M3). Every step with the crew in it now risks the crew, BASPM's patchy flag
  ignored (user, 2026-10-20), in both packs. The pack hashes changed: reset the dev data root, rebuild client and
  server together.
  Apollo's moonwalks show the flag, the rock box and the rover's drives (user, 2026-10-20, `ui-spec.md` §4.7.3), and the
  rover landing's drives and the 7K-L3 landing's touchdown, moonwalks and liftoff are typed by their names: the pack hash
  changed, reset the dev data root, rebuild client and server together.
  The later NASA satellites fly as themselves (user, 2026-10-21, `ui-spec.md` §4.7.3): the Orbiting Frog Otolith (`of-`, its booms
  swinging out), Biosatellite (`bs-`, its capsule coming home alone) and Pegasus (Apollo's launch clips, its wings unfolding
  on its stage, `pg-`). Client only.
  So do the robot probes (user, 2026-10-21, `ui-spec.md` §4.7.3): a drawing each (`parts.py`'s `PROBES`), their own copies
  of the generic clips that draw a craft (`build.py`'s `PROBE_CLIPS`), the pad's clips generic under the fairing; Zond 5 flies
  the 7K-L1's; the Veneras' and Mars 2's landers have their own release, entry, descent and landing too, and Venera 7's carrier its burn-up (a step's
  `"alias"` in `build.py` alone now draws, without a skin). Client only.
  **The overhaul pass is done and confirmed** (user, 2026-10-21): no step draws a generic craft, the rockets, the probes' pad
  clips and Mars 2's entry aside; every clip and the decisions behind it stand as built (`ui-spec.md` §4.7.3).

- **Publishing findings is built** (the user's idea, 2026-10-21, `design.md` §2.13, `ui-spec.md` §4.20 and G16,
  straight in libGDX): science goals in each pack's hashed `findings.json` add `Findings.BONUS_POINTS` to the
  steps they make safer (`rules.Findings`, in the season pass and `MissionAssessment` alike), published by
  `PublishFindingAction`, settled once a turn (`FindingPublications`, `publications/turn-N.json`), paying up
  to 33 % of the goal's prestige. The mapping awaits the user's review. Every finding has its archive page (33 pages in all).
  `-Dshots=findings`, `-Drules.findingBonus=0`. Reset the dev data root, rebuild client and server together.

- **The world reacts is built** (the user's idea, 2026-10-20, built 2026-10-22, `design.md` §5, `ui-spec.md`
  §4.18.1, component 28 and G17, from the canvas `Launch Window — The World Reacts`): rivals give one statement
  on a First in the turn after it (`SubmitStatementRequest`, kept in `reactions/turn-N.json` by rival key), and
  the next resolution prints a follow-up page (`rules.ReactionPrinter`), its words in
  `assets/content/front-page-reactions.json` (outside the pack, `FrontPageReactionsTest`). `FrontPage` gained
  fields (old files load). A new message pair: rebuild client and server together; no reset.

- **Theories of the day are built** (the user's idea, 2026-10-22, flavour only, `design.md` §5 and §1.1, `ui-spec.md`
  §4.18.2 and G18): eight questions in `assets/content/theories.json` (outside the pack, `TheoriesTest`), raised in
  the turn report at a goal, settled by the first flight in the game that reaches an answering goal
  (`rules.SciencePrinter`, a `SCIENCE` page in `frontpages.json`). `FrontPage.firstPage()` means a First's page;
  never `!followUp()`. Rebuild client and server together; no reset. The gaps closed the same day: a late statement
  is refused, not lost (`ReactionStore.seal`); `DevClient`'s `pages` and `react`; the theories in the Museum's
  Archive tab, with their pages and archive links; the science page's "THE QUESTION" box; longer articles.

- **In memory is built** (the user's idea, 2026-10-22, `ui-spec.md` §4.18.3, component 29 and G19, from the canvas
  `Launch Window — In Memory`): a crew lost in flight is recorded a turn early (`losses/turn-N.json`), its agency
  gives a statement in one of four tones and every other agency may send condolences (both `Stance`s on the loss's
  id), and the next resolution prints a memorial page (`rules.MemorialPrinter`, first of the turn's pages). It came
  with **a goal counts only if the crew comes home** (`design.md` §2.6, `GoalsWithheld`). Rebuild client and server
  together; no reset.

- **The code review's launch items** (2026-10-25, `review.md`, finished with six area reviews the same
  day): a rules failure in an action is logged and refused (3.1); the server jar carries only
  `assets/content/` (8.6 MB, was 1.6 GB; the client jar still carries the clips, 2.5); a handler's exception
  no longer kills the server (2.6); a rollback rewinds the bots' `bot.json` (D2); the decisions, press
  stories and clients are pack files (2.2); accounts are made only on purpose, hashed with PBKDF2 and
  written at once (2.3); saved snapshots carry a format version, and a fixture game pins every saved
  format (2.1). The pack hash changed and a message was added: reset the dev data root, rebuild client and
  server together. Next in `review.md` §9: the hidden seeds (2.7) and the client's stale dialog and
  per-game marks (2.8).
- **Filler bots are built** (the user's idea, 2026-10-25, `design.md` §5, Backlog): `"bots"` in `game-meta.json`
  (`BotPlayer`), agencies the server plays itself just before each turn resolves (`BotPlayers`, `bot.FillerBot`
  through the players' own validation and journal; `bot.json` keeps the last turn played). They can win, and the
  race, Intelligence Office and result mark them "(bot)" (`RivalSummary.bot`). The bots' plans moved to `core`
  (`bot.BotPlans`). Every number is unconfirmed. A changed message: rebuild client and server together; no reset.
- **A guided progression** (the user's decision, 2026-10-26, `design.md` §2.3(d)): the standard pack opens in stages, the
  first satellite alone, then the rest with a satellite in orbit, a person in space, and a probe at the Moon with a
  person in orbit; inside a Moon program a crewed flyby, then an orbit, then a landing (`baspm_pack.py`'s
  `progression_gates`, full scope only). The smart bot's landing moves from season 84.2 to 85.0 on average. **Rocket-plane
  wings** (`design.md` §2.2(c), `rules.RocketPlaneWings`): a rocket plane's crew who come home earn them, +4 on every
  capsule step they pilot (unconfirmed). The advisor walks the stages (`Advisor.ROAD_AHEAD`) and can no longer be hidden,
  only minimized (`ui-spec.md` component 24). `-Dshots=progression`. The pack hash changed and saved classes and events
  gained fields: reset the dev data root, rebuild client and server together.
- **The roads to the Moon, measured, and Nova** (2026-10-26, `design.md` §2.3(e)): `-Dbot.roads=<nasa>,<soviet>` plays
  each road (Apollo and the 7K-L3 land at a median of 81, Gemini EOR 74, the LK-700 70 to 73). Gemini direct ascent
  (71) now needs **Nova**, a new heavy rocket (`baspm_pack.py`'s `nova_rocket`; its craft weighs 63.5 t), and lands at
  78. Apollo's CSM and the 7K-L3's LOK inherit 35 % of the first capsule, as Gemini does (`capsule_heritage`): Apollo
  lands at 77, the 7K-L3 at 78. Nova's pictures are the user's (2026-10-28): the rocket program image, and a blueprint generated from the
  user's reference (`assets-raw/blueprints/references/nova.png`). The pack hash changed: reset the dev data root.
- **Roads to the Moon is built** (the user's idea, 2026-10-26, `ui-spec.md` §4.21, from the canvas
  `Launch Window — Roads to the Moon`): the landing roads side by side, never ranked, figures from the rules
  (`client.view.RoadsToMoon`), words in `assets/content/moon-roads.json` (outside the pack, `RoadsToMoonTest`); from
  Headquarters' Lunar expeditions, the road programs' pages and the advisor. `-Dshots=roads`. Gemini EOR's Moon flights
  are now free with its own tests, not Apollo's (`discount_fixes`, a BASPM data slip): the pack hash changed, reset the
  dev data root.

- **A text size in Settings** (the user's request, 2026-10-26, `ui-spec.md` §4.12): Standard or Large
  (112 %, the default since the user tried it), per device; drawings' labels and the newspaper keep their size. Every
  screen checked at Large with the new text-fit report (below). Client only: no reset, no server rebuild.
- **Program details rebuilt full width** (the user's request, 2026-10-27, `ui-spec.md` §4.6.4): a long description for
  every program and the components it grants (`longDescription` in `strings.json`, sources in
  `assets-raw/strings/LONG_DESCRIPTIONS.md`), a striped price table with an upkeep column, each component's blueprint.
  **No Agena for Apollo** (user, `design.md` §5, M3): the converter drops `apollo-docking` (`dropped_configurations`).
  The pack hash changed: reset the dev data root, rebuild client and server together.

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
- **The GitHub repo** (`mariokoehler/Launch-Window`) is public since 2026-10-28 (the user, for the wiki):
  everything pushed is published, so the hard rule below matters doubly.

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
  - **Radio call-outs:** `assets-raw/audio/chatter/make_callouts.py` (the user's VibeVoice workflow,
    the lines in `callouts.json`, 4 seeds each), then `check_callouts.py` (Whisper large-v3 via
    `faster-whisper`: a transcript against the line, plus levels; passing takes go to
    `output/lw_callout/review/` for the user's ear). Run the check only with ComfyUI idle: VibeVoice-Large
    and Whisper don't fit on the GPU together, and the clash hung ComfyUI once. Kept takes are renamed
    into `chatter/`, then `make_radio_sounds.py` and `assets-raw/replay/cues.py`.
  - **Clip effects** (2026-10-21): the user's Pixabay finds in `assets-raw/audio/` go in through
    `assets-raw/replay/sounds/make_effect_sounds.py`; `assets-raw/replay/effects.py` cues them where their layers
    show in the built projects (main PC only; rerun it after publishing a clip whose timeline changed).
    It skips a clip whose project is newer than its render; a rebuild without a publish does that. To clear one, compare
    the render's frames with the project's (`render-frame`; a `"mirror"` step's render is the project flipped by ffmpeg),
    and touch the WebMs only if they match (2026-10-28: all 17 skipped then did, and gained their missing cues).
  - **Sound effects:** Stable Audio 3 (the user's `Sound_Effects.json`) was tried for the replay's sounds on
    2026-10-15 and almost nothing was usable: the user curates clips (Pixabay) for a few moments instead.
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
  Publish a batch of replay clips with `assets-raw/replay/publish.py [-j 3] <clip> ... launch:<skin>`: three at a
  time (one render keeps about three cores busy, user), a log each, the sound cues applied once at the end. A single
  `build.py ... publish` exits non-zero when cues name clips not yet published: that's the cue check, not the render.
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
  "Dev · Resolve turn now" on the hub and allows `DevClient`'s `funds` cheat. `"devArchiveOpen": true`
  beside it opens every archive page, saving nothing (for reviewing them).
- **Bot playouts:** `mvn -pl dev-tools compile exec:java -DmainClass=de.mkoehler.launchwindow.devtools.analysis.ContentPackPlayouts`
  (`-Dexec.args=standard` for the full pack, which plays the Moon race; add `ahead` for the bot that
  works ahead, `side` for the one that also flies side missions, or `smart` for the best player, whose parts switch with `-Dbot.*` properties (`Bot.SMART`'s Javadoc), `-Dexec.args="standard side"`)
  (`-Dexec.args="standard side decisions"` plays with the R&D decisions and reports how often they come)
  (`RaceShock` plays NASA against the Soviets in one game, the answer to rivals' Firsts off and on, `-Drace.shares=0,0.5`, or publishing findings never and always, `-Drace.publish=never,always`; both it and `ContentPackPlayouts` take `-Drules.raiseShare=0` to turn off raises between reviews and `-Drules.findingBonus=0` to turn off the findings' bonus)
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
  - **The repo went public on 2026-10-28,** so release zips download anonymously; the GHCR image's
    visibility is a separate package setting (it was private, needing a token on the NAS; still private on 2026-10-29, the user means it to be public): check it before relying on that.
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
- **Text fit:** every run also writes `text-fit.tsv` beside the shots (`TextFit`: text overlapping
  text, leaving its box or the screen, cut short, or a scroll pane that now scrolls).
  Shots render at the default Large text size; `-Dui.textScale=1` renders at Standard, `-Dtextfit.marks=true` writes each
  problem shot outlined in red to `marked/`. Run at 1 and 1.12 and compare: at 1 only the scroll
  panes that scroll by design and a few deliberate overlays are listed. A new screen must pass at 1.12 (the default).
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
- **Saved formats are pinned by a fixture** (2026-10-25, review.md 2.1): `SavedFormatTest` opens every game
  under `server/src/test/resources/saved-format/` the way the server does and resolves its next turn. Saved
  state is read strictly (never `ignoreUnknown`), so a saved field renamed, removed or retyped, or an enum
  constant renamed, fails there. Before the first live game, `dev` may be written again when a format
  changes (say so in the commit): `mvn -q install -pl core -am -DskipTests`, `mvn -q -pl server
  test-compile`, then `mvn -q -pl server exec:java -Dexec.classpathScope=test
  -DmainClass=de.mkoehler.launchwindow.server.SavedFormatFixture -Dexec.args=dev`. At the release that
  starts the first live game, write `v1` and never write it again: from then on a breaking change needs a
  step in `SnapshotMigrations` (the snapshot carries `formatVersion`). Adding a field needs nothing.
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
  `assets/content/space-race-news.json` (`SpaceRaceNewsTest`),
  `event-stories.json` (`EventStoriesTest`, which holds each event to its sized count),
  `agency-names.json` (`AgencyNamesTest`),
  `front-page-reactions.json` (`FrontPageReactionsTest`, which also holds the fullest article to its columns; printed
  pages store its ids), `theories.json` (`TheoriesTest`; science pages store its ids), `memorial-pages.json` (`MemorialPagesTest`; memorial
  pages store its story and line ids), `replay-sounds.json`
  (`ReplaySoundsTest`: every take there, listed and credited; loops OGG or WAV, never MP3), `replay-clips.json`
  (`ReplayClipsTest`, which holds every rule to its three clips and sidecars), and `front-pages.json`
  (`FrontPagesTest`, which also holds every article to its columns; printed pages store story ids).
  Turn reports store their story ids, so never rename or reuse one.
- **`decisions.json`, `pr-press.json` and `commercial-clients.json` are pack files** (2026-10-25, review.md
  2.2): in `ContentPackLoader.FILES`, hashed and frozen, in both packs and identical (`RulesContentTest`:
  edit standard's, copy it to early-era). A text fix in them is a pack change: it reaches new games only, and
  changes the pack hash. The rules read them through `catalog.rules()`, never `Decisions.standard()` and
  friends, which are the client's copies (installed by `ClientSession.start`). The playouts play without the
  press stories and clients, and with the decisions only when asked (`ContentPackPlayouts`' constructor), as
  they always measured; ids are kept in saved state, never renamed or reused.
- Help's `handsOn.*` tokens quote the first hands-on level in `decisions.json` (rounds, slots, the
  expert's cost): a later level with a different shape needs the help text reworded.
- `findings.json` is a rules file in `ContentPackLoader.FILES` (hashed, frozen), unlike the text files above:
  a pack without one doesn't load. A finding's kinds are step ids without numbering or a/b
  (`Findings.kind`); `FindingsTest` fails on a kind that matches no step of the standard pack.
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
