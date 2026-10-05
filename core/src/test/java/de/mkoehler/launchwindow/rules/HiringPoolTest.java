package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.BuildingSchedule;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.Building;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.TrainingKind;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>Tests for {@link HiringPool}.</p>
 *
 * @author Mario Koehler
 */
class HiringPoolTest {

    /**
     * Verifies a refresh produces exactly {@link HiringPool#HIRING_BATCH_SIZE}
     * candidates per group.
     */
    @Test
    void refreshProducesExactlyOneBatchPerGroup() {
        PlayerSnapshot snapshot = newSnapshot();

        HiringPool.refreshCandidates(snapshot, new Random(1L));

        assertEquals(HiringPool.HIRING_BATCH_SIZE, snapshot.getAstronautCandidates().size());
        assertEquals(HiringPool.HIRING_BATCH_SIZE, snapshot.getSetSpecialistCandidates().size());
        assertEquals(HiringPool.HIRING_BATCH_SIZE, snapshot.getMissionControlCandidates().size());
    }

    /**
     * Verifies no candidate id survives a refresh, and that ids are never
     * reused across refreshes - both properties the async model depends on
     * (a hire action against a stale pool must never accidentally match a
     * different candidate that happens to share an id).
     */
    @Test
    void refreshNeverReusesIds() {
        PlayerSnapshot snapshot = newSnapshot();
        Random random = new Random(1L);

        HiringPool.refreshCandidates(snapshot, random);
        Set<String> firstBatchIds = idsOf(snapshot.getAstronautCandidates());

        HiringPool.refreshCandidates(snapshot, random);
        Set<String> secondBatchIds = idsOf(snapshot.getAstronautCandidates());

        assertTrue(firstBatchIds.stream().noneMatch(secondBatchIds::contains),
            "no id from the first batch should reappear in the second");
    }

    /**
     * Verifies every generated candidate's skills, age (within their portrait's range), and learning
     * potential all fall within their documented ranges.
     */
    @Test
    void generatedValuesStayWithinDocumentedRanges() {
        PlayerSnapshot snapshot = newSnapshot();

        HiringPool.refreshCandidates(snapshot, new Random(1L));

        for (Astronaut candidate : snapshot.getAstronautCandidates()) {
            PortraitCatalog.Portrait portrait = PortraitCatalog.byId(candidate.getPortraitId());
            assertTrue(candidate.getAge() >= portrait.minAge() && candidate.getAge() <= portrait.maxAge());
            double experience = HiringPool.experience(candidate.getAge(), PersonnelGroup.ASTRONAUT);
            assertTrue(candidate.getLearningPotential() >= Math.round(HiringPool.learningPotentialMin(experience))
                && candidate.getLearningPotential() <= Math.round(HiringPool.learningPotentialMax(experience)));
            assertTrue(candidate.getMorale() >= HiringPool.NEW_HIRE_MIN_STARTING_MORALE
                && candidate.getMorale() <= HiringPool.NEW_HIRE_MAX_STARTING_MORALE);
        }
    }

    /**
     * Verifies an empty roster (nothing to average) falls back to the
     * documented fallback morale for quality-banding purposes, landing in
     * the "Average" band's skill range rather than treating it as morale 0
     * (which would produce "Poor"-band candidates for every brand-new
     * agency).
     */
    @Test
    void emptyRosterUsesTheFallbackMorale() {
        assertTrue(HiringPool.EMPTY_ROSTER_FALLBACK_MORALE > HiringPool.POOR_MORALE_CEILING
            && HiringPool.EMPTY_ROSTER_FALLBACK_MORALE <= HiringPool.AVERAGE_MORALE_CEILING,
            "the documented fallback must itself land in the Average band for this test to mean anything");

        PlayerSnapshot snapshot = newSnapshot();

        HiringPool.refreshCandidates(snapshot, new Random(1L));

        // Average band's astronaut skill range is [25, 60]. Two checks pin the band down precisely
        // with a fixed seed: nobody exceeds 60 (rules out Good/Excellent), and at least one value
        // exceeds Poor's own 40 ceiling (rules out Poor) - with 6 candidates x 5 skills all
        // independently drawn from [25, 60], the odds every single one lands below 40 by chance if
        // the band really is Average are astronomically small, so this isn't a flaky assertion.
        boolean sawSomethingAbovePoorsCeiling = false;
        for (Astronaut candidate : snapshot.getAstronautCandidates()) {
            for (var skill : de.mkoehler.launchwindow.model.AstronautSkill.values()) {
                double value = bandRoll(candidate, candidate.getSkill(skill));
                assertTrue(value <= 60.0, "an empty-roster refresh should never produce a Good/Excellent-band skill");
                if (value > 40.0) {
                    sawSomethingAbovePoorsCeiling = true;
                }
            }
        }
        assertTrue(sawSomethingAbovePoorsCeiling, "an empty-roster refresh should use Average, not Poor");
    }

    /**
     * Returns a candidate's skill without their experience bonus: the band's own roll.
     */
    private static double bandRoll(Astronaut candidate, double skill) {
        return skill - HiringPool.experienceBonus(candidate.getAge(), PersonnelGroup.ASTRONAUT);
    }

    /**
     * The experience shift is centred: averaged over many candidates it is about zero, so age is a
     * trade-off and not a difficulty change. Rookies start below the band's roll, veterans above it.
     */
    @Test
    void theExperienceShiftIsCentred() {
        for (PersonnelGroup group : PersonnelGroup.values()) {
            double mean = HiringPool.meanExperience(group);
            assertTrue(mean > 0.0 && mean < 1.0, group + ": " + mean);
            assertTrue(HiringPool.experienceBonus(HiringPool.CAREER_START_AGE, group) < 0.0);
            assertTrue(HiringPool.experienceBonus(PersonnelLifecycle.retirementAgeFor(group), group) > 0.0);
        }
        double total = 0;
        int count = 0;
        for (int round = 0; round < 300; round++) {
            PlayerSnapshot snapshot = new PlayerSnapshot("Agency " + round,
                round % 2 == 0 ? Archetype.NASA : Archetype.SOVIET, 0, 0, 0, 1);
            HiringPool.refreshCandidates(snapshot, new Random(round));
            for (Astronaut candidate : snapshot.getAstronautCandidates()) {
                total += HiringPool.experienceBonus(candidate.getAge(), PersonnelGroup.ASTRONAUT);
                count++;
            }
        }
        assertEquals(0.0, total / count, 1.0, "the average candidate's shift");
    }

    /**
     * An older candidate starts with more skill and less learning potential than a younger one: the
     * experience bonus grows with age against the group's career, and the learning-potential range
     * slides down.
     */
    @Test
    void experienceRaisesSkillAndLowersLearningPotential() {
        assertEquals(0.0, HiringPool.experience(HiringPool.CAREER_START_AGE, PersonnelGroup.SET));
        assertEquals(1.0, HiringPool.experience(PersonnelLifecycle.SET_RETIREMENT_AGE, PersonnelGroup.SET));
        assertTrue(HiringPool.experience(42, PersonnelGroup.ASTRONAUT) > HiringPool.experience(42, PersonnelGroup.SET),
            "the same age is later in an astronaut's career than in an engineer's");
        assertEquals(HiringPool.ROOKIE_MIN_LEARNING_POTENTIAL, HiringPool.learningPotentialMin(0.0));
        assertEquals(HiringPool.VETERAN_MAX_LEARNING_POTENTIAL, HiringPool.learningPotentialMax(1.0));
        assertTrue(HiringPool.learningPotentialMax(1.0) <= HiringPool.learningPotentialMin(0.0),
            "a veteran never learns faster than a rookie");
    }

    /**
     * Verifies each of the four quality bands is actually selected at and
     * around its own boundary - a roster whose average morale sits exactly
     * on a ceiling uses that (lower) band, one point above uses the next.
     */
    @Test
    void moraleBoundariesSelectTheDocumentedBand() {
        assertCandidateSkillsWithinBand(HiringPool.POOR_MORALE_CEILING, 0.0, 40.0, 90.0);
        assertCandidateSkillsWithinBand(HiringPool.POOR_MORALE_CEILING + 1, 25.0, 60.0, 90.0);
        assertCandidateSkillsWithinBand(HiringPool.AVERAGE_MORALE_CEILING, 25.0, 60.0, 90.0);
        assertCandidateSkillsWithinBand(HiringPool.AVERAGE_MORALE_CEILING + 1, 40.0, 75.0, 90.0);
        assertCandidateSkillsWithinBand(HiringPool.GOOD_MORALE_CEILING, 40.0, 75.0, 90.0);
        assertCandidateSkillsWithinBand(HiringPool.GOOD_MORALE_CEILING + 1, 55.0, 90.0, 90.0);
    }

    /**
     * Refreshes a roster whose single member has exactly {@code rosterMorale}
     * morale, and asserts every generated astronaut candidate skill falls
     * within {@code [expectedMin, expectedMax]} - the exact range the
     * selected band should produce. {@code otherBandCeiling} is the next
     * band up's own ceiling, used only to sanity-check the test's own
     * fixture (that {@code expectedMax} is strictly below it, so this test
     * would actually fail if the wrong, higher band were selected).
     */
    private static void assertCandidateSkillsWithinBand(int rosterMorale, double expectedMin, double expectedMax,
            double otherBandCeiling) {
        assertTrue(expectedMax <= otherBandCeiling);
        PlayerSnapshot snapshot = newSnapshot();
        snapshot.getAstronauts().add(new Astronaut(snapshot.allocatePersonnelId(), "Existing", 30, rosterMorale, 50));

        HiringPool.refreshCandidates(snapshot, new Random(7L));

        for (Astronaut candidate : snapshot.getAstronautCandidates()) {
            for (var skill : de.mkoehler.launchwindow.model.AstronautSkill.values()) {
                if (candidate.getSkill(skill) == HiringPool.CANDIDATE_SKILL_CAP) {
                    continue;
                }
                double value = bandRoll(candidate, candidate.getSkill(skill));
                assertTrue(value >= expectedMin - 1e-9 && value <= expectedMax + 1e-9,
                    "roster morale " + rosterMorale + ": expected [" + expectedMin + ", " + expectedMax
                        + "], got " + value);
            }
        }
    }

    /**
     * Verifies a successful hire moves the candidate from the pool to the
     * roster, auto-starting {@code BASIC} training per the user's
     * 2026-09-30 decision.
     */
    @Test
    void hireMovesTheCandidateToTheRosterAndStartsBasicTraining() {
        PlayerSnapshot snapshot = newSnapshot();
        HiringPool.refreshCandidates(snapshot, new Random(1L));
        String candidateId = snapshot.getAstronautCandidates().get(0).getId();

        HireOutcome outcome = HiringPool.hire(snapshot, PersonnelGroup.ASTRONAUT, candidateId, new Random(2L));

        assertEquals(HireOutcome.HIRED, outcome);
        assertTrue(snapshot.getAstronautCandidates().stream().noneMatch(c -> c.getId().equals(candidateId)));
        Astronaut hired = snapshot.getAstronauts().stream()
            .filter(a -> a.getId().equals(candidateId)).findFirst().orElse(null);
        assertNotNull(hired, "the hired candidate should now be on the roster");
        assertEquals(TrainingKind.BASIC, hired.getTrainingKind());
        assertEquals(AssignmentState.IN_TRAINING, hired.getAssignmentState());
        assertEquals(TrainingProgress.ASTRONAUT_BASIC_TRAINING_SEASONS, hired.getTrainingSeasonsTotal());
        assertNotNull(hired.getTrainingTargetSkill());
    }

    /**
     * Verifies hiring an id that isn't in the pool fails cleanly, without
     * mutating the snapshot - the normal async case of a stale candidate
     * view.
     */
    @Test
    void hireWithUnknownIdFailsWithoutMutating() {
        PlayerSnapshot snapshot = newSnapshot();
        HiringPool.refreshCandidates(snapshot, new Random(1L));
        int rosterSizeBefore = snapshot.getAstronauts().size();

        HireOutcome outcome = HiringPool.hire(snapshot, PersonnelGroup.ASTRONAUT, "no-such-id", new Random(1L));

        assertEquals(HireOutcome.UNKNOWN_CANDIDATE, outcome);
        assertEquals(rosterSizeBefore, snapshot.getAstronauts().size());
    }

    /**
     * Verifies hiring a candidate id that belongs to a different group's
     * pool fails, rather than accidentally matching across groups.
     */
    @Test
    void hireWithIdFromTheWrongGroupFails() {
        PlayerSnapshot snapshot = newSnapshot();
        HiringPool.refreshCandidates(snapshot, new Random(1L));
        String setCandidateId = snapshot.getSetSpecialistCandidates().get(0).getId();

        HireOutcome outcome = HiringPool.hire(snapshot, PersonnelGroup.ASTRONAUT, setCandidateId, new Random(1L));

        assertEquals(HireOutcome.UNKNOWN_CANDIDATE, outcome);
    }

    /**
     * Verifies a full roster rejects a hire without mutating anything.
     */
    @Test
    void hireIntoAFullRosterFails() {
        PlayerSnapshot snapshot = newSnapshot();
        int capacity = HiringPool.rosterCapacity(snapshot, PersonnelGroup.ASTRONAUT, BuildingSchedule.baspmClose());
        for (int i = 0; i < capacity; i++) {
            snapshot.getAstronauts().add(new Astronaut(snapshot.allocatePersonnelId(), "Existing", 30, 80, 50));
        }
        HiringPool.refreshCandidates(snapshot, new Random(1L));
        String candidateId = snapshot.getAstronautCandidates().get(0).getId();
        int poolSizeBefore = snapshot.getAstronautCandidates().size();

        HireOutcome outcome = HiringPool.hire(snapshot, PersonnelGroup.ASTRONAUT, candidateId, new Random(1L));

        assertEquals(HireOutcome.ROSTER_FULL, outcome);
        assertEquals(poolSizeBefore, snapshot.getAstronautCandidates().size(), "the pool should be untouched on failure");
    }

    /**
     * Verifies an Astronaut Center upgrade raises the roster cap.
     */
    @Test
    void upgradedBuildingRaisesTheRosterCap() {
        PlayerSnapshot snapshot = newSnapshot();
        BuildingSchedule buildings = BuildingSchedule.baspmClose();
        assertEquals(7, HiringPool.rosterCapacity(snapshot, PersonnelGroup.ASTRONAUT, buildings));
        assertEquals(5, HiringPool.rosterCapacity(snapshot, PersonnelGroup.MISSION_CONTROL, buildings));
        assertEquals(7, HiringPool.rosterCapacity(snapshot, PersonnelGroup.SET, buildings));
        for (int i = 0; i < 7; i++) {
            snapshot.getAstronauts().add(new Astronaut(snapshot.allocatePersonnelId(), "Existing", 30, 80, 50));
        }
        HiringPool.refreshCandidates(snapshot, new Random(1L));
        String candidateId = snapshot.getAstronautCandidates().get(0).getId();

        snapshot.getBuildingLevels().put(Building.ASTRONAUT_CENTER, 2);

        assertEquals(HireOutcome.HIRED,
            HiringPool.hire(snapshot, PersonnelGroup.ASTRONAUT, candidateId, new Random(1L), buildings));
        assertEquals(8, snapshot.getAstronauts().size());
    }

    /**
     * Verifies the same seed produces the same pool.
     */
    @Test
    void sameSeedProducesTheSamePool() {
        PlayerSnapshot first = newSnapshot();
        HiringPool.refreshCandidates(first, new Random(42L));

        PlayerSnapshot second = newSnapshot();
        HiringPool.refreshCandidates(second, new Random(42L));

        assertEquals(idsOf(first.getAstronautCandidates()), idsOf(second.getAstronautCandidates()));
        for (int i = 0; i < HiringPool.HIRING_BATCH_SIZE; i++) {
            assertEquals(first.getAstronautCandidates().get(i).getAge(), second.getAstronautCandidates().get(i).getAge());
        }
    }

    /**
     * Verifies both {@code null} arguments are rejected.
     */
    @Test
    void rejectsNullArguments() {
        PlayerSnapshot snapshot = newSnapshot();
        assertThrows(NullPointerException.class, () -> HiringPool.refreshCandidates(null, new Random(1L)));
        assertThrows(NullPointerException.class, () -> HiringPool.refreshCandidates(snapshot, null));
        assertThrows(NullPointerException.class, () -> HiringPool.hire(null, PersonnelGroup.ASTRONAUT, "x", new Random(1L)));
        assertThrows(NullPointerException.class, () -> HiringPool.hire(snapshot, null, "x", new Random(1L)));
        assertThrows(NullPointerException.class, () -> HiringPool.hire(snapshot, PersonnelGroup.ASTRONAUT, null, new Random(1L)));
        assertThrows(NullPointerException.class, () -> HiringPool.hire(snapshot, PersonnelGroup.ASTRONAUT, "x", null));
    }

    private static Set<String> idsOf(List<Astronaut> candidates) {
        Set<String> ids = new HashSet<>();
        for (Astronaut candidate : candidates) {
            ids.add(candidate.getId());
        }
        return ids;
    }

    private static PlayerSnapshot newSnapshot() {
        return new PlayerSnapshot("Agency", Archetype.NASA, 0, 0, 0, 1);
    }
}
