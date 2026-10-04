package de.mkoehler.launchwindow.client.connect;

import com.badlogic.gdx.Gdx;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.net.AppVersion;
import de.mkoehler.launchwindow.net.NetworkClient;
import de.mkoehler.launchwindow.net.messages.ClaimSlotRequest;
import de.mkoehler.launchwindow.net.messages.ClaimSlotResponse;
import de.mkoehler.launchwindow.net.messages.FetchStateRequest;
import de.mkoehler.launchwindow.net.messages.FetchStateResponse;
import de.mkoehler.launchwindow.net.messages.HandshakeRequest;
import de.mkoehler.launchwindow.net.messages.HandshakeResponse;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The real {@link ServerGateway}: each call runs on one background thread, opens its own
 * short-lived KryoNet connection ({@code design.md} &sect;3.3), authenticates, does its work,
 * closes, and posts the result back to the render thread. Calls run one at a time, in order.
 *
 * @author Mario Koehler
 */
public final class NetworkGateway implements ServerGateway {

    private final ExecutorService worker = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "server-gateway");
        thread.setDaemon(true);
        return thread;
    });

    /**
     * Creates the gateway and its background thread.
     */
    public NetworkGateway() {
    }

    @Override
    public void logIn(Credentials credentials, Consumer<LoginResult> done) {
        run(() -> withConnection(credentials, client -> {
            Optional<LoginResult> refusal = handshake(client, credentials);
            if (refusal.isPresent()) {
                return refusal.get();
            }
            return LoginResult.fromFetch(client.sendAndReceive(new FetchStateRequest(), FetchStateResponse.class));
        }, message -> LoginResult.failure(LoginResult.Kind.UNREACHABLE, message),
            message -> LoginResult.failure(LoginResult.Kind.FAILED, message)), done);
    }

    @Override
    public void claimSlot(Credentials credentials, Archetype archetype, String agencyName, String directorName,
                          Consumer<ClaimResult> done) {
        run(() -> withConnection(credentials, client -> {
            Optional<LoginResult> refusal = handshake(client, credentials);
            if (refusal.isPresent()) {
                return ClaimResult.failure(ClaimResult.Kind.LOGIN_FAILED, loginFailureText(refusal.get()));
            }
            ClaimSlotResponse claim = client.sendAndReceive(
                new ClaimSlotRequest(archetype, agencyName, directorName), ClaimSlotResponse.class);
            if (!claim.success()) {
                return ClaimResult.refused(claim);
            }
            FetchStateResponse fetch = client.sendAndReceive(new FetchStateRequest(), FetchStateResponse.class);
            return fetch.success() ? ClaimResult.joined(fetch.state())
                : ClaimResult.failure(ClaimResult.Kind.FAILED, fetch.message());
        }, message -> ClaimResult.failure(ClaimResult.Kind.UNREACHABLE, message),
            message -> ClaimResult.failure(ClaimResult.Kind.FAILED, message)), done);
    }

    @Override
    public void dispose() {
        worker.shutdownNow();
    }

    private static Optional<LoginResult> handshake(NetworkClient client, Credentials credentials) throws IOException {
        HandshakeResponse response = client.sendAndReceive(
            new HandshakeRequest(AppVersion.getVersion(), credentials.login(), credentials.password()),
            HandshakeResponse.class);
        return LoginResult.fromHandshake(response, AppVersion.getVersion());
    }

    private static String loginFailureText(LoginResult result) {
        if (result.kind() == LoginResult.Kind.VERSION_MISMATCH) {
            return "The server is now on version " + result.serverVersion() + ". Update the client and connect again.";
        }
        return result.message();
    }

    /**
     * Opens a connection, runs the exchange on it, and always closes it.
     *
     * @param credentials   where to connect
     * @param exchange      the work, on the open connection
     * @param ifUnreachable the result when no connection can be made
     * @param ifBroken      the result when the exchange breaks off
     * @param <T>           the result type
     * @return the result
     */
    private static <T> T withConnection(Credentials credentials, Exchange<T> exchange,
                                        Function<String, T> ifUnreachable, Function<String, T> ifBroken) {
        try (NetworkClient client = new NetworkClient()) {
            try {
                client.connect(credentials.address().host(), credentials.address().port());
            } catch (IOException | RuntimeException e) {
                return ifUnreachable.apply(e.getMessage());
            }
            return exchange.run(client);
        } catch (IOException | RuntimeException e) {
            return ifBroken.apply(e.getMessage());
        }
    }

    private <T> void run(Supplier<T> work, Consumer<T> done) {
        worker.execute(() -> {
            T result = work.get();
            Gdx.app.postRunnable(() -> done.accept(result));
        });
    }

    /**
     * Work done on an open, not yet authenticated connection.
     *
     * @param <T> the result type
     */
    @FunctionalInterface
    private interface Exchange<T> {
        /**
         * Runs the work.
         *
         * @param client the connection
         * @return the result
         * @throws IOException if the exchange breaks off
         */
        T run(NetworkClient client) throws IOException;
    }
}
