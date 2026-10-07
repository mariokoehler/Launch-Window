package de.mkoehler.launchwindow.model;

import java.util.Objects;

/**
 * One of a person's traits and whether the player knows it yet ({@code design.md} &sect;2.2(d)).
 *
 * @param trait    the trait
 * @param revealAt the work seasons after which it shows
 * @param revealed whether it has shown
 * @author Mario Koehler
 */
public record TraitState(Trait trait, int revealAt, boolean revealed) {

    /**
     * Checks the trait.
     *
     * @throws NullPointerException if {@code trait} is {@code null}
     */
    public TraitState {
        Objects.requireNonNull(trait, "trait must not be null");
    }

    /**
     * Returns this trait, shown.
     *
     * @return the revealed copy
     */
    public TraitState reveal() {
        return new TraitState(trait, revealAt, true);
    }
}
