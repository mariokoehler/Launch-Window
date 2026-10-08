package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.model.MissionControlOfficer;
import de.mkoehler.launchwindow.model.SetSpecialist;

import java.util.Objects;

/**
 * How one poaching offer was settled ({@code design.md} &sect;2.2(d)), for one side: the agency
 * that made it or the one whose person it was made to. Kept in the turn's intelligence file, never
 * sent to a client: the person who comes over travels here whole, hidden traits and all, so only
 * {@link OfferSettled}, built from the plain fields, ever reaches a turn report.
 *
 * @param personId     the person's id in the agency they were with when the offer was made
 * @param personName   their name
 * @param group        their group, by {@link PersonnelGroup} name
 * @param raisePercent the raise offered, in percent
 * @param outcome      how it ended
 * @param oldSalary    their salary before
 * @param newSalary    the salary offered
 * @param signingBonus the signing bonus the poacher pays if they come, else 0
 * @param engineer     the engineer who comes over, as they were, for the poacher's side of {@link
 *                     Outcome#JOINED}; else {@code null}
 * @param controller   the flight controller who comes over, likewise; else {@code null}
 * @author Mario Koehler
 */
public record OfferResult(String personId, String personName, String group, int raisePercent, Outcome outcome,
                          double oldSalary, double newSalary, long signingBonus, SetSpecialist engineer,
                          MissionControlOfficer controller) {

    /**
     * How an offer ended.
     */
    public enum Outcome {
        /** The person came over (the poacher's side) or left (the target's side). */
        JOINED,
        /** Their agency matched it: they stay, at the offered salary. */
        MATCHED,
        /** They turned it down. */
        TURNED_DOWN,
        /** The poacher had no room in that roster at the resolution. */
        LAPSED_NO_ROOM,
        /** The person had already left that agency. */
        LAPSED_GONE,
        /** The person couldn't be approached at the resolution: too new, approached recently, or busy. */
        LAPSED_NOT_ELIGIBLE
    }

    /**
     * Checks the required fields.
     *
     * @throws NullPointerException if {@code personId}, {@code group} or {@code outcome} is {@code null}
     */
    public OfferResult {
        Objects.requireNonNull(personId, "personId must not be null");
        Objects.requireNonNull(group, "group must not be null");
        Objects.requireNonNull(outcome, "outcome must not be null");
    }

    /**
     * Returns this result without the person who comes over.
     *
     * @return the result, plain
     */
    public OfferResult plain() {
        return new OfferResult(personId, personName, group, raisePercent, outcome, oldSalary, newSalary, signingBonus,
            null, null);
    }
}
