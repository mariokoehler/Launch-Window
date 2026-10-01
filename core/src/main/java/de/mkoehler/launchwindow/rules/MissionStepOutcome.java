package de.mkoehler.launchwindow.rules;

/**
 * <p>The five-tier outcome a single mission step can resolve to - the
 * taxonomy decided on for every mission step, uniformly, rather than a
 * per-step vital/optional split (see {@code design.md} &sect;2.5(c),
 * {@code baspm.md} &sect;8.2). Returned by {@link MissionStepResolution#resolve}.</p>
 *
 * @author Mario Koehler
 */
public enum MissionStepOutcome {

    /** The step's first roll succeeded outright; no saving throw was needed. */
    SUCCESSFUL,

    /** The first roll failed, but the saving throw fully rescued the step. */
    MINOR_GLITCH_SOLVED,

    /** The saving throw failed, landing in its top ("least bad") band; the mission continues at half credit. */
    MINOR_GLITCH,

    /** The saving throw failed, landing in its middle band; the mission ends immediately. */
    FAILURE,

    /**
     * The saving throw failed, landing in its bottom ("worst") band on a
     * step flagged crew-risk; the mission ends immediately. Never returned
     * for a step that is not flagged crew-risk - that case downgrades to
     * {@link #FAILURE} instead.
     */
    CATASTROPHIC
}
