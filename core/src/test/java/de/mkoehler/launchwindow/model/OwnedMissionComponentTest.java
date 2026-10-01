package de.mkoehler.launchwindow.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * <p>Tests for {@link OwnedMissionComponent}.</p>
 *
 * @author Mario Koehler
 */
class OwnedMissionComponentTest {

    /**
     * Verifies that the all-args constructor stores both fields as given.
     */
    @Test
    void constructorStoresBothFields() {
        OwnedMissionComponent owned = new OwnedMissionComponent(42.5, 17.0);

        assertEquals(42.5, owned.getCurrentReliability());
        assertEquals(17.0, owned.getFloorReliability());
    }

    /**
     * Verifies that both setters update their own field independently.
     */
    @Test
    void settersUpdateTheirOwnField() {
        OwnedMissionComponent owned = new OwnedMissionComponent();

        owned.setCurrentReliability(60.0);
        owned.setFloorReliability(25.0);

        assertEquals(60.0, owned.getCurrentReliability());
        assertEquals(25.0, owned.getFloorReliability());
    }
}
