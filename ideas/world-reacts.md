# The world reacts: rivals' statements on a First

**Status: built 2026-10-22; this first plan is superseded where it differs** (`ui-spec.md` §4.18.1, component
28, G17, and `design.md` §5). The main differences: the article is a story chosen by the statements' mood with
linking sentences and public-fact details, not a grid of boxes; at most 4 statements are told in full, not 9;
the messages are `SubmitStatementRequest`/`Response`; the page has four columns and a continuation sheet.


## Context
The user's idea (2026-10-20). When an agency wins a First and its front page is printed, the other players can respond with a press statement. At the next resolve a follow-up page, "The World Reacts to …", prints those statements. The feature is flavour only: no money, prestige or other effect on the game.

The user decided:
- **The statement:** one of five stances plus an optional short quote, printed word for word. There's no LLM; one could be added later as an extra layer.
- **Who reacts:** rivals only; the agencies that won the First don't.
- **The five stances:** enthusiastic congratulations, polite congratulations, "we'll be next", downplay, no comment.

The front pages are built already: `rules.FrontPagePrinter`, `model.FrontPage`, `server.schedule.FrontPageStore` writing `data/frontpages.json`, `client.screen.FrontPageScreen`, and `assets/content/front-pages.json` with `FrontPagesTest`.

## How we work: spec first, then mockups, then client
1. Write the spec. Build the core, server and content parts.
2. The user mocks up the statement dialog (a new component, in Claude Design) and the follow-up page's lower half (in Photocraft, as the original page was).
3. Build the client from those mockups.

## 1. Data model (core)
- **`model.FrontPage` gains three fields:**
  - `Kind kind`: FIRST or REACTIONS; null reads as FIRST.
  - `String reactsTo`: the id of the First's page.
  - `List<Reaction> reactions`.
- **Compatibility:**
  - A delegating 8-argument constructor keeps the existing calls compiling.
  - Old `frontpages.json` files still load, because a missing record field reads as null. So no reset is needed.
  - `tells(goal)` returns false for REACTIONS pages, so `FrontPageScreen.pageFor` and `FrontPageView.race` keep reading First pages only.
- **The follow-up page:**
  - Its id is `<turn>-<goalId>-reactions`.
  - Its goal is the First's own goal, so the photo and the unseen filter keep working.
  - Its achievers and folded goals are copied from the First's page.
- **`FrontPage.Reaction(agencyName, directorName, Stance, quote, lineId)`:**
  - `Stance` is `ENTHUSIASTIC, POLITE, NEXT, DOWNPLAY, NO_COMMENT, DECLINED`. The names are saved, so never rename or reuse one.
  - `DECLINED` is used by the server only, for rivals who stayed silent.
  - The same type holds `MAX_QUOTE_LENGTH = 140` and `ALLOWED_CHARACTERS`, a subset of `Theme.CHARACTERS` with no `{}<>`. They live here because the server must not depend on the libGDX theme.
- **Submissions are saved in `data/reactions/turn-N.json`,** where N is the open turn they were sent in.
  - The shape is `Map<rivalKey, Map<pageId, Draft(stance, quote)>>`. It is keyed by `GameSeeds.rivalKey`, never account ids.
  - The file is written atomically, like `intelligence/` and `market/`.
- **Kryo:** register the new types last in `net/MessageRegistry.java`.

## 2. Rules
- **Who may react:**
  - The page must be a FIRST page printed at the end of the previous turn (`page.gameTurn == open - 1`).
  - The player must not be one of its achievers. In a tie, every achiever is excluded.
  - The player must have a turn report for that turn, so late joiners are out (unconfirmed).
- **One statement per page.** If several Firsts are printed in one turn, each page takes its own statement. A player may change or withdraw theirs until the turn resolves; the last one sent counts.
- **Silent rivals print as "declined to comment."** If nobody reacted at all, no follow-up page is printed (unconfirmed).
- **`rules.ReactionPrinter`** (core) prints the follow-up pages.
  - It is deterministic, seeded `gameSeed ^ REACTIONS_SALT` mixed with the page id's hash, as `FrontPagePrinter.random` is.
  - It draws each template line without repeats within a stance.
  - Order: by stance, quoted before unquoted, then by agency name; DECLINED last.
  - **More than 9 reactions:** the extra ones fold into one "Also:" line. DECLINED rivals always go into one line.

## 3. Server
- **Messages:**
  - `SubmitReactionRequest(gameTurn, pageId, stance, quote)` and `SubmitReactionResponse(success, message, ReactionDraft yours)`.
  - `FrontPagesResponse` gains `List<ReactionDraft> yours`: only the requester's own drafts for the open turn.
- **`RequestDispatcher.handleSubmitReaction`** follows the pattern of `handleSubmitAction` (lines 273-301).
  - Its checks: authenticated, has a slot, the turn is current (else `TURN_RESOLVED`), `UNKNOWN_PAGE`, `REACTION_WINDOW_CLOSED`, `NOT_A_RIVAL`, `INVALID_STANCE` (no DECLINED), `QUOTE_TOO_LONG`, `QUOTE_BAD_CHARACTERS`.
  - The quote is stripped and its whitespace collapsed; control characters and newlines are refused. A NO_COMMENT statement stores an empty quote.
  - New refusal codes go in `client.ui.Refusals`.
- **`server.schedule.ReactionStore`:** `load`, `put`, `remove` and `dropAfter`.
- **`TurnScheduler.printFrontPages(N+1)`:**
  - It loads turn N's First pages and `reactions/turn-(N+1).json`, and maps rival keys to names the same way `resolveAllPlayers` does.
  - It calls `FrontPageStore.keep(N+1, followUps ++ firsts)` **once**, because `keep` replaces all of that turn's pages. Follow-ups come before the new Firsts (unconfirmed).
  - A retry re-reads the same file, so it prints the same pages.
- **`TurnRollback`:** keep `reactions/turn-T.json` and delete the files after T. `--redo` then reprints the same follow-up pages. Update its Javadoc.

## 4. Content
- **New file `assets/content/front-page-reactions.json`,** loaded by `content.FrontPageReactions` (mirrors `FrontPages`). It sits outside the pack and is not hashed.
  - `headlines`: generic ones, plus 2 for each of the five Firsts.
  - Photo captions, `lead` paragraphs, and `close` paragraphs the page may drop to fit.
  - Lines per stance: `quoted` (at least 3, each with `{quote}` exactly once) and `plain` (at least 3). NO_COMMENT has plain lines only.
  - `declined` lines, `one` and `many`.
  - Tokens: `{agency}` `{director}` `{first}` `{season}` `{rival}` `{rivalDirector}` `{quote}`.
- **Quotes are put in after the tokens are filled,** inside curly quotes, so a player's text is never read as a token.
- **`FrontPageReactionsTest`:**
  - ids are unique, all five Firsts are covered, tokens are known, and the `{quote}` rules hold;
  - every character is in `Theme.CHARACTERS`, and `ALLOWED_CHARACTERS` is a subset of it;
  - fit: with 32-character names and a 140-character quote, every line fits its box, and the page fits at least 9 boxes.
- **Write every line from the facts, in our own words.** Add the history to `design.md` §1.1, each figure checked: Eisenhower's answer to Sputnik, Kennedy's after Gagarin, and the Soviet message after Apollo 11.

## 5. Client (after the mockups)
- **The statement dialog (new component 28):**
  - It opens from a "Give a statement" button on the strip of a First's page, while the window is open and the player is eligible. Once a statement is given, the button reads "Your statement: Polite · Change".
  - The page can be reached later through the existing `openFrontPage(goalId)` paths: the Museum and the turn report.
  - Five stance rows, in the choice pattern of `PersonnelDialogs`.
  - A quote field: max length 140, a filter from `ALLOWED_CHARACTERS`, an "n / 140" counter and an inline error. It is disabled for No comment.
  - A sample line: "It might read: …".
  - Buttons: Submit, Withdraw, Cancel.
- **`ServerGateway.submitReaction`** is added as a default no-op method. `NetworkGateway` sends it; `ClientSession` keeps `yourReactions`.
- **The follow-up page:** `FrontPageScreen` branches on `kind`, with its words from `FrontPageView.ofReactions`.
  - It keeps the masthead, folio and headline logic. The ear reads "The world reacts".
  - Its lower half: the First's photo, smaller, with the lead beside it, then a grid of up to 3×3 statement boxes and the declined line under them.
  - The strip says "Statements only: no effect on the race."
  - A reopened First's page gets a "Read the reactions" button.

## 6. Docs (same session as the code)
- **`ui-spec.md`:** a new subsection of §4.18, "The world reacts"; component 28; a new G16 for the server work, referenced from G14; the refusal codes in §5.
- **`design.md`:** an entry beside "Front pages for the Firsts", plus the §1.1 history notes.
- **`CLAUDE.md`:**
  - A status line: new message pair, FrontPage gained fields, rebuild client and server together, no reset.
  - Add `front-page-reactions.json` and its test to the list of files outside the hashed pack.
- **Security note:** quotes are free text shown to every player, with only the character set checked. That's fine among friends; revisit it if games open up.

## Critical files
- `core/.../model/FrontPage.java`, `core/.../rules/ReactionPrinter.java` (new), `core/.../content/FrontPageReactions.java` (new)
- `core/.../net/MessageRegistry.java`, `net/messages/FrontPagesResponse.java`, `SubmitReactionRequest/Response.java` (new)
- `server/.../RequestDispatcher.java`, `server/.../schedule/TurnScheduler.java`, `ReactionStore.java` (new), `TurnRollback.java`
- `core/.../client/screen/FrontPageScreen.java`, `client/view/FrontPageView.java`, `client/ui/Refusals.java`
- `lwjgl3/.../ScreenSnapshot.java`, `dev-tools/.../DevClient.java`

## Verification
- **Tests:**
  - `ReactionPrinterTest`: determinism, ordering, DECLINED, achievers and late joiners excluded, the overflow fold.
  - `FrontPageReactionsTest`.
  - `FrontPageStoreTest`: an old file loads.
  - `ReactionStoreTest`.
  - Dispatcher tests for each refusal, changing a statement, withdrawing one, and `yours`.
  - Scheduler tests: printed once, a retry prints the same pages.
  - Rollback test: `--redo` gives an identical `frontpages.json`.
  - `mvn clean package`.
- **End to end:**
  1. Set `devMode`, start the server and claim three accounts.
  2. Put a First page from the last turn on file, by flying one or writing it by hand.
  3. Add a `react` command to `DevClient`. As B, submit a statement and try each refusal.
  4. Check `reactions/turn-N.json`: keyed by rival key, no names.
  5. Press "Resolve turn now". The follow-up page should show B quoted and C declined.
  6. Log in as each player in the client.
  7. Roll back with `--redo` and diff `frontpages.json`.
- **Shots:** a `-Dshots=frontpage` group with the strip button, the dialog's states, and the follow-up page with 3, 9 at the longest names, overflow, and all declined. Read every PNG.
