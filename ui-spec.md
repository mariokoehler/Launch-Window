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
2. **Candidate card or row.** The same fields, plus a Hire button, the portrait
   (`Personnel.portraitId`, in place of the initials square) and the learning
   potential. *(Added 2026-10-05, user decision:)* next to the age, the years left
   until the group's retirement age (astronauts 50, flight controllers 60, SET 65),
   for example "Age 58 · retires in 7 years". Age is now a real trade-off: older
   candidates start with higher skills but lower learning potential and a shorter
   career, so the player has to see both sides at a glance. The learning potential
   could be a small rated value ("Learns: slow / steady / fast") rather than a bare
   number. That is unconfirmed and for the mockup to settle.
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

**Decided (user, 2026-10-04):** no header or footer chrome on this screen, so
the art stays whole — the slim overlay strip above is the only chrome it gets.
Confirmed by comparing two real mockups side by side (`design.md` §3.13); the
with-chrome variant looked clearly worse (it letterboxes the hero art) and was
removed outright, not kept as an alternative.

**Every strip element is solid (2026-10-04):** a navy nameplate for the names, white
chips for the countdown, funds and prestige, the navy date chip, and a white
Turn Report button with a navy outline. There is no translucent chip and no fade,
because the art's top band runs from near-black (night) to near-white (winter noon).
See `design.md` §3.13.

**Settings gear (decided, user, 2026-10-04):** a white icon-only gear button at the end
of the strip opens Settings (§4.12). This replaces "no Log out" below.

**States:** a building that is upgrading shows a subtle "under construction"
marker; a game result exists (§4.13).

**Implemented in libGDX (2026-10-04)** as `client.screen.SpaceComplexScreen`, from the Space
Complex Hub mockup, replacing the stand-in agency screen. The logic is in `client.view.Hub`.
The art comes from `assets-raw/make_hub_art.py`: 32 JPEGs, about 15 MB, cropped to 1080 rows.
Choices made while building it, **confirmed by the user (2026-10-04)**:
- **Time of day:** it steps on when the player enters the hub and at least 4 minutes have
  passed since the *last step*, not since the last entry. Otherwise someone hopping between
  screens every 3 minutes would never see it change. It moves one step however long it's
  been, and the first entry shows noon, as the mockup does.
- **Chips use the implemented chrome's formats** ("SPRING 1955", "NEXT TURN IN 6 H 12 M",
  "$ 25,000") rather than the mockup's "1958 · S2" and "$ 27,420.0".
- **Hover labels use `HEADING_SM` (18 px), upper case,** rather than the mockup's local 20 px
  CSS, which has no design-system token.
- **Turn Report** opens the latest turn's report (§4.4), and a gold dot marks it unread.
  Both came with G2.
- **Log out** is in Settings (§4.12), from the gear button at the end of the strip.
- **Dev mode only:** a gold "Dev · Resolve turn now" chip at the start of the strip resolves
  the open turn at once and opens its report, for play testing (`design.md` §5, M5).
- **The content-differs warning** from the stand-in stays, as a warning notice under the
  agency name.
- **The strip follows the revised mockup** (solid chips, navy nameplate, outlined
  Turn Report, no fade). It has been checked on Soviet winter noon, the brightest art.
- **The "game result exists" state isn't handled.** `GameInfo` already carries the result
  and the winners; only the §4.13 screen is missing.
- **Only Headquarters, the SET Centre, the VAB and Mission Control open.** The other five buildings toast "isn't
  built yet".
- **Breadcrumbs are now live:** "Space Complex", "Headquarters", "SET Centre", and the
  program details' "Open programs" or body crumb go to their screens.

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
**Server work:** G2, done.

**Mocked up (2026-10-04)** as `TurnReport` on the `Launch Window — Mission Control & Turn
Report mockup` canvas, in three variants: an ordinary turn, a milestone turn with goals,
and a turn with a funding review (1963 · S1; reviews come every 16 seasons). Its budget
lines are `core`'s running costs, as on the Public Affairs Office mockup. `design.md` §3.13
has the writeup. Choices still **unconfirmed**:
- one scrolling screen, not tabs;
- running costs broken down by kind;
- the morale warning is derived by the client.

**Implemented in libGDX (2026-10-04)** as `client.screen.TurnReportScreen`, with its
wording in `client.view.TurnReportView`, from G2's saved reports (see G2). Choices made
while building it, **unconfirmed**:
- **When it opens:** an unread report opens on entering the game. A turn that resolves
  while the player is in the game shows a toast and lights the hub's Turn Report dot,
  rather than taking over the screen.
- **"Read" is remembered per device,** in the client's preferences, per server and login.
- **Earlier turns (user, 2026-10-04):** "Previous turn" and "Next turn" step through the
  saved reports. Previous turns off once the server says a turn resolved before the
  player joined. An earlier report leaves out the morale warnings, which describe the
  roster now. Replays work from any report.
- **No hires on the report:** hires are the player's own actions during the day, not
  season events.
- **Mission-end releases aren't listed** ("the 9 controllers are idle again"): no event
  carries them yet.
- **The funding review** names the new rate but not the old one, which the report
  doesn't keep.
- **The People column:** controllers' flight growth is summed into one line. The
  morale warning goes to idle people at morale 50 or below (the resignation floor
  plus 15).
- **News wording** is the client's own, one line per kind of random event.
- **Later idea (user, 2026-10-04):** one real-world space-race story from the same three
  months in every report's news. See `design.md` §1.1.

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

**Implemented in libGDX (2026-10-04)** from the VAB mockup (`client.screen.VabScreen`,
`OpenRocketScreen`, `RocketProgramsScreen`, and `RndBoardScreen` in its `ROCKET` mode,
which is the SET Centre's Payload R&D board with rockets in it). View logic lives in
`client.view.Rockets`. Greying out and refusals use `RocketPrograms.checkOpen`,
`classAllowed` and `inFlight`, the server's own checks. Choices made while building
them, **confirmed by the user (2026-10-04)**:
- **Numbers come from the rules,** not the mockup: the cap (4/8/14), upgrade cost,
  upkeep and seasons all come from the catalog's building schedule. The upgrade dialog
  names the class the next level unlocks ("and unlocks the Medium class").
- **Reopening a kept rocket** says so in its confirmation: "Still owned from before,
  with its reliability. Reopening charges the creation cost again."
- **A rocket on several missions** shows the first mission's badge and "+N".
- **Close on a rocket in flight** is greyed out, and clicking it toasts the refusal,
  as in the mockup.
- **The class gate can't show with the real pack,** because every early-era rocket is
  Light. `RocketsTest` proves it on a fixture catalog.
- **Rocket R&D's breadcrumb** runs through Manage Rocket Programs, and its back button
  goes there, as in the mockup.

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
- the required goals as goal badges (a goal credited through another goal's
  cascade reads "Achieved", without a date);
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

**Mocked up (2026-10-04), §4.6.5 to §4.6.8,** on the Headquarters canvas (bottom row:
`ConfigSelection`, `ConfigDetails`, `MissionPlanning`, `AssembleMission`), for Vostok
Orbital. `design.md` §3.13 has the full writeup and the choices still **unconfirmed**: the
assessment assumes Auto-assign's staff; a step is "Weak" when it ends the mission at least
one time in ten; the slot picker lists only free people; a crew seat shows a "seat fit"
(the same weighted score Auto-assign ranks by). **For G7:** the assessment needs each
step's outcome odds (success, glitch solved, minor glitch, failure, catastrophic) and the
mission's completion and crew-loss odds. `core` has the pieces (`MissionStepResolution`'s
probabilities and band fractions, `MissionProgress.goalModifierFor`, `slotValue`), but no
one method returns the distribution, so the mockup derives it. A small `core` helper
would keep the client from re-deriving the bands.

**Implemented in libGDX (2026-10-04), §4.6.5 to §4.6.8,** from the Headquarters
mockup's bottom row (`client.screen.MissionConfigsScreen`, `MissionStepsScreen`,
`MissionPlanScreen`, `AssembleMissionScreen`). View logic is in `client.view.Missions`,
`MissionDraft` and `CrewSkills`.
- **Rules (G7):**
  - **Odds:** `core`'s new `rules.MissionAssessment` computes each step's exact outcome
    odds and the flight's completion and crew-loss odds from the season pass's own sums.
    It holds each step's goals once the walk is past it.
  - **Checks:** `MissionLaunch.check` is the launch's own checks without the changes,
    and `MissionLaunch.canLift` and `payloadMass` judge the rockets.
  - **Seat fit:** `MissionStaffing.seatFit` scores a crew seat.
  - **Engine refactor:** the engine now shares these pieces, and `ContentPackPlayouts`
    stayed byte-identical.
- **Plan drafts:** the rocket choice and staffing live per configuration on the session
  (`ClientSession.draft`), so they survive moving between screens. They start from the
  first rocket that fits and Auto-assign's staffing. Every rebuild drops people who
  aren't free any more and rockets that can't fly. The draft is cleared after a launch.

Choices made while building them, **confirmed by the user (2026-10-04)**:
- **The assessment uses the plan as it stands,** starting with Auto-assign's staff, rather
  than always Auto-assign's. It leaves out Expert rescues, changes between seasons and
  random events, and says so on screen.
- **Weak steps:** a step is "Weak" when it ends the mission at least 10 % of the time,
  as the mockup proposed. Weak steps share one notice instead of one each.
- **Kept from the mockup's proposals:** the slot picker lists only free people, and a
  crew seat shows a "seat fit".
- **Big odds figures** use `HEADING_LG` (24 px) rather than the mockup's local 40 px and
  32 px styles. Empty and "nobody free" tiles use solid borders, because `Theme` has no
  dashed rectangle.
- **"See it in Mission Control"** opens Mission Control's scheduled missions (§4.7).
- **Configuration names come from their ids** ("Orbital duration 1"), until G4.
- **Configuration art:** `assets-raw/make_content_art.py` converts the configuration art
  in `assets-raw/Mission Configuration/` (provenance as the design session noted it: the
  user's own).
- **New icons:** `make_ui_icons.py` now renders the ten crew and controller skill icons
  and the Flight Director stand-in.

**Implemented in libGDX (2026-10-04), §4.6 to §4.6.4,** from the Headquarters mockup
(`client.screen.HeadquartersScreen`, `OpenProgramsScreen`, `SelectBodyScreen`,
`BodyProgramsScreen`, `ProgramDetailsScreen`). Choices made while building them,
**unconfirmed**:
- **No "Closed · components kept" state.** The state can't tell a closed program from
  one whose components are owned through another program (Vostok's capsule through
  Korabl-Sputnik 2), so a closed program reads Available. Its price breakdown's
  "already owned · Free" lines say what the label said.
- **No "First" ribbon on goal badges,** and next-goal rewards read "Up to N prestige":
  who reached a goal first is the race's standings (G3).
- **Program cards show the full price to open** (opening cost after discounts plus
  components still to create), from the same rule the server charges, rather than the
  bare opening cost.
- **Program and goal names come from their ids** ("Korabl Sputnik 2") until the pack
  has display text (G4); category names are the pack's own.
- **Inert bodies' labels** read "No programs for / your agency" on two lines, so
  neighbouring chips don't overlap.
- **The building panel** (component 7) is built as a reusable piece and used here.
  The SET Centre landing could show it too (§2 says all five capped buildings), but
  its mockup doesn't, so it isn't added there yet.
- **Mission configurations** opens §4.6.5 (built since, see the note above).

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
crew losses. Playback should be skippable and steppable. **Server work:** G2, done.

**Mocked up (2026-10-04)** in the `Launch Window — Mission Control mockup` canvas
(`MCOverview`, `ScheduledMissions`, `ActiveMissions`, `Restaff`, `Replay`); `design.md`
§3.13 has the writeup. Choices still **unconfirmed**:
- no building panel on the overview, as in the SET Centre;
- re-staffing has no confirmation (it's free and reversible);
- the replay shows chances and pass/fail rather than dice numbers, and steps every
  1.5 s;
- "Can't be abandoned" is shown on a fixture card, because the early pack has no
  multi-season crewed configuration.

**Implemented in libGDX (2026-10-04), the replay viewer included,** from the Mission
Control mockup:
- **Screens:** `client.screen.MissionControlScreen` (flight controllers),
  `MissionsScreen` (scheduled or active missions, one class with two modes) and
  `RestaffScreen`.
- **View logic:** `client.view.MissionControl`. Hire, training, pull out, dismiss and
  Manage department are now shared with the SET Centre, through `client.view.Department`,
  `PersonnelDialogs` and a `HireScreen` that takes the department.
- **Rules (G7):**
  - `MissionCancellation.preview` gives the refund and prestige loss the server will
    apply, so the dialogs show $ 211, the integer refund the rule pays, not the
    mockup's $ 211.3.
  - `MissionAssessment` can start from a season, for the odds of what's left.

- **Replay viewer (came a little later, with G2):** `client.screen.ReplayScreen` plays a
  mission's season from the saved report's step events. Nothing is rolled in the client.
  It steps every 1.5 s, and play, pause, step and skip work. "Replay season N" opens it
  for the latest turn.

Choices made while building them, **unconfirmed**:
- **Building panel (changed 2026-10-04, user):** the mockup had none, which left Mission Control
  with no way to upgrade. A compact building cell (level, upkeep, Upgrade) now ends the summary
  strip, as the Astronaut Complex mockup places it, and the SET Centre has the same.
- **Kept from the mockup's proposals:** re-staffing has no
  confirmation; Scrub and Cancel use the generic dialog with price lines.
- **No "prestige so far"** on an active mission: the state doesn't keep it (added to
  G2).
- **A released console says only "Released",** not the mockup's "Released · Fyodor
  Gusev is idle": the season pass empties a released console, so the name is gone.
- **"Changed today"** on Re-staff lists only this visit's changes. Nothing on the server
  records them (G6).
- **The re-staff picker has no "Empty this slot",** because the rule can't leave a
  console empty, and lists only idle people.
- **The Expert mark** is the prestige star in gold beside the skill. The mockup's ★
  glyph isn't in the font.
- **No Expert training here:** component 12 has no Expert option, and no screen offers
  `StartExpertTrainingAction` yet.
- **The roster filter** says "On a mission" where the SET Centre says "Researching".
- **Not reachable with the real pack:** "Can't be abandoned" for a crewed mission in
  flight. It's covered by `MissionCancellationPreviewTest`, not a screenshot.

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

**Implemented in libGDX (2026-10-04)** from the SET Centre mockup (overview and
Payload R&D) and the older Personnel Recruitment mockup (`client.screen.SetCentreScreen`,
`PayloadRndScreen`, `HireScreen`, on a shared `ChromeScreen` with the header, footer,
toast and dialogs). Decisions made while building them, **all confirmed by the user
(2026-10-04)**:
- **Pull out copy corrected.** The mockup says Permanent earns no partial credit, but
  the rules credit the current round of Basic, Advanced *and* Permanent; only Expert
  gets nothing. The dialog now says "They keep partial credit for the round in
  progress." The mockup should be fixed to match.
- **Training lengths and limits come from `core`**, not the mockup: Basic is
  `SET_BASIC_TRAINING_SEASONS`, Advanced `ADVANCED_TRAINING_SEASONS`, and Permanent's
  target slider starts one above the employee's current value in the chosen skill
  (the server refuses anything else) and ends at the skill cap.
- **Assigning R&D.** The server always seats a new researcher in the component's
  lowest free slot, so only that slot shows "+ Assign". It opens a short list of
  idle engineers, best in the component's skill first. (The mockup silently took
  "the next idle specialist".)
- **"Near retirement"** means within 5 years of the group's retirement age (65 for
  SET), not the mockup's fixed 60.
- **Caps come from the content pack** (the SET Centre's 7/14/20/30 by level), never
  from mockup numbers.
- **No funds delta in the footer.** Last season's net change isn't in the state yet
  (gap G2), so none is shown rather than invented.
- **Secondary buttons on the blue page** (Hire, Manage Department, Cancel) get a
  white fill. The design system's transparent secondary button disappears on the
  page.
- **Component names are made from ids** ("vostok-capsule" reads "Vostok Capsule")
  until the pack's display text exists (G4).
- **Hiring several recruits** sends one hire per recruit, in order, and sums the
  outcomes up in one toast. Selection stops at the SET Centre's free places.
- **Recruitment screen chrome** follows the SET Centre mockup: no End Season button,
  breadcrumb root Space Complex, and the next-turn chip.
- **Home** goes to the stand-in agency screen until the hub is built. Footer chips
  and buttons for screens that don't exist yet show a "isn't built yet" toast.

### 4.9 Astronaut Complex
**Purpose:** the astronaut department. Astronaut cap 7/14/24/35. **Ref:**
`astronaut_centre_*.png`, `hire_recruits.png`.

**4.9.1 Personnel overview** (component 11) for astronauts, showing who is on a
mission (crew or CAPCOM) and Expert stars, with sub-screens Hire and Manage
department.

**Mocked up (2026-10-04)** as `AstronautComplex` on the `Launch Window — Astronaut
Complex & Public Affairs Office mockup` canvas; `design.md` §3.13 has the writeup.
Choices still **unconfirmed**:
- **Decided (user, 2026-10-04): Expert training is a fourth kind in the Training dialog** (Basic · 3 seasons,
  Advanced · 2 seasons, Permanent, Expert), not a separate row action. Only skills at
  80+ that aren't Expert yet can be picked. The dialog shows the per-season chance
  (25 % at 80 up to 35 % at 100), no skill growth, and nothing on pull-out.
- An idle astronaut with such a skill gets a gold "Can train toward Expert in …" hint.
- The building panel (component 7) is a compact cell at the end of the department
  strip: level, cap, upkeep, and Upgrade with a confirmation.
- "Near retirement" is within 5 years of 50, as the libGDX SET Centre does for 65.

**Implemented in libGDX (2026-10-04)** as `client.screen.AstronautComplexScreen`, with its
wording in `client.view.AstronautCorps`. Hire, training, pull out, dismiss and Manage
department are the shared department pieces (`Department.ASTRONAUT`). Choices made while
building it, **unconfirmed**:
- **Expert training reaches controllers too:** the fourth kind sits in the shared training
  dialog, so Mission Control's controllers get it as well (SET never does). The threshold
  and the per-season chance come from `SkillGrowth`. Skills that can't train toward Expert
  are greyed out, and each pick shows its value with a gold star on an Expert skill.
- **The building cell** shows the level, the upkeep and Upgrade; the headcount stat
  already shows the cap.
- **The roster** has no "Mission ›" difference between crew and CAPCOM: both open the
  mission's list.

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

**Mocked up (2026-10-04)** as `PublicAffairs` on the `Launch Window — Astronaut Complex
& Public Affairs Office mockup` canvas, with the three states on mockup chips (the debt
one is a labelled what-if). Every number is `core`'s on the fixture; `design.md` §3.13
has the writeup. Choices still **unconfirmed**:
- three columns: funds and running costs; prestige, the next review and the review
  table; active effects and news;
- the next review is shown as a scale of its prestige range with two markers (you, and
  what keeps today's funding), plus a forecast `Notice` (warning when funding would
  fall);
- Convert prestige is a dialog with a slider from 1 to the most allowed, showing funds,
  prestige and the next review's pay before and after, both limits, and a warning when
  next season would still be short;
- the footer delta is last season's actual change; the net per season is shown here;
- the news log lists random events only.
- **Decided (2026-10-04):** a negative value on the page (funds in debt, a negative
  footer delta) is a small solid `danger` tag with white text, the `danger` token's
  documented rule. No separate red text color: one readable as red fails contrast.

**Implemented in libGDX (2026-10-04)** as `client.screen.PublicAffairsScreen`, reached from the
hub and the footer's funds chip. The numbers come from `core`: `BudgetProgress.costDetail`
(the lines, the base and the multiplier's saving), `BudgetProgress.forecast` (the next
review: when, what it pays, what keeps today's funding) and `PrestigeConversion`. Choices,
**unconfirmed**:
- **Known limit:** the client prices reviews and conversions with
  `BudgetSchedule.baspmClose()`, the schedule the server uses today, because the schedule
  isn't sent to the client. A game with another schedule would show wrong numbers here.
- **"Last season"** under the funds is the latest turn report's funds change, fetched
  quietly; it's left out when there's no report. The footer delta stays deferred.
- **The review scale** has no tag above the gold mark; the legend names both marks.
- **The footer's funds** show the danger tag when negative.

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

**Mocked up (2026-10-04)** on the `Launch Window — Museum, Settings & Game Result mockup`
canvas, one artboard per tab (`Museum`, `MuseumAchievements`, `MuseumFlights`,
`MuseumMemorial`); `design.md` §3.13 has the writeup. Choices still **unconfirmed**:
- the tabs are `NavTabs` in the header;
- the race is a table (place, agency with director and archetype, one column per declared
  goal with the turn done and a First tag, done count, finished turn), next to a "This
  game is won by" card with each declared goal's badge and how to get it, and the rule;
- once decided, a success `Notice` on the race tab says who won, with "Show the result";
- Achievements lists all the pack's goals in two columns (done first, by turn), with how
  each was reached or credited, First in the game, and the prestige paid; open goals show
  their first and later prestige;
- Flights shows one card per configuration with its art and the count;
- the Memorial is one plaque per astronaut lost, with an empty state.
**Server work:** G3 (rivals) and G10 (the memorial), both done.

**Implemented in libGDX (2026-10-04)** as `client.screen.MuseumScreen`, with its wording
in `client.view.Museum` and the race table in `RaceTable` (shared with the game result).
Reached from the hub's Museum building. Choices made while building it, **confirmed by the user
(2026-10-04)**:
- **Dates** are the season the goal was done in ("Fall 1960"), from `GameDate`, rather than
  the mockup's "1960 · S4". The result reads "decided in Fall 1960", not "at the turn into".
- **"This game is won by"** has no hand-written "how" line per goal, since content has no
  descriptions yet (G4). It names the goals that also credit it ("Also credited by …").
- **Achievements:** "Credited through" names the goal actually reached (the root of the
  cascade), not the link just above it. Goals settled before goal records existed say only
  "Reached" or "Credited through the cascade", without prestige or a First tag.
- **Memorial plaques** leave out the astronaut's flight count, which the game doesn't keep.
- **The race** is fetched each time the Museum opens; while it loads, the table says so.
- **The memorial's history note** (Bondarenko) is the mockup's draft, still to verify.

### 4.12 Settings (header gear)
Log out; server address; sound on or off (later); about and version.

**Mocked up (2026-10-04)** as `Settings` on the Museum canvas, as a `Dialog` over the hub.
Choices still **unconfirmed**:
- **Decided (user, 2026-10-04): a gear button at the end of the hub strip** (white,
  icon-only, 32 px, like `IconButton`), so Settings (and Log out) is reachable from the
  hub, which has no header. The hub mockup has it too;
- Log out needs no confirmation, since today's actions are already on the server;
- Switch server logs out and opens Connect with the new address filled in;
- About shows the version, the game's content pack and hash, the fan-project note (not
  affiliated with the original game's makers) and the font licenses.

**Implemented in libGDX (2026-10-04)** as `client.screen.SettingsDialog`, opened by the
header gear on every screen and by a white gear button at the end of the hub strip. Log out
and Switch server close the connection and open Connect, Switch server with the new address
filled in (a bad address shows the Connect screen's own error under the field). Sound is
two disabled segments. **Confirmed by the user (2026-10-04):** the hash is shortened to 8 characters; the
"connected" label is fixed text; and the account note says an unlaunched mission plan is
dropped (plans live on the client session), instead of the mockup's "logging out loses nothing".

### 4.13 Game result
**Purpose:** announce the winner. **Shows:** an overlay on the hub when a result
exists: winner or tied winners (agency and director name), the turn decided, and the
race table. Play continues, so the overlay can be dismissed and is reopenable from
the Museum.

**Mocked up (2026-10-04)** as `GameResult` on the Museum canvas: a `Dialog` over the hub,
shown at the first hub visit after the result, with three states (you won outright, a
rival won on the medal table, an exact tie). Every state is computed by a port of `core`'s
`WinCondition`. Choices still **unconfirmed**:
- the medal table (each tied agency's place per goal and its place counts) shows only
  when several agencies finished at the same turn;
- "Keep playing" closes it, with a toast saying where to find it again;
- the race tab of the Museum carries the reopen button.

**Implemented in libGDX (2026-10-04)** as `client.screen.GameResultDialog`. Who won comes
from the game info (the server's result), never from the client. The race and medal tables
come from the rival summaries (G3) through `core`'s `WinCondition`. Choices, **confirmed by the
user (2026-10-04)**:
- **When it shows:** on any hub visit while the result is unseen, unless an unread turn
  report opened at the same time (then the next hub visit shows it). "Seen" is remembered
  per device, like the report's read mark, and set by Keep playing or Open the Museum.
- **The medal table** lists the winner first.
- **Without the summaries** (a failed fetch), it still names the winner and leaves the
  tables out.

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
| Launch (added 2026-10-04): PROGRAM_NOT_OPEN / LAUNCH_CAP_REACHED / MISSION_COMPONENT_UNAVAILABLE / WRONG_ROCKET_COUNT, ROCKET_NOT_AVAILABLE / ROCKET_CANNOT_LIFT_PAYLOAD / WRONG_*_COUNT / *_NOT_IDLE / DUPLICATE_* / UNKNOWN_ASTRONAUT, UNKNOWN_MISSION_CONTROL_OFFICER / CAPCOM_NOT_AVAILABLE / UNKNOWN_MISSION_CONFIGURATION | Open the program first. / No more launches this turn: the launch window is full. / A component is grounded this turn. / Pick a rocket for every launch. / That rocket can't fly this launch. / Fill every slot the mission uses. / Someone in a slot isn't free any more. / Someone is in two slots. / Someone in a slot isn't on the roster any more. / CAPCOM needs a free astronaut who isn't in the crew. / That mission doesn't exist in this game. Short of funds reads "Not enough funds: $ N short." |
| Mission Control (added 2026-10-04): UNKNOWN_MISSION / CREWED_MISSION_IN_FLIGHT / UNKNOWN_SLOT / SLOT_NOT_USED / WRONG_EMPLOYEE_TYPE / PERSON_NOT_AVAILABLE | That mission isn't scheduled or in flight any more. / A crewed mission in flight can't be abandoned. / That console doesn't exist on this mission. / That console isn't needed any more. / That console takes someone of another type. / They're not free any more. |
| Rocket programs: VAB_FULL / ROCKET_CLASS_TOO_LARGE / UNKNOWN_ROCKET (added 2026-10-04) | The VAB can't hold more rocket programs. Upgrade it or close one. / Upgrade the VAB to build this rocket class. / That rocket doesn't exist in this game. |
| Building upgrade: MAX_LEVEL / ALREADY_UPGRADING; program opening also UNKNOWN_PROGRAM | Already at the top level. / Already upgrading. / That program doesn't exist in this game. |
| Manage department (`AdjustSalariesAction`, added 2026-10-04): INVALID_PERCENT / EMPTY_DEPARTMENT | Pick a change between −50 % and +50 %. / There's nobody in the department yet. |
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
- **G2. Turn report and replay data.** *(Done 2026-10-04: each player's turn report,
  `rules.TurnReport`, is saved per turn under `players/<id>/reports/<turn>.json` and served
  by `TurnReportRequest`. Step events now carry the components as flown, both chances,
  which rolls passed, and the season's running prestige. `GoalSettled` says whether a
  goal was first in the game. `SeasonFundsSettled` carries the running costs by kind,
  and R&D events carry the slot and released people's names. Still missing: "prestige
  so far" on an active mission, which isn't part of the state.)* Resolution events (`MissionStepResolved`,
  `GoalSettled`, `ResearchCompleted`, `TrainingCompleted`, `PersonnelDeparture`,
  `SeasonFundsSettled`, `BudgetReviewed`, and so on) are computed and then thrown
  away. Persist each player's events per turn and serve them. The replay viewer
  also needs the step's success chance, which isn't in `MissionStepResolved` yet.
  **What the Mission Control mockup's replay reads (2026-10-04):**
  - *per step:* the step, its season, each component with its involvement and its
    reliability as flown (after goal modifiers), the success chance, whether the
    first roll passed, the saving-roll chance and whether it passed, each Expert
    rescue (who, which skill, the raised chance), the outcome, and the prestige
    change;
  - *per season:* the prestige total, goals reached with the cascade's credits,
    each component's reliability before and after, skill growth per person, the
    consoles released, and crew losses.
  - *per mission (2026-10-04, from the libGDX build):* the prestige earned so far, for
    an active mission's card.
  **What the turn report mockup adds (2026-10-04):** running costs split by kind
  (salaries, component maintenance, program upkeep, building upkeep; `SeasonFundsSettled`
  has only totals), whether each goal was the first in the game, and each R&D gain with
  its researchers and slots.
  **What the Public Affairs Office mockup adds (2026-10-04):** the money amount of an
  unexpected-cost event (a `NewsEntry` stores only its share of funds), and last
  season's actual funds change for the footer.
- **G3. Rival summaries.** *(Done 2026-10-04: `RivalsRequest`/`RivalsResponse`, one
  `RivalSummary` per player read from each one's latest resolved snapshot, with only the
  declared goals' turns; a credited goal counts from its canceller's turn, through
  `WinCondition.goalTimes`.)* Each player's agency name, director name, archetype, and the
  declared goals they hold, with turns, for the race tab. This is the
  "per-player summary" `design.md` §3.7 planned for game-meta.
- **G4. Display text.** Content has ids only. The UI needs names and short
  descriptions for categories, programs, components, configurations, steps, and
  goals, written in our own words (never BASPM's, `design.md` §1.2). Add a
  strings file per pack. Each entry can also carry an optional history note
  (component 15), so the content screens can show the icon where a story exists.
- **G5. Personnel names.** *(Done 2026-10-04: `rules.PersonnelNames`, our own lists per
  archetype, well-known space-program people left out, seeded per candidate so no
  skill roll changes. Since 2026-10-05 each candidate gets a portrait first, from
  `rules.PortraitCatalog`, and a name and starting age that match it: name lists per
  ethnicity and sex in `core`'s `personnel/names.json`, the age from the portrait's age
  range. `Personnel.portraitId` says which portrait; the screens don't show it yet.)*
  Candidates are all called "Candidate". Generate
  plausible names per archetype (our own name lists).
- **G6. Today's queue.** Today's pending actions and their results, for the queue
  panel.
- **G7. Derived numbers the client shouldn't recompute by hand.** The opening
  price, launch cost, eligible rockets per launch, goal modifiers, mission
  assessment, auto-assign ranking, and running-cost breakdown are rules logic. The
  client ships `core` and calls the same methods locally against the preview
  state, so the UI never duplicates the rules.
- **G8. Department salary adjustment (new rule)** *(decided: BASPM's one-off; done
  2026-10-04: `rules.DepartmentSalaries` and `AdjustSalariesAction`, no once-per-season
  limit, as in BASPM, unconfirmed)*.
  An action that changes every salary in one group by p % (−50 to +50) and every
  morale in it by +p for a raise or −3p for a cut, clamped to 0–100
  (`baspm.md` §9.8). Morale feeds hiring quality and resignations, so this is a
  real lever, not a cosmetic one.
- **G9. Celestial bodies (content).** *(Partly done 2026-10-04: `ProgramCategory.body`, a
  `CelestialBody`, optional in packs and Earth when absent, so the early-era pack and its
  hash are unchanged. The converter needs a body mapping once non-Earth categories
  exist.)* Program categories need a body field
  (Earth, Moon, Sun, Mercury, Venus, Mars, Jupiter, Saturn) for the body picker.
  The available and open counts per body are then derived in the client.
- **G10. Memorial records (found by the Museum mockup, 2026-10-04).** *(Done
  2026-10-04: `PlayerSnapshot.memorial`, a list of `MemorialEntry`, written by
  `MissionProgress` for a catastrophic step and by `RandomEvents` for the perished-astronaut
  event. The Achievements tab also got `PlayerSnapshot.goalRecords`, one `GoalRecord` per
  settled goal.)* Astronauts lost
  leave the roster as a `PersonnelDeparture` event (`LOST_IN_ACCIDENT`, or the
  perished-astronaut random event), and nothing keeps them. The memorial needs a
  persisted list per player: name, age, the turn, the cause, and for a flight loss the
  mission and the step.

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
