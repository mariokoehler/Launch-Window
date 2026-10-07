package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.model.Trait;

/**
 * A trait shown after someone worked in teams for a while ({@code design.md} &sect;2.2(d)).
 *
 * @param group       the person's group
 * @param personnelId their id
 * @param name        their name, as it was
 * @param trait       the trait
 * @param teamKind    what they were working on: {@code "rd"} or {@code "mission"}, or {@code null}
 * @param subjectId   the component (R&amp;D) or mission configuration they were working on, or {@code null}
 * @param workSeasons their work seasons so far
 * @author Mario Koehler
 */
public record TraitRevealed(
    PersonnelGroup group,
    String personnelId,
    String name,
    Trait trait,
    String teamKind,
    String subjectId,
    int workSeasons
) implements ResolutionEvent {
}
