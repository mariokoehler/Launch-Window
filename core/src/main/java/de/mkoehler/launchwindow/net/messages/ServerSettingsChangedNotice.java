package de.mkoehler.launchwindow.net.messages;

/**
 * Sent by the server, unasked, to every logged-in connection once the admin has saved the server's settings
 * ({@code ui-spec.md} &sect;4.22, G21), so clients fetch the game info again (the countdown, the Dev button).
 *
 * @author Mario Koehler
 */
public record ServerSettingsChangedNotice() {
}
