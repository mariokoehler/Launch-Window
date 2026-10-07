package de.mkoehler.launchwindow.net;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import de.mkoehler.launchwindow.actions.BackgroundCheckAction;
import de.mkoehler.launchwindow.actions.CancelIntelOperationAction;
import de.mkoehler.launchwindow.actions.QueueIntelOperationAction;
import de.mkoehler.launchwindow.actions.SetIntelligenceTargetAction;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.Dossier;
import de.mkoehler.launchwindow.model.IntelArea;
import de.mkoehler.launchwindow.model.IntelCatch;
import de.mkoehler.launchwindow.model.IntelFact;
import de.mkoehler.launchwindow.model.IntelOperation;
import de.mkoehler.launchwindow.model.PersonnelFile;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.RivalFolder;
import de.mkoehler.launchwindow.model.SetSpecialist;
import de.mkoehler.launchwindow.model.Tidbit;
import de.mkoehler.launchwindow.model.Trait;
import de.mkoehler.launchwindow.model.TraitState;
import de.mkoehler.launchwindow.net.messages.FetchStateResponse;
import de.mkoehler.launchwindow.net.messages.RivalSummary;
import de.mkoehler.launchwindow.net.messages.RivalsResponse;
import de.mkoehler.launchwindow.net.messages.SubmitActionRequest;
import de.mkoehler.launchwindow.persistence.PlayerAction;
import de.mkoehler.launchwindow.rules.Intelligence;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sends the intelligence service's state and actions over Kryo ({@code design.md} &sect;2.9): a
 * populated snapshot, each new action, and the rivals with their keys, and checks that a late
 * game's folders stay well inside Kryo's 64 KB object buffer.
 *
 * @author Mario Koehler
 */
class IntelligenceWireTest {

    private static final int KRYO_BUFFER = 64 * 1024;

    private static byte[] write(Object message) {
        Kryo kryo = new Kryo();
        MessageRegistry.register(kryo);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (Output output = new Output(bytes)) {
            kryo.writeObject(output, message);
        }
        return bytes.toByteArray();
    }

    private static <T> T read(byte[] bytes, Class<T> type) {
        Kryo kryo = new Kryo();
        MessageRegistry.register(kryo);
        try (Input input = new Input(bytes)) {
            return kryo.readObject(input, type);
        }
    }

    private static IntelFact fact(IntelFact.Kind kind, String ref, int low, int high, int count) {
        return new IntelFact(kind, ref, low, high, count);
    }

    private static RivalFolder folder(String name, int files, int catches) {
        RivalFolder folder = new RivalFolder(name);
        List<IntelFact> rnd = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            rnd.add(fact(IntelFact.Kind.COMPONENT_RELIABILITY, "component-number-" + i, 60, 80, 0));
        }
        folder.getDossiers().put(IntelArea.RND, new Dossier(IntelArea.RND, 30, rnd));
        List<IntelFact> plans = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            plans.add(fact(IntelFact.Kind.OPEN_PROGRAM, "program-number-" + i, 0, 0, 0));
            plans.add(fact(IntelFact.Kind.RD_FOCUS, "component-number-" + i, 0, 0, 3));
            plans.add(fact(IntelFact.Kind.IN_FLIGHT, "configuration-number-" + i, 0, 0, 0));
        }
        plans.add(fact(IntelFact.Kind.INTEL_LEVEL, "", 0, 0, 2));
        folder.getDossiers().put(IntelArea.PLANS, new Dossier(IntelArea.PLANS, 31, plans));
        folder.getDossiers().put(IntelArea.PERSONNEL, new Dossier(IntelArea.PERSONNEL, 29, List.of(
            fact(IntelFact.Kind.HEADCOUNT, "ASTRONAUT", 0, 0, 8), fact(IntelFact.Kind.AVERAGE_SKILL, "ASTRONAUT", 50, 60, 0),
            fact(IntelFact.Kind.HEADCOUNT, "SET", 0, 0, 20), fact(IntelFact.Kind.AVERAGE_SKILL, "SET", 40, 50, 0),
            fact(IntelFact.Kind.HEADCOUNT, "MISSION_CONTROL", 0, 0, 13),
            fact(IntelFact.Kind.AVERAGE_SKILL, "MISSION_CONTROL", 45, 55, 0))));
        for (int i = 0; i < files; i++) {
            folder.getPersonnelFiles().add(new PersonnelFile("p" + i, "Person Number " + i, "portrait-" + i, "SET", 40,
                28, List.of(fact(IntelFact.Kind.SKILL, "ROCKETS", 50, 60, 0), fact(IntelFact.Kind.SKILL, "SPACE_PROBES",
                    40, 50, 0), fact(IntelFact.Kind.SKILL, "HUMAN_RATED_ROCKETS", 30, 40, 0), fact(IntelFact.Kind.SKILL,
                    "CREWED_SPACECRAFT", 60, 70, 0), fact(IntelFact.Kind.SKILL, "EVA_SUITS", 20, 30, 0)),
                i % 2 == 0 ? null : Trait.STEADY));
        }
        for (int i = 0; i < 12; i++) {
            folder.getTidbits().add(new Tidbit(20 + i, fact(IntelFact.Kind.COMPONENT_RELIABILITY, "component-" + i, 50,
                70, 0)));
        }
        for (int i = 0; i < catches; i++) {
            folder.getCatches().add(new IntelCatch(20 + i, i % 2 == 0, i % 3 == 0 ? IntelOperation.Kind.PERSONNEL_FILE
                : IntelOperation.Kind.ASSESSMENT, i % 3 == 0 ? null : IntelArea.RND, 2));
        }
        folder.learnLevel(2, 31, RivalFolder.FROM_DOSSIER);
        return folder;
    }

    @Test
    void aPopulatedIntelligenceStateCrossesTheWire() {
        PlayerSnapshot state = new PlayerSnapshot("Orion Spaceworks", Archetype.NASA, 20_000, 4_000, 3_000, 31);
        state.getIntelligence().setLevel(3);
        state.getIntelligence().setTargetLevel(2);
        state.getIntelligence().setActionsUsed(2);
        state.getIntelligence().getQueued().add(new IntelOperation("op3", IntelOperation.Kind.PERSONNEL_FILE, "kSever",
            null, "ASTRONAUT", null));
        state.getIntelligence().getFolders().put("kSever", folder("Sever Design Bureau", 2, 2));
        SetSpecialist candidate = new SetSpecialist("c1", "Checked Candidate", 30, 70, 50);
        candidate.setTraits(List.of(new TraitState(Trait.WORKAHOLIC, 0, true)));
        candidate.setIntelFind("CHECK");
        candidate.setIntelExhausted(true);
        state.getSetSpecialistCandidates().add(candidate);

        FetchStateResponse copy = read(write(FetchStateResponse.success(state)), FetchStateResponse.class);

        RivalFolder folder = copy.state().getIntelligence().getFolders().get("kSever");
        assertEquals(3, folder.getDossiers().size());
        assertEquals(IntelFact.Kind.INTEL_LEVEL, folder.getDossiers().get(IntelArea.PLANS).facts().getLast().kind());
        assertNull(folder.getPersonnelFiles().getFirst().trait());
        assertEquals(Trait.STEADY, folder.getPersonnelFiles().get(1).trait());
        assertNull(folder.getCatches().getFirst().area());
        assertEquals(2, folder.getKnownLevel());
        assertEquals("op3", copy.state().getIntelligence().getQueued().getFirst().id());
        assertEquals(2, copy.state().getIntelligence().getTargetLevel());
        SetSpecialist sent = copy.state().getSetSpecialistCandidates().getFirst();
        assertEquals("CHECK", sent.getIntelFind());
        assertTrue(sent.isIntelExhausted());
        assertEquals(Trait.WORKAHOLIC, sent.getTraits().getFirst().trait());
    }

    @Test
    void theNewActionsAndTheRivalsKeysCrossTheWire() {
        List<PlayerAction> actions = List.of(new SetIntelligenceTargetAction(3),
            QueueIntelOperationAction.assessment("kSever", IntelArea.PLANS),
            QueueIntelOperationAction.personnelFile("kSever", "SET", "p4"), new CancelIntelOperationAction("op2"),
            new BackgroundCheckAction("c9"));
        for (PlayerAction action : actions) {
            SubmitActionRequest copy = read(write(new SubmitActionRequest(action)), SubmitActionRequest.class);
            assertEquals(action.getClass(), copy.action().getClass());
        }
        QueueIntelOperationAction file = (QueueIntelOperationAction) read(write(new SubmitActionRequest(actions.get(2))),
            SubmitActionRequest.class).action();
        assertEquals("p4", file.getPersonId());
        RivalsResponse rivals = read(write(RivalsResponse.of(List.of(RivalSummary.of("Sever", "T. Abenova",
            Archetype.SOVIET, false, Map.of(), "kSever")))), RivalsResponse.class);
        assertEquals("kSever", rivals.rivals().getFirst().playerId());
    }

    @Test
    void aLateGamesFoldersStayWellInsideTheBuffer() {
        // Five rivals with every list at its cap: full dossiers, 12 tidbits, 8 files, 10 catches.
        PlayerSnapshot state = new PlayerSnapshot("Orion Spaceworks", Archetype.NASA, 20_000, 4_000, 3_000, 60);
        int empty = write(FetchStateResponse.success(state)).length;
        for (int i = 0; i < 5; i++) {
            state.getIntelligence().getFolders().put("k" + i, folder("Rival Agency Number " + i,
                Intelligence.FILES_KEPT, Intelligence.CATCHES_KEPT));
        }
        int full = write(FetchStateResponse.success(state)).length;
        int added = full - empty;
        assertTrue(added < KRYO_BUFFER / 4, "five full folders add " + added + " bytes");
    }
}
