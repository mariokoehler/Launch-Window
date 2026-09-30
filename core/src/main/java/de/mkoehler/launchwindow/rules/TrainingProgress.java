package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.MissionControlOfficer;
import de.mkoehler.launchwindow.model.Personnel;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSpecialist;
import de.mkoehler.launchwindow.model.TrainingKind;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

/**
 * <p>The fixed-length training countdown: the third M2 rules-engine slice
 * (see {@code design.md} &sect;5 M2, &sect;2.2(a)/(c), and &sect;3.4's field
 * proposal). Resolves {@link TrainingKind#BASIC} and {@link TrainingKind#ADVANCED}
 * only - {@link TrainingKind#PERMANENT}'s re-enrollment logic is deliberately
 * out of scope, see {@code design.md} &sect;3.4.</p>
 *
 * <p><b>Assumes an already-started training assignment</b> - this class only
 * ever advances a countdown and resolves completion; nothing in this project
 * yet *starts* one (setting {@code trainingKind}/{@code trainingSeasonsTotal}/
 * a target skill and {@link AssignmentState#IN_TRAINING} is a player-action
 * concern that doesn't exist yet). An employee whose {@code trainingKind} is
 * {@link TrainingKind#NONE} is left untouched.</p>
 *
 * <p><b>Composition order with {@link PersonnelLifecycle}, for whoever wires
 * both into one season pass (M4):</b> run {@link PersonnelLifecycle#advanceSeason}
 * <i>before</i> this class's {@link #advanceSeason}, not after. A training
 * round that completes this season flips the employee to
 * {@link AssignmentState#IDLE} - if {@code PersonnelLifecycle} ran second, it
 * would see that employee as idle in the very season they were actually busy
 * for, and its resignation/retirement checks (idle-only by design) would
 * wrongly apply to them a season early. Running {@code PersonnelLifecycle}
 * first, against the assignment state as it stood going into the season,
 * keeps the "busy is never checked for departure" invariant true for the
 * season a training round completes in, same as any other season.</p>
 *
 * <p>Headless, per {@code design.md} &sect;3.10 - no persistence/network/
 * libGDX dependency. Driven every season by the bot harness
 * ({@code PersonnelLifecyclePlayouts}, {@code dev-tools}); not yet wired
 * into {@code PlayerStateService}/{@code TurnScheduler} (M4).</p>
 *
 * @author Mario Koehler
 */
public final class TrainingProgress {

    /** Fixed length, in seasons, of astronaut basic training. */
    public static final int ASTRONAUT_BASIC_TRAINING_SEASONS = 3;

    /** Fixed length, in seasons, of SET basic training. */
    public static final int SET_BASIC_TRAINING_SEASONS = 1;

    /**
     * Fixed length, in seasons, of Mission Control basic training.
     * Deliberately <b>not</b> 2, BASPM's own value for this group
     * ({@code baspm.md} &sect;9.3) - {@code design.md} &sect;1.2 forbids
     * landing on a BASPM tuning constant verbatim, even by coincidence.
     */
    public static final int MISSION_CONTROL_BASIC_TRAINING_SEASONS = 3;

    /** Fixed length, in seasons, of one advanced-training round, the same for every group. */
    public static final int ADVANCED_TRAINING_SEASONS = 2;

    /**
     * Not instantiable; this class only exposes static methods.
     */
    private TrainingProgress() {
    }

    /**
     * Advances one season of training progress for every employee across
     * all three of the given snapshot's rosters who is currently
     * undergoing {@link TrainingKind#BASIC} or {@link TrainingKind#ADVANCED}
     * training, mutating the snapshot in place.
     *
     * @param snapshot the player snapshot to advance; mutated in place
     * @param random   the random source training-completion gains are drawn
     *                 from (via {@link SkillGrowth#drawTrainingFactor})
     * @return the {@link TrainingCompleted} events this pass caused, in no
     *     particular cross-roster order; empty if nobody completed training
     *     this season
     * @throws NullPointerException if {@code snapshot} or {@code random} is
     *                               {@code null}
     */
    public static List<ResolutionEvent> advanceSeason(PlayerSnapshot snapshot, RandomGenerator random) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(random, "random must not be null");

        List<ResolutionEvent> events = new ArrayList<>();
        for (Astronaut astronaut : snapshot.getAstronauts()) {
            advanceAstronaut(astronaut, random, events);
        }
        for (SetSpecialist specialist : snapshot.getSetSpecialists()) {
            advanceSetSpecialist(specialist, random, events);
        }
        for (MissionControlOfficer officer : snapshot.getMissionControlOfficers()) {
            advanceMissionControlOfficer(officer, random, events);
        }
        return events;
    }

    /**
     * Whether a fixed-length training round is genuinely in progress -
     * both {@link AssignmentState#IN_TRAINING} and a fixed-length
     * {@link TrainingKind} must agree, since the two are independent
     * fields that could otherwise drift apart (e.g. a future pull-out path
     * that sets {@link AssignmentState#IDLE} without also clearing
     * {@code trainingKind} back to {@link TrainingKind#NONE} - checking
     * {@code trainingKind} alone would keep counting that employee down
     * and eventually "complete" training they were actually pulled out of;
     * checking {@link AssignmentState#IN_TRAINING} alone would never
     * advance someone left in that state with no real training kind set,
     * such as {@code PersonnelLifecycleTest}'s own busy-employee fixtures).
     *
     * @param person the employee to check
     * @return {@code true} if a fixed-length round is genuinely active
     */
    private static boolean isActivelyTraining(Personnel person) {
        return person.getAssignmentState() == AssignmentState.IN_TRAINING
            && (person.getTrainingKind() == TrainingKind.BASIC || person.getTrainingKind() == TrainingKind.ADVANCED);
    }

    private static void advanceAstronaut(Astronaut astronaut, RandomGenerator random, List<ResolutionEvent> events) {
        if (!isActivelyTraining(astronaut)) {
            return;
        }
        int completed = astronaut.getTrainingSeasonsCompleted() + 1;
        astronaut.setTrainingSeasonsCompleted(completed);
        if (completed < astronaut.getTrainingSeasonsTotal()) {
            return;
        }
        TrainingKind kind = astronaut.getTrainingKind();
        var targetSkill = astronaut.getTrainingTargetSkill();
        double factor = SkillGrowth.drawTrainingFactor(random);
        double gain = SkillGrowth.diminishingReturnsGain(
            astronaut.getSkill(targetSkill), astronaut.getLearningPotential(), factor);
        astronaut.setSkill(targetSkill, astronaut.getSkill(targetSkill) + gain);
        resetTraining(astronaut);
        astronaut.setTrainingTargetSkill(null);
        events.add(new TrainingCompleted(PersonnelGroup.ASTRONAUT, astronaut.getId(), astronaut.getName(),
            kind, targetSkill.name(), gain));
    }

    private static void advanceSetSpecialist(SetSpecialist specialist, RandomGenerator random, List<ResolutionEvent> events) {
        if (!isActivelyTraining(specialist)) {
            return;
        }
        int completed = specialist.getTrainingSeasonsCompleted() + 1;
        specialist.setTrainingSeasonsCompleted(completed);
        if (completed < specialist.getTrainingSeasonsTotal()) {
            return;
        }
        TrainingKind kind = specialist.getTrainingKind();
        var targetSkill = specialist.getTrainingTargetSkill();
        double factor = SkillGrowth.drawTrainingFactor(random);
        double gain = SkillGrowth.diminishingReturnsGain(
            specialist.getSkill(targetSkill), specialist.getLearningPotential(), factor);
        specialist.setSkill(targetSkill, specialist.getSkill(targetSkill) + gain);
        resetTraining(specialist);
        specialist.setTrainingTargetSkill(null);
        events.add(new TrainingCompleted(PersonnelGroup.SET, specialist.getId(), specialist.getName(),
            kind, targetSkill.name(), gain));
    }

    private static void advanceMissionControlOfficer(MissionControlOfficer officer, RandomGenerator random,
            List<ResolutionEvent> events) {
        if (!isActivelyTraining(officer)) {
            return;
        }
        int completed = officer.getTrainingSeasonsCompleted() + 1;
        officer.setTrainingSeasonsCompleted(completed);
        if (completed < officer.getTrainingSeasonsTotal()) {
            return;
        }
        TrainingKind kind = officer.getTrainingKind();
        var targetSkill = officer.getTrainingTargetSkill();
        double factor = SkillGrowth.drawTrainingFactor(random);
        double gain = SkillGrowth.diminishingReturnsGain(
            officer.getSkill(targetSkill), officer.getLearningPotential(), factor);
        officer.setSkill(targetSkill, officer.getSkill(targetSkill) + gain);
        resetTraining(officer);
        officer.setTrainingTargetSkill(null);
        events.add(new TrainingCompleted(PersonnelGroup.MISSION_CONTROL, officer.getId(), officer.getName(),
            kind, targetSkill.name(), gain));
    }

    private static void resetTraining(Personnel person) {
        person.setTrainingKind(TrainingKind.NONE);
        person.setTrainingSeasonsCompleted(0);
        person.setTrainingSeasonsTotal(0);
        person.setAssignmentState(AssignmentState.IDLE);
    }
}
