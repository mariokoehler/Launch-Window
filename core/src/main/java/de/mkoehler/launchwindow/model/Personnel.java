package de.mkoehler.launchwindow.model;

import java.util.Objects;

/**
 * <p>Common, mutable state shared by every personnel group ({@link Astronaut},
 * {@link SetSpecialist}, {@link MissionControlOfficer}) - identity, age,
 * morale, learning potential, and current assignment. See {@code design.md}
 * &sect;2.2.</p>
 *
 * @author Mario Koehler
 */
public abstract class Personnel {

    private String id;
    private String name;
    private int age;
    private int agingProgressSeasons;
    private int morale;
    private int learningPotential;
    private AssignmentState assignmentState = AssignmentState.IDLE;
    private TrainingKind trainingKind = TrainingKind.NONE;
    private int trainingSeasonsCompleted;
    private int trainingSeasonsTotal;
    private double trainingTargetLevel;
    private String assignedMissionId;
    private double salary;

    /**
     * No-argument constructor required for Jackson deserialization.
     */
    protected Personnel() {
    }

    /**
     * Creates a new personnel record with the given identity.
     *
     * @param id                stable identifier, unique within a player's roster
     * @param name              display name
     * @param age               age in years
     * @param morale            current morale
     * @param learningPotential the employee's own learning-potential stat,
     *                          driving how quickly their skills improve
     */
    protected Personnel(String id, String name, int age, int morale, int learningPotential) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.age = age;
        this.morale = morale;
        this.learningPotential = learningPotential;
    }

    /**
     * Returns this record's stable identifier, unique within a player's roster.
     *
     * @return the identifier
     */
    public String getId() {
        return id;
    }

    /**
     * Sets this record's stable identifier.
     *
     * @param id the identifier
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns this employee's display name.
     *
     * @return the display name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets this employee's display name.
     *
     * @param name the display name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns this employee's age in years.
     *
     * @return the age
     */
    public int getAge() {
        return age;
    }

    /**
     * Sets this employee's age in years.
     *
     * @param age the age
     */
    public void setAge(int age) {
        this.age = age;
    }

    /**
     * Returns how many seasons of aging have accumulated toward this
     * employee's next whole-year {@link #getAge()} increment - internal
     * bookkeeping for the rules engine's season-advance pass (one turn is
     * one BASPM-style season, a quarter year, see {@code design.md}
     * &sect;1/&sect;3.6; {@code age} itself stays whole years rather than
     * carrying a fraction, so this counter rolls it over every 4th season).
     * Not meant to be read or set outside that pass; public only because
     * Jackson needs a bean accessor.
     *
     * @return the accumulated aging progress, 0-3
     */
    public int getAgingProgressSeasons() {
        return agingProgressSeasons;
    }

    /**
     * Sets the accumulated aging progress. See {@link #getAgingProgressSeasons()}.
     *
     * @param agingProgressSeasons the accumulated aging progress, 0-3
     */
    public void setAgingProgressSeasons(int agingProgressSeasons) {
        this.agingProgressSeasons = agingProgressSeasons;
    }

    /**
     * Returns this employee's current morale.
     *
     * @return the morale
     */
    public int getMorale() {
        return morale;
    }

    /**
     * Sets this employee's current morale.
     *
     * @param morale the morale
     */
    public void setMorale(int morale) {
        this.morale = morale;
    }

    /**
     * Returns this employee's learning-potential stat.
     *
     * @return the learning potential
     */
    public int getLearningPotential() {
        return learningPotential;
    }

    /**
     * Sets this employee's learning-potential stat.
     *
     * @param learningPotential the learning potential
     */
    public void setLearningPotential(int learningPotential) {
        this.learningPotential = learningPotential;
    }

    /**
     * Returns what this employee is currently doing.
     *
     * @return the assignment state
     */
    public AssignmentState getAssignmentState() {
        return assignmentState;
    }

    /**
     * Sets what this employee is currently doing.
     *
     * @param assignmentState the assignment state
     */
    public void setAssignmentState(AssignmentState assignmentState) {
        this.assignmentState = Objects.requireNonNull(assignmentState, "assignmentState must not be null");
    }

    /**
     * Returns what kind of training this employee is currently undergoing,
     * meaningful only while {@link #getAssignmentState()} is
     * {@link AssignmentState#IN_TRAINING} - see {@code design.md} &sect;3.4's
     * field proposal.
     *
     * @return the training kind, {@link TrainingKind#NONE} if not training
     */
    public TrainingKind getTrainingKind() {
        return trainingKind;
    }

    /**
     * Sets what kind of training this employee is currently undergoing.
     *
     * @param trainingKind the training kind
     */
    public void setTrainingKind(TrainingKind trainingKind) {
        this.trainingKind = Objects.requireNonNull(trainingKind, "trainingKind must not be null");
    }

    /**
     * Returns how many seasons of the current training round have been
     * completed so far. Meaningless when {@link #getTrainingKind()} is
     * {@link TrainingKind#NONE}.
     *
     * @return the seasons completed
     */
    public int getTrainingSeasonsCompleted() {
        return trainingSeasonsCompleted;
    }

    /**
     * Sets how many seasons of the current training round have been
     * completed so far.
     *
     * @param trainingSeasonsCompleted the seasons completed
     */
    public void setTrainingSeasonsCompleted(int trainingSeasonsCompleted) {
        this.trainingSeasonsCompleted = trainingSeasonsCompleted;
    }

    /**
     * Returns the current training round's total length in seasons.
     * Meaningless for {@link TrainingKind#NONE}. For
     * {@link TrainingKind#PERMANENT}, each individual round is the same
     * length as {@link TrainingKind#ADVANCED} - {@code PERMANENT} simply
     * re-enrolls into another round automatically on completion, rather
     * than having its own distinct round length (see {@code design.md}
     * &sect;3.4).
     *
     * @return the total seasons
     */
    public int getTrainingSeasonsTotal() {
        return trainingSeasonsTotal;
    }

    /**
     * Sets the current training round's total length in seasons.
     *
     * @param trainingSeasonsTotal the total seasons
     */
    public void setTrainingSeasonsTotal(int trainingSeasonsTotal) {
        this.trainingSeasonsTotal = trainingSeasonsTotal;
    }

    /**
     * Returns the skill level {@link TrainingKind#PERMANENT} training
     * re-enrolls toward - meaningless for every other {@link #getTrainingKind()}.
     * Not tied to any particular skill enum type, unlike
     * {@code trainingTargetSkill} (declared per-subclass, since the three
     * groups' skill enums differ) - a plain target *level* has no such
     * type dependency, so it lives here on the shared base instead. See
     * {@code design.md} &sect;3.4.
     *
     * @return the target skill level
     */
    public double getTrainingTargetLevel() {
        return trainingTargetLevel;
    }

    /**
     * Sets the skill level {@link TrainingKind#PERMANENT} training
     * re-enrolls toward.
     *
     * @param trainingTargetLevel the target skill level
     */
    public void setTrainingTargetLevel(double trainingTargetLevel) {
        this.trainingTargetLevel = trainingTargetLevel;
    }

    /**
     * Returns this employee's salary, charged every season they are on the
     * roster - {@code design.md} &sect;2.1(d). Drawn once at creation from
     * BASPM's own 25-60 range, so a candidate's salary is fixed from the moment
     * the candidate appears in a pool.
     *
     * @return the per-season salary, never negative
     */
    public double getSalary() {
        return salary;
    }

    /**
     * Sets this employee's per-season salary.
     *
     * @param salary the salary to set, never negative
     * @throws IllegalArgumentException if {@code salary} is negative
     */
    public void setSalary(double salary) {
        if (salary < 0) {
            throw new IllegalArgumentException("salary must not be negative: " + salary);
        }
        this.salary = salary;
    }

    /**
     * Returns the {@link InProgressMission#getId()} this employee is
     * currently assigned to - meaningful only for an {@link Astronaut}/
     * {@link MissionControlOfficer} while {@link #getAssignmentState()} is
     * {@link AssignmentState#ASSIGNED} (see that enum's own Javadoc); a
     * {@link SetSpecialist}'s {@code ASSIGNED} state never sets this,
     * using {@link SetSpecialist#getRdTargetComponentId()} instead. Not
     * declared on {@link SetSpecialist} at all, unlike
     * {@code trainingTargetSkill} - a plain mission-instance id has no
     * per-subclass skill-enum dependency, so it lives here on the shared
     * base.
     *
     * @return the assigned mission instance id, or {@code null} if not
     *     assigned to a mission
     */
    public String getAssignedMissionId() {
        return assignedMissionId;
    }

    /**
     * Sets the mission instance id this employee is currently assigned to.
     *
     * @param assignedMissionId the assigned mission instance id
     */
    public void setAssignedMissionId(String assignedMissionId) {
        this.assignedMissionId = assignedMissionId;
    }
}
