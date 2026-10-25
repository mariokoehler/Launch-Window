package de.mkoehler.launchwindow.server.schedule;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.content.RandomEventRoster;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.persistence.PlayerSnapshotStore;
import de.mkoehler.launchwindow.persistence.PlayerStateService;
import de.mkoehler.launchwindow.server.GameServer;
import de.mkoehler.launchwindow.server.ServerTestGames;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the filler bots ({@code design.md} &sect;5): their slots are made from {@code game-meta.json}, they
 * play a whole early-era game through the server's own turn resolution without a refusal stopping them, they
 * reach the race's goals, and a turn is played once.
 *
 * @author Mario Koehler
 */
class BotPlayersTest {

    private static final int TURNS = 60;

    private final ObjectMapper mapper = GameServer.createMapper();

    private TurnScheduler botGame(Path dataRoot) throws IOException {
        GameMeta meta = ServerTestGames.meta(false);
        meta.setContentPack("early-era");
        meta.setDeclaredGoalIds(GameMeta.EARLY_ERA_DECLARED_GOALS);
        meta.setBots(List.of(new BotPlayer("bot-1", Archetype.NASA, null, null),
            new BotPlayer("bot-2", Archetype.SOVIET, "Test Agency", "Ann Tester")));
        Files.createDirectories(dataRoot);
        mapper.writeValue(dataRoot.resolve("game-meta.json").toFile(), meta);
        return new TurnScheduler(dataRoot, mapper, Clock.fixed(Instant.parse("2026-09-29T06:00:00Z"),
            ZoneId.of("Europe/Berlin")), ServerTestGames.CONTEXT, RandomEventRoster.baspmClose());
    }

    private PlayerSnapshot bot(Path dataRoot, String id) throws IOException {
        return new PlayerSnapshotStore(dataRoot.resolve("players").resolve(id), mapper).load().orElseThrow();
    }

    @Test
    void botsPlayAWholeGameAndReachTheRace(@TempDir Path dataRoot) throws IOException {
        TurnScheduler scheduler = botGame(dataRoot);
        for (int turn = 1; turn <= TURNS; turn++) {
            scheduler.resolveNow();
        }
        PlayerSnapshot nasa = bot(dataRoot, "bot-1");
        PlayerSnapshot soviet = bot(dataRoot, "bot-2");
        assertEquals("Test Agency", soviet.getAgencyName(), "a name given in game-meta.json is kept");
        assertEquals("Ann Tester", soviet.getDirectorName());
        assertFalse(nasa.getAgencyName().isBlank(), "a name not given is drawn");
        assertNotEquals(nasa.getDirectorName(), soviet.getDirectorName());
        for (PlayerSnapshot agency : List.of(nasa, soviet)) {
            assertEquals(TURNS + 1, agency.getSeason(), agency.getAgencyName() + " resolved every turn");
            assertFalse(agency.getOpenedProgramIds().isEmpty(), agency.getAgencyName() + " opened a program");
            assertFalse(agency.getAstronauts().isEmpty(), agency.getAgencyName() + " hired astronauts");
            for (String goal : GameMeta.EARLY_ERA_DECLARED_GOALS) {
                assertTrue(agency.hasAchievedOrCancelledGoal(goal), agency.getAgencyName() + " reached " + goal);
            }
        }
        assertTrue(Files.exists(dataRoot.resolve(GameResult.FILE_NAME)), "a bot can win the race");
    }

    @Test
    void aTurnIsPlayedOnce(@TempDir Path dataRoot) throws IOException {
        TurnScheduler scheduler = botGame(dataRoot);
        scheduler.resolveNow();
        GameMeta meta = GameMetaStore.loadOrCreateDefault(dataRoot.resolve("game-meta.json"), mapper).meta();
        BotPlayers.play(dataRoot, mapper, ServerTestGames.CONTEXT, meta, 2);
        PlayerStateService service = new PlayerStateService(dataRoot.resolve("players/bot-1"), mapper,
            ServerTestGames.CONTEXT);
        PlayerSnapshot afterFirst = service.currentState();
        BotPlayers.play(dataRoot, mapper, ServerTestGames.CONTEXT, meta, 2);
        assertEquals(mapper.writeValueAsString(afterFirst), mapper.writeValueAsString(service.currentState()),
            "playing the same turn again changes nothing");
    }

    @Test
    void aRollbackOfSeveralTurnsLetsTheBotsPlayThemAgain(@TempDir Path dataRoot) throws IOException {
        TurnScheduler scheduler = botGame(dataRoot);
        for (int turn = 1; turn <= 5; turn++) {
            scheduler.resolveNow();
        }
        String played = mapper.writeValueAsString(bot(dataRoot, "bot-1"));
        TurnRollback.rollBackTo(dataRoot, mapper, ServerTestGames.CONTEXT, 3, java.time.LocalDateTime.of(2026, 9, 29, 7, 0));
        assertEquals(3, mapper.readValue(dataRoot.resolve("players/bot-1").resolve(BotPlayers.MARKER).toFile(),
            BotPlayers.Marker.class).lastPlayedTurn(), "the turn rolled back to counts as played");
        for (int turn = 3; turn <= 5; turn++) {
            scheduler.resolveNow();
        }
        assertEquals(played, mapper.writeValueAsString(bot(dataRoot, "bot-1")),
            "the redone turns come out as the first time: the bots played every one of them");
    }
}
