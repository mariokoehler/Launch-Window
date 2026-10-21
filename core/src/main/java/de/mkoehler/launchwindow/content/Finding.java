package de.mkoehler.launchwindow.content;

import java.util.List;
import java.util.Objects;

/**
 * <p>A finding ({@code design.md} &sect;2.13): a science goal whose result makes some mission steps
 * safer for an agency that knows it, by earning the goal or because someone published it. Read from
 * the pack's {@code findings.json}, which is a rules file: it changes the odds, so it's hashed.</p>
 *
 * <p>A step matches when its kind ({@code de.mkoehler.launchwindow.rules.Findings#kind}) is one of a
 * match's kinds, its flight's body is the match's body (any when none is given), and it carries crew
 * when the match asks for a crewed step (either when none is given).</p>
 *
 * @param goalId  the science goal, also the finding's id
 * @param matches the steps it makes safer
 * @author Mario Koehler
 */
public record Finding(String goalId, List<StepMatch> matches) {

    /**
     * Checks the fields.
     *
     * @throws NullPointerException     if a field is {@code null}
     * @throws IllegalArgumentException if there are no matches
     */
    public Finding {
        Objects.requireNonNull(goalId, "goalId must not be null");
        matches = List.copyOf(Objects.requireNonNull(matches, "matches must not be null"));
        if (matches.isEmpty()) {
            throw new IllegalArgumentException("finding " + goalId + " makes no step safer");
        }
    }

    /**
     * Steps a finding makes safer.
     *
     * @param kinds  the step kinds, with the pack's numbering and a/b variants left off; a kind ending
     *               in {@code *} matches every kind it starts
     * @param body   the body the flight's program is browsed under, or {@code null} for any
     * @param crewed {@code true} for crewed steps only, {@code false} for uncrewed only, {@code null}
     *               for either
     */
    public record StepMatch(List<String> kinds, CelestialBody body, Boolean crewed) {

        /**
         * Checks the kinds.
         *
         * @throws NullPointerException     if {@code kinds} is {@code null}
         * @throws IllegalArgumentException if there are none
         */
        public StepMatch {
            kinds = List.copyOf(Objects.requireNonNull(kinds, "kinds must not be null"));
            if (kinds.isEmpty()) {
                throw new IllegalArgumentException("a step match needs at least one kind");
            }
        }

        /**
         * Whether a step matches.
         *
         * @param kind      the step's kind
         * @param stepBody  the body of the step's flight
         * @param stepCrew  whether the step carries crew
         * @return whether it matches
         */
        public boolean matches(String kind, CelestialBody stepBody, boolean stepCrew) {
            if (body != null && body != stepBody) {
                return false;
            }
            if (crewed != null && crewed != stepCrew) {
                return false;
            }
            for (String each : kinds) {
                if (each.endsWith("*") ? kind.startsWith(each.substring(0, each.length() - 1)) : kind.equals(each)) {
                    return true;
                }
            }
            return false;
        }
    }
}
