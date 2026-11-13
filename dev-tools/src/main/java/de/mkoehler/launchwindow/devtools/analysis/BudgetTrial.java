package de.mkoehler.launchwindow.devtools.analysis;

import de.mkoehler.launchwindow.content.BudgetSchedule;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>The funding the analysis tools play with: the game's own ({@link BudgetSchedule#baspmClose()}) unless
 * {@code -Drules.budget} names other ranges, for trying a stingier economy before changing the rules
 * ({@code design.md} &sect;4, "Money stops mattering from the second review").</p>
 *
 * <p>{@code -Drules.budget=trial} plays the trial ranges below; {@code -Drules.budget=4600-14000,5500-20000,...}
 * gives each review's funding bottom and top from review 2 on (review 1 and the start keep the game's), the last
 * one standing for every review after it. The prestige brackets stay the game's.</p>
 *
 * @author Mario Koehler
 */
final class BudgetTrial {

    /** The first trial (2026-11-13): tops cut from review 2 on, flat from review 5, bottoms kept below them. */
    static final String TRIAL = "4600-14000,5500-20000,8000-26000,10000-28000,12000-29000,14000-30000,15000-31000,"
        + "16000-32000,17000-32000";

    private BudgetTrial() {
    }

    /**
     * Returns the funding to play with.
     *
     * @return the game's, or the ranges {@code -Drules.budget} names
     */
    static BudgetSchedule schedule() {
        // -Drules.flatten=<knee>,<slope> flattens every review's line from review 2 on above the knee (a share of
        // its prestige range) to that share of its slope: diminishing returns at the top (BudgetProgress).
        String flatten = System.getProperty("rules.flatten");
        if (flatten != null && !flatten.isBlank()) {
            String[] parts = flatten.split(",");
            de.mkoehler.launchwindow.rules.BudgetProgress.setFlattening(Double.parseDouble(parts[0].trim()),
                Double.parseDouble(parts[1].trim()));
        }
        // -Drules.pay=all, or some of market,performance,ceiling,upside: pay expectations' parts (design.md 2.2(c)).
        String pay = System.getProperty("rules.pay", "");
        boolean all = pay.contains("all");
        de.mkoehler.launchwindow.rules.PayExpectations.setParts(all || pay.contains("market"),
            all || pay.contains("performance"), all || pay.contains("ceiling"), all || pay.contains("upside"));
        BudgetSchedule game = BudgetSchedule.baspmClose();
        String spec = System.getProperty("rules.budget");
        if (spec == null || spec.isBlank() || spec.equals("game")) {
            return game;
        }
        String[] ranges = (spec.equals("trial") ? TRIAL : spec).split(",");
        List<BudgetSchedule.Review> reviews = new ArrayList<>();
        reviews.add(game.reviews().getFirst());
        for (int i = 1; i < game.reviews().size(); i++) {
            BudgetSchedule.Review bracket = game.reviews().get(i);
            String[] range = ranges[Math.min(i - 1, ranges.length - 1)].trim().split("-");
            long bottom = Long.parseLong(range[0]);
            long top = Long.parseLong(range[1]);
            if (top < bottom) {
                throw new IllegalArgumentException("a review's top is below its bottom: " + ranges[i - 1]);
            }
            reviews.add(new BudgetSchedule.Review(bracket.minPrestige(), bottom, bracket.maxPrestige(), top));
        }
        return new BudgetSchedule(game.initialFunds(), game.initialPrestige(), game.initialFundingRate(),
            game.fundsPerPrestige(), reviews);
    }

    /**
     * Names the funding played with, for a report's heading.
     *
     * @return "the game's funding" or the trial's ranges
     */
    static String label() {
        String spec = System.getProperty("rules.budget");
        String flatten = System.getProperty("rules.flatten");
        String ranges = spec == null || spec.isBlank() || spec.equals("game") ? "the game's funding"
            : "trial funding " + (spec.equals("trial") ? TRIAL : spec);
        String pay = System.getProperty("rules.pay", "");
        String flat = flatten == null || flatten.isBlank() ? ranges
            : ranges + ", flattened above " + flatten.replace(",", " of the range to a slope of ");
        return pay.isBlank() ? flat : flat + ", pay expectations: " + pay;
    }
}
