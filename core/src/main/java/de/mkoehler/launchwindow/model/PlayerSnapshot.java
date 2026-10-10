package de.mkoehler.launchwindow.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * <p>One player's complete resolved game state as of the end of a turn -
 * the aggregate root persisted to disk once per player per turn (see
 * {@code design.md} &sect;3.7).</p>
 *
 * <p>Holds only per-player state, never shared game-wide state (schedule
 * configuration, blackout periods, other players' summaries) - those live in
 * the separate game-meta file per {@code design.md} &sect;3.7. Content
 * definitions (programs, mission components, goals) are referenced by their
 * own identifiers, never embedded here, so a content change never requires a
 * snapshot migration.</p>
 *
 * @author Mario Koehler
 */
public class PlayerSnapshot {

    private String agencyName;
    private String directorName;
    private Archetype archetype;
    private long funds;
    private long prestige;
    private long fundingRatePerSeason;
    private int season;
    private List<Astronaut> astronauts = new ArrayList<>();
    private List<SetSpecialist> setSpecialists = new ArrayList<>();
    private List<MissionControlOfficer> missionControlOfficers = new ArrayList<>();
    private List<Astronaut> astronautCandidates = new ArrayList<>();
    private List<SetSpecialist> setSpecialistCandidates = new ArrayList<>();
    private List<MissionControlOfficer> missionControlCandidates = new ArrayList<>();
    private long nextPersonnelId;
    private List<String> openedProgramIds = new ArrayList<>();
    private Map<String, OwnedMissionComponent> missionComponents = new LinkedHashMap<>();
    private Map<String, InProgressMission> inProgressMissions = new LinkedHashMap<>();
    private long nextMissionInstanceId;
    /** How many news entries a player's log keeps before the oldest drop off - {@code design.md} &sect;2.7. */
    public static final int NEWS_LOG_CAPACITY = 50;

    private Map<String, Integer> successfulFlightCounts = new LinkedHashMap<>();
    private Map<String, Integer> achievedGoals = new LinkedHashMap<>();
    private Set<String> cancelledGoalIds = new LinkedHashSet<>();
    private long prestigeAtLastReview;
    private List<NewsEntry> newsLog = new ArrayList<>();
    private List<MemorialEntry> memorial = new ArrayList<>();
    private List<GoalRecord> goalRecords = new ArrayList<>();
    private int lastFlightSeasonTurn = -1;
    private int lastFailureTurn = -1;
    private double rdResultsMultiplier = 1.0;
    private int rdResultsSeasonsRemaining;
    private double costMultiplier = 1.0;
    private int costMultiplierSeasonsRemaining;
    private int launchCap = -1;
    private int launchesThisWindow;
    private Map<String, Integer> unavailableComponents = new LinkedHashMap<>();
    private List<String> salaryAdjustedGroups = new ArrayList<>();
    private List<String> usedPressStoryIds = new ArrayList<>();
    private List<String> usedEventStoryIds = new ArrayList<>();
    private List<HiringPoolBonus> hiringPoolBonuses = new ArrayList<>();
    private List<Rapport> rapports = new ArrayList<>();
    private Map<Building, Integer> buildingLevels = new LinkedHashMap<>();
    private Map<Building, Integer> buildingUpgradeSeasonsRemaining = new LinkedHashMap<>();
    private IntelligenceState intelligence = new IntelligenceState();
    private Map<String, Integer> retentionMatchPercent = new LinkedHashMap<>();

    /**
     * No-argument constructor required for Jackson deserialization.
     */
    public PlayerSnapshot() {
    }

    /**
     * Creates a new player snapshot with the given identity and economic
     * state, and empty personnel rosters.
     *
     * @param agencyName           the player's chosen agency name
     * @param archetype            the player's chosen agency archetype
     * @param funds                current funds
     * @param prestige             current prestige
     * @param fundingRatePerSeason the recurring per-season funding rate set
     *                             at the player's last budget review
     * @param season               the player's own personal season count,
     *                             decoupled from the game's shared
     *                             wall-clock schedule (see {@code design.md}
     *                             &sect;3.8's late-joiner handling)
     */
    public PlayerSnapshot(String agencyName, Archetype archetype, long funds, long prestige,
                           long fundingRatePerSeason, int season) {
        this.agencyName = Objects.requireNonNull(agencyName, "agencyName must not be null");
        this.archetype = Objects.requireNonNull(archetype, "archetype must not be null");
        this.funds = funds;
        this.prestige = prestige;
        this.fundingRatePerSeason = fundingRatePerSeason;
        this.season = season;
    }

    /**
     * Returns the name of the agency's director, the player's own in-game persona, unique within
     * the game like the agency name (`design.md` &sect;3.8's slot claim), or `null` before it is chosen.
     *
     * @return the director's name
     */
    public String getDirectorName() {
        return directorName;
    }

    /**
     * Sets the name of the agency's director.
     *
     * @param directorName the director's name
     */
    public void setDirectorName(String directorName) {
        this.directorName = directorName;
    }

    /**
     * Returns the player's chosen agency name.
     *
     * @return the agency name
     */
    public String getAgencyName() {
        return agencyName;
    }

    /**
     * Sets the player's chosen agency name.
     *
     * @param agencyName the agency name
     */
    public void setAgencyName(String agencyName) {
        this.agencyName = agencyName;
    }

    /**
     * Returns the player's chosen agency archetype.
     *
     * @return the archetype
     */
    public Archetype getArchetype() {
        return archetype;
    }

    /**
     * Sets the player's chosen agency archetype.
     *
     * @param archetype the archetype
     */
    public void setArchetype(Archetype archetype) {
        this.archetype = archetype;
    }

    /**
     * Returns the player's current funds.
     *
     * @return the funds
     */
    public long getFunds() {
        return funds;
    }

    /**
     * Sets the player's current funds.
     *
     * @param funds the funds
     */
    public void setFunds(long funds) {
        this.funds = funds;
    }

    /**
     * Returns the player's current prestige.
     *
     * @return the prestige
     */
    public long getPrestige() {
        return prestige;
    }

    /**
     * Sets the player's current prestige.
     *
     * @param prestige the prestige
     */
    public void setPrestige(long prestige) {
        this.prestige = prestige;
    }

    /**
     * Returns the recurring per-season funding rate set at the player's
     * last budget review.
     *
     * @return the funding rate per season
     */
    public long getFundingRatePerSeason() {
        return fundingRatePerSeason;
    }

    /**
     * Sets the recurring per-season funding rate.
     *
     * @param fundingRatePerSeason the funding rate per season
     */
    public void setFundingRatePerSeason(long fundingRatePerSeason) {
        this.fundingRatePerSeason = fundingRatePerSeason;
    }

    /**
     * Returns the player's own personal season count, decoupled from the
     * game's shared wall-clock schedule.
     *
     * @return the season count
     */
    public int getSeason() {
        return season;
    }

    /**
     * Sets the player's own personal season count.
     *
     * @param season the season count
     */
    public void setSeason(int season) {
        this.season = season;
    }

    /**
     * Returns the player's astronaut roster.
     *
     * @return the astronaut roster
     */
    public List<Astronaut> getAstronauts() {
        return astronauts;
    }

    /**
     * Replaces the player's astronaut roster.
     *
     * @param astronauts the astronaut roster
     */
    public void setAstronauts(List<Astronaut> astronauts) {
        Objects.requireNonNull(astronauts, "astronauts must not be null");
        this.astronauts = new ArrayList<>(astronauts);
    }

    /**
     * Returns the player's SET (Scientist/Engineer/Technician) roster.
     *
     * @return the SET roster
     */
    public List<SetSpecialist> getSetSpecialists() {
        return setSpecialists;
    }

    /**
     * Replaces the player's SET roster.
     *
     * @param setSpecialists the SET roster
     */
    public void setSetSpecialists(List<SetSpecialist> setSpecialists) {
        Objects.requireNonNull(setSpecialists, "setSpecialists must not be null");
        this.setSpecialists = new ArrayList<>(setSpecialists);
    }

    /**
     * Returns the player's Mission Control roster.
     *
     * @return the Mission Control roster
     */
    public List<MissionControlOfficer> getMissionControlOfficers() {
        return missionControlOfficers;
    }

    /**
     * Replaces the player's Mission Control roster.
     *
     * @param missionControlOfficers the Mission Control roster
     */
    public void setMissionControlOfficers(List<MissionControlOfficer> missionControlOfficers) {
        Objects.requireNonNull(missionControlOfficers, "missionControlOfficers must not be null");
        this.missionControlOfficers = new ArrayList<>(missionControlOfficers);
    }

    /**
     * Returns the player's current astronaut hiring-candidate pool - not
     * part of the real roster until hired (see
     * {@code de.mkoehler.launchwindow.rules.HiringPool}).
     *
     * @return the astronaut candidate pool
     */
    public List<Astronaut> getAstronautCandidates() {
        return astronautCandidates;
    }

    /**
     * Replaces the player's astronaut hiring-candidate pool.
     *
     * @param astronautCandidates the astronaut candidate pool
     */
    public void setAstronautCandidates(List<Astronaut> astronautCandidates) {
        Objects.requireNonNull(astronautCandidates, "astronautCandidates must not be null");
        this.astronautCandidates = new ArrayList<>(astronautCandidates);
    }

    /**
     * Returns the player's current SET hiring-candidate pool.
     *
     * @return the SET candidate pool
     */
    public List<SetSpecialist> getSetSpecialistCandidates() {
        return setSpecialistCandidates;
    }

    /**
     * Replaces the player's SET hiring-candidate pool.
     *
     * @param setSpecialistCandidates the SET candidate pool
     */
    public void setSetSpecialistCandidates(List<SetSpecialist> setSpecialistCandidates) {
        Objects.requireNonNull(setSpecialistCandidates, "setSpecialistCandidates must not be null");
        this.setSpecialistCandidates = new ArrayList<>(setSpecialistCandidates);
    }

    /**
     * Returns the player's current Mission Control hiring-candidate pool.
     *
     * @return the Mission Control candidate pool
     */
    public List<MissionControlOfficer> getMissionControlCandidates() {
        return missionControlCandidates;
    }

    /**
     * Replaces the player's Mission Control hiring-candidate pool.
     *
     * @param missionControlCandidates the Mission Control candidate pool
     */
    public void setMissionControlCandidates(List<MissionControlOfficer> missionControlCandidates) {
        Objects.requireNonNull(missionControlCandidates, "missionControlCandidates must not be null");
        this.missionControlCandidates = new ArrayList<>(missionControlCandidates);
    }

    /**
     * Returns the next id {@link #allocatePersonnelId()} will hand out.
     * Public only because Jackson/Kryo need a bean accessor - callers
     * should use {@link #allocatePersonnelId()}, never read or set this
     * directly.
     *
     * @return the next personnel id counter value
     */
    public long getNextPersonnelId() {
        return nextPersonnelId;
    }

    /**
     * Sets the next id {@link #allocatePersonnelId()} will hand out.
     *
     * @param nextPersonnelId the next personnel id counter value
     */
    public void setNextPersonnelId(long nextPersonnelId) {
        this.nextPersonnelId = nextPersonnelId;
    }

    /**
     * Returns the ids of every program this player has opened (see
     * {@code de.mkoehler.launchwindow.rules.ProgramOpening}).
     *
     * @return the opened program ids
     */
    public List<String> getOpenedProgramIds() {
        return openedProgramIds;
    }

    /**
     * Replaces the set of opened program ids.
     *
     * @param openedProgramIds the opened program ids
     */
    public void setOpenedProgramIds(List<String> openedProgramIds) {
        Objects.requireNonNull(openedProgramIds, "openedProgramIds must not be null");
        this.openedProgramIds = new ArrayList<>(openedProgramIds);
    }

    /**
     * Returns this player's owned mission components, keyed by
     * {@code MissionComponent} id.
     *
     * @return the owned mission components
     */
    public Map<String, OwnedMissionComponent> getMissionComponents() {
        return missionComponents;
    }

    /**
     * Replaces this player's owned mission components.
     *
     * @param missionComponents the owned mission components
     */
    public void setMissionComponents(Map<String, OwnedMissionComponent> missionComponents) {
        Objects.requireNonNull(missionComponents, "missionComponents must not be null");
        this.missionComponents = new LinkedHashMap<>(missionComponents);
    }

    /**
     * Returns this player's in-progress missions, keyed by
     * {@link InProgressMission#getId()}.
     *
     * @return the in-progress missions
     */
    public Map<String, InProgressMission> getInProgressMissions() {
        return inProgressMissions;
    }

    /**
     * Replaces this player's in-progress missions.
     *
     * @param inProgressMissions the in-progress missions
     */
    public void setInProgressMissions(Map<String, InProgressMission> inProgressMissions) {
        Objects.requireNonNull(inProgressMissions, "inProgressMissions must not be null");
        this.inProgressMissions = new LinkedHashMap<>(inProgressMissions);
    }

    /**
     * Returns how many times this player has completed each mission
     * configuration successfully, keyed by configuration id. Only a final,
     * non-failed season counts - see {@code design.md} &sect;2.6(d). Drives
     * the repeat tier a new launch pays at. The live map is returned, so
     * callers can mutate it in place.
     *
     * @return the successful-flight counts, never {@code null}
     */
    public Map<String, Integer> getSuccessfulFlightCounts() {
        return successfulFlightCounts;
    }

    /**
     * Replaces the successful-flight counts. Public for Jackson/Kryo, and
     * copied defensively.
     *
     * @param successfulFlightCounts the counts to set
     * @throws NullPointerException if {@code successfulFlightCounts} is {@code null}
     */
    public void setSuccessfulFlightCounts(Map<String, Integer> successfulFlightCounts) {
        Objects.requireNonNull(successfulFlightCounts, "successfulFlightCounts must not be null");
        this.successfulFlightCounts = new LinkedHashMap<>(successfulFlightCounts);
    }

    /**
     * Returns the R&amp;D results multiplier in force this season: 1.0 unless a random
     * event set it, and it applies to skill and reliability gains alike - {@code design.md} &sect;2.7.
     *
     * @return the multiplier, positive
     */
    public double getRdResultsMultiplier() {
        return rdResultsMultiplier;
    }

    /**
     * Returns how many more seasons the R&amp;D multiplier lasts, counting this one.
     *
     * @return the seasons remaining, zero when the multiplier is at its default
     */
    public int getRdResultsSeasonsRemaining() {
        return rdResultsSeasonsRemaining;
    }

    /**
     * Sets the R&amp;D results multiplier and its duration, replacing any earlier one.
     *
     * @param multiplier the factor applied to each R&amp;D gain, positive
     * @param seasons    how many seasons it lasts, zero or more
     * @throws IllegalArgumentException if {@code multiplier} isn't positive or {@code seasons} is negative
     */
    public void setRdResults(double multiplier, int seasons) {
        if (multiplier <= 0 || seasons < 0) {
            throw new IllegalArgumentException("invalid R&D multiplier or duration");
        }
        this.rdResultsMultiplier = multiplier;
        this.rdResultsSeasonsRemaining = seasons;
    }

    /**
     * Sets the R&amp;D multiplier alone. Public for Jackson, which persists the two
     * R&amp;D fields separately; gameplay code uses {@link #setRdResults}.
     *
     * @param rdResultsMultiplier the factor to store
     */
    public void setRdResultsMultiplier(double rdResultsMultiplier) {
        this.rdResultsMultiplier = rdResultsMultiplier;
    }

    /**
     * Sets the R&amp;D remaining-seasons count alone. Public for Jackson; gameplay code uses {@link #setRdResults}.
     *
     * @param rdResultsSeasonsRemaining the count to store
     */
    public void setRdResultsSeasonsRemaining(int rdResultsSeasonsRemaining) {
        this.rdResultsSeasonsRemaining = rdResultsSeasonsRemaining;
    }

    /**
     * Sets the running-cost multiplier alone. Public for Jackson, which persists the two cost fields separately; gameplay code uses the two-argument overload.
     *
     * @param costMultiplier the factor to store
     */
    public void setCostMultiplier(double costMultiplier) {
        this.costMultiplier = costMultiplier;
    }

    /**
     * Sets the running-cost remaining-seasons count alone. Public for Jackson; gameplay code uses the two-argument overload.
     *
     * @param costMultiplierSeasonsRemaining the count to store
     */
    public void setCostMultiplierSeasonsRemaining(int costMultiplierSeasonsRemaining) {
        this.costMultiplierSeasonsRemaining = costMultiplierSeasonsRemaining;
    }

    /**
     * Returns the running-cost multiplier in force this season: 1.0 unless a random
     * event set it. It scales salaries and maintenance alike, so debt conversions
     * cap against the costs actually paid ({@code design.md} &sect;2.7).
     *
     * @return the multiplier, positive
     */
    public double getCostMultiplier() {
        return costMultiplier;
    }

    /**
     * Returns how many more seasons the running-cost multiplier lasts.
     *
     * @return the seasons remaining, zero when the multiplier is at its default
     */
    public int getCostMultiplierSeasonsRemaining() {
        return costMultiplierSeasonsRemaining;
    }

    /**
     * Sets the running-cost multiplier and its duration, replacing any earlier one.
     *
     * @param multiplier the factor applied to running costs, positive
     * @param seasons    how many seasons it lasts, zero or more
     * @throws IllegalArgumentException if {@code multiplier} isn't positive or {@code seasons} is negative
     */
    public void setCostMultiplier(double multiplier, int seasons) {
        if (multiplier <= 0 || seasons < 0) {
            throw new IllegalArgumentException("invalid cost multiplier or duration");
        }
        this.costMultiplier = multiplier;
        this.costMultiplierSeasonsRemaining = seasons;
    }

    /**
     * Returns the cap on launches in the next launch window, or {@code -1} for no cap.
     *
     * @return the cap, or {@code -1}
     */
    public int getLaunchCap() {
        return launchCap;
    }

    /**
     * Sets the cap on launches in the next launch window.
     *
     * @param launchCap the cap, zero or more, or {@code -1} for none
     * @throws IllegalArgumentException if {@code launchCap} is below {@code -1}
     */
    public void setLaunchCap(int launchCap) {
        if (launchCap < -1) {
            throw new IllegalArgumentException("launchCap must be -1 or more: " + launchCap);
        }
        this.launchCap = launchCap;
    }

    /**
     * Returns how many missions have launched in the current launch window.
     *
     * @return the count, zero or more
     */
    public int getLaunchesThisWindow() {
        return launchesThisWindow;
    }

    /**
     * Sets the number of missions launched in the current window.
     *
     * @param launchesThisWindow the count, zero or more
     */
    public void setLaunchesThisWindow(int launchesThisWindow) {
        this.launchesThisWindow = launchesThisWindow;
    }

    /**
     * Returns the personnel groups whose salaries were adjusted this season, by group
     * name: a department may be adjusted once per season ({@code design.md} &sect;2.2(c),
     * user decision 2026-10-06). Cleared by every season pass. The live list is returned.
     *
     * @return the group names, never {@code null}
     */
    public List<String> getSalaryAdjustedGroups() {
        return salaryAdjustedGroups;
    }

    /**
     * Sets the personnel groups adjusted this season, by group name.
     *
     * @param salaryAdjustedGroups the group names
     * @throws NullPointerException if {@code salaryAdjustedGroups} is {@code null}
     */
    public void setSalaryAdjustedGroups(List<String> salaryAdjustedGroups) {
        Objects.requireNonNull(salaryAdjustedGroups, "salaryAdjustedGroups must not be null");
        this.salaryAdjustedGroups = new ArrayList<>(salaryAdjustedGroups);
    }

    /**
     * Returns the press stories this player has already been shown, by id, so no story repeats
     * until every story of its tone has run once ({@code design.md} &sect;2.2(c), user decision
     * 2026-10-06). The live list is returned.
     *
     * @return the story ids, oldest first, never {@code null}
     */
    public List<String> getUsedPressStoryIds() {
        return usedPressStoryIds;
    }

    /**
     * Sets the press stories this player has already been shown, by id.
     *
     * @param usedPressStoryIds the story ids
     * @throws NullPointerException if {@code usedPressStoryIds} is {@code null}
     */
    public void setUsedPressStoryIds(List<String> usedPressStoryIds) {
        Objects.requireNonNull(usedPressStoryIds, "usedPressStoryIds must not be null");
        this.usedPressStoryIds = new ArrayList<>(usedPressStoryIds);
    }

    /**
     * Returns the random-event stories this player has already been shown, by id, so no story
     * repeats until every story of its event has run once (the user's request, 2026-10-10). The
     * live list is returned.
     *
     * @return the story ids, oldest first, never {@code null}
     */
    public List<String> getUsedEventStoryIds() {
        return usedEventStoryIds;
    }

    /**
     * Sets the random-event stories this player has already been shown, by id.
     *
     * @param usedEventStoryIds the story ids
     * @throws NullPointerException if {@code usedEventStoryIds} is {@code null}
     */
    public void setUsedEventStoryIds(List<String> usedEventStoryIds) {
        Objects.requireNonNull(usedEventStoryIds, "usedEventStoryIds must not be null");
        this.usedEventStoryIds = new ArrayList<>(usedEventStoryIds);
    }

    /**
     * Returns the press stories' boosts to the hiring pools still running ({@code design.md}
     * &sect;2.2(c)). The live list is returned.
     *
     * @return the bonuses, never {@code null}
     */
    public List<HiringPoolBonus> getHiringPoolBonuses() {
        return hiringPoolBonuses;
    }

    /**
     * Sets the boosts to the hiring pools still running.
     *
     * @param hiringPoolBonuses the bonuses
     * @throws NullPointerException if {@code hiringPoolBonuses} is {@code null}
     */
    public void setHiringPoolBonuses(List<HiringPoolBonus> hiringPoolBonuses) {
        Objects.requireNonNull(hiringPoolBonuses, "hiringPoolBonuses must not be null");
        this.hiringPoolBonuses = new ArrayList<>(hiringPoolBonuses);
    }

    /**
     * Returns how every pair of employees who have shared a team get on ({@code design.md}
     * &sect;2.2(d)). The live list is returned; a pair leaves it when either of them leaves the
     * agency.
     *
     * @return the pairs, never {@code null}
     */
    public List<Rapport> getRapports() {
        return rapports;
    }

    /**
     * Sets the pairs' rapport.
     *
     * @param rapports the pairs
     */
    public void setRapports(List<Rapport> rapports) {
        this.rapports = rapports == null ? new ArrayList<>() : new ArrayList<>(rapports);
    }

    /**
     * Returns the mission components currently unavailable, mapped to the launch
     * windows each stays unavailable for. The live map is returned.
     *
     * @return the unavailable components, never {@code null}
     */
    public Map<String, Integer> getUnavailableComponents() {
        return unavailableComponents;
    }

    /**
     * Replaces the unavailable components. Public for Jackson/Kryo, and copied defensively.
     *
     * @param unavailableComponents the component id to remaining-windows map
     * @throws NullPointerException if {@code unavailableComponents} is {@code null}
     */
    public void setUnavailableComponents(Map<String, Integer> unavailableComponents) {
        Objects.requireNonNull(unavailableComponents, "unavailableComponents must not be null");
        this.unavailableComponents = new LinkedHashMap<>(unavailableComponents);
    }

    /**
     * Returns a building's current level. Every building starts at level 1, so a
     * building with no recorded level is at level 1 - {@code design.md} &sect;5 (M3).
     *
     * @param building the building
     * @return its level, 1 or more
     */
    public int buildingLevel(Building building) {
        return buildingLevels.getOrDefault(Objects.requireNonNull(building, "building must not be null"), 1);
    }

    /**
     * Returns the recorded building levels. Buildings missing from the map are at
     * level 1; prefer {@link #buildingLevel(Building)}. The live map is returned.
     *
     * @return the levels, never {@code null}
     */
    public Map<Building, Integer> getBuildingLevels() {
        return buildingLevels;
    }

    /**
     * Replaces the building levels. Public for Jackson/Kryo, and copied defensively.
     *
     * @param buildingLevels the building to level map
     * @throws NullPointerException if {@code buildingLevels} is {@code null}
     */
    public void setBuildingLevels(Map<Building, Integer> buildingLevels) {
        Objects.requireNonNull(buildingLevels, "buildingLevels must not be null");
        this.buildingLevels = new LinkedHashMap<>(buildingLevels);
    }

    /**
     * Returns the buildings currently being upgraded, mapped to the seasons left
     * until each reaches its next level. The live map is returned.
     *
     * @return the upgrades in progress, never {@code null}
     */
    public Map<Building, Integer> getBuildingUpgradeSeasonsRemaining() {
        return buildingUpgradeSeasonsRemaining;
    }

    /**
     * Replaces the upgrades in progress. Public for Jackson/Kryo, and copied defensively.
     *
     * @param buildingUpgradeSeasonsRemaining the building to seasons-left map
     * @throws NullPointerException if {@code buildingUpgradeSeasonsRemaining} is {@code null}
     */
    public void setBuildingUpgradeSeasonsRemaining(Map<Building, Integer> buildingUpgradeSeasonsRemaining) {
        Objects.requireNonNull(buildingUpgradeSeasonsRemaining, "buildingUpgradeSeasonsRemaining must not be null");
        this.buildingUpgradeSeasonsRemaining = new LinkedHashMap<>(buildingUpgradeSeasonsRemaining);
    }

    /**
     * Returns the player's news log, oldest first, capped at the most recent
     * {@link #NEWS_LOG_CAPACITY} entries - {@code design.md} &sect;2.7. The live list is returned.
     *
     * @return the news entries, never {@code null}
     */
    public List<NewsEntry> getNewsLog() {
        return newsLog;
    }

    /**
     * Replaces the news log. Public for Jackson/Kryo, and copied defensively.
     *
     * @param newsLog the entries to set, oldest first
     * @throws NullPointerException if {@code newsLog} is {@code null}
     */
    public void setNewsLog(List<NewsEntry> newsLog) {
        Objects.requireNonNull(newsLog, "newsLog must not be null");
        this.newsLog = new ArrayList<>(newsLog);
    }

    /**
     * Returns the astronauts this agency lost, oldest first, for the Museum's memorial
     * ({@code ui-spec.md} gap G10). Never trimmed. The live list is returned.
     *
     * @return the entries, never {@code null}
     */
    public List<MemorialEntry> getMemorial() {
        return memorial;
    }

    /**
     * Replaces the memorial. Public for Jackson/Kryo, and copied defensively.
     *
     * @param memorial the entries to set, oldest first
     * @throws NullPointerException if {@code memorial} is {@code null}
     */
    public void setMemorial(List<MemorialEntry> memorial) {
        Objects.requireNonNull(memorial, "memorial must not be null");
        this.memorial = new ArrayList<>(memorial);
    }

    /**
     * Returns how each goal settled, in settlement order, for the Museum's achievements. Goals
     * settled before these records existed have none. The live list is returned.
     *
     * @return the records, never {@code null}
     */
    public List<GoalRecord> getGoalRecords() {
        return goalRecords;
    }

    /**
     * Replaces the goal records. Public for Jackson/Kryo, and copied defensively.
     *
     * @param goalRecords the records to set, in settlement order
     * @throws NullPointerException if {@code goalRecords} is {@code null}
     */
    public void setGoalRecords(List<GoalRecord> goalRecords) {
        Objects.requireNonNull(goalRecords, "goalRecords must not be null");
        this.goalRecords = new ArrayList<>(goalRecords);
    }

    /**
     * Returns the game turn of the most recent season a mission of this player flew
     * without a failure, or {@code -1} if none has.
     *
     * @return the turn, or {@code -1}
     */
    public int getLastFlightSeasonTurn() {
        return lastFlightSeasonTurn;
    }

    /**
     * Sets the turn of the most recent failure-free flight season.
     *
     * @param lastFlightSeasonTurn the turn, or {@code -1}
     */
    public void setLastFlightSeasonTurn(int lastFlightSeasonTurn) {
        this.lastFlightSeasonTurn = lastFlightSeasonTurn;
    }

    /**
     * Returns the game turn of the most recent mission failure, or {@code -1} if none has.
     *
     * @return the turn, or {@code -1}
     */
    public int getLastFailureTurn() {
        return lastFailureTurn;
    }

    /**
     * Sets the turn of the most recent mission failure.
     *
     * @param lastFailureTurn the turn, or {@code -1}
     */
    public void setLastFailureTurn(int lastFailureTurn) {
        this.lastFailureTurn = lastFailureTurn;
    }

    /**
     * Returns the player's prestige as it stood at the last funding review, the
     * base of the next review's catch-up shortfall ({@code baspm.md} &sect;11.7).
     *
     * @return the prestige recorded at the last review, zero before the first
     */
    public long getPrestigeAtLastReview() {
        return prestigeAtLastReview;
    }

    /**
     * Sets the prestige recorded at the last funding review.
     *
     * @param prestigeAtLastReview the prestige to record
     */
    public void setPrestigeAtLastReview(long prestigeAtLastReview) {
        this.prestigeAtLastReview = prestigeAtLastReview;
    }

    /**
     * Returns the goals this player has achieved, mapped to the game turn each
     * one settled on. Goals cancelled by an achieved goal are not in here - see
     * {@link #getCancelledGoalIds()}. The live map is returned.
     *
     * @return the achieved goals and their settlement turns, never {@code null}
     */
    public Map<String, Integer> getAchievedGoals() {
        return achievedGoals;
    }

    /**
     * Replaces the achieved goals. Public for Jackson/Kryo, and copied defensively.
     *
     * @param achievedGoals the achieved goals to set
     * @throws NullPointerException if {@code achievedGoals} is {@code null}
     */
    public void setAchievedGoals(Map<String, Integer> achievedGoals) {
        Objects.requireNonNull(achievedGoals, "achievedGoals must not be null");
        this.achievedGoals = new LinkedHashMap<>(achievedGoals);
    }

    /**
     * Returns the goals cancelled by an achieved goal, which count as fulfilled
     * for gates and modifiers but never pay as achieved. The live set is returned.
     *
     * @return the cancelled goal ids, never {@code null}
     */
    public Set<String> getCancelledGoalIds() {
        return cancelledGoalIds;
    }

    /**
     * Replaces the cancelled goal ids. Public for Jackson/Kryo, and copied defensively.
     *
     * @param cancelledGoalIds the cancelled goal ids to set
     * @throws NullPointerException if {@code cancelledGoalIds} is {@code null}
     */
    public void setCancelledGoalIds(Set<String> cancelledGoalIds) {
        Objects.requireNonNull(cancelledGoalIds, "cancelledGoalIds must not be null");
        this.cancelledGoalIds = new LinkedHashSet<>(cancelledGoalIds);
    }

    /**
     * Returns whether a goal counts as fulfilled for gates and modifiers:
     * achieved, or cancelled by an achieved goal. BASPM's own "achieved or
     * cancelled" check - {@code baspm.md} &sect;11.3.
     *
     * @param goalId the goal id to check
     * @return {@code true} if the goal is achieved or cancelled
     */
    public boolean hasAchievedOrCancelledGoal(String goalId) {
        return achievedGoals.containsKey(goalId) || cancelledGoalIds.contains(goalId);
    }

    /**
     * Returns the next id {@link #allocateMissionInstanceId()} will hand
     * out. Public only because Jackson/Kryo need a bean accessor - callers
     * should use {@link #allocateMissionInstanceId()}, never read or set
     * this directly.
     *
     * @return the next mission instance id counter value
     */
    public long getNextMissionInstanceId() {
        return nextMissionInstanceId;
    }

    /**
     * Sets the next id {@link #allocateMissionInstanceId()} will hand out.
     *
     * @param nextMissionInstanceId the next mission instance id counter value
     */
    public void setNextMissionInstanceId(long nextMissionInstanceId) {
        this.nextMissionInstanceId = nextMissionInstanceId;
    }

    /**
     * Allocates a fresh, globally-unique-within-this-snapshot mission
     * instance id and advances the counter - the single source of new ids
     * for every {@link InProgressMission} this player ever launches, so an
     * id is never reused even once that instance has ended. A separate
     * counter from {@link #allocatePersonnelId()}'s, since the two id
     * spaces are never compared against each other.
     *
     * @return the newly allocated id, e.g. {@code "m3"}
     */
    public String allocateMissionInstanceId() {
        String id = "m" + nextMissionInstanceId;
        nextMissionInstanceId++;
        return id;
    }

    /**
     * Allocates a fresh, globally-unique-within-this-snapshot personnel id
     * and advances the counter - the single source of new ids for every
     * astronaut/SET specialist/Mission Control officer this player ever
     * generates (hiring candidates included), across every group and every
     * season, so an id is never reused even once its original owner has
     * departed. A flat counter shared across all three groups, not
     * group-prefixed - {@code PersonnelGroup} (rules-engine side) already
     * disambiguates which roster/pool an id belongs to wherever that
     * matters.
     *
     * @return the newly allocated id, e.g. {@code "p42"}
     */
    public String allocatePersonnelId() {
        String id = "p" + nextPersonnelId;
        nextPersonnelId++;
        return id;
    }

    /**
     * Returns the agency's intelligence service ({@code design.md} &sect;2.9).
     *
     * @return the service, never {@code null}
     */
    public IntelligenceState getIntelligence() {
        return intelligence;
    }

    /**
     * Sets the intelligence service.
     *
     * @param intelligence the service, or {@code null} for none (level 0)
     */
    public void setIntelligence(IntelligenceState intelligence) {
        this.intelligence = intelligence == null ? new IntelligenceState() : intelligence;
    }

    /**
     * Returns the agency's standing order against poaching, per group: rival offers up to this
     * raise, in percent, are matched ({@code design.md} &sect;2.2(d)). A group missing here uses
     * the rules' default.
     *
     * @return the percentages, by {@code PersonnelGroup} name, live
     */
    public Map<String, Integer> getRetentionMatchPercent() {
        return retentionMatchPercent;
    }

    /**
     * Sets the standing order against poaching, per group.
     *
     * @param retentionMatchPercent the percentages, by group name
     */
    public void setRetentionMatchPercent(Map<String, Integer> retentionMatchPercent) {
        this.retentionMatchPercent = retentionMatchPercent == null ? new LinkedHashMap<>()
            : new LinkedHashMap<>(retentionMatchPercent);
    }
}
