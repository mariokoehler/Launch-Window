package de.mkoehler.launchwindow.client.ui;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.utils.TimeUtils;
import de.mkoehler.launchwindow.client.LaunchWindowGame;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.DoubleSupplier;

/**
 * The client's small feedback effects ("juice", {@code design.md} &sect;5, Backlog): one switch for
 * all motion, the numbers that tick to a new value, and the attention dots that pulse when they first
 * appear. Every effect plays once, on a first show or a real change of value, never on a rebuild, so
 * what it remembers lives here, across screens, rather than in a widget that a rebuild throws away.
 *
 * <p>With motion off (the player's Animations setting) every effect shows its end at once. Frozen
 * ({@link #freeze}, for {@code ScreenSnapshot}, where no time passes) it also remembers nothing, so a
 * shot never depends on the shots before it.
 *
 * @author Mario Koehler
 */
public final class Juice {

    /** How long a number takes to tick to its new value, in seconds. Unconfirmed. */
    public static final float TICK_SECONDS = 0.6f;
    /** How long a changed number's flash takes to fade, in seconds. Unconfirmed. */
    public static final float FLASH_SECONDS = 1.0f;
    /** How long one pulse of an attention dot lasts, in seconds. Unconfirmed. */
    public static final float PULSE_SECONDS = 0.6f;
    /** How many times an attention dot pulses. Unconfirmed. */
    public static final int PULSES = 3;
    /** How much bigger an attention dot grows at a pulse's peak. Unconfirmed. */
    public static final float PULSE_GROWTH = 0.5f;

    private static final DoubleSupplier SYSTEM_CLOCK = () -> TimeUtils.nanoTime() / 1e9;

    private static DoubleSupplier clock = SYSTEM_CLOCK;
    private static boolean frozen;
    private static final Map<String, Tick> TICKS = new HashMap<>();
    private static String ticksContext = "";
    private static final Map<String, Double> PULSE_STARTS = new HashMap<>();

    private Juice() {
    }

    /**
     * Freezes every effect at its end and stops remembering anything: for {@code ScreenSnapshot},
     * which renders with no time passing.
     *
     * @param on whether to freeze
     */
    public static void freeze(boolean on) {
        frozen = on;
        forget();
    }

    /**
     * Replaces the clock, so the snapshot tool and the tests can step time by hand; {@code null} puts
     * the system clock back.
     *
     * @param seconds the clock, in seconds
     */
    public static void useClock(DoubleSupplier seconds) {
        clock = seconds != null ? seconds : SYSTEM_CLOCK;
    }

    /**
     * Returns the effects' clock.
     *
     * @return now, in seconds
     */
    public static double now() {
        return clock.getAsDouble();
    }

    /**
     * Returns whether effects move: not frozen and the player's Animations setting on.
     *
     * @return whether to animate
     */
    public static boolean motion() {
        return !frozen && LaunchWindowGame.animationsOn();
    }

    /**
     * Forgets every number and dot, as on entering a game.
     */
    public static void forget() {
        TICKS.clear();
        ticksContext = "";
        PULSE_STARTS.clear();
    }

    // ---------------------------------------------------------------- ticking numbers

    /**
     * A number's tick from the value shown last to its new one.
     */
    public static final class Tick {
        private double from;
        private long to;
        private double start = Double.NEGATIVE_INFINITY;
        private int direction;

        private Tick(long value) {
            from = value;
            to = value;
        }

        /**
         * Returns the value to show now.
         *
         * @return the value, between the old and the new
         */
        public long value() {
            return Math.round(exactValue(now()));
        }

        private double exactValue(double now) {
            float progress = progress(now, TICK_SECONDS);
            return progress >= 1f ? to : from + (to - from) * Interpolation.pow2Out.apply(progress);
        }

        /**
         * Returns the new value.
         *
         * @return the value it ticks to
         */
        public long target() {
            return to;
        }

        /**
         * Returns the value it ticks from.
         *
         * @return the start value, rounded
         */
        public long origin() {
            return Math.round(from);
        }

        /**
         * Returns whether it is still ticking.
         *
         * @return whether moving
         */
        public boolean ticking() {
            return progress(now(), TICK_SECONDS) < 1f;
        }

        /**
         * Returns how strong the flash is now: 1 at the change, fading to 0.
         *
         * @return the flash's strength
         */
        public float flash() {
            return direction == 0 ? 0f : 1f - Interpolation.pow2In.apply(progress(now(), FLASH_SECONDS));
        }

        /**
         * Returns which way the value went: 1 up, -1 down, 0 not at all.
         *
         * @return the direction
         */
        public int direction() {
            return direction;
        }

        private float progress(double now, float seconds) {
            return (float) Math.min(1.0, Math.max(0.0, (now - start) / seconds));
        }
    }

    /**
     * Returns the tick for a number shown under a key, started when the value differs from the one
     * shown last. A changed context (another agency, another turn) shows the new value at once: a
     * number ticks for an order, never for a turn's results, which would give them away before the
     * report.
     *
     * @param key     the number's key, such as {@code funds}
     * @param context who and when it is shown for
     * @param value   the value to show
     * @return the tick
     */
    public static Tick tick(String key, String context, long value) {
        if (frozen) {
            return new Tick(value);
        }
        if (!context.equals(ticksContext)) {
            TICKS.clear();
            ticksContext = context;
        }
        Tick tick = TICKS.get(key);
        if (tick == null || !motion()) {
            tick = new Tick(value);
            TICKS.put(key, tick);
            return tick;
        }
        if (tick.to != value) {
            double now = now();
            tick.from = tick.exactValue(now);
            tick.direction = Long.signum(value - Math.round(tick.from));
            tick.to = value;
            tick.start = now;
        }
        return tick;
    }

    // ---------------------------------------------------------------- attention dots

    /**
     * Returns how big an attention dot draws now: it pulses {@value #PULSES} times the first time it
     * shows, then stays at 1. Call it every frame the dot shows; {@link #retainPulses} forgets the
     * dots that went away, so one that comes back pulses again.
     *
     * @param key the dot's key, such as {@code hub:idle:VAB}
     * @return the scale, 1 at rest
     */
    public static float pulse(String key) {
        if (!motion()) {
            return 1f;
        }
        double now = now();
        double start = PULSE_STARTS.computeIfAbsent(key, k -> now);
        double elapsed = now - start;
        if (elapsed >= PULSES * PULSE_SECONDS || elapsed < 0) {
            return 1f;
        }
        float phase = (float) (elapsed % PULSE_SECONDS / PULSE_SECONDS);
        return 1f + PULSE_GROWTH * (float) Math.sin(Math.PI * phase);
    }

    /**
     * Forgets the dots under a prefix that aren't showing any more.
     *
     * @param prefix  the prefix, such as {@code hub:}
     * @param showing the keys showing now
     */
    public static void retainPulses(String prefix, Set<String> showing) {
        PULSE_STARTS.keySet().removeIf(key -> key.startsWith(prefix) && !showing.contains(key));
    }

    /**
     * Forgets the dots under a prefix whose pulse hasn't finished, as when the player leaves before
     * seeing it: they pulse again next time.
     *
     * @param prefix the prefix
     */
    public static void abandonPulses(String prefix) {
        double now = now();
        Iterator<Map.Entry<String, Double>> entries = PULSE_STARTS.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<String, Double> entry = entries.next();
            if (entry.getKey().startsWith(prefix) && now - entry.getValue() < PULSES * PULSE_SECONDS) {
                entries.remove();
            }
        }
    }
}
