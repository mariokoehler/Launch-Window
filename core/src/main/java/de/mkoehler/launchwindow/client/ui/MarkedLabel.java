package de.mkoehler.launchwindow.client.ui;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.FloatArray;

import java.util.List;

/**
 * A wrapping, top-left label that marks stretches of its text with a highlighter's stroke behind the
 * glyphs, as the help marks what a search found ({@code ui-spec.md} component 23). The stretches are
 * character ranges of the text; the label finds their glyphs in its own layout, so wrapping, the
 * window's scale and the font's rebuilds all follow.
 *
 * @author Mario Koehler
 */
public final class MarkedLabel extends Label {

    private final List<int[]> marks;
    private final Drawable marker;

    /**
     * Builds the label.
     *
     * @param text   the text
     * @param style  the label's style
     * @param marks  the stretches to mark, each {@code {start, end}} (end exclusive), in order
     * @param marker what a mark is drawn with, such as a translucent solid
     */
    public MarkedLabel(String text, LabelStyle style, List<int[]> marks, Drawable marker) {
        super(text, style);
        this.marks = List.copyOf(marks);
        this.marker = marker;
        setWrap(true);
        setAlignment(Align.topLeft);
    }

    /**
     * Returns whether anything is marked.
     *
     * @return whether there are marks
     */
    public boolean marked() {
        return !marks.isEmpty();
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        validate();
        if (!marks.isEmpty()) {
            float before = batch.getPackedColor();
            batch.setColor(1f, 1f, 1f, getColor().a * parentAlpha);
            drawMarks(batch);
            batch.setPackedColor(before);
        }
        super.draw(batch, parentAlpha);
    }

    private void drawMarks(Batch batch) {
        BitmapFont font = getStyle().font;
        CharSequence text = getText();
        // Where Label.layout puts the text when top-aligned without a background: the first line's cap top.
        float top = getY() + getHeight() + font.getDescent();
        float capHeight = font.getCapHeight();
        float descent = -font.getDescent();
        int at = 0;
        for (GlyphLayout.GlyphRun run : getGlyphLayout().runs) {
            FloatArray advances = run.xAdvances;
            float x = getX() + run.x + (advances.size > 0 ? advances.get(0) : 0f);
            float capTop = top + run.y;
            float from = Float.NaN;
            float to = 0f;
            for (int k = 0; k < run.glyphs.size; k++) {
                int id = run.glyphs.get(k).id;
                // Wrapping drops the spaces where lines break: step over them to find this glyph's character.
                while (at < text.length() && text.charAt(at) != id && isSpace(text.charAt(at))) {
                    at++;
                }
                float advance = k + 1 < advances.size ? advances.get(k + 1) : 0f;
                if (at < text.length() && marked(at)) {
                    if (Float.isNaN(from)) {
                        from = x;
                    }
                    to = x + advance;
                } else if (!Float.isNaN(from)) {
                    stroke(batch, from, to, capTop, capHeight, descent);
                    from = Float.NaN;
                }
                x += advance;
                at++;
            }
            if (!Float.isNaN(from)) {
                stroke(batch, from, to, capTop, capHeight, descent);
            }
        }
    }

    private void stroke(Batch batch, float from, float to, float capTop, float capHeight, float descent) {
        // From just under the descenders to a little over the ascenders, so lines' strokes don't touch.
        float bottom = capTop - capHeight - descent * 0.9f;
        float height = capHeight + descent * 0.9f + descent * 0.6f;
        float pad = descent * 0.25f;
        marker.draw(batch, from - pad, bottom, to - from + 2 * pad, height);
    }

    private boolean marked(int index) {
        for (int[] mark : marks) {
            if (index >= mark[0] && index < mark[1]) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSpace(char c) {
        return Character.isWhitespace(c) || Character.isSpaceChar(c);
    }
}
