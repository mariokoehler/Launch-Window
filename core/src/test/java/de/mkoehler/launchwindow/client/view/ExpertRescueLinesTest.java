package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.rules.ExpertRescueApplied;
import de.mkoehler.launchwindow.rules.PersonnelGroup;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link ExpertRescueLines}.
 *
 * @author Mario Koehler
 */
class ExpertRescueLinesTest {

    private static ExpertRescueApplied rescue(String name, double boost, double before, double after) {
        return new ExpertRescueApplied("m1", "s1", PersonnelGroup.MISSION_CONTROL, name, name, "PROPULSION", boost,
            before, after);
    }

    /** Verifies figures past the cap read as the cap, and an Expert past it adds nothing. */
    @Test
    void holdsTheLinesToTheCap() {
        List<String> lines = ExpertRescueLines.of(List.of(rescue("A", 50, 60, 80), rescue("B", 80, 80, 96),
            rescue("C", 80, 96, 99.2)));

        assertTrue(lines.get(0).endsWith("raised the saving roll from 60% to 80%."), lines.get(0));
        assertTrue(lines.get(1).endsWith("raised the saving roll from 80% to 95%."), lines.get(1));
        assertTrue(lines.get(2).endsWith("couldn't add anything: the saving roll is already at its 95% limit."),
            lines.get(2));
    }

    /** Verifies an Expert with no uses left this turn says so, wherever the roll stands. */
    @Test
    void anExhaustedExpertHasNothingLeft() {
        assertTrue(ExpertRescueLines.of(List.of(rescue("A", 0, 60, 60))).get(0).endsWith(
            "had nothing left to give this turn."));
    }

    /** Verifies no Expert means no lines. */
    @Test
    void noRescuesNoLines() {
        assertEquals(List.of(), ExpertRescueLines.of(List.of()));
    }
}
