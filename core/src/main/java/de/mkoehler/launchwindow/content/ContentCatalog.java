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
 * {@link ProgramCategory}, {@link Program}, {@link MissionConfiguration}
 * (together with its own nested {@link MissionSeason}/{@link MissionStep}
 * content), and {@link MissionComponent} a game instance plays with.
 * Validated once at construction, not on every lookup, so a resolver can
 * trust that every id a content definition references actually exists
 * without re-checking.</p>
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
    private final Map<String, Goal> goalsById;
    private final BuildingSchedule buildings;

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
     *                                   present elsewhere in the catalog, a
     *                                   self-referencing discount/transfer
     *                                   entry, or a mission step referencing
     *                                   a {@link MissionComponent} its own
     *                                   {@link Program} does not require
     */
    public ContentCatalog(List<ProgramCategory> categories, List<Program> programs,
            List<MissionConfiguration> missionConfigurations, List<MissionComponent> missionComponents) {
        this(categories, programs, missionConfigurations, missionComponents, List.of());
    }

    /**
     * Builds and validates a catalog that also holds goals.
     *
     * @param categories           every program category
     * @param programs             every program
     * @param missionConfigurations every mission configuration
     * @param missionComponents    every mission component
     * @param goals                every goal a step or modifier can reference
     * @throws NullPointerException     if any argument is {@code null}
     * @throws IllegalArgumentException if any list contains a duplicate id, or
     *                                   any goal, program, step, or modifier
     *                                   references an id the catalog doesn't define
     */
    public ContentCatalog(List<ProgramCategory> categories, List<Program> programs,
            List<MissionConfiguration> missionConfigurations, List<MissionComponent> missionComponents,
            List<Goal> goals) {
        this(categories, programs, missionConfigurations, missionComponents, goals, BuildingSchedule.baspmClose());
    }

    /**
     * Builds and validates a catalog with goals and an explicit building table.
     *
     * @param categories            every program category
     * @param programs              every program
     * @param missionConfigurations every mission configuration
     * @param missionComponents     every mission component
     * @param goals                 every goal a step or modifier can reference
     * @param buildings             the building table
     * @throws NullPointerException     if any argument is {@code null}
     * @throws IllegalArgumentException if any list contains a duplicate id, or
     *                                   any reference doesn't resolve
     */
    public ContentCatalog(List<ProgramCategory> categories, List<Program> programs,
            List<MissionConfiguration> missionConfigurations, List<MissionComponent> missionComponents,
            List<Goal> goals, BuildingSchedule buildings) {
        this.buildings = Objects.requireNonNull(buildings, "buildings must not be null");
        Objects.requireNonNull(categories, "categories must not be null");
        Objects.requireNonNull(programs, "programs must not be null");
        Objects.requireNonNull(missionConfigurations, "missionConfigurations must not be null");
        Objects.requireNonNull(missionComponents, "missionComponents must not be null");
        Objects.requireNonNull(goals, "goals must not be null");

        this.categoriesById = indexById(categories, ProgramCategory::id, "ProgramCategory");
        this.programsById = indexById(programs, Program::id, "Program");
        this.missionConfigurationsById =
            indexById(missionConfigurations, MissionConfiguration::id, "MissionConfiguration");
        this.missionComponentsById = indexById(missionComponents, MissionComponent::id, "MissionComponent");
        this.goalsById = indexById(goals, Goal::id, "Goal");

        validateReferences();
    }

    /**
     * Returns the building table.
     *
     * @return the building levels, caps, costs, and maintenance
     */
    public BuildingSchedule buildings() {
        return buildings;
    }

    /**
     * Looks up a goal by id.
     *
     * @param id the id to look up
     * @return the matching goal, or {@link Optional#empty()} if none exists
     */
    public Optional<Goal> goal(String id) {
        return Optional.ofNullable(goalsById.get(id));
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
                MissionComponent component = missionComponentsById.get(componentId);
                if (component == null) {
                    problems.add("Program " + program.id() + " requires unknown MissionComponent " + componentId);
                } else if (component.hasRocketSpec()) {
                    // Rockets come only from rocket programs, behind the VAB's cap and class limit.
                    problems.add("Program " + program.id() + " requires rocket " + componentId
                        + "; rockets are opened as rocket programs");
                } else if (component.agency() != null && component.agency() != program.agency()) {
                    // A program may only grant components its own agency can own.
                    problems.add("Program " + program.id() + " (agency " + program.agency()
                        + ") requires MissionComponent " + componentId + " of agency " + component.agency());
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
            Program program = programsById.get(configuration.programId());
            if (program == null) {
                problems.add("MissionConfiguration " + configuration.id() + " references unknown Program "
                    + configuration.programId());
                continue;
            }
            validateStepComponentReferences(configuration, program, problems);
            for (MissionConfiguration.Launch launch : configuration.launches()) {
                for (String payloadId : launch.payloadComponentIds()) {
                    if (!program.requiredMissionComponentIds().contains(payloadId)) {
                        problems.add("MissionConfiguration " + configuration.id() + " launches payload " + payloadId
                            + " its program " + program.id() + " doesn't require");
                    }
                }
            }
            validatePrestigeShares(configuration, problems);
            validateGoalReferences(configuration, problems);
        }

        for (Goal goal : goalsById.values()) {
            for (String cancelledId : goal.cancelsGoalIds()) {
                if (!goalsById.containsKey(cancelledId)) {
                    problems.add("Goal " + goal.id() + " cancels unknown Goal " + cancelledId);
                }
            }
        }

        for (Program program : programsById.values()) {
            for (String goalId : program.requiredGoalIds()) {
                if (!goalsById.containsKey(goalId)) {
                    problems.add("Program " + program.id() + " requires unknown Goal " + goalId);
                }
            }
        }

        for (MissionComponent component : missionComponentsById.values()) {
            if (component.id().startsWith(MissionConfiguration.ROCKET_SLOT_PREFIX)) {
                problems.add("MissionComponent " + component.id() + " uses the reserved rocket slot prefix");
            }
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

    /**
     * Checks that every goal a configuration's steps achieve, and every goal
     * modifier it declares, references a goal and mission component the catalog
     * defines.
     *
     * @param configuration the mission configuration to check
     * @param problems      the list to append any problem found to
     */
    private void validateGoalReferences(MissionConfiguration configuration, List<String> problems) {
        String label = "MissionConfiguration " + configuration.id();
        for (MissionSeason season : configuration.seasons()) {
            for (MissionStep step : season.steps()) {
                for (String goalId : step.goalIdsAchieved()) {
                    if (!goalsById.containsKey(goalId)) {
                        problems.add(label + " step " + step.id() + " achieves unknown Goal " + goalId);
                    }
                }
            }
        }
        for (MissionConfiguration.GoalModifier modifier : configuration.goalDependencies()) {
            checkModifier(label, "dependency", modifier, problems);
        }
        for (MissionConfiguration.GoalModifier modifier : configuration.goalBoosts()) {
            checkModifier(label, "boost", modifier, problems);
        }
    }

    private void checkModifier(String label, String kind, MissionConfiguration.GoalModifier modifier,
            List<String> problems) {
        if (!goalsById.containsKey(modifier.goalId())) {
            problems.add(label + " goal " + kind + " references unknown Goal " + modifier.goalId());
        }
        if (!missionComponentsById.containsKey(modifier.componentId())) {
            problems.add(label + " goal " + kind + " references unknown MissionComponent " + modifier.componentId());
        }
    }

    /**
     * Checks that a configuration's step prestige shares are either fully
     * allocated (sum to 100) or not allocated at all (sum to 0), and that a
     * configuration which pays any prestige is fully allocated. See
     * {@code design.md} &sect;2.6(d).
     *
     * @param configuration the mission configuration to check
     * @param problems      the list to append any problem found to
     */
    private void validatePrestigeShares(MissionConfiguration configuration, List<String> problems) {
        int shareSum = configuration.seasons().stream()
            .flatMap(season -> season.steps().stream())
            .mapToInt(MissionStep::prestigeSharePercent)
            .sum();
        String label = "MissionConfiguration " + configuration.id();
        // Either fully allocated or not at all - a partial allocation would silently leave part of the
        // configuration's prestige unpaid, see design.md §2.6(d).
        if (shareSum != 0 && shareSum != 100) {
            problems.add(label + " step prestige shares must sum to 0 or 100, got " + shareSum);
        }
        if (configuration.firstTimePrestige() > 0 && shareSum != 100) {
            problems.add(label + " pays prestige but its step shares sum to " + shareSum + ", not 100");
        }
    }

    /**
     * Checks that every step of {@code configuration} only involves mission
     * components that both exist in this catalog and are actually required
     * by {@code program} - a mission configuration cannot use a component
     * the player was never granted, see {@code design.md} &sect;2.5(e).
     *
     * @param configuration the mission configuration to check
     * @param program       the program {@code configuration} belongs to
     * @param problems      the list to append any problem found to
     */
    private void validateStepComponentReferences(MissionConfiguration configuration, Program program,
            List<String> problems) {
        List<MissionSeason> seasons = configuration.seasons();
        for (int seasonIndex = 0; seasonIndex < seasons.size(); seasonIndex++) {
            for (MissionStep step : seasons.get(seasonIndex).steps()) {
                String stepLabel = "MissionConfiguration " + configuration.id() + " season " + seasonIndex
                    + " step " + step.id();
                for (MissionStep.ComponentInvolvement involvement : step.componentInvolvements()) {
                    String componentId = involvement.componentId();
                    if (componentId.startsWith(MissionConfiguration.ROCKET_SLOT_PREFIX)) {
                        // A rocket slot: the player's rocket choice for that launch, not a program component.
                        int launchIndex = MissionConfiguration.rocketSlotIndex(componentId);
                        if (launchIndex < 0 || launchIndex >= configuration.launches().size()) {
                            problems.add(stepLabel + " names rocket slot " + componentId + " but the configuration has "
                                + configuration.launches().size() + " launch(es)");
                        }
                    } else if (!missionComponentsById.containsKey(componentId)) {
                        problems.add(stepLabel + " references unknown MissionComponent " + componentId);
                    } else if (!program.requiredMissionComponentIds().contains(componentId)) {
                        problems.add(stepLabel + " references MissionComponent " + componentId
                            + " not required by its own Program " + program.id());
                    }
                }
            }
        }
    }
}
