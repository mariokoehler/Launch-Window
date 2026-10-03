package de.mkoehler.launchwindow.content;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * <p>One way of flying a {@link Program} - the third level of the
 * four-level containment hierarchy (see {@code design.md} &sect;2.3(c)).
 * Holds its own step-by-step resolution content - {@link #seasons()} - but
 * nothing about prestige or goals granted, nor the "extended duration"
 * variant's own higher-risk/higher-reward shape (see {@code baspm.md}
 * &sect;10.1); see {@code design.md} &sect;2.5(e) and &sect;2.6
 * (prestige/goals) for what is still deferred. Adding those fields later is
 * not a migration concern for this type, since it isn't persisted anywhere
 * yet (see {@link ContentCatalog}'s own class Javadoc).</p>
 *
 * <p><b>Declares the exact crew/controller headcount
 * {@link de.mkoehler.launchwindow.rules.MissionLaunch} requires</b> (user
 * decision, {@code design.md} &sect;2.5(f), 2026-10-02) - a mission states
 * how many astronauts/controllers it needs, and a launch must supply
 * exactly that many, not "at least one" or "any number." Which skills they
 * need is still expressed by each {@link MissionStep}'s own
 * {@code crewInvolvements}/{@code controllerInvolvements}, unchanged -
 * this is a headcount requirement layered on top of that, not a
 * replacement for it, and does not assign a specific seat to a specific
 * skill.</p>
 *
 * <p><b>{@link #cost()} is a one-time launch cost, charged once per
 * {@link de.mkoehler.launchwindow.rules.MissionLaunch#launch} call</b>
 * (user decision, {@code design.md} &sect;2.5(h), 2026-10-02) - distinct
 * from both the recurring per-season costs this type does not yet model
 * (see {@code design.md} &sect;2.1) and from {@link Program#openingCost()},
 * which prices owning the program itself, not flying one of its missions.
 * {@code 0} is a valid cost (a free mission).</p>
 *
 * <p>A content-definition type: shared, read-only game data loaded from a
 * content pack (not yet implemented - see {@code design.md} &sect;5's M3),
 * never per-player state, and never sent over the network.</p>
 *
 * @param id        a stable identifier, unique within a {@link ContentCatalog}
 * @param programId the {@link Program#id()} this configuration belongs to
 * @param seasons   this configuration's ordered step batches, one per
 *                  season it runs - see {@code baspm.md} &sect;8.1's
 *                  one-file-per-season shape; never empty.
 *                  {@link #durationInSeasons()} is {@code seasons.size()}
 * @param requiredAstronautCount the exact number of astronauts a launch of
 *                  this configuration must supply; must be positive if, and
 *                  only if, some step of some season has a non-empty
 *                  {@code crewInvolvements} - {@code 0} for an uncrewed
 *                  mission
 * @param requiredMissionControlOfficerCount the exact number of Mission
 *                  Control officers a launch of this configuration must
 *                  supply; must be positive if, and only if, some step of
 *                  some season has a non-empty {@code controllerInvolvements}
 * @param cost      the funds cost {@link de.mkoehler.launchwindow.rules.MissionLaunch#launch}
 *                  deducts up front when launching this configuration - a
 *                  one-time launch cost, distinct from
 *                  {@link Program#openingCost()}/{@link Program#maintenanceCost()},
 *                  which price owning the program itself, not flying one of
 *                  its missions
 * @param firstTimePrestige the prestige total this configuration pays when
 *                  flown for the first time; later flights pay a fixed
 *                  fraction of it, by {@link de.mkoehler.launchwindow.rules.MissionPrestige}'s
 *                  own repeat-tier ratios. Each step pays its
 *                  {@link MissionStep#prestigeSharePercent()} share of the
 *                  tier's total. {@code 0} means the configuration pays no
 *                  prestige at all
 * @param goalDependencies reliability penalties while a goal is unmet
 * @param goalBoosts       reliability bonuses once a goal is met
 * @param launches  the launches this configuration flies, one rocket each, chosen at
 *                  launch time - BASPM's {@code LaunchInformation} ({@code baspm.md}
 *                  &sect;10.9). A step names launch {@code n}'s rocket by the slot id
 *                  {@link #rocketSlotId(int) @rocket-n}. Empty for a configuration
 *                  with no rocket choice (its components all come from its program)
 * @author Mario Koehler
 */
public record MissionConfiguration(String id, String programId, List<MissionSeason> seasons,
                                    int requiredAstronautCount, int requiredMissionControlOfficerCount, long cost,
                                    long firstTimePrestige, List<GoalModifier> goalDependencies,
                                    List<GoalModifier> goalBoosts, List<Launch> launches) {

    /** The prefix of a step's rocket slot id; launch {@code n} (1-based) is {@code @rocket-n}. The {@code @} keeps it apart from real component ids. */
    public static final String ROCKET_SLOT_PREFIX = "@rocket-";

    /**
     * One launch: where it goes, and which payloads ride on it. The rocket is the
     * player's choice at launch time, from their owned, active rockets that can lift
     * the summed payload mass to this launch type ({@code baspm.md} &sect;10.9).
     *
     * @param launchType          the destination, and whether the launch is crewed
     * @param payloadComponentIds the payload components this launch carries; never empty
     */
    public record Launch(LaunchType launchType, List<String> payloadComponentIds) {

        /**
         * Validates and copies the launch.
         *
         * @throws NullPointerException     if a field is {@code null}
         * @throws IllegalArgumentException if there are no payloads
         */
        public Launch {
            Objects.requireNonNull(launchType, "launchType must not be null");
            payloadComponentIds = List.copyOf(payloadComponentIds);
            if (payloadComponentIds.isEmpty()) {
                throw new IllegalArgumentException("a launch carries at least one payload");
            }
        }
    }

    /**
     * Returns the slot id a step uses for a launch's rocket.
     *
     * @param launchNumber the 1-based launch number
     * @return {@code @rocket-<launchNumber>}
     */
    public static String rocketSlotId(int launchNumber) {
        return ROCKET_SLOT_PREFIX + launchNumber;
    }

    /**
     * Returns the 0-based launch index a step involvement id names, or {@code -1}
     * when the id is a plain component id rather than a rocket slot.
     *
     * @param involvementId a step's component involvement id
     * @return the launch index, or {@code -1}
     */
    public static int rocketSlotIndex(String involvementId) {
        if (!involvementId.startsWith(ROCKET_SLOT_PREFIX)) {
            return -1;
        }
        try {
            return Integer.parseInt(involvementId.substring(ROCKET_SLOT_PREFIX.length())) - 1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * A configuration with no rocket choice - the shape before rockets were modeled.
     *
     * @param id                                 see {@link MissionConfiguration}
     * @param programId                          see {@link MissionConfiguration}
     * @param seasons                            see {@link MissionConfiguration}
     * @param requiredAstronautCount             see {@link MissionConfiguration}
     * @param requiredMissionControlOfficerCount see {@link MissionConfiguration}
     * @param cost                               see {@link MissionConfiguration}
     * @param firstTimePrestige                  see {@link MissionConfiguration}
     * @param goalDependencies                   see {@link MissionConfiguration}
     * @param goalBoosts                         see {@link MissionConfiguration}
     * @throws NullPointerException     see the canonical constructor
     * @throws IllegalArgumentException see the canonical constructor
     */
    public MissionConfiguration(String id, String programId, List<MissionSeason> seasons,
                                int requiredAstronautCount, int requiredMissionControlOfficerCount, long cost,
                                long firstTimePrestige, List<GoalModifier> goalDependencies,
                                List<GoalModifier> goalBoosts) {
        this(id, programId, seasons, requiredAstronautCount, requiredMissionControlOfficerCount, cost,
            firstTimePrestige, goalDependencies, goalBoosts, List.of());
    }

    /**
     * A goal-linked reliability modifier on one mission component: a
     * dependency subtracts {@code amount} while the goal is still unfulfilled,
     * a boost adds {@code amount} once it is achieved or cancelled - see
     * {@code design.md} &sect;2.6(d) and {@code baspm.md} &sect;11.3.
     *
     * @param goalId      the {@link Goal#id()} the modifier depends on
     * @param componentId the {@link MissionComponent#id()} it changes
     * @param amount      the reliability magnitude, positive
     */
    public record GoalModifier(String goalId, String componentId, int amount) {

        /**
         * Validates every field.
         *
         * @throws NullPointerException     if {@code goalId} or {@code componentId} is {@code null}
         * @throws IllegalArgumentException if either id is blank or {@code amount} is not positive
         */
        public GoalModifier {
            Objects.requireNonNull(goalId, "goalId must not be null");
            Objects.requireNonNull(componentId, "componentId must not be null");
            if (goalId.isBlank() || componentId.isBlank()) {
                throw new IllegalArgumentException("goalModifier ids must not be blank");
            }
            if (amount <= 0) {
                throw new IllegalArgumentException("goalModifier amount must be positive: " + amount);
            }
        }
    }

    /**
     * A configuration with no goal modifiers - the shape before goal
     * modifiers were modeled.
     *
     * @param id                                 see {@link MissionConfiguration}
     * @param programId                          see {@link MissionConfiguration}
     * @param seasons                            see {@link MissionConfiguration}
     * @param requiredAstronautCount             see {@link MissionConfiguration}
     * @param requiredMissionControlOfficerCount see {@link MissionConfiguration}
     * @param cost                               see {@link MissionConfiguration}
     * @param firstTimePrestige                  see {@link MissionConfiguration}
     * @throws NullPointerException     see the canonical constructor
     * @throws IllegalArgumentException see the canonical constructor
     */
    public MissionConfiguration(String id, String programId, List<MissionSeason> seasons,
                                int requiredAstronautCount, int requiredMissionControlOfficerCount, long cost,
                                long firstTimePrestige) {
        this(id, programId, seasons, requiredAstronautCount, requiredMissionControlOfficerCount, cost,
            firstTimePrestige, List.of(), List.of());
    }

    /**
     * A configuration that pays no prestige at all - the shape every
     * configuration had before prestige was modeled, kept so existing
     * content and tests don't all need touching.
     *
     * @param id                                 see {@link MissionConfiguration}
     * @param programId                          see {@link MissionConfiguration}
     * @param seasons                            see {@link MissionConfiguration}
     * @param requiredAstronautCount             see {@link MissionConfiguration}
     * @param requiredMissionControlOfficerCount see {@link MissionConfiguration}
     * @param cost                               see {@link MissionConfiguration}
     * @throws NullPointerException     see the canonical constructor
     * @throws IllegalArgumentException see the canonical constructor
     */
    public MissionConfiguration(String id, String programId, List<MissionSeason> seasons,
                                int requiredAstronautCount, int requiredMissionControlOfficerCount, long cost) {
        this(id, programId, seasons, requiredAstronautCount, requiredMissionControlOfficerCount, cost, 0);
    }

    /**
     * Validates every field (including that both required-count fields
     * agree with whether {@code seasons} actually involves any crew/
     * controllers at all) and defensively copies {@code seasons} into an
     * unmodifiable view.
     *
     * @throws NullPointerException     if {@code id}/{@code programId}/
     *                                   {@code seasons} is {@code null}
     * @throws IllegalArgumentException if {@code id}/{@code programId} is
     *                                   blank, {@code seasons} is empty,
     *                                   either required-count field or
     *                                   {@code cost} is negative, or either
     *                                   required-count field disagrees with
     *                                   whether {@code seasons} actually
     *                                   involves any crew/controllers
     */
    public MissionConfiguration {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(programId, "programId must not be null");
        Objects.requireNonNull(seasons, "seasons must not be null");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (programId.isBlank()) {
            throw new IllegalArgumentException("programId must not be blank");
        }
        if (seasons.isEmpty()) {
            throw new IllegalArgumentException("seasons must not be empty");
        }
        if (requiredAstronautCount < 0) {
            throw new IllegalArgumentException("requiredAstronautCount must not be negative: " + requiredAstronautCount);
        }
        if (requiredMissionControlOfficerCount < 0) {
            throw new IllegalArgumentException(
                "requiredMissionControlOfficerCount must not be negative: " + requiredMissionControlOfficerCount);
        }
        if (cost < 0) {
            throw new IllegalArgumentException("cost must not be negative: " + cost);
        }
        if (firstTimePrestige < 0) {
            throw new IllegalArgumentException("firstTimePrestige must not be negative: " + firstTimePrestige);
        }
        seasons = List.copyOf(seasons);
        Objects.requireNonNull(goalDependencies, "goalDependencies must not be null");
        Objects.requireNonNull(goalBoosts, "goalBoosts must not be null");
        goalDependencies = List.copyOf(goalDependencies);
        goalBoosts = List.copyOf(goalBoosts);
        Objects.requireNonNull(launches, "launches must not be null");
        launches = List.copyOf(launches);

        boolean anyCrewInvolvement = anyStepMatches(seasons, step -> !step.crewInvolvements().isEmpty());
        if ((requiredAstronautCount > 0) != anyCrewInvolvement) {
            throw new IllegalArgumentException(
                "requiredAstronautCount " + requiredAstronautCount + " disagrees with whether any step has crew involvement");
        }
        boolean anyControllerInvolvement = anyStepMatches(seasons, step -> !step.controllerInvolvements().isEmpty());
        if ((requiredMissionControlOfficerCount > 0) != anyControllerInvolvement) {
            throw new IllegalArgumentException("requiredMissionControlOfficerCount "
                + requiredMissionControlOfficerCount + " disagrees with whether any step has controller involvement");
        }
    }

    /**
     * Whether any step of any given season matches the given predicate.
     *
     * @param seasons   the seasons to scan
     * @param predicate the per-step condition to test
     * @return {@code true} if any step matches
     */
    private static boolean anyStepMatches(List<MissionSeason> seasons, Predicate<MissionStep> predicate) {
        for (MissionSeason season : seasons) {
            for (MissionStep step : season.steps()) {
                if (predicate.test(step)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * How many seasons flying this configuration takes - derived from
     * {@link #seasons()} rather than stored separately, so the two can
     * never disagree.
     *
     * @return {@code seasons().size()}
     */
    public int durationInSeasons() {
        return seasons.size();
    }
}
