package de.mkoehler.launchwindow.net.messages;

/**
 * Answers {@link ServerSettingsRequest} and {@link SaveServerSettingsRequest} ({@code ui-spec.md} &sect;4.22, G21).
 *
 * @param status      what happened
 * @param message     the server's words, for a failure
 * @param minutesLeft for {@link Status#ADMIN_LOCKED_OUT}, the minutes until another try
 * @param gzippedJson for OK, the {@link ServerSettings} as saved; for INVALID, the {@link ServerSettings.FieldErrors}
 * @author Mario Koehler
 */
public record ServerSettingsResponse(Status status, String message, int minutesLeft, byte[] gzippedJson) {

    /**
     * What a request came to. Constants are only appended (Kryo writes the ordinal).
     */
    public enum Status {
        /** The settings, read or saved. */
        OK,
        /** Not the admin password. */
        WRONG_ADMIN_PASSWORD,
        /** Too many wrong tries from this address. */
        ADMIN_LOCKED_OUT,
        /** The file changed since the settings were read. */
        STALE_SETTINGS,
        /** A field was refused; nothing was saved. */
        INVALID_FIELD,
        /** Not logged in. */
        NOT_AUTHENTICATED,
        /** The server failed. */
        SERVER_ERROR
    }

    /**
     * Normalizes missing values.
     */
    public ServerSettingsResponse {
        message = message == null ? "" : message;
        gzippedJson = gzippedJson == null ? new byte[0] : gzippedJson;
    }

    /**
     * The settings.
     *
     * @param settings the settings
     * @return the response
     */
    public static ServerSettingsResponse ok(ServerSettings settings) {
        return new ServerSettingsResponse(Status.OK, "", 0, ServerSettings.gzip(settings));
    }

    /**
     * Refused fields.
     *
     * @param errors the fields and the server's words
     * @return the response
     */
    public static ServerSettingsResponse invalid(java.util.Map<String, String> errors) {
        return new ServerSettingsResponse(Status.INVALID_FIELD, "", 0,
            ServerSettings.gzip(new ServerSettings.FieldErrors(errors)));
    }

    /**
     * A failure without fields.
     *
     * @param status  the failure
     * @param message the server's words
     * @return the response
     */
    public static ServerSettingsResponse failure(Status status, String message) {
        return new ServerSettingsResponse(status, message, 0, new byte[0]);
    }

    /**
     * Returns the settings, for OK.
     *
     * @return the settings, or {@code null}
     */
    public ServerSettings settings() {
        return status == Status.OK ? ServerSettings.gunzip(gzippedJson, ServerSettings.class) : null;
    }

    /**
     * Returns the refused fields, for INVALID_FIELD.
     *
     * @return the fields and the server's words, empty for any other status
     */
    public java.util.Map<String, String> fieldErrors() {
        if (status != Status.INVALID_FIELD) {
            return java.util.Map.of();
        }
        ServerSettings.FieldErrors errors = ServerSettings.gunzip(gzippedJson, ServerSettings.FieldErrors.class);
        return errors == null ? java.util.Map.of() : errors.fields();
    }
}
