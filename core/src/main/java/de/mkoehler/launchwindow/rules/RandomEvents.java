package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.EventStories;
import de.mkoehler.launchwindow.content.RandomEventRoster;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.MissionControlOfficer;
import de.mkoehler.launchwindow.model.MemorialEntry;
import de.mkoehler.launchwindow.model.NewsEntry;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.Personnel;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.RandomEventKind;
import de.mkoehler.launchwindow.model.SetSpecialist;
import de.mkoehler.launchwindow.model.TrainingKind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.SplittableRandom;
import java.util.random.RandomGenerator;

/**
 * <p>The random-events season pass: two independent rolls per season, one for a
 * bad event and one for a good event. Each roll that succeeds applies the first
 * event, in a shuffled pool, whose gate passes. Gates are situational, as BASPM's
 * are ({@code baspm.md} &sect;12.1): a bad event needs a real precondition, such
 * as a component in R&amp;D or a recent failure. An M2 rules-engine resolver,
 * headless.</p>
 *
 * <p>All twenty events are implemented. Timed effects (multipliers, the launch cap,
 * component and employee unavailability) are aged once per season by
 * {@link #tickTimedState}, before the roll, so an event that lasts N seasons covers N
 * seasons of budget, research, and launches.</p>
 *
 * <p><b>Deliberate departures from BASPM:</b> BASPM never reads its own R&amp;D and
 * cost multiplier factors, so its R&amp;D and reduced-expenditure events change
 * nothing in the shipped game. Launch Window applies what each event's news text
 * describes: R&amp;D gains (skill and reliability alike) and running costs are
 * scaled. Launch caps and unavailability are enforced by {@code MissionLaunch};
 * unavailable employees are skipped by launches, but still train and research.</p>
 *
 * <p><b>Deliberate deviation from BASPM, for personnel events:</b> dismissal and
 * resignation take any employee who isn't on a mission. BASPM takes any active
 * employee, which would strand a crew member mid-flight; Launch Window's
 * assignment invariants forbid that. Idle astronauts only, for the astronaut group,
 * as BASPM does.</p>
 *
 * @author Mario Koehler
 */
public final class RandomEvents {

    /** Seasons of recent history a "recent success" or "recent failure" gate looks back over. */
    private static final int RECENT_SEASONS = 4;

    /** Seasons of budget review lead time an event needs to be worth applying - BASPM's own gate. */
    static final int MIN_SEASONS_UNTIL_REVIEW = 6;

    /** Seasons between budget reviews, as {@link BudgetProgress} uses. */
    private static final int SEASONS_PER_REVIEW = BudgetProgress.SEASONS_PER_REVIEW;

    /**
     * Not instantiable; this class only exposes static methods.
     */
    private RandomEvents() {
    }

    /**
     * Rolls one season's random events for one player.
     *
     * @param snapshot       the player; mutated in place
     * @param roster         the event content: odds, pools, and ranges
     * @param random         the random source
     * @param turn           the game turn this season resolves on
     * @param seasonsElapsed the player's seasons elapsed counting this one, for the review-lead gate
     * @return the events that applied, in order; each also appended to the news log
     * @throws NullPointerException if an argument is {@code null}
     */
    public static List<ResolutionEvent> advanceSeason(PlayerSnapshot snapshot, RandomEventRoster roster,
            RandomGenerator random, int turn, int seasonsElapsed) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(roster, "roster must not be null");
        Objects.requireNonNull(random, "random must not be null");
        tickTimedState(snapshot);
        List<ResolutionEvent> events = new ArrayList<>();
        if (random.nextDouble() * 100 < roster.badChancePercent()) {
            rollPool(false, snapshot, roster, random, turn, seasonsElapsed, events);
        }
        if (random.nextDouble() * 100 < roster.goodChancePercent()) {
            rollPool(true, snapshot, roster, random, turn, seasonsElapsed, events);
        }
        List<NewsEntry> log = snapshot.getNewsLog();
        while (log.size() > PlayerSnapshot.NEWS_LOG_CAPACITY) {
            log.remove(0);
        }
        return events;
    }

    /**
     * Applies the first event in a shuffled pool whose gate passes, if any does.
     *
     * @param positive       which pool to roll: good events if {@code true}
     * @param snapshot       the player
     * @param roster         the content
     * @param random         the random source
     * @param turn           the game turn
     * @param seasonsElapsed the seasons-elapsed count
     * @param events         the list to append applied events to
     */
    private static void rollPool(boolean positive, PlayerSnapshot snapshot, RandomEventRoster roster,
            RandomGenerator random, int turn, int seasonsElapsed, List<ResolutionEvent> events) {
        List<RandomEventKind> pool = new ArrayList<>();
        for (RandomEventKind kind : RandomEventKind.values()) {
            if (kind.isPositive() == positive) {
                pool.add(kind);
            }
        }
        shuffle(pool, random);
        for (RandomEventKind kind : pool) {
            if (apply(kind, snapshot, roster, random, turn, seasonsElapsed, events)) {
                return;
            }
        }
    }

    /**
     * Checks one event's gate and, if it passes, applies its effect.
     *
     * @return {@code true} if the event applied
     */
    private static boolean apply(RandomEventKind kind, PlayerSnapshot snapshot, RandomEventRoster roster,
            RandomGenerator random, int turn, int seasonsElapsed, List<ResolutionEvent> events) {
        return switch (kind) {
            case BASIC_TRAINING_EXTENSION -> extendBasicTraining(snapshot, random, turn, events);
            case EMPLOYEE_DISMISSED -> departAnyEligible(snapshot, random, DepartureReason.DISMISSED, kind, turn, events);
            case EMPLOYEE_RESIGNING -> departAnyEligible(snapshot, random, DepartureReason.RESIGNED, kind, turn, events);
            case EMPLOYEE_FAILED_BASIC_TRAINING -> failBasicTraining(snapshot, random, turn, events);
            case PERISHED_ASTRONAUT -> perishAstronaut(snapshot, random, turn, events);
            case MORALE_DROP -> shiftMorale(kind, -1, snapshot, roster, random, turn, events);
            case MORALE_BOOST -> shiftMorale(kind, +1, snapshot, roster, random, turn, events);
            case PRESTIGE_DROP -> shiftPrestige(kind, -1, snapshot, roster, random, turn, events);
            case PRESTIGE_POINTS_BOOST -> shiftPrestige(kind, +1, snapshot, roster, random, turn, events);
            case UNEXPECTED_COST -> unexpectedCost(snapshot, roster, random, turn, events);
            case RELIABILITY_SETBACK -> reliabilitySetback(snapshot, roster, random, turn, events);
            case EXTRA_FUNDING -> extraFunding(snapshot, roster, random, turn, seasonsElapsed, events);
            case UNEXPECTED_COST_AND_NO_LAUNCHES -> unexpectedCostAndNoLaunches(snapshot, roster, random, turn,
                events);
            case MAX_LAUNCHES_ALLOWED -> capLaunches(snapshot, 1, kind, turn, events);
            case NO_LAUNCHES_ALLOWED -> capLaunches(snapshot, 0, kind, turn, events);
            case MISSION_COMPONENTS_UNAVAILABLE -> componentsUnavailable(snapshot, roster, random, turn, events);
            case EMPLOYEE_UNAVAILABLE -> employeeUnavailable(snapshot, roster, random, turn, events);
            case RD_RESULTS_SETBACK, RD_RESULTS_BOOST -> rdMultiplier(kind, snapshot, roster, random, turn, events);
            case REDUCED_EXPENDITURES -> costMultiplier(snapshot, roster, random, turn, seasonsElapsed, events);
        };
    }

    /**
     * Ages the timed state by one season, before this season's roll: multipliers
     * count down and reset at zero, the launch cap and the window's launch count
     * reset, component and employee unavailability counts down. An event that sets
     * N seasons therefore covers the next N seasons' budget, research, and launches.
     *
     * @param snapshot the player
     */
    private static void tickTimedState(PlayerSnapshot snapshot) {
        if (snapshot.getRdResultsSeasonsRemaining() > 0) {
            int remaining = snapshot.getRdResultsSeasonsRemaining() - 1;
            snapshot.setRdResults(remaining == 0 ? 1.0 : snapshot.getRdResultsMultiplier(), remaining);
        }
        if (snapshot.getCostMultiplierSeasonsRemaining() > 0) {
            int remaining = snapshot.getCostMultiplierSeasonsRemaining() - 1;
            snapshot.setCostMultiplier(remaining == 0 ? 1.0 : snapshot.getCostMultiplier(), remaining);
        }
        snapshot.setLaunchCap(-1);
        snapshot.setLaunchesThisWindow(0);
        snapshot.getUnavailableComponents().replaceAll((componentId, windows) -> windows - 1);
        snapshot.getUnavailableComponents().values().removeIf(windows -> windows <= 0);
        for (PersonnelGroup group : PersonnelGroup.values()) {
            for (Personnel person : groupRoster(snapshot, group)) {
                person.setUnavailableSeasons(Math.max(0, person.getUnavailableSeasons() - 1));
            }
        }
    }

    /**
     * Cost-plus-no-launches: the unexpected cost, and no launches in the next window.
     * BASPM's own gate needs a vehicle-assembly building Launch Window doesn't have,
     * so this takes the same positive-funds gate as {@link #unexpectedCost}.
     */
    private static boolean unexpectedCostAndNoLaunches(PlayerSnapshot snapshot, RandomEventRoster roster,
            RandomGenerator random, int turn, List<ResolutionEvent> events) {
        if (snapshot.getFunds() <= 0) {
            return false;
        }
        RandomEventRoster.EventSpec spec = roster.spec(RandomEventKind.UNEXPECTED_COST_AND_NO_LAUNCHES);
        double percent = drawFraction(random, spec.min(), spec.max());
        long cost = (long) Math.floor(snapshot.getFunds() * percent / 100.0);
        snapshot.setFunds(snapshot.getFunds() - cost);
        snapshot.setLaunchCap(0);
        occurred(snapshot, events, RandomEventKind.UNEXPECTED_COST_AND_NO_LAUNCHES, null, percent, turn);
        return true;
    }

    /**
     * Sets the launch cap for the next launch window: one for {@code MAX_LAUNCHES_ALLOWED},
     * zero for {@code NO_LAUNCHES_ALLOWED}. Neither has a gate in Launch Window.
     */
    private static boolean capLaunches(PlayerSnapshot snapshot, int cap, RandomEventKind kind, int turn,
            List<ResolutionEvent> events) {
        snapshot.setLaunchCap(cap);
        occurred(snapshot, events, kind, null, cap, turn);
        return true;
    }

    /**
     * Makes half of the components currently in R&amp;D (at least one) unavailable for
     * a rolled number of launch windows. BASPM's own selection and duration.
     */
    private static boolean componentsUnavailable(PlayerSnapshot snapshot, RandomEventRoster roster,
            RandomGenerator random, int turn, List<ResolutionEvent> events) {
        List<String> inResearch = componentsInResearch(snapshot);
        if (inResearch.isEmpty()) {
            return false;
        }
        shuffle(inResearch, random);
        int count = Math.max(1, inResearch.size() / 2);
        RandomEventRoster.EventSpec spec = roster.spec(RandomEventKind.MISSION_COMPONENTS_UNAVAILABLE);
        int windows = drawInt(random, spec.durationMin(), spec.durationMax());
        for (String componentId : inResearch.subList(0, count)) {
            snapshot.getUnavailableComponents().put(componentId, windows);
        }
        occurred(snapshot, events, RandomEventKind.MISSION_COMPONENTS_UNAVAILABLE, null, windows, turn);
        return true;
    }

    /**
     * Makes one eligible employee unavailable for launches for a rolled number of
     * windows - the same eligibility as dismissal.
     */
    private static boolean employeeUnavailable(PlayerSnapshot snapshot, RandomEventRoster roster,
            RandomGenerator random, int turn, List<ResolutionEvent> events) {
        List<PersonnelGroup> groups = new ArrayList<>(List.of(PersonnelGroup.values()));
        shuffle(groups, random);
        for (PersonnelGroup group : groups) {
            List<Personnel> eligible = eligibleEmployees(snapshot, group);
            if (!eligible.isEmpty()) {
                Personnel chosen = eligible.get(random.nextInt(eligible.size()));
                RandomEventRoster.EventSpec spec = roster.spec(RandomEventKind.EMPLOYEE_UNAVAILABLE);
                int windows = drawInt(random, spec.durationMin(), spec.durationMax());
                chosen.setUnavailableSeasons(windows);
                occurred(snapshot, events, RandomEventKind.EMPLOYEE_UNAVAILABLE, chosen.getId(), windows, turn,
                    chosen.getName());
                return true;
            }
        }
        return false;
    }

    /**
     * Adds an R&amp;D results multiplier for a rolled number of seasons. A setback, a boost and a
     * press story share one slot: the factors multiply and the longer duration stays
     * ({@link TimedEffects}; BASPM's setter replaced the older one).
     */
    private static boolean rdMultiplier(RandomEventKind kind, PlayerSnapshot snapshot, RandomEventRoster roster,
            RandomGenerator random, int turn, List<ResolutionEvent> events) {
        RandomEventRoster.EventSpec spec = roster.spec(kind);
        double factor = drawFraction(random, spec.min(), spec.max());
        int seasons = drawInt(random, spec.durationMin(), spec.durationMax());
        TimedEffects.addRdResults(snapshot, factor, seasons);
        occurred(snapshot, events, kind, null, factor, turn);
        return true;
    }

    /**
     * Scales running costs for a rolled number of seasons, multiplying with any running cost
     * effect ({@link TimedEffects}). Needs at least
     * {@link #MIN_SEASONS_UNTIL_REVIEW} seasons until the next review, as BASPM's gate does.
     */
    private static boolean costMultiplier(PlayerSnapshot snapshot, RandomEventRoster roster, RandomGenerator random,
            int turn, int seasonsElapsed, List<ResolutionEvent> events) {
        if (seasonsUntilReview(seasonsElapsed) < MIN_SEASONS_UNTIL_REVIEW) {
            return false;
        }
        RandomEventRoster.EventSpec spec = roster.spec(RandomEventKind.REDUCED_EXPENDITURES);
        double factor = drawFraction(random, spec.min(), spec.max());
        int seasons = drawInt(random, spec.durationMin(), spec.durationMax());
        TimedEffects.addCostMultiplier(snapshot, factor, seasons);
        occurred(snapshot, events, RandomEventKind.REDUCED_EXPENDITURES, null, factor, turn);
        return true;
    }

    /**
     * Extends one random trainee's basic training by one season.
     */
    private static boolean extendBasicTraining(PlayerSnapshot snapshot, RandomGenerator random, int turn,
            List<ResolutionEvent> events) {
        List<Personnel> trainees = basicTrainees(snapshot);
        if (trainees.isEmpty() || firstSeason(snapshot)) {
            return false;
        }
        Personnel trainee = trainees.get(random.nextInt(trainees.size()));
        trainee.setTrainingSeasonsTotal(trainee.getTrainingSeasonsTotal() + 1);
        occurred(snapshot, events, RandomEventKind.BASIC_TRAINING_EXTENSION, trainee.getId(), 1, turn,
            trainee.getName());
        return true;
    }

    /**
     * Whether this is the agency's own first season (it starts at season 1, a late joiner too). Neither
     * basic-training event happens then, so the first turn's hires are ready on the second turn
     * ({@link TrainingProgress#FIRST_TURN_SET_BASIC_TRAINING_SEASONS}; user, 2026-10-23); the roll
     * passes to the pool's next event, as it does when nobody is in training.
     */
    static boolean firstSeason(PlayerSnapshot snapshot) {
        return snapshot.getSeason() == 1;
    }

    /**
     * Removes one random eligible employee from the first group, in a shuffled
     * order, that has one. Used for dismissal and resignation.
     */
    private static boolean departAnyEligible(PlayerSnapshot snapshot, RandomGenerator random,
            DepartureReason reason, RandomEventKind kind, int turn, List<ResolutionEvent> events) {
        List<PersonnelGroup> groups = new ArrayList<>(List.of(PersonnelGroup.values()));
        shuffle(groups, random);
        for (PersonnelGroup group : groups) {
            List<Personnel> eligible = eligibleEmployees(snapshot, group);
            if (!eligible.isEmpty()) {
                Personnel chosen = eligible.get(random.nextInt(eligible.size()));
                depart(snapshot, group, chosen, reason, kind, turn, events);
                return true;
            }
        }
        return false;
    }

    /**
     * Removes one random trainee who failed basic training.
     */
    private static boolean failBasicTraining(PlayerSnapshot snapshot, RandomGenerator random, int turn,
            List<ResolutionEvent> events) {
        List<Personnel> trainees = basicTrainees(snapshot);
        if (trainees.isEmpty() || firstSeason(snapshot)) {
            return false;
        }
        Personnel trainee = trainees.get(random.nextInt(trainees.size()));
        depart(snapshot, groupOf(snapshot, trainee), trainee, DepartureReason.FAILED_BASIC_TRAINING,
            RandomEventKind.EMPLOYEE_FAILED_BASIC_TRAINING, turn, events);
        return true;
    }

    /**
     * One idle astronaut dies. BASPM takes an available astronaut at random.
     */
    private static boolean perishAstronaut(PlayerSnapshot snapshot, RandomGenerator random, int turn,
            List<ResolutionEvent> events) {
        List<Personnel> idle = eligibleEmployees(snapshot, PersonnelGroup.ASTRONAUT);
        if (idle.isEmpty()) {
            return false;
        }
        Personnel astronaut = idle.get(random.nextInt(idle.size()));
        snapshot.getMemorial().add(Careers.memorial((de.mkoehler.launchwindow.model.Astronaut) astronaut, turn,
            MemorialEntry.Cause.ACCIDENT, null, null));
        depart(snapshot, PersonnelGroup.ASTRONAUT, astronaut, DepartureReason.PERISHED,
            RandomEventKind.PERISHED_ASTRONAUT, turn, events);
        return true;
    }

    /**
     * Shifts one random group's morale by a rolled amount, clamped to 0-100. A drop
     * needs a failure this season; a boost needs a failure-free flight within
     * {@link #RECENT_SEASONS} seasons. Both use BASPM's own gates.
     *
     * @param sign {@code -1} for a drop, {@code +1} for a boost
     */
    private static boolean shiftMorale(RandomEventKind kind, int sign, PlayerSnapshot snapshot,
            RandomEventRoster roster, RandomGenerator random, int turn, List<ResolutionEvent> events) {
        boolean gate = sign < 0
            ? snapshot.getLastFailureTurn() == turn
            : recentFlight(snapshot, turn);
        if (!gate) {
            return false;
        }
        RandomEventRoster.EventSpec spec = roster.spec(kind);
        PersonnelGroup group = PersonnelGroup.values()[random.nextInt(PersonnelGroup.values().length)];
        int amount = drawInt(random, spec.min(), spec.max());
        for (Personnel person : groupRoster(snapshot, group)) {
            person.setMorale(clamp(person.getMorale() + sign * amount, 0, 100));
        }
        occurred(snapshot, events, kind, group.name(), amount, turn);
        return true;
    }

    /**
     * Shifts prestige by a rolled share of the current total, floored. Needs prestige above zero.
     *
     * @param sign {@code -1} for a drop, {@code +1} for a boost
     */
    private static boolean shiftPrestige(RandomEventKind kind, int sign, PlayerSnapshot snapshot,
            RandomEventRoster roster, RandomGenerator random, int turn, List<ResolutionEvent> events) {
        if (snapshot.getPrestige() <= 0) {
            return false;
        }
        RandomEventRoster.EventSpec spec = roster.spec(kind);
        double percent = drawFraction(random, spec.min(), spec.max());
        long amount = (long) Math.floor(snapshot.getPrestige() * percent / 100.0);
        snapshot.setPrestige(Math.max(0, snapshot.getPrestige() + sign * amount));
        occurred(snapshot, events, kind, null, percent, turn);
        return true;
    }

    /**
     * Takes a rolled share of current funds, floored. Needs positive funds: in debt,
     * there is no cash to take ({@code design.md} &sect;2.7, a Launch Window choice).
     */
    private static boolean unexpectedCost(PlayerSnapshot snapshot, RandomEventRoster roster, RandomGenerator random,
            int turn, List<ResolutionEvent> events) {
        if (snapshot.getFunds() <= 0) {
            return false;
        }
        RandomEventRoster.EventSpec spec = roster.spec(RandomEventKind.UNEXPECTED_COST);
        double percent = drawFraction(random, spec.min(), spec.max());
        long cost = (long) Math.floor(snapshot.getFunds() * percent / 100.0);
        snapshot.setFunds(snapshot.getFunds() - cost);
        occurred(snapshot, events, RandomEventKind.UNEXPECTED_COST, null, percent, turn);
        return true;
    }

    /**
     * Cuts reliability on one to all of the components currently in R&amp;D, each by a
     * rolled number of points, floored at the component's own transfer floor
     * (BASPM's {@code ModifyCurrReliability} clamp, {@code baspm.md} &sect;12.1).
     * Needs at least one component in R&amp;D.
     */
    private static boolean reliabilitySetback(PlayerSnapshot snapshot, RandomEventRoster roster,
            RandomGenerator random, int turn, List<ResolutionEvent> events) {
        List<String> inResearch = componentsInResearch(snapshot);
        if (inResearch.isEmpty()) {
            return false;
        }
        shuffle(inResearch, random);
        int affected = 1 + random.nextInt(inResearch.size());
        RandomEventRoster.EventSpec spec = roster.spec(RandomEventKind.RELIABILITY_SETBACK);
        for (String componentId : inResearch.subList(0, affected)) {
            OwnedMissionComponent owned = snapshot.getMissionComponents().get(componentId);
            double cut = drawFraction(random, spec.min(), spec.max());
            double before = owned.getCurrentReliability();
            owned.setCurrentReliability(Math.max(owned.getFloorReliability(), before - cut));
            occurred(snapshot, events, RandomEventKind.RELIABILITY_SETBACK, componentId,
                before - owned.getCurrentReliability(), turn);
        }
        return true;
    }

    /**
     * Raises the current funding rate by a rolled share, until the next review
     * resets it. Needs a failure-free flight within {@link #RECENT_SEASONS} seasons
     * and at least {@link #MIN_SEASONS_UNTIL_REVIEW} seasons until the next review
     * ({@code baspm.md} &sect;12.1).
     */
    private static boolean extraFunding(PlayerSnapshot snapshot, RandomEventRoster roster, RandomGenerator random,
            int turn, int seasonsElapsed, List<ResolutionEvent> events) {
        if (!recentFlight(snapshot, turn) || seasonsUntilReview(seasonsElapsed) < MIN_SEASONS_UNTIL_REVIEW) {
            return false;
        }
        RandomEventRoster.EventSpec spec = roster.spec(RandomEventKind.EXTRA_FUNDING);
        double percent = drawFraction(random, spec.min(), spec.max());
        long raised = (long) Math.floor(snapshot.getFundingRatePerSeason() * percent / 100.0);
        snapshot.setFundingRatePerSeason(snapshot.getFundingRatePerSeason() + raised);
        occurred(snapshot, events, RandomEventKind.EXTRA_FUNDING, null, percent, turn);
        return true;
    }

    /**
     * Whether a failure-free flight season happened within the last {@link #RECENT_SEASONS}
     * periods, counting this one - BASPM's own window ({@code baspm.md} &sect;12.1).
     */
    private static boolean recentFlight(PlayerSnapshot snapshot, int turn) {
        int last = snapshot.getLastFlightSeasonTurn();
        return last >= 0 && turn - last < RECENT_SEASONS;
    }

    /**
     * Seasons from this season to the next budget review, as BASPM counts them.
     *
     * @param seasonsElapsed the seasons-elapsed count including this season
     * @return a value from 1 to {@link BudgetProgress#SEASONS_PER_REVIEW}
     */
    static int seasonsUntilReview(int seasonsElapsed) {
        return SEASONS_PER_REVIEW - (seasonsElapsed % SEASONS_PER_REVIEW);
    }

    /**
     * Removes one employee from the roster and reports the departure, with the same
     * bookkeeping {@link PersonnelLifecycle} uses, and records the event.
     */
    private static void depart(PlayerSnapshot snapshot, PersonnelGroup group, Personnel person,
            DepartureReason reason, RandomEventKind kind, int turn, List<ResolutionEvent> events) {
        switch (group) {
            case ASTRONAUT -> removeAndReport(snapshot.getAstronauts(), (Astronaut) person, group, reason, events);
            case SET -> removeAndReport(snapshot.getSetSpecialists(), (SetSpecialist) person, group, reason, events);
            case MISSION_CONTROL -> removeAndReport(snapshot.getMissionControlOfficers(),
                (MissionControlOfficer) person, group, reason, events);
        }
        occurred(snapshot, events, kind, person.getId(), 1, turn, person.getName());
    }

    private static <T extends Personnel> void removeAndReport(List<T> roster, T person, PersonnelGroup group,
            DepartureReason reason, List<ResolutionEvent> events) {
        events.add(new PersonnelDeparture(group, person.getId(), person.getName(), reason, person.getAge(),
            person.getMorale(), person.getAssignmentState(),
            PersonnelLifecycle.rdTargetComponentIdAtExit(person, group)));
        roster.remove(person);
    }

    /**
     * Records one applied event in the season's event list and the player's news log.
     */
    private static void occurred(PlayerSnapshot snapshot, List<ResolutionEvent> events, RandomEventKind kind,
            String subjectId, double magnitude, int turn) {
        occurred(snapshot, events, kind, subjectId, magnitude, turn, null);
    }

    /**
     * Records one applied event with the person it struck, and picks its story (2026-10-10): one the
     * player hasn't seen, from its own salted random so the season's draws stay as they were.
     */
    private static void occurred(PlayerSnapshot snapshot, List<ResolutionEvent> events, RandomEventKind kind,
            String subjectId, double magnitude, int turn, String subjectName) {
        SplittableRandom storyRandom = new SplittableRandom(Intelligence.seed(snapshot.getAgencyName(),
            "event-story/" + turn + "/" + kind.name()) ^ STORY_SALT);
        String storyId = EventStories.standard().pick(kind, storyRandom, snapshot.getUsedEventStoryIds(),
            story -> story.requiresGoal() == null || goalReached(snapshot, story.requiresGoal()));
        events.add(new RandomEventOccurred(kind, subjectId, magnitude, turn, storyId, subjectName));
        snapshot.getNewsLog().add(new NewsEntry(turn, kind, subjectId, magnitude, storyId, subjectName));
    }

    /** Whether the agency has reached a goal, itself or credited through the cascade, as {@link WinCondition} counts. */
    private static boolean goalReached(PlayerSnapshot snapshot, String goalId) {
        return snapshot.getAchievedGoals().containsKey(goalId) || snapshot.getCancelledGoalIds().contains(goalId);
    }

    /** Keeps the story pick's seed apart from every other salted seed. */
    private static final long STORY_SALT = 0x5703_1E5L;

    /** Employees of one group who may be removed by a random event: idle, or in training for the non-astronaut groups. */
    private static List<Personnel> eligibleEmployees(PlayerSnapshot snapshot, PersonnelGroup group) {
        List<Personnel> eligible = new ArrayList<>();
        for (Personnel person : groupRoster(snapshot, group)) {
            AssignmentState state = person.getAssignmentState();
            boolean idle = state == AssignmentState.IDLE;
            boolean training = state == AssignmentState.IN_TRAINING;
            if (group == PersonnelGroup.ASTRONAUT ? idle : idle || training) {
                eligible.add(person);
            }
        }
        return eligible;
    }

    private static List<Personnel> basicTrainees(PlayerSnapshot snapshot) {
        List<Personnel> trainees = new ArrayList<>();
        for (PersonnelGroup group : PersonnelGroup.values()) {
            for (Personnel person : groupRoster(snapshot, group)) {
                if (person.getAssignmentState() == AssignmentState.IN_TRAINING
                        && person.getTrainingKind() == TrainingKind.BASIC) {
                    trainees.add(person);
                }
            }
        }
        return trainees;
    }

    private static PersonnelGroup groupOf(PlayerSnapshot snapshot, Personnel person) {
        for (PersonnelGroup group : PersonnelGroup.values()) {
            if (groupRoster(snapshot, group).contains(person)) {
                return group;
            }
        }
        throw new IllegalStateException("personnel not on any roster: " + person.getId());
    }

    static List<Personnel> groupRoster(PlayerSnapshot snapshot, PersonnelGroup group) {
        return switch (group) {
            case ASTRONAUT -> new ArrayList<>(snapshot.getAstronauts());
            case SET -> new ArrayList<>(snapshot.getSetSpecialists());
            case MISSION_CONTROL -> new ArrayList<>(snapshot.getMissionControlOfficers());
        };
    }

    /** Component ids currently being researched by a SET specialist, in roster order, without repeats. */
    static List<String> componentsInResearch(PlayerSnapshot snapshot) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (SetSpecialist specialist : snapshot.getSetSpecialists()) {
            if (specialist.getAssignmentState() == AssignmentState.ASSIGNED
                    && specialist.getRdTargetComponentId() != null) {
                ids.add(specialist.getRdTargetComponentId());
            }
        }
        return new ArrayList<>(ids);
    }

    /** Draws a fraction uniformly from {@code [min, max)}. */
    private static double drawFraction(RandomGenerator random, double min, double max) {
        return min + random.nextDouble() * (max - min);
    }

    /** Draws an integer from {@code [min, max)}, BASPM's own integer-range convention. */
    private static int drawInt(RandomGenerator random, double min, double max) {
        int low = (int) min;
        int high = (int) max;
        return low + random.nextInt(high - low);
    }

    private static int clamp(int value, int low, int high) {
        return Math.max(low, Math.min(high, value));
    }

    private static <T> void shuffle(List<T> items, RandomGenerator random) {
        for (int i = items.size() - 1; i > 0; i--) {
            Collections.swap(items, i, random.nextInt(i + 1));
        }
    }
}
