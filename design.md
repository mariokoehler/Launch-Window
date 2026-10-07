# Launch Window — Design Document

What the game is, how each system works, the architecture, and the roadmap. It describes the
game **as decided or built now**; how decisions were reached belongs in git history.
**The full history up to 2026-10-06** (every trace, rejected proposal and harness run) is in
`git show 6fc7af5:design.md`. Update this file the same session a decision is made. Section
numbers are stable anchors (code and the other docs cite them): never renumber.

**Markers.** **(unconfirmed)** is a concrete default proposed to keep work moving, not yet
signed off. **(from BASPM)** is carried over from the original game. `baspm.md` has how BASPM
does each thing.

## 1. Concept

**Launch Window** — *"Stay one small step ahead."* — is a non-commercial fan project: an
asynchronous multiplayer reimagining of **Buzz Aldrin's Space Program Manager** (BASPM), a
Cold War space-agency management sim where the player runs NASA or the Soviet program, manages
personnel, budget and prestige, and designs and flies missions from Sputnik onward. A launch
window is the narrow period a rocket has to launch in: each day is the player's window to act,
and it closes on schedule whether they show up or not.

- **Session shape:** players log in whenever it suits them, read the last turn's report and
  queue actions for the current turn. At a fixed server time every player's turn resolves at
  once. Nobody waits for anybody. **One turn = one season** (a quarter of a game year; funding
  reviews every 16 seasons). A game runs weeks to months.
- **Turn schedule per game** (§3.6): a weekday schedule plus host-set blackout periods, added
  any time. A friends group plays daily; a colleagues group Monday to Friday only.
- **Players:** two real target games, about 4 friends and about 10 colleagues. Design for
  4–10, not an MMO. (BASPM's own multiplayer was strictly 1v1.)
- **Fan project:** BASPM is Slitherine/Matrix Games' IP. Launch Window ships its own name, art
  and text (§1.2).

### 1.1 Educational layer (a core pillar)

**Decided (user, 2026-09-27):** the game should teach the real history and engineering of
spaceflight, especially *why* something was hard, which BASPM's historical-facts text stops
short of (it covers what happened and the politics). Whenever work touches a real mission,
engineering problem or tidbit, note it here the same session.

**Forms:**
- **History notes (decided, user, 2026-10-04):** an icon of its own next to a label, field or
  value; hovering shows two to four sentences of real history or engineering, a click pins the
  card (`ui-spec.md` component 15). Always optional, never rules help. The join screen's
  Director field has the first, so players learn the icon there. Content history notes live in
  `assets/content/early-era/strings.json` (G4).
- **Space-race news in the turn report (user 2026-10-04, built 2026-10-06):** real events from
  the same three months as the turn's game date open the report's news.
  - **The stories:** `assets/content/space-race-news.json`, 47 stories from Winter 1955 to
    Spring 1963, keyed by year and season (Winter is January to March), each with a headline,
    two to four sentences, and for the big events an optional American and Soviet view
    (`nasa`, `soviet`). Like `strings.json` it sits outside the frozen pack, so corrections reach
    running games. Never reuse or rename a story id: reports store it.
  - **The pick** (`content.SpaceRaceNews`): when a player's turn resolves, one story from that
    season is picked with the player's season seed (salted, so the season pass's random stream is
    untouched) and stored as `TurnReport.newsStoryId`. Players therefore read different stories
    where a season has several, and a redone turn picks the same one.
  - **Shown** under "On the real timeline · <season>", with "At home:" and the player's own
    side's view where the story has one. Seasons without stories, and dates past the written
    ones, show only the random events.
  - **Writing rules:** our own words from checked facts (§1.2). A side's view is paraphrased
    framing that's documented, never an invented quote. When the game runs past 1963, add stories
    for the later seasons.
- **Explainers (user's idea, 2026-10-06; mocked up; E-01 built in libGDX the same day, E-02 to
  E-05 on 2026-10-07; triggers unconfirmed):** short full-screen "infographic" series that explain *how and why* an
  aerospace idea works, offered (never forced) when it first comes up in play and kept in the
  Museum (`ui-spec.md` component 18, with each sheet's content and the proposed triggers). Canvas `Launch Window — Explainers`
  (private; ask the user for the link), one row per explainer with a yellow note of what to verify.
  - **E-01 Suborbital vs orbital** (3 sheets, built, in the Museum's Encyclopedia tab): Newton's
    cannon and the 8-kilometre rule; Freedom 7 against Friendship 7 to scale; the rocket
    equation's propellant share, staging, re-entry.
  - **E-02 Rendezvous** (2, built 2026-10-07, like E-03 to E-05): brake into a lower, quicker orbit to catch up (drawn true to
    Kepler); Vostok 3/4, Gemini 4, Aldrin's doctorate and Gemini 12, Gemini 6A/7, the first dockings.
  - **E-03 Going to the Moon** (2): the trip priced in delta-v; Redstone to Saturn V and N1 to
    scale, lunar orbit rendezvous.
  - **E-04 Restarting an engine in space** (2): propellant floats in free fall; settling burns,
    self-lighting gas-fed engines, bags and vanes.
  - **E-05 Hypergolic propellants and steering jets** (2, user's suggestion; built as
    "Hypergolics and steering jets"): the three
    propellant families compared; thruster quads turning, sliding and stopping a craft, Gemini 8.
  - **Figures, checked:**
    - E-01 is checked (2026-10-06): Freedom 7 (5 May 1961): peak 187.5 km, 487 km downrange,
      top speed 8,262 km/h, 15 min 28 s. Friendship 7 (20 February 1962): about 161 by 261 km
      (NASA: 100 by 162.2 statute miles; the mockup's 265 km was corrected), 28,234 km/h, 4 h 55
      min 23 s, three orbits, about 121,800 km. Mercury-Redstone about 30 t and Mercury-Atlas
      about 120 t at liftoff. Worked out: Earth curves away about 5 m per 8 km (8² / 2 × 6,371
      km); energy of motion ½v² is 2.6 and 30.7 MJ per kg (ratio about 12); with a 2.9 km/s
      exhaust the rocket equation gives 57 % propellant for 2.5 km/s and 96 % for 9.4 km/s.
      Newton's cannon is from his *Treatise of the System of the World* (written in the 1680s,
      published 1728); Tsiolkovsky published the rocket equation in 1903; the R-7 drops its four
      boosters about two minutes up; the Mercury Atlas dropped its two booster engines and flew
      on with the sustainer. Still to source: the Atlas's tank walls "thinner than a coin".
    - E-02 to E-05 are checked (2026-10-07), with these corrections to the mockup's text:
    - E-02: laps 88.4 min at 200 km and 92.4 min at 400 km (Kepler's third law). Vostok 3 and 4
      (11 and 12 August 1962) came within about 6.5 km (5 km was planned). Gemini 4 (3 June
      1965) gave up station-keeping with its Titan II upper stage to save fuel. Aldrin's Sc.D.,
      MIT, January 1963: "Line-of-Sight Guidance Techniques for Manned Orbital Rendezvous"; on
      Gemini 12 (November 1966) the rendezvous radar failed and he worked the approach with a
      sextant and charts. Gemini 6A and 7 (15 December 1965) as close as about 30 cm (one foot).
      Gemini 8 docked on 16 March 1966; Kosmos 186 and 188 docked automatically on 30 October
      1967.
    - E-03: delta-v rounded for an Apollo-style trip (9.4 to orbit with losses; 3.1 TLI, 0.9
      lunar orbit, 2.0 landing, 1.9 ascent, 1.0 home). Heights 25.4 / 28.7 / 33.2 / 110.6 /
      105.3 m and liftoff masses about 30 / 120 / 150 / 2,900 / 2,750 t (Mercury-Redstone,
      Mercury-Atlas, Gemini-Titan II, Saturn V, N1). Command and service module plus lander
      about 44 to 47 t toward the Moon, the command module about 5 t at splashdown. John Houbolt
      pushed lunar orbit rendezvous. The N1 flew four times (February and July 1969, June 1971,
      November 1972) and failed each time. "Halfway to anywhere" is widely credited to Robert
      Heinlein (repeated by Jerry Pournelle) but not traced to a text, so the sheet says "a
      saying credited to". 2,900 / 120 is about 24: "some 24 times", not 25.
    - E-04: the Saturn V's third stage (S-IVB) fired its small ullage thrusters to settle its
      propellant before relighting its J-2 for TLI, the first restart of a hydrogen-oxygen engine
      in weightlessness. Apollo's service propulsion engine and the lunar module's ascent engine
      were pressure-fed and hypergolic; the ascent engine had no backup. The Soviet probes: the
      top stage (Blok L) was a separate stage that had to start for the first time after a coast
      in orbit, so "relit" was wrong. It stranded the first Venus probe (4 February 1961, an
      ignition failure) and Venus and Mars probes in August, September, October and November
      1962; on 25 August 1962 an ullage motor failed to fire and the stage tumbled (added to
      the sheet).
    - E-05: oxygen boils at −183 °C, hydrogen at −253 °C. Vacuum exhaust speeds about 3.0
      (F-1, 304 s), 4.2 (J-2, 421 s) and 3.1 km/s (Apollo's service engine, 314 s). The word
      "Hypergol" was coined by Wolfgang Nöggerath (Braunschweig) in the early 1940s. The R-16
      (24 October 1960) burned UDMH with a nitric-acid oxidiser and killed at least 78 (other
      counts are higher): the sheet says "at least 78", not "more than 70". Vostok's attitude
      jets used cold nitrogen, Mercury's hydrogen peroxide. Gemini 8 (16 March 1966): a stuck
      thruster (No. 8), a spin reaching about one turn a second, stopped with the re-entry
      control jets, whose use ended the flight by mission rule.
    - "In Launch Window" lines checked against the pack and the rules: the steps (Retrofire,
      Countdown, Reentry preparations), the goal Two crews together ("within a few
      kilometers"), every early-era rocket Light with 0 kg to TLI, VAB level gating rocket
      class, Payload R&D in the SET Centre, Vostok's single retrorocket, the R-7's and Faith 7's
      history notes. Jupiter-C's upper stages are solid, so E-05 says every early-era rocket
      burns liquid oxygen "in its main stages".
  - **Next candidates:** why re-entry capsules are blunt, why reliability matters over a
    30-step mission (the odds multiply), why launch latitude matters (Earth's spin as a boost).
    The concept audit below adds more.
- **Concepts: linking game content to the explainers** (user's idea, 2026-10-07; specified in
  `ui-spec.md` component 19, mocked up the same day). Programs, configurations, steps, rockets and
  capsules each list the real-world concepts behind them; a Concepts section on their screens
  shows them as chips that open the right explainer sheet, with a gold dot until the player
  has read it. This replaces the proposed "New idea" notices. No inline links in text (user,
  2026-10-07); if other text should link, it gets the same chip row below it.
  - **Audit of the early era** (2026-10-07; a first pass, unconfirmed). Step ids without season
    numbers; "all orbital" means every configuration whose flight step is `earth-orbit`,
    `earth-orbital` or `orbital-flight`.
    - *With an explainer today:*
      - **Suborbital** → E-01 sheet 1: the four suborbital configurations, step
        `suborbital-flight`, goal Human in space, Redstone.
      - **Orbit** → E-01 sheet 1: all orbital configurations, steps `earth-orbit`,
        `earth-orbital`, `orbital-flight`, goals Satellite in orbit and Human in orbit,
        programs Sputnik 1, Sputnik 2 and Explorer 1.
      - **Orbital speed** → E-01 sheet 2: Vostok, Mercury, Atlas, R-7 (Vostok).
      - **Staging** → E-01 sheet 3: step `ascent`, R-7 (Sputnik), R-7 (Vostok), Atlas,
        Jupiter-C.
      - **Rocket equation** → E-01 sheet 3: the four rockets that reach orbit.
      - **Retrofire** → E-02 sheet 1: steps `retrofire`, `retrofire-jettison`, every
        configuration that has them, the Vostok and Mercury capsules.
      - **Rendezvous** → E-02 sheet 1 (sheet 2 for the history): both joint configurations,
        step `joint-orbital-flight`, goal Two crews together.
      - **Delta-v** → E-03 sheet 1: the VAB's payload columns (the four rockets), Atlas.
      - **Rocket classes** → E-03 sheet 2: every rocket (all Light).
      - **Engine restarts** → E-04 sheet 1: R-7 (Vostok) (its upper stage lights in flight),
        step `retrofire`.
      - **One-shot retrorocket** → E-04 sheet 2: Vostok capsule.
      - **Cryogenic propellant** → E-05 sheet 1: step `launch-preparations` (liquid oxygen
        boils away), `countdown`, every rocket.
      - **Steering jets** → E-05 sheet 2: step `reentry-preparations`, the Vostok and Mercury
        capsules.
    - *No explainer yet (shown nowhere until one exists; the candidate list):*
      - **Weightlessness** (they're falling, not out of gravity): steps `duration-level-i` and
        `-ii`, `biometric-data-transmission`, goals Life in weightlessness, A day in space, Days
        in space, Sputnik 2, Korabl-Sputnik 2, the duration configurations.
      - **Heat shield and re-entry** (why capsules are blunt): steps `reentry`,
        `modules-separation`, every returning configuration, both capsules.
      - **The odds multiply** (reliability over many steps): every configuration.
      - **Launch escape** (Mercury's tower against Vostok's ejection seat): step `launch`, both
        capsules, steps `descent-and-landing` (Vostok's pilot ejects).
      - **Landing on land or sea** (parachutes, recovery): steps `descent-and-landing`,
        `descent-and-splashdown`.
      - **Talking to an orbiting craft** (ground stations and coverage gaps): steps
        `earth-orbit`, `orbital-flight`, goal Radio from orbit, Sputnik 1.
      - **The space environment** (radiation belts; what Explorer 1 found): Explorer 1, goal
        Space environment measured, step `advanced-experiments-earth-orbit`.
      - **Human rating** (what makes a rocket safe for people): Redstone, Atlas, R-7 (Vostok),
        the crewed configurations.
      - **Launch latitude** (Earth's spin as a boost): step `launch`, every rocket.
      - **Max Q** (the point of greatest aerodynamic stress): step `ascent`.
      - **Power in orbit** (batteries, then solar cells): the extended configurations, Sputnik 1.
      - **Keeping a rocket pointed** (gimbals, vanes, Jupiter-C's spinning upper stages):
        Jupiter-C, step `ascent`.
      - **Orbits decay** (thin air drags a satellite down): Sputnik 1, the extended
        configurations.
      - **Balloon tanks** (Atlas's pressure-stiffened skin): Atlas. Small; its history note may
        be enough.
    - *First impressions:* weightlessness and the heat shield would each attach to most
      crewed content, and "the odds multiply" ties straight to how the game resolves a
      mission, so those three are the strongest next explainers.
- **A Museum encyclopedia** (started 2026-10-06): the deeper version of the same content; its
  first entries are the explainers above (`ui-spec.md` §4.11).
- **Real images:** NASA photos are US government works and normally public domain. Soviet and
  other imagery needs a per-image rights check, and every third-party image gets an entry in
  `assets/CREDITS.md`.

**Used so far:** the Director note (NASA's Administrator was a public, Senate-confirmed
appointee; Korolev was a state secret, known only as "the Chief Designer" until his death in
1966); Gagarin ejecting at about 7 km and landing by parachute, kept quiet because record rules
required landing with the craft (Vostok program details); Bondarenko (Museum memorial; checked:
an oxygen-rich, low-pressure chamber, ten days into a fifteen-day test, secret until 1986); and
G4's notes in `strings.json`: Sputnik 1 as a stopgap for Object D, Laika, Vanguard's televised
explosion, Glenn's heat-shield warning, Vostok's self-righting sphere, Explorer 1's flat spin,
the R-7 and Atlas lighting every engine on the ground, Atlas's balloon tanks, the Jupiter-C's
held-back 1956 flight, Mercury-Redstone 1's "four-inch flight", Ivan Ivanovich, Gagarin's
1-2-5 unlock code, Titov, Bykovsky and Tereshkova, the Vostok 3/4 group flight, Van Allen's
belts, Ham, Liberty Bell 7, Enos, Aurora 7, Faith 7, Belka and Strelka, and Sputnik 1's beeps
leading to satellite navigation.

**Candidates** (verify the figures before using any):
- **Veterans or rookies:** NASA's Mercury Seven (1959) were test pilots aged 32–37 with 1,500+
  flying hours; the first twenty cosmonauts (1960) were young fighter pilots, mostly mid-twenties,
  picked partly to fit Vostok's cabin (under about 1.70 m and 72 kg, younger than about 30).
  Apollo 11's controllers averaged about 27. Fits the hire screen next to a candidate's age.
- **Mary Golda Ross:** Cherokee engineer at Lockheed from 1942, a founding member of its
  Missiles Systems Division (Agena, early Mars and Venus studies). Fits a SET portrait.
- **Nisei first names:** many Japanese Americans born in the 1920s–30s had an English first
  name, which is why the name list mixes "George" and "Kazuo"; many were interned in WWII.
- **Vostok's natural-decay backup:** one retrorocket, no backup, so the orbit was planned low
  enough for drag to bring the capsule down within about ten days, with supplies for that long.
  Vostok 1's stage burned long and put it higher. Fits Vostok Orbital's retrofire step.
- **Saving throws are real:** Apollo 11's 1202/1201 alarms (a controller's call not to abort)
  and Apollo 13's improvised survival plan. Fits a step rescued by its saving throw.
- **Expert rescue:** Apollo 13's command-module power-up procedure, devised by EECOM John Aaron
  and the ground team and tested in the simulator by Ken Mattingly, grounded by measles
  exposure. A controller and a non-flying astronaut rescuing a mission together is exactly the
  Expert mechanic (§2.5(i)).
- **Flight Director as a generalist:** Mission Control's five skills echo real consoles (FDO,
  GUIDO, EECOM). A great Flight Director is broad, not deep.
- **"Tiger Team"** is real NASA jargon for an urgent specialist task force.
- **Rocket families:** reliability transfer encodes the real Redstone → Jupiter-C → Juno II
  and Atlas/Titan/Saturn lineages; real families inherited engines, guidance and experience.
  (A rival-driven "second mover" discount would be a genuine cross-player idea, not decided.)
- **Prestige became budget:** NASA's funding rose sharply after Sputnik and Gagarin, before
  Apollo had flown. Fits the first funding review.
- **Trouble concentrates where things are already strained:** BASPM's bad events strike
  components in R&D, morale after failures. Fits event text that explains why it fired.
- **International cooperation came later:** no unified agency existed in the Cold War; the ISS
  came decades later. A Museum aside.
- **Prime vs extended objectives:** real missions are graded against must-achieve and bonus
  objectives. For wherever a mission's extended phase is explained.
- **Astronauts as PR:** the Mercury Seven's *Life* magazine deal and tours were a deliberate
  strategy to build support. Fits the PR-duty idea (§2.2).
- **Press stories that really happened** (easter eggs in `pr-press.json`, §2.2(c), the player's
  astronaut in the real person's place; no history-note icon):
  - `do-good-work`: Gus Grissom, asked for a pep talk at Convair's Atlas plant in 1959, said
    only "Do good work"; the workers made banners of it.
  - `both-hands`: at the Mercury Seven's introduction on 9 April 1959, asked who expected to
    come back, all raised a hand and John Glenn raised both.
  - `open-car-in-the-rain`: Yuri Gagarin in Manchester on 12 July 1961, guest of the foundry
    workers' union, rode an open-top car in pouring rain: "Surely the least I can do is get wet,
    too."
  - `loose-at-the-ankle`: the newsreels of Gagarin's walk down the red carpet at Vnukovo on 14
    April 1961 show something loose at his ankle, usually told as an untied shoelace (some
    accounts say a sock suspender).
  - `puppy-for-the-children`: Khrushchev sent Pushinka, a puppy of the orbital dog Strelka, to
    the Kennedys in 1961; the White House got about 5,000 requests for her puppies.
  - `family-on-the-cover`: the Mercury Seven's exclusive *Life* contract (April 1959) put the
    wives on its cover that September.
  - `paintings-on-stamps`: Alexei Leonov painted space scenes with Andrei Sokolov; Soviet stamps
    reproduced them from 1967.
  - `fall-from-a-balcony`: on 3 October 1961, at a Crimean sanatorium, Gagarin jumped from a
    balcony and was badly hurt; the official account was vague.
  - `bathroom-mirror`: on 26 February 1964 John Glenn slipped while fixing a bathroom mirror and
    hurt his inner ear, and withdrew from the Ohio Senate race.
  - `station-patrol`: on 27 March 1963 cosmonaut Grigory Nelyubov argued with a military patrol at
    Chkalovsky station and refused to apologise; he and two others were dropped from the corps.
- **Whole-crew losses:** Apollo 1, Soyuz 1, Soyuz 11, Challenger and Columbia lost everyone
  aboard, the real basis of §2.5(f). A sober note the first time it happens.
- **Bars (also called Chaika) and Lisichka** died when their Korabl-Sputnik launch failed on 28
  July 1960, 19 seconds in: a strap-on broke away and the capsule's parachutes opened too low.
  They're the dogs remembered on a failed Korabl-Sputnik 2 flight (§2.5(f)), as "Chaika". Sources
  differ on the first dog's name.
- **Rendezvous is counterintuitive** and **restarting an engine in zero-g is hard:** both now
  explainers (E-02, E-04 above).
- **The R-16 disaster** (Baikonur, 24 October 1960) for a later memorial or news context,
  handled with care; **Apollo-Soyuz's crew breathing nitrogen tetroxide fumes** on the way
  down (1975). Both turned up while writing E-05.

### 1.2 Content & asset policy: no copying BASPM's copyrighted content

**Decided (user, 2026-09-27), a hard rule.** The repo goes public on GitHub, so anything
committed to it is redistributed.

- **Text:** never copy BASPM's strings, and never lightly paraphrase them. Write from the
  real-world facts, researched in public sources (NASA history, encyclopedias), not by
  rereading BASPM's text.
- **Art, audio, video:** nothing extracted from BASPM ever enters the repo or the game. The one
  exception is the UI reference screenshots in `assets-raw/baspm/ui/`. Flag any task that risks
  pulling an extracted file into the tree.
- **Not restricted:** the real history itself (programs, hardware, dates, people). **Numbers**
  (costs, reliabilities, prestige, budget values, training lengths) follow BASPM's data by the
  user's decision (2026-10-03, confirmed 2026-10-06): they aren't creative expression, and
  they're what makes the game feel right. Tune them from play tests.
- **Fictional programs get our own names** (user, 2026-10-03): real programs keep their
  historical names; BASPM's invented ones are renamed. BASPM's Soviet "PKA space plane"
  (`26-01`) is the **OKB-MiG E-154**.

## 2. Core gameplay mechanics

Each subsection: **(a)** how BASPM does it (with the `baspm.md` trace), **(b)** what the async
model changes, **(c)** our decision, then implementation notes. Numbers follow BASPM's Normal
difficulty unless marked as our own (§1.2, §2.10).

**The season pass** (`rules.SeasonResolver`, per player, once per resolution): journaled
player actions first, then `PersonnelLifecycle` → `MissionProgress` (goals and mission prestige
inside it, per step) → `BuildingUpgrades` → `PrDuty` → `BudgetProgress` → `RandomEvents` →
`TrainingProgress` → `ResearchProgress` → `HiringPool.refreshCandidates`. The cross-player win
check runs once after every player has resolved. Each resolver is headless and reports
`ResolutionEvent`s, which become the turn report (§3.7).

### 2.1 Space agencies, budget & funding cycle

**(a) BASPM** (`baspm.md` §11, §13): agencies differ mechanically only in content access (NASA
and Soviet each see only their own programs and components); economics are identical. GSA, a
fictional third agency, sees everything and is only offered in casual and solo modes; BASPM's
competitive mode is NASA vs Soviet only. Prestige, earned only from missions and goals, is the
sole input to a **funding review every 16 seasons**, which sets a recurring per-season funding
rate from a tiered bracket table, with a catch-up for a missed bar. Funding is the only income.

**(b) Async:** nothing structural. Reviews count seasons, not real days, so a weekday game
reaches its first review later in real time, which is fine.

**(c) Decisions:**
- **Two non-exclusive archetypes, NASA-style and Soviet-style, no GSA** (user, 2026-09-27):
  content is gated by archetype, the economy is identical. Several players may share an
  archetype; each player's state is independent, so nobody contends for "their" Sputnik.
- **Port the review shape:** prestige is the only input; funding is a recurring rate set at
  each review; the formula is fully visible in the UI (current bracket, projected next payout).
  Missions and goals never grant funds directly. Prestige floors at zero.

**(d) Budget, as built** (values from BASPM, §1.2):
- **Start:** 25,000 funds, 250 prestige, 3,000 funding per season.
- **Reviews:** every 16 of the *player's own* seasons (M4 decision; a late joiner starts at the
  initial rate). `BudgetSchedule.baspmClose()`: BASPM's ten Normal-difficulty brackets, linear
  between each bracket's ends, ceiling-rounded, flat at the ends. **After the tenth review the
  last bracket repeats** (confirmed 2026-10-06). **Catch-up** (`baspm.md` §11.7): if prestige
  at the previous review fell short of that review's raw minimum, this review's bracket shifts
  down by the shortfall, so shortfalls don't compound.
- **Running costs each season:** every employee's own salary (drawn from BASPM's 25–60 when the
  person is created), program maintenance, mission-component maintenance, and building upkeep
  (§5, M3).
- **Debt is allowed** (user, 2026-10-03; BASPM ends the game below zero): debt blocks new
  spending through the ordinary `INSUFFICIENT_FUNDS` checks and has no other penalty.
- **Prestige-to-funds conversion, only while in debt** (our own addition): the player picks the
  amount; one conversion can bring funds up to two seasons of running costs (with funds at −D
  and running costs R, at most D + 2R). Prestige is spent, which lowers the next review: that's
  the intended trade-off. Validated when applied, not when submitted.
  **Rate: 2 funds per prestige** (user, 2026-10-06). A point is worth about 30 funds at the
  first review and is never spent there, so conversion stays an emergency tool; at the old
  0.25 funds per point a 1,000-fund debt cost 4,000 prestige, which made it unusable. A part
  point is rounded up (`BudgetSchedule.fundsPerPrestige`, `prestigeCost`).
- **Budget order inside `BudgetProgress`** (`baspm.md` §11.7): fixed costs, count the season,
  run a review if due (new rate), then grant this season's funding at the new rate.
- **The game info carries the game's budget schedule** (`BudgetScheduleInfo` in
  `GameInfoResponse`, 2026-10-06), so the client prices reviews and conversions with the game's
  own numbers. Today every game uses `BudgetSchedule.baspmClose()`; a configurable schedule would
  only need the server to load it.

### 2.2 Personnel

**(a) BASPM** (`baspm.md` §9): three groups (Astronauts, Mission Control, SET), five skills each;
Mission Control's Flight Director is the average of the other five. One season pass handles
age, morale, retirement and resignation. Training is fixed-length or "permanent" (repeats until
a target). Growth from training and flying has diminishing returns scaled by learning
potential; SET's R&D growth is flat. Hiring quality follows the group's own morale. **Tiger
Teams** are a live, paid boost to one step's saving throw during a mission.

**(b) Async:** standing orders (R&D, permanent training) and countdowns fit one turn = one
season unchanged. Tiger Teams' live per-step choice doesn't fit, so it was redesigned (c).

**(c) Decisions** (numbers marked "ours" are our own and confirmed for play testing on
2026-10-06):
- **Morale (ours):** idle −2 per season, busy (assigned or training) +3, clamped 0–100.
  **Resignation** at 35 or below. **Retirement ages (ours):** astronauts 50, Mission Control 60,
  SET 65. Per employee: morale, then age, then resignation, then retirement.
- **Departure protection, reversed from BASPM** (user, 2026-09-30): BASPM protects any busy
  employee. We protect **astronauts and controllers only while `ASSIGNED` to a mission**, never
  while training, and **SET never** (they only work on components, so nothing breaks if they
  leave). Applies to both resignation and retirement. A departure reports what the person was
  doing so the player can assign a replacement.
- **Retire on release:** a crew member or controller released from a mission at or past
  retirement age retires on the spot (otherwise back-to-back launches would keep them forever).
- **Skills** are doubles, capped at 100. **Growth** (`rules.SkillGrowth`): diminishing returns,
  `distanceToCap × factor × learningPotential / 100`, training factor 0.25–0.55 (ours); SET's
  R&D gain is flat, factor 0.15–0.35 (ours), clamped at the cap. Pulling out of training
  credits `fullGain × completed / total`.
- **Training** (`TrainingProgress`): lengths are **BASPM's** (user, 2026-10-05): basic 4
  (astronauts) / 2 (SET) / 2 (Mission Control), advanced 3. **Basic training follows BASPM's
  shape:** it starts automatically after hiring, the player can't start it or cut it short, so
  a hire can't be bought the season it's needed. Unlike BASPM, it grows one **random** skill of
  the group on completion (user's choice, kept 2026-10-06). **Permanent** training repeats
  advanced-length rounds until a target level, stopping when a round gains less than 0.05.
- **Expert trait** (replaces Tiger Teams, user, 2026-09-27 and 2026-10-02): an astronaut or
  controller (never SET) with a skill at 80 or more can train toward Expert in it. Each season
  succeeds with `25 + 10 × (skill − 80) / 20` percent, with no skill growth; success grants the
  trait for good. Several skills can be earned over a career. Expert training is the fourth kind
  in the shared training dialog. How Experts rescue steps: §2.5(i).
- **Hiring** (`HiringPool`): each season every group gets a fresh batch of candidates, quality
  banded by the group's average morale. **Batch size: 10, as in BASPM** (user, 2026-10-06).
  Rosters are capped by their buildings' levels (§5, M3). Each agency has about 32 to 36
  portraits per group, so with large rosters a batch reuses faces from the previous pool first.
  **Hiring is free** apart from salary. **Signing bonus** (user, 2026-10-06): the player may pay a
  one-off bonus of one season's salary (rounded to whole funds) to multiply the new hire's morale
  by 1.1, at most 100 (`HireAction.signingBonus`, `HiringPool.signingBonusCost`). A hire whose
  bonus the funds can't cover is refused (`INSUFFICIENT_FUNDS`); the hire screen has a check box
  per selected recruit.
- **Candidates are named after their portrait** (`PersonnelNames`, `HiringPool`): a portrait is
  picked first, then a name matching its ethnicity and sex and an age in its range. Lithuanian
  women always get the married surname form (confirmed 2026-10-06).
- **Age is a trade-off** (user, 2026-10-05; numbers confirmed 2026-10-06): older candidates
  start more skilled (up to about 20 points, centred so the average hire is unchanged, none above
  95), younger ones learn faster (learning potential 45–80 for a rookie, 10–35 for a veteran).
  Rookies catch up after about five years. Tune with `dev-tools`' `CareerCurves`.
- **Department salary adjustment** (G8, `DepartmentSalaries`, BASPM's one-off): a whole group's
  salaries change by −50 to +50 %, morale by +p for a raise or −3p for a cut (`baspm.md` §9.8).
  **Once per department per season** (user, 2026-10-06): `PlayerSnapshot.salaryAdjustedGroups`
  records the groups changed during the open turn, a second change is refused
  (`ALREADY_ADJUSTED`), and the season pass clears the record.
- **CAPCOM:** the astronaut on CAPCOM grows the skills they used that season, as a controller
  does (confirmed 2026-10-06).
- **PR duty for astronauts** (user decisions 2026-10-06, built the same day; `rules.PrDuty`): a
  **standing order** for an idle astronaut to tour and appear in public instead of training,
  until recalled. Real precedent: the Mercury Seven's *Life* deal and Gagarin's world tours (§1.1).
  - **Astronauts only** (user: the public wanted the people with "the right stuff").
  - **Busy, not protected:** state `PR_DUTY` counts as busy for morale (+3 a season, as training)
    but never protects from departure. No skill growth. An astronaut on tour can't fly or sit at
    CAPCOM; recalling is free and immediate. An Expert on tour still helps rescue steps, at 75 %
    (user, 2026-10-06), like a controller on another mission. A touring astronaut can resign, retire or be dismissed, but random
    events that sideline or remove staff only pick idle astronauts, so they never hit one on tour.
  - **Yield with fame** (user: keep fame): Leadership / 10, plus 3 per completed flight and 5 per
    completed flight that set a first in the game, at most 25 a season. A flight credits its crew
    (never CAPCOM) when it completes; a first counts once per flight however many goals the
    cascade credits (`Astronaut.flightsFlown`, `firstsFlown`).
  - **Diminishing returns** (user: yes): ranked by yield, the astronauts on tour earn 100 %, 60 %,
    30 %, then nothing (ties keep roster order). One function serves the season pass, the
    dialog's projection, the badge and the Public Affairs Office.
  - **Rust** (user: yes): from the third season in a row on tour, Piloting and Fitness each lose 1
    a season, floored at 0. The count lives in the season pass (`prDutySeasons`), so a recall and
    restart on the same day doesn't reset it; a pass that finds the astronaut off duty does. An
    Expert keeps the trait even below 80.
  - **Press stories:** each season on tour has a 10 % chance of making the papers, 70 % of them
    good. A good story adds half the season's yield, a bad one turns the season into a loss of
    half of it. Stories live in `assets/content/pr-press.json` (outside the frozen pack, ids never
    reused). No randomness is drawn while nobody is on tour.
  - **No repeats** (user, 2026-10-06): `PlayerSnapshot.usedPressStoryIds` records the stories a
    player has seen; a draw picks only unseen ones of its tone, and once every story of a tone has
    run that tone starts over. Per player, not per astronaut: the player reads them all.
  - **Effects beyond prestige** (user idea and confirmation, 2026-10-06; `rules.PressEffects`,
    authored in the JSON as `effects`, kinds in `PrPress.EffectKind`; the turn report adds one
    sentence per effect). 16 of the 39 stories carry one:

    | Story | Effect |
    |---|---|
    | Applications soar (engineering colleges) | SET pool +3 for 2 seasons |
    | Salutes the old pilots | Astronaut pool +3 for 2 seasons |
    | An engineer writes in | One SET candidate from the best quality band, this season |
    | Credits the engineers | SET morale +5 |
    | Do good work | R&D results ×1.10 for 2 seasons |
    | On the night shift | Running costs ×0.95 for 2 seasons |
    | Both hands | Astronaut morale +3 |
    | Answers every letter | That astronaut's morale +5 |
    | Family on the cover | Funds +10 % of the season's funding rate |
    | Stars in a supplier's adverts | Funds +5 % of the rate, that astronaut's morale −3 |
    | Charms the budget committee | Funding rate +5 % until the next review |
    | Crowds close a runway | The full yield as the bonus instead of half |
    | *Bad:* Shiny new car | SET and Mission Control morale −3 |
    | *Bad:* Argues with a patrol | That astronaut's morale −5 |
    | *Bad:* Slips at home | That astronaut's Fitness −2 |
    | *Bad:* A photographer follows in | One component in R&D loses a season of its expected gain (`ResearchPlanning`), floored |

    - **Coverage:** an effect of N seasons covers this season and the next N − 1 in the phase
      that reads it (budgets, research, pool refreshes); the PR phase runs before the budget and
      before the timed slots count down, so an R&D effect is stored one season longer. Tested
      end to end in `SeasonResolverTest`.
    - **Overlaps multiply** (user, 2026-10-06): the R&D and running-cost slots are shared with
      the random events (§2.7); a new effect multiplies into a running one and the longer
      duration stays (`rules.TimedEffects`).
    - **Bigger pools** live in `PlayerSnapshot.hiringPoolBonuses` until their refreshes run out.
      A standout comes from the best quality band, at whatever age the portrait gives, so its
      story gives no age ("a brilliant engineer").
    - **Only when there's something to act on:** a research setback needs a component in R&D
      expected to gain something; a raised funding rate needs six seasons before the next review
      and no review this season (which would reset it), as BASPM's extra-funding event does.
      When the only unseen stories of a tone can't run, the usable one seen longest ago runs
      again and moves to the back of the record, so the usable ones keep cycling without repeats
      and the others keep their turn.
    - **Effects live in the JSON** (user, 2026-10-06), outside the frozen pack: editing one in a
      running game changes the rules for stories drawn afterwards. Applied effects are stored in
      the turn report (`PrDutyPerformed.pressEffects`), so old reports still read right.
  - **The stories** (written 2026-10-06; user: a good variety, mostly fictional, some real events
    with the player's astronaut in the historical person's place as an easter egg): 27 good and
    12 bad, about the same 70/30 split as the draw, so each story turns up about as often. That
    is roughly 5 to 20 stories per player in an early-era game, so repeats stay rare; add more any
    time. Each story reads for either agency and any astronaut (no pronouns, no country-specific
    terms), models nothing the rules don't (no grounding injuries, no dismissals) and can happen
    twice to the same person. Ten are nods to history (§1.1).
  - **Timing:** the phase runs before the budget, so PR prestige counts at a review that season,
    as mission prestige does.
  - Every number and default here is our own, confirmed by the user on 2026-10-06; tune in play.

### 2.3 Programs & mission configurations

**(a) BASPM** (`baspm.md` §10): program categories (gated by goals) contain programs (opening
and maintenance cost, required components, optional goal gate), which offer mission
configurations (duration in seasons, prestige, goals; "extended" variants run longer and pay
more). A program costs less once a related program of the player's own is open
(`OpeningCostDeductions`). BASPM scaffolds twelve solar-system category groups but fills few.

**(b) Async:** all pre-turn input; nothing changes.

**(c) Decisions:** port the hierarchy and the follow-on discount (only within one agency; the
one cross-agency example in BASPM's data only works for GSA and is dropped). **Opening a program
also pays the creation cost of each component it grants that the agency doesn't own yet**, as
BASPM does (confirmed 2026-10-06). Programs gate on `requiredGoalIds` (an AND list). Programs
can be closed (§5, M3). BASPM's unbuilt scope (asteroids, stations, Mars expeditions) is fair
roadmap inspiration, since those are real domains.

### 2.4 Mission components & reliability

**(a) BASPM** (`baspm.md` §10): every component starts nearly unflyable (about 3 %). It improves
by **reliability transfer** (a one-time bonus at opening from related owned components, a
curated per-pair table that encodes real rocket families), by R&D, and by flying.

**(b) Async:** nothing changes.

**(c) Decisions** (`content.MissionComponent`, `rules.ProgramOpening`, `ResearchProgress`):
- Keep the curated transfer table. **A component's reliability floor is its transfer-inclusive
  starting value** (user, 2026-10-01); flight losses never go below it.
- **R&D, BASPM's full shape** (user, 2026-10-01; `baspm.md` §10.8): two ceilings, a lower
  `maxRdReliability` that R&D alone can reach and the absolute `maxReliability` that only flying
  approaches. **Up to four SET specialists** research one component at once, in slots with
  diminishing reward bands (slot 1 richest). The component's headroom is computed once per
  season and shared by every slot; each specialist's event reports a proportional share of the
  applied gain. **Reaching the R&D ceiling releases every researcher** on it (by R&D or by a
  flight), as BASPM does.

### 2.5 Mission simulation

**(a) BASPM** (`baspm.md` §8): a mission runs over several seasons, each with an ordered batch of
steps. Each step rolls a base chance from component reliability (and crew skill), and on failure
a saving throw scaled by flight-controller skill, which can rescue it or set the failure's
severity. A failure ends the mission. The chain needs no player input once it starts.

**(b) Async:** fits the daily batch directly: "resolve this mission's season" is self-contained.
All player decisions (launch, scrub, staffing) come before the batch.

**(c) Decisions:** port the two-roll shape and **BASPM's five outcome tiers, uniformly for every
step** (user, 2026-10-01): success, glitch solved (rescued), minor glitch (half credit, the
mission continues), failure (ends it), catastrophic (ends it and loses the crew; only on steps
flagged crew-risk). Multi-season missions are kept.

**(d) Step resolution** (`MissionStepResolution`): the base chance is the involvement-weighted
component reliability; once that exceeds **65** and crew are involved, it's blended with crew
skill at **20 %** weight (a weighted average, so weak crew can lower the odds; confirmed by the
user 2026-10-01). Controller skill only scales the saving throw (base × controller sum / 100). A
failed saving throw splits into catastrophic / failure / minor-glitch bands. These numbers are
ours and confirmed for play testing (2026-10-06).

**(e) Content shape** (`MissionStep`, `MissionSeason`, `MissionConfiguration`): a step lists
component involvements (summing to exactly 100), crew involvements (none, or summing to exactly
100, because the blend is a weighted average), controller involvements (100 or less; none means
no saving throw), each entry 1–100, a `crewRisk` flag (needs crew), a prestige share and a
failure deduction (§2.6), and goals achieved. A configuration's duration is its number of
seasons; it declares its exact crew and controller counts, its launch cost, and its staffing
layouts.

**(f) Missions in flight** (`InProgressMission`, `MissionLaunch`, `MissionProgress`):
- **Staffing slots, BASPM's layouts one to one** (2026-10-03, `StaffingSlot`,
  `MissionStaffing`): every crew seat and control console is a slot, and each roll reads **the
  one person in that slot**. This supersedes the 2026-10-02 rule that averaged a skill across
  everyone assigned. Only used slots must be filled. Control slots are released after their
  last season; CAPCOM can be re-staffed mid-mission. **Auto-assign** fills crew seats before
  control slots, so CAPCOM never takes the best pilot; ties go to roster order (confirmed
  2026-10-06).
- **Launch** checks the program is open, the exact headcount, idle people, funds and the launch
  cap, and **charges the launch cost at once**. A scrub before the mission flies refunds it in
  full (M4 decision 5, as BASPM's code does); no refund once it has flown.
- **Launching and the first season's steps happen in the same pass**, so nobody sits assigned
  across a turn boundary without flying.
- **A catastrophic step loses the whole crew** (user, 2026-10-02; real accidents lost everyone,
  §1.1), reported as `LOST_IN_ACCIDENT`. Controllers are never at risk.
- **Animal passengers** (`MissionConfiguration.animalPassengers`) who don't come home go on the
  memorial (`MemorialEntry.Cause.PASSENGER`), and the turn report says so (`PassengerLost`,
  2026-10-06).
  - **Sputnik 2:** Laika, on the first flight; later flights add "An unnamed dog" each
    (confirmed 2026-10-06).
  - **Korabl-Sputnik 2** (built 2026-10-06): Belka and Strelka really came home, so a *failed*
    flight remembers Chaika (also recorded as Bars) and Lisichka, the dogs really lost on 28
    July 1960. No photos yet; the user is looking for licensed ones.
  - **Animals that came home are honoured** in the Museum's Flights tab (user, 2026-10-06):
    `animalPassengers.cameHome` lists them per successful flight, in order, and the count of
    completed flights says which are earned. Belka and Strelka (Korabl-Sputnik 2), Ham (the
    uncrewed suborbital Mercury test), Enos (the uncrewed orbital Mercury test), Chernushka and
    then Zvezdochka (the uncrewed orbital Vostok test, first and second flight). A flight that
    honours someone may remember nobody on failure; no real animal is invented a death.
    **Photos** (2026-10-06, found by the user): Belka and Strelka (one photo of both), Ham, Enos
    and Chernushka show theirs on the Flights tab. Zvezdochka has none: her only known photo is
    non-free (fair use on Russian Wikipedia only), so it can't go in the repo.
    **A failed uncrewed test remembers an unnamed animal** (user, built 2026-10-06):
    `animalPassengers.unnamedLostOnFailure` puts one "An unnamed chimpanzee" (uncrewed Mercury
    tests) or "An unnamed dog" (the uncrewed orbital Vostok test) on the memorial per failure,
    with the step and no photo, and the turn report says so. The uncrewed suborbital Vostok test
    carries no animals.

**(g) After each season's steps** (once per mission per season, whatever the outcome):
- **Reliability change per component** (user, 2026-10-02): the worst outcome the component was
  involved in that season wins (failure > minor glitch > success; glitch solved counts as
  success, catastrophic as failure). Success and minor glitch grow toward `maxReliability` with
  diminishing returns (factors 0.05–0.15 and 0.02–0.06, ours); failure shrinks toward the floor,
  scaled by the failing step's involvement.
- **Skill growth from flying:** every astronaut on the mission grows all five skills; each
  controller grows the skills their console used.
- Both run before the retire-on-release check, so a final season still counts.

**(h) Mission cost:** see (f).

**(i) Expert rescue** (user, 2026-10-02; confirmed 2026-10-06), consuming §2.2(c)'s trait:
- When a step's first roll fails, every Expert whose Expert skill matches a skill the step
  involves (crew or controller) boosts the saving throw. Crew Experts get a role BASPM's
  controller-only Tiger Teams never gave them (Apollo 13's precedent, §1.1).
- **Roster-wide:** idle, training or on this mission counts fully. An astronaut on *another*
  mission can't help (they're in space); a controller on another mission helps at **75 %**, and so
  does an astronaut on PR duty (user, 2026-10-06: on Earth, but probably not in town).
  Each person is consulted once per step.
- **Stacking with exhaustion:** each Expert closes part of the remaining gap to 100 %, applied in
  turn: 50 % on their first use in a resolution, 40 % on the second, 10 % on the third, nothing
  after. Every consultation counts, even if the step still fails. **Capped at 95 %**, without
  ever lowering a higher natural chance.
- No extra randomness. `ExpertRescueApplied` reports each consultation; its "after" value doesn't
  reflect the cap.

### 2.6 Prestige, goals & scoring

**(a) BASPM** (`baspm.md` §11): goals are granted by mission steps. Achieving a goal can
cascade to lower goals (`GoalsCancelled`). A configuration can declare goal-dependent reliability
penalties or bonuses per component, read per attempt. BASPM's rival comparison shows the
opponent's goals next to the budget overview.

**(b) Async:** all deterministic.

**(c) Decisions:** port the cascade and the goal-dependency modifier, and show the modifier in
the UI rather than hiding it. Goals are plain achieved or not (a gold/silver/bronze rating was
scrapped). Goal badges are embroidered mission patches (`ui-spec.md` component 8).

**(d) Prestige and goals, as built:**
- **Mission prestige:** each configuration's total is split across its steps
  (`prestigeSharePercent`, summing to 100). A step pays its share on success or glitch solved,
  half on a minor glitch. **Repeat flights pay 100 / 50 / 30 %** of the total (BASPM's ratios,
  confirmed 2026-10-06), by the count of *completed* flights of that configuration, **read live
  at each season's resolution**, never stamped at launch.
- **Per season, not per mission:** a failing step **replaces** the season's accumulated total
  with its own `failurePrestigeDeduction`, so a failed season forfeits what its earlier steps
  earned (goals stay achieved). Earlier seasons keep their prestige.
- **The goal cascade:** `cancelsGoalIds` means "also credits": each goal credited this way pays
  its own prestige into the achieving goal's reward and counts as achieved, recursively
  (`rules.GoalCascade`). It never makes a goal unearnable.
- **A cross-player goal race** (user, 2026-10-03, our own; BASPM counts per player): a goal pays
  its first-time prestige to the first agency in the game to reach it and its second-time value
  to everyone after. Every achiever in the same resolution gets the first-time value; the game-
  wide count updates after the resolution. The count (`GoalTally`) is rebuilt each turn from
  every player's start-of-turn snapshot, so a retried resolution prices goals the same way.
- **Goal-dependency modifier:** recomputed for every step from the current achieved-or-credited
  goals, clamped at zero; a goal achieved earlier in the same flight counts at once.
- **Bookkeeping on the snapshot:** `successfulFlightCounts`, `achievedGoals` (goal → game
  turn), `cancelledGoalIds`, and `goalRecords` (how and when each goal was reached, for the
  Museum).

### 2.7 Random events

**(a) BASPM** (`baspm.md` §12.1): each season rolls once for a bad event (5 %) and once for a good
one (20 %), each picked from a pool gated by real conditions (a reliability setback only hits a
component in R&D). BASPM registers **15 bad and 5 good** events, hard-coded. Its "decision event"
with a player choice is dead code, so no BASPM event ever asks the player anything.

**(b)/(c) Decisions:** port the two rolls, BASPM's odds and all twenty events with their gates and
BASPM's magnitudes (confirmed 2026-10-06); don't port decision events. Results go into a
per-player **news log** (`NewsEntry`, last 50), read in the turn report; never a shared feed,
which would reveal a rival's setbacks.

**As built** (`RandomEvents`, `RandomEventRoster.baspmClose()`): rolled after the budget pass; in
each roll the first event of a shuffled pool whose gate passes is applied. Timed effects age at
the start of the pass, so an N-season effect covers N seasons.
- **Bad:** basic-training extension (one season), dismissal, failed basic training, resignation,
  unavailable employee (one season), perished astronaut (idle astronauts only), morale drop
  (10–20; only after a failure this season), prestige drop (4–10 %), unexpected cost (7–15 % of
  funds, only while funds are positive), R&D setback (×0.80–0.95 for 1–4 seasons), reliability
  setback (7–15 points on components in R&D, floored), launch cap of one, no launches, unexpected
  cost with no launches, components unavailable (half of them for 1–3 seasons).
- **Good:** extra funding (+7–16 % of the rate until the next review; needs a recent completed
  flight and six seasons to the review), morale boost (9–21; needs a recent completed flight),
  prestige boost (4–7.5 %), R&D boost (×1.15–1.25 for 1–7 seasons), reduced expenditures
  (×0.75–0.95 for 3–11 seasons; needs six seasons to the review).
- **Our deviations:** personnel events only take people not on a mission; BASPM's R&D and cost
  multipliers are never read in the shipped game, but ours apply; an unavailable employee still
  trains and researches. Two multipliers in one slot multiply and the longer duration stays
  (`rules.TimedEffects`, user 2026-10-06, for press stories, §2.2(c)); BASPM's setter replaced
  the older one.

### 2.8 Campaign objectives

**(a) BASPM** (`baspm.md` §12.3): a long-term objective (a goal by a deadline: victory or game
over) plus short-term objectives drawn at each review from a pool of seven predicate types
(funds, headcount, skill, reliability, a goal, a component's use count). `CampaignRTTM` races an
AI rival to one milestone goal.

**(c) Decision:** the predicate shape is prior art only; the win condition (§2.9) uses goal ids.

### 2.9 Multiplayer interaction model

No BASPM precedent beyond its 1v1 PBEM mode and rival comparison screen.
- **Decided:** start with **passive competition** (the race, rival summaries in the Museum) plus
  **in-game messages** between players over the game connection, never email (user,
  2026-09-28). Espionage, sabotage and interference come later, once the daily loop is proven
  fun and fair; they must not just punish whoever logs in less. Cooperative modes are a later
  idea.
- **Win condition** (user, 2026-09-28): the host declares a **list of goal ids** per game
  (`game-meta.json`, visible in the UI). **The first agency to reach all of them wins.** A listed
  goal counts as done when achieved or credited through the cascade. Its time is the game turn
  of the resolution.
- **Ties:** only when several agencies finish in the same resolution. For them, rank each
  declared goal by who reached it first, then compare like an Olympic medal table (most firsts,
  then most seconds…). Placement ties use **standard competition ranking** (1, 1, 3), confirmed
  2026-10-06. `WinCondition` is a headless function; the result goes to `game-result.json`.
- **Default declared goals** (`GameMeta.DEFAULT_DECLARED_GOALS`): satellite in orbit, human in
  space, human in orbit, which ends the race at the first crewed orbital flight, around turn 24
  (about six real weeks). Kept for the first play test; **decide again before the colleagues'
  game** (user, 2026-10-06); "A day in space" and "Two crews together" would lengthen it.
- **The game goes on after a winner:** goals, flights and prestige still count.

### 2.10 Difficulty level

**Decided (user, 2026-09-27): none.** One shared difficulty, based on BASPM's **Normal**
values only (`Value1` in its constants files); no difficulty field anywhere. Retune that one
balance sheet if needed rather than adding levels.

## 3. Architecture

### 3.1 High-level shape

A resident server process (KryoNet needs a listener, and the account store flushes from memory)
that does little most of the time: it answers requests over each client's one long-lived
connection (§3.3) and resolves the turn once per scheduled day (§3.6). Persistence is flat-file
JSON, no database (§3.7). One game per server instance (§3.9). Deployed with Docker, like the
sibling projects.

### 3.2 Relationship to BASPM's own simulation code

BASPM's per-player simulation (`Player.cs`, `GameWorld.cs`, the managers) is a season-oriented
state machine that already tolerates being paused, serialized and resumed (its PBEM mode does
exactly that). So its data shapes (programs, configurations, personnel, budget ledgers) are our
starting schema, and its data-driven XML is where formulas and numbers are traced from.

### 3.3 Tech stack

**Decided (user, 2026-09-27):** the sibling projects' stack: libGDX client, KryoNet network
layer, Jackson for JSON persistence, JUnit 5, Java 25, Maven multi-module (`core`, `lwjgl3`,
`server`, `dev-tools`), modelled on Robot-Rampage's parent pom. Release packaging (a jpackage
client zip with `update.cmd`, a server Docker image, tag-triggered GitHub Actions) was ported
from the siblings on 2026-10-06 (§5, M6; commands in `CLAUDE.md`).

- **Wire format is Kryo, not Jackson.** Message classes are registered in `MessageRegistry`,
  **append-only** (Kryo matches by registration order), with a test guarding every type. Jackson
  is only for files on disk. Kryo needs no-arg-constructible types, so maps are `LinkedHashMap`
  and sets `LinkedHashSet`, never `EnumMap`; enums go by ordinal, so constants are only ever
  appended.
- **One long-lived connection per client session** (user, 2026-10-04; replacing connect-per-
  call, which kept producing timeouts because KryoNet's `Client.stop()` can block about 11 s on
  Windows). Connect logs in once; every later request uses that connection; Log out or exit
  closes it, on a separate thread. `client.connect.NetworkGateway` checks the connection before
  each call and reconnects and logs in again by itself if it dropped. A connection that died
  silently looks alive until KryoNet's 12 s timeout, so a call in that window waits out the 8 s
  response timeout and shows one error. **A call that breaks mid-exchange is never repeated**
  (the server may have acted); read-only calls may retry once, action submits never. Server push
  ("turn resolved") is possible but not built.
- **Server tick:** requests are handled in `GameServer.render()`, which runs 50 times a second
  (a 1 s tick made every round trip take up to a second); the turn scheduler still checks once a
  second.
- No prediction, no live-turn machinery: much simpler netcode than either sibling.

### 3.4 Data model

**Per-player state is mutable** (user, 2026-09-28); the JSON snapshot on disk is the immutable
artifact. **Content definitions are immutable records** (`content` package), validated by
`ContentCatalog` (every cross-reference checked at construction). Everything in `model`,
`content` and `rules` is headless (no libGDX), with Javadoc and unit tests.

- **`PlayerSnapshot`** (`model`): the per-player aggregate root: agency and director name,
  `Archetype` (`NASA`/`SOVIET`), funds, prestige, funding rate, the player's own season counter,
  rosters and candidate pools per group, opened programs and rocket programs, owned components
  (`OwnedMissionComponent`: current reliability and floor), buildings, in-progress missions,
  goal bookkeeping (§2.6(d)), the news log, timed event state (§2.7), the press stories seen and
  the hiring-pool bonuses (§2.2(c)), the memorial, and id
  counters. Content is referenced by id, never embedded.
- **Ids are never reused:** `allocatePersonnelId()` (`p…`, shared by all groups and by
  candidates) and `allocateMissionInstanceId()` (`m…`). A stale action from an old view of the
  state must never hit a different entity.
- **Personnel** (`Personnel` with `Astronaut`, `SetSpecialist`, `MissionControlOfficer`):
  id, name, age (whole years, plus `agingProgressSeasons` 0–3, so age rises once every four
  seasons), morale, learning potential, salary, portrait id, `AssignmentState` (`IDLE`,
  `ASSIGNED`, `IN_TRAINING`, `PR_DUTY` for astronauts; plus an unavailability counter from
  events; enum constants are only ever appended), training fields
  (`TrainingKind` `NONE`/`BASIC`/`ADVANCED`/`PERMANENT`/`EXPERT`, seasons completed and total,
  target level; the target skill on each subclass), and per-subclass skill maps (doubles).
  Flight Director is computed (the average of the five), never stored. Astronauts and
  controllers have `expertSkills`; astronauts also `flightsFlown`, `firstsFlown` and
  `prDutySeasons` (PR duty, §2.2(c)). **`ASSIGNED` means a mission** for astronauts and
  controllers (`assignedMissionId`) and **R&D** for SET (`rdTargetComponentId` plus `rdSlot`
  0–3; both must be set). A freed slot leaves a gap.
- **Candidates** reuse the personnel classes; only the list they're in differs. A hire action
  returns an outcome (`HIRED`, `UNKNOWN_CANDIDATE`, `ROSTER_FULL`) rather than throwing, since a
  stale candidate id is ordinary in an async game. The hiring pool's quality bands (by the
  group's average morale; ceilings 44/64/84, skill ranges Poor 10–40 up to Excellent 55–90, an
  empty roster counts as 50) are our own.
- **`InProgressMission`:** instance id, configuration id, current season index, and the
  slot-aligned `crewSlotIds`/`controlSlotIds`.
- **Content** (`content`): `ProgramCategory`, `Program`, `MissionConfiguration`,
  `MissionSeason`, `MissionStep`, `MissionComponent` (with `maxRdReliability`, `maxReliability`,
  the four R&D band maxima, creation and maintenance cost), `Goal`, buildings, rockets; loaded
  from a content pack (§5, M3).

### 3.5 Asset generation pipeline

**Split by fit** (user, 2026-09-27): **ComfyUI** (local, Qwen Image 2.1) for painterly and
photographic art; **Claude Design** for UI, icons and vector work (the Robot-Rampage
precedent: a design-system canvas as the visual source of truth, its tokens carried into
`Theme.java`, scripts converting its output into the PNGs the game loads).

**Built so far:** personnel portraits (`assets-raw/portraits/`, 100 per agency, greyscale 128 px
JPEGs) and embroidered goal patches (`assets-raw/patches/`, 128 px RGBA PNGs), both with fixed
seeds, a manifest, a human pick, and a converter into `assets/` (commands in `CLAUDE.md`).
Program, category, configuration and hub art is the user's own, converted by
`make_content_art.py` and `make_hub_art.py`.

**Principles:** automation generates candidates, a human picks (a vision-model pre-check is a
cheap first filter; the patches' spelling is checked by eye). Never use BASPM's art as a style
reference or training set (§1.2). For a bigger batch, anchor the style first (a LoRA or
reference conditioning trained on our own hand-picked output across varied subjects).

### 3.6 Turn scheduling & calendar configuration

Per game (one per server, §3.9), in the host-edited `game-meta.json`:
- **Schedule profile:** a set of weekdays (presets `DAILY` and `WEEKDAYS`), a resolution
  time of day, and an explicit timezone (default `Europe/Berlin`), never the server's ambient
  zone.
- **Blackout periods:** inclusive date ranges with no resolution, addable at any time
  (holidays, vacations), checked at trigger time so a blackout added today still stops today's
  turn. Only future turns can be blacked out. The host edits the JSON file; no in-game host UI
  in v1.
- **The announced start date** is an automatic blackout from "server live" to the day before
  the start: the signup window.
- **A non-resolving day** is simply a day the job doesn't fire; queued actions carry over. The
  game turn only counts resolutions.
- **Missed days** (user, 2026-09-29): if the server was down over several resolution times,
  only one turn resolves on the next check; the gap is skipped, not replayed in a burst.
- **Implementation:** `server.schedule`: `GameMeta`, `ScheduleProfile`, `Blackout`,
  `GameMetaStore`, `SchedulerState`(`Store`), `TurnScheduler` (checked once a second on the
  server thread). `ResolutionSchedule` is the one rule shared by the scheduler and the client's
  countdown. **Game date:** turn 1 is Winter 1955, one season per turn (confirmed 2026-10-06),
  giving about 11 turns before the real Sputnik.
- **Dev mode:** `"devMode": true` adds "Resolve turn now" for play testing (the scheduled
  resolution still comes) and allows `DevClient`'s funds cheat.

### 3.7 Persistence & turn-resolution data model

**Decided (user, 2026-09-27): flat-file JSON via Jackson, no database**, matching the sibling
projects and BASPM's own XML save, which maps almost 1:1 onto its object graph.

- **Per player, resolved independently:** one snapshot file per player per turn. A player's
  resolution only needs their own last snapshot and their own journal, so players resolve in any
  order. Cross-player state lives in game-level files beside them (the schedule and declared goals
  in `game-meta.json`, the result in `game-result.json`); the goal tally is rebuilt from the
  players' snapshots each turn. **Rule for future interaction mechanics:** an effect on another
  player is declared as intent and computed at resolution from the target's *start-of-turn*
  snapshot; only a contended, shared, limited resource would need a tie-break rule.
- **Player actions are an append-only journal, one file per action.** Undo is by an inverse
  action at its normal cost, never by deleting from the journal (M4 decision 1). Assignment-style
  actions restate the current intent ("slot N is now X"), so replay converges on the latest.
- **Login** loads the last snapshot and replays the pending journal on top: the preview state.
  **Resolution** does the same, runs the season pass (§2), writes the next snapshot and archives
  the journal.
- **Trust boundary:** actions are intent only; the server always computes outcomes itself.
- **The data root** is a plain `data/` folder next to the server unless a path is given on the
  command line (confirmed 2026-10-06). It holds `game-meta.json`, `accounts.json`,
  `scheduler-state.json`, `game-result.json` once a game is decided, and the folders below.
- **Layout** (`persistence`, `PlayerStateService`):
  ```
  players/<playerId>/
    snapshots/<turn>.json      envelope: the PlayerSnapshot plus the last journal file it
                               applied; written once, never replaced; turn 0 is the claim
    journal/<seq>-<uuid>.json  pending actions; <seq> fixed-width, zero-padded
    journal-sequence.txt       the last sequence number, never reused or reset
    journal-archive/<turn>/    journal files a resolved turn applied
    reports/<turn>.json        the turn report (below)
  content/<packId>/            the game's frozen content pack (§5, M5)
  ```
  **Invariants:** journal names stay fixed-width and sequence-first (crash recovery compares
  them as strings), and the sequence never resets. Writes are atomic (temp file, then rename).
  A retried resolution sees the turn's snapshot already exists and archives exactly the journal
  files the envelope says it applied, so a later action stays pending.
- **Turn reports** (G2): each resolution writes `players/<id>/reports/<turn>.json`, a
  `rules.TurnReport` (the pass's events plus a few before and after numbers), atomically and
  *before* the snapshot, so a crash redoes the turn with the same seed and writes the same
  report. It travels as gzipped JSON inside `TurnReportResponse` (keeping the event classes out
  of `MessageRegistry`); gzip because Kryo's 64 KB buffer overflowed (a real turn is about 8 KB
  raw, 1.6 KB gzipped). The server refuses a report still too big rather than dropping the
  connection.
- **Saved formats are append-only:** `ResolutionEvent`'s `@JsonSubTypes` ids are never renamed
  or reused; event, report and snapshot fields are only added, with values old saves can do
  without (bump `TurnReport.VERSION` when a field's meaning changes). Jackson is strict about
  unknown fields, so a removed or renamed field means resetting the dev data root.
- **Museum records** (2026-10-04, additive): `PlayerSnapshot.memorial` (`MemorialEntry`) and
  `goalRecords` (`GoalRecord`), both append-only.

### 3.8 Player accounts & authentication

**Decided (user, 2026-09-27), reusing StarWars' account code** (`PlayerAccount`,
`AccountStore`, `PasswordHasher`, `AuthResult`):
- **An account is identity only:** a server-assigned id, a unique login (`[A-Za-z0-9_-]{1,32}`,
  `core`'s `LoginNames`), and a salted SHA-256 password hash (good enough for an invite-only hobby
  server). **No display name** (user, 2026-10-04): players know each other by their per-game
  Director name. Everything about a game lives in that game's state.
- **Storage:** one JSON file, an in-memory map flushed on a background cadence with rolling
  backups and a final flush on shutdown.
- **Login:** an unknown login creates the account; a known one needs the right password.
- **Player data is keyed by account id,** never by login (case-insensitive file systems, and
  logins can be renamed).
- **Joining is open for the whole game, with no player cap** (user, 2026-09-27): late joiners
  happen, and accept the disadvantage. A login without a slot in this game claims one: an
  archetype (non-exclusive), an agency name and a **Director** name, both unique in the game
  (case-insensitive), fixed for the game. A late joiner's own season count starts at 1; they
  resolve on the shared clock.
- **Host actions** (announcing, the start date, blackouts) are direct file edits in v1. An
  allowlist mode is a possible later host option.
- **Client settings** are a local file only (`~/.launch-window/client-settings.json`). It keeps
  the last server, login and password so Enter logs straight in (user, 2026-10-05). The
  password is in clear text on purpose: an accepted risk for a hobby game on the player's own
  machine.

### 3.9 Deployment: one game per server instance

**Decided (user, 2026-09-27):** one server process hosts exactly one game. The friends' game and
the colleagues' game are two containers with two data roots. That keeps the scheduler, file
layout and server simple; scaling is "start another container".

### 3.10 Testing strategy: automated bot-driven playthroughs

**Decided (user, 2026-09-27): the game loop must be drivable end to end by simple bots** over
hundreds of seeded games, so balance and multi-season correctness are answered by numbers, not
hours of manual play. The precondition is the rule this whole codebase follows: **the rules
engine is plain headless Java** (no libGDX), unit-tested directly, so a game runs in well under
a second. The model is Robot-Rampage's `Playouts` harness (the real resolver, seeded, a small
purpose-built report).

**Harnesses** (`dev-tools`, `devtools.analysis`; run commands in `CLAUDE.md`):
- **`ContentPackPlayouts`:** bots play the real early-era pack through the real season pass
  and report whether and when they reach the declared goals. With the pool of 10 (2026-10-06):
  198 of 200 games reach all goals (NASA 98, Soviet 100), average season 57.4; before, with 6
  candidates, 177 at 62.2. The bot hires the best of the batch, so a bigger pool means better
  hires.
- **`PersonnelLifecyclePlayouts`:** the personnel, R&D and mission resolvers on a small fixture
  catalog, with a bot that hires, trains, pulls out, opens programs, researches and launches
  through the validated actions.
- **`CareerCurves`:** rookie-vs-veteran skill curves for tuning the age trade-off (§2.2(c)).

**Invariants** are checked every season and recomputed independently of the resolver under
test (never by calling its own predicate), so a clean run is a genuine cross-check: skill and
morale ranges, the `ASSIGNED` pairings in both directions, unique ids across rosters and pools,
reliability ordering (floor ≤ current ≤ max), Expert thresholds and boost values, and more.
Report numbers are single-seed and shift whenever a change consumes random draws, so compare
runs with that in mind; per-run history is in the pre-2026-10-06 design doc (see the header).

**Findings worth remembering when tuning:**
- **Permanent training all but removes idle seasons,** so it strongly suppresses resignation:
  it's a low-effort way to keep staff, not just a convenience.
- **The "Poor" hiring band is nearly unreachable:** people at or below the resignation floor
  (35) leave the average, so a group only averages ≤ 44 if almost everyone sits at 36–44.
  Widen the gap between the floor and the Poor ceiling if unhappy rosters should hire worse.
- **Idle morale loss is a "sustained neglect" penalty** at typical activity levels, not a
  routine one.

### 3.11 Offscreen screenshot tooling for UI checking

**Decided (user, 2026-09-27), built 2026-10-04:** Robot-Rampage's `ScreenSnapshot`
(`lwjgl3/src/test/.../ScreenSnapshot`): a real GPU window that is never shown renders every
screen state from fixture state against a gateway that never answers, one PNG each. It checks
layouts and hard-to-reach states (dialogs, tooltips, refusals) and catches regressions no unit
test would (its first run found the nine-patch filtering bug). How to run it and how to diff
runs is in `CLAUDE.md`.

### 3.12 Visual design system / UI style

**Decided (user, 2026-09-28): a "restrained engineering blueprint" look**, chosen from four
mockup directions and two merge rounds. Dark looks (green-phosphor terminal, dark dashboard)
were ruled out: the game is text-heavy, so dark UI is a readability risk. The kept look merges
a bold flat-colour poster style with a quiet technical-document style; the heaviness people
disliked was border weight, not colour.

The design system lives in Claude Design (the **launchwindow** design system: tokens, components
with READMEs and previews). `core/.../client/ui/Theme.java` is its code twin and is kept in
lockstep with it. The **UI Asset Sheet** canvas collects every icon and texture.

**Colours carry meaning** (contrast checked with WCAG maths, not by eye):

| Role | Colour | Use |
|---|---|---|
| Primary / neutral | Navy `#16305c` | Chrome, primary buttons, table headers, borders |
| Active / in progress | Gold `#e0a72e` (text-safe `#8a5d12`, plus `gold-on-page`) | In progress, the current step, a steady state |
| Good / success | Green `#1f6b47` (on white), `#c4f5e0` `success-on-page` | Positive results; the on-page variant exists because plain green was invisible on the blue |
| Destructive / failure | Red `#c6402b` | Reserved for destructive actions and failures, never "in progress" |
| Commit | Violet `#4b3a72` | Reserved for one big action per screen; used by the launch button ("Launch — $ …") |
| Page frame | Blueprint blue `#3d6a9a`, white grid at low opacity | Page margins only |
| Content surface | White | Cards, tables, body text |
| Ink | `#1c1c1a` on white; `#eaf1fb`/white on the page | Text |

- **Negative values on the page** (debt, a negative delta) are a small solid `danger` tag with
  white text: a red text colour that passes contrast on the blue reads as white.
- **There's no "End Season" button:** turns resolve on the server's schedule.

**Typography:** Archivo (600/700) for headings, labels and buttons; Work Sans for body text,
since the game has real paragraphs. `Theme.TextStyle` holds the type scale (including
`BODY_STRONG`, the bold lead-in, which sits on its own line because a libGDX label can't switch
fonts mid-text). The fonts and their OFL licences are in `assets/fonts`.

**Layout rules:**
- Thin borders (1–1.5 px); solid fills only for state (badges, ratings) or the primary action.
- Anything read rather than glanced at sits on a flat, opaque white surface, never on texture
  or art.
- **No rounded corners and no shadows** anywhere (the history-note card's shadow was dropped,
  user 2026-10-04).
- A virtual 1920×1080 canvas in a `FitViewport`, letterboxed to 16:9 at any window size. The
  launcher sizes the window from the monitor (1080 rows when it fits, else 85 % of its height).

**Logo** (`assets/branding/logo.svg`, 2026-09-28): a mission-patch illustration (a Saturn V-style
rocket past Earth's limb, a gold "window" arc, a double ring and a ribbon wordmark). The wordmark
is its own lettering, not the UI typeface. Open: outline the wordmark text, and make a small
icon-only crop (the full patch reads as a coloured dot at icon sizes). `assets/branding/README.md`
has the details. SVG lessons: a circular `clip-path` silently drops anything outside it, and a
bezier's real peak must be solved for, not guessed from its control points.

**Icons:** all fifteen skills have icons traced from the user's own generated art
(`assets-raw/svg/icon_*.svg`; astronaut EVA and SET EVA suits share one), plus a stand-in Flight
Director icon. UI icons are SVGs in `assets-raw/svg/ui/`, rendered to white PNGs in
`assets/ui/icons/` by `make_ui_icons.py` and tinted at runtime. The **history-note icon** is an
open book (`icon_history_note`): it means only "a real story is here", never rules help.

### 3.13 Common screen chrome & the main-menu hub

`ui-spec.md` is the screen reference (§2 shared components, §4 per screen); this section keeps
the chrome and hub decisions. Mockup canvases are private Claude Design artifacts (ask the user
for links): Personnel Recruitment, Connect & Join Game, Space Complex Hub, SET Centre, VAB,
Headquarters, Mission Control & Turn Report, Astronaut Complex & Public Affairs Office, Museum
& Settings & Game Result, and the UI Asset Sheet. The per-canvas writeups (what each mockup showed
and proposed, which `ui-spec.md` cites as "the writeup") are in `git show 6fc7af5:design.md`;
the outcomes are in each screen's "Implemented" note.

**Chrome (every screen except the hub; `ChromeScreen`):**
- 30 px page padding, 20 px between header, content and footer.
- **Header** above a rule: back, home (the hub) and settings buttons, a breadcrumb rooted at
  "Space Complex" (earlier crumbs clickable), and on the right the prestige, game date and
  next-turn chips. The screen's own eyebrow and title sit below the rule and belong to the
  screen.
- **Footer** below a rule: five clickable stats with a chevron: funds (→ Public Affairs Office),
  SET employees, astronauts and flight controllers (hired / busy / capacity, → their buildings),
  open programs (→ Headquarters). **The footer always sits at the bottom of the frame**, however
  short the content (user, 2026-10-04).
- The page frame is the tiled blueprint grid (`Shapes.blueprintGrid()`).

**The hub** (`SpaceComplexScreen`, `client.view.Hub`) is BASPM's Space Complex screen:
- **Full-bleed art, no chrome** (user, 2026-10-04, after comparing both mockups). 32 images:
  NASA and Soviet, 4 seasons × 4 times of day (`make_hub_art.py`, cropped from 1088 to 1080 rows
  off the bottom).
- **Seven buildings** with one hotspot polygon set per archetype (`*.hotspots.json`, authored
  with `HotspotEditor`; one set fits every variant): VAB, Mission Control, Astronaut Complex,
  SET Centre, Public Affairs Office, Headquarters, Museum. Hover is a hard-edged near-white
  highlight (fill and outline at the same 0.18 alpha) with a label. Every building also shows a
  marker icon (built 2026-10-05; a gold gear while upgrading).
- **Season art follows the game season; time of day is a client-only ambience:** it steps
  dawn → noon → dusk → night when the player re-enters the hub at least 4 minutes after the last
  step (never while they're looking). The first entry shows noon.
- **The strip:** a navy nameplate (agency and director), then solid chips: dev-mode resolve
  (dev only), the navy date chip, next turn, Turn Report (white with a navy outline and a gold
  unread dot), funds, prestige, and a white Settings gear (user, 2026-10-04). Everything is
  solid, because the art's top band runs from near-black to near-white; check overlays on the
  brightest (Soviet winter noon) and darkest (winter night) variants.

**Design-system components** beyond the basics (Button, Badge, Card, Notice with warning /
danger / info / success, Field, OptionCard, HistoryNote, NavTabs, Table, ProgressTrack):
`Dialog`, `FilterChips`, `SegmentedControl`, `ReliabilityBar`, `SkillBar`, `RdSlot` (Release
and Assign are navy, not red: releasing isn't destructive) and `IconButton` (the white on-page
button), promoted from mockup CSS on 2026-10-04. Goal badges are now the generated mission
patches (`ui-spec.md` component 8).

## 4. Open questions

- **Declared goals for the colleagues' game:** decide before it starts (§2.9).
- **NASA/Soviet prestige balance:** BASPM's Soviet programs pay a little more each (about 4 %)
  but NASA has nine more programs, about 34 % more prestige in total. Decided (user,
  2026-10-01) not to close the gap up front; revisit with play-test and harness data.
- **A game-host role and UI** (announcing games, blackouts, allowlists): direct file edits for
  now.
- **Interaction beyond passive competition** (mail, espionage, sabotage, cooperation): after
  the daily loop is proven (§2.9).
- **Can SET staff reach retirement in a realistic game?** At 65 and today's game lengths, few
  will; it matters only if games run decades.

**Settled, for the record:** a player who doesn't log in before a resolution keeps their
default: a scheduled mission launches and training continues. Scrubbing and pulling out are
ordinary actions available until the resolution (user; BASPM's second "launch now?" prompt is
skipped). R&D assignments are standing orders (`baspm.md` §2.1). Tiger Teams became the Expert
trait (§2.2(c)).

## 5. Implementation roadmap: milestones & slices

Format from Robot-Rampage's roadmap: per milestone, its purpose, scope and exit criterion; done
milestones are kept short. Shape-defining choices (user, 2026-09-28): a **walking skeleton**
first (login → action → scheduled resolution → result, end to end) before real systems;
**deployment deferred** until after the first play test. Before starting a slice, grep
`baspm.md` for what it needs; if a fact is only on `E:` and this is the laptop, ask.

### Done

- **M0 — Project skeleton (2026-09-27).**
- **M1 — Walking skeleton (2026-09-29).** Persistence (`PlayerSnapshotStore`,
  `PlayerActionJournal`, `SnapshotReplay`, `AtomicFiles`, `PlayerStateService`), accounts
  (adapted from StarWars' code), the KryoNet protocol (`MessageRegistry`, Robot-Rampage's
  pattern), `RequestDispatcher` (one `handle(connectionId, message)` entry point, no
  KryoNet dependency), `NetworkServer`/`NetworkClient`, the scheduler (`server.schedule`) and
  `DevClient`. Exit proof: a real server resolved a submitted action on schedule over real
  sockets.
  - **The handshake** (`HandshakeRequest(clientVersion, login, password)` /
    `HandshakeResponse`) does the version check and the login in one step. Its two classes are
    **registered first in `MessageRegistry` and their fields never change** (a mismatched pair
    couldn't even decode the refusal). Credentials travel once per connection; later requests
    carry none.
  - **Accepted risk:** the password crosses plain TCP (no TLS), like its storage this suits an
    invite-only hobby server; the client also keeps it in clear text (§3.8).
  - **Threading:** everything the dispatcher calls runs on the server's render thread, and the
    scheduler runs on that same thread, so there's no concurrency to manage.
  - **Accepted limit:** a player whose resolution keeps throwing blocks the turn for everyone
    (the scheduler retries the same turn); fix it directly if it ever happens.
  - **Versioning:** jgitver only changes the version at a new tag, so the handshake check only
    tells builds apart across tags. `useGitCommitId` would fix that (not enabled; user's call).
- **M2 — Rules engine, headless (2026-09-30 to 2026-10-03).** Every §2 system as headless
  resolvers in `rules`, unit-tested and driven by the bot harness (§3.10). Exit criterion met:
  multi-season missions resolve correctly across turns in hundreds of simulated games.
- **M3 — Content pack (2026-10-03).** Decisions (user):
  1. **Values follow BASPM** (§1.2); ids, names and text are our own.
  2. **Early-era scope:** Earth-orbit satellites and the one-crew capsules (Sputnik, Explorer,
     Vostok, Mercury) with their rockets.
  3. **JSON, one file per content type,** in `assets/content/<pack>/`, loaded strictly by
     `ContentPackLoader` (unknown fields and missing numbers fail).
  4. **Rockets as in BASPM:** a rocket is a component bought through a **rocket program** at its
     creation cost (charged again on reopening; reliability kept). Steps reference the slot
     `@rocket-N`; at launch the player picks an owned rocket whose capacity for the launch type
     (suborbital, LEO, TLI, TPI; `_HR` types need human-rated rockets) covers the payload mass.
     The launch costs the payloads' unit cost plus the rocket's.
  5. **Content is gated by archetype** (`agency` on programs and components).
  6. **BASPM's staffing layouts** (now slots, §2.5(f)).
  7. **Buildings, all five of BASPM's** (`Building`, `BuildingSchedule.baspmClose()`,
     `BuildingUpgrades`): all start at level 1; upgrades use BASPM's costs, build times, upkeep
     and caps and are paid up front and final. The Astronaut Complex, Mission Control and SET
     Centre cap their rosters (level 1: 7 astronauts, 5 controllers, 7 SET); Headquarters caps
     open programs (`HEADQUARTERS_FULL`); the VAB caps open rocket programs and the rocket class
     (Light, Medium, Heavy). Upkeep is a running cost (925 per season at level 1).

  Also built: programs can be **closed** as in BASPM (refused while a mission of it flies; its
  components go inactive, keep their reliability, pay no upkeep and release their researchers;
  reopening doesn't charge creation cost again).

  **The pack** (`assets/content/early-era/`): 2 categories, 6 programs (Soviet: Sputnik 1,
  Sputnik 2, Korabl-Sputnik 2, Vostok; NASA: Explorer 1, Mercury), 10 components (five payloads;
  rockets R-7 Sputnik, R-7 Vostok, Jupiter-C, Redstone, Atlas), 19 configurations, 21 goals.
  Generated by `baspm_pack.py` (outside the repo, `CLAUDE.md`). **Mapping rules a regenerated
  pack must follow:**
  - Ids are our own, hand-mapped kebab-case; values are BASPM's.
  - A step's `PrestigeGrantedOnSuccess` is a percentage share → `prestigeSharePercent`; its
    failure value is a percentage of the first-time total →
    `failurePrestigeDeduction = round(pct × firstTime / 100)`.
  - Staffing layouts are emitted one to one: crew slot titles, control slots (title, kind,
    skill), slot-indexed controller involvements, per-slot crew weights. `ROCKET_N` → `@rocket-N`.
  - A configuration's `cost` is its payloads' unit cost.
  - Goal modifiers and achieved goals that reference goals outside the pack are dropped;
    reliability transfers keep only sources in the pack; **discounts whose source program
    belongs to the other agency are dropped** (BASPM's one such case only works for GSA).
  - A category's required goals are copied onto each of its programs. Controller slots map to
    skills by BASPM's speciality index, not the slot name. Animal passengers, and the animals
    honoured for coming home (`cameHome`), come from the converter's `ANIMAL_PASSENGERS` map, not
    from BASPM's data (backups `.bak-prepassengers`, `.bak-predogs`).

  `EarlyEraPackTest` checks the pack loads, every configuration has a liftable rocket of its
  agency at VAB level 1, staffing fits a reachable building level, and both agencies can reach
  the race goals (the human goals through the cascade). Exit criterion met by
  `ContentPackPlayouts` (§3.10).
- **M4 — Integration (2026-10-03).** Real player actions and real resolution behind M1's
  skeleton. Decisions (user):
  1. **Undo is inverse actions only;** the journal stays append-only (§3.7).
  2. **A new player starts with empty rosters, fresh candidate pools**, the budget's starting
     values, and every building at level 1: the first day is spent hiring.
  3. **Funding reviews count the player's own seasons** (§2.1(d)).
  4. **A win is recorded once (`game-result.json`) and play continues;** the host stops the
     server when done.
  5. **Inverse actions** (`baspm.md` §10.10): **dismiss** is free, refused for anyone on a
     mission (SET can be dismissed mid-R&D); **scrub** before flight refunds the launch cost in
     full; **cancel in flight** only for uncrewed missions, with the configuration's prestige
     deduction and partial refund; a crewed mission can't be abandoned; **upgrades are final**.

  Built: `SeasonResolver` (§2), the journaled actions (`actions` package, plain beans for
  Jackson and Kryo, each returning an `ActionResult`; `PlayerAction.seed` is set by the server
  on acceptance), replay that keeps a copy only if the action applied, submission validated
  against the preview, `game-meta.json`'s `contentPack`, `declaredGoalIds`, `gameSeed` and
  `devMode`, the slot claim (`ClaimSlotRequest`; until claimed, fetch and submit answer
  `SLOT_CLAIM_REQUIRED`), and resolution seeded by `GameSeeds.forSeason(gameSeed, turn, account)`
  with the goal tally built from every player's start-of-turn snapshot. Exit proof: three
  `DevClient` processes and a scheduled resolution produced exactly the predicted state.
  - **Open:** the fixture harness still sets some fields directly instead of going through
    actions; there's no multi-player harness (same-turn goal pricing is unit-tested only).
  - **Dispatch notes:** call `HiringPool.hire` with `catalog.buildings()`, and launch through
    `MissionLaunch.launch(snapshot, LaunchRequest, catalog)`.

### M5 — Playable client (libGDX): in play testing

Screens are specified in `ui-spec.md` (server gaps G1–G10 in its §6), mocked up in Claude
Design and built in libGDX (workflow in `CLAUDE.md`). The first play test runs in dev mode, not
from a packaged release.

**Screen-flow decisions (user, 2026-10-03, `ui-spec.md` §3):** BASPM's buildings regrouped: the
VAB owns rockets (open, manage, Rocket R&D), Headquarters owns payload programs and all mission
planning, the SET Centre owns payload R&D, Mission Control watches scheduled and active missions.
Personnel buildings open on a personnel overview, with Hire and Manage department as
sub-screens. Assembly and launch are one screen (slots filled by hand or auto-assign). Programs
are browsed by celestial body (`ProgramCategory.body`, defaulting to Earth). Configurations are
viewable before their program opens, read-only. The mission assessment is computed in the
client from `core` (`MissionAssessment`, exact, not simulated).

**Content packs** (decided 2026-10-03, built 2026-10-04 except the download and the host's
replace step):
- **Frozen per game:** at creation the server copies its pack into
  `<dataRoot>/content/<packId>/` (`FrozenContentPack`, staged and renamed) and only loads it from
  there. `ContentPackHash` (SHA-256 over `ContentPackLoader.FILES`, line endings and BOM
  ignored) identifies it; the game info sends id and hash. Freezing keeps the open turn's
  actions, in-flight state and every player's numbers consistent.
- **When the client's pack differs** it warns and uses its own copy. The download (G1) is
  deferred (user, 2026-10-06): content changes ship as a new server version, and the login
  version check makes every client update.
- **Growth:** one main pack grows in `assets/content/`, renamed `standard` once it covers more
  than the early era. A scenario is a pack plus a declared goal list.
- **Fixes in a running game:** code fixes ship with a server update between resolutions (past
  turns are stored, not recomputed). Content fixes are an explicit host step between
  resolutions, refused unless every referenced id still exists, in-flight missions keep their
  seasons and slots, and the open turn's actions still validate. Numbers may change; structure
  in use may not.
- **Display text** (G4, 2026-10-05): `strings.json` names every id, with descriptions and history
  notes. It is deliberately **not** part of the frozen, hashed pack, so text fixes reach running
  games; `ContentStringsTest` checks coverage, lengths and characters. Configuration names are
  relative to their program ("Orbital flight"); `ContentNames.of` gives the full name. Steps
  are keyed by step type; a configuration can override single steps.

**Built** (each screen's choices are in its `ui-spec.md` "Implemented" note):
- Connect and Join Game; the Space Complex hub; Headquarters (programs, then mission planning
  up to a real launch); the VAB; the SET Centre; Mission Control (controllers, scheduled and
  active missions, scrub, cancel, re-staff); the Astronaut Complex; the Public Affairs Office;
  the turn report and replay viewer (G2); the Museum, Settings and the game result (G3, G10).
- **Client structure:** `ClientSession` (credentials, state, game info, pack, plan draft),
  `ServerGateway`/`NetworkGateway`, `SessionScreen` → `ChromeScreen` (toast, dialogs,
  submit-then-rebuild, chrome), screen logic in `client.view` (tested), widgets in `client.ui`.
- **Game info** (G1): `GameInfoRequest`/`Response` (pack id and hash, declared goals, open turn
  and date, next resolution and the server's clock, the result with winners' agencies and
  directors, dev mode).
- **Dev mode:** `ResolveTurnNowRequest` resolves the open turn at once (refused unless
  `devMode`), without touching the schedule.
- **Personnel:** portraits and matching names (§2.2(c)); 128 px greyscale portraits in
  `assets/ui/portraits/`, no atlas (few on screen at once). Laika on the memorial (§2.5(f)).
- **First play-test feedback** (2026-10-05): the 50 Hz server tick (§3.3), a waiting chip and
  hover tooltips (`ui-spec.md` components 16 and 17; text in `client.view.Tips`), BASPM's basic
  training, the rocket screen's explanations and shared-reliability column, navigation after
  opening, Flight Director sorting, R&D auto-assign with a projected-gain bar
  (`rules.ResearchPlanning`), building markers on the hub.
- **Goal mission patches** (2026-10-06): generated per goal (§3.5) and shown in the Museum at 64
  px (`ui-spec.md` component 8).

### Backlog

**The decisions checklist's code work is done (2026-10-06):** the conversion rate, the pool of
10, the once-per-season salary limit, manual replay stepping, credits in Settings, the signing
bonus, the budget schedule in the game info, the lost-passenger report line, Korabl-Sputnik 2's
dogs, the animals that came home, and the space-race news (each recorded in its section above).
**Reset the dev data root** before the next play test: the pack's hash changed with the new
passengers.

**PR duty for astronauts** is built too (2026-10-06, §2.2(c)), with 39 press stories, no repeats
until all have run, and effects beyond prestige.

**Concept chips** (2026-10-07, `ui-spec.md` component 19): specified, with the early era's
concept audit (§1.1); waiting for the user's mockup, then the build (`concepts.json`, the chip
row on §4.5.1, §4.6.4, §4.6.6/§4.6.7, the "new" dot). Otherwise next is the next play test.

### Later

- **M6 — Deployment: built 2026-10-06, no release cut yet.** The jpackage client zip (built and
  launched locally, also under a pinned version as CI will), the server Docker image with a
  bind-mounted data root (`deploy/docker-compose.yml`, unverified until its first CI run), and
  the two tag-triggered workflows. The packaged exe is DPI-unaware by default (Windows scales
  it); the user is judging the fonts in both modes. Before the first real release: decide
  whether the repo goes public (a private repo makes `update.cmd` and the NAS's image pull need
  a GitHub login) and the first version number.
- **M7+ (order open):** interaction beyond passive competition (§2.9); the rest of the content
  tree beyond the early era; the rest of the Museum encyclopedia (§1.1; E-01 to E-05 built 2026-10-06/07); a game-host role and UI;
  validating the `WEEKDAYS` profile with the colleagues' group.
- **Idea to revisit: launch latitude as a mechanic** (user, 2026-10-07). Give the agencies'
  launch sites their real latitudes (Cape Canaveral about 28.5° N, Baikonur about 46° N) and
  let that matter: Earth's spin gives an eastward launch a free push of about 410 m/s at the
  Cape against about 320 m/s at Baikonur, and no orbit can be tilted less than its site's
  latitude. Possible handles: a site factor on a rocket's LEO and TLI payloads, a site per
  agency (later perhaps more sites to build), orbit-inclination requirements on some goals.
  Not designed yet; explainer E-14 (launch latitude) would explain it once it exists.
