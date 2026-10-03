package de.mkoehler.launchwindow.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * <p>One player's running instance of a
 * {@link de.mkoehler.launchwindow.content.MissionConfiguration} - mutable
 * per-player state, distinct from that content definition itself, since the
 * same configuration can be launched more than once over a game (e.g.
 * repeated satellite launches under one program). See {@code design.md}
 * &sect;2.5(f).</p>
 *
 * @author Mario Koehler
 */
public class InProgressMission {

    private String id;
    private String missionConfigurationId;
    private int currentSeasonIndex;
    private List<String> assignedAstronautIds = new ArrayList<>();
    private List<String> assignedMissionControlOfficerIds = new ArrayList<>();
    private List<String> rocketIds = new ArrayList<>();
    private List<String> capcomAstronautIds = new ArrayList<>();
    private long launchCost;

    /**
     * No-argument constructor required for Jackson/Kryo deserialization.
     */
    public InProgressMission() {
    }

    /**
     * Creates a new in-progress mission at season 0, with the given
     * assigned personnel.
     *
     * @param id                                a fresh instance id, unique
     *                                          within a player's snapshot -
     *                                          see
     *                                          {@link PlayerSnapshot#allocateMissionInstanceId()}
     * @param missionConfigurationId            the
     *                                          {@link de.mkoehler.launchwindow.content.MissionConfiguration#id()}
     *                                          this instance is running
     * @param assignedAstronautIds              the crew assigned to this
     *                                          mission; empty if uncrewed
     * @param assignedMissionControlOfficerIds  the flight controllers
     *                                          assigned to this mission;
     *                                          may be empty
     */
    public InProgressMission(String id, String missionConfigurationId, List<String> assignedAstronautIds,
            List<String> assignedMissionControlOfficerIds) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.missionConfigurationId =
            Objects.requireNonNull(missionConfigurationId, "missionConfigurationId must not be null");
        this.currentSeasonIndex = 0;
        setAssignedAstronautIds(assignedAstronautIds);
        setAssignedMissionControlOfficerIds(assignedMissionControlOfficerIds);
    }

    /**
     * Returns this instance's own id, unique within a player's snapshot.
     *
     * @return the instance id
     */
    public String getId() {
        return id;
    }

    /**
     * Sets this instance's own id.
     *
     * @param id the instance id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the {@link de.mkoehler.launchwindow.content.MissionConfiguration#id()}
     * this instance is running.
     *
     * @return the mission configuration id
     */
    public String getMissionConfigurationId() {
        return missionConfigurationId;
    }

    /**
     * Sets the mission configuration id this instance is running.
     *
     * @param missionConfigurationId the mission configuration id
     */
    public void setMissionConfigurationId(String missionConfigurationId) {
        this.missionConfigurationId = missionConfigurationId;
    }

    /**
     * Returns the 0-based index, into the mission configuration's own
     * {@code seasons()} list, of the season whose step batch resolves next.
     *
     * @return the current season index
     */
    public int getCurrentSeasonIndex() {
        return currentSeasonIndex;
    }

    /**
     * Sets the current season index.
     *
     * @param currentSeasonIndex the current season index
     */
    public void setCurrentSeasonIndex(int currentSeasonIndex) {
        this.currentSeasonIndex = currentSeasonIndex;
    }

    /**
     * Returns the astronaut ids assigned as this mission's crew. Empty for
     * an uncrewed mission.
     *
     * @return the assigned astronaut ids
     */
    public List<String> getAssignedAstronautIds() {
        return assignedAstronautIds;
    }

    /**
     * Replaces the assigned astronaut ids.
     *
     * @param assignedAstronautIds the assigned astronaut ids
     */
    public void setAssignedAstronautIds(List<String> assignedAstronautIds) {
        Objects.requireNonNull(assignedAstronautIds, "assignedAstronautIds must not be null");
        this.assignedAstronautIds = new ArrayList<>(assignedAstronautIds);
    }

    /**
     * Returns the Mission Control officer ids assigned to this mission.
     * May be empty.
     *
     * @return the assigned Mission Control officer ids
     */
    public List<String> getAssignedMissionControlOfficerIds() {
        return assignedMissionControlOfficerIds;
    }

    /**
     * Replaces the assigned Mission Control officer ids.
     *
     * @param assignedMissionControlOfficerIds the assigned Mission Control
     *                                          officer ids
     */
    public void setAssignedMissionControlOfficerIds(List<String> assignedMissionControlOfficerIds) {
        Objects.requireNonNull(assignedMissionControlOfficerIds,
            "assignedMissionControlOfficerIds must not be null");
        this.assignedMissionControlOfficerIds = new ArrayList<>(assignedMissionControlOfficerIds);
    }

    /**
     * Returns the rocket chosen for each launch, in launch order - empty for a
     * configuration with no launches. A step's rocket slot {@code @rocket-n} resolves
     * to entry {@code n - 1} ({@code design.md} &sect;5, M3).
     *
     * @return the rocket component ids, never {@code null}
     */
    public List<String> getRocketIds() {
        return rocketIds;
    }

    /**
     * Replaces the chosen rockets. Public for Jackson/Kryo, and copied defensively.
     *
     * @param rocketIds the rocket component ids, in launch order
     * @throws NullPointerException if {@code rocketIds} is {@code null}
     */
    public void setRocketIds(List<String> rocketIds) {
        Objects.requireNonNull(rocketIds, "rocketIds must not be null");
        this.rocketIds = new ArrayList<>(rocketIds);
    }

    /**
     * Returns the astronauts serving as CAPCOM on the ground for this mission - BASPM's
     * CAPCOM slot ({@code baspm.md} &sect;10.9). They are {@code ASSIGNED} to this mission
     * like the crew, but never fly, so a crew loss never touches them.
     *
     * @return the CAPCOM astronaut ids, never {@code null}
     */
    public List<String> getCapcomAstronautIds() {
        return capcomAstronautIds;
    }

    /**
     * Replaces the CAPCOM astronauts. Public for Jackson/Kryo, and copied defensively.
     *
     * @param capcomAstronautIds the CAPCOM astronaut ids
     * @throws NullPointerException if {@code capcomAstronautIds} is {@code null}
     */
    public void setCapcomAstronautIds(List<String> capcomAstronautIds) {
        Objects.requireNonNull(capcomAstronautIds, "capcomAstronautIds must not be null");
        this.capcomAstronautIds = new ArrayList<>(capcomAstronautIds);
    }

    /**
     * Returns what launching this mission cost - the configuration's cost plus its
     * rockets' unit costs - so a scrub or cancellation refunds against the price
     * actually paid ({@code design.md} &sect;5, M4).
     *
     * @return the launch cost
     */
    public long getLaunchCost() {
        return launchCost;
    }

    /**
     * Sets what launching this mission cost.
     *
     * @param launchCost the launch cost, zero or more
     */
    public void setLaunchCost(long launchCost) {
        this.launchCost = launchCost;
    }
}