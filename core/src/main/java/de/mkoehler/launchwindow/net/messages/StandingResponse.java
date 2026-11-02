package de.mkoehler.launchwindow.net.messages;

import java.util.ArrayList;
import java.util.List;

/**
 * Every agency's standing over time (gap G20), or why there's none.
 *
 * @param success  whether the request was served
 * @param message  why it wasn't, or {@code ""}
 * @param agencies every agency in the game, the asking one included, in a stable order
 * @author Mario Koehler
 */
public record StandingResponse(boolean success, String message, List<StandingSeries> agencies) {

    /**
     * The curves, copied into the {@link ArrayList} Kryo has registered.
     *
     * @param agencies every agency's curves
     * @return the response
     */
    public static StandingResponse of(List<StandingSeries> agencies) {
        return new StandingResponse(true, "", new ArrayList<>(agencies));
    }

    /**
     * The request couldn't be served.
     *
     * @param message why
     * @return the response
     */
    public static StandingResponse failure(String message) {
        return new StandingResponse(false, message, new ArrayList<>());
    }
}
