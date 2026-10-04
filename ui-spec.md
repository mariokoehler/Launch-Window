# Launch Window — UI screen spec (M5)

The brief for the M5 client mockups. It lists every screen, what each one shows
(mapped to the real game state), what the player can do there, and the states
each screen must handle. The visual language is already decided; don't
re-derive it here. Mockups are made in Claude Design against the installed
**launchwindow** design system. Claude implements them in libGDX.

**Revision 2 (2026-10-03):** the screen flow follows the user's reorganization of
BASPM's building layout (§3). The VAB owns rockets, Headquarters owns programs and
mission planning, the SET Centre owns payload R&D, and Mission Control watches
missions. Decisions from that review are marked *(decided)*.

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
- `assets-raw/backgrounds/solar_system.png`: original art for the celestial-body
  picker (§3.6.2).
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
  message near the control. §5 has the reason codes and suggested wording.
- **No live updates.** Nothing changes on screen without a player action, except
  after a turn resolves, when the player comes back to a turn report (§4.4).
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
   value, and Expert stars on the skills that have them. Row actions are listed
   per screen.
2. **Candidate card or row.** The same fields, plus a Hire button.
3. **Reliability bar.** The component's current reliability, a marker at its R&D
   ceiling (`maxRdReliability`), and the bar's end at its absolute maximum
   (`maxReliability`). Label "62.4 % (R&D cap 85.2)". Optionally a dim floor
   marker.
4. **Price breakdown.** Line items and a total, for example opening cost −
   discount + component creation costs. Also used for launches (configuration
   cost + rocket unit cost) and upgrades.
5. **Confirmation dialog.** Title, consequence text, the price if any, a primary
   action in red for final or destructive actions, and Cancel.
6. **Action result toast.** Success in green, refusal in navy with a readable
   reason (§5).
7. **Building panel.** Level n of max, the current cap, upkeep per season, and an
   **Upgrade** button showing cost and build time ("Level 2: 13 controllers,
   900 · 1 season · final"). While building: "Upgrading, 1 season left". It
   appears on the five building screens with a cap: Astronaut Complex, Mission
   Control, SET Centre, Headquarters, and the VAB. Upgrading is final, so it needs
   a confirmation.
8. **Goal badge.** The mission-patch circle (`logo.svg` frame), open or
   achieved, with a "first" ribbon when the player was first to it.
9. **Today's queue panel** (optional but recommended). A collapsible list of
   today's actions with their results ("Opened Sputnik 1 · −876"). Undo is by
   opposite action only, so this is how a player sees what they've committed to.
10. **R&D board** (used twice: Rocket R&D in the VAB, Payload R&D in the SET
    Centre). One card per owned, active component of that kind, with:
    - its reliability bar;
    - its four R&D slots: slot 1 earns the most, slot 4 the least, and each slot
      shows the specialist in it or "empty";
    - the component's skill icon (which SET skill helps).

    Assign an idle SET specialist (they take the lowest free slot) or release one.
    Show "cap reached" when the R&D ceiling is hit; researchers are then released
    automatically.
11. **Personnel overview** (the landing screen of each personnel building,
    *(decided)*, as in BASPM). The roster as personnel rows with row actions;
    filter and sort by state and by skill (skill icons); a **department summary**
    strip (headcount, in training / assigned, near retirement age, average morale,
    total salary per season); and buttons to the sub-screens: **Hire** and
    **Manage department**. **Ref:** `manage_department.png`, `SET_centre.png`,
    `astronaut_centre_*.png`.

    **Row actions:**
    - **Train:** opens the training dialog (component 12);
    - **Expert training:** astronauts and controllers only, when a skill is at 80
      or more; open-ended until the trait is earned;
    - **Pull out of training:** partial credit for fixed-length kinds, nothing for
      Expert; needs a confirmation;
    - **Dismiss:** free, refused for an astronaut or controller on a mission
      (crew, CAPCOM, or controller); SET can be dismissed mid-R&D, which frees the
      slot; needs a confirmation.
12. **Training dialog.** Basic, Advanced (2 seasons), or Permanent (advanced
    rounds until a target level). Pick the skill with the skill icons, and set the
    target level for Permanent. **Ref:**
    `SET_centre_advanced_training_assignment*.png`.
13. **Manage department dialog** *(decided: BASPM's one-off adjustment)*. A slider
    from −50 % to +50 % that changes **every salary in the group once** by that
    percentage. Morale changes immediately: by +p for a raise of p %, by −3×p for
    a cut of p %, clamped to 0–100. The dialog previews the projected department
    salary per season and the projected average morale before OK. **Ref:**
    `manage_department.png`. **Needs rules work:** G8.
14. **Hire screen.** The group's candidate pool (it refreshes every season), as
    candidate rows, sorted and filtered by skill. Hire is refused when the
    building's roster cap is reached. New hires go straight into basic training on
    a random skill (astronauts 3 seasons, SET 1, controllers 3). The existing
    Personnel Recruitment mockup is this screen.
15. **History note** *(decided 2026-10-04; how it looks is open)*. A small icon
    placed next to a label, field, or value. On mouse-over it shows a short card,
    two to four sentences, about the real history or engineering behind that
    thing. Clicking pins the card open until it's dismissed. It's the
    educational layer's in-the-moment form (`design.md` §1.1). Rules:
    - **It's always optional.** Nothing in a history note is ever needed to play.
      Rules help (how a mechanic works) is a different thing and never uses this
      icon.
    - **Its glyph is its own** and must not look like a generic "i" or "?" help
      icon, so players learn that this icon always means "a real-world story".
      *Proposal:* a small open book in navy line art, with the card styled like
      an archive index card (white, thin navy rule, a small "History" kicker).
    - **The text is our own,** written from real-world facts, never from BASPM's
      historical-facts text (`design.md` §1.2).
    - **The join screen has the first one** (§4.2). That's on purpose: it's where
      players meet the icon and learn what it means.

---

## 3. Screen flow *(decided)*

```
Connect ─► Join game (skipped if already in the game) ─► Turn report (if a turn resolved since last visit) ─► Space Complex hub

Space Complex hub
├─ VAB
│   ├─ Open rocket program
│   ├─ Manage rocket programs ─► Rocket R&D
│   └─ (building panel)
├─ SET Centre ─► Personnel overview
│   ├─ Hire SET personnel
│   ├─ Manage department (salary adjustment)
│   ├─ Payload R&D
│   └─ (building panel)
├─ Astronaut Complex ─► Personnel overview
│   ├─ Hire astronauts
│   ├─ Manage department
│   └─ (building panel)
├─ Mission Control ─► Personnel overview
│   ├─ Inspect active missions ─► Mission replay
│   ├─ Inspect scheduled missions (scrub)
│   ├─ Hire flight controllers
│   ├─ Manage department
│   └─ (building panel)
├─ Headquarters
│   ├─ Manage open programs ─► Program details ─┐
│   ├─ Select celestial body ─► Program categories and programs ─► Program details ─┤
│   │                                                                               ▼
│   │   Program details ─► Mission configuration selection
│   │                       ├─ Mission configuration details ─► Mission steps overview
│   │                       └─ Mission configuration
│   │                           ├─ Mission assessment
│   │                           ├─ Rocket assignment
│   │                           ├─ Payload R&D (same screen as the SET Centre's)
│   │                           └─ Assemble mission ─► confirm & pay ─► (Mission Control: scheduled)
│   └─ (building panel)
├─ Public Affairs Office
└─ Museum
```

**Shared screens, several entries.** Program details, mission configuration, and
Payload R&D are each one screen reached from more than one place. Only the
breadcrumb differs.

---

## 4. Screens

Each screen lists its purpose, the state it shows (real field names in
`code style`), its actions, and the states it must handle. Where a BASPM
screenshot shows the equivalent screen, it's listed as **Ref**.

### 4.1 Connect
**Purpose:** connect and authenticate. One form: server (host:port), login,
password. A new login creates the account. There's no display name: other
players see you by your agency and director name, chosen per game (§4.2).
**States:** connecting;
wrong password; login has invalid characters (`[A-Za-z0-9_-]`, up to 32); client
and server versions don't match (show both versions); server unreachable.

### 4.2 Join game (first visit to a game only)
**Purpose:** join the game. **Shows:** an archetype choice between NASA-style and
Soviet-style, with the two hub artworks as large cards (several players may pick
the same one); an agency name; the **director's name** (the player's persona,
the head of the agency, fixed for the whole game); a short line saying the game
is in progress and you start fresh. **Actions:** Join. **States:** agency name
taken; director name taken (both checked ignoring case); empty fields.

**History note on the Director field** (component 15, the first one a player
sees). Suggested text: *"Both agencies here have a Director, but in reality the
two jobs were very different. NASA's Administrator was a public figure,
appointed by the President, confirmed by the Senate, and questioned by Congress.
The man who drove the Soviet program, Sergei Korolev, was a state secret. Until
his death in 1966, the world knew him only as "the Chief Designer"."*

**After joining:** the starting
state is 25,000 funds, 250 prestige, 3,000 per season, every building at level 1,
empty rosters, and six candidates per group. So the first thing to show is a hint
to hire staff (SET Centre first).

**Implemented in libGDX (2026-10-04)** from the Connect and Join Game mockups
(`client.screen.ConnectScreen`/`JoinScreen`). Decisions made while building them,
**all unconfirmed**:
- **No BASPM name in the heading.** The mockup's heading names BASPM's own product
  and a game date the client can't know yet (that needs G1). It's now "Join game" /
  "Found your agency" until G1 brings the date.
- **The hint no longer claims things the client can't know.** "Already in
  progress" can be false during the signup window, and the starting funds come
  from the game's own pack. New text: *"Everyone starts from the same line.
  Whenever you join, your agency starts fresh: every building at level 1, empty
  rosters, and six candidates waiting in each hiring pool. Hire SET specialists
  first. They research the parts every mission flies on."*
- **Text on the blue page is light.** The mockup set the heading, eyebrow and
  footnote in ink and navy directly on the page, which is illegible there. They
  use the on-page colors instead.
- **No archetype picked** shows "Pick an agency style." under the cards when Join
  is pressed (the spec didn't cover this state).
- **Connect's refusals:** version mismatch is a warning notice above the card;
  unreachable and a broken-off exchange are danger notices; any other handshake
  refusal (normally a wrong password) is shown under the password field in the
  server's own words. A bad login or an empty field is caught on the client
  before sending.
- **After joining** there's a stand-in agency screen (name, director, funds,
  Log out) until the hub (§4.3) has a mockup.

### 4.3 Space Complex hub (main menu)
**Purpose:** navigation, and the game's front page. **Shows:** full-screen art for
the player's archetype, picked by the game season, with the time of day advancing
every 4 real minutes on re-entry (`design.md` §3.13). The seven building hotspots
highlight on hover with a label. A slim overlay strip shows:
- the agency name and director name;
- the game date and the next-resolution countdown;
- a turn-report button, badged when unread;
- the funds and prestige chips.

**Decided (user, 2026-10-05):** no header or footer chrome on this screen, so
the art stays whole — the slim overlay strip above is the only chrome it gets.
Confirmed by comparing two real mockups side by side (`design.md` §3.13); the
with-chrome variant looked clearly worse (it letterboxes the hero art) and was
removed outright, not kept as an alternative.

**States:** a building that is upgrading shows a subtle "under construction"
marker; a game result exists (§4.13).

### 4.4 Turn report (automatic after a resolution, reopenable from the hub)
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

**Actions:** open a mission's replay (§4.7.3); continue to the hub.
**Needs server work:** G2.

### 4.5 VAB: rockets
**Purpose:** everything about rockets. The VAB's own cap limits the open rocket
programs (4/8/14), and its level sets the largest rocket class (Light, then
Medium, then Heavy). Both sit on this screen next to its building panel. **Ref:**
`vab_building.png`, `rocket_program_selection.png`.

**4.5.1 Open rocket program.** The agency's rockets that aren't open, each with:
- class, and payload capacity to suborbital, LEO, TLI, and TPI;
- human-rated or not;
- creation cost, unit cost per flight, upkeep.

Open shows the price: the creation cost, charged again on every reopening.
Refused if the VAB is full or its level is too low for the class. Other agencies'
rockets are hidden.

**4.5.2 Manage rocket programs.** The open rockets with reliability bars, which
in-flight missions use them, upkeep, and links to Rocket R&D. **Close** needs a
confirmation: "kept with its reliability, no upkeep; reopening costs the creation
cost again; refused while a mission uses it".

**4.5.3 Rocket R&D** *(decided: split from payload R&D)*. The R&D board
(component 10) for rocket components. **Ref:**
`rocket_program_rnd_assignment.png`.

### 4.6 Headquarters: programs and mission planning
**Purpose:** decide what the agency works on, and plan and launch missions.
Headquarters' cap limits open payload programs (3/7/10/16). **Ref:**
`headquarters*.png`, `program_categories.png`, `program_category_details.png`,
`program_details.png`, `goals_open_up_new_programs.png`, `mission_selection.png`,
`assemble_mission*.png`, `schedule_mission.png`.

**4.6.1 Manage open programs.** A list of open programs, each with upkeep, its
components' reliability, missions in flight, and the next goal it can reach. It
leads to program details.

**4.6.2 Select celestial body** *(decided)*. The `solar_system.png` art with one
target per body: Earth, the Moon, the Sun, Mercury, Venus, Mars, Jupiter, Saturn.
Under each body, the count of programs **available** (unlocked for the player's
agency) and **open**. Earth holds Earth-orbit satellites, space planes, and one-,
two- and three-crew capsules; the Moon holds lunar probes and lunar expeditions.
**Needs content work:** G9.

**4.6.3 Program categories and programs** (for one body). The categories at that
body (category art); then the programs in a category (program art), each with its
state: open, closed (components kept, inactive), available, or locked (with the
missing goals). Other agencies' programs are hidden entirely.

**4.6.4 Program details.** Shows:
- state, upkeep per season;
- the price breakdown: opening cost, discount from other open programs, and
  creation cost for components not owned yet (a component still owned from a
  closed program is free);
- the required goals as goal badges;
- the components it grants, each with its reliability bar if owned;
- a link to mission configuration selection.

**Actions:** Open (shows the price; refused if Headquarters is full, the goals
aren't met, or funds are short). Close, which needs a confirmation: "no refund;
its components go inactive and their researchers are released; refused while one
of its missions flies".

**4.6.5 Mission configuration selection.** The program's configurations, each with
art, duration in seasons, prestige (first time, or the repeat tier: 50 % second
time, 30 % after that), launch types, and crew / controller / CAPCOM counts.
Viewable even when the program isn't open *(decided)*; then the planning screens
are read-only, with an "Open program" call to action.

**4.6.6 Mission configuration details and mission steps overview.** The steps,
season by season. For each step: the components involved (with shares), the
controller skills and CAPCOM involved, the crew skills, whether the crew is at
risk, the prestige share, and the goals it achieves. Also the goal dependencies
(penalties until a goal is reached) and boosts.

**4.6.7 Mission configuration** (the planning hub for one configuration), with
four parts:
- **Mission assessment** *(decided: computed in the client)*. Each flown
  component's reliability with goal penalties and boosts applied, the success odds
  per step, and the weak spots. **Ref:** `mission_assessment.png`.
- **Rocket assignment.** One picker per launch. It lists the owned, active rockets
  that can lift that launch's payload mass to its launch type (only human-rated
  rockets for crewed launches), with reliability, unit cost, and spare capacity.
  Ineligible rockets are greyed out with the reason. Nothing is submitted here;
  the choice feeds assembly.
- **Payload R&D.** The same screen as the SET Centre's (§4.8.3), filtered to this
  mission's payloads.
- **Assemble mission** (below).

**4.6.8 Assemble mission** *(decided: one screen merging BASPM's schedule and
assemble)*. It shows the configuration's **staffing layouts** one to one, as BASPM
does (`baspm.md` §10.11), plus the chosen rocket per launch:
- **Flight crew** (`crewSlots`): one titled seat per astronaut aboard, for example
  ASTRONAUT, or COSMONAUT A and B on a joint flight.
- **Mission control** (`controlSlots`): titled jobs such as BOOSTER, RETRO, FIDO,
  GUIDO, SURGEON, FLIGHT, and CAPCOM. Each slot shows its icon for the skill it reads
  (the Flight Director icon for a FLIGHT slot) and its employee type; CAPCOM takes an
  astronaut, every other slot a flight controller.
- Slots no step uses show **"Not required"** and stay empty, as in BASPM. Crewed
  Mercury, for example, has 11 control slots and uses 10 (9 controllers + CAPCOM).
- Click a slot to pick an available employee of its type: idle, not unavailable, and
  not in another slot. Any controller fits any controller slot; each candidate shows
  their value in the slot's skill (a Flight Director slot reads the average of the
  five skills), which is exactly what the roll will read.
- **Auto-assign** (`MissionStaffing.autoAssign` in `core`) fills the crew seats first,
  each with the astronaut best at the skills the steps weight on that seat, then the
  Flight Director slots, then every other used slot, each with the best available
  person at the slot's skill. Nobody is placed twice; a slot nobody is left for stays
  empty.
- A slot that can't be filled says why ("only 5 flight controllers; this mission
  needs 9. Upgrade Mission Control and hire").
- **Confirm & pay**: a price breakdown (configuration cost + the rockets' unit
  costs) and one confirmation ("Launch for 2,243? It flies at the next turn.
  Scrubbing before then refunds in full"). On confirm, the cost is deducted and the
  mission appears under Mission Control's scheduled missions.

**States:** the launch cap is reached (random event); a component is unavailable
this window (random event); short of staff; short of funds.

### 4.7 Mission Control: controllers and missions
**Purpose:** the flight-controller department, and watching missions. Controller
cap 5/13/22/35. **Ref:** `mission_control_*.png`,
`watch_mission_from_mc_or_skip.png`, `vab_with_in_progress_mission.png`.

**4.7.1 Personnel overview** (component 11) for controllers, with sub-screens
Hire, Manage department, Inspect active missions, and Inspect scheduled missions.

**4.7.2 Inspect scheduled missions.** Missions launched today that fly at the next
resolution. Each shows its configuration, rockets, crew, CAPCOM, controllers, and
cost. **Scrub** (red, confirmation) refunds in full and frees everyone.

**4.7.3 Inspect active missions, and the mission replay.** Missions in flight,
each with season n of m, its people, and the components' reliability.
- **Re-staff mission control** *(decided 2026-10-03)*: reopens the assembly screen
  (§4.6.8) for this mission with the crew seats locked and every used control slot
  reassignable to an available person of its type, at no cost
  (`ReassignControlSlotAction`, one action per changed slot; the previous holder
  goes idle). CAPCOM is reassignable too. The same applies to scheduled missions
  (§4.7.2).
- A control slot whose last season has flown is **released**: its holder went idle at
  the season's end, as in BASPM, and the slot shows "Released" and can't be refilled.
  The crew stays aboard until the mission ends.
- **Cancel** (red, confirmation) is offered only for **uncrewed** missions: it
  shows the prestige loss and the percentage refunded.
- Crewed missions show "Can't be abandoned", with no button.

**Replay viewer:** reached from here or from the turn report. It plays a resolved
season's steps one by one:
- the step name;
- the involved components and their reliability;
- the success chance;
- the outcome, and a saving throw if one happened ("glitch, saved by the
  controllers"), plus any Expert rescue with the Expert's name;
- the prestige running total.

Then the season summary: goals achieved, reliability changes, skill growth, and
crew losses. Playback should be skippable and steppable. **Needs server work:** G2.

### 4.8 SET Centre: engineers and payload R&D
**Purpose:** the engineering department and payload R&D. SET cap 7/14/20/30.
**Ref:** `SET_centre*.png`, `rnd_assignments.png`, `upgrade_SET_centre.png`.

**4.8.1 Personnel overview** (component 11) for SET, with sub-screens Hire,
Manage department, and Payload R&D. The state badge shows "Researching ·
component · slot".

**4.8.2 Hire SET personnel** (component 14). The existing recruitment mockup.

**4.8.3 Payload R&D** *(decided: split from rocket R&D)*. The R&D board
(component 10) for payload components: satellites, probes, capsules. Also reached
from mission configuration (§4.6.7), filtered to that mission.

### 4.9 Astronaut Complex
**Purpose:** the astronaut department. Astronaut cap 7/14/24/35. **Ref:**
`astronaut_centre_*.png`, `hire_recruits.png`.

**4.9.1 Personnel overview** (component 11) for astronauts, showing who is on a
mission (crew or CAPCOM) and Expert stars, with sub-screens Hire and Manage
department.

### 4.10 Public Affairs Office: budget and prestige
**Purpose:** money. **Ref:** `public_affairs_office.png`, `budget_info.png`.

**Shows:**
- funds and the funding rate per season;
- running costs broken down: salaries per department, program upkeep, component
  upkeep (active only), building upkeep per building, times any temporary cost
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

### 4.11 Museum: goals, history, rivals
**Purpose:** the scoreboard and the archive. **Ref:** `museum*.png`.

**Tabs:**
- **The race:** the declared goals as goal badges, own progress, and every
  player's progress side by side, by agency name and director name: which goals they
  hold and the turn they got them. **Needs server work:** G3.
- **Achievements:** every goal achieved or cancelled, with the turn and the
  prestige.
- **Flights:** the completed flight count per configuration (`successfulFlightCounts`).
- **Memorial:** astronauts lost.

**Later (M7+):** the educational encyclopedia (`design.md` §1.1).

### 4.12 Settings (header gear)
Log out; server address; sound on or off (later); about and version.

### 4.13 Game result
**Purpose:** announce the winner. **Shows:** an overlay on the hub when a result
exists: winner or tied winners (agency and director name), the turn decided, and the
race table. Play continues, so the overlay can be dismissed and is reopenable from
the Museum.

---

## 5. Refusal reasons (for toasts and inline messages)

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
| WRONG_ASTRONAUT_COUNT / WRONG_MISSION_CONTROL_OFFICER_COUNT / WRONG_CAPCOM_COUNT | Fill every required slot, and leave "Not required" slots empty. (A required slot is empty, a "Not required" one is filled, or there are more people than slots.) |
| UNKNOWN_MISSION_CONTROL_OFFICER | Only a flight controller can take that slot. |
| ASTRONAUT_NOT_IDLE / MISSION_CONTROL_OFFICER_NOT_IDLE / CAPCOM_NOT_AVAILABLE | Someone picked isn't available. (CAPCOM: it must be an available astronaut who isn't also in the crew.) |
| Re-staffing (`ReassignControlSlotAction`): UNKNOWN_MISSION / UNKNOWN_SLOT / SLOT_NOT_USED / WRONG_EMPLOYEE_TYPE / PERSON_NOT_AVAILABLE | That mission has ended. / There's no such slot. / That slot isn't required. / That slot needs an astronaut (CAPCOM) or a flight controller. / They're busy or unavailable. |
| LAUNCH_CAP_REACHED | No more launches this window. |
| MISSION_COMPONENT_UNAVAILABLE | A component is grounded this window. |
| CREWED_MISSION_IN_FLIGHT | A crewed mission can't be abandoned. |
| MAX_LEVEL / ALREADY_UPGRADING | Already at the top level. / Already upgrading. |
| NOT_IN_DEBT / EXCEEDS_CAP / INSUFFICIENT_PRESTIGE | Only possible while in debt. / More than the conversion allows. / Not enough prestige. |
| SLOT_CLAIM_REQUIRED | (Client goes to §4.2.) |
| Join game (`ClaimSlotResponse.Reason`, added 2026-10-04): AGENCY_NAME_TAKEN / DIRECTOR_NAME_TAKEN / MISSING_FIELDS / ALREADY_CLAIMED / NOT_AUTHENTICATED / SERVER_ERROR | Already taken by another agency. (under the field) / the same / Required. / (client logs in again and opens the agency) / (danger notice with the server's words) |

---

## 6. Gaps the server, rules, or content must fill for M5

Found while writing this spec. The client can't build these screens from the
current state alone. Each is an M5 task.

- **G1. Game info.** *(Done 2026-10-04, except the pack download: `GameInfoRequest`/
  `GameInfoResponse`, `design.md` §5 M5 "Game info". Game date: turn 1 is Winter 1955,
  one season per turn, unconfirmed.)* The content pack id and its content hash (so the client loads
  exactly the game's pack), the declared goals, the current game turn and the game
  date (start year and season), the next resolution time, and the game result if
  any. Needs a new `GameInfoRequest`/`GameInfoResponse`; the handshake can't change.
  - **Content is frozen per game** (`design.md` §5 M5, "Content packs"): the server
    copies the pack into the game's data folder at creation and only loads it from
    there. The hash covers that frozen copy.
  - **On a hash mismatch** the client downloads the game's pack from the server
    (a new `FetchContentPackRequest`/`Response`, a few hundred KB of JSON) and
    uses it for this game, instead of refusing to play. A client may hold several
    packs at once, one per game version.
- **G2. Turn report and replay data.** Resolution events (`MissionStepResolved`,
  `GoalSettled`, `ResearchCompleted`, `TrainingCompleted`, `PersonnelDeparture`,
  `SeasonFundsSettled`, `BudgetReviewed`, and so on) are computed and then thrown
  away. Persist each player's events per turn and serve them. The replay viewer
  also needs the step's success chance, which isn't in `MissionStepResolved` yet.
- **G3. Rival summaries.** Each player's agency name, director name, archetype, and the
  declared goals they hold, with turns, for the race tab. This is the
  "per-player summary" `design.md` §3.7 planned for game-meta.
- **G4. Display text.** Content has ids only. The UI needs names and short
  descriptions for categories, programs, components, configurations, steps, and
  goals, written in our own words (never BASPM's, `design.md` §1.2). Add a
  strings file per pack. Each entry can also carry an optional history note
  (component 15), so the content screens can show the icon where a story exists.
- **G5. Personnel names.** Candidates are all called "Candidate". Generate
  plausible names per archetype (our own name lists).
- **G6. Today's queue.** Today's pending actions and their results, for the queue
  panel.
- **G7. Derived numbers the client shouldn't recompute by hand.** The opening
  price, launch cost, eligible rockets per launch, goal modifiers, mission
  assessment, auto-assign ranking, and running-cost breakdown are rules logic. The
  client ships `core` and calls the same methods locally against the preview
  state, so the UI never duplicates the rules.
- **G8. Department salary adjustment (new rule)** *(decided: BASPM's one-off)*.
  An action that changes every salary in one group by p % (−50 to +50) and every
  morale in it by +p for a raise or −3p for a cut, clamped to 0–100
  (`baspm.md` §9.8). Morale feeds hiring quality and resignations, so this is a
  real lever, not a cosmetic one.
- **G9. Celestial bodies (content).** Program categories need a body field
  (Earth, Moon, Sun, Mercury, Venus, Mars, Jupiter, Saturn) for the body picker.
  The available and open counts per body are then derived in the client.

---

## 7. Suggested mockup order

1. **Shared chrome and components (§2):** header, footer, personnel row and
   overview, candidate card, training and department dialogs, reliability bar,
   R&D board, price breakdown, confirmation dialog, toast, building panel.
2. **The first playable loop:** Connect (§4.1), Join (§4.2), hub (§4.3), SET
   Centre with hiring and payload R&D (§4.8), Headquarters as far as opening a
   program (§4.6.1 to §4.6.4), VAB rocket programs (§4.5).
3. **Flying:** mission configuration and assessment, rocket assignment, assemble
   mission (§4.6.5 to §4.6.8), Mission Control (§4.7), the turn report (§4.4).
4. **The rest:** Astronaut Complex (§4.9), Public Affairs Office (§4.10), Museum
   (§4.11), settings (§4.12), game result (§4.13).

Mockups don't need to be pixel-final. Layout, components, and states are what
implementation needs. Flag any new color, component variant, or icon so
`Theme.java` and the UI Asset Sheet stay in sync.
