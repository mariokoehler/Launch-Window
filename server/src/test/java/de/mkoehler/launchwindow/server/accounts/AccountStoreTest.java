package de.mkoehler.launchwindow.server.accounts;

import de.mkoehler.launchwindow.net.messages.HandshakeResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>Tests for {@link AccountStore}, including its deferred-write behavior:
 * {@link AccountStore#login} only ever mutates the in-memory state, so a
 * persisted-to-disk assertion needs an explicit {@link AccountStore#flush()}
 * first, not just a mutation - see {@link #mutationsAreNotOnDiskUntilFlushed}.</p>
 *
 * @author Mario Koehler
 */
class AccountStoreTest {

    // Every store this test class creates, closed in tearDown() - AccountStore starts a
    // background scheduler thread and registers a JVM shutdown hook per instance, both of
    // which would otherwise leak across the whole test run.
    private final List<AccountStore> stores = new ArrayList<>();

    /**
     * Creates a store backed by the given file and registers it for
     * teardown.
     *
     * @param file the JSON file to back the store with
     * @return the new store
     */
    private AccountStore createStore(Path file) {
        AccountStore store = new AccountStore(file);
        stores.add(store);
        return store;
    }

    /**
     * Closes every store created by this test, so no background thread or
     * shutdown hook leaks across the test run.
     */
    @AfterEach
    void closeStores() {
        stores.forEach(AccountStore::close);
        stores.clear();
    }

    /**
     * Verifies that a newly created account is assigned a non-blank id.
     */
    @Test
    void newLoginIsAssignedAnId(@TempDir Path tempDir) {
        AccountStore store = createStore(tempDir.resolve("accounts.json"));

        AuthResult result = store.create("mario", "hunter2");

        assertFalse(result.account().getId() == null || result.account().getId().isBlank());
    }

    /**
     * Verifies that an account's id stays the same across repeated logins.
     */
    @Test
    void idIsStableAcrossRepeatedLogins(@TempDir Path tempDir) {
        AccountStore store = createStore(tempDir.resolve("accounts.json"));
        String firstId = store.create("mario", "hunter2").account().getId();

        String secondId = store.login("mario", "hunter2").account().getId();

        assertEquals(firstId, secondId);
    }

    /**
     * Verifies that logging in with a login nobody has is refused and makes no account: a typo must
     * never become a second account (review.md 2.3).
     */
    @Test
    void anUnknownLoginIsRefusedAndMakesNoAccount(@TempDir Path tempDir) {
        AccountStore store = createStore(tempDir.resolve("accounts.json"));

        AuthResult result = store.login("mario", "hunter2");

        assertFalse(result.success());
        assertEquals(HandshakeResponse.NO_SUCH_ACCOUNT, result.message());
        assertTrue(store.findByLogin("mario").isEmpty());
    }

    /**
     * Verifies that creating an account makes it, and that it then logs in.
     */
    @Test
    void createMakesTheAccount(@TempDir Path tempDir) {
        AccountStore store = createStore(tempDir.resolve("accounts.json"));

        AuthResult result = store.create("mario", "hunter2");

        assertTrue(result.success());
        assertEquals("mario", result.account().getLogin());
        assertTrue(store.login("mario", "hunter2").success());
    }

    /**
     * Verifies that creating a login that exists is refused with another password and logs in with its
     * own, so a create whose answer was lost can be sent again.
     */
    @Test
    void createOnATakenLoginLogsInOnlyWithItsPassword(@TempDir Path tempDir) {
        AccountStore store = createStore(tempDir.resolve("accounts.json"));
        String id = store.create("mario", "hunter2").account().getId();

        assertFalse(store.create("mario", "something else").success());
        assertEquals(id, store.create("mario", "hunter2").account().getId());
    }

    /**
     * Verifies that an account still on the single SHA-256 round logs in and is rehashed with PBKDF2.
     */
    @Test
    void aLegacyHashLogsInAndIsRehashed(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("accounts.json");
        String salt = PasswordHasher.generateSalt();
        Files.writeString(file, "{\"mario\": {\"id\": \"acct-1\", \"login\": \"mario\", \"passwordHash\": \""
            + PasswordHasher.legacyHash("hunter2", salt) + "\", \"passwordSalt\": \"" + salt + "\"}}");
        AccountStore store = createStore(file);

        assertTrue(store.login("mario", "hunter2").success());
        PlayerAccount account = store.findByLogin("mario").orElseThrow();
        assertEquals(PasswordHasher.SCHEME, account.getPasswordScheme());
        assertTrue(PasswordHasher.matches("hunter2", account.getPasswordSalt(), account.getPasswordHash()));
        assertFalse(store.login("mario", "wrong").success());
    }

    /**
     * Verifies that logging in again with the correct password succeeds.
     */
    @Test
    void existingLoginWithCorrectPasswordSucceeds(@TempDir Path tempDir) {
        AccountStore store = createStore(tempDir.resolve("accounts.json"));
        store.create("mario", "hunter2");

        AuthResult result = store.login("mario", "hunter2");

        assertTrue(result.success());
    }

    /**
     * Verifies that logging in with the wrong password is rejected without
     * altering the existing account.
     */
    @Test
    void existingLoginWithWrongPasswordIsRejected(@TempDir Path tempDir) {
        AccountStore store = createStore(tempDir.resolve("accounts.json"));
        store.create("mario", "hunter2");

        AuthResult result = store.login("mario", "wrong password");

        assertFalse(result.success());
        assertTrue(store.findByLogin("mario").isPresent());
    }

    /**
     * Verifies that a blank or {@code null} login is rejected.
     */
    @Test
    void blankLoginIsRejected(@TempDir Path tempDir) {
        AccountStore store = createStore(tempDir.resolve("accounts.json"));

        assertFalse(store.login("", "password").success());
        assertFalse(store.login(null, "password").success());
    }

    /**
     * Verifies that an empty password is rejected.
     */
    @Test
    void emptyPasswordIsRejected(@TempDir Path tempDir) {
        AccountStore store = createStore(tempDir.resolve("accounts.json"));

        assertFalse(store.login("mario", "").success());
    }

    /**
     * Verifies that a login containing characters unsafe to later use as a
     * filesystem path segment is rejected.
     */
    @Test
    void loginWithUnsafeCharactersIsRejected(@TempDir Path tempDir) {
        AccountStore store = createStore(tempDir.resolve("accounts.json"));

        assertFalse(store.login("../escape", "password").success());
        assertFalse(store.login("a/b", "password").success());
        assertFalse(store.login("a b", "password").success());
    }

    /**
     * Verifies that a {@code null} password is rejected for a brand-new
     * login, rather than throwing.
     */
    @Test
    void nullPasswordIsRejectedForANewLogin(@TempDir Path tempDir) {
        AccountStore store = createStore(tempDir.resolve("accounts.json"));

        assertFalse(store.login("mario", null).success());
        assertFalse(store.create("mario", null).success());
        assertTrue(store.findByLogin("mario").isEmpty());
    }

    /**
     * Verifies that a rejected login for an existing account leaves the
     * stored account untouched, and that a later flush still succeeds.
     */
    @Test
    void rejectedLoginForAnExistingAccountLeavesItIntactAndFlushStillSucceeds(@TempDir Path tempDir) {
        Path file = tempDir.resolve("accounts.json");
        AccountStore store = createStore(file);
        String id = store.create("mario", "hunter2").account().getId();

        assertFalse(store.login("mario", "wrong password").success());
        assertEquals(id, store.findByLogin("mario").orElseThrow().getId());
        store.flush();

        AccountStore reloaded = createStore(file);
        assertEquals(id, reloaded.findByLogin("mario").orElseThrow().getId());
        assertTrue(reloaded.login("mario", "hunter2").success());
    }

    /**
     * Verifies that an account created and flushed by one store instance is
     * found by a fresh instance reading the same file.
     */
    @Test
    void accountSurvivesReloadFromDisk(@TempDir Path tempDir) {
        Path file = tempDir.resolve("accounts.json");
        AccountStore first = createStore(file);
        first.create("mario", "hunter2");
        first.flush();

        AccountStore reloaded = createStore(file);
        AuthResult result = reloaded.login("mario", "hunter2");

        assertTrue(result.success());
        assertTrue(reloaded.findByLogin("mario").isPresent());
    }

    /**
     * Verifies that a reloaded store still rejects the wrong password for a
     * previously persisted account.
     */
    @Test
    void reloadRejectsWrongPasswordForPersistedAccount(@TempDir Path tempDir) {
        Path file = tempDir.resolve("accounts.json");
        AccountStore first = createStore(file);
        first.create("mario", "hunter2");
        first.flush();

        AccountStore reloaded = createStore(file);

        assertFalse(reloaded.login("mario", "wrong password").success());
    }

    /**
     * Verifies that a store backed by a file that doesn't exist yet starts
     * with no accounts, rather than failing to open.
     */
    @Test
    void noStoreFileYetMeansNoAccounts(@TempDir Path tempDir) {
        AccountStore store = createStore(tempDir.resolve("does-not-exist-yet.json"));

        assertTrue(store.findByLogin("anyone").isEmpty());
    }

    /**
     * Verifies that a new account is on disk at once, so a crash right after can't leave an agency whose
     * account was never saved (review.md, appendix D, 4.S3).
     */
    @Test
    void aCreatedAccountIsOnDiskAtOnce(@TempDir Path tempDir) {
        Path file = tempDir.resolve("accounts.json");
        AccountStore store = createStore(file);
        assertFalse(Files.exists(file));

        store.create("mario", "hunter2");

        assertTrue(Files.exists(file), "create() writes the new account through to disk");
    }

    /**
     * Verifies that flushing again with nothing changed does not rewrite
     * the file.
     */
    @Test
    void flushWithNothingChangedDoesNotRewriteTheFile(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("accounts.json");
        AccountStore store = createStore(file);
        store.create("mario", "hunter2");
        store.flush();
        long firstWriteTime = Files.getLastModifiedTime(file).toMillis();

        store.flush(); // nothing changed since the flush above

        assertEquals(firstWriteTime, Files.getLastModifiedTime(file).toMillis());
    }

    /**
     * Verifies that {@code close} flushes once more and is safe to call
     * twice without throwing.
     */
    @Test
    void closeFlushesOnceMoreAndIsSafeToCallTwice(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("accounts.json");
        String salt = PasswordHasher.generateSalt();
        Files.writeString(file, "{\"mario\": {\"id\": \"acct-1\", \"login\": \"mario\", \"passwordHash\": \""
            + PasswordHasher.legacyHash("hunter2", salt) + "\", \"passwordSalt\": \"" + salt + "\"}}");
        AccountStore store = createStore(file);
        // A rehash on login is a change only a flush writes (a new account is written at once).
        assertTrue(store.login("mario", "hunter2").success());

        store.close();
        store.close(); // must not throw, e.g. from double-removing the shutdown hook

        AccountStore reloaded = createStore(file);
        assertEquals(PasswordHasher.SCHEME, reloaded.findByLogin("mario").orElseThrow().getPasswordScheme());
    }

    /**
     * Verifies that pruning backups keeps only the newest ones up to the
     * retention limit.
     */
    @Test
    void pruneOldBackupsKeepsOnlyTheNewestOnes(@TempDir Path tempDir) throws IOException {
        AccountStore store = createStore(tempDir.resolve("accounts.json"));
        Path backupDir = tempDir.resolve("backups");
        Files.createDirectories(backupDir);
        // Zero-padded so lexicographic sort matches intended chronological order, same as the
        // real yyyyMMdd-HHmmss timestamps do - named this way (rather than actually flushing
        // real backups 30 minutes apart) so the test doesn't depend on real wall-clock time.
        for (int i = 0; i < 60; i++) {
            Files.writeString(backupDir.resolve(String.format("accounts-%03d.json", i)), "{}");
        }

        store.pruneOldBackups();

        try (Stream<Path> remaining = Files.list(backupDir)) {
            List<String> names = remaining.map(path -> path.getFileName().toString()).sorted().toList();
            assertEquals(48, names.size());
            assertEquals("accounts-012.json", names.get(0), "the oldest 12 of 60 should have been pruned");
            assertEquals("accounts-059.json", names.get(names.size() - 1));
        }
    }
}
