package de.mkoehler.launchwindow.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * <p>A Scientist/Engineer/Technician (SET) employee - drives R&amp;D on
 * mission components, holds one competency value per {@link SetSkill}.</p>
 *
 * <p>The skill map is a plain {@link LinkedHashMap}, not an
 * {@link java.util.EnumMap} - Kryo's wire serialization (see
 * {@code de.mkoehler.launchwindow.net.MessageRegistry}) needs a
 * no-arg-constructible map, which {@code EnumMap} isn't.</p>
 *
 * <p>Competency values are {@code double}, not {@code int} - SET's flat
 * per-season R&amp;D growth ({@code design.md} &sect;2.2(c)) can be smaller
 * than 1 and would round away to nothing on an integer scale.</p>
 *
 * @author Mario Koehler
 */
public class SetSpecialist extends Personnel {

    private Map<SetSkill, Double> skills = new LinkedHashMap<>();
    private SetSkill trainingTargetSkill;
    private String rdTargetComponentId;
    private Integer rdSlot;

    /**
     * No-argument constructor required for Jackson deserialization.
     */
    public SetSpecialist() {
        super();
    }

    /**
     * Creates a new SET specialist with the given identity.
     *
     * @param id                stable identifier, unique within a player's roster
     * @param name              display name
     * @param age               age in years
     * @param morale            current morale
     * @param learningPotential the specialist's own learning-potential stat
     */
    public SetSpecialist(String id, String name, int age, int morale, int learningPotential) {
        super(id, name, age, morale, learningPotential);
    }

    /**
     * Returns this specialist's competency values, keyed by skill. Skills
     * absent from the map are undefined, not implicitly zero.
     *
     * @return the skill map
     */
    public Map<SetSkill, Double> getSkills() {
        return skills;
    }

    /**
     * Replaces this specialist's entire skill map.
     *
     * @param skills the skill map
     */
    public void setSkills(Map<SetSkill, Double> skills) {
        Objects.requireNonNull(skills, "skills must not be null");
        this.skills = new LinkedHashMap<>(skills);
    }

    /**
     * Returns this specialist's competency value for one skill.
     *
     * @param skill the skill to look up
     * @return the competency value, or {@code 0} if not set
     */
    public double getSkill(SetSkill skill) {
        return skills.getOrDefault(skill, 0.0);
    }

    /**
     * Sets this specialist's competency value for one skill.
     *
     * @param skill the skill to set
     * @param value the competency value
     */
    public void setSkill(SetSkill skill, double value) {
        skills.put(skill, value);
    }

    /**
     * Returns the skill this specialist's current training round is working
     * toward. Meaningless when {@link #getTrainingKind()} is
     * {@link TrainingKind#NONE} - see {@code design.md} &sect;3.4's field
     * proposal.
     *
     * @return the training target skill, or {@code null} if not training
     */
    public SetSkill getTrainingTargetSkill() {
        return trainingTargetSkill;
    }

    /**
     * Sets the skill this specialist's current training round is working
     * toward.
     *
     * @param trainingTargetSkill the training target skill
     */
    public void setTrainingTargetSkill(SetSkill trainingTargetSkill) {
        this.trainingTargetSkill = trainingTargetSkill;
    }

    /**
     * Returns the id of the owned mission component this specialist's
     * current {@link AssignmentState#ASSIGNED} state is doing R&amp;D on.
     * Meaningless when not {@code ASSIGNED} - see {@code design.md}
     * &sect;3.4's field proposal.
     *
     * @return the R&amp;D target component id, or {@code null} if not
     *     doing R&amp;D
     */
    public String getRdTargetComponentId() {
        return rdTargetComponentId;
    }

    /**
     * Sets the id of the owned mission component this specialist's
     * current {@link AssignmentState#ASSIGNED} state is doing R&amp;D on.
     *
     * @param rdTargetComponentId the R&amp;D target component id
     */
    public void setRdTargetComponentId(String rdTargetComponentId) {
        this.rdTargetComponentId = rdTargetComponentId;
    }

    /**
     * Returns which of the targeted component's four R&amp;D slots this
     * specialist occupies (0 = best reward band, 3 = worst) - a deliberate
     * player choice, re-deriving {@code baspm.md} &sect;10.8's own
     * explicit-slot mechanic (user decision, 2026-10-01, see
     * {@code design.md} &sect;3.4). Meaningless when
     * {@link #getRdTargetComponentId()} is {@code null}.
     *
     * @return the slot index (0-3), or {@code null} if not doing R&amp;D
     */
    public Integer getRdSlot() {
        return rdSlot;
    }

    /**
     * Sets which of the targeted component's four R&amp;D slots this
     * specialist occupies.
     *
     * @param rdSlot the slot index (0-3), or {@code null}
     */
    public void setRdSlot(Integer rdSlot) {
        this.rdSlot = rdSlot;
    }
}
