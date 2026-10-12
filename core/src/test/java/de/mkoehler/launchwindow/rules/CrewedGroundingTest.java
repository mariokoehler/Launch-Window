package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.content.MissionSeason;
import de.mkoehler.launchwindow.content.MissionStep;
import de.mkoehler.launchwindow.content.Program;
import de.mkoehler.launchwindow.content.ProgramCategory;
import de.mkoehler.launchwindow.content.TestBuildings;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.AstronautSkill;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSkill;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link CrewedGrounding}: after a crew is lost, crewed launches are refused for exactly
 * {@link CrewedGrounding#GROUNDING_SEASONS} turns, uncrewed ones never ({@code design.md} &sect;2.5(j)).
 *
 * @author Mario Koehler
 */
class CrewedGroundingTest {

    private static final ProgramCategory CATEGORY = new ProgramCategory("early-space", "Early Space");
    private static final MissionComponent CAPSULE = new MissionComponent("capsule-mercury", SetSkill.CREWED_SPACECRAFT,
        3, 90, 82, List.of(3.0, 6.0, 9.0, 12.0), 0, List.of(), 0);
    private static final Program PROGRAM =
        new Program("mercury-program", "early-space", 1_000, 0, List.of("capsule-mercury"), List.of());
    private static final MissionConfiguration UNCREWED = new MissionConfiguration("crewless-std", "mercury-program",
        List.of(new MissionSeason(List.of(new MissionStep("s1",
            List.of(new MissionStep.ComponentInvolvement("capsule-mercury", 100)), List.of(), List.of(), false)))),
        0, 0, 0, 0);
    private static final MissionConfiguration CREWED = new MissionConfiguration("crewed-std", "mercury-program",
        List.of(new MissionSeason(List.of(new MissionStep("s1",
            List.of(new MissionStep.ComponentInvolvement("capsule-mercury", 100)), List.of(),
            List.of(new MissionStep.CrewInvolvement(0, AstronautSkill.PILOTING, 100)), true)))),
        1, 0, 0, 0);
    private static final ContentCatalog CATALOG = TestBuildings.catalogWithoutUpkeep(List.of(CATEGORY), List.of(PROGRAM),
        List.of(UNCREWED, CREWED), List.of(CAPSULE));

    private static PlayerSnapshot player() {
        PlayerSnapshot snapshot = new PlayerSnapshot("Agency", Archetype.NASA, 10_000, 0, 0, 1);
        snapshot.setOpenedProgramIds(List.of("mercury-program"));
        return snapshot;
    }

    @Test
    void onlyAFlightWithCrewSeatsIsCrewed() {
        assertTrue(CrewedGrounding.crewed(CREWED));
        assertFalse(CrewedGrounding.crewed(UNCREWED));
    }

    /**
     * The loss's resolution leaves the count at N; each later resolution counts one down before
     * any flight, so the N turns after the loss refuse crewed launches and the next allows them.
     */
    @Test
    void aLossGroundsCrewedLaunchesForExactlyTheNextTurns() {
        PlayerSnapshot snapshot = player();
        snapshot.setCrewedGroundingSeasons(CrewedGrounding.GROUNDING_SEASONS);

        for (int turn = 1; turn <= CrewedGrounding.GROUNDING_SEASONS; turn++) {
            assertEquals(MissionLaunchOutcome.CREWED_FLIGHTS_GROUNDED,
                MissionLaunch.launch(snapshot, "crewed-std", List.of(), List.of(), CATALOG), "turn " + turn);
            MissionProgress.advanceSeason(snapshot, CATALOG, new Random(turn));
        }

        assertFalse(CrewedGrounding.grounded(snapshot));
        assertNotEquals(MissionLaunchOutcome.CREWED_FLIGHTS_GROUNDED,
            MissionLaunch.launch(snapshot, "crewed-std", List.of(), List.of(), CATALOG));
    }

    @Test
    void uncrewedFlightsLaunchWhileGrounded() {
        PlayerSnapshot snapshot = player();
        snapshot.setCrewedGroundingSeasons(2);

        assertEquals(MissionLaunchOutcome.LAUNCHED,
            MissionLaunch.launch(snapshot, "crewless-std", List.of(), List.of(), CATALOG));
    }
}
