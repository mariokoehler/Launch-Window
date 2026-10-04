package de.mkoehler.launchwindow.client.connect;

import de.mkoehler.launchwindow.model.PlayerSnapshot;

/**
 * What came of an action submit or a state refresh once the player is in the game.
 *
 * @param kind     what happened
 * @param message  for {@link Kind#REFUSED}, the server's refusal code (such as {@code ROSTER_FULL},
 *                 {@code ui-spec.md} &sect;5); for the failures, what went wrong; for {@link Kind#OK},
 *                 the server's outcome code
 * @param state    the player's state afterwards, with today's actions applied; {@code null} if it
 *                 couldn't be fetched (an applied action whose follow-up fetch failed: refresh)
 * @param gameInfo the game info fetched with it, or {@code null}
 * @author Mario Koehler
 */
public record ServerResult(Kind kind, String message, PlayerSnapshot state, GameInfo gameInfo) {

    /**
     * The outcomes of a call.
     */
    public enum Kind {
        /** The action was applied, or the refresh succeeded. */
        OK,
        /** The server refused the action; {@code message} is the refusal code. */
        REFUSED,
        /** The session's connection had to be reopened and the login failed. */
        LOGIN_FAILED,
        /** No connection could be made. */
        UNREACHABLE,
        /** Connected, but the exchange broke off. */
        FAILED
    }

    /**
     * Creates a result that carries only a message.
     *
     * @param kind    the kind
     * @param message the message
     * @return the result
     */
    public static ServerResult of(Kind kind, String message) {
        return new ServerResult(kind, message == null ? "" : message, null, null);
    }
}
