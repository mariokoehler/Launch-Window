package de.mkoehler.launchwindow.actions;

import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.persistence.ActionContext;
import de.mkoehler.launchwindow.persistence.ActionResult;
import de.mkoehler.launchwindow.persistence.PlayerAction;
import de.mkoehler.launchwindow.rules.Intelligence;

/**
 * Sets the level the intelligence service moves toward, one step a season from the next resolution
 * ({@link Intelligence}, {@code design.md} &sect;2.9). A standing order.
 *
 * @author Mario Koehler
 */
public class SetIntelligenceTargetAction extends PlayerAction {

    private int targetLevel;

    /**
     * No-argument constructor for Jackson and Kryo.
     */
    public SetIntelligenceTargetAction() {
    }

    /**
     * Creates the action.
     *
     * @param targetLevel the target level, 0 to 3
     */
    public SetIntelligenceTargetAction(int targetLevel) {
        setTargetLevel(targetLevel);
    }

    /**
     * Returns the target level.
     *
     * @return the level
     */
    public int getTargetLevel() {
        return targetLevel;
    }

    /**
     * Sets the target level.
     *
     * @param targetLevel the level
     */
    public void setTargetLevel(int targetLevel) {
        this.targetLevel = targetLevel;
    }

    @Override
    public ActionResult apply(PlayerSnapshot snapshot, ActionContext context) {
        return ActionResult.of(Intelligence.setTarget(snapshot, targetLevel), Intelligence.Outcome.TARGET_SET);
    }
}
