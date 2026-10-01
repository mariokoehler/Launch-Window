package de.mkoehler.launchwindow.content;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * <p>An immutable, validated set of content definitions - every
 * {@link ProgramCategory}, {@link Program}, {@link MissionConfiguration},
 * and {@link MissionComponent} a game instance plays with. Validated once
 * at construction, not on every lookup, so a resolver can trust that every
 * id a content definition references actually exists without re-checking.</p>
 *
 * <p>Built directly from in-memory fixtures for now (see {@code design.md}
 * &sect;5's M2 status - "headless rules... against an in-memory/test-
 * fixture content set"); a real content-pack loader reading these same
 * shapes from files does not exist yet - see {@code design.md} &sect;5's
 * M3 status.</p>
 *
 * @author Mario Koehler
 */
public final class ContentCatalog {

    private final Map<String, ProgramCategory> categoriesById;
    private final Map<String, Program> programsById;
    private final Map<String, MissionConfiguration> missionConfigurationsById;
    private final Map<String, MissionComponent> missionComponentsById;

    /**
     * Builds and validates a catalog from the given content definitions.
     *
     * @param categories            every program category
     * @param programs              every program
     * @param missionConfigurations every mission configuration
     * @param missionComponents     every mission component
     * @throws NullPointerException     if any argument is {@code null}
     * @throws IllegalArgumentException if any list contains a duplicate id,
     *                                   a dangling reference to an id not
     *                                   present elsewhere in the catalog, or
     *                                   a self-referencing discount/transfer
     *                                   entry
     */
    public ContentCatalog(List<ProgramCategory> categories, List<Program> programs,
            List<MissionConfiguration> missionConfigurations, List<MissionComponent> missionComponents) {
        Objects.requireNonNull(categories, "categories must not be null");
        Objects.requireNonNull(programs, "programs must not be null");
        Objects.requireNonNull(missionConfigurations, "missionConfigurations must not be null");
        Objects.requireNonNull(missionComponents, "missionComponents must not be null");

        this.categoriesById = indexById(categories, ProgramCategory::id, "ProgramCategory");
        this.programsById = indexById(programs, Program::id, "Program");
        this.missionConfigurationsById =
            indexById(missionConfigurations, MissionConfiguration::id, "MissionConfiguration");
        this.missionComponentsById = indexById(missionComponents, MissionComponent::id, "MissionComponent");

        validateReferences();
    }

    /**
     * Looks up a program category by id.
     *
     * @param id the id to look up
     * @return the matching category, or {@link Optional#empty()} if none
     *     exists
     */
    public Optional<ProgramCategory> category(String id) {
        return Optional.ofNullable(categoriesById.get(id));
    }

    /**
     * Looks up a program by id.
     *
     * @param id the id to look up
     * @return the matching program, or {@link Optional#empty()} if none
     *     exists
     */
    public Optional<Program> program(String id) {
        return Optional.ofNullable(programsById.get(id));
    }

    /**
     * Looks up a mission configuration by id.
     *
     * @param id the id to look up
     * @return the matching configuration, or {@link Optional#empty()} if
     *     none exists
     */
    public Optional<MissionConfiguration> missionConfiguration(String id) {
        return Optional.ofNullable(missionConfigurationsById.get(id));
    }

    /**
     * Looks up a mission component by id.
     *
     * @param id the id to look up
     * @return the matching component, or {@link Optional#empty()} if none
     *     exists
     */
    public Optional<MissionComponent> missionComponent(String id) {
        return Optional.ofNullable(missionComponentsById.get(id));
    }

    /**
     * Indexes a list of content definitions by their own id, rejecting a
     * duplicate.
     *
     * @param items    the definitions to index
     * @param idOf     how to read one definition's id
     * @param typeName the type name to report in a duplicate-id exception
     * @param <T>      the definition type
     * @return an unmodifiable map from id to definition, in list order
     * @throws IllegalArgumentException if two definitions share an id
     */
    private static <T> Map<String, T> indexById(List<T> items, Function<T, String> idOf,
            String typeName) {
        Map<String, T> byId = new LinkedHashMap<>();
        for (T item : items) {
            String id = idOf.apply(item);
            if (byId.putIfAbsent(id, item) != null) {
                throw new IllegalArgumentException("duplicate " + typeName + " id: " + id);
            }
        }
        // Collections.unmodifiableMap, not Map.copyOf - Map.copyOf does not preserve insertion
        // order, which would silently break this method's own "in list order" guarantee and any
        // later caller (e.g. a bot harness) relying on deterministic iteration for reproducible
        // results across runs.
        return Collections.unmodifiableMap(byId);
    }

    /**
     * Checks every cross-reference between content definitions already
     * indexed by {@link #indexById}, collecting every problem found rather
     * than failing on the first one, so a malformed fixture/content pack
     * reports its full set of mistakes in one pass.
     *
     * @throws IllegalArgumentException if any reference is dangling or
     *                                   self-referencing
     */
    private void validateReferences() {
        List<String> problems = new ArrayList<>();

        for (Program program : programsById.values()) {
            if (!categoriesById.containsKey(program.categoryId())) {
                problems.add("Program " + program.id() + " references unknown categoryId " + program.categoryId());
            }
            for (String componentId : program.requiredMissionComponentIds()) {
                if (!missionComponentsById.containsKey(componentId)) {
                    problems.add("Program " + program.id() + " requires unknown MissionComponent " + componentId);
                }
            }
            for (Program.OpeningCostDiscount discount : program.openingCostDiscounts()) {
                if (discount.otherProgramId().equals(program.id())) {
                    problems.add("Program " + program.id() + " has a discount referencing itself");
                } else if (!programsById.containsKey(discount.otherProgramId())) {
                    problems.add("Program " + program.id() + " has a discount referencing unknown Program "
                        + discount.otherProgramId());
                }
            }
        }

        for (MissionConfiguration configuration : missionConfigurationsById.values()) {
            if (!programsById.containsKey(configuration.programId())) {
                problems.add("MissionConfiguration " + configuration.id() + " references unknown Program "
                    + configuration.programId());
            }
        }

        for (MissionComponent component : missionComponentsById.values()) {
            for (MissionComponent.ReliabilityTransferSource source : component.reliabilityTransferSources()) {
                if (source.sourceComponentId().equals(component.id())) {
                    problems.add("MissionComponent " + component.id() + " has a transfer source referencing itself");
                } else if (!missionComponentsById.containsKey(source.sourceComponentId())) {
                    problems.add("MissionComponent " + component.id()
                        + " has a transfer source referencing unknown MissionComponent "
                        + source.sourceComponentId());
                }
            }
        }

        if (!problems.isEmpty()) {
            throw new IllegalArgumentException("invalid content catalog: " + String.join("; ", problems));
        }
    }
}
