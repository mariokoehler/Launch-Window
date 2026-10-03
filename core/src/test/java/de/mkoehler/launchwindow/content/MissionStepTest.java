package de.mkoehler.launchwindow.content;

import de.mkoehler.launchwindow.model.AstronautSkill;
import de.mkoehler.launchwindow.model.MissionControlSkill;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>Tests for {@link MissionStep}.</p>
 *
 * @author Mario Koehler
 */
class MissionStepTest {

    /**
     * Verifies a well-formed step keeps every field as given.
     */
    @Test
    void storesEveryField() {
        List<MissionStep.ComponentInvolvement> components =
            List.of(new MissionStep.ComponentInvolvement("rocket-1", 70),
                new MissionStep.ComponentInvolvement("payload-1", 30));
        List<MissionStep.ControllerInvolvement> controllers =
            List.of(new MissionStep.ControllerInvolvement(0, 50));
        List<MissionStep.CrewInvolvement> crew =
            List.of(new MissionStep.CrewInvolvement(0, AstronautSkill.PILOTING, 70),
                new MissionStep.CrewInvolvement(0, AstronautSkill.LEADERSHIP, 30));

        MissionStep step = new MissionStep("ascent", components, controllers, crew, true);

        assertEquals("ascent", step.id());
        assertEquals(components, step.componentInvolvements());
        assertEquals(controllers, step.controllerInvolvements());
        assertEquals(crew, step.crewInvolvements());
        assertTrue(step.crewRisk());
    }

    /**
     * Verifies a blank id is rejected.
     */
    @Test
    void rejectsBlankId() {
        List<MissionStep.ComponentInvolvement> components =
            List.of(new MissionStep.ComponentInvolvement("rocket-1", 100));

        assertThrows(IllegalArgumentException.class,
            () -> new MissionStep(" ", components, List.of(), List.of(), false));
    }

    /**
     * Verifies an empty componentInvolvements list is rejected - a step
     * with no hardware involved at all has no meaningful base probability.
     */
    @Test
    void rejectsEmptyComponentInvolvements() {
        assertThrows(IllegalArgumentException.class,
            () -> new MissionStep("ascent", List.of(), List.of(), List.of(), false));
    }

    /**
     * Verifies componentInvolvements percentages summing to less than 100
     * are rejected.
     */
    @Test
    void rejectsComponentPercentagesSummingBelow100() {
        List<MissionStep.ComponentInvolvement> components =
            List.of(new MissionStep.ComponentInvolvement("rocket-1", 60));

        assertThrows(IllegalArgumentException.class,
            () -> new MissionStep("ascent", components, List.of(), List.of(), false));
    }

    /**
     * Verifies componentInvolvements percentages summing to more than 100
     * are rejected.
     */
    @Test
    void rejectsComponentPercentagesSummingAbove100() {
        List<MissionStep.ComponentInvolvement> components =
            List.of(new MissionStep.ComponentInvolvement("rocket-1", 70),
                new MissionStep.ComponentInvolvement("payload-1", 40));

        assertThrows(IllegalArgumentException.class,
            () -> new MissionStep("ascent", components, List.of(), List.of(), false));
    }

    /**
     * Verifies controllerInvolvements percentages summing past 100 are
     * rejected, even though component percentages are exactly 100.
     */
    @Test
    void rejectsControllerPercentagesSummingPast100() {
        List<MissionStep.ComponentInvolvement> components =
            List.of(new MissionStep.ComponentInvolvement("rocket-1", 100));
        List<MissionStep.ControllerInvolvement> controllers =
            List.of(new MissionStep.ControllerInvolvement(0, 60),
                new MissionStep.ControllerInvolvement(4, 50));

        assertThrows(IllegalArgumentException.class,
            () -> new MissionStep("ascent", components, controllers, List.of(), false));
    }

    /**
     * Verifies crewInvolvements percentages summing past 100 are rejected.
     */
    @Test
    void rejectsCrewPercentagesSummingPast100() {
        List<MissionStep.ComponentInvolvement> components =
            List.of(new MissionStep.ComponentInvolvement("rocket-1", 100));
        List<MissionStep.CrewInvolvement> crew =
            List.of(new MissionStep.CrewInvolvement(0, AstronautSkill.PILOTING, 60),
                new MissionStep.CrewInvolvement(0, AstronautSkill.LEADERSHIP, 50));

        assertThrows(IllegalArgumentException.class,
            () -> new MissionStep("ascent", components, List.of(), crew, false));
    }

    /**
     * Verifies a non-empty crewInvolvements summing to less than 100 is
     * rejected - unlike controllerInvolvements, crew involvement is
     * either absent (empty list) or fully accounted for (sum of exactly
     * 100), never partial, so a partial sum cannot be silently read as a
     * skill shortfall by {@link de.mkoehler.launchwindow.rules.MissionStepResolution}'s
     * weighted-average blend.
     */
    @Test
    void rejectsNonEmptyCrewPercentagesSummingBelow100() {
        List<MissionStep.ComponentInvolvement> components =
            List.of(new MissionStep.ComponentInvolvement("rocket-1", 100));
        List<MissionStep.CrewInvolvement> crew = List.of(new MissionStep.CrewInvolvement(0, AstronautSkill.PILOTING, 40));

        assertThrows(IllegalArgumentException.class,
            () -> new MissionStep("ascent", components, List.of(), crew, false));
    }

    /**
     * Verifies a duplicate component id within componentInvolvements is
     * rejected.
     */
    @Test
    void rejectsDuplicateComponentId() {
        List<MissionStep.ComponentInvolvement> components =
            List.of(new MissionStep.ComponentInvolvement("rocket-1", 50),
                new MissionStep.ComponentInvolvement("rocket-1", 50));

        assertThrows(IllegalArgumentException.class,
            () -> new MissionStep("ascent", components, List.of(), List.of(), false));
    }

    /**
     * Verifies a duplicate skill within crewInvolvements is rejected.
     */
    @Test
    void rejectsDuplicateCrewSkill() {
        List<MissionStep.ComponentInvolvement> components =
            List.of(new MissionStep.ComponentInvolvement("rocket-1", 100));
        List<MissionStep.CrewInvolvement> crew =
            List.of(new MissionStep.CrewInvolvement(0, AstronautSkill.PILOTING, 20),
                new MissionStep.CrewInvolvement(0, AstronautSkill.PILOTING, 20));

        assertThrows(IllegalArgumentException.class,
            () -> new MissionStep("ascent", components, List.of(), crew, false));
    }

    /**
     * Verifies a crew-risk step with no crew involvement at all is
     * rejected - a worst-tier failure with nobody aboard to lose makes no
     * sense.
     */
    @Test
    void rejectsCrewRiskWithNoCrewInvolvement() {
        List<MissionStep.ComponentInvolvement> components =
            List.of(new MissionStep.ComponentInvolvement("rocket-1", 100));

        assertThrows(IllegalArgumentException.class,
            () -> new MissionStep("ascent", components, List.of(), List.of(), true));
    }

    /**
     * Verifies an empty controllerInvolvements/crewInvolvements is allowed
     * (an uncrewed step with no distinguished controller involvement),
     * as long as crewRisk is false.
     */
    @Test
    void allowsEmptyControllerAndCrewInvolvements() {
        List<MissionStep.ComponentInvolvement> components =
            List.of(new MissionStep.ComponentInvolvement("rocket-1", 100));

        MissionStep step = new MissionStep("ascent", components, List.of(), List.of(), false);

        assertEquals(List.of(), step.controllerInvolvements());
        assertEquals(List.of(), step.crewInvolvements());
    }

    /**
     * Verifies the constructor defensively copies all three involvement
     * lists: a mutation to an original list after construction does not
     * affect the stored step.
     */
    @Test
    void defensivelyCopiesInvolvementLists() {
        List<MissionStep.ComponentInvolvement> components =
            new ArrayList<>(List.of(new MissionStep.ComponentInvolvement("rocket-1", 100)));
        List<MissionStep.ControllerInvolvement> controllers = new ArrayList<>();
        List<MissionStep.CrewInvolvement> crew = new ArrayList<>();

        MissionStep step = new MissionStep("ascent", components, controllers, crew, false);

        components.add(new MissionStep.ComponentInvolvement("payload-1", 1));
        controllers.add(new MissionStep.ControllerInvolvement(0, 10));
        crew.add(new MissionStep.CrewInvolvement(0, AstronautSkill.PILOTING, 10));

        assertEquals(1, step.componentInvolvements().size());
        assertEquals(0, step.controllerInvolvements().size());
        assertEquals(0, step.crewInvolvements().size());
    }

    /**
     * Verifies a percentage outside 0-100 on any involvement kind is
     * rejected.
     */
    @Test
    void rejectsOutOfRangePercentages() {
        assertThrows(IllegalArgumentException.class, () -> new MissionStep.ComponentInvolvement("rocket-1", 101));
        assertThrows(IllegalArgumentException.class, () -> new MissionStep.ComponentInvolvement("rocket-1", 0));
        assertThrows(IllegalArgumentException.class, () -> new MissionStep.ComponentInvolvement("rocket-1", -1));
        assertThrows(IllegalArgumentException.class,
            () -> new MissionStep.ControllerInvolvement(0, 101));
        assertThrows(IllegalArgumentException.class,
            () -> new MissionStep.ControllerInvolvement(0, 0));
        assertThrows(IllegalArgumentException.class,
            () -> new MissionStep.CrewInvolvement(0, AstronautSkill.PILOTING, 0));
        assertThrows(IllegalArgumentException.class,
            () -> new MissionStep.CrewInvolvement(0, AstronautSkill.PILOTING, -1));
    }
}
