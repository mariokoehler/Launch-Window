package de.mkoehler.launchwindow.actions;

import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.persistence.ActionContext;
import de.mkoehler.launchwindow.persistence.ActionResult;
import de.mkoehler.launchwindow.persistence.PlayerAction;
import de.mkoehler.launchwindow.rules.LaunchRequest;
import de.mkoehler.launchwindow.rules.MissionLaunch;
import de.mkoehler.launchwindow.rules.MissionLaunchOutcome;

import java.util.ArrayList;
import java.util.List;

/**
 * Launches a mission with one person per staffing slot and one rocket per launch
 * ({@link MissionLaunch#launch}, {@code baspm.md} &sect;10.11).
 *
 * <p>Journaled as {@code "launchMission"}; see {@code design.md} &sect;5, M4.</p>
 *
 * @author Mario Koehler
 */
public class LaunchMissionAction extends PlayerAction {

    private String missionConfigurationId;
    private List<String> crewSlotIds = new ArrayList<>();
    private List<String> controlSlotIds = new ArrayList<>();
    private List<String> rocketIds = new ArrayList<>();

    /**
     * No-argument constructor for Jackson and Kryo.
     */
    public LaunchMissionAction() {
    }

    /**
     * Creates the action.
     *
     * @param missionConfigurationId the configuration to fly
     * @param crewSlotIds            one astronaut per crew slot, {@code null} for an unused slot
     * @param controlSlotIds         one person per control slot, {@code null} for an unused slot
     * @param rocketIds              one rocket per launch, in launch order
     */
    public LaunchMissionAction(String missionConfigurationId, List<String> crewSlotIds, List<String> controlSlotIds,
            List<String> rocketIds) {
        setMissionConfigurationId(missionConfigurationId);
        setCrewSlotIds(crewSlotIds);
        setControlSlotIds(controlSlotIds);
        setRocketIds(rocketIds);
    }

    /**
     * Compatibility form for configurations with generic slots: the controllers fill
     * the first control slots and the CAPCOM astronauts the ones after them
     * ({@link LaunchRequest}'s matching constructor).
     *
     * @param missionConfigurationId   the configuration to fly
     * @param astronautIds             the crew
     * @param missionControlOfficerIds the flight controllers
     * @param capcomAstronautIds       the CAPCOM astronauts
     * @param rocketIds                one rocket per launch, in launch order
     */
    public LaunchMissionAction(String missionConfigurationId, List<String> astronautIds,
            List<String> missionControlOfficerIds, List<String> capcomAstronautIds, List<String> rocketIds) {
        this(missionConfigurationId, astronautIds, new LaunchRequest(missionConfigurationId, astronautIds,
            missionControlOfficerIds, capcomAstronautIds, rocketIds).controlSlotIds(), rocketIds);
    }

    /**
     * Returns the configuration to fly.
     *
     * @return the configuration to fly
     */
    public String getMissionConfigurationId() {
        return missionConfigurationId;
    }

    /**
     * Sets the configuration to fly.
     *
     * @param missionConfigurationId the configuration to fly
     */
    public void setMissionConfigurationId(String missionConfigurationId) {
        this.missionConfigurationId = missionConfigurationId;
    }

    /**
     * Returns the crew, one astronaut per crew slot.
     *
     * @return the crew slot assignments
     */
    public List<String> getCrewSlotIds() {
        return crewSlotIds;
    }

    /**
     * Sets the crew slot assignments.
     *
     * @param crewSlotIds the crew slot assignments
     */
    public void setCrewSlotIds(List<String> crewSlotIds) {
        this.crewSlotIds = crewSlotIds == null ? new ArrayList<>() : new ArrayList<>(crewSlotIds);
    }

    /**
     * Returns the ground staff, one person per control slot.
     *
     * @return the control slot assignments
     */
    public List<String> getControlSlotIds() {
        return controlSlotIds;
    }

    /**
     * Sets the control slot assignments.
     *
     * @param controlSlotIds the control slot assignments
     */
    public void setControlSlotIds(List<String> controlSlotIds) {
        this.controlSlotIds = controlSlotIds == null ? new ArrayList<>() : new ArrayList<>(controlSlotIds);
    }

    /**
     * Returns one rocket per launch, in launch order.
     *
     * @return one rocket per launch, in launch order
     */
    public List<String> getRocketIds() {
        return rocketIds;
    }

    /**
     * Sets one rocket per launch, in launch order.
     *
     * @param rocketIds one rocket per launch, in launch order
     */
    public void setRocketIds(List<String> rocketIds) {
        this.rocketIds = rocketIds == null ? new ArrayList<>() : new ArrayList<>(rocketIds);
    }

    @Override
    public ActionResult apply(PlayerSnapshot snapshot, ActionContext context) {
        return ActionResult.of(MissionLaunch.launch(snapshot, new LaunchRequest(missionConfigurationId, crewSlotIds,
            controlSlotIds, rocketIds), context.catalog()), MissionLaunchOutcome.LAUNCHED);
    }
}
