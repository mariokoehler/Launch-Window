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
 * next season, and a second, dashed gold marker for a cap a revealed Perfectionist raises while on
 * the team (People canvas, 2026-10-07). As a <em>range</em> (Intelligence Office canvas, 2026-10-07)
 * it shows what intelligence learned about a rival's component: a navy band at 42 % between the
 * bounds with a solid 2 px end at each, no fill from zero, the gold cap kept (the cap is the content
 * pack's, not a secret). An R&amp;D board can also mark milestones (2026-10-10): thin navy ticks at
 * reliabilities still ahead, with {@link MilestoneLabels} above the bar naming the seasons to each.
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
    private float raisedCap;
    private float rangeLow = Float.NaN;
    private float rangeHigh;
    private Drawable band;
    private final Drawable milestoneTick;
    private float[] milestones = new float[0];

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
        this.milestoneTick = ui.solid(new Color(Theme.NAVY.r, Theme.NAVY.g, Theme.NAVY.b, 0.55f));
    }

    /**
     * Marks milestones with thin ticks across the track.
     *
     * @param values the reliabilities to mark; the cap needs none, it has its own marker
     * @return this bar
     */
    public ReliabilityBar milestones(double... values) {
        this.milestones = new float[values.length];
        for (int i = 0; i < values.length; i++) {
            this.milestones[i] = (float) values[i];
        }
        return this;
    }

    /**
     * Returns the reliability at the bar's end, for widgets lined up with it.
     *
     * @return the maximum
     */
    public float max() {
        return max;
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

    /**
     * Shows a raised R&amp;D cap as a second, dashed marker, and counts the fill as capped only
     * once it reaches that cap.
     *
     * @param cap the raised cap; nothing is drawn unless it's above the normal one
     * @return this bar
     */
    public ReliabilityBar raisedCap(double cap) {
        this.raisedCap = (float) cap;
        return this;
    }

    /**
     * Draws the bar as a range instead of a fill.
     *
     * @param ui   the widget kit
     * @param low  the range's low end
     * @param high the range's high end
     * @return this bar
     */
    public ReliabilityBar range(UiKit ui, double low, double high) {
        this.rangeLow = (float) low;
        this.rangeHigh = (float) high;
        this.band = ui.solid(new Color(Theme.NAVY.r, Theme.NAVY.g, Theme.NAVY.b, 0.42f));
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
        if (!Float.isNaN(rangeLow)) {
            float lowX = getX() + width * Math.min(1f, rangeLow / max);
            float highX = getX() + width * Math.min(1f, rangeHigh / max);
            band.draw(batch, lowX, trackY, Math.max(2f, highX - lowX), TRACK_HEIGHT);
            fill.draw(batch, lowX, trackY, 2f, TRACK_HEIGHT);
            fill.draw(batch, highX - 2f, trackY, 2f, TRACK_HEIGHT);
            float capX = getX() + width * Math.min(1f, rdCap / max);
            capMarker.draw(batch, capX - 1f, getY() + (getHeight() - CAP_MARKER_HEIGHT) / 2f, 2f, CAP_MARKER_HEIGHT);
            return;
        }
        boolean raised = raisedCap > rdCap;
        boolean capped = current >= (raised ? raisedCap : rdCap);
        if (projected > current) {
            projectedFill.draw(batch, getX(), trackY, width * Math.min(1f, projected / max), TRACK_HEIGHT);
        }
        (capped ? cappedFill : fill).draw(batch, getX(), trackY, width * Math.min(1f, current / max), TRACK_HEIGHT);
        for (float milestone : milestones) {
            float x = getX() + width * Math.min(1f, milestone / max);
            milestoneTick.draw(batch, x - 0.5f, getY() + (getHeight() - FLOOR_MARKER_HEIGHT) / 2f, 1f,
                FLOOR_MARKER_HEIGHT);
        }
        float floorX = getX() + width * Math.min(1f, floor / max);
        floorMarker.draw(batch, floorX, getY() + (getHeight() - FLOOR_MARKER_HEIGHT) / 2f, 1f, FLOOR_MARKER_HEIGHT);
        float capX = getX() + width * Math.min(1f, rdCap / max);
        capMarker.draw(batch, capX - 1f, getY() + (getHeight() - CAP_MARKER_HEIGHT) / 2f, 2f, CAP_MARKER_HEIGHT);
        if (raised) {
            // Dashed: three 3 px dashes with 2.5 px gaps over the marker's height.
            float raisedX = getX() + width * Math.min(1f, raisedCap / max);
            float y = getY() + (getHeight() - CAP_MARKER_HEIGHT) / 2f;
            for (int dash = 0; dash < 3; dash++) {
                capMarker.draw(batch, raisedX - 1f, y + dash * 5.5f, 2f, 3f);
            }
        }
    }
}
