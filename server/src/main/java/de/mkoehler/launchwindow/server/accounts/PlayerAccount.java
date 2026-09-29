package de.mkoehler.launchwindow.server.accounts;

import java.util.Objects;

/**
 * <p>A persisted player account (see {@code design.md} &sect;3.8): a
 * server-assigned id, a unique login, a hashed and salted password, and a
 * separate (not-necessarily-unique) display name. Deliberately minimal -
 * per-game choices (agency archetype, agency name, persona) live entirely
 * in that game's own {@link de.mkoehler.launchwindow.model.PlayerSnapshot},
 * not on the account. An account's only job is identity.</p>
 *
 * <p>{@link #getId()} - not {@link #getLogin()} - is what a per-player data
 * directory (e.g. {@code players/<id>/}, {@code design.md} &sect;3.7) is
 * keyed by: it never changes even if the login one day does, and it
 * carries no case-sensitivity ambiguity across filesystems the way a login
 * used directly as a directory name would.</p>
 *
 * <p>Plain mutable bean (public no-arg constructor, getters and setters) so
 * Jackson can (de)serialize it with no extra configuration, same convention
 * as this project's other Jackson-backed types.</p>
 *
 * <p>The raw password is never stored - only {@link #getPasswordHash()} (a
 * salted digest, see {@link PasswordHasher}) ever reaches disk.</p>
 *
 * @author Mario Koehler
 */
public class PlayerAccount {

    private String id;
    private String login;
    private String passwordHash;
    private String passwordSalt;
    private String displayName;

    /**
     * No-argument constructor required for Jackson deserialization.
     */
    public PlayerAccount() {
    }

    /**
     * Creates a player account.
     *
     * @param id           the server-assigned id, stable even if the login
     *                     later changes
     * @param login        the unique login name
     * @param passwordHash the salted password hash (never the raw password)
     * @param passwordSalt the per-account random salt the hash was computed with
     * @param displayName  the name other players see in-game
     */
    public PlayerAccount(String id, String login, String passwordHash, String passwordSalt, String displayName) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.login = Objects.requireNonNull(login, "login must not be null");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash must not be null");
        this.passwordSalt = Objects.requireNonNull(passwordSalt, "passwordSalt must not be null");
        this.displayName = Objects.requireNonNull(displayName, "displayName must not be null");
    }

    /**
     * Returns this account's server-assigned id.
     *
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * Sets this account's server-assigned id.
     *
     * @param id the id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns this account's unique login name.
     *
     * @return the login name
     */
    public String getLogin() {
        return login;
    }

    /**
     * Sets this account's unique login name.
     *
     * @param login the login name
     */
    public void setLogin(String login) {
        this.login = login;
    }

    /**
     * Returns this account's salted password hash.
     *
     * @return the password hash
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * Sets this account's salted password hash.
     *
     * @param passwordHash the password hash
     */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /**
     * Returns the per-account salt {@link #getPasswordHash()} was computed
     * with.
     *
     * @return the password salt
     */
    public String getPasswordSalt() {
        return passwordSalt;
    }

    /**
     * Sets the per-account salt {@link #getPasswordHash()} was computed
     * with.
     *
     * @param passwordSalt the password salt
     */
    public void setPasswordSalt(String passwordSalt) {
        this.passwordSalt = passwordSalt;
    }

    /**
     * Returns the name this player is shown as in-game.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Sets the name this player is shown as in-game.
     *
     * @param displayName the display name
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns an independent copy of this account - safe to hand to
     * {@link AccountStore}'s background flush thread without racing further
     * mutations made to the original. Every field here is an immutable
     * type, so a plain field-by-field copy is enough.
     *
     * @return an independent copy of this account
     */
    public PlayerAccount copy() {
        return new PlayerAccount(id, login, passwordHash, passwordSalt, displayName);
    }
}
