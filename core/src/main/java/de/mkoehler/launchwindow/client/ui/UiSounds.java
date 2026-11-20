package de.mkoehler.launchwindow.client.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.scenes.scene2d.Actor;
import de.mkoehler.launchwindow.client.LaunchWindowGame;
import java.util.HashMap;
import java.util.Map;

/**
 * The small effects' sounds ({@code assets/audio/ui/}, {@code design.md} &sect;5, Backlog, Juice), owned by
 * the game rather than a screen, as {@link ClickSound} is: a chime or a paper's reverb plays on when the
 * player moves to another screen. Short sounds play at the replay sounds' volume; the race's fanfare is
 * music, at the replay music's volume, tied to the actor it plays under: it fades out once that actor
 * leaves the screen, or the screen changes.
 *
 * @author Mario Koehler
 */
public final class UiSounds {

    /** A turn resolved while the player is in the game. */
    public static final String NEW_TURN = "new-turn";
    /** A First's front page lands. */
    public static final String PAPER_LANDS = "paper-lands";
    /** A reached goal's patch stamps onto the report. */
    public static final String STAMP = "stamp";
    /** The race won. */
    public static final String RACE_WON = "race-won";

    /** How long the fanfare takes to fade out, in seconds. Unconfirmed. */
    public static final float MUSIC_FADE_SECONDS = 1.5f;

    private static final Map<String, Sound> SOUNDS = new HashMap<>();
    private static Music music;
    private static Actor musicOwner;
    private static float musicLevel;
    private static float fading = -1f;

    private UiSounds() {
    }

    /**
     * Plays a short sound, if sound is on.
     *
     * @param name the sound, such as {@link #STAMP}
     * @return the playing instance's id, for {@link #stop}, or -1
     */
    public static long play(String name) {
        if (!LaunchWindowGame.soundOn()) {
            return -1L;
        }
        Sound sound = SOUNDS.computeIfAbsent(name, key -> {
            try {
                return Gdx.audio.newSound(Gdx.files.internal("audio/ui/" + key + ".ogg"));
            } catch (RuntimeException e) {
                Gdx.app.error("Audio", "can't play " + key, e);
                return null;
            }
        });
        return sound == null ? -1L : sound.play(LaunchWindowGame.effectsVolume());
    }

    /**
     * Stops one playing instance of a short sound.
     *
     * @param name the sound
     * @param id   the instance, from {@link #play}
     */
    public static void stop(String name, long id) {
        Sound sound = SOUNDS.get(name);
        if (sound != null && id >= 0) {
            sound.stop(id);
        }
    }

    /**
     * Plays music under an actor, if sound is on, replacing any playing.
     *
     * @param name  the music, such as {@link #RACE_WON}
     * @param owner the actor it plays under: it fades out once that leaves its stage
     */
    public static void playMusic(String name, Actor owner) {
        stopMusic();
        if (!LaunchWindowGame.soundOn()) {
            return;
        }
        try {
            music = Gdx.audio.newMusic(Gdx.files.internal("audio/ui/" + name + ".ogg"));
        } catch (RuntimeException e) {
            Gdx.app.error("Audio", "can't play " + name, e);
            return;
        }
        musicOwner = owner;
        musicLevel = LaunchWindowGame.musicVolume();
        music.setVolume(musicLevel);
        music.play();
    }

    /**
     * Starts the music's fade-out, as when the screen changes.
     */
    public static void fadeMusic() {
        if (music != null && fading < 0f) {
            fading = MUSIC_FADE_SECONDS;
        }
    }

    /**
     * Moves the music on by a frame: fades it once its actor left the stage, and frees it when done.
     * Called every frame by the game.
     *
     * @param delta the frame's seconds
     */
    public static void update(float delta) {
        if (music == null) {
            return;
        }
        if (musicOwner != null && musicOwner.getStage() == null) {
            fadeMusic();
        }
        if (fading >= 0f) {
            fading -= delta;
            if (fading <= 0f) {
                stopMusic();
                return;
            }
            music.setVolume(musicLevel * fading / MUSIC_FADE_SECONDS);
        }
        if (!music.isPlaying()) {
            stopMusic();
        }
    }

    private static void stopMusic() {
        if (music != null) {
            music.stop();
            music.dispose();
        }
        music = null;
        musicOwner = null;
        fading = -1f;
    }

    /**
     * Frees every sound, as the game closes.
     */
    public static void dispose() {
        stopMusic();
        SOUNDS.values().forEach(sound -> {
            if (sound != null) {
                sound.dispose();
            }
        });
        SOUNDS.clear();
    }
}
