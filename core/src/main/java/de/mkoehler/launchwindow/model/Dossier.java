package de.mkoehler.launchwindow.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * One area of a rival, as an agency assessment found it ({@code design.md} &sect;2.9): dated to the
 * resolution that settled it, and kept until a newer one of the same area replaces it.
 *
 * @param area  the area
 * @param turn  the game turn it dates from
 * @param facts what it found
 * @author Mario Koehler
 */
public record Dossier(IntelArea area, int turn, List<IntelFact> facts) {

    /**
     * Checks the area and copies the facts into a list Kryo can carry.
     *
     * @throws NullPointerException if {@code area} is {@code null}
     */
    public Dossier {
        Objects.requireNonNull(area, "area must not be null");
        facts = facts == null ? new ArrayList<>() : new ArrayList<>(facts);
    }
}
