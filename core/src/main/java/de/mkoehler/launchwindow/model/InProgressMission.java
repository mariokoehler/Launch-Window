package de.mkoehler.launchwindow.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private List<String> crewSlotIds = new ArrayList<>();
    private List<String> controlSlotIds = new ArrayList<>();
    private List<String> rocketIds = new ArrayList<>();
    private long launchCost;
    private boolean firstInGame;
    private Map<String, Double> launchReliability = new LinkedHashMap<>();

    /**
     * No-argument constructor required for Jackson/Kryo deserialization.
     */
    public InProgressMission() {
    }

    /**
     * Creates a new in-progress mission at season 0, with one person per slot.
     *
     * @param id                     a fresh instance id, unique within a player's
     *                               snapshot ({@link PlayerSnapshot#allocateMissionInstanceId()})
     * @param missionConfigurationId the configuration this instance is running
     * @param crewSlotIds            one astronaut id per crew slot of the configuration, in
     *                               slot order; {@code null} for a slot no step uses
     * @param controlSlotIds         one id per control slot, in slot order - a flight
     *                               controller, or an astronaut for a ground astronaut
     *                               slot such as CAPCOM; {@code null} for an unused slot
     */
    public InProgressMission(String id, String missionConfigurationId, List<String> crewSlotIds,
            List<String> controlSlotIds) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.missionConfigurationId =
            Objects.requireNonNull(missionConfigurationId, "missionConfigurationId must not be null");
        this.currentSeasonIndex = 0;
        setCrewSlotIds(crewSlotIds);
        setControlSlotIds(controlSlotIds);
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
     * Returns the crew, one astronaut id per crew slot in slot order, {@code null} where
     * a slot isn't used ({@code baspm.md} &sect;10.11). The live list is returned.
     *
     * @return the crew slot assignments
     */
    public List<String> getCrewSlotIds() {
        return crewSlotIds;
    }

    /**
     * Replaces the crew slot assignments. Copied; {@code null} entries are allowed.
     *
     * @param crewSlotIds the crew slot assignments
     */
    public void setCrewSlotIds(List<String> crewSlotIds) {
        Objects.requireNonNull(crewSlotIds, "crewSlotIds must not be null");
        this.crewSlotIds = new ArrayList<>(crewSlotIds);
    }

    /**
     * Returns the ground staff, one id per control slot in slot order: a flight
     * controller, or an astronaut for a ground astronaut slot such as CAPCOM;
     * {@code null} where a slot isn't used. Ids are unique across every roster, so
     * the roster that holds an id says which kind it is. The live list is returned.
     *
     * @return the control slot assignments
     */
    public List<String> getControlSlotIds() {
        return controlSlotIds;
    }

    /**
     * Replaces the control slot assignments. Copied; {@code null} entries are allowed.
     *
     * @param controlSlotIds the control slot assignments
     */
    public void setControlSlotIds(List<String> controlSlotIds) {
        Objects.requireNonNull(controlSlotIds, "controlSlotIds must not be null");
        this.controlSlotIds = new ArrayList<>(controlSlotIds);
    }

    /**
     * Returns the astronauts aboard: the filled crew slots. Derived, not stored.
     *
     * @return the crew ids, in slot order
     */
    public List<String> crewIds() {
        return crewSlotIds.stream().filter(Objects::nonNull).toList();
    }

    /**
     * Returns everyone on the ground: the filled control slots, flight controllers and
     * CAPCOM astronauts alike. Derived, not stored.
     *
     * @return the ground staff ids, in slot order
     */
    public List<String> groundIds() {
        return controlSlotIds.stream().filter(Objects::nonNull).toList();
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

    /**
     * Returns whether a step of this flight set a first in the game, which adds to the crew's fame
     * once the flight completes ({@code design.md} &sect;2.2(c)).
     *
     * @return whether the flight set a first
     */
    public boolean isFirstInGame() {
        return firstInGame;
    }

    /**
     * Records whether the flight set a first in the game.
     *
     * @param firstInGame whether it did
     */
    public void setFirstInGame(boolean firstInGame) {
        this.firstInGame = firstInGame;
    }

    /**
     * Returns each flown component's reliability as it was at launch (user decision 2026-10-07):
     * the hardware that flies is the copy built then, so ground R&amp;D and other flights' results
     * only change the component for later missions. Empty for a mission launched before this was
     * kept; such a flight reads the components as they are now.
     *
     * @return the reliabilities by component id
     */
    public Map<String, Double> getLaunchReliability() {
        return launchReliability;
    }

    /**
     * Sets the reliabilities recorded at launch.
     *
     * @param launchReliability the reliabilities by component id
     */
    public void setLaunchReliability(Map<String, Double> launchReliability) {
        this.launchReliability = launchReliability == null ? new LinkedHashMap<>()
            : new LinkedHashMap<>(launchReliability);
    }
}
