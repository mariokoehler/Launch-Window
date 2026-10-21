package de.mkoehler.launchwindow.net.messages;

/**
 * Asks what publishing a finding now would pay ({@code design.md} &sect;2.13), for the confirmation
 * dialog: the client can't work it out, since it doesn't know which rivals know the finding. Needs a
 * slot in the game.
 *
 * @param goalId the finding
 * @author Mario Koehler
 */
public record PublishPreviewRequest(String goalId) {
}
