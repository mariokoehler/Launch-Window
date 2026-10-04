package de.mkoehler.launchwindow.net.messages;

import java.util.ArrayList;
import java.util.List;

/**
 * Every player's summary in the race (gap G3), or why there's none.
 *
 * @param success whether the request was served
 * @param message why it wasn't, or {@code ""}
 * @param rivals  every player in the game, the asking one included, in a stable order
 * @author Mario Koehler
 */
public record RivalsResponse(boolean success, String message, List<RivalSummary> rivals) {

    /**
     * The summaries, copied into the {@link ArrayList} Kryo has registered.
     *
     * @param rivals every player's summary
     * @return the response
     */
    public static RivalsResponse of(List<RivalSummary> rivals) {
        return new RivalsResponse(true, "", new ArrayList<>(rivals));
    }

    /**
     * The request couldn't be served.
     *
     * @param message why
     * @return the response
     */
    public static RivalsResponse failure(String message) {
        return new RivalsResponse(false, message, new ArrayList<>());
    }
}
