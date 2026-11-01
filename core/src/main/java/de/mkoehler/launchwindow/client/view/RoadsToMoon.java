package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.content.MissionSeason;
import de.mkoehler.launchwindow.content.MissionStep;
import de.mkoehler.launchwindow.content.Program;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.rules.GoalCascade;
import de.mkoehler.launchwindow.rules.ProgramOpening;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The roads to the Moon as one agency sees them now ({@code ui-spec.md} &sect;4.21): for each road of
 * {@link MoonRoads} open to its archetype, what it costs to start (the opening lines as the rule prices them,
 * the parts to build once however many programs need them, the upkeep), the hardware it needs, the rocket that
 * lifts its landing, the crew, the flights and Firsts on the way, and the landing itself. Every figure comes
 * from the rules and the pack (G7); the words come from {@link MoonRoads}.
 *
 * @author Mario Koehler
 */
public final class RoadsToMoon {

    /** The goal a road's landing earns. */
    public static final String LANDING_GOAL = "crewed-lunar-landing";

    /** The smallest rocket first, then the cheapest launch, as {@link Outlook#leastRocket} picks. */
    private static final Comparator<MissionComponent> SIZE = Comparator
        .comparing((MissionComponent rocket) -> rocket.rocket().rocketClass())
        .thenComparingLong(MissionComponent::unitCost);

    private RoadsToMoon() {
    }

    /** What an opening line of "To start now" says. */
    public enum Cost {
        /** The program is open already. */
        OPEN,
        /** Free now: an open program's discount covers it ({@link Opening#freeBecause()}). */
        FREE,
        /** Free once the road's own Earth-orbit tests are open. */
        FREE_AFTER_TESTS,
        /** It costs {@link Opening#amount()}. */
        PRICED
    }

    /**
     * One opening line: the Earth-orbit tests or the Moon flights.
     *
     * @param cost        what it says
     * @param amount      the opening cost now, after discounts (0 unless {@link Cost#PRICED})
     * @param freeBecause the open program whose discount makes it free, for {@link Cost#FREE}, else {@code null}
     */
    public record Opening(Cost cost, long amount, String freeBecause) {
    }

    /**
     * A part the road needs.
     *
     * @param component   the component
     * @param owned       whether the agency has it already
     * @param reliability its reliability now, when owned
     */
    public record Part(MissionComponent component, boolean owned, double reliability) {
    }

    /** The state of the rocket a road's landing needs. */
    public enum RocketState {
        /** Its program is open. */
        OPEN,
        /** It can be opened. */
        NOT_OPEN,
        /** The VAB is too small for its class. */
        VAB_TOO_SMALL
    }

    /**
     * The rocket a road's landing needs: the largest of the smallest rockets each of its launches needs.
     *
     * @param rocket       the rocket
     * @param state        where it stands
     * @param reliability  its reliability now, when open
     * @param vabLevel     the VAB level its class needs
     * @param launches     how many launches each landing takes
     * @param fromTheFirst whether the road's first Earth-orbit test already needs a rocket of its class (the
     *                     LK-700's needs the N1, Gemini direct ascent's the Saturn V)
     */
    public record Rocket(MissionComponent rocket, RocketState state, double reliability, int vabLevel, int launches,
                         boolean fromTheFirst) {
    }

    /**
     * A First on the way: a goal one of the road's flights before the landing earns.
     *
     * @param goalId  the goal
     * @param reached whether the agency holds it
     */
    public record First(String goalId, boolean reached) {
    }

    /**
     * One road as the agency sees it now.
     *
     * @param words        its words
     * @param moon         the Moon program
     * @param earthOrbit   its Earth-orbit tests
     * @param yours        whether the agency has opened either
     * @param missingGoals the goals still needed before its Earth-orbit tests can open
     * @param tests        the Earth-orbit tests' opening line
     * @param flights      the Moon flights' opening line
     * @param partsCost    the creation cost of the parts not owned yet
     * @param total        what it costs to start now: both opening lines and the parts
     * @param upkeep       its upkeep a season once both programs are open: theirs and their parts'
     * @param parts        its parts, the tests' first
     * @param rocket       the rocket its landing needs, or {@code null} if none of the agency's can
     * @param crew         the crew seats of its landing
     * @param flightsOnWay how many of its configurations come before the landing
     * @param firsts       the Firsts on the way, in {@link MoonRoads#firsts()} order
     * @param landing      the landing
     */
    public record Road(MoonRoads.Road words, Program moon, Program earthOrbit, boolean yours, List<String> missingGoals,
                       Opening tests, Opening flights, long partsCost, long total, long upkeep, List<Part> parts,
                       Rocket rocket, int crew, int flightsOnWay, List<First> firsts, MissionConfiguration landing) {

        /**
         * Returns how many parts are still to develop.
         *
         * @return the count
         */
        public long toDevelop() {
            return parts.stream().filter(part -> !part.owned()).count();
        }

        /**
         * Returns the landing's steps.
         *
         * @return how many
         */
        public int landingSteps() {
            return landing.seasons().stream().mapToInt(season -> season.steps().size()).sum();
        }

        /**
         * Returns how many of the landing's steps put the crew at risk.
         *
         * @return how many
         */
        public int landingCrewRiskSteps() {
            return (int) landing.seasons().stream().flatMap(season -> season.steps().stream())
                .filter(MissionStep::crewRisk).count();
        }

        /**
         * Returns the program the screen's button leads to: the Earth-orbit tests until they're open.
         *
         * @param state the agency
         * @return the program
         */
        public Program next(PlayerSnapshot state) {
            return state.getOpenedProgramIds().contains(earthOrbit.id()) ? moon : earthOrbit;
        }
    }

    /**
     * A road that reaches the Moon without landing.
     *
     * @param words        its words
     * @param moon         the Moon program
     * @param earthOrbit   its Earth-orbit tests
     * @param missingGoals the goals still needed before its Earth-orbit tests can open
     * @param total        what it costs to start now
     * @param firsts       the Firsts it can take, in {@link MoonRoads#firsts()} order
     */
    public record NoLanding(MoonRoads.WithoutLanding words, Program moon, Program earthOrbit, List<String> missingGoals,
                        long total, List<First> firsts) {
    }

    /**
     * Returns the agency's roads that land, in the file's order.
     *
     * @param roads   the words
     * @param state   the agency
     * @param catalog the content pack
     * @return the roads
     */
    public static List<Road> roads(MoonRoads roads, PlayerSnapshot state, ContentCatalog catalog) {
        List<Road> out = new ArrayList<>();
        for (MoonRoads.Road words : roads.roads()) {
            Program moon = catalog.program(words.program()).orElse(null);
            Program earthOrbit = catalog.program(words.earthOrbit()).orElse(null);
            if (moon == null || earthOrbit == null || !moon.availableTo(state.getArchetype())) {
                continue;
            }
            MissionConfiguration landing = landing(moon, catalog);
            if (landing == null) {
                continue;
            }
            List<String> opened = state.getOpenedProgramIds();
            boolean yours = opened.contains(moon.id()) || opened.contains(earthOrbit.id());
            Opening tests = opening(earthOrbit, opened, null, catalog);
            Opening flights = opening(moon, opened, earthOrbit.id(), catalog);
            List<Part> parts = parts(List.of(earthOrbit, moon), state, catalog);
            long partsCost = parts.stream().filter(part -> !part.owned())
                .mapToLong(part -> part.component().creationCost()).sum();
            List<MissionConfiguration> before = new ArrayList<>();
            for (Program program : List.of(earthOrbit, moon)) {
                Missions.of(program, catalog).stream().filter(configuration -> !earns(configuration, LANDING_GOAL,
                    catalog)).forEach(before::add);
            }
            out.add(new Road(words, moon, earthOrbit, yours, Programs.missingGoals(earthOrbit, state), tests, flights,
                partsCost, tests.amount() + flights.amount() + partsCost, upkeep(List.of(earthOrbit, moon), parts,
                state), parts, rocket(landing, earthOrbit, state, catalog), Missions.crewCount(landing), before.size(),
                firsts(roads.firsts(), before, state, catalog), landing));
        }
        return out;
    }

    /**
     * Returns the agency's roads that reach the Moon without landing, in the file's order.
     *
     * @param roads   the words
     * @param state   the agency
     * @param catalog the content pack
     * @return the roads
     */
    public static List<NoLanding> withoutLanding(MoonRoads roads, PlayerSnapshot state, ContentCatalog catalog) {
        List<NoLanding> out = new ArrayList<>();
        for (MoonRoads.WithoutLanding words : roads.withoutLanding()) {
            Program moon = catalog.program(words.program()).orElse(null);
            Program earthOrbit = catalog.program(words.earthOrbit()).orElse(null);
            if (moon == null || earthOrbit == null || !moon.availableTo(state.getArchetype())) {
                continue;
            }
            List<String> opened = state.getOpenedProgramIds();
            long open = opening(earthOrbit, opened, null, catalog).amount() + opening(moon, opened, earthOrbit.id(),
                catalog).amount();
            long partsCost = parts(List.of(earthOrbit, moon), state, catalog).stream().filter(part -> !part.owned())
                .mapToLong(part -> part.component().creationCost()).sum();
            List<MissionConfiguration> flights = new ArrayList<>();
            for (Program program : List.of(earthOrbit, moon)) {
                flights.addAll(Missions.of(program, catalog));
            }
            out.add(new NoLanding(words, moon, earthOrbit, Programs.missingGoals(earthOrbit, state), open + partsCost,
                firsts(roads.firsts(), flights, state, catalog)));
        }
        return out;
    }

    /**
     * Returns the word the crew row uses for the agency's crews.
     *
     * @param archetype the agency's archetype
     * @param count     how many
     * @return "astronauts", "cosmonaut" and so on
     */
    public static String crewWord(Archetype archetype, int count) {
        String word = archetype == Archetype.SOVIET ? "cosmonaut" : "astronaut";
        return count == 1 ? word : word + "s";
    }

    /** The first configuration of a Moon program whose flight earns the landing. */
    static MissionConfiguration landing(Program moon, ContentCatalog catalog) {
        return Missions.of(moon, catalog).stream().filter(configuration -> earns(configuration, LANDING_GOAL, catalog))
            .findFirst().orElse(null);
    }

    /** Whether a configuration's flight earns a goal, itself or through the goals it credits. */
    private static boolean earns(MissionConfiguration configuration, String goalId, ContentCatalog catalog) {
        return goals(configuration, catalog).contains(goalId);
    }

    /** Every goal a configuration's flight earns, with the goals they credit. */
    private static Set<String> goals(MissionConfiguration configuration, ContentCatalog catalog) {
        Set<String> goals = new LinkedHashSet<>();
        for (MissionSeason season : configuration.seasons()) {
            for (MissionStep step : season.steps()) {
                for (String goal : step.goalIdsAchieved()) {
                    goals.addAll(GoalCascade.unheld(goal, held -> false, catalog));
                }
            }
        }
        return goals;
    }

    /**
     * An opening line, priced as the rule prices it ({@link ProgramOpening#effectiveOpeningCost}): open, free now
     * through an open program, free once the road's tests are open, or its cost now.
     */
    private static Opening opening(Program program, List<String> opened, String tests, ContentCatalog catalog) {
        if (opened.contains(program.id())) {
            return new Opening(Cost.OPEN, 0, null);
        }
        long now = ProgramOpening.effectiveOpeningCost(program, opened);
        if (now == 0) {
            String because = program.openingCostDiscounts().stream().map(Program.OpeningCostDiscount::otherProgramId)
                .filter(opened::contains).findFirst().orElse(null);
            return new Opening(Cost.FREE, 0, because);
        }
        if (tests != null) {
            List<String> withTests = new ArrayList<>(opened);
            withTests.add(tests);
            if (ProgramOpening.effectiveOpeningCost(program, withTests) == 0) {
                return new Opening(Cost.FREE_AFTER_TESTS, 0, null);
            }
        }
        return new Opening(Cost.PRICED, now, null);
    }

    /** The programs' parts, each once, in the order the programs list them. */
    private static List<Part> parts(List<Program> programs, PlayerSnapshot state, ContentCatalog catalog) {
        Set<String> ids = new LinkedHashSet<>();
        programs.forEach(program -> ids.addAll(program.requiredMissionComponentIds()));
        List<Part> parts = new ArrayList<>();
        for (String id : ids) {
            OwnedMissionComponent owned = state.getMissionComponents().get(id);
            parts.add(new Part(catalog.missionComponent(id).orElseThrow(), owned != null,
                owned == null ? 0 : owned.getCurrentReliability()));
        }
        return parts;
    }

    /** Both programs' upkeep and their parts', each part once, at the agency's cost multiplier. */
    private static long upkeep(List<Program> programs, List<Part> parts, PlayerSnapshot state) {
        double sum = programs.stream().mapToLong(Program::maintenanceCost).sum()
            + parts.stream().mapToLong(part -> part.component().maintenanceCost()).sum();
        return Math.round(sum * state.getCostMultiplier());
    }

    /** The rocket the landing needs: per launch the smallest of the agency's that lifts it, then the largest. */
    private static Rocket rocket(MissionConfiguration landing, Program earthOrbit, PlayerSnapshot state,
                                 ContentCatalog catalog) {
        MissionComponent needed = null;
        int launches = 0;
        for (MissionConfiguration.Launch launch : landing.launches()) {
            if (!launch.launchType().needsRocket()) {
                continue;
            }
            launches++;
            MissionComponent least = Outlook.leastRocket(launch, state, catalog);
            if (least == null) {
                return null;
            }
            if (needed == null || SIZE.compare(least, needed) > 0) {
                needed = least;
            }
        }
        if (needed == null) {
            return null;
        }
        MissionComponent rocket = needed;
        boolean fromTheFirst = Missions.of(earthOrbit, catalog).stream().findFirst()
            .map(first -> first.launches().stream().filter(launch -> launch.launchType().needsRocket())
                .map(launch -> Outlook.leastRocket(launch, state, catalog))
                .anyMatch(least -> least != null && least.rocket().rocketClass() == rocket.rocket().rocketClass()))
            .orElse(false);
        OwnedMissionComponent owned = state.getMissionComponents().get(rocket.id());
        boolean open = Rockets.open(state, catalog).stream().anyMatch(open1 -> open1.id().equals(rocket.id()));
        RocketState where = open ? RocketState.OPEN : Rockets.classAllowed(rocket, state) ? RocketState.NOT_OPEN
            : RocketState.VAB_TOO_SMALL;
        return new Rocket(rocket, where, owned == null ? 0 : owned.getCurrentReliability(),
            rocket.rocket().rocketClass().ordinal() + 1, launches, fromTheFirst);
    }

    /** The goals of the firsts list the flights earn, in the list's order. */
    private static List<First> firsts(List<String> firsts, List<MissionConfiguration> flights, PlayerSnapshot state,
                                      ContentCatalog catalog) {
        Set<String> earned = new LinkedHashSet<>();
        flights.forEach(configuration -> earned.addAll(goals(configuration, catalog)));
        return firsts.stream().filter(earned::contains)
            .map(goal -> new First(goal, state.hasAchievedOrCancelledGoal(goal))).toList();
    }
}
