package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import de.mkoehler.launchwindow.client.ClientSession;
import de.mkoehler.launchwindow.client.LaunchWindowGame;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.view.Hub;
import de.mkoehler.launchwindow.model.Building;
import de.mkoehler.launchwindow.model.PlayerSnapshot;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Locale;

/**
 * The Space Complex hub ({@code ui-spec.md} &sect;4.3): the agency's art, full-bleed and without
 * the header and footer chrome, picked by archetype and game season, with the time of day
 * stepping on as the player comes and goes ({@link Hub.Clock}). Each building highlights on hover
 * with its name and opens its screen on click; a slim overlay strip across the top carries the
 * agency, the date, the countdown, the turn report and the funds and prestige.
 *
 * @author Mario Koehler
 */
public final class SpaceComplexScreen extends SessionScreen {

    /** The hover highlight: white at 18% for fill and outline alike, hard-edged (decided). */
    private static final Color HIGHLIGHT = new Color(1f, 1f, 1f, 0.18f);
    private static final Color LABEL_SHADOW = new Color(0f, 0f, 0f, 0.65f);
    private static final Color DIRECTOR = new Color(1f, 1f, 1f, 0.8f);
    /** Every strip chip's height. */
    private static final float CHIP_HEIGHT = 32f;
    /** The under-construction marker: a gold disc this far above the building's label. */
    private static final float MARKER_RISE = 55f;
    private static final float MARKER_RADIUS = 22f;

    private final List<Hub.Hotspot> hotspots;
    private final Hub.TimeOfDay timeOfDay;
    private final Image art = new Image();
    private final HotspotLayer layer = new HotspotLayer();
    private final Table strip = new Table();
    private final Label countdown;
    private final ShapeRenderer shapes = new ShapeRenderer();

    /**
     * Builds the hub from the session, stepping the time-of-day clock on entry.
     *
     * @param game the game, in a session
     */
    public SpaceComplexScreen(LaunchWindowGame game) {
        this(game, false);
    }

    /**
     * Builds the hub, opening an unread turn report at once when asked: on entering the game, not
     * on every return to the hub.
     *
     * @param game         the game, in a session
     * @param unreadReport whether to open an unread report
     */
    public SpaceComplexScreen(LaunchWindowGame game, boolean unreadReport) {
        super(game);
        this.timeOfDay = game.hubClock().enter(System.currentTimeMillis());
        this.hotspots = loadHotspots();
        countdown = ui.label("", Theme.TextStyle.LABEL, Theme.NAVY);
        root.setBackground(ui.solid(Theme.NAVY));
        art.setScaling(Scaling.fill);

        Table overlay = new Table();
        overlay.top();
        overlay.add(strip).growX().row();
        Stack stack = new Stack();
        stack.add(art);
        stack.add(layer);
        stack.add(overlay);
        root.add(stack).grow();
        rebuild();
        if (unreadReport && game.reportUnread()) {
            openUnreadReport();
        }
    }

    private List<Hub.Hotspot> loadHotspots() {
        try (InputStream in = Gdx.files.internal(Hub.hotspotPath(session.state().getArchetype())).read()) {
            return Hub.readHotspots(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    protected void rebuild() {
        PlayerSnapshot state = session.state();
        // The season can change while the player sits here (a turn resolved); the time of day can't.
        Drawable picture = ui.backdrop(Hub.artPath(state.getArchetype(), session.gameInfo().response().date().season(),
            timeOfDay));
        art.setDrawable(picture);
        rebuildStrip(state);
    }

    private void rebuildStrip(PlayerSnapshot state) {
        // No fade and nothing see-through: the art runs from near-black (winter night) to near-white
        // (Soviet winter noon), so everything on it sits on a solid surface.
        strip.clearChildren();
        strip.pad(20f, Theme.SPACE_6, 20f, Theme.SPACE_6);

        Table agency = new Table();
        agency.setBackground(ui.solid(Theme.NAVY));
        agency.pad(8f, 14f, 8f, 14f);
        agency.defaults().left();
        agency.add(ui.label(state.getAgencyName(), Theme.TextStyle.HEADING_SM, Theme.ON_FILL)).padBottom(2f).row();
        agency.add(ui.label("Dir. " + state.getDirectorName(), Theme.TextStyle.CAPTION, DIRECTOR));
        Table left = new Table();
        left.top().left();
        left.add(agency).left().row();
        if (session.packStatus() == ClientSession.PackStatus.DIFFERENT) {
            left.add(ui.notice(Theme.NoticeKind.WARNING, "Content differs.", "Your copy of this game's content "
                + "differs from the server's. Some numbers shown may be off until the client can download the "
                + "game's own copy.")).width(480f).left().padTop(Theme.SPACE_3);
        }
        strip.add(left).expandX().left().top();

        Table chips = new Table();
        float gap = 10f;
        chips.add(chip(ui.solid(Theme.NAVY), "game_time", Theme.ON_FILL,
            ui.label(session.gameInfo().response().date().label().toUpperCase(Locale.ROOT), Theme.TextStyle.LABEL,
                Theme.ON_FILL))).height(CHIP_HEIGHT).padRight(gap);
        chips.add(chip(ui.solid(Theme.SURFACE_CARD), "clock", Theme.NAVY, countdown)).height(CHIP_HEIGHT)
            .padRight(gap);
        // The strip's one button: white like its neighbours, with a navy outline.
        Table report = chip(ui.rect(Theme.SURFACE_CARD, Theme.NAVY, Theme.BORDER_EMPHASIS), "document", Theme.NAVY,
            ui.label("TURN REPORT", Theme.TextStyle.LABEL, Theme.NAVY));
        if (game.reportUnread()) {
            // The unread mark: a gold dot, as in the mockup.
            report.add(new Image(ui.unreadDot())).size(10f).padLeft(6f);
        }
        report.setTouchable(Touchable.enabled);
        report.addListener(clicked(() -> openLatestReport(null)));
        chips.add(report).height(CHIP_HEIGHT).padRight(gap);
        chips.add(chip(ui.solid(Theme.SURFACE_CARD), "funds", Theme.NAVY,
            ui.label(money(state.getFunds()), Theme.TextStyle.LABEL, Theme.NAVY))).height(CHIP_HEIGHT).padRight(gap);
        chips.add(chip(ui.solid(Theme.SURFACE_CARD), "prestige", Theme.NAVY,
            ui.label(String.format(Locale.ROOT, "%,d", state.getPrestige()), Theme.TextStyle.LABEL, Theme.NAVY)))
            .height(CHIP_HEIGHT);
        strip.add(chips).right().top();
    }

    private Table chip(Drawable background, String icon, Color color, Label text) {
        Table chip = new Table();
        chip.setBackground(background);
        chip.pad(0f, 12f, 0f, 12f);
        Image glyph = new Image(ui.icon(icon), Scaling.fit);
        glyph.setColor(color);
        chip.add(glyph).size(14f).padRight(6f);
        chip.add(text);
        return chip;
    }

    private static String money(long amount) {
        return String.format(Locale.ROOT, "$ %,d", amount);
    }

    @Override
    public void render(float delta) {
        countdown.setText(nextTurnText());
        super.render(delta);
    }

    @Override
    public void dispose() {
        shapes.dispose();
        super.dispose();
    }

    private void open(Hub.Place place) {
        switch (place) {
            case HEADQUARTERS -> goHeadquarters();
            case SET_CENTRE -> goSetCentre();
            case VAB -> goVab();
            case MISSION_CONTROL -> goMissionControl();
            default -> notBuiltYet(place.inSentence());
        }
    }

    /**
     * Shows a building as hovered, as the pointer resting on it does. For the offscreen screenshot
     * tool.
     *
     * @param place the place, or {@code null} for none
     */
    public void hover(Hub.Place place) {
        layer.hovered = place;
    }

    /**
     * The hotspots: finds the building under the pointer, draws its highlight and name, opens it on
     * click, and marks the buildings that are upgrading.
     */
    private final class HotspotLayer extends Actor {
        private final Label name = ui.label("", Theme.TextStyle.HEADING_SM, Theme.ON_FILL);
        private final Label shadow = ui.label("", Theme.TextStyle.HEADING_SM, LABEL_SHADOW);
        private final Drawable gear = ui.icon("settings");
        private Hub.Place hovered;

        HotspotLayer() {
            setTouchable(Touchable.enabled);
            addListener(new InputListener() {
                @Override
                public boolean mouseMoved(InputEvent event, float x, float y) {
                    hovered = placeAt(x, y);
                    return false;
                }

                @Override
                public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                    if (pointer == -1) {
                        hovered = null;
                    }
                }
            });
            addListener(clicked(() -> {
                if (hovered != null) {
                    open(hovered);
                }
            }));
        }

        /** Converts a local point to art pixels; the layer covers the whole art. */
        private float artX(float x) {
            return x * Hub.ART_WIDTH / getWidth();
        }

        private float artY(float y) {
            return y * Hub.ART_HEIGHT / getHeight();
        }

        private Hub.Place placeAt(float x, float y) {
            for (Hub.Hotspot spot : hotspots) {
                if (spot.contains(artX(x), artY(y))) {
                    return spot.place();
                }
            }
            return null;
        }

        @Override
        public Actor hit(float x, float y, boolean touchable) {
            if (touchable && getTouchable() != Touchable.enabled) {
                return null;
            }
            // Only the buildings take the pointer, so a click on bare ground does nothing.
            return placeAt(x, y) != null ? this : null;
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            float scaleX = getWidth() / Hub.ART_WIDTH;
            float scaleY = getHeight() / Hub.ART_HEIGHT;
            List<Hub.Hotspot> upgrading = hotspots.stream().filter(spot -> isUpgrading(spot.place())).toList();
            Hub.Hotspot hover = hotspots.stream().filter(spot -> spot.place() == hovered).findFirst().orElse(null);
            if (hover == null && upgrading.isEmpty()) {
                return;
            }
            batch.end();
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            shapes.setProjectionMatrix(batch.getProjectionMatrix());
            shapes.setTransformMatrix(batch.getTransformMatrix());
            shapes.translate(getX(), getY(), 0f);
            shapes.scale(scaleX, scaleY, 1f);
            if (hover != null) {
                float[] v = hover.vertices();
                short[] tri = hover.triangles();
                shapes.begin(ShapeRenderer.ShapeType.Filled);
                shapes.setColor(HIGHLIGHT);
                for (int i = 0; i < tri.length; i += 3) {
                    shapes.triangle(v[2 * tri[i]], v[2 * tri[i] + 1], v[2 * tri[i + 1]], v[2 * tri[i + 1] + 1],
                        v[2 * tri[i + 2]], v[2 * tri[i + 2] + 1]);
                }
                shapes.end();
                Gdx.gl.glLineWidth(2f);
                shapes.begin(ShapeRenderer.ShapeType.Line);
                shapes.setColor(HIGHLIGHT);
                shapes.polygon(v);
                shapes.end();
                Gdx.gl.glLineWidth(1f);
            }
            if (!upgrading.isEmpty()) {
                shapes.begin(ShapeRenderer.ShapeType.Filled);
                for (Hub.Hotspot spot : upgrading) {
                    shapes.setColor(Color.WHITE);
                    shapes.circle(spot.labelX(), spot.labelY() + MARKER_RISE, MARKER_RADIUS + 2f, 48);
                    shapes.setColor(Theme.GOLD);
                    shapes.circle(spot.labelX(), spot.labelY() + MARKER_RISE, MARKER_RADIUS, 48);
                }
                shapes.end();
            }
            shapes.identity();
            batch.begin();
            for (Hub.Hotspot spot : upgrading) {
                batch.setColor(Theme.NAVY.r, Theme.NAVY.g, Theme.NAVY.b, parentAlpha);
                gear.draw(batch, getX() + (spot.labelX() - 12f) * scaleX, getY() + (spot.labelY() + MARKER_RISE - 12f)
                    * scaleY, 24f * scaleX, 24f * scaleY);
            }
            batch.setColor(1f, 1f, 1f, parentAlpha);
            if (hover != null) {
                String text = hover.place().label().toUpperCase(Locale.ROOT);
                drawCentered(batch, shadow, text, hover.labelX() * scaleX + 1.5f, hover.labelY() * scaleY - 1.5f,
                    parentAlpha);
                drawCentered(batch, name, text, hover.labelX() * scaleX, hover.labelY() * scaleY, parentAlpha);
            }
        }

        private void drawCentered(Batch batch, Label label, String text, float x, float y, float parentAlpha) {
            label.setText(text);
            label.setAlignment(Align.center);
            label.pack();
            label.setPosition(getX() + x - label.getWidth() / 2f, getY() + y - label.getHeight() / 2f);
            label.draw(batch, parentAlpha);
        }

        private boolean isUpgrading(Hub.Place place) {
            Building building = place.building();
            return building != null && session.state().getBuildingUpgradeSeasonsRemaining().containsKey(building);
        }
    }
}
