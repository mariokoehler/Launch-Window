package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.ContentPackLoader;
import de.mkoehler.launchwindow.content.FrontPageReactions;
import de.mkoehler.launchwindow.content.FrontPages;
import de.mkoehler.launchwindow.content.Theories;
import de.mkoehler.launchwindow.model.FrontPage;
import de.mkoehler.launchwindow.net.messages.ArchiveUnlock;
import de.mkoehler.launchwindow.rules.ReactionPrinter;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link FrontPageView} ({@code ui-spec.md} &sect;4.18).
 *
 * @author Mario Koehler
 */
class FrontPageViewTest {

    private static final FrontPages PAGES = FrontPages.load(Path.of("..", "assets", "content", FrontPages.FILE));
    private static final ContentCatalog CATALOG = ContentPackLoader.load(Path.of("..", "assets", "content", "standard"));
    private static final Function<String, String> NAMES = id -> switch (id) {
        case "earth-orbit-satellite" -> "Satellite in orbit";
        case "human-in-space" -> "Human in space";
        case "human-in-orbit" -> "Human in orbit";
        case "crewed-lunar-flyby" -> "People around the Moon";
        default -> "People on the Moon";
    };

    private static final FrontPage SATELLITE = new FrontPage("11-earth-orbit-satellite", "earth-orbit-satellite", 11,
        "sat-new-moon", "Crisp, clear evening", List.of(), List.of(new FrontPage.Achiever("Kestrel Aerospace",
            "Moses Okafor", List.of())), List.of("Orion Spaceworks"));
    private static final FrontPage ORBIT = new FrontPage("26-human-in-orbit", "human-in-orbit", 26, "orbit-circles",
        "Fair and cool tonight", List.of("human-in-space"), List.of(new FrontPage.Achiever("Orion Spaceworks",
            "Ana Reyes", List.of("Daniel Hale"))), List.of("Kestrel Aerospace", "Baltic Star Agency"));

    @Test
    void thePageFillsItsTokens() {
        FrontPageView.Page page = FrontPageView.of(ORBIT, List.of(SATELLITE, ORBIT), PAGES, goal -> true, NAMES)
            .orElseThrow();
        assertEquals("ORION SPACEWORKS PILOT CIRCLES THE EARTH", page.headline());
        assertEquals("VOL. VII · No. 26", page.folio());
        assertEquals("SPRING 1961", page.season());
        assertEquals("frontpage/human-in-orbit.jpg", page.photo());
        assertTrue(page.caption().contains("Daniel Hale"));
        String article = String.join(" ", page.paragraphs());
        assertTrue(article.startsWith("SPRING 1961 — A pilot of Orion Spaceworks"), article);
        assertTrue(article.contains("“Human in space”"), "the folded First is told");
        assertTrue(article.contains("Kestrel Aerospace") && article.contains("Baltic Star Agency"), "two rivals named");
        assertFalse(article.contains("{"), article);
        assertEquals(2, page.droppable());
    }

    @Test
    void theRaceBoxShowsTheRaceAsItStoodThatDay() {
        List<FrontPageView.RaceRow> race = FrontPageView.race(SATELLITE, List.of(SATELLITE, ORBIT), PAGES,
            goal -> true, NAMES);
        assertEquals(5, race.size());
        assertEquals("Kestrel Aerospace · Summer 1957", race.get(0).who());
        assertEquals("still open", race.get(2).who(), "the later orbit isn't on the satellite's page");

        race = FrontPageView.race(ORBIT, List.of(SATELLITE, ORBIT), PAGES, goal -> !goal.startsWith("crewed"), NAMES);
        assertEquals(3, race.size(), "only the pack's Firsts");
        assertEquals("Orion Spaceworks · Spring 1961", race.get(1).who(), "a folded First counts");
        assertTrue(race.get(2).done());
    }

    @Test
    void theGameLineSaysWhoGotThereAndWhatItPays() {
        assertEquals("Orion Spaceworks got there first. Human in orbit now pays 1,500 prestige to whoever follows.",
            FrontPageView.gameLine(ORBIT, "Kestrel Aerospace", CATALOG, NAMES));
        assertEquals("Your agency got there first: Human in orbit paid 3,000 prestige.",
            FrontPageView.gameLine(ORBIT, "Orion Spaceworks", CATALOG, NAMES));
        FrontPage tie = new FrontPage("52-crewed-lunar-landing", "crewed-lunar-landing", 52, "landing-tie", "",
            List.of(), List.of(new FrontPage.Achiever("A", "a", List.of()), new FrontPage.Achiever("B", "b", List.of())),
            List.of());
        assertEquals("You share this First with B: each of you got 10,000 prestige.",
            FrontPageView.gameLine(tie, "A", CATALOG, NAMES));
        assertTrue(FrontPageView.gameLine(tie, "C", CATALOG, NAMES).startsWith("A and B got there first together."));
    }

    @Test
    void namesJoinAsASentenceDoes() {
        assertEquals("", FrontPageView.join(List.of()));
        assertEquals("A", FrontPageView.join(List.of("A")));
        assertEquals("A and B", FrontPageView.join(List.of("A", "B")));
        assertEquals("A, B and C", FrontPageView.join(List.of("A", "B", "C")));
    }

    @Test
    void volumesAreInRomanNumerals() {
        assertEquals("I", FrontPageView.roman(1));
        assertEquals("VII", FrontPageView.roman(7));
        assertEquals("XIV", FrontPageView.roman(14));
        assertEquals("XLIX", FrontPageView.roman(49));
    }

    @Test
    void aFollowUpTellsEveryStatementAsOneArticle() {
        FrontPageReactions words = FrontPageReactions.load(Path.of("..", "assets", "content", FrontPageReactions.FILE));
        List<ReactionPrinter.Agency> agencies = List.of(
            new ReactionPrinter.Agency("Orion Spaceworks", "Ana Reyes", 1, Set.of(), Map.of()),
            new ReactionPrinter.Agency("Kestrel Aerospace", "Moses Okafor", 1, Set.of("earth-orbit-satellite"),
                Map.of(ORBIT.id(), new FrontPage.Statement(ORBIT.id(), FrontPage.Stance.ENTHUSIASTIC,
                    "Today belongs to everyone who has ever looked up"))),
            new ReactionPrinter.Agency("Northern Rocket Bureau", "Ellen Hale", 1, Set.of("human-in-space"),
                Map.of(ORBIT.id(), new FrontPage.Statement(ORBIT.id(), FrontPage.Stance.NEXT,
                    "Enjoy the view while it lasts. We will be up there before the year is out!"))),
            new ReactionPrinter.Agency("Union Rocket Institute", "Pavel Brandt", 1, Set.of("human-in-space"),
                Map.of(ORBIT.id(), new FrontPage.Statement(ORBIT.id(), FrontPage.Stance.DOWNPLAY, ""))),
            new ReactionPrinter.Agency("Baltic Star Agency", "Rosa Ortiz", 1, Set.of(), Map.of()));
        FrontPage followUp = ReactionPrinter.print(27, 99L, List.of(ORBIT), agencies, words, PAGES).get(0);
        FrontPageView.Page page = FrontPageView.ofReactions(followUp, List.of(SATELLITE, ORBIT, followUp), words, PAGES,
            goal -> true, NAMES).orElseThrow();
        String article = String.join("\n", page.paragraphs());
        assertTrue(article.startsWith("SUMMER 1961 — "), article);
        assertTrue(article.contains("“Today belongs to everyone who has ever looked up.”"), article);
        assertTrue(article.contains("“Enjoy the view while it lasts. We will be up there before the year is out!”"),
            article);
        assertTrue(article.indexOf("Kestrel Aerospace") < article.indexOf("Northern Rocket Bureau")
            && article.indexOf("Northern Rocket Bureau") < article.indexOf("Union Rocket Institute"), article);
        assertTrue(article.contains("Baltic Star Agency"), "the silent rival is named");
        assertFalse(article.contains("{"), article);
        assertTrue(page.brief().isEmpty());
        assertEquals(1, page.droppable());
        assertEquals(page.headline().toUpperCase(), page.headline());
    }

    @Test
    void aQuoteGetsCurlyQuotesAndAFullStop() {
        assertEquals("“We will follow.”", FrontPageView.quoted("We will follow"));
        assertEquals("“Why not us?”", FrontPageView.quoted("Why not us?"));
        assertEquals("", FrontPageView.quoted(""));
    }

    /**
     * The Museum lists the theories raised or settled, never one neither (no spoilers), and links a settled one
     * to its open archive pages only.
     */
    @Test
    void theMuseumListsTheTheoriesRaisedOrSettled() {
        Theories theories = Theories.load(Path.of("..", "assets", "content", Theories.FILE));
        Archive archive = Archive.load(Path.of("..", "assets"));
        FrontPage dust = new FrontPage("34-science-deep-dust", "lunar-soft-landing", 34, "deep-dust", "", List.of(),
            List.of(new FrontPage.Achiever("Kestrel", "Moses", List.of())), List.of(), FrontPage.Kind.SCIENCE, null,
            null);
        FrontPage germs = new FrontPage("60-science-moon-germs", "crewed-lunar-landing", 60, "moon-germs", "",
            List.of(), List.of(new FrontPage.Achiever("Kestrel", "Moses", List.of())), List.of(),
            FrontPage.Kind.SCIENCE, null, null);
        List<Archive.Entry> entries = archive.entries(CATALOG,
            List.of(new ArchiveUnlock(archive.page("luna-9").goalId(), 33, false)));
        List<FrontPageView.TheoryRow> rows = FrontPageView.museum(theories,
            Map.of("earth-orbit-satellite", 5, "lunar-probe", 30), List.of(SATELLITE, dust, germs), entries,
            goal -> CATALOG.goal(goal).isPresent());
        assertEquals(List.of("deep-dust", "weightless-body", "radiation-belts", "venus-jungle", "mars-canals",
            "moon-germs"), rows.stream().map(r -> r.theory().id()).toList());
        FrontPageView.TheoryRow first = rows.get(0);
        assertEquals(30, first.raisedOn());
        assertEquals(dust, first.settled());
        assertTrue(first.hasPage());
        assertEquals(List.of("luna-9"), first.archive().stream().map(e -> e.page().id()).toList(),
            "Surveyor 1's page isn't open yet");
        assertTrue(rows.get(1).settled() == null && rows.get(1).archive().isEmpty());
        assertEquals(null, rows.get(5).raisedOn(), "settled before this agency raised it");
        assertFalse(FrontPageView.museum(theories, Map.of(), null, entries, goal -> true).iterator().hasNext());
    }
}
