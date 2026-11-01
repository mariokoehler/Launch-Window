package de.mkoehler.launchwindow.client.view;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.ContentPackLoader;
import de.mkoehler.launchwindow.content.Theories;
import de.mkoehler.launchwindow.model.FrontPage;
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
 * Tests the bundled explainers ({@code ui-spec.md} component 18): every one loads, every sheet has
 * its three points and its drawing (picture and labels), and every piece of text, the drawings'
 * included, uses only letters the fonts can draw.
 *
 * @author Mario Koehler
 */
class ExplainersTest {

    private static final Path ASSETS = Path.of("..", "assets");

    @Test
    void theBundledExplainersAreWellFormed() throws IOException {
        Explainers explainers = Explainers.parse(Files.readString(ASSETS.resolve("content").resolve(Explainers.FILE)));
        assertFalse(explainers.explainers().isEmpty());
        Set<String> ids = new HashSet<>();
        for (Explainers.Explainer explainer : explainers.explainers()) {
            assertTrue(ids.add(explainer.id()), explainer.id());
            assertFalse(explainer.sheets().isEmpty(), explainer.id());
            List<String> text = new ArrayList<>(List.of(explainer.code(), explainer.title(), explainer.eyebrowTopic(),
                explainer.summary()));
            List<String> drawings = new ArrayList<>();
            for (Explainers.Sheet sheet : explainer.sheets()) {
                assertEquals(3, sheet.points().size(), explainer.id() + ": " + sheet.title());
                text.addAll(List.of(sheet.title(), sheet.scale(), sheet.inGame(),
                    "‹ Previous: " + sheet.title(), "Next: " + sheet.title() + " ›"));
                drawings.add(sheet.drawing());
                for (Explainers.Point point : sheet.points()) {
                    text.addAll(List.of(point.title(), point.text()));
                    if (point.drawing() != null) {
                        drawings.add(point.drawing());
                    }
                }
                sheet.legend().forEach(entry -> text.add(entry.text()));
                for (Explainers.BarGroup group : sheet.bars()) {
                    assertTrue(group.max() > 0, group.label());
                    text.add(group.label());
                    group.rows().forEach(row -> {
                        assertTrue(row.value() > 0 && row.value() <= group.max(), row.text());
                        text.add(row.text());
                    });
                }
            }
            assertTrue(Files.exists(ASSETS.resolve(Explainers.DRAWINGS).resolve(drawings.get(0) + "-thumb.png")),
                drawings.get(0) + "-thumb.png");
            for (String drawing : drawings) {
                Path folder = ASSETS.resolve(Explainers.DRAWINGS);
                assertTrue(Files.exists(folder.resolve(drawing + ".png")), drawing + ".png");
                Explainers.Drawing labels = new ObjectMapper().readValue(
                    Files.readString(folder.resolve(drawing + ".json")), Explainers.Drawing.class);
                assertTrue(labels.width() > 0 && labels.height() > 0, drawing);
                labels.labels().forEach(label -> text.add(label.text()));
            }
            for (String each : text) {
                assertTrue(each != null && !each.isBlank(), explainer.id() + ": blank text");
                each.chars().forEach(c -> assertTrue(c == ' ' || Theme.CHARACTERS.indexOf(c) >= 0,
                    explainer.id() + ": '" + (char) c + "' in " + each));
            }
        }
    }

    /**
     * The explainers that answer a theory of the day name it, and only theories the standard pack can settle
     * (2026-10-27): the radiation belts, weightlessness, and the Moon's dust and Venus's heat.
     */
    @Test
    void theExplainersThatAnswerATheoryWaitForIt() throws IOException {
        Explainers explainers = Explainers.parse(Files.readString(ASSETS.resolve("content").resolve(Explainers.FILE)));
        Theories theories = Theories.load(ASSETS.resolve("content").resolve(Theories.FILE));
        ContentCatalog standard = ContentPackLoader.load(ASSETS.resolve("content").resolve("standard"));
        for (Explainers.Explainer explainer : explainers.explainers()) {
            for (String id : explainer.lockedUntil()) {
                Theories.Theory theory = theories.theory(id).orElseThrow(() -> new AssertionError(explainer.id() + ": " + id));
                assertTrue(theory.playable(goal -> standard.goal(goal).isPresent()), explainer.id() + ": " + id);
            }
        }
        assertEquals(List.of("radiation-belts"), explainers.byId("space-environment").orElseThrow().lockedUntil());
        assertEquals(List.of("weightless-body"), explainers.byId("weightlessness").orElseThrow().lockedUntil());
        assertEquals(List.of("deep-dust", "venus-jungle"),
            explainers.byId("landing-on-other-worlds").orElseThrow().lockedUntil());
    }

    /**
     * An explainer stays locked until a science page settles every theory it waits for, unless the pack can't
     * settle one or the race is decided; with no pages fetched it stays locked.
     */
    @Test
    void anExplainerOpensWhenItsTheoriesAreSettled() throws IOException {
        Theories theories = Theories.load(ASSETS.resolve("content").resolve(Theories.FILE));
        Explainers.Explainer both = new Explainers.Explainer("both", "E-99", "Both", null, "", List.of(),
            List.of("deep-dust", "venus-jungle"));
        java.util.function.Predicate<String> all = goal -> true;
        FrontPage dust = science("deep-dust");
        FrontPage venus = science("venus-jungle");
        assertTrue(Explainers.locked(both, theories, null, all, false));
        assertTrue(Explainers.locked(both, theories, List.of(), all, false));
        assertTrue(Explainers.locked(both, theories, List.of(dust), all, false));
        assertFalse(Explainers.locked(both, theories, List.of(dust, venus), all, false));
        assertFalse(Explainers.locked(both, theories, List.of(), all, true), "the race is decided");
        // A pack with no Venus can't settle that theory, so only the dust counts.
        assertFalse(Explainers.locked(both, theories, List.of(dust), goal -> !goal.startsWith("venus"), false));
        Explainers.Explainer none = new Explainers.Explainer("none", "E-98", "None", null, "", List.of(), null);
        assertFalse(Explainers.locked(none, theories, null, all, false));
    }

    private static FrontPage science(String theory) {
        return new FrontPage("1-science-" + theory, "goal", 1, theory, "", List.of(), List.of(), List.of(),
            FrontPage.Kind.SCIENCE, null, null);
    }
}
