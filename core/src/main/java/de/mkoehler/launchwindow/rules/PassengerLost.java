package de.mkoehler.launchwindow.rules;

import java.util.Objects;

/**
 * Reports that an animal flown as a passenger didn't come home this season, either because the
 * flight was never meant to bring it back (Laika) or because the flight failed. The same moment
 * puts it on the memorial ({@code MemorialEntry.Cause.PASSENGER}); this event is what lets the turn
 * report say so ("Laika did not come home."), {@code design.md} &sect;2.5(f), decided 2026-10-06.
 *
 * @param missionInstanceId      the mission it flew on
 * @param missionConfigurationId that mission's configuration
 * @param name                   the name it's remembered by ("An unnamed dog" on a repeat flight)
 * @param failedStepId           the step that ended the flight, or {@code null} if the flight had no
 *                               way home
 * @author Mario Koehler
 */
public record PassengerLost(String missionInstanceId, String missionConfigurationId, String name,
                            String failedStepId) implements ResolutionEvent {

    /**
     * Validates the fields.
     *
     * @throws NullPointerException if a field other than {@code failedStepId} is {@code null}
     */
    public PassengerLost {
        Objects.requireNonNull(missionInstanceId, "missionInstanceId must not be null");
        Objects.requireNonNull(missionConfigurationId, "missionConfigurationId must not be null");
        Objects.requireNonNull(name, "name must not be null");
    }
}
