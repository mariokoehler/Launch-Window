package de.mkoehler.launchwindow.actions;

import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.persistence.ActionContext;
import de.mkoehler.launchwindow.persistence.ActionResult;
import de.mkoehler.launchwindow.persistence.PlayerAction;
import de.mkoehler.launchwindow.rules.PersonnelActions;
import de.mkoehler.launchwindow.rules.PersonnelGroup;

/**
 * Dismisses an employee - the inverse of {@link HireAction}, free ({@link PersonnelActions#dismiss}).
 *
 * <p>Journaled as {@code "dismiss"}; see {@code design.md} &sect;5, M4.</p>
 *
 * @author Mario Koehler
 */
public class DismissAction extends PlayerAction {

    private PersonnelGroup group;
    private String employeeId;

    /**
     * No-argument constructor for Jackson and Kryo.
     */
    public DismissAction() {
    }

    /**
     * Creates the action.
     *
     * @param group the group
     * @param employeeId the employee
     */
    public DismissAction(PersonnelGroup group, String employeeId) {
        setGroup(group);
        setEmployeeId(employeeId);
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
     * Returns the employee.
     *
     * @return the employee
     */
    public String getEmployeeId() {
        return employeeId;
    }

    /**
     * Sets the employee.
     *
     * @param employeeId the employee
     */
    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    @Override
    public ActionResult apply(PlayerSnapshot snapshot, ActionContext context) {
        return ActionResult.of(PersonnelActions.dismiss(snapshot, group, employeeId),
            PersonnelActions.DismissOutcome.DISMISSED);
    }
}
