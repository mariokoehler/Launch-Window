package de.mkoehler.launchwindow.server.schedule;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.actions.QueueIntelOperationAction;
import de.mkoehler.launchwindow.content.RandomEventRoster;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.FrontPage;
import de.mkoehler.launchwindow.model.IntelArea;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.persistence.PlayerSnapshotStore;
import de.mkoehler.launchwindow.persistence.PlayerStateService;
import de.mkoehler.launchwindow.server.GameSeeds;
import de.mkoehler.launchwindow.server.GameServer;
import de.mkoehler.launchwindow.server.ServerTestGames;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link TurnRollback} and {@link TurnBackups}: a rolled-back turn resolves again into
 * exactly the same files, the actions queued after it are set aside, and every turn is zipped
 * before it resolves ({@code design.md} &sect;3.7).
 *
 * @author Mario Koehler
 */
class TurnRollbackTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Berlin");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 1, 12, 0);

    private final ObjectMapper mapper = GameServer.createMapper();

    /**
     * Verifies the heart of it: a turn rolled back and resolved again writes byte-identical
     * snapshots, reports and intelligence, its own actions applied again and the later ones set
     * aside.
     */
    @Test
    void aRolledBackTurnResolvesAgainIntoTheSameFiles(@TempDir Path dataRoot) throws IOException {
        Path spyRoot = twoPlayers(dataRoot);
        resolveOn(dataRoot, "2026-09-29");
        queueOperations(dataRoot, spyRoot);
        resolveOn(dataRoot, "2026-09-30");
        PlayerStateService spy = service(spyRoot);
        assertTrue(spy.submitAction(QueueIntelOperationAction.assessment(target(dataRoot), IntelArea.RND)).applied());
        List<String> turnActions = names(spyRoot.resolve("journal-archive").resolve("2"));
        List<String> laterActions = names(spyRoot.resolve("journal"));
        assertEquals(2, turnActions.size());
        assertEquals(1, laterActions.size());
        Map<String, byte[]> before = turnFiles(dataRoot, spyRoot, 2);

        Path backup = TurnRollback.rollBackTo(dataRoot, mapper, ServerTestGames.CONTEXT, 2, NOW);

        assertTrue(Files.exists(backup));
        for (String file : before.keySet()) {
            assertFalse(Files.exists(dataRoot.resolve(file)), file + " is gone");
        }
        assertEquals(turnActions, names(spyRoot.resolve("journal")), "the turn's own actions are pending again");
        assertFalse(Files.exists(spyRoot.resolve("journal-archive").resolve("2")));
        assertEquals(laterActions, names(spyRoot.resolve("journal-rolled-back").resolve("2")),
            "the action queued after the turn is set aside");
        assertEquals(2, new SchedulerStateStore(dataRoot.resolve("scheduler-state.json"), mapper).load()
            .orElseThrow().getNextGameTurn());

        assertEquals(2, scheduler(dataRoot, "2026-09-30").resolveNow());

        Map<String, byte[]> after = turnFiles(dataRoot, spyRoot, 2);
        assertEquals(before.keySet(), after.keySet());
        for (String file : before.keySet()) {
            assertArrayEquals(before.get(file), after.get(file), file + " comes out the same");
        }
        assertEquals(turnActions, names(spyRoot.resolve("journal-archive").resolve("2")));
        assertTrue(names(spyRoot.resolve("journal")).isEmpty());
    }

    /**
     * Verifies rolling back several turns: only the first one's actions go back into the journal;
     * each later turn's go aside under that turn, so nothing resolves a turn early.
     */
    @Test
    void rollingBackSeveralTurnsSetsAsideEveryLaterTurnsActions(@TempDir Path dataRoot) throws IOException {
        Path spyRoot = twoPlayers(dataRoot);
        queueOperations(dataRoot, spyRoot);
        resolveOn(dataRoot, "2026-09-29");
        List<String> firstTurn = names(spyRoot.resolve("journal-archive").resolve("1"));
        queueOperations(dataRoot, spyRoot);
        resolveOn(dataRoot, "2026-09-30");
        List<String> secondTurn = names(spyRoot.resolve("journal-archive").resolve("2"));
        FrontPage earlier = new FrontPage("0-earth-orbit-satellite", "earth-orbit-satellite", 0, "sat-signal", "",
            List.of(), List.of(), List.of());
        FrontPageStore.keep(dataRoot, mapper, 0, List.of(earlier));
        FrontPageStore.keep(dataRoot, mapper, 2, List.of(new FrontPage("2-human-in-space", "human-in-space", 2,
            "space-first-flight", "", List.of(), List.of(), List.of())));

        TurnRollback.rollBackTo(dataRoot, mapper, ServerTestGames.CONTEXT, 1, NOW);

        assertEquals(firstTurn, names(spyRoot.resolve("journal")));
        assertEquals(secondTurn, names(spyRoot.resolve("journal-rolled-back").resolve("1")));
        assertEquals(List.of("0.json"), names(spyRoot.resolve("snapshots")));
        assertFalse(Files.exists(dataRoot.resolve("intelligence").resolve("turn-1.json")));
        assertFalse(Files.exists(TurnScheduler.marketFile(dataRoot, 1)));
        assertFalse(Files.exists(TurnScheduler.marketFile(dataRoot, 2)));
        assertEquals(List.of(earlier), FrontPageStore.load(dataRoot, mapper), "the undone turns' front pages go");
        assertFalse(Files.exists(dataRoot.resolve(TurnBackups.DIR).resolve(TurnBackups.turnZipName(2))),
            "the abandoned timeline's zip can't stand in for the redone turn's");
        assertTrue(Files.exists(dataRoot.resolve(TurnBackups.DIR).resolve(TurnBackups.turnZipName(1))));
        assertThrows(IllegalArgumentException.class,
            () -> TurnRollback.rollBackTo(dataRoot, mapper, ServerTestGames.CONTEXT, 1, NOW), "turn 1 is open again");
    }

    /**
     * Verifies a turn whose resolution failed halfway can be undone: one player resolved, the other
     * didn't, and the scheduler never moved on.
     */
    @Test
    void aHalfResolvedTurnCanBeRolledBack(@TempDir Path dataRoot) throws IOException {
        Path spyRoot = twoPlayers(dataRoot);
        resolveOn(dataRoot, "2026-09-29");
        queueOperations(dataRoot, spyRoot);
        List<String> turnActions = names(spyRoot.resolve("journal"));
        service(spyRoot).resolveTurn(2, de.mkoehler.launchwindow.rules.GoalTally.fromPlayers(List.of()),
            RandomEventRoster.baspmClose(), 7L);
        Path intelligence = dataRoot.resolve("intelligence").resolve("turn-2.json");
        Files.writeString(intelligence, "{}");
        assertEquals(2, TurnRollback.lastResolved(dataRoot, mapper));

        TurnRollback.rollBackTo(dataRoot, mapper, ServerTestGames.CONTEXT, 2, NOW);

        assertEquals(List.of("0.json", "1.json"), names(spyRoot.resolve("snapshots")));
        assertEquals(turnActions, names(spyRoot.resolve("journal")));
        assertFalse(Files.exists(intelligence));
        assertEquals(2, new SchedulerStateStore(dataRoot.resolve("scheduler-state.json"), mapper).load()
            .orElseThrow().getNextGameTurn());
        assertEquals(1, TurnRollback.lastResolved(dataRoot, mapper));
    }

    /**
     * Verifies the game result goes only when a rolled-back turn decided it.
     */
    @Test
    void theResultGoesOnlyWithTheTurnThatDecidedIt(@TempDir Path dataRoot) throws IOException {
        twoPlayers(dataRoot);
        PlayerSnapshot winner = new PlayerSnapshot("Winner", Archetype.SOVIET, 10_000, 0, 0, 1);
        winner.setAchievedGoals(Map.of("earth-orbit-satellite", 0, "human-in-space", 0, "human-in-orbit", 0));
        new PlayerSnapshotStore(dataRoot.resolve("players").resolve("acct-winner"), mapper).save(0, winner, null);
        resolveOn(dataRoot, "2026-09-29");
        resolveOn(dataRoot, "2026-09-30");
        Path result = dataRoot.resolve(GameResult.FILE_NAME);
        assertEquals(1, mapper.readValue(result.toFile(), GameResult.class).decidedOnTurn());

        TurnRollback.rollBackTo(dataRoot, mapper, ServerTestGames.CONTEXT, 2, NOW);
        assertTrue(Files.exists(result), "decided on turn 1, before the rollback");

        TurnRollback.rollBackTo(dataRoot, mapper, ServerTestGames.CONTEXT, 1, NOW);
        assertFalse(Files.exists(result));
    }

    /**
     * Verifies the zip before a turn: written before it resolves, without the logins or the frozen
     * pack, never replaced by a retry, and only the last {@link TurnBackups#KEEP} turns kept.
     */
    @Test
    void everyTurnIsZippedOnceBeforeItResolves(@TempDir Path dataRoot) throws IOException {
        twoPlayers(dataRoot);
        Files.writeString(dataRoot.resolve("accounts.json"), "[]");
        Files.createDirectories(dataRoot.resolve("content").resolve("early-era"));
        Files.writeString(dataRoot.resolve("content").resolve("early-era").resolve("programs.json"), "[]");
        resolveOn(dataRoot, "2026-09-29");

        Path zip = dataRoot.resolve(TurnBackups.DIR).resolve(TurnBackups.turnZipName(1));
        List<String> entries = entries(zip);
        assertTrue(entries.contains("players/acct-spy/snapshots/0.json"));
        assertTrue(entries.contains("scheduler-state.json"));
        assertTrue(entries.contains("game-meta.json"));
        assertFalse(entries.contains("players/acct-spy/snapshots/1.json"), "zipped before the turn resolved");
        assertFalse(entries.contains("accounts.json"));
        assertTrue(entries.stream().noneMatch(entry -> entry.startsWith("content/")));

        byte[] first = Files.readAllBytes(zip);
        TurnBackups.beforeTurn(dataRoot, 1);
        assertArrayEquals(first, Files.readAllBytes(zip), "a retry keeps the first attempt's zip");

        for (long turn = 2; turn <= 12; turn++) {
            TurnBackups.beforeTurn(dataRoot, turn);
        }
        List<String> kept = names(dataRoot.resolve(TurnBackups.DIR));
        assertEquals(TurnBackups.KEEP, kept.size());
        assertFalse(kept.contains(TurnBackups.turnZipName(2)));
        assertTrue(kept.contains(TurnBackups.turnZipName(3)));
    }

    /**
     * Verifies the follow-ups (ui-spec.md 4.18.1, G17): the turn after a First prints the rivals'
     * statements given in it, and a rolled-back turn keeps its own statements, drops later ones, and
     * prints the same follow-up again.
     */
    @Test
    void aFollowUpIsPrintedAndPrintedTheSameAfterARollback(@TempDir Path dataRoot) throws IOException {
        twoPlayers(dataRoot);
        resolveOn(dataRoot, "2026-09-29");
        FrontPage first = new FrontPage("1-earth-orbit-satellite", "earth-orbit-satellite", 1, "sat-signal", "",
            List.of(), List.of(new FrontPage.Achiever("Target", "Director", List.of())), List.of());
        FrontPageStore.keep(dataRoot, mapper, 1, List.of(first));
        long seed = GameMetaStore.loadOrCreateDefault(dataRoot.resolve("game-meta.json"), mapper).meta().getGameSeed();
        String spy = GameSeeds.rivalKey(seed, "acct-spy");
        ReactionStore.put(dataRoot, mapper, 2, spy,
            new FrontPage.Statement(first.id(), FrontPage.Stance.NEXT, "We will follow"));
        ReactionStore.put(dataRoot, mapper, 2, GameSeeds.rivalKey(seed, "acct-target"),
            new FrontPage.Statement(first.id(), FrontPage.Stance.DOWNPLAY, "Not us"));

        resolveOn(dataRoot, "2026-09-30");

        FrontPage followUp = FrontPageStore.load(dataRoot, mapper).stream().filter(FrontPage::followUp)
            .findFirst().orElseThrow();
        assertEquals("1-earth-orbit-satellite-reactions", followUp.id());
        assertEquals(2, followUp.gameTurn());
        assertEquals(List.of("Spy Bureau"), followUp.reactions().stream().map(FrontPage.Reaction::agencyName).toList(),
            "the First's own agency isn't asked");
        assertEquals("We will follow", followUp.reactions().get(0).quote());
        byte[] printed = Files.readAllBytes(dataRoot.resolve(FrontPageStore.FILE_NAME));
        ReactionStore.put(dataRoot, mapper, 3, spy, new FrontPage.Statement("2-x", FrontPage.Stance.POLITE, ""));

        TurnRollback.rollBackTo(dataRoot, mapper, ServerTestGames.CONTEXT, 2, NOW);

        assertEquals(List.of(first), FrontPageStore.load(dataRoot, mapper));
        assertFalse(ReactionStore.load(dataRoot, mapper, 2).isEmpty(), "the turn's own statements stay");
        assertTrue(ReactionStore.load(dataRoot, mapper, 3).isEmpty(), "later ones go");
        assertEquals(2, scheduler(dataRoot, "2026-09-30").resolveNow());
        assertArrayEquals(printed, Files.readAllBytes(dataRoot.resolve(FrontPageStore.FILE_NAME)),
            "the redone turn prints the same follow-up");
    }

    /** Two players, one a spy with level-2 intelligence, and the scheduler bootstrapped. */
    private Path twoPlayers(Path dataRoot) throws IOException {
        new PlayerSnapshotStore(dataRoot.resolve("players").resolve("acct-target"), mapper)
            .save(0, new PlayerSnapshot("Target", Archetype.NASA, 10_000, 100, 0, 1), null);
        Path spyRoot = dataRoot.resolve("players").resolve("acct-spy");
        PlayerSnapshot spy = new PlayerSnapshot("Spy Bureau", Archetype.SOVIET, 10_000, 100, 0, 1);
        spy.getIntelligence().setLevel(2);
        spy.getIntelligence().setTargetLevel(2);
        new PlayerSnapshotStore(spyRoot, mapper).save(0, spy, null);
        GameMeta meta = GameMetaStore.loadOrCreateDefault(dataRoot.resolve("game-meta.json"), mapper).meta();
        meta.setDeclaredGoalIds(GameMeta.EARLY_ERA_DECLARED_GOALS);
        mapper.writeValue(dataRoot.resolve("game-meta.json").toFile(), meta);
        scheduler(dataRoot, "2026-09-28").checkAndResolveIfDue();
        return spyRoot;
    }

    private void queueOperations(Path dataRoot, Path spyRoot) throws IOException {
        PlayerStateService spy = service(spyRoot);
        assertTrue(spy.submitAction(QueueIntelOperationAction.assessment(target(dataRoot), IntelArea.RND)).applied());
        assertTrue(spy.submitAction(QueueIntelOperationAction.assessment(target(dataRoot), IntelArea.PLANS)).applied());
    }

    private String target(Path dataRoot) throws IOException {
        long seed = GameMetaStore.loadOrCreateDefault(dataRoot.resolve("game-meta.json"), mapper).meta().getGameSeed();
        return GameSeeds.rivalKey(seed, "acct-target");
    }

    private PlayerStateService service(Path playerRoot) {
        return new PlayerStateService(playerRoot, mapper, ServerTestGames.CONTEXT);
    }

    private void resolveOn(Path dataRoot, String day) throws IOException {
        scheduler(dataRoot, day).checkAndResolveIfDue();
    }

    /** A scheduler at 10:00 Berlin on the given day, with the real random events. */
    private TurnScheduler scheduler(Path dataRoot, String day) {
        return new TurnScheduler(dataRoot, mapper, Clock.fixed(Instant.parse(day + "T08:00:00Z"), ZONE),
            ServerTestGames.CONTEXT, RandomEventRoster.baspmClose());
    }

    /** The files a turn writes, by path under the data root. */
    private Map<String, byte[]> turnFiles(Path dataRoot, Path spyRoot, long turn) throws IOException {
        Map<String, byte[]> files = new java.util.TreeMap<>();
        for (Path player : List.of(spyRoot, dataRoot.resolve("players").resolve("acct-target"))) {
            for (String folder : List.of("snapshots", "reports")) {
                Path file = player.resolve(folder).resolve(turn + ".json");
                files.put(dataRoot.relativize(file).toString(), Files.readAllBytes(file));
            }
        }
        Path intelligence = dataRoot.resolve("intelligence").resolve("turn-" + turn + ".json");
        files.put(dataRoot.relativize(intelligence).toString(), Files.readAllBytes(intelligence));
        Path market = TurnScheduler.marketFile(dataRoot, turn);
        files.put(dataRoot.relativize(market).toString(), Files.readAllBytes(market));
        return files;
    }

    private static List<String> names(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return List.of();
        }
        try (Stream<Path> listing = Files.list(dir)) {
            return listing.map(path -> path.getFileName().toString()).sorted().toList();
        }
    }

    private static List<String> entries(Path zip) throws IOException {
        List<String> entries = new ArrayList<>();
        try (ZipInputStream in = new ZipInputStream(Files.newInputStream(zip))) {
            for (ZipEntry entry = in.getNextEntry(); entry != null; entry = in.getNextEntry()) {
                entries.add(entry.getName());
            }
        }
        return Collections.unmodifiableList(entries);
    }
}
