package de.mkoehler.launchwindow.model;

/**
 * An agent caught, either way ({@code design.md} &sect;2.9): the caught agency loses prestige and
 * learns the catcher's counter-intelligence level; the catcher learns the spy's Intelligence level.
 *
 * @param turn  the game turn it happened
 * @param ours  {@code true} if it was this agency's agent the rival caught, {@code false} if this
 *              agency caught the rival's
 * @param kind  the operation that was blown
 * @param area  its area, for an assessment, or {@code null}
 * @param level the other side's level that season: their counter-intelligence when ours was caught,
 *              their Intelligence level when we caught theirs
 * @author Mario Koehler
 */
public record IntelCatch(int turn, boolean ours, IntelOperation.Kind kind, IntelArea area, int level) {
}
