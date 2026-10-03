package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.content.MissionSeason;
import de.mkoehler.launchwindow.content.MissionStep;
import de.mkoehler.launchwindow.content.Program;
import de.mkoehler.launchwindow.content.ProgramCategory;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.InProgressMission;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSkill;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>Tests for prestige and the repeat-flight count inside
 * {@link MissionProgress#advanceSeason}: per-season accumulation, the
 * failure replacement rule, the zero floor, the live repeat tier, and
 * when a flight counts as completed. See {@code design.md} &sect;2.6(d)
 * and {@code baspm.md} &sect;11.7.</p>
 *
 * <p>Uses the same scripted-roll technique as {@link MissionProgressTest};
 * every test pads its rolls with harmless post-flight draws, since the
 * season's reliability and skill-growth passes also draw from the same
 * source.</p>
 *
 * @author Mario Koehler
 */
class MissionProgressPrestigeTest {

    private static final String CONFIG = "prestige-std";

    private static final MissionComponent CAPSULE = new MissionComponent("capsule-mercury", SetSkill.CREWED_SPACECRAFT,
        3, 90, 82, List.of(3.0, 6.0, 9.0, 12.0), 0, List.of());
    private static final Program PROGRAM =
        new Program("mercury-program", "early-space", 1_000, 0, List.of("capsule-mercury"), List.of());
    private static final ProgramCategory CATEGORY = new ProgramCategory("early-space", "Early Space");

    /** Padding for the post-flight reliability and skill-growth draws, appended after each step's rolls. */
    private static final double[] POST_FLIGHT_PADDING = {0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1};

    /** One successful step: a first roll of 99 always clears the base probability. */
    private static final double[] SUCCESS = {99.0};

    /** A step that resolves {@code MINOR_GLITCH}: first roll fails, saving roll lands in the glitch band. */
    private static final double[] MINOR_GLITCH = {0.0, 80.0};

    /** A step that resolves plain {@code FAILURE}: first roll fails, saving roll lands in the failure band. */
    private static final double[] FAILURE = {0.0, 30.0};

    /**
     * A step with the given prestige share and failure deduction, flying on
     * the capsule alone (no crew, so no personnel is needed in the snapshot).
     *
     * @param id        the step id
     * @param share     the step's prestige share percentage
     * @param deduction the step's failure deduction magnitude
     * @return the step
     */
    private static MissionStep step(String id, int share, long deduction) {
        return new MissionStep(id, List.of(new MissionStep.ComponentInvolvement("capsule-mercury", 100)),
            List.of(), List.of(), false, share, deduction);
    }

    private static ContentCatalog catalogWith(MissionConfiguration... configurations) {
        return new ContentCatalog(List.of(CATEGORY), List.of(PROGRAM), List.of(configurations), List.of(CAPSULE));
    }

    /**
     * A snapshot with the mercury program open, a capsule at reliability 10,
     * the given prestige, and one in-progress mission of {@link #CONFIG}.
     *
     * @param prestige the player's starting prestige
     * @return the snapshot
     */
    private static PlayerSnapshot snapshotWithMission(long prestige) {
        PlayerSnapshot snapshot = new PlayerSnapshot("Agency", Archetype.NASA, 10_000, prestige, 0, 1);
        snapshot.setOpenedProgramIds(List.of("mercury-program"));
        snapshot.setMissionComponents(Map.of("capsule-mercury", new OwnedMissionComponent(10.0, 10.0)));
        InProgressMission mission = new InProgressMission("m0", CONFIG, List.of(), List.of());
        snapshot.getInProgressMissions().put("m0", mission);
        return snapshot;
    }

    /**
     * Concatenates the given step rolls with the post-flight padding.
     *
     * @param stepRolls the rolls the season's steps consume, in order
     * @return all rolls, ready for {@link #scripted(double[])}
     */
    private static double[] withPadding(double... stepRolls) {
        double[] all = new double[stepRolls.length + POST_FLIGHT_PADDING.length];
        System.arraycopy(stepRolls, 0, all, 0, stepRolls.length);
        System.arraycopy(POST_FLIGHT_PADDING, 0, all, stepRolls.length, POST_FLIGHT_PADDING.length);
        return all;
    }

    /**
     * Joins two roll arrays, for seasons with more than one step.
     *
     * @param a the first array
     * @param b the second array
     * @return a new array holding {@code a} then {@code b}
     */
    private static double[] concat(double[] a, double[] b) {
        double[] out = new double[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    /**
     * A successful first-time flight pays its full tier total, and a
     * completed flight is counted once.
     */
    @Test
    void successfulFirstTimeFlightPaysTheFullTotalAndCounts() {
        MissionConfiguration configuration = new MissionConfiguration(CONFIG, "mercury-program",
            List.of(new MissionSeason(List.of(step("s1", 100, 0)))), 0, 0, 0, 1_000);
        PlayerSnapshot snapshot = snapshotWithMission(0);

        List<ResolutionEvent> events = MissionProgress.advanceSeason(snapshot, catalogWith(configuration),
            scripted(withPadding(SUCCESS)));

        assertEquals(1_000, snapshot.getPrestige());
        assertEquals(1, snapshot.getSuccessfulFlightCounts().get(CONFIG));
        MissionPrestigeChanged changed = onlyPrestigeEvent(events);
        assertEquals(1_000, changed.requested());
        assertEquals(1_000, changed.applied());
    }

    /**
     * A {@code MINOR_GLITCH} step pays half its share, rounded down.
     */
    @Test
    void minorGlitchPaysHalfItsShare() {
        MissionConfiguration configuration = new MissionConfiguration(CONFIG, "mercury-program",
            List.of(new MissionSeason(List.of(step("s1", 100, 0)))), 0, 0, 0, 1_001);
        PlayerSnapshot snapshot = snapshotWithMission(0);

        List<ResolutionEvent> events = MissionProgress.advanceSeason(snapshot, catalogWith(configuration),
            scripted(withPadding(MINOR_GLITCH)));

        assertEquals(MissionStepOutcome.MINOR_GLITCH, ((MissionStepResolved) events.get(0)).outcome());
        assertEquals(500, snapshot.getPrestige());
        assertEquals(500, onlyPrestigeEvent(events).applied());
    }

    /**
     * A failing step replaces its season's running total with its own
     * deduction. Here step 1 earns 40% of 1,000 = 400, then step 2 fails with
     * deduction 150. The season nets -150 from a prior 500, leaving 350 - not
     * 500 + 400 - 150 = 750, which would be the additive reading.
     */
    @Test
    void failureReplacesTheSeasonsEarlierPrestigeInsteadOfAddingToIt() {
        MissionConfiguration configuration = new MissionConfiguration(CONFIG, "mercury-program",
            List.of(new MissionSeason(List.of(step("s1", 40, 0), step("s2", 60, 150)))), 0, 0, 0, 1_000);
        PlayerSnapshot snapshot = snapshotWithMission(500);

        List<ResolutionEvent> events = MissionProgress.advanceSeason(snapshot, catalogWith(configuration),
            scripted(withPadding(concat(SUCCESS, FAILURE))));

        assertEquals(350, snapshot.getPrestige());
        MissionPrestigeChanged changed = onlyPrestigeEvent(events);
        assertEquals(-150, changed.requested());
        assertEquals(-150, changed.applied());
    }

    /**
     * Cumulative prestige never goes below zero: a deduction larger than the
     * player's prestige takes them to zero and reports only what was taken.
     */
    @Test
    void prestigeIsFlooredAtZeroAndReportsTheAppliedAmount() {
        MissionConfiguration configuration = new MissionConfiguration(CONFIG, "mercury-program",
            List.of(new MissionSeason(List.of(step("s1", 100, 300)))), 0, 0, 0, 1_000);
        PlayerSnapshot snapshot = snapshotWithMission(100);

        List<ResolutionEvent> events = MissionProgress.advanceSeason(snapshot, catalogWith(configuration),
            scripted(withPadding(FAILURE)));

        assertEquals(0, snapshot.getPrestige());
        MissionPrestigeChanged changed = onlyPrestigeEvent(events);
        assertEquals(-300, changed.requested());
        assertEquals(-100, changed.applied());
    }

    /**
     * The payout reads the live completion count: a player with five prior
     * completions of this configuration gets the later-time ratio, not the
     * first-time total.
     */
    @Test
    void payoutReadsTheLiveCompletionCount() {
        MissionConfiguration configuration = new MissionConfiguration(CONFIG, "mercury-program",
            List.of(new MissionSeason(List.of(step("s1", 100, 0)))), 0, 0, 0, 1_000);
        PlayerSnapshot snapshot = snapshotWithMission(0);
        snapshot.getSuccessfulFlightCounts().put(CONFIG, 5);

        MissionProgress.advanceSeason(snapshot, catalogWith(configuration), scripted(withPadding(SUCCESS)));

        assertEquals(1_000 * MissionPrestige.LATER_TIME_PERCENT / 100, snapshot.getPrestige());
    }

    /**
     * A second-time flight pays the fixed second-time ratio of the total.
     */
    @Test
    void secondTimeFlightPaysTheSecondTimeRatio() {
        MissionConfiguration configuration = new MissionConfiguration(CONFIG, "mercury-program",
            List.of(new MissionSeason(List.of(step("s1", 100, 0)))), 0, 0, 0, 1_000);
        PlayerSnapshot snapshot = snapshotWithMission(0);
        snapshot.getSuccessfulFlightCounts().put(CONFIG, 1);

        MissionProgress.advanceSeason(snapshot, catalogWith(configuration), scripted(withPadding(SUCCESS)));

        assertEquals(1_000 * MissionPrestige.SECOND_TIME_PERCENT / 100, snapshot.getPrestige());
    }

    /**
     * Two copies of one configuration that complete in the same pass do not
     * both pay first-time. The first pays the full total and counts; the
     * second, later in launch order, reads the count the first just bumped
     * and pays the second-time ratio. This is the case the launch-time stamp
     * got wrong (five copies all paying 100% in a single pass).
     */
    @Test
    void twoCopiesCompletingInOnePassDoNotBothPayFirstTime() {
        MissionConfiguration configuration = new MissionConfiguration(CONFIG, "mercury-program",
            List.of(new MissionSeason(List.of(step("s1", 100, 0)))), 0, 0, 0, 1_000);
        PlayerSnapshot snapshot = snapshotWithMission(0);
        snapshot.getInProgressMissions().put("m1", new InProgressMission("m1", CONFIG, List.of(), List.of()));

        List<ResolutionEvent> events = MissionProgress.advanceSeason(snapshot, catalogWith(configuration),
            // Draw order matters: the first copy's step roll, then its own post-flight reliability draw (one
            // per involved component, no crew so no skill growth), then the second copy's step roll.
            scripted(new double[] {99.0, 0.1, 99.0, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1}));

        assertEquals(1_000 + 1_000 * MissionPrestige.SECOND_TIME_PERCENT / 100, snapshot.getPrestige());
        assertEquals(2, snapshot.getSuccessfulFlightCounts().get(CONFIG));
        assertEquals(2, events.stream().filter(e -> e instanceof MissionPrestigeChanged).count());
    }

    /**
     * A multi-season flight counts once, when it completes its final season -
     * not once per season.
     */
    @Test
    void multiSeasonFlightCountsOnceOnCompletion() {
        MissionConfiguration configuration = new MissionConfiguration(CONFIG, "mercury-program",
            List.of(new MissionSeason(List.of(step("s1", 50, 0))), new MissionSeason(List.of(step("s2", 50, 0)))),
            0, 0, 0, 1_000);
        PlayerSnapshot snapshot = snapshotWithMission(0);
        ContentCatalog catalog = catalogWith(configuration);

        MissionProgress.advanceSeason(snapshot, catalog, scripted(withPadding(SUCCESS)));
        assertFalse(snapshot.getSuccessfulFlightCounts().containsKey(CONFIG));

        MissionProgress.advanceSeason(snapshot, catalog, scripted(withPadding(SUCCESS)));
        assertEquals(1, snapshot.getSuccessfulFlightCounts().get(CONFIG));
    }

    /**
     * A flight that fails is not counted, so it doesn't move the repeat tier.
     */
    @Test
    void failedFlightIsNotCounted() {
        MissionConfiguration configuration = new MissionConfiguration(CONFIG, "mercury-program",
            List.of(new MissionSeason(List.of(step("s1", 100, 0)))), 0, 0, 0, 1_000);
        PlayerSnapshot snapshot = snapshotWithMission(0);

        MissionProgress.advanceSeason(snapshot, catalogWith(configuration), scripted(withPadding(FAILURE)));

        assertFalse(snapshot.getSuccessfulFlightCounts().containsKey(CONFIG));
    }

    /**
     * A configuration that pays no prestige reports no prestige event, so
     * existing prestige-free content keeps its event stream unchanged.
     */
    @Test
    void prestigeFreeConfigurationReportsNoPrestigeEvent() {
        MissionConfiguration configuration = new MissionConfiguration(CONFIG, "mercury-program",
            List.of(new MissionSeason(List.of(step("s1", 100, 0)))), 0, 0, 0);
        PlayerSnapshot snapshot = snapshotWithMission(0);

        List<ResolutionEvent> events = MissionProgress.advanceSeason(snapshot, catalogWith(configuration),
            scripted(withPadding(SUCCESS)));

        assertEquals(0, snapshot.getPrestige());
        assertTrue(events.stream().noneMatch(e -> e instanceof MissionPrestigeChanged));
    }

    private static MissionPrestigeChanged onlyPrestigeEvent(List<ResolutionEvent> events) {
        List<MissionPrestigeChanged> changes = events.stream()
            .filter(e -> e instanceof MissionPrestigeChanged)
            .map(e -> (MissionPrestigeChanged) e)
            .toList();
        assertEquals(1, changes.size());
        return changes.get(0);
    }

    private static RandomGenerator scripted(double[] rolls) {
        return new ScriptedRolls(rolls);
    }

    /**
     * A {@link RandomGenerator} returning a fixed sequence of
     * {@code nextDouble} results, one per draw - same shape as
     * {@link MissionProgressTest}'s own helper.
     */
    private static final class ScriptedRolls implements RandomGenerator {

        private final double[] rolls;
        private int next = 0;

        private ScriptedRolls(double[] rolls) {
            this.rolls = rolls;
        }

        @Override
        public long nextLong() {
            throw new UnsupportedOperationException("not used by MissionProgress");
        }

        @Override
        public double nextDouble(double origin, double bound) {
            if (next >= rolls.length) {
                throw new IllegalStateException("no more scripted rolls available");
            }
            return rolls[next++];
        }
    }
}
