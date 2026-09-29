package de.mkoehler.launchwindow.net.messages;

/**
 * <p>Sent by a client immediately after a connection is established -
 * announces which build it runs and authenticates for the lifetime of this
 * connection, in one step. There is no separate login call: a short-lived
 * connection (see {@code design.md} &sect;3.3) authenticates once, here, not
 * on every subsequent request.</p>
 *
 * <p>The server checks {@link #clientVersion()} against its own build first
 * and rejects a mismatch without touching the account store at all - an
 * incompatible client must never be able to create an account.</p>
 *
 * <p>This record's fields never change, not even by adding one: it and
 * {@link HandshakeResponse} are registered first in {@code MessageRegistry}
 * specifically so their ids survive drift in every other registered class,
 * but Kryo's record serializer still reads the receiver's own field count
 * to decode one, so two builds with a different field list here could not
 * decode each other's handshake regardless of registration order.</p>
 *
 * @param clientVersion the client's build version, normally
 *                      {@code AppVersion.getVersion()}
 * @param login         the account's login name
 * @param password      the account's plain-text password, checked against
 *                      the stored hash server-side
 * @param displayName   the name to show this player as in-game
 * @author Mario Koehler
 */
public record HandshakeRequest(String clientVersion, String login, String password, String displayName) {

    /**
     * Returns a representation with the password redacted, so this record
     * is safe to include in a log line.
     *
     * @return a redacted representation
     */
    @Override
    public String toString() {
        return "HandshakeRequest[clientVersion=" + clientVersion + ", login=" + login
            + ", password=<redacted>, displayName=" + displayName + "]";
    }
}
