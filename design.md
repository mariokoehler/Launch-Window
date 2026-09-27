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
  must confirm before the turn resolves" model. **(unconfirmed)** turn cadence
  is one real day = one game turn; BASPM's own turns are "seasons," and the
  manual confirms exactly one season = one quarter of a year (four seasons/
  year, budget review every four years/16 seasons — `baspm.md` §3.1/§7) —
  whether a Launch Window turn maps to a BASPM season or to something
  finer-grained (e.g. a season takes several daily turns to play out, mission
  steps trickling in day by day) is an open question, see §7.
- **Match length:** a full game is expected to run **weeks to months** of
  real time, not a single sitting — a deliberate consequence of the daily
  cadence, not a limitation to design around.
- **Players:** more than BASPM's hard 1v1 cap (its own PBEM multiplayer modes
  were strictly two-player — see `baspm.md`). Target player count
  **(unconfirmed)**, likely small-group (perhaps 2-8, echoing the sibling
  projects) rather than a persistent-world MMO — revisit once the
  competitive-interaction model (§2.x, TBD) is chosen.
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
- 2.2 Personnel (astronauts, flight controllers, SET/scientists) — hiring,
  training, skills, morale/fatigue
- 2.3 Programs & mission configurations (the Sputnik→Apollo tech/content
  tree)
- 2.4 Mission components (rockets, capsules, boosters) & reliability
- 2.5 Mission simulation (the step-by-step launch/flight resolution, success/
  failure rolls)
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

## 4. Open questions (tracked here until resolved)

- **Mid-turn/mid-mission player decisions.** BASPM stops and waits for a
  player click at several points *within* a turn, not just between turns:
  scrubbing a scheduled/in-progress mission, pulling an employee out of
  training early, some `REB*`/decision-event choices (`baspm.md` §7.2). A
  daily-batch resolver where nobody's expected to be online has no obvious
  equivalent. Candidates: submit conditional policies alongside the rest of
  the turn ("scrub this mission if X happens"), defer the decision to the
  next day's turn at some in-fiction cost, or auto-resolve with a
  player-overridable default up until that day's server tick.
  **(unconfirmed)** — needs deciding before §2.5 (mission simulation) and
  §2.7 (random events) can be finalized. **One sub-case already resolved
  favorably by real savegame evidence (`baspm.md` §8.1, 2026-09-27):
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
- Turn cadence: one real day = one BASPM "season", or finer-grained daily
  turns within a season? (§1)
- Player count per game / matchmaking model? (§1)
- Multiplayer interaction model: how much of §2.9's brainstorm ships in v1?
- Client platform: native (libGDX, like the sibling projects) vs. web? (§3.3)
- Does the server need a persistent process at all, or is it a stateless API
  + scheduled batch job? (§3.1)
- How much of BASPM's historical Cold War framing (real programs, real
  agencies) do we keep vs. reskin, given this ships under a different name
  and is meant to stand on its own as a fan project? (§1)
