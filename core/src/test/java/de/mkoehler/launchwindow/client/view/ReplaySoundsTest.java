package de.mkoehler.launchwindow.client.view;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ReplaySounds}: the catalog names every sound the console and the beds play,
 * every take is there and in the catalog, credited where it must be, and takes don't repeat.
 *
 * @author Mario Koehler
 */
class ReplaySoundsTest {

    private static final Path ASSETS = Path.of("..", "assets");
    private static final ReplaySounds SOUNDS = ReplaySounds.load(ASSETS);

    @Test
    void theCatalogHasEverySoundTheReplayPlays() {
        Set<String> ids = new HashSet<>();
        for (ReplaySounds.Sound sound : SOUNDS.all()) {
            assertTrue(ids.add(sound.id()), "twice: " + sound.id());
            assertFalse(sound.description().isBlank(), sound.id());
            assertTrue(sound.volume() > 0f && sound.volume() <= 1f, sound.id());
        }
        for (String id : List.of(ReplayConsole.BLIP, ReplayConsole.CAUTION_TONE, ReplayConsole.MASTER_ALARM,
            ReplayConsole.STATIC, ReplayConsole.HOLD_CLICK, ReplaySounds.FANFARE_SUCCESS, ReplaySounds.FANFARE_FAILURE,
            ReplaySounds.ROOM_TONE)) {
            assertTrue(ids.contains(id), id);
        }
        assertEquals(ReplaySounds.Kind.MUSIC, SOUNDS.sound(ReplaySounds.FANFARE_SUCCESS).orElseThrow().kind());
        assertEquals(ReplaySounds.Kind.MUSIC, SOUNDS.sound(ReplaySounds.FANFARE_FAILURE).orElseThrow().kind());
        assertEquals(ReplaySounds.Kind.ROOM, SOUNDS.sound(ReplaySounds.ROOM_TONE).orElseThrow().kind());
        assertEquals(ReplaySounds.Kind.LOOP, SOUNDS.sound(ReplayConsole.MASTER_ALARM).orElseThrow().kind());
        assertEquals(ReplaySounds.Kind.LOOP, SOUNDS.sound(ReplayConsole.STATIC).orElseThrow().kind());
    }

    @Test
    void everyTakeIsThereListedAndCredited() throws IOException {
        String credits = Files.readString(ASSETS.resolve("CREDITS.md"), StandardCharsets.UTF_8);
        Set<String> listed = new HashSet<>();
        for (ReplaySounds.Sound sound : SOUNDS.all()) {
            for (String file : sound.files()) {
                assertTrue(file.startsWith(ReplaySounds.FOLDER), file);
                assertTrue(Files.exists(ASSETS.resolve(file)), file);
                assertTrue(listed.add(file), "twice: " + file);
                if (sound.credit()) {
                    assertTrue(credits.contains(file), "not credited: " + file);
                }
                // A loop must be seamless, and MP3 pads its ends.
                if (sound.kind() == ReplaySounds.Kind.LOOP || sound.kind() == ReplaySounds.Kind.ROOM) {
                    assertTrue(file.endsWith(".ogg") || file.endsWith(".wav"), "not seamless: " + file);
                }
            }
        }
        Path folder = ASSETS.resolve(ReplaySounds.FOLDER);
        if (Files.isDirectory(folder)) {
            try (Stream<Path> files = Files.walk(folder)) {
                files.filter(Files::isRegularFile).map(path -> ASSETS.relativize(path).toString().replace('\\', '/'))
                    .forEach(file -> assertTrue(listed.contains(file), "not in the catalog: " + file));
            }
        }
    }

    @Test
    void takesDontRepeatUntilAllHavePlayed() {
        ReplaySounds sounds = ReplaySounds.load(ASSETS);
        assertNull(sounds.take("no-such-sound"));
        ReplaySounds.Sound blip = new ReplaySounds.Sound("blip", ReplaySounds.Kind.EFFECT, 0.5f,
            List.of("a", "b", "c"), false, "test");
        ReplaySounds three = ReplaySounds.of(List.of(blip));
        String previous = null;
        for (int round = 0; round < 20; round++) {
            Set<String> seen = new HashSet<>();
            for (int i = 0; i < 3; i++) {
                String take = three.take("blip");
                assertNotEquals(previous, take);
                assertTrue(seen.add(take), "repeated " + take);
                previous = take;
            }
        }
    }
}
