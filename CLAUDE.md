# CLAUDE.md — Launch Window project notes

Cross-session memory for this repo: gotchas, decisions and their reasons,
conventions — so future sessions don't have to rediscover them. Game design/
architecture (what the game *is*, how each system is meant to work) belongs
in `design.md`, not here. Everything we learn about the **original** game
we're adapting (Buzz Aldrin's Space Program Manager, "BASPM") belongs in
`baspm.md` — a reference dictionary/index into its decompiled source and
extracted assets, not a copy of its content. All three files get updated the
same session a decision/finding is made, not after.

## What this project is

**Launch Window** — *"Stay one small step ahead."* — a non-commercial fan
project reimagining **Buzz Aldrin's Space Program Manager** (BASPM,
Slitherine/Matrix Games, Unity) as an **asynchronous multiplayer**
space-agency management game. Name and tagline decided 2026-09-27 (a working
title of "Space Race" was used before this). Where BASPM is single-player/
turn-based-realtime (or PBEM 1v1, see below), Launch Window is a
daily-cadence async multiplayer game: players log in once a day (or
whenever), queue up actions for the current turn, and a server-side process
resolves everyone's turn at a fixed time, after which players return
to see results and plan the next turn. A full game is expected to span weeks
to months, not a single sitting.

Full design lives in [`design.md`](./design.md) — read that first for concept,
mechanics, and architecture decisions. Everything about *how the original
game works* (menus, formulas, data files, source layout) lives in
[`baspm.md`](./baspm.md) — read that when a design decision needs to compare
against or diverge from the original.

**Sibling projects** (same author, prior multiplayer games — mined for
patterns, not code, since each has a different network model):
- `C:\Users\mario\StarWars` — real-time authoritative-server deathmatch
  shooter (KryoNet, UDP snapshots, client prediction/reconciliation). Its
  `CLAUDE.md`/`design.md` pairing is the template this project follows.
- `C:\Users\mario\Robot-Rampage` — turn-based simultaneous-programming board
  game adaptation (KryoNet, TCP-only, command/event protocol, no prediction).
  Much closer in shape to what Launch Window needs than StarWars is: turn-based,
  simultaneous secret input, server-resolved. The **big difference from
  Robot-Rampage**: that game resolves a turn the instant every player
  confirms (or a short timer expires) — a live multi-minute session. Space
  Race resolves on a **daily wall-clock schedule** regardless of who's online
  when — nobody is ever "waiting live" for other players, which changes the
  netcode shape entirely (no persistent connections during a turn, no
  reconnect-during-programming machinery, but real scheduled-job
  infrastructure and notification/email needs instead).

**Genuinely relevant prior art found inside BASPM itself** (see `baspm.md`
§5, "PBEM multiplayer"): the original game already shipped **two
asynchronous multiplayer modes** (`MultiplayerColdWar`, `MultiplayerFreeform`)
built on a play-by-email-style HTTP protocol (`PBEMManager.cs`) — challenge/
accept, upload/download a turn's save-game blob, resign, claim-on-timeout.
The manual confirms it's **strictly 1v1** (host + one opponent, "you'll get
an e-mail notification that will inform you when to play your next move").
It proves the underlying single-player simulation was already built to be
paused, serialized, handed off, and resumed — exactly the property a
daily-batch async multiplayer server needs. It also isn't fully isolated
the way an early pass at this analysis assumed: `GameWorld` tracks the
opponent's login name/agency and exposes which *generic goals* they've
achieved (`GetAllGenericGoalsAchievedInfoByOpposingFaction`), which a
side-by-side comparison screen renders — real, numbers-backed precedent for
a "see your rival's progress" feature. What's still genuinely absent is any
*interactive* cross-player mechanic — no chat, no sabotage, no trading —
that part of Launch Window's design has no BASPM precedent. Worth studying its
turn-boundary/serialization design (`baspm.md` §5) before inventing our own
from scratch.

## Current status

**Analysis phase — no code written yet.** This session's deliverable is the
initial `CLAUDE.md`/`design.md`/`baspm.md` scaffolding plus a first pass at
indexing BASPM's decompiled source and extracted assets so future sessions
can navigate it quickly. Not a git repository yet — ask before running `git
init` or making it public.

## Source material locations (read-only, reference-only — never edit, never copy into the repo)

- **Game install** (Steam):
  `E:\Steam\steamapps\common\Buzz Aldrin's Space Program Manager`
- **Extracted/decompiled analysis dump**: `E:\Temp\BASPM` — decompiled C#
  source (`source/`), unpacked asset bundles (`extracted/`), exported
  MonoBehaviour instance data as JSON (`monobehaviours/`). Full map in
  `baspm.md`.
- **These are for private design analysis only.** Nothing under either path
  — text, art, audio, video, code — may be copied or lightly paraphrased
  into the Launch Window repo itself: see `design.md` §1.2 (hard rule, not a
  preference — this repo is public on GitHub, so anything committed to it is
  effectively redistributed). Read for understanding, then write Space
  Race's own original version from the underlying real-world facts.

## Conventions / preferences (carried over from the sibling projects)

- **Design decisions go into `design.md` immediately**, same session as the
  decision — not summarized after the fact. Findings about the original game
  go into `baspm.md` the same way.
- **When a sub-detail is left unspecified, propose a concrete default
  directly in the doc and flag it as unconfirmed**, rather than stalling to
  ask. Reserve a real pause/question for foundational or hard-to-reverse
  choices (a core library, a balance-defining formula, the network model).
- **Proactively flag security-relevant concerns during design even
  unprompted** (e.g. auth, save-data trust boundaries) — kept brief.
- **Proactively flag educational-content opportunities, unprompted, the
  moment they come up** (user, 2026-09-27 — `design.md` §1.1 is the standing
  design pillar this serves). Launch Window wants to explain the real
  engineering/physics behind a mission, not just theme itself around real
  programs — e.g. why orbital rendezvous is counterintuitive, why restarting
  an engine in zero-g is hard. Any time BASPM analysis or Launch Window design
  work surfaces a real historical mission, a real engineering challenge, or
  a place BASPM's own historical-facts text (`baspm.md` §3.2) stops short of
  explaining *why* something was hard, say so and note it in `design.md`
  §1.1 the same session — don't save it up for later.
- Commits (once a repo exists) should be GPG-signed — if `git commit` fails
  with a `gpg-agent` connect error, start Kleopatra
  (`C:\Program Files (x86)\Gpg4win\bin\kleopatra.exe`) and retry the same
  commit; never `--no-gpg-sign`.
- **Advisor before and after** on any multi-file feature once implementation
  starts: consult before writing code, again once it looks complete. Caught
  real design-level and integration-level bugs on both sibling projects.
- Fan-project note: BASPM's names/art/data are Slitherine/Matrix Games'
  IP, referenced here purely for non-commercial analysis and inspiration,
  the same posture StarWars/Robot-Rampage take toward their own source
  material. Launch Window ships its own name, art, and (likely reworked)
  program/mission naming — not a copy of BASPM's content. **This is now a
  hard rule, not just intent — see `design.md` §1.2 and the source-material
  note above: no verbatim or lightly-paraphrased text, no reused art/audio/
  video files, ever committed to this repo.**

## Build system

Not yet chosen. `design.md` §3 will record the decision once made — likely to
lean on the same core stack as the sibling projects (Java/libGDX/Maven +
KryoNet or a plain HTTP/REST API, given the async daily-batch shape may not
need a persistent-socket game server at all — open question, see `design.md`).
