package de.mkoehler.launchwindow.client.view;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.client.ui.Theme;
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
}
