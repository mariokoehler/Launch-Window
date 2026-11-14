package de.mkoehler.launchwindow.rules;

import java.util.Objects;

/**
 * Reports that a failure that lost no crew can be investigated by the agency ({@link Investigations},
 * {@code design.md} &sect;2.14): the turn report offers it until {@code lastTurn}'s resolution.
 *
 * @param incidentId             the incident, the failed flight's mission instance id
 * @param missionConfigurationId the configuration that failed
 * @param stepId                 the step that failed
 * @param totalLoss              the reliability the failure cost, all components together
 * @param lastTurn               the last turn it can be started in
 * @author Mario Koehler
 */
public record InvestigationOffered(String incidentId, String missionConfigurationId, String stepId, double totalLoss,
                                   int lastTurn) implements ResolutionEvent {

    /**
     * Validates the fields.
     *
     * @throws NullPointerException if an id is {@code null}
     */
    public InvestigationOffered {
        Objects.requireNonNull(incidentId, "incidentId must not be null");
        Objects.requireNonNull(missionConfigurationId, "missionConfigurationId must not be null");
        Objects.requireNonNull(stepId, "stepId must not be null");
    }
}
