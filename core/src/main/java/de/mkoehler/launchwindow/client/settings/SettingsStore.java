package de.mkoehler.launchwindow.client.settings;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Reads and writes the {@link ClientSettings} as a JSON file, the same way as the sibling projects. The
 * file is not the player's to depend on: if it is missing or can't be read, the defaults are used, and
 * if it can't be written, the game carries on without remembering.
 *
 * @author Mario Koehler
 */
public final class SettingsStore {

    private static final String DIRECTORY = ".launch-window";
    private static final String FILE = "client-settings.json";

    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private final Path file;

    /**
     * Creates a store for a given file.
     *
     * @param file where the settings are kept; the folder is created when saving
     */
    public SettingsStore(Path file) {
        this.file = file;
    }

    /**
     * Creates the store the game uses: a file in a hidden folder in the player's home directory.
     *
     * @return the store
     */
    public static SettingsStore inHomeDirectory() {
        return new SettingsStore(Path.of(System.getProperty("user.home"), DIRECTORY, FILE));
    }

    /**
     * Loads the settings, merged onto {@link ClientSettings#defaults()}: only the properties the file
     * has override a default, so a file saved by an older version still gets the real default for what
     * it lacks.
     *
     * @return the saved settings, or the defaults if there is no readable file
     */
    public ClientSettings load() {
        if (!Files.isRegularFile(file)) {
            return ClientSettings.defaults();
        }
        try {
            JsonNode loaded = mapper.readTree(file.toFile());
            if (!(loaded instanceof ObjectNode loadedObject)) {
                return ClientSettings.defaults();
            }
            ObjectNode merged = mapper.valueToTree(ClientSettings.defaults());
            merged.setAll(loadedObject);
            return mapper.treeToValue(merged, ClientSettings.class);
        } catch (IOException | RuntimeException e) {
            return ClientSettings.defaults();
        }
    }

    /**
     * Saves the settings. The file is written in full to a neighbour first and then moved into place,
     * so a crash can't leave half a file behind.
     *
     * @param settings what to remember
     * @return {@code true} if the settings were written, {@code false} if writing failed
     */
    public boolean save(ClientSettings settings) {
        try {
            Files.createDirectories(file.getParent());
            Path temporary = file.resolveSibling(FILE + ".tmp");
            mapper.writeValue(temporary.toFile(), settings);
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }
}
