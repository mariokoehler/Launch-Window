package de.mkoehler.launchwindow.lwjgl3;

import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import de.mkoehler.launchwindow.client.LaunchWindowGame;

/**
 * Launches the desktop (LWJGL3) client.
 * <p>
 * No window icon is set yet - there is no game art yet (design.md 1.2), so
 * this deliberately doesn't point at placeholder libGDX icon files the way a
 * freshly generated libGDX project template would; wire one up once real art
 * exists (see the sibling projects' own Lwjgl3Launcher for the pattern).
 *
 * @author Mario Koehler
 */
public final class Lwjgl3Launcher {

    /**
     * Not instantiable; this class only holds the process entry point.
     */
    private Lwjgl3Launcher() {
    }

    /**
     * Starts the client.
     *
     * @param args not used
     */
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration configuration = getDefaultConfiguration();
        new Lwjgl3Application(new LaunchWindowGame(), configuration);
    }

    private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("Launch Window");
        configuration.useVsync(true);
        Graphics.DisplayMode display = Lwjgl3ApplicationConfiguration.getDisplayMode();
        // Limits FPS to the refresh rate of the active monitor, plus 1 to match fractional refresh rates.
        configuration.setForegroundFPS(display.refreshRate + 1);
        configuration.setWindowedMode(1280, 720);
        return configuration;
    }
}
