package de.mkoehler.launchwindow.rules;

/**
 * A poaching offer settled ({@code design.md} &sect;2.2(d)), for the turn report's People section:
 * one the agency made, or one a rival made to one of its people.
 *
 * @param made         whether this agency made the offer (else a rival made it to one of ours)
 * @param rivalId      the other agency's player id (the opaque rival key)
 * @param rivalAgency  the other agency's name
 * @param personId     the person's id in this agency: ours when they stayed or arrived, else the id
 *                     they had with the rival or with us before they left
 * @param personName   their name
 * @param group        their group, by {@link PersonnelGroup} name
 * @param raisePercent the raise offered, in percent
 * @param outcome      how it ended
 * @param oldSalary    their salary before
 * @param newSalary    the salary offered
 * @param signingBonus the signing bonus paid, if they came to us, else 0
 * @param retention    for an offer to one of ours: the standing order that applied, by
 *                     {@code Retention} name, else {@code ""}
 * @param matchPercent for an offer to one of ours on the agency default: the default's cap, else -1
 * @author Mario Koehler
 */
public record OfferSettled(boolean made, String rivalId, String rivalAgency, String personId, String personName,
                           String group, int raisePercent, OfferResult.Outcome outcome, double oldSalary,
                           double newSalary, long signingBonus, String retention, int matchPercent)
    implements ResolutionEvent {
}
