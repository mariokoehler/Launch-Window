package de.mkoehler.launchwindow.rules;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.actions.DecideAction;
import de.mkoehler.launchwindow.actions.HandsOnAction;
import de.mkoehler.launchwindow.content.BudgetSchedule;
import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.ContentPackLoader;
import de.mkoehler.launchwindow.content.Decisions;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.HandsOnState;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.PendingDecision;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSkill;
import de.mkoehler.launchwindow.model.SetSpecialist;
import de.mkoehler.launchwindow.persistence.ActionContext;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the hands-on decision as a decision ({@code ui-spec.md} component 26, type 11): starting it,
 * stepping it through actions, settling when it ends or as it stands when the turn resolves, a
 * replay drawing the same rolls, and every level in {@code decisions.json} winnable.
 *
 * @author Mario Koehler
 */
class HandsOnDecisionTest {

    private static final ContentCatalog PACK = ContentPackLoader.load(Path.of("..", "assets", "content",
        "early-era"));
    private static final Decisions DECISIONS = Decisions.load(Path.of("..", "assets", "content", "standard", Decisions.FILE));
    private static final ActionContext CONTEXT = new ActionContext(PACK, BudgetSchedule.baspmClose());
    private static final String ROCKET = "r7-vostok";

    private static PlayerSnapshot agency() {
        PlayerSnapshot snapshot = new PlayerSnapshot("Vega Bureau", Archetype.SOVIET, 50_000, 250, 3_000, 1);
        snapshot.getMissionComponents().put(ROCKET, new OwnedMissionComponent(40, 3));
        SetSpecialist lead = new SetSpecialist("e1", "Gleb Arkhipov", 41, 70, 60);
        lead.setSkill(SetSkill.HUMAN_RATED_ROCKETS, 74);
        snapshot.setSetSpecialists(List.of(lead));
        PersonnelActions.assignResearch(snapshot, "e1", ROCKET, PACK);
        snapshot.setPendingDecision(PendingDecision.offered("big-test-stand", ROCKET, 5, 12345L));
        return snapshot;
    }

    private static void step(PlayerSnapshot snapshot, RdDecisions.HandsOnStep step, String command) {
        assertTrue(new HandsOnAction(step, command).apply(snapshot, CONTEXT).applied(), step + " " + command);
    }

    @Test
    void runningItStartsThePuzzleWithTheTeamsOdds() {
        PlayerSnapshot snapshot = agency();
        assertTrue(new DecideAction("run", null, null).apply(snapshot, CONTEXT).applied());
        PendingDecision pending = snapshot.getPendingDecision();
        assertTrue(pending.handsOnInProgress());
        assertFalse(pending.decided());
        assertEquals(0.35 - 0.0025 * 74, pending.handsOn().failChance(), 1e-9);
        assertEquals(HandsOn.EXCEPTIONAL, pending.handsOn().exceptionalChance(), 1e-9);
        assertEquals(RdDecisions.Outcome.ALREADY_DECIDED, RdDecisions.decide(snapshot, PACK, DECISIONS, "team", null,
            null), "started, it counts");
        assertEquals(pending.handsOn(), pending.withoutSeed().handsOn(), "the puzzle reaches the client");
        assertEquals(0, pending.withoutSeed().seed(), "its rolls' seed doesn't");
    }

    @Test
    void theBonusPathSettlesAtOnceWithItsOutcome() {
        PlayerSnapshot snapshot = agency();
        new DecideAction("run", null, null).apply(snapshot, CONTEXT);
        // Lucky or not, play the solver's plan, calling in experts while shifts last.
        for (int guard = 0; guard < 40 && snapshot.getPendingDecision().handsOnInProgress(); guard++) {
            HandsOnState state = snapshot.getPendingDecision().handsOn();
            if (state.phase() == HandsOnState.Phase.AWAITING_RESIST) {
                step(snapshot, RdDecisions.HandsOnStep.RESIST, null);
                continue;
            }
            List<List<String>> plan = HandsOn.solve(DECISIONS.byId("big-test-stand").orElseThrow().handsOn(), state);
            for (String command : plan == null || plan.isEmpty() ? List.<String>of() : plan.getFirst()) {
                step(snapshot, RdDecisions.HandsOnStep.QUEUE, command);
            }
            step(snapshot, RdDecisions.HandsOnStep.CONFIRM, null);
        }
        PendingDecision pending = snapshot.getPendingDecision();
        assertTrue(pending.decided());
        assertEquals("run", pending.optionId());
        assertEquals(pending.handsOn().result().name().toLowerCase(), pending.outcomeId());
        assertFalse(new HandsOnAction(RdDecisions.HandsOnStep.CONFIRM, null).apply(snapshot, CONTEXT).applied(),
            "nothing left to play");
    }

    @Test
    void aReplayDrawsTheSameRolls() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        PlayerSnapshot a = agency();
        PlayerSnapshot b = mapper.readValue(mapper.writeValueAsBytes(a), PlayerSnapshot.class);
        for (PlayerSnapshot snapshot : List.of(a, b)) {
            new DecideAction("run", null, null).apply(snapshot, CONTEXT);
            step(snapshot, RdDecisions.HandsOnStep.QUEUE, "make-firing");
            step(snapshot, RdDecisions.HandsOnStep.QUEUE, "sensor-sweep");
            step(snapshot, RdDecisions.HandsOnStep.CONFIRM, null);
        }
        assertEquals(a.getPendingDecision(), b.getPendingDecision());
    }

    @Test
    void anUnfinishedCampaignSettlesAsItStandsWithoutAPause() {
        PlayerSnapshot snapshot = agency();
        new DecideAction("run", null, null).apply(snapshot, CONTEXT);
        step(snapshot, RdDecisions.HandsOnStep.QUEUE, "make-firing");
        List<ResolutionEvent> events = RdDecisions.advanceSeason(snapshot, PACK, DECISIONS, 6);
        DecisionSettled settled = (DecisionSettled) events.stream().filter(DecisionSettled.class::isInstance)
            .findFirst().orElseThrow();
        assertEquals("run", settled.optionId());
        assertEquals("failure", settled.outcomeId(), "the queued firing was refunded and the rounds ran empty");
        assertFalse(settled.lapsed());
        assertFalse(snapshot.isDecisionPaused(), "the player acted");
    }

    @Test
    void leavingItToTheTeamChangesNothing() {
        PlayerSnapshot snapshot = agency();
        assertTrue(new DecideAction("team", null, null).apply(snapshot, CONTEXT).applied());
        assertTrue(snapshot.getPendingDecision().decided());
        assertNull(snapshot.getPendingDecision().handsOn());
        assertEquals(40, snapshot.getMissionComponents().get(ROCKET).getCurrentReliability(), 1e-9);
    }

    @Test
    void everyLevelCanBeWonWithEveryRollAsWritten() {
        long levels = DECISIONS.decisions().stream().filter(decision -> decision.handsOn() != null).count();
        assertTrue(levels >= 2);
        for (Decisions.Decision decision : DECISIONS.decisions()) {
            if (decision.handsOn() == null) {
                continue;
            }
            HandsOnState start = HandsOn.start(decision.handsOn(), 0, 0);
            List<List<String>> plan = HandsOn.solve(decision.handsOn(), start);
            assertNotNull(plan, decision.id());
            HandsOnState state = start;
            for (List<String> round : plan) {
                for (String command : round) {
                    state = HandsOn.queue(decision.handsOn(), state, command).state();
                }
                state = HandsOn.confirm(decision.handsOn(), state, index -> 0.5).state();
            }
            assertEquals(HandsOnState.Result.BONUS, state.result(), decision.id() + " can't reach its bonus");
            assertTrue(decision.playOption().isPresent(), decision.id());
            for (String outcome : List.of("bonus", "success", "failure")) {
                assertTrue(decision.outcome(outcome).isPresent(), decision.id() + " has no " + outcome);
            }
        }
    }
}
