package de.mkoehler.launchwindow.content;

import java.util.List;
import java.util.Objects;

/**
 * <p>A goal a mission step can achieve. Achieving it pays prestige (the
 * first-time value if no one in the game has achieved it yet, the
 * second-time value otherwise), and cancels every goal in
 * {@link #cancelsGoalIds()} - itself paying and counting toward the game-wide
 * tally, recursively. See {@code design.md} &sect;2.6(d) and
 * {@code baspm.md} &sect;11.7.</p>
 *
 * <p>A content-definition type: shared, read-only game data, never per-player
 * state, and never sent over the network.</p>
 *
 * @param id                 a stable identifier, unique within a {@link ContentCatalog}
 * @param firstTimePrestige  the prestige paid when this goal's first achievement
 *                           in the game settles
 * @param secondTimePrestige the prestige paid for every later achievement
 * @param cancelsGoalIds     the goals this goal cancels when it settles; may be empty
 * @author Mario Koehler
 */
public record Goal(String id, long firstTimePrestige, long secondTimePrestige, List<String> cancelsGoalIds) {

    /**
     * Validates every field and defensively copies {@code cancelsGoalIds}.
     *
     * @throws NullPointerException     if {@code id} or {@code cancelsGoalIds} is {@code null}
     * @throws IllegalArgumentException if {@code id} is blank, either prestige
     *                                   value is negative, or {@code cancelsGoalIds}
     *                                   repeats an entry or names this goal itself
     */
    public Goal {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(cancelsGoalIds, "cancelsGoalIds must not be null");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (firstTimePrestige < 0 || secondTimePrestige < 0) {
            throw new IllegalArgumentException("goal prestige must not be negative: " + id);
        }
        cancelsGoalIds = List.copyOf(cancelsGoalIds);
        if (cancelsGoalIds.size() != cancelsGoalIds.stream().distinct().count()) {
            throw new IllegalArgumentException("goal " + id + " cancels a goal more than once");
        }
        if (cancelsGoalIds.contains(id)) {
            throw new IllegalArgumentException("goal " + id + " cancels itself");
        }
    }
}
