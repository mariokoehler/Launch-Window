package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.ContentPackLoader;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.rules.FindingLearned;
import de.mkoehler.launchwindow.rules.FindingPublished;
import de.mkoehler.launchwindow.rules.GoalSettled;
import de.mkoehler.launchwindow.rules.TurnReport;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests what the screens say about findings ({@code design.md} &sect;2.13).
 *
 * @author Mario Koehler
 */
class FindingsViewTest {

    private static final ContentCatalog STANDARD = ContentPackLoader.load(Path.of("..", "assets", "content",
        "standard"));

    /**
     * What a finding makes safer reads as words, a/b variants and repeats folded.
     */
    @Test
    void saferReadsAsWords() {
        assertEquals("crewed suborbital flight, orbit insertion and reentry around Earth",
            FindingsView.safer(STANDARD, "organisms-in-microgravity"));
        assertEquals("crewed long stays in orbit", FindingsView.safer(STANDARD, "amphibians-in-microgravity"));
        assertEquals("landing on flights to Mars", FindingsView.safer(STANDARD, "mars-mapping"));
        assertEquals("crewed Earth orbit, high orbit and trans-lunar injection",
            FindingsView.safer(STANDARD, "magnetic-field-study"));
        assertEquals("interplanetary cruise", FindingsView.safer(STANDARD, "solar-wind-measurements"));
        assertEquals("", FindingsView.safer(STANDARD, "human-in-orbit"));
    }

    /**
     * A finding's standing follows the player's state.
     */
    @Test
    void statusFollowsTheState() {
        PlayerSnapshot state = new PlayerSnapshot("Vega", Archetype.NASA, 0, 0, 0, 1);
        assertEquals(FindingsView.Status.UNKNOWN, FindingsView.status(state, "mars-mapping"));
        state.getAchievedGoals().put("mars-mapping", 4);
        assertEquals(FindingsView.Status.CLASSIFIED, FindingsView.status(state, "mars-mapping"));
        state.getPendingPublications().add("mars-mapping");
        assertEquals(FindingsView.Status.QUEUED, FindingsView.status(state, "mars-mapping"));
        state.getPublicFindings().put("mars-mapping", 5);
        assertEquals(FindingsView.Status.PUBLIC, FindingsView.status(state, "mars-mapping"));
        state.getPublishedFindings().add("mars-mapping");
        assertEquals(FindingsView.Status.PUBLISHED_BY_YOU, FindingsView.status(state, "mars-mapping"));
        assertEquals(List.of("mars-mapping"), FindingsView.known(state, STANDARD));
    }

    /**
     * The report's lines: a finding earned, the agency's own publication and another agency's.
     */
    @Test
    void reportLines() {
        TurnReport report = new TurnReport(TurnReport.VERSION, 7, 0, 0, 0, 0, List.of(), List.of(), List.of(
            new GoalSettled("m1", "mars-mapping", false, 1300, true),
            new GoalSettled("m1", "mars-orbit", false, 0, true),
            new FindingPublished("lunar-mapping", 176, 2, 3, List.of()),
            new FindingLearned("venus-atmosphere-study", List.of("Vega", "Orion"), false)));
        List<FindingsView.ReportLine> lines = FindingsView.reportLines(report, STANDARD);
        assertEquals(3, lines.size());
        assertTrue(lines.get(0).earned());
        assertEquals("You published " + ContentNames.of("lunar-mapping"), lines.get(1).title());
        assertEquals("It helped 2 of your 3 rivals.", lines.get(1).text());
        assertEquals(176, lines.get(1).prestige());
        assertEquals("Vega and Orion published " + ContentNames.of("venus-atmosphere-study"), lines.get(2).title());
    }
}
