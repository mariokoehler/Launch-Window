package de.mkoehler.launchwindow.server.schedule;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.server.GameServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>Tests for {@link GameMetaStore}.</p>
 *
 * @author Mario Koehler
 */
class GameMetaStoreTest {

    private final ObjectMapper mapper = GameServer.createMapper();

    /**
     * Verifies that a missing file is reported as created, defaults to a
     * DAILY schedule in {@code Europe/Berlin}, and is left readable on disk
     * afterward.
     */
    @Test
    void createsAndPersistsADefaultWhenNoFileExists(@TempDir Path dataRoot) throws IOException {
        Path file = dataRoot.resolve("game-meta.json");

        GameMetaStore.Result result = GameMetaStore.loadOrCreateDefault(file, mapper);

        assertTrue(result.created());
        assertEquals(EnumSet.allOf(DayOfWeek.class), result.meta().getSchedule().getActiveDays());
        assertEquals(ZoneId.of("Europe/Berlin"), result.meta().getSchedule().getTimezone());
        assertTrue(file.toFile().exists());
    }

    /**
     * Verifies that the created default is written as a human-readable
     * {@code "09:00"} string, not a numeric {@code [9,0]} array - the shape
     * a host is expected to hand-edit must actually be readable by a host.
     */
    @Test
    void theCreatedDefaultIsHumanReadable(@TempDir Path dataRoot) throws IOException {
        Path file = dataRoot.resolve("game-meta.json");

        GameMetaStore.loadOrCreateDefault(file, mapper);

        String content = Files.readString(file);
        assertTrue(content.contains("\"09:00"), "expected a readable time string, got: " + content);
        assertTrue(content.contains("\"Europe/Berlin\""), "expected a readable zone string, got: " + content);
    }

    /**
     * Verifies that an existing file is loaded rather than overwritten with
     * a fresh default - a host's own edits to an already-created file must
     * survive the next boot.
     */
    @Test
    void loadsAnExistingFileWithoutOverwritingIt(@TempDir Path dataRoot) throws IOException {
        Path file = dataRoot.resolve("game-meta.json");
        GameMetaStore.loadOrCreateDefault(file, mapper);
        // Hand-edit the file, as a host would, to something the default would never produce.
        ScheduleProfile edited = ScheduleProfile.weekdays(LocalTime.of(18, 30), ZoneId.of("America/New_York"));
        mapper.writeValue(file.toFile(), new GameMeta(edited, List.of()));

        GameMetaStore.Result second = GameMetaStore.loadOrCreateDefault(file, mapper);

        assertFalse(second.created());
        assertEquals(ZoneId.of("America/New_York"), second.meta().getSchedule().getTimezone());
    }

    /**
     * Verifies that a hand-edited file failing validation is rejected
     * loudly rather than silently accepted.
     */
    @Test
    void loadRejectsAnInvalidExistingFile(@TempDir Path dataRoot) throws IOException {
        Path file = dataRoot.resolve("game-meta.json");
        Files.writeString(file, "{\"schedule\":{\"activeDays\":[],\"resolutionTime\":\"09:00\",\"timezone\":\"UTC\"},\"blackouts\":[]}");

        assertThrows(IllegalStateException.class, () -> GameMetaStore.loadOrCreateDefault(file, mapper));
    }
}
