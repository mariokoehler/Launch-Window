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
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.AstronautSkill;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSkill;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for M3 slice 3c: CAPCOM, an astronaut on the ground whose skill feeds the
 * saving throw from the controller side (BASPM's CAPCOM slot, {@code baspm.md}
 * &sect;10.9).
 *
 * @author Mario Koehler
 */
class CapcomTest {

    private static final ProgramCategory CATEGORY = new ProgramCategory("capsules", "Capsules");
    private static final MissionComponent CAPSULE = new MissionComponent("capsule", SetSkill.CREWED_SPACECRAFT,
        3, 90, 80, List.of(1.0, 2.0, 3.0, 4.0), 0, List.of(), 0);
    private static final Program PROGRAM = new Program("capsule-program", "capsules", 0, 0, List.of("capsule"),
        List.of());
    /** One uncrewed step whose saving throw rests on CAPCOM alone. */
    private static final MissionConfiguration CAPCOM_ONLY = new MissionConfiguration("capcom-test",
        "capsule-program",
        List.of(new MissionSeason(List.of(new MissionStep("relay",
            List.of(new MissionStep.ComponentInvolvement("capsule", 100)), List.of(), List.of(), false, 0, 0,
            List.of(), List.of(new MissionStep.CrewInvolvement(AstronautSkill.LEADERSHIP, 100)))))),
        0, 0, 0, 0, List.of(), List.of(), List.of(), 1);
    private static final ContentCatalog CATALOG = TestBuildings.catalogWithoutUpkeep(List.of(CATEGORY),
        List.of(PROGRAM), List.of(CAPCOM_ONLY), List.of(CAPSULE));

    private static PlayerSnapshot player(double capsuleReliability, double capcomLeadership) {
        PlayerSnapshot snapshot = new PlayerSnapshot("Agency", Archetype.NASA, 10_000, 0, 0, 1);
        ProgramOpening.open(snapshot, "capsule-program", CATALOG);
        snapshot.getMissionComponents().get("capsule").setCurrentReliability(capsuleReliability);
        Astronaut capcom = new Astronaut("capcom", "capcom", 30, 70, 50);
        capcom.setSkill(AstronautSkill.LEADERSHIP, capcomLeadership);
        Astronaut other = new Astronaut("other", "other", 30, 70, 50);
        snapshot.setAstronauts(new ArrayList<>(List.of(capcom, other)));
        return snapshot;
    }

    private static LaunchRequest withCapcom(List<String> crew, List<String> capcom) {
        return new LaunchRequest("capcom-test", crew, List.of(), capcom, List.of());
    }

    @Test
    void capcomCountAndAvailabilityAreChecked() {
        PlayerSnapshot snapshot = player(50, 50);

        assertEquals(MissionLaunchOutcome.WRONG_CAPCOM_COUNT,
            MissionLaunch.launch(snapshot, withCapcom(List.of(), List.of()), CATALOG));
        assertEquals(MissionLaunchOutcome.CAPCOM_NOT_AVAILABLE,
            MissionLaunch.launch(snapshot, withCapcom(List.of(), List.of("nobody")), CATALOG));
        snapshot.getAstronauts().get(0).setUnavailableSeasons(1);
        assertEquals(MissionLaunchOutcome.CAPCOM_NOT_AVAILABLE,
            MissionLaunch.launch(snapshot, withCapcom(List.of(), List.of("capcom")), CATALOG));
        snapshot.getAstronauts().get(0).setUnavailableSeasons(0);

        assertEquals(MissionLaunchOutcome.LAUNCHED,
            MissionLaunch.launch(snapshot, withCapcom(List.of(), List.of("capcom")), CATALOG));
        assertEquals(AssignmentState.ASSIGNED, snapshot.getAstronauts().get(0).getAssignmentState());
        assertEquals(List.of("capcom"),
            snapshot.getInProgressMissions().values().iterator().next().getCapcomAstronautIds());
    }

    @Test
    void capcomSkillDrivesTheSavingThrow() {
        int solvedWithSkilledCapcom = solvedCount(100);
        int solvedWithUnskilledCapcom = solvedCount(0);

        assertEquals(0, solvedWithUnskilledCapcom, "a zero-skill CAPCOM can never rescue the step");
        assertTrue(solvedWithSkilledCapcom > 0, "a skilled CAPCOM rescues some failed steps");
    }

    private static int solvedCount(double leadership) {
        int solved = 0;
        // One shared stream: java.util.Random's first draw barely differs across consecutive small seeds.
        Random random = new Random(42);
        for (int trial = 0; trial < 300; trial++) {
            PlayerSnapshot snapshot = player(50, leadership);
            assertEquals(MissionLaunchOutcome.LAUNCHED,
                MissionLaunch.launch(snapshot, withCapcom(List.of(), List.of("capcom")), CATALOG));
            for (ResolutionEvent event : MissionProgress.advanceSeason(snapshot, CATALOG, random,
                    new GoalTally(), 1)) {
                if (event instanceof MissionStepResolved resolved
                        && resolved.outcome() == MissionStepOutcome.MINOR_GLITCH_SOLVED) {
                    solved++;
                }
            }
        }
        return solved;
    }

    @Test
    void capcomIsReleasedAndGrowsTheSkillItUsed() {
        PlayerSnapshot snapshot = player(50, 40);
        MissionLaunch.launch(snapshot, withCapcom(List.of(), List.of("capcom")), CATALOG);

        List<ResolutionEvent> events = MissionProgress.advanceSeason(snapshot, CATALOG, new Random(1),
            new GoalTally(), 1);

        Astronaut capcom = snapshot.getAstronauts().get(0);
        assertEquals(AssignmentState.IDLE, capcom.getAssignmentState());
        assertNull(capcom.getAssignedMissionId());
        assertTrue(capcom.getSkill(AstronautSkill.LEADERSHIP) > 40, "CAPCOM grows the skill it exercised");
        assertTrue(events.stream().anyMatch(event -> event instanceof MissionSkillGrowthApplied growth
            && growth.personnelId().equals("capcom")));
    }

    @Test
    void configurationCountMustMatchCapcomInvolvement() {
        MissionSeason withCapcom = CAPCOM_ONLY.seasons().get(0);
        assertThrows(IllegalArgumentException.class, () -> new MissionConfiguration("bad", "capsule-program",
            List.of(withCapcom), 0, 0, 0, 0, List.of(), List.of(), List.of(), 0));
    }

    @Test
    void controllersAndCapcomShareOneHundredPercent() {
        assertThrows(IllegalArgumentException.class, () -> new MissionStep("over",
            List.of(new MissionStep.ComponentInvolvement("capsule", 100)),
            List.of(new MissionStep.ControllerInvolvement(
                de.mkoehler.launchwindow.model.MissionControlSkill.PROPULSION, 80)),
            List.of(), false, 0, 0, List.of(),
            List.of(new MissionStep.CrewInvolvement(AstronautSkill.LEADERSHIP, 30))));
    }
}
