package de.mkoehler.launchwindow.client.ui;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

/**
 * A dashed outline over an optional fill, or faint diagonal hatching under it: the shapes that mark
 * something as not quite the player's own ({@code ui-spec.md} &sect;4.15, the not-yet-analysed mark,
 * the foreign file's strip and its read-only box). Drawn from flat rectangles, so it stays sharp.
 * Hatching costs a quad per stripe per pixel of height, so keep it to small areas.
 *
 * @author Mario Koehler
 */
public final class DashedBox extends BaseDrawable {

    private static final float DASH = 4f;
    private static final float GAP = 3f;
    private static final float HATCH_PITCH = 14f;
    private static final float HATCH_WIDTH = 6f;
    private static final float HATCH_STEP = 1f;

    private final Drawable fill;
    private final Drawable line;
    private final Drawable hatch;

    /**
     * Creates the shape.
     *
     * @param fill  the fill, or {@code null}
     * @param line  the dashes' flat colour, or {@code null} for no outline
     * @param hatch the hatching's flat colour, or {@code null} for none
     */
    public DashedBox(Drawable fill, Drawable line, Drawable hatch) {
        this.fill = fill;
        this.line = line;
        this.hatch = hatch;
    }

    @Override
    public void draw(Batch batch, float x, float y, float width, float height) {
        if (fill != null) {
            fill.draw(batch, x, y, width, height);
        }
        if (hatch != null) {
            // Each stripe is a stack of short runs, one step up and one step along.
            for (float start = -height; start < width; start += HATCH_PITCH) {
                for (float up = 0f; up < height; up += HATCH_STEP) {
                    float from = start + up;
                    float left = Math.max(from, 0f);
                    float right = Math.min(from + HATCH_WIDTH, width);
                    if (right > left) {
                        hatch.draw(batch, x + left, y + up, right - left, HATCH_STEP);
                    }
                }
            }
        }
        if (line != null) {
            for (float along = 0f; along < width; along += DASH + GAP) {
                float run = Math.min(DASH, width - along);
                line.draw(batch, x + along, y, run, 1f);
                line.draw(batch, x + along, y + height - 1f, run, 1f);
            }
            for (float along = 0f; along < height; along += DASH + GAP) {
                float run = Math.min(DASH, height - along);
                line.draw(batch, x, y + along, 1f, run);
                line.draw(batch, x + width - 1f, y + along, 1f, run);
            }
        }
    }
}
