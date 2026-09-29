package de.mkoehler.launchwindow.devtools;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * Standalone prototype (never shipped, see dev-tools' pom comment) for the
 * "clickable aerial-view main menu" idea: hover a hand-authored polygon over
 * a building and see it highlight, comparing a hard-edged fill against a
 * softened glow.
 * <p>
 * The polygon below is placed arbitrarily near the middle of the reference
 * image - it does not trace any real building outline. The point of this
 * prototype is purely to judge how the two highlight styles read on top of
 * the actual art, not to nail hotspot placement.
 * <p>
 * Controls: SPACE toggles hard/soft highlight style, UP/DOWN changes the
 * soft style's blur strength (rebaked on change since the glow is static
 * and cheap to precompute once, see the class-level note on
 * {@link #bakeSoftHighlight()}).
 *
 * @author Mario Koehler
 */
public final class MenuHotspotPrototype extends ApplicationAdapter {

    /**
     * Building polygon in image-pixel space, deliberately arbitrary (see class
     * javadoc) - a convex hexagon so the triangle-fan fill below stays simple.
     */
    private static final float[] BUILDING_POLYGON_FRACTIONS = {
        0.44f, 0.60f,
        0.48f, 0.56f,
        0.54f, 0.55f,
        0.58f, 0.58f,
        0.56f, 0.63f,
        0.48f, 0.65f
    };

    private static final Color HIGHLIGHT_COLOR = new Color(0.92f, 0.94f, 0.96f, 1f);
    private static final int MIN_DOWNSAMPLE = 1;
    private static final int MAX_DOWNSAMPLE = 24;

    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;
    private Texture aerialTexture;
    private Viewport viewport;
    private OrthographicCamera camera;

    private float[] polygonVertices;
    private Rectangle polygonBounds;
    private FrameBuffer softHighlightFbo;
    private int softDownsample = 16;
    private boolean softMode = false;
    private boolean hovering;
    private float highlightAlpha;

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("Launch Window - menu hotspot prototype");
        configuration.useVsync(true);
        configuration.setWindowedMode(1280, 726);
        new Lwjgl3Application(new MenuHotspotPrototype(), configuration);
    }

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();
        font.setColor(Color.WHITE);

        aerialTexture = new Texture(Gdx.files.internal("menu-prototype/aerial.png"));
        aerialTexture.setFilter(TextureFilter.Linear, TextureFilter.Linear);

        camera = new OrthographicCamera();
        viewport = new FitViewport(aerialTexture.getWidth(), aerialTexture.getHeight(), camera);

        polygonVertices = new float[BUILDING_POLYGON_FRACTIONS.length];
        for (int i = 0; i < polygonVertices.length; i += 2) {
            polygonVertices[i] = BUILDING_POLYGON_FRACTIONS[i] * aerialTexture.getWidth();
            polygonVertices[i + 1] = (1f - BUILDING_POLYGON_FRACTIONS[i + 1]) * aerialTexture.getHeight();
        }
        polygonBounds = computeBounds(polygonVertices);

        bakeSoftHighlight();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void render() {
        handleInput();
        updateHover();

        ScreenUtils.clear(Color.DARK_GRAY);
        viewport.apply();
        batch.setProjectionMatrix(camera.combined);
        shapeRenderer.setProjectionMatrix(camera.combined);

        batch.begin();
        batch.draw(aerialTexture, 0, 0);
        batch.end();

        if (highlightAlpha > 0.001f) {
            if (softMode) {
                drawSoftHighlight();
            } else {
                drawHardHighlight();
            }
        }

        drawHud();
    }

    private void handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            softMode = !softMode;
        }
        boolean downsampleChanged = false;
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            softDownsample = Math.max(MIN_DOWNSAMPLE, softDownsample - 2);
            downsampleChanged = true;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            softDownsample = Math.min(MAX_DOWNSAMPLE, softDownsample + 2);
            downsampleChanged = true;
        }
        if (downsampleChanged) {
            bakeSoftHighlight();
        }
    }

    private void updateHover() {
        Vector3 world = camera.unproject(
            new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0), viewport.getScreenX(),
            viewport.getScreenY(), viewport.getScreenWidth(), viewport.getScreenHeight());
        hovering = Intersector.isPointInPolygon(polygonVertices, 0, polygonVertices.length, world.x, world.y);

        float target = hovering ? 1f : 0f;
        float speed = 6f * Gdx.graphics.getDeltaTime();
        highlightAlpha = MathUtils.lerp(highlightAlpha, target, Math.min(1f, speed));
        if (Math.abs(highlightAlpha - target) < 0.005f) {
            highlightAlpha = target;
        }
    }

    private void drawHardHighlight() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeType.Filled);
        shapeRenderer.setColor(HIGHLIGHT_COLOR.r, HIGHLIGHT_COLOR.g, HIGHLIGHT_COLOR.b, 0.18f * highlightAlpha);
        fillPolygon(polygonVertices);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeType.Line);
        Gdx.gl.glLineWidth(2f);
        shapeRenderer.setColor(HIGHLIGHT_COLOR.r, HIGHLIGHT_COLOR.g, HIGHLIGHT_COLOR.b, 0.18f * highlightAlpha);
        drawPolygonOutline();
        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    /**
     * The polygon never moves, so the blurred glow is baked once into an
     * offscreen texture (re-baked only when the downsample slider changes)
     * rather than blurred every frame. The blur itself is the classic cheap
     * trick: render the highlight shape into a framebuffer far smaller than
     * its own bounding box, then let bilinear filtering do the smoothing
     * when that tiny texture is stretched back up over the full-size
     * polygon area - no blur shader needed. Higher downsample = blurrier.
     */
    private void bakeSoftHighlight() {
        if (softHighlightFbo != null) {
            softHighlightFbo.dispose();
        }
        int fboWidth = Math.max(1, MathUtils.round(polygonBounds.width / softDownsample));
        int fboHeight = Math.max(1, MathUtils.round(polygonBounds.height / softDownsample));
        softHighlightFbo = new FrameBuffer(com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888, fboWidth, fboHeight, false);

        OrthographicCamera fboCamera = new OrthographicCamera();
        fboCamera.setToOrtho(false, polygonBounds.width, polygonBounds.height);
        fboCamera.translate(polygonBounds.width / 2f, polygonBounds.height / 2f);
        fboCamera.update();

        softHighlightFbo.begin();
        Gdx.gl.glClearColor(0f, 0f, 0f, 0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setProjectionMatrix(fboCamera.combined);
        shapeRenderer.begin(ShapeType.Filled);
        shapeRenderer.setColor(HIGHLIGHT_COLOR.r, HIGHLIGHT_COLOR.g, HIGHLIGHT_COLOR.b, 1f);
        fillPolygon(toLocal(polygonVertices, polygonBounds));
        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
        softHighlightFbo.end();

        softHighlightFbo.getColorBufferTexture().setFilter(TextureFilter.Linear, TextureFilter.Linear);
    }

    private void drawSoftHighlight() {
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        Color c = batch.getColor();
        batch.setColor(1f, 1f, 1f, 0.55f * highlightAlpha);
        batch.draw(softHighlightFbo.getColorBufferTexture(),
            polygonBounds.x, polygonBounds.y, polygonBounds.width, polygonBounds.height,
            0, 0, 1, 1);
        batch.setColor(c);
        batch.end();
    }

    /**
     * ShapeRenderer has no direct filled-polygon call - {@code polygon()} only
     * draws outlines. A triangle fan from vertex 0 is enough since the demo
     * polygon is convex by construction (see {@link #BUILDING_POLYGON_FRACTIONS}).
     */
    private void fillPolygon(float[] vertices) {
        for (int i = 2; i + 3 < vertices.length; i += 2) {
            shapeRenderer.triangle(
                vertices[0], vertices[1],
                vertices[i], vertices[i + 1],
                vertices[i + 2], vertices[i + 3]);
        }
    }

    private void drawPolygonOutline() {
        int count = polygonVertices.length / 2;
        for (int i = 0; i < count; i++) {
            int next = (i + 1) % count;
            shapeRenderer.line(
                polygonVertices[i * 2], polygonVertices[i * 2 + 1],
                polygonVertices[next * 2], polygonVertices[next * 2 + 1]);
        }
    }

    private void drawHud() {
        batch.setProjectionMatrix(batch.getProjectionMatrix().idt().setToOrtho2D(
            0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
        batch.begin();
        font.draw(batch, "SPACE: toggle hard/soft (" + (softMode ? "soft" : "hard") + ")   "
                + "UP/DOWN: blur strength (downsample=" + softDownsample + ")   "
                + (hovering ? "HOVER" : ""),
            16, Gdx.graphics.getHeight() - 16);
        batch.end();
    }

    private static Rectangle computeBounds(float[] vertices) {
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        for (int i = 0; i < vertices.length; i += 2) {
            minX = Math.min(minX, vertices[i]);
            maxX = Math.max(maxX, vertices[i]);
            minY = Math.min(minY, vertices[i + 1]);
            maxY = Math.max(maxY, vertices[i + 1]);
        }
        // A margin so the baked glow has room to fade out past the polygon's own edge.
        float marginX = (maxX - minX) * 0.35f;
        float marginY = (maxY - minY) * 0.35f;
        return new Rectangle(minX - marginX, minY - marginY,
            (maxX - minX) + marginX * 2, (maxY - minY) + marginY * 2);
    }

    private static float[] toLocal(float[] vertices, Rectangle bounds) {
        float[] local = new float[vertices.length];
        for (int i = 0; i < vertices.length; i += 2) {
            local[i] = vertices[i] - bounds.x;
            local[i + 1] = vertices[i + 1] - bounds.y;
        }
        return local;
    }

    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        font.dispose();
        aerialTexture.dispose();
        if (softHighlightFbo != null) {
            softHighlightFbo.dispose();
        }
    }
}
