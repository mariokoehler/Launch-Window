package de.mkoehler.launchwindow.content;

import java.util.List;
import java.util.Objects;

/**
 * <p>One season's worth of a {@link MissionConfiguration}'s step batch -
 * BASPM stores one mission-simulation file per season a mission runs
 * ({@code baspm.md} &sect;8.1); this is that same per-season batch, ported
 * as Launch Window's own content-definition shape.</p>
 *
 * <p>A content-definition type: shared, read-only game data loaded from a
 * content pack (not yet implemented - see {@code design.md} &sect;5's M3),
 * never per-player state, and never sent over the network.</p>
 *
 * @param steps the ordered batch of steps this season resolves, in
 *              resolution order; never empty, and no two steps may share
 *              an {@link MissionStep#id()} - a step id may still repeat
 *              across two different seasons of the same configuration
 * @author Mario Koehler
 */
public record MissionSeason(List<MissionStep> steps) {

    /**
     * Validates and defensively copies {@code steps} into an unmodifiable
     * view.
     *
     * @throws NullPointerException     if {@code steps} is {@code null}
     * @throws IllegalArgumentException if {@code steps} is empty, or two
     *                                   steps share an id
     */
    public MissionSeason {
        Objects.requireNonNull(steps, "steps must not be null");
        if (steps.isEmpty()) {
            throw new IllegalArgumentException("steps must not be empty");
        }
        steps = List.copyOf(steps);

        List<String> ids = steps.stream().map(MissionStep::id).toList();
        if (ids.size() != ids.stream().distinct().count()) {
            throw new IllegalArgumentException("steps must not contain a duplicate id: " + ids);
        }
    }
}
