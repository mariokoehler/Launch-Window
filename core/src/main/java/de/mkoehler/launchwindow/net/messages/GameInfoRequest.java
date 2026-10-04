package de.mkoehler.launchwindow.net.messages;

/**
 * Asks for the game's shared facts (gap G1, {@code ui-spec.md} &sect;6): its content pack, its
 * declared goals, the game date, the next resolution and the result. Needs an accepted handshake
 * but no slot, so the Join Game screen can ask before the player has joined.
 *
 * @author Mario Koehler
 */
public record GameInfoRequest() {
}
