package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.FrontPages;
import de.mkoehler.launchwindow.content.InvestigationCauses;
import de.mkoehler.launchwindow.model.FrontPage;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ReportPrinter} ({@code design.md} &sect;2.14(b)).
 *
 * @author Mario Koehler
 */
class ReportPrinterTest {

    private static final FrontPages PAGES = FrontPages.load(Path.of("..", "assets", "content", FrontPages.FILE));
    private static final InvestigationCauses WORDS = InvestigationCauses.load(Path.of("..", "assets", "content",
        InvestigationCauses.FILE));
    private static final MemorialPrinter.Agency ORION = new MemorialPrinter.Agency("k-orion", "Orion Spaceworks",
        "Clara Voss");
    private static final MemorialPrinter.Agency KESTREL = new MemorialPrinter.Agency("k-kestrel", "Kestrel Aerospace",
        "Moses Okafor");

    @Test
    void aBoardsReportIsPrintedForEveryone() {
        TurnReport issued = new TurnReport(TurnReport.VERSION, 48, 0, 0, 0, 0, List.of(), List.of(), List.of(
            new OfficialReportIssued("m7", "gemini-spacewalk", "18-reentry", 44, "craft-shield-seam",
                "factor-schedule", "gemini-capsule", List.of("a1", "a2"), List.of("Ana Reyes", "Lev Marek"))), null);
        TurnReport quiet = new TurnReport(TurnReport.VERSION, 48, 0, 0, 0, 0, List.of(), List.of(), List.of(), null);

        List<FrontPage> pages = ReportPrinter.print(48, 77L, List.of(
            new MemorialPrinter.Turn(ORION, issued, List.of()), new MemorialPrinter.Turn(KESTREL, quiet, List.of())),
            WORDS, PAGES);

        assertEquals(1, pages.size());
        FrontPage page = pages.getFirst();
        assertTrue(page.reportPage());
        assertEquals("48-report-1", page.id());
        assertEquals("Orion Spaceworks", page.achievers().getFirst().agencyName());
        assertEquals("craft-shield-seam", page.report().causeId());
        assertEquals(44, page.report().loss().gameTurn());
        assertEquals(List.of("Ana Reyes", "Lev Marek"), page.report().loss().crew().stream().map(FrontPage.Lost::name)
            .toList());
        assertTrue(WORDS.boardStory(page.storyId()).isPresent());
        assertEquals(pages, ReportPrinter.print(48, 77L, List.of(new MemorialPrinter.Turn(ORION, issued, List.of()),
            new MemorialPrinter.Turn(KESTREL, quiet, List.of())), WORDS, PAGES), "a retry prints the same page");
    }
}
