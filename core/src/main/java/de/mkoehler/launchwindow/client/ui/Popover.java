package de.mkoehler.launchwindow.client.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

/**
 * A card that floats below an actor: hovering the actor previews it, clicking pins it open until
 * the actor is clicked again, as a history note's card does ({@code ui-spec.md} component 15). Used
 * by the chemistry indicator's breakdown (component 21). The card goes on the stage itself, above
 * everything else, so no parent table or scroll pane can clip it; its owner closes it when the
 * actor leaves the stage.
 *
 * @author Mario Koehler
 */
public final class Popover {

    private static final float GAP = 6f;

    private final Actor target;
    private final Table card;
    private boolean pinned;

    private Popover(Actor target, Table card) {
        this.target = target;
        this.card = card;
        card.setTouchable(Touchable.childrenOnly);
    }

    /**
     * Gives an actor a popover.
     *
     * @param target the actor that opens it
     * @param card   the card to show
     * @return the popover
     */
    public static Popover attach(Actor target, Table card) {
        Popover popover = new Popover(target, card);
        target.setTouchable(Touchable.enabled);
        target.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                if (pointer == -1) {
                    popover.show();
                }
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                if (pointer == -1 && !popover.pinned && (toActor == null || !toActor.isDescendantOf(target))) {
                    popover.hide();
                }
            }
        });
        target.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                popover.setPinned(!popover.pinned);
            }
        });
        return popover;
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
     * Pins the card open or unpins it; unpinning while the pointer rests on the actor keeps it up
     * as a preview.
     *
     * @param pinned whether to pin
     */
    public void setPinned(boolean pinned) {
        this.pinned = pinned;
        if (pinned || isOver()) {
            show();
        } else {
            hide();
        }
    }

    /**
     * Closes the card. The actor's owner calls this when the actor leaves the stage (its
     * {@code setStage(null)}), so a rebuild never leaves the card behind.
     */
    public void close() {
        pinned = false;
        hide();
    }

    private boolean isOver() {
        Stage stage = target.getStage();
        if (stage == null) {
            return false;
        }
        Vector2 pointer = stage.screenToStageCoordinates(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
        Vector2 local = target.stageToLocalCoordinates(pointer);
        return target.hit(local.x, local.y, true) != null;
    }

    private void show() {
        Stage stage = target.getStage();
        if (stage == null) {
            return;
        }
        card.pack();
        Vector2 corner = target.localToStageCoordinates(new Vector2(0f, 0f));
        float x = Math.min(corner.x, stage.getWidth() - card.getWidth() - Theme.SPACE_4);
        float y = corner.y - GAP - card.getHeight();
        if (y < Theme.SPACE_4) {
            y = corner.y + target.getHeight() + GAP;
        }
        card.setPosition(Math.max(Theme.SPACE_4, x), y);
        if (card.getStage() == null) {
            stage.addActor(card);
        }
        card.toFront();
    }

    private void hide() {
        card.remove();
    }
}
