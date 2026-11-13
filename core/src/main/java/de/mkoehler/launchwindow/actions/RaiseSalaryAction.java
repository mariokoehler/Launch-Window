package de.mkoehler.launchwindow.actions;

import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.persistence.ActionContext;
import de.mkoehler.launchwindow.persistence.ActionResult;
import de.mkoehler.launchwindow.persistence.PlayerAction;
import de.mkoehler.launchwindow.rules.PayExpectations;

/**
 * Raises one person's salary, once a season: to what they expect, or by a percentage ({@link PayExpectations},
 * {@code design.md} &sect;2.2(c)).
 *
 * @author Mario Koehler
 */
public class RaiseSalaryAction extends PlayerAction {

    private String personnelId;
    private int percent;

    /**
     * No-argument constructor for Jackson and Kryo.
     */
    public RaiseSalaryAction() {
    }

    /**
     * Creates the action.
     *
     * @param personnelId the person
     * @param percent     the raise in percent, or 0 for their expectation
     */
    public RaiseSalaryAction(String personnelId, int percent) {
        this.personnelId = personnelId;
        this.percent = percent;
    }

    /**
     * Returns the person.
     *
     * @return their id
     */
    public String getPersonnelId() {
        return personnelId;
    }

    /**
     * Sets the person.
     *
     * @param personnelId their id
     */
    public void setPersonnelId(String personnelId) {
        this.personnelId = personnelId;
    }

    /**
     * Returns the raise in percent, 0 for their expectation.
     *
     * @return the percentage
     */
    public int getPercent() {
        return percent;
    }

    /**
     * Sets the raise in percent, 0 for their expectation.
     *
     * @param percent the percentage
     */
    public void setPercent(int percent) {
        this.percent = percent;
    }

    @Override
    public ActionResult apply(PlayerSnapshot snapshot, ActionContext context) {
        return ActionResult.of(PayExpectations.raise(snapshot, personnelId, percent),
            PayExpectations.RaiseOutcome.RAISED);
    }
}
