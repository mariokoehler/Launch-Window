package de.mkoehler.launchwindow.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.GdxNativesLoader;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Presses go through a real stage, as the player's do; the window behind it is stubbed.
 *
 * @author Mario Koehler
 */
class ClickSoundTest {

    private static final int SIZE = 1000;

    private Stage stage;
    private int clicks;

    @BeforeEach
    void stage() {
        GdxNativesLoader.load();
        Gdx.files = stub(Files.class);
        Gdx.graphics = stub(Graphics.class);
        Gdx.gl = stub(GL20.class);
        stage = new Stage(new ScreenViewport(), stub(Batch.class));
        stage.addCaptureListener(new ClickSound(() -> clicks++));
    }

    @Test
    void aWidgetsOwnClickSounds() {
        place(clickable(new Actor()));
        click(50, 50, 50, 50);
        assertEquals(1, clicks);
    }

    @Test
    void aClickOnAChildOfAClickableRowSounds() {
        Table row = clickable(new Table());
        row.setTouchable(Touchable.enabled);
        place(row);
        Actor label = new Actor();
        label.setBounds(0, 0, 100, 100);
        label.setTouchable(Touchable.enabled);
        row.addActor(label);
        click(50, 50, 50, 50);
        assertEquals(1, clicks);
    }

    @Test
    void aButtonSounds() {
        place(new Button(new Button.ButtonStyle()));
        click(50, 50, 50, 50);
        assertEquals(1, clicks);
    }

    @Test
    void aDisabledButtonIsSilent() {
        Button button = new Button(new Button.ButtonStyle());
        button.setDisabled(true);
        place(button);
        click(50, 50, 50, 50);
        assertEquals(0, clicks);
    }

    @Test
    void aScrimThatOnlySwallowsClicksIsSilent() {
        Actor scrim = new Actor();
        scrim.addListener(new ClickListener());
        place(scrim);
        click(50, 50, 50, 50);
        assertEquals(0, clicks);
    }

    @Test
    void aReleaseOffTheWidgetIsSilent() {
        place(clickable(new Actor()));
        click(50, 50, 500, 500);
        assertEquals(0, clicks);
    }

    @Test
    void theRightButtonIsSilent() {
        place(clickable(new Actor()));
        stage.touchDown(50, SIZE - 50, 0, Input.Buttons.RIGHT);
        stage.touchUp(50, SIZE - 50, 0, Input.Buttons.RIGHT);
        assertEquals(0, clicks);
    }

    @Test
    void aPressTakenByAScrollIsSilent() {
        place(clickable(new Actor()));
        stage.touchDown(50, SIZE - 50, 0, Input.Buttons.LEFT);
        stage.cancelTouchFocus();
        stage.touchUp(50, SIZE - 50, 0, Input.Buttons.LEFT);
        assertEquals(0, clicks);
    }

    private <T extends Actor> T clickable(T actor) {
        actor.addListener(new ClickListener() {
        });
        return actor;
    }

    private void place(Actor actor) {
        actor.setBounds(0, 0, 100, 100);
        stage.addActor(actor);
    }

    /** Coordinates from the bottom left, the stage's way. */
    private void click(int downX, int downY, int upX, int upY) {
        stage.touchDown(downX, SIZE - downY, 0, Input.Buttons.LEFT);
        stage.touchUp(upX, SIZE - upY, 0, Input.Buttons.LEFT);
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (proxy, method, args) -> {
            Class<?> returned = method.getReturnType();
            if (returned == int.class) {
                return SIZE;
            }
            if (returned == float.class) {
                return 1f;
            }
            if (returned == boolean.class) {
                return false;
            }
            return null;
        });
    }
}
