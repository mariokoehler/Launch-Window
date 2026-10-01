package de.mkoehler.launchwindow.content;

import de.mkoehler.launchwindow.model.SetSkill;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * <p>Tests for {@link MissionComponent} and its nested
 * {@link MissionComponent.ReliabilityTransferSource}.</p>
 *
 * @author Mario Koehler
 */
class MissionComponentTest {

    private static final List<Double> RD_BANDS = List.of(3.0, 6.0, 9.0, 12.0);

    /**
     * Verifies that a well-formed component keeps every field as given.
     */
    @Test
    void storesEveryField() {
        MissionComponent component =
            new MissionComponent("rocket-1", SetSkill.ROCKETS, 3, 95, 85, RD_BANDS, 50, List.of());

        assertEquals("rocket-1", component.id());
        assertEquals(SetSkill.ROCKETS, component.associatedSkill());
        assertEquals(3, component.baselineReliability());
        assertEquals(95, component.maxReliability());
        assertEquals(85, component.maxRdReliability());
        assertEquals(RD_BANDS, component.rdSlotBandMaxAmounts());
        assertEquals(50, component.maxReliabilityTransfer());
    }

    /**
     * Verifies that a baseline reliability above the max reliability is
     * rejected.
     */
    @Test
    void rejectsBaselineAboveMax() {
        assertThrows(IllegalArgumentException.class,
            () -> new MissionComponent("rocket-1", SetSkill.ROCKETS, 96, 95, 95, RD_BANDS, 50, List.of()));
    }

    /**
     * Verifies that a baseline reliability above the R&amp;D ceiling
     * (but still at or below the absolute max) is rejected.
     */
    @Test
    void rejectsBaselineAboveMaxRd() {
        assertThrows(IllegalArgumentException.class,
            () -> new MissionComponent("rocket-1", SetSkill.ROCKETS, 90, 95, 85, RD_BANDS, 50, List.of()));
    }

    /**
     * Verifies that an R&amp;D ceiling above the absolute max reliability is
     * rejected.
     */
    @Test
    void rejectsMaxRdAboveMax() {
        assertThrows(IllegalArgumentException.class,
            () -> new MissionComponent("rocket-1", SetSkill.ROCKETS, 3, 95, 96, RD_BANDS, 50, List.of()));
    }

    /**
     * Verifies that a reliability-scale field outside 0-100 is rejected.
     */
    @Test
    void rejectsOutOfRangeReliabilityFields() {
        assertThrows(IllegalArgumentException.class,
            () -> new MissionComponent("rocket-1", SetSkill.ROCKETS, -1, 95, 85, RD_BANDS, 50, List.of()));
        assertThrows(IllegalArgumentException.class,
            () -> new MissionComponent("rocket-1", SetSkill.ROCKETS, 3, 101, 85, RD_BANDS, 50, List.of()));
        assertThrows(IllegalArgumentException.class,
            () -> new MissionComponent("rocket-1", SetSkill.ROCKETS, 3, 95, 85, RD_BANDS, 101, List.of()));
    }

    /**
     * Verifies that {@code rdSlotBandMaxAmounts} with other than exactly
     * four entries is rejected.
     */
    @Test
    void rejectsWrongNumberOfRdSlotBands() {
        assertThrows(IllegalArgumentException.class,
            () -> new MissionComponent("rocket-1", SetSkill.ROCKETS, 3, 95, 85, List.of(3.0, 6.0, 9.0), 50,
                List.of()));
        assertThrows(IllegalArgumentException.class,
            () -> new MissionComponent("rocket-1", SetSkill.ROCKETS, 3, 95, 85, List.of(3.0, 6.0, 9.0, 12.0, 15.0),
                50, List.of()));
    }

    /**
     * Verifies that non-ascending {@code rdSlotBandMaxAmounts} are rejected.
     */
    @Test
    void rejectsNonAscendingRdSlotBands() {
        assertThrows(IllegalArgumentException.class,
            () -> new MissionComponent("rocket-1", SetSkill.ROCKETS, 3, 95, 85, List.of(3.0, 9.0, 6.0, 12.0), 50,
                List.of()));
    }

    /**
     * Verifies that a blank id is rejected.
     */
    @Test
    void rejectsBlankId() {
        assertThrows(IllegalArgumentException.class,
            () -> new MissionComponent(" ", SetSkill.ROCKETS, 3, 95, 85, RD_BANDS, 50, List.of()));
    }

    /**
     * Verifies that a well-formed transfer source keeps both fields as
     * given.
     */
    @Test
    void transferSourceStoresBothFields() {
        MissionComponent.ReliabilityTransferSource source =
            new MissionComponent.ReliabilityTransferSource("rocket-redstone", 50);

        assertEquals("rocket-redstone", source.sourceComponentId());
        assertEquals(50, source.transferPercent());
    }

    /**
     * Verifies that a transfer percentage outside 0-100 is rejected.
     */
    @Test
    void transferSourceRejectsOutOfRangePercent() {
        assertThrows(IllegalArgumentException.class,
            () -> new MissionComponent.ReliabilityTransferSource("rocket-redstone", -1));
        assertThrows(IllegalArgumentException.class,
            () -> new MissionComponent.ReliabilityTransferSource("rocket-redstone", 101));
    }

    /**
     * Verifies that a transfer source with a blank source id is rejected.
     */
    @Test
    void transferSourceRejectsBlankSourceId() {
        assertThrows(IllegalArgumentException.class,
            () -> new MissionComponent.ReliabilityTransferSource(" ", 50));
    }

    /**
     * Verifies that {@code reliabilityTransferSources} is copied at
     * construction, so a later mutation of the caller's own list does not
     * change the record's already-built value.
     */
    @Test
    void copiesReliabilityTransferSourcesDefensively() {
        List<MissionComponent.ReliabilityTransferSource> mutable =
            new ArrayList<>(List.of(new MissionComponent.ReliabilityTransferSource("rocket-redstone", 50)));
        MissionComponent component =
            new MissionComponent("rocket-1", SetSkill.ROCKETS, 3, 95, 85, RD_BANDS, 50, mutable);

        mutable.add(new MissionComponent.ReliabilityTransferSource("other", 10));

        assertEquals(1, component.reliabilityTransferSources().size());
    }

    /**
     * Verifies that the record's own {@code reliabilityTransferSources}
     * cannot be mutated through the returned list.
     */
    @Test
    void reliabilityTransferSourcesIsUnmodifiable() {
        MissionComponent component = new MissionComponent("rocket-1", SetSkill.ROCKETS, 3, 95, 85, RD_BANDS, 50,
            List.of(new MissionComponent.ReliabilityTransferSource("rocket-redstone", 50)));

        assertThrows(UnsupportedOperationException.class, () -> component.reliabilityTransferSources()
            .add(new MissionComponent.ReliabilityTransferSource("other", 10)));
    }

    /**
     * Verifies that {@code rdSlotBandMaxAmounts} is copied at construction,
     * so a later mutation of the caller's own list does not change the
     * record's already-built value.
     */
    @Test
    void copiesRdSlotBandMaxAmountsDefensively() {
        List<Double> mutable = new ArrayList<>(RD_BANDS);
        MissionComponent component = new MissionComponent("rocket-1", SetSkill.ROCKETS, 3, 95, 85, mutable, 50,
            List.of());

        mutable.set(0, 999.0);

        assertEquals(RD_BANDS, component.rdSlotBandMaxAmounts());
    }

    /**
     * Verifies that the record's own {@code rdSlotBandMaxAmounts} cannot be
     * mutated through the returned list.
     */
    @Test
    void rdSlotBandMaxAmountsIsUnmodifiable() {
        MissionComponent component =
            new MissionComponent("rocket-1", SetSkill.ROCKETS, 3, 95, 85, RD_BANDS, 50, List.of());

        assertThrows(UnsupportedOperationException.class, () -> component.rdSlotBandMaxAmounts().add(1.0));
    }
}
