package de.mkoehler.launchwindow.rules;

/**
 * <p>Reports that one {@code SET} employee's R&amp;D work on one mission
 * component produced a season's worth of growth during a
 * {@link ResearchProgress#advanceSeason} pass - both of that season's
 * effects at once, the employee's own skill growing and the targeted
 * component's reliability growing (see {@link ResearchProgress}'s own
 * class Javadoc for why these are two separate numbers, not one).</p>
 *
 * @param personnelId     the researching employee's stable id
 * @param name            the researching employee's display name
 * @param componentId     the mission component id their R&amp;D targeted
 * @param skillGain       how much the employee's own skill grew this season
 * @param reliabilityGain how much the component's reliability grew this
 *                        season
 * @author Mario Koehler
 */
public record ResearchCompleted(
    String personnelId,
    String name,
    String componentId,
    double skillGain,
    double reliabilityGain
) implements ResolutionEvent {
}
