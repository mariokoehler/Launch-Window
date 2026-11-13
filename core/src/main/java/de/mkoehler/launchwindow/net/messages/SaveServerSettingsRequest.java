package de.mkoehler.launchwindow.net.messages;

/**
 * Saves the server's settings ({@code ui-spec.md} &sect;4.22, G21).
 *
 * @param password     the admin password
 * @param gzippedJson  the edited {@link ServerSettings}, carrying the version it was read at
 * @param newPassword  a new admin password, or empty to keep the current one
 * @author Mario Koehler
 */
public record SaveServerSettingsRequest(String password, byte[] gzippedJson, String newPassword) {

    /**
     * Normalizes missing values.
     */
    public SaveServerSettingsRequest {
        gzippedJson = gzippedJson == null ? new byte[0] : gzippedJson;
        newPassword = newPassword == null ? "" : newPassword;
    }

    /**
     * Builds a request.
     *
     * @param password    the admin password
     * @param settings    the edited settings
     * @param newPassword a new admin password, or empty
     * @return the request
     */
    public static SaveServerSettingsRequest of(String password, ServerSettings settings, String newPassword) {
        return new SaveServerSettingsRequest(password, ServerSettings.gzip(settings), newPassword);
    }

    /**
     * Returns the edited settings.
     *
     * @return them, or {@code null} if none were sent
     */
    public ServerSettings settings() {
        return ServerSettings.gunzip(gzippedJson, ServerSettings.class);
    }

    @Override
    public String toString() {
        return "SaveServerSettingsRequest[password=***, " + gzippedJson.length + " bytes]";
    }
}
