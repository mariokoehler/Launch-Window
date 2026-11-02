package de.mkoehler.launchwindow.client.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

/**
 * A skill bar: a thin {@link Theme#SKILL_TRACK} track filled in {@link Theme#NAVY} to a value out
 * of 100, as on the hire screen's candidate rows. It can mark how high the skill can grow with a
 * tick on the track (the person's ceiling, {@code ui-spec.md} component 31).
 *
 * @author Mario Koehler
 */
public final class SkillBar extends Widget {

    private static final float HEIGHT = 6f;

    private final Drawable track;
    private final Drawable fill;
    private final float value;
    private final Drawable tick;
    private final float ceiling;

    /**
     * Builds the bar.
     *
     * @param ui    the widget kit
     * @param value the value, 0 to 100
     */
    public SkillBar(UiKit ui, double value) {
        this(ui, value, 100);
    }

    /**
     * Builds the bar with a ceiling's tick.
     *
     * @param ui      the widget kit
     * @param value   the value, 0 to 100
     * @param ceiling how high the skill can grow; at 100 no tick is drawn
     */
    public SkillBar(UiKit ui, double value, double ceiling) {
        this.ceiling = (float) Math.max(0, Math.min(100, ceiling));
        this.tick = ui.solid(Theme.INK_MUTED);
        this.track = ui.solid(Theme.SKILL_TRACK);
        this.fill = ui.solid(Theme.NAVY);
        this.value = (float) Math.max(0, Math.min(100, value));
    }

    @Override
    public float getPrefHeight() {
        return HEIGHT;
    }

    @Override
    public float getPrefWidth() {
        return 120f;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        Color color = getColor();
        batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);
        float y = getY() + (getHeight() - HEIGHT) / 2f;
        track.draw(batch, getX(), y, getWidth(), HEIGHT);
        fill.draw(batch, getX(), y, getWidth() * value / 100f, HEIGHT);
        if (ceiling < 100f) {
            tick.draw(batch, getX() + getWidth() * ceiling / 100f - 1f, y - 2f, 2f, HEIGHT + 4f);
        }
    }
}
