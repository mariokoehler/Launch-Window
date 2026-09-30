package de.mkoehler.launchwindow.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
}
