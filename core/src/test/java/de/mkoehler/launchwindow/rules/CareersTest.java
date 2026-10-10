package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.MissionConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.ContentPackLoader;
import de.mkoehler.launchwindow.model.Ambition;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.CareerEntry;
import de.mkoehler.launchwindow.model.MemorialEntry;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSpecialist;
import de.mkoehler.launchwindow.model.Trait;
import de.mkoehler.launchwindow.model.TraitState;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests people's careers ({@code design.md} &sect;2.2(d), phase 4): ambitions said, fulfilled,
 * passed over, made impossible and run out; loyalty; the career record and its cap; R&amp;D stints;
 * and the memorial's person page.
 *
 * @author Mario Koehler
 */
class CareersTest {

    private static final ContentCatalog CATALOG =
        ContentPackLoader.load(Path.of("..", "assets", "content", "early-era"));
    private static final String COMPONENT = "sputnik-1-satellite";

    private static PlayerSnapshot agency() {
        PlayerSnapshot snapshot = new PlayerSnapshot("Hanseatic Space Agency", Archetype.SOVIET, 25_000, 0, 0, 1);
        snapshot.getMissionComponents().put(COMPONENT, new OwnedMissionComponent(40, 40));
        return snapshot;
    }

    private static SetSpecialist engineer(PlayerSnapshot snapshot, String id, Trait... traits) {
        SetSpecialist engineer = new SetSpecialist(id, "Engineer " + id, 30, 60, 50);
        engineer.setTraits(java.util.Arrays.stream(traits).map(trait -> new TraitState(trait, 3, false)).toList());
        snapshot.getSetSpecialists().add(engineer);
        return engineer;
    }

    private static void research(SetSpecialist engineer) {
        engineer.setAssignmentState(AssignmentState.ASSIGNED);
        engineer.setRdTargetComponentId(COMPONENT);
        engineer.setRdSlot(0);
    }

    private static List<ResolutionEvent> season(PlayerSnapshot snapshot, int turn, ResolutionEvent... events) {
        return Careers.advanceSeason(snapshot, CATALOG, new GoalTally(), List.of(), List.of(events), turn);
    }

    @Test
    void ambitiousCandidatesAlwaysHaveAnAmbitionComingAndTheClientNeverSeesIt() throws Exception {
        PlayerSnapshot snapshot = agency();
        int pending = 0;
        for (int i = 0; i < 400; i++) {
            SetSpecialist candidate = new SetSpecialist("c" + i, "C", 30, 60, 50);
            candidate.setTraits(List.of(new TraitState(Trait.AMBITIOUS, 3, false)));
            Careers.assignPending(snapshot, candidate);
            assertTrue(candidate.isAmbitionPending());
            SetSpecialist plain = new SetSpecialist("d" + i, "D", 30, 60, 50);
            Careers.assignPending(snapshot, plain);
            pending += plain.isAmbitionPending() ? 1 : 0;
        }
        assertTrue(Math.abs(pending / 4.0 - Careers.AMBITION_PERCENT_OTHERS) < 6, pending + " of 400");
        SetSpecialist hired = engineer(snapshot, "p1");
        hired.setAmbitionPending(true);
        hired.setAmbition(new Ambition(Ambition.Kind.TOP_ENGINEER, "", 5, 6, 3));
        PlayerSnapshot client = Personality.forClient(snapshot, new ObjectMapper());
        assertFalse(client.getSetSpecialists().getFirst().isAmbitionPending(), "a coming ambition stays hidden");
        assertNotNull(client.getSetSpecialists().getFirst().getAmbition(), "a said one is the player's to see");
    }

    @Test
    void anAmbitionIsSaidAfterTwoSeasonsWithAWindowSizedByTraits() {
        PlayerSnapshot snapshot = agency();
        SetSpecialist eager = engineer(snapshot, "p1", Trait.AMBITIOUS);
        SetSpecialist calm = engineer(snapshot, "p2", Trait.STEADY);
        research(eager);
        research(calm);
        eager.setAmbitionPending(true);
        calm.setAmbitionPending(true);
        assertTrue(season(snapshot, 1).isEmpty(), "nobody says anything in their first season");
        List<ResolutionEvent> said = season(snapshot, 2);
        assertEquals(2, said.size());
        Ambition kind = eager.getAmbition();
        assertNotNull(kind);
        assertEquals(Math.max(Careers.WINDOW_MIN, kind.kind().window() + Careers.WINDOW_AMBITIOUS), kind.window());
        assertEquals(calm.getAmbition().kind().window() + Careers.WINDOW_STEADY, calm.getAmbition().window());
        assertFalse(eager.isAmbitionPending());
        assertEquals(CareerEntry.Kind.AMBITION, eager.getCareer().getLast().kind());
    }

    @Test
    void reachingTheCapFulfilsItAndSomeoneElseReachingItPassesOver() {
        PlayerSnapshot snapshot = agency();
        SetSpecialist winner = engineer(snapshot, "p1");
        SetSpecialist waiting = engineer(snapshot, "p2");
        Ambition cap = new Ambition(Ambition.Kind.COMPONENT_CAP, COMPONENT, 6, 6, 1);
        winner.setAmbition(cap);
        waiting.setAmbition(cap);
        int morale = winner.getMorale();
        int loyalty = winner.getLoyalty();
        List<ResolutionEvent> events = season(snapshot, 5,
            new ResearchComponentMaxed(COMPONENT, List.of("p1"), List.of("Engineer p1")));
        assertNull(winner.getAmbition());
        assertEquals(morale + Careers.FULFILLED_MORALE, winner.getMorale());
        assertEquals(loyalty + Careers.LOYALTY_PER_SEASON + Careers.FULFILLED_LOYALTY, winner.getLoyalty());
        assertEquals(4, waiting.getAmbition().seasonsLeft(), "passed over: a season off, and one more");
        assertEquals(AmbitionSettled.Outcome.PASSED_OVER, ((AmbitionSettled) events.get(1)).outcome());
    }

    @Test
    void aRunOutAmbitionLeavesThemDisappointedThenMaybeWantingAgain() {
        PlayerSnapshot snapshot = agency();
        SetSpecialist engineer = engineer(snapshot, "p1");
        engineer.setAmbition(new Ambition(Ambition.Kind.COMPONENT_CAP, COMPONENT, 1, 6, 1));
        int loyalty = engineer.getLoyalty();
        List<ResolutionEvent> events = season(snapshot, 7);
        assertEquals(AmbitionSettled.Outcome.RAN_OUT, ((AmbitionSettled) events.getFirst()).outcome());
        assertEquals(Careers.DISAPPOINTED_SEASONS, engineer.getDisappointedSeasons());
        assertEquals(loyalty + Careers.LOYALTY_PER_SEASON + Careers.RAN_OUT_LOYALTY, engineer.getLoyalty());
        for (int turn = 8; turn < 8 + Careers.DISAPPOINTED_SEASONS; turn++) {
            assertTrue(season(snapshot, turn).isEmpty());
        }
        assertEquals(0, engineer.getDisappointedSeasons());
    }

    @Test
    void anAmbitionWhoseComponentIsGoneBecomesImpossible() {
        PlayerSnapshot snapshot = agency();
        SetSpecialist engineer = engineer(snapshot, "p1");
        engineer.setAmbition(new Ambition(Ambition.Kind.COMPONENT_CAP, COMPONENT, 5, 6, 1));
        snapshot.getMissionComponents().remove(COMPONENT);
        List<ResolutionEvent> events = season(snapshot, 4);
        assertEquals(AmbitionSettled.Outcome.IMPOSSIBLE, ((AmbitionSettled) events.getFirst()).outcome());
        assertNull(engineer.getAmbition());
    }

    @Test
    void stintsAddUpAndCloseIntoTheRecord() {
        PlayerSnapshot snapshot = agency();
        SetSpecialist engineer = engineer(snapshot, "p1");
        research(engineer);
        season(snapshot, 1, new ResearchCompleted("p1", "E", COMPONENT, 0.5, 2.0, 0));
        season(snapshot, 2, new ResearchCompleted("p1", "E", COMPONENT, 0.5, 1.5, 0));
        assertEquals(2, engineer.getStintSeasons());
        engineer.setAssignmentState(AssignmentState.IDLE);
        engineer.setRdTargetComponentId(null);
        season(snapshot, 3);
        CareerEntry stint = engineer.getCareer().getLast();
        assertEquals(CareerEntry.Kind.RD_STINT, stint.kind());
        assertEquals(2, stint.count());
        assertEquals(3.5, stint.value(), 1e-9);
        assertEquals(CareerEntry.Kind.HIRED, engineer.getCareer().getFirst().kind());
    }

    @Test
    void theRecordKeepsItsHiringLineAndTheNewest() {
        PlayerSnapshot snapshot = agency();
        SetSpecialist engineer = engineer(snapshot, "p1");
        for (int turn = 1; turn <= Careers.CAREER_KEPT + 10; turn++) {
            season(snapshot, turn, new TrainingCompleted(PersonnelGroup.SET, "p1", "E",
                de.mkoehler.launchwindow.model.TrainingKind.ADVANCED, "ROCKETS", 1));
        }
        assertEquals(Careers.CAREER_KEPT, engineer.getCareer().size());
        assertEquals(CareerEntry.Kind.HIRED, engineer.getCareer().getFirst().kind());
        assertEquals(Careers.CAREER_KEPT + 10, engineer.getCareer().getLast().turn());
    }

    private static MissionStepResolved step(String mission, int seasonIndex, MissionStepOutcome outcome) {
        return new MissionStepResolved(mission, "vostok-orbital", seasonIndex, "s" + seasonIndex, outcome, List.of(),
            0.9, true, null, null, null, 0);
    }

    private static Astronaut astronaut(PlayerSnapshot snapshot, String id, Ambition.Kind kind) {
        Astronaut astronaut = new Astronaut(id, "Pilot " + id, 30, 60, 50);
        astronaut.setAmbition(new Ambition(kind, "", 6, 6, 1));
        snapshot.getAstronauts().add(astronaut);
        return astronaut;
    }

    @Test
    void theCrewIsFulfilledAndOthersArePassedOverOncePerFlight() {
        PlayerSnapshot snapshot = agency();
        snapshot.getOpenedProgramIds().add("vostok");
        Astronaut aboard = astronaut(snapshot, "a1", Ambition.Kind.FLY_TO_ORBIT);
        Astronaut ground = astronaut(snapshot, "a2", Ambition.Kind.FLY_TO_ORBIT);
        Careers.Flight flight = new Careers.Flight("m1", "vostok-orbital", List.of("a1"), List.of(), List.of());
        snapshot.getInProgressMissions().put("m1", new de.mkoehler.launchwindow.model.InProgressMission());
        List<ResolutionEvent> first = Careers.advanceSeason(snapshot, CATALOG, new GoalTally(), List.of(flight),
            List.of(step("m1", 0, MissionStepOutcome.SUCCESSFUL)), 5);
        assertNull(aboard.getAmbition());
        assertEquals(AmbitionSettled.Outcome.FULFILLED, ((AmbitionSettled) first.getFirst()).outcome());
        assertEquals(4, ground.getAmbition().seasonsLeft());
        Careers.advanceSeason(snapshot, CATALOG, new GoalTally(), List.of(flight),
            List.of(step("m1", 1, MissionStepOutcome.SUCCESSFUL)), 6);
        assertEquals(3, ground.getAmbition().seasonsLeft(), "the same flight's second season doesn't pass over again");
    }

    @Test
    void aLostCrewsLastFlightGoesOnTheirMemorial() {
        PlayerSnapshot snapshot = agency();
        Astronaut lost = new Astronaut("a9", "Lost Pilot", 33, 70, 50);
        snapshot.getMemorial().add(Careers.memorial(lost, 8, MemorialEntry.Cause.MISSION, "vostok-orbital", "s0"));
        Careers.Flight flight = new Careers.Flight("m2", "vostok-orbital", List.of("a9"), List.of(), List.of());
        Careers.advanceSeason(snapshot, CATALOG, new GoalTally(), List.of(flight),
            List.of(step("m2", 0, MissionStepOutcome.CATASTROPHIC)), 8);
        CareerEntry last = snapshot.getMemorial().getFirst().career().getLast();
        assertEquals(CareerEntry.Kind.FLIGHT, last.kind());
        assertEquals("s0", last.outcome());
    }

    @Test
    void aRetriedResolutionSaysTheSameAmbition() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        PlayerSnapshot snapshot = agency();
        snapshot.getOpenedProgramIds().add("vostok");
        for (int i = 0; i < 6; i++) {
            SetSpecialist engineer = engineer(snapshot, "p" + i);
            engineer.setAmbitionPending(true);
            engineer.setTenureSeasons(3);
        }
        PlayerSnapshot again = mapper.readValue(mapper.writeValueAsBytes(snapshot), PlayerSnapshot.class);
        season(snapshot, 9);
        season(again, 9);
        assertTrue(snapshot.getSetSpecialists().stream().anyMatch(engineer -> engineer.getAmbition() != null),
            "someone said an ambition, so the comparison means something");
        for (int i = 0; i < 6; i++) {
            assertEquals(snapshot.getSetSpecialists().get(i).getAmbition(), again.getSetSpecialists().get(i).getAmbition());
        }
    }

    @Test
    void aRaiseBuildsLoyalty() {
        PlayerSnapshot snapshot = agency();
        SetSpecialist engineer = engineer(snapshot, "p1");
        engineer.setSalary(50);
        assertEquals(DepartmentSalaries.AdjustOutcome.ADJUSTED, DepartmentSalaries.adjust(snapshot, PersonnelGroup.SET, 20));
        assertEquals(Careers.LOYALTY_START + 10, engineer.getLoyalty());
    }

    @Test
    void theMemorialKeepsOnlyTheTraitsThePlayerKnew() {
        Astronaut astronaut = new Astronaut("p9", "Lost Pilot", 33, 70, 50);
        astronaut.setTraits(List.of(new TraitState(Trait.GLORY_SEEKER, 0, true), new TraitState(Trait.LONER, 4, false)));
        astronaut.getCareer().add(new CareerEntry(3, CareerEntry.Kind.HIRED, "", "calm", "", 0, 0));
        MemorialEntry entry = Careers.memorial(astronaut, 9, MemorialEntry.Cause.MISSION, "vostok-orbital", "retrofire");
        assertEquals(List.of(Trait.GLORY_SEEKER), entry.traits());
        assertEquals(3, entry.joinedTurn());
        assertEquals("p9", entry.personnelId());
        assertEquals(1, entry.career().size());
    }

    /**
     * A planned crewed orbital flight meets the ambition of a controller on its consoles and passes
     * over a free one who wanted it; a busy one isn't counted, and an uncrewed flight settles nothing.
     */
    @Test
    void ambitionsAtStakeForAPlannedMission() {
        PlayerSnapshot snapshot = agency();
        de.mkoehler.launchwindow.model.MissionControlOfficer seated =
            new de.mkoehler.launchwindow.model.MissionControlOfficer("c1", "Seated", 40, 70, 50);
        de.mkoehler.launchwindow.model.MissionControlOfficer free =
            new de.mkoehler.launchwindow.model.MissionControlOfficer("c2", "Free", 40, 70, 50);
        de.mkoehler.launchwindow.model.MissionControlOfficer busy =
            new de.mkoehler.launchwindow.model.MissionControlOfficer("c3", "Busy", 40, 70, 50);
        busy.setAssignmentState(AssignmentState.IN_TRAINING);
        for (de.mkoehler.launchwindow.model.MissionControlOfficer officer : List.of(seated, free, busy)) {
            officer.setAmbition(new Ambition(Ambition.Kind.CONSOLES_ORBITAL, "", 6, 12, 1));
            snapshot.getMissionControlOfficers().add(officer);
        }
        MissionConfiguration orbital = CATALOG.missionConfiguration("vostok-orbital").orElseThrow();
        List<Careers.AmbitionAtStake> stakes = Careers.ambitionsAtStake(snapshot, orbital, List.of(), List.of("c1"));
        assertEquals(2, stakes.size());
        assertEquals("c1", stakes.get(0).person().getId());
        assertTrue(stakes.get(0).met());
        assertEquals("c2", stakes.get(1).person().getId());
        assertFalse(stakes.get(1).met(), "free and left off: passed over");

        MissionConfiguration uncrewed = CATALOG.missionConfiguration("vostok-uncrewed-orbital").orElseThrow();
        assertTrue(Careers.ambitionsAtStake(snapshot, uncrewed, List.of(), List.of("c1")).isEmpty());
    }
}
