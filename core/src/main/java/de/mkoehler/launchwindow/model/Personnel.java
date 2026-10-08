package de.mkoehler.launchwindow.model;

import java.util.ArrayList;
import java.util.List;
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

    /** {@link #getIntelFind()}: a background check. */
    public static final String FIND_CHECK = "CHECK";
    /** {@link #getIntelFind()}: the passive find when the pool was drawn. */
    public static final String FIND_PASSIVE = "PASSIVE";

    private String id;
    private String name;
    private String portraitId;
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
    private int unavailableSeasons;
    private List<TraitState> traits = new ArrayList<>();
    private String hint;
    private String intelFind;
    private boolean intelExhausted;
    private int loyalty = 50;
    private int tenureSeasons;
    private Ambition ambition;
    private boolean ambitionPending;
    private int disappointedSeasons;
    private String stintComponentId;
    private int stintSeasons;
    private double stintGain;
    private List<CareerEntry> career = new ArrayList<>();
    private int workSeasons;
    private int seasonsSinceTraining;
    private String projectId;
    private int projectSeasons;
    private Integer lastApproachedTurn;
    private Retention retention = Retention.DEFAULT;

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
     * Returns this employee's portrait, an id from {@code personnel/portraits.json} such as
     * {@code nasa_001}. Their name and starting age were chosen to match it.
     *
     * @return the portrait id, or {@code null} for someone created without one (tests, fixtures)
     */
    public String getPortraitId() {
        return portraitId;
    }

    /**
     * Sets this employee's portrait.
     *
     * @param portraitId the portrait id, or {@code null}
     */
    public void setPortraitId(String portraitId) {
        this.portraitId = portraitId;
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
     * Returns how many more launch windows this employee is unavailable for.
     *
     * @return the remaining windows, zero when available
     */
    public int getUnavailableSeasons() {
        return unavailableSeasons;
    }

    /**
     * Sets how many more launch windows this employee is unavailable for - a random
     * "unavailable" event's effect, {@code design.md} &sect;2.7. Launches skip an
     * unavailable employee, as they skip one who isn't idle.
     *
     * @param unavailableSeasons the remaining windows, zero or more
     * @throws IllegalArgumentException if {@code unavailableSeasons} is negative
     */
    public void setUnavailableSeasons(int unavailableSeasons) {
        if (unavailableSeasons < 0) {
            throw new IllegalArgumentException("unavailableSeasons must not be negative: " + unavailableSeasons);
        }
        this.unavailableSeasons = unavailableSeasons;
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

    /**
     * Returns this person's traits, shown and hidden ({@code design.md} &sect;2.2(d)). The player
     * only ever sees the revealed ones: the server strips the rest before sending the state.
     *
     * @return the traits, in the order they were drawn
     */
    public List<TraitState> getTraits() {
        return traits;
    }

    /**
     * Sets this person's traits.
     *
     * @param traits the traits
     */
    public void setTraits(List<TraitState> traits) {
        this.traits = traits == null ? new ArrayList<>() : new ArrayList<>(traits);
    }

    /**
     * Returns the hint a former colleague gave when they were a candidate, such as "driven".
     *
     * @return the hint's id, or {@code null} for someone hired before hints existed
     */
    public String getHint() {
        return hint;
    }

    /**
     * Sets the hint.
     *
     * @param hint the hint's id
     */
    public void setHint(String hint) {
        this.hint = hint;
    }

    /**
     * Returns how the Intelligence Office looked into this candidate ({@code design.md} &sect;2.9):
     * {@link #FIND_CHECK} for a background check, {@link #FIND_PASSIVE} for the passive find, or
     * {@code null} if it hasn't. With no revealed trait it means "no notable traits".
     *
     * @return the find, or {@code null}
     */
    public String getIntelFind() {
        return intelFind;
    }

    /**
     * Sets how the Intelligence Office looked into this candidate.
     *
     * @param intelFind the find, or {@code null}
     */
    public void setIntelFind(String intelFind) {
        this.intelFind = intelFind;
    }

    /**
     * Returns whether a background check found no further trait to reveal.
     *
     * @return whether there's nothing more to find
     */
    public boolean isIntelExhausted() {
        return intelExhausted;
    }

    /**
     * Sets whether a background check found no further trait to reveal.
     *
     * @param intelExhausted whether there's nothing more to find
     */
    public void setIntelExhausted(boolean intelExhausted) {
        this.intelExhausted = intelExhausted;
    }

    /**
     * Returns how many seasons this person has worked in a team (R&amp;D or a mission in flight),
     * which is what reveals their traits.
     *
     * @return the work seasons
     */
    public int getWorkSeasons() {
        return workSeasons;
    }

    /**
     * Sets the work seasons.
     *
     * @param workSeasons the work seasons
     */
    public void setWorkSeasons(int workSeasons) {
        this.workSeasons = workSeasons;
    }

    /**
     * Returns the seasons since this person last trained, for an Ambitious person's patience.
     *
     * @return the seasons
     */
    public int getSeasonsSinceTraining() {
        return seasonsSinceTraining;
    }

    /**
     * Sets the seasons since this person last trained.
     *
     * @param seasonsSinceTraining the seasons
     */
    public void setSeasonsSinceTraining(int seasonsSinceTraining) {
        this.seasonsSinceTraining = seasonsSinceTraining;
    }

    /**
     * Returns what this person worked on last: a component (R&amp;D) or a mission configuration.
     *
     * @return the id, or {@code null} before their first work season
     */
    public String getProjectId() {
        return projectId;
    }

    /**
     * Sets what this person worked on last.
     *
     * @param projectId the id
     */
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    /**
     * Returns how many work seasons in a row this person has spent on {@link #getProjectId()}.
     *
     * @return the seasons
     */
    public int getProjectSeasons() {
        return projectSeasons;
    }

    /**
     * Sets the work seasons in a row on the current project.
     *
     * @param projectSeasons the seasons
     */
    public void setProjectSeasons(int projectSeasons) {
        this.projectSeasons = projectSeasons;
    }

    /**
     * Returns how attached the person is to the agency, 0 to 100 (phase 4); resists poaching later.
     *
     * @return the loyalty
     */
    public int getLoyalty() {
        return loyalty;
    }

    /**
     * Sets how attached the person is to the agency, 0 to 100 (phase 4); resists poaching later.
     *
     * @param loyalty the loyalty
     */
    public void setLoyalty(int loyalty) {
        this.loyalty = loyalty;
    }

    /**
     * Returns the game turn a rival last made the person an offer (poaching, {@code design.md}
     * &sect;2.2(d)), or {@code null} if no one ever has.
     *
     * @return the turn, or {@code null}
     */
    public Integer getLastApproachedTurn() {
        return lastApproachedTurn;
    }

    /**
     * Sets the game turn a rival last made the person an offer.
     *
     * @param lastApproachedTurn the turn, or {@code null}
     */
    public void setLastApproachedTurn(Integer lastApproachedTurn) {
        this.lastApproachedTurn = lastApproachedTurn;
    }

    /**
     * Returns how the agency answers a rival's offer to this person.
     *
     * @return the standing order, never {@code null}
     */
    public Retention getRetention() {
        return retention;
    }

    /**
     * Sets how the agency answers a rival's offer to this person.
     *
     * @param retention the standing order, or {@code null} for the agency default
     */
    public void setRetention(Retention retention) {
        this.retention = retention == null ? Retention.DEFAULT : retention;
    }

    /**
     * Returns the seasons the person has been with the agency.
     *
     * @return the seasons
     */
    public int getTenureSeasons() {
        return tenureSeasons;
    }

    /**
     * Sets the seasons the person has been with the agency.
     *
     * @param tenureSeasons the seasons
     */
    public void setTenureSeasons(int tenureSeasons) {
        this.tenureSeasons = tenureSeasons;
    }

    /**
     * Returns the ambition the person has said, or {@code null}.
     *
     * @return the ambition, or {@code null}
     */
    public Ambition getAmbition() {
        return ambition;
    }

    /**
     * Sets the ambition the person has said, or {@code null}.
     *
     * @param ambition the ambition, or {@code null}
     */
    public void setAmbition(Ambition ambition) {
        this.ambition = ambition;
    }

    /**
     * Returns whether the person will say an ambition when the time comes; hidden from the client.
     *
     * @return whether one is coming
     */
    public boolean isAmbitionPending() {
        return ambitionPending;
    }

    /**
     * Sets whether the person will say an ambition when the time comes; hidden from the client.
     *
     * @param ambitionPending whether one is coming
     */
    public void setAmbitionPending(boolean ambitionPending) {
        this.ambitionPending = ambitionPending;
    }

    /**
     * Returns the seasons left of disappointment after an ambition ran out.
     *
     * @return the seasons
     */
    public int getDisappointedSeasons() {
        return disappointedSeasons;
    }

    /**
     * Sets the seasons left of disappointment after an ambition ran out.
     *
     * @param disappointedSeasons the seasons
     */
    public void setDisappointedSeasons(int disappointedSeasons) {
        this.disappointedSeasons = disappointedSeasons;
    }

    /**
     * Returns the component of the R&amp;D stint running now, or {@code null}.
     *
     * @return the component, or {@code null}
     */
    public String getStintComponentId() {
        return stintComponentId;
    }

    /**
     * Sets the component of the R&amp;D stint running now, or {@code null}.
     *
     * @param stintComponentId the component, or {@code null}
     */
    public void setStintComponentId(String stintComponentId) {
        this.stintComponentId = stintComponentId;
    }

    /**
     * Returns the seasons of the R&amp;D stint running now.
     *
     * @return the seasons
     */
    public int getStintSeasons() {
        return stintSeasons;
    }

    /**
     * Sets the seasons of the R&amp;D stint running now.
     *
     * @param stintSeasons the seasons
     */
    public void setStintSeasons(int stintSeasons) {
        this.stintSeasons = stintSeasons;
    }

    /**
     * Returns the reliability the person has added in the R&amp;D stint running now.
     *
     * @return the reliability
     */
    public double getStintGain() {
        return stintGain;
    }

    /**
     * Sets the reliability the person has added in the R&amp;D stint running now.
     *
     * @param stintGain the reliability
     */
    public void setStintGain(double stintGain) {
        this.stintGain = stintGain;
    }

    /**
     * Returns the person's career record, oldest first (phase 4).
     *
     * @return the record, live
     */
    public List<CareerEntry> getCareer() {
        return career;
    }

    /**
     * Sets the career record.
     *
     * @param career the record
     */
    public void setCareer(List<CareerEntry> career) {
        this.career = career == null ? new ArrayList<>() : new ArrayList<>(career);
    }
}
