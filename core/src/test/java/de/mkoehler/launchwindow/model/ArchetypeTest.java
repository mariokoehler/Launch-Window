package de.mkoehler.launchwindow.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * <p>Tests for {@link Archetype}.</p>
 *
 * @author Mario Koehler
 */
class ArchetypeTest {

    /**
     * Verifies the enum's exact value set and ordering.
     */
    @Test
    void hasExactlyTwoValuesAndNoGsaEquivalent() {
        assertArrayEquals(new Archetype[] {Archetype.NASA, Archetype.SOVIET}, Archetype.values());
    }
}
