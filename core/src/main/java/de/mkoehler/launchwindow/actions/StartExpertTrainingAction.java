package de.mkoehler.launchwindow.actions;

import de.mkoehler.launchwindow.model.AstronautSkill;
import de.mkoehler.launchwindow.model.MissionControlSkill;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.persistence.ActionContext;
import de.mkoehler.launchwindow.persistence.ActionResult;
import de.mkoehler.launchwindow.persistence.PlayerAction;
import de.mkoehler.launchwindow.rules.PersonnelGroup;
import de.mkoehler.launchwindow.rules.StartExpertTrainingOutcome;
import de.mkoehler.launchwindow.rules.TrainingProgress;

/**
 * Starts training toward the Expert trait in one skill, for an astronaut or controller ({@link TrainingProgress}).
 *
 * <p>Journaled as {@code "startExpertTraining"}; see {@code design.md} &sect;5, M4.</p>
 *
 * @author Mario Koehler
 */
public class StartExpertTrainingAction extends PlayerAction {

    private PersonnelGroup group;
    private String employeeId;
    private String skill;

    /**
     * No-argument constructor for Jackson and Kryo.
     */
    public StartExpertTrainingAction() {
    }

    /**
     * Creates the action.
     *
     * @param group ASTRONAUT or MISSION_CONTROL
     * @param employeeId the employee
     * @param skill the skill's enum name
     */
    public StartExpertTrainingAction(PersonnelGroup group, String employeeId, String skill) {
        setGroup(group);
        setEmployeeId(employeeId);
        setSkill(skill);
    }

    /**
     * Returns ASTRONAUT or MISSION_CONTROL.
     *
     * @return ASTRONAUT or MISSION_CONTROL
     */
    public PersonnelGroup getGroup() {
        return group;
    }

    /**
     * Sets ASTRONAUT or MISSION_CONTROL.
     *
     * @param group ASTRONAUT or MISSION_CONTROL
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

    @Override
    public ActionResult apply(PlayerSnapshot snapshot, ActionContext context) {
        StartExpertTrainingOutcome outcome;
        try {
            outcome = switch (group) {
                case ASTRONAUT -> TrainingProgress.startAstronautExpertTraining(snapshot, employeeId,
                    AstronautSkill.valueOf(skill));
                case MISSION_CONTROL -> TrainingProgress.startMissionControlExpertTraining(snapshot, employeeId,
                    MissionControlSkill.valueOf(skill));
                case SET -> null;
            };
        } catch (IllegalArgumentException e) {
            return new ActionResult(false, "UNKNOWN_SKILL");
        }
        if (outcome == null) {
            return new ActionResult(false, "NO_EXPERT_TRAINING_FOR_SET");
        }
        return ActionResult.of(outcome, StartExpertTrainingOutcome.STARTED);
    }
}
