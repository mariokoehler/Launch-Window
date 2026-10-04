package de.mkoehler.launchwindow.client.connect;

import de.mkoehler.launchwindow.client.ui.Countdown;
import de.mkoehler.launchwindow.net.messages.GameInfoResponse;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the client's countdown: {@link GameInfo} against the server's clock, and the
 * {@link Countdown} wording.
 *
 * @author Mario Koehler
 */
class GameInfoTest {

    private static final long HOUR = 3_600_000L;

    private static GameInfoResponse info(long next, long serverNow) {
        return GameInfoResponse.success("early-era", "hash", List.of(), 1, false, next, serverNow, null, List.of(),
            List.of(), 0);
    }

    /**
     * The countdown runs on server time: a client clock that's an hour off still counts down to the
     * right moment, and time held since the reply counts.
     */
    @Test
    void theCountdownRunsOnServerTime() {
        GameInfo held = new GameInfo(info(10 * HOUR, 4 * HOUR), 5 * HOUR);
        assertEquals(OptionalLong.of(6 * HOUR), held.millisUntilNextResolution(5 * HOUR));
        assertEquals(OptionalLong.of(5 * HOUR), held.millisUntilNextResolution(6 * HOUR));
        assertEquals(OptionalLong.of(0), held.millisUntilNextResolution(20 * HOUR));
        assertEquals(OptionalLong.empty(), new GameInfo(info(-1, 0), 0).millisUntilNextResolution(0));
    }

    /**
     * The wording picks the two largest units.
     */
    @Test
    void theWordingPicksTwoUnits() {
        assertEquals("Next turn in 6 h 12 m", Countdown.nextTurn(OptionalLong.of(6 * HOUR + 12 * 60_000L + 30_000L)));
        assertEquals("Next turn in 2 d 4 h", Countdown.nextTurn(OptionalLong.of(52 * HOUR)));
        assertEquals("Next turn in 45 m", Countdown.nextTurn(OptionalLong.of(45 * 60_000L)));
        assertEquals("Next turn in under a minute", Countdown.nextTurn(OptionalLong.of(5_000L)));
        assertEquals("Turn resolving now", Countdown.nextTurn(OptionalLong.of(0)));
        assertEquals("No turn scheduled", Countdown.nextTurn(OptionalLong.empty()));
    }
}
