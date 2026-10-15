package de.mkoehler.launchwindow.net.messages;

import java.util.ArrayList;
import java.util.List;

/**
 * Asks when the archive's goals were first reached in the game ({@code design.md} &sect;2.12, G15).
 * Needs a slot in the game. The client names the goals of its own pages, so the server never reads
 * the archive's content.
 *
 * @param goalIds the goals whose pages the client has
 * @author Mario Koehler
 */
public record ArchiveRequest(List<String> goalIds) {

    /**
     * Copies the goals into the {@link ArrayList} Kryo has registered.
     *
     * @param goalIds the goals
     * @return the request
     */
    public static ArchiveRequest of(List<String> goalIds) {
        return new ArchiveRequest(new ArrayList<>(goalIds));
    }
}
