package de.mkoehler.launchwindow.server.accounts;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>Tests for {@link AuthResult}.</p>
 *
 * @author Mario Koehler
 */
class AuthResultTest {

    /**
     * Verifies that {@code success} builds a successful result carrying the
     * given account and message.
     */
    @Test
    void successBuildsASuccessfulResultCarryingTheAccount() {
        PlayerAccount account = new PlayerAccount("acct-1", "mario", "hash", "salt");

        AuthResult result = AuthResult.success(account, "Welcome back.");

        assertTrue(result.success());
        assertEquals("Welcome back.", result.message());
        assertSame(account, result.account());
    }

    /**
     * Verifies that {@code failure} builds a rejected result with no
     * account.
     */
    @Test
    void failureBuildsARejectedResultWithNoAccount() {
        AuthResult result = AuthResult.failure("Incorrect password.");

        assertFalse(result.success());
        assertEquals("Incorrect password.", result.message());
        assertNull(result.account());
    }
}
