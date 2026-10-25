package de.mkoehler.launchwindow.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.model.FrontPage;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.net.messages.SubmitStatementRequest;
import de.mkoehler.launchwindow.net.messages.SubmitStatementResponse;
import de.mkoehler.launchwindow.rules.ReactionPrinter;
import de.mkoehler.launchwindow.server.schedule.FrontPageStore;
import de.mkoehler.launchwindow.server.schedule.LossStore;
import de.mkoehler.launchwindow.server.schedule.ReactionStore;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * <p>Keeps an agency's statement for the open turn, for a player ({@link RequestDispatcher}) and a filler
 * bot alike: on a rival's First from the turn before ({@code ui-spec.md} &sect;4.18.1), or on a crew lost
 * in it, its own agency's tone or another's condolences (&sect;4.18.3).</p>
 *
 * @author Mario Koehler
 */
public final class Statements {

    private Statements() {
    }

    /**
     * Keeps or withdraws a statement, once the page or loss it answers and its stance check out.
     *
     * @param dataRoot the data root
     * @param mapper   the persistence mapper
     * @param open     the open turn, which the request is for
     * @param key      the agency's key
     * @param state    the agency's preview
     * @param request  the request
     * @return the response
     * @throws IOException                 if the pages or statements can't be read or written
     * @throws ReactionStore.SealedException if the turn has already read its statements
     */
    public static SubmitStatementResponse give(Path dataRoot, ObjectMapper mapper, long open, String key,
            PlayerSnapshot state, SubmitStatementRequest request) throws IOException {
        // A crew lost in the turn before: its agency's statement, or another's condolences (ui-spec.md 4.18.3).
        Optional<FrontPage.Loss> loss = LossStore.load(dataRoot, mapper, open - 1).stream()
            .filter(l -> l.id().equals(request.pageId())).findFirst();
        if (loss.isPresent()) {
            boolean own = loss.get().rivalKey().equals(key);
            return keep(dataRoot, mapper, request, open, key, stance -> own ? stance.tone() : stance.condolence());
        }
        Optional<FrontPage> page = FrontPageStore.load(dataRoot, mapper).stream()
            .filter(p -> p.id().equals(request.pageId()) && p.firstPage()).findFirst();
        if (page.isEmpty()) {
            return SubmitStatementResponse.failure(SubmitStatementResponse.UNKNOWN_PAGE);
        }
        if (page.get().gameTurn() != open - 1) {
            return SubmitStatementResponse.failure(SubmitStatementResponse.REACTION_WINDOW_CLOSED);
        }
        if (!ReactionPrinter.mayReact(page.get(), Math.toIntExact(open), state.getAgencyName(),
                ReactionPrinter.firstTurn(open, state.getSeason()))) {
            return SubmitStatementResponse.failure(SubmitStatementResponse.NOT_A_RIVAL);
        }
        return keep(dataRoot, mapper, request, open, key, FrontPage.Stance::givable);
    }

    /**
     * Keeps or withdraws a statement once the page or loss it answers checked out: the stance must be one this
     * agency may give there, and the quote is cleaned and checked.
     */
    private static SubmitStatementResponse keep(Path dataRoot, ObjectMapper mapper, SubmitStatementRequest request,
            long open, String key, Predicate<FrontPage.Stance> allowed) throws IOException {
        if (request.stance() == null) {
            return SubmitStatementResponse.success(ReactionStore.remove(dataRoot, mapper, open, key,
                request.pageId()));
        }
        if (!allowed.test(request.stance())) {
            return SubmitStatementResponse.failure(SubmitStatementResponse.INVALID_STANCE);
        }
        String quote = request.stance().quotable() ? FrontPage.Statement.clean(request.quote()) : "";
        if (quote.length() > FrontPage.Statement.MAX_QUOTE_LENGTH) {
            return SubmitStatementResponse.failure(SubmitStatementResponse.QUOTE_TOO_LONG);
        }
        if (!FrontPage.Statement.allowed(quote)) {
            return SubmitStatementResponse.failure(SubmitStatementResponse.QUOTE_BAD_CHARACTERS);
        }
        return SubmitStatementResponse.success(ReactionStore.put(dataRoot, mapper, open, key,
            new FrontPage.Statement(request.pageId(), request.stance(), quote)));
    }
}
