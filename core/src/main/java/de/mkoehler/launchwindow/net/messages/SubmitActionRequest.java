package de.mkoehler.launchwindow.net.messages;

import de.mkoehler.launchwindow.persistence.PlayerAction;

/**
 * <p>Sent by a client to submit one action to its own not-yet-resolved
 * journal (see {@code design.md} &sect;3.7). Carries login credentials
 * directly, rather than a prior handshake establishing a session - this
 * project's connections are short-lived RPC calls, not held sessions (see
 * {@code design.md} &sect;3.3).</p>
 *
 * @param login    the account's login name
 * @param password the account's plain-text password, checked against the
 *                 stored hash server-side
 * @param action   the action to submit
 * @author Mario Koehler
 */
public record SubmitActionRequest(String login, String password, PlayerAction action) {

    /**
     * Returns a representation with the password redacted, so this record
     * is safe to include in a log line.
     *
     * @return a redacted representation
     */
    @Override
    public String toString() {
        return "SubmitActionRequest[login=" + login + ", password=<redacted>, action=" + action + "]";
    }
}
