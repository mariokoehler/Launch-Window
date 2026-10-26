package de.mkoehler.launchwindow.model;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * <p>An astronaut/cosmonaut employee - flies missions, holds one competency
 * value per {@link AstronautSkill}.</p>
 *
 * <p>The skill map is a plain {@link LinkedHashMap}, not an
 * {@link java.util.EnumMap} - Kryo's wire serialization (see
 * {@code de.mkoehler.launchwindow.net.MessageRegistry}) needs a
 * no-arg-constructible map, which {@code EnumMap} isn't. {@link #expertSkills}
 * is a {@link LinkedHashSet} for the identical reason.</p>
 *
 * <p>Competency values are {@code double}, not {@code int} - flat per-season
 * growth (e.g. SET's R&amp;D gain, {@code design.md} &sect;2.2(c)) can be
 * smaller than 1 and would round away to nothing on an integer scale.</p>
 *
 * @author Mario Koehler
 */
public class Astronaut extends Personnel {

    private Map<AstronautSkill, Double> skills = new LinkedHashMap<>();
    private AstronautSkill trainingTargetSkill;
    private Set<AstronautSkill> expertSkills = new LinkedHashSet<>();
    private int flightsFlown;
    private int firstsFlown;
    private int prDutySeasons;
    private boolean rocketPlaneWings;

    /**
     * No-argument constructor required for Jackson deserialization.
     */
    public Astronaut() {
        super();
    }

    /**
     * Creates a new astronaut with the given identity.
     *
     * @param id                stable identifier, unique within a player's roster
     * @param name              display name
     * @param age               age in years
     * @param morale            current morale
     * @param learningPotential the astronaut's own learning-potential stat
     */
    public Astronaut(String id, String name, int age, int morale, int learningPotential) {
        super(id, name, age, morale, learningPotential);
    }

    /**
     * Returns this astronaut's competency values, keyed by skill. Skills
     * absent from the map are undefined, not implicitly zero.
     *
     * @return the skill map
     */
    public Map<AstronautSkill, Double> getSkills() {
        return skills;
    }

    /**
     * Replaces this astronaut's entire skill map.
     *
     * @param skills the skill map
     */
    public void setSkills(Map<AstronautSkill, Double> skills) {
        Objects.requireNonNull(skills, "skills must not be null");
        this.skills = new LinkedHashMap<>(skills);
    }

    /**
     * Returns this astronaut's competency value for one skill.
     *
     * @param skill the skill to look up
     * @return the competency value, or {@code 0} if not set
     */
    public double getSkill(AstronautSkill skill) {
        return skills.getOrDefault(skill, 0.0);
    }

    /**
     * Sets this astronaut's competency value for one skill.
     *
     * @param skill the skill to set
     * @param value the competency value
     */
    public void setSkill(AstronautSkill skill, double value) {
        skills.put(skill, value);
    }

    /**
     * Returns the skill this astronaut's current training round is working
     * toward. Meaningless when {@link #getTrainingKind()} is
     * {@link TrainingKind#NONE} - see {@code design.md} &sect;3.4's field
     * proposal.
     *
     * @return the training target skill, or {@code null} if not training
     */
    public AstronautSkill getTrainingTargetSkill() {
        return trainingTargetSkill;
    }

    /**
     * Sets the skill this astronaut's current training round is working
     * toward.
     *
     * @param trainingTargetSkill the training target skill
     */
    public void setTrainingTargetSkill(AstronautSkill trainingTargetSkill) {
        this.trainingTargetSkill = trainingTargetSkill;
    }

    /**
     * Returns every skill this astronaut has earned the Expert trait in -
     * see {@code design.md} &sect;2.2(c). Empty for an astronaut who has
     * never completed {@link TrainingKind#EXPERT} training.
     *
     * @return the Expert skills
     */
    public Set<AstronautSkill> getExpertSkills() {
        return expertSkills;
    }

    /**
     * Replaces this astronaut's entire Expert-skill set.
     *
     * @param expertSkills the Expert skills
     */
    public void setExpertSkills(Set<AstronautSkill> expertSkills) {
        Objects.requireNonNull(expertSkills, "expertSkills must not be null");
        this.expertSkills = new LinkedHashSet<>(expertSkills);
    }

    /**
     * Returns how many flights this astronaut completed as crew: part of their fame on PR duty
     * ({@code design.md} &sect;2.2(c)).
     *
     * @return the completed flights
     */
    public int getFlightsFlown() {
        return flightsFlown;
    }

    /**
     * Sets the completed flights.
     *
     * @param flightsFlown the count, zero or more
     */
    public void setFlightsFlown(int flightsFlown) {
        this.flightsFlown = flightsFlown;
    }

    /**
     * Returns how many of their completed flights set a first in the game, counted once per flight.
     *
     * @return the flights with a first
     */
    public int getFirstsFlown() {
        return firstsFlown;
    }

    /**
     * Sets the flights with a first.
     *
     * @param firstsFlown the count, zero or more
     */
    public void setFirstsFlown(int firstsFlown) {
        this.firstsFlown = firstsFlown;
    }

    /**
     * Returns how many season passes in a row found this astronaut on PR duty; rust sets in after
     * {@code PrDuty.RUST_FREE_SEASONS}. Kept by the season pass only.
     *
     * @return the seasons in a row
     */
    public int getPrDutySeasons() {
        return prDutySeasons;
    }

    /**
     * Sets the seasons in a row on PR duty.
     *
     * @param prDutySeasons the count, zero or more
     */
    public void setPrDutySeasons(int prDutySeasons) {
        this.prDutySeasons = prDutySeasons;
    }

    /**
     * Returns whether this astronaut has flown a rocket plane and landed it: their rocket-plane wings,
     * which make the piloting steps of their capsule flights safer ({@code rules.RocketPlaneWings},
     * {@code design.md} &sect;2.2(c)).
     *
     * @return whether they have them
     */
    public boolean isRocketPlaneWings() {
        return rocketPlaneWings;
    }

    /**
     * Sets whether this astronaut has their rocket-plane wings.
     *
     * @param rocketPlaneWings whether they have them
     */
    public void setRocketPlaneWings(boolean rocketPlaneWings) {
        this.rocketPlaneWings = rocketPlaneWings;
    }
}
