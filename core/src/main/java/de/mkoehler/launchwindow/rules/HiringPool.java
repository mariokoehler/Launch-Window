package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.AstronautSkill;
import de.mkoehler.launchwindow.model.MissionControlOfficer;
import de.mkoehler.launchwindow.model.MissionControlSkill;
import de.mkoehler.launchwindow.model.Personnel;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSkill;
import de.mkoehler.launchwindow.model.SetSpecialist;
import de.mkoehler.launchwindow.model.TrainingKind;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

/**
 * <p>The hiring-candidate pool: the fourth M2 rules-engine slice (see
 * {@code design.md} &sect;5 M2, &sect;2.2(a)/(c), and &sect;3.4's field
 * proposal). Headless, per {@code design.md} &sect;3.10 - no persistence/
 * network/libGDX dependency.</p>
 *
 * <p><b>{@link #refreshCandidates} replaces each group's entire candidate
 * pool with a fresh batch</b> ({@link #HIRING_BATCH_SIZE} each), quality fed
 * by that group's own current average roster morale (`baspm.md` &sect;9.4's
 * mechanic, re-derived numbers per {@code design.md} &sect;1.2) via four
 * bands - {@link #POOR_MORALE_CEILING}/{@link #AVERAGE_MORALE_CEILING}/
 * {@link #GOOD_MORALE_CEILING}, above which is "Excellent." An empty roster
 * falls back to {@link #EMPTY_ROSTER_FALLBACK_MORALE} rather than an
 * undefined average.</p>
 *
 * <p><b>Composition order for whoever wires this into one season pass
 * (M4):</b> journaled player actions (including {@link #hire}) first, then
 * {@link PersonnelLifecycle}, then {@link TrainingProgress}, then
 * {@link ResearchProgress}, then {@link #refreshCandidates} last - a
 * player can then always hire from the exact pool they were shown, and a
 * season's refresh never discards a candidate the same season's own
 * actions just hired. {@code TrainingProgress} before {@code ResearchProgress}
 * specifically (not the reverse) is also what makes their random draws
 * reproducible from a seed, since both draw from the same shared source.</p>
 *
 * <p><b>{@link #hire} is a plain headless operation, not tied to any
 * action-submission system</b> - same posture as {@link TrainingProgress}
 * "assumes an already-started assignment": there is no player-action
 * dispatch path yet, so this is a method a future action handler will call,
 * not built as part of this slice. A hire can fail for ordinary async-game
 * reasons (a stale candidate id, a full roster) - see {@link HireOutcome} -
 * so it returns a result rather than throwing, and leaves the snapshot
 * unchanged on failure.</p>
 *
 * <p><b>New hires start {@link TrainingKind#BASIC} training automatically</b>
 * (user decision, 2026-09-30), on a random skill from their own group -
 * which specific skill to start on isn't specified by that decision, so
 * this is this class's own unconfirmed default, same status as every other
 * numeric constant below.</p>
 *
 * <p><b>All the numeric constants below are Launch Window's own values, not
 * BASPM's</b> (unconfirmed defaults, {@code design.md} &sect;2.2(c),
 * 2026-09-30, pending bot-harness tuning).</p>
 *
 * @author Mario Koehler
 */
public final class HiringPool {

    /** How many fresh candidates each group's pool holds after a refresh. */
    public static final int HIRING_BATCH_SIZE = 6;

    /** At or below this average roster morale, candidate quality is "Poor." */
    public static final int POOR_MORALE_CEILING = 44;

    /** Above {@link #POOR_MORALE_CEILING} and at or below this, candidate quality is "Average." */
    public static final int AVERAGE_MORALE_CEILING = 64;

    /** Above {@link #AVERAGE_MORALE_CEILING} and at or below this, candidate quality is "Good." Above it, "Excellent." */
    public static final int GOOD_MORALE_CEILING = 84;

    /**
     * The average roster morale assumed when a group's roster is empty
     * (nothing to average) - lands solidly in the "Average" band rather
     * than treating an empty roster as morale 0.
     */
    public static final int EMPTY_ROSTER_FALLBACK_MORALE = 50;

    /** Candidate skill range, "Poor" band. */
    public static final double POOR_SKILL_MIN = 10.0;
    public static final double POOR_SKILL_MAX = 40.0;
    /** Candidate skill range, "Average" band. */
    public static final double AVERAGE_SKILL_MIN = 25.0;
    public static final double AVERAGE_SKILL_MAX = 60.0;
    /** Candidate skill range, "Good" band. */
    public static final double GOOD_SKILL_MIN = 40.0;
    public static final double GOOD_SKILL_MAX = 75.0;
    /** Candidate skill range, "Excellent" band. */
    public static final double EXCELLENT_SKILL_MIN = 55.0;
    public static final double EXCELLENT_SKILL_MAX = 90.0;

    /** The youngest age a freshly generated candidate can have. */
    public static final int CANDIDATE_MIN_AGE = 22;

    /** The oldest age a freshly generated candidate can have. */
    public static final int CANDIDATE_MAX_AGE = 40;

    /** The lowest learning-potential a freshly generated candidate can have - independent of quality band. */
    public static final int CANDIDATE_MIN_LEARNING_POTENTIAL = 20;

    /** The highest learning-potential a freshly generated candidate can have. */
    public static final int CANDIDATE_MAX_LEARNING_POTENTIAL = 80;

    /**
     * The lowest starting morale a freshly generated candidate can have -
     * the new employee's own attitude on joining, independent of the
     * hiring group's own morale (which only gates skill quality, not the
     * new hire's morale).
     */
    public static final int NEW_HIRE_MIN_STARTING_MORALE = 55;

    /** The highest starting morale a freshly generated candidate can have. */
    public static final int NEW_HIRE_MAX_STARTING_MORALE = 75;

    /** How many astronauts a roster can hold at once. */
    public static final int ASTRONAUT_ROSTER_CAPACITY = 8;

    /** How many SET specialists a roster can hold at once. */
    public static final int SET_ROSTER_CAPACITY = 12;

    /** How many Mission Control officers a roster can hold at once. */
    public static final int MISSION_CONTROL_ROSTER_CAPACITY = 8;

    /**
     * Not instantiable; this class only exposes static methods.
     */
    private HiringPool() {
    }

    /**
     * Replaces every group's candidate pool with a fresh
     * {@link #HIRING_BATCH_SIZE}-sized batch, discarding whoever was in it
     * before (the job market moves on, not an accumulating pool). Quality
     * is banded by each group's own current roster morale, independently
     * per group.
     *
     * @param snapshot the player snapshot to refresh; mutated in place
     * @param random   the random source candidates are generated from
     * @throws NullPointerException if {@code snapshot} or {@code random} is
     *                               {@code null}
     */
    public static void refreshCandidates(PlayerSnapshot snapshot, RandomGenerator random) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(random, "random must not be null");

        SkillBand astronautBand = bandFor(averageMorale(snapshot.getAstronauts()));
        List<Astronaut> astronautBatch = new ArrayList<>(HIRING_BATCH_SIZE);
        for (int i = 0; i < HIRING_BATCH_SIZE; i++) {
            Astronaut candidate = new Astronaut(snapshot.allocatePersonnelId(), "Candidate",
                randomAge(random), randomStartingMorale(random), randomLearningPotential(random));
            for (AstronautSkill skill : AstronautSkill.values()) {
                candidate.setSkill(skill, randomSkill(random, astronautBand));
            }
            astronautBatch.add(candidate);
        }
        snapshot.setAstronautCandidates(astronautBatch);

        SkillBand setBand = bandFor(averageMorale(snapshot.getSetSpecialists()));
        List<SetSpecialist> setBatch = new ArrayList<>(HIRING_BATCH_SIZE);
        for (int i = 0; i < HIRING_BATCH_SIZE; i++) {
            SetSpecialist candidate = new SetSpecialist(snapshot.allocatePersonnelId(), "Candidate",
                randomAge(random), randomStartingMorale(random), randomLearningPotential(random));
            for (SetSkill skill : SetSkill.values()) {
                candidate.setSkill(skill, randomSkill(random, setBand));
            }
            setBatch.add(candidate);
        }
        snapshot.setSetSpecialistCandidates(setBatch);

        SkillBand missionControlBand = bandFor(averageMorale(snapshot.getMissionControlOfficers()));
        List<MissionControlOfficer> missionControlBatch = new ArrayList<>(HIRING_BATCH_SIZE);
        for (int i = 0; i < HIRING_BATCH_SIZE; i++) {
            MissionControlOfficer candidate = new MissionControlOfficer(snapshot.allocatePersonnelId(), "Candidate",
                randomAge(random), randomStartingMorale(random), randomLearningPotential(random));
            for (MissionControlSkill skill : MissionControlSkill.values()) {
                candidate.setSkill(skill, randomSkill(random, missionControlBand));
            }
            missionControlBatch.add(candidate);
        }
        snapshot.setMissionControlCandidates(missionControlBatch);
    }

    /**
     * Hires one candidate out of the given group's current pool: removes
     * them from the pool and adds them to the real roster, idle no longer -
     * per the user's 2026-09-30 decision, automatically enrolled into
     * {@link TrainingKind#BASIC} training on a random skill from their own
     * group. Fails without mutating anything if the id isn't in that
     * group's pool, or if the roster is already at capacity.
     *
     * @param snapshot    the player snapshot to hire into; mutated in place on success
     * @param group       which group to hire from
     * @param candidateId the candidate's id, from that group's current pool
     * @param random      the random source the auto-started training's target skill is drawn from
     * @return the outcome
     * @throws NullPointerException if any argument is {@code null}
     */
    public static HireOutcome hire(PlayerSnapshot snapshot, PersonnelGroup group, String candidateId,
            RandomGenerator random) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(group, "group must not be null");
        Objects.requireNonNull(candidateId, "candidateId must not be null");
        Objects.requireNonNull(random, "random must not be null");

        return switch (group) {
            case ASTRONAUT -> hireAstronaut(snapshot, candidateId, random);
            case SET -> hireSetSpecialist(snapshot, candidateId, random);
            case MISSION_CONTROL -> hireMissionControlOfficer(snapshot, candidateId, random);
        };
    }

    private static HireOutcome hireAstronaut(PlayerSnapshot snapshot, String candidateId, RandomGenerator random) {
        if (snapshot.getAstronauts().size() >= ASTRONAUT_ROSTER_CAPACITY) {
            return HireOutcome.ROSTER_FULL;
        }
        List<Astronaut> pool = snapshot.getAstronautCandidates();
        Astronaut candidate = removeById(pool, candidateId);
        if (candidate == null) {
            return HireOutcome.UNKNOWN_CANDIDATE;
        }
        snapshot.setAstronautCandidates(pool);
        AstronautSkill[] skills = AstronautSkill.values();
        AstronautSkill target = skills[random.nextInt(skills.length)];
        startBasicTraining(candidate, TrainingProgress.ASTRONAUT_BASIC_TRAINING_SEASONS);
        candidate.setTrainingTargetSkill(target);
        List<Astronaut> roster = snapshot.getAstronauts();
        roster.add(candidate);
        snapshot.setAstronauts(roster);
        return HireOutcome.HIRED;
    }

    private static HireOutcome hireSetSpecialist(PlayerSnapshot snapshot, String candidateId, RandomGenerator random) {
        if (snapshot.getSetSpecialists().size() >= SET_ROSTER_CAPACITY) {
            return HireOutcome.ROSTER_FULL;
        }
        List<SetSpecialist> pool = snapshot.getSetSpecialistCandidates();
        SetSpecialist candidate = removeById(pool, candidateId);
        if (candidate == null) {
            return HireOutcome.UNKNOWN_CANDIDATE;
        }
        snapshot.setSetSpecialistCandidates(pool);
        SetSkill[] skills = SetSkill.values();
        SetSkill target = skills[random.nextInt(skills.length)];
        startBasicTraining(candidate, TrainingProgress.SET_BASIC_TRAINING_SEASONS);
        candidate.setTrainingTargetSkill(target);
        List<SetSpecialist> roster = snapshot.getSetSpecialists();
        roster.add(candidate);
        snapshot.setSetSpecialists(roster);
        return HireOutcome.HIRED;
    }

    private static HireOutcome hireMissionControlOfficer(PlayerSnapshot snapshot, String candidateId,
            RandomGenerator random) {
        if (snapshot.getMissionControlOfficers().size() >= MISSION_CONTROL_ROSTER_CAPACITY) {
            return HireOutcome.ROSTER_FULL;
        }
        List<MissionControlOfficer> pool = snapshot.getMissionControlCandidates();
        MissionControlOfficer candidate = removeById(pool, candidateId);
        if (candidate == null) {
            return HireOutcome.UNKNOWN_CANDIDATE;
        }
        snapshot.setMissionControlCandidates(pool);
        MissionControlSkill[] skills = MissionControlSkill.values();
        MissionControlSkill target = skills[random.nextInt(skills.length)];
        startBasicTraining(candidate, TrainingProgress.MISSION_CONTROL_BASIC_TRAINING_SEASONS);
        candidate.setTrainingTargetSkill(target);
        List<MissionControlOfficer> roster = snapshot.getMissionControlOfficers();
        roster.add(candidate);
        snapshot.setMissionControlOfficers(roster);
        return HireOutcome.HIRED;
    }

    /**
     * Common bookkeeping for auto-starting a new hire's {@code BASIC}
     * training - everything except the group-typed target skill, which
     * each {@code hire*} overload sets itself right after calling this
     * (a generic {@code Personnel} parameter can't call the per-subclass
     * {@code setTrainingTargetSkill}).
     *
     * @param person                the newly hired employee
     * @param basicTrainingSeasons  that group's own basic-training length
     */
    private static void startBasicTraining(Personnel person, int basicTrainingSeasons) {
        person.setTrainingKind(TrainingKind.BASIC);
        person.setTrainingSeasonsTotal(basicTrainingSeasons);
        person.setTrainingSeasonsCompleted(0);
        person.setAssignmentState(AssignmentState.IN_TRAINING);
    }

    private static <T extends Personnel> T removeById(List<T> pool, String id) {
        for (int i = 0; i < pool.size(); i++) {
            if (pool.get(i).getId().equals(id)) {
                return pool.remove(i);
            }
        }
        return null;
    }

    private static double averageMorale(List<? extends Personnel> roster) {
        if (roster.isEmpty()) {
            return EMPTY_ROSTER_FALLBACK_MORALE;
        }
        double sum = 0;
        for (Personnel person : roster) {
            sum += person.getMorale();
        }
        return sum / roster.size();
    }

    private static SkillBand bandFor(double averageMorale) {
        if (averageMorale <= POOR_MORALE_CEILING) {
            return SkillBand.POOR;
        }
        if (averageMorale <= AVERAGE_MORALE_CEILING) {
            return SkillBand.AVERAGE;
        }
        if (averageMorale <= GOOD_MORALE_CEILING) {
            return SkillBand.GOOD;
        }
        return SkillBand.EXCELLENT;
    }

    private static double randomSkill(RandomGenerator random, SkillBand band) {
        return switch (band) {
            case POOR -> POOR_SKILL_MIN + random.nextDouble() * (POOR_SKILL_MAX - POOR_SKILL_MIN);
            case AVERAGE -> AVERAGE_SKILL_MIN + random.nextDouble() * (AVERAGE_SKILL_MAX - AVERAGE_SKILL_MIN);
            case GOOD -> GOOD_SKILL_MIN + random.nextDouble() * (GOOD_SKILL_MAX - GOOD_SKILL_MIN);
            case EXCELLENT -> EXCELLENT_SKILL_MIN + random.nextDouble() * (EXCELLENT_SKILL_MAX - EXCELLENT_SKILL_MIN);
        };
    }

    private static int randomAge(RandomGenerator random) {
        return CANDIDATE_MIN_AGE + random.nextInt(CANDIDATE_MAX_AGE - CANDIDATE_MIN_AGE + 1);
    }

    private static int randomLearningPotential(RandomGenerator random) {
        return CANDIDATE_MIN_LEARNING_POTENTIAL
            + random.nextInt(CANDIDATE_MAX_LEARNING_POTENTIAL - CANDIDATE_MIN_LEARNING_POTENTIAL + 1);
    }

    private static int randomStartingMorale(RandomGenerator random) {
        return NEW_HIRE_MIN_STARTING_MORALE
            + random.nextInt(NEW_HIRE_MAX_STARTING_MORALE - NEW_HIRE_MIN_STARTING_MORALE + 1);
    }

    /**
     * The four candidate-quality bands, keyed by the hiring group's own
     * average roster morale. See {@link HiringPool}'s own class Javadoc.
     */
    private enum SkillBand {
        POOR, AVERAGE, GOOD, EXCELLENT
    }
}
