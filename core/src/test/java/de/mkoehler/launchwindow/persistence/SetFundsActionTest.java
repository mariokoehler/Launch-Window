package de.mkoehler.launchwindow.persistence;

import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * <p>Tests for {@link SetFundsAction}.</p>
 *
 * @author Mario Koehler
 */
class SetFundsActionTest {

    /**
     * Verifies that applying the action sets the snapshot's funds to the
     * declared value, regardless of what it was before.
     */
    @Test
    void applyToSetsSnapshotFundsToDeclaredValue() {
        PlayerSnapshot snapshot = new PlayerSnapshot("Hanseatic Space Agency", Archetype.NASA, 100, 0, 0, 1);
        SetFundsAction action = new SetFundsAction(5_000);

        action.apply(snapshot, null);

        assertEquals(5_000, snapshot.getFunds());
    }
}
