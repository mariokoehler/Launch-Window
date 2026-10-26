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
- `assets-raw/main_menu/`: the hub art, NASA and Soviet, 4 seasons × 4 times of
  day, plus `<faction>.hotspots.json` with the eight building polygons.
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

**Notes for mockup sessions** (lessons from earlier canvases):
- **The footer stays at the bottom of the frame,** also under short lists. A style on an
  `x-import` only sizes its slot, so `flex: 1` on a `LaunchWindow.Card` doesn't fill: put the
  fill and the scroll on a plain wrapper `div`, and give the footer `margin-top: auto`.
- **Never put a `<button>` (or a mounted `LaunchWindow.Button`) inside an `<a href>`:** the
  button swallows the click. Style the `<a>` itself with `class="lw-btn lw-btn--primary label"`.
- **A page's own `renderVals` key named like a `chromeVals` key** (such as `fundsCls`) is
  silently overwritten, because `chromeVals` is merged last.
- **The design system serves no `tokens.css`:** send the local copy as a file when installing
  the system on a new canvas.
- **`<sc-for>` inside a CSS grid** lays its items out as grid cells, so a table can be a grid
  with one `sc-for` per row.
- **Anything drawn over the hub art** must be checked on the brightest variant (Soviet winter
  noon) and the darkest (winter night).
- **Screen canvases use the Design type** (the Design System type is for the design system
  itself).
- **UI icons and portraits:** upload `assets/ui/icons/*.png` and `assets/ui/portraits/*.jpg` as
  canvas assets. The icons are white on transparent, so use them as CSS masks (`mask-image` in a
  `<helmet>` rule, `background-color: currentColor`) to colour them like the client tints them.
- **Local screenshots of an artboard:** save the canvas's `artifact-type/dc-runtime.js` next to
  the `.dc.html` files as `support.js`, copy the `/_blob/` assets into a local `_blob/` folder,
  serve the folder over HTTP (SVGs as `image/svg+xml`) and screenshot with headless Chromium,
  giving the window about 87 px of extra height. `assets-raw/tools/shoot_canvas.py` does it (2026-10-15;
  `pip install playwright` and `python -m playwright install chromium` first).

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

**Header (every screen except the hub):** back, home, settings and help icon buttons (help
added 2026-10-08, component 23);
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
   number. *Implemented in libGDX (2026-10-05):* the caption reads "AGE 41 · RETIRES
   IN 24 YRS" ("RETIRES THIS YEAR" within a year), and learning potential stays the
   existing number chip. The portrait replaces the initials square in every personnel
   and candidate row (`RosterCells.avatar`), and the initials remain the fallback.
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
   a confirmation. The confirmation also gives the new level's upkeep ("Its upkeep rises from
   $ 150.0 to $ 400.0 a season once the upgrade is done.") and, below the price, how long the
   funds last with it, counted from now on, the safe side (component 25's funds runway; user,
   2026-10-23). Shots: `hq-upgrade.png`, `vab-upgrade.png`.
8. **Goal badge.** The mission-patch circle (`logo.svg` frame), open or
   achieved, with a "first" ribbon when the player was first to it.
   *Implemented (2026-10-06, built in libGDX without a mockup round):* each goal has its own
   embroidered mission patch (`assets/ui/patches/<goal-id>.png`, 128 px RGBA, made by
   `assets-raw/patches/make_patch_assets.py` from the generated variants). A done goal shows
   it in colour; an open goal shows a greyscale copy (`<goal-id>_open.png`) at 60% opacity,
   with no check or star glyph (confirmed by the user, 2026-10-06). A goal without patch art falls back to the drawn circle with a
   check or star. Used so far in the Museum: the race table's cells, "This game is won by" and
   the achievements list, all at 64 px (`RaceTable.PATCH_SIZE`), the smallest size the patch
   text stays legible at (user, 2026-10-06: 30 and 40 px were illegible). The program screens
   still use the drawn circle.
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
    *Specified 2026-10-07 (`design.md` §2.2(d)):* each card shows the team's chemistry
    (component 21); the assign dialog previews how the chemistry would change with each
    candidate, marks candidates with a shown relationship to someone on the team (component 22),
    and shows candidates' revealed traits (component 20). A Perfectionist on the team shows the
    raised cap on the reliability bar.
    *Mocked up (2026-10-07, unconfirmed; see the note after component 22):* a "Team chemistry" row
    between the reliability bar and the slots; the expected gain includes the known chemistry. A
    revealed Perfectionist adds a second, dashed gold cap marker, and the caption says "R&D cap
    91.2, raised to 94.2 while Lindqvist, a perfectionist, is on the team". The assign dialog (the
    built list of idle engineers, best in the component's skill first) adds a "The team now" strip
    with its chemistry, and per candidate: traits under the name, marks for relationships with the
    team, the skill, the chemistry preview and Assign.
    *As built (2026-10-07, People canvas):* a tinted "Team chemistry" row between the caption and
    the slots, with the breakdown on hover or click; the caption names a revealed Perfectionist
    ("raised to 94.4 while Fedorchuk, a perfectionist, is on the team") and the bar gets a dashed
    gold marker (`ReliabilityBar.raisedCap`). The cap, the marker and "cap reached" all read
    `TraitEffects.effectiveCap(..., true)`. Past that cap with people still in the slots (a hidden
    Perfectionist's doing) the card leaves out "Released automatically", so the early tell doesn't
    look broken. The assign dialog is a table (traits, "With this team" marks, skill, "Chemistry
    with them" preview), 1080 px wide. The same cards sit on the VAB's rocket board and the mission
    plan's read-only tab.
    *Looking ahead (2026-10-10, component 25):* milestone ticks and the seasons to each above the bar,
    a retirement warning under the caption, and a "Seasons to cap" column in the assign dialog (now
    1220 px wide).
11. **Personnel overview** (the landing screen of each personnel building,
    *(decided)*, as in BASPM). The roster as personnel rows with row actions;
    filter and sort by state and by skill (skill icons); a **department summary**
    strip (headcount, in training / assigned, near retirement age, average morale,
    total salary per season); and buttons to the sub-screens: **Hire** and
    **Manage department**. **Ref:** `manage_department.png`, `SET_centre.png`,
    `astronaut_centre_*.png`. *Specified 2026-10-07:* each row shows the person's revealed traits
    (component 20) and, later, their ambition with seasons left; clicking the name opens the person
    page (§4.14). *Mocked up (2026-10-07, unconfirmed):* a 250 px Traits column after the state,
    with "Still getting to know them" under any chips while the person is in the reveal window.
    *As built (2026-10-07):* a 230 px Traits column after the state on all three rosters
    (`RosterCells.traits`), with "Still getting to know each other" under any chips while someone is
    inside the reveal window; the name opens the person page (`RosterCells.who`).

    **Row actions:**
    - **Train:** opens the training dialog (component 12);
    - **Expert training:** astronauts and controllers only, when a skill is at 80
      or more; open-ended until the trait is earned;
    - **Pull out of training:** partial credit for fixed-length kinds, nothing for
      Expert; needs a confirmation;
    - **Dismiss:** free, refused for an astronaut or controller on a mission
      (crew, CAPCOM, or controller); SET can be dismissed mid-R&D, which frees the
      slot; needs a confirmation.
12. **Training dialog.** Advanced (3 seasons) or Permanent (advanced
    rounds until a target level), plus Expert for astronauts and controllers. No
    Basic: basic training only follows a hire *(user, 2026-10-05)*. Pick the skill
    with the skill icons, each showing the employee's current value in it, and set
    the target level for Permanent. Nobody in basic training has a Pull out button:
    it can't be cut short. **Ref:**
    `SET_centre_advanced_training_assignment*.png`.
    *Estimate (the user's request, 2026-10-10, component 25):* under Permanent's target slider, updated
    as it moves: "About 12 seasons (4 rounds of 3), done by Winter 1958; 9 to 12 seasons in 8 runs of
    10." (`rules.TrainingPlanning`: the season pass's rounds with the training factor's midpoint,
    revealed traits only, and 300 random runs for the spread; `TrainingPlanningTest` checks it against
    1,000 real trainings). A target the gains can't reach says where training stops ("Gains shrink
    toward 100, so training stops at about 99.6 after 60 seasons."), and a retirement that comes first
    is named. Shot `set-training-permanent.png`.
13. **Manage department dialog** *(decided: BASPM's one-off adjustment)*. A slider
    from −50 % to +50 % that changes **every salary in the group once** by that
    percentage. Morale changes immediately: by +p for a raise of p %, by −3×p for
    a cut of p %, clamped to 0–100. The dialog previews the projected department
    salary per season and the projected average morale before OK. **Ref:**
    `manage_department.png`. **Needs rules work:** G8.
14. **Hire screen.** The group's candidate pool (it refreshes every season), as
    candidate rows, sorted and filtered by skill. Hire is refused when the
    building's roster cap is reached. New hires go straight into basic training on
    a random skill (astronauts 4 seasons, SET 2, controllers 2, BASPM's lengths;
    user, 2026-10-05). It can't be cut short, and the hire screen says how long it
    takes. The existing
    Personnel Recruitment mockup is this screen.
    *Signing bonus (built in libGDX 2026-10-06, user decision):* each selected recruit has a
    check box, "Signing bonus · $ 48 once · morale 71 → 78": a one-off payment of one season's
    salary that multiplies the new hire's morale by 1.1 (at most 100). The footer adds up the
    bonuses next to the added fixed costs. A hire whose bonus the funds can't cover is refused.
    *Specified 2026-10-07 (`design.md` §2.2(d)):* each candidate shows **one hint** from a former
    colleague, such as "driven", "calm" or "reliable", in a quote style ("A former colleague:
    “driven”"); no pronouns, since names don't say how someone wants to be referred to. Traits
    themselves stay hidden. **Later**, an agency with an intelligence service gets a **Background
    check** action per candidate that reveals one trait (or "no notable traits") for a fee;
    design room for it now, but it isn't built until espionage.
    *Mocked up (2026-10-07, unconfirmed):* the hint sits under the age line, set off by a thin
    rule: "A former colleague:" in caption, then the word in italics. Below it, a locked, dashed
    "Background check" button; its answer would be a trait chip (or "no notable traits") in the
    same place. The selected recruit's card repeats the hint.
    *As built (2026-10-07):* "A former colleague:" with the word in italics under the age line; the
    selected recruit's card repeats it. The mockup's locked Background check button is left out
    until espionage (the column has room for it).
    *Specified 2026-10-07 (`design.md` §2.9, Intelligence):* the **Background check** button
    comes back, live from Intelligence level 1: "Background check · uses 1 of 1 action". A click
    asks the server and answers at once, in the same place: a trait chip (component 20) or "No
    notable traits". With no action left or below level 1 the button is disabled with the reason
    ("Needs the Intelligence Office at level 1", "No intelligence action left this season").
    The **passive** find (20 % per candidate) shows the same way from the moment the pool
    appears, marked "Found by the Intelligence Office". A candidate can show the colleague's hint
    and a revealed trait together.
    *Mocked up (2026-10-07, unconfirmed):* on the `Launch Window — Intelligence Office` canvas
    (§4.15). The button sits under the hint ("Background check · uses 1 of 1 action", with the
    binoculars); the answer takes its place, a trait chip or a dashed "No notable traits", with a
    small line saying where it came from ("Background check, this season" or "Found by the
    Intelligence Office"). Disabled, it greys out with the reason under it. A trait found before
    hiring rides along on the selected recruit's card.
    *As built (2026-10-07, Intelligence Office canvas):* under the hint, the find (trait chips or a
    framed "No notable traits") with a binoculars line saying where it came from, then the check
    button while there's more to find; disabled, it shows the reason under it. A revealed trait
    rides along on the selected recruit's card. The button shows at level 0 too, disabled, so
    players learn it exists.
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
16. **Waiting indicator** *(built in libGDX 2026-10-05, user's play-test request)*. When a
    server call takes longer than 0.3 s, a small navy chip, "WAITING FOR THE SERVER" with three
    dots lighting up in turn, shows in the middle of the header row (empty there on every
    screen, the hub included). It goes away when the answer arrives. `SessionScreen` draws it
    from `ServerGateway.isWaiting()`, so every in-game screen has it. Connect and Join keep their
    own busy buttons.
17. **Tooltip** *(built in libGDX 2026-10-05, user's play-test request)*. Rules help on hover:
    a small navy card with a bold title and one to three short sentences, shown 0.4 s after the
    pointer rests on the thing it explains (`UiKit.tooltip`; the text lives in `client.view.Tips`,
    with numbers read from the rules' constants). It's deliberately unlike the white history-note
    card: a tooltip says what something means and does in the game, a history note tells a
    real-world story (component 15). Used on: the salary, morale and learning-potential chips;
    every skill icon in the roster rows, the hire screen's skill names and the training dialog's
    skill tiles (what each skill is used for); the footer chips (what "6 / 3 / 7" means, where a
    click goes); the header's prestige, date and countdown chips; and sort buttons.
    - **Roster skill icons are 17 px** (were 14 px, 20 % up at the user's request) on all three
      personnel overviews.
    - **Flight Director rating:** the controller hire screen shows a sixth row, "Flight Director
      (avg.)", the average of the five skills (`MissionControlOfficer.flightDirectorSkill()`, the
      value a FLIGHT console reads), and can sort by it. The Mission Control overview has a sort row
      (the five skills plus Flight Director, best first; click the active one again for roster
      order). The overview's FD cell already existed.
18. **Explainer** *(mocked up 2026-10-06, the user's idea; E-01 built in libGDX the same day as
    `client.screen.ExplainerScreen`, E-02 to E-19 on 2026-10-07)*. A short series
    of full-screen "infographic" sheets that explain an aerospace idea as it comes up in play. It's the third kind of help, beside the tooltip
    (what a game thing means) and the history note (what really happened): an explainer says
    **how and why something works** in the real world.
    - **Look:** each sheet is drawn like an engineering drawing on the blue page: a white sheet
      with a navy line drawing on the left, three numbered points on the right, a gold "In
      Launch Window" strip tying the idea to a mechanic, and a drawing title block (drawing
      number, title, sheet n of m, scale).
    - **Controls:** Close (top right), Previous and Next with page dots, Done on the last sheet.
    - **How players find it** *(user, 2026-10-07; replaces the notice proposals below)*:
      concept chips (component 19) on the screens that show the content an idea belongs to,
      plus the Museum's Encyclopedia tab. It never opens by itself.
    - **Text** lives in `assets/content/explainers.json` (`client.view.Explainers`), outside the
      frozen pack like `strings.json`; `ExplainersTest` checks every string and drawing.
    - **Canvas:** `Launch Window — Explainers` (private, ask the user for the link), one row
      per explainer, each with a yellow note of the figures still to verify.
    - **First explainer, E-01 "Suborbital vs orbital"**, three sheets:
      1. *Falling around the world:* Newton's cannon, the 100 km edge of space, and the
         "8-kilometre rule" (the ground curves away 5 m every 8 km, and anything falls 5 m in
         its first second, so 8 km/s keeps you falling around the Earth).
      2. *Height isn't the hard part:* Freedom 7 against Friendship 7, drawn to scale (both
         barely above the air), with paired bars for height, speed, energy of motion and time.
      3. *The rocket's hardest bargain:* the rocket equation's propellant share (about 57 %
         for a hop, about 96 % for orbit), staging, and the harder way home (retrofire, then
         about 12 times the heat).
    - **E-02 "Rendezvous"**, two sheets:
      1. *Slow down to catch up:* two chases seen from above, timed to Kepler's law. Burning
         forward lifts you into a bigger, slower orbit and the gap grows; burning backward
         drops you into a quicker one and you meet the target one lap later. Lap times at
         about 200 and 400 km. In the game: Retrofire uses the same idea.
      2. *Learning to meet in orbit:* the approach in a view that travels with the target
         (settle in below, drift forward, burn along the line of sight, brake), and the
         history: Vostok 3 and 4, Gemini 4, Buzz Aldrin's doctorate and Gemini 12, Gemini
         6A/7, Gemini 8, Kosmos 186/188. In the game: the Two crews together goal.
    - **E-03 "Going to the Moon"**, two sheets:
      1. *The speed bill for the Moon:* a route map priced in delta-v (9.4 to orbit, then
         3.1, 0.9, 2.0, 1.9 and 1.0 km/s; the air brakes the trip home for free), with bars
         showing the trip after orbit costs almost as much again. In the game: the VAB's TLI
         payload, 0 kg for the first rockets, 47 t for the Saturn V (reworded 2026-10-09 for the
         standard pack).
      2. *Why the Moon rocket is so big:* Redstone, Atlas, Titan II, Saturn V and N1 with
         heights to scale, a liftoff-mass inset (area to scale), lunar orbit rendezvous, and
         the N1's four failures. In the game: rocket classes and the VAB level.
    - **E-04 "Restarting an engine in space"**, two sheets:
      1. *Fuel with no down:* one tank under thrust (settled), coasting (blobs, a film on the
         walls, gas at the pump) and during a settling burn. In the game: the R-7's history
         note (every engine lit on the ground).
      2. *Three ways to a sure restart:* self-lighting gas-fed engines, a bag in the tank, and
         vanes and screens, with their costs and the early Soviet probes stranded in orbit.
         In the game: Vostok's single retrofire and Payload R&D.
    - **E-05 "Hypergolic propellants and steering jets"**, two sheets (the user's idea; built
      under the shorter title "Hypergolics and steering jets", the mockup's title block name, so
      it fits the eyebrow, the title block and the Museum card alike):
      1. *Fuels that light themselves:* kerosene and oxygen, hydrogen and oxygen, and a
         hypergolic pair side by side, with temperatures to scale, how long each waits in the
         tank, how it lights, its hazards and its exhaust speed; the R-16 disaster. In the
         game: the first rockets are fuelled on launch day (the Countdown step); the Titan II and
         Proton burn self-lighting propellants (reworded 2026-10-09).
      2. *Steering with small jets:* one craft from above, turning, sliding and stopping with
         its thruster quads; cold gas and peroxide in Vostok and Mercury; Gemini 8's spin. In
         the game: the Reentry preparations step and Cooper's hand-flown Faith 7.
    - **E-06 to E-19** *(mocked up and built 2026-10-07 from the user's brief; every figure
      checked, `design.md` §1.1)*, one canvas row each, below E-05, in E-02's sheet template, each with a
      yellow note of the figures and claims to verify. The quoted name is the explainer's title
      and the topic goes in the eyebrow (built as `Explainer.topic`; E-04 and E-05 use it too,
      as their mockups did). Two sheets each unless noted:
      - **E-06 "Falling, not floating"** (Weightlessness): *There is gravity in orbit*; *What
        it does to a body*.
      - **E-07 "Why capsules are blunt"** (Heat shields): *A blunt nose stays cooler*; *How
        the shield survives*.
      - **E-08 "The odds multiply"** (Reliability): *A chain of steps*; *Fighting the odds*.
      - **E-09 "Getting out of a failing rocket"** (Launch escape): *Two answers to one
        danger* (Mercury's tower, Vostok's seat); *When it worked*.
      - **E-10 "Land or sea"** (Coming home): *Parachutes in stages*; *Why each side chose
        its way*.
      - **E-11 "Talking to an orbiting craft"** (Tracking): *Only while it's in sight*; *A
        network around the world*.
      - **E-12 "What Explorer 1 found"** (Space environment): *Belts of radiation*; *Why it
        matters*.
      - **E-13 "Safe enough for a person"** (Human rating): *From missile to crew launcher*;
        *Earning trust*.
      - **E-14 "Earth's free push"** (Launch latitude): *Launch east*; *Where the rocket can
        go*. The game has no latitude rule, so its strips say the VAB's payloads already
        include each real site's push (proposed; or fold it into E-01, `design.md` §5
        Backlog has the mechanic idea).
      - **E-15 "The hardest moment of the climb"** (Max Q), one sheet.
      - **E-16 "Batteries and sunlight"** (Power): *A satellite's power budget*; *Day and
        night in orbit*.
      - **E-17 "Keeping a rocket pointed"** (Guidance): *Balancing on a flame*; *Spinning to
        fly straight* (Jupiter-C).
      - **E-18 "Why satellites fall"** (Orbital decay): *Thin air, long drag*; *The Sun stirs
        the air*.
      - **E-19 "A rocket held up by its own pressure"** (Balloon tanks), one sheet.
    - **As built:** full screen with no app chrome; page dots are clickable; Close and
      Done go back to the screen that opened it (the Museum's Encyclopedia tab for now).
    - **Drawings** are SVG sources in `assets-raw/explainers/`, rasterized at 2x without their
      text by `make_explainer_art.py`; the text goes to a labels file beside the PNG and the
      client sets it as real labels (`client.ui.DrawingView`), so it stays sharp at any window
      size. A sheet's bar chart and key are drawn by the client from values, not pictured.
      A drawing may carry the mockup's own `<style>` block: resvg styles the shapes with it,
      and a text class's font size there becomes the label's size. Text inherits class,
      anchor, fill and `translate()` from its groups. Two kinds of text stay in the picture,
      set by resvg in the repo's own fonts: rotated text (E-02 sheet 2's "Line of sight"; a
      label can't turn), and the white outline under text of class `halo`, so its label stays
      readable over hatching (E-04).
    - **Type:** the reading page sets larger type than the working screens: new `Theme`
      styles `EXPLAINER_TITLE` (38), `POINT_TITLE` (21), `READING` (16/24), `CAPTION_LG` (14),
      and the drawing styles `DRAWING_LABEL` (15), `DRAWING_LABEL_SM` (13), `DRAWING_HEAD` (13,
      upper case), `DRAWING_BIG` (26), `DRAWING_SMALL` (11, upper case), `DRAWING_TEXT` (13),
      and `DRAWING_LABEL_MD` (14, the label size from E-02 on). A drawing's `d-body` text
      (E-05 sheet 2) is set in `CAPTION_LG`. Not design-system tokens yet.
    - **Built differently from the mockup:** no mid-sentence bold (a label can't switch fonts);
      the drawing frame hugs the drawing and sits centred in its column; sheet 2's bars span
      560 px, not 700, so the longest value fits beside its bar; the close button has no
      tooltip. From E-02 on: a drawing's smaller text is set at 13 px, not 12.5, and one
      gold semi-bold caption (E-02 sheet 1, "side by side after one lap") in the regular
      drawing text; E-05 sheet 2's "the whole craft slides up, without turning" is shortened to
      "the craft slides up without turning" so it clears the push arrows. The Museum's
      cards in a row are stretched to one height.
    - **Text changed from the mockup by the fact check** (`design.md` §1.1): E-03's Heinlein line
      is "a saying credited to" him; the Saturn V is "some 24 times" the Atlas, not 25; E-04's
      Soviet probes' top stage had to *start* after a coast (it was a separate stage, not a
      relight), with the August 1962 settling-rocket failure added; the R-16 killed "at least
      78", not "more than 70"; Gemini 8 spun "up to about one turn a second"; E-05's "In Launch
      Window" says every early-era rocket burns liquid oxygen "in its main stages" (Jupiter-C's
      upper stages are solid).
    - **As built for E-06 to E-19:** a one-sheet explainer's eyebrow has no sheet count; the
      Museum's cards use a 1x thumbnail of each first drawing (`<name>-thumb.png`, written by
      the converter) instead of the full 2x picture; halo outlines are drawn last, over every
      line. Closing an explainer frees its full drawings (`UiKit.releasePictures`; user
      2026-10-07), so reading every sheet doesn't pile up hundreds of MB of textures; reopening
      one loads them again, a short wait. Changed from the mockups by the visual check: E-07's "Weight set low" moved under
      the ball, E-11's sound rings shrunk clear of the heading, E-14's bar labels pulled inside
      their bars, halos on labels that cross lines in E-10, E-13, E-14 and E-19, E-12's scale
      "Roughly to scale" (the longer one overflowed its cell), and E-13's "A gentler ride"
      replaced by "A safer fuel" (fact check).
    - **Thumbnails leave out halo outlines** (fixed 2026-10-07): a card shows the picture without
      its labels, so an outline alone would read as a smudge (E-04, E-10, E-13, E-14, E-19).
    - **E-20 to E-30, for the standard pack** *(proposed 2026-10-09 and mocked up from this brief;
      built the same day, every figure checked, `design.md` §1.1)*. Same look as E-01 to E-19, one
      canvas row each. As built, each explainer's title is the title block's (short) name, so E-23 is
      "Launch windows", E-25 "Landing on other worlds", E-26 "Far from home", E-28 "Moon rock by
      robot", E-29 "Rocket planes"; the sheet titles keep the mockups' longer ones.
      - **Changed from the mockups:** E-28's Luna 15 crashed "about two hours", not "a few hours",
        before Eagle lifted off; E-22's drawing says "about a third of an atmosphere" (the fonts have
        no ⅓); apostrophes are straight, as in the other explainers; E-30's "In Launch Window" line
        names the pack's Around the Moon step. Visual check: halos on E-25 sheet 1's braking label,
        E-28's Return capsule, E-29's thrusters note and E-30 sheet 2's corridor label; E-26 sheet 2's
        Jupiter bar shortened to stay inside the frame; E-28's "Ascent stage" on two lines to fit
        its box. E-23 sheet 1 and both E-26 sheets draw their bars in the picture, as the mockups
        do, rather than as client-drawn bars.
      - **Sheet titles** (the topic in brackets):
        - **E-20 "Three ways to the Moon"** (Lunar mission modes): *Three routes to the Moon*; *Why
          the small lander won*.
        - **E-21 "Docking"**: *The last hundred metres*; *Turning around, and moving across*.
        - **E-22 "Spacewalks"**: *A balloon shaped like a person*; *Learning to work outside*.
        - **E-23 "Launch windows"** (Interplanetary flight): *The cheapest way across* (the transfer
          ellipse, orbits to scale); *Why you wait* (the windows from 1960 to 1975).
        - **E-24 "Gravity assists"**: *Borrowing a planet's speed*; *Planet to planet* (Mariner 10,
          Pioneer 11).
        - **E-25 "Landing on other worlds"** (Landing): *Rockets all the way down*; *Venus and Mars*.
        - **E-26 "Far from home"** (Deep space): *Sunlight runs out*; *Talking across the solar
          system*.
        - **E-27 "Pictures before digital cameras"** (Space photography), one sheet: *Film,
          developed in space*.
        - **E-28 "Moon rock by robot"** (Sample return), one sheet: *A piece of the Moon by robot*.
        - **E-29 "Rocket planes"**: *Rocket planes at the edge of space*; *Where does space begin?*
          (added 2026-10-23 straight in libGDX, user: a chart of the speed a wing needs against
          orbital speed, von Kármán's 84 km, the US 50 miles and the round 100 km; `design.md` §1.1).
        - **E-30 "Around the Moon and back"** (Circumlunar flight): *The free return*; *Coming home
          fast*.
      - **Changed from the brief** (by the mockup session, each in its row's note): shorter
        title-block names for E-23, E-25, E-26, E-28 and E-29; E-29 one sheet, E-30 two; the "In Launch Window" strips use the
        standard pack's step names ("Transposition and docking", "Cruise to Mars", "Venus flyby":
        the pack has no "Lunar slingshot" or "transit" step); Mars's air "less than 1 %" of Earth's,
        not "about 1 %"; the X-15's height "about 108 km", not 107.8 (354,200 ft).
19. **Concept chips** *(user's idea, 2026-10-07; specified, mocked up and built the same day; the
    look confirmed by the user that day)*. A small **Concepts** section listing the
    real-world ideas behind a piece of game content, as clickable chips. A chip opens the
    explainer (component 18) that explains it, at the right sheet; Close and Done come back to
    the screen the chip was on. It's how players reach explainers from play, instead of a
    notice the first time an idea comes up.
    - **A concept** is an id, a chip label ("Suborbital", "Retrofire") and a target: an
      explainer and a sheet. Several concepts can share an explainer ("Suborbital" and "Orbit"
      both open E-01 sheet 1). Each concept lists the programs, configurations, steps,
      components and goals it belongs to. The audit that assigns them is in `design.md` §1.1.
    - **A concept without an explainer yet** is listed in the data but shows no chip
      (default): no dead ends in the game, and the list doubles as the backlog of explainers
      to write.
    - **Where chips appear** (default):
      - **Program details** (§4.6.4): the program's own concepts and those of the components it
        grants (the mockup's choice, built).
      - **Mission configuration details and steps overview** (§4.6.6): the configuration's
        concepts plus those of its steps, each once. Not on the planning hub (§4.6.7): it's a
        tabbed screen with no summary, and the mockups leave it out.
      - **Open rocket program** (§4.5.1): a line of its own under each rocket's row.
      - Not on the step rows themselves (too dense), nor on the mission selection list.
      - **Later, if wanted:** the same chip row under other text, such as a pinned history
        note card (component 15). Inline links inside text are out (user, 2026-10-07: a
        libGDX label can't make one word clickable, and a word-by-word paragraph widget isn't
        worth it).
    - **Order:** the order of the concepts list, so related chips sit together. If a row runs
      long it wraps; there is no "more" overflow (default; the audit gives at most six per
      item).
    - **Look (proposal for the mockup):** a compact chip in the explainer's drafting style: white
      fill, thin navy rule, the label in `DRAWING_LABEL_SM` navy, and a small glyph of its own
      (a drafting triangle or set square) so players learn that it always means "how it works",
      distinct from the history note's open book and from the rules tooltip. Hover darkens the
      rule; the whole chip is the click target (at least 32 px high). The section's kicker
      reads "Concepts" in the eyebrow style.
    - **New:** a chip whose explainer the player has never opened carries a small gold dot,
      and so does its card in the Museum's Encyclopedia tab. Opening any sheet of the explainer
      clears it everywhere. Seen explainers are remembered on this machine, in the client's
      preferences, keyed by account and explainer id (default; the server doesn't need to know).
    - **Data:** `assets/content/concepts.json` (`client.view.Concepts`), outside the frozen pack
      like `explainers.json`, so a new concept never changes the pack's hash. Step ids are
      the base ids, without the season number or the `spc-a-`/`spc-b-` prefix of a joint flight.
      A test checks that every id exists in the pack, every target explainer and sheet exists,
      and every label uses only letters the fonts can draw.
    - **The standard pack's chips** *(built 2026-10-09 with E-20 to E-30)*: 18 new concepts link the
      new explainers to the full tree (lunar mission modes on the Moon programs, docking and
      spacewalk on their steps, suits and power sources on components, launch windows on the
      planetary programs, and so on; the assignments are in `concepts.json`). To keep every
      configuration at six chips or fewer *(confirmed by the user, 2026-10-09)*, the basics that every later orbital flight shares, Orbit, Retrofire and Tracking, now sit on the
      early flights where they are new (listed by configuration) instead of on the shared steps;
      the early era's chips are unchanged. `ConceptsTest` checks against the standard pack.
    - **The first 19 explainers on the full tree** *(built 2026-10-09)*: rocket ideas (staging,
      rocket classes, delta-v, cryogenic propellant, engine restarts, human rating, launch latitude,
      guidance, max Q, balloon tanks) on the new rockets, capsule ideas (retrofire, launch escape,
      steering jets, the one-shot retrorocket) on the new capsules, and flight ideas on the new
      steps (weightlessness on the longer duration levels, land or sea on splashdowns, rendezvous on
      the rendezvous flights by name). A new concept, "Self-lighting propellants" (E-05 sheet 1),
      sits on the Titans, the Proton, the UR-700, the Transtage and Apollo's two craft. Trimmed to
      the caps: no engine restarts or self-lighting chip on the Agena (its programs would pass six),
      no land or sea on Apollo's recovery step, no launch latitude on the Saturn V (nine rocket
      chips already).
    - **Mocked up (2026-10-07)** on the `Launch Window — Explainers` canvas, bottom row: the chip
      on its own (`ConceptChips`), Program details for Vostok (`ProgramDetailsConcepts`), Vostok
      Orbital's steps and goals (`ConfigDetailsConcepts`), the VAB's Open rocket program with
      Jupiter-C and Atlas (`OpenRocketConcepts`), and the Museum's Encyclopedia
      (`MuseumEncyclopedia`). The screens are copies of the existing mockups (the libGDX layout
      for the VAB row and the Encyclopedia) with only the Concepts section added. A sticky note
      lists the choices, all **unconfirmed**:
      - **Look:** 32 px high, white, a 1 px navy rule; the set square (new icon `concept`,
        `assets/ui/icons/concept.png`) sits in its own 30 px cell with a thin navy-border divider,
        like a title-block cell; the label in Archivo Bold 13 navy. Hover thickens the rule to
        1.5 px and tints the glyph cell; pressed fills navy; keyboard focus is the gold ring.
      - **New dot:** 8 px gold with a 2 px white ring on the chip's top-right corner, like the
        Turn Report's. A Museum card gets a 10 px one on its corner and nothing else.
      - **No hover tooltip** on a chip, so it never reads as rules help.
      - **Section header:** "Concepts" in the card's own section-title style (caption, upper
        case, muted), not the eyebrow, so it sits with Required goals and Components. A wide
        card adds the hint "How the real thing works: each opens an explainer sheet"; the
        narrow configuration column shows "N not read yet" instead.
      - **Program details** shows the program's own concepts *and* those of the components it
        grants, each once (Vostok: Orbital speed, Retrofire, One-shot retrorocket, Steering
        jets), since a capsule has no screen of its own; after "Components it grants". This
        widens the "Where chips appear" default above.
      - **Configuration details:** inside the summary card in the left column, under the key
        facts. Vostok Orbital shows Orbit, Staging, Retrofire, Engine restarts, Cryogenic
        propellant and Steering jets.
      - **Open rocket program:** six chips don't fit on a row, so every row gets an
        always-shown Concepts line under it, aligned with the name column (no expandable
        detail).
      - **Design system:** confirmed (user, 2026-10-07), so the next mockup session should add
        it to the Launch Window design system as `ConceptChip` (it's local CSS in the mockups so
        far). In the client it's `client.ui.ConceptChip`, using `Theme`'s existing tokens.
    - **As built (2026-10-07):** `client.ui.ConceptChip`, laid out by
      `ChromeScreen.conceptsSection`/`conceptChips` (a wrapping `HorizontalGroup`), the data in
      `assets/content/concepts.json` read by `client.view.Concepts` (checked by `ConceptsTest`,
      which pins the mockup's four chip lists). Read marks: `LaunchWindowGame.explainerRead`/
      `markExplainerRead`, in the client's preferences per server and login; opening any sheet
      marks its explainer read. Built differently from the mockup:
      - the set square is drawn at 18 px, not 16, because the 48 px icon's thin strokes turned to
        grey mush at 16;
      - no keyboard focus ring (the client has no keyboard focus traversal);
      - coming back from an explainer rebuilds the screen, so a scrolled screen opens at the top
        again;
      - the Museum's encyclopedia grid is 8 px lower, to leave room for the dots over its top row.
      - Engine restarts is attached to the Vostok configurations and R-7 (Vostok) by name, not
        to the Retrofire step, so it stays off Mercury, whose retropack was solid rockets.
20. **Trait chip** *(specified 2026-10-07 for `design.md` §2.2(d), mocked up and built the same day)*. One of a
    person's revealed traits, such as "Workaholic". Shown on personnel rows (component 11), the
    person page (§4.14) and in staffing pickers. A tooltip says what it does in plain words
    ("More R&D progress. Loses morale fast when idle."), read from the rules' constants.
    - **Look (proposal):** a small tag, quieter than a concept chip (component 19) and not
      clickable, so the two never get confused. Most traits cut both ways, so no good/bad colour
      coding by default; perhaps a subtle mark for the team traits (they act on others) against
      the personal ones.
    - **Not known yet:** never show empty slots or a count (that would leak how many traits
      someone has). Until someone's traits have all had time to show, their row or page may
      say "still getting to know them"; afterwards "nothing more to discover" (or nothing).
    - **Mocked up (2026-10-07)** on the `Launch Window — People` canvas (see the note after
      component 22). Look, all **unconfirmed**: a 22 px tag filled with `skill-track`, Work Sans 500
      12 px in `ink`, no border, square corners, never wrapping. One fill for every trait. Team traits
      carry a small three-dot mark (new icon `trait-team`) before the label; personal traits have
      none. It isn't a button: default cursor and no hover change. Hovering shows the client's
      tooltip with the label, "Personal trait" or "Team trait · acts on the whole team", and
      `TraitTexts.effect`. While someone is in the reveal window, "Still getting to know them" sits
      in italic `ink-muted` under any chips; after it, "Nothing more to discover" on the person page
      only (rows show nothing).
    - *As built (2026-10-07, People canvas):* `client.ui.TraitChip`, 22 px, skill-track fill, Work
      Sans Medium 12, square, no border; team traits get a three-dot mark (`trait_team` icon). No
      hover look, no click; the tooltip names the trait, personal or team, and what it does
      (`TraitTexts.effect`). "Still getting to know each other" (the spec's "them" was a pronoun) and
      "Nothing more to discover" in a small italic (`TextStyle.CAPTION_ITALIC`, SemiBold Italic
      11: the client ships no Medium Italic).
21. **Chemistry indicator** *(specified 2026-10-07, mocked up and built the same day)*. How well a team works
    together: one component's R&D engineers, or one mission's crew and controllers. Five levels
    from the chemistry value (−1..+1): **Poor** (below −0.5), **Strained** (−0.5 to −0.15),
    **Neutral**, **Good** (0.15 to 0.5), **Excellent** (above 0.5), with the effect on output as
    a percentage (chemistry × 15 %, so −15 % to +15 %).
    - **Where:** each R&D board card (component 10), the assign dialog's preview ("with Kowalski:
      Good → Strained"), the mission plan's assessment (§4.6.7) and the assemble mission screen
      (§4.6.8), next to the staffing.
    - **Breakdown** on hover or click: the known reasons, one line each: "Ivanova and Petrov work
      well together +", "Abrasive: Reyes", "Loner, working alone +", "Team player: Okafor +".
    - **Unknown factors:** the client only knows revealed traits and shown relationships, so the
      indicator says "may differ: unknown factors" while some of the team's traits or pairs are
      still hidden. It must never hint at what is hidden.
    - **Mocked up (2026-10-07).** Look, all **unconfirmed**: a five-step scale (12 × 8 px steps,
      2 px apart) with only the current step filled, then the level's word in Work Sans 600 and
      "+4 % output" in `ink-muted`. Unlike traits, colour does code good and bad here: Poor `danger`,
      Strained `gold` (word in `gold-text`), Neutral `navy` (word in `ink`), Good and Excellent
      `success`; the word and the step's position carry it too. "may differ: unknown factors" sits
      under it in italic caption. A compact form (6 px steps, no "output") goes in rows and pickers,
      and the preview reads "Good → Strained −5 %", with "may differ" under it when the candidate or
      the team is still in the reveal window. The breakdown opens on hover and pins on click, like
      a history note: a white card with a navy rule, one line per known reason with its glyph
      (relationship marker, or the team-trait dots) and only a direction (+ or −, in `success` or
      `danger`), never an amount; with unknown factors it ends with "May differ: unknown factors.
      Some of this team are still new to each other, so the real chemistry can be better or worse."
    - *As built (2026-10-07):* `client.ui.ChemistryIndicator`: a five-step scale (12×8 px steps, 6×6
      compact) with the level's step filled, coloured by level (danger, gold, navy, success,
      success), the word and "+4 % output". Forms: `full`, `withBreakdown` (hover previews, a click
      pins, via `client.ui.Popover`), `compact`, `preview` ("Good → Strained −5 %"), and `breakdown`
      for the inline "Why" list. Breakdown lines give direction only (+ / −). Levels and percentages
      come from `client.view.People` (the percentage from `Chemistry.OUTPUT_BAND`).
      Not on the mission plan's assessment tab yet (the mockup left it out); its odds already count
      the known chemistry.
22. **Relationship marker** *(specified 2026-10-07, mocked up and built the same day)*. A shown relationship
    between two people: "works well with" or "clashes with". Shown on the person page as a list,
    and in staffing pickers next to a candidate who has one with someone already in the team (a
    small mark and the partner's name in the tooltip), so the player can build teams around
    friendships or keep enemies apart.
    - **Mocked up (2026-10-07).** Look, all **unconfirmed**: linked rings in `success` for "works
      well with" and a spark in `danger` for "clashes with" (new icons `rel-good`, `rel-clash`):
      a relationship, unlike a trait, is plainly good or bad. On the person page each is a row:
      40 px portrait, the kind in label caps in its colour, the name (opens their page), and on
      the right what they do now and "7 seasons together". In pickers and on mission tiles, the
      glyph and the partner's surname; the tooltip names them in full ("Works well with Gunnar
      Lindqvist · Already on this team").

**People screens mockup (2026-10-07)** on the canvas `Launch Window — People` (a Design canvas;
ask the user for the link), for `design.md` §2.2(d) phases 1 to 3, with room for phase 4. Twelve
artboards in six rows, each row with a yellow note of its choices, all **unconfirmed**:
- **Components 20 to 22** (`Main`): every trait, tooltips, the five chemistry levels, unknown
  factors, compact and preview forms, the breakdown, both relationship forms, and a list of what's
  new: no new colour tokens; icons `trait-team`, `rel-good`, `rel-clash` (inline SVG so far, for
  `assets-raw/svg/ui/` and the UI Asset Sheet); a dashed gold "raised cap" marker on the
  reliability bar. `TraitChip`, `ChemistryIndicator` and `RelationshipMarker` go into the design
  system once the user confirms the look.
- **Person page** (§4.14) in four states: a new hire, a veteran with phase 4 placeholders, someone
  with nothing more to discover, and a memorial entry.
- **Payload R&D** (component 10): the board with chemistry, and the assign dialog.
- **Assemble mission** (§4.6.8) with the mission's chemistry, and the CAPCOM picker.
- **SET Centre roster** (component 11) with a Traits column, and the **hire screen** (component 14)
  with the colleague's hint and the later Background check.
- **Turn report** (§4.4) with the people lines.

The samples are an American agency in 1961 · S3 (SET Centre, hiring, person pages, turn report) and
a Soviet one in 1960 · S3 (Vostok Orbital), with names from the game's own lists, its portraits,
and chemistry worked out by the as-built rules from the revealed traits and shown pairs.
Writing them turned up two things for the rules, recorded in `design.md` §2.2(d).

    - *As built (2026-10-07):* `client.ui.RelationshipMarker`: linked rings in green (`rel_good`) and
      a spark in red (`rel_clash`); `kind` for the person page's list, `mark` for pickers (the
      partner's surname, the full name in the tooltip). Both glyphs, and `trait_team`, are on the UI
      Asset Sheet (2026-10-08).

23. **Help** *(the user's idea, 2026-10-08; specified and built the same day without a mockup, the
    layout being plain; the user confirmed every point below)*. The game's rules and mechanics by
    topic, where the explainers (component 18) tell how the real thing works.
    - **Opening:** a `help` icon button (a question mark in a circle) after the settings gear in
      every header, and after the gear at the end of the hub strip; **F1** anywhere in the game. It
      opens **over the current screen** (a panel of 1560 by 920 on the navy scrim), not as a new
      screen, so closing it (the close button or **Esc**) leaves the player exactly where they
      were, mid-assembly or with a dialog still open underneath. It has its own layer, so it never
      closes a dialog and a dialog never closes it.
    - **Layout:** "Help · Rules and mechanics" and the close button on top; a search field (a
      magnifier inside it) with **Show all topics** beside it; a status line; then the topic list on
      the left (340 px, grouped under small captions: Basics, Agency, Programs and hardware, People,
      Rivals) and the selected topic's text on the right (lines at most 860 px wide), each column
      scrolling on its own.
    - **Context:** each screen names the topics that matter there (`Help.Context`, overridden in
      the screen's `helpContext()`), and help opens filtered to them, on the first, with a navy chip
      in the status line: "3 topics for the SET Centre". A screen without a context opens on every
      topic ("All 7 topics.").
      Synced with the code on 2026-10-22: 32 topics, the newspaper's among them ("The Daily Zenith"); its pages open
      on it (`Help.Context.NEWSPAPER`), the archive's pages and the explainers on the Museum's. `HelpTest` checks that
      every context names real topics and that every topic is on some screen's list (the basics on the hub's).
    - **Filter:** typing searches **every** topic's title and text (numbers filled in, ignoring
      case) and drops the screen's filter; the status line says how many topics mention the text,
      or that none does. **Show all topics** clears both the text and the screen's filter.
    - **A topic:** its group as an eyebrow, its title, then paragraphs, subheadings and bullets.
      Below a rule: **See also**, other topics as secondary buttons (a topic the filter hides
      clears the filter so the list can mark it), and **How the real thing works**, explainers as
      concept chips (component 19) with their unread dot. An explainer opened from help comes back
      to the same screen with help still open on the topic (`LaunchWindowGame.visit` keeps the
      screen instead of disposing it).
    - **Content:** `assets/content/help.json`, outside the content pack like `strings.json` (never
      rename or reuse a topic id). Numbers are tokens such as `{poaching.minTenure}`, filled from
      the rules' constants and the game's budget schedule (`client.view.HelpValues`), never typed
      into the text. `HelpTest` checks the ids, tokens, links, every screen's topic list and the
      letters. **28 topics** (2026-10-08) in six groups: Basics (turns, the buildings, the turn
      report, winning, the Museum), Agency (the budget, prestige and goals, random events),
      Programs and hardware (programs, component R&D, rockets), Missions (planning and launching,
      how steps are decided, Experts, flight controllers and CAPCOM), People (hiring, training,
      morale, salaries, traits, chemistry, ambitions and loyalty, careers, astronauts, PR duty,
      poaching) and Rivals (the race, intelligence). Every number in them is a token, from the
      rules, the budget schedule or the pack's buildings. 16 contexts cover the building screens,
      the hire screen, mission planning, the person pages, the memorial, the turn report and the Museum; the hub
      and the remaining screens open on every topic.
    - *As built:* `client.screen.HelpOverlay`, `client.view.Help` and `HelpValues`; the `help` and
      `search` glyphs in `make_ui_icons.py`, on the UI Asset Sheet since 2026-10-08 (chrome section,
      after `settings`, with the header, hub strip and search field in context). Shots `help-*.png`.
    - **Any topic list** (2026-10-08): help also opens on an ad-hoc list (`Help.Topics`, beside the
      screens' `Help.Context`), as the advisor's items do; the chip names it ("1 topic for "Open a
      rocket program"").
24. **Advisor** *(the user's idea, 2026-10-08; built the same day without a mockup, the user having
    confirmed the plan)*. A new agency's first things to do, then the road ahead as the programs open in
    stages, on a card at the hub's bottom left, until they're done.
    - **The items, in order** (`client.view.Advisor.ITEMS`): open the help; hire engineers; open a
      program (a satellite is the cheapest start); open a rocket program; put engineers on R&D;
      hire flight controllers (once a program is open: every mission, uncrewed too, needs
      consoles); hire astronauts (only once a program with a crew or a CAPCOM is open); raise
      reliability; plan the first mission; read the first turn report (only once a turn has
      resolved).
    - **The road ahead** (the user's idea, 2026-10-26, with the guided progression, `design.md` §2.3(d);
      built without a mockup, they're items like the others; `Advisor.ROAD_AHEAD`), after the first steps:
      "Start on a person in space" (the one-crew capsule, named: Mercury or Vostok); "Fly the X-15
      meanwhile" (or the E-154: cheap and quick while the capsule is in R&D, and its pilots earn
      rocket-plane wings); "Send a probe to the Moon" (once the capsule is open); "Choose your road to the
      Moon" (once a person has been to space: the bigger capsules and the Moon craft's Earth-orbit tests);
      "Go to the Moon" (once a probe has reached it and a person has orbited the Earth). Each comes when
      its programs can be opened (the first two also wait for a first flight, as in the ungated early-era
      pack), goes once one of them is open or the player has moved past it (the rocket plane once a pilot
      has wings or a bigger capsule is open), and is "not needed" for an agency without such programs.
      Waiting on one, the card says "All set for now" and when it comes ("Once your first satellite
      reaches orbit: start on a person in space."). Once the first steps are done the head reads "The
      road ahead". `Advisor.title`/`text` put the agency's own program names in.
    - **Rocket-plane wings on other screens** (2026-10-26, `design.md` §2.2(c); straight into libGDX): the
      Astronaut Complex roster shows "Rocket-plane wings" in green under the astronaut's state, the person
      page a "Rocket-plane wings" badge under the skills with "Safer piloting in capsules", each with the
      tooltip `Tips.wings()`; the mission plan's step list folds the wings into the findings' green badge,
      one "+N" for both, the tooltip naming each source, and its footnote says so; the turn report's People
      card has "<name> earned their rocket-plane wings". Shots `progression-*.png` (`-Dshots=progression`).
    - **Reliability first** (the user's request, 2026-10-08): "Plan your first mission" is held back
      until an open program's payload (its least reliable component) and an open rocket both reach
      `Advisor.READY_RELIABILITY`, 60 %. Until then "Raise reliability to 60 %" stands in for it,
      with a navy line on where things stand ("Now: payload 41 %, rocket 22 %.") and a Go link to the
      R&D board of whichever is further behind. A mission already in flight or flown ticks off both.
    - **Nothing to open while the engineers train** (play test, 2026-10-09): opening a program or a
      rocket costs money every season, so "Open a program", "Open a rocket program" and "Put
      engineers on R&D" wait until an engineer is out of basic training, and "Raise reliability"
      until someone is on R&D. Meanwhile the card says "All set for now" and "Once your first
      engineers finish basic training: open a program." (`Advisor.waitingFor`).
    - **Ticked off from the state, never from a click:** someone hired, a program open, an engineer
      on an R&D slot, a mission in flight or flown before. Only "help opened" and "report read" are
      this client's own marks (preferences per server and login, like the report's read mark).
    - **The card:** a navy head ("ADVISOR", "Your first seasons"), then the next **three** open
      items (each a bold title, a line of why, a **Go** link to the building and a **Rules** link
      that opens help on the item's topics), "N more things after these", a rule, the line that
      there's no button to end a turn. **No hide link** (user, 2026-10-26, replacing "I know the ropes ·
      hide the advisor": a new player could click it by accident and lose the help that comes later;
      minimizing is the way to quiet it). With nothing open but something still to come, it says "All
      set for now" and names it. 380 px wide, touchable as
      a whole so the hotspots underneath never take a click; at the bottom left it covers no
      building marker on either agency's art (only the corner of NASA's SET Centre outline).
    - **Minimize** (the user's idea, 2026-10-10, for the new NASA art, candidate 125, where the card
      covers part of the SET Centre): a white chevron at the end of the head, pointing down. A click
      anywhere on the head shrinks the card to the head alone, which drops to the bottom left, with
      the chevron pointing up and the caption counting what's left ("4 things to do", "1 thing to
      do", or "All set for now") instead of "Your first seasons". Another click opens it again.
      *Unconfirmed default:* the state lasts for the client run (`LaunchWindowGame.advisorMinimized`),
      across hub visits, and a fresh start shows the whole card again, so the advisor is never lost
      by accident. Shot `advisor-minimized.png`.
    - **Gone for good:** once every item is done (the report and the road ahead included: a Moon program
      open), the client marks the advisor retired and never shows it again for that login on that
      server. Not shown once the game is decided. A login retired by the old hide link stays retired. A dev
      data reset deletes the client's preferences, so the advisor comes back.
    - *As built:* `client.view.Advisor` (`AdvisorTest`), the card in `SpaceComplexScreen`, the marks
      in `LaunchWindowGame`. Shots `advisor-*.png` (`advisor-reliability.png` and `advisor-ready.png`
      for the bar); every other hub shot shows the card too.
    - **The idle tip** (the user's idea, 2026-10-12; built without a mockup, it's one more item on the
      card): "Keep idle people busy", with the morale numbers from the rules (idle loses 2 a season,
      resigns at 35) and that training is free. It's a one-off apart from the first steps
      (`Advisor.IDLE_TIP`, never in `ITEMS`, never counted toward retiring): due once the agency is set
      up (someone on R&D or a mission flown) and someone out of basic training is idle and available
      and nobody in advanced or permanent training yet (`Advisor.idleTipDue`). It goes first on the
      card; its Go link ("Their building") opens the building of the first group with someone idle,
      engineers first, and a **Got it** link puts it away. On a retired advisor the card comes back
      for it alone, captioned "A tip", without the footer. It goes for good once it stops being due
      after it was shown (someone sent to training, or nobody idle), or on Got it (client marks
      `idleTipShown`, `idleTipDone`). Its timing (as soon as someone out of basic training sits idle, even during the first steps) is confirmed by the user (2026-10-12). Shots
      `advisor-idle-tip.png` (retired) and `advisor-idle-tip-first-steps.png`.
25. **Looking ahead** *(the user's idea, 2026-10-10: "thinking ahead gives you a big advantage in this
    game"; the user liked all seven proposals and asked for them built at once, without a mockup,
    since each extends a component that exists).* Every season count comes from the rules, never the
    client: `ResearchPlanning.project` and `ranges` for R&D, `PersonnelLifecycle.seasonsUntilRetirement`,
    the training seasons left, and `BudgetProgress.runway`; `client.view.Outlook` words them
    (`OutlookTest`). "By <season>" means the open turn plus that many seasons: the season you have it in.
    - **R&D milestones** (component 10). Thin navy ticks on the bar at 60, 70 and 80 % still ahead
      (`Outlook.MILESTONES`; 60 is `Advisor.READY_RELIABILITY`), a milestone within 3 points below
      the cap left out, and above each tick and the cap's marker a small "~3": the average seasons
      to get there with the team on it now (`MilestoneLabels`, which hides the lower of two labels
      that would overlap; the cap's is gold). "—" means not within ten years (`HORIZON_SEASONS` 40).
      No numbers without a team. The tooltip "Seasons to each mark" lists each mark with its season
      and the spread of 300 runs with random draws ("2 to 3 seasons in 8 runs of 10", fixed seed).
      The projection repeats the season pass's average: each slot's band midpoint, the engineers'
      skill growing by its average draw (a revealed Mentor counts), a revealed Restless engineer
      going stale, an engineer reaching retirement age leaving the team, an event's R&D multiplier
      ending when its seasons run out, chemistry as the player knows it. Nobody new joins. A test
      checks it against 2,000 real eight-season runs (within 3 %). The caption's "about +X next
      season" now reads the projection's first season; `expectedSeasonGain` itself is unchanged,
      since `PressEffects` uses it on the server.
    - **In the turn report** (the user's request, 2026-10-10): the R&D section's component bars get
      the same ticks, counts and tooltip (`RndCards.milestones`), but only while the bar shows
      today's reliability, so an older report never marks a bar the component has moved on from.
    - **Idle people on the hub** (the user's idea, 2026-10-10, §4.3): a small red dot with a white
      ring on the upper right of the Astronaut Complex's, Mission Control's or the SET Centre's
      marker while any of its people are idle (free for work, as assembly counts them:
      `Hub.idle`). Hovering the building adds a line under its name: "3 engineers idle", "1
      flight controller idle", "2 cosmonauts idle" (astronauts for NASA). Shot
      `outlook-hub-idle.png`; every hub shot with idle people shows the dots.
    - **Rocket requirements** (the user's request, 2026-10-10). A mission card says what its rockets must
      lift and which one does it: "Rocket: 4,700 kg to low Earth orbit, human-rated: R-7 (Vostok) can
      fly it." in green, or "...: needs at least Atlas (not open yet)." in red ("needs a bigger VAB for
      medium rockets" when the class isn't built yet; launches alike are said once, "(both launches)").
      A program card adds "Rockets Redstone to Atlas": the smallest rocket its easiest and hardest
      missions need, green when open rockets fly every mission, amber some, red none, with each
      mission's line in the tooltip. *Unconfirmed default:* "the smallest rocket" is the agency's
      rocket that can fly it in the lowest size class, then the cheapest launch (`Outlook.leastRocket`),
      which follows the rocket progression closely where the pack's own order doesn't. The readiness
      row's red Rocket check names it too.
    - **Ambitions at stake when assembling** (the user's request, 2026-10-10). A warning on the assembly
      screen names anyone free whose stated ambition this team would pass over ("Fyodor Gusev is free and
      wants to be on the consoles for a crewed orbital flight. If the flight goes well without them,
      that's a pass-over: -5 morale, -5 loyalty, and less patience."), and the slot picker marks a
      candidate this seat would give their ambition ("Wants this: ..."). `Careers.ambitionsAtStake`
      follows the season pass: crew ambitions by the crew, console ambitions by the ground team, CAPCOM
      aboard for "Be part of a first"; people busy elsewhere aren't counted; the two "first" ambitions
      only while the mission has a goal the agency lacks, worded "if this flight reaches a goal first".
      Shots `assemble-passed-over.png`, `assemble-wants-this.png`.
    - **Random-event stories** (the user's request, 2026-10-10, `design.md` §2.7(d)): the turn
      report's news shows each event's story headline, its sentences, then the event's own line in
      green or red ("R&D results are 16 % higher for a while.", the struck person named). The Public
      Affairs Office's narrow news log shows the headline and the event's line, the story in a
      tooltip. An event without a story (from before) reads as before. Shot `report-news.png`.
    - **Retirement before the cap** (idea 7). A gold line under the caption: "Arkhipov retires in 7
      seasons, before the cap. The projection counts the team without them from then on." Morale
      resignations aren't predicted.
    - **Assign dialog.** A "Seasons to cap" column: the team now, then with the candidate in the
      next free slot ("~12 → ~8", green when faster), from the same projection.
    - **Program card staffing** (§4.6.3). Under the price: "Astronauts 0–3 · Controllers 8–9", as
      `Staffing` counts a single mission (astronauts are crew and CAPCOM). Each part is green when
      enough are free now for the smallest mission that needs the group, amber when the roster has
      enough but some are training or busy, red when the roster is too small. *Unconfirmed default:*
      the colour ignores missions that need nobody from the group, while the range still starts at 0
      for them; the tooltip says so.
    - **Mission readiness** (§4.6.5, open programs only). A row of checks under the "Free now" line:
      Goals (only when one is missing), Payload, Rocket, Astronauts, Controllers, Funds, each a green
      tick, an amber clock with "~N", or a red cross. Payload and rocket count as ready at 60 %
      (the advisor's mark); the rocket is the soonest-ready open rocket that can fly the launch, then
      the cheapest. People: a tick when enough are free now; an amber clock with no number when
      enough only by cutting someone's training short (*unconfirmed default*); "~N" for basic
      training's seasons left; a cross when too few, saying how many to hire, or, when people on a
      mission or PR duty would cover the gap, that they're free again when that ends (no wait
      can be told for them). Funds: the mission plus the chosen rockets. Below it: "Ready to launch
      now.", "Could launch now, if you cut someone's training short.", "Earliest launch: Winter
      1961, in 2 seasons, waiting on payload R&D." (both amber), or "Can't tell when yet: …" (red)
      with the first blocking check. The row's tooltip "Ready to fly?" has a sentence per check.
    - **Earliest launch on program details** (§4.6.4, open programs). A section after the components:
      the soonest of the program's missions open to the agency, its name and its checks, and how
      many missions there are.
    - **Training dates** (§4.7, §4.8, §4.9 rosters). Under the state badge: "Ready by Fall 1960" for
      basic and advanced training (expert and permanent training have no fixed end), and, in gold,
      "Retires in 2 seasons" within a year (`RETIREMENT_NOTICE_SEASONS` 4; astronauts and controllers
      add "(or when a flight ends)", since a flight protects them).
    - **Funds runway** (§4.10, and the open-program confirmation in §4.6.4). Under "Net per season":
      "At today's costs and prestige, funds run out in 9 seasons, in Spring 1962, the next funding
      review counted." or "…last at least ten years…", red within 4 seasons. Each season pays today's
      running costs and funding; the next review pays what today's prestige earns. The confirmation
      adds the program's upkeep and its new components' upkeep and pays the price first ("After its
      price, at today's costs…"). Its first line gives that same total, itemized when components
      add to it: "Its upkeep of $ 88.0 ($ 13.0 for the program, $ 75.0 for the Sputnik 1
      satellite) is charged every season…" (user, 2026-10-23: it named the program's alone, which
      read as a contradiction). Shot: `hq-details-sputnik-2-open.png` (an inactive satellite's
      upkeep coming back).
    - **Where to earn a goal** (§4.6.3, §4.6.4, idea 6). On a locked program's details, a line per
      missing goal: "Satellite in orbit: Earned by Project Mercury (open), Apollo (open), Explorer 1
      (available) and 8 more programs." Open programs first, then available; the agency's own
      programs only; a goal counts when a step achieves it or credits it through a cascade. When only
      locked programs earn it: "Only locked programs earn it: X needs Y first." The card's "Needs:"
      line has the same text in its tooltip.
    - **Help:** the R&D topic has a "Looking ahead" part (tokens `outlook.milestones`, `outlook.ready`).
    - Shots: `outlook-*.png` (R&D tooltip, the rocket board's retirement line, Vostok's program
      page and mission cards, the cosmonauts' roster, the program card's staffing, the locked
      Pioneer 4 page), and the changed `payload-rnd*.png`, `plan-configs.png`, `pao.png` and
      `hq-details-vostok-open.png` (the confirmation's runway).
26. **R&D decision** *(the user's idea, 2026-10-10, talked through the same day: `design.md` §5,
    Backlog, "R&D decisions"; specified, mocked up and built 2026-10-10; the look confirmed by the user).* A choice the game puts to the
    player about one component under R&D, rocket or payload, to fill the lulls between flights. Every
    decision has the same shape whatever its type: a story, two to four options, what each option
    shows in advance, a default, and an outcome the server works out the moment the player decides.

    **Life cycle.**
    - *Offered* when a turn resolves (rules in `design.md`; aimed at the lulls: near certain while no
      mission flies and the component is more than a season from 60 %). It belongs to one component,
      the one the player's soonest mission waits on where there is one. One open decision at a time.
    - *Open* until the next turn resolves. The player decides whenever they like during the turn.
    - *Decided:* an ordinary action, resolved by the server at once from a seed fixed to the decision
      (reopening can't reroll, and the action journal replays it). The answer comes back with the new
      state: the outcome shows in place and the bar and season marks update.
    - *Lapsed:* not decided when the turn resolves, so the default applies then, and the next report
      says so. After a lapse no new decision comes until the player acts again.
    - *Recorded:* the next turn report's news carries one line for it, decided or lapsed.

    **Where the player meets it.**
    - **Turn report:** a full-width Decision card above the columns, gold left accent: eyebrow
      "DECISION · <COMPONENT>", the headline, the story's first sentence, "If you don't decide: <the
      default>." and a primary button "Decide on the R&D board". The at-a-glance strip's Events cell
      becomes "Events · decision" when one waits (or a sixth cell "Decision: 1 waiting", whichever the
      mockup prefers).
    - **Hub:** a gold dot with a white ring on the upper left of the VAB's (rocket) or SET Centre's
      (payload) marker, opposite the red idle-staff dot. Hover adds a line under the building's name:
      "A decision waiting: Atlas".
    - **R&D board (component 10), where the player decides:** a banner inside the component's card,
      between the reliability bar's caption and the team chemistry row. The board scrolls to that card
      when opened from the report or the hub. The mission plan's read-only R&D tab shows the banner
      without its buttons, with a link to the board.

    **The banner, open.** Gold-tinted background, gold border.
    - Eyebrow: "DECISION · <TYPE LABEL>" and, right-aligned, "Until the turn resolves · <countdown>".
    - Headline (heading small) and the story (body small, one to three sentences).
    - Type-specific panel where the type has one (clues, a person picker; see the types).
    - The options as a vertical list of option rows: a label, and under it what it shows in advance in
      caption text ("$ 1,200 · 70 % chance of about +4 reliability"), or nothing for an option that
      shows nothing. The default row carries a small "If you don't decide" tag.
    - Concept chips (component 19) for the explainers the decision draws on, under the options.
    - Clicking an option opens the confirmation. Hovering an option with a preview (trade-offs over
      time) shows it on the bar above while hovered.

    **The confirmation.** A small dialog: the option's label, everything it shows in advance, its cost
    if any against the funds, any person it takes, and the buttons "Decide" (primary) and "Back". The
    default option can be chosen too; it then applies at once instead of waiting.

    **The banner, decided.** It turns into the outcome: a tone mark (green, red, or navy for neutral),
    the outcome's headline and sentence, and the effects in the event lines' style ("Reliability +4.2",
    "Funds - $ 1,200", "Kozlov: rapport with Orlova grew"). The bar shows the change with the projected
    segment and marks redrawn. A story arc's outcome adds a navy "To be continued" tag. The outcome stays
    on the card until the turn resolves, with a close mark that folds it to one line.

    **Effects are sized in seasons of R&D, not raw points** (*unconfirmed default*): a boost or a setback
    is the component's expected gain over a number of seasons (`ResearchPlanning`), so a decision weighs
    the same on an early satellite and on Saturn V. Costs are a share of a season's funding.

    **The ten types**, each with an example. The type is a field in the content file; one resolver per
    type.
    1. **Resource for a chance.** Options with their costs and chances shown. *"Pay for a static test
       firing?"* Pay $ 1,200: 70 % chance of about two seasons' R&D gain, 30 % about half a season's.
       Skip it (default): nothing.
    2. **Knowledge.** One right answer, the rest wrong, and "I don't know" as the default; nothing
       shown in advance. A right answer gives a large boost (about three seasons' gain), a wrong one a
       setback (about one season's), "I don't know" nothing. *"Balloon tanks on the pad"*: the night
       crew let the tank pressure of the rocket drop. What happens? It crumples under its own weight
       (right) / Nothing, the steel holds / The fuel boils off / I don't know. Concept chip: Balloon tanks.
       Reading the explainer is allowed; that's the point.
    3. **Ambiguous.** Two to four options whose outcomes can't be told from their words; people themes;
       the team's traits (revealed or not) shape the result. *"The night shift wants a say"*: Hear them
       out / Back the foreman / Promise a bonus ($ 300). Outcomes range from a morale lift and a small
       boost to a resignation threat.
    4. **Who oversees it.** A person picker in the banner: the idle engineers, best first in the
       component's skill, each with the chance of success their skill gives ("Kozlov · Rockets 72 · 78 %
       clean run"); engineers in training are listed muted with "Pull out of training to choose"
       (a link that opens the pull-out confirmation, partial credit applies). Default: "Nobody, the
       test is put off". *"Who will oversee the test firing?"*
    5. **Trade-offs over time.** Options that change R&D for several seasons; hovering one previews its
       season marks on the bar. *"Freeze the design?"* Freeze: R&D 30 % faster for four seasons, cap
       3 points lower. Keep refining (default): nothing changes.
    6. **Diagnosis.** A clue panel above the options: two to four short clues, each a line with a small
       icon (a telemetry trace, a note, an inspection photo caption). Options are the suspected causes;
       one is right. Right: a fair boost; wrong: a season of R&D lost chasing it; "Wait for more data"
       (default): nothing. *"Shutdown at second 41"*: pressure fell two seconds before cut-off;
       turbopump vibration normal; the igniter fired twice. Concept chips where they help.
    7. **Opportunity with strings.** An offer for this turn only, its catch stated. *"The air force
       lends its altitude chamber"*: Accept: about three seasons' gain at once, one engineer from the
       team seconded for two seasons (the player picks who, with the person picker). Decline (default).
    8. **Pairing.** Two person pickers ("Who works with whom?") over the component's team and the idle
       engineers, with relationship marks (component 22) between the picked pair. Outcome scaled by
       their rapport and traits; the pairing builds rapport or starts a clash, shown in the outcome.
       Default: "Leave the team as it is".
    9. **Intelligence tip.** Only while the Intelligence Office has a level. Shows the tip's
       reliability in words from that level ("Our sources are usually right about this"). *"The
       rival's engine runs cooler"*: Adopt their approach: a large boost if the tip is right, a setback
       if not. Ignore it (default).
    10. **Story arc.** Any type's outcome can open a follow-up in a later quiet season. The follow-up's
        eyebrow reads "FOLLOW-UP · <COMPONENT>" and its story recalls the first decision ("The test
        firing in Spring went clean. The engineers want to try it at full thrust."). Higher stakes both
        ways. An arc ends after two or three steps, or when a step goes badly.

    11. **Hands-on** *(the user's idea, 2026-10-11, `design.md` §5, Backlog; mocked up 2026-10-11)*.
        The banner's options are **Run it yourself** and **Leave it to the team** (the default,
        nothing happens); its eyebrow reads "DECISION · HANDS-ON". Under the story, one caption line:
        "Up to 4 rounds of 3 commands · objective 3 firings, 4 measurements · bonus 6 and 7", and the
        odds the team gives ("Kozlov leads: 17 % of steps fail, 15 % go exceptionally well"). Run it
        yourself asks once ("Once started, the result counts, finished or not") and opens the
        **Hands-on screen**:
        - **Header:** breadcrumb "... › R&D › <component> › Hands-on", eyebrow "HANDS-ON · <COMPONENT>",
          the title (the decision's headline), and right of it "Round 2 of 4".
        - **Objective bar** across the top, in a card: the objective as chips (checkbox, the resource's
          icon, held amount ▸ target), the bonus as a second row ("Bonus: about 3 seasons' R&D"), and
          the other resources' counters (shifts, spare parts). The checkboxes tick live.
        - **Command table** on the left: rows grouped by the resource each group makes, a group tile at
          each group's left (the resource's icon on its colour), columns Command, In, ›, Out; amounts as
          the resource's icon with a number above 1. A row the player can't pay for now is muted with a
          red left edge and doesn't click; a click queues it. Overtime sits last, alone.
        - **Queue panel** on the right: "Commands 1 / 3" with Undo and Reset, three numbered slots (a
          queued command's name and group tile), the odds line, and the footer "Round 2 of 4" with the
          commit-coloured **Confirm commands** button (the screen's one big action). Confirming with
          empty slots is allowed.
        - **Execution:** the slots resolve in order, each turning to its outcome: a red "Failed −1", a
          navy "Done", a green "Exceptional +1", the amounts then counted into the bar. On a fail with
          shifts left, the slot asks: **Call in an expert (1 shift)** or **Accept**; execution waits.
          With no shifts the fail just applies.
        - **Result panel** when the rounds run out or the bonus is met: "Bonus success", "Success" or
          "Not enough", the final amounts against the targets, the effect in seasons of R&D, and
          "Back to the R&D board", where the banner shows the outcome as the other types do.
        - **Colours:** each resource gets a colour from the existing tokens (shifts gold, firings
          danger-red, measurements navy, spare parts success-green), no new tokens; four new icons
          (shift, firing, measurement, spare part) for the UI Asset Sheet.
        - Leaving the screen mid-puzzle is allowed: the state is the server's, the banner reads
          "Hands-on in progress · round 2 of 4 · Continue", and an unfinished puzzle settles as it
          stands when the turn resolves.
        - **Mocked up (2026-10-11)** on the canvas `Launch Window — Hands-on` (the Artifact tool's list finds
          it), a Soviet agency in 1960 with the R-7 (Vostok) and Gleb Arkhipov leading: the banner open, the
          confirmation, planning in round 1, executing in round 2 (a fail asking for an expert, and an
          exceptional result), the three result panels, the banner afterwards and in progress, and the four
          icons. Choices, all **unconfirmed**: the caption line sits in a white panel under the story; "Run it
          yourself" says what the objective and bonus are worth. The confirmation adds "never a loss". The
          header's right side is a "Round 1 of 4" chip. The objective chips show a "+2 on execution" line for
          queued outputs; the counters show held amounts after queued inputs ("6 at the start · 2 paid"). A
          row the player can't pay for says what it is short of ("Short of 1 measurement"). The counter reads
          "Commands 1 / 3" with one queued. Slot badges: Queued, Done, Failed −1, Exceptional +1, Waiting, and
          "Done · no roll" for Overtime; a failed slot says what it makes instead and holds Accept and "Call in
          an expert (1 shift)". During execution the table greys and Confirm reads "Executing…"; after a round,
          "Round 2 done" and "Plan round 3" (no stop button: leaving is the back button). The odds line has a
          three-segment bar. The result panel has a 4 px top accent (gold, success, danger), a table of held
          against objective and bonus, the effect with its reliability, and "Back to the R&D board". Afterwards
          the banner uses the decision outcome ("Decided · Run it yourself"); in progress, a gold row with the
          round, the objective so far and Continue. Icons `res_shift` (a hard hat), `res_firing` (a nozzle and
          plume), `res_measurement` (a gauge), `res_spare_part` (a hex nut), on the UI Asset Sheet; small icons
          on white use gold-text for shifts. No new colour tokens.
        - **Built (2026-10-11)** in `screen.HandsOnScreen` with `view.HandsOnView`, the banner's caption,
          confirmation and in-progress state in `DecisionBanner`, the report's Decision card ("Hands-on in
          progress ... Continue the campaign") and the hub hover ("Hands-on in progress: R-7 (Vostok)").
          Icons in `make_ui_icons.py`. As built, against the mockup: the objective chip reads "2 › 3" (the
          font has no "▸"); a round's results stay up with "Round 1 done" and **Plan round 2** until the
          player moves on, and the outcome shows at once (no animation yet); a command row the player can queue takes a light navy tint on hover (2026-10-25, the user: the rows didn't read as buttons), and the output the roll moves is underlined in its colour, the column headed plain "OUT" with a tooltip (it read "OUT · MAIN FIRST", which didn't say what it meant); during execution Undo and
          Reset grey out; the payload's campaign (*A week in the vacuum chamber*, *Chamber runs*) reuses the
          firing icon for its runs until it has its own. Shots `handson-*.png` (`ScreenSnapshot
          -Dshots=handson`).
        - **Playing a level on its own (the user's idea, 2026-10-25):** dev-tools' `HandsOnPlayground`
          (`start_handson.cmd`, or `mvn -pl dev-tools compile exec:exec@handson`) plays any hands-on level in
          this screen without a server or a game to wait in for one: a list of the levels, then the component
          it runs on (both for one that fits any), the lead's skill (0 to 100 in steps of 20, the fail chance
          shown) and a seed. Steps are applied by the server's rules (`RdDecisions.handsOn`), so the rolls are
          the game's. The levels are read from `assets/content/standard/decisions.json` before every run, so an edited
          level plays without a restart; on the puzzle, R plays the same seed again, N a new one, Esc goes
          back to the list. One engineer on the team, so never the good-chemistry odds. `-Dhandson.smoke=<folder>` (run with
          `java` from `assets/`) plays a winning random run through the screen and saves the chooser, six
          frames of the first round resolving up to a fail, three after it's accepted, the round's results, the
          result and the chooser again; silent, though the sounds load and play.
        - **A round resolves slot by slot (the user's idea, 2026-10-25):** after Confirm, or an answer to a fail,
          the round's commands show one at a time instead of all at once. The running slot has a navy border and
          a "Running…" badge; a needle over the odds bar (now in a 24 px strip) rises from the left to a peak,
          then rings down onto the slot's roll with fading noise, overshooting into the neighbouring segments on
          the way (about 4 s, slowed at the user's word the same day: a 0.9 s rise, then a 0.75 Hz swing damped
          over 3.1 s), and settles in the roll's colour (danger, navy, success) with a light outline.
          The badge then shows, the objective and counters count that slot in, and 0.9 s later the next slot runs
          (a slot that doesn't roll, Overtime, takes 0.3 s with no needle). The foot reads "Round 2 running" with
          **Skip · Space**, which shows the rest at once; any step taken skips it too. A fail that waits for an
          answer asks once its needle has settled; the result panel waits for the last slot. The rules have
          already rolled and the client never sees the drawn number, only the roll, so where in its segment the
          needle settles is made up from the round and the slot. **Sound** (the user's Pixabay finds, the same day):
          a tense loop under the round from Confirm (music volume), fading out over 1.2 s after the last slot or
          on Skip, and a sting as each needle settles, failure, success (a normal result) or exceptional
          (effects volume); Overtime has none. Files in `assets/audio/handson/`, published by
          `assets-raw/audio/make_handson_sounds.py`. Client only, in `HandsOnScreen`; unconfirmed: the timings,
          the motion and the levels.

    **Help:** a "Decisions" topic, linked from the banner's eyebrow.

    *Mocked up (2026-10-10, canvas `Launch Window — R&D decisions`; confirmed by the user the same day):* the board with
    the type 1 banner open, the other nine types as close-ups, the confirmation, outcomes good, bad
    and folded, the report and the hub dot. Choices the spec left open, each in a canvas note:
    - **The banner:** eyebrow row (type label, a help mark for the "Decisions" topic, the countdown
      right-aligned), headline, story, the type's panel, a muted stakes line where all options share
      the stakes, then option rows (white, full width, a chevron; hover: a navy border and a navy bar
      on the left; the default's outlined "If you don't decide" tag before the chevron), then "How it
      works:" with concept chips. A follow-up's eyebrow is "FOLLOW-UP · ATLAS" with no type label,
      and a recall row shows the first outcome and "Step 2 of up to 3".
    - **Per type:** Ambiguous defaults to "Back the foreman" (the spec named none). In "Who oversees
      it" the people are the options (radio rows: name, state, skill, chance); picking one opens the
      confirmation, and the default stays an option row below. Opportunity: the catch in bold, "If
      you accept, who goes?" with each person's R&D lost a season, the Accept row naming who, and a
      gold "This turn only" tag. Pairing: the first picker holds a team member; the second opens a
      dropdown (the team, then idle engineers) with each one's relationship to the first. Trade-offs:
      hovering an option outlines the bar in dashed gold, shows the new counts with the old struck
      through and the old cap as a dashed gold marker, and adds a gold "Preview" tag to the caption.
      Diagnosis: a clue list, each line with an icon and a small kind label.
    - **The confirmation:** the chances as rows, the cost, funds before and after, "Decided at once".
    - **The outcome:** "DECIDED · <option>", headline, sentence, effect lines in green and red, and
      "Stays here until the turn resolves."; the bar shows a green gained (or red hatched lost)
      segment until then. The close mark folds it to one line with a Show link.
    - **Report:** a seventh glance cell, "Decision · 1 waiting" with the component, since the Events
      cell becomes "Funding review" in a review season; the card's button with the countdown under it;
      news lines for a lapsed and a decided decision.
    - **Hub:** the decision dot copies the idle dot (6 art px, 1.5 px white ring) in gold, upper left;
      its hover line stays white like the idle line, after a small gold dot (gold text read poorly on
      the bright art), and comes after the idle line.
    - **New for `Theme.java` and the UI Asset Sheet:** no colour tokens (tints of gold, success and
      danger); icons `clue_trace`, `clue_note` and `clue_inspect` (24 px grid, 2 px round strokes);
      component variants DecisionBanner (open, decided, folded), OptionRow, PersonPicker, ClueList,
      the bar's preview and gained/lost segments, and a gold glance cell.
    **Shots:** one banner per type open, the confirmation, an outcome good and bad, the report card,
    the hub dot, a lapsed decision's report line.

    **Unconfirmed defaults, to tune with the playout bots and the play test:** the chances (about 85 to
    90 % in a lull, low otherwise), effect sizes in seasons of R&D, costs as a share of a season's
    funding, two or three events per type for the first build.

    *As built (2026-10-10, from the canvas `Launch Window — R&D decisions`; the mockup's choices adopted):*
    - **Rules** (`rules.RdDecisions`, `RdDecisionsTest`): a new `DECISIONS` phase after Careers settles last
      turn's decision (the default if still open, which sets `decisionPaused` until any action takes
      effect) and may offer one. A lull is nothing in flight and either a component with engineers more
      than a season from 60 % or `WAITING_SEASONS` (2) seasons and more since the last flight; then the
      chance is `LULL_CHANCE` (0.85), else 0.05 plus 0.02 a quiet season up to 0.35, halved while a mission
      flies or right after a decision. The decision goes on the component furthest from 60 %; a story
      arc's next step waits for a lull. Offer and outcome draw from their own salted seeds: the playouts
      without decisions are unchanged to the digit. The seed never reaches the client
      (`Personality.forClient`). `DecideAction` (Kryo-registered last) refuses `NO_TEAM` when nobody works
      on the component (effects are sized from the team), and a decision whose component goes inactive
      during the turn is dropped silently.
    - **Odds:** an overseer's chance is 0.25 + 0.007 x skill (0.3 to 0.95); a pair's 0.5 + rapport / 200
      (0.15 to 0.9); an intelligence tip's 0.35 + 0.15 x the office's level (to 0.9); a trait an outcome
      names (`favouredBy`) doubles its weight when anyone involved has it, revealed or not. Reliability
      effects are seasons of `ResearchPlanning.expectedSeasonGain`, at least 1 point a season.
    - **Deferred:** the trade-off's lower cap (the mockup's "cap 3 points lower"): the trade-offs use an
      R&D factor on the component for some seasons (`OwnedMissionComponent.rdFactor`, read by the season
      pass and the projection) and a reliability change now instead.
    - **Content:** each pack's `decisions.json` (`assets/content/standard/` and `early-era/`, identical, hashed
      and frozen since 2026-10-25), 23 decisions in the first build: 6 resource (two arcs, the
      test firings of three steps and the vacuum chamber of two), 3 knowledge (balloon tanks, the odds
      multiply, blunt heat shields, each linking its explainer), 2 of every other type. Outside the hashed
      pack; ids are kept in saved state and reports, never reused. `DecisionsTest` holds it to its rules.
    - **The pools grown (2026-10-25, the user's to-do):** 185 decisions, aiming at about 20 first steps a
      type, fewer where more would repeat themselves. First steps a rocket / a payload can draw: resource
      20 (13 / 13; three more arcs: the spare test unit loaded to failure, thermal cycling to a hundred
      cycles, a balloon flight to two days aloft), knowledge 24 (14 / 16, every question linking its
      explainer), ambiguous 21 (all), oversee 20 (16 / 14), trade-off 19 (16 / 16), diagnosis 20 (12 / 12,
      every wrong suspect ruled out by a clue), opportunity 19 (16 / 17), pairing 12 (all), intelligence 17
      (12 / 11), hands-on 7 (4 / 4). Since the picker draws evenly among a component's usable first steps,
      the types keep roughly their old shares; hands-on comes about half as often as before. Choices, all
      **unconfirmed**: effect sizes stay in the first build's band (about +3 seasons at best, -1 to -1.5 at
      worst, costs 0.05 to 0.45 of a season's funding); no negative funds effect (funds aren't checked
      against it); every option that takes an overseer is the decision's only one (the banner lists the
      engineers as rows without the option's label); trade-offs may slow R&D (a factor under 1, "R&D 20 %
      slower"); outcomes favoured by a trait use only engineers' traits. The five new hands-on levels keep
      the first level's shape (shifts pay, Overtime gives one, an expert costs one, 4 rounds of 3, which
      help quotes) and its four icons, the test resource reusing `res_firing`, but differ in what's scarce:
      cold-flow runs (measurements scarce), igniter tests (spare igniters both spent and part of the
      objective), the shake table (start with parts), the sun lamps (a three-part objective), all the
      systems at once (start with readings). A replanning bot at 20 % fails reaches the bonus 37 to 72 % of
      the time on them (the first two levels: 68 %). `DecisionsTest` now wants at least 4 first steps a
      type for rockets and as many for payloads. `-Dshots=decisions` renders every banner
      (`pool-<id>.png`) and each campaign's planning screen. **Measured** (`ContentPackPlayouts standard side
      decisions`, 200 Moon races, the same day): the Moon race averages 121.5 seasons with the grown pools,
      115.9 with the first build's and 130.3 with none, so decisions now shorten it by about 9 seasons, not
      14: more of the new outcomes move morale, money or the pace of R&D instead of reliability, and some slow
      R&D. About 74 decisions a game (70 before); hands-on campaigns 190 bonus, 65 success, no failure.
    - **Client:** `DecisionBanner` in the card (`RndCards`), the board putting that card first; the
      trade-off preview is a gold line under each option ("With this: 60 % in ~3 seasons (now ~2) · the cap
      in ~25 (now ~32)") rather than a hover state on the bar; who-oversees-it lists the idle engineers as
      the options with their chance, engineers in advanced or expert training muted with a pull-out link;
      the seconded and pair pickers sit in the confirmation (900 px wide for a pair). The fold is
      client-only and forgotten on restart. The report's seventh glance cell and Decision card show on the
      latest report only; its news gets a line for last turn's decision (neutral when nothing changed).
      Hub: the gold dot upper left on the VAB (rocket) or the SET Centre (payload). Icons `clue_trace`,
      `clue_note`, `clue_inspect` (the canvas's own drawings) in `make_ui_icons.py`, on the UI Asset Sheet
      with a clue list and the hub's decision dot in context (2026-10-10). Help topic
      "decisions" on the R&D boards.
    - **Playouts** (`ContentPackPlayouts standard side decisions`, the bot taking a random option): about
      80 decisions a game, offered in 65 % of the seasons without a launch and 38 % of those with one,
      covering 60 % of the median game's longest quiet stretch. Being mostly net positive, they shorten
      the Moon race from 151 to 138 seasons: a balance signal for the play test.
    - Shots `decision-*.png`.

27. **Contract marker** *(the user's idea, 2026-10-11, for `design.md` §2.11; mocked up 2026-10-11)*. Shows where commercial contracts come from: which flights open a contract line, and which
    flight a contract flies. One small icon of our own (a signed document with a seal; new, for the
    UI Asset Sheet), in two states:
    - **Unlock flight** (outline icon) on every configuration card in a program's unlock set, until
      the line is unlocked. Tooltip: "Opens commercial contracts for Earth-orbit satellites once every
      marked flight has succeeded: 1 of 2 flown", listing the marked flights with a tick on those
      flown. A flown one shows its tick on the icon too.
    - **Contract flight** (filled icon, gold) on the configuration contracts fly, once the line is
      unlocked. Tooltip: "Contracts in Earth-orbit satellites fly this configuration. Open contracts
      are on the board in Headquarters."
    - **Program cards and program details** (§4.6.3, §4.6.4) get a line under the components:
      "Commercial line: Earth-orbit satellites · 1 of 2 marked flights flown", or "· unlocked", or
      nothing for a program without an unlock set.
    - **A contract's own mission** wears a "Contract · [client]" badge (filled icon) on its plan, its
      Assemble screen, Mission Control's lists and the replay. Where a mission shows prestige, it shows
      the fee instead: "Fee 2,340 on success · insurer refunds the launch cost on failure", plus the
      launch window ("Launch by Autumn 1963"). The first launch of the contract configuration while a
      contract is active *is* the contract flight: Assemble shows the badge and "This launch flies
      your contract for [client]" above the confirm button, with no toggle.
    - The numbers in every tooltip come from the rules' constants (G7), never typed in.
    - **Mocked up (2026-10-11)** on the canvas `Launch Window — Contracts` (artboards 8 and 8b), all
      **unconfirmed**: the icon is ours (24 px grid, 2 px round strokes: a page with a folded corner,
      two lines, a signature, a round seal with two ribbons), as `contract_unlock` (outline, navy) and
      `contract_flight` (filled, used in gold); both are on the UI Asset Sheet, whose SVG source goes into
      `make_ui_icons.py` (the filled one knocks its lines out with a mask). It sits right of the
      configuration's name at 20 px; a flown unlock flight adds an 11 px green tick badge, bottom
      right. The unlock tooltip lists the set with a tick or an empty circle per flight. The program
      card line uses the outline icon until unlocked, then the gold one with "unlocked" in green. The
      badge is gold with navy text and icon. On Assemble the badge sits right of the title, the fee
      box (gold-tinted) takes the prestige's place under "Instead of prestige" with the window under
      it, and the gold line "This launch flies your contract for [client]" sits above the launch row.
    - **Built (2026-10-11)** in `screen.ContractMarker` (cards: `ConfigCards`, `BodyProgramsScreen`,
      `ProgramDetailsScreen`) and `AssembleMissionScreen`. Icons `contract_unlock` and `contract_flight`
      in `make_ui_icons.py` (the filled one as its own SVG with the mask). The unlock tooltip lists the
      set as "Flown · ..." and "To fly · ..." (the font has no tick or circle glyphs). Assemble shows the
      gold box above the launch row, after the price, and the confirmation repeats the line. The
      mission lists in Mission Control don't wear the badge yet. Shots `contracts-configs-*.png`,
      `contracts-program.png`, `contracts-assemble.png`.

28. **Statement dialog** *(the user's idea, 2026-10-20, for §4.18 "The world reacts"; specified, mocked up
    and built 2026-10-22, `screen.StatementDialog`)*. Gives the press one statement about a rival's First, printed the
    next turn in a follow-up page.
    - **Opens from** the strip under a rival's First page (§4.18), from a secondary button left of the
      primary one: "Give a statement" while the player may give one (below), "Your statement: Polite ·
      Change" once given (the stance in the player's words). Not shown when the player made the First,
      joined after it was printed, or the turn after it has resolved. A reopened First page whose
      follow-up is printed shows "Read the reactions" there instead.
    - **A reminder on leaving** (user, 2026-10-23, `FrontPageScreen.remindStatement`, shot `reactions-reminder.png`): the
      strip's button is easy to miss under the paper, so leaving such a page unanswered (Continue, Next front page or
      Close, from either sheet) asks first: eyebrow "A rival's First", title "Give a statement?", "You have a unique
      chance to answer <agency>'s first <First>. Every rival's statement is printed together in next season's paper,
      and this season is the only one to give yours." "Give a statement" (primary) opens this dialog over the page;
      "Decline to comment" (secondary) moves on as the strip's button would, nothing sent. Once a page per visit.    - **About 720 px wide,** a modal over the page and its scrim, in the design system's dialog style.
      Title "Give a statement"; subtitle "On the <agency>'s <First> · printed next season".
    - **Five stance rows,** one choice, in the personnel dialogs' choice-row pattern (radio mark, bold
      label, one muted line):
      1. *Enthusiastic congratulations*: Warm, generous praise.
      2. *Polite congratulations*: Correct, and brief.
      3. *"We'll be next"*: Congratulate them, then promise to catch up.
      4. *Downplay*: It proves less than it seems.
      5. *No comment*: A pointed refusal to say anything. (Staying silent is printed as "did not
         respond".)
    - **Quote,** optional: a multi-line field, at most 140 characters (`FrontPage.Statement.MAX_QUOTE_LENGTH`),
      an "n / 140" counter at its foot, placeholder "Add a quote, printed word for word (optional)".
      Typing is filtered to `FrontPage.Statement.ALLOWED_CHARACTERS` (letters, digits, everyday
      punctuation; no braces or angle brackets). Disabled, greyed and emptied for No comment. Inline
      error under it: "Only letters, numbers and common punctuation." / "At most 140 characters."
    - **Preview,** "IT MIGHT READ": a scrap of newsprint (warm off-white, `Theme.INK`, Libre Caslon
      italic) with one sentence as the paper might print it, from the stance's first line in
      `front-page-reactions.json` with the player's names and quote. It follows the stance and quote.
    - **Footnote** (muted, small): "Every rival's statement is printed together next season. You can
      change or withdraw yours until the turn resolves."
    - **Buttons,** right: Withdraw (only when one was given; quiet style), Cancel, and "Submit statement"
      (primary, disabled until a stance is picked). A refusal from the server shows as a toast through
      `client.ui.Refusals`.
    - **States to mock up:** fresh; "We'll be next" with a quote and its preview; No comment (quote
      disabled); the quote error; editing a given statement (Withdraw shown); the strip's three states.
    - **Mocked up (2026-10-22)** on the canvas `Launch Window — The World Reacts` (the Artifact tool's list
      finds it): the Kestrel Space Directorate's first human in space, seen by the Northern Rocket Bureau, with the
      strip's states and the dialog's five. Choices, all **unconfirmed**:
      - **The strip's button:** a ghost button on the page (transparent, a 1 px `ON_PAGE` line, `ON_PAGE_STRONG`
        text, the inactive NavTab's look) left of the white page button, after "1 of 2". Given: "✓ Your statement:
        Polite · Change", with "Change" underlined.
      - **The dialog:** the design system's Dialog at 720 px over a second scrim (navy at 55 %). The stances are
        real radios in a fieldset ("Your stance"). The picked row has a 1.5 px navy line and a 5 % navy tint. The
        quote box is 76 px with its counter inside at the foot on the right. "It might read" is a scrap of
        `paper.jpg`, tilted 0.4°, its label in Old Standard Bold 13 px with "The Daily Zenith, next season" beside
        it, the text in Libre Caslon italic 15/21. With nothing picked it says "Pick a stance to see how it
        might read."
      - **The error:** the box's line and counter turn `DANGER`, Submit is disabled while it stands, and the
        preview drops the quote for the stance's unquoted line.
      - **No comment:** the box is emptied and greyed. Picking another stance turns it back on, empty.
      - **Editing:** "✓ Given this season: Polite congratulations" in the head (caption, `SUCCESS`). Withdraw
        (destructive outline) sits at the left end of the button row, apart from Cancel and Submit. It
        withdraws at once, with no confirmation.
      - **New for `Theme`:** a page ghost button (`UiKit.pageGhostButton`), a shared stance row, a multi-line
        field with a counter, the newsprint scrap, and the text styles `NEWS_QUOTE` (Caslon italic 15/21),
        `NEWS_LABEL_SM` (Old Standard Bold 13/16) and `NEWS_EAR_TWO` (Old Standard Bold 25/28, below). No new
        colours or icons.
    - **Built as mocked up (2026-10-22):** the strip's ghost button (`UiKit.pageGhostButton`, a check icon
      before "Your statement: ... · Change"); the rows a navy line and a 5 % navy tint when picked (the
      mockup's, not the option card's gold); the quote a `TextArea` (`UiKit.textArea`) filtered to the allowed
      characters, its counter inside at the foot; the preview a piece of `paper.jpg` with `NEWS_LABEL_SM` and
      `NEWS_QUOTE`, the stance's first line with the player's names, the quote left out while it has an error;
      Submit disabled until a stance is picked and while the quote has an error; Withdraw at the left,
      destructive, without confirmation. The picked stance survives a change of stance, the quote not when
      No comment empties it. A history note after the title: how Eisenhower and Kennedy answered the
      Soviets' Firsts (2026-10-22). Shots `-Dshots=reactions` (`reactions-*.png`).

29. **Eulogy dialog** *(the user's idea, 2026-10-22, for §4.18.3 "In memory"; specified, mocked up and built
    2026-10-22, `StatementDialog.eulogy` and `.condolences`)*. The losing agency's statement on its lost crew, printed the next turn on the memorial page.
    - **Opens from** the card "The press is waiting for your statement" in the report of the turn after the loss
      and on the hub (an advisor-style card), and from the strip of the report's People line about the loss.
      After a statement is given: "Your statement: Who they were · Change". Gone once the turn resolves.
    - **About 720 px wide,** a modal in the dialog style, quieter than the statement dialog: no gold, navy
      and ink only. Title "A statement on the loss of <mission>"; subtitle "Printed in tomorrow's paper,
      <season>". A history note after the title (Kranz, §4.18.3).
    - **The crew,** at the top: their portraits in a row (greyscale, 64 px), name and age under each.
    - **Four tone rows,** one choice, in component 28's choice-row pattern (radio mark, bold label, a muted line):
      1. *Their courage*: They knew the risks, and flew.
      2. *Who they were*: The people behind the names.
      3. *We will go on*: We fly again, and finish what they started.
      4. *We will find out why*: We will learn what went wrong, and fix it.
    - **Quote,** optional, as component 28's (140 characters, filtered, counter, error), placeholder "Add words of
      your own, printed as you write them (optional)".
    - **Preview,** "IT MIGHT READ": a scrap of newsprint with the tone's first statement line and the names.
    - **Footnote:** "If you give no statement, a spokesman's short one is printed. You can change yours until the
      turn resolves."
    - **Buttons:** Cancel, "Give statement" (primary, disabled until a tone is picked). No Withdraw (changing is
      enough; unconfirmed).
    - **The rivals' condolence form** is component 28 with its own title ("Condolences to <agency>"), two rows
      (*Condolences*: Sympathy for the families and the agency. *A tribute*: Honour the crew and what they did.),
      the quote, Withdraw, and the footnote "Only what is sent is printed; silence is never reported."
    - **States to mock up:** fresh (three crew); a tone picked with a quote and its preview; a given statement
      being changed; the condolence form; the report card and the hub card.
    - **Mocked up (2026-10-22)** on the canvas `Launch Window — In Memory` (the Artifact tool's list finds it), with
      §4.18.3's page. Choices, all **unconfirmed**:
      - **The frame:** component 28's Dialog at 720 px, quieter: navy and ink only, no gold. The newsprint preview lies
        flat (no tilt), and a given or sent status is in `INK_MUTED` with the check icon (component 28's is `SUCCESS`).
        The history note follows the title's last word (`HistoryNote`, its card opening downward).
      - **The crew:** 64 px greyscale portraits with a hairline, the name in body-strong 12 and "Aged 38" in caption.
      - **The field** is labelled "Your words (optional)", not "Quote". "Pick a tone to see how it might read." stands
        in for the preview until a tone is picked.
      - **The condolence form:** "What you send" (Condolences, A tribute), the status "✓ Sent this season: A tribute",
        Withdraw at the left (component 28's destructive outline; a navy outline would be quieter), Cancel and
        "Send".
      - **The report card:** a full-width primary Card (1.5 px navy) under the summary strip: the eyebrow "The
        press", the title, 40 px greyscale portraits with one line, "If you give none, a spokesman's short one is
        printed." and "Give a statement". **The hub's card:** the same at 420 px, top left (the advisor's place),
        with "Until the turn resolves". **The rival's line:** first in the News card, "<Agency> has lost the crew
        of <mission>" with one more line and "Send condolences" (secondary), no eyebrow and no colour.

---


## 3. Screen flow *(decided)*

```
Splash ─► Connect ─► Join game (skipped if already in the game) ─► [missions flown: "Watch them first?" ─► each mission's replay] ─► front pages ─► Turn report (if a turn resolved since last visit) ─► Space Complex hub

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
│   ├─ Contracts (§4.17) ─► Mission configuration (the contract flight)
│   └─ (building panel)
├─ Public Affairs Office
├─ Intelligence Office (§4.15)
│   ├─ Overview (level dial, actions, costs, counter-intelligence)
│   ├─ Operations (agency assessment, personnel file)
│   └─ Folders (one per rival)
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

### 4.0 Splash
**Purpose:** the animated logo, once, at every start of the client (the user's idea, 2026-10-12).
A borderless, non-resizable square window, 30 % of the monitor's height (confirmed, user, 2026-10-12), centred, plays
`assets/ui/splash/splash.webm` (4 s, 800 × 800) over `navy`, with Armstrong's "one small step"
(`assets/audio/small-step.mp3`, NASA, 2.7 s) from 0.8 s, as the rocket starts to climb (the user's idea,
2026-10-12), when sound is on, scaled to fit with linear
filtering. When it ends, the game's own window opens (title, icon and size as before) and the
splash window closes. **Actions:** any key or click skips it. **States:** a video that can't
play (a missing native, a missing file) or never reports its end (after 8 s) goes straight to
the game's window, logged as `[Splash]`.

**Implemented (2026-10-12)** in the launcher, not in core: `lwjgl3.SplashListener` is the first
window of the `Lwjgl3Application`, and opens the game with `newWindow(new LaunchWindowGame(), ...)`,
so the game's fonts are made at its own window's size from the first frame. It plays through
gdx-video 1.3.4, which on desktop plays WebM and AV1, never MP4. The clip is made in EffectCraft
from `logo.svg`'s own shapes: `python assets-raw/splash/make_splash.py` (the steps are in its
docstring). `ScreenSnapshot` builds `LaunchWindowGame` directly and never shows the splash.

### 4.1 Connect
**Purpose:** connect and authenticate. One form: server (host:port), login,
password. There's no display name: other
players see you by your agency and director name, chosen per game (§4.2).
**States:** connecting;
wrong password; login has invalid characters (`[A-Za-z0-9_-]`, up to 32); client
and server versions don't match (show both versions); server unreachable; **no such account**
(2026-10-25, review.md 2.3; built straight in libGDX, unconfirmed): a login the server doesn't
know is never made into an account by itself, so a typo can't start a second, empty one. An info
notice above the card says "No account named *login* on this server. Check the spelling, or create
it with the password you typed.", with a primary **Create account *login*** button under it, which
creates the account (`CreateAccountRequest`) and logs in. The card's hint says "New here? Connect
with the login you want, then create the account." `-Dshots=connect` renders the state.
**Remembered between runs (user decision, 2026-10-05):** the server, login and
password of the last successful login are kept in
`~/.launch-window/client-settings.json` (`client.settings.SettingsStore`, as in the
sibling projects), so the form comes up filled in, focus starts on the password
field, and Enter logs in. A failed login is never saved. Log out and Switch server
in Settings keep the login and password and save the server chosen there.

### 4.2 Join game (first visit to a game only)
**Purpose:** join the game. **Shows:** an archetype choice between NASA-style and
Soviet-style, with the two hub artworks as large cards (several players may pick
the same one); an agency name; the **director's name** (the player's persona,
the head of the agency, fixed for the whole game); a short line saying the game
is in progress and you start fresh. *Sound (the user's idea, 2026-10-12):* Kennedy's "We choose to go
to the Moon" (`assets/audio/moon-speech.mp3`, NASA, 8.6 s) plays once when the screen opens, when
sound is on; leaving the screen stops it. **Actions:** Join. **States:** agency name
taken; director name taken (both checked ignoring case); empty fields.

**History note on the Director field** (component 15, the first one a player
sees). Suggested text: *"Both agencies here have a Director, but in reality the
two jobs were very different. NASA's Administrator was a public figure,
appointed by the President, confirmed by the Senate, and questioned by Congress.
The man who drove the Soviet program, Sergei Korolev, was a state secret. Until
his death in 1966, the world knew him only as "the Chief Designer"."*

**After joining:** the starting
state is 25,000 funds, 250 prestige, 3,000 per season, every building at level 1,
empty rosters, and ten candidates per group (BASPM's ten, decided 2026-10-06). So the first thing to show is a hint
to hire staff (SET Centre first).

**Implemented in libGDX (2026-10-04)** from the Connect and Join Game mockups
(`client.screen.ConnectScreen`/`JoinScreen`). Decisions made while building them,
**confirmed by the user (2026-10-06)**:
- **No BASPM name in the heading.** The mockup's heading names BASPM's own product
  and a game date the client can't know yet (that needs G1). It's now "Join game" /
  "Found your agency" until G1 brings the date.
- **The hint no longer claims things the client can't know.** "Already in
  progress" can be false during the signup window, and the starting funds come
  from the game's own pack. New text: *"Everyone starts from the same line.
  Whenever you join, your agency starts fresh: every building at level 1, empty
  rosters, and ten candidates waiting in each hiring pool. Hire SET specialists
  first. They research the parts every mission flies on."*
  **Replaced (user, 2026-10-23):** hiring advice got ahead of this screen. The hint
  now explains the two choices made here: *"Your choices here last the whole game."*
  What the style decides (its own program tree and headquarters; budgets and rules
  the same; not exclusive), and where rivals see the agency's and director's names
  (front pages, statements and condolences, Intelligence Office files, the Museum's
  list of agencies), fixed for the game. The text is in `JoinScreen`.
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

**A suggested agency name (the user's idea, built 2026-10-12, no mockup):** players put on the spot
pick weak names, so a secondary **Randomize** button beside the Agency name box fills in a name that
fits the game. It deals from `assets/content/agency-names.json` (20 NASA-style and 20 Soviet-style
names, our own; outside the frozen pack, held by `AgencyNamesTest`) like cards: a deck per style, no
name twice until the whole deck has come up, and a fresh shuffle never starts with the last name
(`client.view.NameSuggestions`). The picked card's style decides the deck; **with no style picked it
deals from both** (confirmed by the user, 2026-10-12, as is the rare early repeat when a player
deals before and after picking a style: "it's just something to guide the players"). A suggestion replaces the box's text and clears its error;
the player can edit it. The server still checks uniqueness, so a suggestion can come back "Already
taken". The button is disabled while joining. Tooltip: "Suggest a name": "Fills in a name in the
style of the agency you picked, or either style if you haven't picked one. Press again for another:
none comes up twice until all 20 of a style have. Change it as you like." (the count read from the
file). `FormField.withAction` puts a control beside a field's box. Shots `join-randomized.png`,
`join-randomize-tooltip.png` (`-Dshots=join`).

### 4.3 Space Complex hub (main menu)
**Purpose:** navigation, and the game's front page. **Shows:** full-screen art for
the player's archetype, picked by the game season, with the time of day advancing
every 4 real minutes on re-entry (`design.md` §3.13). The eight building hotspots
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
of the strip opens Settings (§4.12). This replaces "no Log out" below. A help button of the same
kind follows it (2026-10-08, component 23). A new agency's advisor card sits at the bottom left
until its first steps and the road ahead are done (2026-10-08, 2026-10-26, component 24).

**States:** a building that is upgrading shows a subtle "under construction"
marker; a game result exists (§4.13).

**Building markers** *(user's play-test request, built 2026-10-05; look unconfirmed)*: every
building always shows a marker where the upgrade gear sits, so players see at once what can be
clicked. It's a white disc with a navy ring and the place's own icon, the same one the footer uses
(VAB rocket, Mission Control headset, Astronaut Complex astronauts, SET Centre engineers,
Headquarters folder, Public Affairs Office dollar, Museum trophy). It turns navy with a white
icon while its building is hovered, and an upgrading building shows the larger gold gear marker
instead. The marker counts as part of its building for hover and click. The user offered a
generic "clickable" icon from Claude Design instead; the icon is one field per place
(`Hub.Place.icon()`), so it's easy to swap.

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
*Spoiler-free replays first (the user's idea, 2026-10-12):* when an unread report has missions
flown, a dialog over the hub asks before the report opens: "Last night's missions are in. Watch
them first, spoiler-free?" [Watch the missions] [Show the report], with "Don't ask again". Watching
plays each mission's replay in turn (§4.7.3, the film), then the unseen front pages, then this
report, which itself is unchanged: once the player has seen it, spoilers are fair. Settings keeps
the choice: Ask (the default), Always watch first, Never.
**Purpose:** "what happened overnight", the async equivalent of BASPM's
end-of-season news screen. **Ref:** `bulletin_news_*.png`,
`end_of_season_rnd_results.png`, `mission_assessment.png`.
*News placement (the user's request, 2026-10-10):* the blueprint drawings make the R&D section tall,
so when it shows more than one component the News card (with the real-timeline story and the random
events) and the Intelligence card move to the left column, right under Missions flown, ahead of Goals
and Buildings. With one component or none they stay under R&D. Shot `report.png`.

**Shows, grouped:**
- missions flown this season, with step outcomes (success, glitch solved, minor
  glitch, failure, catastrophic), prestige gained or lost, and goals achieved
  ("first in the game!");
- R&D gains per component, and components that hit their R&D cap (researchers
  released);
- training finished (skill gain), Expert trait earned;
- *(specified 2026-10-07)* people news: a trait revealed ("After two seasons on the Atlas team,
  Kowalski turns out to be a workaholic"), a relationship shown ("Ivanova and Petrov work well
  together"), and later ambitions formed, met, missed or overtaken; names open the person page
  (*mocked up 2026-10-07, unconfirmed:* first in the People card, lightly tinted, a trait line
  ending with its trait chip and a relationship line with its marker and "Clash" or "Work well");
  *as built (2026-10-07):* these lead the People card, lightly tinted, each ending with its trait
  chip or marker ("Clash" / "Work well"); a click opens the person page while they're still in the
  agency (`TurnReportView.peopleNews`);
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
has the writeup. Choices **confirmed by the user (2026-10-06)**:
- one scrolling screen, not tabs;
- running costs broken down by kind;
- the morale warning is derived by the client.

**Implemented in libGDX (2026-10-04)** as `client.screen.TurnReportScreen`, with its
wording in `client.view.TurnReportView`, from G2's saved reports (see G2). Choices made
while building it, **confirmed by the user (2026-10-06)**:
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
- **Real space-race news (built 2026-10-06, `design.md` §1.1):** the news card opens with one
  real event from the season the turn played out in, under an "On the real timeline · <season>"
  eyebrow, with a navy accent bar. Below the story, "At home:" gives the player's own side's view
  where the story has one. The card's caption is now "This season". A season without stories shows
  only the random events.
- **A lost passenger (built 2026-10-06):** the People column says "Laika did not come home", with
  "The flight had no way home." or the step that failed (`PassengerLost` event).
- **A grounding (built 2026-10-12, `design.md` §2.5(j)):** after a crew is lost, the People column says
  "Crewed flights grounded" (tag GROUNDED, red): no crewed flight may launch for the next N turns while the
  accident is investigated; uncrewed flights go on (`CrewedFlightsGrounded` event). Mission assembly gives
  the turns left under its disabled launch button, and every crewed mission card waits on the grounding in
  its Astronauts check and earliest launch (component 25). Shots `grounding-*.png` (`-Dshots=grounding`).
- **The answer to a rival's First (built 2026-10-12, `design.md` §2.1(e)):** the Budget card opens with a
  success notice, "Beaten to it: the government answers.", naming the Firsts another agency took last turn
  and the prestige the next review adds (`RivalFirstsAnswered`). When a review falls in the same season,
  the notice says the review counted it, and the review's notice adds "and N more for rivals' Firsts".
  Shots `rival-firsts-report.png`, `rival-firsts-review.png` (`-Dshots=rival`).
- **A raise between reviews (built 2026-10-12, `design.md` §2.1(f)):** the Budget card gets a success
  notice, "Funding raised: + $X a season.", with the prestige, the review whose bracket priced it and the
  rate before and after (`FundingRaised`); the funding line reads "Funding (new rate)". Shot
  `raise-report.png` (`-Dshots=raise`).
- **Blueprints in the R&D card (built 2026-10-09, user's idea after BASPM's end-of-season R&D
  screen):** each component's entry opens with its blueprint drawing
  (`assets/ui/blueprints/<component-id>.jpg`, 864 x 352) across the full card width, at its own
  aspect (about 576 x 235 at 1920 x 1080), above the name, change, reliability bar and caption. The
  user chose the full-width banner over a thumbnail beside the text, accepting a long column in
  seasons with many components (the column scrolls). A component without a drawing shows none.

### 4.5 VAB: rockets
**Purpose:** everything about rockets. The VAB's own cap limits the open rocket
programs (4/8/14), and its level sets the largest rocket class (Light, then
Medium, then Heavy). Both sit on this screen next to its building panel. **Ref:**
`vab_building.png`, `rocket_program_selection.png`.

**4.5.1 Open rocket program.** The agency's rockets that aren't open, each with:
- class, and payload capacity to suborbital, LEO, TLI, and TPI;
- human-rated or not;
- creation cost, unit cost per flight, upkeep;
- *(specified 2026-10-07)* its concepts (component 19).

Open shows the price: the creation cost, charged again on every reopening.
Refused if the VAB is full or its level is too low for the class. Other agencies'
rockets are hidden.

*Play-test additions (user, built 2026-10-05):*
- **Explained terms.** Each payload heading has a tooltip (suborbital, LEO low Earth orbit,
  TLI trans-lunar injection, TPI trans-planetary injection), and the costs read "Creation ·
  once", "Unit · per launch" and "Upkeep · per season", each with a tooltip.
- **Related rockets.** An info notice above the list explains that a rocket starts with part of
  the reliability of related rockets the player owns, taken when it opens, so researching a
  small rocket first gives a big one a head start. Each row's "Starts at" column shows the
  reliability the rocket would open with right now (`RocketPrograms.openingReliability`, the
  value `open()` itself uses), where it comes from, and an **R&D pace** (fast, average or slow,
  from the slot bands via `SkillGrowth.expectedFullTeamRawAmount`; the thresholds are an
  unconfirmed default). The mechanic already existed: it's BASPM's own transfer data
  (`baspm.md` §10.3), e.g. Atlas takes 35 % of Redstone's reliability and 20 % of Jupiter-C's.
- **After opening,** a dialog asks whether to assign engineers now; **Assign R&D** goes to Rocket
  R&D with that rocket's assign dialog open.

*Rocket art (built 2026-10-08):* both lists (§4.5.1 and §4.5.2) start each row with the rocket's
picture at 140 x 90, from `assets/ui/rockets/<id>.jpg` (`make_content_art.py`, from the user's
`assets-raw/Rocket Programs/`). A rocket without art yet keeps its skill glyph in the same space,
so the columns line up (`ChromeScreen.addRocketPicture`).

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

**Goals and reliability on the cards** *(play-test request, built 2026-10-09 without a mockup)*. A
goal not reached lowers the reliability some of a mission's components fly with; some reached goals
raise it (the configuration's goal dependencies and boosts). Every configuration card shows a red
"Goals not reached: −20 reliability" and a green "Goals reached: +5 reliability" when they apply,
above the prestige note; every program card in Headquarters' lists a red "Skipped goals: up to −30
reliability", counting only goals from outside the program (its own first flights are the way in)
at the most any one of its missions loses, so skipping ahead shows before a program is opened. A
tooltip names each goal and what it does (`client.view.GoalModifiers`).

**Staffing ahead of assembly** *(play-test request, built 2026-10-07 without a mockup)*. Every
configuration card adds "Free now: 5 of 2 astronauts · 10 of 9 controllers" under its stats, red
when short (free means what assembly counts: idle and available). The planning screen's assessment
tab adds a Staffing card: per group needed, free, in training that can be cut short, in basic
training and busy; a red line per short group saying how many short and who's coming ("the first
done in 1 season"), with a reminder that hiring takes basic training; and every seat the mission
uses with the skill it reads most (`client.view.Staffing`).

**Plan a mission** *(play-test request, built 2026-10-07 without a mockup, from the configuration
cards of §4.6.5)*. Headquarters' landing screen has three cards: Plan a mission ("Ready to plan · N
missions"), Manage open programs, Browse programs. Plan a mission lists every configuration of
the open programs, one section per program (its name in gold above its cards), with the §4.6.5
card (`ConfigCards`). Since 2026-10-11 (the user's report) both lists show each configuration's whole
picture at the card's width and its own proportions (`ui.FullWidthPicture`), never cropped or stretched,
four columns a row (empty cells keep the columns equal). A card opens the mission plan on its assessment
tab (§4.6.7), which isn't skipped. Back and the breadcrumb from the plan and from Assemble lead
back to this list ("Space Complex › Headquarters › Plan a mission › Vostok · Orbital flight ›
Plan"); the path through a program's pages works as before (`ClientSession.planningFromList`).
With no program open it points to Browse programs.

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
missing goals). Other agencies' programs are hidden entirely. *Looking ahead (2026-10-10,
component 25):* each card shows the astronauts and controllers its missions need, coloured against
the roster, and a locked card's "Needs:" line says in its tooltip where each goal is earned.

**4.6.4 Program details.** Shows:
- state, upkeep per season;
- the price breakdown: opening cost, discount from other open programs, and
  creation cost for components not owned yet (a component still owned from a
  closed program is free);
- the required goals as goal badges (a goal credited through another goal's
  cascade reads "Achieved", without a date);
- the components it grants, each with its reliability bar if owned;
- a link to mission configuration selection;
- *(specified 2026-10-07)* a Concepts section (component 19) with the program's concepts.

**Actions:** Open (shows the price; refused if Headquarters is full, the goals
aren't met, or funds are short). Close, which needs a confirmation: "no refund;
its components go inactive and their researchers are released; refused while one
of its missions flies". After a successful Open the client goes straight to the program's mission
configuration selection (§4.6.5) with the success toast *(user's play-test request, 2026-10-05)*,
unless one of its components needs engineers. *(User, 2026-10-08, built the same day, as the VAB does
for a new rocket, §4.5.1:)* when a component of the program is active with no one on its R&D and
isn't at its ceiling, a "Program opened" dialog asks "Assign engineers to <component>?" (or "to its
components?" when there are several), with its starting reliability. Its buttons:
- **Plan a mission** (primary) goes on to the mission list as before;
- **Assign R&D** opens Payload R&D (§4.8.3), with that component's assign dialog open when there is
  one; with several it just opens the board (highlighting the program's components waits for a
  program with more than one, Gemini on);
- **Later** stays on Program details with the success toast.

A program sharing a component that already has engineers (Vostok after Korabl-Sputnik 2) skips the
dialog. Shot: `hq-details-offer-rnd.png`.

**4.6.5 Mission configuration selection.** The program's configurations, each with
art, duration in seasons, prestige (first time, or the repeat tier: 50 % second
time, 30 % after that), launch types, and crew / controller / CAPCOM counts. *Looking ahead
(2026-10-10, component 25):* an open program's cards add a readiness row and the earliest launch.
Viewable even when the program isn't open *(decided)*; then the planning screens
are read-only, with an "Open program" call to action. *(Built 2026-10-08, for the full tree:)* a
configuration gated on a goal (`requiredGoalIds`) carries a navy "Locked" tag, and its steps
screen (§4.6.6) names the goal ("Needs first: …") beside a disabled "Plan this mission". An
air-dropped launch (`AIR_DROP`, the X-15 and the MiG E-154) has no rocket picker: the launch card
says a carrier aircraft drops the craft, and assembly lists it as "air drop, no rocket".

*Goal boosts (2026-10-08):* the steps screen's "Goal boosts" list has a caption like the penalties
list's; the plan's components card lists every boost on a component, a reached one as "+1" in green
and one not reached yet as "(+1)" muted; Mission Control's mission view names the boosts in play
("Pegasus flown is reached, so Mercury capsule flies 1 point higher").

**4.6.6 Mission configuration details and mission steps overview.** The steps,
season by season. For each step: the components involved (with shares), the
controller skills and CAPCOM involved, the crew skills, whether the crew is at
risk, the prestige share, and the goals it achieves. Also the goal dependencies
(penalties until a goal is reached) and boosts. *(Specified 2026-10-07:)* a Concepts section
(component 19) with the configuration's concepts and its steps' (built 2026-10-07, in the summary
card).

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
- **Team chemistry** *(specified 2026-10-07)*: the crew and controllers' chemistry (component 21)
  next to the staffing, updating as slots are filled; pickers show candidates' revealed traits
  and relationship marks with people already placed (components 20, 22). Auto-assign keeps
  picking by skill only (first pass). *Mocked up (2026-10-07, unconfirmed; see the note after
  component 22):* one chemistry for the whole mission, crew and consoles together as the rules
  compute it, in a card under the flight crew with its reasons listed; the odds say they include
  it. Tiles show revealed traits and relationship marks (only when the partner is on this mission
  too). The slot pickers get the R&D assign dialog's columns, the current holder highlighted with
  "Keep".
  *As built (2026-10-07):* one chemistry for the whole mission in its own card under the crew, with
  its reasons listed ("Why"); the rocket card stays. Tiles show revealed traits and a mark when
  the partner is also on this mission. The picker shows traits, marks, the skill and the chemistry
  with each candidate in that slot instead of its holder. The odds' caption says they include the
  known chemistry.
  *Changed 2026-10-08 (user's decision, `design.md` §2.2(d)):* a mission is two teams, so the card
  shows two chemistries, "Team chemistry · Crew and CAPCOM" and "Team chemistry · Flight
  controllers", each with its head count and "Why" (a team nobody is in yet is left out). Tile
  marks and the picker's marks and preview are against the slot's own team; the picker's caption
  names it.
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
Orbital. `design.md` §3.13 has the full writeup and the choices, **confirmed by the user (2026-10-06)**: the
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
**confirmed by the user (2026-10-06)**:
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
§3.13 has the writeup. Choices **confirmed by the user (2026-10-06)**:
- no building panel on the overview, as in the SET Centre (later changed, see below);
- re-staffing has no confirmation (it's free and reversible);
- the replay shows chances and pass/fail rather than dice numbers. **It doesn't step on by
  itself (user, 2026-10-06):** the player steps through at their own pace (built the same day);
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
  The player steps through by hand (2026-10-06): back to start, previous, a primary "Next step"
  ("Season summary" on the last step), "Skip to summary", or a click on any step in the list.
  There's no timer and no play/pause. "Replay season N" opens it for the latest turn.

**The replay as a film (the user's idea, 2026-10-12; built directly, no mockup).** Each step plays
an animated clip (`design.md` §5, Backlog, "Animated mission steps"), and the replay plays the steps
by itself again. **This reverses the 2026-10-06 decision** against automatic playback: with the
clips, the replay is meant to be watched like a short film, and the player can still pause and step.
**The clips are confirmed (user, 2026-10-21):** every clip's story, craft, shape and proportion below, and the
decisions made along the way, as built at the end of the overhaul pass. The console, the playback timings and the
sound placements keep their own flags.
- **Layout:** the step card keeps its head (step n of m, name, text, components); the result (the
  roll line and the outcome box) moves up beside the success chance and the saving roll, and a
  monitor fills the rest of the card: a navy console drawn by the game (sharp at any size) across
  the card's full width and remaining height, with a small label in its top strip ("Cam 1 · Launch",
  or "No signal") at the console's left edge, and a 16:9 picture as large as the console allows,
  centred, in a thin lighter lip; the console's wings fill the width the picture's ratio leaves
  (2026-10-13).
- **Clips:** `assets/ui/replay/<clip>-<variant>.webm`, rendered at 960 × 540 (about the picture's
  size at 1080p), each with a sidecar `<clip>-<variant>.json`: `duration` and `revealAt`, the second
  from which the variant can't turn into another one any more (set by hand per clip). Variants:
  `success` (the first roll passed), `glitch` (a glitch, solved or not: the mission goes on) and
  `failure` (failure; also catastrophic, until a step has its own `catastrophic` clip).
  `assets/content/replay-clips.json` maps step ids to clips: a step id loses its number (`03-`),
  then the first rule whose pattern matches the rest names the clip (`launch` matches `launch`,
  `atv-launch`, `gemini-b-launch`). A step matches only by its own name, behind a vehicle the file
  lists (`atv`, `gemini-a`, `soyuz-7k-ok`) or with a craft's letter after it (`reentry-a`), never by
  how its name ends: `lunar-ascent` never plays `ascent`'s clip, only its own (2026-10-13; a test
  holds the list to the pack's vehicles). A rule can also name the step before (`"after"`, matched the
  same way; 2026-10-14): `landing` is on the Moon after `powered-descent` or a robot's
  `lunar-approach`, a probe's after `lander-descent` and a spaceplane's after `descent`, so a rule with
  `after` goes before one for the same step without it. A rule can name a step anywhere before it in the
  season instead (`"earlier"`, 2026-10-15): `lander-descent` follows `lander-entry` at both planets, so
  Mars is told by `mars-approach` earlier in the season. It's outside the content pack (not hashed), like
  the stories.
- **Which steps have clips (2026-10-13):** a first satellite's whole mission (Sputnik 1, Explorer 1):
  launch preparations, countdown, launch, ascent, deployment, Earth orbit. The rest are listed in
  `assets-raw/replay/CLIP_CHECKLIST.md`. Every clip is generic (it plays for every program that flies
  the step: the payload is the launch rocket's own nose section) and has no lettering (digits only).
  Steps cut straight into each other, so a step's success and glitch end on the same frame, and a
  step on the pad ends on the next one's first. Where history differs from a borrowed clip, the step
  gets its own: Venera 7's carrier entry (`interplanetary-bus-entry`) played the capsule's `reentry`
  right after the lander's, but the carrier burned up (`design.md` §1.1), so it has `bus-entry`
  (user, 2026-10-15): the carrier burns up in every variant, and only the data it sends home before
  differs. The landers' entries had the Earth capsule's `reentry` too, Earth's limb and
  all; they have `venus-entry` (Venera's sphere; the Pioneer Venus probes have their own since 2026-10-17) and `mars-entry` (Viking's
  and Mars 3's aeroshell), the reentry's story at their planet, Mars picked by the step before
  (user, 2026-10-15). Their descents had the capsule's chutes; they have `venus-descent` (Venera's
  reefed chute opening; the glitch tears it, as Venera 7's did, the failure tears it away) and
  `mars-descent` (Viking's chute, heat shield, legs and three engines; the glitch lights one late, the
  failure none), Mars picked by an earlier step (user, 2026-10-15). At Mars the
  landing follows on in `mars-landing` (Viking's lander on its engines; the glitch stands on a rock,
  tilted, as Viking 2 did; the failure sends once and falls silent, as Mars 3 did), for `landing` and
  `landing-and-deployment` (user, 2026-10-15), and the work on the surface after it in
  `mars-surface` (the probe's story, its arm stalling as Viking 1's did, with Viking's lander; user,
  2026-10-15).
- **The way home goes the orbit's way (user, 2026-10-16):** the Earth orbit's map goes round
  anticlockwise, so over the Earth the craft travels left; the way-home clips (reentry preparations to
  reentry, and the suborbital flight) now play mirrored, nose left in orbit and nose right for the burn.
  Faint motes stream past against its way in orbit, and slow with the retrofire. A burn with no reentry
  preparations before it (Biosatellite's `de-orbit-burn`, straight from the orbit) plays `deorbit-burn`,
  the capsule turning round first. `reentry` after `earth-approach` (from the Moon, coming in from the
  left) plays `reentry-from-moon`, unmirrored. The cut from `deployment` (going right) to
  `suborbital-flight` (now going left) flips.
- **Orbit insertion coasts to the top, then burns (user, 2026-10-16):** the craft rises from the ascent on
  its arc, slowing, to the top on the planned orbit, and fires there to circularize. The glitch cuts out
  and sags below the orbit until a relight brings it back; the failure falls back and burns up.
- **A Soyuz-type craft drops its orbital module (2026-10-16):** the retrofire now comes before the pack's
  jettison (`design.md` §5, M3). These flights (Soyuz 7K-L3, 7K-OK, LK-700, Gemini EOR's lander tests) carry
  an orbital module on the nose through the burn and the jettison; their modules separation drops it, where
  Vostok's drops the pack. The glitch hangs it on one latch until a second try; the failure never frees it
 .
- **Rules by craft and by what's planned (2026-10-16):** a rule may name `programs` (it matches only a
  configuration of one of them) and `later` (a step anywhere after it in the season's plan, which the player
  sees, so it spoils nothing). The Soyuz-type programs show their orbital module from their first close-up
  (the LK-700's and the 7K-OK's hop only when a jettison is coming); Gemini EOR comes home Gemini's way:
  `equipment-section-separation` (the adapter's back section drops, the capsule turns base first), then the
  retrofire and the retro section's jettison. Vostok had no pack to drop, so its "retrofire jettison" (BASPM's
  Vostok suborbital, a what-if) plays `retro-shutdown`: the burn tails off and the jets steady it; the glitch's
  valve sticks and the engine sputters on, the failure's won't stop and pitches the capsule away.
- **Lunokhod (user, 2026-10-16):** Luna 17's two surface operations had the Mars lander's clip; they play the
  rover's own: `lunokhod-deploy` (the ramps unfold, it rolls down onto the surface, opens its lid to the Sun and
  calls home; the glitch slips halfway down the ramp and tries again; the failure noses in at the ramp's foot and
  pitches over) and then `lunokhod-drive` (on over a crater's rim, a laser from Earth finding its reflector; the
  glitch slips on the slope; the failure is Lunokhod 2's end, its open lid scooping soil that overheats it once
  shut).
- **The Luna landers (user, 2026-10-16):** Luna 17's and Luna 15's landings had the crewed lander's clip, and
  Luna 15's surface operations the Mars lander's; they play the Ye-8 stage now (`luna-landing-rover`,
  `luna-landing-sampler`: down on its engine, which cuts just above the surface; the glitch cuts high and lands
  hard; the failure, Luna 15's, flies into the ground) and `luna-sampler` (the arm drills a core and lifts it
  into the return capsule; the glitch hits rock and drills again; the failure jams), and `luna-liftoff` (the
  return rocket lifts off the stage and climbs away; the glitch's engine sputters first; the failure leans, loses
  its engine and falls back).
- **Surveyor (user, 2026-10-17):** its `lunar-landing` played the crewed lander's clip; it plays
  `surveyor-landing` now: the tripod comes down on its three verniers, which cut a few metres up, and it drops
  onto its pads, calls home and turns its solar panel to the Sun. The glitch is Surveyor 3's: the verniers don't
  cut, and it hops twice before they're shut off from Earth. The failure hops the same way, comes off it leaning,
  lands on one foot and topples onto its antenna.
- **Project Mercury as itself (user, 2026-10-17):** the first of the programs from Mercury on to get their own
  spacecraft in every clip (the rocket stays generic: any rocket can fly them). Mercury's 17 clips (`mercury-*`) are the
  generic timelines drawn with its capsule (a step's `"skin"`, `mc-` parts: the blunt shield, the shingled cone with
  its window, the recovery compartment, the antenna canister), and a few of its own touches, invisible on other craft:
  the escape tower on the pad and jettisoned in the ascent after staging, the periscope out after the turnaround and in
  after the retrofire, the three retros lit one after another, the shield let down on the landing bag before the
  splash. Its orbital flights keep the generic turn before the retrofire, though Mercury flew heat shield first all
  orbit (`design.md` §1.1): kept so (user, 2026-10-17).
- **Project Gemini as itself (user, 2026-10-17):** the second package: Gemini in every clip of the `gemini` program
  (`gemini-*`, `gm-` parts: the dark reentry module seen from the hatches' side, two windows, the RCS and R&R
  sections; the white adapter), and the Agena target vehicle as itself: in the docking close-up, its cone taking
  Gemini's nose, and in its own launch (`agena-*`, `ag-` parts), picked by a rule's new `"vehicle"` (`atv`, the
  steps in front of it). Gemini's touches: no escape tower; the reentry preparations' turn, then the equipment section
  dropped (`adapter.jsx`; after the suborbital flight, which turns already, the trim then the drop); the retros lit
  one after another; the retrograde section jettisoned without straps; the main crossfading onto two points as the
  module tilts to 35 degrees nose up for the splash, upright again after it; the spacewalk's hatch on the cone and
  the experiment on a mount. Its spacewalks in steps (user, 2026-10-18): where three come in a row (`eva`,
  `scientific-experiments`, `eva`) the first sets up the experiment (the collector plate rises out of the package on
  its boom, the lamp comes on; glitch: the boom jams and a push frees it; failure: the visor fogs, Gemini 9A's), the
  second runs it (micrometeoroid sparks on the plate; the lamp goes out and a tap brings it back, or doesn't), the
  third takes it down and brings it in (the boom stalls and is pressed down, or the package is left on the hull). The
  docking flights' spacewalks are with the Agena docked at the nose (user, 2026-10-18; Gemini 10, 11 and 12 had
  theirs so): their `egress` is a stand-up in the hatch (the hatch sticks part open, Gemini 4's), and their two
  `eva`s are out on the tether (the tumble and the gun, or the fog) and then a photo and back in (the camera slips
  and is caught, or drifts away, Gemini 10's). A rendezvous flight's lone `eva`, after the `rendezvous`, has both
  capsules in it, nose to nose a few metres apart (user, 2026-10-18): the walker comes out of one and drifts over the
  noses, and the other's crewman stands up in its hatch and photographs it (the walker tumbles and the gun steadies
  it, the pictures after; or the visor fogs, the walker is hauled in, and the photographer goes back in too). Any
  other Gemini `eva` plays the out-on-the-tether clip from a closed hatch, ending outside. Rules tell them apart by
  `later` and `after`. The Earth-orbit tests of circumlunar Gemini, lunar-orbital Gemini and Gemini
  direct ascent fly Gemini too, with their own hardware (user, 2026-10-18): the Gemini rules name their programs, and
  a stack takes Gemini's place on the pad, in the climb, the deployment and on the map (`parts.py`'s `gt-`, `gl-`,
  `gd-`). The circumlunar heat-shield test flies Gemini on top of its Transtage up to the high-orbit injection; the
  Transtage test launches the Transtage alone first (`transtage-*`, under a shroud; rules by `earlier: deployment`
  and `later: docking`), docks Gemini to it (the Transtage the target) and burns docked nose first
  (`gemini-transtage-docked-hoi`). That burn isn't a trans-lunar injection (user, 2026-10-18; the converter renames
  the step, `high-orbit-injection`): the Transtage stretches the orbit into a long ellipse, out far and back, so the
  capsule meets the air at nearly lunar-return speed. The camera pulls back to fit the ellipse (dashed) and the
  burn puts the craft on it (`gemini-transtage-hoi`, `high-orbit-injection.jsx`; the failure's short burn leaves it
  on a smaller one), and the Transtage lets go 0.4 s after the burn and drifts back off Gemini's tail (`SEP`,
  `hoi-separate.jsx`; the map's stack in two layers, `gt-o-sat-gemini` and `gt-o-sat-stage`, user); the Earth orbit after it is the capsule coasting out along it, round the high point
  (`gemini-high-orbit`, `high-orbit.jsx`; the shared scene `high-orbit-scene.jsx`). Its retrofire at the high point
  lowers the low point into the entry corridor, so the Earth orbit after it is the fall down the lowered path, as the
  lunar-orbit tests' coast to reentry (`gemini-high-orbit-return`, the capsule with its retrograde section only, blunt end first as the
  retrofire left it, `tail-first.jsx`, user). The lunar-orbit tests fly Gemini with its propulsion module behind the adapter
  (`gemini-lpm-*`), the direct-ascent tests Gemini's reentry module on an ascent and a landing module (`gemini-da-*`).
  Their tests fly the same long ellipse as the heat-shield test (user, 2026-10-18): the module's burn is the high-orbit
  injection (`-hoi`), the coast to the high point the coast out (`-high-orbit`), the entry corridor trim (or the
  ascent module's burn at the high point) lowers the low point into the entry corridor, the band turned to it, the new
  path dashed as it's set (`-trim`, `high-orbit-trim.jsx`, 7 s, time slowed there, 0.3: a turn to tail first, the
  burn, the turn back, each given its time, user), and the coast to reentry falls
  down it to the band (`-return`, `high-orbit-return.jsx`; the glitch drifts shallow and is corrected, the failure's
  correction misfires). The direct-ascent craft's ascent and landing modules let go at the
  reentry preparations, as Gemini's equipment section does (`gemini-da-reentry-preparations`, `gd-r-trunk`; it has no
  retrograde section, user, 2026-10-19); the lunar propulsion module is still gone there without a clip for its parting. The Transtage's size is real (3.05 by 4.57 m, two engines); the propulsion module's and the
  direct-ascent craft's shapes follow our blueprints.
  Circumlunar Gemini's flights to the Moon fly as themselves (user, 2026-10-20): the Transtage launches alone on its
  Earth-orbit tests' clips (rules by `"vehicle": "transtage"`), is on the map alone (`tt-o-sat`), and in the uncrewed
  flyby burns for the Moon by itself (`transtage-tli`; its `ignition-and-separation` separates from nothing on screen),
  coasts (`tt-m-probe`) and passes the Moon (`transtage-*`). In the crewed flyby Gemini launches on its own clips, docks
  into the Transtage (`transtage-docking`, played for `approach-and-docking`, which has no approach of its own), and
  the docked stack burns for the Moon, the Transtage letting go 0.4 s after the burn and drifting back
  (`gemini-transtage-tli`, `tpi.jsx`'s `SEP`, the stack in two: `gtd-o-sat-gemini`, `gtd-o-sat-stage`). Gemini coasts out
  and back (`gm-m-probe`), passes the Moon, and on the Earth approach casts off its adapter at 0.6 s in every variant
  (`gemini-earth-approach`, `earth-approach.jsx`'s `DROP`): no step has it, and a capsule must meet the air alone
 . Its reentry is Gemini's unmirrored (`gemini-reentry-from-moon`), in from the left as the approach leaves
  it; its descent and recovery Gemini's. The flame on the map stays the generic one, over the Transtage's drum.
  Lunar-orbit Gemini's flights to the Moon fly as themselves too (user, 2026-10-20): the Earth-orbit tests' launch and
  deployment (`gemini-lpm-*`); the propulsion module makes the trans-lunar injection (the step is 80 % the module's),
  brakes into lunar orbit and fires the craft home (`-tli`, `-loi`, `-lunar-orbit`, `-tei`), or on a flyby passes the
  Moon in one step (`-lunar-flyby`; the flybys' steps corrected in the converter, `design.md` §5, M3). On the Earth
  approach the module and the adapter go together (`-earth-approach`, `DROP`); the reentry, descent and recovery are
  circumlunar Gemini's. The module's burns and the docked Transtage's have their flame at their own engines
  (`gl-o-streak-lpm`, `gtd-o-streak`); the Earth-orbit tests' module burns still have the generic one.
  Direct-ascent Gemini's flights to the Moon fly as themselves too (user, 2026-10-20, `gemini-da-*`): the Earth-orbit
  tests' launch; the rocket's upper stage stays on through the parking orbit (`-deployment-stack`, `-earth-orbit-stack`),
  makes the trans-lunar injection (70 % of the step is the rocket's) and lets go (`-tli`, `SEP`; a plain stage, drawn as
  lunar-orbit Gemini's Centaur once was). The landing module brakes into lunar orbit and lands, or on the orbit flight
  fires the whole craft home; after a landing the ascent module and the capsule lift off, climb with nothing waiting in
  orbit (`o-sat-b` empty) and fire home (`-asc`). The flyby passes the Moon in one step. On the Moon the craft stands in
  the lander's place (`gd-n-descent`, `gd-n-ascent`, 42 px a unit so the capsule's nose stays in the moonwalk's
  close-up), and its one surface step plants the flag (`gemini-da-flag`), which stands through the liftoff. The Earth
  approach casts off what's left (`DROP`); the reentry, descent and recovery are circumlunar Gemini's. Sizes from our
  blueprint.
  Gemini EOR's flights to the Moon fly as themselves too (user, 2026-10-20): the tug and the stack launch on the
  Earth-orbit tests' clips; the flyby and the lunar-orbit flight carry no lander, so their stack flies its frame empty
  (`ge-*-empty`, `gemini-eor-empty-*`; no frame at all is the alternative). The stack meets the tug and docks nose first
  into its ring (`gemini-eor-tug-*`), the tug burns for the Moon and lets go (`gemini-eor-tli`, `SEP`), and the service
  module brakes into lunar orbit and fires home (`-loi`, `-tei`; its flame from the frame's open end, `ge-o-streak`). In
  lunar orbit the close-ups have the Moon below (`d-moon` for `d-earth`, the `-moon` clips): the walk aft to the
  lander, its release, its return and the cast-off are the Earth-orbit tests'. The Langley lander lands at the
  moonwalk's scale (`lg-`, 40 px/m, standing on its pads), its pilot steps off the open platform (`langley-walk.jsx`)
  and plants the flag (`langley-flag`), and the whole lander lifts off (one stage) and climbs to the stack waiting
  in orbit (`langley-lunar-ascent`). The lander test in lunar orbit goes from its powered descent straight to the
  climb (BASPM has no landing between). Home, the modules separation drops the service module, its frame and the
  retrograde section together, there being no retrofire (`gemini-eor-modules-separation-moon`, `ge-r-trunk-moon`),
  then the Earth-orbit tests' reentry, chutes and splashdown; the generic equipment-section separation, which dropped
  the section before the turn, no longer plays for them.
- **The early-era pack's craft as themselves (user, 2026-10-18):** Vostok (`vk-`, `vostok-*`) plays every step as
  itself: the sphere on its instrument module (its one retro flame; the module the separation's, its cable meeting
  the module's near end, `project-vostok.jsx`), the nose fairing thrown off in the climb as Mercury's tower is
  (`a-les`), the sphere alone on its chute. Korabl-Sputnik 2 flies it too, landing on the ground (its
  `descent-and-splashdown`). Sputnik 1 (`sp1-`), Explorer 1 (`ex1-`) and Sputnik 2 (`sp2-`) fly as themselves in
  the satellite's clips, Sputnik 2's heartbeat included. Simplified: the rocket stays generic (no R-7, no Juno I,
  Explorer 1 standing on Juno's spinning tub only; it flew bare, with no fairing, so its whip antennas lie folded
  along it on the rocket and swing out 0.3 s after the deployment's push, `explorer-whips.jsx`, user, 2026-10-20);
  Sputnik 2 separates in its deployment, though it stayed on its
  core stage; the satellites drawn larger than their rocket's scale. Vostok's pilot doesn't eject in the descent
  yet.
- **The later NASA satellites as themselves (user, 2026-10-21):** the Orbiting Frog Otolith (`of-`, `ofo-*`): its
  octagonal lower section and the cone above it under its heat shield, under a fairing on the rocket; its four booms
  ride folded against its side and swing out after the deployment's push (`explorer-whips.jsx`, as Explorer 1's whips;
  the yo-yo despin before it isn't shown). Biosatellite (`bs-`, `biosatellite-*`): the recovery capsule, a bell on its
  heat shield, on its louvred adapter; coming home the capsule alone (the adapter stays in orbit, gone before the de-orbit
  burn, which no step shows), its one deorbit motor on the shield's middle, turned base first by the burn's own turn,
  the generic reentry and splashdown (Biosatellite II's capsule was caught in the air, III's came down in the sea).
  Pegasus (`pg-`, `pegasus-*`) flies Apollo's launch clips, its boilerplate CSM and tower being Apollo's on the rocket;
  in its deployment the boilerplate drifts off and Pegasus stays on the stage, as it did on the S-IV, its two wings
  unfolding to scale 0.9 s later over 2.2 s (`pegasus-wings.jsx`; the glitch's left wing stalls half open for 0.8 s, the
  failure's never open), the radio rings from Pegasus once they're out; on the map its wings at 45 % of their length.
  The rocket stays generic.
- **The robot probes as themselves (user, 2026-10-21):** every probe of the standard pack has its own drawing (`parts.py`'s
  `PROBES`, a prefix each: `pio-` for Pioneer 10 and 11, `pa2-`, `pi4-`, `pi6-` for the Sun orbiters, `pvo-`, `pvm-`, `rng-`, `lor-`,
  `mr5-`, `mr6-`, `mr9-`, `mr10-`, `vik-`, `svy-`, `lu2-`, `lu3-`, `lu15-`, `lu17-`, `ma2-`, `m69-`, `ve15-`, `ve7-`, `ve9-`), folded on
  the rocket and in the deployment, open in orbit, on the maps and at the Earth departure: the ascent throws the nose
  fairing off as Mercury's tower and the probe stands on its adapter, then flies its own copies of the generic clips
  that draw a craft (`build.py`'s `PROBE_CLIPS`, `<family>-<clip>`; the rules are the generic ones with the family's
  programs). The pad's three clips stay generic: under its fairing a probe looks as the generic nose does. Luna 15 comes
  home as its return rocket and then its sphere (`lu15-o-sat-ret`, `-r-capsule`), landing on the ground. Zond 5 flies the
  7K-L1's clips and comes down in the sea (`soyuz-descent-and-splashdown`). The landers' releases too
  (`lander_release_parts()`): Venera 7's and Venera 9's buses let their own spheres go (`venera-7-probe-release`,
  `venera-9-probe-release`; Venera 9's sphere rides on top, so its bus is drawn upside down), and Mars 2's orbiter lets
  its descent module go on Viking's timeline, 4.5 hours out with Mars a disc ahead, one motor on the module's back
  (`mars-2-lander-detachment`). Their entries, descents and landings too (`lander_own_parts()`, aliases in the generic
  parts' places): Venera 7's sphere with its chute's lid, landing on its side in the glitch; Venera 9's 2.4 m heat shield in
  its entry, then its lander (airbrake disc, antenna drum, crush ring) under the chute and on the ground; Mars 2's lander
  as Mars 3's design, a sphere with one retro motor whose petals stay closed on the way down and open on the ground
  (`mars-2-petals-*.jsx`), also on its surface clips (`mars-2-mars-surface`, `mars-2-mars-rover`, where Viking's sampler arm
  still lies across it when stowed). Venera 7's carrier burns up as itself (`venera-7-bus-entry`). Mars 2's entry stays generic: `e-shell` is its aeroshell's shape. The landers' masts
  are whip antennas that extend after landing (Venera 9's antenna was fixed). The probes are drawn to the generic
  sprite's size, panels and booms shortened, their panels folded on the rocket.
- **Apollo's command and service module (user, 2026-10-18; `ap-`, `apollo-*`):** on the pad in its adapter under the
  boost cover and escape tower, thrown off together in the climb (`a-les`); for a flight to the Moon it stays on the
  S-IVB, which makes the burn (the `-stack` clips: the deployment, parking orbit and the burn, rules by `later: tli`),
  and after the extraction it flies with the lander on its nose (`ap-o-sat-lm`, `ap-m-probe`; rules by
  `earlier: extraction`), alone home; the service module's one big engine; three mains in the descent, the glitch's
  half-open main two full and one collapsed (Apollo 15). A target's own launch (`atv-`) stays generic. Its lunar module
  (`ap-n-descent`, `ap-n-ascent`, drawn in the generic lander's place so the landing's and the moonwalk's timings fit)
  in the lander's scenes (engine tests, staging, powered descent, landing, liftoff, moonwalks), in the docking
  close-up upside down, its tunnel to the CSM's probe (`ap-k-target`; the ascent stage alone after the Moon and at
  its jettison, `ap-k-target-asc`), in the transposition and extraction with the CSM and the S-IVB (`ap-t-stage`), and
  on the lunar ascent's map climbing to the CSM.
- **Apollo's Earth-orbit tests as themselves (user, 2026-10-20):** the suborbital flights fly an arc, not an orbit
  (`apollo-suborbital-flight`), their SPS test the burn, the reentry preparations after it a trim (`apollo-reentry-trim`,
  the after-the-burn one). Apollo 5 (`a5-`, `apollo-lm-*`): the LM alone in its adapter under a nose cone, no CSM or tower;
  the adapter's panels open and the LM backs out; alone on the map; its engine test and staging without legs, as LM-1
  flew; the ascent stage's own test a shot of its own, and the ascent stage alone on the map after. The transposition,
  docking and extraction in one scale (Moon flights too): the LM at the CSM's 39 px/m with its legs folded, the S-IVB 6.6 m
  across (`apollo.jsx`'s `LMK`, `LM`), the first docking with the LM still on its S-IVB (`apollo-docking-sivb`); the second
  docking the ascent stage alone. The high-orbit test's burn puts the CSM, still on its S-IVB, on the long ellipse
  (`apollo-hoi`, no separation), its medium Earth orbit the coast round it. The spacewalks on the CSM's own hull, the hatch
  on the command module's cone (`ap-e-capsule`): the spacewalk flights in three steps (out, along a handrail, a photo and
  in), a lunar module test's one with the LM docked at the nose (`apollo-eva-lm`). The rendezvous flight's two CSMs close
  in (`apollo-rendezvous*`), and the docking flight launches, meets, docks with and leaves the Agena target vehicle BASPM
  gives it (`agena-*`, `apollo-agena-*`; user).
- **Apollo's moonwalks as themselves (user, 2026-10-20; `flag.jsx`, `samples.jsx`, `rover.jsx`, the figure in
  `walker.jsx`):** the first (`eva-i` after the landing, `apollo-flag`) plants a flag, a plain cloth with no country's:
  down the ladder with the pole over the shoulder, two pushes into the ground, the cloth run out on its crossbar
  (glitch: the pole jumps off rock and the crossbar sticks halfway, as Apollo 11's did, then runs out; failure: a
  slip, the pole tips over, the visor fogs, back in). The last (`eva-ii` after `eva-i`, and `eva-iii`,
  `apollo-samples`) carries the rock box out past the boulders, scoops up two gold rocks and carries them up and in
  (glitch: a rock bounces off the rim and is chased; failure: a fall chasing it, the box left lying). The rover's
  drives (`rover-trip-i`, `-ii`, `apollo-rover-drive`) ride along with the rover from beside the lander (glitch: the
  fender lost on a rock and the crew showered in dust, as on Apollo 17; failure: the rover dies and they walk back).
  A middle moonwalk keeps the experiment package (`apollo-moonwalk`). The flag stands in every Apollo surface scene
  after it, the liftoff's blast shaking it (`build.py`'s `"vars": {"FLAG": true}`, `moon.jsx`'s `flagPlanted`).
 
- **Soyuz 7K-OK's steps by their names (user, 2026-10-20):** once the converter typed them by BASPM's names (`design.md`
  §5, M3), the lone spacewalk plays the stand-up egress, out on the tether from standing and a photo and back in on its own
  hull (`sy-e-capsule`, wings out); the crew transfer (Soyuz 4 and 5) the stand-up in the first ship's hatch, the crossing
  from standing to standing (`crew-transfer.jsx`'s `FROM_STANDING`, `TO_STANDING`) and in through the second ship's hatch
  (`transfer-ingress.jsx`), all on the docked pair framed as the crossing (`sy-e-capsule-pair`, `soyuz-transfer.jsx`,
  `eva-standup.jsx`'s `STANDUP_TRANSFER`); the long flights' six levels each on from the last, the supplies running lower
  (`duration.jsx`'s `LEVEL` and `SUPPLY`); the suborbital flights' reentry preparations the trim after the turn
  (`soyuz-reentry-trim-sub`) and the KDTU-35 test the burn.
- **Soyuz 7K-OK as itself (user, 2026-10-18; the rocket stays generic, user):** its three modules (`sy-`, `soyuz-*`),
  under a fairing and escape tower on the pad (thrown off in the climb, `a-les`); the orbital module rides on the nose
  through the way home (`project-soyuz.jsx`'s `OM`, read now by `reentry-preparations.jsx`, `reentry-trim.jsx` and
  `modules-separation.jsx` too) and goes off ahead at the modules separation as the service module goes aft; its soft
  landing (`SOFT`); docking, the active probe to a passive Soyuz's drogue. The solar wings drawn short (60 % on the
  pad and in orbit, 40 % coming home, so the turns keep them in frame). The spacewalk (`eva-7k-ok`) and the crew
  transfer stay generic.
- **The Soviet lunar craft's Earth-orbit tests as themselves (user, 2026-10-19; the rocket stays generic):** played
  on Soyuz's skin and timelines with their own parts in place. The 7K-L1 (`s1-`, `soyuz-l1-*`): a support cone where
  the orbital module was, under the same fairing and tower, gone with them in the climb (assumed); the descent and
  service modules, the wings and a high-gain dish on a boom (`soyuz_dish`); no orbital
  module coming home (`project-soyuz-l1.jsx`); a suborbital flight's engine test is its retrofire, and the reentry
  preparations after it trim instead of turning again. The 7K-LOK (`lok-`, `soyuz-lok-*`): 10.06 m, so its service
  module is 4.4 m (`LOK_SM`; longer coming home, `LOK_LONGER`, `project-soyuz-lok.jsx`), no wings (fuel cells), the
  Kontakt probe drawn as Soyuz's; it leaves the rocket on its Blok D (`blok_d`, 5.5 m) and then the
  Blok D (the `soyuz-7k-l3-lok` deployment); its rendezvous and time together as two LOKs; it docks to the LK's
  ascent stage. The LK (`lkl-`, `lk-*`): the LPU without legs, as the T2K tests flew, and the ascent stage (cabin,
  big window, the passive Kontakt plate, the Blok E whose one engine both tests and lifts off, firing down through
  the LPU); its own launch under a shroud, its engine test and staging in the lander's scenes, the ascent stage alone
  on the map after the staging. Both craft share their descent module's reentry, descent and landing with Soyuz
  (`soyuz-reentry`, ...). Refined (user, 2026-10-19): the LOK's burn has its own flame at its own nozzle
  (`lok-r-flame-1`); a second ascent is the rocket's third stage (the Soyuz-L had one) and plays the orbit insertion
  with the craft still on it (`-insertion`, `lkl-o-sat-stage`, `lok-o-sat-stage`); the uncrewed LK's last orbit is the
  ascent stage on its long ellipse (`lk-high-orbit`: Kosmos 379's ascent burn raised its apogee to about 14,000 km,
  and it stayed up); the staging starts where the engine test's burn left the LK, higher and turned
  (`after-engine-test.jsx` sets `lander-staging.jsx`'s `FROM`, user, 2026-10-19). The crewed test's two engine tests
  follow the landing plan (user, 2026-10-19): the first opens docked, the LK lets go of the LOK, which backs off and
  away, then flies the hover and touchdown burn with the main engine and the backup lit together, the backup's two
  nozzles shutting down once the main one burns steadily (on a glitch the backup carries the burn while the main
  one sputters; `lk-engine-test-undock`, `lk-undock-test.jsx`); the second is a short check burn of the main engine
  alone (`lk-engine-test-check`), and the staging after it is `lk-staging-check`, each on from where the one before
  left the LK (`lander-engine-test.jsx` takes `FROM` and its times from a step's file, as the staging does). The LOK's spacewalks have its own hull (`lok-e-capsule`): the lone spacewalk's three
  steps are out on the tether, along the handrail to the docking probe and back (the crossing rehearsed,
  `eva-rail.jsx`), and a photo and back in; the lander test's two are the crossing to the docked LK and back from its
  ascent stage (crew-transfer.jsx's distance and rail and spacewalk.jsx's framing from `lok-transfer.jsx`). In the
  crewed lander test the LK flies with the LOK from the third stage on (user, 2026-10-19; `lok_lk_parts`): stacked as
  the N1 had them, the LK under the LOK on the Blok D, on the insertion and leaving the Blok D (`-lk` clips, the pair
  drawn smaller to fit), and docked, the LOK's probe in the LK's plate, on the orbit's map; the turn-around and
  docking between have no step and aren't shown. The launch and the climb show the LOK alone.
- **The Soviet Moon flights as themselves (user, 2026-10-20; `soviet_moon_parts`):** their tail stage is the Blok D,
  which BASPM counts as the rocket, and the steps give it the burns. The 7K-L1 rides it into the parking orbit
  (`soyuz-l1-deployment-stack`, `-earth-orbit-stack`), burns for the Moon on it and lets go (`soyuz-l1-tli`, `SEP`),
  passes the Moon, and comes home Soyuz's way with no retrofire (both craft's modules separation starts where the reentry
  preparations leave the Earth and sinks faster, `-modules-separation-moon`, `SEP_FROM`). The 7K-L3's LOK rides its Blok D all the way: alone on
  the flyby and the orbit flight, with the LK between them on the lander flights (the N1's stack, `-n1`, by
  `"later": "eva-7k-lok"`); the Blok D burns for the Moon and brakes into lunar orbit, and leaves there (the flyby's
  separation step on all three, the crewed orbit flight's retyped from its second lunar orbit: `soyuz-lok-blok-d-separation-moon`). In lunar orbit the
  Earth-orbit tests' close-ups have the Moon below (`-moon`): the crossing to the LK and back, the LK's release (whole,
  `soyuz-lok-release-lk-moon`), its descent test and staging (the lander test), the docking and the cast-off. On the
  Moon the LK stands on its legs (`lkm-`, a ladder down its right leg, `lk-walk.jsx`); the one cosmonaut plants the
  flag and gathers rocks (`lk-flag`, `lk-samples`), and the cabin lifts off its landing unit. The Blok D's braking of
  the LK down towards the surface isn't shown (the descent and landing show the LK alone). The descent and landing,
  one step, play Soyuz's descent and the recovery its landing and recovery.
- **The LK-700's Earth-orbit tests (user, 2026-10-19):** its own craft (`l7-`, `lk-700-*`, `lk700_parts`), drawn as
  the turn report's blueprint has it, 2.7 m across: the return capsule (a cylinder with two portholes and a hatch, a
  cone with two windows, a cap and a small dish), the return stage, and the landing stage with its engines (a big
  one and two small) and its legs folded (two of six seen). It launches under Soyuz's fairing and tower (assumed),
  rides the rocket's top stage into orbit (the second ascent, `-insertion`), and has its own maps. The way home had
  played Soyuz's orbital-module clips, which the LK-700 never had; it now goes: the landing stage's engines fire the
  burn (the capsule still on the return stage, `l7-r-capsule-stage`), the landing stage is cast off (the jettison,
  `lk700-jettison.jsx`), and the capsule leaves the return stage (the separation, `project-lk700.jsx`, on from the
  jettison: `modules-separation.jsx`'s `SEP_FROM`); then the capsule's own reentry, chutes and landing. Its spacewalk is three steps on its own hull (`l7-e-capsule`): out on the tether,
  along a handrail on the cone and back, and a photo and back in. Sizes from the blueprint.
  Its Moon flights (user, 2026-10-20; `lk700_moon_parts`, `lk-700-*`): BASPM gives every step from the parking orbit on
  to the craft, so its spacecraft is the whole complex: a trans-lunar stage of its own (`l7_tl_stage`, its shape ours)
  rides under it out of the rocket and through the parking orbit, burns for the Moon and lets go (`-tli`, `SEP`). The
  landing stage brakes into lunar orbit and lands (the braking stage folded into it), or fires the craft home on the
  orbit flight; on the Moon the craft stands on its six legs at the moonwalk's scale (`l7m-`), its three surface steps
  the flag, the rocks and the experiment package (in that order, so the package set out stays on the ground through
  the liftoff, `PACKAGE`; user), and the return stage lifts off with the capsule, climbs with nothing
  waiting in orbit and fires home (`-asc`). Home after a landing: the reentry preparations turn the capsule on its return
  stage and the separation follows (`project-lk700-moon.jsx`'s `SEP_FROM`); on the flyby and the orbit flight the
  landing stage is cast off at the reentry preparations, as on the Earth-orbit tests (`lk-700-retrofire-jettison`).
 
- **Gemini EOR's Earth-orbit tests (user, 2026-10-19):** the lander test in low orbit and the lander and heat-shield
  test fly their own hardware (`gemini_eor_parts`; our reading of the blueprints). The stack (`ge-`):
  Gemini's reentry module and retrograde section, the EOR service module in the equipment section's place (a ribbed
  cone, dark on one side), and behind it an open frame holding the Langley lander upright, its canopy towards the
  capsule; no tower on the rocket (ejection seats). The lander (`ll-`): an open platform on legs, two dark tanks
  either side of a clear canopy with its seat and pilot, one engine. The S-IVB tug (`sv-`) has its own launch
  (`tug-*`, the `atv` steps; a step 6 Earth orbit alone, by `"later": "launch-preparations"`). The steps: the
  spacewalk to the lander walks aft along a rail above the service module into the seat (`gemini-eor-transfer-out`,
  the stack drawn nose left; the step `eva-gemini-langley`, or an `eva` before the lander test), and back to Gemini's
  hatch after it (`-transfer-back`, an `eva` after `docking`; `crew-transfer.jsx` takes the rail's height and where
  the figure stands up at each end, `gemini-eor-transfer*.jsx`). The lander's release (`langley-deployment`) backs it
  out of its frame, the engine test and the restart (`langley-engine-test`, `langley-restart`) are two burns in the
  lander's scene, the second on from the first's last frame (the lander is one stage: no staging), `docking` flies it
  back in (the lander as the chaser), and the last `undocking` casts it off, empty. On the heat-shield test the stack
  docks nose first into the tug's ring (`lll-docking`), the tug burns to the high orbit and lets go after
  (`gemini-eor-btm`, `hoi-separate.jsx`). The way home is Gemini's: the service module and its empty frame go at the
  modules separation (`gemini-eor-modules-separation`, `ge-r-trunk`), then Gemini's retrofire, jettison and reentry,
  and its chutes and splashdown on two points (`gemini-descent-chutes`, `gemini-splashdown-and-recovery`). The
  heat-shield test's fast reentry from the high orbit plays as a normal one. The Moon flights (`gemini-eor`) are
  under the lunar Gemini programs' flights to the Moon.
- **Voskhod as itself (user, 2026-10-18):** Vostok's skin with Voskhod's parts in place (`vh-`, `voskhod-*`): the
  backup retro on the sphere's top in orbit, thrown off with the instrument module at the modules separation
  (`vh-r-backup`, `BACKUP`), which the orbital flights' retrofire jettison plays too, as
  Korabl-Sputnik 2's does (their configurations have no separation step; Vostok's suborbital flight, which has one
  after the jettison, keeps the burn's end there, by `later`; user, 2026-10-19); its soft landing,
  a flash under the base just before touchdown (`touchdown.jsx`'s `SOFT`, `project-voskhod.jsx`; the glitch's fires too
  high, hence its hard landing); the joint flight's time together as two Voskhods on the map. Voskhod 2's spacewalk in
  the spacewalk scene with the airlock standing off the sphere (`vh-e-capsule`, the hatch at its end): out through
  the airlock (no hand-held gun), filming (the hand camera only: the airlock's cine camera popped in, user, 2026-10-19; the stiff suit misses the shutter
  in the glitch, and both tries in the failure, `eva-film.jsx`), and back in with the suit ballooned (`eva-in.jsx`:
  bled down and in feet first; the glitch is Leonov's own, stuck and in head first; the failure fogs up too).
 
- **The way home in one piece (user, 2026-10-17):** the suborbital flight had no motes, so they popped in at the
  retrofire; now they stream past along its arc (`motes()` takes the craft's way, and can settle where the next clip's
  begin). Reentry preparations played the reentry preparations' turn even where the craft was already base first,
  after a suborbital flight's turn (Gemini, Voskhod, Vostok) or after the retrofire (Mercury's hop, two Soyuz types).
  There they play `reentry-trim` (after the turn) or `reentry-trim-burn` (after the burn, the Earth held where the
  retrofire left it): no turn, the jets trimming its attitude, and Mercury's periscope sliding in, as it did about half
  a minute after Freedom 7's retrofire. Glitch: the trim overshoots and rocks; failure: a jet sticks open.
- **The OKB-MiG E-154 (user, 2026-10-17):** it's the Soviet X-15 now (`design.md` §1), and flies the X-15's steps with
  its own clips (`e154-takeoff`, `-cruise`, `-drop`, `-climb`, `-peak`, `-fast`, `-descent`, `-landing`): the X-15's
  timelines and stories, with the E-154 from the user's art (a needle nose, a tall swept fin, four nozzles, wheels)
  slung under the belly of a Soviet four-turboprop bomber. Its strake tears away where the X-15 loses its lower fin; it
  lowers its wheels on the way down instead of dropping a fin (each leg on its own hinge, `e154-gear` and `e154-nose`:
  the nose wheel once swung down from the main gear's, user), and in the landing's failure they fold up and it slides
  on its belly.
- **The X-15 (user, 2026-10-17):** the drop had the X-15 climb straight through its carrier; now it runs on ahead, level,
  until its tail is past the carrier's nose, and pulls up there, while the carrier falls behind and the camera eases
  ahead. Its descent had a capsule's chutes, and its peak and its fast run shared one clip; each has its own now:
  `x15-peak` (above the air, no streaks, coasting over the top nose up to nose down, steering with its nose jets; the
  glitch's nose wanders and the jets bring it back, the failure departs into a spin), `x15-fast` (low and level over the
  clouds, fast streaks, a shock cone, its nose glowing; the glitch's belly gets a hot spot and it throttles back, the
  failure tears off its lower fin) and `x15-descent` (nose down through the clouds, pulling out, dropping its lower fin
  on a little chute; the glitch's nose bobs and is damped, the failure breaks up). The lower fin rides under its tail
  from the takeoff on.
- **A rover at Mars (user, 2026-10-17):** Mars 2's third season replayed its second's surface work; its surface step
  there plays `mars-rover` now (season 2 keeps `mars-surface`: a `mars-2` rule after `landing-and-deployment` first, then
  one with no step before). On the lander Mars 2 lands as (Viking's, in the replay), the arm sets PrOP-M down, and it
  walks off on its skis, its tether paying out. The glitch's front bar meets a rock: it steps back, sidesteps and walks
  round it; the failure stops at the rock and the lander falls silent.
- **Viking's separation (user, 2026-10-17):** its `lander-deployment-and-de-orbit` borrowed Venera's release; it plays
  `viking-separation` now: the Viking orbiter (an octagonal body, four wings of panels, the dish on a boom, tanks and
  engine above) lets the lander capsule go from under it, over Mars; the capsule turns side on, fires its deorbit jets,
  falls behind and below, and turns heat shield down for its entry. The glitch's jets cut out and relight; the
  failure's never relight, and it stays in orbit. Mars 2's `lander-detachment` plays it too (user, 2026-10-17): it
  leads into the same Mars entry, descent and landing.
- **The Mars orbit insertion, slower (user, 2026-10-17):** it felt rushed, and the probe jumped where the approach
  handed over. Every approach (`lunar-approach.jsx`, shared by the Moon, Venus, Mars, Jupiter and Saturn) now moves
  along its path as an orbit does, slow far out and faster as it falls in, the view closing in to keep it in frame,
  and arrives at the speed the insertion starts at (it used to all but stop first). The Mars insertion runs at
  `SLOW` 1.5 (8.0 s, every moment half as long again, the probe two thirds as fast), and the Mars orbit after it goes
  round at the same pace from where the insertion leaves it.
- **Pioneer Venus Multiprobe (user, 2026-10-17):** its six probe steps borrowed Venera's release, entry and
  descent and a Moon landing; they play the Multiprobe now, a large and a small clip each. The releases show the
  bus, a spinning drum with the large probe on its forward end and three small ones round it:
  `multiprobe-large-release` (its springs push it off along the axis; the glitch holds at the first try, the
  failure at both) and `multiprobe-small-release` (the bus spins up and flings the three off at a tangent; the
  glitch's North probe holds a turn longer, the failure's for good). The entries, `multiprobe-large-entry` and
  `multiprobe-small-entry` (North, Day and Night side by side), are cone-first, the radio silent in the heat and
  back after; the glitch wobbles and steadies, and the failure breaks up (the large probe, or North). The descents:
  `multiprobe-large-descent` (the chute pulls the aft cover off, the heat shield drops away, the chute is cut
  loose and the sphere falls free to the ground) and `multiprobe-small-descent` (the three keep their heat shields,
  no chute, and hit one after another; the Day probe goes on sending from the ground). None was built to land, so
  a success ends at the impact; the glitch is a spark at the temperature sensor (all four lost theirs near
  12.5 km) and a weaker signal after, and the failure's signal stops there.
- **Earth departure (user, 2026-10-16):** it borrowed `tpi`, so the injection's burn played twice; BASPM gives the
  step to the probe alone. `earth-departure` is the probe close up after the burn, the Earth shrinking behind
  it: puffs spin it up, its boom unfolds, its dish turns home, and a tracking pulse goes out and comes back. The
  glitch's boom sticks and a jolt frees it; in the failure it stays stuck, the probe wobbles, the dish hunts
  and the Earth's pulses get no answer.
- **No clip yet:** a step with no clip, or a variant missing, plays `no-signal.webm` on a loop (an
  antenna searching, outcome-neutral) with "No signal" in the bezel; its result shows after a
  1.5 s pause, and playback moves on at 3 s. If no video can play at all (no file, no native
  decoder), the monitor shows a still drawing with the same label and the same timing.
- **The result shows at `revealAt`:** until then the roll line, the outcome box, the saving roll's
  figure, the step's word in the list, and "Prestige so far" and the season counts stay as before
  the step. A step reached before shows at once.
- **The step list is the plan:** every step the configuration plans for that season, so the count
  ("Step 2 of 6") gives nothing away; after a failure the steps not flown read "Not flown" in the
  summary.
- **Playback:** a Play/Pause button joins the title buttons (restart, previous, Play/Pause, Next
  step, Skip to summary). Playing, the next step starts when a clip ends. Paused, the step's clip
  still plays through once and shows its result; only the move on waits. Next step, a click in the
  list and Skip to summary reveal what they pass. A replay opens playing.
- **The queue:** opened from the dialog (§4.4), the replay plays every mission flown that turn, in
  the report's order. The season summary then offers "Next mission" or, after the last,
  "To the turn report" (the front pages first, if any). Back leaves the queue for the report.
- **Proposed defaults (unconfirmed):** the 1.5 s and 3 s timings; the console's label;
  that the dialog offers only the newest turn's missions when several reports are unread.
- **The console (the user's idea, 2026-10-13; mocked up on the canvas `Launch Window — Replay
  Console` and built the same day, `client.screen.ReplayMonitor`, the instruments' motion in
  `client.view.ReplayConsole`):** the monitor is a mission-control console. The user chose the
  layout most boards show (picture left, instruments stacked in a column on the right) and set
  aside the canvas's two other variants (side-by-side instruments on the shortest console, the
  picture on the right).
  - **Layout:** a 26 px label strip on top: a signal lamp (gold with a ring while the camera is live,
    dark on "No signal"), the label, and on the right "AIR-TO-GROUND" with a history note on the Quindar tones (user's text,
    2026-10-18; the icon in cream on the panel), "PAUSED · NEXT STEP WAITS" left of it while paused. The
    16:9 picture takes the width but a 280 px column, capped by the height above a 53 px row of step
    lamps (the plan's steps: flown cream, current gold with a ring, the rest dark outlines; "3 OF 11";
    the mission's name on the right). The column takes the rest and always stacks three panels: the
    mission clock (with a HOLD lamp), the status gauge (with CAUTION and MASTER ALARM lamps under it)
    and the telemetry bars (PRS, TMP, PWR), the last filling the height left. At 1920 × 1080 the
    console is about 1086 × 430 to 520; the picture is never cropped or stretched. On a console too
    short for all three bars the ones that don't fit are left out.
  - **Mission clock:** "T+ 0d 0h 2m 23s", the clips played so far this season (frozen, dimmed and HOLD
    lit while paused; it jumps on a skip). *A show clock for now (user, 2026-10-13):* a later pass over
    the mission steps gives each step a mission time to start from (`design.md` §5, Backlog).
  - **Status gauge:** a 180° dial, Nominal to 0.55, Warning to 0.80, Critical to the end, 20 ticks,
    a cream needle and a gold peak-hold pointer (the highest reading this step). Before `revealAt` the
    needle wanders 0.22 to 0.38 (two waves, 2.3 s and 0.9 s, phases from the mission and the step's
    place, **never the outcome**; `ReplayConsoleTest` holds every instrument to that). At `revealAt`:
    success eases to 0.30 in 0.6 s and wanders at half the swing; a glitch swings to 0.66 in 0.4 s,
    CAUTION lit; a saved glitch swings to 0.68, CAUTION flashes three times, holds 0.8 s and falls
    back into Nominal in 0.8 s, the peak-hold left in Warning; a failure runs into Critical (0.88) in
    0.5 s and quivers, MASTER ALARM lit; a catastrophic failure hits the end stop in 0.3 s, bounces
    twice and stays pinned, MASTER ALARM flashing and CAUTION lit. The outcome's range brightens. No
    signal: the needle rests at 0, dimmed, no peak-hold, the ranges at 25 %. A step reached with its
    result shown starts settled, with the swing's trace on the peak-hold.
  - **Telemetry:** bars drift between 0.45 and 0.65 before the reveal, with marks at 0.35 and 0.75.
    Success settles them on 0.55; a glitch takes TMP to 0.84 in gold (and back, if saved); a failure
    drops PRS to 0.06 and raises TMP to 0.90, both red; a catastrophic failure empties all three, their
    tracks outlined red; no signal leaves them empty with dashed tracks.
  - **Drawn by the game:** flat rects, circles from `Theme.Shapes.circle` (built at twice their size,
    so thin rings stay round), and lines and arcs drawn with `Theme.Shapes.brush`, a texture that
    anti-aliases a rotated line. The console's colours are the design system's, premixed on the navy
    (constants in `ReplayMonitor`); no new tokens. Live means a clip of the step's own is playing: a
    client with no video decoder shows the dead console, as it shows "No signal".
  - **Unconfirmed:** every timing and reading above (from the canvas's notes), the wander's split
    between its two waves, the bars' periods (3.1, 2.7 and 3.7 s), and the clock.
  - **Shots:** `-Dshots=console` renders the console alone in every state; the replay group has
    Vostok's launch live, while it plays and after (both with a stand-in video player that shows one
    frame of the launch clip).
- **Reviewing the clips (the user's idea, 2026-10-14):** dev-tools' `MissionReplayer` (`mvn -pl dev-tools compile exec:exec@replayer`) plays any mission season in this screen without a game: a list of every season with how many of its steps have a clip, then a step (or every step) and an outcome. It plays a made-up report (`ReviewReport`), so nothing is rolled; keys 1 to 4 switch the outcome at the step showing, R plays it again, Esc goes back to the list. The clips, sidecars and sounds are read from `assets/` before every replay, so a new render plays without a restart.
- **Sound (the user's idea, 2026-10-13; built the same day, `client.screen.ReplayAudio`):** nothing is
  baked into the videos, so sound can run on across steps. Four layers:
  - **The room, the whole replay long:** mission control's room tone, a quiet loop of radio chatter
    (the user's own mix of NASA recordings). It runs on across steps and missions in the queue, and
    through the season summary; leaving the screen stops it.
  - **A fanfare at the season summary** (the user's decision, 2026-10-13, instead of background
    music, which was hard to get right without it grating): `fanfare-failure` when the mission failed
    (its last step a failure or a catastrophe), `fanfare-success` for anything else, glitches, solved
    or not, included, and for a season that ends with the crew still aboard. It plays once, whichever
    way the summary is reached (played on, skipped to, clicked), again if the player comes back to
    it; going back to a step fades it within 0.3 s. A catastrophe shares the failure's fanfare.
  - **The console's sounds, from `ReplayConsole`:** a telemetry blip while a camera is live, one of
    three takes at random, every 2 to 4 s at random, the softest sound in the replay (user, 2026-10-18;
    none once telemetry is lost), the CAUTION tone each time its lamp lights (three for a saved
    glitch), the MASTER ALARM for 3 s after a failure's reveal (the lamp stays lit), quiet static under
    a step with no signal, a click on Play and Pause. A lamp already lit when the step is reached stays
    quiet. The same rule as the lamps: nothing before the reveal depends on the outcome
    (`ReplayConsoleTest`).
  - **The clips' cues**, in each sidecar beside `revealAt`: `"cues": [{"at": 0.4, "sound":
    "engine-roar", "volume": 0.8, "loop": true, "until": 5.8, "fade": 0.8, "carry": true}]` (all but
    `at` and `sound` optional). A loop stops at `until` (or the clip's end) after its fade; `carry` on
    a loop at 0 keeps the same sound running from the clip before instead of starting it again. A
    clip's variants have the same cues before the earliest reveal (`ReplayClipsTest`); what counts is
    what's heard, so a loop whose `until` is at or after the reveal is the same as one that plays on
    (2026-10-14). The cues are kept in `assets-raw/replay/cues.json` and written into the sidecars by
    `cues.py` beside it, which the clip builders run on publish. The first is `rocket-roar`
    (2026-10-14, the user's find): under the launch from ignition, the ascent's two stages (carried on
    from the launch, the upper stage quieter) and the retrofire, each cut with its engine in a failure.
    **Unconfirmed:** its volumes and fades. **Effects** (2026-10-21, the user's finds and where they go,
    `assets-raw/replay/sounds/make_effect_sounds.py`): `rcs-puff` as each thruster puffs (a few while a
    jet is stuck open; quieter for a course correction's puff on the map), `hatch-open` as a spacewalk's
    or a transfer's hatch swings open and as Voskhod's airlock vents, `fueling` while the tanks fill,
    `vent` from the tanks topped off through the countdown to the launch's ignition, `reentry-plasma`
    while an entry's glow shows (and the rocket plane's fast run), `splash` at splashdown, `helicopter`
    from just before it flies into the recovery, `explosion` at every blast (quieter on the map). Four
    more, the user's own from Stable Audio 3: `decoupler` as springs, bolts, straps or latches fire and a
    stage falls away, `explosion-debris` for a blast near the ground (the pad, the Luna landers),
    `servo` as an arm, a dish or a panel moves, `deploy` as booms, whips, masts, ramps, petals, panels or
    gear unfold. Their
    times are read off the clips' built projects by `assets-raw/replay/effects.py`, rerun after a clip
    is published with a changed timeline; it skips a clip whose project is newer than its render. Before
    the reveal an effect only some variants have waits for the reveal if it's within 0.35 s of it, and is
    left out if not. **Unconfirmed:** their volumes, and the 17 steps skipped as stale (their clips get
    no effects until they're published again).
  - **The pool:** `assets/content/replay-sounds.json` (outside the pack, like the clips), each sound an
    id, a kind (effect, loop, radio, music for the fanfares, room), a volume, its takes (several, dealt with no repeats; takes of one line count as one, so two calls in a row never say
    the same words, and a line's takes take turns: `lines` in the catalog, 2026-10-21) and a description that is the brief for making it. A sound with no takes yet plays nothing.
    Files go under `assets/audio/replay/`; loops must be OGG or WAV (MP3 pads its ends, so it can't
    loop seamlessly). `ReplaySoundsTest` holds every take to being there, in the catalog and, where
    it isn't the project's own, credited.
  - **Radio voices** are streamed. English for both sides (user, 2026-10-13). The first are the user's
    (2026-10-14): flight-controller calls made with VibeVoice, the radio's noise and the Quindar tones
    already in them (`assets-raw/audio/chatter/`), `radio-*` sounds whose takes are one line's
    variations or lines for the same moment. More from 2026-10-15, made by Claude with the user's
    workflow, screened by a transcript (Whisper) and picked by the user's ear: 51 sounds in all. Round 2
    (2026-10-21, the user's ask): a call for every step and outcome, from a pool of two lines or more, so
    steps in a row don't say the same words (spacewalks and moonwalks have four): 123 sounds, 802 takes,
    on every variant of every clip but the runway landing's failure (`assets-raw/audio/chatter/CALLOUTS.md`).
    Placed in `cues.json`: a call 0.05 s after its own variant's reveal (the line confirms what was
    just seen), on the glitches and, from 2026-10-15, the failures of most steps (a later start where
    an opening call would hold it up past the wait); a few calls that tell nothing before the earliest
    reveal in every variant ("Guidance is internal", "Max-Q", "Watching it", the reentry's "We're in
    blackout"). A call may run on into the next step. **One voice at a
    time:** a call due while another talks waits up to 1.5 s, and is dropped if it can't start by then
    or its step ends. Lines that fit no clip yet ("engine out", "second stage ignition late", a late
    drogue, a safe shutdown on the pad, the escape tower, which needs to know a crew flies) are in the
    pool, unplaced. **Unconfirmed:** the placements, the 0.7 volume, the 1.5 s wait.
  - **Playback:** a step that follows by itself lets the last one's loops end as their cues say; a
    skip or a step back fades everything but the room within 0.1 s. After the last step, the summary
    and its fanfare wait until the radio is done (a call talking or waiting for its turn), the clip
    holding its last frame, at most 8 s (user, 2026-10-15); skipping to the summary still cuts it off. Silent when sound is off, checked
    every frame. Volumes: the sound's own, times the player's setting (§4.12).
  - **Unconfirmed:** the blip's level (0.25), the 3 s alarm, the static's level, the default volumes (0.7), the
    fanfares' volume (0.8) and their 0.3 s fade.
  - **Placeholders (user, 2026-10-13):** a sound plays a synthesized `placeholder-*` take until its
    real one comes (`assets-raw/replay/sounds/make_placeholder_sounds.py`). Every sound is real since
    2026-10-13: the console's from Pixabay, the room tone the user's mix of NASA recordings, the
    fanfares the user's, made with Stable Audio 3 (all credited). The script stays for any new sound.

Choices made while building them, **confirmed by the user (2026-10-06)**:
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

*Play-test additions (user, built 2026-10-05), on both R&D boards (payload and rocket):*
- **Projected gain.** A lighter navy segment after the reliability bar's fill shows what the
  current team adds in an average season, and the caption says "about +4.2 next season"
  (`ResearchPlanning.expectedSeasonGain`: `ResearchProgress`'s pass with each slot's band
  midpoint; a test checks it against 4,000 simulated seasons).
- **Auto-assign** (a card button) seats the four best engineers for the component, best in the
  richest slot: those already on it plus anyone idle (`ResearchPlanning.bestTeam`). It releases
  whoever is in the wrong slot or not in the team, then assigns the rest, one action after
  another (`PayloadRnd.autoAssignActions`). Engineers busy elsewhere aren't touched.

**Implemented in libGDX (2026-10-04)** from the SET Centre mockup (overview and
Payload R&D) and the older Personnel Recruitment mockup (`client.screen.SetCentreScreen`,
`PayloadRndScreen`, `HireScreen`, on a shared `ChromeScreen` with the header, footer,
toast and dialogs). Decisions made while building them, **all confirmed by the user
(2026-10-04)**:
- **Pull out copy corrected.** The mockup says Permanent earns no partial credit, but
  the rules credit the current round of Basic, Advanced *and* Permanent; only Expert
  gets nothing. The dialog now says "They keep partial credit for the round in
  progress." The mockup should be fixed to match.
- **Training lengths and limits come from `core`**, not the mockup: Advanced is
  `ADVANCED_TRAINING_SEASONS` (Basic left the dialog on 2026-10-05), and Permanent's
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
*Sort (the user's request, 2026-10-10; it had never been built here):* Mission Control's sort buttons
right of the filter chips, one per skill, best first; clicking the active one goes back to roster
order. Shot `ac-sorted.png`.

**4.9.1 Personnel overview** (component 11) for astronauts, showing who is on a
mission (crew or CAPCOM) and Expert stars, with sub-screens Hire and Manage
department.

**PR duty (built in libGDX 2026-10-06, `design.md` §2.2(c)):**
- An idle astronaut's row has **PR duty** next to Train; one on tour has **Recall** instead, and
  the badge reads "On PR tour · +17 prestige / season". A filter chip "On PR tour" exists only
  here, not on Mission Control.
- The strip's first stat is "On missions / PR tour" with the PR income under it.
- **The PR duty dialog:** what the tour does, a history note (the *Life* deal for NASA, Gagarin's
  tours for Soviet), the yield's parts (Leadership, flights and firsts, any cap), what the
  astronaut would earn on tour and the agency's PR income before and after, an info notice on
  diminishing returns when others are already on tour, a warning on rust, and Cancel / Send on
  tour.
- **The turn report's People column** names each tour and its prestige, or the press story's
  headline and text when the papers picked it up, one sentence per effect the story had ("3 more
  SET candidates in the pool for two seasons.", `TurnReportView.pressEffect`), and the rust. Lines with detail text now show
  it under the title (also the lost passenger's).
- **The Public Affairs Office** shows "PR duty: +N / season" under the prestige.

**Mocked up (2026-10-04)** as `AstronautComplex` on the `Launch Window — Astronaut
Complex & Public Affairs Office mockup` canvas; `design.md` §3.13 has the writeup.
Choices **confirmed by the user (2026-10-06)**:
- **Decided (user, 2026-10-04): Expert training is a fourth kind in the Training dialog** (then Basic,
  Advanced, Permanent, Expert; Basic left the dialog on 2026-10-05), not a separate row action. Only skills at
  80+ that aren't Expert yet can be picked. The dialog shows the per-season chance
  (25 % at 80 up to 35 % at 100), no skill growth, and nothing on pull-out.
- An idle astronaut with such a skill gets a gold "Can train toward Expert in …" hint.
- The building panel (component 7) is a compact cell at the end of the department
  strip: level, cap, upkeep, and Upgrade with a confirmation.
- "Near retirement" is within 5 years of 50, as the libGDX SET Centre does for 65.

**Implemented in libGDX (2026-10-04)** as `client.screen.AstronautComplexScreen`, with its
wording in `client.view.AstronautCorps`. Hire, training, pull out, dismiss and Manage
department are the shared department pieces (`Department.ASTRONAUT`). Choices made while
building it, **confirmed by the user (2026-10-06)**:
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

**Actions:** **Convert prestige to funds** (only while in debt, at 2 funds per prestige
point, decided 2026-10-06, a part point rounded up; capped so funds end at no more than two
seasons of running costs). Show the cap and the price.

**States:** in debt, where new spending is refused; a temporary cost or R&D
multiplier is active (random event, n seasons left).

**Mocked up (2026-10-04)** as `PublicAffairs` on the `Launch Window — Astronaut Complex
& Public Affairs Office mockup` canvas, with the three states on mockup chips (the debt
one is a labelled what-if). Every number is `core`'s on the fixture; `design.md` §3.13
has the writeup. Choices **confirmed by the user (2026-10-06)**:
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
**confirmed by the user (2026-10-06)**:
- **The game's own schedule (built 2026-10-06):** reviews and conversions are priced with the
  budget schedule the game info carries (`GameInfoResponse.budgetSchedule()`), so a game with
  another schedule shows its own numbers.
- **"Last season"** under the funds is the latest turn report's funds change, fetched
  quietly; it's left out when there's no report. The footer delta stays deferred.
- **The review scale** has no tag above the gold mark; the legend names both marks.
- **The footer's funds** show the danger tag when negative.
- **A credit for rivals' Firsts (built 2026-10-12, `design.md` §2.1(e)):** the review's bracket ends, its
  pay and the gold mark already include it (`ReviewForecast` shifts the bracket by it, as by the catch-up
  shift); a green caption above the last-review line says how much the review adds. Shots
  `rival-firsts-pao.png`, and `rival-firsts-help.png` for the help topic's new paragraph.
- **Raises between reviews (built 2026-10-12, `design.md` §2.1(f)):** the funding line reads "set by
  review #n, raised since" after a raise, and a caption above the last-review line says what 1,000 more
  prestige would add now (or that the bracket's top is reached, or, before the first review, that raises
  start then), and the raise due next season when prestige has already grown. Shot `raise-pao.png`.

### 4.11 Museum: goals, history, rivals
**Purpose:** the scoreboard and the archive. **Ref:** `museum*.png`.

**Tabs:**
- **The race:** the declared goals as goal badges, own progress, and every
  player's progress side by side, by agency name and director name: which goals they
  hold and the turn they got them. **Needs server work:** G3. A filler bot (`design.md` §5) has
  "(bot)" after its agency's name here, in the Intelligence Office's rival list and in the game
  result (2026-10-25, `Museum.name`); the newspaper names it plainly, as it would any agency.
- **Achievements:** every goal achieved or cancelled, with the turn and the
  prestige.
- **Flights:** the completed flight count per configuration (`successfulFlightCounts`).
  *Built 2026-10-06:* a card whose configuration really carried animals that came home names
  them under a green "Came home" eyebrow once flown (Belka and Strelka; Ham; Enos; Chernushka,
  then Zvezdochka on a second flight). See `design.md` §2.5(f). Each name, or each pair
  photographed together, sits on a row with its 48 px greyscale photo; an animal with no
  licensed photo gets an empty square in the faint navy tint so the names stay aligned
  (2026-10-06).
- **Memorial:** astronauts lost.

- **Encyclopedia** *(built 2026-10-06)*: one card per explainer (component 18), with the first
  sheet's drawing, the drawing number and sheet count, the title, a one-line summary and "Read
  it". An explainer opened here closes back to this tab. *(Specified 2026-10-07:)* a card whose
  explainer the player hasn't opened yet carries the gold "new" dot of component 19.
- **Archive** *(specified 2026-10-15, §4.19)*: the real missions' pages, unlocked by goals.

**Mocked up (2026-10-04)** on the `Launch Window — Museum, Settings & Game Result mockup`
canvas, one artboard per tab (`Museum`, `MuseumAchievements`, `MuseumFlights`,
`MuseumMemorial`); `design.md` §3.13 has the writeup. Choices **confirmed by the user (2026-10-06)**:
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
- **The memorial's history note** (Bondarenko) was checked on 2026-10-05: the chamber's air was
  oxygen-rich (at least 50 %), not pure oxygen, and the fire came ten days into a fifteen-day test.
  The note was corrected to match.

### 4.12 Settings (header gear)
Log out; server address; sound on or off; replays (§4.4); about and version.

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
filled in (a bad address shows the Connect screen's own error under the field). Sound was
two disabled segments until the game had sound; *since 2026-10-12* it's On (the default) or Off, for
everyone on the device, since the splash plays before anyone logs in. *Since 2026-10-13* two sliders
under it set the replay's music (its fanfares) and sounds (§4.7.3, "Sound"), 70 % by default, for the device; the
voices at the start and on joining play at full volume. Replays: Ask, Always or Never,
per account (§4.4). **Confirmed by the user (2026-10-04):** the hash is shortened to 8 characters; the
"connected" label is fixed text; and the account note says an unlaunched mission plan is
dropped (plans live on the client session), instead of the mockup's "logging out loses nothing".
**Credits (built 2026-10-06, user decision):** About has a "Credits" button that shows
`assets/CREDITS.md` in the dialog (headings, paragraphs and bullets, scrolling past 520 px),
with Back and Done. A parenthesis in a script the fonts lack (the archive's Russian name) is
left out, since its English name stands next to it.

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

### 4.14 Person page *(specified 2026-10-07 for `design.md` §2.2(d), mocked up and its phase 3 part built the same day)*
**Purpose:** everything about one employee, so the player gets to know them. Opened from a
personnel row (component 11), from a name in the turn report, and from staffing pickers.
Phase 3 needs the top half; the rest arrives with phase 4.
- **Header:** portrait, name, group, age, salary, morale (bar), state (idle, training, R&D on X,
  on mission Y), and for later, **loyalty** (bar).
- **Skills** with their bars, Expert marks, and learning potential if shown elsewhere.
- **Traits** (component 20), with "still getting to know them" or "nothing more to discover".
- **Relationships** (component 22): works well with / clashes with, each with a portrait and how
  long they've worked together.
- **Ambition** (phase 4): what they want, "N seasons left", or "fulfilled" / "disappointed" (with
  seasons remaining), or nothing for someone without one.
- **Career record** (phase 4): a timeline, newest first: hired (with their hint), training done,
  each R&D stint (component, seasons, reliability gained), each flight or console shift (mission,
  outcome, goals), firsts, traits revealed, relationships formed, ambitions met or missed.
- **Actions:** the row actions of component 11 (Train, Expert training, Dismiss).
- **For a memorial entry,** the same page, read-only, ending with how they died.

**Mocked up (2026-10-07)** in four states (see the note after component 22). Choices, all
**unconfirmed**:
- **Three columns:** who they are (128 px portrait, group, age and retirement, state, salary, morale,
  loyalty, Train and Dismiss), what they're like (skills with learning potential, traits,
  relationships), and their story (ambition, career record).
- **A new hire** shows "Still getting to know them" and the colleague's hint they were hired with.
  Relationships: "None yet. A relationship shows once two people have worked side by side long
  enough to really get on, or not."
- **Phase 4:** loyalty as a navy bar under morale; an Ambition card with the goal, a gold "5 seasons
  left" badge and when it was said (left out for someone without an ambition); the Career record as
  a timeline, newest first, gold dots for traits, relationships and flights. The veteran shows them
  with placeholder content; the other states mark the room with dashed boxes.
- **Memorial:** reached from the Museum, the same page with a greyscale portrait, "Served 1959 to
  1961", skills at the end, no state, salary, morale or actions, and a "How it ended" card under
  the identity card.
- *As built (2026-10-07, People canvas):* `client.screen.PersonScreen`, opened from roster names,
  turn report lines and relationship names, with Back to wherever it came from. Two columns
  for now: who they are (portrait 128, group and role, age line, state, salary, morale, the row's
  actions) and what they are (skills with Expert marks and learning potential, traits, and
  relationships with portrait, kind, name, what they do now and seasons together). The hire hint
  shows while the player is still getting to know them. The right-hand column (ambition, career
  record), the loyalty bar and the memorial's read-only page arrive with phase 4: a memorial entry
  keeps no skills, traits or id yet (`design.md` §2.2(d)). The mockup's "Mockup only" switchers and
  dashed phase 4 boxes aren't built.
- *Phase 4, as built (2026-10-07, `design.md` §2.2(d)):* a third column, the story: the Ambition
  card (what, a gold "N seasons left" badge, how it comes true, when it was said) or a red
  "Disappointed" badge with the seasons left, then the Career record, newest first, the running
  R&D stint on top as "so far", gold dots for traits, relationships, flights, firsts, the Expert
  trait and ambitions met (`client.screen.CareerCards`, `client.view.CareerTexts`). A navy loyalty
  bar under morale, with a tooltip. Roster rows show "Ambition · N seasons left" (gold) or
  "Disappointed · N seasons" (red) under the traits. The turn report's People section has a line
  for each ambition said, met, passed over, impossible or given up. **The memorial's page**
  (`MemorialPersonScreen`, from an "Open the page" link on a plaque that has one): who they were,
  "Served 1959 to 1961", how it ended, skills at the end, the traits the player knew, and the
  career; the portrait is dimmed rather than greyscale. Entries from before phase 4 keep the plain
  plaque.

### 4.15 Intelligence Office *(specified 2026-10-07 for `design.md` §2.9, Intelligence; mocked up and built the same day)*
**Purpose:** the agency's intelligence service: how big it is, what it does this season, and
everything it has learned about rivals. An **eighth hub building** (polygon `INTELLIGENCE_OFFICE`,
drawn by the user on both hub images), with a marker and footer icon of its own (proposed:
binoculars, our own drawing; on the UI Asset Sheet since 2026-10-08). All rules and numbers are in
`design.md` §2.9; the screens read them from the rules' constants (G7).

**Overview** (the landing screen):
- **The level:** the current level (0 to 3, with its name: none, Security office, Analysis
  section, Field network) and the **target level**, set with a stepper or four segments. When
  they differ: "Reaches level 3 in 2 seasons", one step per season. The stepper says changes take
  effect next season and move one step a season.
- **Costs:** cost per season now, and at the target level; what the way there costs.
- **This season's actions:** "1 of 2 used", each used one named (a background check on a
  candidate, an assessment queued).
- **What each level brings:** the four levels as a ladder (component 7's look may fit), each with
  its actions, unlocked operation and passives; levels above the current one dimmed, the target
  marked.
- **Counter-intelligence:** its strength (I to III) and agents caught this game.
- An empty state for level 0: what the service would do, and the target stepper.

**Operations:** pick an operation, then its target:
- **Agency assessment** (level 2): a rival agency (name, director, archetype), then an area:
  R&D, programs and plans, or personnel. Shows the success chance: exact, with where the rival's
  level comes from and its date ("55 % · their counter-intelligence was level 2 as of Spring
  1962"), once a dossier or a catch has shown it, otherwise "40–85 %, depending on their
  counter-intelligence" (`design.md` §2.9). Then what a failure risks (getting caught, −20 prestige),
  and when the answer comes ("in the next turn report").
- **Personnel file** (level 3): a rival, then a group, or a person named in an earlier file.
- Queued operations this season, cancellable until the resolution. The background check isn't
  here: it lives on the hire screen (component 14).
- Last season's results, linked to their folders.

**Folders:** one per rival agency, a list on the left, the open folder on the right:
- **Header:** agency, director, archetype, goals reached (as the Museum's rivals show), and
  their Intelligence level if known, dated ("Level 2 as of Spring 1962", learned from a dossier
  or a catch), else "Intelligence level unknown".
- **One dossier per area** (R&D, programs and plans, personnel), each stamped "As of Spring
  1962" (no visible decay, the date says how old it is); an area never assessed says so. R&D shows the rival's
  components with reliability as ranges (component 3 with a range band rather than a value);
  plans list open programs, missions in flight and next season's scheduled launches, with "may
  have changed since".
- **Personnel files:** a small card per person (portrait, name, skills as ranges, loyalty band,
  ambition, one trait as component 20).
- **Tidbits:** the passive finds, newest first, each dated.
- **Caught:** agents of yours this rival caught, and theirs you caught.

**Hire screen addition** (component 14): see there.

**Turn report:** an "Intelligence" section (level changes, reveals, dossiers, tidbits, catches),
each line linking to its folder.

**Mocked up (2026-10-07)** on the canvas `Launch Window — Intelligence Office` (the Artifact tool's
list finds it): the overview at level 2 climbing to 3 and at level 0, operations, the folder of one
rival, the hire screen with the background check (an action free, the action used), and a sheet of
the new pieces. Not mocked up: the turn report's Intelligence section. Choices the spec left open,
all **unconfirmed** (each row's yellow note lists them):
- **Tabs** Overview, Operations, Folders; at level 0 Operations is locked and Folders stays open,
  empty.
- **Overview:** the target as four segments with − and + (current navy, target framed gold), a gold
  "Reaches level 3 in 1 season" badge; costs now, at the target and on the way; the season's
  actions as a numbered list, each used one named; the ladder top-down (3 to 0), current solid,
  target dashed gold, locked rungs dimmed with a lock on what they unlock; counter-intelligence as
  three pips with the catches both ways; a history note card (the N1 seen from orbit, §1.1).
  Level 0 explains the service in three lines (own people, rival agencies, rivals' people).
- **Footer:** a sixth chip with the binoculars, "L2 → 3 · 1 / 2" (level, target when it differs,
  actions used), opening the office.
- **Operations:** one card walks operation, rival, area; each rival row shows its chance (exact
  with source and date, or "40–85 %, depending on their counter-intelligence"). An odds card shows
  Succeeds, Caught (failure × catch chance) and Answer, and a red box what being caught costs and
  teaches each side. Queued operations have Cancel; last season's results link to their folders.
- **Folders:** the rival list shows each rival's level line and newest dossier, with the gold new
  dot; the header gives the level with date and source. Dossiers date independently. A personnel
  file shows the three best skills as ranges, with loyalty and ambition marked as phase 4.
- **Pieces:** the binoculars icon (24 px grid, 2 px round strokes; to become
  `assets-raw/svg/ui/binoculars.svg` and `assets/ui/icons/binoculars.png`, and go on the UI Asset
  Sheet), the hub marker in the other markers' style (checked on the brightest and darkest hub
  art), the reliability bar as a range band (navy at 42 % with solid ends, the gold cap kept), the
  dossier stamp ("As of Spring 1962", a dashed "Never assessed"), the "may have changed since" tag,
  and the background-check states. No new colour tokens.

*As built (2026-10-07):* `client.screen.IntelligenceOfficeScreen` (tabs in the title row, the title
naming the tab), `client.view.IntelTexts` for every line, `ReliabilityBar.range`, the
`binoculars` icon (`make_ui_icons.py`), `Hub.Place.INTELLIGENCE_OFFICE`, the footer chip, the
report section (`TurnReportView.intelligence`) and an "Intelligence Office" budget line. Differences
from the mockup: the plans dossier shows R&D focus and hardware not flown yet instead of next
season's launches (`design.md` §2.9), and personnel files show the three best skills, then loyalty
as a band and the ambition the person has said (phase 4; never one still hidden). Last season's
results come from the
latest turn report when the client has it.

### 4.16 Poaching *(specified 2026-10-08 for `design.md` §2.2(d), phase 5; mocked up and built the same day)*
**Purpose:** hiring a rival's engineer or flight controller away, and keeping your own. No new
building: offers are made from the Intelligence Office's personnel files; the defence lives with
each department and on the person page. Rules and numbers in `design.md` §2.2(d) (all unconfirmed);
the screens read them from the rules' constants (G7). Never for astronauts: an astronaut's file
shows no offer button and no explanation is needed beyond a tooltip.

**One approach per person** (2026-10-09, `design.md` §2.2(d)): once someone has answered your
offer, the file card shows "You made them an offer in [date]. They won't hear another from you." in
place of the button, like the other reasons someone can't be approached.

**Making an offer** (Intelligence Office, Folders):
- **The personnel file card** (§4.15) gets a **"Make an offer"** button when the person can be
  approached, else a short reason instead ("Astronauts can't be approached", "With the agency under
  4 seasons", "Approached recently"). An old file still works; its date shows. One offer a season: once used, the others say "Your offer this season goes
  to …".
- **The offer dialog:** the person as the file knows them (portrait, name, group, skills as
  ranges, loyalty band, ambition and disappointment, the known trait, the file's date), then the
  **raise** as three segments (+25 / +50 / +100 %), and for the picked one:
  - **What it costs if they come:** new salary per season, the signing bonus now, and the roster
    room it needs ("SET Centre: 7 of 8").
  - **Their answer, as far as you can tell:** a chance range from the loyalty band and what the
    file shows ("15–30 %"), with a line that the rival may match it and that you won't know their
    matching rule. Never an exact number: the loyalty is only a band.
  - **A history note** after the title (2026-10-21, `HistoryNotes.POACHING`, `design.md` §1.1): both
    sides began with poached engineers, Paperclip and Osoaviakhim. `-Dshots=poaching` renders the dialog.
  - **When the answer comes:** "In the next turn report."
  - Buttons: **Make the offer** (queues it), Cancel.
- **Queued offer:** listed on the Operations tab with the season's operations, cancellable until
  the resolution; the file card shows "Offer queued: +50 %".
- **Overview and footer chip:** "Offer: 1 of 1 used" next to the actions (it isn't an action).

**Keeping your people:**
- **Agency default, per group:** on the SET Centre's and Mission Control's personnel overviews
  (component 11), a small **Retention** card: "Match rival offers up to" with four segments
  (Never / +25 % / +50 % / +100 %), a line on what a match does (their salary rises to the offer
  for good) and how many of the group are set otherwise. Astronaut Complex: nothing.
- **Per person:** on the person page (§4.14), under loyalty, a **Retention** row with three
  segments: **Agency default** (naming it, "matches up to +25 %"), **Keep at any price**, **Let
  go**. Engineers and controllers only.
- **Roster rows** (component 11): a small mark for "Keep at any price" and for "Let go", none for
  the default.

**What the player learns:**
- **Turn report**, People section: offers received ("NASA offered Ivan Kozlov +50 %: matched, salary
  now 54" / "turned it down" / "left for NASA"), and offers made ("Your offer to … :
  joined you" / "their agency matched" / "turned it down" / "lapsed: no room in the SET Centre" /
  "lapsed: no longer there"). Each name links to the person page (a person who left links to
  nothing, or to their page in your roster when they joined you).
- **Person page career lines:** "Joined from …", "Turned down an offer from …", "Stayed: the agency
  matched …'s offer".
- **A newcomer from a rival** shows "Joined from Soviet Space Agency" on the person page and roster,
  and the one trait the file showed.

**States to mock up:** the file card (can be approached; can't, with the reason; offer queued;
offer used on someone else), the offer dialog at +25 % and +100 % (one with room, one with the
roster full), the Retention card on a personnel overview, the person page's Retention row (default
and "Keep at any price"), roster rows with both marks, and the turn report lines (received:
matched and left; made: joined and turned down).

**Mocked up (2026-10-08)** on the canvas `Launch Window — Poaching` (the Artifact tool's list finds
it), a Soviet player with NASA as the rival: the folder with four file cards, the offer dialog at
+25 % and at +100 % with Mission Control full, Operations with the queued offer, the SET Centre
with the Retention card and both roster marks, the person page with Retention on the default and
on "Keep at any price" after a match, the page of an engineer who joined from NASA, the turn
report's People section, and a sheet of the new pieces. Each artboard's note lists its choices,
all **unconfirmed**; the main ones:
- **File cards:** one footer each: the button, a reason (a lock for astronauts, a clock for what
  passes with time), or the gold "Offer queued: +50 %" badge with the price if accepted and
  "Cancel the offer". With the season's offer used, the other cards show the button greyed with
  "Your offer this season goes to …". Cards add "With NASA since …". "Offer: 1 of 1 used" sits in
  the title row next to the actions, and the footer chip reads "L3 · 2 / 3 · offer 1 / 1".
- **Offer dialog:** the file on the left (with its date stamp and today's salary, since the offer
  is priced on it), the offer on the right: raise segments with the new salary under each; cost
  (new salary, signing bonus, room, "Nothing is paid unless they come"); the answer as a range on a
  0–90 % scale, worked out from the band's bounds plus what the file shows (+25 % at Middling with
  Restless gives 19–28 %; Low, disappointed and Steady at +100 % gives 71–90 %); "Make the offer ·
  +25 %". With the roster full the room line turns red, with the lapse warning, and the button
  stays enabled.
- **Operations:** the offer is listed with the queued actions, marked apart (a dollar glyph, a
  light gold band, the badge) and cancellable.
- **Retention card:** a full-width strip between the department summary and the filters; four
  segments; "2 engineers set otherwise", naming them, with a new "Set otherwise" filter.
- **Roster marks:** after the name: Keep at any price a navy shield with a tick, Let go a muted
  door with an arrow out, each with a tooltip; none for the default.
- **Person page:** a Retention row under loyalty in the identity card, three segments with second
  lines ("matches up to +25 %", "matches any offer", "never matches") and a caption for this
  person; "Joined from NASA" as a green tag under the age line; career dots green for joined, navy
  with a gold ring for stayed or turned down; lines from the old agency prefixed "At NASA:".
- **Turn report:** small subheads in People ("Offers to your people", "Your offer"), tags Matched
  (navy), Left (red), Joined (green), a "Signing bonus · <name>" budget line.
- **Reasons beyond the spec's three:** "Approached recently: wait until Summer 1963" (with the
  date) and "On a mission in flight this season".
- **New:** no colour tokens; icons `retain_keep`, `retain_go` and `joined` (inline SVG so far).

*As built (2026-10-08):* `IntelligenceOfficeScreen` (the file card's footer, the offer dialog,
the queued offer, "Offer: n of 1 used" in the title row), `RetentionCards` (the department strip,
the person page's row, the roster mark), `PersonScreen` (the joined tag, the row, a left column
widened to 440 px to fit it), `CareerCards` ("At NASA:" before earlier lines, the new dots),
`TurnReportView` (offer lines first in People, and the People tile counting who came and who was
hired away), `client.view.PoachTexts` for every line, three icons in `make_ui_icons.py`, and
refusal texts. Differences from the mockup: the report's People card has no subheads (the tags
say which side); no "No offer made" line in last season's
results; no "Kept from NASA" on a newcomer's ambition; no "Signing bonus" budget line (the report
line names the bonus); the dialog's answer caption also says morale and unshown traits aren't
counted. The three icons are on the UI Asset Sheet (2026-10-08, chrome section, with the
roster marks and the joined tag in context), next to `binoculars`, `rel_good`, `rel_clash` and
`trait_team`.
*Gaps closed (2026-10-08):* a poaching line in the report opens the page of the person it's about
while they're with the agency (one of ours who stayed, or someone who came over; never someone who
left, and never the rival's person, whose id means nothing here; `PoachTexts.pageOf`). The SET Centre
and Mission Control get the mockup's **"Set otherwise"** filter chip, shown once someone is set
otherwise, and the Retention strip's **"Show them"** link switches to it.

### 4.17 Contracts *(specified 2026-10-11 for `design.md` §2.11; mocked up 2026-10-11)*
**Purpose:** the commercial marketplace: see what clients offer, apply for one contract a turn,
follow the active contract. Contracts pay money, not prestige. Rules and every number in `design.md`
§2.11 (all unconfirmed); the screen reads them from the rules' constants (G7). One name for both
archetypes; the Soviet clients are ministries and institutes, which only changes the text.

**Where:** a fourth card on Headquarters' landing screen (§4.6), **Contracts**: "4 on offer · 2 you
can fly · Applied: Transatlantic relay", or "Active: Transatlantic relay · launch by Autumn 1963". The
breadcrumb is "Space Complex › Headquarters › Contracts". Headquarters' hub marker gets the gold
dot (as component 26's) when a listing the agency qualifies for is new this turn and it hasn't
applied anywhere, hover "New contracts on offer". A help topic "Contracts" (component 23).

**Layout, top to bottom:**
- **Your contract** card. With an active contract: client and line, the tier won ("Tier 1 of 3"),
  the fee, the launch cost now, the window ("Launch by Autumn 1963 · 2 seasons left", red in the
  last), and its state (awarded, scheduled, flying, with the flight's season). Button **Plan the
  flight**, to the contract configuration's mission plan on its assessment tab (§4.6.7), badged
  (component 27). Without one: "No active contract. Apply for one below; it's awarded when the turn
  resolves."
- **Your record**: one row per unlocked line: successes, failures and lapses, the record's points
  (+3 / −6, within ±15) and **your score now** (weakest component's reliability plus the record),
  with a tooltip breaking it down ("Weakest: Jupiter-C, 72 % · Record +6 · Score 78"). Locked lines
  aren't listed; a line with no record yet reads "No contracts flown yet".
- **On offer** (the board): one card per listing, the ones the agency can fly first, then the rest
  dimmed. A toggle **"Only what I can fly"** (off by default, remembered per viewer). Each card:
  - the line's category art, the client's name, the story (two or three lines);
  - the terms: three tier chips "+60 % · +40 % · +25 %" with **your fee at each** ("2,340 · 2,048 ·
    1,829") for the contract configuration with the cheapest rocket you could use, and the launch
    cost under them;
  - the flight it takes ("Flies: Explorer 1 · Orbital flight"), with the component 27 icon;
  - "Posted Spring 1962 · expires in 2 turns";
  - **Apply**, or the reason you can't, in place of the button: "Needs the Earth-orbit satellites
    line: fly Explorer 1's marked flights", "Open Explorer 1 to fly it", "You have an active
    contract". Never how many have applied: applications are sealed.
- **Applied:** the card shows "Your application · decided when the turn resolves" and **Withdraw**.
  Applying elsewhere moves it, after a confirmation ("Move your application from Transatlantic relay
  to Weather picture service?").
- **Two programs in a line:** Apply first asks which, each with its contract flight and its launch
  cost now.

**The apply confirmation** (component 5) states the terms once: the fee by tier, "Tiers go to the
best record; a fourth applicant gets nothing", "If the flight fails, the insurer refunds the launch
cost and you lose the prestige that failure costs", "Launch within 3 seasons or it lapses: −10
prestige, and it counts as a failure".

**Turn report** (§4.4): a **Contracts** card and strip cell: awarded (client, tier, fee, window,
link to the board), missed out ("Agencies with a better record won it", never how many applied), a contract flight's payout
or the insurer's refund, a lapse. The news gets the public line for every award: "Transatlantic
relay goes to Pan-Am Space and Orbital Corp."

**Empty board:** "No contracts on offer. Clients come once an agency has flown every marked flight
of a program: look for the contract icon on its flights." With a line unlocked but no listing
(rare): "No contracts in your lines this turn. New ones are posted when a turn resolves."

**For the mockup:** active contract in flight; one listing applied for; a dimmed listing with each
reason; the move confirmation; the turn report's card in its four states; the Headquarters card in
its three states; component 27 on a configuration card (both states) and on a program card.

**Mocked up (2026-10-11)** on the canvas `Launch Window — Contracts` (the Artifact tool's list finds
it), a NASA agency in Summer 1963 with the contract awarded and not yet flown, and in Winter 1962 with
an application out: the screen in both, the contract card's other states, dimmed listings (one per
reason) beside a Soviet listing, the three dialogs, both empty states, the turn report with its card
and strip cell in six states, Headquarters with the fourth card in three states and the hub dot, and
component 27 (below). Numbers are the brief's placeholders. Choices, all **unconfirmed**:
- **Your contract:** the category art, "Tier 1 of 3" as a gold badge, the client and line, the fee,
  the launch cost now and the window as three figures, then a stepper (awarded, scheduled, flying,
  paid). The button follows the state: Plan the flight, Open the mission plan, Follow it in Mission
  Control. Only the window's last season turns red. With none, three short rules and "Help: Contracts".
- **Your record:** a table (line, flown, failed, lapsed, record, score now); the score has a dotted
  underline and a navy tooltip (weakest component, record, score, "Crew and controllers don't
  count"). A line whose program is closed shows "Open Mercury" as its score.
- **The board:** four cards a row, "On offer · 4 · 3 you can fly" over it with "Only what I can fly" on
  the right (a new switch). Cards: the line as a navy chip on the art, the client as an eyebrow, the
  title, the story, three tier boxes, "Your fee at each tier · launch cost now", the Flies and Posted
  lines, and a foot with "Applications are sealed" and Apply. **Dimmed** (greyscale art, muted text)
  means you can't fly it; a listing blocked only by the active contract keeps its look and swaps the
  button for the reason. Reason icons: lock, open_programs, document. **Applied:** a 1.5 px navy
  border, a 3 px gold top accent, a gold "Applied" badge, "Your application · decided when the turn
  resolves" and a red Withdraw.
- **Dialogs:** the apply confirmation adds the score now and "Pays money only: no prestige, not a
  repeat"; the move confirmation's buttons are "Keep Transatlantic relay" and "Move it"; the
  two-programs picker shows each flight's rocket, launch cost, fees and score, then "Next: the terms".
- **Report:** a seventh strip cell, "Contracts", and a card with a 3 px left accent per outcome (gold
  awarded, green paid, navy refunded, red lapsed, muted missed out); a sixth state for an application
  whose program closed. Paid shows the profit and the record's change. The news line lists winners
  alphabetically ("Orbital Corp. and Pan-Am Space"), plus a line for new listings. The budget gains a
  Contracts line. With a decision waiting too, the strip needs a rule (an eighth cell, or one shared).
- **Headquarters:** the card's eyebrow counts the board; a state row above its button (gold-tinted
  with a dot for new offers, plain for applied and active). The hub dot is component 26's, one dot for
  both reasons with both lines on hover.
- **Worth a look in `design.md` §2.11:** the fee follows the launch cost, so of two programs in a line
  the dearer one pays more profit at the same margin.

**Built (2026-10-11)** in `screen.ContractsScreen` with `view.ContractsView`, Headquarters' fourth card
(`HeadquartersScreen`, "Open the board"), the hub's gold dot on Headquarters with "New contracts on
offer" on hover (`SpaceComplexScreen`), the report's Contracts card, strip cell and news lines
(`TurnReportScreen`), and the help topic "Contracts" (`help.json`, its numbers as `contracts.*`
tokens). As built, against the mockup:
- The board shows four cards a row in a scroll pane; the art is the line's category art, whole at the
  card's width (2026-10-11, the user's report: the mockup's cropped banner cut the spacecraft off), as is
  the active contract's, 220 px wide. A listing the agency can't fly is dimmed whatever else holds; one it can fly but not
  apply for (an active contract) keeps its look with the reason (`Contracts.check` asks about
  flying before the active contract).
- "Only what I can fly" is remembered while the client runs; its track is square (no rounded track
  in the kit). The contract card's button follows the state: Plan the flight (the plan's assessment
  tab), Open the mission plan (Mission Control's scheduled missions) and Follow it in flight (the
  active missions).
- The score's tooltip is the kit's tooltip with the breakdown as lines; the record table has no
  dotted underline but a thin rule under the score.
- The report's strip gets an eighth cell beside a waiting decision's seventh; the budget card has no
  Contracts line yet (the fee is in the Contracts card and the funds). The sixth state, an
  application whose program closed, reads "Came to nothing".
- No new tokens and no new `Theme` components: the badge, listing card, switch and stepper are built
  in `ContractsScreen` and `ContractMarker` from the existing tokens.
- Shots `contracts-*.png` (`ScreenSnapshot -Dshots=contracts` renders just these).

### 4.18 Front page *(the user's idea, 2026-10-09; specified and mocked up 2026-10-11)*
**Purpose:** when an agency makes one of the great Firsts, every player in the game sees it the way
the world did in the 1960s: on the front page of a newspaper. It's the first thing they see on their
next login, before the turn report. *Changed 2026-10-12 (user):* when the player watches the
missions first (§4.4), the pages come after the replays, so a First isn't printed before its film. It's news, not a rules screen: the game facts go in one line
under the page.

**Mocked up (2026-10-11) in Photocraft, not Claude Design:** `assets-raw/frontpage/mockup/`
(`frontpage_mockup.pcraft` with every layer named, `frontpage_mockup.png` at 1920 x 1080), from the
generated art (component art below). It stands in for the Claude Design step for this screen. Its
fonts are stand-ins (Times New Roman, Georgia, Segoe UI); the build uses the faces under **Type**.

**The Firsts** are listed in content, not code (`front-pages.json`, below), in race order. The standard
pack's five: `earth-orbit-satellite`, `human-in-space`, `human-in-orbit`, `crewed-lunar-flyby`,
`crewed-lunar-landing`. A pack shows the ones whose goals it has (early-era: the first three).
*Unconfirmed:* spacewalk, docking and a two-crew flight aren't Firsts with a front page; they're in the
turn report as usual.

**When a page is printed:** in the resolution where a First's goal is reached for the first time in the
game (the cross-player goal race, `design.md` §2.6: nobody held it at the start of the turn), reached or
credited through the cascade alike.
- **Ties:** several agencies reaching the same First in one resolution all get the first-time prestige,
  so they share one page: the headline uses the story's tie form (both names), the race box lists both.
- **Several Firsts in one resolution:** one page per First. When one flight earns two Firsts (a landing
  credits the flyby through the cascade; a first orbit credits human in space and the satellite), the
  latest First's page is printed and the others are folded into it: their lines in the race box carry
  the same season, and the article's `also` paragraph mentions it. *(Unconfirmed: one page is the better
  morning; two pages for one flight would read as a repeat.)*

**When it shows:**
- **On login,** after Join game and before the turn report: every page this player hasn't seen, oldest
  first, one at a time ("1 of 2").
- **While logged in,** when the server's `TurnResolvedNotice` arrives: the new pages show before the
  "The turn resolved" flow, the same way.
- **Seen** is remembered per device (preferences per server and login, like the report's read mark),
  per page id. A player who joins a game after a page was printed never gets it on login (it's in
  the Museum): pages printed before their join turn count as seen.
- **Reopen:** a First tag on the Museum's race table opens that page, alone, with "Close" in the strip;
  so does the turn report's "first in the game!" goal line when the First was this player's.

**Layout** (1920 x 1080). *Decided while specifying (unconfirmed):* a full portrait page would put the
body text near 10 px; the page is the top half of a broadsheet, "above the fold", landscape:
- **Backdrop:** the hub, dimmed by a navy scrim (mock: `#0d1b33` at 80 %), so the player knows where
  they are. On login the page comes before the hub screen: draw the hub's art for the current season
  and time of day under it, or, if that isn't loaded yet, the page frame's blueprint texture. Checked
  on the brightest (Soviet winter noon) and darkest (winter night) variants as §1 asks (shots
  `frontpage-winter-noon.png`, `frontpage-winter-night.png`, 2026-10-11).
- **The page:** 1440 x 900 at (240, 40), the paper art (`paper.jpg`) with a soft drop shadow (mock:
  black, 60 %, 8 px down, 28 px blur). Content inside a 40 px margin (x 280 to 1640). All type on the
  page is `Theme.INK`.
- **Top band:** two ear boxes (230 x 84, 1 px rule) left and right of the masthead. Left: "LATE CITY
  EDITION" and a weather line ("Fair and cool tonight"; a short list per season, picked with the
  page's seed). Right: "EXTRA", letter-spaced, and "Special edition" in italics. The masthead
  (`masthead.png`, drawn at 761 x 108) centered between them.
- **Folio line:** a 4 px rule and a 1 px rule under it, then "VOL. <n> · No. <turn>" left (the volume
  is the game year counted from the start year, the number is the game turn; the mock's "VOL. IV" is a
  placeholder, Spring 1961 from a Winter 1955 start is VOL. VII), the season letter-spaced
  in the middle ("SPRING 1961"), "FIVE CENTS" right, a 1 px rule under it.
- **Headline:** the story's headline in capitals, bold, 80 px, centered, one line. If it's wider than
  1360 px, two lines; if two lines still don't fit, the story's short form (`headlineShort`). The mock's
  one line relies on Times Bold squeezed to 88 %; Old Standard TT unsqueezed is wider, so expect two
  lines to be the usual case, pushing the deck and the lower half down about 90 px (the lower half
  then loses that height: the photo shrinks with it, keeping its aspect). *Unconfirmed:* a condensed
  OFL face instead would keep one line.
- **Deck:** the subheadline, italic 25 px, centered, then a 1 px rule across.
- **Lower half, three columns:** the photo (640 x 440, `ui/frontpage/<first-id>.jpg`) at the left with
  its caption under it (italic 14 px, two lines at most); then two text columns of 336 px, 24 px apart,
  each with a 1 px column rule on its left. Column one: the byline ("FROM OUR SPACE CORRESPONDENT",
  12 px, letter-spaced), the article's opening paragraphs, and an "IN BRIEF" box at its foot (two
  teaser lines in italics). Column two: the rest of the article and, at its foot, the race box.
- **Body type:** 16 px on 22 px, justified, a 14 px first-line indent, 4 px between paragraphs. The
  article flows from column one to column two (paragraphs move whole; a paragraph that doesn't fit in
  column one starts column two). A story's paragraphs must fit both columns at the longest agency,
  director and crew names allowed; `FrontPagesTest` checks the length (below).
- **The race box** ("THE RACE SO FAR"): a 1 px box, one row per First in the pack: the First's name
  (from `strings.json`) left, who made it and when right in italics ("Kestrel Space Directorate ·
  Fall 1957"; ties "A and B · Spring 1961"), "still open" for the rest. It shows the race as of this
  page's edition, never later Firsts, also when reopened.
- **The strip** under the page (y 960 to 1040, on the dimmed hub, `Theme.ON_PAGE` text): the game line
  left, then "1 of 2" (muted) and the primary button right: "Next front page", or on the last page
  "Continue" (to the turn report if one is unread, else the hub). Reopened: "Close".
- **The game line:**
  - the First was this player's: "Your agency got there first: <goal> paid <n> prestige.";
  - a tie with this player: "You share this First with <agency>: both got <n> prestige.";
  - a rival's: "<agency> got there first. <goal> now pays <n> prestige to whoever follows." (the
    second-time value).

**Words** (`assets/content/front-pages.json`, outside the frozen pack like `event-stories.json`, so
corrections reach running games; our own text, written from the real Firsts' facts, never from a real
paper's page):
- One entry per First, keyed by goal id: its photo file, its caption, the two "In brief" lines, and at
  least **two stories**. A story has `headline`, `headlineShort`, `headlineTie`, `deck`, `deckTie`,
  `paragraphs` (in order), `rivals` (a paragraph needing one or two rivals, dropped in a game with
  fewer), and `also` (the folded-First paragraph, used only when one is folded in).
- **Tokens,** only what the server prints (G14): `{agency}`, `{director}`, `{season}`, `{crew}` (the
  flight's crew by name, "Daniel Hale" or "Daniel Hale, Rosa Ortiz and Pavel Brandt"), `{commander}`
  (the first of them), `{rival1}`, `{rival2}`, `{also}` (the folded Firsts' names: a first orbit can fold in human in space and even the
  satellite). No hardware: the
  page never names the rocket, the capsule or the program, which the Intelligence Office sells
  (`design.md` §2.9). Fixed facts about the First itself (orbital speed, the distance to the Moon) are
  plain text, checked like a history note.
- **The pick:** a page's story is drawn with the game's seed salted by the goal id, so every player
  reads the same page, and it never touches the season's random stream.
- Ids (story ids, the First keys) are stored in printed pages: never rename or reuse one.

**Type** (to bundle, SIL Open Font License, credited in `assets/CREDITS.md`, generated in
`Theme.Fonts` like the others): **Old Standard TT** Bold for the headline, ears and box titles (a
19th-century newspaper face), **Libre Caslon Text** Regular and Italic for the deck, body, caption and
race box. *Unconfirmed.* The masthead is art, so it needs no font.

**Component art** (`assets/ui/frontpage/`, from `assets-raw/frontpage/`, CLAUDE.md "Tools outside the
repo"): `paper.jpg` (1440 x 900), `masthead.png` (ink on transparent, 1269 x 180, drawn at 60 %), one
press photo per First (1024 x 704, printed in ink on the paper's colour, drawn at 640 x 440). All
generated with Qwen Image (ComfyUI), fixed seeds; the picks are in `picks.json`.

**States:** one page; several ("1 of 3"); a tie; a solo game (no rival paragraph, no rivals in the race
box beyond this player); the early-era pack (three rows in the race box); reopened from the Museum;
the fetch failed (no page, straight to the report: a page is never worth blocking the login).

**Server work:** G14. **New for `Theme.java`:** the two fonts, the scrim (if no existing token
matches), no new colours (the ink is `Theme.INK`).

**Built in libGDX (2026-10-11)** as `client.screen.FrontPageScreen`, its words in `client.view.FrontPageView`,
from `assets/content/front-pages.json` (`content.FrontPages`). Shots `frontpage-*.png`
(`ScreenSnapshot -Dshots=frontpage`). Choices made while building it, **unconfirmed**:
- **The words file's shape:** per First a caption, two brief lines, two stories and a separate `tie`
  story (not `headlineTie`/`deckTie` fields); a story is `headline`, `headlineShort`, `deck`, `lead`,
  `also`, `rivals2`, `rivals1` and `close`, read in that order. `{also}` is the folded Firsts' names in
  quotes. Tie stories are their own text, since "A and B" breaks the verbs of a one-agency story; in a
  tie `{crew}`, `{director}` and `{commander}` list everyone's.
- **The headline:** one line at 80 px, else one line at 64, else two lines at 64, then the short
  headline the same way. Old Standard is wide, so a headline with an agency's name in it usually takes
  two lines at 64; the lower half gives up that height, and the photo is cropped about its middle to
  keep its width.
- **Body text is ragged-right,** not justified (a libGDX label can't justify), with a four-space
  first-line indent (the fonts have no em space).
- **The columns:** the first fills before the second, paragraphs whole; while the second overflows,
  closing paragraphs are dropped from the end (`FrontPagesTest` holds every article to 26 lines at the
  longest names allowed, measured with the page's font). The "In brief" box sits at the foot of the
  first column, the race box at the foot of the second; a race row whose who-and-when doesn't fit beside
  the First's name goes on a second line, right-aligned.
- **The strip's button** is the white page button, not the mock's gold one: the design system has no
  gold button.
- **Joining late:** pages printed before the turn the player joined in count as seen, from their own
  season count (`ClientSession.joinedTurn`).
- **When it shows:** on login the hub fetches the pages; unseen ones replace it, then a fresh hub opens
  the unread report as before. If the server hasn't answered after 4 seconds the report opens without
  them (a late answer is dropped; the pages wait for the next login), and a player who leaves the hub
  before the answer gets neither until then. While playing, a resolved turn's refresh shows the new
  pages over the current screen and comes back to it, then toasts as before; dev mode's "Resolve turn
  now" shows them before the report. The page screen never refreshes itself. A failed fetch shows
  nothing.
- **Reopening:** the race tab's First tags (declared goals only), the Achievements tab's "First in the
  game" tag and the turn report's "First in the game!" chip open the page, with a tooltip saying so.
- **New in `Theme`:** `FontFile` Old Standard Bold and Libre Caslon Regular and Italic (static
  instances at weight 400, cut from the variable fonts), the `NEWS_*` text styles (the headline styles
  generated with capitals only, `Theme.CAPITALS`), and `SCRIM_DEEP` (`#0d1b33` at 80 %).

#### 4.18.1 The world reacts *(the user's idea, 2026-10-20; specified, mocked up and built 2026-10-22, G17)*
**Purpose:** a First is news for the winner and a moment for everyone else. For one turn after a First's
page is printed, every rival may give the press a statement about it (component 28); at the next
resolution a follow-up page, "The World Reacts", prints them all as one article. Flavour only: no money,
prestige or anything else changes.
- **Who may react:** every agency that had a turn report for the First's turn (late joiners are out,
  unconfirmed) and didn't make the First (in a tie, none of the achievers). One statement per First's
  page; several Firsts printed together each take their own. The last statement sent before the turn
  resolves counts; it can be withdrawn.
- **A statement:** one of five stances (enthusiastic, polite, "we'll be next", downplay, no comment) and an
  optional quote of up to 140 characters, printed word for word inside curly quotes. No LLM (user,
  2026-10-20); one could rewrite later, as a layer on top.
- **The article reads as a story, not a list** (user, 2026-10-22, options 2, 3 and 4 of the talk-through):
  - *Mood:* the statements' mix picks the story, written whole per First: GRACIOUS (only
    congratulations), CHALLENGE ("we'll be next", nobody belittling), MIXED (congratulations and
    belittling), DISMISSIVE (belittling, no congratulations), SILENT (only pointed no-comments). A story
    has a headline, a deck and a lead, with a form of the deck and lead for a single statement (two-player
    games).
  - *Statements* are told in stance order (enthusiastic, polite, next, downplay, no comment), quoted
    before unquoted, each by a stance line; a *linking sentence* leads in when the stance changes ("Not
    everyone was impressed.").
  - *Details* from public facts only (the goals an agency holds, as the race box and the archive show;
    never what the Intelligence Office sells, unconfirmed): whether the rival matched the First in the
    meantime (always told), or holds the First before this one or not (told about half the time).
  - *A crowded page:* at most 5 statements are told one by one, quotes first; the rest of each stance
    share one "also" sentence. Rivals who gave none share one closing line ("did not respond").
  - *Close:* per First, pointing at the next contest; the Moon landing's at the race's end. Closing
    paragraphs are dropped from the end when the page is full.
- **No statement at all** (every rival silent): no follow-up page (unconfirmed).
- **The page:** the First page's frame (§4.18), the right ear "THE WORLD / REACTS" in two lines
  (`NEWS_EAR_TWO`), the First's photo and caption, the race box as of the follow-up's turn, no "In brief".
  The strip says "Statements only: no effect on the race." It shows like any page (on login, after a
  resolution) and opens again from the First's page ("Read the reactions", reopened only).
- **Lower half** (mocked up on the canvas `Launch Window — The World Reacts`): four text columns of 322 px
  with 1 px rules between, the photo (322 x 221) and its caption over column one, then the byline; the race
  box at the foot of column four. The article runs from column to column and a paragraph is split between
  words where a column ends (at least two lines stay behind; the rest goes on unindented), ragged-right as
  on a First's page (the mockup's justified, hyphenated text needs a line-level layout).
- **Short articles** (the user's idea, 2026-10-22): the room a short article leaves is filled with short local
  items, as a paper fills its columns: `fillers` in `front-page-reactions.json` (our own fiction, 24 of them since
  2026-10-23, at least 20 (`FrontPageReactionsTest`), at most 9 lines each, true before anyone has flown), a headline
  in `NEWS_LABEL` capitals over Caslon body, a short centred rule (64 px) between items. They go into the column the
  article ends in and every one after it, each at most once a page. None on a page that runs on.
  **No repeats** (user, 2026-10-23, `client.view.FillerDeck`): the client keeps, per server and login, the items shown
  since the last had been and offers those not yet shown first, in an order drawn from the page's id; once all have
  been, the deck starts over. A page keeps the items it first showed when it opens again. Since every player sees the
  same pages in the same order, they read the same items, except a late joiner or a fresh install.
- **History notes** (component 15, the user's idea, 2026-10-22): on the follow-up's strip after "Statements
  only: no effect on the race." (the icon in `ON_PAGE`), how rivals honoured each other (Apollo 11's medals for
  Gagarin and Komarov); and on the statement dialog's title (below). Their words are `historyNotes` in
  `front-page-reactions.json`, checked in `design.md` §1.1.
- **When it doesn't fit:** the closing paragraphs go first; what still doesn't fit runs on to a continuation
  sheet: "Continued on page 2" at the foot of column four, "Turn to page 2" on the strip's button, then a
  sheet with a running head ("THE WORLD REACTS · CONTINUED FROM PAGE 1 · <season>") and four full columns
  (user, 2026-10-22: no scroll area). The fullest article the printer makes (4 told at 140 characters, the
  longest names) still fits one page in the shots; `FrontPageReactionsTest` holds the words to 64 lines at
  316 px, unsplit.
- **Words:** `assets/content/front-page-reactions.json` (outside the pack, not hashed;
  `FrontPageReactionsTest`): per First its stories per mood, its detail clauses and closes; per stance its
  quoted, plain and "also" lines; the linking sentences; the declined lines. Every piece has an id that
  printed pages store: never rename or reuse one. Tokens: `{agency}` and `{director}` (the First's
  makers), `{season}`, `{rival}`, `{rivalDirector}`, `{detail}`, `{quote}` (only at a line's end), `{rivals}`.

---

#### 4.18.2 Theories of the day *(the user's idea, 2026-10-22; built straight in libGDX the same day, no mockup, user; G18)*
**Purpose:** the space age argued over questions nobody could answer from Earth: is the Moon a sea of dust, is
Venus a swamp, does weightlessness stop a pilot swallowing. A flight settled each one. Here a player's scientists
raise such a question at a checkpoint, and the first flight in the game that settles it answers it for everyone.
Flavour only: nothing in the rules reads them.
- **The theories** (`assets/content/theories.json`, outside the pack, `TheoriesTest`), eight: the deep lunar dust,
  eating and working weightless, the radiation belts, micrometeoroids, a warm wet Venus, canals on Mars, the
  Moon's lumpy pull (mascons) and Moon germs. Each has a reach, `PUBLIC` (the public followed it: a science page
  in the paper) or `TECHNICAL` (argued over among engineers: a line in the report's news), the goals that raise
  it (`raisedBy`, e.g. `lunar-probe`, which opens the crewed lunar programs, for the dust) and the goals that
  settle it (`answeredBy`, e.g. `lunar-soft-landing`), a briefing, what would settle it, the answer in a
  sentence, a public theory's headline, deck and article or a technical one's notice, and a history note.
  A theory whose goals the game's pack lacks never shows (early-era: the weightless body and the belts).
- **Raised** (private, in the turn report's News card): a card per theory whose first `raisedBy` goal the
  player reached that turn ("A NEW THEORY · <season>", the question with its history note, "What would settle
  it: ...", or "Settled already, by <agency> in <season>. <answer>" when a flight in the game settled it
  before). Derived on the client from the player's goal turns and the printed pages; nothing saved.
- **Settled** (shared): the server prints a page of kind `SCIENCE` (`rules.SciencePrinter`, in
  `printFrontPages`, after the Firsts) the first time anyone reaches one of a theory's `answeredBy` goals; once
  per theory; agencies settling it in the same turn share it. A public one shows like any unseen page, in the
  paper; every report of that turn lists the theories settled in its News card ("Settled: <question>").
- **The science page:** the follow-up's frame (§4.18.1) with the right ear "SCIENCE / REPORT", no photo; column
  one opens with a ruled box where a First's photo would be, "THE QUESTION" (2026-10-22): the question, then how it
  stood for the reader's agency, in the race box's rows: "Raised by your staff" (the season, or "not by then"),
  "Settled" and "Open for" ("1 year", "2 years, 1 season"; `FrontPageView.standing`), so every game's page
  differs. Then the byline "FROM OUR SCIENCE CORRESPONDENT", the article and the filler items, the race box; the
  strip "Science news: no effect on the race." with the theory's history note. An article runs 5 to 8 paragraphs,
  32 to 48 lines at the longest names (`TheoriesTest`), so it fills about two and a half columns and the filler
  items only the rest; one that still runs over goes on to the continuation sheet, headed "SCIENCE REPORT".
- **In the Museum** (2026-10-22): the Archive tab's "THEORIES OF THE DAY" card, under the findings card, lists the
  theories this agency's scientists raised and those a flight settled (never one neither, so it spoils no
  checkpoint; `FrontPageView.museum`). Each row: the question with its history note, "What would settle it" or the
  answer and who settled it, when your scientists raised it; on the right the status ("OPEN QUESTION" or
  "SETTLED · <season>") over the links: "Read the page" for a public theory (reopens the science page, "Close"),
  and a button per archive page about the flights that settled it, once settled and open in this game
  (`theories.json`'s `archive` ids, checked by `TheoriesTest`; the Moon germs have none: no archive page covers
  the quarantine). The report's "Settled:" line for a public theory opens its page too.
- Shots `-Dshots=theories`.

#### 4.18.3 In memory *(the user's idea, 2026-10-22; specified, mocked up and built 2026-10-22; G19)*
**Purpose:** a lost crew is news for the whole world, and the agency that lost it owes the world a statement.
The paper prints a memorial page for every crew lost in flight, a turn after the loss, with the agency's
statement in the tone its director chose and the other agencies' condolences. It gives the moment the gravity
it deserves, and confronts the player with a choice about how to speak of the dead (user). Flavour only: no
money, prestige or anything else changes (the rules' cost of a loss is the grounding, `design.md` §2.5(j), and
the withheld goals, §2.6).
- **What triggers it:** a catastrophic step that loses a crewed flight's crew, as for the grounding
  (`MissionProgress.loseCrew`). Not an astronaut who perishes in a random event, nor an animal passenger
  (unconfirmed). One page per lost crew; two crews lost in one turn print two pages.
- **When, a turn late** (user, 2026-10-22): nothing is printed the turn of the loss, so no page spoils the
  replay at login. The losing player learns it from the replay and the report, as now.
  - *Turn T resolves:* the crew is lost.
  - *Turn T+1, open:* the losing agency gives its statement (component 29); every other agency may send
    condolences (component 28's condolence form). The losing agency's report and hub show "The press is
    waiting for your statement"; every other agency's report news says "<Agency> has lost the crew of
    <mission>" (their names: public, as on the page) with "Send condolences".
  - *Turn T+1 resolves:* the page is printed (`FrontPage.Kind.MEMORIAL`) and shows to everyone at the next
    login, before any other page that turn.
- **The agency's statement** (component 29): one of four tones (the user's list, 2026-10-22: "do you want to
  focus on the bravery, or the personal, or..."), and an optional quote of up to 140 characters, printed word for
  word. The last one sent before the turn resolves counts; it can be changed.
  1. *Their courage:* they knew the risks and flew anyway.
  2. *Who they were:* the people behind the names, beyond the flight.
  3. *We will go on:* we fly again, and finish what they started.
  4. *We will find out why:* we will learn what went wrong, and fix it.
  No statement by then: the paper prints a short one "from a spokesman", plain and dignified, never a reproach
  (unconfirmed).
- **Condolences** (rivals; component 28 with its own rows): *Condolences* (sympathy to the families and the
  agency), *A tribute* (honour the crew and what they did), each with an optional quote; or nothing. Unlike a
  First's statements, silence is never printed, and no stance can belittle (no downplay, no "we'll be next").
- **The page:** the paper's frame (§4.18) in mourning: a black rule around the sheet, the right ear "IN
  MEMORY", no race box, no filler items, no photo of a launch. Under the headline and deck, the crew's
  portraits in a row (the memorial's 128 px portraits, in greyscale), each with name, age and "N flights".
  Then the article, in the statement's tone (2 or 3 stories a tone, picked by the page's id):
  - *The lead:* what happened, in plain words: the flight and the step it was lost on ("lost during reentry").
  - *The statement,* told in its tone, with the quote.
  - *The people:* a sentence each from what the game knows (age, years with the agency, flights flown, firsts
    flown, a revealed trait; never anything hidden); the tone picks which.
  - *What they nearly did:* when the flight had reached a goal the loss withheld (`GoalsWithheld`): "They had
    become the first to walk in space, and did not come home." The goal stays unclaimed (§2.6).
  - *The stand-down:* "<Agency> has stood down its crewed flights while the accident is investigated."
  - *From the other agencies:* the condolences, quotes first, as one paragraph or two; none when nobody sent
    any (the article simply ends).
  - *A close* per tone.
- **The strip:** "In memory." with a history note (the Fallen Astronaut, below), and Continue. The losing
  agency's copy adds "Open the memorial" (the Museum's Memorial tab). The page opens again from each lost
  person's memorial page ("Read the paper").
- **When the grounding ends** (`CrewedGrounding`): every agency's report news says "<Agency> resumes crewed
  flights" (a line, no page; unconfirmed).
- **History notes** (component 15; checked in `design.md` §1.1): on the page's strip, the Fallen Astronaut
  left on the Moon by Apollo 15 with the names of eight Americans and six Soviets; on the statement dialog,
  Gene Kranz's "tough and competent" talk to his controllers after the Apollo 1 fire; on the condolence form,
  Tom Stafford at the Soyuz 11 funeral, representing the President, a pallbearer.
- **Words:** `assets/content/memorial-pages.json` (outside the pack, a test like `FrontPageReactionsTest`):
  per tone its stories (headline, deck, lead, the statement's lines with and without a quote, the people's
  sentences, close), the withheld-goal sentences per goal kind, the condolence lines, the spokesman's
  statement. Ids are stored on printed pages: never rename or reuse one. Tokens as the reactions' (`{agency}`,
  `{director}`, `{season}`, `{quote}`), plus `{mission}`, `{step}`, `{crew}`, `{name}`, `{age}`, `{flights}`.
- **Mocked up (2026-10-22, unconfirmed)** on the canvas `Launch Window — In Memory`: three crew with a quote and two
  condolences, two crew with a short article, and the losing agency's copy. The mourning frame is a 3 px ink rule
  12 px inside the sheet, with the ear "IN MEMORY" on one line at 26 px. The crew row sits between 1 px rules: the
  128 px portraits in greyscale with a hairline, the name (Caslon 17) and "Aged 38 · 2 flights" (Caslon italic 14).
  The article runs in four columns balanced to end together, or in two centred columns (692 px) when it is short
  (under about 16 lines), then a centred 64 px rule; the paper below stays empty. "From the other agencies" is a
  small capital head between rules (Old Standard Bold 12). The strip's history note follows "In memory."; the
  losing agency's "Open the memorial" is a ghost button left of Continue.
- **Built (2026-10-22)** as mocked up, with these choices (unconfirmed):
  - *The portraits* are greyscale copies made at runtime from the colour portraits (`UiKit.greyPicture`), 128 px
    on the page, 64 in the dialog, 40 on the cards.
  - *Short article:* under 24 lines (the mockup's 16 left a lone line atop the last of four columns), two centred
    columns and a 64 px rule. *Long:* four columns of an even height found by halving, so they end together.
  - *Running on:* a page with the director's words and four rivals' at 140 characters runs on to a continuation
    sheet in the same mourning frame, "Continued on page 2" at the foot of column four (`-Dshots=memorial`'s
    crowded page). Each rival's line is its own paragraph.
  - *The person sentences* (`MemorialView.person`): for "Who they were", a revealed trait's line, else the years
    served; for the other tones, the flights ("was flying for the first time", "was on a second flight").
  - *Places* (the headline's "on the way home", the lead's "during reentry") come from the step's id
    (`memorial-pages.json`'s `places`, in order; "in flight" when none matches).
  - *The firsts* worth a sentence are a ranked list (`firsts`): a withheld goal not on it (a program's own "flown"
    goals) gets none.
  - *The hub's card* sits top left under the strip (420 px), over the art; it covers part of the VAB's marker.
  - *"Read the paper"* on each lost person's memorial page reopens the page; "Open the memorial" on the losing
    agency's copy visits the first of the crew's.
  - The "resumes crewed flights" line is not built (unconfirmed, and a second loss restarts the count).
- **Later** (the user's call, not specified): secrecy, a Soviet agency keeping a loss quiet at the risk of
  rivals' intelligence uncovering it later (Bondarenko's death in 1961 was not revealed until 1986).

### 4.19 Archive *(the user's idea, 2026-10-15; specified, mocked up and built the same day for `design.md` §2.12)*
**Purpose:** pages about the real missions that sent back something memorable, unlocked when a goal is
first reached in the game, so a flight feels like getting its pictures back. The pages, images and
rights are in `assets-raw/archive/PAGES.md`.

**The Museum's Archive tab** (§4.11):
- **The list:** one card per page, in the order of the real missions' dates (1958 to 1990), so it reads
  like a timeline.
- **An unlocked card:** a cropped thumbnail of its first image, the title ("Apollo 8: Earthrise"), the
  real date and agency, a one-line summary, and the gold "new" dot (component 19) until it's opened.
- **A locked card:** greyed out, with no image (a blank frame in the faint navy tint, like the Flights
  tab's missing photos), the title, and "Unlocks with: People in lunar orbit", the goal's name (user,
  2026-10-15).
- **A count in the header:** "5 of 12 in the archive".

**The page** (opened from the tab or the turn report; it closes back to where it was opened), laid out
like an encyclopedia article:
- **Header:** the title, an eyebrow with the agency and date ("NASA · December 1968"), and when it
  unlocked in this game ("In the archive since Spring 1967, flown by you" / "by a rival").
- **Left column, the text:** three to five short sections with headings: the mission, what it found, why
  it mattered. At the end: Sources, a short list.
- **Right column, the fact box:** craft, launch vehicle, launch and end dates, crew or none, the
  mission's firsts. Under it, the audio player, if the page has a sound: play/pause, a progress line,
  the length, and the caption with its credit ("Genesis reading, 24 December 1968. NASA.").
- **The gallery,** under the text or beside it: three to five images, each with a caption and its
  credit line under it in caption style. Clicking an image opens the viewer.
- **The viewer:** the image as large as the screen allows over a scrim, caption and credit under it,
  arrows or the arrow keys to move through the page's images, Escape or a click outside to close.
- **Audio:** plays on the music volume (Settings) and stops when the page closes. Any replay sound
  stops while it plays (*proposed*).

**Turn report** (§4.4): a "New in the archive" card the turn a page unlocks, on every player's report:
- each new page's thumbnail and title, "Flown by you" or "Flown by a rival" (the rival unnamed), and
  "Open";
- it sits with the Museum-like cards, after the Firsts.

**Server work:** G15.

**Mocked up (2026-10-15)** on the canvas `Launch Window — Archive` (the Artifact tool's list finds it): the
Museum's Archive tab, Apollo 8's page (flown by you; flown by a rival with the sound playing), Mariner 4's
page without a sound, the image viewer, and the report card with two pages and with one. The pictures are
placeholders tagged with their real ids. Choices, all **unconfirmed**:
- **The tab:** the sixth, after Encyclopedia. The header line reads "5 of 12 in the archive · 2 new". The 12
  cards form a 6 × 2 grid under a timeline rail: the year and a dot per card (white in the archive, gold new,
  hollow locked), dashed across the jump from 1976 to 1990. An unlocked card is the Encyclopedia card's frame
  with a 158 px thumbnail, the eyebrow, the title, the summary and "With sound" where there is one; the whole
  card opens the page. A locked card keeps its eyebrow and title, muted, on a blue-grey card, with the lock icon
  in the faint navy frame and "Unlocks with:" and the goal at the foot. *Open:* a gold dot on the Archive tab
  itself would need a new NavTabs property.
- **The page:** three columns at 1920: the text (676 px), the gallery beside it, and the fact box (440 px). The
  unlock line sits under the title. The article text is a new style (Work Sans 14/22, headings Archivo 700
  17 px navy), with numbered Sources. The gallery uses 5:4 crops in two columns; with an odd count the first
  spans both at 16:9. The fact box has a navy head, label and value rows, and Firsts with gold squares, and a
  footer line names the goal that unlocked the page.
- **The audio player:** a 40 px navy square button (the play and pause icons), a 4 px track with a knob, the
  time and the caption with its credit. Playing shows "PLAYING" in gold-text with a small four-bar glyph,
  which is optional.
- **The viewer:** a near-opaque navy scrim (`rgba(12, 22, 42, 0.93)`, a new token), the whole picture as large
  as fits (uncropped, so Apollo's frames show square), and the caption and credit under it. "2 OF 4" sits top
  left and the close button with "ESC" top right. 48 px square arrow buttons have the arrow keys hinted under
  them. At either end that side's arrow is hidden.
- **The report:** "New in the archive" sits in the left column right after Goals. Each row has a 112 × 72
  thumbnail, the eyebrow, the title, "Flown by you" (success, with the check) or "Flown by a rival" (muted),
  the goal and a primary Open. The card has a navy line, and no cell in the summary strip.
- **No new icons:** play, pause, close, lock, chevron, check and document exist already.

**Built (2026-10-15)** as mocked up (`client.screen.ArchivePageScreen`, the Museum's `Tab.ARCHIVE`,
`TurnReportScreen.archive`, `client.view.Archive`), with these differences:
- **The wide picture** keeps its own shape, between square and 3:1, instead of 16:9, so a whole disc (Luna
  3) isn't cut.
- **No arrow hints** under the viewer's buttons: the fonts have no arrow glyphs (the keys still work).
- **Left out:** the playing state's four-bar glyph (the word "PLAYING" alone) and the dashed rail across
  the 1976 to 1990 gap.
- **No gold dot on the Archive tab** (*proposed*; NavTabs has none).
- **Line height:** the article's 14/22 sets as Work Sans 14 with its own line spacing, since the client
  applies no style's line height anywhere.
- **Picking the page from the report** returns to the report; from the tab, to the tab.
- **Sound:** plays on the music volume, needs Sound On, and stops when the page closes.
- **When the race is decided** (2026-10-15, `design.md` §2.12, not mocked up): that turn's report opens with
  a full-width card with the gold accent, like the Decision card: "THE ARCHIVE · ALL 12 PAGES OPEN", "The race
  is decided, and the whole archive is open", how many pages opened, and "Open the archive" (to the tab). The
  pages opened that way don't get rows in the "New in the archive" card; their header says "opened when the
  race was decided", and the tab's line says every page is open.
- **New in `Theme.java`:** `SCRIM_VIEWER`, and the text styles `ARTICLE`, `ARTICLE_HEADING` and
  `ARCHIVE_CARD_TITLE`. No new icons. `ScreenSnapshot -Dshots=archive` renders it all.

### 4.20 Findings *(the user's idea, 2026-10-19, `design.md` §2.13; built straight in libGDX on 2026-10-21, no mockup, user)*
**Purpose:** science goals whose results make later steps safer, and publishing them. Small additions to
existing screens, made of existing components; every look is unconfirmed.
- **Turn report, a Findings card** (left column, under Goals; only on a turn with something to say),
  eyebrow "FINDINGS", note "Science that makes flights safer". One row per line:
  - a finding earned this turn: "New finding: Mars mapped", "Your flights know it now. Safer: landing on
    flights to Mars.", the archive link when its page is open, and the finding's controls on the right;
  - the agency's own publication: "You published Moon mapped", "It helped 2 of your 3 rivals." (or "Every
    rival knew it already."; co-publishers named), the prestige on the right in green;
  - another agency's: "Orion Spaceworks published Venus's atmosphere studied", "Safer for you from now on:
    …" or "You knew it already."
- **The finding's controls** (`ChromeScreen.findingControls`): classified, a Publish button (secondary on a
  card, primary on the page's blue); asked for, a gold "Publishing" badge and Withdraw; published, a green
  badge, "Published by you" or "Published". They read today's state, on any report.
- **The confirmation dialog** (component 6's shape, 560 px): eyebrow "PUBLISH A FINDING", "Publish Mars
  mapped?", what publishing does, "Pays now" with "+287 prestige" (green, from the server's preview, G16),
  "It helps 2 of your 3 rivals." with the preview's caveats, and "Keep it classified" / "Publish". Nothing
  is sent before Publish; a failed preview is a warning toast and no dialog.
- **The Museum's Archive tab** also has the theories of the day's card, under this one (§4.18.2).
- **The Museum's Archive tab, a "YOUR FINDINGS" card** above the timeline while the agency knows any: one
  row per finding known (the pack's order), its name, "Makes safer: …" and its controls. The place to
  publish later, and the only place for a finding whose page isn't written yet. The tab's line adds "a
  finding's page, for those who know it".
- **Archive cards and pages of a finding:** a locked card says "A finding: opens when you know" where others
  say "Unlocks with:"; the page's top line says "published" where others say "flown by a rival", and carries
  "A finding. Safer for those who know it: …" with the controls on the right. The report's "New in the
  archive" row says "Published" for a finding a rival published.
- **The mission plan's odds** (§4.6.7): a step a known finding makes safer gets a green outlined chip with
  its points ("+4", "+8" for two) beside its name, with the findings in a tooltip; the table's caption says
  so.
- **Words** come from `client.view.FindingsView`: step kinds fold into a few names ("lunar-landing" and
  "lk-descent" read as landing and powered descent), and the body reads "around Earth" or "on flights to
  Mars".
- **Help:** a "Findings" topic (Rivals), on the mission plan, the turn report and the Museum.
  `ScreenSnapshot -Dshots=findings` renders the report, the dialog, the tab, a page, the help and the plan.

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
| Hands-on campaign (added 2026-10-11, component 26, type 11; `HandsOn.Outcome`, `RdDecisions.Outcome.NO_PUZZLE`): WRONG_PHASE / NO_SLOT / CANT_PAY / UNKNOWN_COMMAND / NOTHING_QUEUED / CANT_RESIST / NO_PUZZLE | Not now: the campaign is somewhere else. / The round's slots are full. / You can't pay for that command. / That command isn't in this campaign. / Nothing is queued. / Not enough shifts to call in an expert. / No campaign is in progress. |
| Findings (added 2026-10-21, §4.20; `Findings.PublishOutcome`): NOT_A_FINDING / NOT_KNOWN / ALREADY_PUBLIC / ALREADY_QUEUED / NOT_QUEUED | That goal isn't a finding. / Reach the goal first. / Someone published it already. / It's published at the next resolution already. / There's nothing to withdraw. |
| Contracts (added 2026-10-11, §4.17; built as `ContractOutcome`): CONTRACT_ACTIVE / NOT_QUALIFIED / PROGRAM_NOT_OPEN / UNKNOWN_LISTING; closing a program while its contract flies is the existing MISSIONS_IN_FLIGHT | You have an active contract: one at a time. / You can't fly this one: unlock its line first. / Open the program first. / That listing isn't on the board any more. / Can't close while a mission uses it. |
| The world reacts (added 2026-10-22, §4.18.1, component 28; `SubmitStatementResponse`): TURN_RESOLVED / UNKNOWN_PAGE / REACTION_WINDOW_CLOSED / NOT_A_RIVAL / INVALID_STANCE / QUOTE_TOO_LONG / QUOTE_BAD_CHARACTERS | The turn resolved before that reached the server, so nothing was done. / That front page isn't there any more. / Statements on that First closed when the turn after it resolved. / Only a rival that was in the race when the First was printed can answer it. / Pick one of the five stances. / At most 140 characters. / Only letters, numbers and common punctuation. |
| Join game (`ClaimSlotResponse.Reason`, added 2026-10-04): AGENCY_NAME_TAKEN / DIRECTOR_NAME_TAKEN / MISSING_FIELDS / ALREADY_CLAIMED / NOT_AUTHENTICATED / SERVER_ERROR | Already taken by another agency. (under the field) / the same / Required. / (client logs in again and opens the agency) / (danger notice with the server's words) |

---

## 6. Gaps the server, rules, or content must fill for M5

Found while writing this spec. The client can't build these screens from the
current state alone. Each is an M5 task.

- **G1. Game info.** *(Done 2026-10-04, except the pack download: `GameInfoRequest`/
  `GameInfoResponse`, `design.md` §3.6 and §5 M5. Game date: turn 1 is Winter 1955,
  one season per turn, confirmed 2026-10-06.)* The content pack id and its content hash (so the client loads
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
  - **Deferred (user, 2026-10-06):** content changes ship as a new server version, and the
    version check at login makes every client update (the client zip's update script), so a
    pack mismatch shouldn't happen in practice. Until the download exists, the client warns
    and uses its own copy.
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
  "per-player summary" `design.md` §3.7 planned for game-meta. `bot` (2026-10-25) says the server
  plays the agency itself.
- **G4. Display text.** *(Done 2026-10-05 for the early-era pack: `strings.json` beside the
  pack's rules files, read by `content.ContentStrings` and looked up through
  `client.view.ContentNames`. Shown as names everywhere, as descriptions on Program details,
  its components, the configuration summary, the replay's step card and the Museum's race
  goals, and as history notes on programs, components, configurations and rockets. Not part
  of the frozen, hashed pack. See `design.md` §5 M5.)* Content has ids only. The UI needs names and short
  descriptions for categories, programs, components, configurations, steps, and
  goals, written in our own words (never BASPM's, `design.md` §1.2). Add a
  strings file per pack. Each entry can also carry an optional history note
  (component 15), so the content screens can show the icon where a story exists.
- **G5. Personnel names.** *(Done 2026-10-04: `rules.PersonnelNames`, our own lists per
  archetype, well-known space-program people left out, seeded per candidate so no
  skill roll changes. Since 2026-10-05 each candidate gets a portrait first, from
  `rules.PortraitCatalog`, and a name and starting age that match it: name lists per
  ethnicity and sex in `core`'s `personnel/names.json`, the age from the portrait's age
  range. `Personnel.portraitId` says which portrait; the screens don't show it yet.
  Since 2026-10-10 no candidate gets a name anyone at the agency has (any roster, the
  memorial, the batch so far): the name is drawn again from the candidate's own stream,
  after a play test turned up two engineers called Joseph Sobczak.)*
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
  2026-10-04: `rules.DepartmentSalaries` and `AdjustSalariesAction`. **Once per department
  per season (user, 2026-10-06),** reversing the 2026-10-04 "no limit, as in BASPM"; built the
  same day: a second change is refused as `ALREADY_ADJUSTED`, and Manage department says so in a
  toast instead of opening)*.
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
- **G10. Memorial records (found by the Museum mockup, 2026-10-04).** *(Extended 2026-10-05: animal
  passengers too, `MemorialEntry.Cause.PASSENGER`, and every plaque shows its portrait or photo. See
  `design.md` §2.5(f).)* *(Done
  2026-10-04: `PlayerSnapshot.memorial`, a list of `MemorialEntry`, written by
  `MissionProgress` for a catastrophic step and by `RandomEvents` for the perished-astronaut
  event. The Achievements tab also got `PlayerSnapshot.goalRecords`, one `GoalRecord` per
  settled goal.)* Astronauts lost
  leave the roster as a `PersonnelDeparture` event (`LOST_IN_ACCIDENT`, or the
  perished-astronaut random event), and nothing keeps them. The memorial needs a
  persisted list per player: name, age, the turn, the cause, and for a flight loss the
  mission and the step.

---
- **G11. R&D decisions (component 26, 2026-10-10).** Server and rules: a pending decision on the
  player's state (one at a time, with its seed, its component and any arc it continues); the offer
  rule at resolution (`design.md` §5, the lull signals); a decide action resolved at once and
  journaled; the default applied when the turn resolves; report events for an offered, decided and
  lapsed decision (fields only added); a content file of decisions by type, outside the hashed pack
  like the stories; and the playout bots taught to decide, to measure how often decisions come.

- **G13. The hands-on decision (component 26, type 11, 2026-10-11). Built the same day** as specified
  (`rules.HandsOn`, `RdDecisions.handsOn`, `HandsOnAction`, two levels in `decisions.json`). Content: a hands-on decision's
  level in `decisions.json` (resources and their names, commands with inputs, outputs and primary
  output, rounds, slots, objective, bonus, start); a test searching each level's all-normal play.
  Rules: `rules.HandsOn`, a pure state machine (plan, execute, awaiting a resist, finished) over the
  puzzle state kept in `PendingDecision`, rolls from the decision's seed and the step index, odds from
  the team, rewards in seasons of R&D gain; settling an unfinished puzzle at the resolution. Server: a
  `HandsOnAction` (one action per step: queue, undo, reset, confirm, resist, accept), registered last in
  `MessageRegistry` and the journal's subtypes, refused out of phase. Client: the Hands-on screen, the
  banner's "in progress" state, four icons. Bots: a playout bot that plays it (greedy on all-normal
  play) to size the rewards.

- **G12. Commercial contracts (§4.17, component 27, 2026-10-11). Built the same day**, with two
  changes: the board is settled before the season passes and saved in each agency's state, so there
  is no board message (`design.md` §2.11, Storage), and the unlock sets are their own hashed file,
  `commercial.json`. As specified: Content: per program a
  `commercial` block (unlock set, contract configuration) in the hashed pack; clients and stories in
  `commercial-clients.json` outside it (in it since 2026-10-25, review.md 2.2). Rules: unlocking lines, the score, the award, the fee and the
  insurer, the window and lapses, the board's seating and extras; the contract flight's own payout
  path in `MissionProgress` (no prestige, no repeat count). State: unlocked lines, records, the
  active contract, the application. Server: the market settled once a turn in `market/turn-N.json`
  (awards before the pass, the board after), applications sealed, a board message pair appended to
  `MessageRegistry`, report events (fields only added), and `TurnRollback` deleting market files. A
  playout bot that takes contracts.

- **G14. Front pages (§4.18, 2026-10-11). Built the same day** as specified, with the name limit at
  32 characters (refused as `NAME_TOO_LONG`, appended to `ClaimSlotResponse.Reason`, and the join
  screen's fields stop there). Classes: `rules.FrontPagePrinter` (from each player's turn report, so a
  retried resolution prints the same), `model.FrontPage`, `server.schedule.FrontPageStore`
  (`frontpages.json`), the hook in `TurnScheduler.printFrontPages`, `TurnRollback` dropping the undone
  turns' pages, `RequestDispatcher.handleFrontPages`. As specified: Rules: after the goal race is
  settled in a resolution,
  find each First (the goal ids in `front-pages.json`'s keys that the pack has) that nobody held at the
  start of the turn and somebody holds now; fold the ones reached by the same flight into the latest;
  draw the story with the game seed salted by the goal id. Server: print a `FrontPage` per First into a
  shared, append-only `data/frontpages.json` (rewritten whole, like the game meta, and covered by
  `TurnRollback`): id (`<turn>-<goal-id>`), `goalId`, `gameTurn`, `storyId`, `foldedGoalIds`, and per
  achieving agency its agency name, director name and, for a crewed First, the crew's names. Agency
  names only, never account ids (as G3); the race box is rebuilt from the pages themselves. A
  `FrontPagesRequest`/`FrontPagesResponse` pair appended last to `MessageRegistry` returns every page
  of the game (a few KB; gzipped JSON like the report if it ever grows), fetched after login and after
  a `TurnResolvedNotice`. Client and server are rebuilt together. Content: `front-pages.json` and a
  `FrontPagesTest` in the `EventStoriesTest` mould: an entry for every First, two stories each, every
  token known, every paragraph set fits the two columns at the longest names allowed. **There is no
  name length limit today** (`RequestDispatcher` only refuses blank names): add one when claiming a
  slot (*unconfirmed:* agency 32 characters, director and astronaut names as `PersonnelNames` makes
  them), or the test has nothing to measure against.
  *Unconfirmed:* printing the crew's names tells rivals who flew. That's public news, but it names
  astronauts the Intelligence Office's personnel file would otherwise have to find.

- **G15. The archive (§4.19, `design.md` §2.12, 2026-10-15). Built the same day,** simpler than first
  specified: no event and no saved field.
  - **Content:** `assets/content/archive.json`, outside the pack like the stories, each page with its id,
    `goalId`, title, agency, date, `when` (its order), summary, facts, firsts, sections, sources, images
    (file under `ui/`, caption, credit) and an optional sound (file, caption, credit, seconds). Pictures
    and thumbnails are in `assets/ui/archive/`, sounds in `assets/audio/archive/`. `ArchiveTest` holds every
    page to a goal of the standard pack, its files to being there and credited, and the folders to
    nothing unused.
  - **Server:** `ArchiveRequest` (the client's pages' goals) and `ArchiveResponse` with `ArchiveUnlock`
    (goal, the first turn any player held it, whether the asker was among the first), appended to the
    registry. `RequestDispatcher.handleArchive` reads every player's latest snapshot through
    `WinCondition.goalTimes`, so a cascade credit counts from the turn of the goal that credited it.
  - **The race decided:** with `game-result.json` written, every goal asked about that nobody held by its
    `decidedOnTurn` answers with that turn and `atTheEnd` (a fourth field on `ArchiveUnlock`).
  - **Client:** the Museum and the report fetch it, the session keeps it (`ClientSession.archive`), and a
    report shows the pages whose unlock turn is its turn (`Archive.openedIn`). A page whose goal the game's
    pack doesn't have isn't shown.
  - **Rebuild client and server together** (a new message); no reset.
- **G16. Findings (§4.20, `design.md` §2.13, 2026-10-21). Built.**
  - **Content:** each pack's `findings.json`, a rules file in `ContentPackLoader.FILES` (hashed): a goal and
    the step kinds it makes safer, each with an optional body and crewed flag.
  - **State:** `PlayerSnapshot.publicFindings` (goal to the turn it became public, the same in every
    agency), `pendingPublications` (this turn's asks) and `publishedFindings` (the agency's own).
  - **Action:** `PublishFindingAction` (goal, publish or withdraw), appended to `PlayerAction`'s subtypes
    and the registry.
  - **Server:** `PublishPreviewRequest`/`PublishPreviewResponse` (prestige, helped, rivals), appended to the
    registry; `RequestDispatcher.handlePublishPreview` reads every player's latest snapshot. The turn's
    publications are settled once (`FindingPublications.settle`) into `publications/turn-N.json`;
    `TurnRollback` deletes them with the turn. `handleArchive` opens a finding's page only for agencies
    that know it (or at the end), and a late joiner starts with the findings already public.
  - **Events:** `FindingPublished`, `FindingLearned` (append-only ids `findingPublished`, `findingLearned`).
  - **Reset the dev data root and rebuild client and server together** (saved fields, a pack hash change,
    a new action and message).
- **G19. In memory (§4.18.3, component 29, 2026-10-22). Built.** As decided at build (the advisor's call): the four
  tones and the two condolences are `FrontPage.Stance` values appended (`tone()`, `condolence()`; `givable()` stays
  the First's five), so the eulogy and the condolences are ordinary statements on the loss's id, kept by
  `ReactionStore` (sealed and rolled back as the reactions are) and sent with `SubmitStatementRequest`; the
  dispatcher checks a loss id before the front pages (its agency may send only a tone, everyone else only a
  condolence). `losses/turn-N.json` (`LossStore`) is a result of turn N, rewritten whole by a retry and dropped by a
  rollback of N; `FrontPagesResponse.losses` carries the turn before's. `rules.MemorialPrinter` finds the losses
  (each grounding with the departures before it and its `GoalsWithheld`, the crew from the memorial) and prints the
  pages first of the turn's; at most 4 condolences are told one by one, the rest share a sentence. The words are in
  `assets/content/memorial-pages.json` (`MemorialPagesTest`), the article put together by `client.view.MemorialView`.
  - **Model:** `FrontPage.Kind.MEMORIAL` (appended); the page carries the lost crew (name, age, portrait, flights,
    firsts flown, a revealed trait), the configuration and failed step, the withheld goals, the agency's tone and
    quote (or the spokesman's), and the condolences (`FrontPage.Reaction`, with two stances appended:
    `CONDOLENCES`, `TRIBUTE`; the eulogy's four tones as their own enum).
  - **A loss is public a turn early:** the resolution that loses a crew records a notice in
    `losses/turn-N.json` (rival key, agency, mission, the crew's public data), which the next turn's
    statements and condolences answer and the next resolution prints from; `TurnRollback` drops the later
    turns' files as for the reactions.
  - **Messages:** the statement and the condolences reuse `SubmitStatementRequest`'s shape (the loss's id in
    place of a page id), or a new pair; `FrontPagesResponse` (or the report) carries the open losses. Decided at
    build.
  - **Rebuild client and server together;** no reset (new fields only).

- **G18. Theories of the day (§4.18.2, 2026-10-22). Built.** `FrontPage.Kind.SCIENCE` (appended; its story id is
  the theory's), `rules.SciencePrinter` in `TurnScheduler.printFrontPages`, settled once per theory (the pages of
  earlier turns are read back), kept in `frontpages.json`, so retries and `TurnRollback` need nothing new. The
  questions are derived on the client. `FrontPage.firstPage()` replaces "not a follow-up" wherever a First is
  meant. Rebuild client and server together; no reset.
- **G17. The world reacts (§4.18.1, component 28, 2026-10-22). Built.**
  - **Model:** `FrontPage` gains `kind` (FIRST or REACTIONS; missing reads as FIRST, so old files load),
    `reactsTo` and `reactions` (`FrontPage.Reaction`: agency, director, stance, quote, and the ids of its
    line, link and detail). `FrontPage.Statement` (page, stance, quote) is what a player gives.
  - **Messages:** `SubmitStatementRequest`/`SubmitStatementResponse`, appended to the registry;
    `FrontPagesResponse` gains the requester's own statements for the open turn.
  - **Server:** statements kept in `reactions/turn-N.json` by `GameSeeds.rivalKey`, never account ids;
    `rules.ReactionPrinter` prints the follow-ups in `printFrontPages`, in one `FrontPageStore.keep` with
    the turn's Firsts; `TurnRollback` keeps turn T's statements and deletes later ones.
  - **Client:** `ServerGateway.submitStatement` (a write, never repeated), `FrontPagesResult.yours`,
    `ClientSession.statements`, the refusals in `client.ui.Refusals`.
  - **Sealed on reading** (2026-10-22): the resolution seals the turn as it reads its statements
    (`ReactionStore.seal`, a `turn-N.sealed` marker), and a statement or withdrawal arriving after that is
    refused with `TURN_RESOLVED` instead of being kept unprinted. A rollback reopens the kept turn (its seal
    goes, its statements stay).
  - **DevClient** (2026-10-22): `pages` lists the printed pages and your statements, `react <pageId>
    <stance|withdraw> [quote...]` gives, changes or withdraws one for the open turn, for trying the refusals.
  - **Rebuild client and server together; no reset.**

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

**Mocked up (2026-10-11, canvas `Launch Window — Contracts`):** Contracts (§4.17) and the contract
marker (component 27): 12 artboards, choices listed under each. New for `Theme.java`: the contract
icon (two files, on the UI Asset Sheet), the contract badge, the listing card and its variants, the
switch, the contract stepper, the report's Contracts card and cell. No new colour tokens.

**Mocked up (2026-10-11, canvas `Launch Window — Hands-on`):** the hands-on decision (component 26, type
11): 8 artboards, choices listed under type 11. New for `Theme.java`: four resource icons (on the UI
Asset Sheet), the objective chip, the bonus row, the muted command row, the queue slot and its badges,
the odds bar, the result panel. No new colour tokens.

**Mocked up and built (2026-10-11, in Photocraft, `assets-raw/frontpage/mockup/`):** the front page for
the great Firsts (§4.18), shown on login before the turn report. The first screen mocked up outside Claude
Design: the art is generated with ComfyUI and the page laid out in Photocraft over the hub. New for
`Theme.java`: two fonts (Old Standard TT, Libre Caslon Text) and perhaps a scrim token; no new colours.

**Mocked up and built (2026-10-10, canvas `Launch Window — R&D decisions`):** the R&D decision (component 26): the banner on an R&D card
open, one per type (ten), the confirmation, an outcome good and bad, the turn report's Decision card
and strip cell, and the hub marker's decision dot beside the idle-staff dot. A new component with a
gold accent; flag any new token for `Theme.java`.

Mockups don't need to be pixel-final. Layout, components, and states are what
implementation needs. Flag any new color, component variant, or icon so
`Theme.java` and the UI Asset Sheet stay in sync.
