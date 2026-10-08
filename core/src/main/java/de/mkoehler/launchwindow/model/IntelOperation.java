package de.mkoehler.launchwindow.model;

import java.util.Objects;

/**
 * An intelligence operation queued for this season ({@code design.md} &sect;2.9), settled at the
 * resolution. It uses one of the season's actions until it's cancelled.
 *
 * @param id       its id, unique within the agency
 * @param kind     what it is
 * @param targetId the rival agency's player id
 * @param area     an assessment's area, or {@code null} for a personnel file
 * @param group    a personnel file's group, by {@code PersonnelGroup} name, or {@code null}
 * @param personId a personnel file's named person, or {@code null} for the group's most skilled without a
 *                 file; an offer's person
 * @param raisePercent an offer's raise over the person's salary, in percent; 0 for the others
 * @author Mario Koehler
 */
public record IntelOperation(String id, Kind kind, String targetId, IntelArea area, String group, String personId,
                             int raisePercent) {

    /**
     * The kinds of queued operation (the background check isn't one: it answers at once).
     */
    public enum Kind {
        /** One area of one rival (level 2). */
        ASSESSMENT,
        /** One of a rival's people (level 3). */
        PERSONNEL_FILE,
        /**
         * An offer to one of a rival's people, made from a personnel file (poaching, {@code design.md}
         * &sect;2.2(d)). One a season; it uses no action.
         */
        OFFER
    }

    /**
     * Checks the required fields.
     *
     * @throws NullPointerException if {@code id}, {@code kind} or {@code targetId} is {@code null}
     */
    public IntelOperation {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(targetId, "targetId must not be null");
    }

    /**
     * An assessment or a personnel file.
     *
     * @param id       its id
     * @param kind     what it is
     * @param targetId the rival agency's player id
     * @param area     an assessment's area
     * @param group    a personnel file's group
     * @param personId a personnel file's named person
     */
    public IntelOperation(String id, Kind kind, String targetId, IntelArea area, String group, String personId) {
        this(id, kind, targetId, area, group, personId, 0);
    }
}
