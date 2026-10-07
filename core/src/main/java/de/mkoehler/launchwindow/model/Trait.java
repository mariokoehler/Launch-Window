package de.mkoehler.launchwindow.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * A personality trait ({@code design.md} &sect;2.2(d)): hidden when someone is hired, shown once
 * they've worked in teams for a while. Most cut both ways. A personal trait changes the person's
 * own numbers; a team trait acts through the people around them (rapport and chemistry).
 *
 * @author Mario Koehler
 */
public enum Trait {

    /** More R&amp;D gain; morale falls fast when idle. */
    WORKAHOLIC(false, "ASTRONAUT", "SET", "MISSION_CONTROL"),
    /** The season's morale change is halved, both ways. */
    STEADY(false, "ASTRONAUT", "SET", "MISSION_CONTROL"),
    /** Trains faster; morale falls after a long time without training. */
    AMBITIOUS(false, "ASTRONAUT", "SET", "MISSION_CONTROL"),
    /** Astronauts: skills grow more on flights; morale falls when others fly. */
    GLORY_SEEKER(false, "ASTRONAUT"),
    /** Engineers: less R&amp;D gain, but a higher R&amp;D cap while on the team. */
    PERFECTIONIST(false, "SET"),
    /** Astronauts and controllers: a bonus on a mission's saving throws. */
    COOL_UNDER_PRESSURE(false, "ASTRONAUT", "MISSION_CONTROL"),
    /** A bonus early on a component, a penalty after many seasons on the same one. */
    RESTLESS(false, "ASTRONAUT", "SET", "MISSION_CONTROL"),
    /** More training gain. */
    QUICK_STUDY(false, "ASTRONAUT", "SET", "MISSION_CONTROL"),
    /** Less skilled teammates learn faster. */
    MENTOR(true, "ASTRONAUT", "SET", "MISSION_CONTROL"),
    /** A bonus alone, a penalty in a team of three or more. */
    LONER(true, "ASTRONAUT", "SET", "MISSION_CONTROL"),
    /** Rapport drifts up faster with everyone. */
    EASY_GOING(true, "ASTRONAUT", "SET", "MISSION_CONTROL"),
    /** Rapport drifts down faster with everyone. */
    ABRASIVE(true, "ASTRONAUT", "SET", "MISSION_CONTROL"),
    /** Works harder as the team's most skilled, sulks when not. */
    MUST_BE_THE_BEST(true, "ASTRONAUT", "SET", "MISSION_CONTROL"),
    /** Lifts the whole team's chemistry a little. */
    TEAM_PLAYER(true, "ASTRONAUT", "SET", "MISSION_CONTROL");

    private final boolean teamTrait;
    private final Set<String> groups;

    Trait(boolean teamTrait, String... groups) {
        this.teamTrait = teamTrait;
        this.groups = Set.of(groups);
    }

    /**
     * Returns whether this trait acts through the team (rapport and chemistry) rather than on the
     * person's own numbers.
     *
     * @return whether it's a team trait
     */
    public boolean teamTrait() {
        return teamTrait;
    }

    /**
     * Returns whether someone in a personnel group can have this trait.
     *
     * @param group the group's name: {@code ASTRONAUT}, {@code SET} or {@code MISSION_CONTROL}
     * @return whether it's possible
     */
    public boolean allowedFor(String group) {
        return groups.contains(group);
    }

    /**
     * Returns the traits someone in a personnel group can have, in declaration order.
     *
     * @param group the group's name
     * @return the traits
     */
    public static Set<Trait> forGroup(String group) {
        EnumSet<Trait> result = EnumSet.noneOf(Trait.class);
        for (Trait trait : values()) {
            if (trait.allowedFor(group)) {
                result.add(trait);
            }
        }
        return result;
    }
}
