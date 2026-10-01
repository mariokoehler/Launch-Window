package de.mkoehler.launchwindow.rules;

import java.util.List;
import java.util.Objects;

/**
 * <p>Reports that one mission component's reliability reached its
 * {@code maxRdReliability} ceiling (see
 * {@link de.mkoehler.launchwindow.content.MissionComponent#maxRdReliability()})
 * at the end of a {@link ResearchProgress#advanceSeason} pass, automatically
 * freeing every {@code SET} employee who was occupying one of its four
 * R&amp;D slots back to {@link de.mkoehler.launchwindow.model.AssignmentState#IDLE} -
 * re-deriving {@code baspm.md} &sect;10.8's own
 * {@code ProcessAndRetrieveAllMissionComponentsThatReachedMaxRDReliabilityAtSomePointDuringTheLastSeason}/
 * {@code UnassignAllSETPersonnelFromMissionComponent} mechanic: once R&amp;D
 * alone can't improve a component any further, there's nothing left for its
 * researchers to do there, so BASPM (and this re-derivation) releases them
 * rather than leaving them parked doing nothing.</p>
 *
 * <p><b>Unconfirmed timing nuance:</b> BASPM triggers this from its own
 * interseason-overlay news-item generation, a UI-adjacent call site, not
 * from inside the R&amp;D resolver itself - functionally the same season,
 * but not re-verified to be the exact same resolution step. This
 * re-derivation folds it into {@link ResearchProgress#advanceSeason}
 * directly, which should be behaviorally equivalent for a single-player-
 * snapshot-at-a-time async turn resolution, but is a simplification worth
 * flagging. <b>The event shape itself (one event per maxed component,
 * listing every released employee's id) is Launch Window's own choice</b> -
 * BASPM's own per-employee {@code SETEmployeeAvailableBackAgain} has no
 * direct Launch Window equivalent yet.</p>
 *
 * @param componentId          the mission component id that reached its
 *                             R&amp;D ceiling this season
 * @param releasedPersonnelIds the stable ids of every {@code SET} employee
 *                             freed back to {@code IDLE} as a result, in
 *                             slot order (0 before 1 before 2 before 3)
 * @author Mario Koehler
 */
public record ResearchComponentMaxed(String componentId, List<String> releasedPersonnelIds)
    implements ResolutionEvent {

    /**
     * Validates both fields and defensively copies
     * {@code releasedPersonnelIds} into an unmodifiable view.
     *
     * @throws NullPointerException if {@code componentId}/
     *                                {@code releasedPersonnelIds} is
     *                                {@code null}
     */
    public ResearchComponentMaxed {
        Objects.requireNonNull(componentId, "componentId must not be null");
        Objects.requireNonNull(releasedPersonnelIds, "releasedPersonnelIds must not be null");
        releasedPersonnelIds = List.copyOf(releasedPersonnelIds);
    }
}
