package de.mkoehler.launchwindow.devtools.analysis;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.content.MissionSeason;
import de.mkoehler.launchwindow.content.MissionStep;
import de.mkoehler.launchwindow.content.Program;
import de.mkoehler.launchwindow.content.ProgramCategory;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.AstronautSkill;
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
import de.mkoehler.launchwindow.rules.HireOutcome;
import de.mkoehler.launchwindow.rules.HiringPool;
import de.mkoehler.launchwindow.rules.MissionLaunch;
import de.mkoehler.launchwindow.rules.MissionLaunchOutcome;
import de.mkoehler.launchwindow.rules.MissionProgress;
import de.mkoehler.launchwindow.rules.MissionStepOutcome;
import de.mkoehler.launchwindow.rules.MissionStepResolved;
import de.mkoehler.launchwindow.rules.OpenProgramOutcome;
import de.mkoehler.launchwindow.rules.PersonnelDeparture;
import de.mkoehler.launchwindow.rules.PersonnelGroup;
import de.mkoehler.launchwindow.rules.PersonnelLifecycle;
import de.mkoehler.launchwindow.rules.ProgramOpening;
import de.mkoehler.launchwindow.rules.ResearchCompleted;
import de.mkoehler.launchwindow.rules.ResearchProgress;
import de.mkoehler.launchwindow.rules.ResolutionEvent;
import de.mkoehler.launchwindow.rules.SkillGrowth;
import de.mkoehler.launchwindow.rules.TrainingCompleted;
import de.mkoehler.launchwindow.rules.TrainingProgress;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
 * employee's own group, and evenly among {@link TrainingKind#BASIC}/
 * {@link TrainingKind#ADVANCED}/{@link TrainingKind#PERMANENT} (the last
 * with a random target level strictly above the current skill and up to
 * {@link SkillGrowth#SKILL_CAP}, per {@code design.md} &sect;3.4's proposed
 * invariant); an employee already in training is <b>never</b> interrupted -
 * there is no pull-out path yet, so training always runs to its own
 * completion (for {@code PERMANENT}, until it stops re-enrolling on its
 * own). <b>An Astronaut/Mission Control officer's idle-to-{@code ASSIGNED}
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
    private static final double OPEN_PROGRAM_CHANCE_PER_SEASON = 0.1;
    private static final double LAUNCH_MISSION_CHANCE_PER_SEASON = 0.15;
    private static final long STARTING_FUNDS = 5_000;

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
     * {@code CATASTROPHIC} crew loss.</p>
     *
     * @return the fixture catalog
     */
    private static ContentCatalog buildCatalog() {
        ProgramCategory category = new ProgramCategory("early-space", "Early Space");

        MissionComponent satelliteSputnik = new MissionComponent("satellite-sputnik", SetSkill.SPACE_PROBES, 3, 90,
            82, RD_SLOT_BANDS, 0, List.of());
        MissionComponent satelliteExplorer = new MissionComponent("satellite-explorer", SetSkill.SPACE_PROBES, 3, 90,
            82, RD_SLOT_BANDS, 40, List.of(new MissionComponent.ReliabilityTransferSource("satellite-sputnik", 40)));
        MissionComponent rocketRedstone = new MissionComponent("rocket-redstone", SetSkill.ROCKETS, 3, 95, 87,
            RD_SLOT_BANDS, 0, List.of());
        MissionComponent capsuleMercury = new MissionComponent("capsule-mercury", SetSkill.CREWED_SPACECRAFT, 3, 90,
            82, RD_SLOT_BANDS, 50, List.of(new MissionComponent.ReliabilityTransferSource("rocket-redstone", 50)));

        Program sputnikProgram =
            new Program("sputnik-program", "early-space", 500, 0, List.of("satellite-sputnik"), List.of());
        Program explorerProgram = new Program("explorer-program", "early-space", 600, 0,
            List.of("satellite-explorer"), List.of(new Program.OpeningCostDiscount("sputnik-program", 40)));
        Program mercuryProgram = new Program("mercury-program", "early-space", 1_200, 0,
            List.of("rocket-redstone", "capsule-mercury"), List.of());

        MissionStep sputnikLaunchStep = new MissionStep("launch",
            List.of(new MissionStep.ComponentInvolvement("satellite-sputnik", 100)),
            List.of(new MissionStep.ControllerInvolvement(MissionControlSkill.PROPULSION, 60),
                new MissionStep.ControllerInvolvement(MissionControlSkill.TRAJECTORY_AND_GNC, 40)),
            List.of(), false);
        MissionConfiguration sputnikLaunchConfiguration = new MissionConfiguration("sputnik-launch-std",
            "sputnik-program", List.of(new MissionSeason(List.of(sputnikLaunchStep))), 0, 1);

        MissionStep explorerLaunchStep = new MissionStep("launch",
            List.of(new MissionStep.ComponentInvolvement("satellite-explorer", 100)),
            List.of(new MissionStep.ControllerInvolvement(MissionControlSkill.PROPULSION, 50),
                new MissionStep.ControllerInvolvement(MissionControlSkill.MISSION_OPERATIONS, 50)),
            List.of(), false);
        MissionStep explorerOrbitOpsStep = new MissionStep("orbit-ops",
            List.of(new MissionStep.ComponentInvolvement("satellite-explorer", 100)),
            List.of(new MissionStep.ControllerInvolvement(MissionControlSkill.MISSION_OPERATIONS, 100)),
            List.of(), false);
        MissionConfiguration explorerMissionConfiguration = new MissionConfiguration("explorer-mission-std",
            "explorer-program",
            List.of(new MissionSeason(List.of(explorerLaunchStep)), new MissionSeason(List.of(explorerOrbitOpsStep))),
            0, 1);

        MissionStep mercuryLaunchStep = new MissionStep("launch",
            List.of(new MissionStep.ComponentInvolvement("rocket-redstone", 60),
                new MissionStep.ComponentInvolvement("capsule-mercury", 40)),
            List.of(new MissionStep.ControllerInvolvement(MissionControlSkill.PROPULSION, 50),
                new MissionStep.ControllerInvolvement(MissionControlSkill.TRAJECTORY_AND_GNC, 50)),
            List.of(new MissionStep.CrewInvolvement(AstronautSkill.PILOTING, 70),
                new MissionStep.CrewInvolvement(AstronautSkill.FITNESS, 30)), true);
        MissionStep mercuryOrbitOpsStep = new MissionStep("orbit-ops",
            List.of(new MissionStep.ComponentInvolvement("capsule-mercury", 100)),
            List.of(new MissionStep.ControllerInvolvement(MissionControlSkill.SPACECRAFT_SYSTEMS, 100)),
            List.of(new MissionStep.CrewInvolvement(AstronautSkill.SCIENCE, 100)), true);
        MissionConfiguration mercuryMissionConfiguration = new MissionConfiguration("mercury-mission-std",
            "mercury-program",
            List.of(new MissionSeason(List.of(mercuryLaunchStep)), new MissionSeason(List.of(mercuryOrbitOpsStep))),
            2, 1);

        return new ContentCatalog(List.of(category), List.of(sputnikProgram, explorerProgram, mercuryProgram),
            List.of(sputnikLaunchConfiguration, explorerMissionConfiguration, mercuryMissionConfiguration),
            List.of(satelliteSputnik, satelliteExplorer, rocketRedstone, capsuleMercury));
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
        int missionStepFailureCount = 0;
        int missionStepCatastrophicCount = 0;
        int lostInAccidentCount = 0;
        int retiredOnMissionReleaseCount = 0;
        int[] bandHits = new int[4];

        for (int game = 0; game < games; game++) {
            PlayerSnapshot snapshot = newRandomSnapshot(employeesPerGroupPerGame, random);
            employeesStarted += rosterSize(snapshot);
            Map<String, Integer> joinedAtSeason = new HashMap<>();
            seedJoinedAtSeason(snapshot, joinedAtSeason);

            for (int season = 0; season < seasons && rosterSize(snapshot) > 0; season++) {
                shuffleAssignments(snapshot, random);
                hiredCount += hireFromPools(snapshot, random, season, joinedAtSeason);
                programsOpenedCount += maybeOpenProgram(snapshot, random);
                missionsLaunchedCount += maybeLaunchMission(snapshot, random);
                checkProgramInvariants(snapshot);

                int before = rosterSize(snapshot);
                Set<String> protectedBefore = protectedEmployeeKeys(snapshot);

                List<ResolutionEvent> lifecycleEvents = PersonnelLifecycle.advanceSeason(snapshot);
                checkInvariants(snapshot, lifecycleEvents, before, protectedBefore);

                int beforeMissionProgress = rosterSize(snapshot);
                List<ResolutionEvent> missionEvents = MissionProgress.advanceSeason(snapshot, CATALOG, random);
                checkMissionInvariants(snapshot, missionEvents, beforeMissionProgress);

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
                    }
                }
                for (ResolutionEvent event : trainingEvents) {
                    trainingCompletedCount++;
                    trainingGainTotal += ((TrainingCompleted) event).skillGain();
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

        int stillEmployed = employeesStarted + hiredCount - resignedCount - retiredCount - lostInAccidentCount;
        double avgResignedTenure = resignedCount == 0 ? 0 : (double) resignedTenureTotal / resignedCount;
        double avgRetiredTenure = retiredCount == 0 ? 0 : (double) retiredTenureTotal / retiredCount;
        double avgTrainingGain = trainingCompletedCount == 0 ? 0 : trainingGainTotal / trainingCompletedCount;
        double avgResearchReliabilityGain =
            researchCompletedCount == 0 ? 0 : researchReliabilityGainTotal / researchCompletedCount;

        return new PersonnelPlayoutReport(games, seasons, employeesStarted, resignedCount, retiredCount,
            stillEmployed, avgResignedTenure, avgRetiredTenure, trainingCompletedCount, avgTrainingGain,
            hiredCount, bandHits[0], bandHits[1], bandHits[2], bandHits[3], programsOpenedCount,
            researchCompletedCount, avgResearchReliabilityGain, missionsLaunchedCount, missionStepSuccessfulCount,
            missionStepMinorGlitchSolvedCount, missionStepMinorGlitchCount, missionStepFailureCount,
            missionStepCatastrophicCount, lostInAccidentCount, retiredOnMissionReleaseCount);
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
        HireOutcome outcome = HiringPool.hire(snapshot, group, candidateId, random);
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
        OpenProgramOutcome outcome = ProgramOpening.open(snapshot, programId, CATALOG);
        return outcome == OpenProgramOutcome.OPENED ? 1 : 0;
    }

    /**
     * The bot's stand-in for a journaled "launch mission" player action:
     * with probability {@link #LAUNCH_MISSION_CHANCE_PER_SEASON}, picks one
     * random mission configuration whose own program is currently open,
     * and attempts to launch it with exactly
     * {@link MissionConfiguration#requiredAstronautCount()}/
     * {@link MissionConfiguration#requiredMissionControlOfficerCount()}
     * randomly-picked currently idle astronauts/Mission Control officers.
     * A no-op (not an error) if no program is open yet, not enough idle
     * personnel exist to meet either exact count, or
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

        List<String> astronautIds =
            pickExactlyIdleIds(snapshot.getAstronauts(), configuration.requiredAstronautCount(), random);
        if (astronautIds == null) {
            return 0;
        }
        List<String> controllerIds = pickExactlyIdleIds(snapshot.getMissionControlOfficers(),
            configuration.requiredMissionControlOfficerCount(), random);
        if (controllerIds == null) {
            return 0;
        }

        MissionLaunchOutcome outcome =
            MissionLaunch.launch(snapshot, configurationId, astronautIds, controllerIds, CATALOG);
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
            if (person.getAssignmentState() == AssignmentState.IDLE) {
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
     * than trusted blindly.
     *
     * @param snapshot       the snapshot after this season's training advance
     * @param trainingEvents the {@link TrainingProgress} events this season's advance returned
     * @throws IllegalStateException if any invariant is violated
     */
    private static void checkTrainingInvariants(PlayerSnapshot snapshot, List<ResolutionEvent> trainingEvents) {
        for (ResolutionEvent event : trainingEvents) {
            double gain = ((TrainingCompleted) event).skillGain();
            if (gain < 0) {
                throw new IllegalStateException("Negative training gain: " + gain);
            }
        }
        for (Astronaut astronaut : snapshot.getAstronauts()) {
            checkTrainingConsistency(astronaut, astronaut.getTrainingTargetSkill());
            checkSkillsInRange(astronaut.getId(), astronaut.getSkills().values());
            if (astronaut.getTrainingKind() == TrainingKind.PERMANENT) {
                checkPermanentTrainingTarget(astronaut, astronaut.getSkill(astronaut.getTrainingTargetSkill()));
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
     * {@link MissionProgress} has), and {@link #checkMissionAssignmentConsistency}
     * holds.
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
            }
        }
        int rosterSizeAfter = rosterSize(snapshot);
        if (rosterSizeBeforeMissionProgress - departureCount != rosterSizeAfter) {
            throw new IllegalStateException("Roster size change does not match the number of reported mission departures");
        }

        checkMissionAssignmentConsistency(snapshot);
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
            checkMissionSideGroup(mission.getId(), mission.getAssignedAstronautIds(), snapshot.getAstronauts(),
                claimedPersonnelIds);
            checkMissionSideGroup(mission.getId(), mission.getAssignedMissionControlOfficerIds(),
                snapshot.getMissionControlOfficers(), claimedPersonnelIds);

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
            if (mission.getAssignedAstronautIds().size() != configuration.requiredAstronautCount()) {
                throw new IllegalStateException("Mission " + mission.getId() + " assigned astronaut count "
                    + mission.getAssignedAstronautIds().size() + " does not match its own configuration's required "
                    + configuration.requiredAstronautCount());
            }
            if (mission.getAssignedMissionControlOfficerIds().size() != configuration
                    .requiredMissionControlOfficerCount()) {
                throw new IllegalStateException("Mission " + mission.getId()
                    + " assigned Mission Control officer count "
                    + mission.getAssignedMissionControlOfficerIds().size()
                    + " does not match its own configuration's required "
                    + configuration.requiredMissionControlOfficerCount());
            }
        }
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
        if (snapshot.getFunds() < 0) {
            throw new IllegalStateException("Funds went negative: " + snapshot.getFunds());
        }

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
            if (!requiredByAnOpenedProgram.contains(componentId)) {
                throw new IllegalStateException(
                    "Owns " + componentId + " but no currently opened program requires it");
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
            checkSkillsInBand(candidate.getId(), candidate.getSkills().values(), astronautBand);
        }

        int setBand = bandIndex(independentAverageMorale(snapshot.getSetSpecialists()));
        bandHits[setBand]++;
        for (SetSpecialist candidate : snapshot.getSetSpecialistCandidates()) {
            checkSkillsInBand(candidate.getId(), candidate.getSkills().values(), setBand);
        }

        int missionControlBand = bandIndex(independentAverageMorale(snapshot.getMissionControlOfficers()));
        bandHits[missionControlBand]++;
        for (MissionControlOfficer candidate : snapshot.getMissionControlCandidates()) {
            checkSkillsInBand(candidate.getId(), candidate.getSkills().values(), missionControlBand);
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

    private static void checkSkillsInBand(String candidateId, Iterable<Double> skills, int band) {
        double min = HIRING_SKILL_RANGES[band][0];
        double max = HIRING_SKILL_RANGES[band][1];
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

    private static void shuffleAssignments(PlayerSnapshot snapshot, Random random) {
        snapshot.getAstronauts().forEach(a -> shuffleAstronaut(a, random));
        snapshot.getSetSpecialists().forEach(s -> shuffleSetSpecialist(s, snapshot, random));
        snapshot.getMissionControlOfficers().forEach(m -> shuffleMissionControlOfficer(m, random));
    }

    private static void shuffleAstronaut(Astronaut astronaut, Random random) {
        if (random.nextDouble() >= REASSIGNMENT_CHANCE_PER_SEASON) {
            return;
        }
        // ASSIGNED is now exclusively mission-driven - MissionLaunch is the only path in
        // (see maybeLaunchMission), MissionProgress.advanceSeason the only path back to IDLE on
        // a mission's end. This shuffle only ever decides idle <-> training.
        if (astronaut.getAssignmentState() != AssignmentState.IDLE) {
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
     * @param specialist the specialist to maybe reassign
     * @param snapshot   the snapshot their owned mission components are
     *                   read from
     * @param random     the random source to draw from
     */
    private static void shuffleSetSpecialist(SetSpecialist specialist, PlayerSnapshot snapshot, Random random) {
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
        List<String> ownedComponentIds = new ArrayList<>(snapshot.getMissionComponents().keySet());
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

    private static void shuffleMissionControlOfficer(MissionControlOfficer officer, Random random) {
        if (random.nextDouble() >= REASSIGNMENT_CHANCE_PER_SEASON) {
            return;
        }
        // Same reasoning as shuffleAstronaut: ASSIGNED is now exclusively mission-driven.
        if (officer.getAssignmentState() != AssignmentState.IDLE) {
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
     * evenly among {@link TrainingKind#BASIC}, {@link TrainingKind#ADVANCED},
     * and {@link TrainingKind#PERMANENT}, except {@code PERMANENT} is
     * skipped (falls back to {@code ADVANCED}) when the target skill is
     * already too close to {@link SkillGrowth#SKILL_CAP} to leave any
     * meaningful room for a target level above it.
     *
     * @param random       the random source to draw from
     * @param currentSkill the target skill's current value
     * @return the drawn training kind
     */
    private static TrainingKind drawTrainingKind(Random random, double currentSkill) {
        int choice = random.nextInt(3);
        if (choice == 2 && SkillGrowth.SKILL_CAP - currentSkill < 1.0) {
            return TrainingKind.ADVANCED;
        }
        return switch (choice) {
            case 0 -> TrainingKind.BASIC;
            case 1 -> TrainingKind.ADVANCED;
            default -> TrainingKind.PERMANENT;
        };
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
        PlayerSnapshot snapshot = new PlayerSnapshot("Bot Agency", Archetype.NASA, STARTING_FUNDS, 0, 0, 1);
        List<Astronaut> astronauts = new ArrayList<>();
        List<SetSpecialist> specialists = new ArrayList<>();
        List<MissionControlOfficer> officers = new ArrayList<>();
        for (int i = 0; i < employeesPerGroup; i++) {
            Astronaut astronaut = new Astronaut("a" + i, "Astronaut " + i,
                randomAge(random), randomMorale(random), randomLearningPotential(random));
            for (AstronautSkill skill : AstronautSkill.values()) {
                astronaut.setSkill(skill, randomStartingSkill(random));
            }
            astronauts.add(astronaut);

            SetSpecialist specialist = new SetSpecialist("s" + i, "Specialist " + i,
                randomAge(random), randomMorale(random), randomLearningPotential(random));
            for (SetSkill skill : SetSkill.values()) {
                specialist.setSkill(skill, randomStartingSkill(random));
            }
            specialists.add(specialist);

            MissionControlOfficer officer = new MissionControlOfficer("m" + i, "Officer " + i,
                randomAge(random), randomMorale(random), randomLearningPotential(random));
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
    }
}
