package de.mkoehler.launchwindow.net;

import java.util.regex.Pattern;

/**
 * The rule a login name must follow ({@code design.md} &sect;3.8), shared by the server's
 * account store and the client's connect screen, so the client can flag a bad login before
 * sending it and both sides always agree on what "valid" means.
 *
 * <p>A login is restricted to characters safe to later use as a filesystem path segment (the
 * per-player data directory): this rejects path-traversal sequences and characters illegal in a
 * Windows file name.</p>
 *
 * @author Mario Koehler
 */
public final class LoginNames {

    /** The longest allowed login. */
    public static final int MAX_LENGTH = 32;

    private static final Pattern VALID = Pattern.compile("[A-Za-z0-9_-]{1," + MAX_LENGTH + "}");

    /**
     * Not instantiable; this class only holds the rule.
     */
    private LoginNames() {
    }

    /**
     * Returns whether a login follows the rule.
     *
     * @param login the login, may be {@code null}
     * @return {@code true} if it is 1 to {@value #MAX_LENGTH} letters, digits, {@code _} or {@code -}
     */
    public static boolean isValid(String login) {
        return login != null && VALID.matcher(login).matches();
    }
}
