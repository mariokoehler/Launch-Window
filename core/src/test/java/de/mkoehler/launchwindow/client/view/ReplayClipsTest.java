package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.rules.MissionStepOutcome;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

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
                ReplayClips.Clip clip = CLIPS.clipFor("03-" + rule.step(), outcome);
                assertTrue(clip.signal(), rule.clip() + " has no " + ReplayClips.variant(outcome));
                assertTrue(Files.exists(ASSETS.resolve(clip.video())), clip.video());
                assertTrue(clip.revealAt() > 0 && clip.revealAt() <= clip.duration(), clip.video());
            }
        }
        assertTrue(Files.exists(ASSETS.resolve(ReplayClips.noSignal().video())));
        assertTrue(Files.exists(ASSETS.resolve(ReplayClips.noSignal().still())));
    }

    /**
     * A step matches by its kind, its number and any vehicle in front dropped; nothing else matches.
     */
    @Test
    void matchesStepsByKind() {
        assertEquals("launch", CLIPS.clipName("03-launch").orElseThrow());
        assertEquals("launch", CLIPS.clipName("09-gemini-b-launch").orElseThrow());
        assertEquals("launch", CLIPS.clipName("03-atv-launch").orElseThrow());
        assertTrue(CLIPS.clipName("01-launch-preparations").isEmpty());
        assertTrue(CLIPS.clipName("03-launch-from-drop-plane").isEmpty());
    }

    /**
     * Both glitches play the glitch; a catastrophe plays the failure while the step has no
     * catastrophic clip; a step without a clip plays no signal.
     */
    @Test
    void picksTheVariant() {
        assertTrue(CLIPS.clipFor("03-launch", MissionStepOutcome.MINOR_GLITCH_SOLVED).video().endsWith("launch-glitch.webm"));
        assertTrue(CLIPS.clipFor("03-launch", MissionStepOutcome.CATASTROPHIC).video().endsWith("launch-failure.webm"));
        ReplayClips.Clip none = CLIPS.clipFor("04-ascent", MissionStepOutcome.SUCCESSFUL);
        assertFalse(none.signal());
        assertTrue(none.loop());
        assertEquals(ReplayClips.NO_SIGNAL_REVEAL, none.revealAt());
    }
}
