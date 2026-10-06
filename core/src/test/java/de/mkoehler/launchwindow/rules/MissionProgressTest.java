package de.mkoehler.launchwindow.rules;

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
import de.mkoehler.launchwindow.model.MemorialEntry;
import de.mkoehler.launchwindow.model.MissionControlOfficer;
import de.mkoehler.launchwindow.model.MissionControlSkill;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSkill;
import de.mkoehler.launchwindow.model.SetSpecialist;
import de.mkoehler.launchwindow.model.TrainingKind;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>Tests for {@link MissionProgress}.</p>
 *
 * @author Mario Koehler
 */
class MissionProgressTest {

    private static final List<Double> RD_SLOT_BANDS = List.of(3.0, 6.0, 9.0, 12.0);

    private static final MissionComponent CAPSULE =
        new MissionComponent("capsule-mercury", SetSkill.CREWED_SPACECRAFT, 3, 90, 82, RD_SLOT_BANDS, 0, List.of());
    private static final Program PROGRAM =
        new Program("mercury-program", "early-space", 1_000, 0, List.of("capsule-mercury"), List.of());
    private static final ProgramCategory CATEGORY = new ProgramCategory("early-space", "Early Space");

    /**
     * Builds a catalog holding {@link #PROGRAM}/{@link #CAPSULE} plus the
     * given mission configuration(s).
     *
     * @param configurations the mission configurations to include
     * @return the built catalog
     */
    /**
     * A one-step uncrewed flight carrying an animal, with or without a way home.
     */
    private static MissionConfiguration passengerFlight(boolean returnHome) {
        MissionStep step = new MissionStep("orbit",
            List.of(new MissionStep.ComponentInvolvement("capsule-mercury", 100)), List.of(), List.of(), false);
        MissionConfiguration plain = new MissionConfiguration("dog-std", "mercury-program",
            List.of(new MissionSeason(List.of(step))), 0, 0, 0);
        return new MissionConfiguration(plain.id(), plain.programId(), plain.seasons(), plain.crewSlots(),
            plain.controlSlots(), plain.cost(), plain.firstTimePrestige(), plain.goalDependencies(),
            plain.goalBoosts(), plain.launches(), plain.cancellationPrestigeDeduction(),
            plain.cancellationRefundPercent(), new MissionConfiguration.AnimalPassengers(
                List.of(new MissionConfiguration.Animal("Laika", "laika")), returnHome, "An unnamed dog"));
    }

    /**
     * A flight with no way home puts its passenger on the memorial when it completes, with her photo;
     * a second flight remembers an unnamed dog, never a second "Laika".
     */
    @Test
    void aNoReturnFlightRemembersItsPassenger() {
        ContentCatalog catalog = catalogWith(passengerFlight(false));
        PlayerSnapshot snapshot = snapshotWithOwnedCapsule(90.0);
        for (int flight = 0; flight < 2; flight++) {
            snapshot.getInProgressMissions().put("m" + flight,
                new InProgressMission("m" + flight, "dog-std", List.of(), List.of()));
            // First roll 99 passes; then 1 reliability-change factor.
            List<ResolutionEvent> events = MissionProgress.advanceSeason(snapshot, catalog,
                new ScriptedRandomGenerator(99.0, 0.1), new GoalTally(), 7 + flight);
            assertEquals(List.of(new PassengerLost("m" + flight, "dog-std", flight == 0 ? "Laika" : "An unnamed dog",
                null)), events.stream().filter(PassengerLost.class::isInstance).toList());
        }

        assertEquals(2, snapshot.getMemorial().size());
        MemorialEntry laika = snapshot.getMemorial().get(0);
        assertEquals("Laika", laika.name());
        assertEquals(MemorialEntry.Cause.PASSENGER, laika.cause());
        assertEquals("laika", laika.portraitId());
        assertEquals(7, laika.turn());
        assertNull(laika.stepId());
        assertEquals("An unnamed dog", snapshot.getMemorial().get(1).name());
        assertNull(snapshot.getMemorial().get(1).portraitId());
    }

    /**
     * A flight meant to bring its passengers home remembers them only when it fails, with the step.
     */
    @Test
    void aReturningFlightRemembersItsPassengersOnlyOnFailure() {
        ContentCatalog catalog = catalogWith(passengerFlight(true));
        PlayerSnapshot snapshot = snapshotWithOwnedCapsule(90.0);
        snapshot.getInProgressMissions().put("m0", new InProgressMission("m0", "dog-std", List.of(), List.of()));
        MissionProgress.advanceSeason(snapshot, catalog, new ScriptedRandomGenerator(99.0, 0.1));
        assertTrue(snapshot.getMemorial().isEmpty());

        snapshot = snapshotWithOwnedCapsule(10.0);
        snapshot.getInProgressMissions().put("m1", new InProgressMission("m1", "dog-std", List.of(), List.of()));
        // First roll 0 fails; no controllers, so saving roll 30 lands in plain failure; 1 reliability factor.
        List<ResolutionEvent> events = MissionProgress.advanceSeason(snapshot, catalog,
            new ScriptedRandomGenerator(0.0, 30.0, 0.1));
        assertEquals(List.of(new PassengerLost("m1", "dog-std", "Laika", "orbit")),
            events.stream().filter(PassengerLost.class::isInstance).toList(), "the turn report hears of it");
        assertEquals(1, snapshot.getMemorial().size());
        assertEquals("Laika", snapshot.getMemorial().get(0).name());
        assertEquals("orbit", snapshot.getMemorial().get(0).stepId());
    }

    private static ContentCatalog catalogWith(MissionConfiguration... configurations) {
        return new ContentCatalog(List.of(CATEGORY), List.of(PROGRAM), List.of(configurations), List.of(CAPSULE));
    }

    /**
     * Builds a snapshot with the mercury program open, one owned capsule
     * at the given reliability, and no personnel yet.
     *
     * @param componentReliability the capsule's current (and floor)
     *                             reliability
     * @return the built snapshot
     */
    private static PlayerSnapshot snapshotWithOwnedCapsule(double componentReliability) {
        PlayerSnapshot snapshot = new PlayerSnapshot("Agency", Archetype.NASA, 10_000, 0, 0, 1);
        snapshot.setOpenedProgramIds(List.of("mercury-program"));
        snapshot.setMissionComponents(
            Map.of("capsule-mercury", new OwnedMissionComponent(componentReliability, componentReliability)));
        return snapshot;
    }

    /**
     * Adds an assigned, {@code ASSIGNED} astronaut with the given piloting
     * skill to the snapshot's roster.
     *
     * @param snapshot    the snapshot to add to
     * @param id          the astronaut's id
     * @param skill       their {@link AstronautSkill#PILOTING} value
     * @param missionId   the mission instance id they are assigned to
     */
    private static void addAssignedAstronaut(PlayerSnapshot snapshot, String id, double skill, String missionId) {
        addAssignedAstronaut(snapshot, id, skill, missionId, 34);
    }

    /**
     * Same as {@link #addAssignedAstronaut(PlayerSnapshot, String, double, String)},
     * with an explicit age - for retire-on-release tests.
     *
     * @param snapshot    the snapshot to add to
     * @param id          the astronaut's id
     * @param skill       their {@link AstronautSkill#PILOTING} value
     * @param missionId   the mission instance id they are assigned to
     * @param age         their age, in whole years
     */
    private static void addAssignedAstronaut(PlayerSnapshot snapshot, String id, double skill, String missionId,
            int age) {
        Astronaut astronaut = new Astronaut(id, id, age, 80, 60);
        astronaut.setSkill(AstronautSkill.PILOTING, skill);
        astronaut.setAssignmentState(AssignmentState.ASSIGNED);
        astronaut.setAssignedMissionId(missionId);
        List<Astronaut> roster = new java.util.ArrayList<>(snapshot.getAstronauts());
        roster.add(astronaut);
        snapshot.setAstronauts(roster);
    }

    /**
     * Adds an assigned, {@code ASSIGNED} Mission Control officer with the
     * given propulsion skill to the snapshot's roster.
     *
     * @param snapshot  the snapshot to add to
     * @param id        the officer's id
     * @param skill     their {@link MissionControlSkill#PROPULSION} value
     * @param missionId the mission instance id they are assigned to
     */
    private static void addAssignedController(PlayerSnapshot snapshot, String id, double skill, String missionId) {
        addAssignedController(snapshot, id, skill, missionId, 45);
    }

    /**
     * Same as {@link #addAssignedController(PlayerSnapshot, String, double, String)},
     * with an explicit age - for retire-on-release tests.
     *
     * @param snapshot  the snapshot to add to
     * @param id        the officer's id
     * @param skill     their {@link MissionControlSkill#PROPULSION} value
     * @param missionId the mission instance id they are assigned to
     * @param age       their age, in whole years
     */
    private static void addAssignedController(PlayerSnapshot snapshot, String id, double skill, String missionId,
            int age) {
        MissionControlOfficer officer = new MissionControlOfficer(id, id, age, 70, 40);
        officer.setSkill(MissionControlSkill.PROPULSION, skill);
        officer.setAssignmentState(AssignmentState.ASSIGNED);
        officer.setAssignedMissionId(missionId);
        List<MissionControlOfficer> roster = new java.util.ArrayList<>(snapshot.getMissionControlOfficers());
        roster.add(officer);
        snapshot.setMissionControlOfficers(roster);
    }

    /**
     * Verifies a crew involvement reads the one astronaut in its slot, as in BASPM
     * ({@code baspm.md} &sect;10.11) - not an average, sum, or max across the crew.
     * Slot 0 holds skill 40 and slot 1 skill 90; only slot 0 is involved. Capsule 80
     * blended with crew 40 gives base probability 72 (first-roll threshold 28) and
     * saving-throw probability 72*50/100=36 (pass threshold 64). A first roll of 20
     * fails, and a saving roll of 63 lands in the {@code MINOR_GLITCH} band. Reading
     * slot 1 (base 82) would succeed on the first roll, and the old average (77,
     * threshold 61.5) would solve the glitch.
     */
    @Test
    void crewContributionReadsTheAstronautInTheInvolvedSlot() {
        MissionStep step = new MissionStep("ops", List.of(new MissionStep.ComponentInvolvement("capsule-mercury", 100)),
            List.of(new MissionStep.ControllerInvolvement(0, 100)),
            List.of(new MissionStep.CrewInvolvement(0, AstronautSkill.PILOTING, 100)), false);
        MissionConfiguration configuration =
            new MissionConfiguration("slot-std", "mercury-program", List.of(new MissionSeason(List.of(step))), 2, 1, 0);
        ContentCatalog catalog = catalogWith(configuration);

        PlayerSnapshot snapshot = snapshotWithOwnedCapsule(80.0);
        addAssignedAstronaut(snapshot, "astro-1", 40.0, "m0");
        addAssignedAstronaut(snapshot, "astro-2", 90.0, "m0");
        addAssignedController(snapshot, "mco-1", 50.0, "m0");
        snapshot.getInProgressMissions().put("m0",
            new InProgressMission("m0", "slot-std", List.of("astro-1", "astro-2"), List.of("mco-1")));

        // The 12 filler values after the two step rolls cover this season's post-flight draws: 1
        // reliability-change factor, 5 skill-growth factors per astronaut aboard, and 1 for the
        // controller slot exercised this season.
        ScriptedRandomGenerator random = new ScriptedRandomGenerator(20.0, 63.0,
            0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1);
        List<ResolutionEvent> events = MissionProgress.advanceSeason(snapshot, catalog, random);

        MissionStepResolved resolved = (MissionStepResolved) events.get(0);
        assertEquals(MissionStepOutcome.MINOR_GLITCH, resolved.outcome());
    }

    /**
     * Verifies a single-season mission that resolves without a
     * mission-ending outcome completes: it is removed from
     * {@code inProgressMissions} and every assigned person is released to
     * {@code IDLE} with {@code assignedMissionId} cleared.
     */
    @Test
    void completingTheFinalSeasonReleasesEveryoneAndEndsTheMission() {
        MissionStep step = new MissionStep("ops", List.of(new MissionStep.ComponentInvolvement("capsule-mercury", 100)),
            List.of(), List.of(new MissionStep.CrewInvolvement(0, AstronautSkill.PILOTING, 100)), false);
        MissionConfiguration configuration = new MissionConfiguration("complete-std", "mercury-program",
            List.of(new MissionSeason(List.of(step))), 1, 0, 0);
        ContentCatalog catalog = catalogWith(configuration);

        PlayerSnapshot snapshot = snapshotWithOwnedCapsule(90.0);
        addAssignedAstronaut(snapshot, "astro-1", 80.0, "m0");
        snapshot.getInProgressMissions().put("m0",
            new InProgressMission("m0", "complete-std", List.of("astro-1"), List.of()));

        // step roll (1) + this season's post-flight draws: 1 reliability-change factor
        // (capsule-mercury, SUCCESSFUL) + 5 skill-growth factors (astro-1's five skills).
        ScriptedRandomGenerator random = new ScriptedRandomGenerator(99.0, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1);
        MissionProgress.advanceSeason(snapshot, catalog, random);

        assertTrue(snapshot.getInProgressMissions().isEmpty());
        assertEquals(AssignmentState.IDLE, snapshot.getAstronauts().get(0).getAssignmentState());
        assertNull(snapshot.getAstronauts().get(0).getAssignedMissionId());
    }

    /**
     * Verifies an astronaut who has already reached retirement age at the
     * moment a completing mission releases them retires on the spot
     * instead of going {@code IDLE} - the retire-on-release check that
     * closes the "relaunch every season" loophole (user decision,
     * 2026-10-02, see {@code design.md} &sect;2.5(f)).
     */
    @Test
    void overAgeAstronautRetiresInsteadOfReleasingOnMissionCompletion() {
        MissionStep step = new MissionStep("ops", List.of(new MissionStep.ComponentInvolvement("capsule-mercury", 100)),
            List.of(), List.of(new MissionStep.CrewInvolvement(0, AstronautSkill.PILOTING, 100)), false);
        MissionConfiguration configuration = new MissionConfiguration("complete-std", "mercury-program",
            List.of(new MissionSeason(List.of(step))), 1, 0, 0);
        ContentCatalog catalog = catalogWith(configuration);

        PlayerSnapshot snapshot = snapshotWithOwnedCapsule(90.0);
        int overAge = PersonnelLifecycle.ASTRONAUT_RETIREMENT_AGE + 5;
        addAssignedAstronaut(snapshot, "astro-1", 80.0, "m0", overAge);
        snapshot.getInProgressMissions().put("m0",
            new InProgressMission("m0", "complete-std", List.of("astro-1"), List.of()));

        // Growth runs before the retire-on-release check, so this still needs its post-flight
        // draws too - see completingTheFinalSeasonReleasesEveryoneAndEndsTheMission's own comment
        // above for the exact breakdown.
        ScriptedRandomGenerator random = new ScriptedRandomGenerator(99.0, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1);
        List<ResolutionEvent> events = MissionProgress.advanceSeason(snapshot, catalog, random);

        assertTrue(snapshot.getAstronauts().isEmpty());
        PersonnelDeparture departure = events.stream()
            .filter(e -> e instanceof PersonnelDeparture)
            .map(e -> (PersonnelDeparture) e)
            .findFirst()
            .orElseThrow();
        assertEquals(DepartureReason.RETIRED, departure.reason());
        assertEquals(PersonnelGroup.ASTRONAUT, departure.group());
        assertEquals(overAge, departure.ageAtExit());
    }

    /**
     * Verifies the exact age boundary: an astronaut exactly at
     * {@link PersonnelLifecycle#ASTRONAUT_RETIREMENT_AGE} also retires on
     * release, not just strictly past it.
     */
    @Test
    void astronautExactlyAtRetirementAgeRetiresOnRelease() {
        MissionStep step = new MissionStep("ops", List.of(new MissionStep.ComponentInvolvement("capsule-mercury", 100)),
            List.of(), List.of(new MissionStep.CrewInvolvement(0, AstronautSkill.PILOTING, 100)), false);
        MissionConfiguration configuration = new MissionConfiguration("complete-std", "mercury-program",
            List.of(new MissionSeason(List.of(step))), 1, 0, 0);
        ContentCatalog catalog = catalogWith(configuration);

        PlayerSnapshot snapshot = snapshotWithOwnedCapsule(90.0);
        addAssignedAstronaut(snapshot, "astro-1", 80.0, "m0", PersonnelLifecycle.ASTRONAUT_RETIREMENT_AGE);
        snapshot.getInProgressMissions().put("m0",
            new InProgressMission("m0", "complete-std", List.of("astro-1"), List.of()));

        // Same post-flight draw breakdown as completingTheFinalSeasonReleasesEveryoneAndEndsTheMission.
        ScriptedRandomGenerator random = new ScriptedRandomGenerator(99.0, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1);
        MissionProgress.advanceSeason(snapshot, catalog, random);

        assertTrue(snapshot.getAstronauts().isEmpty());
    }

    /**
     * Verifies a Mission Control officer who has already reached
     * retirement age at the moment a {@code FAILURE} releases them
     * retires on the spot too - the same retire-on-release check applies
     * to both groups, since both are shielded from departure while
     * {@code ASSIGNED}.
     */
    @Test
    void overAgeControllerRetiresAfterFailure() {
        MissionStep step = new MissionStep("launch", List.of(new MissionStep.ComponentInvolvement("capsule-mercury", 100)),
            List.of(new MissionStep.ControllerInvolvement(0, 100)),
            List.of(new MissionStep.CrewInvolvement(0, AstronautSkill.PILOTING, 100)), false);
        MissionConfiguration configuration = new MissionConfiguration("failure-std", "mercury-program",
            List.of(new MissionSeason(List.of(step))), 1, 1, 0);
        ContentCatalog catalog = catalogWith(configuration);

        PlayerSnapshot snapshot = snapshotWithOwnedCapsule(10.0);
        addAssignedAstronaut(snapshot, "astro-1", 10.0, "m0");
        int overAge = PersonnelLifecycle.MISSION_CONTROL_RETIREMENT_AGE + 3;
        addAssignedController(snapshot, "mco-1", 50.0, "m0", overAge);
        snapshot.getInProgressMissions().put("m0",
            new InProgressMission("m0", "failure-std", List.of("astro-1"), List.of("mco-1")));

        // base probability 10 (component reliability alone, below the 65 blend threshold);
        // saving-throw probability 10*50/100=5 -> pass threshold 95, failing range 95,
        // catastrophic boundary 9.5, failure boundary 57; firstRoll 0 fails, savingRoll 30 lands
        // in the plain-failure band regardless of the controller's own skill. Post-flight draws:
        // 1 reliability-change factor (FAILURE), 5 skill-growth factors for the surviving
        // astronaut, 1 for the controller's own exercised skill (PROPULSION).
        ScriptedRandomGenerator random =
            new ScriptedRandomGenerator(0.0, 30.0, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1);
        List<ResolutionEvent> events = MissionProgress.advanceSeason(snapshot, catalog, random);

        assertTrue(snapshot.getMissionControlOfficers().isEmpty());
        PersonnelDeparture controllerDeparture = events.stream()
            .filter(e -> e instanceof PersonnelDeparture departure && departure.group() == PersonnelGroup.MISSION_CONTROL)
            .map(e -> (PersonnelDeparture) e)
            .findFirst()
            .orElseThrow();
        assertEquals(DepartureReason.RETIRED, controllerDeparture.reason());
        assertEquals(overAge, controllerDeparture.ageAtExit());
    }

    /**
     * Verifies a {@code CATASTROPHIC} outcome on an over-age astronaut
     * reports only {@link DepartureReason#LOST_IN_ACCIDENT}, never a
     * second, redundant {@code RETIRED} event - {@code loseCrew} already
     * removes them from the roster before the retire-on-release check
     * would otherwise run.
     */
    @Test
    void catastrophicOutcomeOnOverAgeAstronautEmitsOnlyLostInAccident() {
        MissionStep step = new MissionStep("launch", List.of(new MissionStep.ComponentInvolvement("capsule-mercury", 100)),
            List.of(), List.of(new MissionStep.CrewInvolvement(0, AstronautSkill.PILOTING, 100)), true);
        MissionConfiguration configuration = new MissionConfiguration("catastrophic-std", "mercury-program",
            List.of(new MissionSeason(List.of(step))), 1, 0, 0);
        ContentCatalog catalog = catalogWith(configuration);

        PlayerSnapshot snapshot = snapshotWithOwnedCapsule(10.0);
        addAssignedAstronaut(snapshot, "astro-1", 10.0, "m0", PersonnelLifecycle.ASTRONAUT_RETIREMENT_AGE + 10);
        snapshot.getInProgressMissions().put("m0",
            new InProgressMission("m0", "catastrophic-std", List.of("astro-1"), List.of()));

        // Post-flight draws: 1 reliability-change factor (FAILURE, from the CATASTROPHIC
        // mapping); no skill-growth draws, since loseCrew already removed the only astronaut.
        ScriptedRandomGenerator random = new ScriptedRandomGenerator(0.0, 5.0, 0.1);
        List<ResolutionEvent> events = MissionProgress.advanceSeason(snapshot, catalog, random);

        long departureCount = events.stream().filter(e -> e instanceof PersonnelDeparture).count();
        assertEquals(1, departureCount);
        PersonnelDeparture departure = (PersonnelDeparture) events.stream()
            .filter(e -> e instanceof PersonnelDeparture)
            .findFirst()
            .orElseThrow();
        assertEquals(DepartureReason.LOST_IN_ACCIDENT, departure.reason());
        assertTrue(snapshot.getAstronauts().isEmpty());
    }

    /**
     * Verifies an over-age astronaut assigned to a mission that has not
     * yet reached its final season stays {@code ASSIGNED} - the
     * retire-on-release check only ever runs when a mission actually
     * releases its crew, never mid-mission.
     */
    @Test
    void overAgeAstronautInNonFinalSeasonStaysAssignedNotRetiredEarly() {
        MissionStep step = new MissionStep("ops", List.of(new MissionStep.ComponentInvolvement("capsule-mercury", 100)),
            List.of(), List.of(new MissionStep.CrewInvolvement(0, AstronautSkill.PILOTING, 100)), false);
        MissionConfiguration configuration = new MissionConfiguration("multi-std", "mercury-program",
            List.of(new MissionSeason(List.of(step)), new MissionSeason(List.of(step))), 1, 0, 0);
        ContentCatalog catalog = catalogWith(configuration);

        PlayerSnapshot snapshot = snapshotWithOwnedCapsule(90.0);
        addAssignedAstronaut(snapshot, "astro-1", 80.0, "m0", PersonnelLifecycle.ASTRONAUT_RETIREMENT_AGE + 10);
        snapshot.getInProgressMissions().put("m0",
            new InProgressMission("m0", "multi-std", List.of("astro-1"), List.of()));

        // Post-flight draws, same breakdown as completingTheFinalSeasonReleasesEveryoneAndEndsTheMission:
        // 1 reliability-change factor, 5 skill-growth factors - still applied even on a
        // continuing (non-final) season, since growth happens every season flown.
        ScriptedRandomGenerator random = new ScriptedRandomGenerator(99.0, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1);
        MissionProgress.advanceSeason(snapshot, catalog, random);

        assertEquals(1, snapshot.getAstronauts().size());
        assertEquals(AssignmentState.ASSIGNED, snapshot.getAstronauts().get(0).getAssignmentState());
    }

    /**
     * Verifies a mission that has not yet reached its final season, and
     * whose step resolves successfully, advances its own season index and
     * stays in progress with its crew still assigned.
     */
    @Test
    void successfulNonFinalSeasonAdvancesTheSeasonIndex() {
        MissionStep step = new MissionStep("ops", List.of(new MissionStep.ComponentInvolvement("capsule-mercury", 100)),
            List.of(new MissionStep.ControllerInvolvement(0, 100)), List.of(), false);
        MissionConfiguration configuration = new MissionConfiguration("multi-std", "mercury-program",
            List.of(new MissionSeason(List.of(step)), new MissionSeason(List.of(step))), 0, 1, 0);
        ContentCatalog catalog = catalogWith(configuration);

        PlayerSnapshot snapshot = snapshotWithOwnedCapsule(90.0);
        addAssignedController(snapshot, "mco-1", 50.0, "m0");
        snapshot.getInProgressMissions().put("m0",
            new InProgressMission("m0", "multi-std", List.of(), List.of("mco-1")));

        // Post-flight draws: 1 reliability-change factor, 1 skill-growth factor for the
        // controller's own exercised skill (PROPULSION).
        ScriptedRandomGenerator random = new ScriptedRandomGenerator(99.0, 0.1, 0.1);
        MissionProgress.advanceSeason(snapshot, catalog, random);

        assertEquals(1, snapshot.getInProgressMissions().size());
        assertEquals(1, snapshot.getInProgressMissions().get("m0").getCurrentSeasonIndex());
        assertEquals(AssignmentState.ASSIGNED, snapshot.getMissionControlOfficers().get(0).getAssignmentState());
    }

    /**
     * Verifies a {@code FAILURE} on the first of two steps in the same
     * season genuinely ends the season there - the second step never
     * resolves at all. Proven by scripting exactly the draws the first
     * step and its own post-flight reliability change consume, no more:
     * if the second step were resolved too, it would need to draw its own
     * first roll, a 4th draw the scripted generator does not have,
     * throwing - checked explicitly via {@code drawCount()} too, not just
     * relying on the absence of a throw.
     */
    @Test
    void failureOnTheFirstStepPreventsTheSecondStepFromRunning() {
        MissionStep firstStep = new MissionStep("launch",
            List.of(new MissionStep.ComponentInvolvement("capsule-mercury", 100)), List.of(), List.of(), false);
        MissionStep secondStep = new MissionStep("orbit-ops",
            List.of(new MissionStep.ComponentInvolvement("capsule-mercury", 100)), List.of(), List.of(), false);
        MissionConfiguration configuration = new MissionConfiguration("two-step-std", "mercury-program",
            List.of(new MissionSeason(List.of(firstStep, secondStep))), 0, 0, 0);
        ContentCatalog catalog = catalogWith(configuration);

        PlayerSnapshot snapshot = snapshotWithOwnedCapsule(10.0);
        snapshot.getInProgressMissions().put("m0", new InProgressMission("m0", "two-step-std", List.of(), List.of()));

        // no controllers -> saving-throw probability 0, failing range 100, failure boundary 60;
        // firstRoll 0 always fails, savingRoll 30 lands in the plain-failure band. Exactly 3
        // values total: the 2 step rolls plus 1 reliability-change factor for the one component
        // involved in the (one) step that actually executed - deliberately no extra value, so
        // that if the second step erroneously ran too, its own first roll would exhaust this
        // generator and throw, same proof mechanism this test's own Javadoc describes.
        ScriptedRandomGenerator random = new ScriptedRandomGenerator(0.0, 30.0, 0.1);
        List<ResolutionEvent> events = MissionProgress.advanceSeason(snapshot, catalog, random);

        // 1 MissionStepResolved (the one executed step) + 1 MissionReliabilityChanged (the one
        // involved component) - no skill growth, since no personnel are assigned.
        assertEquals(2, events.size());
        assertEquals("launch", ((MissionStepResolved) events.get(0)).stepId());
        assertEquals(MissionStepOutcome.FAILURE, ((MissionStepResolved) events.get(0)).outcome());
        assertTrue(snapshot.getInProgressMissions().isEmpty());
        assertEquals(3, random.drawCount());
    }

    /**
     * Verifies a {@code MINOR_GLITCH} outcome lets the mission continue:
     * the season's second step still resolves afterward, and if that
     * second step also succeeds, the (single-season) mission completes
     * normally.
     */
    @Test
    void minorGlitchLetsTheMissionContinueToTheNextStep() {
        MissionStep firstStep = new MissionStep("launch",
            List.of(new MissionStep.ComponentInvolvement("capsule-mercury", 100)), List.of(), List.of(), false);
        MissionStep secondStep = new MissionStep("orbit-ops",
            List.of(new MissionStep.ComponentInvolvement("capsule-mercury", 100)), List.of(), List.of(), false);
        MissionConfiguration configuration = new MissionConfiguration("glitch-std", "mercury-program",
            List.of(new MissionSeason(List.of(firstStep, secondStep))), 0, 0, 0);
        ContentCatalog catalog = catalogWith(configuration);

        PlayerSnapshot snapshot = snapshotWithOwnedCapsule(10.0);
        snapshot.getInProgressMissions().put("m0", new InProgressMission("m0", "glitch-std", List.of(), List.of()));

        // step 1: firstRoll 0 fails (base probability 10, threshold 90); savingRoll 80 lands in
        // the top (minor-glitch) band (failure boundary 60, saving-throw pass threshold 100).
        // step 2: firstRoll 99 succeeds outright (base probability still 10, threshold 90).
        // Post-flight draws: capsule-mercury is involved in both steps, so it is one component
        // with one aggregated category this season - MINOR_GLITCH (the worse of MINOR_GLITCH and
        // SUCCESS) - needing exactly 1 reliability-change factor, not two.
        ScriptedRandomGenerator random = new ScriptedRandomGenerator(0.0, 80.0, 99.0, 0.1);
        List<ResolutionEvent> events = MissionProgress.advanceSeason(snapshot, catalog, random);

        // 2 MissionStepResolved + 1 MissionReliabilityChanged (one component, one aggregated
        // category) - no skill growth, since no personnel are assigned.
        assertEquals(3, events.size());
        assertEquals(MissionStepOutcome.MINOR_GLITCH, ((MissionStepResolved) events.get(0)).outcome());
        assertEquals(MissionStepOutcome.SUCCESSFUL, ((MissionStepResolved) events.get(1)).outcome());
        assertTrue(snapshot.getInProgressMissions().isEmpty());
        assertEquals(4, random.drawCount());
    }

    /**
     * Verifies a {@code FAILURE} outcome ends the mission and releases the
     * assigned astronaut back to {@code IDLE} unharmed - the step is not
     * flagged crew-risk, so a worst-tier loss must not happen.
     */
    @Test
    void failureEndsTheMissionAndReleasesTheCrewUnharmed() {
        MissionStep step = new MissionStep("launch", List.of(new MissionStep.ComponentInvolvement("capsule-mercury", 100)),
            List.of(), List.of(new MissionStep.CrewInvolvement(0, AstronautSkill.PILOTING, 100)), false);
        MissionConfiguration configuration = new MissionConfiguration("failure-std", "mercury-program",
            List.of(new MissionSeason(List.of(step))), 1, 0, 0);
        ContentCatalog catalog = catalogWith(configuration);

        PlayerSnapshot snapshot = snapshotWithOwnedCapsule(10.0);
        addAssignedAstronaut(snapshot, "astro-1", 10.0, "m0");
        snapshot.getInProgressMissions().put("m0",
            new InProgressMission("m0", "failure-std", List.of("astro-1"), List.of()));

        // base/saving-throw probability both low (no controllers -> saving-throw probability 0,
        // failing range 100, catastrophic boundary 10, failure boundary 60); firstRoll 0 always
        // fails, savingRoll 30 lands in the plain-failure band. Post-flight draws: 1
        // reliability-change factor, 5 skill-growth factors for the surviving astronaut.
        ScriptedRandomGenerator random =
            new ScriptedRandomGenerator(0.0, 30.0, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1);
        List<ResolutionEvent> events = MissionProgress.advanceSeason(snapshot, catalog, random);

        MissionStepResolved resolved = (MissionStepResolved) events.get(0);
        assertEquals(MissionStepOutcome.FAILURE, resolved.outcome());
        // 1 MissionStepResolved + 1 MissionReliabilityChanged + 1 MissionSkillGrowthApplied.
        assertEquals(3, events.size());
        assertTrue(snapshot.getInProgressMissions().isEmpty());
        assertEquals(1, snapshot.getAstronauts().size());
        assertEquals(AssignmentState.IDLE, snapshot.getAstronauts().get(0).getAssignmentState());
        assertNull(snapshot.getAstronauts().get(0).getAssignedMissionId());
    }

    /**
     * Verifies a {@code CATASTROPHIC} outcome on a crew-risk step
     * permanently removes the entire assigned crew, reported as a
     * {@link PersonnelDeparture} with {@link DepartureReason#LOST_IN_ACCIDENT}
     * each, and ends the mission.
     */
    @Test
    void catastrophicOutcomeLosesTheWholeCrew() {
        MissionStep step = new MissionStep("launch", List.of(new MissionStep.ComponentInvolvement("capsule-mercury", 100)),
            List.of(), List.of(new MissionStep.CrewInvolvement(0, AstronautSkill.PILOTING, 100)), true);
        MissionConfiguration configuration = new MissionConfiguration("catastrophic-std", "mercury-program",
            List.of(new MissionSeason(List.of(step))), 2, 0, 0);
        ContentCatalog catalog = catalogWith(configuration);

        PlayerSnapshot snapshot = snapshotWithOwnedCapsule(10.0);
        addAssignedAstronaut(snapshot, "astro-1", 10.0, "m0");
        addAssignedAstronaut(snapshot, "astro-2", 10.0, "m0");
        snapshot.getInProgressMissions().put("m0",
            new InProgressMission("m0", "catastrophic-std", List.of("astro-1", "astro-2"), List.of()));

        // catastrophic boundary at 10 (no controllers -> failing range 100); savingRoll 5 lands in
        // it. Post-flight draws: 1 reliability-change factor; no skill growth, since loseCrew
        // already removed both astronauts.
        ScriptedRandomGenerator random = new ScriptedRandomGenerator(0.0, 5.0, 0.1);
        List<ResolutionEvent> events = MissionProgress.advanceSeason(snapshot, catalog, random);

        assertEquals(MissionStepOutcome.CATASTROPHIC, ((MissionStepResolved) events.get(0)).outcome());
        long departures = events.stream().filter(e -> e instanceof PersonnelDeparture).count();
        assertEquals(2, departures);
        for (ResolutionEvent event : events) {
            if (event instanceof PersonnelDeparture departure) {
                assertEquals(DepartureReason.LOST_IN_ACCIDENT, departure.reason());
                assertEquals(PersonnelGroup.ASTRONAUT, departure.group());
            }
        }
        assertTrue(snapshot.getAstronauts().isEmpty());
        assertTrue(snapshot.getInProgressMissions().isEmpty());
        assertEquals(2, snapshot.getMemorial().size(), "both are remembered");
        for (MemorialEntry remembered : snapshot.getMemorial()) {
            assertEquals(MemorialEntry.Cause.MISSION, remembered.cause());
            assertEquals("catastrophic-std", remembered.missionConfigurationId());
            assertEquals("launch", remembered.stepId());
        }
    }

    /**
     * Verifies a {@code CATASTROPHIC} outcome never touches Mission
     * Control officers - they are released to {@code IDLE}, not removed.
     */
    @Test
    void catastrophicOutcomeReleasesControllersUnharmed() {
        MissionStep step = new MissionStep("launch", List.of(new MissionStep.ComponentInvolvement("capsule-mercury", 100)),
            List.of(new MissionStep.ControllerInvolvement(0, 100)),
            List.of(new MissionStep.CrewInvolvement(0, AstronautSkill.PILOTING, 100)), true);
        MissionConfiguration configuration = new MissionConfiguration("catastrophic-ctrl-std", "mercury-program",
            List.of(new MissionSeason(List.of(step))), 1, 1, 0);
        ContentCatalog catalog = catalogWith(configuration);

        PlayerSnapshot snapshot = snapshotWithOwnedCapsule(10.0);
        addAssignedAstronaut(snapshot, "astro-1", 10.0, "m0");
        addAssignedController(snapshot, "mco-1", 10.0, "m0");
        snapshot.getInProgressMissions().put("m0",
            new InProgressMission("m0", "catastrophic-ctrl-std", List.of("astro-1"), List.of("mco-1")));

        // Post-flight draws: 1 reliability-change factor; no astronaut growth (loseCrew already
        // removed astro-1); 1 skill-growth factor for the surviving controller's own exercised
        // skill (PROPULSION).
        ScriptedRandomGenerator random = new ScriptedRandomGenerator(0.0, 1.0, 0.1, 0.1);
        MissionProgress.advanceSeason(snapshot, catalog, random);

        assertTrue(snapshot.getAstronauts().isEmpty());
        assertEquals(1, snapshot.getMissionControlOfficers().size());
        assertEquals(AssignmentState.IDLE, snapshot.getMissionControlOfficers().get(0).getAssignmentState());
        assertNull(snapshot.getMissionControlOfficers().get(0).getAssignedMissionId());
    }

    /**
     * Verifies two different in-progress missions advance independently
     * in one {@code advanceSeason} call - both produce their own events,
     * and one mission's outcome does not affect the other's state.
     */
    @Test
    void multipleInProgressMissionsAdvanceIndependently() {
        MissionStep stepA = new MissionStep("ops-a", List.of(new MissionStep.ComponentInvolvement("capsule-mercury", 100)),
            List.of(), List.of(), false);
        MissionConfiguration configurationA = new MissionConfiguration("mission-a", "mercury-program",
            List.of(new MissionSeason(List.of(stepA))), 0, 0, 0);
        MissionStep stepB = new MissionStep("ops-b", List.of(new MissionStep.ComponentInvolvement("capsule-mercury", 100)),
            List.of(), List.of(), false);
        MissionConfiguration configurationB = new MissionConfiguration("mission-b", "mercury-program",
            List.of(new MissionSeason(List.of(stepB)), new MissionSeason(List.of(stepB))), 0, 0, 0);
        ContentCatalog catalog = catalogWith(configurationA, configurationB);

        PlayerSnapshot snapshot = snapshotWithOwnedCapsule(90.0);
        snapshot.getInProgressMissions().put("m0", new InProgressMission("m0", "mission-a", List.of(), List.of()));
        snapshot.getInProgressMissions().put("m1", new InProgressMission("m1", "mission-b", List.of(), List.of()));

        // Each mission's own advanceOne call runs to completion (step, then its own post-flight
        // reliability change) before the next mission's begins - draw order is m0's step, m0's
        // reliability factor, m1's step, m1's reliability factor, not both missions' steps first.
        ScriptedRandomGenerator random = new ScriptedRandomGenerator(99.0, 0.1, 99.0, 0.1);
        List<ResolutionEvent> events = MissionProgress.advanceSeason(snapshot, catalog, random);

        // 1 MissionStepResolved + 1 MissionReliabilityChanged per mission - no skill growth,
        // since neither mission has any personnel assigned.
        assertEquals(4, events.size());
        // mission-a (one season) completed and was removed; mission-b (two seasons) advanced to
        // season 1 and is still in progress.
        assertEquals(1, snapshot.getInProgressMissions().size());
        assertEquals(1, snapshot.getInProgressMissions().get("m1").getCurrentSeasonIndex());
    }

    /**
     * Verifies a {@code SUCCESSFUL} season grows the one involved
     * component's reliability toward {@link MissionComponent#maxReliability()},
     * by the exact formula: {@code (max - current) * factor}.
     */
    @Test
    void successfulSeasonGrowsComponentReliabilityTowardMax() {
        MissionStep step = new MissionStep("ops", List.of(new MissionStep.ComponentInvolvement("capsule-mercury", 100)),
            List.of(), List.of(), false);
        MissionConfiguration configuration =
            new MissionConfiguration("growth-std", "mercury-program", List.of(new MissionSeason(List.of(step))), 0,
                0, 0);
        ContentCatalog catalog = catalogWith(configuration);

        PlayerSnapshot snapshot = snapshotWithOwnedCapsule(50.0);
        snapshot.getInProgressMissions().put("m0", new InProgressMission("m0", "growth-std", List.of(), List.of()));

        // base probability 50 (reliability alone) -> first-roll threshold 50; firstRoll 99
        // succeeds outright. Reliability factor 0.1 -> gain (90-50)*0.1=4.0.
        ScriptedRandomGenerator random = new ScriptedRandomGenerator(99.0, 0.1);
        List<ResolutionEvent> events = MissionProgress.advanceSeason(snapshot, catalog, random);

        assertEquals(54.0, snapshot.getMissionComponents().get("capsule-mercury").getCurrentReliability(), 1e-9);
        MissionReliabilityChanged changed = (MissionReliabilityChanged) events.get(1);
        assertEquals("capsule-mercury", changed.componentId());
        assertEquals(4.0, changed.delta(), 1e-9);
    }

    /**
     * Verifies a {@code FAILURE} season shrinks each involved component's
     * reliability toward {@link OwnedMissionComponent#getFloorReliability()},
     * scaled by that specific component's own involvement percentage in
     * the failing step - two components at different involvement
     * percentages in the same failing step lose different amounts even
     * with the same drawn factor.
     */
    @Test
    void failureSeasonShrinksComponentReliabilityScaledByItsOwnInvolvement() {
        MissionComponent second =
            new MissionComponent("second-component", SetSkill.ROCKETS, 3, 90, 82, RD_SLOT_BANDS, 0, List.of());
        Program twoComponentProgram = new Program("mercury-program", "early-space", 1_000, 0,
            List.of("capsule-mercury", "second-component"), List.of());
        ContentCatalog catalog = new ContentCatalog(List.of(CATEGORY), List.of(twoComponentProgram),
            List.of(new MissionConfiguration("failure-split-std", "mercury-program",
                List.of(new MissionSeason(List.of(new MissionStep("launch",
                    List.of(new MissionStep.ComponentInvolvement("capsule-mercury", 60),
                        new MissionStep.ComponentInvolvement("second-component", 40)),
                    List.of(), List.of(), false)))), 0, 0, 0)),
            List.of(CAPSULE, second));

        PlayerSnapshot snapshot = new PlayerSnapshot("Agency", Archetype.NASA, 10_000, 0, 0, 1);
        snapshot.setOpenedProgramIds(List.of("mercury-program"));
        snapshot.setMissionComponents(Map.of(
            "capsule-mercury", new OwnedMissionComponent(50.0, 20.0),
            "second-component", new OwnedMissionComponent(50.0, 20.0)));
        snapshot.getInProgressMissions().put("m0",
            new InProgressMission("m0", "failure-split-std", List.of(), List.of()));

        // componentSum = 0.6*50 + 0.4*50 = 50 -> base probability 50, first-roll threshold 50;
        // firstRoll 0 fails. No controllers -> saving-throw probability 0, failing range 100,
        // failure boundary 60; savingRoll 30 lands in the plain-failure band. capsule-mercury's
        // own failure factor is drawn first (it is listed first in componentInvolvements): loss
        // = (50-20)*0.2*0.6 = 3.6 -> new reliability 46.4.
        ScriptedRandomGenerator random = new ScriptedRandomGenerator(0.0, 30.0, 0.2, 0.3);
        MissionProgress.advanceSeason(snapshot, catalog, random);

        assertEquals(46.4, snapshot.getMissionComponents().get("capsule-mercury").getCurrentReliability(), 1e-9);
    }

    /**
     * Verifies a component's reliability never drops below its own floor
     * even at the theoretical maximum factor (1.0) and full involvement -
     * the formula's own distance-to-floor term makes this true by
     * construction, the same way {@link SkillGrowth#reliabilityGain}'s
     * distance-to-cap term does for a gain.
     */
    @Test
    void reliabilityLossNeverDropsBelowTheFloorEvenAtTheMaxFactor() {
        MissionStep step = new MissionStep("launch", List.of(new MissionStep.ComponentInvolvement("capsule-mercury", 100)),
            List.of(), List.of(), false);
        MissionConfiguration configuration = new MissionConfiguration("failure-floor-std", "mercury-program",
            List.of(new MissionSeason(List.of(step))), 0, 0, 0);
        ContentCatalog catalog = catalogWith(configuration);

        PlayerSnapshot snapshot = new PlayerSnapshot("Agency", Archetype.NASA, 10_000, 0, 0, 1);
        snapshot.setOpenedProgramIds(List.of("mercury-program"));
        snapshot.setMissionComponents(Map.of("capsule-mercury", new OwnedMissionComponent(25.0, 20.0)));
        snapshot.getInProgressMissions().put("m0",
            new InProgressMission("m0", "failure-floor-std", List.of(), List.of()));

        // base probability 25 -> first-roll threshold 75; firstRoll 0 fails. No controllers ->
        // failing range 100, failure boundary 60; savingRoll 30 lands in the plain-failure band.
        // factor 1.0, full involvement -> loss = (25-20)*1.0*1.0 = 5.0 -> lands exactly at the
        // floor, 20.0.
        ScriptedRandomGenerator random = new ScriptedRandomGenerator(0.0, 30.0, 1.0);
        MissionProgress.advanceSeason(snapshot, catalog, random);

        assertEquals(20.0, snapshot.getMissionComponents().get("capsule-mercury").getCurrentReliability(), 1e-9);
    }

    /**
     * Verifies a component's reliability never exceeds
     * {@link MissionComponent#maxReliability()} even at the theoretical
     * maximum factor (1.0).
     */
    @Test
    void reliabilityGainNeverExceedsMaxEvenAtTheMaxFactor() {
        MissionStep step = new MissionStep("ops", List.of(new MissionStep.ComponentInvolvement("capsule-mercury", 100)),
            List.of(), List.of(), false);
        MissionConfiguration configuration = new MissionConfiguration("growth-max-std", "mercury-program",
            List.of(new MissionSeason(List.of(step))), 0, 0, 0);
        ContentCatalog catalog = catalogWith(configuration);

        PlayerSnapshot snapshot = snapshotWithOwnedCapsule(85.0);
        snapshot.getInProgressMissions().put("m0",
            new InProgressMission("m0", "growth-max-std", List.of(), List.of()));

        // base probability 85 -> first-roll threshold 15; firstRoll 99 succeeds. factor 1.0 ->
        // gain (90-85)*1.0=5.0 -> lands exactly at maxReliability, 90.0.
        ScriptedRandomGenerator random = new ScriptedRandomGenerator(99.0, 1.0);
        MissionProgress.advanceSeason(snapshot, catalog, random);

        assertEquals(90.0, snapshot.getMissionComponents().get("capsule-mercury").getCurrentReliability(), 1e-9);
    }

    /**
     * Verifies reaching {@link MissionComponent#maxRdReliability()} via a
     * season's post-flight gain releases any SET specialist still
     * assigned to that component's R&amp;D, the same release mechanic
     * {@link ResearchProgress}'s own R&amp;D pass uses - re-deriving
     * {@code baspm.md} &sect;10.4's own second release trigger.
     */
    @Test
    void successfulSeasonReleasesSetResearchersAtTheRdCeiling() {
        MissionStep step = new MissionStep("ops", List.of(new MissionStep.ComponentInvolvement("capsule-mercury", 100)),
            List.of(), List.of(), false);
        MissionConfiguration configuration = new MissionConfiguration("growth-release-std", "mercury-program",
            List.of(new MissionSeason(List.of(step))), 0, 0, 0);
        ContentCatalog catalog = catalogWith(configuration);

        PlayerSnapshot snapshot = snapshotWithOwnedCapsule(80.0);
        SetSpecialist specialist = new SetSpecialist("set-1", "Set One", 40, 70, 50);
        specialist.setAssignmentState(AssignmentState.ASSIGNED);
        specialist.setRdTargetComponentId("capsule-mercury");
        specialist.setRdSlot(0);
        snapshot.setSetSpecialists(List.of(specialist));
        snapshot.getInProgressMissions().put("m0",
            new InProgressMission("m0", "growth-release-std", List.of(), List.of()));

        // base probability 80 -> first-roll threshold 20; firstRoll 99 succeeds. factor 0.5 ->
        // gain (90-80)*0.5=5.0 -> new reliability 85.0, at/above maxRdReliability (82).
        ScriptedRandomGenerator random = new ScriptedRandomGenerator(99.0, 0.5);
        List<ResolutionEvent> events = MissionProgress.advanceSeason(snapshot, catalog, random);

        assertEquals(85.0, snapshot.getMissionComponents().get("capsule-mercury").getCurrentReliability(), 1e-9);
        assertEquals(AssignmentState.IDLE, snapshot.getSetSpecialists().get(0).getAssignmentState());
        assertNull(snapshot.getSetSpecialists().get(0).getRdTargetComponentId());
        assertTrue(events.stream().anyMatch(e -> e instanceof ResearchComponentMaxed));
    }

    /**
     * Verifies mission-based skill growth bumps all five
     * {@link AstronautSkill} values for an assigned astronaut, not just
     * whichever skill a step happened to involve - re-deriving
     * {@code baspm.md} &sect;9.3/&sect;9.4's "flying is what matters"
     * shape.
     */
    @Test
    void skillGrowthBumpsAllFiveAstronautSkills() {
        MissionStep step = new MissionStep("ops", List.of(new MissionStep.ComponentInvolvement("capsule-mercury", 100)),
            List.of(), List.of(), false);
        MissionConfiguration configuration = new MissionConfiguration("skill-growth-std", "mercury-program",
            List.of(new MissionSeason(List.of(step))), 0, 0, 0);
        ContentCatalog catalog = catalogWith(configuration);

        PlayerSnapshot snapshot = snapshotWithOwnedCapsule(90.0);
        Astronaut astronaut = new Astronaut("astro-1", "Jane Reyes", 34, 80, 60);
        astronaut.setSkill(AstronautSkill.PILOTING, 50.0);
        astronaut.setAssignmentState(AssignmentState.ASSIGNED);
        astronaut.setAssignedMissionId("m0");
        snapshot.setAstronauts(List.of(astronaut));
        snapshot.getInProgressMissions().put("m0",
            new InProgressMission("m0", "skill-growth-std", List.of("astro-1"), List.of()));

        // step roll (99, succeeds) + 1 reliability factor (0.1) + 5 skill-growth factors, all
        // 0.1: gain = (100 - currentSkill) * 0.1 * (60/100) for each of the five skills -
        // PILOTING (50) -> (100-50)*0.1*0.6 = 3.0 -> 53.0; every other skill (0) ->
        // (100-0)*0.1*0.6 = 6.0 -> 6.0.
        ScriptedRandomGenerator random = new ScriptedRandomGenerator(99.0, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1);
        MissionProgress.advanceSeason(snapshot, catalog, random);

        Astronaut grown = snapshot.getAstronauts().get(0);
        assertEquals(53.0, grown.getSkill(AstronautSkill.PILOTING), 1e-9);
        assertEquals(6.0, grown.getSkill(AstronautSkill.LEADERSHIP), 1e-9);
        assertEquals(6.0, grown.getSkill(AstronautSkill.EVA), 1e-9);
        assertEquals(6.0, grown.getSkill(AstronautSkill.SCIENCE), 1e-9);
        assertEquals(6.0, grown.getSkill(AstronautSkill.FITNESS), 1e-9);
    }

    /**
     * Verifies a Mission Control officer only grows the skill(s) actually
     * exercised by the season's own executed steps, not every skill they
     * happen to have a value for - the deliberate broadening of BASPM's
     * own "one skill they were actually using" rule onto Launch Window's
     * averaged-contribution model (see {@code design.md} &sect;2.5(g)),
     * not an unconditional "grow everything" rule.
     */
    @Test
    void controllerSkillGrowthOnlyAppliesToSkillsExercisedThisSeason() {
        MissionStep step = new MissionStep("ops", List.of(new MissionStep.ComponentInvolvement("capsule-mercury", 100)),
            List.of(new MissionStep.ControllerInvolvement(0, 100)), List.of(), false);
        MissionConfiguration configuration = new MissionConfiguration("controller-growth-std", "mercury-program",
            List.of(new MissionSeason(List.of(step))), 0, 1, 0);
        ContentCatalog catalog = catalogWith(configuration);

        PlayerSnapshot snapshot = snapshotWithOwnedCapsule(90.0);
        MissionControlOfficer officer = new MissionControlOfficer("mco-1", "Alex Novak", 45, 70, 50);
        officer.setSkill(MissionControlSkill.PROPULSION, 50.0);
        officer.setSkill(MissionControlSkill.TRAJECTORY_AND_GNC, 30.0);
        officer.setAssignmentState(AssignmentState.ASSIGNED);
        officer.setAssignedMissionId("m0");
        snapshot.setMissionControlOfficers(List.of(officer));
        snapshot.getInProgressMissions().put("m0",
            new InProgressMission("m0", "controller-growth-std", List.of(), List.of("mco-1")));

        // step roll (99, succeeds) + 1 reliability factor (0.1) + exactly 1 skill-growth factor
        // (0.1), for PROPULSION only - TRAJECTORY_AND_GNC was never exercised this season and
        // must stay untouched.
        ScriptedRandomGenerator random = new ScriptedRandomGenerator(99.0, 0.1, 0.1);
        MissionProgress.advanceSeason(snapshot, catalog, random);

        MissionControlOfficer grown = snapshot.getMissionControlOfficers().get(0);
        // gain = (100-50)*0.1*(50/100) = 2.5 -> 52.5.
        assertEquals(52.5, grown.getSkill(MissionControlSkill.PROPULSION), 1e-9);
        assertEquals(30.0, grown.getSkill(MissionControlSkill.TRAJECTORY_AND_GNC), 1e-9);
    }

    /**
     * Verifies an in-progress mission whose configuration id is not found
     * in the catalog is skipped defensively, rather than throwing - not
     * expected against a validated catalog, but a graceful no-op either
     * way.
     */
    @Test
    void unknownMissionConfigurationIsSkippedGracefully() {
        ContentCatalog catalog = catalogWith();
        PlayerSnapshot snapshot = snapshotWithOwnedCapsule(90.0);
        snapshot.getInProgressMissions().put("m0", new InProgressMission("m0", "unknown", List.of(), List.of()));

        List<ResolutionEvent> events =
            MissionProgress.advanceSeason(snapshot, catalog, new ScriptedRandomGenerator());

        assertTrue(events.isEmpty());
        assertEquals(1, snapshot.getInProgressMissions().size());
    }

    /**
     * Verifies {@code advanceSeason} is a no-op, returning no events, when
     * there are no in-progress missions at all.
     */
    @Test
    void noInProgressMissionsProducesNoEvents() {
        ContentCatalog catalog = catalogWith();
        PlayerSnapshot snapshot = snapshotWithOwnedCapsule(90.0);

        List<ResolutionEvent> events =
            MissionProgress.advanceSeason(snapshot, catalog, new ScriptedRandomGenerator());

        assertTrue(events.isEmpty());
    }

    /**
     * Verifies an idle, roster-wide Expert (not assigned to the glitching
     * mission at all) boosts the saving throw: with no controller
     * actually assigned to the mission, the unboosted saving-throw
     * probability is 0 (a roll of 60 would otherwise always land in a
     * failing band), but the Expert's own 50% first-use boost raises it
     * to exactly 50, rescuing the step. Also checks the exact
     * {@link ExpertRescueApplied} event fields.
     */
    @Test
    void idleRosterWideControllerExpertRescuesTheSavingThrow() {
        MissionStep step = new MissionStep("ops", List.of(new MissionStep.ComponentInvolvement("capsule-mercury", 100)),
            List.of(new MissionStep.ControllerInvolvement(0, 100)), List.of(), false);
        MissionConfiguration configuration =
            new MissionConfiguration("expert-std", "mercury-program", List.of(new MissionSeason(List.of(step))), 0, 1,
                0);
        ContentCatalog catalog = catalogWith(configuration);

        PlayerSnapshot snapshot = snapshotWithOwnedCapsule(50.0);
        snapshot.getInProgressMissions().put("m0", new InProgressMission("m0", "expert-std", List.of(), List.of()));
        MissionControlOfficer expert = new MissionControlOfficer("mco-expert", "Idle Expert", 45, 70, 40);
        expert.getExpertSkills().add(MissionControlSkill.PROPULSION);
        snapshot.setMissionControlOfficers(List.of(expert));

        // base probability = 50 (uncrewed) -> first-roll threshold 50; firstRoll=10 fails.
        // Unboosted saving-throw probability is 0 (nobody assigned as controller to this
        // mission). The Expert's own first-use boost (50%) raises it to 50 -> pass threshold 50;
        // savingRoll=60 clears it. 1 filler for this season's reliability-change factor.
        ScriptedRandomGenerator random = new ScriptedRandomGenerator(10.0, 60.0, 0.1);
        List<ResolutionEvent> events = MissionProgress.advanceSeason(snapshot, catalog, random);

        assertEquals(MissionStepOutcome.MINOR_GLITCH_SOLVED, findEvent(events, MissionStepResolved.class).outcome());
        ExpertRescueApplied rescue = findEvent(events, ExpertRescueApplied.class);
        assertEquals("mco-expert", rescue.personnelId());
        assertEquals(PersonnelGroup.MISSION_CONTROL, rescue.group());
        assertEquals("PROPULSION", rescue.skillName());
        assertEquals(50.0, rescue.boostPercent());
        assertEquals(0.0, rescue.probabilityBefore());
        assertEquals(50.0, rescue.probabilityAfter());
    }

    /**
     * Verifies an Astronaut-Expert assigned to a <em>different</em> active
     * mission is excluded entirely - physically aboard that other
     * spacecraft, per the real-world grounding {@code design.md}
     * &sect;2.5(i) cites. With no controller involvement at all this
     * step, the unboosted saving-throw probability is always 0 regardless
     * - a roll that would land in {@code FAILURE} only if the excluded
     * Expert contributed nothing confirms the exclusion.
     */
    @Test
    void astronautExpertAssignedToAnotherMissionIsExcludedEntirely() {
        MissionStep step = new MissionStep("ops", List.of(new MissionStep.ComponentInvolvement("capsule-mercury", 100)),
            List.of(), List.of(new MissionStep.CrewInvolvement(0, AstronautSkill.PILOTING, 100)), false);
        MissionConfiguration configuration =
            new MissionConfiguration("expert-std", "mercury-program", List.of(new MissionSeason(List.of(step))), 1, 0,
                0);
        ContentCatalog catalog = catalogWith(configuration);

        PlayerSnapshot snapshot = snapshotWithOwnedCapsule(50.0);
        snapshot.getInProgressMissions().put("m0", new InProgressMission("m0", "expert-std", List.of(), List.of()));
        Astronaut expertElsewhere = new Astronaut("astro-expert", "Elsewhere Expert", 34, 80, 60);
        expertElsewhere.getExpertSkills().add(AstronautSkill.PILOTING);
        expertElsewhere.setAssignmentState(AssignmentState.ASSIGNED);
        expertElsewhere.setAssignedMissionId("some-other-mission");
        snapshot.setAstronauts(List.of(expertElsewhere));

        // firstRoll=10 fails against baseProbability 50. Unboosted saving-throw probability is 0
        // (no controller involvement this step at all) - the excluded Expert must contribute
        // nothing, so savingRoll=50 lands in the FAILURE band (passThreshold would stay 100).
        ScriptedRandomGenerator random = new ScriptedRandomGenerator(10.0, 50.0, 0.1);
        List<ResolutionEvent> events = MissionProgress.advanceSeason(snapshot, catalog, random);

        assertEquals(MissionStepOutcome.FAILURE, findEvent(events, MissionStepResolved.class).outcome());
        assertNull(findEventOrNull(events, ExpertRescueApplied.class), "excluded Expert must never be consulted");
    }

    /**
     * An Astronaut Expert on a PR tour still helps, but at 75 % (user decision 2026-10-06): on Earth,
     * but probably not in town.
     */
    @Test
    void astronautExpertOnPrTourGetsSeventyFivePercentEfficiency() {
        MissionStep step = new MissionStep("ops", List.of(new MissionStep.ComponentInvolvement("capsule-mercury", 100)),
            List.of(), List.of(new MissionStep.CrewInvolvement(0, AstronautSkill.PILOTING, 100)), false);
        MissionConfiguration configuration =
            new MissionConfiguration("expert-std", "mercury-program", List.of(new MissionSeason(List.of(step))), 1, 0,
                0);
        ContentCatalog catalog = catalogWith(configuration);

        PlayerSnapshot snapshot = snapshotWithOwnedCapsule(50.0);
        snapshot.getInProgressMissions().put("m0", new InProgressMission("m0", "expert-std", List.of(), List.of()));
        Astronaut onTour = new Astronaut("astro-expert", "Touring Expert", 34, 80, 60);
        onTour.getExpertSkills().add(AstronautSkill.PILOTING);
        onTour.setAssignmentState(AssignmentState.PR_DUTY);
        snapshot.setAstronauts(List.of(onTour));

        // 1st-use boost scaled to 75%: 0 + (100-0)*37.5/100 = 37.5 -> pass threshold 62.5; roll 70 clears it.
        ScriptedRandomGenerator random = new ScriptedRandomGenerator(10.0, 70.0, 0.1);
        List<ResolutionEvent> events = MissionProgress.advanceSeason(snapshot, catalog, random);

        assertEquals(MissionStepOutcome.MINOR_GLITCH_SOLVED, findEvent(events, MissionStepResolved.class).outcome());
        assertEquals(37.5, findEvent(events, ExpertRescueApplied.class).boostPercent());
    }

    /**
     * Verifies a Mission Control officer Expert assigned to a different
     * active mission is never excluded, but scales to 75% efficiency
     * instead - still on Earth, just not fully dedicated.
     */
    @Test
    void controllerExpertAssignedToAnotherMissionGetsSeventyFivePercentEfficiency() {
        MissionStep step = new MissionStep("ops", List.of(new MissionStep.ComponentInvolvement("capsule-mercury", 100)),
            List.of(new MissionStep.ControllerInvolvement(0, 100)), List.of(), false);
        MissionConfiguration configuration =
            new MissionConfiguration("expert-std", "mercury-program", List.of(new MissionSeason(List.of(step))), 0, 1,
                0);
        ContentCatalog catalog = catalogWith(configuration);

        PlayerSnapshot snapshot = snapshotWithOwnedCapsule(50.0);
        snapshot.getInProgressMissions().put("m0", new InProgressMission("m0", "expert-std", List.of(), List.of()));
        MissionControlOfficer expert = new MissionControlOfficer("mco-expert", "Busy Elsewhere", 45, 70, 40);
        expert.getExpertSkills().add(MissionControlSkill.PROPULSION);
        expert.setAssignmentState(AssignmentState.ASSIGNED);
        expert.setAssignedMissionId("some-other-mission");
        snapshot.setMissionControlOfficers(List.of(expert));

        // 1st-use boost scaled to 75%: 0 + (100-0)*37.5/100 = 37.5 -> pass threshold 62.5;
        // savingRoll=70 clears it (would not clear the unboosted 100 threshold).
        ScriptedRandomGenerator random = new ScriptedRandomGenerator(10.0, 70.0, 0.1);
        List<ResolutionEvent> events = MissionProgress.advanceSeason(snapshot, catalog, random);

        assertEquals(MissionStepOutcome.MINOR_GLITCH_SOLVED, findEvent(events, MissionStepResolved.class).outcome());
        ExpertRescueApplied rescue = findEvent(events, ExpertRescueApplied.class);
        assertEquals(37.5, rescue.boostPercent());
        assertEquals(37.5, rescue.probabilityAfter());
    }

    /**
     * Verifies an Expert currently {@code IN_TRAINING} is still fully
     * eligible - "never unavailable" per {@code design.md} &sect;2.2(c)'s
     * own 2026-09-27 wording, re-confirmed explicitly for this mechanic.
     */
    @Test
    void controllerExpertCurrentlyInTrainingIsStillFullyEligible() {
        MissionStep step = new MissionStep("ops", List.of(new MissionStep.ComponentInvolvement("capsule-mercury", 100)),
            List.of(new MissionStep.ControllerInvolvement(0, 100)), List.of(), false);
        MissionConfiguration configuration =
            new MissionConfiguration("expert-std", "mercury-program", List.of(new MissionSeason(List.of(step))), 0, 1,
                0);
        ContentCatalog catalog = catalogWith(configuration);

        PlayerSnapshot snapshot = snapshotWithOwnedCapsule(50.0);
        snapshot.getInProgressMissions().put("m0", new InProgressMission("m0", "expert-std", List.of(), List.of()));
        MissionControlOfficer expert = new MissionControlOfficer("mco-expert", "In Training", 45, 70, 40);
        expert.getExpertSkills().add(MissionControlSkill.PROPULSION);
        expert.setAssignmentState(AssignmentState.IN_TRAINING);
        expert.setTrainingKind(TrainingKind.BASIC);
        snapshot.setMissionControlOfficers(List.of(expert));

        // Same math as the idle case - full-strength 50% first-use boost.
        ScriptedRandomGenerator random = new ScriptedRandomGenerator(10.0, 60.0, 0.1);
        List<ResolutionEvent> events = MissionProgress.advanceSeason(snapshot, catalog, random);

        assertEquals(MissionStepOutcome.MINOR_GLITCH_SOLVED, findEvent(events, MissionStepResolved.class).outcome());
        assertEquals(50.0, findEvent(events, ExpertRescueApplied.class).boostPercent());
    }

    /**
     * Verifies two different Experts stack sequentially, each against the
     * previous one's own result, not independently against the original
     * unboosted value - each is a genuinely different employee, so each
     * gets their own first-use 50% boost (exhaustion is per employee, not
     * per step): 50% of the remaining 0-to-100 gap, then 50% of the
     * remaining 50-to-100 gap, gives 75 - distinct from both a single
     * Expert's own 50 and a naive "both against the original value" reading's
     * own 100.
     */
    @Test
    void twoDifferentExpertsStackSequentially() {
        MissionStep step = new MissionStep("ops", List.of(new MissionStep.ComponentInvolvement("capsule-mercury", 100)),
            List.of(new MissionStep.ControllerInvolvement(0, 100)), List.of(), false);
        MissionConfiguration configuration =
            new MissionConfiguration("expert-std", "mercury-program", List.of(new MissionSeason(List.of(step))), 0, 1,
                0);
        ContentCatalog catalog = catalogWith(configuration);

        PlayerSnapshot snapshot = snapshotWithOwnedCapsule(50.0);
        snapshot.getInProgressMissions().put("m0", new InProgressMission("m0", "expert-std", List.of(), List.of()));
        MissionControlOfficer expertA = new MissionControlOfficer("mco-a", "Expert A", 45, 70, 40);
        expertA.getExpertSkills().add(MissionControlSkill.PROPULSION);
        MissionControlOfficer expertB = new MissionControlOfficer("mco-b", "Expert B", 45, 70, 40);
        expertB.getExpertSkills().add(MissionControlSkill.PROPULSION);
        snapshot.setMissionControlOfficers(List.of(expertA, expertB));

        // 0 -> (A, own 1st use, 50%) -> 50 -> (B, own 1st use, 50% of the remaining 50) -> 75.
        // Pass threshold 25; savingRoll=50 clears it.
        ScriptedRandomGenerator random = new ScriptedRandomGenerator(10.0, 50.0, 0.1);
        List<ResolutionEvent> events = MissionProgress.advanceSeason(snapshot, catalog, random);

        assertEquals(MissionStepOutcome.MINOR_GLITCH_SOLVED, findEvent(events, MissionStepResolved.class).outcome());
        List<ExpertRescueApplied> rescues = findEvents(events, ExpertRescueApplied.class);
        assertEquals(2, rescues.size());
        assertEquals("mco-a", rescues.get(0).personnelId());
        assertEquals(0.0, rescues.get(0).probabilityBefore());
        assertEquals(50.0, rescues.get(0).probabilityAfter());
        assertEquals("mco-b", rescues.get(1).personnelId());
        assertEquals(50.0, rescues.get(1).probabilityBefore());
        assertEquals(75.0, rescues.get(1).probabilityAfter());
    }

    /**
     * Verifies the same Expert's exhaustion carries across two different
     * missions resolved in the same {@code advanceSeason} call - their
     * second use this turn (across either mission) gets the second-use
     * 40% boost, not another first-use 50%. A roll chosen to pass under
     * the correct 40% boost's own pass threshold but fail under a
     * (buggy) repeated-50% reading's threshold distinguishes the two.
     */
    @Test
    void sameExpertsExhaustionCarriesAcrossDifferentMissionsInOneTurn() {
        MissionStep step = new MissionStep("ops", List.of(new MissionStep.ComponentInvolvement("capsule-mercury", 100)),
            List.of(new MissionStep.ControllerInvolvement(0, 100)), List.of(), false);
        MissionConfiguration configuration =
            new MissionConfiguration("expert-std", "mercury-program", List.of(new MissionSeason(List.of(step))), 0, 1,
                0);
        ContentCatalog catalog = catalogWith(configuration);

        PlayerSnapshot snapshot = snapshotWithOwnedCapsule(50.0);
        snapshot.getInProgressMissions().put("m0", new InProgressMission("m0", "expert-std", List.of(), List.of()));
        snapshot.getInProgressMissions().put("m1", new InProgressMission("m1", "expert-std", List.of(), List.of()));
        MissionControlOfficer expert = new MissionControlOfficer("mco-expert", "Busy Expert", 45, 70, 40);
        expert.getExpertSkills().add(MissionControlSkill.PROPULSION);
        snapshot.setMissionControlOfficers(List.of(expert));

        // m0 (processed first, map insertion order): 1st use, 50% boost -> 0 to 50, pass
        // threshold 50; savingRoll=60 clears it (MINOR_GLITCH_SOLVED).
        // m1: 2nd use, 40% boost -> 0 to 40, pass threshold 60; savingRoll=55 does NOT clear 60
        // (MINOR_GLITCH, unsolved) - but would have cleared a repeated-50%-boost's own threshold
        // of 50, so this exact roll is what actually proves exhaustion carried over.
        ScriptedRandomGenerator random = new ScriptedRandomGenerator(10.0, 60.0, 0.1, 10.0, 55.0, 0.1);
        List<ResolutionEvent> events = MissionProgress.advanceSeason(snapshot, catalog, random);

        List<MissionStepResolved> resolved = findEvents(events, MissionStepResolved.class);
        assertEquals(MissionStepOutcome.MINOR_GLITCH_SOLVED, resolved.get(0).outcome());
        assertEquals(MissionStepOutcome.MINOR_GLITCH, resolved.get(1).outcome());
        List<ExpertRescueApplied> rescues = findEvents(events, ExpertRescueApplied.class);
        assertEquals(2, rescues.size());
        assertEquals(50.0, rescues.get(0).boostPercent());
        assertEquals(40.0, rescues.get(1).boostPercent());
    }

    /**
     * Finds the first event of the given type, failing the test if none
     * exists.
     *
     * @param events the events to search
     * @param type   the event type to find
     * @param <T>    the event type
     * @return the first matching event
     */
    private static <T extends ResolutionEvent> T findEvent(List<ResolutionEvent> events, Class<T> type) {
        T found = findEventOrNull(events, type);
        assertNotNull(found, "expected an event of type " + type.getSimpleName());
        return found;
    }

    /**
     * Finds the first event of the given type, or {@code null} if none
     * exists.
     *
     * @param events the events to search
     * @param type   the event type to find
     * @param <T>    the event type
     * @return the first matching event, or {@code null}
     */
    private static <T extends ResolutionEvent> T findEventOrNull(List<ResolutionEvent> events, Class<T> type) {
        for (ResolutionEvent event : events) {
            if (type.isInstance(event)) {
                return type.cast(event);
            }
        }
        return null;
    }

    /**
     * Finds every event of the given type, in their original relative
     * order.
     *
     * @param events the events to search
     * @param type   the event type to find
     * @param <T>    the event type
     * @return every matching event, in order
     */
    private static <T extends ResolutionEvent> List<T> findEvents(List<ResolutionEvent> events, Class<T> type) {
        List<T> found = new java.util.ArrayList<>();
        for (ResolutionEvent event : events) {
            if (type.isInstance(event)) {
                found.add(type.cast(event));
            }
        }
        return found;
    }

    /**
     * A {@link RandomGenerator} stub that returns a fixed, ordered
     * sequence of {@code nextDouble(origin, bound)} results - same shape
     * as {@code MissionStepResolutionTest}'s own helper, duplicated here
     * since it is {@code private} there.
     */
    private static final class ScriptedRandomGenerator implements RandomGenerator {

        private final double[] scriptedRolls;
        private int drawCount = 0;

        /**
         * @param scriptedRolls the rolls to return, in order, one per call
         *                       to {@link #nextDouble(double, double)}
         */
        private ScriptedRandomGenerator(double... scriptedRolls) {
            this.scriptedRolls = scriptedRolls;
        }

        /**
         * Not used by {@link MissionStepResolution}, which only ever calls
         * {@link #nextDouble(double, double)}.
         *
         * @throws UnsupportedOperationException always
         */
        @Override
        public long nextLong() {
            throw new UnsupportedOperationException("not used by MissionStepResolution");
        }

        /**
         * Returns the next scripted roll in sequence, ignoring
         * {@code origin}/{@code bound}.
         *
         * @param origin unused
         * @param bound  unused
         * @return the next scripted roll
         * @throws IllegalStateException if more rolls are drawn than were scripted
         */
        @Override
        public double nextDouble(double origin, double bound) {
            if (drawCount >= scriptedRolls.length) {
                throw new IllegalStateException("no more scripted rolls available");
            }
            return scriptedRolls[drawCount++];
        }

        /**
         * @return how many times {@link #nextDouble(double, double)} was
         *     actually called
         */
        private int drawCount() {
            return drawCount;
        }
    }
}
