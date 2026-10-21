package de.mkoehler.launchwindow.net.messages;

/**
 * What publishing a finding now would pay, as of now and if no other agency publishes it at the same
 * resolution ({@code design.md} &sect;2.13).
 *
 * @param success  whether the request could be served
 * @param message  why not, or {@code ""}
 * @param prestige the prestige it would pay
 * @param helped   the rivals that don't know the finding
 * @param rivals   every rival in the game
 * @author Mario Koehler
 */
public record PublishPreviewResponse(boolean success, String message, long prestige, int helped, int rivals) {

    /**
     * A preview.
     *
     * @param prestige the prestige it would pay
     * @param helped   the rivals it would help
     * @param rivals   every rival
     * @return the response
     */
    public static PublishPreviewResponse of(long prestige, int helped, int rivals) {
        return new PublishPreviewResponse(true, "", prestige, helped, rivals);
    }

    /**
     * The request couldn't be served.
     *
     * @param message why
     * @return the response
     */
    public static PublishPreviewResponse failure(String message) {
        return new PublishPreviewResponse(false, message, 0, 0, 0);
    }
}
