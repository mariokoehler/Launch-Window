package de.mkoehler.launchwindow.model;

import java.util.Objects;

/**
 * How two employees get on ({@code design.md} &sect;2.2(d)): it builds over every season they
 * share a team, across projects and years, and never resets. The two ids are kept in sorted order.
 *
 * @param first           the smaller of the two ids
 * @param second          the larger
 * @param rapport         from -100 (they clash) to +100 (they work well together)
 * @param affinity        which way and how fast it drifts, -1 to +1; hidden from the player
 * @param seasonsTogether the work seasons they've shared
 * @param shown           whether the player knows about it (it once passed the threshold)
 * @author Mario Koehler
 */
public record Rapport(String first, String second, double rapport, double affinity, int seasonsTogether,
                      boolean shown) {

    /**
     * Checks the ids.
     *
     * @throws NullPointerException if either id is {@code null}
     */
    public Rapport {
        Objects.requireNonNull(first, "first must not be null");
        Objects.requireNonNull(second, "second must not be null");
    }

    /**
     * Returns whether this pair is the two given people, in either order.
     *
     * @param a one id
     * @param b the other
     * @return whether it's them
     */
    public boolean isPair(String a, String b) {
        return first.equals(a) && second.equals(b) || first.equals(b) && second.equals(a);
    }

    /**
     * Returns whether one of the two is the given person.
     *
     * @param id the id
     * @return whether they're in this pair
     */
    public boolean involves(String id) {
        return first.equals(id) || second.equals(id);
    }
}
