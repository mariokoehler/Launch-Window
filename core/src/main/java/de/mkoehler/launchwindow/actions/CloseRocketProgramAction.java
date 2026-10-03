package de.mkoehler.launchwindow.actions;

import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.persistence.ActionContext;
import de.mkoehler.launchwindow.persistence.ActionResult;
import de.mkoehler.launchwindow.persistence.PlayerAction;
import de.mkoehler.launchwindow.rules.RocketProgramOutcome;
import de.mkoehler.launchwindow.rules.RocketPrograms;

/**
 * Closes a rocket program - the inverse of {@link OpenRocketProgramAction}, with no refund ({@link RocketPrograms#close}).
 *
 * <p>Journaled as {@code "closeRocketProgram"}; see {@code design.md} &sect;5, M4.</p>
 *
 * @author Mario Koehler
 */
public class CloseRocketProgramAction extends PlayerAction {

    private String rocketId;

    /**
     * No-argument constructor for Jackson and Kryo.
     */
    public CloseRocketProgramAction() {
    }

    /**
     * Creates the action.
     *
     * @param rocketId the rocket component
     */
    public CloseRocketProgramAction(String rocketId) {
        setRocketId(rocketId);
    }

    /**
     * Returns the rocket component.
     *
     * @return the rocket component
     */
    public String getRocketId() {
        return rocketId;
    }

    /**
     * Sets the rocket component.
     *
     * @param rocketId the rocket component
     */
    public void setRocketId(String rocketId) {
        this.rocketId = rocketId;
    }

    @Override
    public ActionResult apply(PlayerSnapshot snapshot, ActionContext context) {
        return ActionResult.of(RocketPrograms.close(snapshot, rocketId, context.catalog()), RocketProgramOutcome.CLOSED);
    }
}
