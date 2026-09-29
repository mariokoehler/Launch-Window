package de.mkoehler.launchwindow.net.messages;

/**
 * <p>The server's reply to a {@link SubmitActionRequest}.</p>
 *
 * @param success whether the action was accepted and appended to the
 *                player's journal
 * @param message a human-readable message - a confirmation on success, a
 *                rejection reason on failure
 * @author Mario Koehler
 */
public record SubmitActionResponse(boolean success, String message) {

    /**
     * Creates a successful response.
     *
     * @param message a human-readable confirmation
     * @return the successful response
     */
    public static SubmitActionResponse success(String message) {
        return new SubmitActionResponse(true, message);
    }

    /**
     * Creates a rejected response.
     *
     * @param message a human-readable rejection reason
     * @return the failed response
     */
    public static SubmitActionResponse failure(String message) {
        return new SubmitActionResponse(false, message);
    }
}
