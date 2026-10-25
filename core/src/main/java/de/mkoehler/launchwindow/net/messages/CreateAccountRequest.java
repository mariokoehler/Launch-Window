package de.mkoehler.launchwindow.net.messages;

/**
 * <p>Sent instead of a {@link HandshakeRequest} to make a new account and log in with it, in one step
 * (review.md 2.3): a handshake with an unknown login is refused with
 * {@link HandshakeResponse#NO_SUCH_ACCOUNT}, so a typo in a login never quietly makes a second, empty
 * account. Answered with a {@link HandshakeResponse}; accepted, it authenticates the connection as a
 * handshake does.</p>
 *
 * <p>The version is checked first, as the handshake's is. A login that already exists is refused,
 * unless the password is its own: then this simply logs in, so a create whose answer was lost can be
 * sent again.</p>
 *
 * @param clientVersion the client's build version
 * @param login         the new account's login name
 * @param password      its plain-text password, hashed server-side
 * @author Mario Koehler
 */
public record CreateAccountRequest(String clientVersion, String login, String password) {

    /**
     * Returns a representation with the password redacted, so this record is safe to include in a log line.
     *
     * @return a redacted representation
     */
    @Override
    public String toString() {
        return "CreateAccountRequest[clientVersion=" + clientVersion + ", login=" + login
            + ", password=<redacted>]";
    }
}
