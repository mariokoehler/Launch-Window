package de.mkoehler.launchwindow.rules;

import java.util.List;

/**
 * One mission step resolved during a season pass, with everything the replay viewer shows
 * ({@code ui-spec.md} &sect;4.7.3, gap G2): the components as flown, the chances, which rolls passed,
 * and the season's prestige after the step. The chances are the ones the rolls were made against;
 * nothing here is recomputed.
 *
 * @param missionInstanceId      the mission instance
 * @param missionConfigurationId its configuration
 * @param seasonIndex            the season the step flew in, from 0
 * @param stepId                 the step
 * @param outcome                the outcome
 * @param components             each component the step involved, as it flew
 * @param successChance          the first roll's chance, 0-100 (crew blend included)
 * @param firstRollPassed        whether the first roll succeeded
 * @param savingChanceBeforeExperts the controllers' saving-roll chance before any Expert rescue,
 *                               0-100, or {@code null} when no saving roll was needed
 * @param savingChance           the saving-roll chance it was rolled against, Expert rescues
 *                               included, or {@code null} when no saving roll was needed
 * @param savingRollPassed       whether the saving roll succeeded, or {@code null} when none was made
 * @param seasonPrestigeAfter    the season's running prestige after this step (a failing step
 *                               replaces the total with its deduction)
 * @author Mario Koehler
 */
public record MissionStepResolved(
    String missionInstanceId,
    String missionConfigurationId,
    int seasonIndex,
    String stepId,
    MissionStepOutcome outcome,
    List<ComponentFlown> components,
    double successChance,
    boolean firstRollPassed,
    Double savingChanceBeforeExperts,
    Double savingChance,
    Boolean savingRollPassed,
    long seasonPrestigeAfter
) implements ResolutionEvent {

    /**
     * Copies the component list.
     */
    public MissionStepResolved {
        components = components == null ? List.of() : List.copyOf(components);
    }

    /**
     * A component as a step flew it.
     *
     * @param componentId the component (a rocket slot already resolved to the chosen rocket)
     * @param percent     its involvement in the step, 1-100
     * @param reliability its reliability as flown, after goal penalties and boosts
     */
    public record ComponentFlown(String componentId, int percent, double reliability) {
    }

    /**
     * Returns a copy with the season's running prestige after the step filled in.
     *
     * @param prestige the running prestige
     * @return the copy
     */
    MissionStepResolved withSeasonPrestigeAfter(long prestige) {
        return new MissionStepResolved(missionInstanceId, missionConfigurationId, seasonIndex, stepId, outcome,
            components, successChance, firstRollPassed, savingChanceBeforeExperts, savingChance, savingRollPassed,
            prestige);
    }
}
