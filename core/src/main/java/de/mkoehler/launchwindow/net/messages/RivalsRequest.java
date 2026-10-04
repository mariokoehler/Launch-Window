package de.mkoehler.launchwindow.net.messages;

/**
 * Asks for every player's summary in the race ({@code ui-spec.md} &sect;4.11, gap G3): agency,
 * director, archetype and the turn each declared goal was done. Needs a slot in the game.
 *
 * @author Mario Koehler
 */
public record RivalsRequest() {
}
