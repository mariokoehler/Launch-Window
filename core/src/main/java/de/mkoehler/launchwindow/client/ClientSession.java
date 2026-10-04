package de.mkoehler.launchwindow.client;

import de.mkoehler.launchwindow.client.connect.Credentials;
import de.mkoehler.launchwindow.client.connect.GameInfo;
import de.mkoehler.launchwindow.client.connect.ServerResult;
import de.mkoehler.launchwindow.client.view.MissionDraft;
import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.ContentPackHash;
import de.mkoehler.launchwindow.content.ContentPackLoader;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.model.PlayerSnapshot;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * What the client knows about the player's game while they're in it: who they are, their latest
 * state (with today's actions applied), the game info, and the game's content pack, loaded on the
 * client from the copy it bundles so screens can read names, caps and numbers (gap G7: the client
 * calls the same {@code core} code the server does, never its own copy of the rules).
 *
 * <p>The game's pack is identified by the game info's id and hash. If the client's bundled copy
 * hashes differently, the screens still use it, but {@link #packStatus()} says so: the download of
 * the game's own pack (gap G1, second half) isn't built yet.</p>
 *
 * @author Mario Koehler
 */
public final class ClientSession {

    /**
     * How the client's copy of the content pack compares with the game's.
     */
    public enum PackStatus {
        /** Identical to the game's frozen copy. */
        MATCH,
        /** The client has the pack, but a different version of it. */
        DIFFERENT
    }

    private final Credentials credentials;
    private final ContentCatalog catalog;
    private final PackStatus packStatus;
    private PlayerSnapshot state;
    private GameInfo gameInfo;
    private final Map<String, MissionDraft> drafts = new HashMap<>();

    private ClientSession(Credentials credentials, ContentCatalog catalog, PackStatus packStatus, PlayerSnapshot state,
                          GameInfo gameInfo) {
        this.credentials = credentials;
        this.catalog = catalog;
        this.packStatus = packStatus;
        this.state = state;
        this.gameInfo = gameInfo;
    }

    /**
     * Starts a session from a successful login or claim, loading the game's content pack.
     *
     * @param credentials who and where
     * @param state       the player's state
     * @param gameInfo    the game info
     * @param classLoader the class loader holding the bundled packs under {@code content/}
     * @return the session
     * @throws IllegalStateException if the client doesn't bundle the game's pack at all
     */
    public static ClientSession start(Credentials credentials, PlayerSnapshot state, GameInfo gameInfo,
                                      ClassLoader classLoader) {
        Objects.requireNonNull(gameInfo, "gameInfo must not be null");
        String packId = gameInfo.response().contentPackId();
        String prefix = "content/" + packId;
        ContentCatalog catalog;
        String hash;
        try {
            catalog = ContentPackLoader.loadFromClasspath(prefix, classLoader);
            hash = ContentPackHash.ofClasspath(prefix, classLoader);
        } catch (RuntimeException e) {
            throw new IllegalStateException("This client doesn't have the game's content pack \"" + packId + "\".", e);
        }
        PackStatus status = hash.equals(gameInfo.response().contentPackHash()) ? PackStatus.MATCH : PackStatus.DIFFERENT;
        return new ClientSession(credentials, catalog, status, state, gameInfo);
    }

    /**
     * Takes in a submit's or refresh's result: whatever state and game info it carries replace the
     * ones held.
     *
     * @param result the result
     */
    public void update(ServerResult result) {
        if (result.state() != null) {
            state = result.state();
        }
        if (result.gameInfo() != null) {
            gameInfo = result.gameInfo();
        }
    }

    /**
     * Returns who the player is and where they play.
     *
     * @return the credentials
     */
    public Credentials credentials() {
        return credentials;
    }

    /**
     * Returns the player's latest state.
     *
     * @return the state
     */
    public PlayerSnapshot state() {
        return state;
    }

    /**
     * Returns the latest game info.
     *
     * @return the game info
     */
    public GameInfo gameInfo() {
        return gameInfo;
    }

    /**
     * Returns the game's content pack.
     *
     * @return the catalog
     */
    public ContentCatalog catalog() {
        return catalog;
    }

    /**
     * Returns how the client's pack compares with the game's.
     *
     * @return the status
     */
    public PackStatus packStatus() {
        return packStatus;
    }

    /**
     * Returns the plan being drafted for a configuration, started on first use and brought up to
     * date with the current state ({@link MissionDraft#refit}) on every call.
     *
     * @param configuration the configuration
     * @return the draft
     */
    public MissionDraft draft(MissionConfiguration configuration) {
        MissionDraft draft = drafts.get(configuration.id());
        if (draft == null) {
            draft = MissionDraft.start(configuration, state, catalog);
            drafts.put(configuration.id(), draft);
        } else {
            draft.refit(state, catalog);
        }
        return draft;
    }

    /**
     * Forgets the draft for a configuration, after it launched.
     *
     * @param configurationId the configuration
     */
    public void clearDraft(String configurationId) {
        drafts.remove(configurationId);
    }
}
