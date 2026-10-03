# Launch Window — UI screen spec (M5)

The brief for the M5 client mockups. It lists every screen, what each one shows
(mapped to the real game state), what the player can do there, and the states
each screen must handle. The visual language is already decided; don't
re-derive it here. Mockups are made in Claude Design against the installed
**launchwindow** design system. Claude implements them in libGDX.

**Read first:**
- `design.md` §3.12: palette, typography, border and fill rules. Colors carry
  meaning: navy is neutral/primary, gold is in progress, green is good, red is
  destructive or failed (reserved), violet is unused.
- `design.md` §3.13: the main-menu hub, and the header and footer shared by every
  screen.
- `core/src/main/java/de/mkoehler/launchwindow/client/ui/Theme.java`: the code
  twin of the design tokens.
- The existing **Personnel Recruitment** mockup in Claude Design: the reference
  for the chrome and for a roster and candidate table.

**Reference material in this repo:**
- `assets-raw/baspm/ui/*.png`: screenshots of the original game, named per
  screen. Use them for layout ideas and to see what information BASPM showed.
  Never copy their art or text (`design.md` §1.2).
- `assets-raw/main_menu/`: the hub art, NASA and Soviet, 4 seasons × 4 times of
  day, plus `<faction>.hotspots.json` with the seven building polygons.
- `assets-raw/Program Categories/`, `assets-raw/Programs/`,
  `assets-raw/Mission Configuration/`: program and mission art, tracked in
  `assets-raw/ART_CHECKLIST.md`.
- `assets-raw/svg/icon_*.svg`: one icon per skill (all 15).
- `assets/branding/logo.svg`: the mission-patch logo. The same circular frame is
  meant for goal badges, with the gold arc closing solid when a goal is achieved.

**Fixed decisions the mockups must respect:**
- A 1920×1080 canvas with 30 px page padding and a 20 px gap between header,
  content, and footer. The window letterboxes to keep 16:9.
- Light surfaces for anything that is read: tables, cards, and paragraphs sit on
  flat white. The blueprint texture is only for the page frame.
- **There is no "End season" button.** Turns resolve on the server's daily
  schedule. Every action takes effect in the player's preview immediately and is
  final at the next resolution.
- **Undo is by opposite action only** (close, dismiss, scrub). Opposites cost
  what they normally cost: closing refunds nothing. So actions that can't be
  undone cheaply need a confirmation, as listed per screen below.

---

## 1. How the client works (what the mockups must account for)

- **Preview state.** After login the client fetches the player's state with
  everything queued today already applied. Funds have dropped, the program shows
  as open, the hire is on the roster. Every successful action refreshes it.
- **Action results.** Every action returns either success or a refusal with a
  reason code. Each screen needs a place to show both: a toast or an inline
  message near the control. §4 has the reason codes and suggested wording.
- **No live updates.** Nothing changes on screen without a player action, except
  after a turn resolves, when the player comes back to a turn report (§3.3).
- **Two clocks.** The *game date* (for example "1958 · Spring") is shared by
  everyone. A player's *own season count* only drives their budget reviews.
  Headers show the game date.

---

## 2. Shared chrome and components

Detailed in `design.md` §3.13; summarized here with the data each element shows.

**Header (every screen except the hub):** back, home, and settings icon buttons;
a breadcrumb trail (earlier crumbs clickable, the current one in upper case).
On the right:
- a prestige chip (`prestige`);
- a game-date chip ("1958 · S2");
- **new: a next-resolution chip** ("Next turn in 6 h 12 m"). In an async game,
  this is the most useful single fact.

**Footer (every screen except the hub):** five clickable stat chips:

| Chip | Shows | Opens |
|---|---|---|
| Funds | balance and the last season's net change (green or red) | Public Affairs Office |
| SET | hired / busy / cap | SET Centre |
| Astronauts | hired / busy / cap | Astronaut Complex |
| Flight controllers | hired / busy / cap | Mission Control |
| Open programs | open / Headquarters cap | Headquarters |

"Busy" counts anyone not idle: in training, assigned, or on R&D.

**Reusable components** (design these once, then reuse them):
1. **Personnel row.** Name, age, morale (rated with color), salary, state badge
   (Idle, In training · kind · seasons left, On mission · id, Researching ·
   component · slot, Unavailable · n seasons), the group's five skills as icon +
   value, and Expert stars on the skills that have them.
2. **Candidate card or row.** The same fields, plus a Hire button.
3. **Reliability bar.** The component's current reliability, a marker at its R&D
   ceiling (`maxRdReliability`), and the bar's end at its absolute maximum
   (`maxReliability`). Label "62.4 % (R&D cap 85.2)". Optionally a dim floor
   marker.
4. **Price breakdown.** Line items and a total, for example opening cost −
   discount + component creation costs. Also used for launches (configuration
   cost + rocket unit cost) and upgrades.
5. **Confirmation dialog.** Title, consequence text, a primary action in red for
   final or destructive actions, and Cancel.
6. **Action result toast.** Success in green, refusal in navy with a readable
   reason (§4).
7. **Building panel.** Level n of max, the current cap, upkeep per season, and an
   **Upgrade** button showing cost and build time ("Level 2: 13 controllers,
   900 · 1 season · final"). While building: "Upgrading, 1 season left". It
   appears on the Astronaut Complex, Mission Control, SET Centre, Headquarters,
   and VAB screens.
8. **Goal badge.** The mission-patch circle (`logo.svg` frame), open or
   achieved, with a "first" ribbon when the player was first to it.
9. **Today's queue panel** (optional but recommended). A collapsible list of
   today's actions with their results ("Opened Sputnik 1 · −876"). Undo is by
   opposite action only, so this is how a player sees what they've committed to.

---

## 3. Screens

Each screen lists its purpose, the state it shows (real field names in
`code style`), its actions, and the states it must handle. Where a BASPM
screenshot shows the equivalent screen, it's listed as **Ref**.

### 3.1 Login
**Purpose:** connect and authenticate. One form: server (host:port), login,
password, display name. A new login creates the account. **States:** connecting;
wrong password; login has invalid characters (`[A-Za-z0-9_-]`, up to 32); client
and server versions don't match (show both versions); server unreachable.

### 3.2 Claim a slot (first visit to a game only)
**Purpose:** join the game. **Shows:** an archetype choice between NASA-style and
Soviet-style, with the two hub artworks as large cards (several players may pick
the same one); an agency name; a callsign; a short line saying the game is in
progress and you start fresh. **Actions:** Claim. **States:** name taken; callsign
taken (both checked ignoring case); empty fields. **After claiming:** the starting
state is 25,000 funds, 250 prestige, 3,000 per season, every building at level 1,
empty rosters, and six candidates per group. So the first thing to show is a hint
to hire staff (SET Centre first).

### 3.3 Turn report (shown once after each resolution, reopenable from the hub)
**Purpose:** "what happened overnight", the async equivalent of BASPM's
end-of-season news screen. **Ref:** `bulletin_news_*.png`,
`end_of_season_rnd_results.png`, `mission_assessment.png`.

**Shows, grouped:**
- missions flown this season, with step outcomes (success, glitch solved, minor
  glitch, failure, catastrophic), prestige gained or lost, and goals achieved
  ("first in the game!");
- R&D gains per component, and components that hit their R&D cap (researchers
  released);
- training finished (skill gain), Expert trait earned;
- departures (retired, resigned, dismissed, perished, lost in an accident),
  hires;
- the budget: running costs, funding, and a review if one happened (new rate);
- random events (`newsLog` entries);
- building upgrades finished.

**Actions:** open a mission's replay (§3.7); dismiss.
**Needs server work:** gap G2 (§5).

### 3.4 Space Complex hub (main menu)
**Purpose:** navigation, and the game's front page. **Shows:** full-screen art for
the player's archetype, picked by the game season, with the time of day advancing
every 4 real minutes on re-entry (`design.md` §3.13). The seven building hotspots
highlight on hover with a label. A slim overlay strip shows the agency name and
callsign, the game date, the next-resolution countdown, a turn-report badge if one
is unread, and the funds and prestige chips. **Proposal (not decided in
`design.md`): no header or footer chrome on this screen**, so the art stays whole.

**Hotspot targets:**

| Hotspot | Screen |
|---|---|
| HEADQUARTERS | §3.5 Headquarters |
| VAB | §3.6 VAB |
| MISSION_CONTROL | §3.7 Mission Control |
| ASTRONAUT_COMPLEX | §3.8 Astronaut Complex |
| SET_CENTRE | §3.9 SET Centre |
| PUBLIC_AFFAIRS_OFFICE | §3.10 Public Affairs Office |
| MUSEUM | §3.11 Museum |

**States:** a building that is upgrading shows a subtle "under construction"
marker; a game result exists (§3.12).

### 3.5 Headquarters: programs and rocket programs
**Purpose:** decide what the agency works on. **Ref:** `headquarters*.png`,
`program_categories.png`, `program_category_details.png`, `program_details.png`,
`rocket_program_selection.png`, `goals_open_up_new_programs.png`.

**Tabs:** Payload programs | Rocket programs | (building panel).

**Payload programs:** categories (category art), then the programs in a category
(program art), then a program detail panel showing:
- state: open, closed (components kept, inactive), available, or locked;
- the price breakdown: opening cost, discount from other open programs, and
  creation cost for components not owned yet (a component still owned from a
  closed program is free);
- upkeep per season;
- the required goals (locked until achieved or cancelled), with goal badges;
- the components it grants, each with its reliability bar if owned;
- its mission configurations (art, duration in seasons, first-time prestige,
  crew, controller, and CAPCOM counts, launch types);
- "only your agency": other agencies' programs are hidden entirely, not shown
  locked.

**Actions:** Open (shows the price; refused if Headquarters is full, the goals
aren't met, or funds are short). Close, which needs a confirmation: "no refund;
its components go inactive and their researchers are released; refused while one
of its missions flies".

**Rocket programs:** the agency's rockets, each with:
- class (Light, Medium, Heavy) and payload capacity to suborbital, LEO, TLI, and
  TPI;
- human-rated or not;
- creation cost, unit cost per flight, upkeep;
- reliability bar.

**Actions:** Open, refused if the VAB is full or the VAB level is too low for the
class. Close, which needs a confirmation: "reopening costs the creation cost
again".

**Building panel:** Headquarters' cap is the number of open payload programs
(3/7/10/16).

### 3.6 VAB: assemble, launch, scrub
**Purpose:** fly missions. **Ref:** `vab_building.png`,
`vab_with_in_progress_mission.png`, `assemble_mission*.png`,
`mission_selection.png`, `schedule_mission.png`.

**Left:** the mission configurations of open programs, grouped by program, each
with art, duration, prestige (first time, or the repeat tier: 50 % second time,
30 % after that), and a readiness summary.

**Assembly panel** (for the selected configuration):
- **Rockets:** one picker per launch. It lists the owned, active rockets that can
  lift that launch's payload mass to its launch type (human-rated rockets only
  for crewed launches). Each entry shows reliability, unit cost, and spare
  capacity. Ineligible rockets show greyed out with the reason.
- **Crew:** exactly `requiredAstronautCount`, chosen from idle, available
  astronauts, showing the skills the steps use.
- **CAPCOM:** exactly `requiredCapcomCount` more astronauts, on the ground, not
  crew. Leadership matters here.
- **Flight controllers:** exactly `requiredMissionControlOfficerCount`. Crewed
  Mercury and Vostok need 10, so a level-1 Mission Control (cap 5) must be upgraded
  first. Say so plainly when the cap is too low.
- **Readiness:** each flown component's reliability bar, with goal-dependency
  penalties ("−15 until Mercury uncrewed suborbital is flown") and boosts.
- **Price breakdown:** configuration cost + the rockets' unit costs.
- **Launch** (gold or navy). One confirmation: "launches at the next resolution;
  scrubbing before then refunds in full".

**In-flight list:** missions with their id, configuration, season n of m, rockets,
crew, CAPCOM, and controllers. Actions:
- **Scrub** (red), for a mission launched today: full refund, everyone freed;
- **Cancel** (red), for an uncrewed mission in flight: shows the prestige loss and
  the percentage refunded;
- crewed missions in flight show "Can't be abandoned", with no button.

**States:** the launch cap is reached (random event); a component is unavailable
this window (random event); short of staff; short of funds.

**Building panel:** the VAB's cap is the number of open rocket programs (4/8/14),
and its level sets the rocket class (Light, then Medium, then Heavy).

### 3.7 Mission Control: department and mission replay
**Purpose:** the flight-controller roster, and watching missions. **Ref:**
`mission_control_*.png`, `watch_mission_from_mc_or_skip.png`.

**Department tab:** the controllers' personnel rows. Actions: Hire (opens the
candidate list for this group), Dismiss (confirmation; refused while on a
mission), Train (§3.8's training dialog), Expert training (a skill at 80 or more,
open-ended), Pull out of training (partial credit; confirmation).

**Missions tab:** the in-flight missions (as in §3.6, read-only here), with the
current season's steps.

**Replay viewer:** reached from the turn report or a mission. It plays a resolved
season's steps one by one:
- the step name;
- the involved components and their reliability;
- the success chance;
- the outcome, and a saving throw if one happened ("glitch, saved by the
  controllers"), plus any Expert rescue with the Expert's name;
- the prestige running total.

Then the season summary: goals achieved, reliability changes, skill growth, and
crew losses. Playback should be skippable and steppable. **Needs server work:** G2.

**Building panel:** controller cap 5/13/22/35.

### 3.8 Astronaut Complex
**Purpose:** the astronaut roster and recruiting. **Ref:**
`astronaut_centre_*.png`, `hire_recruits.png`, `manage_department.png`.

**Shows:** astronaut personnel rows; who is on a mission, including CAPCOM; Expert
stars; the candidate pool (it refreshes every season). Note that new hires go
straight into basic training on a random skill (3 seasons for astronauts).

**Actions:**
- Hire;
- Dismiss (not while on a mission);
- **Train dialog:** Basic, Advanced (2 seasons), or Permanent (advanced rounds
  until a target level); pick a skill with the skill icons; set the target level
  for Permanent. **Ref:** `SET_centre_advanced_training_assignment*.png`;
- Expert training;
- Pull out.

**Building panel:** astronaut cap 7/14/24/35.

### 3.9 SET Centre: engineers and R&D
**Purpose:** the engineering roster, and R&D on components. **Ref:**
`SET_centre*.png`, `rnd_assignments.png`, `rocket_program_rnd_assignment.png`,
`upgrade_SET_centre.png`. Recruitment uses the existing mockup.

**Shows:** SET personnel rows (state "Researching · component · slot"), and an
**R&D board**. The board has one card per owned, active component, with:
- its reliability bar;
- its four R&D slots: slot 1 earns the most, slot 4 the least, and each slot shows
  the specialist in it or empty;
- the component's skill icon (which SET skill helps).

**Actions:**
- Assign an idle specialist to a component (they take the lowest free slot;
  refused at the R&D cap or when all four slots are full);
- Release;
- Hire, Dismiss (allowed mid-R&D, frees the slot);
- Train (as in §3.8);
- Pull out.

**States:** components reaching their R&D cap release their researchers
automatically. Show "cap reached" on the card. **Building panel:** SET cap
7/14/20/30.

### 3.10 Public Affairs Office: budget and prestige
**Purpose:** money. **Ref:** `public_affairs_office.png`, `budget_info.png`.

**Shows:**
- funds and the funding rate per season;
- running costs broken down: salaries per group, program upkeep, component upkeep
  (active only), building upkeep per building, times any temporary cost
  multiplier from a random event;
- the net per season;
- prestige, and the next funding review: "in n seasons", counted on the player's
  own seasons, every 16;
- the bracket table, and what the current prestige would earn;
- the news log.

**Actions:** **Convert prestige to funds** (only while in debt, at 4 prestige per
fund, capped so funds end at no more than two seasons of running costs). Show the
cap and the price.

**States:** in debt, where new spending is refused; a temporary cost or R&D
multiplier is active (random event, n seasons left).

### 3.11 Museum: goals, history, rivals
**Purpose:** the scoreboard and the archive. **Ref:** `museum*.png`.

**Tabs:**
- **The race:** the declared goals as goal badges, own progress, and every
  player's progress side by side, by agency name and callsign: which goals they
  hold and the turn they got them. **Needs server work:** G3.
- **Achievements:** every goal achieved or cancelled, with the turn and the
  prestige.
- **Flights:** the completed flight count per configuration (`successfulFlightCounts`).
- **Memorial:** astronauts lost.

**Later (M7+):** the educational encyclopedia (`design.md` §1.1).

### 3.12 Game result
**Purpose:** announce the winner. **Shows:** an overlay on the hub when a result
exists: winner or tied winners (agency and callsign), the turn decided, and the
race table. Play continues, so the overlay can be dismissed and is reopenable from
the Museum.

### 3.13 Settings (header gear)
Log out; server address; sound on or off (later); about and version.

---

## 4. Refusal reasons (for toasts and inline messages)

The server replies with these codes. Suggested wording below; final wording is
for the mockups.

| Code | Wording |
|---|---|
| INSUFFICIENT_FUNDS | Not enough funds. |
| HEADQUARTERS_FULL | Headquarters can't run more programs. Upgrade it or close one. |
| WRONG_AGENCY | That belongs to another agency. |
| GOALS_NOT_MET | Achieve the required goals first. |
| ALREADY_OPEN / NOT_OPEN | Already open. / Not open. |
| MISSIONS_IN_FLIGHT | Can't close while a mission uses it. |
| VAB_FULL | The VAB can't hold more rocket programs. |
| ROCKET_CLASS_TOO_LARGE | Upgrade the VAB to build this rocket class. |
| ROSTER_FULL | No room. Upgrade the building. |
| UNKNOWN_CANDIDATE | That candidate has left the pool. |
| ASSIGNED_TO_MISSION | Can't dismiss someone on a mission. |
| NOT_IDLE / WRONG_STATE | They're busy. |
| AT_RD_CEILING / NO_FREE_SLOT | R&D can't improve this further. / All four R&D slots are taken. |
| COMPONENT_NOT_AVAILABLE | That component isn't owned and active. |
| SKILL_BELOW_THRESHOLD / ALREADY_EXPERT_IN_SKILL | Expert training needs a skill of 80. / Already an Expert. |
| TARGET_NOT_ABOVE_SKILL | Pick a target above the current skill. |
| PROGRAM_NOT_OPEN | Open the program first. |
| WRONG_ROCKET_COUNT / ROCKET_NOT_AVAILABLE / ROCKET_CANNOT_LIFT_PAYLOAD | Pick one rocket per launch. / That rocket isn't available. / That rocket can't lift this payload there (or isn't human-rated). |
| WRONG_ASTRONAUT_COUNT / WRONG_MISSION_CONTROL_OFFICER_COUNT / WRONG_CAPCOM_COUNT | This mission needs exactly n. |
| ASTRONAUT_NOT_IDLE / MISSION_CONTROL_OFFICER_NOT_IDLE / CAPCOM_NOT_AVAILABLE | Someone picked isn't available. |
| LAUNCH_CAP_REACHED | No more launches this window. |
| MISSION_COMPONENT_UNAVAILABLE | A component is grounded this window. |
| CREWED_MISSION_IN_FLIGHT | A crewed mission can't be abandoned. |
| MAX_LEVEL / ALREADY_UPGRADING | Already at the top level. / Already upgrading. |
| NOT_IN_DEBT / EXCEEDS_CAP / INSUFFICIENT_PRESTIGE | Only possible while in debt. / More than the conversion allows. / Not enough prestige. |
| SLOT_CLAIM_REQUIRED | (Client goes to §3.2.) |

---

## 5. Gaps the server must fill for M5

Found while writing this spec. The client can't build these screens from the
current protocol alone. Each is an M5 task.

- **G1. Game info.** The content pack id (so the client loads the same pack), the
  declared goals, the current game turn and the game date (start year and
  season), the next resolution time, and the game result if any. Needs a new
  `GameInfoRequest`/`GameInfoResponse`; the handshake can't change.
- **G2. Turn report and replay data.** Resolution events (`MissionStepResolved`,
  `GoalSettled`, `ResearchCompleted`, `TrainingCompleted`, `PersonnelDeparture`,
  `SeasonFundsSettled`, `BudgetReviewed`, and so on) are computed and then thrown
  away. Persist each player's events per turn and serve them. The replay viewer
  also needs the step's success chance, which isn't in `MissionStepResolved` yet.
- **G3. Rival summaries.** Each player's agency name, callsign, archetype, and the
  declared goals they hold, with turns, for the race tab. This is the
  "per-player summary" `design.md` §3.7 planned for game-meta.
- **G4. Display text.** Content has ids only. The UI needs names and short
  descriptions for categories, programs, components, configurations, steps, and
  goals, written in our own words (never BASPM's, `design.md` §1.2). Add a
  strings file per pack.
- **G5. Personnel names.** Candidates are all called "Candidate". Generate
  plausible names per archetype (our own name lists).
- **G6. Today's queue.** Today's pending actions and their results, for the queue
  panel.
- **G7. Derived numbers the client shouldn't recompute.** The opening price,
  launch cost, eligible rockets per launch, goal modifiers, and running-cost
  breakdown are all rules logic. The client can call the same `core` methods
  locally against the preview state (it ships `core`), so this is a client
  concern, not a protocol one. Noted so nobody duplicates the rules in UI code.

---

## 6. Suggested mockup order

1. **Shared chrome and components (§2):** header, footer, personnel row, candidate
   card, reliability bar, price breakdown, confirmation dialog, toast, building
   panel.
2. **The first playable loop:** Login (§3.1), Claim (§3.2), hub (§3.4), SET Centre
   with R&D (§3.9), Headquarters (§3.5). This lets a player hire, open Sputnik and
   its rocket, and research.
3. **Flying:** VAB (§3.6), Mission Control (§3.7), and the turn report (§3.3).
4. **The rest:** Astronaut Complex (§3.8), Public Affairs Office (§3.10), Museum
   (§3.11), game result (§3.12), settings (§3.13).

Mockups don't need to be pixel-final. Layout, components, and states are what
implementation needs. Flag any new color, component variant, or icon so
`Theme.java` and the UI Asset Sheet stay in sync.
