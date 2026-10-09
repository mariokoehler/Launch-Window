package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.ContentPackLoader;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The goal lines on the program and mission cards (play test, 2026-10-09).
 *
 * @author Mario Koehler
 */
class GoalModifiersTest {

    private static final ContentCatalog PACK = ContentPackLoader.load(Path.of("..", "assets", "content", "standard"));

    /** A mission counts every goal it depends on that isn't reached, and every boost that is. */
    @Test
    void aMissionCountsItsMissingGoals() {
        var orbital = PACK.missionConfiguration("vostok-orbital").orElseThrow();
        GoalModifiers.Summary none = GoalModifiers.of(orbital, goal -> false);
        assertEquals(orbital.goalDependencies().stream().mapToInt(d -> d.amount()).sum(), none.missing());
        assertEquals("Goals not reached: −" + none.missing() + " reliability", none.missingText(false));

        GoalModifiers.Summary all = GoalModifiers.of(orbital, goal -> true);
        assertEquals(0, all.missing());
        assertTrue(all.missingText(false) == null);
    }

    /**
     * A program counts only goals from outside it, at the most any one of its missions loses: Apollo
     * flown with nothing before it is a long way ahead.
     */
    @Test
    void aProgramCountsOnlyGoalsFromOutside() {
        var apollo = PACK.program("apollo").orElseThrow();
        GoalModifiers.Summary skipped = GoalModifiers.ofProgram(apollo, PACK, goal -> false);
        assertTrue(skipped.missing() > 0);
        assertTrue(skipped.missingLines().stream().anyMatch(line -> line.goalId().equals("human-in-orbit")));
        assertTrue(skipped.missingLines().stream().noneMatch(line -> line.goalId().equals("apollo-flyby-flown")),
            "Apollo's own flights are the way in, not a skipped step");

        Set<String> earlier = Set.of("human-in-orbit", "duration-level-1", "apollo-uncrewed-orbital-flown",
            "apollo-orbital-flown", "apollo-uncrewed-suborbital-flown", "crewed-lunar-orbit", "crewed-lunar-flyby",
            "apollo-uncrewed-lunar-module-test-flown", "apollo-lunar-module-test-flown");
        assertTrue(GoalModifiers.ofProgram(apollo, PACK, earlier::contains).missing() < skipped.missing());
    }
}
