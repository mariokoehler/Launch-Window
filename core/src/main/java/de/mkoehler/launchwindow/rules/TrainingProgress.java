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
 * <p>The training countdown: the third M2 rules-engine slice (see
 * {@code design.md} &sect;5 M2, &sect;2.2(a)/(c), and &sect;3.4's field
 * proposal). Resolves all three real {@link TrainingKind}s -
 * {@link TrainingKind#BASIC}/{@link TrainingKind#ADVANCED} as one fixed-
 * length round each, {@link TrainingKind#PERMANENT} as the same
 * {@link #ADVANCED_TRAINING_SEASONS}-long round, automatically re-enrolled
 * every time one completes until {@code trainingTargetLevel} is reached (or
 * the {@link #PERMANENT_TRAINING_GAIN_EPSILON} termination guard fires -
 * see {@link #shouldReenroll}).</p>
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
 * <i>before</i> this class's {@link #advanceSeason}, not after. <b>Reason,
 * updated 2026-09-30 after {@code PersonnelLifecycle}'s own departure-
 * protection rule was reversed</b> (see that class's Javadoc - training no
 * longer protects Astronaut/Mission Control from departure at all, so the
 * order no longer changes *whether* a completing trainee is eligible for
 * the resignation/retirement check): it still changes the <i>morale</i> that
 * check is judged against, because a training round that completes this
 * season flips the employee to {@link AssignmentState#IDLE}. If
 * {@code PersonnelLifecycle} ran second, it would apply the *idle* morale
 * penalty for a season the employee actually spent busy/training, not the
 * busy morale gain - potentially the exact difference between staying and
 * resigning right at the floor. Running {@code PersonnelLifecycle} first
 * judges that season's morale and departure eligibility against the
 * assignment state the employee actually held for most of it. <b>Not
 * guarded by the bot harness's own invariants</b> - its "busy never
 * departs" check only covers {@link AssignmentState#ASSIGNED} now (see
 * {@code PersonnelLifecyclePlayouts}), which this class never sets, so a
 * wrong composition order would not trip it; {@code TrainingProgressTest}'s
 * own composition-order tests are the only thing verifying this order
 * still matters.</p>
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
     * The minimum skill gain a {@link TrainingKind#PERMANENT} round must
     * produce to re-enroll into another one. Without this floor, an
     * employee with 0 learning potential (whose gain is always exactly 0 -
     * see {@link SkillGrowth#diminishingReturnsGain}) or a target level
     * at/above {@link SkillGrowth#SKILL_CAP} (which the gain formula only
     * ever asymptotically approaches, never reaches) would re-enroll
     * forever with no real progress - an infinite loop, not a balance
     * question. Own value, not from BASPM (which has no {@code PERMANENT}-
     * equivalent re-enrollment cap to begin with).
     */
    public static final double PERMANENT_TRAINING_GAIN_EPSILON = 0.05;

    /**
     * Not instantiable; this class only exposes static methods.
     */
    private TrainingProgress() {
    }

    /**
     * Advances one season of training progress for every employee across
     * all three of the given snapshot's rosters who is currently training
     * (any of {@link TrainingKind#BASIC}, {@link TrainingKind#ADVANCED}, or
     * {@link TrainingKind#PERMANENT}), mutating the snapshot in place.
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
     * Whether a training round is genuinely in progress - both
     * {@link AssignmentState#IN_TRAINING} and a real (non-{@link TrainingKind#NONE})
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
     * @return {@code true} if a training round is genuinely active
     */
    private static boolean isActivelyTraining(Personnel person) {
        return person.getAssignmentState() == AssignmentState.IN_TRAINING
            && person.getTrainingKind() != TrainingKind.NONE;
    }

    /**
     * Whether a completed round should re-enroll into another one, rather
     * than finish - only ever {@code true} for {@link TrainingKind#PERMANENT},
     * and only while genuine progress is still being made toward the
     * target level.
     *
     * @param kind        the training kind that just completed a round
     * @param newSkill    the target skill's value after this round's gain
     *                    was applied
     * @param targetLevel the level {@code PERMANENT} training is working
     *                     toward (meaningless for other kinds)
     * @param gain        this round's own gain
     * @return {@code true} if another round should start immediately
     */
    private static boolean shouldReenroll(TrainingKind kind, double newSkill, double targetLevel, double gain) {
        if (kind != TrainingKind.PERMANENT) {
            return false;
        }
        if (newSkill >= targetLevel) {
            return false;
        }
        return gain >= PERMANENT_TRAINING_GAIN_EPSILON;
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
        double currentSkill = astronaut.getSkill(targetSkill);
        double gain = SkillGrowth.diminishingReturnsGain(currentSkill, astronaut.getLearningPotential(), factor);
        double newSkill = currentSkill + gain;
        astronaut.setSkill(targetSkill, newSkill);
        if (shouldReenroll(kind, newSkill, astronaut.getTrainingTargetLevel(), gain)) {
            astronaut.setTrainingSeasonsCompleted(0);
        } else {
            resetTraining(astronaut);
            astronaut.setTrainingTargetSkill(null);
        }
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
        double currentSkill = specialist.getSkill(targetSkill);
        double gain = SkillGrowth.diminishingReturnsGain(currentSkill, specialist.getLearningPotential(), factor);
        double newSkill = currentSkill + gain;
        specialist.setSkill(targetSkill, newSkill);
        if (shouldReenroll(kind, newSkill, specialist.getTrainingTargetLevel(), gain)) {
            specialist.setTrainingSeasonsCompleted(0);
        } else {
            resetTraining(specialist);
            specialist.setTrainingTargetSkill(null);
        }
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
        double currentSkill = officer.getSkill(targetSkill);
        double gain = SkillGrowth.diminishingReturnsGain(currentSkill, officer.getLearningPotential(), factor);
        double newSkill = currentSkill + gain;
        officer.setSkill(targetSkill, newSkill);
        if (shouldReenroll(kind, newSkill, officer.getTrainingTargetLevel(), gain)) {
            officer.setTrainingSeasonsCompleted(0);
        } else {
            resetTraining(officer);
            officer.setTrainingTargetSkill(null);
        }
        events.add(new TrainingCompleted(PersonnelGroup.MISSION_CONTROL, officer.getId(), officer.getName(),
            kind, targetSkill.name(), gain));
    }

    private static void resetTraining(Personnel person) {
        person.setTrainingKind(TrainingKind.NONE);
        person.setTrainingSeasonsCompleted(0);
        person.setTrainingSeasonsTotal(0);
        person.setTrainingTargetLevel(0);
        person.setAssignmentState(AssignmentState.IDLE);
    }
}
