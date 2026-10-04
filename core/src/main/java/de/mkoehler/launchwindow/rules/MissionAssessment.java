package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.content.MissionSeason;
import de.mkoehler.launchwindow.content.MissionStep;
import de.mkoehler.launchwindow.model.InProgressMission;
import de.mkoehler.launchwindow.model.PlayerSnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The odds of a planned flight, exactly, from the same formulas the season pass rolls against
 * ({@code ui-spec.md} &sect;4.6.7, gap G7): for each step, the chance of each of the five outcomes,
 * and for the flight, the chance it completes and the chance its crew is lost. Changes nothing.
 *
 * <p>A step's first roll succeeds with the base probability; if it fails, the controllers' saving
 * roll succeeds with the saving-throw probability; what's left splits into catastrophic (a
 * failure on a step whose crew isn't at risk), failure, and minor glitch by
 * {@link MissionStepResolution}'s band fractions. A failure or catastrophe ends the flight; the
 * other three continue and award the step's goals, so later steps read their goal modifiers with
 * those goals held, as the season pass does.</p>
 *
 * <p>Left out, because they can't be known at planning time: Expert rescues (they depend on what
 * the whole turn uses up), the reliability and skill changes between a multi-season mission's
 * seasons, and random events.</p>
 *
 * @author Mario Koehler
 */
public final class MissionAssessment {

    private MissionAssessment() {
    }

    /**
     * One step's odds. The five outcome chances add up to 1.
     *
     * @param seasonIndex  the season the step flies in, from 0
     * @param step         the step
     * @param hardware     the components' weighted reliability after goal modifiers, 0-100
     * @param success      the first roll succeeds
     * @param solved       the first roll fails and the controllers save it
     * @param glitch       a minor glitch: the flight goes on at half the step's prestige
     * @param failure      a failure: the flight ends
     * @param catastrophic a catastrophe: the flight ends and the crew is lost
     */
    public record StepOdds(int seasonIndex, MissionStep step, double hardware, double success, double solved,
                           double glitch, double failure, double catastrophic) {

        /**
         * Returns the chance this step ends the flight.
         *
         * @return failure plus catastrophic
         */
        public double endsFlight() {
            return failure + catastrophic;
        }
    }

    /**
     * A planned flight's odds.
     *
     * @param steps      every step's odds, in flight order
     * @param completion the chance the flight completes every step
     * @param crewLoss   the chance the crew is lost
     */
    public record Assessment(List<StepOdds> steps, double completion, double crewLoss) {
    }

    /**
     * Assesses a planned flight with the given people and rockets, as the player holds things now.
     *
     * @param snapshot       the player
     * @param catalog        the content catalog
     * @param configuration  the configuration
     * @param crewSlotIds    one astronaut id per crew seat, {@code null} for an empty one
     * @param controlSlotIds one id per control slot, {@code null} for an empty one
     * @param rocketIds      one rocket per launch
     * @return the odds
     * @throws NullPointerException if an argument is {@code null}
     */
    public static Assessment assess(PlayerSnapshot snapshot, ContentCatalog catalog,
            MissionConfiguration configuration, List<String> crewSlotIds, List<String> controlSlotIds,
            List<String> rocketIds) {
        return assess(snapshot, catalog, configuration, crewSlotIds, controlSlotIds, rocketIds, 0);
    }

    /**
     * Assesses what's left of a flight from a season on, for a mission already in flight: its
     * earlier seasons have flown, and their goals are already in the player's state.
     *
     * @param snapshot       the player
     * @param catalog        the content catalog
     * @param configuration  the configuration
     * @param crewSlotIds    one astronaut id per crew seat, {@code null} for an empty one
     * @param controlSlotIds one id per control slot, {@code null} for an empty one
     * @param rocketIds      one rocket per launch
     * @param fromSeason     the first season still to fly, from 0
     * @return the odds of the seasons from {@code fromSeason} on
     * @throws NullPointerException if an argument is {@code null}
     */
    public static Assessment assess(PlayerSnapshot snapshot, ContentCatalog catalog,
            MissionConfiguration configuration, List<String> crewSlotIds, List<String> controlSlotIds,
            List<String> rocketIds, int fromSeason) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(catalog, "catalog must not be null");
        Objects.requireNonNull(configuration, "configuration must not be null");
        // A stand-in for the mission as it would fly, so the odds come from the season pass's own sums.
        InProgressMission plan = new InProgressMission("assessment", configuration.id(), crewSlotIds, controlSlotIds);
        plan.setRocketIds(rocketIds);
        Set<String> held = new HashSet<>(snapshot.getAchievedGoals().keySet());
        held.addAll(snapshot.getCancelledGoalIds());

        List<StepOdds> steps = new ArrayList<>();
        double reach = 1;
        double crewLoss = 0;
        for (int seasonIndex = Math.max(0, fromSeason); seasonIndex < configuration.seasons().size(); seasonIndex++) {
            MissionSeason season = configuration.seasons().get(seasonIndex);
            for (MissionStep step : season.steps()) {
                StepOdds odds = stepOdds(snapshot, catalog, plan, seasonIndex, step, held::contains);
                steps.add(odds);
                crewLoss += reach * odds.catastrophic();
                reach *= 1 - odds.endsFlight();
                // Past this step the flight goes on only if the step awarded its goals.
                for (String goalId : step.goalIdsAchieved()) {
                    held.addAll(GoalCascade.unheld(goalId, held::contains, catalog));
                }
            }
        }
        return new Assessment(steps, reach, crewLoss);
    }

    private static StepOdds stepOdds(PlayerSnapshot snapshot, ContentCatalog catalog, InProgressMission plan,
            int seasonIndex, MissionStep step, java.util.function.Predicate<String> held) {
        double componentSum = MissionProgress.componentContributionSum(snapshot, catalog, plan, step, held);
        boolean crewInvolved = !step.crewInvolvements().isEmpty();
        double crewSum = crewInvolved ? MissionProgress.crewContributionSum(snapshot, plan, step) : 0;
        double base = MissionStepResolution.computeBaseProbability(componentSum, crewInvolved, crewSum);
        double controllerSum = MissionProgress.controlContributionSum(snapshot, catalog, plan, step);
        double save = MissionStepResolution.computeSavingThrowProbability(base, controllerSum);

        double first = clamp(base / 100.0);
        double saving = clamp(save / 100.0);
        double rest = (1 - first) * (1 - saving);
        double catastrophicBand = rest * MissionStepResolution.CATASTROPHIC_BAND_FRACTION;
        double failure = rest * MissionStepResolution.FAILURE_BAND_FRACTION;
        double glitch = rest - catastrophicBand - failure;
        double catastrophic = step.crewRisk() ? catastrophicBand : 0;
        if (!step.crewRisk()) {
            failure += catastrophicBand;
        }
        return new StepOdds(seasonIndex, step, componentSum, first, (1 - first) * saving, glitch, failure,
            catastrophic);
    }

    private static double clamp(double probability) {
        return Math.max(0, Math.min(1, probability));
    }
}
