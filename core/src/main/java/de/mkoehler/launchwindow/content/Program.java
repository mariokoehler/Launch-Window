package de.mkoehler.launchwindow.content;

import java.util.List;
import java.util.Objects;

/**
 * <p>One program within a {@link ProgramCategory} - the second level of the
 * four-level containment hierarchy (see {@code design.md} &sect;2.3(c)).
 * Opening a program is a player action that deducts its effective opening
 * cost (see {@code de.mkoehler.launchwindow.rules.ProgramOpening}) from the
 * player's funds and grants ownership of every mission component it
 * requires, each starting at its own baseline reliability plus any
 * reliability transfer it qualifies for (see {@link MissionComponent}).</p>
 *
 * <p>A content-definition type: shared, read-only game data loaded from a
 * content pack (not yet implemented - see {@code design.md} &sect;5's M3),
 * never per-player state, and never sent over the network. Maintenance
 * cost deduction and goal gating are not deducted/checked by any resolver
 * yet - see {@code design.md} &sect;5's M2 status for what each still
 * needs.</p>
 *
 * @param id                          a stable identifier, unique within a
 *                                    {@link ContentCatalog}
 * @param categoryId                  the {@link ProgramCategory#id()} this
 *                                    program belongs to
 * @param openingCost                 the funds cost to open this program
 *                                    before any discount
 * @param maintenanceCost             the recurring per-season upkeep cost
 *                                    once open (not yet deducted by any
 *                                    resolver - a later budget slice's job)
 * @param requiredMissionComponentIds the {@link MissionComponent#id()}s this
 *                                    program's missions need; opening the
 *                                    program grants ownership of each one
 *                                    not already owned
 * @param openingCostDiscounts        discounts this program's opening cost
 *                                    receives from already-owning certain
 *                                    other programs
 * @author Mario Koehler
 */
public record Program(String id, String categoryId, long openingCost, long maintenanceCost,
                       List<String> requiredMissionComponentIds, List<OpeningCostDiscount> openingCostDiscounts) {

    /**
     * Validates every field and defensively copies both lists into
     * unmodifiable views.
     *
     * @throws NullPointerException     if any field is {@code null}
     * @throws IllegalArgumentException if {@code id}/{@code categoryId} is
     *                                   blank, or either cost is negative
     */
    public Program {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(categoryId, "categoryId must not be null");
        Objects.requireNonNull(requiredMissionComponentIds, "requiredMissionComponentIds must not be null");
        Objects.requireNonNull(openingCostDiscounts, "openingCostDiscounts must not be null");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (categoryId.isBlank()) {
            throw new IllegalArgumentException("categoryId must not be blank");
        }
        if (openingCost < 0) {
            throw new IllegalArgumentException("openingCost must not be negative: " + openingCost);
        }
        if (maintenanceCost < 0) {
            throw new IllegalArgumentException("maintenanceCost must not be negative: " + maintenanceCost);
        }
        requiredMissionComponentIds = List.copyOf(requiredMissionComponentIds);
        openingCostDiscounts = List.copyOf(openingCostDiscounts);
    }

    /**
     * One entry of {@link Program#openingCostDiscounts()}: already owning
     * {@link #otherProgramId()} knocks {@link #percent()} off this
     * program's base {@link Program#openingCost()} - the "fast follower"
     * mechanic re-derived from {@code baspm.md} &sect;10.1 (e.g. BASPM's own
     * Explorer I/Sputnik I pair). Every discount a program qualifies for is
     * summed and capped at 100% (see
     * {@code de.mkoehler.launchwindow.rules.ProgramOpening#effectiveOpeningCost}).
     *
     * @param otherProgramId the already-opened program this discount
     *                       triggers on
     * @param percent        the percentage knocked off, 0-100
     */
    public record OpeningCostDiscount(String otherProgramId, int percent) {

        /**
         * Validates both fields.
         *
         * @throws NullPointerException     if {@code otherProgramId} is {@code null}
         * @throws IllegalArgumentException if {@code otherProgramId} is
         *                                   blank, or {@code percent} is
         *                                   outside 0-100
         */
        public OpeningCostDiscount {
            Objects.requireNonNull(otherProgramId, "otherProgramId must not be null");
            if (otherProgramId.isBlank()) {
                throw new IllegalArgumentException("otherProgramId must not be blank");
            }
            if (percent < 0 || percent > 100) {
                throw new IllegalArgumentException("percent must be within 0-100: " + percent);
            }
        }
    }
}
