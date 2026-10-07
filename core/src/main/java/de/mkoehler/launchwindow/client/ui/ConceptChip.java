package de.mkoehler.launchwindow.client.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Scaling;

/**
 * A concept chip ({@code ui-spec.md} component 19, mocked up 2026-10-07): a real-world idea behind
 * a piece of game content, such as "Retrofire". Clicking it opens the explainer sheet that explains
 * it. White, a thin navy rule, the set-square glyph in its own cell like a title-block cell, and the
 * label in bold navy; hover thickens the rule and tints the glyph cell, pressing fills it navy. A
 * gold dot on the corner marks an explainer the player hasn't opened yet.
 *
 * @author Mario Koehler
 */
public final class ConceptChip extends Stack {

    /** The chip's height. */
    public static final float HEIGHT = 32f;
    /** The gap between chips, both ways. */
    public static final float GAP = 8f;

    private static final float GLYPH_CELL = 30f;
    private static final int DOT = 12;
    private static final Color GLYPH_HOVER = new Color(Theme.SURFACE_CARD).lerp(Theme.NAVY, 0.08f);

    private final UiKit ui;
    private final Table frame = new Table();
    private final Table glyphCell = new Table();
    private final Image glyph;
    private final Image divider;
    private final Label label;
    private final ClickListener clicks;
    private int look = -1;

    /**
     * Builds a chip.
     *
     * @param ui      the kit
     * @param text    the concept's label
     * @param isNew   whether its explainer hasn't been opened yet
     * @param onClick what a click does: open the explainer
     */
    public ConceptChip(UiKit ui, String text, boolean isNew, Runnable onClick) {
        this.ui = ui;
        glyph = new Image(ui.icon("concept"), Scaling.fit);
        glyphCell.add(glyph).size(18f);
        divider = new Image(ui.solid(Theme.NAVY_BORDER));
        // White in its style, so the tint below sets its colour for each state.
        label = ui.label(text, Theme.TextStyle.DRAWING_LABEL_SM, Color.WHITE);
        frame.pad(Theme.BORDER_THIN);
        frame.add(glyphCell).width(GLYPH_CELL).growY();
        frame.add(divider).width(Theme.BORDER_THIN).growY();
        frame.add(label).padLeft(10f).padRight(12f);
        add(frame);
        if (isNew) {
            Table overlay = new Table();
            overlay.top().right();
            overlay.setTouchable(Touchable.disabled);
            // Centred on the top-right corner, half outside the chip, as in the mockup.
            overlay.add(new Image(ui.circle(Theme.GOLD, Theme.SURFACE_CARD, 2f, DOT))).size(DOT)
                .padTop(-DOT / 2f).padRight(-DOT / 2f);
            add(overlay);
        }
        setTouchable(Touchable.enabled);
        clicks = new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onClick.run();
            }
        };
        addListener(clicks);
        restyle();
    }

    @Override
    public float getPrefHeight() {
        return HEIGHT;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        restyle();
    }

    /** Sets the look for the pointer's state; drawables come from the kit's caches. */
    private void restyle() {
        int now = clicks.isVisualPressed() ? 2 : clicks.isOver() ? 1 : 0;
        if (now == look) {
            return;
        }
        look = now;
        switch (now) {
            case 2 -> {
                frame.setBackground(ui.rect(Theme.NAVY, Theme.NAVY, Theme.BORDER_THIN));
                glyphCell.setBackground((Drawable) null);
                glyph.setColor(Theme.ON_FILL);
                label.setColor(Theme.ON_FILL);
                divider.setDrawable(ui.solid(Theme.ON_FILL));
            }
            case 1 -> {
                frame.setBackground(ui.rect(Theme.SURFACE_CARD, Theme.NAVY, Theme.BORDER_EMPHASIS));
                glyphCell.setBackground(ui.solid(GLYPH_HOVER));
                glyph.setColor(Theme.NAVY);
                label.setColor(Theme.NAVY);
                divider.setDrawable(ui.solid(Theme.NAVY));
            }
            default -> {
                frame.setBackground(ui.rect(Theme.SURFACE_CARD, Theme.NAVY, Theme.BORDER_THIN));
                glyphCell.setBackground((Drawable) null);
                glyph.setColor(Theme.NAVY);
                label.setColor(Theme.NAVY);
                divider.setDrawable(ui.solid(Theme.NAVY_BORDER));
            }
        }
    }
}
