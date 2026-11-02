package de.mkoehler.launchwindow.net.messages;

import de.mkoehler.launchwindow.rules.BudgetReviewed;
import de.mkoehler.launchwindow.rules.CrewedFlightsGrounded;
import de.mkoehler.launchwindow.rules.ResolutionEvent;
import de.mkoehler.launchwindow.rules.TurnReport;

import java.util.List;

/**
 * One agency's curves ({@code ui-spec.md} component 30, gap G20): a point for each resolved turn,
 * read from its turn reports, so only resolved values. Prestige is public; the funds and the marks
 * only the agency's own player ever gets.
 *
 * @param agencyName    the agency
 * @param you           whether this is the player who asked
 * @param bot           whether the server plays it
 * @param playerId      the agency's opaque key, as in {@link RivalSummary}
 * @param firstTurn     the game turn of the first point, the one before the first report; a late
 *                      joiner's starts late, and 0 means the game's start
 * @param prestige      prestige, one value a turn from {@code firstTurn}
 * @param funds         funds on the same turns, for the asking player; empty for a rival
 * @param reviewTurns   the turns of the asking player's budget reviews; empty for a rival
 * @param lostCrewTurns the turns the asking player lost a crew; empty for a rival
 * @author Mario Koehler
 */
public record StandingSeries(String agencyName, boolean you, boolean bot, String playerId, long firstTurn,
                             long[] prestige, long[] funds, long[] reviewTurns, long[] lostCrewTurns) {

    /**
     * What the curves need from one turn's report.
     *
     * @param turn           the game turn
     * @param prestigeBefore prestige before it
     * @param prestigeAfter  prestige after it
     * @param fundsBefore    funds before it
     * @param fundsAfter     funds after it
     * @param review         whether a budget review fell in it
     * @param crewLost       whether a crew was lost in it
     */
    public record Turn(long turn, long prestigeBefore, long prestigeAfter, long fundsBefore, long fundsAfter,
                       boolean review, boolean crewLost) {

        /**
         * Reads a report.
         *
         * @param report the report
         * @return its numbers
         */
        public static Turn of(TurnReport report) {
            boolean review = false;
            boolean crewLost = false;
            for (ResolutionEvent event : report.events()) {
                review |= event instanceof BudgetReviewed;
                crewLost |= event instanceof CrewedFlightsGrounded;
            }
            return new Turn(report.gameTurn(), report.prestigeBefore(), report.prestigeAfter(),
                report.fundsBefore(), report.fundsAfter(), review, crewLost);
        }
    }

    /**
     * Builds the curves. The first point is the first turn's values before it, the agency's start,
     * then one point after each turn. A turn with no report repeats the point before it, so the
     * points stay one a turn.
     *
     * @param agencyName the agency
     * @param you        whether this is the player who asked
     * @param bot        whether the server plays it
     * @param playerId   the agency's opaque key
     * @param turns      its turns, oldest first
     * @return the curves
     */
    public static StandingSeries of(String agencyName, boolean you, boolean bot, String playerId, List<Turn> turns) {
        if (turns.isEmpty()) {
            return new StandingSeries(agencyName, you, bot, playerId, 0L, new long[0], new long[0], new long[0],
                new long[0]);
        }
        Turn first = turns.getFirst();
        long firstTurn = first.turn() - 1;
        int points = Math.toIntExact(turns.getLast().turn() - firstTurn + 1);
        long[] prestige = new long[points];
        long[] funds = new long[points];
        prestige[0] = first.prestigeBefore();
        funds[0] = first.fundsBefore();
        int filled = 0;
        for (Turn turn : turns) {
            int index = Math.toIntExact(turn.turn() - firstTurn);
            for (int i = filled + 1; i < index; i++) {
                prestige[i] = prestige[filled];
                funds[i] = funds[filled];
            }
            prestige[index] = turn.prestigeAfter();
            funds[index] = turn.fundsAfter();
            filled = index;
        }
        if (!you) {
            return new StandingSeries(agencyName, false, bot, playerId, firstTurn, prestige, new long[0], new long[0],
                new long[0]);
        }
        long[] reviews = turns.stream().filter(Turn::review).mapToLong(Turn::turn).toArray();
        long[] lost = turns.stream().filter(Turn::crewLost).mapToLong(Turn::turn).toArray();
        return new StandingSeries(agencyName, true, bot, playerId, firstTurn, prestige, funds, reviews, lost);
    }
}
