package de.mkoehler.launchwindow.net.messages;

import de.mkoehler.launchwindow.model.GameDate;
import de.mkoehler.launchwindow.model.Season;

import java.util.ArrayList;
import java.util.List;

/**
 * The game's shared facts, the same for every player (gap G1, {@code ui-spec.md} &sect;6). Times
 * are epoch milliseconds, and {@code serverNowMillis} is the server's clock when it answered, so a
 * client counts down to the next resolution against server time and its own clock's error
 * doesn't matter.
 *
 * <p>Lists are always {@link ArrayList}s: Kryo only knows the registered list class, not the
 * immutable ones {@code List.of} makes. Use {@link #success} to build one.</p>
 *
 * @param success              whether the facts could be read; if not, only {@code message} is set
 * @param message              why not, on failure; empty on success
 * @param contentPackId        the game's content pack
 * @param contentPackHash      the game's frozen copy's {@code ContentPackHash}
 * @param declaredGoalIds      the goals this game is won by
 * @param gameTurn             the open game turn, the one players are planning now
 * @param year                 the open turn's year
 * @param season               the open turn's season
 * @param started              whether any turn has resolved yet (before that, the game is in its
 *                             signup window)
 * @param nextResolutionMillis when the open turn resolves, or -1 if no resolution is scheduled
 * @param serverNowMillis      the server's clock when it answered
 * @param resultStatus         the game result's status ({@code WON} or {@code TIED}), or
 *                             {@code null} while undecided
 * @param winnerAgencies       the winning agencies' names; empty while undecided
 * @param winnerDirectors      their directors' names, in the same order; empty while undecided
 * @param resultTurn           the game turn the result was decided on, or 0 while undecided
 * @param devMode              whether the server runs in dev mode, so the client offers its play
 *                             testing tools (resolving the turn now)
 * @author Mario Koehler
 */
public record GameInfoResponse(boolean success, String message, String contentPackId, String contentPackHash,
                               List<String> declaredGoalIds, long gameTurn, int year, Season season,
                               boolean started, long nextResolutionMillis, long serverNowMillis,
                               String resultStatus, List<String> winnerAgencies, List<String> winnerDirectors,
                               long resultTurn, boolean devMode) {

    /**
     * Creates a successful response, copying every list into an {@link ArrayList}.
     *
     * @param contentPackId        the content pack
     * @param contentPackHash      its hash
     * @param declaredGoalIds      the declared goals
     * @param gameTurn             the open game turn
     * @param started              whether any turn has resolved
     * @param nextResolutionMillis the next resolution, or -1
     * @param serverNowMillis      the server's clock
     * @param resultStatus         the result status, or {@code null}
     * @param winnerAgencies       the winners' agencies
     * @param winnerDirectors      the winners' directors
     * @param resultTurn           the deciding turn, or 0
     * @return the response
     */
    public static GameInfoResponse success(String contentPackId, String contentPackHash, List<String> declaredGoalIds,
                                           long gameTurn, boolean started, long nextResolutionMillis,
                                           long serverNowMillis, String resultStatus, List<String> winnerAgencies,
                                           List<String> winnerDirectors, long resultTurn) {
        return success(contentPackId, contentPackHash, declaredGoalIds, gameTurn, started, nextResolutionMillis,
            serverNowMillis, resultStatus, winnerAgencies, winnerDirectors, resultTurn, false);
    }

    /**
     * Creates a successful response that also says whether the server runs in dev mode.
     *
     * @param contentPackId        the content pack
     * @param contentPackHash      its hash
     * @param declaredGoalIds      the declared goals
     * @param gameTurn             the open game turn
     * @param started              whether any turn has resolved
     * @param nextResolutionMillis the next resolution, or -1
     * @param serverNowMillis      the server's clock
     * @param resultStatus         the result status, or {@code null}
     * @param winnerAgencies       the winners' agencies
     * @param winnerDirectors      the winners' directors
     * @param resultTurn           the deciding turn, or 0
     * @param devMode              whether the server runs in dev mode
     * @return the response
     */
    public static GameInfoResponse success(String contentPackId, String contentPackHash, List<String> declaredGoalIds,
                                           long gameTurn, boolean started, long nextResolutionMillis,
                                           long serverNowMillis, String resultStatus, List<String> winnerAgencies,
                                           List<String> winnerDirectors, long resultTurn,
                                           boolean devMode) {
        GameDate date = GameDate.ofTurn(gameTurn);
        return new GameInfoResponse(true, "", contentPackId, contentPackHash, new ArrayList<>(declaredGoalIds),
            gameTurn, date.year(), date.season(), started, nextResolutionMillis, serverNowMillis, resultStatus,
            new ArrayList<>(winnerAgencies), new ArrayList<>(winnerDirectors), resultTurn, devMode);
    }

    /**
     * Creates a failure.
     *
     * @param message why the facts couldn't be read
     * @return the response
     */
    public static GameInfoResponse failure(String message) {
        return new GameInfoResponse(false, message, null, null, new ArrayList<>(), 0, 0, null, false, -1, 0, null,
            new ArrayList<>(), new ArrayList<>(), 0, false);
    }

    /**
     * Returns the open turn's date.
     *
     * @return the date
     */
    public GameDate date() {
        return new GameDate(year, season);
    }

    /**
     * Returns whether the game has been decided.
     *
     * @return whether there's a result
     */
    public boolean decided() {
        return resultStatus != null;
    }
}
