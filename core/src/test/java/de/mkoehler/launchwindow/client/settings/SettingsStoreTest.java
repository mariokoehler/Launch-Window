package de.mkoehler.launchwindow.client.settings;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link SettingsStore}: the Connect screen's remembered server, login and password.
 *
 * @author Mario Koehler
 */
class SettingsStoreTest {

    @TempDir
    Path folder;

    /**
     * What was saved comes back, and the folder is created on the way.
     */
    @Test
    void savedSettingsComeBack() {
        SettingsStore store = new SettingsStore(folder.resolve("sub").resolve("client-settings.json"));
        ClientSettings settings = new ClientSettings("nas.local:45825", "mario", "secret");
        assertTrue(store.save(settings));
        assertEquals(settings, store.load());
    }

    /**
     * No file, a broken file, or a file missing a property all fall back to the defaults for what is
     * missing; unknown properties are ignored.
     */
    @Test
    void missingOrBrokenFilesGiveTheDefaults() throws IOException {
        Path file = folder.resolve("client-settings.json");
        SettingsStore store = new SettingsStore(file);
        assertEquals(ClientSettings.defaults(), store.load());

        Files.writeString(file, "{ not json");
        assertEquals(ClientSettings.defaults(), store.load());

        Files.writeString(file, "{\"login\": \"mario\", \"volume\": 0.5}");
        assertEquals(new ClientSettings(ClientSettings.defaults().server(), "mario", ""), store.load());
    }
}
