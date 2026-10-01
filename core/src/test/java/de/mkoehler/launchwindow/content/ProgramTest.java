package de.mkoehler.launchwindow.content;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * <p>Tests for {@link Program} and its nested
 * {@link Program.OpeningCostDiscount}.</p>
 *
 * @author Mario Koehler
 */
class ProgramTest {

    /**
     * Verifies that a well-formed program keeps every field as given.
     */
    @Test
    void storesEveryField() {
        Program program = new Program("sputnik-1", "earth-orbit", 1_000, 50,
            List.of("rocket-1"), List.of());

        assertEquals("sputnik-1", program.id());
        assertEquals("earth-orbit", program.categoryId());
        assertEquals(1_000, program.openingCost());
        assertEquals(50, program.maintenanceCost());
        assertEquals(List.of("rocket-1"), program.requiredMissionComponentIds());
    }

    /**
     * Verifies that a negative opening cost is rejected.
     */
    @Test
    void rejectsNegativeOpeningCost() {
        assertThrows(IllegalArgumentException.class,
            () -> new Program("sputnik-1", "earth-orbit", -1, 50, List.of(), List.of()));
    }

    /**
     * Verifies that a negative maintenance cost is rejected.
     */
    @Test
    void rejectsNegativeMaintenanceCost() {
        assertThrows(IllegalArgumentException.class,
            () -> new Program("sputnik-1", "earth-orbit", 1_000, -1, List.of(), List.of()));
    }

    /**
     * Verifies that a blank id or category id is rejected.
     */
    @Test
    void rejectsBlankIds() {
        assertThrows(IllegalArgumentException.class,
            () -> new Program(" ", "earth-orbit", 1_000, 50, List.of(), List.of()));
        assertThrows(IllegalArgumentException.class,
            () -> new Program("sputnik-1", " ", 1_000, 50, List.of(), List.of()));
    }

    /**
     * Verifies that {@code requiredMissionComponentIds} is copied at
     * construction, so a later mutation of the caller's own list does not
     * change the record's already-built value.
     */
    @Test
    void copiesRequiredMissionComponentIdsDefensively() {
        List<String> mutable = new ArrayList<>(List.of("rocket-1"));
        Program program = new Program("sputnik-1", "earth-orbit", 1_000, 50, mutable, List.of());

        mutable.add("rocket-2");

        assertEquals(List.of("rocket-1"), program.requiredMissionComponentIds());
    }

    /**
     * Verifies that the record's own {@code requiredMissionComponentIds}
     * cannot be mutated through the returned list.
     */
    @Test
    void requiredMissionComponentIdsIsUnmodifiable() {
        Program program = new Program("sputnik-1", "earth-orbit", 1_000, 50, List.of("rocket-1"), List.of());

        assertThrows(UnsupportedOperationException.class,
            () -> program.requiredMissionComponentIds().add("rocket-2"));
    }

    /**
     * Verifies that {@code openingCostDiscounts} is copied at
     * construction, so a later mutation of the caller's own list does not
     * change the record's already-built value.
     */
    @Test
    void copiesOpeningCostDiscountsDefensively() {
        List<Program.OpeningCostDiscount> mutable =
            new ArrayList<>(List.of(new Program.OpeningCostDiscount("explorer-1", 40)));
        Program program = new Program("sputnik-1", "earth-orbit", 1_000, 50, List.of(), mutable);

        mutable.add(new Program.OpeningCostDiscount("other", 10));

        assertEquals(1, program.openingCostDiscounts().size());
    }

    /**
     * Verifies that a well-formed discount keeps both fields as given.
     */
    @Test
    void discountStoresBothFields() {
        Program.OpeningCostDiscount discount = new Program.OpeningCostDiscount("sputnik-1", 40);

        assertEquals("sputnik-1", discount.otherProgramId());
        assertEquals(40, discount.percent());
    }

    /**
     * Verifies that a discount percentage outside 0-100 is rejected.
     */
    @Test
    void discountRejectsOutOfRangePercent() {
        assertThrows(IllegalArgumentException.class, () -> new Program.OpeningCostDiscount("sputnik-1", -1));
        assertThrows(IllegalArgumentException.class, () -> new Program.OpeningCostDiscount("sputnik-1", 101));
    }

    /**
     * Verifies that a discount with a blank other-program id is rejected.
     */
    @Test
    void discountRejectsBlankOtherProgramId() {
        assertThrows(IllegalArgumentException.class, () -> new Program.OpeningCostDiscount(" ", 40));
    }
}
