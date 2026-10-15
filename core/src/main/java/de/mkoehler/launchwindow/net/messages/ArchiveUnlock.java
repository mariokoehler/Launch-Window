package de.mkoehler.launchwindow.net.messages;

/**
 * An archive goal reached in the game: the first turn any agency held it, achieved or credited
 * through a cascade ({@code design.md} &sect;2.12, G15). Once the race is decided, every page nobody
 * had reached opens too, on the turn it was decided ({@code atTheEnd}).
 *
 * @param goalId   the goal
 * @param turn     the first turn any agency held it, or the turn the race was decided
 * @param byYou    whether the player who asked was among the agencies that reached it that turn
 * @param atTheEnd whether it opened because the race was decided, not because anyone reached it
 * @author Mario Koehler
 */
public record ArchiveUnlock(String goalId, int turn, boolean byYou, boolean atTheEnd) {

    /**
     * A goal some agency reached.
     *
     * @param goalId the goal
     * @param turn   the first turn any agency held it
     * @param byYou  whether the player who asked was among the first
     */
    public ArchiveUnlock(String goalId, int turn, boolean byYou) {
        this(goalId, turn, byYou, false);
    }
}
