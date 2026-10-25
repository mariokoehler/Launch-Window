package de.mkoehler.launchwindow.actions;

import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.persistence.ActionContext;
import de.mkoehler.launchwindow.persistence.ActionResult;
import de.mkoehler.launchwindow.persistence.PlayerAction;
import de.mkoehler.launchwindow.rules.HandsOn;
import de.mkoehler.launchwindow.rules.RdDecisions;

/**
 * One step of a hands-on decision's puzzle ({@code ui-spec.md} component 26, type 11): queue a
 * command, undo, reset, confirm the round, or answer a fail. The server takes it at once and answers
 * with the new state; the rolls come from the decision's seed, which the client never has.
 *
 * @author Mario Koehler
 */
public class HandsOnAction extends PlayerAction {

    private RdDecisions.HandsOnStep step;
    private String commandId;

    /**
     * No-argument constructor for Jackson and Kryo.
     */
    public HandsOnAction() {
    }

    /**
     * Creates the action.
     *
     * @param step      the step
     * @param commandId the command, for {@link RdDecisions.HandsOnStep#QUEUE}, else {@code null}
     */
    public HandsOnAction(RdDecisions.HandsOnStep step, String commandId) {
        this.step = step;
        this.commandId = commandId;
    }

    /**
     * Returns the step.
     *
     * @return the step
     */
    public RdDecisions.HandsOnStep getStep() {
        return step;
    }

    /**
     * Sets the step.
     *
     * @param step the step
     */
    public void setStep(RdDecisions.HandsOnStep step) {
        this.step = step;
    }

    /**
     * Returns the command.
     *
     * @return the command, or {@code null}
     */
    public String getCommandId() {
        return commandId;
    }

    /**
     * Sets the command.
     *
     * @param commandId the command, or {@code null}
     */
    public void setCommandId(String commandId) {
        this.commandId = commandId;
    }

    @Override
    public ActionResult apply(PlayerSnapshot snapshot, ActionContext context) {
        if (step == null) {
            return new ActionResult(false, HandsOn.Outcome.WRONG_PHASE.name());
        }
        String outcome = RdDecisions.handsOn(snapshot, context.catalog(), context.catalog().rules().decisions(), step, commandId);
        return new ActionResult(outcome.equals(HandsOn.Outcome.DONE.name()), outcome);
    }
}
