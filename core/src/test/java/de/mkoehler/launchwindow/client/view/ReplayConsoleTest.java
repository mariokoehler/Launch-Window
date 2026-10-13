package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.rules.MissionStepOutcome;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ReplayConsole}: nothing tells the outcome before the reveal, each outcome settles
 * in its range, a step reached with its result shown starts settled, and no signal is dead.
 *
 * @author Mario Koehler
 */
class ReplayConsoleTest {

    private static final float REVEAL = 3.2f;
    private static final long SEED = ReplayConsole.seed("m-1", 4);

    @Test
    void nothingDependsOnTheOutcomeBeforeTheReveal() {
        ReplayConsole reference = ReplayConsole.live(SEED, MissionStepOutcome.SUCCESSFUL, REVEAL, false);
        for (MissionStepOutcome outcome : MissionStepOutcome.values()) {
            ReplayConsole console = ReplayConsole.live(SEED, outcome, REVEAL, false);
            for (float t = 0f; t < REVEAL; t += 0.05f) {
                assertEquals(reference.needle(t), console.needle(t), outcome + " at " + t);
                assertEquals(reference.peak(t), console.peak(t), outcome + " at " + t);
                for (int bar = 0; bar < ReplayConsole.BARS.length; bar++) {
                    assertEquals(reference.bar(bar, t), console.bar(bar, t), outcome + " at " + t);
                    assertEquals(ReplayConsole.BarState.NORMAL, console.barState(bar, t));
                }
                assertNull(console.activeRange(t));
                assertFalse(console.caution(t));
                assertFalse(console.alarm(t));
                float needle = console.needle(t);
                assertTrue(needle > 0.2f && needle < ReplayConsole.NOMINAL_END, outcome + " wanders in Nominal: " + needle);
            }
        }
    }

    @Test
    void eachOutcomeSettlesInItsRange() {
        float later = REVEAL + 4f;
        assertRange(MissionStepOutcome.SUCCESSFUL, later, 0f, ReplayConsole.NOMINAL_END);
        assertRange(MissionStepOutcome.MINOR_GLITCH_SOLVED, later, 0f, ReplayConsole.NOMINAL_END);
        assertRange(MissionStepOutcome.MINOR_GLITCH, later, ReplayConsole.NOMINAL_END, ReplayConsole.WARNING_END);
        assertRange(MissionStepOutcome.FAILURE, later, ReplayConsole.WARNING_END, 1f);
        assertEquals(1f, ReplayConsole.live(SEED, MissionStepOutcome.CATASTROPHIC, REVEAL, false).needle(later));
    }

    @Test
    void aSavedGlitchSwingsIntoWarningAndLeavesItsTrace() {
        ReplayConsole console = ReplayConsole.live(SEED, MissionStepOutcome.MINOR_GLITCH_SOLVED, REVEAL, false);
        float during = console.needle(REVEAL + 0.8f);
        assertTrue(during > ReplayConsole.NOMINAL_END && during < ReplayConsole.WARNING_END, "in Warning: " + during);
        assertTrue(console.caution(REVEAL + 0.1f));
        assertFalse(console.caution(REVEAL + 2f));
        assertTrue(console.peak(REVEAL + 4f) > ReplayConsole.NOMINAL_END);
        assertEquals(ReplayConsole.Range.NOMINAL, console.activeRange(REVEAL + 0.1f));
    }

    @Test
    void aStepReachedWithItsResultShownStartsSettled() {
        ReplayConsole console = ReplayConsole.live(SEED, MissionStepOutcome.FAILURE, REVEAL, true);
        assertEquals(ReplayConsole.Range.CRITICAL, console.activeRange(0f));
        assertTrue(console.needle(0f) > ReplayConsole.WARNING_END);
        assertTrue(console.alarm(0f));
        assertEquals(ReplayConsole.BarState.CRITICAL, console.barState(0, 0f));
        ReplayConsole saved = ReplayConsole.live(SEED, MissionStepOutcome.MINOR_GLITCH_SOLVED, REVEAL, true);
        assertTrue(saved.needle(0f) < ReplayConsole.NOMINAL_END);
        assertTrue(saved.peak(0f) > ReplayConsole.NOMINAL_END);
        assertFalse(saved.caution(0f));
    }

    @Test
    void noSignalIsDead() {
        ReplayConsole console = ReplayConsole.noSignal();
        assertFalse(console.live());
        assertEquals(0f, console.needle(5f));
        assertTrue(console.peak(5f) < 0f);
        assertNull(console.activeRange(5f));
        assertEquals(ReplayConsole.BarState.DEAD, console.barState(0, 5f));
        assertFalse(console.alarm(5f));
    }

    @Test
    void theSoundsTellNothingBeforeTheReveal() {
        ReplayConsole reference = ReplayConsole.live(SEED, MissionStepOutcome.SUCCESSFUL, REVEAL, false);
        for (MissionStepOutcome outcome : MissionStepOutcome.values()) {
            ReplayConsole console = ReplayConsole.live(SEED, outcome, REVEAL, false);
            for (float t = 0f; t + 1f / 60f < REVEAL; t += 1f / 60f) {
                assertEquals(reference.sounds(t, t + 1f / 60f), console.sounds(t, t + 1f / 60f), outcome + " at " + t);
                assertFalse(console.alarmSounding(t));
            }
        }
    }

    @Test
    void theConsoleSoundsItsLamps() {
        // A blip every 2 s, frame by frame or in one go.
        ReplayConsole success = ReplayConsole.live(SEED, MissionStepOutcome.SUCCESSFUL, REVEAL, false);
        assertEquals(java.util.Collections.nCopies(5, ReplayConsole.BLIP), success.sounds(-0.001f, 9.999f));
        assertEquals(5, frames(success, 10f, ReplayConsole.BLIP));
        assertEquals(1, frames(ReplayConsole.live(SEED, MissionStepOutcome.MINOR_GLITCH, REVEAL, false), 10f,
            ReplayConsole.CAUTION_TONE));
        assertEquals(3, frames(ReplayConsole.live(SEED, MissionStepOutcome.MINOR_GLITCH_SOLVED, REVEAL, false), 10f,
            ReplayConsole.CAUTION_TONE));
        ReplayConsole failure = ReplayConsole.live(SEED, MissionStepOutcome.FAILURE, REVEAL, false);
        assertTrue(failure.alarmSounding(REVEAL + 1f));
        assertFalse(failure.alarmSounding(REVEAL + 3.5f));
        assertTrue(failure.alarm(REVEAL + 3.5f));
        // Telemetry lost: no blips after a catastrophe's reveal.
        ReplayConsole lost = ReplayConsole.live(SEED, MissionStepOutcome.CATASTROPHIC, REVEAL, false);
        assertTrue(lost.sounds(REVEAL, REVEAL + 10f).isEmpty());
        // Reached with the result shown: the lamps are lit already, and quiet.
        ReplayConsole arrived = ReplayConsole.live(SEED, MissionStepOutcome.MINOR_GLITCH, REVEAL, true);
        assertEquals(0, frames(arrived, 10f, ReplayConsole.CAUTION_TONE));
        assertFalse(ReplayConsole.live(SEED, MissionStepOutcome.FAILURE, REVEAL, true).alarmSounding(0f));
        assertTrue(ReplayConsole.noSignal().sounds(-0.001f, 10f).isEmpty());
    }

    private static int frames(ReplayConsole console, float until, String sound) {
        int count = 0;
        for (float t = -0.001f; t < until; t += 1f / 60f) {
            count += (int) console.sounds(t, t + 1f / 60f).stream().filter(sound::equals).count();
        }
        return count;
    }

    @Test
    void theClockReadsLikeAMissionClock() {
        assertEquals("T+ 0d 0h 0m 08s", ReplayConsole.clock(8.7f));
        assertEquals("T+ 1d 2h 3m 04s", ReplayConsole.clock(86_400 + 7_200 + 180 + 4));
    }

    private static void assertRange(MissionStepOutcome outcome, float t, float low, float high) {
        float needle = ReplayConsole.live(SEED, outcome, REVEAL, false).needle(t);
        assertTrue(needle > low && needle < high, outcome + ": " + needle);
    }
}
