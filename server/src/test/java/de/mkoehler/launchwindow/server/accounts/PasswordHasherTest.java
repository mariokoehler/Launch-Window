package de.mkoehler.launchwindow.server.accounts;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>Tests for {@link PasswordHasher}.</p>
 *
 * @author Mario Koehler
 */
class PasswordHasherTest {

    /**
     * Verifies that a password matched against its own hash and salt
     * verifies successfully.
     */
    @Test
    void matchingPasswordAndSaltVerifiesTrue() {
        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hash("correct horse", salt);

        assertTrue(PasswordHasher.matches("correct horse", salt, hash));
    }

    /**
     * Verifies that the wrong password fails verification.
     */
    @Test
    void wrongPasswordVerifiesFalse() {
        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hash("correct horse", salt);

        assertFalse(PasswordHasher.matches("wrong password", salt, hash));
    }

    /**
     * Verifies that different passwords hashed with the same salt produce
     * different hashes.
     */
    @Test
    void sameSaltDifferentPasswordsProduceDifferentHashes() {
        String salt = PasswordHasher.generateSalt();

        assertNotEquals(PasswordHasher.hash("password one", salt), PasswordHasher.hash("password two", salt));
    }

    /**
     * Verifies that the same password hashed with different salts produces
     * different hashes.
     */
    @Test
    void samePasswordDifferentSaltsProduceDifferentHashes() {
        String saltA = PasswordHasher.generateSalt();
        String saltB = PasswordHasher.generateSalt();

        assertNotEquals(PasswordHasher.hash("same password", saltA), PasswordHasher.hash("same password", saltB));
    }

    /**
     * Verifies that repeated calls generate distinct salts.
     */
    @Test
    void generateSaltProducesDistinctValues() {
        assertNotEquals(PasswordHasher.generateSalt(), PasswordHasher.generateSalt());
    }

    /**
     * Verifies that hashing the same password and salt twice produces the
     * same result.
     */
    @Test
    void hashIsDeterministicForSamePasswordAndSalt() {
        String salt = PasswordHasher.generateSalt();

        assertEquals(PasswordHasher.hash("password", salt), PasswordHasher.hash("password", salt));
    }
}
