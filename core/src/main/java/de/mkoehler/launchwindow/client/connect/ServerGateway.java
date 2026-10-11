package de.mkoehler.launchwindow.client.connect;

import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.persistence.PlayerAction;

import java.util.function.Consumer;

/**
 * The client's way to the server, as the screens see it. Every call returns at once and
 * delivers its result later on the render thread, so a screen never blocks while a connection
 * is opened or a reply awaited. An interface so screens can be built against a stub (the
 * offscreen screenshot tool, {@code design.md} &sect;3.11).
 *
 * @author Mario Koehler
 */
public interface ServerGateway {

    /**
     * Logs in on a fresh connection, which then stays open for the session, and fetches the game
     * info and the player's state.
     *
     * @param credentials who and where
     * @param done        receives the result on the render thread
     */
    void logIn(Credentials credentials, Consumer<LoginResult> done);

    /**
     * Claims this account's slot in the game over the session's connection (reconnecting and
     * logging in again first if it has dropped), and fetches the game info and the new state.
     *
     * @param credentials  who and where
     * @param archetype    the agency style
     * @param agencyName   the agency's name
     * @param directorName the director's name
     * @param done         receives the result on the render thread
     */
    void claimSlot(Credentials credentials, Archetype archetype, String agencyName, String directorName,
                   Consumer<ClaimResult> done);

    /**
     * Submits one of today's actions over the session's connection, then fetches the state with it
     * applied and the game info. The submit itself is never retried: if the connection breaks
     * during it, the server may already have applied it. If the turn resolved before the action
     * arrived, nothing is applied and the result is {@link ServerResult.Kind#TURN_RESOLVED}, with the
     * new state.
     *
     * @param credentials who and where
     * @param action      the action
     * @param gameTurn    the open game turn the screen's state belongs to
     * @param done        receives the result on the render thread
     */
    void submit(Credentials credentials, PlayerAction action, long gameTurn, Consumer<ServerResult> done);

    /**
     * Returns the turn the server last said had resolved, unasked, and forgets it: a screen then
     * fetches the new state. Stubs keep the default.
     *
     * @return the resolved turn, or 0 if no notice came since the last call
     */
    default long takeTurnResolvedNotice() {
        return 0;
    }

    /**
     * Returns whether a call is under way: sent, and its result not yet delivered. Screens show a
     * waiting indicator while it stays true for a moment. Stubs that answer at once keep the default.
     *
     * @return whether a call is under way
     */
    default boolean isWaiting() {
        return false;
    }

    /**
     * Fetches the player's state and the game info again. A read, so it may reconnect and retry
     * once if the connection breaks.
     *
     * @param credentials who and where
     * @param done        receives the result on the render thread
     */
    void refresh(Credentials credentials, Consumer<ServerResult> done);

    /**
     * Fetches one of the player's turn reports (gap G2). A read: retried once if the connection
     * broke.
     *
     * @param credentials who and where
     * @param turn        the turn, or a negative number for the latest
     * @param done        called on the render thread with the result
     */
    void fetchTurnReport(Credentials credentials, long turn, Consumer<TurnReportResult> done);

    /**
     * Fetches every player's summary in the race (gap G3). A read: retried once if the
     * connection broke.
     *
     * @param credentials who and where
     * @param done        called on the render thread with the result
     */
    void fetchRivals(Credentials credentials, Consumer<RivalsResult> done);

    /**
     * Fetches every front page printed in the game so far ({@code ui-spec.md} &sect;4.18, G14). A
     * read: retried once if the connection broke. By default it never answers, like a gateway with
     * no server.
     *
     * @param credentials who and where
     * @param done        called on the render thread with the result
     */
    default void fetchFrontPages(Credentials credentials, Consumer<FrontPagesResult> done) {
    }

    /**
     * Asks a dev-mode server to resolve the open turn now, then fetches the new state and game
     * info. Never repeated if the connection breaks mid-exchange.
     *
     * @param credentials who and where
     * @param done        called on the render thread: OK with the new state, or REFUSED with the
     *                    server's reason
     */
    void resolveTurnNow(Credentials credentials, Consumer<ServerResult> done);

    /**
     * Ends the session: closes its connection. The next {@link #logIn} opens a new one.
     */
    void logOut();

    /**
     * Ends the session and stops the background work, when the client exits.
     */
    void dispose();
}
