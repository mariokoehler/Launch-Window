package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.MissionControlOfficer;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSpecialist;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>Tests for {@link PersonnelLifecycle}'s season-advance pass.</p>
 *
 * @author Mario Koehler
 */
class PersonnelLifecycleTest {

    /**
     * Verifies that an idle employee loses the configured amount of morale
     * in one pass.
     */
    @Test
    void idleEmployeeLosesMoraleEachSeason() {
        PlayerSnapshot snapshot = snapshotWithAstronaut(astronaut("a1", 40, 60, AssignmentState.IDLE));

        PersonnelLifecycle.advanceSeason(snapshot);

        assertEquals(60 - PersonnelLifecycle.IDLE_MORALE_LOSS_PER_SEASON, snapshot.getAstronauts().get(0).getMorale());
    }

    /**
     * Verifies that a busy (assigned) employee gains the configured amount
     * of morale in one pass.
     */
    @Test
    void assignedEmployeeGainsMoraleEachSeason() {
        PlayerSnapshot snapshot = snapshotWithAstronaut(astronaut("a1", 40, 60, AssignmentState.ASSIGNED));

        PersonnelLifecycle.advanceSeason(snapshot);

        assertEquals(60 + PersonnelLifecycle.BUSY_MORALE_GAIN_PER_SEASON, snapshot.getAstronauts().get(0).getMorale());
    }

    /**
     * Verifies that an employee in training is treated as busy, the same as
     * one actively assigned.
     */
    @Test
    void inTrainingEmployeeGainsMoraleEachSeason() {
        PlayerSnapshot snapshot = snapshotWithAstronaut(astronaut("a1", 40, 60, AssignmentState.IN_TRAINING));

        PersonnelLifecycle.advanceSeason(snapshot);

        assertEquals(60 + PersonnelLifecycle.BUSY_MORALE_GAIN_PER_SEASON, snapshot.getAstronauts().get(0).getMorale());
    }

    /**
     * Verifies morale never rises above the configured maximum.
     */
    @Test
    void moraleIsClampedAtMaximum() {
        PlayerSnapshot snapshot = snapshotWithAstronaut(
            astronaut("a1", 40, PersonnelLifecycle.MORALE_MAX - 1, AssignmentState.ASSIGNED));

        PersonnelLifecycle.advanceSeason(snapshot);

        assertEquals(PersonnelLifecycle.MORALE_MAX, snapshot.getAstronauts().get(0).getMorale());
    }

    /**
     * Verifies that an idle employee whose morale is already at or below
     * zero resigns with a clamped, never-negative exit morale recorded.
     */
    @Test
    void moraleIsClampedAtMinimumOnTheWayOutTheDoor() {
        PlayerSnapshot snapshot = snapshotWithAstronaut(astronaut("a1", 40, 1, AssignmentState.IDLE));

        List<ResolutionEvent> events = PersonnelLifecycle.advanceSeason(snapshot);

        assertTrue(snapshot.getAstronauts().isEmpty());
        PersonnelDeparture departure = (PersonnelDeparture) events.get(0);
        assertEquals(PersonnelLifecycle.MORALE_MIN, departure.moraleAtExit());
    }

    /**
     * Verifies that an idle employee whose morale lands exactly on the
     * resignation floor resigns the same season.
     */
    @Test
    void idleEmployeeAtResignationFloorResigns() {
        int startMorale = PersonnelLifecycle.RESIGNATION_MORALE_FLOOR + PersonnelLifecycle.IDLE_MORALE_LOSS_PER_SEASON;
        PlayerSnapshot snapshot = snapshotWithAstronaut(astronaut("a1", 40, startMorale, AssignmentState.IDLE));

        List<ResolutionEvent> events = PersonnelLifecycle.advanceSeason(snapshot);

        assertTrue(snapshot.getAstronauts().isEmpty());
        assertEquals(1, events.size());
        PersonnelDeparture departure = (PersonnelDeparture) events.get(0);
        assertEquals("a1", departure.personnelId());
        assertEquals(PersonnelGroup.ASTRONAUT, departure.group());
        assertEquals(DepartureReason.RESIGNED, departure.reason());
    }

    /**
     * Verifies that an idle employee one point above the resignation floor
     * stays.
     */
    @Test
    void idleEmployeeJustAboveResignationFloorStays() {
        int startMorale = PersonnelLifecycle.RESIGNATION_MORALE_FLOOR + PersonnelLifecycle.IDLE_MORALE_LOSS_PER_SEASON + 1;
        PlayerSnapshot snapshot = snapshotWithAstronaut(astronaut("a1", 40, startMorale, AssignmentState.IDLE));

        List<ResolutionEvent> events = PersonnelLifecycle.advanceSeason(snapshot);

        assertEquals(1, snapshot.getAstronauts().size());
        assertTrue(events.isEmpty());
    }

    /**
     * Verifies that a busy employee never resigns, no matter how low their
     * morale already is - resignation is only ever checked while idle.
     */
    @Test
    void busyEmployeeNeverResignsRegardlessOfMorale() {
        PlayerSnapshot snapshot = snapshotWithAstronaut(astronaut("a1", 40, 1, AssignmentState.ASSIGNED));

        List<ResolutionEvent> events = PersonnelLifecycle.advanceSeason(snapshot);

        assertEquals(1, snapshot.getAstronauts().size());
        assertTrue(events.isEmpty());
    }

    /**
     * Verifies that an idle astronaut whose age rolls over to the
     * astronaut retirement age this season retires.
     */
    @Test
    void idleEmployeeReachingRetirementAgeRetires() {
        Astronaut astronaut = astronaut("a1", PersonnelLifecycle.ASTRONAUT_RETIREMENT_AGE - 1, 80, AssignmentState.IDLE);
        astronaut.setAgingProgressSeasons(PersonnelLifecycle.SEASONS_PER_YEAR - 1);
        PlayerSnapshot snapshot = snapshotWithAstronaut(astronaut);

        List<ResolutionEvent> events = PersonnelLifecycle.advanceSeason(snapshot);

        assertTrue(snapshot.getAstronauts().isEmpty());
        assertEquals(1, events.size());
        PersonnelDeparture departure = (PersonnelDeparture) events.get(0);
        assertEquals(DepartureReason.RETIRED, departure.reason());
        assertEquals(PersonnelLifecycle.ASTRONAUT_RETIREMENT_AGE, departure.ageAtExit());
    }

    /**
     * Verifies that an idle astronaut not yet due to age up this season
     * does not retire even if already at the retirement age's threshold
     * year, since the aging-progress counter hasn't rolled over yet.
     */
    @Test
    void idleEmployeeNotYetAgingUpDoesNotRetire() {
        Astronaut astronaut = astronaut("a1", PersonnelLifecycle.ASTRONAUT_RETIREMENT_AGE - 1, 80, AssignmentState.IDLE);
        PlayerSnapshot snapshot = snapshotWithAstronaut(astronaut);

        List<ResolutionEvent> events = PersonnelLifecycle.advanceSeason(snapshot);

        assertEquals(1, snapshot.getAstronauts().size());
        assertTrue(events.isEmpty());
    }

    /**
     * Verifies that a busy employee never retires either, even far past
     * both the retirement age and the resignation morale floor at once.
     */
    @Test
    void busyEmployeeNeverRetiresOrResignsEvenPastBothThresholds() {
        Astronaut astronaut = astronaut("a1", PersonnelLifecycle.ASTRONAUT_RETIREMENT_AGE + 20, 1, AssignmentState.ASSIGNED);
        PlayerSnapshot snapshot = snapshotWithAstronaut(astronaut);

        List<ResolutionEvent> events = PersonnelLifecycle.advanceSeason(snapshot);

        assertEquals(1, snapshot.getAstronauts().size());
        assertTrue(events.isEmpty());
    }

    /**
     * Verifies that a departure merely deferred by being busy actually
     * happens once the employee goes idle again - the protection is only
     * ever "not checked this season while busy", never a permanent
     * exemption once the underlying stats already cross a threshold.
     */
    @Test
    void departureDeferredWhileBusyHappensOnceIdleAgain() {
        Astronaut astronaut = astronaut("a1", PersonnelLifecycle.ASTRONAUT_RETIREMENT_AGE + 20, 1, AssignmentState.ASSIGNED);
        PlayerSnapshot snapshot = snapshotWithAstronaut(astronaut);
        PersonnelLifecycle.advanceSeason(snapshot);
        assertEquals(1, snapshot.getAstronauts().size(), "still protected while busy");

        snapshot.getAstronauts().get(0).setAssignmentState(AssignmentState.IDLE);
        List<ResolutionEvent> events = PersonnelLifecycle.advanceSeason(snapshot);

        assertTrue(snapshot.getAstronauts().isEmpty());
        assertEquals(1, events.size());
    }

    /**
     * Verifies that age only rolls over to a whole extra year once every
     * {@link PersonnelLifecycle#SEASONS_PER_YEAR} seasons, not sooner.
     */
    @Test
    void agingProgressRollsOverEveryFourSeasons() {
        Astronaut astronaut = astronaut("a1", 30, 80, AssignmentState.IDLE);
        PlayerSnapshot snapshot = snapshotWithAstronaut(astronaut);

        for (int i = 0; i < PersonnelLifecycle.SEASONS_PER_YEAR - 1; i++) {
            PersonnelLifecycle.advanceSeason(snapshot);
        }
        assertEquals(30, snapshot.getAstronauts().get(0).getAge());

        PersonnelLifecycle.advanceSeason(snapshot);
        assertEquals(31, snapshot.getAstronauts().get(0).getAge());
    }

    /**
     * Verifies each roster is checked against its own retirement age, not
     * a shared one. Each group gets both an employee due to retire this
     * season and one sitting between the other two groups' thresholds - the
     * second is what actually discriminates a wrong-constant-wired-to-
     * wrong-roster bug from a coincidentally-passing test (SET/MC's own
     * "due to retire" ages are, respectively, above both other groups'
     * thresholds and above the astronaut one, so on their own they can't
     * tell a mis-wiring apart from the correct wiring).
     */
    @Test
    void eachRosterUsesItsOwnRetirementAge() {
        SetSpecialist dueToRetire = new SetSpecialist("s1", "Set One", PersonnelLifecycle.SET_RETIREMENT_AGE - 1, 80, 50);
        dueToRetire.setAgingProgressSeasons(PersonnelLifecycle.SEASONS_PER_YEAR - 1);
        SetSpecialist staysBetweenThresholds = new SetSpecialist("s2", "Set Two",
            PersonnelLifecycle.MISSION_CONTROL_RETIREMENT_AGE + 1, 80, 50);

        MissionControlOfficer officerDueToRetire = new MissionControlOfficer("m1", "MC One",
            PersonnelLifecycle.MISSION_CONTROL_RETIREMENT_AGE - 1, 80, 50);
        officerDueToRetire.setAgingProgressSeasons(PersonnelLifecycle.SEASONS_PER_YEAR - 1);
        MissionControlOfficer officerStaysBetweenThresholds = new MissionControlOfficer("m2", "MC Two",
            PersonnelLifecycle.ASTRONAUT_RETIREMENT_AGE + 1, 80, 50);

        PlayerSnapshot snapshot = new PlayerSnapshot("Agency", Archetype.NASA, 0, 0, 0, 1);
        snapshot.setSetSpecialists(List.of(dueToRetire, staysBetweenThresholds));
        snapshot.setMissionControlOfficers(List.of(officerDueToRetire, officerStaysBetweenThresholds));

        List<ResolutionEvent> events = PersonnelLifecycle.advanceSeason(snapshot);

        assertEquals(1, snapshot.getSetSpecialists().size());
        assertEquals("s2", snapshot.getSetSpecialists().get(0).getId());
        assertEquals(1, snapshot.getMissionControlOfficers().size());
        assertEquals("m2", snapshot.getMissionControlOfficers().get(0).getId());
        assertEquals(2, events.size());
        assertTrue(events.stream().allMatch(e -> ((PersonnelDeparture) e).reason() == DepartureReason.RETIRED));
    }

    /**
     * Verifies that only the departing employee is removed from a roster of
     * several, leaving the others' order and state untouched.
     */
    @Test
    void onlyTheDepartingEmployeeIsRemovedFromARosterOfSeveral() {
        Astronaut stays1 = astronaut("a1", 30, 80, AssignmentState.IDLE);
        Astronaut resigns = astronaut("a2", 30, 1, AssignmentState.IDLE);
        Astronaut stays2 = astronaut("a3", 30, 80, AssignmentState.IDLE);
        PlayerSnapshot snapshot = new PlayerSnapshot("Agency", Archetype.NASA, 0, 0, 0, 1);
        snapshot.setAstronauts(List.of(stays1, resigns, stays2));

        PersonnelLifecycle.advanceSeason(snapshot);

        List<Astronaut> remaining = snapshot.getAstronauts();
        assertEquals(2, remaining.size());
        assertEquals("a1", remaining.get(0).getId());
        assertEquals("a3", remaining.get(1).getId());
    }

    /**
     * Verifies the season-advance pass never touches the snapshot's own
     * turn counter - that is {@code PlayerStateService}'s responsibility,
     * not the rules engine's.
     */
    @Test
    void doesNotTouchTheSnapshotsSeasonCounter() {
        PlayerSnapshot snapshot = snapshotWithAstronaut(astronaut("a1", 40, 60, AssignmentState.IDLE));
        snapshot.setSeason(7);

        PersonnelLifecycle.advanceSeason(snapshot);

        assertEquals(7, snapshot.getSeason());
    }

    /**
     * Verifies {@code advanceSeason} rejects a {@code null} snapshot with
     * its own clear message, rather than failing with a less obvious
     * {@code NullPointerException} deeper inside.
     */
    @Test
    void rejectsNullSnapshot() {
        assertThrows(NullPointerException.class, () -> PersonnelLifecycle.advanceSeason(null));
    }

    private static PlayerSnapshot snapshotWithAstronaut(Astronaut astronaut) {
        PlayerSnapshot snapshot = new PlayerSnapshot("Agency", Archetype.NASA, 0, 0, 0, 1);
        snapshot.setAstronauts(List.of(astronaut));
        return snapshot;
    }

    private static Astronaut astronaut(String id, int age, int morale, AssignmentState state) {
        Astronaut astronaut = new Astronaut(id, "Name " + id, age, morale, 50);
        astronaut.setAssignmentState(state);
        return astronaut;
    }
}
