package de.mkoehler.launchwindow.persistence;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.actions.AssignResearchAction;
import de.mkoehler.launchwindow.actions.CancelMissionAction;
import de.mkoehler.launchwindow.actions.CloseProgramAction;
import de.mkoehler.launchwindow.actions.CloseRocketProgramAction;
import de.mkoehler.launchwindow.actions.ConvertPrestigeAction;
import de.mkoehler.launchwindow.actions.DismissAction;
import de.mkoehler.launchwindow.actions.HireAction;
import de.mkoehler.launchwindow.actions.LaunchMissionAction;
import de.mkoehler.launchwindow.actions.OpenProgramAction;
import de.mkoehler.launchwindow.actions.OpenRocketProgramAction;
import de.mkoehler.launchwindow.actions.PullOutOfTrainingAction;
import de.mkoehler.launchwindow.actions.ReleaseResearchAction;
import de.mkoehler.launchwindow.actions.StartExpertTrainingAction;
import de.mkoehler.launchwindow.actions.StartTrainingAction;
import de.mkoehler.launchwindow.actions.UpgradeBuildingAction;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.Building;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.TrainingKind;
import de.mkoehler.launchwindow.net.MessageRegistry;
import de.mkoehler.launchwindow.net.messages.SubmitActionRequest;
import de.mkoehler.launchwindow.rules.HiringPool;
import de.mkoehler.launchwindow.rules.PersonnelGroup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the M4 journaled actions: every type survives the JSON journal and
 * the Kryo wire, submission journals only actions that take effect, and replay
 * and resolution are deterministic ({@code design.md} &sect;5, M4).
 *
 * @author Mario Koehler
 */
class PlayerActionsTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private static List<PlayerAction> oneOfEach() {
        List<PlayerAction> actions = List.of(
            new OpenProgramAction("sputnik-1"), new CloseProgramAction("sputnik-1"),
            new OpenRocketProgramAction("r7-sputnik"), new CloseRocketProgramAction("r7-sputnik"),
            new LaunchMissionAction("sputnik-1-orbit", List.of(), List.of("p1", "p2"), List.of(), List.of("r7-sputnik")),
            new CancelMissionAction("m0"), new HireAction(PersonnelGroup.SET, "p3"),
            new DismissAction(PersonnelGroup.ASTRONAUT, "p4"), new UpgradeBuildingAction(Building.MISSION_CONTROL),
            new ConvertPrestigeAction(500), new AssignResearchAction("p5", "sputnik-1-satellite"),
            new ReleaseResearchAction("p5"),
            new StartTrainingAction(PersonnelGroup.MISSION_CONTROL, "p6", TrainingKind.PERMANENT, "PROPULSION", 70.5),
            new StartExpertTrainingAction(PersonnelGroup.ASTRONAUT, "p7", "PILOTING"),
            new PullOutOfTrainingAction(PersonnelGroup.SET, "p8"));
        long seed = 1;
        for (PlayerAction action : actions) {
            action.setSeed(seed++);
        }
        return actions;
    }

    @Test
    void everyActionSurvivesTheJsonJournal() throws IOException {
        for (PlayerAction action : oneOfEach()) {
            String json = mapper.writeValueAsString(action);
            PlayerAction copy = mapper.readValue(json, PlayerAction.class);
            assertEquals(action.getClass(), copy.getClass());
            assertEquals(json, mapper.writeValueAsString(copy), "round-trip of " + json);
        }
    }

    @Test
    void everyActionSurvivesTheKryoWire() throws IOException {
        Kryo kryo = new Kryo();
        MessageRegistry.register(kryo);
        for (PlayerAction action : oneOfEach()) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (Output output = new Output(bytes)) {
                kryo.writeClassAndObject(output, new SubmitActionRequest(action));
            }
            SubmitActionRequest copy;
            try (Input input = new Input(bytes.toByteArray())) {
                copy = (SubmitActionRequest) kryo.readClassAndObject(input);
            }
            assertEquals(mapper.writeValueAsString(action), mapper.writeValueAsString(copy.action()));
        }
    }

    @Test
    void submissionJournalsOnlyActionsThatTakeEffect(@TempDir Path playerRoot) throws IOException {
        PlayerStateService service = newPlayer(playerRoot, 25_000);

        ActionResult rejected = service.submitAction(new OpenProgramAction("explorer-1"));
        ActionResult accepted = service.submitAction(new OpenProgramAction("sputnik-1"));

        assertEquals(new ActionResult(false, "WRONG_AGENCY"), rejected);
        assertEquals(new ActionResult(true, "OPENED"), accepted);
        assertEquals(1, new PlayerActionJournal(playerRoot, mapper).readPending().size());
        assertEquals(25_000 - 876, service.currentState().getFunds());
    }

    @Test
    void aSeededHireReplaysIdenticallyAndResolutionIsReproducible(@TempDir Path playerRoot) throws IOException {
        PlayerStateService service = newPlayer(playerRoot, 25_000);
        String candidate = service.currentState().getSetSpecialistCandidates().get(0).getId();
        HireAction hire = new HireAction(PersonnelGroup.SET, candidate);
        hire.setSeed(7);
        assertTrue(service.submitAction(hire).applied());
        assertTrue(service.submitAction(new OpenProgramAction("sputnik-1")).applied());

        String first = mapper.writeValueAsString(service.currentState());
        assertEquals(first, mapper.writeValueAsString(service.currentState()), "replay is deterministic");

        TestContexts.resolveQuietly(service, 1);
        Path turnFile = playerRoot.resolve("snapshots").resolve("1.json");
        String resolved = Files.readString(turnFile);

        // Undo the turn by hand - snapshot gone, journal back - and resolve it again.
        Files.delete(turnFile);
        Path archive = playerRoot.resolve("journal-archive").resolve("1");
        try (var files = Files.list(archive)) {
            for (Path file : files.toList()) {
                Files.move(file, playerRoot.resolve("journal").resolve(file.getFileName()));
            }
        }
        TestContexts.resolveQuietly(service, 1);
        assertEquals(resolved, Files.readString(turnFile), "the same turn resolves to the same bytes");
    }

    @Test
    void stateBeforeReadsTheStartOfTurnSnapshotEvenAfterTheTurnIsWritten(@TempDir Path playerRoot)
            throws IOException {
        PlayerStateService service = newPlayer(playerRoot, 25_000);
        service.submitAction(new OpenProgramAction("sputnik-1"));
        TestContexts.resolveQuietly(service, 1);

        PlayerSnapshot start = service.stateBefore(1).orElseThrow();
        assertFalse(start.getOpenedProgramIds().contains("sputnik-1"));
        assertTrue(service.stateBefore(2).orElseThrow().getOpenedProgramIds().contains("sputnik-1"));
    }

    private PlayerStateService newPlayer(Path playerRoot, long funds) throws IOException {
        PlayerSnapshot start = new PlayerSnapshot("Agency", Archetype.SOVIET, funds, 250, 3_000, 1);
        HiringPool.refreshCandidates(start, new Random(1));
        new PlayerSnapshotStore(playerRoot, mapper).save(0, start, null);
        return new PlayerStateService(playerRoot, mapper, TestContexts.EARLY_ERA);
    }
}
