package de.mkoehler.launchwindow.net;

import com.esotericsoftware.kryo.Kryo;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.AstronautSkill;
import de.mkoehler.launchwindow.model.MissionControlOfficer;
import de.mkoehler.launchwindow.model.MissionControlSkill;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSkill;
import de.mkoehler.launchwindow.model.SetSpecialist;
import de.mkoehler.launchwindow.model.TrainingKind;
import de.mkoehler.launchwindow.net.messages.FetchStateRequest;
import de.mkoehler.launchwindow.net.messages.FetchStateResponse;
import de.mkoehler.launchwindow.net.messages.HandshakeRequest;
import de.mkoehler.launchwindow.net.messages.HandshakeResponse;
import de.mkoehler.launchwindow.net.messages.SubmitActionRequest;
import de.mkoehler.launchwindow.net.messages.SubmitActionResponse;
import de.mkoehler.launchwindow.persistence.SetFundsAction;

import java.util.ArrayList;
import java.util.LinkedHashMap;

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
    }
}
