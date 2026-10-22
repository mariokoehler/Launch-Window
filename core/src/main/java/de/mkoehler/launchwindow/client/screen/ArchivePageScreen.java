package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import de.mkoehler.launchwindow.client.LaunchWindowGame;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.view.Help;
import de.mkoehler.launchwindow.client.view.FindingsView;
import de.mkoehler.launchwindow.client.view.Archive;
import de.mkoehler.launchwindow.client.view.ContentNames;
import de.mkoehler.launchwindow.client.view.Museum;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * An archive page ({@code ui-spec.md} &sect;4.19, from the Archive canvas, 2026-10-15): a real
 * mission's story, its pictures and its fact box, with its sound if it has one, laid out in three
 * columns like an encyclopedia article. A picture opens the viewer over the page. Back returns to
 * where the page was opened, the Museum's Archive tab or the turn report.
 *
 * @author Mario Koehler
 */
public final class ArchivePageScreen extends ChromeScreen {

    /** The text column's width, as in the mockup. */
    private static final float TEXT_WIDTH = 676f;
    /** The fact box column's width. */
    private static final float FACTS_WIDTH = 440f;
    /** A picture's shape in the gallery's grid, and a wide one's. */
    private static final float GRID_ASPECT = 5f / 4f;
    private static final float WIDE_ASPECT = 16f / 9f;
    private static final float MIN_WIDE_ASPECT = 1f;
    private static final float MAX_WIDE_ASPECT = 3f;
    /** The fact box's label column. */
    private static final float FACT_LABEL_WIDTH = 120f;

    private final Archive.Entry entry;
    private final Supplier<Screen> back;
    private Music music;
    private boolean playing;
    private Image playIcon;
    private Table trackFill;
    private Table track;
    private Label time;
    private Label state;
    private Table viewer;
    private int viewing;

    /**
     * Opens a page.
     *
     * @param game  the game, in a session
     * @param entry the page in this game; it must be open
     * @param back  builds the screen to go back to
     */
    public ArchivePageScreen(LaunchWindowGame game, Archive.Entry entry, Supplier<Screen> back) {
        super(game, List.of(Crumb.SPACE_COMPLEX, new Crumb("Museum",
                screen -> screen.game.setScreen(new MuseumScreen(screen.game, MuseumScreen.Tab.ARCHIVE))),
            new Crumb("Archive", null)), entry.page().agency() + " · " + entry.page().date(), entry.page().title());
        this.entry = entry;
        this.back = back;
        game.markArchiveRead(entry.page().id());
        stage.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (viewer == null) {
                    return false;
                }
                switch (keycode) {
                    case Input.Keys.ESCAPE -> closeViewer();
                    case Input.Keys.LEFT -> showPicture(viewing - 1);
                    case Input.Keys.RIGHT -> showPicture(viewing + 1);
                    default -> {
                        return false;
                    }
                }
                return true;
            }
        });
        rebuild();
    }

    @Override
    protected void goBack() {
        game.setScreen(back.get());
    }

    @Override
    public void hide() {
        super.hide();
        stopSound();
    }

    @Override
    public void dispose() {
        super.dispose();
        stopSound();
        // The full pictures are big; free them once the page is gone (the thumbnails stay).
        ui.releasePictures("archive/", "-thumb.jpg");
    }

    @Override
    public void render(float delta) {
        if (music != null && time != null) {
            float position = music.getPosition();
            updateTrack(position);
        }
        super.render(delta);
    }

    @Override
    protected Help.Context helpContext() {
        return Help.Context.MUSEUM;
    }

    @Override
    protected void rebuild() {
        Archive.Page page = entry.page();
        content.clearChildren();
        content.top().left();
        boolean finding = session.catalog().finding(page.goalId()).isPresent();
        String flown = entry.atTheEnd() ? "opened when the race was decided" : entry.byYou() ? "flown by you"
            : finding ? "published" : "flown by a rival";
        Table head = new Table();
        head.add(ui.label("In the archive since " + Museum.date(entry.turn()) + " · " + flown, Theme.TextStyle.BODY,
            Theme.ON_PAGE_STRONG)).expandX().left();
        if (finding) {
            // A finding's page keeps its Publish button until anyone publishes it (design.md 2.13).
            head.add(ui.label("A finding. Safer for those who know it: " + FindingsView.safer(session.catalog(),
                page.goalId()), Theme.TextStyle.CAPTION, Theme.ON_PAGE_STRONG)).right().padRight(Theme.SPACE_3);
            head.add(findingControls(page.goalId(), Theme.ButtonKind.PRIMARY)).right();
        }
        content.add(head).growX().left().padBottom(Theme.SPACE_4).row();
        Table columns = new Table();
        columns.top().left();
        columns.add(scrollPane(text(page))).width(TEXT_WIDTH).top().fillY().padRight(Theme.SPACE_4);
        columns.add(scrollPane(gallery(page))).expandX().fillX().top().fillY().padRight(Theme.SPACE_4);
        Table side = new Table();
        side.top();
        side.defaults().growX();
        side.add(factBox(page)).row();
        if (page.sound() != null) {
            side.add(soundCard(page.sound())).padTop(Theme.SPACE_4).row();
        }
        columns.add(side).width(FACTS_WIDTH).top();
        content.add(columns).expand().fill();
        if (viewer != null) {
            openViewer(viewing);
        }
    }

    // ---- The text

    private Table text(Archive.Page page) {
        Table card = ui.card();
        card.top();
        card.pad(28f, 28f, 24f, 28f);
        card.defaults().growX().left();
        for (Archive.Section section : page.sections()) {
            card.add(ui.label(section.heading(), Theme.TextStyle.ARTICLE_HEADING, Theme.NAVY)).padBottom(6f).row();
            card.add(ui.paragraph(section.text(), Theme.TextStyle.ARTICLE, Theme.INK)).padBottom(18f).row();
        }
        if (!page.sources().isEmpty()) {
            card.add(new Image(ui.solid(Theme.NAVY_BORDER))).height(Theme.BORDER_THIN).padBottom(Theme.SPACE_3).row();
            card.add(ui.label("SOURCES", Theme.TextStyle.LABEL, Theme.INK_MUTED)).padBottom(Theme.SPACE_1).row();
            for (int i = 0; i < page.sources().size(); i++) {
                card.add(ui.paragraph((i + 1) + ".  " + page.sources().get(i), Theme.TextStyle.CAPTION,
                    Theme.INK_MUTED)).padBottom(2f).row();
            }
        }
        return card;
    }

    // ---- The pictures

    private Table gallery(Archive.Page page) {
        Table card = ui.card();
        card.top();
        card.defaults().growX();
        Table head = new Table();
        List<Archive.Picture> pictures = page.images();
        head.add(ui.label("PICTURES · " + pictures.size(), Theme.TextStyle.EYEBROW, Theme.NAVY)).expandX().left();
        head.add(ui.label("Click a picture to see it larger", Theme.TextStyle.CAPTION, Theme.INK_MUTED));
        card.add(head).padBottom(Theme.SPACE_3).row();
        Table grid = new Table();
        grid.top();
        // An odd count: the first spans both columns, wide; the rest sit in pairs.
        int start = 0;
        if (pictures.size() % 2 == 1) {
            // A page's only picture keeps its own shape, between square and 3:1, so a whole disc (Luna 3) isn't
            // cut; the first of several is 16:9, as in the mockup, so the rest stay in view.
            TextureRegionDrawable whole = ui.picture(pictures.get(0).file());
            float aspect = whole == null || pictures.size() > 1 ? WIDE_ASPECT
                : Math.max(MIN_WIDE_ASPECT, Math.min(MAX_WIDE_ASPECT,
                    whole.getRegion().getRegionWidth() / (float) whole.getRegion().getRegionHeight()));
            grid.add(figure(pictures.get(0), 0, aspect)).colspan(2).growX().padBottom(Theme.SPACE_4).row();
            start = 1;
        }
        for (int i = start; i < pictures.size(); i++) {
            boolean left = (i - start) % 2 == 0;
            grid.add(figure(pictures.get(i), i, GRID_ASPECT)).growX().uniformX().top()
                .padRight(left ? Theme.SPACE_4 / 2 : 0f).padLeft(left ? 0f : Theme.SPACE_4 / 2).padBottom(Theme.SPACE_4);
            if (!left) {
                grid.row();
            }
        }
        card.add(grid);
        return card;
    }

    private Table figure(Archive.Picture picture, int index, float aspect) {
        Table figure = new Table();
        figure.top().left();
        figure.defaults().growX().left();
        Drawable art = ui.croppedPicture(picture.file(), aspect);
        Table frame = new Table();
        frame.setBackground(ui.rect(Theme.SKILL_TRACK, Theme.NAVY_BORDER, Theme.BORDER_THIN));
        frame.pad(Theme.BORDER_THIN);
        if (art != null) {
            frame.add(new AspectImage(art, aspect)).grow();
        } else {
            frame.add(new AspectImage(ui.solid(Theme.SKILL_TRACK), aspect)).grow();
        }
        frame.setTouchable(Touchable.enabled);
        frame.addListener(clicked(() -> openViewer(index)));
        figure.add(frame).padBottom(6f).row();
        figure.add(ui.paragraph(picture.caption(), Theme.TextStyle.BODY_SM, Theme.INK)).padBottom(2f).row();
        figure.add(ui.paragraph(picture.credit(), Theme.TextStyle.CAPTION, Theme.INK_MUTED));
        return figure;
    }

    /** An image that keeps a shape: its height follows its width. */
    private static final class AspectImage extends Image {

        private final float aspect;

        AspectImage(Drawable drawable, float aspect) {
            super(drawable, Scaling.stretch);
            this.aspect = aspect;
        }

        @Override
        public float getPrefWidth() {
            return 0f;
        }

        @Override
        public float getPrefHeight() {
            return getWidth() > 0f ? getWidth() / aspect : 0f;
        }

        @Override
        public void layout() {
            super.layout();
            if (getParent() instanceof Table parent && Math.abs(getHeight() - getWidth() / aspect) > 0.5f) {
                parent.invalidateHierarchy();
            }
        }
    }

    // ---- The fact box

    private Table factBox(Archive.Page page) {
        Table box = new Table();
        box.setBackground(ui.rect(Theme.SURFACE_CARD, Theme.NAVY, Theme.BORDER_EMPHASIS));
        box.top();
        box.defaults().growX();
        Table head = new Table();
        head.setBackground(ui.solid(Theme.NAVY));
        head.pad(12f, 16f, 12f, 16f);
        String name = page.title().contains(":") ? page.title().substring(0, page.title().indexOf(':')) : page.title();
        head.add(ui.label(name.toUpperCase(Locale.ROOT), Theme.TextStyle.EYEBROW, Theme.ON_FILL)).expandX().left();
        head.add(ui.label("FACTS", Theme.TextStyle.LABEL, Theme.ON_FILL));
        box.add(head).row();
        Table rows = new Table();
        rows.pad(8f, 16f, 14f, 16f);
        rows.top();
        rows.defaults().growX();
        for (Archive.Fact fact : page.facts()) {
            Table row = new Table();
            row.pad(8f, 0f, 8f, 0f);
            // A long label ("Time on the surface") wraps in its column rather than run into the value.
            row.add(ui.paragraph(fact.label().toUpperCase(Locale.ROOT), Theme.TextStyle.LABEL, Theme.INK_MUTED))
                .width(FACT_LABEL_WIDTH).top().left().padTop(3f).padRight(Theme.SPACE_2);
            row.add(ui.paragraph(fact.value(), Theme.TextStyle.BODY, Theme.INK)).expandX().fillX().left();
            rows.add(row).row();
            rows.add(new Image(ui.solid(Theme.NAVY_BORDER))).height(Theme.BORDER_THIN).row();
        }
        if (!page.firsts().isEmpty()) {
            rows.add(ui.label("FIRSTS", Theme.TextStyle.LABEL, Theme.INK_MUTED)).left().padTop(12f).padBottom(4f).row();
            for (String first : page.firsts()) {
                Table line = new Table();
                line.add(new Image(ui.solid(Theme.GOLD))).size(6f).top().padTop(7f).padRight(10f);
                line.add(ui.paragraph(first, Theme.TextStyle.BODY, Theme.INK)).expandX().fillX().left();
                rows.add(line).padBottom(3f).row();
            }
            rows.add(new Image(ui.solid(Theme.NAVY_BORDER))).height(Theme.BORDER_THIN).padTop(8f).row();
        }
        rows.add(ui.paragraph("Unlocked in this game by the goal " + ContentNames.of(page.goalId()) + ".",
            Theme.TextStyle.CAPTION, Theme.INK_MUTED)).padTop(10f).row();
        box.add(rows).row();
        return box;
    }

    // ---- The sound

    private Table soundCard(Archive.Sound sound) {
        Table card = ui.card();
        card.defaults().growX();
        Table head = new Table();
        head.add(ui.label("SOUND", Theme.TextStyle.EYEBROW, Theme.NAVY)).expandX().left();
        state = ui.label(clock(sound.seconds()), Theme.TextStyle.CAPTION, Theme.INK_MUTED);
        head.add(state);
        card.add(head).padBottom(Theme.SPACE_3).row();
        Table player = new Table();
        Table button = new Table();
        button.setBackground(ui.solid(Theme.NAVY));
        playIcon = new Image(ui.icon("play"), Scaling.fit);
        playIcon.setColor(Theme.ON_FILL);
        button.add(playIcon).size(18f);
        button.setTouchable(Touchable.enabled);
        button.addListener(clicked(this::toggleSound));
        player.add(button).size(40f).padRight(Theme.SPACE_3);
        track = new Table();
        track.setBackground(ui.solid(Theme.SKILL_TRACK));
        trackFill = new Table();
        trackFill.setBackground(ui.solid(Theme.NAVY));
        Stack bar = new Stack();
        bar.add(track);
        Table fillRow = new Table();
        fillRow.left();
        fillRow.add(trackFill).height(4f).width(0f);
        bar.add(fillRow);
        player.add(bar).height(4f).expandX().fillX().padRight(Theme.SPACE_3);
        time = ui.label("0:00 / " + clock(sound.seconds()), Theme.TextStyle.CAPTION, Theme.INK_MUTED);
        player.add(time);
        card.add(player).padBottom(Theme.SPACE_3).row();
        Table caption = new Table();
        caption.left();
        caption.add(ui.paragraph(sound.caption() + " " + sound.credit(), Theme.TextStyle.CAPTION, Theme.INK))
            .expandX().fillX().left();
        card.add(caption).row();
        if (music != null) {
            showPlaying();
        }
        return card;
    }

    private void toggleSound() {
        Archive.Sound sound = entry.page().sound();
        if (sound == null || !LaunchWindowGame.soundOn()) {
            return;
        }
        if (music == null) {
            music = Gdx.audio.newMusic(Gdx.files.internal(sound.file()));
            music.setVolume(LaunchWindowGame.musicVolume());
            music.setOnCompletionListener(done -> {
                playing = false;
                showPlaying();
            });
        }
        if (playing) {
            music.pause();
        } else {
            music.play();
        }
        playing = !playing;
        showPlaying();
    }

    private void showPlaying() {
        if (playIcon == null) {
            return;
        }
        playIcon.setDrawable(ui.icon(playing ? "pause" : "play"));
        if (state != null) {
            Archive.Sound sound = entry.page().sound();
            state.setText(playing ? "PLAYING" : clock(sound.seconds()));
            state.setStyle(ui.label("", playing ? Theme.TextStyle.LABEL : Theme.TextStyle.CAPTION,
                playing ? Theme.GOLD_TEXT : Theme.INK_MUTED).getStyle());
        }
    }

    private void updateTrack(float position) {
        Archive.Sound sound = entry.page().sound();
        if (sound == null || track == null) {
            return;
        }
        float share = Math.min(1f, position / Math.max(1, sound.seconds()));
        time.setText(clock(Math.round(position)) + " / " + clock(sound.seconds()));
        Table fillRow = (Table) trackFill.getParent();
        fillRow.getCell(trackFill).width(track.getWidth() * share);
        fillRow.invalidate();
    }

    private void stopSound() {
        if (music != null) {
            music.stop();
            music.dispose();
            music = null;
        }
        playing = false;
    }

    private static String clock(int seconds) {
        return seconds / 60 + ":" + String.format(Locale.ROOT, "%02d", seconds % 60);
    }

    // ---- The viewer

    /**
     * Opens the viewer on a picture.
     *
     * @param index the picture, from 0
     */
    public void openViewer(int index) {
        if (viewer != null) {
            viewer.remove();
        }
        viewing = index;
        viewer = new Table();
        viewer.setFillParent(true);
        viewer.setBackground(ui.solid(Theme.SCRIM_VIEWER));
        viewer.setTouchable(Touchable.enabled);
        viewer.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (event.getTarget() == viewer) {
                    closeViewer();
                }
            }
        });
        stage.addActor(viewer);
        fillViewer();
    }

    private void showPicture(int index) {
        List<Archive.Picture> pictures = entry.page().images();
        if (index < 0 || index >= pictures.size()) {
            return;
        }
        viewing = index;
        fillViewer();
    }

    private void fillViewer() {
        List<Archive.Picture> pictures = entry.page().images();
        Archive.Picture picture = pictures.get(viewing);
        viewer.clearChildren();
        viewer.pad(Theme.SPACE_6);
        Table top = new Table();
        top.add(ui.label((viewing + 1) + " OF " + pictures.size() + " · " + entry.page().title().toUpperCase(Locale.ROOT),
            Theme.TextStyle.EYEBROW, Theme.ON_PAGE_STRONG)).expandX().left();
        top.add(ui.label("ESC", Theme.TextStyle.LABEL, Theme.ON_PAGE)).padRight(Theme.SPACE_3);
        top.add(outlineButton("close", 0f, this::closeViewer)).size(48f);
        viewer.add(top).colspan(3).growX().row();
        viewer.add(arrow(-1, viewing > 0)).width(96f);
        TextureRegionDrawable art = ui.picture(picture.file());
        Table middle = new Table();
        if (art != null) {
            middle.add(new Image(art, Scaling.fit)).grow().row();
        }
        Label caption = ui.paragraph(picture.caption(), Theme.TextStyle.BODY, Theme.ON_PAGE_STRONG);
        caption.setAlignment(Align.center);
        Label credit = ui.paragraph(picture.credit(), Theme.TextStyle.CAPTION, Theme.ON_PAGE);
        credit.setAlignment(Align.center);
        middle.add(caption).width(900f).padTop(Theme.SPACE_4).row();
        middle.add(credit).width(900f).padTop(2f);
        viewer.add(middle).grow().pad(Theme.SPACE_4, 0f, Theme.SPACE_4, 0f);
        viewer.add(arrow(1, viewing < pictures.size() - 1)).width(96f);
    }

    private Table arrow(int step, boolean shown) {
        Table column = new Table();
        if (!shown) {
            return column;
        }
        // The arrow keys step too (the fonts have no arrow glyphs for the mockup's hint under the button).
        column.add(outlineButton("chevron", step < 0 ? 180f : 0f, () -> showPicture(viewing + step))).size(48f);
        return column;
    }

    private Table outlineButton(String icon, float rotation, Runnable action) {
        Table button = new Table();
        button.setBackground(ui.rect(new Color(0f, 0f, 0f, 0f), Theme.ON_PAGE, Theme.BORDER_THIN));
        Image glyph = new Image(ui.icon(icon), Scaling.fit);
        glyph.setColor(Theme.ON_PAGE_STRONG);
        glyph.setOrigin(Align.center);
        Table holder = new Table();
        holder.add(glyph).size(20f);
        holder.setTransform(rotation != 0f);
        if (rotation != 0f) {
            holder.setOrigin(10f, 10f);
            holder.setRotation(rotation);
        }
        button.add(holder).size(20f);
        button.setTouchable(Touchable.enabled);
        button.addListener(clicked(action));
        return button;
    }

    /**
     * Closes the viewer, back to the page; a sound playing goes on.
     */
    public void closeViewer() {
        if (viewer != null) {
            viewer.remove();
            viewer = null;
        }
    }
}
