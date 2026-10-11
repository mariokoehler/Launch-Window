package de.mkoehler.launchwindow.model;

import java.util.Objects;

/**
 * One thing an intelligence service learned about a rival ({@code design.md} &sect;2.9): a line of a
 * dossier or a personnel file, or a passive tidbit. Only what the server granted is kept, numbers as
 * ranges, so a rival's real values never reach the client.
 *
 * @param kind  what it says
 * @param ref   what it's about: a component, program, configuration, group or skill id
 * @param low   a range's low end (reliability or skill), or 0
 * @param high  a range's high end, or 0
 * @param count a count (engineers, headcount, a level), or 0
 * @author Mario Koehler
 */
public record IntelFact(Kind kind, String ref, int low, int high, int count) {

    /**
     * What a fact says.
     */
    public enum Kind {
        /** A component's reliability, {@code low}&ndash;{@code high} percent. */
        COMPONENT_RELIABILITY,
        /** A program they have open. */
        OPEN_PROGRAM,
        /** A mission configuration they have in flight. */
        IN_FLIGHT,
        /** A component their engineers work on this season, {@code count} of them. */
        RD_FOCUS,
        /** A component they own that has never flown on a successful flight. */
        SHELF,
        /** Their Intelligence level, {@code count}. */
        INTEL_LEVEL,
        /** How many people a group ({@code ref}) has, {@code count}. */
        HEADCOUNT,
        /** A group's ({@code ref}) average skill, {@code low}&ndash;{@code high}. */
        AVERAGE_SKILL,
        /** One person's skill ({@code ref}), {@code low}&ndash;{@code high}. */
        SKILL,
        /** Their active contract, in a line ({@code ref}), at a tier ({@code count}) (design.md 2.11). */
        CONTRACT,
        /**
         * Their record in a contract line ({@code ref}): {@code low} flown, {@code high} failed or lapsed,
         * {@code count} the points it adds to their score.
         */
        CONTRACT_RECORD
    }

    /**
     * Checks the kind and keeps a non-null reference.
     *
     * @throws NullPointerException if {@code kind} is {@code null}
     */
    public IntelFact {
        Objects.requireNonNull(kind, "kind must not be null");
        ref = ref == null ? "" : ref;
    }

    /**
     * Returns which area of an assessment this fact belongs to.
     *
     * @return the area
     */
    public IntelArea area() {
        return switch (kind) {
            case COMPONENT_RELIABILITY -> IntelArea.RND;
            case OPEN_PROGRAM, IN_FLIGHT, RD_FOCUS, SHELF, INTEL_LEVEL, CONTRACT, CONTRACT_RECORD -> IntelArea.PLANS;
            case HEADCOUNT, AVERAGE_SKILL, SKILL -> IntelArea.PERSONNEL;
        };
    }
}
