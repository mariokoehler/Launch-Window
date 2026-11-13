package de.mkoehler.launchwindow.devtools.analysis;

import de.mkoehler.launchwindow.rules.BudgetProgress;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.ToDoubleFunction;

/**
 * <p>The playouts' per-season ledger of money: what each season's funding was, what running costs took, what the
 * agency spent on everything else (launches, R&amp;D, hiring, upgrades, contracts' costs net of their fees, random
 * events), and the cash left at its end. Reported by funding review, before and after the agency had won the race,
 * so a surplus that builds up while racing tells apart from one that only comes once there's nothing left to race
 * for. {@link EconomyCeiling} gives the rules' side of the same question.</p>
 *
 * @author Mario Koehler
 */
final class EconomyLedger {

    /** One season of one agency. */
    record Row(int season, boolean won, long funding, long running, long other, long cash) {

        int review() {
            return season / BudgetProgress.SEASONS_PER_REVIEW;
        }
    }

    private final List<Row> rows = new ArrayList<>();

    /**
     * Records a season.
     *
     * @param season     the season, from 1
     * @param won        whether the agency had already won the race before it
     * @param fundsStart the funds before the season's actions
     * @param funding    the funding granted in its budget pass
     * @param running    the running costs charged in it
     * @param fundsEnd   the funds after it
     */
    void record(int season, boolean won, long fundsStart, long funding, long running, long fundsEnd) {
        rows.add(new Row(season, won, funding, running, fundsStart + funding - running - fundsEnd, fundsEnd));
    }

    /**
     * Prints the report.
     *
     * @param label what was played
     */
    void print(String label) {
        System.out.printf(Locale.ROOT, "%n== Money, %s: %d agency-seasons ==%n", label, rows.size());
        System.out.println("Funding and running costs are medians. The shares are totals over the review's seasons: "
            + "running costs, everything else spent, and what was left, each of the funding. 'Banked' is cash at the "
            + "season's end in seasons of funding, median (q3); 'flush' the share of seasons ending with 2+ banked.");
        for (boolean won : new boolean[] {false, true}) {
            Map<Integer, List<Row>> byReview = new TreeMap<>();
            rows.stream().filter(row -> row.won() == won)
                .forEach(row -> byReview.computeIfAbsent(row.review(), key -> new ArrayList<>()).add(row));
            if (byReview.isEmpty()) {
                continue;
            }
            System.out.println(won ? "After winning the race:" : "While racing:");
            System.out.println("  review  seasons  rows   funding    running  running%   other%   left%     banked"
                + "         flush");
            for (Map.Entry<Integer, List<Row>> entry : byReview.entrySet()) {
                List<Row> list = entry.getValue();
                int review = entry.getKey();
                double flush = 100.0 * list.stream().filter(row -> row.funding() > 0
                    && row.cash() >= 2 * row.funding()).count() / list.size();
                System.out.printf(Locale.ROOT, "  %6s  %3d-%3d %5d  %,8.0f  %,9.0f  %7.0f  %7.0f  %6.0f  %5.1f (%4.1f)  "
                        + "%5.0f %%%n",
                    review == 0 ? "start" : Integer.toString(review),
                    Math.max(1, review * BudgetProgress.SEASONS_PER_REVIEW),
                    (review + 1) * BudgetProgress.SEASONS_PER_REVIEW - 1, list.size(),
                    median(list, Row::funding), median(list, Row::running),
                    total(list, Row::running), total(list, Row::other),
                    total(list, row -> row.funding() - row.running() - row.other()),
                    median(list, row -> row.funding() <= 0 ? 0 : (double) row.cash() / row.funding()),
                    quartile3(list, row -> row.funding() <= 0 ? 0 : (double) row.cash() / row.funding()), flush);
            }
        }
    }

    /** A part's total over the rows, as a share of their total funding. */
    private static double total(List<Row> rows, java.util.function.ToLongFunction<Row> part) {
        long funding = rows.stream().mapToLong(Row::funding).sum();
        return funding <= 0 ? 0 : 100.0 * rows.stream().mapToLong(part).sum() / funding;
    }

    private static double median(List<Row> rows, ToDoubleFunction<Row> value) {
        return at(rows, value, 0.5);
    }

    private static double quartile3(List<Row> rows, ToDoubleFunction<Row> value) {
        return at(rows, value, 0.75);
    }

    private static double at(List<Row> rows, ToDoubleFunction<Row> value, double fraction) {
        double[] sorted = rows.stream().mapToDouble(value).sorted().toArray();
        return sorted.length == 0 ? 0 : sorted[Math.min(sorted.length - 1, (int) (sorted.length * fraction))];
    }
}
