package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.content.Program;
import de.mkoehler.launchwindow.content.ProgramCategory;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSkill;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>Tests for {@link ProgramOpening}.</p>
 *
 * @author Mario Koehler
 */
class ProgramOpeningTest {

    private final ProgramCategory earlyRockets = new ProgramCategory("early-rockets", "Early Rockets");

    private final MissionComponent redstone =
        new MissionComponent("rocket-redstone", SetSkill.ROCKETS, 3, 95, 50, List.of());
    private final MissionComponent jupiterC = new MissionComponent("rocket-jupiter-c", SetSkill.ROCKETS, 3, 95, 60,
        List.of(new MissionComponent.ReliabilityTransferSource("rocket-redstone", 50)));

    private final Program redstoneProgram =
        new Program("redstone-program", "early-rockets", 1_000, 0, List.of("rocket-redstone"), List.of());
    private final Program jupiterCProgram = new Program("jupiter-c-program", "early-rockets", 1_500, 0,
        List.of("rocket-jupiter-c"), List.of(new Program.OpeningCostDiscount("redstone-program", 40)));
    private final Program comboProgram = new Program("combo-program", "early-rockets", 2_000, 0,
        List.of("rocket-redstone", "rocket-jupiter-c"), List.of());

    private final MissionComponent overcapped = new MissionComponent("rocket-overcapped", SetSkill.ROCKETS, 90, 95, 100,
        List.of(new MissionComponent.ReliabilityTransferSource("rocket-source-full", 100)));
    private final MissionComponent sourceFull =
        new MissionComponent("rocket-source-full", SetSkill.ROCKETS, 3, 100, 0, List.of());
    private final Program overcappedProgram =
        new Program("overcapped-program", "early-rockets", 1_000, 0, List.of("rocket-overcapped"), List.of());

    private final ContentCatalog catalog = new ContentCatalog(
        List.of(earlyRockets), List.of(redstoneProgram, jupiterCProgram, comboProgram, overcappedProgram), List.of(),
        List.of(redstone, jupiterC, overcapped, sourceFull));

    /**
     * Creates a player snapshot with the given funds and no programs/
     * components owned yet.
     *
     * @param funds the starting funds
     * @return the new snapshot
     */
    private PlayerSnapshot snapshotWithFunds(long funds) {
        return new PlayerSnapshot("Hanseatic Space Agency", Archetype.NASA, funds, 0, 0, 1);
    }

    /**
     * Verifies that a program's opening cost is unchanged when none of its
     * discounts currently apply.
     */
    @Test
    void effectiveOpeningCostWithNoQualifyingDiscount() {
        assertEquals(1_500, ProgramOpening.effectiveOpeningCost(jupiterCProgram, List.of()));
    }

    /**
     * Verifies that a qualifying discount reduces the opening cost by
     * exactly its percentage.
     */
    @Test
    void effectiveOpeningCostWithOneQualifyingDiscount() {
        assertEquals(900, ProgramOpening.effectiveOpeningCost(jupiterCProgram, List.of("redstone-program")));
    }

    /**
     * Verifies that several qualifying discounts sum, capped at 100%.
     */
    @Test
    void effectiveOpeningCostSumsDiscountsCappedAt100Percent() {
        Program heavilyDiscounted = new Program("heavily-discounted", "early-rockets", 1_000, 0, List.of(),
            List.of(new Program.OpeningCostDiscount("a", 60), new Program.OpeningCostDiscount("b", 70)));

        assertEquals(0, ProgramOpening.effectiveOpeningCost(heavilyDiscounted, List.of("a", "b")));
    }

    /**
     * Verifies a non-evenly-dividing discount percentage, to catch an
     * integer-division mistake.
     */
    @Test
    void effectiveOpeningCostRoundsDownOnNonEvenDivision() {
        Program oddCost = new Program("odd-cost", "early-rockets", 1_000, 0, List.of(),
            List.of(new Program.OpeningCostDiscount("a", 33)));

        // 1000 * (100 - 33) / 100 = 670.
        assertEquals(670, ProgramOpening.effectiveOpeningCost(oddCost, List.of("a")));
    }

    /**
     * Verifies that a reliability transfer is 0 when none of a component's
     * transfer sources are owned.
     */
    @Test
    void reliabilityTransferWithNoOwnedSources() {
        assertEquals(0, ProgramOpening.reliabilityTransferFor(jupiterC, Map.of()));
    }

    /**
     * Verifies the transfer formula for one owned source: the source's own
     * current reliability times its transfer percentage.
     */
    @Test
    void reliabilityTransferFromOneOwnedSource() {
        Map<String, OwnedMissionComponent> owned = Map.of("rocket-redstone", new OwnedMissionComponent(40, 3));

        // 40 * 50% = 20.
        assertEquals(20, ProgramOpening.reliabilityTransferFor(jupiterC, owned));
    }

    /**
     * Verifies that the combined transfer from several owned sources is
     * capped at the target's own {@code maxReliabilityTransfer}.
     */
    @Test
    void reliabilityTransferCappedAtMaximum() {
        MissionComponent heavilyTransferred = new MissionComponent("rocket-target", SetSkill.ROCKETS, 3, 95, 60,
            List.of(new MissionComponent.ReliabilityTransferSource("source-a", 60),
                new MissionComponent.ReliabilityTransferSource("source-b", 60)));
        Map<String, OwnedMissionComponent> owned = Map.of(
            "source-a", new OwnedMissionComponent(90, 3),
            "source-b", new OwnedMissionComponent(90, 3));

        // 90*60% + 90*60% = 108, capped at 60.
        assertEquals(60, ProgramOpening.reliabilityTransferFor(heavilyTransferred, owned));
    }

    /**
     * Verifies the full happy path: funds deducted by the effective cost,
     * the program marked open, and its required component granted at
     * baseline reliability with no transfer (no related component owned
     * yet).
     */
    @Test
    void openDeductsCostAndGrantsRequiredComponents() {
        PlayerSnapshot snapshot = snapshotWithFunds(5_000);

        OpenProgramOutcome outcome = ProgramOpening.open(snapshot, "redstone-program", catalog);

        assertEquals(OpenProgramOutcome.OPENED, outcome);
        assertEquals(4_000, snapshot.getFunds());
        assertTrue(snapshot.getOpenedProgramIds().contains("redstone-program"));
        OwnedMissionComponent owned = snapshot.getMissionComponents().get("rocket-redstone");
        assertEquals(3, owned.getCurrentReliability());
        assertEquals(3, owned.getFloorReliability());
    }

    /**
     * Verifies that opening a program qualifying for a discount actually
     * deducts the discounted cost, not the base cost.
     */
    @Test
    void openAppliesAQualifyingDiscount() {
        PlayerSnapshot snapshot = snapshotWithFunds(5_000);
        ProgramOpening.open(snapshot, "redstone-program", catalog);

        OpenProgramOutcome outcome = ProgramOpening.open(snapshot, "jupiter-c-program", catalog);

        assertEquals(OpenProgramOutcome.OPENED, outcome);
        // 5000 - 1000 (redstone) - 900 (jupiter-c discounted from 1500) = 3100.
        assertEquals(3_100, snapshot.getFunds());
    }

    /**
     * Verifies that opening a program granting a component related to an
     * already-owned one applies the reliability transfer.
     */
    @Test
    void openAppliesReliabilityTransferFromAnAlreadyOwnedComponent() {
        PlayerSnapshot snapshot = snapshotWithFunds(5_000);
        ProgramOpening.open(snapshot, "redstone-program", catalog);
        // Simulate redstone having since grown past its baseline.
        snapshot.getMissionComponents().get("rocket-redstone").setCurrentReliability(40);

        ProgramOpening.open(snapshot, "jupiter-c-program", catalog);

        OwnedMissionComponent jupiterCOwned = snapshot.getMissionComponents().get("rocket-jupiter-c");
        // baseline 3 + (40 * 50%) = 23.
        assertEquals(23, jupiterCOwned.getCurrentReliability());
        assertEquals(23, jupiterCOwned.getFloorReliability());
    }

    /**
     * Verifies that an already-owned component is left completely
     * untouched (not reset, not re-granted a transfer) when a second
     * program that also requires it is opened.
     */
    @Test
    void openNeverResetsAnAlreadyOwnedComponent() {
        PlayerSnapshot snapshot = snapshotWithFunds(10_000);
        ProgramOpening.open(snapshot, "redstone-program", catalog);
        snapshot.getMissionComponents().get("rocket-redstone").setCurrentReliability(40);

        ProgramOpening.open(snapshot, "combo-program", catalog);

        OwnedMissionComponent redstoneOwned = snapshot.getMissionComponents().get("rocket-redstone");
        assertEquals(40, redstoneOwned.getCurrentReliability(), "an already-owned component must not be reset");
        assertEquals(3, redstoneOwned.getFloorReliability(), "its original floor must not change either");
    }

    /**
     * Verifies that two components a single program newly requires, one a
     * transfer source for the other, do not see each other: the transfer
     * is computed against ownership as it stood <em>before</em> this call,
     * never against a sibling component this same call is also granting.
     */
    @Test
    void openComputesTransfersWithinOneCallAgainstPreOpeningOwnershipOnly() {
        PlayerSnapshot snapshot = snapshotWithFunds(10_000);

        ProgramOpening.open(snapshot, "combo-program", catalog);

        OwnedMissionComponent jupiterCOwned = snapshot.getMissionComponents().get("rocket-jupiter-c");
        assertEquals(3, jupiterCOwned.getCurrentReliability(),
            "jupiter-c must get no transfer from redstone, since redstone was not owned before this same call");
    }

    /**
     * Verifies that an unknown program id is rejected without mutating the
     * snapshot.
     */
    @Test
    void openRejectsUnknownProgram() {
        PlayerSnapshot snapshot = snapshotWithFunds(5_000);

        OpenProgramOutcome outcome = ProgramOpening.open(snapshot, "unknown-program", catalog);

        assertEquals(OpenProgramOutcome.UNKNOWN_PROGRAM, outcome);
        assertEquals(5_000, snapshot.getFunds());
    }

    /**
     * Verifies that opening the same program twice is rejected the second
     * time, without a second deduction.
     */
    @Test
    void openRejectsAnAlreadyOpenProgram() {
        PlayerSnapshot snapshot = snapshotWithFunds(5_000);
        ProgramOpening.open(snapshot, "redstone-program", catalog);

        OpenProgramOutcome outcome = ProgramOpening.open(snapshot, "redstone-program", catalog);

        assertEquals(OpenProgramOutcome.ALREADY_OPEN, outcome);
        assertEquals(4_000, snapshot.getFunds(), "a second open must not deduct funds again");
    }

    /**
     * Verifies that insufficient funds reject the open without mutating
     * the snapshot.
     */
    @Test
    void openRejectsInsufficientFunds() {
        PlayerSnapshot snapshot = snapshotWithFunds(500);

        OpenProgramOutcome outcome = ProgramOpening.open(snapshot, "redstone-program", catalog);

        assertEquals(OpenProgramOutcome.INSUFFICIENT_FUNDS, outcome);
        assertEquals(500, snapshot.getFunds());
        assertFalse(snapshot.getOpenedProgramIds().contains("redstone-program"));
        assertTrue(snapshot.getMissionComponents().isEmpty());
    }

    /**
     * Verifies that a component's initial reliability (baseline plus
     * transfer) is clamped at its own {@code maxReliability}, even when
     * the uncapped sum would exceed it.
     */
    @Test
    void openClampsInitialReliabilityAtComponentMax() {
        PlayerSnapshot snapshot = snapshotWithFunds(5_000);
        snapshot.getMissionComponents().put("rocket-source-full", new OwnedMissionComponent(100, 3));

        ProgramOpening.open(snapshot, "overcapped-program", catalog);

        // baseline 90 + (100 * 100%) = 190, uncapped - must clamp to maxReliability 95.
        OwnedMissionComponent owned = snapshot.getMissionComponents().get("rocket-overcapped");
        assertEquals(95, owned.getCurrentReliability());
        assertEquals(95, owned.getFloorReliability());
    }
}
