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
  revisit the upper bound once the competitive-interaction model (§2.x, TBD)
  is chosen, since interference/sabotage mechanics may scale differently
  than passive competition does.
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
  content, if a dedicated screen turns out to be worth it.
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

- 2.1 Space agencies, budget & funding cycle
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
    actually resolves, never on a skipped real day. The one new problem this
    pass surfaces: **Tiger Teams needs a pre-resolution policy, same as
    scrub/training-pull-out/`REB*` choices (§4)** — but unlike those, one of
    its options ("decline, no cost, no boost") is always free and never a
    strictly bad choice, so it has a uniquely safe auto-resolve default. The
    "retirement/resignation only checked while idle" behavior is a design
    choice worth deciding on deliberately rather than inheriting by default —
    it could read as a fairness quirk (an offline player's idle-but-neglected
    staff drift towards leaving, while a busy roster is silently protected)
    once players start comparing notes across an async multiplayer game.
  - **(c) Decision:** **port the three-group/skill shape and the general
    morale/training/lifecycle mechanics** as Launch Window's starting point,
    re-deriving actual numbers rather than reusing BASPM's constants verbatim
    (`design.md` §1.2). **(unconfirmed default)** keep "standing orders, no
    per-turn re-declaration" for R&D-equivalent and permanent-training
    assignments, and keep retirement/resignation gated on being idle for a
    first playable version — revisit only if playtesting makes it feel like
    an exploit. **(unconfirmed default)** add Tiger-Teams-equivalent to §4's
    mid-turn-decision list with a firm answer already available: **auto-
    resolve to "decline" unless the player has pre-set a spending policy/
    budget cap** — the one mid-turn case in this system with a genuinely
    free, always-safe non-interactive default. **(unconfirmed default)**
    keep the morale-gates-hiring-pool-quality feedback loop as a systemic
    mechanic, independent of whichever exact skillset/morale buckets Launch
    Window ends up using.
- 2.3 Programs & mission configurations (the Sputnik→Apollo tech/content
  tree)
- 2.4 Mission components (rockets, capsules, boosters) & reliability
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
- 2.6 Prestige, goals & scoring
- 2.7 Random events
- 2.8 Campaign objectives / win conditions
- 2.9 **Multiplayer interaction model** — the one system with no BASPM
  precedent beyond its arms-length PBEM mode (`baspm.md`). Open brainstorm,
  nothing decided yet:
  - Passive competition only (both agencies race for the same historical
    firsts/prestige, visible to each other only through news-style reports
    and a goal-progress comparison screen — this is BASPM's actual existing
    `MultiplayerColdWar`/`CampaignRTTM` shape, not just an analogy: it really
    does expose which generic goals the opponent has achieved via a
    side-by-side comparison screen, `baspm.md` §5/§2.4, even though the two
    sides otherwise play in isolation).
  - Direct communication: in-game chat/mail between players.
  - Active interference ("sabotage"): espionage, poaching the other side's
    personnel, disrupting a rival's launch window — needs careful balance so
    it doesn't just punish whoever logs in less often, given the async
    cadence.
  - Cooperative options (joint missions, shared programs) as a distinct mode
    alongside head-to-head.
  - **(unconfirmed)** start with passive competition + chat for a first
    playable version; layer sabotage/interference in once the core daily-turn
    loop is proven fun and fair.
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

Not yet decided. Candidate shape **(unconfirmed)**: no persistent game
server process is strictly required by the async/daily-batch model the way
StarWars' real-time server or Robot-Rampage's live session-holding server
are — a player's "connection" is really just authenticated HTTP requests
against a backend that stores per-turn intents and a scheduled job that
resolves all pending turns once a day. This is architecturally closer to a
web app with a cron job than to either sibling project's netcode. Whether it
still benefits from a long-running server process (for push notifications,
an admin/ops view, or reusing sibling-project infrastructure) is open — to
be revisited once §2's mechanics are pinned down enough to know what state
the server actually needs to hold and how often clients need to poll/be
pushed to.

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

Not yet decided. Options to weigh once §2/§3.1 are further along:
- Reuse the sibling projects' Java/libGDX/Maven/KryoNet stack for continuity
  (client tooling, asset pipeline conventions, JUnit testing conventions) —
  even though KryoNet's TCP/UDP dual-channel netcode is arguably wasted on a
  request/response daily-batch game.
- A plainer web stack (HTTP/REST or GraphQL API + a browser client) might fit
  the "check in once a day" session shape better than a native game client
  players have to keep installed/updated — worth an explicit discussion
  before committing, since it's a much bigger platform decision than
  anything either sibling project had to make.

### 3.4 Data model

Not yet started — will be derived from BASPM's Data Scripts XML schemas
(`baspm.md` §"Data-driven architecture") once §2 mechanics are further along.

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
- **(unconfirmed default)** Two named presets to start:
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
  **(unconfirmed default: host's local timezone at game creation, stored
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
- **(unconfirmed default)** Only future dates can be blacked out — a
  blackout can't retroactively undo a turn that already resolved. Adding one
  that overlaps *today* before today's resolution job has fired should still
  suppress today's turn; the scheduler's "does a turn resolve today" check
  needs to run its blackout lookup at trigger time, not from a snapshot
  taken earlier.
- **Who can edit the blackout list (open question, not yet decided):**
  probably whoever "owns"/hosts that game instance, but this project has no
  admin/role model yet — revisit once §2's multiplayer interaction model
  and any notion of a "game host" role are designed.

**What happens on a non-resolving day:** nothing special — it's not a
"skipped turn" needing its own state, it's simply a day the resolution job
doesn't fire. Players can keep reviewing/editing their already-queued
actions for the *upcoming* turn; nothing changes about their queued intents
across a blackout or weekend. The in-game turn counter only increments on
days the job actually runs, so "one turn = one season" (§1) stays exactly
true regardless of how many real days a given turn's window spanned.

**Architecture implication for §3.1:** this rules out a single naive global
cron expression as the whole scheduling mechanism. The actual shape needed
is closer to: a lightweight scheduler (cron-triggered, e.g. hourly) that,
for *each active game*, checks that game's own weekday profile + blackout
list to decide whether *that game* resolves today, then fires its
turn-resolution job independently. Confirms §3.1's "stateless API + scheduled
batch job" candidate shape is still workable — it just means the job is
per-game-aware rather than one-size-fits-all, which was always implied by
supporting multiple concurrent games (friends' game and colleagues' game
running side by side, on different schedules) rather than a single global
game instance.

## 4. Open questions (tracked here until resolved)

- **Mid-turn/mid-mission player decisions — narrowed by the §2.5 mission-sim
  trace (2026-09-27).** BASPM stops and waits for a player click at several
  points *within* a turn, not just between turns: scrubbing a scheduled/
  in-progress mission, pulling an employee out of training early, some
  `REB*`/decision-event choices (`baspm.md` §7.2). Originally worried this
  meant a daily-batch resolver where nobody's expected online has no obvious
  equivalent — **the mission-simulation trace resolves the "mid-*resolution*"
  half of that worry: a season's step-by-step roll sequence is fully
  automatic once triggered, needing no player input while it runs** (`baspm.md`
  §8.1/§8.2) — every one of BASPM's own stop-and-click points (scrub/launch/
  assign, training pull-out, `REB*` choices) happens *before* that sequence
  starts, never interleaved with it. **What's still genuinely open is only
  the *pre*-resolution question:** what does a player who doesn't log in
  before a turn's server tick implicitly decide for each of those points?
  Candidates unchanged: submit conditional policies alongside the rest of the
  turn ("scrub this mission if X happens"), defer the decision to the next
  day's turn at some in-fiction cost, or auto-resolve with a
  player-overridable default up until that day's server tick.
  **(unconfirmed)** — needs deciding before §2.7 (random events) can be
  finalized. **One sub-case already resolved favorably by real savegame
  evidence (`baspm.md` §2.1, 2026-09-27 — this citation previously pointed
  at a §8.1 that didn't yet exist; backfilled properly this session):
  ongoing R&D assignment is NOT one of the awkward mid-turn cases** — despite
  its state name (`ASSIGNED_TO_RD_ONE_TURN`), an assignment persists
  automatically across seasons with no re-declaration needed, confirmed by
  tracking one employee staying assigned to the same component across four
  consecutive seasons untouched. So a Launch Window turn's "who's on R&D" input
  can be a standing order the player sets and leaves alone, not something
  that has to be resubmitted every single day. The remaining open cases
  (scrub, training pull-out, random-event choices) still need a policy —
  and the scrub case has a real nuance now too: a save showed a mission
  scrubbed *before* it ever started executing costing nothing (`MISSION_OUTCOME_CANCELLED`,
  0 prestige) — worth testing separately whether a mid-flight abort is
  actually penalized (`MsnSim*.xml`'s `OnCancellation` fields suggest it
  should be) before assuming "scrub" is uniformly free or uniformly costly.
  **New case found by the §2.2 personnel trace (2026-09-27, `baspm.md`
  §9.5): Tiger Teams (pay to boost a step's saving-throw odds) is a genuine
  mid-mission-*step* pause, not just a between-turns one — but it already
  has a firm answer, unlike the others: auto-resolve to "decline" (free, no
  effect) unless the player has pre-set a spending policy, since decline is
  never a strictly bad choice for someone who wasn't online to answer.**
- Turn cadence resolved (§1/§3.6): one turn = one BASPM "season", schedule
  (which real days produce a turn) is per-game-configurable. Remaining
  sub-question: who is allowed to edit a game's blackout-period list once
  play has started — needs a "game host/admin" role concept that doesn't
  exist yet (§3.6).
- Matchmaking model / how a friends-group game and a colleagues-group game
  (different schedules, different rosters) both exist as separate concurrent
  game instances on the same server? (§1, §3.6)
- Multiplayer interaction model: how much of §2.9's brainstorm ships in v1?
- Client platform: native (libGDX, like the sibling projects) vs. web? (§3.3)
- Does the server need a persistent process at all, or is it a stateless API
  + scheduled batch job? (§3.1)
- How much of BASPM's historical Cold War framing (real programs, real
  agencies) do we keep vs. reskin, given this ships under a different name
  and is meant to stand on its own as a fan project? (§1)
