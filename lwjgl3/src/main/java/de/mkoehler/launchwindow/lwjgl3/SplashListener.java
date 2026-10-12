package de.mkoehler.launchwindow.lwjgl3;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.video.VideoPlayer;
import com.badlogic.gdx.video.VideoPlayerCreator;
import de.mkoehler.launchwindow.client.ui.Theme;

/**
 * The splash window ({@code ui-spec.md} &sect;4.0): a borderless square that plays the animated
 * logo once, then opens the game's own window and closes itself. A key or a click skips it, and a
 * video that can't play (a missing native, a missing file) goes straight on.
 *
 * @author Mario Koehler
 */
final class SplashListener extends ApplicationAdapter {

    /** The logo animation, made in EffectCraft ({@code assets-raw/splash/}). */
    static final String VIDEO = "ui/splash/splash.webm";
    /** Goes on even if the player never reports the end; the clip runs 4 s. */
    private static final float MAX_SECONDS = 8f;

    private final Runnable openMainWindow;
    private Lwjgl3Window window;
    private SpriteBatch batch;
    private VideoPlayer player;
    private float elapsed;
    private boolean finished;

    /**
     * @param openMainWindow opens the game's window; called once, on the render thread
     */
    SplashListener(Runnable openMainWindow) {
        this.openMainWindow = openMainWindow;
    }

    @Override
    public void create() {
        window = ((Lwjgl3Graphics) Gdx.graphics).getWindow();
        batch = new SpriteBatch();
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                finish("skipped");
                return true;
            }

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                finish("skipped");
                return true;
            }
        });
        try {
            player = VideoPlayerCreator.createVideoPlayer();
            // The decoder may report the end off the render thread.
            player.setOnCompletionListener(file -> Gdx.app.postRunnable(() -> finish("completed")));
            if (!player.load(Gdx.files.internal(VIDEO))) {
                throw new IllegalStateException("the player refused " + VIDEO);
            }
            player.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            player.play();
            Gdx.app.log("Splash", "playing " + VIDEO);
        } catch (Exception | LinkageError e) {
            Gdx.app.error("Splash", "no splash video", e);
            finish("failed");
        }
    }

    @Override
    public void resize(int width, int height) {
        batch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
    }

    @Override
    public void render() {
        ScreenUtils.clear(Theme.NAVY); // the clip's own background, also before its first frame
        if (finished || player == null) {
            return;
        }
        elapsed += Gdx.graphics.getDeltaTime();
        if (elapsed > MAX_SECONDS) {
            finish("timed out");
            return;
        }
        player.update();
        Texture frame = player.getTexture();
        int videoWidth = player.getVideoWidth();
        int videoHeight = player.getVideoHeight();
        if (frame == null || videoWidth <= 0 || videoHeight <= 0) {
            return;
        }
        // Fit the video into the window; the texture can be larger than the video.
        float width = Gdx.graphics.getWidth();
        float height = Gdx.graphics.getHeight();
        float scale = Math.min(width / videoWidth, height / videoHeight);
        float drawWidth = videoWidth * scale;
        float drawHeight = videoHeight * scale;
        batch.begin();
        batch.draw(frame, (width - drawWidth) / 2, (height - drawHeight) / 2, drawWidth, drawHeight,
            0, 0, videoWidth, videoHeight, false, false);
        batch.end();
    }

    /**
     * Opens the game's window, then closes this one a step later, so the application never sees
     * its last window close (the backend adds a new window from a queued runnable).
     */
    private void finish(String why) {
        if (finished) {
            return;
        }
        finished = true;
        Gdx.app.log("Splash", why + " after " + Math.round(elapsed * 1000) + " ms");
        openMainWindow.run();
        Gdx.app.postRunnable(window::closeWindow);
    }

    @Override
    public void dispose() {
        if (player != null) {
            player.dispose();
        }
        batch.dispose();
    }
}
