package de.mkoehler.launchwindow.actions;

import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.persistence.ActionContext;
import de.mkoehler.launchwindow.persistence.ActionResult;
import de.mkoehler.launchwindow.persistence.PlayerAction;
import de.mkoehler.launchwindow.rules.Intelligence;

/**
 * Cancels a queued intelligence operation and gives its action back ({@link Intelligence#cancel},
 * {@code design.md} &sect;2.9).
 *
 * @author Mario Koehler
 */
public class CancelIntelOperationAction extends PlayerAction {

    private String operationId;

    /**
     * No-argument constructor for Jackson and Kryo.
     */
    public CancelIntelOperationAction() {
    }

    /**
     * Creates the action.
     *
     * @param operationId the operation
     */
    public CancelIntelOperationAction(String operationId) {
        setOperationId(operationId);
    }

    /**
     * Returns the operation.
     *
     * @return the operation's id
     */
    public String getOperationId() {
        return operationId;
    }

    /**
     * Sets the operation.
     *
     * @param operationId the operation's id
     */
    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    @Override
    public ActionResult apply(PlayerSnapshot snapshot, ActionContext context) {
        return ActionResult.of(Intelligence.cancel(snapshot, operationId), Intelligence.Outcome.CANCELLED);
    }
}
