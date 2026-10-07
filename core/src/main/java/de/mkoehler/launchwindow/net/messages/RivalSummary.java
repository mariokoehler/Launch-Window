package de.mkoehler.launchwindow.net.messages;

import de.mkoehler.launchwindow.model.Archetype;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One player's row in the race (gap G3). Only what the race shows: nothing about funds,
 * prestige or the roster.
 *
 * @param agencyName   the agency
 * @param directorName its director
 * @param archetype    its archetype
 * @param you          whether this is the player who asked
 * @param goalTurns    the turn each declared goal was done (a goal credited through the cascade
 *                     counts from the turn of the goal that credited it); missing = not yet
 * @param playerId     an opaque key for the agency, stable for the game, that intelligence operations
 *                     name their target by ({@code design.md} &sect;2.9); never the account id
 * @author Mario Koehler
 */
public record RivalSummary(String agencyName, String directorName, Archetype archetype, boolean you,
                           Map<String, Integer> goalTurns, String playerId) {

    /**
     * Creates a summary, copying the goal turns into the {@link LinkedHashMap} Kryo has registered.
     *
     * @param agencyName   the agency
     * @param directorName its director
     * @param archetype    its archetype
     * @param you          whether this is the player who asked
     * @param goalTurns    the declared goals done, with their turns
     * @return the summary
     */
    public static RivalSummary of(String agencyName, String directorName, Archetype archetype, boolean you,
                                  Map<String, Integer> goalTurns) {
        return of(agencyName, directorName, archetype, you, goalTurns, "");
    }

    /**
     * Creates a summary with the agency's key.
     *
     * @param agencyName   the agency
     * @param directorName its director
     * @param archetype    its archetype
     * @param you          whether this is the player who asked
     * @param goalTurns    the declared goals done, with their turns
     * @param playerId     the agency's opaque key
     * @return the summary
     */
    public static RivalSummary of(String agencyName, String directorName, Archetype archetype, boolean you,
                                  Map<String, Integer> goalTurns, String playerId) {
        return new RivalSummary(agencyName, directorName, archetype, you, new LinkedHashMap<>(goalTurns),
            playerId == null ? "" : playerId);
    }
}
