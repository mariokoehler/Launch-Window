package de.mkoehler.launchwindow.client.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

/**
 * A step's outcome odds as one bar in five colours ({@code ui-spec.md} &sect;4.6.7): success, glitch
 * solved, minor glitch, failure, catastrophic, each as wide as its chance.
 *
 * @author Mario Koehler
 */
public final class OutcomeBar extends Widget {

    /** The five colours, in outcome order. */
    public static final Color[] COLORS = {
        Theme.SUCCESS,
        new Color(Theme.SUCCESS).lerp(Color.WHITE, 0.55f),
        Theme.GOLD,
        new Color(Theme.DANGER).lerp(Color.WHITE, 0.45f),
        Theme.DANGER};

    private final Drawable track;
    private final Drawable[] segments = new Drawable[COLORS.length];
    private final double[] shares;

    /**
     * Creates the bar.
     *
     * @param ui     the kit, for the drawables
     * @param shares the five chances, in outcome order, adding up to 1
     */
    public OutcomeBar(UiKit ui, double... shares) {
        this.track = ui.solid(Theme.SKILL_TRACK);
        for (int i = 0; i < COLORS.length; i++) {
            segments[i] = ui.solid(COLORS[i]);
        }
        this.shares = shares.clone();
    }

    @Override
    public float getPrefHeight() {
        return 12f;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        batch.setColor(1f, 1f, 1f, parentAlpha * getColor().a);
        track.draw(batch, getX(), getY(), getWidth(), getHeight());
        float x = getX();
        for (int i = 0; i < shares.length && i < segments.length; i++) {
            float width = (float) (getWidth() * shares[i]);
            if (width > 0) {
                segments[i].draw(batch, x, getY(), width, getHeight());
                x += width;
            }
        }
    }
}
