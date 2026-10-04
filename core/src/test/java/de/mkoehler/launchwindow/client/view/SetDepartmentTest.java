package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.client.ui.Refusals;
import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.ContentPackLoader;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.Building;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSkill;
import de.mkoehler.launchwindow.model.SetSpecialist;
import de.mkoehler.launchwindow.model.TrainingKind;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the SET Centre's view logic: row wording, filters, sort, the summary strip, the Payload
 * R&amp;D board, display names and refusal wording.
 *
 * @author Mario Koehler
 */
class SetDepartmentTest {

    private static final ContentCatalog CATALOG = ContentPackLoader.load(Path.of("..", "assets", "content", "early-era"));

    private static SetSpecialist engineer(String id, int age, double rockets) {
        SetSpecialist engineer = new SetSpecialist(id, "Name " + id, age, 60, 50);
        engineer.setSalary(40.0);
        engineer.setSkill(SetSkill.ROCKETS, rockets);
        engineer.setAssignmentState(AssignmentState.IDLE);
        return engineer;
    }

    /**
     * Each state reads as on the badge.
     */
    @Test
    void stateWording() {
        SetSpecialist idle = engineer("a", 30, 50);
        assertEquals("Idle", SetDepartment.stateText(idle));
        assertFalse(SetDepartment.isActive(idle));

        SetSpecialist advanced = engineer("b", 30, 50);
        advanced.setAssignmentState(AssignmentState.IN_TRAINING);
        advanced.setTrainingKind(TrainingKind.ADVANCED);
        advanced.setTrainingSeasonsTotal(2);
        advanced.setTrainingSeasonsCompleted(1);
        assertEquals("In training · Advanced · 1 left", SetDepartment.stateText(advanced));
        assertTrue(SetDepartment.isActive(advanced));

        SetSpecialist researching = engineer("c", 30, 50);
        researching.setAssignmentState(AssignmentState.ASSIGNED);
        researching.setRdTargetComponentId("vostok-capsule");
        researching.setRdSlot(1);
        assertEquals("Researching · Vostok Capsule · slot 2", SetDepartment.stateText(researching));

        SetSpecialist away = engineer("d", 30, 50);
        away.setUnavailableSeasons(1);
        assertEquals("Unavailable · 1 season", SetDepartment.stateText(away));
        assertTrue(SetDepartment.isBusy(away));
        assertFalse(SetDepartment.isActive(away));
    }

    /**
     * Filters and the skill sort pick and order the rows; the summary counts from the content pack's cap.
     */
    @Test
    void filtersSortAndSummary() {
        PlayerSnapshot state = new PlayerSnapshot("Orion", Archetype.NASA, 0, 0, 0, 1);
        SetSpecialist low = engineer("a", 30, 20);
        SetSpecialist high = engineer("b", 62, 80);
        SetSpecialist away = engineer("c", 40, 50);
        away.setUnavailableSeasons(2);
        state.getSetSpecialists().addAll(List.of(low, high, away));

        assertEquals(List.of(high, away, low),
            SetDepartment.rows(state.getSetSpecialists(), SetDepartment.Filter.ALL, SetSkill.ROCKETS, true));
        assertEquals(List.of(low, high),
            SetDepartment.rows(state.getSetSpecialists(), SetDepartment.Filter.IDLE, SetSkill.ROCKETS, false));
        assertEquals(List.of(away),
            SetDepartment.rows(state.getSetSpecialists(), SetDepartment.Filter.UNAVAILABLE, SetSkill.ROCKETS, true));

        SetDepartment.Summary summary = SetDepartment.summary(state, CATALOG);
        assertEquals(3, summary.headcount());
        assertEquals(CATALOG.buildings().capacity(Building.SET_CENTER, 1), summary.cap());
        assertEquals(1, summary.nearRetirement(), "62 is within 5 years of 65");
        assertEquals(1, summary.busy());
        assertEquals(120.0, summary.totalSalary(), 1e-9);
    }

    /**
     * The board shows owned payload components only, seats researchers in their slots, offers the
     * lowest free slot, and offers none at the R&amp;D ceiling.
     */
    @Test
    void payloadBoard() {
        PlayerSnapshot state = new PlayerSnapshot("Orion", Archetype.NASA, 0, 0, 0, 1);
        double cap = CATALOG.missionComponent("sputnik-2-satellite").orElseThrow().maxRdReliability();
        state.getMissionComponents().put("vostok-capsule", new OwnedMissionComponent(50, 40));
        state.getMissionComponents().put("sputnik-2-satellite", new OwnedMissionComponent(cap, 30));
        state.getMissionComponents().put("r7-sputnik", new OwnedMissionComponent(60, 40));
        SetSpecialist researcher = engineer("a", 30, 50);
        researcher.setAssignmentState(AssignmentState.ASSIGNED);
        researcher.setRdTargetComponentId("vostok-capsule");
        researcher.setRdSlot(0);
        state.getSetSpecialists().add(researcher);
        state.getSetSpecialists().add(engineer("b", 30, 50));

        List<PayloadRnd.Card> cards = PayloadRnd.cards(state, CATALOG);
        assertEquals(List.of("sputnik-2-satellite", "vostok-capsule"),
            cards.stream().map(card -> card.component().id()).toList(), "rockets excluded, pack order");
        PayloadRnd.Card capped = cards.get(0);
        assertTrue(capped.atCap());
        assertEquals(-1, capped.nextFreeSlot());
        PayloadRnd.Card vostok = cards.get(1);
        assertEquals(researcher, vostok.slots().get(0));
        assertNull(vostok.slots().get(1));
        assertEquals(1, vostok.nextFreeSlot());
        assertEquals(1, PayloadRnd.assignable(state, vostok.component()).size(), "only the idle engineer");
    }

    /**
     * Display names come from ids until the pack's strings exist; refusal codes get words, unknown ones stay as codes.
     */
    @Test
    void namesAndRefusals() {
        assertEquals("Sputnik 1 Satellite", ContentNames.of("sputnik-1-satellite"));
        assertEquals("No room. Upgrade the building.", Refusals.text("ROSTER_FULL"));
        assertEquals("SOMETHING_NEW", Refusals.text("SOMETHING_NEW"));
    }
}
