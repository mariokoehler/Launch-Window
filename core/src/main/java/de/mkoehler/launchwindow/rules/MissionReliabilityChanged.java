package de.mkoehler.launchwindow.rules;

/**
 * <p>Reports that one mission component's reliability changed as a result
 * of a season's flight, during a {@link MissionProgress#advanceSeason}
 * pass. See {@code design.md} &sect;2.5(g).</p>
 *
 * @param missionInstanceId the {@link de.mkoehler.launchwindow.model.InProgressMission#getId()}
 *                          whose season caused this change
 * @param componentId       the {@link de.mkoehler.launchwindow.content.MissionComponent#id()}
 *                          that changed
 * @param delta             the signed change applied to the component's
 *                          current reliability this season - positive for
 *                          a gain, negative for a loss
 * @author Mario Koehler
 */
public record MissionReliabilityChanged(
    String missionInstanceId,
    String componentId,
    double delta
) implements ResolutionEvent {
}
