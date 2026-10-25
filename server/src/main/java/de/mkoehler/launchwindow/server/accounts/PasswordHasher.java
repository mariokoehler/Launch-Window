package de.mkoehler.launchwindow.server.accounts;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * <p>Pure password hashing/verification for {@link PlayerAccount} (see
 * {@code design.md} &sect;3.8): PBKDF2 with HMAC-SHA-256 over the password and a
 * per-account random salt, {@value #ITERATIONS} rounds, so a stolen
 * {@code accounts.json} is slow to guess from (review.md 2.3). Accounts made before
 * 2026-10-25 hold a single SHA-256 round ({@link #LEGACY_SCHEME}); they still log in,
 * and are rehashed the first time they do ({@link AccountStore}). The raw password is
 * never stored or logged.</p>
 *
 * <p>Stateless and static; every value is hex-encoded so it round-trips
 * cleanly through {@link PlayerAccount}'s plain Jackson-serialized
 * {@code String} fields.</p>
 *
 * @author Mario Koehler
 */
public final class PasswordHasher {

    /** The scheme {@link #hash} uses, as stored on an account. */
    public static final String SCHEME = "pbkdf2-sha256";
    /** The scheme of accounts made before PBKDF2, stored as no scheme at all. */
    public static final String LEGACY_SCHEME = "sha256";
    /**
     * PBKDF2's rounds: a few hundred milliseconds at most on the NAS, paid once a login (a session keeps
     * its connection). Unconfirmed; OWASP's figure is higher, for servers that log in far more often.
     */
    static final int ITERATIONS = 210_000;

    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Not instantiable; every member here is static.
     */
    private PasswordHasher() {
    }

    /**
     * Generates a new random salt for a freshly-created account.
     *
     * @return a random salt, hex-encoded
     */
    public static String generateSalt() {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        return HexFormat.of().formatHex(salt);
    }

    /**
     * Hashes a password with the given salt, in the current {@link #SCHEME}.
     *
     * @param password the plain-text password
     * @param saltHex  the salt to hash it with, hex-encoded (see {@link #generateSalt()})
     * @return the resulting hash, hex-encoded
     */
    public static String hash(String password, String saltHex) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), HexFormat.of().parseHex(saltHex), ITERATIONS,
            KEY_BITS);
        try {
            return HexFormat.of().formatHex(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec).getEncoded());
        } catch (GeneralSecurityException e) {
            // PBKDF2WithHmacSHA256 is in every JDK since 8 - this can't happen.
            throw new IllegalStateException("PBKDF2WithHmacSHA256 unexpectedly unavailable", e);
        } finally {
            spec.clearPassword();
        }
    }

    /**
     * Hashes a password the way accounts made before PBKDF2 were: one SHA-256 round over salt and password.
     *
     * @param password the plain-text password
     * @param saltHex  the salt, hex-encoded
     * @return the hash, hex-encoded
     */
    static String legacyHash(String password, String saltHex) {
        MessageDigest digest = newDigest();
        digest.update(HexFormat.of().parseHex(saltHex));
        digest.update(password.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest.digest());
    }

    /**
     * Checks a plain-text password against a hash in the current {@link #SCHEME}.
     *
     * @param password        the plain-text password to check
     * @param saltHex         the salt the expected hash was computed with
     * @param expectedHashHex the expected hash, hex-encoded
     * @return {@code true} if the password hashes to the expected value
     */
    public static boolean matches(String password, String saltHex, String expectedHashHex) {
        return matches(password, saltHex, expectedHashHex, SCHEME);
    }

    /**
     * Checks a plain-text password against a previously computed hash, using a constant-time comparison
     * ({@link MessageDigest#isEqual}) so timing can't leak how much of the hash matched.
     *
     * @param password        the plain-text password to check
     * @param saltHex         the salt the expected hash was computed with
     * @param expectedHashHex the expected hash, hex-encoded
     * @param scheme          the hash's scheme: {@link #SCHEME}, or {@code null} or {@link #LEGACY_SCHEME}
     *                        for an account made before PBKDF2
     * @return {@code true} if the password hashes to the expected value
     */
    public static boolean matches(String password, String saltHex, String expectedHashHex, String scheme) {
        String actualHex = scheme == null || scheme.equals(LEGACY_SCHEME) ? legacyHash(password, saltHex)
            : hash(password, saltHex);
        return MessageDigest.isEqual(HexFormat.of().parseHex(actualHex), HexFormat.of().parseHex(expectedHashHex));
    }

    /**
     * Creates a fresh SHA-256 digest instance.
     *
     * @return a new digest instance
     */
    private static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is a mandatory JCA algorithm on every conforming JVM - this can't happen.
            throw new IllegalStateException("SHA-256 unexpectedly unavailable", e);
        }
    }
}
