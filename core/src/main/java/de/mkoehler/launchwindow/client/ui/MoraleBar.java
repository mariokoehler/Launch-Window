package de.mkoehler.launchwindow.client.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

/**
 * The person page's morale bar: a 6 px track filled to morale in the roster's morale colour, with the skill
 * ceiling's tick ({@link SkillBar}: 2 px, {@link Theme#INK_MUTED}, 2 px past the track each side) where pay holds
 * morale ({@code ui-spec.md} &sect;4.23).
 *
 * @author Mario Koehler
 */
public final class MoraleBar extends Widget {

    private static final float HEIGHT = 6f;

    private final Drawable track;
    private final Drawable fill;
    private final Drawable tick;
    private final float value;
    private final float ceiling;

    /**
     * Builds the bar.
     *
     * @param ui      the widget kit
     * @param morale  the morale, 0 to 100
     * @param color   its colour
     * @param ceiling how high pay lets it rise; at 100 no tick is drawn
     */
    public MoraleBar(UiKit ui, int morale, Color color, int ceiling) {
        this.track = ui.solid(Theme.SKILL_TRACK);
        this.fill = ui.solid(color);
        this.tick = ui.solid(Theme.INK_MUTED);
        this.value = Math.max(0, Math.min(100, morale));
        this.ceiling = Math.max(0, Math.min(100, ceiling));
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
        fill.draw(batch, getX(), y, getWidth() * value / 100f, HEIGHT);
        if (ceiling < 100f) {
            tick.draw(batch, getX() + getWidth() * ceiling / 100f - 1f, y - 2f, 2f, HEIGHT + 4f);
        }
    }
}
