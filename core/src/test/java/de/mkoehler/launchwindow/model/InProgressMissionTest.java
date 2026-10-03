package de.mkoehler.launchwindow.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * <p>Tests for {@link InProgressMission}.</p>
 *
 * @author Mario Koehler
 */
class InProgressMissionTest {

    /**
     * Verifies that the all-args constructor stores every field as given,
     * starting at season 0.
     */
    @Test
    void constructorStoresEveryFieldStartingAtSeasonZero() {
        InProgressMission mission =
            new InProgressMission("m0", "sputnik-1-std", List.of("astro-1"), List.of("mco-1"));

        assertEquals("m0", mission.getId());
        assertEquals("sputnik-1-std", mission.getMissionConfigurationId());
        assertEquals(0, mission.getCurrentSeasonIndex());
        assertEquals(List.of("astro-1"), mission.getCrewSlotIds());
        assertEquals(List.of("mco-1"), mission.getControlSlotIds());
    }

    /**
     * Verifies that the constructor defensively copies both id lists: a
     * mutation to the original list after construction does not affect
     * the stored mission.
     */
    @Test
    void defensivelyCopiesAssignedIdLists() {
        List<String> astronautIds = new ArrayList<>(List.of("astro-1"));
        List<String> controllerIds = new ArrayList<>(List.of("mco-1"));
        InProgressMission mission = new InProgressMission("m0", "sputnik-1-std", astronautIds, controllerIds);

        astronautIds.add("astro-2");
        controllerIds.add("mco-2");

        assertEquals(1, mission.getCrewSlotIds().size());
        assertEquals(1, mission.getControlSlotIds().size());
    }

    /**
     * Verifies the setters defensively copy an immutable list argument, so
     * passing {@code List.of(...)} does not leave the mission's own list
     * immutable.
     */
    @Test
    void settersCopyAnImmutableListIntoAMutableOne() {
        InProgressMission mission = new InProgressMission("m0", "sputnik-1-std", List.of(), List.of());

        mission.setCrewSlotIds(List.of("astro-1"));
        mission.setControlSlotIds(List.of("mco-1"));

        assertDoesNotThrow(() -> mission.getCrewSlotIds().add("astro-2"));
        assertDoesNotThrow(() -> mission.getControlSlotIds().add("mco-2"));
    }

    /**
     * Verifies the derived crew and ground lists skip the {@code null}s of unused slots.
     */
    @Test
    void derivedListsSkipUnusedSlots() {
        InProgressMission mission = new InProgressMission("m0", "mercury-orbital", Arrays.asList(null, "astro-1"),
            Arrays.asList("mco-1", null, "astro-2"));

        assertEquals(List.of("astro-1"), mission.crewIds());
        assertEquals(List.of("mco-1", "astro-2"), mission.groundIds());
    }

    /**
     * Verifies {@code setCurrentSeasonIndex} updates the field.
     */
    @Test
    void setCurrentSeasonIndexUpdatesTheField() {
        InProgressMission mission = new InProgressMission("m0", "sputnik-1-std", List.of(), List.of());

        mission.setCurrentSeasonIndex(2);

        assertEquals(2, mission.getCurrentSeasonIndex());
    }
}
