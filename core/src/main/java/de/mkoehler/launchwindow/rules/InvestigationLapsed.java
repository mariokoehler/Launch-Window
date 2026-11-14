package de.mkoehler.launchwindow.rules;

import java.util.Objects;

/**
 * Reports that an offered investigation was never started and is closed ({@link Investigations},
 * {@code design.md} &sect;2.14(a)). It costs nothing.
 *
 * @param incidentId             the incident
 * @param missionConfigurationId the configuration that failed
 * @author Mario Koehler
 */
public record InvestigationLapsed(String incidentId, String missionConfigurationId) implements ResolutionEvent {

    /**
     * Validates the fields.
     *
     * @throws NullPointerException if an id is {@code null}
     */
    public InvestigationLapsed {
        Objects.requireNonNull(incidentId, "incidentId must not be null");
        Objects.requireNonNull(missionConfigurationId, "missionConfigurationId must not be null");
    }
}
