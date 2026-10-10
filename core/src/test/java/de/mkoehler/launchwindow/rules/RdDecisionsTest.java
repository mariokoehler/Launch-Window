package de.mkoehler.launchwindow.rules;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.actions.DecideAction;
import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.ContentPackLoader;
import de.mkoehler.launchwindow.content.Decisions;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.PendingDecision;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSkill;
import de.mkoehler.launchwindow.model.SetSpecialist;
import de.mkoehler.launchwindow.persistence.ActionContext;
import de.mkoehler.launchwindow.persistence.SnapshotReplay;
import de.mkoehler.launchwindow.content.BudgetSchedule;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the R&amp;D decisions' rules: offered in lulls, decided at once and the same every time,
 * lapsing to the default and pausing until the player acts, the R&amp;D factor in the season pass,
 * and the seed kept from the client.
 *
 * @author Mario Koehler
 */
class RdDecisionsTest {

    private static final ContentCatalog PACK = ContentPackLoader.load(Path.of("..", "assets", "content",
        "early-era"));
    private static final Decisions DECISIONS = Decisions.load(Path.of("..", "assets", "content", Decisions.FILE));
    private static final String CAPSULE = "vostok-capsule";

    /** A Soviet agency with Vostok's capsule at 30 % and two engineers on it, one idle. */
    private static PlayerSnapshot agency() {
        PlayerSnapshot snapshot = new PlayerSnapshot("Vega Bureau", Archetype.SOVIET, 50_000, 250, 3_000, 1);
        snapshot.getOpenedProgramIds().add("vostok");
        snapshot.getMissionComponents().put(CAPSULE, new OwnedMissionComponent(30, 30));
        List<SetSpecialist> engineers = new ArrayList<>();
        for (String id : List.of("e1", "e2", "e3")) {
            SetSpecialist engineer = new SetSpecialist(id, "Engineer " + id, 35, 70, 60);
            engineer.setSkill(SetSkill.CREWED_SPACECRAFT, 60);
            engineers.add(engineer);
        }
        snapshot.setSetSpecialists(engineers);
        PersonnelActions.assignResearch(snapshot, "e1", CAPSULE, PACK);
        PersonnelActions.assignResearch(snapshot, "e2", CAPSULE, PACK);
        return snapshot;
    }

    private static PendingDecision offer(PlayerSnapshot snapshot, String decisionId) {
        PendingDecision pending = PendingDecision.offered(decisionId, CAPSULE, 5, 12345L);
        snapshot.setPendingDecision(pending);
        return pending;
    }

    /** In a lull (nothing flying, the capsule far from 60 %) a decision comes almost every season. */
    @Test
    void lullsBringDecisions() {
        PlayerSnapshot snapshot = agency();
        int offered = 0;
        for (int turn = 10; turn < 30; turn++) {
            List<ResolutionEvent> events = RdDecisions.advanceSeason(snapshot, PACK, DECISIONS, turn);
            if (events.stream().anyMatch(DecisionOffered.class::isInstance)) {
                offered++;
                // The player answers with the default, so no lapse pauses the next.
                PendingDecision pending = snapshot.getPendingDecision();
                String fallback = DECISIONS.byId(pending.decisionId()).orElseThrow().defaultOption();
                assertEquals(RdDecisions.Outcome.DECIDED, RdDecisions.decide(snapshot, PACK, DECISIONS, fallback, null,
                    null));
                snapshot.getMissionComponents().get(CAPSULE).setCurrentReliability(30);
            }
        }
        assertTrue(offered >= 14, offered + " of 20");
    }

    /** Deciding draws the same outcome for the same decision, so a replay can't differ. */
    @Test
    void decidingIsTheSameEveryTime() {
        String first = null;
        for (int run = 0; run < 3; run++) {
            PlayerSnapshot snapshot = agency();
            offer(snapshot, "static-firing");
            snapshot.getMissionComponents().put(CAPSULE, new OwnedMissionComponent(30, 30));
            assertEquals(RdDecisions.Outcome.DECIDED, RdDecisions.decide(snapshot, PACK, DECISIONS, "full", null, null));
            String outcome = snapshot.getPendingDecision().outcomeId();
            assertNotNull(outcome);
            if (first == null) {
                first = outcome;
            }
            assertEquals(first, outcome);
        }
    }

    /** A decided decision can't be decided again, and the cost is paid. */
    @Test
    void decideOnceAndPay() {
        PlayerSnapshot snapshot = agency();
        offer(snapshot, "extra-inspections");
        long funds = snapshot.getFunds();
        assertEquals(RdDecisions.Outcome.DECIDED, RdDecisions.decide(snapshot, PACK, DECISIONS, "inspect", null, null));
        assertEquals(funds - Math.round(0.2 * 3_000), snapshot.getFunds());
        assertTrue(snapshot.getMissionComponents().get(CAPSULE).getCurrentReliability() > 30);
        assertEquals(RdDecisions.Outcome.ALREADY_DECIDED, RdDecisions.decide(snapshot, PACK, DECISIONS, "trust", null,
            null));
    }

    /** An overseer must be idle; a busy engineer is refused. */
    @Test
    void overseerMustBeIdle() {
        PlayerSnapshot snapshot = agency();
        offer(snapshot, "who-signs-vibration");
        assertEquals(RdDecisions.Outcome.PERSON_NOT_FREE, RdDecisions.decide(snapshot, PACK, DECISIONS, "assign", "e1",
            null));
        assertEquals(RdDecisions.Outcome.DECIDED, RdDecisions.decide(snapshot, PACK, DECISIONS, "assign", "e3", null));
    }

    /** A lapse applies the default, pauses new decisions, and any action lifts the pause. */
    @Test
    void lapseAppliesTheDefaultAndPauses() {
        PlayerSnapshot snapshot = agency();
        offer(snapshot, "static-firing");
        List<ResolutionEvent> events = RdDecisions.advanceSeason(snapshot, PACK, DECISIONS, 6);
        DecisionSettled settled = events.stream().filter(DecisionSettled.class::isInstance)
            .map(DecisionSettled.class::cast).findFirst().orElseThrow();
        assertTrue(settled.lapsed());
        assertEquals("skip", settled.optionId());
        assertTrue(snapshot.isDecisionPaused());
        assertNull(snapshot.getPendingDecision(), "no new one while paused");

        ActionContext context = new ActionContext(PACK, BudgetSchedule.baspmClose());
        SnapshotReplay.applySafely(new de.mkoehler.launchwindow.actions.ReleaseResearchAction("e2"), snapshot, context);
        assertFalse(snapshot.isDecisionPaused());
    }

    /** The decide action goes through the same rules. */
    @Test
    void decideAction() {
        PlayerSnapshot snapshot = agency();
        offer(snapshot, "freeze-design");
        Decisions.setStandard(DECISIONS);
        try {
            ActionContext context = new ActionContext(PACK, BudgetSchedule.baspmClose());
            assertTrue(SnapshotReplay.applySafely(new DecideAction("freeze", null, null), snapshot, context).applied());
        } finally {
            // Other tests in this JVM expect core's classpath: no decisions.
            Decisions.setStandard(Decisions.EMPTY);
        }
        OwnedMissionComponent owned = snapshot.getMissionComponents().get(CAPSULE);
        assertEquals(1.3, owned.getRdFactor(), 1e-9);
        assertEquals(4, owned.getRdFactorSeasons());
    }

    /** A component's R&D factor scales its gain in the season pass. */
    @Test
    void factorScalesTheSeasonPass() {
        double plain = ResearchPlanning.expectedSeasonGain(agency(), PACK, CAPSULE);
        PlayerSnapshot boosted = agency();
        boosted.getMissionComponents().get(CAPSULE).setRdFactor(2);
        boosted.getMissionComponents().get(CAPSULE).setRdFactorSeasons(3);
        assertEquals(plain * 2, ResearchPlanning.expectedSeasonGain(boosted, PACK, CAPSULE), plain * 0.01);
    }

    /** The client never sees the seed. */
    @Test
    void seedHiddenFromTheClient() {
        PlayerSnapshot snapshot = agency();
        offer(snapshot, "static-firing");
        PlayerSnapshot copy = Personality.forClient(snapshot, new ObjectMapper());
        assertEquals(0, copy.getPendingDecision().seed());
        assertEquals(12345L, snapshot.getPendingDecision().seed());
    }

    /** A continuing outcome sets up the arc's next step. */
    @Test
    void storyArcsContinue() {
        PlayerSnapshot snapshot = agency();
        // The tests above: this decision and seed give its good outcome or the rough run; force the good one.
        snapshot.setPendingDecision(PendingDecision.offered("vacuum-chamber", CAPSULE, 5, findGoodSeed()));
        RdDecisions.decide(snapshot, PACK, DECISIONS, "book", null, null);
        assertEquals("passed", snapshot.getPendingDecision().outcomeId());
        assertEquals("long-soak", snapshot.getFollowUpDecisionId());
        assertEquals(CAPSULE, snapshot.getFollowUpComponentId());
        assertEquals(AssignmentState.ASSIGNED, snapshot.getSetSpecialists().get(0).getAssignmentState());
    }

    /** A seed whose roll lands on the vacuum chamber's good outcome. */
    private static long findGoodSeed() {
        for (long seed = 1; seed < 100; seed++) {
            if (new java.util.SplittableRandom(seed).nextDouble() < 0.5) {
                return seed;
            }
        }
        throw new AssertionError("no seed");
    }
}
