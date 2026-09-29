package de.mkoehler.launchwindow.server.schedule;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.persistence.PlayerSnapshotStore;
import de.mkoehler.launchwindow.persistence.PlayerStateService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * <p>Decides, on every call to {@link #checkAndResolveIfDue()}, whether
 * today's turn should resolve for this server's one game (see
 * {@code design.md} &sect;3.6, &sect;3.9), and if so resolves it for every
 * player under this server's data root.</p>
 *
 * <p>Re-reads {@code game-meta.json} on every call rather than caching it at
 * construction time, so a blackout the host adds mid-day - before the
 * configured resolution time - still suppresses that day's resolution, as
 * &sect;3.6 requires.</p>
 *
 * <p>Only ever resolves the single next calendar day it is behind by, never
 * a backlog of several missed days at once: if the server was down across
 * more than one scheduled resolution day, the days in between are skipped
 * rather than caught up on the next boot.</p>
 *
 * <p>Intended to be called from the same thread that drains
 * {@code NetworkServer.poll()} - e.g. inside {@code GameServer.render()} -
 * so a resolution never runs concurrently with {@code RequestDispatcher}
 * handling a request for the same player (see that slice's threading
 * constraint note in {@code design.md} &sect;5).</p>
 *
 * <p>If resolving any one player throws, the exception propagates and this
 * call's scheduler state is not advanced, so the next call retries the same
 * game turn for every player - already-resolved players are unaffected,
 * since {@link PlayerStateService#resolveTurn(long)} is itself idempotent.
 * A player whose resolution keeps failing therefore blocks every other
 * player's turn from ever advancing past it, for as long as it keeps
 * failing; this is accepted for now and would need addressing before this
 * scales past a small group of trusted players.</p>
 *
 * @author Mario Koehler
 */
public class TurnScheduler {

    private final Path dataRoot;
    private final Path gameMetaFile;
    private final ObjectMapper mapper;
    private final Clock clock;
    private final SchedulerStateStore stateStore;

    /**
     * Creates a new scheduler for the game rooted at the given data
     * directory.
     *
     * @param dataRoot the server's data root - {@code game-meta.json},
     *                 {@code scheduler-state.json} and every player's own
     *                 directory live directly under it
     * @param mapper   the Jackson mapper used to read/write every file this
     *                 class touches
     * @param clock    the clock used for "now"
     */
    public TurnScheduler(Path dataRoot, ObjectMapper mapper, Clock clock) {
        this.dataRoot = Objects.requireNonNull(dataRoot, "dataRoot must not be null");
        this.gameMetaFile = dataRoot.resolve("game-meta.json");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.stateStore = new SchedulerStateStore(dataRoot.resolve("scheduler-state.json"), mapper);
    }

    /**
     * Checks whether today's turn is due, and resolves it for every player
     * if so. A no-op if today's turn was already resolved, if today is not
     * one of the schedule's active weekdays, if today falls in a blackout,
     * or if the configured resolution time has not yet passed today.
     *
     * <p>On the very first call ever for this data root, the scheduler
     * never treats a day already past its resolution time as due just
     * because the server happens to be booting late in the day: if "now" is
     * already past today's resolution time, today is bootstrapped as
     * already handled, and the first real resolution happens on the next
     * active, non-blacked-out day after that. If "now" is still before
     * today's resolution time, today itself remains eligible, so a server
     * started fresh earlier in the day can still resolve its first turn
     * later that same day.</p>
     *
     * @throws IOException if {@code game-meta.json}, the scheduler state,
     *                      or any player's own data cannot be read or
     *                      written
     */
    public void checkAndResolveIfDue() throws IOException {
        GameMetaStore.Result metaResult = GameMetaStore.loadOrCreateDefault(gameMetaFile, mapper);
        GameMeta meta = metaResult.meta();
        ZonedDateTime now = ZonedDateTime.now(clock).withZoneSameInstant(meta.getSchedule().getTimezone());
        LocalDate today = now.toLocalDate();

        SchedulerState state = loadOrBootstrapState(now, meta);
        if (today.equals(state.getLastResolvedDate())) {
            return;
        }
        if (!meta.resolvesOn(today)) {
            return;
        }
        if (now.toLocalTime().isBefore(meta.getSchedule().getResolutionTime())) {
            return;
        }

        resolveAllPlayers(state.getNextGameTurn());

        state.setLastResolvedDate(today);
        state.setNextGameTurn(state.getNextGameTurn() + 1);
        stateStore.save(state);
    }

    /**
     * Loads the persisted scheduler state, or bootstraps and saves a fresh
     * one on the very first call: {@code nextGameTurn} 1, and
     * {@code lastResolvedDate} set to yesterday if today's resolution time
     * has not passed yet (today stays eligible later that same day),
     * or to today if it already has (today is treated as already handled,
     * so a late-in-the-day first boot never fires immediately).
     *
     * @param now  the current moment, in the schedule's own timezone
     * @param meta the game meta, for its schedule's resolution time
     * @return the loaded or newly bootstrapped state
     * @throws IOException if the state file exists but cannot be read, or
     *                      a freshly bootstrapped one cannot be written
     */
    private SchedulerState loadOrBootstrapState(ZonedDateTime now, GameMeta meta) throws IOException {
        Optional<SchedulerState> existing = stateStore.load();
        if (existing.isPresent()) {
            return existing.get();
        }
        LocalDate today = now.toLocalDate();
        boolean beforeResolutionTime = now.toLocalTime().isBefore(meta.getSchedule().getResolutionTime());
        SchedulerState fresh = new SchedulerState();
        fresh.setNextGameTurn(1);
        fresh.setLastResolvedDate(beforeResolutionTime ? today.minusDays(1) : today);
        stateStore.save(fresh);
        return fresh;
    }

    /**
     * Resolves the given game turn for every player directory under
     * {@code dataRoot/players}, skipping any directory with no saved
     * snapshot at all - such a directory has never completed its bootstrap
     * handshake (or a crash landed between creating its {@code snapshots}
     * directory and writing turn 0's file into it) and
     * {@link PlayerStateService#resolveTurn(long)} would otherwise throw
     * {@link IllegalStateException} for it.
     *
     * @param gameTurn the game turn to resolve
     * @throws IOException if any player's own data cannot be read or
     *                      written
     */
    private void resolveAllPlayers(long gameTurn) throws IOException {
        Path playersDir = dataRoot.resolve("players");
        if (!Files.exists(playersDir)) {
            return;
        }
        try (Stream<Path> listing = Files.list(playersDir)) {
            List<Path> playerDirs = listing.filter(Files::isDirectory).toList();
            for (Path playerDir : playerDirs) {
                if (new PlayerSnapshotStore(playerDir, mapper).latestTurnNumber().isEmpty()) {
                    continue;
                }
                new PlayerStateService(playerDir, mapper).resolveTurn(gameTurn);
            }
        }
    }
}
