package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.model.Ambition;

/**
 * An ambition said or settled ({@code design.md} &sect;2.2(d), phase 4), for the turn report.
 *
 * @param group         the person's group
 * @param personnelId   the person
 * @param name          their name
 * @param kind          the ambition
 * @param ref           its component, or {@code ""}
 * @param outcome       what happened
 * @param seasonsLeft   the seasons of patience left (for SAID and PASSED_OVER)
 * @param moraleChange  the morale it cost or gave
 * @param loyaltyChange the loyalty it cost or gave
 * @author Mario Koehler
 */
public record AmbitionSettled(PersonnelGroup group, String personnelId, String name, Ambition.Kind kind, String ref,
                              Outcome outcome, int seasonsLeft, int moraleChange, int loyaltyChange)
    implements ResolutionEvent {

    /**
     * What happened to the ambition.
     */
    public enum Outcome {
        /** The person said it. */
        SAID,
        /** It came true. */
        FULFILLED,
        /** Someone else in the agency got it; it stays, with less patience. */
        PASSED_OVER,
        /** It can't come true any more. */
        IMPOSSIBLE,
        /** The patience ran out: the person is disappointed for a while. */
        RAN_OUT
    }
}
