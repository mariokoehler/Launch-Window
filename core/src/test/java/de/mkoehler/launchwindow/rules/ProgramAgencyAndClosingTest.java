package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.content.MissionSeason;
import de.mkoehler.launchwindow.content.MissionStep;
import de.mkoehler.launchwindow.content.Program;
import de.mkoehler.launchwindow.content.ProgramCategory;
import de.mkoehler.launchwindow.content.TestBuildings;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSkill;
import de.mkoehler.launchwindow.model.SetSpecialist;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for M3 slice 3a: programs gated by agency, the component creation cost
 * charged on first acquisition, and closing a program (BASPM's
 * {@code Player.CloseProgram}, {@code baspm.md} &sect;10.9).
 *
 * @author Mario Koehler
 */
class ProgramAgencyAndClosingTest {

    private static final ProgramCategory CATEGORY = new ProgramCategory("sats", "Satellites");
    private static final MissionComponent SATELLITE = component("satellite", 750, 75, Archetype.SOVIET);
    private static final MissionComponent SHARED = component("shared-bus", 400, 40, null);
    private static final Program SPUTNIK = new Program("sputnik", "sats", 126, 13, List.of("satellite", "shared-bus"),
        List.of(), List.of(), Archetype.SOVIET);
    private static final Program FOLLOW_UP = new Program("follow-up", "sats", 200, 20, List.of("shared-bus"),
        List.of(), List.of(), Archetype.SOVIET);
    private static final MissionConfiguration FLIGHT = new MissionConfiguration("sputnik-flight", "sputnik",
        List.of(new MissionSeason(List.of(new MissionStep("orbit",
            List.of(new MissionStep.ComponentInvolvement("satellite", 100)), List.of(), List.of(), false)))),
        0, 0, 0);
    private static final ContentCatalog CATALOG = TestBuildings.catalogWithoutUpkeep(List.of(CATEGORY),
        List.of(SPUTNIK, FOLLOW_UP), List.of(FLIGHT), List.of(SATELLITE, SHARED));

    private static MissionComponent component(String id, long creationCost, long maintenance, Archetype agency) {
        return new MissionComponent(id, SetSkill.SPACE_PROBES, 3, 90, 80, List.of(1.0, 2.0, 3.0, 4.0), 0, List.of(),
            maintenance, creationCost, 0, 0, agency, null);
    }

    private static PlayerSnapshot player(Archetype archetype) {
        return new PlayerSnapshot("Agency", archetype, 10_000, 0, 0, 1);
    }

    @Test
    void anotherAgencysProgramIsRefused() {
        PlayerSnapshot nasa = player(Archetype.NASA);

        assertEquals(OpenProgramOutcome.WRONG_AGENCY, ProgramOpening.open(nasa, "sputnik", CATALOG));
        assertEquals(10_000, nasa.getFunds());
        assertTrue(nasa.getMissionComponents().isEmpty());
    }

    @Test
    void openingChargesTheCreationCostOfUnownedComponents() {
        PlayerSnapshot soviet = player(Archetype.SOVIET);

        assertEquals(126 + 750 + 400, ProgramOpening.openingPrice(soviet, SPUTNIK, CATALOG));
        assertEquals(OpenProgramOutcome.OPENED, ProgramOpening.open(soviet, "sputnik", CATALOG));
        assertEquals(10_000 - 1_276, soviet.getFunds());

        // The shared bus is already owned, so the follow-up only pays its opening cost.
        assertEquals(200, ProgramOpening.openingPrice(soviet, FOLLOW_UP, CATALOG));
    }

    @Test
    void closingDeactivatesOnlyComponentsNoOpenProgramNeeds() {
        PlayerSnapshot soviet = player(Archetype.SOVIET);
        ProgramOpening.open(soviet, "sputnik", CATALOG);
        ProgramOpening.open(soviet, "follow-up", CATALOG);
        long upkeepBefore = BudgetProgress.runningCosts(soviet, CATALOG);

        assertEquals(CloseProgramOutcome.CLOSED, ProgramOpening.close(soviet, "sputnik", CATALOG));

        assertFalse(soviet.getOpenedProgramIds().contains("sputnik"));
        assertFalse(soviet.getMissionComponents().get("satellite").isActive());
        assertTrue(soviet.getMissionComponents().get("shared-bus").isActive(), "still used by the follow-up");
        // The program's 13 and the satellite's 75 stop; the bus's 40 and the follow-up's 20 stay.
        assertEquals(upkeepBefore - 13 - 75, BudgetProgress.runningCosts(soviet, CATALOG));
    }

    @Test
    void reopeningReactivatesWithoutCreationCostOrReliabilityReset() {
        PlayerSnapshot soviet = player(Archetype.SOVIET);
        ProgramOpening.open(soviet, "sputnik", CATALOG);
        soviet.getMissionComponents().get("satellite").setCurrentReliability(55);
        ProgramOpening.close(soviet, "sputnik", CATALOG);
        long fundsBefore = soviet.getFunds();

        assertEquals(OpenProgramOutcome.OPENED, ProgramOpening.open(soviet, "sputnik", CATALOG));

        assertEquals(fundsBefore - 126, soviet.getFunds());
        assertTrue(soviet.getMissionComponents().get("satellite").isActive());
        assertEquals(55, soviet.getMissionComponents().get("satellite").getCurrentReliability());
    }

    @Test
    void closingIsRefusedWhileAMissionFlies() {
        PlayerSnapshot soviet = player(Archetype.SOVIET);
        ProgramOpening.open(soviet, "sputnik", CATALOG);
        assertEquals(MissionLaunchOutcome.LAUNCHED,
            MissionLaunch.launch(soviet, "sputnik-flight", List.of(), List.of(), CATALOG));

        assertEquals(CloseProgramOutcome.MISSIONS_IN_FLIGHT, ProgramOpening.close(soviet, "sputnik", CATALOG));
        assertEquals(CloseProgramOutcome.NOT_OPEN, ProgramOpening.close(soviet, "follow-up", CATALOG));
        assertTrue(soviet.getOpenedProgramIds().contains("sputnik"));
    }

    @Test
    void closingReleasesResearchersOfDeactivatedComponents() {
        PlayerSnapshot soviet = player(Archetype.SOVIET);
        ProgramOpening.open(soviet, "sputnik", CATALOG);
        SetSpecialist researcher = new SetSpecialist("s1", "s1", 30, 60, 50);
        researcher.setAssignmentState(AssignmentState.ASSIGNED);
        researcher.setRdTargetComponentId("satellite");
        researcher.setRdSlot(0);
        soviet.setSetSpecialists(List.of(researcher));

        ProgramOpening.close(soviet, "sputnik", CATALOG);

        assertEquals(AssignmentState.IDLE, soviet.getSetSpecialists().get(0).getAssignmentState());
        assertNull(soviet.getSetSpecialists().get(0).getRdTargetComponentId());
    }

    @Test
    void catalogRejectsAProgramGrantingAnotherAgencysComponent() {
        Program nasaProgram = new Program("explorer", "sats", 126, 13, List.of("satellite"), List.of(), List.of(),
            Archetype.NASA);

        assertThrows(IllegalArgumentException.class, () -> new ContentCatalog(List.of(CATEGORY),
            List.of(nasaProgram), List.of(), List.of(SATELLITE)));
    }
}
