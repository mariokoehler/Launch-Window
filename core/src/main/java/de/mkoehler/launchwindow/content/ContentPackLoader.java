package de.mkoehler.launchwindow.content;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * <p>Loads a content pack - one JSON file per content type - into a validated
 * {@link ContentCatalog}. M3's data-driven content format, see
 * {@code design.md} &sect;5.</p>
 *
 * <p>A pack is a folder holding {@value #CATEGORIES_FILE},
 * {@value #PROGRAMS_FILE}, {@value #MISSION_COMPONENTS_FILE},
 * {@value #MISSION_CONFIGURATIONS_FILE}, and {@value #GOALS_FILE}. Each file is
 * a JSON array whose entries map one-to-one onto the content records' own
 * fields, so the records' validation runs on every entry, and
 * {@link ContentCatalog}'s cross-reference checks run on the whole pack.</p>
 *
 * <p>The mapper is strict on purpose, since a typo in a hand-edited pack
 * should fail loudly rather than load a zero: an unknown field is rejected, a
 * missing number or flag is rejected, and only a missing list defaults to
 * empty.</p>
 *
 * @author Mario Koehler
 */
public final class ContentPackLoader {

    /** The program categories file. */
    public static final String CATEGORIES_FILE = "categories.json";
    /** The programs file. */
    public static final String PROGRAMS_FILE = "programs.json";
    /** The mission components file. */
    public static final String MISSION_COMPONENTS_FILE = "mission-components.json";
    /** The mission configurations file. */
    public static final String MISSION_CONFIGURATIONS_FILE = "mission-configurations.json";
    /** The goals file. */
    public static final String GOALS_FILE = "goals.json";
    /**
     * Every rules file a pack consists of, in a fixed order. The one list the loader, the server's
     * freezing step and {@link ContentPackHash} all go by, so a rules file added to packs later
     * joins all three at once. The display text ({@link ContentStrings#FILE}, gap G4) is
     * deliberately <b>not</b> in here: it changes no rule, so it isn't hashed or frozen, and the
     * client reads its own bundled copy (see {@link ContentStrings}). Adding it would change every
     * bundled pack's hash.
     */
    public static final List<String> FILES = List.of(CATEGORIES_FILE, PROGRAMS_FILE, MISSION_COMPONENTS_FILE,
        MISSION_CONFIGURATIONS_FILE, GOALS_FILE);

    private static final ObjectMapper MAPPER = createMapper();

    /**
     * Not instantiable; this class only exposes static methods.
     */
    private ContentPackLoader() {
    }

    /**
     * Returns the mapper content packs are read with, exposed so tests and tools
     * can write a pack with exactly the same field mapping.
     *
     * @return the shared, strict content mapper
     */
    public static ObjectMapper mapper() {
        return MAPPER;
    }

    /**
     * Loads a pack from a folder on disk.
     *
     * @param packDirectory the folder holding the pack's files
     * @return the validated catalog
     * @throws NullPointerException     if {@code packDirectory} is {@code null}
     * @throws UncheckedIOException     if a file is missing or unreadable
     * @throws IllegalArgumentException if an entry is malformed or the pack's
     *                                   cross-references don't resolve
     */
    public static ContentCatalog load(Path packDirectory) {
        Objects.requireNonNull(packDirectory, "packDirectory must not be null");
        return load(fileName -> Files.newInputStream(packDirectory.resolve(fileName)),
            packDirectory.toString());
    }

    /**
     * Loads a pack from the classpath, as a client or server bundles
     * {@code assets/} onto it.
     *
     * @param resourcePrefix the pack's folder on the classpath, without a
     *                       leading or trailing slash, for example
     *                       {@code content/early-era}
     * @param classLoader    the class loader to read through
     * @return the validated catalog
     * @throws NullPointerException     if an argument is {@code null}
     * @throws UncheckedIOException     if a file is missing or unreadable
     * @throws IllegalArgumentException if an entry is malformed or the pack's
     *                                   cross-references don't resolve
     */
    public static ContentCatalog loadFromClasspath(String resourcePrefix, ClassLoader classLoader) {
        Objects.requireNonNull(resourcePrefix, "resourcePrefix must not be null");
        Objects.requireNonNull(classLoader, "classLoader must not be null");
        return load(fileName -> {
            InputStream in = classLoader.getResourceAsStream(resourcePrefix + "/" + fileName);
            if (in == null) {
                throw new IOException("no classpath resource " + resourcePrefix + "/" + fileName);
            }
            return in;
        }, "classpath:" + resourcePrefix);
    }

    private static ContentCatalog load(Source source, String label) {
        List<ProgramCategory> categories = read(source, label, CATEGORIES_FILE, new TypeReference<>() { });
        List<Program> programs = read(source, label, PROGRAMS_FILE, new TypeReference<>() { });
        List<MissionComponent> components = read(source, label, MISSION_COMPONENTS_FILE, new TypeReference<>() { });
        List<MissionConfiguration> configurations =
            read(source, label, MISSION_CONFIGURATIONS_FILE, new TypeReference<>() { });
        List<Goal> goals = read(source, label, GOALS_FILE, new TypeReference<>() { });
        return new ContentCatalog(categories, programs, configurations, components, goals);
    }

    private static <T> List<T> read(Source source, String label, String fileName, TypeReference<List<T>> type) {
        try (InputStream in = source.open(fileName)) {
            return MAPPER.readValue(in, type);
        } catch (com.fasterxml.jackson.core.JacksonException e) {
            // Record validation failures arrive wrapped; surface them as the content error they are.
            throw new IllegalArgumentException("invalid content in " + label + "/" + fileName + ": "
                + e.getOriginalMessage(), e);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read " + label + "/" + fileName, e);
        }
    }

    private static ObjectMapper createMapper() {
        JsonMapper mapper = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .build();
        // A missing or null list is an empty list; every other missing field fails.
        mapper.configOverride(List.class).setSetterInfo(JsonSetter.Value.forValueNulls(Nulls.AS_EMPTY));
        return mapper;
    }

    /**
     * Opens one of a pack's files by name.
     */
    @FunctionalInterface
    private interface Source {
        InputStream open(String fileName) throws IOException;
    }
}
