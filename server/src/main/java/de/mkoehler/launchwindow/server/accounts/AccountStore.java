package de.mkoehler.launchwindow.server.accounts;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import de.mkoehler.launchwindow.net.LoginNames;
import de.mkoehler.launchwindow.persistence.AtomicFiles;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * <p>File-backed store of every {@link PlayerAccount} (see
 * {@code design.md} &sect;3.8): a single JSON file mapping login name to
 * account, written atomically (temp file, then an atomic replace via
 * {@link AtomicFiles}) so a crash mid-write can never leave it truncated.</p>
 *
 * <p>Every mutation ({@link #login}) only ever touches the in-memory
 * {@link #accountsByLogin} map, under {@code synchronized} - no disk I/O
 * happens on the calling thread. A background task instead flushes the
 * accumulated state to disk on a fixed ~{@value #FLUSH_INTERVAL_SECONDS}
 * -second cadence (skipped entirely if nothing changed since the last
 * flush), and periodically writes a rolling, retention-bounded backup
 * alongside it (see {@link #FLUSHES_PER_BACKUP}/{@link #BACKUPS_TO_KEEP}) -
 * so a login never blocks on disk, and a bad state still has recent
 * snapshots to roll back to. Only the brief "copy the current state" step
 * needs the lock; the actual write happens off it, entirely on the
 * background thread. A JVM shutdown hook ({@link #close()}) flushes once
 * more, synchronously, so a normal stop/restart (SIGTERM, what
 * {@code docker stop} sends) doesn't lose whatever changed since the last
 * scheduled flush - only a hard kill or crash can still do that, an
 * accepted trade-off of not writing through on every change.</p>
 *
 * <p>Implements {@code design.md} &sect;3.8's account creation flow:
 * {@link #login} with a login name that doesn't exist yet auto-creates the
 * account from the supplied password; an existing login
 * requires the password to match, otherwise the attempt is rejected.
 * Deliberately has no dependency on libGDX (plain {@link Path} I/O), so
 * it's directly unit-testable without a running application.</p>
 *
 * @author Mario Koehler
 */
public class AccountStore {

    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private static final TypeReference<LinkedHashMap<String, PlayerAccount>> ACCOUNTS_TYPE = new TypeReference<>() {
    };
    private static final DateTimeFormatter BACKUP_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");


    /** How often the background task flushes accumulated changes to disk. */
    private static final long FLUSH_INTERVAL_SECONDS = 60;
    /** One backup every this many flushes - roughly every 30 minutes, at the default flush interval. */
    private static final int FLUSHES_PER_BACKUP = 30;
    /** Rolling backups kept before the oldest is pruned - roughly a day's worth, at the default cadence. */
    private static final int BACKUPS_TO_KEEP = 48;

    private final Path storageFile;
    private final Path backupDir;
    private final Map<String, PlayerAccount> accountsByLogin;
    private final AtomicBoolean dirty = new AtomicBoolean(false);
    private final AtomicInteger flushesSinceLastBackup = new AtomicInteger(0);
    private final ScheduledExecutorService scheduler;
    private final Thread shutdownHook;

    /**
     * Opens (or, if it doesn't exist yet, prepares to create on first flush)
     * the account store backed by the given file, and starts its background
     * flush task.
     *
     * @param storageFile the JSON file to load from / persist to
     */
    public AccountStore(Path storageFile) {
        this.storageFile = storageFile;
        this.backupDir = storageFile.resolveSibling("backups");
        this.accountsByLogin = load(storageFile);
        this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "account-store-flush");
            thread.setDaemon(true);
            return thread;
        });
        scheduler.scheduleWithFixedDelay(this::flushAndMaybeBackup,
            FLUSH_INTERVAL_SECONDS, FLUSH_INTERVAL_SECONDS, TimeUnit.SECONDS);
        // A plain JVM shutdown hook, not a libGDX lifecycle callback - this class has no libGDX
        // dependency, and a hook here is a stronger guarantee anyway: it fires for any normal
        // JVM exit (including SIGTERM), independent of whether the surrounding application's own
        // shutdown path reliably reaches this class.
        this.shutdownHook = new Thread(this::close, "account-store-shutdown-flush");
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    /**
     * Loads every account from disk, or an empty store if the file doesn't
     * exist yet.
     *
     * @param storageFile the JSON file to load from
     * @return the loaded accounts, keyed by login
     */
    private static Map<String, PlayerAccount> load(Path storageFile) {
        if (!Files.exists(storageFile)) {
            return new LinkedHashMap<>();
        }
        try {
            return MAPPER.readValue(storageFile.toFile(), ACCOUNTS_TYPE);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read account store at " + storageFile, e);
        }
    }

    /**
     * Looks up an account by login name, without authenticating.
     *
     * @param login the login name to look up
     * @return the account, if one exists
     */
    public synchronized Optional<PlayerAccount> findByLogin(String login) {
        return Optional.ofNullable(accountsByLogin.get(login));
    }

    /**
     * Authenticates an account. A login nobody has is refused with
     * {@link de.mkoehler.launchwindow.net.messages.HandshakeResponse#NO_SUCH_ACCOUNT}: accounts are made
     * only on purpose ({@link #create}), so a typo never makes a second one (review.md 2.3). An account
     * still on the old hash is rehashed in the current scheme as it logs in. Synchronized against
     * concurrent logins (the network layer may invoke this from its own thread for multiple
     * simultaneously-connecting clients).
     *
     * @param login       the login name
     * @param password    the plain-text password to verify
     * @return the login result
     */
    public synchronized AuthResult login(String login, String password) {
        AuthResult invalid = checkInput(login, password);
        if (invalid != null) {
            return invalid;
        }
        PlayerAccount existing = accountsByLogin.get(login);
        if (existing == null) {
            return AuthResult.failure(de.mkoehler.launchwindow.net.messages.HandshakeResponse.NO_SUCH_ACCOUNT);
        }
        return verify(existing, password);
    }

    /**
     * Creates an account - see {@code design.md} &sect;3.8's account creation flow - and writes it to disk
     * at once, so a crash right after can't leave an agency whose account was never saved. A login that
     * exists is refused, unless the password is its own: then this logs in, so a create whose answer was
     * lost can be sent again. Synchronized so two connections racing on the same new login can't both
     * "win".
     *
     * @param login    the login name
     * @param password the plain-text password
     * @return the result
     */
    public synchronized AuthResult create(String login, String password) {
        AuthResult invalid = checkInput(login, password);
        if (invalid != null) {
            return invalid;
        }
        PlayerAccount existing = accountsByLogin.get(login);
        if (existing != null) {
            AuthResult again = verify(existing, password);
            return again.success() ? again : AuthResult.failure("The login '" + login + "' is taken.");
        }
        String salt = PasswordHasher.generateSalt();
        PlayerAccount created = new PlayerAccount(UUID.randomUUID().toString(), login,
            PasswordHasher.hash(password, salt), salt);
        accountsByLogin.put(login, created);
        dirty.set(true);
        flush();
        return AuthResult.success(created, "Welcome, " + login + "! Account created.");
    }

    /** The login's and password's own checks; {@code null} if they pass. */
    private static AuthResult checkInput(String login, String password) {
        if (login == null || login.isBlank()) {
            return AuthResult.failure("Login is required.");
        }
        if (!LoginNames.isValid(login)) {
            return AuthResult.failure(
                "Login may only contain letters, digits, '_' and '-', up to " + LoginNames.MAX_LENGTH + " characters.");
        }
        if (password == null || password.isEmpty()) {
            return AuthResult.failure("Password is required.");
        }
        return null;
    }

    /** Checks the password, rehashing an account still on the old scheme. */
    private AuthResult verify(PlayerAccount existing, String password) {
        if (!PasswordHasher.matches(password, existing.getPasswordSalt(), existing.getPasswordHash(),
                existing.getPasswordScheme())) {
            return AuthResult.failure("Incorrect password for '" + existing.getLogin() + "'.");
        }
        if (!PasswordHasher.SCHEME.equals(existing.getPasswordScheme())) {
            String salt = PasswordHasher.generateSalt();
            existing.setPasswordSalt(salt);
            existing.setPasswordHash(PasswordHasher.hash(password, salt));
            existing.setPasswordScheme(PasswordHasher.SCHEME);
            dirty.set(true);
        }
        return AuthResult.success(existing, "Welcome back, " + existing.getLogin() + ".");
    }

    /**
     * Forces an immediate, synchronous write of the current in-memory state
     * to disk, bypassing the ~{@value #FLUSH_INTERVAL_SECONDS}-second
     * background cadence - a no-op if nothing has changed since the last
     * flush. Used on shutdown and directly by tests, so tests don't need to
     * wait on wall-clock timing to observe a write.
     */
    public void flush() {
        Map<String, PlayerAccount> snapshot = snapshot();
        if (snapshot != null) {
            writeToDisk(storageFile, snapshot);
        }
    }

    /**
     * The background task's actual work: flush, then - every
     * {@value #FLUSHES_PER_BACKUP} flushes - a rolling backup. Guards
     * against a single failed run (e.g. a transient disk error) silently
     * cancelling every future scheduled run, which is what an uncaught
     * exception out of a {@link ScheduledExecutorService} task does.
     */
    private void flushAndMaybeBackup() {
        try {
            Map<String, PlayerAccount> snapshot = snapshot();
            if (snapshot == null) {
                return;
            }
            writeToDisk(storageFile, snapshot);
            if (flushesSinceLastBackup.incrementAndGet() >= FLUSHES_PER_BACKUP) {
                flushesSinceLastBackup.set(0);
                writeBackup(snapshot);
            }
        } catch (RuntimeException e) {
            System.err.println("AccountStore: background flush failed, will retry next cycle: " + e.getMessage());
        }
    }

    /**
     * Takes a deep, consistent copy of every account under the lock - the
     * only part of a flush that needs it, since {@link PlayerAccount} is a
     * mutable bean and the live objects could otherwise be mutated by a
     * concurrent {@link #login} while this thread serializes them - or
     * returns {@code null} if nothing has changed since the previous flush,
     * so an idle server doesn't rewrite an unchanged file forever.
     *
     * @return an independent snapshot of every account, or {@code null}
     */
    private synchronized Map<String, PlayerAccount> snapshot() {
        if (!dirty.compareAndSet(true, false)) {
            return null;
        }
        Map<String, PlayerAccount> copy = new LinkedHashMap<>();
        for (Map.Entry<String, PlayerAccount> entry : accountsByLogin.entrySet()) {
            copy.put(entry.getKey(), entry.getValue().copy());
        }
        return copy;
    }

    /**
     * Writes a snapshot of the account map to the given file.
     *
     * @param file the file to write
     * @param data the accounts to write, keyed by login
     */
    private static void writeToDisk(Path file, Map<String, PlayerAccount> data) {
        try {
            Files.createDirectories(file.getParent());
            Path tempFile = Files.createTempFile(file.getParent(), "accounts-", ".json.tmp");
            try {
                MAPPER.writeValue(tempFile.toFile(), data);
                AtomicFiles.moveReplacingWithRetry(tempFile, file);
            } catch (IOException e) {
                Files.deleteIfExists(tempFile);
                throw e;
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write account store at " + file, e);
        }
    }

    /**
     * Writes a timestamped rolling backup of the given snapshot, then prunes
     * old backups beyond the retention limit.
     *
     * @param snapshot the accounts to back up, keyed by login
     */
    private void writeBackup(Map<String, PlayerAccount> snapshot) {
        try {
            Files.createDirectories(backupDir);
            String timestamp = BACKUP_TIMESTAMP.format(LocalDateTime.now());
            writeToDisk(backupDir.resolve("accounts-" + timestamp + ".json"), snapshot);
            pruneOldBackups();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create account store backup in " + backupDir, e);
        }
    }

    /**
     * Deletes the oldest backups beyond {@value #BACKUPS_TO_KEEP} -
     * filenames sort chronologically. Package-private, not private: lets
     * {@code AccountStoreTest} exercise the retention rule directly against
     * a seeded set of fake backup files, rather than needing to wait real
     * wall-clock time for {@link #FLUSHES_PER_BACKUP} real backups to pile
     * up naturally.
     *
     * @throws IOException if the backup directory cannot be read
     */
    void pruneOldBackups() throws IOException {
        try (Stream<Path> files = Files.list(backupDir)) {
            List<Path> backups = files
                .filter(path -> path.getFileName().toString().startsWith("accounts-"))
                .sorted()
                .toList();
            for (int i = 0; i < backups.size() - BACKUPS_TO_KEEP; i++) {
                Files.deleteIfExists(backups.get(i));
            }
        }
    }

    /**
     * Stops the background flush task and performs one final, synchronous
     * flush - registered as a JVM shutdown hook in the constructor, and
     * safe to call again directly (e.g. from a test's teardown) without
     * double-flushing, since {@link #flush()} is itself a no-op once
     * nothing is left dirty.
     */
    public void close() {
        scheduler.shutdown();
        flush();
        try {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
        } catch (IllegalStateException ignored) {
            // Already mid-shutdown (this being called AS the hook, or another one) - can't
            // unregister a hook at that point, and there's no need to.
        }
    }
}
