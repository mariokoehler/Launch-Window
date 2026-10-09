package de.mkoehler.launchwindow.devtools.analysis;

import de.mkoehler.launchwindow.content.ContentPackLoader;
import de.mkoehler.launchwindow.model.Archetype;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M3's exit criterion as a test: the bot harness, running on the real early-era
 * pack, reaches the declared goals in some games, for both agencies, with every
 * invariant holding ({@code design.md} &sect;5, M3 slice 5).
 *
 * @author Mario Koehler
 */
class ContentPackPlayoutsTest {

    @Test
    void botsReachTheDeclaredGoalsOnTheEarlyEraPack() {
        ContentPackPlayouts harness = new ContentPackPlayouts(
            ContentPackLoader.load(Path.of("..", "assets", "content", "early-era")));

        ContentPackPlayouts.Report report = harness.run(20, 80, 7L);

        assertEquals(20, report.goalReachedCounts().get("earth-orbit-satellite"),
            "every game puts a satellite in orbit: " + report);
        assertTrue(report.completedByArchetype().getOrDefault(Archetype.NASA, 0) > 0,
            "some NASA games reach every declared goal: " + report);
        assertTrue(report.completedByArchetype().getOrDefault(Archetype.SOVIET, 0) > 0,
            "some Soviet games reach every declared goal: " + report);
        assertTrue(report.launches().keySet().containsAll(ContentPackPlayouts.PLANS.get(Archetype.NASA)),
            "every NASA plan step was flown: " + report.launches());
    }

    @Test
    void botsWorkingAheadReachTheMoonOnTheStandardPack() {
        ContentPackPlayouts harness = new ContentPackPlayouts(
            ContentPackLoader.load(Path.of("..", "assets", "content", "standard")), true);

        ContentPackPlayouts.Report report = harness.run(10, 240, 7L);

        assertEquals(ContentPackPlayouts.MOON_DECLARED_GOALS, harness.declaredGoals());
        assertTrue(report.completedByArchetype().getOrDefault(Archetype.NASA, 0) > 0,
            "some NASA games land a crew on the Moon: " + report);
        assertTrue(report.completedByArchetype().getOrDefault(Archetype.SOVIET, 0) > 0,
            "some Soviet games land a crew on the Moon: " + report);
    }
}
