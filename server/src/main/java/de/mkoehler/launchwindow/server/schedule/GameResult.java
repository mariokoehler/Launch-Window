package de.mkoehler.launchwindow.server.schedule;

import java.util.List;

/**
 * The game's recorded result, written once to {@value #FILE_NAME} the first turn
 * someone completes the declared goals, and never overwritten. Play continues after
 * it (user decision, 2026-10-03, {@code design.md} &sect;5, M4).
 *
 * @param status        {@code WON} or {@code TIED}
 * @param winnerIds     the winning account ids
 * @param winnerAgencies the winners' agency names, in the same order
 * @param decidedOnTurn the game turn the result was decided on
 * @author Mario Koehler
 */
public record GameResult(String status, List<String> winnerIds, List<String> winnerAgencies, long decidedOnTurn) {

    /** The result file's name under the data root. */
    public static final String FILE_NAME = "game-result.json";
}
