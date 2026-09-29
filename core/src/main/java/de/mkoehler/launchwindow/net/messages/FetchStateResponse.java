package de.mkoehler.launchwindow.net.messages;

import de.mkoehler.launchwindow.model.PlayerSnapshot;

/**
 * <p>The server's reply to a {@link FetchStateRequest}.</p>
 *
 * @param success whether the state was retrieved
 * @param message a human-readable message - empty on success, a rejection
 *                reason on failure
 * @param state   the player's current state, or {@code null} on failure
 * @author Mario Koehler
 */
public record FetchStateResponse(boolean success, String message, PlayerSnapshot state) {

    /**
     * Creates a successful response.
     *
     * @param state the player's current state
     * @return the successful response
     */
    public static FetchStateResponse success(PlayerSnapshot state) {
        return new FetchStateResponse(true, "", state);
    }

    /**
     * Creates a rejected response.
     *
     * @param message a human-readable rejection reason
     * @return the failed response
     */
    public static FetchStateResponse failure(String message) {
        return new FetchStateResponse(false, message, null);
    }
}
