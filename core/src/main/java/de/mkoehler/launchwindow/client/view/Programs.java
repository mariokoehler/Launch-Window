package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.content.CelestialBody;
import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.content.MissionSeason;
import de.mkoehler.launchwindow.content.MissionStep;
import de.mkoehler.launchwindow.content.Program;
import de.mkoehler.launchwindow.content.ProgramCategory;
import de.mkoehler.launchwindow.model.Building;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.rules.BuildingUpgrades;
import de.mkoehler.launchwindow.rules.ProgramOpening;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * What Headquarters shows about programs ({@code ui-spec.md} &sect;4.6.1-&sect;4.6.4): each
 * program's state, the price breakdown, missions in flight, the next goal, and the per-category and
 * per-body counts. Plain logic over {@code core}'s own rules (gap G7), no libGDX.
 *
 * <p>Only the player's own agency's programs exist here: another agency's programs are never shown
 * or counted ({@code design.md} &sect;5, M3 decision 5).</p>
 *
 * @author Mario Koehler
 */
public final class Programs {

    /**
     * A program's state for this player.
     *
     * <p>The mockup also has "Closed &middot; components kept". The state can't tell that apart: a
     * program's components can be owned through another program (Vostok's capsule through
     * Korabl-Sputnik 2) without the program ever having been open. So a closed program reads
     * Available, and the price breakdown's "already owned &middot; Free" lines say what the
     * mockup's label said (2026-10-04, flagged to the user).</p>
     */
    public enum State {
        /** Open now. */
        OPEN,
        /** Can be opened (its goals are met). */
        AVAILABLE,
        /** A required goal isn't achieved or cancelled yet. */
        LOCKED
    }

    /**
     * One line of a price breakdown ({@code ui-spec.md} component 4).
     *
     * @param label  what it is
     * @param amount its effect on the total (negative for a discount)
     * @param text   shown instead of an amount ("Free", "None"), or {@code null}
     * @param upkeep what it adds to the upkeep every season once bought, or {@code null} where that
     *               doesn't apply (a discount, a price that isn't a program's)
     */
    public record PriceLine(String label, long amount, String text, Long upkeep) {

        /**
         * A line with no upkeep of its own.
         *
         * @param label  what it is
         * @param amount its effect on the total (negative for a discount)
         * @param text   shown instead of an amount, or {@code null}
         */
        public PriceLine(String label, long amount, String text) {
            this(label, amount, text, null);
        }
    }

    /**
     * A price breakdown whose lines add up to the rule's own price ({@link ProgramOpening#openingPrice}).
     *
     * @param lines the lines
     * @param total the total
     */
    public record Price(List<PriceLine> lines, long total) {
    }

    /**
     * A category's counts, for the category list.
     *
     * @param open      programs open
     * @param available programs that could be opened
     * @param locked    programs still locked behind goals
     */
    public record Counts(int open, int available, int locked) {

        /**
         * Words the counts, leaving out zeros.
         *
         * @return for example "1 open · 2 available"
         */
        public String text() {
            List<String> parts = new ArrayList<>();
            if (open > 0) {
                parts.add(open + " open");
            }
            if (available > 0) {
                parts.add(available + " available");
            }
            if (locked > 0) {
                parts.add(locked + " locked");
            }
            return parts.isEmpty() ? "No programs" : String.join(" · ", parts);
        }
    }

    /**
     * Not instantiable; this class only exposes static methods.
     */
    private Programs() {
    }

    /**
     * Returns the player's agency's programs, in the pack's order.
     *
     * @param state   the player's state
     * @param catalog the content pack
     * @return the programs
     */
    public static List<Program> visible(PlayerSnapshot state, ContentCatalog catalog) {
        return catalog.programs().stream().filter(program -> program.availableTo(state.getArchetype())).toList();
    }

    /**
     * Returns the player's open programs, in the order they were opened.
     *
     * @param state   the player's state
     * @param catalog the content pack
     * @return the programs
     */
    public static List<Program> open(PlayerSnapshot state, ContentCatalog catalog) {
        return state.getOpenedProgramIds().stream().map(id -> catalog.program(id).orElseThrow()).toList();
    }

    /**
     * Returns a program's state for this player.
     *
     * @param program the program
     * @param state   the player's state
     * @return the state
     */
    public static State state(Program program, PlayerSnapshot state) {
        if (state.getOpenedProgramIds().contains(program.id())) {
            return State.OPEN;
        }
        return missingGoals(program, state).isEmpty() ? State.AVAILABLE : State.LOCKED;
    }

    /**
     * Returns the required goals not yet achieved or cancelled.
     *
     * @param program the program
     * @param state   the player's state
     * @return the goal ids, in the program's order
     */
    public static List<String> missingGoals(Program program, PlayerSnapshot state) {
        return program.requiredGoalIds().stream().filter(goal -> !state.hasAchievedOrCancelledGoal(goal)).toList();
    }

    /**
     * Breaks a program's price down: its opening cost, each discount from another open program
     * (capped at 100% together, as the rule caps it), and each component's creation cost, or
     * "Free" if it's already owned. Each line but a discount carries the upkeep it adds, as
     * {@link Outlook#openingUpkeep} counts it: a component already active through another open
     * program adds none.
     *
     * @param program the program
     * @param state   the player's state
     * @param catalog the content pack
     * @return the breakdown
     */
    public static Price price(Program program, PlayerSnapshot state, ContentCatalog catalog) {
        List<PriceLine> lines = new ArrayList<>();
        double multiplier = state.getCostMultiplier();
        lines.add(new PriceLine("Opening cost", program.openingCost(), null,
            Math.round(program.maintenanceCost() * multiplier)));
        List<Program.OpeningCostDiscount> applicable = program.openingCostDiscounts().stream()
            .filter(discount -> state.getOpenedProgramIds().contains(discount.otherProgramId())).toList();
        long totalDiscount = program.openingCost()
            - ProgramOpening.effectiveOpeningCost(program, state.getOpenedProgramIds());
        if (applicable.isEmpty()) {
            lines.add(new PriceLine("Discount from other open programs", 0, "None"));
        } else {
            long given = 0;
            for (int i = 0; i < applicable.size(); i++) {
                Program.OpeningCostDiscount discount = applicable.get(i);
                // The last line takes up rounding and the 100% cap, so the lines always sum to the rule's price.
                long amount = i == applicable.size() - 1 ? totalDiscount - given
                    : Math.min(totalDiscount - given, program.openingCost() * discount.percent() / 100);
                given += amount;
                lines.add(new PriceLine("Discount: " + ContentNames.of(discount.otherProgramId()) + " is open (−"
                    + discount.percent() + "%)", -amount, null));
            }
        }
        for (String componentId : program.requiredMissionComponentIds()) {
            String name = ContentNames.of(componentId);
            MissionComponent component = catalog.missionComponent(componentId).orElseThrow();
            OwnedMissionComponent owned = state.getMissionComponents().get(componentId);
            long upkeep = owned != null && owned.isActive() ? 0 : Math.round(component.maintenanceCost() * multiplier);
            if (owned != null) {
                lines.add(new PriceLine(name + " (already owned)", 0, "Free", upkeep));
            } else {
                lines.add(new PriceLine(name + " (creation cost)", component.creationCost(), null, upkeep));
            }
        }
        return new Price(lines, ProgramOpening.openingPrice(state, program, catalog));
    }

    /**
     * Returns how many of the player's missions in flight belong to a program (Close is refused
     * while any do).
     *
     * @param program the program
     * @param state   the player's state
     * @param catalog the content pack
     * @return the count
     */
    public static int missionsInFlight(Program program, PlayerSnapshot state, ContentCatalog catalog) {
        return (int) state.getInProgressMissions().values().stream()
            .map(mission -> catalog.missionConfiguration(mission.getMissionConfigurationId()).orElseThrow())
            .filter(configuration -> configuration.programId().equals(program.id()))
            .count();
    }

    /**
     * Returns the first goal a program's missions can achieve that the player hasn't achieved or
     * cancelled yet.
     *
     * @param program the program
     * @param state   the player's state
     * @param catalog the content pack
     * @return the goal id, or empty if none is left
     */
    public static Optional<String> nextGoal(Program program, PlayerSnapshot state, ContentCatalog catalog) {
        for (MissionConfiguration configuration : catalog.missionConfigurations()) {
            if (!configuration.programId().equals(program.id())) {
                continue;
            }
            for (MissionSeason season : configuration.seasons()) {
                for (MissionStep step : season.steps()) {
                    for (String goal : step.goalIdsAchieved()) {
                        if (!state.hasAchievedOrCancelledGoal(goal)) {
                            return Optional.of(goal);
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Returns the player's programs in a category, in the pack's order.
     *
     * @param category the category
     * @param state    the player's state
     * @param catalog  the content pack
     * @return the programs
     */
    public static List<Program> inCategory(ProgramCategory category, PlayerSnapshot state, ContentCatalog catalog) {
        return visible(state, catalog).stream().filter(program -> program.categoryId().equals(category.id())).toList();
    }

    /**
     * Returns the categories under a body that hold at least one of the player's programs.
     *
     * @param body    the body
     * @param state   the player's state
     * @param catalog the content pack
     * @return the categories, in the pack's order
     */
    public static List<ProgramCategory> categories(CelestialBody body, PlayerSnapshot state, ContentCatalog catalog) {
        return catalog.categories().stream()
            .filter(category -> category.body() == body && !inCategory(category, state, catalog).isEmpty())
            .toList();
    }

    /**
     * Counts a list of programs by state.
     *
     * @param programs the programs
     * @param state    the player's state
     * @return the counts
     */
    public static Counts counts(List<Program> programs, PlayerSnapshot state) {
        int open = 0;
        int available = 0;
        int locked = 0;
        for (Program program : programs) {
            switch (state(program, state)) {
                case OPEN -> open++;
                case AVAILABLE -> available++;
                case LOCKED -> locked++;
            }
        }
        return new Counts(open, available, locked);
    }

    /**
     * Counts the player's programs under a body.
     *
     * @param body    the body
     * @param state   the player's state
     * @param catalog the content pack
     * @return the counts; all zero if the body holds none of the player's programs
     */
    public static Counts bodyCounts(CelestialBody body, PlayerSnapshot state, ContentCatalog catalog) {
        List<Program> programs = new ArrayList<>();
        for (ProgramCategory category : categories(body, state, catalog)) {
            programs.addAll(inCategory(category, state, catalog));
        }
        return counts(programs, state);
    }

    /**
     * Returns Headquarters' open-program cap, the same one the opening rule checks.
     *
     * @param state   the player's state
     * @param catalog the content pack
     * @return the cap
     */
    public static int cap(PlayerSnapshot state, ContentCatalog catalog) {
        return BuildingUpgrades.capacity(state, Building.HEADQUARTERS, catalog.buildings());
    }
}
