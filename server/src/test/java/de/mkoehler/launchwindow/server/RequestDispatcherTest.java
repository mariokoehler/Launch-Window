package de.mkoehler.launchwindow.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.net.AppVersion;
import de.mkoehler.launchwindow.net.messages.FetchStateRequest;
import de.mkoehler.launchwindow.net.messages.FetchStateResponse;
import de.mkoehler.launchwindow.net.messages.HandshakeRequest;
import de.mkoehler.launchwindow.net.messages.HandshakeResponse;
import de.mkoehler.launchwindow.net.messages.SubmitActionRequest;
import de.mkoehler.launchwindow.net.messages.SubmitActionResponse;
import de.mkoehler.launchwindow.persistence.SetFundsAction;
import de.mkoehler.launchwindow.server.accounts.AccountStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>Tests for {@link RequestDispatcher}, entirely without real sockets -
 * see {@code ServerClientIntegrationTest} for the real-socket proof.</p>
 *
 * @author Mario Koehler
 */
class RequestDispatcherTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final List<AccountStore> stores = new ArrayList<>();

    /**
     * Creates an account store and registers it for teardown.
     *
     * @param dataRoot the data root to back it with
     * @return the new store
     */
    private AccountStore createAccountStore(Path dataRoot) {
        AccountStore store = new AccountStore(dataRoot.resolve("accounts.json"));
        stores.add(store);
        return store;
    }

    /**
     * Closes every account store this test created, so no background
     * thread leaks across the test run.
     */
    @AfterEach
    void closeStores() {
        stores.forEach(AccountStore::close);
        stores.clear();
    }

    /**
     * Verifies that a version mismatch is rejected before the account
     * store is ever touched - no account gets created.
     */
    @Test
    void versionMismatchIsRejectedAndCreatesNoAccount(@TempDir Path dataRoot) {
        AccountStore accountStore = createAccountStore(dataRoot);
        RequestDispatcher dispatcher = new RequestDispatcher(accountStore, dataRoot, mapper);

        HandshakeResponse response =
            dispatcher.handleHandshake(1, new HandshakeRequest("not-a-real-version", "mario", "hunter2", "Agency"));

        assertFalse(response.accepted());
        assertTrue(accountStore.findByLogin("mario").isEmpty());
        assertFalse(dispatcher.isAuthenticated(1));
    }

    /**
     * Verifies that the wrong password for an existing account is
     * rejected.
     */
    @Test
    void wrongPasswordIsRejected(@TempDir Path dataRoot) {
        AccountStore accountStore = createAccountStore(dataRoot);
        RequestDispatcher dispatcher = new RequestDispatcher(accountStore, dataRoot, mapper);
        dispatcher.handleHandshake(1, new HandshakeRequest(AppVersion.getVersion(), "mario", "hunter2", "Agency"));

        HandshakeResponse response =
            dispatcher.handleHandshake(2, new HandshakeRequest(AppVersion.getVersion(), "mario", "wrong", "Agency"));

        assertFalse(response.accepted());
        assertFalse(dispatcher.isAuthenticated(2));
    }

    /**
     * Verifies that submitting an action on a connection that never
     * completed a handshake is rejected.
     */
    @Test
    void submitBeforeHandshakeIsRejected(@TempDir Path dataRoot) {
        RequestDispatcher dispatcher = new RequestDispatcher(createAccountStore(dataRoot), dataRoot, mapper);

        SubmitActionResponse response = dispatcher.handleSubmitAction(1, new SubmitActionRequest(new SetFundsAction(1_000)));

        assertFalse(response.success());
    }

    /**
     * Verifies that fetching state on a connection that never completed a
     * handshake is rejected.
     */
    @Test
    void fetchBeforeHandshakeIsRejected(@TempDir Path dataRoot) {
        RequestDispatcher dispatcher = new RequestDispatcher(createAccountStore(dataRoot), dataRoot, mapper);

        FetchStateResponse response = dispatcher.handleFetchState(1, new FetchStateRequest());

        assertFalse(response.success());
    }

    /**
     * Verifies that a successful handshake creates the placeholder turn-0
     * snapshot, and that a second handshake for the same account does not
     * recreate or reset it.
     */
    @Test
    void bootstrapSnapshotIsCreatedExactlyOnce(@TempDir Path dataRoot) {
        AccountStore accountStore = createAccountStore(dataRoot);
        RequestDispatcher dispatcher = new RequestDispatcher(accountStore, dataRoot, mapper);
        dispatcher.handleHandshake(1, new HandshakeRequest(AppVersion.getVersion(), "mario", "hunter2", "Agency"));
        dispatcher.handleSubmitAction(1, new SubmitActionRequest(new SetFundsAction(5_000)));

        dispatcher.handleHandshake(2, new HandshakeRequest(AppVersion.getVersion(), "mario", "hunter2", "Agency"));
        FetchStateResponse response = dispatcher.handleFetchState(2, new FetchStateRequest());

        assertTrue(response.success());
        assertEquals(5_000, response.state().getFunds(), "the second handshake must not have reset the bootstrap snapshot");
    }

    /**
     * Verifies the whole authenticated flow: handshake, then submit, then
     * fetch reflects the submitted action.
     */
    @Test
    void submitThenFetchReturnsTheSubmittedAction(@TempDir Path dataRoot) {
        AccountStore accountStore = createAccountStore(dataRoot);
        RequestDispatcher dispatcher = new RequestDispatcher(accountStore, dataRoot, mapper);
        HandshakeResponse handshake =
            dispatcher.handleHandshake(1, new HandshakeRequest(AppVersion.getVersion(), "mario", "hunter2", "Agency"));
        assertTrue(handshake.accepted());

        SubmitActionResponse submitResponse = dispatcher.handleSubmitAction(1, new SubmitActionRequest(new SetFundsAction(2_500)));
        FetchStateResponse fetchResponse = dispatcher.handleFetchState(1, new FetchStateRequest());

        assertTrue(submitResponse.success());
        assertTrue(fetchResponse.success());
        assertEquals(2_500, fetchResponse.state().getFunds());
    }

    /**
     * Verifies that a disconnect clears a connection's authentication, so
     * a later request reusing that connection id is rejected again.
     */
    @Test
    void disconnectClearsTheConnectionsAuthentication(@TempDir Path dataRoot) {
        AccountStore accountStore = createAccountStore(dataRoot);
        RequestDispatcher dispatcher = new RequestDispatcher(accountStore, dataRoot, mapper);
        dispatcher.handleHandshake(1, new HandshakeRequest(AppVersion.getVersion(), "mario", "hunter2", "Agency"));
        assertTrue(dispatcher.isAuthenticated(1));

        dispatcher.handleDisconnect(1);

        assertFalse(dispatcher.isAuthenticated(1));
        assertFalse(dispatcher.handleFetchState(1, new FetchStateRequest()).success());
    }

    /**
     * Verifies that {@code handle} reports a rejected handshake as one to
     * close the connection over, and an accepted one as one to keep open.
     */
    @Test
    void handleReportsCloseConnectionOnlyForARejectedHandshake(@TempDir Path dataRoot) {
        AccountStore accountStore = createAccountStore(dataRoot);
        RequestDispatcher dispatcher = new RequestDispatcher(accountStore, dataRoot, mapper);

        RequestDispatcher.DispatchResult rejected =
            dispatcher.handle(1, new HandshakeRequest("not-a-real-version", "mario", "hunter2", "Agency"));
        assertTrue(rejected.closeConnection());
        assertFalse(((HandshakeResponse) rejected.response()).accepted());

        RequestDispatcher.DispatchResult accepted =
            dispatcher.handle(2, new HandshakeRequest(AppVersion.getVersion(), "mario", "hunter2", "Agency"));
        assertFalse(accepted.closeConnection());
        assertTrue(((HandshakeResponse) accepted.response()).accepted());
    }

    /**
     * Verifies that {@code handle} routes {@link SubmitActionRequest} and
     * {@link FetchStateRequest} the same way their own dedicated methods
     * do, and rejects any other message type.
     */
    @Test
    void handleRoutesKnownMessageTypesAndRejectsUnknownOnes(@TempDir Path dataRoot) {
        AccountStore accountStore = createAccountStore(dataRoot);
        RequestDispatcher dispatcher = new RequestDispatcher(accountStore, dataRoot, mapper);
        dispatcher.handle(1, new HandshakeRequest(AppVersion.getVersion(), "mario", "hunter2", "Agency"));

        RequestDispatcher.DispatchResult submitResult = dispatcher.handle(1, new SubmitActionRequest(new SetFundsAction(1_500)));
        assertFalse(submitResult.closeConnection());
        assertTrue(((SubmitActionResponse) submitResult.response()).success());

        RequestDispatcher.DispatchResult fetchResult = dispatcher.handle(1, new FetchStateRequest());
        assertFalse(fetchResult.closeConnection());
        assertEquals(1_500, ((FetchStateResponse) fetchResult.response()).state().getFunds());

        assertThrows(IllegalArgumentException.class, () -> dispatcher.handle(1, "not a real message"));
    }
}
