package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.FrontPages;
import de.mkoehler.launchwindow.content.MemorialPages;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.CareerEntry;
import de.mkoehler.launchwindow.model.FrontPage;
import de.mkoehler.launchwindow.model.MemorialEntry;
import de.mkoehler.launchwindow.model.Trait;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link MemorialPrinter} ({@code ui-spec.md} &sect;4.18.3).
 *
 * @author Mario Koehler
 */
class MemorialPrinterTest {

    private static final FrontPages PAGES = FrontPages.load(Path.of("..", "assets", "content", FrontPages.FILE));
    private static final MemorialPages WORDS = MemorialPages.load(Path.of("..", "assets", "content", MemorialPages.FILE));
    private static final long SEED = 77L;

    private static final MemorialPrinter.Agency ORION = new MemorialPrinter.Agency("k-orion", "Orion Spaceworks",
        "Clara Voss");
    private static final MemorialPrinter.Agency KESTREL = new MemorialPrinter.Agency("k-kestrel", "Kestrel Aerospace",
        "Moses Okafor");
    private static final MemorialPrinter.Agency POLARIS = new MemorialPrinter.Agency("k-polaris", "Polaris Space Agency",
        "Ruth Calder");

    /** A crew lost on reentry, after a spacewalk that would have been first: the notice tells all of it. */
    @Test
    void aLostCrewIsRecordedFromTheTurnsReport() {
        TurnReport report = new TurnReport(TurnReport.VERSION, 44, 0, 0, 0, 0, List.of(), List.of(), List.of(
            departure("a1", "Ana Reyes", 38), departure("a2", "Lev Marek", 32),
            new CrewedFlightsGrounded("m7", "gemini-spacewalk", 4),
            new GoalsWithheld("m7", "gemini-spacewalk", List.of("gemini-spacewalk-flown", "spacewalk"),
                List.of("spacewalk"))), null);
        List<MemorialEntry> memorial = List.of(
            entry("a1", "Ana Reyes", 38, 16, List.of(Trait.MENTOR), 2),
            entry("a2", "Lev Marek", 32, 40, List.of(), 1));
        List<FrontPage.Loss> losses = MemorialPrinter.losses(44, List.of(
            new MemorialPrinter.Turn(ORION, report, memorial),
            new MemorialPrinter.Turn(KESTREL, new TurnReport(TurnReport.VERSION, 44, 0, 0, 0, 0, List.of(), List.of(),
                List.of(), null), List.of())));

        assertEquals(1, losses.size());
        FrontPage.Loss loss = losses.get(0);
        assertEquals("44-loss-1", loss.id());
        assertEquals("k-orion", loss.rivalKey());
        assertEquals("gemini-spacewalk", loss.missionConfigurationId());
        assertEquals("18-reentry", loss.stepId());
        assertEquals(List.of("spacewalk"), loss.firstGoalIds());
        FrontPage.Lost ana = loss.crew().get(0);
        assertEquals("Ana Reyes", ana.name());
        assertEquals(2, ana.flights(), "the fatal flight counts");
        assertEquals(28, ana.seasons());
        assertEquals(List.of("MENTOR"), ana.traits());
        assertEquals(1, loss.crew().get(1).flights());
    }

    /**
     * The agency's tone picks a story of that tone and its words are kept; rivals' condolences are told, quotes
     * first, the losing agency's own never; a page without a statement prints the spokesman's.
     */
    @Test
    void thePageTellsTheStatementAndTheCondolences() {
        FrontPage.Loss loss = loss();
        Map<String, List<FrontPage.Statement>> statements = Map.of(
            "k-orion", List.of(new FrontPage.Statement(loss.id(), FrontPage.Stance.PEOPLE, "Lev wrote home every day.")),
            "k-kestrel", List.of(new FrontPage.Statement(loss.id(), FrontPage.Stance.CONDOLENCES, "")),
            "k-polaris", List.of(new FrontPage.Statement(loss.id(), FrontPage.Stance.TRIBUTE, "We will not forget.")));
        FrontPage page = MemorialPrinter.print(45, SEED, List.of(loss), statements, List.of(ORION, KESTREL, POLARIS),
            WORDS, PAGES).get(0);

        assertEquals("45-memorial-44-loss-1", page.id());
        assertTrue(page.memorialPage());
        assertEquals(FrontPage.Stance.PEOPLE, page.memorial().tone());
        assertEquals("Lev wrote home every day.", page.memorial().quote());
        assertTrue(page.storyId().startsWith("m-people-"), page.storyId());
        assertEquals(List.of("Polaris Space Agency", "Kestrel Aerospace"),
            page.reactions().stream().map(FrontPage.Reaction::agencyName).toList(), "quotes first");
        assertEquals("m-tribute-q-1", page.reactions().get(0).lineId());
        assertEquals("m-condolences-1", page.reactions().get(1).lineId());
        assertEquals(List.of("Orion Spaceworks"), page.achievers().stream().map(FrontPage.Achiever::agencyName).toList());
        assertEquals(page, MemorialPrinter.print(45, SEED, List.of(loss), statements, List.of(ORION, KESTREL, POLARIS),
            WORDS, PAGES).get(0), "the same page every time");

        FrontPage silent = MemorialPrinter.print(45, SEED, List.of(loss), Map.of(
            // A condolence from the agency itself, and a First's stance from a rival: neither counts.
            "k-orion", List.of(new FrontPage.Statement(loss.id(), FrontPage.Stance.TRIBUTE, "")),
            "k-kestrel", List.of(new FrontPage.Statement(loss.id(), FrontPage.Stance.POLITE, ""))),
            List.of(ORION, KESTREL), WORDS, PAGES).get(0);
        assertEquals(WORDS.spokesman().id(), silent.storyId());
        assertNull(silent.memorial().tone());
        assertTrue(silent.reactions().isEmpty());
    }

    /** More condolences than the page tells one by one share one sentence. */
    @Test
    void aCrowdOfCondolencesSharesASentence() {
        FrontPage.Loss loss = loss();
        List<MemorialPrinter.Agency> agencies = new java.util.ArrayList<>(List.of(ORION));
        Map<String, List<FrontPage.Statement>> statements = new java.util.HashMap<>();
        for (int i = 0; i < 6; i++) {
            agencies.add(new MemorialPrinter.Agency("k" + i, "Agency " + i, "Director " + i));
            statements.put("k" + i, List.of(new FrontPage.Statement(loss.id(), FrontPage.Stance.CONDOLENCES,
                i % 2 == 0 ? "Our thoughts are with you." : "")));
        }
        FrontPage page = MemorialPrinter.print(45, SEED, List.of(loss), statements, agencies, WORDS, PAGES).get(0);
        assertEquals(6, page.reactions().size());
        assertEquals(MemorialPrinter.MAX_TOLD, page.reactions().stream()
            .filter(r -> !r.lineId().equals(WORDS.condolences().also().id())).count());
        assertTrue(page.reactions().subList(MemorialPrinter.MAX_TOLD, 6).stream().allMatch(r -> r.quote().isEmpty()));
    }

    private static FrontPage.Loss loss() {
        return new FrontPage.Loss("44-loss-1", 44, "k-orion", "Orion Spaceworks", "Clara Voss", "gemini-spacewalk",
            "18-reentry", List.of(new FrontPage.Lost("Ana Reyes", 38, null, "a1", 2, 0, 28, List.of("MENTOR"))),
            List.of("spacewalk"), List.of("spacewalk"));
    }

    private static PersonnelDeparture departure(String id, String name, int age) {
        return new PersonnelDeparture(PersonnelGroup.ASTRONAUT, id, name, DepartureReason.LOST_IN_ACCIDENT, age, 80,
            AssignmentState.ASSIGNED, null);
    }

    private static MemorialEntry entry(String id, String name, int age, int joined, List<Trait> traits, int flights) {
        List<CareerEntry> career = new java.util.ArrayList<>();
        career.add(new CareerEntry(joined, CareerEntry.Kind.HIRED, null, null, null, 0, 0));
        for (int i = 0; i < flights; i++) {
            career.add(new CareerEntry(joined + 1 + i, CareerEntry.Kind.FLIGHT, "gemini-spacewalk", "CREW", "", 0, 0));
        }
        return new MemorialEntry(name, age, 44, MemorialEntry.Cause.MISSION, "gemini-spacewalk", "18-reentry", "p-" + id,
            id, Map.of(), traits, joined, career);
    }
}
