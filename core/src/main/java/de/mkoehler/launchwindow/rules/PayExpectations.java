package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.model.CareerEntry;
import de.mkoehler.launchwindow.model.Personnel;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.Trait;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * <p>Pay expectations ({@code design.md} &sect;2.2(c); user, 2026-11-13; every number <em>unconfirmed</em>): each
 * person expects a salary that rises at every funding review and with their performance; being paid less caps their
 * morale, and morale changes how well they work. A person can be raised on their own, and a department brought up to
 * its expectations at once.</p>
 *
 * <p>Each part is off until its screens are built ({@link #setParts}); the playouts turn them on with
 * {@code -Drules.pay}. Off, a person's expectation still follows their salary at hiring, so nothing else changes.</p>
 *
 * @author Mario Koehler
 */
public final class PayExpectations {

    /** How much every expectation rises at a funding review (the market). */
    public static final double REVIEW_STEP = 0.10;
    /** A flight, console or CAPCOM shift that ended without a failed step. */
    public static final double FLIGHT_STEP = 0.02;
    /** Aboard (crew or ground) a mission that settled a goal first in the game. */
    public static final double FIRST_STEP = 0.10;
    /** Earning the Expert trait. */
    public static final double EXPERT_STEP = 0.10;
    /** A fulfilled ambition. */
    public static final double AMBITION_STEP = 0.05;
    /** An engineer's finished R&amp;D stint, per {@link #RD_POINTS} reliability points they added. */
    public static final double RD_STEP = 0.01;
    /** The reliability points an R&amp;D stint adds for one {@link #RD_STEP}. */
    public static final double RD_POINTS = 5.0;
    /** The most performance can add in one turn. */
    public static final double MAX_PERFORMANCE_STEP = 0.15;
    /**
     * Morale points the ceiling drops per percent underpaid: one review unanswered (9 % under) and morale stays
     * below {@link #HIGH_MORALE}; two (17 %), below {@link #LOW_MORALE}; three (25 %), at the resignation line.
     */
    public static final int CEILING_PER_PERCENT = 3;
    /** Underpaid by this much or more, in percent, and the bots raise (the ledger's line too). */
    public static final double BOT_RAISE_AT = 5;
    /** How far morale above the ceiling falls a season (halved for Steady). */
    public static final int DRIFT = 2;
    /** Morale from which a person works better. */
    public static final int HIGH_MORALE = 80;
    /** Morale below which a person works worse. */
    public static final int LOW_MORALE = 50;
    /** How much better or worse, as a share of their skill. */
    public static final double MORALE_SKILL = 0.05;
    /** A person's own raise: the steps offered besides meeting their expectation, in percent. */
    public static final List<Integer> RAISE_STEPS = List.of(5, 10);
    /** The largest raise of one person, in percent. */
    public static final int MAX_RAISE_PERCENT = DepartmentSalaries.MAX_PERCENT;

    private static boolean market;
    private static boolean performance;
    private static boolean ceiling;
    private static boolean upside;

    private PayExpectations() {
    }

    /**
     * Turns the parts on or off: off for the game until the screens are built, on in playouts that measure them.
     *
     * @param marketOn      the review step
     * @param performanceOn the performance steps
     * @param ceilingOn     the morale ceiling
     * @param upsideOn      morale's effect on skill
     */
    public static void setParts(boolean marketOn, boolean performanceOn, boolean ceilingOn, boolean upsideOn) {
        market = marketOn;
        performance = performanceOn;
        ceiling = ceilingOn;
        upside = upsideOn;
    }

    /**
     * Returns whether any part is on.
     *
     * @return whether it is
     */
    public static boolean anyOn() {
        return market || performance || ceiling || upside;
    }

    /**
     * Returns how far below their expectation someone is paid, in percent.
     *
     * @param person the person
     * @return 0 when paid as expected or more
     */
    public static double underpaidPercent(Personnel person) {
        double expected = person.expectedSalary();
        return expected <= 0 || person.getSalary() >= expected ? 0 : 100.0 * (1 - person.getSalary() / expected);
    }

    /**
     * Returns the highest morale work can bring someone to at their pay.
     *
     * @param person the person
     * @return 100 when the ceiling is off or they're paid as expected
     */
    public static int moraleCeiling(Personnel person) {
        if (!ceiling) {
            return PersonnelLifecycle.MORALE_MAX;
        }
        return (int) Math.max(PersonnelLifecycle.MORALE_MIN,
            Math.round(PersonnelLifecycle.MORALE_MAX - CEILING_PER_PERCENT * underpaidPercent(person)));
    }

    /**
     * Returns someone's morale after the season's own change, under their pay's ceiling: work fills it only up to
     * the ceiling, and above it morale drifts down.
     *
     * @param person the person, their morale before the change
     * @param delta  the season's change before the ceiling ({@link TraitEffects#seasonMorale})
     * @return the new morale, not yet clamped to 0-100
     */
    static int withCeiling(Personnel person, int delta) {
        int morale = person.getMorale();
        int top = moraleCeiling(person);
        if (morale > top) {
            int drift = Personality.has(person, Trait.STEADY) ? DRIFT / 2 : DRIFT;
            return Math.max(top, morale + Math.min(delta, -drift));
        }
        return delta > 0 ? Math.min(top, morale + delta) : morale + delta;
    }

    /**
     * Returns what someone's morale does to their skill at work: more from {@link #HIGH_MORALE}, less below
     * {@link #LOW_MORALE}.
     *
     * @param person the person, or {@code null}
     * @return the factor, 1 when the part is off
     */
    public static double skillFactor(Personnel person) {
        if (!upside || person == null) {
            return 1;
        }
        if (person.getMorale() >= HIGH_MORALE) {
            return 1 + MORALE_SKILL;
        }
        return person.getMorale() < LOW_MORALE ? 1 - MORALE_SKILL : 1;
    }

    /**
     * The season's pass, after careers: every expectation rises at a funding review, and with the turn's
     * performance read from the career lines it added.
     *
     * @param snapshot       the player
     * @param gameTurn       the turn resolved
     * @param seasonsElapsed the player's own seasons, counting this one
     * @return the events: the review's rise, and each person's performance rise
     */
    public static List<ResolutionEvent> advanceSeason(PlayerSnapshot snapshot, int gameTurn, long seasonsElapsed) {
        List<ResolutionEvent> events = new ArrayList<>();
        boolean review = market && seasonsElapsed > 0 && seasonsElapsed % BudgetProgress.SEASONS_PER_REVIEW == 0;
        for (Personnel person : everyone(snapshot)) {
            pin(person);
            double before = person.expectedSalary();
            List<String> reasons = new ArrayList<>();
            double step = performance ? Math.min(MAX_PERFORMANCE_STEP, performanceStep(person, gameTurn, reasons)) : 0;
            double after = before * (1 + step) * (review ? 1 + REVIEW_STEP : 1);
            if (after != before) {
                person.setExpectedSalary(after);
            }
            if (step > 0) {
                events.add(new ExpectationRose(groupOf(person), person.getId(), person.getName(), before,
                    before * (1 + step), Math.round(step * 100), reasons));
            }
        }
        if (review) {
            events.add(new ExpectationsRoseAtReview(
                (int) (seasonsElapsed / BudgetProgress.SEASONS_PER_REVIEW), Math.round(REVIEW_STEP * 100)));
        }
        return events;
    }

    /** What the turn's career lines add for one person. */
    private static double performanceStep(Personnel person, int gameTurn, List<String> reasons) {
        double step = 0;
        for (CareerEntry line : person.getCareer()) {
            if (line.turn() != gameTurn) {
                continue;
            }
            double add = switch (line.kind()) {
                case FLIGHT -> line.outcome().isEmpty() ? FLIGHT_STEP : 0;
                case FIRST -> FIRST_STEP;
                case EXPERT -> EXPERT_STEP;
                case AMBITION -> AmbitionSettled.Outcome.FULFILLED.name().equals(line.outcome()) ? AMBITION_STEP : 0;
                case RD_STINT -> Math.max(0, line.value()) / RD_POINTS * RD_STEP;
                default -> 0;
            };
            if (add > 0) {
                step += add;
                reasons.add(line.kind().name());
            }
        }
        return step;
    }

    /** The result of a raise. */
    public enum RaiseOutcome {
        /** The salary rose. */
        RAISED,
        /** Nobody by that id. */
        UNKNOWN_PERSON,
        /** Not a raise, or more than {@link #MAX_RAISE_PERCENT}. */
        INVALID_RAISE,
        /** Already raised this season. */
        ALREADY_RAISED
    }

    /**
     * Returns the salary a raise would bring someone to.
     *
     * @param person  the person
     * @param percent the raise, or 0 for their expectation
     * @return the new salary
     */
    public static double raisedSalary(Personnel person, int percent) {
        return percent == 0 ? Math.max(person.getSalary(), person.expectedSalary())
            : person.getSalary() * (1 + percent / 100.0);
    }

    /**
     * Raises one person, once a season: to their expectation, or by a percentage. Morale rises a point per percent
     * and loyalty by half, as a department raise.
     *
     * @param snapshot the player
     * @param personId the person
     * @param percent  the raise, 1 to {@link #MAX_RAISE_PERCENT}, or 0 for their expectation (however far)
     * @return the outcome; nothing changes unless {@link RaiseOutcome#RAISED}
     */
    public static RaiseOutcome raise(PlayerSnapshot snapshot, String personId, int percent) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Personnel person = everyone(snapshot).stream().filter(p -> p.getId().equals(personId)).findFirst().orElse(null);
        if (person == null) {
            return RaiseOutcome.UNKNOWN_PERSON;
        }
        if (snapshot.getRaisedThisSeason().contains(personId)) {
            return RaiseOutcome.ALREADY_RAISED;
        }
        if (percent < 0 || percent > MAX_RAISE_PERCENT) {
            return RaiseOutcome.INVALID_RAISE;
        }
        double salary = raisedSalary(person, percent);
        int actual = (int) Math.round(100 * (salary / person.getSalary() - 1));
        // Meeting an expectation has no cap, as the department's doesn't; a percentage raise does.
        if (salary <= person.getSalary()) {
            return RaiseOutcome.INVALID_RAISE;
        }
        apply(person, salary, actual);
        snapshot.getRaisedThisSeason().add(personId);
        return RaiseOutcome.RAISED;
    }

    /**
     * Brings everyone in a department paid below their expectation up to it, each with their own morale gain:
     * Manage department's quality-of-life choice, using the department's once a season.
     *
     * @param snapshot the player
     * @param group    the department
     * @return the outcome
     */
    public static DepartmentSalaries.AdjustOutcome meetExpectations(PlayerSnapshot snapshot, PersonnelGroup group) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(group, "group must not be null");
        List<? extends Personnel> roster = roster(snapshot, group);
        if (roster.isEmpty()) {
            return DepartmentSalaries.AdjustOutcome.EMPTY_DEPARTMENT;
        }
        if (DepartmentSalaries.adjustedThisSeason(snapshot, group)) {
            return DepartmentSalaries.AdjustOutcome.ALREADY_ADJUSTED;
        }
        List<? extends Personnel> under = roster.stream().filter(p -> p.getSalary() < p.expectedSalary()).toList();
        if (under.isEmpty()) {
            return DepartmentSalaries.AdjustOutcome.NOBODY_UNDERPAID;
        }
        for (Personnel person : under) {
            double salary = person.expectedSalary();
            apply(person, salary, (int) Math.round(100 * (salary / person.getSalary() - 1)));
        }
        snapshot.getSalaryAdjustedGroups().add(group.name());
        return DepartmentSalaries.AdjustOutcome.ADJUSTED;
    }

    /**
     * Returns what bringing a department up to its expectations adds to the salary bill each season.
     *
     * @param snapshot the player
     * @param group    the department
     * @return the extra per season
     */
    public static double costToMeet(PlayerSnapshot snapshot, PersonnelGroup group) {
        return roster(snapshot, group).stream().mapToDouble(p -> Math.max(0, p.expectedSalary() - p.getSalary()))
            .sum();
    }

    private static void apply(Personnel person, double salary, int percent) {
        pin(person);
        person.setSalary(salary);
        person.setMorale(DepartmentSalaries.adjustedMorale(person.getMorale(), Math.max(0, percent)));
        Careers.salaryChanged(List.of(person), Math.max(0, percent));
    }

    /**
     * The bots' pay: each department with anyone underpaid by {@link #BOT_RAISE_AT} % or more is brought up to its
     * expectations when the funds above the reserve cover two seasons of it; otherwise its best people are raised
     * one by one, as far as that goes.
     *
     * @param snapshot the player
     * @param reserve  the funds the bot keeps back
     */
    public static void botPolicy(PlayerSnapshot snapshot, long reserve) {
        for (int i = 0; i < 200; i++) {
            BotStep step = nextBotStep(snapshot, reserve);
            if (step == null) {
                return;
            }
            if (step.personnelId() == null) {
                meetExpectations(snapshot, step.group());
            } else {
                raise(snapshot, step.personnelId(), 0);
            }
        }
    }

    /**
     * One step of the bots' pay.
     *
     * @param group       the department
     * @param personnelId the person to raise to their expectation, or {@code null} to bring the department up
     */
    public record BotStep(PersonnelGroup group, String personnelId) {
    }

    /**
     * Returns the bots' next pay step ({@link #botPolicy}), without changing anything: for a bot that proposes its
     * actions one at a time.
     *
     * @param snapshot the player
     * @param reserve  the funds the bot keeps back
     * @return the step, or {@code null} when there's none
     */
    public static BotStep nextBotStep(PlayerSnapshot snapshot, long reserve) {
        if (!anyOn()) {
            return null;
        }
        double spare = snapshot.getFunds() - reserve;
        for (PersonnelGroup group : PersonnelGroup.values()) {
            List<? extends Personnel> roster = roster(snapshot, group);
            if (roster.stream().noneMatch(p -> underpaidPercent(p) >= BOT_RAISE_AT)) {
                continue;
            }
            if (!DepartmentSalaries.adjustedThisSeason(snapshot, group) && spare >= 2 * costToMeet(snapshot, group)) {
                return new BotStep(group, null);
            }
            Personnel best = roster.stream()
                .filter(p -> underpaidPercent(p) >= BOT_RAISE_AT && !snapshot.getRaisedThisSeason().contains(p.getId()))
                .filter(p -> spare >= 2 * (p.expectedSalary() - p.getSalary()))
                .max(Comparator.comparingDouble(Chemistry::bestSkill)).orElse(null);
            if (best != null) {
                return new BotStep(group, best.getId());
            }
        }
        return null;
    }

    /**
     * Fixes someone's expectation at their salary if it was never set, before their salary changes, so a raise
     * doesn't carry the expectation along.
     *
     * @param person the person
     */
    public static void pin(Personnel person) {
        if (person.getExpectedSalary() == null) {
            person.setExpectedSalary(person.getSalary());
        }
    }

    private static PersonnelGroup groupOf(Personnel person) {
        if (person instanceof de.mkoehler.launchwindow.model.Astronaut) {
            return PersonnelGroup.ASTRONAUT;
        }
        return person instanceof de.mkoehler.launchwindow.model.MissionControlOfficer
            ? PersonnelGroup.MISSION_CONTROL : PersonnelGroup.SET;
    }

    private static List<? extends Personnel> roster(PlayerSnapshot snapshot, PersonnelGroup group) {
        return switch (group) {
            case ASTRONAUT -> snapshot.getAstronauts();
            case SET -> snapshot.getSetSpecialists();
            case MISSION_CONTROL -> snapshot.getMissionControlOfficers();
        };
    }

    private static List<Personnel> everyone(PlayerSnapshot snapshot) {
        List<Personnel> all = new ArrayList<>();
        all.addAll(snapshot.getAstronauts());
        all.addAll(snapshot.getMissionControlOfficers());
        all.addAll(snapshot.getSetSpecialists());
        return all;
    }
}
