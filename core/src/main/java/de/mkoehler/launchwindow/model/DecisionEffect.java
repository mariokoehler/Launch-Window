package de.mkoehler.launchwindow.model;

import java.util.Objects;

/**
 * One effect an R&amp;D decision had once applied ({@code ui-spec.md} component 26): what changed and
 * by how much, kept for the outcome on the R&amp;D card and the turn report's line.
 *
 * @param kind    what changed
 * @param amount  by how much: reliability points, funds, a factor, morale or rapport points, seasons
 * @param seasons how long it lasts, for a timed effect; 0 otherwise
 * @param subject who or what it touched: a person's id, a pair as "a+b", or {@code null} for the
 *                component itself
 * @author Mario Koehler
 */
public record DecisionEffect(Kind kind, double amount, int seasons, String subject) {

    /** What a decision can change. */
    public enum Kind {
        /** The component's reliability; written in seasons of R&amp;D, applied in points. */
        RELIABILITY,
        /** Funds; written as a share of a season's funding, applied in money. */
        FUNDS,
        /** The component's R&amp;D gains, times a factor for some seasons. */
        RD_FACTOR,
        /** Morale of the people involved: the component's team, or the picked person. */
        MORALE,
        /** The picked person is away for some seasons. */
        UNAVAILABLE,
        /** Rapport between the picked pair. */
        RAPPORT,
        /** The picked person's skill in the component's skill. */
        SKILL
    }

    /**
     * Checks the kind.
     *
     * @throws NullPointerException if {@code kind} is {@code null}
     */
    public DecisionEffect {
        Objects.requireNonNull(kind, "kind must not be null");
    }
}
