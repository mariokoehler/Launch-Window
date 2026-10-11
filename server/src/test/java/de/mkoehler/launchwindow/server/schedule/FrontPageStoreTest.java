package de.mkoehler.launchwindow.server.schedule;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.model.FrontPage;
import de.mkoehler.launchwindow.server.GameServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link FrontPageStore} ({@code ui-spec.md} &sect;4.18, G14).
 *
 * @author Mario Koehler
 */
class FrontPageStoreTest {

    private final ObjectMapper mapper = GameServer.createMapper();

    @Test
    void noFileReadsAsNoPagesAndANoFirstTurnWritesNone(@TempDir Path dataRoot) throws IOException {
        assertTrue(FrontPageStore.load(dataRoot, mapper).isEmpty());
        FrontPageStore.keep(dataRoot, mapper, 4, List.of());
        assertFalse(Files.exists(dataRoot.resolve(FrontPageStore.FILE_NAME)));
    }

    @Test
    void aRetriedTurnReplacesItsPagesAndRollingBackDropsThem(@TempDir Path dataRoot) throws IOException {
        FrontPage satellite = page(11, "earth-orbit-satellite");
        FrontPage space = page(25, "human-in-space");
        FrontPageStore.keep(dataRoot, mapper, 25, List.of(space));
        FrontPageStore.keep(dataRoot, mapper, 11, List.of(satellite));
        assertEquals(List.of(satellite, space), FrontPageStore.load(dataRoot, mapper), "oldest first");

        FrontPageStore.keep(dataRoot, mapper, 25, List.of(space));
        assertEquals(List.of(satellite, space), FrontPageStore.load(dataRoot, mapper), "a retry prints the same once");

        FrontPageStore.dropFrom(dataRoot, mapper, 12);
        assertEquals(List.of(satellite), FrontPageStore.load(dataRoot, mapper));
        FrontPageStore.dropFrom(dataRoot, mapper, 11);
        assertTrue(FrontPageStore.load(dataRoot, mapper).isEmpty());
    }

    private static FrontPage page(int turn, String goal) {
        return new FrontPage(FrontPage.idOf(turn, goal), goal, turn, "story", "Mild, light winds", List.of(),
            List.of(new FrontPage.Achiever("Orion Spaceworks", "Ana Reyes", List.of("Daniel Hale"))),
            List.of("Kestrel Aerospace"));
    }
}
