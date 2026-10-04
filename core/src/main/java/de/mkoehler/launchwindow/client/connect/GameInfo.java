package de.mkoehler.launchwindow.client.connect;

import de.mkoehler.launchwindow.net.messages.GameInfoResponse;

import java.util.Objects;
import java.util.OptionalLong;

/**
 * The game info as the client holds it: the server's reply plus when, by the client's own clock,
 * it arrived. Countdowns run from the server's clock reading in the reply, advanced by however long
 * the client has held it, so a client whose clock is off still counts down to the right moment.
 *
 * @param response         the server's reply
 * @param receivedAtMillis the client's clock when the reply arrived
 * @author Mario Koehler
 */
public record GameInfo(GameInfoResponse response, long receivedAtMillis) {

    /**
     * Checks the reply is there.
     *
     * @throws NullPointerException if it isn't
     */
    public GameInfo {
        Objects.requireNonNull(response, "response must not be null");
    }

    /**
     * Returns how long until the open turn resolves.
     *
     * @param clientNowMillis the client's clock now
     * @return the time left in milliseconds (0 once it's due), or empty if no resolution is scheduled
     */
    public OptionalLong millisUntilNextResolution(long clientNowMillis) {
        if (response.nextResolutionMillis() < 0) {
            return OptionalLong.empty();
        }
        long serverNow = response.serverNowMillis() + (clientNowMillis - receivedAtMillis);
        return OptionalLong.of(Math.max(0L, response.nextResolutionMillis() - serverNow));
    }
}
