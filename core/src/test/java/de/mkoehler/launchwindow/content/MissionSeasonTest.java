package de.mkoehler.launchwindow.content;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * <p>Tests for {@link MissionSeason}.</p>
 *
 * @author Mario Koehler
 */
class MissionSeasonTest {

    /**
     * Builds a single trivial, valid step.
     *
     * @return the built step
     */
    private static MissionStep trivialStep() {
        return new MissionStep("step-1", List.of(new MissionStep.ComponentInvolvement("rocket-1", 100)), List.of(),
            List.of(), false);
    }

    /**
     * Verifies a well-formed season keeps its steps as given.
     */
    @Test
    void storesSteps() {
        List<MissionStep> steps = List.of(trivialStep());
        MissionSeason season = new MissionSeason(steps);

        assertEquals(steps, season.steps());
    }

    /**
     * Verifies an empty step list is rejected.
     */
    @Test
    void rejectsEmptySteps() {
        assertThrows(IllegalArgumentException.class, () -> new MissionSeason(List.of()));
    }

    /**
     * Verifies the constructor defensively copies the step list: a
     * mutation to the original list after construction does not affect the
     * stored season.
     */
    @Test
    void defensivelyCopiesSteps() {
        List<MissionStep> mutable = new ArrayList<>(List.of(trivialStep()));
        MissionSeason season = new MissionSeason(mutable);

        mutable.add(trivialStep());

        assertEquals(1, season.steps().size());
    }

    /**
     * Verifies two steps sharing an id within the same season are
     * rejected.
     */
    @Test
    void rejectsDuplicateStepId() {
        assertThrows(IllegalArgumentException.class,
            () -> new MissionSeason(List.of(trivialStep(), trivialStep())));
    }
}
