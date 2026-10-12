package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.ContentPackLoader;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.MissionControlOfficer;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSpecialist;
import de.mkoehler.launchwindow.model.TrainingKind;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the advisor's items on the real pack: they tick off from the state, crew only matters
 * once a crewed program is open, and the first turn report waits for a resolved turn.
 *
 * @author Mario Koehler
 */
class AdvisorTest {

    private static final ContentCatalog PACK = ContentPackLoader.load(Path.of("..", "assets", "content",
        "early-era"));
    private static final Advisor.Marks NEW = new Advisor.Marks(false, false, false);

    private static List<String> open(PlayerSnapshot state, Advisor.Marks marks) {
        return Advisor.open(state, PACK, marks).stream().map(Advisor.Item::id).toList();
    }

    private static PlayerSnapshot soviet() {
        return new PlayerSnapshot("Vega", Archetype.SOVIET, 25_000, 250, 3_000, 1);
    }

    /**
     * A new agency sees the first steps in order, with nothing about crews or the report yet. A
     * program, a rocket and R&D wait until an engineer is out of basic training (play test,
     * 2026-10-09), and reliability until someone is on R&D.
     */
    @Test
    void newAgency() {
        assertEquals(List.of("help", "hire-engineers"), open(soviet(), NEW));
        assertFalse(Advisor.finished(soviet(), PACK, NEW));

        PlayerSnapshot hired = soviet();
        SetSpecialist recruit = new SetSpecialist("e1", "A. Engineer", 30, 70, 50);
        recruit.setTrainingKind(TrainingKind.BASIC);
        hired.getSetSpecialists().add(recruit);
        assertEquals(List.of("help"), open(hired, NEW), "nothing to open while the engineer trains");
        Advisor.Item next = Advisor.later(hired, PACK, NEW).get(0);
        assertEquals("open-program", next.id());
        assertEquals("Once your first engineers finish basic training", Advisor.waitingFor(next, hired));

        recruit.setTrainingKind(TrainingKind.NONE);
        assertEquals(List.of("help", "open-program", "open-rocket", "assign-rnd"), open(hired, NEW));
    }

    /**
     * Each step ticks itself off from the state; a satellite program needs controllers but no
     * astronauts, a crewed one both.
     */
    @Test
    void stepsTickOff() {
        PlayerSnapshot state = soviet();
        Advisor.Marks helped = new Advisor.Marks(true, false, false);
        SetSpecialist engineer = new SetSpecialist("e1", "A. Engineer", 30, 70, 50);
        state.getSetSpecialists().add(engineer);
        state.getOpenedProgramIds().add("sputnik-1");
        assertEquals(List.of("open-rocket", "assign-rnd", "hire-controllers"), open(state, helped));

        state.getMissionComponents().put("sputnik-1-satellite", new OwnedMissionComponent(3, 3));
        state.getMissionComponents().put("r7-sputnik", new OwnedMissionComponent(3, 3));
        engineer.setRdTargetComponentId("sputnik-1-satellite");
        state.getMissionControlOfficers().add(new MissionControlOfficer("c1", "B. Controller", 30, 70, 50));
        assertEquals(List.of("raise-reliability"), open(state, helped));

        // The mission waits until the payload and a rocket both reach the bar.
        state.getMissionComponents().get("sputnik-1-satellite").setCurrentReliability(64);
        assertEquals(List.of("raise-reliability"), open(state, helped));
        state.getMissionComponents().get("r7-sputnik").setCurrentReliability(Advisor.READY_RELIABILITY);
        assertEquals(List.of("plan-mission"), open(state, helped));

        state.getOpenedProgramIds().add("vostok");
        assertEquals(List.of("hire-astronauts", "plan-mission"), open(state, helped));
        state.getAstronauts().add(new Astronaut("a1", "C. Astronaut", 30, 70, 50));
        // A flight that already ended still counts as the first mission.
        state.setLastFlightSeasonTurn(2);
        assertEquals(List.of(), open(state, helped));
    }

    /**
     * With everything done before the first turn resolves, the card waits for the report rather
     * than retiring; once it's read, the advisor is finished.
     */
    @Test
    void reportWaitsForTheTurn() {
        PlayerSnapshot state = soviet();
        state.getSetSpecialists().add(new SetSpecialist("e1", "A. Engineer", 30, 70, 50));
        state.getSetSpecialists().get(0).setRdTargetComponentId("sputnik-1-satellite");
        state.getOpenedProgramIds().add("sputnik-1");
        state.getMissionComponents().put("r7-sputnik", new OwnedMissionComponent(50, 40));
        state.getMissionControlOfficers().add(new MissionControlOfficer("c1", "B. Controller", 30, 70, 50));
        state.setLastFlightSeasonTurn(1);

        Advisor.Marks waiting = new Advisor.Marks(true, false, false);
        assertEquals(List.of(), open(state, waiting));
        assertEquals(List.of("read-report"), Advisor.later(state, PACK, waiting).stream().map(Advisor.Item::id)
            .toList());
        assertFalse(Advisor.finished(state, PACK, waiting));
        assertEquals(List.of("read-report"), open(state, new Advisor.Marks(true, true, false)));
        assertTrue(Advisor.finished(state, PACK, new Advisor.Marks(true, true, true)));
    }

    /**
     * "Raise reliability" says where things stand and sends the player to whichever board is
     * further behind.
     */
    @Test
    void reliabilityProgress() {
        PlayerSnapshot state = soviet();
        Advisor.Item raise = Advisor.ITEMS.stream().filter(item -> item.id().equals("raise-reliability"))
            .findFirst().orElseThrow();
        assertEquals("Now: payload none open, rocket none open.", Advisor.progress(raise, state, PACK));
        state.getOpenedProgramIds().add("sputnik-1");
        state.getMissionComponents().put("sputnik-1-satellite", new OwnedMissionComponent(41.7, 3));
        state.getMissionComponents().put("r7-sputnik", new OwnedMissionComponent(22, 3));
        assertEquals("Now: payload 41\u00A0%, rocket 22\u00A0%.", Advisor.progress(raise, state, PACK));
        assertEquals(Advisor.Destination.ROCKET_RND, Advisor.destination(raise, state, PACK));
        state.getMissionComponents().get("r7-sputnik").setCurrentReliability(50);
        assertEquals(Advisor.Destination.RND, Advisor.destination(raise, state, PACK));
    }

    /**
     * Every item's "?" opens on topics the help has.
     */
    @Test
    void topicsExist() throws IOException {
        Help help = Help.parse(Files.readString(Path.of("..", "assets", "content", Help.FILE)));
        for (Advisor.Item item : Advisor.ITEMS) {
            assertFalse(item.topicIds().isEmpty(), item.id());
            for (String id : item.topicIds()) {
                assertTrue(help.byId(id).isPresent(), item.id() + " names a missing topic " + id);
            }
        }
    }

    /**
     * The idle tip (2026-10-12) waits for the first steps, then comes when someone is idle, points at
     * their building, and goes once nobody is.
     */
    @Test
    void idleTipComesOnceTheAgencyIsSetUpAndSomeoneIsIdle() {
        PlayerSnapshot state = soviet();
        SetSpecialist working = new SetSpecialist("e1", "A. Engineer", 30, 70, 50);
        SetSpecialist spare = new SetSpecialist("e2", "B. Engineer", 30, 70, 50);
        state.getSetSpecialists().addAll(List.of(working, spare));
        assertFalse(Advisor.idleTipDue(state), "not during the first steps");

        working.setRdTargetComponentId("sputnik-1-satellite");
        working.setRdSlot(0);
        working.setAssignmentState(de.mkoehler.launchwindow.model.AssignmentState.ASSIGNED);
        assertTrue(Advisor.idleTipDue(state));
        assertEquals(Advisor.Destination.SET_CENTRE, Advisor.destination(Advisor.IDLE_TIP, state, PACK));
        assertFalse(Advisor.ITEMS.contains(Advisor.IDLE_TIP), "never one of the first steps");

        Astronaut waiting = new Astronaut("a1", "C. Idle", 30, 70, 50);
        state.getAstronauts().add(waiting);
        spare.setAssignmentState(de.mkoehler.launchwindow.model.AssignmentState.IN_TRAINING);
        spare.setTrainingKind(TrainingKind.ADVANCED);
        assertFalse(Advisor.idleTipDue(state), "someone in training: the player knows, though an astronaut waits");
    }
}
