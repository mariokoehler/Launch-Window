package de.mkoehler.launchwindow.client.settings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import de.mkoehler.launchwindow.net.NetworkConstants;

import java.util.List;

/**
 * What the client remembers between runs: the server, login and password of the last successful login,
 * so the Connect screen comes up filled in and Enter logs straight in (as in the sibling projects).
 * The password is kept in clear text in the player's own home folder, a deliberate choice for a game
 * account (user decision, 2026-10-05). Properties it doesn't know are ignored when loading, so a file
 * written by a newer version can still be read.
 *
 * @param server   the text of the server field, as typed
 * @param login    the login
 * @param password the password
 * @author Mario Koehler
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ClientSettings(String server, String login, String password) {

    /** The public server, on the NAS: the server field's default before anything was saved. */
    public static final String PUBLIC_SERVER = "nas5714.myqnapcloud.com:" + NetworkConstants.TCP_PORT;

    /** A server on this machine, for development. */
    public static final String LOCAL_SERVER = "localhost:" + NetworkConstants.TCP_PORT;

    /** The servers the Connect screen's preset button cycles through, in order (user, 2026-11-12). */
    public static final List<String> SERVER_PRESETS = List.of(LOCAL_SERVER, PUBLIC_SERVER);

    /**
     * Replaces missing values with empty text, so a hand-edited file can't cause a {@code null} on the
     * Connect screen.
     */
    public ClientSettings {
        server = server == null ? "" : server;
        login = login == null ? "" : login;
        password = password == null ? "" : password;
    }

    /**
     * Returns the settings before anything was saved: the public server, no login.
     *
     * @return the defaults
     */
    public static ClientSettings defaults() {
        return new ClientSettings(PUBLIC_SERVER, "", "");
    }
}
