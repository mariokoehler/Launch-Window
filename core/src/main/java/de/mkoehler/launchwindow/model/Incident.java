package de.mkoehler.launchwindow.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * <p>A flight that ended in a failure, kept until its investigation is over ({@code design.md} &sect;2.14,
 * {@code rules.Investigations}). Recorded the moment the failing step resolves, because the flight itself is
 * gone by the end of that pass: everything the investigation needs is copied here.</p>
 *
 * <p>An agency's own investigation ({@link Kind#AGENCY}) is offered, maybe started with a lead, and settled at
 * the end of the season it ran; an official one ({@link Kind#OFFICIAL}, a crew was lost) reports on its due
 * turn. The {@code seed} is the start action's server seed, stripped from the client's copy
 * ({@link #withoutSeed}).</p>
 *
 * @param id                     the flight's mission instance id, unique per agency
 * @param kind                   whose investigation it is
 * @param turn                   the game turn the flight failed in
 * @param missionConfigurationId the configuration that flew
 * @param stepId                 the step that failed
 * @param components             the failing step's components, with what each lost
 * @param culpritComponentId     the component the report will name
 * @param crewIds                the crew lost, for an official investigation; empty otherwise
 * @param crewNames              their names, in the same order
 * @param leadId                 the SET specialist leading it, or {@code null} while only offered
 * @param leadName               the lead's name, or {@code null}
 * @param returnComponentId      the component the lead was researching before, to go back to, or {@code null}
 * @param fee                    the fee paid when it started, refunded if it's withdrawn
 * @param chance                 the chance it succeeds, 0 to 1, stamped when it started
 * @param share                  the share of each loss it gives back on success, 0 to 1, stamped when it started
 * @param seed                   the start action's seed, 0 in the client's copy
 * @author Mario Koehler
 */
public record Incident(String id, Kind kind, int turn, String missionConfigurationId, String stepId,
                       List<Part> components, String culpritComponentId, List<String> crewIds,
                       List<String> crewNames, String leadId, String leadName, String returnComponentId,
                       long fee, double chance, double share, long seed) {

    /**
     * Whose investigation it is.
     */
    public enum Kind {
        /** The agency's own, offered after a failure that lost no crew. */
        AGENCY,
        /** An independent board's, after a crew was lost. */
        OFFICIAL
    }

    /**
     * One component of the failing step.
     *
     * @param componentId the component, rocket slots resolved
     * @param involvement its involvement in the failing step, 1 to 100
     * @param loss        the reliability it lost, 0 or more
     */
    public record Part(String componentId, int involvement, double loss) {

        /**
         * Validates the part.
         *
         * @throws NullPointerException if {@code componentId} is {@code null}
         */
        public Part {
            Objects.requireNonNull(componentId, "componentId must not be null");
        }
    }

    /**
     * Copies the lists into {@link ArrayList}s, which Kryo can rebuild.
     *
     * @throws NullPointerException if {@code id}, {@code kind}, {@code missionConfigurationId} or
     *                              {@code stepId} is {@code null}
     */
    public Incident {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(missionConfigurationId, "missionConfigurationId must not be null");
        Objects.requireNonNull(stepId, "stepId must not be null");
        components = components == null ? new ArrayList<>() : new ArrayList<>(components);
        crewIds = crewIds == null ? new ArrayList<>() : new ArrayList<>(crewIds);
        crewNames = crewNames == null ? new ArrayList<>() : new ArrayList<>(crewNames);
    }

    /**
     * Returns whether a lead has started it.
     *
     * @return {@code true} once started
     */
    public boolean started() {
        return leadId != null;
    }

    /**
     * Returns the reliability the flight cost, all components together.
     *
     * @return the total loss
     */
    public double totalLoss() {
        return components.stream().mapToDouble(Part::loss).sum();
    }

    /**
     * Returns this incident started by a lead.
     *
     * @param lead            the lead's id
     * @param name            the lead's name
     * @param returnComponent the component the lead was researching, or {@code null}
     * @param paid            the fee paid
     * @param odds            the chance it succeeds
     * @param givesBack       the share it gives back on success
     * @param actionSeed      the start action's seed
     * @return the started incident
     */
    public Incident startedBy(String lead, String name, String returnComponent, long paid, double odds,
                              double givesBack, long actionSeed) {
        return new Incident(id, kind, turn, missionConfigurationId, stepId, components, culpritComponentId, crewIds,
            crewNames, lead, name, returnComponent, paid, odds, givesBack, actionSeed);
    }

    /**
     * Returns this incident back to only offered, as before it started.
     *
     * @return the offered incident
     */
    public Incident withdrawn() {
        return new Incident(id, kind, turn, missionConfigurationId, stepId, components, culpritComponentId, crewIds,
            crewNames, null, null, null, 0, 0, 0, 0);
    }

    /**
     * Returns a copy without the seed, for the client.
     *
     * @return the copy
     */
    public Incident withoutSeed() {
        return new Incident(id, kind, turn, missionConfigurationId, stepId, components, culpritComponentId, crewIds,
            crewNames, leadId, leadName, returnComponentId, fee, chance, share, 0);
    }
}
