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
| `VAB` | 20 | Vehicle Assembly Building — scheduled/in-progress missions, rocket/payload assembly (manual p.35-36). Launch Window's own main-menu hub has a confirmed `VAB` hotspot, distinct from `HEADQUARTERS` — see `design.md` §3.13 | manual-confirmed |
| `WM` | 16 | Space Complex screen (the main hub with the 7 building icons, day/night cycle) — **internal codename doesn't match the manual's own name for it** ("Space Complex Screen", p.34); `WorldMapBaseScreenGameLogic.cs`/`WMEmployeeCenterButtonER.cs` etc. are this screen's code. **This is Launch Window's own main menu** — real prepared art independently landed on the same 7-building-hub shape, see `design.md` §3.13 | inferred (mapping to the manual's "Space Complex Screen" is a strong guess, not proven) |
| `REB` | 14 | `RandomAndDecisionEventBase` subclasses, "bad" random events (manual p.28, "RANDOM EVENTS") — traced, see §12, **none of them present a player choice** despite the base class's "...AndDecision..." naming | confirmed (base class name + manual section both line up); choice-presenting behavior corrected, see §12.2 |
| `REG` | 5 | Sibling family to `REB`, missed by this table until the §12 pass — "good" random events (`REGExtraFunding` etc.), same non-interactive shape | confirmed, see §12.1 |
| `Museum` | 14 | Historical stats / achieved-goals / memorial browser (manual p.40, "MUSEUM"). Confirmed `MUSEUM` hotspot on Launch Window's own main menu, and the flagged home for §1.1's educational-encyclopedia content — see `design.md` §1.1/§3.13 | manual-confirmed |
| `ET` | 11+ (undercounted, see above) | **Not just the News Screen** — covers the *whole end-of-turn sequence* triggered by the Space Complex screen's "End season" button (manual p.35, item 7): `ETInterseason*` (the news screen, matching manual p.34 "NEWS SCREEN" — `ETInterseasonOverlay.cs`'s `NewsItemType` enum incl. `CampaignRTTM*OpposingFaction`), then `ETRDResults*`/`ETRDR*` (an R&D-results step, matching the `RDResults*` asset bundles, §4), then `ETLaunchSchedMsns*` (**a per-mission Launch/Scrub/Resume/AssignMC decision step** — `ETLaunchSchedMsnsOverlay.cs`, `ETLaunchSchedMsnScrubButtonER.cs` etc.). **This means BASPM's turn resolution is itself interactive, not a single non-interruptible batch step** — see §7.2, this is directly relevant to designing Launch Window's own daily resolver. | confirmed (content, not just naming) |
| `PAO` | 4 | Public Affairs Office — budget & prestige overview (manual p.40, "PUBLIC AFFAIRS OFFICE") | manual-confirmed |
| `DE` | 1 | Only one file, `DEAvoidPrestigeDropAtACost.cs` — **confirmed dead code, see §12.2**: its applicability check is hardcoded to always return false, every method body is empty, and the manager method that would even roll for it is never called from anywhere in the turn-resolution flow. Not "a scripted decision event" as an earlier draft of this file guessed — a never-finished scaffold. | confirmed dead, see §12.2 |
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
  the non-existent old §8.1). **Root cause found (§9 personnel trace,
  2026-09-27):** `SETEmployeeStateT` actually defines *two* R&D states,
  `ASSIGNED_TO_RD_ONE_TURN` and `ASSIGNED_TO_RD_INDEFINITE` — but a
  codebase-wide search found only one call site that ever sets either of
  them (`UIScreenOrOverlay.SetSETEmployeeAssigned`), and it always passes
  `ASSIGNED_TO_RD_ONE_TURN`. `ASSIGNED_TO_RD_INDEFINITE` is dead code, never
  reachable through play. Nothing in the season-advance logic
  (`Player.UpdatePersonnelSETSpecific`, §9.2) ever reverts an assignment out
  of `ASSIGNED_TO_RD_ONE_TURN` — it's a misleading name for what the only
  live code path actually treats as an indefinite standing order.

### 2.2 Manager classes (one per major subsystem, all extend `CoreManager`)

| Manager | Lines | Likely scope |
|---|---|---|
| `ProgramsManager.cs` | 764 | The Sputnik→Apollo program/tech tree (`Programs.xml`) — traced, see §10 |
| `MissionsConfigurationsManager.cs` | 740 | Mission configuration variants per program (`MsnCnfg*.xml`) — traced, see §10 |
| `MissionComponentsManager.cs` | 631 | Rockets/capsules/components catalog + reliability — traced, see §10 |
| `PersonnelManager.cs` | 595 | Astronauts/flight controllers/SET hiring, skills, training — traced, see §9 |
| `MissionsSimulationsManager.cs` | 450 | Loads & drives one mission's live simulation (see §2.1) |
| `MissionAssignmentsManager.cs` | 449 | Assigning personnel to mission slots |
| `SpaceComplexDataManager.cs` | 424 | The player's space complex/facility layout |
| `GoalsManager.cs` | 392 | Prestige-granting goals (`Goals.xml`) — traced, see §11 |
| `CampaignManager.cs` | 113 | Single-player `CampaignGSA` scripted objectives (`GSACampaignObjectives.xml`) — see §5 for why this is relevant to multiplayer design, and §12.3 for the traced objective-system shape |
| `ConstantsManager.cs` | 143 | Loads `Constants.xml`/`Constants-<Agency>.xml` — tuning knobs (initial funds, budget-review thresholds, Tiger Teams boost/cost, mission-sim "saving throw" multipliers) — budget-review thresholds traced, see §11 |
| `RandomEventsManager.cs` | 96 | Picks/fires `REB*`/`REG*` random events — traced, see §12.1 (**entirely hardcoded in C#, no Data Scripts XML involved at all** — the one system in the whole codebase that isn't data-driven) |
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
  (manned lunar landing by end of **1974**, not 1973 as an earlier draft of
  this file guessed — code-confirmed exact, `CampaignManager.
  GetLongTermGSACampaignObjectives`'s `deadlineYear=1974, deadlinePeriod=4`,
  §12.3) plus periodic short-term objectives from `GSACampaignObjectives.xml`
  (`CampaignManager.cs` only initializes these `if playerSpaceAgency == GSA`).
  **Traced in full, see §12.3.** Its mechanical identity as a playable
  faction outside that campaign (access to every program/component
  regardless of tag, plus a unique cross-lineage Reliability Transfer bonus)
  is traced separately in **§13**.
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
    `RequiredMsnComponents`. **Traced, see §10.**
  - `MsnCnfg<PgmCtgID>-<PgmID>.xml` (57 files) — mission configuration
    variants for each program: duration, prestige granted/deducted, goals
    achieved, launch info, `GoalBoost` (extra goal progress for
    over-delivering, e.g. launching more Sputniks). **Traced, see §10.**
  - `Goals.xml` (207 goals: 34 generic, 173 non-generic) — every
    prestige-granting goal, first/second/later-time prestige values,
    `GoalsCancelled` (a goal can obsolete an earlier one). **Traced, see
    §11.**
  - `MissionComponents.xml` (85 entries) — the rocket/capsule/component
    catalog. **Traced, see §10.**
  - `ProgramCategories.xml` (13 shipped categories, though the code
    references up to 40 IDs — see §10.2) — the grouping programs fall under
    (satellite, manned orbital, lunar, etc. — `PgmCtgID` from `Programs.xml`).
    **Traced, see §10.**
  - `AstronautCandidates-<Agency>.xml`, `MissionControlCandidates-<Agency>.xml`,
    `SETCandidates-<Agency>.xml` — hiring pools, per agency.
  - `ReliabilityTransfers-<Agency>.xml` — how reliability carries over
    between mission component versions/upgrades. **Traced, see §10.3** —
    exact formula now confirmed, was manual-only before this pass.
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
  attributes appear here, but **correction (§10.4, 2026-09-27): no code
  anywhere reads them — post-mission reliability changes are actually driven
  by three global outcome-based ranges in `MsnCmpsManagerConstants.xml`
  instead**, not these per-step XML attributes.
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
   gives the plain-English skill/morale/training model — §7 below. **Done —
   see §9.**
4. **Programs & mission configurations** (`ProgramsManager.cs`,
   `MissionsConfigurationsManager.cs`, `Programs.xml`/`MsnCnfg*.xml`) — the
   content tree players progress through. **Done — see §10.**
5. **Budget & prestige** (`ConstantsManager.cs`'s `BudgetReviewConstants`,
   `PAOMainOverlay.cs`, `Goals.xml`) — the resource loop and scoring. **Done —
   see §11.**
6. **Campaign objectives / goals / random events** (§2.3) — the
   meta-progression and variance layer on top of the above. **Done — see
   §12.**
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
  risk/reward lever, not just a checklist. **All three claims now
  code-confirmed, see §11.3/§11.4** (this bullet was manual-only when
  written).
- **Prestige → Funds**: prestige earned/lost from mission success/failure
  directly determines next budget review's funding. **Code-confirmed, exact
  formula in §11.1** — it's a tiered lookup with a catch-up adjustment, not a
  simple proportional conversion.
- **Seasons**: **confirmed exactly** one season = one quarter of a year (4/
  year). **Budget review every 4 years** (16 seasons), determining funding
  for the next 4-year cycle based on prestige earned. **Code-confirmed,
  §11.1** — and the funding is a recurring per-season grant locked in until
  the next review, not a lump sum.
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
- ~~**`REB*` random/decision events** — `RandomAndDecisionEventBase`'s naming
  ("...AndDecision...") suggests at least some of these present a choice
  rather than just narrating an outcome~~ — **RESOLVED, not actually a
  mid-turn case at all (§12.2, 2026-09-27): zero interactivity.** Every
  `REB*`/`REG*` event just rolls and applies automatically, reported
  after the fact as a news item. A real choice-presenting mechanism does
  exist in the code (`DecisionEventBase`'s `Option1Selected`/
  `Option2Selected`), but it's entirely dead: the only class that extends
  it, `DEAvoidPrestigeDropAtACost`, has its applicability check hardcoded to
  always return `false` and every method body empty, and the manager method
  that would even roll for a decision event is never called from anywhere in
  the actual turn-resolution flow. BASPM's shipped game contains no
  interactive random events whatsoever, despite the scaffolding suggesting
  otherwise — this was this project's oldest unresolved open question
  (present since the very first analysis session) and it resolves to "not a
  case at all," the cleanest possible outcome for the daily-batch model.
- Multiplayer-specific: `PBEMManager`'s resign/claim-on-timeout actions (§5)
  are themselves mid-game player decisions outside the normal turn loop.
- **Tiger Teams (§9.5, code-confirmed 2026-09-27)** — the one genuinely
  *mid-mission-step* pause found so far, not just a between-steps one: the
  live simulation pauses and asks whether to pay to boost a specific step's
  saving-throw odds, the instant that step is found to need one.

**None of these have an obvious equivalent in a daily-batch, nobody's-online
resolver.** `design.md` §4 needs an explicit policy before mission
simulation (§2.1, now traced — see §8) can be finalized for Launch Window:
candidates are pre-declared conditional policies submitted with the rest of
the turn ("scrub this mission if X happens"), deferring the decision to the
*next* day's turn (the mission/event waits, at some in-fiction cost), or an
auto-resolve default the player can override at latest sign-in before that
day's server tick. Flagged in `design.md` §4 as an open question. **Update,
§8 tracing (2026-09-27): the step-by-step roll sequence itself needs
*almost* no player input** — it's a dice-and-consequence chain that runs
automatically once triggered, whether or not the mission spans one season or
several (§8 confirms it commonly spans several). The genuine mid-turn
interaction points listed above (`ETLaunchSchedMsnsOverlay`'s scrub/resume/
assign, training pull-out) all happen strictly *before* a season's
resolution runs, not interleaved with it — **`REB*` choices dropped from this
list entirely, per §12.2: there turned out to be no such choices to
interleave.** **Correction, §9 tracing
(2026-09-27): this "no interleaved input" claim has exactly one confirmed
exception — Tiger Teams (§9.5).** `MissionSimBaseScreen.AnimateMissionStepOutcomeSuspense`
literally calls `MissionSimPlayer.PauseUnpauseMission(value: true)` and pops
a hiring overlay the instant a step's first roll comes back needing a saving
throw, waiting for the player to pick "no thanks" or pay for a
saving-throw boost before the roll continues — a real, code-confirmed,
mid-*mission-step* pause, not just a between-steps or between-seasons one.
Good news for the daily-batch model regardless: one of the four options is
literally "decline, no cost, no boost," giving a trivial, risk-free
auto-resolve default for a turn where nobody's online to see the prompt (see
`design.md` §2.2(c)/§4). So "resolve this mission's current season" still
doesn't need a *player* mid-resolution, but the server-side resolver now
needs to know it must supply *some* Tiger-Teams answer (a pre-set budget
policy, or simply always-decline by default) at that exact junction, not
just at the season's start.

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
   each contributing a percentage), weight that component's *effective*
   reliability by its involvement % and sum. **Precise composition of
   "effective reliability," code-confirmed (§11.3, 2026-09-27) — corrects
   the "plus any modifier" hand-wave in an earlier draft of this section:**
   `max(0, CurrReliability + GetPlayerMissionComponentTotalReliabilityModifier(...))`,
   where `CurrReliability` already bakes in Reliability Transfer (§10.3) and
   every prior use's R&D/mission-outcome adjustment (§10.4), and the modifier
   term is a **separate, per-mission-attempt (not persisted) bonus/penalty**
   from this specific mission configuration's declared goal dependencies/
   boosts (§11.3) — flying without a prerequisite goal already achieved
   penalizes just the components that goal is declared against; having
   already achieved a bonus-goal boosts them instead. Separately sum
   flight-controller contributions
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
   should be treated as unconfirmed until that's settled. **Downgraded from
   blocking to merely curious (user design input, 2026-09-27, `design.md`
   §2.5(c)):** Launch Window is now leaning toward designing its own
   "vital vs. optional step" outcome model from first principles rather than
   porting BASPM's exact step-outcome tiering, so this question no longer
   gates any Launch Window design decision — still worth settling for
   completeness if it comes up naturally, not worth spending a dedicated
   pass on.
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
4. ~~Confirm whether `DifficultyFactor` visibly does anything in a real
   playthrough~~ — **RESOLVED by static analysis, no verification needed
   (§10.5, 2026-09-27):** inert for step odds as suspected, but it does
   scale a mission configuration's total cost via
   `GetMissionConfigurationAverageDifficultyFactorAccrossAllSeasons`.
5. **A save-game pair (one post-success, one post-`MINOR_GLITCH`) for the
   same mission configuration**, if convenient to produce — would let a
   future session diff `SaveSlot*.xml`'s mission/prestige records the same
   way §2.1's "real save example" already did for reliability, and settle
   #1 above from save data directly instead of a screenshot.

## 9. Personnel: hiring, skills, training, and the employee lifecycle (traced 2026-09-27)

Full trace of `Employee.cs`/`PersonnelCandidateOrEmployee.cs` (shared base),
`AstronautEmployee.cs`/`SETEmployee.cs`/`MissionControlEmployee.cs` (the three
groups), `Player.cs`'s `UpdatePersonnel*` season-advance methods,
`PersonnelManager.cs`, and `PersonnelManagerConstants.xml`. This upgrades
§7.1's manual-only personnel summary to **code-confirmed** throughout except
where noted otherwise, and corrects/sharpens it in a few places.

### 9.1 Three groups, five skills each, one derived sixth for Mission Control

**Code-confirmed**, matches §7.1's manual-derived model exactly:
- **Astronauts** (`AstronautEmployee.AstronautSkillT`): Leadership, Piloting,
  EVA, Science, Fitness.
- **SET** (`SETEmployee.SETEmployeeSkillT`): one skill per mission-component
  group — Rockets, Space Probes, Human-Rated Rockets, Crewed Spacecraft, EVA
  Suits.
- **Mission Control** (`MissionControlEmployee.MissionControlSkillT`):
  Propulsion, Trajectory & GNC, Spacecraft Systems, Crew Systems & Payloads,
  Mission Operations, plus **Flight Director** as a sixth named enum value —
  but it has no independent stored value: `PersonnelCandidateOrEmployee.
  GetSkillCompetency(index)` returns the plain average of the other five
  whenever `index >= 5`. A Flight Director isn't a specialist trained
  separately; "how good a Flight Director someone is" is arithmetically just
  how well-rounded they are.
- All three groups share one base class (`Employee` : `PersonnelCandidateOrEmployee`)
  and the same five underlying stat slots (`_skillsCompetencies[5]`); which
  five things those slots *mean* is the only thing that differs by group.
  **Educational note:** Mission Control's five categories map cleanly onto
  real NASA flight-control disciplines — Propulsion (EECOM-adjacent),
  Trajectory & GNC (FDO/GUIDO), Spacecraft Systems, Crew Systems & Payloads,
  Mission Operations — and Flight Director being "the average of your team,"
  not a separately-trained specialist, is a decent abstraction of the real
  role (a Flight Director is a generalist coordinating specialists, not
  another specialist). Flagged into `design.md` §1.1.

### 9.2 Morale, aging, retirement, and resignation — one season-advance pass

**Code-confirmed** (`Player.UpdatePersonnelCommon`, called every season for
every active employee in all three groups):
- **Age** increases by exactly 0.25 every season (matching the
  one-season-per-quarter-year fact already established, `baspm.md` §7.1).
- **Morale**: an *available* (idle) employee loses 1 morale/season. An
  employee who is *busy* (assigned to a mission/R&D, or in training) gains 2
  morale/season, capped at 100. An employee in the separate "unavailable"
  penalty state (see below) gets no morale change at all, just a countdown.
- **Retirement age is per-group and fairly different**: Astronauts 55,
  Mission Control 65, SET 75 (`PersonnelManagerConstants.xml`'s
  `RetirementAge`) — astronauts age out of the corps two decades earlier
  than the engineers who build their hardware, a real-world-plausible
  detail worth keeping if Launch Window keeps a similar three-group split.
  `IsEmployeeApproachingRetirementAge` flags a 3-year advance-warning window
  (`RetirementAgeThresholdInYears`).
- **Resignation is a hard morale floor, not a warning band you can miss**:
  `PersonnelManagerConstants.xml`'s `DisgruntedEmployeesMoraleRange` is
  `MinValue="40" MaxValue="47"` — at or below 40, an available employee
  resigns *that same season* (`Player.ResignEmployee`, called directly from
  the season-advance loop, no separate "at risk" state in between).
  41–47 is a separate, softer **"disgruntled" reporting band**
  (`Player.GetDisgruntledEmployeesFromPersonnelGroup`) that only ever
  produces an end-of-season news item — despite there being a
  `DisgruntledEmployee` class, it's a plain data record for that news
  report, not a distinct lifecycle state; nothing about actually being
  "disgruntled" changes an employee's behavior beyond the warning.
- **Crucially, retirement and resignation are only ever checked while an
  employee is available (idle)** — the season-advance loop's age/morale
  checks live inside the `IsAvailable()` branch. A busy employee (assigned
  to a mission/R&D, or in training) who crosses the retirement age or would
  have resigned simply doesn't that season, however marginal their morale or
  however far past retirement age — the check only fires once they're free
  again. Worth deciding deliberately whether Launch Window keeps this
  "busy employees are protected" property or closes it as a loophole.
- `ResigningEmployee`/`RetiringEmployee`/`DismissedEmployee`/
  `FailedBasicTrainingEmployee` are all thin `InactiveEmployee` subclasses —
  archival snapshot records for save/history/museum purposes (name, when,
  last assignment), not additional state-machine states. The real lifecycle
  is fully captured by `Employee.ActiveOrInactiveStatusT` (`Active`,
  `Dismissed`, `Resigned`, `Retired`, `Fallen`).
- **Dismissal is the one lifecycle exit that's a genuine player action**
  (`Player.DismissEmployee`, called from UI) rather than an automatic
  season-advance consequence — resignation/retirement/failing-basic-training
  are all automatic.

### 9.3 Training: two modes, a real learning-capacity formula, and a precise pull-out penalty

**Code-confirmed.** Basic training duration is group-specific and notably
uneven: **Astronauts 4 seasons**, **SET 2 seasons**, **Mission Control 2
seasons** (`NumSeasonsForBasicTraining` overrides) — astronaut candidates
take twice as long to clear basic training as the other two groups, a
plausible echo of how demanding real astronaut selection/training is.
Advanced training is a flat **3 seasons** for all three groups.

- **The learning-capacity formula, confirmed exactly as the manual implies**
  (`PersonnelManagerConstants.xml`'s doc comment plus `Player.
  UpdateEmployeeSkillsAfterAdvancedTrainingInner`):
  `improvement = (MaxSkillCompetencyValue − currentSkill) × random(0.3, 0.6) × (LearningPotential ÷ MaxSkillCompetencyValue)`,
  where `MaxSkillCompetencyValue = 98` (not 100 — nobody ever caps out
  completely). This is a genuine diminishing-returns curve (the closer to
  the cap, the smaller each training round's gain) scaled by a per-employee
  learning-potential fraction — concretely confirms §7.1's "low skill + high
  learning capacity can beat adequate skill + low learning capacity" claim,
  and gives Launch Window a ready-made, easy-to-port formula shape (re-derive
  the actual constants, per `design.md` §1.2).
- **Two training modes, both code-confirmed:** a fixed-length advanced
  training round (`SendToAdvancedTraining`), or "**permanent** advanced
  training" (`SendToPermanentAdvancedTraining`, a target skill level) which
  auto-re-enrolls the employee into another 3-season round every time one
  completes, for as long as their skill is still below the target — a
  genuine standing order requiring no re-declaration, structurally the same
  shape as §2.1's R&D-assignment finding.
- **The mid-training pull-out penalty, now precisely formulated** (sharpens
  §7.2's "reduced results, no refund" claim): pulling an employee out early
  computes the *same* improvement formula, but scales the random factor by
  `seasonsAlreadyCompleted ÷ totalSeasonsForThatTrainingType` — i.e. genuine
  **linear partial credit for elapsed training time**, not an arbitrary
  penalty and not zero. Pull someone out after 1 of 3 advanced-training
  seasons and they keep roughly a third of a full round's worth of expected
  gain; the season(s) already spent aren't wasted, just not fully banked.
- **SET R&D skill growth is a different, simpler shape than training or
  mission-based growth** — flagged explicitly in `PersonnelManagerConstants.xml`'s
  own comments as an intentional contrast: while assigned to R&D, a SET
  employee's skill-in-use increases by a **flat** `random(0.2, 0.4)` per
  season (no distance-to-cap scaling at all), vs. the diminishing-returns
  curve used everywhere else (training, and mission-based astronaut/
  flight-controller growth, §9.4). Worth deciding deliberately whether
  Launch Window keeps two different growth shapes for two different
  activities, or unifies them.
- **Astronaut/flight-controller skill growth from actually flying a
  mission is a separate mechanic from training, code-confirmed and
  previously unexplored** (`PersonnelManager.
  UpdateFlightCrewsAndFlightControllersSkillAfterMission`, called from
  `MissionSimBaseScreen.cs` on **both** the success and the failure path —
  flying the mission is what matters, not the outcome): assigned flight crew
  get **all 5 of their skills** bumped via the same diminishing-returns
  formula; an assigned flight controller gets **only the one skill they were
  actually using** bumped the same way — *unless* they were serving as
  Flight Director (skill index 5), in which case the gain is computed once
  from their average-skill-to-cap distance, divided by 5, and spread evenly
  across all 5 base skills. Directly ties personnel back into §8's mission
  formula: flying missions is how astronauts and controllers actually grow,
  not (for astronauts) an explicit "send to training" action at all —
  astronauts have no on-the-job-vs-training-only skill tension the way SET's
  R&D-flat-growth vs. training-diminishing-growth choice does.

### 9.4 Hiring: candidate quality is fed by the group's own current morale

**Code-confirmed, a systemic feedback loop not mentioned in §7.1.**
`PersonnelManagerConstants.xml`'s `CandidatesProfilesSets` defines several
named quality-distribution tables, each keyed by a `MoraleUpperLimit`; when a
new batch of 10 recruits (`HiringBatchSize`) is generated, the table actually
used is chosen by **the hiring group's own current average morale** — a
demoralized astronaut corps quite literally attracts worse recruits (mostly
"Poor"/"Average" skillsets, "Generalist" specialization) than a thriving one
(predominantly "VeryGood"/"Excellent", including "VerySkilledInTwoAreas" and
outright "Specialist" candidates). This makes morale management a
double-lever system: it's not just about not losing existing staff to
resignation (§9.2), it's also actively shaping who you're even offered to
hire next. A strong, self-contained mechanic worth carrying into Launch
Window's own personnel design, independent of whether the exact skillset/
morale/learning-potential bucket numbers are kept.

**Correction (2026-10-01): the numbers *are* recoverable after all — the
2026-09-30 search just looked in the wrong half of the dump.**
`PersonnelManagerConstants.xml` isn't inside `extracted/`/`monobehaviours/`
(the packed-asset dump that search covered); it's a **loose file on disk**
in the game install itself, `SPM-Windows_Data\Scripts\Data
Scripts\General Container\PersonnelManagerConstants.xml` — same lesson
§10.8 draws for the R&D-reliability formula's own constants file, found the
same session by checking the install directly instead of the extraction
dump.

**Actual bands, six tiers by `MoraleUpperLimit` (50/60/70/80/90/100), each
listing 5 `CandidateProfile` rows with a `Ratio` (sums to 1.0 per tier):**
only the two lowest tiers ever offer a "Poor" skillset at all —
`MoraleUpperLimit="50"` at `Ratio="0.2"`, `MoraleUpperLimit="60"` at
`Ratio="0.1"` — every tier above 60 never offers "Poor" again. **This is
directionally consistent with the bot harness's own numbers** (`design.md`
§3.10: "Poor" landed 0 times across 200 games/26,589+ "Excellent" draws),
though not a confirmed match of the exact mechanism: Launch Window's own
`HiringPool` picks one deterministic skill band from the roster's average
morale (`POOR_MORALE_CEILING = 44`, its own re-derived number per
`design.md` §1.2), where BASPM instead mixes several candidate profiles
probabilistically within one of six wider tiers — the two aren't directly
comparable band-for-band. What *is* fair to say: in both systems, a
roster's average morale has to be sitting fairly low (at or below 60 in
BASPM, not far above the `DisgruntedEmployeesMoraleRange MinValue="40"`
resignation floor the same file confirms) before "Poor"/worst-tier
candidates become reachable at all, and ordinary churn is normally enough
to keep morale above that line in both — **plausibly the same underlying
shape, not confirmed as the same mechanism** — a looser claim than the
one this correction originally made.

(The same file also confirms, in passing, real BASPM's own retirement
ages — `ASTRONAUTS="55"`, `MISSIONCONTROL="65"`, `SET="75"` — and its
skill cap, `MaxSkillCompetencyValue="98"`, both already known not to be
reused verbatim per `design.md` §1.2, so no action needed, just noted for
completeness now that the source is in hand.)

### 9.5 Tiger Teams: confirmed — a pay-to-boost-the-saving-throw mechanic, and a real mid-step pause

**Code-confirmed, resolves the loose end flagged in §8.2.** The hypothesis
was right: Tiger Teams boost the *saving throw* specifically, never the
step's base success probability. Full mechanism
(`TigerTeamsHiringOverlay.cs`, `MissionSimBaseScreen.
AnimateMissionStepOutcomeSuspense`, `ConstantsManager.
GetTigerTeamsPercentageBoost`/`GetTigerTeamsCostMultiplier`):

- The instant a mission step's first roll comes back needing a saving throw,
  the live simulation **pauses** (`MissionSimPlayer.PauseUnpauseMission(true)`)
  and presents four options: decline (free, no change), or hire 1/2/3 Tiger
  Teams at increasing cost, each buying a bigger slice of the *remaining gap
  to 100%* saving-throw probability (`boost = percentageBoost(option) ×
  (100 − currentSavingThrowProb)`) — the same diminishing-returns shape as
  §9.3's training formula, applied to odds instead of skill. Cost scales
  with the mission's total cost (`missionCost × costMultiplier(option)`,
  `Constants.xml`: multipliers `0 / 1 / 1.75 / 2.5`, boosts `0% / 25% / 40% /
  50%` of the remaining gap — identical across all three difficulty tiers,
  so this mechanic is untouched by `design.md` §2.10's Normal-only
  decision).
- **This corrects §7.2/§8's "the roll sequence needs no player input at
  all" conclusion** — it needs no input *between* steps or seasons, but this
  one juncture genuinely pauses *inside* a step's resolution for a live
  decision. See §7.2's updated correction note and `design.md` §2.2(c)/§4.
  The saving grace for the daily-batch model: "decline" is free and always
  available, so a trivial, harmless auto-resolve default exists (unlike
  scrub/training-pull-out, which have real trade-offs either way — `REB*`
  turned out not to belong on this list at all, §12.2) — Launch Window's
  resolver can simply auto-decline unless a
  player has pre-set a spending policy, without that default ever being a
  strictly bad choice for the player who didn't log in.

### 9.6 A related, unrelated-looking loose end noticed but not chased

**Minor, low-priority — noted so it isn't lost.** `Constants.xml`'s
`MissionSim/MissionSimSavingThrow` block (the catastrophic/regular-failure
multipliers §8.2 already covers) is actually laid out as **three whole
`<Values>` rows, one per difficulty tier**, each carrying both named
multipliers as attributes — not, as §8.2's prose shorthand implies, a single
row with `Value1`/`Value2`/`Value3` standing for the three difficulties.
Doesn't change any number already recorded (this block happens to be
identical across all three difficulty rows anyway), just a phrasing
correction for whoever reads §8.2 closely against the actual XML next.

### 9.7 Verification still needed (in-game / screenshots)

1. **A screenshot of the Tiger Teams hiring popup itself** (§9.5) — confirms
   the exact wording/framing shown to the player at that pause point, useful
   for calibrating Launch Window's own equivalent prompt/default messaging.
2. **Confirm the "disgruntled" news item's actual wording and where it
   surfaces** (§9.2, the 41–47 morale warning band) — the interseason news
   screen presumably, but not confirmed which screen/format, and whether it
   names the employee or just counts them.
3. **A save-game or in-game check of a Mission Control employee acting as
   Flight Director** (§9.1) — confirms in practice (not just from the
   `GetSkillCompetency` averaging code) that the UI itself presents "Flight
   Director skill" as a computed average rather than something a player ever
   sees as an independently trainable number.
4. **Confirm whether the game ever actually offers `ASSIGNED_TO_RD_INDEFINITE`
   through any UI path this pass didn't check** (§2.1/§9.2) — low priority,
   since every code path found treats R&D assignment as indefinite in
   practice regardless of which enum value is active; only worth chasing if
   a future save or screenshot ever shows that state's name somewhere.

## 10. Programs, mission configurations, and reliability (traced 2026-09-27)

Full trace of `ProgramsManager.cs`, `MissionsConfigurationsManager.cs`,
`MissionComponentsManager.cs`, and the `Programs.xml`/`ProgramCategories.xml`/
`MsnCnfg00-00.xml`/`MissionComponents.xml`/`ReliabilityTransfers-NASA.xml`
data files (the live game install's copy under `SPM-Windows_Data\Scripts\
Data Scripts\General Container\` — the `extracted\` dump does **not**
contain these; they only exist unpacked in the actual install directory,
worth remembering next time a Data Scripts file needs a fresh read). This
upgrades manual-only claims in §7.1 to **code-confirmed** and answers the
one previously flagged open question (Reliability Transfer's exact formula).

### 10.1 The containment hierarchy, code-confirmed exactly as §7.1 described

**Program Category → Program → Mission Configuration → Mission Component(s)**,
plus a cross-cutting **Reliability Transfer** layer. Concretely:
- A **Program Category** (`ProgramCategories.xml`) belongs to a
  **Robotic**/**Crewed** group (`ProgramCategoryGroupIDT`) and can require
  one or more prerequisite **Strict Goals** to even become available for
  opening (e.g. Mercury probes require *both* the Venus-probe and
  Mars-probe goals already achieved — multiple `RequiredGoal` entries are
  effectively ANDed).
- A **Program** (`Programs.xml`) sits inside a category, has its own
  `OpeningCost`/`MaintenanceCost`, can carry its own additional
  `RequiredGoal`, and declares `RequiredMsnComponents` — the actual mission
  components it needs built. **`OpeningCostDeductions` confirmed exactly**:
  a list of `(other program, deduction %)` pairs; owning that other program
  knocks the stated percentage off this one's opening cost (summed and
  capped at 100%). Real example: Explorer I (`00-01`) gets a flat **40%**
  discount once Sputnik I (`00-00`) is already open — a genuine, deliberate
  "fast follower" mechanic, not just flavor text (see §1.1 note below).
- A **Mission Configuration** (`MsnCnfg<PgmCtgID>-<PgmID>.xml`) is one way
  of flying a program: `DurationInSeasons`, `NumLaunches`,
  `PrestigeGrantedOnSuccess` (first/second/later-time values, confirming
  §7.1's "diminishing prestige for repeat missions"), `PrestigeDeductedOnFailure`,
  and the goals it grants. **The "extended duration variant" from §7.1 is
  real and directly confirmed**: `MsnCnfg00-00.xml`'s config `1` ("Sputnik -
  Extended mission") is `DurationInSeasons='3'` against config `0`'s `'1'`,
  and its prestige is roughly double (500/250/150 vs. 250/125/75) — a
  genuine higher-risk-longer-commitment, higher-reward alternative to the
  baseline mission, not just a cosmetic label. This is also the concrete,
  named source of §8.1's 3-season Sputnik example — same mission
  configuration, now traced from both ends (data authoring and runtime
  behavior).
- A **Mission Component** (`MissionComponents.xml`, 85 entries in the base
  game) belongs to one `AssociatedSETSkill` group (§9's five SET skills)
  and one `ProgramCategoryGroupID` (Robotic/Crewed), and carries its cost
  triad (`CreationCost`/`MaintenanceCost`/`UnitCost`), its reliability triad
  (see §10.3), and (for rockets) a `MassAndCapacity` payload-to-orbit table.
  **Every mission component in the base game starts at the same tiny
  baseline: `Initial='3.00000'`** — a brand-new, untransferred rocket or
  capsule is a near-coin-flip-of-doom by design; Reliability Transfer
  (§10.3) and R&D (§9.3) are how it becomes flyable at all.

### 10.2 Program categories reveal cut/never-shipped scope beyond the base game

**Code-confirmed, a genuine "the code is bigger than the data" finding.**
`ProgramsManager.GetPgmCtgIDsBasedOnProgramCategoryKind` and the
`ProgramCategoryKindT` enum define **12 solar-system-target category
groupings** — Moon, Sun, Inner Planets, Outer/Dwarf Planets, **Asteroids and
Comets**, **Outer Heliosphere**, Space Planes, Ballistic Capsules, Lunar
Expeditions, **Space Stations**, **SSTO Vehicles**, and **Mars Expeditions**
— referencing category IDs all the way up to `"39"`. But the base game's
actual `ProgramCategories.xml` only ever defines **13 categories**, IDs `00,
02, 04, 06, 08, 10, 14, 16, 26, 27, 28, 29, 30` — covering only the probes
(Earth/Moon/Sun/Mercury/Venus/Mars/Jupiter/Saturn) and the ballistic-capsule/
short-lunar-expedition categories. **Every category ID the code references
for asteroids/comets, the outer heliosphere, space stations, SSTO vehicles,
and Mars expeditions is dead — scaffolded in the enum and lookup tables but
never given actual `Program`/`MsnCnfg` content in this build.** Whether this
is cut content, a DLC/expansion that never shipped, or forward-looking
architecture that was never filled in isn't determinable from the code
alone — but it's a real signal of where BASPM's own designers were already
looking past Apollo, which is squarely useful, non-copyable inspiration for
Launch Window's own long-term content roadmap (the category *names*/domains
are real astronomical/spaceflight targets, not BASPM's invented content —
§1.2's carve-out for real-world facts applies here too).

### 10.3 Reliability Transfer: the exact formula (previously manual-only, now code-confirmed)

**Resolves the one open question going into this pass.** `ReliabilityTransfers-<Agency>.xml`
declares, per **target** mission component, an explicit list of **source**
components and a **per-source transfer percentage**, plus a **cap**
(`MaxReliabilityTransfer`) — "related" is an explicit, hand-authored
per-component list, not a generic "same skill group" heuristic. Real
example, NASA's `JUPITER-C` target: 13 separate rocket sources listed,
transfer percentages ranging 35–60% depending on how closely related each
source rocket actually is (Redstone 50%, Saturn V 60%, Thor-Able 35%), every
one of them capped at the target's own 60% ceiling regardless of how many
sources the player owns.

- **Exact formula, `MissionComponentsManager.ComputeReliabilityTransferForMissionComponent`:**
  `transfer = min(MaxReliabilityTransfer, Σ over owned sources of (source's
  CURRENT reliability × that source's ReliabilityTransferPercentage ÷ 100))`
  — it's driven by the source's *current* (not initial) reliability, so a
  well-developed, heavily-flown source rocket passes on much more than a
  freshly-opened one, and owning several related sources stacks (up to the
  cap).
- **Applied once, at the moment a program is opened** (`Player.AddMsnComponentBought`
  → `GetMissionComponentInitialReliabilityInclRelTransfer`, which takes
  `max(existing tracked reliability, baseline Initial + transfer)`): a new
  component's starting `CurrReliability` is its 3% baseline plus this
  transfer bonus (capped), not a small step up from zero.
- **A naming trap worth flagging precisely, code-confirmed, not yet verified
  in-game:** the field named `InitialReliabilityWithRelTransfer` — which
  post-mission reliability updates (§10.4) treat as a floor a component can
  never drop below — is **never actually set to the transfer-inclusive
  value anywhere in the traced code**; it's initialized once, in
  `MissionComponentsManager.InitializeMissionComponents`, to the exact same
  raw 3% baseline as plain `InitialReliability`, and nothing else writes it
  except save-file deserialization (which just reads back whatever was last
  serialized — itself always that same raw baseline, going by this trace).
  So despite its name, the actual "floor" enforced after repeated mission
  failures (§10.4) appears to be the bare **3% baseline**, not the
  transfer-boosted starting value — meaning a heavily transfer-boosted
  component genuinely can be ground back down near its raw minimum through
  bad luck, harder than the field's name suggests. Not confirmed whether
  this is intentional (transfer is a one-time head start, not a new floor)
  or an oversight; **not a Launch Window design concern either way** since
  we're re-deriving our own formula, but worth getting the *intended*
  behavior right by deliberate choice rather than by inheriting whichever
  one this trace found.

### 10.4 Component reliability changing over time: resolves the MinUpgrade/MaxUpgrade XML mystery

**Corrects a previous-session note.** §3.1 flagged `MissionComponent
MinUpgrade/MaxUpgrade/MinDowngrade/MaxDowngrade` attributes seen in the
Mission Simulations Container XML as apparently driving post-mission
reliability changes. **A codebase-wide search found no code anywhere that
reads attributes by those names** — they don't drive anything; either dead/
informational XML data or read through a path this and the mission-sim pass
both missed. **The actual mechanism, code-confirmed**
(`MissionComponentsManager.UpdatePlayersMissionComponentAfterMission`, called
once per mission per component used): each used component's reliability
change is `(MaxReliability − CurrReliability) × factor`, where `factor` is a
random draw from one of three **global, difficulty-keyed constant ranges**
in `MsnCmpsManagerConstants.xml` (Normal/`Value1` only, per `design.md`
§2.10) selected by that component's own per-use performance category — a
**four-way outcome taxonomy distinct from `MissionStepOutcomeT`** (§8.2):
`SUCCESS` (positive factor), `MINOR_GLITCH` (a smaller positive factor —
**note this is a genuinely distinct, code-confirmed category from a clean
success**, relevant context for the still-open §8.4 item 1 question about
whether an unsolved minor glitch is reachable, though this pass didn't trace
how `MissionStepOutcomeT` maps into this specific enum), `FAILURE` (a
negative factor, scaled down by how much that component was actually
involved in the failure — a component barely involved in a failed step
barely gets penalized), and `NEUTRAL` (no change, for components not
involved in whatever happened that step). Diminishing-returns-shaped by
construction (factor multiplies the *remaining headroom* to max, same
"bigger gains further from the cap" shape as §9.3's training formula) —
another case of this project's preferred diminishing-returns curve shape
showing up independently in a third system.

**Also unassigns SET, same as §10.8's own R&D-side release — found the
same session §10.8 was traced.** Right after computing the new
`CurrReliability` above, `UpdatePlayersMissionComponentAfterMission`
(lines ~595-602) clamps it at `MaxReliability` and checks the resulting
value: **if the post-flight `CurrReliability` is at or above
`MaxRDReliability` — a state check, not a one-directional "pushed past
it," so even a *failed* flight that knocks a component's reliability down
while it's still sitting at/above that ceiling triggers this the same
way** — it calls the same `UnassignAllSETPersonnelFromMissionComponent`
method §10.8 item 5 describes, immediately, not via the interseason
recheck R&D's own release path uses. So any mission flight that leaves a
component's reliability at or above its R&D ceiling releases its
researchers on the spot — a second, more direct trigger for the identical
release mechanic, this one requiring no later recheck since it already
has the post-flight value in hand.

### 10.5 The `DifficultyFactor` XML field: resolves baspm.md §8.4 item 4

**Resolved by this pass, no in-game verification needed after all** — retract
that item from §8.4's list. Per-step `DifficultyFactor` (loaded but, per §8.2,
never read by the actual step-odds roll) **does have a real, confirmed use**:
`MissionsConfigurationsManager.GetMissionConfigurationAverageDifficultyFactorAccrossAllSeasons`
averages every step's `DifficultyFactor` across a mission configuration's
full multi-season run, and that average directly scales the mission
configuration's `TotalCostForPayloads` at load time (harder missions cost
more to fly, mechanically, not just narratively). It's inert for step-outcome
*odds* specifically, exactly as §8.2 found, but very much alive for mission
*cost*.

### 10.6 Async-multiplayer relevance: this system is all clean pre-turn input

**No new mid-turn/mid-resolution decision points found** — opening a
program, choosing a mission configuration, and reliability transfer are all
resolved from state that already exists at the moment a player queues the
action (owned programs, current component reliabilities); none of it
depends on anything rolled during turn resolution, unlike Tiger Teams
(§9.5). This system slots cleanly into "declare intent before the turn,
server resolves mechanically" with no special-casing needed. See
`design.md` §2.3(b)/§2.4(b).

### 10.7 Verification still needed (in-game / screenshots)

1. **Confirm the `InitialReliabilityWithRelTransfer`-as-floor finding
   (§10.3)** — build up a component's reliability via a large reliability
   transfer, drive it down through repeated mission failures, and see
   whether it ever drops below its transfer-boosted starting value or only
   ever floors out at the bare baseline. Low priority (doesn't affect Launch
   Window's own design either way) but would confirm whether this is a real
   BASPM behavior or a trace error.
2. **A screenshot of the Program/Mission Configuration selection screens**
   (HQ's "manage programs" overlay, manual p.33-ish) — confirms how
   `OpeningCostDeductions`, `RequiredGoal` gating, and the "extended
   mission" variant choice are actually surfaced to the player, useful for
   calibrating Launch Window's own program-selection UI.
3. ~~**Trace the actual R&D-to-reliability formula**~~ — **resolved
   2026-10-01, see §10.8.**

### 10.8 R&D reliability gains: the exact formula (resolves §10.7 item 3)

**Code-confirmed (2026-10-01), traced on the main PC with `E:\Temp\BASPM`/
game-install access**, via `MissionComponentsManager.PerformRDProcessOnPlayerMsnCmp`
(static, called every season for every mission component in
`player.GetMissionComponentsToRDIDs()`) plus the loose
`SPM-Windows_Data\Scripts\Data Scripts\General Container\MissionComponents.xml`/
`MsnCmpsManagerConstants.xml` for the actual numbers. **That XML pair lives
unpacked on disk** (`ScriptLocationType.ApplicationDataPathFolder`), not inside
an asset bundle — earlier passes that only searched `extracted/`/
`monobehaviours/` (e.g. the `MoraleUpperLimit` dead end, §9.4) missed it
because they were looking in the wrong half of the dump; worth remembering for
any future "data file not found in the dump" case — check the loose
`SPM-Windows_Data\Scripts\Data Scripts\` tree in the game install itself
before concluding a constants file is unrecoverable.

**The mechanic is substantially richer than the manual-level summary
suggested — five distinct parts, not one:**

1. **A separate, lower ceiling just for R&D, `MaxRDReliability`** — distinct
   from the component's absolute `MaxReliability` (the post-mission-use
   ceiling, §10.4). Per the sample data, `MaxRD` sits roughly 7-10 points
   under `Max` per component (e.g. `JUPITER-C`: `Max=95.05`, `MaxRD=85.16`;
   `ATLAS`: `Max=97.50`, `MaxRD=90.30`) — R&D alone can get a component most
   of the way there, but actually flying it is the only way past that last
   stretch. Both ceilings are separately difficulty-scaled at load time
   (`RelMaxMultiplier`/`RelMaxRDMultiplier`, 1.05/1.0/0.95 across
   Easy/Normal/Hard).
2. **Up to four SET employees can be assigned to the same component's R&D
   simultaneously**, each occupying one of four explicit slots
   (`MissionComponent.AssignSETEmployeeID(slot, id)` — the slot is a
   deliberate player choice, not auto-assigned by skill). Each slot draws
   its raw per-season improvement from its own fixed band, authored per
   component as ascending `RDLevelMaxAmount L1`-`L4` XML attributes:
   **slot 0 draws from `[L3, L4]`, slot 1 from `[L2, L3]`, slot 2 from
   `[L1, L2]`, slot 3 from `[0, L1]`** — e.g. `JUPITER-C`'s `L1..L4 =
   9/14/19/28`, so a lone researcher in slot 0 draws 19-28 raw points, a
   fourth researcher added in slot 3 only ever draws 0-9. This is a second,
   independent diminishing-returns lever on top of the headroom one below —
   stacking researchers on one component has real but sharply shrinking
   marginal value, and *which* slot an employee is placed in matters as
   much as how many are assigned.
3. **Per-slot gain formula:** `rawAmount(slot's band) ×
   ((MaxRDReliability − CurrReliability) / 100) × (assigneeSkillCompetency
   / 100)` — summed across all occupied slots for that season's total
   improvement, then added to `CurrReliability` and clamped to
   `MaxRDReliability`. The `(MaxRDReliability − CurrReliability) / 100`
   term is the "remaining headroom" diminishing-returns factor — same
   shape family as training (§9.3) and post-mission reliability change
   (§10.4), its fourth independent appearance in the codebase. The skill
   term is a flat 0-1 scaling of skill competency (0-100 scale), not
   itself diminishing-returns-shaped.
4. **A stagnation guard:** if remaining headroom drops under `0.1` points
   and any gain was computed, the game snaps straight to `MaxRDReliability`
   instead of letting the per-season gain asymptotically crawl forever.
5. **Reaching `MaxRDReliability` auto-releases every assigned SET
   employee from that component, code-confirmed in `Player.cs` — a
   different, misleadingly-named method than the one above
   (`UpdateMissionComponentAndMakeSETEmployeesAvailableBackAgain`,
   called from inside the R&D pass itself) actually does nothing more
   than record a `MaxRDReliabilityReached` flag on that season's
   `MissionComponentsUpdated` entry, despite its name.** The real
   release is a separate method,
   `Player.ProcessAndRetrieveAllMissionComponentsThatReachedMaxRDReliabilityAtSomePointDuringTheLastSeason`
   (`Player.cs` lines 2184-2200), called from `ETInterseasonOverlay.cs:792`
   (interseason news-item generation, a UI-adjacent call site — not
   re-verified to run at exactly the same resolution step as the R&D pass
   itself, only confirmed to be the same season). It scans every flagged
   component and **rechecks** `CurrReliability >= MaxRDReliability`: if
   still true, it calls `UnassignAllSETPersonnelFromMissionComponent`
   (clearing all four slots and making every employee "available again");
   if the component somehow dropped back below the ceiling by then, it
   just clears the flag instead. The net effect: once R&D alone can't
   improve a component any further, its researchers are automatically
   freed to do something else, rather than sitting parked there
   indefinitely for no further gain.

**`RDDifficultyFactor` (the per-component XML attribute already known about)
plays no role in this formula at all** — confirmed by a codebase-wide
search, it's used exactly once, purely for a UI display stat
(`GetMissionComponentRelativeRDDifficultyFactor`, "this component's R&D
difficulty relative to the hardest component in its skill group," shown to
the player as flavor/comparison info, not consumed by any gain
calculation).

**Decided the same session (user, 2026-10-01), after this trace was
presented as two explicit choices:** adopt BASPM's full shape, both
halves — the two-ceiling mechanic and the four-slot one — rather than
keeping Launch Window's earlier, deliberately-simpler single-assignee
placeholder. Implemented the same session as
`SkillGrowth.rdReliabilityGain`/`drawRdSlotRawAmount`/`rdSlotBandLower`/
`rdSlotBandUpper` and a rewritten `ResearchProgress` (the old
single-assignee `SkillGrowth.reliabilityGain` is gone). See `design.md`
§3.4 for the full decided formula, with Launch Window's own numbers, not
BASPM's.

## 11. Budget, prestige, and goals (traced 2026-09-27)

Full trace of `ConstantsManager.cs`'s `BudgetReviewConstants` loading,
`GameWorld.cs`'s budget-review methods, `Player.cs`'s funds/prestige fields,
`GoalsManager.cs` + `Goal.cs`/`GoalDependencyOrBoost.cs`, `Goals.xml` (the
live install's copy — same "not in `extracted\`" gotcha §10 hit), and
`PAOMainOverlay.cs`. This upgrades §7.1's manual-only claims (prestige→funds,
budget-review cadence, generic/non-generic goals, the goal-skip reliability
penalty) to **code-confirmed** throughout, and answers all four as real,
specific mechanisms rather than paraphrasing the manual's plain-English
version.

### 11.1 Prestige → funds: a tiered lookup with a catch-up adjustment, not a simple conversion

**Code-confirmed** (`GameWorld.ComputeNextBudgetForPlayer`/
`RetrievePrestigePointsAndBudgetsGrantedForBudgetReviewNumber`,
`ConstantsManager`'s `BudgetReviewConstants` list). Three things worth
knowing, none of them guessable from the manual's one-line summary:

- **A budget review isn't one formula — it's a sequence of brackets, one per
  review.** `Constants-<Agency>.xml`'s `BudgetReview/BudgetReviewConstants`
  is a list of `(MinPrestigePoints, MinBudget, MaxPrestigePoints, MaxBudget)`
  tuples, one entry per successive review (review 1, 2, 3, ...), each
  scaled by its own difficulty multiplier (Normal/`Value1` only, per
  `design.md` §2.10). Later reviews demand more cumulative prestige for the
  same or better funding — the bar rises every cycle, it doesn't stay fixed.
  **The `<Agency>` in that filename turns out not to matter in practice —
  §13.4 confirms `Constants-NASA.xml`/`Constants-Soviet.xml`/
  `Constants-GSA.xml` are textually identical, so every number in this
  subsection is the same regardless of which agency is playing.**
- **The formula itself is linear interpolation within the current review's
  bracket, floored/capped at its ends:** at or below `MinPrestigePoints`,
  the player gets exactly `MinBudget`; at or above `MaxPrestigePoints`,
  exactly `MaxBudget`; in between, linear interpolation. **Crucially,
  `PrestigePoints` here is the player's all-time cumulative total, never
  reset** (`Player._prestigePoints` only changes via `AddPrestigePoints`/
  `SubstractPrestigePoints` on mission outcomes and goal achievements,
  confirmed by a codebase-wide search — nothing else touches it, and it's
  zeroed only once, at game start). So each successive review compares your
  *entire game's* prestige history against an ever-higher bar, not "prestige
  earned since the last review."
- **A genuine catch-up mechanic, not documented anywhere in the manual:**
  if the player's prestige *at the time of the previous review*
  (`Player.PrestigePointsInLastBudgetReview`, a snapshot taken each review)
  fell short of *that previous review's own* minimum threshold, the
  **current** review's min/max thresholds both get shifted down by exactly
  that shortfall. Underperforming one review doesn't compound against you
  forever — the next review's bar recalibrates relative to where you
  actually ended up, not where the un-adjusted schedule originally expected
  you to be.
- **Funding is a recurring per-season grant, not a lump sum.** A review
  doesn't hand the player a pile of cash — it sets `_currentBudgetPerSeason`
  (`GameWorld.cs`), and every season thereafter `ComputeMoneyGrantedToPlayer`
  simply returns that same value again, added to funds once per season
  (`GameWorld.AdvanceSeason...` → `player.AddToFunds(...)`), until the next
  review recalculates it. Before the first review, this starts at
  `Constants-<Agency>.xml`'s flat `InitialBudgetPerSeason`.
- **This is genuinely the *only* recurring income source, confirmed by a
  codebase-wide search for every funds-modifying call site**: the sole
  addition is the per-season budget grant above; every other call
  (`Player.SubstractFromFunds`) is an expense — fixed costs each season,
  Tiger Teams hiring (§9.5), random-event unexpected costs, mission/
  construction spending. **Missions and goals never grant cash directly,
  only prestige** — prestige is the only lever, and it only becomes money at
  a review boundary, via the formula above.
- **None of this is hidden from the player.** `PAOMainOverlay.cs` (§11.4)
  displays the current review bracket's exact min/max prestige-to-budget
  mapping and a live-computed *projected* next-review budget based on
  current prestige, every time the screen is opened — the formula above is
  fully transparent in-game, not a black box the player has to infer.

### 11.2 Budget review cadence: every 16th season from game start, confirmed exact

**Code-confirmed** (`GameWorld.IsSeasonForBudgetComputation`/
`ComputeNumberOfSeasonsUntilNextBudgetReview`): a review triggers exactly
when `NumberOfSeasonsElapsed % 16 == 0` — season 16, 32, 48, etc., counted
from game start (not from the last review, though since the interval is
fixed at 16 the two are equivalent here). Confirms the manual's "every 4
years" claim exactly (`baspm.md` §7.1).

### 11.3 Goals: generic vs. non-generic is real and mechanically consequential; the goal-skip reliability penalty, exact mechanism

**Code-confirmed**, `Goal.cs`/`GoalsManager.cs`. `Goals.xml` (the live
install's copy, not present in `extracted\`) defines **207 goals: 34
generic, 173 non-generic** — generic goals are the minority, universal
race-defining milestones; the bulk of content is agency-specific flavor
goals. `IsGeneric` has **two real, separate mechanical consequences**, not
just a display label:
- **Program-opening gates only ever check generic goals**
  (`ProgramsManager.CanOpenProgram` is passed
  `player.GetAllGenericGoalsAchievedOrCancelledIDs()` exclusively) — a
  program's `RequiredGoal` prerequisite (§10.1) can only ever reference a
  generic goal, never a non-generic one.
- **The opposing-faction comparison screen (`baspm.md` §5's PBEM/CampaignRTTM
  precedent) only ever compares generic goals** (`GameWorld.
  GetAllGenericGoalsAchievedInfoByOpposingFaction`) — non-generic,
  agency-specific goals are structurally excluded from cross-agency
  comparison, which makes sense (the other agency literally can't achieve a
  goal declared specific to yours).
- **`GoalsCancelled` confirmed as real recursive obsoletion**
  (`MissionsConfigurationsManager.ComputeAndAddAchievedGoalsToPlayerInner`):
  achieving a goal walks its `GoalsCancelledIDs` list and recursively marks
  each not-yet-achieved one as cancelled too (guarded against reprocessing
  the same goal twice), confirming "Duration III ⇒ II ⇒ I" is a real cascade,
  not just documentation shorthand.
- **The goal-skip reliability penalty, exact mechanism found** — this was
  the one specific manual-only claim going into this pass with no idea where
  it lived in code. **It's neither a mission-sim-formula special case nor a
  program-opening gate — it's a third, distinct, per-mission-attempt
  reliability modifier**, separate from Reliability Transfer (§10.3, a
  one-time bonus at program-opening) and post-mission reliability updates
  (§10.4, permanent, accrues from use). A `MissionConfiguration` declares its
  own `GoalDependencies` (goal ID + specific mission-component IDs + a
  penalty amount each) and `GoalBoosts` (goal ID + mission-component IDs + a
  bonus amount each). At mission-attempt time,
  `MissionsConfigurationsManager.GetPlayerMissionComponentTotalReliabilityModifierForMsnCnfg`
  computes, per component: **subtract** the penalty for every declared
  dependency goal the player hasn't achieved/cancelled yet, **add** the bonus
  for every declared boost goal the player already has achieved/cancelled.
  This modifier is computed fresh for every mission attempt (not persisted
  onto the component) and feeds directly into §8.2's mission-sim base
  probability as `max(0, CurrReliability + modifier)` — confirmed by tracing
  the call chain straight into `MissionSimBaseScreen.cs:918`. So "skipping a
  prerequisite goal" very concretely means: flying a mission configuration
  without having first achieved the specific goal it declares a dependency
  on penalizes exactly the components that goal is tied to, for that
  attempt only — a real, mechanical risk/reward lever exactly as the manual
  claimed, now with the precise formula behind it. **§8.2 updated with a
  cross-reference to this finding.**

### 11.4 The Public Affairs Office screen: full financial/prestige transparency, and where the multiplayer comparison button actually lives

**Code-confirmed**, `PAOMainOverlay.cs` (manual p.40). Three panels:
- **Top:** current per-season budget, seasons until next review, the
  *current review bracket's* min-prestige/min-budget and max-prestige/
  max-budget pair (both ends shown plainly, per §11.1's transparency note),
  and a live-computed *projected* next-review budget from current prestige.
  A **"Goals Comparison" button lives on this same screen**, gated to
  `IsMultiplayerGameMode || IsCampaignRTTMGameMode` — confirms and locates
  exactly where `baspm.md` §5's opposing-faction comparison precedent
  actually surfaces to the player: bundled into the budget/prestige overview,
  not a separate dedicated screen.
- **Center:** current funds, an itemized expenditure breakdown (the three
  personnel groups' combined salaries separately, facilities upkeep,
  program fixed costs, mission-component fixed costs, a total), the current
  per-season budget again, and a computed quarterly balance (income minus
  total expenditure), color-coded green/red.
- **Bottom:** current (cumulative, all-time) prestige points, displayed
  plainly.
- **Relevant for Launch Window's own UI (`design.md` §2.1(b)):** nothing
  about BASPM's resource loop is opaque to the player — every number that
  drives the next review's funding is shown in advance, every season's
  spending is itemized by category. A good concrete precedent for an
  async-multiplayer "why did my budget change" screen, where transparency
  matters even more since the player wasn't present when their turn
  actually resolved.

### 11.5 Async-multiplayer relevance: entirely between-turn, no mid-resolution surprises

**No new mid-turn/mid-resolution decision point found** — the budget review
computation, per-season income/expense application, and goal
achievement/cancellation are all deterministic outcomes of state that
already exists once a turn's missions/personnel/programs are resolved; none
of it requires a live player choice the way Tiger Teams did (§9.5). This
system slots cleanly into "server resolves mechanically, no pause," same as
§10.6's finding for programs/mission-configs/reliability.

### 11.6 Verification still needed (in-game / screenshots)

1. **A screenshot of the PAO screen itself** (§11.4), ideally including the
   Goals Comparison button in a multiplayer/CampaignRTTM game — confirms the
   exact layout/wording for calibrating Launch Window's own budget/status
   screen and any-comparison-with-rival feature.
2. **Confirm in-game that the catch-up adjustment (§11.1) is real and
   visible** — deliberately underperform one budget-review cycle (end it
   below that cycle's minimum prestige threshold), then check whether the
   *next* cycle's displayed min/max prestige thresholds on the PAO screen
   are visibly lower than the un-adjusted schedule would predict. Low
   priority (doesn't change Launch Window's own design either way, we're
   re-deriving formulas per `design.md` §1.2) but would confirm the read of
   the code is right.
3. **Low priority: a save-game or in-game check of a goal-skip penalty in
   effect** (§11.3) — fly a mission configuration that declares a
   `GoalDependencies` entry for a goal the player hasn't achieved, and
   compare that component's displayed/effective reliability for the attempt
   against its plain `CurrReliability` shown elsewhere in the UI. Would
   directly confirm the penalty is player-visible (as opposed to a silent
   under-the-hood odds adjustment), which matters for whether Launch Window
   should surface its own equivalent modifier explicitly in its UI.

## 12. Random events and campaign objectives (traced 2026-09-27)

Full trace of `RandomEventsManager.cs`, `RandomAndDecisionEventBase.cs` +
`DecisionEventBase.cs`, a representative sample of `REB*`/`REG*`/`DE*`
subclasses, `Player.cs`'s event-chance fields, `CampaignManager.cs` +
`CampaignObjective.cs`/`CampaignObjectiveField.cs` + all seven `CampaignOF*`
subclasses, `GameWorld.cs`'s objective-processing methods, and
`GSACampaignObjectives.xml` (the live install's copy — same "not in
`extracted\`" gotcha §10/§11 both hit). This is the last item in §6's
suggested order and **resolves this project's oldest standing open
question** (present since the very first session): whether any `REB*`
decision event ever presents the player a real mid-turn choice.

### 12.1 Random events: two independent per-season dice rolls, entirely hardcoded (no Data Scripts XML at all)

**Code-confirmed, a genuine architectural outlier worth flagging plainly:**
unlike literally every other system traced so far (mission-sim, personnel,
programs, budget/goals — all Data-Scripts-XML-driven), **random events have
no XML backing whatsoever.** `RandomEventsManager.Initialize` hardcodes two
lists of event objects directly in C# — 14 "bad" events (`REB*`) and 5 "good"
events (`REG*`, a sibling family this file's earlier prefix table missed
entirely until now) — and each event class hardcodes its own effect
magnitude as C# fields/literals (e.g. `REBReliabilitySetback`'s
`MinReliabilitySetback = 7f`/`MaxReliabilitySetback = 15f`), not anything
loaded from a Data Script.

- **Trigger chances, code-confirmed** (`Player.cs`'s reset/new-game method):
  a flat **5% chance of a bad event, 20% chance of a good event, every
  single season**, independent rolls (both can fire the same season, or
  neither). These are hardcoded literals too — despite `Player.cs` having
  full XML save/load plumbing for these three chance fields (suggesting a
  design intent to make them tunable/persistent), nothing anywhere in the
  codebase ever sets them to anything other than these three hardcoded
  values at new-game start.
- **Picking an event, code-confirmed**
  (`RandomEventsManager.ComputeChancesAndApplyRandomOrDecisionEvent`): on a
  successful roll, shuffle the relevant pool and apply the first event whose
  `AppliesToCurrentSituation(gameWorld, spmRandom)` gate passes — **most
  events are situationally gated to a real precondition, not pure noise**.
  Concrete examples from the sample read: `REBReliabilitySetback` only
  applies if the player has components actively in R&D; `REBPerishedAstronaut`
  only if there's an available astronaut to kill; `REBMoraleDrop` only
  after a mission failure in the last season; `REGExtraFunding` only after a
  recent run of mission successes *and* only with enough seasons left before
  the next budget review to matter. **This is a real, deliberate design
  choice worth carrying forward**: bad news concentrates around actual
  recent risk factors (a program that's already struggling gets hit harder),
  rather than random events feeling like disconnected noise layered on top
  of an otherwise-stable game. Flagged into `design.md` §1.1 below.
- **Effect ranges, from the sample read**: costs/setbacks are proportional,
  not flat (`REBUnexpectedCost` deducts 7–15% of *current* funds; reliability
  setbacks are 7–15 percentage points off 1–N components in R&D; morale
  drops are 10–20 points off one randomly chosen personnel group; a
  perished astronaut is a specific named employee, permanently). Good events
  are comparably proportional-and-temporary (`REGExtraFunding` bumps the
  per-season budget rate 7–16%; `REGRDResultsBoost` multiplies R&D output
  1.15–1.25× for a random 1–7 seasons).
- **Resolution, code-confirmed** (`ETInterseasonOverlay.
  GetRandomEventBadNewsItemText`/`GetRandomEventGoodNewsItemText`): both
  rolls happen **as part of generating the end-of-season news screen's
  text**, applying their effect immediately and returning a plain narrated
  message — exactly the same "roll happens, effect applies, player reads
  about it afterward" shape as everything else on that screen. Nothing
  pauses for this.

### 12.2 The "decision event" question, definitively resolved: none exist in the shipped game

**This is the answer to the open question flagged since this project's very
first session.** `RandomAndDecisionEventBase`'s own contract
(`AppliesToCurrentSituation`/`Apply(..., out string outputMessage)`) has no
choice-presenting capability at all — it's identical in shape to a plain
random event. A *separate*, more capable base class does exist,
`DecisionEventBase : RandomAndDecisionEventBase`, adding `Option1Text`/
`Option2Text` fields and abstract `Option1Selected`/`Option2Selected`
methods — real, functional choice-presentation machinery, the same general
shape as Tiger Teams' pause-and-choose (§9.5).

**But it was never finished, and it's entirely unreachable in play, for
three independent reasons, any one of which alone would be sufficient:**
1. **Only one class in the entire codebase extends `DecisionEventBase`**:
   `DEAvoidPrestigeDropAtACost` (so "DE" = "Decision Event," confirming the
   earlier guess). Its `AppliesToCurrentSituation` override
   **unconditionally returns `false`** — it can never be selected by the
   picking loop in §12.1, regardless of situation or luck.
2. **Every method on that one class is an empty stub** — `Apply`,
   `Option1Selected`, and `Option2Selected` all have blank bodies. Even if
   reason 1 didn't block it, selecting either option would do nothing.
3. **The manager method that would even roll for a decision event,
   `RandomEventsManager.ComputeChancesAndApplyDecisionEvent`, is never
   called from anywhere in the codebase outside its own definition** —
   confirmed by a codebase-wide search. Not even wired into the
   end-of-season news flow the way the bad/good rolls are (§12.1). Its own
   body is also incomplete: it declares `out string optionA, out string
   optionB` parameters clearly meant to carry the two choices' text back to
   a UI layer, but both are simply set to `string.Empty` right before
   returning, and an `if (num >= 0) { }` block sits empty where option-text
   population would presumably go.
4. (Not load-bearing given 1–3, but consistent with the whole thing being
   abandoned mid-implementation): `_chancesForTriggeringDecisionEvents`
   defaults to `1f` — a 100% roll chance — which would be a strange design
   choice for a real feature and reads much more like a placeholder value
   nobody ever came back to tune.

**Conclusion: BASPM's shipped game contains zero interactive random/decision
events.** Every `REB*`/`REG*` event, and the sole (non-functional) `DE*`
scaffold, confirms the same thing §8/§9/§10/§11 already found for every
*other* system in this codebase — turn resolution is a fully automatic
dice-and-consequence process the player reads about afterward, never one
that pauses for a choice mid-flow, **with Tiger Teams (§9.5) remaining the
sole genuine exception across this entire analysis pass.** `baspm.md` §7.2
and `design.md` §4 updated to close this out.

### 12.3 Campaign objectives: a reusable strategy-pattern scoring shape (GSA's own content out of scope)

**Code-confirmed**, `CampaignManager.cs`/`CampaignObjective.cs`/
`CampaignObjectiveField.cs` + the seven `CampaignOF*` subclasses. This is
`CampaignGSA`-mode-only content (single-player, `baspm.md` §2.4's existing
GSA note) — traced here for its reusable *system shape*, not to catalog
GSA's specific scripted story content, consistent with `design.md` §1.2.

- **One long-term objective, a real win/loss condition, code-confirmed**:
  `CampaignManager.GetLongTermGSACampaignObjectives` hardcodes exactly one —
  achieve the generic goal `MANNED_LUNAR_LANDING` by **Q4 1974** (corrected
  from an earlier draft's "1973" guess, §2.4 above). `GameWorld.
  ProcessExpiringLongTermCampaignObjectives` checks it once, at its
  deadline, and sets one of two player-state flags depending on the
  outcome — `WorldMapBaseScreenGameLogic.cs` reads those flags to show a
  genuine **Victory** or **Game Over** overlay. This is a real, hard
  win/loss condition, not an open-ended score — the GSA campaign definitely
  ends, one way or the other.
  - **Bonus finding, not this pass's main target but directly adjacent**:
    `CampaignRTTM` (the single-player AI-rival mode, `baspm.md` §2.4) uses
    this *exact same* generic goal as its own win condition — first side
    (player or the simulated AI rival) to achieve `MANNED_LUNAR_LANDING`
    triggers the same Victory/Defeat overlay pair. **Real, concrete
    precedent for a race-to-a-specific-milestone head-to-head win
    condition** — directly relevant to `design.md` §2.9's still-open
    "does Launch Window even have a win condition, or is it open-ended
    score comparison" question, flagged there.
- **Short-term objectives, code-confirmed**: drawn 2 at a time (3 on
  BuzzHard — irrelevant, Normal-only per `design.md` §2.10) from a larger
  authored pool, specifically at each budget-review boundary
  (`GameWorld.AdvanceSeason...` → `AddShortTermCampaignObjectives`, gated
  `IsSeasonForBudgetComputation`), each sharing that review period's
  deadline. `GSACampaignObjectives.xml` (41 candidate objectives across 6
  budget-review periods, only 2 ever drawn per period) confirms escalating
  stakes across periods (period 1: 300 prestige granted / 400 deducted;
  period 2: 500/750; rising further in later periods, not fully
  characterized here). Each is independently checked at its own deadline
  (`ProcessExpiringShortTermCampaignObjectives`) and grants
  `PrestigePointsGrantedOnCompletion` on success or deducts
  `PrestigePointsDeductedOnFailure` on failure — always one or the other,
  never a silent expiry.
- **Seven objective-field types, all simple independent state-threshold
  predicates** — confirms and completes `baspm.md` §2.3's existing
  by-filename-only list:
  - `AchieveSetOfGoals` — one or more specific goal IDs achieved.
  - `AgencyFunds` — current funds ≥ a target.
  - `EmployeesSkillset` — at least N employees of a given group with a given
    skill ≥ a target competency.
  - `MsnCmpRel` / `MsnCmpRelSETSkill` — at least N mission components (of a
    given SET skill group) at ≥ a target reliability.
  - `PersonnelCount` — a personnel group's headcount ≥ a target.
  - `SuccessfulUsesCount` — one specific mission component successfully used
    ≥ N times.
  - Every one of these is a clean, generic, re-derivable predicate type
    (funds/headcount/skill/reliability/goal-set/usage-count thresholds) —
    **exactly the reusable "objective system shape" this file's earlier note
    already flagged as worth reusing**, now fully confirmed rather than
    guessed at from class names alone.
- **Async-multiplayer relevance: same clean shape as everything else traced
  so far** — objective checking is a deterministic state comparison run at a
  fixed deadline/budget-review boundary, no mid-resolution surprises, no
  player input needed once an objective's terms are set. See `design.md`
  §2.8(b).

### 12.4 Verification still needed (in-game / screenshots)

1. **Low priority, cosmetic only:** a screenshot of a `REB*`/`REG*` news
   item as it actually renders on the interseason news screen — confirms
   tone/format for calibrating Launch Window's own equivalent, though the
   mechanic itself needed no in-game verification (fully resolved by static
   analysis, §12.1/§12.2).
2. **Low priority:** a screenshot of the GSA campaign's Victory/Game Over
   overlay (§12.3) — confirms presentation only, the win/loss mechanism
   itself is already code-confirmed.

## 13. Per-agency differences: what actually differs between NASA/Soviet/GSA (traced 2026-09-27)

Direct answer to a question raised during design discussion: BASPM's data is
split into per-agency files everywhere (`Constants-<Agency>.xml`,
`AstronautCandidates-<Agency>.xml`, `MissionControlCandidates-<Agency>.xml`,
`SETCandidates-<Agency>.xml`, `ReliabilityTransfers-<Agency>.xml`, plus
`Programs.xml`/`MissionComponents.xml`'s per-entry `SpaceAgency` attribute),
but nothing in this file had actually compared their *contents* before now.
**Code-confirmed** throughout, diffing the live install's actual Data
Scripts files (`SPM-Windows_Data\Scripts\Data Scripts\General Container\` —
same "not in `extracted\`" gotcha as `Programs.xml`/`Goals.xml`, §10/§11)
directly against each other and against the source.

### 13.1 Program/component access — the one real, structurally-enforced difference, confirms the "can't launch Sputnik" guess exactly

`Programs.xml`'s 55 programs split `SpaceAgency='NASA'` (32) /
`SpaceAgency='Soviet'` (23) — no shared/neutral program exists at all.
`MissionComponents.xml`'s 85 components split similarly (50 NASA / 35
Soviet). **The entire gating mechanism is one shared method,
`CoreManager.ElementPassesSpaceAgencyTest`** (used identically by both
`ProgramsManager` and `MissionComponentsManager`, the only two callers in
the whole codebase):

```
return playerSpaceAgency == SpaceAgencyT.GSA || playerSpaceAgency == elementSpaceAgency;
```

I.e.: a NASA player only ever sees NASA-tagged content, a Soviet player only
ever sees Soviet-tagged content — **and a GSA player sees literally
everything**, since the test always passes for GSA regardless of the
element's own tag. GSA isn't a third separate content track; it's "no
filter applied," a genuine super-agency exactly matching the manual's own
framing (`baspm.md` §2.4, "a utopic international space agency that unites
all the world's space programs").

### 13.2 Reliability Transfer: GSA gets a real synergy bonus, not just wider access

`ReliabilityTransfers-Soviet.xml` (416 lines) encodes the real Soviet R-7
family (Sputnik/Vostok/Voskhod/Molniya/Soyuz launch vehicles, all R-7
derivatives) plus Proton and N1, and even the real-but-never-flown Chelomei
UR-700 proposal — same historically-grounded-lineage approach `baspm.md`
§10.3 already found for NASA's table (Redstone/Atlas/Titan/Saturn), same
generosity range (25–60%, same caps, e.g. both agencies' R-7/Redstone-class
targets cap at `MaxReliabilityTransfer="60"`).

**`ReliabilityTransfers-GSA.xml` is not simply the union of the other two
tables re-filed under one agency — 37 of its 85 targets (44%) declare
*cross-lineage* sources that never appear in either single-agency file.**
Concrete example, `JUPITER-C` (a real NASA vehicle) as a GSA target: its
source list includes `REDSTONE`/`JUNO_II`/`ATLAS` (as expected, matching
NASA's own table) but *also* `R-7_SPUTNIK` (25%), `R-7_VOSTOK` (27.5%),
`R-7_SOYUZ` (32.5%), `PROTON` (25%), `N1` (50%) — Soviet-lineage sources a
NASA-only or Soviet-only player's own table never lists for that target.
**This is a genuine, deliberate GSA-exclusive mechanical edge, not just
"access to more programs"**: a GSA player can draw reliability-transfer
benefit from cross-pollinating American and Soviet hardware experience,
representing real East-West technology sharing that was politically
impossible for either historical agency alone — thematically rich, and
worth calling out as real design intent by BASPM's own developers, not an
incidental side effect of GSA's "sees everything" filter.

### 13.3 Personnel candidate pools: cosmetic (name-flavor) only, zero stat differences

`AstronautCandidates-<Agency>.xml`/`MissionControlCandidates-<Agency>.xml`/
`SETCandidates-<Agency>.xml` differ **only in candidate names** — each
`<Candidate>` entry carries just `GlobalID`/`Name`/`Sex`/`HasOwnPortrait`,
no skill or stat attributes at all. NASA's astronaut pool includes named
historical figures (Buzz Aldrin appears once, alongside generic American
names); Soviet's includes Yuri Gagarin alongside Cyrillic-flavored
transliterated names; GSA has its own separate (presumably more
internationally-mixed, not characterized in detail here) name pool. **Every
actual stat/skill value is generated from the same agency-agnostic
quality-distribution tables** already documented (`PersonnelManagerConstants.xml`,
`baspm.md` §9.4) — personnel generation itself has no mechanical agency
dependency whatsoever, only which name gets drawn.

### 13.4 Economic constants: a per-agency lever that exists in the data model but the shipped game never uses

**`Constants-NASA.xml`, `Constants-Soviet.xml`, and `Constants-GSA.xml` are
byte-for-byte textually identical** (confirmed via direct `diff`, zero
output either way) — same `PlayerInitialFunds` (25000), same
`PlayerInitialPrestigePoints` (250), same `InitialBudgetPerSeason` (3000),
and all ten of `baspm.md` §11.1's budget-review brackets, digit for digit.
The per-agency file split clearly exists to *support* independent economic
tuning per agency, but the base game's actual authored content simply never
diverges — NASA/Soviet/GSA are 100% economically identical in the shipped
game, an unused lever exactly like `SpaceAgencyT.Common` below.

### 13.4a Per-mission prestige rewards: a real, partial compensation — corrected after an initial wrong read

**Follow-up question, answered from the data (2026-10-01, corrected same
day after an in-game screenshot caught the error):** does BASPM compensate
Soviet's smaller program roster (§13.1: 23 vs NASA's 32) with richer
prestige rewards per mission?

**First pass was wrong — the decay mechanic is real, not unused.** It
initially looked that way because of checking the wrong file: `Mission
Simulations Container\MsnSim*.xml` (249 files) stores each mission
*step*'s prestige as a **percentage share** of a total
(`MissionStep.SetPrestigeGrantedOnSuccessAsPercentage`,
`percentage × totalMissionPrestige ÷ 100`) — naturally identical across
`FirstTime`/`SecondTime`/`LaterTime` on that file, since it's the same
share either way. The actual per-repeat totals live one level up, in
`General Container\MsnCnfg<PgmCtgID>-<PgmID>.xml` (one file per program,
one `<MsnCnfg>` per mission configuration,
`MissionsConfigurationsManager` lines 76-78) — and there, the decay is
real: **`SecondTime` is exactly 50% of `FirstTime`, and `LaterTime` is
exactly 30% of `FirstTime`, in 186 of 189 mission configurations in the
game** (the remaining 3 differ by a rounding fraction) — e.g. Luna 3
(lunar flyby): 585/292/175, screenshotted in-game and matching to three
decimal places. A hardcoded design rule applied uniformly, not
per-mission tuning.

**Redone with the correct source, per-program prestige (best config's
`FirstTime`, cross-referenced against `Programs.xml`'s `SpaceAgency`/
`OpeningCost`):**

- **Soviet programs are genuinely worth more prestige individually** —
  average `FirstTime` prestige 2,002 vs NASA's 1,930 per program, and
  Soviet is more cost-efficient too (1.84 prestige per funds spent on
  opening vs NASA's 1.66). A real, deliberate per-program compensation,
  not the "uncompensated" conclusion the first pass reached.
- **It doesn't fully close the gap, though.** Summed across each agency's
  whole roster: NASA's 32 programs total 61,750 `FirstTime` prestige vs
  Soviet's 23 programs at 46,050 — NASA ahead by ~34%, almost exactly
  tracking the 32:23 program-count ratio itself. The steady-state repeat
  rate (`LaterTime`, the relevant one once a game is deep enough that
  completed programs are being re-flown in parallel rather than newly
  opened) tells the same story: NASA 18,525 total vs Soviet 13,815 — NASA
  still ~34% ahead.
- **Not chased further: goals stack an additional, separate prestige
  bonus on top of a `MsnCnfg`'s own `FirstTime`/`SecondTime`/`LaterTime`**
  (`MissionsConfigurationsManager.GetPrestigeGrantedIncludingGoals` sums
  `GoalsManager.GetPrestigeGrantedNextTimeGoalGetsAchieved` across every
  goal a configuration's `GoalsAchieved` list declares) - not included in
  the totals above, so the real NASA/Soviet gap could be somewhat
  different once goal-prestige is folded in; low priority to chase since
  it doesn't change which direction the imbalance runs.

**So the historically-accurate program-access asymmetry (§13.1) gets a
real but partial per-program compensation** (Soviet programs pay a bit
more, a bit more efficiently) **that doesn't fully offset having 9 fewer
programs** — NASA retains a genuine prestige-throughput advantage of
roughly the same size as its program-count advantage, most visible once
several of its extra programs are running in parallel in the mid/late
game. Directly relevant to `design.md` §2.1's own archetype-balance
question (§13.7 below) — if Launch Window wants NASA/Soviet-equivalent
archetypes to feel balanced, matching BASPM's own *shape* (partial
per-program compensation, not full) is one option, but closing the gap
further would be a deliberate Launch Window choice, not something to
inherit as-is.

### 13.5 `SpaceAgencyT.Common`: confirmed entirely dead

A codebase-wide search found **zero references to the enum value anywhere
outside its own declaration** (`SpaceAgencyT.cs`), and zero XML data
anywhere tags anything `Common`. Not a playable choice, not an internal
marker actually used for anything in the shipped game — a genuinely unused
enum slot, nothing more.

### 13.6 Goals: agency association is indirect, not a direct tag (inferred, not chased further)

`Goals.xml` itself carries no `SpaceAgency` attribute on any goal entry.
The 173 non-generic, agency-flavored goals (`baspm.md` §11.3) most likely
get their agency association *indirectly*, through which agency's
mission-configurations grant them (a Soviet-only program's mission configs
can only ever be flown, and thus only ever grant their goals, by a Soviet
player) — **inferred from how every other agency-specific system in this
codebase works, not independently code-confirmed**; low priority to chase
further since it doesn't change the ranked picture below.

### 13.7 Ranked summary and the design question it answers

In order of actual mechanical significance:
1. **Program/component access (§13.1)** — the one real, structurally
   enforced gate. Confirms the "NASA can't launch Sputnik" guess exactly.
   **Code-confirmed only partially compensated (§13.4a, 2026-10-01)**:
   Soviet programs pay more prestige individually and more efficiently per
   funds spent, but not enough to offset NASA's 9-program advantage — NASA
   retains a standing ~34% prestige-throughput edge, tracking its
   program-count edge almost exactly.
2. **Reliability Transfer cross-pollination for GSA (§13.2)** — a genuine
   GSA-exclusive bonus mechanic, not reducible to "more content."
3. **Everything else is cosmetic or an unused lever**: personnel pools
   differ only in name flavor (§13.3); economic constants (§13.4) are a
   real per-agency tuning capability the shipped game never actually
   exercises (unlike per-mission prestige rewards, §13.4a, which the
   shipped game *does* meaningfully vary, just not by agency); `Common`
   is dead (§13.5); goals are agency-flavored only indirectly, through
   program access (§13.6).

**Directly answers the design question this pass was launched to inform**
(`design.md` §2.1/§3.8, §3.8's non-exclusive-archetype decision): see
`design.md` §2.1 for the resulting decision on whether Launch Window's own
archetypes should carry a mechanical difference or stay flavor-only.
