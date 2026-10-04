package de.mkoehler.launchwindow.client.connect;

import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.net.messages.ClaimSlotRequest;
import de.mkoehler.launchwindow.net.messages.FetchStateResponse;
import de.mkoehler.launchwindow.net.messages.HandshakeResponse;

import java.util.Optional;

/**
 * What came of pressing Connect: the player is in the game, needs to claim a slot first, or
 * one of the refusals the Connect screen shows ({@code ui-spec.md} &sect;4.1).
 *
 * <p>The handshake's shape is pinned ({@code HandshakeRequest}'s own doc), so it carries no
 * reason code. The cases are told apart without one: a version mismatch is the server reporting
 * a different version than ours, a bad login is caught on the client before sending
 * ({@code LoginNames}), and any other refusal is shown with the server's own words.</p>
 *
 * @param kind          what happened
 * @param message       the server's or the network's words, for {@link Kind#REJECTED},
 *                      {@link Kind#UNREACHABLE} and {@link Kind#FAILED}; empty otherwise
 * @param clientVersion this client's version, for {@link Kind#VERSION_MISMATCH}
 * @param serverVersion the server's version, for {@link Kind#VERSION_MISMATCH}
 * @param state         the player's state with today's actions applied, for {@link Kind#IN_GAME}
 * @param gameInfo      the game's shared facts, for {@link Kind#IN_GAME} and {@link Kind#NEEDS_SLOT};
 *                      {@code null} otherwise
 * @author Mario Koehler
 */
public record LoginResult(Kind kind, String message, String clientVersion, String serverVersion,
                          PlayerSnapshot state, GameInfo gameInfo) {

    /**
     * The outcomes of a login.
     */
    public enum Kind {
        /** Logged in, with a slot in this game. */
        IN_GAME,
        /** Logged in, but this account has no slot in this game yet: go to Join Game. */
        NEEDS_SLOT,
        /** Client and server builds differ. */
        VERSION_MISMATCH,
        /** The server refused the login, normally a wrong password. */
        REJECTED,
        /** No connection could be made. */
        UNREACHABLE,
        /** Connected, but the exchange broke off (timeout, dropped connection, unexpected reply). */
        FAILED
    }

    /**
     * Turns a handshake reply into a result, or nothing if it was accepted and the login goes on.
     *
     * @param response      the server's reply
     * @param clientVersion this client's version
     * @return the refusal, or empty if accepted
     */
    public static Optional<LoginResult> fromHandshake(HandshakeResponse response, String clientVersion) {
        if (response.accepted()) {
            return Optional.empty();
        }
        if (!clientVersion.equals(response.serverVersion())) {
            return Optional.of(new LoginResult(Kind.VERSION_MISMATCH, "", clientVersion, response.serverVersion(),
                null, null));
        }
        return Optional.of(failure(Kind.REJECTED, response.message()));
    }

    /**
     * Turns the state fetch after an accepted handshake into a result.
     *
     * @param response the server's reply
     * @return in the game, needing a slot, or failed
     */
    public static LoginResult fromFetch(FetchStateResponse response) {
        if (response.success()) {
            return new LoginResult(Kind.IN_GAME, "", "", "", response.state(), null);
        }
        if (ClaimSlotRequest.SLOT_CLAIM_REQUIRED.equals(response.message())) {
            return new LoginResult(Kind.NEEDS_SLOT, "", "", "", null, null);
        }
        return failure(Kind.FAILED, response.message());
    }

    /**
     * Creates a result that carries only a message.
     *
     * @param kind    {@link Kind#REJECTED}, {@link Kind#UNREACHABLE} or {@link Kind#FAILED}
     * @param message what went wrong
     * @return the result
     */
    public static LoginResult failure(Kind kind, String message) {
        return new LoginResult(kind, message == null ? "" : message, "", "", null, null);
    }

    /**
     * Returns this result with the game info attached.
     *
     * @param info the game info
     * @return the result
     */
    public LoginResult withGameInfo(GameInfo info) {
        return new LoginResult(kind, message, clientVersion, serverVersion, state, info);
    }
}
