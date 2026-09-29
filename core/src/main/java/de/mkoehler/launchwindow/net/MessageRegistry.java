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
import de.mkoehler.launchwindow.net.messages.FetchStateRequest;
import de.mkoehler.launchwindow.net.messages.FetchStateResponse;
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
 * <p>The order is append-only: a new class, whichever package it comes
 * from, is added as a new line at the end of {@link #register(Kryo)}.
 * Existing lines are never reordered, removed, or grouped by package -
 * inserting a new line in the middle of an existing block would shift
 * every registration id after it and silently break the wire format for
 * anyone running a different build on the other end.</p>
 *
 * @author Mario Koehler
 */
public final class MessageRegistry {

    /**
     * Not instantiable; this class only exposes a static method.
     */
    private MessageRegistry() {
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
        // Wire messages themselves.
        kryo.register(SubmitActionRequest.class);
        kryo.register(SubmitActionResponse.class);
        kryo.register(FetchStateRequest.class);
        kryo.register(FetchStateResponse.class);
    }
}
