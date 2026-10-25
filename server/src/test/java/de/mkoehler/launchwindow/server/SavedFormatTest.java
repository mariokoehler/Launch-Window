package de.mkoehler.launchwindow.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.content.BudgetSchedule;
import de.mkoehler.launchwindow.content.ContentPackLoader;
import de.mkoehler.launchwindow.content.RandomEventRoster;
import de.mkoehler.launchwindow.persistence.ActionContext;
import de.mkoehler.launchwindow.persistence.PlayerSnapshotStore;
import de.mkoehler.launchwindow.persistence.PlayerStateService;
import de.mkoehler.launchwindow.persistence.TurnReportStore;
import de.mkoehler.launchwindow.server.accounts.AccountStore;
import de.mkoehler.launchwindow.server.accounts.PasswordHasher;
import de.mkoehler.launchwindow.server.schedule.FrontPageStore;
import de.mkoehler.launchwindow.server.schedule.GameMeta;
import de.mkoehler.launchwindow.server.schedule.GameMetaStore;
import de.mkoehler.launchwindow.server.schedule.TurnScheduler;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>Verifies that a game saved by an earlier build still loads and resolves on this one (review.md 2.1):
 * every fixture under {@code src/test/resources/saved-format/} ({@link SavedFormatFixture}) is copied, opened
 * the way the server opens a data root, and its open turn resolved. Saved state is read strictly, so a field
 * renamed, removed or retyped on any saved class, or an enum constant renamed, fails here unless a
 * migration step ({@code SnapshotMigrations}) carries the old files over.</p>
 *
 * @author Mario Koehler
 */
class SavedFormatTest {

    private static final Path FIXTURES = Path.of("src", "test", "resources", "saved-format");

    @TempDir
    Path work;

    @TestFactory
    Stream<DynamicTest> everyFixtureLoadsAndResolvesItsNextTurn() throws IOException {
        List<Path> fixtures;
        try (Stream<Path> listing = Files.list(FIXTURES)) {
            fixtures = listing.filter(Files::isDirectory).sorted().toList();
        }
        assertFalse(fixtures.isEmpty(), "no saved-format fixture: run SavedFormatFixture");
        return fixtures.stream().map(fixture -> DynamicTest.dynamicTest(fixture.getFileName().toString(),
            () -> loadsAndResolves(fixture)));
    }

    private void loadsAndResolves(Path fixture) throws IOException {
        Path dataRoot = work.resolve(fixture.getFileName().toString());
        copyTree(fixture, dataRoot);
        ObjectMapper mapper = GameServer.createMapper();

        // As GameServer.create opens a data root: the meta, the frozen pack, the accounts.
        GameMeta meta = GameMetaStore.loadOrCreateDefault(dataRoot.resolve("game-meta.json"), mapper).meta();
        FrozenContentPack pack = FrozenContentPack.freeze(dataRoot, meta.getContentPack(),
            SavedFormatTest.class.getClassLoader());
        ActionContext context = new ActionContext(ContentPackLoader.load(pack.directory()), BudgetSchedule.baspmClose());
        AccountStore accounts = new AccountStore(dataRoot.resolve("accounts.json"));
        try {
            assertTrue(accounts.login(SavedFormatFixture.LOGIN, SavedFormatFixture.PASSWORD).success());
            assertTrue(accounts.login(SavedFormatFixture.LEGACY_LOGIN, SavedFormatFixture.LEGACY_PASSWORD).success(),
                "an account on the old hash still logs in");
            assertEquals(PasswordHasher.SCHEME,
                accounts.findByLogin(SavedFormatFixture.LEGACY_LOGIN).orElseThrow().getPasswordScheme());
        } finally {
            accounts.close();
        }

        // Every player's state, with the pending journal replayed, and their last report.
        Map<Path, Long> before = new HashMap<>();
        List<Path> players;
        try (Stream<Path> listing = Files.list(dataRoot.resolve("players"))) {
            players = listing.filter(Files::isDirectory).sorted().toList();
        }
        assertFalse(players.isEmpty());
        for (Path player : players) {
            new PlayerStateService(player, mapper, context).currentState();
            TurnReportStore reports = new TurnReportStore(player, mapper);
            long last = reports.latestTurn().orElseThrow();
            assertTrue(reports.load(last).isPresent(), player + "'s last report loads");
            before.put(player, new PlayerSnapshotStore(player, mapper).latestTurnNumber().orElseThrow());
        }
        FrontPageStore.load(dataRoot, mapper);
        new GameInfoService(dataRoot, mapper, pack.packId(), pack.hash(), meta, Clock.systemDefaultZone(),
            context.schedule()).info();

        TurnScheduler scheduler = new TurnScheduler(dataRoot, mapper, Clock.fixed(Instant.parse("2026-10-25T06:00:00Z"),
            ZoneId.of("Europe/Berlin")), context, RandomEventRoster.baspmClose());
        long resolved = scheduler.resolveNow();
        for (Path player : players) {
            assertEquals(before.get(player) + 1,
                new PlayerSnapshotStore(player, mapper).latestTurnNumber().orElseThrow(), player + " resolved");
            assertTrue(new TurnReportStore(player, mapper).load(resolved).isPresent(), player + " has a report");
            Path marker = player.resolve("bot.json");
            if (Files.exists(marker)) {
                assertTrue(mapper.readTree(marker.toFile()).get("lastPlayedTurn").asLong() >= resolved,
                    player + " played the turn");
            }
        }
    }

    private static void copyTree(Path from, Path to) throws IOException {
        try (Stream<Path> walk = Files.walk(from)) {
            for (Path source : walk.toList()) {
                Path target = to.resolve(from.relativize(source).toString());
                if (Files.isDirectory(source)) {
                    Files.createDirectories(target);
                } else {
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }
}
