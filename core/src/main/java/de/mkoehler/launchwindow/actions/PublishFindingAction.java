package de.mkoehler.launchwindow.actions;

import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.persistence.ActionContext;
import de.mkoehler.launchwindow.persistence.ActionResult;
import de.mkoehler.launchwindow.persistence.PlayerAction;
import de.mkoehler.launchwindow.rules.Findings;

/**
 * Asks to publish a finding at the next resolution, or withdraws the ask ({@code design.md} &sect;2.13):
 * settled once a turn for every agency, when its rivals learn it and it pays prestige.
 *
 * @author Mario Koehler
 */
public class PublishFindingAction extends PlayerAction {

    private String goalId;
    private boolean publish = true;

    /**
     * No-argument constructor for Jackson and Kryo.
     */
    public PublishFindingAction() {
    }

    /**
     * Creates the action.
     *
     * @param goalId  the finding
     * @param publish {@code true} to ask, {@code false} to withdraw the ask
     */
    public PublishFindingAction(String goalId, boolean publish) {
        this.goalId = goalId;
        this.publish = publish;
    }

    /**
     * Returns the finding.
     *
     * @return its goal
     */
    public String getGoalId() {
        return goalId;
    }

    /**
     * Sets the finding.
     *
     * @param goalId its goal
     */
    public void setGoalId(String goalId) {
        this.goalId = goalId;
    }

    /**
     * Returns whether this asks to publish rather than withdrawing the ask.
     *
     * @return {@code true} to ask
     */
    public boolean isPublish() {
        return publish;
    }

    /**
     * Sets whether this asks to publish.
     *
     * @param publish {@code true} to ask, {@code false} to withdraw
     */
    public void setPublish(boolean publish) {
        this.publish = publish;
    }

    @Override
    public ActionResult apply(PlayerSnapshot snapshot, ActionContext context) {
        Findings.PublishOutcome outcome = Findings.publish(snapshot, goalId, publish, context.catalog());
        return new ActionResult(outcome == Findings.PublishOutcome.QUEUED
            || outcome == Findings.PublishOutcome.WITHDRAWN, outcome.name());
    }
}
