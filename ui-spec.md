# Launch Window — UI screen spec (M5)

Every screen and shared component of the client as built: what each shows (mapped to the
game state), what the player can do there, its states, and the reasons behind the choices.
The libGDX client is the reference for anything built; this file is the brief for a new
screen's mockup, made in Claude Design against the installed **launchwindow** design system.
The pre-2026-11-12 text, with the dated build narratives and mockup notes, is in git
(`git show 5c95135b:ui-spec.md`).

The screen flow follows the user's reorganization of BASPM's building layout (§3, 2026-10-03):
the VAB owns rockets, Headquarters programs and mission planning, the SET Centre payload R&D,
Mission Control the missions. *(decided)* marks decisions from that review.

**Read first:**
- `design.md` §3.12: palette, typography, border and fill rules. Colors carry
  meaning: navy is neutral/primary, gold is in progress, green is good, red is
  destructive or failed (reserved), violet is unused.
- `design.md` §3.13: the main-menu hub, and the header and footer shared by every
  screen.
- `core/src/main/java/de/mkoehler/launchwindow/client/ui/Theme.java`: the code
  twin of the design tokens.

**Reference material in this repo:**
- `assets-raw/main_menu/`: the hub art, NASA and Soviet, 4 seasons × 4 times of
  day, plus `<faction>.hotspots.json` with the building polygons.
- `assets-raw/backgrounds/solar_system.png`: art for the celestial-body picker.
- `assets-raw/Program Categories/`, `assets-raw/Programs/`,
  `assets-raw/Mission Configuration/`: program and mission art, tracked in
  `assets-raw/ART_CHECKLIST.md`.
- `assets-raw/svg/icon_*.svg`: one icon per skill (all 15).
- `assets/branding/logo.svg`: the mission-patch logo.

**Fixed decisions:**
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

**Notes for mockup sessions** (Claude Design gotchas from earlier canvases):
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
- **Local screenshots of an artboard:** `assets-raw/tools/shoot_canvas.py` (save the canvas's
  `artifact-type/dc-runtime.js` beside the `.dc.html` files as `support.js`, copy the `/_blob/`
  assets into `_blob/`, serve over HTTP, headless Chromium with about 87 px of extra height;
  `pip install playwright`, `python -m playwright install chromium`).

---

## 1. How the client works (what the mockups must account for)

- **Preview state.** After login the client fetches the player's state with
  everything queued today already applied. Funds have dropped, the program shows
  as open, the hire is on the roster. Every successful action refreshes it.
- **Action results.** Every action returns either success or a refusal with a
  reason code, shown as a toast or an inline message near the control (§5).
- **No live updates.** Nothing changes on screen without a player action, except
  after a turn resolves, when the player comes back to a turn report (§4.4).
- **Two clocks.** The *game date* (for example "1958 · Spring") is shared by
  everyone. A player's *own season count* only drives their budget reviews.
  Headers show the game date.

---

## 2. Shared chrome and components

Detailed in `design.md` §3.13; summarized here with the data each element shows.

**Header (every screen except the hub):** back, home, settings and help icon buttons
(component 23); a breadcrumb trail (earlier crumbs clickable, the current one in upper case).
On the right: a prestige chip, a game-date chip ("1958 · S2") and a next-resolution chip
("Next turn in 6 h 12 m"), in an async game the most useful single fact.

**Footer (every screen except the hub):** five clickable stat chips:

| Chip | Shows | Opens |
|---|---|---|
| Funds | balance, as the danger tag when negative (the last season's net change isn't shown: the state doesn't carry it) | Public Affairs Office |
| SET | hired / busy / cap | SET Centre |
| Astronauts | hired / busy / cap | Astronaut Complex |
| Flight controllers | hired / busy / cap | Mission Control |
| Open programs | open / Headquarters cap | Headquarters |

"Busy" counts anyone not idle: in training, assigned, or on R&D.

**Reusable components:**
1. **Personnel row.** Name, age, morale (rated with color), salary, state badge
   (Idle, In training · kind · seasons left, On mission · id, Researching ·
   component · slot, Unavailable · n seasons), the group's five skills as icon +
   value, and Expert stars on the skills that have them. The portrait
   (`Personnel.portraitId`) leads the row (`RosterCells.avatar`), initials as the fallback.
   Row actions are listed per screen.
2. **Candidate card or row.** The same fields, plus a Hire button, the portrait and the
   learning potential (a number chip). Next to the age, the years left until the group's
   retirement age (astronauts 50, flight controllers 60, SET 65): "AGE 41 · RETIRES IN 24 YRS"
   ("RETIRES THIS YEAR" within a year). Age is a real trade-off (older candidates start higher
   but learn less and retire sooner), so both sides show at a glance (user, 2026-10-05).
3. **Reliability bar.** The component's current reliability, a marker at its R&D
   ceiling (`maxRdReliability`), and the bar's end at its absolute maximum
   (`maxReliability`). Label "62.4 % (R&D cap 85.2)".
4. **Price breakdown.** Line items and a total, for example opening cost −
   discount + component creation costs. Also used for launches (configuration
   cost + rocket unit cost) and upgrades.
5. **Confirmation dialog.** Title, consequence text, the price if any, a primary
   action in red for final or destructive actions, and Cancel.
6. **Action result toast.** Success in green, refusal in navy with a readable
   reason (§5).
7. **Building panel.** Level n of max, the current cap, upkeep per season, and an
   **Upgrade** button showing cost and build time ("Level 2: 13 controllers,
   900 · 1 season · final"). While building: "Upgrading, 1 season left". On the five
   building screens with a cap: Astronaut Complex, Mission Control, SET Centre, Headquarters,
   and the VAB. Upgrading is final, so it needs a confirmation, which also gives the new
   level's upkeep ("Its upkeep rises from $ 150.0 to $ 400.0 a season once the upgrade is
   done.") and, below the price, how long the funds last with it, counted from now on, the
   safe side (component 25's funds runway; user, 2026-10-23). Shots: `hq-upgrade.png`,
   `vab-upgrade.png`.
8. **Goal badge.** Each goal's own embroidered mission patch (`assets/ui/patches/<goal-id>.png`,
   128 px RGBA, from `assets-raw/patches/make_patch_assets.py`). A done goal shows it in colour;
   an open goal a greyscale copy (`<goal-id>_open.png`) at 60 % opacity, with no check or star
   glyph (user, 2026-10-06). A goal without patch art falls back to the drawn `logo.svg` circle
   with a check or star, as do the program screens. In the Museum (race table, "This game is won
   by", achievements) patches are 64 px (`RaceTable.PATCH_SIZE`), the smallest size their text
   stays legible at (user: 30 and 40 px were illegible).
9. **Today's queue panel** (optional; not built, G6). A collapsible list of
   today's actions with their results ("Opened Sputnik 1 · −876"). Undo is by
   opposite action only, so this is how a player would see what they've committed to.
10. **R&D board** (used twice: Rocket R&D in the VAB, Payload R&D in the SET
    Centre; the same cards sit read-only on the mission plan). One card per owned, active
    component of that kind, two to a row; both cards of a row are the same height, a shorter
    one's team chemistry and slots on its bottom edge, except in a row with an R&D decision,
    whose height would leave a gap in the other card *(user, 2026-11-01)*. Each card has:
    - its reliability bar;
    - its four R&D slots: slot 1 earns the most, slot 4 the least, and each slot
      shows the specialist in it or "empty";
    - the component's skill icon (which SET skill helps);
    - a tinted "Team chemistry" row (component 21) between the caption and the slots, the
      breakdown on hover or click;
    - milestone ticks and the seasons to each above the bar, and a retirement warning under
      the caption (component 25).

    Assign an idle SET specialist (they take the lowest free slot) or release one.
    "Cap reached" shows when the R&D ceiling is hit; researchers are then released
    automatically. A revealed Perfectionist on the team raises the cap: the caption says
    "raised to 94.4 while Fedorchuk, a perfectionist, is on the team" and the bar gets a dashed
    gold marker (`ReliabilityBar.raisedCap`). The cap, the marker and "cap reached" all read
    `TraitEffects.effectiveCap(..., true)`. Past that cap with people still in the slots (a
    hidden Perfectionist's doing) the card leaves out "Released automatically", so the early
    tell doesn't look broken. The assign dialog is a table, 1220 px wide, of idle engineers,
    best in the component's skill first: a "The team now" strip with its chemistry, then per
    candidate traits (component 20), "With this team" relationship marks (component 22), the
    skill, a "Chemistry with them" preview, "Seasons to cap" and Assign.
11. **Personnel overview** (the landing screen of each personnel building,
    *(decided)*, as in BASPM). The roster as personnel rows with row actions;
    filter and sort by state and by skill (skill icons); a **department summary**
    strip (headcount, in training / assigned, near retirement age, average morale,
    total salary per season); and buttons to the sub-screens: **Hire** and
    **Manage department**. A 230 px Traits column after the state (`RosterCells.traits`, the
    revealed traits, component 20), with "Still getting to know each other" under any chips while
    someone is inside the reveal window; the name opens the person page (`RosterCells.who`, §4.14).

    **Row actions:**
    - **Train:** opens the training dialog (component 12);
    - **Expert training:** astronauts and controllers only, once in a person's
      career *(user, 2026-11-01)*, when a skill is at 80
      or more; open-ended until the trait is earned;
    - **Pull out of training:** partial credit for fixed-length kinds, nothing for
      Expert; needs a confirmation, which says the share kept ("They keep 20 % of this round's
      gain. 60 % of the gain comes with the round's last season.", user, 2026-11-05). **All hands on deck** *(user, 2026-11-01)*, a
      title-bar button on the Astronaut Complex, Mission Control and the SET Centre, with a count,
      shown only when someone can be pulled out, does it for everyone at once, basic
      training excepted; its confirmation says what the pull costs;
    - **Keep Busy** *(user, 2026-11-11)*: a title-bar button beside Hire on the same three
      screens, shown only when someone idle and available has a skill below 90. One click, no
      confirmation: everyone idle goes into permanent training to 90 in their highest skill
      still below 90 (ties to the skill first in the department's order); anyone at 90 or more
      in every skill is left alone (`client.view.KeepBusy`). A toast says how many started;
    - **Dismiss:** free, refused for an astronaut or controller on a mission
      (crew, CAPCOM, or controller); SET can be dismissed mid-R&D, which frees the
      slot; needs a confirmation.
12. **Training dialog.** Advanced (3 seasons) or Permanent (advanced
    rounds until a target level), plus Expert for astronauts and controllers. No
    Basic: basic training only follows a hire *(user, 2026-10-05)*. Pick the skill
    with the skill icons, each showing the employee's current value in it, and set
    the target level for Permanent. Nobody in basic training has a Pull out button:
    it can't be cut short.
    Under Permanent's target slider, an estimate updated as it moves (user, 2026-10-10,
    component 25): "About 12 seasons (4 rounds of 3), done by Winter 1958; 9 to 12 seasons in 8
    runs of 10." (`rules.TrainingPlanning`: the season pass's rounds with the training factor's
    midpoint, revealed traits only, and 300 random runs for the spread; `TrainingPlanningTest`
    checks it against 1,000 real trainings). A target the gains can't reach says where training
    stops ("Gains shrink toward 100, so training stops at about 99.6 after 60 seasons."), and a
    retirement that comes first is named. Shot `set-training-permanent.png`.
13. **Manage department dialog** *(decided: BASPM's one-off adjustment)*. A slider
    from −50 % to +50 % that changes **every salary in the group once** by that
    percentage. Morale changes immediately: by +p for a raise of p %, by −3×p for
    a cut of p %, clamped to 0–100. The dialog previews the projected department
    salary per season and the projected average morale before OK (G8).
14. **Hire screen.** The group's candidate pool (it refreshes every season), as
    candidate rows, sorted and filtered by skill. Hire is refused when the
    building's roster cap is reached. New hires go straight into basic training on
    a random skill (astronauts 4 seasons, SET 2, controllers 2, BASPM's lengths;
    user, 2026-10-05). It can't be cut short, and the hire screen says how long it
    takes.
    - **Signing bonus** (user, 2026-10-06): each selected recruit has a check box, "Signing
      bonus · $ 48 once · morale 71 → 78": a one-off payment of one season's salary that
      multiplies the new hire's morale by 1.1 (at most 100). The footer adds up the bonuses next
      to the added fixed costs. A hire whose bonus the funds can't cover is refused.
    - **The Recruits panel scrolls** (the user's play test, 2026-11-11), as the candidates do,
      so a long selection never pushes the Hire Recruits row off the screen. Shot `hire-many.png`.
    - **A former colleague's hint** (`design.md` §2.2(d)): each candidate shows one word, such
      as "driven", "calm" or "reliable", under the age line: "A former colleague:" then the word
      in italics; the selected recruit's card repeats it. No pronouns, since names don't say how
      someone wants to be referred to. Traits themselves stay hidden.
    - **Background check** (`design.md` §2.9, Intelligence): under the hint, "Background check ·
      uses 1 of 1 action" with the binoculars. A click asks the server and answers at once in the
      same place: trait chips (component 20) or a framed "No notable traits", with a line saying
      where it came from ("Background check, this season" or "Found by the Intelligence Office",
      the passive 20 % find per candidate, shown from the moment the pool appears). The button
      stays while there's more to find. Below level 1 or with no action left it is disabled,
      the reason under it ("Needs the Intelligence Office at level 1", "No intelligence action
      left this season"); it shows at level 0 too, so players learn it exists. A revealed trait
      rides along on the selected recruit's card, beside the hint.
15. **History note** *(decided 2026-10-04)*. A small navy open-book icon next to a label,
    field, or value (`client.ui.HistoryNote`). On mouse-over it shows a short card, two to four
    sentences, about the real history or engineering behind that thing; clicking pins the card
    open until its × or the icon is clicked again. The card is flat white (the user dropped the
    design system's one drop shadow, 2026-10-04) and goes on the stage itself, so no parent
    clips it. It's the educational layer's in-the-moment form (`design.md` §1.1). Rules:
    - **It's always optional.** Nothing in a history note is ever needed to play.
      Rules help (how a mechanic works) is a different thing and never uses this icon.
    - **Its glyph is its own,** never a generic "i" or "?", so players learn that it always
      means "a real-world story".
    - **The text is our own,** written from real-world facts, never from BASPM's
      historical-facts text (`design.md` §1.2).
    - **The join screen has the first one** (§4.2), where players meet the icon.
    - **No new ones** (user, 2026-11-12, `design.md` §1.1): the last three added were the Expert
      panel's (Apollo 13), the Contracts screen's (Telstar) and Luna 17's (Lunokhod 2).
16. **Waiting indicator** (the user's play-test request, 2026-10-05). When a
    server call takes longer than 0.3 s, a small navy chip, "WAITING FOR THE SERVER" with three
    dots lighting up in turn, shows in the middle of the header row (empty there on every
    screen, the hub included). It goes away when the answer arrives. `SessionScreen` draws it
    from `ServerGateway.isWaiting()`, so every in-game screen has it. Connect and Join keep their
    own busy buttons.
17. **Tooltip** (the user's play-test request, 2026-10-05). Rules help on hover:
    a small navy card with a bold title and one to three short sentences, shown 0.4 s after the
    pointer rests on the thing it explains (`UiKit.tooltip`; the text lives in `client.view.Tips`,
    with numbers read from the rules' constants). It's deliberately unlike the white history-note
    card: a tooltip says what something means and does in the game, a history note tells a
    real-world story (component 15). Used on: the salary, morale and learning-potential chips;
    every skill icon in the roster rows, the hire screen's skill names and the training dialog's
    skill tiles (what each skill is used for); the footer chips (what "6 / 3 / 7" means, where a
    click goes); the header's prestige, date and countdown chips; and sort buttons.
    - **Roster skill icons are 17 px** (user) on all three personnel overviews.
    - **Flight Director rating:** the controller hire screen shows a sixth row, "Flight Director
      (avg.)", the average of the five skills (`MissionControlOfficer.flightDirectorSkill()`, the
      value a FLIGHT console reads), and can sort by it. The Mission Control overview has a sort row
      (the five skills plus Flight Director, best first; click the active one again for roster
      order).
18. **Explainer** *(the user's idea, 2026-10-06; `client.screen.ExplainerScreen`)*. A short series
    of full-screen "infographic" sheets that explain an aerospace idea as it comes up in play. It's the third kind of help, beside the tooltip
    (what a game thing means) and the history note (what really happened): an explainer says
    **how and why something works** in the real world.
    - **Look:** each sheet is drawn like an engineering drawing on the blue page, full screen with
      no app chrome: a white sheet with a navy line drawing on the left, three numbered points on
      the right, a gold "In Launch Window" strip tying the idea to a mechanic, and a drawing title
      block (drawing number, title, sheet n of m, scale). The quoted name below is the title (the
      title block's short name, so it fits the eyebrow, title block and Museum card alike); the
      topic goes in the eyebrow (`Explainer.topic`). A one-sheet explainer's eyebrow has no sheet
      count.
    - **Controls:** Close (top right), Previous and Next with clickable page dots, Done on the
      last sheet. Close and Done go back to the screen that opened it.
    - **How players find it** *(user, 2026-10-07)*: concept chips (component 19) on the screens
      that show the content an idea belongs to, plus the Museum's Encyclopedia tab, whose cards in
      a row are stretched to one height and use a 1x thumbnail of each first drawing
      (`<name>-thumb.png`, written by the converter, without halo outlines, which would read as
      smudges without their labels). It never opens by itself.
    - **Text** lives in `assets/content/explainers.json` (`client.view.Explainers`), outside the
      frozen pack like `strings.json`; `ExplainersTest` checks every string and drawing. Every
      figure is checked (`design.md` §1.1). Apostrophes are straight.
    - **Canvas:** `Launch Window — Explainers`, one row per explainer, each with a yellow note of
      the figures to verify and what changed from the brief.
    - **Drawings** are SVG sources in `assets-raw/explainers/`, rasterized at 2x without their
      text by `make_explainer_art.py`; the text goes to a labels file beside the PNG and the
      client sets it as real labels (`client.ui.DrawingView`), so it stays sharp at any window
      size. A sheet's bar chart and key are drawn by the client from values, not pictured (E-23
      sheet 1 and both E-26 sheets draw their bars in the picture, as their mockups do).
      A drawing may carry the mockup's own `<style>` block: resvg styles the shapes with it,
      and a text class's font size there becomes the label's size. Text inherits class,
      anchor, fill and `translate()` from its groups. Two kinds of text stay in the picture,
      set by resvg in the repo's own fonts: rotated text (E-02 sheet 2's "Line of sight"; a
      label can't turn), and the white outline under text of class `halo`, drawn last over every
      line, so its label stays readable over hatching or crossing lines. The drawing frame hugs
      the drawing and sits centred in its column. No mid-sentence bold (a label can't switch
      fonts). Closing an explainer frees its full drawings (`UiKit.releasePictures`; user
      2026-10-07), so reading every sheet doesn't pile up hundreds of MB of textures; reopening
      one loads them again, a short wait.
    - **Type:** the reading page sets larger type than the working screens: `Theme` styles
      `EXPLAINER_TITLE` (38), `POINT_TITLE` (21), `READING` (16/24), `CAPTION_LG` (14),
      and the drawing styles `DRAWING_LABEL` (15), `DRAWING_LABEL_SM` (13), `DRAWING_HEAD` (13,
      upper case), `DRAWING_BIG` (26), `DRAWING_SMALL` (11, upper case), `DRAWING_TEXT` (13),
      and `DRAWING_LABEL_MD` (14). A drawing's `d-body` text is set in `CAPTION_LG`. Keep drawing
      labels short (13 to 15 px). Not design-system tokens yet.
    - **E-01 "Suborbital vs orbital"**, three sheets:
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
         payload, 0 kg for the first rockets, 47 t for the Saturn V.
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
    - **E-05 "Hypergolics and steering jets"** (the user's idea), two sheets:
      1. *Fuels that light themselves:* kerosene and oxygen, hydrogen and oxygen, and a
         hypergolic pair side by side, with temperatures to scale, how long each waits in the
         tank, how it lights, its hazards and its exhaust speed; the R-16 disaster. In the
         game: the first rockets are fuelled on launch day (the Countdown step); the Titan II and
         Proton burn self-lighting propellants.
      2. *Steering with small jets:* one craft from above, turning, sliding and stopping with
         its thruster quads; cold gas and peroxide in Vostok and Mercury; Gemini 8's spin. In
         the game: the Reentry preparations step and Cooper's hand-flown Faith 7.
    - **Wording the fact check set** (`design.md` §1.1): E-03's Heinlein line is "a saying
      credited to" him; the Saturn V is "some 24 times" the Atlas; E-04's Soviet probes' top stage
      had to *start* after a coast (a separate stage, not a relight), with the August 1962
      settling-rocket failure; the R-16 killed "at least 78"; Gemini 8 spun "up to about one turn a
      second"; E-05's "In Launch Window" says every early-era rocket burns liquid oxygen "in its
      main stages" (Jupiter-C's upper stages are solid); E-13's "A safer fuel"; E-28's Luna 15
      crashed "about two hours" before Eagle lifted off; E-22 says "about a third of an
      atmosphere" (the fonts have no ⅓).
    - **E-06 to E-19** (from the user's brief, 2026-10-07), in E-02's sheet template, two sheets
      each unless noted:
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
        include each real site's push (`design.md` §5 Backlog has the mechanic idea).
      - **E-15 "The hardest moment of the climb"** (Max Q), one sheet.
      - **E-16 "Batteries and sunlight"** (Power): *A satellite's power budget*; *Day and
        night in orbit*.
      - **E-17 "Keeping a rocket pointed"** (Guidance): *Balancing on a flame*; *Spinning to
        fly straight* (Jupiter-C).
      - **E-18 "Why satellites fall"** (Orbital decay): *Thin air, long drag*; *The Sun stirs
        the air*.
      - **E-19 "A rocket held up by its own pressure"** (Balloon tanks), one sheet.
    - **E-20 to E-30, for the standard pack** (2026-10-09). The "In Launch Window" strips use the
      standard pack's step names ("Transposition and docking", "Cruise to Mars", "Venus flyby"; E-30
      names the Around the Moon step). Mars's air is "less than 1 %" of Earth's; the X-15's height
      "about 108 km" (354,200 ft). Sheet titles (the topic in brackets):
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
        (user, 2026-10-23: a chart of the speed a wing needs against orbital speed, von Kármán's
        84 km, the US 50 miles and the round 100 km; `design.md` §1.1).
      - **E-30 "Around the Moon and back"** (Circumlunar flight): *The free return*; *Coming home
        fast*.
    - **E-31 "Cabin air"** (topic: Breathing in space) *(the user's idea, 2026-11-04; every figure
      checked, `design.md` §1.1; drawings `e31-1` and `e31-2`)*. Two sheets. Id `cabin-air`. Summary: "Why the American capsules breathed
      pure oxygen at a third of the pressure, why the Soviet ones carried air, and what each choice cost."
      - **Sheet 1, *A third of an atmosphere*** (scale "Pressures to scale"). Drawing: pressure as height, one scale
        throughout (1 atm = the tallest column).
        - Left, two columns side by side: *Air at sea level*, 1 atm, a tall nitrogen block (0.78, hatched) under a
          gold oxygen block (0.21); *An American capsule*, 0.34 atm, all gold oxygen. A dashed line from the top of
          the air's oxygen, standing alone as a dashed outline 0.21 tall beside the air column, across to the
          capsule's column, and an arrow up to the 0.34, show the capsule's oxygen standing higher: "more
          oxygen than at sea level, a third of the push". The last 0.01 of the air column is an unlabelled grey sliver.
        - Right, *Inside the lungs*: three short bars, each with a grey cap of the same height for the share water
          vapour and carbon dioxide always take (labelled once, "Water vapour and carbon dioxide: about 0.11"; keys
          "Grey:" and "Gold: the oxygen left") and the oxygen left below it: air at sea level 0.13
          atm; pure oxygen at 0.21 atm (dashed, "never flown") 0.10; the capsule 0.22. A thin red line low on the scale
          at 0.06 atm, its text on three lines: "Below this, water boils at body heat (about 19 km up)". The scale
          has only "0" and "1 atm".
        - Points: **What partial pressure is** (air pushes with all its gases at once; each gas's part of the push is
          its partial pressure; at sea level oxygen pushes with 0.21 of an atmosphere, and that, not the total, is what
          the lungs need); **Why a third** (the hull holds a third of the load, so it can be lighter; one gas means one
          tank and one gauge, no mixing; a leak bleeds slower; Mercury chose it and Gemini and Apollo kept it);
          **Why more oxygen than the air has** (in the lungs water vapour and carbon dioxide always take their share, so
          pure oxygen at the air's 0.21 would leave less in the lungs than a sea-level breath; 0.34 leaves more, with a
          margin for a leak and for the suits, which ran lower still; not, as often said, to stop blood boiling: that
          takes under 0.06 atm).
        - In Launch Window: "Mercury, Gemini and Apollo capsules breathe pure oxygen at about a third of an atmosphere;
          Vostok, Voskhod and Soyuz carry air at sea-level pressure."
      - **Sheet 2, *Two answers, and their price*** (scale "Not to scale"). Drawing: two cabins side by side, cut open:
        an American cone ("Cone", "Thin wall"), "Pure oxygen, 0.34 atm", and a Soviet sphere ("Sphere", "Thicker
        wall"), "Air, 1 atm"; under each, two short tags, gold for what it gave and red for what it cost (American: *lighter,
        simpler* / *fire, prebreathing*; Soviet: *no fire worry, ordinary air* / *heavier hull, prebreathing before
        a spacewalk*). Below both, a small timeline, red dots for the two fires, navy for Leonov, gold for
        Apollo-Soyuz: 1961 Bondarenko's chamber fire, 1965 Leonov's spacewalk, 1967 Apollo 1, 1975 Apollo-Soyuz's airlock.
        - Points: **Light, but it burns** (in pure oxygen almost anything burns fiercely; on 27 January 1967 a test on
          the pad in pure oxygen at a little above outside pressure killed Grissom, White and Chaffee in Apollo 1;
          afterwards Apollo launched on 60 % oxygen and 40 % nitrogen, changed to pure oxygen in orbit, and lost most
          of what could burn); **Heavy, but it's air** (the Soviet cabins held a full atmosphere of air: no special fire
          danger and no prebreathing before launch, but a stronger, heavier hull; a spacewalk meant breathing pure
          oxygen first, then a suit at less than half an atmosphere: Leonov, March 1965, had to let his suit down
          further still to bend it back into the airlock); **Meeting in the middle** (in July 1975 Apollo and Soyuz
          docked with a module between them as an airlock, and Soyuz lowered its cabin to about two-thirds of an
          atmosphere with more oxygen, so the crews could cross without long prebreathing).
        - In Launch Window: "Spacewalk steps: an American crew opens the hatch from its own cabin; a Soviet one goes
          out through an airlock or the orbital module."
      - The mockup's changes from the brief (the drawing details above, the points joined into sentences) are all
        **unconfirmed**. Labels over hatching carry the halo outline.
      - **Concept chip** (built, unconfirmed): "Cabin air" → E-31 sheet 1 on the first crewed program of each line
        (`vostok`, `mercury`, `voskhod`, `gemini`, `apollo-earth-orbit`, `soyuz-7k-ok`), as Orbit and Retrofire sit on
        the early flights. On the capsules it would have put Apollo, Gemini EOR and Circumlunar Gemini at
        seven chips, over `ConceptsTest`'s six.
    - **Locked until its theory is settled** (user, 2026-10-27): an explainer that answers a
      theory of the day (§4.18.2) names it in `lockedUntil` and stays locked until a flight in the game settles
      every one of them (its science page is printed), the race is decided, or the game's pack can't settle it
      (`Explainers.locked`, `LaunchWindowGame.explainerLocked`). E-06 waits for "Can people eat and work
      weightless?", E-12 for the radiation belts, E-25 for the Moon's dust and Venus. A locked explainer has no
      concept chip anywhere (screens, R&D decisions, help), and the Museum shows a closed card: its code, "Locked",
      the title greyed and "It would answer a question the space age argued over. It opens once a flight in this
      game settles that question.", with no drawing, summary or button. Before the pages are fetched it counts as
      locked. `-Dshots=explainers` renders the encyclopedia locked and settled.
19. **Concept chips** *(user's idea, 2026-10-07; the look confirmed by the user that day)*. A small
    **Concepts** section listing the real-world ideas behind a piece of game content, as clickable
    chips. A chip opens the explainer (component 18) that explains it, at the right sheet; Close
    and Done come back to the screen the chip was on (rebuilt, so a scrolled screen opens at the
    top again). It's how players reach explainers from play, instead of a notice the first time
    an idea comes up.
    - **A concept** is an id, a chip label ("Suborbital", "Retrofire") and a target: an
      explainer and a sheet. Several concepts can share an explainer ("Suborbital" and "Orbit"
      both open E-01 sheet 1). Each concept lists the programs, configurations, steps,
      components and goals it belongs to. The audit that assigns them is in `design.md` §1.1.
    - **A concept without an explainer yet** is listed in the data but shows no chip: no dead
      ends in the game, and the list doubles as the backlog of explainers to write.
    - **Where chips appear:**
      - **Program details** (§4.6.4): the program's own concepts and those of the components it
        grants, each once (a capsule has no screen of its own), after "Components it grants".
      - **Mission configuration details and steps overview** (§4.6.6): the configuration's
        concepts plus those of its steps, each once, inside the summary card in the left column
        under the key facts. Not on the planning hub (§4.6.7), a tabbed screen with no summary.
      - **Open rocket program** (§4.5.1): an always-shown Concepts line under each rocket's row,
        aligned with the name column (six chips don't fit on the row).
      - Not on the step rows themselves (too dense), nor on the mission selection list.
      - Inline links inside text are out (user, 2026-10-07: a libGDX label can't make one word
        clickable, and a word-by-word paragraph widget isn't worth it).
    - **Order:** the order of the concepts list, so related chips sit together. A long row wraps;
      no "more" overflow. **At most six chips per item** *(confirmed by the user, 2026-10-09)*: the
      basics every later orbital flight shares (Orbit, Retrofire, Tracking) sit on the early
      flights where they are new, listed by configuration, not on the shared steps. Engine
      restarts is attached to the Vostok configurations and R-7 (Vostok) by name, not to the
      Retrofire step, so it stays off Mercury, whose retropack was solid rockets. Other trims
      for the cap: no engine restarts or self-lighting chip on the Agena, no land or sea on
      Apollo's recovery step, no launch latitude on the Saturn V.
    - **Look:** 32 px high, white, a 1 px navy rule; the set square (icon `concept`) drawn at 18 px
      (16 turned the 48 px icon's thin strokes to grey mush) in its own 30 px cell with a thin
      navy-border divider, like a title-block cell; the label in Archivo Bold 13 navy. Hover
      thickens the rule to 1.5 px and tints the glyph cell; pressed fills navy; the whole chip is
      the click target. **No hover tooltip,** so it never reads as rules help. No keyboard focus
      ring (the client has no focus traversal). The section header is "Concepts" in the card's
      own section-title style (caption, upper case, muted), beside Required goals and Components;
      a wide card adds the hint "How the real thing works: each opens an explainer sheet", the
      narrow configuration column "N not read yet". Meant to go into the design system as
      `ConceptChip`.
    - **New dot:** a chip whose explainer the player has never opened carries an 8 px gold dot
      with a 2 px white ring on its top-right corner; its Museum Encyclopedia card a 10 px one
      (the grid sits 8 px lower to leave room). Opening any sheet clears it everywhere. Read marks
      are the client's preferences per server and login (`LaunchWindowGame.explainerRead`/
      `markExplainerRead`).
    - **Data:** `assets/content/concepts.json` (`client.view.Concepts`), outside the frozen pack
      like `explainers.json`, so a new concept never changes the pack's hash. Step ids are
      the base ids, without the season number or the `spc-a-`/`spc-b-` prefix of a joint flight.
      `ConceptsTest` (against the standard pack) checks every id, target explainer and sheet, and
      that every label uses only letters the fonts can draw.
    - *As built:* `client.ui.ConceptChip`, laid out by `ChromeScreen.conceptsSection`/`conceptChips`
      (a wrapping `HorizontalGroup`). Canvas: `Launch Window — Explainers`, bottom row.
20. **Trait chip** *(for `design.md` §2.2(d), 2026-10-07)*. One of a
    person's revealed traits, such as "Workaholic". Shown on personnel rows (component 11), the
    person page (§4.14) and in staffing pickers. `client.ui.TraitChip`: a 22 px tag filled with
    `skill-track`, Work Sans Medium 12 in `ink`, no border, square corners, never wrapping; one
    fill for every trait, since most cut both ways. Team traits (they act on others) carry a
    three-dot mark (`trait_team` icon) before the label; personal traits none. Quieter than a
    concept chip and not clickable (no hover look), so the two never get confused. The tooltip
    names the trait, "Personal trait" or "Team trait · acts on the whole team", and what it does
    in plain words ("More R&D progress. Loses morale fast when idle.", `TraitTexts.effect`, from
    the rules' constants). The look is **unconfirmed** (People canvas).
    - **Not known yet:** never show empty slots or a count (that would leak how many traits
      someone has). While someone is in the reveal window, "Still getting to know each other" (no
      pronoun) sits under any chips; after it, "Nothing more to discover" on the person page only
      (rows show nothing). Both in `TextStyle.CAPTION_ITALIC` (SemiBold Italic 11: the client
      ships no Medium Italic).
21. **Chemistry indicator** *(2026-10-07)*. How well a team works
    together: one component's R&D engineers, or one mission's crew and controllers. Five levels
    from the chemistry value (−1..+1): **Poor** (below −0.5), **Strained** (−0.5 to −0.15),
    **Neutral**, **Good** (0.15 to 0.5), **Excellent** (above 0.5), with the effect on output as
    a percentage (levels and percentages from `client.view.People`, the band from
    `Chemistry.OUTPUT_BAND`).
    - **Where:** each R&D board card (component 10), the assign dialog's preview, and the assemble
      mission screen (§4.6.8), next to the staffing. Not on the mission plan's assessment tab
      (§4.6.7); its odds already count the known chemistry.
    - **Look** (`client.ui.ChemistryIndicator`, **unconfirmed**): a five-step scale (12×8 px steps,
      2 px apart; 6×6 compact) with only the current step filled, then the level's word in Work
      Sans 600 and "+4 % output" in `ink-muted`. Unlike traits, colour codes good and bad: Poor
      `danger`, Strained `gold` (word in `gold-text`), Neutral `navy`, Good and Excellent `success`.
      Forms: `full`, `withBreakdown` (hover previews, a click pins, via `client.ui.Popover`),
      `compact` (rows and pickers, no "output"), `preview` ("Good → Strained −5 %"), and
      `breakdown` for the inline "Why" list.
    - **Breakdown:** a white card with a navy rule, one line per known reason with its glyph
      (relationship marker, or the team-trait dots) and only a direction (+ or −, in `success` or
      `danger`), never an amount: "Ivanova and Petrov work well together +", "Abrasive: Reyes",
      "Loner, working alone +", "Team player: Okafor +".
    - **Unknown factors:** the client only knows revealed traits and shown relationships, so
      "may differ: unknown factors" sits under it in italic caption while some of the team (or the
      candidate) is still in the reveal window; the breakdown then ends "May differ: unknown
      factors. Some of this team are still new to each other, so the real chemistry can be better
      or worse." It never hints at what is hidden.
22. **Relationship marker** *(2026-10-07)*. A shown relationship
    between two people: "works well with" or "clashes with". Shown on the person page as a list,
    and in staffing pickers and mission tiles next to a candidate who has one with someone already
    in the team, so the player can build teams around friendships or keep enemies apart.
    `client.ui.RelationshipMarker`: linked rings in green (`rel_good`) and a spark in red
    (`rel_clash`), since a relationship, unlike a trait, is plainly good or bad (look
    **unconfirmed**). `kind` for the person page's list: a row each, 40 px portrait, the kind in
    label caps in its colour, the name (opens their page), and on the right what they do now and
    "7 seasons together". `mark` for pickers: the glyph and the partner's surname, the tooltip
    naming them in full ("Works well with Gunnar Lindqvist · Already on this team"). Both glyphs
    and `trait_team` are on the UI Asset Sheet.

**People screens** (components 20 to 22, §4.14, and the people parts of components 10, 11, 14,
§4.4 and §4.6.8) were mocked up on the canvas `Launch Window — People` (2026-10-07), each row's
choices **unconfirmed** in its yellow note.

23. **Help** *(the user's idea, 2026-10-08; built without a mockup, the layout being plain; the
    user confirmed every point below)*. The game's rules and mechanics by
    topic, where the explainers (component 18) tell how the real thing works.
    - **Opening:** a `help` icon button (a question mark in a circle) after the settings gear in
      every header, and after the gear at the end of the hub strip; **F1** anywhere in the game. It
      opens **over the current screen** (a panel of 1560 by 920 on the navy scrim), not as a new
      screen, so closing it (the close button or **Esc**) leaves the player exactly where they
      were, mid-assembly or with a dialog still open underneath. It has its own layer, so it never
      closes a dialog and a dialog never closes it.
    - **Layout:** "Help · Rules and mechanics" and the close button on top; a search field (a
      magnifier inside it) with **Show all topics** beside it; a status line; then the topic list on
      the left (340 px, grouped under small captions) and the selected topic's text on the right
      (lines at most 860 px wide), each column scrolling on its own.
    - **Context:** each screen names the topics that matter there (`Help.Context`, overridden in
      the screen's `helpContext()`), and help opens filtered to them, on the first, with a navy chip
      in the status line: "3 topics for the SET Centre". A screen without a context opens on every
      topic. The newspaper's pages open on "The Daily Zenith" (`Help.Context.NEWSPAPER`), the
      archive's pages and the explainers on the Museum's, the Hands-on screen on "Hands-on
      campaigns" (`Help.Context.HANDS_ON`, user, 2026-10-29), to which R&D decisions' Hands-on
      section points. Help also opens on an ad-hoc list (`Help.Topics`), as the advisor's items do;
      the chip names it ("1 topic for "Open a rocket program""). `HelpTest` checks that every
      context names real topics and that every topic is on some screen's list (the basics on the
      hub's).
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
      rename or reuse a topic id), 34 topics in six groups: Basics, Agency, Programs and hardware,
      Missions, People, Rivals. Numbers are tokens such as `{poaching.minTenure}`, filled from
      the rules' constants, the game's budget schedule and the pack's buildings
      (`client.view.HelpValues`), never typed into the text. `HelpTest` checks the ids, tokens,
      links, every screen's topic list and the letters.
    - *As built:* `client.screen.HelpOverlay`, `client.view.Help` and `HelpValues`; the `help` and
      `search` glyphs in `make_ui_icons.py`, on the UI Asset Sheet. Shots `help-*.png`.
24. **Advisor** *(the user's idea, 2026-10-08; built without a mockup, the user having
    confirmed the plan)*. A new agency's first things to do, then the road ahead as the programs open in
    stages, on a card at the hub's bottom left, until they're done.
    - **The items, in order** (`client.view.Advisor.ITEMS`): open the help; hire engineers; open a
      program (a satellite is the cheapest start); open a rocket program; put engineers on R&D;
      hire flight controllers (once a program is open: every mission, uncrewed too, needs
      consoles); hire astronauts (only once a program with a crew or a CAPCOM is open); raise
      reliability; plan the first mission; read the first turn report (only once a turn has
      resolved).
    - **The road ahead** (the user's idea, 2026-10-26, with the guided progression, `design.md` §2.3(d);
      `Advisor.ROAD_AHEAD`), after the first steps:
      "Start on a person in space" (the one-crew capsule, named: Mercury or Vostok); "Fly the X-15
      meanwhile" (or the E-154: cheap and quick while the capsule is in R&D, and its pilots earn
      rocket-plane wings); "Choose your road to the Moon" (once a person has been to space: the bigger
      capsules and the Moon craft's Earth-orbit tests); "Send a probe to the Moon" (with the roads, once a
      person has been to space: "fly one while your crews work toward orbit"; user, 2026-10-27: earlier it
      read as a third program to run beside the capsule and the rocket plane; a pack without bigger
      capsules keeps the old timing); "Go to the Moon" (once a probe has reached it and a person has orbited the Earth). Each comes when
      its programs can be opened (the first two also wait for a first flight, as in the ungated early-era
      pack), goes once one of them is open or has flown a mission (a program closed after flying, as the close
      tip suggests, mustn't bring its step back: play test, 2026-11-11), or the player has moved past it (the
      rocket plane once a pilot has wings or a bigger capsule is begun), and is "not needed" for an agency without
      such programs.
      Waiting on one, the card says "All set for now" and when it comes ("Once your first satellite
      reaches orbit: start on a person in space."). Once the first steps are done the head reads "The
      road ahead". `Advisor.title`/`text` put the agency's own program names in. "Choose your road to the Moon"'s Go link
      ("Compare the roads") opens Roads to the Moon (§4.21).
    - **Rocket-plane wings on other screens** (`design.md` §2.2(c)): the
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
      there's no button to end a turn. **No hide link** (user, 2026-10-26: a new player could click
      it by accident and lose the help that comes later; minimizing is the way to quiet it). With
      nothing open but something still to come, it says "All set for now" and names it. 380 px
      wide, touchable as a whole so the hotspots underneath never take a click; at the bottom left
      it covers no building marker on either agency's art (only the corner of NASA's SET Centre
      outline).
    - **Minimize** (the user's idea, 2026-10-10): a white chevron at the end of the head, pointing
      down. A click anywhere on the head shrinks the card to the head alone, which drops to the
      bottom left, with the chevron pointing up and the caption counting what's left ("4 things to
      do", "1 thing to do", or "All set for now") instead of "Your first seasons". Another click
      opens it again. *Unconfirmed default:* the state lasts for the client run
      (`LaunchWindowGame.advisorMinimized`), across hub visits, and a fresh start shows the whole
      card again, so the advisor is never lost by accident. Shot `advisor-minimized.png`.
    - **Gone for good:** once every item is done (the report and the road ahead included: a Moon program
      open), the client marks the advisor retired and never shows it again for that login on that
      server. Not shown once the game is decided. A login retired by the old hide link stays retired. A dev
      data reset deletes the client's preferences, so the advisor comes back.
    - *As built:* `client.view.Advisor` (`AdvisorTest`), the card in `SpaceComplexScreen`, the marks
      in `LaunchWindowGame`. Shots `advisor-*.png` (`advisor-reliability.png` and `advisor-ready.png`
      for the bar); every other hub shot shows the card too.
    - **The idle tip** (the user's idea, 2026-10-12): "Keep idle people busy", with the morale
      numbers from the rules (idle loses 2 a season, resigns at 35) and that training is free. A
      one-off apart from the first steps (`Advisor.IDLE_TIP`, never in `ITEMS`, never counted
      toward retiring): due once the agency is set up (someone on R&D or a mission flown) and
      someone out of basic training is idle and available and nobody in advanced or permanent
      training yet (`Advisor.idleTipDue`; the timing confirmed by the user, 2026-10-12). It goes
      first on the card; its Go link ("Their building") opens the building of the first group with
      someone idle, engineers first, and a **Got it** link puts it away. On a retired advisor the
      card comes back for it alone, captioned "A tip", without the footer. It goes for good once it
      stops being due after it was shown (someone sent to training, or nobody idle), or on Got it
      (client marks `idleTipShown`, `idleTipDone`). Shots `advisor-idle-tip.png` (retired) and
      `advisor-idle-tip-first-steps.png`.
    - **The explainer tip** (the user's idea, 2026-11-03): "Read while you wait", so new players
      learn the explainers exist (component 18). It fills the wait while the first engineers are in
      basic training, under the card's "All set for now". Its Go link ("Read it") opens E-01
      "Suborbital vs orbital" (`Advisor.TIP_EXPLAINER`), coming back to the hub; its text names the
      Concepts chips and the Museum; Rules opens the Museum topic; **Got it** puts it away. A
      one-off like the idle tip (`Advisor.EXPLAINER_TIP`): due once an engineer is hired
      (`Advisor.explainerTipDue`), it stays until any explainer is read, from anywhere, or Got it
      (client mark `explainerTipDone`). A player who leaves the wait without it finds it after the
      first steps on the card. Shots `advisor-explainer-tip*.png` (`-Dshots=advisor`).
    - **The close tip** (the user's idea, 2026-11-11): "Close
      {program}?", the first time an open program has flown every one of its configurations and has no contract
      line (`commercial.json`), nothing in flight (Close is refused then), and no unopened program whose opening
      cost its closing would raise (`ProgramOpening.discountsLostByClosing`: an Earth-orbit test program often makes
      its Moon program free while open). Its text says the components are kept, reopening costs only the opening
      cost, and a repeat flight still pays 30 % of its prestige; its progress line the upkeep saved a season
      (`ProgramOpening.upkeepSaved`: the program's own, plus components no other open program needs). Go ("The
      program") opens the program's details, where Close is; Rules opens Programs and Budget; **Got it** puts it
      away. A one-off with the idle tip's lifecycle (`Advisor.CLOSE_TIP`, `closeTipDue`, client marks
      `closeTipShown` / `closeTipDone`): done once it stops qualifying after showing (closed, say), and like the idle
      tip it brings back a retired card. Shots `advisor-close-tip*.png` (`-Dshots=closetip`).
25. **Looking ahead** *(the user's idea, 2026-10-10: "thinking ahead gives you a big advantage in this
    game"; built without a mockup, since each part extends a component that exists).* Every season
    count comes from the rules, never the client: `ResearchPlanning.project` and `ranges` for R&D,
    `PersonnelLifecycle.seasonsUntilRetirement`, the training seasons left, and
    `BudgetProgress.runway`; `client.view.Outlook` words them (`OutlookTest`). "By <season>" means
    the open turn plus that many seasons: the season you have it in.
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
      season" reads the projection's first season; `expectedSeasonGain` itself is unchanged,
      since `PressEffects` uses it on the server.
    - **In the turn report:** the R&D section's component bars get the same ticks, counts and
      tooltip (`RndCards.milestones`), but only while the bar shows today's reliability, so an older
      report never marks a bar the component has moved on from.
    - **Idle people on the hub** (§4.3): a small red dot with a white
      ring on the upper right of the Astronaut Complex's, Mission Control's or the SET Centre's
      marker while any of its people are idle (free for work, as assembly counts them:
      `Hub.idle`). Hovering the building adds a line under its name: "3 engineers idle", "1
      flight controller idle", "2 cosmonauts idle" (astronauts for NASA). Shot
      `outlook-hub-idle.png`.
    - **Rocket requirements.** A mission card says what its rockets must
      lift and which one does it: "Rocket: 4,700 kg to low Earth orbit, human-rated: R-7 (Vostok) can
      fly it." in green, or "...: needs at least Atlas (not open yet)." in red ("needs a bigger VAB for
      medium rockets" when the class isn't built yet; launches alike are said once, "(both launches)").
      A program card adds "Rockets Redstone to Atlas": the smallest rocket its easiest and hardest
      missions need, green when open rockets fly every mission, amber some, red none, with each
      mission's line in the tooltip. *Unconfirmed default:* "the smallest rocket" is the agency's
      rocket that can fly it in the lowest size class, then the cheapest launch (`Outlook.leastRocket`),
      which follows the rocket progression closely where the pack's own order doesn't. The readiness
      row's red Rocket check names it too.
    - **Ambitions at stake when assembling.** A warning on the assembly
      screen names anyone free whose stated ambition this team would pass over ("Fyodor Gusev is free and
      wants to be on the consoles for a crewed orbital flight. If the flight goes well without them,
      that's a pass-over: -5 morale, -5 loyalty, and less patience."), and the slot picker marks a
      candidate this seat would give their ambition ("Wants this: ..."). `Careers.ambitionsAtStake`
      follows the season pass: crew ambitions by the crew, console ambitions by the ground team, CAPCOM
      aboard for "Be part of a first"; people busy elsewhere aren't counted; the two "first" ambitions
      only while the mission has a goal the agency lacks, worded "if this flight reaches a goal first".
      Shots `assemble-passed-over.png`, `assemble-wants-this.png`.
    - **Random-event stories** (`design.md` §2.7(d)): the turn
      report's news shows each event's story headline, its sentences, then the event's own line in
      green or red ("R&D results are 16 % higher for a while.", the struck person named). The Public
      Affairs Office's narrow news log shows the headline and the event's line, the story in a
      tooltip. An event without a story (from before) reads as before. Shot `report-news.png`.
    - **Retirement before the cap.** A gold line under the caption: "Arkhipov retires in 7
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
      satellite) is charged every season…" (user, 2026-10-23: naming the program's alone read as a
      contradiction). Shot: `hq-details-sputnik-2-open.png` (an inactive satellite's upkeep coming
      back).
    - **Where to earn a goal** (§4.6.3, §4.6.4). On a locked program's details, a line per
      missing goal: "Satellite in orbit: Earned by Project Mercury (open), Apollo (open), Explorer 1
      (available) and 8 more programs." Open programs first, then available; the agency's own
      programs only; a goal counts when a step achieves it or credits it through a cascade. When only
      locked programs earn it: "Only locked programs earn it: X needs Y first." The card's "Needs:"
      line has the same text in its tooltip.
    - **Help:** the R&D topic has a "Looking ahead" part (tokens `outlook.milestones`, `outlook.ready`).
    - Shots: `outlook-*.png`, `payload-rnd*.png`, `plan-configs.png`, `pao.png` and
      `hq-details-vostok-open.png` (the confirmation's runway).
26. **R&D decision** *(the user's idea, 2026-10-10: `design.md` §5, Backlog, "R&D decisions"; canvas
    `Launch Window — R&D decisions`, the look confirmed by the user).* A choice the game puts to the
    player about one component under R&D, rocket or payload, to fill the lulls between flights. Every
    decision has the same shape whatever its type: a story, two to four options, what each option
    shows in advance, a default, and an outcome the server works out the moment the player decides.

    **Life cycle.**
    - *Offered* when a turn resolves (aimed at the lulls; rules below). One open decision at a time.
    - *Open* until the next turn resolves. The player decides whenever they like during the turn.
    - *Decided:* an ordinary action (`DecideAction`), resolved by the server at once from a seed fixed
      to the decision (reopening can't reroll, and the action journal replays it). The answer comes
      back with the new state: the outcome shows in place and the bar and season marks update.
    - *Lapsed:* not decided when the turn resolves, so the default applies then, and the next report
      says so. After a lapse no new decision comes until the player acts again (`decisionPaused`).
    - *Recorded:* the next turn report's news carries one line for it, decided or lapsed (neutral
      when nothing changed).

    **Where the player meets it.**
    - **Turn report** (latest report only): a full-width Decision card above the columns, gold left
      accent: eyebrow "DECISION · <COMPONENT>", the headline, the story's first sentence, "If you don't
      decide: <the default>." and a primary button "Decide on the R&D board" with the countdown under
      it. A seventh glance cell, "Decision · 1 waiting" with the component (the Events cell becomes
      "Funding review" in a review season, so it can't carry it).
    - **Hub:** a gold dot (6 art px, 1.5 px white ring, a copy of the idle dot) on the upper left of
      the VAB's (rocket) or SET Centre's (payload) marker, opposite the red idle-staff dot. Hover adds a
      white line after the idle line, behind a small gold dot (gold text read poorly on the bright
      art): "A decision waiting: Atlas". The same dot shows while a component of its board is below
      its R&D cap with no engineers (the user's idea, 2026-10-31), hover "Atlas has no engineers" or
      "3 rockets have no engineers" (`PayloadRnd.unstaffed`).
    - **R&D board (component 10), where the player decides:** a banner inside the component's card
      (`DecisionBanner` in `RndCards`), between the reliability bar's caption and the team chemistry
      row; the board puts that card first. The mission plan's read-only R&D tab shows the banner
      without its buttons, with a link to the board.

    **The banner, open.** Gold-tinted background, gold border.
    - Eyebrow row: "DECISION · <TYPE LABEL>", a help mark for the "Decisions" topic, and,
      right-aligned, "Until the turn resolves · <countdown>". A follow-up's eyebrow is "FOLLOW-UP ·
      ATLAS" with no type label, and a recall row shows the first outcome and "Step 2 of up to 3".
    - Headline (heading small) and the story (body small, one to three sentences).
    - Type-specific panel where the type has one (clues, a person picker; see the types), and a
      muted stakes line where all options share the stakes.
    - The options as option rows (white, full width, a chevron; hover: a navy border and a navy bar on
      the left): a label, and under it what it shows in advance in caption text ("$ 1,200 · 70 %
      chance of about +4 reliability"), or nothing. The default carries an outlined "If you don't
      decide" tag before the chevron.
    - "How it works:" with concept chips (component 19) for the explainers the decision draws on.
    - Clicking an option opens the confirmation.

    **The confirmation.** A small dialog: the option's label, the chances as rows, its cost with
    funds before and after, any person it takes, "Decided at once", and "Decide" (primary) and "Back".
    The default option can be chosen too; it then applies at once instead of waiting.

    **The banner, decided.** "DECIDED · <option>", a tone mark (green, red, or navy for neutral),
    the outcome's headline and sentence, and the effects in the event lines' style ("Reliability +4.2",
    "Funds - $ 1,200", "Kozlov: rapport with Orlova grew"), and "Stays here until the turn resolves.".
    The bar shows a green gained (or red hatched lost) segment until then. A story arc's outcome adds
    a navy "To be continued" tag. A close mark folds it to one line with a Show link (client-only,
    forgotten on restart).

    **Effects are sized in seasons of R&D, not raw points** (*unconfirmed default*): a boost or a setback
    is the component's expected gain over a number of seasons (`ResearchPlanning.expectedSeasonGain`,
    at least 1 point a season), so a decision weighs the same on an early satellite and on Saturn V.
    The gain is read when the decision is offered and kept on it (`PendingDecision.seasonGain`, the
    user's play-test find, 2026-11-11), so moving engineers before deciding changes neither the preview
    nor the outcome. Costs are a share of a season's funding.

    **The types**, each with an example. The type is a field in the content file; one resolver per
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
       out / Back the foreman (default) / Promise a bonus ($ 300). Outcomes range from a morale lift
       and a small boost to a resignation threat.
    4. **Who oversees it.** The people are the options: the idle engineers, best first in the
       component's skill, each with the chance their skill gives ("Kozlov · Rockets 72 · 78 % clean
       run"); engineers in advanced or expert training listed muted with "Pull out of training to
       choose" (a link that opens the pull-out confirmation, partial credit applies). The default,
       "Nobody, the test is put off", stays an option row below. *"Who will oversee the test firing?"*
    5. **Trade-offs over time.** Options that change R&D for several seasons, through an R&D factor
       on the component (`OwnedMissionComponent.rdFactor`, read by the season pass and the projection)
       and a reliability change now; a lower cap is deferred. A gold line under each option previews
       it ("With this: 60 % in ~3 seasons (now ~2) · the cap in ~25 (now ~32)"). *"Freeze the
       design?"* Freeze: about a season's gain lost now, then R&D 30 % faster for four seasons. Keep
       refining (default): nothing changes.
    6. **Diagnosis.** A clue list above the options: two to four short clues, each a line with an
       icon (`clue_trace`, `clue_note`, `clue_inspect`) and a small kind label. Options are the
       suspected causes; one is right. Right: a fair boost; wrong: a season of R&D lost chasing it;
       "Wait for more data" (default): nothing. *"Shutdown at second 41"*: pressure fell two seconds
       before cut-off; turbopump vibration normal; the igniter fired twice.
    7. **Opportunity with strings.** An offer for this turn only (a gold "This turn only" tag), its
       catch in bold. *"The air force lends its altitude chamber"*: Accept: about three seasons' gain
       at once, one engineer from the team seconded for two seasons; "If you accept, who goes?" with
       each person's R&D lost a season, the picker in the confirmation. Decline (default).
    8. **Pairing.** Two person pickers ("Who works with whom?") in the confirmation (900 px wide): the
       first holds a team member, the second a dropdown of the team, then idle engineers, each with
       its relationship to the first (component 22). Outcome scaled by their rapport and traits; the
       pairing builds rapport or starts a clash, shown in the outcome. Default: "Leave the team as it is".
    9. **Intelligence tip.** Only while the Intelligence Office has a level. Shows the tip's
       reliability in words from that level ("Our sources are usually right about this"). *"The
       rival's engine runs cooler"*: Adopt their approach: a large boost if the tip is right, a setback
       if not. Ignore it (default).
    10. **Story arc.** Any type's outcome can open a follow-up in a later quiet season. Its story
        recalls the first decision ("The test firing in Spring went clean. The engineers want to try
        it at full thrust."). Higher stakes both ways. An arc ends after two or three steps, or when a
        step goes badly.

    11. **Hands-on** *(the user's idea, 2026-10-11, `design.md` §5, Backlog; canvas `Launch Window —
        Hands-on`, its choices all **unconfirmed**)*. `screen.HandsOnScreen` with `view.HandsOnView`;
        the rules in `RdDecisions.handsOn`, played step by step against the server (`HandsOnAction`).
        The banner's options are **Run it yourself** and **Leave it to the team** (the default,
        nothing happens); its eyebrow reads "DECISION · HANDS-ON". Under the story, in a white panel,
        one caption line: "Up to 4 rounds of 3 commands · objective 3 firings, 4 measurements · bonus
        6 and 7", and the odds the team gives ("Kozlov leads: 17 % of steps fail, 15 % go exceptionally
        well"). "Run it yourself" says what the objective and bonus are worth, and asks once ("Once
        started, the result counts, finished or not", "never a loss") before opening the
        **Hands-on screen**:
        - **Header:** breadcrumb "... › R&D › <component> › Hands-on", eyebrow "HANDS-ON · <COMPONENT>",
          the title (the decision's headline), and right of it a "Round 2 of 4" chip.
        - **Objective bar** across the top, in a card: the objective as chips (checkbox, the resource's
          icon, held amount › target, "2 › 3", and a "+2 on execution" line for queued outputs), the
          bonus as a second row ("Bonus: about 3 seasons' R&D"), and the other resources' counters
          (shifts, spare parts) showing held amounts after queued inputs ("6 at the start · 2 paid").
        - **Command table** on the left: rows grouped by the resource each group makes, a group tile at
          each group's left (the resource's icon on its colour), columns Command, In, ›, Out (headed plain
          "OUT", with a tooltip); amounts as the resource's icon with a number above 1; the output the
          roll moves underlined in its colour. A row the player can't pay for is muted with a red left
          edge, says what it is short of ("Short of 1 measurement") and doesn't click; a row they can
          queue takes a light navy tint on hover (user, 2026-10-25: the rows didn't read as buttons); a
          click queues it. Overtime sits last, alone.
        - **Queue panel** on the right: "Commands 1 / 3" with Undo and Reset, three numbered slots (a
          queued command's name and group tile), the odds line with a three-segment bar, and the footer
          with the commit-coloured **Confirm commands** button (the screen's one big action). Confirming
          with empty slots is allowed. During execution the table greys and Undo and Reset grey out.
        - **Execution, slot by slot** (user, 2026-10-25): the running slot has a navy border and a
          "Running…" badge; a needle over the odds bar (a 24 px strip) rises from the left to a peak,
          then rings down onto the slot's roll with fading noise, overshooting into the neighbouring
          segments (a 0.9 s rise, then a 0.75 Hz swing damped over 3.1 s), and settles in the roll's
          colour (danger, navy, success) with a light outline. The badge then shows (Failed −1, Done,
          Exceptional +1; "Done · no roll" for Overtime, 0.3 s with no needle), the objective and
          counters count that slot in, and 0.9 s later the next slot runs. The foot reads "Round 2
          running" with **Skip · Space**, which shows the rest at once; any step taken skips it too.
          The rules have already rolled and the client never sees the drawn number, only the roll, so
          where in its segment the needle settles is made up from the round and the slot.
        - **A fail with shifts left** asks once its needle has settled: the slot says what it makes
          instead and holds **Accept** and **Call in an expert (1 shift)**; execution waits. With no
          shifts the fail just applies. After a round its results stay up with "Round 1 done" and
          **Plan round 2** (no stop button: leaving is the back button).
        - **Result panel** when the rounds run out or the bonus is met: a 4 px top accent (gold,
          success, danger), "Bonus success", "Success" or "Not enough", a table of held against
          objective and bonus, the effect in seasons of R&D with its reliability, and "Back to the R&D
          board", where the banner shows "Decided · Run it yourself" with the outcome.
        - **Sound** (the user's Pixabay finds, 2026-10-25): a tense loop under the round from Confirm
          (music volume), fading out over 1.2 s after the last slot or on Skip, and a sting as each
          needle settles, failure, success (a normal result) or exceptional (effects volume); Overtime
          has none. Files in `assets/audio/handson/`, published by
          `assets-raw/audio/make_handson_sounds.py`. Unconfirmed: the timings, the motion and the levels.
        - **Colours and icons:** each resource gets a colour from the existing tokens (shifts gold,
          firings danger-red, measurements navy, spare parts success-green; small icons on white use
          gold-text for shifts), no new tokens; icons `res_shift` (a hard hat), `res_firing` (a nozzle
          and plume, also the other levels' test resource), `res_measurement` (a gauge),
          `res_spare_part` (a hex nut), in `make_ui_icons.py` and on the UI Asset Sheet.
        - **Leaving mid-puzzle** is allowed: the state is the server's, the banner shows a gold row with
          the round, the objective so far and Continue ("Hands-on in progress · round 2 of 4"), the
          report's Decision card "Continue the campaign", the hub hover "Hands-on in progress: R-7
          (Vostok)". An unfinished puzzle settles as it stands when the turn resolves.
        - **Levels:** each campaign keeps the first level's shape (shifts pay, Overtime gives one, an
          expert costs one, 4 rounds of 3, which help quotes) but differs in what's scarce: cold-flow
          runs (measurements scarce), igniter tests (spare igniters both spent and part of the
          objective), the shake table (start with parts), the sun lamps (a three-part objective), all
          the systems at once (start with readings). A replanning bot at 20 % fails reaches the bonus
          37 to 72 % of the time (the first two levels: 68 %). Shots `handson-*.png` (`-Dshots=handson`).
        - **Playing a level on its own** (the user's idea, 2026-10-25): dev-tools' `HandsOnPlayground`
          (`start_handson.cmd`, or `mvn -pl dev-tools compile exec:exec@handson`) plays any hands-on level in
          this screen without a server: a list of the levels, then the component it runs on (both for
          one that fits any), the lead's skill (0 to 100 in steps of 20, the fail chance shown) and a
          seed. Steps are applied by the server's rules, so the rolls are the game's. The levels are read
          from `assets/content/standard/decisions.json` before every run, so an edited level plays
          without a restart; on the puzzle, R plays the same seed again, N a new one, Esc goes back to
          the list. One engineer on the team, so never the good-chemistry odds. `-Dhandson.smoke=<folder>`
          (run with `java` from `assets/`) plays a winning random run through the screen and saves a
          series of frames; silent, though the sounds load and play.

    **Help:** a "Decisions" topic, linked from the banner's eyebrow and on the R&D boards.

    **Rules** (`rules.RdDecisions`, `RdDecisionsTest`): a `DECISIONS` phase after Careers settles last
    turn's decision (the default if still open) and may offer one. A lull is nothing in flight and
    either a component with engineers more than a season from 60 % or `WAITING_SEASONS` (2) seasons and
    more since the last flight; then the chance is `LULL_CHANCE` (0.85), else 0.05 plus 0.02 a quiet
    season up to 0.35, halved while a mission flies or right after a decision. The decision goes on the
    component furthest from 60 %; a story arc's next step waits for a lull. Offer and outcome draw from their own salted seeds, so the playouts without decisions are
    unchanged to the digit; the seed never reaches the client (`Personality.forClient`). `DecideAction`
    refuses `NO_TEAM` when nobody works on the component (effects are sized from the team), and a
    decision whose component goes inactive during the turn is dropped silently. **Unconfirmed
    defaults,** to tune with the playout bots and the play test: the chances (about 85 to 90 % in a
    lull, low otherwise), effect sizes in seasons of R&D, costs as a share of a season's funding.
    - **Odds:** an overseer's chance is 0.25 + 0.007 x skill (0.3 to 0.95); a pair's 0.5 + rapport / 200
      (0.15 to 0.9); an intelligence tip's 0.35 + 0.15 x the office's level (to 0.9); a trait an outcome
      names (`favouredBy`, engineers' traits only) doubles its weight when anyone involved has it,
      revealed or not.
    - **Content:** each pack's `decisions.json` (`assets/content/standard/` and `early-era/`, identical,
      hashed and frozen); ids are kept in saved state and reports, never reused. `DecisionsTest` holds it
      to its rules (at least 4 first steps a type for rockets and as many for payloads). 185 decisions
      (2026-10-25), about 20 first steps a type, fewer where more would repeat themselves (pairing 12,
      hands-on 7); the picker draws evenly among a component's usable first steps. Choices, all
      **unconfirmed**: effects about +3 seasons at best, -1 to -1.5 at worst, costs 0.05 to 0.45 of a
      season's funding; no negative funds effect (funds aren't checked against it); every option that
      takes an overseer is the decision's only one (the banner lists the engineers as rows without the
      option's label); trade-offs may slow R&D ("R&D 20 % slower"). `-Dshots=decisions` renders every
      banner (`pool-<id>.png`) and each campaign's planning screen.
    - **Measured** (`ContentPackPlayouts standard side decisions`, 200 Moon races, 2026-10-25): the Moon
      race averages 121.5 seasons with the pools, 130.3 with none; about 74 decisions a game; hands-on
      campaigns 190 bonus, 65 success, no failure.
    - Shots `decision-*.png`.
27. **Contract marker** *(the user's idea, 2026-10-11, for `design.md` §2.11; canvas `Launch Window — Contracts`)*. Shows where commercial contracts come from: which flights open a contract line, and which
    flight a contract flies. One icon of our own (24 px grid, 2 px round strokes: a page with a folded
    corner, two lines, a signature, a round seal with two ribbons), as `contract_unlock` (outline, navy)
    and `contract_flight` (filled, used in gold; its own SVG, knocking its lines out with a mask), in
    `make_ui_icons.py` and on the UI Asset Sheet. Look **unconfirmed**. `screen.ContractMarker` (cards:
    `ConfigCards`, `BodyProgramsScreen`, `ProgramDetailsScreen`) and `AssembleMissionScreen`.
    - **Unlock flight** (outline icon, right of the configuration's name at 20 px) on every
      configuration card in a program's unlock set, until the line is unlocked; a flown one adds an
      11 px green tick badge, bottom right. Tooltip: "Opens commercial contracts for Earth-orbit
      satellites once every marked flight has succeeded: 1 of 2 flown", listing the set as "Flown ·
      ..." and "To fly · ..." (the font has no tick or circle glyphs).
    - **Contract flight** (filled icon, gold) on the configuration contracts fly, once the line is
      unlocked. Tooltip: "Contracts in Earth-orbit satellites fly this configuration. Open contracts
      are on the board in Headquarters."
    - **Program cards and program details** (§4.6.3, §4.6.4) get a line under the components:
      "Commercial line: Earth-orbit satellites · 1 of 2 marked flights flown" with the outline icon,
      or the gold one with "unlocked" in green, or nothing for a program without an unlock set.
    - **A contract's own mission** wears a gold "Contract · [client]" badge with navy text and icon.
      The first launch of the contract configuration while a contract is active *is* the contract
      flight: Assemble shows the badge right of the title, a gold-tinted fee box in the prestige's
      place under "Instead of prestige" ("Fee 2,340 on success · insurer refunds the launch cost on
      failure", the window under it: "Launch by Autumn 1963"), and the gold line "This launch flies
      your contract for [client]" above the launch row, after the price, with no toggle; the
      confirmation repeats the line. Mission Control's mission lists don't wear the badge yet.
    - The numbers in every tooltip come from the rules' constants (G7), never typed in.
    - Shots `contracts-configs-*.png`, `contracts-program.png`, `contracts-assemble.png`.

28. **Statement dialog** *(the user's idea, 2026-10-20, for §4.18 "The world reacts"; canvas
    `Launch Window — The World Reacts`, 2026-10-22, its choices **unconfirmed**; `screen.StatementDialog`)*. Gives the press one statement about a rival's First, printed the
    next turn in a follow-up page.
    - **Opens from** the strip under a rival's First page (§4.18), from a ghost button
      (`UiKit.pageGhostButton`: transparent, a 1 px `ON_PAGE` line, `ON_PAGE_STRONG` text) left of the
      white page button: "Give a statement" while the player may give one, a check icon and "Your
      statement: Polite · Change" once given (the stance in the player's words). Not shown when the
      player made the First, joined after it was printed, or the turn after it has resolved. A
      reopened First page whose follow-up is printed shows "Read the reactions" there instead.
    - **A reminder on leaving** (user, 2026-10-23, `FrontPageScreen.remindStatement`, shot `reactions-reminder.png`): the
      strip's button is easy to miss under the paper, so leaving such a page unanswered (Continue, Next front page or
      Close, from either sheet) asks first: eyebrow "A rival's First", title "Give a statement?", "You have a unique
      chance to answer <agency>'s first <First>. Every rival's statement is printed together in next season's paper,
      and this season is the only one to give yours." "Give a statement" (primary) opens this dialog over the page;
      "Decline to comment" (secondary) moves on as the strip's button would, nothing sent. Once a page per visit.
    - **720 px wide,** the design system's Dialog over a second scrim (navy at 55 %).
      Title "Give a statement"; subtitle "On the <agency>'s <First> · printed next season". A history
      note after the title: how Eisenhower and Kennedy answered the Soviets' Firsts.
    - **Five stance rows,** real radios in a fieldset ("Your stance"), one choice, in the personnel
      dialogs' choice-row pattern (radio mark, bold label, one muted line); the picked row has a
      1.5 px navy line and a 5 % navy tint:
      1. *Enthusiastic congratulations*: Warm, generous praise.
      2. *Polite congratulations*: Correct, and brief.
      3. *"We'll be next"*: Congratulate them, then promise to catch up.
      4. *Downplay*: It proves less than it seems.
      5. *No comment*: A pointed refusal to say anything. (Staying silent is printed as "did not
         respond".)
    - **Quote,** optional: a 76 px multi-line field (`UiKit.textArea`), at most 140 characters
      (`FrontPage.Statement.MAX_QUOTE_LENGTH`), an "n / 140" counter inside at its foot on the right,
      placeholder "Add a quote, printed word for word (optional)". Typing is filtered to
      `FrontPage.Statement.ALLOWED_CHARACTERS` (letters, digits, everyday punctuation; no braces or
      angle brackets). No comment empties and greys it; picking another stance turns it back on,
      empty. Inline error under it, the line and counter in `DANGER`: "Only letters, numbers and
      common punctuation." / "At most 140 characters."
    - **Preview,** "IT MIGHT READ": a scrap of `paper.jpg`, tilted 0.4°, its label in `NEWS_LABEL_SM`
      with "The Daily Zenith, next season" beside it, one sentence in `NEWS_QUOTE` as the paper might
      print it, from the stance's first line in `front-page-reactions.json` with the player's names and
      quote (the quote left out while it has an error). With nothing picked: "Pick a stance to see how
      it might read."
    - **Footnote** (muted, small): "Every rival's statement is printed together next season. You can
      change or withdraw yours until the turn resolves."
    - **Buttons:** Withdraw at the left end (only when one was given; destructive outline, at once,
      no confirmation), then Cancel and "Submit statement" (primary, disabled until a stance is picked
      and while the quote has an error). Editing shows "✓ Given this season: Polite congratulations"
      in the head (caption, `SUCCESS`). A refusal from the server shows as a toast through
      `client.ui.Refusals`.
    - **Theme:** text styles `NEWS_QUOTE` (Caslon italic 15/21), `NEWS_LABEL_SM` (Old Standard Bold
      13/16) and `NEWS_EAR_TWO` (Old Standard Bold 25/28, §4.18.1). No new colours or icons.
    - Shots `-Dshots=reactions` (`reactions-*.png`).

29. **Eulogy dialog** *(the user's idea, 2026-10-22, for §4.18.3 "In memory"; canvas `Launch Window — In
    Memory`, its choices **unconfirmed**; `StatementDialog.eulogy` and `.condolences`)*. The losing agency's statement on its lost crew, printed the next turn on the memorial page.
    - **Opens from** the card "The press is waiting for your statement" in the report of the turn after the loss
      and on the hub, and from the strip of the report's People line about the loss.
      After a statement is given: "Your statement: Who they were · Change". Gone once the turn resolves.
    - **The frame:** component 28's Dialog at 720 px, quieter: navy and ink only, no gold. Title "A
      statement on the loss of <mission>"; subtitle "Printed in tomorrow's paper, <season>". A history
      note (`HistoryNote`, its card opening downward) follows the title's last word (Kranz, §4.18.3).
      A given or sent status is in `INK_MUTED` with the check icon.
    - **The crew,** at the top: 64 px greyscale portraits with a hairline in a row, the name in
      body-strong 12 and "Aged 38" in caption under each.
    - **Four tone rows,** one choice, in component 28's choice-row pattern (radio mark, bold label, a muted line):
      1. *Their courage*: They knew the risks, and flew.
      2. *Who they were*: The people behind the names.
      3. *We will go on*: We fly again, and finish what they started.
      4. *We will find out why*: We will learn what went wrong, and fix it.
    - **"Your words (optional)",** as component 28's quote (140 characters, filtered, counter, error),
      placeholder "Add words of your own, printed as you write them (optional)".
    - **Preview,** "IT MIGHT READ": newsprint lying flat (no tilt) with the tone's first statement line
      and the names; "Pick a tone to see how it might read." until a tone is picked.
    - **Footnote:** "If you give no statement, a spokesman's short one is printed. You can change yours until the
      turn resolves."
    - **Buttons:** Cancel, "Give statement" (primary, disabled until a tone is picked). No Withdraw (changing is
      enough; unconfirmed).
    - **The rivals' condolence form** is component 28 with its own title ("Condolences to <agency>"), "What
      you send": two rows (*Condolences*: Sympathy for the families and the agency. *A tribute*: Honour
      the crew and what they did.), the quote, the status "✓ Sent this season: A tribute", Withdraw at
      the left, Cancel and "Send", and the footnote "Only what is sent is printed; silence is never
      reported."
    - **The report card:** a full-width primary Card (1.5 px navy) under the summary strip: the eyebrow
      "The press", the title, 40 px greyscale portraits with one line, "If you give none, a spokesman's
      short one is printed." and "Give a statement". **The hub's card:** the same at 420 px, top left
      (the advisor's place), with "Until the turn resolves". **The rival's line:** first in the News
      card, "<Agency> has lost the crew of <mission>" with one more line and "Send condolences"
      (secondary), no eyebrow and no colour.

30. **Standing chart** *(the user's idea, 2026-11-02; canvas `Launch Window — Standing`; the numbers and the
    canvas's choices are **unconfirmed**; G20; `client.screen.StandingChart`, the numbers in
    `client.view.Standing`; `-Dshots=standing` renders the canvas's boards)*. How an agency's prestige and money moved over time: the statistics
    this kind of player loves. Shared by the Public Affairs Office's Standing tab (§4.10) and the game result
    (§4.13).
    - **Two charts stacked on one timeline, never one chart with two Y-axes** (user-approved 2026-11-02): crossings
      between differently scaled lines mean nothing, and rivals' funds are secret, so a shared chart couldn't take
      the toggle. One white card at full width: prestige 470 px on top, funds 220 px below, with an outlined
      navy "YOURS ALONE" tag and a caption that rivals' funds are secret, the same in both modes; one year
      axis under both; a key row for the marks at the foot with "As of the last resolved season, Fall
      1962" at its right. Gutters 64 px left, 236 px right.
    - **The toggle:** a segmented control (`SegmentedControl`, 260 px), "Just me" and "Everyone", at the
      right of the prestige heading. Everyone adds each rival's prestige line, public since `design.md`
      §2.1(g). "Just me" is the default; the last choice is kept per device.
    - **The time axis** uses the full width: the first shown point on the far left, reading "Start" (a
      report's turn is the season it resolved, so the point after turn 1 is Winter 1955), the latest on
      the far right, evenly spaced, so the curve grows denser as points accumulate. Each resolved turn is
      one point; a goal's mark sits on the season the race dates it. Year labels at each Winter, at least
      64 px apart; with six points or fewer the axis names each season instead, and the dots grow (r 6).
    - **Markers and the window:** dots on the points while at most **40** show, a plain line after that. Past
      **120** points (30 game years) the window slides: only the latest 120 show, and a "LATEST 30 YEARS"
      chip (skill-track fill) sits beside the prestige caption. A game lasts about 80 to 95 seasons, so
      most never slide. The game result never slides: it always shows the whole game.
    - **The Y-axis:** round tick values and light gridlines. Prestige starts at 0 and fits the top (a
      small early step never looks like a cliff); funds fit both ends and always show zero, a darker zero
      line labelled "Zero"; below it the line and dots turn `danger` over a 14 % danger fill, and the end
      label is the danger tag ("−2,860").
    - **Lines:** yours navy 3 px, rivals 1.75 px in the chart tokens, coloured by the agencies' order on
      the server (which the race table shares). Each ends in its agency's name in its colour (yours bold)
      over the latest value in ink, instead of a legend; labels closer than 34 px are pushed apart in
      order, with a thin leader. A filler bot reads "(bot)" as elsewhere. A late joiner's line starts at
      its first point.
    - **Hover:** a navy crosshair through both plots at the nearest point, a white ring on each line's
      point, and component 21's white popover: the date, your prestige and funds, with Everyone every
      agency's prestige highest first with a colour swatch, then the event there. It names no prestige
      conversion: the turn report has no event for one, and adding one wasn't worth a saved format.
    - **Event marks:** a declared goal is a dot in its line's colour with a white ring (r 6), a First
      gold ringed in the agency's colour (r 7) (`RivalSummary.goalTurns`; rivals' are public); your
      lost crew an ink diamond on your line; your budget reviews navy 30 % dashed through both plots,
      with "Review n" at the top. Only declared goals are marked.
    - **States:** loading; failed ("Couldn't fetch the standing. Open the office again to retry."); before the first
      resolution, the card keeps its headings and toggle and each chart's place is a hatched dashed box
      ("Your first season hasn't resolved yet.", and why: a line needs two points); with two or three
      points, the dots carry it, the line spanning the width.
    - **Data:** resolved turns only, so the latest point can differ from the header's prestige and funds, which
      include queued actions; hence the caption.
    - **Game result:** the race table gains a Prestige column and a line swatch before each agency, rows
      44 px; under it "Prestige, the whole game", 250 px, every agency, no funds or review lines. The
      dialog is 1280 px wide (the user's play test, 2026-11-11: bots' long names spilled the race table
      out of the card); 32-character names with "(bot)" fit at Large text (`result-long-names.png`).
    - **Tokens in `Theme.java`:** `CHART_RIVAL_1` #0b7480 (teal), `CHART_RIVAL_2` #8a3d82 (plum), `CHART_RIVAL_3`
      #2c6fba (sky blue), `CHART_RIVAL_4` #5a6678 (slate), each 5 : 1 or more on white, none gold, green or red (a fifth
      rival repeats them dashed); `CHART_GRID` navy 10 %, `CHART_AXIS` navy 45 %, `CHART_REVIEW` navy 30 %,
      `CHART_DEBT_FILL` danger 14 %. No icons: the marks are drawn shapes.
31. **Skill ceiling** *(the user's decision, 2026-11-02, built straight in libGDX, `design.md` §2.2(c)):* how high a skill
   can still grow, given the person's other skills. On the person page a `SkillBar` gains a 2 px `INK_MUTED` tick at
   the ceiling, none at 100; the skill's tooltip adds "Their other skills leave room up to 87.5 (the tick)." or, at or above it,
   "Their other skills leave no more room: this one has stopped growing." (`Tips.ceiling`). The Flight Director row has no
   tick: it's an average. The training dialog's permanent estimate says "Their other skills leave room up to 90.0, so training
   stops at about 89.8 after 57 seasons." for a target past it (`TrainingPlanning` takes the ceiling). `-Dshots=focus`.

---
## 3. Screen flow *(decided)*

```
Splash ─► Connect ─► Join game (skipped if already in the game) ─► [missions flown: "Watch them first?" ─► each mission's replay] ─► front pages ─► Turn report (if a turn resolved since last visit) ─► Space Complex hub

Space Complex hub
├─ VAB
│   ├─ Open rocket program
│   ├─ Rocket R&D (also from Manage rocket programs)
│   ├─ Manage rocket programs
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
`code style`), its actions, and the states it must handle.

### 4.0 Splash
**Purpose:** the animated logo, once, at every start of the client (the user's idea, 2026-10-12).
A borderless, non-resizable square window, 30 % of the monitor's height (confirmed, user, 2026-10-12), centred, plays
`assets/ui/splash/splash.webm` (4 s, 800 × 800) over `navy`, scaled to fit with linear filtering, with
Armstrong's "one small step" (`assets/audio/small-step.mp3`, NASA, 2.7 s) from 0.8 s, as the rocket
starts to climb, when sound is on. When it ends, the game's own window opens and the
splash window closes. **Actions:** any key or click skips it. **States:** a video that can't
play (a missing native, a missing file) or never reports its end (after 8 s) goes straight to
the game's window, logged as `[Splash]`.

In the launcher, not in core: `lwjgl3.SplashListener` is the first window of the
`Lwjgl3Application`, and opens the game with `newWindow(new LaunchWindowGame(), ...)`, so the
game's fonts are made at its own window's size from the first frame. gdx-video 1.3.4 plays WebM
and AV1 on desktop, never MP4. The clip is made in EffectCraft from `logo.svg`'s own shapes:
`python assets-raw/splash/make_splash.py`. `ScreenSnapshot` never shows the splash.

### 4.1 Connect
**Purpose:** connect and authenticate (`client.screen.ConnectScreen`). One form: server
(host:port), login, password. There's no display name: other players see you by your agency and
director name, chosen per game (§4.2).
**States:** connecting; wrong password (under the password field, in the server's own words);
login has invalid characters (`[A-Za-z0-9_-]`, up to 32) or an empty field (caught on the client
before sending); client and server versions don't match (a warning notice above the card, both
versions); server unreachable or a broken-off exchange (danger notices); **no such account**
(2026-10-25, review.md 2.3; unconfirmed): a login the server doesn't
know is never made into an account by itself, so a typo can't start a second, empty one. An info
notice above the card says "No account named *login* on this server. Check the spelling, or create
it with the password you typed.", with a primary **Create account *login*** button under it, which
creates the account (`CreateAccountRequest`) and logs in. The card's hint says "New here? Connect
with the login you want, then create the account." `-Dshots=connect` renders the state.
**Remembered between runs (user decision, 2026-10-05):** the server, login and
password of the last successful login are kept in
`~/.launch-window/client-settings.json` (`client.settings.SettingsStore`), so the form comes up
filled in, focus starts on the password field, and Enter logs in. A failed login is never saved.
Log out and Switch server in Settings keep the login and password and save the server chosen there.
**Known servers (user, 2026-11-12):** an icon-only button (the
`restart` arrow) beside the server field, which it shortens, puts the next known server into the
field on each press: the one after the server the field shows, or this machine
(`localhost:45825`) if it shows neither, so every press changes the field; then the public server
(`nas5714.myqnapcloud.com:45825`), round and round (`ClientSettings.SERVER_PRESETS`). With nothing
saved yet, the field starts on the public server (`ClientSettings.defaults()`).

### 4.2 Join game (first visit to a game only)
**Purpose:** join the game (`client.screen.JoinScreen`). **Shows:** an archetype choice between
NASA-style and Soviet-style, with the two hub artworks as large cards (several players may pick
the same one); an agency name; the **director's name** (the player's persona,
the head of the agency, fixed for the whole game); a hint (user, 2026-10-23), *"Your choices here
last the whole game."*, explaining what the style decides (its own program tree and headquarters;
budgets and rules the same; not exclusive) and where rivals see the agency's and director's names
(front pages, statements and condolences, Intelligence Office files, the Museum's list of
agencies); the text is in `JoinScreen`. The heading is "Join game" / "Found your agency"; text on
the blue page uses the on-page colours. *Sound (the user's idea, 2026-10-12):* Kennedy's "We choose to go
to the Moon" (`assets/audio/moon-speech.mp3`, NASA, 8.6 s) plays once when the screen opens, when
sound is on; leaving the screen stops it. **Actions:** Join. **States:** agency name
taken; director name taken (both checked ignoring case); empty fields; no archetype picked
("Pick an agency style." under the cards when Join is pressed). A "Server settings" ghost button at the
foot opens the admin's settings (§4.22), so the game can be set up before the first agency is founded.

**History note on the Director field** (component 15, the first one a player
sees): *"Both agencies here have a Director, but in reality the
two jobs were very different. NASA's Administrator was a public figure,
appointed by the President, confirmed by the Senate, and questioned by Congress.
The man who drove the Soviet program, Sergei Korolev, was a state secret. Until
his death in 1966, the world knew him only as "the Chief Designer"."*

**After joining:** the starting state is 25,000 funds, 250 prestige, 3,000 per season, every
building at level 1, empty rosters, and ten candidates per group (BASPM's ten, decided
2026-10-06). The advisor (component 24) takes it from there.

**Randomize** (the user's idea, 2026-10-12): players put on the spot
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
**Purpose:** navigation, and the game's front page (`client.screen.SpaceComplexScreen`, the logic
in `client.view.Hub`). **Shows:** full-screen art for
the player's archetype, picked by the game season, with the time of day advancing
on re-entry (`design.md` §3.13; the art from `assets-raw/make_hub_art.py`, 32 JPEGs cropped to
1080 rows). The building hotspots highlight on hover with a label (`HEADING_SM`, upper case). A
slim overlay strip shows:
- the agency name and director name (a navy nameplate);
- the game date (navy chip) and the next-resolution countdown;
- a Turn Report button (white, navy outline), opening the latest turn's report (§4.4), with a
  gold dot while unread;
- the funds and prestige chips;
- a white icon-only settings gear (Settings, §4.12, where Log out is), then a help button
  (component 23);
- dev mode only: a gold "Dev · Resolve turn now" chip at the start, resolving the open turn at
  once and opening its report (`design.md` §5, M5).

Chips use the chrome's formats ("SPRING 1955", "NEXT TURN IN 6 H 12 M", "$ 25,000"). The
content-differs warning shows as a warning notice under the agency name. A new agency's advisor
card sits at the bottom left (component 24).

**No header or footer chrome** (user, 2026-10-04), so the art stays whole: the strip is the only
chrome. A with-chrome mockup letterboxed the hero art and looked clearly worse (`design.md` §3.13).
**Every strip element is solid:** no translucent chip and no fade, because the art's top band
runs from near-black (night) to near-white (winter noon); checked on Soviet winter noon.

**Time of day** (confirmed, user, 2026-10-04): it steps on when the player enters the hub and at
least 4 minutes have passed since the *last step*, not since the last entry, so someone hopping
between screens every 3 minutes still sees it change. One step however long it's been; the first
entry shows noon.

**States:** a building that is upgrading shows the larger gold gear marker; a game result exists
(announced on the first hub visit after it, until the player closes it, §4.13).

**Building markers** *(user's play-test request, 2026-10-05; look unconfirmed)*: every
building always shows a marker where the upgrade gear sits, so players see at once what can be
clicked: a white disc with a navy ring and the place's own icon, the same one the footer uses
(VAB rocket, Mission Control headset, Astronaut Complex astronauts, SET Centre engineers,
Headquarters folder, Public Affairs Office dollar, Museum trophy). It turns navy with a white
icon while its building is hovered. The marker counts as part of its building for hover and
click. The icon is one field per place (`Hub.Place.icon()`), easy to swap. Idle-staff and
decision dots sit on the markers (components 25, 26).

### 4.4 Turn report (automatic after a resolution, reopenable from the hub)
**Purpose:** "what happened overnight", the async equivalent of BASPM's end-of-season news
screen (`client.screen.TurnReportScreen`, its wording in `client.view.TurnReportView`, from G2's
saved reports). One scrolling screen, not tabs (confirmed, user, 2026-10-06).

*Spoiler-free replays first (the user's idea, 2026-10-12):* when an unread report has missions
flown, a dialog over the hub asks before the report opens: "Last night's missions are in. Watch
them first, spoiler-free?" [Watch the missions] [Show the report], with "Don't ask again". Watching
plays each mission's replay in turn (§4.7.3, the film), then the unseen front pages, then this
report, which itself is unchanged: once the player has seen it, spoilers are fair. Settings keeps
the choice: Ask, Always watch first (the default since 2026-10-27, user), Never. An unread turn
comes in this way however it arrives: entering the game, the hub's report chip or hotspot, or
the dev button "Resolve turn now". A read report opens at once.

**When it opens** (confirmed, user, 2026-10-06): an unread report opens on entering the game. A
turn that resolves while the player is in the game shows a toast and lights the hub's Turn Report
dot, rather than taking over the screen. "Read" is remembered per device, in the client's
preferences, per server and login.

**Earlier turns (user, 2026-10-04):** "Previous turn" and "Next turn" step through the saved
reports. Previous turns off once the server says a turn resolved before the player joined. An
earlier report leaves out the morale warnings, which describe the roster now. Replays work from
any report.

**Shows, grouped:**
- missions flown this season, with step outcomes (success, glitch solved, minor
  glitch, failure, catastrophic), prestige gained or lost, and goals achieved
  ("first in the game!"). A mission's step squares wrap into even rows of at most 20, so a
  33-step Moon flight doesn't push the other columns off the screen (the user's play test,
  2026-11-11; shot `report-long-flight.png`);
- R&D gains per component, and components that hit their R&D cap (researchers
  released). Each component's entry opens with its blueprint drawing
  (`assets/ui/blueprints/<component-id>.jpg`, 864 x 352) across the full card width, at its own
  aspect, above the name, change, reliability bar and caption (the user chose the full-width
  banner over a thumbnail, accepting a long column; a component without a drawing shows none);
- the People card: training finished (skill gain), Expert trait earned; controllers' flight
  growth summed into one line; a morale warning for idle people at morale 50 or below (the
  resignation floor plus 15, derived by the client). It is led, lightly tinted, by people news: a
  trait revealed ("After two seasons on the Atlas team, Kowalski turns out to be a workaholic"),
  a relationship shown ("Ivanova and Petrov work well together"), ambitions formed, met, missed
  or overtaken, each ending with its trait chip or marker ("Clash" / "Work well"); a click opens
  the person page while they're still in the agency (`TurnReportView.peopleNews`);
- departures (retired, resigned, dismissed, perished, lost in an accident). No hires: they're
  the player's own actions, not season events. Mission-end releases ("the 9 controllers are idle
  again") aren't listed;
- a lost passenger: "Laika did not come home", with "The flight had no way home." or the step that
  failed (`PassengerLost`);
- a grounding (`design.md` §2.5(j)): after a crew is lost, "Crewed flights grounded" (tag
  GROUNDED, red): no crewed flight may launch for the next N turns while the accident is
  investigated; uncrewed flights go on (`CrewedFlightsGrounded`). Mission assembly gives the turns
  left under its disabled launch button, and every crewed mission card waits on the grounding in
  its Astronauts check and earliest launch (component 25). Shots `grounding-*.png`
  (`-Dshots=grounding`);
- the Budget card, which accounts for **what the turn's resolution did to the money** (user,
  2026-11-04), so its lines add up to the change in funds (its Net is the Funds cell of the
  strip): funding and running costs (broken down by kind), then commercial contracts (fees and
  refunds), press coverage (a press story's money), signing bonuses (engineers poached), an R&D
  decision left to the team (its money), and a random event's unexpected cost, which records only
  its share and so takes what's left; a remainder with no such event reads "Other". The player's
  own orders (launches, openings, hiring, conversions, decisions made) are applied before the
  resolution and are on no line (`TurnReportView.budget`). A review names the new rate (the old
  one isn't kept). Notices on top:
  - **The answer to a rival's First** (`design.md` §2.1(e)): a success notice, "Beaten to it: the
    government answers.", naming the Firsts another agency took last turn and the prestige the next
    review adds (`RivalFirstsAnswered`). When a review falls in the same season, the notice says
    the review counted it, and the review's notice adds "and N more for rivals' Firsts". Shots
    `rival-firsts-report.png`, `rival-firsts-review.png` (`-Dshots=rival`).
  - **A raise between reviews** (`design.md` §2.1(f)): "Funding raised: + $X a season.", with the
    prestige, the review whose bracket priced it and the rate before and after (`FundingRaised`);
    the funding line reads "Funding (new rate)". Shot `raise-report.png` (`-Dshots=raise`).
- the News card, captioned "This season": it opens with one real event from the season the turn
  played out in, under an "On the real timeline · <season>" eyebrow, with a navy accent bar, and
  "At home:" with the player's own side's view where the story has one (`design.md` §1.1); then the
  random events (`newsLog` entries) as stories (component 25). When the resolution printed front
  pages the player sees on login (§4.18), it opens with "The Daily Zenith ran a front page this
  season." (or "ran N front pages") and a secondary "Read the paper" button, which reopens them
  all in order (`FrontPageScreen.ofTurn`: "Next front page", then "Close", back to the report;
  user, 2026-11-11; shot `theories-report.png`);
- building upgrades finished.

*News placement (the user's request, 2026-10-10):* the blueprints make the R&D section tall, so
when it shows more than one component the News card and the Intelligence card move to the left
column, right under Missions flown, ahead of Goals and Buildings. With one component or none they
stay under R&D. Shot `report.png`.

**Actions:** open a mission's replay (§4.7.3); continue to the hub.

### 4.5 VAB: rockets
**Purpose:** everything about rockets (`client.screen.VabScreen`, `OpenRocketScreen`,
`RocketProgramsScreen`, and `RndBoardScreen` in its `ROCKET` mode; view logic in
`client.view.Rockets`). The VAB's own cap limits the open rocket programs (4/8/14), and its level
sets the largest rocket class (Light, then Medium, then Heavy). Both sit on this screen next to its
building panel; every number comes from the catalog's building schedule, and the upgrade dialog
names the class the next level unlocks ("and unlocks the Medium class"). Greying out and refusals
use `RocketPrograms.checkOpen`, `classAllowed` and `inFlight`, the server's own checks.

Both rocket lists start each row with the rocket's picture at 140 x 90, from
`assets/ui/rockets/<id>.jpg` (`make_content_art.py`, from `assets-raw/Rocket Programs/`); a rocket
without art keeps its skill glyph in the same space (`ChromeScreen.addRocketPicture`). Under the
row, aligned with the name (`ChromeScreen.ROCKET_TEXT_INDENT`), the rocket's long description
(`longDescription` in `strings.json`, user, 2026-11-04). Shots: `vab-open*.png`, `vab-manage*.png`,
`standard-vab-manage.png`.

**4.5.1 Open rocket program.** The agency's rockets that aren't open, each with:
- class, and payload capacity to suborbital, LEO, TLI, and TPI, each heading with a tooltip;
- human-rated or not;
- costs as "Creation · once", "Unit · per launch" and "Upkeep · per season", each with a tooltip;
- a "Starts at" column: the reliability the rocket would open with right now
  (`RocketPrograms.openingReliability`, the value `open()` uses), where it comes from, and an
  **R&D pace** (fast, average or slow, from the slot bands via
  `SkillGrowth.expectedFullTeamRawAmount`; the thresholds an unconfirmed default). An info notice
  above the list explains that a rocket starts with part of the reliability of related rockets
  the player owns, taken when it opens (BASPM's transfer data, `baspm.md` §10.3: Atlas takes 35 %
  of Redstone's reliability and 20 % of Jupiter-C's);
- its concepts (component 19), after the long description.

Open shows the price: the creation cost, charged again on every reopening ("Still owned from
before, with its reliability. Reopening charges the creation cost again."). Refused if the VAB is
full or its level is too low for the class (`RocketsTest` proves the gate on a fixture catalog).
Other agencies' rockets are hidden. After opening, a dialog asks whether to assign engineers now;
**Assign R&D** goes to Rocket R&D with that rocket's assign dialog open.

**4.5.2 Manage rocket programs.** The open rockets with reliability bars, which
in-flight missions use them (the first mission's badge and "+N"), upkeep, and links to Rocket R&D.
Each row also shows what the rocket can lift, the same four columns as §4.5.1 with their tooltips
(`ChromeScreen.rocketPayload`), and the Human-rated chip beside its class, so deciding which to
keep open needs no trip to Open rocket program (user, 2026-11-06). The row has two lines beside the
picture: name, lift, the in-flight badge and Close on the first; reliability and upkeep under the
lift, so a long badge never runs into Close. **Close** needs a confirmation: "kept with its
reliability, no upkeep; reopening costs the creation cost again; refused while a mission uses it";
on a rocket in flight it is greyed out, and clicking it toasts the refusal.

**4.5.3 Rocket R&D** *(decided: split from payload R&D)*. The R&D board
(component 10) for rocket components. A button on the VAB's landing page as well as on Manage
Rocket Programs, as Payload R&D has on the SET Centre's *(user, 2026-11-01)*; its breadcrumb and
back button go to the VAB.
### 4.6 Headquarters: programs and mission planning
**Purpose:** decide what the agency works on, and plan and launch missions.
Headquarters' cap limits open payload programs (3/7/10/16). Screens:
`client.screen.HeadquartersScreen`, `OpenProgramsScreen`, `SelectBodyScreen`,
`BodyProgramsScreen`, `ProgramDetailsScreen`, `MissionConfigsScreen`, `MissionStepsScreen`,
`MissionPlanScreen`, `AssembleMissionScreen`; view logic in `client.view.Missions`, `MissionDraft`
and `CrewSkills`. The building panel (component 7) sits on Headquarters and the VAB; the personnel
buildings carry a compact building cell at the end of their summary strip.

**Landing screen:** cards for Plan a mission ("Ready to plan · N missions"), Manage open programs,
Browse programs, and Contracts (§4.17). The cards in a row of a landing screen (Headquarters, the
VAB) share the tallest one's height, each button at its card's bottom (`ChromeScreen.navCard`; user,
2026-10-27).

**Plan a mission** (play-test request, 2026-10-07) lists every configuration of the open programs,
one section per program (its name in gold above its cards), with the §4.6.5 card (`ConfigCards`).
Both configuration lists show each configuration's whole picture at the card's width and its own
proportions (`ui.FullWidthPicture`), never cropped or stretched, four columns a row (empty cells
keep the columns equal). A card opens the mission plan on its assessment tab (§4.6.7). Back and
the breadcrumb from the plan and from Assemble lead back to this list ("Space Complex ›
Headquarters › Plan a mission › Vostok · Orbital flight › Plan"); the path through a program's
pages works as before (`ClientSession.planningFromList`). With no program open it points to
Browse programs.

**Goals and reliability on the cards** (play-test request, 2026-10-09). A
goal not reached lowers the reliability some of a mission's components fly with; some reached goals
raise it (the configuration's goal dependencies and boosts). Every configuration card shows a red
"Goals not reached: −20 reliability" and a green "Goals reached: +5 reliability" when they apply,
above the prestige note; every program card in Headquarters' lists a red "Skipped goals: up to −30
reliability", counting only goals from outside the program (its own first flights are the way in)
at the most any one of its missions loses, so skipping ahead shows before a program is opened. A
tooltip names each goal and what it does (`client.view.GoalModifiers`).

**Staffing ahead of assembly** (play-test request, 2026-10-07). Every
configuration card adds "Free now: 5 of 2 astronauts · 10 of 9 controllers" under its stats, red
when short (free means what assembly counts: idle and available). The planning screen's assessment
tab adds a Staffing card: per group needed, free, in training that can be cut short, in basic
training and busy; a red line per short group saying how many short and who's coming ("the first
done in 1 season"), with a reminder that hiring takes basic training; and every seat the mission
uses with the skill it reads most (`client.view.Staffing`).

**4.6.1 Manage open programs.** A list of open programs, each with upkeep, its
components' reliability, missions in flight, and the next goal it can reach. It
leads to program details.

**4.6.2 Select celestial body** *(decided)*. The `solar_system.png` art with one
target per body: Earth, the Moon, the Sun, Mercury, Venus, Mars, Jupiter, Saturn.
Under each body, the count of programs **available** (unlocked for the player's
agency) and **open**. Earth holds Earth-orbit satellites, space planes, and one-,
two- and three-crew capsules; the Moon holds lunar probes and lunar expeditions.
Inert bodies' labels read "No programs for / your agency" on two lines, so neighbouring chips
don't overlap. Content: G9.

**4.6.3 Program categories and programs** (for one body). The categories at that
body (category art); then the programs in a category (program art), each with its
state: open, available, or locked (with the missing goals). There's no "Closed · components kept"
state: the state can't tell a closed program from one whose components are owned through another
program (Vostok's capsule through Korabl-Sputnik 2), so a closed program reads Available, and its
price breakdown's "already owned · Free" lines say it. Other agencies' programs are hidden
entirely. Each card shows:
- the full price to open (opening cost after discounts plus components still to create), from the
  same rule the server charges;
- the astronauts and controllers its missions need, coloured against the roster, and a locked
  card's "Needs:" line saying in its tooltip where each goal is earned (component 25);
- what its missions can reach (user, 2026-11-11), "Achieves up to 10 goals", then "· 3 left" or
  "· all reached" (`Programs.goalsLine`); its tooltip, "Goals it can reach", lists those still to
  reach one a line and those reached in one line (`Programs.goalsDetail`);
- the commercial line, if any (component 27).

**4.6.4 Program details.** The right-hand card runs **full width**, every section in it (user,
2026-10-27). Shows:
- state, upkeep per season;
- the program's **long description** (`longDescription` in `strings.json`,
  `ContentNames.longDescription`; the short `description` stays for compact places such as the turn
  report): 450 to 650 characters for a program, 300 to 480 for a component (unconfirmed), standing
  on its own and never repeating the history note. Every program and every component a program
  grants has one (`ContentStringsTest`); sources in `assets-raw/strings/LONG_DESCRIPTIONS.md`;
- the price as a **table** across the card: Item, One-time price and **Upkeep / season** (what each
  line adds every season, as the Open dialog counts it: none for a component already active through
  another open program, a dash for a discount; the total is `Outlook.openingUpkeep`; the column is
  unconfirmed), its rows striped light navy and white: opening cost, discount from other open
  programs, and creation cost for components not owned yet (a component still owned from a closed
  program is free);
- the required goals as goal badges (a goal credited through another goal's cascade reads
  "Achieved", without a date; no "First" ribbon, which is the race's standings, G3); next-goal
  rewards read "Up to N prestige". On a locked program, where each missing goal is earned
  (component 25);
- under them, **Goals its missions can reach** (user, 2026-11-11): the union over its configurations
  of the goals their steps achieve, with the goals each credits (`Programs.goals`, through
  `GoalCascade`: Apollo's steps name only their own "flown" goals, and the crewed landing comes
  through the credit), the card's line above them, then a wrapping row of quiet chips (component
  20's look), those still to reach first, reached ones ticked and muted. Up to 32 for Apollo, which
  is why chips, not badges;
- the components it grants, each with its long description, its reliability bar if owned, its
  **blueprint drawing** (`FullWidthPicture`) at the card's width, and under its name its **R&D
  skill** (the user's play-test request, 2026-11-04): the skill's icon, "R&D skill: Crewed
  spacecraft", and the agency's best engineer in it ("your best engineer in it: A. Name, 62", busy
  or not; "no engineers hired yet" without any), so a player can judge the staffing before opening;
- the commercial line, if any (component 27); the earliest launch for an open program (component 25);
- a Concepts section (component 19);
- a link to mission configuration selection.

Shots: `hq-details-*.png`.

**Actions:** Open (shows the price and the funds runway, component 25; refused if Headquarters is
full, the goals aren't met, or funds are short). Close, which needs a confirmation: "no refund;
its components go inactive and their researchers are released; refused while one
of its missions flies". After a successful Open the client goes straight to the program's mission
configuration selection (§4.6.5) with the success toast *(user's play-test request, 2026-10-05)*,
unless one of its components needs engineers: when a component of the program is active with no
one on its R&D and isn't at its ceiling, a "Program opened" dialog (user, 2026-10-08, as the VAB
does for a new rocket) asks "Assign engineers to <component>?" (or "to its components?" when there
are several), with its starting reliability. Its buttons:
- **Assign R&D** (primary, rightmost; user, 2026-10-27: a program just opened needs engineers first)
  opens Payload R&D (§4.8.3), with that component's assign dialog open when there is one; with
  several it just opens the board;
- **Plan a mission** goes on to the mission list;
- **Later** stays on Program details with the success toast.

A program sharing a component that already has engineers (Vostok after Korabl-Sputnik 2) skips the
dialog. Shot: `hq-details-offer-rnd.png`.

**4.6.5 Mission configuration selection.** The program's configurations, each with
art (`assets-raw/make_content_art.py`, from `assets-raw/Mission Configuration/`), duration in
seasons, prestige (first time, or the repeat tier: 50 % second time, 30 % after that), launch
types, crew / controller / CAPCOM counts, the goal modifiers and "Free now" (above), and for an
open program a readiness row and the earliest launch (component 25).
Viewable even when the program isn't open *(decided)*; then the planning screens
are read-only, with an "Open program" call to action. A configuration gated on a goal
(`requiredGoalIds`) carries a navy "Locked" tag, and its steps screen (§4.6.6) names the goal
("Needs first: …") beside a disabled "Plan this mission". An air-dropped launch (`AIR_DROP`, the
X-15 and the MiG E-154) has no rocket picker: the launch card says a carrier aircraft drops the
craft, and assembly lists it as "air drop, no rocket".

**4.6.6 Mission configuration details and mission steps overview.** The steps,
season by season. For each step: the components involved (with shares), the
controller skills and CAPCOM involved, the crew skills, whether the crew is at
risk, the prestige share, and the goals it achieves. Also the goal dependencies
(penalties until a goal is reached) and boosts, the "Goal boosts" list captioned like the
penalties list. A Concepts section (component 19) in the summary card.

**4.6.7 Mission configuration** (the planning hub for one configuration), with
four parts:
- **Mission assessment** *(decided: computed in the client, through `core`)*. Each flown
  component's reliability with goal penalties and boosts applied, the success odds
  per step, and the weak spots. `rules.MissionAssessment` computes each step's exact outcome odds
  (success, glitch solved, minor glitch, failure, catastrophic) and the flight's completion and
  crew-loss odds from the season pass's own sums (G7). The assessment uses the plan as it stands,
  starting with Auto-assign's staff, and leaves out Expert rescues, changes between seasons and
  random events, and says so on screen. A step is "Weak" when it ends the mission at least 10 % of
  the time; weak steps share one notice. Big odds figures use `HEADING_LG`. The components card
  lists every boost on a component, a reached one as "+1" in green and one not reached yet as
  "(+1)" muted. The Staffing card (above). (Confirmed, user, 2026-10-04 and 2026-10-06.)
- **Rocket assignment.** One picker per launch. It lists the owned, active rockets
  that can lift that launch's payload mass to its launch type (only human-rated
  rockets for crewed launches), with reliability, unit cost, and spare capacity.
  Ineligible rockets are greyed out with the reason (`MissionLaunch.canLift`, `payloadMass`).
  Nothing is submitted here; the choice feeds assembly.
- **Payload R&D.** The same screen as the SET Centre's (§4.8.3), filtered to this
  mission's payloads, read-only.
- **Assemble mission** (below).

**Plan drafts:** the rocket choice and staffing live per configuration on the session
(`ClientSession.draft`), so they survive moving between screens. They start from the first rocket
that fits and Auto-assign's staffing. Every rebuild drops people who aren't free any more and
rockets that can't fly. The draft is cleared after a launch.

**4.6.8 Assemble mission** *(decided: one screen merging BASPM's schedule and
assemble)*. It shows the configuration's **staffing layouts** one to one, as BASPM
does (`baspm.md` §10.11), plus the chosen rocket per launch:
- **Flight crew** (`crewSlots`): one titled seat per astronaut aboard, for example
  ASTRONAUT, or COSMONAUT A and B on a joint flight. A crew seat shows a "seat fit"
  (`MissionStaffing.seatFit`, the weighted score Auto-assign ranks by).
- **Mission control** (`controlSlots`): titled jobs such as BOOSTER, RETRO, FIDO,
  GUIDO, SURGEON, FLIGHT, and CAPCOM. Each slot shows its icon for the skill it reads
  (the Flight Director icon for a FLIGHT slot) and its employee type; CAPCOM takes an
  astronaut, every other slot a flight controller.
- **Team chemistry** (component 21): a mission is two teams (user's decision, 2026-10-08,
  `design.md` §2.2(d)), so a card under the crew shows two chemistries, "Team chemistry · Crew and
  CAPCOM" and "Team chemistry · Flight controllers", each with its head count and "Why" (a team
  nobody is in yet is left out), updating as slots are filled. Tiles show revealed traits and a
  relationship mark when the partner is on this mission's same team, wrapping inside the tile
  (2026-10-31: one row of them widened every column past the card). The odds' caption says they
  include the known chemistry. Auto-assign picks by skill only.
- Slots no step uses show **"Not required"** and stay empty, as in BASPM. Crewed
  Mercury, for example, has 11 control slots and uses 10 (9 controllers + CAPCOM). Empty and
  "nobody free" tiles use solid borders (`Theme` has no dashed rectangle).
- Click a slot to pick an available employee of its type: idle, not unavailable, and
  not in another slot (only free people are listed). Any controller fits any controller slot; each
  candidate shows their value in the slot's skill (a Flight Director slot reads the average of the
  five skills), which is exactly what the roll will read. The picker has the R&D assign dialog's
  columns: traits, marks and the chemistry with each candidate in that slot instead of its holder,
  against the slot's own team (its caption names it); the current holder is highlighted with
  "Keep"; a candidate this seat would give their ambition is marked (component 25).
- **Auto-assign** (`MissionStaffing.autoAssign` in `core`) fills the crew seats first,
  each with the astronaut best at the skills the steps weight on that seat, then the
  Flight Director slots, then every other used slot, each with the best available
  person at the slot's skill. Nobody is placed twice; a slot nobody is left for stays
  empty.
- **Pull from training · N** *(user, 2026-11-11)*: beside Clear all and Auto-assign, shown only
  when the free staff can't fill every empty used slot and people in training (not basic
  training, available this season) can make up the rest; N is how many would leave training.
  It fills the empty slots in Auto-assign's order, each with the best free person of its type,
  and once a type has nobody free left, with the best of its people in training
  (`MissionStaffing.fillFromTraining`), so only the shortfall is pulled. CAPCOM draws on the
  astronauts. A confirmation (component 5) names who leaves training for which slot and the
  share of the round each keeps (nothing for Expert training); then the pull-outs go one by
  one and the slots are filled after the last. `-Dshots=pulltraining`.
- A slot that can't be filled says why ("only 5 flight controllers; this mission
  needs 9. Upgrade Mission Control and hire").
- A warning names anyone free whose ambition this team would pass over (component 25).
- **Confirm & pay**: a price breakdown (configuration cost + the rockets' unit
  costs) and one confirmation ("Launch for 2,243? It flies at the next turn.
  Scrubbing before then refunds in full"), the launch checked by `MissionLaunch.check`. On
  confirm, the cost is deducted and the mission appears under Mission Control's scheduled
  missions; "See it in Mission Control" opens them (§4.7).

**States:** the launch cap is reached (random event); a component is unavailable
this window (random event); short of staff; short of funds; crewed flights grounded (§4.4).

### 4.7 Mission Control: controllers and missions
**Purpose:** the flight-controller department, and watching missions. Controller
cap 5/13/22/35.

**4.7.1 Personnel overview** (component 11) for controllers, with sub-screens
Hire, Manage department, Inspect active missions, and Inspect scheduled missions.
Its glance row has one card for scheduled missions and one for active ones. With more than one
in a list, arrows beside that card page through them, going round at either end, and the card
says "Mission 1 of 2" (user, 2026-11-01). With one mission there are no arrows. The card still
opens the whole list.

**4.7.2 Inspect scheduled missions.** Missions launched today that fly at the next
resolution. Each shows its configuration, rockets, crew, CAPCOM, controllers, and
cost. **Scrub** (red, confirmation) refunds in full and frees everyone.
**4.7.3 Inspect active missions, and the mission replay.** Missions in flight,
each with season n of m, its people, and the components' reliability (no "prestige so far": the
state doesn't keep it). The mission view names the goal boosts in play ("Pegasus flown is
reached, so Mercury capsule flies 1 point higher"). Screens: `client.screen.MissionControlScreen`
(flight controllers), `MissionsScreen` (scheduled or active missions, one class with two modes)
and `RestaffScreen`; view logic in `client.view.MissionControl`. Hire, training, pull out, dismiss
and Manage department are shared by the three personnel buildings, through `client.view.Department`,
`PersonnelDialogs` and a `HireScreen` that takes the department. The roster filter says "On a
mission" where the SET Centre says "Researching". The Expert mark is the prestige star in gold
beside the skill (the font has no ★).
- **Re-staff mission control** *(decided 2026-10-03)*: reopens the assembly screen
  (§4.6.8) for this mission with the crew seats locked and every used control slot
  reassignable to an available person of its type, at no cost and with no confirmation (it's
  free and reversible; `ReassignControlSlotAction`, one action per changed slot; the previous
  holder goes idle). CAPCOM is reassignable too. The same applies to scheduled missions
  (§4.7.2). The picker lists only idle people and has no "Empty this slot" (the rule can't leave
  a console empty). "Changed today" lists only this visit's changes (nothing on the server
  records them, G6).
- A control slot whose last season has flown is **released**: its holder went idle at
  the season's end, as in BASPM, and the slot shows "Released" (no name: the season pass empties
  it) and can't be refilled. The crew stays aboard until the mission ends.
- **Cancel** (red, confirmation) is offered only for **uncrewed** missions: it
  shows the prestige loss and the refund (`MissionCancellation.preview`, the integer refund the
  rule pays). Scrub and Cancel use the generic dialog with price lines.
- Crewed missions show "Can't be abandoned", with no button (`MissionCancellationPreviewTest`).
- `MissionAssessment` can start from a season, for the odds of what's left.

**Replay viewer** (`client.screen.ReplayScreen`): reached from here or from the turn report
("Replay season N" opens it for the latest turn). It plays a mission's season from the saved
report's step events; nothing is rolled in the client. Per step:
- the step name, its text and the involved components with their reliability;
- the success chance;
- the outcome, and a saving throw if one happened ("glitch, saved by the
  controllers"), plus any Expert rescue with the Expert's name (stopping at the 95 % cap,
  `ExpertRescueLines`);
- the prestige running total.

It shows chances and pass/fail, never dice numbers. Then the season summary: goals achieved,
reliability changes, skill growth, and crew losses.

**The replay as a film (the user's idea, 2026-10-12).** Each step plays an animated clip
(`design.md` §5, Backlog, "Animated mission steps"), and the replay plays the steps by itself,
reversing the 2026-10-06 decision against automatic playback: with the clips it is meant to be
watched like a short film, and the player can still pause and step. **The clips are confirmed
(user, 2026-10-21):** every clip's story, craft, shape and proportion, and the decisions below, as
built at the end of the overhaul pass. The console, the playback timings and the sound placements
keep their own flags. Every step and its clips are listed in `assets-raw/replay/CLIP_CHECKLIST.md`;
the clips are built in EffectCraft by `assets-raw/replay/steps/build.py` and `publish.py`. Each craft's
clip-by-clip notes (what each variant shows) are in the 2026-11-12 version of this section
(`git show 5c95135b:ui-spec.md`); the rules below are what the clips follow.
- **Layout:** the step card keeps its head (step n of m, name, text, components); the result (the
  roll line and the outcome box) sits beside the success chance and the saving roll, and the
  monitor (the console, below) fills the rest of the card's full width and remaining height.
- **Clips:** `assets/ui/replay/<clip>-<variant>.webm`, rendered at 960 × 540, each with a sidecar
  `<clip>-<variant>.json`: `duration` and `revealAt`, the second from which the variant can't turn
  into another one any more (set by hand per clip), and its sound cues. Variants: `success` (the
  first roll passed), `glitch` (a glitch, solved or not: the mission goes on) and `failure`
  (failure; also catastrophic, until a step has its own `catastrophic` clip). No lettering (digits
  only). Steps cut straight into each other, so a step's success and glitch end on the same frame,
  and a step on the pad ends on the next one's first.
- **Choosing a clip:** `assets/content/replay-clips.json` (outside the content pack, not hashed;
  `ReplayClipsTest` holds every rule to its three clips and sidecars) maps step ids to clips. A step
  id loses its number (`03-`), then the first rule whose pattern matches the rest names the clip
  (`launch` matches `launch`, `atv-launch`, `gemini-b-launch`). A step matches only by its own name,
  behind a vehicle the file lists (`atv`, `gemini-a`, `soyuz-7k-ok`) or with a craft's letter after
  it (`reentry-a`), never by how its name ends: `lunar-ascent` never plays `ascent`'s clip (a test
  holds the list to the pack's vehicles). A rule may also name:
  - `"after"`: the step before (`landing` is on the Moon after `powered-descent` or a robot's
    `lunar-approach`, a probe's after `lander-descent`, a spaceplane's after `descent`), so a rule
    with `after` goes before one for the same step without it;
  - `"earlier"`: any step before it in the season (`lander-descent` follows `lander-entry` at both
    planets, so Mars is told by `mars-approach` earlier);
  - `"later"`: any step after it in the season's plan, which the player sees, so it spoils nothing;
  - `"programs"`: it matches only a configuration of one of them;
  - `"vehicle"`: the steps in front of a vehicle id (`atv`, `transtage`).
- **A step's own clip where history differs from a borrowed one** (user): Venera 7's carrier burns up
  in every variant (`bus-entry`; only the data it sends home differs); the landers' entries
  (`venus-entry`, `mars-entry`), descents (`venus-descent`: the glitch tears the chute, as Venera 7's;
  `mars-descent`: the glitch lights one engine late), Viking's landing (`mars-landing`: the glitch
  stands tilted on a rock, as Viking 2; the failure sends once and falls silent, as Mars 3) and
  surface work (`mars-surface`); Mars 2's third season `mars-rover` (PrOP-M on its skis); Viking's
  `viking-separation`, which Mars 2's `lander-detachment` plays too; Lunokhod (`lunokhod-deploy`,
  `lunokhod-drive`, the failure Lunokhod 2's end); the Luna landers (`luna-landing-rover`,
  `luna-landing-sampler`, `luna-sampler`, `luna-liftoff`; the failure Luna 15's); Surveyor
  (`surveyor-landing`, the glitch Surveyor 3's hops); Pioneer Venus Multiprobe (a large and a small
  clip for each release, entry and descent; none was built to land, so a success ends at the
  impact); `earth-departure` (the probe alone after the burn, which BASPM gives the probe).
- **The way home goes the orbit's way** (user, 2026-10-16): the Earth orbit's map goes round
  anticlockwise, so over the Earth the craft travels left; the way-home clips (reentry
  preparations to reentry, and the suborbital flight) play mirrored, nose left in orbit and nose
  right for the burn. Motes stream
  past against its way, along the suborbital arc too, and slow with the retrofire. A burn with no
  reentry preparations before it (Biosatellite's `de-orbit-burn`) plays `deorbit-burn`, the capsule
  turning round first. `reentry` after `earth-approach` (from the Moon) plays `reentry-from-moon`,
  unmirrored. Where the craft is already base first (after a suborbital flight's turn or after the
  retrofire), reentry preparations play `reentry-trim` or `reentry-trim-burn`: no turn, the jets
  trimming. A stuck jet in those failures is a plume (`parts.py`'s `plume()`), not a bar that read
  as a boom (user, 2026-11-06).
- **The retrofire comes before the pack's jettison** (user, 2026-10-16, `design.md` §5, M3), and
  orbit insertion coasts to the top of its arc, then burns to circularize. Every approach
  (`lunar-approach.jsx`) moves as an orbit does and arrives at the insertion's speed; the Mars
  insertion runs at `SLOW` 1.5 (`loi.jsx`).
- **Every craft as itself, the rocket generic** (user, 2026-10-17 to 2026-10-21; the report doesn't
  say which rocket flew, so any rocket can fly them): a program's package draws its own spacecraft
  in the generic timelines, a step's `"skin"` in `build.py` drawing `<prefix>-<part>` for `<part>`
  (`motion.jsx`'s `ALIAS`; a step's `"alias"` alone also draws). No step draws a generic craft but
  the rockets, the probes' pad clips (a probe under its fairing looks as the generic nose) and Mars
  2's entry (`e-shell` is its aeroshell's shape). The packages:
  - **Mercury** (`mc-`): the escape tower jettisoned after staging, the periscope, the three retros
    lit in turn, the landing bag. Its orbital flights keep the generic turn before the retrofire,
    though Mercury flew heat shield first all orbit (`design.md` §1.1; kept so, user).
  - **Gemini** (`gm-`) and the Agena (`ag-`, its own launch by `"vehicle"`): no tower, the equipment
    section dropped, the splash on two points. Spacewalks in steps: setup, experiment and teardown
    where three come in a row; on the docking flights a stand-up, out on the tether, a photo and
    back in with the Agena at the nose; on a rendezvous flight both capsules nose to nose, the
    other's crewman photographing. The Earth-orbit tests of circumlunar, lunar-orbital and
    direct-ascent Gemini fly their own hardware (`gt-`, `gl-`, `gd-`); the Transtage's burn is a
    `high-orbit-injection` (the converter's name, not a trans-lunar injection) onto a long
    ellipse, the camera pulling back to fit it, the capsule meeting the air at nearly lunar-return
    speed. Their Moon flights: the Transtage alone, or Gemini docked into it, letting go 0.4 s after
    the burn (`tpi.jsx`'s `SEP`); the adapter cast off on the Earth approach (`earth-approach.jsx`'s
    `DROP`); the propulsion module's, the direct-ascent craft's and the tug's burns; flybys pass the
    Moon in one step.
  - **Gemini EOR** (`ge-`, the Langley lander `ll-`/`lg-`, the S-IVB tug `sv-`): the walk aft to the
    lander, its release, docking and cast-off; on the Moon its pilot plants the flag and the whole
    lander climbs back to the stack; the Moon under the lunar-orbit close-ups (`d-moon`). Flights
    without a lander fly the frame empty.
  - **Vostok** (`vk-`, Korabl-Sputnik 2 too, landing on the ground), **Sputnik 1** (`sp1-`),
    **Explorer 1** (`ex1-`, its whips swinging out) and **Sputnik 2** (`sp2-`): the early-era pack
    has no generic craft left. Simplified: satellites drawn larger than their rocket's scale;
    Sputnik 2 separates; Vostok's pilot doesn't eject.
  - **Voskhod** (`vh-`): the backup retro, the soft landing, Leonov's spacewalk through the airlock
    and back in with the suit ballooned (the glitch his own, in head first).
  - **Soyuz 7K-OK** (`sy-`): the orbital module on the nose through the way home, gone ahead at the
    modules separation; docking, the spacewalk and the crew transfer (three clips each) by BASPM's
    step names. Every Soyuz-type craft drops its orbital module at the separation, where Vostok
    drops its pack.
  - **The Soviet lunar craft** on Soyuz's skin (`build.py`'s `variant_of`): the 7K-L1 (`s1-`), the
    7K-LOK on its Blok D (`lok-`), the LK (`lkl-` without legs for the Earth-orbit tests, `lkm-` on
    its legs on the Moon), and the LK-700 (`l7-`, a way home of its own and a trans-lunar stage of
    its own). On their Moon flights the Blok D (BASPM's rocket) makes the burns: the 7K-L1 drops it
    after the burn for the Moon, the 7K-L3 keeps it into lunar orbit; the LK lands and its
    cosmonaut plants the flag.
  - **Apollo** (`ap-`): the CSM on the S-IVB for the burn (`-stack` clips), the lunar module in the
    lander's scenes and upside down in the docking close-up, one scale for the transposition;
    Apollo 5's LM alone (`a5-`), the suborbital arc, the CSM's own spacewalks, two CSMs'
    rendezvous, the docking flight's Agena. The moonwalks plant a plain flag (no country's), carry
    the rock box and drive the rover; the flag stands in every surface scene after it.
  - **The later NASA satellites:** the Orbiting Frog Otolith (`of-`, its booms swinging out),
    Biosatellite (`bs-`, its capsule coming home alone) and Pegasus (`pg-`, Apollo's launch clips,
    its wings unfolding on its stage).
  - **The robot probes:** a drawing each (`parts.py`'s `PROBES`), their own copies of the generic
    clips that draw a craft (`build.py`'s `PROBE_CLIPS`); Zond 5 flies the 7K-L1's; the Veneras' and
    Mars 2's landers have their own release, entry, descent and landing.
  - **The X-15 and the E-154** (the Soviet X-15, `design.md` §1): their own takeoff, cruise, drop
    (the plane runs on ahead of its carrier before pulling up), climb, peak, fast run, descent and
    landing; the E-154 slung under a Soviet four-turboprop bomber.
  - **Known simplifications:** the lunar propulsion module is gone at the reentry preparations with
    no clip for its parting; the Earth-orbit tests' module burns and circumlunar Gemini's map keep
    the generic flame; Gemini EOR's heat-shield test reenters from the high orbit as a normal
    reentry; Vostok's pilot doesn't eject; the Frog Otolith's yo-yo despin and Biosatellite's adapter
    parting aren't shown; the LOK and LK's turn-around and docking, and the Blok D braking the LK
    towards the surface, aren't shown; Gemini EOR's lander test in lunar orbit goes from its powered
    descent straight to the climb (BASPM has no landing between).
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
- **Playback:** the title buttons are restart, previous, Play/Pause, Next step ("Season summary"
  on the last step) and Skip to summary; a click on any step in the list goes there. Playing, the
  next step starts when a clip ends. Paused, the step's clip still plays through once and shows its
  result; only the move on waits. Next step, a click in the list and Skip to summary reveal what
  they pass. A replay opens playing.
- **The queue:** opened from the dialog (§4.4), the replay plays every mission flown that turn, in
  the report's order. The season summary then offers "Next mission" or, after the last,
  "To the turn report" (the front pages first, if any). Back leaves the queue for the report.
- **Proposed defaults (unconfirmed):** the 1.5 s and 3 s timings; the console's label;
  that the dialog offers only the newest turn's missions when several reports are unread.
- **The console** (the user's idea, 2026-10-13; canvas `Launch Window — Replay Console`;
  `client.screen.ReplayMonitor`, the instruments' motion in `client.view.ReplayConsole`): the
  monitor is a mission-control console, a navy panel drawn by the game (sharp at any size). The
  user chose the layout most boards show: picture left, instruments stacked in a column on the
  right.
  - **Layout:** a 26 px label strip on top: a signal lamp (gold with a ring while the camera is live,
    dark on "No signal"), the label ("Cam 1 · Launch", or "No signal"), and on the right
    "AIR-TO-GROUND" with a history note on the Quindar tones (user's text; the icon in cream on the
    panel), "PAUSED · NEXT STEP WAITS" left of it while paused. The 16:9 picture, in a thin lighter
    lip, takes the width but a 280 px column, capped by the height above a 53 px row of step lamps
    (the plan's steps: flown cream, current gold with a ring, the rest dark outlines; "3 OF 11"; the
    mission's name on the right). The column always stacks three panels: the mission clock (with a
    HOLD lamp), the status gauge (with CAUTION and MASTER ALARM lamps under it) and the telemetry bars
    (PRS, TMP, PWR), the last filling the height left. At 1920 × 1080 the console is about 1086 × 430
    to 520; the picture is never cropped or stretched. On a console too short for all three bars the
    ones that don't fit are left out.
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
  - **Unconfirmed:** every timing and reading above, the wander's split between its two waves, the
    bars' periods (3.1, 2.7 and 3.7 s), and the clock.
  - **Shots:** `-Dshots=console` renders the console alone in every state; the replay group has
    Vostok's launch live, while it plays and after (with a stand-in video player that shows one
    frame of the launch clip).
- **Reviewing the clips (the user's idea, 2026-10-14):** dev-tools' `MissionReplayer` (`mvn -pl dev-tools compile exec:exec@replayer`, `start_replayer.cmd [pack]`) plays any mission season in this screen without a game: a list of every season with how many of its steps have a clip, then a step (or every step) and an outcome. It plays a made-up report (`ReviewReport`), so nothing is rolled; keys 1 to 4 switch the outcome at the step showing, R plays it again, Esc goes back to the list. The clips, sidecars and sounds are read from `assets/` before every replay, so a new render plays without a restart.
- **Sound** (the user's idea, 2026-10-13; `client.screen.ReplayAudio`): nothing is
  baked into the videos, so sound can run on across steps. All English (user). Four layers:
  - **The room, the whole replay long:** mission control's room tone, a quiet loop of radio chatter
    (the user's own mix of NASA recordings). It runs on across steps and missions in the queue, and
    through the season summary; leaving the screen stops it.
  - **A fanfare at the season summary** (the user's decision, 2026-10-13, instead of background
    music, which was hard to get right without it grating): `fanfare-failure` when the mission failed
    (its last step a failure or a catastrophe), `fanfare-success` for anything else, glitches, solved
    or not, included, and for a season that ends with the crew still aboard. It plays once, whichever
    way the summary is reached, again if the player comes back to it; going back to a step fades it
    within 0.3 s.
  - **The console's sounds, from `ReplayConsole`:** a telemetry blip while a camera is live, one of
    three takes at random, every 2 to 4 s at random, the softest sound in the replay (none once
    telemetry is lost), the CAUTION tone each time its lamp lights (three for a saved glitch), the
    MASTER ALARM for 3 s after a failure's reveal (the lamp stays lit), quiet static under a step with
    no signal, a click on Play and Pause. A lamp already lit when the step is reached stays quiet.
    The same rule as the lamps: nothing before the reveal depends on the outcome
    (`ReplayConsoleTest`).
  - **The clips' cues**, in each sidecar beside `revealAt`: `"cues": [{"at": 0.4, "sound":
    "engine-roar", "volume": 0.8, "loop": true, "until": 5.8, "fade": 0.8, "carry": true}]` (all but
    `at` and `sound` optional). A loop stops at `until` (or the clip's end) after its fade; `carry` on
    a loop at 0 keeps the same sound running from the clip before instead of starting it again. A
    clip's variants have the same cues before the earliest reveal (`ReplayClipsTest`); what counts is
    what's heard, so a loop whose `until` is at or after the reveal is the same as one that plays on.
    The cues are kept in `assets-raw/replay/cues.json` and written into the sidecars by `cues.py`
    beside it, which the clip builders run on publish. `rocket-roar` plays under the launch from
    ignition, the ascent's two stages (the upper stage quieter) and the retrofire, each cut with its
    engine in a failure. **Effects** (the user's finds, `assets-raw/replay/sounds/make_effect_sounds.py`):
    `rcs-puff`, `hatch-open`, `fueling`, `vent`, `reentry-plasma`, `splash`, `helicopter`,
    `explosion`, `decoupler`, `explosion-debris`, `servo`, `deploy`, placed where their layers show in
    the clips' built projects by `assets-raw/replay/effects.py` (rerun after a clip is published with
    a changed timeline; it skips a clip whose project is newer than its render). Before the reveal an
    effect only some variants have waits for the reveal if it's within 0.35 s of it, and is left out
    if not. **Unconfirmed:** the roar's volumes and fades, and the effects' volumes.
  - **The pool:** `assets/content/replay-sounds.json` (outside the pack, like the clips), each sound an
    id, a kind (effect, loop, radio, music for the fanfares, room), a volume, its takes (several,
    dealt with no repeats; takes of one line count as one, so two calls in a row never say the same
    words, and a line's takes take turns: `lines` in the catalog) and a description that is the brief
    for making it. A sound with no takes yet plays nothing. Files go under `assets/audio/replay/`;
    loops must be OGG or WAV (MP3 pads its ends, so it can't loop seamlessly). `ReplaySoundsTest`
    holds every take to being there, in the catalog and, where it isn't the project's own, credited.
  - **Radio voices** are streamed: flight-controller calls made with VibeVoice, the radio's noise
    and the Quindar tones already in them (`assets-raw/audio/chatter/`, `CALLOUTS.md`), a call for
    every step and outcome from a pool of two lines or more (spacewalks and moonwalks four), so steps
    in a row don't say the same words. Placed in `cues.json`: a call 0.05 s after its own variant's
    reveal (the line confirms what was just seen), a later start where an opening call would hold it
    up past the wait; a few calls that tell nothing before the earliest reveal in every variant
    ("Guidance is internal", "Max-Q", "Watching it", the reentry's "We're in blackout"). A call may
    run on into the next step. **One voice at a time:** a call due while another talks waits up to
    1.5 s, and is dropped if it can't start by then or its step ends. Lines that fit no clip yet are
    in the pool, unplaced. **Unconfirmed:** the placements, the 0.7 volume, the 1.5 s wait.
  - **Playback:** a step that follows by itself lets the last one's loops end as their cues say; a
    skip or a step back fades everything but the room within 0.1 s. After the last step, the summary
    and its fanfare wait until the radio is done (a call talking or waiting for its turn), the clip
    holding its last frame, at most 8 s (user, 2026-10-15); skipping to the summary still cuts it off.
    Silent when sound is off, checked every frame. Volumes: the sound's own, times the player's
    setting (§4.12).
  - **Unconfirmed:** the blip's level (0.25), the 3 s alarm, the static's level, the default volumes (0.7), the
    fanfares' volume (0.8) and their 0.3 s fade.
  - **Placeholders (user, 2026-10-13):** a new sound plays a synthesized `placeholder-*` take until
    its real one comes (`assets-raw/replay/sounds/make_placeholder_sounds.py`). Every sound so far is
    real (Pixabay, NASA, Stable Audio 3, the user's mixes and VibeVoice takes; all credited).
### 4.8 SET Centre: engineers and payload R&D
**Purpose:** the engineering department and payload R&D (`client.screen.SetCentreScreen`,
`PayloadRndScreen`, `HireScreen`, on the shared `ChromeScreen` with the header, footer, toast and
dialogs). SET cap 7/14/20/30, from the content pack.

**4.8.1 Personnel overview** (component 11) for SET, with sub-screens Hire,
Manage department, and Payload R&D. The state badge shows "Researching ·
component · slot". "Near retirement" means within 5 years of the group's retirement age (65 for
SET, 50 for astronauts). Secondary buttons on the blue page (Hire, Manage Department, Cancel) get
a white fill: the design system's transparent secondary button disappears on the page.
Training lengths and limits come from `core`: Advanced is `ADVANCED_TRAINING_SEASONS`, and
Permanent's target slider starts one above the employee's current value in the chosen skill (the
server refuses anything else) and ends at the skill cap.

**4.8.2 Hire SET personnel** (component 14). Hiring several recruits sends one hire per recruit,
in order, and sums the outcomes up in one toast. Selection stops at the building's free places.

**4.8.3 Payload R&D** *(decided: split from rocket R&D)*. The R&D board
(component 10) for payload components: satellites, probes, capsules. Also reached
from mission configuration (§4.6.7), filtered to that mission. On both R&D boards:
- **Assigning.** The server always seats a new researcher in the component's lowest free slot,
  so only that slot shows "+ Assign", which opens the assign dialog.
- **Projected gain** (user, 2026-10-05). A lighter navy segment after the reliability bar's fill
  shows what the current team adds in an average season, and the caption says "about +4.2 next
  season" (`ResearchPlanning.expectedSeasonGain`: `ResearchProgress`'s pass with each slot's band
  midpoint; a test checks it against 4,000 simulated seasons).
- **Auto-assign** (a card button, user, 2026-10-05) seats the four best engineers for the
  component, best in the richest slot: those already on it plus anyone idle
  (`ResearchPlanning.bestTeam`). It releases whoever is in the wrong slot or not in the team, then
  assigns the rest, one action after another (`PayloadRnd.autoAssignActions`). Engineers busy
  elsewhere aren't touched.

### 4.9 Astronaut Complex
**Purpose:** the astronaut department (`client.screen.AstronautComplexScreen`, wording in
`client.view.AstronautCorps`, the shared department pieces through `Department.ASTRONAUT`).
Astronaut cap 7/14/24/35. Mission Control's sort buttons sit right of the filter chips, one per
skill, best first; clicking the active one goes back to roster order (user, 2026-10-10; shot
`ac-sorted.png`).

**4.9.1 Personnel overview** (component 11) for astronauts, showing who is on a
mission (crew or CAPCOM, both opening the mission's list), Expert stars and rocket-plane wings
(component 24), with sub-screens Hire and Manage department.

**Expert training** (decided, user, 2026-10-04) is a kind in the shared training dialog
(component 12), for astronauts and controllers (SET never). Only skills at the threshold (80+)
that aren't Expert yet can be picked; the others are greyed out, each pick showing its value with
a gold star on an Expert skill. The dialog shows the per-season chance (25 % at 80 up to 35 % at
100, from `SkillGrowth`), no skill growth, and nothing on pull-out. An idle astronaut with such a
skill gets a gold "Can train toward Expert in …" hint.

**PR duty** (`design.md` §2.2(c)):
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
  SET candidates in the pool for two seasons.", `TurnReportView.pressEffect`), and the rust. Lines
  with detail text show it under the title.
- **The Public Affairs Office** shows "PR duty: +N / season" under the prestige.

### 4.10 Public Affairs Office: budget and prestige
**Purpose:** money (`client.screen.PublicAffairsScreen`, from the hub and the footer's funds chip).
The numbers come from `core`: `BudgetProgress.costDetail` (the lines, the base and the
multiplier's saving), `BudgetProgress.forecast` (the next review: when, what it pays, what keeps
today's funding) and `PrestigeConversion`, priced with the budget schedule the game info carries
(`GameInfoResponse.budgetSchedule()`). `NavTabs` in the title row: Budget and Standing (the
standing chart, component 30, at full width; the user's idea, 2026-11-02; shots `standing-*.png`).

**Shows,** in three columns (confirmed, user, 2026-10-06): funds and running costs; prestige, the
next review and the review table; active effects and news.
- funds and the funding rate per season ("set by review #n, raised since" after a raise); "Last
  season", the latest turn report's funds change, fetched quietly (left out with no report);
- running costs broken down: salaries per department, program upkeep, component
  upkeep (active only), building upkeep per building, times any temporary cost
  multiplier from a random event;
- the net per season, and the funds runway under it (component 25);
- prestige ("PR duty: +N / season" under it), and the next funding review: "in n seasons", counted
  on the player's own seasons, every 16, as a scale of its prestige range with two markers (you,
  and what keeps today's funding; the legend names both), plus a forecast `Notice` (warning when
  funding would fall);
- captions above the last-review line: how much a credit for rivals' Firsts adds (the bracket's
  ends, its pay and the gold mark already include it, `ReviewForecast`; `design.md` §2.1(e)); what
  1,000 more prestige would add now (or that the bracket's top is reached, or, before the first
  review, that raises start then), and the raise due next season when prestige has already grown
  (`design.md` §2.1(f)). Shots `rival-firsts-pao.png`, `rival-firsts-help.png`, `raise-pao.png`;
- the bracket table, and what the current prestige would earn;
- the news log (random events only, the story in a tooltip).

**Negative values** (decided, 2026-10-04): funds in debt, here and in the footer, are a small solid
`danger` tag with white text, the `danger` token's documented rule. No separate red text colour:
one readable as red fails contrast.

**Actions:** **Convert prestige to funds** (at any time, user, 2026-11-02; the button primary in
debt, secondary otherwise, greyed out with its reason in a tooltip when none is possible; at 2
funds per prestige point, decided 2026-10-06, a part point rounded up; capped so funds end at no
more than two seasons of running costs, and a turn's conversions together at what its first could
raise, `design.md` §2.1(d)). A dialog that opens "For emergencies", with a slider from 1 to the
most allowed, showing funds, prestige and the next review's pay before and after, both limits, and
a warning when next season would still be short; in debt it splits the amount into what goes to
the debt and what's left to spend.

**States:** in debt, where new spending is refused; a temporary cost or R&D
multiplier is active (random event, n seasons left).

### 4.11 Museum: goals, history, rivals
**Purpose:** the scoreboard and the archive (`client.screen.MuseumScreen`, wording in
`client.view.Museum`, the race table in `RaceTable`, shared with the game result). Reached from the
hub's Museum building. The tabs are `NavTabs` in the header.

**Tabs:**
- **The race:** a table, fetched each time the Museum opens (it says so while loading): place,
  agency with director and archetype, one column per declared goal with the season done
  ("Fall 1960", from `GameDate`) and a First tag, done count, a **Prestige** column (each agency's
  public prestige at the last resolution, `design.md` §2.1(g)), finished turn. A filler bot
  (`design.md` §5) has "(bot)" after its agency's name here, in the Intelligence Office's rival list
  and in the game result (`Museum.name`); the newspaper names it plainly. Beside it a "This game is
  won by" card with each declared goal's badge, the goals that also credit it ("Also credited by
  …"), and the rule. "Prestige over time" above the table opens the Standing tab (§4.10). Once
  decided, a success `Notice` says who won ("decided in Fall 1960"), with "Show the result".
- **Achievements:** all the pack's goals in two columns (done first, by turn), with how each was
  reached or credited ("Credited through" names the goal actually reached, the root of the cascade),
  First in the game, and the prestige paid; open goals show their first and later prestige. Goals
  settled before goal records existed say only "Reached" or "Credited through the cascade".
- **Flights:** one card per configuration with its art and the completed flight count
  (`successfulFlightCounts`), commercial flights included and named beside it: "14 completed
  flights (12 commercial)" (user, 2026-10-27; `commercialFlightCounts`, kept apart so a client's
  flight never lowers a later flight's prestige). A configuration flown only for clients gets its
  card too; animals are honoured by the agency's own flights. A card whose configuration really
  carried animals that came home names them under a green "Came home" eyebrow once flown (Belka and
  Strelka; Ham; Enos; Chernushka, then Zvezdochka on a second flight; `design.md` §2.5(f)), each
  name, or each pair photographed together, on a row with its 48 px greyscale photo; an animal with
  no licensed photo gets an empty square in the faint navy tint so the names stay aligned.
- **Memorial:** one plaque per astronaut lost, with an empty state; no flight count (the game
  doesn't keep it). A plaque opens the person page (§4.14) where it has one. Its history note
  (Bondarenko): the chamber's air was oxygen-rich (at least 50 %), not pure oxygen, and the fire
  came ten days into a fifteen-day test.
- **Encyclopedia:** one card per explainer (component 18), with the first sheet's drawing, the
  drawing number and sheet count, the title, a one-line summary and "Read it", the gold "new" dot
  of component 19, and locked cards. An explainer opened here closes back to this tab.
- **Archive** (§4.19): the real missions' pages, unlocked by goals, and the theories (§4.18.2).

### 4.12 Settings (header gear)
`client.screen.SettingsDialog`, a `Dialog` opened by the header gear on every screen and by the
white gear at the end of the hub strip (icon-only, 32 px, like `IconButton`; decided, user,
2026-10-04, since the hub has no header).
- **Log out** needs no confirmation (today's actions are already on the server). **Switch
  server** logs out and opens Connect with the new address filled in (a bad address shows the
  Connect screen's own error under the field). Both close the connection. The account note says
  an unlaunched mission plan is dropped (plans live on the client session). The "connected" label
  is fixed text. (Log out without a confirmation, Switch server and About's contents are the
  mockup's choices, unconfirmed.)
- **Text size** (the user's request, 2026-10-26): a section above Sound, Standard or Large, for
  everyone on the device (`LaunchWindowGame.TextSize`). The layout scales with the window, so on a
  small, sharp screen (a 15" 4K laptop) its 10 to 13 px text came out tiny; Large draws every
  working style 12 % larger in the same 1920 by 1080 layout, rebuilding the fonts at once
  (`Theme.Fonts.setTextScale`). Buttons, pictures and chips keep their size; the drawings' labels
  (`DRAWING_*`) and the newspaper (`NEWS_*`) keep the design's size (user). 125 % needed about 100
  layout fixes against 112 %'s handful, so it waits. **Large is the default** (user, after trying it
  on the laptop and the desktop monitor); a player who picked Standard keeps it. Every screen is
  checked at Large by `ScreenSnapshot`'s text-fit report (`TextFit`, `CLAUDE.md`).
- **Sound:** On (the default) or Off, for everyone on the device (`LaunchWindowGame.soundOn`;
  the splash plays before anyone logs in). Two sliders under it set the replay's music (its
  fanfares) and sounds (§4.7.3, "Sound"), 70 % by default, for the device; the voices at the start
  and on joining play at full volume. **Everything clickable clicks** (the user's request,
  2026-10-31, `client.ui.ClickSound`, a capture listener on every screen's stage, so a new widget
  needs nothing): on release
  over the widget, not when disabled, dragged off or scrolling; the sounds slider sets its volume
  too, and sound Off silences it.
- **Replays:** Ask, Always or Never, per account (§4.4).
- **About:** the version, the game's content pack and hash (8 characters), the fan-project note
  (not affiliated with the original game's makers) and the font licenses. A "Credits" button
  (user, 2026-10-06) shows `assets/CREDITS.md` in the dialog (headings, paragraphs and bullets,
  scrolling past 520 px), with Back and Done. A parenthesis in a script the fonts lack (the
  archive's Russian name) is left out, since its English name stands next to it.
- **Server settings…** (the user's idea, 2026-11-13): a row under About that opens the admin's unlock step and
  the server settings (§4.22).

### 4.13 Game result
**Purpose:** announce the winner (`client.screen.GameResultDialog`). **Shows:** a `Dialog` over the
hub when a result exists, 1280 px wide: winner or tied winners (agency and director name), the turn
decided, and the race table, with a Prestige column and a colour swatch tying each agency to the
chart below it, the whole game's prestige with everyone on it, never windowed (component 30; no
funds, reviews or crews: the dialog is about everyone). Play continues, so it can be dismissed
("Keep playing", with a toast saying where to find it again) and is reopenable from the Museum's
race tab (the mockup's choices, unconfirmed).
- **Who won** comes from the game info (the server's result), never from the client. The race and
  medal tables come from the rival summaries (G3) through `core`'s `WinCondition`. The medal table
  (each tied agency's place per goal and its place counts) shows only when several agencies
  finished at the same turn, the winner first. Without the summaries (a failed fetch), it still
  names the winner and leaves the tables out.
- **When it shows:** on any hub visit while the result is unseen, unless an unread turn report
  opened at the same time (then the next hub visit shows it). "Seen" is remembered per device, like
  the report's read mark, and set by Keep playing or Open the Museum.

### 4.14 Person page *(for `design.md` §2.2(d), 2026-10-07)*
**Purpose:** everything about one employee, so the player gets to know them
(`client.screen.PersonScreen`). Opened from roster names (component 11), turn report lines,
relationship names and staffing pickers, with Back to wherever it came from. Three columns:
- **Who they are:** 128 px portrait, group and role, age and retirement line, state (idle,
  training, R&D on X, on mission Y), salary, morale (bar), a navy **loyalty** bar under it with a
  tooltip, rocket-plane wings (component 24), and the row actions of component 11 (Train, Expert
  training, Dismiss).
- **What they're like:** skills with their bars, Expert marks, learning potential and the skill
  ceiling's tick (component 31); traits (component 20), with "Still getting to know each other"
  and the colleague's hint they were hired with, or "Nothing more to discover"; relationships
  (component 22), or "None yet. A relationship shows once two people have worked side by side long
  enough to really get on, or not."
- **Their story:** the Ambition card (what, a gold "N seasons left" badge, how it comes true, when
  it was said) or a red "Disappointed" badge with the seasons left, left out for someone without
  one; then the Career record, newest first (`client.screen.CareerCards`, `client.view.CareerTexts`):
  hired (with their hint), training done, each R&D stint (the running one on top as "so far"), each
  flight or console shift, firsts, traits revealed, relationships formed, the Expert trait,
  ambitions met or missed, gold dots for traits, relationships, flights, firsts, the Expert trait
  and ambitions met.

Roster rows show "Ambition · N seasons left" (gold) or "Disappointed · N seasons" (red) under the
traits. The turn report's People section has a line for each ambition said, met, passed over,
impossible or given up. Look **unconfirmed** (People canvas).

**The memorial's page** (`MemorialPersonScreen`, from an "Open the page" link on a plaque that has
one): read-only, who they were, "Served 1959 to 1961", how it ended, skills at the end, the traits
the player knew, and the career; the portrait dimmed. Entries from before the career records keep
the plain plaque.
### 4.15 Intelligence Office *(for `design.md` §2.9, Intelligence, 2026-10-07)*
**Purpose:** the agency's intelligence service: how big it is, what it does this season, and
everything it has learned about rivals (`client.screen.IntelligenceOfficeScreen`, every line in
`client.view.IntelTexts`). An **eighth hub building** (`Hub.Place.INTELLIGENCE_OFFICE`, polygon drawn
by the user on both hub images), with a marker in the other markers' style and the `binoculars` icon
(our own drawing, `make_ui_icons.py`, on the UI Asset Sheet). All rules and numbers are in
`design.md` §2.9; the screens read them from the rules' constants (G7). Canvas `Launch Window —
Intelligence Office`; its choices are all **unconfirmed** (each row's yellow note lists them).

**Tabs** in the title row (the title naming the tab): Overview, Operations, Folders, and Foreign
hardware (below). At level 0 Operations is locked and Folders stays open, empty. A **footer chip**
with the binoculars, "L2 → 3 · 1 / 2" (level, target when it differs, actions used), "· offer 1 / 1"
with poaching (§4.16), opens the office. The budget has an "Intelligence Office" line.

**Overview** (the landing screen):
- **The level:** the current level (0 to 3, with its name: none, Security office, Analysis
  section, Field network) and the **target level** as four segments with − and + (current navy,
  target framed gold). When they differ, a gold badge: "Reaches level 3 in 2 seasons", one step per
  season; changes take effect next season.
- **Costs:** cost per season now, at the target level, and on the way there.
- **This season's actions:** "1 of 2 used", a numbered list, each used one named (a background check
  on a candidate, an assessment queued).
- **What each level brings:** the ladder top-down (3 to 0), each rung with its actions, unlocked
  operation and passives (what each level learns of a rival's failed attempt; the foreign-hardware
  chance); current solid, target dashed gold, locked rungs dimmed with a lock on what they unlock.
- **Counter-intelligence:** three pips (strength I to III) with the agents caught both ways.
- A history note card (the N1 seen from orbit, `design.md` §1.1), which gives way to a Foreign
  hardware card once there's one.
- At level 0: the service explained in three lines (own people, rival agencies, rivals' people),
  and the target stepper.

**Operations:** one card walks operation, rival, area:
- **Agency assessment** (level 2): a rival agency (name, director, archetype), then an area:
  R&D, programs and plans, or personnel. Each rival row shows the success chance: exact, with where
  the rival's level comes from and its date ("55 % · their counter-intelligence was level 2 as of
  Spring 1962"), once a dossier or a catch has shown it, otherwise "40–85 %, depending on their
  counter-intelligence". An odds card shows Succeeds, Caught (failure × catch chance) and Answer
  ("in the next turn report"), and a red box what being caught costs (−20 prestige) and teaches
  each side.
- **Personnel file** (level 3): a rival, then a group, or a person named in an earlier file.
- Queued operations this season, with Cancel until the resolution. The background check isn't
  here: it lives on the hire screen (component 14).
- Last season's results, from the latest turn report when the client has it, linked to their
  folders.

**Folders:** one per rival agency, a list on the left (each rival's level line, newest dossier and
the gold new dot; "3 failed attempts"), the open folder on the right. Column widths 320 / 680 / 500 /
312 px (personnel files need the 500):
- **Header:** agency, director, archetype, goals reached (as the Museum's race shows), and
  their Intelligence level if known, dated with its source ("Level 2 as of Spring 1962", from a
  dossier or a catch), else "Intelligence level unknown".
- **One dossier per area** (R&D, programs and plans, personnel), each dated independently: "As of
  Spring 1962" (no visible decay, the date says how old it is), a follow-up's stamp adding which
  look it was ("As of Winter 1962 · 3rd look", `IntelTexts.stamp`); an area never assessed shows a
  dashed "Never assessed". R&D shows the rival's components with reliability as ranges
  (`ReliabilityBar.range`: navy at 42 % with solid ends, the gold cap kept); it widens to about
  680 px, so the longest role and counterpart lines fit, and a long one scrolls with the page. Plans
  show open programs, missions in flight, R&D focus and hardware not flown yet (`design.md` §2.9),
  with a "may have changed since" tag; they keep plain names.
- **Personnel files:** a small card per person (portrait, name, the three best skills as ranges,
  loyalty as a band, the ambition the person has said, never one still hidden; one trait as
  component 20; "With NASA since …"), with the poaching footer (§4.16).
- **Failed attempts** (2026-11-06, `design.md` §2.9): above the tidbits in the third column, newest
  first, each dated with the turn it failed, in the turn
  report's words (`IntelTexts.failure`): "Tracking stations report that their Vostok flight
  (Uncrewed orbital test) failed on the way home, on the retrofire step.". Empty: "None seen. Every one of theirs is reported
  the season after." (below level 1, "From level 1 every one of theirs…"). A history note (component
  15) beside the title tells Vanguard, the Kosmos cover names and the N1 seen from orbit
  (`HistoryNotes.FAILURES`). `-Dshots=intel`.
- **Tidbits:** the passive finds, newest first, each dated.
- **Caught:** agents of yours this rival caught, and theirs you caught.

**Foreign hardware** (the user's idea, 2026-11-01, `design.md` §2.9; `client.view.ForeignHardware`,
`client.screen.ForeignFileScreen`, `client.ui.DashedBox` for the dashed and hatched shapes;
`-Dshots=foreign`). Every number and the counterparts await the user's review; the defaults below
are **unconfirmed**:
- **Dossier rows:** a known item's name is a link (navy, the document icon, a quiet underline),
  its role under it (at most 46 characters), then "their counterpart to your Gemini" (the name as
  the game shows it); no counterpart, no line. An unknown item is its name in plain ink with a
  dashed "not yet analysed" mark (the search icon). A component counts as the craft itself when
  it's its program's first; any other is a part (Berkut suit, Agena), showing its program's role
  and "part of their Voskhod"; with several programs needing it, the first known one speaks for it.
  A rival on the player's own side shows roles with no link and no counterparts, and its rival row
  says "Your side: its hardware is known". A counterpart that's a program links to its page; a
  rocket doesn't (rockets have no page).
- **The tab:** fourth, "FOREIGN HARDWARE", with a gold dot (square) while something is new. A
  list, newest first: the season learned, the name link with a Program or Rocket tag (and a gold
  New), the role, the counterpart, a chevron to the file; "9 known" at the top, never how many are
  left (that would give away the pool's size). Beside it how the analysts work (the chance now and
  at the next level, a quiet season means nothing) and a history note (the R-7 as the SS-6
  "Sapwood", first shown in the West at the 1967 Paris Air Show) in the existing style. Empty:
  binoculars, "Nothing learned yet", what will appear. New and the dot mean learned in the last
  resolved turn, not "since you last looked": they clear by themselves, with no per-game mark on the
  client (`review.md` 2.8 has that open).
- **The file:** a read-only program details view: a full-width card under a navy strip with faint
  diagonal hatching, a "Foreign file · read only" stamp and "Soviet hardware · analysed Spring
  1965"; art, the tag and "Their hardware", the role in bold, the counterpart linked; About is the
  long description, and each part shows its description and blueprint. On the right the history
  note (headed "The real thing"), a dashed "Theirs, not yours" box (no prices; reliability lives in
  the dossiers), and the dossiers that list it. Crumbs end in the item; the title row has "All
  foreign hardware" instead of tabs.
- **Turn report:** the item learned leads the Intelligence section, tinted as the people news are,
  with the document glyph: "Foreign hardware · Proton-K with Block D" (a link to the file), the role,
  "Their counterpart to your Titan IIIC." None on a season without one.
- **Ladder:** a row "Hardware" on every rung, "80 % a season: one piece of their hardware explained"
  (50, 25, None at level 0; `Intelligence.HARDWARE_PERCENT`). With no agency on the other side: no
  row, no card, three tabs, and dossiers drop the counterpart.
- **The words:** `assets/content/foreign-hardware.json` (outside the pack, `ForeignHardwareTest`), a
  role and a counterpart for each of the 80 programs and rocket programs (23 have no counterpart).

**Turn report:** an "Intelligence" section (`TurnReportView.intelligence`: level changes, reveals,
dossiers, tidbits, catches, rivals' failed attempts as "Failed attempt · Sever Design Bureau",
naming the season it failed in since it arrives a turn late: "…report that in Winter 1962 their
Vostok flight…"), each line linking to its folder.

### 4.16 Poaching *(for `design.md` §2.2(d), phase 5, 2026-10-08)*
**Purpose:** hiring a rival's engineer or flight controller away, and keeping your own. No new
building: offers are made from the Intelligence Office's personnel files; the defence lives with
each department and on the person page. Rules and numbers in `design.md` §2.2(d) (all unconfirmed);
the screens read them from the rules' constants (G7). Never for astronauts. Canvas `Launch Window
— Poaching`; its choices are all **unconfirmed**. Every line in `client.view.PoachTexts`; three
icons, `retain_keep`, `retain_go` and `joined`, in `make_ui_icons.py` and on the UI Asset Sheet.

**Making an offer** (Intelligence Office, Folders):
- **The personnel file card** (§4.15) has one footer: **"Make an offer"** when the person can be
  approached, else the reason (a lock for astronauts, a clock for what passes with time):
  "Astronauts can't be approached", "With the agency under 4 seasons", "Approached recently: wait
  until Summer 1963", "On a mission in flight this season", or, once they've answered your offer
  (one approach per agency per person, 2026-10-09), "You made them an offer in [date]. They won't
  hear another from you." An old file still works; its date shows. One offer a season: once used,
  the other cards show the button greyed with "Your offer this season goes to …". A queued offer
  shows the gold "Offer queued: +50 %" badge with the price if accepted and "Cancel the offer".
- **The offer dialog:** the file on the left (portrait, name, group, skills as ranges, loyalty band,
  ambition and disappointment, the known trait, the file's date stamp and today's salary, since the
  offer is priced on it); the offer on the right: the **raise** as three segments (+25 / +50 /
  +100 %), the new salary under each, then:
  - **What it costs if they come:** new salary per season, the signing bonus now, the roster room
    it needs ("SET Centre: 7 of 8"), "Nothing is paid unless they come". With the roster full the
    room line turns red with the lapse warning, and the button stays enabled.
  - **Their answer, as far as you can tell:** a range on a 0–90 % scale from the loyalty band's
    bounds plus what the file shows (+25 % at Middling with Restless gives 19–28 %; Low, disappointed
    and Steady at +100 % gives 71–90 %), with a line that the rival may match it, that you won't know
    their matching rule, and that morale and unshown traits aren't counted. Never an exact number:
    the loyalty is only a band.
  - **A history note** after the title (`HistoryNotes.POACHING`, `design.md` §1.1): both sides began
    with poached engineers, Paperclip and Osoaviakhim. `-Dshots=poaching` renders the dialog.
  - **When the answer comes:** "In the next turn report."
  - Buttons: **Make the offer · +25 %** (queues it), Cancel.
- **Queued offer:** on the Operations tab with the season's operations, marked apart (a dollar
  glyph, a light gold band, the badge) and cancellable until the resolution.
- **Title row and footer chip:** "Offer: 1 of 1 used" next to the actions (it isn't an action).

**Keeping your people** (`RetentionCards`):
- **Agency default, per group:** on the SET Centre's and Mission Control's personnel overviews
  (component 11), a full-width **Retention** strip between the department summary and the filters:
  "Match rival offers up to" with four segments (Never / +25 % / +50 % / +100 %), a line on what a
  match does (their salary rises to the offer for good) and "2 engineers set otherwise", naming
  them, with a **"Show them"** link to the **"Set otherwise"** filter chip (shown once someone is set
  otherwise). Astronaut Complex: nothing.
- **Per person:** on the person page (§4.14, its left column 440 px wide), under loyalty in the
  identity card, a **Retention** row with three segments and second lines: **Agency default**
  ("matches up to +25 %"), **Keep at any price** ("matches any offer"), **Let go** ("never
  matches"), and a caption for this person. Engineers and controllers only.
- **Roster marks** (component 11), after the name: Keep at any price a navy shield with a tick, Let
  go a muted door with an arrow out, each with a tooltip; none for the default.

**What the player learns:**
- **Turn report**, People section, offer lines first (no subheads: tags Matched navy, Left red,
  Joined green say which side): offers received ("NASA offered Ivan Kozlov +50 %: matched, salary
  now 54" / "turned it down" / "left for NASA"), and offers made ("Your offer to … : joined you" /
  "their agency matched" / "turned it down" / "lapsed: no room in the SET Centre" / "lapsed: no
  longer there"), the line naming the signing bonus. The People tile counts who came and who was
  hired away. A line opens the page of the person it's about while they're with the agency (one of
  ours who stayed, or someone who came over; never someone who left, and never the rival's person,
  whose id means nothing here; `PoachTexts.pageOf`).
- **Person page:** "Joined from NASA" as a green tag under the age line (and on the roster), the one
  trait the file showed; career lines "Joined from …", "Turned down an offer from …", "Stayed: the
  agency matched …'s offer", dots green for joined, navy with a gold ring for stayed or turned down,
  lines from the old agency prefixed "At NASA:" (`CareerCards`).

### 4.17 Contracts *(for `design.md` §2.11, 2026-10-11)*
**Purpose:** the commercial marketplace: see what clients offer, apply for one contract a turn,
follow the active contract (`screen.ContractsScreen` with `view.ContractsView`). Contracts pay
money, not prestige. Rules and every number in `design.md` §2.11 (all unconfirmed); the screen
reads them from the rules' constants (G7). One name for both archetypes; the Soviet clients are
ministries and institutes, which only changes the text. Canvas `Launch Window — Contracts`; its
choices are all **unconfirmed**. No new tokens and no new `Theme` components: the badge, listing
card, switch and stepper are built in `ContractsScreen` and `ContractMarker` from the existing
tokens. Shots `contracts-*.png` (`-Dshots=contracts`).

**Where:** a fourth card on Headquarters' landing screen (§4.6), **Contracts** ("Open the board"):
its eyebrow counts the board, a state row above its button (gold-tinted with a dot for new offers,
plain for applied and active): "4 on offer · 2 you can fly · Applied: Transatlantic relay", or
"Active: Transatlantic relay · launch by Autumn 1963". The breadcrumb is "Space Complex ›
Headquarters › Contracts". Headquarters' hub marker gets the gold dot (component 26's, one dot for
every reason, each line on hover) when a listing the agency qualifies for is new this turn and it
hasn't applied anywhere, hover "New contracts on offer", and while an awarded contract has no
mission launched for it, hover "Contract to launch: <title> · 2 seasons left" (the user's idea,
2026-10-31; a mission called off before it flies brings it back). A help topic "Contracts"
(component 23, its numbers as `contracts.*` tokens).

**Layout, top to bottom:**
- **Your contract** card. With an active contract: the category art (whole at 220 px wide), "Tier
  1 of 3" as a gold badge, the client and line, the fee, the launch cost now and the window
  ("Launch by Autumn 1963 · 2 seasons left", red in the last) as three figures, then a stepper of
  its state (awarded, scheduled, flying, paid). The button follows the state: **Plan the flight**
  (the contract configuration's mission plan on its assessment tab, §4.6.7, badged, component 27),
  Open the mission plan (Mission Control's scheduled missions), Follow it in flight (the active
  missions). Without one: "No active contract. Apply for one below; it's awarded when the turn
  resolves.", three short rules and "Help: Contracts".
- **Your record**: a table, one row per unlocked line (line, flown, failed, lapsed, the record's
  points, +3 / −6 within ±15, and **your score now**, the weakest component's reliability plus the
  record, a thin rule under it). Its tooltip breaks it down ("Weakest: Jupiter-C, 72 % · Record +6 ·
  Score 78", "Crew and controllers don't count"). A line whose program is closed shows "Open
  Mercury" as its score. Locked lines aren't listed; a line with no record yet reads "No contracts
  flown yet".
- **On offer** (the board): "On offer · 4 · 3 you can fly" over it with a switch **"Only what I can
  fly"** on the right (off by default, remembered while the client runs; its track square), four
  cards a row in a scroll pane, the ones the agency can fly first. Each card:
  - the line's category art, whole at the card's width (the user's report, 2026-10-11: a cropped
    banner cut the spacecraft off), the line as a navy chip on it, the client as an eyebrow, the
    title, the story (two or three lines);
  - the terms: three tier boxes "+60 % · +40 % · +25 %" with **your fee at each** ("2,340 · 2,048 ·
    1,829") for the contract configuration with the cheapest rocket you could use, "Your fee at each
    tier · launch cost now"; a crewed flight's fee includes the crew premium a seat (2026-11-05),
    which the card's rules and the apply confirmation name;
  - the flight it takes ("Flies: Explorer 1 · Orbital flight"), with the component 27 icon;
  - "Posted Spring 1962 · expires in 2 turns";
  - a foot with "Applications are sealed" and **Apply**, or the reason you can't in place of the
    button (icons lock, open_programs, document): "Needs the Earth-orbit satellites line: fly
    Explorer 1's marked flights", "Open Explorer 1 to fly it", "You have an active contract". A
    listing the agency can't fly is **dimmed** (greyscale art, muted text) whatever else holds; one
    it can fly but not apply for (an active contract) keeps its look with the reason
    (`Contracts.check` asks about flying before the active contract). Never how many have applied.
- **Applied:** a 1.5 px navy border, a 3 px gold top accent, a gold "Applied" badge, "Your
  application · decided when the turn resolves" and a red **Withdraw**. Applying elsewhere moves it,
  after a confirmation ("Move your application from Transatlantic relay to Weather picture
  service?", buttons "Keep Transatlantic relay" and "Move it").
- **Two programs in a line:** Apply first asks which, each flight with its rocket, launch cost,
  fees and score, then "Next: the terms".

**The apply confirmation** (component 5) states the terms once: the fee by tier and the score now,
"Tiers go to the best record; a fourth applicant gets nothing", "Pays money only: no prestige, not
a repeat", "If the flight fails, the insurer refunds the launch cost and you lose the prestige that
failure costs", "Launch within 3 seasons or it lapses: −10 prestige, and it counts as a failure".

**Turn report** (§4.4): a **Contracts** card and strip cell (an eighth beside a waiting decision's
seventh), a 3 px left accent per outcome (gold awarded, green paid, navy refunded, red lapsed, muted
missed out): awarded (client, tier, fee, window, link to the board), missed out ("Agencies with a
better record won it", never how many applied), a contract flight's payout (with the profit and the
record's change) or the insurer's refund, a lapse, and "Came to nothing" for an application whose
program closed. The budget card has a Commercial contracts line. The news gets the public line for
every award, winners alphabetically: "Transatlantic relay goes to Orbital Corp. and Pan-Am Space.",
plus a line for new listings.

**Empty board:** "No contracts on offer. Clients come once an agency has flown every marked flight
of a program: look for the contract icon on its flights." With a line unlocked but no listing
(rare): "No contracts in your lines this turn. New ones are posted when a turn resolves."

**Worth a look in `design.md` §2.11:** the fee follows the launch cost, so of two programs in a line
the dearer one pays more profit at the same margin.
### 4.18 Front page *(the user's idea, 2026-10-09; built 2026-10-11)*
**Purpose:** when an agency makes one of the great Firsts, every player in the game sees it the way
the world did in the 1960s: on the front page of a newspaper, *The Daily Zenith*. It's the first
thing they see on their next login, before the turn report; when the player watches the missions
first (§4.4), the pages come after the replays, so a First isn't printed before its film (user,
2026-10-12). It's news, not a rules screen: the game facts go in one line under the page. Mocked up
in Photocraft, not Claude Design (`assets-raw/frontpage/mockup/`, `frontpage_mockup.pcraft` with
every layer named; its fonts are stand-ins). Server side: G14.

**The Firsts** are listed in content, not code (`front-pages.json`, below), in race order. The standard
pack's five: `earth-orbit-satellite`, `human-in-space`, `human-in-orbit`, `crewed-lunar-flyby`,
`crewed-lunar-landing`. A pack shows the ones whose goals it has (early-era: the first three).
*Unconfirmed:* spacewalk, docking and a two-crew flight aren't Firsts with a front page; they're in the
turn report as usual.

**When a page is printed:** in the resolution where a First's goal is reached for the first time in the
game (the cross-player goal race, `design.md` §2.6: nobody held it at the start of the turn), reached or
credited through the cascade alike.
- **Ties:** several agencies reaching the same First in one resolution all get the first-time prestige,
  so they share one page, from a tie story; the race box lists both.
- **Several Firsts in one resolution:** one page per First. When one flight earns two Firsts (a landing
  credits the flyby through the cascade; a first orbit credits human in space and the satellite), the
  latest First's page is printed and the others are folded into it: their lines in the race box carry
  the same season, and the article's `also` paragraph mentions it. *(Unconfirmed: one page is the better
  morning; two pages for one flight would read as a repeat.)*

**When it shows:**
- **On login,** after Join game and before the turn report: every page this player hasn't seen, oldest
  first, one at a time ("1 of 2").
- **While logged in,** when a turn resolves: the new pages show over the current screen and come back to
  it, then the toast as before.
- **Seen** is remembered per device (preferences per server and login, like the report's read mark),
  per page id. Pages printed before the turn a player joined in count as seen (from their own season
  count, `ClientSession.joinedTurn`); they're in the Museum.
- **Reopen:** the Museum's race tab First tags (declared goals only), the Achievements tab's "First in
  the game" tag and the turn report's "First in the game!" chip open the page, alone, with a tooltip
  saying so and "Close" in the strip. The turn report's "Read the paper" reopens a turn's pages (§4.4).

**Layout** (1920 x 1080). *Unconfirmed:* a full portrait page would put the body text near 10 px; the
page is the top half of a broadsheet, "above the fold", landscape:
- **Backdrop:** the hub's art for the current season and time of day, or, if that isn't loaded yet,
  the page frame's blueprint texture, under a navy scrim (`SCRIM_DEEP`, `#0d1b33` at 80 %), so the
  player knows where they are. Checked on the brightest and darkest variants (shots
  `frontpage-winter-noon.png`, `frontpage-winter-night.png`).
- **The page:** 1440 x 900 at (240, 40), the paper art (`paper.jpg`) with a soft drop shadow. Content
  inside a 40 px margin (x 280 to 1640). All type on the page is `Theme.INK`.
- **Top band:** two ear boxes (230 x 84, 1 px rule) left and right of the masthead. Left: "LATE CITY
  EDITION" and a weather line ("Fair and cool tonight"; a short list per season, picked with the
  page's seed). Right: "EXTRA", letter-spaced, and "Special edition" in italics. The masthead
  (`masthead.png`, drawn at 761 x 108) centered between them.
- **Folio line:** a 4 px rule and a 1 px rule under it, then "VOL. <n> · No. <turn>" left (the volume
  is the game year counted from the start year, so Spring 1961 from a Winter 1955 start is VOL. VII;
  the number is the game turn), the season letter-spaced in the middle ("SPRING 1961"), "FIVE CENTS"
  right, a 1 px rule under it.
- **Headline:** the story's headline in capitals, centered: one line at 80 px, else one line at 64,
  else two lines at 64, then the short headline (`headlineShort`) the same way. Old Standard is wide,
  so a headline with an agency's name in it usually takes two lines at 64; the lower half gives up
  that height, and the photo is cropped about its middle to keep its width.
- **Deck:** the subheadline, italic 25 px, centered, then a 1 px rule across.
- **Lower half, three columns:** the photo (640 x 440, `ui/frontpage/<first-id>.jpg`) at the left with
  its caption under it (italic 14 px, two lines at most); then two text columns of 336 px, 24 px apart,
  each with a 1 px column rule on its left. Column one: the byline ("FROM OUR SPACE CORRESPONDENT",
  12 px, letter-spaced), the article's opening paragraphs, and an "IN BRIEF" box at its foot (two
  teaser lines in italics). Column two: the rest of the article and, at its foot, the race box.
- **Body type:** 16 px on 22 px, ragged-right (a libGDX label can't justify), a four-space first-line
  indent (the fonts have no em space), 4 px between paragraphs. The first column fills before the
  second, paragraphs whole; while the second overflows, closing paragraphs are dropped from the end.
  `FrontPagesTest` holds every article to 26 lines at the longest names allowed, measured with the
  page's font.
- **The race box** ("THE RACE SO FAR"): a 1 px box, one row per First in the pack: the First's name
  (from `strings.json`) left, who made it and when right in italics ("Kestrel Space Directorate ·
  Fall 1957"; ties "A and B · Spring 1961"; a row that doesn't fit goes on a second line,
  right-aligned), "still open" for the rest. It shows the race as of this page's edition, never later
  Firsts, also when reopened.
- **The strip** under the page (y 960 to 1040, on the dimmed hub, `Theme.ON_PAGE` text): the game line
  left, then "1 of 2" (muted) and the white page button right (the design system has no gold button):
  "Next front page", or on the last page "Continue" (to the turn report if one is unread, else the
  hub). Reopened: "Close".
- **The game line:**
  - the First was this player's: "Your agency got there first: <goal> paid <n> prestige.";
  - a tie with this player: "You share this First with <agency>: both got <n> prestige.";
  - a rival's: "<agency> got there first. <goal> now pays <n> prestige to whoever follows." (the
    second-time value).

**Words** (`assets/content/front-pages.json`, `content.FrontPages`, outside the frozen pack like
`event-stories.json`, so corrections reach running games; our own text, written from the real Firsts'
facts, never from a real paper's page):
- Per First, keyed by goal id: its photo file, its caption, the two "In brief" lines, at least **two
  stories** and a separate `tie` story (its own text, since "A and B" breaks the verbs of a one-agency
  story). A story is `headline`, `headlineShort`, `deck`, `lead`, `also` (the folded-First paragraph,
  used only when one is folded in), `rivals2`, `rivals1` (paragraphs needing two or one rivals,
  dropped in a game with fewer) and `close`, read in that order.
- **Tokens,** only what the server prints (G14): `{agency}`, `{director}`, `{season}`, `{crew}` (the
  flight's crew by name, "Daniel Hale" or "Daniel Hale, Rosa Ortiz and Pavel Brandt"), `{commander}`
  (the first of them), `{rival1}`, `{rival2}`, `{also}` (the folded Firsts' names, in quotes). In a tie
  `{crew}`, `{director}` and `{commander}` list everyone's. No hardware: the page never names the
  rocket, the capsule or the program, which the Intelligence Office sells (`design.md` §2.9). Fixed
  facts about the First itself (orbital speed, the distance to the Moon) are plain text, checked like a
  history note.
- **The pick:** a page's story is drawn with the game's seed salted by the goal id, so every player
  reads the same page, and it never touches the season's random stream.
- Ids (story ids, the First keys) are stored in printed pages: never rename or reuse one.

**Type** (SIL Open Font License, credited in `assets/CREDITS.md`, generated in `Theme.Fonts`):
**Old Standard TT** Bold for the headline, ears and box titles (a 19th-century newspaper face),
**Libre Caslon Text** Regular and Italic for the deck, body, caption and race box (static instances at
weight 400, cut from the variable fonts); the `NEWS_*` text styles, the headline styles generated with
capitals only (`Theme.CAPITALS`). *Unconfirmed.* The masthead is art, so it needs no font.

**Component art** (`assets/ui/frontpage/`, from `assets-raw/frontpage/`, CLAUDE.md "Tools outside the
repo"): `paper.jpg` (1440 x 900), `masthead.png` (ink on transparent, 1269 x 180, drawn at 60 %), one
press photo per First (1024 x 704, printed in ink on the paper's colour, drawn at 640 x 440). The first human in
orbit's is the one with a face, so it comes in 12 versions (the user's idea and picks, 2026-10-31,
`ui/frontpage/human-in-orbit-<sex>-<look>.jpg`), chosen by the commander's portrait (`PilotPhoto`, kept on the page as
`Achiever.commanderPortraitId`): six broad looks, since 31 ethnicities can't be told apart on newsprint. A tie shows
the first agency's pilot, and a page without a portrait the plain photo. All
generated with Qwen Image (ComfyUI), fixed seeds; the picks are in `picks.json`.

**States:** one page; several ("1 of 3"); a tie; a solo game (no rival paragraph, no rivals in the race
box beyond this player); the early-era pack (three rows in the race box); reopened from the Museum;
the fetch failed (no page, straight to the report: a page is never worth blocking the login).

**Built in libGDX** (2026-10-11) as `client.screen.FrontPageScreen`, its words in
`client.view.FrontPageView`. Shots `frontpage-*.png` (`-Dshots=frontpage`). Its choices, all
**unconfirmed**, are the words file's shape, the headline sizes, the ragged-right body, the column
flow and the white strip button above, and how it shows: on login the hub fetches the pages; unseen
ones replace it, then a fresh hub opens the unread report as before. If the server hasn't answered
after 4 seconds the report opens without them (a late answer is dropped; the pages wait for the next
login), and a player who leaves the hub before the answer gets neither until then. Dev mode's "Resolve
turn now" shows them before the report. The page screen never refreshes itself. A failed fetch shows
nothing.

#### 4.18.1 The world reacts *(the user's idea, 2026-10-20; 2026-10-22, G17)*
**Purpose:** a First is news for the winner and a moment for everyone else. For one turn after a First's
page is printed, every rival may give the press a statement about it (component 28); at the next
resolution a follow-up page, "The World Reacts", prints them all as one article (`rules.ReactionPrinter`).
Flavour only: no money, prestige or anything else changes. Canvas `Launch Window — The World Reacts`.
- **Who may react:** every agency that had a turn report for the First's turn (late joiners are out,
  unconfirmed) and didn't make the First (in a tie, none of the achievers). One statement per First's
  page; several Firsts printed together each take their own. The last statement sent before the turn
  resolves counts; it can be withdrawn. A late statement is refused (`ReactionStore.seal`).
- **A statement:** one of five stances (enthusiastic, polite, "we'll be next", downplay, no comment) and an
  optional quote of up to 140 characters, printed word for word inside curly quotes. No LLM (user,
  2026-10-20); one could rewrite later, as a layer on top.
- **The article reads as a story, not a list** (user, 2026-10-22):
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
- **Lower half:** four text columns of 322 px with 1 px rules between, the photo (322 x 221) and its
  caption over column one, then the byline; the race box at the foot of column four. The article runs
  from column to column and a paragraph is split between words where a column ends (at least two lines
  stay behind; the rest goes on unindented), ragged-right as on a First's page.
- **Short articles** (the user's idea, 2026-10-22): the room a short article leaves is filled with short local
  items, as a paper fills its columns: `fillers` in `front-page-reactions.json` (our own fiction, at least 20
  (`FrontPageReactionsTest`), at most 9 lines each, true before anyone has flown), a headline
  in `NEWS_LABEL` capitals over Caslon body, a short centred rule (64 px) between items. They go into the column the
  article ends in and every one after it, each at most once a page. None on a page that runs on.
  **No repeats** (user, 2026-10-23, `client.view.FillerDeck`): the client keeps, per server and login, the items shown
  since the last had been and offers those not yet shown first, in an order drawn from the page's id; once all have
  been, the deck starts over. A page keeps the items it first showed when it opens again. Since every player sees the
  same pages in the same order, they read the same items, except a late joiner or a fresh install.
- **History notes** (component 15): on the follow-up's strip after "Statements only: no effect on the
  race." (the icon in `ON_PAGE`), how rivals honoured each other (Apollo 11's medals for Gagarin and
  Komarov); and on the statement dialog's title. Their words are `historyNotes` in
  `front-page-reactions.json`, checked in `design.md` §1.1.
- **When it doesn't fit:** the closing paragraphs go first; what still doesn't fit runs on to a continuation
  sheet: "Continued on page 2" at the foot of column four, "Turn to page 2" on the strip's button, then a
  sheet with a running head ("THE WORLD REACTS · CONTINUED FROM PAGE 1 · <season>") and four full columns
  (user, 2026-10-22: no scroll area). The fullest article the printer makes (4 told at 140 characters, the
  longest names) still fits one page; `FrontPageReactionsTest` holds the words to 64 lines at 316 px,
  unsplit.
- **Words:** `assets/content/front-page-reactions.json` (outside the pack, not hashed;
  `FrontPageReactionsTest`): per First its stories per mood, its detail clauses and closes; per stance its
  quoted, plain and "also" lines; the linking sentences; the declined lines. Every piece has an id that
  printed pages store: never rename or reuse one. Tokens: `{agency}` and `{director}` (the First's
  makers), `{season}`, `{rival}`, `{rivalDirector}`, `{detail}`, `{quote}` (only at a line's end), `{rivals}`.

---

#### 4.18.2 Theories of the day *(the user's idea, 2026-10-22; built straight in libGDX, no mockup, user; G18)*
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
  A theory can also be raised by the player's first opening of a program in one of its
  `raisedByCategories` (user, 2026-10-27): the radiation belts by the first satellite program
  (`earth-orbit-satellites`), since their goal, the first satellite, raised and settled them in the same
  report. The goal still raises them in games whose openings have no recorded season.
  `PlayerSnapshot.programOpenedSeasons` keeps the agency's season of each program's first opening
  (`ProgramOpening`), and `FrontPageView.raiseTurns` turns it into game turns.
- **Settled** (shared): the server prints a page of kind `SCIENCE` (`rules.SciencePrinter`, in
  `printFrontPages`, after the Firsts) the first time anyone reaches one of a theory's `answeredBy` goals; once
  per theory; agencies settling it in the same turn share it. A public one shows like any unseen page, in the
  paper; every report of that turn lists the theories settled in its News card ("Settled: <question>").
  `FrontPage.firstPage()` means a First's page; never `!followUp()`.
- **The science page:** the follow-up's frame (§4.18.1) with the right ear "SCIENCE / REPORT", no photo; column
  one opens with a ruled box where a First's photo would be, "THE QUESTION": the question, then how it
  stood for the reader's agency, in the race box's rows: "Raised by your staff" (the season, or "not by then"),
  "Settled" and "Open for" ("1 year", "2 years, 1 season"; `FrontPageView.standing`), so every game's page
  differs. Then the byline "FROM OUR SCIENCE CORRESPONDENT", the article and the filler items, the race box; the
  strip "Science news: no effect on the race." with the theory's history note. An article runs 5 to 8 paragraphs,
  32 to 48 lines at the longest names (`TheoriesTest`), so it fills about two and a half columns and the filler
  items only the rest; one that still runs over goes on to the continuation sheet, headed "SCIENCE REPORT".
- **In the Museum:** the Archive tab's "THEORIES OF THE DAY" card, under the findings card, lists the
  theories this agency's scientists raised and those a flight settled (never one neither, so it spoils no
  checkpoint; `FrontPageView.museum`). Each row: the question with its history note, "What would settle it" or the
  answer and who settled it, when your scientists raised it; on the right the status ("OPEN QUESTION" or
  "SETTLED · <season>") over the links: "Read the page" for a public theory (reopens the science page, "Close"),
  and a button per archive page about the flights that settled it, once settled and open in this game
  (`theories.json`'s `archive` ids, checked by `TheoriesTest`; the Moon germs have none: no archive page covers
  the quarantine). The report's "Settled:" line for a public theory opens its page too.
- An explainer that answers a theory stays locked until it's settled (component 18).
- Shots `-Dshots=theories`.

#### 4.18.3 In memory *(the user's idea, 2026-10-22; G19)*
**Purpose:** a lost crew is news for the whole world, and the agency that lost it owes the world a statement.
The paper prints a memorial page for every crew lost in flight, a turn after the loss, with the agency's
statement in the tone its director chose and the other agencies' condolences (`rules.MemorialPrinter`). It
gives the moment the gravity it deserves, and confronts the player with a choice about how to speak of the
dead (user). Flavour only: no money, prestige or anything else changes (the rules' cost of a loss is the
grounding, `design.md` §2.5(j), and the withheld goals, §2.6). Canvas `Launch Window — In Memory`; its
choices **unconfirmed**.
- **What triggers it:** a catastrophic step that loses a crewed flight's crew, as for the grounding
  (`MissionProgress.loseCrew`). Not an astronaut who perishes in a random event, nor an animal passenger
  (unconfirmed). One page per lost crew; two crews lost in one turn print two pages.
- **When, a turn late** (user, 2026-10-22): nothing is printed the turn of the loss, so no page spoils the
  replay at login. The losing player learns it from the replay and the report.
  - *Turn T resolves:* the crew is lost (recorded in `losses/turn-N.json`).
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
- **The page:** the paper's frame (§4.18) in mourning: a 3 px ink rule 12 px inside the sheet, the right ear
  "IN MEMORY" on one line at 26 px, no race box, no filler items, no photo of a launch. Under the headline and
  deck, between 1 px rules, the crew's portraits in a row (128 px, greyscale copies made at runtime from the
  colour portraits, `UiKit.greyPicture`, with a hairline), each with the name (Caslon 17) and "Aged 38 · 2
  flights" (Caslon italic 14). Then the article, in the statement's tone (2 or 3 stories a tone, picked by the
  page's id):
  - *The lead:* what happened, in plain words: the flight and the step it was lost on ("lost during
    reentry"). Places (the headline's "on the way home", the lead's "during reentry") come from the step's id
    (`memorial-pages.json`'s `places`, in order; "in flight" when none matches).
  - *The statement,* told in its tone, with the quote.
  - *The people:* a sentence each from what the game knows, never anything hidden (`MemorialView.person`):
    for "Who they were", a revealed trait's line, else the years served; for the other tones, the flights
    ("was flying for the first time", "was on a second flight").
  - *What they nearly did:* when the flight had reached a goal the loss withheld (`GoalsWithheld`): "They had
    become the first to walk in space, and did not come home." The goal stays unclaimed (§2.6). The firsts
    worth a sentence are a ranked list (`firsts`): a withheld goal not on it (a program's own "flown" goals)
    gets none.
  - *The stand-down:* "<Agency> has stood down its crewed flights while the accident is investigated."
  - *From the other agencies:* a small capital head between rules (Old Standard Bold 12), the condolences,
    quotes first, each rival's line its own paragraph; none when nobody sent any (the article simply ends).
  - *A close* per tone.
- **Columns:** a short article (under 24 lines) runs in two centred columns (692 px), then a centred 64 px
  rule, the paper below left empty; a long one in four columns of an even height found by halving, so they
  end together. A page with the director's words and four rivals' at 140 characters runs on to a
  continuation sheet in the same mourning frame, "Continued on page 2" at the foot of column four
  (`-Dshots=memorial`'s crowded page).
- **The strip:** "In memory." followed by a history note (the Fallen Astronaut, below), and Continue. The losing
  agency's copy adds "Open the memorial", a ghost button left of Continue, visiting the first of the crew's
  memorial pages. The page opens again from each lost person's memorial page ("Read the paper").
- **The hub's card** (component 29) sits top left under the strip (420 px), over the art; it covers part of
  the VAB's marker.
- **When the grounding ends** (`CrewedGrounding`): a "<Agency> resumes crewed flights" news line was
  proposed (unconfirmed); not built, since a second loss restarts the count.
- **History notes** (component 15; checked in `design.md` §1.1): on the page's strip, the Fallen Astronaut
  left on the Moon by Apollo 15 with the names of eight Americans and six Soviets; on the statement dialog,
  Gene Kranz's "tough and competent" talk to his controllers after the Apollo 1 fire; on the condolence form,
  Tom Stafford at the Soyuz 11 funeral, representing the President, a pallbearer.
- **Words:** `assets/content/memorial-pages.json` (outside the pack, `MemorialPagesTest`):
  per tone its stories (headline, deck, lead, the statement's lines with and without a quote, the people's
  sentences, close), the withheld-goal sentences per goal kind, the condolence lines, the spokesman's
  statement. Ids are stored on printed pages: never rename or reuse one. Tokens as the reactions' (`{agency}`,
  `{director}`, `{season}`, `{quote}`), plus `{mission}`, `{step}`, `{crew}`, `{name}`, `{age}`, `{flights}`.
- **Later** (the user's call, not specified): secrecy, a Soviet agency keeping a loss quiet at the risk of
  rivals' intelligence uncovering it later (Bondarenko's death in 1961 was not revealed until 1986).

### 4.19 Archive *(the user's idea, 2026-10-15, for `design.md` §2.12)*
**Purpose:** pages about the real missions that sent back something memorable, unlocked when a goal is
first reached in the game, so a flight feels like getting its pictures back (`client.screen.ArchivePageScreen`,
the Museum's `Tab.ARCHIVE`, `TurnReportScreen.archive`, `client.view.Archive`; the words in
`assets/content/archive.json`). The pages, images and rights are in `assets-raw/archive/PAGES.md`. Server
side: G15. Canvas `Launch Window — Archive`; its choices are all **unconfirmed**. `-Dshots=archive`.

**The Museum's Archive tab** (§4.11), the sixth, after Encyclopedia:
- **The header line:** "5 of 12 in the archive · 2 new" (every page open once the race is decided).
- **The list:** one card per page, in the order of the real missions' dates (1958 to 1990), so it reads
  like a timeline: a grid under a timeline rail, the year and a dot per card (white in the archive, gold
  new, hollow locked).
- **An unlocked card:** the Encyclopedia card's frame with a 158 px cropped thumbnail of its first image,
  the eyebrow (the real date and agency), the title ("Apollo 8: Earthrise"), a one-line summary, "With
  sound" where there is one, and the gold "new" dot (component 19) until it's opened. The whole card opens
  the page.
- **A locked card:** its eyebrow and title muted on a blue-grey card, the lock icon in a blank frame in the
  faint navy tint (like the Flights tab's missing photos), and "Unlocks with: People in lunar orbit", the
  goal's name (user, 2026-10-15), at the foot.
- No gold dot on the Archive tab itself (*proposed*; `NavTabs` has none).

**The page** (opened from the tab or the turn report; it closes back to where it was opened), laid out
like an encyclopedia article, three columns at 1920:
- **Header:** the title, an eyebrow with the agency and date ("NASA · December 1968"), and under the title
  when it unlocked in this game ("In the archive since Spring 1967, flown by you" / "by a rival" / "opened
  when the race was decided").
- **The text** (676 px): three to five short sections with headings (the mission, what it found, why it
  mattered), then numbered Sources. Text styles `ARTICLE` (Work Sans 14 with its own line spacing: the
  client applies no style's line height) and `ARTICLE_HEADING` (Archivo 700 17 px navy).
- **The gallery** beside it: three to five images in 5:4 crops in two columns, each with a caption and its
  credit line under it in caption style; with an odd count the first spans both, keeping its own shape
  between square and 3:1 (so a whole disc, Luna 3, isn't cut). Clicking an image opens the viewer.
- **The fact box** (440 px): a navy head, label and value rows (craft, launch vehicle, launch and end
  dates, crew or none), the mission's Firsts with gold squares, and a footer line naming the goal that
  unlocked the page. Under it, the audio player, if the page has a sound: a 40 px navy square play/pause
  button, a 4 px track with a knob, the time, and the caption with its credit ("Genesis reading, 24
  December 1968. NASA."); playing shows "PLAYING" in gold-text.
- **The viewer:** the whole picture as large as fits (uncropped, so Apollo's frames show square) over a
  near-opaque navy scrim (`SCRIM_VIEWER`), caption and credit under it, "2 OF 4" top left, the close button
  with "ESC" top right, 48 px square arrow buttons (no arrow hints: the fonts have no arrow glyphs) or the
  arrow keys to move through the page's images, at either end that side's arrow hidden; Escape or a click
  outside closes.
- **Audio:** plays on the music volume (Settings), needs Sound On, and stops when the page closes. Any
  replay sound stops while it plays (*proposed*).

**Turn report** (§4.4): a "New in the archive" card the turn a page unlocks, on every player's report, in
the left column right after Goals, with a navy line and no cell in the summary strip: each new page's
112 × 72 thumbnail, the eyebrow, the title, "Flown by you" (success, with the check) or "Flown by a rival"
(muted, the rival unnamed), the goal and a primary Open. **When the race is decided** (`design.md` §2.12):
that turn's report opens with a full-width card with the gold accent, like the Decision card: "THE ARCHIVE
· ALL 12 PAGES OPEN", "The race is decided, and the whole archive is open", how many pages opened, and
"Open the archive" (to the tab); the pages opened that way get no rows in "New in the archive".

**Theme:** `SCRIM_VIEWER`, `ARTICLE`, `ARTICLE_HEADING`, `ARCHIVE_CARD_TITLE`. No new icons.

### 4.20 Findings *(the user's idea, 2026-10-19, `design.md` §2.13; built straight in libGDX, no mockup, user)*
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
- **The Museum's Archive tab, a "YOUR FINDINGS" card** above the timeline while the agency knows any: one
  row per finding known (the pack's order), its name, "Makes safer: …" and its controls. The place to
  publish later, and the only place for a finding whose page isn't written yet. The tab's line adds "a
  finding's page, for those who know it". The theories of the day's card sits under it (§4.18.2).
- **Archive cards and pages of a finding:** a locked card says "A finding: opens when you know" where others
  say "Unlocks with:"; the page's top line says "published" where others say "flown by a rival", and carries
  "A finding. Safer for those who know it: …" with the controls on the right. The report's "New in the
  archive" row says "Published" for a finding a rival published.
- **The mission plan's odds** (§4.6.7): a step a known finding makes safer gets a green outlined chip with
  its points ("+4", "+8" for two) beside its name, with the findings (and rocket-plane wings, component 24)
  in a tooltip; the table's caption says so.
- **Words** come from `client.view.FindingsView`: step kinds fold into a few names ("lunar-landing" and
  "lk-descent" read as landing and powered descent), and the body reads "around Earth" or "on flights to
  Mars".
- **Help:** a "Findings" topic (Rivals), on the mission plan, the turn report and the Museum.
  `-Dshots=findings` renders the report, the dialog, the tab, a page, the help and the plan.

### 4.21 Roads to the Moon *(the user's idea, 2026-10-26; canvas `Launch Window — Roads to the Moon`)*
**Purpose:** help the player choose how to land on the Moon (`client.screen.RoadsToMoonScreen`, the figures in
`client.view.RoadsToMoon`, the words in `assets/content/moon-roads.json` read by `client.view.MoonRoads`; shots
`roads-*.png`, `-Dshots=roads`). NASA has three roads that land (Apollo, Gemini EOR, Gemini direct ascent) and
the Soviets two (Soyuz 7K-L3, LK-700); each is a Moon program plus the Earth-orbit tests that make its Moon
flights free to open (`design.md` §2.3(d)). The screen sets the roads side by side with what each costs, needs
and asks of you, and the trade-offs in plain words. **It never ranks them** (user, 2026-10-26: inform, don't
shoehorn): no "recommended", no "fastest", no measured season counts. Apollo and the 7K-L3 carry a quiet
"The historical choice" tag, nothing more. The roads are measured close to each other (`design.md` §2.3(e)), so
the facts and the trade-off lines carry the choice.

**Where:** a page button "Compare the roads to the Moon" in the header of a body's category that holds a road
(Lunar expeditions), also while it's locked; a secondary button on every road program's and its tests' details
page (§4.6.4); the advisor's "Choose your road to the Moon" (component 24, Go link "Compare the roads").
Breadcrumb "Space Complex › Headquarters › Roads to the Moon". Help context `ROADS_TO_THE_MOON` and topic
"Roads to the Moon" (component 23).

**Layout, top to bottom** (1920 × 1080, the usual chrome):
- **Title** "Roads to the Moon", and one line under it: "Every road here lands on the Moon. They differ in what
  you build, which rocket you need and how many flights it takes to get there." A concept chip for the explainer
  "Three ways to the Moon" (E-20, component 18/19) at the end of the line.
- **The roads**, one column per road (three for NASA, two for the Soviets), the same rows in each so they read
  across; a narrow label column at the left names the rows. Each column:
  - **Head:** the program's picture, its name, the mode as an outlined tag with its **mode glyph** (icons
    `mode_direct`, `mode_eor`, `mode_lor` in `make_ui_icons.py`: the Earth, the Moon, one path; a ring round the
    Earth or the Moon where the craft meet), and "The historical choice" (`CAPTION_ITALIC`, `NAVY_BORDER`
    outline) where it applies. A road the player has opened is marked **Your road** (`BadgeKind.NAVY`: gold
    would read as in progress), never highlighted otherwise.
  - **How it works:** one sentence (from `moon-roads.json`).
  - **To start now:** the opening price for this player, after discounts, split in two lines: "Earth-orbit tests
    22,012" and "Moon flights: free once the tests are open"; then "Parts to build: 23,400" (the creation cost of the
    components not yet owned) and **Total 45,412**, money in the game's own format (`$ 22,012.0`). "Free: ..."
    names the open program whose discount covers a line ("Free: you run Gemini", in green); the road's own tests
    read "Free: ready to open". Then "Upkeep 7,043 a season" (both programs and their components). Red when the
    funds can't cover it now.
  - **Hardware to develop:** the count leading its cell ("5 to develop", "All built"), then each component as a
    chip with its skill icon and name; an owned one a `SUCCESS` outline, "· 64 % · yours" and a check.
  - **The rocket it needs:** the smallest rocket that lifts its landing (name, class), its picture whole at
    81 × 52 (user, 2026-10-31: a narrow crop cut the Saturn V off), and its state: "Open · 48 %", "Not open:
    12,500 to open", or in red "Needs VAB level 3" (heavy rockets). A second line: "Two launches per flight"
    (Gemini EOR), "A heavy rocket from the first test flight" when the road's first Earth-orbit test already
    needs a rocket of the landing's class (the LK-700's needs the N1, Gemini direct ascent's the Saturn V),
    else "Lifts the landing in one launch". The LK-700's Against line says the same.
  - **Crew per flight:** "3 astronauts" / "2 astronauts".
  - **Flights on the way:** how many configurations its two programs offer before the landing ("21 flights before
    the landing" for Apollo, 5 for Gemini EOR, 6 for Gemini direct ascent, 17 for the 7K-L3, 15 for the LK-700),
    and the Firsts on the way as goal chips (trophy; reached: a 5 % navy fill, `INK_MUTED`, a check): the goals of
    `moon-roads.json`'s `firsts` (rendezvous, docking, crew transfer, crewed flyby, crewed lunar orbit) that the
    road's flights before the landing earn, the cascade included.
  - **The landing:** its steps with the crew at risk and its launches ("28 steps, every one with the crew at
    risk · 1 launch"; Gemini EOR's 30 of 36 · 2 launches, both must work); its first-time prestige.
  - **Trade-offs:** two short lists from `moon-roads.json`, **For** (green 7 px dot) and **Against** (gold, not
    red: they're costs, not failures), two or three lines each.
  - **Foot:** a history-note icon (component 15) with the road's real story, and a button **View the program** to
    the Moon program's details page (or **View the Earth-orbit tests**, whichever is the next to open).
  - **Locked** (before the tests' goals): a veil over the column (white at 72 %, a framed line with the lock
    icon) naming the missing goals: "Opens when you reach Human in space".
- **Reach the Moon without landing:** a slim strip under the columns with the roads that only fly round or orbit the
  Moon (NASA: circumlunar Gemini, lunar-orbital Gemini; Soviets: the 7K-L1), one compact card each: name, mode
  "Flyby only" / "Lunar orbit only", "To start now", the Firsts it can take, and one line ("A cheap way to the
  Moon's first Firsts; it shares Gemini's capsule with the Gemini roads"). Never presented as a mistake. Under
  NASA's three columns it needs a short scroll at 1080 (the page scrolls); the Soviet page fits.
- **Footnote:** "Opening a road's Earth-orbit tests makes its Moon flights free to open. Rockets and components
  you already own count for every road that uses them."

**Every number is the rules'** (G7): opening prices through `ProgramOpening.openingPrice`, the rocket through the
same lift check assembly uses, the component list and steps from the pack; nothing typed into text. The words
(how it works, the trade-offs, the history note, the dead ends' line) live in `assets/content/moon-roads.json`,
outside the pack like `front-pages.json`, keyed by the Moon program's id, with the Earth-orbit program and the
mode; `RoadsToMoonTest` checks the figures against the rules and holds every lunar-expedition program of both
packs to an entry (a road or a dead end). No new colour tokens: the tags, chips, dots and veil are built in the
screen from existing tokens.

**Content** (unconfirmed; the words in `moon-roads.json` are the reference):
| Road | How it works | For | Against |
|---|---|---|---|
| Apollo | A mothership stays in lunar orbit; a small lander goes down and back. | The Saturn V lifts it all in one launch. The most test flights, and Firsts, on the way. | Two craft to develop and test. Three seats to fill on every flight. The Saturn V is needed only for the Moon flights: start it early. |
| Gemini EOR | Two launches meet in Earth orbit; the joined ship flies on to the Moon. | No giant rocket: two medium ones. Builds on Gemini's capsule. | Every flight needs two launches, and both must work. The longest landing flight, and the most parts. |
| Gemini direct ascent | One ship flies to the Moon, lands whole and comes back. | One craft and the fewest flights. Builds on Gemini's capsule. | Needs Nova, the biggest rocket there is. A heavy craft to land and lift off again. |
| Soyuz 7K-L3 | A Soyuz orbits the Moon; one cosmonaut lands in the LK. | The N1 lifts it in one launch. Soyuz heritage; many flights on the way. | Two craft and a docking system to develop. The crew crosses to the lander by spacewalk. |
| LK-700 | One ship flies to the Moon, lands whole and comes back. | One craft to develop; the fewest flights. | The UR-700, the largest rocket of all, from the very first test flight. |

**History notes** (all five checked into `design.md` §1.1): Apollo (Houbolt, July 1962), Gemini EOR (Chamberlin's 1961
Lunar Gemini and the Langley light lander; no source confirms an Earth-orbit-rendezvous variant, so the note
doesn't claim one), Gemini direct ascent (Nova), the 7K-L3 (the LK's three uncrewed tests, the N1's four failures),
the LK-700 (Chelomei's UR-700, never built).

### 4.22 Server settings *(the user's idea, 2026-11-13; canvas `Launch Window — Server settings`)*
**Purpose:** let the server's admin edit everything in `game-meta.json` from the client, so nobody hand-edits
JSON (`client.screen.ServerSettingsDialog`; server side G21, `design.md` §3.6). The server checks every value
and saves the file itself; hand edits still work.

**Where:** a "Server settings…" row under About in Settings (§4.12), and a ghost button "Server settings" at the
foot of Join game (§4.2), so an admin can set the game up before founding an agency. Both need a login. The dialog
brings its own scrim, waiting chip and toast, since Join game has no session; it's built from the server's
`ServerSettings` alone (shots `server-settings-*.png`, `-Dshots=serversettings`, over Join game).

**Unlock** (480 px, eyebrow ADMIN): "Enter the admin password. The server prints it to its log at every start;
it's stored in game-meta.json as adminPassword.", the password field with an eye button (crossed out while shown),
Cancel and Unlock (Enter too). Waiting: the chip at the top centre, Unlock off. A wrong password keeps the text,
frames the field red and says "That's not the admin password." under it; locked out ("Too many wrong tries. Try
again in N minutes.") turns the field and Unlock off. **The password is kept in memory while the dialog is open
and sent with every request**, never saved (the client reconnects on its own, so nothing is kept per connection
on the server); a changed password replaces it on save.

**Settings** (1100 px wide, eyebrow ADMIN): a status line with the clock icon ("Turn 1 open · no agency yet · next
resolution Sat 14 Nov, 09:00 Europe/Berlin (09:00 your time)"), a 210 px section list (the open one with a navy
bar and a light navy fill, a navy dot for unsaved changes, a red "!" for a refused field, which wins), the form
scrolling inside a 560 px shell, and Cancel and Save at the foot (Save on once anything changed). Each section
opens with its name and a line:
- **Schedule** ("When turns resolve. Applies at once, from the next resolution."): seven 50 × 36 weekday chips
  with Every day and Weekdays beside them, "At least one day. Six turns a week."; the time a plain HH:MM field;
  the zone a select showing its offset today, opening a popover with a search field, up to 8 matches (the
  first highlighted, Enter picks it, a check on the current one) and "N of 604 zones. Offsets as of today."
  (The mockup marked the typed part gold; libGDX labels can't, so it isn't.)
- **Breaks**: rows of From, To (inclusive), the day count, a PAST tag (the row at 55 %) and a trash button; each
  date field has a calendar button opening a month (Monday first, the range shaded, its ends navy, today ringed
  gold, chevrons for the months); Add break adds one starting tomorrow.
- **Game**: the pack a select with its line ("The whole race, from the first satellite to the Moon and the
  planets"; early-era "The early years: the first satellites and the first people in space", unconfirmed), and
  a popover of the bundled packs; switching the pack picks its default race. The declared goals as chips with
  their patches (× unticks), a search field and the pack's whole checklist (16 px box, 28 px patch), "3 of 207
  picked · the pack's order"; the seed with Random. **Locked** (`ServerSettings.packLock`, `goalsLock`): grey
  boxes with the lock icon, the goals as plain chips, the reason under each with a small lock, the seed "Hidden
  once the game has begun", and the line "The game itself: these are fixed now that it has begun."
- **Bots**: Bot, Side (NASA / Soviet), Agency name, Director (placeholder "Drawn at random"), trash; Add bot
  takes the next free `bot-N`. A bot in the race is grey with its side off and "In the race since turn 3. Its
  agency stays: nobody else would play it.", without trash.
- **Development**: Dev mode and Open every archive page as Off / On segments, the warning a gold notice ("Every
  player gets Resolve turn now and the funds cheat. Use it for play tests only."), the archive one off without dev
  mode ("Needs dev mode. For reviewing the archive; saves nothing.").
- **Admin**: New password (with the eye, which shows both) and Repeat side by side, "At least 8 characters.";
  Make a new one fills both with an `AdminPasswords.generate()` password shown once in a gold box with Copy.
- **Saving**: the client checks Repeat ("The passwords don't match.") and the seed ("A whole number."), the
  server the rest (§5). Refused: the field framed red with the words under it, the section's "!", the dialog
  jumps to the first one, and the foot says "Nothing was saved. Fix the marked field and save again." Stale: a
  gold banner over the form with Reload, Save off, "Save is off until you reload." Saved: the toast "Server
  settings saved.", the marks gone, the status line recomputed. Cancel with changes asks "Discard your changes?"
  naming the sections, Keep editing or Discard (red).

**New pieces** (in the dialog, from existing tokens; no new colours): weekday chips, the select and its popover,
the date field and calendar, the section list with its marks, the password field with an eye, the "shown once"
box, a 16 px checkbox with a patch row. **New icons** (`assets-raw/svg/ui/make_ui_icons.py`): eye, eye_off, copy,
calendar, trash, plus, dice, spark.

## 5. Refusal reasons (for toasts and inline messages)

The server replies with these codes. The wording below is the spec's; the client's own is in
`client.ui.Refusals` and the screens.

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
| EXCEEDS_CAP / INSUFFICIENT_PRESTIGE | More than the conversion allows. / Not enough prestige. |
| SLOT_CLAIM_REQUIRED | (Client goes to §4.2.) |
| Launch: PROGRAM_NOT_OPEN / LAUNCH_CAP_REACHED / MISSION_COMPONENT_UNAVAILABLE / WRONG_ROCKET_COUNT, ROCKET_NOT_AVAILABLE / ROCKET_CANNOT_LIFT_PAYLOAD / WRONG_*_COUNT / *_NOT_IDLE / DUPLICATE_* / UNKNOWN_ASTRONAUT, UNKNOWN_MISSION_CONTROL_OFFICER / CAPCOM_NOT_AVAILABLE / UNKNOWN_MISSION_CONFIGURATION | Open the program first. / No more launches this turn: the launch window is full. / A component is grounded this turn. / Pick a rocket for every launch. / That rocket can't fly this launch. / Fill every slot the mission uses. / Someone in a slot isn't free any more. / Someone is in two slots. / Someone in a slot isn't on the roster any more. / CAPCOM needs a free astronaut who isn't in the crew. / That mission doesn't exist in this game. Short of funds reads "Not enough funds: $ N short." |
| Mission Control: UNKNOWN_MISSION / CREWED_MISSION_IN_FLIGHT / UNKNOWN_SLOT / SLOT_NOT_USED / WRONG_EMPLOYEE_TYPE / PERSON_NOT_AVAILABLE | That mission isn't scheduled or in flight any more. / A crewed mission in flight can't be abandoned. / That console doesn't exist on this mission. / That console isn't needed any more. / That console takes someone of another type. / They're not free any more. |
| Rocket programs: VAB_FULL / ROCKET_CLASS_TOO_LARGE / UNKNOWN_ROCKET | The VAB can't hold more rocket programs. Upgrade it or close one. / Upgrade the VAB to build this rocket class. / That rocket doesn't exist in this game. |
| Building upgrade: MAX_LEVEL / ALREADY_UPGRADING; program opening also UNKNOWN_PROGRAM | Already at the top level. / Already upgrading. / That program doesn't exist in this game. |
| Manage department (`AdjustSalariesAction`): INVALID_PERCENT / EMPTY_DEPARTMENT | Pick a change between −50 % and +50 %. / There's nobody in the department yet. |
| Hands-on campaign (component 26, type 11; `HandsOn.Outcome`, `RdDecisions.Outcome.NO_PUZZLE`): WRONG_PHASE / NO_SLOT / CANT_PAY / UNKNOWN_COMMAND / NOTHING_QUEUED / CANT_RESIST / NO_PUZZLE | Not now: the campaign is somewhere else. / The round's slots are full. / You can't pay for that command. / That command isn't in this campaign. / Nothing is queued. / Not enough shifts to call in an expert. / No campaign is in progress. |
| Findings (§4.20; `Findings.PublishOutcome`): NOT_A_FINDING / NOT_KNOWN / ALREADY_PUBLIC / ALREADY_QUEUED / NOT_QUEUED | That goal isn't a finding. / Reach the goal first. / Someone published it already. / It's published at the next resolution already. / There's nothing to withdraw. |
| Contracts (§4.17; `ContractOutcome`): CONTRACT_ACTIVE / NOT_QUALIFIED / PROGRAM_NOT_OPEN / UNKNOWN_LISTING; closing a program while its contract flies is the existing MISSIONS_IN_FLIGHT | You have an active contract: one at a time. / You can't fly this one: unlock its line first. / Open the program first. / That listing isn't on the board any more. / Can't close while a mission uses it. |
| The world reacts (§4.18.1, component 28; `SubmitStatementResponse`): TURN_RESOLVED / UNKNOWN_PAGE / REACTION_WINDOW_CLOSED / NOT_A_RIVAL / INVALID_STANCE / QUOTE_TOO_LONG / QUOTE_BAD_CHARACTERS | The turn resolved before that reached the server, so nothing was done. / That front page isn't there any more. / Statements on that First closed when the turn after it resolved. / Only a rival that was in the race when the First was printed can answer it. / Pick one of the five stances. / At most 140 characters. / Only letters, numbers and common punctuation. |
| Join game (`ClaimSlotResponse.Reason`): AGENCY_NAME_TAKEN / DIRECTOR_NAME_TAKEN / MISSING_FIELDS / ALREADY_CLAIMED / NOT_AUTHENTICATED / SERVER_ERROR | Already taken by another agency. (under the field) / the same / Required. / (client logs in again and opens the agency) / (danger notice with the server's words) |
| Server settings (§4.22, G21; `ServerSettingsResponse.Status`): WRONG_ADMIN_PASSWORD / ADMIN_LOCKED_OUT / STALE_SETTINGS / INVALID_FIELD / NOT_AUTHENTICATED / SERVER_ERROR | That's not the admin password. / Too many wrong tries. Try again in N minutes. / The settings changed on the server since you opened them. / (under each field, the server's words: "Pick at least one day.", "Not a time: use HH:MM, 00:00 to 23:59.", "Not a time zone.", "A break ends before it starts.", "Not a date: use YYYY-MM-DD.", "That pack isn't on this server.", "At least one goal.", "That goal isn't in this pack: …", "At most 32 characters.", "Already taken by another agency.", "Already taken by another director.", "At least 8 characters.") / (the foot, with the server's words) |

---

## 6. Gaps the server, rules, or content must fill for M5

What the client needed from the server, rules or content beyond the state, found while writing
this spec. All are done except G6 and G1's pack download; each done item names what was built.

- **G1. Game info.** Done: `GameInfoRequest`/`GameInfoResponse` (`design.md` §3.6 and §5 M5): the
  content pack id and hash, the declared goals, the game turn and date (turn 1 is Winter 1955, one
  season per turn), the next resolution time, the game result, and the budget schedule. Content is
  frozen per game: the server copies the pack into the game's data folder at creation and only loads
  it from there; the hash covers that copy. **Not built:** downloading the game's pack on a hash
  mismatch (a `FetchContentPackRequest`/`Response`). Deferred (user, 2026-10-06): content changes ship
  as a new server version and the version check at login makes every client update, so until then
  the client warns and uses its own copy.
- **G2. Turn report and replay data.** Done: `rules.TurnReport`, saved per player and turn under
  `players/<id>/reports/<turn>.json`, served by `TurnReportRequest` (gzipped); step events carry what
  the replay shows. Not kept: "prestige so far" on an active mission, and last season's funds change
  in the state (so the footer shows none).
- **G3. Rival summaries.** Done: `RivalsRequest`/`RivalsResponse`, one `RivalSummary` per player
  from each one's latest resolved snapshot (agency, director, archetype, the declared goals' turns; a
  credited goal counts from its canceller's turn, through `WinCondition.goalTimes`), plus `bot` and
  `prestige`, `goalTurns`.
- **G4. Display text.** Done: `strings.json` beside each pack's rules files (not hashed), read by
  `content.ContentStrings` and looked up through `client.view.ContentNames`: names, descriptions, long
  descriptions and history notes, in our own words (never BASPM's, `design.md` §1.2).
- **G5. Personnel names.** Done: `rules.PersonnelNames` (our own lists, well-known space-program
  people left out, no name anyone at the agency has), a portrait first (`rules.PortraitCatalog`,
  `Personnel.portraitId`) and a name and age to match it (`personnel/names.json`).
- **G6. Today's queue.** Not built: today's pending actions and their results, for the queue panel
  (component 9).
- **G7. Derived numbers the client shouldn't recompute by hand.** The opening
  price, launch cost, eligible rockets per launch, goal modifiers, mission
  assessment, auto-assign ranking, and running-cost breakdown are rules logic. The
  client ships `core` and calls the same methods locally against the preview
  state, so the UI never duplicates the rules (`rules.MissionAssessment`, `MissionLaunch.check`,
  `MissionStaffing`, `MissionCancellation.preview`, `BudgetProgress`, `PrestigeConversion`, ...).
- **G8. Department salary adjustment (new rule)** *(decided: BASPM's one-off)*. Done:
  `rules.DepartmentSalaries` and `AdjustSalariesAction`: every salary in one group by p % (−50 to +50)
  and every morale in it by +p for a raise or −3p for a cut, clamped to 0–100 (`baspm.md` §9.8).
  **Once per department per season** (user, 2026-10-06): a second change is refused as
  `ALREADY_ADJUSTED`, and Manage department says so in a toast instead of opening.
- **G9. Celestial bodies (content).** Done: `ProgramCategory.body`, a `CelestialBody` (Earth when
  absent, so the early-era pack's hash is unchanged); the standard pack maps its categories. The
  available and open counts per body are derived in the client.
- **G10. Memorial records.** Done: `PlayerSnapshot.memorial` (`MemorialEntry`, animal passengers
  too, `MemorialEntry.Cause.PASSENGER`), written by `MissionProgress` and `RandomEvents`;
  `PlayerSnapshot.goalRecords` for the Achievements tab.

---
- **G11. R&D decisions (component 26).** Done: `PendingDecision` on the state (one at a time, with
  its seed, component and arc), the offer rule at resolution, `DecideAction` resolved at once and
  journaled, the default at resolution, report events, each pack's `decisions.json`, playout bots that
  decide.

- **G13. The hands-on decision (component 26, type 11).** Done: `rules.HandsOn`, a pure state machine
  (plan, execute, awaiting a resist, finished) over the puzzle state in `PendingDecision`, rolls from
  the decision's seed and the step index; `HandsOnAction` (queue, undo, reset, confirm, resist,
  accept), refused out of phase; the levels in `decisions.json`, a test searching each level's
  all-normal play, a playout bot that plays it.

- **G12. Commercial contracts (§4.17, component 27).** Done: `rules.ContractMarket`, the unlock sets in
  each pack's hashed `commercial.json`, clients in `commercial-clients.json`; the board settled before
  the season passes and saved in each agency's state, so no board message (`design.md` §2.11,
  Storage); the market in `market/turn-N.json`, applications sealed, `TurnRollback` deleting market
  files; the contract flight's own payout in `MissionProgress` (no prestige, no repeat count).

- **G14. Front pages (§4.18).** Done: `rules.FrontPagePrinter` (from each player's turn report, so a
  retried resolution prints the same), `model.FrontPage`, `server.schedule.FrontPageStore` (a shared,
  append-only `data/frontpages.json`, covered by `TurnRollback`), `TurnScheduler.printFrontPages`,
  `FrontPagesRequest`/`FrontPagesResponse` (every page of the game, fetched after login and after a
  `TurnResolvedNotice`). A page keeps agency, director and crew names, never account ids. Names are
  capped at 32 characters when claiming a slot (`NAME_TOO_LONG`, unconfirmed), so `FrontPagesTest`
  can measure.
  *Unconfirmed:* printing the crew's names tells rivals who flew. That's public news, but it names
  astronauts the Intelligence Office's personnel file would otherwise have to find.

- **G15. The archive (§4.19, `design.md` §2.12).** Done, with no event and no saved field:
  `assets/content/archive.json` (`ArchiveTest`), `ArchiveRequest`/`ArchiveResponse` with
  `ArchiveUnlock` (goal, the first turn anyone held it, whether the asker was among the first,
  `atTheEnd` once `game-result.json` is written), `RequestDispatcher.handleArchive` through
  `WinCondition.goalTimes`; the client keeps it (`ClientSession.archive`, `Archive.openedIn`).
- **G20. The standing (component 30, `design.md` §2.1(g)).** Done: `StandingRequest`/`StandingResponse`
  with a `StandingSeries` per agency, from the turn reports (never pruned, so nothing new is saved),
  with the player's reviews and lost crews; funds only for the asking player. `server.StandingCache`
  (the user's decision, 2026-11-02): in memory, filled at startup, caught up each turn and request;
  rollbacks run with the server stopped, so the next start rebuilds it (about 2.8 s cold for 6 agencies
  and 240 turns). `ServerGateway.fetchStanding`, `DevClient`'s `standing`.
- **G16. Findings (§4.20, `design.md` §2.13).** Done: each pack's hashed `findings.json`;
  `PlayerSnapshot`'s `publicFindings`, `pendingPublications` and `publishedFindings`;
  `PublishFindingAction`; `PublishPreviewRequest`/`PublishPreviewResponse`; publications settled once
  (`FindingPublications.settle`) into `publications/turn-N.json`, deleted by `TurnRollback`; a finding's
  archive page only for agencies that know it; events `FindingPublished`, `FindingLearned`.
- **G19. In memory (§4.18.3, component 29).** Done: the tones and condolences are `FrontPage.Stance`
  values appended (`tone()`, `condolence()`; `givable()` stays the First's five), sent with
  `SubmitStatementRequest` on the loss's id and kept by `ReactionStore`; `losses/turn-N.json`
  (`LossStore`, a result of turn N, dropped by its rollback), `FrontPagesResponse.losses`;
  `rules.MemorialPrinter` prints the pages first of the turn's (at most 4 condolences told one by
  one); `FrontPage.Kind.MEMORIAL`; the article by `client.view.MemorialView`.

- **G18. Theories of the day (§4.18.2).** Done: `FrontPage.Kind.SCIENCE` (its story id the theory's),
  `rules.SciencePrinter` in `TurnScheduler.printFrontPages`, once per theory, kept in `frontpages.json`;
  the questions derived on the client. `FrontPage.firstPage()` means a First's page.
- **G17. The world reacts (§4.18.1, component 28).** Done: `FrontPage` gains `kind` (FIRST or
  REACTIONS; missing reads as FIRST), `reactsTo` and `reactions` (`FrontPage.Reaction`);
  `FrontPage.Statement`; `SubmitStatementRequest`/`SubmitStatementResponse`; statements kept in
  `reactions/turn-N.json` by `GameSeeds.rivalKey`, sealed as the resolution reads them
  (`ReactionStore.seal`; a later one is refused with `TURN_RESOLVED`); `rules.ReactionPrinter`;
  `ServerGateway.submitStatement`, `ClientSession.statements`; `DevClient`'s `pages` and `react`.
- **G21. Server settings (§4.22, `design.md` §3.6).** Done:
  - **The admin password:** `GameMeta.adminPassword`. `GameServer.create` (startup only, never
    `GameMetaStore.loadOrCreateDefault`, which also runs every tick) makes one when it's missing
    (`ServerSettingsService.ensureAdminPassword`, `core`'s `net.AdminPasswords.generate()`: 16 characters without
    look-alikes, in groups of four) and prints `Admin password (game-meta.json, adminPassword): …` at every start.
    A missing or blank one matches nothing. `data/` is ignored by git.
  - **Messages** (appended last in `MessageRegistry`): `ServerSettingsRequest(password)`,
    `SaveServerSettingsRequest(password, settings, newPassword)`, `ServerSettingsResponse` (status, message,
    minutes left) and `ServerSettingsChangedNotice` (unasked, like `TurnResolvedNotice`; `NetworkClient` keeps
    both out of the reply queue, and a screen fetches the game info again). The settings travel as gzipped JSON
    of `net.messages.ServerSettings`, so none of its types are registered: every field but the password, plus
    the version (the file's SHA-256), the open turn, the agencies, the next resolution, the locks and the bundled
    packs with their goals and default races. `ServerGateway.fetchServerSettings`/`saveServerSettings`.
  - **Every request carries the password** (no per-connection unlock: the client reconnects on its own). Checked
    in constant time; 5 wrong tries from one address in 10 minutes lock it out for 10 (`ADMIN_LOCKED_OUT`), each
    logged with the address (`NetworkServer.remoteHost`). Behind NAT every client may share one address.
  - **The save** (`ServerSettingsService.save`) re-reads the file, refuses STALE_SETTINGS if its hash isn't the
    version sent, and builds the new settings **from the file**, taking only the unlocked fields from the request
    (a hidden seed, the password and the bots in the race carry over; locked fields are ignored, not refused).
    It checks what startup would: the schedule, the breaks, the pack being bundled and loading, the goals in its
    catalog, bot ids, sides and names (32 characters, not taken by an agency or another bot), then writes
    atomically.
  - **Applies at once:** the schedule and breaks are read from the file anyway; `RequestDispatcher`'s meta is
    replaced (dev mode, the archive toggle, the goals, the seed); new bots get their agencies
    (`BotPlayers.ensureSlots`). A changed pack makes `GameServer` rebuild its game in place after answering
    (`startGame`: the frozen pack, catalog, bots, game info, dispatcher, scheduler; the logged-in connections carry
    over), so nobody founds an agency on the old one.
  - **Locks:** the pack and seed once `players/` holds a folder or a turn has resolved (an empty server still
    resolves turns, writing the market and intelligence from the pack and seed); the goals once a turn resolved; a
    bot once its folder exists.
  - **Tests:** `ServerSettingsServiceTest` (the password, the lockout, the locks, a no-change save keeps the file,
    stale, refused fields, a pack switch saving a file startup accepts), `ServerSettingsIntegrationTest` (a real
    server: switch the pack, then found an agency on it). `TurnRollback` doesn't touch `game-meta.json`.

## 7. Suggested mockup order

All done: every screen in §4 is mocked up (or built straight in libGDX) and built. For a new
screen: Claude writes its spec here, the user mocks it up in Claude Design (see the notes at the
top), Claude builds it. Mockups don't need to be pixel-final: layout, components and states are what
implementation needs. Flag any new colour, component variant or icon so `Theme.java` and the UI
Asset Sheet stay in sync.
