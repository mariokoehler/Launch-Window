package de.mkoehler.launchwindow.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A file on one of a rival's people ({@code design.md} &sect;2.9, level 3): who they are, their
 * skills as ranges, and one of their traits. Loyalty and ambition join it with phase 4.
 *
 * @param personId   the person's id in the rival's agency (ids only count within one agency)
 * @param name       their name
 * @param portraitId their portrait
 * @param group      their group, by {@code PersonnelGroup} name
 * @param age        their age when filed
 * @param turn       the game turn the file dates from
 * @param skills     their skills, {@link IntelFact.Kind#SKILL} facts
 * @param trait      one of their traits, or {@code null} for no notable traits
 * @author Mario Koehler
 */
public record PersonnelFile(String personId, String name, String portraitId, String group, int age, int turn,
                            List<IntelFact> skills, Trait trait) {

    /**
     * Checks the ids and copies the skills.
     *
     * @throws NullPointerException if {@code personId} or {@code group} is {@code null}
     */
    public PersonnelFile {
        Objects.requireNonNull(personId, "personId must not be null");
        Objects.requireNonNull(group, "group must not be null");
        skills = skills == null ? new ArrayList<>() : new ArrayList<>(skills);
    }
}
