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
import de.mkoehler.launchwindow.model.CareerEntry;
import de.mkoehler.launchwindow.model.InProgressMission;
import de.mkoehler.launchwindow.model.Incident;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSkill;
import de.mkoehler.launchwindow.model.SetSpecialist;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link Investigations} ({@code design.md} &sect;2.14): a failure offers the agency an
 * investigation, a lead's season and the fee buy a chance at part of the loss, an unanswered offer lapses,
 * and a lost crew's board reports exactly when crewed flights may fly again.
 *
 * @author Mario Koehler
 */
class InvestigationsTest {

    private static final int TURN = 10;
    private static final MissionComponent CAPSULE = new MissionComponent("capsule-mercury", SetSkill.CREWED_SPACECRAFT,
        3, 90, 82, List.of(3.0, 6.0, 9.0, 12.0), 0, List.of());
    private static final Program PROGRAM =
        new Program("mercury-program", "early-space", 1_000, 0, List.of("capsule-mercury"), List.of());
    private static final ProgramCategory CATEGORY = new ProgramCategory("early-space", "Early Space");
    private static final MissionConfiguration UNCREWED = new MissionConfiguration("probe-std", "mercury-program",
        List.of(new MissionSeason(List.of(new MissionStep("03-launch",
            List.of(new MissionStep.ComponentInvolvement("capsule-mercury", 100)), List.of(), List.of(), false)))),
        0, 0, 0);
    private static final MissionConfiguration CREWED = new MissionConfiguration("crewed-std", "mercury-program",
        List.of(new MissionSeason(List.of(new MissionStep("12-reentry",
            List.of(new MissionStep.ComponentInvolvement("capsule-mercury", 100)), List.of(),
            List.of(new MissionStep.CrewInvolvement(0, AstronautSkill.PILOTING, 100)), true)))),
        1, 0, 0);
    private static final ContentCatalog CATALOG = new ContentCatalog(List.of(CATEGORY), List.of(PROGRAM),
        List.of(UNCREWED, CREWED), List.of(CAPSULE));

    private static PlayerSnapshot player(double reliability) {
        PlayerSnapshot snapshot = new PlayerSnapshot("Agency", Archetype.NASA, 10_000, 0, 0, 1);
        snapshot.setOpenedProgramIds(List.of("mercury-program"));
        Map<String, OwnedMissionComponent> owned = new LinkedHashMap<>();
        owned.put("capsule-mercury", new OwnedMissionComponent(reliability, 3));
        snapshot.setMissionComponents(owned);
        return snapshot;
    }

    private static SetSpecialist engineer(PlayerSnapshot snapshot, String id, double skill) {
        SetSpecialist engineer = new SetSpecialist(id, "Engineer " + id, 35, 60, 50);
        for (SetSkill each : SetSkill.values()) {
            engineer.setSkill(each, 20);
        }
        engineer.setSkill(SetSkill.CREWED_SPACECRAFT, skill);
        List<SetSpecialist> roster = new ArrayList<>(snapshot.getSetSpecialists());
        roster.add(engineer);
        snapshot.setSetSpecialists(roster);
        return engineer;
    }

    /** Fails the uncrewed flight: a first roll of 0, a saving roll in the plain-failure band, factor 0.2. */
    private static List<ResolutionEvent> failUncrewed(PlayerSnapshot snapshot) {
        snapshot.getInProgressMissions().put("m0", new InProgressMission("m0", "probe-std", List.of(), List.of()));
        return MissionProgress.advanceSeason(snapshot, CATALOG, new Scripted(0.0, 30.0, 0.2), new GoalTally(), TURN);
    }

    @Test
    void aFailureOffersAnInvestigationOfWhatItLost() {
        PlayerSnapshot snapshot = player(50);
        List<ResolutionEvent> events = failUncrewed(snapshot);

        Incident incident = snapshot.getIncidents().getFirst();
        assertEquals(Incident.Kind.AGENCY, incident.kind());
        assertEquals("03-launch", incident.stepId());
        assertEquals("capsule-mercury", incident.culpritComponentId());
        assertEquals((50 - 3) * 0.2, incident.totalLoss(), 1e-9);
        InvestigationOffered offer = events.stream().filter(InvestigationOffered.class::isInstance)
            .map(InvestigationOffered.class::cast).findFirst().orElseThrow();
        assertEquals(TURN + Investigations.OFFER_TURNS, offer.lastTurn());
    }

    @Test
    void aFailureThatCostNothingOffersNothing() {
        PlayerSnapshot snapshot = player(3);
        failUncrewed(snapshot);
        assertTrue(snapshot.getIncidents().isEmpty());
    }

    @Test
    void startingTakesTheLeadOffResearchAndWithdrawingPutsThemBack() {
        PlayerSnapshot snapshot = player(50);
        failUncrewed(snapshot);
        SetSpecialist lead = engineer(snapshot, "e1", 80);
        assertEquals(PersonnelActions.ResearchOutcome.ASSIGNED,
            PersonnelActions.assignResearch(snapshot, "e1", "capsule-mercury", CATALOG));
        String incidentId = snapshot.getIncidents().getFirst().id();

        assertEquals(Investigations.Outcome.STARTED, Investigations.start(snapshot, CATALOG, incidentId, "e1", 42));
        Incident started = snapshot.getIncidents().getFirst();
        assertEquals(AssignmentState.INVESTIGATING, lead.getAssignmentState());
        assertEquals(10_000 - Investigations.FEE, snapshot.getFunds());
        assertEquals("capsule-mercury", started.returnComponentId());
        double match = Investigations.match(lead, CATALOG, started);
        assertEquals(Investigations.chance(match), started.chance(), 1e-9);
        assertEquals(Investigations.share(match), started.share(), 1e-9);
        assertEquals(Investigations.Outcome.ALREADY_STARTED,
            Investigations.start(snapshot, CATALOG, incidentId, "e1", 42));

        assertEquals(Investigations.Outcome.WITHDRAWN, Investigations.withdraw(snapshot, CATALOG, incidentId));
        assertEquals(10_000, snapshot.getFunds());
        assertEquals(AssignmentState.ASSIGNED, lead.getAssignmentState());
        assertEquals("capsule-mercury", lead.getRdTargetComponentId());
        assertFalse(snapshot.getIncidents().getFirst().started());
    }

    /** The user (2026-11-14): everyone who could lead is in the dialog, a trainee too, pulled out to lead. */
    @Test
    void aTraineePastBasicCanLeadAndIsPulledOut() {
        PlayerSnapshot snapshot = player(50);
        failUncrewed(snapshot);
        SetSpecialist trainee = engineer(snapshot, "e1", 70);
        trainee.setAssignmentState(AssignmentState.IN_TRAINING);
        trainee.setTrainingKind(de.mkoehler.launchwindow.model.TrainingKind.ADVANCED);
        trainee.setTrainingTargetSkill(SetSkill.CREWED_SPACECRAFT);
        trainee.setTrainingSeasonsTotal(3);
        trainee.setTrainingSeasonsCompleted(2);
        SetSpecialist rookie = engineer(snapshot, "e2", 90);
        rookie.setAssignmentState(AssignmentState.IN_TRAINING);
        rookie.setTrainingKind(de.mkoehler.launchwindow.model.TrainingKind.BASIC);
        rookie.setTrainingSeasonsTotal(2);
        Incident incident = snapshot.getIncidents().getFirst();

        assertEquals(List.of(trainee), Investigations.candidates(snapshot, CATALOG, incident), "basic can't be cut short");
        Investigations.Prospect prospect = Investigations.prospect(snapshot, CATALOG, incident, trainee);
        assertEquals(SkillGrowth.partialCreditShare(2, 3), prospect.kept(), 1e-9);
        assertEquals(Investigations.Outcome.LEAD_NOT_FREE,
            Investigations.start(snapshot, CATALOG, incident.id(), "e2", 3));
        double before = trainee.getSkill(SetSkill.CREWED_SPACECRAFT);

        assertEquals(Investigations.Outcome.STARTED, Investigations.start(snapshot, CATALOG, incident.id(), "e1", 3));
        assertEquals(AssignmentState.INVESTIGATING, trainee.getAssignmentState());
        assertEquals(de.mkoehler.launchwindow.model.TrainingKind.NONE, trainee.getTrainingKind());
        assertTrue(trainee.getSkill(SetSkill.CREWED_SPACECRAFT) >= before, "keeps a share of the round");

        Investigations.withdraw(snapshot, CATALOG, incident.id());
        assertEquals(AssignmentState.IDLE, trainee.getAssignmentState(), "a pull-out ends the round");
    }

    @Test
    void theOddsAndTheShareFollowTheMatch() {
        assertEquals(0.40, Investigations.chance(0), 1e-9);
        assertEquals(0.65, Investigations.chance(50), 1e-9);
        assertEquals(0.90, Investigations.chance(100), 1e-9);
        assertEquals(0.0, Investigations.share(0), 1e-9);
        assertEquals(0.6, Investigations.share(90), 1e-9);
        assertEquals(2.0 / 3.0, Investigations.share(100), 1e-9);
    }

    /** Many seeds, so both outcomes come up: a found cause gives back the share, an undetermined one nothing. */
    @Test
    void aSettledInvestigationGivesBackItsShareOrNothing() {
        int found = 0;
        int undetermined = 0;
        for (long seed = 1; seed <= 40; seed++) {
            PlayerSnapshot snapshot = player(50);
            failUncrewed(snapshot);
            SetSpecialist lead = engineer(snapshot, "e1", 70);
            PersonnelActions.assignResearch(snapshot, "e1", "capsule-mercury", CATALOG);
            Incident offered = snapshot.getIncidents().getFirst();
            Investigations.start(snapshot, CATALOG, offered.id(), "e1", seed);
            Incident started = snapshot.getIncidents().getFirst();
            double before = snapshot.getMissionComponents().get("capsule-mercury").getCurrentReliability();
            int moraleBefore = lead.getMorale();

            List<ResolutionEvent> events = Investigations.advanceSeason(snapshot, CATALOG, TURN + 1);

            InvestigationConcluded concluded = (InvestigationConcluded) events.getFirst();
            double after = snapshot.getMissionComponents().get("capsule-mercury").getCurrentReliability();
            CareerEntry line = lead.getCareer().getLast();
            assertEquals(CareerEntry.Kind.INVESTIGATION, line.kind());
            if (concluded.found()) {
                found++;
                assertEquals(before + started.share() * started.totalLoss(), after, 1e-9);
                assertEquals(after - before, concluded.totalRecovered(), 1e-9);
                assertEquals(Investigations.SUCCESS_MORALE, lead.getMorale() - moraleBefore);
                assertEquals(Investigations.FOUND, line.outcome());
            } else {
                undetermined++;
                assertEquals(before, after, 1e-9);
                assertNull(concluded.causeId());
                assertEquals(moraleBefore, lead.getMorale());
                assertEquals(Investigations.UNDETERMINED, line.outcome());
            }
            assertTrue(snapshot.getIncidents().isEmpty());
            assertEquals(AssignmentState.ASSIGNED, lead.getAssignmentState(), "back on the R&D they left");
        }
        assertTrue(found > 0 && undetermined > 0, found + " found, " + undetermined + " undetermined");
    }

    @Test
    void theRecoveryStopsAtTheComponentsMaximum() {
        for (long seed = 1; seed <= 40; seed++) {
            PlayerSnapshot snapshot = player(50);
            failUncrewed(snapshot);
            engineer(snapshot, "e1", 100);
            Investigations.start(snapshot, CATALOG, snapshot.getIncidents().getFirst().id(), "e1", seed);
            snapshot.getMissionComponents().get("capsule-mercury").setCurrentReliability(88);
            InvestigationConcluded concluded =
                (InvestigationConcluded) Investigations.advanceSeason(snapshot, CATALOG, TURN + 1).getFirst();
            if (concluded.found()) {
                assertEquals(CAPSULE.maxReliability(),
                    snapshot.getMissionComponents().get("capsule-mercury").getCurrentReliability(), 1e-9);
                return;
            }
        }
        throw new AssertionError("no seed found the cause");
    }

    @Test
    void anUnansweredOfferLapsesAfterItsLastTurn() {
        PlayerSnapshot snapshot = player(50);
        failUncrewed(snapshot);
        assertTrue(Investigations.advanceSeason(snapshot, CATALOG, TURN).isEmpty(), "not in its own resolution");
        assertTrue(Investigations.advanceSeason(snapshot, CATALOG, TURN + 1).isEmpty());
        List<ResolutionEvent> events = Investigations.advanceSeason(snapshot, CATALOG, TURN + Investigations.OFFER_TURNS);
        assertTrue(events.getFirst() instanceof InvestigationLapsed);
        assertTrue(snapshot.getIncidents().isEmpty());
    }

    @Test
    void aLostCrewsBoardReportsTheTurnCrewedFlightsMayFlyAgain() {
        PlayerSnapshot snapshot = player(50);
        Astronaut pilot = new Astronaut("a1", "Ada Pilot", 34, 80, 60);
        pilot.setAssignmentState(AssignmentState.ASSIGNED);
        pilot.setAssignedMissionId("m1");
        snapshot.setAstronauts(new ArrayList<>(List.of(pilot)));
        snapshot.getInProgressMissions().put("m1", new InProgressMission("m1", "crewed-std", List.of("a1"), List.of()));

        List<ResolutionEvent> events = MissionProgress.advanceSeason(snapshot, CATALOG, new Scripted(0.0, 5.0, 0.2),
            new GoalTally(), TURN);

        Incident incident = snapshot.getIncidents().getFirst();
        assertEquals(Incident.Kind.OFFICIAL, incident.kind());
        assertEquals(List.of("Ada Pilot"), incident.crewNames());
        assertTrue(events.stream().anyMatch(OfficialInvestigationOpened.class::isInstance));
        assertEquals(Investigations.Outcome.NO_INCIDENT,
            Investigations.start(snapshot, CATALOG, incident.id(), "nobody", 1), "nobody at the agency leads it");
        for (int turn = TURN + 1; turn < TURN + CrewedGrounding.GROUNDING_SEASONS; turn++) {
            assertTrue(Investigations.advanceSeason(snapshot, CATALOG, turn).isEmpty(), "turn " + turn);
        }
        OfficialReportIssued report = (OfficialReportIssued) Investigations.advanceSeason(snapshot, CATALOG,
            TURN + CrewedGrounding.GROUNDING_SEASONS).getFirst();
        assertEquals(TURN, report.accidentTurn());
        assertEquals(List.of("Ada Pilot"), report.crewNames());
        assertTrue(snapshot.getIncidents().isEmpty());
    }

    @Test
    void theClientNeverSeesTheSeed() {
        PlayerSnapshot snapshot = player(50);
        failUncrewed(snapshot);
        engineer(snapshot, "e1", 70);
        Investigations.start(snapshot, CATALOG, snapshot.getIncidents().getFirst().id(), "e1", 12345);
        PlayerSnapshot copy = Personality.forClient(snapshot, new com.fasterxml.jackson.databind.ObjectMapper());
        assertEquals(0, copy.getIncidents().getFirst().seed());
        assertEquals(12345, snapshot.getIncidents().getFirst().seed());
    }

    /** Returns scripted rolls from {@code nextDouble(origin, bound)}, in order. */
    private static final class Scripted implements RandomGenerator {

        private final double[] rolls;
        private int next;

        private Scripted(double... rolls) {
            this.rolls = rolls;
        }

        @Override
        public long nextLong() {
            throw new UnsupportedOperationException("not scripted");
        }

        @Override
        public double nextDouble(double origin, double bound) {
            return rolls[next++];
        }
    }
}
