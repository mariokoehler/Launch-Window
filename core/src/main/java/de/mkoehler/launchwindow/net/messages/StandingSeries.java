package de.mkoehler.launchwindow.net.messages;

import de.mkoehler.launchwindow.rules.TurnReport;

import java.util.List;

/**
 * One agency's curves ({@code ui-spec.md} component 30, gap G20): a point for each resolved turn,
 * read from its turn reports, so only resolved values, and only its own player gets the funds.
 *
 * @param agencyName the agency
 * @param you        whether this is the player who asked
 * @param bot        whether the server plays it
 * @param playerId   the agency's opaque key, as in {@link RivalSummary}
 * @param firstTurn  the game turn of the first point; a late joiner's starts late
 * @param prestige   prestige, one value a turn from {@code firstTurn}
 * @param funds      funds on the same turns, for the asking player; empty for a rival
 * @author Mario Koehler
 */
public record StandingSeries(String agencyName, boolean you, boolean bot, String playerId, long firstTurn,
                             long[] prestige, long[] funds) {

    /**
     * Builds the curves from an agency's reports. The first point is the first report's values
     * before its turn, the agency's start, then one point after each turn. A turn with no report
     * repeats the point before it, so the points stay one a turn.
     *
     * @param agencyName the agency
     * @param you        whether this is the player who asked
     * @param bot        whether the server plays it
     * @param playerId   the agency's opaque key
     * @param reports    its reports, oldest first
     * @return the curves
     */
    public static StandingSeries of(String agencyName, boolean you, boolean bot, String playerId,
                                    List<TurnReport> reports) {
        if (reports.isEmpty()) {
            return new StandingSeries(agencyName, you, bot, playerId, 0L, new long[0], new long[0]);
        }
        TurnReport first = reports.getFirst();
        long firstTurn = first.gameTurn() - 1;
        int points = Math.toIntExact(reports.getLast().gameTurn() - firstTurn + 1);
        long[] prestige = new long[points];
        long[] funds = new long[points];
        prestige[0] = first.prestigeBefore();
        funds[0] = first.fundsBefore();
        int filled = 0;
        for (TurnReport report : reports) {
            int index = Math.toIntExact(report.gameTurn() - firstTurn);
            for (int i = filled + 1; i < index; i++) {
                prestige[i] = prestige[filled];
                funds[i] = funds[filled];
            }
            prestige[index] = report.prestigeAfter();
            funds[index] = report.fundsAfter();
            filled = index;
        }
        return new StandingSeries(agencyName, you, bot, playerId, firstTurn, prestige, you ? funds : new long[0]);
    }
}
