package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.model.InProgressMission;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.Personnel;
import de.mkoehler.launchwindow.model.PlayerSnapshot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * What one player's turn did ({@code ui-spec.md} &sect;4.4 and &sect;4.7.3, gap G2): the season
 * pass's events, plus the few numbers they don't carry, taken from the state just before the pass
 * (today's actions applied) and just after. Facts, not wording: the client turns them into the turn
 * report and the mission replay.
 *
 * <p>Saved per player per turn, so this is a save format: only add fields, with values old reports
 * can do without, and bump {@link #VERSION} when the meaning of a field changes.</p>
 *
 * @param version        the format's version, {@link #VERSION} when written
 * @param gameTurn       the turn resolved
 * @param prestigeBefore prestige before the season pass
 * @param prestigeAfter  prestige after it
 * @param fundsBefore    funds before the season pass
 * @param fundsAfter     funds after it
 * @param components     every component whose reliability changed, before and after
 * @param missions       every mission that flew a season, in launch order
 * @param events         the season pass's events, in the order they happened
 * @author Mario Koehler
 */
public record TurnReport(int version, long gameTurn, long prestigeBefore, long prestigeAfter, long fundsBefore,
                         long fundsAfter, List<ComponentChange> components, List<MissionFlown> missions,
                         List<ResolutionEvent> events, String newsStoryId) {

    /** The current format. */
    public static final int VERSION = 1;

    /**
     * Copies the lists.
     */
    public TurnReport {
        components = components == null ? List.of() : List.copyOf(components);
        missions = missions == null ? List.of() : List.copyOf(missions);
        events = events == null ? List.of() : List.copyOf(events);
    }

    /**
     * A report without a space-race story, as every report was before the stories existed.
     *
     * @param version        the format version
     * @param gameTurn       the turn resolved
     * @param prestigeBefore prestige before the pass
     * @param prestigeAfter  prestige after it
     * @param fundsBefore    funds before the pass
     * @param fundsAfter     funds after it
     * @param components     the components' reliability changes
     * @param missions       the missions flown
     * @param events         the pass's events
     */
    public TurnReport(int version, long gameTurn, long prestigeBefore, long prestigeAfter, long fundsBefore,
                      long fundsAfter, List<ComponentChange> components, List<MissionFlown> missions,
                      List<ResolutionEvent> events) {
        this(version, gameTurn, prestigeBefore, prestigeAfter, fundsBefore, fundsAfter, components, missions, events,
            null);
    }

    /**
     * Returns this report with a real space-race story for its news ({@code design.md} &sect;1.1).
     *
     * @param storyId the story's id from {@link de.mkoehler.launchwindow.content.SpaceRaceNews}, or {@code null}
     * @return the report with the story
     */
    public TurnReport withNewsStory(String storyId) {
        return new TurnReport(version, gameTurn, prestigeBefore, prestigeAfter, fundsBefore, fundsAfter, components,
            missions, events, storyId);
    }

    /**
     * A component's reliability across the turn: R&amp;D and flights together.
     *
     * @param componentId the component
     * @param before      its reliability before the season pass (0 if it wasn't owned)
     * @param after       its reliability after it
     */
    public record ComponentChange(String componentId, double before, double after) {
    }

    /**
     * A mission that flew a season this turn.
     *
     * @param missionInstanceId      the mission
     * @param missionConfigurationId its configuration
     * @param seasonIndex            the season it flew, from 0
     * @param seasonsTotal           how many seasons it has
     * @param ended                  whether it's over now (completed, failed, or lost)
     * @param crewNames              who flew aboard, by name
     * @param commanderPortraitId    the first of them's portrait, or {@code null}
     */
    public record MissionFlown(String missionInstanceId, String missionConfigurationId, int seasonIndex,
                               int seasonsTotal, boolean ended, List<String> crewNames, String commanderPortraitId) {

        /**
         * Copies the crew list.
         */
        public MissionFlown {
            crewNames = crewNames == null ? List.of() : List.copyOf(crewNames);
        }

        /**
         * A flight with no portrait to go by.
         *
         * @param missionInstanceId      the mission
         * @param missionConfigurationId its configuration
         * @param seasonIndex            the season it flew, from 0
         * @param seasonsTotal           how many seasons it has
         * @param ended                  whether it's over now
         * @param crewNames              who flew aboard, by name
         */
        public MissionFlown(String missionInstanceId, String missionConfigurationId, int seasonIndex,
                            int seasonsTotal, boolean ended, List<String> crewNames) {
            this(missionInstanceId, missionConfigurationId, seasonIndex, seasonsTotal, ended, crewNames, null);
        }
    }

    /**
     * Builds a player's report from the season pass.
     *
     * @param before   the state just before the pass, with today's actions applied
     * @param after    the state just after it
     * @param events   the pass's events
     * @param gameTurn the turn resolved
     * @param catalog  the content catalog
     * @return the report
     */
    public static TurnReport build(PlayerSnapshot before, PlayerSnapshot after, List<ResolutionEvent> events,
                                   long gameTurn, ContentCatalog catalog) {
        Objects.requireNonNull(before, "before must not be null");
        Objects.requireNonNull(after, "after must not be null");
        Objects.requireNonNull(events, "events must not be null");
        Objects.requireNonNull(catalog, "catalog must not be null");
        Set<String> componentIds = new LinkedHashSet<>(before.getMissionComponents().keySet());
        componentIds.addAll(after.getMissionComponents().keySet());
        List<ComponentChange> components = new ArrayList<>();
        for (String id : componentIds) {
            OwnedMissionComponent was = before.getMissionComponents().get(id);
            OwnedMissionComponent now = after.getMissionComponents().get(id);
            double from = was == null ? 0 : was.getCurrentReliability();
            double to = now == null ? from : now.getCurrentReliability();
            if (Math.abs(to - from) > 1e-9) {
                components.add(new ComponentChange(id, from, to));
            }
        }
        Map<String, Integer> flownSeasons = new LinkedHashMap<>();
        for (ResolutionEvent event : events) {
            if (event instanceof MissionStepResolved step) {
                flownSeasons.putIfAbsent(step.missionInstanceId(), step.seasonIndex());
            }
        }
        List<MissionFlown> missions = new ArrayList<>();
        for (Map.Entry<String, Integer> flown : flownSeasons.entrySet()) {
            InProgressMission mission = before.getInProgressMissions().get(flown.getKey());
            if (mission == null) {
                continue;
            }
            MissionConfiguration configuration = catalog.missionConfiguration(mission.getMissionConfigurationId())
                .orElse(null);
            List<Personnel> aboard = mission.crewIds().stream().map(id -> personOf(before, id))
                .filter(Objects::nonNull).toList();
            List<String> crew = aboard.stream().map(Personnel::getName).toList();
            missions.add(new MissionFlown(mission.getId(), mission.getMissionConfigurationId(), flown.getValue(),
                configuration == null ? 0 : configuration.seasons().size(),
                !after.getInProgressMissions().containsKey(mission.getId()), crew,
                aboard.isEmpty() ? null : aboard.get(0).getPortraitId()));
        }
        return new TurnReport(VERSION, gameTurn, before.getPrestige(), after.getPrestige(), before.getFunds(),
            after.getFunds(), components, missions, events);
    }

    private static Personnel personOf(PlayerSnapshot state, String id) {
        return Stream.of(state.getAstronauts(), state.getMissionControlOfficers(), state.getSetSpecialists())
            .flatMap(List::stream)
            .filter(person -> person.getId().equals(id))
            .map(Personnel.class::cast)
            .findFirst()
            .orElse(null);
    }
}
