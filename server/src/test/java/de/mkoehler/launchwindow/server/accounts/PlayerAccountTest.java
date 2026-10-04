package de.mkoehler.launchwindow.server.accounts;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

/**
 * <p>Tests for {@link PlayerAccount}.</p>
 *
 * @author Mario Koehler
 */
class PlayerAccountTest {

    /**
     * Verifies that {@code copy} returns a distinct instance with the same
     * field values.
     */
    @Test
    void copyReturnsAnIndependentInstanceWithTheSameFields() {
        PlayerAccount original = new PlayerAccount("acct-1", "mario", "hash", "salt");

        PlayerAccount copy = original.copy();

        assertNotSame(original, copy);
        assertEquals(original.getId(), copy.getId());
        assertEquals(original.getLogin(), copy.getLogin());
        assertEquals(original.getPasswordHash(), copy.getPasswordHash());
        assertEquals(original.getPasswordSalt(), copy.getPasswordSalt());
    }

    /**
     * Verifies that changing the original account after copying does not
     * affect the copy.
     */
    @Test
    void mutatingTheOriginalAfterCopyingDoesNotAffectTheCopy() {
        PlayerAccount original = new PlayerAccount("acct-1", "mario", "hash", "salt");
        PlayerAccount copy = original.copy();

        original.setPasswordHash("other-hash");

        assertEquals("hash", copy.getPasswordHash());
    }
}
