package de.mkoehler.launchwindow.client.view;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.rules.MissionStepOutcome;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ReplayClips} and the clips in {@code assets/ui/replay}.
 *
 * @author Mario Koehler
 */
class ReplayClipsTest {

    private static final Path ASSETS = Path.of("..", "assets");
    private static final ReplayClips CLIPS = ReplayClips.load(ASSETS);

    /**
     * Every rule's clip has a success, a glitch and a failure, each a video with a sidecar whose
     * result shows within its length; the no-signal clip and its still are there.
     */
    @Test
    void everyRuleHasItsThreeVariants() {
        for (ReplayClips.Rule rule : CLIPS.rules()) {
            for (MissionStepOutcome outcome : new MissionStepOutcome[]{MissionStepOutcome.SUCCESSFUL,
                MissionStepOutcome.MINOR_GLITCH, MissionStepOutcome.FAILURE}) {
                ReplayClips.Clip clip = CLIPS.clipFor("03-" + rule.step(), before(rule), outcome);
                assertTrue(clip.signal(), rule.clip() + " has no " + ReplayClips.variant(outcome));
                assertTrue(Files.exists(ASSETS.resolve(clip.video())), clip.video());
                assertTrue(clip.revealAt() > 0 && clip.revealAt() <= clip.duration(), clip.video());
            }
        }
        assertTrue(Files.exists(ASSETS.resolve(ReplayClips.noSignal().video())));
        assertTrue(Files.exists(ASSETS.resolve(ReplayClips.noSignal().still())));
    }

    /**
     * A step matches by its kind, its number, any vehicle in front and a craft's letter dropped;
     * nothing else matches, so a step that only ends like another doesn't borrow its clip.
     */
    @Test
    void matchesStepsByKind() {
        assertEquals("launch", CLIPS.clipName("03-launch").orElseThrow());
        assertEquals("launch", CLIPS.clipName("09-gemini-b-launch").orElseThrow());
        assertEquals("launch", CLIPS.clipName("03-atv-launch").orElseThrow());
        assertEquals("launch-preparations", CLIPS.clipName("01-launch-preparations").orElseThrow());
        assertEquals("launch-preparations", CLIPS.clipName("01-atv-launch-preparations").orElseThrow());
        assertEquals("deployment", CLIPS.clipName("05-soyuz-7k-ok-deployment").orElseThrow());
        assertEquals("deployment", CLIPS.clipName("05-spc-b-deployment").orElseThrow());
        assertEquals("earth-orbit", CLIPS.clipName("06-earth-orbit-a").orElseThrow());
        assertEquals("air-drop", CLIPS.clipName("03-launch-from-drop-plane").orElseThrow());
        assertEquals("air-cruise", CLIPS.clipName("02-pre-launch").orElseThrow());
        // The step before picks among a step's rules: a landing after the powered descent is on the
        // Moon (a robot's after its approach too), after a descent on a runway; with no rule for what came
        // before, no clip.
        assertEquals("lunar-landing", CLIPS.clipName("12-landing", "11-powered-descent").orElseThrow());
        assertEquals("lunar-landing", CLIPS.clipName("09-landing", "08-lunar-approach").orElseThrow());
        assertEquals("runway-landing", CLIPS.clipName("06-landing", "05-descent").orElseThrow());
        assertEquals("probe-landing", CLIPS.clipName("14-landing", "13-lander-descent").orElseThrow());
        assertTrue(CLIPS.clipName("14-landing", "13-no-such-step").isEmpty());
        // A step further back can pick too: the step before a lander's descent is its entry at either
        // planet, but only at Mars did the season start with the approach to Mars.
        assertEquals("mars-descent", CLIPS.clipName("05-lander-descent",
            java.util.List.of("01-earth-mars-transit", "02-mars-approach", "03-lander-detachment", "04-lander-entry")).orElseThrow());
        assertEquals("venus-descent", CLIPS.clipName("12-lander-descent",
            java.util.List.of("09-venus-approach", "10-lander-release", "11-lander-entry")).orElseThrow());
        assertEquals("venus-descent", CLIPS.clipName("12-lander-descent", "11-lander-entry").orElseThrow());
        assertTrue(CLIPS.clipName("12-landing").isEmpty());
        // lunar-ascent plays its own clip, never ascent's; ascent plays it only after a lunar liftoff.
        assertEquals("lunar-ascent", CLIPS.clipName("12-lunar-ascent").orElseThrow());
        assertEquals("lunar-ascent", CLIPS.clipName("15-ascent", "14-lunar-liftoff").orElseThrow());
        assertEquals("ascent", CLIPS.clipName("04-ascent", "03-launch").orElseThrow());
        assertEquals("rocket-climb", CLIPS.clipName("04-powered-ascent").orElseThrow());
        assertEquals("undocking", CLIPS.clipName("07-lk-deployment").orElseThrow());
        assertEquals("rocket-flight", CLIPS.clipName("03-wings-deployment").orElseThrow());
        assertEquals("lander-engine-test", CLIPS.clipName("05-lem-tests-earth-orbit").orElseThrow());
    }

    /**
     * The vehicles cover the standard pack: every prefix in front of two of a launch's or a
     * return's steps, or one with a digit (a craft's name), is listed, and every listed one flies.
     */
    @Test
    void vehiclesCoverThePack() throws IOException {
        Set<String> kinds = new TreeSet<>();
        JsonNode configurations = new ObjectMapper().readTree(
            Files.readString(ASSETS.resolve("content/standard/mission-configurations.json")));
        for (JsonNode configuration : configurations) {
            for (JsonNode season : configuration.get("seasons")) {
                for (JsonNode step : season.get("steps")) {
                    kinds.add(ReplayClips.kind(step.get("id").asText()));
                }
            }
        }
        Map<String, Integer> seen = new TreeMap<>();
        for (String kind : kinds) {
            for (String marker : List.of("launch-preparations", "countdown", "launch", "deployment", "retrofire", "reentry")) {
                if (kind.endsWith("-" + marker)) {
                    seen.merge(kind.substring(0, kind.length() - marker.length() - 1), 1, Integer::sum);
                }
            }
        }
        seen.forEach((prefix, count) -> {
            if (count >= 2 || prefix.matches(".*\\d.*")) {
                assertTrue(CLIPS.vehicles().contains(prefix), prefix + " is a vehicle");
            }
        });
        for (String vehicle : CLIPS.vehicles()) {
            assertTrue(kinds.stream().anyMatch(kind -> kind.startsWith(vehicle + "-")), vehicle + " flies no step");
        }
    }

    /**
     * Both glitches play the glitch; a catastrophe plays the failure while the step has no
     * catastrophic clip; a step without a clip plays no signal.
     */
    @Test
    void picksTheVariant() {
        assertTrue(CLIPS.clipFor("03-launch", MissionStepOutcome.MINOR_GLITCH_SOLVED).video().endsWith("launch-glitch.webm"));
        assertTrue(CLIPS.clipFor("03-launch", MissionStepOutcome.CATASTROPHIC).video().endsWith("launch-failure.webm"));
        ReplayClips.Clip none = CLIPS.clipFor("07-no-such-step", MissionStepOutcome.SUCCESSFUL);
        assertFalse(none.signal());
        assertTrue(none.loop());
        assertEquals(ReplayClips.NO_SIGNAL_REVEAL, none.revealAt());
    }

    /**
     * Every cue names a sound in the pool and fits its clip, and a clip's variants sound the same
     * until the earliest of their reveals: the sound tells nothing the picture doesn't.
     */
    @Test
    void cuesNameKnownSoundsAndSpoilNothing() {
        ReplaySounds sounds = ReplaySounds.load(ASSETS);
        for (ReplayClips.Rule rule : CLIPS.rules()) {
            java.util.List<ReplayClips.Clip> variants = java.util.stream.Stream.of(MissionStepOutcome.SUCCESSFUL,
                MissionStepOutcome.MINOR_GLITCH, MissionStepOutcome.FAILURE, MissionStepOutcome.CATASTROPHIC)
                .map(outcome -> CLIPS.clipFor("03-" + rule.step(), before(rule), outcome)).toList();
            float reveal = (float) variants.stream().mapToDouble(ReplayClips.Clip::revealAt).min().orElse(0);
            java.util.List<ReplayClips.Cue> before = null;
            for (ReplayClips.Clip clip : variants) {
                for (ReplayClips.Cue cue : clip.cues()) {
                    assertTrue(sounds.sound(cue.sound()).isPresent(), clip.video() + ": " + cue.sound());
                    assertTrue(cue.at() >= 0 && cue.at() < clip.duration(), clip.video() + ": " + cue);
                    assertTrue(cue.until() == null || cue.until() > cue.at(), clip.video() + ": " + cue);
                    assertTrue(!cue.carries() || cue.at() == 0, clip.video() + ": " + cue);
                }
                // What's heard before the reveal: a loop that stops (its fade starts) at or after it
                // sounds there as one that plays on, so an engine may cut later in one variant. Times are
                // compared as the reveal is, in float: a cue at exactly the reveal is after it.
                java.util.List<ReplayClips.Cue> early = clip.cues().stream().filter(cue -> (float) cue.at() < reveal)
                    .map(cue -> cue.until() != null && (float) cue.until().doubleValue() >= reveal
                        ? new ReplayClips.Cue(cue.at(), cue.sound(), cue.volume(), cue.loop(), null, null, cue.carry())
                        : cue)
                    .toList();
                if (before != null) {
                    assertEquals(before, early, clip.video() + " sounds different before the reveal");
                }
                before = early;
            }
        }
    }

    /** The steps a rule needs before its own: its earlier step, then the one right before. */
    private static java.util.List<String> before(ReplayClips.Rule rule) {
        java.util.List<String> ids = new java.util.ArrayList<>();
        if (rule.earlier() != null) {
            ids.add("01-" + rule.earlier());
        }
        if (rule.after() != null) {
            ids.add("02-" + rule.after());
        }
        return ids;
    }
}
