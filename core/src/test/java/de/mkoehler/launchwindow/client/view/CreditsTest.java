package de.mkoehler.launchwindow.client.view;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link Credits}.
 *
 * @author Mario Koehler
 */
class CreditsTest {

    /**
     * Headings start sections, wrapped lines join, bullets keep their indented continuations, and
     * backticks are dropped.
     */
    @Test
    void parsesSectionsParagraphsAndBullets() {
        String markdown = String.join("\r\n",
            "# Credits",
            "",
            "Intro line one",
            "and two.",
            "",
            "## Fonts",
            "",
            "Archivo, see `fonts/OFL.txt`.",
            "",
            "## Photos",
            "",
            "- `laika.jpg`: a photo",
            "  by someone.",
            "- second bullet");
        List<Credits.Section> sections = Credits.parse(markdown);
        assertEquals(3, sections.size());
        assertEquals("", sections.get(0).title());
        assertEquals(List.of(new Credits.Entry("Intro line one and two.", false)), sections.get(0).entries());
        assertEquals("Fonts", sections.get(1).title());
        assertEquals(List.of(new Credits.Entry("Archivo, see fonts/OFL.txt.", false)), sections.get(1).entries());
        assertEquals(List.of(new Credits.Entry("laika.jpg: a photo by someone.", true),
            new Credits.Entry("second bullet", true)), sections.get(2).entries());
    }

    /**
     * The real credits file parses, and names the fonts and Laika's photo.
     *
     * @throws IOException if the file can't be read
     */
    @Test
    void theRealCreditsFileParses() throws IOException {
        String markdown = Files.readString(Path.of("..", "assets", "CREDITS.md"), StandardCharsets.UTF_8);
        List<Credits.Section> sections = Credits.parse(markdown);
        assertFalse(sections.isEmpty());
        String all = sections.toString();
        assertTrue(all.contains("Archivo"), all);
        assertTrue(all.contains("laika.jpg") && all.contains("CC BY 4.0"), all);
    }

    /**
     * A parenthesis with letters the font lacks is left out; other missing letters are dropped.
     */
    @Test
    void drawableLeavesOutWhatTheFontCantShow() {
        String abc = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ/(),.";
        assertEquals("Museum of Cosmonautics, Mos.ru",
            Credits.drawable("Museum of Cosmonautics (Музей космонавтики), Mos.ru", abc));
        assertEquals("Archive (Moscow) here", Credits.drawable("Archive (Moscow) here", abc));
        assertEquals("ab", Credits.drawable("a\u00e9b", abc));
    }

    /**
     * A missing file gives no sections.
     */
    @Test
    void missingFileGivesNothing() {
        assertTrue(Credits.parse(null).isEmpty());
    }
}
