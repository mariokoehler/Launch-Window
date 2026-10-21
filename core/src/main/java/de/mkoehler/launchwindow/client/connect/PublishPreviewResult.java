package de.mkoehler.launchwindow.client.connect;

/**
 * What a preview of publishing a finding came to ({@code design.md} &sect;2.13).
 *
 * @param ok       whether the answer came back
 * @param message  the server's or connection's reason, or {@code ""}
 * @param prestige the prestige publishing now would pay, when {@code ok}
 * @param helped   the rivals that don't know the finding
 * @param rivals   every rival in the game
 * @author Mario Koehler
 */
public record PublishPreviewResult(boolean ok, String message, long prestige, int helped, int rivals) {
}
