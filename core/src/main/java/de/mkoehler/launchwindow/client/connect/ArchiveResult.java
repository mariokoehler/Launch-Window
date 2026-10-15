package de.mkoehler.launchwindow.client.connect;

import de.mkoehler.launchwindow.net.messages.ArchiveUnlock;

import java.util.List;

/**
 * What an archive fetch came to (G15).
 *
 * @param ok      whether the answer came back
 * @param message the server's or connection's reason, or {@code ""}
 * @param unlocks the archive's goals reached in the game, when {@code ok}
 * @author Mario Koehler
 */
public record ArchiveResult(boolean ok, String message, List<ArchiveUnlock> unlocks) {

    /**
     * Copies the list.
     */
    public ArchiveResult {
        unlocks = unlocks == null ? List.of() : List.copyOf(unlocks);
    }
}
