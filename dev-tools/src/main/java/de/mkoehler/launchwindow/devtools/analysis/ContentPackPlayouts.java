package de.mkoehler.launchwindow.devtools.analysis;

import de.mkoehler.launchwindow.content.BudgetSchedule;
import de.mkoehler.launchwindow.content.BuildingSchedule;
import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.ContentPackLoader;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.content.MissionConfiguration;
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
import de.mkoehler.launchwindow.rules.BudgetProgress;
import de.mkoehler.launchwindow.rules.BuildingUpgradeOutcome;
import de.mkoehler.launchwindow.rules.BuildingUpgrades;
import de.mkoehler.launchwindow.rules.GoalTally;
import de.mkoehler.launchwindow.rules.HireOutcome;
import de.mkoehler.launchwindow.rules.HiringPool;
import de.mkoehler.launchwindow.rules.LaunchRequest;
import de.mkoehler.launchwindow.rules.MissionLaunch;
import de.mkoehler.launchwindow.rules.MissionLaunchOutcome;
import de.mkoehler.launchwindow.rules.MissionProgress;
import de.mkoehler.launchwindow.rules.MissionStepResolved;
import de.mkoehler.launchwindow.rules.OpenProgramOutcome;
import de.mkoehler.launchwindow.rules.PersonnelDeparture;
import de.mkoehler.launchwindow.rules.PersonnelGroup;
import de.mkoehler.launchwindow.rules.PersonnelLifecycle;
import de.mkoehler.launchwindow.rules.PrestigeConversion;
import de.mkoehler.launchwindow.rules.ProgramOpening;
import de.mkoehler.launchwindow.rules.RandomEvents;
import de.mkoehler.launchwindow.rules.ResearchProgress;
import de.mkoehler.launchwindow.rules.ResolutionEvent;
import de.mkoehler.launchwindow.rules.RocketProgramOutcome;
import de.mkoehler.launchwindow.rules.RocketPrograms;
import de.mkoehler.launchwindow.rules.SeasonFundsSettled;
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

    /** The goals a game declares - the race of the early era. */
    public static final List<String> DECLARED_GOALS =
        List.of("earth-orbit-satellite", "human-in-space", "human-in-orbit");

    /** Each agency's plan, in order; the bot flies the first one with a goal still open. */
    static final Map<Archetype, List<String>> PLANS = Map.of(
        Archetype.SOVIET, List.of("sputnik-1-orbit", "vostok-uncrewed-suborbital", "vostok-suborbital",
            "vostok-uncrewed-orbital", "vostok-orbital"),
        Archetype.NASA, List.of("explorer-1-orbit", "mercury-uncrewed-suborbital", "mercury-suborbital",
            "mercury-uncrewed-orbital", "mercury-orbital"));

    /** The bot launches once every component it flies is at least this reliable (or at its R&D ceiling). */
    static final double LAUNCH_RELIABILITY = 70;

    private static final int STARTING_ASTRONAUTS = 3;
    private static final int STARTING_CONTROLLERS = 5;
    private static final int STARTING_SPECIALISTS = 6;

    private final ContentCatalog catalog;
    private final BudgetSchedule schedule = BudgetSchedule.baspmClose();
    private final RandomEventRoster roster = RandomEventRoster.baspmClose();
    private final BuildingSchedule buildings;

    /**
     * Creates a harness for one content pack.
     *
     * @param catalog the pack
     */
    public ContentPackPlayouts(ContentCatalog catalog) {
        this.catalog = catalog;
        this.buildings = catalog.buildings();
    }

    /**
     * The result of a batch of games.
     *
     * @param games                  games played
     * @param seasons                seasons per game
     * @param completedByArchetype   games that reached every declared goal, per agency
     * @param gamesByArchetype       games played, per agency
     * @param averageCompletionSeason the average season the last declared goal fell, over completed games
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
     */
    public record Report(int games, int seasons, Map<Archetype, Integer> completedByArchetype,
                         Map<Archetype, Integer> gamesByArchetype, double averageCompletionSeason,
                         Map<String, Integer> goalReachedCounts, Map<String, Integer> launches,
                         Map<String, Integer> stepOutcomes, int programsOpened, int rocketsOpened, int upgrades,
                         int hires, int debtConversions, long lowestFunds, int astronautsLost,
                         Map<String, Integer> stalls) {
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
    public Report run(int games, int seasons, long seed) {
        Random random = new Random(seed);
        Tally tally = new Tally();
        for (int game = 0; game < games; game++) {
            Archetype archetype = game % 2 == 0 ? Archetype.NASA : Archetype.SOVIET;
            tally.gamesByArchetype.merge(archetype, 1, Integer::sum);
            playGame(archetype, seasons, random, tally);
        }
        double averageCompletion = tally.completionSeasons.stream().mapToInt(Integer::intValue).average().orElse(0);
        return new Report(games, seasons, tally.completedByArchetype, tally.gamesByArchetype, averageCompletion,
            tally.goalReached, tally.launches, tally.stepOutcomes, tally.programsOpened, tally.rocketsOpened,
            tally.upgrades, tally.hires, tally.debtConversions, tally.lowestFunds, tally.astronautsLost, tally.stalls);
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

        for (int season = 0; season < seasons; season++) {
            // Journaled player actions first.
            act(snapshot, random, tally);
            checkState(snapshot);

            List<ResolutionEvent> lifecycle = PersonnelLifecycle.advanceSeason(snapshot);
            List<ResolutionEvent> missions = MissionProgress.advanceSeason(snapshot, catalog, random, goalTally,
                season);
            goalTally.commit(missions);
            BuildingUpgrades.advanceSeason(snapshot, buildings);
            long expectedExpenses = independentRunningCosts(snapshot);
            long fundsBefore = snapshot.getFunds();
            List<ResolutionEvent> budget = BudgetProgress.advanceSeason(snapshot, catalog, schedule, season + 1);
            checkBudget(budget, fundsBefore, expectedExpenses, snapshot);
            List<ResolutionEvent> events = RandomEvents.advanceSeason(snapshot, roster, random, season, season + 1);
            TrainingProgress.advanceSeason(snapshot, random);
            ResearchProgress.advanceSeason(snapshot, catalog, random);
            HiringPool.refreshCandidates(snapshot, random);
            checkState(snapshot);

            for (ResolutionEvent event : missions) {
                if (event instanceof MissionStepResolved resolved) {
                    tally.stepOutcomes.merge(resolved.outcome().name(), 1, Integer::sum);
                }
            }
            for (List<ResolutionEvent> list : List.of(lifecycle, missions, events)) {
                for (ResolutionEvent event : list) {
                    if (event instanceof PersonnelDeparture departure && departure.group() == PersonnelGroup.ASTRONAUT
                            && (departure.reason().name().equals("LOST_IN_ACCIDENT")
                                || departure.reason().name().equals("PERISHED"))) {
                        tally.astronautsLost++;
                    }
                }
            }
            tally.lowestFunds = Math.min(tally.lowestFunds, snapshot.getFunds());
            for (String goal : DECLARED_GOALS) {
                if (snapshot.hasAchievedOrCancelledGoal(goal) && reached.add(goal)) {
                    tally.goalReached.merge(goal, 1, Integer::sum);
                }
            }
            // The harness's own completion check must agree with the win logic M4 will call.
            WinCondition.Outcome win = WinCondition.evaluate(Map.of("bot", snapshot), DECLARED_GOALS, catalog);
            boolean complete = reached.size() == DECLARED_GOALS.size();
            if (complete != (win.status() == WinCondition.Status.WON && win.winners().equals(List.of("bot")))) {
                throw new IllegalStateException("Completion " + complete + " disagrees with WinCondition " + win);
            }
            if (complete) {
                tally.completedByArchetype.merge(archetype, 1, Integer::sum);
                tally.completionSeasons.add(season + 1);
                return;
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
        Program program = catalog.program(target.programId()).orElseThrow();
        if (!snapshot.getOpenedProgramIds().contains(program.id())) {
            openProgram(snapshot, program, tally);
        }
        MissionComponent rocket = cheapestRocketFor(snapshot.getArchetype(), target);
        if (rocket != null && !isActive(snapshot, rocket.id())) {
            openRocket(snapshot, rocket, tally);
        }
        ensureStaff(snapshot, target, random, tally);
        assignResearch(snapshot, target, rocket);
        boolean hardwareReady = rocket != null && hardwareReady(snapshot, target, rocket);
        // Staff train while the hardware is still in R&D, and stay home once it's ready to fly.
        if (!hardwareReady) {
            trainIdle(snapshot, random);
            tally.stalls.merge("hardware not ready", 1, Integer::sum);
        } else if (snapshot.getInProgressMissions().values().stream()
                .noneMatch(mission -> mission.getMissionConfigurationId().equals(target.id()))) {
            launch(snapshot, target, rocket, tally);
        }
    }

    /** The first configuration in the agency's plan that still has a goal of its own to reach. */
    private MissionConfiguration target(PlayerSnapshot snapshot) {
        for (String id : PLANS.get(snapshot.getArchetype())) {
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
        int controllers = target.requiredMissionControlOfficerCount();
        int astronauts = target.requiredAstronautCount() + target.requiredCapcomCount();
        maybeUpgrade(snapshot, Building.MISSION_CONTROL, controllers, tally);
        maybeUpgrade(snapshot, Building.ASTRONAUT_CENTER, astronauts + 1, tally);
        hireBest(snapshot, PersonnelGroup.MISSION_CONTROL,
            controllers - snapshot.getMissionControlOfficers().size(), random, tally);
        hireBest(snapshot, PersonnelGroup.ASTRONAUT, astronauts + 1 - snapshot.getAstronauts().size(), random, tally);
        hireBest(snapshot, PersonnelGroup.SET, STARTING_SPECIALISTS - snapshot.getSetSpecialists().size(), random,
            tally);
    }

    private void maybeUpgrade(PlayerSnapshot snapshot, Building building, int needed, Tally tally) {
        if (needed <= BuildingUpgrades.capacity(snapshot, building, buildings)
                || snapshot.getBuildingUpgradeSeasonsRemaining().containsKey(building)) {
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
    private void assignResearch(PlayerSnapshot snapshot, MissionConfiguration target, MissionComponent rocket) {
        List<String> components = new ArrayList<>(catalog.program(target.programId()).orElseThrow()
            .requiredMissionComponentIds());
        if (rocket != null && isActive(snapshot, rocket.id())) {
            components.add(rocket.id());
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
        if (!isActive(snapshot, rocket.id())) {
            return false;
        }
        List<String> flown = new ArrayList<>(catalog.program(target.programId()).orElseThrow()
            .requiredMissionComponentIds());
        flown.add(rocket.id());
        for (String componentId : flown) {
            OwnedMissionComponent owned = snapshot.getMissionComponents().get(componentId);
            MissionComponent component = catalog.missionComponent(componentId).orElseThrow();
            if (owned == null || owned.getCurrentReliability()
                    < Math.min(LAUNCH_RELIABILITY, component.maxRdReliability() - 0.5)) {
                return false;
            }
        }
        return true;
    }

    private void launch(PlayerSnapshot snapshot, MissionConfiguration target, MissionComponent rocket, Tally tally) {
        Set<String> crewSkills = new HashSet<>();
        target.seasons().forEach(season -> season.steps().forEach(step ->
            step.crewInvolvements().forEach(involvement -> crewSkills.add(involvement.skill().name()))));
        List<Astronaut> idleAstronauts = snapshot.getAstronauts().stream().filter(ContentPackPlayouts::available)
            .sorted(Comparator.comparingDouble((Astronaut a) -> -meanOf(a, crewSkills))).toList();
        if (idleAstronauts.size() < target.requiredAstronautCount() + target.requiredCapcomCount()) {
            tally.stalls.merge("too few idle astronauts", 1, Integer::sum);
            return;
        }
        List<String> crew = idleAstronauts.subList(0, target.requiredAstronautCount()).stream()
            .map(Personnel::getId).toList();
        List<String> capcom = idleAstronauts.subList(target.requiredAstronautCount(),
                target.requiredAstronautCount() + target.requiredCapcomCount()).stream()
            .map(Personnel::getId).toList();
        List<MissionControlOfficer> idleControllers = snapshot.getMissionControlOfficers().stream()
            .filter(ContentPackPlayouts::available)
            .sorted(Comparator.comparingDouble(officer -> -averageSkill(officer))).toList();
        if (idleControllers.size() < target.requiredMissionControlOfficerCount()) {
            tally.stalls.merge("too few idle controllers", 1, Integer::sum);
            return;
        }
        List<String> controllers = idleControllers.subList(0, target.requiredMissionControlOfficerCount()).stream()
            .map(Personnel::getId).toList();
        List<String> rockets = target.launches().stream().map(launch -> rocket.id()).toList();
        long expectedCost = target.cost() + rockets.size() * rocket.unitCost();
        if (snapshot.getFunds() < expectedCost) {
            tally.stalls.merge("funds", 1, Integer::sum);
            return;
        }
        long before = snapshot.getFunds();
        MissionLaunchOutcome outcome = MissionLaunch.launch(snapshot,
            new LaunchRequest(target.id(), crew, controllers, capcom, rockets), catalog);
        long charged = before - snapshot.getFunds();
        if (charged != (outcome == MissionLaunchOutcome.LAUNCHED ? expectedCost : 0)) {
            throw new IllegalStateException("Launching " + target.id() + " (" + outcome + ") charged " + charged
                + ", expected " + expectedCost);
        }
        if (outcome == MissionLaunchOutcome.LAUNCHED) {
            tally.launches.merge(target.id(), 1, Integer::sum);
        } else {
            tally.stalls.merge(outcome.name(), 1, Integer::sum);
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
            if (mission.getRocketIds().size() != configuration.launches().size()
                    || mission.getAssignedAstronautIds().size() != configuration.requiredAstronautCount()
                    || mission.getCapcomAstronautIds().size() != configuration.requiredCapcomCount()
                    || mission.getAssignedMissionControlOfficerIds().size()
                        != configuration.requiredMissionControlOfficerCount()) {
                throw new IllegalStateException("Mission " + mission.getId() + " counts disagree with its configuration");
            }
            for (int i = 0; i < mission.getRocketIds().size(); i++) {
                MissionComponent rocket = catalog.missionComponent(mission.getRocketIds().get(i)).orElseThrow();
                MissionConfiguration.Launch launch = configuration.launches().get(i);
                double mass = launch.payloadComponentIds().stream()
                    .mapToDouble(id -> catalog.missionComponent(id).orElseThrow().mass()).sum();
                double capacity = rocket.rocket() == null ? 0 : switch (launch.launchType()) {
                    case SUBORBITAL, SUBORBITAL_HR -> rocket.rocket().payloadToSuborbital();
                    case LEO, LEO_HR -> rocket.rocket().payloadToLeo();
                    case TLI, TLI_HR -> rocket.rocket().payloadToTli();
                    case TPI, TPI_HR -> rocket.rocket().payloadToTpi();
                };
                if (launch.launchType().humanRated() && rocket.associatedSkill() != SetSkill.HUMAN_RATED_ROCKETS) {
                    capacity = 0;
                }
                if (capacity <= 0 || capacity < mass) {
                    throw new IllegalStateException("Mission " + mission.getId() + " flies " + rocket.id()
                        + ", which can't lift " + mass + " to " + launch.launchType());
                }
            }
            List<String> everyone = new ArrayList<>(mission.getAssignedAstronautIds());
            everyone.addAll(mission.getCapcomAstronautIds());
            everyone.addAll(mission.getAssignedMissionControlOfficerIds());
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
            boolean liftsAll = configuration.launches().stream().allMatch(launch -> {
                double mass = launch.payloadComponentIds().stream()
                    .mapToDouble(p -> catalog.missionComponent(p).orElseThrow().mass()).sum();
                double capacity = rocket.payloadCapacity(launch.launchType());
                return capacity > 0 && capacity >= mass;
            });
            if (liftsAll && (best == null || rocket.unitCost() < best.unitCost())) {
                best = rocket;
            }
        }
        return best;
    }

    private List<String> rocketIds() {
        List<String> ids = new ArrayList<>();
        for (String id : List.of("r7-sputnik", "r7-vostok", "jupiter-c", "redstone", "atlas")) {
            if (catalog.missionComponent(id).isPresent()) {
                ids.add(id);
            }
        }
        return ids;
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

    private static boolean available(Personnel person) {
        return person.getAssignmentState() == AssignmentState.IDLE && person.getUnavailableSeasons() == 0;
    }

    private static double meanOf(Astronaut astronaut, Set<String> skills) {
        if (skills.isEmpty()) {
            return averageSkill(astronaut);
        }
        return skills.stream().mapToDouble(skill -> astronaut.getSkill(AstronautSkill.valueOf(skill))).average()
            .orElse(0);
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
    private static final class Tally {
        final Map<Archetype, Integer> completedByArchetype = new EnumMap<>(Archetype.class);
        final Map<Archetype, Integer> gamesByArchetype = new EnumMap<>(Archetype.class);
        final List<Integer> completionSeasons = new ArrayList<>();
        final Map<String, Integer> goalReached = new LinkedHashMap<>();
        final Map<String, Integer> launches = new LinkedHashMap<>();
        final Map<String, Integer> stepOutcomes = new LinkedHashMap<>();
        final Map<String, Integer> stalls = new LinkedHashMap<>();
        int programsOpened;
        int rocketsOpened;
        int upgrades;
        int hires;
        int debtConversions;
        long lowestFunds = Long.MAX_VALUE;
        int astronautsLost;
    }

    /**
     * Plays 200 games of 80 seasons on the early-era pack and prints the report.
     *
     * @param args ignored
     */
    public static void main(String[] args) {
        ContentCatalog catalog = ContentPackLoader.load(Path.of("assets", "content", "early-era"));
        Report report = new ContentPackPlayouts(catalog).run(200, 80, 42L);
        System.out.println(report.games() + " games, " + report.seasons() + " seasons max");
        System.out.println("Completed (all declared goals): " + report.completedByArchetype() + " of "
            + report.gamesByArchetype() + ", average completion season "
            + String.format("%.1f", report.averageCompletionSeason()));
        System.out.println("Declared goals reached: " + report.goalReachedCounts());
        System.out.println("Launches: " + report.launches());
        System.out.println("Step outcomes: " + report.stepOutcomes());
        System.out.println("Programs opened " + report.programsOpened() + ", rockets opened " + report.rocketsOpened()
            + ", upgrades " + report.upgrades() + ", hires " + report.hires() + ", debt conversions "
            + report.debtConversions() + ", lowest funds " + report.lowestFunds() + ", astronauts lost "
            + report.astronautsLost());
        System.out.println("Stalls: " + report.stalls());
    }
}
