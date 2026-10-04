package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.ContentPackLoader;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the goal cascade against the real early-era pack. No step there names "Human in space"
 * or "Human in orbit"; both are earned, with their prestige, through the cascade of the crewed
 * flight goals that cancel them.
 *
 * @author Mario Koehler
 */
class GoalCascadeTest {

    private static final ContentCatalog PACK = ContentPackLoader.load(Path.of("..", "assets", "content", "early-era"));

    private static PlayerSnapshot soviet() {
        return new PlayerSnapshot("Vega", Archetype.SOVIET, 25_000, 250, 3_000, 1);
    }

    /**
     * The first crewed suborbital flight credits "Human in space" and its 1,750 prestige.
     */
    @Test
    void suborbitalFlightCreditsHumanInSpace() {
        PlayerSnapshot state = soviet();
        assertEquals(List.of("vostok-suborbital-flown", "vostok-uncrewed-suborbital-flown", "human-in-space"),
            GoalCascade.unheld("vostok-suborbital-flown", state, PACK));
        assertEquals(600 + 120 + 1_750, GoalCascade.maxPrestige("vostok-suborbital-flown", state, PACK));
    }

    /**
     * The first crewed orbital flight credits "Human in orbit" (3,000) and, through it, "Human in
     * space" and the satellite goals, each once.
     */
    @Test
    void orbitalFlightCreditsHumanInOrbitAndEverythingBelowIt() {
        List<String> cascade = GoalCascade.unheld("vostok-orbital-flown", soviet(), PACK);
        assertEquals("vostok-orbital-flown", cascade.get(0));
        assertTrue(cascade.containsAll(List.of("human-in-orbit", "human-in-space", "earth-orbit-satellite",
            "radio-signals-from-orbit")), cascade.toString());
        assertEquals(cascade.size(), cascade.stream().distinct().count(), "each goal once");
    }

    /**
     * Goals the player holds, achieved or credited, drop out; a held goal settles nothing.
     */
    @Test
    void heldGoalsDropOut() {
        PlayerSnapshot state = soviet();
        state.getCancelledGoalIds().add("human-in-space");
        assertEquals(List.of("vostok-suborbital-flown", "vostok-uncrewed-suborbital-flown"),
            GoalCascade.unheld("vostok-suborbital-flown", state, PACK));
        state.getAchievedGoals().put("vostok-suborbital-flown", 5);
        assertEquals(List.of(), GoalCascade.unheld("vostok-suborbital-flown", state, PACK));
        assertEquals(0, GoalCascade.maxPrestige("vostok-suborbital-flown", state, PACK));
    }
}
