package de.mkoehler.launchwindow.net;

import com.esotericsoftware.kryo.Kryo;
import de.mkoehler.launchwindow.actions.AdjustSalariesAction;
import de.mkoehler.launchwindow.actions.AssignResearchAction;
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
import de.mkoehler.launchwindow.actions.ReassignControlSlotAction;
import de.mkoehler.launchwindow.actions.ReleaseResearchAction;
import de.mkoehler.launchwindow.actions.StartExpertTrainingAction;
import de.mkoehler.launchwindow.actions.StartTrainingAction;
import de.mkoehler.launchwindow.actions.UpgradeBuildingAction;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.AstronautSkill;
import de.mkoehler.launchwindow.model.Building;
import de.mkoehler.launchwindow.model.InProgressMission;
import de.mkoehler.launchwindow.model.GoalRecord;
import de.mkoehler.launchwindow.model.MemorialEntry;
import de.mkoehler.launchwindow.model.NewsEntry;
import de.mkoehler.launchwindow.model.RandomEventKind;
import de.mkoehler.launchwindow.model.MissionControlOfficer;
import de.mkoehler.launchwindow.model.MissionControlSkill;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.Season;
import de.mkoehler.launchwindow.model.SetSkill;
import de.mkoehler.launchwindow.model.SetSpecialist;
import de.mkoehler.launchwindow.model.TrainingKind;
import de.mkoehler.launchwindow.net.messages.ClaimSlotRequest;
import de.mkoehler.launchwindow.net.messages.ClaimSlotResponse;
import de.mkoehler.launchwindow.net.messages.GameInfoRequest;
import de.mkoehler.launchwindow.net.messages.GameInfoResponse;
import de.mkoehler.launchwindow.net.messages.RivalSummary;
import de.mkoehler.launchwindow.net.messages.RivalsRequest;
import de.mkoehler.launchwindow.net.messages.RivalsResponse;
import de.mkoehler.launchwindow.net.messages.TurnReportRequest;
import de.mkoehler.launchwindow.net.messages.TurnReportResponse;
import de.mkoehler.launchwindow.net.messages.FetchStateRequest;
import de.mkoehler.launchwindow.net.messages.FetchStateResponse;
import de.mkoehler.launchwindow.net.messages.HandshakeRequest;
import de.mkoehler.launchwindow.net.messages.HandshakeResponse;
import de.mkoehler.launchwindow.net.messages.SubmitActionRequest;
import de.mkoehler.launchwindow.net.messages.SubmitActionResponse;
import de.mkoehler.launchwindow.persistence.SetFundsAction;
import de.mkoehler.launchwindow.rules.PersonnelGroup;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

/**
 * <p>Registers every class sent over the wire with a {@link Kryo} instance,
 * in a fixed order.</p>
 *
 * <p>Kryo assigns each registered class a numeric id based on registration
 * order and relies on that id, rather than the class name, to identify
 * types on the wire. Both ends of a connection must therefore register the
 * exact same classes in the exact same order, or messages will be misread
 * on the receiving end. This class is the single shared place that order
 * is defined, to be used by both the server and every client.</p>
 *
 * <p>{@link HandshakeRequest} and {@link HandshakeResponse} are registered
 * first and never move: the version check they carry exists specifically to
 * catch two builds whose registries have drifted apart, so their own two
 * classes must keep the same ids regardless of what else in the registry
 * has changed. This protects against registration-order drift in every
 * *other* class, not against a change to the handshake classes' own
 * fields - see their own Javadoc for why those never change either.
 * Everything else follows,
 * append-only: a new class, whichever package it comes from, is added as a
 * new line at the end of {@link #register(Kryo)}. Existing lines are never
 * reordered, removed, or grouped by package - inserting a new line in the
 * middle of an existing block would shift every registration id after it
 * and silently break the wire format for anyone running a different build
 * on the other end.</p>
 *
 * @author Mario Koehler
 */
public final class MessageRegistry {

    private static final String PROTOCOL_PACKAGE_PREFIX = "de.mkoehler.launchwindow.";

    /**
     * Not instantiable; this class only exposes static methods.
     */
    private MessageRegistry() {
    }

    /**
     * Tells this project's own messages apart from KryoNet's housekeeping
     * objects (keep-alive frames and the like), which a listener must never
     * hand on to application code.
     *
     * @param object what KryoNet delivered
     * @return {@code true} if it is an instance of one of this project's
     *     own classes
     */
    public static boolean isProtocolMessage(Object object) {
        return object != null && object.getClass().getName().startsWith(PROTOCOL_PACKAGE_PREFIX);
    }

    /**
     * Registers all wire message classes, and every class their fields
     * transitively reference, with the given {@link Kryo} instance.
     *
     * @param kryo the Kryo instance to register classes with, typically
     *             obtained from an {@code EndPoint}'s {@code getKryo()}
     *             method
     */
    public static void register(Kryo kryo) {
        // The handshake, first and pinned - see the class Javadoc for why.
        kryo.register(HandshakeRequest.class);
        kryo.register(HandshakeResponse.class);
        // de.mkoehler.launchwindow.model - referenced by PlayerSnapshot, carried in
        // FetchStateResponse.
        kryo.register(Archetype.class);
        kryo.register(AssignmentState.class);
        kryo.register(AstronautSkill.class);
        kryo.register(SetSkill.class);
        kryo.register(MissionControlSkill.class);
        kryo.register(Astronaut.class);
        kryo.register(SetSpecialist.class);
        kryo.register(MissionControlOfficer.class);
        kryo.register(PlayerSnapshot.class);
        kryo.register(ArrayList.class);
        kryo.register(LinkedHashMap.class);
        // de.mkoehler.launchwindow.persistence - concrete PlayerAction subtypes. Every one
        // listed in PlayerAction's own @JsonSubTypes must also be registered here -
        // MessageRegistryTest.everyPlayerActionSubtypeIsRegistered() checks this.
        kryo.register(SetFundsAction.class);
        // Remaining wire messages.
        kryo.register(SubmitActionRequest.class);
        kryo.register(SubmitActionResponse.class);
        kryo.register(FetchStateRequest.class);
        kryo.register(FetchStateResponse.class);
        // Appended 2026-09-30 (M2's training-progress groundwork) - referenced by Personnel,
        // carried in PlayerSnapshot. Never move this line; see the class Javadoc.
        kryo.register(TrainingKind.class);
        // Appended 2026-10-01 (M2's programs/mission-components groundwork) - referenced by
        // PlayerSnapshot.missionComponents, carried in FetchStateResponse. Never move this line;
        // see the class Javadoc.
        kryo.register(OwnedMissionComponent.class);
        // Appended 2026-10-02 (M2's mission-simulation per-player state) - referenced by
        // PlayerSnapshot.inProgressMissions, carried in FetchStateResponse. Never move this line;
        // see the class Javadoc.
        kryo.register(InProgressMission.class);
        // Appended 2026-10-02 (the Expert trait, design.md §2.2(c)) - backs
        // Astronaut.expertSkills/MissionControlOfficer.expertSkills, carried in
        // FetchStateResponse. The first Set type this registry has ever needed - every earlier
        // collection field used List/Map. Never move this line; see the class Javadoc.
        kryo.register(LinkedHashSet.class);
        // Appended 2026-10-03 (random events, design.md §2.7): the news log carries NewsEntry and its kind
        // enum, carried in FetchStateResponse. Never move these lines; see the class Javadoc.
        kryo.register(RandomEventKind.class);
        kryo.register(NewsEntry.class);
        // Appended 2026-10-03 (M3 buildings): the keys of PlayerSnapshot.buildingLevels and
        // buildingUpgradeSeasonsRemaining, carried in FetchStateResponse. Never move this line;
        // see the class Javadoc.
        kryo.register(Building.class);
        // Appended 2026-10-03 (M4 player actions, design.md section 5): every journaled action travels in
        // SubmitActionRequest, plus the group enum several of them carry. Never move these lines; see the
        // class Javadoc.
        kryo.register(PersonnelGroup.class);
        kryo.register(OpenProgramAction.class);
        kryo.register(CloseProgramAction.class);
        kryo.register(OpenRocketProgramAction.class);
        kryo.register(CloseRocketProgramAction.class);
        kryo.register(LaunchMissionAction.class);
        kryo.register(CancelMissionAction.class);
        kryo.register(HireAction.class);
        kryo.register(DismissAction.class);
        kryo.register(UpgradeBuildingAction.class);
        kryo.register(ConvertPrestigeAction.class);
        kryo.register(AssignResearchAction.class);
        kryo.register(ReleaseResearchAction.class);
        kryo.register(StartTrainingAction.class);
        kryo.register(StartExpertTrainingAction.class);
        kryo.register(PullOutOfTrainingAction.class);
        // Appended 2026-10-03 (M4 slot claim, design.md section 3.8). Never move these lines.
        kryo.register(ClaimSlotRequest.class);
        kryo.register(ClaimSlotResponse.class);
        // Appended 2026-10-03 (M5 staffing slots, baspm.md section 10.11). Never move this line.
        kryo.register(ReassignControlSlotAction.class);
        // Appended 2026-10-04 (M5 connect/join screens): ClaimSlotResponse's refusal reason. Never move this line.
        kryo.register(ClaimSlotResponse.Reason.class);
        // Appended 2026-10-04 (M5 game info, ui-spec.md gap G1). Never move these lines.
        kryo.register(Season.class);
        kryo.register(GameInfoRequest.class);
        kryo.register(GameInfoResponse.class);
        // Appended 2026-10-04 (M5 department salary adjustment, ui-spec.md gap G8). Never move this line.
        kryo.register(AdjustSalariesAction.class);
        // Appended 2026-10-04 (M5 turn reports, ui-spec.md gap G2). Never move these lines.
        kryo.register(TurnReportRequest.class);
        kryo.register(TurnReportResponse.class);
        kryo.register(byte[].class);
        // Appended 2026-10-04 (M5 rival summaries, ui-spec.md gap G3). Never move these lines.
        kryo.register(RivalsRequest.class);
        kryo.register(RivalsResponse.class);
        kryo.register(RivalSummary.class);
        // Appended 2026-10-04 (M5 Museum: the memorial, gap G10, and goal records). Never move these lines.
        kryo.register(MemorialEntry.class);
        kryo.register(MemorialEntry.Cause.class);
        kryo.register(GoalRecord.class);
    }
}
