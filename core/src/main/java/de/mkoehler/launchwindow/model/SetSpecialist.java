package de.mkoehler.launchwindow.model;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * <p>A Scientist/Engineer/Technician (SET) employee - drives R&amp;D on
 * mission components, holds one competency value per {@link SetSkill}.</p>
 *
 * @author Mario Koehler
 */
public class SetSpecialist extends Personnel {

    private Map<SetSkill, Integer> skills = new EnumMap<>(SetSkill.class);

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
    public Map<SetSkill, Integer> getSkills() {
        return skills;
    }

    /**
     * Replaces this specialist's entire skill map.
     *
     * @param skills the skill map
     */
    public void setSkills(Map<SetSkill, Integer> skills) {
        Objects.requireNonNull(skills, "skills must not be null");
        this.skills = new EnumMap<>(SetSkill.class);
        this.skills.putAll(skills);
    }

    /**
     * Returns this specialist's competency value for one skill.
     *
     * @param skill the skill to look up
     * @return the competency value, or {@code 0} if not set
     */
    public int getSkill(SetSkill skill) {
        return skills.getOrDefault(skill, 0);
    }

    /**
     * Sets this specialist's competency value for one skill.
     *
     * @param skill the skill to set
     * @param value the competency value
     */
    public void setSkill(SetSkill skill, int value) {
        skills.put(skill, value);
    }
}
