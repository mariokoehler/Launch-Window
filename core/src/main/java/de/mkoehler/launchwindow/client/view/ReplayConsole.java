package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.rules.MissionStepOutcome;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * The replay console's instruments for one step ({@code ui-spec.md} &sect;4.7.3, "The console"), apart
 * from any drawing: the status gauge's needle and peak-hold pointer, the three telemetry bars and
 * the CAUTION and MASTER ALARM lamps, as functions of the time into the step.
 * <p>
 * The rule everything follows: before the step's {@code revealAt} nothing depends on the outcome.
 * The needle wanders inside Nominal and the bars drift on phases drawn from a seed the caller must
 * not derive from the outcome. At {@code revealAt} the instruments tell the outcome together, in
 * the same moment as the screen's roll line and outcome box. A step that had already shown its
 * result when it was reached starts settled, with no swing.
 *
 * @author Mario Koehler
 */
public final class ReplayConsole {

    /** Where Nominal ends and Warning begins, on the gauge's 0 to 1 scale. */
    public static final float NOMINAL_END = 0.55f;
    /** Where Warning ends and Critical begins. */
    public static final float WARNING_END = 0.80f;
    /** The telemetry bars' two marks: below and above them is out of the usual band. */
    public static final float BAR_LOW_MARK = 0.35f;
    /** The telemetry bars' upper mark. */
    public static final float BAR_HIGH_MARK = 0.75f;
    /** The telemetry bars, top to bottom. */
    public static final String[] BARS = {"PRS", "TMP", "PWR"};

    /** The telemetry's blip, in {@link ReplaySounds}. */
    public static final String BLIP = "telemetry-blip";
    /** The CAUTION lamp's tone. */
    public static final String CAUTION_TONE = "caution-tone";
    /** The MASTER ALARM's loop. */
    public static final String MASTER_ALARM = "master-alarm";
    /** The static under a step with no signal. */
    public static final String STATIC = "no-signal-static";
    /** The click of Play and Pause. */
    public static final String HOLD_CLICK = "hold-click";

    /** The blips' period, in seconds (unconfirmed). */
    private static final float BLIP_PERIOD = 2f;
    /** How long the alarm sounds after the reveal; the lamp stays lit (unconfirmed). */
    private static final float ALARM_SOUNDS = 3f;
    /** The needle's rest before the reveal and after a success. */
    private static final float NOMINAL_REST = 0.30f;
    /** The wander's two waves (unconfirmed split of the mockup's 0.22 to 0.38). */
    private static final float SLOW_SWING = 0.055f;
    private static final float FAST_SWING = 0.025f;
    private static final float SLOW_PERIOD = 2.3f;
    private static final float FAST_PERIOD = 0.9f;
    private static final float BAR_REST = 0.55f;
    private static final float BAR_SWING = 0.10f;
    private static final float[] BAR_PERIODS = {3.1f, 2.7f, 3.7f};
    /** A step reached with its result shown counts as revealed this long ago. */
    private static final float SETTLED = 1000f;
    private static final float TAU = (float) (2 * Math.PI);
    /** The peak-hold's sampling step. */
    private static final float PEAK_STEP = 1f / 30f;

    /**
     * A range of the status gauge.
     */
    public enum Range {
        /** All well. */
        NOMINAL,
        /** A glitch. */
        WARNING,
        /** A failure. */
        CRITICAL
    }

    /**
     * How a telemetry bar shows.
     */
    public enum BarState {
        /** Cream, drifting. */
        NORMAL,
        /** Gold: out of band after a glitch. */
        WARNING,
        /** Red: out of band after a failure. */
        CRITICAL,
        /** Fallen to nothing after a catastrophic failure, the track outlined red. */
        LOST,
        /** No signal: no reading, the track dashed. */
        DEAD
    }

    private final MissionStepOutcome outcome;
    private final float revealAt;
    private final boolean revealedOnArrival;
    private final float slowPhase;
    private final float fastPhase;
    private final float[] barPhases = new float[BARS.length];
    private final float blipPhase;

    private ReplayConsole(long seed, MissionStepOutcome outcome, float revealAt, boolean revealedOnArrival) {
        this.outcome = outcome;
        this.revealAt = revealAt;
        this.revealedOnArrival = revealedOnArrival;
        Random random = new Random(seed);
        slowPhase = random.nextFloat() * TAU;
        fastPhase = random.nextFloat() * TAU;
        for (int i = 0; i < barPhases.length; i++) {
            barPhases[i] = random.nextFloat() * TAU;
        }
        blipPhase = random.nextFloat() * BLIP_PERIOD;
    }

    /**
     * A step with its own clip.
     *
     * @param seed              the step's seed; never derived from the outcome
     * @param outcome           the step's outcome
     * @param revealAt          when its result shows, in seconds into the step
     * @param revealedOnArrival whether the result had already shown when the step was reached
     * @return the instruments
     */
    public static ReplayConsole live(long seed, MissionStepOutcome outcome, float revealAt, boolean revealedOnArrival) {
        return new ReplayConsole(seed, outcome, revealAt, revealedOnArrival);
    }

    /**
     * A step with no clip: the gauge dead, the bars empty.
     *
     * @return the instruments
     */
    public static ReplayConsole noSignal() {
        return new ReplayConsole(0L, null, 0f, false);
    }

    /**
     * Returns the seed for a step: from the mission and the step's place, never the outcome.
     *
     * @param missionInstanceId the mission
     * @param step              the step, from 0
     * @return the seed
     */
    public static long seed(String missionInstanceId, int step) {
        return 31L * missionInstanceId.hashCode() + step;
    }

    /**
     * Returns whether the instruments read anything: {@code false} on a step with no signal.
     *
     * @return whether they do
     */
    public boolean live() {
        return outcome != null;
    }

    /**
     * Returns whether the outcome shows at a time.
     *
     * @param t seconds into the step
     * @return whether it does
     */
    public boolean revealed(float t) {
        return live() && sinceReveal(t) >= 0f;
    }

    private float sinceReveal(float t) {
        return revealedOnArrival ? SETTLED + t : t - revealAt;
    }

    /**
     * Returns the needle's reading.
     *
     * @param t seconds into the step
     * @return 0 to 1; 0 with no signal
     */
    public float needle(float t) {
        if (!live()) {
            return 0f;
        }
        float s = sinceReveal(t);
        if (s < 0f) {
            return NOMINAL_REST + wander(t, 1f);
        }
        float from = NOMINAL_REST + wander(revealAt, 1f);
        return switch (outcome) {
            case SUCCESSFUL -> mix(from, NOMINAL_REST + wander(t, 0.5f), easeOut(s / 0.6f));
            case MINOR_GLITCH -> mix(from, 0.66f + wander(t, 0.5f), easeOut(s / 0.4f));
            case MINOR_GLITCH_SOLVED -> {
                float scare = 0.68f + wander(t, 0.15f);
                if (s < 1.2f) {
                    yield mix(from, scare, easeOut(s / 0.4f));
                }
                yield mix(scare, NOMINAL_REST + wander(t, 0.5f), easeOut((s - 1.2f) / 0.8f));
            }
            case FAILURE -> mix(from, 0.88f + 0.02f * (float) Math.sin(TAU * 1.4f * t + slowPhase),
                easeOut(s / 0.5f));
            case CATASTROPHIC -> {
                if (s < 0.3f) {
                    float x = s / 0.3f;
                    yield mix(from, 1f, x * x);
                }
                if (s < 0.5f) {
                    yield 1f - 0.06f * (float) Math.sin(Math.PI * (s - 0.3f) / 0.2f);
                }
                if (s < 0.62f) {
                    yield 1f - 0.02f * (float) Math.sin(Math.PI * (s - 0.5f) / 0.12f);
                }
                yield 1f;
            }
        };
    }

    /**
     * Returns the peak-hold pointer: the highest reading so far in the step.
     *
     * @param t seconds into the step
     * @return 0 to 1, or a negative value for none (no signal)
     */
    public float peak(float t) {
        if (!live()) {
            return -1f;
        }
        float peak = needle(t);
        // Every swing is over 3 s after the reveal: no need to look further back than that.
        float window = Math.min(t, revealAt + 3f);
        for (float at = 0f; at < window; at += PEAK_STEP) {
            peak = Math.max(peak, needle(at));
        }
        if (revealedOnArrival) {
            // The swing happened before the step was reached: its trace stays.
            peak = Math.max(peak, switch (outcome) {
                case SUCCESSFUL -> 0f;
                case MINOR_GLITCH -> 0.70f;
                case MINOR_GLITCH_SOLVED -> 0.69f;
                case FAILURE -> 0.90f;
                case CATASTROPHIC -> 1f;
            });
        }
        return peak;
    }

    /**
     * Returns the range the outcome lit up, once it shows.
     *
     * @param t seconds into the step
     * @return the range, or {@code null} before the reveal or with no signal
     */
    public Range activeRange(float t) {
        if (!revealed(t)) {
            return null;
        }
        return switch (outcome) {
            case SUCCESSFUL, MINOR_GLITCH_SOLVED -> Range.NOMINAL;
            case MINOR_GLITCH -> Range.WARNING;
            case FAILURE, CATASTROPHIC -> Range.CRITICAL;
        };
    }

    /**
     * Returns a telemetry bar's reading.
     *
     * @param bar the bar, an index into {@link #BARS}
     * @param t   seconds into the step
     * @return 0 to 1
     */
    public float bar(int bar, float t) {
        if (!live()) {
            return 0f;
        }
        float drift = BAR_REST + BAR_SWING * (float) Math.sin(TAU * t / BAR_PERIODS[bar] + barPhases[bar]);
        float s = sinceReveal(t);
        if (s < 0f) {
            return drift;
        }
        float from = BAR_REST + BAR_SWING * (float) Math.sin(TAU * revealAt / BAR_PERIODS[bar] + barPhases[bar]);
        float calm = BAR_REST + 0.15f * (drift - BAR_REST);
        float settle = easeOut(s / 0.6f);
        return switch (outcome) {
            case SUCCESSFUL -> mix(from, calm, settle);
            case MINOR_GLITCH -> mix(from, bar == 1 ? 0.84f : calm, settle);
            case MINOR_GLITCH_SOLVED -> {
                if (bar != 1) {
                    yield mix(from, calm, settle);
                }
                if (s < 1.2f) {
                    yield mix(from, 0.84f, easeOut(s / 0.4f));
                }
                yield mix(0.84f, calm, easeOut((s - 1.2f) / 0.8f));
            }
            case FAILURE -> bar == 2 ? drift : mix(from, bar == 0 ? 0.06f : 0.90f, settle);
            case CATASTROPHIC -> mix(from, 0f, easeOut(s / 0.4f));
        };
    }

    /**
     * Returns how a telemetry bar shows.
     *
     * @param bar the bar, an index into {@link #BARS}
     * @param t   seconds into the step
     * @return the state
     */
    public BarState barState(int bar, float t) {
        if (!live()) {
            return BarState.DEAD;
        }
        if (!revealed(t)) {
            return BarState.NORMAL;
        }
        return switch (outcome) {
            case SUCCESSFUL -> BarState.NORMAL;
            case MINOR_GLITCH, MINOR_GLITCH_SOLVED -> bar == 1 && bar(bar, t) > BAR_HIGH_MARK ? BarState.WARNING
                : BarState.NORMAL;
            case FAILURE -> bar == 2 ? BarState.NORMAL : BarState.CRITICAL;
            case CATASTROPHIC -> BarState.LOST;
        };
    }

    /**
     * Returns whether the CAUTION lamp is lit: steady after a glitch, three flashes for one the
     * controllers saved, steady with the alarm after a catastrophic failure.
     *
     * @param t seconds into the step
     * @return whether it is
     */
    public boolean caution(float t) {
        if (!revealed(t)) {
            return false;
        }
        float s = sinceReveal(t);
        return switch (outcome) {
            case MINOR_GLITCH, CATASTROPHIC -> true;
            case MINOR_GLITCH_SOLVED -> s < 1.5f && s % 0.5f < 0.25f;
            case SUCCESSFUL, FAILURE -> false;
        };
    }

    /**
     * Returns whether the MASTER ALARM lamp is lit: steady after a failure, flashing after a
     * catastrophic one.
     *
     * @param t seconds into the step
     * @return whether it is
     */
    public boolean alarm(float t) {
        if (!revealed(t)) {
            return false;
        }
        return switch (outcome) {
            case FAILURE -> true;
            case CATASTROPHIC -> sinceReveal(t) % 0.6f < 0.3f;
            case SUCCESSFUL, MINOR_GLITCH, MINOR_GLITCH_SOLVED -> false;
        };
    }

    /**
     * Returns the sounds the console starts between two times: the telemetry's blips while a camera
     * is live (until telemetry is lost), and the CAUTION tone each time its lamp lights. A lamp
     * already lit when the step was reached stays quiet. The same rule as the lamps: nothing here
     * depends on the outcome before the reveal.
     *
     * @param from the time before, seconds into the step (excluded)
     * @param to   the time now (included)
     * @return the sounds' ids, in order
     */
    public List<String> sounds(float from, float to) {
        List<String> started = new ArrayList<>();
        if (!live() || to <= from) {
            return started;
        }
        for (float at = blipPhase + BLIP_PERIOD * (float) Math.floor((from - blipPhase) / BLIP_PERIOD + 1f);
             at <= to; at += BLIP_PERIOD) {
            if (at > from && at >= 0f && !(outcome == MissionStepOutcome.CATASTROPHIC && revealed(at))) {
                started.add(BLIP);
            }
        }
        if (!revealedOnArrival && caution(to) && !caution(from)) {
            started.add(CAUTION_TONE);
        }
        return started;
    }

    /**
     * Returns whether the MASTER ALARM sounds: for a while after a failure's reveal, while its lamp
     * stays lit; not on a step reached with its result shown.
     *
     * @param t seconds into the step
     * @return whether it does
     */
    public boolean alarmSounding(float t) {
        return revealed(t) && (outcome == MissionStepOutcome.FAILURE || outcome == MissionStepOutcome.CATASTROPHIC)
            && sinceReveal(t) < ALARM_SOUNDS;
    }

    /**
     * Formats the mission clock, as "T+ 0d 0h 2m 23s".
     *
     * @param seconds the mission time
     * @return the text
     */
    public static String clock(float seconds) {
        long total = (long) Math.max(0f, seconds);
        return String.format(Locale.ROOT, "T+ %dd %dh %dm %02ds", total / 86_400, total / 3_600 % 24, total / 60 % 60,
            total % 60);
    }

    private float wander(float t, float scale) {
        return scale * (SLOW_SWING * (float) Math.sin(TAU * t / SLOW_PERIOD + slowPhase)
            + FAST_SWING * (float) Math.sin(TAU * t / FAST_PERIOD + fastPhase));
    }

    private static float easeOut(float x) {
        float c = Math.max(0f, Math.min(1f, x));
        float inv = 1f - c;
        return 1f - inv * inv * inv;
    }

    private static float mix(float from, float to, float amount) {
        return from + (to - from) * amount;
    }
}
