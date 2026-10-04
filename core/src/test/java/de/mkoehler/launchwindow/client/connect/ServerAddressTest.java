package de.mkoehler.launchwindow.client.connect;

import de.mkoehler.launchwindow.net.NetworkConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests {@link ServerAddress#parse}.
 *
 * @author Mario Koehler
 */
class ServerAddressTest {

    /**
     * A host without a port gets the game's default port.
     */
    @Test
    void aMissingPortMeansTheDefault() {
        assertEquals(new ServerAddress("localhost", NetworkConstants.TCP_PORT), ServerAddress.parse(" localhost "));
    }

    /**
     * A host and port are split at the colon.
     */
    @Test
    void hostAndPortAreSplit() {
        assertEquals(new ServerAddress("nas.local", 4000), ServerAddress.parse("nas.local:4000"));
    }

    /**
     * An IPv6 address goes in brackets, with or without a port.
     */
    @Test
    void bracketedIpv6IsAccepted() {
        assertEquals(new ServerAddress("::1", 4000), ServerAddress.parse("[::1]:4000"));
        assertEquals(new ServerAddress("::1", NetworkConstants.TCP_PORT), ServerAddress.parse("[::1]"));
        assertEquals("[::1]:4000", new ServerAddress("::1", 4000).toString());
    }

    /**
     * Empty text, spaces, a bad port and a bare IPv6 address are refused with a message.
     */
    @Test
    void badAddressesAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> ServerAddress.parse(""));
        assertThrows(IllegalArgumentException.class, () -> ServerAddress.parse("my host"));
        assertThrows(IllegalArgumentException.class, () -> ServerAddress.parse("host:"));
        assertThrows(IllegalArgumentException.class, () -> ServerAddress.parse("host:70000"));
        assertThrows(IllegalArgumentException.class, () -> ServerAddress.parse("host:12a"));
        assertThrows(IllegalArgumentException.class, () -> ServerAddress.parse("::1"));
        assertThrows(IllegalArgumentException.class, () -> ServerAddress.parse("[::1"));
    }
}
