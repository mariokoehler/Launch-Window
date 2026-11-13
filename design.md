# Launch Window — Design Document

What the game is, how each system works, the architecture, and the roadmap. It describes the
game **as decided or built now**; how decisions were reached belongs in git history: the full
text up to 2026-10-06 (every trace, rejected proposal and harness run) is in
`git show 6fc7af5:design.md`, and the text before the 2026-11-12 prune (build narratives,
superseded measurements) in `git show 5c95135b:design.md`. Update this file the same session a
decision is made. Section numbers are stable anchors (code and the other docs cite them): never
renumber.

**Markers.** **(unconfirmed)** is a concrete default proposed to keep work moving, not yet
signed off; §4 lists the numbers still awaiting the user's review. **(from BASPM)** is carried
over from the original game. `baspm.md` has how BASPM does each thing.

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
  each pack's `strings.json` (G4); others in `client.ui.HistoryNotes`.
- **Long descriptions (user, 2026-10-27):** every program and every component a program grants has a
  paragraph on Program details (`longDescription` in `strings.json`, `ui-spec.md` §4.6.4): what it was
  for, how it worked and what it had to prove, never repeating its history note. Written from checked
  facts, each figure's source in `assets-raw/strings/LONG_DESCRIPTIONS.md`. Projects that never flew
  are written as proposals.
- **Space-race news in the turn report (user, 2026-10-04):** real events from
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
- **Explainers (user's idea, 2026-10-06; E-01 to E-31 built; triggers unconfirmed):** short
  full-screen "infographic" series that explain *how and why* an aerospace idea works, offered (never
  forced) when it first comes up in play through concept chips, and kept in the Museum's Encyclopedia
  tab (`ui-spec.md` component 18, with each sheet's content). Canvas `Launch Window — Explainers`, one
  row per explainer with a yellow note of what to verify.
  - **E-01 Suborbital vs orbital** (3 sheets): Newton's
    cannon and the 8-kilometre rule; Freedom 7 against Friendship 7 to scale; the rocket
    equation's propellant share, staging, re-entry.
  - **E-02 Rendezvous** (2): brake into a lower, quicker orbit to catch up (drawn true to
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
      the sheet). Its settling rockets were solid ones, four of them (Andrew LePage, *The Space
      Review*, "If at first you don't succeed", part 2).
    - E-04, settling rockets (user, 2026-10-23): many were small solid motors, which can't float but
      can't be stopped or relit either. The Saturn V's third stage (S-IVB-500) fired two solid
      Thiokol TX-280 ullage motors on its aft skirt before its first start, then dropped them; for
      the relight towards the Moon it used the 70 lbf ullage thrusters in its two Auxiliary
      Propulsion System modules, burning nitrogen tetroxide and monomethylhydrazine (the S-IVB
      article on Wikipedia; Roy Godfrey's oral history at the University of Alabama in Huntsville).
    - E-04 sheet 2, gas-fed engines (user, 2026-10-23): no pumps to starve, but the propellant must still
      lie over the outlet, or the pressurising gas is what flows. Apollo's service module engine was
      settled by a +X burn of the steering jets (two or four jets) before its burns, longer when the
      tanks were low and the craft heavy, none with nearly full tanks (Apollo 10 Flight Journal; the
      Apollo 14 CSM checklist's ullage charts). Apollo 10's lunar module ascent stage was starved
      of propellant once for want of settling (TRW's flight evaluation, 1969).
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
  - **E-06 to E-19** (from the user's brief), one per candidate in the concept audit below:
    Weightlessness, Heat shields, Reliability, Launch escape, Coming home, Tracking, Space
    environment, Human rating, Launch latitude, Max Q (one sheet), Power, Guidance, Orbital decay and
    Balloon tanks (one sheet). Sheet titles in `ui-spec.md` component 18. E-14's "In Launch Window"
    strips say the latitude is already counted in every rocket's lift, since each side's rockets only
    ever fly from their own site (user, 2026-10-08, when the latitude mechanic was dropped).
  - **E-20 to E-30**, for the standard pack: see "Explainers for the standard pack" below.
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
- **Explainers for the standard pack (`ui-spec.md` component 18, with the changes from the brief and
  the mockups):** E-20 Three ways to the
  Moon (direct ascent, Earth-orbit and lunar-orbit rendezvous), E-21 Docking, E-22 Spacewalks, E-23
  Launch windows to the planets, E-24 Gravity assists, E-25 Landing without air, or with too much
  (Moon, Venus, Mars), E-26 Far from home (power and signals), E-27 Pictures before digital cameras,
  E-28 A piece of the Moon by robot, E-29 Rocket planes at the edge of space, E-30 Around the Moon
  and back. Checked before the build (2026-10-09): Gemini 12's three outings total 5 h 30 min;
  Apollo's powered descent starts about 15 km up (50,000 ft), and Apollo 11 landed with well under
  a minute of fuel; Luna 16 drilled 35 cm in the Sea of Fertility and returned about 101 g; Luna 15
  crashed at 15:50 UT on 21 July 1969, about two hours before Eagle lifted off (the mockup said "a
  few hours"); the X-15 dropped at about 45,000 ft, burned about 80 s and flew about 10 minutes;
  it landed on two skids at the back and a nose wheel, and on 9 November 1962 a skid gave way on a
  landing at Mud Lake and the second X-15 flipped over (Jack McKay was hurt and flew again; the
  replay's runway-landing failure); on 15 November 1967 the third X-15 went into a spin near its peak
  and broke up, killing Michael Adams (the replay's rocket-plane flight failure);
  Luna 3's camera had 200 and 500 mm lenses, and a spot of light from a cathode-ray tube scanned the
  developed film; Venus's 90 bar is about 900 m of sea; Venera 7 (15 December 1970), the first
  landing on another planet, came down on its side and its faint signal was only found in the
  recordings afterwards; Mars 3 (December 1971) fell silent seconds after landing; Viking 1's sampler
  arm stuck on a locking pin that should have dropped out, and commands from Earth freed it (the
  replay's probe landing and surface work); the planetary window marks on E-23 sheet 2
  match the launches (Venera 1 February 1961, Mariner 4 November 1964, Mars 4 July 1973). The
  sunlight shares, signal delays and "twice the energy" are arithmetic. The concept chips reach the
  standard pack's programs and components too (2026-10-09).
- **E-29's second sheet, "Where does space begin?"** (the user's idea, 2026-10-23, drawn by
  `assets-raw/explainers/make_e29_2.py`): why the edge of space is drawn at 100 km.
  The user's account, checked: the higher a plane flies, the faster it must go to hold its height, and
  where that speed would pass orbital speed, flying has turned into orbiting. Corrections to it: the
  crossing isn't slightly below 100 km but well below, von Kármán's 1956 chart giving about 275,000 ft
  (84 km) and McDowell's 2018 recalculation always 70 to 90 km (*Acta Astronautica* 151; *Physics Today*,
  "Where does outer space begin?"); von Kármán's paper was about aircraft overheating and never mentions
  orbit or space, and Andrew Haley named the line in 1959 from its chart; the FAI settled on 100 km in the
  1960s as a round number; the US Air Force and NASA use 50 miles (80.5 km), and the FAI considered 80 km in
  2018 without changing. The curve is the standard atmosphere's densities (1976) and one wing loading
  set to cross at 84 km; a tenth to ten times that loading crosses between 69 and 91 km, which is why
  the line hardly depends on the craft. At 50 km the air is about 1,200 times thinner than at sea level.
- **E-31 "Cabin air"** (the user's idea, 2026-11-04; the brief in `ui-spec.md` component 18; its chip on
  the six first crewed programs): why the American capsules breathed pure oxygen at a third of an atmosphere, what partial
  pressure is, and why the share was set well above the air's. **Corrections to the user's account:**
  - *Not both sides.* Only the American capsules (Mercury, Gemini, Apollo) flew pure oxygen at low pressure. Vostok,
    Voskhod and Soyuz carried a nitrogen-oxygen mix like air at about one atmosphere (760 mmHg, about 80/20; NASA
    SP-4209, *The Partnership*, ch. 3-3).
  - *The pressure:* 258 mmHg, about 5 psi, 0.34 atm (SP-4209), so "about 30 %" is close. Its oxygen is about 13
    points above the air's 21 %, which is about 1.6 times the oxygen pressure a person breathes at sea level, not 10 %
    more.
  - *Not boiling blood.* Water boils at body temperature only below about 0.062 atm (47 mmHg, 6.3 kPa, the Armstrong
    limit, about 19 km up), a fifth of the cabin's pressure; even then blood in the vessels doesn't boil, only exposed
    fluids (saliva, tears, the lungs' moisture). The real reason for the margin is the lungs: inside them water vapour
    (47 mmHg) and carbon dioxide (about 40 mmHg) always take a fixed share, about 0.11 atm, whatever is breathed
    (the alveolar gas equation). Air at sea level leaves about 100 mmHg (0.13 atm) of oxygen in the lungs; pure oxygen
    at the air's 0.21 atm would leave only about 72 mmHg (0.095 atm), less than at sea level; matching sea level takes
    about 187 mmHg (0.25 atm, 3.6 psi); the cabin's 0.34 atm leaves about 171 mmHg (0.22 atm). Our arithmetic, from the
    textbook equation (lungs' CO2 40 mmHg, water vapour 47). The margin also covers a slow leak and the suits, which
    ran lower (Apollo's about 3.7 to 3.85 psi in EVA; the Smithsonian gives 3.0 psi as the minimum safe oxygen pressure).
  - *Why low pressure:* weight above all; the hull carries a third of the load, one gas needs one tank and a simple
    pressure gauge instead of a mixing system, a small leak bleeds slower, and the suits ran at nearly cabin pressure,
    so a crew could switch to suit oxygen without the bends (SP-4209; Discover, "Why Apollo had a flammable pure
    oxygen environment"). Apollo's command and service module couldn't hold more than 414 mmHg (8 psi) for
    structural reasons, which shaped Apollo-Soyuz (SP-4209 ch. 5-2). The 1961 Apollo request asked for a mixed
    atmosphere; Gilruth signed the change to pure oxygen on 28 August 1962 (Discover); a 1964 NASA paper gives
    "simplicity of control engineering and minimization of weight" for Mercury's choice.
  - *The price:* fire, and prebreathing. US crews breathed pure oxygen before launch to wash the nitrogen out of their
    blood (SP-4209). Apollo 1 (27 January 1967): a pad test in pure oxygen at 16.7 psi, about 2 psi above the outside
    air, killed Grissom, White and Chaffee; afterwards the cabin launched on 60 % oxygen and 40 % nitrogen, changing to
    pure oxygen in orbit, and flammable materials were cut (NASA's *Biomedical Results of Apollo*, as quoted). Bondarenko
    died in 1961 in a low-pressure, oxygen-rich chamber (checked 2026-10-05: at least 50 %, not pure).
  - *The Soviet price:* a heavier hull for the higher pressure, and prebreathing before a spacewalk. Leonov
    (18 March 1965) and Belyayev prebreathed oxygen; his suit ran at about 0.4 atm (5.8 psi) and he let it down to about
    0.27 atm (3.9 psi) to bend it back into the airlock (Smithsonian NASM, 2015; Wikipedia says below the safety limit,
    the Smithsonian within the limit for the lungs: the sheet says neither).
  - *Apollo-Soyuz* (July 1975): a docking module served as an airlock between the two atmospheres, and Soyuz lowered
    its cabin to about two-thirds of an atmosphere with more oxygen, so crews could cross without long prebreathing.
    Figures vary by source (10 to 10.2 psi; 500 to 540 mmHg; about 30 % oxygen in flight, 40 % in the Soyuz 16 plan),
    so the sheet says "about two-thirds of an atmosphere".
  - *Mercury's cabin, settled at the build:* the Mercury capsule's `longDescription` said "about 0.38 bar" (5.5 psi,
    from a secondary source); SP-4209 gives 258 mmHg (0.34 bar) for Mercury, Gemini and Apollo alike, and NASA's own
    papers give Mercury 5 psia of nearly pure oxygen (NTRS 19620004471, 19640020431, 19760005585). The description now
    says "about 0.34 bar" in both packs.
- **Concepts: linking game content to the explainers** (user's idea, 2026-10-07; `ui-spec.md`
  component 19). Programs, configurations, steps, rockets and
  capsules each list the real-world concepts behind them; a Concepts section on their screens
  shows them as chips that open the right explainer sheet, with a gold dot until the player
  has read it. This replaces the proposed "New idea" notices. No inline links in text (user,
  2026-10-07); if other text should link, it gets the same chip row below it.
  - **Audit of the early era** (2026-10-07; a first pass, unconfirmed; `assets/content/concepts.json`
    is the source of truth, this audit its rationale). Step ids without season
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
    - *Explainers E-06 to E-19:*
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
      - **Balloon tanks** (Atlas's pressure-stiffened skin): Atlas.
- **A Museum encyclopedia:** the deeper version of the same content; its entries are the
  explainers above (`ui-spec.md` §4.11).
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

**Moratorium on new history notes (user, 2026-11-12):** the game has enough of them. Don't add
new history notes, don't collect candidates for them, and don't propose them; the notes in the game
and the facts below are the record. Fixing a wrong fact in an existing note is still welcome. The
candidate list kept until then (Mary Golda Ross, the Nisei names, Vostok and Soyuz coming home, the
X-15, the R&D decisions' real problems, and more) is in `git show 9b7107af:design.md`.

The last three added (checked 2026-11-12):
- **Expert rescue** (`HistoryNotes.EXPERT`, on the training dialog's Expert panel): on Apollo 13 John
  Aaron, in charge of the power budget, worked out the command module's power-up on its batteries with
  Ken Mattingly, dropped from the crew two days before launch after Charlie Duke brought German measles
  (rubella) to the crews; Mattingly never caught it, and Jack Swigert flew. Sources: Wikipedia's
  "Apollo 13", "John Aaron", "Ken Mattingly". That Mattingly ran it in the simulator rests on one
  secondary account (Cooper's *Thirteen*), so the note leaves it out.
- **Telstar** (`HistoryNotes.TELSTAR`, beside "Your contract" on the Contracts screen): Bell Labs built
  Telstar 1, AT&T paid NASA for the launch (Thor-Delta, 10 July 1962): sources differ on the fee, $2.68
  million paid in advance (Wikipedia), $2.7 million (UPI at the time), $3 million (IEEE Spectrum, EBSCO),
  $3.5 million (John Pierce), so the note says "close to three million dollars". Radiation from Starfish
  Prime damaged it; its transmitter failed on 21 February 1963.
- **Lunokhod 2** (Luna 17's note in `strings.json`): landed in January 1973, drove about 39 km (the agreed
  figure; 37 km by its wheels, about 42 km from LRO pictures); on 9 May its open lid touched a crater wall
  and gathered dust, which fell onto the radiators when the lid was closed; it overheated, and its signal
  was lost on 11 May (Wikipedia, "Lunokhod 2").

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
- **How the Firsts reached the papers** (checked 2026-10-11, for the front pages, `ui-spec.md`
  §4.18): Pravda printed Sputnik 1 on 5 October 1957 as a plain TASS report on page one, given no
  more room than a report on Marshal Zhukov's visit to Yugoslavia, and without naming the satellite;
  the world's reaction made it a sensation, and the next day's issue, 6 October, gave it the whole
  front page (checked 2026-10-21: Smithsonian Air and Space and Trinity College's "Where Did Sputnik
  Get its Name?" agree). Moscow radio announced Gagarin's flight at 10:02 Moscow time on 12 April 1961,
  while he was still in orbit: he landed at about 10:55. Yuri Levitan read that bulletin (checked
  2026-10-21: the archive recording in Suspilne's media library, a 1961 newspaper account, firstorbit.org's
  transcript). Both are in the space-race news (`space-race-news.json`, 2026-10-21). Sources: Smithsonian Air
  and Space ("We Shocked the World"), The Moscow Times ("Space race started with a cosmic blunder"), NASA
  History, RussianSpaceWeb ("Vostok 1"). Our papers' headlines and articles are our own, never a real
  paper's.
- **How the leaders answered a rival's First** (checked 2026-10-22, for the follow-up pages, `ui-spec.md`
  §4.18.1; our articles quote nobody real). Shown in the game as history notes: the first two on the statement
  dialog, the third on the follow-up page's strip (`historyNotes` in `front-page-reactions.json`):
  - Eisenhower played Sputnik down at his press conference of 9 October 1957: the satellite itself did not
    raise his apprehensions, "not one iota". Biographers count it among the most hostile press conferences
    of his presidency, and the public did not believe him; he knew from the secret U-2 flights how far the
    Soviet missile programme really was. Sources: NASA History ("One Small Ball in the Air", SP-4510), American
    Heritage ("Ike Shrugged at Sputnik"), CIA Studies in Intelligence ("Sputnik and US Intelligence").
  - Kennedy congratulated the Soviet scientists and engineers on Gagarin's flight in a written statement on
    12 April 1961, calling it an outstanding technical accomplishment, and at his press conference the same
    day conceded that the Soviets were ahead and that catching up would take time. Sources: the American
    Presidency Project (the statement), The Battalion of 13 April 1961 (the press conference).
  - Apollo 11 carried two Soviet medals honouring Yuri Gagarin and Vladimir Komarov, both dead by then, as
    Nixon announced on 17 July 1969: Frank Borman had been given them in Moscow by the cosmonauts' widows. The
    crew left them on the Moon in a small memorial packet with an Apollo 1 patch and a gold olive branch.
    Sources: the American Presidency Project (Nixon's statement), History News Network and Astronomy
    ("Apollo 11 brought a message of peace to the Moon").
- **In memory** (checked 2026-10-22, for `ui-spec.md` §4.18.3 and §2.6; shown as history notes):
  - *Fallen Astronaut:* Apollo 15's David Scott left a small aluminium figure and a plaque with 14 names, eight
    American and six Soviet astronauts and cosmonauts who had died, in alphabetical order, at Hadley-Apennine on
    2 August 1971, about 20 feet north of the rover; he made it public only after the flight. The figure is by
    the Belgian artist Paul Van Hoeydonck. Sources: NASA Science ("Memorial to fallen astronauts on the Moon"),
    Scott's memorandum (NASA ALSJ), Wikipedia ("Fallen Astronaut").
  - *Apollo 11's tributes:* Armstrong and Aldrin left an Apollo 1 patch for Grissom, White and Chaffee and two
    Soviet medals for Gagarin and Komarov; NASA's catalogue of man-made material on the Moon lists them.
    Sources: NASA History catalogue, Astronomy.com, Britannica.
  - *Soyuz 11:* the Soviet government gave Dobrovolsky, Volkov and Patsayev a state funeral; NASA astronaut
    Thomas Stafford represented the President of the United States and was one of the pallbearers; they were
    buried in the Kremlin Wall. Sources: NASA ("50 years ago: remembering the crew of Soyuz 11"), AMU Edge. (That
    the Soviets asked him to carry the coffin rests on one source.)
  - *"Tough and competent":* after the Apollo 1 fire Gene Kranz told his flight controllers that they were the
    cause: they had been too eager about the schedule and had shut out the problems they saw every day. From
    then on Mission Control would be "tough" (always accountable for what it did or failed to do) and
    "competent" (never taking anything for granted); the words were to be written on every blackboard and never
    erased. Sources: Wikipedia ("Gene Kranz"), The Space Review. When he spoke differs between accounts (the
    Monday after, or two days after).
  - *Gemini 8* (for §2.6, a flight cut short keeps its goals): Armstrong and Scott made the first docking of two
    spacecraft in orbit, with an Agena, on 16 March 1966; a thruster stuck on soon after, the craft tumbled at
    about one turn a second, and with most of the reentry fuel used to stop it the flight was cut short; they
    came down safely in the Pacific. Sources: Space.com, NASA, Space Center Houston.
  - *Checked and not used:* no Soviet condolence message after Apollo 1 was found; the US astronauts' telegram
    of condolence after Soyuz 1 rests on one 1967 report. Valentin Bondarenko's death in a fire in an
    oxygen-rich chamber on 23 March 1961 was first made public in Izvestia in 1986 (James Oberg; for the secrecy
    idea, if it's ever built).
- **Theories of the day** (checked 2026-10-22, for `ui-spec.md` §4.18.2; shown as history notes, the words in
  `theories.json`; our articles name no real person):
  - *Deep lunar dust:* Thomas Gold proposed in 1955 that the lunar plains were covered in fine dust; the press
    turned it into a dust sea that would swallow a lander, a version Gold said misquoted him, and he never
    accepted the refutation. The Ranger close-ups (1964-65) did not settle it; Luna 9 (landed 3 February 1966)
    and Surveyor 1 (2 June 1966) did: neither sank. Apollo found a few inches of dust. Sources: JSTOR Daily ("The
    first lunar lander and the great Moon dust debate"), Smithsonian Air & Space ("Shooting the Moon"), Cornell
    Chronicle, Britannica (Luna), Air & Space ("1966: the (real) first Moon landing").
  - *Eating weightless:* doctors could not rule out that weightlessness would stop a pilot swallowing or seeing.
    Gagarin ate from tubes on Vostok 1; Glenn ate applesauce from a tube on 20 February 1962; the Mercury pilots
    reported no trouble chewing, swallowing or digesting. Sources: Smithsonian ("What do astronauts eat"),
    Astronomy ("First food in space").
  - *The radiation belts:* Explorer 1's counter (James Van Allen's team) kept falling silent; Explorer 3's
    recordings showed the radiation saturated it. Announced at a joint meeting of the National Academy of Sciences
    and the American Physical Society in Washington on 1 May 1958, with much publicity; the press named the belts.
    The reach on the public is from that; how many front pages it made is unchecked. Sources: University of
    Iowa (Van Allen biography), Physics Today, Symmetry.
  - *Micrometeoroids:* engineers feared punctures of Apollo's skin; the three Pegasus satellites (1965, Saturn I)
    counted the punctures in detector panels of three thicknesses (387, 41 and 201 penetrations by the end of 1965
    in one NASA report). Whether the hazard proved smaller than feared rests on one secondary source, so the
    game doesn't say so. Sources: NASA History (Pegasus), MSFC history, NTRS 19670003479.
  - *Venus:* before 1962 many pictured a warm, swampy Venus. Mariner 2 flew past on 14 December 1962 and found a
    very hot surface, supporting Carl Sagan's runaway greenhouse. The figures differ between sources, so the game
    gives none. Sources: NASA ("60 years ago, Mariner 2"), Inverse.
  - *Mars:* Percival Lowell argued the canals were built by intelligent beings. Mariner 4 flew past on 14 July
    1965 and sent the first close-up pictures of another planet, a cratered, Moon-like surface; on 29 July the New
    York Times called it "a heavy, perhaps fatal, blow" to the possibility of life on Mars. Sources: NASA SP-4212,
    Space.com, History.com.
  - *Mascons:* found in 1968 by Paul Muller and William Sjogren (JPL) from Lunar Orbiter tracking, whose paths
    strayed from predictions. Allowing for them, Apollo 12 landed about 160 m from Surveyor 3 (NASA SVS; a figure of
    30 m in one source is wrong). Sources: Wikipedia ("Mass concentration"), AmericaSpace, NASA SVS 4767.
  - *Moon germs:* NASA's Interagency Committee on Back Contamination was formally set up in January 1966 (one
    NASA report; some accounts say planning began in 1963). Apollo 11's crew were declared releasable on 10
    August 1969; lunar soil was tested on germ-free mice, quail, oysters, insects and plants; crew quarantine was
    dropped after Apollo 14. Sources: NTRS 20170004403, NASA Apollo Flight Journal (LRL), Aeon ("The lunar pandemic
    that never was").
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
  - To the West the R-7 was the SS-6 "Sapwood", a NATO reporting name; the Soviets called it 8K71. The
    rocket itself was first shown in public in 1967, a full-size Vostok launcher at the Paris Air Show (Le Bourget),
    and a US report from the show found Western estimates of its size and configuration substantially correct
    (checked 2026-11-01, for the Foreign hardware tab's history note, ui-spec.md 4.15). Sources: Russian Space Web
    (Vostok launcher; Le Bourget 2009), GlobalSecurity (Vostok, SL-3), a 1967 report released through ISCAP
    (archives.gov, 2009-068-doc176).
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
  - **Operation Osoaviakhim** (checked 2026-10-21, Wikipedia's "Operation Osoaviakhim"): the round-up's
    name, in the early hours of 22 October 1946. It took more than 2,500 specialists from many industries
    (aviation, optics, radio, chemistry, rockets) and their families, over 6,000 people: the "over a
    hundred" above is only the rocket group, so never attach that number to the whole operation.
  - **The note is built** (2026-10-21, `HistoryNotes.POACHING`, on the offer dialog's title): Paperclip's
    120, Osoaviakhim before dawn on 22 October 1946, the rocket group's island in Lake Seliger and the R-1.
- **Bars (also called Chaika) and Lisichka** died when their Korabl-Sputnik launch failed on 28
  July 1960, 19 seconds in: a strap-on broke away and the capsule's parachutes opened too low.
  They're the dogs remembered on a failed Korabl-Sputnik 2 flight (§2.5(f)), as "Chaika". Sources
  differ on the first dog's name.
- **The Quindar tones** (found 2026-10-13, for the replay's radio voices, §5 Backlog; figures checked
  2026-10-18, a history note on the replay console's air-to-ground label, `ui-spec.md` §4.7.3): the beep in Apollo's air-to-ground audio. When CAPCOM keyed the
  microphone, mission control's equipment sent a 250 ms tone at 2525 Hz, and one at 2475 Hz on
  release, to switch the remote tracking stations' transmitters on and off; listeners heard them
  because the air-to-ground loop was recorded and broadcast as it was. The name is a maker's: Quindar
  Electronics built the tone equipment. Tones on the voice line itself spared NASA separate control
  lines to every station, which were expensive; they keyed the stations from Mercury through Apollo.
  Generated for the game in `assets-raw/audio/quindar/` (2026-10-14). Sources: Wikipedia ("Quindar tones"),
  NASA's Apollo Lunar Surface Journal ("Quindar tones").
- **Explorer 1 flew bare** (found 2026-10-20, the replay's Explorer 1): no nose fairing. The satellite was its own
  last stage, a single small motor with the instrument section on its nose, standing on the spinning tub of the
  Jupiter-C's upper stages, spun up on the pad to hold them steady. Its four flexible whip antennas lay folded for
  the launch and were swung out by the spin once it was free (the replay shows them so). The whips flexing also
  drained the spin about its long axis, so within hours it was tumbling end over end, the flat spin in G4's notes:
  an early lesson in why a spinning craft must spin about its widest axis. **Checked 2026-10-21:** the spin about the
  long axis kept the flexible whips extended (NASA History, "Explorer-I and Jupiter-C"), and their flexing drained the
  spin into a tumble (Wikipedia, "Explorer 1"). **Not found** (searched 2026-10-21): how the whips were held for the
  launch and whether the spin swung them out; don't use either in a history note (the replay's swing-out stays, user).
- **Pegasus, Biosatellite and the frogs' satellite** (found 2026-10-21, the replay's later NASA satellites): Pegasus
  rode up folded inside a boilerplate Apollo service module, under a boilerplate command module and escape tower; the
  tower went at second-stage ignition, and in orbit the boilerplate CSM (about 10,000 lb) was jettisoned into an orbit
  of its own while Pegasus stayed attached to the S-IV stage, a motor unfolding its wings to a 96 ft (29 m) span; a TV
  camera in the adapter filmed it. Biosatellite's recovery capsule separated from the satellite, fired its own small
  deorbit motor (a Star-12, per Gunter's Space Page), and came down under a parachute for an aircraft to snag in mid-air:
  Biosatellite II's was caught so after 45 hours (1967); Biosatellite 1's motor never fired, and its capsule stayed up
  until it decayed on 15 February 1967; Biosatellite III's catch was called off for cloud and rain near Hawaii, and the
  capsule with the macaque Bonny was picked up from the sea on 7 July 1969 (he died the next day). The Orbiting Frog
  Otolith (9 November 1970, Scout B from Wallops, 132.9 kg, 0.76 by 1.2 m) had an octagonal electronics section and a
  truncated cone for the frogs' package under a heat shield; after separation a yo-yo despin slowed its spin and four
  booms folded against its side swung out, raising its moment of inertia to keep accelerations below a thousandth of a
  g; the water-filled centrifuge was locked until the orbit settled, then ran cycles of about 8 minutes. **Checked
  2026-10-21:** Biosatellite's reentry vehicle was the experiment capsule, a heat shield running from the nose to a skirt
  over a conical section, and a "thrust cone or retro rocket assembly"; the payload saw spin-up and despin around the retro
  firing and up to 9 g in reentry, as predicted (B. C. Look, NASA Ames, "Biosatellite II Experiment Environment", 1968).
  The motor was a Star-12 (Gunter's Space Page, and a satellite catalog that also calls the capsule a cone; its
  sizes there have no units, so don't use them). OFO separated from the launch vehicle before its yo-yo despin (Wikipedia), so it didn't stay on the Scout's
  last stage. **Not found:** exactly where the thrust cone sat on the capsule; keep it out of history notes. Sources: Wikipedia ("Pegasus (satellite)", "Biosatellite program", "Biosatellite 3", "Orbiting Frog
  Otolith"), the National Air and Space Museum (Biosatellite 2), Gunter's Space Page (Biosat).
- **Zond 5, Pioneer 10 and Luna 16** (found 2026-10-21, the replay's probes): Zond 5, an uncrewed Soyuz 7K-L1, was the
  first craft to go round the Moon and come back (September 1968), with two tortoises, fruit-fly eggs, plants and
  bacteria aboard; its guided reentry switched itself off and it came in too steeply for a crew, splashing down in the
  Indian Ocean instead of landing in Kazakhstan. Pioneer 10's 2.74 m dish sat on a hexagonal bus; its four SNAP-19
  generators rode on two 3 m trusses 120 degrees apart, away from the instruments, and its magnetometer on a 6.6 m boom.
  Luna 16's sample came home in a capsule on top of a small ascent stage, parachuting down near Jezkazgan (24 September
  1970). Sources: Wikipedia ("Zond 5", "Pioneer 10", "Luna 16"). Mars 2's and Mars 3's descent modules were
  mounted on the bus opposite its engine and let go about 4.5 hours before reaching Mars (Mars 3's at 09:14 UT on
  2 December 1971, 4 h 35 min out), not from orbit as Viking's were (Wikipedia, "Mars 2", "Mars 3").
  **Checked 2026-10-21** (mostly from Asif Siddiqi's *Beyond Earth*, NASA, 2018):
  - Pioneer 11 was Pioneer 10's sister craft with one more instrument, a fluxgate magnetometer (Wikipedia); on the way
    out one of its generator booms briefly failed to deploy, a thruster and the dust detector gave trouble, none of it
    serious (Siddiqi). It passed Jupiter at 42,500 km, three times closer than Pioneer 10.
  - Luna 15 (Ye-8-5 no. 401) and Luna 16 (no. 406) were one design. Before Luna 15's launch one of the ascent stage's two
    1.28 kg radio transmitters was taken out to save weight, leaving no spare for the way home (Siddiqi). Luna 16's
    capsule: a small sphere of 34 kg that hit the air at 10.95 km/s and up to 350 g (Siddiqi). **Not found:** its
    diameter (one web source says about 35 cm; 50 cm and 39 kg are Luna 24's): keep it out of history notes.
  - After the release the Mars lander was to spin up about its long axis and fire a solid motor to set its entry
    (the replay's one burn is right). Mars 2's last correction was so accurate that no more was needed, but the
    computer's preset algorithm assumed an error, turned the lander the wrong way, and it entered far too steeply; its
    parachute never opened (Siddiqi). The landers were egg-shaped with petals, about 1,000 kg with a 355 kg surface
    capsule (Siddiqi); Mars 3's a 1.2 m sphere with four triangular petals, under a 2.9 m conical braking shield
    (Wikipedia).
  - Venera 9's lander rode in a 2.4 m spherical pod of 1,560 kg; parachutes pulled off its upper and then its lower
    half, it hung under three main canopies for about 20 minutes, then fell on its disc-shaped airbrake alone from about
    50 km, landing at about 7 m/s on toroidal shells built to crumple (Siddiqi, quoting Pravda of 21 February 1976;
    Wikipedia). The disc's size (the `venera-9-probe` note's "about 2 meters") wasn't found in these sources.
  - In the game since 2026-10-21: history notes on `luna-15-sample-return`, `venera-orbiter-and-lander`,
    `mars-2-orbiter-lander-rover` and `pioneer-11-flyby` (the standard pack's `strings.json`).
- **Apollo 5's "fire in the hole"** (found 2026-10-20, the uncrewed lunar module test's steps): the first lunar module,
  LM-1, flew alone and without legs on a Saturn IB on 22 January 1968: inside its adapter under a nose cone about nine
  feet tall, no CSM (the Saturn IB couldn't lift both). In orbit the cone came off, the adapter's four panels opened, and
  the LM backed away on its own thrusters (NASA's account). Its first descent-engine firing was cut short seconds in by
  the guidance computer, which found the craft slower than it expected (the thrust built more slowly than planned); the
  controllers ran
  the backup plan by hand: more descent-engine firings, then the "fire in the hole" staging, the ascent engine lit
  while still bolted to the descent stage, as it would be in an abort during a landing, then an ascent-engine
  firing alone. It went well enough that a second uncrewed lunar module flight was dropped. The first firing was cut
  after about 4 s of a planned 39 (checked 2026-10-21: NASA History's "55 Years Ago: The First Test Flight of the Apollo
  Lunar Module", Wikipedia); the `apollo-uncrewed-lunar-module-test` note says so.
- **The Soviet way to the Moon and its Blok D** (found 2026-10-20, the replay's Soviet Moon flights): on the N1-L3 plan
  the Blok D stage did the work Apollo's service module did on the way out: course corrections, the braking burn into
  lunar orbit, and then, with the LK alone on it, most of the descent, before it let go a few kilometres up and fell
  to the Moon while the LK's Blok E landed it; the same Blok E engine lifted the cabin off again. There was no tunnel
  between the LOK and the LK: the one cosmonaut who landed crossed outside in his spacesuit, and back. On the 7K-L1
  flights the Proton's Blok D fired a second time from the parking orbit to send the craft to the Moon; Zond 5 (launched
  15 September 1968) passed about 1,950 km over the far side, came home on a ballistic entry after a guidance fault, and
  splashed down in the Indian Ocean with two steppe tortoises, which survived, thinner. Sources: NASA's "50 years ago"
  feature on Zond 5, the NSSDC record, russianspaceweb.com's L3 and LK pages. The Blok D let the LK go at about 100 m/s
  about 4 km up (checked 2026-10-21: Wikipedia's "LK (spacecraft)" and Astronautix, likely one origin; RussianSpaceWeb
  gives no figures). Use them only with "about", as the `lk-lander` note does; RussianSpaceWeb's "10-second final
  descent" can't be right from there, so don't use it.
- **Apollo's flag, rock boxes and rover** (found 2026-10-20, the replay's moonwalks): Apollo 11's flag hung from a
  telescoping crossbar so it would show in the airless sky, and the crew couldn't pull the bar out all the way,
  which left the ripple that looks like a breeze (NASA's account, 1993); the soil was hard a little way down and the
  pole went in only a short way. Aldrin thought he saw it blown over by the ascent engine at liftoff, and later crews
  planted theirs farther from the lander. The samples went home in aluminium rock boxes (the Apollo Lunar Sample
  Return Container), sealed on the surface and carried up into the lander: Apollo 11 brought home 21.5 kg in 58
  samples (NASA's curation paper; the Smithsonian's record for its box says 21.8 kg, a NASA poster 21.7). On Apollo
  17, before the first drive, the hammer in Cernan's thigh pocket caught the rover's right rear fender and tore off
  its extension; the tape repair failed in the dust on the drive back, and the wheel showered them until Houston
  worked out a new fender overnight from four maps, tape and clamps from the cabin. A rover crew never drove farther
  than they could walk back: the walk-back limit, about 10 km, was worked out from the suits' oxygen, cooling water
  and batteries before each moonwalk and recalculated during it. Sources: NASA's Apollo Lunar Surface Journal
  (Apollo 17's fender), J. H. Allton's sample curation paper (NASA JSC), the Smithsonian's rock box records, a Texas
  Tech paper on the walk-back limit. How deep Apollo 11's pole went (checked 2026-10-21): only a few inches; Armstrong
  said "Six to eight inches was about as far as I could get it in" in the 1969 technical debrief, and Aldrin that the
  first 4 or 5 inches went easily (the Lunar Surface Journal's commentary after 110:10:33, `a11.step.html`). Use it only
  as his estimate, never as a measurement. It's on the `apollo-11` archive page (2026-10-21).
- **Apollo's lunar module** (the replay's Apollo, 2026-10-18): about 7 m tall and 4.3 m across its legs; the descent
  stage wrapped in gold and amber Kapton foil against the Sun's heat, the ascent stage's faceted cabin on top with its
  two triangular windows, the docking tunnel on its roof (the commander flew the docking looking up through a small
  window above him) and the rendezvous radar's dish. At liftoff the descent stage stayed behind as the launch pad.
  Sources: Wikipedia ("Apollo Lunar Module").
- **Apollo's command and service module** (the replay's Apollo, 2026-10-18): the command module a cone 3.9 m across;
  on the pad a boost protective cover over it, carried off with the escape tower once the second stage was going.
  Bound for the Moon, the CSM stayed on the S-IVB third stage through the parking orbit, and the S-IVB's second burn
  sent it there; only then did it turn round, dock with the lander in the adapter and pull it out. It came home
  under three main parachutes, built to land safely on two: on Apollo 15 (August 1971) one collapsed during the
  descent, most likely burned by the fuel being vented from the reaction control system, and it splashed down on
  two. Sources: Wikipedia ("Apollo command and service module", "Apollo 15").
- **Soyuz 7K-OK** (the replay's Soyuz, 2026-10-18): 7.95 m long, 2.72 m across at the service module's skirt, its
  solar panels 9.80 m from tip to tip (one source gives 8.37); the orbital module 3.45 m long with its probe, the
  descent module 2.24 m long and 2.17 m across. The orbital and service modules both go at the separation, and a
  soft-landing engine fires just before touchdown. On Soyuz 5 (January 1969) the service module didn't separate
  after the retrofire: the craft entered the air nose first, Boris Volynov hanging in his straps, until the module
  broke away and the descent module righted itself; the parachute lines tangled and the soft-landing engines failed,
  and the hard landing broke his teeth. Sources: Wikipedia ("Soyuz spacecraft", "Soyuz 5"), RussianSpaceWeb
  (Soyuz 7K-OK).
- **Soyuz 7K-L1, 7K-LOK and the LK** (the replay's, 2026-10-19): the 7K-L1 (flown as Zond) was a Soyuz for a loop
  round the Moon: its orbital module gave way to a support cone and a high-gain dish, the reserve parachute was
  dropped, and a gyro platform and star sensors were added for navigating far from the Earth; its wings spanned about
  9 m. The 7K-LOK, the lunar orbiter of the landing plan, was 10.06 m long and 2.93 m across, and swapped the solar
  panels for fuel cells, as Apollo's service module had them. It could dock with the LK but had no tunnel: the
  cosmonaut was to go across to the lander and back by spacewalk. The LK was tested uncrewed in Earth orbit (T2K):
  Kosmos 379 (1970) fired its engine to stand in for the hover and touchdown, which raised its orbit to 196 by
  1,206 km, then a 1.518 km/s burn for the lift-off from the Moon; one source says the test craft had no landing
  legs. The ascent burn left Kosmos 379 on an orbit reaching out to about 14,000 km, and it stayed up until 1983. It
  flew on the Soyuz-L, an R-7 with a third stage (Blok I). The LK's Blok E had a main engine with one nozzle and a
  backup with two, either side of it; at landing and lift-off both would light, and one shut down if both worked.
  None of it flew crewed. The LK flew only alone and uncrewed (Kosmos 379, 398 and 434, 1970-71), and the LOK never
  reached orbit: a LOK and a dummy LK rode the last N1, which broke up on 23 November 1972. The Kontakt docking
  system was never used in space, since the Soyuz flights planned to test it were cancelled. The game's crewed
  Earth-orbit LK test is a rehearsal of the lunar sequence that was never flown: across by spacewalk, undocking,
  the landing burn, lift-off from the descent stage, the LOK docking with the waiting ascent stage, and the
  spacewalk back. Such a rehearsal could not have fired the full 1.5 km/s lift-off burn and still be caught by the
  LOK.
  Sources: Wikipedia ("Soyuz 7K-L1", "Soyuz 7K-LOK", "LK (spacecraft)", "Kosmos 379", "Blok E", "Soyuz-L",
  "Soyuz Kontakt"),
  RussianSpaceWeb (L1, LOK), Gunter's Space Page (LK T2K, Soyuz-L).
- **The LK-700** (the replay's, 2026-10-19): a real proposal, not BASPM's invention. Vladimir Chelomei's OKB-52
  offered it in 1964 as a simpler way than Korolev's N1-L3: one craft for three cosmonauts, launched on his UR-700
  (a heavy rocket related to the Proton, never built), flying straight to the Moon's surface and back with no
  rendezvous. The UR-700's preliminary design was approved in 1965 and a further year's work in 1967; the LK-700
  reached mock-ups and component tests. The first uncrewed launch was proposed for 1972 and the first crewed one for
  1973. Sources disagree on its end: cancelled in 1974 (Wikipedia, the pack's note) or by Minister Afanasyev in 1968
  (GlobalSecurity). Its stages, as described: a big braking stage that did most of the descent and was dropped a few
  kilometres up, a landing stage with six legs (folded on the way) that also served as the launch pad, and a return
  stage that sent the capsule (from Chelomei's LK-1 circumlunar craft) home. Wikipedia's 21.2 m and 154 t are for
  the whole complex with its trans-lunar stage. BASPM's Earth-orbit tests of it are invented, their steps borrowed
  from Soyuz 7K-OK's (the spacewalks are even named after it), which is why the replay once showed an orbital module
  it never had. Sources: Wikipedia ("LK-700"), RussianSpaceWeb (UR-700), GlobalSecurity and Encyclopedia Astronautica
  (from search summaries only; their pages couldn't be read).
- **Voskhod** (the replay's Voskhod, 2026-10-18): a Vostok made to carry two or three, with a solid-fuel backup
  retro rocket on top of the descent sphere. Vostok hadn't needed one: its low orbit would have decayed within
  about ten days if the retro failed, and Voskhod's heavier load flew higher. Its crew stayed aboard to the ground,
  so a braking rocket hung in the parachute lines fired just before touchdown. Voskhod 2 (March 1965) carried
  "Volga", an inflatable airlock on the sphere (its electronics were air-cooled, so the cabin couldn't be emptied),
  jettisoned after Alexei Leonov's spacewalk, the first. Outside, his suit ballooned until he couldn't bend; he bled
  it down to a dangerously low pressure and went back in head first, not feet first as planned, overheated and
  soaked in sweat. The descent module didn't separate cleanly on the way home either: the two modules, still joined,
  spun until the connection burned through at about 100 km, as Vostok 1's had (the replay's modules separation glitch). Sources: Wikipedia
  ("Voskhod (spacecraft)", "Voskhod 2"), Spacefacts (Voskhod 2).
- **Vostok's shape** (the replay's Vostok, 2026-10-18): a spherical descent module 2.3 m across (about 2.46 t),
  which carried the cosmonaut, and a biconical instrument module 2.25 m long and 2.43 m across with the
  propellant and the retro engine; 4.55 m end to end. Sources: Wikipedia ("Vostok (spacecraft)"), Gunter's
  Space Page (Vostok-3K).
- **The Transtage** (the replay's lunar Gemini tests, 2026-10-18): the Titan III's restartable top stage was
  compact, 3.05 m across and 4.57 m long, with two Aerojet AJ10-138 engines of about 36 kN each, burning fuels that
  light on contact. Sources disagree on its length (one gives 7.6 m, which may count the interstage); the replay
  draws 4.57. Sources: Wikipedia ("Transtage", "Titan IIIC").
- **The Sputnik shock** (checked 2026-10-12, for the answer to a rival's First, §2.1(e)): Sputnik 1
  (4 October 1957) set off a year of American answers: ARPA was founded in February 1958, the National
  Aeronautics and Space Act was signed on 29 July 1958, the National Defense Education Act on
  2 September 1958, and NASA opened on 1 October 1958. On the other side, Kennedy's call to land on the
  Moon (25 May 1961) went unanswered for three years: the Soviet decree committing to a crewed Moon
  landing came on 3 August 1964.
- **Failures in public and in secret** (checked 2026-11-06, for rivals' failed attempts, §2.9): America's
  failures were public. Vanguard TV3 (6 December 1957), the first American satellite attempt, rose about
  four feet, fell back onto its pad and exploded in front of the press's cameras; papers called it
  "Flopnik" and "Kaputnik" (NASA), and its little satellite, thrown clear, is in the National Air and Space
  Museum. The Soviet Union announced only successes: a planetary probe stranded in Earth orbit got a
  "Kosmos" number to hide it, from Kosmos 21 (November 1963) on, and Kosmos 27 (March 1964) would have
  been a Venera had its upper stage worked. The four N1 Moon rocket failures (1969 to 1972) were not
  admitted until glasnost, but American reconnaissance satellites photographed the launch complex wrecked
  by the second (3 July 1969), when the rocket fell back onto its pad. Sources: NASA ("60 years ago,
  Vanguard fails to reach orbit"), Wikipedia ("Kosmos 21", "Kosmos 27"), The Space Review (Day and Vick
  on the N1 in CIA imagery). Whether Vanguard's explosion went out live on television is told often but
  unchecked here: say "in front of the cameras". In the game as the history note on a rival folder's Failed
  attempts card (`HistoryNotes.FAILURES`, user, 2026-11-06); Vanguard is also a space-race news story.
- **Stand-downs after a lost crew** (checked 2026-10-12, for the grounding, §2.5(j)): after the Apollo 1
  fire (27 January 1967, a ground test) no crew flew until Apollo 7 (11 October 1968), about 20 months,
  while Apollo 4 (November 1967), Apollo 5 (January 1968) and Apollo 6 (April 1968) flew uncrewed and
  the command module was redesigned. After Soyuz 1 (launched 23 April 1967, lost on landing the next day) the next crew flew on Soyuz 3
  (26 October 1968), about 18 months, after uncrewed Soyuz tests (Kosmos 186 and 188's automatic
  docking in October 1967, Soyuz 2). After Soyuz 11 (30 June 1971) the next crew flew on Soyuz 12
  (27 September 1973), about 27 months, with the crew now in pressure suits. The game's 4 turns are a
  year: shorter than history, on purpose.
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
- **How a Mercury capsule came home** (checked 2026-10-17, for Mercury's replay clips, `ui-spec.md` §4.7.3):
  after it left the rocket (the posigrade rockets in the retropack pushed it off), the capsule turned
  round, heat shield first; in orbit it flew that way, its nose 14.5° down. The periscope came out at
  the turnaround (Freedom 7, 5 May 1961: separation at 2:32, turnaround at 2:35). For retrofire it
  pitched to 34° nose down; its three solid retrorockets fired 10 seconds each, started 5 seconds
  apart, so they overlapped. The periscope retracted about half a minute later, and the retropack was
  jettisoned about a minute after retrofire, its straps cut, leaving the shield clear. At 0.05 g the
  capsule began a slow roll, 10° a second, to even out its lift through the reentry. A drogue opened at
  about 21,000 ft and the main at 10,000 ft, and before the splash the heat shield dropped about 4 ft
  on the landing bag, a skirt that cushioned the impact. On Friendship 7 (MA-6) the pack was kept on
  through reentry, because a signal said the landing bag might have deployed. The capsule itself: a
  blunt shield, a shingled cone, the recovery compartment with the parachutes, the antenna section on
  top, and for launch the escape tower, jettisoned once the climb was safe. The clips leave out the
  pitch angles (they fly level) and the roll. Sources: Wikipedia ("Project Mercury",
  "Mercury-Redstone 1A" flight profile), the MR-3 air-to-ground transcript (NASA JSC; Spacelog).
- **How a Gemini came home** (checked 2026-10-17, for Gemini's replay clips, `ui-spec.md` §4.7.3): the spacecraft
  was 5.74 m long, the reentry module 3.45 m and the adapter 2.29 m, 3.05 m wide at its base (the Titan II's width)
  and 2.29 m at the module's heat shield. The adapter had two sections: the retrograde section next to the shield,
  with four solid retrorockets, and the equipment section behind it (fuel cells, propellant, life support). There
  was no escape tower: the crew had ejection seats, usable only early in the climb, because the Titan's
  hypergolic propellants burn at once instead of exploding. In orbit it usually flew small end forward. For the
  way home the crew turned it blunt end forward (Grissom called it on Gemini 3), and about half a minute before
  retrofire they jettisoned the equipment section, baring the retros. The four fired one after another, 5.5 s
  apart, each burning about 5.5 s, and the retrograde section went about 45 s after the first lit. The module
  flew a lifting reentry, its centre of gravity offset and rolled to steer. A drogue opened at about 15,000 m;
  at about 3,200 m the crew released it to pull out the pilot chute, and 2.5 s later the rendezvous and recovery
  section at the nose came away and drew out the main, a 25.6 m ring-sail. Then the module was turned from nose up
  onto two-point suspension, to meet the water 35 degrees nose up, a rim of its shield first: no landing bag.
  Gemini 2 (19 January 1965) was a suborbital test of the heat shield; Gemini 4 had the first American
  spacewalk (Ed White, 3 June 1965); Gemini 6A and 7 the first rendezvous, 0.3 to 91 m apart (December 1965);
  Gemini 8 the first docking, with an Agena target vehicle (16 March 1966), cut short by a stuck thruster.
  The clips leave out the roll and the R&R section coming away. Sources: Wikipedia ("Project Gemini"), Gunter's
  Space Page (Gemini), the Gemini 12 press kit (NASA), the Project Gemini Familiarization Manual, the Gemini 3
  air-to-ground transcript (Spacelog), the Smithsonian's Gemini main parachute.
- **Gemini's spacewalks, step by step** (the replay's spacewalk clips, 2026-10-18). Ed White's hatch on Gemini 4
  (3 June 1965) wouldn't open at first: a spring that should have compressed to engage the latch gears hadn't,
  a fault McDivitt had seen in a vacuum-chamber test. Closing it afterwards was worse, and it latched only with
  White working the gears by hand and McDivitt pulling on him; it had to, for reentry. White steered with a
  hand-held gun of compressed oxygen until it ran dry. On Gemini 9A (June 1966) Gene Cernan's visor fogged over as
  he struggled to don the Air Force's manoeuvring unit, and the work was called off. Gemini 10, 11 and 12 (1966)
  began their spacewalking with a stand-up EVA, standing in the open hatch to photograph stars and the Earth, and on
  Gemini 10 Michael Collins's Hasselblad worked loose and drifted away while he was busy keeping his tether clear.
  Most of those later spacewalks were made docked to the Agena (checked 2026-10-18): Collins's stand-up EVA on
  Gemini 10 was docked to Agena 10, though his umbilical EVA, across to the Agena left by Gemini 8, came after
  undocking; Dick Gordon's umbilical EVA on Gemini 11 was docked, its task to fix a tether from the Agena's docking
  collar to Gemini's nose; and on Gemini 12 Buzz Aldrin fitted a handrail from his hatch to the Agena's collar in
  his first stand-up EVA and worked at a station on the Agena in his umbilical one. No Gemini spacewalk was made
  beside a second Gemini: Gemini 6A and 7, the one rendezvous of two capsules, had none.
  Sources: Wikipedia ("Gemini 4", "Gemini 10", "Gemini 11", "Gemini 12"), Smithsonian Air & Space ("The First U.S.
  Spacewalk"), Kosmofoto (Collins's camera), NASA ("Gemini XII").

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
  as the user's art shows it (user, 2026-10-08). **Since 2026-10-17 it's the Soviet X-15** (user): BASPM's PKA went
  up on a rocket and unfolded its wings in orbit, which broke with history too far. The converter rebuilds its
  program, craft, configurations and goals from the X-15's (`baspm_pack.py`'s `e154_from_x15`): the same three
  configurations (`mig-e-154-drop-test`, `-research-flight-1` for altitude, `-research-flight-2` for speed), the same
  numbers (costs, reliability, prestige, goals, and the research flights crediting `space-plane`), its own Soviet
  reliability transfers, and the X-15's boost on the Biosatellite goal read as BASPM's PKA read it, on OFO's. The ids
  and the X-15's numbers for the Soviet side are unconfirmed. Its suborbital, orbital, extended and joint flights are
  gone, and with them its own path to `human-in-orbit`.

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
- **Reviews:** every 16 of the *player's own* seasons (a late joiner starts at the initial rate).
  `BudgetSchedule.baspmClose()`: BASPM's ten Normal-difficulty brackets, linear between each
  bracket's ends, ceiling-rounded, flat at the ends. **After the tenth review the last bracket
  repeats** (confirmed 2026-10-06). **Catch-up** (`baspm.md` §11.7): if prestige at the previous
  review fell short of that review's raw minimum, this review's bracket shifts down by the
  shortfall, so shortfalls don't compound.
- **Diminishing returns at the top** (our own change, user, 2026-11-13; §3.10, "Money stops mattering from
  the second review"): from review 2 on (`BudgetProgress.FLATTEN_FROM_REVIEW`), a review's line flattens
  halfway along its prestige range (`KNEE_SHARE` 0.5) and rises above that at 30 % of its slope (`TOP_SLOPE`;
  both *unconfirmed*), so review 4 tops out at 34,650 instead of 49,000 and review 10 at 56,250 instead of
  65,000. Below the knee nothing changes. `BudgetProgress.reviewFunding` is the line everything prices
  with (reviews, raises, the forecast); the Public Affairs Office shows the flattened top
  (`ReviewForecast.topFunding`). The start and review 1 were money's tight stretch in the play tests and
  stay as BASPM has them.
- **Running costs each season:** every employee's own salary (drawn from BASPM's 25–60 when the
  person is created), program maintenance, mission-component maintenance, and building upkeep
  (§5, M3).
- **Debt is allowed** (user, 2026-10-03; BASPM ends the game below zero): debt blocks new
  spending through the ordinary `INSUFFICIENT_FUNDS` checks and has no other penalty.
- **Prestige-to-funds conversion, at any time** (our own addition; at any time, not only in debt,
  since user, 2026-11-02): the player picks the amount; a conversion can bring funds up to two
  seasons of running costs (with funds at −D and running costs R, at most D + 2R). Prestige is
  spent, which lowers the next review: the intended trade-off. Validated when applied, not when
  submitted.
  **A turn's limit:** the turn's first conversion records what it could have raised
  (`PlayerSnapshot.conversionLimitThisTurn`), and the turn's conversions together stop there,
  cleared by the budget. Topping up stays possible; converting and spending in turns doesn't.
  Without a limit, prestige, worth nothing once no review will pay in time, could all be turned
  into money for the last push of the race (a point earns about 25 funds over a review in bracket
  2, 6 in bracket 5, 3 in bracket 10, 2 converted). The ceiling is kept for the same reason.
  **Rate: 2 funds per prestige** (user, 2026-10-06). A point is worth about 30 funds at the first
  review, so conversion stays an emergency tool (the old 0.25 funds per point made a 1,000-fund
  debt cost 4,000 prestige). A part point is rounded up (`BudgetSchedule.fundsPerPrestige`, `prestigeCost`).
- **Budget order inside `BudgetProgress`** (`baspm.md` §11.7): fixed costs, count the season,
  run a review if due (new rate), then grant this season's funding at the new rate.
- **The game info carries the game's budget schedule** (`BudgetScheduleInfo` in
  `GameInfoResponse`), so the client prices reviews and conversions with the game's own numbers.
  Every game uses `BudgetSchedule.baspmClose()`; a configurable schedule would only need the
  server to load it.

**(e) The answer to a rival's First, the "Sputnik shock"** (user, 2026-10-12: "give other players a
bit of a boost once they miss out on a first"; `rules.RivalFirsts`). The slow-game analysis (§2.9, "A
smarter bot") found a budget snowball: a bad start means a low review, which pays less for 16 seasons,
which delays the next goals and lowers the next review. The answer acts on the review, where the
snowball turns, not on today's funds.
- **What counts:** a goal another agency settled first in the game last turn (achieved, or credited
  through its cascade), that pays first-time prestige, that this player doesn't hold, and that a
  program open to their agency can reach (`ContentCatalog.reachableGoals`: an Apollo-only goal never
  counts for the Soviets; the generic goal its cascade credits does). Agencies that reach a goal in
  the same turn are all first, so none of them is answered.
- **The credit:** **50 %** of each counted First's first-time prestige (`RivalFirsts.SHARE`,
  *unconfirmed*) goes onto `PlayerSnapshot.rivalFirstsCredit`. The next review reads the player's
  prestige plus the credit (the bracket shifted down, like the catch-up shift), then clears it. It
  counts once, never carries past that review (*unconfirmed*: keeping it longer is the next knob if
  it's too weak), and the catch-up baseline stays the player's own prestige.
- **When:** a phase of its own just before the budget (`SeasonResolver.Phase.RIVAL_FIRSTS`), so a
  First in the turn before a review counts at that review. The server reads last turn's Firsts from
  every player's goal records at the start of the turn (`RivalFirsts.settledFirstOn`), so a retry
  after a crash answers the same ones and a late joiner isn't paid for Firsts from before they joined.
- **Shown:** the turn report's budget card ("Beaten to it: the government answers.", the
  `RivalFirstsAnswered` event), the review notice ("and N more for rivals' Firsts"), the Public
  Affairs Office's review card (the bracket's ends include it, and a line says so), and the help
  topic "Money and the budget review" (token `budget.rivalShare`).
- **History** (§1.1): Sputnik 1 brought NASA, ARPA and a science-education act within a year; the
  Soviet Moon landing program was approved in 1964, three years after Kennedy's challenge.
- **Measured:** see §2.9, "The Sputnik shock, measured".

**(f) Raises between reviews** (user, 2026-10-12, the "gentler review", option 1 of four). A
review's rate used to hold for 16 seasons, so an agency that recovered from a bad start waited for
the next review to be paid for it. Now funding follows prestige up between reviews, never down.
- **Rule:** in every season that isn't a review season, once the first review has set the rate, if the
  player's prestige is above the highest prestige the rate already pays for
  (`PlayerSnapshot.fundedPrestige`, set by each review and each raise), the rate gains what the review
  in force pays for the difference: its bracket, with the shift it used (`lastReviewShift`: catch-up
  plus rivals' Firsts), at today's prestige less at the funded prestige. The raise is added, so an
  extra-funding event's raise stays on top. It runs inside the budget phase before the funding is
  granted, so the season pays the new rate. The next review sets the rate as before, and can cut it.
- **Before the first review** the starting 3,000 holds (bracket 1 pays 3,800 at its bottom, so
  raises from season 1 would hand every agency +800 on its first turn).
- **Share:** `BudgetProgress.RAISE_SHARE` = 1, all of the difference (*unconfirmed*; 0.5 measured
  nearly as well, see §2.9).
- **The catch-up baseline** (`prestigeAtLastReview`) stays the review's own prestige.
- **Shown:** the turn report's budget card ("Funding raised", the `FundingRaised` event, "new
  rate"); the Public Affairs Office ("set by review #n, raised since", what 1,000 more prestige
  would add, a raise due next season); the help topic "Money and the budget review".
  `BudgetProgress.runway` still assumes a flat rate until the next review: with raises only upward
  it's merely cautious.

**(g) Prestige is public** (user, 2026-11-02). Prestige is an agency's outward standing, so every
agency's is a known figure; funds stay secret. Rivals' figures come only from resolved turns, never
with queued actions, and the race (`RivalSummary.prestige`), the Intelligence Office's rival list and
the standing charts show them (`ui-spec.md` component 30; curves from the turn reports through
`server.StandingCache`, in memory, nothing saved; 40 dots and a window of 120, *unconfirmed*). No
dossier ever reported prestige, so Intelligence loses nothing. What it gives away is deliberate: a
drop with no failed flight suggests a rival converted prestige, the published brackets give a rough
idea of a rival's funding, and a jump or fall hints at a flight the race table doesn't show.

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
  `distanceToCap × factor × learningPotential / 100`, training factor 0.175–0.385 (ours); SET's
  R&D gain is flat, factor 0.105–0.245 (ours), clamped at the ceiling. Flying's factor is
  0.035–0.105. All three were cut by 30 % (user, 2026-11-02: staff grew too fast; *unconfirmed*),
  about 45 % slower to reach 90. **A flight** grows the seat's skills in full, the others by a third
  (§2.5(g)). **Pulling out of training** credits a share of the round's gain: its last season pays
  60 %, the seasons before it share the rest evenly, so 20 / 20 / 60 % for an advanced round (user,
  2026-11-05: training is free, and pulling out had cost nothing, as BASPM's even credit does;
  `SkillGrowth.TRAINING_LAST_SEASON_SHARE`, `partialCreditShare`). The pull-out dialogs say what the
  person keeps.
- **A personal ceiling: specialists, not everything-experts** (user, 2026-11-02;
  `SkillGrowth.ceiling`, *numbers unconfirmed*): growth runs to a ceiling of its own for each skill,
  `100 − 0.5 × (points the person's other skills hold above 75, added up)`, so the better someone
  gets at one thing, the slower and shorter the rest. Two skills stop at about 91.7 (a second 90
  needs the first at 95 or lower), five even ones at about 83.3, and three skills can't all reach 90
  through growth. Computed, never saved: people above it keep what they have and stop gaining. It
  holds for all three groups, and **Flight Directors follow the same rule** (user: capped near 83 is
  fine, "broad, not deep", §1.1): the seat's one gain still comes from the average, each fifth
  stopping at that skill's ceiling. **New hires are fitted to it** (user, 2026-11-05): a candidate
  over any ceiling has every skill's points above 75 scaled down by one factor until each fits, so
  the roll keeps its shape and leaves room to train. The flat gains (R&D, R&D decisions, press
  stories) stop at it too (`SkillGrowth.withinCeiling`). Measured (`ContentPackPlayouts standard
  smart`, the 30 % cut, the ceiling and flying's growth together): average completion 78.8 to 85.9
  seasons, a crewed mission's median chance at launch 0.64 to 0.59, astronauts lost 315 to 447 in
  200 games; `BotPlayersTest`'s early race went from 60 to 90 turns.
- **Training** (`TrainingProgress`): lengths are **BASPM's** (user, 2026-10-05): basic 4
  (astronauts) / 2 (SET) / 2 (Mission Control), advanced 3. **Basic training follows BASPM's
  shape:** it starts automatically after hiring, the player can't start it or cut it short, so
  a hire can't be bought the season it's needed. Unlike BASPM, it grows one **random** skill of
  the group on completion (user, kept 2026-10-06). **Engineers hired on the agency's first turn**
  (its season 1, a late joiner's too) train one season, not two, so a program can open on the
  second turn (user, 2026-10-23: the second day had nothing to do). A quiet gift: the hire screen
  still quotes two seasons, and neither basic-training event (extended, failed) can happen on that
  first season (`RandomEvents.firstSeason`; the roll passes to the pool's next event).
  **Permanent** training repeats advanced-length rounds until a target level, stopping when a
  round gains less than 0.05.
- **Expert trait** (replaces Tiger Teams, user, 2026-09-27 and 2026-10-02): an astronaut or
  controller (never SET) with a skill at 80 or more can train toward Expert in it. Each season
  succeeds with `25 + 10 × (skill − 80) / 20` percent, with no skill growth; success grants the
  trait for good. **One Expert skill a person** (user, 2026-11-01; those who already hold more keep
  them, and someone already training toward a second finishes): an Expert can't start Expert
  training again. Expert training is the fourth kind in the shared training dialog. How Experts
  rescue steps: §2.5(i).
- **Rocket-plane wings** (user, 2026-10-26, `rules.RocketPlaneWings`): a personal mark an astronaut
  earns by flying a rocket plane (the X-15, the MiG E-154; any of their flights) home. Kept for good
  (`Astronaut.rocketPlaneWings`), it makes every capsule step they pilot safer: a step whose crew work
  reads piloting from a seat held by a pilot with wings gains **4 points** on its base chance, once
  however many such pilots share it (*unconfirmed*; the findings' size, §2.13). Any crewed flight
  outside the rocket planes counts as a capsule. The season pass and the planning odds read it alike
  (`MissionAssessment.StepOdds.wings`). It gives the rocket plane a reason to fly while the capsule is
  in R&D, beyond prestige and contracts (its `space-plane` goal only boosts Mercury's capsule by 1).
  Shown: a "WINGS +4" badge on the mission plan's step, "Rocket-plane wings" on the roster and the
  person page, a People line in the turn report (`RocketPlaneWingsEarned`), a career entry
  (`CareerEntry.Kind.WINGS`), the help topic "Astronauts" (token `missions.wingsBonus`). History: §1.1.
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
- **Pay expectations** (user, 2026-11-13; `rules.PayExpectations`; *every number unconfirmed*, and **off
  until its screens are built**: `-Drules.pay` turns its parts on in the playouts). Morale was easy because
  work alone fills it to 100 and high morale bought nothing; now pay sets how far work can fill it, and
  morale changes how well people work.
  - **The expectation:** each person's `expectedSalary`, set to their salary when hired (a recruit's new
    salary when poached; a save without it reads as their salary). It rises at **every funding review** by
    10 % for everyone (the market), and with **performance**, the turn it happens (from the season's career
    lines, never recounted): a flight, console shift or CAPCOM shift without a failed step +2 %; a First
    (crew or ground) +10 %; the Expert trait +10 %; a fulfilled ambition +5 %; an engineer's finished R&D
    stint +1 % per 5 reliability points added, pro rata (engineers never fly or become Experts). In one turn at most
    +15 % from performance. A matched poaching offer raises the expectation to the new salary; any other raise
    leaves it alone, and a cut doesn't lower it (so a cut now hurts twice).
  - **The ceiling:** morale can't rise above 100 − 3 × the percentage someone is underpaid. A review unanswered
    leaves people 9 % under (ceiling 73, no longer working at their best), two 17 % (48, working worse), three
    25 % (25, resigning). Work fills morale only up to the ceiling;
    above it, morale drifts down 2 a season. Drift follows the season's morale rules: halved for Steady, none
    while sidelined. Paid at or above expectation: no ceiling.
  - **What morale does:** from 80 up, a person's skill counts 5 % more on missions (crew, consoles, CAPCOM)
    and in R&D; below 50, 5 % less. Applied where the skill is read, so mission plans, looking ahead and the
    advisor show it too.
  - **Raising one person** (new): from their page, to their expectation or by +5 / +10 %, once per person per
    season; morale +1 per percent and loyalty by half, as a department raise. Raises only: cuts stay a
    department action. **Manage department** keeps its flat percentage and gains "bring everyone up to
    expectations" (each underpaid person to their own expectation, morale by their own percentage), which uses
    the department's once a season.
  - **Shown:** pay against expectation on the person page, the department's underpaid count and the cost of
    meeting it in Manage department, and in the turn report when expectations rose (the review) and who is
    underpaid (`ui-spec.md`, to be specified).
  - **Bots** (`PayExpectations.botPolicy`/`nextBotStep`, shared by the playout bots and `FillerBot`): anyone 5 %
    or more under, the department is brought up to expectations when the funds above a season's funding cover two
    seasons of it, else its best people first.
  - **Meeting an expectation has no cap** (one person or a department); a percentage raise of one person is capped
    at +50 %, as the department's slider.
- **CAPCOM:** the astronaut on CAPCOM grows the skills they used that season, as a controller
  does (confirmed 2026-10-06).
- **PR duty for astronauts** (user, 2026-10-06; `rules.PrDuty`): a **standing order** for an idle
  astronaut to tour and appear in public instead of training, until recalled. Real precedent: the
  Mercury Seven's *Life* deal and Gagarin's world tours (§1.1). Every number here is our own,
  confirmed by the user on 2026-10-06; tune in play.
  - **Astronauts only** (user: the public wanted the people with "the right stuff").
  - **Busy, not protected:** state `PR_DUTY` counts as busy for morale (+3 a season, as training)
    but never protects from departure. No skill growth. An astronaut on tour can't fly or sit at
    CAPCOM; recalling is free and immediate. An Expert on tour still helps rescue steps, at 75 %
    (user, 2026-10-06), like a controller on another mission. A touring astronaut can resign,
    retire or be dismissed, but random events that sideline or remove staff only pick idle
    astronauts.
  - **Yield with fame** (user): Leadership / 10, plus 3 per completed flight and 5 per completed
    flight that set a first in the game, at most 25 a season. A flight credits its crew (never
    CAPCOM) when it completes; a first counts once per flight however many goals the cascade
    credits (`Astronaut.flightsFlown`, `firstsFlown`).
  - **Diminishing returns** (user): ranked by yield, the astronauts on tour earn 100 %, 60 %,
    30 %, then nothing (ties keep roster order). One function serves the season pass, the
    dialog's projection, the badge and the Public Affairs Office.
  - **Rust** (user): from the third season in a row on tour, Piloting and Fitness each lose 1
    a season, floored at 0. The count lives in the season pass (`prDutySeasons`), so a recall and
    restart on the same day doesn't reset it; a pass that finds the astronaut off duty does. An
    Expert keeps the trait even below 80.
  - **Press stories:** each season on tour has a 10 % chance of making the papers, 70 % of them
    good. A good story adds half the season's yield, a bad one turns the season into a loss of
    half of it. Stories live in each pack's `pr-press.json`, a hashed, frozen pack file (ids never
    reused; an edit reaches new games only). No randomness is drawn while nobody is on tour.
  - **No repeats** (user, 2026-10-06): `PlayerSnapshot.usedPressStoryIds` records the stories a
    player has seen; a draw picks only unseen ones of its tone, and once every story of a tone has
    run that tone starts over. Per player, not per astronaut: the player reads them all.
  - **Effects beyond prestige** (user, 2026-10-06; `rules.PressEffects`, authored in the JSON as
    `effects`, kinds in `PrPress.EffectKind`; the turn report adds one sentence per effect). 16 of
    the 39 stories carry one:

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
    - Applied effects are stored in the turn report (`PrDutyPerformed.pressEffects`), so old
      reports still read right.
  - **The stories** (user: a good variety, mostly fictional, some real events with the player's
    astronaut in the historical person's place as an easter egg): 27 good and 12 bad, about the
    same 70/30 split as the draw, so each story turns up about as often; roughly 5 to 20 stories
    per player in an early-era game, so repeats stay rare. Each story reads for either agency and
    any astronaut (no pronouns, no country-specific terms), models nothing the rules don't (no
    grounding injuries, no dismissals) and can happen twice to the same person. Ten are nods to
    history (§1.1).
  - **Timing:** the phase runs before the budget, so PR prestige counts at a review that season,
    as mission prestige does.

**(d) People worth caring about** (user's idea, 2026-10-07; built 2026-10-07 to 10-09 in five
**phases**: (1) chemistry scales output; (2) traits, reveals and hints; (3) rapport and the chemistry
preview in the assignment dialogs; (4) ambitions, loyalty and the career page; (5) poaching and the
background check, with intelligence). The aim: employees stop
being a commodity. Players learn who their people are, care about them, and face hard calls such as
letting a brilliant troublemaker go. Decided by the user: the split between morale and chemistry,
rapport that builds over years, hidden traits with ambiguous hints, the background check through
intelligence, ambitions in every group for some people only, career records, poaching; none to two
traits a person, chemistry's ±15 % band, and rapport's range (−100..+100) and threshold (±30), all
confirmed 2026-10-07; the drift of 10 (user, 2026-10-08). Phases 1 to 3's other numbers are our
defaults, accepted for play testing (2026-10-07) and *unconfirmed*; phase 4's (ambitions, windows,
outcomes, loyalty) are **confirmed as defaults** (user, 2026-10-08), to tune after the play test;
poaching's and the 2026-11-01 relationship rules' are *unconfirmed*. Rules in `rules.Personality`, `Teams` (the TEAMS phase
after RESEARCH), `Chemistry`, `TraitEffects`, `Careers` (a CAREERS phase after TEAMS), `Poaching`;
model `Trait`, `TraitState`, `Rapport`, `Ambition`, `CareerEntry`; events `TraitRevealed`,
`RelationshipShown`, `AmbitionSettled`, `OfferSettled`; screens in `ui-spec.md` components 20 to 22,
§4.14 and §4.16 (canvases `Launch Window — People` and `Launch Window — Poaching`), the known-only
numbers in `client.view.People`, words in `client.view.TraitTexts`.
- **Morale stays personal and public.** It moves with what happens to the person and the agency (a
  successful flight lifts everyone, even a quarrelsome crew), and keeps driving resignation and the
  hiring pool's quality (c).
- **Teams.** A team is the engineers on one component's R&D this season, or one of a mission in
  flight's two teams: its crew with CAPCOM, or its flight controllers (user, 2026-10-08). Someone
  working alone is a team of one. A **work season** is a season spent in a team; training and PR duty
  don't count. Teams are captured after departures and before missions resolve (a finishing mission
  releases its crew); the TEAMS phase skips anyone lost meanwhile. A person's **project** (what they
  last worked on, and for how many seasons in a row) feeds Restless.
- **Rapport** is kept per pair in `PlayerSnapshot` (both ids, the rapport, a hidden affinity, the
  seasons shared), lasts across projects and years, and is lost when either leaves. When two people
  first share a team, the pair gets an **affinity**: a random draw from −1 to +1, plus +0.5 for each
  Easy-going member, −0.6 for each Abrasive one, +0.2 for each Team player, −0.2 for each Loner, and
  −0.4 if both are Must be the best, clamped to −1..+1. Each work season they share, rapport moves by
  **10 × affinity** (`RAPPORT_DRIFT`, user, 2026-10-08, so the strongest pair shows in 3 seasons,
  before a reveal due in 4), within −100..+100, only within each team. It shows once past ±30
  ("work well together", "clash").
- **Few relationships, each one felt** (user, 2026-11-01; *every number unconfirmed*): with 31 shown
  among 19 people by season 37 of a play test, relationships had become noise. Someone can have one
  friendship and one clash (`Personality.RELATIONSHIPS_OF_A_KIND`); Easy-going two friendships,
  Abrasive two clashes. If either of a pair already has the opposite kind, it shows only on the
  pair's own 35 % roll, drawn after its affinity from the pair's stream. A pair held back stays just
  short of showing and nothing is saved: it shows if what held it goes. Relationships from R&D
  decisions ignore all this (they're the player's doing); those already over the limit keep them.
- **Chemistry** of a team is a number from −1 to +1: **±4 % output for each shown relationship in
  the team** (`Chemistry.RELATIONSHIP_OUTPUT`, user, 2026-11-01, whatever the team's size; hidden
  rapport counts for nothing), plus the team traits, clamped. It scales the team's output by
  **1 + 0.15 × chemistry**: the component's R&D reliability gain, and the mission's crew and
  controller contribution sums (each still capped at 100). On a mission each team scales its own
  people's share of a step: the crew's the crew sum and CAPCOM's share of the controller sum, the
  controllers' the rest (`Chemistry.MissionChemistry`, used alike by the resolution and
  `MissionAssessment`). Training is done alone, so chemistry doesn't touch it. Team traits: **Team
  player** +0.15 for each one; **Loner** +0.6 when alone, 0 in a pair, −0.3 each in a team of three
  or more; **Must be the best** +0.2 when they're the team's most skilled, else −0.3 (R&D: the
  component's skill; missions: within their own group by their best skill used). Heads are counted
  per team (a solo cosmonaut with no CAPCOM is a team of one); Cool under pressure and Mentor read
  the whole mission. Measured (2026-11-01): relationships shown in 24 seasons 6.4 to 3.7 a game, the
  race within noise (the bots never choose teams for chemistry).
- **Traits:** **none, one or two** per person (user, 2026-10-07; 20 / 50 / 30 %, *unconfirmed*),
  distinct, from those the group can have, hidden when hired. A person without traits is the safe,
  unremarkable hire. Each trait gets a **reveal point** of 2 to 4 work seasons; Abrasive also shows
  when one of their pairs first passes −30, Easy-going when one passes +30. The turn report says so ("After two seasons on the Atlas team, Kowalski turns
  out to be a workaholic"); after 4 work seasons the page reads **"nothing more to discover"**.
  Effects (personal):
  - **Workaholic** R&D gain ×1.1, idle morale loss ×3 · **Steady** the season's morale change halved
    (rounded toward zero) · **Ambitious** training gain ×1.2, −2 morale a season once 6 seasons have
    passed without training · **Glory seeker** (astronauts) flight skill growth ×1.5, −4 morale each
    time a crewed mission launches without them (not on it as crew or CAPCOM, nor assigned to another
    one) · **Perfectionist** (engineers) R&D gain ×0.85, but while on its team the component's R&D cap
    is 3 points higher (what's gained above the normal cap stays) · **Cool under pressure**
    (astronauts, controllers) +0.03 on the saving throw for each one on the mission · **Restless** R&D
    gain ×1.15 in their first 2 seasons on a component, ×0.85 from their 9th season on the same one ·
    **Quick study** training gain ×1.25 · **Mentor** teammates less skilled than the mentor gain skill
    ×1.2 (R&D skill gain and flight skill growth).
  - In a team: **Loner**, **Easy-going**, **Abrasive**, **Must be the best**, **Team player** (above).
  - Glory seeker is astronauts only, Perfectionist engineers only, Cool under pressure astronauts
    and controllers; every other trait any group.
  - Workaholic, Steady and Ambitious act on the season's own morale change only, not on salary
    changes, press stories or random events. **Early tells are welcome** (user, 2026-10-07):
    Workaholic and Steady change the visible morale numbers before they're revealed.
  - Every rule that checks an R&D cap reads `TraitEffects.effectiveCap` (R&D, the release after a
    flight, the refusal to assign at the cap, which counts only a revealed Perfectionist).
- **Hints at hiring:** each candidate shows one vague hint from a colleague ("colleagues call her
  driven"). A telling hint is always true to one of the person's traits, but every hint is shared by
  several traits, so a player can't learn a code. A candidate without traits always gets a **bland
  hint** (reliable, pleasant to work with, hard-working), one with traits 1 in 4, so a bland hint
  never proves there are none. Hints: *driven* (Workaholic, Ambitious, Must be the best), *calm*
  (Steady, Cool under pressure, Loner), *sociable* (Easy-going, Team player, Glory seeker),
  *exacting* (Perfectionist, Abrasive, Must be the best), *independent* (Loner, Restless, Abrasive),
  *keen to learn* (Quick study, Ambitious, Mentor), *patient* (Mentor, Steady, Perfectionist).
- **Background check:** reveals one of a candidate's traits before hiring (or "no notable traits"),
  only for an agency with an intelligence service (§2.9, Intelligence: an active check, and a
  passive 20 % per candidate from level 1). Without one, the hint is all there is.
- **What the player sees stays honest about what's hidden.** The client's previews (the R&D board,
  mission assessment and staffing) count only revealed traits and shown relationships, and say
  "unknown factors" while any member is inside the reveal window; the season resolves with the real
  values. **The server strips hidden information** (unrevealed traits, affinities, rapport not yet
  shown, candidates' traits, unsaid ambitions) from the player state before sending it
  (`Personality.forClient`); only the player state carries hidden traits. The report's exact step
  odds (`MissionStepResolved`) include hidden chemistry; the client never shows them (accepted).
  Traits and affinities come from streams seeded by the agency and the person's or pair's ids, off
  the season's stream.
- **Ambitions** (user, 2026-10-07): in every group, only for some people. Decided when a candidate
  is drawn, from their own seed: Ambitious 100 %, Glory seeker 70 %, anyone else 25 %. Hidden until
  the person **says so after two seasons with the agency, once out of basic training**; which
  ambition is then drawn from those the agency can reach now (if none, it tries again next season).
  One at a time. "Flew" means: on a mission in flight whose season ended without a failed step.
  Windows are in seasons (doubled on 2026-10-08, user, after 1,310 of 1,852 ambitions ran out in the
  playouts):

  | Group | Ambition | Fulfilled when | Passed over when | Reachable while | Window |
  |---|---|---|---|---|---|
  | Astronaut | Fly in space | flew as crew on a crewed mission | another astronaut flew as crew | an open program has a crewed configuration | 10 |
  | Astronaut | Fly to orbit | flew as crew, crewed orbital launch | another astronaut did | a crewed orbital configuration is open | 12 |
  | Astronaut | Spend days in orbit | flew as crew in the second season or later of a crewed orbital mission | another astronaut did | a crewed orbital configuration of two seasons or more is open | 14 |
  | Astronaut | Be first at something | aboard (crew or CAPCOM) a mission that settles a goal first in the game | someone else in the agency was | an open configuration still grants a goal no agency has | 16 |
  | Astronaut | Fly on a joint flight | flew as crew on a configuration of two launches or more | another astronaut did | such a crewed configuration is open | 14 |
  | Flight controller | Be on the consoles for a crewed orbital flight | flew on a control slot of a crewed orbital mission | another controller did | a crewed orbital configuration is open | 12 |
  | Flight controller | Be on the consoles for a first | on a control slot of a mission that settles a goal first in the game | another controller was | as "be first" | 16 |
  | Flight controller | Become an Expert | earns the Expert trait in their best skill | (never) | not an Expert in anything yet | 16 |
  | Engineer | Bring a component to its R&D cap | on the team when *that* component reaches its cap | it reaches the cap without them | the component is owned, active and below its cap | 12 |
  | Engineer | Build hardware for a first | a component they ever worked on flies on a mission that settles a goal first in the game | (never) | as "be first" | 16 |
  | Engineer | Be the agency's best | the most skilled engineer in their own best skill (at least three engineers) | (never) | not already the best | 12 |

  - "Bring a component to its cap" takes the component they work on when they say it, else a
    random owned one below its cap, and **fits what the agency can do** (user, 2026-10-23: an
    engineer had 11 seasons for a cap four engineers would reach in 26): `Careers.seasonsToCap`
    projects the R&D with the agency's best team (the engineer and the three most skilled others,
    busy or not); the window is the table's 12 or the projection plus 2 (`CAP_SLACK`), whichever is
    longer, up to 20 (`CAP_WINDOW_MAX`; both *unconfirmed*). A cap further off is out of reach:
    another owned component in reach is taken, else another kind is drawn.
  - **Everyone of the group not on the flight is passed over,** busy elsewhere or not; Assemble
    mission's warning lists them all (2026-10-22). A flight passes others over once, in its first
    season (its second, for days in orbit).
  - **The window** counts down once the ambition is said: the table's seasons, −1 for Ambitious (at
    least 3), +2 for Steady. Each season, in this order: fulfilled; else passed over; else no longer
    possible (a program closed, no goal left unreached, the component capped or gone); else the
    window counts down and may run out.
  - **Outcomes** (morale, loyalty): **fulfilled** +5, +15 (Ambitious +20); **passed over** −5, −5,
    and the window shortens by one more season; **no longer possible** −2, −3; **runs out** −10,
    −15, and the person stays **disappointed** for 4 seasons. After fulfilled, no longer possible or
    disappointment, a new ambition may follow: Ambitious 75 %, anyone else 30 %.
  - Measured after the doubling (200 bot games of 24 seasons): 1,790 said, 253 fulfilled, 486 run
    out, 108 no longer possible; the bots' astronauts fulfil none, flying their first crew long
    after season 24.
- **Loyalty**, 0 to 100: starts at 50 on hiring, +1 for each season with the agency, and moves by
  half a department salary change (+10 % salary: +5 loyalty; a cut the same way down). With
  disappointment, it is what resists poaching; both show on the person page and in intelligence
  personnel files.
- **The career record**, per person, newest first, at most 30 lines (the oldest drop off; the
  hiring line and every `JOINED` line stay): hired (with the colleague's hint); training done; each
  R&D stint when it ends (component, seasons, reliability the person added; the running one "so
  far"); each flight or console shift when the mission ends (configuration, role, success or the
  step it failed at); a goal first in the game; traits that came out; relationships shown; the
  Expert trait; wings; ambitions said, met, passed over, impossible and run out; offers and moves.
  Careers stay in the player state (`StateSizeTest`).
- **The memorial** keeps the person's id, skills, *revealed* traits (never hidden ones), the year
  they joined and the career record at death, so its person page reads like the living one.
- **Poaching** (user, 2026-10-08; `rules.Poaching`, `ui-spec.md` §4.16; *every number below
  unconfirmed*): a rival agency can make an offer to one of your people. **Engineers and flight
  controllers only, never astronauts** (a Soviet crew flying a NASA astronaut is silly; an engineer
  changing sides is history, §1.1). **Offers come only from a personnel file** (Intelligence level
  3, §2.9): no file, no offer. Matching works as a standing order, so it never punishes whoever logs
  in less.
  - **Who can be approached:** an engineer or flight controller with that rival at the resolution,
    not in training or (controllers) on a mission, with the agency at least 4 seasons, and not
    approached by anyone in the last 4 seasons. Queuing checks the level, the file, the group, the
    raise and the one offer a season; the resolution checks the rest, and an offer that fails lapses
    untold. A file records the person's salary, seasons with the agency and the last approach, so
    its card can say why someone can't be approached yet and until when.
  - **The offer:** from the file, **one a season** per agency, no intelligence action used (an
    `IntelOperation` of kind `OFFER`), queued and cancellable until the resolution. Its one term is
    a **raise over the current salary: +25, +50 or +100 %**. Nothing is paid unless they come: then
    the poacher pays the new salary from that resolution and a **signing bonus of one season's new
    salary** at once, even into debt. The poacher needs room in that roster at the resolution, or
    the offer lapses unanswered (it can be made with the roster full). Not secret:
    counter-intelligence plays no part. Two offers to one person in a turn: the highest raise
    settles first (then the agency key, then the offer id), the others lapse.
  - **The defence**, a standing order: an **agency default** per group, match offers up to **0
    (never), +25, +50 or +100 %** (new agency: +25 %); **per person**: Agency default, **Keep at any
    price**, or **Let go**. **A matched offer** fails: the salary rises to the offered one for good,
    loyalty +5, both reports say so. Matching ignores funds (a raise is a running cost).
  - **One approach per agency per person, and a ceiling on matches** (the user's catch, 2026-10-09:
    a rival could otherwise double a salary every 4 seasons for nothing). Once a person has answered
    an agency's offer, matched or turned down, that agency can never approach them again, wherever
    they work (`Personnel.approachedBy`; `PersonnelFile.approachedByYou`, refused as
    `ALREADY_APPROACHED`); a lapsed offer doesn't count. A match never takes a salary past 3 times
    what it was before the first matched offer (`Poaching.MATCH_CEILING_FACTOR`,
    `Personnel.salaryBeforeMatches`), and never cuts one raised some other way.
  - **Unmatched, the person chooses.** Chance to accept, clamped to 0–90 %: base by raise (15, 30,
    50) × (100 − loyalty) ÷ 50, then +20 if disappointed, +10 if morale is under 40, +10 for
    Restless, −10 for Steady. Rolled from its own salted seed, settled once a turn with the
    intelligence (`IntelligenceBoard`), never on the season's stream. The dialog's answer range
    counts only what the file shows (the loyalty band, disappointment, the one trait).
  - **A person who leaves** travels whole in the turn's intelligence file (never sent to a client)
    and joins the poacher's roster before the budget: a new id, skills, Expert marks, career record
    ("Joined X from Y"), the filed trait revealed and the rest hidden again (work seasons back to 0),
    idle, tenure 0, loyalty 50, the agency default, any open ambition kept; the poacher's file on them
    is dropped. The old agency takes them off like a dismissal (an R&D slot frees), with their
    rapport pairs. **A person who stays** gets a career line ("Turned down an offer from X", "Stayed:
    the agency matched X's offer"). The target's report names the poacher and the offer; the
    poacher's says matched, turned down, joined or lapsed (`OfferSettled`).
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
  - Poaching's: both space programs began with poached engineers (§1.1, `HistoryNotes.POACHING`).
- **Open:** whether a trait can be misleading at first; the ambition chances, list, windows and
  losses, after play.

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

**(d) A guided progression** (user, 2026-10-26): BASPM opened a dozen programs on day one, so a new
player could pick Gemini first, a poor choice nothing warned of. The standard pack opens in stages, the goals
added on top of BASPM's by the converter (`baspm_pack.py`'s `progression_gates`, full scope only; early-era unchanged):
- **At first:** only the first satellite (Explorer 1, Sputnik 1).
- **A satellite in orbit** (`earth-orbit-satellite`): the other satellites, the rocket plane (X-15, MiG E-154) and the
  one-crew capsule (Mercury, Vostok); the lunar probes as before.
- **A person in space** (`human-in-space`): the two- and three-crew capsules and every Moon craft's Earth-orbit tests
  (Gemini and its variants, Gemini EOR's, Apollo's, Voskhod, Soyuz 7K-OK, the 7K-L1's, 7K-L3's and LK-700's).
  Opening those tests still makes most Moon versions free to open later.
- **A probe at the Moon and a person in orbit** (`lunar-probe` and `human-in-orbit`): the Moon programs. Venus and
  Mars probes stay on `lunar-probe`.
- **Inside a Moon program** (configurations' `requiredGoalIds`, crewed flights only): the lunar orbits and lander
  tests need `crewed-lunar-flyby`, the landings `crewed-lunar-orbit`, from any program. The uncrewed flights stay
  open; their goal dependencies order them.
- **The X-15 never earns `human-in-space`** (its research flights earn `space-plane`), so it's no side door to the
  bigger capsules. It gets its own reason to fly instead: rocket-plane wings (§2.2(c)).
- **Shown:** Headquarters lists locked programs with what they need; the advisor walks the stages
  (`ui-spec.md` component 24); the help topic "Programs" lists them. The filler bots' plans fly in this order.
- Measured: the smart bot's landing moved from season 84.2 to 85.0 on average.

**(e) The roads to the Moon, measured** (2026-10-26; the smart bot, 200 games, seed 42; `ContentPackPlayouts
standard smart` with `-Dbot.roads=<nasa>,<soviet>`, plans in its `ROADS`). Runs with no change on one side moved
that side's median by up to 3 seasons: read differences below that as noise. The comparison screen (`ui-spec.md`
§4.21) shows the trade-offs, never a ranking.
- **Accepted as the status quo** (user, 2026-10-26), median landing season: Gemini EOR 73, the LK-700 70 to 73,
  Apollo 77, the 7K-L3 78, Gemini direct ascent 78; the spread 5 seasons on each side (was 10 and 8 to 11). The clever
  roads keep a small lead, paid for in risk. Levers not pulled, for a later look: the LK-700's R&D or a dearer UR-700,
  more weight on Gemini EOR's two launches. The bot flies its plan one flight at a time, so it undervalues
  overlapping roads (Gemini, then Apollo); play tests may still move it.
- **Why Apollo was slowest** (81 before the changes below): not money (half its 22,012 opening gained one season) but
  time. Its first flight waited 14 seasons (Gemini's 6): the CSM developed at half Gemini's rate (R&D bands 2/4/5/7
  against 4/7/10/14), then the lunar module's tests (about 8 seasons) and the Saturn V, started only when the Moon
  flights need it. The roads whose Earth-orbit tests already need the heavy rocket (direct ascent, the LK-700) build
  it alongside the craft.
- **The lunar-orbit craft inherit like Gemini** (user, 2026-10-26): Apollo's CSM carries over 35 % of the Mercury
  capsule's reliability (was 10 %) and the 7K-L3's LOK 35 % of Vostok's (was 7.5 %), what Gemini takes from Mercury
  (`baspm_pack.py`'s `capsule_heritage`): Apollo 81 to 77, the 7K-L3 81 to 78.
- **The Nova** (user, 2026-10-26; its art the user's, 2026-10-28): BASPM's direct-ascent craft weighed 42 t, so its
  stack fit the Saturn V's 47 t toward the Moon (it landed at 71). The converter (`baspm_pack.py`'s `nova_rocket`)
  makes it 63.5 t, the stack about 67.5 t, and adds **Nova** (`nova`): heavy, 68 t toward the Moon (the eight-F-1 8L
  study), 15,000 to create, 1,500 upkeep, an R&D ceiling of 91 %, reliability carried over from the Saturn family (the
  Saturn V 30 %, C-3B 25 %, IB 15 %, I 10 %, Titan IIIC 5 %). *All its numbers are ours and unconfirmed.* The
  Earth-orbit tests still fly on the Saturn V. History: §1.1.
- **Gemini EOR's Moon discount fixed** (user, 2026-10-26: "probably a data error in BASPM"): BASPM discounted its Moon
  program 100 % through Apollo's Earth-orbit tests, unlike every other road. The converter points it at its own tests
  (`baspm_pack.py`'s `discount_fixes`), so its Moon flights are free once `gemini-eor-earth-orbit` is open; from
  Mercury alone it costs 22,483 (was 32,543).
- **No Agena for Apollo** (user, 2026-10-27: a goal mustn't come from hardware a program never flew): BASPM's Apollo
  Earth-orbit tests had a docking flight with an Agena, which no Apollo flew. The converter drops the configuration
  and the Agena from the program (`baspm_pack.py`'s `dropped_configurations`); the docking goal stays earnable
  through Gemini, the 7K-OK and circumlunar Gemini, and inside Apollo through its lunar module tests (Apollo 9's
  CSM-LM docking: `apollo-lunar-module-test-flown` credits `docking`). No goal was the docking flight's alone. Kept
  out after a second look (user): its picture showed two CSMs nose to nose, which probe-and-drogue docking can't do,
  and Apollo's real Earth-orbit dockings were with its lunar module, Skylab and Soyuz 19. Apollo's road from Mercury
  costs 45,412, 4 parts to develop, 21 flights on the way. Its `splashdown` step type, the configuration's art and
  the Agena's Apollo clip rules went with it. Kontakt in place of Igla on the lunar Soyuz stays as BASPM has it
  (user): Igla serves the 7K-OK too, one component to develop.

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
  missions; never a copy already flying, whether on this mission or one in parallel. The client's
  odds and component list for a launched mission read the launch values too.

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
  - **Korabl-Sputnik 2:** Belka and Strelka really came home, so a *failed*
    flight remembers Chaika (also recorded as Bars) and Lisichka, the dogs really lost on 28
    July 1960. No photos of them yet.
  - **Animals that came home are honoured** in the Museum's Flights tab (user, 2026-10-06):
    `animalPassengers.cameHome` lists them per successful flight, in order, and the count of
    completed flights says which are earned. Belka and Strelka (Korabl-Sputnik 2), Ham (the
    uncrewed suborbital Mercury test), Enos (the uncrewed orbital Mercury test), Chernushka and
    then Zvezdochka (the uncrewed orbital Vostok test, first and second flight). A flight that
    honours someone may remember nobody on failure; no real animal is invented a death.
    **Photos** (found by the user): Belka and Strelka (one photo of both), Ham, Enos
    and Chernushka show theirs on the Flights tab. Zvezdochka has none: her only known photo is
    non-free (fair use on Russian Wikipedia only), so it can't go in the repo.
    **A failed uncrewed test remembers an unnamed animal** (user):
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
- **Skill growth from flying:** every astronaut on the mission grows the skills their seat used in
  full and the others by a third (`SkillGrowth.FLIGHT_UNUSED_SKILL_SHARE`, user, 2026-11-02; unconfirmed):
  without it, a flight drifts everyone evenly toward the ceiling of an all-rounder. Each controller grows
  the skills their console used.
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
  reflect the cap. The replay holds its lines to it (`ExpertRescueLines`, user, 2026-11-01): an
  Expert past the cap "couldn't add anything", one out of uses "had nothing left to give".

**(j) A lost crew grounds crewed flights** (user, 2026-10-12: "gambling with the lives of
the astronauts should be where the line is drawn"; `rules.CrewedGrounding`). The
first of the "failures should be a bit more punishing" adjustments (§2.9, "A smarter bot"), kept small
on purpose: the user wants calculated risk to stay part of the game ("the right stuff"), and a harsher
failure would steer players into longer R&D waits, which lengthens the lulls again.
- **Trigger:** a catastrophic step that loses a mission's crew (`MissionProgress`, the same moment as
  `loseCrew`). Not an astronaut perishing in a random event, nor an animal passenger.
- **Effect:** for the next **4 turns** (`CrewedGrounding.GROUNDING_SEASONS`, confirmed by the user 2026-10-12) no
  configuration with crew seats may launch (`MissionLaunchOutcome.CREWED_FLIGHTS_GROUNDED`). A CAPCOM
  on the ground or an animal aboard doesn't make a flight crewed. Uncrewed flights go on, and so do
  flights already launched, including a crewed one launched the same turn as the accident (its launch
  came first). A second loss while grounded starts the count again at 4, never adds.
- **Counting:** a number of turns on the player's state (`PlayerSnapshot.crewedGroundingSeasons`),
  counted down at the start of the missions pass, before any flight resolves, so a loss in turn T's
  resolution refuses crewed launches in exactly turns T+1 to T+4 (`CrewedGroundingTest`).
- **Shown:** the turn report's People card ("Crewed flights grounded", `CrewedFlightsGrounded`), mission
  assembly's refusal under the disabled launch button, every crewed mission card's Astronauts check
  and earliest launch ("waiting on the grounding", component 25), and the help topic "How mission
  steps are decided" (token `missions.grounding`).
- **History** (§1.1): the real stand-downs ran 18 to 27 months (6 to 9 seasons). 4 is short of that on
  purpose; it's the first, small step.
- **Measured** (the smart bot, 200 games, 2026-10-12): at 4 turns the average landing moved by about
  2 to 3 seasons and the lulls didn't change; 6 turns lengthened the longest gaps. The bot doesn't play
  around it; a player can.

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
  earned (goals stay achieved, unless the crew was lost: below). Earlier seasons keep their prestige.
- **A goal counts only if the crew comes home** (the user's decision, 2026-10-22: "would we really claim
  that Gagarin was the first man in space if he had died during reentry?", Kennedy's "and returning him
  safely to the Earth"; our own, BASPM pays goals step by step). When a catastrophic step loses a crewed
  flight's crew, every goal the flight reached, and those their cascade credited, is taken back
  (`MissionProgress.withholdGoals`): out of `achievedGoals`, `cancelledGoalIds`, `goalRecords` and the
  report's `GoalSettled` events, so nobody holds it and the next agency to reach it is still first. A
  `GoalsWithheld` event names them (with those that would have been first in the game) for the report
  ("No goals for <configuration>") and the memorial page. Steps still settle as they go, so later steps
  read their goal modifiers exactly as before and prestige is unchanged (the failing step already
  replaced the season's total). A crewed flight cut short with the crew alive keeps its goals (Gemini 8's
  docking), and so does an uncrewed flight (a probe that sent its pictures home sent them). No crewed
  configuration spans two seasons in either pack, so the season's goals are the flight's; a multi-season
  crewed flight would need its earlier seasons' goals held back too. It closes the one way a First's page
  could print for a crew that died: a goal credited mid-flight through the cascade (a first spacewalk
  crediting "Human in orbit") before a fatal reentry. The steps screen says "Counts if the crew comes
  home" under a crewed flight's goals, and help says so ("Prestige and goals").
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
bot playouts (a Moon race with side missions sees about 30 good and 8 bad events in 150 seasons;
prestige and R&D boosts come 10 to 17 times a game each, reduced expenditures 6 to 10, every other
event at most about 3): 18 stories each for prestige and R&D boosts, 12 for reduced expenditures, 6
for morale boosts, 5 for extra funding, 4 for reliability setbacks and 3 for every other bad event
(`EventStories.SIZED`). No player sees a story twice until every story of its event has run; then
that event starts over (`PlayerSnapshot.usedEventStoryIds`). A story can wait for a goal
(`requiresGoal`, 2026-10-27, after a first turn told of the agency's hardware overhead): four prestige
stories that need something in orbit wait for `earth-orbit-satellite`, in both packs, keeping their
turn meanwhile. The pick uses its own salted seed (agency, turn, event), so the season's draws, and the
playouts, are unchanged. The event (`RandomEventOccurred`) and the news log entry store the story's id
and the struck person's name (the person may be gone when the report is read); the text stays in the
client. Placeholders: `{name}`, `{component}` (reliability setbacks), `{group}` (morale). Prestige,
cost and funding magnitudes are percentages.

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
- **A random resignation leans on morale** (user, 2026-10-27, after a play test where someone at 100
  morale resigned "after too many nights at the site"): each eligible person's weight is 100 less
  their morale, so nobody at 100 resigns and with everyone there the event doesn't apply (the next
  event in the pool is tried). BASPM takes anyone. The burned-out story (`res-burned-out`) has
  `"maxMorale": 60`; the other two resignation stories fit any morale. The playouts' resignation
  counts (§3.10) predate this and weren't re-measured: the event is rare beside morale's own
  resignations at 35.

### 2.8 Campaign objectives

**(a) BASPM** (`baspm.md` §12.3): a long-term objective (a goal by a deadline: victory or game
over) plus short-term objectives drawn at each review from a pool of seven predicate types
(funds, headcount, skill, reliability, a goal, a component's use count). `CampaignRTTM` races an
AI rival to one milestone goal.

**(c) Decision:** the predicate shape is prior art only; the win condition (§2.9) uses goal ids.

### 2.9 Multiplayer interaction model

No BASPM precedent beyond its 1v1 PBEM mode and rival comparison screen.
- **Decided:** start with **passive competition** (the race, rival summaries, public prestige
  §2.1(g), the standing charts) plus **in-game messages** over the game connection, never email
  (user, 2026-09-28). Interaction must never punish whoever logs in less. What exists beyond that:
  **intelligence** (below), **poaching** with matching as a standing order (§2.2(d)), **publishing
  findings** (§2.13, cooperation's first piece), rivals' **statements and condolences** on Firsts and
  losses (`ui-spec.md` §4.18.1 and §4.18.3), and a shared **contract marketplace** (§2.11).
  Cooperative modes are a later idea.
- **Intelligence** (designed with the user, 2026-10-07; `rules.Intelligence`,
  `rules.IntelligenceBoard`, state in `PlayerSnapshot.intelligence`, screens in `ui-spec.md` §4.15,
  canvas `Launch Window — Intelligence Office`;
  *every number is ours and unconfirmed*). Espionage here buys **information, never damage**: **no
  sabotage** (user, 2026-10-07), since a rival breaking your hardware overnight can't be answered
  until morning and a player-caused death would kill the fun. No BASPM precedent.
  - **Async rules it keeps:** every operation is settled with the turn (except the background
    check), defence is passive, and the service's level is a standing order. Nothing asks a player
    to react.
  - **The Intelligence Office**, an **eighth hub building** (user, 2026-10-07), because the feature
    may grow: the level dial, the operations, the folders on rivals (later perhaps intelligence
    officers as a fourth roster). No building upgrade. One name for both archetypes. A new hub
    building needs its `Hub.Place` before its hotspot reaches `assets/` (`Place.valueOf` throws on
    an unknown name).
  - **The level** (0 to 3) is a **budget dial**, no staff. The player sets a **target level**; at
    each resolution the level moves **one step** toward it (user, 2026-10-07: a network takes time to
    spool up and to dismantle), so nobody can sit at 0 and spike to 3 when a need arises. The level
    steps first in the season pass, so the candidate pool drawn that resolution and the next
    season's actions use the new level. **Cost per season** at the level in effect, paid with the
    fixed costs: 0 / 150 / 350 / 700 (against a starting funding of 3,000). Winding down costs the
    levels on the way. If funds don't cover it (in debt), the level drops one step that season.
  - **What each level brings.** A level keeps everything the levels below it bring. **Actions per
    season** equal the level; unused ones don't carry over. **Passives** are independent of the
    actions: using an action never cancels a passive roll (user, 2026-10-07).

    | Level | Active (unlocked) | Passive |
    |---|---|---|
    | 0 | none | none; a small baseline counter-intelligence |
    | 1 · Security office | **Background check** on one candidate | **20 %** per candidate to reveal a trait; rivals' failed attempts; foreign hardware 25 %; counter-intelligence I |
    | 2 · Analysis section | **Agency assessment** of a chosen rival, one area | **25 %** per season: a tidbit about a random rival (R&D or plans); foreign hardware 50 %; counter-intelligence II |
    | 3 · Field network | **Personnel file** on a rival's employee | the tidbit chance rises to **40 %** (user, 2026-10-07) and can come from any area, people included; foreign hardware 80 %; counter-intelligence III |

    The ladder is people, then agencies, then rivals' people (user, 2026-10-07); level 3's
    personnel file is what makes a poaching offer an informed one.
  - **Foreign hardware** (user, 2026-11-01; *every number and the counterparts await the user's
    review*). Rival hardware means little to a player who isn't a space nerd, so analysts explain it,
    a piece at a time. **Pool:** every program and rocket program that an agency of the other side
    has open now, minus what the player already knows; a closed program leaves the pool until it is
    opened again. **Passive:** each season, a **25 / 50 / 80 %** chance at levels 1 / 2 / 3
    (`Intelligence.HARDWARE_PERCENT`) to learn one random item of the pool, on its own salted seed. No
    lag: the chance is what keeps a player behind (a new item takes about 4 / 2 / 1.25 seasons on
    average), and a failed roll and an empty pool look alike, so the report says nothing then. What
    is learned is kept for good (`IntelligenceState.foreignHardware`), a dropped level included. It
    opens a role line, the counterpart on the player's side where there is one, and a read-only file;
    a component is known once any program needing it is. Dossiers show unknown items by name only,
    "not yet analysed". **Your own side's hardware is always known,** so a game with no agency on the
    other side (bots count) needs no special rules: the pool stays empty, and the passive's ladder
    line, the Foreign hardware tab and counterpart lines are hidden. Once the race is decided,
    everything opens. The words are in `assets/content/foreign-hardware.json` (outside the pack;
    `ForeignHardwareTest` holds every program and rocket to an entry, a role to 46 characters).
  - **Failed attempts** (user, 2026-11-06; "the same for everyone", each side's flavour in the words
    only). A program can be developed in secret, but a rocket that fails can't be hidden (history in
    §1.1, "Failures in public and in secret"). **From level 1 every rival's failed attempt is
    reported, no roll,** and counter-intelligence plays no part. **A failed attempt** is a mission
    that ended on a failing step (`FAILURE` or `CATASTROPHIC`), whatever it was: pad, launch, flight
    or the way home, crewed or not, contract flights too. A glitch the mission survives stays secret.
    A crew lost is reported here too, besides its memorial page. **When:** the turn after; the board
    settles before anyone's season pass and reads every agency's report of the turn before
    (`rules.RivalFailures`, from `TurnScheduler`; `IntelFinding.Kind.FAILED_ATTEMPT`). **Detail by
    level** (stripped when seen, never growing later): level 1 the program and roughly where
    (`RivalFailure.Phase`: on the pad, during launch, in flight, on the way home); level 2 the
    configuration and the step; level 3 the suspected component, the one with the largest share of
    the step's risk (involvement × unreliability as flown). **Where:** preparations and countdown are
    the pad, and they (or the season's start) open a launch that lasts while each step involves a
    rocket slot (or an air drop's carrier); after that a step on the way home (retrofire, reentry,
    recovery and the like) and every step after it in the season is the way home; the rest is
    flight. **Words:** a NASA-side rival's failure is read in the press ("The press reports that…"),
    a Soviet-side one picked up by tracking stations ("Tracking stations report that…") (user). It
    shows in the turn report's Intelligence section and in the rival's folder, which keeps the last
    **24** (`Intelligence.FAILURES_KEPT`, *unconfirmed*; `RivalFolder.failures`).
  - **Background check** (level 1; `BackgroundCheckAction`). The **active check answers at once**:
    the server replies during the season with one of the candidate's traits, or "no notable traits",
    and the action is used. It's desk work, so instant fits. It never fails and isn't against a
    rival, so counter-intelligence plays no part. A second check on the same candidate (a second
    action, level 2 or more) reveals another trait if there is one. **The passive 20 %** is rolled
    once per candidate when the pool is drawn, from the candidate's seed, so it shows the moment the
    pool appears and can't be rerolled. Both report "no notable traits" too, so a missing reveal
    never hints at none. A trait found this way stays revealed after hiring. **The active check is
    the game's first action that changes state mid-turn,** so it is pinned down: which trait it
    reveals comes from the candidate's seed; the answer is saved on the candidate, so asking again
    returns the same answer and a retried resolution never changes it; it can't be cancelled or
    refunded; it counts against the season's actions even if the level drops at the resolution; and
    the server's stripping lets that one revealed trait through.
  - **Agency assessment** (level 2): one action looks at **one area** of one rival, settled at
    resolution, the result a **dossier dated to that resolution** that stays in the rival's folder
    until a newer one replaces it:
    - **R&D:** the rival's components with reliability as **ranges** ("55–70 %").
    - **Programs and plans:** open programs, missions in flight, **R&D focus** (which components
      their engineers work on, with how many), **hardware not flown yet** (owned payloads never
      carried on a successful flight), and the rival's **Intelligence level** (user, 2026-10-07), so
      one assessment tells a player how hard the rival is to spy on. (No "launches scheduled for next
      season": a mission scheduled during a season launches at that resolution, so a dossier would
      always find none.) A rival's plans can still change after the report: that is the rival's
      counter without having to react.
    - **Personnel:** roster sizes and average skill per group.
  - **Personnel file** (level 3): one rival employee, in a group the player picks: the most
    skilled person there without a current file, or a person the player names from an earlier
    file. It shows name and portrait, skills as ranges, a loyalty band (low, middling, high), their
    ambition and whether they're disappointed, one of their traits, and (for poaching) their salary,
    seasons with the agency and last approach. **Never the same person twice in a turn** (user,
    2026-10-23): the files settle one after another, and each skips whoever an earlier one filed
    that turn; with nobody left in the group, the file finds nothing.
  - **Odds against a rival's counter-intelligence** (the target's level, 0 to 3): a targeted
    action succeeds at **85 % minus 15 points per level** of the target's counter-intelligence
    (85 / 70 / 55 / 40). Ranges are tighter the higher the spy's level is over the target's:
    half-width **5 + 5 × (target level − spy level + 2)** points, at least 2 and at most 15.
    **Follow-ups tighten them** (user, 2026-10-27; *unconfirmed*): an assessment of the same area of
    the same rival at most **2 turns** after the folder's dossier there is a follow-up, and each look
    in a row takes **3 points** either side off its ranges, down to 2
    (`Intelligence.followUpHalfWidth`, the row counted in `Dossier.looks`). The range is still drawn
    fresh around today's value, never intersected with the last one, since the rival's numbers move.
    A longer gap starts again at one; a failed look doesn't reset the row by itself, but its season
    counts toward the gap. The dossier's stamp says which look it was. A failed action learns
    nothing, and is **caught** with **10 % + 10 % per target level**: the target's turn report names
    the spying agency, the spy loses **20 prestige**, and the spy's report says the operation was
    blown **and what the target's counter-intelligence level was** that season (user, 2026-10-07:
    getting caught should still teach something). The catcher learns the spying agency's level too.
    A level learned either way goes into the rival's folder, dated like a dossier. **The odds a
    player sees:** exact ("55 %, their counter-intelligence was level 2 as of Spring 1962") once a
    dossier or a catch has shown the level, otherwise the full range, 40–85 %. Passive tidbits roll
    against the same counter-intelligence but are never caught (no one is sent). No secrecy bonus
    for either archetype (user, 2026-10-07: NASA didn't spill everything either).
  - **Turn report:** an "Intelligence" section with reveals, dossiers, tidbits, failed attempts,
    foreign hardware, catches on both sides, and level changes.
  - **Hidden information stays on the server:** a dossier holds only what the server granted and
    is kept in the player's own state; a rival's real values never travel to the client, as with
    traits. Prestige is the one exception: it's public (§2.1(g)).
  - **How it runs:** an `INTELLIGENCE` phase of the season pass, before the budget, applies the
    agency's findings, clears the queue, frees the actions and steps the level (the budget then
    charges the new level, `BudgetProgress.RunningCosts.intelligence`). Everything is **settled once
    a turn** (`TurnScheduler`) from every player's state with the turn's actions applied, before
    anyone's pass, and kept in `intelligence/turn-N.json`, so a retry after a crash applies the same
    findings; an operation queued after that file was written comes to nothing ("queued too late").
    Every roll draws from its own salted seed, never the season's stream. **Rivals are named by an
    opaque key** (`GameSeeds.rivalKey`, carried as `RivalSummary.playerId`): account ids never leave
    the server. **Tidbits** are one fact from one dossier area. **Folders are capped** (12 tidbits,
    8 personnel files, 10 catches, 24 failures per rival, oldest dropped), since they travel in the
    player's state (gzipped JSON, `StateSizeTest`).
  - **Balance check** (dev-tools `IntelligencePlayouts`, 2026-10-07: four agencies, each spending
    every action every season, 24 seasons, 1,000 games; only the intelligence rules run). Per
    agency: level 2 costs about 8,200 in all and gets 29 dossiers and 3.5 tidbits, caught 4.6 times
    (92 prestige); level 3 costs about 15,900 and adds 15 personnel files. 65–70 % of operations
    succeed, 7–10 % are caught. An arms race (all at level 3): 40 % succeed, 24 % caught, 306
    prestige lost per agency, roughly one mid-sized goal. Rival levels are known 77–85 % of the time.
  - **Still open:** whether the target ever learns of an operation that failed but wasn't caught
    (default: no); a dossier ages by its date only, no visible decay (default).
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
- **How long the Moon race runs** (`ContentPackPlayouts standard`; earlier bots and their traces:
  `git show 5c95135b:design.md`). The bots fly a fixed plan to the landing, one flight at a time:
  the early race, one lunar probe, the lunar craft's tests in Earth orbit, then flyby, lunar orbit,
  lander test and landing (17 flights for NASA, 18 for the Soviets).
  - **The length is fine, the lulls are the risk** (user, 2026-10-09): the colleagues played a
    racing-team manager for about nine months. A shorter race is ruled out: the Moon landing is the
    point. What matters is how many turns in a row a player has nothing to do. About 90 turns to win
    is fine (user, 2026-10-12).
  - **What sets the pace:** R&D, then money. Faster R&D shortened the race but hardly the lulls
    (`RaceLulls`, 2026-10-09): once R&D is quick the bot waits for money instead, and saving for a
    20,000 program is the same lull. The pack's other flights hardly fill them (`RaceLulls standard
    side`): a flight pays only for goals the agency doesn't hold yet, most flights in an open program
    are the plan's own, and a venture costs what the plan needs. So the fix was new things to do: the
    R&D decisions (§5, Backlog), the commercial contracts (§2.11, flights that pay for a repeat).
- **A smarter bot, and what it says about the game** (2026-10-12, `ContentPackPlayouts standard smart`,
  200 games, seeds 42 and 7; `Bot.SMART`, each part behind a system property). Diagnosed from the
  side-missions bot: it launched at a median chance to complete of 0.41 crewed and 0.48 uncrewed, lost
  about 4 astronauts a game, and hired about 265 people a game, idle specialists resigning and the
  oldest best hires retiring. The smart bot keeps nobody idle, sizes SET to the slots, hires for the
  years left (only who stays to season 100), matches skills to slots, launches early at odds 0.5
  crewed / 0.3 uncrewed, opens the plan's later rockets with spare money, and runs two teams of
  engineers per slot, swapping research and training every course (the user's ideas). Medians: NASA
  87, Soviet 96 (with R&D decisions 82 / 85); every game lands. Tried and dropped (each slower on both
  seeds): opening later *programs* early (money binds), upgrading the VAB ahead, flying later flights
  alongside the current one (`bot.parallel`), and side flights (about two seasons faster without; kept, as a player
  fills lulls). With today's rules (raises, the ceiling, the grounding, §2.2(c)) the smart bot's
  average game is about 85.9 seasons.
  - **Lulls with the smart bot:** about 33 launches a game; 18 % of gaps 5+ seasons, 3 % 10+; each
    game's longest gap a median of 10 seasons. With R&D decisions: about 50 decisions a game, offered
    in 61 % of quiet seasons, covering 70 % of the median game's longest quiet stretch.
  - **What makes the slow games slow: a budget snowball** (`-Dbot.slowGames=true`). Slow games stall
    on funds about 40 seasons a game against 2 to 4 and spend about 100,000 more on retries. Prestige
    at the review of season 32 is half, so the funding rate is too, and the gap widens: early failures
    delay the first goals, a lower review follows, then less money for 16 seasons. R&D waits barely
    differ.
  - **The user's answers** (2026-10-12): failures should punish a little more, but only in small
    steps (calculated risk fits the theme, and pushing players into R&D lengthens the lulls): the
    grounding after a lost crew (§2.5(j)). The idle-staff point became an advisor tip (`ui-spec.md`
    component 24). For the snowball: the Sputnik shock (§2.1(e)) and raises between reviews
    (§2.1(f)).
  - **The Sputnik shock, measured** (dev-tools `RaceShock`: the smart bot as NASA against the smart
    bot as the Soviets in one game, sharing the goal tally, 200 races). **Racing changes everything
    the solo playouts showed:** without the rule the Soviets land at a median of 146 (96 alone),
    because every First NASA takes pays them only second-time prestige and the reviews snowball.

    | Share | NASA / Soviet median landing | Lands first, NASA : Soviets | Margin, median (quartiles) |
    |---|---|---|---|
    | 0 (off) | 92 / 146 | 150 : 50 | 57 (29-71) |
    | **0.5 (built)** | 88 / 105 | 129 : 70 | 26 (12-49) |
    | 0.75 | 86 / 95 | 128 : 70 | 15 (8-33) |
    | 1.0 | 86 / 90 | 108 : 89 | 14 (7-23) |

    0.5 is "a bit of a boost", as the user asked; the share is the user's call. The rule does nothing
    for a slow start without a rival ahead: raises between reviews are the lever for that.
  - **Raises between reviews, measured** (§2.1(f); `-Drules.raiseShare`). Solo, at share 1: median
    landing 78 / 79 against 87 / 88 without, the slowest quarter 97 / 92 against 135 / 133, games
    stalled on money a fifth as often; **a typical game is about 9 seasons shorter**. Share 0.5 gives
    78 / 81. In the race (with the shock at 0.5): the second agency lands a median of 8 seasons after
    the first (26 without raises), and the side behind on funding at season 32 lands first in 73 of
    200 races. If the shorter games matter, a slightly stingier bracket table would lengthen them
    without bringing back the snowball; the user's call.
  - **Worth knowing for balance, not verdicts:** a failed flight is cheap enough that launching
    earlier is faster at every bar tried, with astronaut losses about flat; a careful player is not
    rewarded for waiting. And an idle employee quits within about ten seasons (from a new hire's 55
    morale), so a player who staffs a building to capacity without the work for them churns people:
    the morale rules may be harsher than a player expects.
- **The game goes on after a winner:** goals, flights and prestige still count.

### 2.10 Difficulty level

**Decided (user, 2026-09-27): none.** One shared difficulty, based on BASPM's **Normal**
values only (`Value1` in its constants files); no difficulty field anywhere. Retune that one
balance sheet if needed rather than adding levels.

### 2.11 Commercial contracts

**The user's idea (2026-10-11), designed with the user** (`rules.Contracts`, `rules.ContractMarket`;
`ui-spec.md` §4.17 and component 27). Aimed at the mid-race lulls, which are mostly a wait for money
(§2.9). Once an agency has shown a capability, clients pay it to fly that capability again: contracts
pay **money, not prestige**, with hardware the agency already has, so they add no R&D wait. Players
compete for them on a shared marketplace. No BASPM precedent; every number is ours, a default and
**unconfirmed** unless a line says otherwise.

- **Unlocking** (user): each program can carry an **unlock set**, the configurations an agency must
  each have flown successfully, chosen per program (rarely all of them; long extended flights are
  usually left out). Completing it unlocks the program's **category as a contract line** for that
  agency, for good. The agency **qualifies** for the line's listings while at least one of its
  unlocked programs in that line is open, so a program's upkeep buys a chance at contracts. The
  unlock set's configuration cards carry a contract icon with a tooltip (`ui-spec.md` component 27).
  Programs with nothing a client would buy have no unlock set. **As authored (unconfirmed):** 25
  programs in six lines. Earth-orbit satellites: every satellite program, its one-season orbital
  flight (the three-season extended flights left out). Lunar probes: every lunar probe, its one
  flight. Space planes: the X-15 and the OKB-MiG E-154 (the test and both research flights;
  contracts fly the first research flight). One-crew capsules: Mercury and Vostok (the uncrewed and
  the crewed orbital flight; contracts fly the crewed one). Two-crew: Gemini (orbital, rendezvous,
  docking) and Voskhod (orbital, spacewalk), contracts flying the orbital flight. Three-crew: Apollo
  in Earth orbit and Soyuz 7K-OK (orbital and docking), the orbital flight. The planetary probes, the
  lunar expeditions and the lunar programs' Earth-orbit tests have none.
- **A contract flies an existing configuration:** each program with an unlock set names the
  configuration its contracts fly, **always one of its unlock set**, so its goals are already
  achieved and a contract can never earn a goal or open a program. Steps, staffing, rockets,
  reliability and the replay are the normal ones. What changes: steps and goals pay **no prestige**;
  the flight raises reliability like any other; it **doesn't count toward the configuration's repeat
  count**; cards and the turn report label it. An agency qualifying through two programs of a line
  picks one when it applies.
- **Which launch is the contract:** while a contract is active, the **first launch of its
  configuration** is the contract flight (no toggle); Assemble says so before the launch. Flying that
  configuration for prestige waits until the contract has flown.
- **Closing a program** is refused while an active contract flies on it (the existing
  `MISSIONS_IN_FLIGHT` refusal). An application whose program was closed before the turn resolves
  comes to nothing and the report says so.
- **Calling a contract flight off** (unconfirmed): a scrub before it flies puts the contract back on
  the ground while its window lasts; cancelling it in flight ends the contract as a failure in the
  record, with neither fee nor the insurer's refund (the cancellation's own refund share applies).
- **Money** (user): the agency pays the launch cost up front as usual. **On success** the client
  pays the **fee**: the launch cost times (1 + the tier's margin), **+60 / +40 / +25 %** for tiers
  1 to 3, plus a **crew premium** for each crew seat of the contract flight, **1,000 / 670 / 420** by
  tier (user, 2026-11-05, *unconfirmed numbers*; `Contracts.CREW_PREMIUMS`): the margin alone made the
  X-15's research flight (launch cost 683) pay a tier-1 profit of 409, the least of any line, while
  risking a pilot; with the premium it pays about 1,400, Mercury 3,560, Gemini 6,690 and Apollo
  10,340. The fee is fixed at the award, from the contract configuration with the cheapest of the
  agency's open rockets that can fly it, or the cheapest of its side's when none of its open ones can;
  a dearer rocket only lowers the profit. **On failure** the insurer pays back the launch cost
  actually paid (break even), and the agency loses the prestige that flight's failure costs anyway. A
  crew lost on a contract flight costs an extra 50 prestige on top of the normal rules.
- **Tiers go by track record** (user, 2026-10-11, over prestige, which would hand the leader the
  money too). An applicant's **score** is the reliability of the weakest component the contract
  flight would use (the agency's hardware at the start of the turn, its most reliable open rocket
  that can fly it) plus its **record in that line**: +3 per contract flown successfully, −6 per
  failed or lapsed one, within ±15. The highest score gets tier 1, the next tier 2, the third tier 3;
  a fourth applicant and later get nothing (user). Ties go to a seeded draw. Scores read
  start-of-turn snapshots (§3.7). **Crew and controllers don't count**: clients judge the hardware,
  so the score differs from the assessment tab's odds on purpose.
- **Applying** (user): **one application per turn**, refused while the agency has an **active
  contract** (awarded and not yet flown to its end), so one contract at a time. Applications are
  **sealed** until the turn resolves, and can be changed or withdrawn until then.
- **The award**, at the resolution: each applicant learns its tier and fee, or that it missed out,
  in the turn report. A listing leaves the board once awarded. Everyone's report gets a news line
  naming the agencies that won it, in alphabetical order: who ranked where stays sealed.
- **A launch window:** an awarded contract must **launch within 3 seasons**. One that doesn't
  **lapses**: it counts as a failure in the record and costs 10 prestige. A scrub before the flight
  refunds the launch cost as usual and keeps the contract while its window lasts.
- **The board** (the user's design, 2026-10-11): settled once a turn, at the resolution, for
  everyone, from its own salted seed.
  - **A listing** is a line, a client with a story, the three tiers' margins, the turn it was
    posted and **its expiry, 3 turns later**. **Only lines at least one agency qualifies for are
    listed** (user); a listing nobody qualifies for any more is withdrawn. Everyone sees the whole
    board, with whether they qualify.
  - **Seating** (the user's queueing idea): every listing has three seats, one per tier. The
    eligible agencies (claimed, no active contract) are seated in turn, **the most constrained
    first** (fewest qualifying lines; ties by seeded draw): each takes a free seat in the fullest
    listing it qualifies for. Only when none has a free seat is a new listing posted, in the line
    **most of the still-unseated agencies qualify for**. Seats only size the board; nobody sees
    them. Packing the narrowest agencies first gives the most overlap, so the most competition.
  - **Extras:** one listing, plus one per four eligible agencies, from the lines they qualify for
    (seeded), added only while the board holds fewer listings than the seating needed plus the
    extras, so expiring listings don't pile up.
- **The Soviet side** has no commercial market, so it is reskinned: its clients are ministries and
  institutes ordering a service (the Ministry of Communications' relays, the Academy of Sciences'
  instruments). Same rules, own texts.
- **Intelligence:** an agency assessment's Programs and plans area also shows the rival's active
  contract and its record per line.
- **Content:** in the pack (hashed), `commercial.json` (in `ContentPackLoader.FILES`; its own file so
  the BASPM converter, which writes `programs.json`, never touches it): per program, the unlock set and
  the contract configuration. The margins and record weights are rules constants (read by help's
  tokens). Clients and their stories by line and side are in each pack's `commercial-clients.json`
  (a pack file too): 90 (user, 2026-11-05, after the early satellite clients came round too soon:
  15 a side for the satellites, 6 a side for every other line), no repeats until a line's have all
  run. A listing names one client per side, so each side reads its own. History: §1.1.
- **Storage:** `market/turn-N.json`, written once at the resolution before the season passes, like
  intelligence, and re-read on a retry: the awards from the turn's applications, the board for the
  coming turn, the clients used. The board is settled before the passes too, since each agency's copy
  is saved in its own state (`PlayerSnapshot.contractBoard`); the client reads it with the state. The
  cost is a one-turn lag: a line unlocked by a flight this turn, or an agency freed by a contract
  ending this turn, is seated at the next resolution. Scores read each agency's state at the start of
  the turn; applications and open programs read it with the turn's actions applied. One edge: a
  rocket program reopened this turn has no open rocket yet at the turn's start, so the applicant
  scores on its record alone; reopen the rocket a turn before applying. Per player: the record per
  line, the active contract with its fee and window, the application, the board; unlocked lines are
  worked out from the flights flown. `TurnRollback` deletes the turns' market files.
- **Measured (2026-10-11, before the crew premium):** `RaceLulls standard ahead+contracts 1.0 0.0`,
  200 Moon races, the market simulated with three rivals as strong as the bot. Per game: 11.5 awards,
  7.3 paid, **2.8 lapsed**, about **6,000 profit** over the race. Gaps of 5 seasons or more fell from
  22 % to 14 % of launches, but the race itself barely moved (NASA slower, the Soviets faster) and
  debt conversions rose. So contracts fill the lulls, as meant, but at these margins don't pay for
  the race. For the play test: higher margins, a fee that scales with the era, or a bot that only
  applies when it can fly the contract soon (§4).

### 2.12 The archive

**The user's idea (2026-10-15)** (`ui-spec.md` §4.19, canvas `Launch Window — Archive`; the pages in `assets/content/archive.json`, their
sources and every figure's check in `assets-raw/archive/CHECKS.md`, images and rights in
`assets-raw/archive/PAGES.md`, assets made by `assets-raw/archive/make_archive_assets.py`). NASA's imagery is public domain.
An archive of pages, one per real mission that sent back something memorable, written like a short
encyclopedia article: the mission's story, a fact box, its pictures and, where there is one, its sound. A
page stays locked until its goal is reached in the game, so flying the mission feels like getting its
pictures back. No BASPM precedent.
- **What unlocks a page:** the goal it names (`lunar-photos` for Luna 3's far side), the first time any
  agency in the game holds it, achieved or credited through a cascade. A page tells the real mission that
  first did it, whatever program earned the goal in the game (the standard pack's Pioneer 4 can earn
  `lunar-photos`; the page is still Luna 3's). Goals pay only for success, so a failed flight never
  delivers the pictures. A finding's page follows its own rule (§2.13).
- **For everyone in the game:** a page unlocked by a rival opens for every player too (user,
  2026-10-15). That tells nothing intelligence keeps secret (§2.9): it needs a success on a goal, and a
  First the player could reach is already in the turn report (§2.1(e)). It does tell a player about a
  First on a goal their agency can't reach (a Soviet player learns of `saturn-flyby`). That's how it was:
  both sides announced their successes and hid their failures.
- **Settled from the goal records:** a page's unlock turn is the earliest turn any player's goal records
  hold its goal. Nothing new is saved, and a late joiner finds the pages unlocked before they joined
  (*proposed*). The server answers when asked (`ArchiveRequest`, with the client's pages' goals, through
  `WinCondition.goalTimes`, cascade credits included): a turn report shows the pages whose unlock turn is
  its own turn. `"devArchiveOpen": true` in `game-meta.json` opens every page, saving nothing.
- **When the race is decided** (user, 2026-10-15): every page nobody has reached by then opens for every
  agency, on the deciding turn (`game-result.json`). Such a page says "opened when the race was decided"
  instead of who flew it, and that turn's report opens with a notice across the top, "The race is decided,
  and the whole archive is open", with the way to the Archive tab. A goal reached later keeps nothing of its
  own.
- **Shown:**
  - **Turn report:** a "New in the archive" card on every player's report of the turn it opens, linking
    to each new page. It says whether the player or a rival flew it, without naming the rival (*proposed*).
  - **Museum:** an Archive tab. Locked pages are greyed out with a hint, "Unlocks with: Pictures of the
    Moon", the goal's name (user, 2026-10-15).
- **The text is our own,** written from the facts (Wikipedia, NASA and others as sources, listed on the
  page), not Wikipedia's text: that keeps one voice and avoids CC BY-SA's share-alike on adapted text. A
  sentence quoted with attribution stays possible. Every figure is checked as for the history notes;
  where the sources disagreed the pages say less.
- **Images:**
  - **Only originals:** NASA's, JPL's, Ames's, Langley's or NSSDC's, credited as their captions say, and
    never a third party's reprocessing. The NASA insignia stays out of the game's own art.
  - **Soviet images:** not public domain by default. Under German law (§72(3) UrhG, 50 years for a photo
    without creative input) the 1959 to 1975 ones are free here; Russia's longer term was to matter only
    once the repo went public, which it did on 2026-10-28. The checks per image are in `PAGES.md`.
  - **Leonov's spacewalk film** is still protected in Russia, so his part is told in words.
  - **Venera 9's** known pictures are Don P. Mitchell's processing; use them only with his permission.
    He says he generally grants it for free, and asking him is the user's call. They aren't in.
- **Sound:** NASA's air-to-ground recordings are public domain (the Genesis reading, "The Eagle has
  landed"). The Pale Blue Dot page carries Carl Sagan's recording of his whole passage, 4:36 (the
  2026-10-13 decision, §5, Backlog).
- **The pages** (user, 2026-10-15, plus the findings' pages of §2.13; 34 in all), with the goal that
  unlocks each mission page:

  | Page | Unlocked by |
  |---|---|
  | Luna 3 | `lunar-photos` |
  | Ranger 7 | `lunar-close-up-photos` |
  | the first spacewalks (Leonov, White) | `spacewalk` |
  | Lunar Orbiter 1 | `lunar-mapping` (a finding) |
  | Apollo 8 | `crewed-lunar-orbit` |
  | Apollo 11 | `crewed-lunar-landing` |
  | Mariner 4 | `mars-photos` |
  | Mariner 9 | `mars-mapping` (a finding) |
  | Viking 1 | `mars-orbiter-and-lander` |
  | Pioneer 10 | `jupiter-photos` |
  | Venera 9 and 13 | `venus-surface-study` (a finding) |
  | Voyager 1's Pale Blue Dot | `saturn-flyby`, the furthest flight in the game; Voyager isn't in either pack |
  | Surveyor 1 | `lunar-soft-landing` |
  | Apollo 15, "driving on the Moon" (Lunokhod 1 was first), with Scott's hammer and feather | `apollo-landing-with-rover-flown` |
  | Mariner 10 | `mercury-mapping` |
  | Pioneer 11 | `saturn-structure-study` (a finding) |
  | Luna 9 | `lunar-surface-photos` |
  | Lunokhod 1 (an LRO frame from NASA's PDS archive and a Soviet photo) | `lunar-rover` |
  | Luna 16 | `lunar-sample-return` |
  | Venera 15 and 16 (their 1983 radar images a medium risk, like Venera 13's) | `venus-radar-mapping` (a finding) |
  | Surveyor 3 | `lunar-soil-analysis` (user, 2026-11-01) |

- **Size:** images at about 1600 px on the long edge as JPEG, 150 to 300 KB each.

### 2.13 Publishing findings (cooperation)

**The user's idea (2026-10-19)** (`rules.Findings`, `FindingPublications`, `PublishFindingAction`;
`ui-spec.md` §4.20, G16). The race and the Intelligence Office (§2.9) are competition. This is the
first cooperation: an agency that has learned something about an environment can publish it, which
makes rivals' flights there safer and pays the publisher prestige. No BASPM precedent. History backs
it: NASA published its results openly, the Soviets announced Sputnik's frequencies so anyone could
track it, and both first satellites were contributions to the International Geophysical Year. No
secrecy bonus for either archetype (as in §2.9). Every point is unconfirmed unless it names the user.
- **A finding is a science goal the pack already has,** mapped to the step kinds it makes safer, in
  each pack's `findings.json`, **a rules file inside the pack hash** (user): 18 findings in the
  standard pack, 2 in the early one (*the mapping awaits the user's review*). A step matches by its
  kind (its id without the numbering and the a/b of a two-craft flight: `rules.Findings.kind`), the
  body its program's category is browsed under (a flight to the Moon is MOON for every step, its Earth
  reentry too), and whether it carries crew. A kind ending in `*` matches every kind it starts
  (`duration*`). `FindingsTest` prints how many steps each kind matches and fails on one that matches
  none.

  | Finding | Makes safer | Steps (standard) |
  |---|---|---|
  | `magnetic-field-study` | crewed Earth orbit, high orbits, trans-lunar injection (the belts) | 125 |
  | `organisms-in-microgravity` | crewed suborbital flight, orbit insertion, reentry, around Earth | 136 |
  | `amphibians-in-microgravity` | crewed long stays (every duration level; the OFO's inner ears) | 89 |
  | `micrometeoroid-study` | crewed spacewalks around Earth | 35 |
  | `lunar-micrometeoroid-study` | crewed lunar orbit, surface activities, spacewalks, Moon flights | 35 |
  | `lunar-bioscience` | crewed lunar flyby, Earth approach and reentry, Moon flights | 41 |
  | `lunar-structure-survey` | lunar orbit insertion and lunar orbit (mascons, found from Lunar Orbiter's tracking, 1968) | 42 |
  | `lunar-soil-study`, `lunar-mapping` | powered descent and landing on the Moon | 17 each |
  | `venus-structure-study` | entry at Venus (Mariner 5's occultation, 1967) | 5 |
  | `venus-atmosphere-study` | lander and probe descent at Venus | 5 |
  | `venus-surface-study` | landing at Venus | 3 |
  | `venus-radar-mapping` | landing at Venus | 2 |
  | `mars-surface-and-atmosphere-study` | entry, lander descent and de-orbit at Mars | 5 |
  | `mars-mapping` | landing at Mars | 2 |
  | `mars-soil-study` | surface work at Mars | 4 |
  | `solar-wind-measurements` | interplanetary cruise | 46 |
  | `saturn-structure-study` | approach and flyby at Saturn (Pioneer 11's ring-plane crossing) | 2 |

  Left out: photo goals, and the goals whose step makes nothing later safer (Mercury's, Jupiter's;
  Pioneer 10's radiation finding has no goal of its own). `organisms-in-microgravity` and
  `magnetic-field-study` reach most crewed Earth flights, and Korabl-Sputnik 2 earns both: the smart bot
  never flies it, so their reach is still to measure with flights that earn them. Every figure the table
  names is to be checked before it goes on a page. What the findings answered, for the pages: Explorer
  1 found the Van Allen belts (1958); Explorer 16 and the Pegasus satellites measured the micrometeoroid
  hazard for Apollo; Korabl-Sputnik 2's dogs came home alive (1960); Zond 5 took tortoises around the
  Moon and back (1968); Luna 9 and Surveyor 1 (1966) showed the surface holds a lander, against Gold's
  deep dust, and Lunar Orbiter mapped the Apollo sites; Venera 4 fell silent some 25 km up (1967; heat
  and pressure or its battery, NSSDC), and Venera 7 was built for 150 to 180 atm; Mariner 4 measured
  4–7 mbar at Mars (1965), far thinner than assumed.
- **A known environment helps; an unknown one never hurts** (user): BASPM's odds stay the baseline. A
  matching step gets **+4 base-chance points** (`Findings.BONUS_POINTS`, user, 2026-10-21) for an
  agency that knows the finding, after the crew blend, in the season pass
  (`MissionProgress.resolveStep`) and the client's odds alike (`MissionAssessment`,
  `StepOdds.findings`). **Bonuses from different findings stack** (user; rarely more than one applies).
  A goal the flight earns counts for its later steps, as goal modifiers do. `-Drules.findingBonus=0`
  turns it off in `ContentPackPlayouts` and `RaceShock`.
- **Known** means held (achieved or credited, user: own science counts, so science flights are worth
  flying in the lulls) or public. `PlayerSnapshot.publicFindings` keeps every published finding with
  its turn, the same in every agency. A late joiner starts with the findings already public.
- **Publishing is an event the player takes part in, not a standing policy** (user). The finding shows
  up in the turn report of the season its goal was earned, with a link to its archive page and a
  **Publish** button; a confirmation dialog shows the prestige it would pay. Doing nothing keeps the
  finding classified; no answer is ever required. **Publishing later** (user): the archive page keeps
  the button until anyone has published it (a second publication would pay nothing), so a player can
  hold a finding back, at the risk that rivals earn it first. Asking is `PublishFindingAction`,
  withdrawn by the same action; an ask is settled or dropped at the resolution.
- **The payment, capped** (user): at most **33 % of the goal's first-time prestige** (user,
  2026-10-21), scaled by the share of rivals it helps, `cap × helped / (players − 1)`, a rival being
  helped if it doesn't know the finding at the start of the resolution (one that earns it at the same
  resolution still counts). The dialog shows the amount and "helps 6 of 9" both (user: the amount gives
  the count away anyway). The preview is exact unless another agency publishes the same finding at the
  same resolution, when they split it.
- **Settled before the turn's flights**, like the market (`FindingPublications.settle`, kept in
  `publications/turn-N.json`); rivals learn it in the same pass (a `FINDINGS` phase before the budget,
  so a review that season counts the prestige), so it helps their flights from the next turn on.
- **Archive pages** (§2.12): **every finding has one** (user: "knowledge is cool"), and **no history
  notes for findings** (user): what a note would say goes on the page. **A finding's page opens only for
  the agencies that know it** (user): its discoverers, everyone once it's published, and everyone once
  the race is decided. Pages written for findings: Explorer 1 and the belts, Belka and Strelka, Pegasus,
  Pioneers 6 to 9, Lunar Orbiter's meteoroid cells, the mascons, `ranger-surface` (what Rangers 7 to 9
  let scientists infer about the soil, for `lunar-soil-study`; user, 2026-11-01), Venera 4 and Mariner
  5, Zond 5's tortoises, Mariners 6 and 7, the Orbiting Frog Otolith, Viking's soil and the Pioneer
  Venus Multiprobe; five mission pages are findings' too (Lunar Orbiter 1, Mariner 9, Venera's surface,
  Venera 15 and 16, Pioneer 11). Riskier pictures: Zond 5's two halves of NASA History's composites
  (credited RKK Energiya, first publication unverified, medium), Venera 4's capsule, Korabl-Sputnik 2's
  one TV frame, a Pioneer Venus painting credited to Paul Hudson. Three are figures from NASA reports'
  PDFs (`make_archive_assets.py`'s `PDF_PAGES`, `BLANKS`). `viking-1` gives no year for the Mutch
  Memorial Station (NASA Science says January 1982, other sources 7 January 1981).
- **Measured (2026-10-21):** `ContentPackPlayouts standard smart`, 200 games, bonus on and off: no
  effect beyond noise (average completion 84.6 against 84.8). The smart bot's route earns only two
  findings, `magnetic-field-study` (Explorer 1, NASA only) and `lunar-soil-study` (the lunar impacts); a
  player who flies the science flights in the lulls gets more. `RaceShock standard
  -Drace.publish=never,always`: about 2 publications a game, 255 prestige in all; the side behind on
  funding after season 32 lands first 76 times against 64, a mild pull toward a closer race, part of it
  perhaps noise. Side flights and the lulls (`RaceLulls`) are still to measure.

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
  (the server may have acted); read-only calls may retry once, action submits never. The one
  server push is `TurnResolvedNotice` (§3.6).
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
  turn. Only future turns can be blacked out.
- **Editing it** (the user's idea, 2026-11-13): the admin edits every field from the client, in
  Settings › Server settings or from Join game (`ui-spec.md` §4.22, G21), with an admin password kept in the
  file as `adminPassword`. The server makes one at startup when it's missing and prints it at every start;
  every admin request carries it. Hand edits still work (a save over one is refused as stale). The schedule,
  breaks, dev mode and new bots apply at once; a changed pack rebuilds the game in place. The pack and seed
  lock once the first agency exists or a turn has resolved, the goals once a turn has resolved, a bot once
  its agency exists. (The lock points, the lockout, the hidden seed and the password's shape are
  unconfirmed, §4.)
- **Starting a new game** (the user's idea, 2026-11-13; `ui-spec.md` §4.22, G21): from the same dialog, behind a
  typed "NEW GAME". What `reset_dev_data.cmd` does, from the client and with the server running: the old game is
  zipped to `data/old-games/`, everything else but the accounts and the settings is deleted, a new random seed is
  saved, the game is rebuilt in place, and everyone online goes back to Connect to found a new agency.
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
  resolution still comes) and allows `DevClient`'s funds cheat. Set in Server settings, it applies at once.
- **Players online while a turn resolves** (the user's question, 2026-10-09). Some players
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
    be taken for the answer to a request. It also brings the Firsts' front pages (§5, Backlog).

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
  market/turn-<N>.json         the contract market (§2.11)
  publications/turn-<N>.json   published findings (§2.13)
  backups/                     zips of the data root (below)
  ```
  Beside them, `frontpages.json`, `reactions/turn-<N>.json` and `losses/turn-<N>.json` (the front
  pages, statements and lost crews, `ui-spec.md` §4.18). A filler bot's folder also holds
  `bot.json`, the last turn it played (§5, Backlog).
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
  without (bump `TurnReport.VERSION` when a field's meaning changes). Saved state is read
  strictly, and `SavedFormatTest` pins every saved format with a fixture game; a breaking change
  after the first live game needs a step in `SnapshotMigrations` (`CLAUDE.md`, "Network and saved
  formats").
- **Museum records** (additive): `PlayerSnapshot.memorial` (`MemorialEntry`) and
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
  `core`'s `LoginNames`), and a salted password hash: PBKDF2-HMAC-SHA-256, 210,000 rounds, since
  2026-10-25 (review.md 2.3; an account on the earlier single SHA-256 round is rehashed when it logs in). **No display name** (user, 2026-10-04): players know each other by their per-game
  Director name. Everything about a game lives in that game's state.
- **Storage:** one JSON file, an in-memory map flushed on a background cadence with rolling
  backups and a final flush on shutdown.
- **Login:** a known login needs the right password. An unknown one is refused
  (`HandshakeResponse.NO_SUCH_ACCOUNT`), and the Connect screen offers to create it
  (`CreateAccountRequest`, review.md 2.3, 2026-10-25): until then a typo silently made a second, empty
  account. A new account is written to disk at once. (`DevClient` always sends a create, which logs in
  when the account exists and the password is its own.)
- **Player data is keyed by account id,** never by login (case-insensitive file systems, and
  logins can be renamed).
- **Joining is open for the whole game, with no player cap** (user, 2026-09-27): late joiners
  happen, and accept the disadvantage. A login without a slot in this game claims one: an
  archetype (non-exclusive), an agency name and a **Director** name, both unique in the game
  (case-insensitive), fixed for the game. A late joiner's own season count starts at 1; they
  resolve on the shared clock.
- **Host actions:** the game's settings, blackouts included, are edited in the client's Server
  settings behind the admin password (§3.6); announcing and the start date stay
  outside the game. An allowlist mode is a possible later host option.
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
  second argument plays the bot that works ahead (§2.9), `side` the one that also flies side
  missions, `smart` the one that plays like a careful player (§2.9, "A smarter bot"); it also opens
  Headquarters and the VAB as the plan needs, and closes programs and rockets it no longer flies.
  The bots wait only for the components a configuration's steps fly. Also `RaceShock` (two smart
  bots in one game), `RaceLulls`, `IntelligencePlayouts`, `StateSizes` (§2.9).
- **The economy** (the user's question, 2026-11-13, after a play test drowning in money): `EconomyCeiling`
  prices, from the rules alone, the most an agency can spend each season to keep running against what each
  review pays; `-Dplayout.economy=true` on `ContentPackPlayouts` and `-Drace.economy=true` on `RaceShock` keep a
  per-season ledger (`EconomyLedger`: funding, running costs, everything else spent, cash banked), by review,
  before and after the race is won.
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
- **Money stops mattering from the second review** (2026-11-13, 100 smart-bot games alone and 200 races of two):
  while racing, a strong agency leaves 17–32 % of a review's funding unspent and ends 76–96 % of its seasons with
  two or more seasons' funding banked (3–6 seasons, median); a rival barely changes it. After the race, two
  thirds goes unspent. The rules' ceiling agrees: buildings and staff maxed with 16 median programs cost about
  21,000 (NASA) to 23,000 (Soviets) a season, 4 programs (a finished race, like the play test) about 15,000,
  against a fourth review paying up to 49,000 and later ones 52,500–65,000; only every dearest program and
  rocket open at top salaries (44,000–49,000) reaches it. A reasonable player (staff for the biggest mission
  plus spares: 7 astronauts, 15 controllers, 16 engineers; buildings only as big as that needs; intelligence
  level 2; 4 to 7 programs, 3 to 4 rockets) runs at about 8,000–10,700, covered from the third review at
  modest prestige. Lowering the ranges' tops alone (`-Drules.budget`, `BudgetTrial`) starves the middle as
  well, since a review pays along a straight line from its bottom to its top: tops of 17,000 / 27,000 /
  36,000 / 38,000+ from review 2 already left a quarter of two-bot races without a landing by season 240,
  and lower ones spiral (debts paid in prestige lower the next review). The smart bot sits mid-range
  (about 26,000 at review 3, 31,000 at review 4, against tops of 35,000 and 49,000), so the surplus is
  worst for a dominant player at the top. Flattening each line from review 2 on above a knee instead
  (`-Drules.flatten=<knee>,<slope>`, `BudgetProgress.setFlattening`) keeps the middle and trims the top. Two
  smart bots racing, 200 games each:

  | Funding | Top, reviews 2 / 3 / 4 | Left at reviews 2–4 | Flush seasons | First landing, quartiles (mean) | Second landing, mean |
  |---|---|---|---|---|---|
  | BASPM's straight line | 21,000 / 35,000 / 49,000 | 7–32 % | 76–96 % | 74 / 78 / 83 (78.8) | 91.1 |
  | Knee 0.6, slope 0.25 | 16,080 / 26,150 / 36,700 | −3–19 % | 36–71 % | 75 / 79 / 85 (80.5) | 96.3 |
  | Knee 0.5, slope 0.3 (adopted) | 15,260 / 24,675 / 34,650 | −4–14 % | 19–61 % | 77 / 80 / 85 (82.1) | 99.9 |
  | Knee 0.4, slope 0.1 | 12,144 / 19,070 / 26,860 | −3–6 % | 3–40 % | 84 / 89 / 96 (92.9) | 116.9 |

  Adopted: knee 0.5, slope 0.3 (user, 2026-11-13; §2.1(d)). `-Drules.flatten=1,1` plays BASPM's line
  again for comparison.
- **Pay expectations, measured** (2026-11-13; §2.2(c); `RaceShock -Drace.economy=true -Drules.pay=…`, the
  people ledger `PeopleLedger`; `-Dbot.pay=false` plays a director who never raises anyone; the ceiling at 3 points
  a percent, bots raising anyone 5 % under). NASA's side, while racing, reviews 2 to 4:

  | | Morale (mean) | 80+ / below 50 | Resignations a season | Salary bill at review 4 | First landing, quartiles (mean) |
  |---|---|---|---|---|---|
  | Today's rules | 94 | 86–87 % / 0 % | 0.01–0.02 | 2,290 | 77 / 80 / 85 (82.1) |
  | All on, bots pay | 87–94 | 73–89 % / 0–2 % | 0.01–0.05 | 3,780 | 76 / 81 / 84 (83.4) |
  | No morale upside, bots pay | 87–93 | 73–86 % / 1–3 % | 0.03–0.09 | 3,590 | 78 / 82 / 89 (86.6) |
  | All on, nobody ever raised | 62–69 | 14–22 % / 14–26 % | 0.8–1.0 | 2,205 | 93 / 101 / 113 (105.7) |

  Paying costs about 65 % more in salaries by review 4 and money runs tighter (flush seasons 7–31 %, from
  19–61 %); the morale upside about pays it back in the race (one season slower on average with it, four
  without). Neglect loses about a person a season from the second review and costs the race over 20 seasons.
  Expectation rises per agency over a race: about 225 for Firsts (crew and ground, ~15 people each), 190 for
  flights, 265 for R&D stints, 15 for ambitions, plus the reviews; the paying bots raise someone in most seasons,
  each department 4 to 9 times a review (they answer every 5 % gap; a player can leave gaps under 7 %, which still
  allow 80+). Watch: Firsts lift whole mission teams, so a crew flying many may end up expecting far more.
- **Someone sidelined loses no idle morale** (the user's decision, 2026-10-22): an employee made unavailable by
  a random event or an R&D decision's secondment can't be given work, so their season's own morale holds
  (`PersonnelLifecycle`); other morale changes (events, salary, ambitions) still apply.

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
and proposed) are in `git show 6fc7af5:design.md`; the outcomes are in each screen's `ui-spec.md`
section.

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

- **Lulls in the Moon race** (§2.9). The length is accepted and the race stays (user, 2026-10-09);
  the question is how to keep players busy between flights. Built for it: the R&D decisions (§5,
  Backlog), commercial contracts (§2.11), science findings worth flying (§2.13). The play test decides
  whether more is needed.
- **Commercial contracts** (§2.11): the first measurement says they fill the lulls but don't speed
  the race, and a quarter of awards lapse: the margins, or a fee scaled to the era, are the first
  thing to look at.
- **Declared goals for the colleagues' game:** the standard pack's race to a crewed Moon landing is the
  default (user, 2026-10-09, §2.9); whether the colleagues' game plays that or a shorter early-era race
  (user, 2026-10-08) is open.
- **NASA/Soviet prestige balance:** BASPM's Soviet programs pay a little more each (about 4 %)
  but NASA has nine more programs, about 34 % more prestige in total. Decided (user,
  2026-10-01) not to close the gap up front; revisit with play-test and harness data.
- **A game-host role and UI:** Server settings (§3.6, `ui-spec.md` §4.22) covers `game-meta.json`;
  announcing games, allowlists and content fixes in a running game are still outside the game.
  Unconfirmed in it: the lock points (pack and seed at the first agency or the first resolution, goals at the
  first resolution), hiding the seed once locked, 5 wrong tries in 10 minutes locking an address out for
  10 (behind NAT, possibly everyone), a 16-character generated password printed at every start, the client
  never saving it, and the early-era pack's one-line description.
- **Interaction:** intelligence, poaching, statements and condolences, published findings and the
  contract market exist (§2.9); sabotage is ruled out. Mail between players and cooperative modes are
  later ideas.
- **Can SET staff reach retirement in a realistic game?** At 65 and today's game lengths, few
  will; it matters only if games run decades.
- **Rights, since the repo went public (2026-10-28):** the Sagan recording was to be revisited before
  that (§5, Backlog), and the Soviet images' Russian term mattered only once public (§2.12). Both are
  the user's call.
- **Balance points the bots raised** (§2.9): cheap failures reward launching early; idle staff quit
  fast; the shorter games after raises between reviews (a stingier bracket table is the lever); the
  filler bots' money piling up (§5, Backlog).
- **Money stops mattering from the second review** (§3.10's findings, 2026-11-13): answered for now by
  diminishing returns at the top of each review (§2.1(d)); a play test decides whether it's enough. Next lever
  (user): salaries that need raising from time to time, or morale slips, so the department screen gets used.
  Measure a change with `EconomyCeiling` and the playouts' money ledger.

**Settled, for the record:** a player who doesn't log in before a resolution keeps their
default: a scheduled mission launches and training continues. Scrubbing and pulling out are
ordinary actions available until the resolution (user; BASPM's second "launch now?" prompt is
skipped). R&D assignments are standing orders (`baspm.md` §2.1). Tiger Teams became the Expert
trait (§2.2(c)).

**Unconfirmed numbers awaiting the user's review** (each also flagged in its section):
- Answer to a rival's First: the 50 % share and counting once (`RivalFirsts.SHARE`, §2.1(e)).
- Raises between reviews: share 1 (`BudgetProgress.RAISE_SHARE`, §2.1(f)).
- Diminishing returns at the top: the knee halfway, 30 % of the slope above it, from review 2
  (`BudgetProgress.KNEE_SHARE`, `TOP_SLOPE`, §2.1(d)).
- Standing charts: 40 dots, a window of 120 (`server.StandingCache`, §2.1(g)).
- Skill growth cut by 30 % for training, flying and R&D (§2.2(c)); a flight's unused skills at a third
  (`SkillGrowth.FLIGHT_UNUSED_SKILL_SHARE`, §2.5(g)).
- Personal skill ceilings, `100 − 0.5 × points above 75` (`SkillGrowth.ceiling`, §2.2(c)).
- Rocket-plane wings: +4 on capsule steps (`rules.RocketPlaneWings`, §2.2(c)).
- People, phases 1 to 3: trait split 20 / 50 / 30 %, reveal points 2 to 4, affinity weights, team-trait
  and personal-trait effects, bland hints 1 in 4 (`Personality`, `Chemistry`, `TraitEffects`, §2.2(d)).
- Relationships: one friendship and one clash a person, the 35 % roll, ±4 % output each
  (`Personality.RELATIONSHIPS_OF_A_KIND`, `Chemistry.RELATIONSHIP_OUTPUT`, §2.2(d)).
- An engineer's cap ambition: `Careers.CAP_SLACK` 2, `CAP_WINDOW_MAX` 20 (§2.2(d)).
- Poaching: tenure and cooldown 4 seasons, raises +25 / +50 / +100 %, acceptance odds, the default match
  +25 %, the match ceiling ×3 (`rules.Poaching`, `Poaching.MATCH_CEILING_FACTOR`, §2.2(d)).
- Nova: 68 t to the Moon, 15,000 to create, 1,500 upkeep, 91 % R&D ceiling, its heritage (§2.3(e)).
- The E-154's ids and the X-15's numbers for the Soviet side (§1.2).
- Intelligence: level costs 0 / 150 / 350 / 700, passives 20 % / 25 % / 40 %, odds 85 % − 15 a level,
  catch 10 % + 10 a level and its 20 prestige, range half-widths (`rules.Intelligence`, §2.9).
- Intelligence follow-ups: 2 turns, 3 points a look (`Intelligence.followUpHalfWidth`, §2.9).
- Foreign hardware: 25 / 50 / 80 % a season, and the roles and counterparts
  (`Intelligence.HARDWARE_PERCENT`, `foreign-hardware.json`, §2.9).
- Failed attempts kept per rival: 24 (`Intelligence.FAILURES_KEPT`, §2.9).
- Commercial contracts: margins +60 / +40 / +25 %, the record's weights (+3, −6, ±15), the 3-season launch
  window, the 3-turn expiry, the extras, the lapse's 10 prestige and a lost crew's 50, the unlock sets,
  calling a flight off (`rules.Contracts`, §2.11).
- Contract crew premiums: 1,000 / 670 / 420 a seat (`Contracts.CREW_PREMIUMS`, §2.11).
- Publishing findings: the mapping of 18 findings to steps (`findings.json`, §2.13); +4 a finding
  (`Findings.BONUS_POINTS`) and the 33 % cap are the user's.
- The archive: a late joiner finds earlier pages open; the report's card names no rival (*proposed*,
  §2.12).
- The hands-on decision: every number, and the bonus too easy at these numbers (§5, Backlog).
- Filler bots: every number (`bot.FillerBot`, §5, Backlog).
- Front pages and the world reacts: every choice; late joiners can't answer, no page when nobody
  spoke, follow-ups first (§5, Backlog).
- Explainers' triggers and the early-era concept audit (§1.1).
- Backups: each zip holds the whole history (§3.7).

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
  `DevClient`.
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
  resolvers in `rules`, unit-tested and driven by the bot harness (§3.10).
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

  Programs can be **closed** as in BASPM (refused while a mission of it flies; its components go
  inactive, keep their reliability, pay no upkeep and release their researchers; reopening
  doesn't charge creation cost again).

  **The early-era pack** (`assets/content/early-era/`): 2 categories, 6 programs (Soviet: Sputnik 1,
  Sputnik 2, Korabl-Sputnik 2, Vostok; NASA: Explorer 1, Mercury), 10 components (five payloads;
  rockets R-7 Sputnik, R-7 Vostok, Jupiter-C, Redstone, Atlas), 19 configurations, 21 goals; for the
  tests and short scenarios. `EarlyEraPackTest` checks it loads, every configuration has a liftable
  rocket of its agency at VAB level 1, staffing fits a reachable building level, and both agencies
  can reach the race goals.

  **The standard pack** (`assets/content/standard`, converter scope `full`, 2026-10-08): all of
  BASPM's tree, 13 categories (with their `body`), 55 programs, 85 components as converted (61
  payloads, 24 rockets; Nova added since, §2.3(e)), 186 configurations (189 less the E-154's two and Apollo's Agena
  docking) and all 207 goals, including umbrella goals that only cascades credit. Our ids are in
  `full_maps.py` (confirmed, user, 2026-10-08). **The server's default since 2026-10-09** (user;
  `GameMeta.DEFAULT_CONTENT_PACK`; tests still pin `early-era`). Its text is our own (names,
  descriptions, long descriptions); **history notes** cover every program and component and the
  configurations that match a real flight (Gemini 1 to 12, Voskhod, Soyuz 1 to 9, Zond 7, Apollo 4
  to 15, Apollo-Soyuz, Skylab, the X-15, Venera 13), every figure checked; configurations with no
  real counterpart (the lunar Gemini plans, the LK-700, the E-154, the extended missions) have none.
  The two Gemini EOR programs share one note. A joint flight's craft-specific steps take their shared
  step's text, named for the craft (`ContentStrings.craftStep`: `gemini-a-`, `atv-` for the target,
  `transtage-`, `-a`/`-b`). Its art is converted (`make_content_art.py`) and its 207 goal patches
  generated (`assets-raw/patches/patches_full.py`; the model can't spell "CIRCUMLUNAR", so those
  patches say "GEMINI"). **No animal passengers on the new flights:** Zond 5's tortoises, the
  Biosatellite and the OFO frogs get no memorial and no honours (user, 2026-10-08). In this scope
  anything that would be dropped fails the run, and the run checks every launch has a rocket of its
  agency that lifts it. BASPM's `HistoricalRocketUsedID` only picks the clip. `StandardPackTest`
  checks the tree: every launch flies on a rocket of its agency at some VAB level, staffing fits,
  every goal can be earned, and the race goals are open to both agencies. Engine additions that came
  with it (`FullTreeLaunchTest`):
  - **`ownLift` on a payload** (`MissionLaunch.requiredLift`): the Agena, the Gemini Transtage, the
    S-IVB tug and Apollo's CSM carry part of their own lift, which BASPM subtracts from what the
    rocket must lift (`ComputeExtraBoostProvidedByPayload`).
  - **`AIR_DROP` launches:** the X-15's and the MiG E-154's configurations fly with no rocket
    (BASPM's `LaunchType` `NA`). The launch keeps its place in the rocket list with `""`, costs no
    rocket, and no step may name its rocket slot; the planning screens say a carrier aircraft drops
    the craft.
  - **`requiredGoalIds` on a configuration:** a hard gate until the goal is achieved or cancelled
    (BASPM's `StrictGoalDependency`; the X-15's and E-154's research flights, the progression gates
    of §2.3(d)). A launch refuses with `GOAL_NOT_REACHED`; the card shows "Locked" and its steps
    screen names the goal and disables "Plan this mission".
  - **`transferPercent` as a double:** many reliability transfers are .5 values
    (`client.view.Rockets` prints it, so format it there).

  **Mapping rules a regenerated pack must follow** (`baspm_pack.py`, outside the repo, `CLAUDE.md`;
  the early scope must keep reproducing the committed pack):
  - Ids are our own, hand-mapped kebab-case; values are BASPM's.
  - A step's `PrestigeGrantedOnSuccess` is a percentage share → `prestigeSharePercent`; its
    failure value is a percentage of the first-time total →
    `failurePrestigeDeduction = round(pct × firstTime / 100)`.
  - Staffing layouts are emitted one to one: crew slot titles, control slots (title, kind,
    skill), slot-indexed controller involvements, per-slot crew weights. `ROCKET_N` → `@rocket-N`.
    Controller slots map to skills by BASPM's speciality index, not the slot name.
  - A configuration's `cost` is its payloads' unit cost times the average step `DifficultyFactor`
    over all its seasons, rounded half to even, as in BASPM (`MissionsConfigurationsManager`; a
    missing factor is 1; only Sputnik 1's extended mission has factors).
  - Goal modifiers and achieved goals that reference goals outside the pack are dropped;
    reliability transfers keep only sources in the pack; **discounts whose source program
    belongs to the other agency are dropped** (BASPM's one such case only works for GSA).
  - A category's required goals are copied onto each of its programs. Animal passengers, and the
    animals honoured for coming home (`cameHome`), come from the converter's `ANIMAL_PASSENGERS`
    map, not from BASPM's data.
  - A goal modifier BASPM repeats within one list is merged into one with the summed amount.
  - **Goal boosts stay** (user, 2026-10-08). BASPM's loader never reads `GoalBoost` elements, so in
    the shipped game only the penalties apply (`baspm.md` §11.3). We apply the boosts the data
    declares (2 in the early-era pack, 99 in the standard pack, +1 or +2 each): they reward related
    work, such as Pegasus and Biosatellite for Mercury. The plan's components card lists every
    boost, including those not yet reached, as "(+1)"; Mission Control names the active ones.
  - **Every step with the crew in it risks the crew** (user, 2026-10-20, both packs): BASPM's
    `CrewCanPerish` flag is patchy (it left out 282 crewed steps in the standard pack, and set it on
    uncrewed flights), so the converter ignores it: a step risks the crew exactly when the crew has
    a part in it. History backs the odd-looking ones: the Apollo 1 fire was a test on the pad,
    Liberty Bell 7 sank after splashdown, and Soyuz 5's modules failed to separate.
  - **Steps typed by BASPM's own step names** (user, 2026-10-20): where BASPM's step TypeIDs and its
    English step names disagree, the names decide. The converter reads the names (`step_names`,
    only to compare, nothing copied) and retypes the steps its `RETYPED` table lists, per program:
    Gemini EOR's Moon flights (which otherwise never left Earth orbit), the 7K-L3's and LK-700's,
    lunar-orbit Gemini's, the 7K-L1's, Soyuz 7K-OK's (insertions, way home, duration levels,
    docking, egress and ingress), Apollo's Earth-orbit flights (including Apollo 5's
    `lem-descent-stage-test` and `fire-in-the-hole`), and the landings' surface steps (Apollo's
    `rover-trip-i`, `rover-trip-ii`, `eva-iii`; the 7K-L3's `lunar-landing`, `eva-i`, `eva-ii`,
    `lunar-liftoff`). The 7K-L3's crewed orbit flight's second lunar orbit is its
    `blok-d-separation`. Left as BASPM has them: the 7K-L3 orbit's two `lunar-orbit` steps (the
    second an inclination change), the LK-700 landing's three `lunar-surface-activities`, smaller
    name mismatches that change no step's meaning, and the 7K-L3's lunar-orbit lander test (its
    deployment typed `lk-test`, its engine test `undocking`).
  - **A flyby passes the Moon in one step** (user, 2026-10-20): BASPM's flybys have a flyby and a
    slingshot step, one more chance to fail for one event; the converter drops the slingshot, its
    prestige share to the flyby (Apollo's too). The free-return concept chip names these flybys.
  - **The retrofire comes before the retro pack's jettison** (user, 2026-10-16): BASPM has them the
    other way round on 20 standard configurations; the converter swaps the pair, each step keeping
    its own data and its number in the order.
  - **Gemini EOR comes home Gemini's way** (user, 2026-10-16): the equipment section first, so the
    modules separation moves in front of the retrofire. Four of its configurations have no
    retrofire step in BASPM's data; left as it is.
  - **Circumlunar Gemini's Earth-orbit tests** (user, 2026-10-18): BASPM names the Transtage's burn
    a trans-lunar injection, but it only stretches the orbit into a long ellipse (the heat-shield
    test, as NASA did on Apollo 4 and 6): renamed `high-orbit-injection` for that program.
  - **The E-154 is rebuilt from the X-15** (`e154_from_x15`, §1.2); **Nova** is added
    (`nova_rocket`), the lunar-orbit craft's heritage set (`capsule_heritage`), Gemini EOR's discount
    pointed at its own tests (`discount_fixes`) and Apollo's Agena docking dropped
    (`dropped_configurations`) (§2.3(e)); the progression gates added (`progression_gates`,
    §2.3(d)).
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
  with the goal tally built from every player's start-of-turn snapshot.
  - **Open:** the fixture harness still sets some fields directly instead of going through
    actions; there's no multi-player harness beyond `RaceShock` (same-turn goal pricing is
    unit-tested only).
  - **Dispatch notes:** call `HiringPool.hire` with `catalog.buildings()`, and launch through
    `MissionLaunch.launch(snapshot, LaunchRequest, catalog)`.

### M5 — Playable client (libGDX): in play testing

Screens are specified in `ui-spec.md` (server gaps in its §6), mocked up in Claude Design and built
in libGDX (workflow in `CLAUDE.md`). Every hub building has its screen, as do the turn report,
replay, Museum, Settings and game result; each screen's choices are in its `ui-spec.md`
section. The game is feature complete and in play testing (dev mode; release builds as
in M6).

**Screen-flow decisions (user, 2026-10-03, `ui-spec.md` §3):** BASPM's buildings regrouped: the
VAB owns rockets (open, manage, Rocket R&D), Headquarters owns payload programs and all mission
planning, the SET Centre owns payload R&D, Mission Control watches scheduled and active missions.
Personnel buildings open on a personnel overview, with Hire and Manage department as
sub-screens. Assembly and launch are one screen (slots filled by hand or auto-assign). Programs
are browsed by celestial body (`ProgramCategory.body`, defaulting to Earth). Configurations are
viewable before their program opens, read-only. The mission assessment is computed in the
client from `core` (`MissionAssessment`, exact, not simulated).

**Content packs** (decided 2026-10-03):
- **Frozen per game:** at creation the server copies its pack into
  `<dataRoot>/content/<packId>/` (`FrozenContentPack`, staged and renamed) and only loads it from
  there. `ContentPackHash` (SHA-256 over `ContentPackLoader.FILES`, line endings and BOM
  ignored) identifies it; the game info sends id and hash. The R&D decisions, the PR press
  stories and the commercial clients are pack files too (review.md 2.2): their effects and draws
  change the rules, so a server update must not change them under a running game. They are in both
  packs, identical (`RulesContentTest`); the rules read them through `ContentCatalog.rules()`, the
  client's screens through the copies its session installs. Still outside: the building table, the
  budget schedule and the random-event odds, compiled in (review.md 2.9). Freezing keeps the open
  turn's actions, in-flight state and every player's numbers consistent.
- **When the client's pack differs** it warns and uses its own copy. The download (G1) is
  deferred (user, 2026-10-06): content changes ship as a new server version, and the login
  version check makes every client update.
- **Growth:** one main pack grows (`standard`). A scenario is a pack plus a declared goal list.
- **Fixes in a running game:** code fixes ship with a server update between resolutions (past
  turns are stored, not recomputed). Content fixes are an explicit host step between
  resolutions (not built: replace the frozen copy by hand), refused unless every referenced id
  still exists, in-flight missions keep their seasons and slots, and the open turn's actions still
  validate. Numbers may change; structure in use may not.
- **Display text** (G4): `strings.json` names every id, with descriptions, long descriptions and
  history notes. It is deliberately **not** part of the frozen, hashed pack, so text fixes reach
  running games; `ContentStringsTest` checks coverage, lengths and characters. Configuration names
  are relative to their program ("Orbital flight"); `ContentNames.of` gives the full name. Steps
  are keyed by step type; a configuration can override single steps.

**Structure:**
- **Client:** `ClientSession` (credentials, state, game info, pack, plan draft),
  `ServerGateway`/`NetworkGateway`, `SessionScreen` → `ChromeScreen` (toast, dialogs,
  submit-then-rebuild, chrome), screen logic in `client.view` (tested), widgets in `client.ui`.
- **Game info** (G1): `GameInfoRequest`/`Response` (pack id and hash, declared goals, open turn
  and date, next resolution and the server's clock, the result with winners' agencies and
  directors, dev mode, the budget schedule).
- **Dev mode:** `ResolveTurnNowRequest` resolves the open turn at once (refused unless
  `devMode`), without touching the schedule.
- **Personnel:** portraits and matching names (§2.2(c)); 128 px greyscale portraits in
  `assets/ui/portraits/`, no atlas (few on screen at once).
- **First play-test feedback** (2026-10-05): the 50 Hz server tick (§3.3), a waiting chip and
  hover tooltips (`ui-spec.md` components 16 and 17; text in `client.view.Tips`), BASPM's basic
  training, R&D auto-assign with a projected-gain bar (`rules.ResearchPlanning`), building markers
  on the hub.

### Backlog

What began here as ideas after M5's screens and is now built, briefly, with where its rules live, then
what is still open. The full build history of each is in `git show 5c95135b:design.md`.

**Help** (the user's idea, 2026-10-08, `ui-spec.md` component 23): a help button in every header and
the hub strip, and F1, open the rules by topic over the current screen, filtered to that screen's
topics; typing searches them all. 34 topics, every number a token (`HelpValues`); a changed rule needs
its topic's text changed in the same session. The public wiki mirrors it (`CLAUDE.md`). Open: put the
`help` and `search` glyphs on the UI Asset Sheet (their SVGs are in `assets-raw/svg/ui/`).

**Advisor** (the user's idea, 2026-10-08, `ui-spec.md` component 24): a card on the hub with a new
agency's first steps, each ticked off from the state, with a link to the building and one to the
item's help topics, then the stages of the guided progression (§2.3(d)); it can be minimized, not
hidden. One-off tips: idle staff, the explainers while the first engineers train, closing a program
that has flown everything. Open: see in the play test whether the order and wording steer a new
player well, and whether three items at a time is the right number.

**Concept chips** (2026-10-07, `ui-spec.md` component 19): `concepts.json` from the concept audit
(§1.1), the chips on program details, configuration details and the VAB's rocket rows, and the "new"
dot there and in the Museum. Their look is confirmed (user, 2026-10-07).

**Animated mission steps** (the user's idea, 2026-10-12; `ui-spec.md` §4.7.3; the clips' list in
`assets-raw/replay/CLIP_CHECKLIST.md`, the pipelines in `assets-raw/replay/` and its README): short
clips per mission step, each with a success, a glitch and a failure variant, drawn in the explainers'
style rather than 3D, made in EffectCraft and played with gdx-video as WebM, the way the splash is
(`ui-spec.md` §4.0, its proof of concept).
- **Decided (user, 2026-10-12):** Claude animates the steps alone (SVG layers and motion as
  formulas: smoother than rebuilding a Claude Design storyboard part by part, and much quicker);
  Claude Design is brought in only for inspiration where a step has no good idea yet.
- **A whole mission as one film:** the replay plays each step's clip back to back, chosen by the
  step's outcome, on a monitor; the result shows at the clip's `revealAt`, and steps play on by
  themselves (Play/Pause). Two alternating players, the next clip preloaded, make the join seamless
  (17–22 ms, within one screen refresh; one player leaves a 37 ms gap). Each clip ends where the next
  begins. A step without a clip plays the no-signal loop. A clip rule can name the step before, or
  with `earlier` any step before it in the season, `programs`, a `later` step and a `"vehicle"`.
- **As built:** every one of the 249 seasons plays whole and every step has its clips, many
  borrowed; from Mercury on the spacecraft and probes fly as themselves (a step's `"skin"` and
  `"alias"` in `build.py`), the rockets stay generic (the report doesn't say which flew). The
  overhaul pass and every clip's choices stand as built (user, 2026-10-21). Laika's lost signal is
  shown as a lost signal, never a flatline.
- **The replay console** (2026-10-13, from a Claude Design mockup, `ui-spec.md` §4.7.3, "The
  console"): the picture, a mission clock, a status gauge whose needle tells the outcome at the
  reveal, and telemetry bars, whose motion tells nothing before the reveal. **Open (user,
  2026-10-13):** a pass over the mission steps' configuration gives each step a mission time to start
  from (T+ at liftoff, at orbit insertion, ...), so the clock advances from there; until then it's a
  show clock counting the clips played.

**Sound for the replay** (the user's idea, 2026-10-13, `ui-spec.md` §4.7.3, "Sound"): no audio baked
into the videos, so ambience can cross from one step to the next. A pool of named sounds
(`assets/content/replay-sounds.json`, each one's description its brief), the clips' sidecars cueing
them like `revealAt` (kept in `assets-raw/replay/cues.json`), the console's own sounds
(`ReplayConsole`), room tone under everything, radio calls on every step and outcome, the user's
effects, and a fanfare at the season summary. **Decided (user, 2026-10-13):**
- **English for both sides.** Soviet recordings are hard to find online without Russian, and none of
  the players speaks Russian: Russian callouts would cost a lot and make the replay harder to follow.
  NASA's recordings serve both agencies, unrealistic as that is.
- **No background music, a fanfare at the end instead** (a few shuffled tracks would be hard to get
  right without grating). One fanfare when the season summary shows a mission that didn't fail
  (glitches, solved or not, included), another when it failed; a catastrophe shares the failure's.
- **Accepted from Claude's proposal:** the pool as data, not an enum; cues with loops, fades and a
  loop carried into the next clip; the spoiler rule for sound (`ReplayClipsTest`); several takes per
  sound, no repeats; skips fading everything within 0.1 s; separate music and effects volumes.
- **Placeholders:** a sound without real takes plays a synthesized `placeholder-*` take
  (`assets-raw/replay/sounds/make_placeholder_sounds.py`); replace it by putting the real files in
  its entry's `files`.
- **The Quindar tones** (§1.1) are generated exactly (pure tones, nothing to license), a cue before
  and after each CAPCOM line; the telemetry blip stays a separate, softer console sound.

**A tribute to Carl Sagan and the Pale Blue Dot** (the user's idea, 2026-10-13): the archive's Pale
Blue Dot page (§2.12, user, 2026-10-15), unlocked by `saturn-flyby`, the furthest flight in the game
(neither pack has a Voyager program).
- **Rights:** the image is NASA/JPL's, public domain. Sagan's text and his reading of it are from
  his book *Pale Blue Dot* (1994) and belong to his estate: not usable without permission, even in a
  non-commercial project.
- **The estate's stance (researched 2026-10-13):** no published policy on fan or non-commercial use.
  Druyan-Sagan Associates (Ann Druyan) licenses Sagan's voice commercially (Apple 2017, Fiat
  Chrysler 2019, Netflix 2025) and sued Luma AI on 25 August 2026 over eight seconds of it in an ad,
  after a cease-and-desist in February was ignored. No takedowns of fan videos were found, and one
  well-known Pale Blue Dot fan video says it's posted "with the kind permission of Cosmos Studios":
  asking has worked. The passage is "© 1994 Carl Sagan, © 2006 Democritus Properties, LLC"; the
  Library of Congress refers permission requests for its Sagan collection to Ann Druyan or her
  designee. No public permissions contact was found; Cosmos Studios is the likely first address.
  Germany's quotation right (§ 51 UrhG) is narrower than US fair use and probably doesn't cover a
  tribute screen.
- **Decided (user, 2026-10-13): use Sagan's recording without asking.** The user accepts the risk as
  very low for a private, non-commercial game among friends, and doesn't want to draw the estate's
  attention to the project by asking. The recording is the Library of Congress's item
  `cosmos000110`, https://www.loc.gov/item/cosmos000110/, Sagan reading the entire passage, 4:36, in
  the Seth MacFarlane Collection of the Carl Sagan and Ann Druyan Archive (Motion Picture,
  Broadcasting and Recorded Sound Division); the user keeps all of it (2026-10-15).
  `assets/CREDITS.md` credits it as it is (Carl Sagan, the LoC item and its collection, the URL, "©
  Druyan-Sagan Associates"), claiming no licence and no public domain. **If the estate ever objects,
  take the clip out at once.** The decision said to revisit it before the repo went public; the repo
  went public on 2026-10-28, and no revisit is recorded: **open, the user's call.**

**R&D decisions** (the user's idea, 2026-10-10, the first of the "interactive events": decisions
between flights, so a wait for R&D isn't empty; `ui-spec.md` component 26 and G11; `rules.RdDecisions`,
`DecideAction`; canvas `Launch Window — R&D decisions`): a choice about a component under R&D, offered mostly in lulls, decided at once (the
outcome drawn from a seed fixed to the decision, so no rerolls and the journal replays it), its
default applied if the turn resolves unanswered. Eleven types in each pack's `decisions.json` (a pack
file): resource for a chance, knowledge (drawing on the explainers), ambiguous, who oversees it,
trade-offs over time, diagnosis, opportunity with strings, pairing, intelligence tip, story arcs and
hands-on. 185 decisions since 2026-10-25, about 20 first steps a type (pairing 12, hands-on 7). Only
one option a decision may take an overseer, and no negative funds effect. **The season's gain is fixed
when a decision is offered** (the user's play-test find, 2026-11-11): a reliability effect's "seasons
of R&D" are the gain of the team that did the season's work (`PendingDecision.seasonGain`,
`RdDecisions.seasonGain`), so moving engineers on and off before deciding grows nothing. Decisions
shorten the bots' Moon race by about 13 seasons. Variety isn't a worry: other lull ideas may join it
(user).
- **The hands-on decision** (type 11; the user's idea, 2026-10-11, from their "Probe Command Minigame"
  spec, the Claude Docs page of that name; canvas `Launch Window — Hands-on`; `rules.HandsOn`,
  `HandsOnAction`; *every number unconfirmed*): a turn-based resource puzzle with a push-your-luck step,
  adapted from a probe in orbit to a ground test campaign. The mechanics come from another game's
  minigame; mechanics are free to borrow, but every name, number, text and drawing here is our own
  (the spirit of §1.2's rule).
  - **The choice:** "Run it yourself" (opens the puzzle) or "Leave it to the team" (the default:
    nothing happens). Starting commits: an unfinished puzzle settles as it stands when the turn
    resolves (a waiting fail accepted, an unconfirmed queue refunded, the rounds left run empty), so
    quitting after bad rolls never falls back to the default.
  - **The puzzle:** four resources, a fixed command list, a few rounds of up to three commands each.
    **Plan:** queue commands; their inputs are paid at once, their outputs only at execution, so
    outputs can't pay for another command the same round; undo and reset refund. **Execute:**
    commands resolve in slot order, each rolling fail (primary output −1, never below 0), normal, or
    exceptional (+1); on a fail the player may pay 1 of the first resource to turn it normal. Secondary
    outputs always pay. The recharge command (no input, +1 of the first resource) never rolls and uses
    a slot. Meeting the objective when the rounds run out is a success, the bonus objective a bonus
    success (ending the puzzle early), anything less a failure. Held amounts count, not totals.
  - **Ground theme for a rocket's test campaign** (reference level): *Shifts* (pays the basic commands
    and for resisting a failure), *Firings* and *Measurements* (the objective), *Spare parts* (the
    intermediate). Three groups of three commands, each group's first costing only shifts, the others
    the other groups' resources; plus *Overtime* (+1 shift). Start 6 shifts; 4 rounds of 3; objective
    3 firings and 4 measurements, bonus 6 and 7. A payload's campaign has its own words (*Bench runs*,
    *Measurements*, *Spare parts*). Levels live in `decisions.json` (*A week on the big test stand*,
    *A week in the vacuum chamber*, and the later ones; help's `handsOn.*` tokens quote the first).
  - **The team sets the odds:** the fail chance is 35 % less a quarter of the best team member's skill
    in the component's skill, never under 10 %; the exceptional chance is 15 %, 20 % with good
    chemistry (as the player sees it); the rest normal. Resisting reads as "call in an expert". A
    component with nobody on it can't offer the type.
  - **Rewards in seasons of R&D gain:** bonus success about three seasons' gain, success about one and
    a half, failure nothing. A choice, not a duty: a player who never plays it isn't behind.
  - **Server-authoritative:** the puzzle's state lives in the open decision (`PendingDecision`), and
    each step (queue, undo, reset, confirm, resist, accept) is a journaled action the server applies at
    once. The rolls come from the decision's own seed and the roll's index, never sent to the client
    (`withoutSeed`), and the expert's answer never changes the next roll. A confirm runs its round up
    to the first fail that waits on a resist choice. The rules are a pure state machine in `core`, the
    client only draws it (G7).
  - **Levels are authored, not generated,** each checked winnable to the bonus by a search over
    all-normal play (`HandsOnTest`); difficulty may rise with the component's era later.
  - **Measured (2026-10-11):** the bot playing the solver's plan and resisting every fail got the bonus
    in 480 of 522 campaigns, no failure: **the bonus is too easy for a careful player.** Levers (for the
    user to choose): a harder bonus, three rounds, or a dearer expert.

**Front pages for the Firsts** (the user's idea, 2026-10-09; `ui-spec.md` §4.18 and G14,
`rules.FrontPagePrinter`): when an agency makes one of the great Firsts (satellite in orbit, human in
space, human in orbit, crewed lunar flyby, the Moon landing), every player sees a fictional
newspaper's front page, *The Daily Zenith*, on login before the turn report. The words are in
`assets/content/front-pages.json`; the art is generated with ComfyUI (`assets-raw/frontpage/`), and
the mockup was laid out in Photocraft. The orbit photo shows the player's own pilot (12 versions,
`client.view.PilotPhoto`, user, 2026-10-31). Every choice there is unconfirmed.

**The world reacts** (the user's idea, 2026-10-20; `ui-spec.md` §4.18.1, component 28 and G17, canvas
`Launch Window — The World Reacts`): when a
First's front page is printed, every rival may give a press statement during the next turn: one of
five stances (enthusiastic, polite, "we'll be next", downplay, no comment) plus an optional quote of
up to 140 characters, printed word for word. At the next resolve a follow-up page, "The World Reacts",
prints them as one article (`rules.ReactionPrinter`), and rivals who stayed silent appear as not
responding. Flavour only. The user chose quotes printed as given over an LLM writing the article (an
LLM rewrite could come later as a layer on top, falling back to the quote). **The article reads as a
story, not a list** (user, 2026-10-22): the statements' mood picks a story written whole for the First;
stance lines are joined by linking sentences; clauses on where a rival stands come from public facts
only. At most 4 statements are told in full; the rest share a line. A page that runs long drops its
close, then runs on to a continuation sheet, never a scroll area (user). A late statement is refused,
not lost. Its first plan is in `ideas/world-reacts.md`, superseded where they differ. Unconfirmed:
late joiners can't answer; no page when nobody spoke; follow-ups print before the turn's own Firsts.

**In memory** (the user's idea, 2026-10-22; `ui-spec.md` §4.18.3, component 29 and G19,
`rules.MemorialPrinter`, canvas `Launch Window — In Memory`): a memorial page in the paper for every crew lost in flight, printed a turn
late so it never spoils the replay (user), with the agency's statement in the tone its director picks
(their courage, who they were, we will go on, we will find out why) and the other agencies'
condolences; a news line when crewed flights resume. It came with **a goal counts only if the crew
comes home** (§2.6). Left for later: secrecy (a Soviet agency keeping a loss quiet).

**Theories of the day** (the user's idea, 2026-10-22, flavour only; `ui-spec.md` §4.18.2 and G18,
`rules.SciencePrinter`, `assets/content/theories.json`): the questions the real space age argued over
before a flight settled them (the deep lunar dust, eating weightless, the radiation belts,
micrometeoroids, a warm Venus, canals on Mars, mascons, Moon germs), raised for a player at a
checkpoint in the turn report and settled for everyone by the first flight that answers them: a
science page in the paper for the public's questions, a news line for the engineers'. The history is
in §1.1. Considered and left for later (user): a light rules effect, an open question showing as
"unknown" on its steps until settled; theories that turn out right (the belts) as a new hazard.

**Filler bots** (the user's idea, 2026-10-25): agencies the server plays itself, so a small play test
has rivals. Listed in `game-meta.json` as `"bots": [{"id": "bot-1", "archetype": "SOVIET"}]`
(`BotPlayer`; an agency or director name is optional, drawn from `agency-names.json` and the personnel
names otherwise), each a player folder like any other (`players/bot-1`, made through `PlayerSlots`),
which no account can log into. A bot plays its turn just before the turn resolves, after the turn's
backup, so `TurnRollback --redo` plays it again with the fixed build; its `bot.json` keeps the last turn
it played, so a retry doesn't play it twice (`BotPlayers`). Its actions go through the same validation
and journal as a player's: `bot.FillerBot` proposes one at a time against the preview, and one the rules
refuse isn't tried again that turn. It follows the playout bot's plan (`bot.BotPlans`): opens the next
flight's program and the cheapest rocket that lifts it, upgrades and hires until it can staff it (one
more for each seat auto-assign can't fill beyond the people away training or sick; without that
allowance hires snowballed), puts its engineers on the flight's hardware, sends idle people to training
while the hardware is far off, and launches at about 70 % reliability (65 to 75, a turn's draw). It
answers R&D decisions with an option at random (never the hands-on puzzle), runs an Analysis section
above 8,000 and an assessment of a random rival about one turn in three, skips about one turn in ten,
and gives statements on 70 % of its rivals' Firsts and condolences on 85 % of their lost crews. **Bots
can win** and are marked "(bot)" in the race, the Intelligence Office and the game result, not in the
paper (user, 2026-10-25; `RivalSummary.bot`). *Every number is unconfirmed.* On the early-era pack two
bots reach the whole race in 90 turns (`BotPlayersTest`, since the skill-growth cut); on the standard
pack, in one 200-turn game, the NASA bot won the Moon race at turn 187: slow, with money piling up past
2 million (unexplained; it sat 40 turns on Apollo's lunar-module test). Left out: contracts, findings,
PR duty, poaching, reacting to the news beyond statements.

**Also built, recorded in their sections:** PR duty (§2.2(c)), people (§2.2(d)), intelligence (§2.9),
commercial contracts (§2.11), the archive (§2.12), publishing findings (§2.13), the guided progression
and the roads to the Moon (§2.3(d), (e)), the Sputnik shock and raises (§2.1(e), (f)), the grounding
(§2.5(j)), rocket-plane wings and specialists (§2.2(c)), public prestige and the standing charts
(§2.1(g)), undoing a bad turn (§3.7).

**Still open:**
- **A smarter bot, next:** the R&D tuning and the hands-on and contract measurements rerun with the
  smart bot, a choice of R&D decision options by expected value (it still picks at random), and the two
  balance points it raised (§2.9): cheap failures reward launching early, and idle staff quit fast.
  Lessons for an NPC rival, if one is ever built: buy nothing permanent for a passing goal, and stay
  solvent before staying busy.
- **More interactive events** beyond the R&D decisions, if the play test asks for them.
- The console's mission times (above); the hands-on levers (above); the Sagan revisit (above).

### Later

- **M6 — Deployment: built 2026-10-06; releases `v0.0.1` and `v0.1.0` tagged.** The jpackage client
  zip, the server Docker image with a bind-mounted data root (`deploy/docker-compose.yml`), and the two
  tag-triggered workflows (commands and gotchas in `CLAUDE.md`). The packaged exe is DPI-unaware by
  default (Windows scales it). **The repo is public since 2026-10-28** (user, for the wiki). The GHCR
  image's visibility is a separate package setting: it was still private on 2026-10-29, so the NAS
  pulls it with an access token until it is made public (the user means to).
- **M7+ (order open):** a game-host role and UI (§4); validating the `WEEKDAYS` profile with the
  colleagues' group; more Museum encyclopedia entries as content calls for them (§1.1). People,
  interaction, the full content tree and E-01 to E-31 are built (§2.2(d), §2.9, §5 M3, §1.1).
- **Dropped: launch latitude as a mechanic** (user's idea 2026-10-07, dropped by the user
  2026-10-08). Considered: a site factor on payloads, orbit classes on missions (polar,
  geostationary, the Soviets' Molniya orbit as their answer), and second launch sites as
  buildings. None was worth its weight: each side's rockets only ever fly from their own site, so
  the latitude is already in every rocket's lift, and E-14 says so.
