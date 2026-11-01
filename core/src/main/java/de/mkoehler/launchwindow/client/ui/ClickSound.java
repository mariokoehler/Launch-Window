package de.mkoehler.launchwindow.client.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Disableable;
import de.mkoehler.launchwindow.client.LaunchWindowGame;

/**
 * The click of everything clickable ({@code ui-spec.md} &sect;4.12): a capture listener on the stage, so buttons,
 * cards, rows and chips all sound without any of them knowing, new ones too. A click counts as the widget's
 * own would: the left button, released still over the widget, not dragged into a scroll. A plain {@link
 * ClickListener} only swallows clicks (a dialog's scrim), so it doesn't make a widget clickable.
 *
 * @author Mario Koehler
 */
public final class ClickSound extends InputListener {

    private static final String FILE = "audio/ui/button-click.mp3";
    /** The click's own level, under the player's effects volume: every click is heard, so it stays soft. */
    private static final float LEVEL = 0.5f;

    private static Sound clip;
    private static boolean loaded;

    private final Runnable play;

    /**
     * Creates a listener that plays the click.
     */
    public ClickSound() {
        this(ClickSound::play);
    }

    /**
     * Creates a listener with its own sound, for the tests.
     *
     * @param play what a click does
     */
    ClickSound(Runnable play) {
        this.play = play;
    }

    @Override
    public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
        return button == Input.Buttons.LEFT && clickable(event.getTarget()) != null;
    }

    @Override
    public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
        Actor pressed = clickable(event.getTarget());
        if (pressed == null || event.isTouchFocusCancel()) {
            return;
        }
        Actor over = event.getStage().hit(event.getStageX(), event.getStageY(), true);
        if (over != null && over.isDescendantOf(pressed)) {
            play.run();
        }
    }

    /**
     * Finds what a press on an actor clicks: the actor or its nearest parent with a click of its own.
     *
     * @param actor the actor under the pointer
     * @return the clickable actor, or null if the press clicks nothing
     */
    static Actor clickable(Actor actor) {
        for (Actor each = actor; each != null; each = each.getParent()) {
            if (each instanceof TextField || each instanceof Disableable disableable && disableable.isDisabled()) {
                return null;
            }
            for (EventListener listener : each.getListeners()) {
                if (listener instanceof ClickListener && listener.getClass() != ClickListener.class) {
                    return each;
                }
            }
        }
        return null;
    }

    private static void play() {
        if (!LaunchWindowGame.soundOn()) {
            return;
        }
        if (!loaded) {
            loaded = true;
            try {
                clip = Gdx.audio.newSound(Gdx.files.internal(FILE));
            } catch (RuntimeException e) {
                Gdx.app.error("Audio", "can't play " + FILE, e);
            }
        }
        if (clip != null) {
            clip.play(LEVEL * LaunchWindowGame.effectsVolume());
        }
    }

    /**
     * Releases the sound, when the game closes.
     */
    public static void dispose() {
        if (clip != null) {
            clip.dispose();
            clip = null;
        }
        loaded = false;
    }
}
