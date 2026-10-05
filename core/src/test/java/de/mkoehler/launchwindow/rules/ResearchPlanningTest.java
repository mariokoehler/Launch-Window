package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.ContentPackLoader;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSkill;
import de.mkoehler.launchwindow.model.SetSpecialist;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for {@link ResearchPlanning}: the projection matches the season pass on average, and the
 * best team takes the most skilled engineers who are free or already on the component.
 *
 * @author Mario Koehler
 */
class ResearchPlanningTest {

    private static final ContentCatalog PACK = ContentPackLoader.load(Path.of("..", "assets", "content", "early-era"));
    private static final String ROCKET = "jupiter-c";

    private static SetSpecialist engineer(String id, double rockets) {
        SetSpecialist specialist = new SetSpecialist(id, id, 30, 70, 50);
        specialist.setSkill(SetSkill.ROCKETS, rockets);
        return specialist;
    }

    private static PlayerSnapshot nasa(List<SetSpecialist> engineers) {
        PlayerSnapshot snapshot = new PlayerSnapshot("Agency", Archetype.NASA, 100_000, 0, 0, 1);
        snapshot.getMissionComponents().put(ROCKET, new OwnedMissionComponent(10, 10));
        snapshot.setSetSpecialists(new ArrayList<>(engineers));
        return snapshot;
    }

    /**
     * Verifies the projection equals the average of many real season passes for the same team, so the
     * board shows what the rules will do on average.
     */
    @Test
    void expectedGainIsTheSeasonPassOnAverage() {
        List<SetSpecialist> team = List.of(engineer("a", 70), engineer("b", 40));
        PlayerSnapshot planned = nasa(team);
        for (SetSpecialist specialist : team) {
            assertEquals(PersonnelActions.ResearchOutcome.ASSIGNED,
                PersonnelActions.assignResearch(planned, specialist.getId(), ROCKET, PACK));
        }
        double expected = ResearchPlanning.expectedSeasonGain(planned, PACK, ROCKET);

        int runs = 4_000;
        double total = 0;
        SplittableRandom random = new SplittableRandom(7);
        for (int i = 0; i < runs; i++) {
            List<SetSpecialist> copy = List.of(engineer("a", 70), engineer("b", 40));
            PlayerSnapshot snapshot = nasa(copy);
            for (SetSpecialist specialist : copy) {
                PersonnelActions.assignResearch(snapshot, specialist.getId(), ROCKET, PACK);
            }
            ResearchProgress.advanceSeason(snapshot, PACK, random);
            total += snapshot.getMissionComponents().get(ROCKET).getCurrentReliability() - 10;
        }
        assertEquals(expected, total / runs, expected * 0.02, "within 2 % of the simulated average");
    }

    /**
     * Verifies the best team is the most skilled engineers who are idle or already on the component,
     * at most four, leaving out anyone busy elsewhere.
     */
    @Test
    void bestTeamTakesTheMostSkilledFreeOrAlreadyAssigned() {
        SetSpecialist onIt = engineer("on-it", 50);
        SetSpecialist best = engineer("best", 90);
        SetSpecialist training = engineer("training", 95);
        training.setAssignmentState(AssignmentState.IN_TRAINING);
        SetSpecialist weak = engineer("weak", 10);
        SetSpecialist good = engineer("good", 60);
        SetSpecialist fair = engineer("fair", 30);
        PlayerSnapshot snapshot = nasa(List.of(onIt, best, training, weak, good, fair));
        PersonnelActions.assignResearch(snapshot, "on-it", ROCKET, PACK);
        MissionComponent rocket = PACK.missionComponent(ROCKET).orElseThrow();

        assertEquals(List.of("best", "good", "on-it", "fair"),
            ResearchPlanning.bestTeam(snapshot, rocket).stream().map(SetSpecialist::getId).toList());
    }
}
