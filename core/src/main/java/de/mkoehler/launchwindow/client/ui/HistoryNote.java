package de.mkoehler.launchwindow.client.ui;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Scaling;

/**
 * The history-note icon ({@code ui-spec.md} component 15): a small navy open book next to a
 * label. Hovering it shows a short card about the real history behind that label; clicking
 * pins the card open until its &times; or the icon is clicked again. The card goes on the
 * stage itself, above everything else, so no parent table can clip it.
 *
 * <p>The card is flat, like every surface in this system (the user dropped the design system's
 * one drop shadow for this popover, 2026-10-04).</p>
 *
 * @author Mario Koehler
 */
public final class HistoryNote extends Image {

    /** The icon's size, as in the design system's {@code .lw-historynote__icon}. */
    public static final float ICON_SIZE = 18f;

    private static final float CARD_WIDTH = 280f;
    private static final float CARD_GAP = 8f;

    private final Table card;
    private final Label close;
    private boolean pinned;

    /**
     * Builds the icon and its card.
     *
     * @param ui   the widget kit
     * @param text the note, two to four sentences
     */
    public HistoryNote(UiKit ui, String text) {
        super(ui.historyNoteIcon(), Scaling.fit);
        setColor(Theme.NAVY);
        setSize(ICON_SIZE, ICON_SIZE);

        card = new Table();
        card.setBackground(ui.historyNoteCard());
        card.pad(Theme.SPACE_3);
        card.defaults().left();
        Table header = new Table();
        header.add(ui.label("History", Theme.TextStyle.LABEL, Theme.NAVY)).expandX().left();
        close = ui.label("×", Theme.TextStyle.BODY, Theme.INK_MUTED);
        close.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                setPinned(false);
            }
        });
        header.add(close).right();
        card.add(header).growX().padBottom(Theme.SPACE_1).row();
        card.add(ui.paragraph(text, Theme.TextStyle.BODY_SM, Theme.INK)).width(CARD_WIDTH - 2 * Theme.SPACE_3);
        card.setTouchable(Touchable.childrenOnly);

        addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                if (pointer == -1) {
                    showCard();
                }
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                if (pointer == -1 && !pinned) {
                    hideCard();
                }
            }
        });
        addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                setPinned(!pinned);
            }
        });
    }

    /**
     * Returns whether the card is pinned open.
     *
     * @return whether pinned
     */
    public boolean isPinned() {
        return pinned;
    }

    /**
     * Pins the card open or unpins it. Unpinning while the pointer still rests on the icon keeps
     * the card up as a hover preview, as in the design system.
     *
     * @param pinned whether to pin
     */
    public void setPinned(boolean pinned) {
        this.pinned = pinned;
        close.setVisible(pinned);
        if (pinned || isOver()) {
            showCard();
        } else {
            hideCard();
        }
    }

    /**
     * Removes the card along with the icon, so a screen change never leaves it behind.
     *
     * @return whether the icon was removed
     */
    @Override
    public boolean remove() {
        hideCard();
        return super.remove();
    }

    private boolean isOver() {
        Stage stage = getStage();
        if (stage == null) {
            return false;
        }
        Vector2 pointer = stage.screenToStageCoordinates(
            new Vector2(com.badlogic.gdx.Gdx.input.getX(), com.badlogic.gdx.Gdx.input.getY()));
        Vector2 local = stageToLocalCoordinates(pointer);
        return hit(local.x, local.y, true) != null;
    }

    private void showCard() {
        Stage stage = getStage();
        if (stage == null) {
            return;
        }
        close.setVisible(pinned);
        card.pack();
        Vector2 corner = localToStageCoordinates(new Vector2(0f, 0f));
        float x = Math.min(corner.x, stage.getWidth() - card.getWidth() - Theme.SPACE_4);
        float y = corner.y + getHeight() + CARD_GAP;
        if (y + card.getHeight() > stage.getHeight() - Theme.SPACE_4) {
            y = corner.y - CARD_GAP - card.getHeight();
        }
        card.setPosition(Math.max(Theme.SPACE_4, x), y);
        if (card.getStage() == null) {
            stage.addActor(card);
        }
        card.toFront();
    }

    private void hideCard() {
        card.remove();
    }
}
