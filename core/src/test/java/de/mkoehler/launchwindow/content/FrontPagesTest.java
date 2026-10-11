package de.mkoehler.launchwindow.content;

import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.model.Season;
import org.junit.jupiter.api.Test;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link FrontPages} and the bundled front pages' words ({@code ui-spec.md} &sect;4.18).
 *
 * @author Mario Koehler
 */
class FrontPagesTest {

    private static final FrontPages PAGES = FrontPages.load(Path.of("..", "assets", "content", FrontPages.FILE));

    /** The tokens a page may use. */
    private static final Set<String> TOKENS = Set.of("agency", "director", "season", "crew", "commander", "rival1",
        "rival2", "also");

    /** The standard pack's five Firsts, in race order. */
    private static final List<String> FIRSTS = List.of("earth-orbit-satellite", "human-in-space", "human-in-orbit",
        "crewed-lunar-flyby", "crewed-lunar-landing");

    /**
     * The longest names a page has to hold: 32 characters, the claim's limit, and three long crew
     * names.
     */
    private static final Map<String, String> LONGEST = Map.of(
        "agency", "Aaaaaaaaaaaa Bbbbbbbbbbbb Cccccc",
        "director", "Alexandra Konstantinopoulou-Sims",
        "season", "SUMMER 1968",
        "crew", "Alexandra Konstantinopoulou, Maximilian Oberhauser and Bartholomew Fairweather",
        "commander", "Alexandra Konstantinopoulou",
        "rival1", "Aaaaaaaaaaaa Bbbbbbbbbbbb Dddddd",
        "rival2", "Aaaaaaaaaaaa Bbbbbbbbbbbb Eeeeee",
        "also", "“Human in space” and “Satellite in orbit”");

    /** The article's lines at the longest names that both columns hold under a two-line headline. */
    private static final int MAX_LINES = 26;

    /** The page's column width and body size, as {@code FrontPageScreen} sets them. */
    private static final float COLUMN = 336f;
    private static final float BODY_SIZE = 16f;

    @Test
    void everyFirstHasItsWords() {
        assertEquals(FIRSTS, PAGES.firsts().stream().map(FrontPages.First::goal).toList());
        Set<String> ids = new HashSet<>();
        for (FrontPages.First first : PAGES.firsts()) {
            assertTrue(first.stories().size() >= 2, first.goal() + " needs two stories");
            assertNotNull(first.tie(), first.goal() + " needs a tie story");
            assertEquals(2, first.brief().size(), first.goal());
            assertFalse(first.caption().isBlank(), first.goal());
            List<FrontPages.Story> stories = new ArrayList<>(first.stories());
            stories.add(first.tie());
            for (FrontPages.Story story : stories) {
                assertTrue(ids.add(story.id()), "duplicate id " + story.id());
                assertFalse(story.headline().isBlank(), story.id());
                assertFalse(story.headlineShort().isBlank(), story.id());
                assertTrue(story.headlineShort().length() <= story.headline().length(), story.id());
                assertFalse(story.lead().isEmpty(), story.id());
                assertFalse(story.rivals1().isBlank(), story.id());
                assertFalse(story.rivals2().isBlank(), story.id());
            }
        }
        for (Season season : Season.values()) {
            assertFalse(PAGES.weatherFor(season).isEmpty(), season.name());
        }
    }

    @Test
    void everyFirstIsAGoalOfTheStandardPack() {
        ContentCatalog catalog = ContentPackLoader.load(Path.of("..", "assets", "content", "standard"));
        for (FrontPages.First first : PAGES.firsts()) {
            assertTrue(catalog.goal(first.goal()).isPresent(), first.goal());
        }
    }

    @Test
    void theWordsUseOnlyKnownTokensAndDrawableLetters() {
        Pattern token = Pattern.compile("\\{([a-z0-9]+)}");
        for (String text : allTexts()) {
            Matcher matcher = token.matcher(text);
            while (matcher.find()) {
                assertTrue(TOKENS.contains(matcher.group(1)), "unknown token in: " + text);
            }
            text.chars().forEach(c -> assertTrue(c == ' ' || Theme.CHARACTERS.indexOf(c) >= 0,
                "'" + (char) c + "' isn't in the fonts: " + text));
        }
    }

    @Test
    void theTokensPagesDependOnAreThere() {
        for (FrontPages.First first : PAGES.firsts()) {
            for (FrontPages.Story story : first.stories()) {
                assertTrue(story.rivals1().contains("{rival1}"), story.id());
                assertTrue(story.rivals2().contains("{rival1}") && story.rivals2().contains("{rival2}"), story.id());
                if (!story.also().isBlank()) {
                    assertTrue(story.also().contains("{also}"), story.id());
                }
            }
        }
    }

    /**
     * Every article, with the longest names allowed, the folded paragraph and two rivals, fits the two
     * text columns under a two-line headline, measured with the page's own body font.
     */
    @Test
    void everyArticleFitsItsColumns() throws Exception {
        Font font = Font.createFont(Font.TRUETYPE_FONT,
            Files.newInputStream(Path.of("..", "assets", "fonts", "LibreCaslonText-Regular.ttf"))).deriveFont(BODY_SIZE);
        FontRenderContext context = new FontRenderContext(null, true, true);
        for (FrontPages.First first : PAGES.firsts()) {
            List<FrontPages.Story> stories = new ArrayList<>(first.stories());
            stories.add(first.tie());
            for (FrontPages.Story story : stories) {
                int lines = 0;
                for (String paragraph : story.paragraphs(true, 2)) {
                    lines += lines(fill(paragraph), font, context);
                }
                assertTrue(lines <= MAX_LINES, story.id() + " runs to " + lines + " lines");
            }
        }
    }

    @Test
    void storiesAreFoundByIdAndOrderIsRaceOrder() {
        FrontPages.First orbit = PAGES.first("human-in-orbit").orElseThrow();
        assertEquals("orbit-tie", orbit.story("orbit-tie").orElseThrow().id());
        assertTrue(orbit.story("sat-new-moon").isEmpty());
        assertTrue(PAGES.order("earth-orbit-satellite") < PAGES.order("crewed-lunar-landing"));
        assertEquals(-1, PAGES.order("spacewalk"));
    }

    @Test
    void paragraphsFollowTheArticleOrder() {
        FrontPages.Story story = new FrontPages.Story("x", "h", "s", "d", List.of("a", "b"), "also", "two", "one",
            List.of("c"));
        assertEquals(List.of("a", "b", "c"), story.paragraphs(false, 0));
        assertEquals(List.of("a", "b", "also", "one", "c"), story.paragraphs(true, 1));
        assertEquals(List.of("a", "b", "two", "c"), story.paragraphs(false, 2));
    }

    private static int lines(String text, Font font, FontRenderContext context) {
        String indented = "    " + text;
        int lines = 1;
        StringBuilder line = new StringBuilder();
        for (String word : indented.strip().split(" ")) {
            String trial = line.isEmpty() ? word : line + " " + word;
            float indent = lines == 1 ? (float) font.getStringBounds("    ", context).getWidth() : 0f;
            if (!line.isEmpty() && font.getStringBounds(trial, context).getWidth() + indent > COLUMN) {
                lines++;
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(trial);
            }
        }
        return lines;
    }

    private static String fill(String text) {
        String filled = text;
        for (Map.Entry<String, String> token : LONGEST.entrySet()) {
            filled = filled.replace("{" + token.getKey() + "}", token.getValue());
        }
        return filled;
    }

    private static List<String> allTexts() {
        List<String> texts = new ArrayList<>();
        for (FrontPages.First first : PAGES.firsts()) {
            texts.add(first.caption());
            texts.addAll(first.brief());
            List<FrontPages.Story> stories = new ArrayList<>(first.stories());
            stories.add(first.tie());
            for (FrontPages.Story story : stories) {
                texts.add(story.headline());
                texts.add(story.headlineShort());
                texts.add(story.deck());
                texts.addAll(story.paragraphs(true, 2));
                texts.add(story.rivals1());
            }
        }
        PAGES.weather().values().forEach(texts::addAll);
        return texts;
    }
}
