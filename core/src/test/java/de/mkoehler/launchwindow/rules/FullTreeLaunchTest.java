package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.Goal;
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
import de.mkoehler.launchwindow.model.InProgressMission;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSkill;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The launch rules BASPM's full tree brings (2026-10-08, {@code design.md} &sect;5, M3): a payload
 * that carries part of its own lift, a launch dropped from an aircraft with no rocket, and a
 * configuration gated on a goal.
 *
 * @author Mario Koehler
 */
class FullTreeLaunchTest {

    private static final List<Double> BANDS = List.of(1.0, 2.0, 3.0, 4.0);
    private static final ProgramCategory CATEGORY = new ProgramCategory("planes", "Planes");
    /** 500 kg, but it lifts 400 of its own to orbit: a rocket only needs to lift 100 of it. */
    private static final MissionComponent STAGE = new MissionComponent("stage", SetSkill.SPACE_PROBES, 3, 90, 80,
        BANDS, 0, List.of(), 0, 0, 100, 500, null, null, new MissionComponent.Lift(0, 400, 0, 0));
    private static final MissionComponent CRAFT = new MissionComponent("craft", SetSkill.CREWED_SPACECRAFT, 3, 90,
        80, BANDS, 0, List.of(), 0, 0, 200, 300, null, null);
    private static final MissionComponent ROCKET = new MissionComponent("rocket", SetSkill.ROCKETS, 3, 90, 80, BANDS,
        0, List.of(), 0, 1_000, 700, 0, null, new MissionComponent.RocketSpec(RocketClass.LIGHT, 1_000, 450, 0, 0));
    private static final Program PROGRAM = new Program("plane", "planes", 100, 0, List.of("stage", "craft"),
        List.of());

    private static MissionStep step(String id, String component, List<String> goals) {
        return new MissionStep(id, List.of(new MissionStep.ComponentInvolvement(component, 100)), List.of(), List.of(),
            false, 100, 0, goals);
    }

    /** Stage and craft to orbit on one rocket. */
    private static final MissionConfiguration ORBIT = new MissionConfiguration("orbit", "plane",
        List.of(new MissionSeason(List.of(step("ascent", MissionConfiguration.rocketSlotId(1), List.of())))),
        List.of(), List.of(), 300, 0, List.of(), List.of(),
        List.of(new MissionConfiguration.Launch(LaunchType.LEO, List.of("stage", "craft"))), 0, 0);
    /** Dropped from a carrier aircraft: no rocket; it achieves the goal the next flight needs. */
    private static final MissionConfiguration DROP = new MissionConfiguration("drop", "plane",
        List.of(new MissionSeason(List.of(step("glide", "craft", List.of("drop-flown"))))),
        List.of(), List.of(), 200, 0, List.of(), List.of(),
        List.of(new MissionConfiguration.Launch(LaunchType.AIR_DROP, List.of("craft"))), 0, 0);
    /** Another air drop, only once the first has flown. */
    private static final MissionConfiguration RESEARCH = new MissionConfiguration("research", "plane",
        List.of(new MissionSeason(List.of(step("glide", "craft", List.of())))),
        List.of(), List.of(), 200, 0, List.of(), List.of(),
        List.of(new MissionConfiguration.Launch(LaunchType.AIR_DROP, List.of("craft"))), 0, 0, null,
        List.of("drop-flown"));
    private static final ContentCatalog CATALOG = new ContentCatalog(List.of(CATEGORY), List.of(PROGRAM),
        List.of(ORBIT, DROP, RESEARCH), List.of(STAGE, CRAFT, ROCKET),
        List.of(new Goal("drop-flown", 100, 50, List.of())), TestBuildings.withoutMaintenance());

    private static PlayerSnapshot player() {
        PlayerSnapshot snapshot = new PlayerSnapshot("Agency", Archetype.SOVIET, 100_000, 0, 0, 1);
        ProgramOpening.open(snapshot, "plane", CATALOG);
        RocketPrograms.open(snapshot, "rocket", CATALOG);
        return snapshot;
    }

    private static LaunchRequest request(String configurationId, String rocketId) {
        return new LaunchRequest(configurationId, List.of(), List.of(), List.of(), List.of(rocketId));
    }

    @Test
    void aPayloadsOwnLiftComesOffWhatTheRocketMustLift() {
        MissionConfiguration.Launch launch = ORBIT.launches().get(0);
        assertEquals(800, MissionLaunch.payloadMass(launch, CATALOG));
        assertEquals(400, MissionLaunch.requiredLift(launch, CATALOG), "800 kg less the stage's 400");
        assertTrue(MissionLaunch.canLift(ROCKET, launch, CATALOG), "450 to orbit covers the 400 left");
        assertEquals(MissionLaunchOutcome.LAUNCHED, MissionLaunch.launch(player(), request("orbit", "rocket"),
            CATALOG));
    }

    @Test
    void anAirDropFliesWithoutARocketAndPaysForNone() {
        PlayerSnapshot snapshot = player();
        long before = snapshot.getFunds();

        assertFalse(MissionLaunch.canLift(ROCKET, DROP.launches().get(0), CATALOG));
        assertEquals(MissionLaunchOutcome.ROCKET_CANNOT_LIFT_PAYLOAD,
            MissionLaunch.launch(snapshot, request("drop", "rocket"), CATALOG), "no rocket on an air drop");
        assertEquals(MissionLaunchOutcome.LAUNCHED, MissionLaunch.launch(snapshot, request("drop", ""), CATALOG));

        InProgressMission mission = snapshot.getInProgressMissions().values().iterator().next();
        assertEquals(200, mission.getLaunchCost(), "the configuration's cost, no rocket's");
        assertEquals(before - 200, snapshot.getFunds());
        assertEquals(List.of(""), mission.getRocketIds());
    }

    @Test
    void anAirDropFliesItsSeasonWithTheEmptyRocketSlot() {
        PlayerSnapshot snapshot = player();
        snapshot.getMissionComponents().get("craft").setCurrentReliability(90);
        assertEquals(MissionLaunchOutcome.LAUNCHED, MissionLaunch.launch(snapshot, request("drop", ""), CATALOG));

        List<ResolutionEvent> events = MissionProgress.advanceSeason(snapshot, CATALOG, new java.util.Random(3),
            new GoalTally(), 1);

        assertTrue(snapshot.getInProgressMissions().isEmpty(), "the one-season drop is over: " + events);
        assertTrue(events.stream().noneMatch(event -> event instanceof MissionReliabilityChanged changed
            && changed.componentId().isEmpty()), "no reliability for the missing rocket");
    }

    @Test
    void aGatedConfigurationWaitsForItsGoal() {
        PlayerSnapshot snapshot = player();
        assertEquals(MissionLaunchOutcome.GOAL_NOT_REACHED,
            MissionLaunch.launch(snapshot, request("research", ""), CATALOG));
        assertTrue(snapshot.getInProgressMissions().isEmpty());

        snapshot.getAchievedGoals().put("drop-flown", 1);
        assertEquals(MissionLaunchOutcome.LAUNCHED, MissionLaunch.launch(snapshot, request("research", ""), CATALOG));
    }

    @Test
    void theCatalogRejectsARocketSlotOnAnAirDropAndAnUnknownGate() {
        MissionConfiguration slotOnDrop = new MissionConfiguration("bad", "plane",
            List.of(new MissionSeason(List.of(step("ascent", MissionConfiguration.rocketSlotId(1), List.of())))),
            List.of(), List.of(), 0, 0, List.of(), List.of(),
            List.of(new MissionConfiguration.Launch(LaunchType.AIR_DROP, List.of("craft"))), 0, 0);
        assertThrows(IllegalArgumentException.class, () -> new ContentCatalog(List.of(CATEGORY), List.of(PROGRAM),
            List.of(slotOnDrop), List.of(STAGE, CRAFT, ROCKET)));

        MissionConfiguration unknownGate = new MissionConfiguration("bad", "plane", DROP.seasons(), List.of(),
            List.of(), 0, 0, List.of(), List.of(), DROP.launches(), 0, 0, null, List.of("no-such-goal"));
        assertThrows(IllegalArgumentException.class, () -> new ContentCatalog(List.of(CATEGORY), List.of(PROGRAM),
            List.of(unknownGate), List.of(STAGE, CRAFT, ROCKET), List.of(new Goal("drop-flown", 100, 50, List.of()))));
    }

    @Test
    void onlyAPayloadCarriesOwnLift() {
        assertThrows(IllegalArgumentException.class, () -> new MissionComponent("bad", SetSkill.ROCKETS, 3, 90, 80,
            BANDS, 0, List.of(), 0, 0, 0, 0, null, new MissionComponent.RocketSpec(RocketClass.LIGHT, 1, 1, 0, 0),
            new MissionComponent.Lift(0, 1, 0, 0)));
    }
}
