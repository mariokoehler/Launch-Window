package de.mkoehler.launchwindow.model;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * <p>A flight-controller employee - staffs Mission Control, holds one
 * competency value per {@link MissionControlSkill}.</p>
 *
 * <p>"Flight Director" is not a separately trained specialty here: per
 * {@code baspm.md} &sect;9.1, it is arithmetically just how well-rounded a
 * controller is, computed on demand by {@link #flightDirectorSkill()} rather
 * than stored.</p>
 *
 * @author Mario Koehler
 */
public class MissionControlOfficer extends Personnel {

    private Map<MissionControlSkill, Integer> skills = new EnumMap<>(MissionControlSkill.class);

    /**
     * No-argument constructor required for Jackson deserialization.
     */
    public MissionControlOfficer() {
        super();
    }

    /**
     * Creates a new mission control officer with the given identity.
     *
     * @param id                stable identifier, unique within a player's roster
     * @param name              display name
     * @param age               age in years
     * @param morale            current morale
     * @param learningPotential the officer's own learning-potential stat
     */
    public MissionControlOfficer(String id, String name, int age, int morale, int learningPotential) {
        super(id, name, age, morale, learningPotential);
    }

    /**
     * Returns this officer's competency values, keyed by skill. Skills
     * absent from the map are undefined, not implicitly zero.
     *
     * @return the skill map
     */
    public Map<MissionControlSkill, Integer> getSkills() {
        return skills;
    }

    /**
     * Replaces this officer's entire skill map.
     *
     * @param skills the skill map
     */
    public void setSkills(Map<MissionControlSkill, Integer> skills) {
        Objects.requireNonNull(skills, "skills must not be null");
        this.skills = new EnumMap<>(MissionControlSkill.class);
        this.skills.putAll(skills);
    }

    /**
     * Returns this officer's competency value for one skill.
     *
     * @param skill the skill to look up
     * @return the competency value, or {@code 0} if not set
     */
    public int getSkill(MissionControlSkill skill) {
        return skills.getOrDefault(skill, 0);
    }

    /**
     * Sets this officer's competency value for one skill.
     *
     * @param skill the skill to set
     * @param value the competency value
     */
    public void setSkill(MissionControlSkill skill, int value) {
        skills.put(skill, value);
    }

    /**
     * Computes this officer's Flight Director aptitude as the plain average
     * of all five stored {@link MissionControlSkill} values. Skills absent
     * from the map count as {@code 0}.
     *
     * @return the average of the five stored competency values
     */
    public double flightDirectorSkill() {
        int sum = 0;
        for (MissionControlSkill skill : MissionControlSkill.values()) {
            sum += getSkill(skill);
        }
        return sum / (double) MissionControlSkill.values().length;
    }
}
