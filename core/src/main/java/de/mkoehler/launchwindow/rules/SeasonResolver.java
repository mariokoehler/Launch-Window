package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.PrPress;
import de.mkoehler.launchwindow.content.BudgetSchedule;
import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.RandomEventRoster;
import de.mkoehler.launchwindow.model.PlayerSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

/**
 * <p>One player's whole season pass: every season resolver, in the composition
 * order the rules depend on. The one place that order lives, used by the server's
 * turn resolution and by the bot harness alike ({@code design.md} &sect;5, M4).
 * The player's journaled actions must already be applied; they come first.</p>
 *
 * <p>Order: {@link Phase#PERSONNEL_LIFECYCLE}, {@link Phase#MISSIONS},
 * {@link Phase#BUILDINGS} (an upgrade lands before the budget, so its new upkeep
 * is charged the season it finishes), {@link Phase#BUDGET},
 * {@link Phase#RANDOM_EVENTS}, {@link Phase#TRAINING}, {@link Phase#RESEARCH},
 * {@link Phase#TEAMS}, and {@link Phase#HIRING_POOL} last.</p>
 *
 * <p><b>Two clocks.</b> {@link Context#gameTurn()} is the shared game turn: goal
 * achievements, news, and flight and failure turns are stamped with it, and the
 * win check orders players by it. {@link Context#seasonsElapsed()} is the player's
 * own season count including this one, which drives budget reviews and the random
 * events' review-lead gate, so a late joiner gets the same budget arc as everyone
 * else (user decision, 2026-10-03). The two are equal only for a player who joined
 * at the start.</p>
 *
 * <p>The goal tally is read here but never changed: the caller commits every
 * player's mission events to it after all players have resolved, so same-turn
 * achievers all price first-time ({@code design.md} &sect;2.6(d)).</p>
 *
 * @author Mario Koehler
 */
public final class SeasonResolver {

    /** The phases of a season pass, in the order they run. */
    public enum Phase {
        /** Ageing, morale, retirement, and resignation. */
        PERSONNEL_LIFECYCLE,
        /** Every mission in flight resolves its season. */
        MISSIONS,
        /** Building upgrades count down and land. */
        BUILDINGS,
        /** Astronauts on PR duty earn prestige, before the budget so a review this season counts it. */
        PR_DUTY,
        /** Running costs, the review if due, and funding. */
        BUDGET,
        /** Random events and their timed state. */
        RANDOM_EVENTS,
        /** Training rounds count down and complete. */
        TRAINING,
        /** R&D on mission components. */
        RESEARCH,
        /** Teamwork: work seasons, pairs' rapport and traits shown (design.md 2.2(d)). */
        TEAMS,
        /** Fresh candidate pools. */
        HIRING_POOL
    }

    /**
     * Everything a season pass reads besides the player and the random source.
     *
     * @param catalog        the content pack
     * @param schedule       the budget schedule
     * @param roster         the random-event content
     * @param goalTally      the game-wide goal counts as of the start of this turn; read only
     * @param gameTurn       the shared game turn being resolved
     * @param seasonsElapsed the player's own seasons elapsed, counting this one
     */
    public record Context(ContentCatalog catalog, BudgetSchedule schedule, RandomEventRoster roster,
                          GoalTally goalTally, int gameTurn, int seasonsElapsed) {

        /**
         * Validates the context.
         *
         * @throws NullPointerException     if a reference is {@code null}
         * @throws IllegalArgumentException if {@code seasonsElapsed} is below 1
         */
        public Context {
            Objects.requireNonNull(catalog, "catalog must not be null");
            Objects.requireNonNull(schedule, "schedule must not be null");
            Objects.requireNonNull(roster, "roster must not be null");
            Objects.requireNonNull(goalTally, "goalTally must not be null");
            if (seasonsElapsed < 1) {
                throw new IllegalArgumentException("seasonsElapsed must be at least 1: " + seasonsElapsed);
            }
        }
    }

    /**
     * Watches a season pass phase by phase - how the bot harness keeps its
     * independent checks around each resolver. Both methods default to doing nothing.
     */
    public interface Observer {

        /** An observer that watches nothing. */
        Observer NONE = new Observer() { };

        /**
         * Called just before a phase runs.
         *
         * @param phase    the phase about to run
         * @param snapshot the player, as the previous phase left it
         */
        default void before(Phase phase, PlayerSnapshot snapshot) {
        }

        /**
         * Called just after a phase runs.
         *
         * @param phase    the phase that ran
         * @param snapshot the player, as the phase left it
         * @param events   the phase's events
         */
        default void after(Phase phase, PlayerSnapshot snapshot, List<ResolutionEvent> events) {
        }
    }

    /**
     * Not instantiable; this class only exposes static methods.
     */
    private SeasonResolver() {
    }

    /**
     * Runs one player's season pass with no observer.
     *
     * @param snapshot the player, actions already applied; mutated in place
     * @param context  the turn's context
     * @param random   the random source for the whole pass
     * @return every event, in phase order
     */
    public static List<ResolutionEvent> advanceSeason(PlayerSnapshot snapshot, Context context,
            RandomGenerator random) {
        return advanceSeason(snapshot, context, random, Observer.NONE);
    }

    /**
     * Runs one player's season pass, telling the observer before and after each phase.
     *
     * @param snapshot the player, actions already applied; mutated in place
     * @param context  the turn's context
     * @param random   the random source for the whole pass
     * @param observer the observer
     * @return every event, in phase order
     * @throws NullPointerException if an argument is {@code null}
     */
    public static List<ResolutionEvent> advanceSeason(PlayerSnapshot snapshot, Context context,
            RandomGenerator random, Observer observer) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(random, "random must not be null");
        Objects.requireNonNull(observer, "observer must not be null");
        List<ResolutionEvent> all = new ArrayList<>();
        List<Teams.Team> teams = List.of();
        for (Phase phase : Phase.values()) {
            observer.before(phase, snapshot);
            List<ResolutionEvent> events = phase == Phase.TEAMS ? Teams.advanceSeason(snapshot, teams)
                : run(phase, snapshot, context, random);
            if (phase == Phase.PERSONNEL_LIFECYCLE) {
                // This season's teams, after departures and before missions resolve (a finishing
                // mission releases its crew).
                teams = Teams.capture(snapshot, context.catalog());
            }
            observer.after(phase, snapshot, events);
            all.addAll(events);
        }
        DepartmentSalaries.startNewSeason(snapshot);
        return all;
    }

    private static List<ResolutionEvent> run(Phase phase, PlayerSnapshot snapshot, Context context,
            RandomGenerator random) {
        return switch (phase) {
            case PERSONNEL_LIFECYCLE -> PersonnelLifecycle.advanceSeason(snapshot);
            case MISSIONS -> MissionProgress.advanceSeason(snapshot, context.catalog(), random, context.goalTally(),
                context.gameTurn());
            case BUILDINGS -> BuildingUpgrades.advanceSeason(snapshot, context.catalog().buildings());
            case PR_DUTY -> PrDuty.advanceSeason(snapshot, PrPress.standard(), random, context.catalog(),
                context.seasonsElapsed());
            case BUDGET -> BudgetProgress.advanceSeason(snapshot, context.catalog(), context.schedule(),
                context.seasonsElapsed());
            case RANDOM_EVENTS -> RandomEvents.advanceSeason(snapshot, context.roster(), random, context.gameTurn(),
                context.seasonsElapsed());
            case TRAINING -> TrainingProgress.advanceSeason(snapshot, random);
            case RESEARCH -> ResearchProgress.advanceSeason(snapshot, context.catalog(), random);
            case TEAMS -> throw new IllegalStateException("teams run with their captured teams");
            case HIRING_POOL -> {
                HiringPool.refreshCandidates(snapshot, random);
                yield List.of();
            }
        };
    }
}
