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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>Tests for {@link ResearchProgress}.</p>
 *
 * @author Mario Koehler
 */
class ResearchProgressTest {

    private static final List<Double> RD_BANDS = List.of(3.0, 6.0, 9.0, 12.0);

    private final MissionComponent redstone =
        new MissionComponent("rocket-redstone", SetSkill.ROCKETS, 3, 95, 90, RD_BANDS, 0, List.of());
    private final ContentCatalog catalog = new ContentCatalog(List.of(), List.of(), List.of(), List.of(redstone));

    /**
     * Creates a snapshot with one {@code SET} specialist assigned to R&D
     * on {@code rocket-redstone} in slot 0, which the snapshot already
     * owns.
     *
     * @param skill the specialist's starting skill in {@link SetSkill#ROCKETS}
     * @return the new snapshot
     */
    private PlayerSnapshot snapshotWithAssignedResearcher(double skill) {
        return snapshotWithAssignedResearcher(skill, 0);
    }

    /**
     * Same as {@link #snapshotWithAssignedResearcher(double)}, but in a
     * caller-chosen slot.
     *
     * @param skill the specialist's starting skill in {@link SetSkill#ROCKETS}
     * @param slot  the R&D slot to occupy
     * @return the new snapshot
     */
    private PlayerSnapshot snapshotWithAssignedResearcher(double skill, int slot) {
        PlayerSnapshot snapshot = new PlayerSnapshot("Agency", Archetype.NASA, 0, 0, 0, 1);
        SetSpecialist specialist = new SetSpecialist("set-1", "Omar Idris", 41, 75, 55);
        specialist.setSkill(SetSkill.ROCKETS, skill);
        specialist.setAssignmentState(AssignmentState.ASSIGNED);
        specialist.setRdTargetComponentId("rocket-redstone");
        specialist.setRdSlot(slot);
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
        assertEquals(50.0 + completed.reliabilityGain(), owned.getCurrentReliability(), 1e-9);
        assertEquals(50.0 + completed.skillGain(),
            snapshot.getSetSpecialists().get(0).getSkill(SetSkill.ROCKETS), 1e-9);
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
     * Verifies an assigned specialist with a target but no slot set does no
     * R&D this season - BASPM has no concept of "assigned but not in any
     * slot" ({@code baspm.md} &sect;10.8), so neither does this resolver.
     */
    @Test
    void skipsAnAssignedSpecialistWithNoSlot() {
        PlayerSnapshot snapshot = snapshotWithAssignedResearcher(50.0);
        snapshot.getSetSpecialists().get(0).setRdSlot(null);

        List<ResolutionEvent> events = ResearchProgress.advanceSeason(snapshot, catalog, new Random(42L));

        assertTrue(events.isEmpty());
    }

    /**
     * Verifies an assigned specialist whose slot is outside the valid
     * {@code [0, 4)} range does no R&D this season, rather than throwing.
     */
    @Test
    void skipsAnAssignedSpecialistWithAnOutOfRangeSlot() {
        PlayerSnapshot snapshot = snapshotWithAssignedResearcher(50.0);
        snapshot.getSetSpecialists().get(0).setRdSlot(4);

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
     * Verifies that a second specialist colliding with the first's slot
     * does no R&D at all this season - first-encountered-in-roster-order
     * wins (see {@link ResearchProgress}'s own Javadoc), the loser gets
     * neither a skill gain nor a reliability contribution.
     */
    @Test
    void secondSpecialistCollidingOnTheSameSlotIsSkippedEntirely() {
        PlayerSnapshot snapshot = snapshotWithAssignedResearcher(50.0, 0);
        SetSpecialist second = new SetSpecialist("set-2", "Second Researcher", 35, 70, 50);
        second.setSkill(SetSkill.ROCKETS, 50.0);
        second.setAssignmentState(AssignmentState.ASSIGNED);
        second.setRdTargetComponentId("rocket-redstone");
        second.setRdSlot(0);
        snapshot.getSetSpecialists().add(second);

        List<ResolutionEvent> events = ResearchProgress.advanceSeason(snapshot, catalog, new Random(42L));

        assertEquals(1, events.size());
        assertEquals("set-1", ((ResearchCompleted) events.get(0)).personnelId());
        assertEquals(50.0, second.getSkill(SetSkill.ROCKETS));
    }

    /**
     * Verifies two specialists in different slots on the same component
     * both contribute, replaying the exact same draw sequence to pin the
     * shared-headroom formula precisely: both slots' raw amounts are drawn
     * against the component's reliability as it stood <em>before</em> this
     * season (50.0), not updated between slots - the behavior that
     * superseded this resolver's earlier sequential-stacking design once
     * {@code baspm.md} &sect;10.8 traced BASPM's actual mechanic.
     */
    @Test
    void twoResearchersInDifferentSlotsShareOneHeadroomFactor() {
        PlayerSnapshot snapshot = snapshotWithAssignedResearcher(80.0, 0);
        SetSpecialist second = new SetSpecialist("set-2", "Second Researcher", 35, 70, 50);
        second.setSkill(SetSkill.ROCKETS, 80.0);
        second.setAssignmentState(AssignmentState.ASSIGNED);
        second.setRdTargetComponentId("rocket-redstone");
        second.setRdSlot(3);
        snapshot.getSetSpecialists().add(second);

        List<ResolutionEvent> events = ResearchProgress.advanceSeason(snapshot, catalog, new Random(42L));

        Random replay = new Random(42L);
        SkillGrowth.drawRdGainFactor(replay); // the first researcher's skill-gain draw, discarded here
        SkillGrowth.drawRdGainFactor(replay); // the second researcher's skill-gain draw, discarded here
        double rawSlot0 = SkillGrowth.drawRdSlotRawAmount(RD_BANDS, 0, replay);
        double rawSlot3 = SkillGrowth.drawRdSlotRawAmount(RD_BANDS, 3, replay);
        double nominalSlot0 = SkillGrowth.rdReliabilityGain(rawSlot0, 50.0, 90.0, 80.0);
        double nominalSlot3 = SkillGrowth.rdReliabilityGain(rawSlot3, 50.0, 90.0, 80.0);
        double expectedDelta = nominalSlot0 + nominalSlot3;

        assertEquals(2, events.size());
        ResearchCompleted first = (ResearchCompleted) events.get(0);
        ResearchCompleted other = (ResearchCompleted) events.get(1);
        assertEquals(expectedDelta, first.reliabilityGain() + other.reliabilityGain(), 1e-9);
        assertTrue(first.reliabilityGain() > other.reliabilityGain(),
            "slot 0's richer band must out-contribute slot 3's poorer one for an equal-skill researcher");
        assertEquals(50.0 + expectedDelta,
            snapshot.getMissionComponents().get("rocket-redstone").getCurrentReliability(), 1e-9);
    }

    /**
     * Verifies the stagnation guard: once remaining headroom to the R&D
     * ceiling drops under 0.1, any positive nominal gain snaps the
     * component straight to {@code maxRdReliability} - re-deriving
     * {@code baspm.md} &sect;10.8's own guard against an asymptotic crawl
     * that never quite finishes - and that the snapped total is still split
     * proportionally between two occupied slots rather than handed
     * entirely to one.
     */
    @Test
    void snapsToCeilingAndSplitsProportionallyOnceHeadroomIsNearlyExhausted() {
        PlayerSnapshot snapshot = snapshotWithAssignedResearcher(80.0, 0);
        SetSpecialist second = new SetSpecialist("set-2", "Second Researcher", 35, 70, 50);
        second.setSkill(SetSkill.ROCKETS, 80.0);
        second.setAssignmentState(AssignmentState.ASSIGNED);
        second.setRdTargetComponentId("rocket-redstone");
        second.setRdSlot(3);
        snapshot.getSetSpecialists().add(second);
        snapshot.setMissionComponents(Map.of("rocket-redstone", new OwnedMissionComponent(89.95, 3.0)));

        List<ResolutionEvent> events = ResearchProgress.advanceSeason(snapshot, catalog, new Random(42L));

        ResearchCompleted first = (ResearchCompleted) events.get(0);
        ResearchCompleted other = (ResearchCompleted) events.get(1);
        assertEquals(90.0, snapshot.getMissionComponents().get("rocket-redstone").getCurrentReliability(), 1e-9);
        assertEquals(0.05, first.reliabilityGain() + other.reliabilityGain(), 1e-9);
        assertTrue(first.reliabilityGain() > other.reliabilityGain(),
            "the snapped total must still favor slot 0's richer nominal share over slot 3's");
    }

    /**
     * Verifies that R&D never pushes a component's reliability above its
     * own {@code maxRdReliability} ceiling, even when already sitting at it
     * - <em>and</em>, regression-covering a real bug this resolver had, that
     * the specialist still gets a {@link ResearchCompleted} event reporting
     * their flat skill gain (with a {@code 0} {@code reliabilityGain}),
     * rather than the whole season silently vanishing because the
     * component-level nominal reliability total was zero. Also exercises
     * the release-at-the-cap mechanic: a component already at its ceiling
     * when a researcher is assigned releases them the very next season too,
     * not only the season it actually reaches the ceiling.
     */
    @Test
    void neverExceedsMaxRdReliabilityButStillReportsTheSkillGainAndReleasesTheResearcher() {
        PlayerSnapshot snapshot = snapshotWithAssignedResearcher(80.0, 0);
        snapshot.setMissionComponents(Map.of("rocket-redstone", new OwnedMissionComponent(90.0, 3.0)));

        List<ResolutionEvent> events = ResearchProgress.advanceSeason(snapshot, catalog, new Random(42L));

        assertEquals(90.0, snapshot.getMissionComponents().get("rocket-redstone").getCurrentReliability());
        assertEquals(2, events.size());
        ResearchCompleted completed = (ResearchCompleted) events.get(0);
        assertEquals(0.0, completed.reliabilityGain());
        assertTrue(completed.skillGain() > 0);
        ResearchComponentMaxed maxed = (ResearchComponentMaxed) events.get(1);
        assertEquals("rocket-redstone", maxed.componentId());
        assertEquals(List.of("set-1"), maxed.releasedPersonnelIds());

        SetSpecialist released = snapshot.getSetSpecialists().get(0);
        assertEquals(AssignmentState.IDLE, released.getAssignmentState());
        assertNull(released.getRdTargetComponentId());
        assertNull(released.getRdSlot());
    }

    /**
     * Verifies that reaching the R&D ceiling <em>this</em> season (not
     * already sitting at it beforehand) releases every occupied slot's
     * researcher in one go, with one {@link ResearchComponentMaxed} event
     * listing both.
     */
    @Test
    void reachingTheCeilingThisSeasonReleasesEveryOccupiedSlot() {
        PlayerSnapshot snapshot = snapshotWithAssignedResearcher(80.0, 0);
        SetSpecialist second = new SetSpecialist("set-2", "Second Researcher", 35, 70, 50);
        second.setSkill(SetSkill.ROCKETS, 80.0);
        second.setAssignmentState(AssignmentState.ASSIGNED);
        second.setRdTargetComponentId("rocket-redstone");
        second.setRdSlot(3);
        snapshot.getSetSpecialists().add(second);
        snapshot.setMissionComponents(Map.of("rocket-redstone", new OwnedMissionComponent(89.95, 3.0)));

        List<ResolutionEvent> events = ResearchProgress.advanceSeason(snapshot, catalog, new Random(42L));

        assertEquals(90.0, snapshot.getMissionComponents().get("rocket-redstone").getCurrentReliability(), 1e-9);
        ResearchComponentMaxed maxed = (ResearchComponentMaxed) events.get(events.size() - 1);
        assertEquals("rocket-redstone", maxed.componentId());
        assertEquals(List.of("set-1", "set-2"), maxed.releasedPersonnelIds());
        assertEquals(AssignmentState.IDLE, snapshot.getSetSpecialists().get(0).getAssignmentState());
        assertEquals(AssignmentState.IDLE, second.getAssignmentState());
    }

    /**
     * Verifies that a component well below its R&D ceiling never releases
     * its researcher.
     */
    @Test
    void doesNotReleaseWhenFarBelowTheCeiling() {
        PlayerSnapshot snapshot = snapshotWithAssignedResearcher(50.0);

        List<ResolutionEvent> events = ResearchProgress.advanceSeason(snapshot, catalog, new Random(42L));

        assertEquals(1, events.size());
        assertEquals(AssignmentState.ASSIGNED, snapshot.getSetSpecialists().get(0).getAssignmentState());
    }
}
