package de.mkoehler.launchwindow.rules;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Reports the independent board's report on a flight that lost its crew ({@link Investigations},
 * {@code design.md} &sect;2.14(b)): always a cause and the steps taken, nothing else changes. The paper prints
 * it for every agency ({@link ReportPrinter}).
 *
 * @param incidentId             the incident, the lost flight's mission instance id
 * @param missionConfigurationId the configuration that was lost
 * @param stepId                 the step it was lost on
 * @param accidentTurn           the turn the crew was lost in
 * @param causeId                the cause, from {@code investigation-causes.json}
 * @param factorId               the contributing factor, from the same file
 * @param culpritComponentId     the component the cause names
 * @param crewIds                the crew lost
 * @param crewNames              their names, in the same order
 * @author Mario Koehler
 */
public record OfficialReportIssued(String incidentId, String missionConfigurationId, String stepId, int accidentTurn,
                                   String causeId, String factorId, String culpritComponentId, List<String> crewIds,
                                   List<String> crewNames) implements ResolutionEvent {

    /**
     * Validates the fields and copies the lists.
     *
     * @throws NullPointerException if an id is {@code null}
     */
    public OfficialReportIssued {
        Objects.requireNonNull(incidentId, "incidentId must not be null");
        Objects.requireNonNull(missionConfigurationId, "missionConfigurationId must not be null");
        Objects.requireNonNull(stepId, "stepId must not be null");
        crewIds = crewIds == null ? new ArrayList<>() : new ArrayList<>(crewIds);
        crewNames = crewNames == null ? new ArrayList<>() : new ArrayList<>(crewNames);
    }
}
