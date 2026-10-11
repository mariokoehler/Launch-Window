package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.HandsOnLevel;
import de.mkoehler.launchwindow.model.HandsOnState;
import de.mkoehler.launchwindow.model.HandsOnState.Phase;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntToDoubleFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the hands-on puzzle's rules: the acceptance tests of the user's spec (section 12), with
 * the test campaign's own names.
 *
 * @author Mario Koehler
 */
class HandsOnTest {

    /** The spec's reference level as a rocket's test campaign. */
    static final HandsOnLevel LEVEL = new HandsOnLevel(List.of(
            new HandsOnLevel.Resource("shift", "Shifts", "shift", "res_shift", "gold"),
            new HandsOnLevel.Resource("firing", "Firings", "firing", "res_firing", "danger"),
            new HandsOnLevel.Resource("measurement", "Measurements", "measurement", "res_measurement", "navy"),
            new HandsOnLevel.Resource("part", "Spare parts", "spare part", "res_spare_part", "success")),
        List.of(
            command("static-firing", "firing", Map.of("shift", 2), amounts("firing", 2)),
            command("data-retest", "firing", Map.of("measurement", 1), amounts("firing", 2, "part", 1)),
            command("rebuilt-firing", "firing", Map.of("part", 1), amounts("firing", 2, "measurement", 1)),
            command("sensor-sweep", "measurement", Map.of("shift", 1), amounts("measurement", 2)),
            command("teardown", "measurement", amounts("shift", 1, "firing", 1), amounts("measurement", 3)),
            command("parts-inspection", "measurement", Map.of("part", 2), amounts("measurement", 3)),
            command("order-stores", "part", Map.of("shift", 1), amounts("part", 1)),
            command("machine-drawings", "part", Map.of("measurement", 1), amounts("part", 2)),
            command("salvage", "part", Map.of("firing", 1), amounts("part", 1, "measurement", 1)),
            new HandsOnLevel.Command("overtime", "Overtime", "shift", Map.of(), Map.of("shift", 1), "shift", false)),
        4, 3, Map.of("shift", 6), amounts("firing", 3, "measurement", 4), amounts("firing", 6, "measurement", 7), 1);

    private static HandsOnLevel.Command command(String id, String group, Map<String, Integer> in,
            Map<String, Integer> out) {
        return new HandsOnLevel.Command(id, id, group, in, out, out.keySet().iterator().next(), true);
    }

    private static Map<String, Integer> amounts(Object... pairs) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((String) pairs[i], (Integer) pairs[i + 1]);
        }
        return map;
    }

    /** Every roll as written. */
    private static final IntToDoubleFunction NORMAL = index -> 0.5;
    /** Every roll a fail. */
    private static final IntToDoubleFunction FAIL = index -> 0.0;
    /** Every roll exceptional. */
    private static final IntToDoubleFunction EXCEPTIONAL = index -> 0.2;

    private static HandsOnState fresh() {
        return HandsOn.start(LEVEL, 0.17, 0.15);
    }

    private static HandsOnState queue(HandsOnState state, String... ids) {
        HandsOnState current = state;
        for (String id : ids) {
            HandsOn.Step step = HandsOn.queue(LEVEL, current, id);
            assertEquals(HandsOn.Outcome.DONE, step.outcome(), id);
            current = step.state();
        }
        return current;
    }

    @Test
    void atTheStartOnlyTheShiftCommandsCanBeQueued() {
        HandsOnState state = fresh();
        List<String> queueable = LEVEL.commands().stream().filter(command -> HandsOn.canQueue(LEVEL, state, command))
            .map(HandsOnLevel.Command::id).toList();
        assertEquals(List.of("static-firing", "sensor-sweep", "order-stores", "overtime"), queueable);
    }

    @Test
    void inputsArePaidOnQueueingAndRefundedByUndo() {
        HandsOnState queued = queue(fresh(), "static-firing");
        assertEquals(4, queued.held("shift"));
        assertEquals(0, queued.held("firing"));
        HandsOnState undone = HandsOn.undo(LEVEL, queued).state();
        assertEquals(6, undone.held("shift"));
        assertTrue(undone.queue().isEmpty());
        assertEquals(HandsOn.Outcome.NOTHING_QUEUED, HandsOn.undo(LEVEL, undone).outcome());
    }

    @Test
    void outputsDontPayForAnotherCommandTheSameRound() {
        HandsOnState state = queue(fresh(), "sensor-sweep");
        assertEquals(HandsOn.Outcome.CANT_PAY, HandsOn.queue(LEVEL, state, "data-retest").outcome());
    }

    @Test
    void aFourthCommandIsRefusedAndChangesNothing() {
        HandsOnState full = queue(fresh(), "overtime", "overtime", "overtime");
        HandsOn.Step step = HandsOn.queue(LEVEL, full, "overtime");
        assertEquals(HandsOn.Outcome.NO_SLOT, step.outcome());
        assertEquals(full, step.state());
    }

    @Test
    void aFailCanBeAcceptedOrResisted() {
        HandsOnState waiting = HandsOn.confirm(LEVEL, queue(fresh(), "static-firing"), FAIL).state();
        assertEquals(Phase.AWAITING_RESIST, waiting.phase());
        HandsOnState accepted = HandsOn.accept(LEVEL, waiting, FAIL).state();
        assertEquals(1, accepted.held("firing"));
        assertEquals(4, accepted.held("shift"));
        HandsOnState resisted = HandsOn.resist(LEVEL, waiting, FAIL).state();
        assertEquals(2, resisted.held("firing"));
        assertEquals(3, resisted.held("shift"), "the expert costs a shift");
        assertTrue(resisted.results().getFirst().resisted());
        assertEquals(2, resisted.round());
    }

    @Test
    void anExceptionalResultAddsOneToTheMainOutputOnly() {
        HandsOnState state = queue(fresh(), "sensor-sweep");
        state = HandsOn.confirm(LEVEL, state, NORMAL).state();
        state = HandsOn.confirm(LEVEL, queue(state, "machine-drawings"), NORMAL).state();
        state = HandsOn.confirm(LEVEL, queue(state, "data-retest"), EXCEPTIONAL).state();
        assertEquals(3, state.held("firing"));
        assertEquals(3, state.held("part"), "the second output is paid as written");
    }

    @Test
    void withNoShiftsLeftAFailJustApplies() {
        HandsOnState state = queue(fresh(), "static-firing", "static-firing", "static-firing");
        assertEquals(0, state.held("shift"));
        HandsOnState done = HandsOn.confirm(LEVEL, state, FAIL).state();
        assertEquals(Phase.PLANNING, done.phase());
        assertEquals(3, done.held("firing"));
    }

    @Test
    void overtimeNeverRollsAndFundsALaterExpert() {
        HandsOnState state = queue(fresh(), "static-firing", "static-firing", "sensor-sweep");
        state = queue(HandsOn.confirm(LEVEL, state, NORMAL).state(), "overtime", "teardown");
        assertEquals(0, state.held("shift"));
        HandsOnState waiting = HandsOn.confirm(LEVEL, state, FAIL).state();
        assertEquals(Phase.AWAITING_RESIST, waiting.phase(), "overtime's shift arrived before the teardown failed");
        assertEquals(HandsOnState.Roll.NONE, waiting.results().getFirst().rolled());
        assertEquals(3 + 1, waiting.rolls(), "round 1's three rolls, then the teardown's; overtime drew none");
    }

    @Test
    void theBonusPathEndsTheCampaignInRoundThree() {
        HandsOnState state = queue(fresh(), "sensor-sweep", "sensor-sweep", "sensor-sweep");
        state = HandsOn.confirm(LEVEL, state, NORMAL).state();
        state = HandsOn.confirm(LEVEL, queue(state, "sensor-sweep", "data-retest", "data-retest"), NORMAL).state();
        state = HandsOn.confirm(LEVEL, queue(state, "rebuilt-firing"), NORMAL).state();
        assertEquals(Phase.FINISHED, state.phase());
        assertEquals(HandsOnState.Result.BONUS, state.result());
        assertEquals(Map.of("shift", 2, "firing", 6, "measurement", 7, "part", 1), state.held());
    }

    @Test
    void fourEmptyRoundsFail() {
        HandsOnState state = fresh();
        for (int i = 0; i < 4; i++) {
            state = HandsOn.confirm(LEVEL, state, NORMAL).state();
        }
        assertEquals(HandsOnState.Result.FAILURE, state.result());
        assertEquals(HandsOn.Outcome.WRONG_PHASE, HandsOn.queue(LEVEL, state, "overtime").outcome());
    }

    @Test
    void anUnfinishedCampaignSettlesAsItStands() {
        HandsOnState state = HandsOn.confirm(LEVEL, queue(fresh(), "static-firing", "static-firing", "sensor-sweep"),
            NORMAL).state();
        state = queue(state, "teardown");
        HandsOnState finished = HandsOn.finishAsItStands(LEVEL, state, NORMAL);
        assertEquals(Phase.FINISHED, finished.phase());
        assertEquals(1, finished.held("shift"), "the unconfirmed queue was refunded");
        assertEquals(HandsOnState.Result.FAILURE, finished.result(), "4 firings but only 2 measurements");
    }

    @Test
    void theSolverFindsTheBonusAndItPlaysOutAsPlanned() {
        List<List<String>> plan = HandsOn.solve(LEVEL, fresh());
        assertNotNull(plan);
        HandsOnState state = fresh();
        for (List<String> round : plan) {
            state = HandsOn.confirm(LEVEL, queue(state, round.toArray(String[]::new)), NORMAL).state();
        }
        assertEquals(HandsOnState.Result.BONUS, state.result());
    }

    @Test
    void theSameRollsGiveTheSameCampaign() {
        IntToDoubleFunction rolls = index -> new java.util.SplittableRandom(99 + index).nextDouble();
        HandsOnState a = HandsOn.confirm(LEVEL, queue(fresh(), "static-firing", "sensor-sweep"), rolls).state();
        HandsOnState b = HandsOn.confirm(LEVEL, queue(fresh(), "static-firing", "sensor-sweep"), rolls).state();
        assertEquals(a, b);
    }
}
