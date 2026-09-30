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
}
