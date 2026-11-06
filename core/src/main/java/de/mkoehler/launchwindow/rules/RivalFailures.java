package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.content.MissionSeason;
import de.mkoehler.launchwindow.content.MissionStep;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.RivalFailure;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Finds the failed attempts in an agency's turn report, for its rivals' intelligence services
 * ({@code design.md} &sect;2.9, "Failed attempts"): every mission that ended on a failing step
 * ({@link MissionStepOutcome#FAILURE} or {@link MissionStepOutcome#CATASTROPHIC}), with everything a
 * level 3 service would learn. {@link IntelligenceBoard} strips each to the spy's level.
 *
 * @author Mario Koehler
 */
public final class RivalFailures {

    /** Step kinds (the id without its number) that come before liftoff. */
    private static final List<String> PAD_ENDINGS = List.of("launch-preparations", "countdown");
    /**
     * An air drop's carrier steps, which count as the launch though no rocket is involved; its
     * pre-launch checks happen under the carrier in the air, not on a pad.
     */
    private static final Set<String> AIR_DROP = Set.of("takeoff", "pre-launch", "launch-from-drop-plane");
    /** Words that mark a step on the way home. */
    private static final List<String> RETURN_WORDS = List.of("reentry", "retrofire", "de-orbit-burn", "splashdown",
        "recovery", "earth-approach", "trans-earth", "moon-earth", "modules-separation");

    private RivalFailures() {
    }

    /**
     * Returns the failed attempts in a turn report, one per mission, in the report's order.
     *
     * @param report  the agency's turn report
     * @param catalog the content catalog
     * @param side    the agency's archetype
     * @return the failures, in full detail
     */
    public static List<RivalFailure> of(TurnReport report, ContentCatalog catalog, Archetype side) {
        Objects.requireNonNull(report, "report must not be null");
        Objects.requireNonNull(catalog, "catalog must not be null");
        Objects.requireNonNull(side, "side must not be null");
        Set<String> crewLost = new HashSet<>();
        for (ResolutionEvent event : report.events()) {
            if (event instanceof CrewedFlightsGrounded grounded) {
                crewLost.add(grounded.missionInstanceId());
            }
        }
        Map<String, RivalFailure> failures = new LinkedHashMap<>();
        for (ResolutionEvent event : report.events()) {
            if (!(event instanceof MissionStepResolved step) || failures.containsKey(step.missionInstanceId())
                    || step.outcome() != MissionStepOutcome.FAILURE && step.outcome() != MissionStepOutcome.CATASTROPHIC) {
                continue;
            }
            MissionConfiguration configuration = catalog.missionConfiguration(step.missionConfigurationId())
                .orElse(null);
            if (configuration == null) {
                continue;
            }
            failures.put(step.missionInstanceId(), new RivalFailure(Math.toIntExact(report.gameTurn()),
                configuration.programId(), configuration.id(), step.stepId(), suspect(step),
                phase(configuration, step.seasonIndex(), step.stepId()), crewLost.contains(step.missionInstanceId()),
                side));
        }
        return List.copyOf(failures.values());
    }

    /**
     * Returns the component with the largest share of a step's risk: its involvement times its
     * unreliability, as flown. The first wins a tie.
     *
     * @param step the failing step
     * @return the component, or {@code null} if the step names none
     */
    static String suspect(MissionStepResolved step) {
        String suspect = null;
        double worst = -1;
        for (MissionStepResolved.ComponentFlown component : step.components()) {
            double risk = component.percent() * (100 - component.reliability());
            if (risk > worst) {
                worst = risk;
                suspect = component.componentId();
            }
        }
        return suspect;
    }

    /**
     * Returns roughly where a step lies in its mission. A pad step (preparations, countdown) is
     * {@link RivalFailure.Phase#PAD}, and it, or the start of the first season, opens a launch: the
     * steps after it that involve a rocket slot (or an air drop's carrier) are
     * {@link RivalFailure.Phase#LAUNCH}, until the first that doesn't. After that, a step on the way
     * home (retrofire, reentry and the like) is {@link RivalFailure.Phase#RETURN}, and so is every step
     * after it in the season (the descent and landing that follow); anything else is
     * {@link RivalFailure.Phase#FLIGHT}.
     *
     * @param configuration the configuration flown
     * @param seasonIndex   the season the step flew in, from 0
     * @param stepId        the step
     * @return the phase
     */
    static RivalFailure.Phase phase(MissionConfiguration configuration, int seasonIndex, String stepId) {
        List<MissionSeason> seasons = configuration.seasons();
        if (seasonIndex >= 0 && seasonIndex < seasons.size()) {
            boolean launching = seasonIndex == 0;
            boolean returning = false;
            for (MissionStep step : seasons.get(seasonIndex).steps()) {
                String kind = kind(step.id());
                RivalFailure.Phase phase;
                if (PAD_ENDINGS.stream().anyMatch(kind::endsWith)) {
                    launching = true;
                    phase = RivalFailure.Phase.PAD;
                } else if (launching && (AIR_DROP.contains(kind) || step.componentInvolvements().stream()
                        .anyMatch(c -> c.componentId().startsWith(MissionConfiguration.ROCKET_SLOT_PREFIX)))) {
                    phase = RivalFailure.Phase.LAUNCH;
                } else {
                    launching = false;
                    returning |= home(kind);
                    phase = returning ? RivalFailure.Phase.RETURN : RivalFailure.Phase.FLIGHT;
                }
                if (step.id().equals(stepId)) {
                    return phase;
                }
            }
        }
        String kind = kind(stepId);
        return PAD_ENDINGS.stream().anyMatch(kind::endsWith) ? RivalFailure.Phase.PAD
            : home(kind) ? RivalFailure.Phase.RETURN : RivalFailure.Phase.FLIGHT;
    }

    private static boolean home(String kind) {
        return kind.equals("tei") || RETURN_WORDS.stream().anyMatch(kind::contains);
    }

    /** A step's id without its number: "03-launch" is "launch". */
    private static String kind(String stepId) {
        return stepId == null ? "" : stepId.replaceFirst("^\\d+-", "");
    }

    /**
     * Returns the failed attempts of several agencies, ready for {@link IntelligenceBoard#settle}.
     *
     * @param reports each agency's last turn report, by rival key; a missing report counts as none
     * @param sides   each agency's archetype, by rival key
     * @param catalog the content catalog
     * @return the failures by rival key; agencies with none are left out
     */
    public static Map<String, List<RivalFailure>> byAgency(Map<String, TurnReport> reports,
            Map<String, Archetype> sides, ContentCatalog catalog) {
        Map<String, List<RivalFailure>> failures = new LinkedHashMap<>();
        reports.forEach((key, report) -> {
            Archetype side = sides.get(key);
            if (report != null && side != null) {
                List<RivalFailure> found = new ArrayList<>(of(report, catalog, side));
                if (!found.isEmpty()) {
                    failures.put(key, List.copyOf(found));
                }
            }
        });
        return failures;
    }
}
