package de.mkoehler.launchwindow.model;

import java.util.Objects;

/**
 * One astronaut, or animal passenger, the agency lost, kept for the Museum's memorial
 * ({@code ui-spec.md} &sect;4.11, gap G10). Written when the loss happens; never removed.
 *
 * @param name                   the astronaut's name
 * @param age                    their age when lost; 0 for an animal passenger, whose age isn't known
 * @param turn                   the game turn the loss resolved on
 * @param cause                  how they were lost
 * @param missionConfigurationId the mission flown, for a {@link Cause#MISSION} loss, else {@code null}
 * @param stepId                 the step that failed, for a {@link Cause#MISSION} loss or a passenger lost to a
 *                               failure, else {@code null}
 * @param portraitId             the astronaut's portrait, or {@code null} if they had none; never given
 *                               to a new candidate afterwards
 * @author Mario Koehler
 */
public record MemorialEntry(String name, int age, int turn, Cause cause, String missionConfigurationId,
                            String stepId, String portraitId) {

    /**
     * How an astronaut was lost.
     */
    public enum Cause {
        /** A catastrophic mission step: the whole crew is lost. */
        MISSION,
        /** The "perished astronaut" random event: an accident while not flying. */
        ACCIDENT,
        /**
         * An animal passenger that didn't come home (2026-10-05): a flight with no way back, such as
         * Sputnik 2, or a failed flight that was meant to return. Added last: Kryo writes enums by
         * ordinal.
         */
        PASSENGER
    }

    /**
     * Checks the required fields.
     */
    public MemorialEntry {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(cause, "cause must not be null");
    }
}
