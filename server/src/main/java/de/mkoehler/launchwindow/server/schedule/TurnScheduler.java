package de.mkoehler.launchwindow.server.schedule;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.content.RandomEventRoster;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.persistence.ActionContext;
import de.mkoehler.launchwindow.persistence.AtomicFiles;
import de.mkoehler.launchwindow.persistence.PlayerSnapshotStore;
import de.mkoehler.launchwindow.persistence.PlayerStateService;
import de.mkoehler.launchwindow.rules.GoalTally;
import de.mkoehler.launchwindow.rules.WinCondition;
import de.mkoehler.launchwindow.server.GameSeeds;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
 * since {@link PlayerStateService#resolveTurn} is itself idempotent.
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
    private final ActionContext context;
    private final RandomEventRoster roster;

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
     * @param context  the game's content pack and budget schedule
     * @param roster   the random-event content
     */
    public TurnScheduler(Path dataRoot, ObjectMapper mapper, Clock clock, ActionContext context,
            RandomEventRoster roster) {
        this.context = Objects.requireNonNull(context, "context must not be null");
        this.roster = Objects.requireNonNull(roster, "roster must not be null");
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
        if (!ResolutionSchedule.isDue(meta, state.getLastResolvedDate(), now)) {
            return;
        }

        resolveAllPlayers(state.getNextGameTurn(), meta);

        state.setLastResolvedDate(today);
        state.setNextGameTurn(state.getNextGameTurn() + 1);
        stateStore.save(state);
    }

    /**
     * Resolves the open turn right away, for play testing in dev mode: the same pass as a
     * scheduled resolution, but it leaves the schedule alone, so today's scheduled turn still
     * resolves at its time.
     *
     * @return the game turn that was resolved
     * @throws IOException if {@code game-meta.json}, the scheduler state, or any player's own data
     *                     cannot be read or written
     */
    public long resolveNow() throws IOException {
        GameMeta meta = GameMetaStore.loadOrCreateDefault(gameMetaFile, mapper).meta();
        ZonedDateTime now = ZonedDateTime.now(clock).withZoneSameInstant(meta.getSchedule().getTimezone());
        SchedulerState state = loadOrBootstrapState(now, meta);
        long turn = state.getNextGameTurn();
        resolveAllPlayers(turn, meta);
        state.setNextGameTurn(turn + 1);
        stateStore.save(state);
        return turn;
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
        SchedulerState fresh = new SchedulerState();
        fresh.setNextGameTurn(1);
        fresh.setLastResolvedDate(ResolutionSchedule.bootstrapLastResolvedDate(meta, now));
        stateStore.save(fresh);
        return fresh;
    }

    /**
     * Resolves one game turn for every claimed player, then checks for a winner.
     *
     * <p>The goal tally is built from every player's state at the <em>start</em> of
     * the turn, so a retry after a crash halfway through prices same-turn goals the
     * same way as the first attempt; already-resolved players are skipped by
     * {@link PlayerStateService#resolveTurn}'s own idempotence. Each player's season
     * pass is seeded from the game seed, the turn, and the player, so it always comes
     * out the same. The win check runs once after everyone has resolved, and the
     * first decided result is recorded and never overwritten
     * ({@code design.md} &sect;5, M4 decision 4).</p>
     *
     * @param gameTurn the turn to resolve
     * @param meta     the game's configuration
     * @throws IOException if a player's files can't be read or written
     */
    private void resolveAllPlayers(long gameTurn, GameMeta meta) throws IOException {
        Path playersDir = dataRoot.resolve("players");
        if (!Files.exists(playersDir)) {
            return;
        }
        Map<String, PlayerStateService> services = new LinkedHashMap<>();
        List<PlayerSnapshot> startOfTurn = new ArrayList<>();
        try (Stream<Path> listing = Files.list(playersDir)) {
            for (Path playerDir : listing.filter(Files::isDirectory).sorted().toList()) {
                if (new PlayerSnapshotStore(playerDir, mapper).latestTurnNumber().isEmpty()) {
                    continue;
                }
                PlayerStateService service = new PlayerStateService(playerDir, mapper, context);
                services.put(playerDir.getFileName().toString(), service);
                service.stateBefore(gameTurn).ifPresent(startOfTurn::add);
            }
        }
        GoalTally tally = GoalTally.fromPlayers(startOfTurn);
        for (Map.Entry<String, PlayerStateService> player : services.entrySet()) {
            player.getValue().resolveTurn(gameTurn, tally, roster,
                GameSeeds.forSeason(meta.getGameSeed(), gameTurn, player.getKey()));
        }
        recordResultIfDecided(gameTurn, meta, services);
    }

    private void recordResultIfDecided(long gameTurn, GameMeta meta, Map<String, PlayerStateService> services)
            throws IOException {
        Path resultFile = dataRoot.resolve(GameResult.FILE_NAME);
        if (Files.exists(resultFile)) {
            return;
        }
        Map<String, PlayerSnapshot> players = new LinkedHashMap<>();
        for (Map.Entry<String, PlayerStateService> player : services.entrySet()) {
            players.put(player.getKey(), player.getValue().stateBefore(gameTurn + 1).orElseThrow());
        }
        WinCondition.Outcome outcome = WinCondition.evaluate(players, meta.getDeclaredGoalIds(), context.catalog());
        if (outcome.status() == WinCondition.Status.NO_WINNER_YET) {
            return;
        }
        List<String> agencies = outcome.winners().stream().map(id -> players.get(id).getAgencyName()).toList();
        GameResult result = new GameResult(outcome.status().name(), outcome.winners(), agencies, gameTurn);
        Path temp = Files.createTempFile(dataRoot, "game-result-", ".json.tmp");
        mapper.writerWithDefaultPrettyPrinter().writeValue(temp.toFile(), result);
        AtomicFiles.moveReplacingWithRetry(temp, resultFile);
    }
}