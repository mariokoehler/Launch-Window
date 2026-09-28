package de.mkoehler.launchwindow.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * <p>Tests for {@link ResolvedSnapshot}.</p>
 *
 * @author Mario Koehler
 */
class ResolvedSnapshotTest {

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Verifies that the snapshot and its cutoff both survive a Jackson
     * round trip unchanged.
     */
    @Test
    void roundTripsThroughJson() throws IOException {
        PlayerSnapshot snapshot = new PlayerSnapshot("Hanseatic Space Agency", Archetype.NASA, 1_000, 0, 0, 1);
        ResolvedSnapshot original = new ResolvedSnapshot(snapshot, "00000000000000000001-abc.json");

        String json = mapper.writeValueAsString(original);
        ResolvedSnapshot restored = mapper.readValue(json, ResolvedSnapshot.class);

        assertEquals(1_000, restored.getSnapshot().getFunds());
        assertEquals("00000000000000000001-abc.json", restored.getLastAppliedActionFile());
    }

    /**
     * Verifies that a {@code null} cutoff (no actions were pending that
     * turn) round-trips as {@code null}, not an empty string.
     */
    @Test
    void roundTripsANullCutoff() throws IOException {
        PlayerSnapshot snapshot = new PlayerSnapshot("Hanseatic Space Agency", Archetype.NASA, 1_000, 0, 0, 1);
        ResolvedSnapshot original = new ResolvedSnapshot(snapshot, null);

        String json = mapper.writeValueAsString(original);
        ResolvedSnapshot restored = mapper.readValue(json, ResolvedSnapshot.class);

        assertNull(restored.getLastAppliedActionFile());
    }
}
