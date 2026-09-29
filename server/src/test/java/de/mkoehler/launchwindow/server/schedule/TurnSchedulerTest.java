package de.mkoehler.launchwindow.server.schedule;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.persistence.PlayerSnapshotStore;
import de.mkoehler.launchwindow.persistence.ResolvedSnapshot;
import de.mkoehler.launchwindow.server.GameServer;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        TurnScheduler scheduler = new TurnScheduler(dataRoot, mapper, clock);

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
        TurnScheduler scheduler = new TurnScheduler(dataRoot, mapper, clock);

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
        new TurnScheduler(dataRoot, mapper, beforeToday).checkAndResolveIfDue();
        Clock afterResolutionTime = Clock.fixed(Instant.parse("2026-09-29T08:00:00Z"), ZONE);
        TurnScheduler scheduler = new TurnScheduler(dataRoot, mapper, afterResolutionTime);

        scheduler.checkAndResolveIfDue();
        scheduler.checkAndResolveIfDue();

        PlayerSnapshotStore store = new PlayerSnapshotStore(dataRoot.resolve("players/acct-1"), mapper);
        assertTrue(store.load(1).isPresent());
        assertTrue(store.load(2).isEmpty());
    }

    /**
     * Verifies that a resolving day before its configured resolution time
     * does not resolve yet.
     */
    @Test
    void doesNotResolveBeforeTheConfiguredTime(@TempDir Path dataRoot) throws IOException {
        createPlayer(dataRoot, "acct-1");
        Clock bootstrap = Clock.fixed(Instant.parse("2026-09-28T08:00:00Z"), ZONE);
        new TurnScheduler(dataRoot, mapper, bootstrap).checkAndResolveIfDue();
        // 07:00 Europe/Berlin - before the DAILY default's 09:00.
        Clock beforeResolutionTime = Clock.fixed(Instant.parse("2026-09-29T05:00:00Z"), ZONE);

        new TurnScheduler(dataRoot, mapper, beforeResolutionTime).checkAndResolveIfDue();

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
        new TurnScheduler(dataRoot, mapper, bootstrap).checkAndResolveIfDue();
        Clock saturday = Clock.fixed(Instant.parse("2026-10-03T08:00:00Z"), ZONE);

        new TurnScheduler(dataRoot, mapper, saturday).checkAndResolveIfDue();

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
        TurnScheduler scheduler = new TurnScheduler(dataRoot, mapper, clock);
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
        new TurnScheduler(dataRoot, mapper, bootstrap).checkAndResolveIfDue();
        Clock day1 = Clock.fixed(Instant.parse("2026-09-29T08:00:00Z"), ZONE);
        new TurnScheduler(dataRoot, mapper, day1).checkAndResolveIfDue();

        // A brand-new instance, as if the process restarted, checking again the same day.
        new TurnScheduler(dataRoot, mapper, day1).checkAndResolveIfDue();
        PlayerSnapshotStore store = new PlayerSnapshotStore(dataRoot.resolve("players/acct-1"), mapper);
        assertTrue(store.load(1).isPresent());
        assertTrue(store.load(2).isEmpty());

        // Another restart, now on the next resolving day.
        Clock day2 = Clock.fixed(Instant.parse("2026-09-30T08:00:00Z"), ZONE);
        new TurnScheduler(dataRoot, mapper, day2).checkAndResolveIfDue();

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
        new TurnScheduler(dataRoot, mapper, bootstrap).checkAndResolveIfDue();
        Clock day1 = Clock.fixed(Instant.parse("2026-09-29T08:00:00Z"), ZONE);
        new TurnScheduler(dataRoot, mapper, day1).checkAndResolveIfDue();

        // The server is down for a few days, then boots again three days later.
        Clock threeDaysLater = Clock.fixed(Instant.parse("2026-10-02T08:00:00Z"), ZONE);
        new TurnScheduler(dataRoot, mapper, threeDaysLater).checkAndResolveIfDue();

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
        new TurnScheduler(dataRoot, mapper, bootstrap).checkAndResolveIfDue();
        Clock day1 = Clock.fixed(Instant.parse("2026-09-29T08:00:00Z"), ZONE);
        TurnScheduler scheduler = new TurnScheduler(dataRoot, mapper, day1);

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
        new TurnScheduler(dataRoot, mapper, bootstrap).checkAndResolveIfDue();
        Clock day1 = Clock.fixed(Instant.parse("2026-09-29T08:00:00Z"), ZONE);

        new TurnScheduler(dataRoot, mapper, day1).checkAndResolveIfDue();

        assertTrue(new PlayerSnapshotStore(dataRoot.resolve("players/acct-1"), mapper).load(1).isPresent());
    }
}
