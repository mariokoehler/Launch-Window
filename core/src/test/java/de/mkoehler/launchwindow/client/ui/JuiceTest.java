package de.mkoehler.launchwindow.client.ui;

import de.mkoehler.launchwindow.client.LaunchWindowGame;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The juice's memory: a number ticks only for a real change in the same context, and a dot pulses
 * once, again only after it went away or its pulse was cut short.
 */
class JuiceTest {

    private double now;

    @BeforeEach
    void setUp() {
        LaunchWindowGame.useAnimationsInMemory(true);
        Juice.freeze(false);
        Juice.useClock(() -> now);
    }

    @AfterEach
    void tearDown() {
        Juice.useClock(null);
        Juice.forget();
    }

    @Test
    void aNumberTicksFromTheValueShownLast() {
        assertFalse(Juice.tick("funds", "a|1", 1000).ticking());
        Juice.Tick tick = Juice.tick("funds", "a|1", 600);
        assertTrue(tick.ticking());
        assertEquals(1000, tick.value());
        assertEquals(-1, tick.direction());
        assertEquals(1f, tick.flash(), 1e-6);
        now += Juice.TICK_SECONDS / 2;
        long midway = tick.value();
        assertTrue(midway < 1000 && midway > 600, "midway " + midway);
        now += Juice.FLASH_SECONDS;
        assertEquals(600, tick.value());
        assertFalse(tick.ticking());
        assertEquals(0f, tick.flash(), 1e-6);
    }

    @Test
    void aRebuildWithTheSameValueKeepsTicking() {
        Juice.tick("funds", "a|1", 1000);
        Juice.tick("funds", "a|1", 1500);
        now += Juice.TICK_SECONDS / 2;
        Juice.Tick again = Juice.tick("funds", "a|1", 1500);
        assertTrue(again.ticking());
        assertTrue(again.value() > 1000 && again.value() < 1500);
    }

    @Test
    void anotherTurnShowsTheNewValueAtOnce() {
        Juice.tick("funds", "a|1", 1000);
        Juice.Tick tick = Juice.tick("funds", "a|2", 4000);
        assertFalse(tick.ticking());
        assertEquals(4000, tick.value());
        assertEquals(0, tick.direction());
    }

    @Test
    void withMotionOffNothingTicks() {
        Juice.tick("funds", "a|1", 1000);
        LaunchWindowGame.useAnimationsInMemory(false);
        Juice.Tick tick = Juice.tick("funds", "a|1", 600);
        assertEquals(600, tick.value());
        assertFalse(tick.ticking());
        assertEquals(1f, Juice.pulse("hub:x"));
    }

    @Test
    void frozenShowsTheValueAndRemembersNothing() {
        Juice.freeze(true);
        Juice.tick("funds", "a|1", 1000);
        Juice.Tick tick = Juice.tick("funds", "a|1", 600);
        assertEquals(600, tick.value());
        assertFalse(tick.ticking());
    }

    @Test
    void aDotPulsesOnceThenRests() {
        assertEquals(1f, Juice.pulse("hub:a"), 1e-6);
        now += Juice.PULSE_SECONDS / 2;
        assertEquals(1f + Juice.PULSE_GROWTH, Juice.pulse("hub:a"), 1e-4);
        now += Juice.PULSES * Juice.PULSE_SECONDS;
        assertEquals(1f, Juice.pulse("hub:a"), 1e-6);
        now += Juice.PULSE_SECONDS / 2;
        assertEquals(1f, Juice.pulse("hub:a"), 1e-6);
    }

    @Test
    void aDotThatWentAwayPulsesAgain() {
        Juice.pulse("hub:a");
        now += Juice.PULSES * Juice.PULSE_SECONDS + 1;
        Juice.retainPulses("hub:", Set.of());
        Juice.pulse("hub:a");
        now += Juice.PULSE_SECONDS / 2;
        assertTrue(Juice.pulse("hub:a") > 1.4f);
    }

    @Test
    void aPulseCutShortPlaysAgain() {
        Juice.pulse("hub:a");
        Juice.pulse("hub:b");
        now += Juice.PULSES * Juice.PULSE_SECONDS + 1;
        Juice.pulse("hub:c");
        now += 0.1;
        Juice.abandonPulses("hub:");
        // a and b finished and stay seen; c was cut short and starts over.
        now += Juice.PULSE_SECONDS / 2;
        assertEquals(1f, Juice.pulse("hub:a"), 1e-6);
        assertEquals(1f, Juice.pulse("hub:c"), 1e-6);
        now += Juice.PULSE_SECONDS / 2;
        assertTrue(Juice.pulse("hub:c") > 1.4f);
    }
}
