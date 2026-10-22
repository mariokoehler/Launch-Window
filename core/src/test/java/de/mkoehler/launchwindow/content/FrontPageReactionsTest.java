package de.mkoehler.launchwindow.content;

import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.model.FrontPage;
import de.mkoehler.launchwindow.rules.ReactionPrinter;
import org.junit.jupiter.api.Test;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
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
 * Tests for {@link FrontPageReactions} and the bundled follow-ups' words ({@code ui-spec.md} &sect;4.18.1).
 *
 * @author Mario Koehler
 */
class FrontPageReactionsTest {

    private static final FrontPageReactions WORDS = FrontPageReactions.load(
        Path.of("..", "assets", "content", FrontPageReactions.FILE));
    private static final FrontPages PAGES = FrontPages.load(Path.of("..", "assets", "content", FrontPages.FILE));

    /** The tokens each kind of text may use. */
    private static final Set<String> PAGE_TOKENS = Set.of("agency", "director", "season", "commander", "crew");
    private static final Set<String> LINE_TOKENS = Set.of("agency", "director", "season", "commander", "crew",
        "rival", "rivalDirector", "detail", "quote");
    private static final Set<String> SHARED_TOKENS = Set.of("agency", "director", "season", "commander", "crew",
        "rivals");

    /** The longest names a page has to hold: 32 characters, the claim's limit. */
    private static final Map<String, String> LONGEST = Map.of(
        "agency", "Aaaaaaaaaaaa Bbbbbbbbbbbb Cccccc",
        "director", "Alexandra Konstantinopoulou-Sims",
        "season", "SUMMER 1968",
        "commander", "Alexandra Konstantinopoulou",
        "crew", "Alexandra Konstantinopoulou, Maximilian Oberhauser and Bartholomew Fairweather",
        "rival", "Aaaaaaaaaaaa Bbbbbbbbbbbb Dddddd",
        "rivalDirector", "Konstantin Alexandropoulos-Smith",
        "rivals", "Aaaaaaaaaaaa Bbbbbbbbbbbb Eeeeee and Aaaaaaaaaaaa Bbbbbbbbbbbb Ffffff");

    /** A quote at the limit, in words of an ordinary length. */
    private static final String LONG_QUOTE = "“" + ("We congratulate them warmly on a fine achievement and we "
        + "look forward to the day when our own crews will follow them into the history books. ").repeat(2).substring(0,
        FrontPage.Statement.MAX_QUOTE_LENGTH) + "”";

    /**
     * The article's lines at the longest names that a follow-up's text columns hold: four columns of
     * {@link #COLUMN} under a two-line headline, less the photo and its caption over the first and the
     * race box at the foot of the last; the photo gives up height when the article needs it. Unconfirmed
     * until the page is mocked up.
     */
    private static final int MAX_LINES = 64;

    /** The follow-up's column width and body size. */
    private static final float COLUMN = 316f;
    private static final float BODY_SIZE = 16f;

    @Test
    void everyFirstHasAStoryPerMoodAndItsDetails() {
        assertEquals(PAGES.firsts().stream().map(FrontPages.First::goal).toList(),
            WORDS.firsts().stream().map(FrontPageReactions.First::goal).toList());
        for (FrontPageReactions.First first : WORDS.firsts()) {
            for (FrontPageReactions.Mood mood : FrontPageReactions.Mood.values()) {
                List<FrontPageReactions.Story> stories = first.stories().stream().filter(s -> s.mood() == mood).toList();
                assertFalse(stories.isEmpty(), first.goal() + " has no " + mood + " story");
                for (FrontPageReactions.Story story : stories) {
                    assertFalse(story.headline().isBlank(), story.id());
                    assertTrue(story.headlineShort().length() <= story.headline().length(), story.id());
                    assertFalse(story.deck().isBlank(), story.id());
                    assertFalse(story.lead().isEmpty(), story.id());
                    assertFalse(story.headline().contains("{agency}'s"), story.id() + ": a tie has two agencies");
                    if (mood != FrontPageReactions.Mood.MIXED) {
                        assertFalse(story.deckOne().isBlank(), story.id() + " needs a deck for one statement");
                        assertFalse(story.leadOne().isEmpty(), story.id() + " needs a lead for one statement");
                    }
                }
            }
            for (FrontPageReactions.DetailKind kind : FrontPageReactions.DetailKind.values()) {
                assertFalse(first.detailsOf(kind).isEmpty(), first.goal() + " has no " + kind + " detail");
                for (FrontPageReactions.Line line : first.detailsOf(kind)) {
                    assertTrue(line.text().startsWith(", ") && line.text().endsWith(","), line.id());
                }
            }
            assertFalse(first.close().isEmpty(), first.goal());
            assertTrue(first.before() == null || PAGES.order(first.before()) == PAGES.order(first.goal()) - 1,
                first.goal() + "'s before is the First before it");
        }
    }

    @Test
    void everyStanceHasItsLines() {
        for (FrontPage.Stance stance : FrontPage.Stance.values()) {
            if (!stance.givable()) {
                continue;
            }
            FrontPageReactions.StanceLines lines = WORDS.linesOf(stance);
            assertTrue(lines.plain().size() >= 3, stance + " plain");
            assertTrue(lines.also().size() >= 2, stance + " also");
            assertEquals(stance.quotable(), !lines.quoted().isEmpty(), stance + " quoted");
            if (stance.quotable()) {
                assertTrue(lines.quoted().size() >= 3, stance + " quoted");
            }
            for (FrontPageReactions.Line line : lines.quoted()) {
                assertTrue(line.text().endsWith("{quote}"), line.id() + " must end in its quote");
                assertEquals(1, count(line.text(), "{quote}"), line.id());
            }
            List<FrontPageReactions.Line> told = new ArrayList<>(lines.quoted());
            told.addAll(lines.plain());
            for (FrontPageReactions.Line line : told) {
                assertEquals(1, count(line.text(), "{rival}{detail} "), line.id() + " sets {detail} after {rival}");
            }
            for (FrontPageReactions.Line line : lines.plain()) {
                assertFalse(line.text().contains("{quote}"), line.id());
            }
            for (FrontPageReactions.Line line : lines.also()) {
                assertTrue(line.text().contains("{rivals}"), line.id());
            }
        }
        assertFalse(WORDS.declined().one().isEmpty());
        assertFalse(WORDS.declined().many().isEmpty());
    }

    @Test
    void everyChangeOfStanceHasALink() {
        List<FrontPage.Stance> givable = List.of(FrontPage.Stance.values()).stream()
            .filter(FrontPage.Stance::givable).toList();
        for (int i = 0; i < givable.size(); i++) {
            for (int j = i + 1; j < givable.size(); j++) {
                assertFalse(WORDS.linksBetween(givable.get(i), givable.get(j)).isEmpty(),
                    givable.get(i) + " to " + givable.get(j));
            }
        }
    }

    @Test
    void idsAreUniqueAndLinesAreFoundByThem() {
        Set<String> ids = new HashSet<>();
        for (FrontPageReactions.First first : WORDS.firsts()) {
            first.stories().forEach(s -> assertTrue(ids.add(s.id()), "duplicate id " + s.id()));
        }
        for (FrontPageReactions.Line line : lineObjects()) {
            assertTrue(ids.add(line.id()), "duplicate id " + line.id());
            assertEquals(line.text(), WORDS.text(line.id()).orElseThrow());
        }
        assertTrue(WORDS.shared(WORDS.declined().one().get(0).id()));
        assertTrue(WORDS.shared(WORDS.linesOf(FrontPage.Stance.POLITE).also().get(0).id()));
        assertFalse(WORDS.shared(WORDS.linesOf(FrontPage.Stance.POLITE).plain().get(0).id()));
    }

    @Test
    void theWordsUseOnlyKnownTokensAndDrawableLetters() {
        Pattern token = Pattern.compile("\\{([A-Za-z0-9]+)}");
        Map<String, Set<String>> texts = new java.util.LinkedHashMap<>();
        for (FrontPageReactions.First first : WORDS.firsts()) {
            for (FrontPageReactions.Story story : first.stories()) {
                List<String> parts = new ArrayList<>(List.of(story.headline(), story.headlineShort(), story.deck(),
                    story.deckOne()));
                parts.addAll(story.lead());
                parts.addAll(story.leadOne());
                parts.forEach(t -> texts.put(t, PAGE_TOKENS));
            }
            first.close().forEach(t -> texts.put(t, PAGE_TOKENS));
            first.details().values().forEach(l -> l.forEach(line -> texts.put(line.text(), Set.of())));
        }
        WORDS.links().forEach(l -> texts.put(l.text(), Set.of()));
        for (FrontPage.Stance stance : FrontPage.Stance.values()) {
            FrontPageReactions.StanceLines lines = WORDS.linesOf(stance);
            lines.quoted().forEach(l -> texts.put(l.text(), LINE_TOKENS));
            lines.plain().forEach(l -> texts.put(l.text(), LINE_TOKENS));
            lines.also().forEach(l -> texts.put(l.text(), SHARED_TOKENS));
        }
        WORDS.declined().one().forEach(l -> texts.put(l.text(), SHARED_TOKENS));
        WORDS.declined().many().forEach(l -> texts.put(l.text(), SHARED_TOKENS));
        for (Map.Entry<String, Set<String>> entry : texts.entrySet()) {
            Matcher matcher = token.matcher(entry.getKey());
            while (matcher.find()) {
                assertTrue(entry.getValue().contains(matcher.group(1)), "unknown token in: " + entry.getKey());
            }
            entry.getKey().chars().forEach(c -> assertTrue(c == ' ' || Theme.CHARACTERS.indexOf(c) >= 0,
                "'" + (char) c + "' isn't in the fonts: " + entry.getKey()));
        }
    }

    /** The fillers: enough for three empty columns, each short enough for a column, in the fonts' letters. */
    @Test
    void fillersAndHistoryNotesAreThere() throws Exception {
        assertTrue(WORDS.fillers().size() >= 6);
        Font font = Font.createFont(Font.TRUETYPE_FONT,
            Files.newInputStream(Path.of("..", "assets", "fonts", "LibreCaslonText-Regular.ttf"))).deriveFont(BODY_SIZE);
        FontRenderContext context = new FontRenderContext(null, true, true);
        Set<String> ids = new HashSet<>();
        for (FrontPageReactions.Filler filler : WORDS.fillers()) {
            assertTrue(ids.add(filler.id()), filler.id());
            assertFalse(filler.headline().isBlank(), filler.id());
            assertFalse(filler.paragraphs().isEmpty(), filler.id());
            int lines = 0;
            for (String paragraph : filler.paragraphs()) {
                lines += lines(paragraph, font, context);
            }
            assertTrue(lines <= 9, filler.id() + " runs to " + lines + " lines");
            List<String> texts = new ArrayList<>(filler.paragraphs());
            texts.add(filler.headline());
            for (String text : texts) {
                assertFalse(text.contains("{"), filler.id());
                text.chars().forEach(c -> assertTrue(c == ' ' || Theme.CHARACTERS.indexOf(c) >= 0,
                    "'" + (char) c + "' isn't in the fonts: " + text));
            }
        }
        for (String note : List.of(WORDS.historyNotes().statement(), WORDS.historyNotes().followUp())) {
            assertFalse(note.isBlank());
            note.chars().forEach(c -> assertTrue(c == ' ' || Theme.CHARACTERS.indexOf(c) >= 0,
                "'" + (char) c + "' isn't in the fonts: " + note));
        }
    }

    @Test
    void aQuoteHoldsOnlyLettersTheFontsHave() {
        FrontPage.Statement.ALLOWED_CHARACTERS.chars().forEach(c -> assertTrue(c == ' '
            || Theme.CHARACTERS.indexOf(c) >= 0, "'" + (char) c + "' isn't in the fonts"));
        for (char c : "{}<>".toCharArray()) {
            assertTrue(FrontPage.Statement.ALLOWED_CHARACTERS.indexOf(c) < 0, "'" + c + "' could read as a token");
        }
        assertEquals("We will be next!", FrontPage.Statement.clean("  We   will be next!  "));
        assertTrue(FrontPage.Statement.allowed("Well flown, all of you: we’ll see you up there (soon)."));
        assertFalse(FrontPage.Statement.allowed("Line one\nline two"));
        assertFalse(FrontPage.Statement.allowed("{quote}"));
    }

    /**
     * The fullest follow-up fits its columns: the longest lead, {@link ReactionPrinter#MAX_TOLD}
     * statements told one by one with the longest link, line and detail and a quote at the limit, an
     * "also" line and a "declined" line, each naming two agencies, at the longest names allowed. The
     * closing paragraphs may be dropped to fit, so they don't count.
     */
    @Test
    void theFullestArticleFitsItsColumns() throws Exception {
        Font font = Font.createFont(Font.TRUETYPE_FONT,
            Files.newInputStream(Path.of("..", "assets", "fonts", "LibreCaslonText-Regular.ttf"))).deriveFont(BODY_SIZE);
        FontRenderContext context = new FontRenderContext(null, true, true);
        String link = longest(WORDS.links().stream().map(FrontPageReactions.Link::text).toList(), font, context);
        String also = longest(WORDS.stances().values().stream().flatMap(s -> s.also().stream())
            .map(FrontPageReactions.Line::text).toList(), font, context);
        String declined = longest(WORDS.declined().many().stream().map(FrontPageReactions.Line::text).toList(), font,
            context);
        String line = longest(WORDS.stances().values().stream().flatMap(s -> s.quoted().stream())
            .map(FrontPageReactions.Line::text).toList(), font, context);
        int worst = 0;
        for (FrontPageReactions.First first : WORDS.firsts()) {
            String detail = longest(first.details().values().stream().flatMap(List::stream)
                .map(FrontPageReactions.Line::text).toList(), font, context);
            for (FrontPageReactions.Story story : first.stories()) {
                int lines = 0;
                for (String paragraph : story.lead().size() >= story.leadOne().size() ? story.lead() : story.leadOne()) {
                    lines += lines(fill(paragraph), font, context);
                }
                String told = fill(link + " " + line.replace("{detail}", detail)).replace("{quote}", LONG_QUOTE);
                lines += ReactionPrinter.MAX_TOLD * lines(told, font, context);
                lines += lines(fill(also), font, context) + lines(fill(declined), font, context);
                worst = Math.max(worst, lines);
                assertTrue(lines <= MAX_LINES, story.id() + " runs to " + lines + " lines");
            }
        }
        assertTrue(worst > 0);
    }

    private static List<FrontPageReactions.Line> lineObjects() {
        List<FrontPageReactions.Line> all = new ArrayList<>();
        WORDS.stances().values().forEach(s -> {
            all.addAll(s.quoted());
            all.addAll(s.plain());
            all.addAll(s.also());
        });
        all.addAll(WORDS.declined().one());
        all.addAll(WORDS.declined().many());
        WORDS.firsts().forEach(f -> f.details().values().forEach(all::addAll));
        WORDS.links().forEach(l -> all.add(new FrontPageReactions.Line(l.id(), l.text())));
        return all;
    }

    private static String longest(List<String> texts, Font font, FontRenderContext context) {
        return texts.stream().max(Comparator.comparingDouble(t -> font.getStringBounds(fill(t), context).getWidth()))
            .orElse("");
    }

    private static int count(String text, String part) {
        int count = 0;
        for (int i = text.indexOf(part); i >= 0; i = text.indexOf(part, i + 1)) {
            count++;
        }
        return count;
    }

    private static int lines(String text, Font font, FontRenderContext context) {
        int lines = 1;
        float indent = (float) font.getStringBounds("    ", context).getWidth();
        StringBuilder line = new StringBuilder();
        for (String word : text.strip().split(" ")) {
            String trial = line.isEmpty() ? word : line + " " + word;
            float width = (float) font.getStringBounds(trial, context).getWidth() + (lines == 1 ? indent : 0f);
            if (!line.isEmpty() && width > COLUMN) {
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
}
