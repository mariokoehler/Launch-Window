package de.mkoehler.launchwindow.client.connect;

import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.net.LoginNames;
import de.mkoehler.launchwindow.net.messages.ClaimSlotRequest;
import de.mkoehler.launchwindow.net.messages.FetchStateResponse;
import de.mkoehler.launchwindow.net.messages.HandshakeResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests how {@link LoginResult} tells the Connect screen's cases apart without a reason code
 * in the pinned handshake.
 *
 * @author Mario Koehler
 */
class LoginResultTest {

    /**
     * An accepted handshake lets the login go on.
     */
    @Test
    void anAcceptedHandshakeGoesOn() {
        assertTrue(LoginResult.fromHandshake(HandshakeResponse.accept("Welcome", "1.0"), "1.0").isEmpty());
    }

    /**
     * A refusal from a server on another version is a version mismatch, carrying both versions.
     */
    @Test
    void aRefusalFromAnotherVersionIsAMismatch() {
        LoginResult result = LoginResult.fromHandshake(
            HandshakeResponse.reject("Version mismatch", "1.1"), "1.0").orElseThrow();
        assertEquals(LoginResult.Kind.VERSION_MISMATCH, result.kind());
        assertEquals("1.0", result.clientVersion());
        assertEquals("1.1", result.serverVersion());
    }

    /**
     * Any other refusal is shown in the server's words.
     */
    @Test
    void anyOtherRefusalKeepsTheServersWords() {
        LoginResult result = LoginResult.fromHandshake(
            HandshakeResponse.reject("Incorrect password for 'mario'.", "1.0"), "1.0").orElseThrow();
        assertEquals(LoginResult.Kind.REJECTED, result.kind());
        assertEquals("Incorrect password for 'mario'.", result.message());
    }

    /**
     * The state fetch decides between the agency, Join Game and a failure.
     */
    @Test
    void theFetchDecidesWhereToGo() {
        PlayerSnapshot state = new PlayerSnapshot("Orion", Archetype.NASA, 1, 2, 3, 1);
        LoginResult inGame = LoginResult.fromFetch(FetchStateResponse.success(state));
        assertEquals(LoginResult.Kind.IN_GAME, inGame.kind());
        assertEquals("Orion", inGame.state().getAgencyName(), "the state, unpacked from the reply");
        assertEquals(LoginResult.Kind.NEEDS_SLOT,
            LoginResult.fromFetch(FetchStateResponse.failure(ClaimSlotRequest.SLOT_CLAIM_REQUIRED)).kind());
        assertEquals(LoginResult.Kind.FAILED, LoginResult.fromFetch(FetchStateResponse.failure("disk full")).kind());
    }

    /**
     * The shared login rule matches what the server's account store has always accepted.
     */
    @Test
    void theLoginRuleIsSharedWithTheServer() {
        assertTrue(LoginNames.isValid("mario_k-2"));
        assertTrue(LoginNames.isValid("a".repeat(LoginNames.MAX_LENGTH)));
        assertFalse(LoginNames.isValid("a".repeat(LoginNames.MAX_LENGTH + 1)));
        assertFalse(LoginNames.isValid("mario k"));
        assertFalse(LoginNames.isValid("../escape"));
        assertFalse(LoginNames.isValid(""));
        assertFalse(LoginNames.isValid(null));
    }
}
