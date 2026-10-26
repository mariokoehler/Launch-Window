package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.content.MissionStep;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.AstronautSkill;
import de.mkoehler.launchwindow.model.InProgressMission;
import de.mkoehler.launchwindow.model.PlayerSnapshot;

import java.util.List;

/**
 * Rocket-plane wings ({@code design.md} &sect;2.2(c), the user's idea, 2026-10-26): an astronaut who flies a
 * rocket plane (the X-15, the E-154) and lands it earns their wings for good, and every capsule flight they
 * pilot is safer for it. A step whose crew work reads piloting from a seat held by someone with wings gains
 * {@link #BONUS_POINTS} on its base chance, once however many of them share the step. The X-15's pilots
 * (Armstrong and Engle among them) took what they learnt into the capsules ({@code design.md} &sect;1.1).
 *
 * <p>Read the same way by the season pass and by {@link MissionAssessment}, so the planning odds are the
 * flight's own.</p>
 *
 * @author Mario Koehler
 */
public final class RocketPlaneWings {

    /** The program category whose flights earn the wings. */
    public static final String CATEGORY = "space-planes";

    /**
     * The base-chance points the wings add to a capsule step they pilot (unconfirmed, 2026-10-26): the
     * findings' size ({@link Findings#BONUS_POINTS}), so the two read alike.
     */
    public static final double BONUS_POINTS = 4.0;

    private RocketPlaneWings() {
    }

    /**
     * Returns whether a configuration's flights earn the wings: any rocket plane's.
     *
     * @param catalog       the content pack
     * @param configuration the configuration
     * @return whether they do
     */
    public static boolean earns(ContentCatalog catalog, MissionConfiguration configuration) {
        return catalog.program(configuration.programId())
            .map(program -> CATEGORY.equals(program.categoryId())).orElse(false);
    }

    /**
     * Returns whether a configuration's piloting steps gain from the wings: a crewed flight that isn't a
     * rocket plane's own.
     *
     * @param catalog       the content pack
     * @param configuration the configuration
     * @return whether they do
     */
    public static boolean applies(ContentCatalog catalog, MissionConfiguration configuration) {
        return !configuration.crewSlots().isEmpty() && !earns(catalog, configuration);
    }

    /**
     * Returns the points the wings add to a step: {@link #BONUS_POINTS} if the step reads piloting from a
     * seat whose holder has their wings, else 0.
     *
     * @param snapshot the player
     * @param catalog  the content pack
     * @param mission  the mission, flying or a plan
     * @param step     the step
     * @return the points
     */
    public static double stepBonus(PlayerSnapshot snapshot, ContentCatalog catalog, InProgressMission mission,
            MissionStep step) {
        MissionConfiguration configuration = catalog.missionConfiguration(mission.getMissionConfigurationId())
            .orElse(null);
        if (configuration == null || !applies(catalog, configuration)) {
            return 0;
        }
        List<String> seats = mission.getCrewSlotIds();
        for (MissionStep.CrewInvolvement involvement : step.crewInvolvements()) {
            if (involvement.skill() != AstronautSkill.PILOTING || involvement.slot() >= seats.size()) {
                continue;
            }
            String holderId = seats.get(involvement.slot());
            if (snapshot.getAstronauts().stream()
                    .anyMatch(astronaut -> astronaut.getId().equals(holderId) && astronaut.isRocketPlaneWings())) {
                return BONUS_POINTS;
            }
        }
        return 0;
    }

    /**
     * Gives the crew of a rocket-plane flight that came home their wings, reporting each one new to them.
     * Called when a flight completes; a failed flight earns nothing.
     *
     * @param snapshot      the player
     * @param catalog       the content pack
     * @param configuration the configuration flown
     * @param mission       the flight
     * @param events        the season's events
     */
    static void award(PlayerSnapshot snapshot, ContentCatalog catalog, MissionConfiguration configuration,
            InProgressMission mission, List<ResolutionEvent> events) {
        if (!earns(catalog, configuration)) {
            return;
        }
        for (Astronaut astronaut : snapshot.getAstronauts()) {
            if (mission.crewIds().contains(astronaut.getId()) && !astronaut.isRocketPlaneWings()) {
                astronaut.setRocketPlaneWings(true);
                events.add(new RocketPlaneWingsEarned(mission.getId(), configuration.id(), astronaut.getId(),
                    astronaut.getName()));
            }
        }
    }
}
