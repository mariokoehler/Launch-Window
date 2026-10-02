package de.mkoehler.launchwindow.content;

import de.mkoehler.launchwindow.model.AstronautSkill;
import de.mkoehler.launchwindow.model.MissionControlSkill;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * <p>Tests for {@link MissionConfiguration}.</p>
 *
 * @author Mario Koehler
 */
class MissionConfigurationTest {

    /**
     * Builds a minimal, valid single-season configuration with the given
     * number of seasons, each holding one trivial, uncrewed, controller-
     * free step.
     *
     * @param seasonCount how many identical seasons to build
     * @return the built season list
     */
    private static List<MissionSeason> seasons(int seasonCount) {
        MissionStep step = new MissionStep("step-1",
            List.of(new MissionStep.ComponentInvolvement("rocket-1", 100)), List.of(), List.of(), false);
        List<MissionSeason> result = new ArrayList<>();
        for (int i = 0; i < seasonCount; i++) {
            result.add(new MissionSeason(List.of(step)));
        }
        return result;
    }

    /**
     * Verifies that a well-formed configuration keeps every field as given.
     */
    @Test
    void storesEveryField() {
        List<MissionSeason> seasons = seasons(1);
        MissionConfiguration configuration =
            new MissionConfiguration("sputnik-1-std", "sputnik-1", seasons, 0, 0, 750);

        assertEquals("sputnik-1-std", configuration.id());
        assertEquals("sputnik-1", configuration.programId());
        assertEquals(seasons, configuration.seasons());
        assertEquals(0, configuration.requiredAstronautCount());
        assertEquals(0, configuration.requiredMissionControlOfficerCount());
        assertEquals(750, configuration.cost());
    }

    /**
     * Verifies a negative cost is rejected.
     */
    @Test
    void rejectsNegativeCost() {
        assertThrows(IllegalArgumentException.class,
            () -> new MissionConfiguration("sputnik-1-std", "sputnik-1", seasons(1), 0, 0, -1));
    }

    /**
     * Verifies a zero cost (a free mission) is accepted.
     */
    @Test
    void acceptsZeroCost() {
        MissionConfiguration configuration =
            new MissionConfiguration("sputnik-1-std", "sputnik-1", seasons(1), 0, 0, 0);

        assertEquals(0, configuration.cost());
    }

    /**
     * Verifies that {@code durationInSeasons} is derived from the season
     * list's own size.
     */
    @Test
    void durationInSeasonsMatchesTheSeasonListSize() {
        MissionConfiguration configuration =
            new MissionConfiguration("sputnik-1-ext", "sputnik-1", seasons(3), 0, 0, 0);

        assertEquals(3, configuration.durationInSeasons());
    }

    /**
     * Verifies that an empty season list is rejected.
     */
    @Test
    void rejectsEmptySeasons() {
        assertThrows(IllegalArgumentException.class,
            () -> new MissionConfiguration("sputnik-1-std", "sputnik-1", List.of(), 0, 0, 0));
    }

    /**
     * Verifies that a blank id or program id is rejected.
     */
    @Test
    void rejectsBlankIds() {
        List<MissionSeason> seasons = seasons(1);
        assertThrows(IllegalArgumentException.class, () -> new MissionConfiguration(" ", "sputnik-1", seasons, 0, 0, 0));
        assertThrows(IllegalArgumentException.class,
            () -> new MissionConfiguration("sputnik-1-std", " ", seasons, 0, 0, 0));
    }

    /**
     * Verifies the constructor defensively copies the season list: a
     * mutation to the original list after construction does not affect the
     * stored configuration.
     */
    @Test
    void defensivelyCopiesSeasons() {
        List<MissionSeason> mutable = new ArrayList<>(seasons(1));
        MissionConfiguration configuration = new MissionConfiguration("sputnik-1-std", "sputnik-1", mutable, 0, 0, 0);

        mutable.add(new MissionSeason(seasons(1).get(0).steps()));

        assertEquals(1, configuration.seasons().size());
    }

    /**
     * Verifies a negative required-astronaut-count is rejected.
     */
    @Test
    void rejectsNegativeRequiredAstronautCount() {
        assertThrows(IllegalArgumentException.class,
            () -> new MissionConfiguration("sputnik-1-std", "sputnik-1", seasons(1), -1, 0, 0));
    }

    /**
     * Verifies a negative required-Mission-Control-officer-count is
     * rejected.
     */
    @Test
    void rejectsNegativeRequiredMissionControlOfficerCount() {
        assertThrows(IllegalArgumentException.class,
            () -> new MissionConfiguration("sputnik-1-std", "sputnik-1", seasons(1), 0, -1, 0));
    }

    /**
     * Verifies a positive required-astronaut-count is rejected when no
     * step actually involves any crew.
     */
    @Test
    void rejectsPositiveRequiredAstronautCountWithNoCrewInvolvement() {
        assertThrows(IllegalArgumentException.class,
            () -> new MissionConfiguration("sputnik-1-std", "sputnik-1", seasons(1), 1, 0, 0));
    }

    /**
     * Verifies a zero required-astronaut-count is rejected when a step
     * does involve crew.
     */
    @Test
    void rejectsZeroRequiredAstronautCountWithCrewInvolvement() {
        MissionStep crewedStep = new MissionStep("step-1",
            List.of(new MissionStep.ComponentInvolvement("rocket-1", 100)), List.of(),
            List.of(new MissionStep.CrewInvolvement(AstronautSkill.PILOTING, 100)), false);
        List<MissionSeason> seasons = List.of(new MissionSeason(List.of(crewedStep)));

        assertThrows(IllegalArgumentException.class,
            () -> new MissionConfiguration("sputnik-1-std", "sputnik-1", seasons, 0, 0, 0));
    }

    /**
     * Verifies a positive required-Mission-Control-officer-count is
     * rejected when no step actually involves any controller.
     */
    @Test
    void rejectsPositiveRequiredMissionControlOfficerCountWithNoControllerInvolvement() {
        assertThrows(IllegalArgumentException.class,
            () -> new MissionConfiguration("sputnik-1-std", "sputnik-1", seasons(1), 0, 1, 0));
    }

    /**
     * Verifies a zero required-Mission-Control-officer-count is rejected
     * when a step does involve a controller.
     */
    @Test
    void rejectsZeroRequiredMissionControlOfficerCountWithControllerInvolvement() {
        MissionStep controlledStep = new MissionStep("step-1",
            List.of(new MissionStep.ComponentInvolvement("rocket-1", 100)),
            List.of(new MissionStep.ControllerInvolvement(MissionControlSkill.PROPULSION, 100)), List.of(), false);
        List<MissionSeason> seasons = List.of(new MissionSeason(List.of(controlledStep)));

        assertThrows(IllegalArgumentException.class,
            () -> new MissionConfiguration("sputnik-1-std", "sputnik-1", seasons, 0, 0, 0));
    }

    /**
     * Verifies a correctly matched positive required-astronaut-count with
     * crew involvement is accepted.
     */
    @Test
    void acceptsMatchedRequiredAstronautCount() {
        MissionStep crewedStep = new MissionStep("step-1",
            List.of(new MissionStep.ComponentInvolvement("rocket-1", 100)), List.of(),
            List.of(new MissionStep.CrewInvolvement(AstronautSkill.PILOTING, 100)), false);
        List<MissionSeason> seasons = List.of(new MissionSeason(List.of(crewedStep)));

        MissionConfiguration configuration = new MissionConfiguration("sputnik-1-std", "sputnik-1", seasons, 2, 0, 0);

        assertEquals(2, configuration.requiredAstronautCount());
    }
}
