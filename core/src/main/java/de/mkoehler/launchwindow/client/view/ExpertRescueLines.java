package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.rules.ExpertRescueApplied;
import de.mkoehler.launchwindow.rules.MissionStepResolution;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Words the Experts' part in a step's saving roll for the replay. {@link ExpertRescueApplied}'s
 * figures ignore the cap, which the rules apply once at the end, so each line is held to it here:
 * the percentages always lead up to the saving roll actually rolled.
 *
 * @author Mario Koehler
 */
public final class ExpertRescueLines {

    private ExpertRescueLines() {
    }

    /**
     * Returns a line per Expert consulted on one step, in the order they were consulted.
     *
     * @param rescues the step's events, in order
     * @return the lines
     */
    public static List<String> of(List<ExpertRescueApplied> rescues) {
        List<String> lines = new ArrayList<>();
        if (rescues.isEmpty()) {
            return lines;
        }
        double unboosted = rescues.get(0).probabilityBefore();
        double limit = Math.max(unboosted, MissionStepResolution.EXPERT_RESCUE_PROBABILITY_CAP);
        double shown = unboosted;
        for (ExpertRescueApplied rescue : rescues) {
            String who = rescue.name() + ", an Expert in " + CrewSkills.label(rescue.skillName());
            double after = MissionStepResolution.capExpertRescue(rescue.probabilityAfter(), unboosted);
            if (rescue.boostPercent() <= 0) {
                lines.add(who + ", had nothing left to give this turn.");
            } else if (Math.round(after) <= Math.round(shown)) {
                lines.add(String.format(Locale.ROOT, "%s, couldn't add anything: the saving roll is already at its "
                    + "%.0f%% limit.", who, limit));
            } else {
                lines.add(String.format(Locale.ROOT, "%s, raised the saving roll from %.0f%% to %.0f%%.", who, shown,
                    after));
            }
            shown = after;
        }
        return lines;
    }
}
