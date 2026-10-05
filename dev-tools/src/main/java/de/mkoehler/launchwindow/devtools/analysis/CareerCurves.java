package de.mkoehler.launchwindow.devtools.analysis;

import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.AstronautSkill;
import de.mkoehler.launchwindow.model.MissionControlOfficer;
import de.mkoehler.launchwindow.model.MissionControlSkill;
import de.mkoehler.launchwindow.model.Personnel;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSkill;
import de.mkoehler.launchwindow.model.SetSpecialist;
import de.mkoehler.launchwindow.model.TrainingKind;
import de.mkoehler.launchwindow.rules.HireOutcome;
import de.mkoehler.launchwindow.rules.HiringPool;
import de.mkoehler.launchwindow.rules.PersonnelGroup;
import de.mkoehler.launchwindow.rules.PersonnelLifecycle;
import de.mkoehler.launchwindow.rules.TrainingProgress;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

/**
 * Compares rookie and veteran hires (the age trade-off in {@link HiringPool}, 2026-10-05): follows
 * thousands of real candidates, each hired alone and trained without a break (always Advanced training
 * on their weakest skill) until they reach their group's retirement age, and prints how their average
 * skill develops by seasons since the hire.
 *
 * <p>Only training grows skills here: no missions, R&amp;D, resignations or random events. That keeps
 * the comparison clean, at the price of understating everyone's growth a little. Candidates are sorted
 * into three buckets by {@link HiringPool#experience}: rookies (below 0.25), mid-career (0.25 to 0.6)
 * and veterans (0.6 and up). The tuning target is that a rookie catches up with a veteran of the same
 * band around the middle of the veteran's remaining career, not after a season or two and not never.</p>
 *
 * <p>Run with {@code mvn -pl dev-tools compile exec:java
 * -DmainClass=de.mkoehler.launchwindow.devtools.analysis.CareerCurves}.</p>
 *
 * @author Mario Koehler
 */
public final class CareerCurves {

    /** How many candidates are followed per group and band. */
    private static final int CANDIDATES = 3000;

    /** The seasons since the hire at which the curves are printed. */
    private static final int[] OFFSETS = {0, 4, 8, 16, 24, 32, 48, 64, 96, 128};

    /** The longest career followed, in seasons (a 27-year-old engineer retiring at 65). */
    private static final int MAX_SEASONS = 4 * 40;

    private static final String[] BUCKETS = {"rookie", "mid", "veteran"};

    /**
     * Not instantiable; run {@link #main}.
     */
    private CareerCurves() {
    }

    /**
     * Prints the curves for every group, for an Average-band and an Excellent-band hiring pool.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        for (PersonnelGroup group : PersonnelGroup.values()) {
            for (int morale : new int[] {50, 90}) {
                print(group, morale, run(group, morale, 42L));
            }
        }
    }

    /**
     * Follows {@link #CANDIDATES} candidates of one group, hired from pools banded by a roster of the
     * given morale.
     *
     * @param group  the personnel group
     * @param morale the hiring roster's average morale, which picks the band
     * @param seed   the random seed
     * @return the curves, per bucket
     */
    static Curves run(PersonnelGroup group, int morale, long seed) {
        SplittableRandom random = new SplittableRandom(seed);
        Curves curves = new Curves();
        int followed = 0;
        for (int round = 0; followed < CANDIDATES; round++) {
            Archetype archetype = round % 2 == 0 ? Archetype.NASA : Archetype.SOVIET;
            PlayerSnapshot pool = new PlayerSnapshot("Agency " + round, archetype, 0, 0, 0, 1);
            addRosterMember(pool, group, morale);
            HiringPool.refreshCandidates(pool, random);
            for (Personnel candidate : candidates(pool, group)) {
                follow(candidate, group, archetype, random, curves);
                followed++;
            }
        }
        return curves;
    }

    private static void follow(Personnel candidate, PersonnelGroup group, Archetype archetype,
            SplittableRandom random, Curves curves) {
        int bucket = bucketOf(HiringPool.experience(candidate.getAge(), group));
        PlayerSnapshot career = new PlayerSnapshot("Career", archetype, 0, 0, 0, 1);
        setCandidates(career, group, candidate);
        if (HiringPool.hire(career, group, candidate.getId(), random) != HireOutcome.HIRED) {
            throw new IllegalStateException("could not hire " + candidate.getId());
        }
        int retirementAge = PersonnelLifecycle.retirementAgeFor(group);
        curves.count[bucket]++;
        curves.learningPotential[bucket] += candidate.getLearningPotential();
        curves.age[bucket] += candidate.getAge();
        int season = 0;
        while (season < MAX_SEASONS && candidate.getAge() < retirementAge) {
            double mean = meanSkill(candidate, group);
            curves.skill[bucket][season] += mean;
            curves.employed[bucket][season]++;
            curves.careerValue[bucket] += mean;
            if (candidate.getAssignmentState() == AssignmentState.IDLE) {
                startAdvancedOnWeakest(candidate, group);
            }
            TrainingProgress.advanceSeason(career, random);
            season++;
            if (season % PersonnelLifecycle.SEASONS_PER_YEAR == 0) {
                candidate.setAge(candidate.getAge() + 1);
            }
        }
        curves.careerSeasons[bucket] += season;
    }

    private static int bucketOf(double experience) {
        if (experience < 0.25) {
            return 0;
        }
        return experience < 0.6 ? 1 : 2;
    }

    private static void print(PersonnelGroup group, int morale, Curves curves) {
        System.out.println();
        System.out.println(group + ", hiring roster morale " + morale);
        System.out.printf("  %-8s %5s %5s %5s %7s %9s", "bucket", "n", "age", "LP", "career", "value");
        for (int offset : OFFSETS) {
            System.out.printf(" %6s", "t" + offset);
        }
        System.out.println();
        for (int b = 0; b < BUCKETS.length; b++) {
            int n = curves.count[b];
            if (n == 0) {
                continue;
            }
            System.out.printf("  %-8s %5d %5.1f %5.1f %7.1f %9.0f", BUCKETS[b], n, curves.age[b] / n,
                curves.learningPotential[b] / n, (double) curves.careerSeasons[b] / n, curves.careerValue[b] / n);
            for (int offset : OFFSETS) {
                int employed = curves.employed[b][offset];
                System.out.printf(" %6s", employed * 5 < n ? "-" : String.format("%.1f", curves.skill[b][offset] / employed));
            }
            System.out.println();
        }
        System.out.println("  rookies catch up with veterans at: " + crossover(curves, 0, 2)
            + "; with mid-career hires at: " + crossover(curves, 0, 1));
    }

    /**
     * Returns the first season since the hire at which the younger bucket's average skill reaches the
     * older one's, while at least a fifth of the older bucket is still employed.
     */
    private static String crossover(Curves curves, int younger, int older) {
        for (int season = 0; season < MAX_SEASONS; season++) {
            int olderEmployed = curves.employed[older][season];
            int youngerEmployed = curves.employed[younger][season];
            if (olderEmployed * 5 < curves.count[older] || youngerEmployed == 0) {
                return "never (the older hires have mostly retired by season " + season + ")";
            }
            if (curves.skill[younger][season] / youngerEmployed >= curves.skill[older][season] / olderEmployed) {
                return "season " + season;
            }
        }
        return "never";
    }

    private static double meanSkill(Personnel person, PersonnelGroup group) {
        Map<?, Double> skills = switch (group) {
            case ASTRONAUT -> ((Astronaut) person).getSkills();
            case SET -> ((SetSpecialist) person).getSkills();
            case MISSION_CONTROL -> ((MissionControlOfficer) person).getSkills();
        };
        return skills.values().stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    private static void startAdvancedOnWeakest(Personnel person, PersonnelGroup group) {
        person.setTrainingKind(TrainingKind.ADVANCED);
        person.setTrainingSeasonsTotal(TrainingProgress.ADVANCED_TRAINING_SEASONS);
        person.setTrainingSeasonsCompleted(0);
        person.setAssignmentState(AssignmentState.IN_TRAINING);
        switch (group) {
            case ASTRONAUT -> {
                Astronaut astronaut = (Astronaut) person;
                astronaut.setTrainingTargetSkill(weakest(astronaut.getSkills(), AstronautSkill.values()));
            }
            case SET -> {
                SetSpecialist specialist = (SetSpecialist) person;
                specialist.setTrainingTargetSkill(weakest(specialist.getSkills(), SetSkill.values()));
            }
            case MISSION_CONTROL -> {
                MissionControlOfficer officer = (MissionControlOfficer) person;
                officer.setTrainingTargetSkill(weakest(officer.getSkills(), MissionControlSkill.values()));
            }
        }
    }

    private static <K> K weakest(Map<K, Double> skills, K[] keys) {
        K weakest = keys[0];
        for (K key : keys) {
            if (skills.get(key) < skills.get(weakest)) {
                weakest = key;
            }
        }
        return weakest;
    }

    private static void addRosterMember(PlayerSnapshot snapshot, PersonnelGroup group, int morale) {
        String id = snapshot.allocatePersonnelId();
        switch (group) {
            case ASTRONAUT -> snapshot.getAstronauts().add(new Astronaut(id, "Roster", 30, morale, 50));
            case SET -> snapshot.getSetSpecialists().add(new SetSpecialist(id, "Roster", 30, morale, 50));
            case MISSION_CONTROL -> snapshot.getMissionControlOfficers().add(
                new MissionControlOfficer(id, "Roster", 30, morale, 50));
        }
    }

    private static List<Personnel> candidates(PlayerSnapshot snapshot, PersonnelGroup group) {
        return new ArrayList<>(switch (group) {
            case ASTRONAUT -> snapshot.getAstronautCandidates();
            case SET -> snapshot.getSetSpecialistCandidates();
            case MISSION_CONTROL -> snapshot.getMissionControlCandidates();
        });
    }

    private static void setCandidates(PlayerSnapshot snapshot, PersonnelGroup group, Personnel candidate) {
        switch (group) {
            case ASTRONAUT -> snapshot.setAstronautCandidates(new ArrayList<>(List.of((Astronaut) candidate)));
            case SET -> snapshot.setSetSpecialistCandidates(new ArrayList<>(List.of((SetSpecialist) candidate)));
            case MISSION_CONTROL -> snapshot.setMissionControlCandidates(
                new ArrayList<>(List.of((MissionControlOfficer) candidate)));
        }
    }

    /**
     * Running totals per bucket.
     */
    static final class Curves {
        final int[] count = new int[BUCKETS.length];
        final double[] age = new double[BUCKETS.length];
        final double[] learningPotential = new double[BUCKETS.length];
        final long[] careerSeasons = new long[BUCKETS.length];
        final double[] careerValue = new double[BUCKETS.length];
        final double[][] skill = new double[BUCKETS.length][MAX_SEASONS];
        final int[][] employed = new int[BUCKETS.length][MAX_SEASONS];
    }
}
