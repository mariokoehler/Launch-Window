package de.mkoehler.launchwindow.content;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * <p>Tests for {@link ProgramCategory}.</p>
 *
 * @author Mario Koehler
 */
class ProgramCategoryTest {

    /**
     * Verifies that a well-formed category keeps both fields as given.
     */
    @Test
    void storesIdAndName() {
        ProgramCategory category = new ProgramCategory("earth-orbit", "Earth Orbit");

        assertEquals("earth-orbit", category.id());
        assertEquals("Earth Orbit", category.name());
    }

    /**
     * Verifies that a blank id is rejected.
     */
    @Test
    void rejectsBlankId() {
        assertThrows(IllegalArgumentException.class, () -> new ProgramCategory(" ", "Earth Orbit"));
    }

    /**
     * Verifies that a blank name is rejected.
     */
    @Test
    void rejectsBlankName() {
        assertThrows(IllegalArgumentException.class, () -> new ProgramCategory("earth-orbit", " "));
    }

    /**
     * Verifies that a {@code null} field is rejected.
     */
    @Test
    void rejectsNullFields() {
        assertThrows(NullPointerException.class, () -> new ProgramCategory(null, "Earth Orbit"));
        assertThrows(NullPointerException.class, () -> new ProgramCategory("earth-orbit", null));
    }
}
