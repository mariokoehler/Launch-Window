package de.mkoehler.launchwindow.model;

import java.util.Objects;

/**
 * One line of a person's career record ({@code design.md} &sect;2.2(d), phase 4). What the fields
 * hold depends on the kind:
 * <ul>
 *   <li>{@link Kind#HIRED}: {@code note} the colleague's hint.</li>
 *   <li>{@link Kind#TRAINING}: {@code ref} the skill, {@code note} the kind of training,
 *       {@code value} the gain.</li>
 *   <li>{@link Kind#RD_STINT}: {@code ref} the component, {@code count} the seasons,
 *       {@code value} the reliability the person added.</li>
 *   <li>{@link Kind#FLIGHT}: {@code ref} the configuration, {@code note} the role (CREW, CAPCOM,
 *       CONTROLLER), {@code outcome} the step it failed at, or {@code ""} for a success.</li>
 *   <li>{@link Kind#FIRST}: {@code ref} the goal, {@code note} the configuration.</li>
 *   <li>{@link Kind#TRAIT}: {@code ref} the trait.</li>
 *   <li>{@link Kind#RELATIONSHIP}: {@code ref} the other person's name, {@code note} what they worked
 *       on, {@code count} the seasons together, {@code value} +1 working well, −1 clashing.</li>
 *   <li>{@link Kind#EXPERT}: {@code ref} the skill.</li>
 *   <li>{@link Kind#AMBITION}: {@code ref} the ambition, {@code note} its component if any,
 *       {@code outcome} SAID, FULFILLED, PASSED_OVER, IMPOSSIBLE or RAN_OUT.</li>
 *   <li>{@link Kind#JOINED}: {@code ref} the agency they left, {@code count} the raise in percent,
 *       {@code value} the new salary, {@code note} the old salary.</li>
 *   <li>{@link Kind#OFFER}: {@code ref} the agency that made it, {@code count} the raise in percent,
 *       {@code outcome} MATCHED or TURNED_DOWN, {@code value} the salary after it, {@code note} the
 *       salary before a match.</li>
 * </ul>
 *
 * @param turn    the game turn it happened
 * @param kind    what happened
 * @param ref     what it's about
 * @param note    a detail
 * @param outcome how it ended
 * @param value   a number
 * @param count   a count
 * @author Mario Koehler
 */
public record CareerEntry(int turn, Kind kind, String ref, String note, String outcome, double value, int count) {

    /**
     * What a career line records.
     */
    public enum Kind {
        /** Joined the agency. */
        HIRED,
        /** Finished a training. */
        TRAINING,
        /** Worked on a component's R&amp;D. */
        RD_STINT,
        /** Flew, or served on the consoles, on a mission. */
        FLIGHT,
        /** Part of a goal reached first in the game. */
        FIRST,
        /** A trait came out. */
        TRAIT,
        /** A relationship showed. */
        RELATIONSHIP,
        /** Earned the Expert trait. */
        EXPERT,
        /** An ambition was said or settled. */
        AMBITION,
        /** Came over from a rival agency (poaching). */
        JOINED,
        /** Stayed when a rival made an offer (poaching). */
        OFFER
    }

    /**
     * Checks the kind and keeps the strings non-null.
     *
     * @throws NullPointerException if {@code kind} is {@code null}
     */
    public CareerEntry {
        Objects.requireNonNull(kind, "kind must not be null");
        ref = ref == null ? "" : ref;
        note = note == null ? "" : note;
        outcome = outcome == null ? "" : outcome;
    }
}
