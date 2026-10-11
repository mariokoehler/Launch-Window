package de.mkoehler.launchwindow.server.schedule;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.actions.ApplyContractAction;
import de.mkoehler.launchwindow.content.RandomEventRoster;
import de.mkoehler.launchwindow.model.ActiveContract;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.ContractListing;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.persistence.PlayerSnapshotStore;
import de.mkoehler.launchwindow.persistence.PlayerStateService;
import de.mkoehler.launchwindow.rules.ContractMarket;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the contract market across turns on the server ({@code design.md} &sect;2.11): settled once
 * a turn into {@code market/turn-N.json}, the board in every agency's state, the awards from the
 * applications made against it.
 *
 * @author Mario Koehler
 */
class ContractMarketTurnTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Berlin");

    private final ObjectMapper mapper = GameServer.createMapper();

    @Test
    void applicationsAgainstTheBoardAreAwardedAtTheNextResolution(@TempDir Path dataRoot) throws IOException {
        agency(dataRoot, "acct-a", "Able", 90);
        agency(dataRoot, "acct-b", "Baker", 70);
        GameMeta meta = GameMetaStore.loadOrCreateDefault(dataRoot.resolve("game-meta.json"), mapper).meta();
        meta.setDeclaredGoalIds(GameMeta.EARLY_ERA_DECLARED_GOALS);
        mapper.writeValue(dataRoot.resolve("game-meta.json").toFile(), meta);
        scheduler(dataRoot, "2026-09-28").checkAndResolveIfDue();
        scheduler(dataRoot, "2026-09-29").checkAndResolveIfDue();

        ContractMarket.Settlement first = mapper.readValue(TurnScheduler.marketFile(dataRoot, 1).toFile(),
            ContractMarket.Settlement.class);
        assertTrue(first.board().size() >= 2, "two agencies in one line: a listing to seat them and an extra");
        PlayerStateService able = service(dataRoot, "acct-a");
        PlayerStateService baker = service(dataRoot, "acct-b");
        assertEquals(first.board(), able.currentState().getContractBoard());
        ContractListing listing = first.board().getFirst();

        assertTrue(able.submitAction(new ApplyContractAction(listing.id(), null)).applied());
        assertTrue(baker.submitAction(new ApplyContractAction(listing.id(), null)).applied());
        scheduler(dataRoot, "2026-09-30").checkAndResolveIfDue();

        ActiveContract won = able.currentState().getActiveContract();
        assertEquals(1, won.tier(), "the better hardware wins tier 1");
        assertEquals(2, baker.currentState().getActiveContract().tier());
        assertEquals(2 + 3, won.launchByTurn());
        assertNull(able.currentState().getContractApplication());
        assertTrue(able.currentState().getContractBoard().stream().noneMatch(l -> l.id().equals(listing.id())));
        assertTrue(Files.exists(TurnScheduler.marketFile(dataRoot, 2)));
    }

    private void agency(Path dataRoot, String account, String name, double reliability) throws IOException {
        PlayerSnapshot snapshot = new PlayerSnapshot(name, Archetype.NASA, 50_000, 100, 0, 1);
        snapshot.getOpenedProgramIds().add("explorer-1");
        snapshot.getMissionComponents().put("explorer-1-satellite", new OwnedMissionComponent(reliability, 3));
        snapshot.getMissionComponents().put("jupiter-c", new OwnedMissionComponent(reliability, 3));
        snapshot.getSuccessfulFlightCounts().put("explorer-1-orbit", 1);
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
