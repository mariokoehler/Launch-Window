package de.mkoehler.launchwindow.devtools.replay;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.content.MissionStep;
import de.mkoehler.launchwindow.rules.MissionLaunch;
import de.mkoehler.launchwindow.rules.MissionStepOutcome;
import de.mkoehler.launchwindow.rules.MissionStepResolved;
import de.mkoehler.launchwindow.rules.ResolutionEvent;
import de.mkoehler.launchwindow.rules.TurnReport;

import java.util.ArrayList;
import java.util.List;

/**
 * A made-up turn report with one mission season in it, for the {@link MissionReplayer}: every step of
 * the season's plan, the chosen outcome on the chosen step (or on all of them), success on the rest.
 * Nothing is rolled, so the replay screen plays exactly the variant asked for.
 *
 * @author Mario Koehler
 */
public final class ReviewReport {

    /** The made-up mission instance's id. */
    public static final String MISSION_ID = "m-review";
    /** Every step's success chance, in percent. */
    private static final double SUCCESS_CHANCE = 85.0;
    /** The saving roll's chance, in percent, for a step whose first roll failed. */
    private static final double SAVING_CHANCE = 60.0;
    /** Each component's reliability as flown. */
    private static final double RELIABILITY = 85.0;
    /** The prestige a step adds while the season goes well. */
    private static final long STEP_PRESTIGE = 10;
    /** The turn the report claims to come from. */
    private static final long TURN = 40;

    private ReviewReport() {
    }

    /**
     * Builds the report.
     *
     * @param catalog the content pack
     * @param config  the mission
     * @param season  the season, from 0
     * @param outcome the outcome to play
     * @param target  the step that gets it, from 0, or -1 for every step; a failure on one step ends
     *                the season there, as in the game, while a failure on every step plays them all
     * @return the report
     */
    public static TurnReport build(ContentCatalog catalog, MissionConfiguration config, int season,
                                   MissionStepOutcome outcome, int target) {
        List<MissionStep> plan = config.seasons().get(season).steps();
        List<ResolutionEvent> events = new ArrayList<>();
        long prestige = 0;
        boolean ended = season == config.seasons().size() - 1;
        for (int i = 0; i < plan.size(); i++) {
            MissionStep step = plan.get(i);
            MissionStepOutcome result = target < 0 || i == target ? outcome : MissionStepOutcome.SUCCESSFUL;
            boolean first = result == MissionStepOutcome.SUCCESSFUL;
            boolean ends = result == MissionStepOutcome.FAILURE || result == MissionStepOutcome.CATASTROPHIC;
            prestige = ends ? -step.failurePrestigeDeduction()
                : prestige + (result == MissionStepOutcome.MINOR_GLITCH ? STEP_PRESTIGE / 2 : STEP_PRESTIGE);
            events.add(new MissionStepResolved(MISSION_ID, config.id(), season, step.id(), result,
                components(catalog, config, step), SUCCESS_CHANCE, first, first ? null : SAVING_CHANCE,
                first ? null : SAVING_CHANCE, first ? null : result == MissionStepOutcome.MINOR_GLITCH_SOLVED, prestige));
            if (ends && target >= 0) {
                ended = true;
                break;
            }
        }
        List<String> crew = new ArrayList<>();
        for (int i = 0; i < config.crewSlots().size(); i++) {
            crew.add("Test Pilot " + (char) ('A' + i));
        }
        TurnReport.MissionFlown flown = new TurnReport.MissionFlown(MISSION_ID, config.id(), season,
            config.seasons().size(), ended, crew);
        return new TurnReport(TurnReport.VERSION, TURN, 0, 0, 0, 0, List.of(), List.of(flown), events);
    }

    /** The step's components, each rocket slot filled with the pack's first rocket that can fly its launch. */
    private static List<MissionStepResolved.ComponentFlown> components(ContentCatalog catalog,
                                                                        MissionConfiguration config, MissionStep step) {
        List<MissionStepResolved.ComponentFlown> flown = new ArrayList<>();
        for (MissionStep.ComponentInvolvement involvement : step.componentInvolvements()) {
            String id = involvement.componentId();
            int slot = MissionConfiguration.rocketSlotIndex(id);
            if (slot >= 0 && slot < config.launches().size()) {
                MissionConfiguration.Launch launch = config.launches().get(slot);
                id = catalog.missionComponents().stream()
                    .filter(component -> component.rocket() != null && MissionLaunch.canLift(component, launch, catalog))
                    .map(MissionComponent::id).findFirst().orElse(id);
            }
            flown.add(new MissionStepResolved.ComponentFlown(id, involvement.percent(), RELIABILITY));
        }
        return flown;
    }
}
