package de.mkoehler.launchwindow.content;

import de.mkoehler.launchwindow.model.SetSkill;

import java.util.List;
import java.util.Objects;

/**
 * <p>One mission component (a rocket, capsule, or similar) a {@link Program}
 * requires - the fourth level of the four-level containment hierarchy (see
 * {@code design.md} &sect;2.3(c)). Every mission component starts at
 * {@link #baselineReliability()}, boosted once by any
 * {@link #reliabilityTransferSources()} it qualifies for when a program
 * first grants ownership of it (see {@code de.mkoehler.launchwindow.rules.ProgramOpening})
 * - re-derived from {@code baspm.md} &sect;10.1/&sect;10.3's "everything
 * starts nearly unflyable" shape, with Launch Window's own numbers, not
 * BASPM's.</p>
 *
 * <p>A content-definition type: shared, read-only game data loaded from a
 * content pack (not yet implemented - see {@code design.md} &sect;5's M3),
 * never per-player state, and never sent over the network. Post-mission
 * reliability change (component wear/growth from actually flying) is not
 * implemented by any resolver yet - see {@code design.md} &sect;5's M2
 * status for what still needs it.</p>
 *
 * @param id                         a stable identifier, unique within a
 *                                   {@link ContentCatalog}
 * @param associatedSkill            the SET skill whose R&amp;D work grows
 *                                   this component's reliability - see
 *                                   {@code de.mkoehler.launchwindow.rules.ResearchProgress}
 * @param baselineReliability        the reliability (0-100) a freshly
 *                                   granted, untransferred component starts at
 * @param maxReliability             the reliability (0-100) ceiling this
 *                                   component can never exceed
 * @param maxReliabilityTransfer     the reliability-transfer cap (0-100):
 *                                   however many qualifying
 *                                   {@link #reliabilityTransferSources()} a
 *                                   player owns, their combined bonus never
 *                                   exceeds this
 * @param reliabilityTransferSources the curated list of other components
 *                                   that grant this one a one-time starting
 *                                   reliability bonus when owned - see
 *                                   {@code design.md} &sect;2.4(c)'s decision
 *                                   to keep this an explicit per-pair table,
 *                                   not a generic same-skill-group rule
 * @author Mario Koehler
 */
public record MissionComponent(String id, SetSkill associatedSkill, double baselineReliability,
                                double maxReliability, double maxReliabilityTransfer,
                                List<ReliabilityTransferSource> reliabilityTransferSources) {

    /**
     * Validates every field and defensively copies
     * {@code reliabilityTransferSources} into an unmodifiable view.
     *
     * @throws NullPointerException     if {@code id}/{@code associatedSkill}/
     *                                   {@code reliabilityTransferSources} is
     *                                   {@code null}
     * @throws IllegalArgumentException if {@code id} is blank, any
     *                                   reliability value is outside 0-100,
     *                                   or {@code baselineReliability}
     *                                   exceeds {@code maxReliability}
     */
    public MissionComponent {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(associatedSkill, "associatedSkill must not be null");
        Objects.requireNonNull(reliabilityTransferSources, "reliabilityTransferSources must not be null");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        requirePercentage(baselineReliability, "baselineReliability");
        requirePercentage(maxReliability, "maxReliability");
        requirePercentage(maxReliabilityTransfer, "maxReliabilityTransfer");
        if (baselineReliability > maxReliability) {
            throw new IllegalArgumentException(
                "baselineReliability " + baselineReliability + " exceeds maxReliability " + maxReliability);
        }
        reliabilityTransferSources = List.copyOf(reliabilityTransferSources);
    }

    /**
     * Validates that a reliability-scale value falls within 0-100.
     *
     * @param value     the value to check
     * @param fieldName the field name to report in the exception message
     * @throws IllegalArgumentException if {@code value} is outside 0-100
     */
    private static void requirePercentage(double value, String fieldName) {
        if (value < 0 || value > 100) {
            throw new IllegalArgumentException(fieldName + " must be within 0-100: " + value);
        }
    }

    /**
     * One entry of {@link MissionComponent#reliabilityTransferSources()}:
     * owning {@link #sourceComponentId()} grants this component a one-time
     * starting reliability bonus of that source's own current reliability
     * times {@link #transferPercent()}, summed across every owned source
     * and capped at {@link MissionComponent#maxReliabilityTransfer()} - the
     * exact formula re-derived from {@code baspm.md} &sect;10.3, with
     * Launch Window's own percentages, not BASPM's.
     *
     * @param sourceComponentId the other component this transfer reads
     *                          from
     * @param transferPercent   the percentage of the source's current
     *                          reliability transferred, 0-100
     */
    public record ReliabilityTransferSource(String sourceComponentId, int transferPercent) {

        /**
         * Validates both fields.
         *
         * @throws NullPointerException     if {@code sourceComponentId} is {@code null}
         * @throws IllegalArgumentException if {@code sourceComponentId} is
         *                                   blank, or {@code transferPercent}
         *                                   is outside 0-100
         */
        public ReliabilityTransferSource {
            Objects.requireNonNull(sourceComponentId, "sourceComponentId must not be null");
            if (sourceComponentId.isBlank()) {
                throw new IllegalArgumentException("sourceComponentId must not be blank");
            }
            if (transferPercent < 0 || transferPercent > 100) {
                throw new IllegalArgumentException("transferPercent must be within 0-100: " + transferPercent);
            }
        }
    }
}
