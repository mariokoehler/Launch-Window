package de.mkoehler.launchwindow.model;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * <p>An astronaut/cosmonaut employee - flies missions, holds one competency
 * value per {@link AstronautSkill}.</p>
 *
 * @author Mario Koehler
 */
public class Astronaut extends Personnel {

    private Map<AstronautSkill, Integer> skills = new EnumMap<>(AstronautSkill.class);

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
    public Map<AstronautSkill, Integer> getSkills() {
        return skills;
    }

    /**
     * Replaces this astronaut's entire skill map.
     *
     * @param skills the skill map
     */
    public void setSkills(Map<AstronautSkill, Integer> skills) {
        Objects.requireNonNull(skills, "skills must not be null");
        this.skills = new EnumMap<>(AstronautSkill.class);
        this.skills.putAll(skills);
    }

    /**
     * Returns this astronaut's competency value for one skill.
     *
     * @param skill the skill to look up
     * @return the competency value, or {@code 0} if not set
     */
    public int getSkill(AstronautSkill skill) {
        return skills.getOrDefault(skill, 0);
    }

    /**
     * Sets this astronaut's competency value for one skill.
     *
     * @param skill the skill to set
     * @param value the competency value
     */
    public void setSkill(AstronautSkill skill, int value) {
        skills.put(skill, value);
    }
}
