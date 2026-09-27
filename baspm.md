# BASPM Reference Dictionary

A navigation index into **Buzz Aldrin's Space Program Manager** (BASPM) —
the original game Launch Window adapts — covering its decompiled C# source,
its data files, and its extracted art/audio assets. This is a **dictionary
for finding things fast**, not a mechanics writeup: formulas, exact values,
and "how system X actually works" belong in `design.md` §2 once we do the
deep dive on that system, cross-referenced back to the file paths recorded
here. Update this file the session a new area gets explored, so we never
re-discover the same directory structure twice.

BASPM is a Unity game (NGUI-based UI toolkit — `UILabel`/`UISprite`/`BMFont`/
`AnimationOrTween`/tweens throughout). It ships with **decompiled C# fully
recovered** (not obfuscated) and its actual gameplay data in **plain XML/CSV
files sitting right in the install directory** — no need to reverse-engineer
balance numbers from binary data at all; see §3.

## 1. Where things live

### 1.1 Live game install (read-only, don't edit)

`E:\Steam\steamapps\common\Buzz Aldrin's Space Program Manager\`

- `SPM-Windows.exe` — the game itself (Unity, Windows).
- `Manuals\SPM-Manual-[E-Book].pdf` / `[PrintFriendly].pdf.pdf` — the
  official manual. **Read this before/alongside the code** for any system —
  it explains intent in plain English where the code just implements it.
- `SPM-Windows_Data\Scripts\` — **the actual game data, in plain text.** See
  §3.1 for the full map. This is the single most valuable directory for
  formula/balance analysis.
- `SPM-Windows_Data\MissionAnimationsMixAndMatchScripts\` — XML describing
  how per-step mission animation frames are composited (see §3.1's
  `MacroDiagramPrefix`/deployment-clip fields in `MsnCnfg*.xml`).
- `SPM-Windows_Data\AssetBundles*` — the packed art/audio Unity asset
  bundles (see §4 for the already-unpacked copies instead).
- `SPM-Windows_Data\Managed\` — compiled .NET assemblies (source for the
  `E:\Temp\BASPM\source` decompile, don't need to touch this directly).
- `lua5.1.dll`/`lua51.dll` at the install root — **grepped the full
  `Assembly-CSharp`/`-firstpass` source for `lua` (case-insensitive): no
  genuine reference**, only a false-positive substring match inside
  `Evaluate` in `UITweener.cs`. So the managed game code doesn't call into
  Lua at all — these DLLs are most likely an unused dependency of a native
  plugin, or leftover Matrix/Slitherine launcher tooling. Not worth further
  time unless something else points back to it.

### 1.2 Extraction/analysis dump

`E:\Temp\BASPM\` — everything below was produced by `extract.py`/
`export_monobehaviours.py` (also in this folder) against the install above.

- `source\` — **decompiled C# source**, 1128 `.cs` files across two
  assemblies. See §2 for the map.
  - `source\Assembly-CSharp\` — the game itself (737 files at the top level
    + `AnimationOrTween`, `SPMGraph`, `Sha1`, `DamienG.Security.Cryptography`,
    `Properties` subfolders).
  - `source\Assembly-CSharp-firstpass\` — third-party/plugin code compiled
    separately: NGUI internals-adjacent helpers (`BlendMode`, `GlowEffect`,
    `ImageEffects`), **Steamworks** (Steam integration), **Vectrosity**
    (vector line-drawing plugin, probably the world-map orbit/trajectory
    lines), **SimpleJSON** (the JSON parser `PBEMManager` uses to talk to
    its HTTP backend, §5).
- `extracted\` — **unpacked asset bundles** (textures, audio, etc. pulled out
  of the `.unity3d` bundle files as plain `.png`/audio/etc.). See §4.
- `monobehaviours\` — **every MonoBehaviour instance in the two main Unity
  data files (`mainData` = the main scene, `resources.assets`,
  `sharedassets0.assets`), exported as one JSON file per instance**,
  organized `monobehaviours\<file>\<ClassName>\<GameObjectName>__<id>.json`.
  This is Unity *scene wiring* (which UI widget references which label,
  which prefab a screen uses) — useful for "what does screen X actually show
  and where does its data come from" questions, but it is **not** where the
  gameplay balance data lives (that's the plain XML in §1.1/§3.1 — confirmed
  by checking `ConstantsManager`'s own exported instance, which is empty;
  `ConstantsManager.cs` loads its real values from `Constants.xml` at
  runtime instead). `_index.json` at the top is a flat `id → {class,
  gameObject}` lookup table across all three files — grep this first when
  hunting for a specific screen's scene objects.
- `ilspy\` — the decompiler tool output/workspace itself, not analysis
  material.
- `run.log` — extraction run log, only useful if extraction needs re-running.

## 2. Source code map (`source\Assembly-CSharp\*.cs`)

737 files at the top level, organized by a fairly consistent naming
convention rather than folders. **UI-screen-prefix cheat sheet** — counts
below use `ls *.cs | grep -cE "^PREFIX[A-Z][a-z]"` (word-boundary-aware so
`WM` doesn't also match `WMG_*`, the unrelated graphing-plugin files,
§2.5), **but this still undercounts**: it misses a prefix's own
further-prefixed subgroups, e.g. `ETRDR*`/`ETRDResults*` (5, R&D-results
sub-screen) and `ETLaunchSchedMsns*` don't match a bare `^ET[A-Z][a-z]`
scan the same way `ETInterseason*` does, and `HQRDM*`(8)/`WMMPM*`(2) are
similarly nested sub-prefixes of `HQ`/`WM`. Treat these counts as "at least
this many", not exact. Meanings are cross-checked against the manual's own
screen names (`SPM-Manual-[E-Book].pdf`, TOC in §7) where possible, and
marked **(inferred)** where the manual doesn't use the same term:

| Prefix | Count | Meaning | Confidence |
|---|---|---|---|
| `HQ` | 100 | Headquarters building screens — open/manage rocket & payload programs, R&D, goals (manual p.39, "HEADQUARTERS") | manual-confirmed |
| `UI` | 77 | Generic NGUI framework helper classes (`UILabel`-adjacent widgets, tooltips, tweening) — not game-specific, skip unless chasing a rendering/widget bug | confirmed (framework code, not gameplay) |
| `MP` | 37 | Multiplayer (PBEM) lobby/challenge screens — see §5 | manual-confirmed (matches "MULTIPLAYER" p.42) |
| `VAB` | 20 | Vehicle Assembly Building — scheduled/in-progress missions, rocket/payload assembly (manual p.35-36) | manual-confirmed |
| `WM` | 16 | Space Complex screen (the main hub with the 7 building icons, day/night cycle) — **internal codename doesn't match the manual's own name for it** ("Space Complex Screen", p.34); `WorldMapBaseScreenGameLogic.cs`/`WMEmployeeCenterButtonER.cs` etc. are this screen's code | inferred (mapping to the manual's "Space Complex Screen" is a strong guess, not proven) |
| `REB` | 14 | `RandomAndDecisionEventBase` subclasses, one class per random/decision event (manual p.28, "RANDOM EVENTS") | confirmed (base class name + manual section both line up) |
| `Museum` | 14 | Historical stats / achieved-goals / memorial browser (manual p.40, "MUSEUM") | manual-confirmed |
| `ET` | 11+ (undercounted, see above) | **Not just the News Screen** — covers the *whole end-of-turn sequence* triggered by the Space Complex screen's "End season" button (manual p.35, item 7): `ETInterseason*` (the news screen, matching manual p.34 "NEWS SCREEN" — `ETInterseasonOverlay.cs`'s `NewsItemType` enum incl. `CampaignRTTM*OpposingFaction`), then `ETRDResults*`/`ETRDR*` (an R&D-results step, matching the `RDResults*` asset bundles, §4), then `ETLaunchSchedMsns*` (**a per-mission Launch/Scrub/Resume/AssignMC decision step** — `ETLaunchSchedMsnsOverlay.cs`, `ETLaunchSchedMsnScrubButtonER.cs` etc.). **This means BASPM's turn resolution is itself interactive, not a single non-interruptible batch step** — see §7.2, this is directly relevant to designing Launch Window's own daily resolver. | confirmed (content, not just naming) |
| `PAO` | 4 | Public Affairs Office — budget & prestige overview (manual p.40, "PUBLIC AFFAIRS OFFICE") | manual-confirmed |
| `DE` | 1 | Only one file, `DEAvoidPrestigeDropAtACost.cs` — a single scripted decision-event, not a real prefix family | n/a (one-off) |
| `*ER` suffix | 411 | "Event Receiver" — one class per clickable UI widget's click handler (`XButtonER.cs` → calls into the real manager). Almost entirely thin NGUI click-glue. Low value to read individually; grep the *manager* method it calls instead. | confirmed (pattern, not a screen) |
| `*Overlay` suffix | 91 | A modal/full-screen UI panel | confirmed (pattern, not a screen) |

**Screen navigation root:** `SceneOrchestrator.cs` (2518 lines) — a
push/pop scene-stack manager (`PushScene(sceneName, ...)`/`PopScene(...)`,
internal `SceneDefinition`/`Scene` classes). This is the actual root of the
menu tree; start here (not guesswork from file-name prefixes) when mapping
the full screen graph for real.

### 2.1 Core simulation / state (highest-value files for mechanics analysis)

- **`Player.cs` (4407 lines)** — the God-class holding one player's entire
  live state: funds, prestige, personnel rosters, opened programs, active
  missions, goals achieved, etc. Start here for "where does the player's
  state live" questions.
- **`GameWorld.cs` (1594 lines)** — the top-level save-game container: holds
  the `Player` and the season/turn counter, and references to every manager
  below. `GamePlayerScriptRef`/`GoalsManagerRef` etc. accessed from here.
  (Whether a PBEM game's `GameWorld` holds both sides' `Player`s locally, or
  each side only ever sees its own — not yet checked; the "each side plays
  its own copy of the sim in isolation" framing in §5 is inferred from
  `PBEMManager`'s upload/download-a-blob shape, not confirmed by reading
  `GameWorld.cs` itself yet.)
- **`CoreManager.cs` (67 lines)** — the tiny base class every `*Manager`
  extends (`Initialize`/`Reset`/`UpdateLanguageChange` contract). Read this
  first, it's short and explains the manager pattern used everywhere else.
- **`SPMRandom.cs` (76 lines)** — the game's own RNG wrapper, and the
  **dominant** RNG for gameplay rolls (used in 49 files). Check here first
  when chasing a "how are odds actually computed" question, then follow into
  `Mission.cs`/`MissionStep.cs`. **Exceptions found by grep, not yet
  explained:** `UnityEngine.Random` is still used directly in 9 files —
  most are clearly cosmetic (`AudioController.cs`, `AudioObject.cs`,
  `BackgroundSwitchManager.cs`, NGUI internals `NGUITools.cs`/`UIAtlas.cs`,
  the unrelated `WMG_*` graphing-plugin demo files, §2.5) but
  **`MissionComponentsManager.cs`** and **`WorldMapBaseScreenGameLogic.cs`**
  also call it — worth checking during the mission-sim deep dive whether
  either use is gameplay-affecting or just presentation (e.g. picking which
  random flavor sprite/sound to show). `GameWorld.cs` additionally has one
  `new System.Random(...)` call, also not yet traced.
- **`Mission.cs` (59 lines)** + **`MissionStep.cs` (388 lines)** +
  **`MissionStepOutcome.cs`** — an in-progress mission and one of its steps;
  these three are just data holders, though (each holds component/personnel
  "involvement" percentages, per the data shape in §3.1's `MsnSim*.xml` —
  `MsnCmpInvolved`/`FlightControllerInvolved` amounts). **The actual
  resolution formula lives in `MissionSimBaseScreen.cs` instead (the screen
  class, not a manager) — fully traced now, see §8.**
  `MissionStepOutcome.MissionStepOutcomeT` confirms the full outcome
  taxonomy per step: `NOT_PERFORMED`, `CATASTROPHIC`, `FAILURE`,
  `MINOR_GLITCH`, `MINOR_GLITCH_SOLVED`, `SUCCESSFUL` — i.e. there are two
  distinct severities of bad outcome (a survivable glitch vs. a full
  failure) plus a catastrophic/fatal tier, not just a binary success/fail.
- **Season/turn advance entry points** (confirmed by grep, not yet read in
  full): `GameWorld.AdvanceSeasonPostInterseason()` →
  `Player.AdvanceSeasonPostInterseasonScreen(gameWorld)`; separately
  `Player.AdvanceSeasonRightAfterEndSeason(gameWorld)` calls
  `Player.AdvanceSeasonInAllMissionsInProgressAndSetToNotExecuted()`. This
  is BASPM's equivalent of what Launch Window's daily server-side batch
  resolver needs to do — **the most directly relevant entry point in the
  whole codebase for designing our own turn-resolution job**, trace this
  fully before designing ours from scratch.
- **Save format — plain XML, not a binary format. Confirmed, real example
  now in hand (2026-09-27).** `Application.persistentDataPath` resolves to
  **`C:\Users\<user>\AppData\LocalLow\MatrixGames\BASPM\`** on this machine —
  the company/product folder names weren't guessable in advance (earlier
  drafts of this file searched for "Matrix Games"/"Slitherine"/"Buzz Aldrin"
  and found nothing; the real folder is `MatrixGames\BASPM`, no space, and
  found only once an actual save existed). Contains `SaveSlot<N>.xml`
  (`SaveSlot0.xml` after a fresh Sandbox/NASA game, saved immediately after
  creation) — presumably also `AutoSave-<timestamp>-<year>-<period>.xml`,
  `MultiplayerGame.xml` (the PBEM payload, §5), and `AllSaveStats-*.csv`
  once more play/autosave has happened. **The XML schema matches the C#
  property names almost 1:1** (`<SaveSlot SPMVersionNumber="1.7.1" ...
  GameMode="Sandbox" SpaceAgency="NASA" GameDate="1.1955" PrestigePoints="250"
  Funds="25000"><GameWorld ...><Player>...`), confirming this is a direct,
  readable serialization of the same `GameWorld`/`Player` object graph
  described in §2.1, not a separate save-specific format — a huge help for
  the mission-simulation deep dive, since we can now read concrete evolved
  state (rolled employee stats, post-mission reliability values, mission
  records) directly instead of only inferring from data-file ranges. More
  snapshots (after hiring, after a mission attempt, ideally one success and
  one failure) are coming — see `CLAUDE.md`/chat for the current state of
  that.
- **R&D employee assignment persists across seasons untouched — confirmed by
  a real save (2026-09-27), backfilled here after `design.md` §4 was found
  citing this finding under a section number (`§8.1`) that never actually
  existed in this file — fixing that dangling cross-reference by writing the
  finding down properly, since it was apparently never committed to
  `baspm.md` in the first place.** Despite its state name
  `ASSIGNED_TO_RD_ONE_TURN` (which reads like a one-turn, must-renew
  assignment), a save showed one employee staying assigned to the same
  mission component's R&D across four consecutive seasons with no
  re-declaration in between. So this is a standing order the player sets
  once and leaves alone, not something that has to be resubmitted every
  season — directly relevant to what a Launch Window turn's input needs to
  carry for "who's on R&D" (design.md §4 already draws this conclusion;
  `design.md`'s citation has been corrected to point here, §2.1, rather than
  the non-existent old §8.1).

### 2.2 Manager classes (one per major subsystem, all extend `CoreManager`)

| Manager | Lines | Likely scope |
|---|---|---|
| `ProgramsManager.cs` | 764 | The Sputnik→Apollo program/tech tree (`Programs.xml`) |
| `MissionsConfigurationsManager.cs` | 740 | Mission configuration variants per program (`MsnCnfg*.xml`) |
| `MissionComponentsManager.cs` | 631 | Rockets/capsules/components catalog + reliability |
| `PersonnelManager.cs` | 595 | Astronauts/flight controllers/SET hiring, skills, training |
| `MissionsSimulationsManager.cs` | 450 | Loads & drives one mission's live simulation (see §2.1) |
| `MissionAssignmentsManager.cs` | 449 | Assigning personnel to mission slots |
| `SpaceComplexDataManager.cs` | 424 | The player's space complex/facility layout |
| `GoalsManager.cs` | 392 | Prestige-granting goals (`Goals.xml`) |
| `CampaignManager.cs` | 113 | Single-player `CampaignGSA` scripted objectives (`GSACampaignObjectives.xml`) — see §5 for why this is relevant to multiplayer design |
| `ConstantsManager.cs` | 143 | Loads `Constants.xml`/`Constants-<Agency>.xml` — tuning knobs (initial funds, budget-review thresholds, Tiger Teams boost/cost, mission-sim "saving throw" multipliers) |
| `RandomEventsManager.cs` | 96 | Picks/fires `REB*` random events |
| `TasksManager.cs` | 60 | Small — player task list/reminders |
| `ScriptsManager.cs` (57) / `DataScriptsManager.cs` (7) / `TextScriptsManager` | — | The data-loading plumbing itself (§3) |
| `PBEMManager.cs` | 955 | The async multiplayer backend client — see §5, dedicated section, this is the most relevant file in the whole codebase for Launch Window |

### 2.3 Gameplay domain classes (grep-by-keyword results, for quick recall)

- **Personnel/Employee** (`Employee.cs`, `AstronautEmployee.cs`,
  `SETEmployee.cs`, `MissionControlEmployee.cs`, `EmployeeFactory.cs`,
  `DisgruntledEmployee.cs`/`ResigningEmployee.cs`/`RetiringEmployee.cs`/
  `DismissedEmployee.cs`/`FailedBasicTrainingEmployee.cs` — the employee
  lifecycle state machine) — 37 files total.
- **Missions** (`Mission*`, `Scheduled Mission*`, `LaunchInformation.cs`) —
  66 files.
- **Goals** (`Goal.cs`, `ConjunctiveGoal.cs`, `GoalDependencyOrBoost.cs`,
  `GoalAccomplished.cs`) — 28 files.
- **Campaign** (`CampaignManager.cs`, `CampaignObjective.cs` +
  `CampaignOF*.cs` "objective field" subclasses — one per objective type:
  `AchieveSetOfGoals`, `AgencyFunds`, `EmployeesSkillset`,
  `MsnCmpRel`/`MsnCmpRelSETSkill` (mission-component reliability),
  `PersonnelCount`, `SuccessfulUsesCount` — a small strategy-pattern
  objective system, worth reusing the *shape* of for Launch Window's own
  scoring/win conditions) — 16 files.
- **Records/save data** (`*Record.cs`, `SaveLoadGameSlot.cs`,
  `MuseumRecords.cs`) — the historical-log/save-game shape — 16 files.
- **Rockets/launches** (`LaunchInformation.cs`,
  `DefaultRocketsForMsnCnfg.cs`, `ScheduledMissionToLaunch.cs`) — 11 files
  (rockets themselves are `MissionComponent`s, not a separate hierarchy).

### 2.4 Top-level enums worth knowing by heart

```csharp
// GameModeT.cs
enum GameMode { CampaignGSA, CampaignRTTM, Sandbox, MultiplayerColdWar, MultiplayerFreeform }
// SpaceAgencyT.cs
enum SpaceAgencyT { GSA = 1, NASA = 2, Soviet = 3, Common = 4 }
// MPGameModeT.cs
enum MPGameModeT { ColdWar, Freeform }
// DifficultyLevelT.cs
enum DifficultyLevelT { Normal = 1, Hard = 2, BuzzHard = 3 }
```

- **`DifficultyLevelT`** has no Launch Window equivalent — `design.md` §2.10
  decided a single shared difficulty for all players, based on **Normal**
  only (`Value1` throughout the difficulty-keyed Data Scripts below); `Hard`/
  `BuzzHard`'s `Value2`/`Value3` numbers are out of scope.

- **`GSA` = "Global Space Agency"** (manual-confirmed, p.5: "a utopic
  international space agency that unites all the world's space programs").
  Not generic/tutorial content — it's a full third playable faction with its
  own scripted single-player campaign (`CampaignGSA`): a long-term goal
  (manned lunar landing by end of 1973) plus periodic short-term objectives
  from `GSACampaignObjectives.xml` (`CampaignManager.cs` only initializes
  these `if playerSpaceAgency == GSA`).
- **`CampaignRTTM`** — the manual never spells out the acronym or uses it as
  a name, but repeatedly calls this mode "the race to the Moon campaign(s)"
  (p.5-6, p.28, p.41), so "Race To The Moon" is a confident reading, not
  a bare guess. Single-player NASA-vs-Soviet with a simulated AI rival
  (`CampaignRTTMParticipant.cs`, `SpaceAgency.
  GetOpposingSpaceAgencyCampaignRTTM`). **This AI rival is the closest thing
  BASPM has to a second competing player outside of actual PBEM
  multiplayer** — its "opposing faction" news feed
  (`ETInterseasonOverlay.NewsItemType.CampaignRTTM*OpposingFaction`) and the
  Public Affairs Office's goal-vs-goal comparison (manual p.40: "compare
  your current progress against the other faction ... along with the date
  [goals] have been accomplished by each faction") are effectively a
  template for what Launch Window needs to show about a *real* opposing human
  player each turn.
- **`MultiplayerColdWar`/`MultiplayerFreeform`** = the two real PBEM modes,
  manual-confirmed (p.43, "MULTIPLAYER"/"CREATE GAME"): **Cold War is
  strictly NASA vs. Soviet**; **Free Form lets either side pick any of the
  three agencies in any combination** (the manual's own example: "play as
  the Soviets and race against the Soviets at the same time"). Both are
  **confirmed 1v1 only** — the manual describes exactly one host and one
  opponent per game (down to "you can even accept your own game to play in
  hotseat mode"), asynchronous via e-mail notification ("you'll get an
  e-mail notification that will inform you when to play your next move"),
  with no in-game player-to-player interaction described anywhere in the
  manual beyond that notification. See §5.

## 3. Data-driven architecture

Three separate, non-overlapping data layers. Knowing which one a given
number/string lives in saves a lot of searching.

### 3.1 Data Scripts (gameplay balance/content — XML + CSV)

`SPM-Windows_Data\Scripts\Data Scripts\` — loaded at runtime via
`DataScriptsManager`/`ScriptsManager.GetScript(container, fileName,
ScriptLocationType.ApplicationDataPathFolder)`. **This is where every
balance number and content definition actually lives** — `ConstantsManager`
et al. are thin XML readers, not the source of truth.

- **`General Container\`** (24 files) — one file per top-level content type:
  - `Constants.xml` + `Constants-{GSA,NASA,Soviet}.xml` — tuning knobs,
    common + per-agency overrides, all values keyed by `DifficultyLevelT`
    (`Value1`/`Value2`/`Value3` attributes = Normal/Hard/BuzzHard).
  - `Programs.xml` — the full program list (e.g. `<Program ID='00-00'
    PgmCtgID='00' ... OpeningCost='126' MaintenanceCost='13'
    SpaceAgency='Soviet'>` for Sputnik I), including `OpeningCostDeductions`
    (cost discounts for having already opened a prerequisite program) and
    `RequiredMsnComponents`.
  - `MsnCnfg<PgmCtgID>-<PgmID>.xml` (57 files) — mission configuration
    variants for each program: duration, prestige granted/deducted, goals
    achieved, launch info, `GoalBoost` (extra goal progress for
    over-delivering, e.g. launching more Sputniks).
  - `Goals.xml` — every prestige-granting goal, first/second/later-time
    prestige values, `GoalsCancelled` (a goal can obsolete an earlier one).
  - `MissionComponents.xml` — the rocket/capsule/component catalog.
  - `ProgramCategories.xml` — the grouping programs fall under (satellite,
    manned orbital, lunar, etc. — `PgmCtgID` from `Programs.xml`).
  - `AstronautCandidates-<Agency>.xml`, `MissionControlCandidates-<Agency>.xml`,
    `SETCandidates-<Agency>.xml` — hiring pools, per agency.
  - `ReliabilityTransfers-<Agency>.xml` — how reliability carries over
    between mission component versions/upgrades.
  - `GSACampaignObjectives.xml` — the scripted single-player campaign's
    objective definitions, read by `CampaignManager.cs`.
  - `Tasks.xml`, `PersonnelManagerConstants.xml`, `MsnCmpsManagerConstants.xml`,
    `SpaceComplexData.xml` — further per-system constants/content.
- **`Mission Simulations Container\Program Category <NN> Container\`**
  (13 subfolders, one per `PgmCtgID`, ~250 files total) — `MsnSim<PgmCtgID>-
  <PgmID>-<MsnCnfgID>-<Season>.xml`, the actual **step-by-step mission
  simulation script**: per-step `MsnCmpInvolved`/`FlightControllerInvolved`
  percentage weights and per-step prestige-on-success/deducted-on-failure —
  this plus `MissionStep.cs`/`SPMRandom.cs` is the complete input to the
  launch success/failure formula. **The single most important data
  directory for a "how does a launch actually resolve" deep dive.**
  `MissionComponent MinUpgrade/MaxUpgrade/MinDowngrade/MaxDowngrade`
  attributes here also drive post-mission reliability changes.
All **444 `.csv` files** under `Scripts\` live entirely under
`Text Scripts\*\CSV\` (confirmed: `find Scripts -name '*.csv' -exec dirname
{} \; | sort | uniq -c` — 62 files × 7 languages under `General Container`
+ 2 files × 5 languages under `UI Container` = 444 exactly). They are
**localization source, not gameplay data** — see §3.2, correcting an earlier
guess in this file that they might be authoring-side balance tables.

### 3.2 Text Scripts (mostly localization — one exception)

`SPM-Windows_Data\Scripts\Text Scripts\` — parallel structure to Data
Scripts, but per-language (`BP`/`DE`/`EN`/`EN_US`/`ES`/`FR`/`IT`/`RU`
subfolders under `General Container`/`UI Container`, `BP`/`DE`/`EN`/`EN_US`/
`ES`/`FR`/`RU` under `Mission Failures Container`) plus a `CSV` source
folder per container. Loaded via `TextScriptsManager`/
`UIComponentsTextScriptsManager`. Most of `General Container`/`UI Container`
is plain UI-copy/localization, not relevant to mechanics — skip unless doing
a UI-copy or localization pass — **except the `*HistoricFactsStrings<LANG>.xml`
files, which matter for a different reason (below).**

**`General Container`'s `*HistoricFactsStrings<LANG>.xml` files — BASPM's
existing real-history content, and the seed of Launch Window's planned
educational layer (`design.md` §1.1).** Three files:
`ProgramsHistoricFactsStringsEN.xml`, `MissionComponentsHistoricFactsStringsEN.xml`,
`MissionsConfigurationsHistoricFactsStringsEN.xml` (plus per-language
siblings), keyed by the same IDs as their Data Scripts counterpart (a
program's `<HistoricFacts><ID>00-00-1</ID>...` in `Programs.xml` §3.1 points
at `String ID="00-00-1"` here). **The content is genuinely well-written**
— e.g. the Sputnik entry explains the real internal Soviet politics behind
flying the simpler "PS-1" instead of the more ambitious "Object D." **But it
answers "what happened, and the politics/history behind it," not "why was
this actually hard, technically"** — there's nothing here explaining e.g.
why orbital rendezvous is counterintuitive or why restarting an engine in
zero-g is hard. That gap is exactly the opportunity `design.md` §1.1 wants
Launch Window to fill — real foundation to build on, not a blank slate, but a
real content gap too.

**`Mission Failures Container` is the one exception worth flagging, though
its actual *gameplay* effect is unconfirmed.** `MissionsSimulationsManager.
GetMissionFailuresForPgmCtgXmlDocument`/`GetMissionFailuresForBoostersXmlDocument`
load `MsnFailuresPgmCtg<NN>Strings<LANG>.xml` / `MsnFailuresBoostersStrings
<LANG>.xml` directly, confirming the mission-sim manager pulls from this
folder — but that only proves the strings get *loaded and selected*, not
that they feed back into the outcome roll itself (they may be pure
narrative text picked *after* an outcome is already decided elsewhere, with
`IsCrewedMissionOnly` just filtering which narrative variant can be shown).
Still worth recording because the IDs encode real structure regardless of
whether the text itself is causal: `<MissionComponentCategory-or-
"ALL_BOOSTERS">+<StepTypeID>+<MissionStepOutcomeT>+<index>`, e.g.
`ALL_BOOSTERS+LAUNCH_PREPARATIONS+MSN_STEP_OUTCOME_CATASTROPHIC+001`, with
an `IsCrewedMissionOnly` flag on crew-fatality-specific entries. This
confirms `MissionStep.TypeID` values
(`LAUNCH_PREPARATIONS`, `COUNTDOWN`, `LAUNCH`, `ASCENT`, ... — the step-type
vocabulary, not yet fully enumerated) and reinforces the outcome taxonomy
from `MissionStepOutcome.cs` (§2.1).

### 3.3 Unity-serialized scene data

`monobehaviours\` (§1.2) — scene wiring only, not balance data (confirmed:
see §1.2's `ConstantsManager` example). Useful for "what widgets exist on
screen X" questions when reading `*Overlay.cs` isn't enough on its own.

## 4. Asset map (`extracted\`)

`extracted\AssetBundles\<Category>\...` — each `.unity3d` bundle unpacked to
`<bundle-name>.unity3d\<UnityType>\<asset-name>.<ext>` (e.g.
`Portraits\GenericPhotos\ASTRONAUTS\ASTRONAUTS-F1001.unity3d\Texture2D\
ASTRONAUTS-F1001.png`).

| Category | Files | Content |
|---|---|---|
| `Portraits` | 255 | Personnel headshots (generic pools, per role) |
| `NewsItems` | 236 | Interseason news-screen illustrations (matches `ETInterseasonOverlay.NewsItemType` values, e.g. `CampaignRTTMGoalsAchievedOpposingFaction-NASA-01.png`) |
| `SndFXs` | 320 | Sound effects |
| `Music` | 43 | Music tracks |
| `Programs` | 108 | Program-related art |
| `VABLayouts` | 63 | Vehicle Assembly Building screen layouts |
| `MicroDiagramsBackgrounds` | 117 | Small diagram backgrounds (mission-config detail views, probably) |
| `MacroDiagramsBackgrounds` / `Celestial Bodies` / `CelestialBodies` | 32 each | World-map / orbital diagram art |
| `MissionControl` | 10 | Mission Control room art |
| `Videos` | 3 | FMV cutscenes |
| `Assignments`, `FillerPanels(NewsScreen)`, `Goals`, `IconsAndButtons`, `In-gameBasescreen(-16.9)`, `MainMenu(-16.9)`, `MissionComponents`, `MissionConfigurationDiagrams(1-3, -16.9)`, `MissionConfigurations`, `RDResults(1-4, -16.9)`, `RDResultsThumbnails`, `SpaceComplex-{GSA,NASA,Soviet}` | (bundle files, not yet unpacked to a folder / not individually counted) | Per-screen UI art, R&D results screens, space complex art per agency |

`extracted\AssetBundlesMixNMatch\` (2182 files, named like
`00-01-00Deployment_02bAlpha.unity3d`) — **per-mission-step animation
frames** for the mission-config diagram animations (matches
`MsnCnfg*.xml`'s `MacroDiagramPrefix` and
`MissionAnimationsMixAndMatchScripts\*.xml`, which describes how these
frames get composited into a playing animation per launch/deployment step).
Naming = `<PgmCtgID>-<PgmID>-<MsnCnfgID><StepTag>_<frame><variant>Alpha` —
not yet fully decoded, revisit if we ever want to reuse/reference the
animation sequencing itself rather than just final art.

## 5. PBEM multiplayer — the key prior-art system

**`PBEMManager.cs` (955 lines)** implements BASPM's actual shipped
asynchronous multiplayer: a play-by-email-style protocol over HTTP
(`UnityEngine.WWW` + `SimpleJSON`), talking to what was presumably a
Slitherine/Matrix web backend (server no longer relevant/reachable — this is
about the *protocol shape*, not the live service).

**Flow, reconstructed from method signatures** (not yet traced line-by-line):

1. `PerformSerialExchange` / `RegisterUser` / `PerformAuthentication` — the
   account/serial-key layer (Matrix Games account system; irrelevant to us
   beyond "auth happens before any match action").
2. `IssueChallenge(gameName, gameMode, challengeCreatorSpaceAgency,
   challengePassword, gameComments, ...)` — create an open (or
   password-protected private) challenge; `ListChallenges` / `CancelChallenge`
   / `CheckChallengePassword` manage the lobby side.
3. `AcceptChallenge(challengeID, mpGameMode, challengeCreatorSpaceAgency,
   playerSpaceAgency, playerName, previousMiscDataCRC32, gameWorld, ...)` —
   joining starts the actual game; note the **CRC32 of "misc data"** passed
   in, i.e. some client-computed integrity check against tampering/desync,
   backed by `DataScriptsCRC32Manager.cs`/`CLZF.cs` (a compression lib,
   probably for the uploaded save blob) and `Sha1`/`DamienG.Security.
   Cryptography` (hashing helpers).
4. **The actual turn exchange is upload/download of a full game-state blob**,
   not incremental moves: `DownloadGameInstance(instanceID, opponentName,
   ...)` fetches the opponent's turn; `UploadGameInstance*AndPopScene`
   (four variants — normal, `InProgress`, `Resigned`, `WinningGame`) pushes
   the local player's turn back up as a `MemoryStream` (a serialized
   `GameWorld`/`Player`, presumably the same format as a local save file —
   worth confirming against `SaveLoadGameSlot.cs`).
5. `ListGamesInProgress` returns per-game status: whose turn it is
   (`TurnSide`/`TurnPlayer`), `TurnNum`, `LastDownloadDate`/`LastUploadDate`
   (used for the "claim a stalled game" mechanic — the `NotifyResults`
   error-code table includes "You cannot claim a game if your opponent has
   moved recently" and "It is not your turn in this game").
6. A rich **HTTP error-code table** (`NotifyResults`, ~30+ codes) covers
   auth failures, challenge-limit throttling, IP lockout, and game-state
   conflicts — a decent checklist of edge cases any Launch Window async-turn
   API will also need to handle (rate limiting challenges/games-in-progress,
   preventing a stale/out-of-turn upload, resignation, claim-on-timeout).

**Correction to an earlier draft of this section: PBEM sides are *not* fully
isolated.** `GameWorld` exposes `OpposingPlayerPBEMLoginName`/
`OpposingPlayerPBEMSpaceAgency`/`GetAllGenericGoalsAchievedInfoByOpposingFaction()`,
consumed by `GoalsListComparisonOverlay.cs` to render a side-by-side "you vs.
your opponent" table of every generic goal and which of you got there first —
in **both** `MultiplayerColdWar`/`Freeform` (labeled with the real opponent's
PBEM login name) **and** single-player `CampaignRTTM` (labeled "CPU"), via
the identical UI code path (`PAOGoalsComparisonButtonER.cs` opens it from the
Public Affairs Office, matching the manual's own description, p.40). So some
opposing-player state (at minimum: which generic goals they've achieved, and
apparently when) **does** cross from one PBEM side to the other — almost
certainly folded into the uploaded/downloaded game blob alongside the
player's own state, though the exact mechanism isn't traced yet. This is a
genuine, numbers-backed precedent for Launch Window's §2.9 "passive
competition" brainstorm option (`design.md`) — not just the flavor-text
"opposing faction news feed" this section originally undersold it as.

**What's still true:** there is still no evidence anywhere in the manual or
the code scanned so far of any *interactive* cross-player mechanic (chat,
sabotage, trading, joint missions) — only this one-way visibility into the
opponent's generic-goal progress. That gap in BASPM is still real; it's the
comparison/visibility mechanic itself that turned out to have more precedent
than first thought, not a hidden interaction system.

**Not yet investigated:** what exactly gets serialized into the uploaded
blob (full save-game vs. a turn delta, and whether it's where the opposing
generic-goals data actually comes from), and whether `MiscData`/the CRC32
covers gameplay-relevant state or just anti-cheat metadata. `GameWorld.cs`
lines 922, 1081 and 1100 (`NextScreenToShowWhenLoadingGame` set to
`Interseason` around `endTurn`/multiplayer-file save calls) are a concrete
starting point — they suggest the PBEM handoff is captured right at End
Season and the receiving side resumes straight into the ET flow (§2.4).
Worth reading `SaveLoadGameSlot.cs` and `GameWorld.cs`'s (de)serialization
methods in full next.

## 6. Suggested order for the upcoming deep dive

Not a commitment, just a sensible dependency order given what's mapped above:

1. **The manual, in full** (§7) — plain-English intent for every system
   before reading a line of code for it. Cheap, and the code's naming
   (`REB`, `RTTM`, `MSN_STEP_OUTCOME_*`) only makes sense once you know the
   real-world concept it's modeling.
2. **Mission simulation** (§2.1, §3.1's `MsnSim*.xml`) — the actual
   launch-success formula. Everything else (personnel skill, component
   reliability, prestige) feeds into this, so understanding it first makes
   every other system's *purpose* clear. **Done — see §8.**
3. **Personnel** (`PersonnelManager.cs`, `Employee.cs` hierarchy,
   `AstronautCandidates-*.xml` etc.) — skills/training feed directly into
   #2's involvement percentages. Manual p.29-32 ("PERSONNEL GROUPS") already
   gives the plain-English skill/morale/training model — §7 below.
4. **Programs & mission configurations** (`ProgramsManager.cs`,
   `MissionsConfigurationsManager.cs`, `Programs.xml`/`MsnCnfg*.xml`) — the
   content tree players progress through.
5. **Budget & prestige** (`ConstantsManager.cs`'s `BudgetReviewConstants`,
   `PAOMainOverlay.cs`, `Goals.xml`) — the resource loop and scoring.
6. **Campaign objectives / goals / random events** (§2.3) — the
   meta-progression and variance layer on top of the above.
7. **PBEM** (§5) — read in full once #2-5 make its data model legible,
   specifically to answer "what would a daily-batch N-player version of this
   turn-exchange protocol look like".
8. **Mid-turn player decisions** (§7's "structural gap for Launch Window") —
   trace every point where BASPM stops and waits for a player click *within*
   a turn/mission (scrub a launch, resolve a random event choice, pull an
   employee out of training early) — these have no obvious equivalent in a
   daily-batch model and need a resolution policy decided in `design.md`
   before mission simulation (#2) can be finalized for our version.

## 7. The manual (`SPM-Manual-[E-Book].pdf`)

Extracted to text with `pdftotext -layout` (works fine despite the
`[brackets]` in the filename once given a Windows-style path — MSYS's
`pdftotext` couldn't open the `/e/...`-style path directly, use
`cygpath -w` first, or just pass the `E:\...` path as-is). **Read this before
the code for any system** — it explains intent in plain English where the
source just implements it, and it's what caught several wrong inferences in
earlier drafts of this file (§2's prefix table, §2.4's `GSA`/PBEM claims).

**Table of contents** (page numbers as printed in the PDF):

| Section | Page |
|---|---|
| Introduction | 5 |
| Designer's Notes | 10 |
| Manual Overview | 11 |
| **Quick Start** | 11 |
| **Core Elements** — Program Categories, Programs, Mission Configurations, Mission Components, Reliability Transfer, Goals, Prestige Points, Funds, Seasons, Random Events, Budget Review, Reliability and R&D, Personnel Groups | 22 |
| **Starting a Game** — Start Menu, Game Mode, Difficulty Levels, News Screen, Space Complex Screen, VAB, Personnel Buildings, Headquarters, Museum, Public Affairs Office, Construction Mode, Common Elements | 33 |
| **Multiplayer** — Create Game, Join Game, Games In Progress, Pending Games | 42 |
| Credits | 44 |
| Appendix A — Why the Soviet Union Lost the Race to the Moon | 47 |
| Appendix B — Historical Information From The Buzz-Opedia | 67 |
| Appendix C — An Interview with Buzz Aldrin | 80 |

### 7.1 Core Elements, condensed (manual p.22-32 — read the manual for the full text)

- **Program Category** → **Program** → **Mission Configuration** → **Mission
  Component(s)** is the containment hierarchy: a category groups similar
  programs (e.g. "Earth Orbiting Research Satellites"); a program is one or
  more payloads launchable on a rocket, split into *payload programs*
  ("programs") and *rocket programs*; a mission configuration is one way of
  using a program's payloads (e.g. an extended-duration variant); a mission
  needs one (occasionally two) rocket plus its payload components.
- **Mission components** belong to exactly one of five groups, each mapped
  to one SET skill (below): **Rockets** (unmanned-only, Light/Medium/Heavy),
  **Space probes**, **Human-rated rockets** (can also carry probes, a way to
  farm reliability), **Crewed spacecraft**, **EVA suits**.
- **Reliability Transfer**: opening a new program with components related to
  ones you already own gives their initial reliability a boost from your
  existing components' current reliability — a stated reason to sequence
  which programs you open, not just what you open.
- **Goals**: `Generic` (any agency can achieve them, the head-to-head
  comparison points, e.g. "Man in orbit") vs. `Non-generic` (agency-specific,
  e.g. "Soyuz orbital flight"). Goals can imply lower-tier goals
  automatically (Duration III ⇒ II ⇒ I). **Skipping a prerequisite goal
  imposes a reliability penalty** on the mission that skips it — a real
  risk/reward lever, not just a checklist.
- **Prestige → Funds**: prestige earned/lost from mission success/failure
  directly determines next budget review's funding.
- **Seasons**: **confirmed exactly** one season = one quarter of a year (4/
  year). **Budget review every 4 years** (16 seasons), determining funding
  for the next 4-year cycle based on prestige earned.
- **Fixed costs per season**: all personnel salaries (SET, Astronauts/
  Cosmonauts, Flight Controllers), building maintenance, active
  payload/rocket program upkeep.
- **Personnel** — three groups, each with per-employee salary, morale (rises
  when kept busy/training, falls and eventually causes resignation when
  idle), age (group-specific retirement age), and learning capacity (fixed
  per employee, governs how much a training session actually improves
  skills — "low skill + high learning capacity" can beat "adequate skill +
  low learning capacity" as a hire). Training can be a fixed duration or an
  open-ended "train to target skill level" mode; pulling an employee out
  early gives reduced results and doesn't refund the cost.
  - **SET** (Scientists/Engineers/Technicians): one skill per mission
    component group above (5 skills) — R&D reliability gains scale with the
    assignee's skill in that component's group.
  - **Flight Controllers**: 5 skills (**Propulsion**, **Trajectory & GNC**,
    **Spacecraft systems**, **Crew And Payloads**, **Mission Operations**)
    plus a derived 6th, **Flight Director** (average of the other five) —
    every Mission Control room needs at least one Flight Director (some also
    an Assistant Flight Director/"AFD"). When a mission step's outcome goes
    wrong, the controllers assigned to *that step* plus the Flight
    Director(s) are what determine whether it gets caught/fixed.
  - **Astronauts/Cosmonauts**: 5 skills (**Leadership**, **Piloting**,
    **EVA**, **Science**, **Fitness**) and a **mandatory rest season** after
    any mission (no assignments, no training) — a real scheduling
    constraint for crew-heavy launch cadences.

### 7.2 A structural gap for Launch Window: mid-turn/mid-mission player decisions

BASPM is not purely "declare a turn's actions, then watch it resolve" — its
own end-of-turn sequence (§2.4's `ET` prefix: Interseason news → R&D results
→ `ETLaunchSchedMsnsOverlay.cs`) is itself a multi-screen interactive flow,
and several further manual-confirmed and code-confirmed points stop
**within** a turn or a mission-in-progress for a player click:
- **The `ETLaunchSchedMsnsOverlay.cs` step itself** — walks the player
  through scheduled missions one at a time with Launch/Scrub/Resume/
  AssignMC actions (`ETLaunchSchedMsn{Launch,Scrub,ResumeMission,AssignMC}
  ButtonER.cs`) as part of *advancing the season*, not as a free-time action
  — i.e. what BASPM calls "ending a season" already requires per-mission
  decisions mid-flow, matching manual p.36 "SCHEDULED MISSIONS"/`Mission.
  OnCancellation`'s `PrestigeDeductedOnFailure`/`FundsRestored` fields in
  `MsnSim*.xml` (§3.1).
- **Pulling an employee out of advanced training early** (manual p.30) —
  an explicit mid-season interrupt with an intentionally worse outcome than
  waiting.
- **`REB*` random/decision events** (§2.3) — `RandomAndDecisionEventBase`'s
  naming ("...AndDecision...") suggests at least some of these present a
  choice rather than just narrating an outcome; not yet confirmed which
  ones, worth checking `RandomEventsManager.cs` and a sample `REB*.cs` with
  actual branching logic (`DEAvoidPrestigeDropAtACost.cs` is a likely
  candidate — a single-purpose decision event, "DE" probably = "Decision
  Event").
- Multiplayer-specific: `PBEMManager`'s resign/claim-on-timeout actions (§5)
  are themselves mid-game player decisions outside the normal turn loop.

**None of these have an obvious equivalent in a daily-batch, nobody's-online
resolver.** `design.md` §4 needs an explicit policy before mission
simulation (§2.1, now traced — see §8) can be finalized for Launch Window:
candidates are pre-declared conditional policies submitted with the rest of
the turn ("scrub this mission if X happens"), deferring the decision to the
*next* day's turn (the mission/event waits, at some in-fiction cost), or an
auto-resolve default the player can override at latest sign-in before that
day's server tick. Flagged in `design.md` §4 as an open question. **Update,
§8 tracing (2026-09-27): the step-by-step roll sequence itself needs no
player input at all** — it's a fully automatic dice-and-consequence chain
once triggered, whether or not the mission spans one season or several (§8
confirms it commonly spans several). The genuine mid-turn interaction points
listed above (`ETLaunchSchedMsnsOverlay`'s scrub/resume/assign, training
pull-out, `REB*` choices) all happen strictly *before* a season's resolution
runs, not interleaved with it — good news for the daily-batch model, since it
means "resolve this mission's current season" can be one uninterrupted
server-side step once the turn's pre-resolution decisions are in.

## 8. Mission simulation: the resolution formula (traced 2026-09-27)

Full trace of `SPMRandom.cs`, `Mission.cs`/`MissionStep.cs`/
`MissionStepOutcome.cs`, `MissionsSimulationsManager.cs`,
`MissionSimBaseScreen.cs` (the screen class turns out to own the actual
resolution logic, not a manager — `ComputeProbabilityOfSuccessForMissionStep`/
`SimulateMissionStepFirstDiceRoll`/`SimulateMissionStepSavingThrow`, all
**code-confirmed**), `ConstantsManager.cs`, and several `MsnSim*.xml` samples
under `Mission Simulations Container\Program Category 00 Container\` (the
Sputnik-era category). Only the Normal-difficulty numbers were characterized
throughout, per `design.md` §2.10's decision to ignore Hard/BuzzHard.

### 8.1 A mission is a multi-season entity; each season is its own file

**Code-confirmed.** A mission configuration's simulation data isn't one
file — it's one file *per season the mission runs*:
`MsnSim<PgmCtgID>-<PgmID>-<MsnCnfgID>-<Season>.xml`
(`MissionsSimulationsManager.GetMissionSimulationDataScriptFileName`). A
one-season mission has only a `-01` file; a longer one has `-01`, `-02`,
`-03`, etc. Real example, `Program Category 00` (Sputnik-era), mission
config `01`: `MsnSim00-00-01-01.xml` (season 1: `NumLaunches="1"`, the actual
launch — `LAUNCH_PREPARATIONS`/`COUNTDOWN`/`LAUNCH`/`ASCENT`-type steps),
`-02.xml` and `-03.xml` (`NumLaunches="0"`, a single
`ADVANCED_EXPERIMENTS_EARTH_ORBIT` step each, prestige rising 25→50 — the
satellite is already up, the "mission" is now just continuing to operate it
across additional seasons). `ScheduledMissionToLaunch`/
`ScheduledMissionInProgress.TotalSeasons` (`ScheduledMissionBase.cs`) is the
authoritative count; `Player.cs`'s season-advance logic only ends/removes a
mission once `CurrSeason == TotalSeasons`.

**Direct implication for Launch Window's "one turn = one season" decision
(design.md §1/§3.6): this was never in tension with BASPM's own model.**
BASPM already resolves one mission-season's worth of steps per season-advance
and leaves the mission "in progress" otherwise (`ETLaunchSchedMsnsOverlay`'s
"MISSION_IN_PROGRESS, CURRENT SEASON: n/N" label, code-confirmed) — a
turn-resolution job that processes "each in-progress mission's *current*
season's step batch, once, per turn" is a direct, faithful port of the
original mechanic, not an adaptation of it.

### 8.2 Step resolution: two rolls, not one

Each step's outcome (`MissionStepOutcomeT`: `NOT_PERFORMED`, `CATASTROPHIC`,
`FAILURE`, `MINOR_GLITCH`, `MINOR_GLITCH_SOLVED`, `SUCCESSFUL`) comes from up
to two sequential rolls against a probability derived from involvement-
weighted reliability/skill sums (all **code-confirmed**, `MissionSimBaseScreen.
ComputeProbabilityOfSuccessForMissionStep`/`SimulateMissionStepFirstDiceRoll`/
`SimulateMissionStepSavingThrow`):

1. **Compute a base success probability for the step.** For every mission
   component involved in the step (`MsnCmpInvolved`, e.g. a rocket + payload
   each contributing a percentage), weight that component's *current*
   reliability (plus any modifier from prior R&D/reliability-transfer) by its
   involvement % and sum. Separately sum flight-controller contributions
   (involvement % × the assigned controller's relevant skill) and flight-crew
   contributions (involvement % × the assigned astronaut's relevant skill,
   summed per-skill-per-crew-member). **Astronaut skill only ever matters as
   a bonus on top of already-solid hardware**: it's blended in (25% weight)
   only when crew are involved *and* the hardware-only sum already exceeds
   70; below that threshold, or with no crew, the step's success probability
   is hardware-and-controller-skill alone — a good astronaut can't rescue a
   fundamentally unreliable rocket. Flight-controller skill isn't part of
   this base probability at all; it instead scales a *second*,
   separate "saving-throw probability" (base probability × flight-controller
   sum ÷ 100) used only if the first roll fails.
2. **First roll:** one `RandomNext(0,100)`; success (step passes cleanly, no
   further roll) if the roll exceeds `100 − base probability`. Note: a
   `DifficultyFactor * randomNumber` value is computed alongside this roll
   but **only feeds a debug UI label, never the actual pass/fail comparison**
   — despite being loaded per-step from XML and looking load-bearing,
   `DifficultyFactor` does not mathematically affect step-outcome odds in
   this code path (its real use is elsewhere — `MissionsSimulationsManager.
   GetMissionStepsSumOfDifficultyFactors` — not traced in this pass, low
   priority since it doesn't change the odds formula).
3. **If the first roll fails, a saving throw follows** — the mechanical
   version of "flight controllers catch/fix the problem in real time"
   (confirms the guess in `baspm.md` §2.1/§7.2). A fresh roll against
   `100 − saving-throw probability`: rolling *above* that threshold flips the
   outcome all the way to `MINOR_GLITCH_SOLVED` (full credit, as if nothing
   went wrong) — i.e. skilled flight controllers can fully rescue a step the
   hardware/base roll already lost. Rolling at or below it still fails, but
   which flavor of failure depends on where in the failing range the roll
   lands, using two **Normal-difficulty** tuning multipliers from
   `Constants.xml`'s `MissionSim/MissionSimSavingThrow` (`Value1` —
   `MaxValueForCatastrophicFailureMultiplier`/`MaxValueForRegularFailureMultiplier`,
   both scaled by the saving-throw chance-to-fail): the lowest slice of the
   failing range is `CATASTROPHIC` (only possible on steps flagged
   `CrewCanPerish`, else it downgrades to plain `FAILURE`), the next slice up
   is `FAILURE`, and the remaining slice just under the threshold is
   `MINOR_GLITCH` (a lesser, only-half-credited failure, not a full one).
- **Dead code worth knowing about so it doesn't mislead a future reading of
  the decompile:** `MissionSimBaseScreen.ComputeRandomNumber(DifficultyLevelT)`
  — which would roll twice and take the max on Normal difficulty vs. a single
  roll on Hard/BuzzHard, a plausible-looking "easier difficulty = friendlier
  rolls" mechanic — **is never called from anywhere in the codebase.** The
  live roll everywhere in §8.2 is a single flat `RandomNext(0,100)`
  regardless of difficulty; difficulty only ever enters through the
  Constants-driven multipliers above. Irrelevant to Launch Window either way
  (single shared Normal-only difficulty, `design.md` §2.10), but worth not
  mistaking for a difficulty-scaling mechanic if this file gets read again.
- Outcome → consequence, **code-confirmed**
  (`MissionSimBaseScreen.MoveToNextMissionStep`/`ProcessStep*`): `SUCCESSFUL`
  or `MINOR_GLITCH_SOLVED` → full prestige for that step's goals, mission
  continues to its next step (possibly next season). `MINOR_GLITCH` → half
  prestige, mission still continues. `CATASTROPHIC`/`FAILURE` → the step's
  `PrestigeDeductedOnFailure`, and **the mission ends immediately** — no
  further steps execute, confirming a mission-in-progress is a strict
  sequential chain that stops at its first hard failure, not an
  all-steps-attempted batch.
- **A loose end, not confirmed either way:** `MissionOutcomeT` has a
  `MISSION_OUTCOME_NEARLY_SUCCESSFUL` value, and `ProcessStepFailed`'s switch
  does map a `MINOR_GLITCH` step to it — but `ProcessStepFailed` is only ever
  *called* when the outcome is `CATASTROPHIC`/`FAILURE` (`MoveToNextMissionStep`'s
  guard), so that branch looks unreachable within `MissionSimBaseScreen.cs`.
  A `MINOR_GLITCH` step (half prestige, mission continues) may just leave the
  mission's final reported outcome as `SUCCESSFUL` rather than
  `NEARLY_SUCCESSFUL` — not chased further outside this one file (the results
  overlay or museum/record screens might recompute it by re-scanning step
  outcomes after the fact). Low-stakes either way for Launch Window's own
  design, since we're writing our own outcome taxonomy from real facts, not
  porting this one verbatim — flagged here only so a future reader doesn't
  trust the enum name at face value.
- **Tiger Teams cross-reference, not chased in this pass:** a
  `_savingThrowProbOfSuccessIncludingExpertTeams` variable appears alongside
  the plain saving-throw probability, and `ConstantsManager` separately loads
  `TigerTeamsPercentageBoost`/`TigerTeamsCostMultiplier` (§2.2). Strongly
  suggests Tiger Teams (a hireable boost mentioned in passing elsewhere) work
  by boosting the saving-throw probability specifically, i.e. they make
  flight controllers better at catching an already-failing step rather than
  improving the step's base odds. Worth confirming when the personnel/R&D
  deep dive reaches Tiger Teams, not here.

### 8.3 Determinism and replay

**Code-confirmed, minor but worth knowing:** `SPMRandom` seeds a plain
`System.Random` and replays it deterministically by re-drawing
`RandomUsageCounter` throwaway values on load — i.e. the entire RNG stream
for a save is reproducible from `(seed, usage count)` alone. Not directly a
Launch Window design decision (our own server-authoritative resolver doesn't
need to replay a client's RNG the way a save-game reload does), but relevant
prior art if a "replay this turn's resolution log for a player" debug/spectate
feature is ever wanted.

### 8.4 Verification still needed (in-game / screenshots)

Everything above came from static code + data analysis; a few things are
easier to settle by actually watching the game than by reading further
decompiled code:

1. **RESOLVED (user, 2026-09-27).** Confirmed in-game: a mission that hit a
   glitch mid-flight but got it rescued still reports **Successful** overall
   — `MISSION_OUTCOME_NEARLY_SUCCESSFUL` is effectively dead in practice,
   matching the "unreachable branch" read from the decompile in §8.2.
   Per-step (clicking through the mission's visualization), a rescued step
   is labeled **"Minor Glitch Resolved"**; a step that fails outright is
   labeled **"Failure"**, with every step after it **"Not Performed"**.
   **New open sub-question this raises, not yet re-checked against the
   decompile:** the user describes only these two step-level outcomes as
   actually occurring — a rescued glitch, or an outright failure that ends
   the mission — not a third, distinct "glitch happened, wasn't rescued, but
   the mission continues anyway at half credit" state, which is what §8.2's
   formula trace inferred `MINOR_GLITCH` (as opposed to `MINOR_GLITCH_SOLVED`)
   should look like from the saving-throw banding logic. Needs a fresh look
   at `MissionSimBaseScreen.ProcessStep*`/`MoveToNextMissionStep` specifically
   for whether a plain, unsolved `MINOR_GLITCH` ever actually lets the
   mission continue, or whether in practice every unsolved glitch falls into
   the same failure band that ends it — §8.2's "half-credit, continues" claim
   should be treated as unconfirmed until that's settled.
2. **A screenshot of `ETLaunchSchedMsnsOverlay`** (the season-advance
   Launch/Scrub/Resume screen, manual p.36-ish) mid-multi-season-mission —
   confirms in the UI itself (not just the code label found in §8.1) exactly
   what a player sees/can do for a mission that's continuing into season 2+
   versus one launching for the first time, which will directly inform how
   Launch Window's own turn-summary screen should present "missions in
   progress."
3. **A screenshot or short recording of a `CATASTROPHIC` step outcome that
   has `CrewCanPerish=true`** — confirms in the client UI what "an astronaut
   died" actually looks/reads like (the manual and code establish it happens
   mechanically via `Player.AddFallenAstronaut`, but not what's communicated
   to the player, which matters for Launch Window's own tone/UX on the same
   moment).
4. **Confirm whether `DifficultyFactor` (per-step, loaded but seemingly
   unused in the odds formula, §8.2) visibly does anything in a real
   playthrough** — e.g. try two mission configs with very different
   `DifficultyFactor` values on an otherwise-similar step and see whether
   failure rates in practice look different. If it turns out truly inert for
   step odds, that's one fewer XML field Launch Window needs to carry
   forward when re-deriving its own mission-content schema.
5. **A save-game pair (one post-success, one post-`MINOR_GLITCH`) for the
   same mission configuration**, if convenient to produce — would let a
   future session diff `SaveSlot*.xml`'s mission/prestige records the same
   way §2.1's "real save example" already did for reliability, and settle
   #1 above from save data directly instead of a screenshot.
