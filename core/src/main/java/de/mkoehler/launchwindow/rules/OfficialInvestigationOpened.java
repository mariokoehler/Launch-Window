package de.mkoehler.launchwindow.rules;

import java.util.Objects;

/**
 * Reports that an independent board investigates a flight that lost its crew ({@link Investigations},
 * {@code design.md} &sect;2.14(b)): its report is issued at {@code dueTurn}'s resolution.
 *
 * @param incidentId             the incident, the lost flight's mission instance id
 * @param missionConfigurationId the configuration that was lost
 * @param stepId                 the step it was lost on
 * @param dueTurn                the turn whose resolution issues the report
 * @author Mario Koehler
 */
public record OfficialInvestigationOpened(String incidentId, String missionConfigurationId, String stepId,
                                          int dueTurn) implements ResolutionEvent {

    /**
     * Validates the fields.
     *
     * @throws NullPointerException if an id is {@code null}
     */
    public OfficialInvestigationOpened {
        Objects.requireNonNull(incidentId, "incidentId must not be null");
        Objects.requireNonNull(missionConfigurationId, "missionConfigurationId must not be null");
        Objects.requireNonNull(stepId, "stepId must not be null");
    }
}
