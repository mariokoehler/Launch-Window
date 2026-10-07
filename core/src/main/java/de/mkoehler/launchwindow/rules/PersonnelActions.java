package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.AstronautSkill;
import de.mkoehler.launchwindow.model.MissionControlOfficer;
import de.mkoehler.launchwindow.model.MissionControlSkill;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.Personnel;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSkill;
import de.mkoehler.launchwindow.model.SetSpecialist;
import de.mkoehler.launchwindow.model.TrainingKind;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * <p>The validated personnel actions M4's dispatch needs beyond hiring and the
 * Expert trait: dismissing an employee, starting basic, advanced, or permanent
 * training, and assigning or releasing a SET specialist's R&amp;D. Each fails
 * without changing anything, with an outcome, rather than throwing - the same
 * posture as every other player action ({@code design.md} &sect;5, M4).</p>
 *
 * @author Mario Koehler
 */
public final class PersonnelActions {

    /** The result of {@link #dismiss}. */
    public enum DismissOutcome {
        /** The employee left the roster. */
        DISMISSED,
        /** No employee with that id in that group. */
        UNKNOWN_EMPLOYEE,
        /** An astronaut or controller assigned to a mission can't be dismissed. */
        ASSIGNED_TO_MISSION
    }

    /** The result of {@link #startTraining}. */
    public enum StartTrainingOutcome {
        /** Training started. */
        STARTED,
        /** No employee with that id in that group. */
        UNKNOWN_EMPLOYEE,
        /** The employee isn't idle (or is unavailable this season). */
        NOT_IDLE,
        /** The kind isn't ADVANCED or PERMANENT (basic training only follows a hire). */
        UNSUPPORTED_KIND,
        /** The skill isn't one of the group's skills. */
        UNKNOWN_SKILL,
        /** A permanent target isn't above the employee's current skill. */
        TARGET_NOT_ABOVE_SKILL
    }

    /** The result of {@link #assignResearch} and {@link #releaseResearch}. */
    public enum ResearchOutcome {
        /** The specialist now researches the component. */
        ASSIGNED,
        /** The specialist is idle again. */
        RELEASED,
        /** No SET specialist with that id. */
        UNKNOWN_SPECIALIST,
        /** The specialist isn't idle (assign) or isn't researching (release). */
        WRONG_STATE,
        /** The component isn't owned and active. */
        COMPONENT_NOT_AVAILABLE,
        /** The component is already at its R&amp;D ceiling. */
        AT_RD_CEILING,
        /** All four R&amp;D slots on the component are taken. */
        NO_FREE_SLOT
    }

    /**
     * Not instantiable; this class only exposes static methods.
     */
    private PersonnelActions() {
    }

    /**
     * Dismisses an employee, as BASPM's {@code DismissEmployee} does: free, and
     * refused for an astronaut or controller assigned to a mission. A SET
     * specialist can be dismissed mid-R&amp;D, which frees their slot
     * ({@code baspm.md} &sect;10.10).
     *
     * @param snapshot   the player; mutated only on success
     * @param group      the employee's group
     * @param employeeId the employee
     * @return the outcome
     */
    public static DismissOutcome dismiss(PlayerSnapshot snapshot, PersonnelGroup group, String employeeId) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(group, "group must not be null");
        Objects.requireNonNull(employeeId, "employeeId must not be null");
        List<? extends Personnel> roster = roster(snapshot, group);
        Personnel person = find(roster, employeeId);
        if (person == null) {
            return DismissOutcome.UNKNOWN_EMPLOYEE;
        }
        if (group != PersonnelGroup.SET && person.getAssignmentState() == AssignmentState.ASSIGNED) {
            return DismissOutcome.ASSIGNED_TO_MISSION;
        }
        roster.remove(person);
        return DismissOutcome.DISMISSED;
    }

    /**
     * Starts advanced or permanent training in one skill. The employee must
     * be idle and available. Basic training isn't a player's choice: every new hire
     * starts it on joining ({@link HiringPool}), so BASIC is refused as
     * {@link StartTrainingOutcome#UNSUPPORTED_KIND}. Permanent training needs a target level above the
     * current skill, and runs advanced-length rounds until it gets there
     * ({@code design.md} &sect;2.2(c)).
     *
     * @param snapshot    the player; mutated only on success
     * @param group       the employee's group
     * @param employeeId  the employee
     * @param kind        ADVANCED or PERMANENT
     * @param skillName   the skill's enum name within the group
     * @param targetLevel the permanent target; ignored for the other kinds
     * @return the outcome
     */
    public static StartTrainingOutcome startTraining(PlayerSnapshot snapshot, PersonnelGroup group,
            String employeeId, TrainingKind kind, String skillName, double targetLevel) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(group, "group must not be null");
        Objects.requireNonNull(employeeId, "employeeId must not be null");
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(skillName, "skillName must not be null");
        Personnel person = find(roster(snapshot, group), employeeId);
        if (person == null) {
            return StartTrainingOutcome.UNKNOWN_EMPLOYEE;
        }
        if (person.getAssignmentState() != AssignmentState.IDLE || person.getUnavailableSeasons() > 0) {
            return StartTrainingOutcome.NOT_IDLE;
        }
        if (kind != TrainingKind.ADVANCED && kind != TrainingKind.PERMANENT) {
            return StartTrainingOutcome.UNSUPPORTED_KIND;
        }
        double current;
        try {
            current = switch (group) {
                case ASTRONAUT -> ((Astronaut) person).getSkill(AstronautSkill.valueOf(skillName));
                case SET -> ((SetSpecialist) person).getSkill(SetSkill.valueOf(skillName));
                case MISSION_CONTROL -> ((MissionControlOfficer) person).getSkill(MissionControlSkill.valueOf(skillName));
            };
        } catch (IllegalArgumentException e) {
            return StartTrainingOutcome.UNKNOWN_SKILL;
        }
        if (kind == TrainingKind.PERMANENT && targetLevel <= current) {
            return StartTrainingOutcome.TARGET_NOT_ABOVE_SKILL;
        }
        switch (group) {
            case ASTRONAUT -> ((Astronaut) person).setTrainingTargetSkill(AstronautSkill.valueOf(skillName));
            case SET -> ((SetSpecialist) person).setTrainingTargetSkill(SetSkill.valueOf(skillName));
            case MISSION_CONTROL ->
                ((MissionControlOfficer) person).setTrainingTargetSkill(MissionControlSkill.valueOf(skillName));
        }
        person.setTrainingKind(kind);
        person.setTrainingSeasonsCompleted(0);
        person.setTrainingSeasonsTotal(TrainingProgress.ADVANCED_TRAINING_SEASONS);
        person.setTrainingTargetLevel(kind == TrainingKind.PERMANENT ? targetLevel : 0);
        person.setAssignmentState(AssignmentState.IN_TRAINING);
        return StartTrainingOutcome.STARTED;
    }

    /**
     * Assigns an idle SET specialist to R&amp;D on an owned, active component, in its
     * lowest free slot (slot 0 earns the most, {@code baspm.md} &sect;10.8).
     *
     * @param snapshot     the player; mutated only on success
     * @param specialistId the specialist
     * @param componentId  the component
     * @param catalog      the content catalog
     * @return the outcome
     */
    public static ResearchOutcome assignResearch(PlayerSnapshot snapshot, String specialistId, String componentId,
            ContentCatalog catalog) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(specialistId, "specialistId must not be null");
        Objects.requireNonNull(componentId, "componentId must not be null");
        Objects.requireNonNull(catalog, "catalog must not be null");
        SetSpecialist specialist = find(snapshot.getSetSpecialists(), specialistId);
        if (specialist == null) {
            return ResearchOutcome.UNKNOWN_SPECIALIST;
        }
        if (specialist.getAssignmentState() != AssignmentState.IDLE || specialist.getUnavailableSeasons() > 0) {
            return ResearchOutcome.WRONG_STATE;
        }
        OwnedMissionComponent owned = snapshot.getMissionComponents().get(componentId);
        MissionComponent component = catalog.missionComponent(componentId).orElse(null);
        if (owned == null || !owned.isActive() || component == null) {
            return ResearchOutcome.COMPONENT_NOT_AVAILABLE;
        }
        // A revealed Perfectionist on the team, or the one being assigned, raises the cap (design.md 2.2(d));
        // only revealed traits count, so a refusal never gives a hidden trait away.
        if (owned.getCurrentReliability() >= TraitEffects.effectiveCap(snapshot, component, List.of(specialist), true)) {
            return ResearchOutcome.AT_RD_CEILING;
        }
        Set<Integer> taken = new HashSet<>();
        for (SetSpecialist other : snapshot.getSetSpecialists()) {
            if (componentId.equals(other.getRdTargetComponentId()) && other.getRdSlot() != null) {
                taken.add(other.getRdSlot());
            }
        }
        for (int slot = 0; slot < MissionComponent.RD_SLOT_COUNT; slot++) {
            if (!taken.contains(slot)) {
                specialist.setRdTargetComponentId(componentId);
                specialist.setRdSlot(slot);
                specialist.setAssignmentState(AssignmentState.ASSIGNED);
                return ResearchOutcome.ASSIGNED;
            }
        }
        return ResearchOutcome.NO_FREE_SLOT;
    }

    /**
     * Takes a SET specialist off R&amp;D, back to idle - the inverse of
     * {@link #assignResearch}.
     *
     * @param snapshot     the player; mutated only on success
     * @param specialistId the specialist
     * @return the outcome
     */
    public static ResearchOutcome releaseResearch(PlayerSnapshot snapshot, String specialistId) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(specialistId, "specialistId must not be null");
        SetSpecialist specialist = find(snapshot.getSetSpecialists(), specialistId);
        if (specialist == null) {
            return ResearchOutcome.UNKNOWN_SPECIALIST;
        }
        if (specialist.getAssignmentState() != AssignmentState.ASSIGNED) {
            return ResearchOutcome.WRONG_STATE;
        }
        specialist.setAssignmentState(AssignmentState.IDLE);
        specialist.setRdTargetComponentId(null);
        specialist.setRdSlot(null);
        return ResearchOutcome.RELEASED;
    }

    private static List<? extends Personnel> roster(PlayerSnapshot snapshot, PersonnelGroup group) {
        return switch (group) {
            case ASTRONAUT -> snapshot.getAstronauts();
            case SET -> snapshot.getSetSpecialists();
            case MISSION_CONTROL -> snapshot.getMissionControlOfficers();
        };
    }

    private static <P extends Personnel> P find(List<P> roster, String id) {
        for (P person : roster) {
            if (person.getId().equals(id)) {
                return person;
            }
        }
        return null;
    }
}
