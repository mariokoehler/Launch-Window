package de.mkoehler.launchwindow.rules;

/**
 * Two people's rapport passed the point where the player notices ({@code design.md} &sect;2.2(d)):
 * they work well together, or they clash.
 *
 * @param firstId    one person's id
 * @param firstName  their name
 * @param secondId   the other's id
 * @param secondName their name
 * @param positive   whether they work well together (else they clash)
 * @param teamKind   what they were working on: {@code "rd"} or {@code "mission"}
 * @param subjectId  the component (R&amp;D) or mission configuration
 * @param seasonsTogether how many seasons the two had shared a team by then (added 2026-10-07; 0 in
 *                   older reports)
 * @author Mario Koehler
 */
public record RelationshipShown(
    String firstId,
    String firstName,
    String secondId,
    String secondName,
    boolean positive,
    String teamKind,
    String subjectId,
    int seasonsTogether
) implements ResolutionEvent {
}
