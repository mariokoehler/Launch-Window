package de.mkoehler.launchwindow.client.view;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ReplayPlayback}: results show at their time, playing moves on, pausing only
 * holds the move, and going forward by hand shows what it passes.
 *
 * @author Mario Koehler
 */
class ReplayPlaybackTest {

    /** Every step: the result at 1 s, the move on at 3 s. */
    private static final ReplayPlayback.Timing TIMING = new ReplayPlayback.Timing() {
        @Override
        public float revealAt(int step) {
            return 1f;
        }

        @Override
        public float duration(int step) {
            return 3f;
        }
    };

    /**
     * Playing, a step's result shows at its time and the next step starts when it's up; after the
     * last step comes the summary, and nothing changes there.
     */
    @Test
    void playsThroughToTheSummary() {
        ReplayPlayback playback = new ReplayPlayback(2, TIMING, true);
        assertEquals(ReplayPlayback.Change.NONE, playback.update(0.9f));
        assertFalse(playback.revealed(0));
        assertEquals(ReplayPlayback.Change.REVEALED, playback.update(0.2f));
        assertTrue(playback.revealed(0));
        assertEquals(ReplayPlayback.Change.MOVED, playback.update(2f));
        assertEquals(1, playback.step());
        assertFalse(playback.revealed(1), "the new step starts unrevealed");
        playback.update(3f);
        assertTrue(playback.atSummary());
        assertTrue(playback.revealed(1));
        assertEquals(ReplayPlayback.Change.NONE, playback.update(5f));
    }

    /**
     * Paused, the step still plays and shows its result, but doesn't move on until playing again.
     */
    @Test
    void pausedShowsTheResultButWaits() {
        ReplayPlayback playback = new ReplayPlayback(2, TIMING, false);
        assertEquals(ReplayPlayback.Change.REVEALED, playback.update(1.5f));
        assertEquals(ReplayPlayback.Change.NONE, playback.update(10f));
        assertEquals(0, playback.step());
        playback.togglePlaying();
        assertEquals(ReplayPlayback.Change.MOVED, playback.update(0.01f));
        assertEquals(1, playback.step());
    }

    /**
     * After the last step, the summary waits while the step's sound goes on, and comes once it's done;
     * between steps nothing waits (a call runs on into the next step).
     */
    @Test
    void theSummaryWaitsForTheSound() {
        ReplayPlayback playback = new ReplayPlayback(2, TIMING, true);
        assertEquals(ReplayPlayback.Change.MOVED, playback.update(3f, true), "a step doesn't wait for sound");
        playback.update(3f, true);
        assertEquals(1, playback.step(), "the last step holds while a call talks");
        assertEquals(ReplayPlayback.Change.NONE, playback.update(2f, true));
        assertEquals(ReplayPlayback.Change.MOVED, playback.update(0.01f, false));
        assertTrue(playback.atSummary());
    }

    /**
     * A sound that never ends holds the summary back no longer than {@link ReplayPlayback#SOUND_WAIT}.
     */
    @Test
    void theWaitForSoundEnds() {
        ReplayPlayback playback = new ReplayPlayback(1, TIMING, true);
        playback.update(3f + ReplayPlayback.SOUND_WAIT - 0.1f, true);
        assertFalse(playback.atSummary());
        assertEquals(ReplayPlayback.Change.MOVED, playback.update(0.2f, true));
        assertTrue(playback.atSummary());
    }

    /**
     * Next step, a later step and the summary show the results passed; going back keeps them, and
     * the step shown again starts from its beginning.
     */
    @Test
    void goingForwardShowsWhatItPasses() {
        ReplayPlayback playback = new ReplayPlayback(4, TIMING, true);
        playback.show(2);
        assertTrue(playback.revealed(0));
        assertTrue(playback.revealed(1));
        assertFalse(playback.revealed(2));
        playback.show(0);
        assertTrue(playback.revealed(1), "going back keeps what showed");
        assertEquals(0f, playback.time());
        playback.show(99);
        assertTrue(playback.atSummary());
        assertTrue(playback.revealed(3));
    }
}
