package de.mkoehler.launchwindow.content;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.model.Archetype;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The agency names the Join screen suggests ({@code ui-spec.md} &sect;4.2): a pool per archetype, so a
 * player put on the spot gets a name that fits the game. They live in
 * {@code assets/content/agency-names.json}, outside the frozen content pack like the display text.
 *
 * @param nasa   the NASA-style names
 * @param soviet the Soviet-style names
 * @author Mario Koehler
 */
@JsonIgnoreProperties({"_comment"})
public record AgencyNames(List<String> nasa, List<String> soviet) {

    /** The file's name, under the assets' {@code content/} folder. */
    public static final String FILE = "agency-names.json";

    /** No names: what a missing file reads as. */
    public static final AgencyNames EMPTY = new AgencyNames(List.of(), List.of());

    private static volatile AgencyNames standard;

    /**
     * Copies the lists.
     *
     * @param nasa   the NASA-style names
     * @param soviet the Soviet-style names
     */
    public AgencyNames(@JsonProperty("nasa") List<String> nasa, @JsonProperty("soviet") List<String> soviet) {
        this.nasa = nasa == null ? List.of() : List.copyOf(nasa);
        this.soviet = soviet == null ? List.of() : List.copyOf(soviet);
    }

    /**
     * Returns the names bundled with the game, read once from the classpath ({@code content/}).
     *
     * @return the names, or {@link #EMPTY} if the file isn't on the classpath
     */
    public static AgencyNames standard() {
        AgencyNames loaded = standard;
        if (loaded == null) {
            try (InputStream in = AgencyNames.class.getClassLoader().getResourceAsStream("content/" + FILE)) {
                loaded = in == null ? EMPTY : new ObjectMapper().readValue(in, AgencyNames.class);
            } catch (IOException e) {
                throw new UncheckedIOException("Could not read " + FILE, e);
            }
            standard = loaded;
        }
        return loaded;
    }

    /**
     * Reads the names from a file.
     *
     * @param file the file
     * @return the names
     * @throws UncheckedIOException if the file can't be read
     */
    public static AgencyNames load(Path file) {
        try {
            return new ObjectMapper().readValue(Files.readString(file), AgencyNames.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + file, e);
        }
    }

    /**
     * Returns the names for an archetype, or both pools when none is picked yet.
     *
     * @param archetype the archetype, or {@code null} for both
     * @return the names
     */
    public List<String> forArchetype(Archetype archetype) {
        if (archetype == null) {
            List<String> both = new ArrayList<>(nasa);
            both.addAll(soviet);
            return List.copyOf(both);
        }
        return archetype == Archetype.SOVIET ? soviet : nasa;
    }
}
