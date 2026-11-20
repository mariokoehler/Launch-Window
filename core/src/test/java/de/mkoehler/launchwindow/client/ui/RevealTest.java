package de.mkoehler.launchwindow.client.ui;

import de.mkoehler.launchwindow.client.LaunchWindowGame;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An entrance's clock: parts start at their delays, a click finishes every one, a beat comes once and
 * never after a skip, and a report seen before shows its end at once.
 */
class RevealTest {

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
    void partsPlayFromTheirDelays() {
        Reveal reveal = Reveal.play();
        assertEquals(0f, reveal.progress(0.5f, 1f), 1e-6);
        now += 1.0;
        assertEquals(0.5f, reveal.progress(0.5f, 1f), 1e-6);
        now += 1.0;
        assertEquals(1f, reveal.progress(0.5f, 1f), 1e-6);
    }

    @Test
    void aClickFinishesEverything() {
        Reveal reveal = Reveal.play();
        reveal.skip();
        assertEquals(1f, reveal.progress(0.5f, 1f), 1e-6);
        now += 1.0;
        assertFalse(reveal.beat("stamp", 0.5f), "a skipped entrance makes no sound");
    }

    @Test
    void aBeatComesOnce() {
        Reveal reveal = Reveal.play();
        assertFalse(reveal.beat("stamp", 0.5f));
        now += 0.6;
        assertTrue(reveal.beat("stamp", 0.5f));
        assertFalse(reveal.beat("stamp", 0.5f));
    }

    @Test
    void noEntranceShowsTheEnd() {
        assertEquals(1f, Reveal.none().progress(0.5f, 1f), 1e-6);
        LaunchWindowGame.useAnimationsInMemory(false);
        assertEquals(1f, Reveal.play().progress(0.5f, 1f), 1e-6);
    }
}
