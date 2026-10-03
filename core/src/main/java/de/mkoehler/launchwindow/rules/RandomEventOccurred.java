package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.model.RandomEventKind;

/**
 * <p>Reports one random event that applied during a {@link RandomEvents#advanceSeason}
 * pass. A matching {@link de.mkoehler.launchwindow.model.NewsEntry} is appended to
 * the player's news log at the same time. See {@code design.md} &sect;2.7.</p>
 *
 * @param kind      which event applied
 * @param subjectId the affected personnel, component, or group name, or {@code null}
 * @param magnitude the rolled magnitude, as recorded in the news entry
 * @param turn      the game turn it applied on
 * @author Mario Koehler
 */
public record RandomEventOccurred(RandomEventKind kind, String subjectId, double magnitude, int turn)
    implements ResolutionEvent {
}
