package de.mkoehler.launchwindow.actions;

import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.persistence.ActionContext;
import de.mkoehler.launchwindow.persistence.ActionResult;
import de.mkoehler.launchwindow.persistence.PlayerAction;
import de.mkoehler.launchwindow.rules.DepartmentSalaries;
import de.mkoehler.launchwindow.rules.PersonnelGroup;

/**
 * Changes every salary in one personnel group once by a percentage, with morale moving along
 * ({@link DepartmentSalaries}, gap G8): the Manage department dialog's OK.
 *
 * @author Mario Koehler
 */
public class AdjustSalariesAction extends PlayerAction {

    private PersonnelGroup group;
    private int percent;

    /**
     * No-argument constructor for Jackson and Kryo.
     */
    public AdjustSalariesAction() {
    }

    /**
     * Creates the action.
     *
     * @param group   the group
     * @param percent the salary change, -50 to +50, not zero
     */
    public AdjustSalariesAction(PersonnelGroup group, int percent) {
        setGroup(group);
        setPercent(percent);
    }

    /**
     * Returns the group.
     *
     * @return the group
     */
    public PersonnelGroup getGroup() {
        return group;
    }

    /**
     * Sets the group.
     *
     * @param group the group
     */
    public void setGroup(PersonnelGroup group) {
        this.group = group;
    }

    /**
     * Returns the salary change in percent.
     *
     * @return the percentage
     */
    public int getPercent() {
        return percent;
    }

    /**
     * Sets the salary change in percent.
     *
     * @param percent the percentage
     */
    public void setPercent(int percent) {
        this.percent = percent;
    }

    @Override
    public ActionResult apply(PlayerSnapshot snapshot, ActionContext context) {
        return ActionResult.of(DepartmentSalaries.adjust(snapshot, group, percent),
            DepartmentSalaries.AdjustOutcome.ADJUSTED);
    }
}
