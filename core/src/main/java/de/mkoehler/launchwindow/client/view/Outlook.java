package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.content.BudgetSchedule;
import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.content.MissionSeason;
import de.mkoehler.launchwindow.content.MissionStep;
import de.mkoehler.launchwindow.content.Program;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.GameDate;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.Personnel;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.TrainingKind;
import de.mkoehler.launchwindow.rules.BudgetProgress;
import de.mkoehler.launchwindow.rules.GoalCascade;
import de.mkoehler.launchwindow.rules.MissionLaunch;
import de.mkoehler.launchwindow.rules.PersonnelGroup;
import de.mkoehler.launchwindow.rules.PersonnelLifecycle;
import de.mkoehler.launchwindow.rules.ResearchPlanning;
import de.mkoehler.launchwindow.rules.TrainingProgress;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.Set;

/**
 * Looking ahead (the user's request, 2026-10-10): what a player can plan around. R&amp;D milestones
 * with the seasons to each, what a mission still waits on and the earliest season it could fly, a
 * program's staffing against the roster, and which missions earn a goal a locked program needs.
 * Every season count comes from the rules ({@link ResearchPlanning}, the training rules); this class
 * only reads and words them.
 *
 * @author Mario Koehler
 */
public final class Outlook {

    /** The milestones an R&amp;D bar marks below the cap; the first is the advisor's "ready" mark. */
    public static final double[] MILESTONES = {Advisor.READY_RELIABILITY, 70, 80};

    /** A milestone this close below the cap is left out: the cap's own marker says it. */
    static final double MERGE_GAP = 3;

    private Outlook() {
    }

    // ---- R&D milestones

    /**
     * A marker on an R&amp;D bar.
     *
     * @param value   the reliability it marks
     * @param cap     whether it's the R&amp;D cap
     * @param seasons the average seasons to get there with this team, empty if it doesn't
     */
    public record Marker(double value, boolean cap, OptionalInt seasons) {
    }

    /**
     * Returns the markers still ahead on a component's bar: the milestones above its reliability
     * and clear of the cap, then the cap itself.
     *
     * @param projection the component's projection
     * @return the markers, lowest first; empty once the cap is reached
     */
    public static List<Marker> markers(ResearchPlanning.Projection projection) {
        List<Marker> markers = new ArrayList<>();
        if (projection.start() >= projection.cap() - 1e-6) {
            return markers;
        }
        for (double milestone : MILESTONES) {
            if (milestone > projection.start() && milestone <= projection.cap() - MERGE_GAP) {
                markers.add(new Marker(milestone, false, projection.seasonsTo(milestone)));
            }
        }
        markers.add(new Marker(projection.cap(), true, projection.seasonsTo(projection.cap())));
        return markers;
    }

    /**
     * Words a season count for a marker: "~3", or "—" when it isn't reached.
     *
     * @param seasons the seasons
     * @return the text
     */
    public static String approx(OptionalInt seasons) {
        return seasons.isPresent() ? "~" + seasons.getAsInt() : "—";
    }

    /**
     * Words a number of seasons.
     *
     * @param seasons the seasons
     * @return "1 season" or "3 seasons"
     */
    public static String seasons(int seasons) {
        return seasons + (seasons == 1 ? " season" : " seasons");
    }

    /**
     * Names the season something done after a number of resolutions is there for: the open turn
     * plus that many.
     *
     * @param openTurn the game's open turn
     * @param seasons  the resolutions
     * @return the season, such as "Fall 1956"
     */
    public static String by(long openTurn, int seasons) {
        return GameDate.ofTurn(openTurn + seasons).label();
    }

    /**
     * Words a range of seasons from a projection's random runs, such as "3 to 5 seasons".
     *
     * @param range the range
     * @return the words
     */
    public static String range(ResearchPlanning.Range range) {
        if (range.high() == Integer.MAX_VALUE) {
            return range.low() == Integer.MAX_VALUE ? "not within ten years" : range.low() + " seasons or more";
        }
        return range.low() == range.high() ? seasons(range.low()) : range.low() + " to " + seasons(range.high());
    }

    /**
     * Returns the line warning that someone on an R&amp;D team retires before the cap, or
     * {@code null}.
     *
     * @param projection the projection
     * @return the line, such as "Hartmann retires in 2 seasons, before the cap."
     */
    public static String departureLine(ResearchPlanning.Projection projection) {
        OptionalInt toCap = projection.seasonsTo(projection.cap());
        List<ResearchPlanning.Departure> leaving = projection.departures().stream()
            .filter(departure -> toCap.isEmpty() || departure.season() <= toCap.getAsInt())
            .toList();
        if (leaving.isEmpty() || projection.start() >= projection.cap() - 1e-6) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        for (ResearchPlanning.Departure departure : leaving) {
            parts.add(People.surname(departure.engineer().getName()) + " retires in "
                + seasons(departure.season()));
        }
        return String.join("; ", parts) + ", before the cap. The projection counts the team without "
            + (leaving.size() == 1 ? "them" : "those leaving") + " from then on.";
    }

    // ---- People: who could fly when

    /**
     * Returns how many seasons until each person in a roster could be on a mission, for those whose
     * wait is known, soonest first: idle now 0; in training that can be cut short 0; basic training
     * its seasons left; unavailable its seasons. Anyone on a mission, PR duty or set otherwise is
     * left out.
     *
     * @param roster the roster
     * @return the waits
     */
    static List<Integer> waits(List<? extends Personnel> roster) {
        List<Integer> waits = new ArrayList<>();
        for (Personnel person : roster) {
            AssignmentState state = person.getAssignmentState();
            if (state == AssignmentState.IDLE) {
                waits.add(Math.max(0, person.getUnavailableSeasons()));
            } else if (state == AssignmentState.IN_TRAINING && person.getTrainingKind() == TrainingKind.BASIC) {
                waits.add(Math.max(1, person.getTrainingSeasonsTotal() - person.getTrainingSeasonsCompleted()));
            } else if (state == AssignmentState.IN_TRAINING) {
                waits.add(0);
            }
        }
        waits.sort(Comparator.naturalOrder());
        return waits;
    }

    /**
     * Returns how long the seasons until training ends are, as a date: "ready by Fall 1956".
     *
     * @param person   someone in training with a fixed length
     * @param openTurn the open turn
     * @return the words, or {@code null} for training with no fixed end
     */
    public static String trainingReady(Personnel person, long openTurn) {
        TrainingKind kind = person.getTrainingKind();
        if (person.getAssignmentState() != AssignmentState.IN_TRAINING || kind == TrainingKind.EXPERT
                || kind == TrainingKind.PERMANENT || kind == TrainingKind.NONE || kind == null) {
            return null;
        }
        int left = Math.max(1, person.getTrainingSeasonsTotal() - person.getTrainingSeasonsCompleted());
        return "ready by " + by(openTurn, left);
    }

    /** Retirement this close, in seasons, gets a note on the roster: a year. */
    public static final int RETIREMENT_NOTICE_SEASONS = 4;

    /**
     * Returns a roster row's look-ahead note: when training ends ("Ready by Fall 1956"), and
     * retirement within a year ("Retires in 3 seasons"), or {@code null} for neither.
     *
     * @param person   the employee
     * @param group    their group
     * @param openTurn the open turn
     * @return the note
     */
    public static String rosterNote(Personnel person, PersonnelGroup group, long openTurn) {
        List<String> parts = new ArrayList<>();
        String ready = trainingReady(person, openTurn);
        if (ready != null) {
            parts.add(Character.toUpperCase(ready.charAt(0)) + ready.substring(1));
        }
        int retires = PersonnelLifecycle.seasonsUntilRetirement(person, group);
        if (retires <= RETIREMENT_NOTICE_SEASONS) {
            parts.add((parts.isEmpty() ? "Retires" : "retires") + " in " + seasons(retires)
                + (group == PersonnelGroup.SET ? "" : " (or when a flight ends)"));
        }
        return parts.isEmpty() ? null : String.join("; ", parts);
    }

    // ---- A program's staffing against the roster

    /** How a program's staffing compares with the roster. */
    public enum Supply {
        /** Enough are free now for its smallest mission. */
        READY,
        /** Enough are on the roster, but some are training or otherwise busy. */
        COMING,
        /** The roster is too small: hire first. */
        SHORT
    }

    /**
     * One group's need across a program's missions, against the roster.
     *
     * @param group  "astronauts" (crew and CAPCOM) or "controllers"
     * @param min    the smallest mission's need, 0 when some fly without this group
     * @param least  the smallest need of the missions that need this group at all
     * @param max    the largest mission's need
     * @param free   free now
     * @param roster on the roster
     * @param supply how that compares with {@code least}
     */
    public record Need(String group, int min, int least, int max, int free, int roster, Supply supply) {

        /**
         * Returns the range as the card shows it: "2" or "2–4".
         *
         * @return the text
         */
        public String range() {
            return min == max ? String.valueOf(min) : min + "–" + max;
        }
    }

    /**
     * Returns a program's staffing needs, astronauts then controllers, leaving out a group none of
     * its missions needs. Counted as {@link Staffing} counts a single mission.
     *
     * @param program the program
     * @param state   the agency
     * @param catalog the content pack
     * @return the needs
     */
    public static List<Need> needs(Program program, PlayerSnapshot state, ContentCatalog catalog) {
        List<MissionConfiguration> configurations = Missions.of(program, catalog);
        List<Need> needs = new ArrayList<>();
        int[] astronauts = range(configurations, true);
        if (astronauts[2] > 0) {
            needs.add(need("astronauts", astronauts, state.getAstronauts()));
        }
        int[] controllers = range(configurations, false);
        if (controllers[2] > 0) {
            needs.add(need("controllers", controllers, state.getMissionControlOfficers()));
        }
        return needs;
    }

    /** The smallest need, the smallest need above zero, and the largest. */
    private static int[] range(List<MissionConfiguration> configurations, boolean astronauts) {
        int min = Integer.MAX_VALUE;
        int least = Integer.MAX_VALUE;
        int max = 0;
        for (MissionConfiguration configuration : configurations) {
            int count = astronauts ? Missions.crewCount(configuration) + Missions.capcomCount(configuration)
                : Missions.controllerCount(configuration);
            min = Math.min(min, count);
            if (count > 0) {
                least = Math.min(least, count);
            }
            max = Math.max(max, count);
        }
        return new int[] {configurations.isEmpty() ? 0 : min, max == 0 ? 0 : least, max};
    }

    private static Need need(String group, int[] range, List<? extends Personnel> roster) {
        int free = (int) roster.stream().filter(MissionLaunch::isAvailable).count();
        int least = range[1];
        Supply supply = free >= least ? Supply.READY : roster.size() >= least ? Supply.COMING : Supply.SHORT;
        return new Need(group, range[0], least, range[2], free, roster.size(), supply);
    }

    // ---- A mission's readiness

    /** Where one check stands. */
    public enum Status {
        /** Ready now. */
        READY,
        /** Ready after a known number of seasons. */
        WAITING,
        /** Not ready, and no wait can be told: something has to change first. */
        BLOCKED
    }

    /**
     * One thing a mission needs.
     *
     * @param label  what it is, such as "Rocket"
     * @param status where it stands
     * @param seasons the seasons until it's ready, 0 when ready, -1 when blocked
     * @param detail a sentence on it
     */
    public record Check(String label, Status status, int seasons, String detail) {
    }

    /**
     * Everything a mission needs, and the earliest season it could fly.
     *
     * @param checks   the checks, in the card's order
     * @param earliest the seasons until every check is ready, empty if one is blocked
     * @param limiter  the check that sets the earliest season or blocks it, {@code null} when ready now
     */
    public record Readiness(List<Check> checks, OptionalInt earliest, Check limiter) {

        /**
         * Returns whether the mission could fly now.
         *
         * @return whether it could
         */
        public boolean readyNow() {
            return earliest.isPresent() && checks.stream().allMatch(check -> check.status() == Status.READY);
        }
    }

    /**
     * Checks a mission configuration against the agency: its goals, its payload and a rocket that
     * can lift it reaching {@link Advisor#READY_RELIABILITY} (as the R&amp;D projection expects), its
     * astronauts and controllers (as the training rules release them), and the funds for the
     * mission and the cheapest rocket that fits.
     *
     * @param configuration the configuration
     * @param state         the agency
     * @param catalog       the content pack
     * @return the readiness
     */
    public static Readiness of(MissionConfiguration configuration, PlayerSnapshot state, ContentCatalog catalog) {
        List<Check> checks = new ArrayList<>();
        List<String> missing = Missions.missingGoals(configuration, state);
        if (!missing.isEmpty()) {
            checks.add(new Check("Goals", Status.BLOCKED, -1, "Needs " + String.join(", ",
                missing.stream().map(ContentNames::of).toList()) + " first."));
        }
        checks.add(payloadCheck(configuration, state, catalog));
        long rocketCost = 0;
        boolean anyRocket = false;
        Check rocket = null;
        for (MissionConfiguration.Launch launch : configuration.launches()) {
            if (!launch.launchType().needsRocket()) {
                continue;
            }
            anyRocket = true;
            RocketPick pick = rocketCheck(launch, state, catalog);
            rocketCost += pick.unitCost();
            if (rocket == null || worse(pick.check(), rocket)) {
                rocket = pick.check();
            }
        }
        if (anyRocket) {
            checks.add(rocket);
        }
        int astronauts = Missions.crewCount(configuration) + Missions.capcomCount(configuration);
        if (astronauts > 0) {
            checks.add(peopleCheck("Astronauts", "astronauts", astronauts, state.getAstronauts(),
                TrainingProgress.ASTRONAUT_BASIC_TRAINING_SEASONS));
        }
        int controllers = Missions.controllerCount(configuration);
        if (controllers > 0) {
            checks.add(peopleCheck("Controllers", "flight controllers", controllers,
                state.getMissionControlOfficers(), TrainingProgress.MISSION_CONTROL_BASIC_TRAINING_SEASONS));
        }
        long cost = configuration.cost() + rocketCost;
        if (state.getFunds() >= cost) {
            checks.add(new Check("Funds", Status.READY, 0, "The launch costs " + Money.of(cost) + "."));
        } else {
            checks.add(new Check("Funds", Status.BLOCKED, -1, "The launch costs " + Money.of(cost) + ", "
                + Money.of(cost - state.getFunds()) + " more than you have."));
        }
        Check limiter = null;
        int earliest = 0;
        for (Check check : checks) {
            if (check.status() == Status.BLOCKED) {
                return new Readiness(checks, OptionalInt.empty(), check);
            }
            if (check.seasons() > earliest || limiter == null && check.status() == Status.WAITING) {
                earliest = check.seasons();
                limiter = check;
            }
        }
        return new Readiness(checks, OptionalInt.of(earliest), limiter);
    }

    private static boolean worse(Check a, Check b) {
        if (a.status() == Status.BLOCKED || b.status() == Status.BLOCKED) {
            return a.status() == Status.BLOCKED && b.status() != Status.BLOCKED;
        }
        return a.seasons() > b.seasons();
    }

    private static Check payloadCheck(MissionConfiguration configuration, PlayerSnapshot state,
                                      ContentCatalog catalog) {
        Set<String> ids = new LinkedHashSet<>();
        for (MissionConfiguration.Launch launch : configuration.launches()) {
            ids.addAll(launch.payloadComponentIds());
        }
        Check worst = null;
        for (String id : ids) {
            Check check = reliabilityCheck("Payload", id, state, catalog);
            if (worst == null || worse(check, worst)) {
                worst = check;
            }
        }
        return worst;
    }

    private static Check reliabilityCheck(String label, String componentId, PlayerSnapshot state,
                                          ContentCatalog catalog) {
        String name = ContentNames.of(componentId);
        OwnedMissionComponent owned = state.getMissionComponents().get(componentId);
        double ready = Advisor.READY_RELIABILITY;
        if (owned == null || !owned.isActive()) {
            return new Check(label, Status.BLOCKED, -1, name + ": open its program first.");
        }
        if (owned.getCurrentReliability() >= ready) {
            return new Check(label, Status.READY, 0, String.format(Locale.ROOT, "%s is at %.0f %%.", name,
                owned.getCurrentReliability()));
        }
        ResearchPlanning.Projection projection = ResearchPlanning.project(state, catalog, componentId);
        OptionalInt seasons = projection.seasonsTo(ready);
        if (seasons.isPresent()) {
            return new Check(label, Status.WAITING, seasons.getAsInt(), String.format(Locale.ROOT,
                "%s reaches %.0f %% in about %s with its R&D team.", name, ready, seasons(seasons.getAsInt())));
        }
        String why = !projection.moving() ? "nobody works on its R&D"
            : projection.cap() < ready ? "its R&D cap is below that" : "its team won't get it there in ten years";
        return new Check(label, Status.BLOCKED, -1, String.format(Locale.ROOT, "%s is at %.0f %%, and %s.", name,
            owned.getCurrentReliability(), why));
    }

    private record RocketPick(Check check, long unitCost) {
    }

    /** The rocket that's ready soonest among those that can fly the launch, then the cheapest. */
    private static RocketPick rocketCheck(MissionConfiguration.Launch launch, PlayerSnapshot state,
                                          ContentCatalog catalog) {
        RocketPick best = null;
        for (Missions.RocketChoice choice : Missions.rocketChoices(launch, state, catalog)) {
            if (!choice.eligible()) {
                continue;
            }
            Check check = reliabilityCheck("Rocket", choice.rocket().id(), state, catalog);
            RocketPick pick = new RocketPick(check, choice.rocket().unitCost());
            if (best == null || worse(best.check(), check)
                    || !worse(check, best.check()) && check.seasons() == best.check().seasons()
                    && check.status() == best.check().status() && pick.unitCost() < best.unitCost()) {
                best = pick;
            }
        }
        if (best == null) {
            return new RocketPick(new Check("Rocket", Status.BLOCKED, -1, "None of your open rockets can fly "
                + Missions.launchTag(launch.launchType()).toLowerCase(Locale.ROOT) + " with this payload."), 0);
        }
        return best;
    }

    private static Check peopleCheck(String label, String group, int needed, List<? extends Personnel> roster,
                                     int basicSeasons) {
        int free = (int) roster.stream().filter(MissionLaunch::isAvailable).count();
        if (free >= needed) {
            return new Check(label, Status.READY, 0, needed + " " + group + " needed; " + free + " are free.");
        }
        List<Integer> waits = waits(roster);
        if (waits.size() >= needed) {
            int wait = waits.get(needed - 1);
            if (wait == 0) {
                // Ready only by pulling someone out of training: amber, with no seasons to wait.
                return new Check(label, Status.WAITING, 0, needed + " " + group + " needed; " + free + " are free, "
                    + "the rest only if you cut someone's training short.");
            }
            return new Check(label, Status.WAITING, wait, needed + " " + group + " needed; the last of them is free "
                + "in " + seasons(wait) + ".");
        }
        int gap = needed - waits.size();
        // On a mission or PR duty: back when that ends, which no count here can tell.
        int busy = roster.size() - waits.size();
        if (busy >= gap) {
            return new Check(label, Status.BLOCKED, -1, needed + " " + group + " needed; " + busy + " of yours "
                + (busy == 1 ? "is" : "are") + " on a mission or PR duty, free again when that ends.");
        }
        return new Check(label, Status.BLOCKED, -1, needed + " " + group + " needed; hire " + (gap - busy)
            + " more" + (busy > 0 ? " (" + busy + " more are on a mission or PR duty)" : "") + ". A new hire spends "
            + seasons(basicSeasons) + " in basic training.");
    }

    /**
     * Returns the readiness of a program's missions that are open to the agency (not locked on a
     * goal), soonest first.
     *
     * @param program the program
     * @param state   the agency
     * @param catalog the content pack
     * @return each configuration and its readiness
     */
    public static List<Planned> soonest(Program program, PlayerSnapshot state, ContentCatalog catalog) {
        List<Planned> planned = new ArrayList<>();
        for (MissionConfiguration configuration : Missions.of(program, catalog)) {
            if (Missions.missingGoals(configuration, state).isEmpty()) {
                planned.add(new Planned(configuration, of(configuration, state, catalog)));
            }
        }
        planned.sort(Comparator.comparingInt((Planned p) -> p.readiness().earliest().orElse(Integer.MAX_VALUE)));
        return planned;
    }

    /**
     * A configuration and its readiness.
     *
     * @param configuration the configuration
     * @param readiness     its readiness
     */
    public record Planned(MissionConfiguration configuration, Readiness readiness) {
    }

    // ---- Funds

    /**
     * Returns what opening a program adds to the running costs: its upkeep and that of each of its
     * components not active yet, under any cost multiplier.
     *
     * @param program the program
     * @param state   the agency
     * @param catalog the content pack
     * @return the extra per season
     */
    public static long openingUpkeep(Program program, PlayerSnapshot state, ContentCatalog catalog) {
        long upkeep = program.maintenanceCost();
        for (String id : program.requiredMissionComponentIds()) {
            OwnedMissionComponent owned = state.getMissionComponents().get(id);
            if (owned == null || !owned.isActive()) {
                upkeep += catalog.missionComponent(id).orElseThrow().maintenanceCost();
            }
        }
        return Math.round(upkeep * state.getCostMultiplier());
    }

    /**
     * Words how long the funds last at today's running costs plus an extra amount
     * ({@link BudgetProgress#runway}).
     *
     * @param state    the agency
     * @param catalog  the content pack
     * @param schedule the funding reviews
     * @param extra    extra costs per season, 0 for none
     * @param spendNow an amount paid before the first season, such as an opening price, 0 for none
     * @param openTurn the open turn
     * @return the sentence
     */
    public static String runwayLine(PlayerSnapshot state, ContentCatalog catalog, BudgetSchedule schedule,
                                    long extra, long spendNow, long openTurn) {
        if (state.getFunds() - spendNow < 0) {
            return "Funds are already below zero.";
        }
        OptionalInt runway = BudgetProgress.runway(state, catalog, schedule, extra, spendNow);
        if (runway.isEmpty()) {
            return "At today's costs and prestige, funds last at least ten years, the next funding review counted.";
        }
        int seasons = runway.getAsInt();
        return "At today's costs and prestige, funds run out in " + seasons(seasons) + ", in "
            + GameDate.ofTurn(openTurn + seasons - 1).label() + ", the next funding review counted.";
    }

    // ---- Goals a locked program needs

    /**
     * A program whose missions earn a goal.
     *
     * @param program        the program
     * @param state          its state for the agency
     * @param configurations its missions that earn the goal, directly or by crediting a bigger one
     */
    public record Source(Program program, Programs.State state, List<MissionConfiguration> configurations) {
    }

    /**
     * Returns the agency's programs whose missions earn a goal: a step achieving it, or a goal whose
     * cascade credits it ({@link GoalCascade}). Open programs first, then available, then locked, each
     * in the pack's order.
     *
     * @param goalId  the goal
     * @param state   the agency
     * @param catalog the content pack
     * @return the sources
     */
    public static List<Source> sources(String goalId, PlayerSnapshot state, ContentCatalog catalog) {
        List<Source> sources = new ArrayList<>();
        for (Program program : Programs.visible(state, catalog)) {
            List<MissionConfiguration> earning = new ArrayList<>();
            for (MissionConfiguration configuration : Missions.of(program, catalog)) {
                if (earns(configuration, goalId, catalog)) {
                    earning.add(configuration);
                }
            }
            if (!earning.isEmpty()) {
                sources.add(new Source(program, Programs.state(program, state), List.copyOf(earning)));
            }
        }
        sources.sort(Comparator.comparingInt(source -> switch (source.state()) {
            case OPEN -> 0;
            case AVAILABLE -> 1;
            case LOCKED -> 2;
        }));
        return sources;
    }

    private static boolean earns(MissionConfiguration configuration, String goalId, ContentCatalog catalog) {
        for (MissionSeason season : configuration.seasons()) {
            for (MissionStep step : season.steps()) {
                for (String achieved : step.goalIdsAchieved()) {
                    if (achieved.equals(goalId) || catalog.goal(achieved).isPresent()
                            && GoalCascade.unheld(achieved, goal -> false, catalog).contains(goalId)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Words where a goal can be earned, for a locked program: "Earned by Sputnik 2 (open) or
     * Vostok (available)", or that only locked programs earn it and what they need.
     *
     * @param goalId  the goal
     * @param state   the agency
     * @param catalog the content pack
     * @return the sentence
     */
    public static String sourceLine(String goalId, PlayerSnapshot state, ContentCatalog catalog) {
        List<Source> sources = sources(goalId, state, catalog);
        if (sources.isEmpty()) {
            return "None of your agency's programs earns it.";
        }
        List<String> reachable = new ArrayList<>();
        for (Source source : sources) {
            if (source.state() != Programs.State.LOCKED) {
                reachable.add(ContentNames.of(source.program().id()) + " ("
                    + (source.state() == Programs.State.OPEN ? "open" : "available") + ")");
            }
        }
        if (!reachable.isEmpty()) {
            if (reachable.size() > SOURCES_NAMED) {
                int more = reachable.size() - SOURCES_NAMED;
                return "Earned by " + String.join(", ", reachable.subList(0, SOURCES_NAMED)) + " and " + more
                    + (more == 1 ? " more program." : " more programs.");
            }
            return "Earned by " + join(reachable) + ".";
        }
        Source first = sources.get(0);
        return "Only locked programs earn it: " + ContentNames.of(first.program().id()) + " needs "
            + join(Programs.missingGoals(first.program(), state).stream().map(ContentNames::of).toList()) + " first.";
    }

    /** How many programs a goal's source line names before "and N more". */
    static final int SOURCES_NAMED = 3;

    private static String join(List<String> parts) {
        if (parts.size() <= 1) {
            return String.join("", parts);
        }
        return String.join(", ", parts.subList(0, parts.size() - 1)) + " or " + parts.get(parts.size() - 1);
    }

    /** Money as the screens write it ({@code ChromeScreen.money}). */
    static final class Money {
        private Money() {
        }

        static String of(long amount) {
            return amount < 0 ? String.format(Locale.ROOT, "− $ %,.1f", (double) -amount)
                : String.format(Locale.ROOT, "$ %,.1f", (double) amount);
        }
    }
}
