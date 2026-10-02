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
import de.mkoehler.launchwindow.model.MissionControlOfficer;
import de.mkoehler.launchwindow.model.MissionControlSkill;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSkill;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
     * Verifies the crew contribution is the average skill across every
     * assigned astronaut, not the sum, the best member, or just the first
     * id in the list: with capsule reliability 80 (clears the 65 blend
     * threshold) and two astronauts at piloting 40/90, the correct average
     * (65) gives base probability 80*0.8+65*0.2=77 and saving-throw
     * probability 77*50/100=38.5 (pass threshold 61.5). A first roll of 20
     * fails under the average or a first-member-only (skill 40 alone, base
     * 72, pass threshold 28) reading, but would instead succeed outright
     * under a max (skill 90, base 82, threshold 18) or sum (base 90+,
     * threshold under 10) reading, which never even reach the saving
     * throw. A saving roll of 63 then distinguishes the two readings that
     * did fall through: 63 &gt; 61.5 resolves to {@code MINOR_GLITCH_SOLVED}
     * under the correct average, but the first-member-only reading's own
     * pass threshold is 64 (saving-throw probability 72*50/100=36), so 63
     * is not above it, landing in the {@code MINOR_GLITCH} band instead.
     */
    @Test
    void crewContributionIsTheAverageAcrossAssignedMembersNotSumOrMax() {
        MissionStep step = new MissionStep("ops", List.of(new MissionStep.ComponentInvolvement("capsule-mercury", 100)),
            List.of(new MissionStep.ControllerInvolvement(MissionControlSkill.PROPULSION, 100)),
            List.of(new MissionStep.CrewInvolvement(AstronautSkill.PILOTING, 100)), false);
        MissionConfiguration configuration =
            new MissionConfiguration("avg-std", "mercury-program", List.of(new MissionSeason(List.of(step))), 2, 1);
        ContentCatalog catalog = catalogWith(configuration);

        PlayerSnapshot snapshot = snapshotWithOwnedCapsule(80.0);
        addAssignedAstronaut(snapshot, "astro-1", 40.0, "m0");
        addAssignedAstronaut(snapshot, "astro-2", 90.0, "m0");
        addAssignedController(snapshot, "mco-1", 50.0, "m0");
        snapshot.getInProgressMissions().put("m0",
            new InProgressMission("m0", "avg-std", List.of("astro-1", "astro-2"), List.of("mco-1")));

        // base probability (average) = 77 -> first-roll threshold 23; saving-throw probability
        // = 77 * 50 / 100 = 38.5 -> saving-throw pass threshold 61.5. A first-member-only bug
        // would instead compute a pass threshold of 64 - see this test's own Javadoc.
        ScriptedRandomGenerator random = new ScriptedRandomGenerator(20.0, 63.0);
        List<ResolutionEvent> events = MissionProgress.advanceSeason(snapshot, catalog, random);

        MissionStepResolved resolved = (MissionStepResolved) events.get(0);
        assertEquals(MissionStepOutcome.MINOR_GLITCH_SOLVED, resolved.outcome());
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
            List.of(), List.of(new MissionStep.CrewInvolvement(AstronautSkill.PILOTING, 100)), false);
        MissionConfiguration configuration = new MissionConfiguration("complete-std", "mercury-program",
            List.of(new MissionSeason(List.of(step))), 1, 0);
        ContentCatalog catalog = catalogWith(configuration);

        PlayerSnapshot snapshot = snapshotWithOwnedCapsule(90.0);
        addAssignedAstronaut(snapshot, "astro-1", 80.0, "m0");
        snapshot.getInProgressMissions().put("m0",
            new InProgressMission("m0", "complete-std", List.of("astro-1"), List.of()));

        ScriptedRandomGenerator random = new ScriptedRandomGenerator(99.0);
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
            List.of(), List.of(new MissionStep.CrewInvolvement(AstronautSkill.PILOTING, 100)), false);
        MissionConfiguration configuration = new MissionConfiguration("complete-std", "mercury-program",
            List.of(new MissionSeason(List.of(step))), 1, 0);
        ContentCatalog catalog = catalogWith(configuration);

        PlayerSnapshot snapshot = snapshotWithOwnedCapsule(90.0);
        int overAge = PersonnelLifecycle.ASTRONAUT_RETIREMENT_AGE + 5;
        addAssignedAstronaut(snapshot, "astro-1", 80.0, "m0", overAge);
        snapshot.getInProgressMissions().put("m0",
            new InProgressMission("m0", "complete-std", List.of("astro-1"), List.of()));

        ScriptedRandomGenerator random = new ScriptedRandomGenerator(99.0);
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
            List.of(), List.of(new MissionStep.CrewInvolvement(AstronautSkill.PILOTING, 100)), false);
        MissionConfiguration configuration = new MissionConfiguration("complete-std", "mercury-program",
            List.of(new MissionSeason(List.of(step))), 1, 0);
        ContentCatalog catalog = catalogWith(configuration);

        PlayerSnapshot snapshot = snapshotWithOwnedCapsule(90.0);
        addAssignedAstronaut(snapshot, "astro-1", 80.0, "m0", PersonnelLifecycle.ASTRONAUT_RETIREMENT_AGE);
        snapshot.getInProgressMissions().put("m0",
            new InProgressMission("m0", "complete-std", List.of("astro-1"), List.of()));

        ScriptedRandomGenerator random = new ScriptedRandomGenerator(99.0);
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
            List.of(new MissionStep.ControllerInvolvement(MissionControlSkill.PROPULSION, 100)),
            List.of(new MissionStep.CrewInvolvement(AstronautSkill.PILOTING, 100)), false);
        MissionConfiguration configuration = new MissionConfiguration("failure-std", "mercury-program",
            List.of(new MissionSeason(List.of(step))), 1, 1);
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
        // in the plain-failure band regardless of the controller's own skill.
        ScriptedRandomGenerator random = new ScriptedRandomGenerator(0.0, 30.0);
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
            List.of(), List.of(new MissionStep.CrewInvolvement(AstronautSkill.PILOTING, 100)), true);
        MissionConfiguration configuration = new MissionConfiguration("catastrophic-std", "mercury-program",
            List.of(new MissionSeason(List.of(step))), 1, 0);
        ContentCatalog catalog = catalogWith(configuration);

        PlayerSnapshot snapshot = snapshotWithOwnedCapsule(10.0);
        addAssignedAstronaut(snapshot, "astro-1", 10.0, "m0", PersonnelLifecycle.ASTRONAUT_RETIREMENT_AGE + 10);
        snapshot.getInProgressMissions().put("m0",
            new InProgressMission("m0", "catastrophic-std", List.of("astro-1"), List.of()));

        ScriptedRandomGenerator random = new ScriptedRandomGenerator(0.0, 5.0);
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
            List.of(), List.of(new MissionStep.CrewInvolvement(AstronautSkill.PILOTING, 100)), false);
        MissionConfiguration configuration = new MissionConfiguration("multi-std", "mercury-program",
            List.of(new MissionSeason(List.of(step)), new MissionSeason(List.of(step))), 1, 0);
        ContentCatalog catalog = catalogWith(configuration);

        PlayerSnapshot snapshot = snapshotWithOwnedCapsule(90.0);
        addAssignedAstronaut(snapshot, "astro-1", 80.0, "m0", PersonnelLifecycle.ASTRONAUT_RETIREMENT_AGE + 10);
        snapshot.getInProgressMissions().put("m0",
            new InProgressMission("m0", "multi-std", List.of("astro-1"), List.of()));

        ScriptedRandomGenerator random = new ScriptedRandomGenerator(99.0);
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
            List.of(new MissionStep.ControllerInvolvement(MissionControlSkill.PROPULSION, 100)), List.of(), false);
        MissionConfiguration configuration = new MissionConfiguration("multi-std", "mercury-program",
            List.of(new MissionSeason(List.of(step)), new MissionSeason(List.of(step))), 0, 1);
        ContentCatalog catalog = catalogWith(configuration);

        PlayerSnapshot snapshot = snapshotWithOwnedCapsule(90.0);
        addAssignedController(snapshot, "mco-1", 50.0, "m0");
        snapshot.getInProgressMissions().put("m0",
            new InProgressMission("m0", "multi-std", List.of(), List.of("mco-1")));

        ScriptedRandomGenerator random = new ScriptedRandomGenerator(99.0);
        MissionProgress.advanceSeason(snapshot, catalog, random);

        assertEquals(1, snapshot.getInProgressMissions().size());
        assertEquals(1, snapshot.getInProgressMissions().get("m0").getCurrentSeasonIndex());
        assertEquals(AssignmentState.ASSIGNED, snapshot.getMissionControlOfficers().get(0).getAssignmentState());
    }

    /**
     * Verifies a {@code FAILURE} on the first of two steps in the same
     * season genuinely ends the season there - the second step never
     * resolves at all. Proven by scripting exactly the two rolls the first
     * step consumes: if the second step were resolved too, it would need
     * to draw a third roll the scripted generator does not have, throwing.
     */
    @Test
    void failureOnTheFirstStepPreventsTheSecondStepFromRunning() {
        MissionStep firstStep = new MissionStep("launch",
            List.of(new MissionStep.ComponentInvolvement("capsule-mercury", 100)), List.of(), List.of(), false);
        MissionStep secondStep = new MissionStep("orbit-ops",
            List.of(new MissionStep.ComponentInvolvement("capsule-mercury", 100)), List.of(), List.of(), false);
        MissionConfiguration configuration = new MissionConfiguration("two-step-std", "mercury-program",
            List.of(new MissionSeason(List.of(firstStep, secondStep))), 0, 0);
        ContentCatalog catalog = catalogWith(configuration);

        PlayerSnapshot snapshot = snapshotWithOwnedCapsule(10.0);
        snapshot.getInProgressMissions().put("m0", new InProgressMission("m0", "two-step-std", List.of(), List.of()));

        // no controllers -> saving-throw probability 0, failing range 100, failure boundary 60;
        // firstRoll 0 always fails, savingRoll 30 lands in the plain-failure band.
        ScriptedRandomGenerator random = new ScriptedRandomGenerator(0.0, 30.0);
        List<ResolutionEvent> events = MissionProgress.advanceSeason(snapshot, catalog, random);

        assertEquals(1, events.size());
        assertEquals("launch", ((MissionStepResolved) events.get(0)).stepId());
        assertEquals(MissionStepOutcome.FAILURE, ((MissionStepResolved) events.get(0)).outcome());
        assertTrue(snapshot.getInProgressMissions().isEmpty());
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
            List.of(new MissionSeason(List.of(firstStep, secondStep))), 0, 0);
        ContentCatalog catalog = catalogWith(configuration);

        PlayerSnapshot snapshot = snapshotWithOwnedCapsule(10.0);
        snapshot.getInProgressMissions().put("m0", new InProgressMission("m0", "glitch-std", List.of(), List.of()));

        // step 1: firstRoll 0 fails (base probability 10, threshold 90); savingRoll 80 lands in
        // the top (minor-glitch) band (failure boundary 60, saving-throw pass threshold 100).
        // step 2: firstRoll 99 succeeds outright (base probability still 10, threshold 90).
        ScriptedRandomGenerator random = new ScriptedRandomGenerator(0.0, 80.0, 99.0);
        List<ResolutionEvent> events = MissionProgress.advanceSeason(snapshot, catalog, random);

        assertEquals(2, events.size());
        assertEquals(MissionStepOutcome.MINOR_GLITCH, ((MissionStepResolved) events.get(0)).outcome());
        assertEquals(MissionStepOutcome.SUCCESSFUL, ((MissionStepResolved) events.get(1)).outcome());
        assertTrue(snapshot.getInProgressMissions().isEmpty());
    }

    /**
     * Verifies a {@code FAILURE} outcome ends the mission and releases the
     * assigned astronaut back to {@code IDLE} unharmed - the step is not
     * flagged crew-risk, so a worst-tier loss must not happen.
     */
    @Test
    void failureEndsTheMissionAndReleasesTheCrewUnharmed() {
        MissionStep step = new MissionStep("launch", List.of(new MissionStep.ComponentInvolvement("capsule-mercury", 100)),
            List.of(), List.of(new MissionStep.CrewInvolvement(AstronautSkill.PILOTING, 100)), false);
        MissionConfiguration configuration = new MissionConfiguration("failure-std", "mercury-program",
            List.of(new MissionSeason(List.of(step))), 1, 0);
        ContentCatalog catalog = catalogWith(configuration);

        PlayerSnapshot snapshot = snapshotWithOwnedCapsule(10.0);
        addAssignedAstronaut(snapshot, "astro-1", 10.0, "m0");
        snapshot.getInProgressMissions().put("m0",
            new InProgressMission("m0", "failure-std", List.of("astro-1"), List.of()));

        // base/saving-throw probability both low (no controllers -> saving-throw probability 0,
        // failing range 100, catastrophic boundary 10, failure boundary 60); firstRoll 0 always
        // fails, savingRoll 30 lands in the plain-failure band.
        ScriptedRandomGenerator random = new ScriptedRandomGenerator(0.0, 30.0);
        List<ResolutionEvent> events = MissionProgress.advanceSeason(snapshot, catalog, random);

        MissionStepResolved resolved = (MissionStepResolved) events.get(0);
        assertEquals(MissionStepOutcome.FAILURE, resolved.outcome());
        assertEquals(1, events.size());
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
            List.of(), List.of(new MissionStep.CrewInvolvement(AstronautSkill.PILOTING, 100)), true);
        MissionConfiguration configuration = new MissionConfiguration("catastrophic-std", "mercury-program",
            List.of(new MissionSeason(List.of(step))), 2, 0);
        ContentCatalog catalog = catalogWith(configuration);

        PlayerSnapshot snapshot = snapshotWithOwnedCapsule(10.0);
        addAssignedAstronaut(snapshot, "astro-1", 10.0, "m0");
        addAssignedAstronaut(snapshot, "astro-2", 10.0, "m0");
        snapshot.getInProgressMissions().put("m0",
            new InProgressMission("m0", "catastrophic-std", List.of("astro-1", "astro-2"), List.of()));

        // catastrophic boundary at 10 (no controllers -> failing range 100); savingRoll 5 lands in it.
        ScriptedRandomGenerator random = new ScriptedRandomGenerator(0.0, 5.0);
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
    }

    /**
     * Verifies a {@code CATASTROPHIC} outcome never touches Mission
     * Control officers - they are released to {@code IDLE}, not removed.
     */
    @Test
    void catastrophicOutcomeReleasesControllersUnharmed() {
        MissionStep step = new MissionStep("launch", List.of(new MissionStep.ComponentInvolvement("capsule-mercury", 100)),
            List.of(new MissionStep.ControllerInvolvement(MissionControlSkill.PROPULSION, 100)),
            List.of(new MissionStep.CrewInvolvement(AstronautSkill.PILOTING, 100)), true);
        MissionConfiguration configuration = new MissionConfiguration("catastrophic-ctrl-std", "mercury-program",
            List.of(new MissionSeason(List.of(step))), 1, 1);
        ContentCatalog catalog = catalogWith(configuration);

        PlayerSnapshot snapshot = snapshotWithOwnedCapsule(10.0);
        addAssignedAstronaut(snapshot, "astro-1", 10.0, "m0");
        addAssignedController(snapshot, "mco-1", 10.0, "m0");
        snapshot.getInProgressMissions().put("m0",
            new InProgressMission("m0", "catastrophic-ctrl-std", List.of("astro-1"), List.of("mco-1")));

        ScriptedRandomGenerator random = new ScriptedRandomGenerator(0.0, 1.0);
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
            List.of(new MissionSeason(List.of(stepA))), 0, 0);
        MissionStep stepB = new MissionStep("ops-b", List.of(new MissionStep.ComponentInvolvement("capsule-mercury", 100)),
            List.of(), List.of(), false);
        MissionConfiguration configurationB = new MissionConfiguration("mission-b", "mercury-program",
            List.of(new MissionSeason(List.of(stepB)), new MissionSeason(List.of(stepB))), 0, 0);
        ContentCatalog catalog = catalogWith(configurationA, configurationB);

        PlayerSnapshot snapshot = snapshotWithOwnedCapsule(90.0);
        snapshot.getInProgressMissions().put("m0", new InProgressMission("m0", "mission-a", List.of(), List.of()));
        snapshot.getInProgressMissions().put("m1", new InProgressMission("m1", "mission-b", List.of(), List.of()));

        ScriptedRandomGenerator random = new ScriptedRandomGenerator(99.0, 99.0);
        List<ResolutionEvent> events = MissionProgress.advanceSeason(snapshot, catalog, random);

        assertEquals(2, events.size());
        // mission-a (one season) completed and was removed; mission-b (two seasons) advanced to
        // season 1 and is still in progress.
        assertEquals(1, snapshot.getInProgressMissions().size());
        assertEquals(1, snapshot.getInProgressMissions().get("m1").getCurrentSeasonIndex());
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
    }
}
