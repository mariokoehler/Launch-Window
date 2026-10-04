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
     * during it, the server may already have applied it.
     *
     * @param credentials who and where
     * @param action      the action
     * @param done        receives the result on the render thread
     */
    void submit(Credentials credentials, PlayerAction action, Consumer<ServerResult> done);

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
     * Ends the session: closes its connection. The next {@link #logIn} opens a new one.
     */
    void logOut();

    /**
     * Ends the session and stops the background work, when the client exits.
     */
    void dispose();
}
