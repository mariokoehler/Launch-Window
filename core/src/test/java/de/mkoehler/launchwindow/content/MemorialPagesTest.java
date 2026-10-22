package de.mkoehler.launchwindow.content;

import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.view.MemorialView;
import de.mkoehler.launchwindow.model.FrontPage;
import de.mkoehler.launchwindow.model.Trait;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link MemorialPages} and the bundled memorial pages' words ({@code ui-spec.md} &sect;4.18.3).
 *
 * @author Mario Koehler
 */
class MemorialPagesTest {

    private static final MemorialPages WORDS = MemorialPages.load(Path.of("..", "assets", "content", MemorialPages.FILE));
    private static final Set<String> TOKENS = Set.of("agency", "director", "season", "crew", "name", "age", "nth",
        "years", "where", "during", "tail", "first", "after", "count", "rival", "rivalDirector", "rivals", "quote");

    /** Every tone has at least two stories, each with its words; the ids are unique. */
    @Test
    void everyToneHasItsStories() {
        Set<String> ids = new HashSet<>();
        for (FrontPage.Stance tone : FrontPage.Stance.values()) {
            if (!tone.tone()) {
                continue;
            }
            List<MemorialPages.Story> stories = WORDS.storiesOf(tone);
            assertTrue(stories.size() >= 2, tone.name());
            for (MemorialPages.Story story : stories) {
                assertTrue(ids.add(story.id()), "duplicate " + story.id());
                assertTrue(story.quoted().endsWith("{quote}"), story.id());
                assertFalse(story.plain().contains("{quote}"), story.id());
                assertFalse(story.close().isBlank(), story.id());
            }
        }
        assertTrue(ids.add(WORDS.spokesman().id()));
        for (FrontPage.Stance stance : List.of(FrontPage.Stance.CONDOLENCES, FrontPage.Stance.TRIBUTE)) {
            assertTrue(WORDS.condolenceLine(stance, true).orElseThrow().text().endsWith("{quote}"));
            assertFalse(WORDS.condolenceLine(stance, false).orElseThrow().text().contains("{quote}"));
        }
        assertFalse(WORDS.historyNotes().page().isBlank());
        assertFalse(WORDS.historyNotes().statement().isBlank());
        assertFalse(WORDS.historyNotes().condolences().isBlank());
    }

    /** A trait line names a real trait; a first names a goal of the standard pack. */
    @Test
    void traitsAndFirstsAreReal() {
        for (String trait : WORDS.people().traits().keySet()) {
            Trait.valueOf(trait);
        }
        ContentCatalog catalog = ContentPackLoader.load(Path.of("..", "assets", "content", "standard"));
        for (MemorialPages.FirstPhrase first : WORDS.firsts()) {
            if (!first.goal().startsWith("crewed-lunar-")) {
                assertTrue(catalog.goal(first.goal()).isPresent(), first.goal());
            }
        }
        assertEquals("reentry", WORDS.place("18-reentry").id());
        assertEquals("pad", WORDS.place("01-launch-preparations").id());
        assertEquals("launch", WORDS.place("03-launch").id());
        assertEquals("spacewalk", WORDS.place("07-eva").id());
        assertEquals("moon", WORDS.place("14-lunar-landing").id());
        assertEquals("landing", WORDS.place("11-descent-and-landing").id());
        assertEquals("flight", WORDS.place("nothing-like-it").id());
    }

    /** The words use only known tokens and letters the fonts have. */
    @Test
    void theWordsUseOnlyKnownTokensAndDrawableLetters() throws Exception {
        String json = java.nio.file.Files.readString(Path.of("..", "assets", "content", MemorialPages.FILE));
        Matcher matcher = Pattern.compile("\\{([A-Za-z]+)}").matcher(json);
        while (matcher.find()) {
            assertTrue(TOKENS.contains(matcher.group(1)), "unknown token {" + matcher.group(1) + "}");
        }
        List<String> texts = new ArrayList<>();
        WORDS.tones().forEach(t -> t.stories().forEach(s -> texts.addAll(List.of(s.quoted(), s.plain(), s.close()))));
        WORDS.condolences().lines().forEach(l -> texts.add(l.text()));
        texts.addAll(WORDS.people().traits().values());
        texts.addAll(List.of(WORDS.spokesman().plain(), WORDS.spokesman().close(), WORDS.withheld().text(),
            WORDS.standDown().text(), WORDS.leads().get(0).text(), WORDS.historyNotes().page(),
            WORDS.historyNotes().statement(), WORDS.historyNotes().condolences()));
        for (String text : texts) {
            text.chars().forEach(c -> assertTrue(c == ' ' || Theme.CHARACTERS.indexOf(c) >= 0,
                "'" + (char) c + "' isn't in the fonts: " + text));
        }
    }

    /** The article reads in order, with the first the flight had reached and the rivals' words. */
    @Test
    void theArticleIsPutTogether() {
        FrontPage.Loss loss = new FrontPage.Loss("44-loss-1", 44, "k", "Orion Spaceworks", "Clara Voss",
            "gemini-spacewalk", "18-reentry", List.of(
                new FrontPage.Lost("Ana Reyes", 38, null, "a1", 2, 0, 28, List.of("MENTOR")),
                new FrontPage.Lost("Lev Marek", 32, null, "a2", 1, 0, 2, List.of())),
            List.of("spacewalk"), List.of("spacewalk"));
        FrontPage page = new FrontPage("45-memorial-44-loss-1", "", 45, "m-people-1", "Cold", List.of(),
            List.of(new FrontPage.Achiever("Orion Spaceworks", "Clara Voss", List.of())), List.of(),
            FrontPage.Kind.MEMORIAL, null, List.of(new FrontPage.Reaction("Kestrel Aerospace", "Ellen Hale",
                FrontPage.Stance.TRIBUTE, "We will not forget them", "m-tribute-q-1", null, null)),
            new FrontPage.Memorial(loss, FrontPage.Stance.PEOPLE, "Lev wrote home every day.", List.of()));
        MemorialView.Page view = MemorialView.of(page, WORDS).orElseThrow();

        assertEquals("ORION SPACEWORKS LOSES TWO ON THE WAY HOME", view.headline());
        assertEquals("Ana Reyes and Lev Marek died during reentry, after the first walk in space", view.deck());
        assertTrue(view.article().get(0).startsWith("WINTER 1966 — Ana Reyes and Lev Marek of Orion Spaceworks died"),
            view.article().get(0));
        assertTrue(view.article().get(1).endsWith("“Lev wrote home every day.”"), view.article().get(1));
        assertEquals("Ana Reyes, 38, was known as a patient teacher of the younger crews. Lev Marek, 32, was flying "
            + "for the first time.", view.article().get(2));
        assertEquals("They had become the first to walk in space, and did not come home.", view.article().get(3));
        assertEquals("From the other agencies", view.head());
        assertEquals(List.of("Ellen Hale, director of Kestrel Aerospace, paid tribute: “We will not forget them.”"),
            view.condolences());
    }
}
