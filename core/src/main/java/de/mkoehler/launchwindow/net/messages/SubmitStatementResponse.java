package de.mkoehler.launchwindow.net.messages;

import de.mkoehler.launchwindow.model.FrontPage;

import java.util.ArrayList;
import java.util.List;

/**
 * What a statement came to ({@code ui-spec.md} &sect;4.18.1, G17): kept or withdrawn, with the player's
 * statements for the open turn, or a refusal code ({@code client.ui.Refusals}).
 *
 * @param success whether it was kept or withdrawn
 * @param message a refusal code, or {@code ""}
 * @param yours   the player's statements for the open turn, after this one
 * @author Mario Koehler
 */
public record SubmitStatementResponse(boolean success, String message, List<FrontPage.Statement> yours) {

    /** The turn resolved while the statement was on its way. */
    public static final String TURN_RESOLVED = SubmitActionRequest.TURN_RESOLVED;
    /** No such First page. */
    public static final String UNKNOWN_PAGE = "UNKNOWN_PAGE";
    /** The page isn't open for statements this turn. */
    public static final String REACTION_WINDOW_CLOSED = "REACTION_WINDOW_CLOSED";
    /** The player made the First, or joined after it. */
    public static final String NOT_A_RIVAL = "NOT_A_RIVAL";
    /** The stance isn't one a player can give. */
    public static final String INVALID_STANCE = "INVALID_STANCE";
    /** The quote is longer than {@link FrontPage.Statement#MAX_QUOTE_LENGTH}. */
    public static final String QUOTE_TOO_LONG = "QUOTE_TOO_LONG";
    /** The quote holds a character outside {@link FrontPage.Statement#ALLOWED_CHARACTERS}. */
    public static final String QUOTE_BAD_CHARACTERS = "QUOTE_BAD_CHARACTERS";

    /**
     * Copies the list into the {@link ArrayList} Kryo has registered.
     */
    public SubmitStatementResponse {
        yours = yours == null ? new ArrayList<>() : new ArrayList<>(yours);
    }

    /**
     * Kept or withdrawn.
     *
     * @param yours the player's statements for the open turn
     * @return the response
     */
    public static SubmitStatementResponse success(List<FrontPage.Statement> yours) {
        return new SubmitStatementResponse(true, "", yours);
    }

    /**
     * Refused.
     *
     * @param message the refusal code, or why the server failed
     * @return the response
     */
    public static SubmitStatementResponse failure(String message) {
        return new SubmitStatementResponse(false, message, List.of());
    }
}
