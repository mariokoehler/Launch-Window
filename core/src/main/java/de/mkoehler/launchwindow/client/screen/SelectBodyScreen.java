package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import de.mkoehler.launchwindow.client.LaunchWindowGame;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.view.BodyMap;
import de.mkoehler.launchwindow.client.view.Programs;
import de.mkoehler.launchwindow.content.CelestialBody;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Select celestial body ({@code ui-spec.md} &sect;4.6.2): the solar-system art with a hover target
 * on each body and a white label chip under it giving the programs the player's agency can open
 * there and those already open. Bodies with none of the agency's programs are inert and say so.
 *
 * @author Mario Koehler
 */
public final class SelectBodyScreen extends ChromeScreen {

    /**
     * Builds the screen from the session.
     *
     * @param game the game, in a session
     */
    public SelectBodyScreen(LaunchWindowGame game) {
        super(game, List.of(new Crumb("Space Complex", null), new Crumb("Headquarters", null),
            new Crumb("Select body", null)), "Headquarters", "Select celestial body");
        titleActions.add(ui.label("Under each body: programs your agency can open, and programs already open.",
            Theme.TextStyle.CAPTION, Theme.ON_PAGE_MUTED));
        rebuild();
    }

    @Override
    protected void goBack() {
        goHeadquarters();
    }

    @Override
    protected void rebuild() {
        content.clearChildren();
        content.add(new ArtMap()).expand().fill();
    }

    /**
     * The art, kept at its aspect ratio inside the content area, with the body targets and labels
     * placed on it by percentage.
     */
    private final class ArtMap extends WidgetGroup {
        private final Image art;
        private final Image frame;
        private final List<BodyTarget> targets = new ArrayList<>();
        private final List<Table> labels = new ArrayList<>();

        ArtMap() {
            frame = new Image(ui.rect(new Color(0f, 0f, 0f, 1f), Theme.ON_PAGE, Theme.BORDER_THIN));
            addActor(frame);
            Drawable picture = ui.picture("solar_system.jpg");
            art = new Image(picture == null ? ui.solid(Theme.NAVY) : picture);
            addActor(art);
            for (BodyMap.Target target : BodyMap.TARGETS) {
                Programs.Counts counts = Programs.bodyCounts(target.body(), session.state(), session.catalog());
                boolean any = counts.open() + counts.available() + counts.locked() > 0;
                BodyTarget hit = new BodyTarget(target, any);
                targets.add(hit);
                addActor(hit);
                Table label = new Table();
                label.setBackground(any ? ui.rect(Theme.SURFACE_CARD, Theme.NAVY, Theme.BORDER_THIN)
                    : ui.rect(new Color(1f, 1f, 1f, 0.85f), Theme.NAVY_BORDER, Theme.BORDER_THIN));
                label.pad(4f, 8f, 4f, 8f);
                label.add(ui.label(BodyMap.name(target.body()).toUpperCase(Locale.ROOT), Theme.TextStyle.LABEL,
                    Theme.NAVY)).row();
                Label countsText = ui.label(any ? counts.available() + " available · " + counts.open() + " open"
                    : "No programs for\nyour agency", Theme.TextStyle.CAPTION, Theme.INK_MUTED);
                countsText.setAlignment(Align.center);
                label.add(countsText);
                label.pack();
                label.setTouchable(Touchable.disabled);
                labels.add(label);
                addActor(label);
            }
        }

        @Override
        public void layout() {
            float width = getWidth();
            float height = width / BodyMap.ASPECT;
            if (height > getHeight()) {
                height = getHeight();
                width = height * BodyMap.ASPECT;
            }
            float x = (getWidth() - width) / 2f;
            float y = (getHeight() - height) / 2f;
            frame.setBounds(x - 1f, y - 1f, width + 2f, height + 2f);
            art.setBounds(x, y, width, height);
            for (int i = 0; i < targets.size(); i++) {
                BodyMap.Target target = BodyMap.TARGETS.get(i);
                float tw = width * target.width() / 100f;
                float th = height * target.height() / 100f;
                float tx = x + width * target.left() / 100f;
                float ty = y + height - height * target.top() / 100f - th;
                targets.get(i).setBounds(tx, ty, tw, th);
                Table label = labels.get(i);
                float lx = x + width * target.labelX() / 100f - label.getWidth() / 2f;
                float ly = y + height - height * target.labelY() / 100f - label.getHeight();
                label.setPosition(Math.max(x, Math.min(lx, x + width - label.getWidth())), ly);
            }
        }
    }

    /**
     * One body's hit ellipse: highlighted on hover with the hub's decided look (white at 18% fill
     * and outline), and clickable when the body holds the agency's programs.
     */
    private final class BodyTarget extends Actor {
        private final Drawable ellipse;
        private boolean hovered;

        BodyTarget(BodyMap.Target target, boolean active) {
            ellipse = ui.ellipseHighlight();
            if (active) {
                setTouchable(Touchable.enabled);
                addListener(new InputListener() {
                    @Override
                    public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                        if (pointer == -1) {
                            hovered = true;
                        }
                    }

                    @Override
                    public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                        if (pointer == -1) {
                            hovered = false;
                        }
                    }
                });
                addListener(clicked(() -> game.setScreen(new BodyProgramsScreen(game, target.body()))));
            } else {
                setTouchable(Touchable.disabled);
            }
        }

        @Override
        public Actor hit(float x, float y, boolean touchable) {
            if (touchable && getTouchable() != Touchable.enabled) {
                return null;
            }
            float rx = getWidth() / 2f;
            float ry = getHeight() / 2f;
            float dx = (x - rx) / rx;
            float dy = (y - ry) / ry;
            return dx * dx + dy * dy <= 1f ? this : null;
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            if (hovered) {
                batch.setColor(1f, 1f, 1f, parentAlpha);
                ellipse.draw(batch, getX(), getY(), getWidth(), getHeight());
            }
        }
    }

    /**
     * Opens a body's programs, as clicking it does.
     *
     * @param body the body
     */
    public void pick(CelestialBody body) {
        game.setScreen(new BodyProgramsScreen(game, body));
    }
}
