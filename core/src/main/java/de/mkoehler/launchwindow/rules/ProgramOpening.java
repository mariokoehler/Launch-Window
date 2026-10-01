package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.content.Program;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.PlayerSnapshot;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <p>Opens a program for a player: an M2 rules-engine resolver (see
 * {@code design.md} &sect;5 M2, &sect;2.3(c)/&sect;2.4(c)). Headless, per
 * {@code design.md} &sect;3.10 - no persistence/network/libGDX
 * dependency.</p>
 *
 * <p><b>{@link #open} is a plain headless operation, not tied to any
 * action-submission system</b> - same posture as {@code HiringPool.hire}:
 * there is no player-action dispatch path yet, so this is a method a future
 * action handler will call.</p>
 *
 * <p><b>Composition order for whoever wires this into one season pass
 * (M4):</b> opening a program is a journaled player action, like
 * {@code HiringPool.hire}, so it runs before any season-advance resolver
 * (personnel lifecycle, training progress, hiring-pool refresh). Several
 * opens queued in the same turn apply in journal order, each one computing
 * its own discount/transfer against the result the previous one left
 * behind - there is no single "this turn's" ownership snapshot they all
 * read from independently.</p>
 *
 * <p><b>Within one single {@link #open} call, every newly granted
 * component's reliability transfer is computed against ownership as it
 * stood before this call</b>, never against a sibling component this same
 * call is also granting - otherwise two components required by the same
 * program, one a transfer source for the other, would resolve differently
 * depending on which one happens to be processed first. See this method's
 * own implementation note.</p>
 *
 * <p><b>An already-owned component is never reset or re-granted a
 * transfer</b> by opening a second program that also requires it - its
 * current and floor reliability are left exactly as they were.</p>
 *
 * @author Mario Koehler
 */
public final class ProgramOpening {

    /**
     * Not instantiable; this class only exposes static methods.
     */
    private ProgramOpening() {
    }

    /**
     * Computes what a program would actually cost this player to open
     * right now: its base {@link Program#openingCost()} minus every
     * {@link Program.OpeningCostDiscount} whose
     * {@link Program.OpeningCostDiscount#otherProgramId()} is already
     * open, summed and capped at 100%.
     *
     * @param program         the program to price
     * @param openedProgramIds the player's currently opened program ids
     * @return the effective opening cost
     * @throws NullPointerException if either argument is {@code null}
     */
    public static long effectiveOpeningCost(Program program, List<String> openedProgramIds) {
        Objects.requireNonNull(program, "program must not be null");
        Objects.requireNonNull(openedProgramIds, "openedProgramIds must not be null");

        int totalDiscountPercent = 0;
        for (Program.OpeningCostDiscount discount : program.openingCostDiscounts()) {
            if (openedProgramIds.contains(discount.otherProgramId())) {
                totalDiscountPercent += discount.percent();
            }
        }
        int cappedDiscountPercent = Math.min(100, totalDiscountPercent);
        return program.openingCost() * (100 - cappedDiscountPercent) / 100;
    }

    /**
     * Computes the one-time reliability-transfer bonus a component would
     * receive right now, from every
     * {@link MissionComponent.ReliabilityTransferSource} it has that the
     * player currently owns: each source contributes its own
     * <em>current</em> reliability times that source's transfer
     * percentage, summed and capped at
     * {@link MissionComponent#maxReliabilityTransfer()}.
     *
     * @param target          the component to compute a transfer bonus for
     * @param ownedComponents the player's currently owned components
     * @return the transfer bonus, 0 if the player owns none of its sources
     * @throws NullPointerException if either argument is {@code null}
     */
    public static double reliabilityTransferFor(MissionComponent target,
            Map<String, OwnedMissionComponent> ownedComponents) {
        Objects.requireNonNull(target, "target must not be null");
        Objects.requireNonNull(ownedComponents, "ownedComponents must not be null");

        double total = 0;
        for (MissionComponent.ReliabilityTransferSource source : target.reliabilityTransferSources()) {
            OwnedMissionComponent owned = ownedComponents.get(source.sourceComponentId());
            if (owned != null) {
                total += owned.getCurrentReliability() * source.transferPercent() / 100.0;
            }
        }
        return Math.min(target.maxReliabilityTransfer(), total);
    }

    /**
     * Opens a program for this player: deducts its
     * {@link #effectiveOpeningCost}, marks it open, and grants ownership of
     * every {@link Program#requiredMissionComponentIds()} not already
     * owned, each starting at its baseline reliability plus
     * {@link #reliabilityTransferFor} - both the floor and current
     * reliability start at that same value (see
     * {@link OwnedMissionComponent}'s own class Javadoc on why the floor is
     * transfer-inclusive). Fails without mutating anything if the program
     * id is unknown, already open, or unaffordable.
     *
     * @param snapshot the player snapshot to open the program for; mutated
     *                 in place on success
     * @param programId the program to open
     * @param catalog  the content catalog the program and its components
     *                 are looked up in
     * @return the outcome
     * @throws NullPointerException if any argument is {@code null}
     */
    public static OpenProgramOutcome open(PlayerSnapshot snapshot, String programId, ContentCatalog catalog) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(programId, "programId must not be null");
        Objects.requireNonNull(catalog, "catalog must not be null");

        return catalog.program(programId)
            .map(program -> openKnownProgram(snapshot, program, catalog))
            .orElse(OpenProgramOutcome.UNKNOWN_PROGRAM);
    }

    /**
     * The rest of {@link #open} once the program itself is known to exist
     * in the catalog.
     *
     * @param snapshot the player snapshot to open the program for; mutated
     *                 in place on success
     * @param program  the program to open
     * @param catalog  the content catalog this program's required
     *                 components are looked up in
     * @return the outcome
     */
    private static OpenProgramOutcome openKnownProgram(PlayerSnapshot snapshot, Program program,
            ContentCatalog catalog) {
        List<String> openedProgramIds = snapshot.getOpenedProgramIds();
        if (openedProgramIds.contains(program.id())) {
            return OpenProgramOutcome.ALREADY_OPEN;
        }

        long cost = effectiveOpeningCost(program, openedProgramIds);
        if (snapshot.getFunds() < cost) {
            return OpenProgramOutcome.INSUFFICIENT_FUNDS;
        }

        Map<String, OwnedMissionComponent> ownedBeforeThisOpening = snapshot.getMissionComponents();
        // Computed into a separate map, read only from ownedBeforeThisOpening - never from each
        // other - so two components this same program newly requires, one a transfer source for
        // the other, resolve the same way regardless of which is processed first. Computed before
        // any mutation below, so an unexpected failure here (the orElseThrow is unreachable given
        // a validated ContentCatalog, but still) never leaves the snapshot partially updated.
        Map<String, OwnedMissionComponent> newlyOwned = new LinkedHashMap<>();
        for (String componentId : program.requiredMissionComponentIds()) {
            if (ownedBeforeThisOpening.containsKey(componentId)) {
                continue;
            }
            MissionComponent component = catalog.missionComponent(componentId)
                .orElseThrow(() -> new IllegalStateException(
                    "content catalog inconsistency: Program " + program.id()
                        + " requires unvalidated MissionComponent " + componentId));
            double transfer = reliabilityTransferFor(component, ownedBeforeThisOpening);
            double initialReliability = Math.min(component.maxReliability(), component.baselineReliability() + transfer);
            newlyOwned.put(componentId, new OwnedMissionComponent(initialReliability, initialReliability));
        }

        snapshot.setFunds(snapshot.getFunds() - cost);
        openedProgramIds.add(program.id());
        snapshot.setOpenedProgramIds(openedProgramIds);
        Map<String, OwnedMissionComponent> allOwned = new LinkedHashMap<>(ownedBeforeThisOpening);
        allOwned.putAll(newlyOwned);
        snapshot.setMissionComponents(allOwned);

        return OpenProgramOutcome.OPENED;
    }
}
