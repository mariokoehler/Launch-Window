package de.mkoehler.launchwindow.net.messages;

/**
 * Sent by the server, unasked, to every logged-in connection once the admin has started a new game
 * ({@link StartNewGameRequest}): the agency the client shows is gone, so it goes back to Connect, where logging in
 * leads to founding a new one.
 *
 * @author Mario Koehler
 */
public record NewGameStartedNotice() {
}
