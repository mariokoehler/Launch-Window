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
  a short beat wherever Launch Window's own gold/silver/bronze goal rating
  (§2.6(c)) is first explained to a player — this is how real missions
  actually get scored, not just a game abstraction layered on top.

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
    assignments, and keep retirement/resignation gated on being idle. **Decision:** keep the morale-gates-hiring-
    pool-quality feedback loop as a systemic mechanic, independent of
    whichever exact skillset/morale buckets Launch Window ends up using.
    - **Tiger Teams — decided to diverge from BASPM here (user design input,
      2026-09-27): promote it from a live per-step spending decision into a
      developed personnel capability**, rather than a pre-declared per-turn
      budget policy. A SET or Mission Control employee (no new department)
      can earn an **"Expert"** trait (working name) once a skill crosses a
      threshold and/or via a dedicated advanced-training track, alongside
      their normal duties — reusing the exact standing-order shape
      `baspm.md` §2.1's R&D assignment and §9.3's permanent training already
      established, rather than inventing a new per-turn UI element. During turn
      resolution, a step needing a saving-throw rescue automatically draws on
      any currently-available Expert-flagged personnel; no live decision, no
      per-turn "what if" declaration required from the player at all.
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
    feel earned rather than automatic.
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
    counts/pacing regardless). **(unconfirmed default)** carry over the
    outcome taxonomy's *shape* (a clean success, a "rescued" success, a
    half-credited partial failure, a full failure, and a crew-fatality-
    eligible worst tier) without committing to BASPM's exact five-plus-one
    labels — revisit once §2.6 (prestige/scoring) is designed, since the
    outcome tiers only matter insofar as they map to a scoring/consequence
    model we still need to design fresh. **Caveat (user in-game check,
    2026-09-27, `baspm.md` §8.4 item 1): the "half-credited partial failure
    that lets the mission continue" tier may not actually be reachable in
    BASPM** — the user only ever saw a glitch either get fully rescued or
    escalate straight into a mission-ending failure, never a
    continuing-but-degraded outcome. Not yet re-confirmed against the
    decompile, so treat the fifth tier above as tentative; it may collapse to
    four tiers instead. Multi-season missions are adopted
    as-is: a mission genuinely taking several turns to play out (with
    escalating stakes each turn, per the Sputnik example in `baspm.md` §8.1)
    is good game feel to keep, not an artifact to engineer away.
    **Potential direction, not yet decided (user, 2026-09-27): design our own
    step-outcome model from first principles — "vital" vs. "optional" steps —
    rather than trying to replicate BASPM's exact tiering.** The idea: some
    steps are make-or-break (e.g. ascent) where failure should end the
    mission and can be crew-fatality-eligible; others are lower-stakes (e.g.
    an in-flight science-collection step) where failure should only reduce
    rewards, not end the mission outright. **This downgrades the still-open
    `baspm.md` §8.4 item 1 question** (whether BASPM's own unresolved
    `MINOR_GLITCH` ever lets a mission continue) **from blocking to merely
    curious** — since Launch Window would define its own vital/optional split
    rather than inherit BASPM's exact step semantics either way, it no longer
    gates any design choice here. This idea also directly motivates a tiered
    goal-achievement rating, cross-referenced into §2.6(c) below.
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
    **Potential direction, not yet decided (user, 2026-09-27, follows from
    §2.5(c)'s "vital vs. optional step" idea): a tiered goal-achievement
    rating** — e.g. gold/3-star for completing a mission without failing any
    optional step, silver/2-star for at most one optional-step failure,
    bronze/1-star for more — giving goal achievement a quality dimension
    beyond BASPM's plain achieved-or-not, without needing a vital step to
    ever fail (that still just ends the mission, per §2.5). No BASPM
    precedent for this; it's a Launch Window-original scoring layer, floated
    as a direction worth keeping in mind rather than committed to yet.
    **No longer purely cosmetic if it ships:** §2.9's win-condition
    tie-break rule (decided 2026-09-28) uses goal-achievement rating as its
    second-order tie-break, so deciding this one way or the other now also
    affects whether that tie-break rule has anything to compare.
    **A second reason to actually ship this (user, 2026-09-28, tying into
    §3.12's logo work): a goal achievement is the natural place to render
    the "mission patch" visual language the logo itself is being built
    from** — the same circular border/rim-text frame as the brand mark,
    with the center art swapped per goal and the ring closed solid gold
    once earned (open/partial gold = the window is still active; closed
    gold = hit it). This doesn't force the gold/silver/bronze quality tiers
    above to be decided — a binary achieved/not-achieved badge already
    works with the same frame — but if the tiered rating does ship, the
    tiers are an obvious second visual axis (e.g. the rim color/thickness,
    or a small corner mark) rather than a whole new badge shape.
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
    tie-break** (only reached if the medal-table comparison is itself still
    tied): best goal-achievement *rating* — this makes §2.6(c)'s
    not-yet-decided gold/silver/bronze tiered goal-achievement rating idea
    load-bearing rather than cosmetic *if* it's ever implemented; until/
    unless that's decided, this tie-break has nothing to compare and simply
    doesn't apply (an unresolved tie at that point is vanishingly unlikely
    given how many independent rankings precede it, and isn't worth
    designing for further right now).
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
  `MissionControlSkill`.
- **`AssignmentState`** — `IDLE`/`ASSIGNED`/`IN_TRAINING`; deliberately does
  not yet distinguish R&D from a mission assignment, since neither exists as
  a model type yet.

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
`core/src/test/.../model`, per `CLAUDE.md`.

**Content definitions themselves remain not yet started** — still to be
derived from BASPM's Data Scripts XML *shapes* (`baspm.md` §"Data-driven
architecture"), re-deriving Launch Window's own actual values per §1.2, once
picked up.

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

**Launch Window's own equivalent, not yet built — correctly deferred, since
there's no rules engine yet (§3.4's status):** once the turn-resolution
engine exists, build an equivalent `Playouts`-style harness — simple bots
making plausible per-season decisions (assign personnel, queue a mission,
open a program), run many simulated multi-season games from a seed, and
report the metrics that actually matter here: whether a multi-season
mission completes correctly across turn boundaries (a real, non-trivial
case per `baspm.md` §8.1's finding that missions commonly span several
seasons), funds/prestige trajectory over N simulated seasons (does an agency
go bankrupt, does the budget-review math actually converge sensibly), and
whether the NASA-style and Soviet-style archetypes (§2.1(a1)/(b1)'s real
content differences) actually play out roughly evenly rather than one
snowballing. This belongs in `dev-tools`, matching
Robot-Rampage's placement exactly, once `dev-tools` has its first real tool
(currently empty, §3.3).

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

**Iconography: first real icon set exists (2026-09-29), still raw.** Five
solid-silhouette icons for the `SetSkill` categories (Rockets, Space
probes, Human-rated rockets, Crewed spacecraft, EVA suits) — the user
generated original bitmap art with an AI image tool and had it contour-
traced to SVG, at `assets-raw/svg/icon_*.svg`. First used as the sort-by-
skill buttons on a Personnel Recruitment screen mockup (a Claude Artifact,
not yet a repo screen); confirmed a second, independent use the same day
when the user pointed out BASPM's own training/skill-selection dialog
uses these same five categories as its icon set — real evidence the
`SetSkill` enum is the right single join point for this artwork wherever
a skill needs a glyph (recruitment, training assignment, R&D assignment
screens all plausibly reuse it), not a one-screen throwaway. **Still
open:** these are raw traced files, not yet promoted into `assets/` or
wired into `Theme`'s icon pipeline (`Theme` has no icon concept yet at
all — see its own "no icon system yet" status) — that promotion, plus
deciding a consistent on-disk/atlas format, is a real task for whenever
the first screen using them gets actually built, not done implicitly by
having them exist in `assets-raw/`. **Provenance, since this exact
mistake happened once already this session:** these are traced from the
user's own new bitmap art, not from BASPM — the five icon SVGs that
*were* traced from BASPM's SET Centre UI got caught and removed the same
day (see `CLAUDE.md`'s source-material note for the full story); don't
conflate the two when this section is read back later.

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
- **The "Expert" personnel trait (§2.2(c)'s Tiger Teams replacement) ships
  after the base mission-simulation formula, not with it.** Mission
  simulation's first version is the base involvement-weighted roll plus a
  flight-controller-skill-scaled saving throw only, with no automatic
  rescue beyond that. The Expert-trait auto-rescue and its still-undecided
  exhaustion/falloff curve is exactly the kind of tunable detail worth
  deferring until there's a playable loop to tune it against — added as a
  later second-wave slice, mirroring how Robot-Rampage's M5 added pushers/
  crushers onto an engine that already structurally supported them.

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
    standing orders, skill growth) — §2.2.
  - Slice: programs, mission configurations, mission components &
    reliability transfer — §2.3/§2.4.
  - Slice: mission simulation, base two-roll formula only (no Expert yet,
    per the shape-defining choice above) — §2.5. **Decision gate:** §2.5(c)'s
    outcome-tier shape (four vs. five tiers, "vital vs. optional step") is
    still marked unconfirmed and must be settled before this slice starts.
  - Slice: budget/funding review, prestige, goals, and the win-condition +
    tie-break check — §2.1/§2.6/§2.9.
  - Slice: random events — §2.7.
  - **The Playouts-style bot-driven harness (§3.10) starts right after the
    first resolution rule exists**, in `dev-tools`, not as a final
    milestone — grown alongside this milestone's slices, not bolted on
    after.
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
- **M7+ — Second wave, order not yet decided:** the Expert personnel trait
  and its exhaustion/falloff curve (§2.2(c)); active
  interference/sabotage/espionage (§2.9's brainstorm, deliberately deferred
  past passive-competition-only v1); the gold/silver/bronze goal-achievement
  rating (§2.6(c), still not decided at all — and if it never is, §2.9's
  win-condition tie-break simply never reaches that tier, which is fine);
  the rest of the Sputnik-through-Apollo content tree beyond M3's
  early-era slice; the educational layer's actual UI/content (§1.1); real
  art via the asset generation pipeline (§3.5, its own small project); a
  real "game host" role/UI (§3.6/§3.8, currently just direct server-file
  access); validating the `WEEKDAYS` schedule profile with the actual
  colleagues group.
