package de.mkoehler.launchwindow.content;

import de.mkoehler.launchwindow.model.AstronautSkill;
import de.mkoehler.launchwindow.model.MissionControlSkill;

import java.util.Objects;

/**
 * <p>One control slot in a mission configuration's mission-control layout - a
 * titled seat with an employee type and a skill, as in BASPM ({@code baspm.md}
 * &sect;10.11). A slot accepts any employee of its type; the skill decides which
 * of that person's values a step involving the slot reads. CAPCOM is a slot of
 * type {@link Kind#ASTRONAUT}: an astronaut on the ground.</p>
 *
 * @param title the slot's title, for example {@code BOOSTER} or {@code CAPCOM}
 * @param kind  which group fills it
 * @param skill the skill read: a {@link MissionControlSkill} name or
 *              {@value #FLIGHT_DIRECTOR} for a mission-control slot, an
 *              {@link AstronautSkill} name for an astronaut slot
 * @author Mario Koehler
 */
public record StaffingSlot(String title, Kind kind, String skill) {

    /** The skill name of a Flight Director slot: the average of a controller's five skills. */
    public static final String FLIGHT_DIRECTOR = "FLIGHT_DIRECTOR";

    /** Which group fills a slot. */
    public enum Kind {
        /** A flight controller. */
        MISSION_CONTROL,
        /** An astronaut on the ground, such as CAPCOM. */
        ASTRONAUT
    }

    /**
     * Validates the slot.
     *
     * @throws NullPointerException     if a field is {@code null}
     * @throws IllegalArgumentException if the title is blank or the skill doesn't
     *                                   belong to the slot's kind
     */
    public StaffingSlot {
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(skill, "skill must not be null");
        if (title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        boolean valid = switch (kind) {
            case MISSION_CONTROL -> FLIGHT_DIRECTOR.equals(skill) || isEnumName(MissionControlSkill.class, skill);
            case ASTRONAUT -> isEnumName(AstronautSkill.class, skill);
        };
        if (!valid) {
            throw new IllegalArgumentException("skill " + skill + " doesn't fit a " + kind + " slot");
        }
    }

    /**
     * A mission-control slot reading one controller skill.
     *
     * @param title the title
     * @param skill the skill
     * @return the slot
     */
    public static StaffingSlot controller(String title, MissionControlSkill skill) {
        return new StaffingSlot(title, Kind.MISSION_CONTROL, skill.name());
    }

    /**
     * A Flight Director slot.
     *
     * @param title the title
     * @return the slot
     */
    public static StaffingSlot flightDirector(String title) {
        return new StaffingSlot(title, Kind.MISSION_CONTROL, FLIGHT_DIRECTOR);
    }

    /**
     * An astronaut slot on the ground, such as CAPCOM.
     *
     * @param title the title
     * @param skill the astronaut skill read
     * @return the slot
     */
    public static StaffingSlot astronaut(String title, AstronautSkill skill) {
        return new StaffingSlot(title, Kind.ASTRONAUT, skill.name());
    }

    /**
     * Returns whether this is a Flight Director slot.
     *
     * @return {@code true} for a Flight Director slot
     */
    public boolean flightDirectorSlot() {
        return kind == Kind.MISSION_CONTROL && FLIGHT_DIRECTOR.equals(skill);
    }

    private static <E extends Enum<E>> boolean isEnumName(Class<E> type, String name) {
        for (E value : type.getEnumConstants()) {
            if (value.name().equals(name)) {
                return true;
            }
        }
        return false;
    }
}
