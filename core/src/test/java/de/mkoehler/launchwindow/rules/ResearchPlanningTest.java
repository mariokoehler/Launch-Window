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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    /**
     * Verifies the projection's path follows many real season passes over several seasons, skill
     * growth included, and that its first season is the board's "next season" gain.
     */
    @Test
    void projectionFollowsTheSeasonPassOverSeveralSeasons() {
        int seasons = 8;
        List<SetSpecialist> team = List.of(engineer("a", 70), engineer("b", 40), engineer("c", 20));
        PlayerSnapshot planned = nasa(team);
        for (SetSpecialist specialist : team) {
            PersonnelActions.assignResearch(planned, specialist.getId(), ROCKET, PACK);
        }
        ResearchPlanning.Projection projection = ResearchPlanning.project(planned, PACK, ROCKET);
        assertEquals(ResearchPlanning.expectedSeasonGain(planned, PACK, ROCKET),
            projection.reliability()[0] - projection.start(), 1e-9);

        int runs = 2_000;
        double total = 0;
        SplittableRandom random = new SplittableRandom(11);
        for (int i = 0; i < runs; i++) {
            List<SetSpecialist> copy = List.of(engineer("a", 70), engineer("b", 40), engineer("c", 20));
            PlayerSnapshot snapshot = nasa(copy);
            for (SetSpecialist specialist : copy) {
                PersonnelActions.assignResearch(snapshot, specialist.getId(), ROCKET, PACK);
            }
            for (int season = 0; season < seasons; season++) {
                ResearchProgress.advanceSeason(snapshot, PACK, random);
            }
            total += snapshot.getMissionComponents().get(ROCKET).getCurrentReliability();
        }
        double expected = projection.reliability()[seasons - 1];
        assertEquals(expected, total / runs, (expected - 10) * 0.03, "within 3 % of the simulated progress");
    }

    /**
     * Verifies an engineer who reaches retirement age drops out of the projection from the season
     * they leave, and is reported as a departure.
     */
    @Test
    void retiringEngineerLeavesTheProjection() {
        SetSpecialist young = engineer("young", 60);
        SetSpecialist old = new SetSpecialist("old", "old", PersonnelLifecycle.SET_RETIREMENT_AGE - 1, 70, 50);
        old.setSkill(SetSkill.ROCKETS, 90);
        old.setAgingProgressSeasons(PersonnelLifecycle.SEASONS_PER_YEAR - 2);
        PlayerSnapshot snapshot = nasa(List.of(young, old));
        PersonnelActions.assignResearch(snapshot, "young", ROCKET, PACK);
        PersonnelActions.assignResearch(snapshot, "old", ROCKET, PACK);
        PlayerSnapshot alone = nasa(List.of(engineer("young", 60)));
        PersonnelActions.assignResearch(alone, "young", ROCKET, PACK);

        ResearchPlanning.Projection projection = ResearchPlanning.project(snapshot, PACK, ROCKET);
        assertEquals(1, projection.departures().size());
        assertEquals("old", projection.departures().get(0).engineer().getId());
        assertEquals(2, projection.departures().get(0).season());
        double first = projection.reliability()[0] - projection.start();
        double second = projection.reliability()[1] - projection.reliability()[0];
        double aloneFirst = ResearchPlanning.expectedSeasonGain(alone, PACK, ROCKET);
        assertTrue(first > aloneFirst, "both work the first season");
        assertTrue(second < aloneFirst, "only the young one works the second, from a higher start");
    }

    /**
     * Verifies a component nobody works on goes nowhere, and a team's seasons to its cap sit inside
     * its random range.
     */
    @Test
    void emptyTeamGoesNowhereAndTheRangeBracketsTheAverage() {
        PlayerSnapshot nobody = nasa(List.of());
        ResearchPlanning.Projection flat = ResearchPlanning.project(nobody, PACK, ROCKET);
        assertFalse(flat.moving());
        assertTrue(flat.seasonsTo(60).isEmpty());
        assertEquals(0, flat.seasonsTo(5).getAsInt());

        List<SetSpecialist> team = List.of(engineer("a", 70), engineer("b", 60));
        PlayerSnapshot snapshot = nasa(team);
        for (SetSpecialist specialist : team) {
            PersonnelActions.assignResearch(snapshot, specialist.getId(), ROCKET, PACK);
        }
        ResearchPlanning.Projection projection = ResearchPlanning.project(snapshot, PACK, ROCKET);
        int average = projection.seasonsTo(60).orElseThrow();
        MissionComponent rocket = PACK.missionComponent(ROCKET).orElseThrow();
        SetSpecialist[] seated = new SetSpecialist[MissionComponent.RD_SLOT_COUNT];
        seated[0] = team.get(0);
        seated[1] = team.get(1);
        ResearchPlanning.Range range = ResearchPlanning.ranges(snapshot, rocket, 10, seated, 60)[0];
        assertTrue(range.low() <= average && average <= range.high(), range + " around " + average);
        assertTrue(projection.seasonsTo(projection.cap()).isPresent(), "the cap is reached within ten years");
    }
}
