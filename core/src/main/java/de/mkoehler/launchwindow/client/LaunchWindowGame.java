package de.mkoehler.launchwindow.client;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import de.mkoehler.launchwindow.client.connect.NetworkGateway;
import de.mkoehler.launchwindow.client.connect.ServerGateway;
import de.mkoehler.launchwindow.client.screen.ConnectScreen;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.ui.UiKit;

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
