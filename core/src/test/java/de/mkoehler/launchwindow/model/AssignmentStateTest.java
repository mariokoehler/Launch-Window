package de.mkoehler.launchwindow.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * <p>Tests for {@link AssignmentState}.</p>
 *
 * @author Mario Koehler
 */
class AssignmentStateTest {

    /**
     * Verifies the enum's value count.
     */
    @Test
    void hasExactlyThreeValues() {
        assertEquals(3, AssignmentState.values().length);
    }

    /**
     * Verifies that a freshly-constructed personnel record starts idle.
     */
    @Test
    void newPersonnelDefaultsToIdle() {
        Astronaut astronaut = new Astronaut("astro-1", "Jane Reyes", 34, 80, 60);

        assertEquals(AssignmentState.IDLE, astronaut.getAssignmentState());
    }
}
