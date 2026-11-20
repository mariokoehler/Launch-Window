package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.utils.viewport.Viewport;
import de.mkoehler.launchwindow.client.ui.Juice;
import de.mkoehler.launchwindow.client.ui.Theme;

/**
 * A short crossfade between screens ({@code design.md} &sect;5, Backlog, J1): when the screen
 * changes, the old one is drawn once more into a frame buffer, without acting, and that picture fades
 * out over the new screen, which takes input at once. Nothing fades with motion off, before the old
 * screen has drawn a frame, or when the window's size changed in between.
 *
 * @author Mario Koehler
 */
public final class ScreenFade {

    /** How long the old screen takes to fade out, in seconds. Unconfirmed: dropped if it feels sluggish. */
    public static final float SECONDS = 0.15f;

    private static FrameBuffer buffer;
    private static SpriteBatch batch;
    private static double start = Double.NEGATIVE_INFINITY;

    private ScreenFade() {
    }

    /**
     * Takes the picture of the screen being left.
     *
     * @param old the screen being left, or {@code null}
     */
    public static void capture(Screen old) {
        start = Double.NEGATIVE_INFINITY;
        if (!Juice.motion() || !(old instanceof StageScreen screen) || !screen.hasDrawn()
            || screen.stage.getBatch().isDrawing()) {
            return;
        }
        int width = Gdx.graphics.getBackBufferWidth();
        int height = Gdx.graphics.getBackBufferHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        if (buffer == null || buffer.getWidth() != width || buffer.getHeight() != height) {
            if (buffer != null) {
                buffer.dispose();
            }
            buffer = new FrameBuffer(Pixmap.Format.RGB888, width, height, false);
            buffer.getColorBufferTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        }
        buffer.begin();
        Gdx.gl.glClearColor(Theme.SURFACE_PAGE.r, Theme.SURFACE_PAGE.g, Theme.SURFACE_PAGE.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        screen.stage.getViewport().apply();
        screen.stage.draw();
        buffer.end();
        // The screen whose act this may be inside draws once more this frame: give it its viewport back.
        screen.stage.getViewport().apply();
        start = Juice.now();
    }

    /**
     * Draws the old screen's picture over the new one while it fades, then puts the screen's
     * viewport back (screens apply theirs only on a resize).
     *
     * @param viewport the screen's viewport
     */
    static void drawOver(Viewport viewport) {
        if (buffer == null) {
            return;
        }
        float progress = (float) ((Juice.now() - start) / SECONDS);
        if (progress >= 1f || progress < 0f) {
            return;
        }
        int width = Gdx.graphics.getBackBufferWidth();
        int height = Gdx.graphics.getBackBufferHeight();
        if (width != buffer.getWidth() || height != buffer.getHeight()) {
            start = Double.NEGATIVE_INFINITY;
            return;
        }
        if (batch == null) {
            batch = new SpriteBatch();
        }
        Gdx.gl.glViewport(0, 0, width, height);
        batch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
        batch.begin();
        batch.setColor(1f, 1f, 1f, 1f - Interpolation.pow2In.apply(progress));
        // A frame buffer's texture is upside down.
        batch.draw(buffer.getColorBufferTexture(), 0, 0, width, height, 0, 0, width, height, false, true);
        batch.end();
        viewport.apply();
    }

    /**
     * Frees the frame buffer, as the game closes.
     */
    public static void dispose() {
        if (buffer != null) {
            buffer.dispose();
            buffer = null;
        }
        if (batch != null) {
            batch.dispose();
            batch = null;
        }
    }
}
