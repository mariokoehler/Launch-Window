package de.mkoehler.launchwindow.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.content.SpaceRaceNews;
import de.mkoehler.launchwindow.model.GameDate;
import de.mkoehler.launchwindow.content.RandomEventRoster;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.rules.GoalTally;
import de.mkoehler.launchwindow.rules.IntelFinding;
import de.mkoehler.launchwindow.rules.ResolutionEvent;
import de.mkoehler.launchwindow.rules.TurnReport;
import de.mkoehler.launchwindow.rules.SeasonResolver;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.SplittableRandom;

/**
 * <p>One player's state on disk: the latest resolved snapshot plus the pending
 * action journal, combined by replay ({@code design.md} &sect;3.7).</p>
 *
 * <ul>
 *   <li>{@link #currentState()} is the preview the player sees: the last resolved
 *       snapshot with today's actions replayed on top.</li>
 *   <li>{@link #submitAction} validates an action against that preview and journals
 *       it only if it takes effect, so the journal never holds an action that
 *       failed when it was submitted.</li>
 *   <li>{@link #resolveTurn} replays the journal and runs the season pass
 *       ({@link SeasonResolver}), writes the next snapshot, and archives the
 *       journal. It is idempotent and crash-safe: a turn already written is never
 *       redone, only its archiving finished.</li>
 * </ul>
 *
 * @author Mario Koehler
 */
public class PlayerStateService {

    private final PlayerSnapshotStore snapshotStore;
    private final TurnReportStore reportStore;
    private final PlayerActionJournal journal;
    private final SnapshotReplay replay;
    private final ActionContext context;

    /**
     * Creates a service for one player's folder.
     *
     * @param playerRoot the player's folder
     * @param mapper     the persistence mapper
     * @param context    the game's content and budget schedule
     */
    public PlayerStateService(Path playerRoot, ObjectMapper mapper, ActionContext context) {
        Objects.requireNonNull(playerRoot, "playerRoot must not be null");
        Objects.requireNonNull(mapper, "mapper must not be null");
        this.context = Objects.requireNonNull(context, "context must not be null");
        this.snapshotStore = new PlayerSnapshotStore(playerRoot, mapper);
        this.reportStore = new TurnReportStore(playerRoot, mapper);
        this.journal = new PlayerActionJournal(playerRoot, mapper);
        this.replay = new SnapshotReplay(mapper);
    }

    /**
     * Returns the player's preview: the last resolved snapshot with today's actions applied.
     *
     * @return the current state
     * @throws IOException if the player's files can't be read
     */
    public PlayerSnapshot currentState() throws IOException {
        return replayPending().state();
    }

    /**
     * Validates an action against the current preview and, if it takes effect,
     * journals it. A rejected action is not journaled.
     *
     * @param action the action, its seed already set by the server
     * @return what the action did against the preview
     * @throws IOException if the player's files can't be read or written
     */
    public ActionResult submitAction(PlayerAction action) throws IOException {
        Objects.requireNonNull(action, "action must not be null");
        PlayerSnapshot preview = currentState();
        ActionResult result = SnapshotReplay.applySafely(action, replay.copy(preview), context);
        if (result.applied()) {
            journal.append(action);
        }
        return result;
    }

    /**
     * Returns the player's state at the start of a turn: the latest snapshot resolved
     * before it, even if a crashed attempt already wrote the turn.
     *
     * @param gameTurn the turn being resolved
     * @return the start-of-turn snapshot, if the player had one
     * @throws IOException if the snapshots can't be read
     */
    public Optional<PlayerSnapshot> stateBefore(long gameTurn) throws IOException {
        return snapshotStore.loadLatestBefore(gameTurn);
    }

    /**
     * Resolves one turn for this player: replays today's actions, runs the season
     * pass, advances the player's own season count, saves the next snapshot, and
     * archives the journal. Does nothing but finish the archiving if the turn is
     * already written.
     *
     * @param gameTurn   the shared game turn
     * @param goalTally  the game-wide goal counts at the start of the turn; read only
     * @param roster     the random-event content
     * @param seasonSeed the seed for this player's season pass
     * @return the season's events, or an empty list if the turn was already resolved
     * @throws IOException if the player's files can't be read or written
     */
    public List<ResolutionEvent> resolveTurn(long gameTurn, GoalTally goalTally, RandomEventRoster roster,
            long seasonSeed) throws IOException {
        return resolveTurn(gameTurn, goalTally, roster, seasonSeed, List.of());
    }

    /**
     * As {@link #resolveTurn(long, GoalTally, RandomEventRoster, long)}, applying this player's share of
     * the turn's intelligence findings ({@code design.md} &sect;2.9, settled once for every player).
     *
     * @param gameTurn     the turn to resolve
     * @param goalTally    the game-wide goal counts at the start of the turn
     * @param roster       the random events
     * @param seasonSeed   this player's seed for the season
     * @param intelligence this player's intelligence findings
     * @return the season's events, or none if the turn was already resolved
     * @throws IOException if the player's files can't be read or written
     */
    public List<ResolutionEvent> resolveTurn(long gameTurn, GoalTally goalTally, RandomEventRoster roster,
            long seasonSeed, List<IntelFinding> intelligence) throws IOException {
        OptionalLong latestTurn = snapshotStore.latestTurnNumber();
        if (latestTurn.isPresent() && latestTurn.getAsLong() >= gameTurn) {
            finishArchivingUpToCutoff(gameTurn);
            return List.of();
        }
        PlayerSnapshot base = loadLatestSnapshot();
        List<PlayerActionJournal.PendingAction> pending = journal.readPending();
        PlayerSnapshot resolved = replay.replay(base,
            pending.stream().map(PlayerActionJournal.PendingAction::action).toList(), context).state();
        SeasonResolver.Context season = new SeasonResolver.Context(context.catalog(), context.schedule(), roster,
            goalTally, Math.toIntExact(gameTurn), resolved.getSeason(), intelligence);
        // The report's "before" is today's state, actions applied, not the stored previous snapshot.
        PlayerSnapshot before = replay.copy(resolved);
        List<ResolutionEvent> events = SeasonResolver.advanceSeason(resolved, season, new SplittableRandom(seasonSeed));
        resolved.setSeason(resolved.getSeason() + 1);
        String cutoff = pending.isEmpty() ? null : lastFileName(pending);
        // The report goes first: a crash before the snapshot redoes the turn, which writes the same report.
        // One real space-race story from the season this turn played out in, picked from the season seed
        // so a redone turn picks the same one (design.md section 1.1).
        String story = SpaceRaceNews.standard().pick(GameDate.ofTurn(gameTurn), seasonSeed);
        reportStore.save(TurnReport.build(before, resolved, events, gameTurn, context.catalog()).withNewsStory(story));
        snapshotStore.save(gameTurn, resolved, cutoff);
        journal.archive(pending, gameTurn);
        return events;
    }

    /**
     * Rolls this player back to the start of a turn, undoing its resolution ({@code design.md}
     * &sect;3.7): sets aside the actions queued since it resolved, puts the turn's own actions
     * back in the journal, and deletes its report and snapshot, so the next resolution of the
     * turn starts from the same inputs. Run with the server stopped. Safe to retry: the actions
     * queued later are told apart by the turn's cutoff, not by where they sit.
     *
     * @param gameTurn the turn to undo; it must be this player's latest
     * @return whether this player had the turn resolved; {@code false} for a player who joined later
     * @throws IOException           if the player's files can't be read or moved
     * @throws IllegalStateException if the player has a later turn, which must be rolled back first
     */
    public boolean rollBack(long gameTurn) throws IOException {
        OptionalLong latest = snapshotStore.latestTurnNumber();
        if (latest.isEmpty() || latest.getAsLong() < gameTurn || gameTurn < 1) {
            return false;
        }
        if (latest.getAsLong() > gameTurn) {
            throw new IllegalStateException("turn " + latest.getAsLong() + " must be rolled back before turn "
                + gameTurn);
        }
        Optional<String> cutoff = snapshotStore.lastAppliedActionFile(gameTurn);
        journal.setAside(journal.readPending().stream()
            .filter(pending -> cutoff.isEmpty() || pending.file().getFileName().toString().compareTo(cutoff.get()) > 0)
            .toList(), gameTurn);
        journal.unarchive(gameTurn);
        reportStore.delete(gameTurn);
        snapshotStore.delete(gameTurn);
        return true;
    }

    /**
     * Loads a turn's report.
     *
     * @param turn the turn, or a negative number for the latest
     * @return the report, or empty if there's none
     * @throws IOException if the report can't be read
     */
    public Optional<TurnReport> turnReport(long turn) throws IOException {
        if (turn >= 0) {
            return reportStore.load(turn);
        }
        OptionalLong latest = reportStore.latestTurn();
        return latest.isPresent() ? reportStore.load(latest.getAsLong()) : Optional.empty();
    }

    private SnapshotReplay.Replayed replayPending() throws IOException {
        PlayerSnapshot base = loadLatestSnapshot();
        List<PlayerActionJournal.PendingAction> pending = journal.readPending();
        return replay.replay(base, pending.stream().map(PlayerActionJournal.PendingAction::action).toList(), context);
    }

    private void finishArchivingUpToCutoff(long gameTurn) throws IOException {
        Optional<String> cutoff = snapshotStore.lastAppliedActionFile(gameTurn);
        if (cutoff.isEmpty()) {
            return;
        }
        List<PlayerActionJournal.PendingAction> toArchive = journal.readPending().stream()
            .filter(pending -> pending.file().getFileName().toString().compareTo(cutoff.get()) <= 0)
            .toList();
        journal.archive(toArchive, gameTurn);
    }

    private String lastFileName(List<PlayerActionJournal.PendingAction> pending) {
        return pending.get(pending.size() - 1).file().getFileName().toString();
    }

    private PlayerSnapshot loadLatestSnapshot() throws IOException {
        return snapshotStore.load()
            .orElseThrow(() -> new IllegalStateException("no snapshot exists for this player yet"));
    }
}
