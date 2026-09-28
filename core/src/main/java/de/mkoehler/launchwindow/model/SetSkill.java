package de.mkoehler.launchwindow.model;

/**
 * <p>The five competencies tracked for a {@link SetSpecialist} (Scientists,
 * Engineers, Technicians).</p>
 *
 * <p>Each value corresponds to a mission-component group - see
 * {@code baspm.md} &sect;9.1: R&amp;D reliability gains for a component
 * scale with the assignee's skill in that component's group.</p>
 *
 * @author Mario Koehler
 */
public enum SetSkill {

    /**
     * Unmanned launch vehicles.
     */
    ROCKETS,

    /**
     * Uncrewed space probes and satellites.
     */
    SPACE_PROBES,

    /**
     * Launch vehicles rated for carrying crew.
     */
    HUMAN_RATED_ROCKETS,

    /**
     * Crewed spacecraft (capsules, landers, and similar).
     */
    CREWED_SPACECRAFT,

    /**
     * Extravehicular activity suits and life-support equipment.
     */
    EVA_SUITS
}
