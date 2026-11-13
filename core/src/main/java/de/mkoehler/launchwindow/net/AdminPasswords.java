package de.mkoehler.launchwindow.net;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * The server's admin password ({@code ui-spec.md} &sect;4.22, G21): how a new one looks (the server makes one at
 * startup when the file has none, the client's "Make a new one" another), and the comparison.
 *
 * @author Mario Koehler
 */
public final class AdminPasswords {

    /** The characters a made password uses: no look-alikes (0/O, 1/l/I). */
    static final String ALPHABET = "abcdefghjkmnpqrstuvwxyz23456789";

    /** The shortest password a save accepts. */
    public static final int MIN_LENGTH = 8;

    private static final SecureRandom RANDOM = new SecureRandom();

    private AdminPasswords() {
    }

    /**
     * Makes a new password: 16 characters in groups of four, like {@code k7mq-r4tx-9hzc-wp3d}.
     *
     * @return the password
     */
    public static String generate() {
        StringBuilder text = new StringBuilder();
        for (int group = 0; group < 4; group++) {
            if (group > 0) {
                text.append('-');
            }
            for (int i = 0; i < 4; i++) {
                text.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
            }
        }
        return text.toString();
    }

    /**
     * Compares a try with the password in constant time. A missing or blank password matches nothing.
     *
     * @param password the password, from the file
     * @param attempt  the try
     * @return whether they match
     */
    public static boolean matches(String password, String attempt) {
        if (password == null || password.isBlank() || attempt == null) {
            return false;
        }
        return MessageDigest.isEqual(password.getBytes(StandardCharsets.UTF_8),
            attempt.getBytes(StandardCharsets.UTF_8));
    }
}
