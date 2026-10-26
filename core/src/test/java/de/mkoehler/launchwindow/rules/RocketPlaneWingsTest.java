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
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.AstronautSkill;
import de.mkoehler.launchwindow.model.InProgressMission;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSkill;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests rocket-plane wings ({@code design.md} &sect;2.2(c)): earned on a rocket plane that comes home, they add
 * their points to the capsule steps their holder pilots, in the season pass and the planning odds alike.
 *
 * @author Mario Koehler
 */
class RocketPlaneWingsTest {

    private static final MissionComponent PLANE = new MissionComponent("plane", SetSkill.CREWED_SPACECRAFT,
        3, 90, 82, List.of(3.0, 6.0, 9.0, 12.0), 0, List.of(), 0);
    private static final MissionComponent CAPSULE = new MissionComponent("capsule", SetSkill.CREWED_SPACECRAFT,
        3, 90, 82, List.of(3.0, 6.0, 9.0, 12.0), 0, List.of(), 0);
    private static final MissionStep PILOTED = step("01-reentry", "capsule", AstronautSkill.PILOTING);
    private static final MissionStep SCIENCE = step("02-experiments", "capsule", AstronautSkill.SCIENCE);
    private static final MissionStep GLIDE = step("01-landing", "plane", AstronautSkill.PILOTING);
    private static final MissionConfiguration PLANE_FLIGHT = new MissionConfiguration("plane-flight", "plane-program",
        List.of(new MissionSeason(List.of(GLIDE))), 1, 0, 0, 0);
    private static final MissionConfiguration CAPSULE_FLIGHT = new MissionConfiguration("capsule-flight",
        "capsule-program", List.of(new MissionSeason(List.of(PILOTED, SCIENCE))), 1, 0, 0, 0);
    private static final ContentCatalog CATALOG = TestBuildings.catalogWithoutUpkeep(
        List.of(new ProgramCategory(RocketPlaneWings.CATEGORY, "Space planes"),
            new ProgramCategory("one-crew-capsules", "One-crew capsules")),
        List.of(new Program("plane-program", RocketPlaneWings.CATEGORY, 100, 0, List.of("plane"), List.of()),
            new Program("capsule-program", "one-crew-capsules", 100, 0, List.of("capsule"), List.of())),
        List.of(PLANE_FLIGHT, CAPSULE_FLIGHT), List.of(PLANE, CAPSULE));

    private static MissionStep step(String id, String componentId, AstronautSkill skill) {
        return new MissionStep(id, List.of(new MissionStep.ComponentInvolvement(componentId, 100)), List.of(),
            List.of(new MissionStep.CrewInvolvement(0, skill, 100)), true);
    }

    private static PlayerSnapshot player(boolean wings) {
        PlayerSnapshot snapshot = new PlayerSnapshot("Agency", Archetype.NASA, 10_000, 0, 0, 1);
        Astronaut pilot = new Astronaut("a1", "Joe Walker", 30, 80, 50);
        pilot.setRocketPlaneWings(wings);
        snapshot.setAstronauts(new ArrayList<>(List.of(pilot)));
        return snapshot;
    }

    private static InProgressMission flight(MissionConfiguration configuration) {
        return new InProgressMission("m1", configuration.id(), List.of("a1"), List.of());
    }

    @Test
    void aRocketPlaneThatComesHomeGivesItsCrewTheirWingsOnce() {
        PlayerSnapshot snapshot = player(false);
        List<ResolutionEvent> events = new ArrayList<>();

        RocketPlaneWings.award(snapshot, CATALOG, PLANE_FLIGHT, flight(PLANE_FLIGHT), events);
        RocketPlaneWings.award(snapshot, CATALOG, PLANE_FLIGHT, flight(PLANE_FLIGHT), events);

        assertTrue(snapshot.getAstronauts().getFirst().isRocketPlaneWings());
        assertEquals(List.of(new RocketPlaneWingsEarned("m1", "plane-flight", "a1", "Joe Walker")), events);
    }

    @Test
    void aCapsuleFlightGivesNoWings() {
        PlayerSnapshot snapshot = player(false);
        List<ResolutionEvent> events = new ArrayList<>();

        RocketPlaneWings.award(snapshot, CATALOG, CAPSULE_FLIGHT, flight(CAPSULE_FLIGHT), events);

        assertFalse(snapshot.getAstronauts().getFirst().isRocketPlaneWings());
        assertTrue(events.isEmpty());
    }

    @Test
    void wingsAddTheirPointsOnlyToTheCapsuleStepsTheirHolderPilots() {
        assertEquals(RocketPlaneWings.BONUS_POINTS,
            RocketPlaneWings.stepBonus(player(true), CATALOG, flight(CAPSULE_FLIGHT), PILOTED));
        assertEquals(0, RocketPlaneWings.stepBonus(player(true), CATALOG, flight(CAPSULE_FLIGHT), SCIENCE));
        assertEquals(0, RocketPlaneWings.stepBonus(player(false), CATALOG, flight(CAPSULE_FLIGHT), PILOTED));
        // The rocket plane itself is where they're earned, not where they help.
        assertEquals(0, RocketPlaneWings.stepBonus(player(true), CATALOG, flight(PLANE_FLIGHT), GLIDE));
    }

    @Test
    void thePlanningOddsCountTheWings() {
        MissionAssessment.StepOdds with = MissionAssessment.assess(player(true), CATALOG, CAPSULE_FLIGHT,
            List.of("a1"), List.of(), List.of()).steps().getFirst();
        MissionAssessment.StepOdds without = MissionAssessment.assess(player(false), CATALOG, CAPSULE_FLIGHT,
            List.of("a1"), List.of(), List.of()).steps().getFirst();

        assertEquals(RocketPlaneWings.BONUS_POINTS, with.wings());
        assertEquals(0, without.wings());
        assertEquals(RocketPlaneWings.BONUS_POINTS / 100, with.success() - without.success(), 1e-9);
    }
}
