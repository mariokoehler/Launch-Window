package de.mkoehler.launchwindow.net.messages;

/**
 * Asks for one of the logged-in player's turn reports ({@code ui-spec.md} &sect;4.4, gap G2). Always
 * the requesting account's own; the server never reads another player's reports.
 *
 * @param turn the turn, or a negative number for the latest
 * @author Mario Koehler
 */
public record TurnReportRequest(long turn) {
}
