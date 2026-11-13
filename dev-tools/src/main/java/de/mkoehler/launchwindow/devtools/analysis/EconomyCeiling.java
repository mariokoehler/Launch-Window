package de.mkoehler.launchwindow.devtools.analysis;

import de.mkoehler.launchwindow.content.BudgetSchedule;
import de.mkoehler.launchwindow.content.BuildingSchedule;
import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.ContentPackLoader;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.content.Program;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.Building;
import de.mkoehler.launchwindow.rules.BudgetProgress;
import de.mkoehler.launchwindow.rules.HiringPool;
import de.mkoehler.launchwindow.rules.Intelligence;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

/**
 * <p>The economy's ceiling, from the rules alone: the most an agency can spend each season just to keep running
 * (every building at its top level, every roster full, the dearest programs open, every component owned) against
 * what each funding review pays. Where even the ceiling sits below the funding, money can't be what holds a player
 * back, however they play. No simulation: the playouts' per-season ledger ({@link ContentPackPlayouts}'
 * {@code economy} report) shows what agencies really spend.</p>
 *
 * <p>Run: {@code mvn -pl dev-tools compile exec:java
 * -DmainClass=de.mkoehler.launchwindow.devtools.analysis.EconomyCeiling [-Dexec.args=standard]}.</p>
 *
 * @author Mario Koehler
 */
public final class EconomyCeiling {

    /** Salaries per head to price a full roster at: the hiring floor, the middle, the ceiling, and it raised 50 %. */
    private static final double[] SALARIES = {HiringPool.SALARY_MIN, (HiringPool.SALARY_MIN + HiringPool.SALARY_MAX) / 2.0,
        HiringPool.SALARY_MAX, HiringPool.SALARY_MAX * 1.5};

    private EconomyCeiling() {
    }

    /**
     * Prints the report.
     *
     * @param args the pack, {@code standard} by default
     */
    public static void main(String[] args) {
        String pack = args.length > 0 ? args[0] : "standard";
        ContentCatalog catalog = ContentPackLoader.load(Path.of("assets", "content", pack));
        BudgetSchedule budget = BudgetTrial.schedule();
        System.out.println("Pack: " + pack + ", " + BudgetTrial.label() + ". Running costs per season, then "
            + "funding per review (one every "
            + BudgetProgress.SEASONS_PER_REVIEW + " seasons).");

        BuildingSchedule buildings = catalog.buildings();
        long buildingsMax = 0;
        int staffMax = 0;
        for (Building building : Building.values()) {
            BuildingSchedule.Levels levels = buildings.of(building);
            if (levels == null) {
                continue;
            }
            buildingsMax += levels.maintenance().get(levels.maintenance().size() - 1);
            if (building == Building.ASTRONAUT_CENTER || building == Building.MISSION_CONTROL
                    || building == Building.SET_CENTER) {
                staffMax += levels.caps().get(levels.caps().size() - 1);
            }
        }
        int programSlots = top(buildings, Building.HEADQUARTERS);
        int rocketSlots = top(buildings, Building.VEHICLE_ASSEMBLY);
        long intelligence = Intelligence.cost(Intelligence.MAX_LEVEL);

        for (Archetype side : Archetype.values()) {
            List<Program> programs = catalog.programs().stream()
                .filter(p -> p.agency() == null || p.agency() == side).toList();
            java.util.Map<String, Long> componentCost = new java.util.HashMap<>();
            catalog.missionComponents().forEach(c -> componentCost.put(c.id(), c.maintenanceCost()));
            List<Long> rockets = catalog.missionComponents().stream()
                .filter(c -> (c.agency() == null || c.agency() == side) && c.rocket() != null)
                .map(MissionComponent::maintenanceCost).sorted(Comparator.reverseOrder()).toList();
            long dearestRockets = rockets.stream().limit(rocketSlots).mapToLong(Long::longValue).sum();
            long medianRocket = rockets.isEmpty() ? 0 : rockets.get(rockets.size() / 2);
            // The dearest programs with the components they keep active (a component pays only while a program uses it).
            long dearest = greedy(programs, programSlots, componentCost);
            // A program's own cost with its components, alone, for the median.
            List<Long> alone = programs.stream().map(p -> p.maintenanceCost() + p.requiredMissionComponentIds().stream()
                .filter(id -> !isRocket(catalog, id)).mapToLong(id -> componentCost.getOrDefault(id, 0L)).sum())
                .sorted().toList();
            long median = alone.isEmpty() ? 0 : alone.get(alone.size() / 2);

            System.out.println();
            System.out.println("== " + side + " ==");
            System.out.printf("Buildings, all at their top level: %,d. Intelligence at its top level: %,d. Staff: %d at "
                + "most.%n", buildingsMax, intelligence, staffMax);
            System.out.printf("Programs (with the payload components they keep active): %d open at most; the %d "
                + "dearest %,d; a median one %,d (of %d programs)%n", programSlots, programSlots, dearest, median,
                programs.size());
            System.out.printf("Rockets: %d at most; the %d dearest %,d; a median one %,d%n", rocketSlots, rocketSlots,
                dearestRockets, medianRocket);
            long base = buildingsMax + intelligence;
            long ceiling = base + dearest + dearestRockets + Math.round(staffMax * SALARIES[3]);
            long full = base + 16 * median + 6 * medianRocket + Math.round(staffMax * SALARIES[1]);
            long playTest = base + 4 * median + 3 * medianRocket + Math.round(staffMax * SALARIES[1]);
            System.out.printf("  A. everything maxed: the dearest programs and rockets, salaries %.0f: %,d%n",
                SALARIES[3], ceiling);
            System.out.printf("  B. buildings and staff maxed, 16 median programs, 6 median rockets, salaries %.1f: "
                + "%,d%n", SALARIES[1], full);
            System.out.printf("  C. buildings and staff maxed, 4 median programs, 3 median rockets, salaries %.1f "
                + "(a finished race, like the play test): %,d%n", SALARIES[1], playTest);
            // A reasonable player (user, 2026-11-13): staff for the biggest mission plus spares, buildings only as
            // high as that staff and the open programs and rockets need, a mid intelligence service.
            long reasonable = reasonable(buildings, REASONABLE_STAFF, 4, 3) + Intelligence.cost(2)
                + 4 * median + 3 * medianRocket + Math.round(reasonableHeads() * SALARIES[1]);
            long reasonableBusy = reasonable(buildings, REASONABLE_STAFF, 7, 4) + Intelligence.cost(2)
                + 7 * median + 4 * medianRocket + Math.round(reasonableHeads() * SALARIES[1]);
            System.out.printf("  D. a reasonable player: 7 astronauts, 15 controllers, 16 engineers at %.1f, "
                + "buildings just big enough, intelligence level 2, 4 median programs, 3 median rockets: %,d%n",
                SALARIES[1], reasonable);
            System.out.printf("     buildings: %s%n", reasonableLevels(buildings, REASONABLE_STAFF, 4, 3));
            System.out.printf("  E. as D, busier: 7 median programs, 4 median rockets: %,d (buildings: %s)%n",
                reasonableBusy, reasonableLevels(buildings, REASONABLE_STAFF, 7, 4));

            System.out.println();
            System.out.println("  Funding per review at half, three quarters and all of its prestige range, what a "
                + "player at the top keeps over D and E, and the prestige at which it passes each scenario:");
            System.out.println("  review  seasons   bracket prestige     at 50%   at 75%  at top   top-D   top-E    "
                + "A / B / C / D / E");
            for (int review = 1; review <= budget.reviews().size(); review++) {
                BudgetSchedule.Review bracket = budget.reviewFor(review);
                long top = at(review, bracket, 1.0);
                System.out.printf("  %6d  %3d-%3d  %,8d..%,-8d  %,7d  %,7d  %,7d  %,6d  %,6d    %s / %s / %s / %s / %s%n",
                    review, review * BudgetProgress.SEASONS_PER_REVIEW,
                    (review + 1) * BudgetProgress.SEASONS_PER_REVIEW - 1, bracket.minPrestige(), bracket.maxPrestige(),
                    at(review, bracket, 0.5), at(review, bracket, 0.75), top, top - reasonable, top - reasonableBusy,
                    breakEven(review, bracket, ceiling), breakEven(review, bracket, full),
                    breakEven(review, bracket, playTest), breakEven(review, bracket, reasonable),
                    breakEven(review, bracket, reasonableBusy));
            }
        }
        System.out.println();
        System.out.println("Not counted: launches, R&D, hiring bonuses, upgrades and contracts, which a player chooses"
            + " to spend on; reviews ignore a catch-up shift (a shortfall at the last review moves the bracket down).");
    }

    private static boolean isRocket(ContentCatalog catalog, String id) {
        return catalog.missionComponent(id).map(c -> c.rocket() != null).orElse(false);
    }

    /** The dearest set of programs, picked one at a time by what each adds with the components it activates. */
    private static long greedy(List<Program> programs, int slots, java.util.Map<String, Long> componentCost) {
        java.util.Set<String> active = new java.util.HashSet<>();
        java.util.Set<String> open = new java.util.HashSet<>();
        long total = 0;
        for (int i = 0; i < slots; i++) {
            Program best = null;
            long bestAdds = -1;
            for (Program program : programs) {
                if (open.contains(program.id())) {
                    continue;
                }
                long adds = program.maintenanceCost() + program.requiredMissionComponentIds().stream()
                    .filter(id -> !active.contains(id)).mapToLong(id -> componentCost.getOrDefault(id, 0L)).sum();
                if (adds > bestAdds) {
                    bestAdds = adds;
                    best = program;
                }
            }
            if (best == null) {
                break;
            }
            open.add(best.id());
            active.addAll(best.requiredMissionComponentIds());
            total += bestAdds;
        }
        return total;
    }

    /**
     * A reasonable player's staff: the biggest crew (6, a two-craft docking) and the biggest flight control team
     * (14, a Moon landing) with spares, and four full R&amp;D teams ({@code MissionComponent.RD_SLOT_COUNT} each).
     */
    private static final java.util.Map<Building, Integer> REASONABLE_STAFF = java.util.Map.of(
        Building.ASTRONAUT_CENTER, 7, Building.MISSION_CONTROL, 15, Building.SET_CENTER,
        4 * MissionComponent.RD_SLOT_COUNT);

    private static int reasonableHeads() {
        return REASONABLE_STAFF.values().stream().mapToInt(Integer::intValue).sum();
    }

    /** The buildings' upkeep at the lowest levels that hold the staff, programs and rockets. */
    private static long reasonable(BuildingSchedule buildings, java.util.Map<Building, Integer> staff, int programs,
            int rockets) {
        long total = 0;
        for (Building building : Building.values()) {
            BuildingSchedule.Levels levels = buildings.of(building);
            if (levels != null) {
                total += levels.maintenance().get(levelFor(levels, need(building, staff, programs, rockets)) - 1);
            }
        }
        return total;
    }

    private static String reasonableLevels(BuildingSchedule buildings, java.util.Map<Building, Integer> staff,
            int programs, int rockets) {
        StringBuilder text = new StringBuilder();
        for (Building building : Building.values()) {
            BuildingSchedule.Levels levels = buildings.of(building);
            if (levels != null) {
                int level = levelFor(levels, need(building, staff, programs, rockets));
                text.append(text.isEmpty() ? "" : ", ").append(building).append(' ').append(level).append(" (")
                    .append(String.format("%,d", levels.maintenance().get(level - 1))).append(')');
            }
        }
        return text.toString();
    }

    private static int need(Building building, java.util.Map<Building, Integer> staff, int programs, int rockets) {
        return switch (building) {
            case HEADQUARTERS -> programs;
            case VEHICLE_ASSEMBLY -> rockets;
            default -> staff.getOrDefault(building, 0);
        };
    }

    /** The lowest level, from 1, whose cap holds a need (the top level if none does). */
    private static int levelFor(BuildingSchedule.Levels levels, int need) {
        for (int i = 0; i < levels.caps().size(); i++) {
            if (levels.caps().get(i) >= need) {
                return i + 1;
            }
        }
        return levels.caps().size();
    }

    private static int top(BuildingSchedule buildings, Building building) {
        BuildingSchedule.Levels levels = buildings.of(building);
        return levels.caps().get(levels.caps().size() - 1);
    }

    /** What a review pays at a share of its prestige range, flattened if the playout asked for it. */
    private static long at(int review, BudgetSchedule.Review bracket, double share) {
        long prestige = bracket.minPrestige() + Math.round((bracket.maxPrestige() - bracket.minPrestige()) * share);
        return BudgetProgress.reviewFunding(review, bracket.minPrestige(), bracket.minFunding(), bracket.maxPrestige(),
            bracket.maxFunding(), prestige);
    }

    /** The prestige at which a review's funding reaches a cost, or a word when it never does or always does. */
    private static String breakEven(int review, BudgetSchedule.Review bracket, long cost) {
        if (at(review, bracket, 1.0) < cost) {
            return "never";
        }
        if (bracket.minFunding() >= cost) {
            return "always";
        }
        for (long prestige = bracket.minPrestige(); prestige <= bracket.maxPrestige(); prestige += 50) {
            if (BudgetProgress.reviewFunding(review, bracket.minPrestige(), bracket.minFunding(),
                    bracket.maxPrestige(), bracket.maxFunding(), prestige) >= cost) {
                return String.format("%,d", prestige);
            }
        }
        return String.format("%,d", bracket.maxPrestige());
    }
}
