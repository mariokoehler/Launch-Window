package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.LaunchType;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.content.MissionSeason;
import de.mkoehler.launchwindow.content.MissionStep;
import de.mkoehler.launchwindow.content.Program;
import de.mkoehler.launchwindow.content.ProgramCategory;
import de.mkoehler.launchwindow.content.RocketClass;
import de.mkoehler.launchwindow.content.TestBuildings;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.Building;
import de.mkoehler.launchwindow.model.InProgressMission;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSkill;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for M3 slice 3b: rocket programs behind the VAB, and the rocket chosen per
 * launch - payload capacity, human rating, launch cost, and the rocket slot resolving
 * to the chosen rocket in flight. See {@code design.md} &sect;5 (M3) and
 * {@code baspm.md} &sect;10.9.
 *
 * @author Mario Koehler
 */
class RocketsTest {

    private static final ProgramCategory CATEGORY = new ProgramCategory("sats", "Satellites");
    private static final MissionComponent SATELLITE = new MissionComponent("satellite", SetSkill.SPACE_PROBES,
        3, 90, 80, List.of(1.0, 2.0, 3.0, 4.0), 0, List.of(), 0, 0, 488, 84, null, null);
    private static final MissionComponent LIGHT = rocket("light-rocket", SetSkill.ROCKETS, RocketClass.LIGHT,
        2_500, 1_625, 500, null);
    private static final MissionComponent WEAK = rocket("weak-rocket", SetSkill.ROCKETS, RocketClass.LIGHT,
        1_000, 500, 50, null);
    private static final MissionComponent HUMAN_RATED = rocket("hr-rocket", SetSkill.HUMAN_RATED_ROCKETS,
        RocketClass.LIGHT, 5_000, 3_000, 500, null);
    private static final MissionComponent MEDIUM = rocket("medium-rocket", SetSkill.ROCKETS, RocketClass.MEDIUM,
        6_000, 3_900, 9_000, null);
    private static final MissionComponent SPARE = rocket("spare-rocket", SetSkill.ROCKETS, RocketClass.LIGHT,
        1_000, 500, 500, null);
    private static final MissionComponent NASA_ROCKET = rocket("nasa-rocket", SetSkill.ROCKETS, RocketClass.LIGHT,
        1_800, 1_170, 500, Archetype.NASA);
    private static final Program PROGRAM = new Program("sat-program", "sats", 126, 13, List.of("satellite"),
        List.of());
    private static final MissionConfiguration ORBIT = new MissionConfiguration("orbit", "sat-program",
        List.of(new MissionSeason(List.of(new MissionStep("launch",
            List.of(new MissionStep.ComponentInvolvement(MissionConfiguration.rocketSlotId(1), 90),
                new MissionStep.ComponentInvolvement("satellite", 10)), List.of(), List.of(), false)))),
        0, 0, 0, 0, List.of(), List.of(),
        List.of(new MissionConfiguration.Launch(LaunchType.LEO, List.of("satellite"))));
    private static final MissionConfiguration CREWED_STYLE = new MissionConfiguration("orbit-hr", "sat-program",
        List.of(new MissionSeason(List.of(new MissionStep("launch",
            List.of(new MissionStep.ComponentInvolvement(MissionConfiguration.rocketSlotId(1), 100)),
            List.of(), List.of(), false)))),
        0, 0, 0, 0, List.of(), List.of(),
        List.of(new MissionConfiguration.Launch(LaunchType.LEO_HR, List.of("satellite"))));
    private static final ContentCatalog CATALOG = TestBuildings.catalogWithoutUpkeep(List.of(CATEGORY),
        List.of(PROGRAM), List.of(ORBIT, CREWED_STYLE),
        List.of(SATELLITE, LIGHT, WEAK, HUMAN_RATED, MEDIUM, SPARE, NASA_ROCKET));

    private static MissionComponent rocket(String id, SetSkill skill, RocketClass rocketClass, long creationCost,
            long unitCost, double payloadToLeo, Archetype agency) {
        return new MissionComponent(id, skill, 3, 90, 80, List.of(1.0, 2.0, 3.0, 4.0), 0, List.of(), 250,
            creationCost, unitCost, 0, agency, new MissionComponent.RocketSpec(rocketClass, payloadToLeo * 4,
                payloadToLeo, 0, 0));
    }

    private static PlayerSnapshot soviet(long funds) {
        PlayerSnapshot snapshot = new PlayerSnapshot("Agency", Archetype.SOVIET, funds, 0, 0, 1);
        ProgramOpening.open(snapshot, "sat-program", CATALOG);
        return snapshot;
    }

    private static LaunchRequest orbitWith(String configurationId, String rocketId) {
        return new LaunchRequest(configurationId, List.of(), List.of(), List.of(), List.of(rocketId));
    }

    @Test
    void openingARocketProgramPaysItsCreationCost() {
        PlayerSnapshot snapshot = soviet(10_000);
        long before = snapshot.getFunds();

        assertEquals(RocketProgramOutcome.OPENED, RocketPrograms.open(snapshot, "light-rocket", CATALOG));
        assertEquals(before - 2_500, snapshot.getFunds());
        assertEquals(1, RocketPrograms.openCount(snapshot, CATALOG));
        assertEquals(RocketProgramOutcome.ALREADY_OPEN, RocketPrograms.open(snapshot, "light-rocket", CATALOG));
    }

    @Test
    void rocketProgramsAreGatedByAgencyClassAndVabCap() {
        PlayerSnapshot snapshot = soviet(100_000);

        assertEquals(RocketProgramOutcome.WRONG_AGENCY, RocketPrograms.open(snapshot, "nasa-rocket", CATALOG));
        assertEquals(RocketProgramOutcome.ROCKET_CLASS_TOO_LARGE,
            RocketPrograms.open(snapshot, "medium-rocket", CATALOG));
        assertEquals(RocketProgramOutcome.UNKNOWN_ROCKET, RocketPrograms.open(snapshot, "satellite", CATALOG));

        snapshot.getBuildingLevels().put(Building.VEHICLE_ASSEMBLY, 2);
        assertEquals(RocketProgramOutcome.OPENED, RocketPrograms.open(snapshot, "medium-rocket", CATALOG));

        snapshot.getBuildingLevels().put(Building.VEHICLE_ASSEMBLY, 1);
        RocketPrograms.open(snapshot, "light-rocket", CATALOG);
        RocketPrograms.open(snapshot, "weak-rocket", CATALOG);
        RocketPrograms.open(snapshot, "hr-rocket", CATALOG);
        assertEquals(4, RocketPrograms.openCount(snapshot, CATALOG));
        // VAB level 1 holds four rocket programs.
        assertEquals(RocketProgramOutcome.VAB_FULL, RocketPrograms.open(snapshot, "spare-rocket", CATALOG));
    }

    @Test
    void openingReliabilityTakesTheTransferFromRelatedRocketsAsTheyAreNow() {
        MissionComponent heavy = new MissionComponent("heavy-rocket", SetSkill.ROCKETS, 3, 90, 80,
            List.of(1.0, 2.0, 3.0, 4.0), 30, List.of(new MissionComponent.ReliabilityTransferSource("light-rocket", 50)),
            250, 5_000, 3_000, 0, null, new MissionComponent.RocketSpec(RocketClass.LIGHT, 4_000, 1_000, 0, 0));
        PlayerSnapshot snapshot = soviet(10_000);
        assertEquals(3, RocketPrograms.openingReliability(snapshot, heavy), "nothing related owned: the baseline");

        RocketPrograms.open(snapshot, "light-rocket", CATALOG);
        snapshot.getMissionComponents().get("light-rocket").setCurrentReliability(40);
        assertEquals(23, RocketPrograms.openingReliability(snapshot, heavy), 1e-9, "half of 40 on top of 3");

        snapshot.getMissionComponents().get("light-rocket").setCurrentReliability(80);
        assertEquals(33, RocketPrograms.openingReliability(snapshot, heavy), 1e-9, "the transfer is capped at 30");

        // A rocket owned from before keeps its own reliability.
        assertEquals(80, RocketPrograms.openingReliability(snapshot, LIGHT), 1e-9);
    }

    @Test
    void reopeningAClosedRocketChargesAgainAndKeepsReliability() {
        PlayerSnapshot snapshot = soviet(10_000);
        RocketPrograms.open(snapshot, "light-rocket", CATALOG);
        snapshot.getMissionComponents().get("light-rocket").setCurrentReliability(40);

        assertEquals(RocketProgramOutcome.CLOSED, RocketPrograms.close(snapshot, "light-rocket", CATALOG));
        assertFalse(snapshot.getMissionComponents().get("light-rocket").isActive());
        long before = snapshot.getFunds();

        assertEquals(RocketProgramOutcome.OPENED, RocketPrograms.open(snapshot, "light-rocket", CATALOG));
        assertEquals(before - 2_500, snapshot.getFunds(), "BASPM charges the creation cost on every opening");
        assertEquals(40, snapshot.getMissionComponents().get("light-rocket").getCurrentReliability());
    }

    @Test
    void launchNeedsOneOwnedActiveRocketPerLaunch() {
        PlayerSnapshot snapshot = soviet(10_000);

        assertEquals(MissionLaunchOutcome.WRONG_ROCKET_COUNT,
            MissionLaunch.launch(snapshot, "orbit", List.of(), List.of(), CATALOG));
        assertEquals(MissionLaunchOutcome.ROCKET_NOT_AVAILABLE,
            MissionLaunch.launch(snapshot, orbitWith("orbit", "light-rocket"), CATALOG));
        RocketPrograms.open(snapshot, "light-rocket", CATALOG);
        RocketPrograms.close(snapshot, "light-rocket", CATALOG);
        assertEquals(MissionLaunchOutcome.ROCKET_NOT_AVAILABLE,
            MissionLaunch.launch(snapshot, orbitWith("orbit", "light-rocket"), CATALOG), "inactive rocket");
        assertEquals(MissionLaunchOutcome.ROCKET_NOT_AVAILABLE,
            MissionLaunch.launch(snapshot, orbitWith("orbit", "satellite"), CATALOG), "not a rocket");
        assertTrue(snapshot.getInProgressMissions().isEmpty());
    }

    @Test
    void rocketMustLiftThePayloadAndBeHumanRatedForACrewedLaunch() {
        PlayerSnapshot snapshot = soviet(100_000);
        RocketPrograms.open(snapshot, "weak-rocket", CATALOG);
        RocketPrograms.open(snapshot, "light-rocket", CATALOG);
        RocketPrograms.open(snapshot, "hr-rocket", CATALOG);

        // 50 to LEO can't lift the satellite's 84.
        assertEquals(MissionLaunchOutcome.ROCKET_CANNOT_LIFT_PAYLOAD,
            MissionLaunch.launch(snapshot, orbitWith("orbit", "weak-rocket"), CATALOG));
        // A plain rocket can't fly a human-rated launch type, however strong.
        assertEquals(MissionLaunchOutcome.ROCKET_CANNOT_LIFT_PAYLOAD,
            MissionLaunch.launch(snapshot, orbitWith("orbit-hr", "light-rocket"), CATALOG));
        assertEquals(MissionLaunchOutcome.LAUNCHED,
            MissionLaunch.launch(snapshot, orbitWith("orbit-hr", "hr-rocket"), CATALOG));
    }

    @Test
    void launchCostsTheConfigurationPlusTheRocketsUnitCost() {
        PlayerSnapshot snapshot = soviet(10_000);
        RocketPrograms.open(snapshot, "light-rocket", CATALOG);
        long before = snapshot.getFunds();

        assertEquals(MissionLaunchOutcome.LAUNCHED,
            MissionLaunch.launch(snapshot, orbitWith("orbit", "light-rocket"), CATALOG));

        assertEquals(before - 1_625, snapshot.getFunds());
        InProgressMission mission = snapshot.getInProgressMissions().values().iterator().next();
        assertEquals(List.of("light-rocket"), mission.getRocketIds());
        assertEquals(RocketProgramOutcome.MISSIONS_IN_FLIGHT,
            RocketPrograms.close(snapshot, "light-rocket", CATALOG));
    }

    @Test
    void theRocketSlotFliesTheChosenRocket() {
        PlayerSnapshot snapshot = soviet(100_000);
        RocketPrograms.open(snapshot, "light-rocket", CATALOG);
        RocketPrograms.open(snapshot, "hr-rocket", CATALOG);
        double untouched = snapshot.getMissionComponents().get("hr-rocket").getCurrentReliability();
        MissionLaunch.launch(snapshot, orbitWith("orbit", "light-rocket"), CATALOG);

        List<ResolutionEvent> events = MissionProgress.advanceSeason(snapshot, CATALOG, new Random(3),
            new GoalTally(), 1);

        List<String> changed = new ArrayList<>();
        for (ResolutionEvent event : events) {
            if (event instanceof MissionReliabilityChanged reliability) {
                changed.add(reliability.componentId());
            }
        }
        assertTrue(changed.contains("light-rocket"), "the chosen rocket's reliability moves: " + changed);
        assertFalse(changed.contains(MissionConfiguration.rocketSlotId(1)));
        assertEquals(untouched, snapshot.getMissionComponents().get("hr-rocket").getCurrentReliability());
    }

    @Test
    void catalogRejectsAProgramGrantingARocketAndAnOutOfRangeSlot() {
        Program grantsRocket = new Program("bad", "sats", 1, 0, List.of("light-rocket"), List.of());
        assertThrows(IllegalArgumentException.class, () -> new ContentCatalog(List.of(CATEGORY),
            List.of(grantsRocket), List.of(), List.of(LIGHT)));

        MissionConfiguration twoSlots = new MissionConfiguration("bad-slot", "sat-program",
            List.of(new MissionSeason(List.of(new MissionStep("launch",
                List.of(new MissionStep.ComponentInvolvement(MissionConfiguration.rocketSlotId(2), 100)),
                List.of(), List.of(), false)))),
            0, 0, 0, 0, List.of(), List.of(),
            List.of(new MissionConfiguration.Launch(LaunchType.LEO, List.of("satellite"))));
        assertThrows(IllegalArgumentException.class, () -> new ContentCatalog(List.of(CATEGORY), List.of(PROGRAM),
            List.of(twoSlots), List.of(SATELLITE)));
    }
}
