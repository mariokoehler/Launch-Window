package de.mkoehler.launchwindow.persistence;

import de.mkoehler.launchwindow.actions.RecallFromPrDutyAction;
import de.mkoehler.launchwindow.actions.StartPrDutyAction;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import de.mkoehler.launchwindow.actions.AdjustSalariesAction;
import de.mkoehler.launchwindow.actions.AssignResearchAction;
import de.mkoehler.launchwindow.actions.BackgroundCheckAction;
import de.mkoehler.launchwindow.actions.CancelIntelOperationAction;
import de.mkoehler.launchwindow.actions.MakeOfferAction;
import de.mkoehler.launchwindow.actions.SetRetentionAction;
import de.mkoehler.launchwindow.actions.SetRetentionDefaultAction;
import de.mkoehler.launchwindow.actions.CancelMissionAction;
import de.mkoehler.launchwindow.actions.CloseProgramAction;
import de.mkoehler.launchwindow.actions.CloseRocketProgramAction;
import de.mkoehler.launchwindow.actions.ConvertPrestigeAction;
import de.mkoehler.launchwindow.actions.DismissAction;
import de.mkoehler.launchwindow.actions.HireAction;
import de.mkoehler.launchwindow.actions.LaunchMissionAction;
import de.mkoehler.launchwindow.actions.OpenProgramAction;
import de.mkoehler.launchwindow.actions.OpenRocketProgramAction;
import de.mkoehler.launchwindow.actions.PullOutOfTrainingAction;
import de.mkoehler.launchwindow.actions.QueueIntelOperationAction;
import de.mkoehler.launchwindow.actions.ReassignControlSlotAction;
import de.mkoehler.launchwindow.actions.ReleaseResearchAction;
import de.mkoehler.launchwindow.actions.SetIntelligenceTargetAction;
import de.mkoehler.launchwindow.actions.StartExpertTrainingAction;
import de.mkoehler.launchwindow.actions.StartTrainingAction;
import de.mkoehler.launchwindow.actions.UpgradeBuildingAction;
import de.mkoehler.launchwindow.model.PlayerSnapshot;

/**
 * <p>One player-submitted action, persisted as its own append-only journal file
 * ({@code design.md} &sect;3.7). Applying it calls the matching validated rules
 * method, which either takes effect or changes nothing and reports why.</p>
 *
 * <p><b>Undo is by inverse action only</b> (user decision, 2026-10-03,
 * {@code design.md} &sect;5, M4): the journal is never edited, and a player takes
 * back a one-off action by queuing its opposite, at that action's normal cost.</p>
 *
 * <p><b>Replays must be deterministic.</b> A player's pending actions are replayed
 * on every state fetch and again at resolution, so an action that draws random
 * numbers draws them from its own {@link #getSeed() seed}. The server assigns the
 * seed when it accepts the action, overwriting anything a client sent, so a client
 * can't choose its own rolls.</p>
 *
 * @author Mario Koehler
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = SetFundsAction.class, name = "setFunds"),
    @JsonSubTypes.Type(value = OpenProgramAction.class, name = "openProgram"),
    @JsonSubTypes.Type(value = CloseProgramAction.class, name = "closeProgram"),
    @JsonSubTypes.Type(value = OpenRocketProgramAction.class, name = "openRocketProgram"),
    @JsonSubTypes.Type(value = CloseRocketProgramAction.class, name = "closeRocketProgram"),
    @JsonSubTypes.Type(value = LaunchMissionAction.class, name = "launchMission"),
    @JsonSubTypes.Type(value = CancelMissionAction.class, name = "cancelMission"),
    @JsonSubTypes.Type(value = HireAction.class, name = "hire"),
    @JsonSubTypes.Type(value = DismissAction.class, name = "dismiss"),
    @JsonSubTypes.Type(value = UpgradeBuildingAction.class, name = "upgradeBuilding"),
    @JsonSubTypes.Type(value = ConvertPrestigeAction.class, name = "convertPrestige"),
    @JsonSubTypes.Type(value = AssignResearchAction.class, name = "assignResearch"),
    @JsonSubTypes.Type(value = ReleaseResearchAction.class, name = "releaseResearch"),
    @JsonSubTypes.Type(value = StartTrainingAction.class, name = "startTraining"),
    @JsonSubTypes.Type(value = StartExpertTrainingAction.class, name = "startExpertTraining"),
    @JsonSubTypes.Type(value = PullOutOfTrainingAction.class, name = "pullOutOfTraining"),
    @JsonSubTypes.Type(value = ReassignControlSlotAction.class, name = "reassignControlSlot"),
    @JsonSubTypes.Type(value = AdjustSalariesAction.class, name = "adjustSalaries"),
    @JsonSubTypes.Type(value = StartPrDutyAction.class, name = "startPrDuty"),
    @JsonSubTypes.Type(value = RecallFromPrDutyAction.class, name = "recallFromPrDuty"),
    @JsonSubTypes.Type(value = SetIntelligenceTargetAction.class, name = "setIntelligenceTarget"),
    @JsonSubTypes.Type(value = QueueIntelOperationAction.class, name = "queueIntelOperation"),
    @JsonSubTypes.Type(value = CancelIntelOperationAction.class, name = "cancelIntelOperation"),
    @JsonSubTypes.Type(value = BackgroundCheckAction.class, name = "backgroundCheck"),
    @JsonSubTypes.Type(value = MakeOfferAction.class, name = "makeOffer"),
    @JsonSubTypes.Type(value = SetRetentionDefaultAction.class, name = "setRetentionDefault"),
    @JsonSubTypes.Type(value = SetRetentionAction.class, name = "setRetention")
})
public abstract class PlayerAction {

    private long seed;

    /**
     * Returns the seed this action draws any random numbers from.
     *
     * @return the seed
     */
    public long getSeed() {
        return seed;
    }

    /**
     * Sets the seed. The server sets it when it accepts the action.
     *
     * @param seed the seed
     */
    public void setSeed(long seed) {
        this.seed = seed;
    }

    /**
     * Applies this action to a player through its validated rules method.
     *
     * @param snapshot the player; mutated only when the action takes effect
     * @param context  the game's content and budget schedule
     * @return whether it took effect, and the rules outcome
     */
    public abstract ActionResult apply(PlayerSnapshot snapshot, ActionContext context);
}
