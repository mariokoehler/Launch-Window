package de.mkoehler.launchwindow.server.schedule;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.actions.PublishFindingAction;
import de.mkoehler.launchwindow.content.RandomEventRoster;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.persistence.PlayerSnapshotStore;
import de.mkoehler.launchwindow.persistence.PlayerStateService;
import de.mkoehler.launchwindow.rules.FindingLearned;
import de.mkoehler.launchwindow.rules.FindingPublications;
import de.mkoehler.launchwindow.rules.FindingPublished;
import de.mkoehler.launchwindow.rules.Findings;
import de.mkoehler.launchwindow.rules.ResolutionEvent;
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
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests publishing a finding across a turn on the server ({@code design.md} &sect;2.13): settled once
 * into {@code publications/turn-N.json}, paid to the publisher, learned by the rivals.
 *
 * @author Mario Koehler
 */
class FindingPublicationsTurnTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Berlin");
    private static final String FINDING = "organisms-in-microgravity";

    private final ObjectMapper mapper = GameServer.createMapper();

    @Test
    void aPublicationPaysThePublisherAndTeachesTheRivals(@TempDir Path dataRoot) throws IOException {
        agency(dataRoot, "acct-a", "Able", true);
        agency(dataRoot, "acct-b", "Baker", false);
        agency(dataRoot, "acct-c", "Charlie", false);
        GameMeta meta = GameMetaStore.loadOrCreateDefault(dataRoot.resolve("game-meta.json"), mapper).meta();
        meta.setDeclaredGoalIds(GameMeta.EARLY_ERA_DECLARED_GOALS);
        mapper.writeValue(dataRoot.resolve("game-meta.json").toFile(), meta);
        scheduler(dataRoot, "2026-09-28").checkAndResolveIfDue();
        scheduler(dataRoot, "2026-09-29").checkAndResolveIfDue();

        PlayerStateService able = service(dataRoot, "acct-a");
        PlayerStateService baker = service(dataRoot, "acct-b");
        assertFalse(baker.submitAction(new PublishFindingAction(FINDING, true)).applied(), "Baker doesn't know it");
        assertTrue(able.submitAction(new PublishFindingAction(FINDING, true)).applied());
        assertEquals(List.of(FINDING), able.currentState().getPendingPublications());
        scheduler(dataRoot, "2026-09-30").checkAndResolveIfDue();

        assertTrue(Files.exists(TurnScheduler.publicationsFile(dataRoot, 2)));
        long pays = Math.round(800 * Findings.PRESTIGE_SHARE);
        List<ResolutionEvent> ableEvents = able.turnReport(2).orElseThrow().events();
        assertTrue(ableEvents.contains(new FindingPublished(FINDING, pays, 2, 2, List.of())), ableEvents.toString());
        assertTrue(able.currentState().getPendingPublications().isEmpty());
        assertEquals(List.of(FINDING), able.currentState().getPublishedFindings());

        PlayerSnapshot bakerNow = baker.currentState();
        assertEquals(2, bakerNow.getPublicFindings().get(FINDING));
        assertTrue(Findings.knows(bakerNow, FINDING));
        assertTrue(baker.turnReport(2).orElseThrow().events()
            .contains(new FindingLearned(FINDING, List.of("Able"), false)));

        // A redone resolution reads the same settlement back.
        FindingPublications.Settlement kept = mapper.readValue(TurnScheduler.publicationsFile(dataRoot, 2).toFile(),
            FindingPublications.Settlement.class);
        assertEquals(pays, kept.turnFor(GameSeeds.rivalKey(meta.getGameSeed(), "acct-a")).getFirst().prestige());
    }

    private void agency(Path dataRoot, String account, String name, boolean holds) throws IOException {
        PlayerSnapshot snapshot = new PlayerSnapshot(name, Archetype.NASA, 50_000, 100, 0, 1);
        if (holds) {
            snapshot.getAchievedGoals().put(FINDING, 0);
        }
        new PlayerSnapshotStore(dataRoot.resolve("players").resolve(account), mapper).save(0, snapshot, null);
    }

    private PlayerStateService service(Path dataRoot, String account) {
        return new PlayerStateService(dataRoot.resolve("players").resolve(account), mapper, ServerTestGames.CONTEXT);
    }

    /** A scheduler at 10:00 Berlin on the given day. */
    private TurnScheduler scheduler(Path dataRoot, String day) {
        return new TurnScheduler(dataRoot, mapper, Clock.fixed(Instant.parse(day + "T08:00:00Z"), ZONE),
            ServerTestGames.CONTEXT, RandomEventRoster.baspmClose());
    }

}
