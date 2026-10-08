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
  giving the window about 87 px of extra height.

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
   a confirmation.
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
         payload, 0 kg for every early-era rocket.
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
         game: every early-era rocket is fuelled on launch day (the Countdown step).
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
      letters. **The first seven topics** (2026-10-08) are there for the user to judge the format:
      turns, the budget, programs, component R&D, team chemistry, poaching and intelligence. The
      rest (about 30 to 40 in all) follow once the format is confirmed.
    - *As built:* `client.screen.HelpOverlay`, `client.view.Help` and `HelpValues`; the `help` and
      `search` glyphs in `make_ui_icons.py`, on the UI Asset Sheet since 2026-10-08 (chrome section,
      after `settings`, with the header, hub strip and search field in context). Shots `help-*.png`.
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

### 4.1 Connect
**Purpose:** connect and authenticate. One form: server (host:port), login,
password. A new login creates the account. There's no display name: other
players see you by your agency and director name, chosen per game (§4.2).
**States:** connecting;
wrong password; login has invalid characters (`[A-Za-z0-9_-]`, up to 32); client
and server versions don't match (show both versions); server unreachable.
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
kind follows it (2026-10-08, component 23).

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
card at a shorter art height (`ConfigCards`). A card opens the mission plan on its assessment
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
missing goals). Other agencies' programs are hidden entirely.

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
time, 30 % after that), launch types, and crew / controller / CAPCOM counts.
Viewable even when the program isn't open *(decided)*; then the planning screens
are read-only, with an "Open program" call to action.

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

### 4.11 Museum: goals, history, rivals
**Purpose:** the scoreboard and the archive. **Ref:** `museum*.png`.

**Tabs:**
- **The race:** the declared goals as goal badges, own progress, and every
  player's progress side by side, by agency name and director name: which goals they
  hold and the turn they got them. **Needs server work:** G3.
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
  "per-player summary" `design.md` §3.7 planned for game-meta.
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
