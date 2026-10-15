package de.mkoehler.launchwindow.net.messages;

import java.util.ArrayList;
import java.util.List;

/**
 * The archive's goals reached in the game so far (G15), or why there's no answer.
 *
 * @param success whether the request was served
 * @param message why it wasn't, or {@code ""}
 * @param unlocks the goals asked about that some agency holds; a goal nobody holds isn't listed
 * @author Mario Koehler
 */
public record ArchiveResponse(boolean success, String message, List<ArchiveUnlock> unlocks) {

    /**
     * The goals reached, copied into the {@link ArrayList} Kryo has registered.
     *
     * @param unlocks the goals reached
     * @return the response
     */
    public static ArchiveResponse of(List<ArchiveUnlock> unlocks) {
        return new ArchiveResponse(true, "", new ArrayList<>(unlocks));
    }

    /**
     * The request couldn't be served.
     *
     * @param message why
     * @return the response
     */
    public static ArchiveResponse failure(String message) {
        return new ArchiveResponse(false, message, new ArrayList<>());
    }
}
