package de.mkoehler.launchwindow.client.connect;

import de.mkoehler.launchwindow.model.FrontPage;

import java.util.List;

/**
 * What giving or withdrawing a statement came to ({@code ui-spec.md} &sect;4.18.1, G17).
 *
 * @param ok      whether it was kept or withdrawn
 * @param message the refusal code or connection's reason, or {@code ""}
 * @param yours   the player's statements in the open turn afterwards, when {@code ok}
 * @author Mario Koehler
 */
public record StatementResult(boolean ok, String message, List<FrontPage.Statement> yours) {

    /**
     * Copies the list.
     */
    public StatementResult {
        yours = yours == null ? List.of() : List.copyOf(yours);
    }
}
