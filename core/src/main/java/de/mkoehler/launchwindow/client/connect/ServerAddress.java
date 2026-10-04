package de.mkoehler.launchwindow.client.connect;

import de.mkoehler.launchwindow.net.NetworkConstants;

/**
 * A server to connect to, as typed on the Connect screen: a host and a TCP port. Parsing
 * follows the same shape as Robot-Rampage's own address field (the same author's code, not
 * BASPM's): {@code host}, {@code host:port}, or a bracketed IPv6 address with an optional port.
 * A missing port means {@link NetworkConstants#TCP_PORT}.
 *
 * @param host the host name or address
 * @param port the TCP port
 * @author Mario Koehler
 */
public record ServerAddress(String host, int port) {

    private static final int MAX_PORT = 65535;

    /**
     * Checks the parts.
     *
     * @throws IllegalArgumentException if the host is blank or the port is out of range; the
     *                                  message is worded for the player
     */
    public ServerAddress {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Enter the server's address.");
        }
        if (port < 1 || port > MAX_PORT) {
            throw new IllegalArgumentException("The port must be a number from 1 to " + MAX_PORT + ".");
        }
    }

    /**
     * Parses an address as the player typed it.
     *
     * @param text the text, such as {@code localhost:45825}
     * @return the address
     * @throws IllegalArgumentException if the text isn't an address; the message is worded for
     *                                  the player
     */
    public static ServerAddress parse(String text) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Enter the server's address.");
        }
        if (trimmed.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("An address has no spaces in it.");
        }
        if (trimmed.startsWith("[")) {
            return parseBracketed(trimmed);
        }
        int colon = trimmed.indexOf(':');
        if (colon < 0) {
            return new ServerAddress(trimmed, NetworkConstants.TCP_PORT);
        }
        if (colon != trimmed.lastIndexOf(':')) {
            throw new IllegalArgumentException(
                "Put an IPv6 address in brackets, like [::1]:" + NetworkConstants.TCP_PORT + ".");
        }
        return new ServerAddress(trimmed.substring(0, colon), parsePort(trimmed.substring(colon + 1)));
    }

    private static ServerAddress parseBracketed(String text) {
        int close = text.indexOf(']');
        if (close < 0) {
            throw new IllegalArgumentException("The bracket after the IPv6 address is missing.");
        }
        String host = text.substring(1, close);
        String rest = text.substring(close + 1);
        if (rest.isEmpty()) {
            return new ServerAddress(host, NetworkConstants.TCP_PORT);
        }
        if (!rest.startsWith(":")) {
            throw new IllegalArgumentException(
                "After the bracket, only a port like :" + NetworkConstants.TCP_PORT + " can follow.");
        }
        return new ServerAddress(host, parsePort(rest.substring(1)));
    }

    private static int parsePort(String text) {
        if (text.isEmpty() || text.length() > 5 || !text.chars().allMatch(c -> c >= '0' && c <= '9')) {
            throw new IllegalArgumentException("The port must be a number from 1 to " + MAX_PORT + ".");
        }
        return Integer.parseInt(text);
    }

    /**
     * Returns the address the way the player would type it.
     *
     * @return {@code host:port}, with an IPv6 host in brackets
     */
    @Override
    public String toString() {
        return (host.contains(":") ? "[" + host + "]" : host) + ":" + port;
    }
}
