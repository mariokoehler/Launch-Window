package de.mkoehler.launchwindow.client.view;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

/**
 * The replay's pool of sounds ({@code ui-spec.md} &sect;4.7.3, "Sound"), read from
 * {@code assets/content/replay-sounds.json}, outside the content pack like the clips. Each sound has an
 * id that the clips' cues and the console name, a kind, a volume and any number of takes, picked
 * with no repeats until all have played. A sound with no takes yet is wanted: its description is
 * the brief for making it, and the replay plays nothing for it until it has one.
 *
 * @author Mario Koehler
 */
public final class ReplaySounds {

    /** The catalog, under {@code assets/content/}. */
    public static final String FILE = "replay-sounds.json";
    /** Where the sounds are, under {@code assets/}. */
    public static final String FOLDER = "audio/replay/";
    /** The fanfare at the end of a mission's replay that didn't fail, glitches or not. */
    public static final String FANFARE_SUCCESS = "fanfare-success";
    /** The fanfare at the end of a mission's replay that failed. */
    public static final String FANFARE_FAILURE = "fanfare-failure";
    /** Mission control's room tone. */
    public static final String ROOM_TONE = "room-tone";

    private static final ObjectMapper JSON = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static ReplaySounds standard;

    /**
     * How a sound plays.
     */
    public enum Kind {
        /** Once, from memory. */
        EFFECT,
        /** Over and over until stopped, from memory. */
        LOOP,
        /** A voice on the radio, streamed. */
        RADIO,
        /** A fanfare, streamed, once, at the player's music volume. */
        MUSIC,
        /** The room's sound, streamed, looping. */
        ROOM
    }

    /**
     * One sound.
     *
     * @param id          its id
     * @param kind        how it plays
     * @param volume      its volume, 0 to 1, before the player's setting
     * @param files       its takes, under {@code assets/}; empty while it's wanted
     * @param credit      whether {@code assets/CREDITS.md} must credit its takes
     * @param description what it is, and the brief for making it
     */
    public record Sound(String id, Kind kind, float volume, List<String> files, boolean credit, String description) {
    }

    /**
     * The catalog's file.
     *
     * @param sounds the sounds
     */
    public record Catalog(List<Sound> sounds) {
    }

    private final Map<String, Sound> sounds = new HashMap<>();
    private final List<Sound> all;
    private final Map<String, Deque<String>> decks = new HashMap<>();
    private final Map<String, String> lastTake = new HashMap<>();
    private final Random random = new Random();

    private ReplaySounds(List<Sound> sounds) {
        this.all = List.copyOf(sounds);
        for (Sound sound : sounds) {
            this.sounds.put(sound.id(), sound);
        }
    }

    /**
     * The game's sounds, read from the classpath once; with no catalog, none.
     *
     * @return the sounds
     */
    public static synchronized ReplaySounds standard() {
        if (standard == null) {
            try (InputStream in = ReplaySounds.class.getClassLoader().getResourceAsStream("content/" + FILE)) {
                standard = new ReplaySounds(in == null ? List.of() : JSON.readValue(in, Catalog.class).sounds());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return standard;
    }

    /**
     * Reads the sounds from the repository's folders (tests and tools).
     *
     * @param assets the {@code assets} folder
     * @return the sounds
     */
    public static ReplaySounds load(Path assets) {
        try {
            return new ReplaySounds(JSON.readValue(Files.readString(assets.resolve("content").resolve(FILE)),
                Catalog.class).sounds());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * A catalog of given sounds (tests).
     *
     * @param sounds the sounds
     * @return the catalog
     */
    static ReplaySounds of(List<Sound> sounds) {
        return new ReplaySounds(sounds);
    }

    /**
     * Returns every sound, in the catalog's order.
     *
     * @return the sounds
     */
    public List<Sound> all() {
        return all;
    }

    /**
     * Returns a sound.
     *
     * @param id its id
     * @return the sound, if the catalog has it
     */
    public Optional<Sound> sound(String id) {
        return Optional.ofNullable(sounds.get(id));
    }

    /**
     * Returns the next take of a sound: shuffled, no repeats until all have played, and never the
     * same take twice in a row.
     *
     * @param id the sound's id
     * @return the take, under {@code assets/}, or {@code null} while the sound has none
     */
    public synchronized String take(String id) {
        Sound sound = sounds.get(id);
        if (sound == null || sound.files().isEmpty()) {
            return null;
        }
        Deque<String> deck = decks.computeIfAbsent(id, key -> new ArrayDeque<>());
        if (deck.isEmpty()) {
            List<String> shuffled = new ArrayList<>(sound.files());
            Collections.shuffle(shuffled, random);
            String last = lastTake.get(id);
            if (shuffled.size() > 1 && shuffled.get(0).equals(last)) {
                Collections.swap(shuffled, 0, shuffled.size() - 1);
            }
            deck.addAll(shuffled);
        }
        String take = deck.poll();
        lastTake.put(id, take);
        return take;
    }
}
