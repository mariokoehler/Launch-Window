package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.ContentPackLoader;
import de.mkoehler.launchwindow.content.FrontPages;
import de.mkoehler.launchwindow.model.FrontPage;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
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
}
