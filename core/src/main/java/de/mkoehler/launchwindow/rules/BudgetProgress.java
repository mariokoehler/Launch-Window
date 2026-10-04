package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.BudgetSchedule;
import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.model.Building;
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
 * maintenance, every owned mission component's maintenance, and every
 * building's maintenance at its current level (M3). Funds may go
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
        return runningCostBreakdown(snapshot, catalog).total();
    }

    /**
     * A season's running costs by kind, after the cost multiplier, adding up exactly to the total:
     * the building upkeep line absorbs the rounding.
     *
     * @param salaries      every employee's salary
     * @param employees     how many employees that is
     * @param programs      the opened programs' upkeep
     * @param programCount  how many programs are open
     * @param components    the active components' upkeep
     * @param componentCount how many components are active
     * @param buildings     the buildings' upkeep at their levels
     * @param total         the running costs charged
     */
    public record RunningCosts(long salaries, int employees, long programs, int programCount, long components,
                               int componentCount, long buildings, long total) {
    }

    /**
     * Returns a player's running costs by kind ({@code ui-spec.md} &sect;4.4's budget lines, gap G7).
     *
     * @param snapshot the player's state
     * @param catalog  the content catalog the program and component maintenance comes from
     * @return the breakdown, its total exactly {@link #runningCosts}
     * @throws NullPointerException if an argument is {@code null}
     */
    public static RunningCosts runningCostBreakdown(PlayerSnapshot snapshot, ContentCatalog catalog) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(catalog, "catalog must not be null");
        long salaries = 0;
        int employees = 0;
        for (Personnel person : allEmployees(snapshot)) {
            salaries += Math.round(person.getSalary());
            employees++;
        }
        long programs = 0;
        for (String programId : snapshot.getOpenedProgramIds()) {
            programs += catalog.program(programId).orElseThrow().maintenanceCost();
        }
        long components = 0;
        int componentCount = 0;
        for (var owned : snapshot.getMissionComponents().entrySet()) {
            // An inactive component (its last program closed) is kept but pays no upkeep (baspm.md §10.9).
            if (owned.getValue().isActive()) {
                components += catalog.missionComponent(owned.getKey()).orElseThrow().maintenanceCost();
                componentCount++;
            }
        }
        long buildings = BuildingUpgrades.maintenance(snapshot, catalog.buildings());
        double multiplier = snapshot.getCostMultiplier();
        long total = Math.round((salaries + programs + components + buildings) * multiplier);
        long scaledSalaries = Math.round(salaries * multiplier);
        long scaledPrograms = Math.round(programs * multiplier);
        long scaledComponents = Math.round(components * multiplier);
        return new RunningCosts(scaledSalaries, employees, scaledPrograms, snapshot.getOpenedProgramIds().size(),
            scaledComponents, componentCount, total - scaledSalaries - scaledPrograms - scaledComponents, total);
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

        RunningCosts costs = runningCostBreakdown(snapshot, catalog);
        long expenses = costs.total();
        snapshot.setFunds(snapshot.getFunds() - expenses);

        if (seasonsElapsed > 0 && seasonsElapsed % SEASONS_PER_REVIEW == 0) {
            events.add(review(snapshot, schedule, seasonsElapsed / SEASONS_PER_REVIEW));
        }

        long income = snapshot.getFundingRatePerSeason();
        snapshot.setFunds(snapshot.getFunds() + income);
        events.add(new SeasonFundsSettled(expenses, income, snapshot.getFunds(), costs));
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
        long catchUpShift = catchUpShift(snapshot, schedule, reviewNumber);
        long prestige = snapshot.getPrestige();
        long rate = reviewRate(schedule.reviewFor(reviewNumber), catchUpShift, prestige);
        snapshot.setFundingRatePerSeason(rate);
        snapshot.setPrestigeAtLastReview(prestige);
        return new BudgetReviewed(reviewNumber, prestige, catchUpShift, rate);
    }

    /**
     * The catch-up shift a review applies: how far the previous review's baseline fell short of
     * that review's bottom.
     */
    private static long catchUpShift(PlayerSnapshot snapshot, BudgetSchedule schedule, int reviewNumber) {
        if (reviewNumber <= 1) {
            return 0;
        }
        // The previous review's *raw* minimum, not its shifted one, so shortfalls never compound
        // (baspm.md §11.7).
        long previousMin = schedule.reviewFor(reviewNumber - 1).minPrestige();
        return snapshot.getPrestigeAtLastReview() < previousMin ? previousMin - snapshot.getPrestigeAtLastReview() : 0;
    }

    /** The funding a review's bracket pays for a prestige, after the catch-up shift. */
    private static long reviewRate(BudgetSchedule.Review bracket, long catchUpShift, long prestige) {
        return interpolate(bracket.minPrestige() - catchUpShift, bracket.minFunding(),
            bracket.maxPrestige() - catchUpShift, bracket.maxFunding(), prestige);
    }

    /**
     * What the next funding review looks like from today ({@code ui-spec.md} &sect;4.10, gap G7).
     *
     * @param reviewNumber   the next review's number, from 1
     * @param seasonsUntil   how many resolutions until it, counting the next one (1 = the next turn)
     * @param catchUpShift   the shift it will apply, from the last review's baseline
     * @param minPrestige    its bracket's bottom prestige, shifted
     * @param minFunding     the funding at the bottom
     * @param maxPrestige    its bracket's top prestige, shifted
     * @param maxFunding     the funding at the top
     * @param payToday       what it would pay at today's prestige
     * @param currentRate    today's funding per season
     * @param keepPrestige   the least prestige that keeps today's funding, or -1 if none can
     */
    public record ReviewForecast(int reviewNumber, int seasonsUntil, long catchUpShift, long minPrestige,
                                 long minFunding, long maxPrestige, long maxFunding, long payToday, long currentRate,
                                 long keepPrestige) {

        /**
         * Returns what the review would pay at a given prestige.
         *
         * @param prestige the prestige at the review
         * @return the funding per season
         */
        public long payAt(long prestige) {
            return interpolate(minPrestige, minFunding, maxPrestige, maxFunding, prestige);
        }
    }

    /**
     * Forecasts the next funding review: when it comes and what today's prestige would earn
     * there. The season pass runs a review when the player's own season count, counting the
     * season being resolved ({@link PlayerSnapshot#getSeason()}), is a multiple of
     * {@link #SEASONS_PER_REVIEW}.
     *
     * @param snapshot the player
     * @param schedule the bracket table
     * @return the forecast
     */
    public static ReviewForecast forecast(PlayerSnapshot snapshot, BudgetSchedule schedule) {
        return forecast(snapshot, schedule, snapshot.getPrestige());
    }

    /**
     * Forecasts the next funding review as if the player held a given prestige, such as after a
     * conversion.
     *
     * @param snapshot the player
     * @param schedule the bracket table
     * @param prestige the prestige to price
     * @return the forecast
     */
    public static ReviewForecast forecast(PlayerSnapshot snapshot, BudgetSchedule schedule, long prestige) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(schedule, "schedule must not be null");
        int season = Math.max(1, snapshot.getSeason());
        int seasonsUntil = (SEASONS_PER_REVIEW - season % SEASONS_PER_REVIEW) % SEASONS_PER_REVIEW + 1;
        int reviewNumber = (season + seasonsUntil - 1) / SEASONS_PER_REVIEW;
        long shift = catchUpShift(snapshot, schedule, reviewNumber);
        BudgetSchedule.Review bracket = schedule.reviewFor(reviewNumber);
        long min = bracket.minPrestige() - shift;
        long max = bracket.maxPrestige() - shift;
        long current = snapshot.getFundingRatePerSeason();
        long keep = -1;
        if (reviewRate(bracket, shift, max) >= current) {
            if (reviewRate(bracket, shift, min) >= current) {
                keep = Math.max(0, min);
            } else {
                // The rate rises with prestige: find the least prestige that pays today's rate.
                long low = min;
                long high = max;
                while (high - low > 1) {
                    long mid = low + (high - low) / 2;
                    if (reviewRate(bracket, shift, mid) >= current) {
                        high = mid;
                    } else {
                        low = mid;
                    }
                }
                keep = high;
            }
        }
        return new ReviewForecast(reviewNumber, seasonsUntil, shift, min, bracket.minFunding(), max,
            bracket.maxFunding(), reviewRate(bracket, shift, prestige), current, keep);
    }

    /**
     * One line of the detailed running costs.
     *
     * @param label  what it is, such as "Astronauts (6)"
     * @param amount its cost per season, before the cost multiplier
     */
    public record CostLine(String label, long amount) {
    }

    /**
     * A group of cost lines, such as "Salaries".
     *
     * @param title the group's title
     * @param lines its lines
     */
    public record CostGroup(String title, List<CostLine> lines) {

        /**
         * Copies the lines.
         */
        public CostGroup {
            lines = List.copyOf(lines);
        }
    }

    /**
     * The running costs line by line ({@code ui-spec.md} &sect;4.10): salaries per department,
     * program and active-component upkeep, and upkeep per building, then any cost multiplier.
     *
     * @param groups            the lines, before the multiplier; they add up to {@code base}
     * @param base              every line together
     * @param total             what the season pass charges, {@link #runningCosts}
     * @param multiplier        the cost multiplier in force, 1 when none
     * @param multiplierSeasons how many more seasons it runs, 0 when none
     */
    public record CostDetail(List<CostGroup> groups, long base, long total, double multiplier,
                             int multiplierSeasons) {

        /**
         * Copies the groups.
         */
        public CostDetail {
            groups = List.copyOf(groups);
        }

        /**
         * Returns what the multiplier saves (or adds, when negative) this season.
         *
         * @return base less total
         */
        public long saving() {
            return base - total;
        }
    }

    /**
     * Returns the running costs line by line. Salaries round per person, as the season pass does,
     * so the base matches {@link #runningCostBreakdown} before its multiplier.
     *
     * @param snapshot the player's state
     * @param catalog  the content catalog
     * @return the detail
     */
    public static CostDetail costDetail(PlayerSnapshot snapshot, ContentCatalog catalog) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(catalog, "catalog must not be null");
        List<CostLine> salaries = List.of(
            salaryLine("Astronauts", snapshot.getAstronauts()),
            salaryLine("SET", snapshot.getSetSpecialists()),
            salaryLine("Flight controllers", snapshot.getMissionControlOfficers()));
        long programs = 0;
        for (String programId : snapshot.getOpenedProgramIds()) {
            programs += catalog.program(programId).orElseThrow().maintenanceCost();
        }
        long components = 0;
        int componentCount = 0;
        for (var owned : snapshot.getMissionComponents().entrySet()) {
            if (owned.getValue().isActive()) {
                components += catalog.missionComponent(owned.getKey()).orElseThrow().maintenanceCost();
                componentCount++;
            }
        }
        List<CostLine> upkeep = List.of(
            new CostLine("Program upkeep (" + snapshot.getOpenedProgramIds().size() + " open)", programs),
            new CostLine("Component upkeep (" + componentCount + " active)", components));
        List<CostLine> buildings = new ArrayList<>();
        for (Building building : Building.values()) {
            int level = snapshot.buildingLevel(building);
            long cost = catalog.buildings().maintenance(building, level);
            if (cost != 0) {
                buildings.add(new CostLine(buildingName(building) + ", level " + level, cost));
            }
        }
        List<CostGroup> groups = List.of(new CostGroup("Salaries", salaries),
            new CostGroup("Programs and components", upkeep), new CostGroup("Buildings", buildings));
        long base = groups.stream().flatMap(group -> group.lines().stream()).mapToLong(CostLine::amount).sum();
        boolean active = snapshot.getCostMultiplierSeasonsRemaining() > 0;
        return new CostDetail(groups, base, runningCosts(snapshot, catalog), snapshot.getCostMultiplier(),
            active ? snapshot.getCostMultiplierSeasonsRemaining() : 0);
    }

    private static CostLine salaryLine(String label, List<? extends Personnel> roster) {
        long total = 0;
        for (Personnel person : roster) {
            total += Math.round(person.getSalary());
        }
        return new CostLine(label + " (" + roster.size() + ")", total);
    }

    private static String buildingName(Building building) {
        return switch (building) {
            case HEADQUARTERS -> "Headquarters";
            case VEHICLE_ASSEMBLY -> "VAB";
            case ASTRONAUT_CENTER -> "Astronaut Complex";
            case SET_CENTER -> "SET Centre";
            case MISSION_CONTROL -> "Mission Control";
        };
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
