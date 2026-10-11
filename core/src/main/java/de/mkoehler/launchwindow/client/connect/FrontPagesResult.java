package de.mkoehler.launchwindow.client.connect;

import de.mkoehler.launchwindow.model.FrontPage;

import java.util.List;

/**
 * What a front-pages fetch came to (G14).
 *
 * @param ok      whether the pages came back
 * @param message the server's or connection's reason, or {@code ""}
 * @param pages   every page printed so far, oldest first, when {@code ok}
 * @author Mario Koehler
 */
public record FrontPagesResult(boolean ok, String message, List<FrontPage> pages) {

    /**
     * Copies the list.
     */
    public FrontPagesResult {
        pages = pages == null ? List.of() : List.copyOf(pages);
    }
}
