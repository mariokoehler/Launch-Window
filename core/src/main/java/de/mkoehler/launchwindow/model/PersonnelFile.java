package de.mkoehler.launchwindow.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A file on one of a rival's people ({@code design.md} &sect;2.9, level 3): who they are, their
 * skills as ranges, one of their traits, their loyalty as a band, and the ambition they've said
 * (phase 4; never one still hidden).
 *
 * @param personId   the person's id in the rival's agency (ids only count within one agency)
 * @param name       their name
 * @param portraitId their portrait
 * @param group      their group, by {@code PersonnelGroup} name
 * @param age        their age when filed
 * @param turn       the game turn the file dates from
 * @param skills     their skills, {@link IntelFact.Kind#SKILL} facts
 * @param trait      one of their traits, or {@code null} for no notable traits
 * @param loyalty    their loyalty as a band ("Low", "Middling", "High"), or {@code ""} in an older file
 * @param ambition   the ambition they've said, or {@code null}
 * @param disappointed whether an ambition of theirs ran out lately
 * @author Mario Koehler
 */
public record PersonnelFile(String personId, String name, String portraitId, String group, int age, int turn,
                            List<IntelFact> skills, Trait trait, String loyalty, Ambition ambition,
                            boolean disappointed) {

    /**
     * Checks the ids and copies the skills.
     *
     * @throws NullPointerException if {@code personId} or {@code group} is {@code null}
     */
    public PersonnelFile {
        Objects.requireNonNull(personId, "personId must not be null");
        Objects.requireNonNull(group, "group must not be null");
        skills = skills == null ? new ArrayList<>() : new ArrayList<>(skills);
        loyalty = loyalty == null ? "" : loyalty;
    }

    /**
     * A file without the phase 4 fields.
     *
     * @param personId   the person's id in the rival's agency
     * @param name       their name
     * @param portraitId their portrait
     * @param group      their group
     * @param age        their age when filed
     * @param turn       the game turn the file dates from
     * @param skills     their skills
     * @param trait      one of their traits, or {@code null}
     */
    public PersonnelFile(String personId, String name, String portraitId, String group, int age, int turn,
            List<IntelFact> skills, Trait trait) {
        this(personId, name, portraitId, group, age, turn, skills, trait, "", null, false);
    }
}
