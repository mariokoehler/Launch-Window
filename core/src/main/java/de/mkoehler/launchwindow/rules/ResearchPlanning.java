package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSpecialist;
import de.mkoehler.launchwindow.model.Trait;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.SplittableRandom;
import java.util.random.RandomGenerator;

/**
 * Planning help for R&amp;D, for the client's R&amp;D boards: the reliability a component can expect
 * to gain next season from the engineers on it, where it is headed season by season (and how much
 * that varies), and the best team the player could put on it now.
 * Headless and read-only; it follows {@link ResearchProgress}'s season pass step for step, with
 * each slot's random draw replaced by the middle of its band, so a board's projection is the
 * rules' own average and never a client re-derivation.
 *
 * @author Mario Koehler
 */
public final class ResearchPlanning {

    private ResearchPlanning() {
    }

    /**
     * Returns the reliability a component can expect to gain next season from the engineers on it
     * now: the average of {@link ResearchProgress}'s season pass for this team.
     *
     * @param snapshot    the player
     * @param catalog     the content
     * @param componentId the component
     * @return the expected gain in reliability points, 0 if nobody researches it or it's at its ceiling
     * @throws NullPointerException if an argument is {@code null}
     */
    public static double expectedSeasonGain(PlayerSnapshot snapshot, ContentCatalog catalog, String componentId) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(catalog, "catalog must not be null");
        Objects.requireNonNull(componentId, "componentId must not be null");
        OwnedMissionComponent owned = snapshot.getMissionComponents().get(componentId);
        MissionComponent component = catalog.missionComponent(componentId).orElse(null);
        if (owned == null || component == null) {
            return 0;
        }
        SetSpecialist[] team = team(snapshot, componentId);
        // The chemistry the player can see: revealed traits and shown relationships only.
        double chemistry = Chemistry.research(snapshot, members(team), component.associatedSkill(), true)
            .multiplier();
        return expectedSeasonGain(component, owned.getCurrentReliability(), team,
            snapshot.getRdResultsMultiplier() * owned.getRdFactor() * chemistry);
    }

    /**
     * Returns the expected gain for a given team, slot by slot.
     *
     * @param component  the component
     * @param current    its current reliability
     * @param team       who sits in each of the four slots, {@code null} for an empty one
     * @param multiplier the player's current R&amp;D results multiplier (random events)
     * @return the expected gain in reliability points
     */
    static double expectedSeasonGain(MissionComponent component, double current, SetSpecialist[] team,
                                     double multiplier) {
        double[] skills = new double[team.length];
        for (int slot = 0; slot < team.length; slot++) {
            skills[slot] = team[slot] == null ? 0 : team[slot].getSkill(component.associatedSkill());
        }
        return seasonGain(component, current, team, skills, multiplier, 0, null);
    }

    /**
     * One season of {@link ResearchProgress}'s component pass for a planned team: each slot's draw
     * from {@code random}, or the middle of its band without one.
     *
     * @param skills       each slot's skill this season, in the component's skill
     * @param seasonsAhead how many seasons from the next one this is, for the Restless trait
     */
    private static double seasonGain(MissionComponent component, double current, SetSpecialist[] team,
                                     double[] skills, double multiplier, int seasonsAhead, RandomGenerator random) {
        // Traits as the player knows them: a revealed Perfectionist's higher cap, revealed R&D traits.
        double cap = cap(component, team);
        double nominal = 0;
        List<Double> bands = component.rdSlotBandMaxAmounts();
        for (int slot = 0; slot < team.length; slot++) {
            if (team[slot] == null) {
                continue;
            }
            double raw = random == null
                ? (SkillGrowth.rdSlotBandLower(bands, slot) + SkillGrowth.rdSlotBandUpper(bands, slot)) / 2.0
                : SkillGrowth.drawRdSlotRawAmount(bands, slot, random);
            nominal += SkillGrowth.rdReliabilityGain(raw, current, cap, skills[slot]) * multiplier
                * TraitEffects.rdGain(team[slot], component.id(), true, seasonsAhead);
        }
        if (nominal <= 0) {
            return 0;
        }
        double headroom = Math.max(0.0, cap - current);
        double applied = headroom < ResearchProgress.HEADROOM_STAGNATION_THRESHOLD ? headroom : nominal;
        return Math.min(cap, current + applied) - current;
    }

    private static double cap(MissionComponent component, SetSpecialist[] team) {
        return Math.min(component.maxReliability(),
            component.maxRdReliability() + TraitEffects.capBonus(members(team), true));
    }

    private static List<SetSpecialist> members(SetSpecialist[] team) {
        List<SetSpecialist> members = new ArrayList<>();
        for (SetSpecialist member : team) {
            if (member != null) {
                members.add(member);
            }
        }
        return members;
    }

    /**
     * An engineer leaving the team during a projection.
     *
     * @param engineer the engineer
     * @param season   the season they're gone from: 1 is the next one
     */
    public record Departure(SetSpecialist engineer, int season) {
    }

    /**
     * Where a component's reliability is headed with a team: season by season, the rules' average.
     *
     * @param start       the reliability now
     * @param cap         the R&amp;D cap with this team, as the player knows it
     * @param reliability the expected reliability after each season, index 0 after the next one;
     *                    shorter than {@link #HORIZON_SEASONS} once progress stops
     * @param departures  engineers who retire within the horizon, earliest first
     */
    public record Projection(double start, double cap, double[] reliability, List<Departure> departures) {

        /**
         * Returns how many seasons the average takes to reach a reliability.
         *
         * @param target the reliability
         * @return the seasons, 0 if already there, empty if not within the horizon
         */
        public OptionalInt seasonsTo(double target) {
            if (start >= target - EPSILON) {
                return OptionalInt.of(0);
            }
            for (int season = 0; season < reliability.length; season++) {
                if (reliability[season] >= target - EPSILON) {
                    return OptionalInt.of(season + 1);
                }
            }
            return OptionalInt.empty();
        }

        /**
         * Returns whether the team makes any progress at all.
         *
         * @return whether it does
         */
        public boolean moving() {
            return reliability.length > 0;
        }

        /**
         * Returns the engineers who retire before a season's R&amp;D.
         *
         * @param season the season, 1 for the next one
         * @return the departures by then, earliest first
         */
        public List<Departure> departuresBy(int season) {
            return departures.stream().filter(departure -> departure.season() <= season).toList();
        }
    }

    /**
     * The spread of a projection's random runs for one target.
     *
     * @param low  the tenth percentile, in seasons
     * @param high the ninetieth percentile, in seasons; {@code Integer.MAX_VALUE} when more than one
     *             run in ten doesn't get there within the horizon
     */
    public record Range(int low, int high) {
    }

    /** How far a projection looks ahead: ten years. */
    public static final int HORIZON_SEASONS = 40;

    /** How many random runs a projection's range takes. */
    static final int RANGE_RUNS = 300;

    private static final long RANGE_SEED = 0x5EA50L;

    private static final double EPSILON = 1e-6;

    /**
     * Projects a component's R&amp;D with the engineers on it now.
     *
     * @param snapshot    the player
     * @param catalog     the content
     * @param componentId the component
     * @return the projection, flat if nobody works on it
     * @throws NullPointerException if an argument is {@code null}
     */
    public static Projection project(PlayerSnapshot snapshot, ContentCatalog catalog, String componentId) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(catalog, "catalog must not be null");
        Objects.requireNonNull(componentId, "componentId must not be null");
        MissionComponent component = catalog.missionComponent(componentId).orElseThrow();
        OwnedMissionComponent owned = snapshot.getMissionComponents().get(componentId);
        double current = owned == null ? 0 : owned.getCurrentReliability();
        return project(snapshot, component, current, team(snapshot, componentId));
    }

    /**
     * Projects a component's R&amp;D with a planned team, such as the team now plus a candidate in
     * the next free slot. Each season follows {@link ResearchProgress}'s pass with each slot's band
     * midpoint, as {@link #expectedSeasonGain} does, and between seasons: the engineers' skill grows
     * by its average draw (a revealed Mentor counts), a revealed Restless engineer's freshness wears
     * off, someone reaching retirement age leaves the team
     * ({@link PersonnelLifecycle#seasonsUntilRetirement}), and an event's R&amp;D multiplier ends when
     * its seasons run out. Chemistry is the team's as the player knows it, recomputed when someone
     * leaves. Nobody new joins; morale and rapport stay as they are.
     *
     * @param snapshot  the player
     * @param component the component
     * @param current   its reliability now
     * @param team      who sits in each of the four slots, {@code null} for an empty one
     * @return the projection
     * @throws NullPointerException if an argument is {@code null}
     */
    public static Projection project(PlayerSnapshot snapshot, MissionComponent component, double current,
                                     SetSpecialist[] team) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(component, "component must not be null");
        Objects.requireNonNull(team, "team must not be null");
        double[] average = run(snapshot, component, current, team, null);
        List<Departure> departures = new ArrayList<>();
        for (SetSpecialist member : team) {
            if (member != null) {
                int season = PersonnelLifecycle.seasonsUntilRetirement(member, PersonnelGroup.SET);
                if (season <= HORIZON_SEASONS) {
                    departures.add(new Departure(member, season));
                }
            }
        }
        departures.sort(Comparator.comparingInt(Departure::season));
        return new Projection(current, cap(component, team), average, List.copyOf(departures));
    }

    /**
     * Returns the spread of how many seasons a component takes to reach each of some reliabilities
     * with the engineers on it now; see {@link #ranges(PlayerSnapshot, MissionComponent, double,
     * SetSpecialist[], double...)}.
     *
     * @param snapshot    the player
     * @param catalog     the content
     * @param componentId the component
     * @param targets     the reliabilities
     * @return one range per target, in order
     * @throws NullPointerException if an argument is {@code null}
     */
    public static Range[] ranges(PlayerSnapshot snapshot, ContentCatalog catalog, String componentId,
                                 double... targets) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(catalog, "catalog must not be null");
        Objects.requireNonNull(componentId, "componentId must not be null");
        MissionComponent component = catalog.missionComponent(componentId).orElseThrow();
        OwnedMissionComponent owned = snapshot.getMissionComponents().get(componentId);
        return ranges(snapshot, component, owned == null ? 0 : owned.getCurrentReliability(),
            team(snapshot, componentId), targets);
    }

    /**
     * Returns the spread of how many seasons a component takes to reach each of some reliabilities
     * with a team: the tenth and ninetieth percentile of {@link #RANGE_RUNS} runs of the same
     * projection with each slot's draw random, as in the season pass. The seed is fixed, so a screen
     * shows the same ranges every time it's built.
     *
     * @param snapshot  the player
     * @param component the component
     * @param current   its reliability now
     * @param team      who sits in each slot, {@code null} for an empty one
     * @param targets   the reliabilities
     * @return one range per target, in order
     * @throws NullPointerException if an argument is {@code null}
     */
    public static Range[] ranges(PlayerSnapshot snapshot, MissionComponent component, double current,
                                 SetSpecialist[] team, double... targets) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(component, "component must not be null");
        Objects.requireNonNull(team, "team must not be null");
        int[][] seasons = new int[targets.length][RANGE_RUNS];
        SplittableRandom random = new SplittableRandom(RANGE_SEED);
        for (int run = 0; run < RANGE_RUNS; run++) {
            double[] path = run(snapshot, component, current, team, random);
            for (int target = 0; target < targets.length; target++) {
                seasons[target][run] = Integer.MAX_VALUE;
                if (current >= targets[target] - EPSILON) {
                    seasons[target][run] = 0;
                    continue;
                }
                for (int season = 0; season < path.length; season++) {
                    if (path[season] >= targets[target] - EPSILON) {
                        seasons[target][run] = season + 1;
                        break;
                    }
                }
            }
        }
        Range[] ranges = new Range[targets.length];
        for (int target = 0; target < targets.length; target++) {
            Arrays.sort(seasons[target]);
            ranges[target] = new Range(seasons[target][RANGE_RUNS / 10],
                seasons[target][RANGE_RUNS - 1 - RANGE_RUNS / 10]);
        }
        return ranges;
    }

    /**
     * Runs one projection: the reliability after each season, until progress stops or the horizon.
     */
    private static double[] run(PlayerSnapshot snapshot, MissionComponent component, double current,
                                SetSpecialist[] planned, RandomGenerator random) {
        SetSpecialist[] team = planned.clone();
        double[] skills = new double[team.length];
        int[] retiresIn = new int[team.length];
        for (int slot = 0; slot < team.length; slot++) {
            if (team[slot] != null) {
                skills[slot] = team[slot].getSkill(component.associatedSkill());
                retiresIn[slot] = PersonnelLifecycle.seasonsUntilRetirement(team[slot], PersonnelGroup.SET);
            }
        }
        double averageFactor = (SkillGrowth.RD_GAIN_FACTOR_MIN + SkillGrowth.RD_GAIN_FACTOR_MAX) / 2.0;
        double[] path = new double[HORIZON_SEASONS];
        int length = 0;
        double reliability = current;
        double chemistry = Double.NaN;
        for (int season = 1; season <= HORIZON_SEASONS; season++) {
            // Retirement comes first in a season's resolution, before its R&D.
            for (int slot = 0; slot < team.length; slot++) {
                if (team[slot] != null && retiresIn[slot] <= season) {
                    team[slot] = null;
                    chemistry = Double.NaN;
                }
            }
            if (Double.isNaN(chemistry)) {
                chemistry = Chemistry.research(snapshot, members(team), component.associatedSkill(), true)
                    .multiplier();
            }
            // Random events tick before R&D: N seasons left now leaves N - 1 for that season's R&D.
            double event = snapshot.getRdResultsSeasonsRemaining() - season > 0 ? snapshot.getRdResultsMultiplier() : 1;
            // An R&D decision's factor on this component, while its seasons last (ui-spec.md component 26).
            OwnedMissionComponent own = snapshot.getMissionComponents().get(component.id());
            if (own != null && season <= own.getRdFactorSeasons()) {
                event *= own.getRdFactor();
            }
            double gain = seasonGain(component, reliability, team, skills, event * chemistry, season - 1, random);
            if (gain <= EPSILON) {
                break;
            }
            reliability += gain;
            path[length++] = reliability;
            // Each engineer's own flat skill gain, the average draw, after this season's R&D read it.
            double[] before = skills.clone();
            for (int slot = 0; slot < team.length; slot++) {
                if (team[slot] == null) {
                    continue;
                }
                double mentored = 1;
                for (int other = 0; other < team.length; other++) {
                    if (other != slot && team[other] != null && Personality.known(team[other], Trait.MENTOR)
                            && before[other] > before[slot]) {
                        mentored = TraitEffects.MENTORED;
                    }
                }
                double factor = random == null ? averageFactor : SkillGrowth.drawRdGainFactor(random);
                skills[slot] = Math.min(SkillGrowth.SKILL_CAP,
                    before[slot] + SkillGrowth.flatRdGain(before[slot], factor) * event * mentored);
            }
        }
        return Arrays.copyOf(path, length);
    }

    private static SetSpecialist[] team(PlayerSnapshot snapshot, String componentId) {
        SetSpecialist[] team = new SetSpecialist[MissionComponent.RD_SLOT_COUNT];
        for (SetSpecialist specialist : snapshot.getSetSpecialists()) {
            Integer slot = specialist.getRdSlot();
            if (specialist.getAssignmentState() == AssignmentState.ASSIGNED
                && componentId.equals(specialist.getRdTargetComponentId()) && slot != null && slot >= 0
                && slot < team.length && team[slot] == null) {
                team[slot] = specialist;
            }
        }
        return team;
    }

    /**
     * Returns the best R&amp;D team the player could put on a component now, best first: the
     * engineers already on it plus every idle, available one, by their skill in the component's
     * skill, at most four. Seated in this order, the best engineer takes the richest slot, which
     * gives the largest expected gain. Engineers busy elsewhere (training, another component) are
     * left alone.
     *
     * @param snapshot  the player
     * @param component the component
     * @return the engineers, best first
     * @throws NullPointerException if an argument is {@code null}
     */
    public static List<SetSpecialist> bestTeam(PlayerSnapshot snapshot, MissionComponent component) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(component, "component must not be null");
        List<SetSpecialist> pool = new ArrayList<>();
        for (SetSpecialist specialist : snapshot.getSetSpecialists()) {
            boolean onIt = specialist.getAssignmentState() == AssignmentState.ASSIGNED
                && component.id().equals(specialist.getRdTargetComponentId());
            boolean free = specialist.getAssignmentState() == AssignmentState.IDLE;
            // Only those who could be seated again after a release: assignResearch needs them available.
            if ((onIt || free) && specialist.getUnavailableSeasons() == 0) {
                pool.add(specialist);
            }
        }
        pool.sort(Comparator.comparingDouble((SetSpecialist s) -> s.getSkill(component.associatedSkill())).reversed());
        return List.copyOf(pool.subList(0, Math.min(MissionComponent.RD_SLOT_COUNT, pool.size())));
    }
}
