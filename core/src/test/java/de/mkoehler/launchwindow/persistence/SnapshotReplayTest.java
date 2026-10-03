package de.mkoehler.launchwindow.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * <p>Tests for {@link SnapshotReplay}.</p>
 *
 * @author Mario Koehler
 */
class SnapshotReplayTest {

    private final SnapshotReplay replay = new SnapshotReplay(new ObjectMapper());

    /**
     * Verifies that replaying no actions returns a snapshot equal to the
     * base one.
     */
    @Test
    void replayingNoActionsReturnsSnapshotEqualToBase() {
        PlayerSnapshot base = new PlayerSnapshot("Hanseatic Space Agency", Archetype.NASA, 1_000, 0, 0, 1);

        PlayerSnapshot result = replay.replay(base, List.of(), TestContexts.EARLY_ERA).state();

        assertEquals(1_000, result.getFunds());
        assertEquals("Hanseatic Space Agency", result.getAgencyName());
    }

    /**
     * Verifies that actions are applied in the given order, with later
     * actions superseding earlier ones for the same field.
     */
    @Test
    void appliesActionsInOrder() {
        PlayerSnapshot base = new PlayerSnapshot("Hanseatic Space Agency", Archetype.NASA, 1_000, 0, 0, 1);

        PlayerSnapshot result = replay.replay(base, List.of(new SetFundsAction(2_000), new SetFundsAction(3_000)),
            TestContexts.EARLY_ERA).state();

        assertEquals(3_000, result.getFunds());
    }

    /**
     * Verifies that replaying does not mutate the base snapshot passed in.
     */
    @Test
    void doesNotMutateTheBaseSnapshot() {
        PlayerSnapshot base = new PlayerSnapshot("Hanseatic Space Agency", Archetype.NASA, 1_000, 0, 0, 1);

        replay.replay(base, List.of(new SetFundsAction(9_999)), TestContexts.EARLY_ERA);

        assertEquals(1_000, base.getFunds());
    }

    /**
     * Verifies that an action that no longer validates is skipped and reported,
     * and the rest still apply.
     */
    @Test
    void anActionThatNoLongerValidatesIsSkippedAndReported() {
        PlayerSnapshot base = new PlayerSnapshot("Agency", Archetype.SOVIET, 500, 0, 0, 1);

        SnapshotReplay.Replayed replayed = replay.replay(base, List.of(
            new de.mkoehler.launchwindow.actions.OpenProgramAction("sputnik-1"), new SetFundsAction(3_000)),
            TestContexts.EARLY_ERA);

        assertEquals(List.of(new ActionResult(false, "INSUFFICIENT_FUNDS"), new ActionResult(true, "FUNDS_SET")),
            replayed.results());
        assertEquals(3_000, replayed.state().getFunds());
        assertEquals(List.of(), replayed.state().getOpenedProgramIds());
    }
}