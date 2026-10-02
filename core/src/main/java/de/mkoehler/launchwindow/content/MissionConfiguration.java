package de.mkoehler.launchwindow.content;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * <p>One way of flying a {@link Program} - the third level of the
 * four-level containment hierarchy (see {@code design.md} &sect;2.3(c)).
 * Holds its own step-by-step resolution content - {@link #seasons()} - but
 * nothing about prestige or goals granted, nor the "extended duration"
 * variant's own higher-risk/higher-reward shape (see {@code baspm.md}
 * &sect;10.1); see {@code design.md} &sect;2.5(e) and &sect;2.6
 * (prestige/goals) for what is still deferred. Adding those fields later is
 * not a migration concern for this type, since it isn't persisted anywhere
 * yet (see {@link ContentCatalog}'s own class Javadoc).</p>
 *
 * <p><b>Declares the exact crew/controller headcount
 * {@link de.mkoehler.launchwindow.rules.MissionLaunch} requires</b> (user
 * decision, {@code design.md} &sect;2.5(f), 2026-10-02) - a mission states
 * how many astronauts/controllers it needs, and a launch must supply
 * exactly that many, not "at least one" or "any number." Which skills they
 * need is still expressed by each {@link MissionStep}'s own
 * {@code crewInvolvements}/{@code controllerInvolvements}, unchanged -
 * this is a headcount requirement layered on top of that, not a
 * replacement for it, and does not assign a specific seat to a specific
 * skill.</p>
 *
 * <p>A content-definition type: shared, read-only game data loaded from a
 * content pack (not yet implemented - see {@code design.md} &sect;5's M3),
 * never per-player state, and never sent over the network.</p>
 *
 * @param id        a stable identifier, unique within a {@link ContentCatalog}
 * @param programId the {@link Program#id()} this configuration belongs to
 * @param seasons   this configuration's ordered step batches, one per
 *                  season it runs - see {@code baspm.md} &sect;8.1's
 *                  one-file-per-season shape; never empty.
 *                  {@link #durationInSeasons()} is {@code seasons.size()}
 * @param requiredAstronautCount the exact number of astronauts a launch of
 *                  this configuration must supply; must be positive if, and
 *                  only if, some step of some season has a non-empty
 *                  {@code crewInvolvements} - {@code 0} for an uncrewed
 *                  mission
 * @param requiredMissionControlOfficerCount the exact number of Mission
 *                  Control officers a launch of this configuration must
 *                  supply; must be positive if, and only if, some step of
 *                  some season has a non-empty {@code controllerInvolvements}
 * @author Mario Koehler
 */
public record MissionConfiguration(String id, String programId, List<MissionSeason> seasons,
                                    int requiredAstronautCount, int requiredMissionControlOfficerCount) {

    /**
     * Validates every field (including that both required-count fields
     * agree with whether {@code seasons} actually involves any crew/
     * controllers at all) and defensively copies {@code seasons} into an
     * unmodifiable view.
     *
     * @throws NullPointerException     if {@code id}/{@code programId}/
     *                                   {@code seasons} is {@code null}
     * @throws IllegalArgumentException if {@code id}/{@code programId} is
     *                                   blank, {@code seasons} is empty,
     *                                   either required-count field is
     *                                   negative, or either disagrees with
     *                                   whether {@code seasons} actually
     *                                   involves any crew/controllers
     */
    public MissionConfiguration {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(programId, "programId must not be null");
        Objects.requireNonNull(seasons, "seasons must not be null");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (programId.isBlank()) {
            throw new IllegalArgumentException("programId must not be blank");
        }
        if (seasons.isEmpty()) {
            throw new IllegalArgumentException("seasons must not be empty");
        }
        if (requiredAstronautCount < 0) {
            throw new IllegalArgumentException("requiredAstronautCount must not be negative: " + requiredAstronautCount);
        }
        if (requiredMissionControlOfficerCount < 0) {
            throw new IllegalArgumentException(
                "requiredMissionControlOfficerCount must not be negative: " + requiredMissionControlOfficerCount);
        }
        seasons = List.copyOf(seasons);

        boolean anyCrewInvolvement = anyStepMatches(seasons, step -> !step.crewInvolvements().isEmpty());
        if ((requiredAstronautCount > 0) != anyCrewInvolvement) {
            throw new IllegalArgumentException(
                "requiredAstronautCount " + requiredAstronautCount + " disagrees with whether any step has crew involvement");
        }
        boolean anyControllerInvolvement = anyStepMatches(seasons, step -> !step.controllerInvolvements().isEmpty());
        if ((requiredMissionControlOfficerCount > 0) != anyControllerInvolvement) {
            throw new IllegalArgumentException("requiredMissionControlOfficerCount "
                + requiredMissionControlOfficerCount + " disagrees with whether any step has controller involvement");
        }
    }

    /**
     * Whether any step of any given season matches the given predicate.
     *
     * @param seasons   the seasons to scan
     * @param predicate the per-step condition to test
     * @return {@code true} if any step matches
     */
    private static boolean anyStepMatches(List<MissionSeason> seasons, Predicate<MissionStep> predicate) {
        for (MissionSeason season : seasons) {
            for (MissionStep step : season.steps()) {
                if (predicate.test(step)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * How many seasons flying this configuration takes - derived from
     * {@link #seasons()} rather than stored separately, so the two can
     * never disagree.
     *
     * @return {@code seasons().size()}
     */
    public int durationInSeasons() {
        return seasons.size();
    }
}
