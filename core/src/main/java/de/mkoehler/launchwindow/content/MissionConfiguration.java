package de.mkoehler.launchwindow.content;

import de.mkoehler.launchwindow.model.AstronautSkill;
import de.mkoehler.launchwindow.model.MissionControlSkill;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * <p>One way of flying a {@link Program} - the third level of the four-level
 * containment hierarchy ({@code design.md} &sect;2.3(c)) - with its step-by-step
 * resolution content ({@link #seasons()}).</p>
 *
 * <p><b>Staffing is by slot, as in BASPM</b> ({@code baspm.md} &sect;10.11, user
 * decision 2026-10-03). The configuration carries its mission-control layout as
 * {@link #controlSlots()} - titled seats, each with an employee type and a skill -
 * and its flight-crew layout as {@link #crewSlots()} (titles). A launch fills each
 * slot the steps use with one person. Layout slots no step uses stay empty, as
 * BASPM's "Not required" slots do. The required head-counts
 * ({@link #requiredAstronautCount()} and friends) are derived from the slots the
 * steps use across every season. As in BASPM, a control slot is released after the
 * last season that uses it ({@link #usedControlSlotsFrom(int)}, user decision
 * 2026-10-03); the crew stays aboard to the end.</p>
 *
 * <p>{@link #cost()} is a one-time launch cost for the payloads; each chosen
 * rocket's unit cost is added at launch ({@code design.md} &sect;2.5(h), &sect;5 M3).</p>
 *
 * <p>A content-definition type: shared, read-only game data loaded from a content
 * pack, never per-player state, and never sent over the network.</p>
 *
 * @param id                 a stable identifier, unique within a {@link ContentCatalog}
 * @param programId          the {@link Program#id()} this configuration belongs to
 * @param seasons            the ordered step batches, one per season; never empty
 * @param crewSlots          the flight-crew layout's slot titles; empty for an uncrewed
 *                           mission. A crew slot holds one astronaut aboard
 * @param controlSlots       the mission-control layout: flight-controller slots and
 *                           ground astronaut slots such as CAPCOM
 * @param cost               the payloads' launch cost
 * @param firstTimePrestige  the prestige total of a first flight; later flights pay
 *                           {@link de.mkoehler.launchwindow.rules.MissionPrestige}'s
 *                           repeat-tier share of it
 * @param goalDependencies   reliability penalties while a goal is unmet
 * @param goalBoosts         reliability bonuses once a goal is met
 * @param launches           the launches, one chosen rocket each ({@code baspm.md}
 *                           &sect;10.9); a step names launch {@code n}'s rocket by
 *                           {@link #rocketSlotId(int) @rocket-n}
 * @param cancellationPrestigeDeduction the prestige an uncrewed mission loses when
 *                           cancelled in flight ({@code baspm.md} &sect;10.10)
 * @param cancellationRefundPercent the share of the launch cost an in-flight
 *                           cancellation restores, 0-100
 * @param animalPassengers   the animals the flight carries, remembered on the memorial when
 *                           they're lost; {@code null} for none (2026-10-05)
 * @param requiredGoalIds    goals the player must have achieved or cancelled before this
 *                           configuration can fly, a hard gate (BASPM's
 *                           {@code StrictGoalDependency}, 2026-10-08): the X-15's research flights
 *                           after its flight test, say. Missing means none
 * @author Mario Koehler
 */
public record MissionConfiguration(String id, String programId, List<MissionSeason> seasons,
                                    List<String> crewSlots, List<StaffingSlot> controlSlots, long cost,
                                    long firstTimePrestige, List<GoalModifier> goalDependencies,
                                    List<GoalModifier> goalBoosts, List<Launch> launches,
                                    long cancellationPrestigeDeduction, int cancellationRefundPercent,
                                    AnimalPassengers animalPassengers, List<String> requiredGoalIds) {

    /**
     * The form without a goal gate, as every configuration had before the full tree.
     *
     * @param id                            the id
     * @param programId                     the program
     * @param seasons                       the seasons
     * @param crewSlots                     the flight-crew layout
     * @param controlSlots                  the mission-control layout
     * @param cost                          the launch cost
     * @param firstTimePrestige             the first flight's prestige
     * @param goalDependencies              reliability penalties
     * @param goalBoosts                    reliability bonuses
     * @param launches                      the launches
     * @param cancellationPrestigeDeduction the prestige lost on an in-flight cancellation
     * @param cancellationRefundPercent     the refund on an in-flight cancellation
     * @param animalPassengers              the animals aboard, or {@code null}
     */
    public MissionConfiguration(String id, String programId, List<MissionSeason> seasons, List<String> crewSlots,
                                List<StaffingSlot> controlSlots, long cost, long firstTimePrestige,
                                List<GoalModifier> goalDependencies, List<GoalModifier> goalBoosts,
                                List<Launch> launches, long cancellationPrestigeDeduction,
                                int cancellationRefundPercent, AnimalPassengers animalPassengers) {
        this(id, programId, seasons, crewSlots, controlSlots, cost, firstTimePrestige, goalDependencies, goalBoosts,
            launches, cancellationPrestigeDeduction, cancellationRefundPercent, animalPassengers, List.of());
    }

    /**
     * The form without animal passengers, as every configuration had before they existed.
     *
     * @param id                            the id
     * @param programId                     the program
     * @param seasons                       the seasons
     * @param crewSlots                     the flight-crew layout
     * @param controlSlots                  the mission-control layout
     * @param cost                          the launch cost
     * @param firstTimePrestige             the first flight's prestige
     * @param goalDependencies              reliability penalties
     * @param goalBoosts                    reliability bonuses
     * @param launches                      the launches
     * @param cancellationPrestigeDeduction the prestige lost on an in-flight cancellation
     * @param cancellationRefundPercent     the refund on an in-flight cancellation
     */
    public MissionConfiguration(String id, String programId, List<MissionSeason> seasons, List<String> crewSlots,
                                List<StaffingSlot> controlSlots, long cost, long firstTimePrestige,
                                List<GoalModifier> goalDependencies, List<GoalModifier> goalBoosts,
                                List<Launch> launches, long cancellationPrestigeDeduction,
                                int cancellationRefundPercent) {
        this(id, programId, seasons, crewSlots, controlSlots, cost, firstTimePrestige, goalDependencies, goalBoosts,
            launches, cancellationPrestigeDeduction, cancellationRefundPercent, (AnimalPassengers) null);
    }

    /**
     * The animals a flight carries (user decision, 2026-10-05). They're remembered on the
     * player's memorial when the flight costs their lives: always at its end if they don't come
     * home, otherwise only if the flight fails. The animals that really flew and came home are
     * honoured in the Museum instead, one entry per successful flight in order (user decision
     * 2026-10-06), so a flight can honour Belka and Strelka while a failure remembers the dogs
     * really lost on another attempt.
     *
     * @param animals    the animals remembered if the flight costs their lives, as on the first
     *                   flight; may be empty only when the flight comes home and honours someone
     * @param returnHome whether the flight is meant to bring them back
     * @param unnamed    the memorial name for an animal on a later flight, once the named
     *                   animal is already remembered (a second "Laika" would be wrong)
     * @param cameHome   for each successful flight in turn, the animals honoured for coming home
     *                   on it; a flight past the end of the list honours nobody. Missing means none.
     * @param unnamedLostOnFailure whether a failed flight with no named animal to remember
     *                   remembers one animal by the unnamed form, such as a failed uncrewed Mercury
     *                   test's chimpanzee (user decision 2026-10-06). Missing means no.
     */
    public record AnimalPassengers(List<Animal> animals, boolean returnHome, String unnamed,
                                   List<List<Animal>> cameHome, Boolean unnamedLostOnFailure) {

        /**
         * Validates and copies.
         */
        public AnimalPassengers {
            Objects.requireNonNull(unnamed, "unnamed must not be null");
            animals = List.copyOf(animals);
            cameHome = cameHome == null ? List.of() : cameHome.stream().map(List::copyOf).toList();
            unnamedLostOnFailure = Boolean.TRUE.equals(unnamedLostOnFailure);
            if (animals.isEmpty() && !unnamedLostOnFailure && (!returnHome || cameHome.isEmpty())) {
                throw new IllegalArgumentException("animalPassengers needs an animal to remember or to honour");
            }
        }

        /**
         * Animals carried and honoured per successful flight, with nobody unnamed lost on a failure.
         *
         * @param animals    the animals remembered if the flight costs their lives
         * @param returnHome whether the flight is meant to bring them back
         * @param unnamed    the memorial name for an animal on a later flight
         * @param cameHome   for each successful flight in turn, the animals honoured for coming home
         */
        public AnimalPassengers(List<Animal> animals, boolean returnHome, String unnamed,
                                List<List<Animal>> cameHome) {
            this(animals, returnHome, unnamed, cameHome, false);
        }

        /**
         * Animals carried with nobody to honour for coming home.
         *
         * @param animals    the animals remembered if the flight costs their lives
         * @param returnHome whether the flight is meant to bring them back
         * @param unnamed    the memorial name for an animal on a later flight
         */
        public AnimalPassengers(List<Animal> animals, boolean returnHome, String unnamed) {
            this(animals, returnHome, unnamed, List.of());
        }

        /**
         * Returns the animals honoured after a number of successful flights, in flight order.
         *
         * @param completedFlights the configuration's successful flights so far
         * @return the honoured animals, possibly none
         */
        public List<Animal> honouredAfter(int completedFlights) {
            return cameHome.stream().limit(Math.max(0, completedFlights)).flatMap(List::stream).toList();
        }
    }

    /**
     * One animal passenger.
     *
     * @param name       its name, as the memorial shows it
     * @param portraitId its photo under {@code assets/ui/portraits/}, or {@code null}
     */
    public record Animal(String name, String portraitId) {

        /**
         * Checks the name.
         */
        public Animal {
            Objects.requireNonNull(name, "name must not be null");
        }
    }

    /** The prefix of a step's rocket slot id; launch {@code n} (1-based) is {@code @rocket-n}. */
    public static final String ROCKET_SLOT_PREFIX = "@rocket-";

    /**
     * A goal-linked reliability modifier on one mission component: a dependency
     * subtracts {@code amount} while the goal is still unfulfilled, a boost adds it once
     * the goal is achieved or cancelled ({@code design.md} &sect;2.6(d)).
     *
     * @param goalId      the {@link Goal#id()} the modifier depends on
     * @param componentId the {@link MissionComponent#id()} it changes
     * @param amount      the reliability magnitude, positive
     */
    public record GoalModifier(String goalId, String componentId, int amount) {

        /**
         * Validates every field.
         *
         * @throws NullPointerException     if an id is {@code null}
         * @throws IllegalArgumentException if an id is blank or {@code amount} is not positive
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
     * One launch: where it goes, and which payloads ride on it. The rocket is the
     * player's choice at launch time ({@code baspm.md} &sect;10.9), except on an
     * {@link LaunchType#AIR_DROP}, which has none.
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
     * Compatibility form for fixtures written before slots: {@code astronauts} crew
     * slots titled {@code CREW n}, then {@code controllers} flight-controller slots
     * titled {@code CONTROLLER n} whose skills cycle through {@link MissionControlSkill}
     * in declaration order (slot 0 Propulsion, slot 1 Trajectory and GNC, ...), then
     * {@code capcom} astronaut slots titled {@code CAPCOM n} reading Leadership.
     *
     * @param id                            see {@link MissionConfiguration}
     * @param programId                     see {@link MissionConfiguration}
     * @param seasons                       see {@link MissionConfiguration}
     * @param astronauts                    the number of crew slots
     * @param controllers                   the number of flight-controller slots
     * @param cost                          see {@link MissionConfiguration}
     * @param firstTimePrestige             see {@link MissionConfiguration}
     * @param goalDependencies              see {@link MissionConfiguration}
     * @param goalBoosts                    see {@link MissionConfiguration}
     * @param launches                      see {@link MissionConfiguration}
     * @param capcom                        the number of CAPCOM slots
     * @param cancellationPrestigeDeduction see {@link MissionConfiguration}
     * @param cancellationRefundPercent     see {@link MissionConfiguration}
     * @throws NullPointerException     see the canonical constructor
     * @throws IllegalArgumentException see the canonical constructor
     */
    public MissionConfiguration(String id, String programId, List<MissionSeason> seasons, int astronauts,
                                int controllers, long cost, long firstTimePrestige,
                                List<GoalModifier> goalDependencies, List<GoalModifier> goalBoosts,
                                List<Launch> launches, int capcom, long cancellationPrestigeDeduction,
                                int cancellationRefundPercent) {
        this(id, programId, seasons, genericCrewSlots(astronauts), genericControlSlots(controllers, capcom), cost,
            firstTimePrestige, goalDependencies, goalBoosts, launches, cancellationPrestigeDeduction,
            cancellationRefundPercent);
    }

    /**
     * Compatibility form with generic slots and no cancellation terms.
     *
     * @param id                see {@link MissionConfiguration}
     * @param programId         see {@link MissionConfiguration}
     * @param seasons           see {@link MissionConfiguration}
     * @param astronauts        the number of crew slots
     * @param controllers       the number of flight-controller slots
     * @param cost              see {@link MissionConfiguration}
     * @param firstTimePrestige see {@link MissionConfiguration}
     * @param goalDependencies  see {@link MissionConfiguration}
     * @param goalBoosts        see {@link MissionConfiguration}
     * @param launches          see {@link MissionConfiguration}
     * @param capcom            the number of CAPCOM slots
     */
    public MissionConfiguration(String id, String programId, List<MissionSeason> seasons, int astronauts,
                                int controllers, long cost, long firstTimePrestige,
                                List<GoalModifier> goalDependencies, List<GoalModifier> goalBoosts,
                                List<Launch> launches, int capcom) {
        this(id, programId, seasons, astronauts, controllers, cost, firstTimePrestige, goalDependencies, goalBoosts,
            launches, capcom, 0, 0);
    }

    /**
     * Compatibility form with generic slots, no CAPCOM, and no cancellation terms.
     *
     * @param id                see {@link MissionConfiguration}
     * @param programId         see {@link MissionConfiguration}
     * @param seasons           see {@link MissionConfiguration}
     * @param astronauts        the number of crew slots
     * @param controllers       the number of flight-controller slots
     * @param cost              see {@link MissionConfiguration}
     * @param firstTimePrestige see {@link MissionConfiguration}
     * @param goalDependencies  see {@link MissionConfiguration}
     * @param goalBoosts        see {@link MissionConfiguration}
     * @param launches          see {@link MissionConfiguration}
     */
    public MissionConfiguration(String id, String programId, List<MissionSeason> seasons, int astronauts,
                                int controllers, long cost, long firstTimePrestige,
                                List<GoalModifier> goalDependencies, List<GoalModifier> goalBoosts,
                                List<Launch> launches) {
        this(id, programId, seasons, astronauts, controllers, cost, firstTimePrestige, goalDependencies, goalBoosts,
            launches, 0);
    }

    /**
     * Compatibility form with generic slots and no launches.
     *
     * @param id                see {@link MissionConfiguration}
     * @param programId         see {@link MissionConfiguration}
     * @param seasons           see {@link MissionConfiguration}
     * @param astronauts        the number of crew slots
     * @param controllers       the number of flight-controller slots
     * @param cost              see {@link MissionConfiguration}
     * @param firstTimePrestige see {@link MissionConfiguration}
     * @param goalDependencies  see {@link MissionConfiguration}
     * @param goalBoosts        see {@link MissionConfiguration}
     */
    public MissionConfiguration(String id, String programId, List<MissionSeason> seasons, int astronauts,
                                int controllers, long cost, long firstTimePrestige,
                                List<GoalModifier> goalDependencies, List<GoalModifier> goalBoosts) {
        this(id, programId, seasons, astronauts, controllers, cost, firstTimePrestige, goalDependencies, goalBoosts,
            List.of());
    }

    /**
     * Compatibility form with generic slots and no goal modifiers.
     *
     * @param id                see {@link MissionConfiguration}
     * @param programId         see {@link MissionConfiguration}
     * @param seasons           see {@link MissionConfiguration}
     * @param astronauts        the number of crew slots
     * @param controllers       the number of flight-controller slots
     * @param cost              see {@link MissionConfiguration}
     * @param firstTimePrestige see {@link MissionConfiguration}
     */
    public MissionConfiguration(String id, String programId, List<MissionSeason> seasons, int astronauts,
                                int controllers, long cost, long firstTimePrestige) {
        this(id, programId, seasons, astronauts, controllers, cost, firstTimePrestige, List.of(), List.of());
    }

    /**
     * Compatibility form with generic slots and no prestige.
     *
     * @param id          see {@link MissionConfiguration}
     * @param programId   see {@link MissionConfiguration}
     * @param seasons     see {@link MissionConfiguration}
     * @param astronauts  the number of crew slots
     * @param controllers the number of flight-controller slots
     * @param cost        see {@link MissionConfiguration}
     */
    public MissionConfiguration(String id, String programId, List<MissionSeason> seasons, int astronauts,
                                int controllers, long cost) {
        this(id, programId, seasons, astronauts, controllers, cost, 0);
    }

    /**
     * Validates every field and copies the lists.
     *
     * @throws NullPointerException     if a reference field is {@code null}
     * @throws IllegalArgumentException if an id is blank, {@code seasons} is empty, a
     *                                   value is negative or out of range, or a step names a
     *                                   crew or control slot the layouts don't have
     */
    public MissionConfiguration {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(programId, "programId must not be null");
        Objects.requireNonNull(seasons, "seasons must not be null");
        Objects.requireNonNull(crewSlots, "crewSlots must not be null");
        Objects.requireNonNull(controlSlots, "controlSlots must not be null");
        Objects.requireNonNull(goalDependencies, "goalDependencies must not be null");
        Objects.requireNonNull(goalBoosts, "goalBoosts must not be null");
        Objects.requireNonNull(launches, "launches must not be null");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (programId.isBlank()) {
            throw new IllegalArgumentException("programId must not be blank");
        }
        if (seasons.isEmpty()) {
            throw new IllegalArgumentException("seasons must not be empty");
        }
        if (cost < 0) {
            throw new IllegalArgumentException("cost must not be negative: " + cost);
        }
        if (firstTimePrestige < 0) {
            throw new IllegalArgumentException("firstTimePrestige must not be negative: " + firstTimePrestige);
        }
        if (cancellationPrestigeDeduction < 0 || cancellationRefundPercent < 0 || cancellationRefundPercent > 100) {
            throw new IllegalArgumentException("cancellation terms out of range for " + id);
        }
        seasons = List.copyOf(seasons);
        crewSlots = List.copyOf(crewSlots);
        controlSlots = List.copyOf(controlSlots);
        goalDependencies = List.copyOf(goalDependencies);
        goalBoosts = List.copyOf(goalBoosts);
        launches = List.copyOf(launches);
        requiredGoalIds = requiredGoalIds == null ? List.of() : List.copyOf(requiredGoalIds);
        for (MissionSeason season : seasons) {
            for (MissionStep step : season.steps()) {
                for (MissionStep.ControllerInvolvement involvement : step.controllerInvolvements()) {
                    if (involvement.slot() >= controlSlots.size()) {
                        throw new IllegalArgumentException(id + " step " + step.id() + " names control slot "
                            + involvement.slot() + " of " + controlSlots.size());
                    }
                }
                for (MissionStep.CrewInvolvement involvement : step.crewInvolvements()) {
                    if (involvement.slot() >= crewSlots.size()) {
                        throw new IllegalArgumentException(id + " step " + step.id() + " names crew slot "
                            + involvement.slot() + " of " + crewSlots.size());
                    }
                }
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
     * Returns the crew slots some step of some season involves - the seats a launch
     * must fill aboard.
     *
     * @return the slot indices, ascending
     */
    public SortedSet<Integer> usedCrewSlots() {
        SortedSet<Integer> used = new TreeSet<>();
        for (MissionSeason season : seasons) {
            for (MissionStep step : season.steps()) {
                step.crewInvolvements().forEach(involvement -> used.add(involvement.slot()));
            }
        }
        return used;
    }

    /**
     * Returns the control slots some step of some season involves - the seats a launch
     * must fill on the ground.
     *
     * @return the slot indices, ascending
     */
    public SortedSet<Integer> usedControlSlots() {
        return usedControlSlotsFrom(0);
    }

    /**
     * Returns the control slots some step of season {@code seasonIndex} or later
     * involves - the seats a mission still needs from that season on. A slot outside
     * this set is released once its last season has flown ({@code baspm.md} &sect;10.11).
     *
     * @param seasonIndex the 0-based season index
     * @return the slot indices, ascending; empty past the last season
     */
    public SortedSet<Integer> usedControlSlotsFrom(int seasonIndex) {
        SortedSet<Integer> used = new TreeSet<>();
        for (int i = Math.max(0, seasonIndex); i < seasons.size(); i++) {
            for (MissionStep step : seasons.get(i).steps()) {
                step.controllerInvolvements().forEach(involvement -> used.add(involvement.slot()));
            }
        }
        return used;
    }

    /**
     * Returns how many astronauts fly: the used crew slots.
     *
     * @return the crew count
     */
    public int requiredAstronautCount() {
        return usedCrewSlots().size();
    }

    /**
     * Returns how many flight controllers the mission needs: the used mission-control slots.
     *
     * @return the controller count
     */
    public int requiredMissionControlOfficerCount() {
        return (int) usedControlSlots().stream()
            .filter(slot -> controlSlots.get(slot).kind() == StaffingSlot.Kind.MISSION_CONTROL).count();
    }

    /**
     * Returns how many astronauts the mission needs on the ground (CAPCOM): the used
     * astronaut-type control slots.
     *
     * @return the CAPCOM count
     */
    public int requiredCapcomCount() {
        return (int) usedControlSlots().stream()
            .filter(slot -> controlSlots.get(slot).kind() == StaffingSlot.Kind.ASTRONAUT).count();
    }

    /**
     * How many seasons flying this configuration takes.
     *
     * @return {@code seasons().size()}
     */
    public int durationInSeasons() {
        return seasons.size();
    }

    private static List<String> genericCrewSlots(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("crew slot count must not be negative: " + count);
        }
        List<String> slots = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            slots.add("CREW " + i);
        }
        return slots;
    }

    private static List<StaffingSlot> genericControlSlots(int controllers, int capcom) {
        if (controllers < 0 || capcom < 0) {
            throw new IllegalArgumentException("slot counts must not be negative");
        }
        List<StaffingSlot> slots = new ArrayList<>();
        MissionControlSkill[] skills = MissionControlSkill.values();
        for (int i = 0; i < controllers; i++) {
            slots.add(StaffingSlot.controller("CONTROLLER " + (i + 1), skills[i % skills.length]));
        }
        for (int i = 0; i < capcom; i++) {
            slots.add(StaffingSlot.astronaut("CAPCOM " + (i + 1), AstronautSkill.LEADERSHIP));
        }
        return slots;
    }
}
