package de.mkoehler.launchwindow.model;

import java.util.List;
import java.util.Objects;

/**
 * What one person wants ({@code design.md} &sect;2.2(d), phase 4): an ambition, once the person has
 * said it, and how long they'll wait for it.
 *
 * @param kind        the ambition
 * @param ref         what it's about: a component for {@link Kind#COMPONENT_CAP}, else {@code ""}
 * @param seasonsLeft the seasons of patience left
 * @param window      the seasons of patience it started with
 * @param saidTurn    the game turn the person said it
 * @author Mario Koehler
 */
public record Ambition(Kind kind, String ref, int seasonsLeft, int window, int saidTurn) {

    /**
     * The ambitions, each for one group, with its patience window in seasons before traits.
     */
    public enum Kind {
        /** Fly as crew on a crewed mission. */
        FLY_IN_SPACE("ASTRONAUT", 5),
        /** Fly as crew on a crewed orbital mission. */
        FLY_TO_ORBIT("ASTRONAUT", 6),
        /** Fly in the second season or later of a crewed orbital mission. */
        DAYS_IN_ORBIT("ASTRONAUT", 7),
        /** Be aboard a mission that settles a goal first in the game. */
        BE_FIRST("ASTRONAUT", 8),
        /** Fly as crew on a configuration of two launches or more. */
        JOINT_FLIGHT("ASTRONAUT", 7),
        /** Be on a control slot of a crewed orbital mission. */
        CONSOLES_ORBITAL("MISSION_CONTROL", 6),
        /** Be on a control slot of a mission that settles a goal first in the game. */
        CONSOLES_FIRST("MISSION_CONTROL", 8),
        /** Earn the Expert trait in their best skill. */
        BECOME_EXPERT("MISSION_CONTROL", 8),
        /** Be on the team when a given component reaches its R&amp;D cap. */
        COMPONENT_CAP("SET", 6),
        /** A component they worked on flies on a mission that settles a goal first in the game. */
        FIRST_HARDWARE("SET", 8),
        /** Be the agency's most skilled engineer in their own best skill. */
        TOP_ENGINEER("SET", 6);

        private final String group;
        private final int window;

        Kind(String group, int window) {
            this.group = group;
            this.window = window;
        }

        /**
         * Returns the group this ambition belongs to.
         *
         * @return the group, by {@code PersonnelGroup} name
         */
        public String group() {
            return group;
        }

        /**
         * Returns the patience window before traits.
         *
         * @return the seasons
         */
        public int window() {
            return window;
        }

        /**
         * Returns the ambitions a group can have.
         *
         * @param group the group, by {@code PersonnelGroup} name
         * @return the ambitions, in declaration order
         */
        public static List<Kind> forGroup(String group) {
            return java.util.Arrays.stream(values()).filter(kind -> kind.group.equals(group)).toList();
        }
    }

    /**
     * Checks the kind and keeps a non-null reference.
     *
     * @throws NullPointerException if {@code kind} is {@code null}
     */
    public Ambition {
        Objects.requireNonNull(kind, "kind must not be null");
        ref = ref == null ? "" : ref;
    }

    /**
     * Returns this ambition with fewer seasons left.
     *
     * @param seasons how many seasons to take off
     * @return the ambition
     */
    public Ambition shortened(int seasons) {
        return new Ambition(kind, ref, Math.max(0, seasonsLeft - seasons), window, saidTurn);
    }
}
