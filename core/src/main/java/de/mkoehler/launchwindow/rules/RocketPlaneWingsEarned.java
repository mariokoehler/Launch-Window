package de.mkoehler.launchwindow.rules;

/**
 * Reports that an astronaut earned their rocket-plane wings by flying a rocket plane home
 * ({@link RocketPlaneWings}, {@code design.md} &sect;2.2(c)).
 *
 * @param missionInstanceId      the flight's {@link de.mkoehler.launchwindow.model.InProgressMission#getId()}
 * @param missionConfigurationId the configuration flown
 * @param personnelId            the astronaut's stable id
 * @param name                   the astronaut's display name
 * @author Mario Koehler
 */
public record RocketPlaneWingsEarned(
    String missionInstanceId,
    String missionConfigurationId,
    String personnelId,
    String name
) implements ResolutionEvent {
}
