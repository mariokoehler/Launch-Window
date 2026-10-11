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
      on with the sustainer. The Atlas's tank walls were 0.36 to 0.94 mm of stainless steel
      (sourced 2026-10-07 for E-19), so "thinner than a coin" holds.
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
  - **E-06 to E-19 are built** (mocked up and built 2026-10-07 from the user's brief), one per
    candidate in the concept audit below: Weightlessness, Heat shields, Reliability, Launch
    escape, Coming home, Tracking, Space environment, Human rating, Launch latitude, Max Q (one
    sheet), Power, Guidance, Orbital decay and Balloon tanks (one sheet). Sheet titles in
    `ui-spec.md` component 18. E-14's "In Launch Window" strips say the latitude is already
    counted in every rocket's lift, since each side's rockets only ever fly from their own site
    (user, 2026-10-08, when the latitude mechanic was dropped).
  - **E-20 to E-30 were mocked up and then checked and built (2026-10-09)**, for the full content
    pack: Three ways to the Moon, Docking, Spacewalks, Launch windows, Gravity assists, Landing on
    other worlds, Far from home, Pictures before digital cameras, Moon rock by robot, Rocket planes,
    Around the Moon and back. The checks are in the "Explainers for the standard pack" item below
    (sheet titles and the changes from the brief and the mockups in `ui-spec.md` component 18).
  - **E-06 to E-19, checked (2026-10-07).** Corrections to the mockups' text:
    - E-06: Gagarin ate paste from tubes on Vostok 1 (whether he drank isn't sourced, so the
      sheet says only that he ate). Gravity at 300 km is 91 % ((6,371 / 6,671)²). Titov's
      Vostok 2 (6 to 7 August 1961), about 25 hours, first sleep and first space sickness;
      Glenn's apple sauce (February 1962); parabolic flights about 25 s each.
    - E-07: only Mercury's orbital capsules had the glass-fibre-and-resin ablative shield; the
      suborbital ones used a beryllium heat sink (said now). Allen and Eggers' blunt body at
      NACA Ames, early 1950s; about four minutes of blackout; about 8 g from orbit.
    - E-08: 0.95^11 is about 57 %, 0.99^11 about 90 %, 0.9^11 about 31 %; Vostok's and
      Mercury's orbital flights have 11 steps in the pack. Belka and Strelka (19 to 20 August
      1960), Ham (31 January 1961).
    - E-09: MA-3 (25 April 1961) was destroyed about 43 s in, the tower pulled the capsule
      clear, and the same capsule flew again on MA-4 (13 September 1961) as No. 8A. "Would have
      survived" became "would very likely have survived".
    - E-10: drogue at about 6.4 km, main at about 3 km, landing bag about 1.2 m; Vostok's pilot
      ejected at about 7 km. Liberty Bell 7 (21 July 1961) was the second crewed Mercury
      flight; raised in 1999.
    - E-11: a pass at 200 km lasts at most about 7 minutes (worked out). Mercury's network: 18
      ground stations plus two ships (Rose Knot Victor, Coastal Sentry Quebec) by some counts,
      18 in all by others, so the sheet says "stations on land, plus two ships". Guier and
      Weiffenbach at Johns Hopkins, then Transit.
    - E-12: the inner belt's drawn top was 6,000 km; NASA gives about 1,600 to 13,000 km and
      others about 1,000 to 12,000, so the band and label now read 1,000 to 12,000 km. Explorer 1
      (31 January 1958, 358 by 2,550 km); Explorer 3 (26 March 1958) confirmed.
    - E-13: Mercury-Redstone's lengthened tanks (about 20 s more burn) and abort sensing are
      right; the mockup's "a gentler ride" isn't sourced and became "a safer fuel": alcohol
      instead of Jupiter-C's more toxic hydyne. Of the five uncrewed Mercury-Atlas flights two
      failed and three succeeded (MA-5 was cut short, so not "as planned"). MA-1's fix was a
      steel "belly band" on the adapter.
    - E-14: 465 m/s times cos(latitude) gives about 409 m/s at 28.5° and about 323 at 45.9°.
      Vostok's orbits were tilted about 65° because Baikonur couldn't launch due east (the
      spent stages would have fallen on China), not, as mocked, for tracking coverage.
    - E-15: MA-1 broke up 58 s after liftoff, near max Q.
    - E-16: Sputnik 1 silent 26 October 1957, re-entered 4 January 1958; Explorer 1's radio to
      23 May 1958; Vanguard 1 (17 March 1958), six solar panels, solar radio to May 1964; Bell
      Labs' silicon cell 1954.
    - E-17: Redstone's jet vanes, the R-7's verniers, Jupiter-C's 11 + 3 + 1 spun stages. The
      "In Launch Window" line says Jupiter-C lifts "just enough" for Explorer 1, with no figure:
      the pack gives the satellite 11 kg (its mass, from BASPM) while its description says about
      14 kg (the real mass), so the pack disagrees with itself there (left as it is).
    - E-18: Sputnik 1 at 215 by 939 km, its core stage down about a month earlier; Explorer 1
      down 31 March 1970; Skylab 11 July 1979; the lifetime curve is a rough standard one.
    - E-19: the Atlas's skin 0.36 to 0.94 mm, kept at about 5 psi when empty; "one of the
      lightest rockets of its size" became "very light for its size".
    - "In Launch Window" lines checked against the pack: the steps named, Explorer 1 reaching
      Space environment measured (its plain flight in Earth orbit, its extended one in the
      experiments step, hence "Explorer 1's flights"), the Launch step's crew risk.
- **Explainers for the standard pack (proposed, mocked up and built 2026-10-09; `ui-spec.md`
  component 18):** the full tree brings ideas no explainer covers yet. E-20 Three ways to the
  Moon (direct ascent, Earth-orbit and lunar-orbit rendezvous), E-21 Docking, E-22 Spacewalks, E-23
  Launch windows to the planets, E-24 Gravity assists, E-25 Landing without air, or with too much
  (Moon, Venus, Mars), E-26 Far from home (power and signals), E-27 Pictures before digital cameras,
  E-28 A piece of the Moon by robot, E-29 Rocket planes at the edge of space, E-30 Around the Moon
  and back. Checked before the build (2026-10-09): Gemini 12's three outings total 5 h 30 min;
  Apollo's powered descent starts about 15 km up (50,000 ft), and Apollo 11 landed with well under
  a minute of fuel; Luna 16 drilled 35 cm in the Sea of Fertility and returned about 101 g; Luna 15
  crashed at 15:50 UT on 21 July 1969, about two hours before Eagle lifted off (the mockup said "a
  few hours"); the X-15 dropped at about 45,000 ft, burned about 80 s and flew about 10 minutes;
  Luna 3's camera had 200 and 500 mm lenses, and a spot of light from a cathode-ray tube scanned the
  developed film; Venus's 90 bar is about 900 m of sea; the planetary window marks on E-23 sheet 2
  match the launches (Venera 1 February 1961, Mariner 4 November 1964, Mars 4 July 1973). The
  sunlight shares, signal delays and "twice the energy" are arithmetic. The concept chips
  don't reach the standard pack's new programs and components yet either; the existing 19
  explainers already cover many of them (staging, delta-v, cryogenics, rocket classes).
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
      - **Engine restarts** → E-04 sheet 1: R-7 (Vostok) (its upper stage lights in flight) and
        the Vostok and Korabl-Sputnik 2 configurations by name, not the `retrofire` step, so it
        stays off Mercury, whose retropack was solid rockets.
      - **One-shot retrorocket** → E-04 sheet 2: Vostok capsule.
      - **Cryogenic propellant** → E-05 sheet 1: step `launch-preparations` (liquid oxygen
        boils away), `countdown`, every rocket.
      - **Steering jets** → E-05 sheet 2: step `reentry-preparations`, the Vostok and Mercury
        capsules.
    - *Explainers E-06 to E-19, built 2026-10-07 (chips re-homed then, see `concepts.json`):*
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
    - *Since the build (2026-10-07), `assets/content/concepts.json` is the source of truth;*
      *this audit is its rationale.*
    - *First impressions:* weightlessness and the heat shield would each attach to most
      crewed content, and "the odds multiply" ties straight to how the game resolves a
      mission, so those three (E-06 to E-08) are the first to build.
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
- **Commercial launches began with Telstar** (for the contracts, §2.11): AT&T built Telstar 1 and
  paid NASA about $3 million a launch, the first commercial payload, launched on a Thor-Delta on
  10 July 1962; with Telstar 2 in 1963 that was about $6 million for both. Sources: EBSCO's
  research starter "First Commercial Communications Satellite Is Launched", IEEE Spectrum
  ("Happy Birthday, Telstar"), Space.com ("Telstar: Satellites Beamed 1st TV Signals Across the
  Sea", the $6 million): ebsco.com/research-starters/communication-and-mass-media/first-commercial-communications-satellite-launched,
  spectrum.ieee.org/happy-birthday-telstar, www.space.com/19756-telstar.html. Fits the first contract line's history note.
- **Space insurance is as old as commercial satellites:** Lloyd's of London wrote the first
  satellite policy in 1965, pre-launch cover for Intelsat I "Early Bird", the first commercial
  satellite in geostationary orbit; COMSAT never claimed on it. Cover for the launch itself came
  later, so the contracts' insurer is a small liberty. Sources: Wikipedia ("Satellite
  insurance", en.wikipedia.org/wiki/Satellite_insurance), H. Yoshida, IISL 1993 ("first insurance
  policy for satellite was written in 1965 to provide prelaunch coverage").
- **Flying other people's experiments:** the X-15 carried micrometeorite collection pods and
  ablative heat-shield samples for Apollo (NASA, www.nasa.gov/specials/60th/x-15). The supersonic
  airliner's flight data came from the XB-70, not the X-15 (NASA's image archive, "Fleet aircraft"), so the X-15's contract clients are
  research labs and Apollo's contractors, not airliner makers.
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
- **Checked for the standard pack's history notes** (2026-10-09; the notes in `strings.json` are the
  record, these are the corrections the checking found):
  - Apollo 13's distance record (about 400,000 km) was broken by Artemis II in April 2026; the note
    says so.
  - Luna 3 was the first *Soviet* craft stabilized on three axes, not the first of all (Discoverer 1,
    February 1959); its path around the Moon was set by the launch, not a gravity assist.
  - Sputnik 2's counter did record the radiation belts, but the team blamed a solar flare; Explorer 1
    gets the discovery.
  - Kosmos 110's dogs (22 days) were outlasted by Soyuz 11 in 1971, not first in 1973.
  - Zond 5's reentry g-load has no reliable figure, so the note says "much higher than planned".
  - Mariner 7's reprogramming came from Mariner 6's polar-cap pictures; "first close-up of the pole"
    isn't sourced and isn't claimed.
  - The Venera 15 and 16 buses came from the Venera orbiters, not the landers; Luna 17 had ramps on
    two sides, not two pairs at each end.
- **Whole-crew losses:** Apollo 1, Soyuz 1, Soyuz 11, Challenger and Columbia lost everyone
  aboard, the real basis of §2.5(f). A sober note the first time it happens.
- **Spying on the Moon race** (checked 2026-10-07, for the Intelligence Office's history notes):
  - A CORONA satellite caught the first signs of a huge new construction project at Tyuratam
    (Baikonur) in 1963. In December 1967 the President's Daily Brief reported that a US satellite
    had, for the first time, seen a giant rocket on one of the two new pads: the N1. Sources:
    The Space Review, Wikipedia's N1 article.
  - In August 1968 Frank Borman was told the CIA reported a Soviet crewed lunar fly-by planned
    before year's end, as NASA moved Apollo 8 to lunar orbit. The CIA's science directorate
    credited its own reports for the change; historians (Murray and Cox) find the decision was
    driven mainly by the schedule, and NASA's own papers from that August focus on the schedule
    and don't mention the press reports of Zond 4. A good note on how sure intelligence ever is. Sources: The Space Review,
    Smithsonian Air and Space.
  - NASA was public: congressional hearings, published budgets, famous faces. The Soviet program
    was secret down to the name of its chief. Still, NASA kept plenty quiet, and the user decided
    against any secrecy bonus (§2.9). The claim that Soviet engineers learned much about Apollo
    from the US trade press couldn't be sourced (searched 2026-10-07): **dropped**, don't use it.
- **Both space programs began with poached engineers** (for poaching's history note, §2.2(d);
  checked 2026-10-08):
  - **The US:** von Braun asked to bring about 500 of his Peenemünde people; the Army agreed to
    118, and about 120 came under Operation Paperclip, most reaching Fort Bliss, Texas, in late 1945
    and early 1946. In 1950 the group moved to Redstone Arsenal in Huntsville. Sources: White Sands
    Missile Range Museum ("Operation Paperclip at Fort Bliss: 1945-1950"), German Way.
  - **The Soviet Union:** in October 1946 (Albring's memoir gives 22 October) the leading German
    rocket specialists still in the Soviet zone and their families were put on trains, without
    warning, to the USSR; the core group, Helmut Gröttrup among them, ended up on Gorodomlya
    Island in Lake Seliger as Branch 1 of the NII-88 institute, kept apart from the Soviet design
    work. Their work fed the R-1, the Soviet copy of the V-2. **Numbers disagree** (73 at first,
    "about 150", "more than 170"): say "over a hundred" or none. Sources: Wikipedia
    ("Gorodomlya Island"), War on the Rocks ("The Forgotten Rocketeers", 2019), RussianSpaceWeb.
  - **Not confirmed:** the name "Operation Osoaviakhim" for the October 1946 round-up wasn't in
    the sources found; leave it out until checked.
  - **Suggested note** (for the offer dialog): "Both sides began with poached engineers. After the
    war about 120 of von Braun's Peenemünde team came to Texas under Operation Paperclip; in
    October 1946 the Soviets put others on trains to an island in Lake Seliger, where they helped
    build the R-1, a copy of the V-2."
- **Bars (also called Chaika) and Lisichka** died when their Korabl-Sputnik launch failed on 28
  July 1960, 19 seconds in: a strap-on broke away and the capsule's parachutes opened too low.
  They're the dogs remembered on a failed Korabl-Sputnik 2 flight (§2.5(f)), as "Chaika". Sources
  differ on the first dog's name.
- **Rendezvous is counterintuitive** and **restarting an engine in zero-g is hard:** both now
  explainers (E-02, E-04 above).
- **The R-16 disaster** (Baikonur, 24 October 1960) for a later memorial or news context,
  handled with care; **Apollo-Soyuz's crew breathing nitrogen tetroxide fumes** on the way
  down (1975). Both turned up while writing E-05.
- **CAPCOM, the capsule communicator** (the user's question, checked 2026-10-08; shown on mission
  assembly's Mission control card, `HistoryNotes.CAPCOM`): the name comes from Project Mercury,
  when spacecraft were called capsules. Mercury flights were followed by tracking stations around
  the world, each with a three-man team led by a CAPCOM who talked to the astronaut overhead; some
  were astronauts (Glenn, Grissom, Shepard, Carpenter, Schirra, Cooper served; Slayton was CAPCOM at
  the Cape for Mercury-Atlas 8), many were flight controllers. Later one CAPCOM in Mission Control
  spoke to the crew for everyone, usually an astronaut, often from the backup crew, as the one best
  placed to understand the crew's situation; for the ISS, non-astronauts serve too. On Apollo 11,
  Charlie Duke was CAPCOM for the landing and stumbled over "Tranquility" in his first reply
  (transcript). Sources: Encyclopedia.com ("Capcom"), NASA's Apollo Lunar Surface Journal
  ("CapComs and others"), JSC oral history (Gene Roach, 2000), Apollo 11 Spacelog, Space.com
  (Charles Duke biography). **SET** in the game is BASPM's "scientists, engineers and technicians"
  (the help spells it out); not a history note.

### 1.2 Content & asset policy: no copying BASPM's copyrighted content

**Decided (user, 2026-09-27), a hard rule.** The repo goes public on GitHub, so anything
committed to it is redistributed.

- **Text:** never copy BASPM's strings, and never lightly paraphrase them. Write from the
  real-world facts, researched in public sources (NASA history, encyclopedias), not by
  rereading BASPM's text.
- **Art, audio, video:** nothing extracted from BASPM ever enters the repo or the game. The UI
  reference screenshots once kept in `assets-raw/baspm/ui/` were removed on 2026-10-08 (user: our
  own design had moved past them); they remain in git history. Flag any task that risks pulling an
  extracted file into the tree.
- **One exception, generated from a reference (user, 2026-10-09):** the mission components'
  blueprint drawings (`assets-raw/blueprints/`) are generated image-to-image from BASPM's R&D renders,
  each with a prompt we wrote from the render in our own words and a hand-drawn blueprint style.
  The user judged the results different enough to use. The renders themselves are read from `E:` at
  generation time and never enter the repo; only the generated drawings do. This is not a precedent
  for other art: ask before using BASPM's art as a reference again.
- **Not restricted:** the real history itself (programs, hardware, dates, people). **Numbers**
  (costs, reliabilities, prestige, budget values, training lengths) follow BASPM's data by the
  user's decision (2026-10-03, confirmed 2026-10-06): they aren't creative expression, and
  they're what makes the game feel right. Tune them from play tests.
- **Fictional programs get our own names** (user, 2026-10-03): real programs keep their
  historical names; BASPM's invented ones are renamed. BASPM's Soviet "PKA space plane"
  (`26-01`) is the **OKB-MiG E-154** (ids `mig-e-154-*`), a rocket plane in the style of its era,
  as the user's art shows it: only its name and text change, the numbers stay BASPM's (user,
  2026-10-08).

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

**(d) People worth caring about** *(user's idea, 2026-10-07; designed, not built. Decided: the
split between morale and chemistry, pair rapport that builds over years, hidden traits with
ambiguous hints, the background check through intelligence, ambitions, career records and
poaching. Confirmed the same day: none to two traits per person, chemistry's ±15 % band, and
rapport's range, drift and threshold; ambitions in every group, for some people only. The rest
are proposed defaults, unconfirmed.)* The aim: employees
stop being a commodity. Players learn who their people are, care about them, and face hard calls
such as letting a brilliant troublemaker go.
- **Morale stays personal and public** (decided). It moves with what happens to the person and
  the agency (a successful flight lifts everyone, even a quarrelsome crew), and it keeps driving
  resignation and the hiring pool's quality, as now (c).
- **Chemistry is per team and short-lived** (decided). A team is the engineers on one
  component's R&D, or the crew and controllers of one mission. Its chemistry comes from the pair
  rapport inside it plus its members' traits, and is worked out again whenever the team changes:
  a new project, a new team, a new chemistry. **It scales the team's output:** R&D gain, the
  crew's and controllers' contributions to a step's odds, and the controllers' rescue roll, within
  a band of about ±15 % (confirmed). Training is done alone, so chemistry doesn't touch it.
- **Rapport between two people** (decided) lasts across projects and years. Each season two
  people share a team, their rapport drifts toward liking or friction, biased by their traits; it
  never resets, so old colleagues meet again as friends or enemies. Confirmed: a range of −100 to
  +100, a drift of about 5 a season, shown once past ±30 ("work well together", "clash"),
  lost when either leaves.
- **Traits** (decided): **none, one or two** per person (confirmed, user 2026-10-07; default
  split 20 / 50 / 30 %, unconfirmed), hidden when hired. A person without traits is the safe,
  unremarkable hire: rapport still drifts (unbiased), chemistry still forms, the base ambition
  chance applies. A trait shows itself
  after the person has worked in teams for a while (default: 2 to 4 seasons, drawn per trait),
  or sooner when it matters (an Abrasive member's first clash), and the turn report says so
  ("After two seasons on the Atlas team, Kowalski turns out to be a workaholic"). Once someone has
  worked long enough for every trait to have shown (the end of the reveal window), their page
  says **"nothing more to discover"**, whether they turned out to have two traits, one or none. Most traits cut
  both ways. Proposed list, to cut down in play:
  - *Personal:* **Workaholic** (more R&D gain; morale falls fast when idle) · **Steady** (morale
    changes halved, both ways) · **Ambitious** (trains faster; morale falls without training or a
    new skill level for long) · **Glory seeker** (astronauts: skills grow more on flights; morale
    falls each time someone else flies) · **Perfectionist** (engineers: less R&D gain, but their
    component's failed steps are milder) · **Cool under pressure** (astronauts, controllers: a
    bonus on rescue rolls) · **Restless** (a bonus on a new project, a penalty after many seasons
    on one) · **Quick study** (more training gain).
  - *In a team:* **Mentor** (younger or less skilled teammates learn faster) · **Loner** (a bonus
    alone, a penalty in a full team) · **Easy-going** (rapport drifts up faster with everyone) ·
    **Abrasive** (rapport drifts down faster, whatever their skill) · **Must be the best** (works
    harder as the team's most skilled, sulks when not) · **Team player** (lifts the whole team's
    chemistry a little).
- **Hints at hiring** (decided): each candidate shows one vague hint from a colleague
  ("colleagues call her driven"). A telling hint is always true to one of the person's traits,
  but every hint is shared by several traits, so a player can't learn a code. There are also
  **bland hints** ("reliable", "pleasant to work with"): a candidate without traits always gets
  one, and a candidate with traits sometimes does too (default: 1 in 4), so a bland hint never
  proves a candidate has none. Proposed hints: *driven*
  (Workaholic, Ambitious, Must be the best), *calm* (Steady, Cool under pressure, Loner),
  *sociable* (Easy-going, Team player, Glory seeker), *exacting* (Perfectionist, Abrasive, Must be
  the best), *independent* (Loner, Restless, Abrasive), *keen to learn* (Quick study, Ambitious,
  Mentor), *patient* (Mentor, Steady, Perfectionist).
- **Background check** (decided): reveals one of a candidate's traits before hiring (or reports
  "no notable traits"), but only for
  an agency that keeps an intelligence service (§2.9, Intelligence: a guaranteed active check and a
  passive 20 % per candidate from level 1). Without one, the hint is all there is.
- **Ambitions** (decided, user 2026-10-07): in **every group**, but **only some people have
  one**, so there are only a few to keep track of. Traits shape who: **Ambitious practically
  guarantees one**, **Glory seeker** makes one much more likely, and others start with a modest
  chance (defaults: Ambitious 100 %, Glory seeker 70 %, anyone else 25 %, all unconfirmed). One
  ambition at a time; a fulfilled one may be followed by a new one, likelier for the Ambitious.
  An ambition shows in the turn report once the person has said so (default: after their first
  season of work). Proposed ambitions, tied to goals, flights and R&D:
  - *Astronauts:* fly in space, fly to orbit, spend a day in orbit, be first at something (a goal
    no agency has reached yet), fly on a joint flight.
  - *Mission Control:* be on the consoles for a crewed orbital flight, or for a first; reach
    Expert in their best skill.
  - *Engineers (SET):* bring a component to its R&D cap; work on the rocket or capsule of a flight
    that reaches a first; be the most skilled engineer on a component.

  **Deadlines** (user's question, 2026-10-07; defaults unconfirmed): each ambition has a
  **patience window** in seasons, shown as a number on the person's page and the personnel
  overview ("4 seasons left"). It's sized by how hard the ambition is (fly in space short, be
  first at something long; default 4 to 8 seasons, short because a default game lasts about 24)
  and by traits: **Ambitious** is impatient (shorter window, bigger loyalty gain when met),
  **Steady** waits longer. Only ambitions the agency can currently reach are drawn (a component
  that's open, a program with a fitting configuration), so the clock is fair without pauses.
  - **Fulfilled:** builds **loyalty**, and may be followed by a new ambition.
  - **Passed over** (someone else in the agency gets it: flies the flight they wanted, or the
    component reaches its cap without them): costs morale and loyalty and shortens the window,
    but the ambition stays open.
  - **No longer possible** (a rival reaches the first they wanted): a mild loss, since it wasn't
    the player's fault; a new ambition may follow.
  - **Runs out:** the biggest loss of loyalty and morale, and the person stays **disappointed**
    for a while (default 4 seasons), easy to poach; then a new ambition may follow.

  Loyalty also grows with tenure and raises, and is what resists poaching.
- **Career record** (decided): a page per person with a timeline of hiring, training, flights,
  the R&D they worked on, firsts, near misses, revealed traits, relationships and ambitions met.
  The Memorial's living counterpart; it makes a loss mean something.
- **Poaching** (decided, with the player interaction work, §2.9): a rival agency can make an offer
  to one of your people. Its chance rises with low loyalty, low morale and frustrated ambition, and
  with the offer's size; a nurtured star is hard to take, someone passed over again and again is
  easy. The target's agency is told in its turn report and can match, and because players log in
  at different times, matching has to work as a standing order ("match offers up to N %") so it
  never punishes whoever logs in less.
  - **Decided (user, 2026-10-08):** **engineers and flight controllers only, never astronauts** (a
    Soviet crew flying a NASA astronaut is silly; an engineer changing sides is history: see the
    note below). **Offers come only from a personnel file** (Intelligence level 3, §2.9): no
    file, no offer. **Defence is an agency default plus a per-person override.**
  - **First-pass rules (2026-10-08; every number and detail below is a proposed default,
    unconfirmed):**
    - **Who can be approached:** an engineer or flight controller you hold a personnel file on,
      still with that rival at the resolution, not on a mission in flight that season, with the
      agency at least 4 seasons, and not approached by anyone in the last 4 seasons.
    - **The offer:** made from the file, **one a season** per agency (no intelligence action
      used), queued and cancellable until the resolution like an operation. Its one term is a
      **raise over the person's current salary: +25, +50 or +100 %**. Nothing is paid unless they
      come: then the poacher pays the new salary from the next season, and a **signing bonus of one
      season's new salary** at once. The poacher needs room in that roster (its building's cap) at
      the resolution, or the offer lapses unanswered. An offer isn't secret: counter-intelligence
      plays no part.
    - **The defence**, a standing order:
      - **Agency default** per group: match offers up to **0 (never), +25, +50 or +100 %**.
        Default for a new agency: +25 %.
      - **Per person**: **Agency default**, **Keep at any price** (matches anything), or **Let go**
        (never matches).
      - **A matched offer** fails: the person's salary rises to the offered one for good, loyalty
        +5. Both agencies' reports say so. Matching ignores funds: a raise is a running cost, not a
        payment, and a game in debt already has its own rules.
    - **Unmatched, the person chooses.** Chance to accept, clamped to 0–90 %: base by raise (+25 %:
      15, +50 %: 30, +100 %: 50) × a loyalty factor (100 − loyalty) ÷ 50 (loyalty 50 gives × 1,
      loyalty 100 gives 0), then +20 if disappointed, +10 if morale is under 40, +10 for Restless,
      −10 for Steady. Rolled from its own salted seed, settled once a turn with the intelligence
      (`IntelligenceBoard`), never on the season's stream.
    - **A person who leaves** moves to the poacher's roster at the resolution. They keep their
      skills, traits (revealed to the new agency: only the one the file showed; the rest hidden
      again with fresh reveal points), Expert marks and career record, with a line "Joined X from
      Y". Their old agency loses their R&D slot or console assignment at once, and their rapport
      pairs. They start at loyalty 50 and keep any open ambition with its seasons left.
    - **A person who stays** (matched or turned it down) gets a career line ("Turned down an
      offer from X", "Stayed: the agency matched X's offer").
    - **Reports:** the target's turn report always names the poacher and the offer. The poacher's
      says matched, turned down, joined or lapsed.
  - **History note to go with it (verify before use, §1.1):** after the war, the US brought
    German rocket engineers over under Operation Paperclip (von Braun's team), and the Soviets
    took others to the USSR (Gröttrup's group on Gorodomlya island). Both programs started from
    poached engineers.
  - **Mocked up (2026-10-08)** on the canvas `Launch Window — Poaching` (`ui-spec.md` §4.16). The
    canvas added to the rules, all unconfirmed: a file records the person's **salary, seasons with
    the agency and the last approach** (a file card can then say why someone can't be approached
    yet, with the date it ends); "on a mission in flight" is a reason too; **the offer stays
    possible with the roster full** (it lapses at the resolution unless a place frees up); and the
    **answer range counts only what the file shows** (the loyalty band's bounds, disappointment,
    the one trait), never morale or unshown traits, and the dialog says so.
  - **As built (2026-10-08):** `rules.Poaching` (numbers, the offer, the standing orders, the
    answer, and each side's apply), offers settled with the intelligence by `IntelligenceBoard`
    (an offer is an `IntelOperation` of kind `OFFER` with its raise; it uses no action, and
    cancelling one gives none back), each side applied by its own `Intelligence` pass, and the
    report line `OfferSettled` (event and `IntelFinding` kinds appended). Choices made while
    building, all unconfirmed:
    - **Who can be approached** is decided at the resolution: the file only tells as of its date,
      so queuing checks the level, the file, the group, the raise and the one offer a season, and
      the resolution checks tenure, the cooldown, training (excluded too) and, for controllers,
      being on a mission (any assignment). An offer that fails these lapses, and the target is
      never told.
    - **One approach per agency per person, and a ceiling on matches** (the user's catch,
      2026-10-09, built the same day). A rival paid nothing for a matched offer, so one with a file
      could offer +100 % every 4 seasons to someone kept "at any price" (or under a +100 % default)
      and double their salary each time, without limit. Now:
      - **One shot:** once a person has answered an agency's offer, matched or turned down, that
        agency can never approach them again, wherever they work (`Personnel.approachedBy`, by
        player id, travels with them). A lapsed offer doesn't count: the target never heard of it.
        The poacher's file says so (`PersonnelFile.approachedByYou`): the card reads "You made them
        an offer in [date]. They won't hear another from you", queuing is refused
        (`ALREADY_APPROACHED`), and an offer from an older file lapses untold.
      - **Ceiling:** a match never takes a salary past 3 times what it was before the first matched
        offer (`Poaching.MATCH_CEILING_FACTOR`, `Personnel.salaryBeforeMatches`), and never cuts one
        raised some other way. With about ten players, nine rivals each taking their one shot at
        +100 % could otherwise multiply a salary by 512.
      - The 4-season cooldown stays, so offers can't land back to back. **Reset the dev data root**
        before the next play test (new fields on employees and files).
    - **Two offers to one person in one turn:** the highest raise settles first (then the
      agency key, then the offer id), the others lapse, whatever order agencies resolve in.
    - **The person who comes over** travels whole in the turn's intelligence file (concrete
      engineer and controller fields, never sent to a client: the report line is built from plain
      fields). They get a new id in the poacher's agency, the filed trait revealed and the rest
      hidden again (work seasons back to 0, so the reveals re-arm), idle, tenure 0, loyalty 50,
      the agency default, and the new salary; the poacher's file on them is dropped. They're on
      the roster before the budget, so the new salary is paid from that resolution; the signing
      bonus is paid at once, even into debt.
    - **The person who leaves** is taken off the roster like a dismissal (an R&D slot frees up).
      If they already left that season another way (retired, resigned), only the line remains.
    - **Career:** `JOINED` and `OFFER` lines; the 30-line cap keeps every `JOINED` line, and the
      page prefixes each earlier line with the agency it happened at.
    - **Measured:** the bot playouts are unchanged (no bot makes offers); `PoachingTest` runs the
      board and both passes through the saved findings' JSON.
    - **Reset the dev data root** before the next play test (new fields on employees, files and
      the snapshot).
- **First-pass rules for phases 1 to 3** (2026-10-07; the user accepted the proposed defaults for
  play testing; every number is ours and to be tuned):
  - **A team** is the engineers working on one component's R&D this season, or one of a mission
    in flight's two teams: its crew with CAPCOM, or its flight controllers (user, 2026-10-08; it
    was one team of crew and controllers before). Someone working alone is a team of one. A **work
    season** is a season spent in a team (R&D or a mission in flight); training and PR duty
    don't count.
  - **Rapport** is kept per pair in `PlayerSnapshot` (both ids, the rapport, a hidden affinity,
    the seasons they shared). When two people first share a team, the pair gets an **affinity**:
    a random draw from −1 to +1, plus +0.5 for each Easy-going member, −0.6 for each Abrasive
    one, +0.2 for each Team player, −0.2 for each Loner, and −0.4 if both are Must be the best,
    clamped to −1..+1. Each work season they share, rapport moves by **10 × affinity** (5 until
    2026-10-08), within
    −100..+100. It shows once past ±30 ("work well together", "clash").
  - **Chemistry** of a team is a number from −1 to +1: the average rapport / 100 over all its
    pairs (0 for a team of one), plus the team traits below, clamped. It scales the team's output
    by **1 + 0.15 × chemistry**: the component's R&D reliability gain, and the mission's crew and
    controller contribution sums (each still capped at 100). Team traits: **Team player** +0.15
    for each one in the team; **Loner** +0.6 when alone, 0 in a pair, −0.3 each in a team of three
    or more; **Must be the best** +0.2 when they're the team's most skilled, else −0.3 (R&D: the
    component's skill; missions: compared within their own group by their best skill used on the
    mission).
  - **Personal traits** change the person's own numbers: **Workaholic** R&D gain ×1.1, idle morale
    loss ×3; **Steady** the season's morale change halved (rounded toward zero); **Ambitious**
    training gain ×1.2, and −2 morale a season once 6 seasons have passed without training;
    **Glory seeker** (astronauts) flight skill growth ×1.5, and −4 morale each time a crewed
    mission of the agency launches without them; **Perfectionist** (engineers) R&D gain ×0.85,
    but while they're on its team the component's R&D cap is 3 points higher (reliability gained
    above the normal cap stays); **Cool under pressure** (astronauts, controllers) +0.03 on the
    saving throw for each one on the mission; **Restless** R&D gain ×1.15 in their first 2 seasons
    on a component, ×0.85 from their 9th season on the same one; **Quick study** training gain
    ×1.25; **Mentor** teammates less skilled than the mentor gain skill ×1.2 (R&D skill gain and
    flight skill growth). *Changed from the first list:* Perfectionist's "milder failed steps"
    became the higher cap, which is simpler and easy to see.
  - **Which group can have which trait:** Glory seeker astronauts only; Perfectionist engineers
    only; Cool under pressure astronauts and controllers; every other trait any group.
  - **Drawing traits** when a candidate is generated: 0 / 1 / 2 traits at 20 / 50 / 30 %, distinct,
    from the traits the group can have. Each trait gets a **reveal point** of 2 to 4 work seasons;
    it shows when the person's work seasons reach it. Two shortcuts: Abrasive shows when one of
    their pairs first passes −30, Easy-going when one passes +30. After 4 work seasons the person
    reads "nothing more to discover".
  - **Hints:** a candidate without traits gets a bland hint; one with traits gets a bland one 1 in
    4, else a hint that fits one of their traits at random. Hints and their traits: driven
    (Workaholic, Ambitious, Must be the best), calm (Steady, Cool under pressure, Loner), sociable
    (Easy-going, Team player, Glory seeker), exacting (Perfectionist, Abrasive, Must be the best),
    independent (Loner, Restless, Abrasive), keen to learn (Quick study, Ambitious, Mentor),
    patient (Mentor, Steady, Perfectionist); bland: reliable, pleasant to work with, hard-working.
  - **What the player sees stays honest about what's hidden.** The client's previews (the R&D
    board, mission assessment and staffing) count only revealed traits and shown relationships, and
    say "unknown factors" when the team's real chemistry may differ; the season resolves with the
    real values. **The server strips hidden information** (unrevealed traits, affinities, rapport
    not yet shown, candidates' traits) from the player state before sending it, so it can't be read
    out of the client.
  - **Turn report** lines: a trait revealed; a relationship shown (passed ±30); with the person,
    the trait or partner, and what they were working on.
  - **As built, phases 1 to 3 and the personal traits** (2026-10-07; rules only, the screens wait
    for mockups): `model.Trait`, `TraitState`, `Rapport`; `rules.Personality` (trait draws, hints,
    affinity, `forClient`), `rules.Teams` (the TEAMS phase after RESEARCH), `rules.Chemistry`,
    `rules.TraitEffects`; events `TraitRevealed` and `RelationshipShown` with turn report lines
    (`client.view.TraitTexts` has the words). Notes on how it came out:
    - **Separate random streams:** traits come from a stream seeded by the agency and the
      candidate's id, and a pair's affinity from one seeded by the agency and both ids, so no
      other random draw moved (seeded tests and screenshots unchanged).
    - **Teams are captured** after departures and before missions resolve, because a finishing
      mission releases its crew; the TEAMS phase then skips anyone lost meanwhile. A person's
      **project** (what they last worked on, and for how many seasons in a row) feeds Restless.
    - **"Unknown factors"** shows while any member is still inside the reveal window (fewer
      than 4 work seasons). A pair's rapport below ±30 isn't shown and counts as known noise: it
      moves chemistry by at most ±0.3 (±4.5 % output).
    - **The report's exact step odds** (`MissionStepResolved`) include hidden chemistry. The
      client never displays them, so only someone reading the report's raw JSON could work it out;
      accepted for now.
    - **Morale effects, first pass:** Workaholic, Steady and Ambitious act on the season's own
      morale change only, not on salary changes, press stories or random events.
    - **Early tells are welcome** (user decision 2026-10-07): Workaholic and Steady change the
      visible morale numbers before they're revealed, so an attentive player can guess them.
      That rewards perception; hidden traits don't have to leave no trace.
    - **Perfectionist's cap:** reliability gained above the normal cap stays when they leave; the
      rest of the team is then released once, as at any cap. Every rule that checks the cap reads
      `TraitEffects.effectiveCap` (R&D, the release after a flight, and the refusal to assign at
      the cap, which counts only a revealed Perfectionist so it never gives a hidden one away).
      The client's "cap reached" captions and the reliability bar's cap marker still show the
      normal cap; they change with the screens.
    - **Glory seeker** loses morale only when left on the ground: not on that mission (crew or
      CAPCOM) and not assigned to another one.
    - **Hidden data audit:** only the player state carried hidden traits; the turn report, the
      rivals and the game info take names, goals and directors only.
    - **Measured** (`ContentPackPlayouts.people`, 200 bot games of 24 seasons): 8.7 traits revealed
      and 5.1 relationships shown per game (2.2 friendships), from 8.5 pairs. The bots finish in
      55.0 seasons on average, against 56.7 before personalities; step outcomes within noise.
  - **Screens mocked up (2026-10-07)** on the canvas `Launch Window — People`: components 20 to 22,
    the person page in four states, the R&D board and assign dialog, assemble mission and its
    picker, the roster, the hire screen and the turn report (`ui-spec.md`, the note after
    component 22). Their look is unconfirmed. Two findings for the rules, from working out the
    samples. **Decided (user, 2026-10-08) and built the same day:** `RAPPORT_DRIFT` 10, so the
    most strongly matched pair (affinity ±1) shows in 3 seasons, before a reveal due in 4
    (`TeamsTest`); and a mission as two teams, the crew with CAPCOM and the controllers. Each
    team's chemistry scales its own people's share of a step: the crew's the crew sum and
    CAPCOM's share of the controller sum, the controllers' the rest (`Chemistry.MissionChemistry`,
    used alike by the resolution and `MissionAssessment`). Rapport drifts only within each team.
    Trait effects that count heads now count per team: a solo cosmonaut with no CAPCOM is a team
    of one, so a Loner there gets +0.6 (+9 % output) instead of −0.3 among eleven. Cool under
    pressure and Mentor still read the whole mission. The findings were:
    - **The Abrasive and Easy-going shortcuts can never fire first.** Rapport moves at most
      `RAPPORT_DRIFT` (5) a shared season and shows past `RAPPORT_SHOWN_AT` (30), so a pair needs at
      least 6 shared work seasons to show (30 ÷ 5 at the largest affinity; the mockup said 7).
      Every trait shows by `REVEAL_MAX_SEASONS` (4) work seasons anyway, so the first clash or friendship always comes after the normal reveal. To make
      the shortcuts matter, the reveal window would have to be longer, or the drift faster.
    - **On a mission, relationships barely count.** Chemistry averages rapport over all pairs, and a
      crewed Vostok or Mercury flight has 11 people (55 pairs), so one shown clash at −34 moves the
      mission's chemistry by about 0.006 (0.1 % output), while one Loner moves it by 0.3. If crew
      friendships and feuds should matter, weight the pairs involving the crew, or count the crew
      and CAPCOM as their own small team.
  - **The screens** (built 2026-10-07 from the People canvas; `ui-spec.md` components 20 to 22,
    §4.14): trait chips on rosters and pickers, a person page, chemistry on the R&D boards and
    Assemble mission with previews in the pickers, hire hints, and people news in the turn
    report. Every number they show is the known-only one (`client.view.People`).
  - **The memorial's person page waits for phase 4.** A memorial entry keeps a name, age, cause
    and portrait, but no id, skills or traits, and rapport with the dead is dropped. Default:
    build it with the career record, capturing the id, skills and *revealed* traits at death
    (never hidden ones, so `Personality.forClient` needn't change).
  - **The Background check** stays out until espionage; the hire rows leave room for it.
- **First-pass rules for phase 4** (2026-10-07, written while the user was away). Ambitions, loyalty, the
  career record and the memorial's person page. **The numbers and reinterpretations below are
  confirmed as defaults (user, 2026-10-08)**, to tune after the play test, except the windows,
  which are doubled (see "Measured" below).
  - **Who gets an ambition:** decided when a candidate is drawn, from the candidate's own seed
    (Ambitious 100 %, Glory seeker 70 %, anyone else 25 %, as above). It stays hidden, and the
    server strips it, until the person **says so after two seasons with the agency, once out of
    basic training** (astronauts train for four). Which ambition is drawn then, from those the agency can reach
    now, again from the person's own seed. If none can be reached, it waits a season and tries
    again.
  - **The ambitions and their checks** ("flew" means: on a mission in flight whose season ended
    without a failed step; "this season" is the season pass):

    | Group | Ambition | Fulfilled when | Passed over when | Reachable while | Window |
    |---|---|---|---|---|---|
    | Astronaut | Fly in space | flew as crew on a crewed mission | another astronaut flew as crew | an open program has a crewed configuration | 10 |
    | Astronaut | Fly to orbit | flew as crew, crewed orbital launch | another astronaut did | a crewed orbital configuration is open | 12 |
    | Astronaut | Spend days in orbit | flew as crew in the second season or later of a crewed orbital mission | another astronaut did | a crewed orbital configuration of two seasons or more is open | 14 |
    | Astronaut | Be first at something | aboard (crew or CAPCOM) a mission that settles a goal first in the game | someone else in the agency was | an open configuration still grants a goal no agency has | 16 |
    | Astronaut | Fly on a joint flight | flew as crew on a configuration of two launches or more | another astronaut did | such a crewed configuration is open | 14 |
    | Flight controller | Be on the consoles for a crewed orbital flight | flew on a control slot of a crewed orbital mission | another controller did | a crewed orbital configuration is open | 12 |
    | Flight controller | Be on the consoles for a first | on a control slot of a mission that settles a goal first in the game | another controller was | as "be first" | 16 |
    | Flight controller | Become an Expert | earns the Expert trait in their best skill | (never) | not yet Expert in it | 16 |
    | Engineer | Bring a component to its R&D cap | on the team when *that* component reaches its cap | it reaches the cap without them | the component is owned, active and below its cap | 12 |
    | Engineer | Build hardware for a first | a component they ever worked on flies on a mission that settles a goal first in the game | (never) | as "be first" | 16 |
    | Engineer | Be the agency's best | the most skilled engineer in their own best skill (at least three engineers) | (never) | not already the best | 12 |

    *Reinterpreted from the list above:* "spend a day in orbit" became days (a second season)
    in orbit, since the game counts seasons, not hours; "be the most skilled engineer on a
    component" became the agency's most skilled engineer in their best skill. The component for
    "bring a component to its cap" is the one they work on when they say it, else a random owned
    one below its cap.
  - **A flight passes others over once,** in its first season (its second, for days in orbit),
    however many seasons it lasts.
  - **The window** counts down once the ambition is said: the table's seasons, −1 for Ambitious
    (at least 3), +2 for Steady. Each season, in this order: fulfilled; else passed over; else no
    longer possible (it can't be reached any more: a program closed, no goal left unreached,
    the component capped or gone); else the window counts down and may run out.
  - **Outcomes** (morale, loyalty): **fulfilled** +5, +15 (Ambitious +20); **passed over** −5, −5,
    and the window shortens by one more season, the ambition stays; **no longer possible** −2, −3;
    **runs out** −10, −15, and the person stays **disappointed** for 4 seasons. After fulfilled, no
    longer possible or disappointment, a new ambition may follow: Ambitious 75 %, anyone else 30 %.
  - **Loyalty**, 0 to 100: starts at 50 on hiring, +1 for each season with the agency, and moves by
    half a department salary change (+10 % salary: +5 loyalty; a cut takes it down the same way).
    **Loyalty and disappointment do nothing else yet:** the design ties them to poaching (phase
    5); they show on the person page and in intelligence personnel files.
  - **The career record**, per person, newest first on the page, at most 30 lines (the oldest
    drop off, the hiring line always stays): hired (with the colleague's hint); training done;
    each R&D stint when it ends (component, seasons, reliability gained; the one still running
    shows as "so far"); each flight or console shift when the mission ends (configuration, role:
    crew, CAPCOM or flight controller, and success or the step it failed at); a goal first in the
    game; traits that came out; relationships shown; the Expert trait; ambitions said, met,
    passed over, impossible and run out.
  - **The memorial** keeps the person's id, skills, *revealed* traits (never hidden ones), the
    year they joined, and the career record at death, so its person page reads like the living
    one.
  - **Rules as built** (2026-10-07; `rules.Careers`, a `CAREERS` phase after `TEAMS`; `model.Ambition`,
    `CareerEntry`, new `Personnel` and `MemorialEntry` fields; event `AmbitionSettled`). Flights are
    captured with the teams, before missions resolve; every draw uses the person's own seed. With
    every ambition chance at 0 the bot playouts match the old output exactly, so the rest of the
    phase changes nothing. Careers stay in the player state: a full agency with every record full
    is about 22 KB zipped (`StateSizeTest`). The hiring line is written at the first resolution
    after hiring, and an R&D stint's reliability is what the person added (`ResearchCompleted`).
  - **Measured, and worth deciding** (`ContentPackPlayouts`, 200 bot games, after counting a pass-over
    once per flight and waiting out basic training): over 24 seasons the bots' people said 1,852
    ambitions; **122 were fulfilled and 1,310 ran out** (astronauts: none fulfilled, 107 ran out,
    50 became impossible). Over 80 seasons, before those two fixes, it was 520 fulfilled to 2,429.
    Morale at season 24 averages 81 (lowest 36), loyalty 65. The bots are slow (a first crewed orbit around season
    56), but people even in a quick human game will mostly see ambitions run out: crewed flights
    are few, and 5 to 8 seasons of patience is short. Options: longer windows (say 10 to 16), a
    window that only counts down while the ambition can be acted on (a crewed configuration ready
    to fly, an R&D slot on the component), or milder run-out losses. **Decided (user, 2026-10-08):
    longer windows, the table's seasons doubled (10 to 16)**, with Ambitious still −1 and Steady
    still +2. Built the same day, with the faster rapport and the split mission teams; the same
    200 games of 24 seasons then gave 1,790 ambitions said, **253 fulfilled, 486 run out** and 108
    no longer possible (about 1 to 2 instead of 1 to 11); the rest were still open at season 24.
    Astronauts still fulfilled none (104 no longer possible, 22 run out): the bots fly their first
    crewed mission long after season 24. 8.8 traits revealed and 6.2 relationships shown per game
    (2.5 friendships); morale at season 24 averages 82 (lowest 36), loyalty 67; the bots finish in
    56.4 seasons.
  - **Screens as built** (2026-10-07): the person page's story column (ambition, career record)
    and loyalty bar, the memorial's person page from the Museum, ambition lines on the rosters and
    in the turn report, and loyalty and the said ambition in intelligence personnel files
    (`ui-spec.md` §4.14, §4.15). **Reset the dev data root** before the next play test (new saved
    fields).
- **History notes to go with it** (checked 2026-10-08):
  - **Gagarin over Titov:** both were picked for their training results and for being short enough
    for the cramped Vostok. In a secret vote, 17 of the 20 cosmonaut candidates named Gagarin to fly
    first. Kamanin, who ran cosmonaut training, wrote in his diary on 5 April 1961 that he was
    still torn, and that what kept him from choosing Titov was the need for the stronger man on the
    next, day-long flight: Titov flew Vostok 2 in August 1961, over 25 hours. Gagarin's peasant
    background and easy charm are often given as reasons too, but that's later journalism and
    biography (Jenks), not the record: say "it was said" if used. Sources: Wikipedia ("Vostok 1",
    citing Kamanin's diary), ESA ("Yuri Gagarin"), AFP via Space Daily.
  - **Slayton's crews: only partly confirmed.** Deke Slayton, grounded in 1962, ran crew
    assignments as Director of Flight Crew Operations from late 1963 to 1972 and had the deciding
    say on the Gemini and Apollo crews, including the first Moon landing's (NASA history). That
    he weighed who worked well together is said only in secondary accounts (forums, fan wikis); his
    memoir *Deke!* (1994) is the primary source and hasn't been checked. **Use only the confirmed
    part** ("one man chose the crews") until *Deke!* is read. Also from his memoir, via a secondary
    source: the Gemini 9 crew's death in 1966 moved the backups along, which is how Buzz Aldrin came
    to fly Gemini 12 and, Slayton said, to be in line for the first landing.
- **Phases:** (1) chemistry scales output; (2) traits, reveals and hints; (3) rapport and the
  chemistry preview in the assignment dialogs; (4) ambitions, loyalty and the career page; (5)
  poaching and the background check, with espionage.
- **What it touches:** new fields on `Personnel` (traits, revealed traits, ambition, loyalty) and
  a rapport table in `PlayerSnapshot` (a dev data reset); new `ResolutionEvent` types, appended
  (§3.7); screens: revealed traits on the personnel overviews, the hint on the hire screen, a
  chemistry preview where teams are formed (R&D slots, mission staffing), the career page, and
  the turn report's lines. Effects read their numbers from the rules' constants (`Tips`).
- **Open:** which traits suit which group, whether a trait can be misleading at first, the
  ambition chances, list, windows and losses, and when an ambition shows.

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
- **The hardware that flies is the copy built at launch** (user decision 2026-10-07; BASPM reads
  the one shared value at every step instead, `MissionSimBaseScreen.cs:917`). A launch records
  each component's reliability (`InProgressMission.launchReliability`), and every step of that
  flight reads it (`MissionProgress.flyingReliability`). Ground R&D, a random setback, and every
  flight's own results, failures included, change only the component on the ground, for later
  missions; never a copy already flying, whether on this mission or one in parallel. Missions
  saved before this read the component as it is now. The client's odds and component list for a
  launched mission read the launch values too. Today it only matters for the two extended
  satellite missions (three seasons) and for two flights sharing a component in one turn; the
  bot playouts, which fly neither, came out identical.

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

**(d) Stories** (the user's request, 2026-10-10: the events "give you no context at all"). Each
applied event carries a little story: a headline and a sentence or two that say what happened, then
the event's own line with the figures. 105 stories in `assets/content/event-stories.json`
(`EventStories`), our own text, fictional, and neutral between the agencies. Sized per event from the
bot playouts (`ContentPackPlayouts`, which now counts random events): a Moon race with side missions
runs about 150 seasons and sees about 30 good and 8 bad events, the plain bot's 237 seasons 47 and
12; prestige and R&D boosts come 10 to 17 times a game each, reduced expenditures 6 to 10, every
other event at most about 3. So: 18 stories each for prestige and R&D boosts, 12 for reduced
expenditures, 6 for morale boosts, 5 for extra funding, 4 for reliability setbacks and 3 for every
other bad event (`EventStories.SIZED`). No player sees a story twice until every story of its event
has run; then that event starts over (`PlayerSnapshot.usedEventStoryIds`). The pick uses its own
salted seed (agency, turn, event), so the season's draws, and the playouts, are unchanged. The
event (`RandomEventOccurred`) and the news log entry store the story's id and the struck person's
name, since the person may be gone when the report is read; the text stays in the client. Placeholders:
`{name}`, `{component}` (reliability setbacks), `{group}` (morale). The same pass fixed the event
lines' figures: prestige, cost and funding magnitudes are percentages, and were worded as points
and dollars.

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
  idea. Two pieces are already designed for it (§2.2(d), user 2026-10-07): an **intelligence
  service** that can run background checks on job candidates, and **poaching** other agencies'
  employees, with matching as a standing order.
- **Intelligence** (designed with the user, 2026-10-07; every number is ours, a default and
  unconfirmed). Espionage here buys **information, never damage**: **no sabotage** (user,
  2026-10-07), since a rival breaking your hardware overnight can't be answered until morning and
  a player-caused death would kill the fun. No BASPM precedent (its source has no espionage).
  - **Async rules it keeps:** every operation is settled in the season pass (except the background
    check below), defence is passive, and the service's level is a standing order. Nothing asks a
    player to react, so logging in less never costs anything here.
  - **The Intelligence Office**, an **eighth hub building** (user, 2026-10-07), because the feature
    will grow. It holds the level dial, the operations, the folders on rivals, and later perhaps
    intelligence officers as a fourth roster (the building's level would cap it, like the others).
    No building upgrade for now. One name for both archetypes. The user draws its hotspot polygon
    (`INTELLIGENCE_OFFICE`) on both hub images.
  - **The level** (0 to 3) is a **budget dial**, no staff for now. The player sets a **target
    level**; at each resolution the level moves **one step** toward it (user, 2026-10-07: a
    network takes time to spool up and to dismantle), so a change only ever takes effect next
    season and nobody can sit at 0 and spike to 3 when a need arises. The level steps first in the
    season pass, so the candidate pool drawn that resolution and the next season's actions use
    the new level. **Cost per season** at the level in effect, paid with the fixed costs: 0 / 150
    / 350 / 700 (against a starting funding of 3,000). Winding down costs the levels on the way.
    If funds don't cover it (in debt), the level drops one step that season.
  - **What each level brings.** A level keeps everything the levels below it bring. **Actions per
    season** equal the level; unused ones don't carry over. **Passives** are independent of the
    actions: using an action never cancels a passive roll (user, 2026-10-07).

    | Level | Active (unlocked) | Passive |
    |---|---|---|
    | 0 | none | none; a small baseline counter-intelligence |
    | 1 · Security office | **Background check** on one candidate | **20 %** per candidate to reveal a trait; counter-intelligence I |
    | 2 · Analysis section | **Agency assessment** of a chosen rival, one area | **25 %** per season: a tidbit about a random rival (R&D or plans); counter-intelligence II |
    | 3 · Field network | **Personnel file** on a rival's employee | the tidbit chance rises to **40 %** (user: fine to start with, 2026-10-07) and can come from any area, people included; counter-intelligence III |

    The ladder is people, then agencies, then rivals' people (user, 2026-10-07); level 3's
    personnel file is what makes a poaching offer an informed one.
  - **Background check** (the decided feature above, now concrete). The **active check answers
    at once**: the server replies during the season with one of the candidate's traits, or "no
    notable traits", and the action is used. It's desk work, so instant fits, and an answer at
    resolution would arrive just as the pool refreshes. It never fails and isn't against a rival,
    so counter-intelligence plays no part. A second check on the same candidate (a second
    action, level 2 or more) reveals another trait if there is one. **The passive 20 %** is
    rolled once per candidate when the pool is drawn, from the candidate's seed, so it shows the
    moment the pool appears and can't be rerolled. Both report "no notable traits" too, so a
    missing reveal never hints at none. A trait found this way stays revealed after hiring.
    **The active check is the game's first action that changes state mid-turn,** so it is
    pinned down: which trait it reveals comes from the candidate's seed; the answer is saved on
    the candidate, so asking again returns the same answer and a retried resolution never
    changes it; it can't be cancelled or refunded (unlike queued operations); it counts against
    the season's actions even if the level drops at the resolution; and the server's stripping
    lets that one revealed trait through.
  - **Agency assessment** (level 2): one action looks at **one area** of one rival, settled at
    resolution, the result a **dossier dated to that resolution** that stays in the rival's folder
    until a newer one replaces it:
    - **R&D:** the rival's components with reliability as **ranges** ("55–70 %").
    - **Programs and plans:** open programs, missions in flight, **R&D focus** (which components
      their engineers work on, with how many), **hardware not flown yet** (owned payloads never
      carried on a successful flight), and the rival's **Intelligence level** (user, 2026-10-07), so
      one assessment tells a player how hard the rival is to spy on before spending more. The
      mockup's "launches scheduled for next season" can't exist: a mission scheduled during a
      season launches at that season's resolution, so a dossier made there would always find none.
      The user chose both replacements (2026-10-07). A rival's plans can still change after the
      report: intelligence can be wrong, and that is the rival's counter without having to react.
    - **Personnel:** roster sizes and average skill per group.
  - **Personnel file** (level 3): one rival employee, in a group the player picks: the most
    skilled person there without a current file, or a person the player names from an earlier
    file. It shows name and portrait, skills as ranges, a loyalty band (low, middling, high; needs phase 4's loyalty), their ambition and
    whether they're disappointed, and one of their traits.
  - **Odds against a rival's counter-intelligence** (the target's level, 0 to 3): a targeted
    action succeeds at **85 % minus 15 points per level** of the target's counter-intelligence
    (85 / 70 / 55 / 40). Ranges are tighter the higher the spy's level is over the target's:
    half-width **5 + 5 × (target level − spy level + 2)** points, at least 2 and at most 15. A
    failed action learns nothing, and is **caught** with **10 % + 10 % per target level**: the
    target's turn report names the spying agency, the spy loses **20 prestige**, and the spy's
    report says the operation was blown **and what the target's counter-intelligence level was**
    that season (user, 2026-10-07: getting caught should still teach something). Catching works
    the other way too: the catcher learns the spying agency's Intelligence level. A level learned
    either way goes into the rival's folder, dated like a dossier. **The odds a player sees:**
    exact ("55 %, their counter-intelligence was level 2 as of Spring 1962") once a dossier or a
    catch has shown the level, otherwise the full range, 40–85 %. Passive tidbits roll against the same counter-
    intelligence but are never caught (no one is sent). No secrecy bonus for either archetype
    (user, 2026-10-07: NASA didn't spill everything either).
  - **Turn report:** a new "Intelligence" section with reveals, dossiers, tidbits, catches on both
    sides, and level changes.
  - **Hidden information stays on the server:** a dossier holds only what the server granted
    and is kept in the player's own state; a rival's real values never travel to the client, as
    with traits.
  - **As built (2026-10-07):**
    - **Rules** (`rules.Intelligence`, `rules.IntelligenceBoard`; state in
      `PlayerSnapshot.intelligence`): a new `INTELLIGENCE` phase of the season pass, before the
      budget, applies the agency's findings, clears the queue, frees the actions and steps the
      level (the budget then charges the new level, `BudgetProgress.RunningCosts.intelligence`).
      The passive candidate roll and every operation, catch and tidbit draw from their own salted
      seeds, never the season's stream: the bot playouts match the old baseline exactly.
    - **Settled once a turn** (`TurnScheduler`): from every player's state with the turn's
      actions applied, before anyone's pass, and kept in `intelligence/turn-N.json`, so a retry
      after a crash applies the same findings. An operation queued after that file was written
      comes to nothing and says so in the report ("queued too late").
    - **Rivals are named by an opaque key** (`GameSeeds.rivalKey`, carried as
      `RivalSummary.playerId`): operations and folders use it, and account ids never leave the
      server.
    - **Tidbits** are one fact from one dossier area; a rival's event history isn't read.
    - **Folders are capped** (12 tidbits, 8 personnel files, 10 catches per rival, oldest
      dropped): they travel in the player's state, under Kryo's 64 KB object buffer. Five rivals
      at every cap add about 10 KB unzipped (`IntelligenceWireTest`). A full agency (every roster
      at its top cap, 300 rapport pairs, five full folders) measured 34 KB as an object graph, so
      the state now goes over the wire as gzipped JSON like the turn report: about 17 KB
      (`StateSizeTest`, 2026-10-07). The bots' states stay under 8 KB over 80 seasons
      (dev-tools `StateSizes`), and under 22 KB on the standard pack, played to the Moon by
      the bot that works ahead over 240 seasons (2026-10-09, 200 games, 33 % of the buffer;
      `-Dexec.args=standard`).
  - **Balance check** (dev-tools `IntelligencePlayouts`, 2026-10-07: four agencies, each spending
    every action every season, 24 seasons, 1,000 games; only the intelligence rules run). Per
    agency over the 24 seasons:
    - **Cost:** level 2 about 340 a season (8,200 in all), level 3 about 660 (15,900), against a
      starting bank of 25,000 and funding of 3,000 a season.
    - **Against a mix of levels 0 to 3:** level 2 gets 29 dossiers and 3.5 tidbits and is caught 4.6
      times (92 prestige); level 3 gets 31 dossiers, 15 personnel files and 6 tidbits for the same
      catches. Level 3 costs twice as much mainly for the files, which only pay off once poaching
      exists. 65–70 % of operations succeed, 7–10 % are caught.
    - **An arms race (all at level 3):** 40 % succeed, 24 % are caught, about 15 catches each way
      and 306 prestige lost per agency: roughly one mid-sized goal (goals pay 120 to 3,000).
    - **Rival levels become known fast:** 77–85 % of the time each rival's level is known, from
      plans dossiers and catches; a level 1 service alone, by catching agents, knows 24 %.
    - **Passive finds** come at the designed rate: 6 of a pool's 30 candidates each season.
  - **Still open:** whether the target ever learns of an operation that failed but wasn't caught
    (default: no); a dossier ages by its date only, no visible decay (default).
  - **Before the hotspot reaches the client:** `make_hub_art.py` copies the raw hotspot files to
    `assets/ui/hub/` as they are, and `Hub` turns each polygon name into a `Hub.Place` with
    `valueOf`, which throws on an unknown name; `HubTest` expects seven places. Add
    `Hub.Place.INTELLIGENCE_OFFICE` (icon, names, "isn't built yet") and update the test before
    running the converter with the new polygons.
- **Win condition** (user, 2026-09-28): the host declares a **list of goal ids** per game
  (`game-meta.json`, visible in the UI). **The first agency to reach all of them wins.** A listed
  goal counts as done when achieved or credited through the cascade. Its time is the game turn
  of the resolution.
- **Ties:** only when several agencies finish in the same resolution. For them, rank each
  declared goal by who reached it first, then compare like an Olympic medal table (most firsts,
  then most seconds…). Placement ties use **standard competition ranking** (1, 1, 3), confirmed
  2026-10-06. `WinCondition` is a headless function; the result goes to `game-result.json`.
- **Default declared goals** (`GameMeta.DEFAULT_DECLARED_GOALS`), **decided for the standard pack
  (user, 2026-10-09):** satellite in orbit, human in orbit, and a person landed on the Moon and
  brought home alive (`crewed-lunar-landing`). The early-era pack can't hold that race, so a game on
  it declares `GameMeta.EARLY_ERA_DECLARED_GOALS` instead (satellite in orbit, human in space, human
  in orbit; the server tests do, since they run on the early-era pack).
- **How long the Moon race runs** (measured 2026-10-09, `ContentPackPlayouts standard`, 200 bot
  games of up to 240 seasons, one agency per game, no rivals). The bots fly a fixed plan to the
  landing, one flight at a time: the early race, one lunar probe (it gates the lunar programs),
  the lunar craft's tests in Earth orbit, then flyby, lunar orbit, lander test and landing, 17
  flights for NASA (Mercury, Ranger, Apollo) and 18 for the Soviets (Vostok, Luna, Soyuz 7K-L3).
  - **The plain bot** (one flight's components at a time, six SET specialists): 36 of 200 games
    land by season 240, NASA's at a median of 223, the Soviets' 2 at 230 and 239.
  - **The bot that works ahead** (`standard ahead`: SET Center filled and upgraded, spare
    specialists on later flights' components, two spare controllers and astronauts, nobody sent
    to training when the hardware is close): every game lands. **NASA median 135 seasons**
    (quartiles 122 and 158, fastest 85), **Soviet median 161** (142 and 184, fastest 113). It runs
    into debt doing it (lowest −150k, 28 conversions).
  - **What sets the pace:** R&D. The bot spends 2 to 24 seasons on each flight, mostly waiting
    for its hardware to reach its launch bar of 70 % reliability (about 84 of 152 seasons per
    game; the rest is funds, staffing and building upgrades). The long waits come with a new craft
    (Apollo's first suborbital test about 21, Soyuz 7K-L3's 24) or a new rocket: Vostok's
    uncrewed suborbital test rides the cheaper R-7 of Sputnik, so its first crewed flight waits
    about 24 seasons for the human-rated R-7, where Mercury's crewed flight rides the Redstone its
    uncrewed test already used (about 3).
  - **Against history and the schedule:** a season is a quarter of a game year, so 135 seasons is
    34 game years against the real 47 seasons from Sputnik to Apollo 11. At a turn a day that is
    about four and a half months for the friends' game; Monday to Friday, about six for
    the colleagues'. The early race with the same bot takes a median of 32 (NASA) and 34 (Soviet)
    seasons, the plain bot's 61 and 49. A human player plays better than either bot, flying
    several things at once, but these are the only numbers so far.
  - **The length is fine, the lulls are the risk** (user, 2026-10-09): the colleagues played a
    racing-team manager for about nine months. A shorter race is ruled out: the Moon landing is
    the point. What matters is how many turns in a row a player has nothing to do.
  - **Lulls, measured** (dev-tools `RaceLulls`, 2026-10-09: the bot that works ahead, 200 games per
    cell, components' R&D bands times a factor and their start moved part of the way from
    baseline to R&D ceiling). A gap is the run of seasons without a launch before a launch.

    | R&D, start | Landing median NASA / Soviet | Launches | Gaps of 5+ / 10+ | Longest gap (median game) |
    |---|---|---|---|---|
    | ×1, as is | 135 / 161 | 39 | 22 % / 6 % | 18 |
    | ×1, 50 % | 95 / 115 | 30 | 18 % / 6 % | 15 |
    | ×2, as is | 104 / 120 | 31 | 19 % / 6 % | 15 |
    | ×2, 50 % | 80 / 117 | 27 | 16 % / 7 % | 14 |
    | ×3, 50 % | 80 / 116 | 26 | 14 % / 7 % | 17 |

    Faster R&D shortens the race but hardly the lulls: about three turns in four have no launch
    in every cell, and the median game still has a stretch of two to three weeks of daily turns
    without one. Once R&D is quick the bot waits for money instead (about 50 of its seasons
    stalled on funds at ×3 and 50 %, against 25 as is): saving for a 20,000 program or a new
    rocket is the same lull. The Soviet race gains least past ×2, for the same reason. The bot
    flies only its plan, though; a player can fill a wait with the pack's other flights (probes,
    satellites, records).
  - **Do the pack's other flights fill the lulls?** Hardly (measured 2026-10-09, `RaceLulls standard
    side`, the side-missions bot, two seeds). It flies about four side flights per game besides its
    plan, half of them fillers (a flight in a program already open, on a rocket already open, with
    its hardware at the bar: Vostok's and Soyuz's longer flights, Explorer 1's extended mission, the
    X-15's) and half ventures (a new program, rocket or research first: Mariner 9, Mars 2, Luna 15,
    Lunar Orbiter). The landing stays where it was (NASA 134 to 135, Soviet 166 to 168), and so do
    the lulls: gaps of 5+ turns 19 % of all gaps against 22 %, the median game's longest gap 20 to
    22 turns against 17 to 18. Why:
    - **Fillers are few.** A flight pays only for goals the agency doesn't hold yet (repeats pay
      nothing, `MissionProgress`), and most flights in an open program are the plan's own steps or
      already flown. Where a filler exists, the bot flies it.
    - **Ventures cost what the plan needs.** The middle of the race is a saving game: Apollo's or
      Soyuz 7K-L3's 20,000 to 25,000 to open against 20,000 to 60,000 in hand, a funding rate that
      grows from 3,000 to about 47,000 a season over the race, and money piling up only after about
      season 140. A venture's opening price, new rocket and upkeep come out of that, and the
      Headquarters and VAB caps leave little room for a second line of work.
    - **Earlier versions of this bot made the lulls longer** (up to 36 turns) by buying permanent
      costs (building levels, hires, reopened rockets) for passing side flights, which sank the
      agency into debt before its first satellite.

    So under these numbers a waiting player has little worth flying. The fix is new things to do,
    not more of the same: the interactive events (§5, Backlog), and perhaps flights that pay for a
    repeat or cheaper ways to keep a program open. Both became the commercial contracts (§2.11,
    2026-10-11).
- **The game goes on after a winner:** goals, flights and prestige still count.

### 2.10 Difficulty level

**Decided (user, 2026-09-27): none.** One shared difficulty, based on BASPM's **Normal**
values only (`Value1` in its constants files); no difficulty field anywhere. Retune that one
balance sheet if needed rather than adding levels.

### 2.11 Commercial contracts

**The user's idea (2026-10-11), designed with the user the same day, mocked up and built the same day**
(`rules.Contracts`, `rules.ContractMarket`; `ui-spec.md` §4.17 and component 27). Aimed at the
mid-race lulls, which are mostly a wait for money (§2.9, `RaceLulls`). Once an agency has shown a
capability, clients pay it to fly that capability again: contracts pay **money, not prestige**,
with hardware the agency already has, so they add no R&D wait. Players compete for them on a shared
marketplace, another interaction beside intelligence and poaching. No BASPM precedent; every number is
ours, a default and **unconfirmed** unless a line says otherwise.

- **Unlocking** (user): each program can carry an **unlock set**, the configurations an agency must
  each have flown successfully, chosen per program (rarely all of them; long extended flights are
  usually left out). Completing it unlocks the program's **category as a contract line** for that
  agency, for good. The agency **qualifies** for the line's listings while at least one of its
  unlocked programs in that line is open, so a program's upkeep now buys a chance at contracts. The
  unlock set's configuration cards carry a contract icon with a tooltip (`ui-spec.md` component 27).
  Programs with nothing a client would buy (the late Moon programs, say) have no unlock set.
  **As authored (2026-10-11, unconfirmed):** 25 programs in six lines. Earth-orbit satellites: every
  satellite program, its one-season orbital flight (the three-season extended flights left out).
  Lunar probes: every lunar probe, its one flight. Space planes: the X-15 (its test and both research
  flights; contracts fly the first research flight) and the OKB-MiG E-154 (drop test, suborbital, orbital;
  contracts fly the suborbital test). One-crew capsules: Mercury and Vostok (the uncrewed and the
  crewed orbital flight; contracts fly the crewed one). Two-crew: Gemini (orbital, rendezvous,
  docking) and Voskhod (orbital, spacewalk), contracts flying the orbital flight. Three-crew: Apollo
  in Earth orbit and Soyuz 7K-OK (orbital and docking), the orbital flight. The planetary probes, the
  lunar expeditions and the lunar programs' Earth-orbit tests have none.
- **A contract flies an existing configuration** (decided): each program with an unlock set names
  the configuration its contracts fly (Explorer 1's orbital flight, say), **always one of its unlock
  set**, so its goals are already achieved and a contract can never earn a goal or open a program. Steps, staffing, rockets,
  reliability and the replay are the normal ones. What changes: steps and goals pay **no prestige**;
  the flight raises reliability like any other; it **doesn't count toward the configuration's repeat
  count**, so a contract never lowers a later flight's prestige; cards and the turn report label it.
  An agency qualifying through two programs of a line picks one when it applies.
- **Which launch is the contract:** while a contract is active, the **first launch of its
  configuration** is the contract flight (no toggle); Assemble says so before the launch. Flying that
  configuration for prestige waits until the contract has flown.
- **Closing a program** is refused while an active contract flies on it, as while any of its
  missions flies (the existing `MISSIONS_IN_FLIGHT` refusal covers it). An application whose program
  was closed before the turn resolves comes to nothing and the report says so.
- **Calling a contract flight off** (built default, unconfirmed): a scrub before it flies puts the
  contract back on the ground while its window lasts; cancelling it in flight ends the contract as a
  failure in the record, with neither fee nor the insurer's refund (the cancellation's own refund
  share applies).
- **Money** (user): the agency pays the launch cost up front as usual. **On success** the client
  pays the **fee**: the launch cost times (1 + the tier's margin), **+60 / +40 / +25 %** for tiers
  1 to 3. The fee is fixed at the award, from the contract configuration with the cheapest of the
  agency's open rockets that can fly it, or the cheapest of its side's when none of its open ones can
  (built default); a dearer rocket only lowers the profit. **On failure** the
  insurer pays back the launch cost actually paid (break even), and the agency loses the prestige
  that flight's failure costs anyway. A crew lost on a contract flight costs an extra 50 prestige on
  top of the normal rules.
- **Tiers go by track record** (user, 2026-10-11, over prestige, which would hand the leader the
  money too). An applicant's **score** is the reliability of the weakest component the contract
  flight would use (the agency's hardware at the start of the turn, its most reliable open rocket
  that can fly it) plus
  its **record in that line**: +3 per contract flown successfully, −6 per failed or lapsed one,
  within ±15. The highest score gets tier 1, the next tier 2, the third tier 3; a fourth applicant
  and later get nothing (user). Ties go to a seeded draw. Scores read start-of-turn snapshots
  (§3.7's rule for contended resources). Clients choosing the most dependable launcher is what real
  customers do, and it rewards R&D past the bar. **Crew and controllers don't count**: clients judge
  the hardware, not who flies it, so the score differs from the assessment tab's odds on purpose.
- **Applying** (user): **one application per turn**, refused while the agency has an **active
  contract** (awarded and not yet flown to its end), so one contract at a time. Applications are
  **sealed** until the turn resolves. An application restates intent ("my application this turn is
  X"), so it can be changed or withdrawn until then.
- **The award**, at the resolution: each applicant learns its tier and fee, or that it missed out,
  in the turn report. A listing leaves the board once awarded. Everyone's report gets a news line
  naming the agencies that won it, in alphabetical order: who ranked where stays sealed.
- **A launch window:** an awarded contract must **launch within 3 seasons**. One that doesn't
  **lapses**: it counts as a failure in the record and costs 10 prestige. A scrub before the flight
  refunds the launch cost as usual and keeps the contract while its window lasts.
- **The board** (the user's design, 2026-10-11, refined the same day): settled once a turn, at the
  resolution, for everyone (game-level, like intelligence), from its own salted seed.
  - **A listing** is a line, a client with a story (flavour content), the three tiers' margins, the
    turn it was posted and **its expiry, 3 turns later**. Listings stay until awarded or expired.
    **Only lines at least one agency qualifies for are listed** (user: no aspirational listings); a
    listing nobody qualifies for any more is withdrawn. Everyone sees the whole board, with whether
    they qualify.
  - **Seating** (the user's queueing idea): every listing has three seats, one per tier. The
    eligible agencies (claimed, no active contract) are seated in turn, **the most constrained
    first** (fewest qualifying lines; ties by seeded draw): each takes a free seat in the fullest
    listing it qualifies for. Only when none has a free seat is a new listing posted, in the line
    **most of the still-unseated agencies qualify for** (ties by seeded draw). Seats are only how the
    board is sized; nobody sees them, and players apply wherever they qualify. Packing the narrowest
    agencies first gives the most overlap, so the most competition.
  - **Extras** to ease the competition: one listing, plus one per four eligible agencies, from the
    lines they qualify for (seeded), added only while the board holds fewer listings than the
    seating needed plus the extras, so expiring listings don't pile up.
- **The Soviet side** has no commercial market, so it is reskinned, not shortened: its clients are
  ministries and institutes ordering a service (the Ministry of Communications' relays, the Academy
  of Sciences' instruments). Same rules, own texts.
- **Intelligence:** an agency assessment's Programs and plans area also shows the rival's active
  contract and its record per line.
- **Content:** in the pack (hashed), a file of its own, `commercial.json` (in `ContentPackLoader.FILES`;
  its own file so the BASPM converter, which writes `programs.json`, never touches it): per program,
  the unlock set and the contract configuration. The margins and record weights are rules constants
  (read by help's tokens, never written into text). Clients and their stories by line and side go in
  `assets/content/commercial-clients.json` (31, at least two per line a side flies), outside
  `ContentPackLoader.FILES` like the press stories, no repeats until a line's have all run. A listing
  names one client per side, so each side reads its own.
- **Storage** (as built): `market/turn-N.json`, written once at the resolution before the season
  passes, like intelligence, and re-read on a retry: the awards from the turn's applications, the
  board for the coming turn, the clients used. **The board is settled before the passes too**, since
  each agency's copy is saved in its own state (`PlayerSnapshot.contractBoard`) and a snapshot is
  written once; so no new message is needed and the client reads the board with the state. The cost
  is a one-turn lag: a line unlocked by a flight this turn, or an agency freed by a contract ending
  this turn, is seated at the next resolution. Scores read each agency's state at the start of the
  turn (an R&D decision taken during the turn can move a reliability); applications and open
  programs read the state with the turn's actions applied. One edge: a rocket program reopened this
  turn has no open rocket yet at the turn's start, so the applicant scores on its record alone;
  reopen the rocket a turn before applying. Per player: the record per line, the
  active contract with its fee and window, the application, the board; unlocked lines are worked
  out from the flights flown, not stored. `TurnRollback` deletes the turns' market files.
- **Measured (2026-10-11):** `RaceLulls standard ahead+contracts 1.0 0.0` against `ahead`, 200 Moon
  races. A playout has one agency, so the market is simulated: every listing the bot applies for draws
  three rivals as strong as it, its rank among them random (a fourth of applications miss out). The
  bot applies whenever it has no contract and flies the contract as soon as its hardware is ready.
  Per game: 15.6 applications, 11.5 awards, 7.3 paid, 1.4 refunded, **2.8 lapsed** (a quarter of
  awards: short of money or staff with its own flight going), about **6,000 profit** over the race.
  Launches per game rise from 39.1 to 44.6; gaps of 5 seasons or more fall from 22 % to 14 % of
  launches, the 90th percentile gap from 8 to 6 seasons; seasons without a launch from 74 % to 70 %.
  The race itself barely moves: NASA's median **slower** (135 to 142 seasons), the Soviet one faster
  (161 to 153), 197 of 200 finished against 200, and debt conversions up from 28 to 49. So contracts
  fill the lulls, as meant, but at these margins they don't pay for the race: a fee on an early
  flight is small beside a season's funding, and the lapses and failures cost prestige that the
  budget reviews then miss. For the play test: higher margins, a fee that scales with the era, or
  a bot that only applies when it can fly the contract soon (§4).

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
JPEGs), embroidered goal patches (`assets-raw/patches/`, 128 px RGBA PNGs) and the mission
components' blueprint drawings (`assets-raw/blueprints/`, 864 x 352 JPEGs for the turn report,
2026-10-09, the §1.2 exception), all with fixed seeds, a manifest, a human pick, and a converter
into `assets/` (commands in `CLAUDE.md`). The blueprints share one background wording ("soft": a
gentle blue gradient with a grid, no frame); paper texture clashed with the UI's own engineering-
paper background and a stronger gradient lost the pencil feel (user, 2026-10-09). Their slight
differences in shade are wanted and never normalised. Where BASPM's render shows another craft than
ours, the reference is the user's own art instead: the MiG E-154's four views (2026-10-09; BASPM's
was its PKA capsule plane).
Program, category, configuration and hub art is the user's own, converted by
`make_content_art.py` and `make_hub_art.py`.

**Principles:** automation generates candidates, a human picks (a vision-model pre-check is a
cheap first filter; the patches' spelling is checked by eye). Never use BASPM's art as a style
reference or training set (§1.2), apart from the blueprints' one decided exception. For a bigger batch, anchor the style first (a LoRA or
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
- **Players online while a turn resolves** (the user's question, built 2026-10-09). Some players
  wait online to see the turn first. The server handles requests and resolves the turn on one
  thread, so a request never interleaves with a resolution; it waits in the network queue and
  is handled after the resolution is saved. Two guards on top:
  - **An action names its turn.** `SubmitActionRequest.gameTurn` is the open turn the client's
    state belongs to. If that turn has resolved since (the player acted on the old turn's screen
    as it resolved), the server refuses with `TURN_RESOLVED` and applies nothing, so an action
    meant for one turn never lands in the next against a state the player hasn't seen. The
    client takes in the new turn's state and says the turn resolved and nothing was done; a hire
    or R&D chain stops there. 0 skips the check (`DevClient`, tests).
  - **The server says when a turn resolved.** Once a resolution is saved (scheduled or forced),
    every logged-in connection gets a `TurnResolvedNotice`, and the client fetches the new state
    at once and shows "The turn resolved", instead of polling 5 s after the deadline and then
    every 15 s (still the fallback). The notice never goes through the reply queue, so it can't
    be taken for the answer to a request. It is also the hook for the Firsts' front pages (§5,
    Backlog).

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
    journal-rolled-back/<turn>/ actions set aside by a rollback of that turn (below)
  content/<packId>/            the game's frozen content pack (§5, M5)
  intelligence/turn-<N>.json   the turn's intelligence, settled once (§2.9)
  backups/                     zips of the data root (below)
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
- **Rolling back a bad turn** (the user's idea, 2026-10-11): when a bug makes a turn come out
  wrong, the game goes back to the start of the turn, the server gets the fix, and the turn
  resolves again. Each player's season is seeded from the game seed and the turn, so the redone
  turn rolls the same dice and only what the bug touched changes (`TurnRollbackTest` holds a
  redo to byte-identical files; on the dev game, redoing turn 19 with a later build changed only
  the two duplicate candidate names that build's fix was for).
  - **`server.schedule.TurnRollback`**, run with the server stopped:
    `java -cp LaunchWindow-Server-<version>.jar de.mkoehler.launchwindow.server.schedule.TurnRollback <dataRoot> <turn> [--redo] [--yes]`.
    It undoes `<turn>` and every turn after it. Without `--yes` it only says what it would do.
    `--redo` resolves the turn again at once with the build it runs from, before anyone
    reconnects and changes their queued actions; it leaves the schedule alone, so the next turn
    resolves on its usual day. Without it the server redoes the turn at its next scheduled time
    (or by dev mode's "Resolve turn now").
  - **Per player** (`PlayerStateService.rollBack`): the actions queued after the turn resolved
    are set aside in `journal-rolled-back/<turn>/` (told apart by the snapshot's cutoff, so a
    retry is safe): they were planned against the wrong results, and players queue again. The
    turn's own actions go back from `journal-archive/<turn>/` into the journal, and the turn's
    report and snapshot are deleted. Several turns roll back one at a time from the latest, so
    each later turn's actions land under that turn and none resolve a turn early.
  - **Game-wide:** the turns' `intelligence/turn-N.json` are deleted (re-settled from the same
    inputs), `game-result.json` only if a rolled-back turn decided it, the abandoned timeline's
    `backups/before-turn-N.zip` after the turn, and `scheduler-state.json`'s next turn becomes
    the turn.
  - **Backups** (`TurnBackups`): the data root is zipped before every turn resolves
    (`backups/before-turn-N.zip`, written once so a retried resolution keeps the first attempt's,
    the last 10 turns kept) and before every rollback (`before-rollback-to-turn-N-<time>.zip`,
    never pruned). They leave out `accounts.json`, `content/` and `backups/`. They are the net
    under the rollback, for a bug that damages files it relies on: stop the server, delete
    everything in the data root but those three, *then* unzip, and start it again. Never unzip over
    the folder: newer journal files would stay beside an older `journal-sequence.txt`, and a reused
    sequence number breaks crash recovery. Each zip holds the
    whole history, so it grows with the game (unconfirmed: fine for a hobby server's few players).
  - **Known limits:** a player who joined after the turn resolved is resolved into the redone
    turn too, one extra season for them. A fix to the content pack also needs the frozen copy
    in `content/<packId>/` replaced, or the server keeps the old one. Clients keep the highest
    report they read, so a redone turn's report doesn't show as new: tell the players.

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
- **`ContentPackPlayouts`:** bots play a real pack through the real season pass and report
  whether and when they reach the declared goals: the early race on `early-era`, the Moon race on
  `standard` (a pack with the `crewed-lunar-landing` goal), with a plan per agency. `ahead` as the
  second argument plays the bot that works ahead (§2.6), `side` the one that also flies side
  missions; it also opens Headquarters and the VAB as
  the plan needs, and closes programs and rockets it no longer flies. Both bots now wait only for
  the components a configuration's steps fly (before 2026-10-09, every component of its program;
  the early-era numbers came out the same). With the pool of 10 (2026-10-06):
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
- **Header** above a rule: back, home (the hub), settings and help (2026-10-08, `ui-spec.md`
  component 23) buttons, a breadcrumb rooted at
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
  NASA and Soviet, 4 seasons × 4 times of day (`make_hub_art.py`, cropped to 16:9 off the bottom and scaled to
  1920 × 1080). The art was redone on 2026-10-10: the user's own renders at 2720 × 1536, NASA from
  seed 125 of a ComfyUI batch (picked because the VAB's crawlerway runs off the left edge) and a
  Baikonur set from the same prompt adapted.
- **Eight buildings** with one hotspot polygon set per archetype (`*.hotspots.json`, authored
  with `HotspotEditor`; one set fits every variant): VAB, Mission Control, Astronaut Complex,
  SET Centre, Public Affairs Office, Headquarters, Museum, and the Intelligence Office (2026-10-07,
  §2.9; drawn by the user, a different building on each archetype's art). Hover is a hard-edged near-white
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

- **Lulls in the Moon race** (§2.6, measured 2026-10-09). The length is accepted and the race stays
  (user, 2026-10-09); the open question is how to keep players busy between flights. Faster R&D
  and more reliable starts shorten the race but leave the lulls, which turn into waiting for
  money, and the pack's other flights barely fill them. Next: the interactive events (§5, Backlog),
  then the R&D tuning with the side-missions bot. The Soviets take about a fifth longer.
- **Commercial contracts** (§2.11, 2026-10-11): every number is unconfirmed (margins, the record's
  weights, the launch window, the expiry, the extras), and so are the unlock sets, authored per
  program. The first measurement (§2.11) says they fill the lulls but don't speed the race, and a
  quarter of awards lapse: the margins, or a fee scaled to the era, are the first thing to look at.
- **Declared goals for the colleagues' game:** an early-era race (user, 2026-10-08), with goals
  drawn from the early-era pack. Since then the standard pack became the default with its own race to
  a crewed Moon landing (user, 2026-10-09, §2.6); whether the colleagues' game plays that or a
  shorter early-era race is open.
- **NASA/Soviet prestige balance:** BASPM's Soviet programs pay a little more each (about 4 %)
  but NASA has nine more programs, about 34 % more prestige in total. Decided (user,
  2026-10-01) not to close the gap up front; revisit with play-test and harness data.
- **A game-host role and UI** (announcing games, blackouts, allowlists): direct file edits for
  now.
- **Interaction beyond passive competition** (mail, cooperation): after the daily loop is
  proven (§2.9). Intelligence is designed (§2.9, 2026-10-07); sabotage is ruled out.
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
  - A configuration's `cost` is its payloads' unit cost times the average step `DifficultyFactor`
    over all its seasons, rounded half to even, as in BASPM (`MissionsConfigurationsManager`; a
    missing factor is 1). Only Sputnik 1's extended mission has factors (1.75 on its two later
    steps), so its cost went from 488 to 580 (2026-10-08).
  - A step only risks the crew when the configuration has crew slots: BASPM flags some steps of
    two uncrewed Soyuz flights anyway.
  - A goal modifier BASPM repeats within one list (the X-15's research flights boost on the same
    goal twice) is merged into one with the summed amount; the engine sums them either way.
  - **Goal boosts stay (user, 2026-10-08).** BASPM's loader never reads `GoalBoost` elements, so in
    the shipped game only the penalties apply (`baspm.md` §11.3). We keep applying the boosts the
    data declares (2 in the early-era pack, 99 in the standard pack, +1 or +2 each): they reward
    related work, such as Pegasus and Biosatellite for Mercury, or a robot lander for a crewed
    one. The plan's components card lists every boost, including those not yet reached, as
    "(+1)"; Mission Control names the active ones; help explains both kinds.

  **The full tree: the `standard` pack (converter scope `full`, 2026-10-08).** The converter writes
  all of BASPM's tree: 13 categories (with their `body`), 55 programs, 85 components (61 payloads,
  24 rockets), 189 configurations and all 207 goals. The goals include the umbrella goals no
  configuration achieves directly, because other goals' cancellations credit them. Our ids are in
  `full_maps.py` next to the converter; the early-era ids are kept. The ids are confirmed (user,
  2026-10-08). It is a second pack, `assets/content/standard`, beside `early-era`, and **the server's
  default since 2026-10-09** (user; `GameMeta.DEFAULT_CONTENT_PACK`), with its own race ending at a
  crewed Moon landing (§2.6); the early-era pack stays for the tests and short scenarios. **Its text is written** (2026-10-08): names and descriptions for every
  category, program, component, configuration, goal and step type, in our own words, with the
  early-era text carried over unchanged. **History notes are complete** (2026-10-09, every figure
  checked by web search): every program and every component, and the 38 new configurations that
  match a real flight (Gemini 1 to 12, Voskhod, Soyuz 1 to 9, Zond 7, Apollo 4 to 15, Apollo-Soyuz,
  Skylab, the X-15, Venera 13). Configurations with no real counterpart (the lunar Gemini plans,
  the LK-700, the E-154, the extended missions) stay without, as in the early era. The two Gemini
  EOR programs share one note, the Langley lander, like the other lunar Gemini programs. A joint flight's craft-specific steps take their
  shared step's text, named for the craft (`ContentStrings.craftStep`: `gemini-a-`, `atv-` for the
  target, `transtage-`, `-a`/`-b`). **Its art is converted** (`make_content_art.py`: 55 programs,
  189 configurations, 22 category images), and `ScreenSnapshot` shoots it (`standard-*.png`). **Its
  goal patches are generated** (2026-10-08): all 207, the new 186 defined in
  `assets-raw/patches/patches_full.py` (scene templates per kind of mission, each with its program's
  craft), three variants each, spelling checked by eye, ten goals rerolled or reworded where no
  variant was spelled right (the model can't spell "CIRCUMLUNAR", so those patches say "GEMINI"). The
  bots play it (`ContentPackPlayouts standard`) with no invariant broken. The text for the
  MiG E-154's program and missions should fit the user's art, a rocket plane of its era, not
  BASPM's PKA (user, 2026-10-08). **No animal passengers on the new flights:** Zond 5's tortoises,
  the Biosatellite and the OFO frogs get no memorial and no honours (user, 2026-10-08), so
  `ANIMAL_PASSENGERS` stays as it is. In this scope, anything that would be dropped fails the run, and the run checks
  every launch has a rocket of its agency that lifts it (106 configurations need at least a
  Medium rocket, 36 a Heavy). BASPM's `HistoricalRocketUsedID` only picks the clip: 12 launches
  name one that can't lift them. `StandardPackTest` checks the tree: every launch flies on a rocket
  of its agency at some VAB level, staffing fits, every goal can be earned, and the race goals
  (through the crewed lunar landing, Venus and Mars) are open to both agencies. Four engine
  additions came with it (built 2026-10-08, `FullTreeLaunchTest`):
  - **`ownLift` on a payload** (`MissionLaunch.requiredLift`): the Agena, the Gemini Transtage, the S-IVB tug and Apollo's CSM
    carry part of their own lift, which BASPM subtracts from what the rocket must lift
    (`ComputeExtraBoostProvidedByPayload`). Without it, Apollo's CSM needs a Saturn C-3B in
    Earth orbit instead of BASPM's Saturn IB.
  - **`AIR_DROP` launches:** the X-15's three configurations and the MiG E-154's drop test fly
    with no rocket (BASPM's `LaunchType` `NA`, rocket `N/A`). The launch keeps its place in the
    rocket list with `""`, costs no rocket, and no step may name its rocket slot; the planning
    screens say a carrier aircraft drops the craft.
  - **`requiredGoalIds` on a configuration:** a hard gate until the goal is achieved or
    cancelled (BASPM's `StrictGoalDependency`): the X-15's two research flights and the MiG E-154's
    suborbital test. A launch refuses with `GOAL_NOT_REACHED`; the configuration card shows
    "Locked" and its steps screen names the goal and disables "Plan this mission".
  - **`transferPercent` as a double:** many of the full tree's reliability transfers are .5
    values (`client.view.Rockets` prints it, so format it there).

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

**Concept chips** (2026-10-07, `ui-spec.md` component 19) are built: `concepts.json` from the
concept audit (§1.1), the chips on program details, configuration details and the VAB's rocket
rows, and the "new" dot there and in the Museum. Their look is confirmed (user, 2026-10-07). Otherwise
next is the next play test.

**Poaching** (people phase 5, built 2026-10-08, §2.2(d), `ui-spec.md` §4.16): offers to a rival's
engineers and flight controllers from a personnel file, one a season, and standing orders to match
them, per department and per person. Mocked up on the canvas `Launch Window — Poaching`. Every number
is unconfirmed and unmeasured: no bot makes offers yet. **Reset the dev data root** before the next
play test.

**Intelligence** (designed 2026-10-07, §2.9): an eighth building, the Intelligence Office, with a
level dial (0 to 3, one step a season), actions per season equal to the level, passives, the
background check on the hire screen, agency assessments and personnel files. Mocked up on the
canvas `Launch Window — Intelligence Office` and built (2026-10-07), with the user's hotspot
polygons. The personnel file's loyalty band and ambition wait for phase 4. **Reset the dev data
root** before the next play test (new fields on the snapshot and on employees).

**Help** (the user's idea, built 2026-10-08, `ui-spec.md` component 23): a help button in every
header and the hub strip, and F1, open the rules by topic over the current screen, filtered to that
screen's topics; typing searches them all. 28 topics, written 2026-10-08 from this document and
the rules, every number a token. **Next:** keep the topics in step with the rules (a changed
constant updates itself, a changed rule needs its text), and put the `help` and `search` glyphs on
the UI Asset Sheet (prompt given to the user 2026-10-08).

**Advisor** (the user's idea, built 2026-10-08, `ui-spec.md` component 24): a card on the hub with
a new agency's first steps (help, engineers, a program, a rocket, R&D, controllers, astronauts
once crewed, reliability to 60 % for a payload and a rocket, then the first mission, the first
report), each ticked off from the state, with a link to
the building and one to the item's help topics. It retires for good once everything is done or the
player hides it. **Next:** see in the play test whether the order and wording steer a new player
well, and whether three items at a time is the right number.

**A smarter bot** (user, 2026-10-09): worth real effort, for better analysis now and maybe an NPC
rival later. Built in dev-tools the same day: `ContentPackPlayouts.Bot`, plain, working ahead,
and flying side missions (§2.6). Lessons for an NPC: buy nothing permanent (building levels,
hires, rocket programs) for a passing goal, and stay solvent before staying busy; debt stalls the
plan for longer than any side flight fills. Next for the bot: several flights of the plan at once,
and the R&D tuning (§2.6) rerun with it.

**Interactive events** (the user's idea, 2026-10-09, not designed yet): decisions between
flights, so a wait for R&D isn't empty. R&D events with choices (a supplier delay, a trade of
cost against reliability, paying for a ground test or test firing whose result shows), and
decisions in the turn report. Some should draw on what the explainers teach (§1.1, `ui-spec.md`
component 18), so a player uses that knowledge to improve a design. To be fleshed out after the
bot.
- **R&D decisions, the first step** (talked through with the user, 2026-10-10; not specified yet):
  - **Shape:** an event arises at a turn's resolution, attached to one component under R&D (rocket or
    payload), and waits for the player. Choosing is an ordinary action that the server resolves on the
    spot (outcome drawn from a seed fixed to the event, so no rerolls and the journal replays it), with
    the result shown at once. Unanswered when the turn resolves, its default applies. One open decision
    at a time.
  - **Where:** a gold Decision card at the top of the turn report and a "Decisions" cell in its strip,
    linking to the R&D board; a gold dot on the VAB's or SET Centre's hub marker (hover "A decision
    waiting: Atlas"); the decision itself as a banner on the component's R&D card (story, choices with
    what each shows in advance, the default marked, concept chips), a confirmation, then the outcome in
    place with the bar and season marks updating; a line in the next report's news.
  - **When (aimed at the lulls):** near certain (about 85-90 %) when the agency has no mission in flight
    and the component is more than a season from 60 % (`ResearchPlanning.project`), on the component the
    soonest mission waits on, else the one furthest from 60 %. Raised by seasons since the last launch,
    nothing becoming ready next season (no launch, training, upgrade or milestone), waiting for money,
    and a quiet last turn; lowered by a mission in flight or a launch this turn, and by a decision last
    season. Paused after an unanswered decision until the player acts again, so absent players don't
    collect defaults. Measured with the playout bots before tuning.
  - **Types** (the user's three, then six more; the user wants all of them, story arcs especially):
    1. *Resource for a chance:* spend X for a stated chance of Y (pay for a ground test).
    2. *Knowledge:* a right and a wrong answer, a large boost or a penalty, and "I don't know" as the
       default; drawing on the explainers.
    3. *Ambiguous:* several choices whose outcomes can't be told in advance, social and personnel themes,
       traits shaping the result.
    4. *Who oversees it:* assign an idle engineer, their skill setting the odds; pulling someone out of
       training makes them available.
    5. *Trade-offs over time:* an effect over several seasons (freeze the design: faster R&D, lower cap;
       redesign: a drop now, a higher cap), the season marks previewing each option.
    6. *Diagnosis:* clues (telemetry, a technician's note), pick the cause; a wrong pick costs a season.
    7. *Opportunity with strings:* an offer for this turn only (a borrowed test stand for a seconded
       engineer, an untested cheaper part).
    8. *Pairing:* pick two engineers to work together; rapport and traits decide, and the pairing builds
       rapport or starts a clash.
    9. *Intelligence tip:* adopt a rival's approach, right more often the higher the Intelligence
       Office's level.
    10. *Story arcs:* a good outcome opens a follow-up in a later quiet season, with higher stakes.
    11. *Hands-on* (the user's idea, 2026-10-11, from the "Probe Command Minigame" spec doc; specified,
        not built): instead of options, a short resource puzzle the player can play, or leave to the
        team. Specified below and in `ui-spec.md` component 26, type 11.
  - **Specified** as `ui-spec.md` component 26 (2026-10-10), with G11 for the server; **mocked up** the same
    day on the canvas `Launch Window — R&D decisions` (the choices are in component 26; the look confirmed by
    the user the same day) and **built** the same day: all ten types, 23 decisions, the rules, the client and
    the bots ("as built" in component 26). Next: the play test, then the pools grown by type, and the balance
    (decisions shorten the bots' Moon race by about 13 seasons).
  - **Build:** the type a field in a content file (story, options, what shows in advance, effect,
    default), one resolver per type. Variety isn't a worry: other lull ideas will join it (user).
- **The hands-on decision** (type 11; the user's idea, 2026-10-11; mocked up the same day, canvas
  `Launch Window — Hands-on`; every number unconfirmed): a
  turn-based resource puzzle with a push-your-luck step, adapted from the user's "Probe Command
  Minigame" spec (the Claude Docs page of that name) from a probe in orbit to a ground test campaign.
  The mechanics come from another game's minigame; mechanics are free to borrow, but every name,
  number, text and drawing here is our own (the spirit of §1.2's rule).
  - **The choice:** the banner offers "Run it yourself" (opens the puzzle) and "Leave it to the team"
    (the default: nothing happens). Starting commits: from then on the decision settles from the
    puzzle, and an unfinished puzzle settles as it stands when the turn resolves (its remaining rounds
    spent empty), so quitting after bad rolls never falls back to the default.
  - **The puzzle** (the spec's rules, kept): four resources, a fixed command list, a few rounds of up to
    three commands each. **Plan:** queue commands; their inputs are paid at once, their outputs only
    at execution, so outputs can't pay for another command the same round; undo and reset refund.
    **Execute:** commands resolve in slot order, each rolling fail (primary output -1, never below
    0), normal, or exceptional (+1); on a fail the player may pay 1 of the first resource to turn it
    normal. Secondary outputs always pay. The recharge command (no input, +1 of the first resource)
    never rolls and uses a slot. Meeting the objective when the rounds run out is a success, the bonus
    objective a bonus success (ending the puzzle early), anything less a failure. Held amounts count,
    not totals produced.
  - **Ground theme for a rocket's test campaign** (reference level, unconfirmed): *Shifts* (the
    spec's power: pays the basic commands and for resisting a failure), *Firings* and *Measurements*
    (the objective), *Spare parts* (the intermediate). Three groups of three commands, each group's
    first costing only shifts, the others costing the other groups' resources; plus *Overtime* (+1
    shift). Start 6 shifts; 4 rounds of 3; objective 3 firings and 4 measurements, bonus 6 and 7, as
    in the spec's reference level. A payload's campaign is the same structure with its own words
    (*Bench runs*, *Measurements*, *Spare parts*). Command names are our own and live in
    `decisions.json` with the level.
  - **The team sets the odds** (unconfirmed): the fail chance is 35 % less a quarter of the best
    team member's skill in the component's skill, never under 10 %; the exceptional chance is 15 %,
    20 % with good chemistry on the team; the rest normal. Resisting reads as "call in an expert". A
    component with nobody on it can't offer the type.
  - **Rewards in seasons of R&D gain** (as the other types): bonus success about three seasons' gain,
    success about one and a half, failure nothing. Close to a right knowledge answer at best, so the
    puzzle is a choice, not a duty: a player who never plays it isn't behind.
  - **Server-authoritative:** the puzzle's state lives in the open decision (`PendingDecision`), and
    each step (queue, undo, reset, confirm, resist, accept) is a journaled action the server applies at
    once and answers with the new state. The rolls come from the decision's own seed and the step's
    index, never sent to the client (`withoutSeed`), so a player can't preview the rolls offline, and
    a redone turn replays the same puzzle. A confirm runs its round up to the first fail that waits on
    a resist choice. The rules are a pure state machine in `core` (`rules.HandsOn`), the client's
    screen only draws it (G7).
  - **Levels are authored, not generated:** a handful per component kind, each checked in a test by a
    search over all-normal play (the objective must be reachable, the bonus too); difficulty may rise
    with the component's era later.
  - **How often:** one type among the eleven, drawn like the others, so a few times a game; four rounds
    of three commands take a few minutes, fine now and then, tiring every turn.
  - **Built (2026-10-11):** `rules.HandsOn` (the state machine and an all-normal solver, the spec's
    acceptance tests in `HandsOnTest`), `RdDecisions.handsOn` (rolls from the decision's seed and the
    roll's index, the expert's answer never changing the next roll), `HandsOnAction`, and two levels in
    `decisions.json`: *A week on the big test stand* (rockets) and *A week in the vacuum chamber*
    (payloads), both checked winnable to the bonus by the solver. An unfinished campaign at the
    resolution accepts a waiting fail, refunds an unconfirmed queue and runs the rounds left empty.
    The odds use the team's chemistry as the player sees it.
  - **Measured (2026-10-11):** `ContentPackPlayouts standard side decisions`, 200 Moon races. The bot
    picks "Run it yourself" at random like any option, then plays the solver's plan each round and
    calls in an expert on every fail while shifts last: 522 campaigns, **480 bonus, 42 success, no
    failure**. A perfect planner with four rounds for a three-round bonus almost always gets it, so
    the bonus is too easy at these numbers, at least for a careful player: a harder bonus, three
    rounds, or a dearer expert are the levers (unconfirmed, for the user to choose).

**Front pages for the Firsts** (the user's idea, 2026-10-09, not designed yet): when an agency
makes one of the great Firsts (satellite in orbit, human in space, human in orbit, crewed lunar
flyby, the Moon landing), every player sees a fictional newspaper's front page with the headline
and an article on who made it first. It pops up on login before the turn report, the first thing
they see. A new screen: spec, then a mockup session (`ui-spec.md` §7).

**Commercial contracts** (the user's idea, designed, mocked up and **built** 2026-10-11, §2.11,
`ui-spec.md` §4.17 and component 27): contract lines unlocked per program, a shared marketplace with
sealed applications, tiers by track record, money instead of prestige. Rules, market, content (25
unlock sets, 31 clients), screens, help topic, a contract bot (`RaceLulls ... ahead+contracts`) and
`TurnRollback`'s market files are in. Next: the play test, then tuning from the measurement in
§2.11.

### Later

- **M6 — Deployment: built 2026-10-06, no release cut yet.** The jpackage client zip (built and
  launched locally, also under a pinned version as CI will), the server Docker image with a
  bind-mounted data root (`deploy/docker-compose.yml`, unverified until its first CI run), and
  the two tag-triggered workflows. The packaged exe is DPI-unaware by default (Windows scales
  it); the user is judging the fonts in both modes. **The repo stays private for now** (user,
  2026-10-08). The NAS pulls the image with an access token. The repo goes public when a real
  first game starts. The first version number is still open.
- **M7+ (order open):** people worth caring about (chemistry, traits, rapport, ambitions, career
  records, poaching; §2.2(d), in five phases, all built by 2026-10-08); interaction beyond passive competition (§2.9); the rest of the content
  tree beyond the early era; more Museum encyclopedia entries once there's content for them (§1.1; E-01 to E-19
  built by 2026-10-07; E-20 to E-30, for the full content pack, built 2026-10-09); a game-host role and UI;
  validating the `WEEKDAYS` profile with the colleagues' group.
- **Dropped: launch latitude as a mechanic** (user's idea 2026-10-07, dropped by the user
  2026-10-08). Considered: a site factor on payloads, orbit classes on missions (polar,
  geostationary, the Soviets' Molniya orbit as their answer), and second launch sites as
  buildings. None was worth its weight: each side's rockets only ever fly from their own site, so
  the latitude is already in every rocket's lift, and E-14 says so.
