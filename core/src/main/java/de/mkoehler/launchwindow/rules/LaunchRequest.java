package de.mkoehler.launchwindow.rules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * <p>Everything a player chooses when launching a mission: the configuration, one
 * person per staffing slot, and one rocket per launch. See
 * {@link MissionLaunch#launch(de.mkoehler.launchwindow.model.PlayerSnapshot, LaunchRequest,
 * de.mkoehler.launchwindow.content.ContentCatalog)} and {@code baspm.md} &sect;10.11.</p>
 *
 * @param missionConfigurationId the configuration to fly
 * @param crewSlotIds            one astronaut id per crew slot, in slot order;
 *                               {@code null} for a slot no step uses. A shorter list
 *                               leaves the remaining slots empty
 * @param controlSlotIds         one id per control slot, in slot order - a flight
 *                               controller, or an astronaut for a ground astronaut slot
 *                               such as CAPCOM; {@code null} for an unused slot
 * @param rocketIds              the rocket for each launch, in launch order
 * @author Mario Koehler
 */
public record LaunchRequest(String missionConfigurationId, List<String> crewSlotIds, List<String> controlSlotIds,
                            List<String> rocketIds) {

    /**
     * Validates and copies the request. The slot lists may hold {@code null}s.
     *
     * @throws NullPointerException if a field is {@code null}
     */
    public LaunchRequest {
        Objects.requireNonNull(missionConfigurationId, "missionConfigurationId must not be null");
        crewSlotIds = Collections.unmodifiableList(new ArrayList<>(crewSlotIds));
        controlSlotIds = Collections.unmodifiableList(new ArrayList<>(controlSlotIds));
        rocketIds = List.copyOf(rocketIds);
    }

    /**
     * Compatibility form for fixtures built with
     * {@link de.mkoehler.launchwindow.content.MissionConfiguration}'s generic slots:
     * the crew in slot order, then the controllers followed by the CAPCOM astronauts,
     * matching that layout's order.
     *
     * @param missionConfigurationId the configuration to fly
     * @param astronautIds           the crew, one per crew slot
     * @param controllerIds          the flight controllers, one per controller slot
     * @param capcomIds              the CAPCOM astronauts, one per CAPCOM slot
     * @param rocketIds              the rocket for each launch
     */
    public LaunchRequest(String missionConfigurationId, List<String> astronautIds, List<String> controllerIds,
            List<String> capcomIds, List<String> rocketIds) {
        this(missionConfigurationId, astronautIds, concat(controllerIds, capcomIds), rocketIds);
    }

    private static List<String> concat(List<String> first, List<String> second) {
        List<String> all = new ArrayList<>(first);
        all.addAll(second);
        return all;
    }
}
