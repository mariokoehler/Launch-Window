package de.mkoehler.launchwindow.net.messages;

/**
 * <p>Sent by a client to fetch its own current state: its last resolved
 * snapshot with today's not-yet-resolved actions replayed on top (see
 * {@code design.md} &sect;3.7's login flow). Carries login credentials
 * directly, the same as {@link SubmitActionRequest} - see there for why.</p>
 *
 * @param login    the account's login name
 * @param password the account's plain-text password, checked against the
 *                 stored hash server-side
 * @author Mario Koehler
 */
public record FetchStateRequest(String login, String password) {

    /**
     * Returns a representation with the password redacted, so this record
     * is safe to include in a log line.
     *
     * @return a redacted representation
     */
    @Override
    public String toString() {
        return "FetchStateRequest[login=" + login + ", password=<redacted>]";
    }
}
