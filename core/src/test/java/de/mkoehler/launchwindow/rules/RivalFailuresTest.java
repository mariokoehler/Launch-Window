package de.mkoehler.launchwindow.rules;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.ContentPackLoader;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.content.MissionStep;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.RivalFailure;
import de.mkoehler.launchwindow.model.RivalFailure.Phase;
import de.mkoehler.launchwindow.model.RivalFolder;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests rivals' failed attempts ({@code design.md} &sect;2.9, "Failed attempts"): what a turn report
 * gives, roughly where each failed, and what each level of service learns, with no roll.
 *
 * @author Mario Koehler
 */
class RivalFailuresTest {

    private static final ContentCatalog CATALOG =
        ContentPackLoader.load(Path.of("..", "assets", "content", "standard"));
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static MissionConfiguration configuration(String id) {
        return CATALOG.missionConfiguration(id).orElseThrow();
    }

    private static MissionStepResolved step(String mission, String configurationId, String stepId,
            MissionStepOutcome outcome, MissionStepResolved.ComponentFlown... components) {
        return new MissionStepResolved(mission, configurationId, 0, stepId, outcome, List.of(components), 80, false,
            40.0, 40.0, false, 0);
    }

    private static TurnReport report(int turn, ResolutionEvent... events) {
        return new TurnReport(TurnReport.VERSION, turn, 0, 0, 0, 0, List.of(), List.of(), List.of(events), null);
    }

    private static PlayerSnapshot agency(String name, Archetype archetype, int level) {
        PlayerSnapshot snapshot = new PlayerSnapshot(name, archetype, 25_000, 500, 0, 1);
        snapshot.getIntelligence().setLevel(level);
        snapshot.getIntelligence().setTargetLevel(level);
        return snapshot;
    }

    @Test
    void phasesFollowTheMission() {
        MissionConfiguration sputnik = configuration("sputnik-1-orbit");
        assertEquals(Phase.PAD, RivalFailures.phase(sputnik, 0, "01-launch-preparations"));
        assertEquals(Phase.PAD, RivalFailures.phase(sputnik, 0, "02-countdown"));
        assertEquals(Phase.LAUNCH, RivalFailures.phase(sputnik, 0, "03-launch"));
        assertEquals(Phase.LAUNCH, RivalFailures.phase(sputnik, 0, "05-deployment"));
        assertEquals(Phase.FLIGHT, RivalFailures.phase(sputnik, 0, "06-earth-orbit"));
        MissionConfiguration vostok = configuration("vostok-orbital");
        assertEquals(Phase.FLIGHT, RivalFailures.phase(vostok, 0, "06-orbital-flight"));
        assertEquals(Phase.RETURN, RivalFailures.phase(vostok, 0, "08-retrofire"));
        assertEquals(Phase.RETURN, RivalFailures.phase(vostok, 0, "11-descent-and-landing"),
            "the way home lasts to the landing");
        MissionConfiguration x15 = configuration("x-15-flight-test");
        assertEquals(Phase.LAUNCH, RivalFailures.phase(x15, 0, "01-takeoff"), "an air drop's carrier is its launch");
        assertEquals(Phase.LAUNCH, RivalFailures.phase(x15, 0, "02-pre-launch"), "under the carrier, not on a pad");
        assertEquals(Phase.LAUNCH, RivalFailures.phase(x15, 0, "03-launch-from-drop-plane"));
        assertEquals(Phase.FLIGHT, RivalFailures.phase(x15, 0, "04-powered-ascent"), "the plane's own burn");
    }

    @Test
    void everyLaunchStepOfThePackIsOnThePadOrInTheLaunch() {
        List<String> wrong = new ArrayList<>();
        for (MissionConfiguration configuration : CATALOG.missionConfigurations()) {
            if (configuration.seasons().isEmpty()) {
                continue;
            }
            for (MissionStep step : configuration.seasons().getFirst().steps()) {
                String kind = step.id().replaceFirst("^\\d+-", "");
                Phase phase = RivalFailures.phase(configuration, 0, step.id());
                if ((kind.endsWith("launch") && !kind.contains("lunar")) && phase != Phase.LAUNCH
                        || kind.endsWith("countdown") && phase != Phase.PAD) {
                    wrong.add(configuration.id() + "/" + step.id() + " " + phase);
                }
            }
        }
        assertTrue(wrong.isEmpty(), wrong.toString());
    }

    @Test
    void aReportGivesOneFailurePerMissionWithItsSuspect() {
        TurnReport report = report(7,
            step("m1", "vostok-orbital", "03-launch", MissionStepOutcome.SUCCESSFUL),
            step("m1", "vostok-orbital", "04-ascent", MissionStepOutcome.FAILURE,
                new MissionStepResolved.ComponentFlown("r-7", 90, 80),
                new MissionStepResolved.ComponentFlown("vostok-capsule", 10, 50)),
            step("m2", "sputnik-1-orbit", "06-earth-orbit", MissionStepOutcome.MINOR_GLITCH),
            step("m3", "vostok-orbital", "10-reentry", MissionStepOutcome.CATASTROPHIC,
                new MissionStepResolved.ComponentFlown("vostok-capsule", 100, 70)),
            new CrewedFlightsGrounded("m3", "vostok-orbital", 4));
        List<RivalFailure> failures = RivalFailures.of(report, CATALOG, Archetype.SOVIET);
        assertEquals(2, failures.size(), "a glitch the mission survives stays secret");
        RivalFailure first = failures.getFirst();
        assertEquals(new RivalFailure(7, "vostok", "vostok-orbital", "04-ascent", "r-7", Phase.LAUNCH, false,
            Archetype.SOVIET), first, "90 × 20 outweighs 10 × 50");
        RivalFailure second = failures.get(1);
        assertEquals(Phase.RETURN, second.phase());
        assertTrue(second.crewLost());
    }

    @Test
    void everyServiceFromLevelOneLearnsEveryRivalsFailureToItsLevel() throws Exception {
        RivalFailure full = new RivalFailure(7, "vostok", "vostok-orbital", "04-ascent", "r-7", Phase.LAUNCH, false,
            Archetype.SOVIET);
        Map<String, PlayerSnapshot> agencies = Map.of(
            "none", agency("None", Archetype.NASA, 0),
            "one", agency("One", Archetype.NASA, 1),
            "two", agency("Two", Archetype.NASA, 2),
            "three", agency("Three", Archetype.NASA, 3),
            "soviet", agency("Soviet", Archetype.SOVIET, 3));
        Map<String, List<RivalFailure>> failures = Map.of("soviet", List.of(full));
        for (long seed = 1; seed <= 20; seed++) {
            Map<String, List<IntelFinding>> without = IntelligenceBoard.settle(agencies, CATALOG, seed, 8);
            Map<String, List<IntelFinding>> with = IntelligenceBoard.settle(agencies, CATALOG, seed, 8, failures);
            assertEquals(without.get("none"), with.get("none"), "level 0 learns nothing");
            assertEquals(without.get("soviet"), with.get("soviet"), "nobody learns of their own");
            for (String spy : List.of("one", "two", "three")) {
                List<IntelFinding> seen = with.get(spy).stream()
                    .filter(f -> f.kind() == IntelFinding.Kind.FAILED_ATTEMPT).toList();
                assertEquals(1, seen.size(), spy + ": no roll, every time");
                List<IntelFinding> rest = new ArrayList<>(with.get(spy));
                rest.removeAll(seen);
                assertEquals(without.getOrDefault(spy, List.of()), rest, spy + ": nothing else shifts");
                assertEquals("soviet", seen.getFirst().rivalId());
            }
        }
        RivalFailure one = failure(IntelligenceBoard.settle(agencies, CATALOG, 1, 8, failures).get("one"));
        assertNull(one.configurationId());
        assertNull(one.stepId());
        assertNull(one.componentId());
        assertEquals(Phase.LAUNCH, one.phase());
        RivalFailure two = failure(IntelligenceBoard.settle(agencies, CATALOG, 1, 8, failures).get("two"));
        assertEquals("04-ascent", two.stepId());
        assertNull(two.componentId());
        assertEquals(full, failure(IntelligenceBoard.settle(agencies, CATALOG, 1, 8, failures).get("three")));

        // Applied, it goes in the rival's folder, kept to the cap, and survives a save.
        PlayerSnapshot spy = agency("One", Archetype.NASA, 1);
        for (int i = 0; i < Intelligence.FAILURES_KEPT + 3; i++) {
            Intelligence.advanceSeason(spy, List.of(IntelFinding.failedAttempt("soviet", "Soviet", full.seenAt(1))), i);
        }
        RivalFolder folder = spy.getIntelligence().getFolders().get("soviet");
        assertEquals(Intelligence.FAILURES_KEPT, folder.getFailures().size());
        PlayerSnapshot saved = MAPPER.readValue(MAPPER.writeValueAsBytes(spy), PlayerSnapshot.class);
        assertEquals(folder.getFailures(), saved.getIntelligence().getFolders().get("soviet").getFailures());
    }

    private static RivalFailure failure(List<IntelFinding> findings) {
        return findings.stream().filter(f -> f.kind() == IntelFinding.Kind.FAILED_ATTEMPT).findFirst().orElseThrow()
            .failure();
    }
}
