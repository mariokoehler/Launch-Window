package de.mkoehler.launchwindow.net.messages;

import de.mkoehler.launchwindow.model.FrontPage;

import java.util.ArrayList;
import java.util.List;

/**
 * Every front page printed in the game so far (G14), or why there's none, with the statements the
 * requester gave in the open turn (G17) and the crews lost in the turn before, which the open turn's
 * statements and condolences answer (G19).
 *
 * @param success whether the request was served
 * @param message why it wasn't, or {@code ""}
 * @param pages   every page, oldest first
 * @param yours   the requester's statements in the open turn, never anyone else's
 * @param losses  the crews lost in the turn before the open one, their pages not printed yet
 * @author Mario Koehler
 */
public record FrontPagesResponse(boolean success, String message, List<FrontPage> pages,
                                 List<FrontPage.Statement> yours, List<FrontPage.Loss> losses) {

    /**
     * Copies the lists into the {@link ArrayList}s Kryo has registered.
     */
    public FrontPagesResponse {
        pages = pages == null ? new ArrayList<>() : new ArrayList<>(pages);
        yours = yours == null ? new ArrayList<>() : new ArrayList<>(yours);
        losses = losses == null ? new ArrayList<>() : new ArrayList<>(losses);
    }

    /**
     * The pages, with no statements of the requester's.
     *
     * @param pages every page
     * @return the response
     */
    public static FrontPagesResponse of(List<FrontPage> pages) {
        return of(pages, List.of());
    }

    /**
     * The pages and the requester's statements.
     *
     * @param pages every page
     * @param yours the requester's statements in the open turn
     * @return the response
     */
    public static FrontPagesResponse of(List<FrontPage> pages, List<FrontPage.Statement> yours) {
        return of(pages, yours, List.of());
    }

    /**
     * The pages, the requester's statements and the crews lost in the turn before.
     *
     * @param pages  every page
     * @param yours  the requester's statements in the open turn
     * @param losses the crews lost in the turn before the open one
     * @return the response
     */
    public static FrontPagesResponse of(List<FrontPage> pages, List<FrontPage.Statement> yours,
                                        List<FrontPage.Loss> losses) {
        return new FrontPagesResponse(true, "", pages, yours, losses);
    }

    /**
     * The request couldn't be served.
     *
     * @param message why
     * @return the response
     */
    public static FrontPagesResponse failure(String message) {
        return new FrontPagesResponse(false, message, List.of(), List.of(), List.of());
    }
}
