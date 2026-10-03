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
 * Launches a mission with the chosen crew, controllers, CAPCOM, and rockets ({@link MissionLaunch#launch}).
 *
 * <p>Journaled as {@code "launchMission"}; see {@code design.md} &sect;5, M4.</p>
 *
 * @author Mario Koehler
 */
public class LaunchMissionAction extends PlayerAction {

    private String missionConfigurationId;
    private List<String> astronautIds = new ArrayList<>();
    private List<String> missionControlOfficerIds = new ArrayList<>();
    private List<String> capcomAstronautIds = new ArrayList<>();
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
     * @param astronautIds the crew
     * @param missionControlOfficerIds the flight controllers
     * @param capcomAstronautIds the CAPCOM astronauts
     * @param rocketIds one rocket per launch, in launch order
     */
    public LaunchMissionAction(String missionConfigurationId, List<String> astronautIds, List<String> missionControlOfficerIds, List<String> capcomAstronautIds, List<String> rocketIds) {
        setMissionConfigurationId(missionConfigurationId);
        setAstronautIds(astronautIds);
        setMissionControlOfficerIds(missionControlOfficerIds);
        setCapcomAstronautIds(capcomAstronautIds);
        setRocketIds(rocketIds);
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
     * Returns the crew.
     *
     * @return the crew
     */
    public List<String> getAstronautIds() {
        return astronautIds;
    }

    /**
     * Sets the crew.
     *
     * @param astronautIds the crew
     */
    public void setAstronautIds(List<String> astronautIds) {
        this.astronautIds = astronautIds == null ? new ArrayList<>() : new ArrayList<>(astronautIds);
    }

    /**
     * Returns the flight controllers.
     *
     * @return the flight controllers
     */
    public List<String> getMissionControlOfficerIds() {
        return missionControlOfficerIds;
    }

    /**
     * Sets the flight controllers.
     *
     * @param missionControlOfficerIds the flight controllers
     */
    public void setMissionControlOfficerIds(List<String> missionControlOfficerIds) {
        this.missionControlOfficerIds = missionControlOfficerIds == null ? new ArrayList<>() : new ArrayList<>(missionControlOfficerIds);
    }

    /**
     * Returns the CAPCOM astronauts.
     *
     * @return the CAPCOM astronauts
     */
    public List<String> getCapcomAstronautIds() {
        return capcomAstronautIds;
    }

    /**
     * Sets the CAPCOM astronauts.
     *
     * @param capcomAstronautIds the CAPCOM astronauts
     */
    public void setCapcomAstronautIds(List<String> capcomAstronautIds) {
        this.capcomAstronautIds = capcomAstronautIds == null ? new ArrayList<>() : new ArrayList<>(capcomAstronautIds);
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
        return ActionResult.of(MissionLaunch.launch(snapshot, new LaunchRequest(missionConfigurationId, astronautIds,
            missionControlOfficerIds, capcomAstronautIds, rocketIds), context.catalog()), MissionLaunchOutcome.LAUNCHED);
    }
}
