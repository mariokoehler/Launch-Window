package de.mkoehler.launchwindow.net.messages;

import de.mkoehler.launchwindow.model.FrontPage;

import java.util.ArrayList;
import java.util.List;

/**
 * Every front page printed in the game so far (G14), or why there's none.
 *
 * @param success whether the request was served
 * @param message why it wasn't, or {@code ""}
 * @param pages   every page, oldest first
 * @author Mario Koehler
 */
public record FrontPagesResponse(boolean success, String message, List<FrontPage> pages) {

    /**
     * The pages, copied into the {@link ArrayList} Kryo has registered.
     *
     * @param pages every page
     * @return the response
     */
    public static FrontPagesResponse of(List<FrontPage> pages) {
        return new FrontPagesResponse(true, "", new ArrayList<>(pages));
    }

    /**
     * The request couldn't be served.
     *
     * @param message why
     * @return the response
     */
    public static FrontPagesResponse failure(String message) {
        return new FrontPagesResponse(false, message, new ArrayList<>());
    }
}
