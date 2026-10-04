package de.mkoehler.launchwindow.net.messages;

/**
 * Asks the server to resolve the open turn right away, for play testing. Refused unless the
 * server runs in dev mode ({@code game-meta.json}'s {@code devMode}).
 *
 * @author Mario Koehler
 */
public record ResolveTurnNowRequest() {
}
