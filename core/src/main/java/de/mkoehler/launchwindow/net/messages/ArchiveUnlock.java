package de.mkoehler.launchwindow.net.messages;

/**
 * An archive goal reached in the game: the first turn any agency held it, achieved or credited
 * through a cascade ({@code design.md} &sect;2.12, G15).
 *
 * @param goalId the goal
 * @param turn   the first turn any agency held it
 * @param byYou  whether the player who asked was among the agencies that reached it that turn
 * @author Mario Koehler
 */
public record ArchiveUnlock(String goalId, int turn, boolean byYou) {
}
