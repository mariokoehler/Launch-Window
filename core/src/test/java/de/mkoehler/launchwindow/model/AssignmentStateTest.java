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
     * Pins the constants' order: Kryo sends enums by ordinal, so new ones are only ever appended
     * (PR_DUTY came last, 2026-10-06).
     */
    @Test
    void constantsKeepTheirOrdinals() {
        assertEquals(java.util.List.of(AssignmentState.IDLE, AssignmentState.ASSIGNED, AssignmentState.IN_TRAINING,
            AssignmentState.PR_DUTY), java.util.List.of(AssignmentState.values()));
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
