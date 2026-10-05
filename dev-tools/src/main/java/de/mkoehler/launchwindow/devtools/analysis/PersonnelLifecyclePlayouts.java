package de.mkoehler.launchwindow.devtools.analysis;

import java.util.Iterator;
import de.mkoehler.launchwindow.rules.LaunchRequest;
import de.mkoehler.launchwindow.content.StaffingSlot;
import de.mkoehler.launchwindow.content.BudgetSchedule;
import de.mkoehler.launchwindow.content.BuildingSchedule;
import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.Goal;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.content.MissionSeason;
import de.mkoehler.launchwindow.content.MissionStep;
import de.mkoehler.launchwindow.content.Program;
import de.mkoehler.launchwindow.content.ProgramCategory;
import de.mkoehler.launchwindow.content.RandomEventRoster;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.AstronautSkill;
import de.mkoehler.launchwindow.model.Building;
import de.mkoehler.launchwindow.model.FlightTier;
import de.mkoehler.launchwindow.model.RandomEventKind;
import de.mkoehler.launchwindow.model.InProgressMission;
import de.mkoehler.launchwindow.model.MissionControlOfficer;
import de.mkoehler.launchwindow.model.MissionControlSkill;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.Personnel;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSkill;
import de.mkoehler.launchwindow.model.SetSpecialist;
import de.mkoehler.launchwindow.model.TrainingKind;
import de.mkoehler.launchwindow.rules.DepartureReason;
import de.mkoehler.launchwindow.rules.ExpertRescueApplied;
import de.mkoehler.launchwindow.rules.ExpertTrainingCompleted;
import de.mkoehler.launchwindow.rules.HireOutcome;
import de.mkoehler.launchwindow.rules.HiringPool;
import de.mkoehler.launchwindow.rules.MissionLaunch;
import de.mkoehler.launchwindow.rules.MissionLaunchOutcome;
import de.mkoehler.launchwindow.rules.MissionProgress;
import de.mkoehler.launchwindow.rules.MissionReliabilityChanged;
import de.mkoehler.launchwindow.rules.MissionSkillGrowthApplied;
import de.mkoehler.launchwindow.rules.MissionStepOutcome;
import de.mkoehler.launchwindow.rules.MissionStepResolution;
import de.mkoehler.launchwindow.rules.BudgetProgress;
import de.mkoehler.launchwindow.rules.BudgetReviewed;
import de.mkoehler.launchwindow.rules.BuildingUpgradeOutcome;
import de.mkoehler.launchwindow.rules.BuildingUpgraded;
import de.mkoehler.launchwindow.rules.BuildingUpgrades;
import de.mkoehler.launchwindow.rules.CloseProgramOutcome;
import de.mkoehler.launchwindow.rules.GoalSettled;
import de.mkoehler.launchwindow.rules.PrestigeConversion;
import de.mkoehler.launchwindow.rules.RandomEventOccurred;
import de.mkoehler.launchwindow.rules.RandomEvents;
import de.mkoehler.launchwindow.rules.SeasonFundsSettled;
import de.mkoehler.launchwindow.rules.GoalTally;
import de.mkoehler.launchwindow.rules.MissionPrestige;
import de.mkoehler.launchwindow.rules.MissionPrestigeChanged;
import de.mkoehler.launchwindow.rules.MissionStepResolved;
import de.mkoehler.launchwindow.rules.OpenProgramOutcome;
import de.mkoehler.launchwindow.rules.PersonnelDeparture;
import de.mkoehler.launchwindow.rules.PersonnelGroup;
import de.mkoehler.launchwindow.rules.PersonnelLifecycle;
import de.mkoehler.launchwindow.rules.ProgramOpening;
import de.mkoehler.launchwindow.rules.PullOutOfTrainingOutcome;
import de.mkoehler.launchwindow.rules.ResearchCompleted;
import de.mkoehler.launchwindow.rules.ResearchComponentMaxed;
import de.mkoehler.launchwindow.rules.ResearchProgress;
import de.mkoehler.launchwindow.rules.ResolutionEvent;
import de.mkoehler.launchwindow.rules.SkillGrowth;
import de.mkoehler.launchwindow.rules.StartExpertTrainingOutcome;
import de.mkoehler.launchwindow.rules.TrainingCompleted;
import de.mkoehler.launchwindow.rules.TrainingProgress;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * <p>Launch Window's own {@code Playouts}-style bot-driven harness (see
 * {@code design.md} &sect;3.10, following Robot-Rampage's own precedent) -
 * drives {@link PersonnelLifecycle#advanceSeason}, {@link TrainingProgress#advanceSeason},
 * {@link HiringPool#refreshCandidates}/{@link HiringPool#hire}, and
 * {@link ProgramOpening#open}, the rules-engine resolvers that exist so
 * far, across many random simulated rosters and seasons, purely headless
 * (no client/server/persistence involved), and reports the tenure/
 * departure/training/hiring/program statistics that make it a tuning tool
 * rather than just another unit test.</p>
 *
 * <p><b>Deliberately narrow, named after the resolvers it drives</b> - this
 * is not yet the general full-game {@code Playouts} harness
 * {@code design.md} &sect;3.10 describes (that needs a real bot making
 * per-season decisions like "queue a mission," which doesn't exist yet).
 * Expected to grow alongside M2's remaining slices ({@code design.md}
 * &sect;5), most likely folding into a more general harness once there are
 * resolvers for those too, rather than staying its own permanent,
 * resolver-specific class.</p>
 *
 * <p><b>{@link #CATALOG} is a small hand-built fixture, not real content</b>
 * (see {@code design.md} &sect;5's M2 status - "against an in-memory/
 * test-fixture content set"; real, authored content is M3's job): three
 * programs and four mission components, with two reliability-transfer
 * relationships and one opening-cost discount, enough to exercise
 * {@link ProgramOpening}'s main paths without building a fake tech tree
 * bigger than this harness actually needs. Only one of the two transfer
 * relationships can ever actually produce a nonzero bonus - see
 * {@link #buildCatalog()}'s own Javadoc for why the other one is a
 * deliberate illustration of {@link ProgramOpening}'s same-call isolation
 * rule, not a bug.</p>
 *
 * <p>The "bot" here is intentionally simple, per employee per season
 * (chance {@link #REASSIGNMENT_CHANCE_PER_SEASON}): idle either stays idle
 * or starts a real training assignment - a random target skill from that
 * employee's own group, and evenly between
 * {@link TrainingKind#ADVANCED}/{@link TrainingKind#PERMANENT} (the last
 * with a random target level strictly above the current skill and up to
 * {@link SkillGrowth#SKILL_CAP}, per {@code design.md} &sect;3.4's proposed
 * invariant). <b>An employee already in training is no longer always run
 * to completion</b> (2026-10-02) - a small per-season chance
 * ({@link #PULL_OUT_OF_TRAINING_CHANCE_PER_SEASON}) pulls them out early
 * via the general {@link TrainingProgress#pullOutOfTraining} action
 * instead, the first exercise of that path in this harness; otherwise
 * training still runs to its own completion (for {@code PERMANENT}, until
 * it stops re-enrolling on its own). Idle Astronauts/Mission Control
 * officers also have a smaller chance
 * ({@link #EXPERT_TRAINING_START_CHANCE_PER_SEASON}) of starting
 * {@link TrainingKind#EXPERT} training instead, when eligible - see
 * {@link #shuffleAstronaut}/{@link #shuffleMissionControlOfficer}'s own
 * Javadoc. <b>An Astronaut/Mission Control officer's idle-to-{@code ASSIGNED}
 * transition no longer happens here</b> (2026-10-02) - it is exclusively
 * {@link #maybeLaunchMission}'s job now, since {@code ASSIGNED} means
 * "on a specific {@link de.mkoehler.launchwindow.model.InProgressMission}"
 * and an individual employee shuffle has no mission to point at; the
 * reverse transition is exclusively {@link MissionProgress#advanceSeason}'s
 * job, when that mission ends. Not a stand-in for a real per-turn player
 * decision, which doesn't exist yet at this milestone.</p>
 *
 * <p><b>Resolver composition order matches the contract {@link HiringPool}'s,
 * {@link ProgramOpening}'s, and {@link MissionLaunch}'s own Javadoc
 * documents</b>: each season, the bot may first hire out of <i>last</i>
 * season's candidate pool ({@link #HIRE_CHANCE_PER_SEASON} per group),
 * open one program ({@link #OPEN_PROGRAM_CHANCE_PER_SEASON}), and launch
 * one mission ({@link #LAUNCH_MISSION_CHANCE_PER_SEASON}) - all three
 * stand-ins for a journaled player action - then {@link PersonnelLifecycle}
 * runs, then {@link MissionProgress} (right after, so a crew lost or
 * released this season is already reflected before the resolvers below
 * run), then {@link TrainingProgress} and {@link ResearchProgress} (same
 * position, since both only ever touch someone already
 * {@link AssignmentState#IN_TRAINING}/{@link AssignmentState#ASSIGNED}
 * going into this season, never someone {@link PersonnelLifecycle}/
 * {@link MissionProgress} just removed or released), then
 * {@link HiringPool#refreshCandidates} last - all sharing the same seeded
 * {@link Random}, so a whole game's outcome stays reproducible from a
 * single seed.</p>
 *
 * @author Mario Koehler
 */
public final class PersonnelLifecyclePlayouts {

    private static final int MIN_START_AGE = 24;
    private static final int MAX_START_AGE = 45;
    private static final int MIN_START_MORALE = 50;
    private static final int MAX_START_MORALE = 90;
    private static final int MIN_START_LEARNING_POTENTIAL = 20;
    private static final int MAX_START_LEARNING_POTENTIAL = 80;
    private static final double MIN_START_SKILL = 20.0;
    private static final double MAX_START_SKILL = 90.0;
    private static final double REASSIGNMENT_CHANCE_PER_SEASON = 0.3;
    private static final double HIRE_CHANCE_PER_SEASON = 0.25;

    /** The bot's per-season chance of starting one building upgrade (journaled-action bucket). */
    private static final double BUILDING_UPGRADE_CHANCE = 0.05;

    /** The bot's per-season chance of closing one open program (journaled-action bucket). */
    private static final double CLOSE_PROGRAM_CHANCE_PER_SEASON = 0.02;
    private static final double OPEN_PROGRAM_CHANCE_PER_SEASON = 0.1;
    private static final double LAUNCH_MISSION_CHANCE_PER_SEASON = 0.15;
    /**
     * The starting economy, close to BASPM's own Normal-difficulty values (the user's
     * instruction of 2026-10-03: stay close to BASPM's values for defaults). Launch
     * Window's own review table is BASPM's shape at BASPM's magnitudes, rounded
     * where BASPM's are not already round - {@code design.md} &sect;2.1(d).
     */
    private static final long STARTING_FUNDS = 25_000;
    private static final long STARTING_PRESTIGE = 250;
    private static final long STARTING_FUNDING_RATE = 3_000;
    private static final long PRESTIGE_PER_FUND = 4;
    private static final RandomEventRoster ROSTER = RandomEventRoster.baspmClose();
    private static final BudgetSchedule SCHEDULE = BudgetSchedule.baspmClose();


    /**
     * For an idle Astronaut/Mission Control officer whose shuffle roll
     * already cleared {@link #REASSIGNMENT_CHANCE_PER_SEASON}, the chance
     * - checked <em>before</em> the shuffle's own 50/50 idle-vs-training
     * split, not after it - that the bot starts {@link TrainingKind#EXPERT}
     * training instead of ever reaching that split at all. Only ever
     * rolled when at least one of their skills already clears
     * {@link SkillGrowth#EXPERT_TRAINING_SKILL_THRESHOLD} and isn't
     * already an Expert skill (see {@link #eligibleExpertAstronautSkills}/
     * {@link #eligibleExpertMissionControlSkills}).
     */
    private static final double EXPERT_TRAINING_START_CHANCE_PER_SEASON = 0.2;

    /**
     * The chance, per currently-{@code IN_TRAINING} employee per season
     * (any {@link TrainingKind}, any of the three groups), that the bot
     * pulls them out early via {@link TrainingProgress#pullOutOfTraining} -
     * deliberately low, since this is meant to exercise the general
     * pull-out path at all, not to make training routinely abandoned.
     */
    private static final double PULL_OUT_OF_TRAINING_CHANCE_PER_SEASON = 0.03;

    /**
     * Every program id {@link #CATALOG} defines, matching
     * {@link #buildCatalog()} exactly - {@link #maybeOpenProgram} picks
     * among these rather than enumerating {@link ContentCatalog} itself,
     * which exposes lookup by id but not "every id of a given type."
     */
    private static final List<String> PROGRAM_IDS =
        List.of("sputnik-program", "explorer-program", "mercury-program");

    /**
     * Every mission configuration id {@link #CATALOG} defines, matching
     * {@link #buildCatalog()} exactly - {@link #maybeLaunchMission} picks
     * among these, the same reason {@link #PROGRAM_IDS} exists.
     */
    private static final List<String> MISSION_CONFIGURATION_IDS =
        List.of("sputnik-launch-std", "explorer-mission-std", "mercury-mission-std");

    /**
     * The fixture catalog's shared R&amp;D slot-band ceilings, re-deriving
     * {@code baspm.md} &sect;10.8's per-component {@code RDLevelMaxAmount
     * L1}-{@code L4} shape with Launch Window's own numbers - every fixture
     * component uses the same bands, since this harness isn't tuning R&amp;D
     * balance, only exercising the multi-slot mechanic.
     */
    private static final List<Double> RD_SLOT_BANDS = List.of(3.0, 6.0, 9.0, 12.0);

    /**
     * The fixture content catalog every simulated game opens programs
     * against - see this class's own Javadoc for why it's small and not
     * real content.
     */
    private static final ContentCatalog CATALOG = buildCatalog();

    static {
        // Guards PROGRAM_IDS against silently drifting out of sync with buildCatalog(): if a
        // program were added to the catalog without adding its id here, the bot would simply
        // never pick it, with nothing failing loudly to say so.
        for (String programId : PROGRAM_IDS) {
            if (CATALOG.program(programId).isEmpty()) {
                throw new ExceptionInInitializerError("PROGRAM_IDS has an id not in CATALOG: " + programId);
            }
        }
        // Same guard, for MISSION_CONFIGURATION_IDS against buildCatalog().
        for (String missionConfigurationId : MISSION_CONFIGURATION_IDS) {
            if (CATALOG.missionConfiguration(missionConfigurationId).isEmpty()) {
                throw new ExceptionInInitializerError(
                    "MISSION_CONFIGURATION_IDS has an id not in CATALOG: " + missionConfigurationId);
            }
        }
    }

    /**
     * Not instantiable; this class only exposes static methods.
     */
    private PersonnelLifecyclePlayouts() {
    }

    /**
     * Builds {@link #CATALOG}: one program category, four mission
     * components, three programs (one carrying an opening-cost discount
     * from another), and three mission configurations, one per program -
     * original structure, not reproducing any real BASPM mission's own
     * season/step layout, per {@code design.md} &sect;1.2.
     *
     * <p>Two components declare a reliability-transfer source, but only
     * one of those can ever actually fire: {@code satellite-explorer}'s
     * transfer from {@code satellite-sputnik} fires whenever
     * {@code sputnik-program} was opened in an <em>earlier</em> call than
     * {@code explorer-program}, since by then {@code satellite-sputnik} is
     * already owned. {@code capsule-mercury}'s transfer from
     * {@code rocket-redstone} can <b>never</b> fire, since
     * {@code mercury-program} grants both components in the very same
     * {@link ProgramOpening#open} call, and {@link ProgramOpening}
     * computes every transfer within one call against ownership as it
     * stood before that call - a deliberate illustration of that rule
     * inside this fixture, not a bug to fix by reshaping the catalog.</p>
     *
     * <p>The three mission configurations exercise every major
     * {@link MissionProgress} code path: {@code sputnik-launch-std}
     * (one season, uncrewed, controller-involved) and
     * {@code explorer-mission-std} (two seasons, uncrewed, controller-
     * involved - the multi-season continuation path) cover an uncrewed
     * mission with and without a saving throw worth anything;
     * {@code mercury-mission-std} (two seasons, crewed, every step flagged
     * crew-risk) is the only one that can ever produce a
     * {@code CATASTROPHIC} crew loss. Each also declares its own
     * {@link MissionConfiguration#cost()} (50/75/150 respectively,
     * ascending with crew/duration) - deliberately small next to the
     * programs' own opening costs (500/600/1,200), since flying a mission
     * should cost much less than building the hardware it flies, not
     * balance-tuned numbers.</p>
     *
     * @return the fixture catalog
     */
    private static ContentCatalog buildCatalog() {
        ProgramCategory category = new ProgramCategory("early-space", "Early Space");

        // Program openings, maintenance, and mission costs are BASPM's own values for the programs and
        // components these fixtures stand for (Sputnik 00-00, Explorer 00-02, crewed 00-05 approximately;
        // a mission costs the unit cost of the components it flies). Component upkeep close to BASPM's own MaintenanceCost values (Sputnik 75, Explorer I 90,
        // Redstone 188, Mercury 158) - user's 2026-10-03 instruction to stay close for defaults.
        MissionComponent satelliteSputnik = new MissionComponent("satellite-sputnik", SetSkill.SPACE_PROBES, 3, 90,
            82, RD_SLOT_BANDS, 0, List.of(), 75);
        MissionComponent satelliteExplorer = new MissionComponent("satellite-explorer", SetSkill.SPACE_PROBES, 3, 90,
            82, RD_SLOT_BANDS, 40, List.of(new MissionComponent.ReliabilityTransferSource("satellite-sputnik", 40)), 90);
        MissionComponent rocketRedstone = new MissionComponent("rocket-redstone", SetSkill.ROCKETS, 3, 95, 87,
            RD_SLOT_BANDS, 0, List.of(), 188);
        MissionComponent capsuleMercury = new MissionComponent("capsule-mercury", SetSkill.CREWED_SPACECRAFT, 3, 90,
            82, RD_SLOT_BANDS, 50, List.of(new MissionComponent.ReliabilityTransferSource("rocket-redstone", 50)), 158);

        Program sputnikProgram =
            new Program("sputnik-program", "early-space", 126, 13, List.of("satellite-sputnik"), List.of());
        Program explorerProgram = new Program("explorer-program", "early-space", 354, 36,
            List.of("satellite-explorer"), List.of(new Program.OpeningCostDiscount("sputnik-program", 40)),
            List.of("satellite-in-orbit"));
        Program mercuryProgram = new Program("mercury-program", "early-space", 449, 45,
            List.of("rocket-redstone", "capsule-mercury"), List.of());

        MissionStep sputnikLaunchStep = new MissionStep("launch",
            List.of(new MissionStep.ComponentInvolvement("satellite-sputnik", 100)),
            List.of(new MissionStep.ControllerInvolvement(0, 60),
                new MissionStep.ControllerInvolvement(1, 40)),
            List.of(), false, 100, 100, List.of("satellite-in-orbit"));
        MissionConfiguration sputnikLaunchConfiguration = new MissionConfiguration("sputnik-launch-std",
            "sputnik-program", List.of(new MissionSeason(List.of(sputnikLaunchStep))), 0, 2, 488, 250);

        MissionStep explorerLaunchStep = new MissionStep("launch",
            List.of(new MissionStep.ComponentInvolvement("satellite-explorer", 100)),
            List.of(new MissionStep.ControllerInvolvement(0, 50),
                new MissionStep.ControllerInvolvement(4, 50)),
            List.of(), false, 40, 80, List.of());
        MissionStep explorerOrbitOpsStep = new MissionStep("orbit-ops",
            List.of(new MissionStep.ComponentInvolvement("satellite-explorer", 100)),
            List.of(new MissionStep.ControllerInvolvement(4, 100)),
            List.of(), false, 60, 60, List.of());
        MissionConfiguration explorerMissionConfiguration = new MissionConfiguration("explorer-mission-std",
            "explorer-program",
            List.of(new MissionSeason(List.of(explorerLaunchStep)), new MissionSeason(List.of(explorerOrbitOpsStep))),
            0, 5, 585, 500);

        MissionStep mercuryLaunchStep = new MissionStep("launch",
            List.of(new MissionStep.ComponentInvolvement("rocket-redstone", 60),
                new MissionStep.ComponentInvolvement("capsule-mercury", 40)),
            List.of(new MissionStep.ControllerInvolvement(0, 50),
                new MissionStep.ControllerInvolvement(1, 50)),
            List.of(new MissionStep.CrewInvolvement(0, AstronautSkill.PILOTING, 35),
                new MissionStep.CrewInvolvement(0, AstronautSkill.FITNESS, 15),
                new MissionStep.CrewInvolvement(1, AstronautSkill.PILOTING, 35),
                new MissionStep.CrewInvolvement(1, AstronautSkill.FITNESS, 15)), true, 50, 200, List.of("crewed-training"));
        MissionStep mercuryOrbitOpsStep = new MissionStep("orbit-ops",
            List.of(new MissionStep.ComponentInvolvement("capsule-mercury", 100)),
            List.of(new MissionStep.ControllerInvolvement(2, 100)),
            List.of(new MissionStep.CrewInvolvement(0, AstronautSkill.SCIENCE, 50),
                new MissionStep.CrewInvolvement(1, AstronautSkill.SCIENCE, 50)), true, 50, 150, List.of("crewed-flight"));
        MissionConfiguration mercuryMissionConfiguration = new MissionConfiguration("mercury-mission-std",
            "mercury-program",
            List.of(new MissionSeason(List.of(mercuryLaunchStep)), new MissionSeason(List.of(mercuryOrbitOpsStep))),
            2, 3, 2_243, 1_000, List.of(new MissionConfiguration.GoalModifier("satellite-in-orbit", "capsule-mercury", 5)),
            List.of());

        return new ContentCatalog(List.of(category), List.of(sputnikProgram, explorerProgram, mercuryProgram),
            List.of(sputnikLaunchConfiguration, explorerMissionConfiguration, mercuryMissionConfiguration),
            List.of(satelliteSputnik, satelliteExplorer, rocketRedstone, capsuleMercury), List.of(
                new Goal("satellite-in-orbit", 300, 150, List.of()),
                new Goal("crewed-flight", 800, 400, List.of("crewed-training")),
                new Goal("crewed-training", 200, 100, List.of())));
    }

    /**
     * Runs {@code games} independent simulated games, each with a freshly
     * random roster of {@code employeesPerGroupPerGame} astronauts, SET
     * specialists, and Mission Control officers, advanced for up to
     * {@code seasons} seasons (a game ends early if every roster empties
     * out first).
     *
     * @param games                   how many independent games to simulate; must be positive
     * @param employeesPerGroupPerGame how many employees each of the three rosters starts
     *                                with per game; must be positive
     * @param seasons                 the season cap per game; must be positive
     * @param seed                    the random seed - the same seed always produces the
     *                                same report
     * @return the summary report
     * @throws IllegalArgumentException if any count is not positive
     */
    public static PersonnelPlayoutReport run(int games, int employeesPerGroupPerGame, int seasons, long seed) {
        if (games <= 0 || employeesPerGroupPerGame <= 0 || seasons <= 0) {
            throw new IllegalArgumentException(
                "games, employeesPerGroupPerGame and seasons must all be positive");
        }
        Random random = new Random(seed);
        int resignedCount = 0;
        int retiredCount = 0;
        long resignedTenureTotal = 0;
        long retiredTenureTotal = 0;
        int employeesStarted = 0;
        int trainingCompletedCount = 0;
        double trainingGainTotal = 0;
        int hiredCount = 0;
        int programsOpenedCount = 0;
        int researchCompletedCount = 0;
        double researchReliabilityGainTotal = 0;
        int missionsLaunchedCount = 0;
        int missionStepSuccessfulCount = 0;
        int missionStepMinorGlitchSolvedCount = 0;
        int missionStepMinorGlitchCount = 0;
        int reviewCount = 0;
        int debtConversionCount = 0;
        int dismissedByEventCount = 0;
        Map<RandomEventKind, Integer> randomEventCounts = new EnumMap<>(RandomEventKind.class);
        long prestigePaidTotal = 0;
        long prestigeDeductedTotal = 0;
        int completedFlightCount = 0;
        int missionStepFailureCount = 0;
        int missionStepCatastrophicCount = 0;
        int lostInAccidentCount = 0;
        int retiredOnMissionReleaseCount = 0;
        int missionReliabilityChangeCount = 0;
        double missionReliabilityDeltaTotal = 0;
        int missionSkillGrowthCount = 0;
        double missionSkillGrowthTotal = 0;
        int missionTriggeredSetReleaseCount = 0;
        int expertTrainingStartedCount = 0;
        int expertTrainingCompletedCount = 0;
        int pulledOutOfTrainingCount = 0;
        int expertRescueAppliedCount = 0;
        int[] bandHits = new int[4];
        int buildingUpgradesStartedCount = 0;
        int programsClosedCount = 0;
        int buildingUpgradesCompletedCount = 0;

        for (int game = 0; game < games; game++) {
            PlayerSnapshot snapshot = newRandomSnapshot(employeesPerGroupPerGame, random);
            GoalTally goalTally = new GoalTally();
            employeesStarted += rosterSize(snapshot);
            Map<String, Integer> joinedAtSeason = new HashMap<>();
            seedJoinedAtSeason(snapshot, joinedAtSeason);

            for (int season = 0; season < seasons && rosterSize(snapshot) > 0; season++) {
                // index 0: Expert training starts this season; index 1: pull-outs this season.
                int[] expertTally = new int[2];
                shuffleAssignments(snapshot, random, expertTally);
                expertTrainingStartedCount += expertTally[0];
                pulledOutOfTrainingCount += expertTally[1];
                hiredCount += hireFromPools(snapshot, random, season, joinedAtSeason);
                programsOpenedCount += maybeOpenProgram(snapshot, random);
                programsClosedCount += maybeCloseProgram(snapshot, random);
                missionsLaunchedCount += maybeLaunchMission(snapshot, random);
                buildingUpgradesStartedCount += maybeUpgradeBuilding(snapshot, random);
                checkProgramInvariants(snapshot);
                // A player in debt may convert prestige into funds, before any resolver runs (journaled bucket).
                debtConversionCount += maybeConvertPrestige(snapshot, random);

                int before = rosterSize(snapshot);
                Set<String> protectedBefore = protectedEmployeeKeys(snapshot);

                List<ResolutionEvent> lifecycleEvents = PersonnelLifecycle.advanceSeason(snapshot);
                checkInvariants(snapshot, lifecycleEvents, before, protectedBefore);

                int beforeMissionProgress = rosterSize(snapshot);
                long prestigeBefore = snapshot.getPrestige();
                // Launch order, the same order MissionProgress resolves them in.
                Map<String, String> configByMission = new LinkedHashMap<>();
                for (InProgressMission flying : snapshot.getInProgressMissions().values()) {
                    configByMission.put(flying.getId(), flying.getMissionConfigurationId());
                }
                Map<String, Integer> countsBefore = new HashMap<>(snapshot.getSuccessfulFlightCounts());
                List<ResolutionEvent> missionEvents = MissionProgress.advanceSeason(snapshot, CATALOG, random, goalTally, season);
                checkMissionInvariants(snapshot, missionEvents, beforeMissionProgress);
                long[] prestigeFlows = checkPrestigeInvariants(snapshot, missionEvents, prestigeBefore, goalTally,
                    configByMission, countsBefore);
                checkGoalState(snapshot);
                prestigePaidTotal += prestigeFlows[0];
                prestigeDeductedTotal += prestigeFlows[1];
                completedFlightCount += (int) prestigeFlows[2];
                // The tally moves only after the whole resolution has been checked (design.md §2.6(d)).
                goalTally.commit(missionEvents);
                // Building upgrades finish before the budget pass, so a new level's upkeep is charged the season it lands.
                Map<Building, Integer> levelsBefore = new EnumMap<>(Building.class);
                for (Building building : Building.values()) {
                    levelsBefore.put(building, snapshot.buildingLevel(building));
                }
                List<ResolutionEvent> upgradeEvents = BuildingUpgrades.advanceSeason(snapshot, CATALOG.buildings());
                checkBuildingInvariants(snapshot, upgradeEvents, levelsBefore);
                buildingUpgradesCompletedCount += upgradeEvents.size();
                // The budget season pass, after mission progress so this season's prestige can feed a review.
                long expectedExpenses = independentRunningCosts(snapshot);
                long fundsBefore = snapshot.getFunds();
                long prestigeAtLastReviewBefore = snapshot.getPrestigeAtLastReview();
                List<ResolutionEvent> budgetEvents = BudgetProgress.advanceSeason(snapshot, CATALOG, SCHEDULE,
                    season + 1);
                checkBudgetInvariants(snapshot, budgetEvents, fundsBefore, expectedExpenses, prestigeAtLastReviewBefore,
                    season + 1);
                if (budgetEvents.stream().anyMatch(event -> event instanceof BudgetReviewed)) {
                    reviewCount++;
                }
                checkProgramInvariants(snapshot);

                // Random events, after the budget pass. Their departures go through the same accounting as
                // lifecycle departures: resignations count as resignations, perished astronauts as losses.
                int rosterBeforeEvents = rosterSize(snapshot);
                int newsBeforeEvents = snapshot.getNewsLog().size();
                List<ResolutionEvent> randomEventEvents = RandomEvents.advanceSeason(snapshot, ROSTER, random,
                    season, season + 1);
                checkRandomEventInvariants(snapshot, randomEventEvents, rosterBeforeEvents, newsBeforeEvents);
                for (ResolutionEvent event : randomEventEvents) {
                    if (event instanceof RandomEventOccurred occurred) {
                        randomEventCounts.merge(occurred.kind(), 1, Integer::sum);
                    } else if (event instanceof PersonnelDeparture departure) {
                        String key = employeeKey(departure.group(), departure.personnelId());
                        Integer joinedAt = joinedAtSeason.remove(key);
                        if (joinedAt == null) {
                            throw new IllegalStateException("Event departure with no recorded join season: " + key);
                        }
                        int tenureSeasons = season + 1 - joinedAt;
                        switch (departure.reason()) {
                            case RESIGNED -> {
                                resignedCount++;
                                resignedTenureTotal += tenureSeasons;
                            }
                            case PERISHED, LOST_IN_ACCIDENT -> lostInAccidentCount++;
                            case DISMISSED, FAILED_BASIC_TRAINING -> dismissedByEventCount++;
                            case RETIRED -> {
                                retiredCount++;
                                retiredTenureTotal += tenureSeasons;
                            }
                        }
                    }
                }

                List<ResolutionEvent> trainingEvents = TrainingProgress.advanceSeason(snapshot, random);
                List<ResolutionEvent> researchEvents = ResearchProgress.advanceSeason(snapshot, CATALOG, random);
                checkTrainingInvariants(snapshot, trainingEvents);
                checkResearchInvariants(snapshot);

                for (ResolutionEvent event : lifecycleEvents) {
                    PersonnelDeparture departure = (PersonnelDeparture) event;
                    String key = employeeKey(departure.group(), departure.personnelId());
                    Integer joinedAt = joinedAtSeason.remove(key);
                    if (joinedAt == null) {
                        throw new IllegalStateException("Departure for an employee with no recorded join season: " + key);
                    }
                    int tenureSeasons = season + 1 - joinedAt;
                    if (departure.reason() == DepartureReason.RESIGNED) {
                        resignedCount++;
                        resignedTenureTotal += tenureSeasons;
                    } else {
                        retiredCount++;
                        retiredTenureTotal += tenureSeasons;
                    }
                }
                for (ResolutionEvent event : missionEvents) {
                    if (event instanceof MissionStepResolved resolved) {
                        switch (resolved.outcome()) {
                            case SUCCESSFUL -> missionStepSuccessfulCount++;
                            case MINOR_GLITCH_SOLVED -> missionStepMinorGlitchSolvedCount++;
                            case MINOR_GLITCH -> missionStepMinorGlitchCount++;
                            case FAILURE -> missionStepFailureCount++;
                            case CATASTROPHIC -> missionStepCatastrophicCount++;
                        }
                    } else if (event instanceof PersonnelDeparture departure) {
                        String key = employeeKey(departure.group(), departure.personnelId());
                        Integer joinedAt = joinedAtSeason.remove(key);
                        if (joinedAt == null) {
                            throw new IllegalStateException(
                                "Departure for an employee with no recorded join season: " + key);
                        }
                        if (departure.reason() == DepartureReason.LOST_IN_ACCIDENT) {
                            lostInAccidentCount++;
                        } else {
                            // RETIRED, from MissionProgress's own retire-on-release check - folded into the
                            // same retiredCount/retiredTenureTotal PersonnelLifecycle's own RETIRED
                            // departures feed, plus its own dedicated counter for the harness's test.
                            retiredCount++;
                            retiredTenureTotal += season + 1 - joinedAt;
                            retiredOnMissionReleaseCount++;
                        }
                    } else if (event instanceof MissionReliabilityChanged changed) {
                        missionReliabilityChangeCount++;
                        missionReliabilityDeltaTotal += changed.delta();
                    } else if (event instanceof MissionSkillGrowthApplied growth) {
                        missionSkillGrowthCount++;
                        missionSkillGrowthTotal += growth.totalSkillGain();
                    } else if (event instanceof ResearchComponentMaxed maxed) {
                        // Mission-triggered release (the post-season reliability gain pushed a
                        // component to its R&D ceiling) - distinct from ResearchProgress's own
                        // R&D-pass release, which is deliberately not counted below. Counts every
                        // released specialist, not just the triggering event.
                        missionTriggeredSetReleaseCount += maxed.releasedPersonnelIds().size();
                    } else if (event instanceof ExpertRescueApplied rescued && rescued.boostPercent() > 0) {
                        // Only counts a rescue that actually contributed something - an eligible
                        // Expert already exhausted past the third use this turn still gets
                        // consulted (and emits an event), but at a 0% boost.
                        expertRescueAppliedCount++;
                    }
                }
                checkExpertRescueInvariants(missionEvents);
                for (ResolutionEvent event : trainingEvents) {
                    if (event instanceof TrainingCompleted completed) {
                        trainingCompletedCount++;
                        trainingGainTotal += completed.skillGain();
                    } else if (event instanceof ExpertTrainingCompleted) {
                        expertTrainingCompletedCount++;
                    }
                }
                for (ResolutionEvent event : researchEvents) {
                    // ResearchComponentMaxed (a component reaching its R&D ceiling, releasing its
                    // researchers - baspm.md §10.8) isn't counted here; only actual research seasons are.
                    if (event instanceof ResearchCompleted completed) {
                        researchCompletedCount++;
                        researchReliabilityGainTotal += completed.reliabilityGain();
                    }
                }

                HiringPool.refreshCandidates(snapshot, random);
                checkHiringInvariants(snapshot, bandHits);
            }
        }

        int stillEmployed = employeesStarted + hiredCount - resignedCount - retiredCount - lostInAccidentCount
            - dismissedByEventCount;
        double avgResignedTenure = resignedCount == 0 ? 0 : (double) resignedTenureTotal / resignedCount;
        double avgRetiredTenure = retiredCount == 0 ? 0 : (double) retiredTenureTotal / retiredCount;
        double avgTrainingGain = trainingCompletedCount == 0 ? 0 : trainingGainTotal / trainingCompletedCount;
        double avgResearchReliabilityGain =
            researchCompletedCount == 0 ? 0 : researchReliabilityGainTotal / researchCompletedCount;
        double avgMissionReliabilityDelta =
            missionReliabilityChangeCount == 0 ? 0 : missionReliabilityDeltaTotal / missionReliabilityChangeCount;
        double avgMissionSkillGrowth =
            missionSkillGrowthCount == 0 ? 0 : missionSkillGrowthTotal / missionSkillGrowthCount;

        return new PersonnelPlayoutReport(games, seasons, employeesStarted, resignedCount, retiredCount,
            stillEmployed, avgResignedTenure, avgRetiredTenure, trainingCompletedCount, avgTrainingGain,
            hiredCount, bandHits[0], bandHits[1], bandHits[2], bandHits[3], programsOpenedCount,
            researchCompletedCount, avgResearchReliabilityGain, missionsLaunchedCount, missionStepSuccessfulCount,
            missionStepMinorGlitchSolvedCount, missionStepMinorGlitchCount, missionStepFailureCount,
            missionStepCatastrophicCount, lostInAccidentCount, retiredOnMissionReleaseCount,
            missionReliabilityChangeCount, avgMissionReliabilityDelta, missionSkillGrowthCount,
            avgMissionSkillGrowth, missionTriggeredSetReleaseCount, expertTrainingStartedCount,
            expertTrainingCompletedCount, pulledOutOfTrainingCount, expertRescueAppliedCount,
            prestigePaidTotal, prestigeDeductedTotal, completedFlightCount, reviewCount, debtConversionCount,
            dismissedByEventCount, randomEventCounts, buildingUpgradesStartedCount, buildingUpgradesCompletedCount,
            programsClosedCount);
    }

    /**
     * The bot's stand-in for a journaled "hire" player action: for each
     * group, with probability {@link #HIRE_CHANCE_PER_SEASON}, hires one
     * random candidate out of that group's <i>current</i> pool (i.e. the one
     * last season's {@link HiringPool#refreshCandidates} produced) - a
     * no-op if that pool is currently empty (season 0, before any refresh
     * has happened yet) or the roster is already at capacity, both of which
     * {@link HiringPool#hire} itself reports via {@link HireOutcome} rather
     * than throwing. On a successful hire, records the join season in
     * {@code joinedAtSeason} so tenure can be computed correctly later -
     * a hire mid-game must not be credited with tenure back to season 0.
     *
     * @param snapshot        the snapshot to hire into; mutated in place
     * @param random          the random source to draw from
     * @param season          the current season index, recorded as the new
     *                        hire's join season
     * @param joinedAtSeason  this game's {@code group:id -> join season} map;
     *                        mutated in place on a successful hire
     * @return how many of the three groups actually hired this season
     */
    private static int hireFromPools(PlayerSnapshot snapshot, Random random, int season,
            Map<String, Integer> joinedAtSeason) {
        int hired = 0;
        hired += maybeHire(snapshot, PersonnelGroup.ASTRONAUT, snapshot.getAstronautCandidates(), random, season,
            joinedAtSeason);
        hired += maybeHire(snapshot, PersonnelGroup.SET, snapshot.getSetSpecialistCandidates(), random, season,
            joinedAtSeason);
        hired += maybeHire(snapshot, PersonnelGroup.MISSION_CONTROL, snapshot.getMissionControlCandidates(), random,
            season, joinedAtSeason);
        return hired;
    }

    private static int maybeHire(PlayerSnapshot snapshot, PersonnelGroup group, List<? extends Personnel> pool,
            Random random, int season, Map<String, Integer> joinedAtSeason) {
        if (pool.isEmpty() || random.nextDouble() >= HIRE_CHANCE_PER_SEASON) {
            return 0;
        }
        String candidateId = pool.get(random.nextInt(pool.size())).getId();
        int rosterBefore = rosterSize(snapshot, group);
        HireOutcome outcome = HiringPool.hire(snapshot, group, candidateId, random, CATALOG.buildings());
        // The roster cap, read straight off the level table: a hire may only land below it, and a full
        // roster must be refused.
        int cap = independentCap(switch (group) {
            case ASTRONAUT -> Building.ASTRONAUT_CENTER;
            case SET -> Building.SET_CENTER;
            case MISSION_CONTROL -> Building.MISSION_CONTROL;
        }, snapshot);
        if ((outcome == HireOutcome.HIRED) != (rosterBefore < cap)) {
            throw new IllegalStateException(group + " hire " + outcome + " with roster " + rosterBefore + " and cap "
                + cap);
        }
        if (outcome != HireOutcome.HIRED) {
            return 0;
        }
        joinedAtSeason.put(employeeKey(group, candidateId), season);
        return 1;
    }

    /**
     * The bot's stand-in for a journaled "open program" player action: with
     * probability {@link #OPEN_PROGRAM_CHANCE_PER_SEASON}, picks one random
     * not-yet-opened program from {@link #CATALOG} and opens it. A no-op if
     * every program is already open, or if {@link ProgramOpening#open}
     * reports {@link OpenProgramOutcome#INSUFFICIENT_FUNDS} - both ordinary
     * outcomes this harness lets happen rather than working around, the
     * same posture as {@link #maybeHire}.
     *
     * @param snapshot the snapshot to open a program for; mutated in place
     *                 on success
     * @param random   the random source to draw from
     * @return {@code 1} if a program was opened this call, {@code 0} otherwise
     */
    private static int maybeOpenProgram(PlayerSnapshot snapshot, Random random) {
        if (random.nextDouble() >= OPEN_PROGRAM_CHANCE_PER_SEASON) {
            return 0;
        }
        List<String> candidateProgramIds = new ArrayList<>();
        for (String programId : PROGRAM_IDS) {
            if (!snapshot.getOpenedProgramIds().contains(programId)) {
                candidateProgramIds.add(programId);
            }
        }
        if (candidateProgramIds.isEmpty()) {
            return 0;
        }
        String programId = candidateProgramIds.get(random.nextInt(candidateProgramIds.size()));
        // The price, recomputed independently: the discounted opening cost plus the creation cost of every
        // required component not owned yet (baspm.md §10.9).
        Program program = CATALOG.program(programId).orElseThrow();
        int discountPercent = 0;
        for (Program.OpeningCostDiscount discount : program.openingCostDiscounts()) {
            if (snapshot.getOpenedProgramIds().contains(discount.otherProgramId())) {
                discountPercent += discount.percent();
            }
        }
        long expectedPrice = program.openingCost() * (100 - Math.min(100, discountPercent)) / 100;
        for (String componentId : program.requiredMissionComponentIds()) {
            if (!snapshot.getMissionComponents().containsKey(componentId)) {
                expectedPrice += CATALOG.missionComponent(componentId).orElseThrow().creationCost();
            }
        }
        long fundsBefore = snapshot.getFunds();
        OpenProgramOutcome outcome = ProgramOpening.open(snapshot, programId, CATALOG);
        long charged = fundsBefore - snapshot.getFunds();
        if (charged != (outcome == OpenProgramOutcome.OPENED ? expectedPrice : 0)) {
            throw new IllegalStateException("Opening " + programId + " (" + outcome + ") charged " + charged
                + ", expected " + expectedPrice);
        }
        return outcome == OpenProgramOutcome.OPENED ? 1 : 0;
    }

    /**
     * The bot's stand-in for a journaled "close program" action: with a small chance per
     * season, closes one random open program. Refused while one of its missions flies.
     *
     * @param snapshot the player
     * @param random   the shared random source
     * @return 1 if a program closed, else 0
     */
    private static int maybeCloseProgram(PlayerSnapshot snapshot, Random random) {
        if (snapshot.getOpenedProgramIds().isEmpty() || random.nextDouble() >= CLOSE_PROGRAM_CHANCE_PER_SEASON) {
            return 0;
        }
        String programId = snapshot.getOpenedProgramIds().get(random.nextInt(snapshot.getOpenedProgramIds().size()));
        boolean flying = snapshot.getInProgressMissions().values().stream()
            .anyMatch(mission -> CATALOG.missionConfiguration(mission.getMissionConfigurationId()).orElseThrow()
                .programId().equals(programId));
        CloseProgramOutcome outcome = ProgramOpening.close(snapshot, programId, CATALOG);
        if ((outcome == CloseProgramOutcome.MISSIONS_IN_FLIGHT) != flying) {
            throw new IllegalStateException("Closing " + programId + " gave " + outcome + " with flying=" + flying);
        }
        return outcome == CloseProgramOutcome.CLOSED ? 1 : 0;
    }

    /**
     * The bot's stand-in for a journaled "launch mission" player action:
     * with probability {@link #LAUNCH_MISSION_CHANCE_PER_SEASON}, picks one
     * random mission configuration whose own program is currently open,
     * and attempts to launch it with one randomly-picked currently idle
     * person of the right type in each staffing slot its steps use.
     * A no-op (not an error) if no program is open yet, not enough idle
     * personnel exist to meet either exact count, the player's funds are
     * below the picked configuration's own {@link MissionConfiguration#cost()}
     * ({@link MissionLaunchOutcome#INSUFFICIENT_FUNDS} - this harness has no
     * income mechanic yet, so funds only ever fall over a game), or
     * {@link MissionLaunch#launch} itself reports anything else - the same
     * posture as {@link #maybeHire}/{@link #maybeOpenProgram}.
     *
     * @param snapshot the snapshot to launch a mission for; mutated in
     *                 place on success
     * @param random   the random source to draw from
     * @return {@code 1} if a mission was launched this call, {@code 0} otherwise
     */
    private static int maybeLaunchMission(PlayerSnapshot snapshot, Random random) {
        if (random.nextDouble() >= LAUNCH_MISSION_CHANCE_PER_SEASON) {
            return 0;
        }
        List<String> candidateConfigurationIds = new ArrayList<>();
        for (String configurationId : MISSION_CONFIGURATION_IDS) {
            MissionConfiguration configuration = CATALOG.missionConfiguration(configurationId).orElseThrow();
            if (snapshot.getOpenedProgramIds().contains(configuration.programId())) {
                candidateConfigurationIds.add(configurationId);
            }
        }
        if (candidateConfigurationIds.isEmpty()) {
            return 0;
        }
        String configurationId = candidateConfigurationIds.get(random.nextInt(candidateConfigurationIds.size()));
        MissionConfiguration configuration = CATALOG.missionConfiguration(configurationId).orElseThrow();

        List<String> astronautIds = pickExactlyIdleIds(snapshot.getAstronauts(),
            configuration.requiredAstronautCount() + configuration.requiredCapcomCount(), random);
        if (astronautIds == null) {
            return 0;
        }
        List<String> controllerIds = pickExactlyIdleIds(snapshot.getMissionControlOfficers(),
            configuration.requiredMissionControlOfficerCount(), random);
        if (controllerIds == null) {
            return 0;
        }

        // One picked person per used slot, in slot order; an unused slot stays empty.
        Iterator<String> astronauts = astronautIds.iterator();
        Iterator<String> controllers = controllerIds.iterator();
        List<String> crewSlotIds = new ArrayList<>();
        for (int slot = 0; slot < configuration.crewSlots().size(); slot++) {
            crewSlotIds.add(configuration.usedCrewSlots().contains(slot) ? astronauts.next() : null);
        }
        List<String> controlSlotIds = new ArrayList<>();
        for (int slot = 0; slot < configuration.controlSlots().size(); slot++) {
            boolean astronautSlot = configuration.controlSlots().get(slot).kind() == StaffingSlot.Kind.ASTRONAUT;
            controlSlotIds.add(!configuration.usedControlSlots().contains(slot) ? null
                : astronautSlot ? astronauts.next() : controllers.next());
        }

        MissionLaunchOutcome outcome = MissionLaunch.launch(snapshot,
            new LaunchRequest(configurationId, crewSlotIds, controlSlotIds, List.of()), CATALOG);
        return outcome == MissionLaunchOutcome.LAUNCHED ? 1 : 0;
    }

    /**
     * Picks exactly {@code count} random currently idle ids from a roster.
     *
     * @param roster the roster to pick from
     * @param count  exactly how many ids to pick
     * @param random the random source to draw from
     * @param <P>    the roster's own {@link Personnel} subtype
     * @return the picked ids, or {@code null} if fewer than {@code count}
     *     are currently idle
     */
    private static <P extends Personnel> List<String> pickExactlyIdleIds(List<P> roster, int count, Random random) {
        if (count == 0) {
            return List.of();
        }
        List<String> idleIds = new ArrayList<>();
        for (P person : roster) {
            if (person.getAssignmentState() == AssignmentState.IDLE && person.getUnavailableSeasons() == 0) {
                idleIds.add(person.getId());
            }
        }
        if (idleIds.size() < count) {
            return null;
        }
        Collections.shuffle(idleIds, random);
        return new ArrayList<>(idleIds.subList(0, count));
    }

    /**
     * Seeds {@code joinedAtSeason} with join season {@code 0} for every
     * employee already on the roster at game start, so {@link #maybeHire}'s
     * later entries and this initial batch are treated uniformly when
     * computing tenure on departure.
     *
     * @param snapshot        the freshly-created snapshot
     * @param joinedAtSeason  the map to seed; mutated in place
     */
    private static void seedJoinedAtSeason(PlayerSnapshot snapshot, Map<String, Integer> joinedAtSeason) {
        seedJoinedAtSeason(snapshot.getAstronauts(), PersonnelGroup.ASTRONAUT, joinedAtSeason);
        seedJoinedAtSeason(snapshot.getSetSpecialists(), PersonnelGroup.SET, joinedAtSeason);
        seedJoinedAtSeason(snapshot.getMissionControlOfficers(), PersonnelGroup.MISSION_CONTROL, joinedAtSeason);
    }

    private static void seedJoinedAtSeason(List<? extends Personnel> roster, PersonnelGroup group,
            Map<String, Integer> joinedAtSeason) {
        for (Personnel person : roster) {
            joinedAtSeason.put(employeeKey(group, person.getId()), 0);
        }
    }

    /**
     * Cross-checks {@link PersonnelLifecycle}'s own output for internal
     * consistency - age monotonicity is already covered exhaustively by
     * {@code PersonnelLifecycleTest}'s own unit tests, so this focuses on
     * what many random combinations can catch that a handful of hand-picked
     * unit tests might not: morale staying in bounds, every reported
     * departure actually matching the threshold it claims to, and that
     * nobody <i>protected</i> going into this season's advance is ever
     * among its departures - computed independently here
     * ({@link #protectedEmployeeKeys}), not by calling
     * {@code PersonnelLifecycle}'s own (package-private, and in a different
     * package) protection predicate, so this is a genuine cross-check of
     * the resolver's behavior rather than the same logic agreeing with
     * itself. See {@code design.md} &sect;2.2(c) for the actual per-group
     * protection rule this mirrors (SET never protected; Astronaut/Mission
     * Control protected only while {@link AssignmentState#ASSIGNED}) -
     * reversed from BASPM's own uniform "any busy employee" shape the same
     * session this harness was first built against.
     *
     * @param snapshot        the snapshot after this season's advance
     * @param events          the {@link PersonnelLifecycle} events this season's advance returned
     * @param rosterSizeBefore the combined roster size before this season's advance
     * @param protectedBefore the {@code group:id} keys of every employee who was
     *                        protected from departure immediately before this
     *                        season's advance ran
     * @throws IllegalStateException if any invariant is violated
     */
    private static void checkInvariants(PlayerSnapshot snapshot, List<ResolutionEvent> events, int rosterSizeBefore,
            Set<String> protectedBefore) {
        snapshot.getAstronauts().forEach(PersonnelLifecyclePlayouts::checkMoraleRange);
        snapshot.getSetSpecialists().forEach(PersonnelLifecyclePlayouts::checkMoraleRange);
        snapshot.getMissionControlOfficers().forEach(PersonnelLifecyclePlayouts::checkMoraleRange);

        for (ResolutionEvent event : events) {
            PersonnelDeparture departure = (PersonnelDeparture) event;
            if (departure.reason() == DepartureReason.RESIGNED
                    && departure.moraleAtExit() > PersonnelLifecycle.RESIGNATION_MORALE_FLOOR) {
                throw new IllegalStateException("Resigned employee " + departure.personnelId()
                    + " had morale above the resignation floor: " + departure.moraleAtExit());
            }
            if (departure.reason() == DepartureReason.RETIRED
                    && departure.ageAtExit() < retirementAgeFor(departure.group())) {
                throw new IllegalStateException("Retired employee " + departure.personnelId()
                    + " was younger than their group's retirement age: " + departure.ageAtExit());
            }
            String key = employeeKey(departure.group(), departure.personnelId());
            if (protectedBefore.contains(key)) {
                throw new IllegalStateException("Employee " + departure.personnelId()
                    + " departed despite being protected going into this season");
            }
        }

        int rosterSizeAfter = rosterSize(snapshot);
        if (rosterSizeBefore - events.size() != rosterSizeAfter) {
            throw new IllegalStateException(
                "Roster size change does not match the number of reported departures");
        }
    }

    /**
     * Cross-checks {@link TrainingProgress}'s own output and the resulting
     * snapshot state: every reported gain is non-negative, every skill
     * stays within {@code [0, SkillGrowth.SKILL_CAP]}, and
     * {@link AssignmentState#IN_TRAINING} agrees with a non-{@link TrainingKind#NONE}
     * training kind and a non-null target skill in both directions - the
     * same consistency {@link TrainingProgress#isActivelyTraining} itself
     * relies on, re-checked here across many random combinations rather
     * than trusted blindly. Also cross-checks the two Expert-specific
     * invariants (2026-10-02, see {@link #checkExpertTrainingEligibility}/
     * {@link #checkExpertSkillThreshold}): an in-progress
     * {@link TrainingKind#EXPERT} round's own target skill is eligible,
     * and every already-held Expert skill's own current value still
     * clears the threshold.
     *
     * @param snapshot       the snapshot after this season's training advance
     * @param trainingEvents the {@link TrainingProgress} events this season's advance returned
     * @throws IllegalStateException if any invariant is violated
     */
    private static void checkTrainingInvariants(PlayerSnapshot snapshot, List<ResolutionEvent> trainingEvents) {
        for (ResolutionEvent event : trainingEvents) {
            if (event instanceof TrainingCompleted completed && completed.skillGain() < 0) {
                throw new IllegalStateException("Negative training gain: " + completed.skillGain());
            }
        }
        for (Astronaut astronaut : snapshot.getAstronauts()) {
            checkTrainingConsistency(astronaut, astronaut.getTrainingTargetSkill());
            checkSkillsInRange(astronaut.getId(), astronaut.getSkills().values());
            if (astronaut.getTrainingKind() == TrainingKind.PERMANENT) {
                checkPermanentTrainingTarget(astronaut, astronaut.getSkill(astronaut.getTrainingTargetSkill()));
            }
            if (astronaut.getTrainingKind() == TrainingKind.EXPERT) {
                checkExpertTrainingEligibility(astronaut.getId(), astronaut.getSkill(astronaut.getTrainingTargetSkill()),
                    astronaut.getExpertSkills().contains(astronaut.getTrainingTargetSkill()));
            }
            for (AstronautSkill skill : astronaut.getExpertSkills()) {
                checkExpertSkillThreshold(astronaut.getId(), skill.name(), astronaut.getSkill(skill));
            }
        }
        for (SetSpecialist specialist : snapshot.getSetSpecialists()) {
            checkTrainingConsistency(specialist, specialist.getTrainingTargetSkill());
            checkSkillsInRange(specialist.getId(), specialist.getSkills().values());
            if (specialist.getTrainingKind() == TrainingKind.PERMANENT) {
                checkPermanentTrainingTarget(specialist, specialist.getSkill(specialist.getTrainingTargetSkill()));
            }
        }
        for (MissionControlOfficer officer : snapshot.getMissionControlOfficers()) {
            checkTrainingConsistency(officer, officer.getTrainingTargetSkill());
            checkSkillsInRange(officer.getId(), officer.getSkills().values());
            if (officer.getTrainingKind() == TrainingKind.PERMANENT) {
                checkPermanentTrainingTarget(officer, officer.getSkill(officer.getTrainingTargetSkill()));
            }
            if (officer.getTrainingKind() == TrainingKind.EXPERT) {
                checkExpertTrainingEligibility(officer.getId(), officer.getSkill(officer.getTrainingTargetSkill()),
                    officer.getExpertSkills().contains(officer.getTrainingTargetSkill()));
            }
            for (MissionControlSkill skill : officer.getExpertSkills()) {
                checkExpertSkillThreshold(officer.getId(), skill.name(), officer.getSkill(skill));
            }
        }
    }

    /**
     * Cross-checks the invariant {@link TrainingProgress#startAstronautExpertTraining}/
     * {@code startMissionControlExpertTraining} must enforce, re-checked
     * independently here across many random combinations: an employee
     * currently training {@link TrainingKind#EXPERT} must have a current
     * value in the targeted skill at or above
     * {@link SkillGrowth#EXPERT_TRAINING_SKILL_THRESHOLD}, and must not
     * already be an Expert in it - both conditions the start action itself
     * rejects, so finding either false here would mean the bot bypassed
     * that action (it never should - {@link #shuffleAstronaut}/
     * {@link #shuffleMissionControlOfficer} only ever call it directly).
     *
     * @param personnelId            the employee to check
     * @param currentSkillOfTarget   the targeted skill's current value
     * @param alreadyExpertInTarget  whether the employee already holds the
     *                               Expert trait in the targeted skill
     * @throws IllegalStateException if either condition is violated
     */
    private static void checkExpertTrainingEligibility(String personnelId, double currentSkillOfTarget,
            boolean alreadyExpertInTarget) {
        if (currentSkillOfTarget < SkillGrowth.EXPERT_TRAINING_SKILL_THRESHOLD) {
            throw new IllegalStateException("EXPERT training in progress for " + personnelId
                + " below the threshold: " + currentSkillOfTarget);
        }
        if (alreadyExpertInTarget) {
            throw new IllegalStateException(
                "EXPERT training in progress for " + personnelId + " targeting a skill already held");
        }
    }

    /**
     * Cross-checks that every currently-held Expert skill's own current
     * value is still at or above {@link SkillGrowth#EXPERT_TRAINING_SKILL_THRESHOLD} -
     * skills never decrease anywhere in this project, so this also proves
     * the threshold genuinely held at the moment the trait was granted,
     * not just right now.
     *
     * @param personnelId  the employee to check
     * @param skillName    the Expert skill's own {@code name()}
     * @param currentValue the skill's current value
     * @throws IllegalStateException if the value is below the threshold
     */
    private static void checkExpertSkillThreshold(String personnelId, String skillName, double currentValue) {
        if (currentValue < SkillGrowth.EXPERT_TRAINING_SKILL_THRESHOLD) {
            throw new IllegalStateException(
                "Expert skill " + skillName + " for " + personnelId + " is below the threshold: " + currentValue);
        }
    }

    /**
     * Cross-checks every {@link ExpertRescueApplied} event this season's
     * mission advance reported, computed independently of
     * {@link MissionProgress#applyExpertRescue}'s own internals: counting
     * each person's own earlier occurrences in this same event list
     * re-derives which use number this one must be, and the reported
     * {@code boostPercent} must match {@link MissionStepResolution#expertRescueBoostPercentForUse}
     * for that use number exactly, or that value scaled by
     * {@link MissionStepResolution#EXPERT_RESCUE_OFF_MISSION_CONTROLLER_EFFICIENCY}
     * (an off-mission Mission Control officer) - no other value is ever
     * valid. Also checks {@code probabilityAfter} never drops below
     * {@code probabilityBefore} and never reaches 100, the per-event
     * guarantee the geometric gap-closing formula provides (the separate
     * 95% cap is only ever applied once, to the final aggregated value, so
     * it is deliberately not re-checked per event here).
     *
     * @param missionEvents this season's {@link MissionProgress#advanceSeason} events
     * @throws IllegalStateException if any invariant is violated
     */
    private static void checkExpertRescueInvariants(List<ResolutionEvent> missionEvents) {
        Map<String, Integer> useCountSoFar = new HashMap<>();
        for (ResolutionEvent event : missionEvents) {
            if (!(event instanceof ExpertRescueApplied rescue)) {
                continue;
            }
            int useNumber = useCountSoFar.merge(rescue.personnelId(), 1, Integer::sum);
            double expectedBase = MissionStepResolution.expertRescueBoostPercentForUse(useNumber);
            double expectedScaled =
                expectedBase * MissionStepResolution.EXPERT_RESCUE_OFF_MISSION_CONTROLLER_EFFICIENCY;
            if (rescue.boostPercent() != expectedBase && rescue.boostPercent() != expectedScaled) {
                throw new IllegalStateException("Expert rescue boost percent for " + rescue.personnelId()
                    + " on use " + useNumber + " was " + rescue.boostPercent() + ", expected " + expectedBase
                    + " or " + expectedScaled);
            }
            if (rescue.probabilityAfter() < rescue.probabilityBefore() || rescue.probabilityAfter() >= 100.0) {
                throw new IllegalStateException("Expert rescue probability out of range for " + rescue.personnelId()
                    + ": before=" + rescue.probabilityBefore() + ", after=" + rescue.probabilityAfter());
            }
        }
    }

    /**
     * Cross-checks the invariants {@link #shuffleSetSpecialist} must uphold
     * for {@link ResearchProgress} to ever find anyone to advance: a
     * {@code SET} specialist is {@link AssignmentState#ASSIGNED} if and
     * only if their {@code rdTargetComponentId} is set to an id the
     * snapshot actually owns <em>and</em> their {@code rdSlot} is within
     * {@code [0, }{@value MissionComponent#RD_SLOT_COUNT}{@code )} - the
     * pairing {@link AssignmentState}'s own Javadoc documents, extended by
     * {@code baspm.md} &sect;10.8's own slot mechanic - and, independently,
     * that no two specialists ever claim the same (component, slot) pair,
     * computed from scratch here rather than reusing
     * {@link #firstFreeRdSlot}, so this check doesn't just confirm the bot's
     * own allocation logic agrees with itself.
     *
     * @param snapshot the snapshot to check, after this season's
     *                 {@link #shuffleAssignments} and
     *                 {@link PersonnelLifecycle#advanceSeason} calls
     * @throws IllegalStateException if any invariant is violated
     */
    private static void checkResearchInvariants(PlayerSnapshot snapshot) {
        Set<String> claimedSlots = new HashSet<>();
        for (SetSpecialist specialist : snapshot.getSetSpecialists()) {
            boolean assigned = specialist.getAssignmentState() == AssignmentState.ASSIGNED;
            String targetId = specialist.getRdTargetComponentId();
            Integer slot = specialist.getRdSlot();
            boolean hasValidSlot = slot != null && slot >= 0 && slot < MissionComponent.RD_SLOT_COUNT;
            boolean hasOwnedTarget = targetId != null && snapshot.getMissionComponents().containsKey(targetId);
            if (assigned != (hasOwnedTarget && hasValidSlot)) {
                throw new IllegalStateException("ASSIGNED/rdTargetComponentId/rdSlot pairing broken for "
                    + specialist.getId() + ": assigned=" + assigned + ", targetId=" + targetId + ", slot=" + slot);
            }
            if (assigned) {
                String claim = targetId + "#" + slot;
                if (!claimedSlots.add(claim)) {
                    throw new IllegalStateException("Two SET specialists claim the same R&D slot: " + claim);
                }
            }
        }
    }

    /**
     * Cross-checks {@link MissionProgress#advanceSeason}'s own output,
     * computed independently of its internals: the roster-size change
     * matches the reported departures exactly, every
     * {@code LOST_IN_ACCIDENT} departure is astronaut-only, every
     * {@code RETIRED} departure (either group - the retire-on-release
     * check) is actually at or above that group's own retirement age, no
     * {@code RESIGNED} departure is ever reported (not a mechanic
     * {@link MissionProgress} has), every {@link MissionSkillGrowthApplied}
     * event's own gain is non-negative, and
     * {@link #checkMissionAssignmentConsistency} holds.
     * {@code MissionReliabilityChanged}/{@code ResearchComponentMaxed}'s own
     * bounds aren't re-checked here - the caller runs
     * {@link #checkProgramInvariants}/{@link #checkResearchInvariants}
     * later the same season regardless (including the game's last
     * season, so a mission's final-season reliability change is never
     * left unchecked), so those cover it instead.
     *
     * @param snapshot                    the snapshot after this season's
     *                                     mission advance
     * @param missionEvents                the events
     *                                     {@link MissionProgress#advanceSeason}
     *                                     returned this season
     * @param rosterSizeBeforeMissionProgress the combined roster size
     *                                     immediately before this season's
     *                                     mission advance ran
     * @throws IllegalStateException if any invariant is violated
     */
    private static void checkMissionInvariants(PlayerSnapshot snapshot, List<ResolutionEvent> missionEvents,
            int rosterSizeBeforeMissionProgress) {
        int departureCount = 0;
        for (ResolutionEvent event : missionEvents) {
            if (event instanceof PersonnelDeparture departure) {
                switch (departure.reason()) {
                    case LOST_IN_ACCIDENT -> {
                        if (departure.group() != PersonnelGroup.ASTRONAUT) {
                            throw new IllegalStateException(
                                "MissionProgress reported a non-astronaut LOST_IN_ACCIDENT departure: "
                                    + departure.group());
                        }
                    }
                    case RETIRED -> {
                        if (departure.ageAtExit() < retirementAgeFor(departure.group())) {
                            throw new IllegalStateException("MissionProgress retired " + departure.personnelId()
                                + " younger than their group's retirement age: " + departure.ageAtExit());
                        }
                    }
                    case RESIGNED -> throw new IllegalStateException(
                        "MissionProgress reported an unexpected RESIGNED departure for " + departure.personnelId());
                }
                departureCount++;
            } else if (event instanceof MissionSkillGrowthApplied growth && growth.totalSkillGain() < 0) {
                throw new IllegalStateException(
                    "Negative mission skill growth for " + growth.personnelId() + ": " + growth.totalSkillGain());
            }
        }
        int rosterSizeAfter = rosterSize(snapshot);
        if (rosterSizeBeforeMissionProgress - departureCount != rosterSizeAfter) {
            throw new IllegalStateException("Roster size change does not match the number of reported mission departures");
        }

        checkMissionAssignmentConsistency(snapshot);
    }

    /**
     * Cross-checks one season's prestige and repeat-flight bookkeeping
     * against an independent recomputation from this season's
     * {@link MissionStepResolved} outcomes and the catalog's own step
     * definitions, rather than trusting {@link MissionProgress}'s own
     * arithmetic (see {@code design.md} &sect;2.6(d)):
     * <ul>
     *   <li>prestige is never negative;</li>
     *   <li>each mission's requested change is rebuilt step by step - full
     *       share for {@code SUCCESSFUL}/{@code MINOR_GLITCH_SOLVED}, half
     *       for {@code MINOR_GLITCH}, and a failing step replacing the running
     *       total with its own deduction;</li>
     *   <li>each reported change applies the zero floor to the running prestige
     *       total, and the applied amounts sum to the season's real delta;</li>
     *   <li>a mission that leaves the roster without a failure bumped its
     *       configuration's count by exactly one, and nothing else moved it.</li>
     * </ul>
     *
     * @param snapshot       the snapshot after this season's {@link MissionProgress} pass
     * @param events         this season's {@link MissionProgress} events
     * @param prestigeBefore the player's prestige before the pass
     * @param configByMission each in-progress mission's configuration id, in launch order, captured before the pass
     * @param countsBefore   the successful-flight counts, captured before the pass
     * @return {@code {paid, deducted, completedFlights}} for this season
     * @throws IllegalStateException if any invariant is violated
     */
    private static long[] checkPrestigeInvariants(PlayerSnapshot snapshot, List<ResolutionEvent> events,
            long prestigeBefore, GoalTally goalTally, Map<String, String> configByMission,
            Map<String, Integer> countsBefore) {
        if (snapshot.getPrestige() < 0) {
            throw new IllegalStateException("Prestige went negative: " + snapshot.getPrestige());
        }

        // A goal settles right after the step that achieved it (MissionProgress emits it there), so each
        // GoalSettled belongs to the most recent step of its mission - its prestige folds into that step.
        Map<String, List<StepRecord>> stepsByMission = new LinkedHashMap<>();
        for (ResolutionEvent event : events) {
            if (event instanceof MissionStepResolved resolved) {
                stepsByMission.computeIfAbsent(resolved.missionInstanceId(), id -> new ArrayList<>())
                    .add(new StepRecord(resolved));
            } else if (event instanceof GoalSettled settled) {
                checkGoalPrice(settled, goalTally);
                List<StepRecord> steps = stepsByMission.get(settled.missionInstanceId());
                steps.get(steps.size() - 1).goalPrestige += settled.prestige();
            }
        }

        // Recomputed in launch order, the same order MissionProgress resolves in, with the repeat tier read
        // live from a running copy of the counts - a mission that completes here bumps the count before
        // any later mission reads it (design.md §2.6(d); baspm.md §11.7).
        Map<String, Integer> runningCounts = new HashMap<>(countsBefore);
        Map<String, Long> expectedRequested = new HashMap<>();
        Map<String, Boolean> failedThisSeason = new HashMap<>();
        for (Map.Entry<String, String> mission : configByMission.entrySet()) {
            String missionId = mission.getKey();
            String configId = mission.getValue();
            List<StepRecord> steps = stepsByMission.getOrDefault(missionId, List.of());
            FlightTier tier = MissionPrestige.tierFor(runningCounts.getOrDefault(configId, 0));
            long tierTotal = MissionPrestige.tierTotal(CATALOG.missionConfiguration(configId).orElseThrow(), tier);
            long requested = 0;
            boolean failed = false;
            for (StepRecord record : steps) {
                MissionStepResolved resolved = record.resolved;
                MissionStep step = stepOf(resolved);
                long payout = tierTotal * step.prestigeSharePercent() / 100 + record.goalPrestige;
                switch (resolved.outcome()) {
                    case SUCCESSFUL, MINOR_GLITCH_SOLVED -> requested += payout;
                    case MINOR_GLITCH -> requested += payout / 2;
                    case FAILURE, CATASTROPHIC -> {
                        requested = -step.failurePrestigeDeduction();
                        failed = true;
                    }
                }
            }
            expectedRequested.put(missionId, requested);
            failedThisSeason.put(missionId, failed);
            if (!failed && !snapshot.getInProgressMissions().containsKey(missionId)) {
                runningCounts.merge(configId, 1, Integer::sum);
            }
        }

        long running = prestigeBefore;
        long paid = 0;
        long deducted = 0;
        int expectedChanges = 0;
        for (long requested : expectedRequested.values()) {
            if (requested != 0) {
                expectedChanges++;
            }
        }
        int changes = 0;
        for (ResolutionEvent event : events) {
            if (!(event instanceof MissionPrestigeChanged change)) {
                continue;
            }
            changes++;
            Long expected = expectedRequested.get(change.missionInstanceId());
            if (expected == null || change.requested() != expected) {
                String configId = configByMission.get(change.missionInstanceId());
                throw new IllegalStateException("Prestige change for " + change.missionInstanceId()
                    + " requested " + change.requested() + ", independent recomputation says " + expected
                    + " (config " + configId + ": count before pass " + countsBefore.getOrDefault(configId, 0)
                    + ", count now " + snapshot.getSuccessfulFlightCounts().getOrDefault(configId, 0)
                    + ", events this season " + events + ")");
            }
            long expectedApplied = Math.max(-running, change.requested());
            if (change.applied() != expectedApplied) {
                throw new IllegalStateException("Prestige change for " + change.missionInstanceId()
                    + " applied " + change.applied() + ", expected " + expectedApplied + " after the zero floor");
            }
            running += change.applied();
            if (change.applied() > 0) {
                paid += change.applied();
            } else {
                deducted -= change.applied();
            }
        }
        if (changes != expectedChanges) {
            throw new IllegalStateException("Expected " + expectedChanges + " prestige changes this season, saw " + changes);
        }
        if (running != snapshot.getPrestige()) {
            throw new IllegalStateException("Prestige events sum to " + running + " but the snapshot holds "
                + snapshot.getPrestige());
        }

        // The count is per configuration, not per mission: two flights of one configuration can end in the
        // same season (one completing, one failing), so the expected delta is summed per configuration.
        Map<String, Integer> expectedDeltaByConfig = new HashMap<>();
        int completed = 0;
        for (String missionId : configByMission.keySet()) {
            if (snapshot.getInProgressMissions().containsKey(missionId)) {
                continue;
            }
            if (!failedThisSeason.getOrDefault(missionId, false)) {
                expectedDeltaByConfig.merge(configByMission.get(missionId), 1, Integer::sum);
                completed++;
            }
        }
        Set<String> configIds = new HashSet<>(countsBefore.keySet());
        configIds.addAll(snapshot.getSuccessfulFlightCounts().keySet());
        for (String configId : configIds) {
            int delta = snapshot.getSuccessfulFlightCounts().getOrDefault(configId, 0)
                - countsBefore.getOrDefault(configId, 0);
            int expected = expectedDeltaByConfig.getOrDefault(configId, 0);
            if (delta != expected) {
                throw new IllegalStateException("Successful-flight count for " + configId + " moved by " + delta
                    + " this season, expected " + expected + " (completions only, failures never count)");
            }
        }
        return new long[] {paid, deducted, completed};
    }

    /**
     * Cross-checks one goal settlement's price against the game-wide tally as it
     * stood before this resolution's commit: the first-time value when nobody has
     * settled the goal yet, the second-time value otherwise (design.md &sect;2.6(d)).
     *
     * @param settled   the settlement event
     * @param goalTally the tally, not yet committed for this resolution
     * @throws IllegalStateException if the paid prestige doesn't match
     */
    private static void checkGoalPrice(GoalSettled settled, GoalTally goalTally) {
        Goal goal = CATALOG.goal(settled.goalId()).orElseThrow();
        long expected = goalTally.countOf(settled.goalId()) == 0
            ? goal.firstTimePrestige() : goal.secondTimePrestige();
        if (settled.prestige() != expected) {
            throw new IllegalStateException("Goal " + settled.goalId() + " settled for " + settled.prestige()
                + ", expected " + expected + " from the pre-commit tally");
        }
    }

    /**
     * Cross-checks a player's goal state: every achieved or cancelled id is a
     * goal the catalog defines, and no goal is both achieved and cancelled.
     *
     * @param snapshot the player to check
     * @throws IllegalStateException if any invariant is violated
     */
    private static void checkGoalState(PlayerSnapshot snapshot) {
        for (String goalId : snapshot.getAchievedGoals().keySet()) {
            if (CATALOG.goal(goalId).isEmpty()) {
                throw new IllegalStateException("Achieved goal unknown to the catalog: " + goalId);
            }
            if (snapshot.getCancelledGoalIds().contains(goalId)) {
                throw new IllegalStateException("Goal both achieved and cancelled: " + goalId);
            }
        }
        for (String goalId : snapshot.getCancelledGoalIds()) {
            if (CATALOG.goal(goalId).isEmpty()) {
                throw new IllegalStateException("Cancelled goal unknown to the catalog: " + goalId);
            }
        }
    }

    /**
     * Cross-checks one season's random events: each applied event's magnitude is inside
     * its content range, the news log grows by exactly the number of events (up to its
     * capacity), the roster shrinks by exactly the number of departures, and no
     * component falls below its transfer floor.
     *
     * @param snapshot       the player after the events
     * @param events         the events the pass reported
     * @param rosterBefore   the roster size before the pass
     * @param newsBefore     the news log size before the pass
     * @throws IllegalStateException if any invariant is violated
     */
    private static void checkRandomEventInvariants(PlayerSnapshot snapshot, List<ResolutionEvent> events,
            int rosterBefore, int newsBefore) {
        int occurred = 0;
        int departures = 0;
        for (ResolutionEvent event : events) {
            if (event instanceof RandomEventOccurred applied) {
                occurred++;
                RandomEventRoster.EventSpec spec = ROSTER.spec(applied.kind());
                // A reliability setback reports what was actually taken after the transfer floor clips it,
                // so its lower bound is zero, not the drawn minimum.
                double low = applied.kind() == RandomEventKind.RELIABILITY_SETBACK ? 0 : spec.min();
                if (applied.magnitude() < low || applied.magnitude() > spec.max()) {
                    throw new IllegalStateException(applied.kind() + " magnitude " + applied.magnitude()
                        + " outside its content range [" + low + ", " + spec.max() + "]");
                }
            } else if (event instanceof PersonnelDeparture) {
                departures++;
            }
        }
        int expectedNews = Math.min(PlayerSnapshot.NEWS_LOG_CAPACITY, newsBefore + occurred);
        if (snapshot.getNewsLog().size() != expectedNews) {
            throw new IllegalStateException("News log holds " + snapshot.getNewsLog().size() + ", expected "
                + expectedNews);
        }
        if (rosterSize(snapshot) != rosterBefore - departures) {
            throw new IllegalStateException("Roster went " + rosterBefore + " -> " + rosterSize(snapshot)
                + " with " + departures + " random-event departures");
        }
        for (OwnedMissionComponent owned : snapshot.getMissionComponents().values()) {
            if (owned.getCurrentReliability() < owned.getFloorReliability()) {
                throw new IllegalStateException("Component reliability fell below its transfer floor");
            }
        }
    }

    /**
     * Recomputes the season's running costs from the roster, the opened programs, and
     * the owned components, the same figure {@link BudgetProgress#runningCosts} charges.
     *
     * @param snapshot the player
     * @return the running costs
     */
    private static long independentRunningCosts(PlayerSnapshot snapshot) {
        long total = 0;
        for (Astronaut astronaut : snapshot.getAstronauts()) {
            total += Math.round(astronaut.getSalary());
        }
        for (SetSpecialist specialist : snapshot.getSetSpecialists()) {
            total += Math.round(specialist.getSalary());
        }
        for (MissionControlOfficer officer : snapshot.getMissionControlOfficers()) {
            total += Math.round(officer.getSalary());
        }
        for (String programId : snapshot.getOpenedProgramIds()) {
            total += CATALOG.program(programId).orElseThrow().maintenanceCost();
        }
        for (Map.Entry<String, OwnedMissionComponent> owned : snapshot.getMissionComponents().entrySet()) {
            if (owned.getValue().isActive()) {
                total += CATALOG.missionComponent(owned.getKey()).orElseThrow().maintenanceCost();
            }
        }
        // Building upkeep, read straight off the level table rather than through BuildingUpgrades.
        for (Building building : Building.values()) {
            BuildingSchedule.Levels table = CATALOG.buildings().of(building);
            int level = snapshot.getBuildingLevels().getOrDefault(building, 1);
            total += table.maintenance().get(level - 1);
        }
        return Math.round(total * snapshot.getCostMultiplier());
    }

    /**
     * The bot's building upgrade (journaled-action bucket): with a small chance per
     * season, starts upgrading one random building, through the validated action.
     *
     * @param snapshot the player
     * @param random   the shared random source
     * @return 1 if an upgrade started, else 0
     */
    private static int maybeUpgradeBuilding(PlayerSnapshot snapshot, Random random) {
        if (random.nextDouble() >= BUILDING_UPGRADE_CHANCE) {
            return 0;
        }
        Building[] buildings = Building.values();
        Building building = buildings[random.nextInt(buildings.length)];
        long fundsBefore = snapshot.getFunds();
        int levelBefore = snapshot.buildingLevel(building);
        BuildingUpgradeOutcome outcome = BuildingUpgrades.startUpgrade(snapshot, building, CATALOG.buildings());
        if (outcome != BuildingUpgradeOutcome.STARTED) {
            if (snapshot.getFunds() != fundsBefore) {
                throw new IllegalStateException("A refused upgrade changed funds: " + outcome);
            }
            return 0;
        }
        long expectedCost = CATALOG.buildings().of(building).upgradeCosts().get(levelBefore - 1);
        if (fundsBefore - snapshot.getFunds() != expectedCost) {
            throw new IllegalStateException("Upgrade of " + building + " charged " + (fundsBefore - snapshot.getFunds())
                + ", expected " + expectedCost);
        }
        return 1;
    }

    /**
     * Checks the building state after the upgrade pass: every level is within its
     * table, every roster within its building's cap, and opened programs within the
     * Headquarters cap.
     *
     * @param snapshot the player
     * @param upgradeEvents the upgrade pass's events
     * @param levelsBefore the levels before the pass
     */
    private static void checkBuildingInvariants(PlayerSnapshot snapshot, List<ResolutionEvent> upgradeEvents,
            Map<Building, Integer> levelsBefore) {
        for (Building building : Building.values()) {
            int level = snapshot.buildingLevel(building);
            int max = CATALOG.buildings().of(building).maxLevel();
            if (level < 1 || level > max) {
                throw new IllegalStateException(building + " at level " + level + ", outside 1.." + max);
            }
            long finished = upgradeEvents.stream()
                .filter(event -> event instanceof BuildingUpgraded upgraded && upgraded.building() == building)
                .count();
            if (level != levelsBefore.get(building) + finished) {
                throw new IllegalStateException(building + " moved from " + levelsBefore.get(building) + " to "
                    + level + " with " + finished + " upgrade(s) finished");
            }
        }
        // Rosters can start above their cap (the harness seeds them freely), so roster caps are checked where
        // hires happen. Opened programs only ever grow through ProgramOpening, so Headquarters holds outright.
        int headquartersCap = independentCap(Building.HEADQUARTERS, snapshot);
        if (snapshot.getOpenedProgramIds().size() > headquartersCap) {
            throw new IllegalStateException(snapshot.getOpenedProgramIds().size()
                + " programs open, Headquarters allows " + headquartersCap);
        }
    }

    /**
     * Reads a building's current cap straight off the level table.
     *
     * @param building the building
     * @param snapshot the player
     * @return the cap at the building's recorded level
     */
    private static int independentCap(Building building, PlayerSnapshot snapshot) {
        return CATALOG.buildings().of(building).caps().get(snapshot.getBuildingLevels().getOrDefault(building, 1) - 1);
    }

    /**
     * Cross-checks one budget season against an independent recomputation: the
     * expenses match the roster and holdings, the review (if one falls due) matches
     * the bracket table and catch-up rule, and funds reconcile to fundsBefore - expenses + income.
     *
     * @param snapshot              the player after the budget pass
     * @param events                the budget pass's events
     * @param fundsBefore           funds before the pass
     * @param expectedExpenses      the independently recomputed running costs
     * @param prestigeAtLastReview  the review baseline before the pass
     * @param seasonsElapsed        the seasons-elapsed count the pass was given
     * @throws IllegalStateException if any invariant is violated
     */
    private static void checkBudgetInvariants(PlayerSnapshot snapshot, List<ResolutionEvent> events, long fundsBefore,
            long expectedExpenses, long prestigeAtLastReview, int seasonsElapsed) {
        SeasonFundsSettled settled = null;
        BudgetReviewed review = null;
        for (ResolutionEvent event : events) {
            if (event instanceof SeasonFundsSettled found) {
                settled = found;
            } else if (event instanceof BudgetReviewed found) {
                review = found;
            }
        }
        if (settled == null) {
            throw new IllegalStateException("Budget pass produced no SeasonFundsSettled event");
        }
        if (settled.expenses() != expectedExpenses) {
            throw new IllegalStateException("Expenses " + settled.expenses() + ", independent recomputation says "
                + expectedExpenses);
        }
        boolean reviewDue = seasonsElapsed % BudgetProgress.SEASONS_PER_REVIEW == 0;
        if (reviewDue != (review != null)) {
            throw new IllegalStateException("Review due=" + reviewDue + " but an event was "
                + (review != null ? "reported" : "absent") + " at seasons elapsed " + seasonsElapsed);
        }
        if (review != null) {
            int reviewNumber = seasonsElapsed / BudgetProgress.SEASONS_PER_REVIEW;
            long shift = 0;
            if (reviewNumber > 1) {
                long previousMin = SCHEDULE.reviewFor(reviewNumber - 1).minPrestige();
                if (prestigeAtLastReview < previousMin) {
                    shift = previousMin - prestigeAtLastReview;
                }
            }
            if (review.reviewNumber() != reviewNumber || review.catchUpShift() != shift) {
                throw new IllegalStateException("Review " + review + " disagrees with review " + reviewNumber
                    + " and catch-up " + shift);
            }
            long expectedRate = independentRate(SCHEDULE.reviewFor(reviewNumber), shift, review.prestige());
            if (review.fundingRate() != expectedRate) {
                throw new IllegalStateException("Review set rate " + review.fundingRate()
                    + ", independent recomputation says " + expectedRate);
            }
            if (snapshot.getPrestigeAtLastReview() != review.prestige()) {
                throw new IllegalStateException("Review baseline not recorded");
            }
        }
        long rate = snapshot.getFundingRatePerSeason();
        if (settled.income() != rate || settled.fundsAfter() != snapshot.getFunds()) {
            throw new IllegalStateException("Season funds event disagrees with the snapshot");
        }
        if (snapshot.getFunds() != fundsBefore - expectedExpenses + rate) {
            throw new IllegalStateException("Funds don't reconcile: " + snapshot.getFunds() + " != " + fundsBefore
                + " - " + expectedExpenses + " + " + rate);
        }
    }

    /**
     * Recomputes a review's funding from the bracket, the catch-up shift, and the
     * prestige, with the same straight-line shape BASPM uses - independently of
     * {@link BudgetProgress}'s own code.
     *
     * @param bracket the bracket the review reads
     * @param shift   the catch-up shift applied to the bracket's prestige ends
     * @param prestige the player's prestige
     * @return the funding per season
     */
    private static long independentRate(BudgetSchedule.Review bracket, long shift, long prestige) {
        long minP = bracket.minPrestige() - shift;
        long maxP = bracket.maxPrestige() - shift;
        if (prestige <= minP) {
            return bracket.minFunding();
        }
        if (prestige >= maxP) {
            return bracket.maxFunding();
        }
        // Same floating-point form as BudgetProgress's own line, so a legitimate rounding edge can't split the two.
        double slope = (double) (bracket.maxFunding() - bracket.minFunding()) / (maxP - minP);
        double intercept = bracket.maxFunding() - slope * maxP;
        return (long) Math.ceil(slope * prestige + intercept);
    }

    /**
     * The bot's debt conversion: with a 50% chance when in debt, converts a random
     * amount up to what the cap and the player's prestige allow. Every such attempt
     * must succeed, and funds must never end past two seasons of running costs.
     *
     * @param snapshot the player
     * @param random   the bot's random source
     * @return {@code 1} if a conversion happened, {@code 0} otherwise
     */
    private static int maybeConvertPrestige(PlayerSnapshot snapshot, Random random) {
        if (snapshot.getFunds() >= 0 || random.nextDouble() >= 0.5) {
            return 0;
        }
        long affordable = snapshot.getPrestige() / PRESTIGE_PER_FUND;
        long max = Math.min(PrestigeConversion.maxConversion(snapshot, CATALOG), affordable);
        if (max <= 0) {
            return 0;
        }
        long amount = 1 + (long) (random.nextDouble() * max);
        PrestigeConversion.Outcome outcome = PrestigeConversion.convert(snapshot, CATALOG, SCHEDULE, amount);
        if (outcome != PrestigeConversion.Outcome.CONVERTED) {
            throw new IllegalStateException("Conversion of " + amount + " within the cap gave " + outcome);
        }
        long cap = PrestigeConversion.COVERED_SEASONS * BudgetProgress.runningCosts(snapshot, CATALOG);
        if (snapshot.getFunds() > cap) {
            throw new IllegalStateException("Funds " + snapshot.getFunds() + " exceed the two-season cover " + cap);
        }
        return 1;
    }

    /**
     * One mission step in one season, with the goal prestige its settlements paid.
     */
    private static final class StepRecord {

        private final MissionStepResolved resolved;
        private long goalPrestige;

        private StepRecord(MissionStepResolved resolved) {
            this.resolved = resolved;
        }
    }

    /**
     * Looks up the {@link MissionStep} a resolved step event refers to, from
     * the catalog - the same definition {@link MissionProgress} resolved against.
     *
     * @param resolved the step event
     * @return the step definition
     * @throws IllegalStateException if the catalog has no such step
     */
    private static MissionStep stepOf(MissionStepResolved resolved) {
        return CATALOG.missionConfiguration(resolved.missionConfigurationId())
            .map(configuration -> configuration.seasons().get(resolved.seasonIndex()).steps().stream()
                .filter(step -> step.id().equals(resolved.stepId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No step " + resolved.stepId())))
            .orElseThrow(() -> new IllegalStateException("No configuration " + resolved.missionConfigurationId()));
    }

    /**
     * Cross-checks the bidirectional pairing between every
     * Astronaut/Mission Control officer's own {@code ASSIGNED}/
     * {@code assignedMissionId} and every {@link InProgressMission}'s own
     * assigned-id lists: each side must agree with the other, nobody is
     * assigned to more than one mission at once, and every mission's own
     * season index is within its configuration's own range.
     *
     * @param snapshot the snapshot to check
     * @throws IllegalStateException if any invariant is violated
     */
    private static void checkMissionAssignmentConsistency(PlayerSnapshot snapshot) {
        for (Astronaut astronaut : snapshot.getAstronauts()) {
            checkAssignmentPointer(astronaut);
        }
        for (MissionControlOfficer officer : snapshot.getMissionControlOfficers()) {
            checkAssignmentPointer(officer);
        }

        Set<String> claimedPersonnelIds = new HashSet<>();
        for (InProgressMission mission : snapshot.getInProgressMissions().values()) {
            // Ids are unique across rosters, so each slot holder belongs to exactly one roster.
            checkMissionSideGroup(mission.getId(), mission.crewIds(), snapshot.getAstronauts(), claimedPersonnelIds);
            List<String> groundAstronauts = new ArrayList<>();
            List<String> groundControllers = new ArrayList<>();
            for (String id : mission.groundIds()) {
                (findAstronautById(snapshot, id) != null ? groundAstronauts : groundControllers).add(id);
            }
            checkMissionSideGroup(mission.getId(), groundAstronauts, snapshot.getAstronauts(), claimedPersonnelIds);
            checkMissionSideGroup(mission.getId(), groundControllers, snapshot.getMissionControlOfficers(),
                claimedPersonnelIds);

            MissionConfiguration configuration =
                CATALOG.missionConfiguration(mission.getMissionConfigurationId()).orElse(null);
            if (configuration == null) {
                continue;
            }
            if (mission.getCurrentSeasonIndex() < 0
                    || mission.getCurrentSeasonIndex() >= configuration.durationInSeasons()) {
                throw new IllegalStateException(
                    "Mission " + mission.getId() + " season index out of range: " + mission.getCurrentSeasonIndex());
            }
            checkSlotFilling(snapshot, mission, configuration);
            if (mission.getRocketIds().size() != configuration.launches().size()) {
                throw new IllegalStateException("Mission " + mission.getId() + " flies " + mission.getRocketIds().size()
                    + " rocket(s) for " + configuration.launches().size() + " launch(es)");
            }
        }
    }

    /**
     * Checks a mission's staffing slot by slot, re-derived from the configuration's
     * steps rather than trusting {@link MissionLaunch}: a slot some step uses holds
     * exactly one person of the slot's type, and a slot no step uses holds nobody.
     *
     * @param snapshot      the player
     * @param mission       the mission
     * @param configuration its configuration
     * @throws IllegalStateException if a slot is filled wrongly
     */
    private static void checkSlotFilling(PlayerSnapshot snapshot, InProgressMission mission,
            MissionConfiguration configuration) {
        Set<Integer> usedCrew = new HashSet<>();
        Set<Integer> usedControl = new HashSet<>();
        // The crew stays aboard for the whole mission; a control slot is held only while the current or a
        // later season still uses it.
        for (int season = 0; season < configuration.seasons().size(); season++) {
            for (MissionStep step : configuration.seasons().get(season).steps()) {
                step.crewInvolvements().forEach(involvement -> usedCrew.add(involvement.slot()));
                if (season >= mission.getCurrentSeasonIndex()) {
                    step.controllerInvolvements().forEach(involvement -> usedControl.add(involvement.slot()));
                }
            }
        }
        for (int slot = 0; slot < Math.max(configuration.crewSlots().size(), mission.getCrewSlotIds().size());
                slot++) {
            String holder = slot < mission.getCrewSlotIds().size() ? mission.getCrewSlotIds().get(slot) : null;
            if ((holder != null) != usedCrew.contains(slot)
                    || (holder != null && findAstronautById(snapshot, holder) == null)) {
                throw new IllegalStateException("Mission " + mission.getId() + " crew slot " + slot + " holds "
                    + holder);
            }
        }
        for (int slot = 0; slot < Math.max(configuration.controlSlots().size(), mission.getControlSlotIds().size());
                slot++) {
            String holder = slot < mission.getControlSlotIds().size() ? mission.getControlSlotIds().get(slot) : null;
            boolean wantsAstronaut = slot < configuration.controlSlots().size()
                && configuration.controlSlots().get(slot).kind() == StaffingSlot.Kind.ASTRONAUT;
            boolean rightType = holder == null || (wantsAstronaut
                ? findAstronautById(snapshot, holder) != null
                : snapshot.getMissionControlOfficers().stream().anyMatch(o -> o.getId().equals(holder)));
            if ((holder != null) != usedControl.contains(slot) || !rightType) {
                throw new IllegalStateException("Mission " + mission.getId() + " control slot " + slot + " holds "
                    + holder);
            }
        }
    }

    private static Astronaut findAstronautById(PlayerSnapshot snapshot, String id) {
        return snapshot.getAstronauts().stream().filter(a -> a.getId().equals(id)).findFirst().orElse(null);
    }

    /**
     * Checks one Astronaut/Mission Control officer's own
     * {@code ASSIGNED}/{@code assignedMissionId} pairing.
     *
     * @param person the employee to check
     * @throws IllegalStateException if the pairing is broken
     */
    private static void checkAssignmentPointer(Personnel person) {
        boolean assigned = person.getAssignmentState() == AssignmentState.ASSIGNED;
        String missionId = person.getAssignedMissionId();
        if (assigned && missionId == null) {
            throw new IllegalStateException("ASSIGNED employee with no assignedMissionId: " + person.getId());
        }
        if (!assigned && missionId != null) {
            throw new IllegalStateException("Not ASSIGNED but assignedMissionId still set: " + person.getId());
        }
    }

    /**
     * Checks one mission's own assigned-id list, from the mission's side:
     * every id must exist on {@code roster}, be claimed by no other
     * mission, and agree with that person's own {@code ASSIGNED}/
     * {@code assignedMissionId}.
     *
     * @param missionId           the mission these ids are assigned to
     * @param ids                 the assigned ids to check
     * @param roster              the roster the ids are looked up against
     * @param claimedPersonnelIds every id claimed by any mission so far,
     *                            across both groups; mutated in place
     * @param <P>                 the roster's own {@link Personnel} subtype
     * @throws IllegalStateException if any id is unknown, claimed twice, or
     *                                disagrees with that person's own state
     */
    private static <P extends Personnel> void checkMissionSideGroup(String missionId, List<String> ids,
            List<P> roster, Set<String> claimedPersonnelIds) {
        for (String id : ids) {
            if (!claimedPersonnelIds.add(id)) {
                throw new IllegalStateException("Personnel " + id + " is assigned to more than one in-progress mission");
            }
            P person = findPersonById(roster, id);
            if (person == null) {
                throw new IllegalStateException("Mission " + missionId + " references unknown personnel id " + id);
            }
            if (person.getAssignmentState() != AssignmentState.ASSIGNED
                    || !missionId.equals(person.getAssignedMissionId())) {
                throw new IllegalStateException("Mission " + missionId + "'s own assigned id " + id
                    + " disagrees with that person's own ASSIGNED/assignedMissionId state");
            }
        }
    }

    /**
     * Finds a {@link Personnel} record by id within a roster.
     *
     * @param roster the roster to search
     * @param id     the id to find
     * @param <P>    the roster's own {@link Personnel} subtype
     * @return the matching record, or {@code null} if none matches
     */
    private static <P extends Personnel> P findPersonById(List<P> roster, String id) {
        for (P person : roster) {
            if (person.getId().equals(id)) {
                return person;
            }
        }
        return null;
    }

    /**
     * Cross-checks {@link ProgramOpening#open}'s own output, computed
     * independently of its internals: funds never go negative, every
     * opened program id is one {@link #CATALOG} actually defines, every
     * owned component's reliability and floor stay ordered
     * {@code baselineReliability <= floorReliability <= currentReliability
     * <= maxReliability}, and every owned component is required by at
     * least one currently opened program (no component is ever owned
     * without a program having granted it).
     *
     * <p>The reliability-range check genuinely exercises
     * {@link ResearchProgress}'s own output too, not only
     * {@link ProgramOpening}'s grant-time computation - it runs every
     * season, after R&amp;D may have grown a component's
     * {@code currentReliability} well past what it started at.</p>
     *
     * @param snapshot the snapshot to check, at any point after this
     *                 season's {@link #maybeOpenProgram} call
     * @throws IllegalStateException if any invariant is violated
     */
    private static void checkProgramInvariants(PlayerSnapshot snapshot) {
        // Funds may be negative: debt is allowed (design.md §2.1(d)), so there is no non-negativity invariant.

        Set<String> requiredByAnOpenedProgram = new HashSet<>();
        for (String programId : snapshot.getOpenedProgramIds()) {
            Program program = CATALOG.program(programId)
                .orElseThrow(() -> new IllegalStateException("Opened an unknown program: " + programId));
            requiredByAnOpenedProgram.addAll(program.requiredMissionComponentIds());
        }

        for (Map.Entry<String, OwnedMissionComponent> entry : snapshot.getMissionComponents().entrySet()) {
            String componentId = entry.getKey();
            OwnedMissionComponent owned = entry.getValue();
            MissionComponent component = CATALOG.missionComponent(componentId)
                .orElseThrow(() -> new IllegalStateException("Owns an unknown mission component: " + componentId));
            if (owned.getCurrentReliability() < component.baselineReliability()
                    || owned.getCurrentReliability() > component.maxReliability()) {
                throw new IllegalStateException("Reliability out of range for " + componentId + ": "
                    + owned.getCurrentReliability());
            }
            if (owned.getFloorReliability() < component.baselineReliability()
                    || owned.getFloorReliability() > owned.getCurrentReliability()) {
                throw new IllegalStateException("Floor reliability out of order for " + componentId
                    + ": baseline " + component.baselineReliability() + " / floor " + owned.getFloorReliability()
                    + " / current " + owned.getCurrentReliability());
            }
            // Active exactly when some open program requires it (baspm.md §10.9's IsActive).
            if (owned.isActive() != requiredByAnOpenedProgram.contains(componentId)) {
                throw new IllegalStateException(componentId + " active=" + owned.isActive()
                    + " but required by an open program=" + requiredByAnOpenedProgram.contains(componentId));
            }
        }
        for (SetSpecialist specialist : snapshot.getSetSpecialists()) {
            String target = specialist.getRdTargetComponentId();
            if (target != null && !snapshot.getMissionComponents().get(target).isActive()) {
                throw new IllegalStateException(specialist.getId() + " researches inactive component " + target);
            }
        }
    }

    /**
     * The four {@link HiringPool} quality bands' skill ranges, in the same
     * POOR/AVERAGE/GOOD/EXCELLENT order {@link #bandIndex} returns - built
     * from {@link HiringPool}'s own public range constants, but the
     * band-selection logic below ({@link #bandIndex}) is this class's own
     * independent re-derivation, not a call into {@link HiringPool}'s
     * private {@code bandFor}/{@code randomSkill}.
     */
    private static final double[][] HIRING_SKILL_RANGES = {
        {HiringPool.POOR_SKILL_MIN, HiringPool.POOR_SKILL_MAX},
        {HiringPool.AVERAGE_SKILL_MIN, HiringPool.AVERAGE_SKILL_MAX},
        {HiringPool.GOOD_SKILL_MIN, HiringPool.GOOD_SKILL_MAX},
        {HiringPool.EXCELLENT_SKILL_MIN, HiringPool.EXCELLENT_SKILL_MAX},
    };

    /**
     * Cross-checks {@link HiringPool#refreshCandidates}'s own output,
     * computed independently of its internals: every id across all three
     * rosters <i>and</i> all three candidate pools is globally unique (the
     * shared {@link PlayerSnapshot#allocatePersonnelId()} counter must never
     * repeat), each pool holds exactly {@link HiringPool#HIRING_BATCH_SIZE}
     * candidates after a refresh, and every candidate's skills fall inside
     * the range this class's own {@link #bandIndex} would select for that
     * group's current average roster morale - also tallies which band each
     * group's refresh landed in, for {@link PersonnelPlayoutReport}.
     *
     * @param snapshot the snapshot immediately after this season's
     *                 {@link HiringPool#refreshCandidates} call
     * @param bandHits four running totals, indexed by {@link #bandIndex};
     *                 incremented in place, once per group per season
     * @throws IllegalStateException if any invariant is violated
     */
    private static void checkHiringInvariants(PlayerSnapshot snapshot, int[] bandHits) {
        checkUniqueIds(snapshot);
        checkPoolSize(snapshot.getAstronautCandidates());
        checkPoolSize(snapshot.getSetSpecialistCandidates());
        checkPoolSize(snapshot.getMissionControlCandidates());

        int astronautBand = bandIndex(independentAverageMorale(snapshot.getAstronauts()));
        bandHits[astronautBand]++;
        for (Astronaut candidate : snapshot.getAstronautCandidates()) {
            checkSkillsInBand(candidate, PersonnelGroup.ASTRONAUT, candidate.getSkills().values(), astronautBand);
        }

        int setBand = bandIndex(independentAverageMorale(snapshot.getSetSpecialists()));
        bandHits[setBand]++;
        for (SetSpecialist candidate : snapshot.getSetSpecialistCandidates()) {
            checkSkillsInBand(candidate, PersonnelGroup.SET, candidate.getSkills().values(), setBand);
        }

        int missionControlBand = bandIndex(independentAverageMorale(snapshot.getMissionControlOfficers()));
        bandHits[missionControlBand]++;
        for (MissionControlOfficer candidate : snapshot.getMissionControlCandidates()) {
            checkSkillsInBand(candidate, PersonnelGroup.MISSION_CONTROL, candidate.getSkills().values(), missionControlBand);
        }
    }

    private static void checkUniqueIds(PlayerSnapshot snapshot) {
        Set<String> ids = new HashSet<>();
        addIds(snapshot.getAstronauts(), ids);
        addIds(snapshot.getSetSpecialists(), ids);
        addIds(snapshot.getMissionControlOfficers(), ids);
        addIds(snapshot.getAstronautCandidates(), ids);
        addIds(snapshot.getSetSpecialistCandidates(), ids);
        addIds(snapshot.getMissionControlCandidates(), ids);
    }

    private static void addIds(List<? extends Personnel> people, Set<String> ids) {
        for (Personnel person : people) {
            if (!ids.add(person.getId())) {
                throw new IllegalStateException("Duplicate personnel id across rosters/pools: " + person.getId());
            }
        }
    }

    private static void checkPoolSize(List<? extends Personnel> pool) {
        if (pool.size() != HiringPool.HIRING_BATCH_SIZE) {
            throw new IllegalStateException("Candidate pool size mismatch: " + pool.size());
        }
    }

    /**
     * Checks a candidate's skills against their band's range, shifted by their experience bonus
     * (re-derived here from the age, the group's retirement age and {@link HiringPool}'s public
     * constants) and capped at {@link HiringPool#CANDIDATE_SKILL_CAP}; also checks their learning
     * potential lies in the range for their experience.
     */
    private static void checkSkillsInBand(Personnel candidate, PersonnelGroup group, Iterable<Double> skills,
            int band) {
        int retirementAge = switch (group) {
            case ASTRONAUT -> PersonnelLifecycle.ASTRONAUT_RETIREMENT_AGE;
            case SET -> PersonnelLifecycle.SET_RETIREMENT_AGE;
            case MISSION_CONTROL -> PersonnelLifecycle.MISSION_CONTROL_RETIREMENT_AGE;
        };
        double experience = Math.max(0.0, Math.min(1.0, (candidate.getAge() - HiringPool.CAREER_START_AGE)
            / (double) (retirementAge - HiringPool.CAREER_START_AGE)));
        // Centred on the group's average candidate, whose experience HiringPool derives from the portraits.
        double bonus = (experience - HiringPool.meanExperience(group)) * HiringPool.EXPERIENCE_SKILL_RANGE;
        double min = Math.max(0.0, Math.min(HiringPool.CANDIDATE_SKILL_CAP, HIRING_SKILL_RANGES[band][0] + bonus))
            - 1e-9;
        double max = Math.min(HiringPool.CANDIDATE_SKILL_CAP, HIRING_SKILL_RANGES[band][1] + bonus) + 1e-9;
        double lpMin = HiringPool.ROOKIE_MIN_LEARNING_POTENTIAL
            + experience * (HiringPool.VETERAN_MIN_LEARNING_POTENTIAL - HiringPool.ROOKIE_MIN_LEARNING_POTENTIAL);
        double lpMax = HiringPool.ROOKIE_MAX_LEARNING_POTENTIAL
            + experience * (HiringPool.VETERAN_MAX_LEARNING_POTENTIAL - HiringPool.ROOKIE_MAX_LEARNING_POTENTIAL);
        int learningPotential = candidate.getLearningPotential();
        if (learningPotential < Math.round(lpMin) || learningPotential > Math.round(lpMax)) {
            throw new IllegalStateException("Candidate " + candidate.getId() + " learning potential "
                + candidate.getLearningPotential() + " outside [" + lpMin + ", " + lpMax + "] for experience "
                + experience);
        }
        String candidateId = candidate.getId();
        for (double skill : skills) {
            if (skill < min || skill > max) {
                throw new IllegalStateException("Candidate " + candidateId + " skill " + skill
                    + " outside band range [" + min + ", " + max + "]");
            }
        }
    }

    private static double independentAverageMorale(List<? extends Personnel> roster) {
        if (roster.isEmpty()) {
            return HiringPool.EMPTY_ROSTER_FALLBACK_MORALE;
        }
        double sum = 0;
        for (Personnel person : roster) {
            sum += person.getMorale();
        }
        return sum / roster.size();
    }

    private static int bandIndex(double averageMorale) {
        if (averageMorale <= HiringPool.POOR_MORALE_CEILING) {
            return 0;
        }
        if (averageMorale <= HiringPool.AVERAGE_MORALE_CEILING) {
            return 1;
        }
        if (averageMorale <= HiringPool.GOOD_MORALE_CEILING) {
            return 2;
        }
        return 3;
    }

    /**
     * Cross-checks the invariant {@code design.md} &sect;3.4 proposes for
     * {@link TrainingKind#PERMANENT}: while still actively re-enrolled, the
     * target level must be set (not stale/zero) and no higher than
     * {@link SkillGrowth#SKILL_CAP}, and the target skill's current value
     * must still be strictly below it - otherwise {@code TrainingProgress}
     * should already have finished this training, per its own
     * {@code shouldReenroll}.
     *
     * @param person             the employee to check, currently {@code PERMANENT}-training
     * @param currentSkillOfTarget the target skill's current value
     * @throws IllegalStateException if the invariant is violated
     */
    private static void checkPermanentTrainingTarget(Personnel person, double currentSkillOfTarget) {
        double targetLevel = person.getTrainingTargetLevel();
        if (targetLevel <= 0 || targetLevel > SkillGrowth.SKILL_CAP) {
            throw new IllegalStateException(
                "PERMANENT target level out of range for " + person.getId() + ": " + targetLevel);
        }
        if (currentSkillOfTarget >= targetLevel) {
            throw new IllegalStateException(
                "PERMANENT training still active despite the target already being reached for " + person.getId());
        }
    }

    private static void checkTrainingConsistency(Personnel person, Object targetSkill) {
        boolean inTraining = person.getAssignmentState() == AssignmentState.IN_TRAINING;
        boolean hasTrainingKind = person.getTrainingKind() != TrainingKind.NONE;
        if (inTraining != hasTrainingKind) {
            throw new IllegalStateException("IN_TRAINING/trainingKind disagree for " + person.getId());
        }
        if (inTraining && targetSkill == null) {
            throw new IllegalStateException("IN_TRAINING employee with no target skill: " + person.getId());
        }
        if (!inTraining && targetSkill != null) {
            throw new IllegalStateException("Not training but a target skill is still set: " + person.getId());
        }
    }

    private static void checkSkillsInRange(String personnelId, Iterable<Double> skillValues) {
        for (double skill : skillValues) {
            if (skill < 0 || skill > SkillGrowth.SKILL_CAP) {
                throw new IllegalStateException("Skill out of range for " + personnelId + ": " + skill);
            }
        }
    }

    /**
     * Collects the {@code group:id} key of every employee currently
     * protected from departure, across all three rosters, for
     * {@link #checkInvariants}'s cross-check - computed independently from
     * {@code PersonnelLifecycle}'s own protection rule (SET is never
     * protected; Astronaut/Mission Control are protected only while
     * {@link AssignmentState#ASSIGNED}, not merely non-idle), deliberately
     * not by calling it.
     *
     * @param snapshot the snapshot to inspect
     * @return the protected employees' keys
     */
    private static Set<String> protectedEmployeeKeys(PlayerSnapshot snapshot) {
        Set<String> keys = new HashSet<>();
        addProtectedKeys(snapshot.getAstronauts(), PersonnelGroup.ASTRONAUT, keys);
        addProtectedKeys(snapshot.getMissionControlOfficers(), PersonnelGroup.MISSION_CONTROL, keys);
        // SET is deliberately excluded - never protected, regardless of assignment state.
        return keys;
    }

    private static void addProtectedKeys(List<? extends Personnel> roster, PersonnelGroup group, Set<String> keys) {
        for (Personnel person : roster) {
            if (person.getAssignmentState() == AssignmentState.ASSIGNED) {
                keys.add(employeeKey(group, person.getId()));
            }
        }
    }

    private static String employeeKey(PersonnelGroup group, String personnelId) {
        return group + ":" + personnelId;
    }

    private static void checkMoraleRange(Personnel person) {
        if (person.getMorale() < PersonnelLifecycle.MORALE_MIN || person.getMorale() > PersonnelLifecycle.MORALE_MAX) {
            throw new IllegalStateException("Morale out of range for " + person.getId() + ": " + person.getMorale());
        }
    }

    private static int retirementAgeFor(PersonnelGroup group) {
        return switch (group) {
            case ASTRONAUT -> PersonnelLifecycle.ASTRONAUT_RETIREMENT_AGE;
            case SET -> PersonnelLifecycle.SET_RETIREMENT_AGE;
            case MISSION_CONTROL -> PersonnelLifecycle.MISSION_CONTROL_RETIREMENT_AGE;
        };
    }

    /**
     * Drives every employee's per-season idle/training/pull-out decision
     * across all three rosters - see {@link #shuffleAstronaut}/
     * {@link #shuffleSetSpecialist}/{@link #shuffleMissionControlOfficer}
     * for the per-group shape.
     *
     * @param snapshot    the snapshot to shuffle; mutated in place
     * @param random      the random source to draw from
     * @param expertTally {@code [0]} incremented once per successful
     *                    Expert-training start, {@code [1]} once per
     *                    successful pull-out, across every group; mutated
     *                    in place
     */
    private static void shuffleAssignments(PlayerSnapshot snapshot, Random random, int[] expertTally) {
        snapshot.getAstronauts().forEach(a -> shuffleAstronaut(snapshot, a, random, expertTally));
        snapshot.getSetSpecialists().forEach(s -> shuffleSetSpecialist(s, snapshot, random, expertTally));
        snapshot.getMissionControlOfficers().forEach(m -> shuffleMissionControlOfficer(snapshot, m, random, expertTally));
    }

    /**
     * The bot's per-season decision for one astronaut: if currently
     * {@code IN_TRAINING} (any kind), a small chance to pull out early via
     * the validated {@link TrainingProgress#pullOutOfTraining} action
     * (never by setting fields directly - see
     * {@link #checkExpertTrainingEligibility}'s own cross-check, which
     * depends on this). Otherwise, if idle and the shuffle's own
     * reassignment roll clears, either starts {@link TrainingKind#EXPERT}
     * training (via {@link TrainingProgress#startAstronautExpertTraining},
     * same validated-action posture) when at least one skill is eligible
     * and a separate, smaller chance clears, or falls back to the
     * existing fixed-length pick.
     *
     * @param snapshot    the snapshot {@code astronaut} belongs to
     * @param astronaut   the astronaut to maybe reassign
     * @param random      the random source to draw from
     * @param expertTally mutated in place - see {@link #shuffleAssignments}
     */
    private static void shuffleAstronaut(PlayerSnapshot snapshot, Astronaut astronaut, Random random,
            int[] expertTally) {
        if (astronaut.getAssignmentState() == AssignmentState.IN_TRAINING) {
            if (random.nextDouble() < PULL_OUT_OF_TRAINING_CHANCE_PER_SEASON
                    && TrainingProgress.pullOutOfTraining(snapshot, PersonnelGroup.ASTRONAUT, astronaut.getId(),
                        random) == PullOutOfTrainingOutcome.PULLED_OUT) {
                expertTally[1]++;
            }
            return;
        }
        if (random.nextDouble() >= REASSIGNMENT_CHANCE_PER_SEASON) {
            return;
        }
        // ASSIGNED is now exclusively mission-driven - MissionLaunch is the only path in
        // (see maybeLaunchMission), MissionProgress.advanceSeason the only path back to IDLE on
        // a mission's end. This shuffle only ever decides idle <-> training.
        if (astronaut.getAssignmentState() != AssignmentState.IDLE) {
            return;
        }
        List<AstronautSkill> eligibleExpertSkills = eligibleExpertAstronautSkills(astronaut);
        if (!eligibleExpertSkills.isEmpty() && random.nextDouble() < EXPERT_TRAINING_START_CHANCE_PER_SEASON) {
            AstronautSkill target = eligibleExpertSkills.get(random.nextInt(eligibleExpertSkills.size()));
            if (TrainingProgress.startAstronautExpertTraining(snapshot, astronaut.getId(), target)
                    == StartExpertTrainingOutcome.STARTED) {
                expertTally[0]++;
            }
            return;
        }
        if (random.nextBoolean()) {
            return;
        }
        AstronautSkill[] skills = AstronautSkill.values();
        AstronautSkill target = skills[random.nextInt(skills.length)];
        TrainingKind kind = drawTrainingKind(random, astronaut.getSkill(target));
        int total = kind == TrainingKind.BASIC
            ? TrainingProgress.ASTRONAUT_BASIC_TRAINING_SEASONS
            : TrainingProgress.ADVANCED_TRAINING_SEASONS;
        astronaut.setTrainingKind(kind);
        astronaut.setTrainingSeasonsTotal(total);
        astronaut.setTrainingSeasonsCompleted(0);
        astronaut.setTrainingTargetSkill(target);
        if (kind == TrainingKind.PERMANENT) {
            astronaut.setTrainingTargetLevel(drawPermanentTargetLevel(random, astronaut.getSkill(target)));
        }
        astronaut.setAssignmentState(AssignmentState.IN_TRAINING);
    }

    /**
     * Returns every {@link AstronautSkill} this astronaut could validly
     * start {@link TrainingKind#EXPERT} training in right now: at or above
     * {@link SkillGrowth#EXPERT_TRAINING_SKILL_THRESHOLD}, and not already
     * held as an Expert skill.
     *
     * @param astronaut the astronaut to check
     * @return the eligible skills, possibly empty
     */
    private static List<AstronautSkill> eligibleExpertAstronautSkills(Astronaut astronaut) {
        List<AstronautSkill> eligible = new ArrayList<>();
        for (AstronautSkill skill : AstronautSkill.values()) {
            if (astronaut.getSkill(skill) >= SkillGrowth.EXPERT_TRAINING_SKILL_THRESHOLD
                    && !astronaut.getExpertSkills().contains(skill)) {
                eligible.add(skill);
            }
        }
        return eligible;
    }

    /**
     * The Mission Control equivalent of {@link #eligibleExpertAstronautSkills}.
     *
     * @param officer the officer to check
     * @return the eligible skills, possibly empty
     */
    private static List<MissionControlSkill> eligibleExpertMissionControlSkills(MissionControlOfficer officer) {
        List<MissionControlSkill> eligible = new ArrayList<>();
        for (MissionControlSkill skill : MissionControlSkill.values()) {
            if (officer.getSkill(skill) >= SkillGrowth.EXPERT_TRAINING_SKILL_THRESHOLD
                    && !officer.getExpertSkills().contains(skill)) {
                eligible.add(skill);
            }
        }
        return eligible;
    }

    /**
     * The bot's reassignment move for one SET specialist - same shape as
     * {@link #shuffleAstronaut}/{@link #shuffleMissionControlOfficer}, with
     * one difference: "assigned" only ever means R&amp;D for SET (see
     * {@link AssignmentState#ASSIGNED}'s own Javadoc), so moving idle to
     * assigned requires an owned mission component to target. With none
     * owned yet (no program opened this game so far), falls back to
     * starting training instead - the same path an idle specialist with no
     * assignable work takes. Moving assigned back to idle clears
     * {@link SetSpecialist#getRdTargetComponentId()} too, so the pairing
     * {@link AssignmentState#ASSIGNED} &hArr; a non-null, owned target
     * never drifts apart.
     *
     * <p>Picking a target also means picking one of its four R&amp;D slots
     * ({@code baspm.md} &sect;10.8) - this bot takes the first free slot on
     * a randomly-chosen owned component, and falls back to starting
     * training instead if that component's four slots are all already
     * occupied, the same way "no owned component at all" already falls
     * back.</p>
     *
     * <p>SET is never Expert-eligible, but still uses the same general
     * pull-out path as the other two groups if currently {@code IN_TRAINING} -
     * see {@link #shuffleAstronaut}'s own Javadoc for why pull-out is
     * general, not Expert-specific.</p>
     *
     * @param specialist  the specialist to maybe reassign
     * @param snapshot    the snapshot their owned mission components are
     *                    read from
     * @param random      the random source to draw from
     * @param expertTally mutated in place - see {@link #shuffleAssignments}
     */
    private static void shuffleSetSpecialist(SetSpecialist specialist, PlayerSnapshot snapshot, Random random,
            int[] expertTally) {
        if (specialist.getAssignmentState() == AssignmentState.IN_TRAINING) {
            if (random.nextDouble() < PULL_OUT_OF_TRAINING_CHANCE_PER_SEASON
                    && TrainingProgress.pullOutOfTraining(snapshot, PersonnelGroup.SET, specialist.getId(), random)
                        == PullOutOfTrainingOutcome.PULLED_OUT) {
                expertTally[1]++;
            }
            return;
        }
        if (random.nextDouble() >= REASSIGNMENT_CHANCE_PER_SEASON) {
            return;
        }
        if (specialist.getAssignmentState() == AssignmentState.ASSIGNED) {
            specialist.setAssignmentState(AssignmentState.IDLE);
            specialist.setRdTargetComponentId(null);
            specialist.setRdSlot(null);
            return;
        }
        if (specialist.getAssignmentState() != AssignmentState.IDLE) {
            return;
        }
        // Only an active component can be researched; a closed program's components are idle stock.
        List<String> ownedComponentIds = new ArrayList<>();
        snapshot.getMissionComponents().forEach((id, owned) -> {
            if (owned.isActive()) {
                ownedComponentIds.add(id);
            }
        });
        if (!ownedComponentIds.isEmpty() && random.nextBoolean()) {
            String target = ownedComponentIds.get(random.nextInt(ownedComponentIds.size()));
            Integer freeSlot = firstFreeRdSlot(snapshot, target);
            if (freeSlot != null) {
                specialist.setRdTargetComponentId(target);
                specialist.setRdSlot(freeSlot);
                specialist.setAssignmentState(AssignmentState.ASSIGNED);
                return;
            }
        }
        SetSkill[] skills = SetSkill.values();
        SetSkill target = skills[random.nextInt(skills.length)];
        TrainingKind kind = drawTrainingKind(random, specialist.getSkill(target));
        int total = kind == TrainingKind.BASIC
            ? TrainingProgress.SET_BASIC_TRAINING_SEASONS
            : TrainingProgress.ADVANCED_TRAINING_SEASONS;
        specialist.setTrainingKind(kind);
        specialist.setTrainingSeasonsTotal(total);
        specialist.setTrainingSeasonsCompleted(0);
        specialist.setTrainingTargetSkill(target);
        if (kind == TrainingKind.PERMANENT) {
            specialist.setTrainingTargetLevel(drawPermanentTargetLevel(random, specialist.getSkill(target)));
        }
        specialist.setAssignmentState(AssignmentState.IN_TRAINING);
    }

    /**
     * Returns the lowest-numbered R&amp;D slot not already occupied by
     * another {@code ASSIGNED} specialist targeting the same component, or
     * {@code null} if all {@value MissionComponent#RD_SLOT_COUNT} are
     * taken.
     *
     * @param snapshot    the snapshot whose roster to scan
     * @param componentId the target component's id
     * @return the first free slot index, or {@code null}
     */
    private static Integer firstFreeRdSlot(PlayerSnapshot snapshot, String componentId) {
        boolean[] occupied = new boolean[MissionComponent.RD_SLOT_COUNT];
        for (SetSpecialist other : snapshot.getSetSpecialists()) {
            if (other.getAssignmentState() == AssignmentState.ASSIGNED
                    && componentId.equals(other.getRdTargetComponentId())
                    && other.getRdSlot() != null && other.getRdSlot() >= 0
                    && other.getRdSlot() < MissionComponent.RD_SLOT_COUNT) {
                occupied[other.getRdSlot()] = true;
            }
        }
        for (int slot = 0; slot < occupied.length; slot++) {
            if (!occupied[slot]) {
                return slot;
            }
        }
        return null;
    }

    /**
     * The Mission Control equivalent of {@link #shuffleAstronaut} - same
     * pull-out-first, then Expert-training-start, then fixed-length-pick
     * shape.
     *
     * @param snapshot    the snapshot {@code officer} belongs to
     * @param officer     the officer to maybe reassign
     * @param random      the random source to draw from
     * @param expertTally mutated in place - see {@link #shuffleAssignments}
     */
    private static void shuffleMissionControlOfficer(PlayerSnapshot snapshot, MissionControlOfficer officer,
            Random random, int[] expertTally) {
        if (officer.getAssignmentState() == AssignmentState.IN_TRAINING) {
            if (random.nextDouble() < PULL_OUT_OF_TRAINING_CHANCE_PER_SEASON
                    && TrainingProgress.pullOutOfTraining(snapshot, PersonnelGroup.MISSION_CONTROL, officer.getId(),
                        random) == PullOutOfTrainingOutcome.PULLED_OUT) {
                expertTally[1]++;
            }
            return;
        }
        if (random.nextDouble() >= REASSIGNMENT_CHANCE_PER_SEASON) {
            return;
        }
        // Same reasoning as shuffleAstronaut: ASSIGNED is now exclusively mission-driven.
        if (officer.getAssignmentState() != AssignmentState.IDLE) {
            return;
        }
        List<MissionControlSkill> eligibleExpertSkills = eligibleExpertMissionControlSkills(officer);
        if (!eligibleExpertSkills.isEmpty() && random.nextDouble() < EXPERT_TRAINING_START_CHANCE_PER_SEASON) {
            MissionControlSkill target = eligibleExpertSkills.get(random.nextInt(eligibleExpertSkills.size()));
            if (TrainingProgress.startMissionControlExpertTraining(snapshot, officer.getId(), target)
                    == StartExpertTrainingOutcome.STARTED) {
                expertTally[0]++;
            }
            return;
        }
        if (random.nextBoolean()) {
            return;
        }
        MissionControlSkill[] skills = MissionControlSkill.values();
        MissionControlSkill target = skills[random.nextInt(skills.length)];
        TrainingKind kind = drawTrainingKind(random, officer.getSkill(target));
        int total = kind == TrainingKind.BASIC
            ? TrainingProgress.MISSION_CONTROL_BASIC_TRAINING_SEASONS
            : TrainingProgress.ADVANCED_TRAINING_SEASONS;
        officer.setTrainingKind(kind);
        officer.setTrainingSeasonsTotal(total);
        officer.setTrainingSeasonsCompleted(0);
        officer.setTrainingTargetSkill(target);
        if (kind == TrainingKind.PERMANENT) {
            officer.setTrainingTargetLevel(drawPermanentTargetLevel(random, officer.getSkill(target)));
        }
        officer.setAssignmentState(AssignmentState.IN_TRAINING);
    }

    /**
     * Picks which kind of training a newly-started assignment uses -
     * evenly between {@link TrainingKind#ADVANCED} and
     * {@link TrainingKind#PERMANENT} (basic training only follows a hire,
     * user decision 2026-10-05), except {@code PERMANENT} is
     * skipped (falls back to {@code ADVANCED}) when the target skill is
     * already too close to {@link SkillGrowth#SKILL_CAP} to leave any
     * meaningful room for a target level above it.
     *
     * @param random       the random source to draw from
     * @param currentSkill the target skill's current value
     * @return the drawn training kind
     */
    private static TrainingKind drawTrainingKind(Random random, double currentSkill) {
        if (random.nextBoolean() || SkillGrowth.SKILL_CAP - currentSkill < 1.0) {
            return TrainingKind.ADVANCED;
        }
        return TrainingKind.PERMANENT;
    }

    /**
     * Picks a target level for a newly-started {@link TrainingKind#PERMANENT}
     * assignment - somewhere strictly between the current skill and
     * {@link SkillGrowth#SKILL_CAP}, per {@code design.md} &sect;3.4's
     * proposed harness invariant.
     *
     * @param random       the random source to draw from
     * @param currentSkill the target skill's current value
     * @return the drawn target level
     */
    private static double drawPermanentTargetLevel(Random random, double currentSkill) {
        double headroom = SkillGrowth.SKILL_CAP - currentSkill;
        return currentSkill + random.nextDouble() * headroom;
    }

    private static PlayerSnapshot newRandomSnapshot(int employeesPerGroup, Random random) {
        PlayerSnapshot snapshot = new PlayerSnapshot("Bot Agency", Archetype.NASA, STARTING_FUNDS,
            STARTING_PRESTIGE, STARTING_FUNDING_RATE, 1);
        List<Astronaut> astronauts = new ArrayList<>();
        List<SetSpecialist> specialists = new ArrayList<>();
        List<MissionControlOfficer> officers = new ArrayList<>();
        for (int i = 0; i < employeesPerGroup; i++) {
            Astronaut astronaut = new Astronaut("a" + i, "Astronaut " + i,
                randomAge(random), randomMorale(random), randomLearningPotential(random));
            astronaut.setSalary(randomSalary(random));
            for (AstronautSkill skill : AstronautSkill.values()) {
                astronaut.setSkill(skill, randomStartingSkill(random));
            }
            astronauts.add(astronaut);

            SetSpecialist specialist = new SetSpecialist("s" + i, "Specialist " + i,
                randomAge(random), randomMorale(random), randomLearningPotential(random));
            specialist.setSalary(randomSalary(random));
            for (SetSkill skill : SetSkill.values()) {
                specialist.setSkill(skill, randomStartingSkill(random));
            }
            specialists.add(specialist);

            MissionControlOfficer officer = new MissionControlOfficer("m" + i, "Officer " + i,
                randomAge(random), randomMorale(random), randomLearningPotential(random));
            officer.setSalary(randomSalary(random));
            for (MissionControlSkill skill : MissionControlSkill.values()) {
                officer.setSkill(skill, randomStartingSkill(random));
            }
            officers.add(officer);
        }
        snapshot.setAstronauts(astronauts);
        snapshot.setSetSpecialists(specialists);
        snapshot.setMissionControlOfficers(officers);
        return snapshot;
    }

    /**
     * Draws one employee's per-season salary from BASPM's own 25-60 range, the same
     * range {@link HiringPool} draws candidates from ({@code design.md} &sect;2.1(d)).
     *
     * @param random the bot's random source
     * @return the salary, whole units
     */
    private static double randomSalary(Random random) {
        return HiringPool.SALARY_MIN + random.nextInt(HiringPool.SALARY_MAX - HiringPool.SALARY_MIN + 1);
    }

    private static int randomAge(Random random) {
        return MIN_START_AGE + random.nextInt(MAX_START_AGE - MIN_START_AGE + 1);
    }

    private static int randomMorale(Random random) {
        return MIN_START_MORALE + random.nextInt(MAX_START_MORALE - MIN_START_MORALE + 1);
    }

    private static int randomLearningPotential(Random random) {
        return MIN_START_LEARNING_POTENTIAL + random.nextInt(MAX_START_LEARNING_POTENTIAL - MIN_START_LEARNING_POTENTIAL + 1);
    }

    private static double randomStartingSkill(Random random) {
        return MIN_START_SKILL + random.nextDouble() * (MAX_START_SKILL - MIN_START_SKILL);
    }

    private static int rosterSize(PlayerSnapshot snapshot, PersonnelGroup group) {
        return switch (group) {
            case ASTRONAUT -> snapshot.getAstronauts().size();
            case SET -> snapshot.getSetSpecialists().size();
            case MISSION_CONTROL -> snapshot.getMissionControlOfficers().size();
        };
    }

    private static int rosterSize(PlayerSnapshot snapshot) {
        return snapshot.getAstronauts().size() + snapshot.getSetSpecialists().size()
            + snapshot.getMissionControlOfficers().size();
    }

    /**
     * Runs a default-sized playout and prints its report - a quick way to
     * eyeball the current tuning (see {@code design.md} &sect;2.2(c)'s
     * "unconfirmed default" numbers) without writing a test.
     *
     * @param args ignored
     */
    public static void main(String[] args) {
        PersonnelPlayoutReport report = run(200, 5, 80, 42L);

        System.out.printf("Games: %d, seasons/game: %d, employees started: %d%n",
            report.gamesPlayed(), report.seasonsPerGame(), report.employeesStarted());
        System.out.printf("Resigned: %d (avg tenure %.1f seasons)%n",
            report.resignedCount(), report.averageTenureSeasonsWhenResigned());
        System.out.printf("Retired: %d (avg tenure %.1f seasons)%n",
            report.retiredCount(), report.averageTenureSeasonsWhenRetired());
        System.out.printf("Still employed at end: %d%n", report.stillEmployedAtEnd());
        System.out.printf("Training completions: %d (avg gain %.2f)%n",
            report.trainingCompletedCount(), report.averageTrainingGain());
        System.out.printf("Hired: %d%n", report.hiredCount());
        System.out.printf("Hiring-pool band hits - poor: %d, average: %d, good: %d, excellent: %d%n",
            report.poorBandBatches(), report.averageBandBatches(), report.goodBandBatches(),
            report.excellentBandBatches());
        System.out.printf("Programs opened: %d%n", report.programsOpenedCount());
        System.out.printf("R&D seasons completed: %d (avg reliability gain %.2f)%n",
            report.researchCompletedCount(), report.averageResearchReliabilityGain());
        System.out.printf("Missions launched: %d%n", report.missionsLaunchedCount());
        System.out.printf(
            "Mission steps - successful: %d, minor glitch solved: %d, minor glitch: %d, failure: %d, catastrophic: %d%n",
            report.missionStepSuccessfulCount(), report.missionStepMinorGlitchSolvedCount(),
            report.missionStepMinorGlitchCount(), report.missionStepFailureCount(),
            report.missionStepCatastrophicCount());
        System.out.printf("Astronauts lost in accidents: %d%n", report.lostInAccidentCount());
        System.out.printf("Retired on mission release (of total retired): %d%n",
            report.retiredOnMissionReleaseCount());
        System.out.printf("Mission reliability changes: %d (avg delta %.2f)%n",
            report.missionReliabilityChangeCount(), report.averageMissionReliabilityDelta());
        System.out.printf("Mission skill growth events: %d (avg gain %.2f)%n",
            report.missionSkillGrowthCount(), report.averageMissionSkillGrowth());
        System.out.printf("SET specialists released via mission-triggered R&D ceiling: %d%n",
            report.missionTriggeredSetReleaseCount());
        System.out.printf("Expert training started: %d (completed: %d)%n",
            report.expertTrainingStartedCount(), report.expertTrainingCompletedCount());
        System.out.printf("Pulled out of training: %d%n", report.pulledOutOfTrainingCount());
        System.out.printf("Expert rescues applied (nonzero boost): %d%n", report.expertRescueAppliedCount());
        System.out.printf("Prestige paid / deducted: %d / %d%n", report.prestigePaidTotal(),
            report.prestigeDeductedTotal());
        System.out.printf("Completed flights (advance repeat tier): %d%n", report.completedFlightCount());
        System.out.printf("Funding reviews: %d; debt conversions: %d%n", report.reviewCount(), report.debtConversionCount());
        System.out.printf("Employees removed by events (dismissed or failed training): %d%n", report.dismissedByEventCount());
        report.randomEventCounts().forEach((kind, count) -> System.out.printf("  %s: %d%n", kind, count));
        System.out.printf("Building upgrades started / completed: %d / %d%n", report.buildingUpgradesStartedCount(),
            report.buildingUpgradesCompletedCount());
        System.out.printf("Programs closed: %d%n", report.programsClosedCount());
    }
}
