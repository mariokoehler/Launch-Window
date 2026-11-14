package de.mkoehler.launchwindow.rules;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Reports the agency's own investigation's result ({@link Investigations}, {@code design.md} &sect;2.14(a)):
 * the cause found and the reliability given back, or an undetermined cause and nothing.
 *
 * @param incidentId             the incident
 * @param missionConfigurationId the configuration that failed
 * @param stepId                 the step that failed
 * @param leadId                 the lead
 * @param leadName               the lead's name
 * @param found                  whether the cause was found
 * @param causeId                the cause, from {@code investigation-causes.json}, or {@code null} when not found
 * @param culpritComponentId     the component the cause names
 * @param recoveries             the reliability given back per component; empty when not found
 * @param morale                 the lead's morale change
 * @author Mario Koehler
 */
public record InvestigationConcluded(String incidentId, String missionConfigurationId, String stepId, String leadId,
                                     String leadName, boolean found, String causeId, String culpritComponentId,
                                     List<Recovery> recoveries, int morale) implements ResolutionEvent {

    /**
     * Reliability given back to one component.
     *
     * @param componentId the component
     * @param points      the points given back, after the clamp at its maximum
     */
    public record Recovery(String componentId, double points) {

        /**
         * Validates the recovery.
         *
         * @throws NullPointerException if {@code componentId} is {@code null}
         */
        public Recovery {
            Objects.requireNonNull(componentId, "componentId must not be null");
        }
    }

    /**
     * Validates the fields and copies the list.
     *
     * @throws NullPointerException if an id is {@code null}
     */
    public InvestigationConcluded {
        Objects.requireNonNull(incidentId, "incidentId must not be null");
        Objects.requireNonNull(missionConfigurationId, "missionConfigurationId must not be null");
        Objects.requireNonNull(stepId, "stepId must not be null");
        recoveries = recoveries == null ? new ArrayList<>() : new ArrayList<>(recoveries);
    }

    /**
     * Returns the reliability given back, all components together.
     *
     * @return the points
     */
    public double totalRecovered() {
        return recoveries.stream().mapToDouble(Recovery::points).sum();
    }
}
