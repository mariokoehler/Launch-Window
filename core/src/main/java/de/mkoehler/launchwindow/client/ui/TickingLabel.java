package de.mkoehler.launchwindow.client.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import java.util.function.LongFunction;

/**
 * A number that ticks to its new value and flashes green or red when an order changes it: the
 * header's funds and prestige ({@link Juice}, {@code design.md} &sect;5, Backlog, J1). On a card the
 * text flashes in the signal colour; on the page, where those colours don't read, a solid tag in that
 * colour shows behind the white text and fades. While it ticks, the label keeps the width of its
 * widest value, right-aligned, so the chip doesn't wobble as the digits change.
 *
 * @author Mario Koehler
 */
public final class TickingLabel extends Label {

    /** How far a page tag reaches past the text on each side. */
    private static final float TAG_PAD_X = 5f;
    private static final float TAG_PAD_Y = 1f;

    private final String key;
    private final LongFunction<String> format;
    private final Color base;
    private final Drawable upTag;
    private final Drawable downTag;
    private final GlyphLayout measure = new GlyphLayout();
    private final Color shown = new Color();
    private Juice.Tick tick;
    private boolean wasTicking;

    /**
     * Builds the label.
     *
     * @param ui      the kit, for the font and the page tags
     * @param style   the text style
     * @param color   the text's colour at rest
     * @param onPage  whether it sits on the page (a fading tag) rather than on a card (coloured text)
     * @param key     the number's key, shared by every place that shows it, such as {@code funds}
     * @param format  how a value is written
     */
    public TickingLabel(UiKit ui, Theme.TextStyle style, Color color, boolean onPage, String key,
                        LongFunction<String> format) {
        super("", ui.label("", style, color).getStyle());
        // An own style whose colour this label changes; the actor's colour would multiply with it.
        LabelStyle own = new LabelStyle(getStyle());
        own.fontColor = shown.set(color);
        setStyle(own);
        this.key = key;
        this.format = format;
        this.base = new Color(color);
        this.upTag = onPage ? ui.solid(Theme.SUCCESS) : null;
        this.downTag = onPage ? ui.solid(Theme.DANGER) : null;
        setAlignment(Align.right);
    }

    /**
     * Shows a value, ticking to it when it differs from the one shown last under this key.
     *
     * @param value   the value
     * @param context who and when it is shown for ({@link Juice#tick})
     * @return this label
     */
    public TickingLabel show(long value, String context) {
        tick = Juice.tick(key, context, value);
        update();
        return this;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        update();
    }

    private void update() {
        if (tick == null) {
            return;
        }
        String text = format.apply(tick.value());
        boolean ticking = tick.ticking();
        if (!textEquals(text) || ticking != wasTicking) {
            // Also once as the tick ends: the label lets go of the widest value's width it held.
            setText(text);
            invalidateHierarchy();
            wasTicking = ticking;
        }
        float flash = upTag == null ? tick.flash() : 0f;
        Color signal = tick.direction() > 0 ? Theme.SUCCESS : Theme.DANGER;
        shown.set(base).lerp(signal, flash);
    }

    @Override
    public float getPrefWidth() {
        float width = super.getPrefWidth();
        if (tick != null && tick.ticking()) {
            width = Math.max(width, Math.max(widest(tick.origin()), widest(tick.target())));
        }
        return width;
    }

    /** The width of a value with every digit as wide as the font's widest. */
    private float widest(long value) {
        BitmapFont font = getStyle().font;
        String text = format.apply(value);
        char wide = '0';
        float wideWidth = 0f;
        for (char digit = '0'; digit <= '9'; digit++) {
            measure.setText(font, String.valueOf(digit));
            if (measure.width > wideWidth) {
                wideWidth = measure.width;
                wide = digit;
            }
        }
        measure.setText(font, text.replaceAll("[0-9]", String.valueOf(wide)));
        return measure.width * getFontScaleX();
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (upTag != null && tick != null) {
            float flash = tick.flash();
            if (flash > 0f) {
                Drawable tag = tick.direction() > 0 ? upTag : downTag;
                Color old = batch.getColor().cpy();
                batch.setColor(1f, 1f, 1f, flash * parentAlpha * getColor().a);
                // The tag's text is white on the page already; the tag only shows behind it.
                float textWidth = getGlyphLayout().width;
                float x = getX() + getWidth() - textWidth;
                tag.draw(batch, x - TAG_PAD_X, getY() - TAG_PAD_Y, textWidth + 2 * TAG_PAD_X,
                    getHeight() + 2 * TAG_PAD_Y);
                batch.setColor(old);
            }
        }
        super.draw(batch, parentAlpha);
    }
}
