package de.mkoehler.launchwindow.actions;

import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.persistence.ActionContext;
import de.mkoehler.launchwindow.persistence.ActionResult;
import de.mkoehler.launchwindow.persistence.PlayerAction;
import de.mkoehler.launchwindow.rules.PrDuty;

/**
 * Calls an astronaut back from PR duty; they are idle again at once ({@link PrDuty}, {@code design.md} &sect;2.2(c)).
 *
 * @author Mario Koehler
 */
public class RecallFromPrDutyAction extends PlayerAction {

    private String astronautId;

    /**
     * No-argument constructor for Jackson and Kryo.
     */
    public RecallFromPrDutyAction() {
    }

    /**
     * Creates the action.
     *
     * @param astronautId the astronaut
     */
    public RecallFromPrDutyAction(String astronautId) {
        setAstronautId(astronautId);
    }

    /**
     * Returns the astronaut.
     *
     * @return the astronaut's id
     */
    public String getAstronautId() {
        return astronautId;
    }

    /**
     * Sets the astronaut.
     *
     * @param astronautId the astronaut's id
     */
    public void setAstronautId(String astronautId) {
        this.astronautId = astronautId;
    }

    @Override
    public ActionResult apply(PlayerSnapshot snapshot, ActionContext context) {
        return ActionResult.of(PrDuty.recall(snapshot, astronautId), PrDuty.Outcome.RECALLED);
    }
}
