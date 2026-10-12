package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.BudgetSchedule;
import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.Goal;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.content.MissionSeason;
import de.mkoehler.launchwindow.content.MissionStep;
import de.mkoehler.launchwindow.content.Program;
import de.mkoehler.launchwindow.content.ProgramCategory;
import de.mkoehler.launchwindow.content.TestBuildings;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.GoalRecord;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSkill;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link RivalFirsts}, the answer to a rival's First ({@code design.md} &sect;2.1(e)): which
 * Firsts count, the credit they add, and how the next funding review reads and clears it.
 *
 * @author Mario Koehler
 */
class RivalFirstsTest {

    private static final ProgramCategory CATEGORY = new ProgramCategory("early-space", "Early Space");
    private static final MissionComponent CAPSULE = new MissionComponent("capsule", SetSkill.CREWED_SPACECRAFT,
        3, 90, 82, List.of(3.0, 6.0, 9.0, 12.0), 0, List.of(), 0);
    private static final Program SHARED = new Program("shared", "early-space", 1_000, 0, List.of("capsule"), List.of());
    private static final Program SOVIET_ONLY = new Program("soviet-only", "early-space", 1_000, 0, List.of("capsule"),
        List.of(), List.of(), Archetype.SOVIET);
    private static final Goal ORBIT = new Goal("orbit", 3_000, 1_500, List.of("space"));
    private static final Goal SPACE = new Goal("space", 1_750, 875, List.of());
    private static final Goal NOTHING = new Goal("nothing", 0, 0, List.of());
    private static final Goal SOVIET_FLOWN = new Goal("soviet-flown", 1_600, 800, List.of());
    private static final ContentCatalog CATALOG = new ContentCatalog(List.of(CATEGORY), List.of(SHARED, SOVIET_ONLY),
        List.of(configuration("orbit-flight", "shared", List.of("orbit", "nothing")),
            configuration("soviet-flight", "soviet-only", List.of("soviet-flown"))),
        List.of(CAPSULE), List.of(ORBIT, SPACE, NOTHING, SOVIET_FLOWN), TestBuildings.withoutMaintenance());

    private static MissionConfiguration configuration(String id, String program, List<String> goals) {
        return new MissionConfiguration(id, program, List.of(new MissionSeason(List.of(new MissionStep("s1",
            List.of(new MissionStep.ComponentInvolvement("capsule", 100)), List.of(), List.of(), false, 0, 0,
            goals)))), 0, 0, 0, 0);
    }

    private static PlayerSnapshot player(Archetype archetype) {
        return new PlayerSnapshot("Agency", archetype, 10_000, 2_000, 300, 1);
    }

    @Test
    void reachableGoalsFollowTheProgramsOpenToAnAgencyAndTheirCascades() {
        assertEquals(java.util.Set.of("orbit", "space", "nothing"), CATALOG.reachableGoals(Archetype.NASA));
        assertEquals(java.util.Set.of("orbit", "space", "nothing", "soviet-flown"),
            CATALOG.reachableGoals(Archetype.SOVIET));
    }

    @Test
    void theFirstsOfATurnComeFromEveryPlayersGoalRecords() {
        PlayerSnapshot first = player(Archetype.SOVIET);
        first.setGoalRecords(List.of(new GoalRecord("orbit", 7, "orbit-flight", null, true, 3_000),
            new GoalRecord("space", 7, "orbit-flight", "orbit", true, 1_750),
            new GoalRecord("nothing", 6, "orbit-flight", null, true, 0)));
        PlayerSnapshot second = player(Archetype.NASA);
        second.setGoalRecords(List.of(new GoalRecord("orbit", 7, "orbit-flight", null, true, 3_000),
            new GoalRecord("soviet-flown", 7, "soviet-flight", null, false, 800)));

        assertEquals(List.of("orbit", "space"), RivalFirsts.settledFirstOn(List.of(first, second), 7));
        assertEquals(List.of("nothing"), RivalFirsts.settledFirstOn(List.of(first, second), 6));
    }

    /**
     * Half of each counted First's first-time prestige: the cascade's goals count on their own, a goal
     * with no prestige, one the player holds and one their agency can't fly don't.
     */
    @Test
    void onlyFirstsThePlayerWasRacingForAddToTheCredit() {
        PlayerSnapshot nasa = player(Archetype.NASA);
        nasa.setAchievedGoals(Map.of("space", 3));

        List<ResolutionEvent> events = RivalFirsts.advanceSeason(nasa, CATALOG,
            List.of("orbit", "space", "nothing", "soviet-flown"));

        long credit = Math.round(3_000 * RivalFirsts.SHARE);
        assertEquals(List.of(new RivalFirstsAnswered(List.of("orbit"), credit, credit)), events);
        assertEquals(credit, nasa.getRivalFirstsCredit());

        RivalFirsts.advanceSeason(nasa, CATALOG, List.of("soviet-flown"));
        assertEquals(credit, nasa.getRivalFirstsCredit(), "another agency's own program never counts");
        assertTrue(RivalFirsts.advanceSeason(nasa, CATALOG, List.of()).isEmpty());
    }

    /**
     * The credit shifts the review's bracket down like extra prestige, then clears; the baseline the
     * next review's catch-up shift reads stays the player's own prestige.
     */
    @Test
    void theNextReviewCountsTheCreditOnceAndKeepsTheBaselineRaw() {
        BudgetSchedule schedule = new BudgetSchedule(1_000, 0, 50, 4, List.of(
            new BudgetSchedule.Review(0, 100, 1_000, 200),
            new BudgetSchedule.Review(2_000, 300, 4_000, 600)));
        PlayerSnapshot snapshot = player(Archetype.NASA);
        snapshot.setPrestigeAtLastReview(500);
        snapshot.setRivalFirstsCredit(1_000);
        snapshot.setSeason(32);

        BudgetProgress.ReviewForecast forecast = BudgetProgress.forecast(snapshot, schedule);
        List<ResolutionEvent> events = BudgetProgress.advanceSeason(snapshot, TestBuildings.catalogWithoutUpkeep(
            List.of(), List.of(), List.of(), List.of()), schedule, 32);

        // 2,000 prestige plus 1,000 of credit reads as 3,000: halfway between 300 and 600.
        assertEquals(new BudgetReviewed(2, 2_000, 0, 450, 1_000), events.get(0));
        assertEquals(450, forecast.payToday(), "the forecast matches the review");
        assertEquals(1_000, forecast.rivalCredit());
        assertEquals(1_000, forecast.minPrestige(), "the bracket's ends move down by the credit");
        assertEquals(0, snapshot.getRivalFirstsCredit());
        assertEquals(2_000, snapshot.getPrestigeAtLastReview());
    }
}
