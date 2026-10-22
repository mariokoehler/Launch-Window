package de.mkoehler.launchwindow.client.connect;

import de.mkoehler.launchwindow.model.FrontPage;

import java.util.List;

/**
 * What a front-pages fetch came to (G14), with the player's own statements in the open turn (G17).
 *
 * @param ok      whether the pages came back
 * @param message the server's or connection's reason, or {@code ""}
 * @param pages   every page printed so far, oldest first, when {@code ok}
 * @param yours   the player's statements in the open turn, when {@code ok}
 * @author Mario Koehler
 */
public record FrontPagesResult(boolean ok, String message, List<FrontPage> pages, List<FrontPage.Statement> yours) {

    /**
     * Copies the lists.
     */
    public FrontPagesResult {
        pages = pages == null ? List.of() : List.copyOf(pages);
        yours = yours == null ? List.of() : List.copyOf(yours);
    }

    /**
     * A result without statements.
     *
     * @param ok      whether the pages came back
     * @param message the reason, or {@code ""}
     * @param pages   the pages
     */
    public FrontPagesResult(boolean ok, String message, List<FrontPage> pages) {
        this(ok, message, pages, List.of());
    }
}
