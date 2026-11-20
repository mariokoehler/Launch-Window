# A release branch for the live game

**Status: plan, not built** (user, 2026-11-20). To be refined before the first live game in January 2027;
nothing is set up yet. Linked from `design.md` §5, M6.

## The plan

When the first live game starts, cut a release branch from `master` (name open, e.g. `release/1.0`) for the
version being played.

- **The release branch** takes only fixes and additions that the live game can take without a data reset. Each
  is released from there and merged into `master`. Merges go one way only, branch into `master`, never back.
- **`master`** takes everything that would need a reset. That work is for a later game and release.

## Refinements discussed (2026-11-20)

1. **What counts as needing a reset, made checkable.** On the release branch, a change is unsafe if it:
   - touches a **pack file** (anything in `ContentPackLoader.FILES`, a text fix included): the live game's pack
     hash is frozen. Text files outside the pack (`strings.json`, `help.json`, front pages, stories, ...) are fine.
   - **breaks the saved format** (a saved field renamed, removed or retyped, or an enum constant renamed).
     `SavedFormatTest` against the `v1` fixture, written at the release, catches this on the branch, so keep it
     green there. `SnapshotMigrations` remains the way out if a critical fix must break the format mid-game.
   - **changes a message's shape**: no reset, but client and server must ship together (the handshake refuses the
     old side), so players update at the moment the server does.
2. **Rule changes:** technically safe, but they change a game in progress. Proposed soft rule (unconfirmed): bug
   fixes and quality-of-life changes on the branch; balance changes only when something is clearly broken.
3. **Tags and the client updater:** `update.cmd` downloads `releases/latest`, and every `v*` tag publishes a
   GitHub Release (and pushes the server image as `:latest`). A tag on `master` would put every player on a
   client the live server refuses. Either tag only from the release branch, or have `release-client.yml` mark
   tags like `v2.0.0-beta.1` as pre-releases so they never become "latest". The NAS's compose file pins the image
   version, so the server isn't at risk.
4. **Versions:** patch releases `v1.0.x` are tagged on the branch and reach `master` through the merges (jgitver on
   `master` counts on from them). `master` is tagged `v1.1.0`/`v2.0.0` when it starts a new game.
5. **The wiki follows the live game:** `HelpWiki` writes whatever `help.json` says, so generate it from the release
   branch until the next game starts, not from `master`.
6. **Working habits:** CLAUDE.md's "commit and push finished work to `master` without asking" becomes: classify each
   change (or ask), commit and push safe changes to the release branch and merge them into `master`, and commit
   reset-needing work to `master` only. Merge after every fix, not in batches, so conflicts stay small as the two
   drift apart.

## When the branch is cut

- Write the `v1` saved-format fixture (CLAUDE.md, "Saved formats are pinned by a fixture").
- Change the commit rule in CLAUDE.md (point 6) and say which branch the wiki is generated from (point 5).
- Settle the tag rule (point 3) and, if chosen, the pre-release change to `release-client.yml`.
