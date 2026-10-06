package de.mkoehler.launchwindow.client.view;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The Museum encyclopedia's explainers ({@code ui-spec.md} component 18, {@code design.md}
 * &sect;1.1): how things work, drawn as a short series of engineering sheets. The text lives in
 * {@code assets/content/explainers.json}, outside the frozen content pack, so fixes reach running
 * games; each sheet's drawing is a picture under {@code assets/ui/explainers/} with its text in a
 * labels file beside it, so the client sets that text as sharply as any other.
 *
 * @param explainers every explainer, in the Museum's order
 * @author Mario Koehler
 */
@JsonIgnoreProperties({"_comment"})
public record Explainers(List<Explainer> explainers) {

    /** The explainers file, under the assets' {@code content/} folder. */
    public static final String FILE = "explainers.json";

    /** Where the drawings and their labels live, under the assets folder. */
    public static final String DRAWINGS = "ui/explainers/";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static volatile Explainers standard;

    /**
     * Copies the list.
     */
    public Explainers {
        explainers = explainers == null ? List.of() : List.copyOf(explainers);
    }

    /**
     * One explainer: a short series of sheets on one idea.
     *
     * @param id      a stable id
     * @param code    the drawing number in the title block, such as "E-01"
     * @param title   the idea, such as "Suborbital vs orbital"
     * @param summary one sentence for the Museum's list
     * @param sheets  the sheets, in reading order
     */
    public record Explainer(String id, String code, String title, String summary, List<Sheet> sheets) {

        /**
         * Checks the fields.
         */
        public Explainer {
            Objects.requireNonNull(id, "id must not be null");
            sheets = sheets == null ? List.of() : List.copyOf(sheets);
        }
    }

    /**
     * One sheet: a drawing, three numbered points and what it means in the game.
     *
     * @param title   the sheet's title
     * @param drawing the drawing's name under {@link #DRAWINGS}, without extension
     * @param scale   the title block's scale note, such as "Not to scale"
     * @param legend  a key under the drawing, possibly empty
     * @param bars    a bar chart under the drawing, possibly empty
     * @param points  the numbered points, the last one gold
     * @param inGame  the "In Launch Window" line
     */
    public record Sheet(String title, String drawing, String scale, List<LegendEntry> legend, List<BarGroup> bars,
                        List<Point> points, String inGame) {

        /**
         * Copies the lists; missing ones read as empty.
         */
        public Sheet {
            legend = legend == null ? List.of() : List.copyOf(legend);
            bars = bars == null ? List.of() : List.copyOf(bars);
            points = points == null ? List.of() : List.copyOf(points);
        }
    }

    /**
     * A numbered point.
     *
     * @param title   its title
     * @param text    two to four sentences
     * @param drawing a small drawing under the text, by name under {@link #DRAWINGS}, or {@code null}
     */
    public record Point(String title, String text, String drawing) {
    }

    /**
     * A key entry: a navy or gold swatch and its text.
     *
     * @param gold whether the swatch is gold, else navy
     * @param text the text
     */
    public record LegendEntry(boolean gold, String text) {
    }

    /**
     * One measure compared in a bar chart: a navy bar, then a gold one.
     *
     * @param label the measure, such as "Highest point"
     * @param max   the value a full-length bar stands for
     * @param rows  the bars, navy first
     */
    public record BarGroup(String label, double max, List<BarRow> rows) {

        /**
         * Copies the rows.
         */
        public BarGroup {
            rows = rows == null ? List.of() : List.copyOf(rows);
        }
    }

    /**
     * One bar.
     *
     * @param value its value, against the group's max
     * @param text  the words beside it
     */
    public record BarRow(double value, String text) {
    }

    /**
     * A drawing's text, placed over the picture by the client.
     *
     * @param width  the drawing's width in layout units
     * @param height its height
     * @param labels the text
     */
    public record Drawing(int width, int height, List<DrawingLabel> labels) {

        /**
         * Copies the labels.
         */
        public Drawing {
            labels = labels == null ? List.of() : List.copyOf(labels);
        }
    }

    /**
     * One piece of a drawing's text, as the converter found it in the drawing's source.
     *
     * @param x      where it is anchored, from the drawing's left
     * @param y      its baseline, from the drawing's top
     * @param anchor "start", "middle" or "end"
     * @param kind   its look: the source's class (d-lbl, d-lbl-gold, d-txt, d-sm, d-head, d-big) or
     *               "num" for the digit in a numbered circle
     * @param size   a font size the source sets, or 0 for the kind's own
     * @param color  its color, as #rrggbb
     * @param text   the text
     */
    public record DrawingLabel(float x, float y, String anchor, String kind, float size, String color, String text) {
    }

    /**
     * Returns the explainers bundled with the game, read once from the classpath.
     *
     * @return the explainers, or none if the file isn't there
     */
    public static Explainers standard() {
        Explainers loaded = standard;
        if (loaded == null) {
            loaded = read("content/" + FILE, Explainers.class);
            standard = loaded == null ? new Explainers(List.of()) : loaded;
        }
        return standard;
    }

    /**
     * Reads explainers from JSON text.
     *
     * @param json the text
     * @return the explainers
     */
    public static Explainers parse(String json) {
        try {
            return MAPPER.readValue(json, Explainers.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the explainers", e);
        }
    }

    /**
     * Reads a drawing's text from the classpath.
     *
     * @param name the drawing's name, such as "e01-1"
     * @return the drawing, or empty if its labels file isn't there
     */
    public static Optional<Drawing> drawing(String name) {
        return Optional.ofNullable(read(DRAWINGS + name + ".json", Drawing.class));
    }

    /**
     * Returns an explainer by id.
     *
     * @param id the id
     * @return the explainer, or empty
     */
    public Optional<Explainer> byId(String id) {
        return explainers.stream().filter(explainer -> explainer.id().equals(id)).findFirst();
    }

    private static <T> T read(String path, Class<T> type) {
        try (InputStream in = Explainers.class.getClassLoader().getResourceAsStream(path)) {
            return in == null ? null : MAPPER.readValue(in, type);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + path, e);
        }
    }
}
