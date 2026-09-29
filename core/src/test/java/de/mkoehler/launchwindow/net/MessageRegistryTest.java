package de.mkoehler.launchwindow.net;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.AstronautSkill;
import de.mkoehler.launchwindow.model.MissionControlOfficer;
import de.mkoehler.launchwindow.model.MissionControlSkill;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSkill;
import de.mkoehler.launchwindow.model.SetSpecialist;
import de.mkoehler.launchwindow.net.messages.FetchStateRequest;
import de.mkoehler.launchwindow.net.messages.FetchStateResponse;
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
import java.util.List;

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
            FetchStateResponse.class, HandshakeRequest.class, HandshakeResponse.class};
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
            FetchStateResponse.class};
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
        HandshakeRequest original = new HandshakeRequest("1.0", "mario", "hunter2", "Hanseatic Space Agency");

        HandshakeRequest copy = roundTrip(original, HandshakeRequest.class);

        assertEquals("1.0", copy.clientVersion());
        assertEquals("mario", copy.login());
        assertEquals("hunter2", copy.password());
        assertEquals("Hanseatic Space Agency", copy.displayName());
    }

    /**
     * A {@link HandshakeRequest}'s string representation must never contain
     * the plain-text password, so it is safe to appear in a log line.
     */
    @Test
    void handshakeRequestToStringRedactsThePassword() {
        HandshakeRequest request = new HandshakeRequest("1.0", "mario", "hunter2", "Hanseatic Space Agency");

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
     * A {@link FetchStateRequest} must survive the wire - it carries no
     * fields, so this just proves the empty record itself round-trips.
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
        astronaut.setSkill(AstronautSkill.PILOTING, 70);
        snapshot.setAstronauts(List.of(astronaut));
        SetSpecialist specialist = new SetSpecialist("set-1", "Omar Idris", 41, 75, 55);
        specialist.setSkill(SetSkill.ROCKETS, 63);
        snapshot.setSetSpecialists(List.of(specialist));
        MissionControlOfficer officer = new MissionControlOfficer("mco-1", "Alex Novak", 45, 70, 40);
        officer.setSkill(MissionControlSkill.PROPULSION, 50);
        snapshot.setMissionControlOfficers(List.of(officer));

        FetchStateResponse copy = roundTrip(FetchStateResponse.success(snapshot), FetchStateResponse.class);

        assertTrue(copy.success());
        assertEquals("Hanseatic Space Agency", copy.state().getAgencyName());
        assertEquals(Archetype.SOVIET, copy.state().getArchetype());
        assertEquals(70, copy.state().getAstronauts().get(0).getSkill(AstronautSkill.PILOTING));
        assertEquals(63, copy.state().getSetSpecialists().get(0).getSkill(SetSkill.ROCKETS));
        assertEquals(50, copy.state().getMissionControlOfficers().get(0).getSkill(MissionControlSkill.PROPULSION));

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
