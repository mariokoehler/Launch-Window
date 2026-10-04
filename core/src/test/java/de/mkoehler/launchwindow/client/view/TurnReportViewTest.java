package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.ContentPackLoader;
import de.mkoehler.launchwindow.rules.BudgetProgress;
import de.mkoehler.launchwindow.rules.GoalSettled;
import de.mkoehler.launchwindow.rules.MissionStepOutcome;
import de.mkoehler.launchwindow.rules.MissionStepResolved;
import de.mkoehler.launchwindow.rules.SeasonFundsSettled;
import de.mkoehler.launchwindow.rules.TurnReport;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The turn report's wording: the budget adds up, cascade goals read as credited, and a mission
 * that ended on a failure counts as failed in the glance strip.
 */
class TurnReportViewTest {

    private static final ContentCatalog PACK = ContentPackLoader.load(Path.of("..", "assets", "content", "early-era"));

    private static TurnReport report() {
        BudgetProgress.RunningCosts costs = new BudgetProgress.RunningCosts(900, 12, 300, 2, 150, 3, 250, 1_600);
        return new TurnReport(TurnReport.VERSION, 5, 300, 340, 20_000, 21_400,
            List.of(), List.of(
                new TurnReport.MissionFlown("m-1", "vostok-orbital", 0, 1, true, List.of()),
                new TurnReport.MissionFlown("m-2", "sputnik-1-extended", 0, 2, false, List.of())),
            List.of(
                step("m-1", "launch", MissionStepOutcome.SUCCESSFUL),
                step("m-1", "orbit", MissionStepOutcome.FAILURE),
                step("m-2", "launch", MissionStepOutcome.SUCCESSFUL),
                new GoalSettled("m-2", "goal-satellite", false, 30, true),
                new GoalSettled("m-2", "goal-object-in-orbit", true, 10, false),
                new SeasonFundsSettled(1_600, 3_000, 21_400, costs)));
    }

    private static MissionStepResolved step(String mission, String stepId, MissionStepOutcome outcome) {
        return new MissionStepResolved(mission, "config", 0, stepId, outcome, List.of(), 60, true, null, null,
            null, 0);
    }

    @Test
    void budgetLinesAddUpToTheNet() {
        TurnReport report = report();
        long sum = TurnReportView.budget(report).stream().mapToLong(TurnReportView.BudgetLine::amount).sum();
        assertEquals(3_000 - 1_600, sum);
        assertEquals(sum, TurnReportView.net(report));
    }

    @Test
    void cascadeGoalsReadAsCredited() {
        List<TurnReportView.GoalLine> goals = TurnReportView.goals(report());
        assertEquals(2, goals.size());
        assertTrue(goals.get(0).first());
        assertTrue(goals.get(0).how().startsWith("reached"), goals.get(0).how());
        assertTrue(goals.get(1).how().contains("credited"), goals.get(1).how());
    }

    @Test
    void aMissionEndingOnAFailureCountsAsFailed() {
        TurnReportView.Glance missions = TurnReportView.glance(report(), PACK).stream()
            .filter(glance -> glance.label().equals("Missions")).findFirst().orElseThrow();
        assertEquals("2 flown", missions.value());
        assertEquals("1 failed", missions.note());
        assertEquals(-1, missions.tone());
    }
}
