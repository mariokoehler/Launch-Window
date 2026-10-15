package de.mkoehler.launchwindow.client.view;

/**
 * The replay's playback as a film ({@code ui-spec.md} &sect;4.7.3), apart from any screen: which step
 * shows, how far into it, which results have shown, and whether it moves on by itself.
 * <ul>
 *   <li>A step's result shows once its clip reaches {@code revealAt}; a step reached before shows
 *   at once. Results show in order, so they're a count from the first step.</li>
 *   <li>Playing, the next step starts when a step's time is up; after the last, the season summary,
 *   once the last step's sound has finished (a call mid-sentence isn't cut off by the fanfare). Paused,
 *   the step's clip still plays through and its result shows; only the move on waits.</li>
 *   <li>Going forward by hand (next step, a later step in the list, the summary) shows the results
 *   of the steps passed.</li>
 * </ul>
 *
 * @author Mario Koehler
 */
public final class ReplayPlayback {

    /**
     * The steps' timings.
     */
    public interface Timing {

        /**
         * Returns when a step's result shows.
         *
         * @param step the step, from 0
         * @return seconds into the step
         */
        float revealAt(int step);

        /**
         * Returns how long a step lasts while playing.
         *
         * @param step the step, from 0
         * @return seconds
         */
        float duration(int step);
    }

    /**
     * What a call to {@link #update} changed, for the screen to redraw.
     */
    public enum Change {
        /** Nothing the screen shows. */
        NONE,
        /** The step's result now shows. */
        REVEALED,
        /** Another step, or the summary, now shows. */
        MOVED
    }

    /**
     * How long, at most, the summary waits for the last step's sound once its clip is over, in seconds:
     * a stream that never reports its end can't hold the replay.
     */
    public static final float SOUND_WAIT = 8f;

    private final int steps;
    private final Timing timing;
    private int step;
    private float time;
    private int revealed;
    private boolean playing;

    /**
     * Starts at the first step.
     *
     * @param steps   the steps flown
     * @param timing  their timings
     * @param playing whether it plays by itself from the start
     */
    public ReplayPlayback(int steps, Timing timing, boolean playing) {
        this.steps = steps;
        this.timing = timing;
        this.playing = playing;
    }

    /**
     * Returns the step showing, from 0; the step count means the season summary.
     *
     * @return the step
     */
    public int step() {
        return step;
    }

    /**
     * Returns whether the season summary shows.
     *
     * @return whether it does
     */
    public boolean atSummary() {
        return step >= steps;
    }

    /**
     * Returns how far into the step it is.
     *
     * @return seconds
     */
    public float time() {
        return time;
    }

    /**
     * Returns whether it moves on by itself.
     *
     * @return whether it plays
     */
    public boolean playing() {
        return playing;
    }

    /**
     * Returns whether a step's result shows.
     *
     * @param index the step, from 0
     * @return whether it does
     */
    public boolean revealed(int index) {
        return index < revealed;
    }

    /**
     * Plays or pauses: only the move on to the next step waits.
     */
    public void togglePlaying() {
        playing = !playing;
    }

    /**
     * Shows a step from its start, or the summary for the step count; going forward shows the
     * results passed.
     *
     * @param index the step, from 0
     */
    public void show(int index) {
        int target = Math.max(0, Math.min(index, steps));
        revealed = Math.max(revealed, target);
        step = target;
        time = 0f;
    }

    /**
     * Moves the clock on, with no sound to wait for.
     *
     * @param delta seconds since the last call
     * @return what changed
     */
    public Change update(float delta) {
        return update(delta, false);
    }

    /**
     * Moves the clock on.
     *
     * @param delta    seconds since the last call
     * @param sounding whether the step's sound is still going (a radio call mid-sentence): after the
     *                 last step the summary, and its fanfare, wait for it, up to {@link #SOUND_WAIT}
     * @return what changed
     */
    public Change update(float delta, boolean sounding) {
        if (atSummary()) {
            return Change.NONE;
        }
        time += delta;
        Change change = Change.NONE;
        if (!revealed(step) && time >= timing.revealAt(step)) {
            revealed = step + 1;
            change = Change.REVEALED;
        }
        float end = timing.duration(step);
        boolean waiting = step == steps - 1 && sounding && time < end + SOUND_WAIT;
        if (playing && time >= end && !waiting) {
            show(step + 1);
            return Change.MOVED;
        }
        return change;
    }
}
