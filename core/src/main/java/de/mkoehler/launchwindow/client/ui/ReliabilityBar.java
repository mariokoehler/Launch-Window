package de.mkoehler.launchwindow.client.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

/**
 * The reliability bar ({@code ui-spec.md} component 3): a thin track whose end is the component's
 * absolute maximum, filled to its current reliability, with a gold marker at its R&amp;D ceiling and
 * a dim marker at its floor. The fill turns green once the R&amp;D ceiling is reached. An R&amp;D
 * board can add a lighter segment after the fill: the reliability its engineers are expected to add
 * next season.
 *
 * @author Mario Koehler
 */
public final class ReliabilityBar extends Widget {

    private static final float TRACK_HEIGHT = 8f;
    private static final float CAP_MARKER_HEIGHT = 14f;
    private static final float FLOOR_MARKER_HEIGHT = 12f;

    private final Drawable track;
    private final Drawable fill;
    private final Drawable cappedFill;
    private final Drawable capMarker;
    private final Drawable floorMarker;
    private final float current;
    private final float rdCap;
    private final float max;
    private final float floor;
    private final Drawable projectedFill;
    private float projected;

    /**
     * Builds the bar.
     *
     * @param ui      the widget kit
     * @param current the current reliability
     * @param rdCap   the R&amp;D ceiling ({@code maxRdReliability})
     * @param max     the absolute maximum ({@code maxReliability}), the bar's end
     * @param floor   the floor reliability
     */
    public ReliabilityBar(UiKit ui, double current, double rdCap, double max, double floor) {
        this.track = ui.solid(Theme.SKILL_TRACK);
        this.fill = ui.solid(Theme.NAVY);
        this.cappedFill = ui.solid(Theme.SUCCESS);
        this.capMarker = ui.solid(Theme.GOLD);
        this.floorMarker = ui.solid(new Color(Theme.INK_MUTED.r, Theme.INK_MUTED.g, Theme.INK_MUTED.b, 0.5f));
        this.current = (float) current;
        this.rdCap = (float) rdCap;
        this.max = (float) Math.max(max, 0.0001);
        this.floor = (float) floor;
        this.projectedFill = ui.solid(new Color(Theme.NAVY.r, Theme.NAVY.g, Theme.NAVY.b, 0.3f));
        this.projected = this.current;
    }

    /**
     * Shows the reliability expected after next season as a lighter segment after the fill.
     *
     * @param expected the expected reliability, at least the current one
     * @return this bar
     */
    public ReliabilityBar projected(double expected) {
        this.projected = (float) Math.max(current, expected);
        return this;
    }

    @Override
    public float getPrefHeight() {
        return CAP_MARKER_HEIGHT;
    }

    @Override
    public float getPrefWidth() {
        return 200f;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        Color color = getColor();
        batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);
        float width = getWidth();
        float trackY = getY() + (getHeight() - TRACK_HEIGHT) / 2f;
        track.draw(batch, getX(), trackY, width, TRACK_HEIGHT);
        boolean capped = current >= rdCap;
        if (projected > current) {
            projectedFill.draw(batch, getX(), trackY, width * Math.min(1f, projected / max), TRACK_HEIGHT);
        }
        (capped ? cappedFill : fill).draw(batch, getX(), trackY, width * Math.min(1f, current / max), TRACK_HEIGHT);
        float floorX = getX() + width * Math.min(1f, floor / max);
        floorMarker.draw(batch, floorX, getY() + (getHeight() - FLOOR_MARKER_HEIGHT) / 2f, 1f, FLOOR_MARKER_HEIGHT);
        float capX = getX() + width * Math.min(1f, rdCap / max);
        capMarker.draw(batch, capX - 1f, getY() + (getHeight() - CAP_MARKER_HEIGHT) / 2f, 2f, CAP_MARKER_HEIGHT);
    }
}
