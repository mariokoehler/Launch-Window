package de.mkoehler.launchwindow.net.messages;

/**
 * Ends the game for everyone and starts a fresh one with a new seed ({@code ui-spec.md} &sect;4.22, G21): every
 * agency, report and record goes, the accounts and the settings stay. Answered with a {@link ServerSettingsResponse}
 * carrying the settings as they are now; everyone online then hears a {@link NewGameStartedNotice}.
 *
 * @param password     the admin password
 * @param version      the settings' version the admin was looking at, so a change meanwhile refuses it
 * @param confirmation the words the admin typed to confirm, {@link #CONFIRMATION}
 * @author Mario Koehler
 */
public record StartNewGameRequest(String password, String version, String confirmation) {

    /** What the admin types to confirm. */
    public static final String CONFIRMATION = "NEW GAME";

    /**
     * Normalizes missing values.
     */
    public StartNewGameRequest {
        password = password == null ? "" : password;
        version = version == null ? "" : version;
        confirmation = confirmation == null ? "" : confirmation;
    }

    @Override
    public String toString() {
        return "StartNewGameRequest[password=***, version=" + version + ", confirmation=" + confirmation + "]";
    }
}
