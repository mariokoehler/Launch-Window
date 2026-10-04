package de.mkoehler.launchwindow.net.messages;

/**
 * The answer to a {@link ResolveTurnNowRequest}.
 *
 * @param success      whether the turn was resolved
 * @param message      why not, or {@code ""}
 * @param resolvedTurn the game turn that was resolved, or -1
 * @author Mario Koehler
 */
public record ResolveTurnNowResponse(boolean success, String message, long resolvedTurn) {

    /**
     * A resolved turn.
     *
     * @param turn the game turn that was resolved
     * @return the response
     */
    public static ResolveTurnNowResponse resolved(long turn) {
        return new ResolveTurnNowResponse(true, "", turn);
    }

    /**
     * A refusal or failure.
     *
     * @param message why
     * @return the response
     */
    public static ResolveTurnNowResponse failure(String message) {
        return new ResolveTurnNowResponse(false, message, -1);
    }
}
