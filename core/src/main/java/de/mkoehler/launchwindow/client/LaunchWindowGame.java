package de.mkoehler.launchwindow.client;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;

/**
 * The client application's shared entry point (design.md 3.3: lwjgl3 stays a
 * thin launcher, core holds the actual client). Currently just a placeholder
 * that clears the screen - no screens/UI exist yet, this only proves the
 * libGDX rendering pipeline itself is wired up correctly end to end.
 *
 * @author Mario Koehler
 */
public class LaunchWindowGame extends Game {

    @Override
    public void create() {
        Gdx.app.log("LaunchWindowGame", "Started");
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
    }
}
