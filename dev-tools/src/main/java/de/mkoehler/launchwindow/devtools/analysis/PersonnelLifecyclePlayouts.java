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
import de.mkoehler.launchwindow.rules.PersonnelDeparture;
import de.mkoehler.launchwindow.rules.PersonnelGroup;
import de.mkoehler.launchwindow.rules.PersonnelLifecycle;
import de.mkoehler.launchwindow.rules.ResolutionEvent;
import de.mkoehler.launchwindow.rules.SkillGrowth;
import de.mkoehler.launchwindow.rules.TrainingCompleted;
import de.mkoehler.launchwindow.rules.TrainingProgress;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * <p>Launch Window's own {@code Playouts}-style bot-driven harness (see
 * {@code design.md} &sect;3.10, following Robot-Rampage's own precedent) -
 * drives {@link PersonnelLifecycle#advanceSeason} and
 * {@link TrainingProgress#advanceSeason}, the two rules-engine resolvers
 * that exist so far, across many random simulated rosters and seasons,
 * purely headless (no client/server/persistence involved), and reports the
 * tenure/departure/training statistics that make it a tuning tool rather
 * than just another unit test.</p>
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
 * assigned, or starts a real fixed-length training round (a random kind, a
 * random target skill from that employee's own group); assigned becomes
 * idle; an employee already in training is <b>never</b> interrupted - there
 * is no pull-out path yet, so training always runs to its own completion.
 * Not a stand-in for a real per-turn player decision, which doesn't exist
 * yet at this milestone.</p>
 *
 * <p><b>Resolver composition order matches the contract {@link TrainingProgress}'s
 * own Javadoc documents</b>: {@link PersonnelLifecycle} runs before
 * {@link TrainingProgress} every season, using the same seeded
 * {@link Random} for both this season's assignment shuffle and any
 * training-completion gain it draws - reusing one source keeps a whole
 * game's outcome reproducible from a single seed.</p>
 *
 * @author Mario Koehler
 */
public final class PersonnelLifecyclePlayouts {

    private static final int MIN_START_AGE = 24;
    private static final int MAX_START_AGE = 45;
    private static final int MIN_START_MORALE = 50;
    private static final int MAX_START_MORALE = 90;
    private static final int STARTING_LEARNING_POTENTIAL = 50;
    private static final double REASSIGNMENT_CHANCE_PER_SEASON = 0.3;

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

        for (int game = 0; game < games; game++) {
            PlayerSnapshot snapshot = newRandomSnapshot(employeesPerGroupPerGame, random);
            employeesStarted += rosterSize(snapshot);

            for (int season = 0; season < seasons && rosterSize(snapshot) > 0; season++) {
                shuffleAssignments(snapshot, random);
                int before = rosterSize(snapshot);
                Set<String> busyBefore = busyEmployeeKeys(snapshot);

                List<ResolutionEvent> lifecycleEvents = PersonnelLifecycle.advanceSeason(snapshot);
                List<ResolutionEvent> trainingEvents = TrainingProgress.advanceSeason(snapshot, random);
                checkInvariants(snapshot, lifecycleEvents, before, busyBefore);
                checkTrainingInvariants(snapshot, trainingEvents);

                for (ResolutionEvent event : lifecycleEvents) {
                    PersonnelDeparture departure = (PersonnelDeparture) event;
                    int tenureSeasons = season + 1;
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
            }
        }

        int stillEmployed = employeesStarted - resignedCount - retiredCount;
        double avgResignedTenure = resignedCount == 0 ? 0 : (double) resignedTenureTotal / resignedCount;
        double avgRetiredTenure = retiredCount == 0 ? 0 : (double) retiredTenureTotal / retiredCount;
        double avgTrainingGain = trainingCompletedCount == 0 ? 0 : trainingGainTotal / trainingCompletedCount;

        return new PersonnelPlayoutReport(games, seasons, employeesStarted, resignedCount, retiredCount,
            stillEmployed, avgResignedTenure, avgRetiredTenure, trainingCompletedCount, avgTrainingGain);
    }

    /**
     * Cross-checks {@link PersonnelLifecycle}'s own output for internal
     * consistency - age monotonicity and "busy never departs" are already
     * covered exhaustively by {@code PersonnelLifecycleTest}'s own unit
     * tests, so this focuses on what many random combinations can catch
     * that a handful of hand-picked unit tests might not: morale staying
     * in bounds, every reported departure actually matching the threshold
     * it claims to, and that nobody who was busy going into this season's
     * advance is ever among its departures - the property that would break
     * first if {@link PersonnelLifecycle} and {@link TrainingProgress} were
     * ever accidentally run in the wrong order.
     *
     * @param snapshot        the snapshot after this season's advance
     * @param events          the {@link PersonnelLifecycle} events this season's advance returned
     * @param rosterSizeBefore the combined roster size before this season's advance
     * @param busyBefore      the {@code group:id} keys of every employee who was
     *                        busy immediately before this season's advance ran
     * @throws IllegalStateException if any invariant is violated
     */
    private static void checkInvariants(PlayerSnapshot snapshot, List<ResolutionEvent> events, int rosterSizeBefore,
            Set<String> busyBefore) {
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
            if (busyBefore.contains(key)) {
                throw new IllegalStateException("Employee " + departure.personnelId()
                    + " departed despite being busy going into this season - resignation/retirement must only"
                    + " ever be checked while idle");
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
        }
        for (SetSpecialist specialist : snapshot.getSetSpecialists()) {
            checkTrainingConsistency(specialist, specialist.getTrainingTargetSkill());
            checkSkillsInRange(specialist.getId(), specialist.getSkills().values());
        }
        for (MissionControlOfficer officer : snapshot.getMissionControlOfficers()) {
            checkTrainingConsistency(officer, officer.getTrainingTargetSkill());
            checkSkillsInRange(officer.getId(), officer.getSkills().values());
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
     * Collects the {@code group:id} key of every currently-busy employee
     * across all three rosters, for {@link #checkInvariants}'s "busy never
     * departs" check.
     *
     * @param snapshot the snapshot to inspect
     * @return the busy employees' keys
     */
    private static Set<String> busyEmployeeKeys(PlayerSnapshot snapshot) {
        Set<String> keys = new HashSet<>();
        addBusyKeys(snapshot.getAstronauts(), PersonnelGroup.ASTRONAUT, keys);
        addBusyKeys(snapshot.getSetSpecialists(), PersonnelGroup.SET, keys);
        addBusyKeys(snapshot.getMissionControlOfficers(), PersonnelGroup.MISSION_CONTROL, keys);
        return keys;
    }

    private static void addBusyKeys(List<? extends Personnel> roster, PersonnelGroup group, Set<String> keys) {
        for (Personnel person : roster) {
            if (person.getAssignmentState() != AssignmentState.IDLE) {
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
        TrainingKind kind = random.nextBoolean() ? TrainingKind.BASIC : TrainingKind.ADVANCED;
        int total = kind == TrainingKind.BASIC
            ? TrainingProgress.ASTRONAUT_BASIC_TRAINING_SEASONS
            : TrainingProgress.ADVANCED_TRAINING_SEASONS;
        astronaut.setTrainingKind(kind);
        astronaut.setTrainingSeasonsTotal(total);
        astronaut.setTrainingSeasonsCompleted(0);
        astronaut.setTrainingTargetSkill(target);
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
        TrainingKind kind = random.nextBoolean() ? TrainingKind.BASIC : TrainingKind.ADVANCED;
        int total = kind == TrainingKind.BASIC
            ? TrainingProgress.SET_BASIC_TRAINING_SEASONS
            : TrainingProgress.ADVANCED_TRAINING_SEASONS;
        specialist.setTrainingKind(kind);
        specialist.setTrainingSeasonsTotal(total);
        specialist.setTrainingSeasonsCompleted(0);
        specialist.setTrainingTargetSkill(target);
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
        TrainingKind kind = random.nextBoolean() ? TrainingKind.BASIC : TrainingKind.ADVANCED;
        int total = kind == TrainingKind.BASIC
            ? TrainingProgress.MISSION_CONTROL_BASIC_TRAINING_SEASONS
            : TrainingProgress.ADVANCED_TRAINING_SEASONS;
        officer.setTrainingKind(kind);
        officer.setTrainingSeasonsTotal(total);
        officer.setTrainingSeasonsCompleted(0);
        officer.setTrainingTargetSkill(target);
        officer.setAssignmentState(AssignmentState.IN_TRAINING);
    }

    private static PlayerSnapshot newRandomSnapshot(int employeesPerGroup, Random random) {
        PlayerSnapshot snapshot = new PlayerSnapshot("Bot Agency", Archetype.NASA, 0, 0, 0, 1);
        List<Astronaut> astronauts = new ArrayList<>();
        List<SetSpecialist> specialists = new ArrayList<>();
        List<MissionControlOfficer> officers = new ArrayList<>();
        for (int i = 0; i < employeesPerGroup; i++) {
            astronauts.add(new Astronaut("a" + i, "Astronaut " + i,
                randomAge(random), randomMorale(random), STARTING_LEARNING_POTENTIAL));
            specialists.add(new SetSpecialist("s" + i, "Specialist " + i,
                randomAge(random), randomMorale(random), STARTING_LEARNING_POTENTIAL));
            officers.add(new MissionControlOfficer("m" + i, "Officer " + i,
                randomAge(random), randomMorale(random), STARTING_LEARNING_POTENTIAL));
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
    }
}
