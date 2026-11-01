package de.mkoehler.launchwindow.lwjgl3;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.BitmapFontCache;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Finds text that doesn't fit on a rendered screen, for {@link ScreenSnapshot}: every visible
 * label's drawn glyphs are measured, and three things are reported. Text that overlaps another
 * label's text on the same layer, text that leaves the box it sits in (the nearest card, button or
 * badge with a background) or the screen, and text cut short with an ellipsis. Text scrolled out of
 * a scroll pane is clipped first, so a row half under a pane's edge is no problem.
 * <p>
 * Made to measure what a larger text size ({@code -Dui.textScale}) breaks: run it at 1 and at the
 * larger size, and the problems only the larger size has are the work. Text drawn straight with a
 * font by a custom widget (the front pages, the replay console) isn't a label and isn't checked.
 *
 * @author Mario Koehler
 */
final class TextFit {

    /** Layout units a glyph may stray before it counts: the glyph quads carry a little padding. */
    private static final float TOLERANCE = 1f;

    /** One problem on one screen. */
    record Issue(String kind, String text, Rectangle where, String other) {

        String line(String shot) {
            return String.format(Locale.ROOT, "%s\t%s\t%s\t%.0f,%.0f %.0fx%.0f\t%s", shot, kind, clean(text),
                where.x, where.y, where.width, where.height, other == null ? "" : clean(other));
        }

        private static String clean(String text) {
            String flat = text.replace('\n', ' ').replace('\t', ' ');
            return flat.length() > 60 ? flat.substring(0, 57) + "..." : flat;
        }
    }

    /** A label's drawn text: where its glyphs are, in stage coordinates, after clipping. */
    private record Text(Label label, Rectangle drawn, Actor layer) {
    }

    private TextFit() {
    }

    /**
     * Checks every visible label on a stage.
     *
     * @param stage the stage, drawn at least once since its last change
     * @return the problems found, empty when every text fits
     */
    static List<Issue> check(Stage stage) {
        List<Text> texts = new ArrayList<>();
        for (Actor layer : stage.getActors()) {
            collect(layer, layer, texts);
        }
        List<Issue> issues = new ArrayList<>();
        float stageWidth = stage.getViewport().getWorldWidth();
        float stageHeight = stage.getViewport().getWorldHeight();
        for (Text text : texts) {
            String content = text.label.getText().toString();
            if (truncated(text.label)) {
                issues.add(new Issue("TRUNCATED", content, text.drawn, null));
            }
            Rectangle box = boxOf(text.label);
            if (box != null && !inside(text.drawn, box)) {
                issues.add(new Issue("ESCAPES", content, text.drawn, describe(text.label, box)));
            } else if (!inside(text.drawn, new Rectangle(0, 0, stageWidth, stageHeight))) {
                issues.add(new Issue("OFFSCREEN", content, text.drawn, null));
            }
        }
        for (ScrollPane pane : panes(stage.getRoot(), new ArrayList<>())) {
            // Content that no longer fits its pane: a page that has to scroll now, or cards cut off.
            if (pane.getMaxX() > TOLERANCE || pane.getMaxY() > TOLERANCE) {
                issues.add(new Issue("SCROLLS", firstText(pane), stageBounds(pane),
                    String.format(Locale.ROOT, "%.0f x %.0f more", pane.getMaxX(), pane.getMaxY())));
            }
        }
        for (int i = 0; i < texts.size(); i++) {
            for (int j = i + 1; j < texts.size(); j++) {
                Text a = texts.get(i);
                Text b = texts.get(j);
                if (a.layer == b.layer && overlap(a.drawn, b.drawn)) {
                    issues.add(new Issue("OVERLAP", a.label.getText().toString(), a.drawn,
                        b.label.getText().toString()));
                }
            }
        }
        return issues;
    }

    /**
     * Outlines every problem on a screenshot, in red, for looking at by eye.
     *
     * @param pixmap the screenshot, upright, at the stage's own size or a multiple of it
     * @param stageHeight the stage's height in layout units
     * @param issues the problems found on it
     */
    static void mark(Pixmap pixmap, float stageHeight, List<Issue> issues) {
        float scale = pixmap.getHeight() / stageHeight;
        pixmap.setColor(1f, 0f, 0f, 1f);
        for (Issue issue : issues) {
            Rectangle r = issue.where;
            int x = Math.round(r.x * scale) - 2;
            int y = Math.round((stageHeight - r.y - r.height) * scale) - 2;
            int w = Math.round(r.width * scale) + 4;
            int h = Math.round(r.height * scale) + 4;
            pixmap.drawRectangle(x, y, w, h);
            pixmap.drawRectangle(x - 1, y - 1, w + 2, h + 2);
        }
    }

    private static void collect(Actor actor, Actor layer, List<Text> texts) {
        if (!actor.isVisible() || actor.getColor().a <= 0f) {
            return;
        }
        if (actor instanceof Label label && label.getText().length() > 0) {
            Rectangle drawn = drawnText(label);
            if (drawn != null && clip(label, drawn)) {
                texts.add(new Text(label, drawn, layer));
            }
        }
        if (actor instanceof Group group) {
            for (Actor child : group.getChildren()) {
                collect(child, layer, texts);
            }
        }
    }

    private static List<ScrollPane> panes(Actor actor, List<ScrollPane> found) {
        if (!actor.isVisible()) {
            return found;
        }
        if (actor instanceof ScrollPane pane) {
            pane.validate();
            found.add(pane);
        }
        if (actor instanceof Group group) {
            for (Actor child : group.getChildren()) {
                panes(child, found);
            }
        }
        return found;
    }

    /** The first text in a widget, to name it by in the report. */
    private static String firstText(Actor actor) {
        if (actor instanceof Label label && label.getText().length() > 0) {
            return label.getText().toString();
        }
        if (actor instanceof Group group) {
            for (Actor child : group.getChildren()) {
                String text = firstText(child);
                if (!text.isEmpty()) {
                    return text;
                }
            }
        }
        return "";
    }

    /** Where a label's glyphs are, in stage coordinates, from the font cache it last drew with. */
    private static Rectangle drawnText(Label label) {
        label.validate();
        BitmapFontCache cache = cacheOf(label);
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        int pages = cache.getFont().getRegions().size;
        for (int page = 0; page < pages; page++) {
            float[] vertices = cache.getVertices(page);
            int count = cache.getVertexCount(page);
            for (int v = 0; v < count; v += 5) {
                minX = Math.min(minX, vertices[v]);
                maxX = Math.max(maxX, vertices[v]);
                minY = Math.min(minY, vertices[v + 1]);
                maxY = Math.max(maxY, vertices[v + 1]);
            }
        }
        if (minX > maxX) {
            return null; // only spaces
        }
        // The cache holds the glyphs moved to where the label last drew; back to the label's own corner.
        Vector2 low = label.localToStageCoordinates(new Vector2(minX - cache.getX(), minY - cache.getY()));
        Vector2 high = label.localToStageCoordinates(new Vector2(maxX - cache.getX(), maxY - cache.getY()));
        return new Rectangle(low.x, low.y, high.x - low.x, high.y - low.y);
    }

    /** A label's font cache, which libGDX keeps protected: the glyphs exactly where the label draws them. */
    private static BitmapFontCache cacheOf(Label label) {
        try {
            java.lang.reflect.Method method = Label.class.getDeclaredMethod("getBitmapFontCache");
            method.setAccessible(true);
            return (BitmapFontCache) method.invoke(label);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Clips the drawn text to every scroll pane it sits in; false when nothing of it shows. */
    private static boolean clip(Label label, Rectangle drawn) {
        for (Group parent = label.getParent(); parent != null; parent = parent.getParent()) {
            if (parent instanceof ScrollPane pane) {
                Rectangle view = stageBounds(pane);
                if (!drawn.overlaps(view)) {
                    return false;
                }
                float x = Math.max(drawn.x, view.x);
                float y = Math.max(drawn.y, view.y);
                float right = Math.min(drawn.x + drawn.width, view.x + view.width);
                float top = Math.min(drawn.y + drawn.height, view.y + view.height);
                drawn.set(x, y, right - x, top - y);
            }
        }
        return drawn.width > 0 && drawn.height > 0;
    }

    /**
     * The nearest box with a background a label sits in (its own, a badge's, a button's or a card's),
     * in stage coordinates; null below a scroll pane's edge or straight on the screen's page.
     */
    private static Rectangle boxOf(Label label) {
        if (label.getStyle().background != null) {
            return stageBounds(label);
        }
        Stage stage = label.getStage();
        for (Group parent = label.getParent(); parent != null; parent = parent.getParent()) {
            if (parent instanceof ScrollPane) {
                return null;
            }
            Drawable background = parent instanceof Table table ? table.getBackground()
                : parent instanceof Container<?> container ? container.getBackground() : null;
            if (background != null) {
                Rectangle bounds = stageBounds(parent);
                // The screen's own page (the blueprint grid under everything) is the screen check's job.
                boolean page = bounds.width >= stage.getWidth() && bounds.height >= stage.getHeight();
                return page ? null : bounds;
            }
        }
        return null;
    }

    private static Rectangle stageBounds(Actor actor) {
        Vector2 low = actor.localToStageCoordinates(new Vector2(0, 0));
        Vector2 high = actor.localToStageCoordinates(new Vector2(actor.getWidth(), actor.getHeight()));
        return new Rectangle(Math.min(low.x, high.x), Math.min(low.y, high.y),
            Math.abs(high.x - low.x), Math.abs(high.y - low.y));
    }

    /** Whether a label's text was cut short: it ends in an ellipsis its own text doesn't have. */
    private static boolean truncated(Label label) {
        StringBuilder shown = new StringBuilder();
        for (GlyphLayout.GlyphRun run : label.getGlyphLayout().runs) {
            for (int i = 0; i < run.glyphs.size; i++) {
                shown.append((char) run.glyphs.get(i).id);
            }
        }
        String text = label.getText().toString().strip();
        return shown.toString().endsWith("...") && !text.endsWith("...");
    }

    private static boolean inside(Rectangle inner, Rectangle outer) {
        return inner.x >= outer.x - TOLERANCE && inner.y >= outer.y - TOLERANCE
            && inner.x + inner.width <= outer.x + outer.width + TOLERANCE
            && inner.y + inner.height <= outer.y + outer.height + TOLERANCE;
    }

    private static boolean overlap(Rectangle a, Rectangle b) {
        float w = Math.min(a.x + a.width, b.x + b.width) - Math.max(a.x, b.x);
        float h = Math.min(a.y + a.height, b.y + b.height) - Math.max(a.y, b.y);
        return w > TOLERANCE && h > TOLERANCE;
    }

    private static String describe(Label label, Rectangle box) {
        return String.format(Locale.ROOT, "box %.0f,%.0f %.0fx%.0f", box.x, box.y, box.width, box.height);
    }
}
