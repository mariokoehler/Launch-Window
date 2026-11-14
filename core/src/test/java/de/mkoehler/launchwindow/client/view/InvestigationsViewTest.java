package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.ContentPackLoader;
import de.mkoehler.launchwindow.content.InvestigationCauses;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.FrontPage;
import de.mkoehler.launchwindow.model.Incident;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.rules.InvestigationConcluded;
import de.mkoehler.launchwindow.rules.InvestigationOffered;
import de.mkoehler.launchwindow.rules.TurnReport;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link InvestigationsView} ({@code ui-spec.md} &sect;4.24): the report's lines, the offers still open,
 * and a board's page filled from the words.
 *
 * @author Mario Koehler
 */
class InvestigationsViewTest {

    private static final InvestigationCauses WORDS = InvestigationCauses.load(Path.of("..", "assets", "content",
        InvestigationCauses.FILE));
    private static final ContentCatalog STANDARD = ContentPackLoader.load(Path.of("..", "assets", "content",
        "standard"));

    @Test
    void aFoundCauseTellsItsFindingAndWhatCameBack() {
        TurnReport report = new TurnReport(TurnReport.VERSION, 22, 0, 0, 0, 0, List.of(), List.of(), List.of(
            new InvestigationOffered("m5", "explorer-1-orbit", "04-ascent", 9.9, 24),
            new InvestigationConcluded("m2", "explorer-1-orbit", "03-launch", "e2", "Mikhail Brandt", true,
                "rocket-turbopump-bearing", "jupiter-c",
                List.of(new InvestigationConcluded.Recovery("jupiter-c", 4.1)), 5)), null);

        List<InvestigationsView.ReportLine> lines = InvestigationsView.reportLines(report, STANDARD, WORDS);

        assertEquals(InvestigationsView.Kind.OFFERED, lines.get(0).kind());
        InvestigationsView.ReportLine found = lines.get(1);
        assertEquals(InvestigationsView.Kind.FOUND, found.kind());
        assertEquals("Cause found: a seized turbopump bearing", found.title());
        assertFalse(found.text().contains("{component}"));
        assertEquals("+4.1", found.value());
    }

    @Test
    void anOfferFromAnEarlierTurnStaysOnTheLatestReport() {
        PlayerSnapshot state = new PlayerSnapshot("Orion", Archetype.NASA, 1_000, 0, 0, 1);
        state.getIncidents().add(new Incident("m5", Incident.Kind.AGENCY, 22, "explorer-1-orbit", "04-ascent",
            List.of(new Incident.Part("jupiter-c", 70, 7.3)), "jupiter-c", List.of(), List.of(), null, null, null,
            0, 0, 0, 0));

        assertEquals(1, InvestigationsView.stillOpen(state, List.of(), STANDARD).size());
        assertTrue(InvestigationsView.stillOpen(state, List.of(new InvestigationsView.ReportLine(
            InvestigationsView.Kind.OFFERED, "m5", "", "", "", "")), STANDARD).isEmpty(), "told once");
    }

    @Test
    void aBoardsPageIsFilledFromItsStory() {
        FrontPage.Loss loss = new FrontPage.Loss("m4", 22, "k", "Orion Spaceworks", "Clara Voss", "gemini-spacewalk",
            "04-ascent", List.of(new FrontPage.Lost("Ana Reyes", 38, null, "a1", 2, 0, 28, List.of())), List.of(),
            List.of());
        FrontPage page = new FrontPage("26-report-1", "", 26, "board-names-cause", "Fair", List.of(), List.of(),
            List.of(), FrontPage.Kind.REPORT, null, List.of(), null,
            new FrontPage.Report(loss, "craft-stuck-vent", "factor-earlier-signs", "gemini-capsule"));

        MemorialView.Page words = InvestigationsView.reportPage(page, WORDS, STANDARD).orElseThrow();

        assertTrue(words.headline().startsWith("BOARD NAMES CAUSE OF"), words.headline());
        assertFalse(words.headline().contains("{"));
        assertTrue(words.deck().contains("a cabin vent that stuck open"), words.deck());
        assertEquals(4, words.article().size());
        assertTrue(words.close().contains("Orion Spaceworks"));
        assertTrue(words.all().stream().noneMatch(text -> text.contains("{")));
    }
}
