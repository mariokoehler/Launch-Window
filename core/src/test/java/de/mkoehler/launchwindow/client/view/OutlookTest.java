package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.ContentPackLoader;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.content.Program;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.TrainingKind;
import de.mkoehler.launchwindow.rules.ResearchPlanning;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the look-ahead helpers: which marks an R&amp;D bar shows, where a goal is earned, a
 * program's staffing against the roster, and what a mission still waits on.
 *
 * @author Mario Koehler
 */
class OutlookTest {

    private static final ContentCatalog EARLY = ContentPackLoader.load(Path.of("..", "assets", "content",
        "early-era"));
    private static final ContentCatalog STANDARD = ContentPackLoader.load(Path.of("..", "assets", "content",
        "standard"));

    /**
     * The bar marks the milestones still ahead and clear of the cap, then the cap; nothing once the
     * cap is reached.
     */
    @Test
    void markersSkipPassedMilestonesAndOnesBesideTheCap() {
        ResearchPlanning.Projection projection = new ResearchPlanning.Projection(64, 82, new double[] {70, 80, 82},
            List.of());
        List<Outlook.Marker> markers = Outlook.markers(projection);
        assertEquals(List.of(70.0, 82.0), markers.stream().map(Outlook.Marker::value).toList(),
            "60 is passed and 80 sits within the merge gap of the cap");
        assertEquals(OptionalInt.of(1), markers.get(0).seasons());
        assertTrue(markers.get(1).cap());
        assertEquals(OptionalInt.of(3), markers.get(1).seasons());
        assertTrue(Outlook.markers(new ResearchPlanning.Projection(82, 82, new double[0], List.of())).isEmpty());
    }

    /**
     * A goal's sources are the agency's own programs whose missions earn it, never the other side's.
     */
    @Test
    void goalSourcesAreTheAgencysOwnPrograms() {
        PlayerSnapshot nasa = new PlayerSnapshot("Orion", Archetype.NASA, 25_000, 250, 3_000, 1);
        List<String> programs = Outlook.sources("earth-orbit-satellite", nasa, STANDARD).stream()
            .map(source -> source.program().id()).toList();
        assertTrue(programs.contains("explorer-1"), programs.toString());
        assertFalse(programs.contains("korabl-sputnik-2"), "a Soviet program never shows for NASA");
        assertTrue(Outlook.sourceLine("earth-orbit-satellite", nasa, STANDARD).startsWith("Earned by "));
    }

    /**
     * A program's staffing reads short with an empty roster, coming while recruits train, and ready
     * once enough are free.
     */
    @Test
    void programStaffingAgainstTheRoster() {
        PlayerSnapshot soviet = new PlayerSnapshot("Vega", Archetype.SOVIET, 25_000, 250, 3_000, 1);
        Program vostok = EARLY.program("vostok").orElseThrow();
        Outlook.Need astronauts = Outlook.needs(vostok, soviet, EARLY).get(0);
        assertEquals("astronauts", astronauts.group());
        assertEquals(Outlook.Supply.SHORT, astronauts.supply());

        for (int i = 0; i < astronauts.least(); i++) {
            Astronaut recruit = new Astronaut("a" + i, "A. Cosmonaut", 28, 70, 50);
            recruit.setAssignmentState(AssignmentState.IN_TRAINING);
            recruit.setTrainingKind(TrainingKind.BASIC);
            soviet.getAstronauts().add(recruit);
        }
        assertEquals(Outlook.Supply.COMING, Outlook.needs(vostok, soviet, EARLY).get(0).supply());
        soviet.getAstronauts().forEach(astronaut -> astronaut.setAssignmentState(AssignmentState.IDLE));
        assertEquals(Outlook.Supply.READY, Outlook.needs(vostok, soviet, EARLY).get(0).supply());
    }

    /**
     * A crewed mission with nobody hired is blocked on its astronauts and has no earliest season; a
     * payload short of the ready mark with nobody on its R&amp;D blocks it too.
     */
    @Test
    void readinessNamesWhatBlocksAMission() {
        PlayerSnapshot soviet = new PlayerSnapshot("Vega", Archetype.SOVIET, 25_000, 250, 3_000, 1);
        soviet.getOpenedProgramIds().add("vostok");
        soviet.getMissionComponents().put("vostok-capsule", new OwnedMissionComponent(30, 30));
        MissionConfiguration mission = Missions.of(EARLY.program("vostok").orElseThrow(), EARLY).stream()
            .filter(configuration -> Missions.crewCount(configuration) > 0).findFirst().orElseThrow();
        Outlook.Readiness readiness = Outlook.of(mission, soviet, EARLY);
        assertTrue(readiness.earliest().isEmpty());
        Outlook.Check payload = readiness.checks().stream().filter(check -> check.label().equals("Payload"))
            .findFirst().orElseThrow();
        assertEquals(Outlook.Status.BLOCKED, payload.status());
        assertTrue(payload.detail().contains("nobody works on its R&D"), payload.detail());
        assertTrue(readiness.checks().stream().anyMatch(check -> check.label().equals("Astronauts")
            && check.status() == Outlook.Status.BLOCKED));

        // Astronauts away on a mission come back: never "hire more" for them.
        for (int i = 0; i < 3; i++) {
            Astronaut flying = new Astronaut("f" + i, "A. Cosmonaut", 28, 70, 50);
            flying.setAssignmentState(AssignmentState.ASSIGNED);
            soviet.getAstronauts().add(flying);
        }
        Outlook.Check astronauts = Outlook.of(mission, soviet, EARLY).checks().stream()
            .filter(check -> check.label().equals("Astronauts")).findFirst().orElseThrow();
        assertEquals(Outlook.Status.BLOCKED, astronauts.status());
        assertFalse(astronauts.detail().contains("hire"), astronauts.detail());

        // One free and one in training that can be cut short: amber, not a tick.
        soviet.getAstronauts().clear();
        Astronaut idle = new Astronaut("i", "A. Cosmonaut", 28, 70, 50);
        Astronaut training = new Astronaut("t", "B. Cosmonaut", 28, 70, 50);
        training.setAssignmentState(AssignmentState.IN_TRAINING);
        training.setTrainingKind(TrainingKind.ADVANCED);
        soviet.getAstronauts().addAll(List.of(idle, training));
        Outlook.Check cutShort = Outlook.of(mission, soviet, EARLY).checks().stream()
            .filter(check -> check.label().equals("Astronauts")).findFirst().orElseThrow();
        assertEquals(Outlook.Status.WAITING, cutShort.status());
        assertEquals(0, cutShort.seasons());
    }

    /**
     * A mission's rocket is the smallest of the agency's that can fly it: Redstone for Mercury's
     * crewed hop, Atlas for its orbit. With Redstone open, the hop fits and the orbit names Atlas;
     * the program's line spans both and reads "some".
     */
    @Test
    void rocketRequirementsNameTheSmallestRocket() {
        PlayerSnapshot nasa = new PlayerSnapshot("Orion", Archetype.NASA, 25_000, 250, 3_000, 1);
        MissionConfiguration hop = EARLY.missionConfiguration("mercury-suborbital").orElseThrow();
        MissionConfiguration orbit = EARLY.missionConfiguration("mercury-orbital").orElseThrow();
        assertEquals("redstone", Outlook.leastRocket(hop.launches().get(0), nasa, EARLY).id());
        assertEquals("atlas", Outlook.leastRocket(orbit.launches().get(0), nasa, EARLY).id());

        nasa.getMissionComponents().put("redstone", new OwnedMissionComponent(50, 30));
        assertTrue(Outlook.rocketLine(hop, nasa, EARLY).fits());
        Outlook.RocketLine line = Outlook.rocketLine(orbit, nasa, EARLY);
        assertFalse(line.fits());
        assertTrue(line.text().contains("needs at least Atlas"), line.text());

        Outlook.ProgramRockets rockets = Outlook.programRockets(EARLY.program("mercury").orElseThrow(), nasa, EARLY);
        assertEquals(Outlook.Supply.COMING, rockets.supply());
        assertEquals("atlas", rockets.hardest().id());
    }
}
