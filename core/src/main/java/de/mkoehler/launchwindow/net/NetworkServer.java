package de.mkoehler.launchwindow.net;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * <p>The server end of the connection: a thin wrapper around a KryoNet
 * {@link Server} (TCP only, see {@code design.md} &sect;3.3) that registers
 * the wire messages and keeps KryoNet's own threads away from the rest of
 * the program.</p>
 *
 * <p><b>Threading contract.</b> KryoNet calls its listener on its own
 * network threads. This class only <em>enqueues</em> what happens there;
 * the owner of the server loop calls {@link #poll(Handler)} from
 * <em>its</em> thread to receive the events one by one, so everything
 * downstream (in particular, the request dispatcher) is single-threaded
 * and needs no locking of its own. {@link #send(int, Object)} and
 * {@link #close(int)} may be called from the polling thread as usual.</p>
 *
 * <p>Connections are identified by KryoNet's integer connection id, which
 * is unique for the lifetime of the server process.</p>
 *
 * @author Mario Koehler
 */
public final class NetworkServer {

    /**
     * Receives the events {@link NetworkServer#poll(Handler)} drains from
     * the network threads.
     */
    public interface Handler {

        /**
         * A message arrived from a client.
         *
         * @param connectionId the connection it arrived on
         * @param message      the message
         */
        void onMessage(int connectionId, Object message);

        /**
         * A connection ended, whether closed by the client, by
         * {@link NetworkServer#close(int)}, or by a timeout or error.
         * Called once per connection.
         *
         * @param connectionId the connection that ended
         */
        void onDisconnect(int connectionId);
    }

    /**
     * Something that happened on a network thread and is waiting to be
     * handled on the polling thread.
     *
     * @param connectionId the connection concerned
     * @param message      the received message, or {@code null} for a
     *                     disconnect
     */
    private record Event(int connectionId, Object message) {
    }

    private final Server server;
    private final ConcurrentLinkedQueue<Event> inbox = new ConcurrentLinkedQueue<>();
    /** Connection id to the {@link System#nanoTime()} after which it may be closed. */
    private final Map<Integer, Long> pendingCloses = new ConcurrentHashMap<>();

    /**
     * How long {@link #sendThenClose(int, Object)} waits before closing, so the message can reach
     * the client first. Measured in time, not in polls: the server polls many times a second.
     */
    static final long CLOSE_DELAY_NANOS = 500_000_000L;

    /**
     * Creates a server that is not listening yet; call {@link #start(int)}.
     */
    public NetworkServer() {
        this.server = new Server(NetworkConstants.WRITE_BUFFER_SIZE, NetworkConstants.OBJECT_BUFFER_SIZE);
        MessageRegistry.register(server.getKryo());
        server.addListener(new Listener() {
            @Override
            public void received(Connection connection, Object object) {
                if (MessageRegistry.isProtocolMessage(object)) {
                    inbox.add(new Event(connection.getID(), object));
                }
            }

            @Override
            public void disconnected(Connection connection) {
                inbox.add(new Event(connection.getID(), null));
            }
        });
    }

    /**
     * Starts the network threads and begins listening.
     *
     * @param tcpPort the TCP port to listen on
     * @throws IOException if the port cannot be bound
     */
    public void start(int tcpPort) throws IOException {
        server.start();
        server.bind(tcpPort);
    }

    /**
     * Stops the network threads and closes every connection.
     */
    public void stop() {
        server.stop();
    }

    /**
     * Hands every event that has arrived since the last call to a handler,
     * in arrival order. Also closes every connection queued by
     * {@link #sendThenClose(int, Object)} whose delay has passed, before
     * handing over any new events - see that method for why the close is
     * deferred rather than immediate. Call this regularly from the thread
     * that owns the server state.
     *
     * @param handler where the events go
     */
    public void poll(Handler handler) {
        if (!pendingCloses.isEmpty()) {
            long now = System.nanoTime();
            for (Map.Entry<Integer, Long> pending : pendingCloses.entrySet()) {
                if (now - pending.getValue() >= 0) {
                    pendingCloses.remove(pending.getKey());
                    close(pending.getKey());
                }
            }
        }
        Event event;
        while ((event = inbox.poll()) != null) {
            if (event.message() == null) {
                handler.onDisconnect(event.connectionId());
            } else {
                handler.onMessage(event.connectionId(), event.message());
            }
        }
    }

    /**
     * Sends a message to one client. Silently does nothing if that
     * connection no longer exists.
     *
     * @param connectionId the connection to send on
     * @param message      the message, one of the classes registered in
     *                     {@link MessageRegistry}
     */
    public void send(int connectionId, Object message) {
        server.sendToTCP(connectionId, message);
    }

    /**
     * Sends a message to one client, then closes the connection - on the
     * first call to {@link #poll(Handler)} at least {@link #CLOSE_DELAY_NANOS}
     * later, not immediately. Closing a connection can drop a write still in
     * flight, so an immediate close right after {@link #send(int, Object)}
     * risks the client never actually receiving this message (e.g. a
     * rejected handshake's reason); the delay gives the underlying socket a
     * chance to flush it first.
     *
     * @param connectionId the connection to send on, then close
     * @param message      the message, one of the classes registered in
     *                     {@link MessageRegistry}
     */
    public void sendThenClose(int connectionId, Object message) {
        send(connectionId, message);
        pendingCloses.put(connectionId, System.nanoTime() + CLOSE_DELAY_NANOS);
    }

    /**
     * Closes one connection. Its {@link Handler#onDisconnect(int)} event
     * follows like for any other end of a connection.
     *
     * @param connectionId the connection to close
     */
    public void close(int connectionId) {
        for (Connection connection : server.getConnections()) {
            if (connection.getID() == connectionId) {
                connection.close();
                return;
            }
        }
    }
}
