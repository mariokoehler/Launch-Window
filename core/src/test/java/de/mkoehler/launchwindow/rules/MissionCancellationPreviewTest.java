package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.ContentPackLoader;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.InProgressMission;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests that the cancellation preview says exactly what {@link MissionCancellation#cancel} then
 * does, and that a mission in flight can be assessed from its next season.
 *
 * @author Mario Koehler
 */
class MissionCancellationPreviewTest {

    private static final ContentCatalog PACK = ContentPackLoader.load(Path.of("..", "assets", "content", "early-era"));

    private static PlayerSnapshot withMission(String configurationId, int seasonIndex, List<String> crew) {
        PlayerSnapshot state = new PlayerSnapshot("Vega", Archetype.SOVIET, 10_000, 1_000, 0, 1);
        MissionConfiguration configuration = PACK.missionConfiguration(configurationId).orElseThrow();
        InProgressMission mission = new InProgressMission("m1", configurationId, crew,
            new ArrayList<>(Arrays.asList(new String[configuration.controlSlots().size()])));
        mission.setLaunchCost(2_113);
        for (int i = 0; i < seasonIndex; i++) {
            mission.setCurrentSeasonIndex(i + 1);
        }
        state.getInProgressMissions().put("m1", mission);
        return state;
    }

    /**
     * An unflown mission scrubs for the whole launch cost; the preview and the cancel agree.
     */
    @Test
    void scrubRefundsInFull() {
        PlayerSnapshot state = withMission("sputnik-1-extended", 0, List.of());
        MissionCancellation.Preview preview = MissionCancellation.preview(state, "m1", PACK);
        assertEquals(new MissionCancellation.Preview(MissionCancellation.Outcome.SCRUBBED, 2_113, 0), preview);
        assertEquals(MissionCancellation.Outcome.SCRUBBED, MissionCancellation.cancel(state, "m1", PACK));
        assertEquals(10_000 + 2_113, state.getFunds());
    }

    /**
     * An uncrewed mission in flight returns the configuration's share, rounded down as the rule
     * pays it, and costs its prestige; the preview names both.
     */
    @Test
    void cancelInFlightMatchesThePreview() {
        PlayerSnapshot state = withMission("sputnik-1-extended", 2, List.of());
        MissionConfiguration configuration = PACK.missionConfiguration("sputnik-1-extended").orElseThrow();
        MissionCancellation.Preview preview = MissionCancellation.preview(state, "m1", PACK);
        assertEquals(MissionCancellation.Outcome.CANCELLED, preview.outcome());
        assertEquals(2_113L * configuration.cancellationRefundPercent() / 100, preview.refund());
        assertEquals(Math.min(1_000, configuration.cancellationPrestigeDeduction()), preview.prestigeLost());
        MissionCancellation.cancel(state, "m1", PACK);
        assertEquals(10_000 + preview.refund(), state.getFunds());
        assertEquals(1_000 - preview.prestigeLost(), state.getPrestige());
    }

    /**
     * A crewed mission in flight can't be abandoned, and the preview says so first.
     */
    @Test
    void crewedInFlightCantBeAbandoned() {
        PlayerSnapshot state = withMission("vostok-orbital", 1, List.of("a1"));
        assertEquals(MissionCancellation.Outcome.CREWED_MISSION_IN_FLIGHT,
            MissionCancellation.preview(state, "m1", PACK).outcome());
        assertEquals(MissionCancellation.Outcome.CREWED_MISSION_IN_FLIGHT, MissionCancellation.cancel(state, "m1", PACK));
        assertEquals(10_000, state.getFunds());
    }

    /**
     * Assessing from season 0 is the whole flight; from a later season, only what's left.
     */
    @Test
    void assessFromASeason() {
        PlayerSnapshot state = withMission("sputnik-1-extended", 0, List.of());
        MissionConfiguration configuration = PACK.missionConfiguration("sputnik-1-extended").orElseThrow();
        List<String> control = Arrays.asList(new String[configuration.controlSlots().size()]);
        MissionAssessment.Assessment whole = MissionAssessment.assess(state, PACK, configuration, List.of(), control,
            List.of("r7-sputnik"));
        assertEquals(whole, MissionAssessment.assess(state, PACK, configuration, List.of(), control,
            List.of("r7-sputnik"), 0));
        MissionAssessment.Assessment last = MissionAssessment.assess(state, PACK, configuration, List.of(), control,
            List.of("r7-sputnik"), configuration.seasons().size() - 1);
        assertEquals(configuration.seasons().get(configuration.seasons().size() - 1).steps().size(),
            last.steps().size());
    }
}
