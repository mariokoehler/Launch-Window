package de.mkoehler.launchwindow.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies the saved snapshot's format version (review.md 2.1): files from before versioning load as
 * format 1, steps run in order up to the current format, and a file from a newer build is refused.
 *
 * @author Mario Koehler
 */
class SnapshotMigrationsTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void aFileFromBeforeVersioningLoads(@TempDir Path folder) throws Exception {
        PlayerSnapshotStore store = new PlayerSnapshotStore(folder, mapper);
        store.save(1, new PlayerSnapshot("Vega Bureau", Archetype.SOVIET, 50_000, 250, 3_000, 1), null);
        Path file = folder.resolve("snapshots").resolve("1.json");
        ObjectNode written = (ObjectNode) mapper.readTree(file.toFile());
        assertEquals(SnapshotMigrations.CURRENT, written.get("formatVersion").asInt(), "new files carry the format");

        written.remove("formatVersion");
        Files.writeString(file, mapper.writeValueAsString(written));
        assertEquals("Vega Bureau", store.load(1).orElseThrow().getAgencyName());
    }

    @Test
    void stepsRunInOrderUpToTheCurrentFormat() {
        ObjectNode envelope = mapper.createObjectNode();
        envelope.putObject("snapshot").put("agency", "Vega Bureau");
        List<SnapshotMigrations.Step> steps = List.of(
            new SnapshotMigrations.Step(2, node -> {
                ((ObjectNode) node.get("snapshot")).put("agencyName", node.get("snapshot").get("agency").asText());
                ((ObjectNode) node.get("snapshot")).remove("agency");
                return node;
            }),
            new SnapshotMigrations.Step(1, node -> {
                ((ObjectNode) node.get("snapshot")).put("agency", node.get("snapshot").get("agency").asText() + "!");
                return node;
            }));

        ObjectNode migrated = SnapshotMigrations.migrate(envelope, steps, 3);

        assertEquals("Vega Bureau!", migrated.get("snapshot").get("agencyName").asText());
        assertEquals(3, migrated.get("formatVersion").asInt());
    }

    @Test
    void aFileFromANewerBuildIsRefused() {
        ObjectNode envelope = mapper.createObjectNode().put("formatVersion", SnapshotMigrations.CURRENT + 1);
        assertThrows(IllegalStateException.class, () -> SnapshotMigrations.migrate(envelope));
    }

    @Test
    void aMissingStepIsLoud() {
        ObjectNode envelope = mapper.createObjectNode().put("formatVersion", 1);
        assertThrows(IllegalStateException.class, () -> SnapshotMigrations.migrate(envelope, List.of(), 2));
    }
}
