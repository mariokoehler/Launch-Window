package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.BudgetSchedule;
import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.model.Personnel;
import de.mkoehler.launchwindow.model.PlayerSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * <p>The budget season pass: charges the running costs, runs the funding
 * review when one is due, and grants the season's funding. An M2 rules-engine
 * resolver, headless. See {@code design.md} &sect;2.1(d) and {@code baspm.md}
 * &sect;11.1/&sect;11.7 for the order and the catch-up rule.</p>
 *
 * <p>Order within the pass, as BASPM traces it: (1) running costs, (2) the
 * seasons-elapsed counter moves on, (3) the review, if one is due, sets the new
 * rate, (4) funding is granted at the rate in force. A review season therefore
 * pays its new rate straight away.</p>
 *
 * <p>Running costs are every employee's salary, every opened program's
 * maintenance, and every owned mission component's maintenance. Funds may go
 * negative; debt blocks new spending through the existing {@code INSUFFICIENT_FUNDS}
 * checks, and nothing else ({@code design.md} &sect;2.1(d)).</p>
 *
 * @author Mario Koehler
 */
public final class BudgetProgress {

    /** Seasons between funding reviews - BASPM's own cadence, {@code baspm.md} &sect;11.2. */
    public static final int SEASONS_PER_REVIEW = 16;

    /**
     * Not instantiable; this class only exposes static methods.
     */
    private BudgetProgress() {
    }

    /**
     * Returns what a player pays out each season, before any funding: every
     * employee on the roster, every opened program, and every owned component.
     *
     * @param snapshot the player's state
     * @param catalog  the content catalog the program and component maintenance comes from
     * @return the running costs, non-negative
     * @throws NullPointerException if an argument is {@code null}
     */
    public static long runningCosts(PlayerSnapshot snapshot, ContentCatalog catalog) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(catalog, "catalog must not be null");
        long salaries = 0;
        for (Personnel person : allEmployees(snapshot)) {
            salaries += Math.round(person.getSalary());
        }
        long maintenance = 0;
        for (String programId : snapshot.getOpenedProgramIds()) {
            maintenance += catalog.program(programId).orElseThrow().maintenanceCost();
        }
        for (String componentId : snapshot.getMissionComponents().keySet()) {
            maintenance += catalog.missionComponent(componentId).orElseThrow().maintenanceCost();
        }
        return Math.round((salaries + maintenance) * snapshot.getCostMultiplier());
    }

    /**
     * Advances the budget by one season.
     *
     * @param snapshot       the player whose funds, rate, and review baseline move; mutated in place
     * @param catalog        the content catalog
     * @param schedule       the bracket table and starting values
     * @param seasonsElapsed the player's seasons elapsed counting this one; the caller
     *                       passes it explicitly, since the season counter isn't advanced
     *                       inside a season pass. A review falls due when it is a
     *                       positive multiple of {@link #SEASONS_PER_REVIEW}
     * @return the events produced: at most one {@link BudgetReviewed}, then one {@link SeasonFundsSettled}
     * @throws NullPointerException     if an argument is {@code null}
     * @throws IllegalArgumentException if {@code seasonsElapsed} is negative
     */
    public static List<ResolutionEvent> advanceSeason(PlayerSnapshot snapshot, ContentCatalog catalog,
            BudgetSchedule schedule, int seasonsElapsed) {
        Objects.requireNonNull(schedule, "schedule must not be null");
        if (seasonsElapsed < 0) {
            throw new IllegalArgumentException("seasonsElapsed must not be negative: " + seasonsElapsed);
        }
        List<ResolutionEvent> events = new ArrayList<>();

        long expenses = runningCosts(snapshot, catalog);
        snapshot.setFunds(snapshot.getFunds() - expenses);

        if (seasonsElapsed > 0 && seasonsElapsed % SEASONS_PER_REVIEW == 0) {
            events.add(review(snapshot, schedule, seasonsElapsed / SEASONS_PER_REVIEW));
        }

        long income = snapshot.getFundingRatePerSeason();
        snapshot.setFunds(snapshot.getFunds() + income);
        events.add(new SeasonFundsSettled(expenses, income, snapshot.getFunds()));
        return events;
    }

    /**
     * Runs one funding review: picks the bracket, applies the catch-up shift if
     * the previous review's baseline was missed, interpolates the new rate, and
     * records this review's prestige as the next review's baseline.
     *
     * @param snapshot     the player being reviewed; mutated in place
     * @param schedule     the bracket table
     * @param reviewNumber the one-based review number
     * @return the review's event
     */
    private static BudgetReviewed review(PlayerSnapshot snapshot, BudgetSchedule schedule, int reviewNumber) {
        BudgetSchedule.Review bracket = schedule.reviewFor(reviewNumber);
        long catchUpShift = 0;
        if (reviewNumber > 1) {
            // The previous review's *raw* minimum, not its shifted one, so shortfalls never compound
            // (baspm.md §11.7).
            long previousMin = schedule.reviewFor(reviewNumber - 1).minPrestige();
            if (snapshot.getPrestigeAtLastReview() < previousMin) {
                catchUpShift = previousMin - snapshot.getPrestigeAtLastReview();
            }
        }
        long prestige = snapshot.getPrestige();
        long rate = interpolate(bracket.minPrestige() - catchUpShift, bracket.minFunding(),
            bracket.maxPrestige() - catchUpShift, bracket.maxFunding(), prestige);
        snapshot.setFundingRatePerSeason(rate);
        snapshot.setPrestigeAtLastReview(prestige);
        return new BudgetReviewed(reviewNumber, prestige, catchUpShift, rate);
    }

    /**
     * Interpolates a review's funding for a prestige: the bottom funding at or
     * below the bracket's bottom, the top funding at or above its top, and a
     * rounded-up straight line between. The shape BASPM's own review uses
     * ({@code baspm.md} &sect;11.1).
     *
     * @param minPrestige the bracket's bottom prestige, after any catch-up shift
     * @param minFunding  the funding at the bottom
     * @param maxPrestige the bracket's top prestige, after any catch-up shift
     * @param maxFunding  the funding at the top
     * @param prestige    the player's prestige
     * @return the funding per season for that prestige
     */
    static long interpolate(long minPrestige, long minFunding, long maxPrestige, long maxFunding, long prestige) {
        if (prestige <= minPrestige) {
            return minFunding;
        }
        if (prestige >= maxPrestige) {
            return maxFunding;
        }
        double slope = (double) (maxFunding - minFunding) / (maxPrestige - minPrestige);
        double intercept = maxFunding - slope * maxPrestige;
        return (long) Math.ceil(slope * prestige + intercept);
    }

    /**
     * Every employee on the roster, across all three groups - candidates excluded.
     *
     * @param snapshot the player
     * @return the roster, in group order
     */
    static List<Personnel> allEmployees(PlayerSnapshot snapshot) {
        List<Personnel> everyone = new ArrayList<>(snapshot.getAstronauts());
        everyone.addAll(snapshot.getSetSpecialists());
        everyone.addAll(snapshot.getMissionControlOfficers());
        return everyone;
    }
}
