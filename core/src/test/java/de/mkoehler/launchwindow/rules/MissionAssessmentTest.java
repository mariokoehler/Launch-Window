package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.ContentPackLoader;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.AstronautSkill;
import de.mkoehler.launchwindow.model.MissionControlOfficer;
import de.mkoehler.launchwindow.model.MissionControlSkill;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the mission assessment against the real early-era pack: the odds add up, match the rolls
 * the season pass makes, and follow the goal penalties.
 *
 * @author Mario Koehler
 */
class MissionAssessmentTest {

    private static final ContentCatalog PACK = ContentPackLoader.load(Path.of("..", "assets", "content", "early-era"));

    /**
     * A Soviet agency with Vostok open, its capsule and the R-7 Vostok owned, both uncrewed Vostok
     * flights flown, two cosmonauts and ten controllers.
     */
    private static PlayerSnapshot vostokAgency() {
        PlayerSnapshot state = new PlayerSnapshot("Vega", Archetype.SOVIET, 50_000, 0, 0, 1);
        state.getOpenedProgramIds().add("vostok");
        state.getMissionComponents().put("vostok-capsule", new OwnedMissionComponent(80.6, 8.7));
        state.getMissionComponents().put("r7-vostok", new OwnedMissionComponent(84.3, 3.0));
        state.getAchievedGoals().put("vostok-uncrewed-suborbital-flown", 3);
        state.getAchievedGoals().put("vostok-uncrewed-orbital-flown", 4);
        for (int i = 0; i < 2; i++) {
            Astronaut astronaut = new Astronaut("a" + i, "Cosmonaut " + i, 30, 70, 60);
            for (AstronautSkill skill : AstronautSkill.values()) {
                astronaut.setSkill(skill, 55.0 + 10 * i);
            }
            astronaut.setAssignmentState(AssignmentState.IDLE);
            state.getAstronauts().add(astronaut);
        }
        for (int i = 0; i < 10; i++) {
            MissionControlOfficer officer = new MissionControlOfficer("c" + i, "Controller " + i, 35, 70, 60);
            for (MissionControlSkill skill : MissionControlSkill.values()) {
                officer.setSkill(skill, 50.0 + i * 3);
            }
            officer.setAssignmentState(AssignmentState.IDLE);
            state.getMissionControlOfficers().add(officer);
        }
        return state;
    }

    private static MissionAssessment.Assessment assessOrbital(PlayerSnapshot state) {
        MissionConfiguration orbital = PACK.missionConfiguration("vostok-orbital").orElseThrow();
        MissionStaffing.Staffing staffing = MissionStaffing.autoAssign(state, orbital);
        return MissionAssessment.assess(state, PACK, orbital, staffing.crewSlotIds(), staffing.controlSlotIds(),
            List.of("r7-vostok"));
    }

    /**
     * Each step's five outcomes add up to 1, and the flight's odds are probabilities.
     */
    @Test
    void oddsAddUp() {
        MissionAssessment.Assessment assessment = assessOrbital(vostokAgency());
        assertEquals(11, assessment.steps().size());
        for (MissionAssessment.StepOdds odds : assessment.steps()) {
            assertEquals(1.0, odds.success() + odds.solved() + odds.glitch() + odds.failure() + odds.catastrophic(),
                1e-9, odds.step().id());
        }
        assertTrue(assessment.completion() > 0 && assessment.completion() < 1);
        assertTrue(assessment.crewLoss() > 0 && assessment.crewLoss() < 1 - assessment.completion());
    }

    /**
     * Each step's odds match the season pass's own rolls: a seeded simulation of
     * {@link MissionStepResolution#resolve} lands within a fraction of a percent.
     */
    @Test
    void oddsMatchTheRolls() {
        MissionAssessment.Assessment assessment = assessOrbital(vostokAgency());
        SplittableRandom random = new SplittableRandom(42);
        int trials = 200_000;
        for (MissionAssessment.StepOdds odds : assessment.steps()) {
            double base = odds.success() * 100;
            double saving = odds.success() >= 1 ? 0 : odds.solved() / (1 - odds.success()) * 100;
            Map<MissionStepOutcome, Integer> counts = new EnumMap<>(MissionStepOutcome.class);
            for (int i = 0; i < trials; i++) {
                counts.merge(MissionStepResolution.resolve(base, saving, odds.step().crewRisk(), random), 1,
                    Integer::sum);
            }
            assertEquals(odds.success(), share(counts, MissionStepOutcome.SUCCESSFUL, trials), 0.005);
            assertEquals(odds.solved(), share(counts, MissionStepOutcome.MINOR_GLITCH_SOLVED, trials), 0.005);
            assertEquals(odds.glitch(), share(counts, MissionStepOutcome.MINOR_GLITCH, trials), 0.005);
            assertEquals(odds.failure(), share(counts, MissionStepOutcome.FAILURE, trials), 0.005);
            assertEquals(odds.catastrophic(), share(counts, MissionStepOutcome.CATASTROPHIC, trials), 0.005);
        }
    }

    private static double share(Map<MissionStepOutcome, Integer> counts, MissionStepOutcome outcome, int trials) {
        return counts.getOrDefault(outcome, 0) / (double) trials;
    }

    /**
     * "Human in space" not reached costs the capsule 20 points: holding it raises every capsule
     * step's hardware by 20 times the capsule's share, and the odds of completing.
     */
    @Test
    void openGoalPenaltyLowersTheOdds() {
        PlayerSnapshot without = vostokAgency();
        PlayerSnapshot with = vostokAgency();
        with.getCancelledGoalIds().add("human-in-space");
        MissionAssessment.Assessment penalised = assessOrbital(without);
        MissionAssessment.Assessment lifted = assessOrbital(with);
        // Step 6, orbital flight, rests on the capsule alone.
        assertEquals(20.0, lifted.steps().get(5).hardware() - penalised.steps().get(5).hardware(), 1e-9);
        assertTrue(lifted.completion() > penalised.completion());
    }

    /**
     * An empty slot reads 0, so an unstaffed flight is worse than a staffed one.
     */
    @Test
    void emptySlotsReadZero() {
        PlayerSnapshot state = vostokAgency();
        MissionConfiguration orbital = PACK.missionConfiguration("vostok-orbital").orElseThrow();
        MissionAssessment.Assessment empty = MissionAssessment.assess(state, PACK, orbital,
            java.util.Arrays.asList(new String[orbital.crewSlots().size()]),
            java.util.Arrays.asList(new String[orbital.controlSlots().size()]), List.of("r7-vostok"));
        assertTrue(empty.completion() < assessOrbital(state).completion());
    }

    /**
     * The launch check refuses exactly as the launch does, and changes nothing either way.
     */
    @Test
    void launchCheckMatchesTheLaunch() {
        PlayerSnapshot state = vostokAgency();
        MissionConfiguration orbital = PACK.missionConfiguration("vostok-orbital").orElseThrow();
        MissionStaffing.Staffing staffing = MissionStaffing.autoAssign(state, orbital);
        LaunchRequest sputnikRocket = new LaunchRequest(orbital.id(), staffing.crewSlotIds(),
            staffing.controlSlotIds(), List.of("r7-sputnik"));
        assertEquals(MissionLaunchOutcome.ROCKET_NOT_AVAILABLE, MissionLaunch.check(state, sputnikRocket, PACK));
        LaunchRequest good = new LaunchRequest(orbital.id(), staffing.crewSlotIds(), staffing.controlSlotIds(),
            List.of("r7-vostok"));
        assertEquals(MissionLaunchOutcome.LAUNCHED, MissionLaunch.check(state, good, PACK));
        assertEquals(50_000, state.getFunds(), "checking changes nothing");
        assertTrue(state.getInProgressMissions().isEmpty());
        assertEquals(MissionLaunchOutcome.LAUNCHED, MissionLaunch.launch(state, good, PACK));
        assertEquals(MissionLaunchOutcome.ASTRONAUT_NOT_IDLE, MissionLaunch.check(state, good, PACK));
    }
}
