package de.mkoehler.launchwindow.content;

import de.mkoehler.launchwindow.client.ui.Theme;
import org.junit.jupiter.api.Test;

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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link Theories} and the bundled theories of the day ({@code ui-spec.md} &sect;4.18.2).
 *
 * @author Mario Koehler
 */
class TheoriesTest {

    private static final Theories THEORIES = Theories.load(Path.of("..", "assets", "content", Theories.FILE));

    @Test
    void everyTheoryIsCompleteForItsReach() {
        assertTrue(THEORIES.theories().size() >= 8);
        Set<String> ids = new HashSet<>();
        for (Theories.Theory theory : THEORIES.theories()) {
            assertTrue(ids.add(theory.id()), "duplicate id " + theory.id());
            assertFalse(theory.raisedBy().isEmpty(), theory.id());
            assertFalse(theory.answeredBy().isEmpty(), theory.id());
            for (String text : List.of(theory.title(), theory.question(), theory.settledBy(), theory.answer(),
                    theory.historyNote())) {
                assertFalse(text == null || text.isBlank(), theory.id());
            }
            if (theory.reach() == Theories.Reach.PUBLIC) {
                assertFalse(theory.headline() == null || theory.headline().isBlank(), theory.id());
                assertTrue(theory.headlineShort().length() <= theory.headline().length(), theory.id());
                assertTrue(theory.deck().contains("{agency}"), theory.id());
                assertFalse(theory.article().isEmpty(), theory.id());
                assertTrue(theory.article().get(0).startsWith("{season} — "), theory.id());
            } else {
                assertTrue(theory.notice().contains("{agency}"), theory.id());
            }
        }
    }

    /** A theory a pack can't raise or settle never shows, so every goal must be one of the standard pack's. */
    @Test
    void everyGoalIsAGoalOfTheStandardPack() {
        ContentCatalog catalog = ContentPackLoader.load(Path.of("..", "assets", "content", "standard"));
        for (Theories.Theory theory : THEORIES.theories()) {
            List<String> goals = new ArrayList<>(theory.raisedBy());
            goals.addAll(theory.answeredBy());
            for (String goal : goals) {
                assertTrue(catalog.goal(goal).isPresent(), theory.id() + ": " + goal);
            }
            for (String category : theory.raisedByCategories()) {
                assertTrue(catalog.category(category).isPresent(), theory.id() + ": " + category);
            }
            assertTrue(theory.playable(g -> catalog.goal(g).isPresent()), theory.id());
        }
    }

    @Test
    void theWordsUseOnlyKnownTokensAndDrawableLetters() {
        Pattern token = Pattern.compile("\\{([A-Za-z0-9]+)}");
        for (Theories.Theory theory : THEORIES.theories()) {
            List<String> texts = new ArrayList<>(List.of(theory.title(), theory.question(), theory.settledBy(),
                theory.answer(), theory.historyNote()));
            for (String text : new String[] {theory.headline(), theory.headlineShort(), theory.deck(), theory.notice()}) {
                if (text != null) {
                    texts.add(text);
                }
            }
            texts.addAll(theory.article());
            for (String text : texts) {
                Matcher matcher = token.matcher(text);
                while (matcher.find()) {
                    assertTrue(Set.of("agency", "season").contains(matcher.group(1)), "unknown token in: " + text);
                }
                text.chars().forEach(c -> assertTrue(c == ' ' || Theme.CHARACTERS.indexOf(c) >= 0,
                    "'" + (char) c + "' isn't in the fonts: " + text));
            }
        }
    }

    /** The Museum links a settled theory to the archive's pages about the flights that settled it. */
    @Test
    void everyArchiveLinkIsAnArchivePage() {
        de.mkoehler.launchwindow.client.view.Archive archive =
            de.mkoehler.launchwindow.client.view.Archive.load(Path.of("..", "assets"));
        for (Theories.Theory theory : THEORIES.theories()) {
            for (String id : theory.archive()) {
                assertTrue(archive.page(id) != null, theory.id() + ": " + id);
            }
        }
        assertEquals(List.of("luna-9", "surveyor-1"), THEORIES.theory("deep-dust").orElseThrow().archive());
    }

    /**
     * A science page's article fills most of its four columns and stays on the page: at least {@link #MIN_LINES}
     * (fewer leaves most of the page to the filler items) and at most {@link #MAX_LINES} at the longest names, two
     * agencies sharing it. The page holds about 56 lines under "THE QUESTION" and over the race box.
     */
    @Test
    void everyArticleFillsItsPageAndFits() throws Exception {
        java.awt.Font font = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, java.nio.file.Files.newInputStream(
            Path.of("..", "assets", "fonts", "LibreCaslonText-Regular.ttf"))).deriveFont(16f);
        java.awt.font.FontRenderContext context = new java.awt.font.FontRenderContext(null, true, true);
        for (Theories.Theory theory : THEORIES.theories()) {
            if (theory.reach() != Theories.Reach.PUBLIC) {
                continue;
            }
            int lines = 0;
            for (String paragraph : theory.article()) {
                lines += lines(paragraph.replace("{agency}", "Aaaaaaaaaaaa Bbbbbbbbbbbb Cccccc and Aaaaaaaaaaaa "
                    + "Bbbbbbbbbbbb Dddddd").replace("{season}", "SUMMER 1968"), font, context);
            }
            assertTrue(lines >= MIN_LINES && lines <= MAX_LINES, theory.id() + " runs to " + lines + " lines");
        }
    }

    private static final int MIN_LINES = 32;
    private static final int MAX_LINES = 48;

    private static int lines(String text, java.awt.Font font, java.awt.font.FontRenderContext context) {
        int lines = 1;
        float indent = (float) font.getStringBounds("    ", context).getWidth();
        StringBuilder line = new StringBuilder();
        for (String word : text.strip().split(" ")) {
            String trial = line.isEmpty() ? word : line + " " + word;
            float width = (float) font.getStringBounds(trial, context).getWidth() + (lines == 1 ? indent : 0f);
            if (!line.isEmpty() && width > 316f) {
                lines++;
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(trial);
            }
        }
        return lines;
    }

    @Test
    void aTheoryIsRaisedOnTheFirstOfItsGoals() {
        Theories.Theory dust = THEORIES.theory("deep-dust").orElseThrow();
        assertEquals(java.util.Optional.of(14), dust.raisedOn(Map.of("lunar-probe", 14, "earth-orbit-satellite", 3)));
        assertTrue(dust.raisedOn(Map.of("earth-orbit-satellite", 3)).isEmpty());
        assertFalse(dust.playable(g -> g.equals("lunar-probe")));
    }

    /**
     * The radiation belts are raised when the first satellite program opens (2026-10-27), since the first satellite's
     * goal would raise and settle them in the same turn; the goal still raises them where no opening is known.
     */
    @Test
    void theRadiationBeltsAreRaisedByOpeningASatelliteProgram() {
        Theories.Theory belts = THEORIES.theory("radiation-belts").orElseThrow();
        assertEquals(List.of("earth-orbit-satellites"), belts.raisedByCategories());
        assertEquals(java.util.Optional.of(2), belts.raisedOn(Map.of(Theories.CATEGORY + "earth-orbit-satellites", 2,
            "earth-orbit-satellite", 9)));
        assertEquals(java.util.Optional.of(9), belts.raisedOn(Map.of("earth-orbit-satellite", 9)));
        assertTrue(belts.raisedOn(Map.of(Theories.CATEGORY + "lunar-probes", 2)).isEmpty());
    }
}
