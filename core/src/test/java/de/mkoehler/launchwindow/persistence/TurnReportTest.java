package de.mkoehler.launchwindow.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.AstronautSkill;
import de.mkoehler.launchwindow.model.Building;
import de.mkoehler.launchwindow.model.MissionControlOfficer;
import de.mkoehler.launchwindow.model.MissionControlSkill;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.RandomEventKind;
import de.mkoehler.launchwindow.model.TrainingKind;
import de.mkoehler.launchwindow.rules.BudgetProgress;
import de.mkoehler.launchwindow.rules.BudgetReviewed;
import de.mkoehler.launchwindow.rules.BuildingUpgraded;
import de.mkoehler.launchwindow.rules.DepartureReason;
import de.mkoehler.launchwindow.rules.ExpertRescueApplied;
import de.mkoehler.launchwindow.rules.ExpertTrainingCompleted;
import de.mkoehler.launchwindow.rules.GoalSettled;
import de.mkoehler.launchwindow.rules.LaunchRequest;
import de.mkoehler.launchwindow.rules.MissionLaunch;
import de.mkoehler.launchwindow.rules.MissionLaunchOutcome;
import de.mkoehler.launchwindow.rules.MissionPrestigeChanged;
import de.mkoehler.launchwindow.rules.MissionReliabilityChanged;
import de.mkoehler.launchwindow.rules.MissionSkillGrowthApplied;
import de.mkoehler.launchwindow.rules.MissionStaffing;
import de.mkoehler.launchwindow.rules.MissionStepOutcome;
import de.mkoehler.launchwindow.rules.MissionStepResolved;
import de.mkoehler.launchwindow.rules.PassengerLost;
import de.mkoehler.launchwindow.rules.PrDutyPerformed;
import de.mkoehler.launchwindow.rules.PersonnelDeparture;
import de.mkoehler.launchwindow.rules.PersonnelGroup;
import de.mkoehler.launchwindow.rules.RandomEventOccurred;
import de.mkoehler.launchwindow.rules.ResearchCompleted;
import de.mkoehler.launchwindow.rules.ResearchComponentMaxed;
import de.mkoehler.launchwindow.rules.ResolutionEvent;
import de.mkoehler.launchwindow.rules.SeasonFundsSettled;
import de.mkoehler.launchwindow.rules.TrainingCompleted;
import de.mkoehler.launchwindow.rules.TurnReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the turn report as a save format (gap G2): every event type survives the strict mapper,
 * a resolved turn writes its report with the enriched step events, and redoing a turn after a
 * crash writes the same report again.
 *
 * @author Mario Koehler
 */
class TurnReportTest {

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Every event type round-trips through JSON by its type id.
     */
    @Test
    void everyEventTypeRoundTrips() throws IOException {
        List<ResolutionEvent> events = List.of(
            new BudgetReviewed(2, 10_190, 0, 7_250),
            new BuildingUpgraded(Building.SET_CENTER, 2, 20),
            new ExpertRescueApplied("m1", "08-retrofire", PersonnelGroup.MISSION_CONTROL, "c1", "Sergei Markov",
                "PROPULSION", 50, 40.5, 70.25),
            new ExpertTrainingCompleted(PersonnelGroup.MISSION_CONTROL, "c1", "Sergei Markov", "PROPULSION"),
            new GoalSettled("m1", "human-in-orbit", true, 3_000, true),
            new MissionPrestigeChanged("m1", "vostok-orbital", 0, 1_250, 1_250),
            new MissionReliabilityChanged("m1", "vostok-capsule", 2.0),
            new MissionSkillGrowthApplied("m1", "a1", "Pavel Orlov", PersonnelGroup.ASTRONAUT, 4.5),
            new MissionStepResolved("m1", "vostok-orbital", 0, "08-retrofire", MissionStepOutcome.MINOR_GLITCH_SOLVED,
                List.of(new MissionStepResolved.ComponentFlown("vostok-capsule", 100, 60.6)), 61.0, false, 40.5, 70.25,
                true, 1_100),
            new MissionStepResolved("m1", "vostok-orbital", 0, "01-launch-preparations", MissionStepOutcome.SUCCESSFUL,
                List.of(), 72.4, true, null, null, null, 0),
            new PersonnelDeparture(PersonnelGroup.SET, "s9", "Nadezhda Orlova", DepartureReason.RETIRED, 65, 60,
                AssignmentState.IDLE, null),
            new PassengerLost("m2", "sputnik-2-orbit", "Laika", null),
            new PrDutyPerformed("a2", "Alan Shepard", 13, 13, null, 0.0),
            new PrDutyPerformed("a3", "Yuri Gagarin", 16, 24, "placeholder-good-1", 1.0),
            new RandomEventOccurred(RandomEventKind.values()[0], "a1", 1, 7),
            new ResearchCompleted("s1", "Irina Petrova", "vostok-capsule", 0.4, 0.7, 0),
            new ResearchComponentMaxed("r7-sputnik", List.of("s2"), List.of("Pyotr Morozov")),
            new SeasonFundsSettled(4_090, 6_500, 14_820,
                new BudgetProgress.RunningCosts(2_310, 28, 193, 3, 997, 4, 590, 4_090)),
            new TrainingCompleted(PersonnelGroup.ASTRONAUT, "a6", "Viktor Dorofeev", TrainingKind.ADVANCED, "FITNESS",
                3.1));
        TurnReport report = new TurnReport(TurnReport.VERSION, 23, 1_940, 2_140, 12_410, 14_820,
            List.of(new TurnReport.ComponentChange("vostok-capsule", 77.8, 79.0)),
            List.of(new TurnReport.MissionFlown("m1", "vostok-orbital", 0, 1, true, List.of("Pavel Orlov"))), events)
            .withNewsStory("1960-summer-echo");
        String json = mapper.writeValueAsString(report);
        TurnReport copy = mapper.readValue(json, TurnReport.class);
        assertEquals(report, copy);
        assertTrue(json.contains("\"type\":\"missionStepResolved\""), json);
        assertTrue(json.contains("\"type\":\"passengerLost\""), json);
        // A report saved before the story field existed still loads, without a story.
        TurnReport older = mapper.readValue(json.replace(",\"newsStoryId\":\"1960-summer-echo\"", ""),
            TurnReport.class);
        assertEquals(null, older.newsStoryId());
        assertEquals(report.events(), older.events());
    }

    /**
     * A resolved turn with a mission flying writes its report: the mission, its step events with
     * their chances and component reliabilities, and the before and after numbers. Redoing the
     * turn after a crash before the snapshot writes the same report.
     */
    @Test
    void resolvedTurnWritesItsReport(@TempDir Path playerRoot) throws IOException {
        PlayerSnapshot state = vostokAgency();
        MissionConfiguration orbital = TestContexts.EARLY_ERA.catalog().missionConfiguration("vostok-orbital")
            .orElseThrow();
        MissionStaffing.Staffing staffing = MissionStaffing.autoAssign(state, orbital);
        assertEquals(MissionLaunchOutcome.LAUNCHED, MissionLaunch.launch(state, new LaunchRequest(orbital.id(),
            staffing.crewSlotIds(), staffing.controlSlotIds(), List.of("r7-vostok")), TestContexts.EARLY_ERA.catalog()));
        new PlayerSnapshotStore(playerRoot, mapper).save(0, state, null);
        PlayerStateService service = new PlayerStateService(playerRoot, mapper, TestContexts.EARLY_ERA);

        TestContexts.resolveQuietly(service, 1);

        TurnReport report = service.turnReport(1).orElseThrow();
        assertEquals(report, service.turnReport(-1).orElseThrow(), "the latest is turn 1");
        assertEquals(1, report.missions().size());
        assertEquals("vostok-orbital", report.missions().get(0).missionConfigurationId());
        assertEquals(List.of("Pavel Orlov"), report.missions().get(0).crewNames());
        assertEquals(state.getFunds(), report.fundsBefore());
        List<MissionStepResolved> steps = report.events().stream().filter(MissionStepResolved.class::isInstance)
            .map(MissionStepResolved.class::cast).toList();
        assertFalse(steps.isEmpty());
        MissionStepResolved first = steps.get(0);
        assertTrue(first.successChance() > 0 && first.successChance() <= 100);
        assertFalse(first.components().isEmpty());
        assertEquals(first.outcome() == MissionStepOutcome.SUCCESSFUL, first.firstRollPassed());
        assertTrue(report.events().stream().anyMatch(SeasonFundsSettled.class::isInstance));

        // A crash after the report but before the snapshot: the turn is redone and writes the same report.
        String written = Files.readString(playerRoot.resolve("reports").resolve("1.json"));
        Files.delete(playerRoot.resolve("snapshots").resolve("1.json"));
        new PlayerStateService(playerRoot, mapper, TestContexts.EARLY_ERA);
        TestContexts.resolveQuietly(new PlayerStateService(playerRoot, mapper, TestContexts.EARLY_ERA), 1);
        assertEquals(written, Files.readString(playerRoot.resolve("reports").resolve("1.json")));
    }

    /**
     * No report yet reads as empty, not as an error.
     */
    @Test
    void noReportIsEmpty(@TempDir Path playerRoot) throws IOException {
        new PlayerSnapshotStore(playerRoot, mapper).save(0, new PlayerSnapshot("Vega", Archetype.SOVIET, 1_000, 0, 0,
            1), null);
        PlayerStateService service = new PlayerStateService(playerRoot, mapper, TestContexts.EARLY_ERA);
        assertTrue(service.turnReport(-1).isEmpty());
        assertTrue(service.turnReport(5).isEmpty());
    }

    private static PlayerSnapshot vostokAgency() {
        PlayerSnapshot state = new PlayerSnapshot("Vega", Archetype.SOVIET, 50_000, 0, 0, 1);
        state.getOpenedProgramIds().add("vostok");
        state.getMissionComponents().put("vostok-capsule", new OwnedMissionComponent(80.6, 8.7));
        state.getMissionComponents().put("r7-vostok", new OwnedMissionComponent(84.3, 3.0));
        Astronaut astronaut = new Astronaut("a1", "Pavel Orlov", 30, 70, 60);
        for (AstronautSkill skill : AstronautSkill.values()) {
            astronaut.setSkill(skill, 60.0);
        }
        astronaut.setAssignmentState(AssignmentState.IDLE);
        state.getAstronauts().add(astronaut);
        Astronaut capcom = new Astronaut("a2", "Grigori Zaitsev", 34, 70, 60);
        for (AstronautSkill skill : AstronautSkill.values()) {
            capcom.setSkill(skill, 55.0);
        }
        capcom.setAssignmentState(AssignmentState.IDLE);
        state.getAstronauts().add(capcom);
        for (int i = 0; i < 9; i++) {
            MissionControlOfficer officer = new MissionControlOfficer("c" + i, "Controller " + i, 35, 70, 60);
            for (MissionControlSkill skill : MissionControlSkill.values()) {
                officer.setSkill(skill, 60.0);
            }
            officer.setAssignmentState(AssignmentState.IDLE);
            state.getMissionControlOfficers().add(officer);
        }
        return state;
    }
}
