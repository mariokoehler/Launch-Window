package de.mkoehler.launchwindow.client.connect;

import de.mkoehler.launchwindow.net.messages.RivalSummary;

import java.util.List;

/**
 * What a rival-summaries fetch came to (gap G3).
 *
 * @param ok      whether the summaries came back
 * @param message the server's or connection's reason, or {@code ""}
 * @param rivals  every player's summary, when {@code ok}
 * @author Mario Koehler
 */
public record RivalsResult(boolean ok, String message, List<RivalSummary> rivals) {

    /**
     * Copies the list.
     */
    public RivalsResult {
        rivals = rivals == null ? List.of() : List.copyOf(rivals);
    }
}
