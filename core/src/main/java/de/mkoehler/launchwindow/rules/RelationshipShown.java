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
 * @author Mario Koehler
 */
public record RelationshipShown(
    String firstId,
    String firstName,
    String secondId,
    String secondName,
    boolean positive,
    String teamKind,
    String subjectId
) implements ResolutionEvent {
}
