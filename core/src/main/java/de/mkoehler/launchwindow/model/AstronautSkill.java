package de.mkoehler.launchwindow.model;

/**
 * <p>The five competencies tracked for an {@link Astronaut}. See
 * {@code baspm.md} &sect;9.1 for where this five-skill shape comes from.</p>
 *
 * @author Mario Koehler
 */
public enum AstronautSkill {

    /**
     * Ability to lead a crew and coordinate under pressure.
     */
    LEADERSHIP,

    /**
     * Manual spacecraft piloting skill.
     */
    PILOTING,

    /**
     * Extravehicular activity (spacewalk) proficiency.
     */
    EVA,

    /**
     * In-flight scientific work proficiency.
     */
    SCIENCE,

    /**
     * Physical fitness and resilience to spaceflight stresses.
     */
    FITNESS
}
