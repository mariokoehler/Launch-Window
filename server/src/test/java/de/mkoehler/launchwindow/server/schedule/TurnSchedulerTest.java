package de.mkoehler.launchwindow.server.schedule;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.actions.QueueIntelOperationAction;
import de.mkoehler.launchwindow.content.RandomEventRoster;
import de.mkoehler.launchwindow.model.IntelArea;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.persistence.PlayerSnapshotStore;
import de.mkoehler.launchwindow.persistence.PlayerStateService;
import de.mkoehler.launchwindow.rules.IntelligenceFound;
import de.mkoehler.launchwindow.server.GameSeeds;
import de.mkoehler.launchwindow.persistence.ResolvedSnapshot;
import de.mkoehler.launchwindow.server.GameServer;
import de.mkoehler.launchwindow.server.ServerTestGames;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>Tests for {@link TurnScheduler}, using a fixed or manually-advanced
 * {@link Clock} throughout so every scenario is deterministic and needs no
 * real waiting.</p>
 *
 * @author Mario Koehler
 */
class TurnSchedulerTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Berlin");

    private final ObjectMapper mapper = GameServer.createMapper();

    /**
     * A {@link Clock} whose instant can be changed after construction, so a
     * single {@link TurnScheduler} instance can be driven across several
     * simulated moments - proving behavior that depends on re-reading state
     * on every call, rather than something a fresh instance per call would
     * mask.
     */
    private static final class MutableClock extends Clock {
        private Instant instant;
        private final ZoneId zone;

        MutableClock(Instant instant, ZoneId zone) {
            this.instant = instant;
            this.zone = zone;
        }

        void set(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }

    /**
     * Creates a player directory with a bootstrap turn-0 snapshot, matching
     * what {@code RequestDispatcher} does for a real handshake.
     *
     * @param dataRoot   the server data root
     * @param accountId  the player's account id, used as its directory name
     * @return the player's own data directory
     * @throws IOException if the bootstrap snapshot cannot be written
     */
    private Path createPlayer(Path dataRoot, String accountId) throws IOException {
        Path playerRoot = dataRoot.resolve("players").resolve(accountId);
        new PlayerSnapshotStore(playerRoot, mapper).save(0, new PlayerSnapshot("Agency", Archetype.NASA, 0, 0, 0, 1), null);
        return playerRoot;
    }

    /**
     * Creates a scheduler on the early-era pack with random events switched off.
     *
     * @param dataRoot the server data root
     * @param clock    the clock
     * @return the scheduler
     */
    private TurnScheduler scheduler(Path dataRoot, Clock clock) {
        earlyEraRace(dataRoot);
        return new TurnScheduler(dataRoot, mapper, clock, ServerTestGames.CONTEXT,
            new RandomEventRoster(0, 0, RandomEventRoster.baspmClose().events()));
    }

    /**
     * Verifies a turn's intelligence is settled once for every player, from the actions queued that
     * turn, kept on disk, and applied to the spying player's own state under the rival's opaque key
     * ({@code design.md} &sect;2.9).
     */
    @Test
    void intelligenceIsSettledOnceAcrossPlayersAndKept(@TempDir Path dataRoot) throws IOException {
        createPlayer(dataRoot, "acct-target");
        Path spyRoot = dataRoot.resolve("players").resolve("acct-spy");
        PlayerSnapshot spy = new PlayerSnapshot("Spy Bureau", Archetype.SOVIET, 10_000, 100, 0, 1);
        spy.getIntelligence().setLevel(2);
        spy.getIntelligence().setTargetLevel(2);
        new PlayerSnapshotStore(spyRoot, mapper).save(0, spy, null);
        scheduler(dataRoot, Clock.fixed(Instant.parse("2026-09-28T08:00:00Z"), ZONE)).checkAndResolveIfDue();
        long seed = GameMetaStore.loadOrCreateDefault(dataRoot.resolve("game-meta.json"), mapper).meta().getGameSeed();
        String target = GameSeeds.rivalKey(seed, "acct-target");
        PlayerStateService service = new PlayerStateService(spyRoot, mapper, ServerTestGames.CONTEXT);
        assertTrue(service.submitAction(QueueIntelOperationAction.assessment(target, IntelArea.RND)).applied());
        assertTrue(service.submitAction(QueueIntelOperationAction.assessment(target, IntelArea.PLANS)).applied());

        scheduler(dataRoot, Clock.fixed(Instant.parse("2026-09-29T08:00:00Z"), ZONE)).checkAndResolveIfDue();

        Path kept = dataRoot.resolve("intelligence").resolve("turn-1.json");
        assertTrue(Files.exists(kept));
        PlayerSnapshot resolved = new PlayerSnapshotStore(spyRoot, mapper).load(1).orElseThrow();
        assertTrue(resolved.getIntelligence().getFolders().containsKey(target));
        assertFalse(resolved.getIntelligence().getFolders().containsKey("acct-target"), "never the account id");
        assertEquals(2, service.turnReport(1).orElseThrow().events().stream()
            .filter(IntelligenceFound.class::isInstance).map(IntelligenceFound.class::cast)
            .filter(found -> found.finding().operationId() != null).count(),
            "both operations settled (a passive tidbit may come on top)");
        String before = Files.readString(kept);
        scheduler(dataRoot, Clock.fixed(Instant.parse("2026-09-30T08:00:00Z"), ZONE)).checkAndResolveIfDue();
        assertEquals(before, Files.readString(kept), "a turn's findings are written once");
    }

    /**
     * Verifies the season pass really runs at resolution - building upkeep is charged
     * and the candidate pools refresh - and that the first player to hold every
     * declared goal is recorded as the winner once, never overwritten.
     */
    @Test
    void resolutionRunsTheSeasonPassAndRecordsTheFirstWinnerOnce(@TempDir Path dataRoot) throws IOException {
        createPlayer(dataRoot, "acct-1");
        Path winnerRoot = dataRoot.resolve("players").resolve("acct-2");
        PlayerSnapshot winner = new PlayerSnapshot("Winner", Archetype.SOVIET, 10_000, 0, 0, 1);
        winner.setAchievedGoals(Map.of("earth-orbit-satellite", 0, "human-in-space", 0, "human-in-orbit", 0));
        new PlayerSnapshotStore(winnerRoot, mapper).save(0, winner, null);
        Clock bootstrap = Clock.fixed(Instant.parse("2026-09-28T08:00:00Z"), ZONE);
        scheduler(dataRoot, bootstrap).checkAndResolveIfDue();

        scheduler(dataRoot, Clock.fixed(Instant.parse("2026-09-29T08:00:00Z"), ZONE)).checkAndResolveIfDue();

        PlayerSnapshot resolved = new PlayerSnapshotStore(winnerRoot, mapper).load(1).orElseThrow();
        assertEquals(10_000 - 925, resolved.getFunds(), "level-1 building upkeep was charged");
        assertEquals(2, resolved.getSeason());
        assertFalse(resolved.getAstronautCandidates().isEmpty(), "the candidate pools were refreshed");
        GameResult result = mapper.readValue(dataRoot.resolve(GameResult.FILE_NAME).toFile(), GameResult.class);
        assertEquals("WON", result.status());
        assertEquals(List.of("acct-2"), result.winnerIds());
        assertEquals(List.of("Winner"), result.winnerAgencies());
        assertEquals(1, result.decidedOnTurn());

        scheduler(dataRoot, Clock.fixed(Instant.parse("2026-09-30T08:00:00Z"), ZONE)).checkAndResolveIfDue();
        assertEquals(1, mapper.readValue(dataRoot.resolve(GameResult.FILE_NAME).toFile(), GameResult.class)
            .decidedOnTurn(), "the recorded result is never overwritten");
        assertTrue(new PlayerSnapshotStore(winnerRoot, mapper).load(2).isPresent(), "play continues");
    }

    /**
     * Makes the game's {@code game-meta.json}, the default one if there is none yet, declare the
     * early era's race: the scheduler runs on the early-era pack, which lacks the default race's
     * lunar landing.
     *
     * @param dataRoot the server data root
     */
    private void earlyEraRace(Path dataRoot) {
        try {
            Path file = dataRoot.resolve("game-meta.json");
            GameMeta meta = GameMetaStore.loadOrCreateDefault(file, mapper).meta();
            if (!meta.getDeclaredGoalIds().equals(GameMeta.EARLY_ERA_DECLARED_GOALS)) {
                meta.setDeclaredGoalIds(GameMeta.EARLY_ERA_DECLARED_GOALS);
                writeGameMeta(dataRoot, meta);
            }
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    /**
     * Writes a custom {@code game-meta.json}, for scenarios that need a
     * specific schedule rather than the DAILY default.
     *
     * @param dataRoot the server data root
     * @param meta     the game meta to write
     * @throws IOException if the file cannot be written
     */
    private void writeGameMeta(Path dataRoot, GameMeta meta) throws IOException {
        mapper.writeValue(dataRoot.resolve("game-meta.json").toFile(), meta);
    }

    /**
     * Verifies that the very first call for a fresh data root, made after
     * today's resolution time has already passed, never resolves a turn
     * immediately even though today is an active weekday.
     */
    @Test
    void firstCallEverAfterTheResolutionTimeDoesNotResolveImmediately(@TempDir Path dataRoot) throws IOException {
        createPlayer(dataRoot, "acct-1");
        // 2026-09-29 is a Tuesday, 10:00 Europe/Berlin - past the DAILY default's 09:00.
        Clock clock = Clock.fixed(Instant.parse("2026-09-29T08:00:00Z"), ZONE);
        TurnScheduler scheduler = scheduler(dataRoot, clock);

        scheduler.checkAndResolveIfDue();

        assertTrue(new PlayerSnapshotStore(dataRoot.resolve("players/acct-1"), mapper).load(1).isEmpty());
    }

    /**
     * Verifies that the very first call for a fresh data root, made before
     * today's resolution time, still leaves today eligible: a second check
     * later the same day, once the resolution time has passed, resolves
     * normally rather than needing an extra day to "warm up".
     */
    @Test
    void firstCallEverBeforeTheResolutionTimeCanStillResolveLaterTheSameDay(@TempDir Path dataRoot) throws IOException {
        createPlayer(dataRoot, "acct-1");
        // 07:00 Europe/Berlin - before the DAILY default's 09:00.
        MutableClock clock = new MutableClock(Instant.parse("2026-09-29T05:00:00Z"), ZONE);
        TurnScheduler scheduler = scheduler(dataRoot, clock);

        scheduler.checkAndResolveIfDue();
        assertTrue(new PlayerSnapshotStore(dataRoot.resolve("players/acct-1"), mapper).load(1).isEmpty());

        // 10:00 Europe/Berlin, same day, now past the resolution time.
        clock.set(Instant.parse("2026-09-29T08:00:00Z"));
        scheduler.checkAndResolveIfDue();

        assertTrue(new PlayerSnapshotStore(dataRoot.resolve("players/acct-1"), mapper).load(1).isPresent());
    }

    /**
     * Verifies that a resolving day, once past the resolution time, resolves
     * exactly once - a second call the same day is a no-op.
     */
    @Test
    void resolvesOnceOnAResolvingDayAndNotTwice(@TempDir Path dataRoot) throws IOException {
        createPlayer(dataRoot, "acct-1");
        Clock beforeToday = Clock.fixed(Instant.parse("2026-09-28T08:00:00Z"), ZONE);
        scheduler(dataRoot, beforeToday).checkAndResolveIfDue();
        Clock afterResolutionTime = Clock.fixed(Instant.parse("2026-09-29T08:00:00Z"), ZONE);
        TurnScheduler scheduler = scheduler(dataRoot, afterResolutionTime);

        scheduler.checkAndResolveIfDue();
        scheduler.checkAndResolveIfDue();

        PlayerSnapshotStore store = new PlayerSnapshotStore(dataRoot.resolve("players/acct-1"), mapper);
        assertTrue(store.load(1).isPresent());
        assertTrue(store.load(2).isEmpty());
    }

    /**
     * Resolving now (dev mode) resolves the open turn at once and leaves the schedule alone:
     * the day's scheduled resolution still comes, and resolves the next turn.
     */
    @Test
    void resolveNowResolvesTheOpenTurnAndKeepsTheSchedule(@TempDir Path dataRoot) throws IOException {
        createPlayer(dataRoot, "acct-1");
        Clock beforeResolutionTime = Clock.fixed(Instant.parse("2026-09-29T06:00:00Z"), ZONE);

        List<Long> heard = new java.util.ArrayList<>();
        TurnScheduler forcing = scheduler(dataRoot, beforeResolutionTime);
        forcing.setOnResolved(heard::add);
        assertEquals(1, forcing.resolveNow());
        assertEquals(List.of(1L), heard, "a forced resolution is announced");
        PlayerSnapshotStore store = new PlayerSnapshotStore(dataRoot.resolve("players/acct-1"), mapper);
        assertTrue(store.load(1).isPresent());
        assertTrue(store.load(2).isEmpty());

        TurnScheduler scheduled = scheduler(dataRoot, Clock.fixed(Instant.parse("2026-09-29T08:00:00Z"), ZONE));
        scheduled.setOnResolved(heard::add);
        scheduled.checkAndResolveIfDue();
        assertTrue(store.load(2).isPresent(), "the scheduled resolution still comes, a turn later");
        assertEquals(List.of(1L, 2L), heard, "and so is a scheduled one");
        assertTrue(store.load(3).isEmpty());
    }

    /**
     * Verifies that a resolving day before its configured resolution time
     * does not resolve yet.
     */
    @Test
    void doesNotResolveBeforeTheConfiguredTime(@TempDir Path dataRoot) throws IOException {
        createPlayer(dataRoot, "acct-1");
        Clock bootstrap = Clock.fixed(Instant.parse("2026-09-28T08:00:00Z"), ZONE);
        scheduler(dataRoot, bootstrap).checkAndResolveIfDue();
        // 07:00 Europe/Berlin - before the DAILY default's 09:00.
        Clock beforeResolutionTime = Clock.fixed(Instant.parse("2026-09-29T05:00:00Z"), ZONE);

        scheduler(dataRoot, beforeResolutionTime).checkAndResolveIfDue();

        assertTrue(new PlayerSnapshotStore(dataRoot.resolve("players/acct-1"), mapper).load(1).isEmpty());
    }

    /**
     * Verifies that an inactive weekday under a WEEKDAYS schedule does not
     * resolve.
     */
    @Test
    void doesNotResolveOnAnInactiveWeekday(@TempDir Path dataRoot) throws IOException {
        createPlayer(dataRoot, "acct-1");
        writeGameMeta(dataRoot, new GameMeta(ScheduleProfile.weekdays(LocalTime.of(9, 0), ZONE), List.of()));
        // 2026-10-03 is a Saturday.
        Clock bootstrap = Clock.fixed(Instant.parse("2026-10-02T08:00:00Z"), ZONE);
        scheduler(dataRoot, bootstrap).checkAndResolveIfDue();
        Clock saturday = Clock.fixed(Instant.parse("2026-10-03T08:00:00Z"), ZONE);

        scheduler(dataRoot, saturday).checkAndResolveIfDue();

        assertTrue(new PlayerSnapshotStore(dataRoot.resolve("players/acct-1"), mapper).load(1).isEmpty());
    }

    /**
     * Verifies that a blackout added to {@code game-meta.json} after the
     * scheduler last checked, but before today's resolution time is
     * reached, still suppresses today - proving the file is re-read on
     * every {@link TurnScheduler#checkAndResolveIfDue()} call rather than
     * cached, by driving one single scheduler instance across all three
     * moments below with a {@link MutableClock}.
     */
    @Test
    void aBlackoutAddedMidDaySuppressesTodaysResolution(@TempDir Path dataRoot) throws IOException {
        createPlayer(dataRoot, "acct-1");
        MutableClock clock = new MutableClock(Instant.parse("2026-09-28T08:00:00Z"), ZONE);
        TurnScheduler scheduler = scheduler(dataRoot, clock);
        scheduler.checkAndResolveIfDue();

        // 07:00 Europe/Berlin the next day - before the resolution time.
        clock.set(Instant.parse("2026-09-29T05:00:00Z"));
        scheduler.checkAndResolveIfDue();

        // The host adds a same-day blackout before the resolution time is reached.
        GameMeta meta = mapper.readValue(dataRoot.resolve("game-meta.json").toFile(), GameMeta.class);
        meta.setBlackouts(List.of(new Blackout(LocalDate.of(2026, 9, 29), LocalDate.of(2026, 9, 29))));
        writeGameMeta(dataRoot, meta);

        // 10:00 Europe/Berlin, same day, now past the resolution time.
        clock.set(Instant.parse("2026-09-29T08:00:00Z"));
        scheduler.checkAndResolveIfDue();

        assertTrue(new PlayerSnapshotStore(dataRoot.resolve("players/acct-1"), mapper).load(1).isEmpty());
    }

    /**
     * Verifies that scheduler state survives a restart across two different
     * resolving days: a fresh {@link TurnScheduler} instance backed by the
     * same data root does not re-resolve a date already resolved by an
     * earlier instance, and picks up the next consecutive game turn number
     * on the following resolving day rather than reusing or skipping one.
     */
    @Test
    void schedulerStateSurvivesARestartAcrossDaysWithConsecutiveGameTurns(@TempDir Path dataRoot) throws IOException {
        createPlayer(dataRoot, "acct-1");
        Clock bootstrap = Clock.fixed(Instant.parse("2026-09-28T08:00:00Z"), ZONE);
        scheduler(dataRoot, bootstrap).checkAndResolveIfDue();
        Clock day1 = Clock.fixed(Instant.parse("2026-09-29T08:00:00Z"), ZONE);
        scheduler(dataRoot, day1).checkAndResolveIfDue();

        // A brand-new instance, as if the process restarted, checking again the same day.
        scheduler(dataRoot, day1).checkAndResolveIfDue();
        PlayerSnapshotStore store = new PlayerSnapshotStore(dataRoot.resolve("players/acct-1"), mapper);
        assertTrue(store.load(1).isPresent());
        assertTrue(store.load(2).isEmpty());

        // Another restart, now on the next resolving day.
        Clock day2 = Clock.fixed(Instant.parse("2026-09-30T08:00:00Z"), ZONE);
        scheduler(dataRoot, day2).checkAndResolveIfDue();

        assertTrue(store.load(2).isPresent(), "turn 2 must resolve as the next consecutive game turn");
        Optional<SchedulerState> state = new SchedulerStateStore(dataRoot.resolve("scheduler-state.json"), mapper).load();
        assertEquals(3, state.get().getNextGameTurn());
    }

    /**
     * Verifies the unconfirmed missed-day behavior (design.md &sect;3.6):
     * if the server is down across more than one scheduled resolution day,
     * only the single next game turn resolves on the next check - the
     * missed days in between are skipped, not caught up one turn each.
     */
    @Test
    void missedDaysAreSkippedNotCaughtUpOneTurnEach(@TempDir Path dataRoot) throws IOException {
        createPlayer(dataRoot, "acct-1");
        Clock bootstrap = Clock.fixed(Instant.parse("2026-09-28T08:00:00Z"), ZONE);
        scheduler(dataRoot, bootstrap).checkAndResolveIfDue();
        Clock day1 = Clock.fixed(Instant.parse("2026-09-29T08:00:00Z"), ZONE);
        scheduler(dataRoot, day1).checkAndResolveIfDue();

        // The server is down for a few days, then boots again three days later.
        Clock threeDaysLater = Clock.fixed(Instant.parse("2026-10-02T08:00:00Z"), ZONE);
        scheduler(dataRoot, threeDaysLater).checkAndResolveIfDue();

        PlayerSnapshotStore store = new PlayerSnapshotStore(dataRoot.resolve("players/acct-1"), mapper);
        assertTrue(store.load(2).isPresent(), "exactly one more turn resolves, not one per missed day");
        assertTrue(store.load(3).isEmpty());
        Optional<SchedulerState> state = new SchedulerStateStore(dataRoot.resolve("scheduler-state.json"), mapper).load();
        assertEquals(3, state.get().getNextGameTurn());
    }

    /**
     * Verifies that one player's resolution failing leaves the scheduler
     * state unadvanced, and that a retry converges: the players that
     * already succeeded are untouched (idempotent), and the one that failed
     * resolves on the retry.
     */
    @Test
    void aFailingPlayerLeavesStateUnadvancedAndARetryConverges(@TempDir Path dataRoot) throws IOException {
        createPlayer(dataRoot, "good");
        Path badPlayerRoot = dataRoot.resolve("players").resolve("bad");
        Path badSnapshotFile = badPlayerRoot.resolve("snapshots").resolve("0.json");
        Files.createDirectories(badSnapshotFile.getParent());
        Files.writeString(badSnapshotFile, "not valid json");
        Clock bootstrap = Clock.fixed(Instant.parse("2026-09-28T08:00:00Z"), ZONE);
        scheduler(dataRoot, bootstrap).checkAndResolveIfDue();
        Clock day1 = Clock.fixed(Instant.parse("2026-09-29T08:00:00Z"), ZONE);
        TurnScheduler scheduler = scheduler(dataRoot, day1);

        assertThrows(IOException.class, scheduler::checkAndResolveIfDue);
        // Fix the broken player, as an operator would, then retry the same day.
        mapper.writeValue(badSnapshotFile.toFile(),
            new ResolvedSnapshot(new PlayerSnapshot("Agency", Archetype.NASA, 0, 0, 0, 1), null));
        scheduler.checkAndResolveIfDue();

        assertTrue(new PlayerSnapshotStore(dataRoot.resolve("players/good"), mapper).load(1).isPresent());
        assertTrue(new PlayerSnapshotStore(badPlayerRoot, mapper).load(1).isPresent());
        Optional<SchedulerState> state = new SchedulerStateStore(dataRoot.resolve("scheduler-state.json"), mapper).load();
        assertTrue(state.isPresent());
        assertEquals(2, state.get().getNextGameTurn());
        assertEquals(LocalDate.of(2026, 9, 29), state.get().getLastResolvedDate());
    }

    /**
     * Verifies that a player directory with no saved snapshot yet - whether
     * it has no {@code snapshots} subdirectory at all, or an empty one
     * (e.g. a crash between creating that directory and writing turn 0's
     * file into it) - is skipped rather than causing the whole check to
     * fail.
     */
    @Test
    void skipsPlayerDirectoriesWithNoSavedSnapshotYet(@TempDir Path dataRoot) throws IOException {
        createPlayer(dataRoot, "acct-1");
        Files.createDirectories(dataRoot.resolve("players").resolve("no-snapshots-dir-at-all"));
        Files.createDirectories(dataRoot.resolve("players").resolve("empty-snapshots-dir").resolve("snapshots"));
        Clock bootstrap = Clock.fixed(Instant.parse("2026-09-28T08:00:00Z"), ZONE);
        scheduler(dataRoot, bootstrap).checkAndResolveIfDue();
        Clock day1 = Clock.fixed(Instant.parse("2026-09-29T08:00:00Z"), ZONE);

        scheduler(dataRoot, day1).checkAndResolveIfDue();

        assertTrue(new PlayerSnapshotStore(dataRoot.resolve("players/acct-1"), mapper).load(1).isPresent());
    }
}
