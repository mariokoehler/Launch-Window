package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSkill;
import de.mkoehler.launchwindow.model.SetSpecialist;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>Tests for {@link ResearchProgress}.</p>
 *
 * @author Mario Koehler
 */
class ResearchProgressTest {

    private final MissionComponent redstone =
        new MissionComponent("rocket-redstone", SetSkill.ROCKETS, 3, 90, 0, List.of());
    private final ContentCatalog catalog = new ContentCatalog(List.of(), List.of(), List.of(), List.of(redstone));

    /**
     * Creates a snapshot with one {@code SET} specialist assigned to R&D
     * on {@code rocket-redstone}, which the snapshot already owns.
     *
     * @param skill the specialist's starting skill in {@link SetSkill#ROCKETS}
     * @return the new snapshot
     */
    private PlayerSnapshot snapshotWithAssignedResearcher(double skill) {
        PlayerSnapshot snapshot = new PlayerSnapshot("Agency", Archetype.NASA, 0, 0, 0, 1);
        SetSpecialist specialist = new SetSpecialist("set-1", "Omar Idris", 41, 75, 55);
        specialist.setSkill(SetSkill.ROCKETS, skill);
        specialist.setAssignmentState(AssignmentState.ASSIGNED);
        specialist.setRdTargetComponentId("rocket-redstone");
        snapshot.setSetSpecialists(List.of(specialist));
        snapshot.setMissionComponents(Map.of("rocket-redstone", new OwnedMissionComponent(50.0, 3.0)));
        return snapshot;
    }

    /**
     * Verifies that a season of R&D grows both the component's reliability
     * and the researcher's own skill, and reports both in one event.
     */
    @Test
    void advanceSeasonGrowsReliabilityAndSkillAndReportsOneEvent() {
        PlayerSnapshot snapshot = snapshotWithAssignedResearcher(50.0);
        Random random = new Random(42L);

        List<ResolutionEvent> events = ResearchProgress.advanceSeason(snapshot, catalog, random);

        assertEquals(1, events.size());
        ResearchCompleted completed = (ResearchCompleted) events.get(0);
        assertEquals("set-1", completed.personnelId());
        assertEquals("rocket-redstone", completed.componentId());
        assertTrue(completed.skillGain() > 0);
        assertTrue(completed.reliabilityGain() > 0);

        OwnedMissionComponent owned = snapshot.getMissionComponents().get("rocket-redstone");
        assertEquals(50.0 + completed.reliabilityGain(), owned.getCurrentReliability());
        assertEquals(50.0 + completed.skillGain(),
            snapshot.getSetSpecialists().get(0).getSkill(SetSkill.ROCKETS));
    }

    /**
     * Verifies that R&D never moves a component's floor reliability -
     * only {@code currentReliability} grows from it.
     */
    @Test
    void advanceSeasonNeverMovesTheFloor() {
        PlayerSnapshot snapshot = snapshotWithAssignedResearcher(50.0);

        ResearchProgress.advanceSeason(snapshot, catalog, new Random(42L));

        assertEquals(3.0, snapshot.getMissionComponents().get("rocket-redstone").getFloorReliability());
    }

    /**
     * Verifies a specialist not {@code ASSIGNED} does no R&D this season.
     */
    @Test
    void skipsASpecialistNotAssigned() {
        PlayerSnapshot snapshot = snapshotWithAssignedResearcher(50.0);
        snapshot.getSetSpecialists().get(0).setAssignmentState(AssignmentState.IDLE);

        List<ResolutionEvent> events = ResearchProgress.advanceSeason(snapshot, catalog, new Random(42L));

        assertTrue(events.isEmpty());
        assertEquals(50.0, snapshot.getMissionComponents().get("rocket-redstone").getCurrentReliability());
    }

    /**
     * Verifies an assigned specialist with no R&D target set does no R&D
     * this season - the existing fixtures elsewhere in this codebase that
     * set {@code ASSIGNED} without a target must not trip this resolver.
     */
    @Test
    void skipsAnAssignedSpecialistWithNoTarget() {
        PlayerSnapshot snapshot = snapshotWithAssignedResearcher(50.0);
        snapshot.getSetSpecialists().get(0).setRdTargetComponentId(null);

        List<ResolutionEvent> events = ResearchProgress.advanceSeason(snapshot, catalog, new Random(42L));

        assertTrue(events.isEmpty());
    }

    /**
     * Verifies an assigned specialist whose target isn't actually owned
     * (a stale reference) does no R&D this season, rather than throwing.
     */
    @Test
    void skipsAnAssignedSpecialistWithAnUnownedTarget() {
        PlayerSnapshot snapshot = snapshotWithAssignedResearcher(50.0);
        snapshot.setMissionComponents(Map.of());

        List<ResolutionEvent> events = ResearchProgress.advanceSeason(snapshot, catalog, new Random(42L));

        assertTrue(events.isEmpty());
    }

    /**
     * Verifies an assigned specialist whose owned target isn't in the
     * content catalog does no R&D this season, rather than throwing.
     */
    @Test
    void skipsAnAssignedSpecialistWhoseTargetIsUnknownToTheCatalog() {
        PlayerSnapshot snapshot = snapshotWithAssignedResearcher(50.0);
        snapshot.setMissionComponents(Map.of("unknown-component", new OwnedMissionComponent(10, 3)));
        snapshot.getSetSpecialists().get(0).setRdTargetComponentId("unknown-component");

        List<ResolutionEvent> events = ResearchProgress.advanceSeason(snapshot, catalog, new Random(42L));

        assertTrue(events.isEmpty());
    }

    /**
     * Verifies that two specialists researching the same component stack,
     * applied in roster order, by replaying the exact same draw sequence
     * independently and asserting both events' gains match it exactly -
     * pins three things at once: the skill-factor-then-reliability-factor
     * draw order, that each gain uses the skill value as it stood before
     * this season's own skill gain (not after), and that the second
     * researcher's gain is computed against the reliability the first
     * one's gain already produced, not the pre-season value both started
     * from. A looser "second gain is smaller" assertion would pass even
     * for a non-stacking implementation most of the time, since each
     * researcher's own reliability-gain factor is drawn independently from
     * a 3x-wide range that usually dominates the small amount stacking
     * itself removes - only an exact replay actually proves stacking
     * happened.
     */
    @Test
    void twoResearchersOnTheSameComponentStackWithDiminishingReturns() {
        PlayerSnapshot snapshot = snapshotWithAssignedResearcher(80.0);
        SetSpecialist second = new SetSpecialist("set-2", "Second Researcher", 35, 70, 50);
        second.setSkill(SetSkill.ROCKETS, 80.0);
        second.setAssignmentState(AssignmentState.ASSIGNED);
        second.setRdTargetComponentId("rocket-redstone");
        snapshot.getSetSpecialists().add(second);

        List<ResolutionEvent> events = ResearchProgress.advanceSeason(snapshot, catalog, new Random(42L));

        Random replay = new Random(42L);
        SkillGrowth.drawRdGainFactor(replay); // the first researcher's skill-gain draw, discarded here
        double firstReliabilityFactor = SkillGrowth.drawRdReliabilityFactor(replay);
        double firstGain = SkillGrowth.reliabilityGain(50.0, 90.0, 80.0, firstReliabilityFactor);
        SkillGrowth.drawRdGainFactor(replay); // the second researcher's skill-gain draw, discarded here
        double secondReliabilityFactor = SkillGrowth.drawRdReliabilityFactor(replay);
        double secondGain = SkillGrowth.reliabilityGain(50.0 + firstGain, 90.0, 80.0, secondReliabilityFactor);

        assertEquals(2, events.size());
        assertEquals(firstGain, ((ResearchCompleted) events.get(0)).reliabilityGain());
        assertEquals(secondGain, ((ResearchCompleted) events.get(1)).reliabilityGain());
        assertTrue(secondGain < firstGain,
            "the second researcher's gain must be smaller, computed against the already-grown reliability");
    }
}
