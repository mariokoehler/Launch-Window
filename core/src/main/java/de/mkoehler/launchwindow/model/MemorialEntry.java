package de.mkoehler.launchwindow.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * One astronaut, or animal passenger, the agency lost, kept for the Museum's memorial
 * ({@code ui-spec.md} &sect;4.11, gap G10). Written when the loss happens; never removed. From phase 4
 * ({@code design.md} &sect;2.2(d)) an astronaut's entry also keeps enough for a person page: the
 * id, skills at the end, the traits the player knew (never hidden ones), when they joined, and the
 * career record.
 *
 * @param name                   the astronaut's name
 * @param age                    their age when lost; 0 for an animal passenger, whose age isn't known
 * @param turn                   the game turn the loss resolved on
 * @param cause                  how they were lost
 * @param missionConfigurationId the mission flown, for a {@link Cause#MISSION} loss, else {@code null}
 * @param stepId                 the step that failed, for a {@link Cause#MISSION} loss or a passenger lost to a
 *                               failure, else {@code null}
 * @param portraitId             the astronaut's portrait, or {@code null} if they had none; never given
 *                               to a new candidate afterwards
 * @param personnelId            the astronaut's id, or {@code null} for a passenger or an older entry
 * @param skills                 skills at the end, by skill name; empty for a passenger or an older entry
 * @param traits                 the traits the player knew of
 * @param joinedTurn             the game turn they joined, or -1 if not known
 * @param career                 the career record at the end, oldest first
 * @author Mario Koehler
 */
public record MemorialEntry(String name, int age, int turn, Cause cause, String missionConfigurationId,
                            String stepId, String portraitId, String personnelId, Map<String, Double> skills,
                            List<Trait> traits, int joinedTurn, List<CareerEntry> career) {

    /**
     * How an astronaut was lost.
     */
    public enum Cause {
        /** A catastrophic mission step: the whole crew is lost. */
        MISSION,
        /** The "perished astronaut" random event: an accident while not flying. */
        ACCIDENT,
        /**
         * An animal passenger that didn't come home (2026-10-05): a flight with no way back, such as
         * Sputnik 2, or a failed flight that was meant to return. Added last: Kryo writes enums by
         * ordinal.
         */
        PASSENGER
    }

    /**
     * Checks the required fields and copies the collections into types Kryo can carry.
     */
    public MemorialEntry {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(cause, "cause must not be null");
        skills = skills == null ? new LinkedHashMap<>() : new LinkedHashMap<>(skills);
        traits = traits == null ? new ArrayList<>() : new ArrayList<>(traits);
        career = career == null ? new ArrayList<>() : new ArrayList<>(career);
    }

    /**
     * An entry without a person page: a passenger.
     *
     * @param name                   the name
     * @param age                    the age, or 0
     * @param turn                   the game turn
     * @param cause                  how
     * @param missionConfigurationId the mission, or {@code null}
     * @param stepId                 the step, or {@code null}
     * @param portraitId             the portrait, or {@code null}
     */
    public MemorialEntry(String name, int age, int turn, Cause cause, String missionConfigurationId, String stepId,
            String portraitId) {
        this(name, age, turn, cause, missionConfigurationId, stepId, portraitId, null, null, null, -1, null);
    }

    /**
     * Returns this entry with a line added to the career record.
     *
     * @param entry the line
     * @return the entry
     */
    public MemorialEntry withCareer(CareerEntry entry) {
        List<CareerEntry> longer = new ArrayList<>(career);
        longer.add(entry);
        return new MemorialEntry(name, age, turn, cause, missionConfigurationId, stepId, portraitId, personnelId,
            skills, traits, joinedTurn, longer);
    }
}
