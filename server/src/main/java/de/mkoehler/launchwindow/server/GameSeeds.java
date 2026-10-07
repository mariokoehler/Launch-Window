package de.mkoehler.launchwindow.server;

import java.util.SplittableRandom;

/**
 * <p>Derives the seeds a game's randomness flows from, all from the one game seed in
 * {@code game-meta.json}: a new player's first candidate pools, and each player's
 * season pass for each turn. The same inputs always give the same seed, so a turn
 * re-resolved after a crash comes out the same ({@code design.md} &sect;5, M4).</p>
 *
 * <p>The inputs go through {@link SplittableRandom}'s output mixing rather than
 * being used directly: {@code java.util.Random} gives correlated first draws for
 * nearby seeds, and consecutive turns are nearby.</p>
 *
 * @author Mario Koehler
 */
public final class GameSeeds {

    private GameSeeds() {
    }

    /**
     * Returns the seed for a new player's starting state.
     *
     * @param gameSeed  the game's seed
     * @param accountId the player's account id
     * @return the seed
     */
    public static long forNewPlayer(long gameSeed, String accountId) {
        return new SplittableRandom(gameSeed ^ mixString(accountId)).nextLong();
    }

    /**
     * Returns the seed for one player's season pass on one turn.
     *
     * @param gameSeed  the game's seed
     * @param gameTurn  the turn
     * @param accountId the player's account id
     * @return the seed
     */
    public static long forSeason(long gameSeed, long gameTurn, String accountId) {
        SplittableRandom byTurn = new SplittableRandom(gameSeed + gameTurn * 0x9E3779B97F4A7C15L);
        return new SplittableRandom(byTurn.nextLong() ^ mixString(accountId)).nextLong();
    }

    /**
     * Returns the opaque key other players know an agency by ({@code design.md} &sect;2.9): stable for
     * the game, and never the account id, which stays on the server.
     *
     * @param gameSeed  the game's seed
     * @param accountId the account
     * @return the key
     */
    public static String rivalKey(long gameSeed, String accountId) {
        return "a" + Long.toHexString(new SplittableRandom(gameSeed ^ 0x5EC2_E7A6_E7C1_1957L ^ mixString(accountId))
            .nextLong());
    }

    private static long mixString(String value) {
        long hash = 1125899906842597L;
        for (int i = 0; i < value.length(); i++) {
            hash = 31 * hash + value.charAt(i);
        }
        return new SplittableRandom(hash).nextLong();
    }
}
