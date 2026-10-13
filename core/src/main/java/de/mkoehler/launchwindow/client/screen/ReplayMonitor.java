package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.video.VideoPlayer;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.ui.UiKit;
import de.mkoehler.launchwindow.client.view.ReplayClips;

import java.util.Locale;

/**
 * The replay's monitor ({@code ui-spec.md} &sect;4.7.3): a navy console filling its space, with a label
 * strip on top and a 16:9 picture as large as the rest allows, centred, playing the step's clip. With no video
 * player, or a clip that won't load, it shows the clip's still (the no-signal picture) and says "No
 * signal". Lives as long as its screen, which calls {@link #update()} every frame and
 * {@link #dispose()} at the end.
 *
 * @author Mario Koehler
 */
final class ReplayMonitor extends WidgetGroup {

    /** The picture's shape. */
    private static final float ASPECT = 16f / 9f;
    /** The bezel at the sides and bottom. */
    private static final float BEZEL = 12f;
    /** The bezel on top, holding the label. */
    private static final float STRIP = 28f;
    /** The lighter lip around the picture, so the console's wings read as its surface. */
    private static final Color LIP = new Color(Theme.NAVY).lerp(Theme.SURFACE_CARD, 0.12f);
    private static final float LIP_WIDTH = 2f;

    private final UiKit ui;
    private final VideoPlayer player;
    private final Label label;
    private Texture still;
    private String stillPath;
    private boolean video;
    private float px;
    private float py;
    private float pw;
    private float ph;

    /**
     * Builds the monitor.
     *
     * @param ui     the widget kit
     * @param player the video player, or {@code null} for stills only
     */
    ReplayMonitor(UiKit ui, VideoPlayer player) {
        this.ui = ui;
        this.player = player;
        label = ui.label("", Theme.TextStyle.LABEL, Theme.ON_FILL);
        addActor(label);
    }

    /**
     * Plays a step's clip from its start.
     *
     * @param clip the clip
     * @param step the step's name, for the label
     */
    void play(ReplayClips.Clip clip, String step) {
        video = false;
        if (player != null) {
            try {
                video = player.load(Gdx.files.internal(clip.video()));
                if (video) {
                    player.setLooping(clip.loop());
                    player.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                    player.play();
                }
            } catch (Exception e) {
                Gdx.app.error("Replay", "can't play " + clip.video(), e);
                video = false;
            }
        }
        label.setText(video && clip.signal() ? "CAM 1 · " + step.toUpperCase(Locale.ROOT) : "NO SIGNAL");
        if (!video) {
            showStill(clip.still() != null ? clip.still() : ReplayClips.noSignal().still());
        }
        invalidate();
    }

    /**
     * Stops the clip, for the season summary.
     */
    void stop() {
        if (player != null && video) {
            player.stop();
        }
        video = false;
    }

    /**
     * Decodes the clip's next frame, if it's time; call once a frame.
     */
    void update() {
        if (player != null && video) {
            player.update();
        }
    }

    @Override
    public void layout() {
        float roomW = getWidth() - 2 * BEZEL;
        float roomH = getHeight() - STRIP - BEZEL;
        pw = Math.max(0f, Math.min(roomW, roomH * ASPECT));
        ph = pw / ASPECT;
        px = (getWidth() - pw) / 2f;
        py = (getHeight() - STRIP - BEZEL - ph) / 2f + BEZEL;
        label.pack();
        label.setPosition(BEZEL, getHeight() - STRIP + (STRIP - label.getHeight()) / 2f);
    }

    @Override
    public float getPrefWidth() {
        return 0f;
    }

    @Override
    public float getPrefHeight() {
        return 0f;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        validate();
        Color tint = batch.getColor().cpy();
        batch.setColor(1f, 1f, 1f, parentAlpha);
        float x = getX() + px;
        float y = getY() + py;
        ui.solid(Theme.NAVY).draw(batch, getX(), getY(), getWidth(), getHeight());
        ui.solid(LIP).draw(batch, x - LIP_WIDTH, y - LIP_WIDTH, pw + 2 * LIP_WIDTH, ph + 2 * LIP_WIDTH);
        ui.solid(Theme.SURFACE_CARD).draw(batch, x, y, pw, ph);
        Texture frame = video && player != null ? player.getTexture() : null;
        if (frame != null && player.getVideoWidth() > 0) {
            batch.draw(frame, x, y, pw, ph, 0, 0, player.getVideoWidth(), player.getVideoHeight(), false, false);
        } else if (still != null) {
            batch.draw(still, x, y, pw, ph);
        }
        batch.setColor(tint);
        super.draw(batch, parentAlpha);
    }

    private void showStill(String path) {
        if (path.equals(stillPath)) {
            return;
        }
        disposeStill();
        stillPath = path;
        try {
            still = new Texture(Gdx.files.internal(path));
            still.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        } catch (RuntimeException e) {
            Gdx.app.error("Replay", "no still " + path, e);
            still = null;
        }
    }

    private void disposeStill() {
        if (still != null) {
            still.dispose();
            still = null;
        }
        stillPath = null;
    }

    /**
     * Frees the player and the still.
     */
    void dispose() {
        if (player != null) {
            player.dispose();
        }
        disposeStill();
    }
}
