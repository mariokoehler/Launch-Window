package de.mkoehler.launchwindow.model;

import java.util.Objects;

/**
 * A rival's failed attempt, as an agency's intelligence service saw it ({@code design.md} &sect;2.9,
 * "Failed attempts"): a mission that ended on a failing step. Development can be kept secret, but a
 * rocket that fails on the pad or in flight can't, so from level 1 every one is reported, no roll.
 * The detail is stripped to what the service's level earned when it was seen, and never grows later.
 *
 * @param turn            the game turn the mission failed (reported the turn after)
 * @param programId       the program it belonged to
 * @param configurationId the configuration flown, from level 2, else {@code null}
 * @param stepId          the step it failed on, from level 2, else {@code null}
 * @param componentId     the suspected component, from level 3, else {@code null}: the one with the
 *                        largest share of the step's risk (its involvement times its unreliability)
 * @param phase           roughly where it failed
 * @param crewLost        whether the crew was lost (public anyway, through the memorial page)
 * @param side            the rival's archetype: a NASA-side failure is read in the press, a Soviet-side
 *                        one picked up by tracking stations (wording only)
 * @author Mario Koehler
 */
public record RivalFailure(int turn, String programId, String configurationId, String stepId, String componentId,
                           Phase phase, boolean crewLost, Archetype side) {

    /**
     * Roughly where a mission failed: what anyone watching could tell.
     */
    public enum Phase {
        /** Before liftoff: preparations or the countdown. */
        PAD,
        /** On the way up, while the rocket still carried the craft (or the carrier plane, for an air drop). */
        LAUNCH,
        /** Anywhere after the rocket let go and before the way home. */
        FLIGHT,
        /** On the way home: retrofire, reentry, descent, recovery. */
        RETURN
    }

    /**
     * Checks the required fields.
     *
     * @throws NullPointerException if {@code programId}, {@code phase} or {@code side} is {@code null}
     */
    public RivalFailure {
        Objects.requireNonNull(programId, "programId must not be null");
        Objects.requireNonNull(phase, "phase must not be null");
        Objects.requireNonNull(side, "side must not be null");
    }

    /**
     * Returns this failure with only what a service at the given level learns.
     *
     * @param level the service's level, 1 to 3
     * @return the copy
     */
    public RivalFailure seenAt(int level) {
        return new RivalFailure(turn, programId, level >= 2 ? configurationId : null, level >= 2 ? stepId : null,
            level >= 3 ? componentId : null, phase, crewLost, side);
    }
}
