package de.mkoehler.launchwindow.actions;

import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.persistence.ActionContext;
import de.mkoehler.launchwindow.persistence.ActionResult;
import de.mkoehler.launchwindow.persistence.PlayerAction;
import de.mkoehler.launchwindow.rules.PersonnelGroup;
import de.mkoehler.launchwindow.rules.PullOutOfTrainingOutcome;
import de.mkoehler.launchwindow.rules.TrainingProgress;

import java.util.SplittableRandom;

/**
 * Ends any training early, with partial credit - the inverse of starting it ({@link TrainingProgress#pullOutOfTraining}). Uses the action's seed for the partial-credit draw.
 *
 * <p>Journaled as {@code "pullOutOfTraining"}; see {@code design.md} &sect;5, M4.</p>
 *
 * @author Mario Koehler
 */
public class PullOutOfTrainingAction extends PlayerAction {

    private PersonnelGroup group;
    private String employeeId;

    /**
     * No-argument constructor for Jackson and Kryo.
     */
    public PullOutOfTrainingAction() {
    }

    /**
     * Creates the action.
     *
     * @param group the group
     * @param employeeId the employee
     */
    public PullOutOfTrainingAction(PersonnelGroup group, String employeeId) {
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
        return ActionResult.of(TrainingProgress.pullOutOfTraining(snapshot, group, employeeId,
            new SplittableRandom(getSeed())), PullOutOfTrainingOutcome.PULLED_OUT);
    }
}
