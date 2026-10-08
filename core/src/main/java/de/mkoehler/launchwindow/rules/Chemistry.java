package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.AstronautSkill;
import de.mkoehler.launchwindow.model.InProgressMission;
import de.mkoehler.launchwindow.model.MissionControlOfficer;
import de.mkoehler.launchwindow.model.MissionControlSkill;
import de.mkoehler.launchwindow.model.Personnel;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.Rapport;
import de.mkoehler.launchwindow.model.SetSkill;
import de.mkoehler.launchwindow.model.SetSpecialist;
import de.mkoehler.launchwindow.model.Trait;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.ToDoubleFunction;

/**
 * A team's chemistry ({@code design.md} &sect;2.2(d)): how well it works together, from -1 to +1,
 * and what that does to its output, 1 + 0.15 &times; chemistry. One function for every caller:
 * the season's resolution uses the real traits and rapport; the client's previews ask for the
 * known part only (revealed traits, shown relationships), so a preview equals the resolution for
 * everything the player knows and never gives away what's hidden.
 *
 * @author Mario Koehler
 */
public final class Chemistry {

    /** How much a team's output moves at the extremes of chemistry: 15 %. */
    public static final double OUTPUT_BAND = 0.15;

    /** What each Team player adds to the team's chemistry. */
    public static final double TEAM_PLAYER = 0.15;

    /** What a Loner adds when working alone. */
    public static final double LONER_ALONE = 0.6;

    /** What each Loner adds in a team of three or more. */
    public static final double LONER_CROWDED = -0.3;

    /** What someone who must be the best adds when they're the team's most skilled. */
    public static final double MUST_BE_BEST_ON_TOP = 0.2;

    /** What someone who must be the best adds when they aren't. */
    public static final double MUST_BE_BEST_OUTDONE = -0.3;

    private Chemistry() {
    }

    /**
     * One reason for a team's chemistry, for the indicator's breakdown.
     *
     * @param kind   what it is: {@code "pair"} or a trait's name
     * @param ids    the people it's about
     * @param amount what it adds to the chemistry
     */
    public record Reason(String kind, List<String> ids, double amount) {

        /**
         * Copies the ids.
         */
        public Reason {
            ids = List.copyOf(ids);
        }
    }

    /**
     * A team's chemistry.
     *
     * @param value          from -1 to +1
     * @param reasons        what made it, pairs first
     * @param unknownFactors whether part of it may still be hidden from the player (only ever set
     *                       on a known-only result)
     */
    public record Result(double value, List<Reason> reasons, boolean unknownFactors) {

        /**
         * Copies the reasons.
         */
        public Result {
            reasons = List.copyOf(reasons);
        }

        /**
         * Returns what the chemistry multiplies the team's output by.
         *
         * @return 1 + 0.15 &times; chemistry
         */
        public double multiplier() {
            return 1 + OUTPUT_BAND * value;
        }
    }

    /** The neutral chemistry of nobody, or of someone without traits working alone. */
    public static final Result NEUTRAL = new Result(0, List.of(), false);

    /**
     * Returns a team's chemistry.
     *
     * @param snapshot  the player
     * @param members   the team, each once
     * @param skill     each member's skill for "most skilled", compared within their own group
     * @param knownOnly whether to count only what the player knows (for previews)
     * @return the chemistry
     */
    public static Result of(PlayerSnapshot snapshot, List<? extends Personnel> members,
            ToDoubleFunction<Personnel> skill, boolean knownOnly) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        if (members.isEmpty()) {
            return NEUTRAL;
        }
        List<Reason> reasons = new ArrayList<>();
        double pairs = 0;
        int pairCount = 0;
        for (int i = 0; i < members.size(); i++) {
            for (int j = i + 1; j < members.size(); j++) {
                pairCount++;
                Rapport pair = Personality.rapport(snapshot, members.get(i).getId(), members.get(j).getId());
                if (pair == null || knownOnly && !pair.shown()) {
                    continue;
                }
                pairs += pair.rapport() / Personality.RAPPORT_MAX;
                if (pair.shown()) {
                    reasons.add(new Reason("pair", List.of(pair.first(), pair.second()), pair.rapport() / Personality.RAPPORT_MAX));
                }
            }
        }
        double value = pairCount == 0 ? 0 : pairs / pairCount;
        // A shown pair's reason carries its share of the average.
        if (pairCount > 1) {
            List<Reason> scaled = new ArrayList<>();
            for (Reason reason : reasons) {
                scaled.add(new Reason(reason.kind(), reason.ids(), reason.amount() / pairCount));
            }
            reasons = scaled;
        }
        for (Personnel person : members) {
            if (counts(person, Trait.TEAM_PLAYER, knownOnly)) {
                value += TEAM_PLAYER;
                reasons.add(new Reason(Trait.TEAM_PLAYER.name(), List.of(person.getId()), TEAM_PLAYER));
            }
            if (counts(person, Trait.LONER, knownOnly)) {
                double loner = members.size() == 1 ? LONER_ALONE : members.size() >= 3 ? LONER_CROWDED : 0;
                if (loner != 0) {
                    value += loner;
                    reasons.add(new Reason(Trait.LONER.name(), List.of(person.getId()), loner));
                }
            }
            if (counts(person, Trait.MUST_BE_THE_BEST, knownOnly) && members.size() > 1) {
                double best = mostSkilled(person, members, skill) ? MUST_BE_BEST_ON_TOP : MUST_BE_BEST_OUTDONE;
                value += best;
                reasons.add(new Reason(Trait.MUST_BE_THE_BEST.name(), List.of(person.getId()), best));
            }
        }
        value = Math.max(-1, Math.min(1, value));
        boolean unknown = knownOnly && members.stream().anyMatch(person -> !Personality.fullyKnown(person));
        return new Result(value, reasons, unknown);
    }

    /**
     * Returns the chemistry of a component's R&amp;D team.
     *
     * @param snapshot  the player
     * @param team      the engineers on it
     * @param skill     the component's skill
     * @param knownOnly whether to count only what the player knows
     * @return the chemistry
     */
    public static Result research(PlayerSnapshot snapshot, List<SetSpecialist> team, SetSkill skill,
            boolean knownOnly) {
        return of(snapshot, team, person -> ((SetSpecialist) person).getSkill(skill), knownOnly);
    }

    /**
     * A mission's two teams' chemistry (user, 2026-10-08): the crew with CAPCOM, and the flight
     * controllers on the consoles. Each scales what its own people bring to a step.
     *
     * @param crew     the astronauts: the crew and anyone at CAPCOM
     * @param consoles the flight controllers
     */
    public record MissionChemistry(Result crew, Result consoles) {

        /**
         * Returns what a team's chemistry multiplies a person's contribution by.
         *
         * @param astronaut whether the person is an astronaut (crew or CAPCOM), else a controller
         * @return the multiplier
         */
        public double multiplier(boolean astronaut) {
            return (astronaut ? crew : consoles).multiplier();
        }
    }

    /**
     * Returns the chemistry of a mission's two teams: the crew with CAPCOM, and the controllers.
     *
     * @param snapshot  the player
     * @param mission   the mission, flying or planned
     * @param knownOnly whether to count only what the player knows
     * @return the chemistry
     */
    public static MissionChemistry mission(PlayerSnapshot snapshot, InProgressMission mission, boolean knownOnly) {
        List<Personnel> team = MissionProgress.missionTeam(snapshot, mission);
        return new MissionChemistry(of(snapshot, crewTeam(team), Chemistry::bestSkill, knownOnly),
            of(snapshot, consoleTeam(team), Chemistry::bestSkill, knownOnly));
    }

    /**
     * Returns a mission team's astronauts: the crew and CAPCOM, in the team's order.
     *
     * @param team the mission's people
     * @return the crew team
     */
    public static List<Personnel> crewTeam(List<? extends Personnel> team) {
        return team.stream().filter(Astronaut.class::isInstance).map(Personnel.class::cast).toList();
    }

    /**
     * Returns a mission team's flight controllers, in the team's order.
     *
     * @param team the mission's people
     * @return the console team
     */
    public static List<Personnel> consoleTeam(List<? extends Personnel> team) {
        return team.stream().filter(person -> !(person instanceof Astronaut)).map(Personnel.class::cast).toList();
    }

    /**
     * Returns someone's best skill in their own group, for comparing mission teammates.
     *
     * @param person the person
     * @return the skill, 0-100
     */
    public static double bestSkill(Personnel person) {
        double best = 0;
        if (person instanceof Astronaut astronaut) {
            for (AstronautSkill skill : AstronautSkill.values()) {
                best = Math.max(best, astronaut.getSkill(skill));
            }
        } else if (person instanceof MissionControlOfficer officer) {
            for (MissionControlSkill skill : MissionControlSkill.values()) {
                best = Math.max(best, officer.getSkill(skill));
            }
        } else if (person instanceof SetSpecialist specialist) {
            for (SetSkill skill : SetSkill.values()) {
                best = Math.max(best, specialist.getSkill(skill));
            }
        }
        return best;
    }

    private static boolean counts(Personnel person, Trait trait, boolean knownOnly) {
        return knownOnly ? Personality.known(person, trait) : Personality.has(person, trait);
    }

    private static boolean mostSkilled(Personnel person, List<? extends Personnel> members,
            ToDoubleFunction<Personnel> skill) {
        double own = skill.applyAsDouble(person);
        for (Personnel other : members) {
            if (other != person && other.getClass() == person.getClass() && skill.applyAsDouble(other) > own) {
                return false;
            }
        }
        return true;
    }
}
