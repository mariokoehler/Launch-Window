package de.mkoehler.launchwindow.devtools.analysis;

import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.MissionControlOfficer;
import de.mkoehler.launchwindow.model.Personnel;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSpecialist;
import de.mkoehler.launchwindow.rules.DepartureReason;
import de.mkoehler.launchwindow.rules.PersonnelDeparture;
import de.mkoehler.launchwindow.rules.PersonnelGroup;
import de.mkoehler.launchwindow.rules.PersonnelLifecycle;
import de.mkoehler.launchwindow.rules.ResolutionEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * <p>The first slice of Launch Window's own {@code Playouts}-style bot-driven
 * harness (see {@code design.md} &sect;3.10, following Robot-Rampage's own
 * precedent) - drives {@link PersonnelLifecycle#advanceSeason}, the only
 * rules-engine resolver that exists so far, across many random simulated
 * rosters and seasons, purely headless (no client/server/persistence
 * involved), and reports the tenure/departure statistics that make it a
 * tuning tool rather than just another unit test.</p>
 *
 * <p><b>Deliberately narrow, named after the one resolver it drives</b> -
 * this is not yet the general full-game {@code Playouts} harness
 * {@code design.md} &sect;3.10 describes (that needs a real bot making
 * per-season decisions like "queue a mission" or "open a program," none of
 * which exist yet). Expected to grow alongside M2's remaining slices
 * ({@code design.md} &sect;5), most likely folding into a more general
 * harness once there is more than one resolver to drive per season, rather
 * than staying its own permanent, resolver-specific class.</p>
 *
 * <p>The "bot" here is intentionally trivial: each employee independently
 * has a fixed per-season chance of toggling between idle and busy
 * ({@link #REASSIGNMENT_CHANCE_PER_SEASON}), just enough churn to actually
 * exercise both the morale-gain-while-busy path and the idle-only
 * resignation/retirement checks over many seasons - not a stand-in for a
 * real per-turn player decision, which doesn't exist yet at this milestone.</p>
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

        for (int game = 0; game < games; game++) {
            PlayerSnapshot snapshot = newRandomSnapshot(employeesPerGroupPerGame, random);
            employeesStarted += rosterSize(snapshot);

            for (int season = 0; season < seasons && rosterSize(snapshot) > 0; season++) {
                shuffleAssignments(snapshot, random);
                int before = rosterSize(snapshot);

                List<ResolutionEvent> events = PersonnelLifecycle.advanceSeason(snapshot);
                checkInvariants(snapshot, events, before);

                for (ResolutionEvent event : events) {
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
            }
        }

        int stillEmployed = employeesStarted - resignedCount - retiredCount;
        double avgResignedTenure = resignedCount == 0 ? 0 : (double) resignedTenureTotal / resignedCount;
        double avgRetiredTenure = retiredCount == 0 ? 0 : (double) retiredTenureTotal / retiredCount;

        return new PersonnelPlayoutReport(games, seasons, employeesStarted, resignedCount, retiredCount,
            stillEmployed, avgResignedTenure, avgRetiredTenure);
    }

    /**
     * Cross-checks the resolver's own output for internal consistency -
     * age monotonicity and "busy never departs" are already covered
     * exhaustively by {@code PersonnelLifecycleTest}'s own unit tests, so
     * this focuses on what many random combinations can catch that a
     * handful of hand-picked unit tests might not: morale staying in
     * bounds, and every reported departure actually matching the threshold
     * it claims to.
     *
     * @param snapshot        the snapshot after this season's advance
     * @param events          the events this season's advance returned
     * @param rosterSizeBefore the combined roster size before this season's advance
     * @throws IllegalStateException if any invariant is violated
     */
    private static void checkInvariants(PlayerSnapshot snapshot, List<ResolutionEvent> events, int rosterSizeBefore) {
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
        }

        int rosterSizeAfter = rosterSize(snapshot);
        if (rosterSizeBefore - events.size() != rosterSizeAfter) {
            throw new IllegalStateException(
                "Roster size change does not match the number of reported departures");
        }
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
        snapshot.getAstronauts().forEach(a -> maybeToggleAssignment(a, random));
        snapshot.getSetSpecialists().forEach(s -> maybeToggleAssignment(s, random));
        snapshot.getMissionControlOfficers().forEach(m -> maybeToggleAssignment(m, random));
    }

    private static void maybeToggleAssignment(Personnel person, Random random) {
        if (random.nextDouble() >= REASSIGNMENT_CHANCE_PER_SEASON) {
            return;
        }
        if (person.getAssignmentState() == AssignmentState.IDLE) {
            person.setAssignmentState(random.nextBoolean() ? AssignmentState.ASSIGNED : AssignmentState.IN_TRAINING);
        } else {
            person.setAssignmentState(AssignmentState.IDLE);
        }
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
    }
}
