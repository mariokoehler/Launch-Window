package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import de.mkoehler.launchwindow.client.LaunchWindowGame;
import de.mkoehler.launchwindow.client.view.ReplayClips;
import de.mkoehler.launchwindow.client.view.ReplayConsole;
import de.mkoehler.launchwindow.client.view.ReplaySounds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * The replay's sound ({@code ui-spec.md} &sect;4.7.3, "Sound"), in layers: the room tone under the
 * whole replay; the console's blips, tones and alarm, from its {@link ReplayConsole}; each clip's cues
 * from its sidecar, in step with the clip's time; and a fanfare at the season summary, one for a
 * mission that failed and one for any other. Nothing is baked into the videos.
 * <ul>
 *   <li>A step that follows on by itself lets the last one's loops fade as their cues say, and a loop
 *   the new clip carries on keeps playing; a skip or a step back fades everything but the room within
 *   a tenth of a second, and the fanfare a little slower.</li>
 *   <li>One voice on the radio at a time: a call due while another is still talking waits for it, up to
 *   {@link #RADIO_WAIT}, and is dropped if it can't start by then or its step ends first.</li>
 *   <li>Silent when the game's sound is off (checked every frame); a sound with no takes yet plays
 *   nothing.</li>
 * </ul>
 *
 * @author Mario Koehler
 */
final class ReplayAudio {

    /** How fast a skip silences the step's sounds, in seconds. */
    private static final float SKIP_FADE = 0.1f;
    /** How fast leaving the summary silences its fanfare, in seconds. */
    private static final float FANFARE_FADE = 0.3f;
    /** How long a radio call waits for the one still talking, in seconds. */
    private static final float RADIO_WAIT = 1.5f;

    private final ReplaySounds sounds;
    private final Map<String, Sound> loaded = new HashMap<>();
    private final List<Loop> loops = new ArrayList<>();
    private final List<Music> radio = new ArrayList<>();
    private String waitingVoice;
    private Float waitingLevel;
    private float waitingFor;
    private ReplayClips.Clip clip;
    private ReplayConsole console;
    private float previous;
    private Boolean held;
    private Music fanfare;
    private boolean fanfareFailed;
    private boolean fanfareFading;
    private float fanfareFadeLeft;
    private Music room;

    /** A looping sound playing. */
    private static final class Loop {
        private final String sound;
        private final Sound clip;
        private final long id;
        private final float volume;
        /** Whether the console plays it (the alarm, the static), not a clip's cue. */
        private final boolean console;
        private Float until;
        private float fade;
        private float fadeLeft = -1f;

        private Loop(String sound, Sound clip, long id, float volume, boolean console, Float until, float fade) {
            this.sound = sound;
            this.clip = clip;
            this.id = id;
            this.volume = volume;
            this.console = console;
            this.until = until;
            this.fade = fade;
        }

        private boolean fading() {
            return fadeLeft >= 0f;
        }

        private void fadeOut(float seconds) {
            if (!fading()) {
                fade = Math.max(seconds, 0.001f);
                fadeLeft = fade;
            }
        }
    }

    /**
     * Builds the replay's sound, silent until the first step.
     *
     * @param sounds the pool of sounds
     */
    ReplayAudio(ReplaySounds sounds) {
        this.sounds = sounds;
    }

    /**
     * Starts a step.
     *
     * @param next    the step's clip
     * @param machine the step's console
     * @param natural whether the step follows the one before by itself ({@code false}: a skip)
     */
    void step(ReplayClips.Clip next, ReplayConsole machine, boolean natural) {
        if (fanfare != null && !fanfareFading) {
            fanfareFading = true;
            fanfareFadeLeft = FANFARE_FADE;
        }
        for (Loop loop : loops) {
            if (natural && next != null && carried(loop, next)) {
                continue;
            }
            boolean staticGoesOn = loop.console && loop.sound.equals(ReplayConsole.STATIC) && machine != null
                && !machine.live();
            if (staticGoesOn) {
                continue;
            }
            loop.fadeOut(natural ? loop.fade : SKIP_FADE);
        }
        if (!natural || next == null) {
            radio.forEach(Music::stop);
        }
        waitingVoice = null;
        clip = next;
        console = machine;
        previous = -0.001f;
    }

    /**
     * Shows the season summary: the step's sounds end, and the fanfare plays, once.
     *
     * @param failed  whether the mission failed (a glitch, solved or not, isn't a failure)
     * @param natural whether the summary follows the last step by itself ({@code false}: a skip)
     */
    void summary(boolean failed, boolean natural) {
        step(null, null, natural);
        if (fanfare != null) {
            fanfare.dispose();
        }
        fanfareFading = false;
        fanfareFailed = failed;
        fanfare = LaunchWindowGame.soundOn()
            ? stream(sounds.take(failed ? ReplaySounds.FANFARE_FAILURE : ReplaySounds.FANFARE_SUCCESS)) : null;
        if (fanfare != null) {
            fanfare.setVolume(fanfareVolume(failed));
            fanfare.play();
        }
    }

    private float fanfareVolume(boolean failed) {
        return volume(failed ? ReplaySounds.FANFARE_FAILURE : ReplaySounds.FANFARE_SUCCESS)
            * LaunchWindowGame.musicVolume();
    }

    /** Whether the next clip carries a running loop on, rather than starting it again. */
    private static boolean carried(Loop loop, ReplayClips.Clip next) {
        for (ReplayClips.Cue cue : next.cues()) {
            if (cue.at() == 0 && cue.loops() && cue.carries() && cue.sound().equals(loop.sound) && !loop.console
                && !loop.fading()) {
                loop.until = cue.until() == null ? null : cue.until().floatValue();
                loop.fade = cue.fade() == null ? 0f : cue.fade().floatValue();
                return true;
            }
        }
        return false;
    }

    /**
     * Moves the sound on; call once a frame.
     *
     * @param t       seconds into the step
     * @param playing whether the replay moves on by itself
     * @param delta   seconds since the last frame
     */
    void update(float t, boolean playing, float delta) {
        if (!LaunchWindowGame.soundOn()) {
            stop();
            // Turned back on, the step goes on from now, not with everything it missed at once.
            previous = t;
            return;
        }
        startRoom();
        if (held != null && held == playing) {
            effect(ReplayConsole.HOLD_CLICK, null);
        }
        held = !playing;
        if (clip != null && console != null) {
            for (ReplayClips.Cue cue : clip.cues()) {
                if (cue.at() > previous && cue.at() <= t
                    && !(cue.carries() && cue.at() == 0 && running(cue.sound(), false))) {
                    play(cue);
                }
            }
            for (String sound : console.sounds(previous, t)) {
                effect(sound, null);
            }
            keepLooping(ReplayConsole.MASTER_ALARM, console.alarmSounding(t));
            keepLooping(ReplayConsole.STATIC, !console.live());
            for (Loop loop : loops) {
                if (loop.until != null && t >= loop.until) {
                    loop.fadeOut(loop.fade);
                }
            }
            previous = t;
        }
        fade(delta);
        radio.removeIf(voice -> {
            if (voice.isPlaying()) {
                return false;
            }
            voice.dispose();
            return true;
        });
        if (waitingVoice != null) {
            waitingFor += delta;
            if (radio.isEmpty()) {
                String id = waitingVoice;
                waitingVoice = null;
                voice(id, waitingLevel);
            } else if (waitingFor > RADIO_WAIT) {
                waitingVoice = null;
            }
        }
        if (fanfare != null) {
            float level = 1f;
            if (fanfareFading) {
                fanfareFadeLeft -= delta;
                level = Math.max(fanfareFadeLeft, 0f) / FANFARE_FADE;
            }
            if (!fanfare.isPlaying() || level <= 0f) {
                fanfare.dispose();
                fanfare = null;
                fanfareFading = false;
            } else {
                fanfare.setVolume(fanfareVolume(fanfareFailed) * level);
            }
        }
        if (room != null) {
            room.setVolume(volume(ReplaySounds.ROOM_TONE) * LaunchWindowGame.effectsVolume());
        }
    }

    private boolean running(String sound, boolean console) {
        return loops.stream().anyMatch(loop -> loop.sound.equals(sound) && loop.console == console && !loop.fading());
    }

    /** Starts or stops one of the console's loops, leaving the clips' cues alone. */
    private void keepLooping(String sound, boolean wanted) {
        boolean playing = running(sound, true);
        if (wanted && !playing) {
            loop(sound, null, true, null, 0f);
        } else if (!wanted && playing) {
            loops.stream().filter(loop -> loop.console && loop.sound.equals(sound))
                .forEach(loop -> loop.fadeOut(SKIP_FADE));
        }
    }

    private void play(ReplayClips.Cue cue) {
        ReplaySounds.Sound sound = sounds.sound(cue.sound()).orElse(null);
        if (sound == null) {
            return;
        }
        Float level = cue.volume() == null ? null : cue.volume().floatValue();
        if (sound.kind() == ReplaySounds.Kind.RADIO) {
            voice(cue.sound(), level);
        } else if (cue.loops() || sound.kind() == ReplaySounds.Kind.LOOP) {
            loop(cue.sound(), level, false, cue.until() == null ? null : cue.until().floatValue(),
                cue.fade() == null ? 0f : cue.fade().floatValue());
        } else {
            effect(cue.sound(), level);
        }
    }

    private void effect(String id, Float level) {
        Sound clip = load(sounds.take(id));
        if (clip != null) {
            clip.play((level != null ? level : volume(id)) * LaunchWindowGame.effectsVolume());
        }
    }

    private void loop(String id, Float level, boolean console, Float until, float fade) {
        Sound clip = load(sounds.take(id));
        if (clip != null) {
            float base = level != null ? level : volume(id);
            long handle = clip.loop(base * LaunchWindowGame.effectsVolume());
            loops.add(new Loop(id, clip, handle, base, console, until, fade));
        }
    }

    private void voice(String id, Float level) {
        if (!radio.isEmpty()) {
            waitingVoice = id;
            waitingLevel = level;
            waitingFor = 0f;
            return;
        }
        Music voice = stream(sounds.take(id));
        if (voice != null) {
            voice.setVolume((level != null ? level : volume(id)) * LaunchWindowGame.effectsVolume());
            voice.play();
            radio.add(voice);
        }
    }

    /** Fades the loops that are fading, keeps the rest at the player's volume. */
    private void fade(float delta) {
        float effects = LaunchWindowGame.effectsVolume();
        for (Iterator<Loop> it = loops.iterator(); it.hasNext(); ) {
            Loop loop = it.next();
            if (!loop.fading()) {
                loop.clip.setVolume(loop.id, loop.volume * effects);
                continue;
            }
            loop.fadeLeft -= delta;
            if (loop.fadeLeft <= 0f) {
                loop.clip.stop(loop.id);
                it.remove();
            } else {
                loop.clip.setVolume(loop.id, loop.volume * effects * loop.fadeLeft / loop.fade);
            }
        }
    }

    private void startRoom() {
        if (room != null) {
            return;
        }
        room = stream(sounds.take(ReplaySounds.ROOM_TONE));
        if (room != null) {
            room.setLooping(true);
            room.setVolume(volume(ReplaySounds.ROOM_TONE) * LaunchWindowGame.effectsVolume());
            room.play();
        }
    }

    private float volume(String id) {
        return sounds.sound(id).map(ReplaySounds.Sound::volume).orElse(1f);
    }

    private Sound load(String file) {
        if (file == null) {
            return null;
        }
        if (!loaded.containsKey(file)) {
            Sound clip = null;
            try {
                clip = Gdx.audio.newSound(Gdx.files.internal(file));
            } catch (RuntimeException e) {
                Gdx.app.error("Replay", "can't load " + file, e);
            }
            loaded.put(file, clip);
        }
        return loaded.get(file);
    }

    private static Music stream(String file) {
        if (file == null) {
            return null;
        }
        try {
            return Gdx.audio.newMusic(Gdx.files.internal(file));
        } catch (RuntimeException e) {
            Gdx.app.error("Replay", "can't stream " + file, e);
            return null;
        }
    }

    /**
     * Stops everything, the fanfare and the room too: the screen is hidden or the sound turned off. The
     * room starts again with the next frame that has sound; the fanfare doesn't.
     */
    void stop() {
        for (Loop loop : loops) {
            loop.clip.stop(loop.id);
        }
        loops.clear();
        radio.forEach(Music::dispose);
        radio.clear();
        waitingVoice = null;
        if (fanfare != null) {
            fanfare.dispose();
            fanfare = null;
        }
        fanfareFading = false;
        if (room != null) {
            room.dispose();
            room = null;
        }
        held = null;
    }

    /**
     * Stops everything and frees the sounds.
     */
    void dispose() {
        stop();
        loaded.values().stream().filter(java.util.Objects::nonNull).forEach(Sound::dispose);
        loaded.clear();
    }
}
