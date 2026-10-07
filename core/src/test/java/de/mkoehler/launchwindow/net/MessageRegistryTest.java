package de.mkoehler.launchwindow.net;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import de.mkoehler.launchwindow.content.BudgetSchedule;
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
import de.mkoehler.launchwindow.model.Rapport;
import de.mkoehler.launchwindow.model.Trait;
import de.mkoehler.launchwindow.model.TraitState;
import de.mkoehler.launchwindow.model.SetSkill;
import de.mkoehler.launchwindow.model.SetSpecialist;
import de.mkoehler.launchwindow.model.TrainingKind;
import de.mkoehler.launchwindow.net.messages.ClaimSlotRequest;
import de.mkoehler.launchwindow.net.messages.ClaimSlotResponse;
import de.mkoehler.launchwindow.net.messages.FetchStateRequest;
import de.mkoehler.launchwindow.net.messages.FetchStateResponse;
import de.mkoehler.launchwindow.net.messages.GameInfoRequest;
import de.mkoehler.launchwindow.net.messages.GameInfoResponse;
import de.mkoehler.launchwindow.net.messages.ResolveTurnNowRequest;
import de.mkoehler.launchwindow.net.messages.ResolveTurnNowResponse;
import de.mkoehler.launchwindow.net.messages.RivalSummary;
import de.mkoehler.launchwindow.net.messages.RivalsRequest;
import de.mkoehler.launchwindow.net.messages.RivalsResponse;
import de.mkoehler.launchwindow.net.messages.TurnReportRequest;
import de.mkoehler.launchwindow.net.messages.TurnReportResponse;
import de.mkoehler.launchwindow.net.messages.HandshakeRequest;
import de.mkoehler.launchwindow.net.messages.HandshakeResponse;
import de.mkoehler.launchwindow.net.messages.SubmitActionRequest;
import de.mkoehler.launchwindow.net.messages.SubmitActionResponse;
import de.mkoehler.launchwindow.persistence.PlayerAction;
import de.mkoehler.launchwindow.persistence.SetFundsAction;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>Verifies the guarantees {@link MessageRegistry} relies on: registration
 * order is deterministic across independent {@link Kryo} instances (a
 * client and the server each own one), and every registered class survives
 * a serialize/deserialize round trip.</p>
 *
 * <p>When a new message class, or a new {@code model}/{@code persistence}
 * class reachable from one, is added to {@link MessageRegistry}, add it to
 * {@link #registrationIdsAreIdenticalAcrossIndependentKryoInstances()} and
 * give it a round-trip test.</p>
 *
 * @author Mario Koehler
 */
class MessageRegistryTest {

    /**
     * Two independent Kryo instances, registered by the same method, must
     * hand out identical ids for every registered class - otherwise a
     * client and a server would misread each other's messages.
     */
    @Test
    void registrationIdsAreIdenticalAcrossIndependentKryoInstances() {
        Kryo first = new Kryo();
        Kryo second = new Kryo();

        MessageRegistry.register(first);
        MessageRegistry.register(second);

        Class<?>[] registeredClasses = {
            Archetype.class, AssignmentState.class, AstronautSkill.class, SetSkill.class,
            MissionControlSkill.class, Astronaut.class, SetSpecialist.class, MissionControlOfficer.class,
            PlayerSnapshot.class, ArrayList.class, LinkedHashMap.class, SetFundsAction.class,
            SubmitActionRequest.class, SubmitActionResponse.class, FetchStateRequest.class,
            FetchStateResponse.class, HandshakeRequest.class, HandshakeResponse.class, TrainingKind.class,
            OwnedMissionComponent.class, InProgressMission.class, LinkedHashSet.class, RandomEventKind.class, NewsEntry.class};
        for (Class<?> registeredClass : registeredClasses) {
            assertEquals(first.getRegistration(registeredClass).getId(), second.getRegistration(registeredClass).getId(),
                "id mismatch for " + registeredClass.getSimpleName());
        }
    }

    /**
     * Every concrete {@link PlayerAction} subtype listed in its own
     * {@code @JsonSubTypes} annotation must also be registered with Kryo -
     * an action added to one registry but not the other would fail only
     * once actually sent over the wire, not at compile time.
     */
    @Test
    void everyPlayerActionSubtypeIsRegistered() {
        Kryo kryo = new Kryo();
        MessageRegistry.register(kryo);

        JsonSubTypes jsonSubTypes = PlayerAction.class.getAnnotation(JsonSubTypes.class);
        assertNotNull(jsonSubTypes);
        assertTrue(jsonSubTypes.value().length > 0);
        for (JsonSubTypes.Type subType : jsonSubTypes.value()) {
            assertNotNull(kryo.getClassResolver().getRegistration(subType.value()),
                subType.value().getSimpleName() + " is not registered with Kryo");
        }
    }

    /**
     * {@link HandshakeRequest} and {@link HandshakeResponse} must be
     * registered before every other class, so their ids stay stable
     * regardless of what else in the registry has drifted between two
     * builds - see {@link MessageRegistry}'s class Javadoc.
     */
    @Test
    void handshakeClassesAreRegisteredBeforeEverythingElse() {
        Kryo kryo = new Kryo();
        MessageRegistry.register(kryo);

        int requestId = kryo.getRegistration(HandshakeRequest.class).getId();
        int responseId = kryo.getRegistration(HandshakeResponse.class).getId();
        Class<?>[] everyOtherClass = {
            Archetype.class, AssignmentState.class, AstronautSkill.class, SetSkill.class,
            MissionControlSkill.class, Astronaut.class, SetSpecialist.class, MissionControlOfficer.class,
            PlayerSnapshot.class, ArrayList.class, LinkedHashMap.class, SetFundsAction.class,
            SubmitActionRequest.class, SubmitActionResponse.class, FetchStateRequest.class,
            FetchStateResponse.class, TrainingKind.class, OwnedMissionComponent.class, InProgressMission.class,
            LinkedHashSet.class, RandomEventKind.class, NewsEntry.class};
        for (Class<?> other : everyOtherClass) {
            int otherId = kryo.getRegistration(other).getId();
            assertTrue(requestId < otherId, "HandshakeRequest's id must be lower than " + other.getSimpleName());
            assertTrue(responseId < otherId, "HandshakeResponse's id must be lower than " + other.getSimpleName());
        }
    }

    /**
     * Verifies that {@code isProtocolMessage} accepts this project's own
     * message classes and rejects everything else, including {@code null}.
     */
    @Test
    void isProtocolMessageDistinguishesOwnMessagesFromAnythingElse() {
        assertTrue(MessageRegistry.isProtocolMessage(new FetchStateRequest()));
        assertFalse(MessageRegistry.isProtocolMessage("a plain string"));
        assertFalse(MessageRegistry.isProtocolMessage(null));
    }

    /**
     * A {@link HandshakeRequest} must keep its version and credentials
     * across the wire.
     */
    @Test
    void handshakeRequestSurvivesRoundTrip() {
        HandshakeRequest original = new HandshakeRequest("1.0", "mario", "hunter2");

        HandshakeRequest copy = roundTrip(original, HandshakeRequest.class);

        assertEquals("1.0", copy.clientVersion());
        assertEquals("mario", copy.login());
        assertEquals("hunter2", copy.password());
    }

    /**
     * A {@link HandshakeRequest}'s string representation must never contain
     * the plain-text password, so it is safe to appear in a log line.
     */
    @Test
    void handshakeRequestToStringRedactsThePassword() {
        HandshakeRequest request = new HandshakeRequest("1.0", "mario", "hunter2");

        assertFalse(request.toString().contains("hunter2"));
    }

    /**
     * A {@link HandshakeResponse} must keep its outcome across the wire,
     * for both an accepted and a rejected handshake.
     */
    @Test
    void handshakeResponseSurvivesRoundTrip() {
        HandshakeResponse accepted = roundTrip(HandshakeResponse.accept("Welcome.", "1.0"), HandshakeResponse.class);
        assertTrue(accepted.accepted());
        assertEquals("Welcome.", accepted.message());
        assertEquals("1.0", accepted.serverVersion());

        HandshakeResponse rejected =
            roundTrip(HandshakeResponse.reject("Version mismatch.", "2.0"), HandshakeResponse.class);
        assertFalse(rejected.accepted());
        assertEquals("Version mismatch.", rejected.message());
        assertEquals("2.0", rejected.serverVersion());
    }

    /**
     * A {@link SubmitActionRequest} must keep its action across the wire.
     */
    @Test
    void submitActionRequestSurvivesRoundTrip() {
        SubmitActionRequest original = new SubmitActionRequest(new SetFundsAction(5_000));

        SubmitActionRequest copy = roundTrip(original, SubmitActionRequest.class);

        assertEquals(5_000, ((SetFundsAction) copy.action()).getFunds());
    }

    /**
     * A {@link SubmitActionResponse} must keep its outcome across the wire,
     * for both a successful and a rejected submission.
     */
    @Test
    void submitActionResponseSurvivesRoundTrip() {
        SubmitActionResponse accepted = roundTrip(SubmitActionResponse.success("Accepted."), SubmitActionResponse.class);
        assertTrue(accepted.success());
        assertEquals("Accepted.", accepted.message());

        SubmitActionResponse rejected =
            roundTrip(SubmitActionResponse.failure("Not authenticated."), SubmitActionResponse.class);
        assertFalse(rejected.success());
        assertEquals("Not authenticated.", rejected.message());
    }

    /**
     * The game-info request and its reply must survive the wire.
     */
    @Test
    void devModeMessagesSurviveRoundTrip() {
        assertEquals(new ResolveTurnNowRequest(), roundTrip(new ResolveTurnNowRequest(), ResolveTurnNowRequest.class));
        assertEquals(ResolveTurnNowResponse.resolved(4), roundTrip(ResolveTurnNowResponse.resolved(4),
            ResolveTurnNowResponse.class));
        GameInfoResponse dev = GameInfoResponse.success("early-era", "ab12", List.of("goal-a"), 6, true, 1_000L, 900L,
            null, List.of(), List.of(), 0, true);
        assertTrue(roundTrip(dev, GameInfoResponse.class).devMode());
    }

    @Test
    void rivalMessagesSurviveRoundTrip() {
        assertEquals(new RivalsRequest(), roundTrip(new RivalsRequest(), RivalsRequest.class));
        // Built from immutable inputs, as the server does: the factories copy into registered collections.
        RivalsResponse rivals = RivalsResponse.of(List.of(
            RivalSummary.of("Zarya Design Bureau", "Nina Tarasova", Archetype.SOVIET, true,
                Map.of("earth-orbit-satellite", 12)),
            RivalSummary.of("Orion Spaceworks", "Ana Reyes", Archetype.NASA, false, Map.of())));
        assertEquals(rivals, roundTrip(rivals, RivalsResponse.class));
        assertEquals(RivalsResponse.failure("nope"), roundTrip(RivalsResponse.failure("nope"), RivalsResponse.class));
    }

    @Test
    void turnReportMessagesSurviveRoundTrip() throws Exception {
        assertEquals(new TurnReportRequest(4), roundTrip(new TurnReportRequest(4), TurnReportRequest.class));
        TurnReportResponse found = TurnReportResponse.of(4, "{\"gameTurn\":4}");
        TurnReportResponse back = roundTrip(found, TurnReportResponse.class);
        assertEquals(found, back);
        assertEquals("{\"gameTurn\":4}", back.json());
        assertEquals(TurnReportResponse.none(), roundTrip(TurnReportResponse.none(), TurnReportResponse.class));
    }

    @Test
    void gameInfoMessagesSurviveRoundTrip() {
        assertEquals(new GameInfoRequest(), roundTrip(new GameInfoRequest(), GameInfoRequest.class));
        // Built through the factory from immutable lists, as the server does: the factory must copy them into
        // the registered ArrayList, or Kryo refuses to write them.
        GameInfoResponse info = GameInfoResponse.success("early-era", "ab12", List.of("goal-a", "goal-b"), 6, true,
            1_000L, 900L, "WINNER", List.of("Orion"), List.of("A. Reyes"), 5);
        assertEquals(info, roundTrip(info, GameInfoResponse.class));
        assertEquals(BudgetSchedule.baspmClose(), roundTrip(info, GameInfoResponse.class).budgetSchedule(),
            "the game's budget schedule survives the wire");
        assertEquals(GameInfoResponse.failure("nope"), roundTrip(GameInfoResponse.failure("nope"), GameInfoResponse.class));
    }

    /**
     * A slot claim and its reply must survive the wire, a refusal's reason included.
     */
    @Test
    void claimSlotMessagesSurviveRoundTrip() {
        assertEquals(new ClaimSlotRequest(Archetype.SOVIET, "OKB-1", "Chief"),
            roundTrip(new ClaimSlotRequest(Archetype.SOVIET, "OKB-1", "Chief"), ClaimSlotRequest.class));
        assertEquals(ClaimSlotResponse.success("Welcome"),
            roundTrip(ClaimSlotResponse.success("Welcome"), ClaimSlotResponse.class));
        ClaimSlotResponse refusal = ClaimSlotResponse.failure(ClaimSlotResponse.Reason.DIRECTOR_NAME_TAKEN, "Taken");
        assertEquals(refusal, roundTrip(refusal, ClaimSlotResponse.class));
    }

    /**
     * Verifies the empty fetch request round-trips.
     */
    @Test
    void fetchStateRequestSurvivesRoundTrip() {
        assertNotNull(roundTrip(new FetchStateRequest(), FetchStateRequest.class));
    }

    /**
     * A {@link FetchStateResponse} must keep its full snapshot - including
     * every personnel roster and their skill maps - across the wire, and a
     * rejected response must keep {@code null} rather than substitute an
     * empty snapshot.
     */
    @Test
    void fetchStateResponseSurvivesRoundTrip() {
        PlayerSnapshot snapshot = new PlayerSnapshot("Hanseatic Space Agency", Archetype.SOVIET, 1_000, 0, 0, 1);
        Astronaut astronaut = new Astronaut("astro-1", "Jane Reyes", 34, 80, 60);
        astronaut.setSalary(42);
        astronaut.setSkill(AstronautSkill.PILOTING, 70.5);
        astronaut.setAgingProgressSeasons(2);
        astronaut.setTrainingKind(TrainingKind.ADVANCED);
        astronaut.setTrainingSeasonsCompleted(1);
        astronaut.setTrainingSeasonsTotal(2);
        astronaut.setTrainingTargetSkill(AstronautSkill.EVA);
        astronaut.setTrainingTargetLevel(85.5);
        astronaut.setAssignedMissionId("m0");
        astronaut.getExpertSkills().add(AstronautSkill.PILOTING);
        snapshot.setAstronauts(List.of(astronaut));
        SetSpecialist specialist = new SetSpecialist("set-1", "Omar Idris", 41, 75, 55);
        specialist.setSkill(SetSkill.ROCKETS, 63);
        specialist.setRdTargetComponentId("rocket-redstone");
        specialist.setRdSlot(0);
        snapshot.setSetSpecialists(List.of(specialist));
        MissionControlOfficer officer = new MissionControlOfficer("mco-1", "Alex Novak", 45, 70, 40);
        officer.setSkill(MissionControlSkill.PROPULSION, 50);
        officer.getExpertSkills().add(MissionControlSkill.PROPULSION);
        snapshot.setMissionControlOfficers(List.of(officer));
        Astronaut candidate = new Astronaut("astro-cand-1", "Priya Nair", 27, 65, 45);
        candidate.setSkill(AstronautSkill.LEADERSHIP, 33.0);
        snapshot.setAstronautCandidates(List.of(candidate));
        snapshot.setNextPersonnelId(7);
        snapshot.setOpenedProgramIds(List.of("sputnik-1"));
        OwnedMissionComponent closed = new OwnedMissionComponent(42.5, 17.0);
        closed.setActive(false);
        snapshot.setMissionComponents(Map.of("rocket-1", closed));
        // A null marks a slot no step uses; it must survive the round trip.
        InProgressMission flying = new InProgressMission("m0", "sputnik-1-std", List.of("astro-1"),
            Arrays.asList("mco-1", null, "astro-2"));
        flying.setRocketIds(List.of("r7-sputnik"));
        snapshot.setInProgressMissions(Map.of("m0", flying));
        snapshot.setNextMissionInstanceId(1);
        snapshot.setSuccessfulFlightCounts(Map.of("sputnik-1-std", 2));
        // M3 state: the first enum-keyed maps on the wire.
        snapshot.getBuildingLevels().put(Building.MISSION_CONTROL, 2);
        snapshot.getBuildingUpgradeSeasonsRemaining().put(Building.VEHICLE_ASSEMBLY, 3);
        snapshot.setPrestigeAtLastReview(700);
        snapshot.setNewsLog(List.of(new NewsEntry(3, RandomEventKind.EXTRA_FUNDING, null, 9.5)));
        snapshot.setLastFailureTurn(4);
        snapshot.setAchievedGoals(Map.of("orbit", 4));
        snapshot.setCancelledGoalIds(Set.of("pre-flight"));
        // M5 Museum state: the memorial (G10) and the goal records.
        snapshot.setMemorial(List.of(new MemorialEntry("Ivan Petrov", 31, 19, MemorialEntry.Cause.MISSION,
            "vostok-orbital", "reentry", "soviet_007")));
        snapshot.setGoalRecords(List.of(new GoalRecord("pre-flight", 4, "sputnik-1-std", "orbit", true, 30)));

        // Personalities (design.md 2.2(d)): traits, the hint, work seasons and pair rapport.
        astronaut.setTraits(List.of(new TraitState(Trait.WORKAHOLIC, 3, true), new TraitState(Trait.LONER, 2, false)));
        astronaut.setHint("driven");
        astronaut.setWorkSeasons(5);
        snapshot.setRapports(List.of(new Rapport("astro-1", "mco-1", 34.5, 0.4, 7, true)));

        FetchStateResponse copy = roundTrip(FetchStateResponse.success(snapshot), FetchStateResponse.class);
        assertEquals(snapshot.getAstronauts().get(0).getTraits(), copy.state().getAstronauts().get(0).getTraits());
        assertEquals("driven", copy.state().getAstronauts().get(0).getHint());
        assertEquals(5, copy.state().getAstronauts().get(0).getWorkSeasons());
        assertEquals(snapshot.getRapports(), copy.state().getRapports());
        assertEquals(snapshot.getMemorial(), copy.state().getMemorial());
        assertEquals(snapshot.getGoalRecords(), copy.state().getGoalRecords());

        assertTrue(copy.success());
        assertEquals("Hanseatic Space Agency", copy.state().getAgencyName());
        assertEquals(Archetype.SOVIET, copy.state().getArchetype());
        assertEquals(70.5, copy.state().getAstronauts().get(0).getSkill(AstronautSkill.PILOTING));
        assertEquals(2, copy.state().getAstronauts().get(0).getAgingProgressSeasons());
        assertEquals(TrainingKind.ADVANCED, copy.state().getAstronauts().get(0).getTrainingKind());
        assertEquals(1, copy.state().getAstronauts().get(0).getTrainingSeasonsCompleted());
        assertEquals(2, copy.state().getAstronauts().get(0).getTrainingSeasonsTotal());
        assertEquals(AstronautSkill.EVA, copy.state().getAstronauts().get(0).getTrainingTargetSkill());
        assertEquals(85.5, copy.state().getAstronauts().get(0).getTrainingTargetLevel());
        assertEquals(63, copy.state().getSetSpecialists().get(0).getSkill(SetSkill.ROCKETS));
        assertEquals("rocket-redstone", copy.state().getSetSpecialists().get(0).getRdTargetComponentId());
        assertEquals(0, copy.state().getSetSpecialists().get(0).getRdSlot());
        assertEquals(50, copy.state().getMissionControlOfficers().get(0).getSkill(MissionControlSkill.PROPULSION));
        assertEquals(Set.of(AstronautSkill.PILOTING), copy.state().getAstronauts().get(0).getExpertSkills());
        assertEquals(Set.of(MissionControlSkill.PROPULSION),
            copy.state().getMissionControlOfficers().get(0).getExpertSkills());
        assertEquals(1, copy.state().getAstronautCandidates().size());
        assertEquals("astro-cand-1", copy.state().getAstronautCandidates().get(0).getId());
        assertEquals(33.0, copy.state().getAstronautCandidates().get(0).getSkill(AstronautSkill.LEADERSHIP));
        assertEquals(7, copy.state().getNextPersonnelId());
        assertEquals(List.of("sputnik-1"), copy.state().getOpenedProgramIds());
        assertEquals(42.5, copy.state().getMissionComponents().get("rocket-1").getCurrentReliability());
        assertEquals(17.0, copy.state().getMissionComponents().get("rocket-1").getFloorReliability());
        assertEquals("m0", copy.state().getAstronauts().get(0).getAssignedMissionId());
        assertEquals(1, copy.state().getInProgressMissions().size());
        InProgressMission copiedMission = copy.state().getInProgressMissions().get("m0");
        assertEquals("sputnik-1-std", copiedMission.getMissionConfigurationId());
        assertEquals(List.of("astro-1"), copiedMission.getCrewSlotIds());
        assertEquals(Arrays.asList("mco-1", null, "astro-2"), copiedMission.getControlSlotIds());
        assertEquals(List.of("r7-sputnik"), copiedMission.getRocketIds());
        assertEquals(false, copy.state().getMissionComponents().get("rocket-1").isActive());
        assertEquals(2, copy.state().buildingLevel(Building.MISSION_CONTROL));
        assertEquals(1, copy.state().buildingLevel(Building.HEADQUARTERS));
        assertEquals(Map.of(Building.VEHICLE_ASSEMBLY, 3), copy.state().getBuildingUpgradeSeasonsRemaining());
        assertEquals(2, copy.state().getSuccessfulFlightCounts().get("sputnik-1-std"));
        assertEquals(700, copy.state().getPrestigeAtLastReview());
        assertEquals(new NewsEntry(3, RandomEventKind.EXTRA_FUNDING, null, 9.5), copy.state().getNewsLog().get(0));
        assertEquals(4, copy.state().getLastFailureTurn());
        assertEquals(42, copy.state().getAstronauts().get(0).getSalary());
        assertEquals(4, copy.state().getAchievedGoals().get("orbit"));
        assertEquals(Set.of("pre-flight"), copy.state().getCancelledGoalIds());
        assertEquals(1, copy.state().getNextMissionInstanceId());

        FetchStateResponse rejected = roundTrip(FetchStateResponse.failure("Not authenticated."), FetchStateResponse.class);
        assertFalse(rejected.success());
        assertNull(rejected.state());
    }

    /**
     * Serializes {@code original} with a freshly registered {@link Kryo}
     * and reads it back with a second, independently registered one,
     * mimicking one message travelling from one endpoint to the other.
     *
     * @param original the message to send
     * @param type     the message's class
     * @param <T>      the message type
     * @return the deserialized copy
     */
    private static <T> T roundTrip(T original, Class<T> type) {
        Kryo sender = new Kryo();
        MessageRegistry.register(sender);
        Kryo receiver = new Kryo();
        MessageRegistry.register(receiver);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (Output output = new Output(bytes)) {
            sender.writeObject(output, original);
        }
        try (Input input = new Input(bytes.toByteArray())) {
            return receiver.readObject(input, type);
        }
    }
}
