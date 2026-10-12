package de.mkoehler.launchwindow.rules;

import java.util.Objects;

/**
 * Reports that the agency's crewed flights are grounded after a mission lost its crew
 * ({@link CrewedGrounding}, {@code design.md} &sect;2.5(j)): the turn report says for how long.
 *
 * @param missionInstanceId      the mission that lost its crew
 * @param missionConfigurationId that mission's configuration
 * @param seasons                the turns no crewed flight may launch, from the next one
 * @author Mario Koehler
 */
public record CrewedFlightsGrounded(String missionInstanceId, String missionConfigurationId, int seasons)
        implements ResolutionEvent {

    /**
     * Validates the fields.
     *
     * @throws NullPointerException if an id is {@code null}
     */
    public CrewedFlightsGrounded {
        Objects.requireNonNull(missionInstanceId, "missionInstanceId must not be null");
        Objects.requireNonNull(missionConfigurationId, "missionConfigurationId must not be null");
    }
}
