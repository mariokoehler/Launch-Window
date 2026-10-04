package de.mkoehler.launchwindow.client.connect;

import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.net.messages.ClaimSlotResponse;

/**
 * What came of pressing Join on the Join Game screen ({@code ui-spec.md} &sect;4.2).
 *
 * @param kind    what happened
 * @param reason  the server's refusal reason, for {@link Kind#REFUSED}
 * @param message the server's or the network's words; empty for {@link Kind#JOINED}
 * @param state   the new player's starting state, for {@link Kind#JOINED}
 * @author Mario Koehler
 */
public record ClaimResult(Kind kind, ClaimSlotResponse.Reason reason, String message, PlayerSnapshot state) {

    /**
     * The outcomes of a slot claim.
     */
    public enum Kind {
        /** The slot is claimed and the starting state fetched. */
        JOINED,
        /** The server refused the claim; see the reason. */
        REFUSED,
        /** The new connection's login failed (the password changed, or the server's version did). */
        LOGIN_FAILED,
        /** No connection could be made. */
        UNREACHABLE,
        /** Connected, but the exchange broke off. */
        FAILED
    }

    /**
     * Creates a joined result.
     *
     * @param state the starting state
     * @return the result
     */
    public static ClaimResult joined(PlayerSnapshot state) {
        return new ClaimResult(Kind.JOINED, null, "", state);
    }

    /**
     * Creates a refusal.
     *
     * @param response the server's reply
     * @return the result
     */
    public static ClaimResult refused(ClaimSlotResponse response) {
        return new ClaimResult(Kind.REFUSED, response.reason(), response.message(), null);
    }

    /**
     * Creates a result that carries only a message.
     *
     * @param kind    {@link Kind#LOGIN_FAILED}, {@link Kind#UNREACHABLE} or {@link Kind#FAILED}
     * @param message what went wrong
     * @return the result
     */
    public static ClaimResult failure(Kind kind, String message) {
        return new ClaimResult(kind, null, message == null ? "" : message, null);
    }
}
