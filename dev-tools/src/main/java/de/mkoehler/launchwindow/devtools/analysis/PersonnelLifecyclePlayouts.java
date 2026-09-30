package de.mkoehler.launchwindow.devtools.analysis;

import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.AstronautSkill;
import de.mkoehler.launchwindow.model.MissionControlOfficer;
import de.mkoehler.launchwindow.model.MissionControlSkill;
import de.mkoehler.launchwindow.model.Personnel;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSkill;
import de.mkoehler.launchwindow.model.SetSpecialist;
import de.mkoehler.launchwindow.model.TrainingKind;
import de.mkoehler.launchwindow.rules.DepartureReason;
import de.mkoehler.launchwindow.rules.HireOutcome;
import de.mkoehler.launchwindow.rules.HiringPool;
import de.mkoehler.launchwindow.rules.PersonnelDeparture;
import de.mkoehler.launchwindow.rules.PersonnelGroup;
import de.mkoehler.launchwindow.rules.PersonnelLifecycle;
import de.mkoehler.launchwindow.rules.ResolutionEvent;
import de.mkoehler.launchwindow.rules.SkillGrowth;
import de.mkoehler.launchwindow.rules.TrainingCompleted;
import de.mkoehler.launchwindow.rules.TrainingProgress;

import java.util.ArrayList;
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
 * and {@link HiringPool#refreshCandidates}/{@link HiringPool#hire}, the
 * rules-engine resolvers that exist so far, across many random simulated
 * rosters and seasons, purely headless (no client/server/persistence
 * involved), and reports the tenure/departure/training/hiring statistics
 * that make it a tuning tool rather than just another unit test.</p>
 *
 * <p><b>Deliberately narrow, named after the resolvers it drives</b> - this
 * is not yet the general full-game {@code Playouts} harness
 * {@code design.md} &sect;3.10 describes (that needs a real bot making
 * per-season decisions like "queue a mission" or "open a program," none of
 * which exist yet). Expected to grow alongside M2's remaining slices
 * ({@code design.md} &sect;5), most likely folding into a more general
 * harness once there are resolvers for those too, rather than staying its
 * own permanent, resolver-specific class.</p>
 *
 * <p>The "bot" here is intentionally simple, per employee per season
 * (chance {@link #REASSIGNMENT_CHANCE_PER_SEASON}): idle becomes either
 * assigned, or starts a real training assignment - a random target skill
 * from that employee's own group, and evenly among {@link TrainingKind#BASIC}/
 * {@link TrainingKind#ADVANCED}/{@link TrainingKind#PERMANENT} (the last
 * with a random target level strictly above the current skill and up to
 * {@link SkillGrowth#SKILL_CAP}, per {@code design.md} &sect;3.4's proposed
 * invariant); assigned becomes idle; an employee already in training is
 * <b>never</b> interrupted - there is no pull-out path yet, so training
 * always runs to its own completion (for {@code PERMANENT}, until it stops
 * re-enrolling on its own). Not a stand-in for a real per-turn player
 * decision, which doesn't exist yet at this milestone.</p>
 *
 * <p><b>Resolver composition order matches the contract {@link HiringPool}'s
 * own Javadoc documents</b>: each season, the bot may first hire out of
 * <i>last</i> season's candidate pool ({@link #HIRE_CHANCE_PER_SEASON} per
 * group, a stand-in for a journaled player action), then
 * {@link PersonnelLifecycle} runs, then {@link TrainingProgress}, then
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

    /**
     * Not instantiable; this class only exposes static methods.
     */
    private PersonnelLifecyclePlayouts() {
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
        int[] bandHits = new int[4];

        for (int game = 0; game < games; game++) {
            PlayerSnapshot snapshot = newRandomSnapshot(employeesPerGroupPerGame, random);
            employeesStarted += rosterSize(snapshot);
            Map<String, Integer> joinedAtSeason = new HashMap<>();
            seedJoinedAtSeason(snapshot, joinedAtSeason);

            for (int season = 0; season < seasons && rosterSize(snapshot) > 0; season++) {
                shuffleAssignments(snapshot, random);
                hiredCount += hireFromPools(snapshot, random, season, joinedAtSeason);

                int before = rosterSize(snapshot);
                Set<String> protectedBefore = protectedEmployeeKeys(snapshot);

                List<ResolutionEvent> lifecycleEvents = PersonnelLifecycle.advanceSeason(snapshot);
                List<ResolutionEvent> trainingEvents = TrainingProgress.advanceSeason(snapshot, random);
                checkInvariants(snapshot, lifecycleEvents, before, protectedBefore);
                checkTrainingInvariants(snapshot, trainingEvents);

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
                for (ResolutionEvent event : trainingEvents) {
                    trainingCompletedCount++;
                    trainingGainTotal += ((TrainingCompleted) event).skillGain();
                }

                HiringPool.refreshCandidates(snapshot, random);
                checkHiringInvariants(snapshot, bandHits);
            }
        }

        int stillEmployed = employeesStarted + hiredCount - resignedCount - retiredCount;
        double avgResignedTenure = resignedCount == 0 ? 0 : (double) resignedTenureTotal / resignedCount;
        double avgRetiredTenure = retiredCount == 0 ? 0 : (double) retiredTenureTotal / retiredCount;
        double avgTrainingGain = trainingCompletedCount == 0 ? 0 : trainingGainTotal / trainingCompletedCount;

        return new PersonnelPlayoutReport(games, seasons, employeesStarted, resignedCount, retiredCount,
            stillEmployed, avgResignedTenure, avgRetiredTenure, trainingCompletedCount, avgTrainingGain,
            hiredCount, bandHits[0], bandHits[1], bandHits[2], bandHits[3]);
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
        snapshot.getSetSpecialists().forEach(s -> shuffleSetSpecialist(s, random));
        snapshot.getMissionControlOfficers().forEach(m -> shuffleMissionControlOfficer(m, random));
    }

    private static void shuffleAstronaut(Astronaut astronaut, Random random) {
        if (random.nextDouble() >= REASSIGNMENT_CHANCE_PER_SEASON) {
            return;
        }
        if (astronaut.getAssignmentState() == AssignmentState.ASSIGNED) {
            astronaut.setAssignmentState(AssignmentState.IDLE);
            return;
        }
        if (astronaut.getAssignmentState() != AssignmentState.IDLE) {
            return;
        }
        if (random.nextBoolean()) {
            astronaut.setAssignmentState(AssignmentState.ASSIGNED);
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

    private static void shuffleSetSpecialist(SetSpecialist specialist, Random random) {
        if (random.nextDouble() >= REASSIGNMENT_CHANCE_PER_SEASON) {
            return;
        }
        if (specialist.getAssignmentState() == AssignmentState.ASSIGNED) {
            specialist.setAssignmentState(AssignmentState.IDLE);
            return;
        }
        if (specialist.getAssignmentState() != AssignmentState.IDLE) {
            return;
        }
        if (random.nextBoolean()) {
            specialist.setAssignmentState(AssignmentState.ASSIGNED);
            return;
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

    private static void shuffleMissionControlOfficer(MissionControlOfficer officer, Random random) {
        if (random.nextDouble() >= REASSIGNMENT_CHANCE_PER_SEASON) {
            return;
        }
        if (officer.getAssignmentState() == AssignmentState.ASSIGNED) {
            officer.setAssignmentState(AssignmentState.IDLE);
            return;
        }
        if (officer.getAssignmentState() != AssignmentState.IDLE) {
            return;
        }
        if (random.nextBoolean()) {
            officer.setAssignmentState(AssignmentState.ASSIGNED);
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
        PlayerSnapshot snapshot = new PlayerSnapshot("Bot Agency", Archetype.NASA, 0, 0, 0, 1);
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
    }
}
