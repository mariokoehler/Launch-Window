package de.mkoehler.launchwindow.net;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * <p>The client end of the connection: a thin wrapper around a KryoNet
 * {@link Client} (TCP only, see {@code design.md} &sect;3.3), shaped for
 * this project's usage - connect, send one request, block for its one
 * reply, disconnect - not for a long-held, continuously-polled session the
 * way the sibling projects' own {@code NetworkClient} is. There is exactly
 * one request in flight at a time per connection, so a simple blocking
 * queue is enough: no correlation id is needed to match a reply to its
 * request.</p>
 *
 * @author Mario Koehler
 */
public final class NetworkClient implements AutoCloseable {

    /**
     * Enqueued in place of a message when the connection ends, so a
     * blocked {@link #sendAndReceive(Object, Class)} wakes up and fails
     * immediately instead of waiting out the full response timeout.
     */
    private static final Object DISCONNECTED = new Object();

    private final Client client;
    private final BlockingQueue<Object> inbox = new LinkedBlockingQueue<>();
    private volatile java.util.function.Consumer<Object> noticeListener = notice -> { };

    /**
     * Creates a client that is not connected yet; call
     * {@link #connect(String, int)}.
     */
    public NetworkClient() {
        this.client = new Client(NetworkConstants.WRITE_BUFFER_SIZE, NetworkConstants.OBJECT_BUFFER_SIZE);
        MessageRegistry.register(client.getKryo());
        client.addListener(new Listener() {
            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof de.mkoehler.launchwindow.net.messages.TurnResolvedNotice) {
                    // Unasked: never a reply, so it must not be taken for one.
                    noticeListener.accept(object);
                } else if (MessageRegistry.isProtocolMessage(object)) {
                    inbox.add(object);
                }
            }

            @Override
            public void disconnected(Connection connection) {
                inbox.add(DISCONNECTED);
            }
        });
    }

    /**
     * Connects to a server. Blocks until the connection is established or
     * fails.
     *
     * @param host    the server's host name or address
     * @param tcpPort the server's TCP port
     * @throws IOException if the connection could not be established within
     *                     {@link NetworkConstants#CONNECTION_TIMEOUT_MILLIS}
     */
    public void connect(String host, int tcpPort) throws IOException {
        client.start();
        try {
            client.connect(NetworkConstants.CONNECTION_TIMEOUT_MILLIS, host, tcpPort);
        } catch (IOException | RuntimeException e) {
            client.stop();
            throw e;
        }
    }

    /**
     * Sends one request and blocks for its one reply.
     *
     * @param request      the request to send, one of the classes registered
     *                     in {@link MessageRegistry}
     * @param responseType the expected response type
     * @param <T>          the response type
     * @return the response
     * @throws IOException if the connection ends before a response arrives,
     *                      no response arrives within
     *                      {@link NetworkConstants#RESPONSE_TIMEOUT_MILLIS},
     *                      or the response is not an instance of
     *                      {@code responseType}
     */
    public <T> T sendAndReceive(Object request, Class<T> responseType) throws IOException {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(responseType, "responseType must not be null");
        // Discards anything left over from a previous call that itself timed out - a reply
        // that arrives after its own sendAndReceive gave up must never be mistaken for the
        // answer to this, later, call.
        inbox.clear();
        client.sendTCP(request);
        Object response;
        try {
            response = inbox.poll(NetworkConstants.RESPONSE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for a response", e);
        }
        if (response == DISCONNECTED) {
            throw new IOException("Connection closed before a response arrived");
        }
        if (response == null) {
            throw new IOException(
                "No response within " + NetworkConstants.RESPONSE_TIMEOUT_MILLIS + "ms");
        }
        if (!responseType.isInstance(response)) {
            throw new IOException(
                "Expected " + responseType.getSimpleName() + " but got " + response.getClass().getSimpleName());
        }
        return responseType.cast(response);
    }

    /**
     * Sets who hears the server's unasked notices. Called on KryoNet's network thread.
     *
     * @param listener the listener
     */
    public void setNoticeListener(java.util.function.Consumer<Object> listener) {
        this.noticeListener = Objects.requireNonNull(listener, "listener must not be null");
    }

    /**
     * Returns whether the connection is currently up.
     *
     * @return {@code true} while connected
     */
    public boolean isConnected() {
        return client.isConnected();
    }

    /**
     * Closes the connection and stops the network thread. Safe to call
     * repeatedly, including on a client that never connected.
     */
    @Override
    public void close() {
        client.stop();
    }
}
