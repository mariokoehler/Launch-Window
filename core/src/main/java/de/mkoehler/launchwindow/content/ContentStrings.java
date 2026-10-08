package de.mkoehler.launchwindow.content;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;

/**
 * A content pack's display text (gap G4, {@code ui-spec.md} &sect;6): names, short descriptions and
 * optional history notes for its categories, programs, components, mission configurations, steps and
 * goals, read from the pack's {@value #FILE}. Written in our own words from the real history behind
 * BASPM's content, never BASPM's own text ({@code design.md} &sect;1.2).
 *
 * <p><b>Not part of the frozen pack.</b> {@value #FILE} is deliberately left out of
 * {@link ContentPackLoader#FILES}: it changes no rule, so it isn't hashed and isn't copied into a
 * game's frozen pack, and the client always reads its own bundled copy. A text fix therefore reaches
 * running games without changing their pack hash. Never add it to {@code FILES}: that would change the
 * hash of every bundled pack, and every running game would report its pack as different.</p>
 *
 * <p>Steps are keyed by their type, the step id without its number ({@code 08-retrofire} is
 * {@code retrofire}). A configuration may override single steps by their full id, for steps whose type
 * alone doesn't say which craft or launch they belong to (the joint flights' second launch).</p>
 *
 * @param categories     category text, by id
 * @param programs       program text, by id
 * @param components     mission component text, by id
 * @param configurations mission configuration text, by id; names are relative to the program
 *                       ("Orbital flight", not "Vostok orbital flight")
 * @param steps          step text, by step type
 * @param goals          goal text, by id
 * @author Mario Koehler
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ContentStrings(Map<String, Text> categories, Map<String, Text> programs, Map<String, Text> components,
                             Map<String, ConfigurationText> configurations, Map<String, Text> steps,
                             Map<String, Text> goals) {

    /** The file name inside a pack. */
    public static final String FILE = "strings.json";

    /** No text at all: every name falls back to its id. */
    public static final ContentStrings EMPTY = new ContentStrings(null, null, null, null, null, null);

    /**
     * Replaces missing maps with empty ones.
     */
    public ContentStrings {
        categories = categories == null ? Map.of() : Map.copyOf(categories);
        programs = programs == null ? Map.of() : Map.copyOf(programs);
        components = components == null ? Map.of() : Map.copyOf(components);
        configurations = configurations == null ? Map.of() : Map.copyOf(configurations);
        steps = steps == null ? Map.of() : Map.copyOf(steps);
        goals = goals == null ? Map.of() : Map.copyOf(goals);
    }

    /**
     * Loads a pack's text from the classpath.
     *
     * @param resourcePrefix the pack's folder on the classpath, such as {@code content/early-era}
     * @param classLoader    the class loader to read it with
     * @return the text, or {@link #EMPTY} if the pack has no {@value #FILE}
     * @throws UncheckedIOException     if the file can't be read
     * @throws IllegalArgumentException if the file isn't valid
     */
    public static ContentStrings loadFromClasspath(String resourcePrefix, ClassLoader classLoader) {
        try (InputStream in = classLoader.getResourceAsStream(resourcePrefix + "/" + FILE)) {
            if (in == null) {
                return EMPTY;
            }
            return new ObjectMapper().readValue(in, ContentStrings.class);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalArgumentException("invalid " + resourcePrefix + "/" + FILE + ": " + e.getOriginalMessage(),
                e);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read " + resourcePrefix + "/" + FILE, e);
        }
    }

    /**
     * Loads a pack's text from a pack folder on disk.
     *
     * @param packDirectory the pack's folder
     * @return the text, or {@link #EMPTY} if the pack has no {@value #FILE}
     * @throws UncheckedIOException     if the file can't be read
     * @throws IllegalArgumentException if the file isn't valid
     */
    public static ContentStrings load(java.nio.file.Path packDirectory) {
        java.nio.file.Path file = packDirectory.resolve(FILE);
        if (!java.nio.file.Files.isRegularFile(file)) {
            return EMPTY;
        }
        try (InputStream in = java.nio.file.Files.newInputStream(file)) {
            return new ObjectMapper().readValue(in, ContentStrings.class);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalArgumentException("invalid " + file + ": " + e.getOriginalMessage(), e);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read " + file, e);
        }
    }

    /**
     * Returns the type of a step, its id without the leading number.
     *
     * @param stepId the step id, such as {@code 08-retrofire}
     * @return the type, such as {@code retrofire}
     */
    public static String stepType(String stepId) {
        return stepId.replaceFirst("^\\d+-", "");
    }

    /**
     * Returns a step's text inside a configuration: the configuration's override for that step if it
     * has one, else the text for the step's type.
     *
     * @param configurationId the configuration, or {@code null}
     * @param stepId          the step id
     * @return the text, or {@code null} if there is none
     */
    public Text step(String configurationId, String stepId) {
        ConfigurationText configuration = configurationId == null ? null : configurations.get(configurationId);
        String type = stepType(stepId);
        if (configuration != null && configuration.steps().containsKey(stepId)) {
            Text override = configuration.steps().get(stepId);
            Text shared = steps.get(type);
            if (shared == null || (override.description() != null && override.name() != null)) {
                return override;
            }
            // An override may rename a step without describing it again, or the other way round.
            return new Text(override.name() != null ? override.name() : shared.name(),
                override.description() != null ? override.description() : shared.description(),
                override.historyNote() != null ? override.historyNote() : shared.historyNote());
        }
        Text text = steps.get(type);
        String[] craft = craftStep(type);
        if (text == null && craft != null) {
            // A joint flight's craft-specific step: the shared step's text, named for its craft.
            Text shared = steps.get(craft[1]);
            if (shared != null) {
                return new Text(craft[0] + shared.name(), shared.description(), shared.historyNote());
            }
        }
        return text;
    }

    /**
     * The step-type prefixes BASPM's data marks one craft of a joint flight with, and how a step is
     * named for it. The full tree adds Gemini's two capsules, the Agena target and the Transtage.
     */
    private static final String[][] CRAFT_PREFIXES = {{"spc-a-", "Craft A: "}, {"spc-b-", "Craft B: "},
        {"sc-a-", "Craft A: "}, {"sc-b-", "Craft B: "}, {"gemini-a-", "Gemini A: "}, {"gemini-b-", "Gemini B: "},
        {"atv-", "Target: "}, {"transtage-", "Transtage: "}};

    /**
     * Splits a craft-specific step type into the label for its craft and the shared step type: by a
     * prefix ({@code gemini-a-launch}) or a suffix ({@code reentry-b}).
     *
     * @param type the step type
     * @return {@code {label, shared type}}, or {@code null} for a step of no particular craft
     */
    public static String[] craftStep(String type) {
        for (String[] prefix : CRAFT_PREFIXES) {
            if (type.startsWith(prefix[0])) {
                return new String[] {prefix[1], type.substring(prefix[0].length())};
            }
        }
        if (type.endsWith("-a") || type.endsWith("-b")) {
            return new String[] {type.endsWith("-a") ? "Craft A: " : "Craft B: ", type.substring(0, type.length() - 2)};
        }
        return null;
    }

    /**
     * One entry's text.
     *
     * @param name        the display name
     * @param description a short description, or {@code null}
     * @param historyNote a short real-world story for the history-note icon, or {@code null}
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Text(String name, String description, String historyNote) {
    }

    /**
     * A mission configuration's text, with optional overrides for single steps.
     *
     * @param name        the display name, relative to the program
     * @param description a short description, or {@code null}
     * @param historyNote a short real-world story, or {@code null}
     * @param steps       step text by full step id, overriding the shared step text
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ConfigurationText(String name, String description, String historyNote, Map<String, Text> steps) {

        /**
         * Replaces missing step overrides with an empty map.
         */
        public ConfigurationText {
            steps = steps == null ? Map.of() : Map.copyOf(steps);
        }
    }
}
