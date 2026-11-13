package de.mkoehler.launchwindow.actions;

import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.persistence.ActionContext;
import de.mkoehler.launchwindow.persistence.ActionResult;
import de.mkoehler.launchwindow.persistence.PlayerAction;
import de.mkoehler.launchwindow.rules.DepartmentSalaries;
import de.mkoehler.launchwindow.rules.PayExpectations;
import de.mkoehler.launchwindow.rules.PersonnelGroup;

/**
 * Brings everyone in a department paid below their expectation up to it: Manage department's "bring everyone up to
 * expectations", using the department's once a season ({@link PayExpectations}, {@code design.md} &sect;2.2(c)).
 *
 * @author Mario Koehler
 */
public class MeetExpectationsAction extends PlayerAction {

    private PersonnelGroup group;

    /**
     * No-argument constructor for Jackson and Kryo.
     */
    public MeetExpectationsAction() {
    }

    /**
     * Creates the action.
     *
     * @param group the department
     */
    public MeetExpectationsAction(PersonnelGroup group) {
        this.group = group;
    }

    /**
     * Returns the department.
     *
     * @return the group
     */
    public PersonnelGroup getGroup() {
        return group;
    }

    /**
     * Sets the department.
     *
     * @param group the group
     */
    public void setGroup(PersonnelGroup group) {
        this.group = group;
    }

    @Override
    public ActionResult apply(PlayerSnapshot snapshot, ActionContext context) {
        return ActionResult.of(PayExpectations.meetExpectations(snapshot, group),
            DepartmentSalaries.AdjustOutcome.ADJUSTED);
    }
}
