package de.mkoehler.launchwindow.client.connect;

import java.util.Objects;

/**
 * Who the player is and where they play: kept in memory for the client session only, never
 * written to disk. Every server call opens its own short connection and authenticates again
 * with these ({@code design.md} &sect;3.3: no connection is held between actions).
 *
 * @param address  the server
 * @param login    the account's login
 * @param password the account's password
 * @author Mario Koehler
 */
public record Credentials(ServerAddress address, String login, String password) {

    /**
     * Checks that nothing is missing.
     *
     * @throws NullPointerException if any part is {@code null}
     */
    public Credentials {
        Objects.requireNonNull(address, "address must not be null");
        Objects.requireNonNull(login, "login must not be null");
        Objects.requireNonNull(password, "password must not be null");
    }

    /**
     * Returns a representation with the password redacted, so this record is safe in a log line.
     *
     * @return a redacted representation
     */
    @Override
    public String toString() {
        return "Credentials[address=" + address + ", login=" + login + ", password=<redacted>]";
    }
}
