package de.mkoehler.launchwindow.devtools.analysis;

import de.mkoehler.launchwindow.model.Personnel;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.rules.BudgetProgress;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * <p>The playouts' per-season ledger of people, beside {@link EconomyLedger}'s money: how many there are, their
 * morale (the share high, from 80, and low, below 50), who resigned, and the salary bill against what everyone
 * expects ({@code design.md} &sect;2.2(c), pay expectations). Reported by funding review, while racing.</p>
 *
 * @author Mario Koehler
 */
final class PeopleLedger {

    /** One season of one agency. */
    record Row(int season, int heads, int high, int low, int moraleSum, int resigned, double salaries,
               double expected, int underpaid, int raised, int departmentsRaised) {

        int review() {
            return season / BudgetProgress.SEASONS_PER_REVIEW;
        }
    }

    private final List<Row> rows = new ArrayList<>();
    private final Map<String, Double> lastSalary = new java.util.HashMap<>();
    private final Map<String, Integer> reasons = new TreeMap<>();
    private int agencies;

    /**
     * Records a season, after it resolved.
     *
     * @param season   the season, from 1
     * @param snapshot the agency
     * @param resigned how many resigned in it
     * @param events   the season's events, for why expectations rose
     */
    void record(int season, PlayerSnapshot snapshot, int resigned,
            List<de.mkoehler.launchwindow.rules.ResolutionEvent> events) {
        if (season == 1) {
            lastSalary.clear();
            agencies++;
        }
        for (de.mkoehler.launchwindow.rules.ResolutionEvent event : events) {
            if (event instanceof de.mkoehler.launchwindow.rules.ExpectationRose rose) {
                rose.reasons().forEach(reason -> reasons.merge(reason, 1, Integer::sum));
            } else if (event instanceof de.mkoehler.launchwindow.rules.ExpectationsRoseAtReview) {
                reasons.merge("REVIEW", 1, Integer::sum);
            }
        }
        int raised = 0;
        java.util.Set<String> departments = new java.util.HashSet<>();
        List<Personnel> people = Stream.of(snapshot.getAstronauts(), snapshot.getMissionControlOfficers(),
            snapshot.getSetSpecialists()).flatMap(List::stream).map(Personnel.class::cast).toList();
        int high = 0;
        int low = 0;
        int sum = 0;
        int underpaid = 0;
        double salaries = 0;
        double expected = 0;
        for (Personnel person : people) {
            Double before = lastSalary.put(person.getId(), person.getSalary());
            if (before != null && person.getSalary() > before + 1e-6) {
                raised++;
                departments.add(person.getClass().getSimpleName());
            }
            high += person.getMorale() >= 80 ? 1 : 0;
            low += person.getMorale() < 50 ? 1 : 0;
            sum += person.getMorale();
            salaries += person.getSalary();
            double expects = person.expectedSalary();
            expected += expects;
            underpaid += person.getSalary() < expects * 0.95 ? 1 : 0;
        }
        rows.add(new Row(season, people.size(), high, low, sum, resigned, salaries, expected, underpaid, raised,
            departments.size()));
    }

    /**
     * Prints the report.
     *
     * @param label what was played
     */
    void print(String label) {
        System.out.printf(Locale.ROOT, "%n== People, %s: %d agency-seasons ==%n", label, rows.size());
        System.out.println("Per review: people (mean), morale mean, share of people from 80 / below 50, "
            + "resignations per agency-season, salary bill (mean), paid as a share of expected, share underpaid 5 %+, "
            + "people raised and departments raising per agency over the review.");
        System.out.println("  review  seasons   people  morale  80+ %  <50 %  resigned   salaries  paid/exp  under %"
            + "  raised  depts");
        Map<Integer, List<Row>> byReview = new TreeMap<>();
        rows.forEach(row -> byReview.computeIfAbsent(row.review(), key -> new ArrayList<>()).add(row));
        for (Map.Entry<Integer, List<Row>> entry : byReview.entrySet()) {
            List<Row> list = entry.getValue();
            int review = entry.getKey();
            double heads = list.stream().mapToInt(Row::heads).sum();
            // The agencies still racing in this review: its rows over its seasons.
            double perAgency = (double) list.size() / Math.max(1, list.stream().mapToInt(Row::season).distinct().count());
            System.out.printf(Locale.ROOT, "  %6s  %3d-%3d  %7.1f  %6.1f  %5.0f  %5.0f  %8.3f  %9.0f  %8.2f  %7.0f"
                    + "  %6.1f  %5.1f%n",
                review == 0 ? "start" : Integer.toString(review),
                Math.max(1, review * BudgetProgress.SEASONS_PER_REVIEW),
                (review + 1) * BudgetProgress.SEASONS_PER_REVIEW - 1,
                heads / list.size(),
                heads == 0 ? 0 : list.stream().mapToInt(Row::moraleSum).sum() / heads,
                heads == 0 ? 0 : 100.0 * list.stream().mapToInt(Row::high).sum() / heads,
                heads == 0 ? 0 : 100.0 * list.stream().mapToInt(Row::low).sum() / heads,
                (double) list.stream().mapToInt(Row::resigned).sum() / list.size(),
                list.stream().mapToDouble(Row::salaries).sum() / list.size(),
                list.stream().mapToDouble(Row::expected).sum() == 0 ? 1.0
                    : list.stream().mapToDouble(Row::salaries).sum() / list.stream().mapToDouble(Row::expected).sum(),
                heads == 0 ? 0 : 100.0 * list.stream().mapToInt(Row::underpaid).sum() / heads,
                list.stream().mapToInt(Row::raised).sum() / perAgency,
                list.stream().mapToInt(Row::departmentsRaised).sum() / perAgency);
        }
        System.out.println("Expectation rises by cause, per agency: " + reasons.entrySet().stream()
            .map(e -> e.getKey() + " " + String.format(Locale.ROOT, "%.1f", (double) e.getValue() / Math.max(1, agencies)))
            .toList());
    }
}
