package de.mkoehler.launchwindow.lwjgl3;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowConfiguration;
import de.mkoehler.launchwindow.client.LaunchWindowGame;

/**
 * Launches the desktop (LWJGL3) client.
 * <p>
 * The window icon (what the taskbar shows for the running window) comes from
 * {@code launchwindow-icon-*.png} on the classpath, made from the mission-patch
 * logo; the packaged exe's own icon is {@code assets-raw/icon.ico}, set by
 * jpackage. Both are placeholders until a small icon-only logo exists (design.md
 * 3.12): the full patch reads as a coloured dot at 16 px.
 * <p>
 * The application starts with the splash window ({@link SplashListener}), which opens the game's
 * own window when its video ends; the app-wide settings (vsync, frame rate) are set on the
 * splash's configuration and carry over.
 *
 * @author Mario Koehler
 */
public final class Lwjgl3Launcher {

    /** The splash window's side, as a share of the monitor's height. */
    private static final float SPLASH_SHARE = 0.3f;

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
        Graphics.DisplayMode display = Lwjgl3ApplicationConfiguration.getDisplayMode();
        Lwjgl3WindowConfiguration game = gameWindow(display);
        SplashListener splash = new SplashListener(
            () -> ((Lwjgl3Application) Gdx.app).newWindow(new LaunchWindowGame(), game));
        new Lwjgl3Application(splash, splashWindow(display));
    }

    private static Lwjgl3ApplicationConfiguration splashWindow(Graphics.DisplayMode display) {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        decorate(configuration);
        configuration.useVsync(true);
        // Limits FPS to the refresh rate of the active monitor, plus 1 to match fractional refresh rates.
        configuration.setForegroundFPS(display.refreshRate + 1);
        int side = Math.round(display.height * SPLASH_SHARE);
        configuration.setWindowedMode(side, side);
        configuration.setDecorated(false);
        configuration.setResizable(false);
        return configuration;
    }

    private static Lwjgl3WindowConfiguration gameWindow(Graphics.DisplayMode display) {
        Lwjgl3WindowConfiguration configuration = new Lwjgl3WindowConfiguration();
        decorate(configuration);
        int height = windowHeight(display.height);
        configuration.setWindowedMode(height * 16 / 9, height);
        return configuration;
    }

    private static void decorate(Lwjgl3WindowConfiguration configuration) {
        configuration.setTitle("Launch Window");
        configuration.setWindowIcon("launchwindow-icon-128.png", "launchwindow-icon-64.png",
            "launchwindow-icon-32.png", "launchwindow-icon-16.png");
    }

    /**
     * Picks a 16:9 window height that fits the monitor: the design's own 1080 rows when there's
     * room for them, otherwise 85% of the monitor's height. The screens are laid out at
     * 1920 by 1080 and scaled to fit, so a too-small window makes the 10 to 13 px design text
     * hard to read.
     *
     * @param monitorHeight the monitor's height in pixels
     * @return the window height
     */
    private static int windowHeight(int monitorHeight) {
        int design = 1080;
        return monitorHeight > design * 1.1 ? design : Math.round(monitorHeight * 0.85f);
    }
}
