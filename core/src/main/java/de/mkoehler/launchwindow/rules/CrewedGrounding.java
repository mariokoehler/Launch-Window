package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.model.InProgressMission;
import de.mkoehler.launchwindow.model.PlayerSnapshot;

import java.util.List;
import java.util.Objects;

/**
 * Grounds an agency's crewed flights after it loses a crew in flight ({@code design.md} &sect;2.5(j),
 * the user's decision 2026-10-12): for {@link #GROUNDING_SEASONS} turns no flight with crew seats
 * may launch, as NASA stood down after Apollo 1 and the Soviets after Soyuz 1 and Soyuz 11.
 * Uncrewed flights, and flights already launched, go on: Apollo 4, 5 and 6 flew uncrewed during
 * the Apollo 1 stand-down. A second loss while grounded starts the count again, never adds to it.
 *
 * <p>The count runs down at the start of the missions pass, before any flight resolves, so a loss
 * in turn T's resolution refuses crewed launches in exactly the turns T+1 to T+{@link
 * #GROUNDING_SEASONS}.</p>
 *
 * @author Mario Koehler
 */
public final class CrewedGrounding {

    /** Turns crewed launches stay grounded after a crew is lost. Unconfirmed default, 2026-10-12. */
    public static final int GROUNDING_SEASONS = 4;

    private CrewedGrounding() {
    }

    /**
     * Whether crewed launches are grounded now.
     *
     * @param snapshot the player
     * @return {@code true} while the count runs
     */
    public static boolean grounded(PlayerSnapshot snapshot) {
        return Objects.requireNonNull(snapshot, "snapshot must not be null").getCrewedGroundingSeasons() > 0;
    }

    /**
     * Whether a configuration is a crewed flight, which a grounding holds: it has crew seats to fill.
     * A CAPCOM on the ground or an animal passenger doesn't make it one.
     *
     * @param configuration the configuration
     * @return {@code true} if it flies astronauts
     */
    public static boolean crewed(MissionConfiguration configuration) {
        return !Objects.requireNonNull(configuration, "configuration must not be null").usedCrewSlots().isEmpty();
    }

    /**
     * Counts one turn of a grounding down; called at the start of the missions pass.
     *
     * @param snapshot the player; mutated in place
     */
    static void countDown(PlayerSnapshot snapshot) {
        if (snapshot.getCrewedGroundingSeasons() > 0) {
            snapshot.setCrewedGroundingSeasons(snapshot.getCrewedGroundingSeasons() - 1);
        }
    }

    /**
     * Grounds crewed flights after a mission lost its crew, and reports it.
     *
     * @param snapshot the player; mutated in place
     * @param mission  the mission that lost its crew
     * @param events   the list the report's event goes to
     */
    static void ground(PlayerSnapshot snapshot, InProgressMission mission, List<ResolutionEvent> events) {
        snapshot.setCrewedGroundingSeasons(Math.max(snapshot.getCrewedGroundingSeasons(), GROUNDING_SEASONS));
        events.add(new CrewedFlightsGrounded(mission.getId(), mission.getMissionConfigurationId(),
            GROUNDING_SEASONS));
    }
}
