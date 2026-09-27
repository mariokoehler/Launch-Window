package de.mkoehler.launchwindow.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Proves Jackson round-trips a plain record to/from JSON with zero extra
 * configuration - the exact mechanism design.md 3.7/3.8 relies on for every
 * on-disk snapshot, action-journal entry, and account record. Delete once
 * real persistence types have their own equivalent tests.
 *
 * @author Mario Koehler
 */
class JacksonRoundTripTest {

    @Test
    void roundTripsThroughJson() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        PlaceholderSnapshot original = new PlaceholderSnapshot("Hanseatic Space Agency", 25_000, 1);

        String json = mapper.writeValueAsString(original);
        PlaceholderSnapshot restored = mapper.readValue(json, PlaceholderSnapshot.class);

        assertEquals(original, restored);
    }
}
