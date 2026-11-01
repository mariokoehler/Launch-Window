package de.mkoehler.launchwindow.client.view;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.mkoehler.launchwindow.model.ActiveContract;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import org.junit.jupiter.api.Test;

/**
 * The reminder on the hub's Headquarters marker.
 *
 * @author Mario Koehler
 */
class ContractsViewTest {

    private static final ActiveContract AWARDED = new ActiveContract("listing", "line", null, "program",
        "configuration", 1, 3_390, 22, 25, null);

    private final PlayerSnapshot state = new PlayerSnapshot("Vega", Archetype.SOVIET, 25_000, 250, 3_000, 1);

    @Test
    void noContractNoReminder() {
        assertNull(ContractsView.launchReminder(state, 23));
    }

    @Test
    void anAwardedContractWaitsAndSaysHowLongItHas() {
        state.setActiveContract(AWARDED);
        String reminder = ContractsView.launchReminder(state, 23);
        assertNotNull(reminder);
        assertTrue(reminder.endsWith("3 seasons left"), reminder);
    }

    @Test
    void aLaunchedMissionEndsTheReminder() {
        state.setActiveContract(AWARDED.withMission("m1"));
        assertNull(ContractsView.launchReminder(state, 23));
    }

    @Test
    void aMissionCalledOffBringsItBack() {
        state.setActiveContract(AWARDED.withMission("m1").withMission(null));
        assertNotNull(ContractsView.launchReminder(state, 23));
    }
}
