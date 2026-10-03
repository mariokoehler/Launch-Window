# Launch Window — Design Document

This is a living document describing what the game is, the architecture, and
current/open work. It describes the game **as designed or implemented right
now** — build narrative, bug-hunt stories and verification logs belong in git
history/commit messages, not here. Update it the same session a decision is
made. Section numbers are stable cross-reference anchors (including from
`CLAUDE.md`) — don't renumber when editing.

**Status legend.** Everything in this document is *design* unless a section
says "implemented". A detail marked **(unconfirmed)** is a concrete default
proposed to keep the design moving; it has not been signed off and may
change. Details marked **(from BASPM)** are carried over from the original
game as a starting point, not yet re-evaluated for the multiplayer/async
context — see `baspm.md` for where that came from.

## 1. Concept

**Launch Window** — *"Stay one small step ahead."* (name and tagline decided
2026-09-27; a working title of "Space Race" was used before this) — is a
non-commercial fan project: an asynchronous multiplayer reimagining of
**Buzz Aldrin's Space Program Manager** (BASPM) — a Cold War-era space
agency management sim where the player runs NASA or the Soviet space
program, manages personnel/budget/prestige, and designs and flies missions
from Sputnik through Apollo (and beyond, in later game modes).

**On the name:** "Launch Window" is a real astrodynamics term — the narrow,
non-negotiable time period a rocket actually has to launch in — chosen
because it doubles as a description of the game's own defining mechanic
(§1's "session shape" below): each day is the player's window to act, and it
closes on schedule whether they show up or not. The tagline riffs on
Armstrong's "one small step" — reframed from humility into competitive edge,
fitting a game about staying ahead of a rival agency over a long game.

- **Genre:** asynchronous multiplayer strategy/management sim (space agency
  builder). Not real-time, not simultaneous-live-session turn-based — see
  the "session shape" note below, this is the core structural difference
  from both sibling projects.
- **Session shape (the defining mechanic):** players log in whenever
  convenient during the day, review the previous turn's results, and queue
  up their actions (research/hiring/program decisions, mission scheduling,
  etc.) for the *current* turn. At a fixed daily server time, the server
  resolves every player's queued actions simultaneously and advances the
  world by one turn. Nobody needs to be online at resolution time, and
  nobody blocks anybody else — the opposite of Robot-Rampage's "everyone
  must confirm before the turn resolves" model. **Decided (user,
  2026-09-27): one real-world turn = one BASPM "season"**, straightforward
  1:1 mapping, no finer-grained daily sub-turns within a season. This
  resolves the turn-cadence open question that used to live in §4 — a season
  is still exactly one quarter of a year in fiction (four seasons/year,
  budget review every four years/16 seasons, `baspm.md` §3.1/§7), it's just
  that "one turn" no longer means "one calendar day" universally — see the
  **turn scheduling** note below and §3.6: not every real day produces a
  turn.
- **Match length:** a full game is expected to run **weeks to months** of
  real time, not a single sitting — a deliberate consequence of the daily-ish
  cadence, not a limitation to design around.
- **Turn scheduling is per-game-instance configurable, not a fixed global
  cadence (user, 2026-09-27).** Two anticipated player pools with different
  needs: a friends group wants a turn every real day, 7 days a week; a
  work-colleagues group wants turns only Monday–Friday, explicitly to avoid
  any pressure to log in on a weekend (they'd play during a work coffee
  break). Both are the same underlying mechanic — a per-game weekday
  schedule — plus host-configurable **blackout periods** (holidays,
  vacations) that suppress resolution even on an otherwise-active weekday,
  addable *mid-playthrough*, not just at game creation. Full design in §3.6.
- **Players:** more than BASPM's hard 1v1 cap (its own PBEM multiplayer modes
  were strictly two-player — see `baspm.md`). **Two concrete target sizes
  (user, 2026-09-27):** a friends group at **~4 players** and a
  work-colleagues group at **~10 players** — these are the two real games
  this project is being built to actually run, not hypothetical scale
  targets. Architecture should comfortably handle small-group sizes in that
  ~4–10 range rather than being designed around a persistent-world MMO;
  revisit the upper bound once §2.9's interaction-model scope
  (passive-competition-first, sabotage/interference layered in later) is
  actually built, since interference/sabotage mechanics may scale
  differently than passive competition does.
- **Fan project note:** BASPM is a game by Slitherine/Matrix Games. This
  project is non-commercial, references BASPM purely for analysis and
  inspiration (`baspm.md`), and intends to ship its own name, art, and
  content rather than copying BASPM's.

### 1.1 Educational layer (a core pillar, not a bolt-on)

**Decided (user, 2026-09-27):** Launch Window should be genuinely educational
about the real history and engineering of spaceflight, not just themed
around it. This is a design pillar to actively look for opportunities to
serve while building every other system, not a separate feature to schedule
later — **whenever a system being designed or analyzed touches a real
historical mission/challenge, flag the educational angle it opens up, the
same session it comes up**, the same standing-note-to-self habit as
`CLAUDE.md`'s existing ones (see there).

- **BASPM already has a real foundation here, not a blank slate.** Every
  program, mission component, and mission configuration ships with
  well-written historical/political narrative text keyed to its ID
  (`ProgramsHistoricFactsStringsEN.xml`, `MissionComponentsHistoricFactsStringsEN.xml`,
  `MissionsConfigurationsHistoricFactsStringsEN.xml` — `baspm.md` §3.2 has
  the exact paths). E.g. its Sputnik entry covers the real politics behind
  why the simpler "PS-1" flew instead of the more ambitious "Object D."
  **This existing text answers "what happened and why, politically/
  historically" — it does not explain "why was this actually hard,
  technically."** That gap is exactly where Launch Window can add real value.
- **The concrete example that prompted this** (user): Gemini's orbital
  rendezvous/docking missions sound trivial ("fly up to the other capsule
  and dock") but are deeply counterintuitive — you can't just point at the
  target and thrust toward it, because orbital mechanics means burning
  toward a target actually raises your orbit and puts you on a slower path
  that falls *behind* it; catching up means dropping to a lower, faster
  orbit first. Similarly: restarting a rocket engine in zero-g/vacuum is
  hard because there's no gravity to settle the propellant against the
  tank outlet, among other ignition problems. **These "why is this
  counterintuitive/hard" explanations are the target content type** — short,
  optional, in-the-moment — not a full textbook.
- **Presentation (unconfirmed, needs its own design pass once §2's core
  loop exists):** almost certainly opt-in/non-blocking — an expandable
  panel, tooltip, or "learn more" link attached to the relevant mission
  step/program/component, not a forced pop-up — so it adds depth for
  curious players without slowing down anyone who just wants to play the
  strategy game. BASPM's own Museum building (`baspm.md` §2, manual p.40)
  is a plausible home for a deeper "encyclopedia" version of the same
  content, if a dedicated screen turns out to be worth it. **No longer
  hypothetical (2026-09-30):** the main-menu art the user authored already
  has a `MUSEUM` hotspot, alongside BASPM's own other six Space Complex
  buildings — see §3.13. The educational-encyclopedia screen this bullet
  proposed now has a confirmed place to live; still not designed in any
  detail (content model, how it surfaces the §1.1 beats scattered through
  this section), just no longer an "if."
- **Real images:** wanted, per the user. **Flagging a licensing
  consideration proactively** (unprompted, per this project's existing
  security/legal-flagging convention): NASA-produced photos/footage are
  U.S. government works and normally public domain, so those should be
  low-friction; Soviet/Russian-era imagery and any ESA/other-agency material
  has murkier and inconsistent rights depending on source/photographer —
  this needs a real check per-image before shipping, not an assumption that
  "old space photo" automatically means free to use.
- **Scope note:** this is additive to whatever real-program/agency framing
  §1's core concept ends up keeping vs. reskinning (see §4's open question
  on that) — the educational angle works whether Launch Window keeps BASPM's
  real Sputnik-through-Apollo framing or reskins it, as long as *some*
  real-world engineering challenge is still being modeled underneath.
- **Surfaced during the §2.5 mission-simulation trace (2026-09-27): the
  "saving throw" mechanic (a failing step getting caught/fixed by skilled
  flight controllers, `baspm.md` §8.2) is modeling something real and
  genuinely dramatic, not just a game-balance abstraction** — real Mission
  Control teams have saved missions through exactly this kind of real-time
  anomaly response: Apollo 11's 1202/1201 program alarms during the lunar
  descent, where a controller's split-second correct call *not* to abort
  (recognizing the computer was just overloaded, not failing) let the
  landing proceed; Apollo 13's oxygen-tank explosion, where the "failure"
  didn't end the mission because ground teams improvised a survival plan in
  real time. A short "here's the real event this kind of save is modeling"
  note attached to a step that gets rescued by a good saving throw is a
  strong, concrete candidate for this pillar — dramatic, true, and it lands
  at the exact moment the mechanic just made it matter to the player.
- **Surfaced during the §2.2 personnel trace (2026-09-27): Mission Control's
  five skill categories (`baspm.md` §9.1) map cleanly onto real NASA flight-
  control disciplines** — Propulsion, Trajectory & GNC, Spacecraft Systems,
  Crew Systems & Payloads, Mission Operations echo real console positions
  like FDO/GUIDO and EECOM. **"Flight Director" being computed as the plain
  average of a controller's other five skills, not an independently trained
  specialty, is itself a decent small lesson**: a real Flight Director is a
  generalist who coordinates specialists rather than being the best at any
  one discipline — worth a short in-game beat the first time a player
  designates someone as Flight Director, explaining that a great FD is
  broad, not deep.
- **Surfaced while redesigning Tiger Teams for async play (user, 2026-09-27):
  "Tiger Team" is itself real aerospace/NASA jargon** — a temporary task
  force of specialists assembled to solve an urgent problem, real-world
  terminology that predates BASPM's use of it (not BASPM's own invented
  content, so §1.2's no-copying rule doesn't apply to keeping the term
  itself). Worth a short in-fiction note on why the term exists/where it
  comes from whenever a player first assembles one, in the same spirit as
  this pillar's other real-history beats.
- **Surfaced while designing the Expert trait's rescue mechanic (2026-10-02):
  Apollo 13's own command-module power-up is real, concrete precedent for
  letting both an Astronaut and a Mission Control Expert rescue the same
  mission together** — the emergency procedure for powering the nearly-
  dead Command Module back up for reentry, after days running on the Lunar
  Module's own limited resources, is most often credited to EECOM flight
  controller John Aaron and the ground team he worked with, who devised
  the sequence under severe power constraints; Ken Mattingly — bumped from
  Apollo 13's prime crew by measles exposure, and so grounded for the
  entire mission — spent that time in the simulator testing and refining
  it before it was ever read up to the crew. A real flight controller
  devising the fix and a real, non-flying astronaut verifying it from the
  ground, together, is exactly the two-group rescue this project's own
  Expert mechanic models (§2.5(i)) — worth a short in-fiction beat the
  first time a player's own Expert of either kind actually rescues a step.
- **Surfaced during the §2.3/§2.4 programs/reliability trace (2026-09-27):
  Reliability Transfer's data is a real rocket-family lineage, not a
  gameplay abstraction** — NASA's `ReliabilityTransfers-NASA.xml`
  (`baspm.md` §10.3) literally encodes the real Redstone → Jupiter-C → Juno
  II and Atlas/Titan/Saturn family relationships, at percentages that echo
  how much actual hardware/guidance experience really carried over between
  those programs. A strong, concrete beat for showing *why* a new rocket
  isn't just reused art with a new name — real rocket families really did
  inherit engines, guidance systems, and institutional experience from their
  predecessors, which is exactly what let real programs move fast. Also
  worth a beat on **`OpeningCostDeductions`'s "fast follower" discount**
  (`baspm.md` §10.1, e.g. Explorer I costing 40% less once Sputnik I is
  already open) — a real, true dynamic of the actual Space Race (whoever
  answers second gets to skip R&D the first mover already paid for).
- **Surfaced during the §2.1 budget/prestige trace (2026-09-27): "prestige
  literally becomes your budget" is not a game abstraction — it's a fair
  read of how NASA's real funding actually worked.** Congress's willingness
  to fund Apollo at the scale it did was substantially driven by Cold War
  prestige competition, most visibly the political shock of Sputnik (1957)
  and Gagarin (1961) — NASA's budget rose sharply in the years right after
  each, well before Apollo had flown a single crewed mission of its own to
  "earn" that funding on engineering merit alone. A strong, true beat for
  the moment a player first sees their own budget review: the real space
  race really was funded by prestige, not purely by demonstrated capability.
- **Surfaced during the §2.7 random-events trace (2026-09-27): BASPM's bad
  events mostly aren't pure noise, they're gated to real recent risk
  factors** (`baspm.md` §12.1) — a reliability setback only strikes a
  component actually in R&D, morale only drops after a recent mission
  failure, extra funding only arrives after a recent run of successes. This
  is a true structural fact about real program risk, not just a design
  nicety: **trouble in a real space program rarely comes from nowhere** —
  it concentrates around whatever's already under strain (an
  already-stretched budget, an already-fatigued team, hardware already
  being pushed hard), rather than striking uniformly at random. Worth
  keeping this framing explicit if Launch Window's own event flavor text
  ever explains *why* a given event fired, rather than presenting it as
  arbitrary bad luck.
- **Surfaced during the §2.8 campaign-objectives trace (2026-09-27): GSA's
  fictional "one unified world space agency" premise (`baspm.md` §2.4) isn't
  pure fantasy — it's a real counterfactual with a real answer.** No such
  agency existed during the actual Cold War space race, but international
  cooperation in orbit did eventually happen for real, just decades later
  and in a very different form: the **International Space Station**, a
  genuine multi-agency partnership (NASA, Roscosmos, ESA, JAXA, CSA) that
  only became possible once Cold War competition itself wound down. **Launch
  Window decided against a GSA-equivalent faction entirely (§2.1(a1)/(b1),
  2026-09-27), so this no longer has a faction to attach to** — still a true,
  worth-keeping beat, just relocated to wherever it naturally fits instead
  (a museum/history screen "did you know" aside is the obvious candidate,
  not a dedicated faction-introduction moment).
- **Surfaced during the §2.5/§2.6 "vital vs. optional step" discussion
  (2026-09-27): grading a mission by how many non-critical objectives it
  hit, rather than a flat success/fail, isn't a video-game invention** —
  real space agencies genuinely plan and report missions this way. NASA/JPL
  project formulation routinely distinguishes **must-achieve requirements**
  from lower-tier **bonus/extended objectives**, and real missions get
  graded on exactly that spectrum after the fact (e.g. "prime mission
  success" vs. an extended/bonus phase for a planetary probe that outlives
  its baseline requirements). A true, concrete real-world precedent worth
  a short beat for wherever Launch Window ever explains a mission's
  prime-vs-extended framing to a player — this is how real missions
  actually get scored, not just a game abstraction layered on top.
  (§2.6(c)'s own gold/silver/bronze goal rating, the originally-intended
  home for this beat, is scrapped for v1 — this stays a true, worth-
  keeping fact regardless of which future screen it ends up attached to.)
- **Surfaced during the §2.2 astronaut-PR-duty idea (2026-09-30): real
  astronauts genuinely served a public-relations/fundraising function for
  their programs, not a gameplay invention layered onto them.** The
  Mercury Seven signed an exclusive personal-story deal with *Life*
  magazine, toured extensively, and were deliberately used by NASA to
  build and sustain public/congressional support — the same real dynamic
  §2.1's budget/prestige trace already found (prestige from Sputnik/Gagarin
  drove real funding increases before Apollo had flown a single crewed
  mission of its own). A strong, true beat for wherever an "assign an
  astronaut to PR duty" screen/option ends up landing, whenever that idea
  (§2.2) actually gets designed and built — astronaut-as-celebrity was a
  real, deliberate strategy, not just a game giving idle astronauts
  something to do.
- **Surfaced while deciding `CATASTROPHIC` crew loss for §2.5(f)
  (2026-10-02): real catastrophic spaceflight accidents lost the entire
  crew, not a partial/random subset** — Apollo 1's launchpad fire, Soyuz 1
  and Soyuz 11's re-entry failures, and the Challenger and Columbia shuttle
  losses all claimed every crew member aboard, never just one. This is the
  real-world fact behind the "whole assigned crew" decision in §2.5(f), not
  an arbitrary balance choice — worth a short, sober in-fiction note the
  first time a player actually experiences this outcome, naming the real
  precedent rather than letting it read as a pure game-mechanic penalty.

### 1.2 Content & asset policy: no copying BASPM's copyrighted content

**Decided (user, 2026-09-27), and treated as a hard rule, not a preference.**
Launch Window is non-commercial and will only ever be played with friends/
colleagues — but it lives in a **public GitHub repo**, so anything committed
to it is effectively redistributed. BASPM's actual copyrighted expression
(Slitherine/Matrix Games' IP) must never end up in that repo, in any form:

- **Text** (`HistoricFacts` entries, program/mission names & descriptions, UI
  copy, etc.): **never copy BASPM's strings verbatim, and never lightly
  paraphrase them either** (swapping synonyms around BASPM's own sentence
  structure is still derivative of their specific expression). Any
  educational content Launch Window ships (§1.1) must be **independently
  written from the underlying real-world facts** (the actual history of
  Sputnik/Gemini/Apollo/etc. is public-domain reality, owned by nobody) —
  original prose in the LLM's own words, not a rewrite of BASPM's copy. When
  drafting this content, research/verify against primary or public sources
  (NASA history office, Wikipedia, etc.), not by rereading BASPM's own text
  and rephrasing it.
- **Art/audio/video**: no file extracted from BASPM (`E:\Temp\BASPM\extracted\...`,
  `baspm.md` §4) is ever copied into the Launch Window repo or shipped game —
  those exist purely for private design-reference/analysis, the same
  read-only posture `CLAUDE.md` already states for the source material
  generally, just spelled out explicitly here because art/audio is the
  easiest thing to casually drag-and-drop by accident. The user plans to
  create Launch Window's own original bitmap assets from scratch.
  **Standing note to self: proactively flag if a task ever risks pulling a
  BASPM-extracted file into the actual project tree (as opposed to
  referencing it for analysis) — don't just do it because it's convenient.**
- **What this does *not* restrict:** the real-world subject matter itself —
  actual historical space programs, missions, hardware, dates, people — is
  public historical fact, not BASPM's property. The constraint is on BASPM's
  own specific expression of that subject matter (its particular prose, its
  particular art), never on the underlying facts.
- This sharpens (doesn't replace) the existing "fan project note" in §1 and
  `CLAUDE.md`: those said "we intend to ship our own name/art/content" as
  general intent; this is the explicit, binding version of that intent.

## 2. Core gameplay mechanics

Not yet written. This section will be filled in during the deep-dive analysis
pass over BASPM (`baspm.md`), system by system — each subsection should end
up recording **(a)** how BASPM does it, **(b)** what changes for the
async-multiplayer context, and **(c)** the concrete decision. Candidate
subsection list, derived from BASPM's own manager/system boundaries
(`baspm.md` §"Source code map"), to be expanded/reordered as analysis
proceeds:

- 2.1 **Space agencies, budget & funding cycle — analyzed 2026-09-27, see
  `baspm.md` §11 for the budget/funding trace and §13 for the agency-
  differences trace (added after a direct user question, 2026-09-27).**
  - **(a1) How BASPM's agencies actually differ, mechanically (§13):**
    ranked by real significance — **(1) program/component access is the one
    real, structurally-enforced gate**: NASA and Soviet each only ever see
    their own tagged content (55 programs split 32/23, zero shared ones; 85
    mission components split 50/35), enforced by one shared rule
    (`CoreManager.ElementPassesSpaceAgencyTest`) that also gives **GSA
    universal access to everything, regardless of tag** — confirms the "NASA
    can't launch Sputnik" guess exactly, and confirms GSA is a genuine
    super-agency, not a third separate content track. **(2) GSA also gets a
    real synergy bonus beyond raw access**: its Reliability Transfer table
    isn't just NASA's + Soviet's tables filed together — 44% of its targets
    declare cross-lineage sources neither single-agency table has (a real
    NASA rocket drawing a reliability bonus from a real Soviet one), modeling
    genuine East-West tech-sharing that was historically impossible for
    either agency alone. **(3) Everything else is cosmetic or a structurally
    present but unused lever**: personnel candidate pools differ only in
    name flavor (zero stat differences); `Constants-<Agency>.xml` for all
    three agencies are byte-for-byte identical (same funds, same budget-
    review brackets) despite the per-agency file split existing to support
    divergence; `SpaceAgencyT.Common` is entirely dead code, never
    referenced or tagged anywhere.
  - **(a) How BASPM does it (budget/funding):** prestige (earned only from missions/goals,
    never cash directly) is the sole input to a budget review every 16
    seasons (4 years), which sets a **recurring per-season funding rate**
    (not a lump sum) via a tiered lookup — each review has its own
    prestige-to-budget bracket, later reviews demanding more cumulative,
    all-time prestige for the same funding, with a built-in catch-up
    adjustment if the player fell short of the previous review's bar.
    Funding is the *only* recurring income; every other funds-touching call
    in the codebase is an expense (fixed salaries/maintenance every season,
    Tiger Teams, random-event costs, program/mission/construction spending).
    None of this is hidden — the PAO screen (`baspm.md` §11.4) shows the
    current bracket's exact thresholds and a live-projected next-review
    budget at all times.
  - **(b1)/(c1) Non-exclusive archetypes (`design.md` §3.8, already decided)
    meets (a1)'s findings — decided (user, 2026-09-27): carry over real
    mechanical content-gating by archetype, but keep the economic side
    identical across archetypes.** BASPM's own precedent turns out to be
    exactly this shape already — a real, structurally-enforced content
    difference (program/component access) paired with an economic side that
    the shipped game deliberately leaves identical despite the data model
    supporting divergence (a1's finding #3). Since Launch Window's archetypes
    are non-exclusive (§3.8 — several players can share one), gating content
    by archetype creates no scarcity problem: each player's programs/
    components are resolved from their own independent snapshot (§3.7), so
    multiple players racing to open "their" archetype's Sputnik-equivalent
    never actually contend with each other.
  - **A GSA-equivalent "unlocks everything" archetype is real strict
    superiority in a competitive setting — correction of the previous
    session's proposal (user pushback, 2026-09-27), resolved by re-reading
    BASPM's own actual precedent rather than inventing a compensating bonus.**
    More content options plus a real synergy bonus, on the *same* economy as
    every other archetype (a1's finding #3), is a strict advantage, not a
    trade-off — the earlier idea to "re-derive an equivalent bonus so it
    isn't just better" doesn't actually fix that, since the imbalance is
    structural (more optionality is never worse), not a missing downside to
    balance out. **BASPM's own designers evidently reached the same
    conclusion and solved it structurally, not by nerfing GSA**:
    `MultiplayerColdWar` — the mode explicitly modeling the real historical
    race, `baspm.md` §2.4/§5 — is **hard-restricted to NASA vs. Soviet only,
    GSA isn't even a selectable option there.** GSA is only choosable in
    `MultiplayerFreeform`, the explicitly casual/"anything goes" mode (the
    manual's own example is a player racing *themselves* in hotseat mode).
    And GSA's actual home turf is solo: `CampaignGSA` (`baspm.md` §12.3) is a
    fixed-deadline race with **no AI rival at all** — contrast `CampaignRTTM`,
    which explicitly does have one using the identical win condition. The
    manual's own word for GSA is "**utopic**" (`baspm.md` §2.4) — a deliberate
    wish-fulfillment/alternate-history mode (what if Cold War competition had
    been cooperation instead — the same real-world counterfactual `design.md`
    §1.1 already connects to the ISS, decades later and in a different form),
    not a faction meant to be raced against a rival on equal footing.
  - **Decided (user, 2026-09-27): no GSA-equivalent archetype at all —
    Launch Window ships exactly two non-exclusive archetypes, NASA-style and
    Soviet-style.** Simpler than the previous session's "gate a
    universal-access archetype behind an opt-in ruleset" idea — that whole
    ruleset-flag mechanism is now unnecessary, retracted along with it, since
    there's no universal-access content to gate in the first place. This
    also isn't just "the safe choice" — it's the literal same shape as
    BASPM's own actual competitive mode: `MultiplayerColdWar` (`baspm.md`
    §2.4/§5) is *itself* strictly NASA vs. Soviet only, no GSA option,
    exactly what Launch Window is now doing for its one and only ruleset.
    Fully resolves `design.md` §4's "how much of BASPM's Cold War framing to
    keep vs. reskin" question as far as archetype count/scope goes: two
    archetypes, both real historical agencies, non-exclusive, no fictional
    third faction.
  - **(b) What changes for async multiplayer:** nothing structural — this
    is entirely a between-turn, deterministic computation with no
    mid-resolution decision point (`baspm.md` §11.5), same shape as §2.3/
    §2.4's finding. **Decision:** the
    16-season/4-year review cadence is counted in *seasons elapsed*, not
    real days — so under `design.md` §3.6's schedule profiles, a `WEEKDAYS`
    game and a `DAILY` game reach review #1 after a different number of
    *real* days (a `WEEKDAYS` game needs more real-world time per season on
    average, once weekends are excluded), even though both experience
    exactly the same in-fiction pacing. This is fine — "one
    turn = one season" (§1) already treats real-time elapsed as irrelevant
    to game-time elapsed everywhere else.
  - **(c) Decision:** **port the "prestige is the only currency that
    converts to funding, on a periodic tiered review with a catch-up
    mechanic" shape**, re-deriving actual thresholds/multipliers rather than
    reusing BASPM's constants verbatim (`design.md` §1.2). **Decision:** keep funding as a recurring per-turn rate set at review time,
    not a lump sum — it's the cleaner fit for a persistent async economy.
    **Decision:** keep
    full transparency of the review formula in Launch Window's own UI
    (current bracket, live-projected next payout) rather than hiding it —
    BASPM already treats this as a feature, not a spoiler, and transparency
    matters more, not less, when a player wasn't present for the turn that
    changed their funding.
  - **(d) Proposal for the budget slice (2026-10-03; every default below is
    unconfirmed, and the four starred items need user decisions before any
    code, per `CLAUDE.md`'s pause rule for balance-defining choices):**
    - **Income.** `PlayerSnapshot.fundingRatePerSeason` (already present) is
      credited to funds every season. Its initial value is a Launch Window-
      own `initialFundingRate` in content, not BASPM's `InitialBudgetPerSeason`
      (§1.2).
    - **Reviews.** Every 16 seasons, a review sets the new
      `fundingRatePerSeason` from a `BudgetSchedule` content record: an ordered
      list of per-review brackets `(minPrestige, minFunding, maxPrestige,
      maxFunding)`, with BASPM's own shape - linear interpolation between the
      ends, ceiling-rounded, flat at each end. **Numbers: close to BASPM's own
      Normal-difficulty table (user, 2026-10-03; see the salary bullet below for
      the §1.2 flag).** The M3 content pack will own the final table.
    - **Catch-up.** Traced this session (`baspm.md` §11.7): if prestige at the
      previous review fell short of that review's *raw* minimum, the current
      review's bracket shifts down by that shortfall. Shortfalls therefore
      don't compound. Snapshot fields: `budgetReviewCount`,
      `prestigeAtLastReview`.
    - **Expenses, charged each season after `PersonnelLifecycle`:** program
      `maintenanceCost` for every opened program (exists already), plus
      **\*personnel salaries** (see the starred decision below). Not modeled
      in v1: mission-component upkeep and any facilities/building cost - Launch
      Window has no facilities concept, and BASPM's per-component fixed costs
      have no Launch Window field yet.
    - **Salaries - decided (user, 2026-10-03).** Yes, in v1. This supersedes
      the 2026-09-30 "hiring is free until budget exists" decision (§2.2).
      **Values: close to BASPM's own, per the user's instruction of 2026-10-03
      ("stay close to the original BASPM values" for defaults).** Each employee
      carries an individual salary drawn from BASPM's own range of 25-60 per
      season (`PersonnelManagerConstants.xml`'s salary range), drawn when the
      employee is created (candidate or starting roster), not a flat
      per-group figure. Charged every season the employee is on the roster.
      **Flag:** §1.2 says content is re-derived rather than copied, so these
      numbers are a deliberate, user-directed exception, to be reviewed if the
      user prefers stricter re-derivation.
    - **Budget defaults, close to BASPM (user, 2026-10-03):** initial funds
      25,000; initial prestige 250; initial funding rate 3,000 per season; the
      ten BASPM review brackets (Normal difficulty) as the `BudgetSchedule`;
      program maintenance and mission-component maintenance in BASPM's own range
      (component maintenance is a real BASPM expense - `MissionComponents.xml`'s
      `MaintenanceCost`, and it's now modeled as `MissionComponent.maintenanceCost`
      rather than left out). Beyond the tenth review, which BASPM's own table
      doesn't cover, the last bracket repeats (Launch Window's own choice, flagged).
      Conversion rate: 4 prestige points per 1 fund (unconfirmed, see below).
    - **Negative funds - decided (user, 2026-10-03), a departure from
      BASPM.** BASPM treats funds below zero as an immediate game-over
      (multiplayer: forced surrender, `baspm.md` §11.8). Launch Window allows
      debt instead: funds can go negative, and debt blocks new spending through
      the existing `INSUFFICIENT_FUNDS` checks, with no other penalty. The
      harness's "funds never negative" invariant is dropped accordingly.
    - **Prestige-to-funds conversion while in debt - decided (user,
      2026-10-03), a Launch Window addition with no BASPM precedent.** A player
      whose funds are below zero may convert prestige into funds:
      - **Offered whenever the player is in debt, and only then.** The offer
        repeats every time the player is in debt, for as long as they have
        prestige. A player in debt with no prestige left has to hope the next
        funding review saves them.
      - **Fixed conversion rate:** `prestigeToFundsRate` (funds per prestige
        point). The number is unconfirmed; it's set in the harness fixture and
        in M3's content. It should sit well below what a review pays per
        prestige point, so conversion is an emergency tool, not a strategy -
        the same prestige would otherwise have gone toward the review.
      - **The player chooses the amount**, from 1 up to the cap below. The
        conversion itself is a journaled player action, applied in the same
        "before any resolver" bucket as the other actions.
      - **Cap - decided (user, 2026-10-03): the conversion brings funds up to
        two seasons of running costs.** With funds at `-D` and running costs
        `R` per season, the most a single conversion can add is `D + 2R`, so
        funds end at no more than `2R`. Running costs are salaries plus
        maintenance, the same figure as the season's expenses. The conversion
        is also capped by the prestige the player holds times the rate. A
        deeply indebted player can therefore clear the debt and keep a
        two-season buffer in one conversion.
      - **Prestige is spent,** so a conversion lowers the prestige that the next
        review will see. That's the intended trade-off.
      - **Validation:** a conversion submitted while the player is not in debt
        at application time is rejected (no state change). Validation happens
        at application, not submission.
    - **Season order (corrected 2026-10-03 after advisor review).** Journaled
      actions (mission launches, debt conversions, hires), then
      `PersonnelLifecycle`, then `MissionProgress`. Goals are awarded inside
      `MissionProgress`, per step, as BASPM does (§2.6(d)), so this season's
      step prestige and goal prestige both exist before any review. Then
      `BudgetProgress`, in the internal order BASPM traces
      (`baspm.md` §11.7): (1) fixed costs (salaries and maintenance), (2)
      increment the player's seasons-elapsed counter, (3) run the review if
      one is due, which sets the new funding rate, (4) grant this season's
      funding at the *new* rate. A review season therefore pays the new rate
      immediately. Then `TrainingProgress`, `ResearchProgress`, and
      `HiringPool.refreshCandidates`. The cross-player win check (§2.9) runs
      once per resolution, after every player has resolved.
    - **Review cadence and late joiners (unconfirmed default, to be decided
      with M4's slot-claim work).** BASPM counts each player's own seasons
      (`baspm.md` §11.7), which is the same as game time only when everyone
      starts together. Default proposal: reviews fire on fixed *game* turns
      (16, 32, ...), so every player is reviewed together and a late joiner
      starts at `initialFundingRate` until their first review. This needs a
      `joinedAtGameTurn` on the snapshot, which M4 owns.
    - **Currency rule carried over:** prestige, not cash, is the only input to
      a review. Missions and goals grant prestige only; they never grant
      funds directly (`baspm.md` §11.1). Prestige is clamped at zero, since a
      cumulative total that can go negative would make the review bracket
      meaningless. Unconfirmed default, flagged.
  - **(e) Consequence for the composition order (`CLAUDE.md`, M2 status):**
    the budget and goal passes change the season order documented in §2.5(f)
    once they land. The pure-function resolvers each stay headless and
    independently testable; only the caller's order changes.
- 2.2 **Personnel (astronauts, flight controllers, SET/scientists) — hiring,
  training, skills, morale/fatigue — analyzed 2026-09-27, see `baspm.md` §9
  for the full trace.**
  - **(a) How BASPM does it:** three groups (Astronauts, Mission Control,
    SET), each with 5 skills; Mission Control's "Flight Director" is a
    derived average of the other five, not an independently trained skill.
    Age/morale/retirement/resignation are all evaluated in one season-advance
    pass: idle staff lose 1 morale/season, busy staff (assigned or training)
    gain 2; resignation is a hard floor (morale ≤ 40) checked only while
    idle, so a busy employee never resigns or ages into retirement mid-
    assignment no matter how marginal their stats get. Training has two
    modes — fixed-length, or "permanent" (auto-repeats every season until a
    target skill is hit, a standing order needing no re-declaration, same
    shape as R&D assignment per `baspm.md` §2.1) — with a real diminishing-
    returns formula scaled by learning potential, and a precisely
    proportional partial-credit penalty for pulling someone out early (linear
    in seasons-already-completed). SET's R&D skill growth is flat per season
    (no diminishing returns), unlike training and unlike astronaut/
    controller skill growth from actually flying missions (which does use
    the diminishing-returns curve, and grows from flying regardless of
    mission outcome). Hiring pool quality is fed by the hiring group's own
    current morale — a demoralized agency gets worse recruit offers. **Tiger
    Teams** (pay to boost a specific step's saving-throw odds mid-mission,
    `baspm.md` §9.5) turned out to be a genuine mid-*mission-step* pause,
    correcting §2.5's "the roll sequence needs no player input at all" —
    it needs no input *between* steps, but this one junction does pause for
    a live choice.
  - **(b) What changes for async multiplayer:** mostly nothing structural —
    R&D assignment and permanent training are already standing orders (no
    per-turn re-declaration), and fixed-length training/mandatory rest
    seasons are simple countdowns; both fit "one turn = one season" (§1)
    cleanly with zero special handling, including under a `WEEKDAYS`/
    blackout schedule (§3.6), since morale/age only change on days a turn
    actually resolves, never on a skipped real day. **Tiger Teams is the one
    place this pass's finding actually forces a design choice, not just a
    default (user design input, 2026-09-27, see decision (c) below):** BASPM's
    own mechanic is a live per-step spending decision, which has no clean
    async equivalent short of asking the player to pre-declare a
    "what if a glitch happens" budget every turn a mission is active — doable
    (see (c)'s fallback), but exactly the kind of repetitive per-turn
    busywork this project has otherwise avoided (R&D/permanent-training are
    both "set once, no redeclaration" by design, immediately above). The
    "retirement/resignation only checked while idle" behavior is a design
    choice worth deciding on deliberately rather than inheriting by default —
    it could read as a fairness quirk (an offline player's idle-but-neglected
    staff drift towards leaving, while a busy roster is silently protected)
    once players start comparing notes across an async multiplayer game.
  - **(c) Decision:** **port the three-group/skill shape and the general
    morale/training/lifecycle mechanics** as Launch Window's starting point,
    re-deriving actual numbers rather than reusing BASPM's constants verbatim
    (`design.md` §1.2). **Decision: ** keep "standing orders, no
    per-turn re-declaration" for R&D-equivalent and permanent-training
    assignments, and keep retirement/resignation gated on being idle.
    **Superseded 2026-09-30 — see this same subsection's own later entry
    below ("Reversed (user, 2026-09-30)"): the "gated on being idle" *shape*
    of this decision was reversed once the personnel-lifecycle rules-engine
    slice actually existed to react to.** Left here rather than deleted so
    the history of how the decision evolved stays legible. **Decision:** keep the morale-gates-hiring-
    pool-quality feedback loop as a systemic mechanic, independent of
    whichever exact skillset/morale buckets Launch Window ends up using.
    - **Tiger Teams — decided to diverge from BASPM here (user design input,
      2026-09-27): promote it from a live per-step spending decision into a
      developed personnel capability**, rather than a pre-declared per-turn
      budget policy. An Astronaut or Mission Control employee (never SET -
      corrected 2026-10-02, see below; the original 2026-09-27 sketch said
      "SET or Mission Control," which doesn't match either BASPM's own
      control-room-flavored Tiger Teams or the user's later-confirmed
      eligibility) can earn an **"Expert"** trait (working name) once a
      skill crosses a threshold and/or via a dedicated advanced-training
      track, alongside their normal duties — reusing the exact standing-
      order shape `baspm.md` §2.1's R&D assignment and §9.3's permanent
      training already established, rather than inventing a new per-turn
      UI element. During turn resolution, a step needing a saving-throw
      rescue automatically draws on any currently-available Expert-flagged
      personnel; no live decision, no per-turn "what if" declaration
      required from the player at all.
      **Contention resolved (user, 2026-09-27): no hard cap on how many
      times an Expert gets drawn on in a single turn — instead, diminishing
      returns/exhaustion.** Each additional rescue the same Expert performs
      within one turn makes them progressively less effective (exact
      falloff curve TBD, likely echoing the diminishing-returns shape
      already established for training/skill growth, `baspm.md` §9.3, for
      consistency rather than inventing a new curve shape). This sidesteps
      needing any priority/queueing rule at all: an Expert is never
      *unavailable*, just increasingly less reliable the more a single turn
      leans on them — which also nicely discourages a player from treating
      Expert staff as a free unlimited safety net.
      **Decision:** No secondary fallback in the case no Expert
      personnel are available. players need to cultivate their own Experts to have a safety net, rather than relying on a pre-declared budget to buy one on demand.
      - **Fully decided and implemented (user, 2026-10-02), closing the
        "exact falloff curve TBD"/eligibility-group open ends above:**
        - **Eligibility:** Astronaut or Mission Control officer only,
          never SET (corrects the 2026-09-27 sketch's "SET or Mission
          Control" error). To even *start* training toward becoming an
          Expert in a skill, that skill must already be at or above
          **80** (out of `SkillGrowth.SKILL_CAP`) - a new headless
          `TrainingProgress.startAstronautExpertTraining`/
          `startMissionControlExpertTraining` action (same "plain method
          a future action handler will call" posture as `HiringPool.hire`/
          `MissionLaunch.launch`) rejects an id below the threshold,
          already Expert in that skill, not currently `IDLE`, or unknown.
        - **Training shape - a new `TrainingKind.EXPERT`, genuinely
          open-ended, unlike `BASIC`/`ADVANCED`/`PERMANENT`'s own fixed-
          or re-enrolling-fixed-length rounds:** no total season count at
          all - each season while `IN_TRAINING` with kind `EXPERT` rolls
          a fresh pass/fail chance; success ends training and grants the
          Expert trait for that skill immediately; failure keeps the
          employee training (unavailable for other tasks) into another
          season, with **no skill growth either way** - Expert status is
          a binary unlock, not a continuous value, so there is nothing
          for `SkillGrowth.diminishingReturnsGain` to apply here.
        - **Per-season success chance (user-specified formula, 2026-10-02):
          a flat 25% base, plus up to another 10% scaled by how far the
          employee's current skill sits above the 80 threshold** - `25 +
          10 × (skill − 80) ÷ (SkillGrowth.SKILL_CAP − 80)`, so a skill of
          exactly 80 gets no bonus at all and a skill at the cap gets the
          full +10%; re-derived as `SkillGrowth.expertTrainingSuccessChance`,
          a new pure formula, plus `drawExpertTrainingRoll`/
          `expertTrainingSucceeds` for the actual pass/fail check -
          mirroring `MissionStepResolution`'s own "pure formula, separate
          draw, separate resolve" split for direct testability. Since the
          targeted skill never grows during this training, the chance
          stays constant across every season of one attempt.
        - **Multi-skill accumulation, confirmed:** "only an Expert in a
          single skill" means per training *attempt*, not ever - an
          employee can hold the Expert trait in several skills over their
          career, one gained per successful training stint, each
          requiring its own separate 80-threshold stint for that skill.
          Stored as a new per-subclass `Set<AstronautSkill>`/
          `Set<MissionControlSkill> expertSkills` field (never on
          `SetSpecialist`), backed by a `LinkedHashSet` for the same
          Kryo-wire reason the existing skill maps use `LinkedHashMap`
          (see `Astronaut`/`MissionControlOfficer`'s own class Javadoc) -
          a new `LinkedHashSet` registration was needed in
          `MessageRegistry`, since nothing before this used a `Set` type
          over the wire at all.
        - **Permanence, confirmed:** once granted, the Expert trait is
          never lost by any mechanic built so far (no decay, no removal
          on reassignment/retraining).
        - **A new general "pull employee out of training" action**
          (`TrainingProgress.pullOutOfTraining`, dispatched by
          `PersonnelGroup` since it must internally read/write each
          subclass's own typed training fields) - applies to *any*
          `TrainingKind`, not an Expert-specific mechanic, since nothing
          in the project had this before at all (flagged as a known gap,
          `design.md` §3.10's own "no pull-out path exists yet" note).
          For `BASIC`/`ADVANCED`/`PERMANENT`'s current round, draws a
          fresh gain factor the same way a normal completion would and
          applies `SkillGrowth.partialCredit` against that round's own
          completed/total seasons, crediting the skill with a scaled-down
          gain rather than nothing; for `EXPERT`, credits nothing at all
          (there is no continuous skill value this training ever grows,
          so there is nothing to partially credit). Either way, resets to
          `IDLE` and clears every training field. **No event reported** -
          same posture as every other player action this project has
          built (`HiringPool.hire`/`ProgramOpening.open`/`MissionLaunch.launch`
          none report one either; only season-advance resolvers do).
          **Still open, not part of this decision:** what a player who
          hasn't logged in implicitly decides for a pull-out opportunity
          before a turn resolves - `design.md` §4/`baspm.md` §8.4's own
          still-open pre-resolution question, unaffected by this slice
          landing.
      - See §2.5(i) for how the Expert trait is actually *consumed*
        during mission-step resolution (the rescue mechanic itself) - a
        separate decision, made the same day, once the eligibility pool's
        scope (roster-wide, not mission-scoped) and the stacking/cap rules
        were settled.
      **Adopt regardless of the above, and generalized beyond Tiger Teams:**
      a post-turn **"replay"** of what an in-progress mission's automatic
      resolution actually did — `baspm.md` §8.3 already flagged BASPM's
      deterministic-RNG-replay mechanism as relevant prior art for exactly
      this. Since the player isn't present for anything a daily-batch turn
      resolves unattended, this is the general answer to that gap, not a
      Tiger-Teams-specific feature — the replay should show which rescue
      mechanic, if any, triggered and why.
      **Naming aside, low priority:** "Tiger Team" is real historical NASA/
      aerospace jargon that predates BASPM's use of it — per §1.2's carve-out
      for real-world facts (as opposed to BASPM's own specific expression of
      them), Launch Window is free to keep the term verbatim if it fits the
      tone; this isn't a case needing a reskin.
    - **Age/morale/retirement/resignation sub-slice implemented (2026-09-30,
      M2's first rules-engine slice, §5) — concrete numbers decided, all
      (unconfirmed defaults) pending real bot-harness tuning (§3.10), and all
      deliberately different from BASPM's own constants (`baspm.md` §9.2)
      per §1.2's no-verbatim-reuse rule, while keeping the identical
      mechanic shape:**
      - Idle: -2 morale/season. Busy (assigned or in training): +3 morale/
        season. Morale clamped to [0, 100].
      - Resign at or below 35 morale, while not protected from departure
        (see the reversed protection rule below). A 36-44 morale band is
        reserved for a future "disgruntled" season-news warning
        (`baspm.md` §9.2's precedent) — not yet consumed anywhere, no news/
        report system exists yet to read it.
      - Retirement age, while not protected from departure: Astronauts 50,
        Mission Control 60, **SET 65** (lowered from an initial 70 the same
        session, user decision, 2026-09-30 — "70 seemed a bit extreme") —
        same "astronauts age out earliest, by a wide margin" shape as
        BASPM's own 55/65/75, different absolute values.
      - Age itself still advances every season regardless of protection
        status — only the retirement/resignation *check* is gated. See §3.4
        for how age's own whole-year-vs-fractional representation was
        resolved.
      - Order of operations per employee: morale updates first, then age,
        then (only if not protected) resignation is checked against the
        just-updated morale, then (only if not resigned) retirement against
        the just-updated age.
      - **Departure-protection rule reversed from BASPM's own shape (user
        decision, 2026-09-30) — this is a shape change, not just a
        re-derived number, and deliberately not what the "keep retirement/
        resignation gated on being idle" decision above originally said:**
        BASPM protects any busy (non-idle) employee uniformly across all
        three groups (`baspm.md` §9.2). Launch Window now protects
        **per group** instead: **SET is never protected**, regardless of
        assignment state - the user's own reasoning: "SET personnel only
        ever work on mission components, so there is no reason not to
        retire them even when they are assigned to a project" (a departed
        SET employee's `assignmentStateAtExit`, new on `PersonnelDeparture`,
        gives the player a "was working on something, assign a replacement"
        hint - exactly *what* they were working on remains blocked on
        §3.4's still-open R&D-target-linkage note). **Astronaut and Mission
        Control are protected only while `ASSIGNED`** - critically, **no
        longer while `IN_TRAINING`**, which used to protect alongside
        `ASSIGNED` before this reversal - the user's own reasoning: this
        protection "should only ever apply to flight controllers and
        astronauts while they are participating in an active mission,"
        which training isn't. **Two interpretations, both confirmed by the
        user after review (2026-09-30), not left as unilateral judgment
        calls (see `PersonnelLifecycle`'s own Javadoc for the full
        reasoning):** (1) confirmed the rule extends to resignation as well
        as retirement, even though the user's original wording named
        retirement specifically. (2) Confirmed `AssignmentState.ASSIGNED`
        as the stand-in for "on an active mission" for Astronaut/Mission
        Control - and the user gave a reason the seemingly-open gap (a crew
        `ASSIGNED` to a mission that hasn't launched yet) likely isn't a
        real one: a mission is only ever `SCHEDULED` until the *next* turn
        evaluation, at which point it becomes `ACTIVE` - the same
        evaluation that would also run the first (and only per-turn)
        departure check. So there's no real multi-turn window where a crew
        sits merely assigned-but-not-flying while a check could fire on
        them. **Still worth a quick re-confirmation once mission
        simulation actually exists (slice 3, §2.5)** - this reasoning is
        about the *intended* scheduling shape, not yet verified against
        real running code.
      - Implemented in `core`'s new `de.mkoehler.launchwindow.rules`
        package (headless, §3.10) — `PersonnelLifecycle.advanceSeason`,
        with `PersonnelDeparture`/`DepartureReason`/`PersonnelGroup` as its
        reported events, and a `ResolutionEvent` marker interface meant to
        be the shared event vocabulary for every future rules-engine
        resolver (feeding the post-turn replay/season-news/Museum
        consumers this section and §3.13 already flag as wanting one).
        21 unit tests, fully headless — not yet wired into
        `PlayerStateService`/`TurnScheduler` (M4 scope, per §5).
      - **Skill growth, training-state transitions, hiring, and Expert are
        still fully deferred** — this sub-slice only ever touches age,
        morale, and roster membership, never a skill map.
    - **Skill-growth formulas sub-slice implemented (2026-09-30) — pure
      functions, first called by the training-progress sub-slice
      immediately below (mission-based growth's own caller is still
      later, slice 3's mission-simulation work):**
      - Skill values themselves switched from `Integer` to `Double`
        (`Astronaut`/`SetSpecialist`/`MissionControlOfficer`) — a flat gain
        under 1/season would otherwise round to nothing on an `int` scale.
        See §3.4.
      - `de.mkoehler.launchwindow.rules.SkillGrowth`: one shared
        diminishing-returns formula (`baspm.md` §9.3's shape, reused for
        both training and mission-based growth exactly as BASPM itself
        does) — `distanceToCap × factor × (learningPotential ÷ 100)`,
        skill cap **100** (BASPM: 98), training factor range **0.25-0.55**
        (BASPM: 0.3-0.6, all **unconfirmed defaults**, pending bot-harness
        tuning like §2.2(c)'s other numbers). SET's flat R&D gain — **its
        factor range 0.15-0.35** (BASPM: 0.2-0.4) — reuses the same
        "formula takes a pre-drawn factor, a thin wrapper draws it from an
        injected `RandomGenerator`" shape for API consistency, but unlike
        the diminishing-returns formula has no distance-to-cap term of its
        own (it's flat by definition), so it clamps against the skill cap
        explicitly — **a real bug caught before it shipped**: an unclamped
        flat gain under a standing R&D order would otherwise walk a skill
        straight past 100 after enough seasons, breaking any later formula
        that treats skill as a 0-100 percentage. Linear pull-out partial
        credit (`baspm.md` §9.3) is its own tiny function,
        `fullGain × completed ÷ total`, decoupled from the growth formula
        itself. 13 unit tests, fully headless, no dependency on any
        `Personnel` type — deciding *which* skill(s) a call touches
        (a single skill for R&D/most training, all five for a flying
        astronaut, a Flight Director's five-way-spread average per
        `baspm.md` §9.4) is deliberately left to each caller, not built
        here.
    - **Training-progress sub-slice implemented (2026-09-30) — fixed-length
      training only (`BASIC`/`ADVANCED`); `PERMANENT`'s re-enrollment and
      R&D's own target-skill/growth caller are still deferred, per §3.4's
      field proposal:**
      - New fields, exactly as §3.4 proposed: `TrainingKind` (`NONE`/
        `BASIC`/`ADVANCED`/`PERMANENT`) plus seasons-completed/total
        counters on `Personnel`; a per-subclass `trainingTargetSkill` field
        on `Astronaut`/`SetSpecialist`/`MissionControlOfficer`.
        `TrainingKind` registered with `MessageRegistry`, appended at the
        end per its own ordering rule; both `MessageRegistryTest` class
        lists and the Kryo/Jackson round-trip tests updated to cover it.
      - `de.mkoehler.launchwindow.rules.TrainingProgress.advanceSeason`:
        counts down `BASIC`/`ADVANCED` training, and on the season it
        completes, draws a training factor
        (`SkillGrowth.drawTrainingFactor`), applies
        `SkillGrowth.diminishingReturnsGain` to the target skill, resets
        every training field to its not-training default, and returns the
        employee to `IDLE` — reporting a `TrainingCompleted` event.
        Per-group fixed lengths, own numbers (BASPM: Astronaut 4/SET 2/
        MC 2 basic, 3 advanced for all): **Astronaut basic 3, SET basic 1,
        Mission Control basic 3, advanced 2 for all three** — all
        **unconfirmed defaults**, pending bot-harness tuning.
        **Corrected same session:** Mission Control basic training was
        first set to 2, landing on BASPM's own exact value for that group
        by coincidence — caught by the advisor, re-derived to 3 instead,
        since §1.2 forbids matching a BASPM tuning constant verbatim even
        unintentionally.
      - **What `BASIC` training's completion actually *does* is this
        session's own invented behavior, not something `baspm.md` §9.3
        specifies — flagged for the user, not decided unilaterally.**
        `baspm.md` §9.3's diminishing-returns gain formula is documented
        there for *advanced* training and mission-based growth; BASPM's own
        basic training is described as a one-time post-hire phase with its
        own separate failure exit (`FailedBasicTrainingEmployee`,
        `baspm.md` §9.2), not a repeatable skill-boosting round the way
        this implementation treats it (the same diminishing-returns gain,
        on one chosen target skill, re-runnable on veteran staff same as
        `ADVANCED`). **Decided (user, 2026-09-30): keep the current
        repeatable, same-shape-as-`ADVANCED` behavior for now, revisit once
        the hiring pool exists** — BASPM's own distinct one-time/pass-fail
        post-hire shape would need a real hiring-pool/new-hire concept to
        even trigger from, which isn't built yet; no code change from this
        decision, it confirms what was already implemented.
      - **Assumes an already-started training assignment** — nothing in
        this project yet *starts* one (that's a player-action concern,
        doesn't exist). Only ever advances a countdown/resolves completion.
      - **A fixed-length round is only genuinely active when
        `AssignmentState.IN_TRAINING` *and* a fixed-length `trainingKind`
        both agree** (`TrainingProgress.isActivelyTraining`) — the two
        fields are independent and could otherwise drift apart: a stale
        `trainingKind` left behind by some future pull-out path that only
        updates the assignment state would otherwise keep silently
        counting down and eventually "complete" a round the employee was
        actually pulled out of; conversely `IN_TRAINING` with no real
        `trainingKind` set (exactly what `PersonnelLifecycleTest`'s own
        busy-employee fixtures do, by design - a deliberately minimal
        fixture, not a real training assignment) must never be advanced
        either. **Caught before it shipped**, not found later — checking
        `trainingKind` alone was the first version.
      - **Composition-order contract for whoever wires this together with
        `PersonnelLifecycle` in one season pass (M4), decided, documented
        in both classes' Javadoc, and test-enforced:** run
        `PersonnelLifecycle` *before* `TrainingProgress`, not after.
        **Reason updated 2026-09-30 after §2.2(c)'s own departure-
        protection reversal below:** the order no longer changes *whether*
        a completing trainee is eligible for the resignation/retirement
        check (training stopped protecting Astronaut/Mission Control
        entirely) - it still changes the *morale* that check is judged
        against, since a training round completing this season flips the
        employee to `IDLE`. Running `PersonnelLifecycle` first judges that
        season against the assignment state the employee actually held for
        most of it (busy/training), not the one they only just switched to.
        Two tests prove this with a concrete number: an astronaut at
        morale 37, age 30 (well clear of retirement), in the final season
        of `ADVANCED` training - correct order applies the +3 busy gain
        (37→40) and stays; reversed order applies the −2 idle loss instead
        (37→35), landing exactly on the resignation floor and resigning.
        **Not guarded by the bot harness's own invariants** - its
        "protected never departs" check (see §3.10) only covers
        `AssignmentState.ASSIGNED` now, which `TrainingProgress` never
        sets, so a wrong composition order wouldn't trip it; these two unit
        tests are the only thing verifying the order still matters. The two
        resolvers still aren't composed together anywhere in real code at
        first — only the contract itself was verified by a test.
        **Composed for real later the same session**, in the bot harness
        only (still not `PlayerStateService`/`TurnScheduler`, that's M4) —
        see §3.10's own entry: the harness's bot grew a real "start a
        training assignment" decision specifically so this composition
        could be exercised, not left proven only in isolated unit tests.
      - 16 unit tests, fully headless (12 fixed-length ones, plus 4 for
        `PERMANENT`'s re-enrollment/termination-guard behavior once that
        landed later the same session — see this same §3.4 entry's own
        "Implemented" note for the full description).
    - **Idea for later, not decided (user, 2026-09-30): astronaut PR duty
      as a third season activity, earning prestige directly.** The
      motivating gap: in BASPM itself, an astronaut between missions has
      essentially one thing to do — train — and with `PERMANENT` training
      now a real standing order needing zero further input, that gap
      becomes a real "nothing left to decide" stretch for the player, not
      just a BASPM quirk being carried over. The user's proposed fix has
      real historical grounding, not just a gameplay patch: real astronauts
      genuinely served a public-relations/fundraising role for their
      programs (Mercury Seven press tours, magazine deals, parades) — the
      same "prestige funds the program" dynamic §2.1's budget/funding trace
      already established as historically real, not a game abstraction, now
      extended to a third lever beyond missions/goals for *earning* it.
      **Proposed shape (unconfirmed, needs its own design pass before
      implementation):** a third `AssignmentState`-adjacent activity
      alongside idle/training/mission-assigned — "PR duty" — that an
      otherwise-idle astronaut can be assigned to, yielding a small amount
      of prestige each season it continues, scaled by that astronaut's own
      `AstronautSkill.LEADERSHIP` (a real, already-modeled skill this
      would be the first personnel-facing use of beyond flying missions).
      Also floated: PR duty as a hook for bulletin/news-style events
      (`design.md` §2.7's random-events system, or the "replay"/season-news
      surface §2.2(c) already wants for other personnel events) - a
      standout PR season could generate its own flavor news item, not just
      a silent prestige tick. **Open questions this idea doesn't answer
      yet, deliberately not guessed at here:** exact prestige-per-season
      formula and how it scales with leadership; whether SET/Mission
      Control get an equivalent off-mission activity or this stays
      astronaut-specific (their own "boring gap" problem is different,
      since SET already has R&D as a standing background activity and
      Mission Control has no BASPM precedent for an idle-time duty at
      all); how this interacts with `PERMANENT` training as a competing
      choice for the same astronaut's time. Not scheduled into any M2
      slice yet - flagged here so it isn't lost, to be designed properly
      whenever personnel work resumes after the current M2 slices finish.
- 2.3 **Programs & mission configurations (the Sputnik→Apollo tech/content
  tree) — analyzed 2026-09-27, see `baspm.md` §10 for the full trace.**
  - **(a) How BASPM does it:** a Program Category (Robotic or Crewed group,
    gated behind prerequisite goals) contains Programs (their own
    opening/maintenance cost, required mission components, optional extra
    goal gate), each offering one or more Mission Configurations (duration
    in seasons, prestige granted/deducted, goals achieved — including a
    genuine "extended duration" variant that's longer, costlier-feeling, and
    pays out roughly double prestige, not just a cosmetic label). Opening a
    related program discounts a new one's opening cost by a flat,
    hand-authored percentage (e.g. Explorer I is 40% cheaper once Sputnik I
    is open) — a real "fast follower" mechanic. A genuinely notable finding:
    the code scaffolds **12 solar-system-target category groupings**
    (`baspm.md` §10.2) but the shipped game only ever fills in 13 of the
    ~40 category IDs those groupings reference — everything for asteroids/
    comets, the outer heliosphere, space stations, SSTO vehicles, and Mars
    expeditions is dead code, never-shipped scope.
  - **(b) What changes for async multiplayer:** nothing structural — this
    is entirely pre-turn player input (which program to open, which mission
    configuration to fly) resolved from state that already exists when the
    turn is queued; no mid-resolution surprises the way Tiger Teams turned
    out to have (§2.2(c), §4). Fits "declare intent before the turn, server
    resolves mechanically" cleanly.
  - **(c) Decision:** **port the four-level containment hierarchy and the
    opening-cost-discount-for-related-programs mechanic**, re-deriving
    actual cost/prestige numbers rather than reusing BASPM's constants
    verbatim (`design.md` §1.2). **Decision:** treat BASPM's
    never-shipped category scaffolding (asteroid belt, space stations, SSTO,
    Mars expeditions, outer heliosphere) as legitimate **long-term content
    roadmap inspiration** for Launch Window once the core Sputnik-through-
    Apollo content exists and is proven fun — these are real spaceflight
    domains, not BASPM's invented content, so using them as a roadmap isn't
    a copying concern, just good evidence of "what comes after the Moon"
    that the original's own architecture already anticipated.
- 2.4 **Mission components (rockets, capsules, boosters) & reliability —
  analyzed 2026-09-27, see `baspm.md` §10 for the full trace.**
  - **(a) How BASPM does it:** every mission component starts at a tiny
    ~3% baseline reliability regardless of type — genuinely unflyable until
    boosted. Two mechanisms boost it: **Reliability Transfer** (opening a
    related component grants a one-time starting bonus computed from
    already-owned related components' *current* reliability × an explicit,
    hand-authored per-pair transfer percentage, capped per target — "related"
    is a curated list, e.g. a real Redstone/Atlas/Titan/Saturn rocket-family
    web, not a generic same-skill-group rule) and ongoing use (R&D, §2.2, or
    actually flying missions, both diminishing-returns-shaped, consistently
    with §2.2(a) and §9.3/§10.4's training/skill-growth formulas). Post-mission
    reliability change is driven by a component's own per-use outcome
    (success/minor-glitch/failure/neutral, scaled by involvement for
    failures) against three global tuning ranges, not by any per-step XML
    field — a previous session's guess about specific per-step upgrade/
    downgrade XML attributes driving this turned out to be wrong (`baspm.md`
    §10.4 corrects it).
  - **(b) What changes for async multiplayer:** nothing structural, same as
    §2.3(b) — reliability transfer and R&D/mission-based growth are all
    either one-time (at program opening) or already-standing-order shaped
    (§2.2), fitting the daily-turn model with no special handling.
  - **(c) Decision:** **port the "everything starts nearly unflyable, transfer
    and use are how it becomes viable" shape** and the diminishing-returns
    reliability-growth curve, re-deriving actual percentages
    (`design.md` §1.2). **Decision:** keep reliability transfer
    as an explicit, curated per-pair relationship table (not a generic
    same-category rule) since that's what makes a real rocket-family lineage
    feel earned rather than automatic. **Implemented (2026-10-01, §5's M2
    status):** `MissionComponent`/`ProgramOpening` in
    `de.mkoehler.launchwindow.content`/`.rules`. **Decided (user,
    2026-10-01): the per-component reliability floor is the
    transfer-inclusive starting value, not the bare baseline** —
    `baspm.md` §10.3's own trace found this exact choice left ambiguous in
    BASPM's code (the field name implies the former, the code does the
    latter); a future mission-simulation slice's post-mission reliability
    change clamps against it. See §5's own entry for the full writeup.
- 2.5 **Mission simulation (the step-by-step launch/flight resolution,
  success/failure rolls) — analyzed 2026-09-27, see `baspm.md` §8 for the
  full formula trace.**
  - **(a) How BASPM does it:** a mission is a multi-season entity (commonly
    spans several real seasons already, e.g. a satellite's launch season
    plus several follow-on "keep operating it" seasons), and each season
    contributes its own ordered batch of steps. Each step resolves via up to
    two rolls — a base roll weighted by component-reliability/personnel-skill
    involvement percentages, then (only if that fails) a "saving throw"
    scaled by flight-controller skill that can fully rescue the step
    (`MINOR_GLITCH_SOLVED`) or downgrade the failure's severity. A
    `CATASTROPHIC`/`FAILURE` step **ends the mission immediately** — no
    further steps run that season or in any later season. Crucially, this
    entire step-by-step chain is a **fully automatic dice-and-consequence
    sequence once triggered — it needs no player input mid-sequence**; every
    genuine player decision point (scrub/launch/assign, per `baspm.md` §7.2)
    happens *before* a season's step batch runs, never interleaved with it.
  - **(b) What changes for async multiplayer:** almost nothing structurally
    — this is the rare case where BASPM's own model already fits the
    daily-batch shape directly rather than needing adaptation. "Resolve this
    mission's current season" is a clean, self-contained, non-interactive
    operation the server-side turn job can run once per in-progress mission
    per turn, with zero risk of needing to pause mid-resolution for a player
    click (§3.6/§4's mid-turn-decision problem turns out to be entirely a
    *pre*-resolution problem — which missions get scrubbed/launched/assigned
    before the job runs — not a mid-resolution one).
  - **(c) Decision:** **port the two-roll formula shape** (base
    involvement-weighted probability, then a flight-controller-scaled saving
    throw with catastrophic/failure/minor-glitch bands) as Launch Window's own
    starting mission-resolution mechanic, re-deriving the actual tuning
    numbers rather than reusing BASPM's `Constants.xml` values verbatim
    (`design.md` §1.2 — and we're free to retune balance for our own player
    counts/pacing regardless). **Decided (user, 2026-10-01): port the full
    five-tier outcome taxonomy, uniformly across every step** (not a
    per-step vital/optional split) - a clean success, a "rescued" success
    (saving throw fully catches it), a half-credited failure that lets the
    mission continue, a full mission-ending failure, and a crew-fatality-
    eligible worst tier (only reachable on steps flagged crew-risk) -
    re-deriving BASPM's own labels/bands per `baspm.md` §8.2's trace, with
    Launch Window's own tuning numbers. **This also settles `baspm.md`
    §8.4 item 1's question** (whether the half-credited "continues but
    degraded" tier is actually reachable): `baspm.md` §8.2's own
    code-confirmed trace already shows it is - a plain glitch grants half
    prestige and the mission continues - the user's own in-game play
    simply never happened to land that particular roll band, evidently a
    narrow one. Multi-season missions are adopted as-is: a mission
    genuinely taking several turns to play out (with escalating stakes
    each turn, per the Sputnik example in `baspm.md` §8.1) is good game
    feel to keep, not an artifact to engineer away.
  - **(d) Implementation (2026-10-01):** the pure formula half ships first,
    as `MissionStepResolution` (`core/.../rules`) plus a new
    `MissionStepOutcome` enum — the same "formula class before its caller"
    order `SkillGrowth` followed. It takes already-involvement-weighted
    contribution sums as plain `double`s (no `MissionComponent`/`Personnel`
    dependency), mirroring every other rules class here: a base probability
    from the component-reliability sum alone, optionally blended with the
    crew-skill sum (20% weight) once that base sum exceeds a 65 threshold
    and crew are actually involved — flight-controller skill plays no part
    in this base roll at all, it only scales a second saving-throw
    probability (base × controller-skill-sum ÷ 100) used only if the first
    roll fails. A failing saving-throw splits its own range into
    CATASTROPHIC (bottom slice, only reachable when the step is flagged
    crew-risk, else downgraded to FAILURE)/FAILURE/MINOR_GLITCH (top slice)
    bands. All four numbers (the 65 blend threshold, the 20% blend weight,
    and the two band-size fractions) are unconfirmed defaults, re-derived to
    values different from BASPM's own code-confirmed numbers (a 70
    threshold and 25% weight, from `ComputeProbabilityOfSuccessForMissionStep`
    itself, not `Constants.xml` — only the band-size fractions trace to
    `Constants.xml`, and `baspm.md` §8.2 never gives their exact values) per
    `design.md`
    §1.2 — concrete placeholders for the bot harness to tune later, not
    BASPM's numbers and not final balance. **The per-player in-progress-
    mission state and the resolver that walks a season's steps against it
    are done - see (f) below**, including the re-confirmation of
    `AssignmentState.ASSIGNED` this entry originally deferred alongside
    them (resolved mechanically, not just re-argued, once launch and
    season-0 resolution turned out to genuinely share one pass). **Still
    deferred to a later sub-slice:** post-mission reliability change
    (expected to reuse the same headroom-based diminishing-returns shape as
    `SkillGrowth.rdReliabilityGain`'s own headroom term, against the
    component's absolute `maxReliability` this time rather than
    `maxRdReliability` - but as a single random-factor draw against the
    whole component, not a multi-slot one: BASPM's own post-mission
    formula, `baspm.md` §10.4, has no slot concept at all, only R&D does,
    per §10.8), mission-based astronaut/controller skill growth, and **a
    post-flight SET-release mechanic parallel to R&D's own** (see R&D's
    entry above, `ResearchComponentMaxed`): `baspm.md` §10.4's
    `UpdatePlayersMissionComponentAfterMission` unassigns every SET
    researcher from a component whenever a mission flight leaves its
    reliability at or above `MaxRDReliability` - a state check, not a
    one-directional "pushed past it," so even a failed flight that drops
    reliability while it's still at/above that ceiling triggers it the
    same way - the exact same
    `UnassignAllSETPersonnelFromMissionComponent` call R&D's own release
    uses, just triggered by a flight outcome instead of an R&D season.
  - **(e) The step/season content shape (2026-10-01):** `MissionStep`
    (`core/.../content`) holds what a step needs to feed
    `MissionStepResolution`, nothing about *who* is assigned — three
    involvement lists (`componentInvolvements` as
    `(componentId, percent)`, `controllerInvolvements`/`crewInvolvements`
    as `(skill, percent)` against `MissionControlSkill`/`AstronautSkill`)
    plus the `crewRisk` flag. Skills, not specific personnel: which actual
    astronaut/controller fills a skill slot is `MissionLaunch`'s own job
    (§2.5(f)); how several crew sharing one skill combine into that
    skill's single contribution is the mean skill value across every
    assigned person, decided in §2.5(f) once that resolver existed —
    `baspm.md` §8.2's own trace ("summed per-skill-per-crew-member")
    confirms crew contributions are tallied per skill but did not itself
    settle this aggregation rule. `MissionSeason` is just `List<MissionStep> steps` (at least
    one); `MissionConfiguration` now holds `List<MissionSeason> seasons`
    (at least one) instead of a stored `durationInSeasons` — that accessor
    is now derived (`seasons.size()`), so the two can never disagree.
    **Unconfirmed defaults, written down since `baspm.md` doesn't settle
    any of them:** a step's `componentInvolvements` percentages must sum
    to exactly 100 (every step is always fully, jointly covered by its
    hardware — a weighted-average effective reliability, not a partial
    one). `crewInvolvements` is either empty (no crew on this step) or
    also sums to exactly 100 when non-empty — **not** "100 or less" as
    first written: `MissionStepResolution.computeBaseProbability`'s blend
    is a weighted average of the component sum and the crew sum, so a
    partial crew sum (e.g. one skill at 40%) would read as a skill
    shortfall rather than reduced crew presence, quietly pulling the
    blended probability *down* even when every involved crew member is
    maximally skilled — caught by the advisor before this landed, not
    found by testing. `controllerInvolvements` is the one list that
    legitimately stays "100 or less" (partial or absent controller
    participation is a meaningful content lever) — an empty list means
    the saving throw can never succeed at all, since
    `computeSavingThrowProbability` scales it by this same sum. Every
    individual involvement entry must also be 1-100, not 0-100 — a
    zero-percent entry is indistinguishable from "not involved at all,"
    so it is rejected outright rather than silently accepted as noise. A
    `crewRisk` step must have at least one crew involvement,
    since a worst-tier failure with nobody aboard to lose makes no sense.
    Step ids are unique within their own `MissionSeason` only (the same id
    may recur in a later season, e.g. a repeating "continue operating it"
    step across several extended seasons). `ContentCatalog` additionally
    requires every step's component ids both exist and belong to the
    step's own mission's program's `requiredMissionComponentIds` — no
    trace of BASPM missions reaching into another program's components,
    so this is enforced as a real rule, not left open. **Per-step prestige
    deferred to §2.6**, not added here — `MissionStepOutcome.MINOR_GLITCH`'s
    half-credit needs it, but prestige/goals is its own slice's concern; a
    caller resolver will need this field once it exists.
    **Blend shape confirmed (user, 2026-10-01):** keep the weighted
    average as implemented, rather than switching to an additive bonus
    that could only ever help. The trade-off was surfaced first - under
    this blend, involving crew *lowers* the step's success probability
    whenever the crew-skill sum is below the component sum (the ordinary
    case, since the blend only ever applies once the component sum
    already clears the 65 threshold), which sits a little oddly against
    `baspm.md` §8.2's "a bonus on top of already-solid hardware" wording -
    but the user chose to keep the simpler, already-implemented shape
    rather than rework it for a reading of that one phrase. Not expected
    to be revisited without a concrete balance reason to.
  - **(f) Per-player mission state, launch, and season resolution
    (2026-10-02):** `InProgressMission` (`core/.../model`, mutable, Kryo-
    registered) is the per-player runtime instance a `MissionConfiguration`
    produces once launched — `id` (a fresh instance id, since the same
    configuration can run more than once), `missionConfigurationId`,
    `currentSeasonIndex` (0-based, into `seasons()`), and the two assigned-
    personnel id lists (`assignedAstronautIds`/`assignedMissionControlOfficerIds`).
    `PlayerSnapshot` gains `inProgressMissions` (keyed by instance id, a
    `LinkedHashMap` for reproducible iteration order against the shared
    random source — same reasoning as `ContentCatalog.indexById`) and
    `nextMissionInstanceId`/`allocateMissionInstanceId()` (prefix `m`,
    same shape as `allocatePersonnelId()`'s `p` prefix).
    `Personnel` gains `assignedMissionId` (nullable) — for Astronaut/
    Mission Control, `AssignmentState.ASSIGNED` now means "assigned to
    this mission instance," the same way `SetSpecialist.rdTargetComponentId`
    already disambiguates SET's own `ASSIGNED`. **This also resolves
    §2.2(c)'s own standing re-confirmation note**, now that mission
    scheduling is real: launching a mission and resolving its first
    season's step batch happen in the same pass (see below), so there is
    never a turn boundary where a crew sits `ASSIGNED` without also
    already being active — exactly the reasoning the user accepted on
    trust back in §2.2(c), now mechanically true rather than just argued.
    - **`MissionLaunch.launch(snapshot, missionConfigurationId,
      astronautIds, controllerIds, catalog)`** (`core/.../rules`), a plain
      headless method a future action-dispatch system will call (M4, not
      this slice) — returns a `MissionLaunchOutcome` rather than throwing,
      the same `HireOutcome`/`OpenProgramOutcome` pattern. Validates the
      configuration and its program exist/are open, and that the supplied
      crew/controllers are each a known, currently-`IDLE` id with no
      duplicates. **Decided (user, 2026-10-02), superseding this slice's
      own first-draft shape: a mission's crew/controller headcount is
      declared on `MissionConfiguration` itself
      (`requiredAstronautCount`/`requiredMissionControlOfficerCount`), not
      inferred from its content.** The original design scanned every step
      of every season for a non-empty `crewInvolvements`/`controllerInvolvements`
      to decide whether a mission needs crew/controllers *at all*; the
      user's own framing was stronger - a real mission states exactly how
      many astronauts/controllers it needs, and a launch must supply
      exactly that many, not "at least one" or "any number." Which skills
      they need is still expressed by each `MissionStep`'s own involvement
      lists, unchanged - the headcount is a requirement layered on top,
      not a replacement; it does not assign a specific seat to a specific
      skill (a possible stricter reading the user's own wording also
      suggested - "how many astronauts and flight controllers *with which
      skills*" - raised but not adopted, since the involvement-list
      skills already answer "which skills," just not seat-by-seat; worth
      revisiting if per-seat skill assignment ever becomes its own
      design need). A launch supplying any count other than the exact
      required one is rejected (`MissionLaunchOutcome.WRONG_ASTRONAUT_COUNT`/
      `WRONG_MISSION_CONTROL_OFFICER_COUNT`), which still closes the same
      departure-protection exploit the content-derived shape existed to
      close in the first place: a configuration requiring 0 astronauts can
      never have one assigned to it, so parking an idle astronaut on an
      uncrewed mission just to shield them from resignation/retirement
      checks is still impossible. On success, creates the
      `InProgressMission`, sets every assigned id's
      `AssignmentState`/`assignedMissionId`, and stores it.
    - **`MissionProgress.advanceSeason(snapshot, catalog, random)`**
      (`core/.../rules`), the resolver that actually walks one season's
      step batch for every in-progress mission. For each step: the
      component contribution sum weights each `effectiveReliability`
      (modifier fixed at 0 for now — the §11.3 goal-dependency bonus/
      penalty this parameter exists for has no caller until §2.6/§2.9
      exist) by involvement percentage, exactly as `MissionStepResolution`
      expects. **Decided (user, 2026-10-02): the crew/controller
      contribution for a given skill is the *average* skill value across
      every assigned crew/controller**, not a sum — a sum would push the
      weighted-average blend's inputs past the 0-100 scale §2.5(e)'s own
      fix depends on, rewarding crew size rather than crew quality; average
      keeps both contributions on the same 0-100 scale regardless of how
      many people are assigned. Each step's outcome is reported as a new
      `MissionStepResolved` event. **On `FAILURE`/`CATASTROPHIC`, the
      mission ends immediately** (no further steps that season or later),
      consistent with §2.5(a)'s original trace. **Decided (user,
      2026-10-02): a `CATASTROPHIC` outcome permanently loses the
      *entire* assigned crew**, not one random member — matching real
      catastrophic accidents with no partial survival (Apollo 1, Challenger,
      Columbia; `baspm.md`'s own `Player.AddFallenAstronaut` mechanism
      confirms BASPM permanently removes a named astronaut on this outcome,
      though the exact "who/how many" rule is not yet traced against the
      decompiled source - see `baspm.md` §8.4 item 6 for that follow-up).
      Each lost astronaut is
      reported as a `PersonnelDeparture` with a new `DepartureReason.LOST_IN_ACCIDENT`
      — reusing the existing event/record rather than inventing a parallel
      one, since every field it already carries (age/morale/assignment
      state at exit) applies unchanged. Mission control officers are never
      crew and are not at risk; on any mission end (success, `FAILURE`, or
      `CATASTROPHIC`), every surviving assigned person (controllers always,
      astronauts unless just lost) is released: back to `IDLE` with
      `assignedMissionId` cleared, or retired on the spot instead - see
      the retire-on-release decision below. **A successfully completed
      mission** (every season's step batch resolved without a mission-
      ending outcome, `currentSeasonIndex` reaching `durationInSeasons`)
      releases its crew the same way; prestige for a completed mission is
      §2.6's job, not this resolver's — not yet implemented.
    - **Decided (user, 2026-10-02): releasing a crew/controller who has
      already reached their group's retirement age retires them on the
      spot, with a `PersonnelDeparture`/`DepartureReason.RETIRED` event,
      instead of returning them to `IDLE`.** This closes a loophole
      harness-wiring this slice surfaced (§3.10): launching is free and
      runs before `PersonnelLifecycle` each season, so without this check
      an over-age employee released one season could be relaunched the
      very next, every season, and never sit `IDLE` long enough for
      `PersonnelLifecycle`'s own retirement check to ever catch them -
      structurally different from the training-based "immortality
      loophole" §2.2(c) already closed, since that one was about a
      protection *rule*, not a scheduling *gap*. Applies to both
      Astronaut and Mission Control (both are shielded from departure
      while `ASSIGNED`, so both have the same loophole), using a new
      package-private `PersonnelLifecycle.retirementAgeFor(PersonnelGroup)`
      helper shared by both resolvers. The user's own stated reasoning
      doubled as a concrete UX idea for later, not implemented now - a
      news-bulletin beat ("after a successful/failed mission, fellow
      astronaut John Doe decided enough is enough and retired...") -
      deferred since no news/bulletin system exists yet; the event stream
      this already produces (`MissionStepResolved` plus the `RETIRED`
      departure, in order) carries everything such a bulletin would need
      once one does.
    - **Composition order**: `MissionLaunch.launch` is a journaled player
      action, so it runs in the same "before any resolver" bucket as
      `ProgramOpening.open`/`HiringPool.hire`. `MissionProgress.advanceSeason`
      runs right after `PersonnelLifecycle.advanceSeason` (so a crew
      lost/released this season is reflected before `TrainingProgress`/
      `ResearchProgress` run against the same snapshot) and before both of
      those and `HiringPool.refreshCandidates` — the full order is now
      `PersonnelLifecycle` → `MissionProgress` → `TrainingProgress` →
      `ResearchProgress` → `HiringPool.refreshCandidates`.
    - **Deferred, same posture as §2.5(d)/(e)'s own lists:** prestige/goals
      (§2.6) and the §11.3 goal-dependency reliability modifier (both
      blocked on the budget/prestige/goals slice, §5). Post-mission
      reliability change and mission-based skill growth are done - see
      (g) below; mission cost at launch is done - see (h) below.
      **The Expert trait is done, not just no longer blocked** -
      `baspm.md` §9.5 fully traced Tiger Teams the same day this list was
      first written (formula and constants both code-confirmed), and the
      user then made every design decision needed to implement the
      mission-side rescue mechanic the same day (2026-10-02) - see (i)
      below for the concrete shape and the harness numbers.
      **Two balance holes harness-wiring this surfaced (§3.10) are both
      resolved, same day, not left open:** the crew-size question became
      the exact-headcount decision above (`requiredAstronautCount`/
      `requiredMissionControlOfficerCount`); the free-relaunch retirement
      loophole became the retire-on-release decision above.
  - **(g) Post-mission reliability change and mission-based skill growth
    (2026-10-02):** both happen once per in-progress mission *per
    season*, right after that season's step batch resolves, regardless
    of outcome - `baspm.md` §10.4/§9.3 both describe their BASPM
    counterparts as firing "on both the success and the failure path,"
    once per mission; Launch Window's own turn-based model naturally maps
    that onto "once per season a mission actually flies," not once for an
    entire multi-season mission, since a season boundary is already where
    this resolver's own state genuinely advances.
    - **Decided (user, 2026-10-02): reliability change aggregates per
      component across the whole season, not per individual step.** The
      alternative (a separate reliability nudge after every single step)
      was considered and rejected: a season with an early successful step
      and a later step that fails and ends the mission would otherwise
      apply a positive nudge and then a negative one to the same
      component in the same season, which has no equivalent in BASPM's
      own once-per-mission granularity. For each component involved in
      any step the season actually executed (the steps up to and
      including a mission-ending `FAILURE`/`CATASTROPHIC`, if any), the
      worst outcome category it was involved in that season wins:
      `FAILURE` (the step resolved `FAILURE`/`CATASTROPHIC` - necessarily
      at most one such step per season, since the first one ends the
      season) beats `MINOR_GLITCH` (unsolved) beats `SUCCESS`
      (`SUCCESSFUL`/`MINOR_GLITCH_SOLVED` only). **Two mapping decisions,
      both unconfirmed defaults, not traced:** `MINOR_GLITCH_SOLVED` maps
      to the `SUCCESS` category (matching its own full-credit prestige
      semantics); `CATASTROPHIC` maps to the same `FAILURE` category as
      plain `FAILURE` rather than getting a fourth, harsher category of
      its own - `baspm.md` §10.4 only traces a four-way taxonomy
      (`SUCCESS`/`MINOR_GLITCH`/`FAILURE`/`NEUTRAL`) with no fifth tier
      for a worse-than-`FAILURE` case, and inventing one isn't backed by
      anything - just another number to tune later if it turns out to
      matter.
    - **Formula, re-deriving `baspm.md` §10.4's own shape**
      (`SkillGrowth.missionReliabilityGain`/`missionReliabilityLoss`):
      `SUCCESS`/`MINOR_GLITCH` both grow toward
      `MissionComponent.maxReliability()` the same diminishing-returns way
      `rdReliabilityGain` grows toward `maxRdReliability` - `(max −
      current) × factor`, `MINOR_GLITCH`'s own factor range lower than
      `SUCCESS`'s (0.02-0.06 vs. 0.05-0.15 - a glitch is meant as a
      weaker vote of confidence than a clean run), though the two ranges
      overlap slightly rather than being strictly separated, so a glitch
      can occasionally out-gain a clean success at the extremes - worth
      revisiting once real balance tuning starts, same posture as every
      other unconfirmed-default range here. `FAILURE` shrinks the *other*
      direction,
      toward `OwnedMissionComponent.getFloorReliability()` rather than 0 -
      `(current − floor) × factor`, additionally scaled by the failing
      step's own involvement percentage for that component (per
      `baspm.md`'s own "scaled down by how much that component was
      actually involved in the failure"; `SUCCESS`/`MINOR_GLITCH` are
      **not** involvement-scaled, matching the trace, which only
      describes the failure case that way). All three factor ranges are
      unconfirmed defaults, re-derived to Launch Window's own numbers per
      §1.2. **Reaching `maxRdReliability` this way releases any SET
      researchers still assigned to that component**, exactly like
      §10.8's own R&D-side release - re-derived as a new shared
      `ResearchProgress.releaseResearchersIfMaxed` helper both this
      resolver and `ResearchProgress`'s own pass call, rather than
      duplicating the release logic, since `baspm.md` traces both
      triggers calling the identical `UnassignAllSETPersonnelFromMissionComponent`.
    - **Mission-based skill growth, re-deriving `baspm.md` §9.3/§9.4's own
      shape** (`SkillGrowth.diminishingReturnsGain`, the same formula
      training uses): every astronaut still on the mission at the end of
      the season grows **all five** `AstronautSkill` values; every
      Mission Control officer still on it grows every skill that
      appeared in *any* step the season actually executed -
      **deliberately broader than BASPM's own "the one skill they were
      actually using,"** since Launch Window's own per-skill averaging
      model (§2.5(f)) has no concept of one controller personally owning
      one skill the way BASPM's per-person assignment does; growing every
      skill genuinely exercised that season is the natural equivalent.
      **BASPM's Flight-Director-specific spread-the-average variant is
      deferred, not adopted:** Launch Window has no "who is Flight
      Director for this mission" concept at all yet -
      `MissionControlOfficer.flightDirectorSkill()` is a computed stat,
      never an assigned role - so there is nothing to key this variant
      off of without first deciding whether Launch Window wants a
      designate-a-Flight-Director mechanic at all, a real design question
      of its own, not decided here.
    - **Ordering: growth and reliability change run before
      `releaseSurvivors`'s own retire-on-release check, so a crew
      member's final season still counts for both** - they flew the
      season, then (if applicable) retired afterward; a `CATASTROPHIC`
      outcome's lost crew never grows at all, since `loseCrew` already
      removed them from the roster first. Draw order (for the shared
      random source, per-mission, in `inProgressMissions`' own map
      order): a season's steps first (unchanged from §2.5(f)), then
      reliability changes (components in first-involved-this-season
      order), then skill growth (astronauts in `assignedAstronautIds`
      order, then controllers in `assignedMissionControlOfficerIds`
      order).
    - **Harness-wired the same day:** since
      `PersonnelLifecyclePlayouts` already calls
      `MissionProgress.advanceSeason` (§3.10's "mission simulation wired
      in" entry), both mechanics ran automatically the moment they
      existed — no new bot decision was needed. What *was* added: five
      new report fields (`missionReliabilityChangeCount`/
      `averageMissionReliabilityDelta`, `missionSkillGrowthCount`/
      `averageMissionSkillGrowth`, `missionTriggeredSetReleaseCount` —
      the last one specifically a mission-triggered
      `ResearchProgress.releaseResearchersIfMaxed` call, kept separate
      from `ResearchProgress`'s own R&D-pass release, which still isn't
      counted at all); a new `checkMissionInvariants` check that every
      `MissionSkillGrowthApplied` event's own gain is non-negative
      (`MissionReliabilityChanged`/`ResearchComponentMaxed`'s own bounds
      were already covered every season by `checkProgramInvariants`/
      `checkResearchInvariants`, so nothing new was needed there); and
      two new `PersonnelLifecyclePlayoutsTest` tests proving both
      mechanics, and specifically the mission-triggered SET-release path,
      are actually reachable under the fixture's own numbers, not just
      theoretically wired in.
      **Numbers (same 200-game/80-season/seed-42 run as §3.10's "second
      pass," now with both mechanics live): 1,053 resigned/1,037
      retired/5,344 still employed/84,592 training completions/4,516
      hires/hiring bands 0/1,994/33,131/12,875 poor-average-good-
      excellent/598 programs opened/36,131 R&D seasons; 2,103 missions
      launched, mission steps resolved 1,725 successful/403 minor-
      glitch-solved/422 minor glitch/604 failure/41 catastrophic, 82
      astronauts lost in accidents, 24 retired on mission release.**
      Personnel/hiring/program totals are within a few percent of the
      second-pass baseline, consistent with the random stream shifting
      (the new post-season draws consume additional values every mission
      season). **The mission-step mix also moved toward success (50.4%,
      1,610/3,192 second pass, &rarr; 54.0%, 1,725/3,195), with every
      unsuccessful category falling (minor glitch 475&rarr;422, failure
      650&rarr;604) despite the total step count staying effectively
      flat - the direction the new feedback loop predicts** (post-mission
      reliability gain raising each component's own base probability,
      mission-based skill growth raising the crew-blend/saving-throw
      inputs, both now feeding back into later step rolls, something the
      harness had no mechanism to do before this slice). **Single seed,
      not checked against run-to-run variance** - steps aren't
      independent draws (reliability/skill state persists within a game,
      so outcomes cluster per game, making the effective sample size much
      closer to 200 games than to 3,195 steps), so this is a plausible,
      not confirmed, causal read; a few more seeds would be needed to
      treat the shift itself as established rather than directional.
      R&D seasons completed fell slightly too (36,313&rarr;36,131, about
      0.5% - noise-level, not asserted as caused by anything here).
      **The two new mechanics themselves: 3,903 reliability
      changes (avg. delta +1.24, i.e. gains outweigh losses overall —
      expected, since `SUCCESSFUL`/`MINOR_GLITCH_SOLVED` together far
      outnumber `FAILURE`/`CATASTROPHIC` steps above), 5,567 skill-growth
      events (avg. total gain 5.29 across whichever skills grew that
      season), and 152 SET specialists released specifically via the new
      mission-triggered R&D-ceiling path** (one `ResearchComponentMaxed`
      event can release up to four specialists at once, since a
      component has four R&D slots; distinct from and not counted in
      `ResearchProgress`'s own R&D-pass release count) — confirms the
      mission-triggered release path, the one genuinely new mechanic with
      no prior analog in the harness, is reachable at this scale, not
      just theoretically implemented.
  - **(h) Mission cost at launch (2026-10-02):** `baspm.md` &sect;9.5's
    Tiger-Teams trace confirms BASPM's own code references a
    `missionCost` value (used as the base its Tiger-Teams cost-scaling
    formula multiplies against) - real evidence a per-mission cost is
    genuine BASPM content, not invented, but that one reference is all
    &sect;9.5 traces; when/how BASPM's own code deducts it was not traced
    there. **User-confirmed from in-game play (2026-10-02, `baspm.md`
    &sect;9.5's own added note): deducted in full at the moment the
    player decides to launch, before the server evaluates the next turn,
    and never refunded on a later scrub** - "charge the full cost once,
    at launch, no refund" is therefore a confirmed match to BASPM's own
    behavior, not an unconfirmed default anymore. `MissionConfiguration`
    gained a new `cost` field - a one-time funds cost `MissionLaunch.launch`
    deducts up front, checked right after the program-open check and
    before any crew/controller validation (same ordering `ProgramOpening.open`
    already uses for its own funds check). Rejects with a new
    `MissionLaunchOutcome.INSUFFICIENT_FUNDS` when the player's current
    funds are below the configuration's own cost, mutating nothing, same
    "fail before any mutation" contract every other `MissionLaunch`
    rejection already upholds. **No refund on `FAILURE`/`CATASTROPHIC`
    either** - confirmed by the same user statement above, not just
    matching `Program.openingCost()`'s own (still unconfirmed) non-refund
    behavior by analogy. Deliberately simple: a flat one-time cost, not the
    recurring per-season upkeep §2.1's budget slice still owns, and
    distinct from `Program.openingCost()`/`maintenanceCost()`, which
    price owning the program itself rather than flying one of its
    missions. 45 existing `MissionConfiguration`
    constructor call sites across 5 files needed the new argument added
    (mechanical, same kind of change &sect;3.4's R&D two-ceiling upgrade
    needed) - all defaulted to `0` (free) except where a test specifically
    exercises cost. 5 new tests: two in `MissionConfigurationTest`
    (rejects negative cost, accepts zero), three in `MissionLaunchTest`
    (deducts the exact cost on success, rejects when funds are below
    cost with nothing mutated, accepts when funds exactly match cost -
    the boundary case a strict-inequality bug could get wrong).
    **Harness-wired the same day:** the three fixture configurations
    each declare a small launch cost (50/75/150, ascending with crew/
    duration, deliberately small next to the programs' own 500/600/1,200
    opening costs - not balance-tuned numbers); `maybeLaunchMission`
    already treats any non-`LAUNCHED` outcome as an ordinary no-op, so
    `INSUFFICIENT_FUNDS` needed no new harness code, only a Javadoc
    update. **Confirmed not meaningfully exercised by this harness**
    (same situation `ProgramOpening`'s own `INSUFFICIENT_FUNDS` is in,
    §3.10's own note) - the harness's full numbers run is bit-for-bit
    identical to the pre-cost run (a funds deduction never consumes a
    random draw, which explains why a successful launch changes
    nothing downstream), **and `missionsLaunchedCount` itself didn't
    drop either, confirming `INSUFFICIENT_FUNDS` never actually fired in
    this 200-game run** - this harness has no income mechanic at all, so
    funds only ever fall, but the fixture's modest costs against 5,000
    starting funds never drove any game's funds low enough to matter
    within 80 seasons. The dedicated `MissionLaunchTest` cases above are
    what actually exercise `INSUFFICIENT_FUNDS`, not the harness.
  - **(i) Expert rescue during mission-step resolution (2026-10-02),
    consuming §2.2(c)'s Expert trait:** this is the mission-side half of
    Tiger Teams' redesign - §2.2(c) covers how an employee *becomes* an
    Expert; this covers how an existing Expert actually *rescues* a
    mission step.
    - **Where the boost applies - both skill types feed the same
      saving-throw roll (user decision, 2026-10-02):** `baspm.md` §9.5's
      own Tiger Teams only ever boosted the saving throw, and only ever
      via flight-controller-adjacent personnel (the control-room setting
      the mechanic is themed around) - `MissionStepResolution`'s own
      formula mirrors that split today, crew skill feeding only the base
      probability (`computeBaseProbability`), controller skill feeding
      only the saving throw (`computeSavingThrowProbability`), with no
      crew term in the saving throw at all. Since the user wants
      Astronauts Expert-eligible too, "the skill needed to fix the
      glitch" is read as whichever skill (crew or controller) the
      glitching step actually involves - an Astronaut-Expert and a
      Mission-Control-Expert both boost the *same* saving-throw
      probability, if their own Expert skill matches that step's
      `crewInvolvements`/`controllerInvolvements`. This gives crew skill
      a real mechanical role in glitch recovery it never had before -
      a deliberate departure from BASPM's own controller-only shape, not
      an oversight. **Real-world grounding (`design.md` &sect;1.1):**
      Apollo 13's own command-module power-up procedure - devised chiefly
      by EECOM flight controller John Aaron and the ground team he worked
      with, then tested and refined in the simulator by Ken Mattingly
      (bumped from the prime crew by measles exposure, grounded for the
      whole mission) before it was ever read up to the crew - is real
      precedent for a flight controller and a non-flying astronaut
      rescuing an active mission together, informing the pool-scope
      decision directly below.
    - **Eligibility pool - roster-wide, not mission-scoped (user
      decision, 2026-10-02):** §2.2(c)'s own 2026-09-27 wording ("any
      currently-available Expert-flagged personnel... an Expert is never
      *unavailable*") already implied this, re-confirmed explicitly once
      asked. An eligible Expert need not be assigned to the glitching
      mission at all - idle, training (including training toward a
      *different* Expert skill), or assigned to another mission can all
      be drawn on. **One asymmetric exclusion, reasoned from physical
      presence:** an Astronaut currently `ASSIGNED` to a *different*
      active mission is excluded entirely (0%) - they are physically
      aboard that other spacecraft, not available at all. A Mission
      Control officer assigned to a *different* active mission is never
      excluded, but their own contribution scales to **75%** (user's own
      number) - still on Earth, just not fully dedicated. Every other
      state (idle, training, assigned to *this* mission) contributes at
      full strength for either group. Matches by simple overlap - any
      shared skill between the Expert's own `expertSkills` and the
      step's involved skills qualifies them, regardless of involvement
      weight (unconfirmed default, a sub-detail not worth its own
      question). Each eligible person is consulted **at most once per
      step**, even if they hold the Expert trait in several skills that
      all happen to match - one person, one rescue attempt, not one per
      matching skill (also an unconfirmed default).
    - **Stacking with exhaustion, confirmed (user, 2026-10-02): multiple
      Experts' effects stack, with diminishing returns, but success must
      never become guaranteed - some failure chance must always
      remain.** Re-deriving `baspm.md` §9.5's own single-use formula
      (`boost = percentageBoost × (100 − currentProbability)`) and
      applying it *sequentially*, once per eligible Expert, each against
      the *already-boosted* probability the previous Expert left behind -
      a new `MissionStepResolution.applyExpertBoost(currentProbability,
      boostPercent)` pure function. This structure inherently satisfies
      "never guaranteed" on its own: since every `boostPercent` used is
      strictly below 100%, the remaining gap to 100% shrinks
      geometrically but mathematically never reaches exactly zero, no
      matter how many Experts stack - confirmed algebraically, not just
      assumed. **Per-person exhaustion (user-confirmed numbers): the
      first time a given employee is drawn on in one turn closes 50% of
      the remaining gap; the second time, 40%; the third, 10%; the
      fourth and beyond, 0%** (negligible, matching "not to a degree
      where success is guaranteed" from the other side too - an
      over-exhausted Expert simply stops helping rather than ever
      hurting) - a new `MissionStepResolution.expertRescueBoostPercentForUse`
      lookup. Exhaustion counts uses, not successes - an Expert consulted
      for a step that still ultimately fails despite their boost is just
      as "used" as one whose rescue worked, matching the plain reading
      of "drawn on" (unconfirmed default). **Scope: per employee, per
      `MissionProgress.advanceSeason` call** ("the same turn
      evaluation," reset fresh every call, not persisted on the
      snapshot) - a fresh `Map<personnelId, useCount>` threaded through
      every mission/step this call processes, missions walked in
      `inProgressMissions`' own map order, personnel consulted in
      roster order (astronauts before controllers) for determinism.
    - **An explicit 95% cap, confirmed (user, 2026-10-02) - the
      asymptote alone was judged insufficient**, since with a large
      enough roster the remaining failure chance can shrink close enough
      to zero to feel guaranteed in practice, not just mathematically
      nonzero. `final = min(boosted, max(unboosted, 95))` - never lets
      Expert stacking push the saving-throw probability above 95%, but
      also never *lowers* an already-higher unboosted probability down
      to 95% when no Experts were involved at all (a step whose natural
      odds already exceed 95% stays at its own natural value, uncapped -
      the cap only ever constrains what *Experts* can add, never the
      formula's own baseline result). 95 is this project's own
      unconfirmed default, not traced from BASPM (§9.5 has no equivalent
      cap at all - BASPM's own tiered Tiger Teams boost maxes out at 50%
      of the remaining gap in a single use, structurally far short of
      ever needing one).
    - **No new randomness consumed:** every Expert-rescue computation
      above (eligibility, stacking, exhaustion, the cap) is fully
      deterministic given roster state and this turn's use counts so
      far - only the pre-existing first-roll/saving-roll draws
      `MissionStepResolution` already made are still drawn.
      `resolveStep`'s own flow needed restructuring to make this
      possible: it used to call `MissionStepResolution.resolve`'s auto-
      drawing overload directly, which draws (and may skip) the saving
      roll internally; it now draws the first roll itself via a newly
      package-private `MissionStepResolution.drawRoll`, checks it via a
      new package-private `firstRollSucceeded`, and - only if a saving
      throw is actually needed - computes and Expert-boosts the saving-
      throw probability *before* drawing the saving roll, then calls the
      already-existing fully-deterministic `resolve` overload with both
      explicit rolls. This also fixes the exhaustion-counting
      requirement: an Expert is only ever marked "used" when a step
      genuinely needed rescuing (the first roll already failed), never
      merely for being eligible.
    - **New event, `ExpertRescueApplied`:** one per eligible Expert
      actually consulted for a step needing a saving throw (mirroring
      `MissionReliabilityChanged`/`MissionSkillGrowthApplied`'s own
      per-application shape) - reports the mission/step, the person, the
      matched skill, their own boost percent this use (already scaled
      for off-mission controllers), and the probability before/after
      their own contribution. **Caveat: `probabilityAfter` never reflects
      the 95% cap** - `capExpertRescue` runs once, after every Expert's
      own event for a step has already been emitted, so a sequence of
      these events can together show a probability above 95% even though
      the roll that actually decided the step used the capped value;
      worth remembering if a later "what actually happened" replay (the
      post-turn replay idea &sect;2.2(c) floats) is ever built straight
      from this event stream. **Caveat: eligibility depends on mission
      processing order within one turn** - missions are walked in
      `inProgressMissions`' own map order, so a crew member released
      earlier in the same turn becomes an eligible roster-wide Expert for
      a mission processed later in that same turn, while one lost earlier
      does not; deterministic, but order-dependent, not something a
      player could predict without knowing that internal order.
- 2.6 **Prestige, goals & scoring — analyzed 2026-09-27, see `baspm.md` §11
  for the full trace.**
  - **(a) How BASPM does it:** goals are `Generic` (universal race
    milestones, the minority — 34 of 207 in the base game) vs. `Non-generic`
    (agency-specific flavor content, the bulk). The generic/non-generic
    split is mechanically real, not cosmetic: only generic goals can gate a
    program's opening, and only generic goals ever appear in the
    opposing-faction comparison screen (non-generic goals are structurally
    excluded, since a rival agency can't achieve one declared specific to
    yours). Achieving a goal can recursively cancel/obsolete lower-tier
    goals (`GoalsCancelled`). **The manual's "skipping a prerequisite goal
    imposes a reliability penalty" claim resolved to a specific, third
    reliability-modifying mechanism** (distinct from Reliability Transfer,
    §2.4, and post-mission reliability growth): a mission configuration can
    declare goal-gated per-component reliability penalties (goal not yet
    achieved) or bonuses (goal already achieved), computed fresh per mission
    attempt and fed straight into §2.5's mission-sim formula — not
    persisted onto the component, and not a program-opening gate either.
  - **(b) What changes for async multiplayer:** nothing structural — goal
    achievement/cancellation and the per-attempt reliability modifier are
    both deterministic outcomes of state that exists when a turn is queued,
    no mid-resolution surprises (`baspm.md` §11.5). The one place this
    system is *already* concrete multiplayer precedent, not just adaptable
    single-player content: BASPM's own opposing-faction goal comparison
    (§2.9's brainstorm already cites this) lives bundled into the same
    budget/prestige overview screen the player already checks every turn
    (`baspm.md` §11.4), not a separate dedicated screen — worth carrying
    that placement decision into Launch Window's own UI rather than
    burying a "how's my rival doing" view somewhere less visited.
  - **(c) Decision:** **port the generic/non-generic goal split and its two
    concrete mechanical consequences** (program-gating eligibility,
    comparison-screen visibility) as Launch Window's starting shape,
    re-deriving Launch Window's own actual goal list/prestige values rather
    than reusing BASPM's (`design.md` §1.2). **Decision:** port
    the goal-gated reliability-penalty/bonus mechanic too, as a genuine
    risk/reward lever distinct from reliability transfer — Launch Window surfaces this modifier explicitly in its own UI
    (baspm.md §11.6 flags whether BASPM even does this as still unverified)
    rather than leaving it as an invisible odds adjustment, since
    async-multiplayer players benefit more from a legible "why did this
    mission's odds look different" explanation than an offline player would.
    **Scrapped for v1 (user, 2026-10-01):** a tiered gold/silver/bronze
    goal-achievement rating was floated (2026-09-27, originally following
    from §2.5(c)'s "vital vs. optional step" idea - e.g. gold for
    completing a mission without failing any optional step, silver for
    one such failure, bronze for more) as a Launch Window-original quality
    dimension beyond BASPM's plain achieved-or-not. Its own trigger
    stopped existing once §2.5(c) settled on BASPM's uniform five-tier
    outcome ladder instead of a per-step vital/optional split, and the
    idea is scrapped for v1 rather than redesigned around a new trigger -
    goal achievement is plain achieved/not-achieved for now.
    **Consequence:** §2.9's win-condition tie-break rule had reserved a
    second-order tie-break for this rating; see that section's own note -
    it simply doesn't apply now, nothing else needed.
    **The "mission patch" visual language this idea was also meant to
    support (user, 2026-09-28, tying into §3.12's logo work) still
    works without it:** a goal achievement is the natural place to render
    the same circular border/rim-text frame as the brand mark, with the
    center art swapped per goal and the ring closed solid gold once
    earned (open/partial gold = the window is still active; closed gold =
    hit it) - a binary achieved/not-achieved badge already works with
    this same frame, no quality-tier visual axis needed for v1.
  - **(d) Proposal for the prestige/goals slice (2026-10-03; unconfirmed
    defaults, one starred user decision in the last bullet).** Two sources of
    prestige and one sink, all deterministic, and no funds coupling - a mission
    or goal never adds cash directly (§2.1(d)).
    - **Mission prestige.** Each `MissionConfiguration` declares a
      `firstTimePrestige` total. The repeat tier is read from the player's
      count of **successful** completions of that configuration (not launches,
      `baspm.md` §11.7 `GetMissionRunCount`), and the total is
      `firstTime` / `secondTime` / `laterTime` by tier. Launch Window's own
      ratios: second-time 60%, later-time 40% - the same *shape* as BASPM's
      50%/30% per §1.2 and §4's balance note, not its numbers.
      **The tier is read live, at each season's own resolution, not stamped
      at launch** (decided 2026-10-03 after advisor review, implemented the
      same day). BASPM's own step handlers call `GetMissionRunCount` during
      resolution (`baspm.md` §11.7), and missions resolve one at a time in
      launch order, so a second copy of a configuration that completes
      earlier in the same pass already counts. The first draft stamped the tier
      at launch; with five copies launched together, that paid all five at the
      first-time total (5,000 instead of 1,000 + 600 + 400×3 = 2,800). No
      `flightTier` field is stored on `InProgressMission` and nothing about it
      travels over the wire.
    - **Two more prestige rules, from the same trace (`baspm.md` §11.7),
      both implemented and harness-checked in sub-slice 1:** a `MINOR_GLITCH`
      step pays half, and goal prestige (sub-slice 2) is folded into that same
      season total. A failing season forfeits the earlier prestige its own
      steps earned - the failing step replaces the season total instead of
      adding to it.
    - **Per-step share.** Each `MissionStep` declares `prestigeSharePercent`.
      The shares across every step of a configuration must sum to exactly 100
      (validated in `ContentCatalog`, like the crew-involvement rule in §2.5(e)),
      so the configuration's total is always paid out whole. Paid per step on
      `SUCCESS`/`MINOR_GLITCH_SOLVED` at full share, `MINOR_GLITCH` at half.
      Unexecuted steps' shares are simply not paid.
    - **Prestige accumulates per season, not per mission** (`baspm.md` §11.7:
      each season is its own screen, paid at that season's end). Within one
      season, a `FAILURE`/`CATASTROPHIC` step **replaces** the season's
      accumulated total with its own failure deduction, rather than adding to
      it. So a failed season forfeits the prestige its earlier steps earned,
      including goal prestige from those steps. The goals themselves stay
      achieved, since they're awarded immediately, not deferred. Earlier,
      completed seasons of the same mission keep their prestige. This is the
      one mechanic here that's easy to get wrong, so it's pinned to a harness
      invariant in sub-slice 1.
    - **Failure deduction is per step, not per configuration.** Each
      `MissionStep` declares `failurePrestigeDeduction`, applied as the
      replacement total above. A configuration-level deduction exists in BASPM
      (`baspm.md` §11.7) but is used there only for mid-flight cancellation,
      which Launch Window doesn't have yet (no scrub). Clamped so cumulative
      prestige never goes below zero (§2.1(d)).
    - **Goals are granted by mission steps,** exactly as BASPM does (`baspm.md`
      §11.7): each `MissionStep` lists `goalIdsAchieved`. There is no separate
      state-predicate goal system in v1 (see the win-condition note in §2.9).
      Achieving a goal cancels every goal in its `cancelsGoalIds` list
      (recursively, as §11.3 traces). **Corrected 2026-10-03 (`baspm.md`
      §11.7):** a cancelled goal is not free. It pays its own first/second-time
      prestige into the achieving goal's reward, and it counts toward the game-wide
      tally, so every later achiever's value moves. The cascade is recursive.
    - **Goal prestige: the count is per game instance, and in human play that
      means per player - corrected 2026-10-03.** Each `Goal` carries
      `firstTimePrestige` and `secondTimePrestige`, and the value is chosen by
      how many times that goal has already been achieved in the local game
      instance (`baspm.md` §11.7). In human multiplayer, the opponent's goals
      are read only for the comparison screen and never enter that count, so
      BASPM's behavior is each player's own count. Only CampaignRTTM's
      simulated rival adds to it. **An earlier draft of this section said
      BASPM runs a cross-player first-come race; that was wrong.** The
      cross-player race is a Launch Window addition, if kept at all (see the
      same-turn bullet below and the open question in the slice's status).
      The per-player count needs no game-level record. A cross-player race would
      need one, persisted alongside `game-meta.json` (an M4 requirement).
    - **Cross-player goal race - decided (user, 2026-10-03), as a Launch
      Window design, not a BASPM port.** The user was told BASPM doesn't run
      this race (see the bullet above) and chose to keep it anyway. So the goal
      count is game-wide, across all players of a game. Consequences: a
      game-level record of goal achievements is needed, persisted alongside
      `game-meta.json` (an M4 requirement), and the harness has to run
      multi-player games to exercise second-time prestige at all.
    - **Same-turn achievers - decided (user, 2026-10-03), with the cross-player
      race above.** Every goal achieved in one resolution pays the first-time
      value to all its achievers, and the game-wide count is incremented only
      after that resolution finishes. This avoids any order-dependence between
      players. A deterministic order by player id was the alternative; it was
      rejected.
    - **Generic flag and program gate - corrected 2026-10-03 (`baspm.md`
      §11.3).** The earlier "generic goals only gate a program" rule was a
      mis-trace: BASPM's gate is an AND list of required goals (`StrictGoalsIDs`)
      plus a category gate and conjunctive lists, checked against all achieved or
      cancelled goals. So: **no `Goal.generic` flag and no generic-only validation
      in v1.** `Program` gets `requiredGoalIds` (an AND list, default empty),
      unconfirmed shape. The opposing-faction comparison screen's generic-only
      filter belongs to M5, where that screen gets built, and the flag can be
      added then if the filter needs one.
    - **Goal-dependency reliability modifier - corrected 2026-10-03 (`baspm.md`
      §11.7): read live on every step, not stamped at launch.** A
      `MissionConfiguration` declares `goalDependencies` (goal id, component id,
      penalty) and `goalBoosts` (goal id, component id, bonus). For each step, and
      for each component that step involves, the modifier is recomputed from the
      snapshot's current achieved-or-cancelled state, inside
      `componentContributionSum` (replacing its `effectiveReliability(current, 0)`
      placeholder), then clamped as `max(0, current + modifier)`. **So no
      `InProgressMission` field and no wire traffic.** A goal achieved earlier in
      the same flight changes the next step's odds immediately, which matches
      BASPM. The UI should surface the modifier (§2.6(c)).
    - **Bookkeeping:** `PlayerSnapshot` gains `successfulFlightCounts`
      (configuration id → count), `achievedGoals` (goal id → game turn of
      achievement, not per-player season), and `cancelledGoalIds`.
      `OwnedMissionComponent` gains no field in this slice. Every new field
      travels in `FetchStateResponse` and needs `MessageRegistryTest` and the
      Jackson snapshot round-trip extended; any new class is appended to
      `MessageRegistry`, never reordered, and avoids `EnumMap`
      (`design.md` §3.4's own Kryo gotcha).
- 2.7 **Random events — analyzed 2026-09-27, see `baspm.md` §12.1/§12.2 for
  the full trace.**
  - **(a) How BASPM does it:** two independent per-season dice rolls (5%
    chance of a "bad" event, 20% chance of a "good" one), each picking from a
    pool of 14/5 candidates gated by real precondition checks (e.g. a
    reliability setback can't strike unless something's actually in R&D) —
    situational, not pure noise. Applied automatically, narrated as a news
    item afterward. **A real choice-presenting mechanism exists in the code
    (`DecisionEventBase`) but is entirely dead in the shipped game** — the
    one class using it is permanently disabled and every method on it is an
    empty stub, and the manager method that would even roll for it is never
    called. **This resolves this project's oldest open question, present
    since the first session: no `REB*` event, and no BASPM random event at
    all, ever presents the player a real mid-turn choice.** Also notable:
    unlike every other system traced so far, this one is entirely hardcoded
    in C# with zero Data Scripts XML backing.
  - **(b) What changes for async multiplayer:** nothing — this needed no
    adaptation even in principle, since it was already fully non-interactive
    in the original. §4's mid-turn-decision list can now drop `REB*`/random
    events entirely; there was never a real case there to resolve.
  - **(c) Decision:** **port the two-tier good/bad shape with situational
    gating** (events react to real recent player state, per §1.1's note
    above) as Launch Window's starting point, re-deriving Launch Window's
    own event list/odds (`design.md` §1.2). **Decision:** make
    Launch Window's own events data-driven from the start (unlike BASPM's
    hardcoded approach) — no reason to inherit that specific limitation.
    **Decision:** don't port the dead "decision event" concept —
    BASPM itself never finished it, and the Tiger Teams redesign (§2.2(c))
    already established this project's preferred alternative to a live
    per-turn choice popup (a standing capability the player develops in
    advance) whenever a similar need comes up, rather than trying to build
    the live-choice UI BASPM itself abandoned.
  - **Decided (user, 2026-10-03), for the implementation slice:**
    - **Delivery: a stored news log per player.** Each resolved season appends
      its random events to a per-player news list kept in `PlayerSnapshot`, and
      the player reads it at their next login. This is the planned season-news
      surface (§2.2(c)) and fits the async model. Not a shared game feed, since
      that would reveal one player's setbacks to everyone.
    - **Event content: BASPM's full set, re-derived.** All 14 negative and 5
      positive events, with their situational gates, using Launch Window's own
      magnitudes (`baspm.md` §12.1). Per the user's standing instruction, the
      magnitudes stay close to BASPM's own values for defaults.
    - **Odds: BASPM's own.** Each season rolls once for a negative event at 5%
      and once for a positive event at 20%, independently.
    - **Still to decide when the slice is built, under the same closeness rule:**
      the exact magnitudes, which are defaults and not new questions.
  - **Implementation mapping (2026-10-03, unconfirmed where marked):**
    - **Correction to `baspm.md` §12.1:** BASPM registers **15** bad events, not
      14 (`RandomEventsManager.Initialize`, lines 14-28), plus 5 good ones.
    - **Roll:** once per season, after the budget pass. A bad roll at 5%, a good
      roll at 20%, independently. The first event in a shuffled pool whose gate
      passes is applied, as BASPM does. Timed effects tick down before the roll,
      so an event that sets N seasons is active for the next N seasons.
    - **Gates** (BASPM's gate where one exists; marked where re-derived):
      - Bad, applicable when the gate holds: `EmployeeBasicTrainingExtension`
        (someone in basic training; extends their training by one season,
        *unconfirmed effect*); `EmployeeDismissed` (an idle employee exists
        *re-derived*); `EmployeeFailedBasicTraining` (someone in basic training);
        `EmployeeResigning` (an idle employee exists *re-derived*);
        `EmployeeUnavailable` (an idle employee exists *re-derived*);
        `MaxNumLaunchesAllowed`, `NoLaunchesAllowed`, `UnexpectedCostAndNoLaunches`
        (gate dropped, since Launch Window has no vehicle-assembly building
        *re-derived*); `MissionComponentsUnavailable`, `ReliabilitySetback`
        (an owned component is in R&D *re-derived*); `MoraleDrop` (a mission
        failed in the current season, BASPM's own gate); `PerishedAstronaut` (an
        idle astronaut exists); `PrestigeDrop` (prestige above zero);
        `RDResultsSetback` (no gate); `UnexpectedCost` (no gate, but only
        applied while funds are positive, since debt has no cost to take
        *re-derived*).
      - Good: `ExtraFunding` (a completed flight in the last four seasons, and at
        least six seasons until the next review - BASPM's own gate);
        `MoraleBoost` (a completed flight in the last four seasons); `PrestigePointsBoost`
        (prestige above zero); `RDResultsBoost` (no gate); `ReducedExpenditures`
        (at least six seasons until the next review).
    - **Effects, BASPM magnitudes, Launch Window's own state:**
      - Morale effects: a random group loses 10-20 (`MoraleDrop`) or gains 9-21
        (`MoraleBoost`) morale.
      - Prestige: `PrestigeDrop` takes 4-10% of current prestige, floored;
        `PrestigePointsBoost` adds 4-7.5%.
      - Funding: `ExtraFunding` raises the current funding rate by 7-16% until the
        next review resets it.
      - Costs: `UnexpectedCost` takes 7-15% of current funds, and
        `UnexpectedCostAndNoLaunches` does the same plus the launch cap below.
        `ReducedExpenditures` multiplies running costs by 0.75-0.95 for 3-11
        seasons.
      - R&D: `RDResultsSetback` multiplies R&D gains by 0.80-0.95 for 1-4 seasons;
        `RDResultsBoost` by 1.15-1.25 for 1-7 seasons. Each sets one slot, so a
        later event replaces an earlier one (as BASPM's setter does).
      - Reliability: `ReliabilitySetback` picks 1 to N of the components in R&D
        and cuts each by 7-15 points, floored at its own transfer floor
        *re-derived*.
      - Personnel: `EmployeeDismissed` and `EmployeeResigning` remove one idle
        employee (new departure reasons); `PerishedAstronaut` removes one idle
        astronaut as lost in an accident; `EmployeeFailedBasicTraining` removes one
        trainee; `EmployeeUnavailable` makes one idle employee unavailable for one
        season, during which launches skip them *re-derived*.
      - Missions: `MaxNumLaunchesAllowed` caps launches at one, and
        `NoLaunchesAllowed` at zero, for the next season's launch window.
        `MissionComponentsUnavailable` takes half (at least one) of the owned
        components out for 1-3 seasons, during which launches using them are refused.
    - **New state:** a per-player news log (`NewsEntry`, appended to the snapshot,
      capped at the most recent 50); timed multipliers for R&D and fixed costs;
      a launch cap; per-component and per-employee unavailability counters; and
      the turns of the last completed and last failed flights.
  - **Source verification, 2026-10-03 (`baspm.md` §12.1, read in full, not grepped).**
    The gates marked "re-derived" above are now resolved against source where BASPM
    has them. Confirmed: the basic-training extension is exactly one season;
    dismissal, resignation, and unavailability gate on any group with an eligible
    member, with astronauts needing an idle one; a morale drop needs a `FAILURE` in
    the current period (never `CATASTROPHIC`); a success needs any failure-free
    season in the last four periods; `ModifyCurrReliability` clamps at the
    transfer-inclusive floor. **Two real findings:** BASPM registers 15 bad events,
    not 14; and the R&D and fixed-cost multipliers are never read, so those three
    events change nothing in the shipped game.
  - **Sub-slice (a), done (2026-10-03):** the mechanism (`RandomEvents`), content
    (`RandomEventRoster.baspmClose()`, all 20 events with BASPM's ranges and
    durations), the structured news log (`NewsEntry`, capped at 50, appended to
    `MessageRegistry`), and the twelve events that only touch existing state. Those
    are: basic-training extension, dismissal, failed basic training, resignation,
    perished astronaut, morale drop and boost, prestige drop and boost, unexpected
    cost, reliability setback, and extra funding. Deliberate deviations: personnel
    events take only employees not on a mission (idle or training, and idle only for
    astronauts), because a crew member can't be removed mid-flight without breaking
    Launch Window's assignment invariants; the unexpected-cost event needs positive
    funds, since debt has nothing to take; and the reliability floor is BASPM's own.
    **Sub-slice (b), done (2026-10-03):** all twenty events are implemented. Timed
    state (`PlayerSnapshot`: R&D and cost multipliers with their seasons remaining,
    the launch cap and launches this window, unavailable components by window;
    `Personnel.unavailableSeasons`) is aged once per season at the start of the
    events pass, so an event lasting N seasons covers N seasons of research, budget,
    and launches. The R&D multiplier scales each specialist's flat skill gain and
    reliability gain, with the skill clamped at `SkillGrowth.SKILL_CAP`. The cost
    multiplier scales salaries and maintenance, so the debt cap moves with it. Launches
    refuse at the cap (`LAUNCH_CAP_REACHED`) and for an unavailable component
    (`MISSION_COMPONENT_UNAVAILABLE`); an unavailable employee is treated as not idle.
    The unavailable employee still trains and researches (a narrower effect than
    BASPM's, which isn't traced further). **Harness, after (b):** the playout's
    events fire all twenty kinds, with `MORALE_DROP` rarest (3 in 200 games). Over
    the same run: 814,335 prestige paid, 41,744 deducted, 1,563 completed flights,
    1,081 resigned, 1,036 retired, 5,337 still employed, 115 removed by dismissal or
    failed training, 4,694 hires.
    **Harness, earlier note:** the playout now runs the events every season after the budget
    pass, with departures counted through the existing accounting (dismissals and
    failed training as a separate count). The long run fires eleven of the twelve
    implemented events; `MORALE_DROP` does not fire there, because its gate needs a
    mission failure in the very season the roll happens, and this harness's failure
    rate is low. A 100-season, 200-game run does fire it (`RandomEventsTest` and
    `PersonnelLifecyclePlayoutsTest`, both green).
- 2.8 **Campaign objectives / win conditions — analyzed 2026-09-27, see
  `baspm.md` §12.3 for the full trace (GSA's own single-player scripted
  content is out of scope per `design.md` §1.2 — only the reusable system
  shape matters here).**
  - **(a) How BASPM does it:** a strategy-pattern objective system — one
    long-term objective (achieve a specific goal by a hard deadline; success
    triggers a real Victory screen, failure a real Game Over screen — a
    genuine win/loss condition, not open-ended scoring) plus short-term
    objectives drawn periodically (2 at a time, at each budget-review
    boundary) from a larger authored pool, each independently timed and
    granting/deducting prestige at its own deadline. Seven objective-field
    types, all simple, generic state-threshold predicates (funds, personnel
    headcount, skill competency, component reliability, a specific goal set,
    a component's successful-use count) — a clean, reusable shape completely
    independent of GSA's specific scripted content. **Bonus finding:**
    `CampaignRTTM` (the single-player AI-rival mode) uses this exact same
    "race to one specific milestone goal" pattern as its own win condition —
    real precedent for a head-to-head race-to-a-goal win state, not just an
    open-ended score comparison.
  - **(b) What changes for async multiplayer:** nothing structural for
    objective *checking* — deterministic state comparisons at a fixed
    deadline, no mid-resolution surprises, same clean shape §2.3/§2.4/§2.6
    already found. **But this system's existence surfaces a real, still-open
    design question for §2.9, not a minor detail:** does Launch Window want
    an actual win condition at all (a race to some finish line, mirroring
    `CampaignRTTM`'s shared-goal race) or is it an ongoing/open-ended score
    comparison (mirroring the passive `MultiplayerColdWar` mode)? This
    system's shape supports either answer equally well — it doesn't resolve
    the question, it just means whichever direction §2.9 lands on has a
    working mechanism to build from.
  - **(c) Decision:** **port the objective-field strategy-pattern shape**
    (the seven generic predicate types above) as reusable prior art for
    Launch Window's own scoring/achievement system, independent of GSA's
    specific content (`design.md` §1.2). Leave the actual win-condition
    question itself for §2.9 to resolve. **Resolved 2026-09-28, see §2.9:**
    a declared-goal-list race, built on this same objective-field shape.
- 2.9 **Multiplayer interaction model** — the one system with no BASPM
  precedent beyond its arms-length PBEM mode (`baspm.md`). Brainstorm below;
  the interaction-mode starting point and the win condition are both now
  decided (see the `Decision`/`decided` bullets further down):
  - Passive competition only (both agencies race for the same historical
    firsts/prestige, visible to each other only through news-style reports
    and a goal-progress comparison screen — this is BASPM's actual existing
    `MultiplayerColdWar`/`CampaignRTTM` shape, not just an analogy: it really
    does expose which generic goals the opponent has achieved via a
    side-by-side comparison screen, `baspm.md` §5/§2.4, even though the two
    sides otherwise play in isolation — and per `baspm.md` §11.4/`design.md`
    §2.6(b), it's not a standalone screen, it's bundled directly into the
    budget/prestige overview the player already checks every turn, a
    placement worth carrying over).
  - Direct communication: in-game chat/mail between players.
  - Active interference ("sabotage"): espionage, poaching the other side's
    personnel, disrupting a rival's launch window — needs careful balance so
    it doesn't just punish whoever logs in less often, given the async
    cadence.
  - Cooperative options (joint missions, shared programs) as a distinct mode
    alongside head-to-head.
  - **Decision:** start with passive competition + in-game player-to-player
    mail for a first playable version; layer espionage/sabotage/interference
    in once the core daily-turn loop is proven fun and fair. **Clarified
    (user, 2026-09-28): "mail" means in-game messages over the same
    connection as everything else, not server-sent notification emails to a
    real address** — no email-address field needed on the §3.8 account
    schema.
  - **Win condition — decided (user, 2026-09-28), resolving the question
    §2.8's campaign-objectives trace surfaced (2026-09-27, `baspm.md`
    §12.3):** Launch Window has a real hard win condition, mirroring
    `CampaignRTTM`'s "race to a goal" shape rather than
    `MultiplayerColdWar`'s open-ended score comparison — but generalized from
    `CampaignRTTM`'s single milestone goal to a **declared list of goals**.
    **When a game is created, the host declares the list of goals that
    define that game's win condition** (reusing §2.8(c)'s ported
    objective-field strategy-pattern shape for the goals themselves — this is
    what makes an arbitrary declared list practical rather than a single
    hardcoded milestone). **First player to achieve every goal on the list
    wins.** This list is a piece of per-game config, alongside §3.6's
    turn-cadence/calendar config, and **must be visible to players in the UI**
    throughout the game (the goal-progress comparison screen §2.6(b)/§2.9's
    first bullet already established belongs on the budget/prestige overview
    is the natural home for it — it already shows each side's goal
    achievement, this just adds "these are the ones that end the game").
    **Tie-break rule (fully specified, user, 2026-09-28):** normally "first
    to complete the list" already resolves the race outright — daily-batch
    resolution means one player reaches their final goal on an earlier
    calendar turn than the other. A tie only exists in the narrow case where
    **more than one player completes their final goal in the same turn's
    resolution.** For just those tied players, build a **placement table
    across every goal in the declared list**: for each individual goal,
    rank the tied players by who achieved *that* goal earliest (1st, 2nd,
    3rd, ...). Then compare the tied players' placement counts
    Olympic-medal-table style — most 1st-place goal finishes wins; if still
    tied, most 2nd-place finishes; and so on down the list. **Second-order
    tie-break, now moot (user, 2026-10-01):** was meant to be best
    goal-achievement *rating*, but §2.6(c)'s gold/silver/bronze tiered
    goal-achievement rating idea is scrapped for v1 - there is nothing for
    this second-order tie-break to compare, so it simply doesn't apply. An
    unresolved tie after the medal-table comparison is vanishingly unlikely
    given how many independent rankings precede it, and isn't worth
    designing a replacement criterion for right now.
  - **Win-condition check (2026-10-03; the list-shape decision is user-decided,
    the rest are unconfirmed defaults).**
    - **The win list is a list of goal ids - decided (user, 2026-10-03).**
      Not a list of state predicates. BASPM's goals are granted by mission
      steps (`baspm.md` §11.7), so the seven-predicate objective shape (§2.8(c))
      belongs to campaign objectives only and is not used here. The §2.8(c)
      reuse is withdrawn for v1.
    - **"Done" for a listed goal means achieved, or cancelled by an achieved
      goal.** Without the second clause, a list containing both a goal and a
      goal it cancels could never be completed. This mirrors BASPM's own
      gate semantics, which counts achieved-or-cancelled (`baspm.md` §11.3).
    - **Achievement time is the game turn of the resolution** that achieved
      the goal, not the player's own season count. That keeps the placement
      table comparable across late joiners (§3.8).
    - **Placement table ties** use standard competition ranking: players who
      achieved the same goal in the same resolution share a placement, and the
      next placement skips accordingly (1, 1, 3). This is an unconfirmed
      default; the placement rule itself is the one already decided above.
    - **Headless function, not a per-player resolver:** takes the declared list
      and every player's `achievedGoals`/`cancelledGoalIds`, and returns either
      no winner yet, a single winner, or the tied set plus its placement table.
      The real call site is M4's `TurnScheduler`, which owns the game-meta file
      that declares the list (§3.6).
- 2.10 **Difficulty level — decided (user, 2026-09-27): none, by design.**
  BASPM has three difficulty levels (`DifficultyLevelT.{Normal, Hard,
  BuzzHard}`, `baspm.md` §2.4) as a single-player choice — the player picks
  how much punishment to opt into. **Launch Window has exactly one
  difficulty, shared by every player in a game, with no difficulty selection
  offered at all.** Reasoning: difficulty-as-a-player-choice doesn't fit the
  multiplayer angle — it can force players into a difficulty they'd rather
  not engage with (e.g. a game host picking Hard, or a mismatch where one
  player wants a harder game than the rest of their group). A shared,
  non-optional difficulty removes that friction entirely rather than trying
  to solve it (per-player difficulty scaling, host-only override, etc.).
  **Concrete decision: base all of Launch Window's balance on BASPM's
  Normal difficulty specifically** — i.e., wherever a Data Scripts value is
  keyed by difficulty (`Constants.xml`/`Constants-{GSA,NASA,Soviet}.xml`'s
  `Value1`/`Value2`/`Value3` = Normal/Hard/BuzzHard, `baspm.md` §3.1), Launch
  Window reads only `Value1`/Normal when re-deriving formulas; Hard/BuzzHard
  numbers are out of scope entirely, not something to port, categorize, or
  even keep around for reference. **Architecture implication:** no
  difficulty field anywhere — not in game creation, not per-player, not a
  `DifficultyLevelT`-equivalent enum at all. One less axis of configuration
  for §3.6's game-instance config to carry. If Normal later proves miscalibrated
  for the async multiplayer context, that's retuned as Launch Window's own
  single evolving balance sheet, not by resurrecting a three-tier system.

## 3. Architecture

### 3.1 High-level shape

**The "long-running process or not" question resolves itself once §3.3's
tech stack and §3.8's account model are decided (2026-09-27) — yes, there is
one, it just does much less continuous work than either sibling project's.**
KryoNet needs a resident listener process regardless (same as both sibling
projects), and §3.8's `AccountStore` pattern (an in-memory map, background-
flushed to disk on a fixed cadence) also assumes a live, resident process —
there's no version of this stack that's genuinely serverless. What's
different from StarWars/Robot-Rampage isn't "process or no process," it's
*how little* that process actually does most of the time: no continuous
real-time simulation loop, no live session state per connection — just
occasional short-lived client connections (§3.3's "connect, send today's
actions, get current state, disconnect" RPC-style usage) plus one scheduled
turn-resolution job per day (§3.6). Architecturally closer to a lightly-used
long-running service than to either sibling's continuously-simulating game
server, but a genuine resident process (and genuine Docker deployment,
matching StarWars' own `Dockerfile` precedent, §3.9) either way.

See §3.7 for the persistence/data-model decision (flat-file JSON, no
database) and §3.9 for the one-game-per-server-instance decision that
simplifies both §3.6's scheduler and §3.7's file layout.

### 3.2 Relationship to BASPM's own simulation code

BASPM's per-player simulation (`Player.cs`, `GameWorld.cs`, the various
`*Manager` classes — see `baspm.md`) is a **single-player state machine that
already tolerates being paused, serialized, handed to a remote peer, and
resumed** (that's literally what its PBEM mode does over HTTP). The
resolution logic itself (mission step outcomes, budget reviews, personnel
events) is turn/season-oriented already, not frame-oriented — much more
directly portable to a daily-batch server-authoritative model than either
sibling project's simulation was to *their* respective networking models.
Concretely, this suggests: keep BASPM's turn/season-oriented data shapes
(programs, mission configs, personnel records, prestige/budget ledgers) as
the starting schema for our own turn-resolution engine, re-deriving formulas
from its data-driven XML (`baspm.md` §"Data-driven architecture") rather than
guessing at balance numbers from scratch.

### 3.3 Tech stack

**Decided (user, 2026-09-27): stay close to the sibling projects' stack —
libGDX (client), KryoNet (network layer), Jackson (JSON), JUnit 5 (tests),
Java 25.** Rationale is continuity and proven experience (tooling, asset
pipeline conventions, testing conventions all carry over directly) rather
than re-litigating tooling choices for a fan project — the earlier "maybe a
plainer web stack fits the daily-check-in shape better" alternative
considered here is dropped, not left open.

**Implemented (2026-09-27):** the Maven multi-module skeleton exists —
`pom.xml` + `core`/`lwjgl3`/`server`/`dev-tools`, modeled on Robot-Rampage's
own parent pom specifically (its `lwjgl-bom` import is cleaner than
StarWars' older per-classifier block). `mvn clean package` passes from the
repo root; the server binds its TCP port and the client opens a real LWJGL3
window, both actually launched and confirmed running (not just "no error in
the log") before being killed. No real game logic yet — this is a proven
skeleton, not a feature. See `CLAUDE.md` "Build system" for the exact
commands/gotchas. **Release packaging (client zip, server Docker image) is
deliberately deferred** until there's actual game content to package — both
sibling projects have a complete, working template for this
(`release-client` Maven profile + `jpackage` + `Dockerfile` + tag-triggered
GitHub Actions) to migrate from once it's needed, not worth re-deriving or
even scaffolding empty right now.

- **KryoNet's usage pattern here genuinely differs from both sibling
  projects, worth being explicit about so implementation doesn't
  accidentally inherit assumptions that don't apply**: StarWars uses it for
  continuous real-time UDP snapshots with client-side prediction/
  reconciliation; Robot-Rampage holds a live session open for a
  simultaneous-programming turn. Launch Window needs neither — per
  `CLAUDE.md`'s own framing of this project, "nobody is ever waiting live
  for other players," so a client's KryoNet usage looks more like a
  lightweight RPC transport (connect, send today's queued actions/query
  current state, receive the reply, disconnect) than a session-holding
  connection. No prediction/reconciliation layer, no reconnect-during-a-live-
  turn machinery — genuinely simpler netcode than either sibling project's,
  despite reusing the same library.
- **Correction (found while scaffolding, 2026-09-27): Jackson does *not*
  double as the wire-format library — an earlier draft of this bullet said
  it did, which is wrong.** KryoNet serializes wire messages with Kryo (a
  binary format, not JSON), the same way both sibling projects use it —
  compatibility depends on registration order, not class names, so real
  message classes need a Robot-Rampage-style `MessageRegistry` (append-only,
  never reordered) plus a test guarding every registered type — built in
  §5's M1 protocol slice, `de.mkoehler.launchwindow.net`. **Jackson's actual,
  sole job here is §3.7/§3.8's
  on-disk persistence** — flat-file JSON snapshots, the action journal, and
  the account store — which is exactly why `core`'s persistence-format
  proof lives as `PlayerSnapshotTest`'s round-trip test (§3.4), under the
  `model` package, not a `net` one.
- **Unblocks §3.4 (Data model)**: with the tech stack settled and most of
  §2's core systems now traced (`baspm.md` §8-§12), that section is ready to
  actually start once picked up — not attempted in this same pass, flagged
  here as no longer gated on an open tech-stack question.

### 3.4 Data model

**First slice implemented (2026-09-28):** `core`'s new
`de.mkoehler.launchwindow.model` package (zero libGDX imports, per §3.10),
covering the shapes already decided in §2/§3.7/§3.8:

- **`PlayerSnapshot`** — the aggregate root (§3.7's per-player-per-turn
  persisted file): agency name, `Archetype`, funds, prestige, the recurring
  per-season funding rate (§2.1(a)/(c)), the player's own personal season
  counter (§3.8's late-joiner decoupling), and the three personnel rosters.
  Holds only per-player state, references future content-definition types
  (programs, goals, etc.) by ID rather than embedding them, once those types
  exist.
- **`Archetype`** — exactly two values, `NASA`/`SOVIET` (§2.1's no-GSA
  decision).
- **Personnel** — `Personnel` (shared mutable base: id, name, age, morale,
  learning potential, `AssignmentState`) with three concrete subclasses,
  `Astronaut`/`SetSpecialist`/`MissionControlOfficer`, each holding a
  `LinkedHashMap` of its own five-value skill enum (§2.2(a)/`baspm.md`
  §9.1) — `LinkedHashMap`, not `EnumMap`, since Kryo's wire serialization
  (§5's M1 protocol slice) needs a no-arg-constructible map type, which
  `EnumMap` isn't.
  `MissionControlOfficer.flightDirectorSkill()` computes the sixth "Flight
  Director" aptitude as the plain average of the other five, exactly as
  `baspm.md` §9.1 traced — not a stored value, so not a member of
  `MissionControlSkill`. **Skill values switched from `Integer` to `Double`
  (2026-09-30, M2's skill-growth groundwork):** SET's flat per-season R&D
  growth (§2.2(c)/`baspm.md` §9.3's re-derived shape) is well under 1 per
  season, which an `Integer` map would round away to nothing every single
  season. All three `getSkill`/`setSkill` accessors and the underlying maps
  changed together; every existing skill-map test updated to match, and the
  Kryo/Jackson round-trip tests (`MessageRegistryTest`/`PlayerSnapshotTest`)
  now assert on a fractional value specifically, not just an integer that
  happens to also be a valid double.
- **`AssignmentState`** — `IDLE`/`ASSIGNED`/`IN_TRAINING`; deliberately does
  not yet distinguish R&D from a mission assignment, since neither exists as
  a model type yet.

**Age representation — decided (2026-09-30, M2's personnel-lifecycle
slice, §2.2(c)/§5):** `Personnel.age` stays a whole-years `int` (its
existing M1 shape/API is unchanged), rather than switching to a fractional
or seasons-only representation. A new, separate `agingProgressSeasons`
field (`int`, 0-3, public accessors only because Jackson/Kryo need a bean)
tracks how many seasons have accumulated since `age` last incremented -
BASPM's own age advances by exactly 0.25/season (`baspm.md` §9.2), and one
Launch Window turn is one such season (§1/§3.6), so this counter rolls
`age` up by one whole year every 4 seasons rather than the model carrying
a fractional year anywhere client code would have to know to round for
display. Not meant to be read/written outside the rules engine's own
season-advance pass.

**Training/R&D fields — proposed ahead of M2's training-progress sub-slice,
written down before any code so the sub-slice starts from a settled shape
rather than inventing fields mid-implementation; the `Personnel`/per-
subclass fields below are now implemented exactly as proposed
(2026-09-30, same day), the `PERMANENT`/R&D items remain proposed-only,
unconfirmed:**
- **On `Personnel` (shared, since all three groups train the same way,
  `baspm.md` §9.3):** a new `TrainingKind` enum (`NONE`/`BASIC`/`ADVANCED`/
  `PERMANENT`), plus `trainingSeasonsCompleted`/`trainingSeasonsTotal`
  (`int`) for the fixed-length kinds. `trainingSeasonsTotal` is meaningless
  for `PERMANENT` (no fixed length - it re-enrolls until a target level is
  hit, `baspm.md` §9.3) and for `NONE`.
- **On each subclass, not `Personnel`** (`Astronaut`/`SetSpecialist`/
  `MissionControlOfficer`): a `trainingTargetSkill` field of that
  subclass's own skill enum (`AstronautSkill`/`SetSkill`/
  `MissionControlSkill`) - the three groups' skill enums are different
  types, so this can't be one shared field on the base class the way the
  counters above can.
- **`PERMANENT` training additionally needs a target level** (`double`,
  alongside `trainingTargetSkill`) - re-enrolls every completed round until
  the target skill reaches it. **Implemented (2026-09-30):**
  `Personnel.trainingTargetLevel` (shared base field, not per-subclass like
  `trainingTargetSkill` - a plain level has no skill-enum-type dependency)
  and `TrainingProgress.shouldReenroll` — each `PERMANENT` round is the
  same length as `ADVANCED` (`TrainingProgress.ADVANCED_TRAINING_SEASONS`),
  and on completion either re-enrolls (resets `trainingSeasonsCompleted`
  to 0, keeps everything else) or finishes exactly like a completed
  `BASIC`/`ADVANCED` round would. `TrainingCompleted` is now raised for
  every completed *round*, not just a finished assignment — check the
  employee's own `trainingKind` afterward (`PERMANENT` if re-enrolled,
  `NONE` if that was the final round) to tell the two apart. 16 unit tests
  total for `TrainingProgress` (up from 12), covering re-enrollment,
  stopping at the target, and both termination-guard cases below. **Not
  yet wired into the bot harness** — the harness's own bot still only
  starts `BASIC`/`ADVANCED`; teaching it to start `PERMANENT` (with a
  target somewhere in `(currentSkill, SkillGrowth.SKILL_CAP]`) and adding
  the matching invariant is separate follow-up work. **The "immortality
  loophole" flagged here originally is
  now resolved as a side effect of §2.2(c)'s 2026-09-30 departure-
  protection reversal, not by a guard inside `PERMANENT` itself:** this
  note originally warned that combining `PERMANENT` with the
  then-current "resignation/retirement only checked while idle" property
  would let an employee on a high/unreachable-target standing order stay
  protected indefinitely. Since training (any kind, including the future
  `PERMANENT`) no longer protects Astronaut/Mission Control from departure
  at all - only `AssignmentState.ASSIGNED` does now, and SET is never
  protected regardless - an employee stuck in `PERMANENT` training is
  exactly as eligible to resign/retire as an idle one, every season.
  **A second, mission-shaped variant of this same loophole surfaced
  later (2026-10-02) and is also closed, by a different mechanism - see
  §2.5(f)'s retire-on-release decision:** `ASSIGNED` protecting someone
  on an active mission is correct, but releasing and immediately
  relaunching them every season would otherwise let an over-age employee
  never sit unprotected long enough for a retirement check to run;
  `MissionProgress` retires them on release instead, rather than
  reworking the protection rule itself again. **A
  termination guard is still needed for a different reason, not this
  one:** without a cap on re-enrollment, a standing order with learning
  potential 0 (the gain is always 0) or a target at/above `SkillGrowth.SKILL_CAP`
  (the curve only approaches it asymptotically) would loop forever,
  re-enrolling every round with no progress - an infinite-loop bug, not a
  balance question. Stop re-enrolling once the target is reached *or* a
  round's own gain falls below a small epsilon.
- **R&D-target-component linkage - implemented (2026-10-01), written down
  before any code, same convention as the training fields above - see
  §5's M2 status for the concrete classes:**
  `AssignmentState.ASSIGNED` now distinguishes "doing R&D" from
  "assigned to a mission" via the fields below, resolved without a new
  enum constant - SET's
  own `ASSIGNED` already means "working on mission components"
  (§2.2(c)'s departure-protection rule already treats it that way, and a
  new `AssignmentState` value would carry Kryo-ordinal and departure-
  protection consequences for no real gain) - instead `SetSpecialist`
  gains two new fields, `rdTargetComponentId` (`String`, nullable, the
  owned `MissionComponent#id()` a `SET` employee's current `ASSIGNED`
  state is working on; `null` when not doing R&D) and `rdSlot` (`Integer`,
  nullable, which of that component's four R&D slots the employee
  occupies; see below - "doing R&D" means both fields are set, not just
  the target id).
  - **`baspm.md` §9.3 confirms two separate effects from one season of
    R&D, not one:** the SET employee's own skill-in-use grows by a flat
    `random(0.2, 0.4)` (BASPM's own numbers; re-derived as
    `SkillGrowth.RD_GAIN_FACTOR_MIN`/`MAX`, already implemented as
    `SkillGrowth.flatRdGain` - this part of the mechanic was already built
    ahead of its caller existing). Separately, `baspm.md` §10.8 - traced
    the same session once the user was back on the main PC with decompiled-
    source access, resolving what had been a manual-level-only claim -
    confirms "R&D reliability gains scale with the assignee's skill in that
    component's group" means a considerably richer mechanic than a single
    assignee scaling a single formula: see below.
  - **Decided (user, 2026-10-01), re-deriving `baspm.md` §10.8's exact
    code-confirmed mechanic with Launch Window's own numbers, after
    presenting both halves of it as explicit choices:** adopt BASPM's full
    multi-slot shape, not a simplified single-assignee version, and adopt
    its two-ceiling shape too.
    - **Two ceilings, not one:** `MissionComponent` gains `maxRdReliability`
      (R&D alone can never push a component's `currentReliability` past
      this; always ≤ `maxReliability`) alongside the existing
      `maxReliability` (the absolute ceiling only a future mission-
      simulation slice's post-mission reliability change can approach, per
      §2.5(c)). Closing the gap between the two needs actually flying the
      component, not more R&D - the point of having two ceilings at all.
    - **Four explicit R&D slots per component**, not one open-ended pool of
      assignees: `MissionComponent` gains `rdSlotBandMaxAmounts` (four
      ascending raw-improvement-amount ceilings, re-deriving BASPM's own
      per-component `RDLevelMaxAmount L1`-`L4` XML data), and
      `SetSpecialist.rdSlot` records which slot (0 = best band, 3 = worst)
      an assigned employee occupies - a deliberate choice, not auto-
      assigned by skill, matching BASPM's own UI-level slot-assignment
      mechanic (no player-action path for *choosing* a slot exists yet
      either, same "no real assignment UI yet" status as R&D targeting
      itself - see below).
    - **Exact per-slot formula, `SkillGrowth.rdReliabilityGain(rawAmount,
      currentReliability, maxRdReliability, assigneeSkill)`:** `rawAmount ×
      ((maxRdReliability − currentReliability) ÷ 100) × clamp(assigneeSkill
      ÷ SKILL_CAP, 0, 1)` - code-confirmed, not this project's own
      invention the way the single-assignee placeholder was.
      `SkillGrowth.drawRdSlotRawAmount`/`rdSlotBandLower`/`rdSlotBandUpper`
      derive a slot's own band from the four ascending ceilings (slot 0
      draws from the band between the 3rd and 4th entries, each
      successive slot one band lower, slot 3 from `[0,` 1st entry`]`).
      **Supersedes the single-assignee `SkillGrowth.reliabilityGain`**,
      removed the same session, along with its now-dead
      `RD_RELIABILITY_FACTOR_MIN`/`MAX` constants and
      `drawRdReliabilityFactor`.
    - **A component's headroom factor is computed once per season, shared
      by every occupied slot** - not recomputed per assignee - matching
      BASPM's own code exactly (`PerformRDProcessOnPlayerMsnCmp` computes
      it once, before iterating slots). **This supersedes an earlier
      version of this same proposal** ("two SET employees assigned to the
      same component stack, applied in roster order, each computed against
      whatever the component's reliability already is") - that sequential
      model doesn't match BASPM's actual shared-headroom mechanic, only
      traced after the fact.
    - **A stagnation guard**: once remaining headroom drops under
      `ResearchProgress.HEADROOM_STAGNATION_THRESHOLD` (Launch Window's own
      value, `0.2`, not BASPM's `0.1` copied verbatim, per §1.2), any
      positive nominal gain snaps the component straight to
      `maxRdReliability` in one step, rather than crawling toward it
      asymptotically forever - re-deriving BASPM's own guard's shape, not
      its exact number.
    - **A component reaching its R&D ceiling automatically releases every
      SET employee occupying one of its slots back to `IDLE`** - caught
      only on a post-implementation advisor pass, not in the first version
      of this same session's implementation - re-deriving `baspm.md`
      §10.8's own `ProcessAndRetrieveAllMissionComponentsThatReachedMaxRDReliabilityAtSomePointDuringTheLastSeason`/
      `UnassignAllSETPersonnelFromMissionComponent` mechanic: checked
      against reliability as it stands at the end of the season (so a
      component already at its cap when first assigned to releases its
      researcher the very next season too, not only the season it actually
      reaches the cap). New event, `ResearchComponentMaxed(componentId,
      releasedPersonnelIds)` - one per maxed component, not per employee;
      BASPM's own per-employee `SETEmployeeAvailableBackAgain` has no
      direct equivalent here yet, this grouping is Launch Window's own
      choice. **Unconfirmed timing nuance:** BASPM triggers the release
      from its own interseason-overlay news-item generation, a UI-adjacent
      call site, not from inside the R&D resolver itself - functionally
      the same season, but not re-verified to be the exact same resolution
      step; folded directly into `ResearchProgress.advanceSeason` here,
      which should be behaviorally equivalent for one player-snapshot's
      turn resolution but is a simplification worth flagging.
    - **Per-employee event attribution is Launch Window's own choice, not
      a BASPM concept:** BASPM has no notion of per-employee credit once
      slots are summed into one component-wide total. `ResearchProgress`
      reports each contributing specialist's `ResearchCompleted`
      `reliabilityGain` as their proportional share of the season's
      actually-applied total (their own nominal, pre-clamp contribution
      divided by the sum of every occupied slot's nominal contribution,
      times the applied total) - chosen specifically so summing every
      event's `reliabilityGain` for a season always equals that
      component's real reliability delta, which the bot harness's own
      reporting (§3.10) relies on.
    - **Slot-collision/no-real-assignment-UI handling, a defensive
      simplification flagged unconfirmed rather than asked about:** with no
      player-action path for *choosing* a slot yet (same status as R&D
      targeting itself - both are bot-harness-only for now, M4 scope to
      wire into real player actions), `ResearchProgress` treats a
      specialist with no slot set, an out-of-range slot, or a slot
      colliding with another specialist already on the same (component,
      slot) pair as simply not researching at all this season (skipped for
      *both* their own skill gain and the component's reliability gain,
      first-encountered-in-roster-order wins a collision) - real
      enforcement belongs at a future assignment layer, not here.
  - **Both of a season's R&D effects use the assignee's skill as it stood
    at the start of that employee's own turn this season** (before this
    same season's own flat skill gain is applied) - keeps the two draws
    independent of each other's own output, not entangled. **The random-
    draw order changed from an earlier version of this proposal**: every
    researching specialist's flat skill gain is now drawn and applied
    first, grouped by target component (in the order each component's
    first active researcher appears in roster order) and slot 0 before 1
    before 2 before 3 within a component - not literal roster order, since
    both passes iterate in this same grouped order - *then* every
    component's reliability draws happen, in the same grouped order,
    necessary once reliability became a per-component, not per-specialist,
    computation.
  - **Never touches `OwnedMissionComponent.floorReliability`** - only
    `currentReliability` grows from R&D; the floor stays exactly what
    `ProgramOpening` set it to.
  - **Composition order: `PersonnelLifecycle`, then `TrainingProgress`,
    then this resolver, then `HiringPool.refreshCandidates` last** (a
    departed SET employee does no further R&D the same season they left;
    running after `TrainingProgress`, not before, is also what keeps the
    two resolvers' random draws reproducible from a seed, since both draw
    from the same shared source).
  - **Reverse pointer (2026-09-30), resolved by this same proposal:**
    `PersonnelDeparture` (§2.2(c)) gains `rdTargetComponentIdAtExit`
    (`String`, nullable) - `null` for every non-`SET` departure and for a
    `SET` departure with no R&D target set, populated otherwise. (Doesn't
    carry a matching `rdSlotAtExit`: a departed employee simply stops
    being found by `ResearchProgress`'s own roster scan next season, which
    frees their slot automatically - no separate bookkeeping needed. A
    freed slot leaves a gap rather than compacting the remaining
    employees down, matching BASPM's own `RemoveSETEmployeeFromSlot`,
    unconfirmed whether that's the behavior worth keeping once a real
    slot-assignment UI exists.)

**Hiring pool — proposed ahead of M2's hiring-pool sub-slice (2026-09-30),
written down before any code, same convention as the training fields
above; implemented the same day, right after the proposal, with the field
shape exactly as proposed:**
- **Candidates reuse the existing `Astronaut`/`SetSpecialist`/
  `MissionControlOfficer` classes directly, rather than a parallel
  candidate-specific type** - a candidate and a hired employee need the
  same shape (age, skills, learning potential), and the only real
  difference is *which list they're in*. `PlayerSnapshot` gets three new
  fields, one candidate pool per group:
  `astronautCandidates`/`setSpecialistCandidates`/`missionControlCandidates`
  (`List<Astronaut>`/etc., same defensive-copy-on-set shape as the existing
  roster lists). A candidate's `morale`/`assignmentState`/training fields
  are meaningless until hired - seeded to a real (not default-zero) value
  at generation time regardless, so the record reads sensibly even before
  hire. **No `MessageRegistry` change needed** - these are `List<Astronaut>`
  etc., and both `Astronaut` and `ArrayList` are already registered.
- **Quality is fed by the hiring group's own current average morale**
  (`baspm.md` §9.4's mechanic, kept per §2.2(c)'s existing decision to
  carry this feedback loop over) - re-derived as four morale bands, own
  boundaries and skill ranges, not BASPM's named tiers/values: roughly
  "Poor" (low morale → narrow, low skill range), "Average", "Good", up to
  "Excellent" (high morale → wide, high skill range). Exact numbers to be
  decided in code as **(unconfirmed defaults)**, same as every other M2
  numeric constant. An empty roster (nothing to average) falls back to a
  fixed neutral morale for band purposes, not zero/undefined.
- **A fixed batch size, regenerated in full each time** (own number, not
  BASPM's 10) - refreshing discards whoever wasn't hired from the previous
  batch, framed as "the job market moved on," not an accumulating pool
  with a cap.
- **Hiring itself (moving a candidate into the real roster) is a plain
  headless operation, not tied to the action-submission system** - same
  posture as `TrainingProgress` "assumes an already-started assignment":
  there is no player-action dispatch path yet for either starting training
  or hiring, so both stay pure state-mutation methods a future action
  handler will call, not built as part of this slice.
- **Refresh cadence: every season**, alongside the other two personnel
  resolvers - fits "one turn = one season" the same way training/lifecycle
  already do, no special handling needed.

**Implemented (2026-09-30), `de.mkoehler.launchwindow.rules.HiringPool`
(headless, §3.10) — concrete numbers, all unconfirmed defaults pending
bot-harness tuning:**
- `HIRING_BATCH_SIZE = 6` (BASPM: 10). Four quality bands by average
  roster morale - `POOR_MORALE_CEILING = 44`, `AVERAGE_MORALE_CEILING = 64`,
  `GOOD_MORALE_CEILING = 84`, above which is Excellent - each with its own
  skill-value range for every generated candidate skill: Poor [10, 40],
  Average [25, 60], Good [40, 75], Excellent [55, 90]. **Deliberately kept
  all four ceilings comfortably above the §2.2(c) resignation floor (35)**
  - a caught-before-shipping consideration, not an afterthought: if every
  ceiling sat at or below the floor, a struggling-but-not-yet-resigning
  roster could never actually land in the Poor band, since almost nobody
  survives long below 35 morale to be averaged in. `EMPTY_ROSTER_FALLBACK_MORALE = 50`
  (lands in Average) for a brand-new agency with nobody hired yet.
  Candidate age 22-40, learning potential 20-80 (independent of quality
  band), starting morale 55-75 (the new hire's own attitude on joining,
  independent of the hiring group's morale, which only gates skill
  quality).
- **Roster capacity, decided at the user's own request (2026-09-30,
  answering a question this section deferred to exactly this point):**
  Astronaut 8, SET 12, Mission Control 8 (own numbers, no other system to
  derive them from yet - a building/budget-driven capacity mechanic is a
  real future possibility, not decided here). `hire` returns
  `HireOutcome.ROSTER_FULL` without mutating anything if the group is
  already at capacity.
- **New hires auto-start `BASIC` training (user decision, 2026-09-30) on
  a random skill from their own group** - which specific skill isn't
  specified by that decision, so the random choice is this slice's own
  unconfirmed default.
- **Hiring is free (user decision, 2026-09-30)** - no funds/salary model
  exists yet (§2.1), so no cost is charged; revisit once budget/funding is
  implemented.
- **A new `PlayerSnapshot.nextPersonnelId` counter, allocated through one
  `allocatePersonnelId()` helper, backs every id `HiringPool` generates** -
  a flat, monotonic, never-reused counter shared across all three groups
  (`"p42"`-style ids, not group-prefixed), needed because the async model
  lets a hire action resolve against a pool the player saw seasons ago; if
  ids were ever reused across a refresh, a stale hire action could
  silently hire the wrong person. **Not yet retrofitted onto the bot
  harness's own initial-roster generation** (still `"a"+i`-style, seeded
  independently of any `PlayerSnapshot` counter) - the two id styles don't
  collide (different prefixes: `"a"/"s"/"m"` vs. `HiringPool`'s `"p"`), and
  since the harness was wired to hire from `HiringPool` the same session
  (§3.10), a roster now genuinely does mix both styles at once in
  practice, not just hypothetically - still low risk, but worth unifying
  if that stops being true (e.g. a future id format that could collide).
- `HireOutcome` (`HIRED`/`UNKNOWN_CANDIDATE`/`ROSTER_FULL`) - `hire`
  returns a result rather than throwing, since an unknown candidate id is
  an expected, ordinary async-multiplayer case (a stale pool view), not an
  error.
- **Composition order for whoever wires this into one season pass (M4):**
  journaled actions (including `hire`) first, then `PersonnelLifecycle`,
  then `TrainingProgress`, then `refreshCandidates` last - a player can
  then always hire from the exact pool they were shown, and a season's
  refresh never discards a candidate that same season's own actions just
  hired. Not yet composed into a real per-turn season pass (that's M4
  scope) - the bot harness (§3.10) exercises this same order every season,
  but that's a test harness, not the production turn-resolution pipeline.
- 11 unit tests, fully headless: batch size, id non-reuse across refreshes,
  value ranges, the empty-roster fallback, all four band boundaries (a
  dedicated test per ceiling, checking both that the right band's range is
  hit and that a wrong higher band's range isn't), a successful hire
  (including the auto-`BASIC`-training side effect), an unknown id, an id
  from the wrong group's pool, a full roster, and seed reproducibility.

- **Wire-format note for whoever implements this:** `TrainingKind` is a new
  enum reachable from `PlayerSnapshot`, so it needs a `MessageRegistry`
  entry appended at the end of `register(Kryo)` (never inserted earlier -
  see that class's own Javadoc on registration order) and added to both
  class-list arrays in `MessageRegistryTest`. Kryo serializes enums by
  ordinal, so `TrainingKind`'s four values, once shipped, must only ever
  gain new constants at the end too, matching `AssignmentState`'s own
  existing constraint.

**Decided (user, 2026-09-28): state is mutable, not immutable records with
withers.** Turn resolution touches many fields per employee across a
potentially large roster every turn; the on-disk JSON snapshot (§3.7) is
already the immutable artifact, so in-memory state doesn't also need to be.
Content *definitions* (programs, mission components, goals, events), once
they exist, are expected to stay data-driven/immutable by contrast — only
per-player *state* is mutable.

**Deliberately deferred, not modeled yet** (formulas/balance numbers, and
shapes §2 still marks unconfirmed): mission outcome tiers (§2.5(c)), the
Expert exhaustion/falloff curve (§2.2(c)), the gold/silver/bronze goal
rating (§2.6(c)), programs/mission configurations/mission components/goals/
events as their own types, and all numeric balance values. Every class in
`de.mkoehler.launchwindow.model` has full Javadoc and a JUnit test under
`core/src/test/.../model`, per `CLAUDE.md`. **Partially superseded
(2026-09-30):** the age/morale/retirement/resignation slice of §2.2(a)/(c)
is now modeled and resolved — see §5's M2 status and the new
`de.mkoehler.launchwindow.rules` package. Skill growth, training-state
fields, hiring, and Expert remain deferred exactly as this paragraph
originally said. **Further superseded (2026-10-01):** programs/mission
configurations/mission components now exist as their own immutable types
(`de.mkoehler.launchwindow.content`) — see §5's M2 status. Goals/events
remain deferred exactly as this paragraph originally said. **Also
superseded (2026-10-01):** mission outcome tiers (§2.5(c)) are decided,
not unconfirmed — port BASPM's full five-tier ladder; the gold/silver/
bronze goal rating (§2.6(c)) is scrapped for v1, not merely deferred.

**Content-definition *types* now exist (2026-10-01) - real, authored
content still doesn't.** `de.mkoehler.launchwindow.content`'s
`ProgramCategory`/`Program`/`MissionConfiguration`/`MissionSeason`/
`MissionStep`/`MissionComponent` are built and validated from in-memory
fixtures only (see §5's M2 status); a
real content pack, and the loader reading these same shapes from files,
remain M3's job, re-deriving Launch Window's own actual values from
BASPM's Data Scripts XML *shapes* (`baspm.md` §"Data-driven architecture")
per §1.2.

### 3.5 Asset generation pipeline (early idea, not yet designed in detail)

**Idea (user, 2026-09-27):** BASPM's own asset bundles (`baspm.md` §4) give
Launch Window something most from-scratch generative-art pipelines don't get
for free — **a complete, pre-enumerated manifest of exactly which assets are
needed** (every portrait, icon, mission diagram, background, per category and
count), since BASPM already had to solve "what art does this kind of game
actually need." The proposal: a batch pipeline that takes a list of asset
definitions/descriptions (derived from that manifest, describing each asset
fresh in our own words — §1.2's no-copying rule applies here too, the
manifest tells us *what* to make, not what BASPM's version looks like) and
generates them via **ComfyUI** (HTTP API and MCP server both available,
either fits automation), with a **pragmatic mixed approach: automation to
generate candidates, a human in the loop to verify/select/regenerate**,
rather than either fully-manual art or fully-unattended generation. Not
being designed in detail yet — flagged here to revisit once §2's actual
content list (which programs/components/personnel/etc. Launch Window ships)
exists to generate a real manifest from.

**Initial thoughts, for whenever this gets picked up for real:**
- **Two complementary tools, split by fit, not one pipeline for everything
  (user, 2026-09-27).** **ComfyUI** for painterly/photographic content
  (personnel portraits, news illustrations, celestial/background art) —
  squarely diffusion models' strength zone. **Claude Design for UI and
  vector/icon work** instead — precise, legible, scalable output is exactly
  where ComfyUI is weakest and Claude Design is strongest: it can be taught
  a project's design philosophy/style once and then produce consistent
  output against it, as SVG for icons/UI chrome specifically, which scales
  cleanly to whatever resolution the game needs with no upscaling artifacts.
  **Real precedent, not a guess**: Robot-Rampage's entire UI look-and-feel
  and its board tiles/sprites were designed this way — a Claude Design
  canvas as the visual source of truth, its `Theme` design tokens (colors/
  spacing/type) carried into the actual client code, and a
  `tools/design-import` script pipeline (`rasterize.js`/
  `make-board-sprites.js`) converting the design output into the PNGs the
  game actually loads (see Robot-Rampage's own `CLAUDE.md`/`design.md` for
  the exact mechanics). Launch Window can very likely reuse that same
  import-pipeline *shape*, not just the idea. Assets needing pixel-level
  correctness against a non-visual constraint (e.g. a sprite silhouette that
  has to match a hitbox) may still need hand-finishing either way, but
  starting from a Claude-Design SVG is a much better base for that than a
  diffusion-model raster.
- **Consistency across a large batch is the sharpest hard problem on the
  ComfyUI side specifically** (Claude Design's own "teach it the project's
  style once" mechanism largely covers this for the UI/vector side), and
  worth solving *before* generating anything for real rather than after a
  pile of inconsistent output exists: a fixed style anchor (a small
  hand-curated reference set driving a LoRA, or reference-image conditioning
  via IPAdapter/ControlNet) baked into the pipeline from the first batch, not
  bolted on retroactively.
  - **Whatever reference set anchors that style, it must never be BASPM's own
    extracted art** (user + assistant, 2026-09-27) — training a LoRA (or
    IPAdapter-referencing) on BASPM's assets would bake its specific
    copyrighted expression into everything Launch Window then generates, which
    is §1.2's no-copying rule in a less obvious but no less real form.
    Practical path instead: generate an initial small batch across several
    representative subject types (a portrait, a rocket, a background, an
    icon) from prompting alone, hand-pick the best "keepers," and train the
    style LoRA on that self-generated, diverse set — diverse *subject matter*
    in the training set is also what makes a *style* LoRA generalize well in
    the first place (a same-subject training set risks the style bleeding
    together with that one subject's shape).
- **On the "verifying generated assets are really what we want" problem the
  user flagged as an open challenge:** a cheap first line of defense before
  a human ever looks at output is an automated check against each asset's
  own textual definition — a vision-capable model (could be Claude itself)
  doing a pass/fail/flag-for-review checklist per candidate, so the human
  reviewer's time goes to genuinely ambiguous cases rather than obvious
  misses. Worth prototyping early since it changes how much human time the
  whole pipeline actually costs per asset.
- **This is a genuinely interesting problem to build out, not just a means
  to an end** — worth treating "the asset pipeline" as a small project of
  its own with its own iteration, not a one-off script.

### 3.6 Turn scheduling & calendar configuration

Per §1, turn cadence isn't a single global constant — each game instance has
its own **schedule profile** plus an editable list of **blackout periods**.
This is a server-side concern (a scheduled job deciding "does a turn resolve
today"), not a client feature, so it lives here rather than in §2.

**Schedule profile (per game, set at creation, editable by the host later):**
- **Decision:** Two named presets to start:
  - `DAILY` — resolves every day of the week (fits the friends-group use
    case).
  - `WEEKDAYS` — resolves Monday–Friday only, no Saturday/Sunday turns
    (fits the work-colleagues use case: playable on a work laptop during a
    break, no weekend pressure).
  - Modeled as a weekday bitmask/set rather than a hardcoded enum of two
    values, so a future custom profile (e.g. "Tuesday/Thursday only") is a
    data change, not a code change — cheap to generalize now, not
    over-engineering, since it's the same data shape either way.
- Each profile also carries a **resolution time-of-day and timezone**
  **(Decision: host's local timezone at game creation, stored
  explicitly — never left as ambient server-local time**, since players may
  span timezones and "midnight UTC" silently drifting relative to a host's
  workday would be confusing).

**Blackout periods (per game, a list, editable *during* a playthrough):**
- A blackout is a **date range** (inclusive start/end date, in the game's
  configured timezone) during which no turn resolves, *regardless* of what
  the weekday schedule would otherwise say — covers national holidays or a
  player's vacation.
- **Must be addable mid-game, not just at game creation** (explicit user
  requirement, 2026-09-27) — e.g. a host adding a Christmas/New Year
  blackout mid-season, or blocking out a week once someone announces
  vacation. Implies blackout periods are their own persisted, mutable
  records (`ScheduleException` or similar), not baked into an
  immutable game-config blob written once at creation.
- **(Decision:)** Only future dates can be blacked out — a
  blackout can't retroactively undo a turn that already resolved. Adding one
  that overlaps *today* before today's resolution job has fired should still
  suppress today's turn; the scheduler's "does a turn resolve today" check
  needs to run its blackout lookup at trigger time, not from a snapshot
  taken earlier.
- **Who can edit the blackout list:**
  The server will have a json file that holds the blackouts (and potentially other server configurations). In the first version it's good enough for the host of the server to just manually edit this json file.

**The game's announced start date reuses this exact mechanism (user,
2026-09-27, §3.8's join-anytime decision):** a server can go live days before
a game's announced start, accepting account creation/slot-claiming during
that window, but its *first turn* shouldn't resolve early. No new mechanism
needed — this is just an automatic blackout spanning "server goes live"
through "the day before the announced start date," applied the moment the
game is created, same data shape as any host-added blackout above.

**What happens on a non-resolving day:** nothing special — it's not a
"skipped turn" needing its own state, it's simply a day the resolution job
doesn't fire. Players can keep reviewing/editing their already-queued
actions for the *upcoming* turn; nothing changes about their queued intents
across a blackout or weekend. The in-game turn counter only increments on
days the job actually runs, so "one turn = one season" (§1) stays exactly
true regardless of how many real days a given turn's window spanned.

**Decided (user, 2026-09-29): missed-day catch-up.** If the server itself is
down across more than one scheduled resolution day, only the single next day
resolves on the next check — the missed days in between are skipped, not
caught up in a burst. The rejected alternative, resolving one turn per
missed day on restart, would jump the group several seasons forward in one
go with no chance to submit actions in between.

**Architecture implication for §3.1, simplified by §3.9's one-game-per-server
decision (2026-09-27):** a single server instance only ever needs to track
*one* schedule profile and *one* blackout list — its own — not a per-game
loop juggling several. The scheduler is just: on a fixed cadence (e.g.
hourly), check *this* game's weekday profile + blackout list to decide
whether today's turn resolves, then fire the turn-resolution job. Running
the friends' game and the colleagues' game (each on their own schedule)
means running two separate server instances (§3.9), not one scheduler
managing two games — simpler than the "per-game-aware" framing this section
originally had, back when a single server was assumed to host multiple
concurrent games side by side.

### 3.7 Persistence & turn-resolution data model — decided (user, 2026-09-27): flat-file JSON, no database

**Decision: keep the server lightweight, matching the sibling projects' own
convention — no database.** The game world's state serializes to JSON and
persists as a flat file on disk, one snapshot per resolved turn, not a
live mutable DB row. This is a strong match for BASPM's own precedent, not
just sibling-project convention: `baspm.md` §2.1's real save-game analysis
already found BASPM's own save format is plain XML mapping almost 1:1 onto
its in-memory object graph, and its PBEM multiplayer mode (`baspm.md` §5) is
literally "pause, serialize the whole state, hand off, resume" over HTTP —
the exact same shape, just JSON instead of XML and file-storage instead of
file-transfer.

**Player actions as an append-only, one-file-per-action journal.** Each
submitted action (assign personnel, queue a mission, declare a scrub policy,
etc.) is written as its own JSON file rather than mutating one shared
"today's declared turn" blob. Two things this buys: **crash robustness**
(only the most recent write can ever be lost, never a whole day's declared
turn), and **natural separation** (one player's actions never collide with
another's on disk, and are trivially all-in-one-place — their own
subdirectory — for both login-time reconstruction and turn-resolution).

**Decided (user, 2026-09-27): actions are idempotent "declare current
intent" statements, not deltas.** E.g. an action isn't "add Employee X to
R&D" as a delta on whatever's currently declared — it's "R&D assignment for
slot N is now Employee X," a complete restatement. This is what makes both
replay use cases below work with zero extra machinery: replaying a player's
whole day's action journal in chronological order always converges on their
latest actual intent, because a later action for the same "slot" simply
supersedes an earlier one during replay — no separate cancel/retract event
type needed for "I changed my mind." Stated as an explicit rule for every
action type Launch Window ever defines, not discovered as a need later.

**Decided (user, 2026-09-27): each player's state resolves independently —
one snapshot file per player per turn, not one combined whole-game-state
blob.** Per BASPM's own architecture (`baspm.md` §2.1's `Player.cs`/
`GameWorld.cs`, and §5's PBEM precedent — two agencies' simulations that only
ever get *compared*, never directly touch each other's state) and matching
§2.9's current passive-competition-only default: a turn resolution for
player A only ever needs player A's last snapshot plus player A's action
journal for that day. A lightweight separate **game-meta file** (schedule
config/§3.6, blackout list, and whatever minimal per-player summary the
goals-comparison screen needs, `baspm.md` §11.4) sits alongside the
per-player files for the cross-player bits.
- **Sharper than originally written here (user design input, 2026-09-27):
  cross-player ordering turns out to never be a real problem, not just a
  problem deferred until §2.9 adds interaction.** The reasoning: even a
  future sabotage/interaction action is only ever *declared as intent*
  during the day (a plain action-journal entry, same as everything else) —
  its actual effect is computed once, by the server, at turn-resolution
  time, not applied as a live incremental mutation during the day. As long
  as that resolution step always reads the *target* player's **last
  confirmed (start-of-turn) snapshot** as its input — never a same-turn,
  already-in-progress result from resolving the target's own turn — there's
  no ordering dependency to resolve: each player's turn can be computed
  independently, in any order, even in parallel, exactly like the current
  passive-competition case. **The one real boundary case this doesn't
  cover, worth remembering if it ever comes up:** a mechanic where multiple
  players contend over one genuinely *shared, limited* resource within the
  same turn (not just "reads another player's snapshot," but "only one of
  us can actually get this") would still need an explicit tie-break rule —
  nothing currently proposed in §2.9's brainstorm is shaped like that, but
  if a future idea is, that's the actual trigger for revisiting this
  section, not "any interaction at all."

**Turn-resolution loop, concretely:** for each player independently — load
their last snapshot, replay/apply their day's action journal onto it (same
replay logic the login flow below already uses), run it through the actual
simulation/resolution logic (§2's various formulas), write the result as the
next snapshot, archive or delete that day's action-journal files. Per game,
this loop runs once per player, independently, whenever that game's schedule
(§3.6) says a turn resolves.

**Login flow, concretely:** load the player's last snapshot, replay whatever
exists in today's action journal on top of it (possibly empty, if nothing's
been submitted yet today), present that as "current state" to the client. No
special-casing for "first login of the day" vs. "checking back after already
submitting something" — it's the same replay operation either way.

**Security note** (flagged per this project's standing practice of
surfacing trust-boundary concerns during design, unprompted): action files
represent a player's *declared intent* only — the server must always
recompute the actual resolved outcome itself from that intent plus its own
simulation logic, never accept or trust a client-submitted "here's what
happened" result. This matters more, not less, in a self-hosted-among-
friends context, since a technically-inclined player is exactly the kind of
person who could otherwise hand-craft a favorable result file.

**Implementation detail worth flagging now rather than discovering it during
implementation:** writes need to be atomic (write to a temp file, then
rename into place) to avoid a half-written JSON file on disk if the server
crashes mid-write — otherwise the "only the last action can be lost"
crash-robustness goal doesn't actually hold.

**File layout, simplified by §3.9's one-game-per-server decision
(2026-09-27):** no multi-game top-level directory scheme needed — a server
instance only ever persists *one* game, so its whole data root just contains
that game's per-player snapshot/journal subdirectories plus its one
game-meta file directly. Running the friends' game and the colleagues' game
means two separate server instances with two separate data roots (§3.9), not
one shared root disambiguating between games by ID.

**Concrete per-player layout (unconfirmed default, from M1's
implementation, 2026-09-28 — this is a call the implementation made, not a
locked decision; reversible if a different shape turns out to fit better
once real network/scheduler code is layered on top):**
```
players/<playerId>/
  snapshots/<turn>.json      one ResolvedSnapshot envelope per resolved
                              game turn — the PlayerSnapshot itself, plus
                              the journal file name of the last action
                              applied to produce it (or null if none were
                              pending). Never overwritten once written and
                              never pruned; "current state" is always the
                              highest-numbered file. Turn 0 is the
                              bootstrap snapshot a new player starts from,
                              written by the slot-claim flow (M4) rather
                              than by any turn resolution.
  journal/<seq>-<uuid>.json   pending, not-yet-resolved actions; <seq> is
                              a zero-padded monotonic counter (not a
                              timestamp — two actions submitted in the
                              same millisecond still need a stable order).
  journal-sequence.txt        the last sequence number handed out for this
                              player, persisted and never reused or reset,
                              not even across an archive — see the
                              invariant below.
  journal-archive/<turn>/     that turn's journal files, moved here once
                              resolved
```
**Invariants future work must preserve:** journal file names must stay
fixed-width, zero-padded, sequence-number-first — crash recovery (below)
compares them as plain strings, not by parsing a sequence number back out,
so a format change here would silently break that comparison. **The
sequence number itself must never be reused or reset for a given player,
ever** — any future change to how the counter is persisted or seeded must
preserve strict, permanent monotonicity, or two different actions on
either side of an archive could share a sequence prefix and make the
crash-recovery cutoff comparison below ambiguous.

Resolving a turn is idempotent and crash-safe by construction: a turn's
snapshot file is written once and never replaced, so a crashed-and-retried
resolution detects "this turn's snapshot already exists" and skips
re-replaying/re-saving. It still finishes the job rather than leaving it
half-done: the envelope's saved cutoff tells the retry exactly which
journal files that turn actually applied, so it archives only those —
never an action submitted after the original attempt (which stays pending
for the next turn instead of being silently swallowed), and never fails
by re-archiving a file an earlier attempt already moved.

### 3.8 Player accounts & authentication

**Decided (user, 2026-09-27): persistent player accounts, minimal — Display
Name, Login, Password — deliberately mirroring the account system already
built and working in the StarWars sibling project** (`StarWars/server/src/
main/java/.../accounts/`: `PlayerAccount`, `AccountStore`, `PasswordHasher`,
`AuthResult`). This is the same author's own prior code, not BASPM-derived,
so directly reusing its actual shape (not just its idea) carries none of
`design.md` §1.2's copying concerns.

- **Account fields, deliberately minimal:** a server-assigned `id`
  (generated once at account creation, never reused/reassigned), `login`
  (unique), `passwordHash` + `passwordSalt` (the raw password is never
  stored, never logged), and a re-writable `displayName` — StarWars' own
  convention is that a successful login simply re-writes whatever display
  name was supplied that time, so "change how I present in-game" needs no
  separate edit-profile flow, just typing a different name next login.
  **Explicitly excludes anything game-specific** (StarWars' account also
  carries lifetime XP/kills/deaths/ship-unlocks, because those are
  cross-game persistent progression in that project) — **decided (user,
  2026-09-27): Launch Window's per-game choices (agency/faction, and
  anything else about a specific playthrough) live entirely in that game's
  own state, under the player's slot, not on the account.** An account's
  only job is identity.
- **Password hashing: salted SHA-256**, reusing `PasswordHasher` directly —
  StarWars' own documented reasoning ("good enough for this project's threat
  model — a hobby dedicated server, not a target worth pulling in bcrypt/
  Argon2 for") applies at least as strongly here, arguably more so (an
  invite-only, non-real-time, small-group game is a lower-value target than
  even that).
- **Storage: reuse `AccountStore`'s pattern directly** — a single JSON file
  mapping login → account, held as an in-memory synchronized map (no disk
  I/O on the calling thread), flushed to disk on a fixed background cadence
  with a dirty flag so an unchanged store never rewrites, periodic
  retention-bounded rolling backups, and a JVM shutdown hook for one final
  synchronous flush on a clean stop (`docker stop` included) so only a hard
  crash can lose anything since the last scheduled flush. No libGDX
  dependency — plain `Path`/Jackson I/O, directly unit-testable exactly as
  StarWars' own `AccountStoreTest` already does. This is `design.md` §3.7's
  flat-file-JSON-via-Jackson philosophy applied to a third concern, alongside
  per-player game snapshots and the action journal, not a new persistence
  idea.
- **Login flow: reuse the exact same semantics** — a login name that doesn't
  exist yet auto-creates the account from the supplied password + display
  name; an existing login requires the password to match, or the attempt is
  rejected. **Decided (2026-09-29): a login is restricted to
  `[A-Za-z0-9_-]{1,32}`**, and a blank/null display name is rejected. The
  login charset restriction is defense in depth, not the actual mapping
  mechanism (see below) — it still matters because a rejected-but-not-yet-
  mapped login is a cheaper failure than a malformed one reaching a path
  operation later.
- **Decided (user, 2026-09-29): a per-player data directory
  (`players/<id>/`, §3.7) is keyed by the account's server-assigned `id`,
  never by the login directly.** A login-derived directory name would have
  meant either normalizing case everywhere (simpler, but makes two logins
  differing only in case the same identity - `Mario`/`mario` collide on
  NTFS, this laptop's filesystem, but not on the Linux server, §3.9's
  Docker deployment) or accepting that ambiguity. A stable, server-assigned
  id sidesteps the whole question, and also means a login can be renamed
  later without moving or migrating that player's data directory.
- **Slot claiming — superseded (user, 2026-09-27): dynamic slot count, and
  joining stays open for the entire life of the game, not just before it
  starts.** The earlier default here (a configured max-player-count, first
  logins claim a pre-existing slot) assumed a fixed player pool decided at
  game creation — **real past experience says that assumption doesn't
  hold**: people hear about an ongoing game later (word of mouth, "the
  coffee kitchen") and want to join mid-game, accepting they're starting
  from a serious, possibly un-recoverable disadvantage rather than being
  turned away. So: **no upper bound on player count, and no cutoff after
  which joining closes.**
  - **Concrete flow:** the host announces a new game for a given **start
    date**; the server itself goes live a few days earlier, but — per §3.6's
    scheduling mechanism — its *first turn doesn't resolve until that start
    date* (mechanically identical to an automatic blackout period covering
    "server-live" through "the day before start," no new mechanism needed).
    That gap is the signup window: an account that doesn't exist yet gets
    created on first login (§3.8 above), then immediately walks through
    **claiming a slot** — picking an agency archetype (NASA-style or
    Soviet-style only, per §2.1(a1)/(c1)'s decision to drop the GSA-
    equivalent third archetype entirely), and naming both the agency and the
    player's own in-game persona/callsign. An existing
    account with an existing slot just logs into it as before; a login that
    doesn't have a slot yet in *this* game always gets the claim flow,
    whether that's on day one of the signup window or in the middle of an
    already-months-long game.
  - **(Decision:) Agency name and persona/callsign must be unique
    within a given game** (first claimed, first served — hence the "already
    taken" joke), but nothing about identity needs to be unique *across*
    different games.
  - **A late joiner's own season/turn count starts fresh from their personal
    turn 1, decoupled from the game's shared wall-clock schedule (§3.6).**
    The schedule (which real days produce *a* turn) is one shared clock for
    the whole game; how many turns any given player has personally played is
    a separate per-player quantity. A player joining on the game's calendar
    turn 40 starts their own personnel/budget/program state at their turn 1,
    which then resolves alongside everyone else's current turn on every
    subsequent shared schedule tick — they're simply far behind on content,
    not on a different clock.
  - **Open joining is the intended default posture, not a gap to close**:
    the earlier allowlist idea assumed a small, fixed, pre-vetted circle,
    which this design explicitly no longer assumes. An invite-only/allowlist
    mode is worth keeping as an optional host toggle for whichever group
    wants it, not the default.
  - **Decided (user, 2026-09-27): agency archetypes are non-exclusive.**
    BASPM itself only has three real factions (NASA/Soviet/GSA, `baspm.md`
    §2.4) — nowhere near enough seats for §1's ~10-player colleagues group if
    archetypes had to be exclusive per game. Confirmed instead: several
    players can pick the same historical/flavor archetype, distinguished
    entirely by their own unique agency name and persona, same as how a
    board game lets multiple people play "the same faction" flavor-wise in a
    free-for-all. **Whether an archetype carries a real mechanical
    difference — resolved by the later §2.1(a1)/(c1) agency-differences
    research pass (2026-09-27), not still open as this bullet originally
    said:** yes, a real one, not pure flavor — program/mission-component
    access is gated by archetype (NASA/Soviet each only see their own tagged
    content), while the economic side (funding formulas, costs) stays
    identical across both, matching what BASPM's own shipped game already
    does. See §2.1(a1)/(c1) for the full trace; that pass also fully
    resolved `design.md` §4's "how much of BASPM's Cold War framing to keep
    vs. reskin" question as far as archetype scope goes.
  - **Ties into the "game host" role concept (§3.6/§4):** announcing a game
    and setting its start date is itself a "host" action, and so is editing
    the blackout list — **for v1 both are just direct server-file/manual
    access (blackout editing decided §3.6, 2026-09-28: the host edits the
    server's own JSON config directly), not an in-game permission system.**
    A real lightweight "game host" role/UI is still open for later, but
    blocks nothing about a first playable version.
- **Client-local settings (volume, display resolution, keybindings, etc.)
  are explicitly out of scope for accounts entirely (user, 2026-09-27)** —
  a plain local JSON file on the client machine, no server or account
  involvement at all.

### 3.9 Deployment: one game per server instance

**Decided (user, 2026-09-27): a running server instance hosts exactly one
game — no multi-tenancy inside a single process.** Supporting §1's two
target groups running concurrently (the friends' game and the colleagues'
game) means running two separate server instances — e.g. two Docker
containers — not one server juggling several games internally. Real,
already-used precedent for exactly this deployment shape: StarWars already
ships its own dedicated-server `Dockerfile`.

This is the decision that simplified §3.1 (a resident process, but a single
one per game, not a multi-tenant one), §3.6 (the scheduler tracks one
schedule profile/blackout list, not a per-game loop), and §3.7 (no
multi-game directory layout needed) — each cross-references back here as the
originating decision, recorded once in full instead of three times.
Rationale, as the user put it: keeps the server itself simpler (no
multi-tenant game-selection/routing layer to design or build at all), and
scaling across groups becomes infrastructure-level parallelism ("start
another container") rather than an application-level concern.

### 3.10 Testing strategy: automated bot-driven playthroughs

**Decided (user, 2026-09-27): the core gameplay loop must be automatable
from the start** — not just unit-testable in isolation, but drivable
end-to-end by simple bots across many simulated turns/seasons, so balance
questions (does a multi-season mission actually resolve correctly across
turn boundaries? does an agency's budget trajectory make sense over 50
simulated seasons? does one archetype snowball relative to another?) get
answered by running hundreds of automated games, not by a human manually
playing for hours to reach the endgame.

**Real, working precedent to follow, not a fresh design: Robot-Rampage's
`Playouts`/`BoardMetrics` harness** (`dev-tools/.../analysis/Playouts.java`,
`BoardMetrics.java`). The mechanism, precisely:
- A pure, libGDX-free simulation loop (`Playouts.run`): builds a game state,
  then repeatedly deals cards, asks a `BotBrain` for a decision per seat
  (always at `BotDifficulty.HARD`, regardless of what a lobby bot happens to
  be set to — so the metrics' calibration never silently drifts with an
  unrelated difficulty setting), submits it, and calls the *real*
  `TurnResolver.resolve(state)` — the exact same resolution code the real
  server uses, not a simplified stand-in.
- Everything is seeded, so the same input always reproduces the same
  numbers — deterministic, reproducible comparisons across runs.
- The result is a small, purpose-built `Report` record (games played, how
  many finished within a turn cap, average turns to finish, deaths by cause,
  wins by seat, a fairness measure like the gap between the best/worst
  seat's average) — not a generic dump, a handful of numbers actually chosen
  to answer "is this balanced/does this work," decided in advance.
- **The structural precondition this depends on, worth adopting
  deliberately**: the rules engine (`rules` package) has zero libGDX
  dependency and is unit-tested directly — this is what makes hundreds of
  simulated games run in about a second each, with no window, no rendering,
  no real time passing at all. Robot-Rampage's own testing-strategy section
  states the mirror image explicitly: "Not tested: screens, rendering,
  animation, Scene2D wiring." **The same split should apply to Launch
  Window's own core simulation logic once it exists (§3.4, not started
  yet): the turn/season-resolution engine (mission simulation, personnel,
  budget, goals — `baspm.md` §8-§13's traced formulas) needs to be plain,
  headless-simulatable Java from day one**, never entangled with any client
  rendering code — this is the one architectural principle from this
  session worth actively designing for before the rules engine's first line
  is written, not retrofitting later.

**First slice started (2026-09-30), right after the first rules-engine
resolver existed, per this section's own sequencing rule.**
`dev-tools`'s new `de.mkoehler.launchwindow.devtools.analysis` package,
matching Robot-Rampage's placement exactly, holds
`PersonnelLifecyclePlayouts`: drives `PersonnelLifecycle.advanceSeason`
(§2.2(c)'s new resolver) across many random rosters/seasons, seeded for
reproducibility, with a trivial "bot" (each employee independently has a
per-season chance of toggling idle/busy — just enough churn to exercise
both the morale-gain-while-busy path and the (at the time) idle-only
resignation/retirement checks, not a stand-in for a real per-turn
decision, which doesn't exist yet). **Historical, pre-reversal**: this
paragraph describes the harness as first built, before both
`TrainingProgress` was wired in and the departure-protection rule was
reversed later the same day - see both entries below for the current
shape. Reports a small `PersonnelPlayoutReport` — games
played, resigned/retired counts and their average tenure in seasons,
still-employed count — plus self-checking invariants (morale bounds, every
reported departure actually matching the threshold it claims) that throw
if violated, catching what a handful of hand-picked unit tests might miss
across many random combinations. **Deliberately named after the one
resolver it drives, not yet the general full-game harness this section
originally described** — that needs a real bot making per-season decisions
like "queue a mission" or "open a program," none of which exist yet;
expected to grow/generalize alongside M2's remaining slices rather than
staying permanently resolver-specific. **First real tuning numbers seen
(2026-09-30, seed 42, 200 games, 80 seasons, 5 employees/group/game, 3,000
employees total):** 202 resigned (avg. tenure 31.0 seasons), 875 retired
(avg. tenure 54.5 seasons), 1,923 still employed at the 80-season cap —
a plausible-looking first-pass distribution, not yet judged against any
real target (there isn't one yet), but exactly the kind of number this
harness exists to produce. **Caveat, worked out the same day these numbers
were first seen:** these mostly reflect the harness's own bot/roster
choices, not the underlying game-balance constants yet. **SET can't retire
at all under this particular run's settings** - starting ages are 24-45
and SET retires at 70, a 25-year/100-season gap the 80-season cap never
reaches, so every SET retirement in a longer run only starts becoming
possible past season 100 (equivalently, past 25 real years at one turn per
season). And at the bot's roughly 50/50 idle/busy equilibrium (a 30%
per-season toggle chance), morale drifts *upward* on average (−2 idle,
+3 busy nets to +0.5/season at 50/50) - resignation in practice only fires
after an unusually long idle streak, which is a real, useful finding about
the current numbers (§2.2(c)'s morale deltas function more as a "sustained
neglect" penalty than a routine one at this bot's churn rate), not a bug.
**This equilibrium shifted further toward busy once `TrainingProgress` was
wired in** (below): an employee who starts training can no longer be
interrupted, so actual busy-time is somewhat above 50% once training spells
(1-3 seasons each) are accounted for, pushing the average morale drift
still further upward - the same "sustained neglect" reading holds, just
more so.

**`TrainingProgress` wired in the same day, once it existed (2026-09-30):**
the bot got real per-employee decisions to go with it — idle becomes either
assigned or a genuine fixed-length training assignment (a random kind,
random target skill from that employee's own group, the correct per-group/
per-kind length); assigned becomes idle; an employee already training is
never interrupted (no pull-out path exists yet, so training always runs to
completion). Both resolvers run every season in the documented composition
order (`PersonnelLifecycle` then `TrainingProgress`, §2.2(c)), sharing one
seeded `Random` so a whole game stays reproducible from its seed. Two new
self-checking invariants: every skill stays in `[0, SkillGrowth.SKILL_CAP]`,
and `AssignmentState.IN_TRAINING` agrees with a non-`NONE` training kind and
a non-null target skill in both directions — running clean across many
random combinations is itself evidence the `isActivelyTraining` consistency
guard (§2.2(c)) and the bot's own start/stop logic agree with each other.
**Updated tuning numbers, same seed/game shape as above** (the RNG stream
shifted once `TrainingProgress` started consuming from it, so these
supersede the earlier run, not just add to it): 262 resigned (avg. tenure
37.6 seasons), 875 retired (avg. tenure 55.6 seasons), 1,863 still employed,
and — the new number — 18,963 training completions, average gain 17.50
skill points per completion. **That first average gain turned out to be
misleading, caught the same session:** `newRandomSnapshot` never seeded any
starting skill values (everyone began at 0) and gave everyone a flat
learning-potential of 50, so 17.50 was mostly first-training-round gains
starting from zero, and the new skill-cap invariant almost never actually
exercised itself (nobody got near 100). **Fixed the same session**: starting
skills now randomize across 20-90, learning potential across 20-80 (both
per employee, not flat). **Numbers after that fix, same seed/game shape**:
273 resigned (avg. tenure 31.8 seasons), 884 retired (avg. tenure 55.4
seasons), 1,843 still employed, 18,329 training completions, **average gain
7.78** — the more realistic number, now that most training rounds start
from a non-zero skill and genuinely feel the diminishing-returns curve.
The completion count itself is still large mostly because SET's
basic-training length is only 1 season (§2.2(c)) - worth keeping in mind if
that constant ever gets retuned, since it will move this number a lot.
**Also corrected the same session:** Mission Control's basic-training
length was briefly 2 seasons, landing on BASPM's own exact value for that
group by coincidence — re-derived to 3 instead before any of the numbers
above were finalized, per §1.2's no-verbatim-match rule; see §2.2(c)'s own
entry for the full note. `BASIC` training's completion behavior (the same
diminishing-returns gain `ADVANCED` uses) was flagged as this session's own
invented shape, not something `baspm.md` traces for basic training
specifically — **decided (user, 2026-09-30): keep it, revisit once the
hiring pool exists** (§2.2(c)).

**Departure-protection rule reversed the same session (user decision,
2026-09-30, §2.2(c)) — the harness's own invariant check updated to match,
independently re-deriving the new rule rather than calling the resolver's
own predicate** (renamed `busyEmployeeKeys`/`checkInvariants`'s "busy never
departs" wording to `protectedEmployeeKeys`/"protected never departs" -
`group != SET && state == ASSIGNED`, computed separately in
`PersonnelLifecyclePlayouts` itself). SET's retirement age also dropped
from 70 to 65 the same session. **Numbers after both changes, same seed/
game shape as above:** 234 resigned (avg. tenure 33.2 seasons), **992
retired** (avg. tenure 56.4 seasons, up from 884 - training and SET's
own assignment state no longer protecting anyone pushed many more
employees past their retirement age before the season cap), 1,774 still
employed, 18,237 training completions, average gain 7.79 (essentially
unchanged, as expected - the growth formula itself didn't change). **The
earlier "SET can't retire at all under this run's settings" caveat above
is now only barely avoided, not comfortably true**: with SET's own age now
65 (not 70) and starting ages 24-45, only an employee who started at
exactly the oldest age (45) reaches retirement exactly at the 80-season
cap's edge (45 + 20 years = 65) - still not a comfortable margin, so the
same underlying finding holds (§2.2(c)'s own open question: is a realistic
game long enough for SET to actually retire meaningfully?), just less
extreme than the original 70-age/100-season gap.

**`PERMANENT` training wired into the bot's own decisions the same session
it was implemented (2026-09-30):** starting a training assignment now picks
evenly among `BASIC`/`ADVANCED`/`PERMANENT` (falling back to `ADVANCED`
when the target skill is already too close to `SkillGrowth.SKILL_CAP` to
leave room for a meaningful target level); `PERMANENT`'s own target level
is drawn strictly between the current skill and the cap. A new invariant
(`checkPermanentTrainingTarget`) checks, independently of the resolver's
own logic, that any still-`PERMANENT`-training employee has a target level
in range and hasn't already reached it - `TrainingProgress` should have
finished their training already if so. **Numbers after wiring it in, same
seed/game shape as above:** **144 resigned** (down sharply from 234, avg.
tenure 24.2 seasons), 980 retired (avg. tenure 57.1 seasons, essentially
unchanged - retirement is age-driven and runs regardless of assignment
state for the reasons already established), 1,876 still employed, and
**34,747 training completions** (nearly double, avg. gain 5.35, down from
7.79). **The real finding, not just noise:** `PERMANENT` training
substantially suppresses resignation, because an employee re-enrolled in
it spends far more of their career busy/training (gaining morale each
season) than periodically cycling idle↔`BASIC`/`ADVANCED`↔idle would -
resignation depends on accumulated idle seasons, and `PERMANENT` all but
removes idle seasons for whoever the bot assigns to it. Worth keeping in
mind once real balance tuning starts: `PERMANENT` isn't just a training
convenience, it's a genuine low-effort way to keep staff from leaving.

**`HiringPool` wired in the same session it was implemented (2026-09-30):**
each season the bot may first hire one random candidate out of each
group's current pool (25% chance per group, a stand-in for a journaled
player action — a no-op on an empty pool, i.e. season 0 before any refresh
has happened, or a full roster), then `PersonnelLifecycle` and
`TrainingProgress` run as before, then `HiringPool.refreshCandidates` runs
last — the exact composition order `HiringPool`'s own Javadoc documents.
Three new invariants, all computed independently of `HiringPool`'s own
logic rather than trusting it: every id is globally unique across all
three rosters *and* all three candidate pools (the shared
`PlayerSnapshot.allocatePersonnelId()` counter must never repeat); each
pool holds exactly `HIRING_BATCH_SIZE` candidates after a refresh; and
every candidate's skills fall inside the range the harness's own
independently-reimplemented morale-to-band mapping would select for that
group's current roster (this required promoting `HiringPool`'s per-band
skill-range constants from `private` to `public`, since they're plain
tuning numbers the harness needs to cross-check against, not the kind of
internal decision logic — like the departure-protection predicate — that
must stay off-limits to keep the check genuine). **Tenure accounting
needed a fix here, caught by the advisor before committing:** the
departure-tenure calculation (`season + 1`) implicitly assumed every
employee joined at season 0, which was true before hiring existed but
silently over-counts tenure for anyone hired mid-game; fixed with a
per-game `group:id -> join season` map, seeded at 0 for the starting
roster and updated on every successful hire, so tenure is now
`(departure season + 1) - join season`. This changes only the two average-
tenure figures below, not any count (a map lookup doesn't consume from
`Random`). **Numbers after wiring it in, same seed/game shape as above:**
259 resigned (avg. tenure 29.3 seasons, corrected from an initial,
uncorrected 35.7), 1,271 retired (avg. tenure 55.7 seasons, corrected from
57.6 — both counts up from 144/980, since hired replacements now
themselves age toward retirement/resignation within the same 80-season
cap), 5,358 still employed (up sharply — hires now add to the roster over
time, not just replace departures 1:1, since `HIRE_CHANCE_PER_SEASON` and
roster capacity are independent of the current departure rate; note this
is ~96% of the 5,600-employee capacity ceiling this run's settings imply
— 200 games × 28 total roster slots (8+12+8) — so "still employed" here is
now largely a *capacity-saturation* number, not a pure attrition one),
70,424 training completions (avg. gain 5.25 — lower, since every new hire
auto-starts a fresh `BASIC` round at a random skill, diluting the average
with more early-career, larger-headroom gains), **3,888 successful
hires**. **Band-hit distribution is the real finding here:** across all 3
groups × 200 games × 80 seasons, refreshes landed in "Poor" 0 times,
"Average" 575 times, "Good" 20,836 times, and "Excellent" 26,589 times —
the bot's rosters essentially never drop into the bottom two quality
bands. Part of this is the bot's own churn (`BUSY_MORALE_GAIN_PER_SEASON`
outweighing `IDLE_MORALE_LOSS_PER_SEASON` at this bot's rate, the same
equilibrium noted above, keeps average roster morale high most of the
time), but the "Poor" band is also *structurally* narrow regardless of bot
behavior: anyone whose morale drops to the §2.2(c) resignation floor (35)
or below resigns and leaves the average entirely, so landing a group
average at or below `POOR_MORALE_CEILING` (44) requires nearly the *whole*
roster to be sitting in the narrow 36-44 band simultaneously, not just a
few struggling employees dragging the average down — a genuinely rare
event by construction, not only by this bot's behavior. Worth keeping in
mind once real balance tuning starts: the reachable "Poor" window is only
ever the 9-point gap between the resignation floor (35, below which
nobody survives to be averaged in) and `POOR_MORALE_CEILING` (44) —
raising the ceiling widens that window and makes "Poor" reachable more
often; moving it *closer* to the floor would only narrow the window
further. If the game is meant to make hiring noticeably worse for a
roster that's merely somewhat unhappy (not uniformly near-resignation),
this pairing of constants needs revisiting - the same "keep the ceiling
comfortably above the floor" reasoning in §2.2(c)'s own `HiringPool` entry
turns out to need *more* headroom than the 9 points currently given, not
less.

**`ProgramOpening` wired in the same session it was implemented
(2026-10-01):** a small fixture `ContentCatalog` (three programs, four
mission components - not real content, see §5's M2 status for the exact
shape) lets the bot open one random not-yet-open program per season at a
10% chance, in the same journaled-player-action bucket as
`HiringPool.hire`, before `PersonnelLifecycle`/`TrainingProgress`/
`HiringPool.refreshCandidates` run. Four new invariants, computed
independently of `ProgramOpening`'s own logic: funds never negative;
every opened program id is one the catalog defines; every owned
component's reliability and floor stay ordered
`baselineReliability <= floorReliability <= currentReliability <=
maxReliability`; every owned component is required by at least one
currently opened program.

**Only one of the fixture's two reliability-transfer relationships can
ever actually fire.** `satellite-explorer`'s transfer from
`satellite-sputnik` fires whenever `sputnik-program` opened in an earlier
season than `explorer-program`, since by then `satellite-sputnik` is
already owned. `capsule-mercury`'s transfer from `rocket-redstone` can
**never** fire: `mercury-program` grants both components in the very same
`open` call, and `ProgramOpening` computes every transfer within one call
against ownership as it stood *before* that call - a deliberate
illustration of that rule living inside the fixture itself, not a flaw to
fix by reshaping it.

**Numbers (same 200-game/80-season run as the figures above, now shifted
by this slice's own `random.nextDouble()` calls consuming the shared
random stream earlier each season - no personnel resolver's own behavior
changed, since funds don't feed any of them): 236 resigned (avg. tenure
29.7 seasons), 1,242 retired (avg. tenure 55.9 seasons), 5,371 still
employed, 69,961 training completions (avg. gain 5.24), 3,849 hires,
hiring-pool band hits 0/597/21,001/26,402 (poor/average/good/excellent),
and 599 programs opened.** The fixture's three programs cost at most
2,300 combined even with no discount applied, well under the 5,000
starting funds, so almost every game opens all three (599 of a possible
600) well before running out of money; the `INSUFFICIENT_FUNDS` path is
exercised by `ProgramOpeningTest`'s own unit test, not meaningfully by
this harness run. A future real content pack's actual program costs,
once authored (M3), will likely make funds a genuine constraint the way
they aren't here.

**`ResearchProgress` wired in the same session it was implemented
(2026-10-01):** `shuffleSetSpecialist`'s own reassignment move now picks a
random *owned* mission component when moving a `SET` specialist from idle
to assigned - falling back to starting training instead when nothing is
owned yet (no program opened in that game so far) - and clears
`rdTargetComponentId` on the move back to idle, so the
`AssignmentState.ASSIGNED` &hArr; non-null-and-owned-target pairing never
drifts apart; a new invariant checks that pairing every season,
independently of `ResearchProgress`'s own logic. The existing reliability-
range/floor-ordering invariant (already added for `ProgramOpening`) now
genuinely exercises R&D's own output too, not just grant-time values, since
it runs every season after R&D may have grown a component's reliability.

**Numbers (same 200-game/80-season run as the figures above, shifted
again, this time partly for a behavioral reason, not only a random-stream
one): 287 resigned (avg. tenure 29.2 seasons), 1,269 retired (avg. tenure
55.9 seasons), 5,345 still employed, 72,258 training completions (avg.
gain 5.16), 3,901 hires, hiring-pool band hits 0/608/21,560/25,832
(poor/average/good/excellent), 597 programs opened, and 37,300 R&D
seasons completed (avg. reliability gain 1.65).** The behavioral part: a
`SET` specialist can only go `ASSIGNED` once at least one program is
open and a component is actually owned, which it wasn't at all before
this wiring - early-season `SET` specialists that would previously have
gone `ASSIGNED` (idle-morale-loss-avoiding busywork with no real target)
now more often train instead, changing morale/resignation timing for that
group specifically, on top of the usual random-stream shift from the new
draws themselves. the general full-game harness described in this
section's original text below — simple bots making plausible per-season
decisions (assign personnel, queue a mission), run many simulated
multi-season games from a seed, and reporting the metrics that actually
matter here: whether a multi-season mission completes correctly across
turn boundaries (a real, non-trivial case per `baspm.md` §8.1's finding
that missions commonly span several seasons), funds/prestige trajectory
over N simulated seasons (does an agency go bankrupt, does the
budget-review math actually converge sensibly), and whether the NASA-style
and Soviet-style archetypes (§2.1(a1)/(b1)'s real content differences)
actually play out roughly evenly rather than one snowballing. "Open a
program" is already in this harness, as of the entry above; blocked on
the mission-simulation and budget resolvers not existing yet (§5's M2
slice list).

**`ResearchProgress`'s multi-slot upgrade wired in the same session it
shipped (2026-10-01):** `shuffleSetSpecialist` now claims the first free
R&D slot (0-3) on the randomly-picked owned component when moving a `SET`
specialist from idle to assigned, via a new `firstFreeRdSlot` helper that
scans the roster fresh each call - falling back to starting training
instead, same as before, both when nothing is owned yet *and* now also
when the chosen component's four slots are already full. Clears
`rdSlot` alongside `rdTargetComponentId` on the move back to idle. Two
invariants replace the old single pairing check: `rdSlot` is set, in
range, if and only if the specialist is `ASSIGNED` with an owned target
(computed independently of `firstFreeRdSlot`'s own allocation logic, so
it doesn't just confirm the bot agrees with itself), and no two
specialists ever claim the same (component, slot) pair.

**The release-at-the-cap mechanic (`ResearchComponentMaxed`, see §3.4)
needed no harness-side wiring of its own** - it's entirely internal to
`ResearchProgress.advanceSeason`, so a freed specialist simply shows up
`IDLE` with a cleared target/slot the next time `shuffleSetSpecialist`
looks at them, same as any other idle specialist. The one real harness
fix it forced: the season loop's own reliability-gain aggregation was
blindly casting every event in `ResearchProgress`'s return list to
`ResearchCompleted`, which threw a `ClassCastException` the first time a
`ResearchComponentMaxed` event appeared in a real run (caught by this
slice's own unit tests failing, not by the harness itself) - fixed to
skip non-`ResearchCompleted` events via `instanceof` instead.

**Numbers (same 200-game/80-season run, shifted twice more after this
paragraph was first written):**
1. First shift, same day: multiple `SET` specialists can now stack on one
   component's R&D and the new two-ceiling cap changes how long it keeps
   paying off - 259 resigned, 1,277 retired, 73,867 training completions,
   36,310 R&D seasons completed.
2. Second shift, a real bug fix caught in the post-implementation advisor
   pass: `ResearchProgress` was silently dropping the `ResearchCompleted`
   event - and with it, the already-applied flat skill gain - for any
   specialist whose component had zero nominal reliability gain this
   season (e.g. one already sitting at `maxRdReliability`). Fixed to
   always emit one event per occupied slot, with a `0` reliability share
   when there's nothing to distribute - 36,347 R&D seasons completed.
3. Third shift, the release-at-the-cap mechanic (see below) landing the
   same advisor pass: freed specialists go back to `IDLE` and get
   reshuffled by the bot sooner (often straight into training, which
   raises morale) instead of sitting parked on a maxed component doing
   nothing - a real behavioral change, not just a random-stream shift.

**Final numbers for this slice: 223 resigned (avg. tenure 31.3 seasons),
1,244 retired (avg. tenure 55.2 seasons), 5,365 still employed, 73,553
training completions (avg. gain 5.11), 3,832 hires, hiring-pool band hits
0/573/20,384/27,043 (poor/average/good/excellent), 599 programs opened,
and 36,285 R&D seasons completed (avg. reliability gain 1.53).**

**Mission simulation wired in (2026-10-02):** `MissionLaunch`/`MissionProgress`
added to the harness, completing §2.5(f)'s own M2 exit criterion - a
multi-season mission spanning several turns, now actually driven by the
bot. Three new fixture mission configurations (`sputnik-launch-std`,
one season, uncrewed, controller-involved; `explorer-mission-std`, two
seasons, uncrewed; `mercury-mission-std`, two seasons, crewed, every step
flagged crew-risk - see `buildCatalog()`'s own Javadoc for the exact
step/skill composition, original structure per `design.md` &sect;1.2, not
copying any real BASPM mission's own layout). `maybeLaunchMission` is the
new journaled-action stand-in (same bucket as `maybeHire`/
`maybeOpenProgram`): picks a random configuration whose program is open
and exactly as many random idle astronauts/controllers as its own
declared `requiredAstronautCount`/`requiredMissionControlOfficerCount`
(via `pickExactlyIdleIds`), same "ordinary outcome, not an error" posture
when not enough idle personnel exist to meet either count.
`MissionProgress.advanceSeason`
runs right after `PersonnelLifecycle`, per `design.md` &sect;2.5(f)'s
decided composition order. **A real bot-shape change this slice forced**:
an Astronaut/Mission Control officer's idle-to-`ASSIGNED` transition used
to happen directly inside `shuffleAstronaut`/`shuffleMissionControlOfficer`
(a per-employee coin flip); since `ASSIGNED` now always points at a real
`InProgressMission`, that transition moved entirely into
`maybeLaunchMission` - the per-employee shuffle now only ever decides
idle vs. training, never idle vs. assigned.

**New invariants:** `checkMissionInvariants` (roster-size delta matches
reported `LOST_IN_ACCIDENT` departures exactly, each one astronaut-only
with that reason) and `checkMissionAssignmentConsistency` (the
bidirectional `ASSIGNED`/`assignedMissionId` pairing, from both the
person's own side and every `InProgressMission`'s own assigned-id lists;
nobody assigned to two missions at once; every mission's season index
within its own configuration's range) - the same "re-derive independently,
don't call the resolver's own logic" posture every earlier invariant in
this harness uses.

**Numbers, first pass (200-game/80-season/seed-42 run, crew/controller
counts still content-derived, no retire-on-release yet): 2,089 missions
launched, mission steps resolved 1,567 successful / 400 minor-glitch-
solved / 451 minor glitch / 673 failure / 40 catastrophic (all 40 from
`mercury-mission-std`, the only crew-risk configuration - sputnik/
explorer can only ever downgrade to `FAILURE`), 57 astronauts lost in
accidents.**

**The personnel numbers from that first pass were not comparable to the
pre-mission-sim numbers quoted earlier in this section, and the direction
of the shift was the opposite of what a first reading suggests:** 1,047
resigned (vs. 223 before)/1,038 retired (vs. 1,244)/5,342 still employed
(vs. 5,365)/84,142 training completions (vs. 73,553)/4,484 hires (vs.
3,832)/hiring bands 0/2,127/33,084/12,789 poor-average-good-excellent
(vs. 0/573/20,384/27,043)/594 programs opened (vs. 599)/35,765 R&amp;D
seasons (vs. 36,285). Resignations roughly *quadrupled*, not dropped, and
the Excellent hiring band fell by more than half - **more idle time, not
less.** The cause is the bot-shape change itself, not a bug: Astronaut/
Mission Control officers lost their old unconditional coin-flip into
`ASSIGNED` (which granted the busy-morale bonus to roughly half of them
every reassignment roll), and now reach `ASSIGNED` only when
`maybeLaunchMission` actually fires and picks them - every other season
they sit `IDLE`, losing morale. A genuine side effect of making
`ASSIGNED` mean something real, not a defect in this slice's own
correctness, and still true after the two fixes below.

**Two balance holes this first pass surfaced were both resolved the same
day, not left open - see §2.5(f)'s own entries:** the crew-size question
became the exact-headcount decision (`requiredAstronautCount`/
`requiredMissionControlOfficerCount`, replacing the content-derived
boolean shape); the free-relaunch retirement loophole became the
retire-on-release decision (`MissionProgress` retires an over-age
crew/controller on release instead of returning them to `IDLE`). Both
needed a harness update: `maybeLaunchMission` now reads a configuration's
own exact counts via `pickExactlyIdleIds` rather than picking a random
1-2; the fixture's three mission configurations each declare their own
counts (`sputnik-launch-std`/`explorer-mission-std`: 0 astronauts, 1
controller; `mercury-mission-std`: 2 astronauts, 1 controller).

**Numbers, second pass (same 200-game/80-season/seed-42 run, both fixes
in place): 1,098 resigned/1,032 retired/5,333 still employed/83,951
training completions/4,537 hires/hiring bands 0/2,073/33,323/12,604
poor-average-good-excellent/598 programs opened/36,313 R&amp;D seasons;
2,138 missions launched, mission steps resolved 1,610 successful / 420
minor-glitch-solved / 475 minor glitch / 650 failure / 37 catastrophic,
74 astronauts lost in accidents, 17 of the 1,032 total retirements
specifically from the new retire-on-release check (not additional to it
- already included).** The personnel totals are close to the first
pass's (within a few percent each), consistent with the random stream
simply shifting rather than the underlying dynamics changing. **The
exact-count fix shows up directly and exactly in the accident math**:
74 lost ÷ 37 catastrophic steps = 2.0 exactly, since
`mercury-mission-std` now always flies exactly its declared 2 astronauts
and the whole-crew-loss rule removes both every time; the first pass's
57 ÷ 40 ≈ 1.4 is consistent with the old random 1-2 crew size instead.
Confirms whole-crew loss and exact headcounts are working together
correctly end to end, not just independently.

**Third pass (post-mission reliability change/mission-based skill
growth wired in, 2026-10-02) - see §2.5(g) for the full numbers,
including the step-mix shift this slice's own feedback loop produces
and the mission-triggered SET-release count**, not repeated here.

**Mission cost at launch wired in the same day (2026-10-02) - see
§2.5(h).** The three fixture configurations each got a small launch
cost; the full numbers run came back bit-for-bit identical to the third
pass above, confirming a funds deduction alone never shifts the random
draw stream - not a sign the wiring did nothing, just that this harness
(no income mechanic, modest fixture costs against 5,000 starting funds)
never meaningfully exercises `INSUFFICIENT_FUNDS`. That outcome is
exercised directly instead, by `MissionLaunchTest`'s own dedicated
cases - see §2.5(h).

**The Expert trait wired in the same day (2026-10-02) - see §2.2(c)/§2.5(i).**
Two new bot decisions, both exclusively through the validated actions
(never by setting fields directly, so the harness's own invariants
genuinely cross-check them): starting `TrainingKind.EXPERT` training
(20% chance, once idle and eligible - see `eligibleExpertAstronautSkills`/
`eligibleExpertMissionControlSkills`), and pulling anyone currently
training out early (3% chance per season, any kind, any group - the
first exercise of the new general `pullOutOfTraining` path, closing the
"no pull-out path exists yet" gap this section's own 2026-09-30 entry
flagged). Three new invariants: every currently-held Expert skill's own
value is still at or above the threshold (skills never decrease, so
this also proves the threshold held at grant time); an in-progress
`EXPERT` round's own target skill is at or above the threshold and not
already held; and every `ExpertRescueApplied` event's own `boostPercent`
matches `MissionStepResolution.expertRescueBoostPercentForUse` for that
employee's own re-derived use-count position in the same season's event
list (or that value scaled by the off-mission-controller efficiency),
with `probabilityAfter` never below `probabilityBefore` and never
reaching 100. **Numbers (same 200-game/80-season/seed-42 run): 1,092
resigned/1,013 retired/5,344 still employed/71,178 training
completions/4,515 hires/598 programs opened/37,426 R&D seasons; 2,164
missions launched, mission steps 1,958 successful/793 minor-glitch-
solved/285 minor glitch/385 failure/33 catastrophic, 66 lost in
accidents, 30 retired on mission release; 3,518 Expert-training starts
(3,108 completed), 3,663 pull-outs, 1,766 nonzero Expert rescues.**
**The step-mix shift is the rescue mechanic itself working, not noise:**
minor-glitch-solved rose 403&rarr;793, failure fell 604&rarr;385,
catastrophic fell 41&rarr;33, accidents fell 82&rarr;66, and the average
post-mission reliability delta rose 1.24&rarr;2.17 - all the direct,
expected effect of 1,766 nonzero rescues pulling outcomes toward
success. Fixed-length training completions fell 84,592&rarr;71,178,
consistent with Expert stints (which never emit `TrainingCompleted` at
all, success or fail) plus 3,663 pull-outs displacing rounds that would
otherwise have completed normally. Single seed, not checked against
run-to-run variance, same caveat as every other numbers pass in this
section.

### 3.11 Offscreen screenshot tooling for UI checking

**Decided (user, 2026-09-27): reuse Robot-Rampage's `ScreenSnapshot`
mechanism once real UI screens exist — not re-derive a new one.** The
mechanism, precisely: an `Lwjgl3ApplicationConfiguration` with
`setInitialVisible(false)` (a real GPU-backed window, just never shown on
screen — not truly headless, genuine rendering still happens), building a
screen from canned/synthetic state (no real server needed, a dead-end
connection stub that goes nowhere), rendering it into a libGDX `FrameBuffer`,
and writing the result out via `Pixmap.createFromFrameBuffer`/
`PixmapIO.writePNG`. This is how Robot-Rampage's own development checked
layouts, hard-to-reach states (damaged/locked UI, dialogs that only open
from a click), and caught real regressions (a moved/renamed sprite silently
invisible on screen, not a crash) that no unit test would catch and no
screenshot-free playtest reliably would either.

**Not built yet, correctly deferred** — `LaunchWindowGame` (`core`'s
`client` package) is currently a blank placeholder with no real screens to
photograph. Build this the same session real screens start existing,
following `ScreenSnapshot`'s exact shape (an `lwjgl3/src/test` dev tool,
never shipped) rather than reinventing the hidden-window/`FrameBuffer`
mechanism from scratch.

### 3.12 Visual design system / UI style — approved direction (user, 2026-09-28)

**Decided: a "restrained engineering blueprint" look**, arrived at through a
round of side-by-side HTML/CSS mockups (four independent directions, then
two rounds of merging/refining the winner) rather than a single guess —
worth recording the process briefly since the ruled-out directions are real
information for future UI work, not just the final answer:

- **Ruled out:** a dark green-phosphor "retro Mission Control" terminal look
  and a dark modern minimal dashboard — both read as visually striking but
  too hard on the eyes for the amount of actual reading Launch Window
  requires (personnel/mission tables, the educational "why was this hard"
  passages `design.md` §1.1 commits to). **Lesson for any future screen
  design: this project is text-heavy enough that a dark UI is a real
  readability risk, not just a style preference — default to light,
  high-contrast surfaces for anything with a paragraph of body copy on it.**
- **Kept and merged:** a bold flat-color "Cold War atomic-age poster" look
  (strong navy/red/mustard, confident color blocking) and a "technical
  document" look (paper texture, thin rules, corner register marks, quiet
  restraint) — merged because the heaviness people disliked in the poster
  direction turned out to be about **border weight**, not color: thin
  borders (1–1.5px) plus solid, saturated fills gives both the color
  confidence of the poster look and the lighter touch of the document look
  at the same time. That's the core visual rule going forward, not just a
  one-off mockup choice.
- **Final variant:** the paper/parchment page background was swapped for a
  cyanotype "engineering blueprint" blue with a two-tier drafting grid (fine
  hairlines every 20px, a bolder line every 100px) — fits Launch Window's
  own "space agency instrument panel" framing better than paper did. The
  page background and the content cards are **deliberately different
  values**: readability-critical surfaces (the mission-detail card, any
  paragraph of body text, data tables) sit on a **flat, opaque white card**,
  never directly on the textured/patterned page background — texture and
  pattern are for the page frame (header/nav/footer margins) only, kept out
  of anything meant to be read at length.

**Color — has real semantic meaning, not picked per-component:**

| Role | Color | Usage |
|---|---|---|
| Primary / neutral UI | Navy `#16305c` | Chrome: headers, primary buttons, table header rows, borders |
| Active / in-progress / caution | Gold `#e0a72e` (fill), `#8a5d12` (text-safe darker variant) | "In progress" status, the current step in a sequence, a neutral/steady state |
| Good / high / success | Green `#1f6b47` (on white), `#c4f5e0` pale-mint `success-on-page` (directly on the blue frame) | A positive rating (e.g. high morale) — **new color, not present in any of the ruled-out directions**; added deliberately once mission/personnel outcomes are going to need a real "this is good" signal distinct from "this is just active." **The on-page variant was a real fix, not a preemptive one** (2026-09-29): a footer stat's positive delta was first set in plain `#1f6b47` directly on the blue frame and measured ~1.15:1 contrast there — essentially invisible, caught by the user, not by any contrast check before shipping. Mirrors the gold/`gold-on-page` split that already existed for the identical reason; `success` just hadn't been used directly on the page background until this footer stat did. |
| Destructive / failure | Red `#c6402b` | **Reserved** for genuinely destructive actions (e.g. scrubbing a mission) or failure states — not used for "in progress," which earlier mockup rounds had wrongly conflated with danger |
| Season-commit action | Violet `#4b3a72` | **Reserved for exactly one action per screen**: advancing the game clock — added 2026-09-29, **its one use dropped 2026-09-30, see below** |
| Page frame (blueprint) | Blue `#3d6a9a` background, white grid lines at low opacity | Header/nav/footer margins only |
| Content surface | White `#ffffff` | Cards, tables, body-text blocks |
| Ink / body text | `#1c1c1a` (on white), `#eaf1fb`/`#ffffff` (on the blue frame) | — |

Each color was picked/tuned against real contrast math (WCAG relative
luminance), not by eye — e.g. the blueprint page background was deliberately
lightened from an initial too-dark value once it became clear it was
sitting at roughly the same luminance as the navy UI accent (~1.2:1 contrast
between them, i.e. barely distinguishable); the shipped value roughly
doubles that separation.

**A fifth semantic color, violet/`commit` (2026-09-29):** added specifically
for the common footer's "End Season" button — the player's way of advancing
time after finishing a season's management steps, present on every screen.
Deliberately its own hue, not a reuse of gold ("active") or navy
("primary/neutral"): this button needed to read as clearly distinct from
every other button on screen, since it's the one action that ends the
current turn rather than opening a sub-screen or committing a small local
change. Violet was picked over an orange/coral option specifically because
it sits outside the hue range gold and danger already occupy (avoiding "is
this a caution color or a danger color?" ambiguity), and it has a loose
thematic fit — dusk/day's-end — for a "close out the season" action. Pushed
into the actual Design System artifact as a `commit` color token and a
matching `Button` `commit` variant (not just hacked into one mockup), with
the explicit rule that it's reserved for this one action per screen, never
a general fourth button style. **Not yet in `Theme.java`** — same
not-yet-translated state as everything else in this section.

**Its one intended use — an "End Season" button — was itself dropped the
same way it arrived, by re-examining the design rather than by building it
first (2026-09-30):** working through the common footer's exact layout
against the Personnel Recruitment mockup (§3.13), the user realized Launch
Window has no equivalent action at all — the player never explicitly
advances the season themselves; every action is submitted individually and
takes effect at the server's next scheduled resolution (§3.6), so there is
no "I'm ready, resolve now" moment for a button to mean. BASPM has one
because it's the single-player/PBEM turn-based game actually waiting on
that click; Launch Window's async daily-batch model structurally doesn't.
**The `commit` color token and `Button` `commit` variant stay defined in
the design system** (removing them is more churn than leaving an unused
token, and a genuinely different single "big, deliberate, one-per-screen"
action could still turn up later — Tiger Team assembly was floated as one
informal candidate, not decided) **but nothing currently needs
`Theme.COMMIT`/`ButtonKind.COMMIT`** — don't add either speculatively; add
them only once a real screen actually needs this exact "one outside-the-
normal-palette action" treatment.

**Typography — a two-font functional system, wordmark deliberately
excluded from it:** **Archivo** (600/700 weight, not the single-weight
"Archivo Black") for headers, labels, and buttons; **Work Sans** for body
copy, chosen specifically because Launch Window has real paragraphs to
display (the educational asides in §1.1, mission narrative), not just UI
chrome. **Explicitly decided not to lean on the UI typeface for the
"LAUNCH WINDOW" wordmark** — a plain grotesque font was never going to give
the logotype presence, and conflating "the font used everywhere for
buttons" with "the brand mark" was the actual problem, not a font choice to
keep tuning. Precedent noted during the decision: NASA's own 1975 "worm"
logotype is a bespoke piece of lettering used only as the wordmark, while
every actual document/sign used plain Helvetica — same separation of
concerns. **Open, tracked in §4:** the actual wordmark/logo is a separate,
parallel design task, not blocking anything else in this section.

**Layout principles that generalize beyond this one mockup screen:**
- Thin borders (1–1.5px) throughout; heavy/thick borders read as dated once
  a screen has more than one bordered element on it.
- Solid color fills are reserved for elements that are communicating state
  (a status badge, a rating) or are the primary action in a group — not
  applied decoratively.
- A card/table/paragraph that will be read (as opposed to glanced at) always
  gets a flat, opaque background, regardless of what texture or pattern the
  page around it uses.

This section is the reference for any future screen mockup — a new screen
should look like it belongs next to this one without needing to re-derive
the palette or the border/fill rules from scratch.

**Implemented in code (2026-09-28):**
`core/src/main/java/de/mkoehler/launchwindow/client/ui/Theme.java` — the
libGDX translation of this section, mirroring the sibling projects'
`Theme.java` shape (StarWars/Robot-Rampage; Robot-Rampage's is the closer
template). Every color/spacing/radius/border constant above has a matching
Java constant with the same usage note; `Theme.Shapes` builds the actual
Scene2D drawables (buttons, badges, cards, nav tabs, progress dots).
Simpler than the sibling's version in one respect: this system has no
rounded corners or shadows, so the shape rendering is plain bordered
rectangles plus small anti-aliased circles, not a full signed-distance-field
rounded-box-with-shadow renderer. **(unconfirmed default)** `Theme`
declares a virtual layout resolution of 1920×1080 (matching Robot-Rampage's
own default) purely so `Theme.Fonts`/a future `FitViewport` have a
reference size — no real screen has been laid out yet to confirm or refute
it against, see §4. **Not yet done:** the actual Archivo/Work Sans TrueType
files aren't in the repo yet (`Theme.FontFile` names the expected filenames
under `assets/fonts/`); both are open-source Google Fonts, so no BASPM-
content concern, just an outstanding asset-adding task.

**Logo decided (2026-09-28): a mission-patch illustration.** After the
initial four abstract-symbol concepts (an orbit ring with a gold "window"
arc; the crosshair register-mark motif as a camera-aperture; a two-post
gate with an ascent line; and a mission-patch framing of the orbit-ring
concept), the user supplied a generated reference image of a full
illustrated NASA-style patch and asked for that spirit specifically —
superseding the four abstract options rather than picking among them.
The final mark (`assets/branding/logo.svg`): a Saturn V-style rocket
(tapered multi-stage body, its own black roll-pattern stripe — not a
generic silhouette) ascending past a simplified Earth limb into a
starfield, with a gold arc — the "window" — sweeping from lower-left,
intersecting the rocket's nose, and terminating at an accent star; a
mission-patch double-ring border (gold outer, thin cream inner); and a
horizontal ribbon with the wordmark and tagline. Reached over several
iteration rounds against specific visual feedback (arc curvature/height,
star placement, ribbon-line spacing) rather than in one pass — see the
session history if the reasoning behind a specific coordinate ever needs
re-deriving. **The mission-patch-as-goal-badge tie-in from the earlier
draft still stands**: the same circular frame is intended for in-game
goal-achievement badges (§2.6(c)), swapping the center art and closing
the gold arc solid when a goal is achieved, rather than being a
one-off piece of art with no reuse elsewhere.
**Note on this section's own iteration:** several rounds of building this
logo hit real self-inflicted bugs worth remembering for any future
SVG/vector work in this project — a circular `clip-path` silently
discards anything outside its radius (several star positions were
authored outside the visible circle and never rendered, more than once,
until each coordinate was checked by hand against the actual radius
rather than eyeballed), and a multi-control-point bezier's true peak/
curvature needs solving for (or pinning via an on-curve point), not
estimated from the control points' own coordinates.

**Iconography: full skill icon set exists (2026-09-29/2026-10-01), still
raw.** Solid-silhouette icons for every value of all three skill enums -
`SetSkill` (Rockets, Space probes, Human-rated rockets, Crewed spacecraft,
EVA suits), `AstronautSkill` (Leadership, Piloting, EVA, Science, Fitness),
`MissionControlSkill` (Propulsion, Trajectory/GNC, Spacecraft systems,
Crew systems & payloads, Mission operations) - the user generated original
bitmap art with an AI image tool and had it contour-traced to SVG, at
`assets-raw/svg/icon_*.svg`. The `SetSkill` batch (five icons) came first
(2026-09-29); the astronaut/mission-control batch (nine more bitmaps) came
2026-10-01, completing the set - **`AstronautSkill.EVA` and
`SetSkill.EVA_SUITS` share one file, `icon_eva_suits.svg`**, per the
user's own instruction, rather than each getting its own traced copy.
First used as the sort-by-skill buttons on a Personnel Recruitment screen
mockup (a Claude Artifact, not yet a repo screen); confirmed a second,
independent use the same day the first batch was made, when the user
pointed out BASPM's own training/skill-selection dialog uses the same
category set as its icon set — real evidence each skill enum is the right
single join point for this artwork wherever a skill needs a glyph
(recruitment, training assignment, R&D assignment screens all plausibly
reuse it), not a one-screen throwaway. **Still open:** these are raw
traced files, not yet promoted into `assets/` or wired into `Theme`'s icon
pipeline (`Theme` has no icon concept yet at all — see its own "no icon
system yet" status) — that promotion, plus deciding a consistent on-disk/
atlas format, is a real task for whenever the first screen using them gets
actually built, not done implicitly by having them exist in
`assets-raw/`. **Provenance, since this exact mistake happened once
already this session:** these are traced from the user's own new bitmap
art, not from BASPM — the five icon SVGs that *were* traced from BASPM's
SET Centre UI got caught and removed the same day (see `CLAUDE.md`'s
source-material note for the full story); don't conflate the two when
this section is read back later.

### 3.13 Common screen chrome & the main-menu hub (2026-09-30, groundwork ahead of M5)

**Not implemented yet — M5 scope (§5). Recorded now because the user
prepared the actual art/mockup this session, not because the client work
has started.** Two artifacts inform this section, both private Claude
Artifacts kept off GitHub rather than pasted into this repo (same posture
as the earlier UI Asset Sheet entry above) — ask the user for the links if
a future session needs them and doesn't have them in context:
- A **Personnel Recruitment screen mockup**, built with the launchwindow
  design system installed as its Design System artifact (confirms
  `Theme.java`'s constants are pixel/value-identical to the published
  `tokens.json` — cross-checked directly this session, no drift found).
  Its recruitment-specific content is secondary; what matters here is the
  header/footer chrome, present and marked "identical on every screen."
- The existing **UI Asset Sheet** (§3.12's iconography entry) — re-checked
  this session for completeness, no new icons beyond what §3.12 already
  documents.

**The main menu is BASPM's "Space Complex Screen" (`WM` prefix,
`baspm.md`'s screen table), not a new invention.** BASPM's own version is
already documented there as "the main hub with the 7 building icons, day/
night cycle." The art the user prepared this session
(`assets-raw/main_menu/`) independently lands on exactly that shape:

- **Two complete asset sets, one per `Archetype`** (`LW_main_menu_NASA_*`,
  `LW_main_menu_SOVIET_*`): 4 seasons &times; 4 times of day = 16 background
  images per faction, 32 total, plus one `<faction>.hotspots.json` sidecar
  per faction (authored with the `HotspotEditor` dev tool, see its own
  session history) — **a single polygon set per faction turned out to
  cover every season/time-of-day variant**, confirmed by the user after
  trying it, since the buildings themselves don't move between variants,
  only the lighting/season dressing around them. Both sidecars' polygon
  names match exactly.
- **Each hotspot set has exactly 7 named polygons** — `VAB`,
  `MISSION_CONTROL`, `ASTRONAUT_COMPLEX`, `SET_CENTRE`,
  `PUBLIC_AFFAIRS_OFFICE`, `HEADQUARTERS`, `MUSEUM` — the same count as
  BASPM's own 7-building hub, and every name maps onto one of BASPM's own
  screens (`baspm.md`'s table): Mission Control, Astronaut Complex, SET
  Centre, Public Affairs Office and Headquarters all already have a
  `baspm.md` entry; `VAB` and `MUSEUM` are the two that don't yet appear
  anywhere in this file's own roadmap (§5) — see the note below.
- **Raw images are 1920&times;1088, 8 px taller than the 1920&times;1080
  target** — confirmed by direct inspection this session. **When these
  assets get promoted from `assets-raw/` into `assets/`, crop exactly the
  bottom 8 rows off every image**, nothing else. The user deliberately
  placed every hotspot polygon clear of that bottom strip before authoring
  them, and hotspot coordinates are image-pixel space with a top-left
  origin (the `HotspotEditor` tool's own documented convention) — a
  bottom-only crop never shifts any polygon's coordinates, so the existing
  `.hotspots.json` sidecars need no rework once this crop happens.
- **Season/time-of-day art selection (decided, 2026-09-30):** which of the
  4 season images shows is driven by the real in-game current season
  (server state, §3.6/§3.7) — not a real-world calendar date. Which of the
  4 time-of-day images shows is purely a **client-side, wall-clock-driven
  ambience effect with no server involvement at all**: it advances one
  step every 4 real-world minutes the player has spent in the game
  session, checked/applied only when the player *re-enters* the main menu
  screen (never while they're actually sitting on it — no background timer
  ticking the art out from under them mid-view). Concretely: on entering
  the main menu, compare the wall-clock time since the *last* time it was
  entered; if &ge; 4 minutes have passed, advance to the next time-of-day
  image (dawn &rarr; noon &rarr; dusk &rarr; night &rarr; dawn), otherwise
  leave it as is. Purely cosmetic, gives "time is passing" ambience while
  flipping between screens, matching BASPM's own "day/night cycle" note on
  this same hub screen but adapted to Launch Window's async/no-live-session
  model (BASPM's cycle is presumably tied to actual turn/session time; this
  one is wall-clock since nothing else in Launch Window ties the client to
  a shared session clock).
- **`VAB` and `MUSEUM` aren't arbitrary extra buildings — both are
  BASPM-confirmed distinct screens not yet reflected in §5's roadmap:**
  - **`VAB`** (manual p.35-36): scheduled/in-progress missions,
    rocket/payload *assembly* — distinct from `HEADQUARTERS` (manual
    p.39), which is open/manage *programs*, R&D, goals. BASPM keeps these
    as two separate screens (open a program at Headquarters; assemble/
    schedule/launch a specific mission at the VAB). **M5's current slice
    list (§5) bundles "program/mission selection" into one screen** —
    worth revisiting whether that should actually split into two, matching
    both BASPM's own division and the main-menu art's own two separate
    hotspots, when M5 is actually sized. Not decided either way yet.
  - **`MUSEUM`** (manual p.40): historical stats/achieved-goals/memorial
    browser — this is the same screen §1.1 already flagged as "a plausible
    home for a deeper encyclopedia version" of the educational-layer
    content; see that section's own updated note. Also the natural home
    for whatever the win-condition tie-break's goal-comparison screen
    (§2.6(b), M4/M5) ends up looking like, though that's a guess, not a
    decision — the two aren't necessarily the same screen.

**Common header/footer chrome, transcribed from the mockup (identical on
every screen, per the mockup's own comments):**

- **Fixed virtual canvas, 1920&times;1080** (`Theme.VIEW_WIDTH`/
  `VIEW_HEIGHT` already match — real confirmation, not a new decision;
  see this narrowing §4's still-open "screen layout resolution" question),
  30px outer page padding (`Theme.SPACE_6`), 20px gap between the header/
  content/footer major sections (`Theme.SPACE_5`) — both spacing tokens
  already existed in `Theme.java` and match exactly.
- **Header, above a horizontal separator rule — shared chrome:**
  back / home (main menu) / settings icon buttons, a divider, then a
  breadcrumb trail (each prior crumb clickable, navigating back to that
  screen; the current screen's crumb non-clickable, rendered upper-case).
  Right-aligned: a prestige chip (white card, navy icon/text) and a
  game-time chip (solid navy fill, on-fill text, "`1955 &middot; S1`"-style
  year/season format).
- **Screen-specific title block sits directly *below* that separator** —
  an eyebrow context label (e.g. "SET Centre") plus a large heading (e.g.
  "Personnel Recruitment"). **This is part of each screen's own content
  area, not shared chrome**, despite visually reading like part of the
  header — the separator line is the actual boundary the user described
  ("everything above that line is common header area, anything below is
  specific").
- **Footer, below its own horizontal separator rule — shared chrome:**
  five evenly-spaced clickable stat chips, each with a small trailing
  chevron hinting it navigates: **Funds/Budget** (&rarr; Public Affairs
  Office; shows current balance plus a colored delta, e.g.
  "`$ 25,000.0` `+2,420.0`"), **SET employees** (&rarr; SET Centre; "hired /
  busy / capacity", e.g. "`5 / 5 / 7`"), **Astronauts** (&rarr; Astronaut
  Complex; same three-number format), **Flight controllers** (&rarr;
  Mission Control; same format), **Open programs** (&rarr; Headquarters;
  "open / total", e.g. "`0 / 3`"). **No sixth "End Season" action** — see
  §3.12's `commit`-color entry for why that button was dropped the same
  session it was being laid out.
- **Background:** every non-main-menu screen reuses the tiled blueprint-
  grid page texture (§3.12's iconography entry / the UI Asset Sheet) as
  its frame. **The main menu is the one exception**, filling the entire
  screen with the season/time-of-day art above instead of the blueprint
  texture — already noted as the plan in §3.12's own background-texture
  history, now confirmed by the actual prepared art.
- **Window resize must preserve a 16:9 content area** (user requirement,
  2026-09-30): whatever the real window size, the rendered game area keeps
  a 16:9 aspect ratio (letterboxed/pillarboxed as needed), consistent with
  a libGDX `FitViewport` of `Theme.VIEW_WIDTH`&times;`VIEW_HEIGHT` — the
  approach `Theme.java`'s own doc comment already assumes (`Fonts.load`'s
  `scale` parameter), not a new technical decision, just an explicit
  requirement now on record.

**A real gap found by cross-checking the mockup against `Theme.java`
directly this session:** the published `tokens.json` (§3.12) defines
`success-on-page` (`#c4f5e0`, used by the footer's funds-delta text) and
`commit` (`#4b3a72`, now unused per the note above) — `Theme.java` has
neither as a constant yet, unlike every other token in that file, which
all have exact matches. `success-on-page` is a real, currently-missing
gap (the footer's positive funds delta needs it and nothing else in
`Theme.java` covers that role); `commit` is optional per the note above.
Add `Theme.SUCCESS_ON_PAGE` when the footer/header chrome actually gets
built, not speculatively now.

## 4. Open questions (tracked here until resolved)

- **Mid-turn/mid-mission player decisions — narrowed by the §2.5 mission-sim
  trace (2026-09-27).** BASPM stops and waits for a player click at several
  points *within* a turn, not just between turns: scrubbing a scheduled/
  in-progress mission, pulling an employee out of training early, and
  (originally suspected, per naming alone) some `REB*`/decision-event
  choices (`baspm.md` §7.2). Originally worried this meant a daily-batch
  resolver where nobody's expected online has no obvious equivalent — **the
  mission-simulation trace resolves the "mid-*resolution*" half of that
  worry: a season's step-by-step roll sequence is fully automatic once
  triggered, needing no player input while it runs** (`baspm.md` §8.1/§8.2)
  — every one of BASPM's own stop-and-click points (scrub/launch/assign,
  training pull-out) happens *before* that sequence starts, never
  interleaved with it. **The `REB*` suspicion itself resolved to zero real
  cases (`baspm.md` §12.2, 2026-09-27): no BASPM random event ever presents
  a real choice** — the one class that could (`DEAvoidPrestigeDropAtACost`)
  is a permanently-disabled, entirely-unimplemented stub. §2.7 needed no
  policy decision at all as a result. **What's still genuinely open is only
  the *pre*-resolution question for the two remaining real cases (scrub,
  training pull-out):** what does a player who doesn't log in before a
  turn's server tick implicitly decide for each of those points? Candidates
  unchanged: submit conditional policies alongside the rest of the turn
  ("scrub this mission if X happens"), defer the decision to the next day's
  turn at some in-fiction cost, or auto-resolve with a player-overridable
  default up until that day's server tick. **Decision:** — The player is already explicitly asked if he wants to launch a mission when he assembles it. in the original game he is asked a second time right before the turn evaluation starts, but this is just a nice to have thing, not a necessity. So we can simply skip this second confirmation in Launch Window. If the player intends to scrub the mission he will be able to do so through normal game play dialogs up until the current days turn gets evaluated. The same is true if he wants to pull an employee out of training. The player will be able to do so through normal game play dialogs up until the current days turn gets evaluated. If he does not do so, the default is that the mission will launch and the employee will stay in training. This is a reasonable default and it is consistent with the original game.
- **One sub-case already resolved favorably by real savegame
  evidence (`baspm.md` §2.1, 2026-09-27 — this citation previously pointed
  at a §8.1 that didn't yet exist; backfilled properly this session):
  ongoing R&D assignment is NOT one of the awkward mid-turn cases** — despite
  its state name (`ASSIGNED_TO_RD_ONE_TURN`), an assignment persists
  automatically across seasons with no re-declaration needed, confirmed by
  tracking one employee staying assigned to the same component across four
  consecutive seasons untouched. So a Launch Window turn's "who's on R&D" input
  can be a standing order the player sets and leaves alone, not something
  that has to be resubmitted every single day. 
- a save showed a mission
  scrubbed *before* it ever started executing costing nothing (`MISSION_OUTCOME_CANCELLED`,
  0 prestige) — worth testing separately whether a mid-flight abort is
  actually penalized (`MsnSim*.xml`'s `OnCancellation` fields suggest it
  should be) before assuming "scrub" is uniformly free or uniformly costly.
  **Tiger Teams resolved differently (2026-09-27, `baspm.md` §9.5, §2.2(c)):**
  BASPM's version is a genuine mid-mission-*step* pause (pay to boost a
  step's saving-throw odds), not just a between-turns one — but rather than
  give it a live-decision-with-auto-default treatment like the remaining
  open cases below, Launch Window promotes it into a standing personnel
  capability instead (an "Expert"-trait employee pool that auto-triggers
  during resolution, §2.2(c)), sidestepping the need for any per-turn
  decision at all. **No cash-budget fallback** for when no Expert personnel
  are available (§2.2(c)'s decision, tightened after this bullet was first
  written) — players need to cultivate their own Experts to have a safety
  net, not buy one on demand.
- Turn cadence resolved (§1/§3.6): one turn = one BASPM "season", schedule
  (which real days produce a turn) is per-game-configurable. **Blackout-list
  editing decided for v1 (§3.6, 2026-09-28): the host edits the server's own
  JSON config file directly** — no in-game permissions/role system needed
  yet. A real "game host/admin" role concept (needed eventually for
  announcing games, and anything beyond direct file access, §3.8) is still
  open, just no longer blocking v1.
- **Resolved (§3.9, 2026-09-27):** a friends-group game and a colleagues-
  group game are two separate server instances (e.g. two Docker containers),
  not one server hosting both — no matchmaking/game-selection layer needed.
  **Resolved (§3.8, 2026-09-27):** player-slot count within a game is dynamic
  and never closes — joining stays open for the game's entire life, not just
  before it starts.
- Multiplayer interaction model: how much of §2.9's brainstorm ships in v1?
- **Screen layout resolution — narrowed, not yet fully resolved (§3.12,
  2026-09-28; reinforced §3.13, 2026-09-30).** `Theme.VIEW_WIDTH`/
  `VIEW_HEIGHT` (1920×1080) was an unconfirmed default carried over from
  Robot-Rampage's own `Theme.java`. Real evidence now points the same way,
  not just the carried-over default: the Personnel Recruitment mockup's
  own root element is a fixed 1920×1080 canvas, and the main-menu art
  (§3.13) targets 1920×1080 after its 8px crop — but this is still mockup/
  art evidence, not a real libGDX screen laid out and checked against it.
  Revisit once the first real screen (the Space Complex hub, §3.13, is the
  obvious first candidate) is actually being built in `lwjgl3`.
- **Resolved (§3.12, 2026-09-28): the wordmark/logo.** A mission-patch
  illustration — see §3.12's own entry for the full description and
  `assets/branding/logo.svg` for the file. Two follow-ups remain, tracked
  there rather than here since they're production tasks, not open design
  questions: the wordmark text isn't outlined yet (needs the real font
  files first), and no separate small-size icon-only crop exists yet.
- **Resolved (§5, 2026-09-28): how much of §2.9's multiplayer-interaction
  brainstorm ships in v1?** Passive competition only (already decided,
  §2.9) plus the win-condition/tie-break check, both scheduled into M2/M4.
  Espionage/sabotage/interference is explicitly M7+. Still open: whether
  §2.9's "email" means player-to-player mail or turn-notification emails —
  M1's decision gate.
- **Resolved (§3.3, 2026-09-27):** native client, libGDX, matching the
  sibling projects — the web-client alternative once considered here was
  dropped.
- **Resolved (§3.1, 2026-09-27):** yes, a persistent server process — just a
  lightly-loaded one (short-lived client connections plus one scheduled
  turn-resolution job), not a continuously-simulating one like either
  sibling project's.
- **Resolved, as far as archetype count/scope goes (§2.1(a1)/(b1), 2026-09-27):
  how much of BASPM's historical Cold War framing (real programs, real
  agencies) do we keep vs. reskin?** Two non-exclusive archetypes, NASA-style
  and Soviet-style, real content-gating carried over between them, economics
  identical across both (matching what BASPM's own shipped game already
  does). **No GSA-equivalent third faction** — a universal-access archetype
  turned out to be real strict superiority in a competitive setting (user
  pushback, 2026-09-27), not something a compensating bonus could fix, and
  BASPM's own actual competitive mode (`MultiplayerColdWar`) already excludes
  GSA the exact same way. What's still open is the *content* layer this
  scope decision sits under — real program/mission-config names and history
  vs. an original reskin, still governed by `design.md` §1.2's no-copying
  rule either way.
- **Resolved (user, 2026-10-01), refining the above once `baspm.md`
  §13.4a actually measured BASPM's own NASA/Soviet prestige balance: don't
  try to fully close the gap for v1, but stay close to BASPM's own
  compensation ratios when Launch Window's own mission-prestige formula
  gets designed (a later M2/M3 slice — no mission-simulation/prestige
  resolver exists yet, see §5).** `baspm.md` §13.4a found BASPM gives
  Soviet programs a real but partial per-program compensation (worth
  ~4% more prestige on average, ~11% more cost-efficient) that narrows but
  doesn't close NASA's ~34% total prestige-throughput edge from having 9
  more programs. **User's reasoning: not confident this is actually a
  problem worth engineering around** — Soviet's own total available
  prestige (46,050 in BASPM's numbers) is almost certainly far more than
  enough to reach a normal endgame before any prestige cap/ceiling could
  matter, so a real gap in *how much* prestige is technically available
  doesn't necessarily translate into a real gap in *whether either
  archetype can win* - treat as a balance question to revisit with actual
  playtest/harness data (§3.10) once Launch Window has a real
  mission-simulation resolver, not a v1 blocker to pre-solve from table
  numbers alone. In the meantime: **re-derive Launch Window's own
  mission-configuration prestige formula close to BASPM's own shape** —
  its `SecondTime`/`LaterTime` repeat-mission decay (50%/30% of
  `FirstTime`, essentially universal across 186/189 of BASPM's own
  mission configurations, re-confirmed in-game via screenshot) is a
  reasonable curve to keep (not verbatim BASPM numbers, per §1.2, but the
  same *shape* - a fixed, uniform decay ratio applied to every mission
  rather than per-mission-tuned), and whatever per-program NASA/Soviet
  reward-scaling Launch Window ends up with should land in the same
  "partial, not full" compensation range BASPM itself used, rather than
  either leaving the gap fully open or over-correcting to full parity.

## 5. Implementation roadmap: milestones & slices

**Draft (user + assistant, 2026-09-28), format borrowed directly from
Robot-Rampage's own `design.md` "Components/TODOs" section** (fetched from
its public GitHub repo, `mariokoehler/Robot-Rampage`, since that project
isn't cloned on this laptop at all — an unrelated absence from the laptop's
missing `E:` drive, `CLAUDE.md`'s caveat about that is specifically about
BASPM's own extracted files; StarWars' own equivalent section wasn't
checked). Robot-Rampage's
own list is headed "Proposed implementation order" and accumulated **done**
markers as milestones actually finished — this list is written the same
way, prospectively, so treat granularity/order as **(unconfirmed)** until a
milestone is actually underway. Each milestone: what it's for, what's in/out
of scope, its exit criterion, and (where relevant) which still-open
`design.md` item has to be decided before that milestone's affected slice
can start (a **decision gate**). `baspm.md`, already in the repo, is where
every milestone's BASPM-shape references point — but **check per slice, not
as a blanket assumption**: grep `baspm.md` for the section a slice's
formulas/content need before starting it, and if the fact isn't there (only
on `E:`), stop and ask, per `CLAUDE.md`'s `COMPUTERNAME` rule, rather than
guessing. The sibling projects' own template code (StarWars'
`AccountStore`/`PasswordHasher`, Robot-Rampage's `MessageRegistry`/
`Playouts`/`ScreenSnapshot`) isn't on this laptop either, but both repos are
public on GitHub and fetchable read-only via `gh api repos/mariokoehler/
<repo>/contents/<path>` the same way this roadmap section was drafted — so
M1 is buildable on the laptop despite neither sibling repo being cloned
here.

**Three shape-defining choices made before drafting this list (user,
2026-09-28):**
- **Walking skeleton before real game systems**, not Robot-Rampage's own
  layered order (rules engine → persistence → network → client). An early
  milestone proves login → a queued action → the scheduler resolves it →
  the player sees the result, end to end, before any real game system
  exists — de-risking the riskiest integration (the scheduled turn job,
  KryoNet connect/send/disconnect, and action-journal replay at login,
  §3.7) early rather than at the end.
- **Deployment (jpackage client zip, Docker server image, §3.9) is
  deferred, matching Robot-Rampage's own M7 placement** — not required for
  the MVP. The first real playtest happens in dev-mode (the commands in
  `CLAUDE.md` "Build system"), validating the core loop is worth packaging
  before investing in packaging it.
- **The "Expert" personnel trait (§2.2(c)'s Tiger Teams replacement)
  originally shipped after the base mission-simulation formula, not with
  it** — mission simulation's first version was the base involvement-
  weighted roll plus a flight-controller-skill-scaled saving throw only,
  with no automatic rescue beyond that, deferring the then-still-
  undecided exhaustion/falloff curve until there was a playable loop to
  tune it against. **Superseded (2026-10-02): once the base formula
  existed and `baspm.md` §9.5 fully traced Tiger Teams, the user made
  every remaining design decision the same day, so the Expert trait's
  mission-side rescue mechanic moved up into this same mission-
  simulation work instead of waiting for a second wave** — see §2.2(c)/
  §2.5(i) for the implemented shape.

- **M0 — Project skeleton. Done (2026-09-27).** Maven multi-module
  scaffold, `NetworkConstants`, `LaunchWindowGame` placeholder — see
  `CLAUDE.md` "Build system"/"Current status".
- **M1 — Walking skeleton: one trivial action, end to end — done
  (2026-09-29).** No real game systems yet. **Decision gate resolved (user,
  2026-09-28):** §2.9's "email"
  means in-game player-to-player mail, not turn-notification email — no
  email-address field needed on §3.8's account schema (login, password
  hash/salt, display name stays as-is). In-game mail itself is still M7+
  scope (§2.9's own sequencing), not part of this milestone.
  - **Slice: snapshot + journal + atomic writes + login-time replay — done
    (2026-09-28, hardened 2026-09-29).** `de.mkoehler.launchwindow.persistence`:
    `PlayerSnapshotStore` (one immutable file per resolved turn, per the
    concrete layout above), `PlayerActionJournal` (sequence-numbered
    action files, the sequence durably persisted and never reused/reset —
    see §3.7's invariant note), `SnapshotReplay`, `AtomicFiles` (the
    shared temp-file-plus-retrying-atomic-replace helper every flat-file
    store in this project now uses, including §3.8's `AccountStore`
    below), and `PlayerStateService` tying the rest into the login flow
    and an idempotent, crash-safe `resolveTurn(gameTurn)` (a cutoff saved
    alongside each turn's snapshot lets a crashed-and-retried resolution
    finish archiving exactly the actions it actually applied, never a
    later one submitted during the crash window). `SetFundsAction` is the
    placeholder action exercising all of this end to end. No network yet —
    proven by direct unit/integration tests against the file system, not
    yet over real sockets.
  - **Slice: `AccountStore` — done (2026-09-29).** Adapted from StarWars'
    own source (`gh api repos/mariokoehler/Star-Wars/contents/server/src/
    main/java/de/mkoehler/starwars/server/accounts/AccountStore.java` —
    the repo is `Star-Wars`, hyphenated, not `StarWars`) rather than
    reimplemented from prose. `de.mkoehler.launchwindow.server.accounts`
    gets `PlayerAccount` (including the server-assigned `id`, §3.8's
    login → player-data-directory decision), `PasswordHasher`,
    `AuthResult`, and `AccountStore` — see §3.8 for the full account
    schema/validation rules and what was dropped from StarWars' version.
    Its own file write is now atomic via `AtomicFiles` (§3.7's slice),
    which StarWars' version doesn't have.
  - **Slice: minimal KryoNet protocol — done (2026-09-29).**
    `de.mkoehler.launchwindow.net`: `MessageRegistry` (Robot-Rampage's
    pattern, fetched via `gh api` rather than reimplemented from prose) plus
    `net.messages`. **Credentials travel once per connection, in the
    handshake (see the dispatcher slice below), not on
    `SubmitActionRequest`/`FetchStateRequest`** — those two carry no fields
    beyond the action/nothing at all. **A real Kryo compatibility issue:**
    `EnumMap` has no no-arg constructor, so Kryo's default reflective
    instantiation can't create one on read; fixed by switching the
    personnel skill maps from `EnumMap` to `LinkedHashMap` (§3.4) rather
    than fighting Kryo's per-key-type `EnumMapSerializer`, which doesn't
    generalize across our three different skill enums anyway.
  - **Slice: request dispatcher + KryoNet server/client wiring — done
    (2026-09-29).** This is the piece that actually receives a message on
    the server, authenticates it, and calls `AccountStore`/
    `PlayerStateService` — without it, M1's exit criterion (submit an
    action, read it back over real sockets) has no server-side home.
    - **`HandshakeRequest(clientVersion, login, password, displayName)` /
      `HandshakeResponse(accepted, message, serverVersion)`** (adapted from
      Robot-Rampage's own pattern) do double duty: the version check *and*
      the login, in one step, before anything else on that connection is
      honored. `AccountStore.login` needs a `displayName` to auto-create an
      account, so credentials and the display name travel here, once per
      connection, not on every subsequent request. This is
      connection-scoped, not a cross-connection session the way
      Robot-Rampage's own reconnect tokens are — §3.3's "no held session"
      still holds, since nothing survives past that one short connection.
      **Registered first in `MessageRegistry`, and never moved:** the
      version check exists specifically to catch two builds whose
      registries differ, so the two handshake classes must keep the same
      ids regardless of what else in the registry has changed - this
      protects against registration-order drift in every *other* class,
      not against a future change to the handshake classes' own fields,
      which is why their own field layout must never change at all (adding
      a field included - Kryo's record serializer reads the receiver's own
      field count, so a mismatched pair could not decode an extra one
      either). **Accepted risk, not yet mitigated:** the password travels
      once per connection, in this handshake, in plain text over KryoNet's
      TCP channel, with no TLS layer. §3.8's threat model ("a hobby
      dedicated server, not a target worth pulling in bcrypt/Argon2 for")
      already covers password *storage*; this extends the same acceptance
      to *transport*, for the same reasons (invite-only, non-real-time,
      small-group, self-hosted-among-friends).
    - **`RequestDispatcher`** (`server` module, plain class, no KryoNet/
      libGDX dependency, so it's testable without sockets): its single
      entry point, `handle(connectionId, message)`, dispatches by message
      type and reports whether the connection should be closed after the
      response is sent (a rejected handshake) - `GameServer` and the
      real-socket test both call this one method, so the routing logic
      exists in exactly one place. For a `HandshakeRequest`: checks the
      client version first — an incompatible client never reaches
      `AccountStore` at all — then `AccountStore.login`, then bootstraps a
      placeholder turn-0 snapshot for a brand-new account (agency name =
      display name, archetype `NASA`, replaced by the real slot-claim flow
      in M4), then tracks `connectionId → accountId` for the rest of that
      connection's life so `SubmitActionRequest`/`FetchStateRequest` need
      no credentials of their own.
    - **`NetworkServer`** (`core`, adapted from Robot-Rampage's own,
      poll-based) closes a rejected connection only on the *next* call to
      `poll()`, not immediately after sending the rejection - an immediate
      close can drop a write still in flight, which would mean the client
      never actually receives the rejection reason. **`NetworkClient`**
      (`core`) is a deliberately different, simpler design from
      Robot-Rampage's own poll-based one — a blocking
      `sendAndReceive(request, responseType)`, since this project's usage
      really is "connect, one request, one reply, disconnect" with no
      concurrent in-flight requests to correlate; it clears any stale
      queued message before each send and treats a disconnect as an
      immediate failure rather than waiting out the full response timeout.
      `GameServer` wires `NetworkServer` + `RequestDispatcher` +
      `AccountStore` together. `ServerLauncher` takes the data root as an
      optional second CLI argument (unconfirmed default: a plain relative
      `data/` directory).
    - **Threading constraint the scheduler slice below upholds:**
      `RequestDispatcher` and everything it calls (including
      `PlayerActionJournal.append`, whose `synchronized` is only per
      *instance*, and its read-increment-write sequence counter, which
      isn't safe across two separately-constructed instances touching the
      same player) must only ever run on the single thread that drains
      `NetworkServer.poll()` - `GameServer`'s own render loop. The
      scheduler must never append to, or otherwise touch, a player's
      journal concurrently with that thread. `TurnScheduler.checkAndResolveIfDue()`
      runs as a check inside `GameServer.render()` itself, on that same
      thread, right after the `NetworkServer.poll()` call - this removes
      the concurrency question entirely rather than needing a separate
      synchronization mechanism.
    - **jgitver finding, verified directly** (the plugin's own bytecode for
      what Maven properties it exposes; the plausible candidate expressions
      tested via `mvn help:evaluate`; two local-only tags, never pushed, on
      the same history to observe the actual computed version at different
      points): with this repo's current bare `jgitver-maven-plugin`
      registration (no `useCommitDistance`/`useGitCommitId`, no tags),
      `AppVersion.getVersion()` returns the constant `0.0.0-SNAPSHOT` for
      every commit. **A first tag does not fix this on its own** - it only
      bumps the version once, at the tag; every commit between that tag and
      the next one still shares one identical version string, so the
      handshake's version check only distinguishes builds across a tag
      boundary, not commit-to-commit in general. `jgitver`'s own
      `useGitCommitId`/`gitCommitIdLength` configuration options would
      embed a git commit SHA fragment directly into the computed version
      itself, which would actually solve this — not enabled yet, a
      decision for the user.
  - **Slice: scheduler + server config file — done.** The scheduled
    turn-resolution job honors a real schedule profile + blackout list
    (§3.6) rather than running on every tick. Game creation itself is also
    a v1 file edit, not a client screen, consistent with the blackout-list
    stopgap (§3.6/§3.8/§4) — so this slice is also where that file's shape
    is defined, under the `server.schedule` package (`server` module,
    plain classes, no KryoNet/libGDX dependency):
    - **`game-meta.json`** (`GameMeta`, loaded/created by `GameMetaStore`),
      sitting alongside `accounts.json` and `players/` under the data
      root: a `schedule` (`ScheduleProfile` — the active weekdays as a
      `Set<DayOfWeek>`, a resolution time of day, and a timezone; `daily()`
      and `weekdays()` factory presets) and a `blackouts` list
      (`Blackout` — an inclusive `[start, end]` date range). A server's
      announced-start-date gap reuses this same blackout mechanism rather
      than a dedicated field. `TurnScheduler` calls `GameMetaStore` to
      re-read this file on every check rather than caching it, so a
      blackout a host adds mid-day, before the resolution time is reached,
      still suppresses that day. `GameMeta`/`ScheduleProfile`
      validate on load (non-empty active days, no null fields, no
      inverted blackout range) and fail loudly rather than silently
      accepting a malformed hand edit. If the file does not exist yet,
      `GameMetaStore` creates a default DAILY profile — **decided (user,
      2026-09-29): its timezone is hardcoded to `Europe/Berlin`**, since the
      user is expected to remain the only host for the foreseeable future,
      not derived from the server process's own clock. Logged loudly to
      stderr on creation, meant to be hand-edited afterward rather than used
      as-is by a host running elsewhere.
    - **`scheduler-state.json`** (`SchedulerState`, via
      `SchedulerStateStore`, atomic writes like every other flat-file
      store here): the scheduler's own progress, never host-edited — the
      next `gameTurn` number to use, and the last calendar date (in the
      schedule's timezone) a turn was resolved for. On this data root's
      very first check ever, that date bootstraps to *yesterday* if today's
      resolution time hasn't passed yet (today itself stays eligible for a
      resolution later that same day), or to *today* if it already has (a
      late-in-the-day first boot never resolves immediately) - staying
      unset would resolve turn 1 immediately on any first boot past the
      resolution time.
    - **`TurnScheduler.checkAndResolveIfDue()`** ties the two together:
      resolves today's turn, for every player under `players/` that
      already has a saved snapshot, exactly once — a no-op if
      today was already resolved, isn't an active weekday, falls in a
      blackout, or the resolution time hasn't passed yet. Upholds the
      persistence slice's contract of calling
      `PlayerStateService.resolveTurn(gameTurn)` for consecutive `gameTurn`
      values, never skipping one, by only advancing its own state after
      *every* player resolves without error: if any one player throws, the
      exception propagates, the scheduler state is left unadvanced, and
      the next check retries the same game turn for every player — already-
      resolved players are unaffected, since `resolveTurn` is itself
      idempotent. **Accepted limit, not yet mitigated (user, 2026-09-29):** a
      player whose resolution keeps failing therefore blocks every other
      player's turn from ever advancing past it, for as long as it keeps
      failing — accepted, since this needs looking at and fixing directly if
      it ever actually happens rather than designed around in advance.
    - **Decided (missed-day catch-up, see §3.6):** if the server is down
      across more than one scheduled resolution day, only the single next
      day is resolved on the next check — the missed days in between are
      skipped, not caught up.
  - **Slice: CLI/console dev client — done.** `dev-tools`'s
    `de.mkoehler.launchwindow.devtools.DevClient`: no libGDX UI, a plain
    console REPL over `core`'s `NetworkClient` - prompts for login/
    password/display name, sends the handshake, then accepts `state`
    (`FetchStateRequest`, prints a one-line summary), `funds <amount>`
    (`SubmitActionRequest` with `SetFundsAction`), `help`, and `quit`/
    `exit`. `main()` itself (the interactive console/socket loop) is
    exercised manually against a running server, matching
    `ServerLauncher`'s own untested bootstrap entry point in this
    codebase; its two pure helpers (`parseFundsAmount`, `formatSnapshot`)
    have unit tests.
  - **Exit criterion — verified (2026-09-29), real sockets, real scheduled
    job:** a player logs in (auto-creating an account) via `DevClient`,
    submits a `funds` action, disconnects; the real `GameServer` process,
    running standalone with `game-meta.json`'s resolution time set a couple
    of minutes out, resolves the turn on its own via
    `TurnScheduler.checkAndResolveIfDue()` inside `render()` -
    `scheduler-state.json` shows `nextGameTurn` advancing from 1 to 2 and
    `lastResolvedDate` set to that day, the player's `snapshots/1.json`
    holds `funds: 5000`, its `journal/` directory is empty, and
    `journal-archive/1/` holds the one archived action file - not just a
    replay of a still-pending action. A second, separate `DevClient`
    process then logs back in and `state` shows the submitted funds value
    with the season counter advanced from 1 to 2. **M1 is done.**
- **M2 — Rules engine, headless.** The turn-resolution logic for each
  system traced in §2, built and unit-tested independent of M1's
  network/persistence pipe (§3.10's "headless rules first" principle),
  against an in-memory/test-fixture content set (real content is M3).
  **Domain-model groundwork already done ahead of this milestone
  (2026-09-28, before this roadmap existed):** `PlayerSnapshot`,
  `Archetype`, `Personnel`/`Astronaut`/`SetSpecialist`/`MissionControlOfficer`
  and their skill enums (`de.mkoehler.launchwindow.model`) — state shapes
  only, no resolution logic yet.
  - Slice: personnel lifecycle (morale/age/resignation, R&D/training
    standing orders, skill growth) — §2.2. **Age/morale/retirement/
    resignation sub-slice done (2026-09-30)** — see §2.2(c)'s own entry for
    the concrete numbers and `de.mkoehler.launchwindow.rules.PersonnelLifecycle`.
    **Skill-growth formulas done (2026-09-30, `SkillGrowth`) and skill
    maps switched to `Double`** — see §2.2(c). **Fixed-length and
    `PERMANENT` training done (2026-09-30, `TrainingProgress`)** — see
    §2.2(c)'s own entry for the new fields and the composition-order
    contract with `PersonnelLifecycle`. **Hiring pool done (2026-09-30,
    `HiringPool`)** — see §3.4/§2.2(c) for the candidate-pool fields,
    quality bands, roster capacities, and the three user decisions
    (auto-`BASIC`-training on hire, capacity now decided, hiring free for
    now). **R&D-target-component linkage done (2026-10-01, `ResearchProgress`/
    `ResearchCompleted`), then upgraded to BASPM's full multi-slot/
    two-ceiling mechanic the same day once `baspm.md` §10.8 traced it** —
    see §3.4's own entry for the full, current proposal this implements
    exactly: `SetSpecialist.rdTargetComponentId`/`rdSlot`,
    `SkillGrowth.rdReliabilityGain` and its slot-band helpers, the
    composition order (same position as `TrainingProgress`, after
    `PersonnelLifecycle`), and `PersonnelDeparture.rdTargetComponentIdAtExit`.
    **The Expert trait done (2026-10-02)** - see §2.2(c)'s full decided
    shape (mission-based skill growth's own caller shipped earlier, as
    part of the mission-simulation slice below — see §2.5(g)). **Starting
    training via a real player action is now partly done**: `TrainingKind.EXPERT`
    has one (`TrainingProgress.startAstronautExpertTraining`/
    `startMissionControlExpertTraining`), and a new general
    `pullOutOfTraining` action ends any kind early - but `BASIC`/
    `ADVANCED`/`PERMANENT` still have no *start* action of their own, only
    the bot setting their fields directly. Also still open: *submitting*
    a hire as a real player action (currently a plain headless method a
    future action handler will call, no dispatch path exists yet), plus
    composing `PersonnelLifecycle`/`TrainingProgress`/`HiringPool`/a
    future R&D resolver into one real season pass (M4, not M2).
  - **Slice: programs, mission configurations, mission components &
    reliability transfer — done (2026-10-01)** — §2.3/§2.4. New
    `de.mkoehler.launchwindow.content` package: `ProgramCategory`/`Program`/
    `MissionConfiguration`/`MissionComponent` (plus `Program`'s nested
    `OpeningCostDiscount` and `MissionComponent`'s nested
    `ReliabilityTransferSource`) are immutable records, matching
    &sect;3.4's "content definitions stay data-driven/immutable" decision -
    built from in-memory fixtures for this slice, a real content-pack
    loader reading these same shapes from files is M3's job.
    `MissionConfiguration` is deliberately minimal (id, programId,
    durationInSeasons only) - prestige/goals-granted belong to the
    mission-simulation and prestige/goals slices below. `ContentCatalog`
    indexes all four by id and validates every cross-reference at
    construction (no duplicate ids, no dangling category/component/
    discount-target/transfer-source reference, no self-referencing
    discount or transfer entry), so a resolver can trust a looked-up id's
    references are real without re-checking. New
    `de.mkoehler.launchwindow.rules.ProgramOpening` (headless, same
    "plain method a future action handler will call" posture as
    `HiringPool.hire` - no action-dispatch path exists yet):
    `effectiveOpeningCost` (sums every qualifying
    `OpeningCostDiscount`, capped at 100%, re-deriving &sect;10.1's "fast
    follower" mechanic) and `reliabilityTransferFor` (sums each owned
    transfer source's own *current* reliability times its transfer
    percentage, capped at the target's own `maxReliabilityTransfer`,
    re-deriving &sect;10.3's exact formula) back `open`, which deducts the
    cost, marks the program open, and grants every required component not
    already owned at baseline-plus-transfer. **Two correctness properties
    specifically tested:** an already-owned component is never reset or
    re-granted a transfer by a second program that also requires it; and
    within one single `open` call, every newly granted component's
    transfer is computed against ownership as it stood *before* that call,
    never against a sibling component the same call is also granting (so
    two components required together, one a transfer source for the
    other, resolve the same way regardless of processing order). New
    `PlayerSnapshot` fields: `openedProgramIds` (`List<String>`) and
    `missionComponents` (`Map<String, OwnedMissionComponent>`, a new
    per-player, mutable state class - `MessageRegistry` gained an entry
    for it, appended after `TrainingKind`). **Decided (user, 2026-10-01,
    &sect;2.4(c)): the per-component reliability floor a future
    mission-simulation slice's post-mission reliability change must clamp
    against is the transfer-inclusive starting value** (baseline plus
    whatever one-time transfer bonus applied when first granted), stored
    explicitly as `OwnedMissionComponent.floorReliability` rather than
    re-derived from the catalog's bare baseline - `baspm.md` &sect;10.3
    flagged this exact choice as deliberately unresolved in BASPM's own
    code (the field name implies a transfer-inclusive floor was intended,
    but the code never actually sets it that way).
    **Unblocked the R&D-target-component linkage &sect;3.4 flagged as this
    slice's follow-up - done the same day, see &sect;3.4's own entry and
    this list's `ResearchProgress` entry below.**
    **`ProgramOpening` wired into `PersonnelLifecyclePlayouts` (2026-10-01):**
    a small fixture `ContentCatalog` (three programs, four mission
    components, two reliability-transfer relationships - only one of which
    can ever actually fire, by design - one opening-cost discount; not
    real content) lets the bot open one random not-yet-open program per
    season with probability `0.1`, in the same "journaled player action,
    runs before the season-advance resolvers" bucket as `HiringPool.hire`.
    Four new independent invariants, checked every season: funds never
    negative, every opened program id is one the catalog actually
    defines, every owned component's reliability and floor stay ordered
    `baselineReliability &le; floorReliability &le; currentReliability
    &le; maxReliability`, and every owned component is required by at
    least one currently opened program. Full numbers and the
    never-fires-by-design transfer relationship's explanation in
    &sect;3.10's own entry.
  - Slice: mission simulation, base two-roll formula only (no Expert yet,
    per the shape-defining choice above) — §2.5. **Decision gate resolved
    (user, 2026-10-01):** §2.5(c)'s outcome-tier shape is settled - port
    BASPM's full five-tier ladder, uniformly across every step, re-deriving
    Launch Window's own numbers. This slice can start.
    **Re-confirmation resolved, mechanically rather than just argued
    (2026-10-02):** Astronaut/Mission Control's departure protection uses
    `AssignmentState.ASSIGNED` as a stand-in for "on an active mission" -
    now that `MissionLaunch`/`MissionProgress` exist (§2.5(f)), launching a
    mission and resolving its first season's step batch happen in the same
    journaled-action-then-resolver pass, so there genuinely is no turn
    boundary where a crew sits `ASSIGNED` without already being active,
    exactly the reasoning the user accepted on trust back when this was
    first raised (2026-09-30).
    **Pure formula half done (2026-10-01):** `MissionStepResolution` +
    `MissionStepOutcome` (`core/.../rules`) implement the two-roll formula
    itself - see §2.5(d) for the exact shape and its four re-derived
    constants. **Step/season content shape also done the same day:**
    `MissionStep`/`MissionSeason` plus a reshaped `MissionConfiguration`
    (`core/.../content`) - see §2.5(e) for the exact fields and their
    percentage-sum rules (the crew-blend-direction question raised there
    is since confirmed - see §2.5(e)'s own "Blend shape confirmed"
    paragraph). **Per-player state, launch, and season resolution done
    (2026-10-02):** `InProgressMission`, `MissionLaunch`, and
    `MissionProgress` - see §2.5(f) for the full shape, the average-not-sum
    crew-contribution decision, and the whole-crew `CATASTROPHIC` loss
    decision. Wired into the bot harness the same day - see §3.10's own
    entry for the fixture mission configurations and the real numbers.
    **Post-mission reliability change and mission-based astronaut/
    controller skill growth done (2026-10-02)** - see §2.5(g) for the
    full shape (the per-season aggregation decision, the worst-category-
    wins rule, the shared SET-release helper, the broader-than-BASPM
    controller-growth rule) and its own harness numbers.
    **Mission cost at launch done (2026-10-02)** - see §2.5(h) for the
    new `MissionConfiguration.cost`/`MissionLaunchOutcome.INSUFFICIENT_FUNDS`
    shape and why it isn't meaningfully exercised by the bot harness.
    **The Expert trait's mission-side rescue mechanic is done
    (2026-10-02)** - see §2.5(i) for the full shape (roster-wide pool,
    stacking with exhaustion, the 95% cap) and §3.10's own entry for the
    harness numbers. **Still open, same posture as
    §2.5(f)'s own deferred list:** prestige/goals (§2.6) and the §11.3
    goal-dependency reliability modifier (both still blocked on the
    budget/prestige/goals slice below) - the crew-size question, the
    free-relaunch retirement loophole, the reliability-change/skill-
    growth items, and mission cost are all resolved - see
    §2.5(f)/(g)/(h).
  - Slice: budget/funding review, prestige, goals, and the win-condition +
    tie-break check — §2.1/§2.6/§2.9. **Design drafted and four user
    decisions taken (2026-10-03):** salaries yes, debt allowed with a
    prestige-to-funds conversion (new, no BASPM precedent), same-turn goal
    achievers all get first-time, win list is goal ids (§2.1(d), §2.6(d),
    §2.9). Remaining defaults are unconfirmed and flagged in those sections.
    Build order, each sub-slice with its own advisor review and harness
    invariants: (1) mission prestige, (2) goals and the goal-dependency
    modifier, (3) budget (with salaries, maintenance, and the conversion
    action), (4) the win-condition check.
    - **Sub-slice 1, mission prestige — done (2026-10-03).** `MissionPrestige`
      (tier ratios 60%/40%, own numbers, unconfirmed), `FlightTier`,
      `MissionPrestigeChanged`; `MissionStep.prestigeSharePercent`/
      `failurePrestigeDeduction` and `MissionConfiguration.firstTimePrestige`
      (both with defaulted constructors, so existing fixtures are unchanged);
      `PlayerSnapshot.successfulFlightCounts`; `ContentCatalog`'s share-sum
      rule (0 or 100, and 100 whenever a configuration pays). `MissionProgress`
      accumulates per season, applies a failing step as a replacement, floors
      at zero, and counts a flight only on completion. **Tier is read live, not
      stamped at launch** - a launch-time stamp would let simultaneous copies
      of one configuration all pay first-time; see §2.6(d). Unit tests:
      `MissionPrestigeTest`, `MissionProgressPrestigeTest` (including two copies
      completing in one pass). Harness: the fixture configurations now pay
      prestige, and `checkPrestigeInvariants` recomputes each season's requested
      change from the step events and catalog, checks the zero floor, and checks
      that completions bump the count per configuration. **Numbers (200 games,
      80 seasons, seed 42, same run as before): 677,880 prestige paid, 38,250
      deducted, 1,728 completed flights.** The personnel numbers are identical to
      the pre-prestige pass (1,092 resigned, 1,013 retired, 5,344 still employed,
      71,178 training completions) - prestige adds no random draws, so nothing
      else shifts. Still open for this slice: goals (sub-slice 2), budget
      (sub-slice 3), win check (sub-slice 4).
    - **Sub-slice 2, goals — done (2026-10-03).** `Goal` (first/second-time
      prestige, cancellation list, validated in `ContentCatalog`),
      `MissionStep.goalIdsAchieved`, `Program.requiredGoalIds` (an AND gate, which
      `ProgramOpening` checks and reports as `GOALS_NOT_MET`), and
      `MissionConfiguration.goalDependencies`/`goalBoosts` (`GoalModifier`). The
      game-wide count is `GoalTally`: `MissionProgress` reads it but never changes
      it, and the caller commits each resolution's `GoalSettled` events afterward,
      which gives the same-turn rule. Goal prestige folds into the season total
      (so a `MINOR_GLITCH` halves it, and a failing season forfeits it while the goal
      stays achieved and counted). Cancelled goals pay and count too. The modifier
      is read live per step (`goalModifierFor`, inside the component sum). **Not
      built:** a separate multi-player playout. The race is covered by
      `MissionProgressGoalsTest`, which drives the real `MissionLaunch` and
      `MissionProgress` against a real `GoalTally`. The main harness now has three
      goals, an explorer program gated on one, and a mercury modifier, with the
      goal prestige and gate checked each season (`checkPrestigeInvariants`
      folds it in; `checkGoalPrice` checks each settlement against the pre-commit
      tally; `checkGoalState` checks the achieved and cancelled sets). **Numbers
      (200 games, 80 seasons, seed 42): 813,110 prestige paid, 27,400 deducted,
      1,536 completed flights; 1,168 resigned, 1,057 retired, 5,333 still
      employed, 73,575 training completions, 4,632 hires, 1,870 missions
      launched.** Personnel numbers moved, because the explorer program's gate
      now changes which programs open. Goal settlement counts are not in the
      report, only the prestige they feed into. Still open: budget (sub-slice 3),
      the win check (sub-slice 4).
    - **Sub-slice 3, budget — done (2026-10-03).** `BudgetSchedule` content
      (starting funds 25,000, prestige 250, funding 3,000 per season, the ten
      review brackets close to BASPM's Normal-difficulty table, conversion rate 4
      prestige per fund - unconfirmed). `BudgetProgress.advanceSeason` runs in
      BASPM's traced order: running costs (every employee's salary, every opened
      program's maintenance, every owned component's maintenance), the review if
      `seasonsElapsed` is a multiple of 16, then funding at the rate in force.
      Catch-up is measured against the previous review's raw minimum. Funds may
      go negative. `PrestigeConversion.convert` is the headless debt conversion: only
      while in debt, capped so funds end at most two seasons of running costs, and
      it spends prestige at 4 points per fund. `Personnel.salary` (25-60, BASPM's
      range, drawn at candidate creation) and `MissionComponent.maintenanceCost`
      (BASPM's values for the fixture components) are new. Caveat: the M2 harness
      no longer checks "funds never negative", since debt is allowed. **Fixture
      costs aligned to BASPM (2026-10-03, same day):** program openings 126 /
      354 / 449 (BASPM 00-00, 00-02, 00-05 - the crewed mapping is approximate),
      program maintenance 13 / 36 / 45, component maintenance 75 / 90 / 188 / 158
      (BASPM's MaintenanceCost for Sputnik, Explorer I, Redstone, Mercury), and
      mission costs equal to the unit costs of the components flown (488 Sputnik,
      585 Explorer, 2,243 Redstone + Mercury). The Sputnik prestige fixture is now
      250 (BASPM's first-time value for that configuration). Prestige and goal
      fixture values for the other missions are still our own, flagged for M3's
      content alignment. **Numbers
      (200 games, 80 seasons, seed 42, aligned costs): 817,500 prestige paid, 43,650 deducted,
      1,583 completed flights; 1,076 resigned, 1,026 retired, 5,368 still employed,
      72,544 training completions, 4,528 hires; 1,000 funding reviews (five per
      game). The bot never went into debt at these values, so 0 debt conversions
      happened in the harness - the conversion is covered by `BudgetProgressTest`
      only. Each season's expenses and funds are checked independently.** Still open:
      the conversion and budget actions' dispatch (M4).
    - **Sub-slice 4, win-condition check — done (2026-10-03).** `WinCondition.evaluate`
      (headless, cross-player) takes the declared goal list, every player's state,
      and the catalog. A goal is done when achieved, or cancelled by an achieved goal;
      a cancelled goal's time is the turn of the earliest achieved goal whose cascade
      cancels it. A player's completion turn is the latest of their listed goal times.
      The earliest completion turn wins outright; a same-turn group is settled by the
      medal table (per listed goal, competition-ranked placings, then most first, then
      second places, and so on), with an exact tie reported as `TIED`. Six unit tests
      in `WinConditionTest`, covering no winner yet, an outright earlier finish, a
      medal-table win, an exact tie, and the cancellation timing. **Not built here:**
      the declared list itself (it lives in `game-meta.json`, M4), and any multi-player
      playout. The harness runs one player per game, so the win check is covered by
      its unit tests, not by a long simulation. **M2 status after this:** the four
      budget/prestige/goals/win sub-slices are done. Still open in M2: random events
      (§2.7), and the player-action dispatch for conversions, budget actions, and
      hires (M4).
  - Slice: random events — §2.7.
  - **The Playouts-style bot-driven harness (§3.10) starts right after the
    first resolution rule exists**, in `dev-tools`, not as a final
    milestone — grown alongside this milestone's slices, not bolted on
    after. **Started (2026-09-30):** `PersonnelLifecyclePlayouts` — see
    §3.10's own entry for what it does and its first tuning numbers.
  - **Exit criterion:** a full season/turn resolves correctly for a
    multi-season mission spanning several turns, proven by the bot harness
    across many simulated games, headlessly, with no client or server
    process involved.
- **M3 — Content format + first MVP content pack.** Data-driven
  `Program`/`MissionConfiguration`/`MissionComponent`/`Goal`/`Event`
  types (Jackson-loaded, re-deriving BASPM's Data Scripts *shape* per
  `baspm.md` "Data-driven architecture", never its values, per §1.2), plus
  a real authored **early-era-only content pack** — enough to make a small
  declared goal list achievable (e.g. a first-satellite-equivalent and a
  first-crewed-orbit-equivalent goal), not the full Sputnik-through-Apollo
  tree. **Exit criterion:** M2's bot harness runs against this real content
  pack instead of test fixtures, and actually reaches the declared goals in
  some simulated games.
  - **Decisions (user, 2026-10-03), made before M3 started:**
    1. **Values are BASPM-close, not re-derived.** This extends the budget
       exception (§2.1(d)) to the whole content pack: costs, maintenance,
       reliability, prestige, step involvements, and goal values follow BASPM's
       Data Scripts. Ids, names, and all text stay our own. This overrides the
       "never its values" wording above for M3, and §1.2's rule for these numbers.
    2. **Early-era scope:** Earth-orbit satellites and the one-crew ballistic
       capsules (the Sputnik/Explorer/Vostok/Mercury equivalents), plus the rockets
       they fly on.
    3. **Format:** JSON, one file per content type, under
       `assets/content/<pack>/`, loaded with Jackson.
    4. **Rockets work as in BASPM, including choosing one at launch.** A rocket is
       its own mission component, bought through a *rocket program* at its
       creation cost. A mission step references the configuration's rocket slot,
       not a specific rocket. At launch, the player picks any owned rocket whose
       payload capacity for that launch's type (suborbital, LEO, TLI, TPI) covers
       the payload mass. Human-rated launch types accept only human-rated rockets
       (`baspm.md` §10.9).
    5. **Content is filtered by `Archetype`.** As in BASPM, each player can only
       open their own agency's programs and rockets. Generic goals are the shared
       race; agency-specific goals are separate.
    6. **Staffing uses BASPM's slot counts.** A configuration requires as many
       controllers as its BASPM mission-control layout has slots, so crewed
       Mercury needs nine. A CAPCOM slot is an extra required astronaut whose
       skill counts on the controller side.
    7. **Buildings and upgrades, all five of BASPM's.** Astronaut Center, Mission
       Control, SET Center, Headquarters, and the VAB all start at level 1 for
       free; there is no construction step. Upgrades use BASPM's costs, build
       times, per-level maintenance, and caps. The three personnel buildings cap
       their rosters (level 1: 7 astronauts, 5 controllers, 7 SET), Headquarters
       caps open payload programs, and the VAB caps open rocket programs and the
       rocket class (Light, Medium, Heavy by VAB level), as BASPM's code does
       (`baspm.md` §10.9; the option text said "concurrent missions", which was
       my misreading before the trace). These caps replace the earlier 8/12/8
       roster capacity (§2.2(c)).
  - **Defaults chosen without asking (unconfirmed, flagged):** opening a program
    also charges the creation cost of every component it grants that the player
    doesn't own yet, as BASPM does. A mission costs the unit cost of its rocket
    plus its payloads. The repeat-flight prestige tiers move from 60%/40% to
    BASPM's 50%/30%. A Flight Director slot's involvement is spread evenly
    across the five controller skills. Controller slots map to skills by
    BASPM's speciality index, not by the slot's name. A category's required goals
    are copied onto every program in it. Goal dependencies and boosts that
    reference goals outside the pack are dropped.
  - **Slice 1, content-pack loader — done (2026-10-03).** `ContentPackLoader` reads
    `categories.json`, `programs.json`, `mission-components.json`,
    `mission-configurations.json`, and `goals.json` from a folder or the classpath.
    The JSON maps one-to-one onto the content records, so record validation and
    `ContentCatalog`'s cross-reference checks both run. The mapper is strict:
    unknown fields and missing numbers or flags fail, and only a missing list
    defaults to empty. `server` and `lwjgl3` already bundle `assets/` on the
    classpath; `core` and `dev-tools` don't.
  - **Slice 2, buildings — done (2026-10-03).** `Building` (five values, Kryo-registered),
    `BuildingSchedule.baspmClose()` (caps, upkeep, upgrade cost and seasons per
    level, from `baspm.md` §10.9), `PlayerSnapshot.buildingLevels`/
    `buildingUpgradeSeasonsRemaining` (a missing level reads as 1), and
    `BuildingUpgrades` (`startUpgrade` pays up front, refusing a second upgrade of
    the same building, the top level, or short funds; `advanceSeason` finishes
    upgrades). `ContentCatalog` carries the table, defaulting to BASPM's.
    `HiringPool.hire` caps each roster by its building, replacing the 8/12/8
    constants. `ProgramOpening` refuses past the Headquarters cap
    (`HEADQUARTERS_FULL`). Running costs include building upkeep (925 per season
    at level 1). The VAB cap is wired in the rocket slice. Harness (200 games, 80
    seasons, seed 42): the bot starts an upgrade at 5% per season, and upgrades
    finish before the budget pass. Independent invariants check the charged cost,
    the level after the pass, hires against the cap read from the table, and open
    programs against Headquarters. Result: 754 upgrades started, 749 completed,
    4,871 hires, 569 programs opened, 1,920 missions, still no debt conversions.
- **M4 — Integration: wire M2/M3's real engine into M1's proven skeleton.**
  Replace M1's placeholder trivial action with real player actions (assign
  personnel, open a program, queue a mission) and real turn resolution.
  Slot-claiming/late-join (§3.8) and the game-meta file (schedule config,
  blackout list, per-player summary for the goal-comparison screen, §3.7)
  land here too. **Exit criterion:** the same proof as M1's, but resolving
  a real turn through the real engine and real content, still via a
  CLI-style dev client.
- **M5 — Playable client (libGDX UI).** First real playtest happens at the
  end of this milestone. Expected slices, sizing not yet estimated:
  connect/login/slot-claim screens; roster/personnel management;
  program/mission selection bundled with the budget/prestige overview and
  goal-comparison screen (§2.6(b) established this bundled placement,
  including the declared goal list itself, §2.9); the mission replay view
  (§2.2(c)'s "adopt regardless" decision — this is v1, not deferred); a
  win/game-over screen. Game *creation* itself (declaring the goal list,
  picking a schedule profile) is **not** a client screen in v1 — see M1's
  server-config-file note and M7+ below. **Build the offscreen screenshot
  tooling (§3.11) alongside whichever slice introduces the first real
  screen**, not as its own separate slice — `design.md` §3.11 already
  commits to this exact sequencing. **Exit criterion:** the milestone's own
  remaining bar is the owner actually running a first real playtest, not a
  further slice to build (Robot-Rampage's own M4 phrasing) — this is where
  the deferred-deployment choice above matters: that playtest runs in
  dev-mode, not from a packaged release.
- **M6 — Deployment.** jpackage client zip, Docker server image,
  tag-driven GitHub Actions release, reusing the sibling projects' own
  working templates (§3.3/§3.9). Comes after M5's playtest validates the
  loop is worth packaging, not before.
- **M7+ — Second wave, order not yet decided:** active
  interference/sabotage/espionage (§2.9's brainstorm, deliberately deferred
  past passive-competition-only v1); the rest of the Sputnik-through-Apollo
  content tree beyond M3's
  early-era slice; the educational layer's actual UI/content (§1.1); real
  art via the asset generation pipeline (§3.5, its own small project); a
  real "game host" role/UI (§3.6/§3.8, currently just direct server-file
  access); validating the `WEEKDAYS` schedule profile with the actual
  colleagues group.
