package de.mkoehler.launchwindow.model;

import java.util.Objects;

/**
 * One astronaut the agency lost, kept for the Museum's memorial ({@code ui-spec.md} &sect;4.11,
 * gap G10). Written when the loss happens; never removed.
 *
 * @param name                   the astronaut's name
 * @param age                    their age when lost
 * @param turn                   the game turn the loss resolved on
 * @param cause                  how they were lost
 * @param missionConfigurationId the mission flown, for a {@link Cause#MISSION} loss, else {@code null}
 * @param stepId                 the step that failed, for a {@link Cause#MISSION} loss, else {@code null}
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
        ACCIDENT
    }

    /**
     * Checks the required fields.
     */
    public MemorialEntry {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(cause, "cause must not be null");
    }
}
