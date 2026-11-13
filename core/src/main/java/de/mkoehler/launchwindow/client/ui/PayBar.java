package de.mkoehler.launchwindow.client.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

/**
 * The pay bar ({@code ui-spec.md} &sect;4.23): a 4 px track from 0 to what someone expects, filled in
 * {@link Theme#NAVY} to their salary and in {@link Theme#GOLD} for the gap; full and navy when they're paid as
 * expected. Thinner than the morale and loyalty bars, so it reads as a detail of the pay line above it.
 *
 * @author Mario Koehler
 */
public final class PayBar extends Widget {

    private static final float HEIGHT = 4f;

    private final Drawable track;
    private final Drawable fill;
    private final Drawable gap;
    private final float paid;

    /**
     * Builds the bar.
     *
     * @param ui       the widget kit
     * @param salary   the salary
     * @param expected what they expect
     */
    public PayBar(UiKit ui, double salary, double expected) {
        this.track = ui.solid(Theme.SKILL_TRACK);
        this.fill = ui.solid(Theme.NAVY);
        this.gap = ui.solid(Theme.GOLD);
        this.paid = expected <= 0 ? 1f : (float) Math.min(1, salary / expected);
    }

    @Override
    public float getPrefHeight() {
        return HEIGHT;
    }

    @Override
    public float getPrefWidth() {
        return 180f;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        Color color = getColor();
        batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);
        float y = getY() + (getHeight() - HEIGHT) / 2f;
        track.draw(batch, getX(), y, getWidth(), HEIGHT);
        fill.draw(batch, getX(), y, getWidth() * paid, HEIGHT);
        if (paid < 1f) {
            gap.draw(batch, getX() + getWidth() * paid, y, getWidth() * (1f - paid), HEIGHT);
        }
    }
}
