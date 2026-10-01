package de.mkoehler.launchwindow.content;

import de.mkoehler.launchwindow.model.SetSkill;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>Tests for {@link ContentCatalog}.</p>
 *
 * @author Mario Koehler
 */
class ContentCatalogTest {

    private final ProgramCategory earthOrbit = new ProgramCategory("earth-orbit", "Earth Orbit");
    private final MissionComponent rocket =
        new MissionComponent("rocket-1", SetSkill.ROCKETS, 3, 95, 50, List.of());
    private final Program sputnik =
        new Program("sputnik-1", "earth-orbit", 1_000, 50, List.of("rocket-1"), List.of());
    private final MissionConfiguration sputnikStandard =
        new MissionConfiguration("sputnik-1-std", "sputnik-1", 1);

    /**
     * Verifies that a well-formed catalog looks up every definition it was
     * built from.
     */
    @Test
    void looksUpEveryDefinition() {
        ContentCatalog catalog = new ContentCatalog(
            List.of(earthOrbit), List.of(sputnik), List.of(sputnikStandard), List.of(rocket));

        assertEquals(earthOrbit, catalog.category("earth-orbit").orElseThrow());
        assertEquals(sputnik, catalog.program("sputnik-1").orElseThrow());
        assertEquals(sputnikStandard, catalog.missionConfiguration("sputnik-1-std").orElseThrow());
        assertEquals(rocket, catalog.missionComponent("rocket-1").orElseThrow());
    }

    /**
     * Verifies that an unknown id looks up empty rather than throwing.
     */
    @Test
    void unknownIdLooksUpEmpty() {
        ContentCatalog catalog = new ContentCatalog(
            List.of(earthOrbit), List.of(sputnik), List.of(sputnikStandard), List.of(rocket));

        assertTrue(catalog.program("unknown").isEmpty());
    }

    /**
     * Verifies that two programs sharing an id are rejected.
     */
    @Test
    void rejectsDuplicateProgramId() {
        Program duplicate = new Program("sputnik-1", "earth-orbit", 2_000, 0, List.of(), List.of());

        assertThrows(IllegalArgumentException.class, () -> new ContentCatalog(
            List.of(earthOrbit), List.of(sputnik, duplicate), List.of(), List.of(rocket)));
    }

    /**
     * Verifies that a program referencing an unknown category is rejected.
     */
    @Test
    void rejectsProgramWithUnknownCategory() {
        assertThrows(IllegalArgumentException.class, () -> new ContentCatalog(
            List.of(), List.of(sputnik), List.of(), List.of(rocket)));
    }

    /**
     * Verifies that a program requiring an unknown mission component is
     * rejected.
     */
    @Test
    void rejectsProgramWithUnknownRequiredComponent() {
        assertThrows(IllegalArgumentException.class, () -> new ContentCatalog(
            List.of(earthOrbit), List.of(sputnik), List.of(), List.of()));
    }

    /**
     * Verifies that a program whose opening-cost discount references an
     * unknown program is rejected.
     */
    @Test
    void rejectsProgramWithUnknownDiscountTarget() {
        Program explorer = new Program("explorer-1", "earth-orbit", 1_000, 0, List.of(),
            List.of(new Program.OpeningCostDiscount("unknown-program", 40)));

        assertThrows(IllegalArgumentException.class, () -> new ContentCatalog(
            List.of(earthOrbit), List.of(explorer), List.of(), List.of()));
    }

    /**
     * Verifies that a program whose opening-cost discount references
     * itself is rejected.
     */
    @Test
    void rejectsProgramDiscountReferencingItself() {
        Program selfDiscounting = new Program("sputnik-1", "earth-orbit", 1_000, 0, List.of(),
            List.of(new Program.OpeningCostDiscount("sputnik-1", 40)));

        assertThrows(IllegalArgumentException.class, () -> new ContentCatalog(
            List.of(earthOrbit), List.of(selfDiscounting), List.of(), List.of()));
    }

    /**
     * Verifies that a mission configuration referencing an unknown program
     * is rejected.
     */
    @Test
    void rejectsMissionConfigurationWithUnknownProgram() {
        MissionConfiguration orphan = new MissionConfiguration("orphan-std", "unknown-program", 1);

        assertThrows(IllegalArgumentException.class, () -> new ContentCatalog(
            List.of(earthOrbit), List.of(sputnik), List.of(orphan), List.of(rocket)));
    }

    /**
     * Verifies that a mission component whose transfer source references
     * an unknown component is rejected.
     */
    @Test
    void rejectsComponentWithUnknownTransferSource() {
        MissionComponent withBadSource = new MissionComponent("rocket-2", SetSkill.ROCKETS, 3, 95, 50,
            List.of(new MissionComponent.ReliabilityTransferSource("unknown-component", 50)));

        assertThrows(IllegalArgumentException.class, () -> new ContentCatalog(
            List.of(), List.of(), List.of(), List.of(withBadSource)));
    }

    /**
     * Verifies that a mission component whose transfer source references
     * itself is rejected.
     */
    @Test
    void rejectsComponentTransferSourceReferencingItself() {
        MissionComponent selfTransferring = new MissionComponent("rocket-1", SetSkill.ROCKETS, 3, 95, 50,
            List.of(new MissionComponent.ReliabilityTransferSource("rocket-1", 50)));

        assertThrows(IllegalArgumentException.class, () -> new ContentCatalog(
            List.of(), List.of(), List.of(), List.of(selfTransferring)));
    }
}
