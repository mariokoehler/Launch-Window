package de.mkoehler.launchwindow.client;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import de.mkoehler.launchwindow.client.connect.Credentials;
import de.mkoehler.launchwindow.client.connect.GameInfo;
import de.mkoehler.launchwindow.client.connect.NetworkGateway;
import de.mkoehler.launchwindow.client.connect.ServerGateway;
import de.mkoehler.launchwindow.client.screen.ConnectScreen;
import de.mkoehler.launchwindow.client.screen.SpaceComplexScreen;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.ui.UiKit;
import de.mkoehler.launchwindow.client.view.Hub;
import de.mkoehler.launchwindow.model.PlayerSnapshot;

/**
 * The client application's shared entry point ({@code design.md} &sect;3.3: lwjgl3 stays a
 * thin launcher, core holds the actual client). Owns the widget kit and the server gateway for
 * the whole run and starts on the Connect screen.
 *
 * @author Mario Koehler
 */
public class LaunchWindowGame extends Game {

    private static final float MAX_FONT_SCALE = 2f;

    private UiKit ui;
    private ServerGateway gateway;
    private String lastServer = "localhost:45825";
    private String lastLogin = "";
    private ClientSession session;
    private final Hub.Clock hubClock = new Hub.Clock();

    @Override
    public void create() {
        ui = new UiKit(Gdx.files.internal(""), fontScale());
        gateway = createGateway();
        setScreen(new ConnectScreen(this));
    }

    /**
     * Creates the server gateway. The offscreen screenshot tool overrides this with a stub.
     *
     * @return the gateway
     */
    protected ServerGateway createGateway() {
        return new NetworkGateway();
    }

    /**
     * Returns the widget kit.
     *
     * @return the kit
     */
    public UiKit ui() {
        return ui;
    }

    /**
     * Returns the server gateway.
     *
     * @return the gateway
     */
    public ServerGateway gateway() {
        return gateway;
    }

    /**
     * Enters the game after a successful login or claim: starts the session (loading the game's
     * content pack) and opens the Space Complex hub.
     *
     * @param credentials who and where
     * @param state       the player's state
     * @param gameInfo    the game info
     * @return {@code null} on success, or why the game couldn't be entered (the client lacks the
     *     game's content pack)
     */
    public String enterGame(Credentials credentials, PlayerSnapshot state, GameInfo gameInfo) {
        try {
            session = ClientSession.start(credentials, state, gameInfo, LaunchWindowGame.class.getClassLoader());
        } catch (IllegalStateException e) {
            return e.getMessage();
        }
        setScreen(new SpaceComplexScreen(this, true));
        return null;
    }

    /**
     * Returns the latest turn whose report this player has read on this device. Kept in the
     * client's preferences per server and login, so it's per device (unconfirmed).
     *
     * @return the turn, or -1 if none was read
     */
    public long lastReadReport() {
        if (session == null) {
            return -1;
        }
        return preferences().getLong(readKey(), -1);
    }

    /**
     * Marks a turn's report as read, if it's newer than the one read before.
     *
     * @param turn the turn
     */
    public void markReportRead(long turn) {
        if (session == null || turn <= lastReadReport()) {
            return;
        }
        com.badlogic.gdx.Preferences preferences = preferences();
        preferences.putLong(readKey(), turn);
        preferences.flush();
    }

    /**
     * Returns whether a resolved turn's report hasn't been read yet.
     *
     * @return whether one is unread
     */
    public boolean reportUnread() {
        return session != null && session.latestResolvedTurn() >= 1 && session.latestResolvedTurn() > lastReadReport();
    }

    /**
     * Returns whether the game has a result this player hasn't seen announced on this device yet,
     * for the game result dialog ({@code ui-spec.md} &sect;4.13). Kept like the report's read mark.
     *
     * @return whether to announce the result
     */
    public boolean resultUnseen() {
        return session != null && session.gameInfo().response().decided()
            && !preferences().getBoolean(key("resultSeen"), false);
    }

    /**
     * Marks the game result as seen, once the player closed its announcement.
     */
    public void markResultSeen() {
        if (session == null) {
            return;
        }
        com.badlogic.gdx.Preferences preferences = preferences();
        preferences.putBoolean(key("resultSeen"), true);
        preferences.flush();
    }

    private String readKey() {
        return key("reportRead");
    }

    private String key(String name) {
        Credentials credentials = session.credentials();
        return name + "." + credentials.address().host() + ":" + credentials.address().port() + "."
            + credentials.login();
    }

    private static com.badlogic.gdx.Preferences preferences() {
        return Gdx.app.getPreferences("launchwindow");
    }

    /**
     * Returns the hub's time-of-day clock, which lives as long as the client does.
     *
     * @return the clock
     */
    public Hub.Clock hubClock() {
        return hubClock;
    }

    /**
     * Returns the session, once the player is in the game.
     *
     * @return the session, or {@code null} before entering
     */
    public ClientSession session() {
        return session;
    }

    /**
     * Sets the session directly. For the offscreen screenshot tool, which builds screens without a
     * server.
     *
     * @param session the session
     */
    public void setSession(ClientSession session) {
        this.session = session;
    }

    /**
     * Returns the server address typed at the last connect, to prefill the Connect screen.
     *
     * @return the address text
     */
    public String lastServer() {
        return lastServer;
    }

    /**
     * Returns the login typed at the last connect, to prefill the Connect screen. The password
     * is never remembered.
     *
     * @return the login
     */
    public String lastLogin() {
        return lastLogin;
    }

    /**
     * Remembers what was typed at a connect, for the next visit to the Connect screen.
     *
     * @param server the address text
     * @param login  the login
     */
    public void rememberConnect(String server, String login) {
        lastServer = server;
        lastLogin = login;
    }

    /**
     * Switches screens and disposes the previous one once the switch is done.
     *
     * @param screen the next screen
     */
    @Override
    public void setScreen(Screen screen) {
        Screen previous = getScreen();
        super.setScreen(screen);
        if (previous != null && previous != screen) {
            Gdx.app.postRunnable(previous::dispose);
        }
    }

    @Override
    public void dispose() {
        Screen current = getScreen();
        super.dispose();
        if (current != null) {
            current.dispose();
        }
        if (gateway != null) {
            gateway.dispose();
        }
        if (ui != null) {
            ui.dispose();
        }
    }

    /**
     * Picks the font scale from the monitor height, so text stays sharp on large displays.
     *
     * @return the scale, from 1 to {@value #MAX_FONT_SCALE}
     */
    private static float fontScale() {
        float monitorHeight = Gdx.graphics.getDisplayMode().height;
        return Math.min(MAX_FONT_SCALE, Math.max(1f, monitorHeight / Theme.VIEW_HEIGHT));
    }
}
