package de.mkoehler.launchwindow.client.connect;

import de.mkoehler.launchwindow.net.messages.StandingSeries;

import java.util.List;

/**
 * What a standing fetch came to (gap G20).
 *
 * @param ok       whether the curves came back
 * @param message  the server's or connection's reason, or {@code ""}
 * @param agencies every agency's curves, when {@code ok}
 * @author Mario Koehler
 */
public record StandingResult(boolean ok, String message, List<StandingSeries> agencies) {

    /**
     * Copies the list.
     */
    public StandingResult {
        agencies = agencies == null ? List.of() : List.copyOf(agencies);
    }
}
