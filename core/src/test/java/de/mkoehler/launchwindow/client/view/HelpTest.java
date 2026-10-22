package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.content.BudgetSchedule;
import de.mkoehler.launchwindow.content.BuildingSchedule;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the bundled help ({@code ui-spec.md} component 23): ids are unique, every token has a
 * value, every link and every screen's topic list names a topic or explainer that exists, the
 * fonts can draw every letter, and the filter works as specified.
 *
 * @author Mario Koehler
 */
class HelpTest {

    private static final Path CONTENT = Path.of("..", "assets", "content");

    private static Help bundled() throws IOException {
        return Help.parse(Files.readString(CONTENT.resolve(Help.FILE)));
    }

    @Test
    void theBundledHelpIsWellFormed() throws IOException {
        Help help = bundled();
        Explainers explainers = Explainers.parse(Files.readString(CONTENT.resolve(Explainers.FILE)));
        HelpValues values = HelpValues.of(BudgetSchedule.baspmClose(), BuildingSchedule.baspmClose());
        assertFalse(help.topics().isEmpty());
        Set<String> ids = new HashSet<>();
        for (Help.Topic topic : help.topics()) {
            assertTrue(ids.add(topic.id()), "duplicate id " + topic.id());
        }
        for (Help.Topic topic : help.topics()) {
            assertFalse(topic.body().isEmpty(), topic.id());
            topic.related().forEach(id -> assertTrue(ids.contains(id), topic.id() + " links to unknown topic " + id));
            assertFalse(topic.related().contains(topic.id()), topic.id() + " links to itself");
            topic.explainers().forEach(id -> assertTrue(explainers.byId(id).isPresent(),
                topic.id() + " links to unknown explainer " + id));
            List<String> text = new ArrayList<>(List.of(topic.id(), topic.group(), topic.title()));
            for (String line : topic.body()) {
                assertEquals(Set.of(), values.unknown(line), topic.id() + ": " + line);
                text.add(values.fill(Help.Line.of(line).text()));
            }
            for (String each : text) {
                assertTrue(each != null && !each.isBlank(), topic.id() + ": blank text");
                each.chars().forEach(c -> assertTrue(c == ' ' || Theme.CHARACTERS.indexOf(c) >= 0,
                    topic.id() + ": '" + (char) c + "' in " + each));
            }
        }
        for (Help.Context context : Help.Context.values()) {
            assertFalse(context.topicIds().isEmpty(), context.name());
            context.topicIds().forEach(id -> assertTrue(ids.contains(id), context + " names unknown topic " + id));
        }
    }

    /** F1's filter (component 23): every screen context names topics that exist, and every topic is on one. */
    @Test
    void everyContextNamesRealTopicsAndEveryTopicHasOne() throws IOException {
        Set<String> ids = new HashSet<>();
        bundled().topics().forEach(topic -> ids.add(topic.id()));
        Set<String> reached = new HashSet<>();
        for (Help.Context context : Help.Context.values()) {
            for (String id : context.topicIds()) {
                assertTrue(ids.contains(id), context + " names unknown topic " + id);
                reached.add(id);
            }
        }
        // The basics are the full list's: the hub opens help unfiltered.
        Set<String> unfiltered = Set.of("turns", "buildings");
        for (String id : ids) {
            assertTrue(reached.contains(id) || unfiltered.contains(id), "no screen's F1 shows " + id);
        }
    }

    @Test
    void aContextFiltersUntilThePlayerTypes() throws IOException {
        Help help = bundled();
        HelpValues values = HelpValues.of(BudgetSchedule.baspmClose(), BuildingSchedule.baspmClose());
        assertEquals(help.topics(), help.shown("", null, values));
        List<Help.Topic> intelligence = help.shown("  ", Help.Context.INTELLIGENCE, values);
        assertEquals(Set.copyOf(Help.Context.INTELLIGENCE.topicIds()),
            Set.copyOf(intelligence.stream().map(Help.Topic::id).toList()));
        // Typing searches every topic, not only the context's, ignoring case.
        List<Help.Topic> rapport = help.shown("RAPPORT", Help.Context.INTELLIGENCE, values);
        assertTrue(rapport.stream().anyMatch(topic -> topic.id().equals("chemistry")));
        assertTrue(help.shown("no topic says this", null, values).isEmpty());
    }

    @Test
    void searchFindsTheFilledNumbersNotTheTokens() throws IOException {
        Help help = bundled();
        HelpValues values = HelpValues.of(BudgetSchedule.baspmClose(), BuildingSchedule.baspmClose());
        assertTrue(help.shown("{", null, values).isEmpty());
        assertTrue(help.shown("minTenure", null, values).isEmpty());
        assertTrue(help.shown("16 seasons", null, values).stream().anyMatch(topic -> topic.id().equals("budget")));
    }

    @Test
    void linesKeepTheirKind() {
        assertEquals(new Help.Line(Help.Line.Kind.HEADING, "Odds"), Help.Line.of("## Odds"));
        assertEquals(new Help.Line(Help.Line.Kind.BULLET, "one"), Help.Line.of("- one"));
        assertEquals(new Help.Line(Help.Line.Kind.PARAGRAPH, "-10 points"), Help.Line.of("-10 points"));
        assertEquals("a 4 b {nope}", HelpValues.of(BudgetSchedule.baspmClose(), BuildingSchedule.baspmClose()).fill("a {rnd.slots} b {nope}"));
    }
}
