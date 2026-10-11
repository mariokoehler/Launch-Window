package de.mkoehler.launchwindow.devtools.analysis;

import de.mkoehler.launchwindow.rules.MissionStaffing;
import de.mkoehler.launchwindow.content.StaffingSlot;
import de.mkoehler.launchwindow.content.BudgetSchedule;
import de.mkoehler.launchwindow.content.BuildingSchedule;
import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.ContentPackLoader;
import de.mkoehler.launchwindow.content.LaunchType;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.content.MissionSeason;
import de.mkoehler.launchwindow.content.MissionStep;
import de.mkoehler.launchwindow.content.Program;
import de.mkoehler.launchwindow.content.RandomEventRoster;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.AstronautSkill;
import de.mkoehler.launchwindow.model.Building;
import de.mkoehler.launchwindow.model.InProgressMission;
import de.mkoehler.launchwindow.model.MissionControlOfficer;
import de.mkoehler.launchwindow.model.MissionControlSkill;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.Personnel;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSkill;
import de.mkoehler.launchwindow.model.SetSpecialist;
import de.mkoehler.launchwindow.model.TrainingKind;
import de.mkoehler.launchwindow.rules.BuildingUpgradeOutcome;
import de.mkoehler.launchwindow.rules.BuildingUpgrades;
import de.mkoehler.launchwindow.rules.GoalTally;
import de.mkoehler.launchwindow.rules.HireOutcome;
import de.mkoehler.launchwindow.rules.HiringPool;
import de.mkoehler.launchwindow.rules.LaunchRequest;
import de.mkoehler.launchwindow.rules.MissionLaunch;
import de.mkoehler.launchwindow.rules.MissionLaunchOutcome;
import de.mkoehler.launchwindow.rules.MissionStepResolved;
import de.mkoehler.launchwindow.rules.OpenProgramOutcome;
import de.mkoehler.launchwindow.rules.PersonnelDeparture;
import de.mkoehler.launchwindow.rules.PersonnelGroup;
import de.mkoehler.launchwindow.rules.PrestigeConversion;
import de.mkoehler.launchwindow.rules.ProgramOpening;
import de.mkoehler.launchwindow.rules.ResolutionEvent;
import de.mkoehler.launchwindow.rules.RocketProgramOutcome;
import de.mkoehler.launchwindow.rules.RocketPrograms;
import de.mkoehler.launchwindow.rules.SeasonFundsSettled;
import de.mkoehler.launchwindow.rules.SeasonResolver;
import de.mkoehler.launchwindow.rules.TrainingProgress;
import de.mkoehler.launchwindow.rules.WinCondition;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.ToDoubleFunction;

/**
 * <p>The M3 bot harness: whole games on a real content pack, run headlessly
 * through every season resolver in composition order, with a goal-directed bot
 * that plays toward a declared goal list. M3's exit criterion is that some games
 * reach the declared goals ({@code design.md} &sect;5, M3 slice 5).</p>
 *
 * <p>{@link PersonnelLifecyclePlayouts} stays as the resolvers' random stress test
 * on its own fixtures. This harness plays like a player instead: each agency
 * follows its own plan of configurations toward the declared goals, opening the
 * program and the cheapest rocket able to fly the next one, upgrading Mission
 * Control and hiring until it can be staffed, pointing R&amp;D at its components,
 * and launching once their reliability is high enough. Every action goes through
 * its validated rules method, and its charge is checked against an independent
 * recomputation, as are the season's running costs, the building caps, the
 * agency filter, and each flying mission's rockets and staffing.</p>
 *
 * <p>Run with
 * {@code mvn -pl dev-tools compile exec:java -DmainClass=de.mkoehler.launchwindow.devtools.analysis.ContentPackPlayouts}
 * from the repo root.</p>
 *
 * @author Mario Koehler
 */
public final class ContentPackPlayouts {

    /** The goals a game declares on the early-era pack, its race (the server's {@code GameMeta.EARLY_ERA_DECLARED_GOALS}). */
    public static final List<String> DECLARED_GOALS =
        List.of("earth-orbit-satellite", "human-in-space", "human-in-orbit");

    /** Each agency's plan, in order; the bot flies the first one with a goal still open. */
    static final Map<Archetype, List<String>> PLANS = Map.of(
        Archetype.SOVIET, List.of("sputnik-1-orbit", "vostok-uncrewed-suborbital", "vostok-suborbital",
            "vostok-uncrewed-orbital", "vostok-orbital"),
        Archetype.NASA, List.of("explorer-1-orbit", "mercury-uncrewed-suborbital", "mercury-suborbital",
            "mercury-uncrewed-orbital", "mercury-orbital"));

    /** The Moon race, the standard pack's (the server's {@code GameMeta.STANDARD_DECLARED_GOALS}, design.md 2.6). */
    public static final List<String> MOON_DECLARED_GOALS =
        List.of("earth-orbit-satellite", "human-in-orbit", "crewed-lunar-landing");

    /**
     * The plans to the Moon: the early race, one lunar probe (it gates the lunar programs), the lunar
     * craft's tests in Earth orbit, then out to the Moon step by step. Each flight clears reliability
     * penalties of the ones after it, as a careful player would fly them.
     */
    static final Map<Archetype, List<String>> MOON_PLANS = Map.of(
        Archetype.SOVIET, List.of("sputnik-1-orbit", "vostok-uncrewed-suborbital", "vostok-suborbital",
            "vostok-uncrewed-orbital", "vostok-orbital", "luna-2-impact",
            "soyuz-7k-l3-uncrewed-suborbital", "soyuz-7k-l3-uncrewed-orbital", "soyuz-7k-l3-orbital-duration-1",
            "soyuz-7k-l3-spacewalk", "soyuz-7k-l3-uncrewed-lander-test", "soyuz-7k-l3-lander-test",
            "soyuz-7k-l3-uncrewed-flyby", "soyuz-7k-l3-flyby", "soyuz-7k-l3-uncrewed-lunar-orbit",
            "soyuz-7k-l3-lunar-orbit", "soyuz-7k-l3-lander-test-lunar-orbit", "soyuz-7k-l3-landing"),
        Archetype.NASA, List.of("explorer-1-orbit", "mercury-uncrewed-suborbital", "mercury-suborbital",
            "mercury-uncrewed-orbital", "mercury-orbital", "ranger-3-impact",
            "apollo-uncrewed-suborbital", "apollo-uncrewed-orbital", "apollo-orbital-duration-1",
            "apollo-uncrewed-lunar-module-test", "apollo-lunar-module-test-low-orbit",
            "apollo-uncrewed-flyby", "apollo-flyby", "apollo-uncrewed-lunar-orbit", "apollo-lunar-orbit",
            "apollo-lander-test", "apollo-landing"));

    /** The bot launches once every component it flies is at least this reliable (or at its R&D ceiling). */
    static final double LAUNCH_RELIABILITY = 70;

    private static final int STARTING_ASTRONAUTS = 3;
    private static final int STARTING_CONTROLLERS = 5;
    private static final int STARTING_SPECIALISTS = 6;
    /** The working-ahead bot upgrades its SET Center only with more money than this in hand. */
    private static final long AHEAD_RESERVE = 10_000;
    /** The spare controllers and astronauts the working-ahead bot keeps. */
    private static final int AHEAD_SPARES = 2;
    /** How close to {@link #LAUNCH_RELIABILITY} the working-ahead bot stops sending its people to training. */
    private static final double CLOSE_TO_READY = 15;
    /** Seasons of running costs the side-missions bot keeps in hand on top of what its plan needs next. */
    private static final int SIDE_CUSHION_SEASONS = 4;
    /** Seasons the side-missions bot gives a side flight before it tries another. */
    private static final int SIDE_GIVE_UP = 16;
    /** What a point of reliability still to research weighs against a unit of money, choosing a side flight. */
    private static final double SIDE_RESEARCH_WEIGHT = 100;

    /** The side flight the bot is working on in the current game, its rocket, and since when. */
    private String sideId;
    private String sideRocketId;
    private int sideSince;
    private boolean sideFiller;
    private int seasonNow;
    private final Set<String> sideGivenUp = new HashSet<>();

    private final ContentCatalog catalog;
    private final BudgetSchedule schedule = BudgetSchedule.baspmClose();
    private final RandomEventRoster roster = RandomEventRoster.baspmClose();
    private final BuildingSchedule buildings;
    private final List<String> declaredGoals;
    private final Map<Archetype, List<String>> plans;
    private final Bot bot;
    private final boolean ahead;
    /**
     * With contracts (design.md 2.11): the rivals that apply for every listing the bot applies for, as
     * strong as the bot, so its rank among them is drawn at random; -1 plays without contracts.
     */
    private int contractRivals = -1;
    private int gameNumber;

    /** How the bot plays. */
    public enum Bot {
        /** One flight at a time, six SET specialists, nothing else. */
        PLAIN,
        /**
         * Works ahead as a player would: fills the SET Center (upgrading it with money to spare), puts
         * specialists the current flight can't use on the plan's later flights' components wherever
         * their program is open, keeps spare controllers and astronauts, and keeps its people home
         * when the hardware is close.
         */
        AHEAD,
        /**
         * Works ahead and flies side missions: one flight at a time off its plan, the best prestige for
         * the money and research it needs, from money above a reserve the plan needs next, while the
         * plan's own flight isn't close.
         */
        SIDE_MISSIONS
    }

    /**
     * Creates a harness for one content pack with the plain bot, which works on one flight at a time.
     * A pack that can fly to the Moon plays the Moon race, any other the early era's.
     *
     * @param catalog the pack
     */
    public ContentPackPlayouts(ContentCatalog catalog) {
        this(catalog, Bot.PLAIN);
    }

    /**
     * Creates a harness for one content pack.
     *
     * @param catalog the pack
     * @param bot     how the bot plays
     */
    public ContentPackPlayouts(ContentCatalog catalog, Bot bot) {
        this.bot = bot;
        this.ahead = bot != Bot.PLAIN;
        this.catalog = catalog;
        this.buildings = catalog.buildings();
        boolean moon = catalog.goal("crewed-lunar-landing").isPresent();
        this.declaredGoals = moon ? MOON_DECLARED_GOALS : DECLARED_GOALS;
        this.plans = moon ? MOON_PLANS : PLANS;
    }

    /**
     * Returns the goals this harness's games declare.
     *
     * @return the declared goals
     */
    public List<String> declaredGoals() {
        return declaredGoals;
    }

    /**
     * Plays with commercial contracts (design.md 2.11): the bot applies for a listing whenever it has no
     * contract, and flies an awarded one as soon as its hardware is ready. A playout has one agency, so
     * the market is simulated: every listing the bot applies for draws {@code rivals} rival applicants
     * as strong as the bot, and the bot's rank among them is random.
     *
     * @param rivals the rivals per listing; 3 makes a fourth of applications miss out
     * @return this harness
     */
    public ContentPackPlayouts withContracts(int rivals) {
        this.contractRivals = rivals;
        return this;
    }

    /**
     * The result of a batch of games.
     *
     * @param games                  games played
     * @param seasons                seasons per game
     * @param completedByArchetype   games that reached every declared goal, per agency
     * @param gamesByArchetype       games played, per agency
     * @param averageCompletionSeason the average season the last declared goal fell, over completed games
     * @param completionSeasons      the season the last declared goal fell, per completed game, per agency
     * @param goalReachedCounts      per declared goal, in how many games it was reached
     * @param launches               missions launched, per configuration
     * @param stepOutcomes           resolved mission steps, per outcome
     * @param programsOpened         programs opened
     * @param rocketsOpened          rocket programs opened
     * @param upgrades               building upgrades started
     * @param hires                  hires
     * @param debtConversions        prestige conversions while in debt
     * @param lowestFunds            the lowest funds any game reached
     * @param astronautsLost         astronauts lost in accidents or perished
     * @param stalls                 seasons the bot could not launch its target, per reason
     * @param targetSeasons          seasons each configuration was the bot's target, over all games
     * @param launchGaps             in completed games, the seasons without a launch before each launch
     * @param longestGaps            in completed games, each game's longest run of seasons without a launch
     * @param sideLaunches           side flights launched, per configuration, over all games
     * @param sideKinds              side flights launched, fillers and ventures
     * @param sideRejections         side flights passed over in a season, per kind and reason, over all games
     */
    public record Report(int games, int seasons, Map<Archetype, Integer> completedByArchetype,
                         Map<Archetype, Integer> gamesByArchetype, double averageCompletionSeason,
                         Map<Archetype, List<Integer>> completionSeasons,
                         Map<String, Integer> goalReachedCounts, Map<String, Integer> launches,
                         Map<String, Integer> stepOutcomes, int programsOpened, int rocketsOpened, int upgrades,
                         int hires, int debtConversions, long lowestFunds, int astronautsLost,
                         Map<String, Integer> stalls, Map<String, Integer> targetSeasons,
                         List<Integer> launchGaps, List<Integer> longestGaps,
                         Map<String, Integer> sideLaunches, Map<String, Integer> sideKinds,
                         Map<String, Integer> sideRejections) {
    }

    /**
     * Plays a batch of games, alternating NASA and Soviet players.
     *
     * @param games   how many games
     * @param seasons seasons per game
     * @param seed    the random seed
     * @return the report
     * @throws IllegalStateException if any invariant breaks
     */
    /**
     * What teamwork shows a player over a game (design.md 2.2(d)): traits revealed and relationships
     * shown per game, playing every season even after the declared goals are reached.
     *
     * @param games               how many games
     * @param traitsRevealed      traits revealed, over all games
     * @param relationshipsShown  relationships shown, over all games
     * @param positive            of those, how many were friendships
     * @param pairs               pairs who ever shared a team, over all games
     * @param employees           employees on the rosters at the end, over all games
     */
    public record PeopleReport(int games, int traitsRevealed, int relationshipsShown, int positive, int pairs,
                               int employees) {
    }

    /**
     * Plays games for a fixed number of seasons and counts what teamwork showed.
     *
     * @param games   how many games
     * @param seasons seasons per game
     * @param seed    the random seed
     * @return the counts
     */
    public PeopleReport people(int games, int seasons, long seed) {
        keepPlaying = true;
        try {
            run(games, seasons, seed);
        } finally {
            keepPlaying = false;
        }
        return new PeopleReport(games, peopleTally[0], peopleTally[1], peopleTally[2], peopleTally[3], peopleTally[4]);
    }

    private boolean keepPlaying;
    private final int[] peopleTally = new int[5];
    /** Ambitions said and settled, by "GROUP OUTCOME", in people mode (phase 4). */
    private final Map<String, Integer> ambitionTally = new java.util.TreeMap<>();
    /** At each game's end in people mode: morale and loyalty of every employee. */
    private final List<int[]> endOfGamePeople = new ArrayList<>();

    /**
     * Returns the ambitions said and settled in the last {@link #people} run, by group and outcome.
     *
     * @return the counts
     */
    public Map<String, Integer> ambitionTally() {
        return ambitionTally;
    }

    /**
     * Returns every employee's morale and loyalty at the end of each game of the last
     * {@link #people} run.
     *
     * @return pairs of morale and loyalty
     */
    public List<int[]> endOfGamePeople() {
        return endOfGamePeople;
    }

    /**
     * How big the player's state gets on the wire, per season: the largest and the average size in
     * bytes of the {@code FetchStateResponse} a client would receive (the stripped copy, Kryo, not
     * gzipped), which has to fit Kryo's 64 KB object buffer.
     *
     * @param games   the games played
     * @param largest the largest size seen at each season, index 0 for season 1
     * @param average the average size at each season
     */
    public record SizeReport(int games, int[] largest, double[] average) {
    }

    /**
     * Plays games to their end, measuring the state each season ({@link SizeReport}). Draws nothing
     * from the games' random stream, so the games play as in {@link #run}.
     *
     * @param games   how many games
     * @param seasons how many seasons each
     * @param seed    the seed
     * @return the sizes
     */
    public SizeReport sizes(int games, int seasons, long seed) {
        sizeLargest = new int[seasons];
        sizeSum = new long[seasons];
        sizeCount = new int[seasons];
        keepPlaying = true;
        try {
            run(games, seasons, seed);
        } finally {
            keepPlaying = false;
        }
        double[] average = new double[seasons];
        for (int i = 0; i < seasons; i++) {
            average[i] = sizeCount[i] == 0 ? 0 : sizeSum[i] / (double) sizeCount[i];
        }
        int[] largest = sizeLargest;
        sizeLargest = null;
        return new SizeReport(games, largest, average);
    }

    private int[] sizeLargest;
    private long[] sizeSum;
    private int[] sizeCount;
    private final com.fasterxml.jackson.databind.ObjectMapper sizeMapper =
        new com.fasterxml.jackson.databind.ObjectMapper();

    private void measure(PlayerSnapshot snapshot, int season) {
        com.esotericsoftware.kryo.Kryo kryo = new com.esotericsoftware.kryo.Kryo();
        de.mkoehler.launchwindow.net.MessageRegistry.register(kryo);
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        try (com.esotericsoftware.kryo.io.Output output = new com.esotericsoftware.kryo.io.Output(bytes)) {
            kryo.writeObject(output, de.mkoehler.launchwindow.net.messages.FetchStateResponse.success(
                de.mkoehler.launchwindow.rules.Personality.forClient(snapshot, sizeMapper)));
        }
        int size = bytes.size();
        sizeLargest[season] = Math.max(sizeLargest[season], size);
        sizeSum[season] += size;
        sizeCount[season]++;
    }

    public Report run(int games, int seasons, long seed) {
        Random random = new Random(seed);
        Tally tally = new Tally();
        lastTally = tally;
        for (int game = 0; game < games; game++) {
            Archetype archetype = game % 2 == 0 ? Archetype.NASA : Archetype.SOVIET;
            tally.gamesByArchetype.merge(archetype, 1, Integer::sum);
            playGame(archetype, seasons, random, tally);
        }
        double averageCompletion = tally.completionSeasons.stream().mapToInt(Integer::intValue).average().orElse(0);
        return new Report(games, seasons, tally.completedByArchetype, tally.gamesByArchetype, averageCompletion,
            tally.completionSeasonsByArchetype,
            tally.goalReached, tally.launches, tally.stepOutcomes, tally.programsOpened, tally.rocketsOpened,
            tally.upgrades, tally.hires, tally.debtConversions, tally.lowestFunds, tally.astronautsLost, tally.stalls,
            tally.targetSeasons, tally.launchGaps, tally.longestGaps, tally.sideLaunches,
            tally.sideKinds, tally.sideRejections);
    }

    private void playGame(Archetype archetype, int seasons, Random random, Tally tally) {
        PlayerSnapshot snapshot = new PlayerSnapshot("Bot " + archetype, archetype, schedule.initialFunds(),
            schedule.initialPrestige(), schedule.initialFundingRate(), 1);
        HiringPool.refreshCandidates(snapshot, random);
        hireBest(snapshot, PersonnelGroup.ASTRONAUT, STARTING_ASTRONAUTS, random, tally);
        hireBest(snapshot, PersonnelGroup.MISSION_CONTROL, STARTING_CONTROLLERS, random, tally);
        hireBest(snapshot, PersonnelGroup.SET, STARTING_SPECIALISTS, random, tally);
        GoalTally goalTally = new GoalTally();
        Set<String> reached = new HashSet<>();
        sideId = null;
        sideRocketId = null;
        sideGivenUp.clear();
        List<Integer> gaps = new ArrayList<>();
        int sinceLaunch = 0;
        // Random events seen this game, good then bad (2026-10-10, sizing the news stories).
        int[] randomEvents = new int[2];
        // R&D decisions this game (2026-10-10): per season, whether one was offered and whether it flew.
        List<boolean[]> decisionSeasons = new ArrayList<>();
        // The contract market's board and clients used, with contracts (design.md 2.11).
        List<de.mkoehler.launchwindow.model.ContractListing> board = new ArrayList<>();
        List<String> usedClients = new ArrayList<>();
        gameNumber++;

        for (int season = 0; season < seasons; season++) {
            // Journaled player actions first.
            tally.launchedThisSeason = 0;
            seasonNow = season;
            decideWaiting(snapshot, random);
            act(snapshot, random, tally);
            if (contractRivals >= 0) {
                actContracts(snapshot, tally);
            }
            if (tally.launchedThisSeason > 0) {
                gaps.add(sinceLaunch);
                sinceLaunch = 0;
            } else {
                sinceLaunch++;
            }
            checkState(snapshot);

            de.mkoehler.launchwindow.rules.Contracts.Turn contracts = de.mkoehler.launchwindow.rules.Contracts.Turn.NONE;
            if (contractRivals >= 0) {
                contracts = market(snapshot, board, usedClients, season);
                board = contracts.board();
            }
            // One player per game, so the game turn and the player's own seasons coincide here.
            SeasonResolver.Context context = new SeasonResolver.Context(catalog, schedule, roster, goalTally, season,
                season + 1, List.of(), contracts);
            BudgetWatch watch = new BudgetWatch();
            List<ResolutionEvent> seasonEvents = SeasonResolver.advanceSeason(snapshot, context, random, watch);
            goalTally.commit(seasonEvents);
            checkState(snapshot);
            if (sizeLargest != null) {
                measure(snapshot, season);
            }

            decisionSeasons.add(new boolean[] {seasonEvents.stream()
                .anyMatch(de.mkoehler.launchwindow.rules.DecisionOffered.class::isInstance), tally.launchedThisSeason > 0});
            for (ResolutionEvent event : seasonEvents) {
                if (event instanceof de.mkoehler.launchwindow.rules.RandomEventOccurred occurred) {
                    randomEvents[occurred.kind().isPositive() ? 0 : 1]++;
                    tally.randomEventKinds.merge(occurred.kind().name(), 1, Integer::sum);
                }
                if (event instanceof de.mkoehler.launchwindow.rules.ContractSettled settled) {
                    tally.contracts.merge(settled.kind().name(), 1, Integer::sum);
                    if (settled.kind() == de.mkoehler.launchwindow.rules.ContractSettled.Kind.PAID) {
                        tally.contractProfit += settled.fee() - settled.launchCost();
                    }
                }
                if (event instanceof MissionStepResolved resolved) {
                    tally.stepOutcomes.merge(resolved.outcome().name(), 1, Integer::sum);
                } else if (event instanceof de.mkoehler.launchwindow.rules.AmbitionSettled ambition && keepPlaying) {
                    ambitionTally.merge(ambition.group() + " " + ambition.outcome(), 1, Integer::sum);
                } else if (event instanceof de.mkoehler.launchwindow.rules.TraitRevealed) {
                    peopleTally[0]++;
                } else if (event instanceof de.mkoehler.launchwindow.rules.RelationshipShown shown) {
                    peopleTally[1]++;
                    if (shown.positive()) {
                        peopleTally[2]++;
                    }
                }
            }
            if (keepPlaying && season == seasons - 1) {
                peopleTally[3] += snapshot.getRapports().size();
                peopleTally[4] += snapshot.getAstronauts().size() + snapshot.getSetSpecialists().size()
                    + snapshot.getMissionControlOfficers().size();
                for (Personnel person : allEmployees(snapshot)) {
                    endOfGamePeople.add(new int[] {person.getMorale(), person.getLoyalty()});
                }
            }
            for (List<ResolutionEvent> list : List.of(seasonEvents)) {
                for (ResolutionEvent event : list) {
                    if (event instanceof PersonnelDeparture departure && departure.group() == PersonnelGroup.ASTRONAUT
                            && (departure.reason().name().equals("LOST_IN_ACCIDENT")
                                || departure.reason().name().equals("PERISHED"))) {
                        tally.astronautsLost++;
                    }
                }
            }
            tally.lowestFunds = Math.min(tally.lowestFunds, snapshot.getFunds());
            for (String goal : declaredGoals) {
                if (snapshot.hasAchievedOrCancelledGoal(goal) && reached.add(goal)) {
                    tally.goalReached.merge(goal, 1, Integer::sum);
                }
            }
            // The harness's own completion check must agree with the win logic M4 will call.
            WinCondition.Outcome win = WinCondition.evaluate(Map.of("bot", snapshot), declaredGoals, catalog);
            boolean complete = reached.size() == declaredGoals.size();
            if (complete != (win.status() == WinCondition.Status.WON && win.winners().equals(List.of("bot")))) {
                throw new IllegalStateException("Completion " + complete + " disagrees with WinCondition " + win);
            }
            if (complete && !keepPlaying) {
                tally.completedByArchetype.merge(archetype, 1, Integer::sum);
                tally.completionSeasons.add(season + 1);
                tally.completionSeasonsByArchetype.computeIfAbsent(archetype, key -> new ArrayList<>()).add(season + 1);
                tally.launchGaps.addAll(gaps);
                tally.longestGaps.add(gaps.stream().mapToInt(Integer::intValue).max().orElse(season + 1));
                tally.randomEventsPerGame.add(new int[] {randomEvents[0], randomEvents[1], season + 1});
                tally.decisionSeasons.add(decisionSeasons);
                return;
            }
        }
        tally.randomEventsPerGame.add(new int[] {randomEvents[0], randomEvents[1], seasons});
        tally.decisionSeasons.add(decisionSeasons);
    }

    /**
     * Answers a waiting R&amp;D decision the way a player might: a random option, with the first
     * engineers who fit when it takes someone. Draws from its own seed, so the season's draws stay.
     */
    private void decideWaiting(PlayerSnapshot snapshot, Random random) {
        var pending = snapshot.getPendingDecision();
        if (pending == null || pending.decided()) {
            return;
        }
        var decision = de.mkoehler.launchwindow.content.Decisions.standard().byId(pending.decisionId()).orElse(null);
        var component = catalog.missionComponent(pending.componentId()).orElse(null);
        if (decision == null || component == null) {
            return;
        }
        java.util.SplittableRandom pick = new java.util.SplittableRandom(pending.offeredTurn() * 31L + 7);
        var option = decision.options().get(pick.nextInt(decision.options().size()));
        String person = null;
        String second = null;
        switch (option.takes()) {
            case OVERSEER -> person = first(de.mkoehler.launchwindow.rules.RdDecisions.overseers(snapshot, component), 0);
            case SECONDED -> person = first(de.mkoehler.launchwindow.rules.RdDecisions.team(snapshot, component.id()), 0);
            case PAIR -> {
                var people = de.mkoehler.launchwindow.rules.RdDecisions.pairable(snapshot, component);
                person = first(people, 0);
                second = first(people, 1);
            }
            default -> { }
        }
        var outcome = de.mkoehler.launchwindow.rules.RdDecisions.decide(snapshot, catalog,
            de.mkoehler.launchwindow.content.Decisions.standard(), option.id(), person, second);
        if (outcome == de.mkoehler.launchwindow.rules.RdDecisions.Outcome.STARTED) {
            playHandsOn(snapshot, decision);
            return;
        }
        if (outcome != de.mkoehler.launchwindow.rules.RdDecisions.Outcome.DECIDED) {
            de.mkoehler.launchwindow.rules.RdDecisions.decide(snapshot, catalog,
                de.mkoehler.launchwindow.content.Decisions.standard(), decision.defaultOption(), null, null);
        }
    }

    /**
     * Plays a hands-on campaign to its end the way a careful player might: each round the solver's
     * plan from where it stands (as if every roll went as written), an expert on every fail while
     * shifts last. Counts the results in {@link #handsOnResults}.
     */
    private void playHandsOn(PlayerSnapshot snapshot, de.mkoehler.launchwindow.content.Decisions.Decision decision) {
        var decisions = de.mkoehler.launchwindow.content.Decisions.standard();
        for (int guard = 0; guard < 100 && snapshot.getPendingDecision().handsOnInProgress(); guard++) {
            var state = snapshot.getPendingDecision().handsOn();
            if (state.phase() == de.mkoehler.launchwindow.model.HandsOnState.Phase.AWAITING_RESIST) {
                String resisted = de.mkoehler.launchwindow.rules.RdDecisions.handsOn(snapshot, catalog, decisions,
                    de.mkoehler.launchwindow.rules.RdDecisions.HandsOnStep.RESIST, null);
                if (!resisted.equals("DONE")) {
                    de.mkoehler.launchwindow.rules.RdDecisions.handsOn(snapshot, catalog, decisions,
                        de.mkoehler.launchwindow.rules.RdDecisions.HandsOnStep.ACCEPT, null);
                }
                continue;
            }
            List<List<String>> plan = de.mkoehler.launchwindow.rules.HandsOn.solve(decision.handsOn(), state);
            for (String command : plan == null || plan.isEmpty() ? List.<String>of() : plan.getFirst()) {
                de.mkoehler.launchwindow.rules.RdDecisions.handsOn(snapshot, catalog, decisions,
                    de.mkoehler.launchwindow.rules.RdDecisions.HandsOnStep.QUEUE, command);
            }
            de.mkoehler.launchwindow.rules.RdDecisions.handsOn(snapshot, catalog, decisions,
                de.mkoehler.launchwindow.rules.RdDecisions.HandsOnStep.CONFIRM, null);
        }
        var finished = snapshot.getPendingDecision().handsOn();
        if (finished != null && finished.result() != null) {
            handsOnResults.merge(finished.result().name(), 1, Integer::sum);
        }
    }

    /** The hands-on campaigns the bot played, by result, over every run of this harness. */
    private final Map<String, Integer> handsOnResults = new java.util.TreeMap<>();

    /**
     * Returns the hands-on campaigns the bot played, by result.
     *
     * @return the counts
     */
    public Map<String, Integer> handsOnResults() {
        return handsOnResults;
    }

    private static String first(List<? extends Personnel> people, int index) {
        return people.size() > index ? people.get(index).getId() : null;
    }

    /** Checks the budget phase against an independent recomputation taken just before it runs. */
    private final class BudgetWatch implements SeasonResolver.Observer {
        private long expectedExpenses;
        private long fundsBefore;

        @Override
        public void before(SeasonResolver.Phase phase, PlayerSnapshot snapshot) {
            if (phase == SeasonResolver.Phase.BUDGET) {
                expectedExpenses = independentRunningCosts(snapshot);
                fundsBefore = snapshot.getFunds();
            }
        }

        @Override
        public void after(SeasonResolver.Phase phase, PlayerSnapshot snapshot, List<ResolutionEvent> events) {
            if (phase == SeasonResolver.Phase.BUDGET) {
                checkBudget(events, fundsBefore, expectedExpenses, snapshot);
            }
        }
    }

    // ------------------------------------------------------------------ the bot

    private void act(PlayerSnapshot snapshot, Random random, Tally tally) {
        if (snapshot.getFunds() < 0) {
            long amount = PrestigeConversion.maxConversion(snapshot, catalog);
            if (amount > 0 && PrestigeConversion.convert(snapshot, catalog, schedule, amount)
                    == PrestigeConversion.Outcome.CONVERTED) {
                tally.debtConversions++;
            }
        }
        MissionConfiguration target = target(snapshot);
        if (target == null) {
            return;
        }
        tally.targetSeasons.merge(target.id(), 1, Integer::sum);
        Program program = catalog.program(target.programId()).orElseThrow();
        if (!snapshot.getOpenedProgramIds().contains(program.id())) {
            makeRoomForProgram(snapshot, target, tally);
            openProgram(snapshot, program, tally);
        }
        MissionComponent rocket = cheapestRocketFor(snapshot.getArchetype(), target);
        if (rocket != null && !isActive(snapshot, rocket.id())) {
            makeRoomForRocket(snapshot, rocket, tally);
            openRocket(snapshot, rocket, tally);
        }
        ensureStaff(snapshot, target, random, tally);
        MissionConfiguration side = bot == Bot.SIDE_MISSIONS ? sideTarget(snapshot, target, rocket, tally) : null;
        assignResearch(snapshot, target, rocket, side);
        boolean hardwareReady = rocket != null && hardwareReady(snapshot, target, rocket);
        // Staff train while the hardware is still in R&D, and stay home once it's ready to fly.
        if (!hardwareReady) {
            // The working-ahead bot keeps its people home once the hardware is close, so the flight
            // doesn't wait out a training course.
            if (!ahead || !hardwareReady(snapshot, target, rocket, LAUNCH_RELIABILITY - CLOSE_TO_READY)) {
                trainIdle(snapshot, random);
            }
            tally.stalls.merge("hardware not ready", 1, Integer::sum);
        } else if (snapshot.getInProgressMissions().values().stream()
                .noneMatch(mission -> mission.getMissionConfigurationId().equals(target.id()))) {
            launch(snapshot, target, rocket, tally, "");
        }
        if (side != null) {
            flySide(snapshot, target, rocket, side, random, tally);
        }
    }

    // ------------------------------------------------------------------ contracts

    /**
     * The bot's contract actions: fly the awarded contract once its hardware is ready, else apply for
     * the first listing it can.
     */
    private void actContracts(PlayerSnapshot snapshot, Tally tally) {
        var contract = snapshot.getActiveContract();
        if (contract != null) {
            if (contract.missionId() != null || !snapshot.getOpenedProgramIds().contains(contract.programId())) {
                return;
            }
            MissionConfiguration configuration = catalog.missionConfiguration(contract.configurationId()).orElseThrow();
            MissionComponent rocket = configuration.launches().stream().anyMatch(launch -> launch.launchType()
                .needsRocket()) ? cheapestRocketFor(snapshot.getArchetype(), configuration) : null;
            boolean ready = rocket == null ? hardwareReadyWithoutRocket(snapshot, configuration)
                : hardwareReady(snapshot, configuration, rocket);
            if (ready) {
                launch(snapshot, configuration, rocket, tally, "contract: ");
            } else {
                tally.stalls.merge("contract: hardware not ready", 1, Integer::sum);
            }
            return;
        }
        if (snapshot.getContractApplication() == null) {
            for (var listing : snapshot.getContractBoard()) {
                if (de.mkoehler.launchwindow.rules.Contracts.apply(snapshot, listing.id(), null, catalog)
                        == de.mkoehler.launchwindow.rules.ContractOutcome.APPLIED) {
                    tally.contracts.merge("APPLIED", 1, Integer::sum);
                    return;
                }
            }
        }
    }

    /**
     * Settles the simulated market for the bot: the board from its own lines, and its award at the rank
     * drawn among its rivals. Draws from its own seed, so the season's draws stay as without contracts.
     */
    private de.mkoehler.launchwindow.rules.Contracts.Turn market(PlayerSnapshot snapshot,
            List<de.mkoehler.launchwindow.model.ContractListing> board, List<String> usedClients, int season) {
        var settlement = de.mkoehler.launchwindow.rules.ContractMarket.settle(Map.of("bot", snapshot), Map.of(), board,
            usedClients, catalog, de.mkoehler.launchwindow.content.CommercialClients.EMPTY, gameNumber, season);
        usedClients.clear();
        usedClients.addAll(settlement.usedClientIds());
        java.util.SplittableRandom draw = new java.util.SplittableRandom(gameNumber * 104_729L + season);
        List<de.mkoehler.launchwindow.rules.ContractFinding> findings = new ArrayList<>();
        for (var finding : settlement.findings().getOrDefault("bot", List.of())) {
            if (finding.kind() != de.mkoehler.launchwindow.rules.ContractFinding.Kind.AWARDED) {
                findings.add(finding);
                continue;
            }
            int rank = draw.nextInt(contractRivals + 1) + 1;
            findings.add(rank > de.mkoehler.launchwindow.rules.Contracts.TIERS
                ? new de.mkoehler.launchwindow.rules.ContractFinding(
                    de.mkoehler.launchwindow.rules.ContractFinding.Kind.MISSED, finding.listing(), 0, 0,
                    finding.programId(), finding.configurationId(), List.of())
                : new de.mkoehler.launchwindow.rules.ContractFinding(finding.kind(), finding.listing(), rank,
                    de.mkoehler.launchwindow.rules.Contracts.fee(de.mkoehler.launchwindow.rules.Contracts.launchCost(
                        snapshot, finding.configurationId(), catalog), rank), finding.programId(),
                    finding.configurationId(), List.of()));
        }
        return new de.mkoehler.launchwindow.rules.Contracts.Turn(findings, settlement.board(), settlement.posted());
    }

    // ------------------------------------------------------------------ side missions

    /**
     * The side flight to work on: the current one while it's still worth it, otherwise the best new one,
     * or none. A filler (a flight in an open program, on an open rocket, with its hardware at the bar)
     * beats a venture (anything that needs a program, a rocket or research first). A venture gets
     * {@link #SIDE_GIVE_UP} seasons; one that didn't fly is left alone until the next funding review.
     * A finished or abandoned side flight's program closes unless the plan flies it.
     */
    private MissionConfiguration sideTarget(PlayerSnapshot snapshot, MissionConfiguration target,
            MissionComponent mainRocket, Tally tally) {
        if (seasonNow % de.mkoehler.launchwindow.rules.BudgetProgress.SEASONS_PER_REVIEW == 0) {
            sideGivenUp.clear();
        }
        if (sideId != null) {
            MissionConfiguration current = catalog.missionConfiguration(sideId).orElseThrow();
            boolean flying = snapshot.getInProgressMissions().values().stream()
                .anyMatch(mission -> mission.getMissionConfigurationId().equals(current.id()));
            boolean keep = flying || hasOpenGoal(snapshot, current)
                && (sideFiller ? isFiller(snapshot, current) : seasonNow - sideSince < SIDE_GIVE_UP);
            if (keep) {
                return current;
            }
            if (hasOpenGoal(snapshot, current) && !sideFiller) {
                sideGivenUp.add(current.id());
            }
            if (!remainingPlanPrograms(snapshot, target).contains(current.programId())) {
                ProgramOpening.close(snapshot, current.programId(), catalog);
            }
            sideId = null;
            sideRocketId = null;
        }
        long spendable = snapshot.getFunds() - reserve(snapshot, target, mainRocket);
        // A new program's upkeep must fit inside the season's funding, or the agency slides into debt
        // that stalls the plan.
        long headroom = snapshot.getFundingRatePerSeason() - independentRunningCosts(snapshot);
        Set<String> plan = new HashSet<>(plans.get(snapshot.getArchetype()));
        MissionConfiguration best = null;
        MissionComponent bestRocket = null;
        boolean bestFiller = false;
        double bestScore = 0;
        for (MissionConfiguration candidate : catalog.missionConfigurations()) {
            if (plan.contains(candidate.id())) {
                continue;
            }
            Program program = catalog.program(candidate.programId()).orElseThrow();
            if (!program.availableTo(snapshot.getArchetype())
                    || !program.requiredGoalIds().stream().allMatch(snapshot::hasAchievedOrCancelledGoal)
                    || !candidate.requiredGoalIds().stream().allMatch(snapshot::hasAchievedOrCancelledGoal)) {
                continue;
            }
            // A flight pays only for goals the agency doesn't hold yet, with everything they cancel.
            long gain = 0;
            for (String goal : goalsOf(candidate)) {
                gain += de.mkoehler.launchwindow.rules.GoalCascade.maxPrestige(goal, snapshot, catalog);
            }
            if (gain <= 0) {
                continue;
            }
            boolean filler = isFiller(snapshot, candidate);
            String rejected = sideRejection(snapshot, candidate, program, filler, spendable, headroom, mainRocket,
                target);
            if (rejected != null) {
                tally.sideRejections.merge((filler ? "filler: " : "venture: ") + rejected, 1, Integer::sum);
                continue;
            }
            MissionComponent rocket = sideRocketFor(snapshot, candidate, filler);
            long cost = launchCost(candidate, rocket)
                + (snapshot.getOpenedProgramIds().contains(program.id()) ? 0
                    : ProgramOpening.openingPrice(snapshot, program, catalog))
                + (rocket == null || isActive(snapshot, rocket.id()) ? 0 : rocket.creationCost());
            double score = gain / (cost + SIDE_RESEARCH_WEIGHT * toResearch(snapshot, candidate, rocket));
            if (filler && !bestFiller || filler == bestFiller && score > bestScore) {
                best = candidate;
                bestRocket = rocket;
                bestFiller = filler;
                bestScore = score;
            }
        }
        if (best != null) {
            sideId = best.id();
            sideRocketId = bestRocket == null ? null : bestRocket.id();
            sideFiller = bestFiller;
            sideSince = seasonNow;
        }
        return best;
    }

    /** Why a side flight can't be taken on now, or {@code null} if it can. */
    private String sideRejection(PlayerSnapshot snapshot, MissionConfiguration candidate, Program program,
            boolean filler, long spendable, long headroom, MissionComponent mainRocket, MissionConfiguration target) {
        if (!filler && sideGivenUp.contains(candidate.id())) {
            return "given up";
        }
        if (candidate.requiredAstronautCount() + candidate.requiredCapcomCount()
                    > BuildingUpgrades.capacity(snapshot, Building.ASTRONAUT_CENTER, buildings)
                || candidate.requiredMissionControlOfficerCount()
                    > BuildingUpgrades.capacity(snapshot, Building.MISSION_CONTROL, buildings)) {
            return "crew over the building caps";
        }
        boolean needsRocket = candidate.launches().stream().anyMatch(launch -> launch.launchType().needsRocket());
        MissionComponent rocket = needsRocket ? sideRocketFor(snapshot, candidate, filler) : null;
        if (needsRocket && rocket == null) {
            return "no rocket";
        }
        if (filler) {
            return fillerAffordable(snapshot, candidate, rocket, target, mainRocket) ? null : "funds";
        }
        boolean programOpen = snapshot.getOpenedProgramIds().contains(program.id());
        if (!programOpen && snapshot.getOpenedProgramIds().size()
                >= BuildingUpgrades.capacity(snapshot, Building.HEADQUARTERS, buildings)) {
            return "Headquarters full";
        }
        if (!programOpen && program.maintenanceCost() > headroom) {
            return "upkeep over the funding";
        }
        long cost = launchCost(candidate, rocket)
            + (programOpen ? 0 : ProgramOpening.openingPrice(snapshot, program, catalog))
            + (rocket == null || isActive(snapshot, rocket.id()) ? 0 : rocket.creationCost());
        return cost > spendable ? "funds" : null;
    }

    /** A filler is flown from what the agency has: its program open, a rocket open, its hardware at the bar. */
    private boolean isFiller(PlayerSnapshot snapshot, MissionConfiguration configuration) {
        if (!snapshot.getOpenedProgramIds().contains(configuration.programId())) {
            return false;
        }
        boolean needsRocket = configuration.launches().stream().anyMatch(launch -> launch.launchType().needsRocket());
        MissionComponent rocket = needsRocket ? sideRocketFor(snapshot, configuration, true) : null;
        return (!needsRocket || rocket != null) && toResearch(snapshot, configuration, rocket) == 0;
    }

    /** A filler costs only its launch: the agency stays solvent and keeps the plan's own launch in hand. */
    private boolean fillerAffordable(PlayerSnapshot snapshot, MissionConfiguration side, MissionComponent rocket,
            MissionConfiguration target, MissionComponent mainRocket) {
        long cost = launchCost(side, rocket);
        return snapshot.getFunds() > 0 && snapshot.getFunds() - cost >= launchCost(target, mainRocket);
    }

    /** The reliability still to research before a flight reaches the bar, summed over what it flies. */
    private double toResearch(PlayerSnapshot snapshot, MissionConfiguration configuration, MissionComponent rocket) {
        List<String> flown = flownComponents(configuration);
        if (rocket != null) {
            flown.add(rocket.id());
        }
        double total = 0;
        for (String componentId : flown) {
            MissionComponent component = catalog.missionComponent(componentId).orElseThrow();
            OwnedMissionComponent owned = snapshot.getMissionComponents().get(componentId);
            double now = owned == null ? component.baselineReliability() : owned.getCurrentReliability();
            total += Math.max(0, Math.min(LAUNCH_RELIABILITY, component.maxRdReliability() - 0.5) - now);
        }
        return total;
    }

    /**
     * A rocket for a side flight: the cheapest already open that lifts it, so side flights don't churn
     * rocket programs; for a venture, failing that, the cheapest the VAB can open now.
     */
    private MissionComponent sideRocketFor(PlayerSnapshot snapshot, MissionConfiguration configuration,
            boolean openOnly) {
        MissionComponent bestOpen = null;
        MissionComponent bestNew = null;
        boolean vabRoom = openRockets(snapshot)
            < BuildingUpgrades.capacity(snapshot, Building.VEHICLE_ASSEMBLY, buildings);
        for (String id : rocketIds()) {
            MissionComponent rocket = catalog.missionComponent(id).orElseThrow();
            if (!rocket.availableTo(snapshot.getArchetype()) || !lifts(rocket, configuration)) {
                continue;
            }
            if (isActive(snapshot, id)) {
                if (bestOpen == null || rocket.unitCost() < bestOpen.unitCost()) {
                    bestOpen = rocket;
                }
            } else if (!openOnly && vabRoom && RocketPrograms.classAllowed(rocket.rocket().rocketClass(),
                    snapshot.buildingLevel(Building.VEHICLE_ASSEMBLY))
                    && (bestNew == null || rocket.unitCost() < bestNew.unitCost())) {
                bestNew = rocket;
            }
        }
        return bestOpen != null ? bestOpen : bestNew;
    }

    /**
     * Opens what a venture needs, then, once the hardware is ready, hires the crew within the buildings'
     * caps and launches. Nothing permanent is bought for a side flight before it can fly: no building
     * levels, and no hires while it waits on R&amp;D.
     */
    private void flySide(PlayerSnapshot snapshot, MissionConfiguration target, MissionComponent mainRocket,
            MissionConfiguration side, Random random, Tally tally) {
        if (snapshot.getInProgressMissions().values().stream()
                .anyMatch(mission -> mission.getMissionConfigurationId().equals(side.id()))) {
            return;
        }
        Program program = catalog.program(side.programId()).orElseThrow();
        MissionComponent rocket = sideRocketId == null ? null : catalog.missionComponent(sideRocketId).orElseThrow();
        if (!sideFiller) {
            long spendable = snapshot.getFunds() - reserve(snapshot, target, mainRocket);
            if (!snapshot.getOpenedProgramIds().contains(program.id())) {
                if (ProgramOpening.openingPrice(snapshot, program, catalog) > spendable) {
                    return;
                }
                openProgram(snapshot, program, tally);
                spendable = snapshot.getFunds() - reserve(snapshot, target, mainRocket);
            }
            if (rocket != null && !isActive(snapshot, rocket.id())) {
                if (rocket.creationCost() > spendable) {
                    return;
                }
                openRocket(snapshot, rocket, tally);
            }
        }
        boolean ready = rocket == null ? hardwareReadyWithoutRocket(snapshot, side)
            : hardwareReady(snapshot, side, rocket);
        boolean affordable = sideFiller ? fillerAffordable(snapshot, side, rocket, target, mainRocket)
            : launchCost(side, rocket) <= snapshot.getFunds() - reserve(snapshot, target, mainRocket);
        if (!ready || !affordable) {
            return;
        }
        // The side flight's people come on top of what the plan's flight needs, so it never takes theirs.
        boolean mainFlying = snapshot.getInProgressMissions().values().stream()
            .anyMatch(mission -> mission.getMissionConfigurationId().equals(target.id()));
        int astronauts = side.requiredAstronautCount() + side.requiredCapcomCount()
            + (mainFlying ? 0 : target.requiredAstronautCount() + target.requiredCapcomCount());
        int controllers = side.requiredMissionControlOfficerCount()
            + (mainFlying ? 0 : target.requiredMissionControlOfficerCount());
        hireBest(snapshot, PersonnelGroup.ASTRONAUT, astronauts - idleCount(snapshot.getAstronauts()), random, tally);
        hireBest(snapshot, PersonnelGroup.MISSION_CONTROL,
            controllers - idleCount(snapshot.getMissionControlOfficers()), random, tally);
        if (idleCount(snapshot.getAstronauts()) < astronauts
                || idleCount(snapshot.getMissionControlOfficers()) < controllers) {
            tally.stalls.merge("side: the plan's people", 1, Integer::sum);
            return;
        }
        int before = tally.launchedThisSeason;
        launch(snapshot, side, rocket, tally, "side: ");
        if (tally.launchedThisSeason > before) {
            tally.sideLaunches.merge(side.id(), 1, Integer::sum);
            tally.sideKinds.merge(sideFiller ? "filler" : "venture", 1, Integer::sum);
        }
    }

    /**
     * The money the plan needs next, which a venture leaves alone: the plan's current launch, its rocket
     * if it isn't open, the next program it hasn't opened, and a few seasons' running costs.
     */
    private long reserve(PlayerSnapshot snapshot, MissionConfiguration target, MissionComponent mainRocket) {
        long reserve = launchCost(target, mainRocket) + SIDE_CUSHION_SEASONS * independentRunningCosts(snapshot);
        if (mainRocket != null && !isActive(snapshot, mainRocket.id())) {
            reserve += mainRocket.creationCost();
        }
        List<String> plan = plans.get(snapshot.getArchetype());
        for (String id : plan.subList(plan.indexOf(target.id()), plan.size())) {
            Program program = catalog.program(catalog.missionConfiguration(id).orElseThrow().programId())
                .orElseThrow();
            if (!snapshot.getOpenedProgramIds().contains(program.id())) {
                reserve += ProgramOpening.openingPrice(snapshot, program, catalog);
                break;
            }
        }
        return reserve;
    }

    /** The programs the plan still flies, from its current flight on. */
    private Set<String> remainingPlanPrograms(PlayerSnapshot snapshot, MissionConfiguration target) {
        List<String> plan = plans.get(snapshot.getArchetype());
        Set<String> programs = new HashSet<>();
        for (String id : plan.subList(plan.indexOf(target.id()), plan.size())) {
            programs.add(catalog.missionConfiguration(id).orElseThrow().programId());
        }
        return programs;
    }

    private static long launchCost(MissionConfiguration configuration, MissionComponent rocket) {
        long rockets = configuration.launches().stream().filter(launch -> launch.launchType().needsRocket()).count();
        return configuration.cost() + (rocket == null ? 0 : rockets * rocket.unitCost());
    }

    private boolean lifts(MissionComponent rocket, MissionConfiguration configuration) {
        return configuration.launches().stream().filter(launch -> launch.launchType().needsRocket())
            .allMatch(launch -> {
                double capacity = rocket.payloadCapacity(launch.launchType());
                return capacity > 0 && capacity >= requiredLift(launch);
            });
    }

    private boolean hardwareReadyWithoutRocket(PlayerSnapshot snapshot, MissionConfiguration configuration) {
        for (String componentId : flownComponents(configuration)) {
            OwnedMissionComponent owned = snapshot.getMissionComponents().get(componentId);
            MissionComponent component = catalog.missionComponent(componentId).orElseThrow();
            if (owned == null || owned.getCurrentReliability()
                    < Math.min(LAUNCH_RELIABILITY, component.maxRdReliability() - 0.5)) {
                return false;
            }
        }
        return true;
    }

    private static int idleCount(List<? extends Personnel> roster) {
        return (int) roster.stream().filter(ContentPackPlayouts::available).count();
    }

    private static Set<String> goalsOf(MissionConfiguration configuration) {
        Set<String> goals = new java.util.LinkedHashSet<>();
        configuration.seasons().forEach(season -> season.steps().forEach(step -> goals.addAll(step.goalIdsAchieved())));
        return goals;
    }

    private static boolean hasOpenGoal(PlayerSnapshot snapshot, MissionConfiguration configuration) {
        return goalsOf(configuration).stream().anyMatch(goal -> !snapshot.hasAchievedOrCancelledGoal(goal));
    }

    /** The first configuration in the agency's plan that still has a goal of its own to reach. */
    private MissionConfiguration target(PlayerSnapshot snapshot) {
        for (String id : plans.get(snapshot.getArchetype())) {
            MissionConfiguration configuration = catalog.missionConfiguration(id).orElseThrow();
            boolean open = configuration.seasons().stream().flatMap(season -> season.steps().stream())
                .flatMap(step -> step.goalIdsAchieved().stream())
                .anyMatch(goal -> !snapshot.hasAchievedOrCancelledGoal(goal));
            if (open) {
                return configuration;
            }
        }
        return null;
    }

    private void openProgram(PlayerSnapshot snapshot, Program program, Tally tally) {
        long expected = independentOpeningPrice(snapshot, program);
        long before = snapshot.getFunds();
        OpenProgramOutcome outcome = ProgramOpening.open(snapshot, program.id(), catalog);
        long charged = before - snapshot.getFunds();
        if (charged != (outcome == OpenProgramOutcome.OPENED ? expected : 0)) {
            throw new IllegalStateException("Opening " + program.id() + " (" + outcome + ") charged " + charged
                + ", expected " + expected);
        }
        if (outcome == OpenProgramOutcome.OPENED) {
            tally.programsOpened++;
        } else {
            tally.stalls.merge("program " + outcome, 1, Integer::sum);
        }
    }

    /**
     * With Headquarters full, closes the open programs the rest of the plan no longer flies, or failing
     * that starts Headquarters' next level (the early race never fills it).
     */
    private void makeRoomForProgram(PlayerSnapshot snapshot, MissionConfiguration target, Tally tally) {
        if (snapshot.getOpenedProgramIds().size()
                < BuildingUpgrades.capacity(snapshot, Building.HEADQUARTERS, buildings)) {
            return;
        }
        List<String> plan = plans.get(snapshot.getArchetype());
        Set<String> stillFlown = new HashSet<>();
        for (String id : plan.subList(plan.indexOf(target.id()), plan.size())) {
            stillFlown.add(catalog.missionConfiguration(id).orElseThrow().programId());
        }
        if (sideId != null) {
            stillFlown.add(catalog.missionConfiguration(sideId).orElseThrow().programId());
        }
        for (String programId : List.copyOf(snapshot.getOpenedProgramIds())) {
            if (!stillFlown.contains(programId)) {
                ProgramOpening.close(snapshot, programId, catalog);
            }
        }
        if (snapshot.getOpenedProgramIds().size()
                >= BuildingUpgrades.capacity(snapshot, Building.HEADQUARTERS, buildings)) {
            upgrade(snapshot, Building.HEADQUARTERS, tally);
        }
    }

    /**
     * Makes the VAB ready for a rocket: starts its next level when the rocket's class needs one, and
     * with the VAB full, closes the rocket programs no mission in flight uses.
     */
    private void makeRoomForRocket(PlayerSnapshot snapshot, MissionComponent rocket, Tally tally) {
        if (!RocketPrograms.classAllowed(rocket.rocket().rocketClass(),
                snapshot.buildingLevel(Building.VEHICLE_ASSEMBLY))) {
            upgrade(snapshot, Building.VEHICLE_ASSEMBLY, tally);
            return;
        }
        if (openRockets(snapshot) < BuildingUpgrades.capacity(snapshot, Building.VEHICLE_ASSEMBLY, buildings)) {
            return;
        }
        for (String id : rocketIds()) {
            if (isActive(snapshot, id) && !RocketPrograms.inFlight(snapshot, id)) {
                RocketPrograms.close(snapshot, id, catalog);
            }
        }
    }

    private void openRocket(PlayerSnapshot snapshot, MissionComponent rocket, Tally tally) {
        long before = snapshot.getFunds();
        int openBefore = openRockets(snapshot);
        RocketProgramOutcome outcome = RocketPrograms.open(snapshot, rocket.id(), catalog);
        long charged = before - snapshot.getFunds();
        if (charged != (outcome == RocketProgramOutcome.OPENED ? rocket.creationCost() : 0)) {
            throw new IllegalStateException("Opening rocket " + rocket.id() + " (" + outcome + ") charged " + charged);
        }
        if (outcome != RocketProgramOutcome.OPENED) {
            tally.stalls.merge("rocket " + outcome, 1, Integer::sum);
        }
        if (outcome == RocketProgramOutcome.OPENED) {
            tally.rocketsOpened++;
            int vabLevel = snapshot.getBuildingLevels().getOrDefault(Building.VEHICLE_ASSEMBLY, 1);
            if (openBefore >= tableCap(Building.VEHICLE_ASSEMBLY, vabLevel)
                    || rocket.rocket().rocketClass().ordinal() >= vabLevel) {
                throw new IllegalStateException("Rocket " + rocket.id() + " opened past the VAB's cap or class");
            }
        }
    }

    /** Upgrades Mission Control or the Astronaut Center when the target needs more, and hires toward it. */
    private void ensureStaff(PlayerSnapshot snapshot, MissionConfiguration target, Random random, Tally tally) {
        // The working-ahead bot keeps spares, so someone back from training or sick doesn't hold a flight.
        int spares = ahead ? AHEAD_SPARES : 0;
        int controllers = target.requiredMissionControlOfficerCount() + spares;
        int astronauts = target.requiredAstronautCount() + target.requiredCapcomCount() + spares;
        maybeUpgrade(snapshot, Building.MISSION_CONTROL, controllers, tally);
        maybeUpgrade(snapshot, Building.ASTRONAUT_CENTER, astronauts + 1, tally);
        hireBest(snapshot, PersonnelGroup.MISSION_CONTROL,
            controllers - snapshot.getMissionControlOfficers().size(), random, tally);
        hireBest(snapshot, PersonnelGroup.ASTRONAUT, astronauts + 1 - snapshot.getAstronauts().size(), random, tally);
        int specialists = STARTING_SPECIALISTS;
        if (ahead) {
            specialists = BuildingUpgrades.capacity(snapshot, Building.SET_CENTER, buildings);
            if (snapshot.getSetSpecialists().size() >= specialists && snapshot.getFunds() > AHEAD_RESERVE) {
                upgrade(snapshot, Building.SET_CENTER, tally);
            }
        }
        hireBest(snapshot, PersonnelGroup.SET, specialists - snapshot.getSetSpecialists().size(), random, tally);
    }

    private void maybeUpgrade(PlayerSnapshot snapshot, Building building, int needed, Tally tally) {
        if (needed <= BuildingUpgrades.capacity(snapshot, building, buildings)) {
            return;
        }
        upgrade(snapshot, building, tally);
    }

    /** Starts a building's next level, unless one is under way. */
    private void upgrade(PlayerSnapshot snapshot, Building building, Tally tally) {
        if (snapshot.getBuildingUpgradeSeasonsRemaining().containsKey(building)) {
            return;
        }
        int level = snapshot.buildingLevel(building);
        long before = snapshot.getFunds();
        BuildingUpgradeOutcome outcome = BuildingUpgrades.startUpgrade(snapshot, building, buildings);
        long expected = outcome == BuildingUpgradeOutcome.STARTED
            ? buildings.of(building).upgradeCosts().get(level - 1) : 0;
        if (before - snapshot.getFunds() != expected) {
            throw new IllegalStateException("Upgrade of " + building + " charged " + (before - snapshot.getFunds()));
        }
        if (outcome == BuildingUpgradeOutcome.STARTED) {
            tally.upgrades++;
        }
    }

    private void hireBest(PlayerSnapshot snapshot, PersonnelGroup group, int count, Random random, Tally tally) {
        for (int i = 0; i < count; i++) {
            List<? extends Personnel> pool = switch (group) {
                case ASTRONAUT -> snapshot.getAstronautCandidates();
                case SET -> snapshot.getSetSpecialistCandidates();
                case MISSION_CONTROL -> snapshot.getMissionControlCandidates();
            };
            if (pool.isEmpty()) {
                return;
            }
            Personnel best = pool.stream().max(Comparator.comparingDouble(ContentPackPlayouts::averageSkill))
                .orElseThrow();
            int sizeBefore = rosterSize(snapshot, group);
            HireOutcome outcome = HiringPool.hire(snapshot, group, best.getId(), random, buildings);
            int cap = tableCap(switch (group) {
                case ASTRONAUT -> Building.ASTRONAUT_CENTER;
                case SET -> Building.SET_CENTER;
                case MISSION_CONTROL -> Building.MISSION_CONTROL;
            }, snapshot.getBuildingLevels().getOrDefault(switch (group) {
                case ASTRONAUT -> Building.ASTRONAUT_CENTER;
                case SET -> Building.SET_CENTER;
                case MISSION_CONTROL -> Building.MISSION_CONTROL;
            }, 1));
            if ((outcome == HireOutcome.HIRED) != (sizeBefore < cap)) {
                throw new IllegalStateException(group + " hire " + outcome + " at " + sizeBefore + "/" + cap);
            }
            if (outcome != HireOutcome.HIRED) {
                return;
            }
            tally.hires++;
        }
    }

    /** Points idle SET specialists at the target's components below their R&D ceiling. */
    private void assignResearch(PlayerSnapshot snapshot, MissionConfiguration target, MissionComponent rocket,
            MissionConfiguration side) {
        List<String> components = flownComponents(target);
        if (rocket != null && isActive(snapshot, rocket.id())) {
            components.add(rocket.id());
        }
        // A side flight comes after the plan's current flight and before its later ones.
        if (side != null) {
            components.addAll(flownComponents(side));
            if (sideRocketId != null) {
                components.add(sideRocketId);
            }
        }
        if (ahead) {
            List<String> plan = plans.get(snapshot.getArchetype());
            for (String id : plan.subList(plan.indexOf(target.id()) + 1, plan.size())) {
                MissionConfiguration later = catalog.missionConfiguration(id).orElseThrow();
                components.addAll(flownComponents(later));
                MissionComponent laterRocket = cheapestRocketFor(snapshot.getArchetype(), later);
                if (laterRocket != null) {
                    components.add(laterRocket.id());
                }
            }
        }
        for (SetSpecialist specialist : snapshot.getSetSpecialists()) {
            if (specialist.getAssignmentState() != AssignmentState.IDLE || specialist.getUnavailableSeasons() > 0) {
                continue;
            }
            for (String componentId : components) {
                OwnedMissionComponent owned = snapshot.getMissionComponents().get(componentId);
                MissionComponent component = catalog.missionComponent(componentId).orElseThrow();
                if (owned == null || !owned.isActive()
                        || owned.getCurrentReliability() >= component.maxRdReliability()) {
                    continue;
                }
                Integer slot = freeSlot(snapshot, componentId);
                if (slot != null) {
                    specialist.setRdTargetComponentId(componentId);
                    specialist.setRdSlot(slot);
                    specialist.setAssignmentState(AssignmentState.ASSIGNED);
                    break;
                }
            }
        }
    }

    /** Sends idle astronauts and controllers into advanced training on their weakest skill, to keep morale up. */
    private void trainIdle(PlayerSnapshot snapshot, Random random) {
        for (Astronaut astronaut : snapshot.getAstronauts()) {
            if (astronaut.getAssignmentState() == AssignmentState.IDLE && random.nextDouble() < 0.25) {
                AstronautSkill weakest = AstronautSkill.values()[0];
                for (AstronautSkill skill : AstronautSkill.values()) {
                    if (astronaut.getSkill(skill) < astronaut.getSkill(weakest)) {
                        weakest = skill;
                    }
                }
                startAdvanced(astronaut);
                astronaut.setTrainingTargetSkill(weakest);
            }
        }
        for (MissionControlOfficer officer : snapshot.getMissionControlOfficers()) {
            if (officer.getAssignmentState() == AssignmentState.IDLE && random.nextDouble() < 0.25) {
                MissionControlSkill weakest = MissionControlSkill.values()[0];
                for (MissionControlSkill skill : MissionControlSkill.values()) {
                    if (officer.getSkill(skill) < officer.getSkill(weakest)) {
                        weakest = skill;
                    }
                }
                startAdvanced(officer);
                officer.setTrainingTargetSkill(weakest);
            }
        }
    }

    private static void startAdvanced(Personnel person) {
        person.setTrainingKind(TrainingKind.ADVANCED);
        person.setTrainingSeasonsTotal(TrainingProgress.ADVANCED_TRAINING_SEASONS);
        person.setTrainingSeasonsCompleted(0);
        person.setAssignmentState(AssignmentState.IN_TRAINING);
    }

    private boolean hardwareReady(PlayerSnapshot snapshot, MissionConfiguration target, MissionComponent rocket) {
        return hardwareReady(snapshot, target, rocket, LAUNCH_RELIABILITY);
    }

    private boolean hardwareReady(PlayerSnapshot snapshot, MissionConfiguration target, MissionComponent rocket,
            double threshold) {
        if (rocket == null || !isActive(snapshot, rocket.id())) {
            return false;
        }
        List<String> flown = flownComponents(target);
        flown.add(rocket.id());
        for (String componentId : flown) {
            OwnedMissionComponent owned = snapshot.getMissionComponents().get(componentId);
            MissionComponent component = catalog.missionComponent(componentId).orElseThrow();
            if (owned == null || owned.getCurrentReliability()
                    < Math.min(threshold, component.maxRdReliability() - 0.5)) {
                return false;
            }
        }
        return true;
    }

    private void launch(PlayerSnapshot snapshot, MissionConfiguration target, MissionComponent rocket, Tally tally,
            String label) {
        // Staffed the way a player's "auto-assign" would: the best idle person per slot (baspm.md §10.11).
        MissionStaffing.Staffing staffing = MissionStaffing.autoAssign(snapshot, target);
        for (int slot : target.usedCrewSlots()) {
            if (staffing.crewSlotIds().get(slot) == null) {
                tally.stalls.merge(label + "too few idle astronauts", 1, Integer::sum);
                return;
            }
        }
        for (int slot : target.usedControlSlots()) {
            if (staffing.controlSlotIds().get(slot) == null) {
                boolean astronautSlot = target.controlSlots().get(slot).kind() == StaffingSlot.Kind.ASTRONAUT;
                tally.stalls.merge(label + (astronautSlot ? "too few idle astronauts" : "too few idle controllers"),
                    1, Integer::sum);
                return;
            }
        }
        // An air drop flies without a rocket: its slot holds "" and costs nothing.
        List<String> rockets = target.launches().stream()
            .map(launch -> launch.launchType().needsRocket() ? rocket.id() : "").toList();
        long expectedCost = launchCost(target, rocket);
        if (snapshot.getFunds() < expectedCost) {
            tally.stalls.merge(label + "funds", 1, Integer::sum);
            return;
        }
        long before = snapshot.getFunds();
        MissionLaunchOutcome outcome = MissionLaunch.launch(snapshot,
            new LaunchRequest(target.id(), staffing.crewSlotIds(), staffing.controlSlotIds(), rockets), catalog);
        long charged = before - snapshot.getFunds();
        if (charged != (outcome == MissionLaunchOutcome.LAUNCHED ? expectedCost : 0)) {
            throw new IllegalStateException("Launching " + target.id() + " (" + outcome + ") charged " + charged
                + ", expected " + expectedCost);
        }
        if (outcome == MissionLaunchOutcome.LAUNCHED) {
            tally.launches.merge(target.id(), 1, Integer::sum);
            tally.launchedThisSeason++;
        } else {
            tally.stalls.merge(label + outcome.name(), 1, Integer::sum);
        }
    }

    // ------------------------------------------------------------------ independent checks

    /** Recomputes the opening price straight from the content: discounted opening cost plus unowned creation cost. */
    private long independentOpeningPrice(PlayerSnapshot snapshot, Program program) {
        int discount = 0;
        for (Program.OpeningCostDiscount entry : program.openingCostDiscounts()) {
            if (snapshot.getOpenedProgramIds().contains(entry.otherProgramId())) {
                discount += entry.percent();
            }
        }
        long price = program.openingCost() * (100 - Math.min(100, discount)) / 100;
        for (String componentId : program.requiredMissionComponentIds()) {
            if (!snapshot.getMissionComponents().containsKey(componentId)) {
                price += catalog.missionComponent(componentId).orElseThrow().creationCost();
            }
        }
        return price;
    }

    /** Salaries, open programs, active components, and buildings at their levels, times the cost multiplier. */
    private long independentRunningCosts(PlayerSnapshot snapshot) {
        long total = 0;
        for (Personnel person : allEmployees(snapshot)) {
            total += Math.round(person.getSalary());
        }
        for (String programId : snapshot.getOpenedProgramIds()) {
            total += catalog.program(programId).orElseThrow().maintenanceCost();
        }
        for (Map.Entry<String, OwnedMissionComponent> owned : snapshot.getMissionComponents().entrySet()) {
            if (owned.getValue().isActive()) {
                total += catalog.missionComponent(owned.getKey()).orElseThrow().maintenanceCost();
            }
        }
        for (Building building : Building.values()) {
            int level = snapshot.getBuildingLevels().getOrDefault(building, 1);
            total += buildings.of(building).maintenance().get(level - 1);
        }
        return Math.round(total * snapshot.getCostMultiplier());
    }

    private static void checkBudget(List<ResolutionEvent> budget, long fundsBefore, long expectedExpenses,
            PlayerSnapshot snapshot) {
        SeasonFundsSettled settled = budget.stream().filter(SeasonFundsSettled.class::isInstance)
            .map(SeasonFundsSettled.class::cast).findFirst().orElseThrow();
        if (settled.expenses() != expectedExpenses) {
            throw new IllegalStateException("Running costs " + settled.expenses() + ", expected " + expectedExpenses);
        }
        if (snapshot.getFunds() != fundsBefore - settled.expenses() + settled.income()) {
            throw new IllegalStateException("Funds don't reconcile after the budget pass");
        }
    }

    /** Caps, agency, and every flying mission's rockets, staffing, and assignment pairing. */
    private void checkState(PlayerSnapshot snapshot) {
        Archetype archetype = snapshot.getArchetype();
        int hqCap = tableCap(Building.HEADQUARTERS, snapshot.getBuildingLevels().getOrDefault(Building.HEADQUARTERS, 1));
        if (snapshot.getOpenedProgramIds().size() > hqCap) {
            throw new IllegalStateException("More programs open than Headquarters allows");
        }
        for (String programId : snapshot.getOpenedProgramIds()) {
            Program program = catalog.program(programId).orElseThrow();
            if (program.agency() != null && program.agency() != archetype) {
                throw new IllegalStateException(archetype + " opened another agency's program " + programId);
            }
        }
        for (String componentId : snapshot.getMissionComponents().keySet()) {
            MissionComponent component = catalog.missionComponent(componentId).orElseThrow();
            if (component.agency() != null && component.agency() != archetype) {
                throw new IllegalStateException(archetype + " owns another agency's component " + componentId);
            }
        }
        if (openRockets(snapshot) > tableCap(Building.VEHICLE_ASSEMBLY,
                snapshot.getBuildingLevels().getOrDefault(Building.VEHICLE_ASSEMBLY, 1))) {
            throw new IllegalStateException("More rocket programs open than the VAB allows");
        }
        Set<String> claimed = new HashSet<>();
        for (InProgressMission mission : snapshot.getInProgressMissions().values()) {
            MissionConfiguration configuration =
                catalog.missionConfiguration(mission.getMissionConfigurationId()).orElseThrow();
            if (mission.getRocketIds().size() != configuration.launches().size()) {
                throw new IllegalStateException("Mission " + mission.getId() + " rockets disagree with its launches");
            }
            checkSlots(snapshot, mission, configuration);
            for (int i = 0; i < mission.getRocketIds().size(); i++) {
                MissionConfiguration.Launch launch = configuration.launches().get(i);
                if (launch.launchType() == LaunchType.AIR_DROP) {
                    if (!mission.getRocketIds().get(i).isEmpty()) {
                        throw new IllegalStateException("Mission " + mission.getId() + " puts a rocket on an air drop");
                    }
                    continue;
                }
                MissionComponent rocket = catalog.missionComponent(mission.getRocketIds().get(i)).orElseThrow();
                double mass = requiredLift(launch);
                double capacity = rocket.rocket() == null ? 0 : switch (launch.launchType()) {
                    case SUBORBITAL, SUBORBITAL_HR -> rocket.rocket().payloadToSuborbital();
                    case LEO, LEO_HR -> rocket.rocket().payloadToLeo();
                    case TLI, TLI_HR -> rocket.rocket().payloadToTli();
                    case TPI, TPI_HR -> rocket.rocket().payloadToTpi();
                    case AIR_DROP -> 0;
                };
                if (launch.launchType().humanRated() && rocket.associatedSkill() != SetSkill.HUMAN_RATED_ROCKETS) {
                    capacity = 0;
                }
                if (capacity <= 0 || capacity < mass) {
                    throw new IllegalStateException("Mission " + mission.getId() + " flies " + rocket.id()
                        + ", which can't lift " + mass + " to " + launch.launchType());
                }
            }
            List<String> everyone = new ArrayList<>(mission.crewIds());
            everyone.addAll(mission.groundIds());
            for (String id : everyone) {
                if (!claimed.add(id)) {
                    throw new IllegalStateException(id + " holds two seats");
                }
                Personnel person = allEmployees(snapshot).stream().filter(p -> p.getId().equals(id)).findFirst()
                    .orElseThrow(() -> new IllegalStateException("Mission seat " + id + " is not on the roster"));
                if (person.getAssignmentState() != AssignmentState.ASSIGNED
                        || !mission.getId().equals(person.getAssignedMissionId())) {
                    throw new IllegalStateException(id + " disagrees with mission " + mission.getId());
                }
            }
        }
        for (String goal : snapshot.getAchievedGoals().keySet()) {
            catalog.goal(goal).orElseThrow(() -> new IllegalStateException("Achieved unknown goal " + goal));
        }
    }

    // ------------------------------------------------------------------ helpers

    private MissionComponent cheapestRocketFor(Archetype archetype, MissionConfiguration configuration) {
        MissionComponent best = null;
        for (String id : rocketIds()) {
            MissionComponent rocket = catalog.missionComponent(id).orElseThrow();
            if (!rocket.availableTo(archetype)) {
                continue;
            }
            boolean liftsAll = configuration.launches().stream().filter(launch -> launch.launchType().needsRocket())
                .allMatch(launch -> {
                    double capacity = rocket.payloadCapacity(launch.launchType());
                    return capacity > 0 && capacity >= requiredLift(launch);
                });
            if (liftsAll && (best == null || rocket.unitCost() < best.unitCost())) {
                best = rocket;
            }
        }
        return best;
    }

    /**
     * The components a configuration flies, from its steps, without its rockets: what its launch
     * reliability is stamped from. A program's other components (the lander on its Earth-orbit tests,
     * say) don't hold the flight back.
     */
    private static List<String> flownComponents(MissionConfiguration configuration) {
        List<String> flown = new ArrayList<>();
        for (MissionSeason season : configuration.seasons()) {
            for (MissionStep step : season.steps()) {
                for (MissionStep.ComponentInvolvement involvement : step.componentInvolvements()) {
                    String id = involvement.componentId();
                    if (!id.startsWith(MissionConfiguration.ROCKET_SLOT_PREFIX) && !flown.contains(id)) {
                        flown.add(id);
                    }
                }
            }
        }
        return flown;
    }

    /** Every rocket in the pack, in the pack's order. */
    private List<String> rocketIds() {
        return catalog.missionComponents().stream().filter(MissionComponent::hasRocketSpec).map(MissionComponent::id)
            .toList();
    }

    /** The payloads' mass less their own lift, worked out here rather than trusting MissionLaunch's. */
    private double requiredLift(MissionConfiguration.Launch launch) {
        double mass = 0;
        for (String id : launch.payloadComponentIds()) {
            MissionComponent payload = catalog.missionComponent(id).orElseThrow();
            mass += payload.mass() - (payload.ownLift() == null ? 0 : switch (launch.launchType()) {
                case SUBORBITAL, SUBORBITAL_HR -> payload.ownLift().payloadToSuborbital();
                case LEO, LEO_HR -> payload.ownLift().payloadToLeo();
                case TLI, TLI_HR -> payload.ownLift().payloadToTli();
                case TPI, TPI_HR -> payload.ownLift().payloadToTpi();
                case AIR_DROP -> 0;
            });
        }
        return Math.max(0, mass);
    }

    private int openRockets(PlayerSnapshot snapshot) {
        int count = 0;
        for (Map.Entry<String, OwnedMissionComponent> owned : snapshot.getMissionComponents().entrySet()) {
            if (owned.getValue().isActive() && catalog.missionComponent(owned.getKey()).orElseThrow().hasRocketSpec()) {
                count++;
            }
        }
        return count;
    }

    private int tableCap(Building building, int level) {
        return buildings.of(building).caps().get(level - 1);
    }

    private static boolean isActive(PlayerSnapshot snapshot, String componentId) {
        OwnedMissionComponent owned = snapshot.getMissionComponents().get(componentId);
        return owned != null && owned.isActive();
    }

    private static Integer freeSlot(PlayerSnapshot snapshot, String componentId) {
        Set<Integer> taken = new HashSet<>();
        for (SetSpecialist specialist : snapshot.getSetSpecialists()) {
            if (componentId.equals(specialist.getRdTargetComponentId())) {
                taken.add(specialist.getRdSlot());
            }
        }
        for (int slot = 0; slot < MissionComponent.RD_SLOT_COUNT; slot++) {
            if (!taken.contains(slot)) {
                return slot;
            }
        }
        return null;
    }

    /**
     * Checks a mission's staffing slot by slot, re-derived from the steps: a used slot
     * holds one person of the slot's type, an unused slot holds nobody.
     */
    private static void checkSlots(PlayerSnapshot snapshot, InProgressMission mission,
            MissionConfiguration configuration) {
        Set<Integer> usedCrew = new HashSet<>();
        Set<Integer> usedControl = new HashSet<>();
        // Crew for the whole mission; a control slot only while the current or a later season uses it.
        for (int season = 0; season < configuration.seasons().size(); season++) {
            for (MissionStep step : configuration.seasons().get(season).steps()) {
                step.crewInvolvements().forEach(involvement -> usedCrew.add(involvement.slot()));
                if (season >= mission.getCurrentSeasonIndex()) {
                    step.controllerInvolvements().forEach(involvement -> usedControl.add(involvement.slot()));
                }
            }
        }
        Set<String> astronautIds = new HashSet<>();
        snapshot.getAstronauts().forEach(astronaut -> astronautIds.add(astronaut.getId()));
        Set<String> controllerIds = new HashSet<>();
        snapshot.getMissionControlOfficers().forEach(officer -> controllerIds.add(officer.getId()));
        if (mission.getCrewSlotIds().size() != configuration.crewSlots().size()
                || mission.getControlSlotIds().size() != configuration.controlSlots().size()) {
            throw new IllegalStateException("Mission " + mission.getId() + " slot lists don't match its layouts");
        }
        for (int slot = 0; slot < configuration.crewSlots().size(); slot++) {
            String holder = mission.getCrewSlotIds().get(slot);
            if ((holder != null) != usedCrew.contains(slot) || (holder != null && !astronautIds.contains(holder))) {
                throw new IllegalStateException("Mission " + mission.getId() + " crew slot " + slot + ": " + holder);
            }
        }
        for (int slot = 0; slot < configuration.controlSlots().size(); slot++) {
            String holder = mission.getControlSlotIds().get(slot);
            Set<String> roster = configuration.controlSlots().get(slot).kind() == StaffingSlot.Kind.ASTRONAUT
                ? astronautIds : controllerIds;
            if ((holder != null) != usedControl.contains(slot) || (holder != null && !roster.contains(holder))) {
                throw new IllegalStateException("Mission " + mission.getId() + " control slot " + slot + " ("
                    + configuration.controlSlots().get(slot).title() + "): " + holder);
            }
        }
    }

    private static boolean available(Personnel person) {
        return person.getAssignmentState() == AssignmentState.IDLE && person.getUnavailableSeasons() == 0;
    }

    private static double averageSkill(Personnel person) {
        ToDoubleFunction<Personnel> mean = p -> switch (p) {
            case Astronaut a -> java.util.Arrays.stream(AstronautSkill.values()).mapToDouble(a::getSkill).average()
                .orElse(0);
            case MissionControlOfficer m -> java.util.Arrays.stream(MissionControlSkill.values())
                .mapToDouble(m::getSkill).average().orElse(0);
            case SetSpecialist s -> java.util.Arrays.stream(SetSkill.values()).mapToDouble(s::getSkill).average()
                .orElse(0);
            default -> 0;
        };
        return mean.applyAsDouble(person);
    }

    private static int rosterSize(PlayerSnapshot snapshot, PersonnelGroup group) {
        return switch (group) {
            case ASTRONAUT -> snapshot.getAstronauts().size();
            case SET -> snapshot.getSetSpecialists().size();
            case MISSION_CONTROL -> snapshot.getMissionControlOfficers().size();
        };
    }

    private static List<Personnel> allEmployees(PlayerSnapshot snapshot) {
        List<Personnel> everyone = new ArrayList<>(snapshot.getAstronauts());
        everyone.addAll(snapshot.getSetSpecialists());
        everyone.addAll(snapshot.getMissionControlOfficers());
        return everyone;
    }

    /** Mutable totals across a batch. */
    /**
     * Prints how often R&amp;D decisions came: per game, in seasons with nothing launched (the quiet
     * ones) against the others, and how much of each game's longest quiet stretch they covered.
     */
    private static void decisionReport(List<List<boolean[]>> games) {
        double offers = 0;
        int quiet = 0;
        int quietOffered = 0;
        int busy = 0;
        int busyOffered = 0;
        List<Double> covered = new ArrayList<>();
        for (List<boolean[]> game : games) {
            int longestStart = 0;
            int longest = 0;
            int run = 0;
            for (int i = 0; i < game.size(); i++) {
                boolean offered = game.get(i)[0];
                boolean launched = game.get(i)[1];
                offers += offered ? 1 : 0;
                if (launched) {
                    busy++;
                    busyOffered += offered ? 1 : 0;
                    run = 0;
                } else {
                    quiet++;
                    quietOffered += offered ? 1 : 0;
                    run++;
                    if (run > longest) {
                        longest = run;
                        longestStart = i - run + 1;
                    }
                }
            }
            int inLongest = 0;
            for (int i = longestStart; i < longestStart + longest; i++) {
                inLongest += game.get(i)[0] ? 1 : 0;
            }
            covered.add(longest == 0 ? 0 : inLongest / (double) longest);
        }
        covered.sort(null);
        System.out.printf(java.util.Locale.ROOT, "R&D decisions per game: %.1f; offered in %.0f %% of quiet seasons "
                + "and %.0f %% of seasons with a launch; median game's longest quiet stretch covered %.0f %%%n",
            offers / games.size(), 100.0 * quietOffered / Math.max(1, quiet), 100.0 * busyOffered / Math.max(1, busy),
            100 * covered.get(covered.size() / 2));
    }

    /** The last run's tally, for {@link #main}'s random-event lines. */
    private Tally lastTally;

    /**
     * Returns the last run's contract outcomes by kind, with "APPLIED" for applications.
     *
     * @return the counts
     */
    public Map<String, Integer> contracts() {
        return lastTally.contracts;
    }

    /**
     * Returns the last run's contract profit, the fees less the launch costs, over every game.
     *
     * @return the profit
     */
    public long contractProfit() {
        return lastTally.contractProfit;
    }

    private static final class Tally {
        /** Contract outcomes by kind, and applications, over every game (design.md 2.11). */
        final Map<String, Integer> contracts = new java.util.TreeMap<>();
        /** The fees less the launch costs of every contract paid, over every game. */
        long contractProfit;
        /** Random events by kind, over every game. */
        final Map<String, Integer> randomEventKinds = new java.util.TreeMap<>();
        /** Per game: good events, bad events, and the seasons it ran (to completion or the cap). */
        final List<int[]> randomEventsPerGame = new ArrayList<>();
        /** Per game, per season: whether an R&D decision was offered, and whether something launched. */
        final List<List<boolean[]>> decisionSeasons = new ArrayList<>();
        final Map<Archetype, Integer> completedByArchetype = new EnumMap<>(Archetype.class);
        final Map<Archetype, Integer> gamesByArchetype = new EnumMap<>(Archetype.class);
        final List<Integer> completionSeasons = new ArrayList<>();
        final Map<Archetype, List<Integer>> completionSeasonsByArchetype = new EnumMap<>(Archetype.class);
        final Map<String, Integer> goalReached = new LinkedHashMap<>();
        final Map<String, Integer> launches = new LinkedHashMap<>();
        final Map<String, Integer> stepOutcomes = new LinkedHashMap<>();
        final Map<String, Integer> stalls = new LinkedHashMap<>();
        final Map<String, Integer> targetSeasons = new LinkedHashMap<>();
        int programsOpened;
        int rocketsOpened;
        int upgrades;
        int hires;
        int debtConversions;
        long lowestFunds = Long.MAX_VALUE;
        int astronautsLost;
        final List<Integer> launchGaps = new ArrayList<>();
        final List<Integer> longestGaps = new ArrayList<>();
        int launchedThisSeason;
        final Map<String, Integer> sideLaunches = new LinkedHashMap<>();
        final Map<String, Integer> sideKinds = new LinkedHashMap<>();
        final Map<String, Integer> sideRejections = new java.util.TreeMap<>();
    }

    /**
     * Plays 200 games on a pack and prints the report: 80 seasons each for the early race, 240 for the
     * Moon race.
     *
     * @param args the pack's folder under {@code assets/content}, {@code early-era} if none is given, then
     *             {@code ahead} for the bot that works ahead or {@code side} for the one that also flies
     *             side missions
     */
    public static void main(String[] args) {
        ContentCatalog catalog = ContentPackLoader.load(Path.of("assets", "content",
            args.length > 0 ? args[0] : "early-era"));
        Bot bot = args.length < 2 ? Bot.PLAIN : switch (args[1]) {
            case "ahead" -> Bot.AHEAD;
            case "side" -> Bot.SIDE_MISSIONS;
            default -> throw new IllegalArgumentException("Unknown bot " + args[1]);
        };
        // "decisions" as a third argument plays with the R&D decisions (ui-spec.md component 26); without
        // it there are none, so the earlier numbers stay comparable.
        boolean withDecisions = args.length > 2 && args[2].equals("decisions");
        if (withDecisions) {
            de.mkoehler.launchwindow.content.Decisions.setStandard(de.mkoehler.launchwindow.content.Decisions.load(
                Path.of("assets", "content", de.mkoehler.launchwindow.content.Decisions.FILE)));
        }
        ContentPackPlayouts harness = new ContentPackPlayouts(catalog, bot);
        int seasons = harness.declaredGoals().equals(MOON_DECLARED_GOALS) ? 240 : 80;
        Report report = harness.run(200, seasons, 42L);
        System.out.println(report.games() + " games, " + report.seasons() + " seasons max, declared goals "
            + harness.declaredGoals());
        System.out.println("Completed (all declared goals): " + report.completedByArchetype() + " of "
            + report.gamesByArchetype() + ", average completion season "
            + String.format("%.1f", report.averageCompletionSeason()));
        report.completionSeasons().forEach((archetype, list) -> {
            List<Integer> sorted = list.stream().sorted().toList();
            System.out.printf("  %s completion season: min %d, quartiles %d / %d / %d, max %d%n", archetype,
                sorted.getFirst(), sorted.get(sorted.size() / 4), sorted.get(sorted.size() / 2),
                sorted.get(sorted.size() * 3 / 4), sorted.getLast());
        });
        System.out.println("Declared goals reached: " + report.goalReachedCounts());
        System.out.println("Launches: " + report.launches());
        System.out.println("Step outcomes: " + report.stepOutcomes());
        System.out.println("Programs opened " + report.programsOpened() + ", rockets opened " + report.rocketsOpened()
            + ", upgrades " + report.upgrades() + ", hires " + report.hires() + ", debt conversions "
            + report.debtConversions() + ", lowest funds " + report.lowestFunds() + ", astronauts lost "
            + report.astronautsLost());
        System.out.println("Stalls: " + report.stalls());
        List<int[]> perGame = harness.lastTally.randomEventsPerGame;
        System.out.printf(java.util.Locale.ROOT, "Random events per game: %.1f good, %.1f bad over %.1f seasons; "
                + "most in one game %d good, %d bad%n",
            perGame.stream().mapToInt(game -> game[0]).average().orElse(0),
            perGame.stream().mapToInt(game -> game[1]).average().orElse(0),
            perGame.stream().mapToInt(game -> game[2]).average().orElse(0),
            perGame.stream().mapToInt(game -> game[0]).max().orElse(0),
            perGame.stream().mapToInt(game -> game[1]).max().orElse(0));
        System.out.println("Random events by kind: " + harness.lastTally.randomEventKinds);
        if (withDecisions) {
            decisionReport(harness.lastTally.decisionSeasons);
            System.out.println("Hands-on campaigns played, by result: " + harness.handsOnResults());
        }
        System.out.print("Seasons as the target, per game of its agency:");
        report.targetSeasons().forEach((id, count) -> System.out.printf(java.util.Locale.ROOT, " %s %.1f,", id,
            count / (report.games() / 2.0)));
        System.out.println();
        PeopleReport people = new ContentPackPlayouts(catalog).people(200, 24, 42L);
        System.out.printf("People over 24 seasons, per game: %.1f traits revealed, %.1f relationships shown "
                + "(%.1f friendships), %.1f pairs who shared a team, %.1f employees at the end%n",
            people.traitsRevealed() / (double) people.games(), people.relationshipsShown() / (double) people.games(),
            people.positive() / (double) people.games(), people.pairs() / (double) people.games(),
            people.employees() / (double) people.games());
        ContentPackPlayouts careers = new ContentPackPlayouts(catalog);
        careers.people(200, 24, 42L);
        System.out.println("Ambitions over 24 seasons, all games: " + careers.ambitionTally());
        List<int[]> end = careers.endOfGamePeople();
        System.out.printf("At season 24: morale average %.1f (lowest %d), loyalty average %.1f (lowest %d, highest %d)%n",
            end.stream().mapToInt(pair -> pair[0]).average().orElse(0), end.stream().mapToInt(pair -> pair[0]).min()
                .orElse(0), end.stream().mapToInt(pair -> pair[1]).average().orElse(0),
            end.stream().mapToInt(pair -> pair[1]).min().orElse(0), end.stream().mapToInt(pair -> pair[1]).max()
                .orElse(0));
    }
}
