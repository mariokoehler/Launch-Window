package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.FrontPages;
import de.mkoehler.launchwindow.content.Theories;
import de.mkoehler.launchwindow.model.FrontPage;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link SciencePrinter} ({@code ui-spec.md} &sect;4.18.2).
 *
 * @author Mario Koehler
 */
class SciencePrinterTest {

    private static final FrontPages PAGES = FrontPages.load(Path.of("..", "assets", "content", FrontPages.FILE));
    private static final Theories THEORIES = Theories.load(Path.of("..", "assets", "content", Theories.FILE));
    private static final long SEED = 99L;

    @Test
    void aFirstAnsweringGoalSettlesItsTheoryForEveryone() {
        List<FrontPagePrinter.Agency> agencies = List.of(
            agency("Orion Spaceworks", settled("lunar-lander", true)),
            agency("Kestrel Aerospace", settled("lunar-soft-landing", false)),
            agency("Baltic Star Agency", null));
        List<FrontPage> pages = SciencePrinter.print(30, SEED, agencies, THEORIES, Set.of(), PAGES, g -> true);
        assertEquals(1, pages.size());
        FrontPage page = pages.get(0);
        assertEquals("30-science-deep-dust", page.id());
        assertEquals(FrontPage.Kind.SCIENCE, page.kind());
        assertTrue(page.science());
        assertEquals("deep-dust", page.storyId());
        assertEquals("lunar-lander", page.goalId());
        assertEquals(List.of("Orion Spaceworks"), page.achievers().stream().map(FrontPage.Achiever::agencyName).toList());
        assertFalse(page.tells("lunar-lander"), "a science page tells no First");
        assertFalse(page.firstPage());
    }

    @Test
    void aTheoryIsSettledOnce() {
        List<FrontPagePrinter.Agency> agencies = List.of(agency("Orion Spaceworks",
            settled("crewed-lunar-landing", true)));
        List<FrontPage> pages = SciencePrinter.print(60, SEED, agencies, THEORIES, Set.of("deep-dust"), PAGES,
            g -> true);
        assertTrue(pages.stream().noneMatch(p -> p.storyId().equals("deep-dust")), "settled by the probe already");
        assertTrue(pages.stream().anyMatch(p -> p.storyId().equals("moon-germs")));
    }

    @Test
    void twoAgenciesInOneTurnShareThePageAndAPackWithoutTheGoalsPrintsNothing() {
        List<FrontPagePrinter.Agency> agencies = List.of(
            agency("Orion Spaceworks", settled("mars-probe", true)),
            agency("Kestrel Aerospace", settled("mars-probe", true)));
        FrontPage page = SciencePrinter.print(44, SEED, agencies, THEORIES, Set.of(), PAGES, g -> true).get(0);
        assertEquals(List.of("Kestrel Aerospace", "Orion Spaceworks"),
            page.achievers().stream().map(FrontPage.Achiever::agencyName).toList());
        assertEquals("mars-probe", page.goalId());
        assertEquals(SciencePrinter.print(44, SEED, agencies, THEORIES, Set.of(), PAGES, g -> true),
            List.of(page), "the same pages every time");
        assertTrue(SciencePrinter.print(44, SEED, agencies, THEORIES, Set.of(), PAGES,
            g -> !g.equals("lunar-probe")).isEmpty(), "Mars is raised by lunar-probe, which this pack lacks");
    }

    private static FrontPagePrinter.Agency agency(String name, TurnReport report) {
        return new FrontPagePrinter.Agency(name, "Director of " + name, report);
    }

    private static TurnReport settled(String goal, boolean firstInGame) {
        return new TurnReport(TurnReport.VERSION, 30, 0, 0, 0, 0, List.of(), List.of(),
            List.of(new GoalSettled("m1", goal, false, 100, firstInGame)), null);
    }
}
