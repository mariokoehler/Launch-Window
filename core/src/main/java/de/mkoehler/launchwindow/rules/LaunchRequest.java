package de.mkoehler.launchwindow.rules;

import java.util.List;
import java.util.Objects;

/**
 * <p>Everything a player chooses when launching a mission: the configuration, the
 * crew, the flight controllers, the CAPCOM astronauts on the ground, and one
 * rocket per launch. See {@link MissionLaunch#launch(de.mkoehler.launchwindow.model.PlayerSnapshot,
 * LaunchRequest, de.mkoehler.launchwindow.content.ContentCatalog)} and
 * {@code design.md} &sect;5 (M3).</p>
 *
 * @param missionConfigurationId   the configuration to fly
 * @param astronautIds             the crew
 * @param missionControlOfficerIds the flight controllers
 * @param capcomAstronautIds       the astronauts serving as CAPCOM
 * @param rocketIds                the rocket for each launch, in launch order
 * @author Mario Koehler
 */
public record LaunchRequest(String missionConfigurationId, List<String> astronautIds,
                            List<String> missionControlOfficerIds, List<String> capcomAstronautIds,
                            List<String> rocketIds) {

    /**
     * Validates and copies the request.
     *
     * @throws NullPointerException if any field is {@code null}
     */
    public LaunchRequest {
        Objects.requireNonNull(missionConfigurationId, "missionConfigurationId must not be null");
        astronautIds = List.copyOf(astronautIds);
        missionControlOfficerIds = List.copyOf(missionControlOfficerIds);
        capcomAstronautIds = List.copyOf(capcomAstronautIds);
        rocketIds = List.copyOf(rocketIds);
    }
}
