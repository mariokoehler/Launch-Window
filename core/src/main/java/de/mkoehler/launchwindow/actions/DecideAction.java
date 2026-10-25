package de.mkoehler.launchwindow.actions;

import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.persistence.ActionContext;
import de.mkoehler.launchwindow.persistence.ActionResult;
import de.mkoehler.launchwindow.persistence.PlayerAction;
import de.mkoehler.launchwindow.rules.RdDecisions;

/**
 * Decides the waiting R&amp;D decision ({@link RdDecisions#decide}, {@code ui-spec.md} component 26):
 * worked out at once, so the player sees the outcome before the turn resolves. The outcome comes
 * from the decision's own seed, not this action's.
 *
 * <p>Journaled as {@code "decide"}.</p>
 *
 * @author Mario Koehler
 */
public class DecideAction extends PlayerAction {

    private String optionId;
    private String personId;
    private String secondPersonId;

    /**
     * No-argument constructor for Jackson and Kryo.
     */
    public DecideAction() {
    }

    /**
     * Creates the action.
     *
     * @param optionId       the option chosen
     * @param personId       the person it takes, or {@code null}
     * @param secondPersonId the second of a pair, or {@code null}
     */
    public DecideAction(String optionId, String personId, String secondPersonId) {
        this.optionId = optionId;
        this.personId = personId;
        this.secondPersonId = secondPersonId;
    }

    /**
     * Returns the option chosen.
     *
     * @return the option
     */
    public String getOptionId() {
        return optionId;
    }

    /**
     * Sets the option chosen.
     *
     * @param optionId the option
     */
    public void setOptionId(String optionId) {
        this.optionId = optionId;
    }

    /**
     * Returns the person the option takes.
     *
     * @return the person, or {@code null}
     */
    public String getPersonId() {
        return personId;
    }

    /**
     * Sets the person the option takes.
     *
     * @param personId the person, or {@code null}
     */
    public void setPersonId(String personId) {
        this.personId = personId;
    }

    /**
     * Returns the second of a pair.
     *
     * @return the person, or {@code null}
     */
    public String getSecondPersonId() {
        return secondPersonId;
    }

    /**
     * Sets the second of a pair.
     *
     * @param secondPersonId the person, or {@code null}
     */
    public void setSecondPersonId(String secondPersonId) {
        this.secondPersonId = secondPersonId;
    }

    @Override
    public ActionResult apply(PlayerSnapshot snapshot, ActionContext context) {
        RdDecisions.Outcome outcome = RdDecisions.decide(snapshot, context.catalog(), context.catalog().rules().decisions(), optionId,
            personId, secondPersonId);
        // Starting a hands-on puzzle is a decision taken too.
        return new de.mkoehler.launchwindow.persistence.ActionResult(outcome == RdDecisions.Outcome.DECIDED
            || outcome == RdDecisions.Outcome.STARTED, outcome.name());
    }
}
