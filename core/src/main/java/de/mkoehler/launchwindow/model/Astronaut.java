package de.mkoehler.launchwindow.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * <p>An astronaut/cosmonaut employee - flies missions, holds one competency
 * value per {@link AstronautSkill}.</p>
 *
 * <p>The skill map is a plain {@link LinkedHashMap}, not an
 * {@link java.util.EnumMap} - Kryo's wire serialization (see
 * {@code de.mkoehler.launchwindow.net.MessageRegistry}) needs a
 * no-arg-constructible map, which {@code EnumMap} isn't.</p>
 *
 * <p>Competency values are {@code double}, not {@code int} - flat per-season
 * growth (e.g. SET's R&amp;D gain, {@code design.md} &sect;2.2(c)) can be
 * smaller than 1 and would round away to nothing on an integer scale.</p>
 *
 * @author Mario Koehler
 */
public class Astronaut extends Personnel {

    private Map<AstronautSkill, Double> skills = new LinkedHashMap<>();

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
}
