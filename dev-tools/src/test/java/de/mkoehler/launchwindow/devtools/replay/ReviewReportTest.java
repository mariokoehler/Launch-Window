package de.mkoehler.launchwindow.devtools.replay;

import de.mkoehler.launchwindow.client.view.TurnReportView;
import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.ContentPackLoader;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.content.MissionStep;
import de.mkoehler.launchwindow.rules.MissionStepOutcome;
import de.mkoehler.launchwindow.rules.MissionStepResolved;
import de.mkoehler.launchwindow.rules.TurnReport;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The replayer's made-up report: the plan's steps in order, the outcome where asked, the rest
 * successful, and real components where the plan names a rocket slot.
 */
class ReviewReportTest {

    private static final ContentCatalog CATALOG = ContentPackLoader.load(
        java.nio.file.Path.of("..", "assets", "content", "standard"));

    private static MissionConfiguration longest() {
        return CATALOG.missionConfigurations().stream()
            .max(java.util.Comparator.comparingInt(c -> c.seasons().get(0).steps().size())).orElseThrow();
    }

    @Test
    void everyStepGetsTheOutcomeAndTheSeasonPlaysOn() {
        MissionConfiguration config = longest();
        TurnReport report = ReviewReport.build(CATALOG, config, 0, MissionStepOutcome.FAILURE, -1);
        List<MissionStepResolved> steps = TurnReportView.steps(report, ReviewReport.MISSION_ID);
        List<String> plan = config.seasons().get(0).steps().stream().map(MissionStep::id).toList();
        assertEquals(plan, steps.stream().map(MissionStepResolved::stepId).toList());
        assertTrue(steps.stream().allMatch(step -> step.outcome() == MissionStepOutcome.FAILURE));
        assertTrue(steps.stream().noneMatch(MissionStepResolved::firstRollPassed));
    }

    @Test
    void aFailureOnOneStepEndsTheSeasonThere() {
        MissionConfiguration config = longest();
        TurnReport report = ReviewReport.build(CATALOG, config, 0, MissionStepOutcome.CATASTROPHIC, 2);
        List<MissionStepResolved> steps = TurnReportView.steps(report, ReviewReport.MISSION_ID);
        assertEquals(3, steps.size());
        assertEquals(MissionStepOutcome.SUCCESSFUL, steps.get(1).outcome());
        assertEquals(MissionStepOutcome.CATASTROPHIC, steps.get(2).outcome());
        assertTrue(report.missions().get(0).ended());
    }

    @Test
    void aSavedGlitchPassesItsSavingRoll() {
        TurnReport report = ReviewReport.build(CATALOG, longest(), 0, MissionStepOutcome.MINOR_GLITCH_SOLVED, 0);
        MissionStepResolved step = TurnReportView.steps(report, ReviewReport.MISSION_ID).get(0);
        assertFalse(step.firstRollPassed());
        assertEquals(Boolean.TRUE, step.savingRollPassed());
    }

    @Test
    void rocketSlotsFlyARealRocket() {
        for (MissionConfiguration config : CATALOG.missionConfigurations()) {
            TurnReport report = ReviewReport.build(CATALOG, config, 0, MissionStepOutcome.SUCCESSFUL, -1);
            for (MissionStepResolved step : TurnReportView.steps(report, ReviewReport.MISSION_ID)) {
                for (MissionStepResolved.ComponentFlown component : step.components()) {
                    assertTrue(CATALOG.missionComponent(component.componentId()).isPresent(),
                        config.id() + " " + step.stepId() + " flies " + component.componentId());
                }
            }
        }
    }
}
