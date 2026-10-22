package de.mkoehler.launchwindow.net.messages;

import de.mkoehler.launchwindow.model.FrontPage;

/**
 * Gives the press a statement about a rival's First, or withdraws one ({@code ui-spec.md} &sect;4.18.1,
 * component 28, G17): printed at the next resolution with every other rival's. The last one sent before
 * the turn resolves counts. Needs a slot in the game.
 *
 * @param gameTurn the open turn the player saw, so a statement that crosses a resolution is refused
 * @param pageId   the First's page
 * @param stance   what it says, or {@code null} to withdraw the statement
 * @param quote    the quote as typed, or {@code ""}
 * @author Mario Koehler
 */
public record SubmitStatementRequest(int gameTurn, String pageId, FrontPage.Stance stance, String quote) {
}
