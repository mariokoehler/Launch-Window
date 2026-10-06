package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.FitViewport;
import de.mkoehler.launchwindow.client.LaunchWindowGame;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.ui.UiKit;

/**
 * The base of every screen: a Scene2D stage in a {@value Theme#VIEW_WIDTH} by {@value
 * Theme#VIEW_HEIGHT} fit viewport, letterboxed in the page color, with a full-screen root
 * table on the blueprint grid.
 *
 * @author Mario Koehler
 */
abstract class StageScreen extends ScreenAdapter {

    protected final LaunchWindowGame game;
    protected final UiKit ui;
    protected final Stage stage;
    protected final Table root = new Table();
    private boolean shown;
    private int fontGeneration;

    /**
     * Creates the stage and its root table.
     *
     * @param game the game
     */
    protected StageScreen(LaunchWindowGame game) {
        this.game = game;
        this.ui = game.ui();
        this.stage = new Stage(new FitViewport(Theme.VIEW_WIDTH, Theme.VIEW_HEIGHT));
        root.setFillParent(true);
        root.setBackground(ui.blueprintGrid());
        stage.addActor(root);
        fontGeneration = ui.fonts().generation();
    }

    /**
     * Returns the screen's stage, for the offscreen screenshot tool to find widgets in.
     *
     * @return the stage
     */
    public Stage stage() {
        return stage;
    }

    /**
     * Returns whether this screen is the one showing. A result that arrives from the server
     * after the player has moved on must be dropped, not applied to a screen nobody sees.
     *
     * @return whether showing
     */
    protected boolean isShowing() {
        return shown;
    }

    @Override
    public void show() {
        shown = true;
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void hide() {
        shown = false;
        if (Gdx.input.getInputProcessor() == stage) {
            Gdx.input.setInputProcessor(null);
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(Theme.SURFACE_PAGE.r, Theme.SURFACE_PAGE.g, Theme.SURFACE_PAGE.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        if (fontGeneration != ui.fonts().generation()) {
            // The fonts were rebuilt for a new window size: text laid out before points at the old glyphs.
            fontGeneration = ui.fonts().generation();
            stage.getActors().forEach(UiKit::relayout);
        }
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
