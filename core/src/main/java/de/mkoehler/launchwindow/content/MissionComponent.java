package de.mkoehler.launchwindow.content;

import de.mkoehler.launchwindow.model.Archetype;
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
 * reliability change (component wear/growth from actually flying) is
 * resolved by {@code de.mkoehler.launchwindow.rules.MissionProgress} - see
 * {@code design.md} &sect;2.5(g).</p>
 *
 * @param id                         a stable identifier, unique within a
 *                                   {@link ContentCatalog}
 * @param associatedSkill            the SET skill whose R&amp;D work grows
 *                                   this component's reliability - see
 *                                   {@code de.mkoehler.launchwindow.rules.ResearchProgress}
 * @param baselineReliability        the reliability (0-100) a freshly
 *                                   granted, untransferred component starts at
 * @param maxReliability             the reliability (0-100) ceiling this
 *                                   component can never exceed, by any means
 * @param maxRdReliability           the reliability (0-100) ceiling R&amp;D
 *                                   alone can reach - at or below
 *                                   {@link #maxReliability}, never above;
 *                                   closing the remaining gap (if any) is
 *                                   {@code MissionProgress}'s own post-mission
 *                                   reliability-gain job instead, re-deriving
 *                                   {@code baspm.md} &sect;10.8's own
 *                                   two-ceiling shape (user decision,
 *                                   2026-10-01, see {@code design.md}
 *                                   &sect;3.4/&sect;2.5(g))
 * @param rdSlotBandMaxAmounts       four ascending raw-improvement-amount
 *                                   ceilings, one per R&amp;D slot band -
 *                                   see {@code de.mkoehler.launchwindow.rules.SkillGrowth}
 *                                   for how a slot's band is derived from
 *                                   this list, re-deriving {@code baspm.md}
 *                                   &sect;10.8's own per-component
 *                                   {@code RDLevelMaxAmount L1}-{@code L4}
 *                                   data, with Launch Window's own numbers
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
 * @param maintenanceCost            the upkeep per season while the component is active
 * @param creationCost               the one-time price of first acquiring it, charged
 *                                   when a program or rocket program grants it
 *                                   ({@code baspm.md} &sect;10.9)
 * @param unitCost                   the price of one flight's worth, part of a
 *                                   mission's launch cost
 * @param mass                       the payload mass it adds to a launch; a rocket
 *                                   must lift the summed mass of its launch's payloads
 * @param agency                     the only archetype that may own it, or
 *                                   {@code null} for any - {@code design.md} &sect;5 (M3)
 * @param rocket                     the rocket's class and payload capacities, or
 *                                   {@code null} for a component that isn't a rocket;
 *                                   only allowed on a rocket skill
 * @param ownLift                    for a payload that is partly its own upper stage (the Agena,
 *                                   Apollo's service module), the lift it adds, which comes off
 *                                   what the rocket must carry (BASPM's
 *                                   {@code ComputeExtraBoostProvidedByPayload}, 2026-10-08);
 *                                   {@code null} for none, never on a rocket
 * @author Mario Koehler
 */
public record MissionComponent(String id, SetSkill associatedSkill, double baselineReliability,
                                double maxReliability, double maxRdReliability,
                                List<Double> rdSlotBandMaxAmounts, double maxReliabilityTransfer,
                                List<ReliabilityTransferSource> reliabilityTransferSources,
                                long maintenanceCost, long creationCost, long unitCost, double mass,
                                Archetype agency, RocketSpec rocket, Lift ownLift) {

    /**
     * A rocket's class and how much payload it lifts to each destination - BASPM's
     * {@code MassAndCapacity} ({@code baspm.md} &sect;10.9).
     *
     * @param rocketClass         the size class, gated by the VAB level
     * @param payloadToSuborbital the payload it lifts on a suborbital launch
     * @param payloadToLeo        the payload it lifts to low Earth orbit
     * @param payloadToTli        the payload it lifts to trans-lunar injection
     * @param payloadToTpi        the payload it lifts to trans-planetary injection
     */
    public record RocketSpec(RocketClass rocketClass, double payloadToSuborbital, double payloadToLeo,
                             double payloadToTli, double payloadToTpi) {

        /**
         * Validates the spec.
         *
         * @throws NullPointerException     if {@code rocketClass} is {@code null}
         * @throws IllegalArgumentException if a capacity is negative
         */
        public RocketSpec {
            Objects.requireNonNull(rocketClass, "rocketClass must not be null");
            if (payloadToSuborbital < 0 || payloadToLeo < 0 || payloadToTli < 0 || payloadToTpi < 0) {
                throw new IllegalArgumentException("payload capacities must not be negative");
            }
        }

        /**
         * Returns the payload this rocket lifts to a launch type's destination.
         *
         * @param launchType the launch type
         * @return the capacity
         */
        public double capacityFor(LaunchType launchType) {
            return switch (launchType) {
                case SUBORBITAL, SUBORBITAL_HR -> payloadToSuborbital;
                case LEO, LEO_HR -> payloadToLeo;
                case TLI, TLI_HR -> payloadToTli;
                case TPI, TPI_HR -> payloadToTpi;
                case AIR_DROP -> 0;
            };
        }
    }

    /**
     * The lift a payload adds to its own launch, per destination: BASPM reads it from the same
     * {@code MassAndCapacity} as a rocket's.
     *
     * @param payloadToSuborbital on a suborbital launch
     * @param payloadToLeo        to low Earth orbit
     * @param payloadToTli        to trans-lunar injection
     * @param payloadToTpi        to trans-planetary injection
     */
    public record Lift(double payloadToSuborbital, double payloadToLeo, double payloadToTli, double payloadToTpi) {

        /**
         * Validates the lift.
         *
         * @throws IllegalArgumentException if a value is negative
         */
        public Lift {
            if (payloadToSuborbital < 0 || payloadToLeo < 0 || payloadToTli < 0 || payloadToTpi < 0) {
                throw new IllegalArgumentException("lift must not be negative");
            }
        }

        /**
         * Returns the lift for a launch type's destination.
         *
         * @param launchType the launch type
         * @return the lift
         */
        public double capacityFor(LaunchType launchType) {
            return new RocketSpec(RocketClass.LIGHT, payloadToSuborbital, payloadToLeo, payloadToTli, payloadToTpi)
                .capacityFor(launchType);
        }
    }

    /**
     * A component with no lift of its own, as every component was before the full tree.
     *
     * @param id                         see {@link MissionComponent}
     * @param associatedSkill            see {@link MissionComponent}
     * @param baselineReliability        see {@link MissionComponent}
     * @param maxReliability             see {@link MissionComponent}
     * @param maxRdReliability           see {@link MissionComponent}
     * @param rdSlotBandMaxAmounts       see {@link MissionComponent}
     * @param maxReliabilityTransfer     see {@link MissionComponent}
     * @param reliabilityTransferSources see {@link MissionComponent}
     * @param maintenanceCost            see {@link MissionComponent}
     * @param creationCost               see {@link MissionComponent}
     * @param unitCost                   see {@link MissionComponent}
     * @param mass                       see {@link MissionComponent}
     * @param agency                     see {@link MissionComponent}
     * @param rocket                     see {@link MissionComponent}
     */
    public MissionComponent(String id, SetSkill associatedSkill, double baselineReliability,
                            double maxReliability, double maxRdReliability,
                            List<Double> rdSlotBandMaxAmounts, double maxReliabilityTransfer,
                            List<ReliabilityTransferSource> reliabilityTransferSources,
                            long maintenanceCost, long creationCost, long unitCost, double mass,
                            Archetype agency, RocketSpec rocket) {
        this(id, associatedSkill, baselineReliability, maxReliability, maxRdReliability, rdSlotBandMaxAmounts,
            maxReliabilityTransfer, reliabilityTransferSources, maintenanceCost, creationCost, unitCost, mass, agency,
            rocket, null);
    }

    /**
     * A component without the M3 economy and launch fields: no creation or unit
     * cost, no mass, any agency, not a rocket.
     *
     * @param id                          see {@link MissionComponent}
     * @param associatedSkill             see {@link MissionComponent}
     * @param baselineReliability         see {@link MissionComponent}
     * @param maxReliability              see {@link MissionComponent}
     * @param maxRdReliability            see {@link MissionComponent}
     * @param rdSlotBandMaxAmounts        see {@link MissionComponent}
     * @param maxReliabilityTransfer      see {@link MissionComponent}
     * @param reliabilityTransferSources  see {@link MissionComponent}
     * @param maintenanceCost             see {@link MissionComponent}
     * @throws NullPointerException     see the canonical constructor
     * @throws IllegalArgumentException see the canonical constructor
     */
    public MissionComponent(String id, SetSkill associatedSkill, double baselineReliability,
                            double maxReliability, double maxRdReliability,
                            List<Double> rdSlotBandMaxAmounts, double maxReliabilityTransfer,
                            List<ReliabilityTransferSource> reliabilityTransferSources, long maintenanceCost) {
        this(id, associatedSkill, baselineReliability, maxReliability, maxRdReliability, rdSlotBandMaxAmounts,
            maxReliabilityTransfer, reliabilityTransferSources, maintenanceCost, 0, 0, 0, null, null, null);
    }

    /**
     * A component with no upkeep - the shape before maintenance was modeled.
     *
     * @param id                          see {@link MissionComponent}
     * @param associatedSkill             see {@link MissionComponent}
     * @param baselineReliability         see {@link MissionComponent}
     * @param maxReliability              see {@link MissionComponent}
     * @param maxRdReliability            see {@link MissionComponent}
     * @param rdSlotBandMaxAmounts        see {@link MissionComponent}
     * @param maxReliabilityTransfer      see {@link MissionComponent}
     * @param reliabilityTransferSources  see {@link MissionComponent}
     * @throws NullPointerException     see the canonical constructor
     * @throws IllegalArgumentException see the canonical constructor
     */
    public MissionComponent(String id, SetSkill associatedSkill, double baselineReliability,
                            double maxReliability, double maxRdReliability,
                            List<Double> rdSlotBandMaxAmounts, double maxReliabilityTransfer,
                            List<ReliabilityTransferSource> reliabilityTransferSources) {
        this(id, associatedSkill, baselineReliability, maxReliability, maxRdReliability, rdSlotBandMaxAmounts,
            maxReliabilityTransfer, reliabilityTransferSources, 0L);
    }

    /** The number of simultaneous R&amp;D slots every mission component has - {@code baspm.md} &sect;10.8. */
    public static final int RD_SLOT_COUNT = 4;

    /**
     * Validates every field and defensively copies
     * {@code rdSlotBandMaxAmounts}/{@code reliabilityTransferSources} into
     * unmodifiable views.
     *
     * @throws NullPointerException     if {@code id}/{@code associatedSkill}/
     *                                   {@code rdSlotBandMaxAmounts}/
     *                                   {@code reliabilityTransferSources} is
     *                                   {@code null}
     * @throws IllegalArgumentException if {@code id} is blank, any
     *                                   reliability value is outside 0-100,
     *                                   {@code baselineReliability} exceeds
     *                                   {@code maxRdReliability},
     *                                   {@code maxRdReliability} exceeds
     *                                   {@code maxReliability}, or
     *                                   {@code rdSlotBandMaxAmounts} doesn't
     *                                   have exactly {@value #RD_SLOT_COUNT}
     *                                   non-negative, ascending entries
     */
    public MissionComponent {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(associatedSkill, "associatedSkill must not be null");
        Objects.requireNonNull(rdSlotBandMaxAmounts, "rdSlotBandMaxAmounts must not be null");
        Objects.requireNonNull(reliabilityTransferSources, "reliabilityTransferSources must not be null");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        requirePercentage(baselineReliability, "baselineReliability");
        requirePercentage(maxReliability, "maxReliability");
        requirePercentage(maxRdReliability, "maxRdReliability");
        requirePercentage(maxReliabilityTransfer, "maxReliabilityTransfer");
        if (baselineReliability > maxRdReliability) {
            throw new IllegalArgumentException(
                "baselineReliability " + baselineReliability + " exceeds maxRdReliability " + maxRdReliability);
        }
        if (maxRdReliability > maxReliability) {
            throw new IllegalArgumentException(
                "maxRdReliability " + maxRdReliability + " exceeds maxReliability " + maxReliability);
        }
        if (rdSlotBandMaxAmounts.size() != RD_SLOT_COUNT) {
            throw new IllegalArgumentException(
                "rdSlotBandMaxAmounts must have exactly " + RD_SLOT_COUNT + " entries: " + rdSlotBandMaxAmounts);
        }
        double previous = 0.0;
        for (double amount : rdSlotBandMaxAmounts) {
            if (amount < previous) {
                throw new IllegalArgumentException("rdSlotBandMaxAmounts must be ascending: " + rdSlotBandMaxAmounts);
            }
            previous = amount;
        }
        rdSlotBandMaxAmounts = List.copyOf(rdSlotBandMaxAmounts);
        reliabilityTransferSources = List.copyOf(reliabilityTransferSources);
        if (maintenanceCost < 0 || creationCost < 0 || unitCost < 0 || mass < 0) {
            throw new IllegalArgumentException("costs and mass must not be negative: " + id);
        }
        if (rocket != null && !isRocketSkill(associatedSkill)) {
            throw new IllegalArgumentException("only a rocket may carry a rocket spec: " + id);
        }
        if (ownLift != null && isRocketSkill(associatedSkill)) {
            throw new IllegalArgumentException("a rocket's lift is its rocket spec, not ownLift: " + id);
        }
    }

    /**
     * Returns whether a SET skill is one of the two rocket skills.
     *
     * @param skill the skill
     * @return {@code true} for {@code ROCKETS} and {@code HUMAN_RATED_ROCKETS}
     */
    public static boolean isRocketSkill(SetSkill skill) {
        return skill == SetSkill.ROCKETS || skill == SetSkill.HUMAN_RATED_ROCKETS;
    }

    /**
     * Returns whether this component is a rocket, meaning it carries a rocket spec.
     * Deliberately not named {@code isRocket}, which Jackson would read as a
     * second {@code rocket} property.
     *
     * @return {@code true} for a rocket
     */
    public boolean hasRocketSpec() {
        return rocket != null;
    }

    /**
     * Returns the payload this component lifts for a launch type, as BASPM's
     * {@code GetMaxPayloadSupported}: zero for anything that isn't a rocket, and
     * zero for a plain {@code ROCKETS} rocket on a human-rated launch.
     *
     * @param launchType the launch type
     * @return the capacity, zero if it can't fly that launch
     */
    public double payloadCapacity(LaunchType launchType) {
        if (rocket == null || (launchType.humanRated() && associatedSkill != SetSkill.HUMAN_RATED_ROCKETS)) {
            return 0;
        }
        return rocket.capacityFor(launchType);
    }

    /**
     * Returns the lift this payload adds to its own launch (zero for most).
     *
     * @param launchType the launch type
     * @return the lift
     */
    public double ownLiftFor(LaunchType launchType) {
        return ownLift == null ? 0 : ownLift.capacityFor(launchType);
    }

    /**
     * Returns whether a player of an archetype may own this component.
     *
     * @param archetype the player's archetype
     * @return {@code true} when the component has no agency or matches it
     */
    public boolean availableTo(Archetype archetype) {
        return agency == null || agency == archetype;
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
     *                          reliability transferred, 0-100; BASPM's full tree has
     *                          halves such as 12.5
     */
    public record ReliabilityTransferSource(String sourceComponentId, double transferPercent) {

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
