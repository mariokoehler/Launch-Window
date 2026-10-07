package de.mkoehler.launchwindow.client.view;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.content.ContentStrings;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.content.Program;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * The concepts behind the game's content ({@code ui-spec.md} component 19, {@code design.md}
 * &sect;1.1): real-world ideas such as "Suborbital" or "Retrofire", each pointing at the explainer
 * sheet that explains it and listing the programs, configurations, steps, components and goals it
 * belongs to. The screens show them as concept chips. They live in
 * {@code assets/content/concepts.json}, outside the frozen content pack, like the explainers.
 *
 * @param concepts every concept, in chip order
 * @author Mario Koehler
 */
@JsonIgnoreProperties({"_comment"})
public record Concepts(List<Concept> concepts) {

    /** The concepts file, under the assets' {@code content/} folder. */
    public static final String FILE = "concepts.json";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static volatile Concepts standard;

    /**
     * Copies the list.
     */
    public Concepts {
        concepts = concepts == null ? List.of() : List.copyOf(concepts);
    }

    /**
     * One concept.
     *
     * @param id             a stable id
     * @param label          the chip's text
     * @param explainer      the explainer that explains it, by id, or {@code null} if none is written yet
     * @param sheet          the sheet to open, from 1
     * @param programs       the programs it belongs to
     * @param configurations the mission configurations it belongs to
     * @param steps          the step types it belongs to (a step id without its season number or a joint
     *                       flight's craft prefix)
     * @param components     the components it belongs to
     * @param goals          the goals it belongs to
     */
    public record Concept(String id, String label, String explainer, int sheet, List<String> programs,
                          List<String> configurations, List<String> steps, List<String> components,
                          List<String> goals) {

        /**
         * Checks the id and copies the lists; missing ones read as empty.
         */
        public Concept {
            Objects.requireNonNull(id, "id must not be null");
            programs = programs == null ? List.of() : List.copyOf(programs);
            configurations = configurations == null ? List.of() : List.copyOf(configurations);
            steps = steps == null ? List.of() : List.copyOf(steps);
            components = components == null ? List.of() : List.copyOf(components);
            goals = goals == null ? List.of() : List.copyOf(goals);
        }

        /**
         * Returns whether the concept has an explainer, so it shows as a chip.
         *
         * @return whether it has one
         */
        public boolean shown() {
            return explainer != null;
        }
    }

    /**
     * Returns the concepts bundled with the game, read once from the classpath.
     *
     * @return the concepts, or none if the file isn't there
     */
    public static Concepts standard() {
        Concepts loaded = standard;
        if (loaded == null) {
            try (InputStream in = Concepts.class.getClassLoader().getResourceAsStream("content/" + FILE)) {
                loaded = in == null ? new Concepts(List.of()) : MAPPER.readValue(in, Concepts.class);
            } catch (IOException e) {
                throw new UncheckedIOException("Could not read the concepts", e);
            }
            standard = loaded;
        }
        return loaded;
    }

    /**
     * Reads concepts from JSON text.
     *
     * @param json the text
     * @return the concepts
     */
    public static Concepts parse(String json) {
        try {
            return MAPPER.readValue(json, Concepts.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the concepts", e);
        }
    }

    /**
     * Returns the type of a step inside a configuration: its id without the season number, and
     * without the craft prefix of a joint flight's craft-specific step.
     *
     * @param stepId the step id, such as {@code 08-spc-a-retrofire}
     * @return the type, such as {@code retrofire}
     */
    public static String stepType(String stepId) {
        String type = ContentStrings.stepType(stepId);
        return type.startsWith("spc-a-") || type.startsWith("spc-b-") ? type.substring(6) : type;
    }

    /**
     * Returns the shown concepts of a program: its own and those of the components it grants (a
     * capsule has no screen of its own), each once, in chip order.
     *
     * @param program the program
     * @return the concepts
     */
    public List<Concept> forProgram(Program program) {
        return shown(concept -> concept.programs().contains(program.id())
            || concept.components().stream().anyMatch(program.requiredMissionComponentIds()::contains));
    }

    /**
     * Returns the shown concepts of a mission configuration: its own and those of its steps, each
     * once, in chip order.
     *
     * @param configuration the configuration
     * @return the concepts
     */
    public List<Concept> forConfiguration(MissionConfiguration configuration) {
        Set<String> types = new LinkedHashSet<>();
        configuration.seasons().forEach(season -> season.steps().forEach(step -> types.add(stepType(step.id()))));
        return shown(concept -> concept.configurations().contains(configuration.id())
            || concept.steps().stream().anyMatch(types::contains));
    }

    /**
     * Returns the shown concepts of a component, such as a rocket, in chip order.
     *
     * @param componentId the component
     * @return the concepts
     */
    public List<Concept> forComponent(String componentId) {
        return shown(concept -> concept.components().contains(componentId));
    }

    /**
     * Returns a concept by id.
     *
     * @param id the id
     * @return the concept, or empty
     */
    public Optional<Concept> byId(String id) {
        return concepts.stream().filter(concept -> concept.id().equals(id)).findFirst();
    }

    private List<Concept> shown(java.util.function.Predicate<Concept> belongs) {
        return concepts.stream().filter(Concept::shown).filter(belongs).toList();
    }
}
