package de.mkoehler.launchwindow.actions;

import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.TrainingKind;
import de.mkoehler.launchwindow.persistence.ActionContext;
import de.mkoehler.launchwindow.persistence.ActionResult;
import de.mkoehler.launchwindow.persistence.PlayerAction;
import de.mkoehler.launchwindow.rules.PersonnelActions;
import de.mkoehler.launchwindow.rules.PersonnelGroup;

/**
 * Starts advanced or permanent training ({@link PersonnelActions#startTraining}). The inverse is {@link PullOutOfTrainingAction}.
 *
 * <p>Journaled as {@code "startTraining"}; see {@code design.md} &sect;5, M4.</p>
 *
 * @author Mario Koehler
 */
public class StartTrainingAction extends PlayerAction {

    private PersonnelGroup group;
    private String employeeId;
    private TrainingKind kind;
    private String skill;
    private double targetLevel;

    /**
     * No-argument constructor for Jackson and Kryo.
     */
    public StartTrainingAction() {
    }

    /**
     * Creates the action.
     *
     * @param group the group
     * @param employeeId the employee
     * @param kind BASIC, ADVANCED, or PERMANENT
     * @param skill the skill's enum name
     * @param targetLevel the permanent target level; ignored otherwise
     */
    public StartTrainingAction(PersonnelGroup group, String employeeId, TrainingKind kind, String skill, double targetLevel) {
        setGroup(group);
        setEmployeeId(employeeId);
        setKind(kind);
        setSkill(skill);
        setTargetLevel(targetLevel);
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

    /**
     * Returns BASIC, ADVANCED, or PERMANENT.
     *
     * @return BASIC, ADVANCED, or PERMANENT
     */
    public TrainingKind getKind() {
        return kind;
    }

    /**
     * Sets BASIC, ADVANCED, or PERMANENT.
     *
     * @param kind BASIC, ADVANCED, or PERMANENT
     */
    public void setKind(TrainingKind kind) {
        this.kind = kind;
    }

    /**
     * Returns the skill's enum name.
     *
     * @return the skill's enum name
     */
    public String getSkill() {
        return skill;
    }

    /**
     * Sets the skill's enum name.
     *
     * @param skill the skill's enum name
     */
    public void setSkill(String skill) {
        this.skill = skill;
    }

    /**
     * Returns the permanent target level; ignored otherwise.
     *
     * @return the permanent target level; ignored otherwise
     */
    public double getTargetLevel() {
        return targetLevel;
    }

    /**
     * Sets the permanent target level; ignored otherwise.
     *
     * @param targetLevel the permanent target level; ignored otherwise
     */
    public void setTargetLevel(double targetLevel) {
        this.targetLevel = targetLevel;
    }

    @Override
    public ActionResult apply(PlayerSnapshot snapshot, ActionContext context) {
        return ActionResult.of(PersonnelActions.startTraining(snapshot, group, employeeId, kind, skill, targetLevel),
            PersonnelActions.StartTrainingOutcome.STARTED);
    }
}
