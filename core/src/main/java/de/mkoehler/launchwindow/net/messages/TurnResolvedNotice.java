package de.mkoehler.launchwindow.net.messages;

/**
 * Sent by the server, unasked, to every logged-in connection the moment a turn has
 * resolved, so players waiting online see it at once rather than at their next poll.
 * The client fetches the new state and game info itself.
 *
 * @param resolvedTurn the game turn that resolved
 * @author Mario Koehler
 */
public record TurnResolvedNotice(long resolvedTurn) {
}
