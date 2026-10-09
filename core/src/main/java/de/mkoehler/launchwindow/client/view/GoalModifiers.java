package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.content.Program;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * What goals do to a flight's reliability, for the program and mission cards (play test,
 * 2026-10-09): a goal not reached yet lowers the reliability some of a mission's components fly
 * with, a goal reached can raise it. Shown so a player sees the cost of skipping ahead before
 * opening a program or picking a mission. The numbers are the content's own, applied by
 * {@code MissionProgress.goalModifierFor}.
 *
 * @author Mario Koehler
 */
public final class GoalModifiers {

    private GoalModifiers() {
    }

    /**
     * One goal's effect.
     *
     * @param goalId      the goal
     * @param componentId the component it touches, or {@code null} for a program's summary
     * @param amount      the reliability points, always positive
     */
    public record Line(String goalId, String componentId, int amount) {
    }

    /**
     * The effects that apply now.
     *
     * @param missing        the points lost to goals not reached
     * @param reached        the points gained from goals reached
     * @param missingLines   each goal not reached, largest first
     * @param reachedLines   each goal reached, largest first
     */
    public record Summary(int missing, int reached, List<Line> missingLines, List<Line> reachedLines) {

        /**
         * Returns whether there's nothing to show.
         *
         * @return whether neither applies
         */
        public boolean isEmpty() {
            return missing == 0 && reached == 0;
        }

        /**
         * Returns the card's line on goals not reached, or {@code null}.
         *
         * @param program whether it's a program's card ("up to", over its missions)
         * @return the line
         */
        public String missingText(boolean program) {
            return missing == 0 ? null
                : (program ? "Skipped goals: up to −" : "Goals not reached: −") + missing + " reliability";
        }

        /**
         * Returns the card's line on goals reached, or {@code null}.
         *
         * @param program whether it's a program's card
         * @return the line
         */
        public String reachedText(boolean program) {
            return reached == 0 ? null : (program ? "Goals reached: up to +" : "Goals reached: +") + reached
                + " reliability";
        }

        /**
         * Returns the tooltip: every goal and what it does.
         *
         * @param program whether it's a program's card
         * @return the text
         */
        public String detail(boolean program) {
            List<String> parts = new ArrayList<>();
            parts.add(program
                ? "Its missions count on earlier goals. Each one not reached lowers the reliability the mission's "
                    + "components fly with; some reached goals raise it. The figures are the most any one of its "
                    + "missions gets."
                : "Each goal not reached lowers the reliability a component flies with; some reached goals raise it.");
            for (Line line : missingLines) {
                parts.add(describe(line, "−", "not reached"));
            }
            for (Line line : reachedLines) {
                parts.add(describe(line, "+", "reached"));
            }
            return String.join(" ", parts);
        }

        private static String describe(Line line, String sign, String state) {
            return ContentNames.of(line.goalId()) + " " + state + ": " + sign + line.amount()
                + (line.componentId() == null ? "." : " on " + ContentNames.of(line.componentId()) + ".");
        }
    }

    /**
     * Returns what goals do to one mission now.
     *
     * @param configuration the mission
     * @param held          whether a goal is achieved or cancelled
     * @return the effects
     */
    public static Summary of(MissionConfiguration configuration, Predicate<String> held) {
        List<Line> missing = new ArrayList<>();
        List<Line> reached = new ArrayList<>();
        for (MissionConfiguration.GoalModifier dependency : configuration.goalDependencies()) {
            if (!held.test(dependency.goalId())) {
                missing.add(new Line(dependency.goalId(), dependency.componentId(), dependency.amount()));
            }
        }
        for (MissionConfiguration.GoalModifier boost : configuration.goalBoosts()) {
            if (held.test(boost.goalId())) {
                reached.add(new Line(boost.goalId(), boost.componentId(), boost.amount()));
            }
        }
        return summary(missing, reached);
    }

    /**
     * Returns what goals do to a program's missions now, counting only goals from outside the program
     * (its own early flights are the way in, not a skipped step): the most any one mission loses or
     * gains, and each goal at the most it does to any mission.
     *
     * @param program the program
     * @param catalog the content pack
     * @param held    whether a goal is achieved or cancelled
     * @return the effects
     */
    public static Summary ofProgram(Program program, ContentCatalog catalog, Predicate<String> held) {
        List<MissionConfiguration> missions = Missions.of(program, catalog);
        Set<String> own = new HashSet<>();
        for (MissionConfiguration mission : missions) {
            mission.seasons().forEach(season -> season.steps().forEach(step -> own.addAll(step.goalIdsAchieved())));
        }
        int worst = 0;
        int best = 0;
        Map<String, Integer> missingByGoal = new LinkedHashMap<>();
        Map<String, Integer> reachedByGoal = new LinkedHashMap<>();
        for (MissionConfiguration mission : missions) {
            int lost = 0;
            for (MissionConfiguration.GoalModifier dependency : mission.goalDependencies()) {
                if (!own.contains(dependency.goalId()) && !held.test(dependency.goalId())) {
                    lost += dependency.amount();
                    missingByGoal.merge(dependency.goalId(), dependency.amount(), Math::max);
                }
            }
            int gained = 0;
            for (MissionConfiguration.GoalModifier boost : mission.goalBoosts()) {
                if (!own.contains(boost.goalId()) && held.test(boost.goalId())) {
                    gained += boost.amount();
                    reachedByGoal.merge(boost.goalId(), boost.amount(), Math::max);
                }
            }
            worst = Math.max(worst, lost);
            best = Math.max(best, gained);
        }
        List<Line> missing = new ArrayList<>();
        missingByGoal.forEach((goal, amount) -> missing.add(new Line(goal, null, amount)));
        List<Line> reached = new ArrayList<>();
        reachedByGoal.forEach((goal, amount) -> reached.add(new Line(goal, null, amount)));
        Summary lines = summary(missing, reached);
        return new Summary(worst, best, lines.missingLines(), lines.reachedLines());
    }

    private static Summary summary(List<Line> missing, List<Line> reached) {
        Comparator<Line> largest = Comparator.comparingInt(Line::amount).reversed();
        missing.sort(largest);
        reached.sort(largest);
        return new Summary(missing.stream().mapToInt(Line::amount).sum(), reached.stream().mapToInt(Line::amount).sum(),
            List.copyOf(missing), List.copyOf(reached));
    }
}
